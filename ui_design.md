# Tuck — Master UI/UX Design System & Architectural Blueprint

> **"Save anything. Find everything. Remember what matters."**  
> *A calm, tactile, editorial, and private digital archive for Android.*

---

## Table of Contents

1. [Design Philosophy & Core Principles](#1-design-philosophy--core-principles)
2. [Iconography System & Visual Asset Guide](#2-iconography-system--visual-asset-guide)
3. [Color Architecture & Theme Flavors](#3-color-architecture--theme-flavors)
4. [Typography & Spatial Hierarchy](#4-typography--spatial-hierarchy)
5. [Component Design System (Atoms, Molecules, Organisms)](#5-component-design-system)
6. [Screen-by-Screen Deep Dive & ASCII Visual Blueprints](#6-screen-by-screen-deep-dive)
   - [6.1 Global Shell & Fluid Navigation](#61-global-shell--fluid-navigation)
   - [6.2 Home Screen — The Digital Memory Hub](#62-home-screen--the-digital-memory-hub)
   - [6.3 Inbox Screen — Zero-Friction Triage Stream](#63-inbox-screen--zero-friction-triage-stream)
   - [6.4 Collections & Smart Categories Hub](#64-collections--smart-categories-hub)
   - [6.5 Search & Retrieval Engine](#65-search--retrieval-engine)
   - [6.6 Item Detail Screen — The Flagship View](#66-item-detail-screen--the-flagship-view)
   - [6.7 Instant Share Sheet HUD & Capture Overlay](#67-instant-share-sheet-hud--capture-overlay)
   - [6.8 Global Quick Capture Speed Dial Modal](#68-global-quick-capture-speed-dial-modal)
   - [6.9 Favorites & Pinned Shelf](#69-favorites--pinned-shelf)
   - [6.10 Settings, Vault & Storage Center](#610-settings-vault--storage-center)
   - [6.11 Trash & Safe Recovery Vault](#611-trash--safe-recovery-vault)
7. [Micro-Interactions, Motion Physics & Haptics](#7-micro-interactions-motion-physics--haptics)
8. [Step-by-Step Implementation Roadmap](#8-step-by-step-implementation-roadmap)

---

## 1. Design Philosophy & Core Principles

```
  ┌────────────────────────────────────────────────────────────────────────┐
  │                           TUCK DESIGN PILLARS                          │
  ├───────────────────┬────────────────────┬───────────────────────────────┤
  │   CALM EDITORIAL  │ CONTENT-FIRST HERO │    ZERO-FRICTION CAPTURE      │
  │ Warm paper tones, │ Minimal chrome,    │ Save in <400ms, non-blocking, │
  │ refined type, no  │ high-aspect ratio  │ background OCR/enrichment,    │
  │ visual clutter.   │ media previews.    │ zero mandatory triage.        │
  └───────────────────┴────────────────────┴───────────────────────────────┘
```

1. **The Digital Drawer**: Tuck is not another rigid folder-based bookmark manager. It is a calm, personal repository that captures everything instantly and makes retrieval effortless through multi-faceted indexing (FTS4/5, OCR, extracted entities, smart topics).
2. **Tactile Craft & Warmth**: Rejecting sterile flat design, Tuck employs warm, tactile paper-inspired aesthetics (`Linen`), deep contrast OLED modes (`Noir`), organic palettes (`Forest`, `Cobalt`, `Plum`), subtle 1dp hairline borders, and layered tonal elevations.
3. **Content is the Hero**: Card layouts prioritize rich media thumbnails, platform origin cues, and legible typography over app chrome.
4. **Transparent Trust Boundary**: Captured source data (original posts, raw comments, OCR text, images) is sacred and immutable. AI-derived metadata is visually distinguished with `✦` markers, is always collapsible, and can be regenerated or deleted without affecting source items.

---

## 2. Iconography System & Visual Asset Guide

Tuck uses a dual-state (Outlined for inactive/rest, Filled for active/accent) iconography language based on Google Material Symbols with customized optical weighting (200 weight for subtle chrome, 400 for content, 600 for active states).

### 2.1 Navigation & Core Actions

| Icon | Name / Identifier | Outlined State | Filled / Active State | Semantic Purpose |
|:---|:---|:---:|:---:|:---|
| 🏠 | `NavHome` | `Icons.Outlined.Home` | `Icons.Filled.Home` | Home dashboard & recent memory |
| 📥 | `NavInbox` | `Icons.Outlined.Inbox` | `Icons.Filled.Inbox` | Unfiled triage stream with badge count |
| 📁 | `NavCollections` | `Icons.Outlined.Folder` | `Icons.Filled.Folder` | User collections & smart boards |
| 🔍 | `NavSearch` | `Icons.Outlined.Search` | `Icons.Filled.Search` | Hybrid FTS & DSL search |
| ⭐ | `ActionFavorite` | `Icons.Outlined.StarBorder` | `Icons.Filled.Star` | Toggle favorite status |
| 📌 | `ActionPin` | `Icons.Outlined.PushPin` | `Icons.Filled.PushPin` | Pin to top of shelves/lists |
| 🏷️ | `ActionTag` | `Icons.Outlined.LocalOffer` | `Icons.Filled.LocalOffer` | Add/manage category tags |
| ↗️ | `ActionLaunch` | `Icons.Filled.ArrowOutward` | — | Open original URL in browser/app |
| 📤 | `ActionShare` | `Icons.Outlined.Share` | `Icons.Filled.Share` | System share sheet forward |
| 🗑️ | `ActionTrash` | `Icons.Outlined.Delete` | `Icons.Filled.Delete` | Move item to 30-day trash |
| ♻️ | `ActionRestore` | `Icons.Outlined.Restore` | `Icons.Filled.Restore` | Restore item from trash |
| ✦ | `ActionSparkle` | `Icons.Outlined.AutoAwesome` | `Icons.Filled.AutoAwesome` | AI insights & summarization |

### 2.2 Content-Type Badge Glyphs

Each item type has a distinct container color, border tint, and vector glyph:

```
┌──────────────┬──────────────────────────────┬──────────────────┬─────────────────┐
│ Content Type │ Compose Vector Glyph         │ Badge Background │ Icon / Text Tint│
├──────────────┼──────────────────────────────┼──────────────────┼─────────────────┤
│ LINK / WEB   │ Icons.Filled.Language        │ Sky Container    │ AccentSky       │
│ REDDIT POST  │ Icons.Filled.Forum           │ Orange Container │ AccentOrange    │
│ YOUTUBE / VID│ Icons.Filled.PlayArrow       │ Rose Container   │ AccentRose      │
│ TWITTER / X  │ Icons.Filled.AlternateEmail  │ Slate Container  │ SlateDark       │
│ SCREENSHOT   │ Icons.Filled.CenterFocusWeak │ Amber Container  │ AccentAmber     │
│ IMAGE/PHOTO  │ Icons.Filled.Image           │ Emerald Container│ AccentEmerald   │
│ PDF / DOC    │ Icons.Filled.PictureAsPdf    │ Rose Container   │ #E11D48         │
│ NOTE / TEXT  │ Icons.Filled.Notes           │ Purple Container │ AccentPurple    │
│ AUDIO / VOICE│ Icons.Filled.Mic             │ Indigo Container │ #6366F1         │
│ LOCATION     │ Icons.Filled.Place           │ Teal Container   │ #0D9488         │
└──────────────┴──────────────────────────────┴──────────────────┴─────────────────┘
```

### 2.3 Platform Brand Glyphs

For recognized source domains, Tuck renders custom SVG platform badges:

- **Reddit**: Alien glyph in `#FF4500` on subtle orange tint.
- **YouTube**: Play button glyph in `#FF0000` on soft rose tint.
- **X / Twitter**: Minimalist monochrome glyph in `#0F1419` (Light) / `#FFFFFF` (Dark).
- **GitHub**: Octocat silhouette in `#24292F` / `#F0F6FC`.
- **Medium**: Monogram 'M' in `#12100E` / `#FFFFFF`.
- **Substack**: Bookmark orange in `#FF6719`.
- **ArXiv / Papers**: Scientific document glyph in `#B31B1B`.
- **Instagram**: Camera gradient badge in `#E4405F`.

### 2.4 Extracted Entity Badges

```
[ 👤 u/karpathy ]   [ 🏷️ machine-learning ]   [ 💵 $499.00 ]   [ 📅 Oct 24, 2026 ]   [ 🔗 github.com ]
  Person Entity        Topic / Tag Entity        Price Entity       Date Entity         URL Entity
  (Indigo Pill)        (Emerald Pill)            (Amber Pill)       (Sky Pill)          (Slate Pill)
```

---

## 3. Color Architecture & Theme Flavors

Tuck features five meticulously tuned color flavors. Each flavor provides consistent contrast ratios (≥4.5:1 for body text, ≥3:1 for large display text) and seamlessly transitions between Light and Dark environments.

```
       5 SIGNATURE PALETTES
┌─────────┬─────────┬─────────┬─────────┬─────────┐
│  LINEN  │  NOIR   │ FOREST  │ COBALT  │  PLUM   │
│ Warm    │ OLED    │ Sage &  │ Navy &  │ Velvet  │
│ Paper   │ Deep    │ Earth   │ Electric│ Muted   │
│ Classic │ Minimal │ Nature  │ Tech    │ Luxury  │
└─────────┴─────────┴─────────┴─────────┴─────────┘
```

### 3.1 Linen (Default Editorial Warm Paper)
*A soothing, tactile paper feel inspired by high-end typography journals.*

| Token | Light Mode Hex | Dark Mode Hex | Usage |
|:---|:---:|:---:|:---|
| `background` | `#FAF7F2` | `#181614` | Window background & root scaffolds |
| `surface` | `#FFFFFF` | `#221F1C` | App bars, bottom nav, dialogs |
| `surfaceCard` | `#F4EEE5` | `#2A2622` | Item cards, container chips, list items |
| `surfaceElevated` | `#FFFFFF` | `#322E29` | Floating HUDs, modals, speed dials |
| `surfaceVariant` | `#EDE5D8` | `#38332E` | Search input fill, entity pills |
| `accent` | `#E25C34` | `#FF7A50` | Primary actions, FAB, active indicators |
| `accentContainer` | `#FDEEE9` | `#3D261E` | Active nav indicator, selected pill fill |
| `textPrimary` | `#1C1917` | `#F7F4EE` | Headlines, item titles, primary text |
| `textSecondary` | `#6E665E` | `#B5ADA4` | Subtitles, author names, relative time |
| `textMuted` | `#9C9287` | `#7C746B` | Search placeholders, inactive icons |
| `border` | `#E8DFD3` | `#3B352E` | 1dp card borders, dividers |

### 3.2 Noir (High-Contrast Monochrome & OLED Dark)
*Ultra-clean, crisp minimalism for night owls and OLED power users.*

| Token | Light Mode Hex | Dark Mode Hex |
|:---|:---:|:---:|
| `background` | `#F5F5F7` | `#0D0D0E` |
| `surface` | `#FFFFFF` | `#161619` |
| `surfaceCard` | `#ECECEE` | `#1F1F24` |
| `accent` | `#E64A19` | `#FF6B4A` |
| `accentContainer` | `#FFEBE6` | `#3A1E17` |
| `textPrimary` | `#111113` | `#F4F4F6` |
| `border` | `#DCDCE2` | `#2E2E38` |

### 3.3 Forest (Sage & Earthy Botanical)
*A grounded, tranquil green palette for focused, low-eye-strain reading.*

| Token | Light Mode Hex | Dark Mode Hex |
|:---|:---:|:---:|
| `background` | `#F3F7F4` | `#101813` |
| `surface` | `#FFFFFF` | `#17231C` |
| `surfaceCard` | `#E8F0EA` | `#1E2E25` |
| `accent` | `#2D6A4F` | `#52B788` |
| `accentContainer` | `#E2EFE7` | `#1D3B2B` |
| `textPrimary` | `#122017` | `#ECF5EE` |
| `border` | `#D5E4D8` | `#2A4033` |

### 3.4 Cobalt (Deep Navy & Electric Sky)
*Technical, crisp, and energetic palette for power developers and researchers.*

| Token | Light Mode Hex | Dark Mode Hex |
|:---|:---:|:---:|
| `background` | `#F0F4F8` | `#0B132B` |
| `surface` | `#FFFFFF` | `#121E42` |
| `surfaceCard` | `#E3ECF5` | `#1A2B5E` |
| `accent` | `#2563EB` | `#38BDF8` |
| `accentContainer` | `#DBEAFE` | `#1E3A8A` |
| `textPrimary` | `#0E1726` | `#F0F9FF` |
| `border` | `#CFDDEB` | `#24386B` |

### 3.5 Plum (Muted Velvet Dusk)
*Rich, luxurious dark plum and warm rose accents for curated collections.*

| Token | Light Mode Hex | Dark Mode Hex |
|:---|:---:|:---:|
| `background` | `#F8F4F8` | `#18111B` |
| `surface` | `#FFFFFF` | `#221827` |
| `surfaceCard` | `#EFE6F0` | `#2D2034` |
| `accent` | `#8B2687` | `#E879F9` |
| `accentContainer` | `#F9E8FB` | `#451A4D` |
| `textPrimary` | `#1F0E24` | `#FAF5FB` |
| `border` | `#E4D4E6` | `#3E2A47` |

---

## 4. Typography & Spatial Hierarchy

Tuck uses an 8dp spatial scale with 4dp micro-increments, coupled with an editorial type ramp optimized for readability.

```
SPACING SCALE:
  xxs: 4dp  |  xs: 8dp  |  s: 12dp  |  m: 16dp  |  l: 20dp  |  xl: 24dp  |  xxl: 32dp  |  xxxl: 48dp

BORDER RADIUS SYSTEM:
  Small (12dp)  Medium (16dp)   Large (20dp)   Extra Large (24dp)   Pill (28dp)   Circle (50%)
  [Tag / Chip]  [Standard Card] [Cover Mosaic] [Modal Sheet]       [Search Bar]  [Avatar/FAB]
```

### Type Hierarchy Specifications

| Style Token | Font Size | Line Height | Weight | Tracking | Usage |
|:---|:---:|:---:|:---:|:---:|:---|
| `displayLarge` | 32sp | 38sp | ExtraBold (800) | -0.75sp | Major headers, Empty state heroes |
| `headlineLarge` | 28sp | 34sp | ExtraBold (800) | -0.5sp | Section headers ("Recently Saved") |
| `headlineMedium`| 22sp | 28sp | Bold (700) | -0.3sp | Card titles, Modal headers |
| `headlineSmall` | 18sp | 24sp | Bold (700) | -0.2sp | Item Detail title |
| `titleLarge` | 17sp | 23sp | Bold (700) | 0.0sp | Card headers, Collection names |
| `titleMedium` | 15sp | 21sp | SemiBold (600) | 0.0sp | Comment author, Filter group labels |
| `bodyLarge` | 15sp | 22sp | Medium (500) | 0.15sp | Article prose, Note body, Post text |
| `bodyMedium` | 13sp | 19sp | Medium (500) | 0.25sp | Card description snippets, OCR text |
| `bodySmall` | 12sp | 16sp | Normal (400) | 0.4sp | Secondary metadata, dates, hashes |
| `labelLarge` | 14sp | 20sp | Bold (700) | 0.1sp | Button text, FAB labels |
| `labelMedium` | 12sp | 16sp | SemiBold (600) | 0.5sp | Entity pills, Content badges |
| `labelSmall` | 11sp | 14sp | Medium (500) | 0.5sp | Relative timestamps, domain tags |

---

## 5. Component Design System

### 5.1 Content-Forward Saved Item Card (Grid & List Modes)

```
┌─────────────────────────────────────────────────────────────┐
│ ┌──────────────┐  Reddit · r/MachineLearning   ·  2h ago  ⭐│
│ │              │  How should I learn GNNs in 2026?         │
│ │  THUMBNAIL   │  u/karpathy · 2.3k ↑ · 127 comments       │
│ │   PREVIEW    │  "Start with GCN, implement PyG examples"  │
│ │  (16:9 / 1:1)│  [ 👤 u/karpathy ] [ 🏷️ PyG ] [ 📄 Research ]│
│ └──────────────┘                                            │
└─────────────────────────────────────────────────────────────┘
```

**Anatomy & Specs:**
- **Container**: `surfaceCard` background, 1dp `border` stroke, 16dp rounded corner shape.
- **Header Row**: Left-aligned `ContentTypeBadge` + Source Domain/Subreddit + Bullet separator + `RelativeTime`. Right-aligned `FavoriteStar` button (with spring pulse animation).
- **Title**: Up to 2 lines `titleLarge`, `textPrimary`, bold font weight.
- **Snippet / OCR / Intent Highlight**: `bodyMedium`, max 2 lines, `textSecondary`.
- **Entity Chip Row**: Horizontally scrollable or wrapping row of micro pills (`labelSmall`, 12dp height, `surfaceVariant` fill).

### 5.2 Dynamic Collection Mosaic Card

```
┌────────────────────────────────────────────────────────┐
│  RESEARCH PAPERS & CS                         [ 18 ]   │
│  ┌─────────────────────────┬─────────────────────────┐ │
│  │ [Image: ArXiv GNN]      │ [Image: Attention Doc]  │ │
│  ├─────────────────────────┼─────────────────────────┤ │
│  │ [Image: PyTorch Note]   │ [Image: Transformer]    │ │
│  └─────────────────────────┴─────────────────────────┘ │
│  Last updated 3 days ago · 4 smart topics              │
└────────────────────────────────────────────────────────┘
```
- A four-cell dynamic grid thumbnail that automatically assembles the 4 most recently added items' thumbnails with subtle 2dp internal gutters.

### 5.3 Unified Tuck Search Bar

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 🔍  Search anything (e.g. type:pdf in:research)                    ✖  │
└─────────────────────────────────────────────────────────────────────────┘
```
- **Pill Shape**: 28dp radius, `surfaceCard` unfocused fill, `surface` focused fill with `accent` 1.5dp stroke.
- **Leading Icon**: Search glass vector tinted in `accent`.
- **Trailing Controls**: Fast Clear `✖` icon button + Microphone / Filter trigger.

---

## 6. Screen-by-Screen Deep Dive & ASCII Visual Blueprints

---

### 6.1 Global Shell & Fluid Navigation

The root app structure provides seamless top-level switching with an animated bottom navigation bar and an omnipresent Quick Capture Floating Action Button.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         STATUS BAR (24dp)                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│                                                                         │
│                         ACTIVE SCREEN CONTENT                           │
│                      (Home / Inbox / Collections)                       │
│                                                                         │
│                                                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                    ┌──────────────────┐ │
│                                                    │   ＋ QUICK SAVE  │ │
│                                                    └──────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │    🏠 Home         📥 Inbox (3)      📁 Collections     🔍 Search   │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│                         NAVIGATION BAR (80dp)                           │
└─────────────────────────────────────────────────────────────────────────┘
```

**Key Navigation Specs:**
- **Navigation Bar**: 80dp height, Material 3 Expressive pill indicator with `accentContainer` tint for active tab.
- **Unfiled Counter Badge**: Pill badge on `Inbox` icon displaying pending unclassified saves.
- **Transitions**: Horizontal slide & fade (300ms easing) between main screens; shared element container transform when expanding an item into Detail view.

---

### 6.2 Home Screen — The Digital Memory Hub

The Home Screen acts as the personal morning dashboard, displaying what you saved recently, resurfacing forgotten gems, and offering instant jump points.

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 9:41                                                               85%  │
├─────────────────────────────────────────────────────────────────────────┤
│ Good morning, Ravi                                         ⚙️ Settings   │
│ 142 items archived  ·  3 unfiled in Inbox                               │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 🔍  Search anything, OCR text, or tags...                     🎙️   │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ QUICK CAPTURE                                                           │
│ [ 📝 Note ]   [ 📷 Scan OCR ]   [ 📄 Doc / PDF ]   [ 📋 Paste Link ]    │
├─────────────────────────────────────────────────────────────────────────┤
│ RECENTLY SAVED                                             See all (142) ›│
│ ┌──────────────────────┐  ┌──────────────────────┐  ┌─────────────────┐ │
│ │ [Reddit Thumbnail]   │  │ [PDF 1st Page]       │  │ [YouTube Cover] │ │
│ │ r/MachineLearning    │  │ Attention Is All...  │  │ Andrej Karpathy │ │
│ │ How to learn GNNs    │  │ ArXiv · 14 pages     │  │ Building GPT    │ │
│ │ 2 hours ago          │  │ Yesterday            │  │ 3 days ago      │ │
│ └──────────────────────┘  └──────────────────────┘  └─────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ ✦ RESURFACED MEMORY                                       [ Dismiss ✕ ] │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ "You saved this 4 months ago: 'Compare these laptops before buying'"│ │
│ │ Framework Laptop 16 Review: Modular & Repairable                     │ │
│ │ 🔗 theverge.com · ₹1,45,000 · [ Tech ] [ Hardware ]                 │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ FEATURED COLLECTIONS                                      + New Board › │
│ ┌───────────────────────────┐   ┌───────────────────────────┐           │
│ │ 📚 Research & Papers (24) │   │ 🛒 Wishlist & Gear (12)   │           │
│ │ [ 2x2 Mosaic Preview ]    │   │ [ 2x2 Mosaic Preview ]    │           │
│ └───────────────────────────┘   └───────────────────────────┘           │
└─────────────────────────────────────────────────────────────────────────┘
```

**UX Interactions & Polish:**
- **Search Header**: Tapping the search bar triggers a smooth shared-axis transition into the full Search Screen with the keyboard raised immediately.
- **Quick Capture Pills**: One-tap triggers for rapid ingest without leaving Home.
- **Resurfaced Memory Card**: Displays items with unread `open_count = 0` or matching time-based anchors (e.g. "Saved 1 year ago today").

---

### 6.3 Inbox Screen — Zero-Friction Triage Stream

The release valve for high-speed capturing. Everything saved lands here in chronological order until filed into a collection or archived.

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 📥 Inbox (3 Unfiled)                             [ Multi-Select ]  ⋮    │
│ "Save first, organize when you have time."                              │
├─────────────────────────────────────────────────────────────────────────┤
│ TODAY                                                                   │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 🌐 reddit.com/r/androiddev  ·  45m ago                           ⭐ │ │
│ │ Jetpack Compose 1.8 Release Notes & Performance Improvements        │ │
│ │ [ Swipe Right: 📁 Move to Collection ]   [ Swipe Left: 🗑️ Trash ]    │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 📷 Screenshot OCR  ·  2h ago                                      ⭐ │ │
│ │ [Image] "Flight UK-812 Departure Gate 4B boarding 18:30"             │ │
│ │ Extracted: [ ✈️ Flight ] [ 📅 Today 18:30 ] [ 📍 Gate 4B ]           │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ YESTERDAY                                                               │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 📄 PDF  ·  DeepSeek-V3 Technical Report                           ⭐ │ │
│ │ 54 pages · Extracted 12 key equations · [ 🏷️ AI ] [ 🏷️ LLM ]        │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ [ BOTTOM BATCH TOOLBAR - Shown during Multi-Select Mode ]               │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 2 selected   │   📁 Move (2)   🏷️ Tag   ⭐ Fav   📤 Export   🗑️ Del │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

**Key Gestures:**
- **Swipe Right**: Smooth green spring reveal to instantly assign to a Collection.
- **Swipe Left**: Rose red spring reveal to move item directly to Trash (with 5-second snackbar undo).
- **Long-Press**: Activates multi-select mode with checkmarks and floating batch toolbar.

---

### 6.4 Collections & Smart Categories Hub

A dual-tab view presenting User-Curated Collections and Auto-Generated Smart Categories.

```
┌─────────────────────────────────────────────────────────────────────────┐
│ 📁 Collections & Boards                         [ + New Collection ]   │
│ [ All Collections (8) ]       [ Smart Categories (12) ]                 │
├─────────────────────────────────────────────────────────────────────────┤
│ USER COLLECTIONS                                   Sort by: Recent ▾    │
│ ┌───────────────────────────┐   ┌───────────────────────────┐           │
│ │ 🔬 Machine Learning (38)  │   │ ✈️ Japan Travel 2026 (14) │           │
│ │ ┌───────────┬───────────┐ │   │ ┌───────────┬───────────┐ │           │
│ │ │ [Img 1]   │ [Img 2]   │ │   │ │ [Img 1]   │ [Img 2]   │ │           │
│ │ ├───────────┼───────────┤ │   │ ├───────────┼───────────┤ │           │
│ │ │ [Img 3]   │ [Img 4]   │ │   │ │ [Img 3]   │ [Img 4]   │ │           │
│ │ └───────────┴───────────┘ │   │ └───────────┴───────────┘ │           │
│ │ 📌 Pinned · Updated today │   │ Updated 4d ago            │           │
│ └───────────────────────────┘   └───────────────────────────┘           │
├─────────────────────────────────────────────────────────────────────────┤
│ SMART AUTO-CATEGORIES (Generated by Rules & Extracted Topics)           │
│ [ 💻 Programming (42) ]    [ 📄 Research Papers (19) ]                  │
│ [ 🛍️ Shopping & Prices (8) ] [ 🍳 Food & Recipes (11) ]                │
│ [ 💼 Work & Notes (15) ]    [ 🎬 Videos & Talks (9) ]                  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 6.5 Search & Retrieval Engine

The core power-tool of Tuck. Blends lexical SQLite FTS with parsed query DSL tokens and matched snippet highlighting.

```
┌─────────────────────────────────────────────────────────────────────────┐
│ ‹  [ 🔍 type:pdf in:research transformer_                         ] ✖   │
├─────────────────────────────────────────────────────────────────────────┤
│ PARSED FILTERS:                                                         │
│ [ Type: PDF ✕ ]   [ In: Research ✕ ]   [ Matched: "transformer" ]       │
├─────────────────────────────────────────────────────────────────────────┤
│ SEARCH RESULTS (3 matches in 12ms)                 Sort: Relevance ▾    │
│                                                                         │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 📄 Attention Is All You Need.pdf                                    │ │
│ │ ArXiv Research · 12 June 2024 · 1.4 MB                              │ │
│ │ 💬 Matched in Abstract (Page 1):                                    │ │
│ │ "...dominant sequence transduction models are based on complex      │ │
│ │ recurrent or convolutional neural networks. We propose <Transformer>│ │
│ │ based solely on attention mechanisms..."                            │ │
│ │ [ 🏷️ Attention ] [ 🏷️ NLP ] [ 🏷️ Vaswani ]                          │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 🌐 reddit.com/r/MachineLearning                                     │ │
│ │ Why Vision <Transformers> (ViT) outperform CNNs at scale            │ │
│ │ 💬 Matched in Comment by u/lucidrains:                              │ │
│ │ "...the self-attention matrix in the vision <transformer> scales..."│ │
│ └─────────────────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────────────────┤
│ DSL QUICK-CHEAT CHIPS (Shown when search bar is focused):               │
│ [ + source:reddit ] [ + type:screenshot ] [ + has:price ] [ + is:fav ]  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 6.6 Item Detail Screen — The Flagship View

The crown jewel of the application. Sectioned, distraction-free, and structured for maximum information retrieval.

```
┌─────────────────────────────────────────────────────────────────────────┐
│ ‹ Back      Reddit · r/MachineLearning             ⭐  📌  📤  ⋮ Menu  │
├─────────────────────────────────────────────────────────────────────────┤
│ How should I learn Graph Neural Networks in 2026?            [ ✏️ Edit ]│
│ u/karpathy · 2,340 upvotes · 127 comments · Saved 2 days ago            │
│ 🔗 https://reddit.com/r/MachineLearning/comments/xyz123                 │
├─────────────────────────────────────────────────────────────────────────┤
│ 💡 WHY I SAVED THIS                                         [ Edit ✏️ ] │
│ "Study this roadmap thoroughly after the NeurIPS deadline."             │
├─────────────────────────────────────────────────────────────────────────┤
│ ✦ AI KEY TAKEAWAYS (Derived from 127 comments)             [ Regenerate]│
│ • Start with Message Passing fundamentals               ⟵ 14 comments  │
│ • PyG (PyTorch Geometric) is overwhelmingly preferred    ⟵ 9 comments   │
│ • Graph Attention Networks (GAT) should be step 2        ⟵ 6 comments   │
│ *(Tap any takeaway to jump directly to cited source comment)*           │
├─────────────────────────────────────────────────────────────────────────┤
│ ORIGINAL POST BODY                                      [ Open Web ↗ ]  │
│ I'm looking to transition from computer vision to geometric deep        │
│ learning. What are the best lecture series, GitHub repos, and           │
│ practical projects to build from scratch?                               │
├─────────────────────────────────────────────────────────────────────────┤
│ THREADED COMMENTS (127)                    Filter: [ Top ▾ ]  [ 🔍 Find ]│
│                                                                         │
│ ┌ u/petar_velickovic (Top Contributor) · 412 ↑                          │
│ │ I would recommend starting with Petar Veličković's Cambridge lectures │
│ │ followed by the Stanford CS224W course by Jure Leskovec...            │
│ │                                                                       │
│ └─┬─ u/student_dev · 89 ↑                                               │
│   │   Is CS224W updated for the latest PyG release?                     │
│   │                                                                     │
│   └── u/petar_velickovic · 142 ↑                                        │
│       Yes, the 2025/2026 edition uses the new PyG 3.0 API.              │
├─────────────────────────────────────────────────────────────────────────┤
│ EXTRACTED ENTITIES & TOPICS                                             │
│ [ 👤 u/karpathy ]   [ 👤 u/petar_velickovic ]   [ 🏷️ GNN ]   [ 🏷️ PyG ]     │
│ [ 🔗 github.com/pyg-team ]                      [ 📅 NeurIPS 2026 ]     │
├─────────────────────────────────────────────────────────────────────────┤
│ 📝 PERSONAL NOTES & MARKDOWN HIGHLIGHTS                                 │
│ [ Tap to add personal notes, code snippets, or thoughts... ]            │
├─────────────────────────────────────────────────────────────────────────┤
│ TECHNICAL METADATA & FORENSICS (Collapsible)                            │
│ Content ID: 8f4b-21e9 · Local Cache: 245 KB · SHA-256: e3b0c442...      │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 6.7 Instant Share Sheet HUD & Capture Overlay

Triggered when the user shares content to Tuck from any Android application. It achieves a **sub-400ms** perceived save time and dismisses automatically.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                                                                         │
│             [ Translucent Blurred Background: 40% Opacity ]             │
│                                                                         │
│        ┌───────────────────────────────────────────────────────┐        │
│        │  TUCK  ·  SAVED INSTANTLY                      [ ✕ ]  │        │
│        │                                                       │        │
│        │  ✅  Saved to Inbox                                   │        │
│        │  "How should I learn Graph Neural Networks..."        │        │
│        │  Reddit · r/MachineLearning                           │        │
│        │                                                       │        │
│        │  FILE DIRECTLY INTO COLLECTION:                       │        │
│        │  [ + Research ]  [ + Tech ]  [ + Reading List ]  [+]  │        │
│        │                                                       │        │
│        │  📝 Add a quick note ("Why am I saving this?")...    │        │
│        │                                                       │        │
│        │  [ ▬▬▬ Auto-dismissing in 2.5s ▬▬▬ ]  [ Open in Tuck ]│        │
│        └───────────────────────────────────────────────────────┘        │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 6.8 Global Quick Capture Speed Dial Modal

```
┌─────────────────────────────────────────────────────────────────────────┐
│ QUICK CAPTURE                                                           │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 📝  Take a Quick Note (Markdown supported)                         │ │
│ ├─────────────────────────────────────────────────────────────────────┤ │
│ │ 📷  Scan Document / Screenshot (Local ML Kit OCR)                   │ │
│ ├─────────────────────────────────────────────────────────────────────┤ │
│ │ 📄  Import PDF or Research Paper (Native Page Renderer)             │ │
│ ├─────────────────────────────────────────────────────────────────────┤ │
│ │ 📋  Paste Link from Clipboard ("https://github.com/...")            │ │
│ ├─────────────────────────────────────────────────────────────────────┤ │
│ │ 🎙️  Record Audio Voice Memo (Local Storage)                         │ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 6.9 Favorites & Pinned Shelf

```
┌─────────────────────────────────────────────────────────────────────────┐
│ ⭐ Starred & Pinned Items (14)                            Sort: Name ▾  │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌───────────────────────────┐   ┌───────────────────────────┐           │
│ │ 📌 Attention Is All You...│   │ 📌 Andrej Karpathy - GPT  │           │
│ │ PDF · ArXiv Paper         │   │ YouTube Talk · 2h 14m     │           │
│ │ [Preview Thumbnail]       │   │ [Preview Thumbnail]       │           │
│ └───────────────────────────┘   └───────────────────────────┘           │
│ ┌───────────────────────────┐   ┌───────────────────────────┐           │
│ │ ⭐ WiFi Password at Home  │   │ ⭐ Tokyo Flight Ticket    │           │
│ │ Note · 4 lines            │   │ Image OCR · Terminal 2    │           │
│ └───────────────────────────┘   └───────────────────────────┘           │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 6.10 Settings, Vault & Storage Center

```
┌─────────────────────────────────────────────────────────────────────────┐
│ ‹ Back     Settings & Preferences                                       │
├─────────────────────────────────────────────────────────────────────────┤
│ APPEARANCE & THEME FLAVOR                                               │
│ [ ⚪ Linen (Warm Paper) ]   [ ⚫ Noir (OLED) ]   [ 🌿 Forest (Sage) ]    │
│ [ 🔵 Cobalt (Navy) ]       [ 🟣 Plum (Dusk) ]                          │
├─────────────────────────────────────────────────────────────────────────┤
│ STORAGE USAGE & CLEANUP                                                 │
│ Total Used: 48.2 MB / 128 GB Free                                       │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ ■ Images: 24.1 MB   ■ PDFs: 18.4 MB   ■ OCR Cache: 3.2 MB   ■ DB: 2.5│ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│ [ Clear Image Cache (12 MB) ]             [ Optimize Database (Vacuum) ]│
├─────────────────────────────────────────────────────────────────────────┤
│ PRIVACY & SECURITY                                                      │
│ 🔒 App Lock (Biometric / Fingerprint)                         [ Toggle ON]│
│ 🛡️ 100% Offline Guarantee: Zero analytics, zero cloud transmission.     │
├─────────────────────────────────────────────────────────────────────────┤
│ DATA VAULT & EXPORT                                                     │
│ [ 📤 Export Full JSON Vault ]         [ 📄 Export All to Markdown / Zip]│
│ [ 📥 Restore Vault Backup ]                                             │
├─────────────────────────────────────────────────────────────────────────┤
│ BACKGROUND WORKER QUEUE                                                 │
│ Active: 0   ·   Enriched: 142   ·   Errors: 0             [ Inspect ⚙️ ]│
└─────────────────────────────────────────────────────────────────────────┘
```

---

### 6.11 Trash & Safe Recovery Vault

```
┌─────────────────────────────────────────────────────────────────────────┐
│ ‹ Back     Trash (4 Items)                           [ Empty Trash 🗑️ ] │
│ "Items are permanently erased after 30 days."                           │
├─────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 🌐 Old Broken Link · example.com/old-page                           │ │
│ │ Deleted 4 days ago · 26 days remaining                              │ │
│ │ [ ♻️ Restore to Inbox ]                         [ ✕ Delete Forever ]│ │
│ └─────────────────────────────────────────────────────────────────────┘ │
│ ┌─────────────────────────────────────────────────────────────────────┐ │
│ │ 📷 Screenshot 2026-08-01                                            │ │
│ │ Deleted 12 days ago · 18 days remaining                             │ │
│ │ [ ♻️ Restore to Inbox ]                         [ ✕ Delete Forever ]│ │
│ └─────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Micro-Interactions, Motion Physics & Haptics

### 7.1 Motion Specs (Spring Constants)
- **Card Press**: `spring(dampingRatio = 0.75f, stiffness = 400f)` — gentle 2% scale dip with immediate tactile rebound.
- **Save HUD Ingress**: `slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(250))` with `OvershootInterpolator(1.2f)`.
- **Detail View Expansion**: Shared element boundary transform from Card Rect to Full Window Bounds (350ms duration, `FastOutSlowInEasing`).

### 7.2 Haptic Feedback Profiles
- **Quick Save Completed**: Double micro-tick (`HapticFeedbackType.LongPress` at 30% intensity).
- **Swipe Action Triggered**: Sharp crisp click (`HapticFeedbackType.TextHandleMove`).
- **Tag/Entity Tap**: Light tick (`HapticFeedbackType.GestureThresholdActivated`).

---

## 8. Step-by-Step Implementation Roadmap

Below is the verified implementation order. Each slice will be built and verified sequentially:

```
┌──────────────────────────────────────────────────────────────────────────┐
│                   STEP-BY-STEP IMPLEMENTATION SLICES                     │
├──────┬───────────────────────────────┬───────────────────────────────────┤
│ SLICE│ SCOPE                         │ PRIMARY DELIVERABLES              │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-1  │ Design Tokens & Theming       │ Update `Color.kt`, `Theme.kt`,    │
│      │                               │ `TuckDesignTokens.kt`, `Type.kt`  │
│      │                               │ with the 5 curated theme flavors. │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-2  │ Core Component Library        │ Build & polish `TuckCard`, badges,│
│      │                               │ entity chips, platform glyphs,    │
│      │                               │ search bars, and empty states.    │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-3  │ Global Shell & Navigation     │ Refine `TuckApp.kt` bottom nav,   │
│      │                               │ pill indicators, and Speed Dial.  │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-4  │ Home Screen Polish            │ Greeting hero, quick capture row, │
│      │                               │ horizontal carousel, memory card. │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-5  │ Inbox Screen Triage           │ Day-grouped items, swipe gestures,│
│      │                               │ batch multi-select toolbar.       │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-6  │ Collections & Smart Boards    │ 4-cell mosaic covers, smart query │
│      │                               │ badges, grid/list view switcher.  │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-7  │ Hybrid Search Screen          │ Live DSL chips, debounced FTS,    │
│      │                               │ match snippet highlighting.       │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-8  │ Flagship Item Detail Screen   │ In-place media header, intent note│
│      │                               │ threaded comments, entity drawer. │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-9  │ Share Sheet HUD & Capture     │ Sub-400ms floating glassmorphic   │
│      │                               │ card, pulse animation, quick tag. │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-10 │ Settings, Vault & Storage     │ Flavor switcher, storage chart,   │
│      │                               │ JSON/Markdown export, app lock.   │
├──────┼───────────────────────────────┼───────────────────────────────────┤
│ S-11 │ Verification & Polish         │ Macrobenchmarks, contrast audit,  │
│      │                               │ automated tests, demo recordings. │
└──────┴───────────────────────────────┴───────────────────────────────────┘
```
