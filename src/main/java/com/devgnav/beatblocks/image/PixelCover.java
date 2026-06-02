package com.devgnav.beatblocks.image;

public record PixelCover(int width, int height, int[] argb) {
    public int colorAt(int x, int y) {
        return argb[y * width + x];
    }
}
