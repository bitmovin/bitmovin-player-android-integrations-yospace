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
import com.bitmovin.player.api.event.data.ReadyEvent;
import com.bitmovin.player.api.event.data.WarningEvent;
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener;
import com.bitmovin.player.api.event.listener.OnAdClickedListener;
import com.bitmovin.player.api.event.listener.OnAdErrorListener;
import com.bitmovin.player.api.event.listener.OnAdFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdSkippedListener;
import com.bitmovin.player.api.event.listener.OnAdStartedListener;
import com.bitmovin.player.api.event.listener.OnErrorListener;
import com.bitmovin.player.api.event.listener.OnReadyListener;
import com.bitmovin.player.api.event.listener.OnWarningListener;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.media.HLSSource;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.integration.yospace.BitmovinYospacePlayer;
import com.bitmovin.player.integration.yospace.YospaceAssetType;
import com.bitmovin.player.integration.yospace.YospaceLiveInitialisationType;
import com.bitmovin.player.integration.yospace.config.TruexConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration;
import com.bitmovin.player.model.advertising.Ad;
import com.bitmovin.player.model.advertising.AdBreak;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, KeyEvent.Callback {
    private Button liveButton;
    private Button vodButton;
    private Button unloadButton;
    private Button clickThroughButton;
    private Button customButton;
    private Button truexButton;
    private Button seekButton;
    private Spinner assetTypeSpinner;
    private EditText urlInputEditText;
    private EditText seekEditText;
    private BitmovinPlayerView bitmovinPlayerView;

    private BitmovinYospacePlayer bitmovinYospacePlayer;

    private String currentClickThroughUrl;
    private TruexConfiguration trueXConfiguration;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vodButton = findViewById(R.id.vod_button);
        liveButton = findViewById(R.id.live_button);
        unloadButton = findViewById(R.id.unload_button);
        customButton = findViewById(R.id.custom_button);
        truexButton = findViewById(R.id.truex_button);
        seekButton = findViewById(R.id.seek_button);
        clickThroughButton = findViewById(R.id.click_through_button);
        assetTypeSpinner = findViewById(R.id.asset_type_spinner);
        urlInputEditText = findViewById(R.id.url_edit_text);
        seekEditText = findViewById(R.id.seek_edit_text);
        bitmovinPlayerView = findViewById(R.id.bitmovinPlayerView);

        vodButton.setOnClickListener(this);
        liveButton.setOnClickListener(this);
        unloadButton.setOnClickListener(this);
        customButton.setOnClickListener(this);
        truexButton.setOnClickListener(this);
        clickThroughButton.setOnClickListener(this);
        seekButton.setOnClickListener(this);

        PlayerConfiguration playerConfiguration = new PlayerConfiguration();
        YospaceConfiguration yospaceConfiguration = new YospaceConfiguration("", 25000, 25000, 25000, YospaceLiveInitialisationType.DIRECT, true);
        trueXConfiguration = new TruexConfiguration(bitmovinPlayerView, null, null);

        bitmovinYospacePlayer = new BitmovinYospacePlayer(getApplicationContext(), playerConfiguration, yospaceConfiguration);
        bitmovinYospacePlayer.getConfig().getPlaybackConfiguration().setAutoplayEnabled(true);
        bitmovinYospacePlayer.addEventListener(onAdBreakStartedListener);
        bitmovinYospacePlayer.addEventListener(onAdBreakFinishedListener);
        bitmovinYospacePlayer.addEventListener(onAdStartedListener);
        bitmovinYospacePlayer.addEventListener(onAdFinishedListener);
        bitmovinYospacePlayer.addEventListener(onAdClickedListener);
        bitmovinYospacePlayer.addEventListener(onAdErrorListener);
        bitmovinYospacePlayer.addEventListener(onAdSkippedListener);
        bitmovinYospacePlayer.addEventListener(onErrorListener);
        bitmovinYospacePlayer.addEventListener(onReadyListener);
        bitmovinYospacePlayer.addEventListener(onWarningListener);
        bitmovinYospacePlayer.setPlayerPolicy(new BitmovinYospacePolicy(bitmovinYospacePlayer));

        bitmovinPlayerView.setPlayer(bitmovinYospacePlayer);
    }

    private void unload() {
        bitmovinYospacePlayer.unload();
    }

    private void loadLive() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://live-manifests-aka-qa.warnermediacdn.com/csmp/cmaf/live/2000073/cnn-clear-novpaid/master.m3u8"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.LINEAR);

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadVod() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://vod-manifests-aka-qa.warnermediacdn.com/csm/tcm/clear/3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c/master_cl.m3u8?afid=222591187&caid=2100555&conf_csid=tbs.com_mobile_iphone_test&context=181740194&nw=48804&prof=48804:tbs_ios_vod&vdur=1800&yo.vp=false&yo.ad=true"));
        SourceConfiguration sourceConfig = new SourceConfiguration();
        sourceConfig.addSourceItem(sourceItem);

        YospaceSourceConfiguration yospaceSourceConfiguration = new YospaceSourceConfiguration(YospaceAssetType.VOD);

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration);
    }

    private void loadTruex() {
        SourceItem sourceItem = new SourceItem(new HLSSource("https://vod-manifests-aka-qa.warnermediacdn.com/csm/tcm/clear/3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c/master_cl.m3u8?_fw_nielsen_app_id=P923E8EA9-9B1B-4F15-A180-F5A4FD01FE38&afid=222591187&caid=2100555&conf_csid=tbs.com_mobile_androidphone&context=182883174&nw=42448&prof=48804%3Amp4_plus_vast_truex&vdur=1800&yo.vp=true"));
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

    private OnReadyListener onReadyListener = new OnReadyListener() {
        @Override
        public void onReady(ReadyEvent readyEvent) {
            Log.d(TAG, "Ad Breaks - " + bitmovinYospacePlayer.getAdTimeline());
        }
    };

    private OnAdBreakStartedListener onAdBreakStartedListener = new OnAdBreakStartedListener() {
        @Override
        public void onAdBreakStarted(AdBreakStartedEvent adBreakStartedEvent) {
            AdBreak adBreak = adBreakStartedEvent.getAdBreak();
            if (adBreak != null) {
                Toast.makeText(MainActivity.this, "Ad Break Started: id=" + adBreak.getId(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onAdBreakStarted: id=" + adBreak.getId());
            }
        }
    };

    private OnAdBreakFinishedListener onAdBreakFinishedListener = new OnAdBreakFinishedListener() {
        @Override
        public void onAdBreakFinished(AdBreakFinishedEvent adBreakFinishedEvent) {
            AdBreak adBreak = adBreakFinishedEvent.getAdBreak();
            if (adBreak != null) {
                Toast.makeText(MainActivity.this, "Ad Break Finished: id=" + adBreak.getId(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onAdBreakFinished: id=" + adBreak.getId());
            }
        }
    };

    private OnAdStartedListener onAdStartedListener = new OnAdStartedListener() {
        @Override
        public void onAdStarted(AdStartedEvent adStartedEvent) {
            Ad ad = adStartedEvent.getAd();
            if (ad != null) {
                Toast.makeText(MainActivity.this, "Ad Started: id=" + ad.getId(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onAdStarted: id=" + ad.getId());
            }

            currentClickThroughUrl = adStartedEvent.getClickThroughUrl();

            if (currentClickThroughUrl != null && !currentClickThroughUrl.isEmpty()) {
                clickThroughButton.setEnabled(true);
                clickThroughButton.setClickable(true);
            }
        }
    };

    private OnAdFinishedListener onAdFinishedListener = new OnAdFinishedListener() {
        @Override
        public void onAdFinished(AdFinishedEvent adFinishedEvent) {
            Ad ad = adFinishedEvent.getAd();
            if (ad != null) {
                Toast.makeText(MainActivity.this, "Ad Finished: id=" + ad.getId(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onAdFinished: id=" + ad.getId());
            }

            clickThroughButton.setEnabled(false);
            clickThroughButton.setClickable(false);
        }
    };

    private OnAdSkippedListener onAdSkippedListener = new OnAdSkippedListener() {
        @Override
        public void onAdSkipped(AdSkippedEvent adSkippedEvent) {
            Toast.makeText(getApplicationContext(), "Ad Skipped", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onAdSkipped: ");
        }
    };

    private OnAdErrorListener onAdErrorListener = new OnAdErrorListener() {
        @Override
        public void onAdError(AdErrorEvent adErrorEvent) {
            Toast.makeText(getApplicationContext(), "Ad Error", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onAdError: ");
        }
    };

    private OnAdClickedListener onAdClickedListener = new OnAdClickedListener() {
        @Override
        public void onAdClicked(AdClickedEvent adClickedEvent) {
            Toast.makeText(getApplicationContext(), "Ad Clicked", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onAdClicked: ");
        }
    };

    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(ErrorEvent errorEvent) {
            Toast.makeText(getApplicationContext(), "Error - code=" + errorEvent.getCode() + ", message=" + errorEvent.getMessage(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "onError: code=" + errorEvent.getCode() + ", message=" + errorEvent.getMessage());
        }
    };

    private OnWarningListener onWarningListener = new OnWarningListener() {
        @Override
        public void onWarning(WarningEvent warningEvent) {
            Toast.makeText(getApplicationContext(), "On Warning", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onWarning: ");
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
        } else if (v == truexButton) {
            loadTruex();
        } else if (v == seekButton) {
            bitmovinYospacePlayer.seek(Double.valueOf(seekEditText.getText().toString()));
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
