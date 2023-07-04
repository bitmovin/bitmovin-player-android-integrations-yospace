# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## Unreleased

## 2.0.0

### Added
- Added support for Bitmovin Player SDK v3

### Changed
- Migrated to new Listener APIs
- Added custom events and error code for yospace
- Upgrade `compileSdkVersion` and `targetSdkVersion` to `33`

### Removed
- Support for Bitmovin Player SDK v2

## 1.20.0

### Added
- Added support for Yospace SDK v3

## Changed
- Migrated to new Listener APIs
- Removed obsolete listeners
- Upgraded to Java 11 and Gradle 7.2

## 1.18.4

### Added
- Ability to filter for specific metadata types to be fired to Yospace.

## 1.18.3

### Changed
- Copying thumbnailTracks from original SourceConfig to new SourceItem

## 1.18.2

### Changed
- Bitmovin Maven link to new Bintray URL

## 1.18.1

### Changed
- Bitmovin player to `2.62.1+jason`

## 1.18.0

### Changed
- Bitmovin player to `2.62.0+jason`

## 1.17.0

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

## 1.16.0

### Added
- `nextAdBreak()` to `AdTimeline`
- `previousAdBreak()` to `AdTimeline`

### Changed
- Bitmovin player to `2.58.0+jason`

## 1.15.3

### Changed
- `fireCompanionEvent()` in `BitmovinYospacePlayer` to `onCompanionRendered()`

## 1.15.1

### Added
- `fireCompanionEvent()` to `BitmovinYospacePlayer`, which sends companion tracking events
- `id` property to `CompanionAd`

## 1.15.0

### Added
- Creative companion ad list `AdStartedEvent`

### Removed
- Duplicate `truexAd` in from `AdStartedEvent`

## 1.14.0
- Bitmovin player to `2.55.0+jason`

## 1.13.0
- Bitmovin player to `2.53.0+jason`

## 1.12.0

### Changed
- Bitmovin player to `2.52.0+jason`

## 1.11.0

### Changed
- Bitmovin player to `2.51.0+jason`

## 1.10.0

### Changed
- Bitmovin player to `2.50.0+jason`

## 1.9.0

### Changed
- Bitmovin player to `2.49.0+jason`

## 1.8.0

### Changed
- Bitmovin player to `2.48.0+jason`

## 1.7.0

### Changed
- Bitmovin player to `2.47.0+jason`

## 1.6.0

### Added
- Emit `AdQuartileEvent`

### Fixed
- Player pausing indefinitely when `TruexConfiguration` is null and a TrueX ad is found

## 1.5.0

### Changed
- Bitmovin player to `2.46.0+jason`

## 1.4.0

### Added
- Ad VAST extensions property to `Ad`

## 1.3.0

### Added
- `position` property (pre/mid/post roll or unknown) to `AdBreak`

### Changed
- Bitmovin player `2.45.0+jason`

## 1.2.2

### Changed
- Bitmovin player to `2.44.0`

## 1.2.1

### Fixed
- Player getting stuck after attempting to seek over midroll TrueX ad break

## 1.2.0

### Changed
- Bitmovin player to `2.43.0`

## 1.1.5

### Fixed
- Ad time not respected for LIVE ads
- Inconsistency between ad and ad break absolute time values

## 1.1.4

### Changed
- TrueX prerolls that meet ad free conditions now yield an ad free experience for entire session
- TrueX midrolls that meet ad free conditions now yield an ad free experience for current ad break only

### Fixed
- Pause and mute suppressed for non-Yospace content

## 1.1.3

### Changed
- Bitmovin player to `2.41.2`

### Fixed
- Incorrect `Ad` id for timeline ads

## 1.1.2

### Fixed
- `BitmovinYospacePlayerPolicy` not being respected
