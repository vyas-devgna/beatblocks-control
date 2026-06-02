package com.devgnav.beatblocks.image;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

import java.io.IOException;

/** MC 1.21–1.21.1: PNG decode + single-arg dynamic texture. */
final class NativeImageCompat {
    private NativeImageCompat() {}

    static NativeImage readRgbaPng(byte[] pngBytes) throws IOException {
        return NativeImage.read(pngBytes);
    }

    static NativeImageBackedTexture createTexture(NativeImage image) {
        return new NativeImageBackedTexture(image);
    }
}