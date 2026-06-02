## BeatBlocks Control 1.3.0 — PNG icons, compact volume, layout fix

### Icons
- PNG icons from mod assets via `Icons` enum + `GuiDrawCompat` (no texture upload, no vector fallback).
- Uniform **14×14** draw size from **64×64** source art.

### Player bar
- **`PlayerBarLayout`**: reserves track / controls / volume zones so text and buttons do not overlap.
- Track titles clipped with scissor + pixel-width truncation.
- **Smaller volume slider** (max 48×4 px); % label hidden on narrow panels.
- Controls hide progressively (shuffle/repeat → heart/queue) when space is tight.

### Install
Replace JAR and fully restart Minecraft.