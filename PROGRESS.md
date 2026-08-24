# Tuck Build Progress & Milestone Tracker

Living tracker of milestone completion.

**A box is `[x]` only when the claim was verified by running something** — a build, a test, or the
app on a device. Not when the code merely exists, and not when a class exists but nothing calls it.

**Status legend**

| Mark | Meaning |
|---|---|
| `[x]` | Verified. Evidence noted inline. |
| `[~]` | Partially implemented. The gap is stated. |
| `[!]` | Code exists but is **not wired** — nothing references it, so it does not run. |
| `[ ]` | Not implemented. |
| `[?]` | Cannot be verified — the test infrastructure it would need does not exist. |

**Rules for editing this file**

1. Never delete an unticked line. A line is removed only when the feature is **cut**, and then it
   stays with a `~~strikethrough~~` and the reason. Deleting an open item is not completing it.
2. Never narrow a claim to make it tickable. If the milestone said "R8 obfuscation", `[x] ProGuard
   rules file exists` is not that claim — R8 does not run while `isMinifyEnabled = false`.
3. A class that nothing calls gets `[!]`, never `[x]`.
4. Do not tick a performance or stability claim without a benchmark or test to point at.

---

## Verification baseline — 2026-08-24 (second audit)

Re-audited the working tree after the extractors/widget/security pass. What was actually run:

- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**
- **57 unit tests, 0 failures, 0 skipped** (up from 52)
- No emulator run, no instrumentation tests — **`app/src/androidTest/` still does not exist**

### Corrections applied in this audit

| Claimed | Reality |
|---|---|
| `[x]` Weekly memory notification | `MemoryNotificationWorker` is **never enqueued** and the manifest has no `POST_NOTIFICATIONS`. Dead code. → `[!]` |
| `[x]` ProGuard rules configured | Rules file exists but `isMinifyEnabled = false` on both build types, so R8 never runs and the rules are inert. → `[ ]` R8 obfuscation |
| `[x]` Platform extractors (M3) | `SourceExtractorRegistry` is not referenced outside its own package; the pipeline still uses `UrlMetadataProcessor`. → `[!]` |
| Release signing, baseline profile, Markdown/HTML export | These lines were **deleted** from M10 rather than completed. Restored below, unticked. |
| FTS5 / BM25 (README, `SearchRepositoryImpl.name`) | It is Room `@Fts4` with plain `MATCH`. README corrected; the misleading `"FTS5_Keyword_Engine"` string renamed. `DECISIONS.md` was already corrected. |

### Fixed since the first audit

- `2.json` restored to its shipped state — verified `git diff 7cf7a14 -- 2.json` is empty
- Comment tree is now read from `source_comments`, with the legacy blob kept as a pre-migration fallback
- `DECISIONS.md` FTS entry rewritten honestly (`@Fts4`, FTS5 listed as the rejected alternative)

---

## Milestone 0: Audit, Cleanup & Stabilization — **complete**
- [x] Read every Kotlin file end to end; produce `AUDIT.md`
- [x] Abandoned Expo/RN prototype removed
- [x] Schema v3 migration plan artifact written
- [x] `.gitignore` added, repository initialized with a baseline commit
- [x] `./gradlew assembleDebug` passes — re-verified 2026-08-24
- [x] `./gradlew testDebugUnitTest` passes, 57/57 — re-verified 2026-08-24
- [ ] Emulator baseline run — `walkthrough.md` was never created
- [x] Living files initialized: `PROGRESS.md`, `DECISIONS.md`

---

## Milestone 1: Sacred Ingestion Engine
- [~] Non-blocking share target for single/multi-stream Uris — implemented; **the <400ms budget has never been measured**
- [x] Immediate byte copying to app-private storage before the share sheet dismisses
- [x] Raw forensic intent payload captured into `item_raw_payload`
- [x] Background enrichment WorkManager pipeline with retry and backoff
- [x] Share HUD with 1-tap category assignment and auto-dismiss
- [x] `<share-target>` Direct Share target declared in `shortcuts.xml`
- [ ] Long-lived dynamic shortcuts pushed per collection, so real collections appear in the share sheet
- [?] Instrumentation tests asserting a row exists within 400ms per mime type

---

## Milestone 2: Schema v3 Room Migration
- [x] v3 entities: `item_raw_payload`, `media_assets`, `source_posts`, `source_comments`, `derived_summaries`, `derived_points`, `ocr_blocks`
- [x] `MIGRATION_2_3` implemented; no `fallbackToDestructiveMigration` in the tree
- [x] `commentsJson` migrated into `source_posts` + `source_comments` with materialized paths, and now read by the UI
- [x] Checked-in `2.json` restored to its shipped state
- [ ] FTS5 external-content table with triggers — **it is `@Fts4`, no triggers, no BM25.** Recorded as a deliberate decision in `DECISIONS.md`; left unticked because the milestone as written is not met
- [ ] Migration test proving zero data loss — the existing test asserts on mocked SQL strings, not on data
- [~] Source/derived split — new tables sit *alongside* `saved_items`, which still carries `originalText`/`extractedText`/`ocrText`/`commentsJson`

---

## Milestone 3: Enrichment Pipeline
- [x] ML Kit on-device OCR for images and screenshots
- [x] PDF text extraction and first-page thumbnail
- [x] Entity extraction (URLs, emails, phones, money, dates, hashtags)
- [x] Rule-based content classifier
- [x] Pluggable `AiProvider` abstraction, NoOp by default
- [x] Golden fixtures under `app/src/test/resources/fixtures/` (reddit, article, youtube)
- [!] `SourceExtractor` registry with Reddit/YouTube/Twitter/Generic handlers — **written and unit-tested, but nothing calls it.** `ItemProcessingWorker` still enriches via `UrlMetadataProcessor`
- [ ] Wire `SourceExtractorRegistry` into `ItemProcessingWorker`
- [ ] Instagram / LinkedIn / TikTok handlers

---

## Milestone 4: Jetpack Compose UI & Design System
- [x] 5 theme palettes (Linen, Noir, Forest, Cobalt, Plum)
- [x] 4-tab navigation (Home, Inbox, Collections, Search) + global FAB
- [x] Inbox release valve with swipe actions
- [x] Collections with multi-home item support
- [x] Rich content cards with hero media and platform badges

---

## Milestone 5: Detail Experience & Media Viewer
- [x] Detail view with live webview vs snapshot archive tabs
- [x] In-place media player
- [x] Entity action chips (dial, compose, convert)
- [x] Inline title editing and collection management dialog
- [x] Threaded comment tree read from `source_comments`, legacy blob as fallback
- [ ] Comment collapse/expand, depth guides, sort (top / new / important)

---

## Milestone 6: Search & Vault Backup
- [~] Keyword search with prefix matching — works, on **FTS4 with plain MATCH**
- [x] Search filter chips (type, date range, favorites, sorting)
- [x] Search history
- [x] Offline vault JSON backup export and import
- [x] Cache cleanup and local data management
- [ ] Weighted relevance ranking
- [ ] Query DSL (`source:reddit`, `in:research`, `after:last-month`)
- [ ] Semantic search / embeddings

---

## Milestone 7: Performance & Hardening — **not started**
- [?] Share-to-save perceived latency < 400ms — no benchmark exists
- [ ] LeakCanary validation — not a dependency
- [?] Integration test coverage — no `androidTest` source set
- [ ] 10,000-item seeded database and performance baseline
- [ ] Edge case validation (airplane mode, large PDFs, multi-image intents)
- [ ] Production release build — still debug-signed

---

## Milestone 8: Memory & Recall
- [x] Related items engine (entity/tag/domain weighted scoring)
- [x] Inline capture note and user note editing and recall
- [x] Forgotten saves query (`getForgottenSaves`, `openCount == 0`)
- [!] Weekly memory notification — `MemoryNotificationWorker` exists but is **never enqueued**; no `POST_NOTIFICATIONS` permission declared
- [ ] Schedule the worker and request the notification permission
- [ ] Forgotten saves surfaced in the UI
- [~] "You saved this before" at capture — `DuplicateDetector` exists; surfacing in the share HUD unconfirmed

---

## Milestone 9: Sync, Backup & Sharing
- [x] Local export/import (JSON manifest + media)
- [x] Scheduled local backup worker
- [x] Standalone `.tuck` collection pack export/import
- [x] Locked collections gated by `BiometricPrompt` — wired at `CollectionsScreen.kt:228`
- [ ] Byte-identical restore verified on a second device
- [ ] Nested collections (the `parentId` column exists; no UI)
- [ ] Cross-device sync

---

## Milestone 10: Power Tools & Release Hardening
- [x] Quick Settings tile for 1-tap capture (`TuckQuickTileService.kt`)
- [x] Home-screen AppWidget for quick capture — RemoteViews, not Glance; registered as a receiver
- [x] Static launcher shortcuts and a `<share-target>` declaration
- [ ] R8 / ProGuard obfuscation — rules exist but `isMinifyEnabled = false`, so R8 never runs
- [ ] Real release signing config — **currently the debug key**
- [ ] Baseline profile
- [ ] Markdown / HTML export

---

## Next up, in order

1. **Wire the two dead components.** `SourceExtractorRegistry` into `ItemProcessingWorker`, and
   `MemoryNotificationWorker` into a scheduler plus a `POST_NOTIFICATIONS` request. Both are written
   and tested; neither runs. This is the cheapest real progress available.
2. **Replace `RoomMigrationTest` with a real one** — add `app/src/androidTest/`, use
   `MigrationTestHelper` against the now-restored v2 schema, assert row counts and field mapping.
   This is the only thing standing between an existing user and data loss.
3. **Release hardening** — real signing config from a git-ignored `keystore.properties`, turn on
   `isMinifyEnabled`, test the R8 rules, add a baseline profile.
4. **Decide on FTS.** Either accept FTS4 and drop the BM25/relevance-ranking goals from M2/M6, or
   move to a raw FTS5 external-content table with triggers.
5. **Finish the source/derived split** — `saved_items` still carries the text columns the v3 tables
   were meant to own.
