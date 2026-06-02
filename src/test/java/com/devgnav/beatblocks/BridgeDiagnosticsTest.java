package com.devgnav.beatblocks;

import com.devgnav.beatblocks.model.BridgeDiagnostics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BridgeDiagnostics record — health checks and status summaries.
 */
public class BridgeDiagnosticsTest {

    @Test
    void healthy_whenAllGood() {
        BridgeDiagnostics d = new BridgeDiagnostics(
                true, true, System.currentTimeMillis(),
                "", 50321, "1.0.0", "2.2.0", 1
        );
        assertTrue(d.isHealthy());
        assertTrue(d.statusSummary().contains("Connected"));
    }

    @Test
    void unhealthy_whenBridgeNotRunning() {
        BridgeDiagnostics d = new BridgeDiagnostics(
                false, false, 0,
                "Port in use", 50321, "", "", 0
        );
        assertFalse(d.isHealthy());
        assertFalse(d.bridgeRunning());
    }

    @Test
    void unhealthy_whenExtensionNotConnected() {
        BridgeDiagnostics d = new BridgeDiagnostics(
                true, false, 0,
                "", 50321, "", "", 0
        );
        assertFalse(d.isHealthy());
        assertTrue(d.bridgeRunning());
        assertFalse(d.extensionConnected());
    }

    @Test
    void unhealthy_whenHeartbeatStale() {
        BridgeDiagnostics d = new BridgeDiagnostics(
                true, true, System.currentTimeMillis() - 60_000,
                "", 50321, "1.0.0", "2.2.0", 1
        );
        assertFalse(d.isHealthy());
    }

    @Test
    void statusSummary_bridgeNotRunning() {
        BridgeDiagnostics d = new BridgeDiagnostics(
                false, false, 0, "", 50321, "", "", 0
        );
        assertEquals("Bridge server not running", d.statusSummary());
    }

    @Test
    void statusSummary_waitingForExtension() {
        BridgeDiagnostics d = new BridgeDiagnostics(
                true, false, 0, "", 50321, "", "", 0
        );
        assertTrue(d.statusSummary().contains("Waiting"));
    }

    @Test
    void statusSummary_heartbeatLost() {
        BridgeDiagnostics d = new BridgeDiagnostics(
                true, true, System.currentTimeMillis() - 30_000,
                "", 50321, "1.0.0", "2.2.0", 1
        );
        assertTrue(d.statusSummary().contains("heartbeat lost"));
    }

    @Test
    void offline_factory() {
        BridgeDiagnostics d = BridgeDiagnostics.offline(50321);
        assertFalse(d.bridgeRunning());
        assertFalse(d.isHealthy());
        assertEquals(50321, d.port());
    }
}
