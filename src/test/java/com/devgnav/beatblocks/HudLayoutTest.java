package com.devgnav.beatblocks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HUD scaling math tests. The renderer uses GUI-space dimensions so the HUD
 * stays readable across Minecraft GUI scale settings.
 * Tests the exact same formula used in BeatBlocksHudRenderer.
 */
public class HudLayoutTest {

    private static final int BASE_WIDTH = 190;
    private static final int BASE_HEIGHT = 42;
    private static final int BASE_PADDING = 4;
    private static final double HUD_SCALE_MIN = 0.35;
    private static final double HUD_SCALE_MAX = 3.0;

    /**
     * Compute HUD dimensions in GUI units for the given scaled screen size.
     * This mirrors the formula in BeatBlocksHudRenderer exactly.
     */
    private static int[] computeHud(int screenW, int screenH, double hudScaleMultiplier) {
        double hudScale = Math.max(HUD_SCALE_MIN, Math.min(HUD_SCALE_MAX, hudScaleMultiplier));

        int w = Math.max(96, Math.min((int) Math.round(BASE_WIDTH * hudScale), Math.max(96, screenW - 4)));
        int h = Math.max(24, Math.min((int) Math.round(BASE_HEIGHT * hudScale), Math.max(24, screenH / 4)));
        int p = Math.max(2, Math.min((int) Math.round(BASE_PADDING * hudScale), 10));

        return new int[]{w, h, p};
    }

    @Test
    void testBaseline() {
        int[] r = computeHud(960, 540, 1.0);
        assertEquals(190, r[0], "GUI width at 1.0x");
        assertEquals(42, r[1], "GUI height at 1.0x");
        assertEquals(4, r[2], "GUI padding at 1.0x");
    }

    @ParameterizedTest(name = "Scaled resolution {0}x{1}")
    @CsvSource({
            "427, 240",
            "640, 360",
            "854, 480",
            "960, 540",
            "1280, 720",
            "1920, 1080",
    })
    void testHudReadable(int screenW, int screenH) {
        int[] r = computeHud(screenW, screenH, 1.0);
        assertTrue(r[0] >= 96, "GUI width must be >= 96: was " + r[0]);
        assertTrue(r[1] >= 24, "GUI height must be >= 24: was " + r[1]);
        assertTrue(r[2] >= 2, "GUI padding must be >= 2: was " + r[2]);
        assertTrue(r[2] <= 10, "GUI padding must be <= 10: was " + r[2]);
    }

    @Test
    void testScaleMultiplier() {
        int[] normal = computeHud(960, 540, 1.0);
        int[] large = computeHud(960, 540, 1.5);
        int[] small = computeHud(960, 540, 0.7);

        assertTrue(large[0] > normal[0], "1.5x multiplier should increase width");
        assertTrue(small[0] < normal[0], "0.7x multiplier should decrease width");
    }

    @Test
    void testSmallResolution_clamps() {
        int[] r = computeHud(160, 90, 1.0);
        assertTrue(r[0] >= 96, "Must clamp width to >= 96");
        assertTrue(r[1] >= 24, "Must clamp height to >= 24");
    }

    @Test
    void testScaleIsClamped() {
        int[] tiny = computeHud(960, 540, 0.01);
        int[] huge = computeHud(960, 540, 99.0);

        assertEquals(96, tiny[0]);
        assertTrue(huge[0] <= 960 - 4);
    }
}
