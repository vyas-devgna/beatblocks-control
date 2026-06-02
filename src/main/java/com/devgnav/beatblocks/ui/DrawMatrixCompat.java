package com.devgnav.beatblocks.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.lang.reflect.Method;

/**
 * HUD text scaling uses matrix stacks that changed from 3D MatrixStack to 2D Matrix3x2fStack in newer Minecraft.
 */
final class DrawMatrixCompat {
    private static final Method PUSH;
    private static final Method POP;
    private static final Method TRANSLATE;
    private static final Method SCALE;
    private static final boolean TRANSLATE_2D;
    private static final boolean SCALE_2D;

    static {
        Method push = null;
        Method pop = null;
        Method translate = null;
        Method scale = null;
        boolean translate2d = false;
        boolean scale2d = false;

        try {
            Class<?> matrixType = DrawContext.class.getMethod("getMatrices").getReturnType();

            for (String name : new String[]{"push", "pushMatrix"}) {
                try {
                    push = matrixType.getMethod(name);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            for (String name : new String[]{"pop", "popMatrix"}) {
                try {
                    pop = matrixType.getMethod(name);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }

            try {
                translate = matrixType.getMethod("translate", float.class, float.class);
                translate2d = true;
            } catch (NoSuchMethodException e) {
                translate = matrixType.getMethod("translate", float.class, float.class, float.class);
            }

            try {
                scale = matrixType.getMethod("scale", float.class, float.class);
                scale2d = true;
            } catch (NoSuchMethodException e) {
                scale = matrixType.getMethod("scale", float.class, float.class, float.class);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not probe DrawContext matrix API", e);
        }

        PUSH = push;
        POP = pop;
        TRANSLATE = translate;
        SCALE = scale;
        TRANSLATE_2D = translate2d;
        SCALE_2D = scale2d;
    }

    private DrawMatrixCompat() {}

    static void drawScaledText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color, float scale) {
        try {
            Object matrices = context.getMatrices();
            if (PUSH != null) PUSH.invoke(matrices);
            if (TRANSLATE_2D) {
                TRANSLATE.invoke(matrices, (float) x, (float) y);
            } else {
                TRANSLATE.invoke(matrices, (float) x, (float) y, 0f);
            }
            if (SCALE_2D) {
                SCALE.invoke(matrices, scale, scale);
            } else {
                SCALE.invoke(matrices, scale, scale, 1.0f);
            }
            context.drawTextWithShadow(textRenderer, text, 0, 0, color);
            if (POP != null) POP.invoke(matrices);
        } catch (ReflectiveOperationException e) {
            context.drawTextWithShadow(textRenderer, text, x, y, color);
        }
    }
}