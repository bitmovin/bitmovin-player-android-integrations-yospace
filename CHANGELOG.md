# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Added
- AdBreak position property (Pre/mid/post roll or unknown)

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
