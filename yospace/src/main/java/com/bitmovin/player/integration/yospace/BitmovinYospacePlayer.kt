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
import com.bitmovin.player.api.advertising.AdvertisingApi
import com.bitmovin.player.api.advertising.vast.AdSystem
import com.bitmovin.player.api.analytics.AnalyticsApi
import com.bitmovin.player.api.analytics.AnalyticsApi.Companion.analytics
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.Event as BitmovinEvent
import com.bitmovin.player.api.event.EventListener as BitmovinEventListener
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.metadata.emsg.EventMessage
import com.bitmovin.player.api.metadata.id3.BinaryFrame
import com.bitmovin.player.api.source.SourceType as MediaSourceType
import com.bitmovin.player.api.source.*
import com.bitmovin.player.integration.yospace.advertising.Ad
import com.bitmovin.player.integration.yospace.advertising.AdBreak
import com.bitmovin.player.integration.yospace.advertising.AdBreakPosition
import com.bitmovin.player.integration.yospace.advertising.AdTimeline
import com.bitmovin.player.integration.yospace.advertising.BitmovinTruexAdRenderer
import com.bitmovin.player.integration.yospace.advertising.BitmovinTruexAdRendererListener
import com.bitmovin.player.integration.yospace.advertising.CompanionAd
import com.bitmovin.player.integration.yospace.advertising.CompanionAdResource
import com.bitmovin.player.integration.yospace.advertising.CompanionAdType
import com.bitmovin.player.integration.yospace.config.TruexConfig
import com.bitmovin.player.integration.yospace.config.YospaceAssetType
import com.bitmovin.player.integration.yospace.config.YospaceConfig
import com.bitmovin.player.integration.yospace.config.YospaceDebugMode
import com.bitmovin.player.integration.yospace.config.YospaceLiveInitializationType
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfig
import com.bitmovin.player.integration.yospace.deficiency.YospaceErrorCode
import com.bitmovin.player.integration.yospace.deficiency.YospaceWarningCode
import com.bitmovin.player.integration.yospace.policy.BitmovinYospacePlayerPolicy
import com.bitmovin.player.integration.yospace.policy.DefaultBitmovinYospacePlayerPolicy
import com.bitmovin.player.integration.yospace.policy.YospacePlayerPolicy
import com.bitmovin.player.integration.yospace.events.AdClickThroughReporter
import com.bitmovin.player.integration.yospace.events.PlayerEventDispatcher
import com.bitmovin.player.integration.yospace.events.YospaceAdStartedSnapshot
import com.bitmovin.player.integration.yospace.events.YospaceEventEmitter
import com.bitmovin.player.integration.yospace.events.YospacePlayerEvent
import com.bitmovin.player.integration.yospace.events.YospacePlayerEventDispatcher
import com.bitmovin.player.integration.yospace.events.YospacePlayerEventListener
import com.yospace.admanagement.*
import com.yospace.admanagement.TimedMetadata
import com.yospace.admanagement.EventListener as YospaceEventListener
import com.yospace.admanagement.PlaybackEventHandler.PlayerEvent as YoPlayerEvent
import com.yospace.admanagement.Session.SessionProperties
import com.yospace.admanagement.util.YoLog
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.properties.Delegates
import kotlin.reflect.KClass

// Yospace Error/Warning Codes
private const val INVALID_YOSPACE_SOURCE = 6001
private const val SESSION_NO_ANALYTICS = 6002
private const val SESSION_NOT_INITIALISED = 6003
private const val UNSUPPORTED_API = 6004

// State enums
private enum class LoadState { LOADING, UNLOADING, UNKNOWN }
private enum class SessionStatus { NOT_INITIALIZED, INITIALIZED }

private fun MediaSourceType.isSupportedYospaceSource() = this == MediaSourceType.Hls || this == MediaSourceType.Dash

open class BitmovinYospacePlayer(
    private val context: Context,
    private val playerConfig: PlayerConfig = PlayerConfig(),
    private val player: Player,
    private val yospaceConfig: YospaceConfig,
    analyticsConfig: AnalyticsPlayerConfig? = null
) : Player by player {

    /**
     * Creates a [BitmovinYospacePlayer] with an internally created [Player].
     *
     * Pass [analyticsConfig] to configure Bitmovin Analytics, e.g.
     * `AnalyticsPlayerConfig.Enabled(AnalyticsConfig(licenseKey = "..."))`, or
     * [AnalyticsPlayerConfig.Disabled] to turn analytics off. When omitted, the analytics license
     * is resolved from the player license.
     */
    @JvmOverloads
    constructor(
        context: Context,
        playerConfig: PlayerConfig = PlayerConfig(),
        yospaceConfig: YospaceConfig,
        analyticsConfig: AnalyticsPlayerConfig? = null
    ) : this(
        context,
        playerConfig,
        Player(context, playerConfig, analyticsConfig ?: AnalyticsPlayerConfig.Enabled()),
        yospaceConfig,
        // Already applied to the Player created above.
        analyticsConfig = null
    )

    /**
     * The [AnalyticsApi] of the underlying player, or `null` if analytics is disabled.
     *
     * Exposed explicitly because `Player.analytics` is an extension property and is therefore not
     * forwarded by interface delegation.
     */
    val analytics: AnalyticsApi? get() = player.analytics

    private var yospaceSession: Session? = null
    private val yospaceMetadataSource = EventSource<TimedMetadata>()
    private val yospacePlayerPolicy: YospacePlayerPolicy = YospacePlayerPolicy(DefaultBitmovinYospacePlayerPolicy(this))
    private val yospaceEventEmitter = YospaceEventEmitter()
    private var yospaceSessionProperties: Session.SessionProperties? = null
    private var yospaceSourceConfig: YospaceSourceConfig? = null
    private var yospaceSessionStatus = SessionStatus.NOT_INITIALIZED
    private val yospaceTime: Int get() = yospaceTime()
    private var isLiveAdPaused = false
    private val handler = Handler(Looper.getMainLooper())
    private var loadState: LoadState = LoadState.UNKNOWN
    private val timedMetadataEvents: MutableList<TimedMetadata> = mutableListOf()
    private var isPlayingEventSent = false
    private var sourceConfig: SourceConfig? = null
    private var truexRenderer: BitmovinTruexAdRenderer? = null
    private val playerEventDispatcher = PlayerEventDispatcher(player, ::getCurrentTimeMinusAd)
    private val yospacePlayerEventDispatcher = YospacePlayerEventDispatcher(yospaceEventEmitter)
    private val adClickThroughReporter = AdClickThroughReporter(yospaceEventEmitter)

    var adTimeline: AdTimeline? = null
        private set
    var activeAd: Ad? = null
        private set
    var activeAdBreak: AdBreak? = null
        private set

    var playerPolicy: BitmovinYospacePlayerPolicy? by Delegates.observable<BitmovinYospacePlayerPolicy?>(null) { _, _, new ->
        yospacePlayerPolicy.playerPolicy = new
    }

    override fun <E : BitmovinEvent> on(eventClass: KClass<E>, action: (E) -> Unit) =
        playerEventDispatcher.on(eventClass, action)

    override fun <E : BitmovinEvent> next(eventClass: KClass<E>, action: (E) -> Unit) =
        playerEventDispatcher.next(eventClass, action)

    override fun <E : BitmovinEvent> off(eventClass: KClass<E>, action: (E) -> Unit) =
        playerEventDispatcher.off(eventClass, action)

    override fun <E : BitmovinEvent> off(action: (E) -> Unit) =
        playerEventDispatcher.off(action)

    override fun <E : BitmovinEvent> on(eventClass: Class<E>, eventListener: BitmovinEventListener<in E>) =
        playerEventDispatcher.on(eventClass, eventListener)

    override fun <E : BitmovinEvent> next(eventClass: Class<E>, eventListener: BitmovinEventListener<in E>) =
        playerEventDispatcher.next(eventClass, eventListener)

    override fun <E : BitmovinEvent> off(eventClass: Class<E>, eventListener: BitmovinEventListener<in E>) =
        playerEventDispatcher.off(eventClass, eventListener)

    override fun <E : BitmovinEvent> off(eventListener: BitmovinEventListener<in E>) =
        playerEventDispatcher.off(eventListener)

    @PublishedApi
    internal fun <E : YospacePlayerEvent> onYospacePlayerEvent(eventClass: KClass<E>, action: (E) -> Unit) =
        yospacePlayerEventDispatcher.on(eventClass, action)

    @PublishedApi
    internal fun <E : YospacePlayerEvent> nextYospacePlayerEvent(eventClass: KClass<E>, action: (E) -> Unit) =
        yospacePlayerEventDispatcher.next(eventClass, action)

    @PublishedApi
    internal fun <E : YospacePlayerEvent> offYospacePlayerEvent(eventClass: KClass<E>, action: (E) -> Unit) =
        yospacePlayerEventDispatcher.off(eventClass, action)

    fun <E : YospacePlayerEvent> on(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) =
        yospacePlayerEventDispatcher.on(eventClass, listener)

    fun <E : YospacePlayerEvent> next(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) =
        yospacePlayerEventDispatcher.next(eventClass, listener)

    fun <E : YospacePlayerEvent> off(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) =
        yospacePlayerEventDispatcher.off(eventClass, listener)

    init {
        BitLog.isEnabled = yospaceConfig.isDebug
        BitLog.d("Version ${BuildConfig.BUILD_TYPE}")

        if (analyticsConfig != null) {
            // Analytics is baked into the Player at construction, so it cannot be applied to one
            // that was passed in. Posted so listeners attached after construction still receive it.
            BitLog.e("analyticsConfig is ignored when a Player instance is provided")
            handler.post {
                yospaceEventEmitter.emit(
                    YospacePlayerEvent.Warning(
                        YospaceWarningCode.AnalyticsConfigIgnored,
                        "analyticsConfig is ignored when a Player instance is provided. " +
                            "Configure analytics on the Player you pass in instead."
                    )
                )
            }
        }

        onYospaceEvents()
    }

    ///////////////////////////////////////////////////////////////
    // Playback
    ///////////////////////////////////////////////////////////////

    @Suppress("DEPRECATION")
    fun load(sourceConfig: SourceConfig, yospaceSourceConfig: YospaceSourceConfig, truexConfig: TruexConfig? = null) {
        BitLog.d("Load YoSpace Source Configuration")

        loadState = LoadState.LOADING
        truexRenderer = truexConfig?.let { BitmovinTruexAdRenderer(context, it).apply { listener = truexAdRendererListener } }
        this.yospaceSourceConfig = yospaceSourceConfig
        this.sourceConfig = sourceConfig

        if (yospaceSession != null) {
            player.unload()
        }

        resetYospaceSession()

        val originalUrl = sourceConfig.url
        val mediaSourceType = sourceConfig.type
        if (originalUrl.isEmpty() || !mediaSourceType.isSupportedYospaceSource()) {
            yospaceEventEmitter.emit(
                YospacePlayerEvent.Error(
                    YospaceErrorCode.InvalidYospaceSource,
                    "Invalid YoSpace source. You must provide an HLS or DASH source"
                )
            )
            unload()
            return
        }

        val sessionProperties = SessionProperties()
        sessionProperties.requestTimeout = yospaceConfig.requestTimeout
        sessionProperties.userAgent = yospaceConfig.userAgent

        SessionProperties.setDebugFlags(yospaceConfig.yospaceDebugMode.toYospaceDebugFlags())

        yospaceSessionProperties = sessionProperties

        when (yospaceSourceConfig.assetType) {
            YospaceAssetType.LINEAR -> loadLive(originalUrl, mediaSourceType, yospaceSessionProperties!!)
            YospaceAssetType.VOD -> loadVod(originalUrl, mediaSourceType, yospaceSessionProperties!!)
            YospaceAssetType.LINEAR_START_OVER -> loadStartOver(originalUrl, mediaSourceType, yospaceSessionProperties!!)
        }
    }

    private fun loadLive(originalUrl: String, mediaSourceType: MediaSourceType, properties: SessionProperties) =
        when (yospaceConfig.liveInitializationType) {
            YospaceLiveInitializationType.PROXY -> {
                val playbackUrl = SessionFactory.create(
                    originalUrl,
                    Session.SessionMode.LIVE,
                    properties,
                    sessionListener
                )
                startPlayback(mediaSourceType, playbackUrl)
            }
            YospaceLiveInitializationType.DIRECT -> SessionLive.create(
                originalUrl,
                properties,
                sessionListener
            )
        }

    private fun loadVod(originalUrl: String, mediaSourceType: MediaSourceType, properties: SessionProperties) {
        SessionVOD.create(
            originalUrl, properties
        ) { event: Event<Session> ->
            onSessionInitialized(
                event.payload,
                "Yospace analytics session VOD initialized"
            )
            startPlayback(mediaSourceType, event.payload.playbackUrl)
        }
    }

    private fun loadStartOver(originalUrl: String, mediaSourceType: MediaSourceType, properties: SessionProperties) {
        when (yospaceConfig.liveInitializationType) {
            YospaceLiveInitializationType.PROXY -> {
                val playbackUrl = SessionFactory.create(
                    originalUrl,
                    Session.SessionMode.DVRLIVE,
                    properties,
                    sessionListener
                )
                startPlayback(mediaSourceType, playbackUrl)
            }
            YospaceLiveInitializationType.DIRECT -> {
                SessionDVRLive.create(
                    originalUrl,
                    properties,
                    sessionListener
                )
            }
        }
    }

    private fun onSessionInitialized(session: Session, message: String) {
        when (session.sessionState) {
            Session.SessionState.INITIALISED -> {
                yospaceSession = session
                session.addAnalyticObserver(analyticEventListener)
                session.setPlaybackPolicyHandler(yospacePlayerPolicy)
                BitLog.i("Session is initialized and analytic listener is registered %s".format(message))
                return
            }
            else -> {
                BitLog.e("Session Initialization failed with state: %s"
                    .format(session.sessionState.toString()))
            }
        }
    }

    override fun unload() {
        loadState = LoadState.UNLOADING
        adClickThroughReporter.clear()
        truexRenderer?.stop()
        player.unload()
    }

    private fun startPlayback(mediaSourceType: MediaSourceType, playbackUrl: String) {
        if (loadState != LoadState.UNLOADING) {
            handler.post {
                player.load(buildPlaybackSourceConfig(playbackUrl, mediaSourceType))
            }
        }
    }

    /**
     * Yospace returns a proxied playback URL, so playback runs on a new [SourceConfig].
     * Carry over the settings of the source the user passed to [load].
     */
    private fun buildPlaybackSourceConfig(playbackUrl: String, mediaSourceType: MediaSourceType): SourceConfig {
        val original = sourceConfig ?: return SourceConfig(playbackUrl, mediaSourceType)

        return SourceConfig(playbackUrl, mediaSourceType).apply {
            title = original.title
            description = original.description
            posterSource = original.posterSource
            isPosterPersistent = original.isPosterPersistent
            subtitleTracks = original.subtitleTracks
            thumbnailTrack = original.thumbnailTrack
            drmConfig = original.drmConfig
            labelingConfig = original.labelingConfig
            vrConfig = original.vrConfig
            videoCodecPriority = original.videoCodecPriority
            audioCodecPriority = original.audioCodecPriority
            options = original.options
            metadata = original.metadata
            networkConfig = original.networkConfig
            adaptationConfig = original.adaptationConfig
            cmcdConfig = original.cmcdConfig
        }
    }

    override fun pause() {
        if (yospaceSession?.canPause() == true || yospaceSession == null) {
            player.pause()
        }
    }

    private fun calDuration(): Double = player.duration - (adTimeline?.totalAdBreakDurations() ?: 0.0)

    private fun getCurrentTimeMinusAd(): Double = when {
        isYospaceAd() -> {
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

    /**
     * Yospace expects milliseconds from the fixed timeline origin where the first media segment
     * was available to this session. This anchors ad-break matching and analytics for DVR live.
     */
    private fun yospacePlayheadMs(playbackTime: Double = currentTimeWithAds()): Long =
        ((playbackTime + player.playbackTimeOffsetToRelativeTime) * 1000)
            .roundToLong()
            .coerceAtLeast(0)

    override fun seek(time: Double) {
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

    override fun mute() {
        if (yospaceSession?.canChangeVolume(true) == true || yospaceSession == null) {
            player.mute()
        }
    }

    fun isYospaceAd(): Boolean = when {
        yospaceSourceConfig != null -> activeAd != null
        else -> player.isAd
    }

    override val ads: AdvertisingApi = object : AdvertisingApi by player.ads {
        override fun skip() {
            if (yospaceSourceConfig == null) {
                player.ads.skip()
            }
        }

        override fun schedule(adItem: AdItem) = if (yospaceSourceConfig != null) {
            yospaceEventEmitter.emit(
                YospacePlayerEvent.Warning(
                    YospaceWarningCode.UnsupportedAPI,
                    "ads.schedule API is not available when playing back a Yospace asset"
                )
            )
        } else {
            player.ads.schedule(adItem)
        }

        override fun setViewGroup(viewGroup: ViewGroup?) = if (yospaceSourceConfig != null) {
            yospaceEventEmitter.emit(
                YospacePlayerEvent.Warning(
                    YospaceWarningCode.UnsupportedAPI,
                    "ads.setViewGroup API is not available when playing back a Yospace asset"
                )
            )
        } else {
            player.ads.setViewGroup(viewGroup)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun skipAd() = ads.skip()

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun scheduleAd(adItem: AdItem) = ads.schedule(adItem)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun setAdViewGroup(adViewGroup: ViewGroup?) = ads.setViewGroup(adViewGroup)

    ///////////////////////////////////////////////////////////////
    // Player Event Listeners
    ///////////////////////////////////////////////////////////////

    @Suppress("DEPRECATION")
    private fun onYospaceEvents() {
        yospaceMetadataSource.addListener {
            BitLog.d("Sending Timed Metadata: $yospaceTime")
            yospaceSession?.onTimedMetadata(it.payload)
        }

        player.on<PlayerEvent.Ready> {
            BitLog.d("Sending PLAYSTART event: $yospaceTime")
            yospaceSessionStatus = SessionStatus.INITIALIZED

            // Live sessions need the playhead overload to start the analytic poller.
            when (yospaceSourceConfig?.assetType) {
                YospaceAssetType.LINEAR -> (yospaceSession as? SessionLive)?.onPlaybackStart(yospacePlayheadMs())
                YospaceAssetType.LINEAR_START_OVER ->
                    (yospaceSession as? SessionDVRLive)?.onPlaybackStart(yospacePlayheadMs())
                else -> yospaceSession?.onPlaybackStart()
            }
        }

        player.on<PlayerEvent.Paused> {
            BitLog.d("Sending PAUSED event: $yospaceTime")
            isLiveAdPaused = player.isLive && isYospaceAd()

            yospaceSession?.onPlayerEvent(YoPlayerEvent.PAUSE, yospacePlayheadMs())
        }

        player.on<PlayerEvent.Playing> {
            BitLog.d("Sending PLAYING event: $yospaceTime")
            isPlayingEventSent = true
        }

        player.on<PlayerEvent.PlaybackFinished> {
            BitLog.d("Sending STOPPED event: $yospaceTime")
            yospaceSession?.onPlayerEvent(YoPlayerEvent.STOP, yospacePlayheadMs())
        }

        player.on<SourceEvent.Loaded> {
            BitLog.d("Source loaded: $yospaceTime")
            (yospaceSession as? SessionVOD)?.let {
                val adBreaks = it.getAdBreaks(com.yospace.admanagement.AdBreak.BreakType.LINEAR).toAdBreaks()
                adTimeline = AdTimeline(adBreaks)
                BitLog.d("Ad breaks: ${it.getAdBreaks(com.yospace.admanagement.AdBreak.BreakType.LINEAR)}")
                BitLog.d(adTimeline.toString())
            }
        }

        player.on<SourceEvent.Unloaded> {
            if (yospaceSessionStatus !== SessionStatus.NOT_INITIALIZED) {
                BitLog.d("Sending STOPPED event: $yospaceTime")
                resetYospaceSession()
            }
        }

        player.on<PlayerEvent.StallEnded> {
            BitLog.d("Sending CONTINUE event: $yospaceTime")
            yospaceSession?.onPlayerEvent(YoPlayerEvent.CONTINUE, yospacePlayheadMs())
        }

        player.on<PlayerEvent.StallStarted> {
            BitLog.d("Sending STALL event: $yospaceTime")
            yospaceSession?.onPlayerEvent(YoPlayerEvent.STALL, yospacePlayheadMs())
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
            val currentTime = getCurrentTimeMinusAd()

            if (yospaceSessionStatus == SessionStatus.INITIALIZED) {
                yospaceSession?.onPlayheadUpdate(yospacePlayheadMs())
            }

            if (yospaceSession as? SessionLive != null || yospaceSession as? SessionDVRLive != null) {
                // Live session
                val adSkippedEvent = YospacePlayerEvent.AdSkipped(activeAd)
                handler.post {
                    if (isLiveAdPaused) {
                        activeAdBreak?.let {
                            // Send skip event if live window has moved beyond paused ad
                            if (currentTime > it.absoluteEnd) {
                                yospaceEventEmitter.emit(adSkippedEvent)
                            }
                        }
                    }
                    isLiveAdPaused = false
                }
            }
        }

        player.on<PlayerEvent.FullscreenEnter> {
            yospaceSession?.onViewSizeChange(PlaybackEventHandler.ViewSize.EXPANDED)
        }

        player.on<PlayerEvent.FullscreenExit> {
            yospaceSession?.onViewSizeChange(PlaybackEventHandler.ViewSize.COLLAPSED)
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
            yospaceEventEmitter.emit(YospacePlayerEvent.TruexAdFree)
        }
    }

    ///////////////////////////////////////////////////////////////
    // Yospace Session
    ///////////////////////////////////////////////////////////////

    private val sessionListener: YospaceEventListener<Session> = YospaceEventListener { event ->
        yospaceSession = event.payload
        BitLog.d("Session state: ${yospaceSession?.sessionState?.name}, result code: ${yospaceSession?.resultCode}")

        when (yospaceSession?.sessionState) {
            Session.SessionState.INITIALISED -> {
                BitLog.d("YoSpace session Initialized: url=${yospaceSession?.playbackUrl}")

                yospaceSession?.addAnalyticObserver(analyticEventListener)
                yospaceSession?.setPlaybackPolicyHandler(yospacePlayerPolicy)

                if (yospaceConfig.liveInitializationType != YospaceLiveInitializationType.DIRECT) {
                    return@YospaceEventListener
                }

                yospaceSession?.let {
                    sourceConfig?.type?.let { sourceType ->
                        startPlayback(sourceType, it.playbackUrl)
                    }
                }
            }
            Session.SessionState.FAILED, Session.SessionState.NO_ANALYTICS -> handleYospaceSessionFailure(
                SESSION_NO_ANALYTICS,
                "Source URL does not refer to a YoSpace stream"
            )
            else -> handleYospaceSessionFailure(
                SESSION_NOT_INITIALISED,
                "Failed to initialize YoSpace stream."
            )
        }
    }

    private fun handleYospaceSessionFailure(errorCode: Int, message: String) =
        if (yospaceSourceConfig?.retryExcludingYospace == true) {
            handler.post {
                yospaceEventEmitter.emit(
                    YospacePlayerEvent.Warning(
                        errorCode.toYospaceWarningCode(),
                        message
                    )
                )

                if (loadState != LoadState.UNLOADING) {
                    sourceConfig?.let { player.load(it) }
                }
            }
        } else {
            BitLog.d("YoSpace session failed, shutting down playback...")
            handler.post {
                yospaceEventEmitter.emit(
                    YospacePlayerEvent.Error(
                        YospaceErrorCode.fromValue(errorCode) ?: YospaceErrorCode.SessionNotInitialized,
                        message
                    )
                )
            }
        }

    private fun resetYospaceSession() {
        yospaceSessionStatus = SessionStatus.NOT_INITIALIZED
        yospaceSession?.removeAnalyticObserver(analyticEventListener)
        yospaceSession?.shutdown()
        yospaceSession = null
        isLiveAdPaused = false
        isPlayingEventSent = false
        adClickThroughReporter.clear()
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

        override fun onAdvertBreakStart(adBreak: com.yospace.admanagement.AdBreak?, session: Session) {
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
            val adBreakStartedEvent = YospacePlayerEvent.AdBreakStarted(activeAdBreak)
            handler.post { yospaceEventEmitter.emit(adBreakStartedEvent) }
        }

        override fun onAdvertStart(advert: Advert, session: Session) {
            BitLog.d("YoSpace onAdvertStart")

            val interactiveCreative = advert.interactiveCreatives.firstOrNull()

            // Render TrueX ad
            if (interactiveCreative != null) {
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
                ?.firstOrNull { it.id == advert.identifier }

                // Else create ad manually
                ?: run {
                    val absoluteTime = currentTimeWithAds()
                    val adAbsoluteStart: Double
                    val adRelativeStart: Double

                    if (player.isLive) {
                        adAbsoluteStart = absoluteTime
                        adRelativeStart = activeAdBreak?.relativeStart ?: absoluteTime
                    } else /* VOD */ {
                        adAbsoluteStart = advert.start.div(1000.0)
                        adRelativeStart = adTimeline?.absoluteToRelative(adAbsoluteStart)
                            ?: adAbsoluteStart
                    }

                    advert.toAd(adAbsoluteStart, adRelativeStart)
                }

            val companionAds = interactiveCreative?.nonLinearCreatives?.map { creative ->
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

            val adStartedSnapshot = YospaceAdStartedSnapshot(
                ad = activeAd,
                companionAds = companionAds,
                clientType = toAdType(advert.adType ?: "Yospace"),
                clickThroughUrl = advert.linearCreative?.clickThroughUrl.orEmpty(),
                indexInQueue = advert.sequence,
                duration = advert.duration.div(1000.0),
                timeOffset = advert.start.div(1000.0),
                position = activeAdBreak?.position?.value ?: AdBreakPosition.UNKNOWN.value
            )
            adClickThroughReporter.activate(activeAd, advert)

            // Notify listeners of AS event
            handler.post {
                yospaceEventEmitter.emit(adStartedSnapshot.toYospacePlayerEvent())
            }
        }

        override fun onAdvertEnd(session: Session) {
            BitLog.d("YoSpace onAdvertEnd")

            val adFinishedEvent = YospacePlayerEvent.AdFinished(activeAd)
            handler.post { yospaceEventEmitter.emit(adFinishedEvent) }

            adClickThroughReporter.deactivate(activeAd)
            activeAd = null
        }

        override fun onAdvertBreakEnd(session: Session) {
            BitLog.d("YoSpace onAdvertBreakEnd")

            val adBreakFinishedEvent = YospacePlayerEvent.AdBreakFinished(activeAdBreak)
            handler.post { yospaceEventEmitter.emit(adBreakFinishedEvent) }
            activeAdBreak = null
        }

        override fun onTrackingEvent(type: String, session: Session) {
            BitLog.d("YoSpace onTrackingUrlCalled: $type")

            when (type) {
                "firstQuartile" -> {
                    handler.post {
                        yospaceEventEmitter.emit(YospacePlayerEvent.AdQuartile(AdQuartile.FirstQuartile))
                    }
                }
                "midpoint" -> {
                    handler.post {
                        yospaceEventEmitter.emit(YospacePlayerEvent.AdQuartile(AdQuartile.MidPoint))
                    }
                }
                "thirdQuartile" -> {
                    handler.post {
                        yospaceEventEmitter.emit(YospacePlayerEvent.AdQuartile(AdQuartile.ThirdQuartile))
                    }
                }
            }
        }

        override fun onAnalyticUpdate(session: Session) {
            BitLog.d("YoSpace onAnalyticUpdate event")
        }

        override fun onEarlyReturn(adBreak: com.yospace.admanagement.AdBreak, session: Session) {
            BitLog.d("YoSpace onEarlyReturn: ${adBreak.identifier}")
        }

        override fun onSessionError(error: AnalyticEventObserver.SessionError, session: Session) {
            BitLog.e("YoSpace onSessionError: $error")
            handler.post {
                yospaceEventEmitter.emit(
                    YospacePlayerEvent.Warning(
                        YospaceWarningCode.SessionAnalyticsIssue,
                        "YoSpace session error: $error"
                    )
                )
            }
        }

        override fun onTrackingError(error: TrackingErrors.Error, session: Session) {
            BitLog.e("YoSpace onTrackingError: ${error.toJsonString()}")
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // AdBreak Transformation
    ///////////////////////////////////////////////////////////////////////////

    private fun Int.toYospaceWarningCode(): YospaceWarningCode = when (this) {
        SESSION_NO_ANALYTICS -> YospaceWarningCode.NoAnalytics
        else -> YospaceWarningCode.SessionInitializationIssue
    }

    private fun toAdType(type: String): AdSourceType = when {
        type == "ima" -> AdSourceType.Ima
        type == "progressive" -> AdSourceType.Progressive
        else -> AdSourceType.Unknown
    }

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

    private fun YospaceDebugMode.toYospaceDebugFlags() = when (this) {
        YospaceDebugMode.NONE -> 0
        YospaceDebugMode.VALIDATION -> YoLog.DEBUG_VALIDATION
        YospaceDebugMode.ALL -> YoLog.DEBUG_POLLING or
            YoLog.DEBUG_PARSING or
            YoLog.DEBUG_REPORTS or
            YoLog.DEBUG_HTTP_REQUESTS or
            YoLog.DEBUG_VALIDATION
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
