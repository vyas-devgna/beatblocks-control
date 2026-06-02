package com.devgnav.beatblocks.compat;

import net.minecraft.util.Identifier;

import java.lang.reflect.Method;

public final class IdentifierCompat {
    private static final Method OF_TWO;
    private static final Method TRY_PARSE_TWO;

    static {
        Method ofTwo = null;
        Method tryParse = null;
        try {
            ofTwo = Identifier.class.getMethod("of", String.class, String.class);
        } catch (NoSuchMethodException ignored) {}
        try {
            tryParse = Identifier.class.getMethod("tryParse", String.class, String.class);
        } catch (NoSuchMethodException ignored) {}
        OF_TWO = ofTwo;
        TRY_PARSE_TWO = tryParse;
    }

    private IdentifierCompat() {}

    public static Identifier of(String namespace, String path) {
        if (OF_TWO != null) {
            try {
                return (Identifier) OF_TWO.invoke(null, namespace, path);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        if (TRY_PARSE_TWO != null) {
            try {
                Identifier id = (Identifier) TRY_PARSE_TWO.invoke(null, namespace, path);
                if (id != null) return id;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("No compatible Identifier factory");
    }
}