package com.devgnav.beatblocks.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

/**
 * Cross-version key state for modifier checks (e.g. Alt+I overlay).
 * Probes {@link InputUtil#isKeyPressed} when present; always falls back to GLFW
 * so modpacks that mixin/replace InputUtil cannot crash the client.
 */
public final class InputUtilCompat {
    private static volatile boolean probed;
    private static Method keyPressedMethod;
    private static boolean firstParamIsLong;

    private InputUtilCompat() {}

    public static boolean isKeyPressed(MinecraftClient client, int glfwKey) {
        if (client == null || client.getWindow() == null) {
            return false;
        }
        long windowHandle = client.getWindow().getHandle();

        ensureProbed();
        if (keyPressedMethod != null) {
            try {
                Object arg0 = firstParamIsLong ? windowHandle : client.getWindow();
                return (boolean) keyPressedMethod.invoke(null, arg0, glfwKey);
            } catch (ReflectiveOperationException ignored) {
                // fall through to GLFW
            }
        }

        return GLFW.glfwGetKey(windowHandle, glfwKey) == GLFW.GLFW_PRESS;
    }

    private static synchronized void ensureProbed() {
        if (probed) {
            return;
        }
        probed = true;
        keyPressedMethod = findKeyPressedMethod(InputUtil.class.getMethods());
        if (keyPressedMethod == null) {
            keyPressedMethod = findKeyPressedMethod(InputUtil.class.getDeclaredMethods());
        }
        if (keyPressedMethod != null) {
            Class<?> first = keyPressedMethod.getParameterTypes()[0];
            firstParamIsLong = first == long.class;
        }
    }

    private static Method findKeyPressedMethod(Method[] methods) {
        Method longSig = null;
        Method windowSig = null;
        for (Method method : methods) {
            if (!"isKeyPressed".equals(method.getName()) || method.getParameterCount() != 2) {
                continue;
            }
            Class<?>[] params = method.getParameterTypes();
            if (params[1] != int.class) {
                continue;
            }
            if (params[0] == long.class) {
                longSig = method;
            } else if (params[0].getSimpleName().contains("Window")) {
                windowSig = method;
                if (!method.canAccess(null)) {
                    method.setAccessible(true);
                }
            }
        }
        // Prefer long handle (1.21–1.21.8); Window param on 1.21.9+
        if (longSig != null) {
            return longSig;
        }
        return windowSig;
    }
}