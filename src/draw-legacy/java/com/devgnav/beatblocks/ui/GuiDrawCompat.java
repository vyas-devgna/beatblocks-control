package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** MC 1.21–1.21.1: Identifier-based {@link DrawContext#drawTexture}. */
public final class GuiDrawCompat {
    private GuiDrawCompat() {}

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int screenW, int screenH, int regionW, int regionH) {
        try {
            context.drawTexture(texture, x, y, 0f, 0f, screenW, screenH, regionW, regionH);
            return;
        } catch (Throwable t) {
            drawFallback(context, x, y, screenW, screenH);
        }
    }

    public static void drawGuiTexture(DrawContext context, Identifier texture, int x, int y, int size) {
        drawTexture(context, texture, x, y, size, size, size, size);
    }

    public static void drawGuiTexture(DrawContext context, Identifier texture, int x, int y, int w, int h, int textureWidth, int textureHeight) {
        drawTexture(context, texture, x, y, w, h, textureWidth, textureHeight);
    }

    private static void drawFallback(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0xFF2A2A2A);
    }
}