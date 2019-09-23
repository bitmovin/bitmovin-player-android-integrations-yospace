package com.bitmovin.player.integration.yospacesample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
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
import com.bitmovin.player.api.event.data.PlayingEvent;
import com.bitmovin.player.api.event.data.WarningEvent;
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener;
import com.bitmovin.player.api.event.listener.OnAdClickedListener;
import com.bitmovin.player.api.event.listener.OnAdErrorListener;
import com.bitmovin.player.api.event.listener.OnAdFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdSkippedListener;
import com.bitmovin.player.api.event.listener.OnAdStartedListener;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnPlayingListener;
import com.bitmovin.player.api.event.listener.OnWarningListener;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.media.HLSSource;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.integration.yospace.Ad;
import com.bitmovin.player.integration.yospace.BitmovinYospacePlayer;
import com.bitmovin.player.integration.yospace.YospaceAssetType;
import com.bitmovin.player.integration.yospace.config.TrueXConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceConfigurationBuilder;
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, KeyEvent.Callback {
    private BitmovinPlayerView bitmovinPlayerView;
    private BitmovinYospacePlayer bitmovinYospacePlayer;
    private Button liveButton;
    private Button vodButton;
    private Button unloadButton;
    private Button clickThroughButton;
    private Button customButton;
    private Button trueXButton;
    private Button defaultButton;
    private Spinner assetTypeSpinner;
    private EditText urlInputEditText;

    private String currentClickThroughUrl;
    private TrueXConfiguration trueXConfiguration;

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
        clickThroughButton = findViewById(R.id.click_through_button);
        clickThroughButton.setOnClickListener(this);
        customButton = findViewById(R.id.custom_button);
        customButton.setOnClickListener(this);
        trueXButton = findViewById(R.id.truex_button);
        trueXButton.setOnClickListener(this);
        defaultButton = findViewById(R.id.default_button);
        defaultButton.setOnClickListener(this);
        assetTypeSpinner = findViewById(R.id.asset_type_spinner);
        urlInputEditText = findViewById(R.id.url_edit_text);

        // Creating a new PlayerConfiguration
        PlayerConfiguration playerConfiguration = new PlayerConfiguration();

        bitmovinPlayerView = findViewById(R.id.bitmovinPlayerView);

        YospaceConfiguration yospaceConfiguration = new YospaceConfigurationBuilder().setConnectTimeout(25000).setReadTimeout(25000).setRequestTimeout(25000).setDebug(true).build();
        String userId = "turner_bm_ys_tester_001";
        String vastConfigUrl = "qa-get.truex.com/07d5fe7cc7f9b5ab86112433cf0a83b6fb41b092/vast/config?asnw=&cpx_url=&dimension_2=0&flag=%2Bamcb%2Bemcr%2Bslcb%2Bvicb%2Baeti-exvt&fw_key_values=&metr=0&network_user_id=turner_bm_ys_tester_001&prof=g_as3_truex&ptgt=a&pvrn=&resp=vmap1&slid=fw_truex&ssnw=&stream_position=midroll&vdur=&vprn=";
        trueXConfiguration = new TrueXConfiguration(bitmovinPlayerView, userId, vastConfigUrl);

        bitmovinYospacePlayer = new BitmovinYospacePlayer(getApplicationContext(), playerConfiguration, yospaceConfiguration);
        this.bitmovinPlayerView.setPlayer(bitmovinYospacePlayer);
        bitmovinYospacePlayer.getConfig().getPlaybackConfiguration().setAutoplayEnabled(true);

        bitmovinYospacePlayer.addEventListener(onAdBreakStartedListener);
        bitmovinYospacePlayer.addEventListener(onAdBreakFinishedListener);
        bitmovinYospacePlayer.addEventListener(onAdStartedListener);
        bitmovinYospacePlayer.addEventListener(onAdFinishedListener);
        bitmovinYospacePlayer.addEventListener(onAdClickedListener);
        bitmovinYospacePlayer.addEventListener(onAdErrorListener);
        bitmovinYospacePlayer.addEventListener(onAdSkippedListener);
        bitmovinYospacePlayer.addEventListener(onErrorListener);
        bitmovinYospacePlayer.addEventListener(onPlayingListener);
        bitmovinYospacePlayer.addEventListener(onWarningListener);

        bitmovinYospacePlayer.setPlayerPolicy(new BitmovinYospacePolicy(bitmovinYospacePlayer));
    }

    private void unload() {
        bitmovinYospacePlayer.unload();
    }

    private void loadLive() {
        SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e.cds1.yospace.com/csm/live/158519304.m3u8?yo.ac=false"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);
        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR);

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadVod() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://turnercmaf.cdn.turner.com/csm/qa/cmaf_advanced_fmp4_from_inter/prog_seg/bones_RADS1008071800025944_v12/clear/3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c/master_cl_ifp.m3u8?context=525947592"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadDefault() {
        SourceItem sourceItem = new SourceItem(new HLSSource("http://csm-e.cds1.yospace.com/access/d/400/u/0/1/130782300?f=0000130753172&format=vmap"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);
        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);
        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadTrueX() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://turnercmaf.cdn.turner.com/csm/qa/cmaf_advanced_fmp4_from_inter/prog_seg/bones_RADS1008071800025944_v12/clear/3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c/master_cl_ifp.m3u8?context=525955018"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration, trueXConfiguration);
    }

    private void loadCustomUrl() {
        String url = urlInputEditText.getText().toString();
        SourceItem sourceItem = new SourceItem(new HLSSource(url));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration;

        if (assetTypeSpinner.getSelectedItemPosition() == 0) {
            yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR);
        } else {
            yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);
        }

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
//            Toast.makeText(getApplicationContext(), "Ad Break Started", Toast.LENGTH_SHORT).show();
        }
    };

    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(final ErrorEvent errorEvent) {
            Toast.makeText(getApplicationContext(), "Error: " + errorEvent.getCode() + " - " + errorEvent.getMessage(), Toast.LENGTH_LONG).show();
        }
    };

    private OnAdBreakFinishedListener onAdBreakFinishedListener = new OnAdBreakFinishedListener() {
        @Override
        public void onAdBreakFinished(AdBreakFinishedEvent adBreakFinishedEvent) {
            Toast.makeText(getApplicationContext(), "Ad Break Finished", Toast.LENGTH_SHORT).show();
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

            String url = currentClickThroughUrl;

            if (url != null && !url.isEmpty()) {
                clickThroughButton.setEnabled(true);
                clickThroughButton.setClickable(true);
            }
            Ad activeAd = bitmovinYospacePlayer.getActiveAd();
            if (activeAd != null) {
                Toast.makeText(getApplicationContext(), "Ad Started: " + activeAd.getIdentifier(), Toast.LENGTH_SHORT).show();
            }
        }
    };

    private OnAdFinishedListener onAdFinishedListener = new OnAdFinishedListener() {
        @Override
        public void onAdFinished(AdFinishedEvent adFinishedEvent) {
            clickThroughButton.setEnabled(false);
            clickThroughButton.setClickable(false);
            Toast.makeText(getApplicationContext(), "Ad Finished: " + bitmovinYospacePlayer.isAd(), Toast.LENGTH_SHORT).show();
        }
    };

    private OnAdSkippedListener onAdSkippedListener = new OnAdSkippedListener() {
        @Override
        public void onAdSkipped(AdSkippedEvent adSkippedEvent) {
            Toast.makeText(getApplicationContext(), "Ad Skipped", Toast.LENGTH_SHORT).show();
        }
    };

    private OnPlayingListener onPlayingListener = new OnPlayingListener() {
        @Override
        public void onPlaying(PlayingEvent playingEvent) {
            Log.d("BitmovinYospaceSample", "ad breaks - " + bitmovinYospacePlayer.getAdTimeline().toString());
        }
    };

    private OnAdErrorListener onAdErrorListener = new OnAdErrorListener() {
        @Override
        public void onAdError(AdErrorEvent adErrorEvent) {
            Toast.makeText(getApplicationContext(), "Ad Error", Toast.LENGTH_SHORT).show();
        }
    };

    private OnAdClickedListener onAdClickedListener = new OnAdClickedListener() {
        @Override
        public void onAdClicked(AdClickedEvent adClickedEvent) {
            Toast.makeText(getApplicationContext(), "Ad Clicked", Toast.LENGTH_SHORT).show();
        }
    };

    private OnWarningListener onWarningListener = new OnWarningListener() {
        @Override
        public void onWarning(WarningEvent warningEvent) {
            Toast.makeText(getApplicationContext(), "On Warning", Toast.LENGTH_SHORT).show();
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
        } else if (v == clickThroughButton) {
            clickThroughPressed();
        } else if (v == customButton) {
            loadCustomUrl();
        } else if (v == trueXButton) {
            loadTrueX();
        } else if (v == defaultButton) {
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
