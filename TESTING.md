# Testing Guide

## Automated Tests

Run via Gradle:

```bash
./gradlew test
```

### Test Matrix

| Test Class | Validates | MC Required |
|---|---|---|
| `HudLayoutTest` | HUD scaling formula across 5 resolutions × 4 GUI scales | No |
| `BridgeDiagnosticsTest` | Health checks, status summaries, stale heartbeat | No |
| `ConfigTest` | Load/save roundtrip, normalize clamping, defaults | No |

### Resolution Coverage (HudLayoutTest)

| Resolution | GUI Scales | Checked |
|---|---|---|
| 1920×1080 | 1.0, 2.0, 3.0, 4.0 | ✅ |
| 1366×768 | 1.0, 2.0 | ✅ |
| 1600×900 | 1.0, 2.0 | ✅ |
| 2560×1440 | 1.0, 2.0, 3.0, 4.0 | ✅ |
| 3840×2160 | 1.0, 2.0, 3.0, 4.0 | ✅ |

All tests verify:
- GUI width ≥ 80
- GUI height ≥ 20
- GUI padding 2–10
- Scale multiplier effect (0.7x, 1.0x, 1.5x)

---

## Bridge Connectivity Test

```powershell
.\scripts\test-spicetify-bridge.ps1
```

Checks:
1. Spicetify CLI installed
2. Extension file deployed
3. Desktop music player running
4. Port 50321 listening
5. HTTP endpoint responding

---

## QA Checklist GUI

```powershell
.\scripts\launch-checklist-gui.ps1
```

Standalone Swing app (no Minecraft needed) with:
- All spec checklist items
- Built-in bridge diagnostics
- Pass/Fail/Skip status per item
- Notes field per item
- Export to timestamped HTML or Markdown

---

## Manual In-Minecraft Checklist

### Keybinds

| # | Test | Expected |
|---|------|----------|
| 1 | Press Alt+I in-game | Overlay opens |
| 2 | Press Alt+S in-game | Nothing happens |
| 3 | Press K in-game | Play/pause toggles |
| 4 | Press L in-game | Next track |
| 5 | Press J in-game | Previous track |

### Default Mode

| # | Test | Expected |
|---|------|----------|
| 6 | Alt+I with selectedMode=DEFAULT | Settings/status overlay (not full UI) |
| 7 | Enhanced Mode greyed out when bridge not connected | Greyed out with reason |
| 8 | GitHub setup link visible | Clickable link |

### Enhanced Mode

| # | Test | Expected |
|---|------|----------|
| 9 | Switch to Enhanced Mode when bridge connected | Full BeatBlocks UI opens |
| 10 | Gear icon top-left | Visible, clickable → opens settings |
| 11 | Sidebar tabs work | All tabs navigate |
| 12 | Playlists load | List populated from library |
| 14 | Play/pause button | Toggles playback |
| 15 | Seek bar | Click to seek |
| 16 | Volume display | Shows percentage |
| 17 | Shuffle/Repeat | Toggles correctly |
| 18 | Cover art | Loads, pixelated correctly |

### HUD

| # | Test | Expected |
|---|------|----------|
| 19 | HUD visible at top-right | Song info displayed |
| 20 | HUD ≈283×70 at 1920×1080 | Measure with F3 |
| 21 | HUD has transport buttons | ⏮ ▶/⏸ ⏭ visible |
| 22 | HUD hides during F1 | Hidden |
| 23 | HUD hides when overlay open | Hidden |

### Overlay Appearance

| # | Test | Expected |
|---|------|----------|
| 24 | Overlay NOT fullscreen | Game visible behind |
| 25 | No blur effects | Crisp panels, no blur |
| 26 | Sharp borders, dark panels | Polished look |
| 27 | Readable text size | No tiny text |

### Input Isolation

| # | Test | Expected |
|---|------|----------|
| 28 | WASD while overlay open | No player movement |
| 29 | E while overlay open | No inventory |
| 30 | Q while overlay open | No item drop |
| 31 | Mouse while overlay open | Clicks stay in overlay |
| 32 | Close overlay → controls work | Full game control restored |

### Reconnection

| # | Test | Expected |
|---|------|----------|
| 33 | Close BeatBlocks → reopen | Reconnects |
| 34 | spicetify apply | Reconnects after restart |
| 35 | Close Minecraft | BeatBlocks keeps playing |

### Performance

| # | Test | Expected |
|---|------|----------|
| 36 | FPS with overlay closed | < 5 FPS drop |
| 37 | FPS with overlay open | < 10 FPS drop |
| 38 | Cover art cache | No repeat downloads |

---

## Test Reports

Generated reports saved to `test-reports/` by the checklist GUI:
- `beatblocks-qa-YYYYMMDD_HHmmss.html`
- `beatblocks-qa-YYYYMMDD_HHmmss.md`
