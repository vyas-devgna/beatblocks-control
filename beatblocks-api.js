(function BeatBlocksAPI() {
    if (!window.Spicetify || !Spicetify.Player || !Spicetify.Platform) {
        setTimeout(BeatBlocksAPI, 300);
        return;
    }

    const EXTENSION_VERSION = "2.3.0";
    const PORT = 50321;
    const STATE_URL = `http://127.0.0.1:${PORT}/state`;
    const COMMANDS_URL = `http://127.0.0.1:${PORT}/commands`;
    const RESPONSE_URL = `http://127.0.0.1:${PORT}/response`;
    const WEB_API_URL = "https://api.spotify.com/v1/";
    const DEFAULT_LIMIT = 30;
    const MAX_LIMIT = 50;

    const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
    const clampLimit = (value) => Math.max(1, Math.min(MAX_LIMIT, Number(value || DEFAULT_LIMIT)));
    const offsetFrom = (params) => Math.max(0, Number(params?.offset || params?.next || 0));

    function typeFromUri(uri) {
        const text = String(uri || "");
        const urlMatch = text.match(/open\.spotify\.com\/(track|album|artist|playlist)\/([^/?#]+)/i);
        if (urlMatch) return { type: urlMatch[1].toLowerCase(), id: urlMatch[2] };
        const parts = text.split(":");
        for (const type of ["track", "album", "artist", "playlist"]) {
            const index = parts.indexOf(type);
            if (index >= 0 && index + 1 < parts.length) return { type, id: parts[index + 1] };
        }
        return { type: "", id: "" };
    }

    function playableUri(value) {
        const text = String(value || "");
        const parsed = typeFromUri(text);
        if (!parsed.type || !parsed.id) return text;
        return `spotify:${parsed.type}:${parsed.id}`;
    }

    function idFromPlayable(value, expectedType = "") {
        const parsed = typeFromUri(value);
        if (expectedType && parsed.type && parsed.type !== expectedType) return "";
        return parsed.id || String(value || "");
    }

    function getPath(object, path) {
        let cursor = object;
        for (const part of path) {
            if (cursor == null) return undefined;
            cursor = cursor[part];
        }
        return cursor;
    }

    function firstValue(...values) {
        for (const value of values) {
            if (value !== undefined && value !== null && value !== "") return value;
        }
        return undefined;
    }

    function unwrapEntity(entity) {
        let current = entity;
        for (let i = 0; i < 8; i++) {
            if (!current || typeof current !== "object") return current;
            if (current.data && typeof current.data === "object" && (current.data.uri || current.data.__typename || current.data.name)) {
                current = current.data;
                continue;
            }
            if (current.item && typeof current.item === "object" && (current.item.uri || current.item.data || current.item.__typename)) {
                current = current.item;
                continue;
            }
            if (current.track && typeof current.track === "object" && (current.track.uri || current.track.data)) {
                current = current.track;
                continue;
            }
            if (current.contextTrack && typeof current.contextTrack === "object" && (current.contextTrack.uri || current.contextTrack.metadata)) {
                current = current.contextTrack;
                continue;
            }
            const ownType = String(firstValue(current.type, typenameType(current), typeFromUri(firstValue(current.uri, current.metadata?.uri, current.link, current._uri)).type, "")).toLowerCase();
            if (["track", "album", "playlist", "artist"].includes(ownType)) {
                return current;
            }
            if (current.album && typeof current.album === "object" && (current.album.uri || current.album.data)) {
                current = current.album;
                continue;
            }
            if (current.playlist && typeof current.playlist === "object" && (current.playlist.uri || current.playlist.data)) {
                current = current.playlist;
                continue;
            }
            if (current.artist && typeof current.artist === "object" && (current.artist.uri || current.artist.data || current.artist.profile)) {
                current = current.artist;
                continue;
            }
            return current;
        }
        return current;
    }

    function toImageUrl(value) {
        if (!value) return null;
        if (typeof value === "string") {
            if (value.startsWith("http://") || value.startsWith("https://")) return value;
            if (value.startsWith("spotify:image:")) return `https://i.scdn.co/image/${value.split(":").pop()}`;
            if (/^[a-f0-9]{40}$/i.test(value)) return `https://i.scdn.co/image/${value}`;
            return null;
        }
        if (Array.isArray(value)) {
            const candidates = value
                .map((entry) => ({ entry, url: toImageUrl(entry), width: Number(entry?.width || entry?.height || 0) }))
                .filter((entry) => entry.url);
            candidates.sort((a, b) => Math.abs((a.width || 640) - 640) - Math.abs((b.width || 640) - 640));
            return candidates[0]?.url || null;
        }
        return toImageUrl(value.url || value.uri || value.image_url || value.imageUrl || value.source || value.sources || value.cdnUrl || value.fileId);
    }

    function imageFrom(entity) {
        const data = unwrapEntity(entity);
        const metadata = data?.metadata || entity?.metadata || {};
        return toImageUrl(
            metadata.image_xlarge_url ||
            metadata.image_large_url ||
            metadata.image_url ||
            metadata.image_small_url ||
            metadata.picture ||
            data?.image_url ||
            data?.imageUrl ||
            data?.coverUrl ||
            data?.images?.items?.[0]?.sources ||
            data?.images ||
            data?.coverArt?.sources ||
            data?.visuals?.avatarImage?.sources ||
            data?.albumOfTrack?.coverArt?.sources ||
            data?.album?.images ||
            data?.album?.coverArt?.sources ||
            data?.coverArt?.sources ||
            entity?.images?.items?.[0]?.sources ||
            entity?.images ||
            entity?.coverArt?.sources
        );
    }

    function artistArrayText(value) {
        if (!value) return "";
        const items = Array.isArray(value) ? value : value.items;
        if (!Array.isArray(items)) return "";
        const names = items
            .map((artist) => firstValue(artist?.name, artist?.profile?.name, artist?.data?.profile?.name, artist?.data?.name))
            .filter(Boolean);
        return names.join(", ");
    }

    function artistText(entity) {
        const data = unwrapEntity(entity);
        const metadata = data?.metadata || entity?.metadata || {};
        return firstValue(
            metadata.artist_name,
            typeof data?.artists === "string" ? data.artists : "",
            artistArrayText(data?.artists),
            artistArrayText(data?.firstArtist),
            artistArrayText(data?.artists?.items),
            artistArrayText(entity?.artists),
            data?.artistName,
            data?.artist_name,
            data?.firstArtist?.items?.[0]?.profile?.name,
            data?.firstArtist?.data?.profile?.name,
            data?.firstArtist?.profile?.name,
            data?.byLine,
            "Unknown artist"
        );
    }

    function ownerText(entity) {
        const data = unwrapEntity(entity);
        return firstValue(
            data?.owner?.display_name,
            data?.owner?.name,
            data?.ownerName,
            data?.metadata?.owner_name,
            data?.ownerV2?.data?.name,
            data?.ownerV2?.data?.username,
            data?.creator?.name,
            data?.profile?.name,
            "BeatBlocks"
        );
    }

    function typenameType(entity) {
        const name = String(entity?.__typename || entity?.data?.__typename || "");
        if (/track/i.test(name)) return "track";
        if (/album|prerelease/i.test(name)) return "album";
        if (/playlist/i.test(name)) return "playlist";
        if (/artist/i.test(name)) return "artist";
        return "";
    }

    function nameFrom(entity, fallback) {
        const data = unwrapEntity(entity);
        return firstValue(
            data?.name,
            data?.title,
            data?.metadata?.name,
            data?.metadata?.title,
            data?.profile?.name,
            data?.trackName,
            data?.albumName,
            entity?.name,
            fallback
        );
    }

    function durationFrom(entity) {
        const data = unwrapEntity(entity);
        return Number(firstValue(
            data?.duration_ms,
            data?.durationMs,
            data?.duration?.totalMilliseconds,
            data?.duration?.milliseconds,
            data?.length,
            data?.metadata?.duration,
            entity?.duration_ms,
            0
        )) || 0;
    }

    function normalizeEntity(entity, forcedType = "") {
        if (!entity || typeof entity !== "object") return null;
        const data = unwrapEntity(entity);
        let uri = firstValue(
            data?.uri,
            data?.metadata?.uri,
            data?.link,
            data?.metadata?.link,
            entity?.uri,
            entity?.metadata?.uri,
            entity?.link,
            entity?.metadata?.link,
            entity?.item?.uri,
            entity?.item?.data?.uri,
            entity?.track?.uri,
            entity?.track?.data?.uri,
            entity?.contextTrack?.uri,
            entity?.contextTrack?.data?.uri,
            entity?.album?.uri,
            entity?.album?.data?.uri,
            entity?.artist?.uri,
            entity?.artist?.data?.uri,
            entity?._uri
        );
        uri = playableUri(uri);
        const parsed = typeFromUri(uri);
        let type = String(firstValue(forcedType, data?.type, entity?.type, typenameType(data), typenameType(entity), parsed.type, "")).toLowerCase();
        if (type === "episode" || type === "show" || type === "user" || type === "folder") return null;
        if (type === "prerelease" || type === "pre_release_album" || type === "ep" || type === "single") type = "album";
        if (!["track", "album", "playlist", "artist"].includes(type) || !uri) return null;

        const name = nameFrom(data, `Unknown ${type}`);
        const subtitle = type === "playlist" ? `Playlist - ${ownerText(data)}` : type === "artist" ? "Artist" : artistText(data);

        return {
            type,
            id: data?.id || entity?.id || parsed.id || "",
            name,
            subtitle,
            uri,
            image_url: imageFrom(data),
            external_url: null,
            duration_ms: type === "track" ? durationFrom(data) : 0
        };
    }

    function normalizePlayerItem(item) {
        if (!item) return null;
        const normalized = normalizeEntity(item, "track");
        if (!normalized) return null;
        normalized.name = firstValue(item.metadata?.title, normalized.name);
        normalized.subtitle = firstValue(item.metadata?.artist_name, normalized.subtitle);
        normalized.duration_ms = Number(Spicetify.Player.data?.duration || item.duration_ms || item.durationMs || normalized.duration_ms || 0);
        return normalized;
    }

    function collectBeatBlocksItems(root, limit = MAX_LIMIT * 4) {
        const result = [];
        const seen = new Set();
        const visited = new WeakSet();

        function visit(value) {
            if (!value || result.length >= limit) return;
            if (Array.isArray(value)) {
                for (const entry of value) visit(entry);
                return;
            }
            if (typeof value !== "object" || visited.has(value)) return;
            visited.add(value);

            const direct = normalizeEntity(value);
            if (direct && !seen.has(direct.uri)) {
                seen.add(direct.uri);
                result.push(direct);
                if (result.length >= limit) return;
            }

            for (const key of [
                "items", "itemsV2", "rows", "children", "contents", "tracks", "tracksV2",
                "albums", "albumsV2", "playlists", "artists", "artistsV2", "data", "results", "topResults", "topResultsV2",
                "nextItems", "nextTracks", "prevTracks", "queue", "featured", "sectionItems",
                "item", "track", "contextTrack", "album", "playlist", "artist",
                "libraryV3"
            ]) {
                if (value[key]) visit(value[key]);
            }
        }

        visit(root);
        return result;
    }

    function makePage(items, offset, limit, total = items.length, warning = "") {
        const start = Math.max(0, offset);
        const end = Math.min(items.length, start + limit);
        const pageItems = items.slice(start, end);
        const nextOffset = end < Math.max(total, items.length) ? end : null;
        return { items: pageItems, next: nextOffset === null ? null : String(nextOffset), total: Math.max(total, items.length), warning };
    }

    function makeAlreadyPaged(items, offset, limit, total, warning = "") {
        const nextOffset = items.length > 0 && offset + items.length < total ? offset + limit : null;
        return { items, next: nextOffset === null ? null : String(nextOffset), total, warning };
    }

    async function sessionAccessToken() {
        const session = Spicetify.Platform?.Session;
        if (!session) return "";

        const getters = [
            () => session.accessToken,
            () => session.accessToken?.accessToken,
            () => session.accessToken?.access_token,
            () => session.token,
            () => session.token?.accessToken,
            () => session.token?.access_token,
            () => session.getAccessToken?.(),
            () => session.getToken?.()
        ];

        for (const getter of getters) {
            try {
                let value = getter();
                if (value && typeof value.then === "function") value = await value;
                const token = firstValue(
                    typeof value === "string" ? value : "",
                    value?.accessToken,
                    value?.access_token,
                    value?.token
                );
                if (token) return token;
            } catch (_) {
                // Try the next token shape.
            }
        }
        return "";
    }

    async function webApi(endpoint, params = {}) {
        const url = new URL(endpoint.startsWith("http") ? endpoint : WEB_API_URL + endpoint.replace(/^\/+/, ""));
        for (const [key, value] of Object.entries(params)) {
            if (value !== undefined && value !== null && value !== "") url.searchParams.set(key, String(value));
        }

        if (Spicetify.CosmosAsync?.get) {
            try {
                return await Spicetify.CosmosAsync.get(url.toString());
            } catch (error) {
                console.warn("[BeatBlocks] Cosmos web request failed", error);
            }
        }

        const token = await sessionAccessToken();
        if (!token) throw new Error("Desktop player session token unavailable");

        const response = await fetch(url.toString(), {
            headers: { Authorization: `Bearer ${token}` }
        });
        if (!response.ok) {
            let body = "";
            try { body = await response.text(); } catch (_) {}
            throw new Error(`BeatBlocks session request failed ${response.status}: ${body.slice(0, 160)}`);
        }
        return await response.json();
    }

    function webPage(root, offset, limit, mapItem, warning = "") {
        const rawItems = Array.isArray(root?.items) ? root.items : [];
        const items = rawItems.map(mapItem).filter(Boolean);
        const total = Number(root?.total || offset + items.length);
        const hasMore = Boolean(root?.next) && items.length > 0 && offset + items.length < total;
        return { items, next: hasMore ? String(offset + items.length) : null, total, warning };
    }

    async function webPlaylists(offset, limit) {
        const root = await webApi("me/playlists", { offset, limit });
        return webPage(root, offset, limit, (item) => normalizeEntity(item, "playlist"));
    }

    async function webSavedAlbums(offset, limit) {
        const root = await webApi("me/albums", { offset, limit });
        return webPage(root, offset, limit, (item) => normalizeEntity(item?.album || item, "album"));
    }

    async function webSavedTracks(offset, limit) {
        const root = await webApi("me/tracks", { offset, limit });
        return webPage(root, offset, limit, (item) => normalizeEntity(item?.track || item, "track"));
    }

    async function webPlaylistTracks(uri, offset, limit) {
        const id = idFromPlayable(uri, "playlist");
        if (!id) return makeAlreadyPaged([], offset, limit, 0, "Playlist id unavailable");
        const root = await webApi(`playlists/${encodeURIComponent(id)}/tracks`, {
            offset,
            limit,
            market: "from_token"
        });
        return webPage(root, offset, limit, (item) => normalizeEntity(item?.track || item, "track"));
    }

    async function cosmosGet(url, body) {
        if (!Spicetify.CosmosAsync) throw new Error("CosmosAsync unavailable");
        if (body === undefined) return await Spicetify.CosmosAsync.get(url);
        return await Spicetify.CosmosAsync.get(url, body);
    }

    async function graphQL(name, variables, waitAttempts = 20) {
        let def = Spicetify.GraphQL?.Definitions?.[name];
        for (let i = 0; !def && i < waitAttempts; i++) {
            await sleep(150);
            def = Spicetify.GraphQL?.Definitions?.[name];
        }
        if (!def || typeof Spicetify.GraphQL?.Request !== "function") {
            throw new Error(`GraphQL definition unavailable: ${name}`);
        }
        return await Spicetify.GraphQL.Request(def, variables, { persistCache: true });
    }

    async function getRootlistItems() {
        for (const body of [
            { policy: { folder: { rows: true, link: true, name: true }, playlist: { link: true, name: true, owner: true, picture: true } } },
            undefined
        ]) {
            try {
                const root = await cosmosGet("sp://core-playlist/v1/rootlist", body);
                const items = collectBeatBlocksItems(root, 1000).filter((item) => item.type === "playlist");
                if (items.length) return items;
            } catch (error) {
                console.warn("[BeatBlocks] rootlist failed", error);
            }
        }
        return [];
    }

    async function rootlistApiPage(offset, limit) {
        const api = Spicetify.Platform?.RootlistAPI;
        if (!api) return null;

        const calls = [
            { paged: true, run: () => api.getContents?.({ offset, limit, flatten: true }) },
            { paged: true, run: () => api.getContents?.({ offset, limit }) },
            { paged: true, run: () => api.getRootlist?.({ offset, limit }) },
            { paged: false, run: () => api.getContents?.() },
            { paged: false, run: () => api.getRootlist?.() },
            { paged: false, run: () => api.getRootlistRows?.() }
        ];

        for (const call of calls) {
            try {
                const response = await call.run();
                const items = collectBeatBlocksItems(response, 1000).filter((item) => item.type === "playlist");
                const total = Number(response?.totalLength || response?.totalCount || response?.unfilteredTotalLength || items.length);
                if (items.length || total) {
                    if (call.paged) return makeAlreadyPaged(items.slice(0, limit), offset, limit, total || offset + items.length);
                    return makePage(items, offset, limit, total || items.length);
                }
            } catch (error) {
                console.warn("[BeatBlocks] RootlistAPI failed", error);
            }
        }

        return null;
    }

    async function libraryContents(filter, offset, limit) {
        const api = Spicetify.Platform?.LibraryAPI;
        if (!api) return null;

        const calls = [
            () => api.getContents?.({ filters: [filter], offset, limit, flatten: true, includeLikedSongs: true, includePreReleases: true }),
            () => api.getContents?.({ filters: [filter], offset, limit }),
            () => api.getContents?.({ offset, limit, flatten: true }),
        ];

        for (const call of calls) {
            try {
                const response = await call();
                const items = collectBeatBlocksItems(response, 500).filter((item) => {
                    if (filter === "Albums") return item.type === "album";
                    if (filter === "Playlists") return item.type === "playlist";
                    return true;
                });
                const total = Number(response?.totalLength || response?.totalCount || response?.unfilteredTotalLength || items.length);
                if (items.length || total) return makeAlreadyPaged(items.slice(0, limit), offset, limit, total || items.length);
            } catch (_) {
                // Try next shape.
            }
        }

        return null;
    }

    async function libraryGraphQL(filter, offset, limit) {
        try {
            const response = await graphQL("libraryV3", {
                filters: [filter],
                order: null,
                textFilter: null,
                features: ["LikedSongs", "Prereleases", "PrereleasesV2"],
                limit,
                offset,
                flatten: true,
                expandedFolders: undefined,
                folderUri: null,
                includeFoldersWhenFlattening: true
            });
            const items = collectBeatBlocksItems(response?.data?.me?.libraryV3 || response, 500).filter((item) => {
                if (filter === "Albums") return item.type === "album";
                if (filter === "Playlists") return item.type === "playlist";
                return true;
            });
            const root = response?.data?.me?.libraryV3 || {};
            const total = Number(root.totalLength || root.totalCount || root.unfilteredTotalLength || items.length);
            if (items.length || total) return makeAlreadyPaged(items.slice(0, limit), offset, limit, total || items.length);
        } catch (error) {
            console.warn("[BeatBlocks] libraryV3 failed", filter, error);
        }
        return null;
    }

    async function getPlaylists(params = {}) {
        const offset = offsetFrom(params);
        const limit = clampLimit(params.limit);
        const viaLibrary = await libraryContents("Playlists", offset, limit) || await libraryGraphQL("Playlists", offset, limit);
        if (viaLibrary?.items?.length) return viaLibrary;
        const viaRootApi = await rootlistApiPage(offset, limit);
        if (viaRootApi?.items?.length) return viaRootApi;
        const all = await getRootlistItems();
        if (all.length) return makePage(all, offset, limit, all.length);
        try {
            return await webPlaylists(offset, limit);
        } catch (error) {
            console.warn("[BeatBlocks] Web playlist fallback failed", error);
            return makeAlreadyPaged([], offset, limit, 0, "No playlists returned by desktop player");
        }
    }

    async function getSavedAlbums(params = {}) {
        const offset = offsetFrom(params);
        const limit = clampLimit(params.limit);
        const viaLibrary = await libraryContents("Albums", offset, limit) || await libraryGraphQL("Albums", offset, limit);
        if (viaLibrary) return viaLibrary;
        try {
            return await webSavedAlbums(offset, limit);
        } catch (error) {
            console.warn("[BeatBlocks] Web albums fallback failed", error);
            return makeAlreadyPaged([], offset, limit, 0, "Albums are unavailable from this desktop player build");
        }
    }

    async function getSavedTracks(params = {}) {
        const offset = offsetFrom(params);
        const limit = clampLimit(params.limit);
        const api = Spicetify.Platform?.LibraryAPI;
        if (api?.getTracks) {
            try {
                const response = await api.getTracks({ offset, limit });
                const items = collectBeatBlocksItems(response, limit * 2).filter((item) => item.type === "track").slice(0, limit);
                const total = Number(response?.totalLength || response?.totalCount || response?.unfilteredTotalLength || offset + items.length);
                if (items.length || total) return makeAlreadyPaged(items, offset, limit, total || offset + items.length);
            } catch (error) {
                console.warn("[BeatBlocks] LibraryAPI.getTracks failed", error);
            }
        }
        try {
            return await webSavedTracks(offset, limit);
        } catch (error) {
            console.warn("[BeatBlocks] Web liked songs fallback failed", error);
            return makeAlreadyPaged([], offset, limit, 0, "Liked songs are unavailable from this desktop player build");
        }
    }

    async function playlistApiTracks(uri, offset, limit) {
        const api = Spicetify.Platform?.PlaylistAPI;
        if (!api || !uri) return null;

        const calls = [
            () => api.getContents?.(uri, { offset, limit }),
            () => api.getContents?.({ uri, offset, limit }),
            () => api.getPlaylistTracks?.(uri, { offset, limit }),
            () => api.getPlaylist?.(uri, { offset, limit }),
            () => api.getPlaylist?.({ uri, offset, limit })
        ];

        for (const call of calls) {
            try {
                const response = await call();
                const items = collectBeatBlocksItems(response, limit * 4)
                    .filter((item) => item.type === "track")
                    .slice(0, limit);
                const total = Number(response?.totalLength || response?.totalCount || response?.unfilteredTotalLength || offset + items.length);
                if (items.length || total) return makeAlreadyPaged(items, offset, limit, total || offset + items.length);
            } catch (error) {
                console.warn("[BeatBlocks] PlaylistAPI tracks failed", error);
            }
        }
        return null;
    }

    async function getPlaylistTracks(params = {}) {
        const offset = offsetFrom(params);
        const limit = clampLimit(params.limit);
        const uri = playableUri(firstValue(params.uri, params.id ? `spotify:playlist:${params.id}` : ""));
        if (!uri) return makeAlreadyPaged([], offset, limit, 0, "Playlist uri unavailable");

        const viaPlatform = await playlistApiTracks(uri, offset, limit);
        if (viaPlatform?.items?.length) return viaPlatform;

        try {
            return await webPlaylistTracks(uri, offset, limit);
        } catch (error) {
            console.warn("[BeatBlocks] Web playlist tracks fallback failed", error);
            return makeAlreadyPaged([], offset, limit, 0, "Playlist tracks are unavailable from this desktop player build");
        }
    }

    async function getQueue() {
        const data = Spicetify.Player.data || {};
        const getters = [
            () => Spicetify.Queue?.nextTracks,
            () => Spicetify.Queue?.queue,
            () => Spicetify.Queue?.tracks,
            () => Spicetify.Queue?.getQueue?.(),
            () => data.nextItems,
            () => data.queue,
            () => data.next_tracks,
            () => data.context?.nextTracks
        ];
        for (const getter of getters) {
            try {
                let value = getter();
                if (value && typeof value.then === "function") value = await value;
                const items = collectBeatBlocksItems(value, MAX_LIMIT).filter((item) => item.type === "track");
                if (items.length) return makeAlreadyPaged(items, 0, MAX_LIMIT, items.length);
            } catch (_) {
                // Try the next queue shape.
            }
        }
        return makeAlreadyPaged([], 0, MAX_LIMIT, 0);
    }

    function getPlaybackState() {
        const data = Spicetify.Player.data || {};
        const track = normalizePlayerItem(data.item);
        let repeatState = "off";
        const repeat = Spicetify.Player.getRepeat?.() ?? 0;
        if (repeat === 1 || repeat === "context") repeatState = "context";
        else if (repeat === 2 || repeat === "track") repeatState = "track";

        return {
            is_playing: Boolean(Spicetify.Player.isPlaying?.()),
            device_name: "Local BeatBlocks",
            progress_ms: Number(Spicetify.Player.getProgress?.() || 0),
            volume_percent: Math.round(Number(Spicetify.Player.getVolume?.() ?? 0.5) * 100),
            shuffle_state: Boolean(Spicetify.Player.getShuffle?.()),
            repeat_state: repeatState,
            track,
            extension_version: EXTENSION_VERSION
        };
    }

    let lastSentStateJson = "";
    function sendState(force = false) {
        try {
            const stateJson = JSON.stringify(getPlaybackState());
            if (!force && stateJson === lastSentStateJson) return;
            lastSentStateJson = stateJson;
            fetch(STATE_URL, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: stateJson
            }).catch(() => {});
        } catch (_) {
            // Minecraft may not be running yet.
        }
    }

    function addPlayerListener(name) {
        try {
            Spicetify.Player.addEventListener(name, () => sendState(false));
        } catch (_) {
            // Event name unavailable in this BeatBlocks build.
        }
    }

    addPlayerListener("songchange");
    addPlayerListener("onplaypause");
    addPlayerListener("onprogress");
    setInterval(() => sendState(true), 1000);

    async function webPlaybackPlay(body) {
        if (Spicetify.CosmosAsync?.put) {
            return await Spicetify.CosmosAsync.put("https://api.spotify.com/v1/me/player/play", body);
        }
        throw new Error("BeatBlocks playback API unavailable");
    }

    async function playUri(uri, contextUri = "") {
        const playable = playableUri(uri);
        const context = playableUri(contextUri);
        if (context && playable) {
            const body = { context_uri: context, offset: { uri: playable } };
            try {
                if (Spicetify.Platform?.PlayerAPI?.play) {
                    return await Spicetify.Platform.PlayerAPI.play({ uri: playable, context_uri: context }, {}, {});
                }
            } catch (error) {
                console.warn("[BeatBlocks] Context track play via PlayerAPI failed", error);
            }
            return await webPlaybackPlay(body);
        }
        if (context) {
            try {
                if (Spicetify.Platform?.PlayerAPI?.play) {
                    return await Spicetify.Platform.PlayerAPI.play({ uri: context }, {}, {});
                }
            } catch (error) {
                console.warn("[BeatBlocks] Context play via PlayerAPI failed", error);
            }
            if (Spicetify.Player.playUri) return await Spicetify.Player.playUri(context);
            return await webPlaybackPlay({ context_uri: context });
        }
        if (!playable) {
            if (Spicetify.Player.play) return await Spicetify.Player.play();
            if (Spicetify.Platform?.PlayerAPI?.resume) return await Spicetify.Platform.PlayerAPI.resume();
            return;
        }
        if (Spicetify.Player.playUri) return await Spicetify.Player.playUri(playable);
        if (Spicetify.Platform?.PlayerAPI?.play) return await Spicetify.Platform.PlayerAPI.play({ uri: playable }, {}, {});
    }

    async function handleCommand(command) {
        const { action, params = {}, req_id } = command;
        let result = null;

        try {
            switch (action) {
                case "play":
                    await playUri(params.uri, params.context_uri);
                    sendState(true);
                    break;
                case "play_context":
                    if (params.shuffle !== undefined) {
                        await Spicetify.Player.setShuffle?.(Boolean(params.shuffle));
                        await sleep(120);
                    }
                    await playUri("", params.uri);
                    sendState(true);
                    break;
                case "pause":
                    if (Spicetify.Player.pause) await Spicetify.Player.pause();
                    else await Spicetify.Platform?.PlayerAPI?.pause?.();
                    sendState(true);
                    break;
                case "next":
                    await Spicetify.Player.next?.();
                    sendState(true);
                    break;
                case "previous":
                    await (Spicetify.Player.back || Spicetify.Player.previous)?.call(Spicetify.Player);
                    sendState(true);
                    break;
                case "set_shuffle":
                    await Spicetify.Player.setShuffle?.(Boolean(params.enabled));
                    sendState(true);
                    break;
                case "set_repeat": {
                    const mode = params.state === "track" ? 2 : params.state === "context" ? 1 : 0;
                    await Spicetify.Player.setRepeat?.(mode);
                    sendState(true);
                    break;
                }
                case "set_volume":
                    await Spicetify.Player.setVolume?.(Math.max(0, Math.min(100, Number(params.volume_percent || 0))) / 100);
                    sendState(true);
                    break;
                case "seek":
                    await Spicetify.Player.seek?.(Math.max(0, Number(params.position_ms || 0)));
                    sendState(true);
                    break;
                case "add_to_queue":
                    if (params.uri) await Spicetify.Platform?.PlayerAPI?.addToQueue?.([{ uri: params.uri }]);
                    sendState(true);
                    break;
                case "toggle_like":
                    await Spicetify.Player.toggleHeart?.();
                    sendState(true);
                    break;
                case "check_liked":
                    result = [Boolean(Spicetify.Player.getHeart?.())];
                    break;
                case "get_playlists":
                    result = await getPlaylists(params);
                    break;
                case "get_saved_albums":
                    result = await getSavedAlbums(params);
                    break;
                case "get_saved_tracks":
                    result = await getSavedTracks(params);
                    break;
                case "get_playlist_tracks":
                    result = await getPlaylistTracks(params);
                    break;
                case "get_queue":
                    result = await getQueue();
                    break;
                default:
                    result = makeAlreadyPaged([], 0, DEFAULT_LIMIT, 0, `Unknown command: ${action}`);
                    break;
            }

            if (req_id) {
                await fetch(RESPONSE_URL, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ req_id, data: result ?? {} })
                }).catch(() => {});
            }
        } catch (error) {
            console.error("[BeatBlocks] Command error:", action, error);
            if (req_id) {
                await fetch(RESPONSE_URL, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ req_id, error: error?.message || "Unknown bridge error" })
                }).catch(() => {});
            }
        }
    }

    async function pollCommands() {
        while (true) {
            try {
                const response = await fetch(COMMANDS_URL, { method: "GET" });
                if (response.ok) {
                    const command = await response.json();
                    if (command?.action) await handleCommand(command);
                    else await sleep(250);
                }
            } catch (_) {
                await sleep(2000);
            }
        }
    }

    sendState(true);
    pollCommands();
    console.log(`[BeatBlocks] Extension v${EXTENSION_VERSION} loaded on localhost:${PORT}`);
})();
