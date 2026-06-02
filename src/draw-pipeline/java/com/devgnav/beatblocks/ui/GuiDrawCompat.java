package com.devgnav.beatblocks.ui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** MC 1.21.7+: {@link RenderPipelines#GUI_TEXTURED} draw API. */
public final class GuiDrawCompat {
    private GuiDrawCompat() {}

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        try {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, width, height, textureWidth, textureHeight);
            return;
        } catch (Throwable ignored) {
        }
        try {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, width, height, textureWidth, textureHeight, 0xFFFFFFFF);
            return;
        } catch (Throwable t) {
            drawFallback(context, x, y, width, height);
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