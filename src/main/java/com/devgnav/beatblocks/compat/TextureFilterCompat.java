package com.devgnav.beatblocks.compat;

import net.minecraft.client.texture.NativeImageBackedTexture;

import java.lang.reflect.Method;

public final class TextureFilterCompat {
    private static final Method SET_FILTER;

    static {
        Method setFilter = null;
        for (Method method : NativeImageBackedTexture.class.getMethods()) {
            if (!"setFilter".equals(method.getName())) continue;
            Class<?>[] p = method.getParameterTypes();
            if (p.length == 2 && p[0] == boolean.class && p[1] == boolean.class) {
                setFilter = method;
                break;
            }
        }
        SET_FILTER = setFilter;
    }

    private TextureFilterCompat() {}

    public static void setBilinear(NativeImageBackedTexture texture) {
        if (SET_FILTER == null) return;
        try {
            SET_FILTER.invoke(texture, true, false);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}