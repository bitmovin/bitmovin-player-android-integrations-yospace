# Bitmovin Player Yospace Integration

This is an open-source project to enable the use of a third-party component (Yospace) with the Bitmovin Player Android SDK.

## Maintenance and Update

This project is not part of a regular maintenance or update schedule. For any update requests, please take a look at the guidance further below.

## Contributions to this project

As an open-source project, we are pleased to accept any and all changes, updates and fixes from the community wishing to use this project. Please see [CONTRIBUTING.md](CONTRIBUTING.md) for more details on how to contribute.

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
    implementation 'com.bitmovin.player.integration:yospace:2.0.0'
}
```

### Examples

The following example creates a `BitmovinYospacePlayer` and loads a `YospaceSourceConfig`.

#### Basic video playback

```kotlin
// Create a YospaceConfig
val yospaceConfig = YospaceConfig(
    userAgent = "userAgent",
    readTimeout = 25_000,
    connectTimeout = 25_000,
    requestTimeout = 25_000,
    liveInitialisationType = YospaceLiveInitialisationType.DIRECT,
    isDebug = true
)

// Create the BitmovinYospacePlayer
val player = BitmovinYospacePlayer(
    context = this,
    playerConfig = PlayerConfig(),
    yospaceConfig = yospaceConfig
)

// Attach it to your PlayerView (e.g. from your layout)
playerView.player = player

// Create a SourceConfig pointing at your HLS asset
val sourceConfig = SourceConfig("asset-url", SourceType.Hls)

// Create a YospaceSourceConfig with the YospaceAssetType
val yospaceSourceConfig = YospaceSourceConfig(YospaceAssetType.VOD)

// If playing an asset with TrueX ads, create a TruexConfig (optional)
val truexConfig = TruexConfig(viewGroup = playerView)

// Load the source (truexConfig is optional and defaults to null)
player.load(sourceConfig, yospaceSourceConfig, truexConfig)
```

#### Ad Tracking Events
The `BitmovinYospacePlayer` fires events through the standard Bitmovin Player event API.
Subscribe with the `on<EventType> { }` extension. These are the ad related events you will
typically observe:

```kotlin
player.on<PlayerEvent.AdBreakStarted> { ... }
player.on<PlayerEvent.AdBreakFinished> { ... }
player.on<PlayerEvent.AdStarted> { ... }
player.on<PlayerEvent.AdFinished> { ... }
player.on<PlayerEvent.AdClicked> { ... }
player.on<PlayerEvent.AdError> { ... }
player.on<PlayerEvent.AdSkipped> { ... }
```

#### Click Through Urls
The click-through URL is delivered with each ad-started event:

```kotlin
player.on<PlayerEvent.AdStarted> { event ->
    val clickThroughUrl = event.clickThroughUrl
}
```
