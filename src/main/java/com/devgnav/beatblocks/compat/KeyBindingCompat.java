package com.devgnav.beatblocks.compat;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class KeyBindingCompat {
    private static final Constructor<?> CONSTRUCTOR;
    private static final boolean CATEGORY_ENUM;

    static {
        Constructor<?> ctor = null;
        boolean enumCategory = false;
        for (Constructor<?> candidate : KeyBinding.class.getConstructors()) {
            Class<?>[] p = candidate.getParameterTypes();
            if (p.length != 4 || p[0] != String.class || p[1] != InputUtil.Type.class || p[2] != int.class) {
                continue;
            }
            ctor = candidate;
            enumCategory = p[3].getName().contains("Category");
            break;
        }
        if (ctor == null) {
            throw new IllegalStateException("No compatible KeyBinding constructor");
        }
        CONSTRUCTOR = ctor;
        CATEGORY_ENUM = enumCategory;
    }

    private KeyBindingCompat() {}

    private static Object resolveCategory() throws ReflectiveOperationException {
        Class<?> categoryClass = Class.forName("net.minecraft.client.option.KeyBinding$Category");
        try {
            Method create = categoryClass.getMethod("create", Identifier.class);
            return create.invoke(null, IdentifierCompat.of("beatblocks", "main"));
        } catch (NoSuchMethodException e) {
            return categoryClass.getField("MISC").get(null);
        }
    }

    public static KeyBinding create(String translationKey, InputUtil.Type type, int code, String category) {
        try {
            Object categoryArg = CATEGORY_ENUM ? resolveCategory() : category;
            return (KeyBinding) CONSTRUCTOR.newInstance(translationKey, type, code, categoryArg);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create KeyBinding", e);
        }
    }
}