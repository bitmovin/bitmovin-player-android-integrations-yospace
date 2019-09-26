package com.bitmovin.player.integration.yospace;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.ViewGroup;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.api.event.data.AdBreakFinishedEvent;
import com.bitmovin.player.api.event.data.AdBreakStartedEvent;
import com.bitmovin.player.api.event.data.AdFinishedEvent;
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
import com.bitmovin.player.integration.yospace.config.TrueXConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration;
import com.truex.adrenderer.IEventHandler;
import com.truex.adrenderer.TruexAdRenderer;
import com.truex.adrenderer.TruexAdRendererConstants;
import com.yospace.android.hls.analytic.AnalyticEventListener;
import com.yospace.android.hls.analytic.Session;
import com.yospace.android.hls.analytic.SessionFactory;
import com.yospace.android.hls.analytic.SessionLive;
import com.yospace.android.hls.analytic.SessionNonLinear;
import com.yospace.android.hls.analytic.SessionNonLinearStartOver;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class BitmovinYospacePlayer extends BitmovinPlayer {
    private Session session;
    private SessionFactory sessionFactory;
    private final EventSourceImpl<PlayerState> stateSource = new EventSourceImpl<>();
    private final EventSourceImpl<TimedMetadata> metadataSource = new EventSourceImpl<>();
    private final YospacePlayerPolicy yospacePlayerPolicy;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final YospaceEventEmitter yospaceEventEmitter = new YospaceEventEmitter();
    private Session.SessionProperties properties;
    private YospaceSourceConfiguration yospaceSourceConfiguration;
    private YospaceConfiguration yospaceConfiguration;
    private SourceConfiguration sourceConfiguration;
    private TrueXConfiguration trueXConfiguration;
    private YospaceSesssionStatus sessionStatus = YospaceSesssionStatus.NOT_INITIALIZED;
    private String originalUrl;
    private boolean isYospaceAd = false;
    private TruexAdRenderer truexAdRenderer;
    private Context context;
    private boolean adFree = false;
    private boolean isTrueXRendering = false;
    private AdTimeline adTimeline;
    private Ad liveAd;
    private AdBreak liveAdBreak;
    private UI_LOADING_STATE uiLoadingState;

    private enum UI_LOADING_STATE {
        LOADING,
        UNLOADING,
        UNKNOWN
    }

    public BitmovinYospacePlayer(Context context, PlayerConfiguration playerConfiguration, YospaceConfiguration yospaceConfiguration) {
        super(context, playerConfiguration);
        this.context = context;
        this.yospaceConfiguration = yospaceConfiguration;
        this.yospacePlayerPolicy = new YospacePlayerPolicy(new DefaultBitmovinYospacePlayerPolicy(this));
        this.uiLoadingState = UI_LOADING_STATE.UNKNOWN;
        updateLogVisibility(yospaceConfiguration.isDebug());

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
        load(sourceConfiguration, yospaceSourceConfiguration, null);
    }

    public void load(SourceConfiguration sourceConfiguration, YospaceSourceConfiguration yospaceSourceConfiguration, TrueXConfiguration trueXConfiguration) {
        Validate.notNull(sourceConfiguration, "SourceConfiguration must not be null");
        Validate.notNull(yospaceSourceConfiguration, "YospaceSourceConfiguration must not be null");

        uiLoadingState = UI_LOADING_STATE.LOADING;

        this.trueXConfiguration = trueXConfiguration;
        if (trueXConfiguration == null) {
            truexAdRenderer = null;
        }

        resetYospaceSession();

        BitmovinLogger.d(Constants.TAG, "Load Yospace Source Configuration");
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

    @Override
    public void unload() {
        uiLoadingState = UI_LOADING_STATE.UNLOADING;
        super.unload();
    }

    public double currentTimeWithAds() {
        return super.getCurrentTime();
    }

    public Ad getActiveAd() {
        if (isLive()) {
            return liveAd;
        } else if (adTimeline != null) {
            return adTimeline.currentAd(this.currentTimeWithAds());
        } else {
            return null;
        }
    }

    public AdBreak getActiveAdBreak() {
        if (isLive()) {
            return liveAdBreak;
        } else if (adTimeline != null) {
            return adTimeline.currentAdBreak(this.currentTimeWithAds());
        } else {
            return null;
        }
    }

    public void setPlayerPolicy(BitmovinYospacePlayerPolicy bitmovinYospacePlayerPolicy) {
        this.yospacePlayerPolicy.setPlayerPolicy(bitmovinYospacePlayerPolicy);
    }

    private void resetYospaceSession() {
        if (session != null) {
            sessionStatus = YospaceSesssionStatus.NOT_INITIALIZED;
            session.removeAnalyticListener(analyticEventListener);
            session.shutdown();
            session = null;
            super.unload();
        }
        isYospaceAd = false;
        adFree = false;
        liveAd = null;
        liveAdBreak = null;
        adTimeline = null;
        isTrueXRendering = false;
    }

    private void loadLive() {
        sessionFactory = SessionFactory.createForLiveWithThread(sessionEventListener, properties);
        String url = sessionFactory.getPlayerUrl();
        startPlayback(url);
    }

    private void loadVod() {
        SessionNonLinear.create(sessionEventListener, properties);
    }

    private void loadStartOver() {
        SessionNonLinearStartOver.create(sessionEventListener, properties);
    }

    private void updateLogVisibility(boolean isLoggingEnabled) {
        if (isLoggingEnabled) {
            BitmovinLogger.enableLogging();
        } else {
            BitmovinLogger.disableLogging();
        }
    }

    private EventListener<Session> sessionEventListener = new EventListener<Session>() {

        public void handle(Event<Session> event) {

            // Retrieve the initialised session
            session = event.getPayload();
            BitmovinLogger.d(Constants.TAG, "Session State: " + session.getState().toString() + " Result Code: " + session.getResultCode());
            switch (session.getState()) {
                case INITIALISED:
                    session.addAnalyticListener(analyticEventListener);
                    BitmovinLogger.i(Constants.TAG, "Yospace Session Initialized. Session Player Url: " + session.getPlayerUrl());
                    session.setPlayerStateSource(stateSource);
                    session.setPlayerPolicy(yospacePlayerPolicy);
                    if (session instanceof SessionLive) {
                        ((SessionLive) session).setTimedMetadataSource(metadataSource);
                    } else {
                        startPlayback(session.getPlayerUrl());
                    }
                    return;
                case NO_ANALYTICS:
                    handleYospaceSessionFailure(YospaceErrorCodes.YOSPACE_NO_ANALYTICS, "Source URL does not refer to a Yospace stream");
                    return;
                case NOT_INITIALISED:
                    handleYospaceSessionFailure(YospaceErrorCodes.YOSPACE_NOT_INITIALISED, "Failed to initialise Yospace stream.");
                    return;
            }
        }
    };

    private void handleYospaceSessionFailure(int yospaceErrorCode, String message) {
        if (yospaceSourceConfiguration.shouldRetryExcludingYospace()) {
            handler.post(new Runnable() {
                public void run() {
                    yospaceEventEmitter.emit(new WarningEvent(yospaceErrorCode, message));
                    if (uiLoadingState != UI_LOADING_STATE.UNLOADING) {
                        load(sourceConfiguration);
                    }
                }
            });
        } else {
            BitmovinLogger.i(Constants.TAG, "Yospace Session failed, shutting down playback");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    yospaceEventEmitter.emit(new ErrorEvent(yospaceErrorCode, message));
                }
            });
        }
    }

    private void startPlayback(final String playbackUrl) {
        if (uiLoadingState != UI_LOADING_STATE.UNLOADING) {
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
    }

    public AdTimeline getAdTimeline() {
        return adTimeline;
    }

    /**
     * TrueXRendering
     */
    private void renderTrueXAd(String creativeURL, String adParameters) {
        try {
            pause();
            JSONObject adParams = new JSONObject(adParameters);
            truexAdRenderer = new TruexAdRenderer(context);
            truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_STARTED, this.adStartedListener);
            truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_COMPLETED, this.adCompletedListener);
            truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_ERROR, this.adErrorListener);
            truexAdRenderer.addEventListener(TruexAdRendererConstants.NO_ADS_AVAILABLE, this.noAdsListener);
            truexAdRenderer.addEventListener(TruexAdRendererConstants.AD_FREE_POD, this.adFreeListener);
            truexAdRenderer.addEventListener(TruexAdRendererConstants.POPUP_WEBSITE, this.popupListener);
            truexAdRenderer.init(creativeURL, adParams, TruexAdRendererConstants.PREROLL);
            truexAdRenderer.start(trueXConfiguration.getViewGroup());
            isTrueXRendering = true;
            BitmovinLogger.d(Constants.TAG, "TrueX Ad rendered successfully");
        } catch (JSONException e) {
            BitmovinLogger.e(Constants.TAG, "Failed to render TrueX Ad: " + e);
        }
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

        if (!(listener instanceof OnTimeChangedListener)) {
            super.addEventListener(listener);
        }
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
    public double getDuration() {
        double adBreakDurations = 0;
        if (adTimeline != null) {
            adBreakDurations = adTimeline.totalAdBreakDurations();
        }
        return super.getDuration() - adBreakDurations;
    }

    @Override
    public double getCurrentTime() {
        if (isAd()) {
            if (isLive()) {
                if (liveAd != null) {
                    return super.getCurrentTime() - liveAd.getRelativeStart();
                } else {
                    return super.getCurrentTime();
                }
            } else {
                if (adTimeline != null) {
                    return adTimeline.adTime(super.getCurrentTime());
                } else {
                    return super.getCurrentTime();
                }
            }
        } else {
            if (adTimeline != null) {
                return adTimeline.absoluteToRelative(super.getCurrentTime());
            } else {
                return super.getCurrentTime();
            }
        }
    }

    @Override
    public void seek(double time) {
        if (session != null) {
            if (session.canSeek()) {
                if (adTimeline != null) {
                    long seekTime = session.willSeekTo((long) time);
                    double absoluteSeekTime = adTimeline.relativeToAbsolute(seekTime);
                    BitmovinLogger.d(Constants.TAG, "Seeking to " + absoluteSeekTime);
                    super.seek(absoluteSeekTime);
                } else {
                    BitmovinLogger.d(Constants.TAG, "Seeking to " + time);
                    super.seek(time);
                }
            }
        } else {
            BitmovinLogger.d(Constants.TAG, "Seeking to " + time);
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
        int i = (int) Math.round(currentTimeWithAds() * 1000);
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
            BitmovinLogger.d(Constants.TAG, "Sending Initialising Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.INITIALISING, getYospaceTime(), false));
            if (session instanceof SessionNonLinear) {
                List<com.yospace.android.hls.analytic.advert.AdBreak> adBreaks = ((SessionNonLinear) session).getAdBreaks();
                if (adBreaks != null) {
                    BitmovinLogger.d(Constants.TAG, "Ad Breaks: " + adBreaks.toString());
                    adTimeline = new AdTimeline(adBreaks);
                    BitmovinLogger.d(Constants.TAG, adTimeline.toString());
                }
            }
        }
    };

    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
        @Override
        public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
            if (sessionStatus != YospaceSesssionStatus.NOT_INITIALIZED) {
                BitmovinLogger.d(Constants.TAG, "Sending Stopped Event: " + getYospaceTime());
                stateSource.notify(new PlayerState(PlaybackState.STOPPED, getYospaceTime(), false));
                resetYospaceSession();
            }
        }
    };

    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            BitmovinLogger.d(Constants.TAG, "Sending Stopped Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.STOPPED, getYospaceTime(), false));
        }
    };

    private OnPausedListener onPausedListener = new OnPausedListener() {
        @Override
        public void onPaused(PausedEvent pausedEvent) {
            BitmovinLogger.d(Constants.TAG, "Sending Paused Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.PAUSED, getYospaceTime(), false));
        }
    };

    private OnPlayingListener onPlayingListener = new OnPlayingListener() {
        @Override
        public void onPlaying(PlayingEvent playingEvent) {
            BitmovinLogger.d(Constants.TAG, "Sending Playing Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.PLAYING, getYospaceTime(), false));
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
            BitmovinLogger.d(Constants.TAG, "Sending Stall Ended Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.BUFFERING_END, getYospaceTime(), false));
        }
    };

    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            BitmovinLogger.d(Constants.TAG, "Sending Stall Started Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.BUFFERING_START, getYospaceTime(), false));
        }
    };

    private OnTimeChangedListener onTimeChangedListener = new OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimeChangedEvent timeChangedEvent) {
            if (session != null && getAdTimeline() != null) {
                // Notify Yospace of the Time Update
                if (!(session instanceof SessionLive)) {
                    // BitmovinLogger.d(Constants.TAG, "Sending Playhead Update Event" + getYospaceTime());
                    stateSource.notify(new PlayerState(PlaybackState.PLAYHEAD_UPDATE, getYospaceTime(), false));
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isYospaceAd) {
                            // If we are in a Yospace ad, send the ad time
                            double adTime = getAdTimeline().adTime(timeChangedEvent.getTime());
                            // BitmovinLogger.d(Constants.TAG, "Emitting TimeChangedEvent");
                            yospaceEventEmitter.emit(new TimeChangedEvent(adTime));
                        } else {
                            // If we are not in an ad, send converted relative time
                            double relativeTime = getAdTimeline().absoluteToRelative(timeChangedEvent.getTime());
                            // BitmovinLogger.d(Constants.TAG, "Emitting TimeChangedEvent");
                            yospaceEventEmitter.emit(new TimeChangedEvent(relativeTime));
                        }
                    }
                });
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // BitmovinLogger.d(Constants.TAG, "Emitting TimeChangedEvent");
                        yospaceEventEmitter.emit(timeChangedEvent);
                    }
                });
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
            if (yospaceSourceConfiguration.getAssetType() == YospaceAssetType.LINEAR) {
                TimedMetadata timedMetadata = YospaceUtil.createTimedMetadata(metadataEvent);
                if (timedMetadata != null) {
                    BitmovinLogger.d(Constants.TAG, "Sending Metadata Event: " + timedMetadata.toString());
                    metadataSource.notify(timedMetadata);
                }
            }
        }
    };

    /**
     * TrueX Event Handlers
     */
    private IEventHandler adStartedListener = new IEventHandler() {
        @Override
        public void handleEvent(Map<String, ?> data) {
            BitmovinLogger.d(Constants.TAG, "TrueX - Ad started");
            YospaceAdStartedEvent adStartedEvent;
            Ad activeAd = getActiveAd();
            if (activeAd != null) {
                adStartedEvent = YospaceUtil.createAdStartEvent(AdSourceType.UNKNOWN, activeAd.getClickThroughUrl(), activeAd.getSequence(), activeAd.getDuration(), activeAd.getRelativeStart(), "position", 0, activeAd.isTrueX());
            } else {
                adStartedEvent = YospaceUtil.createAdStartEvent(AdSourceType.UNKNOWN, "", 0, 0, 0, "0", 0, true);
            }
            BitmovinLogger.d(Constants.TAG, "Emitting AdBreakStartedEvent");
            yospaceEventEmitter.emit(new AdBreakStartedEvent());
            BitmovinLogger.d(Constants.TAG, "Emitting AdStartedEvent");
            yospaceEventEmitter.emit(adStartedEvent);
            isYospaceAd = true;
            pause();
        }
    };

    private IEventHandler adCompletedListener = new IEventHandler() {
        @Override
        public void handleEvent(Map<String, ?> data) {
            BitmovinLogger.d(Constants.TAG, "TrueX - Ad completed");
            isYospaceAd = false;
            if (!adFree) {
                BitmovinLogger.d(Constants.TAG, "Emitting AdFinishedEvent");
                yospaceEventEmitter.emit(new AdFinishedEvent());
                play();
            }
        }
    };

    private IEventHandler adErrorListener = new IEventHandler() {
        @Override
        public void handleEvent(Map<String, ?> data) {
            BitmovinLogger.d(Constants.TAG, "TrueX - Ad error");
            isYospaceAd = false;
            play();
        }
    };

    private IEventHandler noAdsListener = new IEventHandler() {
        @Override
        public void handleEvent(Map<String, ?> data) {
            BitmovinLogger.d(Constants.TAG, "TrueX - No ads found");
            isYospaceAd = false;
            play();
        }
    };

    private IEventHandler popupListener = new IEventHandler() {
        @Override
        public void handleEvent(Map<String, ?> data) {
            BitmovinLogger.d(Constants.TAG, "TrueX - Popup");
        }
    };

    private IEventHandler adFreeListener = new IEventHandler() {
        @Override
        public void handleEvent(Map<String, ?> data) {
            BitmovinLogger.d(Constants.TAG, "TrueX - Ad free");
            adFree = true;
            Ad currentAd = getActiveAd();
            if (currentAd != null) {
                BitmovinLogger.d(Constants.TAG, "Emitting AdFinishedEvent");
                yospaceEventEmitter.emit(new AdFinishedEvent());
                BitmovinLogger.d(Constants.TAG, "Emitting AdBreakFinishedEvent");
                yospaceEventEmitter.emit(new AdBreakFinishedEvent());
                // Seek to end of underlying non-TrueX ad
                seek(currentAd.getAbsoluteEnd() + 1);
                play();
            }
        }
    };

    /**
     * Yospace Listeners
     */
    private AnalyticEventListener analyticEventListener = new AnalyticEventListener() {
        @Override
        public void onAdvertBreakStart(com.yospace.android.hls.analytic.advert.AdBreak adBreak) {
            if (adFree) {
                BitmovinLogger.d(Constants.TAG, "Skipping Ad Break due to TrueX ad free experience");
                seek(getCurrentTime() + 1);
            } else {
                if (trueXConfiguration != null) {
                    // Render TrueX ad if found in ad break
                    for (Advert advert : adBreak.getAdverts()) {
                        if (advert.getAdSystem().getAdSystemType().equals("trueX")) {
                            String creativeUrl = "https://qa-get.truex.com/07d5fe7cc7f9b5ab86112433cf0a83b6fb41b092/vast?dimension_2=0&stream_position=midroll&network_user_id=" + trueXConfiguration.getUserId();
                            BitmovinLogger.d(Constants.TAG, "TrueX Ad Found - Source:" + creativeUrl);
                            String adParams = "{\"user_id\":\"" + trueXConfiguration.getUserId() + "\",\"placement_hash\":\"07d5fe7cc7f9b5ab86112433cf0a83b6fb41b092\",\"vast_config_url\":\"" + trueXConfiguration.getVastConfigUrl() + "\"}";
                            BitmovinLogger.d(Constants.TAG, "Rendering TrueX Ad: " + advert.toString());
                            renderTrueXAd(creativeUrl, adParams);
                        }
                    }
                }
                if (!isTrueXRendering) {
                    // No TrueX ad present in ad break, so handle ad events here
                    if (isLive()) {
                        String adId = adBreak.toString() + System.currentTimeMillis();
                        double absoluteTime = BitmovinYospacePlayer.super.getCurrentTime();
                        liveAdBreak = new AdBreak(adId, absoluteTime, adBreak.getDuration() / 1000.0, absoluteTime, (adBreak.getStartMillis() + adBreak.getDuration()) / 1000.0);
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            BitmovinLogger.d(Constants.TAG, "Emitting AdBreakStartedEvent");
                            yospaceEventEmitter.emit(new AdBreakStartedEvent());
                        }
                    });
                }
            }
        }

        @Override
        public void onAdvertBreakEnd(com.yospace.android.hls.analytic.advert.AdBreak adBreak) {
            if (!isTrueXRendering && !adFree) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        BitmovinLogger.d(Constants.TAG, "Emitting AdBreakFinishedEvent");
                        yospaceEventEmitter.emit(new AdBreakFinishedEvent());
                    }
                });
            }
            liveAdBreak = null;
            isTrueXRendering = false;
        }

        @Override
        public void onAdvertStart(Advert advert) {
            if (adFree) {
                BitmovinLogger.d(Constants.TAG, "Skipping Ad Break due to TrueX ad free experience");
                seek(getCurrentTime() + 1);
            } else {
                if (!isTrueXRendering) {
                    isYospaceAd = true;
                    String clickThroughUrl = YospaceUtil.getAdClickThroughUrl(advert);
                    double absoluteTime = BitmovinYospacePlayer.super.getCurrentTime();
                    boolean isTrueX = advert.getAdSystem().getAdSystemType().equals("trueX");
                    liveAd = new Ad(advert.getIdentifier(), absoluteTime, advert.getDuration() / 1000.0, absoluteTime, (advert.getStartMillis() + advert.getDuration()) / 1000.0, advert.getSequence(), clickThroughUrl, advert.hasLinearInteractiveUnit(), isTrueX);
                    YospaceAdStartedEvent adStartedEvent = YospaceUtil.createAdStartEvent(AdSourceType.UNKNOWN, clickThroughUrl, advert.getSequence(), advert.getDuration(), advert.getStartMillis(), "position", 0, isTrueX);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            BitmovinLogger.d(Constants.TAG, "Emitting AdStartedEvent");
                            yospaceEventEmitter.emit(adStartedEvent);
                        }
                    });
                }
            }
        }

        @Override
        public void onAdvertEnd(Advert advert) {
            isYospaceAd = false;
            if (!isTrueXRendering && !adFree) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        BitmovinLogger.d(Constants.TAG, "Emitting AdFinishedEvent");
                        yospaceEventEmitter.emit(new AdFinishedEvent());
                    }
                });
            }
            liveAd = null;
            isTrueXRendering = false;
        }

        @Override
        public void onTimelineUpdateReceived(VmapPayload vmapPayload) {
            BitmovinLogger.d(Constants.TAG, "onTimelineUpdateReceived: ");
        }

        @Override
        public void onTrackingUrlCalled(Advert advert, String s, String s1) {
            BitmovinLogger.d(Constants.TAG, "OnTrackingUrlCalled: " + s);
        }

        @Override
        public void onVastReceived(VastPayload vastPayload) {
            BitmovinLogger.d(Constants.TAG, "OnVastReceived: " + vastPayload.getRaw());
        }
    };
}
