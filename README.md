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
    implementation 'com.bitmovin.player.integration:yospace:1.0.5'
}
```

## Examples

The following example creates a `BitmovinYospacePlayer` object and loads a `YospaceSourceConfiguration`

#### Basic video playback 

```java
//Create a YospaceConfiguration using the YospaceConfigurationBuilder
YospaceConfiguration yospaceConfiguration = new YospaceConfigurationBuilder().setConnectTimeout(25000).setReadTimeout(25000).setRequestTimeout(25000).setDebug(true).build();

//Optionally create a TruexConfiguration
TruexConfiguration truexConfiguration = new TruexConfiguration(bitmovinPlayerView);

//Create a BitmovinYospacePlayer
BitmovinYospacePlayer bitmovinYospacePlayer = new BitmovinYospacePlayer(getApplicationContext(), playerConfiguration, yospaceConfiguration, trueXConfiguration);
    
//Set it to your BitmovinPlayerView
bitmovinPlayerView.setPlayer(bitmovinYospacePlayer);
    
//Create a SourceConfiguration 
SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e-ces1eurxaws101j8-6x78eoil2agd.cds1.yospace.com/csm/extlive/yospace02,hlssample.m3u8?yo.br=true&yo.ac=true"));
SourceConfiguration sourceConfig = new SourceConfiguration();
sourceConfig.addSourceItem(sourceItem);
    
//Create a YospaceSourceConfiguration with your SourceConfiguration and a YospaceAssetType
YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR_START_OVER);

//Load your YospaceSourceConfiguration
bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
```

#### Ad Tracking Events
The BitmovinYospacePlayer will fire events thought the normal Bitmovin event listeners. These are the ad related event listeners you should create 

```java
bitmovinYospacePlayer.addEventListener(onAdBreakStartedListener);
bitmovinYospacePlayer.addEventListener(onAdBreakFinishedListener);
bitmovinYospacePlayer.addEventListener(onAdStartedListener);
bitmovinYospacePlayer.addEventListener(onAdFinishedListener);
bitmovinYospacePlayer.addEventListener(onAdClickedListener);
bitmovinYospacePlayer.addEventListener(onAdErrorListener);
bitmovinYospacePlayer.addEventListener(onAdSkippedListener);
``` 


#### Click Through Urls
Click through URLs will be delivered through each AdStartedEvent.
```java
private OnAdStartedListener onAdStartedListener = new OnAdStartedListener() {
   @Override
   public void onAdStarted(AdStartedEvent adStartedEvent) {
       String clickThroughUrl = adStartedEvent.getClickThroughUrl();
   };
}
```

In order to properly track ads, you must call `clickThroughPressed()` whenever the user clicks on an ad
```java
bitmovinYospacePlayer.clickThroughPressed();
```

