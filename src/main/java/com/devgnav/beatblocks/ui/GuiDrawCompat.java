package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Draws arbitrary textures (resource-pack icons and dynamic cover textures) across MC 1.21–1.21.11.
 */
public final class GuiDrawCompat {
    private static final Method DRAW_TEXTURE;
    private static final Function<Identifier, RenderLayer> LAYER_GETTER;

    static {
        Method drawTexture = null;
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawTexture".equals(method.getName()) || method.getParameterCount() != 9) {
                continue;
            }
            Class<?>[] p = method.getParameterTypes();
            if (p[0] != Function.class || p[1] != Identifier.class || p[4] != float.class) {
                continue;
            }
            drawTexture = method;
            break;
        }
        DRAW_TEXTURE = drawTexture;
        LAYER_GETTER = findLayerGetter();
    }

    private GuiDrawCompat() {}

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        if (DRAW_TEXTURE == null || LAYER_GETTER == null) {
            throw new IllegalStateException("No compatible DrawContext.drawTexture method found");
        }
        try {
            DRAW_TEXTURE.invoke(context, LAYER_GETTER, texture, x, y, 0f, 0f, width, height, textureWidth, textureHeight);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to draw texture " + texture, e);
        }
    }

    public static void drawGuiTexture(DrawContext context, Identifier texture, int x, int y, int size) {
        drawGuiTexture(context, texture, x, y, size, size, size, size);
    }

    public static void drawGuiTexture(DrawContext context, Identifier texture, int x, int y, int w, int h, int textureWidth, int textureHeight) {
        drawTexture(context, texture, x, y, w, h, textureWidth, textureHeight);
    }

    @SuppressWarnings("unchecked")
    private static Function<Identifier, RenderLayer> findLayerGetter() {
        for (String name : new String[]{"getGuiTextured", "getGuiOpaqueTextured"}) {
            try {
                Method method = RenderLayer.class.getMethod(name, Identifier.class);
                return id -> {
                    try {
                        return (RenderLayer) method.invoke(null, id);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                };
            } catch (NoSuchMethodException ignored) {}
        }
        for (Method method : RenderLayer.class.getMethods()) {
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != Identifier.class) {
                continue;
            }
            if (!RenderLayer.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }
            String name = method.getName();
            if (name.contains("Gui") && name.contains("Textur")) {
                return id -> {
                    try {
                        return (RenderLayer) method.invoke(null, id);
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                };
            }
        }
        return null;
    }
}