package com.devgnav.beatblocks.mode;

import com.devgnav.beatblocks.config.BeatBlocksConfig;
import com.devgnav.beatblocks.model.BridgeDiagnostics;
import com.devgnav.beatblocks.spotify.BeatBlocksApiClient;

/**
 * Two-mode state machine: DEFAULT and ENHANCED.
 * Enhanced mode is only effective when the Spicetify bridge is healthy.
 */
public final class ModeManager {

    public enum Mode {
        DEFAULT,
        ENHANCED
    }

    private final BeatBlocksConfig config;
    private final BeatBlocksApiClient apiClient;

    public ModeManager(BeatBlocksConfig config, BeatBlocksApiClient apiClient) {
        this.config = config;
        this.apiClient = apiClient;
    }

    /** The mode the user has selected (persisted in config). */
    public Mode getSelectedMode() {
        return parseMode(config.selectedMode);
    }

    /** Set the user's selected mode and persist to config. */
    public void setSelectedMode(Mode mode) {
        config.selectedMode = mode.name();
        config.save();
    }

    /**
     * The mode that is actually active right now.
     * Returns ENHANCED only if selected AND bridge is healthy.
     * Falls back to DEFAULT otherwise.
     */
    public Mode getEffectiveMode() {
        if (getSelectedMode() == Mode.ENHANCED && isEnhancedAvailable()) {
            return Mode.ENHANCED;
        }
        return Mode.DEFAULT;
    }

    /** Whether Enhanced mode can be activated right now. */
    public boolean isEnhancedAvailable() {
        return apiClient.getDiagnostics().isHealthy();
    }

    /**
     * Human-readable reason why Enhanced mode is unavailable, or null if available.
     */
    public String getUnavailableReason() {
        if (isEnhancedAvailable()) return null;
        BridgeDiagnostics diag = apiClient.getDiagnostics();
        if (!diag.bridgeRunning()) {
            return "Bridge server failed to start (port " + diag.port() + " may be in use)";
        }
        if (!diag.extensionConnected()) {
            return "Spicetify extension not connected — is your desktop player open with Spicetify applied?";
        }
        long ago = (System.currentTimeMillis() - diag.lastHeartbeatAt()) / 1000;
        if (ago > 10) {
            return "Extension heartbeat lost (" + ago + "s ago) — desktop player may have closed or restarted";
        }
        return "Enhanced Mode requires desktop player + Spicetify bridge extension";
    }

    /** Short status line for display. */
    public String getStatusLine() {
        Mode effective = getEffectiveMode();
        if (effective == Mode.ENHANCED) {
            return "Enhanced Mode — Connected";
        }
        if (getSelectedMode() == Mode.ENHANCED) {
            return "Enhanced Mode selected but unavailable";
        }
        return "Default Mode";
    }

    private static Mode parseMode(String value) {
        if (value == null) return Mode.DEFAULT;
        try {
            return Mode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mode.DEFAULT;
        }
    }
}
