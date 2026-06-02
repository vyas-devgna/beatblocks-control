package com.devgnav.beatblocks.compat;

import net.minecraft.util.Identifier;

/**
 * Identifier factory for all supported MC versions (1.21–1.21.11).
 * Uses compile-time {@link Identifier#of} — do not reflect; Fabric remaps bytecode
 * but reflection with yarn names fails at runtime in production.
 */
public final class IdentifierCompat {
    private IdentifierCompat() {}

    public static Identifier of(String namespace, String path) {
        return Identifier.of(namespace, path);
    }
}