package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * HUD text scaling across MatrixStack (1.21.x) and Matrix3x2fStack APIs.
 * Never fails during class load — probes lazily and falls back to unscaled text.
 */
final class DrawMatrixCompat {
    private static volatile boolean probed;
    private static volatile boolean scalingAvailable;

    private static Method getMatricesMethod;
    private static Method pushMethod;
    private static Method popMethod;
    private static Method translateMethod;
    private static Method scaleMethod;
    private static boolean translate2d;
    private static boolean scale2d;

    private static Field matricesField;

    private DrawMatrixCompat() {}

    static void drawScaledText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, float scale) {
        if (scale >= 0.99f && scale <= 1.01f) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
            return;
        }

        ensureProbed();
        if (!scalingAvailable) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
            return;
        }

        try {
            Object matrices = resolveMatrices(context);
            if (matrices == null) {
                context.drawTextWithShadow(textRenderer, text, x, y, color);
                return;
            }
            if (pushMethod != null) pushMethod.invoke(matrices);
            if (translate2d) {
                translateMethod.invoke(matrices, (float) x, (float) y);
            } else {
                translateMethod.invoke(matrices, (float) x, (float) y, 0f);
            }
            if (scale2d) {
                scaleMethod.invoke(matrices, scale, scale);
            } else {
                scaleMethod.invoke(matrices, scale, scale, 1.0f);
            }
            context.drawTextWithShadow(textRenderer, text, 0, 0, color);
            if (popMethod != null) popMethod.invoke(matrices);
        } catch (ReflectiveOperationException e) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
        }
    }

    private static synchronized void ensureProbed() {
        if (probed) return;
        probed = true;
        try {
            getMatricesMethod = findMatricesGetter();
            if (getMatricesMethod == null) {
                matricesField = findMatricesField();
            }
            if (getMatricesMethod == null && matricesField == null) {
                scalingAvailable = false;
                return;
            }

            Class<?> matrixType = getMatricesMethod != null
                    ? getMatricesMethod.getReturnType()
                    : matricesField.getType();
            probeMatrixMethods(matrixType);
            scalingAvailable = translateMethod != null && scaleMethod != null;
        } catch (ReflectiveOperationException e) {
            scalingAvailable = false;
        }
    }

    private static Method findMatricesGetter() {
        for (Method method : DrawContext.class.getMethods()) {
            if (method.getParameterCount() != 0) continue;
            String name = method.getName();
            if ("getMatrices".equals(name) || "getMatrix".equals(name) || name.endsWith("Matrices")) {
                return method;
            }
        }
        for (Method method : DrawContext.class.getDeclaredMethods()) {
            if (method.getParameterCount() != 0) continue;
            String returnName = method.getReturnType().getSimpleName();
            if (returnName.contains("Matrix")) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    private static Field findMatricesField() throws ReflectiveOperationException {
        for (Field field : DrawContext.class.getDeclaredFields()) {
            String typeName = field.getType().getSimpleName();
            if (typeName.contains("Matrix")) {
                field.setAccessible(true);
                return field;
            }
        }
        return null;
    }

    private static void probeMatrixMethods(Class<?> matrixType) throws ReflectiveOperationException {
        for (String name : new String[]{"push", "pushMatrix"}) {
            try {
                pushMethod = matrixType.getMethod(name);
                break;
            } catch (NoSuchMethodException ignored) {}
        }
        for (String name : new String[]{"pop", "popMatrix"}) {
            try {
                popMethod = matrixType.getMethod(name);
                break;
            } catch (NoSuchMethodException ignored) {}
        }

        try {
            translateMethod = matrixType.getMethod("translate", float.class, float.class);
            translate2d = true;
        } catch (NoSuchMethodException e) {
            translateMethod = matrixType.getMethod("translate", float.class, float.class, float.class);
            translate2d = false;
        }

        try {
            scaleMethod = matrixType.getMethod("scale", float.class, float.class);
            scale2d = true;
        } catch (NoSuchMethodException e) {
            scaleMethod = matrixType.getMethod("scale", float.class, float.class, float.class);
            scale2d = false;
        }
    }

    private static Object resolveMatrices(DrawContext context) throws ReflectiveOperationException {
        if (getMatricesMethod != null) {
            return getMatricesMethod.invoke(context);
        }
        if (matricesField != null) {
            return matricesField.get(context);
        }
        return null;
    }
}