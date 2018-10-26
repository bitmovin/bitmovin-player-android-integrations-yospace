# [![bitmovin](http://bitmovin-a.akamaihd.net/webpages/bitmovin-logo-github.png)](http://www.bitmovin.com)
Android module that integrates the Bitmovin Android SDK with the YoSpace Ad Management SDK

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
    compile 'com.bitmovin.player.integrations:bitmovinyospacemodule:0.1.0'
}
```

## Examples

The following example creates a `BitmovinYoSpacePlayer` object loads a `YoSpaceSourceConfiguration`

#### Basic video playback 
```java
    //Create a BitmovinYoSpacePlayer
    BitmovinYoSpacePlayer bitmovinYoSpacePlayer = new BitmovinYoSpacePlayer(getApplicationContext());
    
    //Set it to your BitmovinPlayerView
    this.bitmovinPlayerView.setPlayer(bitmovinYoSpacePlayer);
    
    //Create a SourceConfiguration 
    SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e-ces1eurxaws101j8-6x78eoil2agd.cds1.yospace.com/csm/extlive/yospace02,hlssample.m3u8?yo.br=true&yo.ac=true"));
    SourceConfiguration sourceConfig = new SourceConfiguration();
    sourceConfig.addSourceItem(sourceItem);
    
    //Create a YoSpaceSourceConfiguration with your SourceConfiguration and a YoSpaceAssetType
    YoSpaceSourceConfiguration yoSpaceSourceConfiguration = new YoSpaceSourceConfiguration(sourceConfig,YoSpaceAssetType.LINEAR);
    
    //Load your YoSpaceSourceConfiguration
    bitmovinYoSpacePlayer.load(yoSpaceSourceConfiguration);
        
```

#### Ad Tracking Events
The BitmovinYoSpacePlayer will fire events thought the normal Bitmovin event listeners. These are the ad related event listeners you should create 

```java
    bitmovinYoSpacePlayer.addEventListener(onAdBreakStartedListener);
    bitmovinYoSpacePlayer.addEventListener(onAdBreakFinishedListener);
    bitmovinYoSpacePlayer.addEventListener(onAdStartedListener);
    bitmovinYoSpacePlayer.addEventListener(onAdFinishedListener);
    bitmovinYoSpacePlayer.addEventListener(onAdClickedListener);
    bitmovinYoSpacePlayer.addEventListener(onAdErrorListener);
    bitmovinYoSpacePlayer.addEventListener(onAdSkippedListener);
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

In order to properly track ads you must call `clickThroughPressed()`
```java
    bitmovinYoSpacePlayer.clickThroughPressed();
```


