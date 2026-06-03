# Contributing to BeatBlocks Control

Thanks for taking an interest in this project. Contributions of all sizes are welcome — bug reports, docs, UI polish, bridge improvements, and new Minecraft versions.

## Ways to help

| Type | What to do |
|------|------------|
| **Bug report** | Open a [GitHub issue](https://github.com/vyas-devgna/beatblocks-control/issues) with Minecraft version, mod version, OS, and steps to reproduce. |
| **Feature idea** | Open an issue first so we can agree on scope before you spend time on a large PR. |
| **Code** | Fork, branch from `main`, keep changes focused, and open a pull request. |
| **Docs** | README, `TESTING.md`, or in-game setup text improvements are always useful. |
| **Translations** | Not set up yet — if you want to add language files, open an issue to discuss format. |

## Development setup

1. **Java 21** — set `JAVA_HOME` or use a launcher runtime.
2. Clone the repo and run:
   ```powershell
   .\gradlew.bat clean build
   ```
3. Output JAR: `build/libs/` (per-version builds: `.\scripts\build-minecraft-versions.ps1` → `releases/`).
4. **Spicetify bridge** — copy `beatblocks-api.js` into Spicetify Extensions and run `.\scripts\setup-spicetify-bridge.ps1` if you are testing playback integration.
5. Run tests: `.\gradlew.bat test`

See [TESTING.md](TESTING.md) for manual QA checklists.

## Pull request guidelines

- One logical change per PR when possible (e.g. do not mix a version bump with unrelated refactors).
- Match existing code style and naming in `com.devgnav.beatblocks`.
- If you change bridge behavior, update `beatblocks-api.js` and document it in the PR description.
- Note which Minecraft versions you tested in-game.
- Do not commit build artifacts, `releases/*.jar`, session exports, or local config.

## Project areas

| Area | Path |
|------|------|
| Mod entry & services | `src/main/java/com/devgnav/beatblocks/` |
| Bridge HTTP server | `spotify/BeatBlocksApiClient.java` |
| UI (HUD & overlays) | `ui/` |
| Spicetify extension | `beatblocks-api.js` |
| Setup scripts | `scripts/` |

## Code of conduct

Be respectful and constructive. Maintainers may close issues or PRs that are off-topic, abusive, or out of scope.

## Questions

Open a GitHub issue or reach out via the links on the [project homepage](https://github.com/vyas-devgna/beatblocks-control).