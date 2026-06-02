package com.devgnav.beatblocks.model;

import java.util.List;

public record BeatBlocksPage(List<BeatBlocksItem> items, String nextUrl) {
    public static BeatBlocksPage empty() {
        return new BeatBlocksPage(List.of(), null);
    }

    public boolean hasMore() {
        return nextUrl != null && !nextUrl.isBlank();
    }
}
