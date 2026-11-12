# BDIX TV

BDIX TV is a Kotlin IPTV client with separate Android mobile and Android TV applications. It loads M3U playlists, keeps an offline channel cache, persists favorites, and plays supported streams with AndroidX Media3. The included playlist entries are references to third-party streams; availability and usage rights vary.

## Features

- Loads the BDIX main and international playlists over HTTPS and parses channel names, stream URLs, logos, categories, and explicit `tvg-id`/country metadata.
- Keeps the last successfully loaded channel list locally and shows it if a later refresh fails.
- Persists favorites and recently watched channels on mobile. Favorite identifiers use `tvg-id` where available.
- Supports channel search, category/country filtering, and a channel guide matched by `tvg-id`.
- Plays supported HLS and direct streams with Media3, player retry/error feedback, previous/next channel controls, and a landscape mobile player.
- Provides a separate TV browsing experience with D-pad-focusable channel cards, favorites, search, and remote-friendly playback controls.
- Provides English and Bangla Android TV interfaces with a language switch beside the playlist refresh control; the selected language is persisted by Android.
- Organizes Android TV channels into Bangladesh, Indian Bangla, Indian Movies, Indian Music, Indian Entertainment, Pakistani, International, Documentary & Wildlife, Kids, Religious, and Sports rows. Row counts reflect the channels currently loaded from the playlists.
- Limits Bangladesh, Indian Bangla, and Indian Movies rows to channels with an explicitly listed resolution of 720p or higher; channels with unknown resolution are excluded from these rows.
- Loads the generated `bangla_channels.json` on Android TV and shows its entries in a separate **Premium Channel** row; the last successful JSON response is cached for offline use.
- Checks for Android TV app updates on request using the GitHub-hosted `tv/appinfo.json` manifest, with optional or mandatory update handling.
- Stores the EPG cache in app-private storage and favorites/preferences locally.

## Playlist and EPG sources

- Main: `https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/IPTV-Playlist.m3u`
- International: `https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/International-Playlist.m3u`
- XMLTV: `https://raw.githubusercontent.com/saeidrahmanbd/BDIX-IPTV/main/epg/epg.xml`
- Android TV Premium Channel JSON: `https://raw.githubusercontent.com/bs23-mhasan/ipTV/master/bangla_channels.json`

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

## Publish an Android TV update

The TV app checks `tv/appinfo.json` only when the user selects **Check for Updates**. For each TV release:

1. Increase `versionCode` and update `versionName` in `tv/build.gradle.kts`.
2. Build and sign the TV APK with the same signing key used for the installed release, then publish it at the URL in `tv/appinfo.json` (or update `download_url`).
3. Update `latest_version`, `latest_version_code`, `minimum_supported_version_code`, and `release_notes` in `tv/appinfo.json`. Set `force_update` to `true` to require the newest version for all older installs; setting the minimum supported code above an installed version also requires an update.
4. Publish the APK and manifest changes to the `master` branch so the raw GitHub manifest URL can serve them.

The app compares Android version codes, not version-name strings. Update installation is confirmed by Android's package installer; on Android 8 and later, users may need to allow installs from BDIX TV in system settings.

## Project layout

- `mobile/` — Compose mobile app, navigation, channel/player/guide screens, ViewModels, repositories, parsers, and Preferences DataStore storage.
- `tv/` — TV-specific Compose browsing and Media3 playback.
- `gradle/libs.versions.toml` — dependency and plugin versions.

## Limitations and rights

Playlist presence does not guarantee that a stream is online, reachable from a particular ISP or location, or playable on a given device. Media3 support depends on stream format, codecs, and device capabilities. Use streams only when authorized; BDIX TV does not host or proxy the listed content. Third-party channel names, logos, streams, and programme data may have separate rights.
# bangla-iptv

Auto-updated JSON list of Bangladesh and Indian Bangla IPTV channels (720p and up, including sports).

- `bangla_channels.json` – the generated list (updated automatically)
- `fetch_channels.py` – builds the list from iptv-org playlists and your own repo files
- `.github/workflows/update-channels.yml` – refreshes the JSON every 12 hours

## Use in your app

```
https://raw.githubusercontent.com/<user>/<repo>/main/bangla_channels.json
```

Shape: `{ total, byCountry, channels[] }`. Each channel has `name`, `id`, `country`,
`resolution`, `logo`, `group`, `geoBlocked`, `status`, `url`, and optionally `userAgent` / `referrer`.

`status` is `online` or `offline`, tested by the workflow on every update. Channels are never removed,
so hide offline ones in your app: `channels.filter { it.status != "offline" }`.
The check runs from GitHub's servers (outside Bangladesh), so Bangladesh-only or geo-blocked streams
may show `offline` even though they work for you. A missing `status` means it hasn't been checked yet.

## Setup

1. Unzip, then push the files to a new GitHub repo (make sure the hidden `.github` folder is included).
2. That's it. The first push starts the workflow, then it runs every 12 hours and commits the updated JSON.
3. (Optional) Add your own playlists to `MY_REPO_SOURCES` in `fetch_channels.py`.

If a run fails with a permission error: **Settings -> Actions -> General -> Workflow permissions** -> *Read and write permissions*.
You can also start a run anytime: **Actions -> Update channels -> Run workflow**.

## Run locally

```
python fetch_channels.py                    # 720p+ (default), no stream check
python fetch_channels.py --check            # also test every stream (adds status)
python fetch_channels.py --min-res 1080     # 1080p only
python fetch_channels.py --filter-mine      # also filter your own entries
```

Streams are public and can go offline or be geo-blocked at any time.