# Stash for macOS

> **Your Spotify + YouTube Music library, on your Mac. In FLAC, if you bring the source.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-purple.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-macOS%20(Apple%20Silicon)-purple)](#requirements)
[![Upstream](https://img.shields.io/badge/upstream-rawnaldclark%2FStash-purple)](https://github.com/rawnaldclark/Stash)

Stash mirrors your Spotify and YouTube Music libraries to your Mac. You connect each service, pick the playlists and mixes you want, and Stash either downloads them for offline playback — as real FLAC files once you've connected a lossless source — or surfaces them as a streaming index so you can stream tracks without filling up your storage. Same library, two modes, one click to switch.

There's no Stash account. No subscription. No ads. No analytics. Your credentials live on your Mac — the Spotify, YouTube, and Discord tokens encrypted with AES-256-GCM and backed by the macOS Keychain, the rest in your user Library folder — and each one is only ever sent back to the service it came from. Spotify and YouTube aren't the only hosts Stash talks to, though — lyrics, scrobbling, artist metadata, Discord presence, shared mixes, and lossless all have their own. [The full list is below](#what-stash-talks-to).

### About this port

This repository is a native **macOS (Apple Silicon)** desktop port of **[Stash](https://github.com/rawnaldclark/Stash)**, created and maintained by **[Rawnald Clark (`@rawnaldclark`)](https://github.com/rawnaldclark)** and **[`@ParaliyzedEvo`](https://github.com/ParaliyzedEvo)**.

It is built as a **pure, zero-touch port**: all 18 upstream Android modules (`core/*`, `data/*`, `feature/*`, `app`) compile directly from their original source directories without modifying a single upstream code file. Every macOS platform shim, audio DSP pipeline bridge, and `.app` / `.dmg` packaging script lives strictly inside [`desktop/`](desktop/). See [`desktop/PORTING.md`](desktop/PORTING.md) for the technical architecture.

---

## Online vs Offline

Stash has two modes. They decide what a sync actually does.

**Offline mode** Sync downloads each track and stores it on your Mac. Once a track is on disk you can play it forever with no connection. It costs storage but no recurring data. Whether a download lands as FLAC depends entirely on the lossless source you've connected — see [Lossless](#lossless). Without one you get the AAC/Opus fallback, or nothing at all if you've turned that fallback off.

**Online mode** is for people who don't want the storage hit. Builds a streamable local index of your library — almost no storage, but you need a connection to play.

---

## Lossless

**Stash ships no shared user accounts.** There's no bundled Qobuz account and no shared token pool — nothing in the app that plays music on someone else's subscription. FLAC comes from a source *you* own, and you pick which:

- **Your own Qobuz account** — connect it in Settings › Audio & Quality. Connecting asks for your Qobuz email and password, which are sent once to Qobuz to mint a token; Stash stores the token, not the password. It then streams and downloads from your own subscription, the same catalog your Qobuz app sees.
- **Your own relay endpoint** — if you run a Qobuz relay, paste its URL into the custom-endpoint field and Stash routes through it.

Connect neither of them and Stash still works — playback and downloads fall back to AAC or Opus, and Home tells you so instead of pretending.

---

## Features

### Library

- **Spotify + YouTube Music in one unified library** — liked songs, playlists, daily mixes, every Spotify mix worth syncing
- **Bulletproof matching** — finds the right version of a track 99% of the time
- **Shared mixes** — share a mix link or follow a friend's mix with live updates
- **Last.fm scrobbling**, optional, off by default
- **Last.fm recommendations** — connect Last.fm and it appears under Sync → Sources, keeping a rotating "Recommended by Last.fm" playlist of tracks it suggests from your listening
- **Wrong-match flag** — if Stash picked the wrong version, click once from Now Playing and it queues a re-search
- **Likes and History mirroring** — when enabled, each track you like & stream in Stash lands in your Spotify & YouTube accounts

### Playback

- **5-band parametric equalizer** with presets, preamp, bass shelf boost, and soft-clip limiter running in real time on 16-bit PCM audio
- **Crossfader**
- **Loudness normalizer**
- **Word-synced & line-synced lyrics** — pulled from Apple Music (via Paxsenix), LRCLIB, and KuGou, scrolled in real time with the track

### Discord

- **Rich Presence** — show what you're listening to as your Discord status, art and all. Optional, off by default, and disconnecting clears it immediately.

### Privacy

- No Stash account server, no login, no analytics, no third-party crash reporters.
- Cookies stored locally on your Mac, encrypted with AES-256-GCM via Google's [Tink](https://developers.google.com/tink) with the master keyset stored in your macOS Login Keychain.
- Your Spotify, YouTube, and Discord credentials are sent to Spotify, YouTube, and Discord respectively and nowhere else.
- GPL-3.0, every line of code is open source.

---

## What Stash talks to

Everything a source build of Stash for macOS can reach, and what for:

- **Spotify** (`accounts.`, `open.`, `api-partner.`, `api.spotify.com`, `www.spotify.com`, `clienttoken.spotify.com`) — login, library sync, likes and history mirroring
- **YouTube + YouTube Music** (`music.youtube.com`, `www.youtube.com`, and Google's OAuth endpoints if you use the Google sign-in) — library sync, and the audio itself via InnerTube / `yt-dlp`
- **`*.googlevideo.com`** — YouTube's audio CDN; the URL comes back in the player response
- **Google OAuth** (`oauth2.googleapis.com`) — only if you use the YouTube device-code sign-in
- **yt-dlp** (`api.github.com`, `github.com`, `objects.githubusercontent.com`) — updates its own `yt-dlp_macos` nightly binary every 24 hours
- **Qobuz — catalog** (`www.qobuz.com`, and `open.qobuz.com` to refresh its public web-player key) — the New Releases, Top Albums and Qobuz Playlists rows on Home. No account needed and **on by default**; turn it off with "Qobuz discovery on Home" in Settings › Library & Storage, or hide those rows in Home layout.
- **Qobuz — lossless** (`www.qobuz.com`) — FLAC streams and downloads, only once you connect your own account
- **`stash-share.rawnaldclark.workers.dev`** (`stash.rawnaldclark.com`) — only when you share a mix, open a shared-mix link, or follow a shared mix
- **JioSaavn** (`www.jiosaavn.com`, `aac.saavncdn.com`) — the AAC 320 fallback when nothing lossless matched
- **Lyrics** (`itunes.apple.com` + `lyrics.paxsenix.org`, `lrclib.net`, `lyrics.kugou.com`) — word-synced (TTML) and line-synced (LRC) lyrics
- **Last.fm** (`ws.audioscrobbler.com`) — optional scrobbling, plus artist bios and images (when configured via `local.properties`)
- **ListenBrainz** (`api.listenbrainz.org`) — optional scrobbling, only if you connect it
- **Discord** (`discord.com`, `cdn.discordapp.com`) — optional Rich Presence, only if you connect your account
- **MusicBrainz** (`musicbrainz.org`) — artist metadata
- **GitHub** (`api.github.com`) — the update check
- **`stash-tipjar.rawnaldclark.workers.dev`** — the public supporters list behind the Home supporter pill. Fetch only; it's told nothing about you.
- **Album art CDNs** — `i.scdn.co`, `lh3.googleusercontent.com`, `yt3.googleusercontent.com`, `yt3.ggpht.com`, `i.ytimg.com`, `static.qobuz.com`, `c.saavncdn.com`

---

## Install

### Option 1 — Download `Stash.dmg` (Recommended)

1. Install the required open-source audio & extraction tools using [Homebrew](https://brew.sh):
   ```bash
   brew install ffmpeg yt-dlp quickjs
   ```
2. Open the [Releases page](https://github.com/antoniocosta/Stash-macOS/releases) and download the latest **`Stash.dmg`**.
3. Open `Stash.dmg` and drag **`Stash.app`** into your **`Applications`** folder.
4. *(First launch only)* Because `Stash.app` is ad-hoc signed rather than Apple Developer notarized, clear the macOS quarantine attribute before opening it the first time:
   ```bash
   xattr -cr /Applications/Stash.app
   ```

### Option 2 — Build from source

```bash
brew install openjdk@17 ffmpeg yt-dlp quickjs
git clone https://github.com/antoniocosta/Stash-macOS.git
cd Stash-macOS

# Launch directly from source
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop run

# Or build the standalone Stash.app bundle and Stash.dmg installer
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop packageDmg
# Stash.app -> desktop/build/compose/binaries/main/app/Stash.app
# Stash.dmg -> desktop/build/compose/binaries/main/dmg/Stash.dmg
```

### Requirements

- **macOS 13.0 (Ventura)** or later on **Apple Silicon (M1 / M2 / M3 / M4)**
- `ffmpeg`, `yt-dlp`, and `quickjs` installed via Homebrew
- About **9–15 GB** of free disk space for a medium library in Offline mode (scales with how much you sync). Online mode needs almost nothing.
- A Spotify and/or YouTube Music account

### Where Stash stores data on your Mac

All runtime files live in standard macOS user directories — never inside the repository:

| Location | Contents |
|---|---|
| `~/Library/Application Support/Stash/databases/stash.db` | Room SQLite library database |
| `~/Library/Application Support/Stash/files/datastore/` | Preferences DataStore |
| `~/Library/Application Support/Stash/shared_prefs/` | AES-256-GCM encrypted credentials |
| `~/Library/Application Support/Stash/files/Stash/` | Downloaded offline tracks (FLAC / AAC / Opus) |
| `~/Library/Application Support/Stash/yt-dlp/` | Auto-updated `yt-dlp_macos` binary |
| `~/Library/Caches/Stash/` | Streaming audio cache & album artwork cache |
| **macOS Login Keychain** (`com.stash.desktop`) | Master Tink AEAD keyset |

---

## First-time setup

Stash doesn't use Spotify's or YouTube's official APIs, because the official APIs don't let third-party apps do what Stash does. It uses your existing login cookies instead. Cookies live only on your Mac, encrypted with AES-256-GCM, and the only place they ever get sent is back to Spotify or YouTube themselves.

<details>
<summary><b>🎵 Connect Spotify</b></summary>

1. Open **[https://open.spotify.com](https://open.spotify.com)** in your browser and make sure you're logged in.
2. Open Developer Tools (**Cmd+Option+I** or **F12**).
3. Go to the **Application** tab (or **Storage** in Firefox/Safari).
4. In the left sidebar, expand **Cookies** → click `https://open.spotify.com`.
5. Find the cookie named **`sp_dc`**, double-click its value, and copy it.
6. In Stash on your Mac, go to **Settings** → **Spotify** → **Connect** → **"Paste cookie"**.
7. Paste the `sp_dc` value and click **Connect**.

> **Tip:** cookies from incognito / private windows expire as soon as you close the window. Use a regular browser window.

</details>

<details>
<summary><b>📺 Connect YouTube Music</b></summary>

1. Open **[https://music.youtube.com](https://music.youtube.com)** in your browser and make sure you're logged in.
2. Open Developer Tools (**Cmd+Option+I** or **F12**) and click the **Network** tab.
3. Refresh the page (**Cmd+R**).
4. In the filter box, type **`browse`** and click any `browse` request.
5. In **Request Headers**, find the **`cookie:`** header and copy the entire value after `cookie:`.
6. In Stash on your Mac, go to **Settings** → **YouTube Music** → **Connect**, paste the cookie string, and click **Connect**.

</details>

<details>
<summary><b>🎮 Connect Discord (Rich Presence)</b></summary>

1. Sign in at **[https://discord.com/login](https://discord.com/login)** in your browser, open DevTools (**Cmd+Option+I**) → **Application** → **Local Storage** → `https://discord.com`, find the `token` key, and copy its value.
2. In Stash on your Mac, go to **Settings** → **Discord** → **Connect** → **Paste token**.
3. Disconnecting (`Settings` → `Discord` → `Disconnect`) clears your status immediately and deletes the stored token from your Mac.

> **Note:** As documented in the upstream project, Discord Rich Presence posts to Discord's headless-session endpoint using your user session token. See the in-app prompt before connecting.

</details>

### After setup

Open the **Sync** tab. Before you click **Sync Now**, expand the **Spotify Sync Preferences** card and pick the playlists and mixes you want — each playlist has its own toggle. For mixes, choose between **Refresh** mode (each sync replaces the mix's contents) and **Accumulate** mode (each sync stacks new tracks on top of what's there).

---

## Port Architecture & Staying in Sync with Upstream

All macOS port code lives inside [`desktop/`](desktop/):
- **[`desktop/README.md`](desktop/README.md)** — Build, run, packaging, and test commands
- **[`desktop/PORTING.md`](desktop/PORTING.md)** — Complete technical ledger of the Android → JVM shims, Dagger KSP wiring, `DesktopSpotifyWebPlayerBridge`, HTTP Range streaming, and 5-stage 16-bit PCM DSP audio engine

Because zero files outside `desktop/` are modified, pulling new upstream releases from `rawnaldclark/Stash` is a clean rebase:

```bash
git fetch upstream
git rebase upstream/master
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop test
```

---

## Credits & Supporting the Upstream Project

**Stash** is created and maintained by **[Rawnald Clark (`@rawnaldclark`)](https://github.com/rawnaldclark)** (owner & lead developer) and **[`@ParaliyzedEvo`](https://github.com/ParaliyzedEvo)** (co-developer).

All credit for Stash's design, sync engine, matching pipeline, lossless integration, lyrics system, and UI belongs to the upstream authors. If Stash is useful to you, please support their work directly:

**rawnaldclark (rawn)** — Owner, main dev of [Stash](https://github.com/rawnaldclark/Stash)<br>
<a href="https://ko-fi.com/rawnald"><img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="Support on Ko-fi" height="36"></a>

**Paraliyzed_evo** — Co-dev<br>
<a href="https://www.paypal.com/paypalme/Paraliyzedevo"><img src="https://img.shields.io/badge/PayPal-Donate-00457C?logo=paypal&logoColor=white" alt="Support on PayPal" height="36"></a>

You can also [sponsor `@rawnaldclark` on GitHub](https://github.com/sponsors/rawnaldclark) or join the [Stash Discord](https://discord.gg/vcbjEby5PC).

---

## Legal disclaimer

Stash is an independent, unofficial project. It is **not affiliated with, endorsed by, or sponsored by Spotify AB, YouTube LLC, Google LLC, Alphabet Inc., or Apple Inc.** All trademarks belong to their respective owners.

Stash is provided **for personal use only** — a tool for managing your own library. You're responsible for complying with the Terms of Service of any music service you use Stash with. Downloading copyrighted content without a license may be illegal in your jurisdiction. The Stash project accepts no responsibility for misuse.

---

## Acknowledgments

- **[Stash (`rawnaldclark/Stash`)](https://github.com/rawnaldclark/Stash)** — the upstream Android application
- **[JetBrains Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)** — desktop UI runtime
- **[yt-dlp](https://github.com/yt-dlp/yt-dlp)** — the YouTube extraction backbone
- **[FFmpeg](https://ffmpeg.org/)** — audio decoding, stream probing, and metadata tagging
- **[QuickJS-NG](https://github.com/quickjs-ng/quickjs)** — lightweight JS engine for YouTube's signature challenges
- **[Room](https://developer.android.com/training/data-storage/room) & [Bundled SQLite](https://developer.android.com/kotlin/multiplatform/sqlite)** — local SQLite persistence
- **[Dagger](https://dagger.dev/)** — compile-time dependency injection
- **[Spicy Lyrics](https://github.com/Spikerko/spicy-lyrics)** & **[paxsenix's lyrics API](https://lyrics.paxsenix.org/)** — word-synced lyrics reference & lookup

---

## License

Copyright © 2026 Rawnald Clark (upstream Stash application)  
macOS desktop port (`desktop/`) released under the same license.

Stash is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](LICENSE), either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but **WITHOUT ANY WARRANTY**; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [LICENSE](LICENSE) file for the full text.
