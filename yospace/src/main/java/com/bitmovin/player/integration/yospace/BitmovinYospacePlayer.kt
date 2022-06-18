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
import com.bitmovin.player.config.media.*
import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration
import com.bitmovin.player.model.advertising.AdQuartile
import com.bitmovin.player.model.advertising.AdSystem
import com.bitmovin.player.model.emsg.EventMessage
import com.bitmovin.player.model.id3.BinaryFrame
import com.yospace.admanagement.*
import com.yospace.admanagement.Session.SessionProperties
import com.yospace.admanagement.Session.SessionProperties.addDebugFlags
import com.yospace.hls.TimedMetadata
import com.yospace.hls.player.PlaybackState
import com.yospace.hls.player.PlayerState
import com.yospace.util.YoLog
import com.yospace.util.event.EventSourceImpl
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import com.bitmovin.player.api.event.listener.EventListener as BitmovinEventListener
import com.yospace.admanagement.EventListener as YospaceEventListener

// Yospace Error/Warning Codes
private const val INVALID_YOSPACE_SOURCE = 6001
private const val SESSION_NO_ANALYTICS = 6002
private const val SESSION_NOT_INITIALISED = 6003
private const val UNSUPPORTED_API = 6004

// State enums
private enum class LoadState { LOADING, UNLOADING, UNKNOWN }
private enum class SessionStatus { NOT_INITIALIZED, INITIALIZED }

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
    private var yospaceSessionProperties: Session.SessionProperties? = null
    private var yospaceSourceConfig: YospaceSourceConfiguration? = null
    private var yospaceSessionStatus = SessionStatus.NOT_INITIALIZED
    private val yospaceTime: Int get() = yospaceTime()
    private var isLiveAdPaused = false
    private val handler = Handler(Looper.getMainLooper())
    private var loadState: LoadState = LoadState.UNKNOWN
    private val timedMetadataEvents: MutableList<TimedMetadata> = mutableListOf()
    private var isPlayingEventSent = false
    private var sourceConfig: SourceConfiguration? = null
    private var truexRenderer: BitmovinTruexAdRenderer? = null

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
        BitLog.d("Version ${BuildConfig.BUILD_TYPE}")
        addEventListeners()
    }

    ///////////////////////////////////////////////////////////////
    // Playback
    ///////////////////////////////////////////////////////////////

    fun load(sourceConfig: SourceConfiguration?, yospaceSourceConfig: YospaceSourceConfiguration, truexConfig: TruexConfiguration? = null) {
        BitLog.d("Load YoSpace Source Configuration")

        loadState = LoadState.LOADING
        truexRenderer = truexConfig?.let { BitmovinTruexAdRenderer(context, it).apply { listener = truexAdRendererListener } }
        this.yospaceSourceConfig = yospaceSourceConfig
        this.sourceConfig = sourceConfig

        if (yospaceSession != null) {
            super.unload()
        }

        resetYospaceSession()

        val originalUrl = sourceConfig?.firstSourceItem?.hlsSource?.url
        if (originalUrl == null) {
            yospaceEventEmitter.emit(ErrorEvent(
                INVALID_YOSPACE_SOURCE,
                "Invalid YoSpace source. You must provide an HLS source"
            ))
            unload()
            return
        }

        val sessionProperties = SessionProperties()
        sessionProperties.connectTimeout = yospaceConfig.connectTimeout
        sessionProperties.requestTimeout = yospaceConfig.requestTimeout
        sessionProperties.userAgent = yospaceConfig.userAgent

        yospaceSessionProperties = sessionProperties
            .apply {
                addDebugFlags(
                    YoLog.DEBUG_POLLING or YoLog.DEBUG_ID3TAG or YoLog.DEBUG_PARSING
                            or YoLog.DEBUG_REPORTS or YoLog.DEBUG_HTTP or YoLog.DEBUG_RAW_XML
                )
            }

        when (yospaceSourceConfig.assetType) {
            YospaceAssetType.LINEAR -> loadLive(originalUrl, yospaceSessionProperties!!)
            YospaceAssetType.VOD -> loadVod(originalUrl, yospaceSessionProperties!!)
            YospaceAssetType.LINEAR_START_OVER -> loadStartOver(originalUrl, yospaceSessionProperties!!)
        }
    }

    private fun loadLive(originalUrl: String, properties: SessionProperties) =
        when (yospaceConfig.liveInitialisationType) {
            YospaceLiveInitialisationType.PROXY -> {
                SessionLive.create(
                    originalUrl, properties
                ) { event: Event<Session> ->
                    // Callback made by SessionLive once it has initialised a session on the Yospace CSM
                    // Retrieve the initialised session
                    whenSessionInitialized(
                        event.payload,
                        "Yospace analytics session live initialised"
                    )
                }
                startPlayback(MediaSourceType.HLS, originalUrl)
            }
            YospaceLiveInitialisationType.DIRECT -> SessionLive.create(
                originalUrl,
                yospaceSessionProperties,
                sessionListener
            )
        }

    private fun loadVod(originalUrl: String, properties: SessionProperties) {
        SessionVOD.create(
            originalUrl, properties
        ) { event: Event<Session> ->
            // Callback made by Session once it has initialised a session on the Yospace CSM
            // Retrieve the initialised session
            whenSessionInitialized(
                event.payload,
                "Yospace analytics session VOD initialised"
            )
            startPlayback(MediaSourceType.HLS, event.payload.playbackUrl)
        }
    }

    private fun loadStartOver(originalUrl: String, properties: SessionProperties) {
        SessionNLSO.create(
            originalUrl, properties
        ) { event: Event<Session> ->
            // Callback made by Session once it has initialised a session on the Yospace CSM
            // Retrieve the initialised session
            whenSessionInitialized(
                event.payload,
                "Yospace analytics session NLSO initialised"
            )
            yospaceSession = event.payload
        }
        startPlayback(MediaSourceType.HLS, originalUrl)
    }

    private fun whenSessionInitialized(mSession: Session,message: String) {
        when (mSession.getSessionResult()) {
            Session.SessionResult.INITIALISED -> {
                yospaceSession = mSession
                mSession.addAnalyticObserver(analyticEventListener)
                mSession.setPlaybackPolicyHandler(yospacePlayerPolicy)
                BitLog.i(message)
                return
            }
        }
    }

    override fun unload() {
        loadState = LoadState.UNLOADING
        truexRenderer?.stop()
        super.unload()
    }

    private fun startPlayback(mediaSourceType: MediaSourceType, playbackUrl: String) {
        if (loadState != LoadState.UNLOADING) {
            handler.post {
                var sourceItem: SourceItem? = null;
                if (mediaSourceType == MediaSourceType.DASH) {
                    sourceItem = SourceItem(DASHSource(playbackUrl))
                } else if (mediaSourceType == MediaSourceType.HLS) {
                    sourceItem = SourceItem(HLSSource(playbackUrl))
                } else if (mediaSourceType == MediaSourceType.SMOOTH) {
                    sourceItem = SourceItem(SmoothSource(playbackUrl))
                } else {
                    sourceItem = SourceItem(HLSSource(playbackUrl))
                }
                val drmConfiguration =
                    sourceConfig?.firstSourceItem?.getDrmConfiguration(DRMSystems.WIDEVINE_UUID)
                sourceConfig?.firstSourceItem?.thumbnailTrack?.let {
                    sourceItem?.setThumbnailTrack(it);
                }
                drmConfiguration?.let { sourceItem?.addDRMConfiguration(it) }
                if (sourceItem != null) {
                    load(sourceItem)
                }
            }
        }
    }

    override fun pause() {
        if (yospaceSession?.canPause() == true || yospaceSession == null) {
            super.pause()
        }
    }

    override fun getDuration(): Double = super.getDuration() - (adTimeline?.totalAdBreakDurations()
        ?: 0.0)

    override fun getCurrentTime(): Double = when {
        isAd -> {
            // Return ad time
            super.getCurrentTime() - (activeAd?.absoluteStart ?: 0.0)
        }
        isLive -> {
            // Return absolute time for LIVE
            super.getCurrentTime()
        }
        else -> {
            // Return relative time for VOD, or fallback to absolute time
            adTimeline?.absoluteToRelative(super.getCurrentTime()) ?: super.getCurrentTime()
        }
    }

    private fun yospaceTime(): Int {
        val time = (currentTimeWithAds() * 1000).roundToInt()
        return if (time < 0) 0 else time
    }

    fun currentTimeWithAds(): Double = super.getCurrentTime()

    override fun seek(time: Double) {
        adTimeline?.let {
            val seekTime = yospaceSession!!.willSeekTo(time.toLong())
            val absoluteSeekTime = it.relativeToAbsolute(seekTime.toDouble())
            BitLog.d("Seeking to $absoluteSeekTime")
            super.seek(absoluteSeekTime)
            return
        }
        BitLog.d("Seeking to $time")
        super.seek(time)
    }

    fun forceSeek(time: Double) {
        BitLog.d("Seeking to $time")
        super.seek(time)
    }

    override fun mute() {
        if (yospaceSession?.canChangeVolume(true) == true || yospaceSession == null) {
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
        yospaceEventEmitter.emit(
            WarningEvent(
                UNSUPPORTED_API,
                "scheduleAd API is not available when playing back a YoSpace asset"
            )
        )
    } else {
        super.scheduleAd(adItem)
    }

    override fun setAdViewGroup(adViewGroup: ViewGroup?) = if (yospaceSourceConfig != null) {
        yospaceEventEmitter.emit(WarningEvent(
            UNSUPPORTED_API,
            "setAdViewGroup API is not available when playing back a YoSpace asset"
        ))
    } else {
        super.setAdViewGroup(adViewGroup)
    }

    ///////////////////////////////////////////////////////////////
    // Player Event Listeners
    ///////////////////////////////////////////////////////////////

    private fun addEventListeners() {
        super.addEventListener(OnPausedListener {
            BitLog.d("Sending PAUSED event: $yospaceTime")
            isLiveAdPaused = isLive && isAd
            yospaceStateSource.notify(PlayerState(PlaybackState.PAUSED, yospaceTime, false))
        })

        super.addEventListener(OnPlayingListener {
            BitLog.d("Sending PLAYING event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.PLAYING, yospaceTime, false))
            isPlayingEventSent = true
        })

        super.addEventListener(OnPlaybackFinishedListener {
            BitLog.d("Sending STOPPED event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.STOPPED, yospaceTime, false))
        })

        super.addEventListener(OnSourceLoadedListener {
            BitLog.d("Sending INITIALISING event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.INITIALISING, yospaceTime, false))
            (yospaceSession as? SessionNLSO)?.let {
                val adBreaks = it.adBreaks.toAdBreaks()
                adTimeline = AdTimeline(adBreaks)
                BitLog.d("Ad breaks: ${it.adBreaks}")
                BitLog.d(adTimeline.toString())
            }
        })

        super.addEventListener(OnSourceUnloadedListener {
            if (yospaceSessionStatus !== SessionStatus.NOT_INITIALIZED) {
                BitLog.d("Sending STOPPED event: $yospaceTime")
                yospaceStateSource.notify(PlayerState(PlaybackState.STOPPED, yospaceTime, false))
                resetYospaceSession()
            }
        })

        super.addEventListener(OnStallEndedListener {
            BitLog.d("Sending BUFFERING_END event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.BUFFERING_END, yospaceTime, false))
        })

        super.addEventListener(OnStallStartedListener {
            BitLog.d("Sending BUFFERING_START event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.BUFFERING_START, yospaceTime, false))
        })

        super.addEventListener(OnMetadataListener { metadataEvent ->
            if (yospaceSourceConfig?.assetType == YospaceAssetType.LINEAR) {
                if (yospaceConfig.filterMetadataType == null || metadataEvent.type == yospaceConfig.filterMetadataType.name) { // Some Yospace Streams will have both emsg v0(emsg) and v1(id3) which can cause duplicate metadata events
                    metadataEvent.toTimedMetadata()?.let {
                        timedMetadataEvents.add(it)
                        // Only send metadata events if play event has been sent
                        if (isPlayingEventSent) {
                            for (metadata in timedMetadataEvents) {
                                BitLog.d("Sending METADATA event: $metadata")
                                yospaceMetadataSource.notify(metadata)
                            }
                            timedMetadataEvents.clear()
                        }
                    }
                }
            }
        })

        super.addEventListener(OnTimeChangedListener {
            val timeChangedEvent = TimeChangedEvent(currentTime)
            val playbackEventHandler = yospaceSession as PlaybackEventHandler
            playbackEventHandler.onPlayheadUpdate((currentTime * 1000).toLong());
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
                handler.post { yospaceEventEmitter.emit(timeChangedEvent) }
            }
        })

        super.addEventListener(OnFullscreenEnterListener {
            yospaceSession?.onViewSizeChange(PlaybackEventHandler.ViewSize.MAXIMISED)
        })

        super.addEventListener(OnFullscreenExitListener {
            yospaceSession?.onViewSizeChange(PlaybackEventHandler.ViewSize.MINIMISED)
        })

        super.addEventListener(OnReadyListener {
            yospaceSessionStatus = SessionStatus.INITIALIZED
        })
    }

    override fun addEventListener(listener: BitmovinEventListener<*>?) {
        listener?.let {
            yospaceEventEmitter.addEventListener(it)
            if (it !is OnTimeChangedListener) {
                super.addEventListener(it)
            }
        }
    }

    override fun removeEventListener(listener: BitmovinEventListener<*>?) {
        listener?.let {
            yospaceEventEmitter.removeEventListener(it)
            super.removeEventListener(it)
        }
    }

    ///////////////////////////////////////////////////////////////
    // TrueX
    ///////////////////////////////////////////////////////////////

    private val truexAdRendererListener = object : BitmovinTruexAdRendererListener {

        override fun onAdCompleted() {
            BitLog.d("YoSpace analytics unsuppressed")
            yospaceSession?.suppressAnalytics(false)

            activeAd?.let {
                // Only seek over filler if there is at least one second remaining
                // This prevents the player from getting stuck indefinitely in filler
                if (it.absoluteEnd - currentTimeWithAds() >= 1) {
                    BitLog.d("Skipping TrueX filler")
                    forceSeek(it.absoluteEnd)
                }
            }

            BitLog.d("Resuming player")
            play()
        }

        override fun onAdFree() {
            BitLog.d("YoSpace analytics unsuppressed")
            yospaceSession?.suppressAnalytics(false)

            // Seek to end of ad break
            activeAdBreak?.let {
                BitLog.d("Skipping ad break")
                forceSeek(it.absoluteEnd + 0.5)
            }

            BitLog.d("Resuming player")
            play()
        }

        override fun onSessionAdFree() {
            BitLog.d("Session ad free")
            yospaceEventEmitter.emit(TruexAdFreeEvent())
        }
    }

    ///////////////////////////////////////////////////////////////
    // Yospace Session
    ///////////////////////////////////////////////////////////////

    private val sessionListener: YospaceEventListener<Session> = YospaceEventListener { event ->
        // Retrieve the initialised session
        yospaceSession = event.payload
        BitLog.d("Session state: ${yospaceSession?.sessionResult?.name}, result code: ${yospaceSession?.resultCode}")

        when (yospaceSession?.sessionResult) {
            Session.SessionResult.INITIALISED -> {
                BitLog.d("YoSpace session Initialized: url=${yospaceSession?.playbackUrl}")

                yospaceSession?.addAnalyticObserver(analyticEventListener)
                yospaceSession?.setPlaybackPolicyHandler(yospacePlayerPolicy)

                (yospaceSession as? SessionLive)?.let {
                    if (yospaceConfig.liveInitialisationType != YospaceLiveInitialisationType.DIRECT) {
                        return@YospaceEventListener
                    }
                }

                yospaceSession?.let {
                    it.addAnalyticObserver(analyticEventListener)
                    startPlayback(MediaSourceType.HLS, it.playbackUrl) }
            }
            Session.SessionResult.FAILED -> handleYospaceSessionFailure(
                SESSION_NO_ANALYTICS,
                "Source URL does not refer to a YoSpace stream"
            )
            Session.SessionResult.NOT_INITIALISED -> handleYospaceSessionFailure(
                SESSION_NOT_INITIALISED,
                "Failed to initialise YoSpace stream."
            )
        }
    }

    private fun handleYospaceSessionFailure(errorCode: Int, message: String) =
        if (yospaceSourceConfig?.retryExcludingYospace == true) {
            handler.post {
                yospaceEventEmitter.emit(WarningEvent(errorCode, message))
                if (loadState != LoadState.UNLOADING) {
                    sourceConfig?.let { load(it) }
                }
            }
        } else {
            BitLog.d("YoSpace session failed, shutting down playback...")
            handler.post { yospaceEventEmitter.emit(ErrorEvent(errorCode, message)) }
        }

    private fun resetYospaceSession() {
        yospaceSessionStatus = SessionStatus.NOT_INITIALIZED
        yospaceSession?.removeAnalyticObserver(analyticEventListener)
        yospaceSession?.shutdown()
        yospaceSession = null
        isLiveAdPaused = false
        isPlayingEventSent = false
        activeAd = null
        activeAdBreak = null
        adTimeline = null
        truexRenderer?.stop()
        timedMetadataEvents.clear()
    }

    ///////////////////////////////////////////////////////////////
    // Yospace Analytics
    ///////////////////////////////////////////////////////////////

    private val analyticEventListener: AnalyticEventObserver = object : AnalyticEventObserver {

        override fun onAdvertBreakStart(adBreak: com.yospace.admanagement.AdBreak?) {
            BitLog.d("YoSpace onAdvertBreakStart")

            val absoluteTime = currentTimeWithAds()
            val adBreakAbsoluteStart: Double
            val adBreakRelativeStart: Double

            if (isLive) {
                adBreakAbsoluteStart = absoluteTime
                adBreakRelativeStart = absoluteTime
            } else /* VOD */ {
                adBreakAbsoluteStart = adBreak?.start?.div(1000.0) ?: absoluteTime
                adBreakRelativeStart = adTimeline?.absoluteToRelative(adBreakAbsoluteStart)
                    ?: adBreakAbsoluteStart
            }

            activeAdBreak = adBreak?.toAdBreak(adBreakAbsoluteStart, adBreakRelativeStart)

            // Notify listeners of ABS event
            val adBreakStartedEvent = AdBreakStartedEvent(activeAdBreak)
            handler.post { yospaceEventEmitter.emit(adBreakStartedEvent) }
        }

        override fun onAdvertStart(advert: Advert) {
            BitLog.d("YoSpace onAdvertStart")

            // Render TrueX ad
            if (advert.interactiveCreative != null) {
                truexRenderer?.let {
                    BitLog.d("TrueX ad found: $advert")

                    // Suppress analytics in order for YoSpace TrueX tracking to work
                    BitLog.d("YoSpace analytics suppressed")
                    yospaceSession?.suppressAnalytics(true)
                    BitLog.d("Pausing player")
                    super@BitmovinYospacePlayer.pause()

                    val adBreakPosition = activeAdBreak?.position ?: AdBreakPosition.PREROLL
                    it.renderAd(advert, adBreakPosition)
                }
            }

            // Use ad from activeAdBreak if matching id is found
            activeAd = activeAdBreak
                ?.ads
                ?.filterIsInstance<Ad>()
                ?.firstOrNull { it.id == advert?.identifier }

                // Else create ad manually
                ?: run {
                    val absoluteTime = currentTimeWithAds()
                    val adAbsoluteStart: Double
                    val adRelativeStart: Double

                    if (isLive) {
                        adAbsoluteStart = absoluteTime
                        adRelativeStart = activeAdBreak?.relativeStart ?: absoluteTime
                    } else /* VOD */ {
                        adAbsoluteStart = advert.start?.div(1000.0) ?: absoluteTime
                        adRelativeStart = adTimeline?.absoluteToRelative(adAbsoluteStart)
                            ?: adAbsoluteStart
                    }

                    advert.toAd(adAbsoluteStart, adRelativeStart)
                }

            val companionAds = advert.interactiveCreative?.nonLinearCreatives?.map { creative ->
                val resource = creative.getResource(Resource.ResourceType.HTML)?.let {
                    CompanionAdResource(it.stringData, CompanionAdType.HTML)
                } ?: creative.getResource(Resource.ResourceType.STATIC)?.let {
                    CompanionAdResource(it.creativeType, CompanionAdType.STATIC)
                }
                val width = creative?.getProperty("width")?.value?.toInt() ?: 0
                val height = creative?.getProperty("height")?.value?.toInt() ?: 0
                CompanionAd(
                    creative.creativeIdentifier,
                    creative.advertIdentifier,
                    width,
                    height,
                    creative.clickThroughUrl,
                    resource
                )
            }.orEmpty()

            // Notify listeners of AS event
            handler.post {
                yospaceEventEmitter.emit(
                    YospaceAdStartedEvent(
                        clickThroughUrl = advert.linearCreative?.clickThroughUrl.orEmpty(),
                        indexInQueue = advert.sequence ?: 0,
                        duration = advert.duration.div(1000.0) ?: 0.0,
                        timeOffset = advert.start.div(1000.0) ?: 0.0,
                        ad = activeAd,
                        companionAds = companionAds
                    )
                )
            }
        }

        override fun onAdvertEnd() {
            BitLog.d("YoSpace onAdvertEnd")

            val adFinishedEvent = AdFinishedEvent(activeAd)
            handler.post { yospaceEventEmitter.emit(adFinishedEvent) }

            activeAd = null
        }

        override fun onAdvertBreakEnd() {
            BitLog.d("YoSpace onAdvertBreakEnd")

            val adBreakFinishedEvent = AdBreakFinishedEvent(activeAdBreak)
            handler.post { yospaceEventEmitter.emit(adBreakFinishedEvent) }
            activeAdBreak = null
        }

        override fun onTrackingEvent(type: String) {
            BitLog.d("YoSpace onTrackingUrlCalled: $type")

            when (type) {
                "firstQuartile" -> {
                    handler.post {
                        yospaceEventEmitter.emit(AdQuartileEvent(AdQuartile.FIRST_QUARTILE))
                    }
                }
                "midpoint" -> {
                    handler.post {
                        yospaceEventEmitter.emit(AdQuartileEvent(AdQuartile.MIDPOINT))
                    }
                }
                "thirdQuartile" -> {
                    handler.post {
                        yospaceEventEmitter.emit(AdQuartileEvent(AdQuartile.THIRD_QUARTILE))
                    }
                }
            }
        }

        override fun onAnalyticUpdate() {
            BitLog.d("YoSpace onAnalyticUpdate event")
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // AdBreak Transformation
    ///////////////////////////////////////////////////////////////////////////

    private fun List<com.yospace.admanagement.AdBreak>.toAdBreaks(): List<AdBreak> {
        var adBreakDurations = 0.0
        return map {
            it.toAdBreak(it.start / 1000.0, (it.start - adBreakDurations) / 1000.0)
                .apply { adBreakDurations += it.duration }
        }
    }

    private fun com.yospace.admanagement.AdBreak.toAdBreak(absoluteStart: Double, relativeStart: Double) = AdBreak(
        this.identifier,
        absoluteStart,
        relativeStart,
        duration / 1000.0,
        absoluteStart + duration / 1000.0,
        position = position.toLowerCase()
            .run { AdBreakPosition.values().find { it.value == this } ?: AdBreakPosition.UNKNOWN },
        ads = adverts.toAds(absoluteStart, relativeStart).toMutableList()
    )

    ///////////////////////////////////////////////////////////////////////////
    // Ad Transformation
    ///////////////////////////////////////////////////////////////////////////

    private fun List<Advert>.toAds(
        adBreakAbsoluteStart: Double,
        adBreakRelativeStart: Double
    ): List<Ad> {
        var absoluteStart = adBreakAbsoluteStart
        return map {
            it.toAd(absoluteStart, adBreakRelativeStart)
                .apply { absoluteStart += it.duration / 1000.0 }
        }
    }

    private fun Advert.toAd(absoluteStart: Double, relativeStart: Double) = Ad(
        identifier,
        linearCreative?.advertIdentifier,
        sequence,
        absoluteStart,
        relativeStart,
        duration / 1000.0,
        absoluteStart + duration / 1000.0,
        AdSystem(activeAd?.title.orEmpty(), activeAd?.id.orEmpty()),
        activeAd?.title,
        activeAd?.advertiser,
        true,
        isFiller,
        activeAd?.lineage,
        activeAd?.extensions.orEmpty(),
        true,
        clickThroughUrl = linearCreative?.clickThroughUrl.orEmpty()
    )

    ///////////////////////////////////////////////////////////////////////////
    // Metadata Transformation
    ///////////////////////////////////////////////////////////////////////////

    private fun MetadataEvent.toTimedMetadata() = when {
        type === "EMSG" -> convertEmsgToId3()
        type === "ID3" -> processId3()
        else -> null
    }

    private fun MetadataEvent.processId3(): TimedMetadata? {
        var ymid: String? = null
        var yseq: String? = null
        var ytyp: String? = null
        var ydur: String? = null
        var yprg: String? = null

        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i)
            if (entry is BinaryFrame) {
                when (entry.id) {
                    "YMID" -> ymid = String(entry.data)
                    "YSEQ" -> yseq = String(entry.data)
                    "YTYP" -> ytyp = String(entry.data)
                    "YDUR" -> ydur = String(entry.data)
                    "YPRG" -> yprg = String(entry.data)
                }
            }
        }

        return generateTimedMetadata(ymid, yseq, ytyp, ydur, yprg)
    }

    private fun MetadataEvent.convertEmsgToId3(): TimedMetadata? {
        var ymid: String? = null
        var yseq: String? = null
        var ytyp: String? = null
        var ydur: String? = null
        var yprg: String? = null

        for (i in 0 until metadata.length()) {
            val message = metadata.get(i) as EventMessage
            val data = String(message.messageData).split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (j in data.indices) {
                val entry = data[j].split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (entry.size > 1) {
                    val key = entry[0]
                    val value = entry[1]
                    BitLog.d("Key: $key, value: $value")
                    when (key) {
                        "YMID" -> ymid = value
                        "YSEQ" -> yseq = value
                        "YTYP" -> ytyp = value
                        "YDUR" -> ydur = value
                        "YPRG" -> yprg = value
                    }
                }
            }
        }

        return generateTimedMetadata(ymid, yseq, ytyp, ydur, yprg)
    }

    private fun generateTimedMetadata(ymid: String?, yseq: String?, ytyp: String?, ydur: String?, yprg: String?) = when {
        ymid != null && yseq != null && ytyp != null && ydur != null -> TimedMetadata.createFromMetadata(ymid, yseq, ytyp, ydur)
        yprg != null -> TimedMetadata.createFromMetadata(yprg, 0.0f)
        else -> null
    }
}
