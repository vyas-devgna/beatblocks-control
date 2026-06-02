package com.devgnav.beatblocks.spotify;

import com.devgnav.beatblocks.BeatBlocksClient;
import com.devgnav.beatblocks.BeatBlocksServices;
import com.devgnav.beatblocks.config.BeatBlocksConfig;
import com.devgnav.beatblocks.model.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class BeatBlocksApiClient implements AutoCloseable {
    private static final Gson GSON = new Gson();
    private static final int PAGE_LIMIT = 30;
    private static final long HEARTBEAT_STALE_MS = 15_000L;
    private static final int MAX_COMMAND_QUEUE = 128;

    private final int port;
    private final int requestTimeoutSeconds;
    private final ExecutorService executor;
    private final HttpServer server;
    private final LinkedBlockingQueue<JsonObject> commandQueue = new LinkedBlockingQueue<>(MAX_COMMAND_QUEUE);
    private final ConcurrentHashMap<String, PendingQuery> pendingQueries = new ConcurrentHashMap<>();

    private volatile PlaybackState currentPlaybackState = PlaybackState.inactive();

    // Diagnostics
    private volatile boolean bridgeRunning = false;
    private volatile boolean extensionConnected = false;
    private volatile long lastHeartbeatAt = 0;
    private volatile String lastError = "";
    private volatile String extensionVersion = "";
    private volatile int connectionCount = 0;

    // Toast tracking
    private volatile String lastTrackId = "";
    private volatile BeatBlocksItem toastTrack = null;
    private volatile long toastShowAt = 0;

    private final ScheduledExecutorService cleanupScheduler;

    public BeatBlocksApiClient(BeatBlocksConfig config, ExecutorService executor) {
        this.port = config.bridgePort;
        this.requestTimeoutSeconds = config.requestTimeoutSeconds;
        this.executor = executor;
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "beatblocks-cleanup");
            t.setDaemon(true);
            return t;
        });

        try {
            this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            this.server.createContext("/state", new StateHandler());
            this.server.createContext("/commands", new CommandsHandler());
            this.server.createContext("/response", new ResponseHandler());
            this.server.setExecutor(executor);
            this.server.start();
            this.bridgeRunning = true;
            BeatBlocksClient.LOGGER.info("BeatBlocks bridge started on port {}", port);
        } catch (IOException e) {
            this.bridgeRunning = false;
            this.lastError = "Port " + port + " in use: " + e.getMessage();
            throw new RuntimeException("Failed to start local BeatBlocks bridge server on port " + port, e);
        }

        // Clean up timed-out pending queries every 15 seconds
        cleanupScheduler.scheduleAtFixedRate(this::cleanupTimedOutQueries, 15, 15, TimeUnit.SECONDS);
    }

    // ── Playback State ──────────────────────────────────────────────────

    public CompletableFuture<PlaybackState> getPlaybackState() {
        return CompletableFuture.completedFuture(currentState());
    }

    public PlaybackState currentState() {
        return isExtensionFresh() ? currentPlaybackState : PlaybackState.inactive();
    }

    // ── Toast ────────────────────────────────────────────────────────────

    public BeatBlocksItem getToastTrack() { return toastTrack; }
    public long getToastShowAt() { return toastShowAt; }
    public void clearToast() { toastTrack = null; }

    // ── Diagnostics ─────────────────────────────────────────────────────

    public BridgeDiagnostics getDiagnostics() {
        boolean connected = isExtensionFresh();
        return new BridgeDiagnostics(
                bridgeRunning, connected,
                lastHeartbeatAt, lastError, port,
                BeatBlocksServices.MOD_VERSION, extensionVersion, connectionCount
        );
    }

    private boolean isExtensionFresh() {
        return extensionConnected
                && lastHeartbeatAt > 0
                && System.currentTimeMillis() - lastHeartbeatAt < HEARTBEAT_STALE_MS;
    }

    // ── Library Queries ─────────────────────────────────────────────────

    public CompletableFuture<BeatBlocksPage> getPlaylists() {
        return getPlaylists(null);
    }

    public CompletableFuture<BeatBlocksPage> getPlaylists(String nextPageToken) {
        return queryBridge("get_playlists", paginationParams(nextPageToken))
                .thenApply(json -> pageFromArray(json, "items", this::parsePlaylist));
    }

    public CompletableFuture<BeatBlocksPage> getSavedAlbums() {
        return getSavedAlbums(null);
    }

    public CompletableFuture<BeatBlocksPage> getSavedAlbums(String nextPageToken) {
        return queryBridge("get_saved_albums", paginationParams(nextPageToken))
                .thenApply(json -> {
                    JsonObject root = json != null && json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
                    List<BeatBlocksItem> items = new ArrayList<>();
                    for (JsonElement element : array(root, "items")) {
                        if (!element.isJsonObject()) continue;
                        JsonObject item = element.getAsJsonObject();
                        JsonObject album = object(item, "album");
                        items.add(album != null ? parseAlbum(album) : parseAnyItem(item));
                    }
                    return new BeatBlocksPage(items, string(root, "next", null));
                });
    }

    public CompletableFuture<BeatBlocksPage> getSavedTracks() {
        return getSavedTracks(null);
    }

    public CompletableFuture<BeatBlocksPage> getSavedTracks(String nextPageToken) {
        return queryBridge("get_saved_tracks", paginationParams(nextPageToken))
                .thenApply(json -> {
                    JsonObject root = json != null && json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
                    List<BeatBlocksItem> items = new ArrayList<>();
                    for (JsonElement element : array(root, "items")) {
                        if (!element.isJsonObject()) continue;
                        JsonObject item = element.getAsJsonObject();
                        JsonObject track = object(item, "track");
                        items.add(track != null ? parseTrack(track) : parseAnyItem(item));
                    }
                    return new BeatBlocksPage(items, string(root, "next", null));
                });
    }

    public CompletableFuture<BeatBlocksPage> getQueue() {
        return queryBridge("get_queue", null)
                .thenApply(json -> {
                    JsonObject root = json != null && json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
                    List<BeatBlocksItem> items = new ArrayList<>();
                    JsonArray queue = array(root, "queue");
                    if (queue.isEmpty()) queue = array(root, "items");
                    for (JsonElement element : queue) {
                        if (element.isJsonObject()) {
                            items.add(parseAnyItem(element.getAsJsonObject()));
                        }
                    }
                    return new BeatBlocksPage(items, null);
                });
    }

    public CompletableFuture<BeatBlocksPage> getPlaylistTracks(BeatBlocksItem playlist, String nextPageToken) {
        if (playlist == null || !playlist.playable()) {
            return CompletableFuture.completedFuture(BeatBlocksPage.empty());
        }
        JsonObject params = paginationParams(nextPageToken);
        params.addProperty("uri", playlist.uri());
        params.addProperty("id", playlist.id());
        return queryBridge("get_playlist_tracks", params)
                .thenApply(this::pageFromTrackItems);
    }

    // ── Playback Commands ───────────────────────────────────────────────

    public CompletableFuture<Void> playItem(BeatBlocksItem item) {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("action", "play");
        JsonObject params = new JsonObject();
        params.addProperty("uri", item.uri());
        cmd.add("params", params);
        enqueueCommand(cmd);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> playItemInContext(BeatBlocksItem item, BeatBlocksItem context) {
        JsonObject params = new JsonObject();
        params.addProperty("uri", item.uri());
        if (context != null && context.playable()) params.addProperty("context_uri", context.uri());
        sendFireAndForget("play", params);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> playContext(BeatBlocksItem context, boolean shuffle) {
        if (context == null || !context.playable()) return CompletableFuture.completedFuture(null);
        JsonObject params = new JsonObject();
        params.addProperty("uri", context.uri());
        params.addProperty("shuffle", shuffle);
        sendFireAndForget("play_context", params);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> resume() {
        sendFireAndForget("play", null);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> pause() {
        sendFireAndForget("pause", null);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> next() {
        sendFireAndForget("next", null);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> previous() {
        sendFireAndForget("previous", null);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> setShuffle(boolean enabled) {
        JsonObject params = new JsonObject();
        params.addProperty("enabled", enabled);
        sendFireAndForget("set_shuffle", params);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> setRepeat(String state) {
        JsonObject params = new JsonObject();
        params.addProperty("state", state);
        sendFireAndForget("set_repeat", params);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> setVolume(int volumePercent) {
        JsonObject params = new JsonObject();
        params.addProperty("volume_percent", Math.max(0, Math.min(100, volumePercent)));
        sendFireAndForget("set_volume", params);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> seek(int positionMs) {
        JsonObject params = new JsonObject();
        params.addProperty("position_ms", Math.max(0, positionMs));
        sendFireAndForget("seek", params);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> addToQueue(BeatBlocksItem item) {
        JsonObject params = new JsonObject();
        params.addProperty("uri", item.uri());
        sendFireAndForget("add_to_queue", params);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> toggleLike() {
        sendFireAndForget("toggle_like", null);
        return CompletableFuture.completedFuture(null);
    }

    // ── Internal Bridge ─────────────────────────────────────────────────

    private void sendFireAndForget(String action, JsonObject params) {
        JsonObject cmd = new JsonObject();
        cmd.addProperty("action", action);
        if (params != null) cmd.add("params", params);
        enqueueCommand(cmd);
    }

    private CompletableFuture<JsonElement> queryBridge(String action, JsonObject params) {
        String reqId = UUID.randomUUID().toString();
        CompletableFuture<JsonElement> future = new CompletableFuture<>();
        pendingQueries.put(reqId, new PendingQuery(future, System.currentTimeMillis()));

        JsonObject cmd = new JsonObject();
        cmd.addProperty("action", action);
        cmd.addProperty("req_id", reqId);
        if (params != null) cmd.add("params", params);
        enqueueCommand(cmd);

        return future;
    }

    private void enqueueCommand(JsonObject command) {
        while (!commandQueue.offer(command)) {
            JsonObject dropped = commandQueue.poll();
            if (dropped == null) break;
            String droppedReqId = string(dropped, "req_id", null);
            if (droppedReqId != null) {
                PendingQuery pq = pendingQueries.remove(droppedReqId);
                if (pq != null && !pq.future.isDone()) {
                    pq.future.completeExceptionally(new BeatBlocksApiException(0, "Bridge command queue overflow", ""));
                }
            }
        }
    }

    private JsonObject paginationParams(String nextPageToken) {
        JsonObject params = new JsonObject();
        params.addProperty("limit", PAGE_LIMIT);
        int offset = 0;
        if (nextPageToken != null && !nextPageToken.isBlank()) {
            try {
                offset = Math.max(0, Integer.parseInt(nextPageToken));
            } catch (NumberFormatException ignored) {
                offset = 0;
            }
        }
        params.addProperty("offset", offset);
        return params;
    }

    private void cleanupTimedOutQueries() {
        long deadline = System.currentTimeMillis() - (requestTimeoutSeconds * 1000L);
        pendingQueries.forEach((id, pq) -> {
            if (pq.createdAt < deadline && !pq.future.isDone()) {
                pq.future.completeExceptionally(
                        new BeatBlocksApiException(0, "Bridge request timed out after " + requestTimeoutSeconds + "s. Is BeatBlocks open with Spicetify?", ""));
                pendingQueries.remove(id);
            }
        });
    }

    // ── HTTP Handlers ───────────────────────────────────────────────────

    private class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "");
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, GSON.toJson(currentPlaybackState));
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    JsonObject stateJson = GSON.fromJson(reader, JsonObject.class);
                    if (stateJson != null) {
                        boolean isPlaying = bool(stateJson, "is_playing", false);
                        String deviceName = string(stateJson, "device_name", "BeatBlocks");
                        int progressMs = integer(stateJson, "progress_ms", 0);
                        int volumePercent = integer(stateJson, "volume_percent", 50);
                        boolean shuffleState = bool(stateJson, "shuffle_state", false);
                        String repeatState = string(stateJson, "repeat_state", "off");

                        BeatBlocksItem track = null;
                        if (stateJson.has("track") && !stateJson.get("track").isJsonNull()) {
                            JsonObject trackJson = stateJson.getAsJsonObject("track");
                            String uri = string(trackJson, "uri", "");
                            String id = string(trackJson, "id", idFromUri(uri));
                            String coverUrl = string(trackJson, "image_url", null);
                            if (coverUrl == null || coverUrl.isBlank()) {
                                coverUrl = imageUrl(trackJson);
                            }
                            track = new BeatBlocksItem(
                                    BeatBlocksItemType.TRACK,
                                    id,
                                    string(trackJson, "name", "Unknown track"),
                                    string(trackJson, "subtitle", string(trackJson, "artists", "Unknown artist")),
                                    uri,
                                    coverUrl,
                                    string(trackJson, "external_url", id.isBlank() ? null : "https://open.spotify.com/track/" + id),
                                    integer(trackJson, "duration_ms", 0)
                            );
                        }

                        // Detect track change for toast
                        String newTrackId = track != null ? track.id() : "";
                        if (!newTrackId.isEmpty() && !newTrackId.equals(lastTrackId)) {
                            toastTrack = track;
                            toastShowAt = System.currentTimeMillis();
                        }
                        lastTrackId = newTrackId;

                        currentPlaybackState = new PlaybackState(
                                track != null, deviceName, isPlaying, progressMs,
                                volumePercent, shuffleState, repeatState, track
                        );

                        // Update diagnostics
                        extensionConnected = true;
                        lastHeartbeatAt = System.currentTimeMillis();
                        connectionCount++;
                        lastError = "";
                        extensionVersion = string(stateJson, "extension_version", "");
                    }
                } catch (Exception e) {
                    lastError = "State parse error: " + e.getMessage();
                    BeatBlocksClient.LOGGER.debug("State parse error", e);
                }
                sendResponse(exchange, 200, "{}");
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private class CommandsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "");
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                JsonObject command = null;
                try {
                    command = commandQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                String responseBody = command != null ? GSON.toJson(command) : "{}";
                sendResponse(exchange, 200, responseBody);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private class ResponseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "");
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    JsonObject res = GSON.fromJson(reader, JsonObject.class);
                    if (res != null && res.has("req_id")) {
                        String reqId = res.get("req_id").getAsString();
                        PendingQuery pq = pendingQueries.remove(reqId);
                        if (pq != null) {
                            // Check if the extension sent an error
                            if (res.has("error") && !res.get("error").isJsonNull()) {
                                String errMsg = res.get("error").getAsString();
                                lastError = errMsg;
                                pq.future.completeExceptionally(new BeatBlocksApiException(0, errMsg, ""));
                            } else {
                                JsonElement data = res.has("data") && !res.get("data").isJsonNull()
                                        ? res.get("data")
                                        : new JsonObject();
                                pq.future.complete(data);
                            }
                        }
                    }
                } catch (Exception e) {
                    lastError = "Response parse error: " + e.getMessage();
                    BeatBlocksClient.LOGGER.debug("Response parse error", e);
                }
                sendResponse(exchange, 200, "{}");
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ── Parsing ─────────────────────────────────────────────────────────

    private BeatBlocksPage pageFromArray(JsonElement json, String field, Parser parser) {
        JsonObject root = json != null && json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
        List<BeatBlocksItem> items = new ArrayList<>();
        for (JsonElement element : array(root, field)) {
            if (element.isJsonObject()) items.add(parser.parse(element.getAsJsonObject()));
        }
        return new BeatBlocksPage(items, string(root, "next", null));
    }

    private BeatBlocksPage pageFromTrackItems(JsonElement json) {
        JsonObject root = json != null && json.isJsonObject() ? json.getAsJsonObject() : new JsonObject();
        List<BeatBlocksItem> items = new ArrayList<>();
        for (JsonElement element : array(root, "items")) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            JsonObject track = object(item, "track");
            items.add(track != null ? parseTrack(track) : parseAnyItem(item));
        }
        return new BeatBlocksPage(items, string(root, "next", null));
    }

    private BeatBlocksItem parseAnyItem(JsonObject item) {
        if (item == null) {
            return new BeatBlocksItem(BeatBlocksItemType.TRACK, "", "Unknown item", "", "", null, null, 0);
        }
        String type = string(item, "type", "").toLowerCase(Locale.ROOT);
        if (type.isBlank()) type = typeFromUri(string(item, "uri", ""));
        if (item.has("subtitle") || item.has("image_url")) {
            BeatBlocksItemType itemType = switch (type) {
                case "album" -> BeatBlocksItemType.ALBUM;
                case "playlist" -> BeatBlocksItemType.PLAYLIST;
                case "artist" -> BeatBlocksItemType.ARTIST;
                default -> BeatBlocksItemType.TRACK;
            };
            return new BeatBlocksItem(
                    itemType,
                    string(item, "id", idFromUri(string(item, "uri", ""))),
                    string(item, "name", "Unknown " + itemType.name().toLowerCase(Locale.ROOT)),
                    string(item, "subtitle", defaultSubtitle(itemType)),
                    string(item, "uri", ""),
                    string(item, "image_url", null),
                    string(item, "external_url", null),
                    itemType == BeatBlocksItemType.TRACK ? integer(item, "duration_ms", 0) : 0
            );
        }
        return switch (type) {
            case "album" -> parseAlbum(item);
            case "playlist" -> parsePlaylist(item);
            case "artist" -> parseArtist(item);
            default -> parseTrack(item);
        };
    }

    private BeatBlocksItem parseTrack(JsonObject track) {
        if (track.has("subtitle") || track.has("image_url")) return parseAnyItem(track);
        JsonObject album = object(track, "album");
        return new BeatBlocksItem(
                BeatBlocksItemType.TRACK,
                string(track, "id", ""),
                string(track, "name", "Unknown track"),
                joinArtists(array(track, "artists")),
                string(track, "uri", ""),
                imageUrl(album),
                externalUrl(track),
                integer(track, "duration_ms", 0)
        );
    }

    private BeatBlocksItem parseAlbum(JsonObject album) {
        if (album.has("subtitle") || album.has("image_url")) return parseAnyItem(album);
        return new BeatBlocksItem(
                BeatBlocksItemType.ALBUM,
                string(album, "id", ""),
                string(album, "name", "Unknown album"),
                joinArtists(array(album, "artists")),
                string(album, "uri", ""),
                imageUrl(album),
                externalUrl(album),
                0
        );
    }

    private BeatBlocksItem parsePlaylist(JsonObject playlist) {
        if (playlist.has("subtitle") || playlist.has("image_url")) return parseAnyItem(playlist);
        JsonObject owner = object(playlist, "owner");
        return new BeatBlocksItem(
                BeatBlocksItemType.PLAYLIST,
                string(playlist, "id", ""),
                string(playlist, "name", "Unknown playlist"),
                "Playlist • " + string(owner, "display_name", "BeatBlocks"),
                string(playlist, "uri", ""),
                imageUrl(playlist),
                externalUrl(playlist),
                0
        );
    }

    private BeatBlocksItem parseArtist(JsonObject artist) {
        if (artist.has("subtitle") || artist.has("image_url")) return parseAnyItem(artist);
        return new BeatBlocksItem(
                BeatBlocksItemType.ARTIST,
                string(artist, "id", ""),
                string(artist, "name", "Unknown artist"),
                "Artist",
                string(artist, "uri", ""),
                imageUrl(artist),
                externalUrl(artist),
                0
        );
    }

    private static String defaultSubtitle(BeatBlocksItemType type) {
        return switch (type) {
            case TRACK -> "Unknown artist";
            case ALBUM -> "Album";
            case PLAYLIST -> "Playlist";
            case ARTIST -> "Artist";
        };
    }

    private static String joinArtists(JsonArray artists) {
        List<String> names = new ArrayList<>();
        for (JsonElement element : artists) {
            if (!element.isJsonObject()) continue;
            JsonObject artist = element.getAsJsonObject();
            String name = string(artist, "name", null);
            if (name != null) names.add(name);
        }
        return names.isEmpty() ? "Unknown artist" : String.join(", ", names);
    }

    private static String imageUrl(JsonObject object) {
        if (object == null) return null;
        List<JsonObject> candidates = new ArrayList<>();
        addImageCandidates(candidates, array(object, "images"));
        addImageCandidates(candidates, array(object(object, "coverArt"), "sources"));
        addImageCandidates(candidates, array(object(object(object, "visuals"), "avatarImage"), "sources"));
        addImageCandidates(candidates, array(object(object(object, "albumOfTrack"), "coverArt"), "sources"));
        addImageCandidates(candidates, array(object(object(object, "album"), "coverArt"), "sources"));
        addImageCandidates(candidates, array(object(object, "album"), "images"));
        candidates.sort(Comparator.comparingInt(img -> Math.abs(integer(img, "width", integer(img, "height", 0)) - 640)));
        for (JsonObject image : candidates) {
            String url = string(image, "url", null);
            if (url != null && !url.isBlank()) return url;
        }
        return null;
    }

    private static void addImageCandidates(List<JsonObject> candidates, JsonArray images) {
        for (JsonElement element : images) {
            if (element.isJsonObject()) {
                JsonObject image = element.getAsJsonObject();
                if (image.has("url")) candidates.add(image);
            }
        }
    }

    private static String externalUrl(JsonObject object) {
        JsonObject external = object(object, "external_urls");
        return string(external, "spotify", null);
    }

    private static String idFromUri(String uri) {
        if (uri == null) return "";
        String[] parts = uri.split(":");
        for (String type : List.of("track", "album", "artist", "playlist")) {
            for (int i = 0; i < parts.length - 1; i++) {
                if (type.equalsIgnoreCase(parts[i])) return cleanId(parts[i + 1]);
            }
        }
        for (String type : List.of("track", "album", "artist", "playlist")) {
            String marker = "/" + type + "/";
            int idx = uri.toLowerCase(Locale.ROOT).indexOf(marker);
            if (idx >= 0) return cleanId(uri.substring(idx + marker.length()));
        }
        int idx = uri.lastIndexOf(':');
        return idx >= 0 && idx + 1 < uri.length() ? cleanId(uri.substring(idx + 1)) : cleanId(uri);
    }

    private static String typeFromUri(String uri) {
        if (uri == null) return "";
        String[] parts = uri.split(":");
        for (String type : List.of("track", "album", "artist", "playlist")) {
            for (String part : parts) {
                if (type.equalsIgnoreCase(part)) return type;
            }
        }
        String lower = uri.toLowerCase(Locale.ROOT);
        for (String type : List.of("track", "album", "artist", "playlist")) {
            if (lower.contains("/" + type + "/")) return type;
        }
        return "";
    }

    private static String cleanId(String id) {
        if (id == null) return "";
        int end = id.length();
        for (char delimiter : new char[] {'?', '#', '/'}) {
            int idx = id.indexOf(delimiter);
            if (idx >= 0) end = Math.min(end, idx);
        }
        return id.substring(0, end);
    }

    // ── JSON Helpers ────────────────────────────────────────────────────

    private static JsonObject object(JsonObject object, String field) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull() || !object.get(field).isJsonObject()) return null;
        return object.getAsJsonObject(field);
    }

    private static JsonArray array(JsonObject object, String field) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull() || !object.get(field).isJsonArray()) return new JsonArray();
        return object.getAsJsonArray(field);
    }

    private static String string(JsonObject object, String field, String fallback) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) return fallback;
        try { return object.get(field).getAsString(); } catch (Exception ignored) { return fallback; }
    }

    private static int integer(JsonObject object, String field, int fallback) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) return fallback;
        try { return object.get(field).getAsInt(); } catch (Exception ignored) { return fallback; }
    }

    private static boolean bool(JsonObject object, String field, boolean fallback) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) return fallback;
        try { return object.get(field).getAsBoolean(); } catch (Exception ignored) { return fallback; }
    }

    @Override
    public void close() {
        cleanupScheduler.shutdownNow();
        if (server != null) {
            server.stop(0);
        }
        bridgeRunning = false;
    }

    private record PendingQuery(CompletableFuture<JsonElement> future, long createdAt) {}

    @FunctionalInterface
    private interface Parser {
        BeatBlocksItem parse(JsonObject object);
    }
}
