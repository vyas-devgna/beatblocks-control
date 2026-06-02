package com.devgnav.beatblocks.ui;

import com.devgnav.beatblocks.BeatBlocksServices;
import com.devgnav.beatblocks.mode.ModeManager;
import com.devgnav.beatblocks.model.BridgeDiagnostics;
import com.devgnav.beatblocks.model.PlaybackState;
import com.devgnav.beatblocks.model.BeatBlocksItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * Default Mode overlay — settings, status, mode selection, and basic playback controls.
 * Does NOT show the full BeatBlocks UI (playlists, albums, etc.).
 * Shown when Alt+I is pressed and mode is DEFAULT or Enhanced is unavailable.
 */
public class DefaultOverlayScreenBase extends Screen {

    private static final int PANEL_BG = 0xEE0E0E0E;
    private static final int BORDER_HI = 0xFF3C3C3C;
    private static final int BORDER_SH = 0xFF141414;
    private static final int ACCENT = 0xFF1DB954;
    private static final int ACCENT_DIM = 0xFF158C3F;
    private static final int GOLD = 0xFFD8AF2F;
    private static final int TEXT_MAIN = 0xFFEFEFEF;
    private static final int TEXT_DIM = 0xFFAAAAAA;
    private static final int TEXT_DARK = 0xFF666666;
    private static final int ERROR_COLOR = 0xFFFF7777;
    private static final int HOVER_BG = 0x33FFFFFF;
    private static final int ACTIVE_BG = 0x4433AA66;
    private static final int DISABLED_BG = 0x22FFFFFF;
    private static final double HUD_SCALE_MIN = 0.35;
    private static final double HUD_SCALE_MAX = 3.0;
    private static final long HUD_PREVIEW_MS = 1200L;

    private static final String SETUP_URL = "https://github.com/vyas-devgna/beatblocks-control#setup-guide";

    private final BeatBlocksServices services;
    private long lastPoll = 0L;
    private PlaybackState playback = PlaybackState.inactive();

    private int defaultBtnY = 0;
    private int enhancedBtnY = 0;
    private int transportY = -1;
    private int transportX = 0;
    private int hudScaleSliderX = 0;
    private int hudScaleSliderY = 0;
    private int hudScaleSliderW = 0;
    private boolean hudScaleDragging = false;

    public DefaultOverlayScreenBase(BeatBlocksServices services) {
        super(Text.literal("BeatBlocks Settings"));
        this.services = services;
    }

    @Override
    protected void init() {
        refreshPlayback();
    }

    @Override
    public void tick() {
        long now = System.currentTimeMillis();
        if (now - lastPoll > services.config().apiPollSeconds * 1000L) {
            lastPoll = now;
            refreshPlayback();
        }
    }

    private int calculateContentHeight(int panelW, boolean enhancedAvail, BridgeDiagnostics diag, BeatBlocksItem track) {
        int h = 12; // top margin
        h += 14; // title
        h += 6; // separator space
        h += 10; // "Mode" label
        h += 16 + 3; // Default button + spacing
        h += 16 + 4; // Enhanced button + spacing
        
        if (!enhancedAvail) {
            ModeManager mm = services.modeManager();
            String reason = mm.getUnavailableReason();
            if (reason == null) reason = "Enhanced Mode requires desktop player + Spicetify bridge extension";
            h += wrapText(reason, panelW - 32).length * 9;
            h += 11; // Setup url
            h += 2; // spacer
        }

        h += 6; // separator space
        h += 10; // "HUD" label
        h += 18; // scale slider
        h += 4; // spacer
        
        h += 6; // separator space
        h += 10; // "Bridge Status" label
        h += 10 * 3 + 12; // status rows (Server, Extension, Heartbeat, Port)
        h += 6; // separator space
        h += 10; // "Now Playing" label
        
        if (track != null) {
            h += 10; // track name
            h += 10; // artist
            h += 14; // transport
        } else {
            h += 10; // "No track playing"
            if (diag.extensionConnected()) {
                h += 10; // "Open BeatBlocks..."
            }
        }
        h += 8; // footer spacer
        h += 10; // footer text
        h += 14; // bottom margin
        return h;
    }

    // ── Render ───────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Translucent background — game visible behind
        ctx.fill(0, 0, width, height, hudScaleDragging ? 0x44000000 : 0x88000000);
        if (hudScaleDragging) BeatBlocksHudRenderer.showPreviewFor(HUD_PREVIEW_MS);

        int panelW = Math.min(360, width - 40);
        ModeManager mm = services.modeManager();
        boolean enhancedAvail = mm.isEnhancedAvailable();
        BridgeDiagnostics diag = services.api().getDiagnostics();
        BeatBlocksItem track = playback.track();

        int contentH = calculateContentHeight(panelW, enhancedAvail, diag, track);
        int panelH = Math.min(contentH, height - 20);
        int px = (width - panelW) / 2;
        int py = (height - panelH) / 2;

        drawPanel(ctx, px, py, panelW, panelH);

        int cx = px + panelW / 2;
        int y = py + 12;

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("♫ BeatBlocks Control"), cx, y, GOLD);
        y += 14;
        ctx.fill(px + 12, y, px + panelW - 12, y + 1, BORDER_SH);
        y += 6;

        // ── Mode Selection ──────────────────────────────────────────────
        ctx.drawTextWithShadow(textRenderer, Text.literal("Mode"), px + 14, y, TEXT_DIM);
        y += 10;

        ModeManager.Mode selected = mm.getSelectedMode();

        // Default mode button
        int btnW = panelW - 28;
        int btnH = 16;
        int btnX = px + 14;
        boolean defaultHover = mx >= btnX && mx < btnX + btnW && my >= y && my < y + btnH;
        boolean defaultActive = selected == ModeManager.Mode.DEFAULT;
        ctx.fill(btnX, y, btnX + btnW, y + btnH, defaultActive ? ACTIVE_BG : defaultHover ? HOVER_BG : 0x11FFFFFF);
        if (defaultActive) ctx.fill(btnX, y, btnX + 3, y + btnH, ACCENT);
        this.defaultBtnY = y;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Default Mode"), btnX + 8, y + 4, defaultActive ? TEXT_MAIN : TEXT_DIM);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Settings & status only"), btnX + btnW - textRenderer.getWidth("Settings & status only") - 4, y + 4, TEXT_DARK);
        y += btnH + 3;

        // Enhanced mode button
        boolean enhancedHover = mx >= btnX && mx < btnX + btnW && my >= y && my < y + btnH;
        boolean enhancedActive = selected == ModeManager.Mode.ENHANCED;
        int enhBg = !enhancedAvail ? DISABLED_BG : enhancedActive ? ACTIVE_BG : enhancedHover ? HOVER_BG : 0x11FFFFFF;
        ctx.fill(btnX, y, btnX + btnW, y + btnH, enhBg);
        if (enhancedActive && enhancedAvail) ctx.fill(btnX, y, btnX + 3, y + btnH, ACCENT);
        this.enhancedBtnY = y;
        int enhTextColor = enhancedAvail ? (enhancedActive ? TEXT_MAIN : TEXT_DIM) : TEXT_DARK;
        ctx.drawTextWithShadow(textRenderer, Text.literal("Enhanced Spicetify Mode"), btnX + 8, y + 4, enhTextColor);
        if (enhancedAvail) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("Full BeatBlocks UI"), btnX + btnW - textRenderer.getWidth("Full BeatBlocks UI") - 4, y + 4, TEXT_DARK);
        } else {
            ctx.drawTextWithShadow(textRenderer, Text.literal("✗"), btnX + btnW - 12, y + 4, ERROR_COLOR);
        }
        y += btnH + 4;

        // Enhanced unavailable message
        if (!enhancedAvail) {
            String reason = mm.getUnavailableReason();
            if (reason == null) reason = "Enhanced Mode requires desktop player + Spicetify bridge extension";
            // Word wrap long messages
            for (String line : wrapText(reason, panelW - 32)) {
                ctx.drawTextWithShadow(textRenderer, Text.literal(line), px + 16, y, ERROR_COLOR);
                y += 9;
            }
            ctx.drawTextWithShadow(textRenderer, Text.literal("Setup: " + SETUP_URL), px + 16, y, ACCENT_DIM);
            y += 11;
            y += 2;
        }

        ctx.fill(px + 12, y, px + panelW - 12, y + 1, BORDER_SH);
        y += 6;

        // ── HUD Settings ────────────────────────────────────────────────
        ctx.drawTextWithShadow(textRenderer, Text.literal("HUD"), px + 14, y, TEXT_DIM);
        y += 10;
        renderHudScaleSlider(ctx, px + 14, y, panelW - 28, mx, my);
        y += 22;

        ctx.fill(px + 12, y, px + panelW - 12, y + 1, BORDER_SH);
        y += 6;

        // ── Bridge Status ───────────────────────────────────────────────
        ctx.drawTextWithShadow(textRenderer, Text.literal("Bridge Status"), px + 14, y, TEXT_DIM);
        y += 10;
        drawStatusRow(ctx, px + 16, y, panelW - 36, "Bridge Server", diag.bridgeRunning() ? "Running" : "Stopped", diag.bridgeRunning() ? ACCENT : ERROR_COLOR);
        y += 10;
        drawStatusRow(ctx, px + 16, y, panelW - 36, "Extension", diag.extensionConnected() ? "Connected" : "Waiting...", diag.extensionConnected() ? ACCENT : GOLD);
        y += 10;
        long ago = diag.lastHeartbeatAt() > 0 ? (System.currentTimeMillis() - diag.lastHeartbeatAt()) / 1000 : -1;
        String hb = ago < 0 ? "Never" : ago + "s ago";
        int hbColor = ago >= 0 && ago < 5 ? ACCENT : ago >= 0 && ago < 15 ? GOLD : ERROR_COLOR;
        drawStatusRow(ctx, px + 16, y, panelW - 36, "Heartbeat", hb, hbColor);
        y += 10;
        drawStatusRow(ctx, px + 16, y, panelW - 36, "Port", String.valueOf(diag.port()), TEXT_MAIN);
        y += 12;

        ctx.fill(px + 12, y, px + panelW - 12, y + 1, BORDER_SH);
        y += 6;

        // ── Basic Playback Controls ─────────────────────────────────────
        ctx.drawTextWithShadow(textRenderer, Text.literal("Now Playing"), px + 14, y, TEXT_DIM);
        y += 10;

        if (track != null) {
            String name = track.name().length() > 35 ? track.name().substring(0, 34) + "…" : track.name();
            String artist = track.subtitle() != null && track.subtitle().length() > 40 ? track.subtitle().substring(0, 39) + "…" : (track.subtitle() != null ? track.subtitle() : "");
            ctx.drawTextWithShadow(textRenderer, Text.literal(name), px + 16, y, TEXT_MAIN);
            y += 10;
            ctx.drawTextWithShadow(textRenderer, Text.literal(artist), px + 16, y, TEXT_DIM);
            y += 10;

            // Transport buttons
            this.transportY = y;
            this.transportX = cx - 35;
            ctx.drawTextWithShadow(textRenderer, Text.literal("⏮"), transportX, transportY, TEXT_DIM);
            String ppIcon = playback.playing() ? "⏸" : "▶";
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(ppIcon), cx, transportY, ACCENT);
            ctx.drawTextWithShadow(textRenderer, Text.literal("⏭"), transportX + 55, transportY, TEXT_DIM);
            y += 14;
        } else {
            this.transportY = -1;
            ctx.drawTextWithShadow(textRenderer, Text.literal("No track playing"), px + 16, y, TEXT_DARK);
            y += 10;
            if (diag.extensionConnected()) {
                ctx.drawTextWithShadow(textRenderer, Text.literal("Open your player and start playback"), px + 16, y, TEXT_DARK);
                y += 10;
            }
        }

        // ── Footer ──────────────────────────────────────────────────────
        y += 8;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Alt+I: toggle overlay • ESC: close"), cx, y, TEXT_DARK);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Prevent default Minecraft screen blur/gradient
    }

    // ── Input ────────────────────────────────────────────────────────────

    protected boolean mouseClickedImpl(double mx, double my, int button) {
        if (button != 0) return true;

        int panelW = Math.min(360, width - 40);
        int btnW = panelW - 28;
        int btnH = 16;
        int px = (width - panelW) / 2;
        int btnX = px + 14;

        // Default mode click
        if (mx >= btnX && mx < btnX + btnW && my >= defaultBtnY && my < defaultBtnY + btnH) {
            services.modeManager().setSelectedMode(ModeManager.Mode.DEFAULT);
            return true;
        }

        // Enhanced mode click (only if available)
        if (mx >= btnX && mx < btnX + btnW && my >= enhancedBtnY && my < enhancedBtnY + btnH) {
            if (services.modeManager().isEnhancedAvailable()) {
                services.modeManager().setSelectedMode(ModeManager.Mode.ENHANCED);
                // Switch to Enhanced overlay immediately
                MinecraftClient.getInstance().setScreen(new BeatBlocksOverlayScreen(services));
            }
            return true;
        }

        if (isHudScaleSliderHit(mx, my)) {
            hudScaleDragging = true;
            updateHudScaleFromMouse(mx);
            BeatBlocksHudRenderer.showPreviewFor(HUD_PREVIEW_MS);
            return true;
        }

        // Transport control clicks
        if (transportY != -1 && my >= transportY && my < transportY + 12) {
            int cx = px + panelW / 2;
            if (mx >= transportX && mx < transportX + 20) {
                services.api().previous();
                return true;
            }
            if (mx >= cx - 8 && mx < cx + 8) {
                if (playback.playing()) services.api().pause();
                else services.api().resume();
                return true;
            }
            if (mx >= transportX + 50 && mx < transportX + 70) {
                services.api().next();
                return true;
            }
        }

        return true; // consume all clicks
    }

    protected boolean mouseDraggedImpl(double mx, double my, int button, double dx, double dy) {
        if (hudScaleDragging && button == 0) {
            updateHudScaleFromMouse(mx);
            BeatBlocksHudRenderer.showPreviewFor(HUD_PREVIEW_MS);
            return true;
        }
        return true;
    }

    protected boolean mouseReleasedImpl(double mx, double my, int button) {
        if (button == 0 && hudScaleDragging) {
            updateHudScaleFromMouse(mx);
            hudScaleDragging = false;
            services.config().save();
            BeatBlocksHudRenderer.showPreviewFor(HUD_PREVIEW_MS);
            return true;
        }
        return true;
    }

    protected boolean keyPressedImpl(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        // Consume all keys — input isolation
        return true;
    }

    protected boolean charTypedImpl(int codePoint, int modifiers) {
        return true; // consume all
    }

    protected boolean mouseScrolledImpl(double mx, double my, double hAmount, double vAmount) {
        return true; // consume all
    }

    @Override
    public void close() {
        if (hudScaleDragging) {
            hudScaleDragging = false;
            services.config().save();
        }
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void refreshPlayback() {
        services.api().getPlaybackState()
                .thenAccept(state -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) client.execute(() -> playback = state);
                    else playback = state;
                })
                .exceptionally(err -> null);
    }

    private void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, PANEL_BG);
        ctx.fill(x, y, x + w, y + 1, BORDER_HI);
        ctx.fill(x, y, x + 1, y + h, BORDER_HI);
        ctx.fill(x, y + h - 1, x + w, y + h, BORDER_SH);
        ctx.fill(x + w - 1, y, x + w, y + h, BORDER_SH);
    }

    private void drawStatusRow(DrawContext ctx, int x, int y, int w, String label, String value, int valueColor) {
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), x, y, TEXT_DIM);
        int valueW = textRenderer.getWidth(value);
        ctx.drawTextWithShadow(textRenderer, Text.literal(value), x + w - valueW, y, valueColor);
    }

    private void renderHudScaleSlider(DrawContext ctx, int x, int y, int w, int mx, int my) {
        double scale = clamp(services.config().hudScaleMultiplier, HUD_SCALE_MIN, HUD_SCALE_MAX);
        String value = Math.round(scale * 100) + "%";

        ctx.drawTextWithShadow(textRenderer, Text.literal("HUD Size"), x + 2, y + 5, TEXT_MAIN);
        ctx.drawTextWithShadow(textRenderer, Text.literal(value), x + w - textRenderer.getWidth(value), y + 5, ACCENT);

        hudScaleSliderX = x + 78;
        hudScaleSliderY = y + 8;
        hudScaleSliderW = Math.max(80, w - 130);

        int trackY = hudScaleSliderY + 3;
        boolean hover = isHudScaleSliderHit(mx, my);
        ctx.fill(hudScaleSliderX, trackY, hudScaleSliderX + hudScaleSliderW, trackY + 2, 0xFF202020);

        double ratio = (scale - HUD_SCALE_MIN) / (HUD_SCALE_MAX - HUD_SCALE_MIN);
        int handleX = hudScaleSliderX + (int) Math.round(ratio * hudScaleSliderW);
        ctx.fill(hudScaleSliderX, trackY, handleX, trackY + 2, ACCENT_DIM);
        ctx.fill(handleX - 3, hudScaleSliderY, handleX + 4, hudScaleSliderY + 9, hover || hudScaleDragging ? ACCENT : TEXT_DIM);
        ctx.fill(handleX - 2, hudScaleSliderY + 1, handleX + 3, hudScaleSliderY + 8, 0xFF0B0B0B);
    }

    private boolean isHudScaleSliderHit(double mx, double my) {
        return hudScaleSliderW > 0
                && mx >= hudScaleSliderX - 6
                && mx <= hudScaleSliderX + hudScaleSliderW + 6
                && my >= hudScaleSliderY - 5
                && my <= hudScaleSliderY + 14;
    }

    private void updateHudScaleFromMouse(double mx) {
        if (hudScaleSliderW <= 0) return;
        double ratio = clamp((mx - hudScaleSliderX) / (double) hudScaleSliderW, 0.0, 1.0);
        double scale = HUD_SCALE_MIN + ratio * (HUD_SCALE_MAX - HUD_SCALE_MIN);
        services.config().hudScaleMultiplier = Math.round(scale * 20.0) / 20.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String[] wrapText(String text, int maxPixelWidth) {
        if (text == null) return new String[0];
        if (textRenderer.getWidth(text) <= maxPixelWidth) return new String[]{text};
        // Simple word-wrap
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() > 0 && textRenderer.getWidth(current + " " + word) > maxPixelWidth) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (current.length() > 0) current.append(" ");
                current.append(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines.toArray(new String[0]);
    }
}
