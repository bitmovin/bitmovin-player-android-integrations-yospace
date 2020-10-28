# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.14.0]
- Bitmovin Player version to 2.55.0+jason
    - Add `startTime` to metadata events

## [1.13.0]
- Bitmovin Player version to 2.53.0+jason

## [1.12.0]

### Changed
- Bitmovin Player version to 2.52.0+jason

## [1.11.0]

### Changed
- Bitmovin Player version to 2.51.0+jason

## [1.10.0]

### Changed
- Bitmovin Player version to 2.50.0+jason

## [1.9.0]

### Changed
- Bitmovin Player version to 2.49.0+jason

## [1.8.0]

### Changed
- Bitmovin Player version to 2.48.0+jason

## [1.7.0]

### Changed
- Bitmovin Player version to 2.47.0+jason

## [1.6.0]

### Added
- Exposed AdQuartile event

### Fixed
- Player pausing indefinitely when TruexConfiguration is null and a TrueX ad is found

## [1.5.0]

### Changed
- Bitmovin Player version to 2.46.0+jason

## [1.4.0]

### Added
- Ad VAST extensions property

## [1.3.0]

### Added
- AdBreak position property (pre/mid/post roll or unknown)

### Changed
- Bitmovin Player version to 2.45.0+jason

## [1.2.2]

### Changed
- Bitmovin Player version to 2.44.0

## [1.2.1]

### Fixed
- Player getting stuck after attempting to seek over midroll TrueX ad break

## [1.2.0]

### Changed
- Bitmovin Player version to 2.43.0

## [1.1.5]

### Fixed
- Ad time not respected for LIVE ads
- Inconsistency between ad and ad break absolute time values

## [1.1.4]

### Changed
- TrueX prerolls that meet ad free conditions now yield an ad free experience for entire session
- TrueX midrolls that meet ad free conditions now yield an ad free experience for current ad break only

### Fixed
- Pause and mute suppressed for non-YoSpace content

## [1.1.3]

### Changed
- Bitmovin Player version to 2.41.2

### Fixed
- Incorrect Ad id for timeline ads

## [1.1.2]

### Fixed
- BitmovinYospacePlayerPolicy not being respected

## [1.1.1]

### Changed
- Remaining classes to Kotlin
