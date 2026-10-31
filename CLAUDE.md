# CLAUDE.md - SimpMusic Project Guide for AI Agents

## 🌐 Language Rule

**Response language**: Always respond in **English**.

## 📋 Project Overview

**SimpMusic** is a FOSS (Free and Open Source Software) YouTube Music client for Android and Desktop, built with Compose Multiplatform.

### Main Purpose
- Stream music from YouTube Music and YouTube for free, ad-free, with background playback
- Provide advanced features like Spotify Canvas, AI song suggestions, synced lyrics
- Support both Android and Desktop (Windows, macOS, Linux)

### Basic Information
- **Package name**: `com.maxrave.simpmusic`
- **Primary language**: Kotlin
- **UI Framework**: Jetpack Compose / Compose Multiplatform
- **Architecture**: Clean Architecture + MVVM
- **Build system**: Gradle (Kotlin DSL)

## 🏗️ Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────┐
│  Presentation Layer (UI)            │
│  - Jetpack Compose / Compose MP     │
│  - ViewModels (MVVM)                │
│  - UI States                        │
├─────────────────────────────────────┤
│  Domain Layer                       │
│  - Use Cases                        │
│  - Domain Models                    │
│  - Repository Interfaces            │
├─────────────────────────────────────┤
│  Data Layer                         │
│  - Repository Implementations       │
│  - Data Sources (Remote/Local)      │
│  - Database (Room)                  │
├─────────────────────────────────────┤
│  Service Layer                      │
│  - YouTube Music Scraper            │
│  - Spotify Service                  │
│  - AI Service                       │
│  - Lyrics Service                   │
│  - Discord RPC (Kizzy)              │
└─────────────────────────────────────┘
```

## 📁 Module Structure

### Root Modules

#### 1. **composeApp/**
- **Shared Compose Multiplatform module** - main module containing shared code
- Supports: Android, Desktop (JVM), iOS (future)
- Contains all UI (Compose) and business logic
- Source sets:
  - `commonMain/`: Shared code for all platforms
  - `androidMain/`: Android-specific code
  - `desktopMain/`: Desktop-specific code
- Can run **Desktop app directly** from this module

#### 2. **androidApp/**
- **Android-specific module** to build Android app
- Depends on `composeApp` as a shared module
- Contains Android-specific configuration:
  - AndroidManifest.xml
  - Android build configuration
  - Android resources (if needed)
  - Entry point for Android app
- **vivoAndroidApp/** builds the same app under `com.spotify.music` for vivo's Origin Island. It has no code of its own: its source set points at androidApp's sources, resources and manifest. Its `build.gradle.kts` mirrors androidApp's, so keep the two in step.

#### 3. **core/**
Contains core modules organized by functionality:

##### **core/common/**
- Shared utilities
- Extension functions
- Constants
- Helper classes

##### **core/domain/**
- Domain models
- Use cases
- Repository interfaces
- Business logic rules

##### **core/data/**
- Repository implementations
- Data sources (Remote & Local)
- Database schemas (Room)
- Data mappers

##### **core/media/**
- **media3/**: Media3 ExoPlayer integration (includes `CrossfadeExoPlayerAdapter` for DJ-style crossfade on Android, and `audio/EqualizerAudioProcessor` for the ten-band equalizer)
- **media3-ui/**: Media3 UI components
- **media-jvm/**: JVM media playback (libmpv via JNA — replaced VLCJ, which replaced GStreamer post-1.0.4)
- **media-jvm-ui/**: JVM media UI components

##### **core/service/**
Service modules:

- **kotlinYtmusicScraper/**: YouTube Music API scraper
- **spotify/**: Spotify Web API integration (Canvas, Lyrics)
- **aiService/**: AI features (OpenAI, Gemini integration)
- **autoEqService/**: AutoEq headphone correction profiles (index + fixed-band curves)
- **lyricsService/**: Lyrics fetching (LRCLIB, SimpMusic Lyrics, BetterLyrics)
- **listenTogether/**: shared listening rooms, wire-compatible with Metrolist (`MetrolistGroup/metroproto`)
- **loginSync/**: the QR-paired transport that moves sign-ins from the Android app to the Desktop app (Ktor client on the phone, one-shot Ktor CIO server on the Desktop, AES-256-GCM). Android + JVM targets only; carries opaque bytes — the data layer decides what they mean
- **kizzy/**: Discord Rich Presence
- **ktorExt/**: Ktor extensions for networking

#### 4. **crashlytics/** & **crashlytics-empty/**
- **crashlytics/**: Full version with Sentry crash reporting
- **crashlytics-empty/**: FOSS version without tracking

#### 5. **cast/** & **cast-empty/**, 6. **lastfm/** & **lastfm-empty/**
Full-build module with a no-op FOSS twin of identical public API, chosen by the `isFullBuild` Gradle property (same shape as crashlytics). Cast: Google Cast handoff in `core/media/media3`. Last.fm: scrobbling (web auth flow, never the desktop flow) and public track tags for genre playlists. Details and traps: `docs/ARCHITECTURE_HISTORY.md`.

## 🛠️ Key Technologies

### Android/Mobile
- **Jetpack Compose**: Modern UI toolkit
- **Material Design 3**: Design system
- **Media3 (ExoPlayer)**: Media playback
- **Room**: Local database
- **Coroutines & Flow**: Async programming
- **Hilt/Koin**: Dependency injection

### Desktop
- **Compose for Desktop**: UI
- **libmpv** (mpv's C client API, bound with JNA): audio + video playback. Replaced VLCJ, which had replaced GStreamer post-1.0.4
- libmpv natives are bundled per platform via `./gradlew :composeApp:mpvSetupAll` into `mpv-natives/<os>-<arch>/`

### Networking & APIs
- **Ktor Client**: HTTP client
- **Kotlin Serialization**: JSON parsing
- **YouTube Music hidden API**: Data source
- **quickjs-kt** (`io.github.dokar3:quickjs-kt`): runs YouTube's player JS on-device to solve signature/`n` challenges
- **Spotify Web API**: Canvas and lyrics
- **OpenAI/Gemini API**: AI features

### Data & Storage
- **Room Database**: Local persistence
- **DataStore**: Preferences
- **Caching**: Offline playback support

### Third-party Integrations
- **SponsorBlock**: Skip sponsors
- **ReturnYouTubeDislike**: Vote information
- **LRCLIB**: Lyrics provider
- **BetterLyrics**: Additional lyrics provider (added in v1.0.4)
- **Sentry**: Crash reporting (Full version only)

## 📝 Development Guidelines

### Code Style
- **Kotlin coding conventions**: Follow Kotlin official guidelines
- **Compose best practices**: Single source of truth, unidirectional data flow
- **Clean Architecture**: Strict layer separation, dependency rule

### Module Dependencies
```
UI Layer (composeApp)
    ↓
Domain Layer (core/domain)
    ↓
Data Layer (core/data)
    ↓
Service Layer (core/service/*)
    ↓
Common (core/common)
```

**Dependency Rule**: Higher layer modules can only depend on lower layer modules, NOT vice versa.

### Working with UI
- Use **Jetpack Compose** for all new UI
- Follow **Material Design 3** guidelines
- State management with **StateFlow** or **State\<T>**
- Side effects with **LaunchedEffect**, **DisposableEffect**

### Working with Data
- Repository pattern for all data operations
- Use cases for complex business logic
- Mapping between Data models ↔ Domain models ↔ UI models
- Room for local persistence
- Ktor for network requests

### Research Before Implementation (MANDATORY)

Before implementing code, researching code, or answering technical questions, the AI agent **MUST** follow this research workflow:

#### Step 1: Look up official documentation
- Use **MCP Context7** (`resolve-library-id` → `query-docs`) to fetch up-to-date documentation for any library/framework about to be used
- Understand the latest API surface, breaking changes, and recommended usage patterns

#### Step 2: Evaluate pros, cons, and alternatives
- Use **WebSearch** to research:
  - Pros and cons of the library/approach
  - Alternative libraries or approaches that solve the same problem
  - Known issues, performance concerns, or deprecation notices
- Compare and evaluate whether the chosen library/approach is the best fit for this project

#### Step 3: Study OSS best practices
- Use **Grep** (on GitHub via web search) or **WebSearch** to find how well-known open-source projects implement similar features
- Verify the approach follows established best practices before adopting it
- Pay attention to patterns used in projects with similar architecture (Clean Architecture, Compose Multiplatform, etc.)

#### Step 4: Make a decision and justify
- Only proceed with implementation after completing steps 1-3
- If a library/approach has significant drawbacks or better alternatives exist, recommend the better option to the user before proceeding
- Document the rationale briefly when introducing new dependencies or patterns

**This workflow applies to**: Adding new libraries, choosing architectural patterns, implementing new features with unfamiliar APIs, answering "how should we do X?" questions, and evaluating technical approaches.

**This workflow does NOT apply to**: Simple bug fixes in existing code, minor refactoring, or tasks using libraries already well-established in the project.

### Verification After Code Changes
- **Do NOT build the app** to verify code changes. Instead, use **JetBrains MCP** tools (`get_file_problems`, `getDiagnostics`) to check for compile errors and warnings in real-time.
- Only run Gradle build when explicitly requested by the user or for final release verification.

### Testing
- Unit tests for Domain layer (Use cases)
- Repository tests with fake data sources
- UI tests with Compose Testing

## 🎯 Common Tasks

### 1. Add New UI Feature
**Location**: `composeApp/src/commonMain/kotlin/`
- Create Composable function in appropriate package
- Use ViewModel for state management
- Follow Material 3 design patterns

### 2. Add New API Endpoint
**Location**: `core/service/kotlinYtmusicScraper/`
- Implement endpoint in corresponding service
- Create data model for response
- Map to domain model

### 3. Add New Database Entity
**Location**: `core/data/src/main/java/.../database/`
- Define Entity with Room annotations
- Create DAO interface
- Update Database class
- Create migration if needed

### 4. Add New Use Case
**Location**: `core/domain/src/main/java/.../usecase/`
- Create use case class
- Inject repository dependencies
- Implement business logic
- Return Result/Flow

### 5. Work with Media Playback
**Location**: `core/media/media3/` (Android) or `core/media/media-jvm/` (Desktop)
- Media3/ExoPlayer + CrossfadeExoPlayerAdapter for Android
- libmpv (MpvPlayerAdapter / MpvPlayer / MpvLibrary) for Desktop
- Queue management in `core/data/src/.../mediaservice/`
- Playback controls

### 6. Add New Lyrics Provider
**Location**: `core/service/lyricsService/`
- Implement lyrics fetcher interface
- Add fallback logic
- Handle synced/unsynced lyrics

### 7. AI Features
**Location**: `core/service/aiService/`
- OpenAI integration
- Gemini integration
- AI lyrics translation
- Song recommendations

### 8. Add a New Icon
Use the `simpmusic-icons` skill. Icons are generated Material Symbols Rounded `ImageVector`s in `composeApp/.../ui/icon/`, used as `SimpIcons.<Name>` with a per-icon import, and must end with `.buildSimpIcon()` (`SimpIconsFitTest` enforces it). No `material-icons-extended`, no XML drawables, never `autoMirror = true`. Full traps: `docs/ARCHITECTURE_HISTORY.md`.

### 9. Prepare a Release (changelogs)

**Location**: `changelogs/<version>/`, one folder per version, holding `en-US.md` and `vi-VN.md`. The full procedure is the `simpmusic-release-changelog` skill in `.claude/skills/`.

- `fastlane/metadata/android/<locale>/changelogs/<versionCode>.txt` stays the short store text (F-Droid caps it at 500 characters) and is written by the owner. The full changelog lives in `changelogs/`.
- Sections are New / Improved / Fixed (vi: Mới / Cải thiện / Sửa lỗi) and list only shipped work. Every line links its issue or PR with a full URL, because GitHub does not autolink `#N` inside repository files.
- Shipped changes with no issue of their own are gathered into ONE "Polish for <version>" task on Project #2, and their lines link that task.
- Reconcile Project #2 before writing. Close issues fixed by shipped commits: post the "Fixed in <version> (<sha>)" comment FIRST, then set the milestone and Status Done. Done closes the issue by itself, so a later `gh issue close --comment` silently drops the comment.
- vi-VN addresses the reader as "bạn".

## 📍 Important Files and Locations

### Configuration
- `build.gradle.kts` (root): Root build configuration
- `gradle/libs.versions.toml`: Version catalog for dependencies
- `settings.gradle.kts`: Module inclusion

### Main Application
- `composeApp/src/commonMain/kotlin/`: Shared Compose code
- `composeApp/src/androidMain/kotlin/`: Android-specific code
- `composeApp/src/desktopMain/kotlin/`: Desktop-specific code

### Database
- `core/data/src/main/java/.../database/`: Room database schemas
- Migrations in Database class

### Network
- `core/service/kotlinYtmusicScraper/`: YouTube Music API
- `core/service/spotify/`: Spotify API
- `core/service/ktorExt/`: Ktor utilities

### Resources
- `composeApp/src/commonMain/composeResources/`: Shared resources
- `composeApp/src/androidMain/res/`: Android resources
- Crowdin integration for translations

## 🔧 Build Variants

### Android
- **Full**: With Sentry crash reporting (module: `crashlytics`)
- **FOSS**: No tracking (module: `crashlytics-empty`)

### Desktop
- **Windows**: `.msi` installer
- **macOS**: `.dmg` (ARM and x86-64)
- **Linux**: `.AppImage` (DEB and RPM removed post-1.0.4)

## 🚨 Important Notes

### Privacy & Data Collection
- FOSS version: NO tracking
- Full version: Only Sentry crash reporting
- "Send back to Google" feature: Optional, only when user enables

### Platform-specific Considerations

#### Android
- Min SDK: Check `androidApp/build.gradle.kts`
- Target SDK: Latest stable
- Android Auto support
- Background playback with MediaSession

#### Desktop
- **Required Dependencies**:
  - libmpv: audio + video playback (bundled via `mpvSetupAll`; falls back to a system-wide libmpv when `mpv-natives/` has not been staged)
- **Minimum macOS: 15.0** — raised from 11.0 when VLC was replaced by mpv. mpv's macOS release builds target macOS 15 (96/98 arm64 dylibs declare `minos 15.0`; on Intel `libmpv` itself does), and Conveyor rejects a lower `LSMinimumSystemVersion`. No mpv artifact covers both architectures below 15.
- **Features**:
  - Deep link support (`simpmusic://` and `simpmusic.org`)
  - Mini Player window (always-on-top, resizable, draggable)
  - Crash dialog
  - Custom title bar (macOS only; Windows and Linux keep the native title bar)
- **Limitations**:
  - No offline playback

### External APIs
- YouTube Music: Hidden/unofficial API (may change anytime)
- Spotify: Requires login for lyrics
- OpenAI/Gemini: User must provide API key
- SponsorBlock: Public API
- LRCLIB: Public lyrics API

## 🎵 Media Playback
- **Desktop**: libmpv via JNA (`core/media/media-jvm/.../mpv/`: `MpvLibrary`, `MpvPlayer`, `MpvPlayerAdapter`, `MpvVideoFrameSource`), natives staged by `mpvSetupAll`. Separate audio/video URLs merge into one `edl://` source.
- **Android**: Media3/ExoPlayer with `CrossfadeExoPlayerAdapter` (`core/media/media3`).
- Crossfade guards must exist on BOTH trigger paths (position polling and EOF); playback settings must reach every live mpv handle. Details: `docs/ARCHITECTURE_HISTORY.md`.

## 🤝 Contributing
Branch from `dev`, follow the conventions, test before submitting, clear PR title and description. Translations go through Crowdin, never edit them directly. See `CODE_OF_CONDUCT.md`.

## 🎯 Quick Start
1. Respect layer dependencies. 2. Reuse existing patterns. 3. `commonMain` must work on Android and Desktop. 4. FOSS must not track. 5. YouTube Music's API can change anytime. Dependency graph: `asset/dependencies_graph.svg`.

## 📚 History and feature docs
Dated design decisions and traps live in `docs/ARCHITECTURE_HISTORY.md`. Feature docs: `docs/GENRE_PLAYLISTS.md` (automatic `Genre · <name>` playlists on like; Room v28), `docs/DEBUG_APK.md`.

## 🔄 CLAUDE.md Rule
Keep this file under ~400 lines. Update it only for module or architecture changes, one or two lines at most. Put dated details in `docs/ARCHITECTURE_HISTORY.md` and feature explanations in their own `docs/*.md`.

**Last updated**: 2026-10-23
