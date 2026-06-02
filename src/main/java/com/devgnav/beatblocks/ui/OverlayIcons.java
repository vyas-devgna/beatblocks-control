package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;

/** Programmatic overlay icons — no image textures. */
public final class OverlayIcons {
    public static final int DISPLAY_SIZE = 14;

    public enum Kind {
        NOW,
        PLAYLIST,
        LIKED,
        ALBUM,
        QUEUE,
        DIAGNOSTICS,
        SETTINGS,
        PLAY,
        PAUSE,
        PREVIOUS,
        NEXT,
        SHUFFLE,
        REPEAT,
        REPEAT_ONE,
        VOLUME,
        VOLUME_LOW,
        VOLUME_MUTED,
        HEART
    }

    private static final float GRID = 14f;

    private OverlayIcons() {}

    public static void draw(DrawContext ctx, Kind kind, int x, int y, int size, int color) {
        if (kind == null || size <= 0) {
            return;
        }
        float scale = size / GRID;
        switch (kind) {
            case NOW -> drawNow(ctx, x, y, scale, color);
            case PLAYLIST -> drawPlaylist(ctx, x, y, scale, color);
            case LIKED, HEART -> drawHeart(ctx, x, y, scale, color, false);
            case ALBUM -> drawAlbum(ctx, x, y, scale, color);
            case QUEUE -> drawQueue(ctx, x, y, scale, color);
            case DIAGNOSTICS -> drawDiagnostics(ctx, x, y, scale, color);
            case SETTINGS -> drawSettings(ctx, x, y, scale, color);
            case PLAY -> drawPlay(ctx, x, y, scale, color);
            case PAUSE -> drawPause(ctx, x, y, scale, color);
            case PREVIOUS -> drawSkip(ctx, x, y, scale, color, true);
            case NEXT -> drawSkip(ctx, x, y, scale, color, false);
            case SHUFFLE -> drawShuffle(ctx, x, y, scale, color);
            case REPEAT -> drawRepeat(ctx, x, y, scale, color, false);
            case REPEAT_ONE -> drawRepeat(ctx, x, y, scale, color, true);
            case VOLUME -> drawVolume(ctx, x, y, scale, color, 2);
            case VOLUME_LOW -> drawVolume(ctx, x, y, scale, color, 1);
            case VOLUME_MUTED -> drawVolumeMuted(ctx, x, y, scale, color);
        }
    }

    private static void drawNow(DrawContext ctx, int ox, int oy, float s, int c) {
        rect(ctx, ox, oy, s, 3, 2, 4, 12, c);
        rect(ctx, ox, oy, s, 6, 2, 8, 12, c);
        rect(ctx, ox, oy, s, 9, 2, 12, 12, c);
        rect(ctx, ox, oy, s, 2, 10, 4, 12, c);
    }

    private static void drawPlaylist(DrawContext ctx, int ox, int oy, float s, int c) {
        for (int i = 0; i < 4; i++) {
            float y = 3 + i * 3f;
            rect(ctx, ox, oy, s, 3, y, 11, y + 1.5f, c);
        }
        rect(ctx, ox, oy, s, 3, 3, 5, 11, c);
    }

    private static void drawHeart(DrawContext ctx, int ox, int oy, float s, int c, boolean filled) {
        rect(ctx, ox, oy, s, 4, 3, 6, 5, c);
        rect(ctx, ox, oy, s, 8, 3, 10, 5, c);
        rect(ctx, ox, oy, s, 5, 2, 9, 4, c);
        rect(ctx, ox, oy, s, 6, 5, 8, 8, c);
        rect(ctx, ox, oy, s, 5, 8, 9, 11, c);
        if (filled) {
            rect(ctx, ox, oy, s, 5, 5, 9, 9, c);
        }
    }

    private static void drawAlbum(DrawContext ctx, int ox, int oy, float s, int c) {
        rect(ctx, ox, oy, s, 3, 3, 11, 4, c);
        rect(ctx, ox, oy, s, 3, 10, 11, 11, c);
        rect(ctx, ox, oy, s, 3, 4, 4, 10, c);
        rect(ctx, ox, oy, s, 10, 4, 11, 10, c);
        rect(ctx, ox, oy, s, 5, 8, 7, 10, c);
        rect(ctx, ox, oy, s, 8, 5, 10, 7, c);
    }

    private static void drawQueue(DrawContext ctx, int ox, int oy, float s, int c) {
        rect(ctx, ox, oy, s, 3, 3, 11, 5, c);
        rect(ctx, ox, oy, s, 3, 6, 11, 8, c);
        rect(ctx, ox, oy, s, 3, 9, 11, 11, c);
    }

    private static void drawDiagnostics(DrawContext ctx, int ox, int oy, float s, int c) {
        rect(ctx, ox, oy, s, 3, 9, 5, 11, c);
        rect(ctx, ox, oy, s, 6, 6, 8, 11, c);
        rect(ctx, ox, oy, s, 9, 3, 11, 11, c);
        rect(ctx, ox, oy, s, 3, 3, 11, 11, 0x44FFFFFF);
    }

    private static void drawSettings(DrawContext ctx, int ox, int oy, float s, int c) {
        rect(ctx, ox, oy, s, 6, 6, 8, 8, c);
        rect(ctx, ox, oy, s, 6.5f, 2, 7.5f, 4, c);
        rect(ctx, ox, oy, s, 6.5f, 10, 7.5f, 12, c);
        rect(ctx, ox, oy, s, 2, 6.5f, 4, 7.5f, c);
        rect(ctx, ox, oy, s, 10, 6.5f, 12, 7.5f, c);
        rect(ctx, ox, oy, s, 3.5f, 3.5f, 5, 5, c);
        rect(ctx, ox, oy, s, 9, 3.5f, 10.5f, 5, c);
        rect(ctx, ox, oy, s, 3.5f, 9, 5, 10.5f, c);
        rect(ctx, ox, oy, s, 9, 9, 10.5f, 10.5f, c);
    }

    private static void drawPlay(DrawContext ctx, int ox, int oy, float s, int c) {
        for (int row = 0; row < 10; row++) {
            float y0 = 2 + row;
            float y1 = y0 + 1;
            float half = Math.min(row, 9 - row) + 1;
            float x0 = 5f;
            float x1 = 5f + half * 0.7f;
            rect(ctx, ox, oy, s, x0, y0, x1, y1, c);
        }
    }

    private static void drawPause(DrawContext ctx, int ox, int oy, float s, int c) {
        rect(ctx, ox, oy, s, 4, 3, 6, 11, c);
        rect(ctx, ox, oy, s, 8, 3, 10, 11, c);
    }

    private static void drawSkip(DrawContext ctx, int ox, int oy, float s, int c, boolean previous) {
        if (previous) {
            rect(ctx, ox, oy, s, 3, 3, 5, 11, c);
            drawPlayTriangle(ctx, ox, oy, s, c, 5, true);
        } else {
            drawPlayTriangle(ctx, ox, oy, s, c, 4, false);
            rect(ctx, ox, oy, s, 9, 3, 11, 11, c);
        }
    }

    private static void drawPlayTriangle(DrawContext ctx, int ox, int oy, float s, int c, float baseX, boolean pointLeft) {
        for (int row = 0; row < 8; row++) {
            float y0 = 3 + row;
            float y1 = y0 + 1;
            float half = Math.min(row, 7 - row) + 1;
            if (pointLeft) {
                float x1 = baseX + 5;
                float x0 = x1 - half * 0.65f;
                rect(ctx, ox, oy, s, x0, y0, x1, y1, c);
            } else {
                float x0 = baseX;
                float x1 = x0 + half * 0.65f;
                rect(ctx, ox, oy, s, x0, y0, x1, y1, c);
            }
        }
    }

    private static void drawShuffle(DrawContext ctx, int ox, int oy, float s, int c) {
        rect(ctx, ox, oy, s, 3, 4, 8, 5, c);
        rect(ctx, ox, oy, s, 8, 4, 9, 5, c);
        rect(ctx, ox, oy, s, 8, 9, 11, 10, c);
        rect(ctx, ox, oy, s, 3, 9, 6, 10, c);
        rect(ctx, ox, oy, s, 6, 5, 8, 9, c);
    }

    private static void drawRepeat(DrawContext ctx, int ox, int oy, float s, int c, boolean one) {
        rect(ctx, ox, oy, s, 3, 4, 11, 6, c);
        rect(ctx, ox, oy, s, 3, 8, 11, 10, c);
        rect(ctx, ox, oy, s, 9, 3, 11, 5, c);
        rect(ctx, ox, oy, s, 3, 3, 5, 5, c);
        rect(ctx, ox, oy, s, 9, 9, 11, 11, c);
        if (one) {
            rect(ctx, ox, oy, s, 6, 6, 8, 10, c);
        }
    }

    private static void drawVolume(DrawContext ctx, int ox, int oy, float s, int c, int waves) {
        rect(ctx, ox, oy, s, 2, 5, 5, 9, c);
        rect(ctx, ox, oy, s, 5, 4, 7, 10, c);
        if (waves >= 1) {
            rect(ctx, ox, oy, s, 8, 5, 9, 9, c);
        }
        if (waves >= 2) {
            rect(ctx, ox, oy, s, 10, 4, 11, 10, c);
        }
    }

    private static void drawVolumeMuted(DrawContext ctx, int ox, int oy, float s, int c) {
        drawVolume(ctx, ox, oy, s, c, 0);
        rect(ctx, ox, oy, s, 8, 6, 12, 7, c);
        rect(ctx, ox, oy, s, 9, 7, 11, 8, c);
    }

    private static void rect(DrawContext ctx, int ox, int oy, float scale, float x0, float y0, float x1, float y1, int color) {
        int left = ox + Math.round(x0 * scale);
        int top = oy + Math.round(y0 * scale);
        int right = ox + Math.round(x1 * scale);
        int bottom = oy + Math.round(y1 * scale);
        if (right <= left) {
            right = left + Math.max(1, Math.round(scale));
        }
        if (bottom <= top) {
            bottom = top + Math.max(1, Math.round(scale));
        }
        ctx.fill(left, top, right, bottom, color);
    }
}