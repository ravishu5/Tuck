# Tuck — Local-First Personal Digital Archive

> **"Save anything. Find it later."**

Tuck is a production-quality, local-first Android application that solves content fragmentation. It acts as a calm, intelligent "digital drawer" where you can share and store links, screenshots, notes, PDFs, research papers, and documents without ever being forced to organize them manually. When you need them later, search and retrieve them instantly using on-device full-text search, OCR, entity extraction, and smart categories.

---

## Key Features

- **⚡ Instant Non-Blocking Share Sheet**:
  - Appears in Android's system share menu for `ACTION_SEND` and `ACTION_SEND_MULTIPLE`.
  - Supports URLs, Notes, Images/Screenshots, PDFs, and Documents.
  - Instantly saves content locally (`PENDING`), displays a sleek bottom HUD ("Saved ✓"), and processes in the background via WorkManager.
- **🔍 Full-Text Search (SQLite FTS5 / BM25 Ranking)**:
  - Searches simultaneously across titles, descriptions, URLs, raw text, on-device OCR text, entity values, and tags.
  - Supports prefix matching, phrase search, and live debounced results as you type.
  - Filters by Content Type, Favorites, Date ranges (e.g. Last 30 days), Collections, and multiple sort orders (Relevance, Newest, Oldest, Recently Opened).
- **📷 On-Device OCR (ML Kit Text Recognition)**:
  - Automatically extracts all text from shared screenshots, photos, and infographics locally on your device.
  - Zero cloud dependency, zero external API calls.
- **📄 Native PDF Processing**:
  - Generates lightweight first-page thumbnail previews via Android's `PdfRenderer`.
  - Extracts text from searchable and scanned PDF documents for full-text search.
  - Securely opens PDFs in external viewers via Android `FileProvider`.
- **🏷️ Deterministic Entity Extraction & Smart Categorization**:
  - Deterministic regex entity extraction for Phone Numbers, Emails, URLs, Prices/Money (₹, $, €, Rs.), Dates, and Hashtags.
  - Automatic rule-based categorization into *Programming, Research, Shopping, Travel, Food & Dining, Finance, Work, Education, Articles, Videos, Images, PDFs, and Notes*.
  - Manual custom collections support (create, rename, delete without deleting items).
- **🎨 5 Curated Theme Palettes**:
  - Linen (Warm Paper), Noir (High Contrast Dark), Forest (Earth/Sage), Cobalt (Deep Navy), Plum (Muted Purple).
  - Material 3 dynamic color styling with intentional typography and smooth micro-interactions.
- **🔒 100% Offline-First & Private**:
  - No user account or login required.
  - No remote servers, no cloud telemetry, no analytics on user data.
  - Strict app-private storage for media and metadata.
- **🗑️ Non-Destructive Trash & Restore**:
  - Items are moved to Trash first before permanent deletion, with single-tap restore and "Empty Trash" confirmation.

---

## Living Documentation & Engineering Artifacts
- [`PROGRESS.md`](file:///Users/ravi/Desktop/Tuck/PROGRESS.md) — Detailed milestone acceptance criteria tracker (M0–M7).
- [`AUDIT.md`](file:///Users/ravi/Desktop/Tuck/AUDIT.md) — Comprehensive codebase audit across all 52 Kotlin files, DAOs, and workers.
- [`DECISIONS.md`](file:///Users/ravi/Desktop/Tuck/DECISIONS.md) — Architecture Decision Records (ADRs).

---

## Architecture & Technology Stack

Tuck is built following **Clean Architecture**, **MVVM**, and the **Repository Pattern** with modern Android development practices:

- **Language & Runtime**: Kotlin 2.0.x with Coroutines & Flow.
- **UI Framework**: Jetpack Compose with Material 3, Custom Typography, and Dark/Light/System theme support.
- **Dependency Injection**: Hilt (Dagger).
- **Database**: Room 2.6.x with SQLite FTS5 virtual table for full-text search.
- **Background Processing**: AndroidX WorkManager `CoroutineWorker` for reliable, retryable background enrichment.
- **Image Loading**: Coil Compose.
- **On-Device OCR**: Google ML Kit Text Recognition (`play-services-mlkit-text-recognition`).
- **Web Parsing**: Jsoup for safe OpenGraph metadata extraction (title, description, canonical URL, domain) with tracking parameter stripping (`utm_*`, `fbclid`, `gclid`, etc.).
- **Build System**: Gradle 8.10+ with Gradle Version Catalogs (`libs.versions.toml`).

---

## System Architecture Diagram

```
+---------------------------------------------------------------------------------------+
|                                    UI Layer (Jetpack Compose)                         |
|  - HomeScreen           - SearchScreen          - ItemDetailScreen                    |
|  - CollectionsScreen    - SettingsScreen        - TrashScreen                         |
|  - ShareActivity (Non-blocking floating HUD: Save -> Background Work -> Done)         |
+-------------------------------------------+-------------------------------------------+
                                            |
+-------------------------------------------v-------------------------------------------+
|                                Presentation / ViewModels                              |
|  - HomeViewModel        - SearchViewModel       - ItemDetailViewModel                 |
|  - CollectionsViewModel - SettingsViewModel     - ShareViewModel                      |
+-------------------------------------------+-------------------------------------------+
                                            |
+-------------------------------------------v-------------------------------------------+
|                                Domain / Business Logic                                |
|  - SavedItemRepository  - SearchRepository      - CollectionRepository                |
|  - ContentClassifier    - DuplicateDetector     - EntityExtractor                     |
+-------------------------------------------+-------------------------------------------+
                                            |
+-------------------------------------------v-------------------------------------------+
|                             Data Layer & Background Workers                           |
|  - SavedItemRepositoryImpl  - SearchRepositoryImpl (FTS5 BM25 Engine)                 |
|  - FileStorageService       - UrlMetadataProcessor   - ImageOcrProcessor              |
|  - PdfProcessor             - ItemProcessingWorker (WorkManager CoroutineWorker)      |
+-------------------------------------------+-------------------------------------------+
                                            |
+-------------------------------------------v-------------------------------------------+
|                               Local Database & Storage                                |
|  - Room Database (saved_items, saved_items_fts, entities, tags, collections)          |
|  - App-private disk storage (files/saved/images, files/saved/pdfs, files/thumbnails)  |
+---------------------------------------------------------------------------------------+
```

---

## Processing Pipeline

```
   Incoming Share Intent (ACTION_SEND / ACTION_SEND_MULTIPLE)
                            ↓
   ShareParser: Content Normalization & Fast Local Save (PENDING)
                            ↓
   WorkManager Enqueue (ItemProcessingWorker) & Instant HUD Dismiss
                            ↓
   +---------------------------------------------------------+
   | Background Processing (ItemProcessingWorker):           |
   | 1. State -> PROCESSING                                  |
   | 2. URL Metadata / Image OCR / PDF Stream & Page OCR     |
   | 3. Regex Entity Extraction (Phone, Email, Money, Date)  |
   | 4. Rule-based Smart Classification & Tagging            |
   | 5. SQLite FTS5 Indexing & Room Update                   |
   | 6. State -> READY                                       |
   +---------------------------------------------------------+
```

---

## Project Structure

```
Tuck/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tuck/app/
│   │   │   │   ├── TuckApplication.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── db/
│   │   │   │   │   │   │   ├── Converters.kt
│   │   │   │   │   │   │   ├── TuckDatabase.kt
│   │   │   │   │   │   │   ├── dao/ (SavedItemDao, SavedItemFtsDao, EntityDao, TagDao, CollectionDao)
│   │   │   │   │   │   │   └── entity/ (SavedItemEntity, SavedItemFtsEntity, EntityEntity, etc.)
│   │   │   │   │   │   └── storage/ (FileStorageService.kt)
│   │   │   │   │   └── repository/ (SavedItemRepositoryImpl, SearchRepositoryImpl, etc.)
│   │   │   │   ├── di/ (DatabaseModule, RepositoryModule, etc.)
│   │   │   │   ├── domain/
│   │   │   │   │   ├── classifier/ (ContentClassifier.kt)
│   │   │   │   │   ├── model/ (SavedItem, ContentType, ExtractedEntity, Collection, etc.)
│   │   │   │   │   └── repository/ (SavedItemRepository, SearchRepository, etc.)
│   │   │   │   ├── processing/
│   │   │   │   │   ├── DuplicateDetector.kt
│   │   │   │   │   ├── EntityExtractor.kt
│   │   │   │   │   ├── ImageOcrProcessor.kt
│   │   │   │   │   ├── ItemProcessingWorker.kt
│   │   │   │   │   ├── PdfProcessor.kt
│   │   │   │   │   ├── RuleBasedContentClassifier.kt
│   │   │   │   │   ├── ShareParser.kt
│   │   │   │   │   └── UrlMetadataProcessor.kt
│   │   │   │   └── ui/
│   │   │   │       ├── MainActivity.kt
│   │   │   │       ├── TuckApp.kt
│   │   │   │       ├── collections/ (CollectionsScreen, CollectionsViewModel)
│   │   │   │       ├── components/ (CommonComponents.kt)
│   │   │   │       ├── detail/ (ItemDetailScreen, ItemDetailViewModel)
│   │   │   │       ├── home/ (HomeScreen, HomeViewModel)
│   │   │   │       ├── navigation/ (NavRoutes.kt)
│   │   │   │       ├── search/ (SearchScreen, SearchViewModel)
│   │   │   │       ├── settings/ (SettingsScreen, SettingsViewModel)
│   │   │   │       ├── share/ (ShareActivity, ShareViewModel)
│   │   │   │       ├── theme/ (Theme.kt, Color.kt, Type.kt)
│   │   │   │       └── trash/ (TrashScreen, TrashViewModel)
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/java/com/tuck/app/ (Unit & Integration Tests)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```

---

## Build & Run Instructions

### Prerequisites
- JDK 17 (recommended: Oracle / OpenJDK 17)
- Android SDK with Platform 35 (`android-35`) and Build-Tools 35.0.0+

### Building via Terminal
```bash
# Set Java 17 Home (if not set in environment)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# Run Unit Tests
./gradlew testDebugUnitTest

# Build Debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing on Device / Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Supported Android Versions
- **Minimum SDK**: Android 8.0 (API Level 26)
- **Target SDK**: Android 15 (API Level 35)
- **Compile SDK**: Android 15 (API Level 35)

---

## Privacy Model
Tuck enforces a strict local-first privacy policy:
- **No Cloud Synchronization**: All SQLite databases, FTS indexes, OCR text, and media files remain strictly on the user's physical device.
- **Zero Generative LLM / Cloud AI**: Intelligence is handled via local deterministic regex extractors, rule-based heuristics, and on-device ML Kit OCR models.
- **No Analytics / Telemetry**: No user content, URLs, titles, or OCR outputs are ever collected or transmitted.
- **Safe Networking**: Internet access is solely used for optional on-demand webpage OpenGraph title/description fetching when a user saves a URL.
