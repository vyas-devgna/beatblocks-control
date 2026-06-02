package com.devgnav.beatblocks.model;

public record BridgeDiagnostics(
        boolean bridgeRunning,
        boolean extensionConnected,
        long lastHeartbeatAt,
        String lastError,
        int port,
        String modVersion,
        String extensionVersion,
        int connectionCount
) {
    public boolean isHealthy() {
        return bridgeRunning && extensionConnected
                && System.currentTimeMillis() - lastHeartbeatAt < 10_000;
    }

    public String statusSummary() {
        if (!bridgeRunning) return "Bridge server not running";
        if (!extensionConnected) return "Waiting for Spicetify extension...";
        long ago = (System.currentTimeMillis() - lastHeartbeatAt) / 1000;
        if (ago > 10) return "Extension heartbeat lost (" + ago + "s ago)";
        return "Connected";
    }

    public static BridgeDiagnostics offline(int port) {
        return new BridgeDiagnostics(false, false, 0, "Bridge not started", port, "", "", 0);
    }
}
