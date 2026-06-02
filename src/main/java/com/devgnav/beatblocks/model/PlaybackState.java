package com.devgnav.beatblocks.model;

public record PlaybackState(
        boolean active,
        String deviceName,
        boolean playing,
        int progressMs,
        int volumePercent,
        boolean shuffle,
        String repeatState,
        BeatBlocksItem track
) {
    public static PlaybackState inactive() {
        return new PlaybackState(false, "No active BeatBlocks device", false, 0, 0, false, "off", null);
    }
}
