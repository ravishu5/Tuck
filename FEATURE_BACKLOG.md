# Tuck — Compiled Feature Backlog

Sources reviewed on 2026-08-29:

- `stashanything.com` — marketing feature list and pricing (rendered in a browser; the page is
  JS-only and returns an empty shell to plain fetches)
- Stash Anything on the App Store (`id6758998468`) — full description, **version history**, and
  **user reviews**. The changelog is the most useful artifact of the lot: what a solo developer
  shipped *after* launch is a direct readout of what users demanded
- `github.com/savewithstash/stash` — **a different product that shares the name.** This is a
  self-hosted, local-LLM app (Docker, Raspberry Pi, Umbrel, Qwen/Gemma models, AGPL). It is not the
  iOS app's source. Still mined for ideas below
- Web search for category-wide complaints
- Prior analysis in this repo (`TASKS.md`, `REDESIGN_TASKS.md`, and the lifecycle/import argument)

**Not reviewed: the four Reddit threads.** Reddit is blocked by policy in this environment — plain
fetch, `old.reddit.com` and the browser were all refused. Nothing from those threads is in this
document. Paste the post and comment text and I will fold it in; the competitor's *own* changelog and
reviews already cover much of the same ground, but user comments usually surface complaints the
developer never advertises.

**Competitive note:** the App Store listing says Stash is on iPhone, iPad, Mac and Vision, and the
website has an **"Android coming soon"** waitlist. The window Tuck is aiming at is real but closing.

---

## Legend

| Mark | Meaning |
|---|---|
| ✅ | Already built in Tuck |
| 🟡 | Partly built — the gap is stated |
| ❌ | Not built |
| 🚫 | Blocked by a decision already taken (AI cut, cloud deferred) |

---

# A. Already in Tuck — do not rebuild

| # | Feature | Notes |
|---|---|---|
| A1 | Share-sheet capture from any app | ✅ `SEND`, `SEND_MULTIPLE`, `PROCESS_TEXT` |
| A2 | Share targets in the system share sheet | ✅ `<share-target>` declared |
| A3 | Instant save, background enrichment | ✅ bytes copied before the sheet can be dismissed |
| A4 | OCR text search inside screenshots/photos/PDFs | ✅ ML Kit → `ocr_blocks`, indexed |
| A5 | PDF text + first-page thumbnail | ✅ |
| A6 | Rich link preview cards | ✅ OpenGraph + platform extractors |
| A7 | Full-text search, local | ✅ FTS4 with weighted relevance ranking |
| A8 | Filter by type, date, favorites, collection | ✅ plus a query DSL Stash has no equivalent of |
| A9 | Category suggestion on save | 🟡 `RuleBasedContentClassifier` exists but is **not surfaced in the share sheet** |
| A10 | Inbox with swipe triage | ✅ Stash calls this "Tinder for your links" |
| A11 | Bulk select | 🟡 Inbox only; Stash shipped it "in any stash" |
| A12 | Trash with restore | ✅ |
| A13 | Face ID / biometric locked collections | ✅ wired at `CollectionsScreen.kt:228` |
| A14 | Offline reading | ✅ genuinely offline-complete, including comment trees |
| A15 | No account, no tracking, no ads | ✅ and stronger than Stash: no cloud at all |
| A16 | Quick Settings tile, widget, app shortcuts | ✅ Android-only surfaces Stash cannot match |
| A17 | Nested comment capture | ✅ **nothing in this category does this** |
| A18 | Duplicate detection | 🟡 detects and stores `dedupeGroupId`; no merge UI |
| A19 | Local export / import | ✅ JSON vault + `.tuck` packs |

---

# B. Shipped by the competitor after launch — i.e. what users actually asked for

These come from the App Store version history. Ranked by my read of value to Tuck.

| # | Feature | Status | Why it matters |
|---|---|---|---|
| B1 | **Reminders on saved items** | ❌ | The single biggest gap. Turns an archive into a workflow — see §D1 |
| B2 | **Smart dates — read dates out of images via OCR and offer a reminder** | ❌ | Tuck already extracts `DATE` entities from OCR text. This is regex + a notification, **no AI needed**. Screenshot a concert poster → "Remind me 14 Sep?" |
| B3 | **Search from the home screen**, no tab switch | 🟡 | Home has a search field that navigates to the Search tab; make it search inline |
| B4 | **Pin favourite collections to top** | 🟡 | Item pinning exists; collection pinning does not |
| B5 | **Bulk select in any stash**, not just triage | 🟡 | Extend beyond Inbox |
| B6 | **Grid view for visual browsing** | ❌ | No `LazyVerticalGrid` anywhere. Covered by `REDESIGN_TASKS.md` R4 |
| B7 | **Sub-stashes (nested collections)** | 🟡 | `parentId` column exists, no UI |
| B8 | **Filter a stash by "Just here" / "Everything" / one sub-stash** | ❌ | Depends on B7 |
| B9 | **Storage screen + one-tap "Reclaim Space"** | 🟡 | Settings shows usage and clears cache; no per-category breakdown or reclaim |
| B10 | **Markdown export** | ❌ | Also the honest answer to "your data is yours" |
| B11 | **Copy text out of images and PDFs; select text in full-screen photos** | ❌ | OCR text is captured but the user cannot *use* it. Cheap, very satisfying |
| B12 | **Change the preview image for a save** | ❌ | Small, high perceived polish |
| B13 | **Richer item details** — dates, location, dimensions, file size, video length | 🟡 | Some captured, not surfaced |
| B14 | **Bulk photo import (up to 50 at once)** | 🟡 | `importAllScreenshots()` exists but is called with `limit = 30` recent — see §D2 |
| B15 | **Emoji icons for categories** | 🟡 | `collections.icon` exists; emoji picker does not |
| B16 | **Duplicate tidying on sync** | 🟡 | Detection exists, resolution does not (A18) |
| B17 | **Better link capture from Facebook, LinkedIn, X, Instagram** | 🟡 | Extractor registry exists; these platforms degrade to OG metadata |
| B18 | **Save via action button / back tap / Siri** | ❌ | Android equivalents: Assistant, gesture shortcuts, `PROCESS_TEXT` (have), Quick Tile (have) |

---

# C. On their marketing page, not yet in Tuck

| # | Feature | Status | Notes |
|---|---|---|---|
| C1 | 18 curated starter categories with icons **and colours** | 🟡 | Tuck auto-creates categories from the classifier; `collections.color` is unused. Covered by `THEME_TASK.md` |
| C2 | Unlimited custom categories with custom icon + colour | 🟡 | Creation works; icon/colour pickers missing |
| C3 | Haptic confirmation on save | ❌ | One line. Disproportionate effect on how "instant" a save *feels* |
| C4 | Reorder categories manually | 🟡 | `sortOrdinal` exists, no drag UI |
| C5 | Hidden collections (distinct from locked) | ❌ | Hidden = not shown; locked = biometric. Tuck has locked only |
| C6 | Shared / collaborative collections, real-time | 🚫 | Needs the deferred cloud layer |
| C7 | Live shared stash via a public link | 🚫 | Same |
| C8 | Cross-device sync | 🚫 | Deferred. See §F1 before building it |
| C9 | "Save time 0.3s" as a public claim | ❌ | Tuck has never measured share-to-save. Worth measuring, then claiming |

---

# D. From analysis of Tuck itself — not on any competitor's list

This is where Tuck can be better rather than equal.

| # | Feature | Why |
|---|---|---|
| **D1** | **Item lifecycle: status + reminders + snooze** | The structural gap. A saved item today has four booleans (`isPinned`, `isFavorite`, `isArchived`, `isDeleted`) and no notion of intent, state or completion. People save in order to *do* something — read it, buy it, cook it, apply to it. Add a status (`INBOX` / `TO_ACT` / `DONE`), a `remindAt`, and a snooze. `captureNote` already records *why* something was saved and nothing ever acts on it. WorkManager and notification plumbing already exist. **Highest leverage item in this document** |
| **D2** | **Day-one bulk import of the existing screenshot library** | For the first ~2 weeks a save-anything app is *worse* than screenshots because there is nothing to retrieve — which is exactly when people churn. The user already owns thousands of screenshots. `importAllScreenshots()` is written and is being called with `limit = 30`. Turning it into a first-run "import your screenshot history" flow, OCR'd and searchable, hands the user a valuable archive on day one instead of day thirty. **Highest retention-per-line-of-code in the project** |
| D3 | **Import from browser bookmarks / Pocket / Raindrop / Instapaper** | Same argument as D2. A category-wide complaint found in search was wanting to import from another service |
| D4 | **Match-reason chips in search results** | Tuck knows *why* something matched — title vs OCR vs a comment. Stash shows one `OCR` badge. Showing this on every result is a visible advantage |
| D5 | **Reading progress / unread state for long articles** | `openCount` and `lastOpenedAt` already exist; unread is free |
| D6 | **Entity chips and entity pages** | `TASKS.md` Phase A. See the sequencing note in §G |
| D7 | **Source vs derived visual convention** | Nobody else distinguishes captured content from computed content. It is a trust feature |
| D8 | **Comment-tree reading experience** | Tuck archives 300-comment Reddit threads. No competitor has a mobile design for reading one offline |
| D9 | **Actionable OCR entities** | A screenshot with a phone number → tap to call; an address → tap to map; a price → track. Entities are extracted and currently inert |
| D10 | **Measured performance budgets** | Cold start, share-to-save, search latency. Never measured; Stash advertises 0.3s |

---

# E. From the self-hosted `savewithstash/stash` project

Different product, same problem space. Most of its value is AI-shaped and therefore blocked, but two
ideas survive without a model.

| # | Idea | Status |
|---|---|---|
| E1 | **"Instant saves, lazy AI"** — item appears immediately, enrichment queued | ✅ Tuck's architecture already does exactly this |
| E2 | **Reminder parsing to a due date** | ❌ Achievable with regex over OCR/text — this is B2/D1, no model required |
| E3 | Heuristic instant classification before enrichment | 🟡 Tuck classifies after enrichment; doing a cheap pass *before* would let the share sheet suggest a category instantly (A9) |
| E4 | Local vision captioning of screenshots for search | 🚫 Needs a vision model |
| E5 | Ask mode with citations, semantic search, chat history | 🚫 AI cut from v1 |
| E6 | Swappable model settings with device badges | 🚫 Same |
| E7 | Grammar-constrained JSON so model output never fails to parse | 🚫 Relevant only if AI returns — worth remembering then |

---

# F. Lessons from their failures — anti-features

These come from actual App Store reviews and are worth as much as the feature list.

| # | Lesson |
|---|---|
| **F1** | **Their worst review is data loss through sync.** A user reported iCloud stuck "Syncing" for 20+ minutes, toggled settings, and **lost all saved items**; the developer confirmed a known CloudKit bug. This is the single most damaging failure a memory app can have. When Tuck eventually builds sync: local is the source of truth, sync is additive, never destructive, and a local snapshot is taken before any first sync. Tuck's current local-first position is a genuine advantage — do not trade it away casually |
| F2 | **Accessibility was raised by a visually impaired reviewer** (contrast). Tuck's redesign brief already treats contrast as a test, not an opinion — keep it that way |
| F3 | **A home section ("Jump Back In") was called distracting.** Tuck has the equivalent in "Rediscover". Make resurfacing dismissible and quiet — this is already partly handled |
| F4 | **Reviewers wanted a trial before the lifetime purchase.** Relevant whenever Tuck monetises |
| F5 | **Their free tier is 100 items / 10 categories.** That is tight. Generosity here is cheap differentiation for a local-first app with no server costs |
| F6 | **Raindrop was criticised** for not handling screenshots, not processing images, and not capturing from Instagram/TikTok — browser-first tools lose on mobile capture. Tuck's capture breadth is the moat |
| F7 | **Users asked for bulk delete by tag or collection.** Covered by B5, but note the *by-tag* dimension |

---

# H. Adjacent competitors (App Store, 2026-08-29)

Reddit is unreachable from here, so I went at the same question from the App Store side. Four apps
in the neighbouring space, mined for features and — more usefully — for their reviews.

**MarkMark — Read it Later** (iPhone/iPad/Mac; free, Pro $1.99/mo, $9.99/yr, $14.99 lifetime)
**Save for Later: AI Bookmarks** (free, $1.99/mo, $8.99/yr, $19.99 lifetime)
**Recall for Reddit** (Mac, $4.99) · **Everything-Save All** ($1.99; minimal, nothing to learn)

| # | Feature | Status | Notes |
|---|---|---|---|
| **H1** | **Rule-based auto-filing** — "Reddit links go to my Reddit folder" | ❌ | **The best find in this batch.** A real MarkMark review asks for exactly this and does not have it. Tuck already owns both halves: a query DSL parser and the unused `collections.isSmart` / `smartQuery` columns. A rule *is* a saved DSL query plus a destination. Turns Tuck's auto-organisation promise into something the user can actually steer |
| H2 | **Offline web archive** — full page snapshot, not just readable text | 🟡 | Tuck stores `readable_html` and a thumbnail; it does not snapshot images or CSS. Real gap for the "archive" claim |
| H3 | **Reader view** — distraction-free reading | 🟡 | Detail has webview vs snapshot tabs; there is no typographic reading mode |
| H4 | **Import from Pocket / Raindrop / Omnivore / browser bookmarks** | ❌ | Both apps ship this; Save for Later advertises "in one tap". Reinforces D3 |
| H5 | **Preserve folder structure on import** | ❌ | A MarkMark reviewer complained the import "didn't take any of the folders I had taken the time to organize". Cheap way to be better than the incumbent on day one |
| H6 | **Refresh / re-fetch an existing save** | 🟡 | Tuck can retry enrichment internally; it is not a user-visible action. Useful when a page 404s or a title was bad at capture time |
| H7 | **Ratings or rankings on saves** | ❌ | Minor, but a cheap signal for surfacing later |
| H8 | **Shake for a random saved item** | ❌ | Playful rediscovery. Fits the "memory" positioning better than it fits a bookmark manager |
| H9 | **Global quick-search shortcut** (Cmd+K in Recall) | 🟡 | Android equivalents: search directly from the widget, the QS tile, and a notification action |
| H10 | **Passive capture** — record every Reddit post *visited*, not just saved | ❌ | Recall's whole premise. Powerful for recall, heavy on privacy, and not really viable on Android without accessibility-service overreach. **Recommend against** |
| H11 | **Import existing history on first install** | ❌ | Recall does this too. Independent confirmation of D2 — every app that solves day-one emptiness does it by importing something the user already has |
| H12 | **Localisation** | ❌ | Save for Later ships German and Brazilian Portuguese. Relevant if India is a target market |

**Pricing intel.** Lifetime sits in a tight $9.99–$19.99 band across all three paid competitors:
Stash $9.99 (promo from $19.99), MarkMark $14.99, Save for Later $19.99. Annual is $8–10.

**Competitive read.** Nobody in this set does OCR *and* nested comment capture *and* ranked search
with match provenance. Recall for Reddit is Reddit-only and Mac-only. Stash is the closest overall
and is coming to Android. The gap Tuck can hold is **depth of capture plus quality of retrieval**,
not breadth of platform.


---

# G. Suggested order

Reasoning rather than a list: **the entity/knowledge-graph work in `TASKS.md` Phase A is sequenced
too early, and I wrote it that way.** An entity graph needs volume to be useful — with 20 saves,
"everything about Ravi" is an empty page and topic extraction is noise. It pays off around 500+
items, which describes a user you do not have yet. D1 and D2 are what create that user.

**Phase 0 — make it productive and non-empty**
D1 lifecycle + reminders · D2 first-run screenshot import · **H1 rule-based auto-filing** ·
B2 smart dates → reminders · C3 haptics · A9 surface the category suggestion in the share sheet ·
B11 copy text out of images

**Phase 1 — retrieval made visible**
B3 search on home · D4 match-reason chips · B5 bulk select everywhere · B9 storage screen ·
B13 richer details

**Phase 2 — organisation catch-up**
B6 grid view (via redesign R4) · B4 pin collections · B7/B8 sub-stashes · C2 icon + colour pickers ·
C4 reorder · A18/B16 duplicate merge · C5 hidden collections

**Phase 3 — ownership**
B10 Markdown export · D3/H4 importers (H5: preserve folders) · B12 custom preview image ·
H2 offline web archive · H3 reader view · H6 refresh a save

**Phase 4 — hardening before any launch**
D10 measured budgets · baseline profile · APK size decision (43.6 MB against a 25 MB budget)

**Phase 5 — the differentiators that need volume**
D6 entities · D8 comment-tree design · D9 actionable entities · D7 source/derived convention

**Deferred pending decisions:** everything marked 🚫 — sync, shared collections, and anything
AI-shaped.

---

## Still missing from this document

The four Reddit threads. Reddit is blocked by policy here. Paste their text — especially the
comments — and I will merge the findings in and re-rank.
