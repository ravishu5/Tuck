# Tuck Build Progress & Milestone Tracker

Living tracker of milestone completion. Tasks are marked `[x]` only when all acceptance criteria (clean build, passing unit tests, and emulator verification) are verifiably satisfied.

---

## Milestone 0: Audit, Cleanup & Stabilization
- [x] Read every Kotlin file end to end; produce `AUDIT.md`
- [x] Propose exact deletion list for abandoned Expo/RN prototype and execute removal
- [x] Schema v3 migration plan artifact written and approved
- [x] Android `.gitignore` added and repository initialized with clean baseline commit
- [x] `./gradlew assembleDebug` passes cleanly with 0 errors
- [x] `./gradlew testDebugUnitTest` passes cleanly with all unit tests green
- [x] Emulator baseline run captured and documented in `walkthrough.md`
- [x] Living files initialized: `PROGRESS.md` and `DECISIONS.md`

---

## Milestone 1: Sacred Ingestion Engine (The Core Law)
- [ ] Non-blocking share target handles single/multi-stream Uris with zero UI jank (<400ms perceived)
- [ ] Immediate byte copying to app-private storage before dismissing share sheet
- [ ] Raw forensic intent payload captured into `item_raw_payload`
- [ ] Background enrichment WorkManager pipeline with retry and backoff
- [ ] Share HUD overlay with 1-tap category assignment and fast auto-dismiss

---

## Milestone 2: Schema v3 Room Migration with Materialized Path
- [ ] Schema v3 entity models (`items`, `item_raw_payload`, `media_assets`, `source_posts`, `source_comments`, `derived_entities`, `derived_tags`, `derived_summaries`, `ocr_blocks`)
- [ ] Strict `Migration(2, 3)` implemented without `.fallbackToDestructiveMigration()`
- [ ] Comment JSON blob migration into `source_posts` + `source_comments` with materialized path (`0001.0002`)
- [ ] FTS5 virtual table with SQLite triggers on `items` and source tables
- [ ] Automated Room migration tests verifying zero data loss on sample v2 database

---

## Milestone 3: On-Device & Pluggable Enrichment Pipeline
- [ ] ML Kit on-device Text Recognition (OCR) for images and screenshots
- [ ] PDF text extraction and first-page thumbnail generation
- [ ] Platform-specific extractors for Reddit, Instagram, YouTube, TikTok, LinkedIn
- [ ] Entity extraction (URLs, emails, phones, money, dates, hashtags)
- [ ] Rule-based content classifier for smart collections
- [ ] Pluggable AI provider abstraction (NoOp by default, OnDevice/Gemini Nano, BYO Cloud API key)

---

## Milestone 4: Jetpack Compose UI & Intentional Design System
- [ ] 5 Theme Palettes: Linen (Warm Paper), Noir (High Contrast Dark), Forest (Earth/Sage), Cobalt (Deep Navy), Plum (Muted Purple)
- [ ] 4-Tab Main Navigation: `Home`, `Inbox`, `Collections`, `Search` + Global FAB `+`
- [ ] Inbox release valve with swipe actions (Keep, Archive, Categorize, Trash)
- [ ] Collections hierarchy with multi-home item support
- [ ] Rich Content Cards with hero media previews, platform badges, snippet highlighting

---

## Milestone 5: Detail Experience & In-Place Media Viewer
- [ ] Detail view with live webview vs snapshot archive tabs
- [ ] In-place responsive media player for Instagram reels, YouTube embeds, Reddit discussions
- [ ] Interactive comment tree rendering via materialized paths
- [ ] Detected entity action chips (tap phone to dial, tap email to compose, tap money to convert)
- [ ] Title inline editing and collection management dialog

---

## Milestone 6: Hybrid Search, Vault Backup & Offline Privacy
- [ ] Fast FTS5 search with prefix matching (`term*`) and relevance ranking
- [ ] Search filter chips (content type, date range, favorites, sorting)
- [ ] Natural query prompts and search history
- [ ] Offline vault JSON backup export and import
- [ ] Cache cleanup and local data management

---

## Milestone 7: Final Polish, Performance Benchmarking & Hardening
- [ ] End-to-end performance verification: Share-to-save perceived latency < 400ms
- [ ] LeakCanary validation: 0 memory leaks across activity transitions
- [ ] Comprehensive unit and integration test coverage
- [ ] Edge case validation: Airplane mode saves, large PDFs, multi-image intents
- [ ] Final production build and release artifact verification
