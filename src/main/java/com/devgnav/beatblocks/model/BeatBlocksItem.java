package com.devgnav.beatblocks.model;

public record BeatBlocksItem(
        BeatBlocksItemType type,
        String id,
        String name,
        String subtitle,
        String uri,
        String imageUrl,
        String externalUrl,
        int durationMs
) {
    public boolean playable() {
        return uri != null && !uri.isBlank();
    }

    public String kindLabel() {
        return switch (type) {
            case TRACK -> "Track";
            case ALBUM -> "Album";
            case PLAYLIST -> "Playlist";
            case ARTIST -> "Artist";
        };
    }
}
