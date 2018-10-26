package com.bitmovin.player.integrations.bitmovinyospacemodule;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bitmovin.player.BitmovinPlayer;
import com.bitmovin.player.api.event.data.AdBreakFinishedEvent;
import com.bitmovin.player.api.event.data.AdBreakStartedEvent;
import com.bitmovin.player.api.event.data.AdFinishedEvent;
import com.bitmovin.player.api.event.data.AdStartedEvent;
import com.bitmovin.player.api.event.data.FullscreenEnterEvent;
import com.bitmovin.player.api.event.data.FullscreenExitEvent;
import com.bitmovin.player.api.event.data.MetadataEvent;
import com.bitmovin.player.api.event.data.PausedEvent;
import com.bitmovin.player.api.event.data.PlaybackFinishedEvent;
import com.bitmovin.player.api.event.data.PlayingEvent;
import com.bitmovin.player.api.event.data.SourceLoadedEvent;
import com.bitmovin.player.api.event.data.SourceUnloadedEvent;
import com.bitmovin.player.api.event.data.StallEndedEvent;
import com.bitmovin.player.api.event.data.StallStartedEvent;
import com.bitmovin.player.api.event.data.TimeChangedEvent;
import com.bitmovin.player.api.event.listener.OnAdBreakFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdBreakStartedListener;
import com.bitmovin.player.api.event.listener.OnAdFinishedListener;
import com.bitmovin.player.api.event.listener.OnAdStartedListener;
import com.bitmovin.player.api.event.listener.OnFullscreenEnterListener;
import com.bitmovin.player.api.event.listener.OnFullscreenExitListener;
import com.bitmovin.player.api.event.listener.OnMetadataListener;
import com.bitmovin.player.api.event.listener.OnPausedListener;
import com.bitmovin.player.api.event.listener.OnPlaybackFinishedListener;
import com.bitmovin.player.api.event.listener.OnPlayingListener;
import com.bitmovin.player.api.event.listener.OnSourceLoadedListener;
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener;
import com.bitmovin.player.api.event.listener.OnStallEndedListener;
import com.bitmovin.player.api.event.listener.OnStallStartedListener;
import com.bitmovin.player.api.event.listener.OnTimeChangedListener;
import com.bitmovin.player.config.PlayerConfiguration;
import com.bitmovin.player.config.advertising.AdSourceType;
import com.bitmovin.player.config.media.HLSSource;
import com.bitmovin.player.config.media.SourceConfiguration;
import com.bitmovin.player.config.media.SourceItem;
import com.bitmovin.player.model.id3.BinaryFrame;
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

import java.util.List;


public class BitmovinYoSpacePlayer extends BitmovinPlayer {
    private static final String TAG = "BitmovinYoSpaceLive";

    private String userAgent = "BitmovinPlayer";
    private Session session;
    private SessionFactory sessionFactory;

    private final EventSourceImpl<PlayerState> stateSource = new EventSourceImpl<>();
    private final EventSourceImpl<TimedMetadata> metadataSource = new EventSourceImpl<>();
    private final BitmovinPlayerPolicy bitmovinPlayerPolicy = new BitmovinPlayerPolicy();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BitmovinYoSpaceEventEmitter bitmovinYoSpaceEventEmitter = new BitmovinYoSpaceEventEmitter();
    private Session.SessionProperties properties;
    private YoSpaceSourceConfiguration yoSpaceSourceConfiguration;
    private String originalUrl;

    public BitmovinYoSpacePlayer(Context context, PlayerConfiguration playerConfiguration) {
        this(context, playerConfiguration, true);
    }

    public BitmovinYoSpacePlayer(Context context) {
        this(context, new PlayerConfiguration());
    }

    protected BitmovinYoSpacePlayer(Context context, PlayerConfiguration playerConfiguration, boolean useCast) {
        super(context, playerConfiguration, useCast);
        this.addEventListener(onPausedListener);
        this.addEventListener(onPlayingListener);
        this.addEventListener(onPlaybackFinishedListener);
        this.addEventListener(onSourceLoadedListener);
        this.addEventListener(onSourceUnloadedListener);
        this.addEventListener(onStallEndedListener);
        this.addEventListener(onStallStartedListener);
        this.addEventListener(onMetadataListener);
        this.addEventListener(onTimeChangedListener);
        this.addEventListener(onFullscreenEnterListener);
        this.addEventListener(onFullscreenExitListener);
    }

    public void load(YoSpaceSourceConfiguration sourceConfiguration) {
        Log.d(Constants.TAG, "Load YoSpace Source Configuration");
        this.yoSpaceSourceConfiguration = sourceConfiguration;

        originalUrl =  yoSpaceSourceConfiguration.getSourceConfiguration().getFirstSourceItem().getHlsSource().getUrl();

        properties = new Session.SessionProperties(originalUrl).userAgent(userAgent).readTimeout(20000).connectTimeout(20000).requestTimeout(2000);
        properties.addDebugFlags(YoLog.DEBUG_POLLING | YoLog.DEBUG_ID3TAG | YoLog.DEBUG_PARSING | YoLog.DEBUG_REPORTS | YoLog.DEBUG_HTTP | YoLog.DEBUG_RAW_XML);

        switch (yoSpaceSourceConfiguration.getAssetType()) {
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

    public void clickThroughPressed(){
        session.onLinearClickThrough();
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

                    if (session instanceof SessionLive){
                        ((SessionLive) session).setTimedMetadataSource(metadataSource);
                    }else{
                        startPlayback(session.getPlayerUrl());
                    }
                    return;
                case NO_ANALYTICS:
                    Log.i(Constants.TAG,
                            "PlayerNLSO.initYospace - Video URL does not refer to a Yospace stream, no analytics session created");
                    startPlayback(originalUrl);
                    return;
                case NOT_INITIALISED:
                    Log.e(Constants.TAG, "PlayerNLSO.initYospace - Failed to initialise analytics session");
                    startPlayback(originalUrl);
            }
        }
    };

    private void startPlayback(final String playbackUrl) {

        handler.post(new Runnable() {
            public void run() {
                final SourceConfiguration sourceConfiguration = new SourceConfiguration();
                SourceItem sourceItem = new SourceItem(new HLSSource(playbackUrl));
                sourceConfiguration.addSourceItem(sourceItem);
                load(sourceConfiguration);
            }
        });
    }

    /**
     * EventListener
     */
    @Override
    public void addEventListener(com.bitmovin.player.api.event.listener.EventListener listener) {
        bitmovinYoSpaceEventEmitter.addEventListener(listener);
        super.addEventListener(listener);
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

    /**
     * Player Listeners
     */
    private OnSourceLoadedListener onSourceLoadedListener = new OnSourceLoadedListener() {
        @Override
        public void onSourceLoaded(SourceLoadedEvent sourceLoadedEvent) {
            stateSource.notify(new PlayerState(PlaybackState.INITIALISING, (int) Math.round(getCurrentTime()), false));
        }
    };

    private OnSourceUnloadedListener onSourceUnloadedListener = new OnSourceUnloadedListener() {
        @Override
        public void onSourceUnloaded(SourceUnloadedEvent sourceUnloadedEvent) {
            stateSource.notify(new PlayerState(PlaybackState.STOPPED, (int) Math.round(getCurrentTime()), false));
        }
    };

    private OnPlaybackFinishedListener onPlaybackFinishedListener = new OnPlaybackFinishedListener() {
        @Override
        public void onPlaybackFinished(PlaybackFinishedEvent playbackFinishedEvent) {
            stateSource.notify(new PlayerState(PlaybackState.STOPPED, (int) Math.round(getCurrentTime()), false));
        }
    };

    private OnPausedListener onPausedListener = new OnPausedListener() {
        @Override
        public void onPaused(PausedEvent pausedEvent) {
            stateSource.notify(new PlayerState(PlaybackState.PAUSED, (int) Math.round(getCurrentTime()), false));
        }
    };

    private OnPlayingListener onPlayingListener = new OnPlayingListener() {
        @Override
        public void onPlaying(PlayingEvent playingEvent) {
            stateSource.notify(new PlayerState(PlaybackState.PLAYING, (int) Math.round(getCurrentTime()), false));
        }
    };

    private OnStallEndedListener onStallEndedListener = new OnStallEndedListener() {
        @Override
        public void onStallEnded(StallEndedEvent stallEndedEvent) {
            stateSource.notify(new PlayerState(PlaybackState.BUFFERING_END, (int) Math.round(getCurrentTime()), false));
        }
    };

    private OnStallStartedListener onStallStartedListener = new OnStallStartedListener() {
        @Override
        public void onStallStarted(StallStartedEvent stallStartedEvent) {
            stateSource.notify(new PlayerState(PlaybackState.BUFFERING_START, (int) Math.round(getCurrentTime()), false));
        }
    };

    private OnTimeChangedListener onTimeChangedListener = new OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimeChangedEvent timeChangedEvent) {
            if (!(session instanceof SessionLive)) {
                int timeUpdate = (int) Math.round(getCurrentTime() * 1000);
                stateSource.notify(new PlayerState(PlaybackState.PLAYHEAD_UPDATE, timeUpdate, false));
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
            if (metadataEvent.getType() == "ID3") {
                com.bitmovin.player.model.Metadata metadata = metadataEvent.getMetadata();
                String ymid = null;
                String yseq = null;
                String ytyp = null;
                String ydur = null;
                String yprg = null;

                for (int i = 0; i < metadata.length(); i++) {
                    com.bitmovin.player.model.Metadata.Entry entry = metadata.get(i);
                    if (entry instanceof BinaryFrame) {
                        BinaryFrame binFrame = (BinaryFrame) entry;

                        if ("YMID".equals(binFrame.id)) {
                            ymid = new String(binFrame.data);
                        } else if ("YSEQ".equals(binFrame.id)) {
                            yseq = new String(binFrame.data);
                        } else if ("YTYP".equals(binFrame.id)) {
                            ytyp = new String(binFrame.data);
                        } else if ("YDUR".equals(binFrame.id)) {
                            ydur = new String(binFrame.data);
                        } else if ("YPRG".equals(binFrame.id)) {
                            yprg = new String(binFrame.data);
                        }
                    }
                }

                TimedMetadata timedMetadata = null;
                if (ymid != null && yseq != null && ytyp != null && ydur != null) {
                    timedMetadata = TimedMetadata.createFromId3Tags(ymid, yseq, ytyp, ydur);
                } else if (yprg != null) {
                    timedMetadata = TimedMetadata.createFromId3Tags(yprg, 0.0f);
                }

                if (timedMetadata != null) {
                    metadataSource.notify(timedMetadata);
                }
            }
        }
    };
    
    /**
     * YoSpace Listeners
     */
    private AnalyticEventListener analyticEventListener = new AnalyticEventListener() {
        @Override
        public void onAdvertBreakEnd(AdBreak adBreak) {
            Log.d(Constants.TAG, "OnAdvertBreakEnd: " + adBreak.toString());

            List<com.bitmovin.player.api.event.listener.EventListener> eventListeners = bitmovinYoSpaceEventEmitter.getEventListeners().get(OnAdBreakFinishedListener.class);

            for (com.bitmovin.player.api.event.listener.EventListener listener : eventListeners) {
                ((OnAdBreakFinishedListener) listener).onAdBreakFinished(new AdBreakFinishedEvent());
            }
        }

        @Override
        public void onAdvertBreakStart(AdBreak adBreak) {

            Log.d(Constants.TAG, "OnAdvertBreakStart: " + adBreak.toString());
            List<com.bitmovin.player.api.event.listener.EventListener> eventListeners = bitmovinYoSpaceEventEmitter.getEventListeners().get(OnAdBreakStartedListener.class);

            for (com.bitmovin.player.api.event.listener.EventListener listener : eventListeners) {
                ((OnAdBreakStartedListener) listener).onAdBreakStarted(new AdBreakStartedEvent());
            }

        }

        @Override
        public void onAdvertEnd(Advert advert) {
            Log.d(Constants.TAG, "OnAdvertEnd: " + advert.getId() + " duration - " + advert.getDuration());

            List<com.bitmovin.player.api.event.listener.EventListener> eventListeners = bitmovinYoSpaceEventEmitter.getEventListeners().get(OnAdFinishedListener.class);

            for (com.bitmovin.player.api.event.listener.EventListener listener : eventListeners) {
                ((OnAdFinishedListener) listener).onAdFinished(new AdFinishedEvent());
            }

        }

        @Override
        public void onAdvertStart(Advert advert) {

            Log.d(Constants.TAG, "OnAdvertStart: " + advert.getId() + " duration - " + advert.getDuration());

            List<com.bitmovin.player.api.event.listener.EventListener> eventListeners = bitmovinYoSpaceEventEmitter.getEventListeners().get(OnAdStartedListener.class);

            String clickThroughUrl = "";

            if(advert.getLinearCreative() != null && advert.getLinearCreative().getVideoClicks() != null) {
                clickThroughUrl = advert.getLinearCreative().getVideoClicks().getClickThroughUrl();
            }

            AdStartedEvent adStartedEvent = new AdStartedEvent(AdSourceType.IMA, clickThroughUrl, advert.getSequence(), advert.getDuration(), advert.getStartMillis() / 1000, "position", 0);

            for (com.bitmovin.player.api.event.listener.EventListener listener : eventListeners) {
                ((OnAdStartedListener) listener).onAdStarted(adStartedEvent);
            }
        }

        @Override
        public void onTimelineUpdateReceived(VmapPayload vmapPayload) {
            Log.d(Constants.TAG, "OnTimelineUpdateReceived: " + vmapPayload.toString());

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
