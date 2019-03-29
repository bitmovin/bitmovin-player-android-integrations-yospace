package com.bitmovin.player.integration.yospace;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.ViewGroup;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.api.event.data.AdBreakFinishedEvent;
import com.bitmovin.player.api.event.data.AdBreakStartedEvent;
import com.bitmovin.player.api.event.data.AdFinishedEvent;
import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.bitmovin.player.api.event.data.ErrorEvent;
import com.bitmovin.player.api.event.data.FullscreenEnterEvent;
import com.bitmovin.player.api.event.data.FullscreenExitEvent;
import com.bitmovin.player.api.event.data.MetadataEvent;
import com.bitmovin.player.api.event.data.PausedEvent;
import com.bitmovin.player.api.event.data.PlaybackFinishedEvent;
import com.bitmovin.player.api.event.data.PlayingEvent;
import com.bitmovin.player.api.event.data.ReadyEvent;
import com.bitmovin.player.api.event.data.SourceLoadedEvent;
import com.bitmovin.player.api.event.data.SourceUnloadedEvent;
import com.bitmovin.player.api.event.data.StallEndedEvent;
import com.bitmovin.player.api.event.data.StallStartedEvent;
import com.bitmovin.player.api.event.data.TimeChangedEvent;
import com.bitmovin.player.api.event.data.WarningEvent;
import com.bitmovin.player.api.event.listener.OnFullscreenEnterListener;
import com.bitmovin.player.api.event.listener.OnFullscreenExitListener;
import com.bitmovin.player.api.event.listener.OnMetadataListener;
import com.bitmovin.player.api.event.listener.OnPausedListener;
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener;
import com.bitmovin.player.api.event.listener.OnPlayingListener;
import com.bitmovin.player.api.event.listener.OnReadyListener;
import com.bitmovin.player.api.event.listener.OnSourceLoadedListener;
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener;
import com.bitmovin.player.api.event.listener.OnStallEndedListener;
import com.bitmovin.player.api.event.listener.OnStallStartedListener;
import com.bitmovin.player.api.event.listener.OnTimeChangedListener;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.advertising.AdItem;
import com.bitmovin.player.config.advertising.AdSourceType;
import com.bitmovin.player.config.drm.DRMConfiguration;
import com.bitmovin.player.config.drm.DRMSystems;
import com.bitmovin.player.config.media.HLSSource;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration;
import com.yospace.android.hls.analytic.AnalyticEventListener;
import com.yospace.android.hls.analytic.Session;
import com.yospace.android.hls.analytic.SessionFactory;
import com.yospace.android.hls.analytic.SessionLive;
import com.yospace.android.hls.analytic.SessionNonLinear;
import com.yospace.android.hls.analytic.SessionNonLinearStartOver;
import com.yospace.android.hls.analytic.advert.AdBreak;
import com.yospace.android.hls.analytic.advert.Advert;
import com.yospace.android.xml.VastPayload;
import com.yospace.android.xml.VmapPayload;
import com.yospace.hls.TimedMetadata;
import com.yospace.hls.player.PlaybackState;
import com.yospace.hls.player.PlayerState;
import com.yospace.util.YoLog;
import com.yospace.util.event.Event;
import com.yospace.util.event.EventListener;
import com.yospace.util.event.EventSourceImpl;

import org.apache.commons.lang3.Validate;


public class YospacePlayer extends BitmovinPlayer {
    private Session session;
    private SessionFactory sessionFactory;
    private final EventSourceImpl<PlayerState> stateSource = new EventSourceImpl<>();
    private final EventSourceImpl<TimedMetadata> metadataSource = new EventSourceImpl<>();
    private final BitmovinPlayerPolicy bitmovinPlayerPolicy = new BitmovinPlayerPolicy();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final YospaceEventEmitter yospaceEventEmitter = new YospaceEventEmitter();
    private Session.SessionProperties properties;
    private YospaceSourceConfiguration yospaceSourceConfiguration;
    private YospaceConfiguration yospaceConfiguration;
    private SourceConfiguration sourceConfiguration;
    private YospaceSesssionStatus sessionStatus = YospaceSesssionStatus.NOT_INITIALIZED;
    private String originalUrl;
    private boolean isYospaceAd = false;

    public YospacePlayer(Context context, PlayerConfiguration playerConfiguration, YospaceConfiguration yospaceConfiguration) {
        super(context, playerConfiguration);
        this.yospaceConfiguration = yospaceConfiguration;

        HandlerThread handlerThread = new HandlerThread("BitmovinYospaceHandlerThread");
        handlerThread.start();

        super.addEventListener(onPausedListener);
        super.addEventListener(onPlayingListener);
        super.addEventListener(onPlaybackFinishedListener);
        super.addEventListener(onSourceLoadedListener);
        super.addEventListener(onSourceUnloadedListener);
        super.addEventListener(onStallEndedListener);
        super.addEventListener(onStallStartedListener);
        super.addEventListener(onMetadataListener);
        super.addEventListener(onTimeChangedListener);
        super.addEventListener(onFullscreenEnterListener);
        super.addEventListener(onFullscreenExitListener);
        super.addEventListener(onReadyListener);
    }

    public void load(SourceConfiguration sourceConfiguration, YospaceSourceConfiguration yospaceSourceConfiguration) {
        Validate.notNull(sourceConfiguration, "SourceConfiguration must not be null");
        Validate.notNull(yospaceSourceConfiguration, "YospaceSourceConfiguration must not be null");

        resetYospaceSession();

        Log.d(Constants.TAG, "Load Yospace Source Configuration");
        this.yospaceSourceConfiguration = yospaceSourceConfiguration;
        this.sourceConfiguration = sourceConfiguration;

        SourceItem sourceItem = sourceConfiguration.getFirstSourceItem();
        if (sourceItem == null) {
            yospaceEventEmitter.emit(new ErrorEvent(YospaceErrorCodes.YOSPACE_INVALID_SOURCE, "Invalid Yospace source. You must provide an HLS source"));
            unload();
            return;
        }

        HLSSource hlsSource = sourceItem.getHlsSource();

        if (hlsSource == null || hlsSource.getUrl() == null) {
            yospaceEventEmitter.emit(new ErrorEvent(YospaceErrorCodes.YOSPACE_INVALID_SOURCE, "Invalid Yospace source. You must provide an HLS source"));
            unload();
            return;
        }

        originalUrl = hlsSource.getUrl();
        properties = new Session.SessionProperties(originalUrl).readTimeout(yospaceConfiguration.getReadTimeout()).connectTimeout(yospaceConfiguration.getConnectTimeout()).requestTimeout(yospaceConfiguration.getRequestTimeout());

        if (yospaceConfiguration.getUserAgent() != null) {
            properties.userAgent(yospaceConfiguration.getUserAgent());
        }

        if (yospaceConfiguration.isDebug()) {
            properties.addDebugFlags(YoLog.DEBUG_POLLING | YoLog.DEBUG_ID3TAG | YoLog.DEBUG_PARSING | YoLog.DEBUG_REPORTS | YoLog.DEBUG_HTTP | YoLog.DEBUG_RAW_XML);
        }

        switch (yospaceSourceConfiguration.getAssetType()) {
            case LINEAR:
                loadLive();
                break;
            case VOD:
                loadVod();
                break;
            case LINEAR_START_OVER:
                loadStartOver();
                break;
        }
    }

    private void resetYospaceSession() {
        if (session != null) {
            sessionStatus = YospaceSesssionStatus.NOT_INITIALIZED;
            session.shutdown();

        }
    }

    private void loadLive() {
        sessionFactory = SessionFactory.createForLiveWithThread(sessionEventListener, properties);
        String url = sessionFactory.getPlayerUrl();
        startPlayback(url);
    }

    private void loadVod() {
//        sessionHandler.post(new Runnable() {
//            @Override
//            public void run() {
        SessionNonLinear.create(sessionEventListener, properties);
//            }
//        });
    }

    private void loadStartOver() {
        SessionNonLinearStartOver.create(sessionEventListener, properties);
    }

    private EventListener<Session> sessionEventListener = new EventListener<Session>() {

        public void handle(Event<Session> event) {

            // Retrieve the initialised session
            session = event.getPayload();
            Log.d(Constants.TAG, "Session State: " + session.getState().toString() + " Result Code: " + session.getResultCode());
            switch (session.getState()) {
                case INITIALISED:
                    session.addAnalyticListener(analyticEventListener);
                    session.setPlayerPolicy(new BitmovinPlayerPolicy());
                    Log.i(Constants.TAG, "Session Player Url: " + session.getPlayerUrl());
                    session.setPlayerStateSource(stateSource);
                    session.setPlayerPolicy(bitmovinPlayerPolicy);

                    startPlayback(session.getPlayerUrl());

                    if (session instanceof SessionLive) {
                        ((SessionLive) session).setTimedMetadataSource(metadataSource);
                    } else {
                        startPlayback(session.getPlayerUrl());
                    }

                    return;
                case NO_ANALYTICS:
                    yospaceEventEmitter.emit(new ErrorEvent(YospaceErrorCodes.YOSPACE_NO_ANALYTICS, "Source URL does not refer to a Yospace stream"));
                    startPlayback(sourceConfiguration.getFirstSourceItem().getHlsSource().getUrl());
                    return;
                case NOT_INITIALISED:
                    yospaceEventEmitter.emit(new ErrorEvent(YospaceErrorCodes.YOSPACE_NOT_INITIALISED, "Failed to initialise Yospace stream."));
                    startPlayback(sourceConfiguration.getFirstSourceItem().getHlsSource().getUrl());
                    return;
            }
        }
    };

    private void startPlayback(final String playbackUrl) {
        handler.post(new Runnable() {
            public void run() {
                final SourceConfiguration newSourceConfiguration = new SourceConfiguration();
                SourceItem sourceItem = new SourceItem(new HLSSource(playbackUrl));
                DRMConfiguration drmConfiguration = sourceConfiguration.getFirstSourceItem().getDrmConfiguration(DRMSystems.WIDEVINE_UUID);
                if (drmConfiguration != null) {
                    sourceItem.addDRMConfiguration(drmConfiguration);
                }

                newSourceConfiguration.addSourceItem(sourceItem);
                load(newSourceConfiguration);
            }
        });
    }


    public void clickThroughPressed() {
        session.onLinearClickThrough();
    }

    /**
     * EventListener
     */
    @Override
    public void addEventListener(com.bitmovin.player.api.event.listener.EventListener listener) {
        yospaceEventEmitter.addEventListener(listener);
        super.addEventListener(listener);
    }

    @Override
    public void removeEventListener(com.bitmovin.player.api.event.listener.EventListener listener) {
        yospaceEventEmitter.removeEventListener(listener);
        super.removeEventListener(listener);
    }

    /**
     * Playback Parameters
     */
    @Override
    public void play() {
        super.play();
    }

    @Override
    public void pause() {
        if (session != null) {
            if (session.canPause()) {
                super.pause();
            }
        } else {
            super.pause();
        }
    }

    @Override
    public void seek(double time) {
        if (session != null) {
            if (session.canSeek()) {
                super.seek(time);
            }
        } else {
            super.seek(time);
        }
    }

    @Override
    public void mute() {
        if (session != null) {
            if (session.canMute()) {
                super.mute();
            }
        } else {
            super.mute();
        }
    }

    @Override
    public void timeShift(double offset) {
        super.timeShift(offset);
    }

    @Override
    public boolean isAd() {
        if (yospaceSourceConfiguration != null) {
            return isYospaceAd;
        } else {
            return super.isAd();
        }
    }

    @Override
    public void skipAd() {
        if (yospaceSourceConfiguration != null) {
            //TODO skip ad via yospace
        } else {
            super.skipAd();
        }
    }

    @Override
    public void scheduleAd(AdItem adItem) {
        if (yospaceSourceConfiguration != null) {
            yospaceEventEmitter.emit(new WarningEvent(YospaceWarningCodes.UNSUPPORTED_API, "scheduleAd API is not available when playing back a Yospace asset"));
        } else {
            super.scheduleAd(adItem);
        }
    }

    @Override
    public void setAdViewGroup(ViewGroup adViewGroup) {
        if (yospaceSourceConfiguration != null) {
            yospaceEventEmitter.emit(new WarningEvent(YospaceWarningCodes.UNSUPPORTED_API, "setAdViewGroup API is not available when playing back a Yospace asset"));
        } else {
            super.setAdViewGroup(adViewGroup);
        }
    }

    private int getYospaceTime() {
        int i = (int) Math.round(getCurrentTime() * 1000);
        if (i < 0) {
            i = 0;
        }
        return i;
    }

    /**
     * Player Listeners
     */
    private OnSourceLoadedListener onSourceLoadedListener = new OnSourceLoadedListener() {
        @Override
        public void onSourceLoaded(SourceLoadedEvent sourceLoadedEvent) {
            Log.d(Constants.TAG, "Sending Initialising Event" + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.INITIALISING, getYospaceTime(), false));
        }
    };

    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
        @Override
        public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
            if (sessionStatus != YospaceSesssionStatus.NOT_INITIALIZED) {
                resetYospaceSession();
            }

            Log.d(Constants.TAG, "Sending Stopped Event" + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.STOPPED, getYospaceTime(), false));
        }
    };

    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            Log.d(Constants.TAG, "Sending Finished Event" + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.STOPPED, getYospaceTime(), false));
        }
    };

    private OnPausedListener onPausedListener = new OnPausedListener() {
        @Override
        public void onPaused(PausedEvent pausedEvent) {
            Log.d(Constants.TAG, "Sending Paused Event" + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.PAUSED, getYospaceTime(), false));
        }
    };

    private OnPlayingListener onPlayingListener = new OnPlayingListener() {
        @Override
        public void onPlaying(PlayingEvent playingEvent) {
            Log.d(Constants.TAG, "SendingPlaying Event" + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.PLAYING, getYospaceTime(), false));

            if (session instanceof SessionNonLinear) {
                Log.d(Constants.TAG, "Ad Breaks: " + ((SessionNonLinear) session).getAdBreaks().toString());
            }

            Log.d(Constants.TAG, "Duration: " + getDuration() * 1000 + " - " + getCurrentTime() * 1000);


        }
    };

    private OnReadyListener onReadyListener = new OnReadyListener() {
        @Override
        public void onReady(ReadyEvent readyEvent) {
            sessionStatus = YospaceSesssionStatus.INITIALIZED;
        }
    };

    private OnStallEndedListener onStallEndedListener = new OnStallEndedListener() {
        @Override
        public void onStallEnded(StallEndedEvent stallEndedEvent) {
            Log.d(Constants.TAG, "Sending Stall Ended Event" + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.BUFFERING_END, getYospaceTime(), false));
        }
    };

    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            Log.d(Constants.TAG, "Sending Stall Started Event" + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.BUFFERING_START, getYospaceTime(), false));
        }
    };

    private OnTimeChangedListener onTimeChangedListener = new OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimeChangedEvent timeChangedEvent) {
            if (!(session instanceof SessionLive)) {
                stateSource.notify(new PlayerState(PlaybackState.PLAYHEAD_UPDATE, getYospaceTime(), false));
            }
        }
    };

    private OnFullscreenEnterListener onFullscreenEnterListener = new OnFullscreenEnterListener() {
        @Override
        public void onFullscreenEnter(FullscreenEnterEvent fullscreenEnterEvent) {
            session.onFullScreenModeChange(true);
        }
    };

    private OnFullscreenExitListener onFullscreenExitListener = new OnFullscreenExitListener() {
        @Override
        public void onFullscreenExit(FullscreenExitEvent fullscreenExitEvent) {
            session.onFullScreenModeChange(false);
        }
    };

    private OnMetadataListener onMetadataListener = new OnMetadataListener() {
        @Override
        public void onMetadata(MetadataEvent metadataEvent) {
            TimedMetadata timedMetadata = YospaceUtil.createTimedMetadata(metadataEvent);
            if (timedMetadata != null) {
                Log.d(Constants.TAG, "Metadata - " + timedMetadata.toString());
                metadataSource.notify(timedMetadata);
            }
        }
    };

    /**
     * Yospace Listeners
     */
    private AnalyticEventListener analyticEventListener = new AnalyticEventListener() {
        @Override
        public void onAdvertBreakEnd(AdBreak adBreak) {
            Log.d(Constants.TAG, "OnAdvertBreakEnd: " + adBreak.toString());
            yospaceEventEmitter.emit(new AdBreakFinishedEvent());
        }

        @Override
        public void onAdvertBreakStart(AdBreak adBreak) {
            Log.d(Constants.TAG, "OnAdvertBreakStart: " + adBreak.toString());
            yospaceEventEmitter.emit(new AdBreakStartedEvent());
        }

        @Override
        public void onAdvertEnd(Advert advert) {
            Log.d(Constants.TAG, "OnAdvertEnd: " + advert.getId() + " duration - " + advert.getDuration());
            isYospaceAd = false;
            yospaceEventEmitter.emit(new AdFinishedEvent());
        }

        @Override
        public void onAdvertStart(Advert advert) {
            Log.d(Constants.TAG, "OnAdvertStart: " + advert.getId() + " duration - " + advert.getDuration());
            isYospaceAd = true;
            String clickThroughUrl = "";
            if (advert.getLinearCreative() != null && advert.getLinearCreative().getVideoClicks() != null) {
                clickThroughUrl = advert.getLinearCreative().getVideoClicks().getClickThroughUrl();
            }

            Log.d(Constants.TAG, "Extensions Block: " + advert.getExtensionBlock());

            AdStartedEvent adStartedEvent = new AdStartedEvent(AdSourceType.IMA, clickThroughUrl, advert.getSequence(), advert.getDuration(), advert.getStartMillis() / 1000, "position", 0);
            yospaceEventEmitter.emit(adStartedEvent);
        }

        @Override
        public void onTimelineUpdateReceived(VmapPayload vmapPayload) {
            Log.d(Constants.TAG, "OnTimelineUpdateReceived: " + vmapPayload.toString());
            Log.d(Constants.TAG, vmapPayload.getAdBreaks().toString());
        }

        @Override
        public void onTrackingUrlCalled(Advert advert, String s, String s1) {
            Log.d(Constants.TAG, "OnTrackingUrlCalled: " + s);
        }

        @Override
        public void onVastReceived(VastPayload vastPayload) {
            Log.d(Constants.TAG, "OnVastReceived: " + vastPayload.getRaw());
        }
    };
}
