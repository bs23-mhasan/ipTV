# BDIX TV

BDIX TV is a Kotlin IPTV client with separate Android mobile and Android TV applications. It loads M3U playlists, keeps an offline channel cache, persists favorites, and plays supported streams with AndroidX Media3. The included playlist entries are references to third-party streams; availability and usage rights vary.

## Features

- Loads the BDIX main and international playlists over HTTPS and parses channel names, stream URLs, logos, categories, and explicit `tvg-id`/country metadata.
- Keeps the last successfully loaded channel list locally and shows it if a later refresh fails.
- Persists favorites and recently watched channels on mobile. Favorite identifiers use `tvg-id` where available.
- Supports channel search, category/country filtering, and a channel guide matched by `tvg-id`.
- Plays supported HLS and direct streams with Media3, player retry/error feedback, previous/next channel controls, and a landscape mobile player.
- Provides a separate TV browsing experience with D-pad-focusable channel cards, favorites, search, and remote-friendly playback controls.
- Stores the EPG cache in app-private storage and favorites/preferences locally.

## Playlist and EPG sources

- Main: `https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/IPTV-Playlist.m3u`
- International: `https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/International-Playlist.m3u`
- XMLTV: `https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/epg/epg.xml`

Only metadata present in a playlist is shown. Channels without a `tvg-country` or `country` attribute are not assigned an inferred country, and channels without a category are not placed in an invented category.

## Build

Requirements: Android Studio, JDK 17, and Android SDK Platform 36.

From the repository root:

```bash
./gradlew :mobile:assembleDebug :tv:assembleDebug
./gradlew :mobile:testDebugUnitTest :tv:testDebugUnitTest
```

APK outputs:

- Mobile: `mobile/build/outputs/apk/debug/mobile-debug.apk`
- Android TV: `tv/build/outputs/apk/debug/tv-debug.apk`

## Build release APKs

Build both release APKs from the repository root:

```bash
./gradlew :mobile:assembleRelease :tv:assembleRelease
```

The APK files are generated here:

- Mobile: `mobile/build/outputs/apk/release/mobile-release-unsigned.apk`
- Android TV: `tv/build/outputs/apk/release/tv-release-unsigned.apk`

These Gradle release outputs are unsigned and are not ready to install or distribute. To create signed APKs, open the project in Android Studio and select **Build → Generate Signed Bundle / APK → APK**. Choose the `mobile` or `tv` module, then sign with your release keystore. Keep the keystore and passwords private and backed up; the same signing key is needed for future app updates.

## Download

- [Download Android TV release APK](https://github.com/bs23-mhasan/ipTV/raw/refs/heads/master/tv/release/tv-release.apk)
- [Download mobile release APK](https://github.com/bs23-mhasan/ipTV/raw/refs/heads/master/mobile/release/mobile-release.apk)

Open the repository root in Android Studio to run either application on a device or emulator. The TV app declares the Leanback launcher category; touch input is optional.

## Project layout

- `mobile/` — Compose mobile app, navigation, channel/player/guide screens, ViewModels, repositories, parsers, and Preferences DataStore storage.
- `tv/` — TV-specific Compose browsing and Media3 playback.
- `gradle/libs.versions.toml` — dependency and plugin versions.

## Limitations and rights

Playlist presence does not guarantee that a stream is online, reachable from a particular ISP or location, or playable on a given device. Media3 support depends on stream format, codecs, and device capabilities. Use streams only when authorized; BDIX TV does not host or proxy the listed content. Third-party channel names, logos, streams, and programme data may have separate rights.
