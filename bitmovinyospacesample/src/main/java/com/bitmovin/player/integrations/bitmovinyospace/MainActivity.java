package com.bitmovin.player.integrations.bitmovinyospace;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
import com.bitmovin.player.config.drm.DRMSystems;
import com.bitmovin.player.config.media.HLSSource;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.integrations.bitmovinyospacemodule.BitmovinYospacePlayer;
import com.bitmovin.player.integrations.bitmovinyospacemodule.YospaceAssetType;
import com.bitmovin.player.integrations.bitmovinyospacemodule.YospaceSourceConfiguration;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinYospacePlayer bitmovinYospacePlayer;
    private Button liveButton;
    private Button vodButton;
    private Button unloadButton;
    private Button clickThrough;
    private String currentClickThroughUrl;
    private Handler handler = new Handler(Looper.getMainLooper());

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
        bitmovinYospacePlayer = new BitmovinYospacePlayer(getApplicationContext());
        this.bitmovinPlayerView.setPlayer(bitmovinYospacePlayer);
        bitmovinYospacePlayer.getConfig().getPlaybackConfiguration().setAutoplayEnabled(true);

        bitmovinYospacePlayer.getYoSpaceConfiguration().debug = true;
        bitmovinYospacePlayer.getYoSpaceConfiguration().connectTimeout = 5000;
        bitmovinYospacePlayer.getYoSpaceConfiguration().requestTimeout = 5000;
        bitmovinYospacePlayer.getYoSpaceConfiguration().readTimeout = 5000;
        bitmovinYospacePlayer.getYoSpaceConfiguration().userAgent = "BitmovinPlayerUserAgent";

        bitmovinYospacePlayer.addEventListener(onAdBreakStartedListener);
        bitmovinYospacePlayer.addEventListener(onAdBreakFinishedListener);
        bitmovinYospacePlayer.addEventListener(onAdStartedListener);
        bitmovinYospacePlayer.addEventListener(onAdFinishedListener);
        bitmovinYospacePlayer.addEventListener(onAdClickedListener);
        bitmovinYospacePlayer.addEventListener(onAdErrorListener);
        bitmovinYospacePlayer.addEventListener(onAdSkippedListener);

        this.loadLive();

    }

    private void unload() {
        bitmovinYospacePlayer.unload();
    }

    private void loadLive() {
        SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e-ces1eurxaws101j8-6x78eoil2agd.cds1.yospace.com/csm/extlive/yospace02,hlssample.m3u8?yo.br=true&yo.ac=true"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);
        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR);

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadVod() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://vodp-e-turner-eb.tls1.yospace.com/csm/access/152064303/Q01BRl9GTVA0X0RBU0hfQ0VOQ19ITFNfQ0JDU19DRU5DX2Zyb21faW50ZXJfbWFpbl9NYXJyaWVkd2l0aENoaWxkcmVuX0NBUkUxMDA5MjYxNzAwMDE4ODUyX3dpdGhfYnJlYWtzLzNjM2MzYzNjM2MzYzNjM2MzYzNjM2MzYzNjM2MzYzNjMDAwMDAyNjIvbWFzdGVyX3d2Lm0zdTg=?yo.ad=true"));
        // setup DRM handling
        String drmLicenseUrl = "https://widevine.license.istreamplanet.com/widevine/api/license/a229afbf-e1d3-499e-8127-c33cd7231e58";
        UUID drmSchemeUuid = DRMSystems.WIDEVINE_UUID;
        sourceItem.addDRMConfiguration(drmSchemeUuid, drmLicenseUrl);

        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadLinearStartOver() {
        SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e-ces1eurxaws101j8-6x78eoil2agd.cds1.yospace.com/access/d/400/u/0/1/130782300?f=0000130753172&format=vmap"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);
        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR_START_OVER);
        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bitmovinPlayerView.onResume();
        bitmovinYospacePlayer.play();
    }

    @Override
    protected void onPause() {
        bitmovinYospacePlayer.pause();
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
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Ad Break Started", Toast.LENGTH_SHORT).show();
                }
            });

        }
    };

    private OnAdBreakFinishedListener onAdBreakFinishedListener = new OnAdBreakFinishedListener() {
        @Override
        public void onAdBreakFinished(AdBreakFinishedEvent adBreakFinishedEvent) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Ad Break Finished", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private OnAdStartedListener onAdStartedListener = new OnAdStartedListener() {
        @Override
        public void onAdStarted(AdStartedEvent adStartedEvent) {
            String clickThroughUrl = null;
            if (adStartedEvent != null) {
                clickThroughUrl = adStartedEvent.getClickThroughUrl();
            }
            currentClickThroughUrl = clickThroughUrl;

            final String url = currentClickThroughUrl;

            handler.post(new Runnable() {
                public void run() {
                    if (url != null && url != "") {
                        clickThrough.setEnabled(true);
                        clickThrough.setClickable(true);

                    }
                    Toast.makeText(getApplicationContext(), "Ad Started: " + bitmovinYospacePlayer.isAd(), Toast.LENGTH_SHORT).show();
                }
            });

        }
    };

    private OnAdFinishedListener onAdFinishedListener = new OnAdFinishedListener() {
        @Override
        public void onAdFinished(AdFinishedEvent adFinishedEvent) {
            clickThrough.setEnabled(false);
            clickThrough.setClickable(false);
            currentClickThroughUrl = null;

            handler.post(new Runnable() {
                public void run() {
                    clickThrough.setEnabled(false);
                    clickThrough.setClickable(false);
                    Toast.makeText(getApplicationContext(), "Ad Finished: " + bitmovinYospacePlayer.isAd(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private OnAdSkippedListener onAdSkippedListener = new OnAdSkippedListener() {
        @Override
        public void onAdSkipped(AdSkippedEvent adSkippedEvent) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Ad Skipped", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private OnAdErrorListener onAdErrorListener = new OnAdErrorListener() {
        @Override
        public void onAdError(AdErrorEvent adErrorEvent) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Ad Error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private OnAdClickedListener onAdClickedListener = new OnAdClickedListener() {
        @Override
        public void onAdClicked(AdClickedEvent adClickedEvent) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Ad Clicked", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private void clickThroughPressed() {
        bitmovinYospacePlayer.clickThroughPressed();

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
