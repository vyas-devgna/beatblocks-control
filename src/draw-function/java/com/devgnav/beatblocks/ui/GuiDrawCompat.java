package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/** MC 1.21.2–1.21.6: {@link RenderLayer} function draw API. */
public final class GuiDrawCompat {
    private GuiDrawCompat() {}

    /**
     * Draws a texture region scaled to screen size.
     *
     * @param screenW drawn width on screen
     * @param screenH drawn height on screen
     * @param regionW source region width in texture pixels
     * @param regionH source region height in texture pixels
     */
    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int screenW, int screenH, int regionW, int regionH) {
        try {
            context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0f, 0f, screenW, screenH, regionW, regionH, regionW, regionH, 0xFFFFFFFF);
            return;
        } catch (Throwable ignored) {
        }
        try {
            context.drawTexture(RenderLayer::getGuiTextured, texture, x, y, 0f, 0f, screenW, screenH, regionW, regionH);
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