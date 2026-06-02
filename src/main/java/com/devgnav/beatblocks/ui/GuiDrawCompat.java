package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * DrawContext texture APIs changed between Minecraft 1.21 and 1.21.5.
 */
public final class GuiDrawCompat {
    private static final Method DRAW_WITH_LAYER = findDrawWithLayer();
    private static final Method DRAW_LEGACY = findDrawLegacy();
    private static final Function<Identifier, RenderLayer> LAYER_GETTER = findLayerGetter();

    private GuiDrawCompat() {}

    public static void drawGuiTexture(DrawContext context, Identifier texture, int x, int y, int size) {
        drawGuiTexture(context, texture, x, y, size, size, size, size);
    }

    public static void drawGuiTexture(DrawContext context, Identifier texture, int x, int y, int w, int h, int textureWidth, int textureHeight) {
        int texW = textureWidth;
        int texH = textureHeight;
        try {
            if (DRAW_WITH_LAYER != null && LAYER_GETTER != null) {
                DRAW_WITH_LAYER.invoke(context, LAYER_GETTER, texture, x, y, 0f, 0f, w, h, texW, texH);
                return;
            }
            if (DRAW_LEGACY != null) {
                DRAW_LEGACY.invoke(context, texture, x, y, 0f, 0f, w, h, texW, texH);
                return;
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to draw GUI texture", e);
        }
        throw new IllegalStateException("No compatible DrawContext.drawTexture method found");
    }

    private static Method findDrawWithLayer() {
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawTexture".equals(method.getName()) || method.getParameterCount() != 9) continue;
            Class<?>[] p = method.getParameterTypes();
            if (p[0] != Function.class || p[1] != Identifier.class) continue;
            return method;
        }
        return null;
    }

    private static Method findDrawLegacy() {
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawTexture".equals(method.getName()) || method.getParameterCount() != 9) continue;
            Class<?>[] p = method.getParameterTypes();
            if (p[0] != Identifier.class) continue;
            return method;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Function<Identifier, RenderLayer> findLayerGetter() {
        for (Method method : RenderLayer.class.getMethods()) {
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != Identifier.class) continue;
            if (!RenderLayer.class.isAssignableFrom(method.getReturnType())) continue;
            String name = method.getName();
            if (name.startsWith("getGui") || "getTextured".equals(name)) {
                return id -> {
                    try {
                        return (RenderLayer) method.invoke(null, id);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
        }
        for (Method method : RenderLayer.class.getMethods()) {
            if (method.getParameterCount() != 0 || !RenderLayer.class.isAssignableFrom(method.getReturnType())) continue;
            String name = method.getName();
            if ("getGuiTextured".equals(name) || "getGui".equals(name) || "getGuiOpaqueTextured".equals(name)) {
                try {
                    RenderLayer layer = (RenderLayer) method.invoke(null);
                    return id -> layer;
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return null;
    }
}