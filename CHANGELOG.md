# Change Log

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added

- `BitmovinYospacePlayer` support for configuring Bitmovin Analytics via `AnalyticsPlayerConfig`, including disabling it with `AnalyticsPlayerConfig.Disabled`
- `BitmovinYospacePlayer.analytics` to access the `AnalyticsApi` of the underlying player
- `YospaceWarningCode.AnalyticsConfigIgnored`, emitted when an analytics configuration is passed together with a `Player` instance
- `YospaceSourceConfig.sourceMetadata` to set Bitmovin Analytics metadata for a Yospace source

### Changed

### Deprecated

### Removed

### Fixed

- Source settings such as title, subtitles, poster image, and codec priorities were ignored when playing a Yospace asset

### Security

## [2.3.0] - 2026-07-29

### Added

- DASH stream support for Yospace assets.

## [2.2.0] - 2026-07-01

### Added

- `YospaceConfig.yospaceDebugMode` to enable Yospace SDK validation or full debug logging
- Sample-app validation mode, capture script, and manual GitHub Action for generating Yospace validation-tool log submissions
- `YospacePlayerEvent` ad lifecycle events (`AdBreakStarted`, `AdBreakFinished`, `AdStarted`, `AdClicked`, `AdFinished`, `AdSkipped`, `AdQuartile`) carrying integration-owned `Ad`/`AdBreak` payloads
- `BitmovinYospacePlayer` `on`/`next`/`off` support for `YospacePlayerEvent`, reified Kotlin extensions for `on`/`next`, and `Class`-based Java overloads using `YospacePlayerEventListener`, e.g. `player.on<YospacePlayerEvent.AdBreakStarted> { ... }`
- `YospacePlayerEvent.TruexAdFree` for TrueX ad-free sessions
- Yospace ad click-through reporting when `Ad.clickThroughUrlOpened()` is called
- Yospace warning codes for no-analytics, initialization, and analytics-session issues

### Changed

- Upgraded Yospace Ad Management SDK from `3.3.3` to `3.11.2`
- `YospaceAssetType.LINEAR_START_OVER` now uses the recommended DVRLive session mode
- Split `YospaceErrorCode` and `YospaceWarningCode` into separate files and 
- Moved advertising types (`Ad`, `AdBreak`, `AdBreakPosition`, `AdData`, `CompanionAd`, `AdTimeline`, `BitmovinTruexAdRenderer`) to the `com.bitmovin.player.integration.yospace.advertising` package, playback policy types (`BitmovinYospacePlayerPolicy`, `DefaultBitmovinYospacePlayerPolicy`, `YospacePlayerPolicy`) to the `com.bitmovin.player.integration.yospace.policy` package, `YospaceAssetType`/`YospaceLiveInitializationType` to the `com.bitmovin.player.integration.yospace.config` package, and `YospaceErrorCode`/`YospaceWarningCode` to the `com.bitmovin.player.integration.yospace.deficiency` package; update imports accordingly

### Fixed

- Ad breaks were not correctly reported if `YospaceAssetType.LINEAR_START_OVER` was used
- `YospaceConfig.liveInitializationType` had no effect for `YospaceAssetType.LINEAR_START_OVER`
- Live proxy sessions could fail Yospace validation
- Recoverable Yospace session failures could crash before fallback playback

### Deprecated

- `YospaceAssetType.LINEAR`: Use `YospaceAssetType.LINEAR_START_OVER` for DVR live playback instead.

### Removed

- `com.yospace:admanagement-util` dependency as the Yospace SDK now provides the required utilities directly
- `YospaceConfig.readTimeout` and `YospaceConfig.connectTimeout` as the Yospace SDK no longer exposes these settings
- Routing of Yospace ad events through `Player.on<PlayerEvent...>`; subscribe via `player.on<YospacePlayerEvent...>` instead, as `Player.on<PlayerEvent...>` now receives Bitmovin Player events only
- Legacy `OnAdBreakStartedListener`/`OnAdFinishedListener`/etc. event-listener interfaces, superseded by `YospacePlayerEvent`

## [2.1.0] - 2026-06-03

### Changed

- Upgraded Bitmovin Player SDK to `3.154.0`
- Raised `minSdkVersion` from `19` to `23`
- Upgraded `compileSdkVersion` from `33` to `35`

### Fixed

- Yospace ad rules are now enforced on the `Player.ads.*` API

## [2.0.0]

### Added

- Added support for Bitmovin Player SDK v3

### Changed

- Migrated to new Listener APIs
- Added custom events and error code for yospace
- Upgrade `compileSdkVersion` and `targetSdkVersion` to `33`

### Removed

- Support for Bitmovin Player SDK v2

## [1.20.0]

### Added

- Added support for Yospace SDK v3

### Changed

- Migrated to new Listener APIs
- Removed obsolete listeners
- Upgraded to Java 11 and Gradle 7.2

## [1.18.4]

### Added

- Ability to filter for specific metadata types to be fired to Yospace.

## [1.18.3]

### Changed

- Copying thumbnailTracks from original SourceConfig to new SourceItem

## [1.18.2]

### Changed

- Bitmovin Maven link to new Bintray URL

## [1.18.1]

### Changed

- Bitmovin player to `2.62.1+jason`

## [1.18.0]

### Changed

- Bitmovin player to `2.62.0+jason`

## [1.17.0]

### Added

- `creativeId`, `title`, `avertiser`, `system`, `lineage` and `isFiller` properties to `Ad`

### Changed

- Bitmovin player to `2.60.0+jason`
- `id` property from `AdBreak` now returns `breakId`
- `id` property from `Ad` now returns shortened identifier
- `mediaFileUrl` property from `Ad` now returns the asset URI

### Removed

- `isTruex` property from `Ad` (use `hasInteractiveUnit`)

### Fixed

- Player stuck in TrueX filler asset

## [1.16.0]

### Added

- `nextAdBreak()` to `AdTimeline`
- `previousAdBreak()` to `AdTimeline`

### Changed

- Bitmovin player to `2.58.0+jason`

## [1.15.3]

### Changed

- `fireCompanionEvent()` in `BitmovinYospacePlayer` to `onCompanionRendered()`

## [1.15.1]

### Added

- `fireCompanionEvent()` to `BitmovinYospacePlayer`, which sends companion tracking events
- `id` property to `CompanionAd`

## [1.15.0]

### Added

- Creative companion ad list `AdStartedEvent`

### Removed

- Duplicate `truexAd` in from `AdStartedEvent`

## [1.14.0]

- Bitmovin player to `2.55.0+jason`

## [1.13.0]

- Bitmovin player to `2.53.0+jason`

## [1.12.0]

### Changed

- Bitmovin player to `2.52.0+jason`

## [1.11.0]

### Changed

- Bitmovin player to `2.51.0+jason`

## [1.10.0]

### Changed

- Bitmovin player to `2.50.0+jason`

## [1.9.0]

### Changed

- Bitmovin player to `2.49.0+jason`

## [1.8.0]

### Changed

- Bitmovin player to `2.48.0+jason`

## [1.7.0]

### Changed

- Bitmovin player to `2.47.0+jason`

## [1.6.0]

### Added

- Emit `AdQuartileEvent`

### Fixed

- Player pausing indefinitely when `TruexConfiguration` is null and a TrueX ad is found

## [1.5.0]

### Changed

- Bitmovin player to `2.46.0+jason`

## [1.4.0]

### Added

- Ad VAST extensions property to `Ad`

## [1.3.0]

### Added

- `position` property (pre/mid/post roll or unknown) to `AdBreak`

### Changed

- Bitmovin player `2.45.0+jason`

## [1.2.2]

### Changed

- Bitmovin player to `2.44.0`

## [1.2.1]

### Fixed

- Player getting stuck after attempting to seek over midroll TrueX ad break

## [1.2.0]

### Changed

- Bitmovin player to `2.43.0`

## [1.1.5]

### Fixed

- Ad time not respected for LIVE ads
- Inconsistency between ad and ad break absolute time values

## [1.1.4]

### Changed

- TrueX prerolls that meet ad free conditions now yield an ad free experience for entire session
- TrueX midrolls that meet ad free conditions now yield an ad free experience for current ad break only

### Fixed

- Pause and mute suppressed for non-Yospace content

## [1.1.3]

### Changed

- Bitmovin player to `2.41.2`

### Fixed

- Incorrect `Ad` id for timeline ads

## [1.1.2]

### Fixed

- `BitmovinYospacePlayerPolicy` not being respected

[Unreleased]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/2.3.0...HEAD
[2.3.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/2.2.0...2.3.0
[2.2.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/2.1.0...2.2.0
[2.1.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/2.0.0...2.1.0
[2.0.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.20.0...2.0.0
[1.20.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.18.4...1.20.0
[1.18.4]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.18.3...1.18.4
[1.18.3]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.18.2...1.18.3
[1.18.2]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.18.1...1.18.2
[1.18.1]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.18.0...1.18.1
[1.18.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.17.0...1.18.0
[1.17.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.16.0...1.17.0
[1.16.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.15.3...1.16.0
[1.15.3]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.15.1...1.15.3
[1.15.1]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.15.0...1.15.1
[1.15.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.14.0...1.15.0
[1.14.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.13.0...1.14.0
[1.13.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.12.0...1.13.0
[1.12.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.11.0...1.12.0
[1.11.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.10.0...1.11.0
[1.10.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.9.0...1.10.0
[1.9.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.8.0...1.9.0
[1.8.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.7.0...1.8.0
[1.7.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.6.0...1.7.0
[1.6.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.5.0...1.6.0
[1.5.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.4.0...1.5.0
[1.4.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.3.0...1.4.0
[1.3.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.2.2...1.3.0
[1.2.2]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.2.1...1.2.2
[1.2.1]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.2.0...1.2.1
[1.2.0]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.1.5...1.2.0
[1.1.5]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.1.4...1.1.5
[1.1.4]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.1.3...1.1.4
[1.1.3]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/compare/1.1.2...1.1.3
[1.1.2]: https://github.com/bitmovin/bitmovin-player-android-integrations-yospace/commits/1.1.2
