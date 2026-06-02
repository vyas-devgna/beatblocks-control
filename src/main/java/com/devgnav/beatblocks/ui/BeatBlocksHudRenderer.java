package com.devgnav.beatblocks.ui;

import com.devgnav.beatblocks.BeatBlocksServices;
import com.devgnav.beatblocks.config.BeatBlocksConfig;
import com.devgnav.beatblocks.image.PixelCover;
import com.devgnav.beatblocks.model.PlaybackState;
import com.devgnav.beatblocks.model.BeatBlocksItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class BeatBlocksHudRenderer implements HudRenderCallback {
    private static final int BASE_WIDTH = 190;
    private static final int BASE_HEIGHT = 42;
    private static final int BASE_PADDING = 4;
    private static final double HUD_SCALE_MIN = 0.35;
    private static final double HUD_SCALE_MAX = 3.0;
    private static final int PANEL_BG = 0xCC0B0B0B;
    private static final int ACCENT = 0xFF1DB954;
    private static final int TEXT_MAIN = 0xFFEFEFEF;
    private static final int TEXT_DIM = 0xFFAAAAAA;
    private static final int FONT_HEIGHT = 9;
    private static volatile long previewUntilMs = 0L;

    private final BeatBlocksServices services;
    private String loadedCoverUrl = null;
    private PixelCover currentCover = null;
    private String lastTrackId = "";

    public BeatBlocksHudRenderer(BeatBlocksServices services) {
        this.services = services;
    }

    public static void showPreviewFor(long millis) {
        previewUntilMs = Math.max(previewUntilMs, System.currentTimeMillis() + Math.max(0, millis));
    }

    @Override
    public void onHudRender(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        BeatBlocksConfig config = services.config();
        boolean previewing = System.currentTimeMillis() < previewUntilMs;

        if (!config.hudEnabled && !previewing) return;
        if (client.options.hudHidden && !previewing) return;
        if (client.currentScreen != null && config.hudAutoHide && !previewing) return;
        if (config.hudAutoHide && client.getCurrentFps() < 15 && !previewing) return;

        PlaybackState playback = services.api().currentState();
        if (!previewing && (!playback.active() || playback.track() == null)) return;

        BeatBlocksItem track = playback.track();
        TextRenderer textRenderer = client.textRenderer;

        if (track != null && config.toastEnabled && !track.id().equals(lastTrackId)) {
            lastTrackId = track.id();
            if (client.player != null) {
                client.player.sendMessage(
                        net.minecraft.text.Text.literal("§a♫ §fNow Playing: §e" + track.name() + " §7- " + track.subtitle()),
                        true
                );
            }
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        double hudScale = Math.max(HUD_SCALE_MIN, Math.min(HUD_SCALE_MAX, config.hudScaleMultiplier));

        int w = Math.max(96, Math.min((int) Math.round(BASE_WIDTH * hudScale), Math.max(96, screenW - 4)));
        int h = Math.max(24, Math.min((int) Math.round(BASE_HEIGHT * hudScale), Math.max(24, screenH / 4)));
        int minHeight = Math.max(32, (int) Math.round(36 * hudScale));
        h = Math.min(Math.max(h, minHeight), Math.max(24, screenH / 4));
        int p = Math.max(2, Math.min((int) Math.round(BASE_PADDING * hudScale), 10));
        int margin = Math.max(0, Math.min(config.hudMargin, 32));

        String position = config.hudPosition == null ? "top_right" : config.hudPosition;
        int x = switch (position) {
            case "top_left", "bottom_left" -> margin;
            default -> screenW - w - margin;
        };
        int y = switch (position) {
            case "bottom_left", "bottom_right" -> screenH - h - margin;
            default -> margin;
        };
        x = Math.max(0, Math.min(x, screenW - w));
        y = Math.max(0, Math.min(y, screenH - h));

        drawPanel(context, x, y, w, h);

        loadCoverIfNeeded(track != null ? track.imageUrl() : null);

        int barY = y + h - 2;
        int innerTop = y + p;
        int innerBottom = barY - 1;

        int coverSize = Math.min(innerBottom - innerTop, Math.max(16, (w - 3 * p) / 3));
        int coverX = x + p;
        int coverY = innerTop + Math.max(0, (innerBottom - innerTop - coverSize) / 2);

        if (currentCover != null) {
            renderPixelCover(context, currentCover, coverX, coverY, coverSize);
        } else {
            renderCoverPlaceholder(context, coverX, coverY, coverSize);
        }

        int textGap = Math.max(4, (int) Math.round(5 * hudScale));
        int textX = coverX + coverSize + textGap;
        int textRight = x + w - p;
        int textAreaWidth = Math.max(8, textRight - textX);

        String trackName = trimToWidth(
                track != null ? track.name() : "HUD Size Preview", textAreaWidth, textRenderer);
        String artistName = trimToWidth(
                track != null ? track.subtitle() : "BeatBlocks", textAreaWidth, textRenderer);

        int lineGap = 1;
        int textY1 = innerTop;
        int textY2 = textY1 + FONT_HEIGHT + lineGap;
        boolean showArtist = textY2 + FONT_HEIGHT <= innerBottom;

        context.enableScissor(textX, innerTop, textRight, innerBottom);
        context.drawTextWithShadow(textRenderer, Text.literal(trackName), textX, textY1, TEXT_MAIN);
        if (showArtist) {
            context.drawTextWithShadow(textRenderer, Text.literal(artistName), textX, textY2, TEXT_DIM);
        }
        context.disableScissor();

        int barX = x;
        int barW = w;
        int durationMs = track != null ? track.durationMs() : 100_000;
        if (barW > 10 && durationMs > 0) {
            context.fill(barX, barY, barX + barW, barY + 2, 0xFF333333);
            int progress = track != null ? currentProgressMs(playback) : 62_000;
            int filled = Math.min(barW, Math.max(0, (int) (barW * (progress / (double) durationMs))));
            context.fill(barX, barY, barX + filled, barY + 2, ACCENT);
        }
    }

    private void loadCoverIfNeeded(String url) {
        if (url == null || url.isBlank()) {
            loadedCoverUrl = null;
            currentCover = null;
            return;
        }
        if (url.equals(loadedCoverUrl)) return;
        loadedCoverUrl = url;
        currentCover = null;
        services.imageCache().getCover(url).thenAccept(cover -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;
            client.execute(() -> {
                if (url.equals(loadedCoverUrl)) {
                    currentCover = cover;
                }
            });
        });
    }

    private int currentProgressMs(PlaybackState playback) {
        int progress = playback.progressMs();
        if (playback.playing()) {
            progress += (int) (System.currentTimeMillis() - services.api().getDiagnostics().lastHeartbeatAt());
        }
        BeatBlocksItem track = playback.track();
        if (track != null && track.durationMs() > 0) progress = Math.min(progress, track.durationMs());
        return Math.max(0, progress);
    }

    private void renderPixelCover(DrawContext context, PixelCover cover, int x, int y, int size) {
        context.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF000000);
        net.minecraft.util.Identifier textureId = com.devgnav.beatblocks.image.CoverTextureManager.getOrCreateTexture(loadedCoverUrl, cover);
        if (textureId != null) {
            GuiDrawCompat.drawTexture(context, textureId, x, y, size, size, cover.width(), cover.height());
        } else {
            renderCoverPlaceholder(context, x, y, size);
        }
    }

    private void renderCoverPlaceholder(DrawContext context, int x, int y, int size) {
        context.fill(x, y, x + size, y + size, 0xFF141414);
        int cx = x + size / 2;
        int cy = y + size / 2 - 4;
        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.literal("♫"), cx - 2, cy, ACCENT);
    }

    private void drawPanel(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, PANEL_BG);
        context.fill(x, y, x + w, y + 1, 0xFF3C3C3C);
        context.fill(x, y, x + 1, y + h, 0xFF3C3C3C);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF141414);
        context.fill(x + w - 1, y, x + w, y + h, 0xFF141414);
    }

    private static String trimToWidth(String value, int maxWidth, TextRenderer textRenderer) {
        if (value == null || value.isBlank()) return "";
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        String dots = "…";
        int dotsWidth = textRenderer.getWidth(dots);
        while (value.length() > 0 && textRenderer.getWidth(value) + dotsWidth > maxWidth) {
            value = value.substring(0, value.length() - 1);
        }
        return value.isEmpty() ? dots : value + dots;
    }
}