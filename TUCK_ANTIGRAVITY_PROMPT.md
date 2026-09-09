# Tuck — Build Brief for Antigravity Agent

> Paste this entire document into Antigravity as the first message of a new Manager/Agent session,
> **or** commit it to the repo root and open with:
> *"Read `TUCK_ANTIGRAVITY_PROMPT.md` end to end. Do not write code yet. Produce the Milestone 0 audit + plan artifact first."*

---

## 0. How you (the agent) must work

1. **Plan before code.** For every milestone, first emit a plan artifact: files you will create/modify/delete, schema changes, new dependencies, risks, and the verification steps. Wait for approval before editing.
2. **One milestone at a time.** Never start M(n+1) work while M(n) acceptance criteria are unmet. No speculative abstractions for future milestones.
3. **Maintain two living files in the repo root:**
   - `PROGRESS.md` — the milestone checklists from §18, with `[ ]` → `[x]` only when the acceptance criteria in this doc are verifiably met (build passes + test passes + emulator demo captured).
   - `DECISIONS.md` — one entry per non-obvious choice: `Date | Decision | Alternatives considered | Why | Reversible?`
4. **Verify on a real emulator, not by reasoning.** Every milestone ends with: `./gradlew assembleDebug`, unit tests green, install on an API 35 emulator, run the demo script for that milestone (§18), and attach screenshots/recording to the walkthrough.
5. **Dependency policy.** Ask before adding any library. Justify with: what it replaces, APK size delta, maintenance status, licence. Default answer is "write 100 lines ourselves."
6. **Never fabricate.** If an extractor cannot get a field, store `null` and a `failure_reason`. Never invent a title, author, score, or date. Never let AI output overwrite source data.
7. **No network calls in unit tests.** Extractors are tested against checked-in HTML/JSON fixtures under `app/src/test/resources/fixtures/`.
8. **Stop and ask** if a requirement here conflicts with something you find in the code. Do not silently reinterpret.

---

## 1. Mission

Build **Tuck** — an Android-first personal memory system.

**Tagline:** *Save anything. Find everything. Remember what matters.*

The core loop is exactly this and nothing else:

```
See something → Share to Tuck → Saved instantly → Never lose it again
```

It is explicitly **not**:

```
Open app → create folder → add item → fill metadata → save
```

The market has proven people will use a share-sheet save-for-later app. Tuck's differentiation is what happens *after* the save: Tuck captures the **real content** (a Reddit post *and its comment tree*, not a URL), preserves it offline forever, derives structure from it (key points, important comments, entities, OCR, embeddings), and makes it **retrievable months later by fuzzy human memory** ("that Reddit thread about GNNs").

Positioning ladder: bookmark manager → archive → **personal memory for the internet**.

---

## 2. The ten product laws (violating any of these is a bug)

1. **Save must never block.** Persist to disk in <400ms perceived. Enrichment, metadata, OCR, extraction, AI — all background, all resumable, all optional.
2. **Save must never fail.** If the URL is unreachable, the file is huge, the parser breaks — the item is still saved with whatever we got, marked `PARTIAL`, and retried later.
3. **No mandatory organisation.** Saving requires zero decisions. Collection assignment is optional, always. "Inbox" is a first-class destination, not a failure state.
4. **Source content is sacred.** Original post, comments, text, media, author, timestamp, URL are immutable once captured. AI output is *additive metadata*, stored separately, always labelled, always deletable and regenerable. The user must always be able to see the original.
5. **AI is optional and off by default.** The app must be fully useful with every AI feature disabled. No feature may be reachable only through AI.
6. **Local-first.** Everything works offline with the plane in flight mode. No account required to use the app, ever. Sync is opt-in and additive.
7. **Nothing leaves the device without explicit, per-feature, informed consent.** Show exactly what would be sent, to whom, before the first send.
8. **Retrieval beats storage.** Any feature that adds save-time friction to improve organisation is the wrong trade. Fix retrieval instead.
9. **Items are multi-home.** An item belongs to zero or many collections. No forced hierarchy, no single-parent folders.
10. **Deletion is deliberate.** Trash → restore → explicit empty. Media files are only unlinked from disk after the DB row is hard-deleted.

---

## 3. Current repo state (audit before you touch anything)

The repository at `/Users/ravi/Desktop/Tuck` is **brownfield and polluted**. It contains two overlapping projects:

**A. The real app** — Kotlin/Compose, package `com.tuck.app`, ~75 source files, Room schema v2, minSdk 26, targetSdk/compileSdk 35, Hilt + KSP + WorkManager + Coil + ML Kit + jsoup + DataStore. Already implemented, roughly:
- `ui/share/ShareActivity` — `ACTION_SEND` / `ACTION_SEND_MULTIPLE`, translucent dialog theme, instant-save HUD
- `processing/` — `ShareParser`, `UrlMetadataProcessor` (jsoup/OG), `ImageOcrProcessor` (ML Kit), `PdfProcessor` (PdfRenderer), `EntityExtractor` (regex), `RuleBasedContentClassifier`, `DuplicateDetector`, `ScreenshotImporter`, `ItemProcessingWorker`
- Room v2 tables: `saved_items`, `saved_items_fts` (FTS + BM25), `entities`, `tags`, `saved_item_tags`, `collections`, `saved_item_collections`, `search_history`
- Screens: Home, Search, Collections, ItemDetail, Favorites, Trash, Settings + a design-token file

**B. Dead weight to delete** — an abandoned Expo/React Native port: `App.tsx`, `app.json`, `babel.config.js`, `package.json`, `package-lock.json`, `tsconfig.json`, `node_modules/`, `src/` (TSX screens/components). None of it is used by the Android build.

**Known gaps in (A) versus this brief — these define the work:**
- `saved_items.commentsJson` is a JSON blob. There is **no real post/comment tree**, no nesting, no per-comment score/author/ordering, no way to query or rank comments. This is the single biggest architectural gap.
- **No source/derived separation.** OCR text, extracted text and original text live in the same row as the source. Trust boundary is missing.
- **One media file per item** (`localFilePath` + `thumbnailPath`). Cannot represent a Reddit gallery, a multi-image share, or post media + thumbnail together.
- **No pluggable extractor framework.** Only generic OG scraping. No Reddit, YouTube, X, Instagram, or article-readability extraction.
- **No AI layer**, no embeddings, no semantic search, no "Ask Tuck".
- **No Android surfaces beyond the share sheet**: no share shortcuts (Direct Share), no widget, no Quick Settings tile, no app shortcuts, no clipboard save.
- **Missing organisation basics**: pinning, bulk selection, bulk move, list/grid toggle, editable titles everywhere, export/import.
- **No sync/backup.**
- **Release build is signed with the debug signing config** and `isMinifyEnabled = false` — must be fixed before any release milestone.
- **No instrumentation tests, no Room migration tests, no macrobenchmark.**

**Milestone 0 is: audit, clean, and stabilise this — not rewrite it.** Preserve working code. Only rewrite where §4/§5 explicitly require it.

---

## 4. Target architecture

Keep Kotlin + Compose + Room + Hilt + WorkManager. Do **not** migrate to React Native, KMP, or anything else.

### 4.1 Module layout (migrate incrementally, starting at M2 — do not do a big-bang split)

```
:app                     ← Application, MainActivity, nav host, DI wiring
:core:model              ← pure Kotlin domain models, no Android deps
:core:common             ← Result/Either, dispatchers, time, hashing, url utils
:core:designsystem       ← tokens, theme, typography, reusable Compose components
:core:database           ← Room entities, DAOs, migrations, FTS, converters
:core:datastore          ← preferences / settings
:core:media              ← file storage, thumbnails, PdfRenderer, ExoPlayer wrapper
:data:capture            ← intent parsing, ingestion, dedupe
:data:extractors         ← SourceExtractor registry + per-source implementations
:data:ai                 ← AiProvider interface + NoOp/OnDevice/Cloud impls
:data:search             ← query DSL parser, FTS repo, vector repo, hybrid ranker
:feature:inbox :feature:collections :feature:detail :feature:search :feature:settings :feature:capture-ui
:benchmark               ← macrobenchmark (startup, scroll, share-to-save latency)
```

**Rules:** `:feature:*` never depends on another `:feature:*`. `:data:*` never imports Compose. `:core:model` has zero Android imports. All cross-layer contracts are interfaces in `:core:model` or the owning `:data` module.

### 4.2 Patterns

- MVVM + unidirectional data flow. ViewModel exposes a single immutable `UiState` via `StateFlow`; UI sends `Event` objects up. No `LiveData`, no mutable state escaping the VM.
- Repository returns `Flow<T>` from Room; one-shot ops are `suspend` returning a sealed `TuckResult`.
- All I/O on injected dispatchers (`@IoDispatcher`), never `Dispatchers.IO` inline — makes tests deterministic.
- Compose: stateless composables + `@Preview` for every non-trivial one; hoisted state; `key`-stable lazy lists; no `remember { mutableStateOf }` holding business state.
- Errors are values, not exceptions, across module boundaries.

---

## 5. Data model v3 — the source/derived split (the central refactor)

This replaces Room schema v2. **Write a real `Migration(2, 3)`** that preserves every existing row — no destructive fallback, ever. Add a `MigrationTest` that builds a v2 DB from the checked-in `schemas/…/2.json`, migrates, and asserts row counts and field mapping.

### 5.1 The trust boundary

| SOURCE (immutable, captured, never overwritten) | DERIVED (regenerable, labelled, deletable) |
|---|---|
| original post/article/text, comment tree, author, timestamp, score, URL, media bytes, mime, the raw share payload | summary, key points, important comments, tags, categories, entities, OCR text, embeddings, classification, dedupe grouping |

Every derived row carries `producer` (e.g. `mlkit-ocr:16.0`, `gemini-2.x`, `rules:v3`), `produced_at`, and `model_version`. "Regenerate derived data" must be a working Settings action that wipes and rebuilds all derived tables from source, and it must be idempotent.

### 5.2 Tables

**Core**
- `items` — `id` (UUID string), `kind` (LINK|ARTICLE|SOCIAL_POST|VIDEO|IMAGE|SCREENSHOT|PDF|DOCUMENT|NOTE|AUDIO|LOCATION|CONTACT|FILE), `source_type` (REDDIT|YOUTUBE|X|INSTAGRAM|WEB|GALLERY|FILES|WHATSAPP|CLIPBOARD|MANUAL|UNKNOWN), `title` (user-editable), `title_is_user_edited` BOOL, `origin_url`, `canonical_url`, `source_domain`, `source_app_package`, `captured_at`, `created_at`, `updated_at`, `last_opened_at`, `open_count`, `is_pinned`, `is_favorite`, `is_archived`, `deleted_at` NULLABLE, `processing_state` (SAVED|ENRICHING|PARTIAL|COMPLETE|FAILED), `capture_note` ("why did I save this?"), `user_note`, `dedupe_group_id` NULLABLE, `content_hash`
- `item_raw_payload` — the exact intent extras as received (`action`, `mime`, `text`, `uris`, `referrer_package`, `received_at`). Append-only forensic record; lets you replay a failed ingest after fixing a parser.
- `media_assets` — `id`, `item_id`, `role` (PRIMARY|THUMBNAIL|GALLERY|ATTACHMENT|POSTER|AUDIO), `local_path`, `mime`, `bytes`, `width`, `height`, `duration_ms`, `sha256`, `ordinal`, `download_state`. **One item → many assets.**
- `collections` — `id`, `name`, `icon`, `color`, `parent_id` NULLABLE (nesting arrives in M9; the column exists from M2), `is_smart` BOOL, `smart_query` NULLABLE, `sort_ordinal`, `is_locked`, `created_at`
- `item_collections` — `item_id`, `collection_id`, `added_at`, `ordinal`. Composite PK. Many-to-many. **An item can be in many collections.**

**Source content**
- `source_posts` — `item_id` PK, `platform`, `platform_post_id`, `community` (subreddit/channel/handle), `author_handle`, `author_display`, `title`, `body_text`, `body_html`, `score`, `comment_count`, `posted_at`, `permalink`, `is_nsfw`, `raw_json` (the untouched API/parse response), `extractor_version`, `fetched_at`
- `source_comments` — `id`, `item_id`, `platform_comment_id`, `parent_comment_id` NULLABLE, `depth`, `path` (materialised path like `0003.0001.0007` for cheap ordered tree queries), `author_handle`, `body_text`, `body_html`, `score`, `posted_at`, `is_op`, `is_stickied`, `child_count`, `ordinal`
- `source_article` — `item_id` PK, `readable_html`, `readable_text`, `byline`, `published_at`, `site_name`, `word_count`, `lead_image_url`, `excerpt`
- `source_text` — `item_id` PK, `text`, `origin` (SHARED_TEXT|CLIPBOARD|TYPED_NOTE|PROCESS_TEXT)

**Derived**
- `derived_summaries` — `item_id`, `kind` (TLDR|ABSTRACT|WHY_SAVED), `text`, `producer`, `model_version`, `produced_at`, `confidence`
- `derived_points` — `id`, `item_id`, `kind` (KEY_POINT|PRO|CON|RECOMMENDATION|WARNING|ACTION), `text`, `ordinal`, `evidence_ref` (nullable FK → `source_comments.id` or a char offset range into source text), `producer`, `produced_at`. **`evidence_ref` is mandatory-by-convention: every generated point must be traceable to source.**
- `derived_comment_signals` — `comment_id`, `item_id`, `signal` (IMPORTANT|CONSENSUS|DISSENT|EXPERT|ANSWER), `score` FLOAT, `rationale`, `producer`
- `derived_tags` — `item_id`, `tag_id`, `source` (AI|RULE|USER), `confidence`
- `tags` — `id`, `name` (unique, normalised lowercase), `display_name`
- `derived_entities` — `id`, `item_id`, `type` (PERSON|ORG|PRODUCT|PRICE|DATE|PLACE|URL|EMAIL|PHONE|HASHTAG|MODEL_NUMBER), `value`, `normalized_value`, `char_start`, `char_end`, `producer`
- `ocr_blocks` — `id`, `asset_id`, `text`, `confidence`, `bbox_x/y/w/h`, `block_index`, `producer` (keeps spatial data for later features; the flattened text also feeds FTS)
- `embeddings` — `id`, `owner_type` (ITEM|COMMENT|CHUNK), `owner_id`, `chunk_index`, `chunk_text`, `vector` BLOB (float32 array), `dim`, `model`, `produced_at`

**Search & system**
- `items_fts` — FTS5 **external-content** table over a materialised `search_documents` view/table, columns: `title`, `body`, `ocr_text`, `comments_text`, `author`, `domain`, `tags`, `entities`, `notes`. Use `porter unicode61` tokenizer, `prefix='2 3 4'`, BM25 with column weights (title 8, tags 5, notes 4, body 3, comments 2, ocr 2, domain 1). Keep it in sync with triggers, and add a `rebuildFtsIndex()` maintenance op.
- `processing_jobs` — `id`, `item_id`, `stage`, `state`, `attempt`, `last_error`, `scheduled_at`, `completed_at`. Drives the visible "processing" chip and Settings → Processing queue.
- `search_history`, `saved_searches`
- `resurfacing_events` — `id`, `item_id`, `reason`, `shown_at`, `action` (OPENED|DISMISSED|SNOOZED)

### 5.3 Migration mapping from v2

`saved_items` → `items` + (`source_text` | `source_article`) + `media_assets` (from `localFilePath`/`thumbnailPath`) + `ocr_blocks` (single block from `ocrText`) + `derived_entities` (from `entities`) + `derived_tags`. `commentsJson` must be **parsed into `source_posts`/`source_comments` where possible**, and the untouched JSON preserved in `source_posts.raw_json` regardless. Nothing is dropped.

---

## 6. Capture layer — the #1 feature

### 6.1 Accepted inputs (all must land as a valid item)

Text, URLs, HTML, `text/uri-list`, images (incl. GIF/WebP/HEIC), videos, audio/voice notes, PDFs, documents (docx/xlsx/pptx/epub/txt/md/csv), arbitrary `*/*` files, multi-item shares, `geo:` locations and map URLs, `text/x-vcard` contacts, and copied text via `ACTION_PROCESS_TEXT`.

### 6.2 Intent surface (AndroidManifest)

- `ACTION_SEND` + `ACTION_SEND_MULTIPLE` for the mime list above (already present — keep, and add `text/x-vcard`, `application/epub+zip`, explicit `text/markdown`).
- `ACTION_PROCESS_TEXT` (`android:label="Save to Tuck"`) so text selection anywhere gets a Tuck action.
- `ACTION_VIEW` deep links for `tuck://item/{id}` and `tuck://collection/{id}`.
- **Sharing Shortcuts / Direct Share** (`res/xml/shortcuts.xml` with `<share-target>` + `ShortcutManagerCompat` long-lived dynamic shortcuts): publish the user's top 4 collections so the Android share sheet shows **"Tuck → Research"** as a one-tap row. This is a headline capability — most competitors don't do it.
- App shortcuts (long-press launcher): *New note*, *Save clipboard*, *Search*, *Inbox*.

### 6.3 The share flow (hard requirements)

1. `ShareActivity` is translucent, `excludeFromRecents`, `singleTop`, and **must not** show a full screen. It shows a compact bottom sheet.
2. **Persist first, render second.** Order of operations: read extras → for every content URI immediately `contentResolver.openInputStream()` and **copy bytes into app-private storage** (shared URIs are revoked the moment the sheet closes — this is the #1 source of "my saved image is gone" bugs; also call `takePersistableUriPermission` where the flag is granted) → insert `items` + `item_raw_payload` + `media_assets` rows → enqueue `ItemProcessingWorker` → *then* update the UI.
3. The sheet shows within 400ms: thumbnail (or type glyph), auto title (best-effort, may be the bare URL), **Saved ✓**, and three optional affordances: `Add to collection` (recent 4 + search), `Add note ("why am I saving this?")`, `Undo`. Auto-dismiss after 2.5s of inactivity; **dismissal never cancels the save**.
4. Large files: copy on a background coroutine with a progress row; the item exists in the DB before the copy finishes, with `media_assets.download_state = COPYING`.
5. Multi-share (`SEND_MULTIPLE`): default to **one item with N gallery assets** when all URIs are images from the same app within the same second; otherwise N items. Offer a toggle in the sheet: `Save as 1 item / Save as N items`.
6. Duplicate on capture: if `content_hash` or `canonical_url` matches an existing live item, still save, but link both via `dedupe_group_id` and show `Already saved 3 weeks ago — View / Keep both / Merge`.

### 6.4 Other capture surfaces

- **Quick Settings tile** (`TileService`): tap → save current clipboard; long-press → open Tuck. Offer `requestAddTileService` from onboarding on Android 13+.
- **Glance home-screen widget**, 3 sizes: `＋ Save something` button + last N saved thumbnails, tap-through to item. Resizable, theme-aware, updates via a `GlanceAppWidget` `updateAll` after each insert.
- **Clipboard save**: never read the clipboard in the background (Android shows a toast and it's a privacy violation) — only on explicit user action (tile, widget, shortcut, in-app FAB).
- **Screenshot intelligence** (M4): do **not** use a background clipboard/file watcher as the primary mechanism. Use: on app foreground, query `MediaStore.Images` for items in `Screenshots/` newer than `last_screenshot_scan_at` → show a dismissible banner *"7 new screenshots — review"* → user multi-selects → save + OCR. Optionally, an explicit opt-in foreground service using `ContentObserver` for near-instant "Save to Tuck?" notifications, clearly disclosed, off by default, with `READ_MEDIA_IMAGES` (and Android 14+ partial-access `READ_MEDIA_VISUAL_USER_SELECTED` handled gracefully).

---

## 7. Processing pipeline

```
SHARE → SAVE (DB commit, UI free) → enqueue → WorkManager chain
        ├─ Stage 1  normalize      strip utm_*/fbclid/gclid, resolve redirects & short links, canonicalise
        ├─ Stage 2  classify       kind + source_type (rules; deterministic, offline)
        ├─ Stage 3  extract        SourceExtractor for the detected platform (network)
        ├─ Stage 4  media          download remote media, thumbnails, PDF page 1, video poster
        ├─ Stage 5  ocr            ML Kit on every image/screenshot asset → ocr_blocks
        ├─ Stage 6  entities+tags  regex + rules
        ├─ Stage 7  index          rebuild the item's FTS row
        ├─ Stage 8  embed          (optional) chunk + embed for semantic search
        └─ Stage 9  ai             (optional, consented) summary, key points, important comments
```

Rules: each stage is **idempotent** and independently retryable, records into `processing_jobs`, and updates `items.processing_state`. Network stages carry `NetworkType.CONNECTED` + exponential backoff; the AI stage additionally requires `BatteryNotLow` and (by default) unmetered network. A failed stage never fails the item — it marks `PARTIAL` and leaves a visible, tappable "Retry enrichment" affordance on the detail screen. Stages 1–7 must work fully offline-capable-by-design (3 and 4 defer until connectivity returns).

---

## 8. Source extractor framework

```kotlin
interface SourceExtractor {
  val id: String                    // "reddit", "youtube", "article", ...
  val version: Int
  fun canHandle(url: HttpUrl, mime: String?): Boolean
  suspend fun extract(ctx: ExtractionContext): ExtractionResult
}

sealed interface ExtractionResult {
  data class Success(val post: SourcePost?, val comments: List<SourceComment>,
                     val article: SourceArticle?, val media: List<RemoteMedia>,
                     val rawResponse: String, val confidence: Float) : ExtractionResult
  data class Partial(val partial: Success, val missing: List<String>, val reason: String) : ExtractionResult
  data class Unsupported(val reason: String) : ExtractionResult
  data class Failed(val reason: String, val retryable: Boolean) : ExtractionResult
}
```

Registry picks the first `canHandle` match; **`GenericWebExtractor` is always the fallback and must never fail** (OpenGraph/Twitter cards/JSON-LD via jsoup + a readability implementation for article text). Every extractor: declares a realistic User-Agent, respects a per-host rate limiter, caps response size and comment count, has a hard timeout, and ships with golden fixtures.

**Per-source targets:**
- **Reddit** — resolve `redd.it`/`/s/` short links, then use the public JSON endpoint (`<permalink>.json?limit=…&depth=…`) to get post + nested comments. Map `kind: "more"` nodes into a `has_more` marker with a *Load more comments* action rather than silently truncating. Store subreddit, author, score, flair, awards count, created_utc, gallery/media metadata, crossposts. Persist the full comment tree with `depth` and materialised `path`. Cap at a configurable 500 comments / depth 8 initially, expandable on demand. Handle 403/429/quarantined/deleted gracefully → `Partial`.
- **YouTube** — oEmbed + page metadata: title, channel, thumbnail (max-res), duration, published date, description. Transcript/captions are **best-effort only**; if unavailable, say so in the UI, never fake it.
- **Article/Web** — readability extraction → `source_article.readable_text` + `readable_html`, byline, published date, lead image, word count, reading time. This is the highest-volume path; make it excellent.
- **X/Twitter, Instagram, Threads, LinkedIn** — no reliable free API. Be honest: attempt oEmbed/OG, otherwise capture the shared text + URL + any thumbnail we can legitimately fetch, mark `Partial` with `missing = [thread, comments]`, and offer *"Add content manually"* (paste text / attach screenshot) so the user can complete the archive themselves. **Never scrape behind a login and never ask the user for third-party credentials.**
- **Files/PDF/Doc** — page-1 thumbnail via `PdfRenderer`, text via extraction, OCR fallback for scanned pages, page count, title from metadata.

Legal/ethical line to hold: public content only, no auth bypass, no credential collection, respect `robots.txt` for the generic crawler, and keep everything on the user's device.

---

## 9. AI layer

```kotlin
interface AiProvider {
  val id: String; val runsOnDevice: Boolean; val requiresConsent: Boolean
  suspend fun summarize(input: AiInput): AiResult<Summary>
  suspend fun keyPoints(input: AiInput): AiResult<List<Point>>
  suspend fun rankComments(input: AiInput): AiResult<List<CommentSignal>>
  suspend fun classify(input: AiInput): AiResult<Classification>
  suspend fun embed(texts: List<String>): AiResult<List<FloatArray>>
  suspend fun answer(question: String, context: List<Passage>): AiResult<GroundedAnswer>
}
```

Implementations, in this order of preference at runtime: `NoOpAiProvider` (default; app fully functional) → `OnDeviceAiProvider` (ML Kit GenAI / Gemini Nano via AICore where the device supports it; on-device text embedder for semantic search) → `CloudAiProvider` (Gemini API, **user supplies their own API key in Settings** for v1 — no Tuck-operated backend, no key shipped in the APK, key stored in `EncryptedSharedPreferences`).

Hard rules:
- Every AI call is a **background job on an already-saved item**. Nothing in the save path awaits a model.
- All model output is parsed into a **strict JSON schema**; invalid output is discarded and retried once, then the stage is marked failed. Never store free-form model prose into a source field.
- Every generated point carries an `evidence_ref` to the exact comment or text span. The UI renders it as a tappable citation that scrolls to the original.
- **Grounding rule for "Ask Tuck":** answer only from retrieved passages of the user's own saved items; every claim gets a citation chip linking to the item; if retrieval is empty, say *"I couldn't find that in your saves"* — never answer from general knowledge.
- Settings must show: which provider is active, what data would be sent, a per-collection AI toggle, an estimated token/cost counter for the cloud provider, and a *Delete all AI-derived data* button.
- Prompts live in versioned files under `:data:ai/src/main/assets/prompts/` with the version recorded in `producer`, so regeneration is reproducible.

**Reddit key-points prompt shape (the flagship):** input = post title/body + top-N comments with scores and depth; output JSON = `{ summary, key_points[{text, evidence_comment_ids[]}], consensus[{claim, supporting_count, comment_ids[]}], disagreements[], notable_comments[{comment_id, why}] }`. Render as:

```
KEY TAKEAWAYS
• Battery life is the most common complaint        ⟵ 14 comments
• Most users prefer Model A                        ⟵ 9 comments
• Model B is considered better for gaming          ⟵ 6 comments
```

---

## 10. Search — the retrieval engine

Search is the product. Build it as a **hybrid** of four retrievers fused with Reciprocal Rank Fusion:

1. **Lexical** — FTS5 + BM25 over title/body/comments/OCR/tags/entities/notes with the column weights in §5.2.
2. **Structured filters** via a query DSL parsed *from the same input box*: `source:reddit`, `type:pdf`, `in:research`, `tag:nike`, `is:pinned`, `is:favorite`, `has:comments`, `domain:amazon.in`, `before:2026-03`, `after:last-month`, `during:2025`. Unrecognised tokens fall through to free text. Show parsed filters as removable chips above the results.
3. **Semantic** — cosine similarity over `embeddings`. Brute-force scan is fine to ~50k vectors if you (a) store float32 in a single contiguous `ByteBuffer` cache, (b) pre-normalise vectors so it's a dot product, (c) run on a background dispatcher. Only consider an ANN index (ObjectBox HNSW / sqlite-vec) if a benchmark proves >120ms p95 at realistic scale — put the numbers in `DECISIONS.md`.
4. **Recency/affinity boost** — small multiplier on recently opened, pinned, and frequently accessed items.

Also required: instant-as-you-type results with 120ms debounce, snippet highlighting (`snippet()`/`highlight()` from FTS5) showing *why* an item matched ("matched in OCR text", "matched in a comment"), zero-result recovery ("no exact matches — 6 similar items"), saved searches → smart collections, and a search-history/recent-queries row.

**Ask Tuck** (M7) sits above the same retrieval stack: retrieve top-k passages → ground the answer → cite items. Example target query: *"What were the three best Android phones I saved under ₹40,000?"* — this must work by retrieving the actual saved items and their extracted prices/entities, not by the model's world knowledge.

---

## 11. Memory / resurfacing (M8)

- **"You saved this before"** — on capture, if the new item is semantically near an existing one, surface it in the share sheet.
- **Weekly memory** — a local notification (opt-in, user-set day/time) with 3–5 items: an old save never re-opened, an item matching a current research thread, an item with a `capture_note` that reads like an intent ("compare these before buying").
- **Related items** rail on the detail screen — embedding neighbours + shared tags/entities/domain.
- **Forgotten saves** view — saved >30 days ago, `open_count = 0`.
- **Intent recall** — surface the user's own `capture_note` prominently when the item resurfaces: *"You saved this 4 months ago: 'Compare these laptops before buying.'"*
- All of it is computed locally, is dismissible, and has a global off switch. No dark patterns, no streaks, no engagement bait.

---

## 12. Screens

**Bottom nav: Home · Inbox · Collections · Search.** Global FAB `＋` (note / clipboard / camera / file).

**Home** — greeting, prominent search field (tapping it opens Search, doesn't inline-expand), `＋ Save`, *Recently saved* horizontal rail of rich visual cards, *Collections* grid (cover mosaic of the 4 most recent items + count), *Continue where you left off*, and an optional *Memory* card.

**Inbox** — everything unfiled, newest first, grouped by day (`Today / Yesterday / This week / Earlier`). This is the "save first, organise later" release valve. Fast affordances per row: file into collection, pin, delete. Multi-select via long-press → bulk move / tag / delete / share / export. A visible count badge on the nav item.

**Collections** — visual boards with cover mosaics, drag-reorder, pin to top, smart collections rendered with a distinct badge, and a "+ New collection" tile. Inside a collection: grid/list toggle (persisted per collection), sort (recent / oldest / title / manual), filter chips by type.

**Search** — see §10. Empty state shows recent searches + example queries that teach the DSL.

**Item detail — the best screen in the app.** Sectioned, scrollable, and *ordered by usefulness*:

```
┌─────────────────────────────────────────┐
│ ‹  Reddit · r/MachineLearning       ⋮   │
│                                          │
│ How should I learn GNNs?          [edit] │
│ u/someone · 2.3k ↑ · 127 comments        │
│ saved 12 Aug · [Research] [ML]           │
│──────────────────────────────────────────│
│ WHY I SAVED THIS                         │
│ "Start here after the exams"             │
│──────────────────────────────────────────│
│ KEY POINTS               ✦ AI · regenerate│
│ • Start with GCN                    ⟵ 8  │
│ • Learn message passing             ⟵ 5  │
│ • Implement PyG examples            ⟵ 4  │
│   (tap a point → jumps to the comments)  │
│──────────────────────────────────────────│
│ ORIGINAL POST            [Open original] │
│ full body text + media, offline           │
│──────────────────────────────────────────│
│ COMMENTS (127)   ⭐ Important | All | Top │
│ threaded, collapsible, depth guides       │
│──────────────────────────────────────────│
│ MY NOTES  (markdown, autosaved)           │
│ COLLECTIONS  [Research ×] [ML ×] [+]      │
│ DETAILS  source · captured · size · hash  │
└─────────────────────────────────────────┘
```

Every AI section is visually distinguished (a `✦` badge + subtle tint) and collapsible; source sections never are. Overflow menu: share, open original, copy link, export (MD/HTML/JSON), regenerate derived, add to collection, pin, archive, delete.

**Share sheet** — §6.3. **Settings** — appearance, storage usage + cleanup, AI provider & consent, screenshot import, resurfacing, export/import, app lock (BiometricPrompt), processing queue, about.

---

## 13. Design system

Material 3 (Expressive where available) with **dynamic color** support plus a signature Tuck palette fallback; full light/dark/system with a true-dark option. Reuse and extend the existing `TuckDesignTokens` — do not introduce a second token system. Requirements: 8dp spacing scale; a type ramp with a distinctive display face for headers; content-forward cards where the *content* is the hero (thumbnail-dominant, chrome-light); source-badge glyphs per platform; shared-element transitions from card → detail; skeleton loaders (never spinners) for enriching items; and delightful, specific empty states.

Accessibility is not optional: min 48dp touch targets, `contentDescription` on every meaningful element, TalkBack-navigable comment trees with correct headings, dynamic type up to 200% without clipping, and contrast ≥ 4.5:1 verified.

---

## 14. Privacy, security, performance

**Privacy** — no account required, ever; zero analytics by default (if any telemetry is added later it must be opt-in and locally inspectable); all media in app-private storage; no third-party SDK receives user content; `android:allowBackup` reviewed deliberately (currently `true` — decide and document); network access limited to fetching content the user explicitly saved.

**Security** — app lock via `BiometricPrompt` with a per-collection lock option; API keys in `EncryptedSharedPreferences`; `FileProvider` for all outbound file sharing (never `file://`); optional SQLCipher for the DB (M10, behind a decision entry); `usesCleartextTraffic=false`; strict `network_security_config`.

**Performance budgets** (enforce with the `:benchmark` module; a regression is a release blocker):
| Metric | Budget |
|---|---|
| Share intent → row committed | < 250ms p95 |
| Share intent → "Saved ✓" visible | < 400ms p95 |
| Cold start to first frame | < 1.2s p95 (baseline profile required) |
| List scroll | zero dropped frames at 120Hz on a mid-range device |
| Search keystroke → results | < 120ms p95 at 10k items |
| Semantic search | < 300ms p95 at 10k items |
| APK size | < 25MB base |

Test with a **seeded 10,000-item database** (write a debug-only seeder). Most bookmark apps fall apart at scale; Tuck must not.

**Release hygiene** — fix the release build: real signing config from `keystore.properties` (git-ignored), `isMinifyEnabled = true` with tested R8 rules, `lint.abortOnError = true` before v1, baseline profile, and a `debug`/`release` applicationIdSuffix split so both can be installed side by side.

---

## 15. Testing & verification

- **Unit** — extractors against golden fixtures (offline), URL canonicalisation, query-DSL parser, dedupe, ranking/RRF fusion, entity extraction, comment-tree flattening/`path` generation.
- **Room** — a migration test for *every* version step, plus a v2→v3 data-preservation test.
- **Instrumentation** — share-intent tests for each mime type asserting a row exists within 400ms; permission-denied paths; the URI-revocation case (share, close the sheet, assert bytes are still readable).
- **Compose UI** — detail-screen section ordering, comment-tree expansion, bulk-select, search filter chips.
- **Macrobenchmark** — startup, scroll, share-to-save.

Manual verification via adb, to be run at the end of every capture-related milestone:

```bash
adb shell am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.TEXT "https://www.reddit.com/r/MachineLearning/comments/xxxxx/" -n com.tuck.app/.ui.share.ShareActivity
```

```bash
adb shell am start -a android.intent.action.SEND -t image/jpeg --eu android.intent.extra.STREAM file:///sdcard/Pictures/Screenshots/test.png -n com.tuck.app/.ui.share.ShareActivity
```

Also verify from real apps (Chrome, YouTube, Reddit, Gallery, Files, WhatsApp) on the emulator — synthetic intents hide real-world URI permission bugs.

---

## 16. Milestones

Each milestone ends with: build green · tests green · installed on emulator · demo script executed · screenshots attached · `PROGRESS.md` and `DECISIONS.md` updated. **Do not proceed without explicit approval.**

### M0 — Audit & cleanup *(no new features)*
- [ ] Delete the abandoned Expo/React Native project: `App.tsx`, `app.json`, `babel.config.js`, `package.json`, `package-lock.json`, `tsconfig.json`, `node_modules/`, `src/`. Confirm nothing in the Gradle build references them.
- [ ] Add `.gitignore` (build dirs, `local.properties`, `keystore.properties`, `.gradle`, `.kotlin`, `*.jks`). Initialise git if absent and make an initial commit — the repo is currently untracked, which is unacceptable before a refactor of this size.
- [ ] Produce `AUDIT.md`: every existing file, what it does, keep / refactor / replace, and why.
- [ ] Get `./gradlew assembleDebug` + `testDebugUnitTest` green; fix or document every warning.
- [ ] Rewrite `README.md` to describe the actual current state (the existing one overstates what is built).
- **Demo:** app installs, share a URL from Chrome, item appears. Baseline screenshots of every existing screen.

### M1 — Capture, hardened
- [ ] Bulletproof the share path: copy-bytes-before-anything, `takePersistableUriPermission`, large-file streaming, multi-share, `ACTION_PROCESS_TEXT`, vCard, `geo:`, audio.
- [ ] `item_raw_payload` recording + replay-a-failed-ingest debug tool.
- [ ] Instant HUD with add-to-collection / add-note / undo; auto-dismiss never cancels the save.
- [ ] Share targets (Direct Share) for top 4 collections + launcher app shortcuts.
- [ ] Instrumentation tests per mime type asserting the 400ms budget.
- **Demo:** save from Chrome, YouTube, Reddit, Gallery (multi-select 5 images), Files (PDF), WhatsApp, and a text selection — all seven land correctly, offline too.

### M2 — Schema v3 + organisation
- [ ] Migration 2→3 implementing §5 with a passing data-preservation test.
- [ ] Begin the module split (`:core:model`, `:core:database`, `:core:designsystem` first).
- [ ] Multi-asset media, multi-collection membership, pin, favorite, archive, rename, bulk select/move/tag/delete, grid⇄list toggle, sort options, trash with restore + explicit empty.
- [ ] Debug seeder for 10,000 items; record baseline performance numbers.
- **Demo:** an item in three collections; bulk-move 20 items; smooth scrolling over 10k items.

### M3 — Source intelligence
- [ ] `SourceExtractor` registry + `GenericWebExtractor` (OG/JSON-LD/Twitter cards) + readability article extraction, with fixtures.
- [ ] Canonicalisation, redirect/short-link resolution, tracking-param stripping, favicon + lead image caching.
- [ ] Processing queue UI, `PARTIAL` state, retry affordance.
- **Demo:** save 10 varied URLs offline, go online, watch all enrich; kill the app mid-enrichment and confirm it resumes.

### M4 — Screenshot intelligence
- [ ] MediaStore-based new-screenshot review banner + multi-select import; optional disclosed foreground observer.
- [ ] OCR into `ocr_blocks`, auto-title from OCR, rule-based tags/category, price/product entity extraction, perceptual-hash duplicate detection with a merge UI.
- **Demo:** screenshot an Amazon product page in the emulator → Tuck offers it → saved with title, price entity, and `shopping` tag; save the same screenshot twice → duplicate detected.

### M5 — Search
- [ ] FTS5 external-content rebuild with weighted BM25 + triggers + `rebuildFtsIndex()`.
- [ ] Query DSL parser + filter chips + snippet highlighting with match-reason labels.
- [ ] Saved searches → smart collections; search history; zero-result recovery.
- **Demo:** find a screenshot by a price that only exists in its OCR text; `source:reddit tag:ml after:last-month` returns the right set in <120ms over 10k items.

### M6 — Platform extractors (the differentiator)
- [ ] Reddit: post + full nested comment tree + media + `more` handling, stored in `source_posts`/`source_comments`.
- [ ] Threaded comment UI: collapse/expand, depth guides, sort (top/new/important), *Load more*, offline-complete.
- [ ] YouTube, and honest `Partial` handling with manual completion for X/Instagram/LinkedIn.
- **Demo:** share a 300-comment Reddit thread → full tree readable in airplane mode.

### M7 — AI layer
- [ ] `AiProvider` interface + NoOp default + on-device + BYO-key Gemini cloud, with the consent screen.
- [ ] Summaries, key points with `evidence_ref` citations, important-comment ranking, auto-tags/categories — all background, all optional, all regenerable and deletable.
- [ ] Embeddings + semantic search fused into §10; **Ask Tuck** with mandatory citations.
- **Demo:** the Reddit thread from M6 gets key points that each cite a real comment; turn AI off in Settings and confirm every part of the app still works.

### M8 — Memory
- [ ] Related items, "you saved this before" at capture, weekly memory notification, forgotten saves, `capture_note` recall, AI-suggested collections (suggest only — never auto-file without consent).
- **Demo:** save a Japan travel article, then months-old related saves surface with your original note.

### M9 — Sync, backup, sharing
- [ ] Full local export/import (JSON manifest + media, restores byte-identical), scheduled local backup, optional Google Drive / user-chosen SAF folder.
- [ ] Optional account + cross-device sync with last-writer-wins per field plus a conflict inspector; nested collections; shared/collaborative collections; locked collections.
- **Demo:** export on device A, import on device B, everything intact including comment trees and media.

### M10 — Power tools & release
- [ ] Markdown / HTML / JSON export per item and per collection; browser extension endpoint (deep link + local receiver); Glance widget (3 sizes); QS tile; app lock; automation intents (Tasker-compatible); optional DB encryption.
- [ ] Release hygiene from §14, baseline profile, Play listing assets, crash-free verification.
- **Demo:** a signed release build, installed clean, passing the full performance budget table.

---

## 17. Explicitly do NOT do

- Do **not** require login, an account, or a Tuck-operated server for any v1 feature.
- Do **not** put AI in the save path or make any feature AI-only.
- Do **not** let derived data overwrite, replace, or hide source data.
- Do **not** design a folder-first UX, force single-parent hierarchy, or require a collection at save time.
- Do **not** scrape behind authentication, ask for third-party passwords, or bundle any API key in the APK.
- Do **not** read the clipboard in the background or watch the filesystem without disclosed, opt-in consent.
- Do **not** add analytics, ads, paywalls, or upsell surfaces. Monetisation is deliberately deferred: the goal is adoption → usage data → retention, then revenue.
- Do **not** rewrite working code for taste. Refactor only where this brief requires it.
- Do **not** use `fallbackToDestructiveMigration()`. Ever.
- Do **not** add a second design-token system, a second DI framework, or a second navigation library.
- Do **not** mark a checkbox done without running the demo.

---

## 18. Kickoff

Start with **M0 only**.

1. Read every existing Kotlin file and produce `AUDIT.md` (§16 M0).
2. Propose the deletion list for the Expo project and wait for approval.
3. Propose the exact schema v3 migration plan as an artifact — table by table, with the v2→v3 field mapping — before writing any migration code.
4. Then stop and present the plan.

Ask me now about anything in this document that is ambiguous or that conflicts with what you find in the code. Do not begin coding until the M0 plan is approved.
