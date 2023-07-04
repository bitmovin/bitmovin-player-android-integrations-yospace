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

Customers will need to use Yospace SDK from their own Yospace account so add Yospace credentails in build.gradle.
```
allprojects {

    credentials {
        //ADD credentials for build so that gradle can download yospace libraries
        username = ""
        password = ""
    }
}
```


And this line to your main project `build.gradle`

```
dependencies {
    implementation 'com.bitmovin.player.integration:yospace:1.15.3'
}
```

### Examples

The following example creates a `BitmovinYospacePlayer` object and loads a `YospaceSourceConfiguration`

#### Basic video playback 

```kotlin
// Create a YospaceConfiguration using the YospaceConfigurationBuilder
val yospaceConfig = YospaceConfiguration(
    userAgent = "userAgent",
    readTimeout = 25000,
    requestTimeout = 25000,
    connectTimeout = 25000,
    liveInitialisationType = YospaceLiveInitialisationType.DIRECT,
    isDebug = true
)

// If playing an asset with TrueX ads, create a TruexConfiguration
val truexConfig = TruexConfiguration(player_view)

// Create the BitmovinYospacePlayer
val player = BitmovinYospacePlayer(this, playerConfig, yospaceConfig)
    
// Set it to your BitmovinPlayerView
bitmovinPlayerView.setPlayer(player);
    
// Create a SourceConfiguration 
val sourceItem = SourceItem(HLSSource("asset-url"))
val sourceConfig = SourceConfiguration()
sourceConfig.addSourceItem(sourceItem) 
    
// Create a YospaceSourceConfiguration with a SourceConfiguration and YospaceAssetType
val yospaceSourceConfig = YospaceSourceConfiguration(YospaceAssetType.VOD)

//Load your YospaceSourceConfiguration
player.load(sourceConfig, yospaceSourceConfig)
```

#### Ad Tracking Events
The BitmovinYospacePlayer will fire events thought the normal Bitmovin event listeners. These are the ad related event listeners you should create 

```kotlin
player.addEventListener(OnAdBreakStartedListener { ... })
player.addEventListener(OnAdBreakFinishedListener { ... })
player.addEventListener(OnAdStartedListener { ... })
player.addEventListener(OnAdFinishedListener { ... })
player.addEventListener(OnAdClickedListener { ... })
player.addEventListener(OnAdErrorListener { ... })
player.addEventListener(OnAdSkippedListener { ... })
``` 


#### Click Through Urls
Click through URLs will be delivered through each AdStartedEvent.
```kotlin
OnAdStartedListener {
    val clickThroughUrl = it.clickThroughUrl
}
```

To track ads, call `clickThroughPressed()` when a user clicks on an ad
```kotlin
player.clickThroughPressed();
```
