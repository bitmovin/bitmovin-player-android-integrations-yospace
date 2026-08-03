# Bitmovin Player Yospace Integration

This is an open-source project to enable the use of a third-party component (Yospace) with the Bitmovin Player Android SDK.

## Maintenance and Update

This project is not part of a regular maintenance or update schedule. For any update requests, please take a look at the guidance further below.

## Contributions to this project

As an open-source project, we are pleased to accept any and all changes, updates and fixes from the community wishing to use this project. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for more details on how to contribute.

## Releasing

Releasing a new version is automated and tag-driven. A maintainer triggers the **Release new version** GitHub Action (`workflow_dispatch`) on the `main` branch. The workflow determines the next version from the `## [Unreleased]` entries in [CHANGELOG.md](CHANGELOG.md) (minor for `Added`/`Changed`/`Removed`, patch for `Fixed`/`Security`/`Deprecated`), bumps the version, updates the changelog, tags the release, publishes the AAR to Artifactory, and creates a GitHub release. See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## Reporting player bugs

If you come across a bug related to the player, please raise this through your support ticketing system.

## Need more help?

Should you want some help updating this project (update, modify, fix or otherwise) and can't contribute for any reason, please raise your request to your Bitmovin account team, who can discuss your request.

## Support and SLA Disclaimer

As an open-source project and not a core product offering, any request, issue or query related to this project is excluded from any SLA and Support terms that a customer might have with either Bitmovin or another third-party service provider or Company contributing to this project. Any and all updates are purely at the contributor's discretion.

Thank you for your contributions!

## Getting started
### Gradle

Add this to your top level `build.gradle`

```
allprojects {
    repositories {
        maven {
            url  'https://bitmovin.jfrog.io/artifactory/public-releases'
        }
    }
}
```

Customers will need to use the Yospace SDK from their own Yospace account, so add the Yospace
repository with credentials to your top level `build.gradle`. The credentials must live inside the
`maven { }` repository block:

```groovy
allprojects {
    repositories {
        maven {
            url 'https://yospacerepo.jfrog.io/artifactory/android-sdk'
            credentials {
                // Use your Yospace account's username and a JFrog identity/reference token.
                // Prefer reading these from ~/.gradle/gradle.properties or environment variables
                // rather than hardcoding them here.
                username = findProperty('yospaceUser') ?: System.getenv('YOSPACE_USER')
                password = findProperty('yospaceToken') ?: System.getenv('YOSPACE_TOKEN')
            }
        }
    }
}
```


And this line to your main project `build.gradle`

```groovy
dependencies {
    implementation 'com.bitmovin.player.integration:yospace:2.3.0'
}
```

### Examples

The following example creates a `BitmovinYospacePlayer` and loads a `YospaceSourceConfig`.

#### Basic video playback

```kotlin
// Create a YospaceConfig
val yospaceConfig = YospaceConfig(
    userAgent = "userAgent",
    requestTimeout = 25_000,
    liveInitializationType = YospaceLiveInitializationType.DIRECT,
    isDebug = true,
    yospaceDebugMode = YospaceDebugMode.VALIDATION
)

// Create the BitmovinYospacePlayer
val player = BitmovinYospacePlayer(
    context = this,
    playerConfig = PlayerConfig(),
    yospaceConfig = yospaceConfig
)

// Attach it to your PlayerView (e.g. from your layout)
playerView.player = player

// Create a SourceConfig pointing at your HLS or DASH asset
val sourceConfig = SourceConfig("asset-url", SourceType.Hls) // or SourceType.Dash

// Create a YospaceSourceConfig with the YospaceAssetType
val yospaceSourceConfig = YospaceSourceConfig(YospaceAssetType.VOD)

// If playing an asset with TrueX ads, create a TruexConfig (optional)
val truexConfig = TruexConfig(viewGroup = playerView)

// Load the source (truexConfig is optional and defaults to null)
player.load(sourceConfig, yospaceSourceConfig, truexConfig)
```

When uploading live validation logs to Yospace, select the same initialization type in the validation tool as configured in `YospaceConfig.liveInitializationType`.

#### Bitmovin Analytics

Pass an `AnalyticsPlayerConfig` to configure [Bitmovin Analytics](https://developer.bitmovin.com/playback/docs/setup-analytics-android), just as you would when using the Bitmovin Player directly:

```kotlin
val player = BitmovinYospacePlayer(
    context = this,
    playerConfig = PlayerConfig(),
    yospaceConfig = yospaceConfig,
    analyticsConfig = AnalyticsPlayerConfig.Enabled(
        AnalyticsConfig(
            licenseKey = "your-analytics-license-key",
            // Required for SSAI ad quartile tracking
            ssaiEngagementTrackingEnabled = true
        ),
        DefaultMetadata(cdnProvider = "akamai", customUserId = "user-id")
    )
)
```

When `analyticsConfig` is omitted, the analytics license is resolved from your player license. Pass `AnalyticsPlayerConfig.Disabled` to turn analytics off.

The `AnalyticsApi` of the underlying player is available through `player.analytics`:

```kotlin
val impressionId = player.analytics?.impressionId
```

If you pass your own `Player` instance, configure analytics on that instance instead — `analyticsConfig` is ignored in that case and a `YospacePlayerEvent.Warning` with `YospaceWarningCode.AnalyticsConfigIgnored` is emitted.

##### Source metadata

Set per-source analytics metadata through `YospaceSourceConfig`:

```kotlin
val yospaceSourceConfig = YospaceSourceConfig(
    assetType = YospaceAssetType.VOD,
    sourceMetadata = SourceMetadata(
        title = "My Stream",
        videoId = "video-123",
        customData = CustomData(customData1 = "campaign-x")
    )
)
```

Yospace assets play through a proxied URL, so the metadata is applied to the source the integration loads internally. `SourceMetadata.isLive` is derived from `assetType` when you do not set it, and `title` falls back to the title of the `SourceConfig` you pass to `load`.

#### Yospace validation logs

The sample app can generate upload-ready validation logs for the Yospace validation tool:

```shell
scripts/capture-yospace-validation-logs.sh --submission vod
scripts/capture-yospace-validation-logs.sh --submission dvr-live-direct
```

Each submission creates two logs, one for playback through an ad break and one for playback across two sessions. The generated manifest names the matching Yospace validation-tool selection.

Maintainers can also run the **Yospace Validation Logs** GitHub Action manually. It captures the selected submission on an Android emulator and uploads the generated logs and manifests as a workflow artifact.

#### Yospace Events
Yospace events carry integration-owned payloads. Subscribe with the
`on<EventType> { }` extension.
These are the events you will typically observe:

```kotlin
player.on<YospacePlayerEvent.Error> { event -> event.message }
player.on<YospacePlayerEvent.Warning> { event -> event.message }
player.on<YospacePlayerEvent.AdBreakStarted> { event -> event.adBreak }
player.on<YospacePlayerEvent.AdBreakFinished> { event -> event.adBreak }
player.on<YospacePlayerEvent.AdStarted> { event -> event.ad }
player.on<YospacePlayerEvent.AdClicked> { event -> event.clickThroughUrl }
player.on<YospacePlayerEvent.AdFinished> { event -> event.ad }
player.on<YospacePlayerEvent.AdSkipped> { event -> event.ad }
player.on<YospacePlayerEvent.AdQuartile> { event -> event.quartile }
player.on<YospacePlayerEvent.TruexAdFree> { /* session is ad-free */ }
```

Standard `player.on<PlayerEvent...>` callbacks still receive Bitmovin Player events. For VOD SSAI,
`PlayerEvent.TimeChanged` reports content-relative time outside ads and ad-relative time during
Yospace ad playback.

#### Click Through Urls
The click-through URL is delivered with each ad-started event:

```kotlin
player.on<YospacePlayerEvent.AdStarted> { event ->
    val clickThroughUrl = event.clickThroughUrl
}
```

After opening the click-through URL, notify the ad object so the Yospace SDK can fire its click
tracking and the integration can emit `YospacePlayerEvent.AdClicked`:

```kotlin
player.on<YospacePlayerEvent.AdStarted> { event ->
    event.ad?.clickThroughUrlOpened()
}
```
