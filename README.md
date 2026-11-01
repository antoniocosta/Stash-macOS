# Stash for macOS

> **Your Spotify + YouTube Music library, on your Mac. In FLAC, if you bring the source.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-purple.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-macOS%20(Apple%20Silicon)-purple)](#requirements)
[![Upstream](https://img.shields.io/badge/upstream-rawnaldclark%2FStash-purple)](https://github.com/rawnaldclark/Stash)

Stash mirrors your Spotify and YouTube Music libraries to your Mac. You connect each service, pick the playlists and mixes you want, and Stash either downloads them for offline playback — as real FLAC files once you've connected a lossless source — or surfaces them as a streaming index so you can stream tracks without filling up your storage. Same library, two modes, one click to switch.

There's no Stash account. No subscription. No ads. No analytics. Your credentials live on your Mac — the Spotify, YouTube, and Discord tokens encrypted with AES-256-GCM and backed by the macOS Keychain, the rest in your user Library folder — and each one is only ever sent back to the service it came from. Spotify and YouTube aren't the only hosts Stash talks to, though — lyrics, scrobbling, artist metadata, Discord presence, shared mixes, and lossless all have their own. [The full list is below](#what-stash-talks-to).

If you like this or find it useful, support the project by buying me a coffee.

<a href="https://www.buymeacoffee.com/antoniocosta" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="height: 40px !important;width: 140px !important;" ></a>

### About this port

This repository is a native **macOS (Apple Silicon)** desktop port of **[Stash](https://github.com/rawnaldclark/Stash)**, created and maintained by **[Rawnald Clark (`@rawnaldclark`)](https://github.com/rawnaldclark)** and **[`@ParaliyzedEvo`](https://github.com/ParaliyzedEvo)**.

It is built as a **pure, zero-touch port**: all 23 upstream Android modules (`core/*`, `data/*`, `feature/*`, `app`) compile directly from their original source directories without modifying a single upstream code file. Every macOS platform shim, audio DSP pipeline bridge, and `.app` / `.dmg` packaging script lives strictly inside [`desktop/`](desktop/). See [`desktop/PORTING.md`](desktop/PORTING.md) for the technical architecture.

---

## Download or stream

Stash has two ways to hold your library, and you can mix them.

**Download** — turn it on in the Sync tab and a sync saves each song to your Mac, so it plays with no connection. Download mode plays your downloads and hides anything that isn't downloaded. It costs storage but no recurring data. Any single playlist also has its own Download button.

**Stream** — leave Download off and your library is a streamable index: almost no storage, but you need a connection to play.

Either way you pick the quality: **Lossless**, **AAC 320** or **Opus**. Whether a song lands as FLAC depends on the lossless source you've connected — see [Lossless](#lossless).

---

## Lossless

**This macOS build ships no shared accounts and no relay.** Upstream's official Android release can get FLAC links through the project's own relay; a build from source, which is what this port is, has no relay config, so FLAC comes from a source *you* own:

- **Your own Qobuz account** — connect it in Settings › Audio & Quality. Connecting sends your Qobuz email and password to Qobuz once to get a token; Stash keeps the token, not the password. It then streams and downloads from your own subscription.
- **Your own relay endpoint** — if you run one, paste its URL into the custom-endpoint field and Stash routes through it.

Connect neither and Stash still works: songs play and download as AAC 320 or Opus instead, and Now Playing tells you where each one came from.

---

## Features

### Your library, one place

- **Spotify + YouTube Music together** — liked songs, playlists, daily mixes
- **Your listening, wrapped** — top songs, stats, and a recap of your month; **Your record** brings every year you've listened together, from Spotify, Last.fm or Stash
- **Refresh or Accumulate** — when a song leaves a playlist on the service, remove it here too, or keep it
- **Wrong-match flag** — if Stash picked the wrong version, flag it from Now Playing and it searches again
- **Likes and history mirroring** — optional: what you like and play in Stash lands in your Spotify and YouTube Music accounts

### Sound

- **Real FLAC files, up to Hi-Res** (with a lossless source, see above). Downloads keep their full resolution; playback on the Mac currently outputs 44.1 kHz stereo
- **5-band EQ** with presets, preamp, bass boost and a soft limiter, running in real time on 32-bit float audio
- **Crossfade** and a **loudness normalizer**
- **Synced lyrics** that light up word by word, pulled automatically from Apple Music (via paxsenix), LRCLIB, KuGou and YouTube Music

### Finding new stuff

- **Daily Discover, Stash Mixes and radios**
- **Last.fm recommendations** — a rotating "Recommended by Last.fm" playlist under Sync › Sources
- **Shared mixes and song links** — share a song or a mix as a link for friends to open in Stash (opening one on the Mac isn't wired up yet, see below)
- **Community** — public playlists and songs other people post, with votes. Off until you turn it on in Home layout

### Social

- **Listen Together** — in sync with friends
- **Last.fm and ListenBrainz scrobbling** — optional, off by default
- **Discord Rich Presence** — what you're playing, art and all, as your Discord status. Optional, and disconnecting clears it immediately
- **Link Stash on the web** — pick up on another device where you left off, end-to-end encrypted (Settings › Library & Storage)

Dark, light and pure black themes, a Display size setting, and 14 languages (English, Arabic, Chinese, Filipino, French, German, Hindi, Korean, Marathi, Persian, Polish, Portuguese, Russian, Spanish). Everything optional is off until you turn it on.

### Not available on macOS

These upstream features depend on Android, Google Play services or Android app links. They're in the app, but on a Mac they take the path Stash already takes on a phone without that capability:

- **Casting** to Chromecast / Nest speakers — no speakers are found
- **Your music in Dropbox, OneDrive or Google Drive** — Google Drive needs Google Play services, and the Dropbox / OneDrive sign-in returns to the app through a link the macOS app doesn't handle yet (a plain build hides all three)
- **Opening Stash links** — a shared-mix or song link opens in your browser rather than in Stash, so following a friend's mix needs the phone app for now; sharing your own works
- **Scanning a QR code** to link Stash on the web — there's no camera support, so type the code instead
- **The in-app updater** — update from the [Releases page](https://github.com/antoniocosta/Stash-macOS/releases)
- **Bit-perfect USB DAC output** — audio goes to macOS's current output device through the system mixer
- **Your record's web card** — the app shows its "WebView too old" notice; the Mixes tab works

### Privacy

- No Stash account, no analytics, no third-party crash reporters.
- Your logins are stored on your Mac, encrypted with AES-256-GCM via Google's [Tink](https://developers.google.com/tink), with the master keyset in your macOS login Keychain.
- Each login is only ever sent back to the service it came from.
- GPL-3.0, every line of code is open source.

---

## What Stash talks to

Everything a source build of Stash for macOS can reach, and what for. It's upstream's [PRIVACY.md](PRIVACY.md#what-stash-talks-to) list minus what this build doesn't have: the project's lossless relay and its config, the Last.fm proxy, cloud storage (Dropbox, OneDrive, Google Drive) and Cast. PRIVACY.md has the full detail on what each host is sent and keeps.

- **Spotify** (`accounts.`, `open.`, `api-partner.`, `api.spotify.com`, `www.spotify.com`, `clienttoken.spotify.com`) — sign-in and library sync; optionally, mirroring your likes and saving songs you play often to Liked Songs
- **YouTube + YouTube Music** (`music.youtube.com`, `www.youtube.com`, `m.youtube.com` for the sign-in window, and Google's OAuth endpoints if you use the Google sign-in) — library sync, search, artist and album pages, artist photos, and the audio itself via InnerTube / `yt-dlp`; optionally, mirroring likes and play history
- **`*.googlevideo.com`** — YouTube's audio CDN; the URL comes back in the player response
- **Google OAuth** (`oauth2.googleapis.com`) — only if you use the YouTube device-code sign-in
- **The sign-in windows** — the Spotify, YouTube and Discord sign-in pages load what they need from those services' own hosts (and Google reCAPTCHA); Stash itself doesn't call them
- **GitHub** (`api.github.com`, `github.com`, `release-assets.githubusercontent.com`) — the update check, and `yt-dlp` updating its own `yt-dlp_macos` nightly binary every 24 hours
- **SoundCloud** (`api-v2.soundcloud.com`, `api-auth.soundcloud.com`, and its `*.sndcdn.com` audio CDN) — a streaming source, and your SoundCloud library if you connect it
- **Qobuz — catalog** (`www.qobuz.com`, and `open.qobuz.com` to refresh its public web-player key) — the New Releases, Top Albums and Qobuz Playlists rows on Home (**on by default**; turn off "Qobuz discovery on Home" in Settings › Library & Storage), and albums YouTube is missing on artist pages
- **Qobuz — lossless** (`www.qobuz.com`, and its audio CDN, today `streaming-qobuz-std.akamaized.net`) — while Lossless is on (the default), each song about to play or download is looked up in Qobuz's catalog first, account or not, so Qobuz learns what you play; turn Lossless off in Settings › Audio & Quality to stop that. FLAC itself comes only through your own account or relay endpoint
- **JioSaavn** (`www.jiosaavn.com`, `aac.saavncdn.com`) — the AAC 320 fallback when nothing lossless matched
- **Lyrics** (`itunes.apple.com` + `lyrics.paxsenix.org`, `lrclib.net`, `mobileservice.kugou.com` + `lyrics.kugou.com`, and `music.youtube.com` as a last resort) — word-synced and line-synced lyrics
- **Last.fm** (`ws.audioscrobbler.com`, `www.last.fm` for sign-in) — scrobbling and loved tracks if you connect it; artist bios, tags and the lookups behind Stash Mixes and radios when the build has Last.fm keys in `local.properties`
- **ListenBrainz** (`api.listenbrainz.org`) — optional scrobbling, only if you connect it
- **Discord** (`discord.com`, `cdn.discordapp.com`) — optional Rich Presence, only if you connect your account
- **MusicBrainz** (`musicbrainz.org`) — artist metadata
- **The share Worker** (`stashfm.app`, or `stash-share.rawnaldclark.workers.dev`) — only when you share a mix or a short song link, check mixes you follow, or use Listen Together or Community
- **The sync Worker** (`sync.stashfm.app`) — only after you link Stash on the web; what it stores is end-to-end encrypted
- **`stash-tipjar.rawnaldclark.workers.dev`** — the public supporters list behind the Home supporter pill. Fetch only; it's told nothing about you
- **Album art and artist photos** — straight from Spotify (`i.scdn.co`, `mosaic.scdn.co`, `*.spotifycdn.com`), YouTube (`i.ytimg.com`, `lh3.googleusercontent.com`, `yt3.googleusercontent.com`, `yt3.ggpht.com`), Last.fm (`lastfm.freetls.fastly.net`, `lastfm-img.freetls.fastly.net`), Qobuz (`static.qobuz.com`), JioSaavn (`c.saavncdn.com`) and, for some song links, Deezer (`cdn-images.dzcdn.net`, `e-cdns-images.dzcdn.net`)

Some of this runs without being asked: at every start, a YouTube visitor id and warm-up, the update checks, a `yt-dlp` test run on one public video, missing album art for downloads, and artist photos for your library; while Stash Mixes is on (the default), lookups of what you've played most. PRIVACY.md lists exactly when each runs.

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
   xattr -dr com.apple.quarantine /Applications/Stash.app
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
- A Spotify and/or YouTube Music account to sync your library (searching YouTube Music and streaming what you find works without one)

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

Open the **Sync** tab, pick the playlists and mixes you want, and click **Sync Now**. Choose what happens when a song leaves a playlist on the service — **Refresh** removes it here too, **Accumulate** keeps it — and turn **Download** on if you want songs saved to your Mac.

The first sync is the slow one (a big library with Download on can take an hour). After that it only grabs what's new. Turn on **Auto-sync** and forget about it.

---

## Port Architecture & Staying in Sync with Upstream

All macOS port code lives inside [`desktop/`](desktop/):
- **[`desktop/README.md`](desktop/README.md)** — Build, run, packaging, and test commands
- **[`desktop/PORTING.md`](desktop/PORTING.md)** — Complete technical ledger of the Android → JVM shims, Dagger KSP wiring, `DesktopSpotifyWebPlayerBridge`, HTTP Range streaming, the audio engine (ffmpeg decoding into upstream's float DSP), and the upstream features without a macOS equivalent

Because zero files outside `desktop/` are modified, pulling new upstream releases from `rawnaldclark/Stash` is a clean rebase:

```bash
git fetch upstream
git rebase upstream/master
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  sh ./gradlew -p desktop test
# add -Pstash.networkTests=true to also stream a YouTube Music search result
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

Stash is an independent, unofficial project, and this macOS port is an independent port of it. Neither is **affiliated with, endorsed by, or sponsored by Spotify, YouTube, Google, Apple, Qobuz, Tidal, Deezer, SoundCloud, Discord, Last.fm, ListenBrainz, MusicBrainz, JioSaavn, KuGou, LRCLIB, Dropbox, Microsoft, or any other service it connects to.** Third-party names and logos are used only to identify the services they belong to, and all trademarks belong to their respective owners.

Stash is a tool for managing your own music library. You're responsible for complying with the Terms of Service of any music service you use Stash with. Downloading copyrighted content without a license may be illegal in your jurisdiction. The Stash project accepts no responsibility for misuse.

Takedown or abuse reports about the app itself go to the upstream project (see its [README](https://github.com/rawnaldclark/Stash#takedown-and-abuse-reports)); reports about this port go to this repository's [Issues](https://github.com/antoniocosta/Stash-macOS/issues).

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
- **[Bungee Shade](https://fonts.google.com/specimen/Bungee+Shade)** — the wordmark font, by David Jonathan Ross (SIL OFL)
- **Discord logo** from Simple Icons (CC0)

---

## License

Copyright © 2026 Rawnald Clark (upstream Stash application)  
macOS desktop port (`desktop/`) released under the same license.

Stash is free software: you can redistribute it and/or modify it under the terms of the [GNU General Public License](LICENSE), either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but **WITHOUT ANY WARRANTY**; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [LICENSE](LICENSE) file for the full text.
