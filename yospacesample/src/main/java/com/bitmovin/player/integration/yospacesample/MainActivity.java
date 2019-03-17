package com.bitmovin.player.integration.yospacesample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.bitmovin.player.BitmovinPlayerView;
import com.bitmovin.player.api.event.data.AdBreakFinishedEvent;
import com.bitmovin.player.api.event.data.AdBreakStartedEvent;
import com.bitmovin.player.api.event.data.AdClickedEvent;
import com.bitmovin.player.api.event.data.AdErrorEvent;
import com.bitmovin.player.api.event.data.AdFinishedEvent;
import com.bitmovin.player.api.event.data.AdSkippedEvent;
import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener;
import com.bitmovin.player.api.event.listener.OnAdClickedListener;
import com.bitmovin.player.api.event.listener.OnAdErrorListener;
import com.bitmovin.player.api.event.listener.OnAdFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdSkippedListener;
import com.bitmovin.player.api.event.listener.OnAdStartedListener;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.media.HLSSource;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.integration.yospace.YospacePlayer;
import com.bitmovin.player.integration.yospace.YospaceAssetType;
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceConfigurationBuilder;
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration;
import com.bitmovin.player.integration.yospace.config.TrueXConfiguration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, KeyEvent.Callback {
    private BitmovinPlayerView bitmovinPlayerView;
    private YospacePlayer yospacePlayer;
    private Button liveButton;
    private Button vodButton;
    private Button unloadButton;
    private Button clickThrough;
    private Button customButton;
    private Button trueXButton;
    private Button defaultButton;
    private Spinner customSpinner;
    private EditText urlInput;

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
        customButton = findViewById(R.id.custom_button);
        customButton.setOnClickListener(this);
        customSpinner = findViewById(R.id.spinner1);
        urlInput = findViewById(R.id.url_input);
        trueXButton = findViewById(R.id.truex_button);
        trueXButton.setOnClickListener(this);
        defaultButton = findViewById(R.id.default_button);
        defaultButton.setOnClickListener(this);

        // Creating a new PlayerConfiguration
        PlayerConfiguration playerConfiguration = new PlayerConfiguration();

        bitmovinPlayerView = findViewById(R.id.bitmovinPlayerView);

        YospaceConfiguration yospaceConfiguration = new YospaceConfigurationBuilder().setConnectTimeout(25000).setReadTimeout(25000).setRequestTimeout(25000).setDebug(true).build();
        TrueXConfiguration trueXConfiguration = new TrueXConfiguration(bitmovinPlayerView);

        yospacePlayer = new YospacePlayer(getApplicationContext(), playerConfiguration, yospaceConfiguration,trueXConfiguration);
        this.bitmovinPlayerView.setPlayer(yospacePlayer);
        yospacePlayer.getConfig().getPlaybackConfiguration().setAutoplayEnabled(true);

        yospacePlayer.addEventListener(onAdBreakStartedListener);
        yospacePlayer.addEventListener(onAdBreakFinishedListener);
        yospacePlayer.addEventListener(onAdStartedListener);
        yospacePlayer.addEventListener(onAdFinishedListener);
        yospacePlayer.addEventListener(onAdClickedListener);
        yospacePlayer.addEventListener(onAdErrorListener);
        yospacePlayer.addEventListener(onAdSkippedListener);
        yospacePlayer.addEventListener(onErrorListener);

    }

    private void unload() {
        yospacePlayer.unload();
    }

    private void loadLive() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://csm-e-turnerstg-5p30c9t6lfad.tls1.yospace.com/csm/extlive/turnerdev01,tbse-clear.m3u8?yo.ac=true&yo.ch=true&yo.av=2"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);
        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR);

        yospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadVod() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://csm-e-turnerstg-5p30c9t6lfad.tls1.yospace.com/csm/access/525947592/cWEvY21hZl9hZHZhbmNlZF9mbXA0X2Zyb21faW50ZXIvcHJvZ19zZWcvYm9uZXNfUkFEUzEwMDgwNzE4MDAwMjU5NDRfdjEyL2NsZWFyLzNjM2MzYzNjM2MzYzNjM2MzYzNjM2MzYzNjM2MzYzNjL21hc3Rlcl9jbF9pZnAubTN1OA==?yo.av=2&yo.ad=true&yo.ac=true"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);

        yospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadDefault() {
        SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e.cds1.yospace.com/access/d/400/u/0/1/130782300?f=0000130753172&format=vmap"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);
        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);
        yospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadTrueX() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://csm-e-stg.tls1.yospace.com/csm/access/525943851/cWEvY21hZl9hZHZhbmNlZF9mbXA0X2Zyb21faW50ZXIvcHJvZ19zZWcvbXdjX0NBUkUxMDA5MjYxNzAwMDE4ODUyL2NsZWFyLzNjM2MzYzNjM2MzYzNjM2MzYzNjM2MzYzNjM2MzYzNjL21hc3Rlcl9jbF9ub19pZnJhbWUubTN1OA==?yo.av=2"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);

        yospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadLinearStartOver() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://vodp-e-turner-eb.tls1.yospace.com/access/event/latest/110611066?promo=130805986"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR_START_OVER);

        yospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadCustomUrl(){
        String url = urlInput.getText().toString();
        SourceItem sourceItem = new SourceItem(new HLSSource(url));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration;

        if(customSpinner.getSelectedItemPosition() == 0) {
            yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR);
        }else {
            yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);
        }

        yospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bitmovinPlayerView.onResume();
        yospacePlayer.play();
    }

    @Override
    protected void onPause() {
        yospacePlayer.pause();
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

    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(final ErrorEvent errorEvent) {
            handler.post(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Error: " + errorEvent.getCode() + " - " + errorEvent.getMessage(), Toast.LENGTH_LONG).show();
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
                    Toast.makeText(getApplicationContext(), "Ad Started: " + yospacePlayer.isAd(), Toast.LENGTH_SHORT).show();
                }
            });

        }
    };

    private OnAdFinishedListener onAdFinishedListener = new OnAdFinishedListener() {
        @Override
        public void onAdFinished(AdFinishedEvent adFinishedEvent) {
            handler.post(new Runnable() {
                public void run() {
                    clickThrough.setEnabled(false);
                    clickThrough.setClickable(false);
                    Toast.makeText(getApplicationContext(), "Ad Finished: " + yospacePlayer.isAd(), Toast.LENGTH_SHORT).show();
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
        yospacePlayer.clickThroughPressed();

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
        }else if (v == customButton) {
            loadCustomUrl();
        }else if (v == trueXButton) {
            loadTrueX();
        }else if (v == defaultButton) {
            loadDefault();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                // ... handle selections
                handled = true;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                // ... handle left action
                handled = true;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // ... handle right action
                handled = true;
                break;
        }

        return super.onKeyDown(keyCode, event);
    }
}