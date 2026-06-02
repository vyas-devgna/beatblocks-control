package com.devgnav.beatblocks.ui;

import com.devgnav.beatblocks.BeatBlocksServices;

import com.devgnav.beatblocks.model.*;
import com.devgnav.beatblocks.spotify.BeatBlocksApiException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class BeatBlocksOverlayScreenBase extends Screen {
    // ── Color Palette ───────────────────────────────────────────────────
    private static final int BG_DARK = 0xF0101010;
    private static final int PANEL_BG = 0xEE0B0B0B;
    private static final int SIDEBAR_BG = 0xEE080808;
    private static final int PLAYER_BG = 0xF0080808;
    private static final int ACCENT = 0xFF1DB954;
    private static final int ACCENT_DIM = 0xFF158C3F;
    private static final int GOLD = 0xFFD8AF2F;
    private static final int GOLD_SHADOW = 0xFF5F4F13;
    private static final int TEXT_MAIN = 0xFFEFEFEF;
    private static final int TEXT_DIM = 0xFFAAAAAA;
    private static final int TEXT_DARK = 0xFF666666;
    private static final int ERROR_COLOR = 0xFFFF7777;
    private static final int BORDER_LIGHT = 0xFF3C3C3C;
    private static final int BORDER_DARK = 0xFF141414;
    private static final int HOVER_BG = 0x33FFFFFF;
    private static final int ACTIVE_BG = 0x4433AA66;
    private static final int PROGRESS_BG = 0xFF333333;

    private static final int SIDEBAR_W = 140;
    private static final int PLAYER_BAR_H = 44;
    private static final int PANEL_EDGE_GAP = 8;
    private static final int PANEL_MIN_W = 360;
    private static final int PANEL_MIN_H = 260;
    private static final int ROW_H = 28;
    private static final int ROW_COVER = 22;
    private static final int CONTEXT_HEADER_H = 78;
    private static final int ICON_TEXTURE_SIZE = 64;
    private static final int PLAYER_ICON = 16;

    private static final Identifier ICON_NOW = icon("beatblocks_icon_now_playing");
    private static final Identifier ICON_PLAYLIST = icon("beatblocks_icon_playlist");
    private static final Identifier ICON_LIKED = icon("beatblocks_icon_liked_songs");
    private static final Identifier ICON_ALBUM = icon("beatblocks_icon_album");
    private static final Identifier ICON_QUEUE = icon("beatblocks_icon_queue");
    private static final Identifier ICON_DIAGNOSTICS = icon("beatblocks_icon_diagnostics");
    private static final Identifier ICON_SETTINGS = icon("beatblocks_icon_settings");
    private static final Identifier ICON_PLAY = icon("beatblocks_icon_play");
    private static final Identifier ICON_PAUSE = icon("beatblocks_icon_pause");
    private static final Identifier ICON_PREVIOUS = icon("beatblocks_icon_previous");
    private static final Identifier ICON_NEXT = icon("beatblocks_icon_next");
    private static final Identifier ICON_SHUFFLE = icon("beatblocks_icon_shuffle");
    private static final Identifier ICON_REPEAT = icon("beatblocks_icon_repeat");
    private static final Identifier ICON_REPEAT_ONE = icon("beatblocks_icon_repeat_one");
    private static final Identifier ICON_VOLUME = icon("beatblocks_icon_volume");
    private static final Identifier ICON_VOLUME_LOW = icon("beatblocks_icon_volume_low");
    private static final Identifier ICON_VOLUME_MUTED = icon("beatblocks_icon_volume_muted");
    private static final Identifier ICON_HEART = icon("beatblocks_icon_heart");

    private final BeatBlocksServices services;

    private Tab tab = Tab.NOW;
    private PlaybackState playback = PlaybackState.inactive();
    private long playbackFetchedAt = 0L;
    private List<BeatBlocksItem> items = new ArrayList<>();
    private List<BeatBlocksItem> queueItems = new ArrayList<>();
    private BeatBlocksItem openedContext = null;
    private Tab contextParentTab = Tab.PLAYLISTS;

    private int selectedIndex = -1;
    private String statusMsg = "";
    private int statusColor = TEXT_DIM;
    private boolean loading = false;
    private boolean loadingMore = false;
    private String nextPageToken = null;
    private int listRequestSerial = 0;
    private long lastPoll = 0L;
    private long lastClickAt = 0L;
    private String loadedCoverUrl = null;
    private byte[] currentCoverPng = null;
    private final Map<String, byte[]> itemCoverCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > 256;
        }
    };
    private final Set<String> itemCoverLoading = new HashSet<>();

    private int mainScroll = 0;
    private String lastTrackId = "";
    private int detailBackX, detailBackY, detailBackW, detailBackH;
    private int detailPlayX, detailPlayY, detailPlayW, detailPlayH;
    private int detailShuffleX, detailShuffleY, detailShuffleW, detailShuffleH;
    private int detailQueueX, detailQueueY, detailQueueW, detailQueueH;
    private int queueListX, queueListY, queueListW, queueListH;
    private int queueSelectedIndex = -1;
    private long lastQueueClickAt = 0L;
    private int shuffleX, shuffleY, shuffleW, shuffleH;
    private int prevX, prevY, prevW, prevH;
    private int playX, playY, playW, playH;
    private int nextX, nextY, nextW, nextH;
    private int repeatX, repeatY, repeatW, repeatH;
    private int heartX, heartY, heartW, heartH;
    private int queueButtonX, queueButtonY, queueButtonW, queueButtonH;
    private int volumeIconX, volumeIconY, volumeIconW, volumeIconH;
    private int volumeSliderX, volumeSliderY, volumeSliderW, volumeSliderH;
    private boolean volumeDragging = false;
    private int volumeDragValue = -1;
    private int volumeOverrideValue = -1;
    private long volumeOverrideUntil = 0L;
    private int volumeBeforeMute = 50;
    private long lastVolumeSendAt = 0L;

    public BeatBlocksOverlayScreenBase(BeatBlocksServices services) {
        super(Text.literal("BeatBlocks"));
        this.services = services;
    }

    @Override
    protected void init() {
        refreshAll();
    }

    // ── Tick ─────────────────────────────────────────────────────────────

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastPoll > services.config().apiPollSeconds * 1000L) {
            lastPoll = now;
            refreshPlayback();
        }

        // Fetch queue once per song change, drastically reducing rate limits
        BeatBlocksItem currentTrack = playback.track();
        String currentTrackId = currentTrack == null ? "" : currentTrack.id();
        if (!currentTrackId.equals(lastTrackId)) {
            lastTrackId = currentTrackId;
            refreshQueue();
        }
        loadCoverIfNeeded();
    }

    // ── Render ───────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Translucent background — game visible behind
        ctx.fill(0, 0, width, height, 0x88000000);

        int panelW = panelW();
        int panelH = panelH();
        int panelX = panelX();
        int panelY = panelY();

        // Main panel background
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0101010);
        // Panel border
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, BORDER_LIGHT);
        ctx.fill(panelX, panelY, panelX + 1, panelY + panelH, BORDER_LIGHT);
        ctx.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, BORDER_DARK);
        ctx.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, BORDER_DARK);

        // Sidebar (inside panel)
        int sideX = panelX;
        int sideW = SIDEBAR_W;
        renderSidebar(ctx, mx, my, sideX, panelY + 16, sideW, panelH - 16 - PLAYER_BAR_H);

        // Main content area (inside panel)
        int mainX = panelX + SIDEBAR_W;
        int mainW = panelW - SIDEBAR_W;
        int contentTop = panelY;
        int contentH = panelH - PLAYER_BAR_H;
        drawPanel(ctx, mainX, contentTop, mainW, contentH, false);
        renderMainContent(ctx, mainX + 8, contentTop + 8, mainW - 16, contentH - 16, mx, my);

        // Bottom player bar (inside panel)
        renderPlayerBar(ctx, mx, my, panelX, panelY + panelH - PLAYER_BAR_H, panelW);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Do absolutely nothing to prevent the default Minecraft screen blur and gradient from rendering
    }

    // ── Sidebar ──────────────────────────────────────────────────────────

    private void renderSidebar(DrawContext ctx, int mx, int my, int sx, int sy, int sw, int sh) {
        ctx.fill(sx, sy - 16, sx + sw, sy + sh, SIDEBAR_BG);
        // Right border
        ctx.fill(sx + sw - 1, sy - 16, sx + sw, sy + sh, BORDER_DARK);

        // Title
        ctx.drawTextWithShadow(textRenderer, Text.literal("♫ BeatBlocks"), sx + 10, sy - 12, GOLD);
        ctx.fill(sx + 8, sy - 2, sx + sw - 8, sy - 1, BORDER_DARK);

        Tab[] navTabs = { Tab.NOW, Tab.PLAYLISTS, Tab.LIKED, Tab.ALBUMS, Tab.QUEUE, Tab.DIAGNOSTICS, Tab.SETTINGS };
        String[] navLabels = { "Now Playing", "Playlists", "Liked Songs", "Albums", "Queue", "Diagnostics", "Settings" };
        Identifier[] navIcons = { ICON_NOW, ICON_PLAYLIST, ICON_LIKED, ICON_ALBUM, ICON_QUEUE, ICON_DIAGNOSTICS, ICON_SETTINGS };

        int itemY = sy;
        for (int i = 0; i < navTabs.length; i++) {
            boolean active = isSidebarTabActive(navTabs[i]);
            boolean hovered = mx >= sx + 4 && mx < sx + sw - 4 && my >= itemY && my < itemY + 16;

            if (active) {
                ctx.fill(sx + 4, itemY, sx + sw - 4, itemY + 15, ACTIVE_BG);
                ctx.fill(sx + 4, itemY, sx + 6, itemY + 15, ACCENT); // Active indicator bar
            } else if (hovered) {
                ctx.fill(sx + 4, itemY, sx + sw - 4, itemY + 15, HOVER_BG);
            }

            drawIcon(ctx, navIcons[i], sx + 12, itemY + 3, 10);
            ctx.drawTextWithShadow(textRenderer, Text.literal(navLabels[i]), sx + 26, itemY + 4, active ? TEXT_MAIN : TEXT_DIM);
            itemY += 17;
        }
    }

    private boolean isSidebarTabActive(Tab navTab) {
        if (tab == navTab) return true;
        return tab == Tab.PLAYLIST_DETAIL && navTab == Tab.PLAYLISTS;
    }

    // ── Main Content ─────────────────────────────────────────────────────

    private void renderMainContent(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        switch (tab) {
            case NOW -> renderNowPlaying(ctx, x, y, w, h, mx, my);
            case PLAYLIST_DETAIL -> renderPlaylistDetail(ctx, x, y, w, h, mx, my);
            case DIAGNOSTICS -> renderDiagnostics(ctx, x, y, w, h);
            default -> renderListTab(ctx, x, y, w, h, mx, my);
        }
    }

    private void renderNowPlaying(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        BeatBlocksItem track = playback.track();
        if (track == null) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("No track playing"), x + 10, y + 30, TEXT_DIM);
            ctx.drawTextWithShadow(textRenderer, Text.literal("Open BeatBlocks and play something"), x + 10, y + 44, TEXT_DARK);
            ctx.drawTextWithShadow(textRenderer, Text.literal("Make sure the Spicetify extension is loaded"), x + 10, y + 58, TEXT_DARK);
            return;
        }

        int halfW = (w - 16) / 2;

        // ── Left: Now Playing Card ──
        drawPanel(ctx, x, y, halfW, h, false);

        int coverSize = Math.min(halfW - 24, h - 100);
        coverSize = Math.max(32, Math.min(coverSize, 256));

        int coverX = x + (halfW - coverSize) / 2;
        int coverY = y + 16;

        if (currentCoverPng != null) {
            renderPixelCover(ctx, currentCoverPng, coverX, coverY, coverSize);
        } else {
            renderCoverPlaceholder(ctx, coverX, coverY, coverSize);
        }

        int infoY = coverY + coverSize + 10;
        ctx.drawCenteredTextWithShadow(textRenderer, trimText(track.name(), halfW / 6), x + halfW / 2, infoY, TEXT_MAIN);
        ctx.drawCenteredTextWithShadow(textRenderer, trimText(track.subtitle(), halfW / 6), x + halfW / 2, infoY + 12, TEXT_DIM);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(playback.deviceName()), x + halfW / 2, infoY + 26, ACCENT_DIM);

        // Progress bar
        int barX = x + 12;
        int barY = infoY + 42;
        int barW = halfW - 24;
        if (barW > 20 && track.durationMs() > 0) {
            ctx.fill(barX, barY, barX + barW, barY + 3, PROGRESS_BG);
            int progress = currentProgressMs();
            int filled = Math.min(barW, Math.max(0, (int) (barW * (progress / (double) track.durationMs()))));
            ctx.fill(barX, barY, barX + filled, barY + 3, ACCENT);
            // Time labels
            String timeStr = formatMs(progress) + " / " + formatMs(track.durationMs());
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(timeStr), x + halfW / 2, barY + 6, TEXT_DARK);
        }

        // ── Right: Queue ──
        int qx = x + halfW + 16;
        int qw = w - halfW - 16;
        drawPanel(ctx, qx, y, qw, h, false);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Up Next"), qx + 8, y + 8, GOLD);
        ctx.fill(qx + 8, y + 20, qx + qw - 8, y + 21, BORDER_DARK);

        if (queueItems.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Queue is empty"), qx + 8, y + 28, TEXT_DARK);
        } else {
            int qy = y + 26;
            int maxRows = Math.max(1, (h - 40) / ROW_H);
            queueListX = qx + 4;
            queueListY = qy;
            queueListW = qw - 8;
            queueListH = Math.min(queueItems.size(), maxRows) * ROW_H;
            for (int i = 0; i < Math.min(queueItems.size(), maxRows); i++) {
                BeatBlocksItem item = queueItems.get(i);
                boolean selected = i == queueSelectedIndex;
                boolean hovered = mx >= queueListX && mx < queueListX + queueListW && my >= qy && my < qy + ROW_H;
                if (selected) {
                    ctx.fill(queueListX, qy, queueListX + queueListW, qy + ROW_H - 1, ACTIVE_BG);
                } else if (hovered) {
                    ctx.fill(queueListX, qy, queueListX + queueListW, qy + ROW_H - 1, HOVER_BG);
                }
                ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(i + 1)), qx + 8, qy + 9, TEXT_DARK);
                renderItemCover(ctx, item, qx + 22, qy + 3, ROW_COVER);
                ctx.drawTextWithShadow(textRenderer, trimText(item.name(), (qw - 72) / 6), qx + 50, qy + 4, TEXT_MAIN);
                ctx.drawTextWithShadow(textRenderer, trimText(item.subtitle(), (qw - 72) / 6), qx + 50, qy + 15, TEXT_DIM);
                qy += ROW_H;
            }
        }
    }

    private void renderListTab(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        String header = tab.label + (loading ? " • Loading..." : items.isEmpty() ? " • Empty" : " • " + items.size() + " items");
        ctx.drawTextWithShadow(textRenderer, Text.literal(header), x + 6, y + 6, GOLD);
        ctx.fill(x + 6, y + 18, x + w - 6, y + 19, BORDER_DARK);

        renderItemList(ctx, x, y + 22, w, h - 30, mx, my);
    }

    private void renderPlaylistDetail(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        BeatBlocksItem playlist = openedContext;
        if (playlist == null) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("No playlist selected"), x + 10, y + 10, TEXT_DARK);
            return;
        }

        int cover = Math.min(56, Math.max(42, CONTEXT_HEADER_H - 22));
        renderItemCover(ctx, playlist, x + 8, y + 8, cover);

        int titleX = x + cover + 18;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Playlist"), titleX, y + 6, GOLD);
        ctx.drawTextWithShadow(textRenderer, trimText(playlist.name(), Math.max(12, (w - cover - 150) / 6)), titleX, y + 20, TEXT_MAIN);
        ctx.drawTextWithShadow(textRenderer, trimText(playlist.subtitle(), Math.max(12, (w - cover - 150) / 6)), titleX, y + 32, TEXT_DIM);

        int buttonY = y + 52;
        detailBackX = titleX;
        detailBackY = buttonY;
        detailBackW = 34;
        detailBackH = 16;
        drawActionButton(ctx, detailBackX, detailBackY, detailBackW, detailBackH, "Back", mx, my, TEXT_DIM);

        detailPlayX = detailBackX + detailBackW + 6;
        detailPlayY = buttonY;
        detailPlayW = 34;
        detailPlayH = 16;
        drawActionButton(ctx, detailPlayX, detailPlayY, detailPlayW, detailPlayH, "Play", mx, my, ACCENT);

        detailShuffleX = detailPlayX + detailPlayW + 6;
        detailShuffleY = buttonY;
        detailShuffleW = 48;
        detailShuffleH = 16;
        drawActionButton(ctx, detailShuffleX, detailShuffleY, detailShuffleW, detailShuffleH, "Shuffle", mx, my, GOLD);

        detailQueueX = detailShuffleX + detailShuffleW + 6;
        detailQueueY = buttonY;
        detailQueueW = 44;
        detailQueueH = 16;
        drawActionButton(ctx, detailQueueX, detailQueueY, detailQueueW, detailQueueH, "Queue", mx, my, selectedIndex >= 0 ? TEXT_MAIN : TEXT_DARK);

        String count = loading ? "Loading tracks..." : items.isEmpty() ? "No tracks loaded" : items.size() + " tracks";
        ctx.drawTextWithShadow(textRenderer, Text.literal(count), x + w - textRenderer.getWidth(count) - 8, y + 10, TEXT_DARK);

        ctx.fill(x + 6, y + CONTEXT_HEADER_H - 4, x + w - 6, y + CONTEXT_HEADER_H - 3, BORDER_DARK);
        renderItemList(ctx, x, y + CONTEXT_HEADER_H, w, h - CONTEXT_HEADER_H, mx, my);
    }

    private void drawActionButton(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, int color) {
        boolean hovered = mx >= x && mx < x + w && my >= y && my < y + h;
        ctx.fill(x, y, x + w, y + h, hovered ? HOVER_BG : 0x11FFFFFF);
        ctx.fill(x, y, x + w, y + 1, BORDER_LIGHT);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER_DARK);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + w / 2, y + 4, color);
    }

    private void renderItemList(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        clampMainScroll();

        if (items.isEmpty() && loading) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Loading..."), x + 10, y + 10, TEXT_DIM);
            return;
        }

        if (items.isEmpty() && !loading) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("No items found"), x + 10, y + 10, TEXT_DARK);
            if (!statusMsg.isEmpty()) {
                ctx.drawTextWithShadow(textRenderer, Text.literal(statusMsg), x + 10, y + 24, statusColor);
            }
            return;
        }

        int rowH = ROW_H;
        boolean showFooter = loadingMore || nextPageToken != null;
        int footerH = showFooter ? 12 : 0;
        int maxRows = Math.max(1, (h - footerH) / rowH);
        int startIdx = Math.min(items.size(), mainScroll);
        int rowY = y;

        for (int i = startIdx; i < Math.min(items.size(), startIdx + maxRows); i++) {
            BeatBlocksItem item = items.get(i);
            boolean selected = (i == selectedIndex);
            boolean hovered = mx >= x + 4 && mx < x + w - 4 && my >= rowY && my < rowY + rowH;

            if (selected) {
                ctx.fill(x + 4, rowY, x + w - 8, rowY + rowH - 1, ACTIVE_BG);
            } else if (hovered) {
                ctx.fill(x + 4, rowY, x + w - 8, rowY + rowH - 1, HOVER_BG);
            }

            // Type badge
            int badgeColor = switch (item.type()) {
                case TRACK -> ACCENT;
                case ALBUM -> 0xFF4488CC;
                case PLAYLIST -> GOLD;
                case ARTIST -> 0xFFCC66FF;
            };
            renderItemCover(ctx, item, x + 8, rowY + 3, ROW_COVER);
            ctx.drawTextWithShadow(textRenderer, Text.literal(item.kindLabel()), x + 36, rowY + 9, badgeColor);

            // Name and subtitle
            ctx.drawTextWithShadow(textRenderer, trimText(item.name(), (w - 122) / 6), x + 86, rowY + 4, TEXT_MAIN);
            ctx.drawTextWithShadow(textRenderer, trimText(item.subtitle(), (w - 122) / 6), x + 86, rowY + 15, TEXT_DIM);

            rowY += rowH;
        }

        if (showFooter) {
            String footer = loadingMore ? "Loading more..." : "Scroll for more";
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(footer), x + w / 2, y + h - 10, loadingMore ? TEXT_DIM : TEXT_DARK);
        }

        // Scrollbar
        if (items.size() > maxRows) {
            int trackX = x + w - 6;
            int trackH = h;
            ctx.fill(trackX, y, trackX + 3, y + trackH, 0xFF141414);
            int handleH = Math.max(8, trackH * maxRows / items.size());
            int maxOff = Math.max(1, items.size() - maxRows);
            int handleY = y + (int) ((trackH - handleH) * ((double) mainScroll / maxOff));
            ctx.fill(trackX, handleY, trackX + 3, handleY + handleH, TEXT_DARK);
        }

        maybeLoadMore();
    }

    private void renderDiagnostics(DrawContext ctx, int x, int y, int w, int h) {
        ctx.drawTextWithShadow(textRenderer, Text.literal("Diagnostics"), x + 6, y + 6, GOLD);
        ctx.fill(x + 6, y + 18, x + w - 6, y + 19, BORDER_DARK);

        BridgeDiagnostics diag = services.api().getDiagnostics();
        int dy = y + 26;
        int labelX = x + 10;
        int valueX = x + 160;

        drawDiagRow(ctx, labelX, valueX, dy, "Bridge Server", diag.bridgeRunning() ? "Running" : "Stopped", diag.bridgeRunning() ? ACCENT : ERROR_COLOR); dy += 14;
        drawDiagRow(ctx, labelX, valueX, dy, "Extension Connected", diag.extensionConnected() ? "Yes" : "No", diag.extensionConnected() ? ACCENT : ERROR_COLOR); dy += 14;
        drawDiagRow(ctx, labelX, valueX, dy, "Bridge Port", String.valueOf(diag.port()), TEXT_MAIN); dy += 14;
        drawDiagRow(ctx, labelX, valueX, dy, "Mod Version", diag.modVersion(), TEXT_MAIN); dy += 14;
        drawDiagRow(ctx, labelX, valueX, dy, "Extension Version", diag.extensionVersion().isEmpty() ? "Unknown" : diag.extensionVersion(), TEXT_MAIN); dy += 14;

        long ago = diag.lastHeartbeatAt() > 0 ? (System.currentTimeMillis() - diag.lastHeartbeatAt()) / 1000 : -1;
        String heartbeat = ago < 0 ? "Never" : ago + "s ago";
        int heartColor = ago >= 0 && ago < 5 ? ACCENT : ago >= 0 && ago < 15 ? GOLD : ERROR_COLOR;
        drawDiagRow(ctx, labelX, valueX, dy, "Last Heartbeat", heartbeat, heartColor); dy += 14;

        drawDiagRow(ctx, labelX, valueX, dy, "Messages Received", String.valueOf(diag.connectionCount()), TEXT_MAIN); dy += 14;
        drawDiagRow(ctx, labelX, valueX, dy, "Status", diag.statusSummary(), diag.isHealthy() ? ACCENT : GOLD); dy += 14;

        if (!diag.lastError().isEmpty()) {
            drawDiagRow(ctx, labelX, valueX, dy, "Last Error", diag.lastError(), ERROR_COLOR); dy += 14;
        }

        // Setup hints
        dy += 10;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Setup Hints"), labelX, dy, TEXT_DARK); dy += 14;

        if (!diag.bridgeRunning()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("• Port " + diag.port() + " may be in use. Check config."), labelX, dy, TEXT_DIM); dy += 12;
        }
        if (!diag.extensionConnected()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("• Ensure BeatBlocks is open with Spicetify applied"), labelX, dy, TEXT_DIM); dy += 12;
            ctx.drawTextWithShadow(textRenderer, Text.literal("• Run: spicetify config extensions beatblocks-api.js"), labelX, dy, TEXT_DIM); dy += 12;
            ctx.drawTextWithShadow(textRenderer, Text.literal("• Run: spicetify apply"), labelX, dy, TEXT_DIM);
        }
    }

    private void drawDiagRow(DrawContext ctx, int lx, int vx, int y, String label, String value, int color) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), lx, y, TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, Text.literal(value), vx, y, color);
    }

    // ── Player Bar ───────────────────────────────────────────────────────

    private void renderPlayerBar(DrawContext ctx, int mx, int my, int barX, int barY, int barW) {
        ctx.fill(barX, barY, barX + barW, barY + PLAYER_BAR_H, PLAYER_BG);
        ctx.fill(barX, barY, barX + barW, barY + 1, BORDER_DARK);

        BeatBlocksItem track = playback.track();
        int shownVolume = displayedVolume();
        if (!volumeDragging && playback.volumePercent() > 0) volumeBeforeMute = playback.volumePercent();

        // Progress bar (full panel width, thin, at the very top of player bar)
        if (track != null && track.durationMs() > 0) {
            int progress = currentProgressMs();
            ctx.fill(barX, barY + 1, barX + barW, barY + 3, PROGRESS_BG);
            int filled = Math.min(barW, (int) (barW * (progress / (double) track.durationMs())));
            ctx.fill(barX, barY + 1, barX + filled, barY + 3, ACCENT);
        }

        // Left section: cover + track info
        int lx = barX + 8;
        int ly = barY + 6;
        if (track != null) {
            // Mini cover
            int miniCoverSize = PLAYER_BAR_H - 12;
            if (currentCoverPng != null) {
                renderPixelCover(ctx, currentCoverPng, lx, ly, miniCoverSize);
            } else {
                ctx.fill(lx, ly, lx + miniCoverSize, ly + miniCoverSize, 0xFF141414);
            }
            lx += miniCoverSize + 6;
            ctx.drawTextWithShadow(textRenderer, trimText(track.name(), 30), lx, ly + 4, TEXT_MAIN);
            ctx.drawTextWithShadow(textRenderer, trimText(track.subtitle(), 36), lx, ly + 16, TEXT_DIM);
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("No track"), lx, ly + 10, TEXT_DARK);
        }

        // Center/right: measured controls. Lower-priority buttons collapse first on narrow GUI scales.
        int buttonY = barY + 12;
        int rightReserved = Math.min(178, Math.max(108, barW / 3));
        int controlsLeft = barX + Math.min(170, Math.max(96, barW / 4));
        int controlsRight = barX + barW - rightReserved - 8;
        int centerX = (controlsLeft + controlsRight) / 2;
        boolean compactControls = controlsRight - controlsLeft < 170;
        boolean tinyControls = controlsRight - controlsLeft < 138;

        shuffleX = compactControls ? 0 : centerX - 80; shuffleY = buttonY + 2; shuffleW = compactControls ? 0 : 18; shuffleH = compactControls ? 0 : 18;
        prevX = centerX - 52; prevY = buttonY + 2; prevW = 18; prevH = 18;
        playX = centerX - 13; playY = buttonY; playW = 24; playH = 24;
        nextX = centerX + 28; nextY = buttonY + 2; nextW = 18; nextH = 18;
        repeatX = compactControls ? 0 : centerX + 58; repeatY = buttonY + 2; repeatW = compactControls ? 0 : 18; repeatH = compactControls ? 0 : 18;
        heartX = tinyControls ? 0 : controlsRight - 46; heartY = buttonY + 2; heartW = tinyControls ? 0 : 18; heartH = tinyControls ? 0 : 18;
        queueButtonX = tinyControls ? 0 : controlsRight - 20; queueButtonY = buttonY + 2; queueButtonW = tinyControls ? 0 : 18; queueButtonH = tinyControls ? 0 : 18;

        drawIconButton(ctx, ICON_SHUFFLE, shuffleX, shuffleY, shuffleW, shuffleH, mx, my, playback.shuffle());
        drawIconButton(ctx, ICON_PREVIOUS, prevX, prevY, prevW, prevH, mx, my, false);
        drawIconButton(ctx, playback.playing() ? ICON_PAUSE : ICON_PLAY, playX, playY, playW, playH, mx, my, playback.playing());
        drawIconButton(ctx, ICON_NEXT, nextX, nextY, nextW, nextH, mx, my, false);
        boolean repeatActive = !"off".equalsIgnoreCase(playback.repeatState());
        drawIconButton(ctx, "track".equalsIgnoreCase(playback.repeatState()) ? ICON_REPEAT_ONE : ICON_REPEAT, repeatX, repeatY, repeatW, repeatH, mx, my, repeatActive);
        drawIconButton(ctx, ICON_HEART, heartX, heartY, heartW, heartH, mx, my, false);
        drawIconButton(ctx, ICON_QUEUE, queueButtonX, queueButtonY, queueButtonW, queueButtonH, mx, my, tab == Tab.QUEUE);

        // Right: responsive volume slider + time
        int rx = barX + barW - rightReserved;
        volumeIconX = rx;
        volumeIconY = barY + 13;
        volumeIconW = 18;
        volumeIconH = 18;
        Identifier volumeIcon = shownVolume == 0 ? ICON_VOLUME_MUTED : shownVolume < 45 ? ICON_VOLUME_LOW : ICON_VOLUME;
        drawIconButton(ctx, volumeIcon, volumeIconX, volumeIconY, volumeIconW, volumeIconH, mx, my, shownVolume == 0);

        volumeSliderX = rx + 24;
        volumeSliderY = barY + 19;
        volumeSliderW = Math.max(42, Math.min(86, barX + barW - volumeSliderX - 46));
        volumeSliderH = 10;
        renderVolumeSlider(ctx, shownVolume, mx, my);

        String volText = shownVolume + "%";
        ctx.drawTextWithShadow(textRenderer, Text.literal(volText), volumeSliderX + volumeSliderW + 8, barY + 16, TEXT_DIM);

        if (track != null && track.durationMs() > 0) {
            String timeStr = formatMs(currentProgressMs()) + " / " + formatMs(track.durationMs());
            int timeW = textRenderer.getWidth(timeStr);
            ctx.drawTextWithShadow(textRenderer, Text.literal(timeStr), Math.max(rx, barX + barW - timeW - 8), barY + 30, TEXT_DARK);
        }

        // Status message (bottom-right)
        if (!statusMsg.isEmpty()) {
            int statusW = textRenderer.getWidth(statusMsg);
            int maxStatusX = Math.max(barX + 8, rx - statusW - 12);
            ctx.drawTextWithShadow(textRenderer, Text.literal(statusMsg), maxStatusX, barY + PLAYER_BAR_H - 12, statusColor);
        }
    }

    // ── Input ────────────────────────────────────────────────────────────

    protected boolean mouseClickedImpl(double mx, double my, int button) {
        if (button != 0) return true; // consume all mouse buttons

        int panelW = panelW();
        int panelH = panelH();
        int panelX = panelX();
        int panelY = panelY();

        // Sidebar nav clicks
        int sideX = panelX;
        int sideW = SIDEBAR_W;
        int sideTop = panelY + 16;
        int barY = panelY + panelH - PLAYER_BAR_H;
        if (mx >= sideX && mx < sideX + sideW && my >= panelY && my < barY) {
            Tab[] navTabs = { Tab.NOW, Tab.PLAYLISTS, Tab.LIKED, Tab.ALBUMS, Tab.QUEUE, Tab.DIAGNOSTICS, Tab.SETTINGS };
            int itemY = sideTop;
            for (Tab navTab : navTabs) {
                if (my >= itemY && my < itemY + 16) {
                    switchTab(navTab);
                    return true;
                }
                itemY += 17;
            }
            return true;
        }

        // Player bar controls
        if (my >= barY && my < panelY + panelH) {
            // Seek via progress bar click
            if (my >= barY && my <= barY + 4) {
                BeatBlocksItem track = playback.track();
                if (track != null && track.durationMs() > 0) {
                    double ratio = (mx - panelX) / (double) panelW;
                    int seekPos = (int) (Math.max(0, Math.min(1, ratio)) * track.durationMs());
                    fireAction(services.api().seek(seekPos));
                    return true;
                }
            }

            if (hit(mx, my, shuffleX, shuffleY, shuffleW, shuffleH)) {
                fireAction(services.api().setShuffle(!playback.shuffle()));
                return true;
            }
            if (hit(mx, my, prevX, prevY, prevW, prevH)) {
                fireAction(services.api().previous());
                return true;
            }
            if (hit(mx, my, playX, playY, playW, playH)) {
                togglePlayPause();
                return true;
            }
            if (hit(mx, my, nextX, nextY, nextW, nextH)) {
                fireAction(services.api().next());
                return true;
            }
            if (hit(mx, my, repeatX, repeatY, repeatW, repeatH)) {
                fireAction(services.api().setRepeat(nextRepeatState()));
                return true;
            }
            if (hit(mx, my, heartX, heartY, heartW, heartH)) {
                fireAction(services.api().toggleLike());
                return true;
            }
            if (hit(mx, my, queueButtonX, queueButtonY, queueButtonW, queueButtonH)) {
                switchTab(Tab.QUEUE);
                return true;
            }
            if (hit(mx, my, volumeIconX, volumeIconY, volumeIconW, volumeIconH)) {
                toggleMute();
                return true;
            }
            if (hit(mx, my, volumeSliderX - 4, volumeSliderY - 5, volumeSliderW + 8, volumeSliderH + 10)) {
                volumeDragging = true;
                updateVolumeFromMouse(mx, true);
                return true;
            }
            return true;
        }

        int mainX = panelX + SIDEBAR_W;

        if (tab == Tab.NOW && hit(mx, my, queueListX, queueListY, queueListW, queueListH)) {
            int idx = (int) ((my - queueListY) / ROW_H);
            if (idx >= 0 && idx < queueItems.size()) {
                long now = System.currentTimeMillis();
                boolean doubleClick = queueSelectedIndex == idx && now - lastQueueClickAt < 350;
                queueSelectedIndex = idx;
                lastQueueClickAt = now;
                if (doubleClick) {
                    BeatBlocksItem item = queueItems.get(idx);
                    fireAction(services.api().playItem(item));
                    setStatus("Playing from queue: " + item.name(), ACCENT);
                }
                return true;
            }
        }

        // Playlist detail header controls
        if (tab == Tab.PLAYLIST_DETAIL && mx >= mainX && mx < panelX + panelW) {
            if (hit(mx, my, detailBackX, detailBackY, detailBackW, detailBackH)) {
                returnToContextParent();
                return true;
            }
            if (hit(mx, my, detailPlayX, detailPlayY, detailPlayW, detailPlayH)) {
                playOpenedContext(false);
                return true;
            }
            if (hit(mx, my, detailShuffleX, detailShuffleY, detailShuffleW, detailShuffleH)) {
                playOpenedContext(true);
                return true;
            }
            if (hit(mx, my, detailQueueX, detailQueueY, detailQueueW, detailQueueH)) {
                addSelectedToQueue();
                return true;
            }
        }

        // Main content list clicks
        if (tab != Tab.NOW && tab != Tab.DIAGNOSTICS && mx >= mainX && mx < panelX + panelW) {
            int listY = currentListY();
            int listBottom = listY + currentListHeight();
            if (my < listY || my >= listBottom) return true;
            int rowH = ROW_H;
            int idx = (int) ((my - listY) / rowH) + mainScroll;
            if (idx >= 0 && idx < items.size()) {
                long now = System.currentTimeMillis();
                boolean doubleClick = selectedIndex == idx && now - lastClickAt < 350;
                selectedIndex = idx;
                lastClickAt = now;
                BeatBlocksItem clicked = items.get(idx);
                if (clicked.type() == BeatBlocksItemType.PLAYLIST) {
                    openPlaylist(clicked);
                    return true;
                }
                if (doubleClick) playSelected();
                return true;
            }
        }

        return true; // consume all clicks
    }

    protected boolean mouseDraggedImpl(double mx, double my, int button, double dx, double dy) {
        if (button == 0 && volumeDragging) {
            updateVolumeFromMouse(mx, false);
            return true;
        }
        return true;
    }

    protected boolean mouseReleasedImpl(double mx, double my, int button) {
        if (button == 0 && volumeDragging) {
            updateVolumeFromMouse(mx, true);
            volumeDragging = false;
            return true;
        }
        return true;
    }

    protected boolean mouseScrolledImpl(double mx, double my, double hAmount, double vAmount) {
        int delta = (int) Math.round(vAmount);
        if (delta == 0 && vAmount != 0) delta = vAmount > 0 ? 1 : -1;
        int panelX = panelX();
        int panelY = panelY();
        int panelW = panelW();
        int panelH = panelH();
        if (mx < panelX || mx >= panelX + panelW || my < panelY || my >= panelY + panelH) {
            return true;
        }
        int barY = panelY + panelH - PLAYER_BAR_H;
        if (my >= barY && hit(mx, my, volumeIconX - 2, volumeIconY - 2, volumeSliderX + volumeSliderW - volumeIconX + 8, Math.max(volumeIconH, volumeSliderH) + 8)) {
            int value = Math.max(0, Math.min(100, displayedVolume() + (delta > 0 ? 5 : -5)));
            volumeDragValue = value;
            sendVolume(value, true);
            return true;
        }
        if (mx < panelX + SIDEBAR_W) return true;
        mainScroll = Math.max(0, mainScroll - delta);
        clampMainScroll();
        maybeLoadMore();
        return true;
    }

    protected boolean keyPressedImpl(int keyCode, int scanCode, int modifiers) {
        // ESC closes
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (tab == Tab.PLAYLIST_DETAIL && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            returnToContextParent();
            return true;
        }

        // Navigation
        if (keyCode == GLFW.GLFW_KEY_UP) {
            if (!items.isEmpty()) selectedIndex = selectedIndex <= 0 ? items.size() - 1 : selectedIndex - 1;
            ensureSelectedVisible();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            if (!items.isEmpty()) selectedIndex = selectedIndex >= items.size() - 1 ? 0 : selectedIndex + 1;
            ensureSelectedVisible();
            maybeLoadMore();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            playSelected();
            return true;
        }
        if (tab == Tab.PLAYLIST_DETAIL && keyCode == GLFW.GLFW_KEY_Q) {
            addSelectedToQueue();
            return true;
        }
        if (tab == Tab.PLAYLIST_DETAIL && keyCode == GLFW.GLFW_KEY_P) {
            playOpenedContext(false);
            return true;
        }
        if (tab == Tab.PLAYLIST_DETAIL && keyCode == GLFW.GLFW_KEY_S) {
            playOpenedContext(true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F5) {
            refreshAll();
            return true;
        }

        // Consume ALL keys to prevent gameplay leakage
        return true;
    }

    protected boolean charTypedImpl(int codePoint, int modifiers) {
        return true; // consume ALL char input
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void refreshAll() {
        refreshPlayback();
        refreshQueue();
        if (isPageableTab(tab)) loadTab(tab, false);
        else if (tab == Tab.QUEUE) loadQueue();
    }

    private void refreshPlayback() {
        services.api().getPlaybackState()
                .thenAccept(state -> runOnClient(() -> {
                    playback = state;
                    playbackFetchedAt = System.currentTimeMillis();
                }))
                .exceptionally(err -> { handleError(err); return null; });
    }

    private void refreshQueue() {
        services.api().getQueue()
                .thenAccept(page -> runOnClient(() -> queueItems = new ArrayList<>(page.items())))
                .exceptionally(err -> {
                    runOnClient(() -> queueItems = new ArrayList<>());
                    return null;
                });
    }

    private void switchTab(Tab next) {
        if (next == Tab.SETTINGS) {
            MinecraftClient.getInstance().setScreen(new DefaultOverlayScreen(services));
            return;
        }
        if (next != Tab.PLAYLIST_DETAIL) openedContext = null;
        tab = next;
        selectedIndex = -1;
        mainScroll = 0;
        nextPageToken = null;
        loadingMore = false;
        listRequestSerial++;
        if (next == Tab.NOW || next == Tab.DIAGNOSTICS) {
            items = new ArrayList<>();
            loading = false;
            if (next == Tab.NOW) refreshPlayback();
            return;
        }
        if (next == Tab.QUEUE) {
            loadQueue();
            return;
        }
        loadTab(next, false);
    }

    private void loadTab(Tab selectedTab) {
        loadTab(selectedTab, false);
    }

    private void loadQueue() {
        int serial = ++listRequestSerial;
        loading = true;
        loadingMore = false;
        nextPageToken = null;
        services.api().getQueue()
                .thenAccept(page -> runOnClient(() -> {
                    if (serial != listRequestSerial || tab != Tab.QUEUE) return;
                    items = new ArrayList<>(page.items());
                    selectedIndex = items.isEmpty() ? -1 : 0;
                    mainScroll = 0;
                    loading = false;
                    setStatus("Queue loaded", TEXT_DIM);
                }))
                .exceptionally(err -> {
                    if (serial == listRequestSerial && tab == Tab.QUEUE) handleError(err);
                    return null;
                });
    }

    private void loadTab(Tab selectedTab, boolean append) {
        if (!isPageableTab(selectedTab)) return;
        if (append) {
            if (loading || loadingMore || nextPageToken == null) return;
            loadingMore = true;
        } else {
            loading = true;
            loadingMore = false;
            nextPageToken = null;
            items = new ArrayList<>();
            selectedIndex = -1;
            mainScroll = 0;
        }

        int serial = append ? listRequestSerial : ++listRequestSerial;
        String pageToken = append ? nextPageToken : null;
        CompletableFuture<BeatBlocksPage> future = switch (selectedTab) {
            case PLAYLISTS -> services.api().getPlaylists(pageToken);
            case ALBUMS -> services.api().getSavedAlbums(pageToken);
            case LIKED -> services.api().getSavedTracks(pageToken);
            case PLAYLIST_DETAIL -> services.api().getPlaylistTracks(openedContext, pageToken);
            default -> CompletableFuture.completedFuture(null);
        };

        future.whenComplete((page, throwable) -> runOnClient(() -> {
            if (serial != listRequestSerial || selectedTab != tab) return;
            loading = false;
            loadingMore = false;
            if (throwable != null) {
                handleError(throwable);
                return;
            }
            if (page == null) return;
            String status = statusForPage(selectedTab, page.items().size());
            if (append) appendItems(page.items(), page.nextUrl(), status);
            else setItems(page.items(), page.nextUrl(), status);
            maybeLoadMore();
        }));
    }

    private void setItems(List<BeatBlocksItem> newItems, String nextToken, String okStatus) {
        items = new ArrayList<>(newItems);
        selectedIndex = items.isEmpty() ? -1 : 0;
        mainScroll = 0;
        nextPageToken = normalizeToken(nextToken);
        setStatus(okStatus, TEXT_DIM);
    }

    private void appendItems(List<BeatBlocksItem> moreItems, String nextToken, String okStatus) {
        Set<String> seen = new HashSet<>();
        for (BeatBlocksItem item : items) seen.add(itemKey(item));
        int added = 0;
        for (BeatBlocksItem item : moreItems) {
            if (seen.add(itemKey(item))) {
                items.add(item);
                added++;
            }
        }
        if (selectedIndex < 0 && !items.isEmpty()) selectedIndex = 0;
        nextPageToken = added == 0 ? null : normalizeToken(nextToken);
        clampMainScroll();
        setStatus(okStatus, TEXT_DIM);
    }

    private void openPlaylist(BeatBlocksItem playlist) {
        if (playlist == null || playlist.type() != BeatBlocksItemType.PLAYLIST) return;
        contextParentTab = tab == Tab.PLAYLIST_DETAIL ? contextParentTab : tab;
        openedContext = playlist;
        tab = Tab.PLAYLIST_DETAIL;
        selectedIndex = -1;
        mainScroll = 0;
        nextPageToken = null;
        loadingMore = false;
        setStatus("Opening " + playlist.name(), TEXT_DIM);
        loadTab(Tab.PLAYLIST_DETAIL, false);
    }

    private void returnToContextParent() {
        Tab parent = contextParentTab == Tab.PLAYLIST_DETAIL ? Tab.PLAYLISTS : contextParentTab;
        openedContext = null;
        switchTab(parent);
    }

    private void togglePlayPause() {
        fireAction(playback.playing() ? services.api().pause() : services.api().resume());
    }

    private void playSelected() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            setStatus("Select an item first", ERROR_COLOR);
            return;
        }
        BeatBlocksItem item = items.get(selectedIndex);
        if (item.type() == BeatBlocksItemType.PLAYLIST) {
            openPlaylist(item);
            return;
        }
        if (!item.playable()) {
            setStatus("Item not playable", ERROR_COLOR);
            return;
        }
        if (tab == Tab.PLAYLIST_DETAIL && openedContext != null && item.type() == BeatBlocksItemType.TRACK) {
            fireAction(services.api().playItemInContext(item, openedContext));
        } else {
            fireAction(services.api().playItem(item));
        }
        setStatus("Playing: " + item.name(), ACCENT);
    }

    private void playOpenedContext(boolean shuffle) {
        if (openedContext == null || !openedContext.playable()) {
            setStatus("No playlist open", ERROR_COLOR);
            return;
        }
        fireAction(services.api().playContext(openedContext, shuffle));
        setStatus((shuffle ? "Shuffle playing: " : "Playing: ") + openedContext.name(), ACCENT);
    }

    private void addSelectedToQueue() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            setStatus("Select a track first", ERROR_COLOR);
            return;
        }
        BeatBlocksItem item = items.get(selectedIndex);
        if (!item.playable()) {
            setStatus("Item not queueable", ERROR_COLOR);
            return;
        }
        fireAction(services.api().addToQueue(item));
        setStatus("Queued: " + item.name(), ACCENT);
    }

    private void fireAction(CompletableFuture<Void> future) {
        future.thenRun(() -> runOnClient(this::refreshPlayback))
                .exceptionally(err -> { handleError(err); return null; });
    }

    private String nextRepeatState() {
        return switch (playback.repeatState().toLowerCase(Locale.ROOT)) {
            case "off" -> "context";
            case "context" -> "track";
            default -> "off";
        };
    }

    private void ensureSelectedVisible() {
        // Auto-scroll to keep selection visible
        if (selectedIndex >= 0) {
            int maxRows = currentVisibleRows();
            if (selectedIndex < mainScroll) mainScroll = selectedIndex;
            if (selectedIndex >= mainScroll + maxRows) mainScroll = selectedIndex - maxRows + 1;
            clampMainScroll();
        }
    }

    private boolean isPageableTab(Tab value) {
        return value == Tab.PLAYLISTS || value == Tab.ALBUMS || value == Tab.LIKED || value == Tab.PLAYLIST_DETAIL;
    }

    private void maybeLoadMore() {
        if (!isPageableTab(tab) || loading || loadingMore || nextPageToken == null) return;
        if (items.isEmpty()) return;
        int rows = currentVisibleRows();
        if (mainScroll + rows >= items.size() - 5) {
            loadTab(tab, true);
        }
    }

    private int currentVisibleRows() {
        return Math.max(1, currentListHeight() / ROW_H);
    }

    private void clampMainScroll() {
        int maxScroll = Math.max(0, items.size() - currentVisibleRows());
        if (mainScroll > maxScroll) mainScroll = maxScroll;
        if (mainScroll < 0) mainScroll = 0;
    }

    private String statusForPage(Tab selectedTab, int loadedCount) {
        String noun = switch (selectedTab) {
            case PLAYLISTS -> "playlists";
            case ALBUMS -> "albums";
            case LIKED -> "songs";
            case PLAYLIST_DETAIL -> "tracks";
            default -> "items";
        };
        return "Loaded " + loadedCount + " " + noun;
    }

    private static String normalizeToken(String token) {
        return token == null || token.isBlank() ? null : token;
    }

    private static String itemKey(BeatBlocksItem item) {
        if (item.uri() != null && !item.uri().isBlank()) return item.uri();
        return item.type() + ":" + item.id() + ":" + item.name();
    }

    private void loadCoverIfNeeded() {
        BeatBlocksItem track = playback.track();
        String url = track == null ? null : track.imageUrl();
        if (url == null || url.isBlank()) {
            loadedCoverUrl = null;
            currentCoverPng = null;
            return;
        }
        if (url.equals(loadedCoverUrl)) return;
        loadedCoverUrl = url;
        currentCoverPng = null;
        services.imageCache().getCoverPngBytes(url).thenAccept(png -> runOnClient(() -> {
            if (url.equals(loadedCoverUrl)) currentCoverPng = png;
        }));
    }

    private int currentProgressMs() {
        int progress = playback.progressMs();
        if (playback.playing()) {
            progress += (int) (System.currentTimeMillis() - playbackFetchedAt);
        }
        BeatBlocksItem track = playback.track();
        if (track != null && track.durationMs() > 0) progress = Math.min(progress, track.durationMs());
        return Math.max(0, progress);
    }

    private void handleError(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) root = root.getCause();
        String message;
        if (root instanceof BeatBlocksApiException apiErr) {
            message = apiErr.getMessage();
        } else {
            message = root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
        }
        String finalMsg = message;
        runOnClient(() -> {
            setStatus(finalMsg, ERROR_COLOR);
            loading = false;
            loadingMore = false;
            nextPageToken = null;
        });
    }

    private void runOnClient(Runnable r) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) r.run();
        else client.execute(r);
    }

    private void setStatus(String msg, int color) {
        statusMsg = msg == null ? "" : msg;
        statusColor = color;
    }

    // ── Drawing Helpers ──────────────────────────────────────────────────

    private int panelW() {
        int available = Math.max(1, width - PANEL_EDGE_GAP * 2);
        int minimum = Math.min(PANEL_MIN_W, available);
        int preferred = Math.max(PANEL_MIN_W, (int) (width * 0.86));
        return Math.min(available, Math.max(minimum, preferred));
    }

    private int panelH() {
        int available = Math.max(1, height - PANEL_EDGE_GAP * 2);
        int minimum = Math.min(PANEL_MIN_H, available);
        int preferred = Math.max(PANEL_MIN_H, (int) (height * 0.82));
        return Math.min(available, Math.max(minimum, preferred));
    }

    private int panelX() {
        return Math.max(0, (width - panelW()) / 2);
    }

    private int panelY() {
        return Math.max(0, (height - panelH()) / 2);
    }

    private int mainX() {
        return panelX() + SIDEBAR_W;
    }

    private int currentListY() {
        if (tab == Tab.PLAYLIST_DETAIL) return panelY() + 8 + CONTEXT_HEADER_H;
        return panelY() + 30;
    }

    private int currentListHeight() {
        int contentH = panelH() - PLAYER_BAR_H - 16;
        if (tab == Tab.PLAYLIST_DETAIL) return Math.max(20, contentH - CONTEXT_HEADER_H);
        return Math.max(20, contentH - 30);
    }

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h, boolean gold) {
        ctx.fill(x, y, x + w, y + h, PANEL_BG);
        int hi = gold ? GOLD : BORDER_LIGHT;
        int sh = gold ? GOLD_SHADOW : BORDER_DARK;
        ctx.fill(x, y, x + w, y + 1, hi);
        ctx.fill(x, y, x + 1, y + h, hi);
        ctx.fill(x, y + h - 1, x + w, y + h, sh);
        ctx.fill(x + w - 1, y, x + w, y + h, sh);
    }

    private void renderPixelCover(DrawContext ctx, byte[] png, int x, int y, int size) {
        renderCover(ctx, loadedCoverUrl, png, x, y, size);
    }

    private void renderItemCover(DrawContext ctx, BeatBlocksItem item, int x, int y, int size) {
        String url = item == null ? null : item.imageUrl();
        byte[] png = coverPngForItem(url);
        if (png != null) {
            renderCover(ctx, url, png, x, y, size);
        } else {
            renderCoverPlaceholder(ctx, x, y, size);
        }
    }

    private byte[] coverPngForItem(String url) {
        if (url == null || url.isBlank()) return null;
        byte[] cached = itemCoverCache.get(url);
        if (cached != null) return cached;
        if (itemCoverLoading.add(url)) {
            services.imageCache().getCoverPngBytes(url).thenAccept(png -> runOnClient(() -> {
                itemCoverLoading.remove(url);
                if (png != null) itemCoverCache.put(url, png);
            })).exceptionally(err -> {
                runOnClient(() -> itemCoverLoading.remove(url));
                return null;
            });
        }
        return null;
    }

    private void renderCover(DrawContext ctx, String key, byte[] png, int x, int y, int size) {
        ctx.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        if (key == null || key.isBlank() || png == null) {
            renderCoverPlaceholder(ctx, x, y, size);
            return;
        }
        net.minecraft.util.Identifier textureId = com.devgnav.beatblocks.image.CoverTextureManager.getOrCreateTexture(key, png);
        if (textureId != null) {
            GuiDrawCompat.drawTexture(ctx, textureId, x, y, size, size, size, size);
        } else {
            renderCoverPlaceholder(ctx, x, y, size);
        }
    }

    private void renderCoverPlaceholder(DrawContext ctx, int x, int y, int size) {
        ctx.fill(x, y, x + size, y + size, 0xFF141414);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("♫"), x + size / 2, y + size / 2 - 4, ACCENT);
    }

    private void renderVolumeSlider(DrawContext ctx, int volume, int mx, int my) {
        int trackY = volumeSliderY + volumeSliderH / 2 - 1;
        boolean hovered = hit(mx, my, volumeSliderX - 4, volumeSliderY - 5, volumeSliderW + 8, volumeSliderH + 10);
        ctx.fill(volumeSliderX, trackY, volumeSliderX + volumeSliderW, trackY + 3, 0xFF202020);
        ctx.fill(volumeSliderX, trackY, volumeSliderX + volumeSliderW, trackY + 1, 0xFF3A3A3A);

        int filled = Math.round(volumeSliderW * (Math.max(0, Math.min(100, volume)) / 100.0f));
        ctx.fill(volumeSliderX, trackY, volumeSliderX + filled, trackY + 3, ACCENT);

        int thumbX = volumeSliderX + filled;
        int thumbColor = hovered || volumeDragging ? ACCENT : TEXT_DIM;
        ctx.fill(thumbX - 3, volumeSliderY, thumbX + 4, volumeSliderY + volumeSliderH, 0xFF050505);
        ctx.fill(thumbX - 2, volumeSliderY + 1, thumbX + 3, volumeSliderY + volumeSliderH - 1, thumbColor);
    }

    private int displayedVolume() {
        if (volumeDragging && volumeDragValue >= 0) return volumeDragValue;
        if (volumeOverrideValue >= 0 && System.currentTimeMillis() < volumeOverrideUntil) return volumeOverrideValue;
        return playback.volumePercent();
    }

    private void updateVolumeFromMouse(double mx, boolean forceSend) {
        if (volumeSliderW <= 0) return;
        double ratio = Math.max(0.0, Math.min(1.0, (mx - volumeSliderX) / (double) volumeSliderW));
        int value = (int) Math.round(ratio * 100);
        volumeDragValue = value;
        if (value > 0) volumeBeforeMute = value;
        sendVolume(value, forceSend);
    }

    private void sendVolume(int value, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastVolumeSendAt < 80L) return;
        lastVolumeSendAt = now;
        volumeOverrideValue = Math.max(0, Math.min(100, value));
        volumeOverrideUntil = now + 1500L;
        services.api().setVolume(Math.max(0, Math.min(100, value)))
                .exceptionally(err -> { handleError(err); return null; });
        if (force) refreshPlayback();
    }

    private void toggleMute() {
        int current = displayedVolume();
        if (current > 0) volumeBeforeMute = current;
        volumeDragValue = current == 0 ? Math.max(10, volumeBeforeMute) : 0;
        sendVolume(volumeDragValue, true);
    }

    private void drawIconButton(DrawContext ctx, Identifier icon, int x, int y, int w, int h, int mx, int my, boolean active) {
        if (w <= 0 || h <= 0) return;
        boolean hovered = hit(mx, my, x, y, w, h);
        int bg = active ? ACTIVE_BG : hovered ? HOVER_BG : 0x00000000;
        if (bg != 0) ctx.fill(x - 2, y - 2, x + w + 2, y + h + 2, bg);
        if (active) ctx.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, ACCENT);
        int iconSize = Math.max(8, Math.min(w, h) - 2);
        drawIcon(ctx, icon, x + (w - iconSize) / 2, y + (h - iconSize) / 2, iconSize);
    }

    private static void drawIcon(DrawContext ctx, Identifier icon, int x, int y, int size) {
        GuiDrawCompat.drawTexture(ctx, icon, x, y, size, size, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
    }

    private static Identifier icon(String name) {
        return Identifier.of("beatblocks", "textures/gui/icons/" + name);
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return w > 0 && h > 0 && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static Text trimText(String value, int maxChars) {
        if (value == null) return Text.literal("");
        if (maxChars <= 3) return Text.literal("...");
        return Text.literal(value.length() <= maxChars ? value : value.substring(0, Math.max(0, maxChars - 3)) + "...");
    }

    private static String formatMs(int ms) {
        int seconds = Math.max(0, ms / 1000);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // ── Tab Enum ─────────────────────────────────────────────────────────

    private enum Tab {
        NOW("Now Playing"),
        PLAYLISTS("Playlists"),
        PLAYLIST_DETAIL("Playlist"),
        LIKED("Liked Songs"),
        ALBUMS("Albums"),
        QUEUE("Queue"),
        DIAGNOSTICS("Diagnostics"),
        SETTINGS("Settings");

        final String label;
        Tab(String label) { this.label = label; }
    }
}
