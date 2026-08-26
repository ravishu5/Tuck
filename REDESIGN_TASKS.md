# Tuck — Visual Redesign Brief

Reference material: App Store screenshots for **Stash Anything**. The goal is a Tuck that feels as
considered as that, and better where Tuck actually does more.

Work these **one at a time, in order**. R0 is blocking: it is a set of decisions, not code.

---

## ⚠️ Read this before you look at the reference images

**The reference screenshots are App Store marketing artwork, not app UI.**

Roughly 60% of each image is marketing chrome that must **never** appear in the app:

| In the reference | What it is |
|---|---|
| Giant headlines — "Stop saving to your camera roll", "Tap Share on anything." | Marketing copy |
| Coral / yellow / green / pink full-bleed backdrops | Marketing backdrop |
| Serif italic body text ("Recipes, outfits, ideas — buried under 4,820 screenshots") | Marketing copy |
| Sunburst and starburst graphics, highlighter swashes, hand-drawn arrows | Marketing decoration |
| The phone bezel/frame itself | Device mockup |
| The floating app-icon collage (Instagram, YouTube, Reddit logos orbiting a phone) | Marketing illustration |

**Only the content inside the phone frames is app UI.** If you build a screen with a giant serif
headline on a coral background, you have copied the poster instead of the product. Do not do that.

### What the actual app UI looks like in those frames

Five real screens are visible. This is the design language to extract:

1. **Save sheet** — `Cancel · Save to Stash · Save` header; a preview card with thumbnail, title,
   domain; a `PICK A CATEGORY` label; a 3-column grid of saturated color tiles, each with a line
   icon, name and a check when selected.
2. **Home ("My Stash")** — big title, `375 saves · 12 folders` subtitle, search and `+` icon buttons,
   a row of filter pills (All / Recent / Shared / Locked), then a 3-column grid of color tiles each
   with icon, folder name and save count.
3. **Search** — a plain search field showing the query and `8 results`; result rows with a thumbnail,
   a title with the **matched term highlighted in yellow**, and a metadata line (`Reel · Recipes · 2d
   ago`). One row carries an `OCR` badge reading *found "ramen" in image*.
4. **Note sheet** — the preview card, a horizontal row of folder chips with `+ New`, then
   `ADD A NOTE` above a free-text field.
5. **Shared folder** — folder title, overlapping member avatars, `3 people · 27 saves`, then rows
   attributed per person with a type badge (`Reel`, `Link`, `IG`, `Photo`, `PDF`).

### The design language, stated plainly

- **Color is the organising principle.** Every folder owns a hue from a coordinated family, and the
  grid of colored tiles *is* the navigation. Color carries identity, not decoration — which only
  works if the hues visibly belong together.
- **Warm paper background**, not white and not grey. Cream/bone.
- **Large corner radii** on tiles and sheets (roughly 20–24dp), consistently.
- **Chunky, confident type.** Big bold titles; small all-caps section labels with wide letter-spacing
  (`PICK A CATEGORY`, `ADD A NOTE`, `FOLDER`).
- **Counts everywhere.** `375 saves · 12 folders`, `Recipes 47`. Numbers make a library feel owned.
- **Flat, not glassy.** Solid fills, minimal shadow, thin hairline borders where separation is needed.
- **Content is the hero.** Thumbnails dominate; chrome recedes.

---

## What "better than this" means for Tuck

Do not stop at a clone. Stash has no answer for the things Tuck already does, and those need design
language of their own — this is where the redesign earns its keep:

- **Ranked search with provenance.** Tuck knows *why* something matched (title vs OCR vs a comment)
  and ranks by weighted relevance. Stash shows one `OCR` badge; Tuck should show match reason on
  every result, honestly and compactly.
- **Nested comment trees.** Tuck archives entire Reddit threads with depth. Nobody in this category
  has a good mobile design for a 300-comment tree that is pleasant to read offline.
- **Source vs derived.** Tuck must visibly distinguish what was captured from what was computed.
  That trust boundary deserves a visual convention.
- **Entities and topics** (see `TASKS.md` Phase A) will need chips, entity pages and relationship
  affordances that this reference has no equivalent for.

---

## Non-negotiables

The redesign must not break these. A beautiful app that regresses any of them is a failed task.

1. **Share-to-saved stays under 400ms perceived.** The save sheet is on the hot path. No layout that
   requires loading collections, thumbnails or counts before the sheet can render.
2. **No feature regressions.** Every existing action must survive: pin, favorite, archive, trash,
   restore, bulk actions in Inbox, collection management, biometric-locked collections, filters, sort.
3. **Accessibility.** 48dp minimum touch targets, `contentDescription` on every meaningful element,
   TalkBack order that makes sense, dynamic type to 200% without clipping, contrast ≥ 4.5:1 for text.
   **Color must never be the only carrier of meaning** — a colored tile also needs its name and icon.
4. **Light and dark both complete.** No screen that only works in one.
5. **Reuse `TuckDesignTokens`.** Extend the existing token system; do not start a second one.
6. **No new dependency without approval,** and note the APK delta. The release APK is already 43.6 MB
   against a 25 MB budget.

---

# R0 — Decisions before any code *(blocking — answer these first)*

Produce a short document answering each, with a recommendation. Do not start R1 until Ravi has
signed off. Record the outcomes in `DECISIONS.md`.

### R0.1 — How many themes, and what *is* a theme?

**A theme is not an accent color. A theme is a coordinated family of colors.**

The reference app is not "coral". It is a warm cream canvas plus a *set* of saturated hues — coral,
mustard, blue, purple, green, pink, near-black — that clearly belong together. That set is what makes
the grid of tiles read as one designed thing instead of a bag of random colors.

So every Tuck theme must define:

| Layer | What it is | Roughly how many |
|---|---|---|
| **Neutrals** | canvas, surface, card, border, and the text ramp | ~8 slots |
| **Palette** | the coordinated hues used for collection tiles, badges and category chips | **8–10 hues** |
| **Primary** | one hue lifted from the palette for interactive chrome — FAB, selection, links | 1 |
| **Roles** | destructive, warning, success — must stay distinguishable from the palette | 3 |

The decision here is **how many themes**, not whether they are multi-hue. They all are.

Each flavor keeps its own character through its neutrals and its hue family, for example:

- **Linen** — warm cream canvas; terracotta, mustard, sage, denim, plum, clay, rust, ink
- **Noir** — near-black canvas; the same hue positions pushed brighter and cleaner so they hold up
  against dark
- **Forest** — sage canvas; moss, olive, ochre, rust, deep teal, bark

*Recommendation:* design and QA **two** properly — Linen (light) and Noir (dark) — as the signature
pair, and either cut the other three or ship them clearly marked as experimental. Five hand-tuned
multi-hue families, each in light and dark, is ten palettes of roughly nine colors: **around ninety
colors to balance and contrast-check.** That is not a side quest, and doing it badly is worse than
not doing it.

### R0.2 — Dynamic color (Material You)?
Currently supported. It overrides brand identity with the user's wallpaper.
*Recommendation:* drop it, or make it opt-in and off by default. State the choice explicitly.

### R0.3 — Typeface
Everything is `FontFamily.SansSerif` today — the system font. The reference's character comes largely
from its type.
Options: (a) system font, 0 KB, no identity; (b) bundle a variable font, ~150–300 KB, full control,
works offline; (c) downloadable Google Fonts, 0 KB but requires Play Services and can flash fallback.
*Recommendation:* (b), one variable font with real weight range. Name the font in your proposal and
check the licence permits bundling.

### R0.4 — Per-collection color
`collections.color` **already exists in the schema and is read by nothing.** The whole colored-tile
design depends on it.

Store the collection's **palette slot** (a stable index or name such as `terracotta`), *not* a raw
hex value. A stored hex would be frozen at whatever the theme was when the collection was created,
so switching theme — or light to dark — would leave stale, clashing colors behind. Storing the slot
means every collection re-maps correctly into whatever palette is active.

Free color-picking is out: it guarantees clashes and unreadable tiles. The user picks from the
active palette.

### R0.5 — Icon set
Reference uses custom line glyphs per category. Today: Material Icons Extended.
*Recommendation:* stay on Material Symbols for coverage and zero cost; curate a fixed mapping of
category → icon so it looks deliberate rather than defaulted.

### R0.6 — Scope boundary
Confirm: is this a **visual** redesign on the existing information architecture (4 tabs: Home, Inbox,
Collections, Search), or is the IA changing too? The reference has no Inbox concept. Tuck's Inbox is
a deliberate product decision ("save first, organise later") and I would keep it.

---

# R1a — Foundation: theme structure and neutrals

**Files:** `ui/theme/TuckDesignTokens.kt`, `ui/theme/Color.kt`, `ui/theme/Theme.kt`

**Scope**
- Restructure `TuckColors` so a theme carries three groups rather than one flat list: **neutrals**
  (canvas, surface, card, elevated, border, hairline, and the text ramp), **roles** (destructive,
  warning, success), and a reference to its **palette** (R1b).
- Add the slots this design needs and the current set lacks: `canvas` (the warm paper base),
  `highlight` (search match marker), `badgeBackground`, `dividerHairline`.
- Keep the existing slot names resolving so screens migrate one at a time instead of in one commit.

**Acceptance criteria**
- [ ] Neutrals pass 4.5:1 for body text and 3:1 for large text against canvas and card, in both themes
- [ ] Role colors are distinguishable from every palette hue (see R1b's separation test)
- [ ] Existing screens still compile and render unchanged before any screen work begins

---

# R1b — The collection palette *(the heart of the redesign)*

**Goal:** 8–10 hues per theme that unmistakably belong to one family, stay legible, and stay
distinguishable from each other — including for colorblind users.

**Build them in a perceptual color space, not by eye.** This is the part that separates a designed
palette from a bag of colors: pick the hues in **OKLCH**, holding lightness (L) and chroma (C)
roughly constant across the family and varying only hue (H). In sRGB or HSL, a "same brightness"
yellow and blue are nowhere near the same perceived lightness, and the tile grid ends up looking
lumpy — some tiles glaring, others muddy. Fixing L and C fixes that.

**Scope**
- Define each palette as data: a slot name, an OKLCH triple, and its paired foreground.
- Convert to sRGB at build or test time, and check the color is actually in gamut — out-of-gamut
  OKLCH values clip silently and break the family's evenness.
- Light theme and dark theme keep the **same hue positions and slot names**, at different L/C, so a
  collection called `terracotta` stays recognisably itself across themes rather than jumping hue.
- Space hues **perceptually, not by dividing 360 evenly** — the yellow-green region crowds badly, so
  even mathematical spacing produces several hues that read as "sort of yellow".
- Deterministic slot assignment for collections with no color set, so an upgraded library looks
  intentional rather than uniformly grey — and so two adjacent tiles rarely land on neighbouring hues.

**Validation — write these as tests, not as a visual check.** This is the whole reason the palette
will hold up:
- **Foreground contrast:** every hue's paired foreground ≥ 4.5:1 on that hue.
- **Tile separation:** every hue ≥ 3:1 against the canvas, so tiles never dissolve into the background.
- **Pairwise distinctness:** every pair of hues separated by a perceptual distance floor, so no two
  tiles read as the same color.
- **Colorblind safety:** run the palette through deuteranopia, protanopia and tritanopia simulation
  and assert the pairwise floor still holds. Around 8% of men have some form of color vision
  deficiency, and a palette of 9 hues is exactly where naive palettes collapse into three.
- **Gamut:** every color round-trips OKLCH → sRGB → OKLCH within tolerance.

**Acceptance criteria**
- [ ] All five validation tests pass for every shipped theme, in light and dark
- [ ] A palette gallery screen renders every hue as a tile with its foreground, in both themes, plus
      a simulated-colorblind row — attached as screenshots to the walkthrough
- [ ] Collections store a **slot**, not a hex value, and re-map correctly when the theme changes
- [ ] An existing library upgraded from the current build shows colored collections with no user action
- [ ] Switching theme changes every collection tile coherently, with no stale or clashing color

**Out of scope:** letting users define custom hues. If that is ever wanted, it has to run the same
validation, which is a separate piece of work.

# R2 — Typography

**Files:** `ui/theme/Type.kt`, plus the font resource if R0.3 chose bundling

**Scope**
- Implement the scale decided in R0.3: display (screen titles like "My Stash"), title, body, label,
  and a distinct **section label** style — small, uppercase, wide letter-spacing — for `PICK A
  CATEGORY` / `ADD A NOTE` / `FOLDER`.
- Numeric style for counts, ideally tabular figures so counts do not jitter.
- Verify at 200% font scale on the densest screen (item detail) before calling it done.

**Acceptance criteria**
- [ ] No clipped or overlapping text at 200% scale on Home, Collections, Search, Detail, Save sheet
- [ ] Section-label style used consistently, not re-specified ad hoc per screen
- [ ] APK size delta reported

---

# R3 — Component library

**Files:** `ui/components/` (extend `TuckComponents.kt`; do not start a parallel set)

Build these as previewable, stateless composables **before** touching screens. Every one needs
`@Preview` in light and dark, with a long-text and empty variant.

- `CollectionTile` — palette-slot fill with its paired foreground, icon, name, count, selected
  state, locked badge. Takes a slot, never a raw color
- `SaveCard` — thumbnail or type glyph, title, source line, type badge
- `FilterPill` — selected/unselected
- `SectionLabel` — the all-caps label
- `TypeBadge` — Reel / Link / PDF / Photo / Note / Reddit, per source and per content type
- `MatchReasonChip` — "matched in OCR", "matched in a comment" (Tuck-specific; no reference equivalent)
- `CountStat` — `375 saves · 12 folders`
- `TuckBottomSheet` — the shared sheet container for save and note flows
- `EmptyState` — illustration slot, title, body, optional action
- `SkeletonCard` — loading placeholder, since enrichment is async and users see this often

**Acceptance criteria**
- [ ] Every component renders correctly with a 60-character title and with empty/null data
- [ ] No component reaches into a ViewModel or repository
- [ ] A gallery preview screen shows all components in both themes

---

# R4 — Collections and Home

**Files:** `ui/collections/CollectionsScreen.kt`, `ui/home/HomeScreen.kt`

**Scope**
- Replace the list with a **3-column `LazyVerticalGrid`** of `CollectionTile`s. There is currently no
  `LazyVerticalGrid` anywhere in the codebase — this also satisfies `TASKS.md` T8, so mark T8 as
  absorbed rather than doing it twice.
- Header: title, `N saves · M folders`, search and add actions.
- Filter pills: All / Recent / Favorites / Locked.
- Grid/list toggle, persisted per surface in DataStore.
- Home keeps its distinct job — recent saves rail, Rediscover — restyled to the new system rather
  than becoming a second Collections screen.
- Locked collections show a lock affordance and still require biometric auth before opening.

**Acceptance criteria**
- [ ] Grid scrolls with no dropped frames over a 10,000-item seeded database
- [ ] Toggle survives process death
- [ ] Collections with no items render a sensible empty tile, not a broken one
- [ ] Biometric gate still fires on locked collections

---

# R5 — Save sheet *(highest-risk screen)*

**Files:** `ui/share/ShareActivity.kt`, `ui/share/ShareViewModel.kt`

This is the hot path and the app's whole reason for existing. **Design around the latency budget, not
the other way round.**

**Scope**
- Restyle the instant-save HUD: preview card, `Saved ✓`, then optional category grid and note field.
- The item is already persisted before the sheet renders — keep that ordering absolutely intact. The
  sheet is a confirmation with optional follow-ups, never a form gating the save.
- Category tiles reuse `CollectionTile` at a compact size; recent-4 first, then a search field.
- `+ New` creates a collection inline without leaving the sheet.
- `ADD A NOTE` — the "why did I save this" capture.
- Auto-dismiss must never cancel the save.

**Acceptance criteria**
- [ ] Share intent → row committed still under 250ms p95; → sheet visible under 400ms p95, measured
      not estimated
- [ ] Dismissing the sheet mid-typing keeps the item and any text already entered
- [ ] Works with zero collections, and with 200 collections
- [ ] Instrumentation test for the latency budget per mime type

---

# R6 — Search

**Files:** `ui/search/SearchScreen.kt`

**Scope**
- Result rows: thumbnail, title with the **matched term highlighted**, metadata line, type badge.
- `N results` count.
- Surface the existing FTS snippet with highlight markers — the backend already returns
  `<b>`-delimited snippets; render them rather than stripping them.
- **`MatchReasonChip` on every result** — title / OCR / comment / note. Tuck knows this and the
  reference app barely does; showing it is the differentiator.
- Keep the query-DSL chips already built (`source:`, `tag:`, `after:` …) and restyle them.
- Zero-result state that teaches the DSL by example.

**Acceptance criteria**
- [ ] Highlight renders from the real FTS snippet, not a client-side string match
- [ ] Match reason is accurate — an OCR-only hit never claims a title match
- [ ] Results still appear within 120ms of a keystroke at 10k items

---

# R7 — Item detail

**Files:** `ui/detail/ItemDetailScreen.kt` (currently ~1,000 lines — split it up as part of this)

**Scope**
- Restyle the section stack: hero media, title, source line, why-I-saved, original content, comments,
  notes, collections, details.
- **Establish the source/derived visual convention** — computed content (OCR text, auto tags,
  categories) is visually marked and collapsible; captured content never is. This is a trust
  boundary, and it should be legible at a glance.
- **Comment tree**: depth guides, collapse/expand per node, sticky parent context on scroll, readable
  at depth 5+. Design for a 300-comment thread on a phone, offline. Nobody in this category does this
  well; it is worth the effort.
- Entity chips, ready for the entity pages in `TASKS.md` T4.

**Acceptance criteria**
- [ ] A 300-comment thread scrolls smoothly and stays comprehensible at depth
- [ ] Derived content is distinguishable from source content without reading the labels
- [ ] File split into focused composables, none over ~250 lines

---

# R8 — Inbox

**Files:** `ui/inbox/InboxScreen.kt`

**Scope:** restyle triage rows and swipe actions to the new system; day grouping headers using
`SectionLabel`; keep bulk selection working; make "file this into a collection" a one-tap colored
tile picker rather than a dialog.

**Acceptance criteria**
- [ ] Swipe actions unchanged in behaviour, restyled in appearance
- [ ] Bulk selection survives rotation
- [ ] Filing an item takes one tap plus one tile tap

---

# R9 — Motion

**Scope:** shared-element transition from card to detail; sheet spring in/out; tile press feedback;
skeletons instead of spinners while enrichment runs; a restrained "Saved ✓" confirmation. Respect
`Settings.Global.ANIMATOR_DURATION_SCALE` and reduce-motion accessibility settings.

**Acceptance criteria**
- [ ] No animation longer than 300ms on a primary path
- [ ] Everything still usable with animations disabled
- [ ] No jank introduced on the share path

---

# R10 — States, polish and audit

**Scope:** empty, loading, error and offline states for every screen; app icon and widget restyle to
match; `PROGRESS.md` and `DECISIONS.md` updated; before/after screenshots of every screen in light
and dark.

**Acceptance criteria**
- [ ] Every screen has a designed empty state — none show a blank canvas
- [ ] Full accessibility pass: TalkBack walkthrough of each screen, 200% type, contrast verified
- [ ] Every screen checked in every shipped theme, light and dark — palette regressions surface on
      real screens, not in the gallery
- [ ] Widget and app icon consistent with the new system
- [ ] Before/after screenshot set attached

---

# Sequencing note

Do **not** run this alongside `TASKS.md` Phase A. Both touch the same UI files and will conflict.
Finish the redesign through R4 at minimum, then resume Phase A so entity pages (T4) are designed in
the new system from the start rather than retrofitted.

`TASKS.md` T8 (grid view) is absorbed by R4 — mark it as such rather than building it twice.
