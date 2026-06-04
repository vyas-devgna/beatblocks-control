#!/usr/bin/env python3
"""Polish BeatBlocks Modrinth project metadata and fix early version entries."""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path

import requests

API = "https://api.modrinth.com/v2"
PROJECT_SLUG = "beatblocks"
FABRIC_API_PROJECT = "P7dR8mSH"
MOD_VERSION = "1.3.2"
LOADER_MIN = "0.16.10"
TARGETS_PATH = Path(__file__).resolve().parent / "fabric-targets.json"

PROJECT_BODY = """\
# BeatBlocks Control

**BeatBlocks Control** is a client-side **Fabric** mod for Minecraft Java **1.21 – 1.21.11** that lets you control **Spotify** while you play — overlay, now-playing HUD, playlists, and global hotkeys. Everything runs through a **local Spicetify bridge** on your PC. No OAuth, no API keys, and no login inside the mod.

## Features

- **In-game music control** — play, pause, skip, and browse your library without alt-tabbing
- **Now-playing HUD** — track, artist, and cached album art on screen
- **Enhanced overlay** — playlists, albums, liked songs, and queue
- **Default overlay** — settings, HUD scale, and UI mode selection
- **Global hotkeys** — configurable transport and overlay keys
- **Diagnostics** — bridge, extension, and heartbeat status in-game
- **Privacy-first** — the mod only talks to `127.0.0.1`; credentials stay in Spotify

## Requirements

### In Minecraft

| Item | Details |
|------|---------|
| **Minecraft Java** | One JAR per patch (1.21 through 1.21.11) |
| **Fabric Loader** | ≥ 0.16.10 |
| **Fabric API** | Match your MC version (see version page) |

### On your PC

| Item | Details |
|------|---------|
| **Spotify desktop** | Must stay open while playing |
| **Spicetify** | Installed and applied |
| **`beatblocks-api.js`** | Spicetify extension from the [GitHub repo](https://github.com/vyas-devgna/beatblocks-control) |

## Quick setup

1. **Install Spicetify** and apply it to Spotify
2. Copy **`beatblocks-api.js`** into your Spicetify `Extensions` folder, then run:

```text
spicetify config extensions beatblocks-api.js
spicetify apply
```

Restart Spotify after apply.

3. Download the **JAR for your exact Minecraft version** and put it in `mods/` with **Fabric API**
4. Launch Minecraft, start playback in Spotify, press **Alt+I** (default) to open the overlay

Full guide: [GitHub README](https://github.com/vyas-devgna/beatblocks-control#try-it-in-5-minutes) · [Website](https://vyas-devgna.github.io/beatblocks-control/)

## Default controls

Change under **Options → Controls → BeatBlocks**

| Action | Default key |
|--------|-------------|
| Open overlay | **Alt+I** |
| Play / pause | **K** |
| Next track | **L** |
| Previous track | **J** |

You can also use **`/sp`** in chat to open the overlay.

## How it works

```text
Minecraft mod → localhost:50321 → beatblocks-api.js → Spicetify → Spotify
```

The mod runs a small HTTP server on your machine. The Spicetify extension sends playback updates and receives commands. Nothing is sent to external servers except normal Spotify traffic.

## Pick the right JAR

**Use one JAR per Minecraft patch** — do not use a 1.21.5 JAR on 1.21.11.

Examples:

- Minecraft **1.21** → `beatblocks-control-mc-1.21.jar` + Fabric API `0.102.0+1.21`
- Minecraft **1.21.5** → `beatblocks-control-mc-1.21.5.jar` + Fabric API `0.128.2+1.21.5`
- Minecraft **1.21.11** → `beatblocks-control-mc-1.21.11.jar` + Fabric API `0.141.4+1.21.11`

## Configuration

File: `.minecraft/config/beatblocks/beatblocks.json`

| Setting | Default | Description |
|---------|---------|-------------|
| `bridgePort` | `50321` | Local HTTP port |
| `apiPollSeconds` | `4` | Playback poll interval |
| `hudScaleMultiplier` | `1.0` | HUD size |
| `coverPixels` | `256` | Max cover art size |
| `selectedMode` | `DEFAULT` | `DEFAULT` or `ENHANCED` |

## Roadmap

| Status | Plan |
|--------|------|
| **Now** | Spicetify bridge — Spotify must be open with Spicetify + extension installed |
| **Future** | Spotify Web API support for simpler setup (planned) |

## Links

- **Website:** https://vyas-devgna.github.io/beatblocks-control/
- **Source & issues:** https://github.com/vyas-devgna/beatblocks-control
- **Releases:** https://github.com/vyas-devgna/beatblocks-control/releases

## License

MIT — free to use and modify. See [LICENSE](https://github.com/vyas-devgna/beatblocks-control/blob/main/LICENSE) on GitHub.
"""


def auth_headers(token: str) -> dict[str, str]:
    return {"Authorization": f"Bearer {token}", "User-Agent": "beatblocks-cleanup/1.0"}


def changelog_for_mc(mc: str) -> str:
    fabric = ""
    if TARGETS_PATH.is_file():
        for t in json.loads(TARGETS_PATH.read_text(encoding="utf-8")):
            if t.get("Minecraft") == mc:
                fabric = t.get("Fabric", "")
                break
    fabric_line = f"- **Fabric API** `{fabric}`\n" if fabric else ""
    return f"""\
## BeatBlocks Control {MOD_VERSION} — Minecraft {mc} (Beta)

Fabric client mod to control Spotify from in-game (overlay, HUD, hotkeys) via a local Spicetify bridge.

### Requirements
- **Minecraft {mc}** (use the JAR built for this exact patch version)
- **Fabric Loader** ≥ **{LOADER_MIN}**
{fabric_line}- **Spotify desktop** + **Spicetify** + `beatblocks-api.js` ([setup](https://github.com/vyas-devgna/beatblocks-control#try-it-in-5-minutes))

### Highlights
- In-game music overlay and now-playing HUD
- Playlists, albums, liked songs, queue
- Global hotkeys (default Alt+I, J/K/L)
- Localhost bridge only — no API keys in the mod

**Website:** https://vyas-devgna.github.io/beatblocks-control/  
**Source:** https://github.com/vyas-devgna/beatblocks-control
"""


def main() -> None:
    token = os.environ.get("MODRINTH_TOKEN", "").strip()
    if not token:
        sys.exit("Set MODRINTH_TOKEN")

    h = auth_headers(token)

    # --- Project metadata ---
    patch = {
        "title": "BeatBlocks Control",
        "description": (
            "Control Spotify from Minecraft — overlay, HUD, and hotkeys "
            "via a local Spicetify bridge. No API keys in the mod."
        ),
        "body": PROJECT_BODY,
        "categories": ["fabric", "utility", "game-mechanics"],
        "additional_categories": ["technology"],
        "client_side": "required",
        "server_side": "unsupported",
        "issues_url": "https://github.com/vyas-devgna/beatblocks-control/issues",
        "source_url": "https://github.com/vyas-devgna/beatblocks-control",
        "wiki_url": "https://vyas-devgna.github.io/beatblocks-control/",
        "requested_status": "approved",
        "donation_urls": [
            {
                "id": "github",
                "platform": "GitHub",
                "url": "https://github.com/sponsors/vyas-devgna",
            },
            {
                "id": "other",
                "platform": "UPI Tip",
                "url": "https://vyas-devgna.github.io/Portfolio/tip.html",
            },
        ],
    }
    r = requests.patch(f"{API}/project/{PROJECT_SLUG}", headers=h, json=patch, timeout=60)
    if r.status_code != 204:
        sys.exit(f"Project patch failed ({r.status_code}): {r.text}")
    print("Updated project: title, description, body, categories, links, requested_status=approved")

    # --- Version fixes (early uploads) ---
    versions = requests.get(
        f"{API}/project/{PROJECT_SLUG}/version", headers=h, timeout=30
    ).json()

    fixes = {
        "1.21": {"name": f"BeatBlocks {MOD_VERSION} (1.21)"},
        "1.21.1": {
            "name": f"BeatBlocks {MOD_VERSION} (1.21.1)",
            "changelog": changelog_for_mc("1.21.1"),
            "dependencies": [
                {"project_id": FABRIC_API_PROJECT, "dependency_type": "required"},
            ],
        },
    }

    for ver in versions:
        gvs = ver.get("game_versions") or []
        if len(gvs) != 1:
            continue
        mc = gvs[0]
        if mc not in fixes:
            continue
        body = fixes[mc]
        pr = requests.patch(
            f"{API}/version/{ver['id']}", headers=h, json=body, timeout=30
        )
        if pr.status_code != 204:
            print(f"  WARN {mc} patch ({pr.status_code}): {pr.text}")
        else:
            print(f"  Fixed version {mc} ({ver['id']})")

    # --- Optional gallery: repo banner ---
    root = Path(__file__).resolve().parent.parent
    banner = root / "docs" / "banner.jpg"
    if banner.is_file():
        params = {
            "ext": "jpg",
            "featured": "false",
            "title": "BeatBlocks Control",
            "description": "Control Spotify from inside Minecraft",
            "ordering": "3",
        }
        with banner.open("rb") as img:
            gr = requests.post(
                f"{API}/project/{PROJECT_SLUG}/gallery",
                headers={**h, "Content-Type": "image/jpeg"},
                params=params,
                data=img.read(),
                timeout=120,
            )
        if gr.status_code == 204:
            print("Uploaded gallery banner (docs/banner.jpg)")
        elif "duplicate" in (gr.text or "").lower():
            print("Gallery banner already present (skipped)")
        else:
            print(f"  Gallery banner ({gr.status_code}): {gr.text[:120]}")

    print(f"\nDone: https://modrinth.com/mod/{PROJECT_SLUG}")


if __name__ == "__main__":
    main()