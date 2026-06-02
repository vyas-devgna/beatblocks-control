package com.devgnav.beatblocks.image;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Minecraft 1.21–1.21.5 differ in {@link NativeImage} pixel APIs and texture constructors.
 */
final class NativeImageCompat {
    private static final Method SET_COLOR;
    private static final Method SET_COLOR_ARGB;
    private static final Constructor<NativeImageBackedTexture> TEXTURE_WITH_IMAGE;
    private static final Constructor<NativeImageBackedTexture> TEXTURE_WITH_LABEL;

    static {
        Method setColor = null;
        Method setColorArgb = null;
        Constructor<NativeImageBackedTexture> withImage = null;
        Constructor<NativeImageBackedTexture> withLabel = null;

        for (Method method : NativeImage.class.getDeclaredMethods()) {
            if (method.getParameterCount() != 3) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params[0] != int.class || params[1] != int.class || params[2] != int.class) continue;
            String name = method.getName();
            if ("setColor".equals(name)) {
                setColor = method;
            } else if ("setColorArgb".equals(name) || "setPixelRGBA".equals(name)) {
                setColorArgb = method;
            }
        }

        for (Constructor<?> ctor : NativeImageBackedTexture.class.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 1 && params[0] == NativeImage.class) {
                @SuppressWarnings("unchecked")
                Constructor<NativeImageBackedTexture> typed = (Constructor<NativeImageBackedTexture>) ctor;
                withImage = typed;
            } else if (params.length == 2 && params[1] == NativeImage.class) {
                @SuppressWarnings("unchecked")
                Constructor<NativeImageBackedTexture> typed = (Constructor<NativeImageBackedTexture>) ctor;
                withLabel = typed;
            }
        }

        SET_COLOR = setColor;
        SET_COLOR_ARGB = setColorArgb;
        TEXTURE_WITH_IMAGE = withImage;
        TEXTURE_WITH_LABEL = withLabel;
    }

    private NativeImageCompat() {}

    static void writePixel(NativeImage image, int x, int y, int argb) {
        int a = (argb >> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int abgr = (a << 24) | (b << 16) | (g << 8) | r;

        if (SET_COLOR_ARGB != null) {
            invoke(SET_COLOR_ARGB, image, x, y, argb);
            return;
        }
        if (SET_COLOR != null) {
            invoke(SET_COLOR, image, x, y, abgr);
            return;
        }
        throw new IllegalStateException("No compatible NativeImage pixel setter found");
    }

    static NativeImageBackedTexture createTexture(NativeImage image) {
        try {
            if (TEXTURE_WITH_LABEL != null) {
                TEXTURE_WITH_LABEL.setAccessible(true);
                return TEXTURE_WITH_LABEL.newInstance((Object) (java.util.function.Supplier<String>) () -> "beatblocks_cover", image);
            }
            if (TEXTURE_WITH_IMAGE != null) {
                TEXTURE_WITH_IMAGE.setAccessible(true);
                return TEXTURE_WITH_IMAGE.newInstance(image);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not construct NativeImageBackedTexture", e);
        }
        throw new IllegalStateException("No compatible NativeImageBackedTexture constructor found");
    }

    private static void invoke(Method method, NativeImage image, int x, int y, int color) {
        try {
            method.setAccessible(true);
            method.invoke(image, x, y, color);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to write NativeImage pixel", e);
        }
    }
}