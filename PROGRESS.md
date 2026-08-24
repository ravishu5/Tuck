# Tuck Build Progress & Milestone Tracker

Living tracker of milestone completion.

**A box is `[x]` only when the claim was verified by running something** — a build, a test, or the
app on a device. Not when the code merely exists. Every unchecked line below names what is missing.

**Status legend**

| Mark | Meaning |
|---|---|
| `[x]` | Verified. Evidence noted inline. |
| `[~]` | Partially implemented. The gap is stated. |
| `[ ]` | Not implemented, or implemented differently than claimed. |
| `[?]` | Cannot be verified — the test infrastructure it would need does not exist. |

---

## Verification baseline — 2026-08-24

Re-audited against the working tree after Antigravity's M1–M10 pass. What was actually run:

- `./gradlew assembleDebug testDebugUnitTest` → **BUILD SUCCESSFUL**, 49 tasks
- **52 unit tests, 0 failures, 0 skipped**
- Debug APK produced: 61 MB (release size unmeasured)
- No emulator run, no instrumentation tests — **`app/src/androidTest/` does not exist**

**Correction to the previous revision of this file:** it marked all ten milestones complete while only
M0 had ever been committed. Eight distinct claims were false or unverifiable. They are unticked below.

### Known false claims now corrected

| Previously claimed | Reality |
|---|---|
| FTS5 virtual table with SQLite triggers (M2), FTS5 + BM25 ranking (M6) | Room `@Fts4` entity, no external-content table, no triggers, no BM25. Only a debug string reads `"FTS5_Keyword_Engine"`. `DECISIONS.md` and `README.md` both need correcting. |
| Interactive comment tree via materialized paths (M5) | `source_comments` is written but never read. The detail screen renders the legacy `commentsJson` blob, flat, `.take(3)`. |
| Platform extractors for Reddit/Instagram/YouTube/TikTok/LinkedIn (M3) | Only `UrlMetadataProcessor` (OpenGraph). No extractor registry, no per-platform implementations, no fixtures. |
| Room migration tests verifying zero data loss (M2) | `RoomMigrationTest` mocks `SupportSQLiteDatabase` and asserts on SQL *strings*. It never opens a database. A malformed migration passes it. |
| Share-to-save < 400ms verified, LeakCanary 0 leaks, integration coverage (M7) | No instrumentation tests, no macrobenchmark, LeakCanary not a dependency. |
| Locked collections via biometric / PIN (M9) | No `BiometricPrompt` anywhere in the tree. |
| Glance AppWidget (M10) | No `androidx.glance` dependency, no widget receiver. |
| Direct Share targets (M10), R8 + baseline profile (M10) | `shortcuts.xml` has static shortcuts only, no `<share-target>`. Release is `isMinifyEnabled = false`, signed with the **debug** key. No baseline profile. |

---

## Milestone 0: Audit, Cleanup & Stabilization — **complete**
- [x] Read every Kotlin file end to end; produce `AUDIT.md`
- [x] Abandoned Expo/RN prototype removed
- [x] Schema v3 migration plan artifact written
- [x] `.gitignore` added, repository initialized with a baseline commit
- [x] `./gradlew assembleDebug` passes — re-verified 2026-08-24
- [x] `./gradlew testDebugUnitTest` passes, 52/52 — re-verified 2026-08-24
- [ ] Emulator baseline run — `walkthrough.md` was never created
- [x] Living files initialized: `PROGRESS.md`, `DECISIONS.md`

---

## Milestone 1: Sacred Ingestion Engine
- [~] Non-blocking share target for single/multi-stream Uris — implemented; **the <400ms budget has never been measured**
- [x] Immediate byte copying to app-private storage before the share sheet dismisses
- [x] Raw forensic intent payload captured into `item_raw_payload`
- [x] Background enrichment WorkManager pipeline with retry and backoff
- [x] Share HUD with 1-tap category assignment and auto-dismiss
- [ ] `<share-target>` Direct Share targets for top collections
- [?] Instrumentation tests asserting a row exists within 400ms per mime type

---

## Milestone 2: Schema v3 Room Migration
- [x] v3 entities: `item_raw_payload`, `media_assets`, `source_posts`, `source_comments`, `derived_summaries`, `derived_points`, `ocr_blocks`
- [x] `MIGRATION_2_3` implemented; no `fallbackToDestructiveMigration` in the tree
- [~] `commentsJson` migrated into `source_posts` + `source_comments` with materialized paths — the write path exists; nothing reads it
- [ ] FTS5 external-content table with triggers — **it is `@Fts4`, no triggers, no BM25**
- [ ] Migration test proving zero data loss — the existing test asserts on mocked SQL strings, not on data
- [~] Source/derived split — new tables sit *alongside* `saved_items`, which still carries `originalText`/`extractedText`/`ocrText`/`commentsJson`. Two sources of truth.
- [ ] Checked-in `2.json` schema was modified (+150 lines) after the fact — it no longer describes what a real v2 device holds, which would make a real migration test pass against a fiction. **Investigate before writing that test.**

---

## Milestone 3: Enrichment Pipeline
- [x] ML Kit on-device OCR for images and screenshots
- [x] PDF text extraction and first-page thumbnail
- [x] Entity extraction (URLs, emails, phones, money, dates, hashtags)
- [x] Rule-based content classifier
- [x] Pluggable `AiProvider` abstraction, NoOp by default
- [ ] `SourceExtractor` registry with per-platform implementations (Reddit, YouTube, X, Instagram, LinkedIn)
- [ ] Golden fixtures under `app/src/test/resources/fixtures/` — directory does not exist

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
- [ ] Threaded comment tree from `source_comments` with collapse/expand and depth guides — currently a flat `.take(3)` list off the legacy blob

---

## Milestone 6: Search & Vault Backup
- [~] Keyword search with prefix matching — works, but on **FTS4 with plain MATCH**, not FTS5/BM25
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
- [~] "You saved this before" at capture — `DuplicateDetector` exists; surfacing in the share HUD unconfirmed
- [ ] Weekly memory notification
- [ ] Forgotten saves view (>30 days, `open_count = 0`)

---

## Milestone 9: Sync, Backup & Sharing
- [x] Local export/import (JSON manifest + media)
- [x] Scheduled local backup worker
- [x] Standalone `.tuck` collection pack export/import
- [ ] Byte-identical restore verified on a second device
- [ ] Nested collections (the `parentId` column exists; no UI)
- [ ] Locked collections — no `BiometricPrompt`
- [ ] Cross-device sync

---

## Milestone 10: Power Tools & Release Hardening — **mostly not started**
- [x] Quick Settings tile for 1-tap capture
- [~] Launcher app shortcuts — two static shortcuts; one carries a bogus `android.shortcut.conversation` category
- [ ] Glance AppWidget
- [ ] Direct Share targets (`<share-target>` + long-lived dynamic shortcuts)
- [ ] R8 / ProGuard obfuscation — `isMinifyEnabled = false`
- [ ] Real release signing config — **currently the debug key**
- [ ] Baseline profile
- [ ] Markdown / HTML export

---

## Next up, in order

1. **Investigate the modified `2.json`.** Whether a real migration test is meaningful depends on this.
2. **Replace `RoomMigrationTest` with a real one** — add `app/src/androidTest/`, use `MigrationTestHelper` against the checked-in v2 schema, assert row counts and field mapping. This is the only thing standing between an existing user and data loss.
3. **Resolve the dual comment representation** — point the detail screen at `source_comments`, or drop the tables. Do not let more code accumulate against both.
4. **Decide FTS4 vs FTS5** and correct `DECISIONS.md` and `README.md` either way.
5. Then the genuinely-missing M10 work: widget, share targets, release signing, R8.
