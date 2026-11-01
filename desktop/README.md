# Stash for macOS (Apple Silicon)

A native macOS desktop port of **[Stash](../README.md)** (`com.stash.app`) built with Compose Multiplatform, Room (Bundled SQLite), Dagger 2, OkHttp, `yt-dlp`, and `ffmpeg`.

Every line of the upstream Android codebase (`core/*`, `data/*`, `feature/*`, `app`) is compiled **as-is without modifying a single upstream file**. All platform shims, network bridges, audio pipeline components, and macOS packaging live strictly inside [`desktop/`](.).

---

## Requirements

Install the required runtime tools via [Homebrew](https://brew.sh):

```bash
brew install openjdk@17 ffmpeg yt-dlp quickjs
```

| Dependency | Purpose |
|---|---|
| `openjdk@17` | Compiles the project and provides the bundled JVM runtime inside `Stash.app` |
| `ffmpeg` / `ffprobe` | Real-time 44.1 kHz stereo PCM decoding (float for upstream's DSP), HLS / DASH demuxing, stream inspection, and metadata tagging |
| `yt-dlp` | Initial YouTube audio stream resolver (auto-updates its own `yt-dlp_macos` binary in `~/Library/Application Support/Stash/yt-dlp/`) |
| `quickjs` (`qjs`) | Executes YouTube player signature/N-challenge JavaScript during stream extraction |

---

## Quick Start

### 1. Run Directly via Gradle

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop run
```

### 2. Build the Native macOS `Stash.app` Bundle

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop createDistributable
```

The self-contained macOS application bundle is written to:
```
desktop/build/compose/binaries/main/app/Stash.app
```

It uses the exact metadata and assets from the Android counterpart (`app/build.gradle.kts` and `app/src/main/res/`):
- **Name:** `Stash`
- **Bundle Identifier (`CFBundleIdentifier`):** `com.stash.app`
- **Version (`CFBundleShortVersionString`):** `0.9.122`
- **Build (`CFBundleVersion`):** `158`
- **Icon (`Stash.icns`):** Generated at build time from Android's adaptive icon assets (`ic_launcher_background.xml` + `ic_launcher_foreground.png`)

You can launch it from Finder, copy it to `/Applications`, or open it from the terminal:
```bash
open desktop/build/compose/binaries/main/app/Stash.app
```

### 3. Build a Distributable `.dmg` Disk Image

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop packageDmg
```

When installing from a downloaded `.dmg`, clear the macOS quarantine attribute before first launch:
```bash
xattr -dr com.apple.quarantine /Applications/Stash.app
```

### 4. Run the Verification Suite

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop test
```

The suite boots the real Dagger graph, builds every ViewModel and worker, renders the UI, and plays audio through the real pipeline (a local file, an HLS stream, and a Download-mode refusal). It uses your real `~/Library/Application Support/Stash` data. To also search YouTube Music and stream the first result (needs the network, no account; Online mode is switched on for the test and restored after):

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop test -Pstash.networkTests=true
```

---

## Where Data Is Stored on macOS

**No user data, credentials, databases, or downloaded tracks are ever stored inside the git repository.**

All runtime files live in standard macOS per-user directories:

| Path | Contents |
|---|---|
| `~/Library/Application Support/Stash/databases/stash.db` | Room SQLite database (playlists, tracks, mixes, sync state) |
| `~/Library/Application Support/Stash/files/datastore/` | Preferences DataStore (`stash_preferences.preferences_pb`) |
| `~/Library/Application Support/Stash/shared_prefs/` | AES-256-GCM encrypted cookie & token stores |
| `~/Library/Application Support/Stash/files/Stash/` | Offline downloaded audio files (FLAC / M4A / Opus) |
| `~/Library/Application Support/Stash/yt-dlp/` | Auto-updated `yt-dlp_macos` binary |
| `~/Library/Caches/Stash/` | Streaming audio cache (`exo_stream_cache`) & Coil image cache |
| **macOS Login Keychain** (`com.stash.desktop`) | Master Tink AEAD keyset protecting your encrypted credentials |

To completely reset your local Stash state on macOS:
```bash
rm -rf ~/Library/Application\ Support/Stash ~/Library/Caches/Stash
security delete-generic-password -s com.stash.desktop 2>/dev/null || true
```

---

## Optional Build Configuration (`local.properties`)

Just like the Android build, optional API keys (such as Last.fm or a custom lossless relay config) can be placed in an untracked `local.properties` file at the repository root:

```properties
lastfm.apiKey=YOUR_LASTFM_API_KEY
lastfm.apiSecret=YOUR_LASTFM_API_SECRET
lossless.configUrl=https://...
lossless.configPubKey=...
```

If omitted, the app builds and runs normally with those optional integrations disabled.

---

## Architecture & Porting Details

See **[`PORTING.md`](PORTING.md)** for the complete technical ledger of:
- How all 23 upstream Android modules are compiled untouched
- The 3 excluded Google Cast files and the 5 shadowed library classes (`Migration`, `PlatformContext`, `NavTypeConverter`, `NavHostController`, `InvalidationTracker`)
- The `DesktopSpotifyWebPlayerBridge` authentication interceptor
- The chunked HTTP Range streaming, the HLS / DASH proxy, and the `ffmpeg` → media3 `AudioOutput` → upstream float DSP audio engine
- The upstream features without a macOS equivalent
- Every Android → JVM shim package and class
