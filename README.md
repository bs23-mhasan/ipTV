# BDIX TV — Android + Android TV starter

A Kotlin starter for a dual-form-factor IPTV client. It reads the public M3U playlist maintained by `saeidrahmanbd/BDIX-IPTV`, parses channel metadata, displays a responsive channel grid, stores favorites locally, and plays selected streams with AndroidX Media3 ExoPlayer.

> This is a client starter, not a TV streaming server. The repository contains stream references; it does not host the channels. Stream availability, network/ISP reachability, formats, and rights vary by channel.

## Repository integration

Main playlist:
`https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/IPTV-Playlist.m3u`

Companion international playlist:
`https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/International-Playlist.m3u`

XMLTV EPG:
`https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/epg/epg.xml`

The starter currently loads the main playlist. Add a source selector and a second repository call to merge the international list if desired. The EPG URL is documented but XMLTV parsing and programme display are a planned next increment.

## Architecture

```text
UI (Compose phone / Compose for TV)
        |
BdixViewModel (StateFlow UI state)
        |
Use cases / domain models (Channel)
        |
ChannelRepository ---- OkHttp ---- remote M3U
        |
M3uParser
        |
FavoritesStore ---- Preferences DataStore
        |
Media3 ExoPlayer ---- HLS / supported media streams
```

### Package layout

- `domain/Channel.kt` — platform-independent channel model
- `data/M3uParser.kt` — EXTINF metadata and stream URL parser
- `data/ChannelRepository.kt` — remote playlist retrieval
- `data/FavoritesStore.kt` — persistent favorite IDs
- `presentation/BdixViewModel.kt` — state and user actions
- `presentation/HomeScreen.kt` — adaptive modern channel grid
- `presentation/PlayerScreen.kt` — Media3 playback surface

The same app installs on phones and Android TV. TV is detected using `UiModeManager`; the grid uses larger cards and focusable items, and the manifest declares optional Leanback support.

## Open and run

1. Install Android Studio with JDK 17 and Android SDK Platform 36.
2. Extract this ZIP.
3. Open the `BDIX-TV` folder in Android Studio.
4. Allow Gradle sync and install missing SDK components if prompted.
5. Run on an Android phone/emulator or Android TV device/emulator.

If you prefer command line, generate the Gradle wrapper from the project root with an installed Gradle 8.13-compatible distribution, then run:

```bash
gradle wrapper --gradle-version 8.13
./gradlew :app:assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Implementation plan

### Phase 1 — working MVP
- [x] Fetch main M3U playlist over HTTPS
- [x] Parse `#EXTINF` channel metadata: `tvg-id`, `tvg-logo`, `group-title`, display name
- [x] Display channel grid on phone and TV
- [x] Persist favorites across app restarts
- [x] Play selected URL through Media3 ExoPlayer
- [ ] Add search, country/group filters, and recently watched

### Phase 2 — IPTV features
- Add multiple playlist sources and source management
- Add XMLTV parser for `epg.xml`, map `tvg-id` to programme schedules
- Add channel details, now/next programme and full TV guide
- Add stream error classification, retry/backoff, and user-visible diagnostics
- Add channel logo caching and playlist cache/offline last-known-good list

### Phase 3 — TV polish and production
- Add dedicated TV navigation rails, TV Material focus states and remote key handling
- Add next/previous channel controls and number-key channel selection
- Add player lifecycle/background audio policy and playback restoration
- Add unit tests for M3U parsing and repository error cases
- Add UI tests on phone and TV emulator, accessibility and large-screen QA
- Add release signing, privacy policy, attribution, and store compliance review

## Important engineering notes

- The M3U parser is intentionally tolerant, but production playlists may contain multi-line metadata, quoted commas, relative logo paths, unusual attributes, or URL redirects. Expand parser tests against actual repository snapshots.
- ExoPlayer supports HLS when `media3-exoplayer-hls` is present. Other formats/codecs may require additional Media3 modules or may not be supported on a device.
- Some streams may only work from a particular ISP/BDIX network, may be geo-restricted, or may be offline. A playlist entry is not a playback guarantee.
- Add request timeouts, conditional refresh/ETag, bounded playlist size, and a last-known-good cache before production.
- The current starter uses local preference storage for favorites. IDs use `tvg-id` when available and fall back to title + stream URL; this makes identity more stable when the playlist provides IDs.
- Do not proxy, redistribute, record, or expose streams unless you have the required authorization. The repository's MIT license does not grant rights to third-party streams, channel names, logos, trademarks, or programme data.

## Version baseline

The starter uses a conservative Android Gradle/Kotlin baseline and explicit dependency versions. Review the official release notes before upgrading:
- Compose BOM `2025.12.01`
- Compose for TV `tv-material 1.1.0`, `tv-foundation 1.0.0`
- Media3 `1.11.1`
- Android Gradle Plugin `8.13.2`, Kotlin `2.2.20`
- `compileSdk 36`, `minSdk 23`, Java 17

This is a source starter; it has not been built or device-tested in this environment. Run Gradle sync/build in Android Studio and resolve any environment-specific SDK or dependency issues before extending it.
