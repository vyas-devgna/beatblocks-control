package com.devgnav.beatblocks.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.lang.reflect.Method;

public final class InputUtilCompat {
    private static final Method KEY_PRESSED_LONG;
    private static final Method KEY_PRESSED_WINDOW;

    static {
        Method longMethod = null;
        Method windowMethod = null;
        for (Method method : InputUtil.class.getMethods()) {
            if (!"isKeyPressed".equals(method.getName()) || method.getParameterCount() != 2) continue;
            Class<?> p0 = method.getParameterTypes()[0];
            if (p0 == long.class) longMethod = method;
            else if (p0.getSimpleName().contains("Window")) windowMethod = method;
        }
        KEY_PRESSED_LONG = longMethod;
        KEY_PRESSED_WINDOW = windowMethod;
    }

    private InputUtilCompat() {}

    public static boolean isKeyPressed(MinecraftClient client, int key) {
        try {
            if (KEY_PRESSED_LONG != null) {
                return (boolean) KEY_PRESSED_LONG.invoke(null, client.getWindow().getHandle(), key);
            }
            if (KEY_PRESSED_WINDOW != null) {
                return (boolean) KEY_PRESSED_WINDOW.invoke(null, client.getWindow(), key);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("No compatible InputUtil.isKeyPressed");
    }
}