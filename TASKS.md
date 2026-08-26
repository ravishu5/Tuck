# Tuck — Task Queue

Work these **one at a time, in order**. Each task is self-contained: goal, scope, what is
explicitly out of scope, acceptance criteria, and how to verify.

## Working agreement

The rules in `PROGRESS.md` apply to every task here, in particular:

1. **Plan before code.** Post the file list and schema changes, wait for approval, then implement.
2. **A class nothing calls is not done.** Wire it, or mark it `[!]` not-wired. This repo has already
   shipped two dead components (`SourceExtractorRegistry`, `MemoryNotificationWorker`) that were
   marked complete while nothing referenced them.
3. **Verify on a device, not by reasoning.** `./gradlew assembleDebug testDebugUnitTest
   connectedDebugAndroidTest`, then exercise the actual feature. An FTS5 index compiled and passed
   review, then failed on device with `no such module: fts5` — only the device run caught it.
4. **Never delete an unticked line** from `PROGRESS.md`. Cut features get a strikethrough and a reason.
5. **Ask before adding a dependency.** State what it replaces and the APK size delta.
6. Update `PROGRESS.md` and `DECISIONS.md` as part of the task, not afterwards.

---

## Reality check: what already exists

Do **not** rebuild these. The product brief this queue came from describes several as if they were new.

| Brief says | Already in the repo |
|---|---|
| Share Sheet as the core capture flow | Done — `ACTION_SEND`, `SEND_MULTIPLE`, `PROCESS_TEXT`, `<share-target>`, QS tile, widget, app shortcuts |
| OCR on screenshots | Done — ML Kit, results in `ocr_blocks`, indexed for search |
| Metadata extraction | Done — `UrlMetadataProcessor` + a `SourceExtractor` registry (Reddit, YouTube, Twitter, generic) |
| Entity extraction | **Partial** — regex only: `URL`, `EMAIL`, `PHONE`, `MONEY`, `DATE`, `HASHTAG`. `PERSON`, `ORGANIZATION`, `LOCATION`, `PRODUCT` exist in the enum but are **never emitted** |
| Related content | **Partial** — `RelatedItemsEngine` scores shared entities (+3), tags (+2), domain (+1.5), keywords (+0.5). No persisted graph |
| Search: exact / source / date | Done — FTS4 with weighted relevance ranking plus a query DSL (`source:`, `type:`, `in:`, `tag:`, `is:`, `after:`, `before:`) |
| Automatic collections | **Scaffolding only** — `collections.isSmart` and `smartQuery` columns exist and are read by nothing |
| Rule-based categorisation | Done — `RuleBasedContentClassifier` creates smart collections by category |
| Local-first, export | Done — JSON vault export/import and `.tuck` collection packs. Markdown/HTML export not built |
| Comment capture | Done — full nested Reddit trees in `source_comments` with materialized paths |

## Reality check: what conflicts with decisions already made

These are in the brief but are **out of scope** until a decision is reversed. Do not build them.

- **Semantic search, natural-language queries ("What did I save about Ravi?"), topic detection by LLM,
  AI summarisation.** AI was cut from v1 on 2026-08-25 (see `DECISIONS.md`). Everything in this queue
  is achievable with deterministic code. If Ravi reverses that decision, semantic search returns.
- **Public shareable collections and the growth loop.** Requires hosting; cloud was deferred on
  2026-08-25. Local export (T11) is the honest substitute for now.
- **Automatic person/organisation detection from free text.** There is no on-device NLP for this
  without a model — ML Kit's Entity Extraction API does *not* recognise person or company names.
  T1 gets people a different, more reliable way. Do not add an NER dependency without approval.

---

## The through-line

Everything in Phase A serves one moment, and it is the moment the whole product rests on:

> *"I saved this six months ago and Tuck knew exactly what I meant."*

Extraction accuracy and retrieval matter more than any screen. A wrong entity merge is worse than no
merge at all, because it silently corrupts the memory the user is trusting.

---

# Phase A — Make the entity layer real

## T1 — People from structured source metadata

**Goal:** Start emitting `PERSON` entities, from data already captured rather than from NLP.

**Why this way:** Every saved Reddit post carries `source_posts.authorHandle` (`u/someone`), every
comment carries one, YouTube carries a channel, X carries a handle. These are *exact, unambiguous
identities* — no inference required. This gets 80% of the "everything about Ravi" value with none of
the false-positive risk of name detection in prose.

**Scope**
- On enrichment, write a `PERSON` entity for the post author and for each comment author, with
  `normalizedValue` = platform-qualified handle (`reddit:someone`, `youtube:channelId`) so two people
  with the same display name on different platforms never collide.
- Backfill existing rows via a maintenance action (reuse the "rebuild derived data" path).
- Record `producer = "source-metadata"` so these are distinguishable from anything inferred later.

**Out of scope:** detecting names in body text or OCR. That is T3's problem and it is not solved here.

**Acceptance criteria**
- [ ] Saving a Reddit post creates a `PERSON` entity for its author, and one per comment author
- [ ] The same author across two saves resolves to one `normalizedValue`
- [ ] Same display name on different platforms produces two distinct entities
- [ ] Unit tests for normalisation; instrumentation test asserting entity rows after a full worker run
- [ ] Backfill leaves an already-processed library unchanged when run twice (idempotent)

---

## T2 — Entity identity and aliases (suggest, never silently merge)

**Goal:** One canonical entity per real-world thing, with aliases pointing at it.

**Why:** The brief wants `Ravi`, `Ravi Shankar`, `Ravi S.`, `@ravishankar` to be one person. That is
the right goal, but **automatic merging is dangerous** — merging two different people silently
corrupts the user's memory and is nearly impossible to notice. Tuck suggests; the user confirms.

**Scope**
- New tables: `entities_canonical` (id, type, displayName, createdAt) and `entity_aliases`
  (aliasValue, canonicalId, source: USER|EXACT|SUGGESTED, confidence).
- Automatic linking **only on exact normalised match** (same platform-qualified handle, same email,
  same phone). Never on fuzzy name similarity.
- Fuzzy candidates (normalised edit distance, initials, handle-vs-name) are stored as *suggestions*
  with a confidence, surfaced in the entity page as "Is this the same person?" with Merge / Not the same.
- "Not the same" is remembered so the same suggestion is never offered twice.
- Merging is reversible: keep the alias rows so a split can restore the previous state.

**Out of scope:** any cross-entity-type merging; merging organisations with people.

**Acceptance criteria**
- [ ] Two saves from the same Reddit author link to one canonical entity with zero user action
- [ ] Two different people with similar names are **not** merged automatically — only suggested
- [ ] Rejecting a suggestion suppresses it permanently
- [ ] Merge is undoable and a test proves item→entity links survive a merge/split round-trip
- [ ] Migration test proves existing `entities` rows map onto canonical entities without loss

---

## T3 — Topics without a model

**Goal:** Emit `TOPIC` entities so "you've been interested in Android, Supabase, Kotlin" is real.

**Why deterministic:** the corpus is local and small. Classic term weighting works, is explainable,
and costs nothing at runtime.

**Scope**
- Corpus-wide TF-IDF over indexed text (title, body, OCR), with a stopword list and a
  minimum-document-frequency floor so one-off words never become topics.
- Promote a term to a topic when it appears in ≥ N items (start N = 3, make it a constant with a
  comment explaining the trade-off).
- Recompute incrementally in the existing WorkManager pipeline, never on the save path.
- Keep the existing `RuleBasedContentClassifier` categories; topics are additive, not a replacement.

**Out of scope:** phrase extraction, embeddings, clustering, an LLM.

**Acceptance criteria**
- [ ] A library with 3+ Supabase items surfaces `supabase` as a topic; a single mention does not
- [ ] Stopwords and boilerplate ("http", "com", "the") never become topics
- [ ] Topic recompute over a seeded 10,000-item database completes in under 5s off the main thread
- [ ] Unit tests over a fixture corpus with asserted topic output

---

## T4 — Entity pages

**Goal:** Tap `Ravi` → everything Tuck knows about Ravi. This is the screen that makes the product
feel like memory rather than storage.

**Scope**
- Route `tuck://entity/{id}`, reachable from entity chips on the detail screen, from search results,
  and from automatic collections.
- Sections: header (name, type, aliases, item count) · related entities (co-occurrence, T5) · saved
  items grouped by source · timeline of first-seen/last-seen · pending alias suggestions (T2).
- Rename the canonical entity; hide an entity the user does not care about.

**Out of scope:** editing extracted values on the source item — source content stays immutable.

**Acceptance criteria**
- [ ] Entity page lists every item mentioning that entity, including via aliases
- [ ] Renaming updates every surface without touching source rows
- [ ] Opens in under 300ms with 500 linked items
- [ ] Compose UI test covering an entity with aliases and one with none

---

## T5 — Persisted relationship graph

**Goal:** Turn "related items" from a runtime scan into a real, queryable graph.

**Why:** `RelatedItemsEngine` currently recomputes by scanning all items in memory on every call. That
will not hold at 10k items, and nothing can be built on top of it.

**Scope**
- `entity_edges` (fromEntityId, toEntityId, weight, kind: CO_OCCURS|SAME_SOURCE|SAME_COLLECTION,
  lastUpdatedAt) plus `item_entities` as the item↔entity join.
- Update edge weights incrementally when an item is enriched or deleted.
- Reimplement `getRelatedItems` on top of the graph, keeping the existing weighting as the starting
  point so behaviour does not regress.
- Benchmark before and after against a 10,000-item seed; put both numbers in `DECISIONS.md`.

**Out of scope:** graph visualisation. That is optional and comes much later, if ever.

**Acceptance criteria**
- [ ] Related items for a 10k-item library return in under 100ms p95
- [ ] Deleting an item removes its edges — no orphans (instrumentation test)
- [ ] Results are at least as relevant as the current implementation on a fixture library

---

## T6 — Automatic collections that actually work

**Goal:** Make `collections.isSmart` and `smartQuery` do something. They have existed since schema v3
and are read by nothing.

**Scope**
- A smart collection stores a query DSL string (the parser from `SearchQueryParser` already exists) and
  resolves live rather than storing membership.
- Seed automatic collections from real signal: top topics (T3), top people (T1), top sources.
- Render them distinctly from manual collections, with counts.
- "Save this search as a collection" from the search screen.
- A smart collection can be dismissed; dismissal is remembered.

**Out of scope:** hand-built rule builder UI. The query DSL *is* the rule language.

**Acceptance criteria**
- [ ] A smart collection updates automatically when a matching item is saved, with no rebuild step
- [ ] Saving a search produces a working smart collection
- [ ] Manual and smart collections are visually distinguishable
- [ ] Deleting a smart collection never deletes items

---

# Phase B — Retrieval and organisation

## T7 — Entity operators in search

**Goal:** `person:ravi`, `topic:android`, `entity:supabase` in the existing search box.

**Scope:** extend `SearchQueryParser` (operators, chips and fall-through behaviour are already built
and tested — follow the existing pattern), resolve names through aliases from T2, and apply as a join
on `item_entities`.

**Acceptance criteria**
- [ ] `person:ravi` matches items linked via any alias of that canonical entity
- [ ] Unknown entity names return zero results rather than falling back to free text silently
- [ ] Parser unit tests extended; chips render for the new operators

---

## T8 — Grid view and view toggle

**Goal:** The brief and the original spec both promise "visual boards". There is no `LazyVerticalGrid`
anywhere in the codebase; everything is a list.

**Scope:** grid/list toggle on Collections, Inbox and search results; persisted per surface in
DataStore; thumbnail-dominant grid cards; correct empty and loading states in both modes.

**Acceptance criteria**
- [ ] Toggle persists across process death
- [ ] Grid scrolls at 60fps over a 10k-item seed (macrobenchmark or a documented manual measurement)
- [ ] Items with no thumbnail degrade to a readable type-glyph card, not a blank tile

---

## T9 — Bulk selection everywhere

**Goal:** Bulk actions exist only in Inbox. Extend to Collections, Home rails and search results.

**Scope:** long-press enters selection mode; select-all and range select; bulk move, tag, favorite,
archive, delete, export; a count in the app bar; back exits selection.

**Acceptance criteria**
- [ ] Selection survives rotation
- [ ] Bulk delete of 100 items is one undoable action, not 100
- [ ] Compose UI test for enter/exit/select-all

---

## T10 — Duplicate merge UI

**Goal:** `DuplicateDetector` and `items.dedupeGroupId` exist; nothing surfaces them. Detected
duplicates currently do nothing at all.

**Scope:** at capture, when a duplicate is detected, offer *View existing / Keep both / Merge* in the
share HUD without delaying the save. A merged item keeps the union of collections, tags and notes,
and every source capture is preserved — merging must never destroy a source row.

**Acceptance criteria**
- [ ] Saving the same URL twice offers the choice and never blocks the save
- [ ] Merge preserves both source captures and the union of user data
- [ ] Merge is undoable

---

# Phase C — Ownership

## T11 — Real export: Markdown, HTML, JSON, Obsidian-compatible

**Goal:** "Your memories belong to you" needs to be true in a format other tools can read. The `.tuck`
pack is only readable by Tuck.

**Scope**
- Per item and per collection: Markdown (YAML front-matter carrying entities, source URL, dates),
  HTML (self-contained, media inlined or in a sibling folder), JSON (full fidelity).
- An Obsidian-compatible vault export: one note per item, `[[wikilinks]]` to entity notes, one note
  per entity. This maps Tuck's automatic graph onto a structure Obsidian users already understand —
  and it is the honest version of the brief's interoperability promise.
- Share via `FileProvider`; never a raw `file://` URI.

**Out of scope:** import from Obsidian. One direction first.

**Acceptance criteria**
- [ ] A collection exported as an Obsidian vault opens in Obsidian with working wikilinks
- [ ] Markdown front-matter round-trips through the JSON exporter without loss
- [ ] Export of 1,000 items completes without OOM and reports progress

---

## T12 — Nested collections

**Goal:** `collections.parentId` has existed since v3 with no UI.

**Scope:** create a sub-collection, drag to re-parent, breadcrumbs, cycle prevention, and a decision
recorded on whether a parent shows descendants' items (recommend: yes, with a toggle).

**Acceptance criteria**
- [ ] A collection cannot become its own ancestor (unit test)
- [ ] Deleting a parent does not delete items; children re-parent to root
- [ ] Depth limit enforced and documented

---

# Phase D — Hardening

## T13 — Performance harness

**Goal:** Every performance claim in this repo is currently unmeasured.

**Scope:** debug-only seeder for 10,000 realistic items; `:benchmark` macrobenchmark module covering
cold start, list scroll, share-to-save latency and search latency; a baseline profile; numbers
recorded in `PROGRESS.md` against the budgets.

**Acceptance criteria**
- [ ] Seeder produces 10k items with media, entities and comments in under 60s
- [ ] Benchmarks run in CI-able form and print a comparable table
- [ ] Baseline profile measurably improves cold start; before/after in `DECISIONS.md`

---

## T14 — Fix the double-quoted SQL literal

**Goal:** A `SQLiteLog: double-quoted string literal: "428ac47e-..."` warning appears during save.
Some raw SQL interpolates a UUID in double quotes instead of binding it.

**Why it matters:** SQLite accepts it only as a legacy quirk. It is the shape of an injection bug, and
it breaks outright if the value ever contains a quote.

**Scope:** find the call site (likely dedupe or the migration path), bind the parameter, add a test.

**Acceptance criteria**
- [ ] No `double-quoted string literal` warnings in logcat during a full save-and-enrich cycle
- [ ] The query is parameterised and covered by a test

---

## T15 — Finish the source/derived split

**Goal:** `saved_items` still carries `originalText`, `extractedText`, `ocrText` and `commentsJson`
alongside the v3 tables that were meant to own them. Two sources of truth.

**Scope:** migrate readers onto `source_*` and `derived_*`, drop the legacy columns in a new migration,
keep the pre-v3 fallback path only where a real device could still hold old rows.

**Acceptance criteria**
- [ ] Migration test proves no text is lost
- [ ] "Regenerate derived data" rebuilds every derived table from source and is idempotent
- [ ] No reader touches the legacy columns

---

# Blocked — needs a decision from Ravi, do not start

| Item | Blocker |
|---|---|
| Reddit comment capture | The public `.json` endpoint returns **403** to every User-Agent tried. Needs OAuth app credentials. The extractor parses correctly (fixture-tested); only the fetch is blocked |
| Release keystore | Must be generated by a human. Signing plumbing is in place and falls back to the debug key, which must never reach Play |
| APK size — 43.6 MB against a 25 MB budget | Dominated by the bundled ML Kit model. Swapping `com.google.mlkit:text-recognition` for the Play-Services variant saves ~20 MB but requires Play Services, excluding de-Googled devices. Product decision |
| Semantic search, natural-language queries, AI summaries | AI cut from v1 on 2026-08-25 |
| Public collections and the sharing growth loop | Cloud deferred on 2026-08-25 |
| Graph visualisation | Only meaningful after T5. Optional even then — the brief itself says the graph should be a view of the system, not the system |

---

# Suggested order

T1 → T2 → T4 → T3 → T5 → T6 → T7 gets the entity-memory product working end to end, and T4 is
deliberately early so the value is visible before more plumbing goes in.

T14 is small and can be done any time it is convenient.

T8 → T9 → T10 → T12 is the organisation catch-up batch. T11 is independent and can be slotted anywhere.
T13 should happen before any release, and T15 before the schema grows further.
