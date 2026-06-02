# Security & Privacy

## Overview

BeatBlocks Control is a **client-side-only** Minecraft Fabric mod that controls a locally running desktop music player through a local Spicetify extension. This document describes the security boundaries and privacy properties.

## No API Credentials

- The mod **never** asks for or stores a music service username, password, or API token.
- The mod does **not** use OAuth, client IDs, client secrets, or developer dashboards.
- There is **no** browser login flow in the mod.
- The mod does **not** call remote music APIs directly.

## Localhost-Only Bridge

- The HTTP bridge server binds exclusively to `127.0.0.1` (localhost) on a configurable port (default `50321`).
- The bridge is **not** accessible from other machines on the network.
- Communication is plain HTTP over localhost — no TLS is needed because traffic never leaves the machine.
- The bridge only accepts connections from the Spicetify extension running inside the local desktop player.

## No Hidden Background Services

- The mod does **not** install system services, scheduled tasks, startup entries, or background daemons.
- The bridge server starts when Minecraft launches and stops when Minecraft closes.
- No processes persist after Minecraft exits.

## No Data Collection

- No analytics, telemetry, crash reporting, or tracking of any kind.
- No data is sent to any remote server by the mod (cover art may be fetched from the player's CDN via HTTPS).
- Cover art images are cached locally in the Minecraft config directory.

## Setup Scripts

- The optional `scripts/setup-spicetify-bridge.ps1` is a **manual user tool**.
- It is **never** run automatically by the mod.
- Every step requires explicit user confirmation before executing.
- The script only copies a JavaScript file and runs standard Spicetify CLI commands.

## No Raw Audio

- The mod does **not** stream, download, decode, or play audio.
- All audio playback is handled natively by the desktop player.
- The mod only sends control commands (play, pause, next, etc.) and displays metadata (song name, artist, cover art URL).

## File System Access

The mod writes only to:
- `.minecraft/config/beatblocks/` — configuration JSON
- `.minecraft/config/beatblocks/covers/` — cached album cover art PNG files

## Network Access

- **Inbound**: Localhost HTTP server on `127.0.0.1:50321` (configurable)
- **Outbound**: HTTPS for album cover art images only (when URLs are provided by the bridge)

## Source Code

The complete source code is available for audit:
- Java mod source: `src/main/java/com/devgnav/beatblocks/`
- Spicetify extension: `beatblocks-api.js`

Both are MIT licensed.