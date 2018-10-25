package com.bitmovin.player.integrations.bitmovinyospace;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.api.event.data.AdBreakFinishedEvent;
import com.bitmovin.player.api.event.data.AdBreakStartedEvent;
import com.bitmovin.player.api.event.data.AdClickedEvent;
import com.bitmovin.player.api.event.data.AdErrorEvent;
import com.bitmovin.player.api.event.data.AdFinishedEvent;
import com.bitmovin.player.api.event.data.AdSkippedEvent;
import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener;
import com.bitmovin.player.api.event.listener.OnAdClickedListener;
import com.bitmovin.player.api.event.listener.OnAdErrorListener;
import com.bitmovin.player.api.event.listener.OnAdFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdSkippedListener;
import com.bitmovin.player.api.event.listener.OnAdStartedListener;
import com.bitmovin.player.config.media.HLSSource;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.integrations.bitmovinyospacemodule.BitmovinYoSpacePlayer;
import com.bitmovin.player.integrations.bitmovinyospacemodule.Constants;
import com.bitmovin.player.integrations.bitmovinyospacemodule.YoSpaceAssetType;
import com.bitmovin.player.integrations.bitmovinyospacemodule.YoSpaceSourceConfiguration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinYoSpacePlayer yoSpaceLivePlayer;
    private Button liveButton;
    private Button vodButton;
    private Button unloadButton;
    private Button clickThrough;
    private String currentClickThroughUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vodButton = findViewById(R.id.vod_button);
        vodButton.setOnClickListener(this);
        liveButton = findViewById(R.id.live_button);
        liveButton.setOnClickListener(this);
        unloadButton = findViewById(R.id.unload_button);
        unloadButton.setOnClickListener(this);
        clickThrough = findViewById(R.id.click_through);
        clickThrough.setOnClickListener(this);

        bitmovinPlayerView = findViewById(R.id.bitmovinPlayerView);
        yoSpaceLivePlayer = new BitmovinYoSpacePlayer(getApplicationContext());
        this.bitmovinPlayerView.setPlayer(yoSpaceLivePlayer);
        yoSpaceLivePlayer.getConfig().getPlaybackConfiguration().setAutoplayEnabled(true);

        yoSpaceLivePlayer.addEventListener(onAdBreakStartedListener);
        yoSpaceLivePlayer.addEventListener(onAdBreakFinishedListener);
        yoSpaceLivePlayer.addEventListener(onAdStartedListener);
        yoSpaceLivePlayer.addEventListener(onAdFinishedListener);
        yoSpaceLivePlayer.addEventListener(onAdClickedListener);
        yoSpaceLivePlayer.addEventListener(onAdErrorListener);
        yoSpaceLivePlayer.addEventListener(onAdSkippedListener);

        this.loadLive();

    }

    private void unload(){
        yoSpaceLivePlayer.unload();
    }

    private void loadLive() {
        YoSpaceSourceConfiguration yoSpaceSourceConfiguration = new YoSpaceSourceConfiguration();
        SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e-ces1eurxaws101j8-6x78eoil2agd.cds1.yospace.com/csm/extlive/yospace02,hlssample.m3u8?yo.br=true&yo.ac=true"));
        yoSpaceSourceConfiguration.addSourceItem(sourceItem);
        yoSpaceSourceConfiguration.setYoSpaceAssetType(YoSpaceAssetType.LINEAR);

        yoSpaceLivePlayer.load(yoSpaceSourceConfiguration);
    }

    private void loadVod() {
        YoSpaceSourceConfiguration yoSpaceSourceConfiguration = new YoSpaceSourceConfiguration();
        SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e-ces1eurxaws101j8-6x78eoil2agd.cds1.yospace.com/access/d/400/u/0/1/130782300?f=0000130753172&format=vmap"));
        yoSpaceSourceConfiguration.addSourceItem(sourceItem);
        yoSpaceSourceConfiguration.setYoSpaceAssetType(YoSpaceAssetType.VOD);

        yoSpaceLivePlayer.load(yoSpaceSourceConfiguration);
    }

    private void loadLinearStartOver() {
        YoSpaceSourceConfiguration yoSpaceSourceConfiguration = new YoSpaceSourceConfiguration();
        SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e-ces1eurxaws101j8-6x78eoil2agd.cds1.yospace.com/access/d/400/u/0/1/130782300?f=0000130753172&format=vmap"));
        yoSpaceSourceConfiguration.addSourceItem(sourceItem);
        yoSpaceSourceConfiguration.setYoSpaceAssetType(YoSpaceAssetType.LINEAR_START_OVER);
        yoSpaceLivePlayer.load(yoSpaceSourceConfiguration);
    }


    @Override
    protected void onResume() {
        super.onResume();
        bitmovinPlayerView.onResume();
        yoSpaceLivePlayer.play();
    }

    @Override
    protected void onPause() {
        yoSpaceLivePlayer.pause();
        bitmovinPlayerView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        bitmovinPlayerView.onDestroy();
        super.onDestroy();
    }

    private OnAdBreakStartedListener onAdBreakStartedListener = new OnAdBreakStartedListener() {
        @Override
        public void onAdBreakStarted(AdBreakStartedEvent adBreakStartedEvent) {
            Toast.makeText(getApplicationContext(),"Ad Break Started",Toast.LENGTH_SHORT).show();
        }
    };

    private OnAdBreakFinishedListener onAdBreakFinishedListener = new OnAdBreakFinishedListener() {
        @Override
        public void onAdBreakFinished(AdBreakFinishedEvent adBreakFinishedEvent) {
            Toast.makeText(getApplicationContext(),"Ad Break Finished",Toast.LENGTH_SHORT).show();
        }
    };

    private OnAdStartedListener onAdStartedListener = new OnAdStartedListener() {
        @Override
        public void onAdStarted(AdStartedEvent adStartedEvent) {
            String clickThroughUrl = null;
            if(adStartedEvent != null){
                clickThroughUrl = adStartedEvent.getClickThroughUrl();
            }

            if(clickThroughUrl != null && clickThroughUrl != ""){
                clickThrough.setEnabled(true);
                clickThrough.setClickable(true);
                currentClickThroughUrl = clickThroughUrl;
            }

            Toast.makeText(getApplicationContext(),"Ad Started",Toast.LENGTH_SHORT).show();
        }
    };

    private OnAdFinishedListener onAdFinishedListener = new OnAdFinishedListener() {
        @Override
        public void onAdFinished(AdFinishedEvent adFinishedEvent) {
            clickThrough.setEnabled(false);
            clickThrough.setClickable(false);
            currentClickThroughUrl = null;
            Toast.makeText(getApplicationContext(),"Ad Finished",Toast.LENGTH_SHORT).show();
        }
    };

    private OnAdSkippedListener onAdSkippedListener = new OnAdSkippedListener() {
        @Override
        public void onAdSkipped(AdSkippedEvent adSkippedEvent) {
            Toast.makeText(getApplicationContext(),"Ad Skipped",Toast.LENGTH_SHORT).show();
        }
    };

    private OnAdErrorListener onAdErrorListener = new OnAdErrorListener() {
        @Override
        public void onAdError(AdErrorEvent adErrorEvent) {
            Toast.makeText(getApplicationContext(),"Ad Error",Toast.LENGTH_SHORT).show();
        }
    };

    private OnAdClickedListener onAdClickedListener = new OnAdClickedListener() {
        @Override
        public void onAdClicked(AdClickedEvent adClickedEvent) {
            Toast.makeText(getApplicationContext(),"Ad Clicked",Toast.LENGTH_SHORT).show();
        }
    };

    private void clickThroughPressed(){
        yoSpaceLivePlayer.clickThroughPressed();

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentClickThroughUrl));
        startActivity(browserIntent);
    }

    @Override
    public void onClick(View v) {
        if (v == liveButton) {
            loadLive();
        } else if (v == vodButton) {
            loadVod();
        } else if (v == unloadButton) {
            unload();
        } else if (v == clickThrough) {
            clickThroughPressed();
        }
    }

}
