package com.bitmovin.player.integration.yospace

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.advertising.AdItem
import com.bitmovin.player.api.advertising.AdQuartile
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.advertising.vast.AdSystem
import com.bitmovin.player.api.deficiency.SourceErrorCode
import com.bitmovin.player.api.event.EventListener as BitmovinEventListener
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.data.*
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.media.*
import com.bitmovin.player.api.metadata.emsg.EventMessage
import com.bitmovin.player.api.metadata.id3.BinaryFrame
import com.bitmovin.player.api.source.SourceType as MediaSourceType
import com.bitmovin.player.api.drm.WidevineConfig as DRMSystems
import com.bitmovin.player.api.source.*
import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration
import com.yospace.admanagement.*
import com.yospace.admanagement.TimedMetadata
import com.yospace.admanagement.EventListener as YospaceEventListener
import com.yospace.admanagement.Session.SessionProperties
import com.yospace.admanagement.Session.SessionProperties.addDebugFlags
import com.yospace.hls.player.PlaybackState
import com.yospace.hls.player.PlayerState
import com.yospace.util.YoLog
import com.yospace.util.event.EventSourceImpl
import kotlin.math.roundToInt
import kotlin.properties.Delegates

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
    private val playerConfig: PlayerConfig = PlayerConfig(),
    private val yospaceConfig: YospaceConfiguration
) {

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
    private var sourceConfig: SourceConfig? = null
    private var truexRenderer: BitmovinTruexAdRenderer? = null
    val player: Player = Player.create(context, playerConfig)

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

    fun load(sourceConfig: SourceConfig?, yospaceSourceConfig: YospaceSourceConfiguration, truexConfig: TruexConfiguration? = null) {
        BitLog.d("Load YoSpace Source Configuration")

        loadState = LoadState.LOADING
        truexRenderer = truexConfig?.let { BitmovinTruexAdRenderer(context, it).apply { listener = truexAdRendererListener } }
        this.yospaceSourceConfig = yospaceSourceConfig
        this.sourceConfig = sourceConfig

        if (yospaceSession != null) {
            player.unload()
        }

        resetYospaceSession()

        val originalUrl = sourceConfig?.url
        if (originalUrl == null) {
            yospaceEventEmitter.emit(
                CustomSourceEvent.Error(
                    YospaceErrorCode.InvalidYospaceSourceE,
                    "Invalid YoSpace source. You must provide an HLS source"
                )
            )
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
        SessionProperties.setDebugFlags(com.yospace.admanagement.util.YoLog.DEBUG_VALIDATION)

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
                    onSessionInitialized(
                        event.payload,
                        "Yospace analytics session live initialised"
                    )
                }
                startPlayback(MediaSourceType.Hls, originalUrl)
            }
            YospaceLiveInitialisationType.DIRECT -> SessionLive.create(
                originalUrl,
                properties,
                sessionListener
            )
        }

    private fun loadVod(originalUrl: String, properties: SessionProperties) {
        SessionVOD.create(
            originalUrl, properties
        ) { event: Event<Session> ->
            // Callback made by Session once it has initialised a session on the Yospace CSM
            // Retrieve the initialised session
            onSessionInitialized(
                event.payload,
                "Yospace analytics session VOD initialised"
            )
            startPlayback(MediaSourceType.Hls, event.payload.playbackUrl)
        }
    }

    private fun loadStartOver(originalUrl: String, properties: SessionProperties) {
        SessionNLSO.create(
            originalUrl, properties
        ) { event: Event<Session> ->
            // Callback made by Session once it has initialised a session on the Yospace CSM
            // Retrieve the initialised session
            onSessionInitialized(
                event.payload,
                "Yospace analytics session NLSO initialised"
            )
            yospaceSession = event.payload
        }
        startPlayback(MediaSourceType.Hls, originalUrl)
    }

    private fun onSessionInitialized(session: Session, message: String) {
        when (session.sessionResult) {
            Session.SessionResult.INITIALISED -> {
                yospaceSession = session
                session.addAnalyticObserver(analyticEventListener)
                session.setPlaybackPolicyHandler(yospacePlayerPolicy)
                BitLog.i(message)
                return
            }
            else -> {
                BitLog.e("Session Initialization failed with result: %s"
                    .format(session.sessionResult.toString()))
            }
        }
    }

    fun unload() {
        loadState = LoadState.UNLOADING
        truexRenderer?.stop()
        player.unload()
    }

    private fun startPlayback(mediaSourceType: MediaSourceType, playbackUrl: String) {
        if (loadState != LoadState.UNLOADING) {
            handler.post {
                var sourceItem: SourceConfig? = null;
                sourceItem = when (mediaSourceType) {
                    MediaSourceType.Dash -> {
                        SourceConfig(DASHSource(playbackUrl))
                    }
                    MediaSourceType.Hls -> {
                        SourceConfig(HLSSource(playbackUrl))
                    }
                    MediaSourceType.Smooth -> {
                        SourceConfig(SmoothSource(playbackUrl))
                    }
                    else -> {
                        SourceConfig(HLSSource(playbackUrl))
                    }
                }
                val drmConfiguration =
                    sourceConfig?.getDrmConfig(DRMSystems.UUID)
                sourceConfig?.thumbnailTrack?.let {
                    sourceItem.thumbnailTrack = it
                }
                drmConfiguration?.let { sourceItem.addDrmConfig(it) }
                player.load(sourceItem)
            }
        }
    }

    fun pause() {
        if (yospaceSession?.canPause() == true || yospaceSession == null) {
            player.pause()
        }
    }

    fun getDuration(): Double = player.duration - (adTimeline?.totalAdBreakDurations()
        ?: 0.0)

    fun getCurrentTime(): Double = when {
        player.isAd -> {
            // Return ad time
            player.currentTime - (activeAd?.absoluteStart ?: 0.0)
        }
        player.isLive -> {
            // Return absolute time for LIVE
            player.currentTime
        }
        else -> {
            // Return relative time for VOD, or fallback to absolute time
            adTimeline?.absoluteToRelative(player.currentTime) ?: player.currentTime
        }
    }

    private fun yospaceTime(): Int {
        val time = (currentTimeWithAds() * 1000).roundToInt()
        return if (time < 0) 0 else time
    }

    fun currentTimeWithAds(): Double = player.currentTime

    fun seek(time: Double) {
        adTimeline?.let {
            val seekTime = yospaceSession!!.willSeekTo(time.toLong())
            val absoluteSeekTime = it.relativeToAbsolute(seekTime.toDouble())
            BitLog.d("Seeking to $absoluteSeekTime")
            player.seek(absoluteSeekTime)
            return
        }
        BitLog.d("Seeking to $time")
        player.seek(time)
    }

    fun forceSeek(time: Double) {
        BitLog.d("Seeking to $time")
        player.seek(time)
    }

    fun mute() {
        if (yospaceSession?.canChangeVolume(true) == true || yospaceSession == null) {
            player.mute()
        }
    }

    fun isAd(): Boolean = when {
        yospaceSourceConfig != null -> activeAd != null
        else -> player.isAd
    }

    fun skipAd() {
        if (yospaceSourceConfig == null) {
            player.skipAd()
        }
    }

    fun scheduleAd(adItem: AdItem) = if (yospaceSourceConfig != null) {
        yospaceEventEmitter.emit(
            CustomSourceEvent.Warning(
                YospaceWarningCode.UnsupportedAPI,
                "scheduleAd API is not available when playing back a YoSpace asset"
            )
        )
    } else {
        player.scheduleAd(adItem)
    }

    fun setAdViewGroup(adViewGroup: ViewGroup?) = if (yospaceSourceConfig != null) {
        yospaceEventEmitter.emit(
            CustomSourceEvent.Warning(
                YospaceWarningCode.UnsupportedAPI,
                "setAdViewGroup API is not available when playing back a YoSpace asset"
            )
        )
    } else {
        player.setAdViewGroup(adViewGroup)
    }

    ///////////////////////////////////////////////////////////////
    // Player Event Listeners
    ///////////////////////////////////////////////////////////////

    private fun addEventListeners() {
        yospaceMetadataSource.addListener(
            com.yospace.util.event.EventListener<TimedMetadata> {
                BitLog.d("Sending Timed Metadata: $yospaceTime")
                yospaceSession?.onTimedMetadata(it.payload)
            }
        )

        player.on<PlayerEvent.Paused> {
            BitLog.d("Sending PAUSED event: $yospaceTime")
            isLiveAdPaused = player.isLive && player.isAd
            yospaceStateSource.notify(PlayerState(PlaybackState.PAUSED, yospaceTime, false))
        }

        player.on<PlayerEvent.Playing> {
            BitLog.d("Sending PLAYING event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.PLAYING, yospaceTime, false))

            isPlayingEventSent = true
        }

        player.on<PlayerEvent.PlaybackFinished> {
            BitLog.d("Sending STOPPED event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.STOPPED, yospaceTime, false))
        }

        player.on<SourceEvent.Loaded> {
            BitLog.d("Sending INITIALISING event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.INITIALISING, yospaceTime, false))
            (yospaceSession as? SessionVOD)?.let {
                val adBreaks = it.adBreaks.toAdBreaks()
                adTimeline = AdTimeline(adBreaks)
                BitLog.d("Ad breaks: ${it.adBreaks}")
                BitLog.d(adTimeline.toString())
            }
        }

        player.on<SourceEvent.Unloaded> {
            if (yospaceSessionStatus !== SessionStatus.NOT_INITIALIZED) {
                BitLog.d("Sending STOPPED event: $yospaceTime")
                yospaceStateSource.notify(PlayerState(PlaybackState.STOPPED, yospaceTime, false))
                resetYospaceSession()
            }
        }

        player.on<PlayerEvent.StallEnded> {
            BitLog.d("Sending BUFFERING_END event: $yospaceTime")
            yospaceStateSource.notify(PlayerState(PlaybackState.BUFFERING_END, yospaceTime, false))
        }

        player.on<PlayerEvent.StallStarted> {
            BitLog.d("Sending BUFFERING_START event: $yospaceTime")
            yospaceStateSource.notify(
                PlayerState(
                    PlaybackState.BUFFERING_START,
                    yospaceTime,
                    false
                )
            )
        }

        player.on<PlayerEvent.Metadata> { metadataEvent ->
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
        }

        player.on<PlayerEvent.TimeChanged> {
            val timeChangedEvent = PlayerEvent.TimeChanged(getCurrentTime())
            val playbackEventHandler = yospaceSession as PlaybackEventHandler
            playbackEventHandler.onPlayheadUpdate((getCurrentTime() * 1000).toLong());
            if (yospaceSession as? SessionLive != null) {
                // Live session
                val adSkippedEvent = PlayerEvent.AdSkipped(activeAd)
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
                yospaceStateSource.notify(
                    PlayerState(
                        PlaybackState.PLAYHEAD_UPDATE,
                        yospaceTime,
                        false
                    )
                )
                handler.post { yospaceEventEmitter.emit(timeChangedEvent) }
            }
        }

        player.on<PlayerEvent.FullscreenEnter> {
            yospaceSession?.onViewSizeChange(PlaybackEventHandler.ViewSize.MAXIMISED)
        }

        player.on<PlayerEvent.FullscreenExit> {
            yospaceSession?.onViewSizeChange(PlaybackEventHandler.ViewSize.MINIMISED)
        }

        player.on<PlayerEvent.Ready> {
            yospaceSessionStatus = SessionStatus.INITIALIZED
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
            player.play()
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
            player.play()
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
                    startPlayback(MediaSourceType.Hls, it.playbackUrl)
                }
            }
            Session.SessionResult.FAILED -> handleYospaceSessionFailure(
                SESSION_NO_ANALYTICS,
                "Source URL does not refer to a YoSpace stream"
            )
            Session.SessionResult.NOT_INITIALISED -> handleYospaceSessionFailure(
                SESSION_NOT_INITIALISED,
                "Failed to initialise YoSpace stream."
            )
            else -> {
                BitLog.e("Yospace Session Initialization failed with result: %s"
                    .format(yospaceSession?.sessionResult.toString()))
            }
        }
    }

    private fun handleYospaceSessionFailure(errorCode: Int, message: String) =
        if (yospaceSourceConfig?.retryExcludingYospace == true) {
            handler.post {
                yospaceEventEmitter.emit(
                    CustomSourceEvent.Warning(
                        YospaceWarningCode.fromValue(errorCode)!!,
                        "scheduleAd API is not available when playing back a YoSpace asset"
                    )
                )

                if (loadState != LoadState.UNLOADING) {
                    sourceConfig?.let { player.load(it) }
                }
            }
        } else {
            BitLog.d("YoSpace session failed, shutting down playback...")
            handler.post { yospaceEventEmitter.emit(SourceEvent.Error(SourceErrorCode.fromValue(errorCode)!!, message)) }
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

            if (player.isLive) {
                adBreakAbsoluteStart = absoluteTime
                adBreakRelativeStart = absoluteTime
            } else /* VOD */ {
                adBreakAbsoluteStart = adBreak?.start?.div(1000.0) ?: absoluteTime
                adBreakRelativeStart = adTimeline?.absoluteToRelative(adBreakAbsoluteStart)
                    ?: adBreakAbsoluteStart
            }

            activeAdBreak = adBreak?.toAdBreak(adBreakAbsoluteStart, adBreakRelativeStart)

            // Notify listeners of ABS event
            val adBreakStartedEvent = PlayerEvent.AdBreakStarted(activeAdBreak)
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
                    pause()

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

                    if (player.isLive) {
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
                        clientType = AdSourceType.Unknown,
                        clickThroughUrl = advert.linearCreative?.clickThroughUrl.orEmpty(),
                        indexInQueue = advert.sequence ?: 0,
                        duration = advert.duration.div(1000.0) ?: 0.0,
                        timeOffset = advert.start.div(1000.0) ?: 0.0,
                        position = "position",
                        skipOffset = 0.0,
                        ad = activeAd,
                        companionAds = companionAds
                    )
                )
            }
        }

        override fun onAdvertEnd() {
            BitLog.d("YoSpace onAdvertEnd")

            val adFinishedEvent = PlayerEvent.AdFinished(activeAd)
            handler.post { yospaceEventEmitter.emit(adFinishedEvent) }

            activeAd = null
        }

        override fun onAdvertBreakEnd() {
            BitLog.d("YoSpace onAdvertBreakEnd")

            val adBreakFinishedEvent = PlayerEvent.AdBreakFinished(activeAdBreak)
            handler.post { yospaceEventEmitter.emit(adBreakFinishedEvent) }
            activeAdBreak = null
        }

        override fun onTrackingEvent(type: String) {
            BitLog.d("YoSpace onTrackingUrlCalled: $type")

            when (type) {
                "firstQuartile" -> {
                    handler.post {
                        yospaceEventEmitter.emit(PlayerEvent.AdQuartile(AdQuartile.FirstQuartile))
                    }
                }
                "midpoint" -> {
                    handler.post {
                        yospaceEventEmitter.emit(PlayerEvent.AdQuartile(AdQuartile.MidPoint))
                    }
                }
                "thirdQuartile" -> {
                    handler.post {
                        yospaceEventEmitter.emit(PlayerEvent.AdQuartile(AdQuartile.ThirdQuartile))
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
        this.identifier?:"",
        absoluteStart,
        relativeStart,
        duration / 1000.0,
        absoluteStart + duration / 1000.0,
        position = position.lowercase()
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

    private fun PlayerEvent.Metadata.toTimedMetadata() = when {
        type === "EMSG" -> convertEmsgToId3()
        type === "ID3" -> processId3()
        else -> null
    }

    private fun PlayerEvent.Metadata.processId3(): TimedMetadata? {
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

    private fun PlayerEvent.Metadata.convertEmsgToId3(): TimedMetadata? {
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
        ymid != null && yseq != null && ytyp != null && ydur != null -> TimedMetadata.createFromMetadata(ymid, yseq, ytyp, ydur,
            yospaceTime.toLong()
        )
        else -> null
    }
}
