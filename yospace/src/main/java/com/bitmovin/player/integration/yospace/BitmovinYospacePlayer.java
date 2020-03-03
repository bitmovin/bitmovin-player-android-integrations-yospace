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
import com.bitmovin.player.api.event.data.AdSkippedEvent;
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
import com.bitmovin.player.integration.yospace.util.AdvertUtilKt;
import com.bitmovin.player.integration.yospace.config.TruexConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration;
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration;
import com.bitmovin.player.integration.yospace.util.MetadataUtilKt;
import com.yospace.android.hls.analytic.AnalyticEventListener;
import com.yospace.android.hls.analytic.Session;
import com.yospace.android.hls.analytic.SessionFactory;
import com.yospace.android.hls.analytic.SessionLive;
import com.yospace.android.hls.analytic.SessionNonLinear;
import com.yospace.android.hls.analytic.SessionNonLinearStartOver;
import com.yospace.android.hls.analytic.advert.Advert;
import com.yospace.android.hls.analytic.advert.InteractiveUnit;
import com.yospace.android.hls.analytic.advert.LinearCreative;
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

import java.util.ArrayList;
import java.util.List;

public class BitmovinYospacePlayer extends BitmovinPlayer {
    private Session session;
    private final EventSourceImpl<PlayerState> stateSource = new EventSourceImpl<>();
    private final EventSourceImpl<TimedMetadata> metadataSource = new EventSourceImpl<>();
    private final YospacePlayerPolicy yospacePlayerPolicy;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final YospaceEventEmitter yospaceEventEmitter = new YospaceEventEmitter();
    private Session.SessionProperties properties;
    private YospaceSourceConfiguration yospaceSourceConfiguration;
    private YospaceConfiguration yospaceConfiguration;
    private SourceConfiguration sourceConfiguration;
    private YospaceSesssionStatus sessionStatus = YospaceSesssionStatus.NOT_INITIALIZED;
    private BitmovinTruexRenderer bitmovinTruexRenderer;
    private Context context;
    private boolean isSessionAdFree;
    private AdTimeline adTimeline;
    private Ad activeAd;
    private AdBreak activeAdBreak;
    private boolean isLiveAdPaused;
    private UI_LOADING_STATE uiLoadingState;
    private boolean isPlayingEventSent;
    private List<TimedMetadata> timedMetadataEvents = new ArrayList<>();

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
        BitLog.INSTANCE.setEnabled(yospaceConfiguration.isDebug());

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

        BitLog.INSTANCE.d("Version 1.0.8");
    }

    public void load(SourceConfiguration sourceConfiguration, YospaceSourceConfiguration yospaceSourceConfiguration) {
        load(sourceConfiguration, yospaceSourceConfiguration, null);
    }

    public void load(SourceConfiguration sourceConfiguration, YospaceSourceConfiguration yospaceSourceConfiguration, TruexConfiguration truexConfiguration) {
        Validate.notNull(sourceConfiguration, "SourceConfiguration must not be null");
        Validate.notNull(yospaceSourceConfiguration, "YospaceSourceConfiguration must not be null");

        uiLoadingState = UI_LOADING_STATE.LOADING;

        if (truexConfiguration != null) {
            this.bitmovinTruexRenderer = new BitmovinTruexRenderer(truexConfiguration, truexRendererListener, context);
        }

        if (session != null) {
            super.unload();
        }
        resetYospaceSession();

        BitLog.INSTANCE.d("Load Yospace Source Configuration");
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

        String originalUrl = hlsSource.getUrl();
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
        stopTruexAdRenderer();
        super.unload();
    }

    public double currentTimeWithAds() {
        return super.getCurrentTime();
    }

    public Ad getActiveAd() {
        return activeAd;
    }

    public AdBreak getActiveAdBreak() {
        return activeAdBreak;
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
        }
        isSessionAdFree = false;
        isLiveAdPaused = false;
        isPlayingEventSent = false;
        activeAd = null;
        activeAdBreak = null;
        adTimeline = null;
        stopTruexAdRenderer();
        timedMetadataEvents.clear();
    }

    private void loadLive() {
        switch (yospaceConfiguration.getLiveInitialisationType()) {
            case PROXY:
                SessionFactory sessionFactory = SessionFactory.createForLiveWithThread(sessionEventListener, properties);
                String url = sessionFactory.getPlayerUrl();
                startPlayback(url);
                break;
            case DIRECT:
                SessionLive.create(sessionEventListener, properties);
                break;
        }
    }

    private void loadVod() {
        SessionNonLinear.create(sessionEventListener, properties);
    }

    private void loadStartOver() {
        SessionNonLinearStartOver.create(sessionEventListener, properties);
    }

    private EventListener<Session> sessionEventListener = new EventListener<Session>() {

        public void handle(Event<Session> event) {

            // Retrieve the initialised session
            session = event.getPayload();
            BitLog.INSTANCE.d("Session State: " + session.getState().toString() + " Result Code: " + session.getResultCode());
            switch (session.getState()) {
                case INITIALISED:
                    session.addAnalyticListener(analyticEventListener);
                    BitLog.INSTANCE.i("Yospace Session Initialized. Session Player Url: " + session.getPlayerUrl());
                    session.setPlayerStateSource(stateSource);
                    session.setPlayerPolicy(yospacePlayerPolicy);
                    if (session instanceof SessionLive) {
                        ((SessionLive) session).setTimedMetadataSource(metadataSource);
                        if (yospaceConfiguration.getLiveInitialisationType() != YospaceLiveInitialisationType.DIRECT) {
                            return;
                        }
                    }
                    startPlayback(session.getPlayerUrl());
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
        if (yospaceSourceConfiguration.getRetryExcludingYospace()) {
            handler.post(new Runnable() {
                public void run() {
                    yospaceEventEmitter.emit(new WarningEvent(yospaceErrorCode, message));
                    if (uiLoadingState != UI_LOADING_STATE.UNLOADING) {
                        load(sourceConfiguration);
                    }
                }
            });
        } else {
            BitLog.INSTANCE.i("Yospace Session failed, shutting down playback");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    yospaceEventEmitter.emit(new ErrorEvent(yospaceErrorCode, message));
                }
            });
        }
    }

    private void startPlayback(String playbackUrl) {
        if (uiLoadingState != UI_LOADING_STATE.UNLOADING) {
            handler.post(new Runnable() {
                public void run() {
                    SourceConfiguration newSourceConfiguration = new SourceConfiguration();
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

    private void stopTruexAdRenderer() {
        if (bitmovinTruexRenderer != null) {
            bitmovinTruexRenderer.stop();
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
            return super.getCurrentTime() - activeAd.getRelativeStart();
        } else if (adTimeline != null) {
            return adTimeline.absoluteToRelative(super.getCurrentTime());
        } else {
            return super.getCurrentTime();
        }
    }

    @Override
    public void seek(double time) {
        if (session != null) {
            if (session.canSeek()) {
                if (adTimeline != null) {
                    long seekTime = session.willSeekTo((long) time);
                    double absoluteSeekTime = adTimeline.relativeToAbsolute(seekTime);
                    BitLog.INSTANCE.d("Seeking to " + absoluteSeekTime);
                    super.seek(absoluteSeekTime);
                } else {
                    BitLog.INSTANCE.d("Seeking to " + time);
                    super.seek(time);
                }
            }
        } else {
            BitLog.INSTANCE.d("Seeking to " + time);
            super.seek(time);
        }
    }

    public void forceSeek(double time) {
        BitLog.INSTANCE.d("Seeking to " + time);
        super.seek(time);
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
            return activeAd != null;
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
            BitLog.INSTANCE.d("Sending Initialising Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.INITIALISING, getYospaceTime(), false));
            if (session instanceof SessionNonLinear) {
                List<com.yospace.android.hls.analytic.advert.AdBreak> adBreaks = ((SessionNonLinear) session).getAdBreaks();
                if (adBreaks != null) {
                    BitLog.INSTANCE.d("Ad Breaks: " + adBreaks.toString());
                    adTimeline = new AdTimeline(adBreaks);
                    BitLog.INSTANCE.d(adTimeline.toString());
                }
            }
        }
    };

    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
        @Override
        public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
            if (sessionStatus != YospaceSesssionStatus.NOT_INITIALIZED) {
                BitLog.INSTANCE.d("Sending Stopped Event: " + getYospaceTime());
                stateSource.notify(new PlayerState(PlaybackState.STOPPED, getYospaceTime(), false));
                resetYospaceSession();
            }
        }
    };

    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            BitLog.INSTANCE.d("Sending Stopped Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.STOPPED, getYospaceTime(), false));
        }
    };

    private OnPausedListener onPausedListener = new OnPausedListener() {
        @Override
        public void onPaused(PausedEvent pausedEvent) {
            isLiveAdPaused = isLive() && isAd();
            BitLog.INSTANCE.d("Sending Paused Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.PAUSED, getYospaceTime(), false));
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
            BitLog.INSTANCE.d("Sending Stall Ended Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.BUFFERING_END, getYospaceTime(), false));
        }
    };

    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            BitLog.INSTANCE.d("Sending Stall Started Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.BUFFERING_START, getYospaceTime(), false));
        }
    };

    private OnTimeChangedListener onTimeChangedListener = new OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimeChangedEvent timeChangedEvent) {
            if (session != null && getAdTimeline() != null) {
                // Notify Yospace of the Time Update
                if (!(session instanceof SessionLive)) {
                    stateSource.notify(new PlayerState(PlaybackState.PLAYHEAD_UPDATE, getYospaceTime(), false));
                }
                TimeChangedEvent event = new TimeChangedEvent(getCurrentTime());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        yospaceEventEmitter.emit(event);
                    }
                });
            } else {
                AdSkippedEvent adSkippedEvent = new AdSkippedEvent(activeAd);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        yospaceEventEmitter.emit(timeChangedEvent);
                        if (isLiveAdPaused) {
                            if (activeAdBreak != null) {
                                if (timeChangedEvent.getTime() > activeAdBreak.getAbsoluteEnd()) {
                                    yospaceEventEmitter.emit(adSkippedEvent);
                                }
                            }
                        }
                        isLiveAdPaused = false;
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

    private OnPlayingListener onPlayingListener = new OnPlayingListener() {
        @Override
        public void onPlaying(PlayingEvent playingEvent) {
            BitLog.INSTANCE.d("Sending Playing Event: " + getYospaceTime());
            stateSource.notify(new PlayerState(PlaybackState.PLAYING, getYospaceTime(), false));
            isPlayingEventSent = true;
        }
    };

    private OnMetadataListener onMetadataListener = new OnMetadataListener() {
        @Override
        public void onMetadata(MetadataEvent metadataEvent) {
            if (yospaceSourceConfiguration.getAssetType() == YospaceAssetType.LINEAR) {
                TimedMetadata timedMetadata = MetadataUtilKt.createTimedMetadata(metadataEvent);
                if (timedMetadata != null) {
                    timedMetadataEvents.add(timedMetadata);
                    // Only send metadata events if play event has been sent
                    if (isPlayingEventSent) {
                        for (TimedMetadata metadata : timedMetadataEvents) {
                            BitLog.INSTANCE.d("Sending Metadata Event: " + metadata.toString());
                            metadataSource.notify(metadata);
                        }
                        timedMetadataEvents.clear();
                    }
                }
            }
        }
    };

    /**
     * TrueX renderer listener
     */
    private BitmovinTruexRendererListener truexRendererListener = new BitmovinTruexRendererListener() {
        @Override
        public void onTruexAdFree() {
            yospaceEventEmitter.emit(new TruexAdFreeEvent());
        }

        @Override
        public void onTruexAdCompleted() {
            play();
        }
    };

    /**
     * Yospace Listeners
     */
    private AnalyticEventListener analyticEventListener = new AnalyticEventListener() {
        @Override
        public void onAdvertBreakStart(com.yospace.android.hls.analytic.advert.AdBreak adBreak) {
            double absoluteTime = currentTimeWithAds();
            double adBreakAbsoluteEnd = absoluteTime + adBreak.getDuration() / 1000.0;
            if (isSessionAdFree) {
                BitLog.INSTANCE.d("Skipping Ad Break due to TrueX ad free experience");
                forceSeek(adBreakAbsoluteEnd);
            } else {
                if (bitmovinTruexRenderer != null) {
                    // Render TrueX ad if found in ad break
                    for (Advert advert : adBreak.getAdverts()) {
                        if (AdvertUtilKt.isTruex(advert)) {
                            bitmovinTruexRenderer.renderAd(advert, adBreak.getStartMillis() == 0);
                            pause();
                            break;
                        }
                    }
                }
                activeAdBreak = new AdBreak(
                        "unknown",
                        absoluteTime,
                        adBreak.getDuration() / 1000.0,
                        absoluteTime,
                        adBreakAbsoluteEnd,
                        0,
                        0,
                        new ArrayList<>()
                );
                double absoluteStartOffset = absoluteTime;
                for (Advert advert : adBreak.getAdverts()) {
                    AdData adData = new AdData(AdvertUtilKt.adMimeType(advert), -1, -1, -1);
                    boolean isTruex = AdvertUtilKt.isTruex(advert);
                    Ad ad = new Ad(
                            advert.getId(),
                            absoluteTime,
                            advert.getDuration() / 1000.0,
                            absoluteStartOffset,
                            absoluteStartOffset + advert.getDuration() / 1000.0,
                            advert.getSequence(),
                            advert.hasLinearInteractiveUnit(),
                            isTruex,
                            !isTruex,
                            AdvertUtilKt.adClickThroughUrl(advert),
                            adData,
                            -1,
                            -1,
                            null
                    );
                    activeAdBreak.getAds().add(ad);
                    absoluteStartOffset += advert.getDuration() / 1000.0;
                }
                AdBreakStartedEvent adBreakStartedEvent = new AdBreakStartedEvent(activeAdBreak);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        yospaceEventEmitter.emit(adBreakStartedEvent);
                    }
                });

            }
        }

        @Override
        public void onAdvertBreakEnd(com.yospace.android.hls.analytic.advert.AdBreak adBreak) {
            AdBreakFinishedEvent adBreakFinishedEvent = new AdBreakFinishedEvent(activeAdBreak);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    yospaceEventEmitter.emit(adBreakFinishedEvent);
                }
            });
            activeAdBreak = null;
        }

        @Override
        public void onAdvertStart(Advert advert) {
            double absoluteTime = currentTimeWithAds();
            double activeAdAbsoluteEnd = absoluteTime + advert.getDuration() / 1000.0;
            if (isSessionAdFree) {
                BitLog.INSTANCE.d("Skipping Ad Break due to TrueX ad free experience");
                forceSeek(activeAdAbsoluteEnd);
            } else {
                String clickThroughUrl = AdvertUtilKt.adClickThroughUrl(advert);
                boolean isTruex = AdvertUtilKt.isTruex(advert);
                AdData adData = new AdData(AdvertUtilKt.adMimeType(advert), 0, 0, 0);
                activeAd = new Ad(
                        advert.getId(),
                        absoluteTime,
                        advert.getDuration() / 1000.0,
                        absoluteTime,
                        activeAdAbsoluteEnd,
                        advert.getSequence(),
                        advert.hasLinearInteractiveUnit(),
                        isTruex,
                        !isTruex,
                        clickThroughUrl,
                        adData,
                        -1,
                        -1,
                        null
                );
                YospaceAdStartedEvent adStartedEvent = new YospaceAdStartedEvent(
                        AdSourceType.UNKNOWN,
                        clickThroughUrl,
                        advert.getSequence(),
                        advert.getDuration() / 1000.0,
                        advert.getStartMillis() / 1000.0,
                        "position",
                        0,
                        isTruex,
                        activeAd
                );
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        yospaceEventEmitter.emit(adStartedEvent);
                    }
                });
            }
        }

        @Override
        public void onAdvertEnd(Advert advert) {
            AdFinishedEvent adFinishedEvent = new AdFinishedEvent(activeAd);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    yospaceEventEmitter.emit(adFinishedEvent);
                }
            });
            activeAd = null;
        }

        @Override
        public void onTimelineUpdateReceived(VmapPayload vmapPayload) {
            BitLog.INSTANCE.d("onTimelineUpdateReceived: ");
        }

        @Override
        public void onTrackingUrlCalled(Advert advert, String s, String s1) {
            BitLog.INSTANCE.d("OnTrackingUrlCalled: " + s);
        }

        @Override
        public void onVastReceived(VastPayload vastPayload) {
            BitLog.INSTANCE.d("OnVastReceived: " + vastPayload.getRaw());
        }
    };
}
