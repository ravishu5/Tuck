# Task for Gemini — Tuck theme system rebuild

Self-contained instruction. Read it fully before writing code. Post a plan and wait for approval.

---

## What you are building

Replace Tuck's single-accent themes with **multi-hue theme families**. Today a theme is one accent
color plus neutrals. After this task, a theme is:

| Layer | Purpose | Count |
|---|---|---|
| **Neutrals** | canvas, surface, card, elevated, border, hairline, text ramp | ~8 slots |
| **Palette** | coordinated hues used for collection tiles, badges, category chips | **8 hues** |
| **Primary** | one hue lifted from the palette for FAB, selection, links | 1 |
| **Roles** | destructive, warning, success | 3 |

The goal is a grid of colored collection tiles that reads as **one designed family**, not a bag of
colors. Everything below exists to make that true and keep it accessible.

---

## Current state of the code

- `app/src/main/java/com/tuck/app/ui/theme/TuckDesignTokens.kt` — defines `TuckColors` (15 flat
  slots), `TuckSpacing`, `TuckShapes`. **Extend this. Do not create a second token system.**
- `app/src/main/java/com/tuck/app/ui/theme/Color.kt` — 10 palettes today: 5 flavors (Linen, Noir,
  Forest, Cobalt, Plum) × light/dark.
- `app/src/main/java/com/tuck/app/ui/theme/Theme.kt` — theme selection, currently also supports
  Material You dynamic color.
- `collections.color` is **`TEXT` nullable and read by nothing.** It has existed since schema v3.
  You will finally use it — and because it is already `TEXT`, **no database migration is needed.**

**Scope decision to confirm before starting:** implement **two** themes properly — one light
(warm paper) and one dark. Five multi-hue families in light and dark is ~90 colors to balance and
contrast-check. Ask before attempting more than two.

---

## Step 1 — OKLCH color utility

Create `ui/theme/color/Oklch.kt`. No new dependency; this is ~60 lines of math.

**Why OKLCH and not HSL:** you need the hues to look equally bright. In sRGB/HSL, a "same lightness"
yellow and blue differ enormously in perceived lightness — the tile grid comes out lumpy, some tiles
glaring and others muddy. OKLCH is perceptually uniform, so holding **L** and **C** fixed and varying
only **H** produces a family that looks deliberate.

Implement:

```
data class Oklch(val l: Double, val c: Double, val h: Double)   // l 0..1, c 0..~0.4, h degrees
fun Oklch.toSrgb(): Color            // returns null/flag if out of gamut
fun Color.toOklch(): Oklch
fun Oklch.clampChromaToGamut(): Oklch
```

Conversion chain: `OKLCH → OKLab (a = C·cos H, b = C·sin H) → LMS' → cube → LMS → linear sRGB →
gamma encode`. Use Björn Ottosson's published Oklab matrices.

> **Verify the constants against the published reference before relying on them.** Transcribed
> matrix coefficients are a classic source of silent, hard-to-spot error — a single wrong digit
> shifts every hue slightly and nothing crashes.

Sanity vectors for your tests (approximate — confirm against the reference):

| sRGB | OKLCH |
|---|---|
| `#FFFFFF` | L 1.0, C 0 |
| `#000000` | L 0.0, C 0 |
| `#FF0000` | L ≈ 0.628, C ≈ 0.258, H ≈ 29° |
| `#0000FF` | L ≈ 0.452, C ≈ 0.313, H ≈ 264° |

Also assert a round trip: `Color → Oklch → Color` within 1/255 per channel for a spread of colors.

---

## Step 2 — Define the palettes

Create `ui/theme/color/TuckPalette.kt`.

```
enum class PaletteSlot { TERRACOTTA, AMBER, MUSTARD, SAGE, TEAL, DENIM, PLUM, ROSE }

data class PaletteEntry(val slot: PaletteSlot, val fill: Color, val onFill: Color)
data class TuckPalette(val entries: List<PaletteEntry>)
```

**Hue positions** — space them *perceptually*, not by dividing 360 evenly. The yellow-green region
crowds badly, so mathematically even spacing yields three hues that all read as "sort of yellow".
Start from these angles and adjust by eye afterwards:

```
TERRACOTTA  25°     TEAL    195°
AMBER       65°     DENIM   245°
MUSTARD    105°     PLUM    290°
SAGE       145°     ROSE    335°
```

**Lightness and chroma** — start here and tune:

- Light theme tiles: `L ≈ 0.70`, `C ≈ 0.15`
- Dark theme tiles: `L ≈ 0.65`, `C ≈ 0.13` — slightly dimmer so saturated tiles do not glare on a
  dark canvas

**The gotcha that will bite you:** maximum in-gamut chroma varies a lot by hue at a fixed lightness.
At `L = 0.70`, a yellow can hold far more chroma than a blue. If you set `C = 0.15` for every hue,
some will fall outside sRGB and **clip silently**, which breaks exactly the evenness you used OKLCH
to get.

Handle it deliberately: for each hue, binary-search the maximum in-gamut chroma at the target L, then
use the **minimum across the family** as the shared C. An even family at slightly lower chroma beats
an uneven one at higher chroma.

**Foregrounds are per-slot, not global.** Mustard needs near-black text; denim needs white. Compute
each `onFill` by picking whichever of the theme's ink/paper colors scores higher contrast on that
fill, then assert the result (Step 4).

**Keep slot names and hue positions identical across themes.** A collection assigned `TERRACOTTA`
must stay recognisably terracotta in light, dark, and every flavor — only L and C change.

---

## Step 3 — Restructure the tokens

In `TuckDesignTokens.kt`, regroup `TuckColors` into **neutrals**, **roles**, and a `palette`
reference. Add the slots this design needs and the current set lacks: `canvas` (warm paper base),
`highlight` (search-match marker), `badgeBackground`, `dividerHairline`.

**Keep every existing slot name resolving**, so the ~15 screens migrate one at a time rather than in
one enormous commit. This task should not touch screen files at all beyond compiling.

---

## Step 4 — Validation tests *(the part that makes this hold up)*

Create `app/src/test/java/com/tuck/app/PaletteValidationTest.kt`. These run against **every shipped
theme in light and dark**. Do not verify by eye — a palette that looks fine on your monitor still
fails for a user at 200% brightness with a color vision deficiency.

1. **Foreground contrast** — every entry's `onFill` scores **≥ 4.5:1** on its `fill`.
   Use the WCAG relative-luminance formula on sRGB.
2. **Tile separation** — every `fill` scores **≥ 3:1** against `canvas`, so tiles never dissolve into
   the background.
3. **Pairwise distinctness** — every pair of fills is separated by a perceptual distance floor.
   Compute in OKLab (`ΔE = √(ΔL² + Δa² + Δb²)`); start the floor at `0.10` and tune so a genuinely
   distinguishable palette passes and a deliberately bad one fails. **Write the failing case too** —
   a test that only ever passes proves nothing.
4. **Colorblind safety** — run every fill through **deuteranopia, protanopia and tritanopia**
   simulation (Viénot 1999 or Machado 2009 matrices) and assert the pairwise floor from (3) still
   holds under each. This is where naive palettes collapse: eight hues routinely reduce to three
   distinguishable ones for a deuteranope, which is ~8% of men.
5. **Gamut** — every entry round-trips `OKLCH → sRGB → OKLCH` within tolerance, proving nothing
   clipped.

If a palette cannot pass all five, **change the palette, not the thresholds.**

---

## Step 5 — Wire `collections.color`

- Store the **`PaletteSlot` name** (e.g. `"TERRACOTTA"`), never a hex value. A stored hex freezes at
  whatever theme was active when the collection was created, so changing theme — or just light to
  dark — would leave stale, clashing tiles behind. A slot re-maps correctly every time.
- The column is already `TEXT` nullable: **no migration required.**
- Collections with `color = null` get a **deterministic** slot from a stable hash of the collection
  id, so an upgraded library looks intentional rather than uniformly grey, and the same collection
  keeps the same color across launches. Bias the assignment so adjacent tiles in the default sort
  rarely land on neighbouring hues.
- Add a slot picker to collection create/edit — the eight palette swatches, not a free color wheel.
  Free picking guarantees clashes and unreadable tiles.

---

## Step 6 — Palette gallery screen

A debug-only screen showing, for each theme in light and dark:

- every slot as a tile with its `onFill` text and slot name
- the neutral ramp
- the role colors
- a **simulated-colorblind row** for all three deficiency types

This is how the palette gets reviewed, and how regressions get caught later.

---

## Deliverables

- `ui/theme/color/Oklch.kt`, `ui/theme/color/TuckPalette.kt`
- `TuckDesignTokens.kt` restructured, existing slot names still resolving
- `PaletteValidationTest.kt` — the five tests, plus the deliberately-failing palette case
- `OklchTest.kt` — conversion vectors and round trip
- Collection slot picker + deterministic default assignment
- Debug palette gallery screen
- `DECISIONS.md` entry recording: how many themes ship, why OKLCH, the chosen chroma and why it was
  clamped, and what happened to dynamic color
- `PROGRESS.md` updated

## Verification

```bash
./gradlew assembleDebug testDebugUnitTest connectedDebugAndroidTest
```

Then install and attach to your walkthrough:

- palette gallery screenshots, **every theme, light and dark**
- the Collections screen before and after, showing colored tiles on a real library
- one screenshot at 200% font scale proving nothing clips

A device run is not optional here. This repo has already shipped a full-text index that compiled,
passed review, and then failed on device with `no such module: fts5`.

---

## Do not

- Do not touch screen files beyond what is needed to compile. Screens are a separate task.
- Do not add a color or design-system dependency. This is arithmetic.
- Do not store hex values in `collections.color`.
- Do not loosen a validation threshold to make a palette pass.
- Do not implement more than two themes without asking.
- Do not delete or repurpose the existing flavor enum values — some are referenced in settings; if
  a flavor is dropped, migrate stored preferences to a shipped theme rather than crashing on an
  unknown value.
- Do not mark this done while any component still reads a raw color instead of a slot.
