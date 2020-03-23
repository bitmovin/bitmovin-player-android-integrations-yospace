package com.bitmovin.player.integration.yospace

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.data.*
import com.bitmovin.player.api.event.listener.*
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.advertising.AdItem
import com.bitmovin.player.config.drm.DRMSystems
import com.bitmovin.player.config.media.HLSSource
import com.bitmovin.player.config.media.SourceConfiguration
import com.bitmovin.player.config.media.SourceItem
import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration
import com.bitmovin.player.integration.yospace.util.adClickThroughUrl
import com.bitmovin.player.integration.yospace.util.adMimeType
import com.bitmovin.player.integration.yospace.util.createTimedMetadata
import com.bitmovin.player.integration.yospace.util.isTruex
import com.yospace.android.hls.analytic.*
import com.yospace.android.hls.analytic.Session.SessionProperties
import com.yospace.android.hls.analytic.advert.Advert
import com.yospace.android.xml.VastPayload
import com.yospace.android.xml.VmapPayload
import com.yospace.hls.TimedMetadata
import com.yospace.hls.player.PlaybackState
import com.yospace.hls.player.PlayerState
import com.yospace.util.YoLog
import com.yospace.util.event.EventListener as YospaceEventListener
import com.yospace.util.event.EventSourceImpl
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import com.bitmovin.player.api.event.listener.EventListener as BitmovinEventListener

open class BitmovinYospacePlayer(
    private val context: Context,
    playerConfig: PlayerConfiguration?,
    private val yospaceConfig: YospaceConfiguration
) : BitmovinPlayer(context, playerConfig) {

    private var yospaceSession: Session? = null
    private val yospaceStateSource = EventSourceImpl<PlayerState>()
    private val yospaceMetadataSource = EventSourceImpl<TimedMetadata>()
    private val yospacePlayerPolicy: YospacePlayerPolicy = YospacePlayerPolicy(DefaultBitmovinYospacePlayerPolicy(this))
    private val yospaceEventEmitter = YospaceEventEmitter()
    private var yospaceSessionProperties: SessionProperties? = null
    private var yospaceSourceConfig: YospaceSourceConfiguration? = null
    private var yospaceSessionStatus = YospaceSesssionStatus.NOT_INITIALIZED
    private val yospaceTime: Int get() = yospaceTime()
    private var isLiveAdPaused = false
    private val handler = Handler(Looper.getMainLooper())
    private var loadState: LoadState = LoadState.UNKNOWN
    private val timedMetadataEvents: MutableList<TimedMetadata> = mutableListOf()
    private var isPlayingEventSent = false
    private var sourceConfig: SourceConfiguration? = null
    private var bitmovinTruexRenderer: BitmovinTruexRenderer? = null

    var adTimeline: AdTimeline? = null
        private set
    var activeAd: Ad? = null
        private set
    var activeAdBreak: AdBreak? = null
        private set

    var playerPolicy: BitmovinYospacePlayerPolicy? by Delegates.observable<BitmovinYospacePlayerPolicy?>(null) { _, _, new ->
        yospacePlayerPolicy.playerPolicy = new
    }

    init {
        BitLog.isEnabled = yospaceConfig.isDebug
        BitLog.d("Version 1.1.3")
        addEventListeners()
    }

    ///////////////////////////////////////////////////////////////
    // Player event listeners
    ///////////////////////////////////////////////////////////////

    private fun addEventListeners() {
        super.addEventListener(OnPausedListener {
            BitLog.d("Sending Paused Event: $yospaceTime")
            isLiveAdPaused = isLive && isAd
            yospaceStateSource.notify(PlayerState(PlaybackState.PAUSED, yospaceTime, false))
        })

        super.addEventListener(OnPlayingListener {
            BitLog.d("Sending Playing Event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.PLAYING, yospaceTime, false))
            isPlayingEventSent = true
        })

        super.addEventListener(OnPlaybackFinishedListener {
            BitLog.d("Sending Stopped Event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.STOPPED, yospaceTime, false))
        })

        super.addEventListener(OnSourceLoadedListener {
            BitLog.d("Sending Initialising Event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.INITIALISING, yospaceTime, false))
            (yospaceSession as? SessionNonLinear)?.let {
                adTimeline = AdTimeline(it.adBreaks)
                BitLog.d("Ad Breaks: ${it.adBreaks}")
                BitLog.d(adTimeline.toString())
            }
        })

        super.addEventListener(OnSourceUnloadedListener {
            if (yospaceSessionStatus !== YospaceSesssionStatus.NOT_INITIALIZED) {
                BitLog.d("Sending Stopped Event: $yospaceTime")
                yospaceStateSource.notify(PlayerState(PlaybackState.STOPPED, yospaceTime, false))
                resetYospaceSession()
            }
        })

        super.addEventListener(OnStallEndedListener {
            BitLog.d("Sending Stall Ended Event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.BUFFERING_END, yospaceTime, false))
        })

        super.addEventListener(OnStallStartedListener {
            BitLog.d("Sending Stall Started Event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.BUFFERING_START, yospaceTime, false))
        })

        super.addEventListener(OnMetadataListener { metadataEvent ->
            if (yospaceSourceConfig?.assetType == YospaceAssetType.LINEAR) {
                metadataEvent.createTimedMetadata()?.let {
                    timedMetadataEvents.add(it)
                    // Only send metadata events if play event has been sent
                    if (isPlayingEventSent) {
                        for (metadata in timedMetadataEvents) {
                            BitLog.d("Sending Metadata Event: $metadata")
                            yospaceMetadataSource.notify(metadata)
                        }
                        timedMetadataEvents.clear()
                    }
                }
            }
        })

        super.addEventListener(OnTimeChangedListener { timeChangedEvent ->
            if (yospaceSession as? SessionLive != null) {
                // Live session
                val adSkippedEvent = AdSkippedEvent(activeAd)
                handler.post {
                    yospaceEventEmitter.emit(timeChangedEvent)
                    if (isLiveAdPaused) {
                        activeAdBreak?.let {
                            // Send skip event if live window has moved beyond paused ad
                            if (timeChangedEvent.time > it.absoluteEnd) {
                                yospaceEventEmitter.emit(adSkippedEvent)
                            }
                        }
                    }
                    isLiveAdPaused = false
                }
            } else {
                // Non-live session
                yospaceStateSource.notify(PlayerState(PlaybackState.PLAYHEAD_UPDATE, yospaceTime, false))
                val event = TimeChangedEvent(currentTime)
                handler.post { yospaceEventEmitter.emit(event) }
            }
        })

        super.addEventListener(OnFullscreenEnterListener {
            yospaceSession?.onFullScreenModeChange(true)
        })

        super.addEventListener(OnFullscreenExitListener {
            yospaceSession?.onFullScreenModeChange(false)
        })

        super.addEventListener(OnReadyListener {
            yospaceSessionStatus = YospaceSesssionStatus.INITIALIZED
        })
    }

    override fun addEventListener(listener: BitmovinEventListener<*>) {
        yospaceEventEmitter.addEventListener(listener)
        if (listener !is OnTimeChangedListener) {
            super.addEventListener(listener)
        }
    }

    override fun removeEventListener(listener: BitmovinEventListener<*>) {
        yospaceEventEmitter.removeEventListener(listener)
        super.removeEventListener(listener)
    }

    ///////////////////////////////////////////////////////////////
    // Playback
    ///////////////////////////////////////////////////////////////

    fun load(sourceConfig: SourceConfiguration, yospaceSourceConfig: YospaceSourceConfiguration, truexConfig: TruexConfiguration? = null) {
        BitLog.d("Load Yospace Source Configuration")

        loadState = LoadState.LOADING
        bitmovinTruexRenderer = truexConfig?.let { BitmovinTruexRenderer(it, truexRendererListener, context) }
        this.yospaceSourceConfig = yospaceSourceConfig
        this.sourceConfig = sourceConfig

        if (yospaceSession != null) {
            super.unload()
        }

        resetYospaceSession()

        val originalUrl = sourceConfig.firstSourceItem?.hlsSource?.url
        if (originalUrl == null) {
            yospaceEventEmitter.emit(ErrorEvent(
                YospaceErrorCodes.YOSPACE_INVALID_SOURCE,
                "Invalid Yospace source. You must provide an HLS source"
            ))
            unload()
            return
        }

        yospaceSessionProperties = SessionProperties(originalUrl).readTimeout(yospaceConfig.readTimeout)
            .connectTimeout(yospaceConfig.connectTimeout)
            .requestTimeout(yospaceConfig.requestTimeout)
            .apply {
                userAgent(yospaceConfig.userAgent)
                addDebugFlags(YoLog.DEBUG_POLLING or YoLog.DEBUG_ID3TAG or YoLog.DEBUG_PARSING
                    or YoLog.DEBUG_REPORTS or YoLog.DEBUG_HTTP or YoLog.DEBUG_RAW_XML)
            }

        when (yospaceSourceConfig.assetType) {
            YospaceAssetType.LINEAR -> loadLive()
            YospaceAssetType.VOD -> loadVod()
            YospaceAssetType.LINEAR_START_OVER -> loadStartOver()
        }
    }

    private fun loadLive() = when (yospaceConfig.liveInitialisationType) {
        YospaceLiveInitialisationType.PROXY -> {
            val sessionFactory = SessionFactory.createForLiveWithThread(sessionListener, yospaceSessionProperties)
            startPlayback(sessionFactory.playerUrl)
        }
        YospaceLiveInitialisationType.DIRECT -> SessionLive.create(sessionListener, yospaceSessionProperties)
    }

    private fun loadVod() = SessionNonLinear.create(sessionListener, yospaceSessionProperties)

    private fun loadStartOver() = SessionNonLinearStartOver.create(sessionListener, yospaceSessionProperties)

    override fun unload() {
        loadState = LoadState.UNLOADING
        bitmovinTruexRenderer?.stop()
        super.unload()
    }

    private fun startPlayback(playbackUrl: String) {
        if (loadState != LoadState.UNLOADING) {
            handler.post {
                val newSourceConfiguration = SourceConfiguration()
                val sourceItem = SourceItem(HLSSource(playbackUrl))
                val drmConfiguration = sourceConfig?.firstSourceItem?.getDrmConfiguration(DRMSystems.WIDEVINE_UUID)
                drmConfiguration?.let { sourceItem.addDRMConfiguration(it) }
                newSourceConfiguration.addSourceItem(sourceItem)
                load(newSourceConfiguration)
            }
        }
    }

    ///////////////////////////////////////////////////////////////
    // Playback parameters
    ///////////////////////////////////////////////////////////////

    override fun pause() {
        if (yospaceSession?.canPause() ?: yospaceSession == null) {
            super.pause()
        }
    }

    override fun getDuration(): Double = super.getDuration() - (adTimeline?.totalAdBreakDurations()
        ?: 0.0)

    override fun getCurrentTime(): Double = when {
        isAd -> super.getCurrentTime() - (activeAd?.relativeStart ?: 0.0)
        adTimeline != null -> adTimeline!!.absoluteToRelative(super.getCurrentTime())
        else -> super.getCurrentTime()
    }

    private fun yospaceTime(): Int {
        val time = (currentTimeWithAds() * 1000).roundToInt()
        return if (time < 0) 0 else time
    }

    fun currentTimeWithAds(): Double = super.getCurrentTime()

    override fun seek(time: Double) {
        if (yospaceSession?.canSeek() == true) {
            adTimeline?.let {
                val seekTime = yospaceSession!!.willSeekTo(time.toLong())
                val absoluteSeekTime = it.relativeToAbsolute(seekTime.toDouble())
                BitLog.d("Seeking to $absoluteSeekTime")
                super.seek(absoluteSeekTime)
                return
            }
        }
        BitLog.d("Seeking to $time")
        super.seek(time)
    }

    fun forceSeek(time: Double) {
        BitLog.d("Seeking to $time")
        super.seek(time)
    }

    override fun mute() {
        if (yospaceSession?.canMute() ?: yospaceSession == null) {
            super.mute()
        }
    }

    override fun isAd(): Boolean = when {
        yospaceSourceConfig != null -> activeAd != null
        else -> super.isAd()
    }

    override fun skipAd() {
        if (yospaceSourceConfig == null) {
            super.skipAd()
        }
    }

    override fun scheduleAd(adItem: AdItem) = if (yospaceSourceConfig != null) {
        yospaceEventEmitter.emit(WarningEvent(
            YospaceWarningCodes.UNSUPPORTED_API,
            "scheduleAd API is not available when playing back a Yospace asset"
        ))
    } else {
        super.scheduleAd(adItem)
    }

    override fun setAdViewGroup(adViewGroup: ViewGroup) = if (yospaceSourceConfig != null) {
        yospaceEventEmitter.emit(WarningEvent(
            YospaceWarningCodes.UNSUPPORTED_API,
            "setAdViewGroup API is not available when playing back a Yospace asset"
        ))
    } else {
        super.setAdViewGroup(adViewGroup)
    }

    fun clickThroughPressed() = yospaceSession?.onLinearClickThrough()

    ///////////////////////////////////////////////////////////////
    // TrueX
    ///////////////////////////////////////////////////////////////

    private val truexRendererListener: BitmovinTruexRendererListener = object : BitmovinTruexRendererListener {

        override fun onAdFinished(isAdFree: Boolean) {
            yospaceSession?.suppressAnalytics(false)
            if (isAdFree) {
                yospaceEventEmitter.emit(TruexAdFreeEvent())
            } else {
                // Seek to end of filler
                activeAd?.absoluteEnd?.let { forceSeek(it) }
                play()
            }
        }

        override fun onAdError() = play()
    }

    ///////////////////////////////////////////////////////////////
    // Yospace session
    ///////////////////////////////////////////////////////////////

    private val sessionListener: YospaceEventListener<Session> = YospaceEventListener { event ->
        // Retrieve the initialised session
        yospaceSession = event.payload
        BitLog.d("Session state: ${yospaceSession?.state}, result code: ${yospaceSession?.resultCode}")

        when (yospaceSession?.state) {
            Session.State.INITIALISED -> {
                BitLog.d("Yospace Session Initialized: url=${yospaceSession?.playerUrl}")

                yospaceSession?.addAnalyticListener(analyticEventListener)
                yospaceSession?.setPlayerStateSource(yospaceStateSource)
                yospaceSession?.setPlayerPolicy(yospacePlayerPolicy)

                (yospaceSession as? SessionLive)?.let {
                    it.setTimedMetadataSource(yospaceMetadataSource)
                    if (yospaceConfig.liveInitialisationType != YospaceLiveInitialisationType.DIRECT) {
                        return@YospaceEventListener
                    }
                }

                yospaceSession?.let { startPlayback(it.playerUrl) }
            }
            Session.State.NO_ANALYTICS -> handleYospaceSessionFailure(
                YospaceErrorCodes.YOSPACE_NO_ANALYTICS,
                "Source URL does not refer to a Yospace stream"
            )
            Session.State.NOT_INITIALISED -> handleYospaceSessionFailure(
                YospaceErrorCodes.YOSPACE_NOT_INITIALISED,
                "Failed to initialise Yospace stream."
            )
        }
    }

    private fun handleYospaceSessionFailure(errorCode: Int, message: String) =
        if (yospaceSourceConfig?.retryExcludingYospace == true) {
            handler.post {
                yospaceEventEmitter.emit(WarningEvent(errorCode, message))
                if (loadState != LoadState.UNLOADING) {
                    load(sourceConfig)
                }
            }
        } else {
            BitLog.d("Yospace Session failed, shutting down playback...")
            handler.post { yospaceEventEmitter.emit(ErrorEvent(errorCode, message)) }
        }

    private fun resetYospaceSession() {
        yospaceSessionStatus = YospaceSesssionStatus.NOT_INITIALIZED
        yospaceSession?.removeAnalyticListener(analyticEventListener)
        yospaceSession?.shutdown()
        yospaceSession = null
        isLiveAdPaused = false
        isPlayingEventSent = false
        activeAd = null
        activeAdBreak = null
        adTimeline = null
        bitmovinTruexRenderer?.stop()
        timedMetadataEvents.clear()
    }

    ///////////////////////////////////////////////////////////////
    // Yospace analytics
    ///////////////////////////////////////////////////////////////

    private val analyticEventListener: AnalyticEventListener = object : AnalyticEventListener {

        override fun onAdvertBreakStart(adBreak: com.yospace.android.hls.analytic.advert.AdBreak?) {
            BitLog.d("Yospace onAdvertBreakStart: ")

            val absoluteTime = currentTimeWithAds()
            val adBreakAbsoluteEnd = absoluteTime + (adBreak?.duration?.div(1000.0) ?: 0.0)

            // Store active ad break
            activeAdBreak = AdBreak(
                relativeStart = absoluteTime,
                duration = adBreak?.duration?.div(1000.0) ?: 0.0,
                absoluteStart = absoluteTime,
                absoluteEnd = adBreakAbsoluteEnd
            )

            // Append active ads to active ad break
            var absoluteStartOffset = absoluteTime
            val activeAds = adBreak?.adverts?.map {
                val ad = Ad(
                    it.id,
                    absoluteTime,
                    it.duration / 1000.0,
                    absoluteStartOffset,
                    absoluteStartOffset + it.duration / 1000.0,
                    it.sequence,
                    it.hasLinearInteractiveUnit(),
                    it.isTruex(),
                    !it.isTruex(),
                    it.adClickThroughUrl()
                )
                absoluteStartOffset += it.duration / 1000.0
                ad
            }
            activeAdBreak?.ads?.addAll(activeAds ?: emptyList())

            // Notify listeners of ABS event
            val adBreakStartedEvent = AdBreakStartedEvent(activeAdBreak)
            handler.post { yospaceEventEmitter.emit(adBreakStartedEvent) }
        }

        override fun onAdvertStart(advert: Advert?) {
            BitLog.d("Yospace onAdvertStart: ")

            // Render TrueX ad
            if (advert?.isTruex() == true) {
                yospaceSession?.suppressAnalytics(true)
                pause()
                bitmovinTruexRenderer?.renderAd(advert)
            }

            val absoluteTime = currentTimeWithAds()
            val activeAdAbsoluteEnd = absoluteTime + (advert?.duration?.div(1000.0) ?: 0.0)

            // Store active ad
            activeAd = Ad(
                advert?.id,
                absoluteTime,
                advert?.duration?.div(1000.0) ?: 0.0,
                absoluteTime,
                activeAdAbsoluteEnd,
                advert?.sequence ?: 0,
                advert?.hasLinearInteractiveUnit() ?: false,
                advert?.isTruex() ?: false,
                advert?.isTruex()?.not() ?: false,
                advert?.adClickThroughUrl(),
                AdData(advert?.adMimeType().orEmpty())
            )

            // Notify listeners of AS event
            handler.post {
                yospaceEventEmitter.emit(YospaceAdStartedEvent(
                    clickThroughUrl = advert?.adClickThroughUrl().orEmpty(),
                    indexInQueue = advert?.sequence ?: 0,
                    duration = advert?.duration?.div(1000.0) ?: 0.0,
                    timeOffset = advert?.startMillis?.div(1000.0) ?: 0.0,
                    isTruex = advert?.isTruex() ?: false,
                    ad = activeAd
                ))
            }
        }

        override fun onAdvertEnd(advert: Advert?) {
            BitLog.d("Yospace onAdvertEnd: ")

            val adFinishedEvent = AdFinishedEvent(activeAd)
            handler.post { yospaceEventEmitter.emit(adFinishedEvent) }
            activeAd = null
        }

        override fun onAdvertBreakEnd(adBreak: com.yospace.android.hls.analytic.advert.AdBreak?) {
            BitLog.d("Yospace onAdvertBreakEnd: ")

            val adBreakFinishedEvent = AdBreakFinishedEvent(activeAdBreak)
            handler.post { yospaceEventEmitter.emit(adBreakFinishedEvent) }
            activeAdBreak = null
        }

        override fun onTimelineUpdateReceived(vmapPayload: VmapPayload) {
            BitLog.d("Yospace onTimelineUpdateReceived: ")
        }

        override fun onTrackingUrlCalled(advert: Advert, type: String, url: String) {
            BitLog.d("Yospace onTrackingUrlCalled: $type")
        }

        override fun onVastReceived(vastPayload: VastPayload) {
            BitLog.d("Yospace onVastReceived: " + vastPayload.raw)
        }
    }
}
