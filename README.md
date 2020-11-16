# [![bitmovin](http://bitmovin-a.akamaihd.net/webpages/bitmovin-logo-github.png)](http://www.bitmovin.com)
Android module that integrates the Bitmovin Android SDK with the Yospace Ad Management SDK

# Getting started
## Gradle

Add this to your top level `build.gradle`

```
allprojects {
    repositories {
	maven {
	    url  'http://bitmovin.bintray.com/maven'
	}
    }
}
```

And this line to your main project `build.gradle`

```
dependencies {
    implementation 'com.bitmovin.player.integration:yospace:1.15.0'
}
```

## Examples

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
