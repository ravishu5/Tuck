# Architecture Decision Records (ADR) — Tuck

This document records the architectural and design decisions made throughout the evolution of the Tuck Android codebase.

---

## ADR 001: Abandoning React Native/Expo Prototype in Favor of Pure Kotlin/Compose
- **Date:** August 2026 (Milestone 0)
- **Status:** Accepted
- **Context:** The repository contained an abandoned React Native/Expo prototype alongside a native Jetpack Compose implementation.
- **Decision:** Delete all Expo/React Native files (`App.tsx`, `app.json`, `babel.config.js`, `package.json`, `package-lock.json`, `tsconfig.json`, `node_modules/`, `src/`). The application is 100% native Android built with Kotlin 2.0, Jetpack Compose, Hilt, Room, and WorkManager.
- **Consequences:** Eliminates build ambiguity, reduces repository footprint, and focuses entirely on high-performance native Android capabilities (instant share targets, on-device ML Kit, WorkManager background processing).

---

## ADR 002: Sacred Source vs. Additive Derived Architecture
- **Date:** August 2026 (Milestone 0)
- **Status:** Accepted
- **Context:** Bookmarking tools often overwrite or mutate user source content when AI summaries or transformations run.
- **Decision:** Strict segregation between:
  1. **Source Data (Sacred & Immutable):** `items`, `item_raw_payload`, `media_assets`, `source_posts`, `source_comments`, `source_article`, `source_text`. Once saved, raw URLs, titles, text, author info, and media bytes are never mutated.
  2. **Derived Data (Additive & Ephemeral):** `derived_summaries`, `derived_points`, `derived_entities`, `derived_tags`, `ocr_blocks`, `embeddings`. Derived data is generated asynchronously, can be regenerated, and can be cleared by the user at any time without impacting source records.
- **Consequences:** Guarantees data integrity and ensures the user always retains the authentic original artifact.

---

## ADR 003: Non-Blocking Share Pipeline (<400ms Perceived Latency)
- **Date:** August 2026 (Milestone 0)
- **Status:** Accepted
- **Context:** When users share content from Twitter, Reddit, or Chrome, long blocking operations cause Android ANRs or user frustration.
- **Decision:**
  - ShareActivity runs with a lightweight HUD overlay.
  - On receiving an Intent, bytes from input streams are copied immediately to app-private storage, minimal database rows are inserted, and an `ItemProcessingWorker` is enqueued via WorkManager.
  - The Share HUD confirms "✓ Tucked" and auto-dismisses in < 400ms without waiting for network scraping, OCR, or AI.
- **Consequences:** Fast, predictable share UX regardless of network conditions or heavy payload size.

---

## ADR 004: Schema Evolution & Eliminating Destructive Migration
- **Date:** August 2026 (Milestone 0)
- **Status:** Accepted
- **Context:** `DatabaseModule.kt` previously utilized `.fallbackToDestructiveMigration()`.
- **Decision:** Destructive migrations are strictly banned. Schema transitions (such as v2 to v3) must be executed using explicit, thoroughly tested `Migration` objects with automated Room migration test coverage verifying zero data loss.
- **Consequences:** Ensures user data and saved vaults are protected against schema changes across app updates.

---

## ADR 005: Materialized Path for Hierarchical Comment Trees
- **Date:** August 2026 (Milestone 0)
- **Status:** Accepted
- **Context:** Schema v2 stored community comments as an unstructured JSON array. Rich social posts (Reddit, nested discussions) require multi-level tree rendering.
- **Decision:** Migrate from JSON blob to `source_comments` with a `path` column using materialized paths (e.g. `0001`, `0001.0001`, `0001.0002`).
- **Consequences:** Enables fast single-query hierarchical retrieval (`ORDER BY path ASC`), subtree deletion, and deterministic tree indentation in Compose.

---

## ADR 006: Local-First Privacy & Optional Pluggable AI
- **Date:** August 2026 (Milestone 0)
- **Status:** Accepted
- **Context:** Users require absolute privacy for personal notes, screenshots, and bookmarks.
- **Decision:**
  - Tuck requires no account and transmits no analytics or data to external servers by default.
  - OCR, classification, and entity extraction operate 100% on-device using ML Kit and regex.
  - AI summarization uses a NoOp provider by default, with opt-in on-device (Gemini Nano) or BYO Cloud API key.
- **Consequences:** 100% offline functionality and zero data leakage.
