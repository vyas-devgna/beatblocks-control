## BeatBlocks Control 1.2.7 — cover images fix

### Fixes
- **Cover textures**: upload decoded PNG bytes via `NativeImage.read()` instead of per-pixel reflection (broken on 1.21.5+).
- **Texture registration**: register covers on the render thread with bilinear filtering.
- **Now playing**: bridge fills `image_url` from Spicetify player metadata when the API omits it.
- **API client**: falls back to `images[]` / `album.images[]` when `image_url` is missing.

### Install
1. Replace the mod JAR for your Minecraft version from this release.
2. Re-run `scripts/setup-spicetify-bridge.ps1` so `beatblocks-api.js` is updated in Spicetify.
3. Fully quit and restart Minecraft and Spotify.