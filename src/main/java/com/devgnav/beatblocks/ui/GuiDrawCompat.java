package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Draws arbitrary textures (resource-pack icons and dynamic cover textures) across MC 1.21–1.21.11.
 */
public final class GuiDrawCompat {
    @FunctionalInterface
    private interface TextureDrawer {
        void draw(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) throws ReflectiveOperationException;
    }

    private static final TextureDrawer DRAWER = probeDrawer();

    private GuiDrawCompat() {}

    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height, int textureWidth, int textureHeight) {
        if (DRAWER == null) {
            throw new IllegalStateException("No compatible DrawContext texture draw method found");
        }
        try {
            DRAWER.draw(context, texture, x, y, width, height, textureWidth, textureHeight);
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

    private static TextureDrawer probeDrawer() {
        Method texturedQuad = findTexturedQuad();
        if (texturedQuad != null) {
            return (ctx, tex, x, y, w, h, tw, th) ->
                    texturedQuad.invoke(ctx, tex, x, y, x + w, y + h, 0f, 0f, 1f, 1f);
        }

        Method functionDraw = findFunctionDrawTexture();
        Function<Identifier, RenderLayer> layerGetter = findLayerGetter();
        if (functionDraw != null && layerGetter != null) {
            return (ctx, tex, x, y, w, h, tw, th) ->
                    functionDraw.invoke(ctx, layerGetter, tex, x, y, 0f, 0f, w, h, tw, th);
        }

        Object guiPipeline = findGuiTexturedPipeline();
        Method pipelineDraw = findPipelineDrawTexture();
        if (pipelineDraw != null && guiPipeline != null) {
            return (ctx, tex, x, y, w, h, tw, th) ->
                    pipelineDraw.invoke(ctx, guiPipeline, tex, x, y, 0f, 0f, w, h, tw, th);
        }

        Method legacyUv = findLegacyUvDrawTexture();
        if (legacyUv != null) {
            return (ctx, tex, x, y, w, h, tw, th) ->
                    legacyUv.invoke(ctx, tex, x, y, 0f, 0f, w, h, tw, th);
        }

        Method legacySimple = findLegacySimpleDrawTexture();
        if (legacySimple != null) {
            return (ctx, tex, x, y, w, h, tw, th) ->
                    legacySimple.invoke(ctx, tex, x, y, w, h, tw, th);
        }

        Method functionGui = findFunctionDrawGuiTexture();
        if (functionGui != null && layerGetter != null) {
            return (ctx, tex, x, y, w, h, tw, th) ->
                    functionGui.invoke(ctx, layerGetter, tex, x, y, w, h);
        }

        Method pipelineGui = findPipelineDrawGuiTexture();
        if (pipelineGui != null && guiPipeline != null) {
            return (ctx, tex, x, y, w, h, tw, th) ->
                    pipelineGui.invoke(ctx, guiPipeline, tex, x, y, w, h);
        }

        return null;
    }

    private static Method findTexturedQuad() {
        try {
            return DrawContext.class.getMethod(
                    "drawTexturedQuad",
                    Identifier.class,
                    int.class, int.class, int.class, int.class,
                    float.class, float.class, float.class, float.class
            );
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method findFunctionDrawTexture() {
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawTexture".equals(method.getName()) || method.getParameterCount() != 10) {
                continue;
            }
            Class<?>[] p = method.getParameterTypes();
            if (p[0] == Function.class && p[1] == Identifier.class && p[4] == float.class) {
                return method;
            }
        }
        return null;
    }

    private static Method findPipelineDrawTexture() {
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawTexture".equals(method.getName()) || method.getParameterCount() != 10) {
                continue;
            }
            Class<?>[] p = method.getParameterTypes();
            if (p[0].getSimpleName().equals("RenderPipeline") && p[1] == Identifier.class && p[4] == float.class) {
                return method;
            }
        }
        return null;
    }

    private static Method findLegacyUvDrawTexture() {
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawTexture".equals(method.getName()) || method.getParameterCount() != 10) {
                continue;
            }
            Class<?>[] p = method.getParameterTypes();
            if (p[0] == Identifier.class && p[3] == float.class) {
                return method;
            }
        }
        return null;
    }

    private static Method findLegacySimpleDrawTexture() {
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawTexture".equals(method.getName()) || method.getParameterCount() != 7) {
                continue;
            }
            Class<?>[] p = method.getParameterTypes();
            if (p[0] == Identifier.class) {
                return method;
            }
        }
        return null;
    }

    private static Method findFunctionDrawGuiTexture() {
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawGuiTexture".equals(method.getName()) || method.getParameterCount() != 6) {
                continue;
            }
            Class<?>[] p = method.getParameterTypes();
            if (p[0] == Function.class && p[1] == Identifier.class) {
                return method;
            }
        }
        return null;
    }

    private static Method findPipelineDrawGuiTexture() {
        for (Method method : DrawContext.class.getMethods()) {
            if (!"drawGuiTexture".equals(method.getName()) || method.getParameterCount() != 6) {
                continue;
            }
            Class<?>[] p = method.getParameterTypes();
            if (p[0].getSimpleName().equals("RenderPipeline") && p[1] == Identifier.class) {
                return method;
            }
        }
        return null;
    }

    private static Object findGuiTexturedPipeline() {
        for (String className : new String[]{
                "net.minecraft.client.gl.RenderPipelines",
                "net.minecraft.client.render.RenderPipelines"
        }) {
            try {
                Class<?> type = Class.forName(className);
                for (Field field : type.getFields()) {
                    if ("GUI_TEXTURED".equals(field.getName())) {
                        return field.get(null);
                    }
                }
            } catch (ReflectiveOperationException ignored) {}
        }
        return null;
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