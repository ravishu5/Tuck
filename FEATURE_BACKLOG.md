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

**Reddit: five threads, ~250 comments, supplied by Ravi and folded in as §I.** Reddit itself is
unreachable from this environment (fetch, `old.reddit.com` and the browser are all refused by
policy). §I is the highest-signal section in this document — user comments surface the complaints a
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
| **H1** | **Rule-based auto-filing** — "Reddit links go to my Reddit folder" | ✅ *shipped* | **The best find in this batch.** A real MarkMark review asks for exactly this and does not have it. Tuck already owns both halves: a query DSL parser and the unused `collections.isSmart` / `smartQuery` columns. A rule *is* a saved DSL query plus a destination. Turns Tuck's auto-organisation promise into something the user can actually steer |
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

# I. Reddit — 5 threads, ~250 comments

Sources: r/iosapps (launch, 94 threads), r/SideProject (10k users), r/macapps (ADHD post, 63
threads), r/ProductivityApps, r/iosapps (10k update). Dev is `u/N0omi`, solo, ~4 months post-launch.

## I-0. The finding that matters most

**The "save and forget" problem is the loudest unsolved complaint in the category, and the
developer has no answer for it.** Six separate users raised it independently:

> *"Save it and forget it. Just because I have a new place to save and sort my images doesn't mean I
> will ever use them… my problem is advancing the things I've organized into something actionable.
> If I had 4, 40, 400, or 40,000 images with information, I never look at them again after I save
> them."* — u/eternus

> *"If Stash makes it much easier to save everything, how do you help users actually return to what
> they saved later? Is the app meant to be mainly a searchable archive, or do you plan lightweight
> **review/reminder flows** so it doesn't become another place where saved things quietly pile up?"*
> — u/omegafi

> *"What's stopping this from piling up and becoming something else to micromanage?"* — u/GassyJr

Also u/couldhvdancedallnite (*"I feel like I would never go back to check it"*), u/movingimagecentral
(*"stashing endless ideas rather than doing something is a nightmare"*), u/i_like_pickles_too
(*"I'll still have problem finding the piece of info from this pile"*).

The developer's answer was that things pile up "but into categories." That is not an answer, and
users said so. **This is independent, unprompted confirmation of D1 (item lifecycle, reminders,
snooze) and it is the single clearest wedge available to Tuck.**

## I-1. Auto-categorisation — heavily demanded, philosophically refused

Requested by u/Barton5877, u/AndyKJMehta, u/rukola99, u/darknternal, u/CaptainMarder, u/thegurjyot,
u/Harikrishnareddy9 — seven users across three threads.

> *"Don't want to save and then pick a folder. I want it to automatically thematically group."*

The developer refuses, with a genuinely good reason:

> *"Categories overlap too much. Is this a breakfast recipe or a dinner recipe? Is this address a
> friend's house or just somewhere to park? There's no way any AI could reliably know that."*

**He is right that guessing fails, and wrong that the answer is nothing.** The answer is
**user-defined rules** — H1 — plus a suggestion the user can correct in one tap. Tuck already owns
the query DSL and the unused `isSmart`/`smartQuery` columns. This is a real strategic opening: strong
repeat demand, incumbent ideologically committed to not serving it.

## I-2. Trust and data portability — the deepest anxiety in the threads

| Quote | User |
|---|---|
| *"I'm always worried about apps going bottoms up, and I don't really see any way to export entered data."* | u/ababababaiopop |
| *"The real question is how to get the data out again."* | u/asiastar |
| *"It's hard to trust a service that's not established… Apple Notes does the job, and I'll know it'll be around."* | u/serifoblique |
| *"A vast majority of vibe coders abandon their projects… those few early adopters have to figure out how to get their data out."* | u/Bamboodl |
| *"Why can't I import a previously saved export?"* | u/ChurchOfSatin |

Note the last one: **Stash can export but cannot import.** An export you cannot restore is not a
backup. Tuck already has working import — that is a shippable trust claim today.

`u/bogdallica` articulated the "vibe coded" anxiety precisely: it is not that AI-built apps are bad,
it is that **the signal of commitment is gone**, so nobody knows whether the app will exist in a
year. Tuck's counter is structural, not rhetorical: open formats, documented schema, working
import, and everything on-device.

## I-3. Sync is their Achilles heel — twelve separate reports

u/Diggler_von_Anhalt, u/Sweaty-Attention768, u/camelopardalisx, u/ChurchOfSatin, u/Macreddit01
(repeatedly, and eventually *"Wonder if the App has been abandoned"* after no support reply),
u/arrogantheart, u/Came2PooOnlySharted, u/esdoenone (*"Mostly not. I have to sync it manually"*),
u/LucidXonline (syncing 3+ hours), u/wporchard, u/guigro (a `Containers/Stash` folder filling the
hard drive — *"Money lost unfortunately"*), u/per4o (stashes showing empty or half-synced).

Combined with the App Store review where a user **lost every saved item**, this is the incumbent's
defining weakness. Tuck should not ship sync until it can be additive, non-destructive, and
snapshotted before first run.

## I-4. Performance — they fall over at ~200 items

> *"It's got really laggy and takes ages to save or load… I think I have about 200 links."* — u/wporchard

> *"Quite laggy when it comes to viewing the screenshots. Look at how smoothly CaptureLab works."*
> — u/Budget_Valuable3992, who reported after the fix: *"Doesn't seem like it has the problems fixed."*

**200 items.** Tuck's 10,000-item performance target stops being an engineering nicety and becomes a
marketing claim.

## I-5. Accessibility — cost them at least one paying user

Font size raised by u/Syxball, u/Mythenmetz1, u/theAImajo (a grateful paying user who still could not
read it at max size) and u/blackcat562, who **uninstalled over it**:

> *"The tiny fonts… the option in settings changes both, and the smaller fonts are still tiny even
> when you change it to huge, so that sucks."*

The redesign brief already mandates 200% dynamic-type testing and contrast as a test rather than an
opinion. This validates it.

## I-6. Feature requests from the threads

Ranked by frequency and by how well Tuck is placed to serve them.

| # | Request | Status | Notes |
|---|---|---|---|
| **R1** | **Archive the page, not just the link** — *"What happens when you save a post and it gets deleted from the original location?"* (u/Anxious-Mango5143); *"Could it archive the content like Pocket?"* (u/skysurfer425, u/miomao10) | 🟡 | **People screenshot precisely because content disappears.** A link-saver that does not archive fails at the actual job. Tuck stores readable text but not a full snapshot — see H2 |
| **R2** | **Browser extension** — 7 users (u/Fylleth666, u/Maleficent_Air1940, u/TheVillageRuse, u/jaredkent wants Firefox, u/LucidXonline, u/theCuriousObserver02, u/closedmic_) | ❌ | The most-requested single feature after Android |
| **R3** | **Auto-delete the original screenshot after import** (u/skysurfer425) | ❌ | Directly serves the "clean my camera roll" job. Stash shipped it |
| **R4** | **Import from Instagram / TikTok / Facebook saved collections** (u/jl748795, u/Trailhawk8: *"this app would be absolute perfection"*) | ❌ | Technically hard, no public APIs. Worth investigating; huge if solved |
| **R5** | **Swipe between items full-screen like Photos** (u/_apatheticenthusiast) | ❌ | Real UX gap: currently tap in, back out, tap next |
| **R6** | **Preserve original photo metadata (date taken)** (u/nicebrah, u/Negative-Complex-482) | 🟡 | *"I'm holding off on deleting the originals because I need to know when a photo was taken."* Blocks the core job |
| **R7** | **Global timeline — all saves by date, without entering categories** (u/omurices) | 🟡 | Inbox is close but is triage-only |
| **R8** | **Paste a link instead of using Share** (u/LucidXonline) | ❌ | Trivial; removes a real friction on desktop-to-phone |
| **R9** | **Copy image to clipboard** to paste into WhatsApp (u/listexplode) | ❌ | Trivial |
| **R10** | **Animated GIF playback** (u/listexplode) | ❌ | GIFs render as stills |
| **R11** | **Video audio preserved** (u/IndieTeifling) | ❌ | Stash silently dropped audio — a user deleted originals first. Data-loss class bug; verify Tuck does not do this |
| **R12** | **Photo quality preserved** (u/mambo-2008: 3 MB → a few KB) | ❌ | Same class. Verify Tuck's copy path is byte-exact |
| **R13** | **Multi-language / localisation** (Italian, Spanish, Swedish, Portuguese users) | ❌ | Relevant for India |
| **R14** | **Regional / PPP pricing** (u/iam_malc: *"I live somewhere the currency is weak"*; u/TheBrainer0815 student) | ❌ | Directly relevant to an India-first launch |
| **R15** | **A–Z sort and manual reordering of collections** (u/LucidXonline) | 🟡 | `sortOrdinal` exists, no UI |
| **R16** | **Pin frequently used collections** (u/Jaybotics) | 🟡 | = B4 |
| **R17** | **List view alongside grid** (u/Jaybotics) | 🟡 | = B6 |
| **R18** | **Shortcuts / automation to add a note** (u/Sway_RL) | 🟡 | Android: Tasker intents, app shortcuts |
| **R19** | **System-wide search integration** (u/chrislaw Spotlight, u/enigma707 semantic index) | ❌ | Android: App Search / Assistant |
| **R20** | **Choose your cloud provider — Google Drive** (u/psychobeno, u/76: iCloud 5 GB cap is a bottleneck) | 🚫 | Deferred with sync, but note: **the dev plans Google Drive for his Android build** |
| **R21** | **Group a batch of imported screenshots as related** (u/skysurfer425) | ❌ | Nice fit with Tuck's multi-asset `media_assets` |
| **R22** | **Fetch the actual tweet: content + a rendered screenshot of the post** (u/Fylleth666) | 🟡 | Tuck's extractors are the right shape for this |
| **R23** | **App Store links save with almost no data** (u/per4o) | 🟡 | A specific extractor gap worth copying as a test case |
| **R24** | **Dark-mode share sheet** (u/unabatedshagie) and **themed/dark app icon** (u/hexegol) | ❌ | Android: themed icon (monochrome) support |
| **R25** | **Optional local AI summaries** (u/theCuriousObserver02, u/isrinivas) | 🚫 | Requested but explicitly cut from v1 |
| **R26** | **Onboarding is too long and the forced category step confuses** (u/NickNimmin, detailed); no help/instructions (u/kimvy, u/reditding, u/terza36) | ❌ | u/NickNimmin: *"I started tuning out 2 screens before the paywall."* Tuck has no onboarding at all yet — build it right the first time |

## I-7. Competitors named by users

**Pool / pool.day** (VC-backed, named 3×) · **Gladys** · **Raindrop.io** (3×) · **GoodLinks** ·
**Pocket** · **Anybox** · **Capture / CaptureLab** (praised for smoothness) · **Pile** ·
**Albo** (albo.inc) · **Resurf** (praised for Mac capture UX) · **usemosaik.com** · **joinrecall** ·
plus Notion, Evernote, Apple Notes and Photos albums as the incumbent behaviours.

The recurring *"why not just use Photos albums?"* objection (u/PersonoFly, u/atiaa11) is worth
answering directly in positioning: Photos albums are references, not an archive, and they cannot
hold links, PDFs or posts.

## I-8. Business and growth intel

- **10,000 downloads in 3 months, no ads, almost entirely Reddit** — posting, absorbing feedback,
  posting the update, repeating.
- **12–15% free→paid conversion.** Extremely high for consumer.
- Free tier: **100 items, 10 categories.** Pro $9.99 lifetime promo, $19.99 standard, $7.99/yr.
- **Price doubled and revenue stayed flat**, per u/Smart-Button1428 — demand is elastic in that band,
  so the model is the lever, not the price.
- **Posting repeatedly caused backlash** (*"Do you post this every month?"*, downvotes). One post per
  subreddit per genuine milestone is the sustainable cadence.
- **"Is this vibe coded?" was the top comment on the biggest thread**, with ~110 upvotes across the
  accusations. This is now a default suspicion for any new indie app. Tuck's defence is evidence:
  tests that run on device, a public repo, a documented schema, and working import.
- **App Store screenshots drove installs directly** — *"Downloaded just because of the screenshots"* —
  though another user found them *"a bit overwhelming."* The redesign work pays for itself here.
- **Android is the #1 request the developer receives**, and he stated in mid-June it was
  *"hopefully a month or so"* away, using Google Drive for sync. **That window is now effectively
  closed or closing.** Tuck's advantage has to be depth, not being first.

---

# G. Suggested order

*Revised after the Reddit threads. Two things changed: the lifecycle argument went from my inference
to directly evidenced user demand, and data-loss-class bugs moved to the front because two of them
cost the incumbent users who had already deleted their originals.*

**The through-line:** the incumbent wins on capture and loses on *return*. Every complaint that
matters — save-and-forget, sync data loss, lag at 200 items, unreadable text, no import — is about
what happens **after** the save. That is where Tuck should compete.

### Phase 0 — the wedge: make saving lead somewhere
- [x] **D1 — item lifecycle: reminders, snooze, done.** *Shipped 2026-08-29.* `remindAt` and
  `completedAt` via migration 5→6, four coarse presets, a WorkManager reminder with Done and
  Snooze notification actions, and a FOLLOW UP section on the detail screen. Verified on device:
  tapping "Tomorrow" stored 09:00 the next morning and scheduled the job. Deliberately **no status
  enum** - the evidence pointed at "bring it back" and "I dealt with it", nothing more
- **D2 — first-run bulk screenshot import**, plus **R3 auto-delete the original** after import
- [x] **H1 — user-defined auto-filing rules.** *Shipped 2026-08-29.* A rule is a query-DSL string
  plus a destination collection, so the search syntax doubles as the rule language and there is
  nothing new to learn. Runs after enrichment (a `source:` rule needs the domain), additive to the
  classifier, and a rule stating no condition is rejected in both the UI and the engine - a rule
  that silently files everything is the worst failure here. Verified on device: `source:reddit`
  filed a shared Reddit link into Articles alongside the classifier's own choice
- **B2 — smart dates from OCR → offer a reminder.** Regex, no AI
- **A9 — surface the category suggestion in the share sheet** · **C3 — haptics**

### Phase 0.5 — data-loss bugs — *done 2026-08-29*
- [x] **R11/R12 — audio and image quality preserved.** The copy path was already a verbatim byte
  stream; six instrumentation tests now prove it by SHA-256 rather than assuming it
- [x] **R6 — original capture time preserved** via `capturedAt` (migration 4→5), read from EXIF.
  It was silently dropped in *two* further places - the repository mapping and the vault
  export/import - so an export/restore round-trip would have lost it
- [x] File type no longer forced: every image was written `.jpg` and every video `.mp4`, and the
  importer hardcoded `image/png` over every JPEG in the gallery
- [ ] **Detail screen spins forever when an item does not exist** — found while testing; the
  loading state never resolves for a missing id instead of showing an empty state

### Phase 1 — retrieval made visible
B3 search on home · D4 match-reason chips · B5 bulk select everywhere · R7 global timeline ·
R5 full-screen swipe between items · B9 storage screen · B13 richer details

### Phase 2 — organisation catch-up
B6 grid view (redesign R4) · B4/R16 pin collections · B7/B8 sub-stashes · C2 icon and colour pickers ·
R15 sort and reorder · A18/B16 duplicate merge · C5 hidden collections

### Phase 3 — ownership and trust *(the incumbent's soft underbelly)*
B10 Markdown export · **working import** (Stash cannot restore its own export) · D3/H4 importers with
**H5 folder preservation** · **R1 offline page archive** — people screenshot because content
disappears · H3 reader view · H6 refresh a save

### Phase 4 — hardening before launch
D10 measured budgets — the incumbent lags at **200 items**, so 10k is a marketing claim ·
baseline profile · APK size · **R26 onboarding**, built once and built short ·
**I-5 accessibility**: dynamic type to 200% and contrast, which cost the incumbent a paying user

### Phase 5 — differentiators that need volume
D6 entities · D8 comment-tree reading · D9 actionable entities · D7 source/derived convention ·
R22 richer social extraction

### Phase 6 — reach
R2 browser extension (7 requests, second only to Android) · R13 localisation · R14 regional pricing ·
R19 system search integration · R24 themed icon and dark share sheet

**Deferred pending decisions:** everything marked 🚫 — sync, shared collections, anything AI-shaped.
Note that sync is both the most-requested capability *and* the incumbent's most damaging failure.
When Tuck builds it: local is the source of truth, sync is additive and never destructive, and a
local snapshot is taken before the first run.

---

## Positioning notes drawn from the threads

- **Free tier.** Theirs is 100 items / 10 categories. Tuck has no server costs at all, so a
  genuinely generous free tier is cheap differentiation — and their own numbers show price is not the
  lever (doubling it left revenue flat).
- **"Is this vibe coded?" was the top comment on their biggest thread.** This is now the default
  suspicion for any new indie app. Tuck's answer should be evidence rather than protest: a public
  repo, tests that run on a real device, a documented schema, and an import that actually restores.
- **Answer the "why not just use Photos albums?" objection directly** — it came up repeatedly.
  Albums are references, not an archive, and they cannot hold links, PDFs or posts.
- **Do not market Tuck as an ADHD app.** Their ADHD-framed post drew the most hostility in the whole
  corpus, including from people with ADHD. One commenter put it well: it is useful to everyone, and
  the framing narrows reach.

## Still missing from this document

The four Reddit threads. Reddit is blocked by policy here. Paste their text — especially the
comments — and I will merge the findings in and re-rank.
