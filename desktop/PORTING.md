# Stash macOS Port — Porting Ledger & Architecture

## 1. Core Principles
1. **Upstream code and build files are never edited.** `git diff --name-only upstream/master -- ':!desktop' ':!README.md'` must always be empty (only [`desktop/`](.) and the front-page [`README.md`](../README.md) differ from `upstream/master`).
2. **Upstream module sources are compiled in place.** The `syncUpstreamSources` task in [`desktop/build.gradle.kts`](build.gradle.kts) mirrors all 23 upstream modules (`core/*`, `data/*`, `feature/*`, `app`) into `desktop/build/upstream-src` at build time. Nothing is copied or forked into git.
3. **Binary/source-compatible JVM shims.** Android-only APIs are satisfied by shims in `desktop/src/main/{kotlin,java}`, declared at the exact same fully-qualified package and class names (or same-package Kotlin extensions) so upstream code compiles unmodified.
4. **Real runtime behaviour only.**
   - **Database:** Room (`BundledSQLiteDriver` via `androidx.sqlite:sqlite-bundled`) running all upstream Room migrations (`MIGRATION_1_2` through `MIGRATION_74_75`).
   - **Dependency Injection:** Upstream `@Module` and `@InstallIn` graphs compiled by Dagger 2 KSP (`DesktopAppComponent`, `DesktopViewModelComponent` with all 72 ViewModels, `DesktopWorkerFactories` with all 34 Workers).
   - **Networking:** OkHttp 4.12 with desktop Spotify Web Player token bridge (`DesktopSpotifyWebPlayerBridge`).
   - **Media Extraction & Transcoding:** Native macOS `yt-dlp`, `ffmpeg`, `ffprobe`, and QuickJS (`qjs`).
   - **Audio Playback & DSP:** Real-time `ffmpeg` 44.1 kHz stereo PCM decoding (float when the sink asks for it, as upstream's does) → media3 1.9's `AudioOutputProvider` seam → upstream's `StashAudioOutputProvider` / `StashAudioOutput` (float `FloatDspChain`: preamp, EQ, bass shelf, loudness, soft limiter) → `AudioTrack` shim on `javax.sound.sampled.SourceDataLine`.
5. **Minimal exclusions.** An upstream file may be excluded only when it cannot exist on the JVM. Every exclusion is recorded below with its rationale and desktop replacement.

---

## 2. Updating from Upstream
Because zero upstream code or build files are modified, rebasing onto upstream changes is conflict-free:
```bash
git fetch upstream && git rebase upstream/master
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home sh ./gradlew -p desktop test
# also stream a YouTube Music search result (needs the network, no account):
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home sh ./gradlew -p desktop test -Pstash.networkTests=true
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home sh ./gradlew -p desktop run
```

---

## 3. Excluded Upstream Files
| Upstream File | Why Impossible on JVM | Desktop Replacement |
|---|---|---|
| `data/ytmusic/.../potoken/BotGuardPoTokenMinter.kt` | Executes BotGuard JavaScript inside `android.webkit.WebView` | [`DesktopBotGuardPoTokenMinter.kt`](src/main/kotlin/com/stash/data/ytmusic/potoken/DesktopBotGuardPoTokenMinter.kt) — same class/constructor signature, delegates to upstream's `PoTokenMinter.None` fallback |
| `app/.../cast/GoogleCastDevices.kt` | Play Services Cast framework (`CastContext`, `SessionManager`) and `androidx.mediarouter` | [`GoogleCastDevices.kt`](src/main/kotlin/com/stash/app/cast/GoogleCastDevices.kt) — same class and `@Inject` constructor; reports no Cast devices and `CastConnection.Unavailable`, which upstream already handles for phones without Play Services |
| `app/.../cast/GoogleCastRemote.kt` | Play Services Cast `RemoteMediaClient`; used only by `GoogleCastDevices` | none needed (its only caller is the replacement above) |
| `app/.../cast/StashCastOptionsProvider.kt` | Play Services `OptionsProvider`, registered in the Android manifest | none needed (no manifest on desktop) |

---

## 4. Shadowed Library Classes (`StripRoomMigration` Artifact Transform)
Five classes in multiplatform/JVM dependency jars differ slightly from their Android counterparts that upstream expects. [`desktop/build.gradle.kts`](build.gradle.kts) strips the original `.class` entries from those library jars (both on Gradle classpaths and inside packaged `Stash.app/Contents/app/*.jar`) and provides binary-compatible supersets in `desktop/src/main/kotlin`:

| Class | Library Jar | Why Shadowed | Desktop Superset |
|---|---|---|---|
| `androidx.room.migration.Migration` | `room-runtime-jvm 2.7.1` | JVM artifact omits `open fun migrate(db: SupportSQLiteDatabase)`, which all 37 upstream Room migrations override | [`Migration.kt`](src/main/kotlin/androidx/room/migration/Migration.kt) bridges `migrate(SQLiteConnection)` to `migrate(SupportSQLiteDatabase)` |
| `coil3.PlatformContext` | `coil-core-jvm 3.4.0` | On Android, `PlatformContext` is a `typealias` for `android.content.Context`; on JVM it is `abstract class PlatformContext private constructor()` | [`PlatformContext.kt`](src/main/kotlin/coil3/PlatformContext.kt) opens constructor so `android.content.Context` subclasses `PlatformContext` directly |
| `androidx.navigation.serialization.NavTypeConverter_nonAndroidKt` | `navigation-common-desktop 2.9.2` | Multiplatform `nonAndroid` stub returns `UNKNOWN` for `SerialKind.ENUM` route fields (`SearchAlbumRoute.source: AlbumSource`) | [`NavTypeConverter.nonAndroid.kt`](src/main/kotlin/androidx/navigation/serialization/NavTypeConverter.nonAndroid.kt) resolves JVM `Enum` classes via `Class.forName` |
| `androidx.room.InvalidationTracker` | `room-runtime-jvm 2.7.1` | The JVM class lacks the Android `Observer` / `addObserver` / `removeObserver` API that upstream's web-link mirror uses | [`InvalidationTracker.java`](src/main/java/androidx/room/InvalidationTracker.java) — the 2.7.1 class plus `Observer`s run off `createFlow` in the database's scope |
| `androidx.navigation.NavHostController` | `navigation-runtime-desktop 2.9.2` | Multiplatform `NavDestination.route` returns short class names (`HomeRoute`) while Android returns FQN (`com.stash.app.navigation.HomeRoute`), breaking top-level tab switching when `launchSingleTop` / `popUpTo` checks `route?.contains("HomeRoute")` | [`NavHostController.kt`](src/main/kotlin/androidx/navigation/NavHostController.kt) normalizes `popUpTo` route strings and resets sub-routes when re-selecting the active bottom bar tab |

---

## 5. Key Desktop Subsystems

### 5.1 Spotify Authentication Bridge (`DesktopNetworkModule`)
Spotify's mobile `clienttoken.spotify.com` endpoint and TOTP parameters require Android/iOS client attestation and reject desktop requests when validating an `sp_dc` cookie. [`DesktopNetworkModule.kt`](src/main/kotlin/com/stash/desktop/di/DesktopNetworkModule.kt) attaches `DesktopSpotifyWebPlayerBridge` as an OkHttp application interceptor on `@AuthenticatedClient`:
- Short-circuits `POST https://clienttoken.spotify.com/v1/clienttoken` with a synthetic response so `SpotifyTotpManager` does not block sync.
- Intercepts `GET https://open.spotify.com/api/token` and exchanges the user's `sp_dc` cookie via Spotify's `server-time` + `get_access_token` endpoint, falling back to `open.spotify.com` HTML session extraction or Partner GraphQL verification if needed.

### 5.2 Streaming & Audio Playback Engine (`ExoPlayer`, `DefaultHttpDataSource`, `MediaSessionShims`)
- **Chunked Range Streaming:** YouTube's `googlevideo.com` CDN throttles open-ended HTTP streams (`Range: bytes=0-`) to ~64 KB/s, which starves high-bitrate streams and causes mid-song EOFs. [`DefaultHttpDataSource.kt`](src/main/kotlin/androidx/media3/datasource/DefaultHttpDataSource.kt) automatically splits open-ended `googlevideo.com` requests into sequential **2 MB** HTTP Range requests (`bytes=start-end`), achieving full broadband download speed.
- **Mid-Stream URL Refresh:** [`ExoPlayer.kt`](src/main/kotlin/androidx/media3/exoplayer/ExoPlayer.kt) (`StashExoDownloader`) catches mid-stream HTTP 403 / connection resets and re-invokes `dataSource.open(resumeSpec)` at `position = streamBytesWritten`, triggering upstream's `RefreshingDataSource` to mint a fresh CDN URL seamlessly without interrupting playback.
- **Real-Time PCM & 5-Stage DSP:** `ExoPlayer` decodes audio via `ffmpeg` (`s16le`, 44.1 kHz, 2 channels) and feeds 4096-byte PCM frames through upstream's `PreampProcessor` → `EqProcessor` → `BassShelfProcessor` → `LoudnessGainProcessor` → `SoftClipLimiterProcessor` before writing to `javax.sound.sampled.SourceDataLine`.
- **Service Idle-Stop Reconnection:** [`MediaSessionShims.kt`](src/main/kotlin/androidx/media3/session/MediaSessionShims.kt) clears `DesktopMediaServiceRegistry.activeService` / `activeSession` when `StashPlaybackService`'s 5-minute idle timer stops the service, and `MediaController.isConnected` reflects `!session.isReleased` so clicking a track after idle-stop transparently spins up a fresh `StashPlaybackService` session.

### 5.3 Native macOS `.app` & `.dmg` Packaging
- [`desktop/build.gradle.kts`](build.gradle.kts) parses `applicationId` (`com.stash.app`), `versionName` (`0.9.122`), `versionCode` (`158`), and `app_name` (`Stash`) directly from `app/build.gradle.kts` and `app/src/main/res/values/strings.xml`.
- The `generateMacIcon` task composites `app/src/main/res/drawable/ic_launcher_background.xml` (`#111111`) and `app/src/main/res/drawable/ic_launcher_foreground.png` into a macOS squircle `.iconset` (`16x16` through `512x512@2x`) and compiles `Stash.icns` via `/usr/bin/iconutil`.
- `createDistributable` builds `desktop/build/compose/binaries/main/app/Stash.app`, patches `Contents/Info.plist` with the exact Android `CFBundleShortVersionString` and `CFBundleVersion`, strips the shadowed classes from bundled jars, and ad-hoc signs the bundle with `/usr/bin/codesign --force --deep --sign -`.

### 5.4 Activity, Configuration & Resources
- `Main.kt` launches `MainActivity` the way `ActivityThread` does: `attach` (upstream's `attachBaseContext` applies the Settings › Display size override there), inject, `onCreate`, `onResume`; minimising the window pauses it. `recreate()` (Display size, app language) builds a fresh instance in the same window, handing over saved UI state (nav back stack included) and the window's ViewModels.
- The system `Configuration` is the window's content size in dp, the screen's backing scale as `densityDpi`, and the process locale ([`DesktopDisplay.kt`](src/main/kotlin/com/stash/desktop/res/DesktopDisplay.kt)); the activity's configuration (system + override) is what `LocalConfiguration` and `LocalDensity` provide.
- `generateAndroidResources` turns every module's `res/` into `R` classes plus a classpath index: strings and plurals (with all shipped translations, resolved per locale with CLDR plural rules), drawables, fonts, raw and xml files, the AGP-style `_generated_res_locale_config`, and merged `assets/` (served by `WebViewAssetLoader`).

### 5.5 Upstream Features Without a macOS Equivalent
These compile and run, and land on the path upstream already takes on an Android device that lacks the capability:
- **Google Cast** — no devices are discovered (see §3).
- **Google Drive (Accounts & Sync)** — `GOOGLE_DRIVE_ENABLED=false`; Play Services `Identity` reports `API_UNAVAILABLE`. Dropbox / OneDrive work when their keys are supplied in `local.properties`.
- **Your Record's WebView card** — `WebViewFeature.isFeatureSupported` is false, so upstream shows its "WebView too old" notice; the Mixes tab works.
- **QR scanning (web link pairing)** — CameraX reports no camera provider; pair by typing the code.
- **In-app APK updater** — there is no APK to install; update the macOS app from the releases page.
- **USB hi-res / bit-perfect output** — macOS routes to CoreAudio's default device as a shared mixer, so `HiResOutputController` stays on the shared-mixer path.

---

## 6. Complete Android → JVM Shim Ledger
| Shim Package / Class | Backing Implementation |
|---|---|
| `android.util.Log` | Standard error (`System.err`), configurable via `-Dstash.log.level` |
| `android.util.Base64` | `java.util.Base64` with full Android flag semantics (`NO_WRAP`, `URL_SAFE`, `NO_PADDING`, 76-char line wrap) |
| `android.content.Context` / `Application` / `Activity` / `Service` | Maps app storage to `~/Library/Application Support/Stash` and `~/Library/Caches/Stash` |
| `android.content.SharedPreferences` | [`DesktopSharedPreferences.kt`](src/main/kotlin/android/content/DesktopSharedPreferences.kt) — atomic XML/properties persistence under `shared_prefs/` with listener dispatch |
| `androidx.activity.ComponentActivity` / `setContent` / `BackHandler` | Hosts `Lifecycle`, `ViewModelStore`, `SavedStateRegistry`, `ActivityResultRegistry`, and bridges Compose root content to Compose Desktop's `Window` |
| `dagger.hilt.android.*` / `androidx.hilt.*` | `@ApplicationContext`, `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@HiltWorker`, `HiltWorkerFactory`, `hiltViewModel()` backed by Dagger 2 KSP |
| `androidx.datastore.preferences.*` | `PreferenceDataStoreFactory.create` using Android's exact path (`files/datastore/<name>.preferences_pb`) |
| `com.google.crypto.tink.integration.android.AndroidKeysetManager` | JVM Tink AEAD with master keyset stored in the macOS login Keychain (`security` CLI, service `com.stash.desktop`) |
| `androidx.sqlite.db.*` & `android.database.sqlite.SQLiteDatabase` | Backed by `androidx.sqlite.driver.bundled.BundledSQLiteDriver` (`libsqliteJni.dylib`) and materialized `Cursor`s |
| `androidx.room.withTransaction` / `Room.databaseBuilder` | `useWriterConnection { immediateTransaction { } }` and `DesktopRoom.builder` (`BundledSQLiteDriver`) |
| `android.graphics.Bitmap` / `BitmapFactory` / `Color` / `Palette` | `java.awt.image.BufferedImage` + `javax.imageio.ImageIO` + AOSP `Palette` & `ColorUtils` |
| `coil3.toBitmap()` / `ImageRequest.Builder.allowHardware` | Converts Skiko `coil3.Image` to `android.graphics.Bitmap` for `ColorExtractor` |
| `com.yausername.youtubedl_android.YoutubeDL` / `FFmpeg` | Executes macOS `yt-dlp` (auto-updatable `yt-dlp_macos` in `~/Library/Application Support/Stash/yt-dlp/`) and `ffmpeg` |
| `android.media.MediaExtractor` / `MediaMetadataRetriever` / `AudioManager` | `ffprobe` JSON stream inspection, `ffmpeg` cover art extraction, and in-process audio focus arbitration |
| `androidx.media3.datasource.*` / `SimpleCache` | `OkHttpDataSource`, `FileDataSource`, `DefaultHttpDataSource` (2 MB Range chunking), `CacheDataSource`, and disk-backed `SimpleCache` |
| `androidx.media3.exoplayer.*` / `DefaultAudioSink` / `AudioOutputProvider` | `ffmpeg` PCM decoder → media3 1.9 `AudioOutput` seam (upstream's float DSP) → `AudioTrack` shim → `javax.sound.sampled.SourceDataLine`; HLS / DASH are demuxed by `ffmpeg`, which reads the manifest and every segment through a loopback proxy ([`DesktopManifestProxy.kt`](src/main/kotlin/androidx/media3/exoplayer/source/DesktopManifestProxy.kt)) backed by the source's own `DataSource`, so upstream's Download-mode gate and manifest refresh apply |
| `androidx.media3.common.SimpleBasePlayer` / `ForwardingSimpleBasePlayer` / `Timeline` | Ports of the media3 1.9 classes (upstream's crossfade and cast players build on them) |
| `androidx.media3.session.*` | In-process `MediaSession`, `MediaLibraryService`, and `MediaController` connecting `PlayerRepositoryImpl` to `StashPlaybackService` |
| `androidx.work.*` | Coroutine-backed `WorkManager` supporting periodic/one-time work, backoff policies, tags, unique work chains, and `ListenableWorker` |
| `android.webkit.WebView` / `CookieManager` / `AndroidView` | Cookie store synchronized with `DesktopCookiePersistence` + embedded Swing/system browser auth window |

---

## 7. Runtime Data & Privacy Guarantees
All runtime state is stored strictly outside the repository under standard macOS user directories:
- **Application Support (`~/Library/Application Support/Stash/`):**
  - `databases/stash.db` — Room SQLite database
  - `files/datastore/stash_preferences.preferences_pb` — Preferences DataStore
  - `shared_prefs/` — Encrypted Tink keysets & SharedPreferences
  - `files/Stash/` — Downloaded offline tracks (`Offline mode`)
  - `yt-dlp/yt-dlp_macos` — Auto-updated `yt-dlp` binary
  - `native_libs/` — Symlinks to `ffmpeg` and `qjs`
- **Caches (`~/Library/Caches/Stash/`):**
  - `exo_stream_cache/` — Streaming audio cache
  - `image_cache/` — Coil album & artist artwork cache
- **macOS Keychain:**
  - Service `com.stash.desktop` stores the Tink keyset used to AES-256-GCM encrypt Spotify, YouTube, and Discord credentials.
