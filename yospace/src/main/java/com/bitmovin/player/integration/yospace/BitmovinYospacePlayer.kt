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
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.Event as BitmovinEvent
import com.bitmovin.player.api.event.EventListener as BitmovinEventListener
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.metadata.emsg.EventMessage
import com.bitmovin.player.api.metadata.id3.BinaryFrame
import com.bitmovin.player.api.source.SourceType as MediaSourceType
import com.bitmovin.player.api.source.*
import com.bitmovin.player.integration.yospace.config.TruexConfig
import com.bitmovin.player.integration.yospace.config.YospaceConfig
import com.bitmovin.player.integration.yospace.config.YospaceDebugMode
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfig
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

open class BitmovinYospacePlayer(
    private val context: Context,
    private val playerConfig: PlayerConfig = PlayerConfig(),
    private val player: Player = Player(context, playerConfig),
    private val yospaceConfig: YospaceConfig
) : Player by player {

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
    private val timeChangedActions =
        mutableMapOf<EventActionKey, MutableList<(PlayerEvent.TimeChanged) -> Unit>>()
    private val timeChangedJavaListenerActions =
        mutableMapOf<JavaEventListenerKey, MutableList<(PlayerEvent.TimeChanged) -> Unit>>()
    private val yospaceJavaListenerActions =
        mutableMapOf<YospaceJavaListenerKey, MutableList<(YospacePlayerEvent) -> Unit>>()

    var adTimeline: AdTimeline? = null
        private set
    var activeAd: Ad? = null
        private set
    var activeAdBreak: AdBreak? = null
        private set

    var playerPolicy: BitmovinYospacePlayerPolicy? by Delegates.observable<BitmovinYospacePlayerPolicy?>(null) { _, _, new ->
        yospacePlayerPolicy.playerPolicy = new
    }

    override fun <E : BitmovinEvent> on(eventClass: KClass<E>, action: (E) -> Unit) {
        if (eventClass == PlayerEvent.TimeChanged::class) {
            val wrappedAction = adjustedTimeChangedAction(action)
            addTimeChangedAction(eventClass, action, wrappedAction)
            player.on(PlayerEvent.TimeChanged::class, wrappedAction)
        } else {
            player.on(eventClass, action)
        }
    }

    override fun <E : BitmovinEvent> next(eventClass: KClass<E>, action: (E) -> Unit) {
        if (eventClass == PlayerEvent.TimeChanged::class) {
            lateinit var wrappedAction: (PlayerEvent.TimeChanged) -> Unit
            wrappedAction = {
                removeTimeChangedAction(eventClass, action, wrappedAction)
                player.off(PlayerEvent.TimeChanged::class, wrappedAction)
                emitAdjustedTimeChanged(action)
            }
            addTimeChangedAction(eventClass, action, wrappedAction)
            player.on(PlayerEvent.TimeChanged::class, wrappedAction)
        } else {
            player.next(eventClass, action)
        }
    }

    override fun <E : BitmovinEvent> off(eventClass: KClass<E>, action: (E) -> Unit) {
        if (eventClass == PlayerEvent.TimeChanged::class) {
            removeLastTimeChangedAction(eventClass, action)?.let {
                player.off(PlayerEvent.TimeChanged::class, it)
            }
        } else {
            player.off(eventClass, action)
        }
    }

    override fun <E : BitmovinEvent> off(action: (E) -> Unit) {
        removeAllTimeChangedActions(action).forEach {
            player.off(PlayerEvent.TimeChanged::class, it)
        }
        player.off(action)
    }

    override fun <E : BitmovinEvent> on(eventClass: Class<E>, eventListener: BitmovinEventListener<in E>) {
        if (eventClass == PlayerEvent.TimeChanged::class.java) {
            val wrappedAction = adjustedTimeChangedAction(eventListener)
            addTimeChangedJavaListenerAction(eventClass, eventListener, wrappedAction)
            player.on(PlayerEvent.TimeChanged::class, wrappedAction)
        } else {
            player.on(eventClass, eventListener)
        }
    }

    override fun <E : BitmovinEvent> next(eventClass: Class<E>, eventListener: BitmovinEventListener<in E>) {
        if (eventClass == PlayerEvent.TimeChanged::class.java) {
            lateinit var wrappedAction: (PlayerEvent.TimeChanged) -> Unit
            wrappedAction = {
                removeTimeChangedJavaListenerAction(eventClass, eventListener, wrappedAction)
                player.off(PlayerEvent.TimeChanged::class, wrappedAction)
                emitAdjustedTimeChanged(eventListener)
            }
            addTimeChangedJavaListenerAction(eventClass, eventListener, wrappedAction)
            player.on(PlayerEvent.TimeChanged::class, wrappedAction)
        } else {
            player.next(eventClass, eventListener)
        }
    }

    override fun <E : BitmovinEvent> off(eventClass: Class<E>, eventListener: BitmovinEventListener<in E>) {
        if (eventClass == PlayerEvent.TimeChanged::class.java) {
            removeLastTimeChangedJavaListenerAction(eventClass, eventListener)?.let {
                player.off(PlayerEvent.TimeChanged::class, it)
            }
        } else {
            player.off(eventClass, eventListener)
        }
    }

    override fun <E : BitmovinEvent> off(eventListener: BitmovinEventListener<in E>) {
        removeAllTimeChangedJavaListenerActions(eventListener).forEach {
            player.off(PlayerEvent.TimeChanged::class, it)
        }
        player.off(eventListener)
    }

    @PublishedApi
    internal fun <E : YospacePlayerEvent> onYospacePlayerEvent(eventClass: KClass<E>, action: (E) -> Unit) =
        yospaceEventEmitter.on(eventClass, action)

    @PublishedApi
    internal fun <E : YospacePlayerEvent> nextYospacePlayerEvent(eventClass: KClass<E>, action: (E) -> Unit) =
        yospaceEventEmitter.next(eventClass, action)

    @PublishedApi
    internal fun <E : YospacePlayerEvent> offYospacePlayerEvent(eventClass: KClass<E>, action: (E) -> Unit) =
        yospaceEventEmitter.off(eventClass, action)

    fun <E : YospacePlayerEvent> on(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) {
        val action = yospacePlayerEventAction(eventClass, listener)
        addYospaceJavaListenerAction(eventClass, listener, action)
        addYospaceEmitterAction(eventClass.kotlin, action)
    }

    fun <E : YospacePlayerEvent> next(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) {
        lateinit var action: (YospacePlayerEvent) -> Unit
        action = action@{
            val event = castYospacePlayerEvent(eventClass, it) ?: return@action
            removeYospaceJavaListenerAction(eventClass, listener, action)
            removeYospaceEmitterAction(eventClass.kotlin, action)
            listener.onEvent(event)
        }

        addYospaceJavaListenerAction(eventClass, listener, action)
        addYospaceEmitterAction(eventClass.kotlin, action)
    }

    fun <E : YospacePlayerEvent> off(eventClass: Class<E>, listener: YospacePlayerEventListener<E>) {
        val action = removeLastYospaceJavaListenerAction(eventClass, listener) ?: return
        removeYospaceEmitterAction(eventClass.kotlin, action)
    }

    init {
        BitLog.isEnabled = yospaceConfig.isDebug
        BitLog.d("Version ${BuildConfig.BUILD_TYPE}")
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
        if (originalUrl.isEmpty() || sourceConfig.type != MediaSourceType.Hls) {
            yospaceEventEmitter.emit(
                CustomSourceEvent.Error(
                    YospaceErrorCode.InvalidYospaceSource,
                    "Invalid YoSpace source. You must provide an HLS source"
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
            YospaceAssetType.LINEAR -> loadLive(originalUrl, yospaceSessionProperties!!)
            YospaceAssetType.VOD -> loadVod(originalUrl, yospaceSessionProperties!!)
            YospaceAssetType.LINEAR_START_OVER -> loadStartOver(originalUrl, yospaceSessionProperties!!)
        }
    }

    private fun loadLive(originalUrl: String, properties: SessionProperties) =
        when (yospaceConfig.liveInitializationType) {
            YospaceLiveInitializationType.PROXY -> {
                val playbackUrl = SessionFactory.create(
                    originalUrl,
                    Session.SessionMode.LIVE,
                    properties,
                    sessionListener
                )
                startPlayback(MediaSourceType.Hls, playbackUrl)
            }
            YospaceLiveInitializationType.DIRECT -> SessionLive.create(
                originalUrl,
                properties,
                sessionListener
            )
        }

    private fun loadVod(originalUrl: String, properties: SessionProperties) {
        SessionVOD.create(
            originalUrl, properties
        ) { event: Event<Session> ->
            onSessionInitialized(
                event.payload,
                "Yospace analytics session VOD initialized"
            )
            startPlayback(MediaSourceType.Hls, event.payload.playbackUrl)
        }
    }

    private fun loadStartOver(originalUrl: String, properties: SessionProperties) {
        when (yospaceConfig.liveInitializationType) {
            YospaceLiveInitializationType.PROXY -> {
                val playbackUrl = SessionFactory.create(
                    originalUrl,
                    Session.SessionMode.DVRLIVE,
                    properties,
                    sessionListener
                )
                startPlayback(MediaSourceType.Hls, playbackUrl)
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
        truexRenderer?.stop()
        player.unload()
    }

    private fun startPlayback(mediaSourceType: MediaSourceType, playbackUrl: String) {
        if (loadState != LoadState.UNLOADING) {
            handler.post {
                val sourceItem = SourceConfig(playbackUrl, mediaSourceType)
                sourceConfig?.thumbnailTrack?.let {
                    sourceItem.thumbnailTrack = it
                }
                sourceConfig?.drmConfig?.let {
                    sourceItem.drmConfig = it
                }
                player.load(sourceItem)
            }
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

    private fun <E : BitmovinEvent> adjustedTimeChangedAction(action: (E) -> Unit): (PlayerEvent.TimeChanged) -> Unit =
        { emitAdjustedTimeChanged(action) }

    @Suppress("UNCHECKED_CAST")
    private fun <E : BitmovinEvent> emitAdjustedTimeChanged(action: (E) -> Unit) {
        action(PlayerEvent.TimeChanged(getCurrentTimeMinusAd()) as E)
    }

    private fun <E : BitmovinEvent> adjustedTimeChangedAction(
        eventListener: BitmovinEventListener<in E>
    ): (PlayerEvent.TimeChanged) -> Unit = {
        emitAdjustedTimeChanged(eventListener)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : BitmovinEvent> emitAdjustedTimeChanged(eventListener: BitmovinEventListener<in E>) {
        eventListener.onEvent(PlayerEvent.TimeChanged(getCurrentTimeMinusAd()) as E)
    }

    private fun addTimeChangedAction(
        eventClass: KClass<out BitmovinEvent>,
        action: Any,
        wrappedAction: (PlayerEvent.TimeChanged) -> Unit
    ) {
        synchronized(timeChangedActions) {
            timeChangedActions.getOrPut(EventActionKey(eventClass, action)) { mutableListOf() }.add(wrappedAction)
        }
    }

    private fun removeLastTimeChangedAction(
        eventClass: KClass<out BitmovinEvent>,
        action: Any
    ): ((PlayerEvent.TimeChanged) -> Unit)? =
        synchronized(timeChangedActions) {
            removeLastAction(timeChangedActions, EventActionKey(eventClass, action))
        }

    private fun removeTimeChangedAction(
        eventClass: KClass<out BitmovinEvent>,
        action: Any,
        wrappedAction: (PlayerEvent.TimeChanged) -> Unit
    ) {
        synchronized(timeChangedActions) {
            removeAction(timeChangedActions, EventActionKey(eventClass, action), wrappedAction)
        }
    }

    private fun removeAllTimeChangedActions(action: Any): List<(PlayerEvent.TimeChanged) -> Unit> =
        synchronized(timeChangedActions) {
            removeAllActions(timeChangedActions) { it.matchesAction(action) }
        }

    private fun addTimeChangedJavaListenerAction(
        eventClass: Class<out BitmovinEvent>,
        eventListener: Any,
        wrappedAction: (PlayerEvent.TimeChanged) -> Unit
    ) {
        synchronized(timeChangedJavaListenerActions) {
            timeChangedJavaListenerActions
                .getOrPut(JavaEventListenerKey(eventClass, eventListener)) { mutableListOf() }
                .add(wrappedAction)
        }
    }

    private fun removeLastTimeChangedJavaListenerAction(
        eventClass: Class<out BitmovinEvent>,
        eventListener: Any
    ): ((PlayerEvent.TimeChanged) -> Unit)? =
        synchronized(timeChangedJavaListenerActions) {
            removeLastAction(timeChangedJavaListenerActions, JavaEventListenerKey(eventClass, eventListener))
        }

    private fun removeTimeChangedJavaListenerAction(
        eventClass: Class<out BitmovinEvent>,
        eventListener: Any,
        wrappedAction: (PlayerEvent.TimeChanged) -> Unit
    ) {
        synchronized(timeChangedJavaListenerActions) {
            removeAction(timeChangedJavaListenerActions, JavaEventListenerKey(eventClass, eventListener), wrappedAction)
        }
    }

    private fun removeAllTimeChangedJavaListenerActions(
        eventListener: Any
    ): List<(PlayerEvent.TimeChanged) -> Unit> =
        synchronized(timeChangedJavaListenerActions) {
            removeAllActions(timeChangedJavaListenerActions) { it.matchesListener(eventListener) }
        }

    private fun <E : YospacePlayerEvent> yospacePlayerEventAction(
        eventClass: Class<E>,
        listener: YospacePlayerEventListener<E>
    ): (YospacePlayerEvent) -> Unit = action@{
        val event = castYospacePlayerEvent(eventClass, it) ?: return@action
        listener.onEvent(event)
    }

    private fun addYospaceJavaListenerAction(
        eventClass: Class<out YospacePlayerEvent>,
        listener: YospacePlayerEventListener<out YospacePlayerEvent>,
        action: (YospacePlayerEvent) -> Unit
    ) {
        synchronized(yospaceJavaListenerActions) {
            yospaceJavaListenerActions
                .getOrPut(YospaceJavaListenerKey(eventClass, listener)) { mutableListOf() }
                .add(action)
        }
    }

    private fun removeLastYospaceJavaListenerAction(
        eventClass: Class<out YospacePlayerEvent>,
        listener: YospacePlayerEventListener<out YospacePlayerEvent>
    ): ((YospacePlayerEvent) -> Unit)? =
        synchronized(yospaceJavaListenerActions) {
            removeLastAction(yospaceJavaListenerActions, YospaceJavaListenerKey(eventClass, listener))
        }

    private fun removeYospaceJavaListenerAction(
        eventClass: Class<out YospacePlayerEvent>,
        listener: YospacePlayerEventListener<out YospacePlayerEvent>,
        action: (YospacePlayerEvent) -> Unit
    ) {
        synchronized(yospaceJavaListenerActions) {
            removeAction(yospaceJavaListenerActions, YospaceJavaListenerKey(eventClass, listener), action)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : YospacePlayerEvent> castYospacePlayerEvent(
        eventClass: Class<E>,
        event: YospacePlayerEvent
    ): E? = if (eventClass.isInstance(event)) event as E else null

    @Suppress("UNCHECKED_CAST")
    private fun <E : YospacePlayerEvent> addYospaceEmitterAction(
        eventClass: KClass<E>,
        action: (YospacePlayerEvent) -> Unit
    ) = yospaceEventEmitter.on(eventClass, action as (E) -> Unit)

    @Suppress("UNCHECKED_CAST")
    private fun <E : YospacePlayerEvent> removeYospaceEmitterAction(
        eventClass: KClass<E>,
        action: (YospacePlayerEvent) -> Unit
    ) = yospaceEventEmitter.off(eventClass, action as (E) -> Unit)

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
                CustomSourceEvent.Warning(
                    YospaceWarningCode.UnsupportedAPI,
                    "ads.schedule API is not available when playing back a Yospace asset"
                )
            )
        } else {
            player.ads.schedule(adItem)
        }

        override fun setViewGroup(viewGroup: ViewGroup?) = if (yospaceSourceConfig != null) {
            yospaceEventEmitter.emit(
                CustomSourceEvent.Warning(
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

            yospaceSession?.onPlayheadUpdate(yospacePlayheadMs())

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
            yospaceEventEmitter.emit(TruexAdFreeEvent())
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
                    startPlayback(MediaSourceType.Hls, it.playbackUrl)
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
                    CustomSourceEvent.Warning(
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
                    CustomSourceEvent.Error(
                        YospaceErrorCode.fromValue(errorCode) ?: YospaceErrorCode.SessionNotInitialised,
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

            val clientType = toAdType(advert.adType ?: "Yospace")
            val clickThroughUrl = advert.linearCreative?.clickThroughUrl.orEmpty()
            val duration = advert.duration.div(1000.0)
            val timeOffset = advert.start.div(1000.0)
            val position = activeAdBreak?.position?.value
            val adClickThroughUrl = activeAd?.clickThroughUrl
            activeAd?.onClickThroughUrlOpened = {
                advert.linearCreative?.onClickThrough()
                yospaceEventEmitter.emit(YospacePlayerEvent.AdClicked(adClickThroughUrl))
            }

            // Notify listeners of AS event
            handler.post {
                // Yospace SDK custom-listener event
                yospaceEventEmitter.emit(
                    YospaceAdStartedEvent(
                        clientType = clientType,
                        clickThroughUrl = clickThroughUrl,
                        indexInQueue = advert.sequence,
                        duration = duration,
                        timeOffset = timeOffset,
                        position = position ?: AdBreakPosition.UNKNOWN.value,
                        skipOffset = 0.0,
                        ad = activeAd,
                        companionAds = companionAds
                    )
                )
                // Integration ad event
                yospaceEventEmitter.emit(
                    YospacePlayerEvent.AdStarted(
                        ad = activeAd,
                        companionAds = companionAds,
                        clientType = clientType,
                        clickThroughUrl = clickThroughUrl,
                        indexInQueue = advert.sequence,
                        duration = duration,
                        timeOffset = timeOffset,
                        position = position,
                        skipOffset = 0.0
                    )
                )
            }
        }

        override fun onAdvertEnd(session: Session) {
            BitLog.d("YoSpace onAdvertEnd")

            val adFinishedEvent = YospacePlayerEvent.AdFinished(activeAd)
            handler.post { yospaceEventEmitter.emit(adFinishedEvent) }

            activeAd?.onClickThroughUrlOpened = null
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
                    CustomSourceEvent.Warning(
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

private fun <K, A> removeLastAction(
    actionsByKey: MutableMap<K, MutableList<A>>,
    key: K
): A? {
    val actions = actionsByKey[key] ?: return null
    val action = actions.removeAt(actions.lastIndex)
    if (actions.isEmpty()) {
        actionsByKey.remove(key)
    }
    return action
}

private fun <K, A> removeAction(
    actionsByKey: MutableMap<K, MutableList<A>>,
    key: K,
    action: A
) {
    val actions = actionsByKey[key] ?: return
    actions.remove(action)
    if (actions.isEmpty()) {
        actionsByKey.remove(key)
    }
}

private fun <K, A> removeAllActions(
    actionsByKey: MutableMap<K, MutableList<A>>,
    matchesKey: (K) -> Boolean
): List<A> {
    val keys = actionsByKey.keys.filter(matchesKey)
    return keys.flatMap { key ->
        actionsByKey.remove(key).orEmpty()
    }
}

private class EventActionKey(
    private val eventClass: KClass<out BitmovinEvent>,
    private val action: Any
) {
    fun matchesAction(action: Any) = this.action === action

    override fun equals(other: Any?) =
        other is EventActionKey && eventClass == other.eventClass && action === other.action

    override fun hashCode() = 31 * eventClass.hashCode() + System.identityHashCode(action)
}

private class JavaEventListenerKey(
    private val eventClass: Class<out BitmovinEvent>,
    private val eventListener: Any
) {
    fun matchesListener(eventListener: Any) = this.eventListener === eventListener

    override fun equals(other: Any?) =
        other is JavaEventListenerKey && eventClass == other.eventClass && eventListener === other.eventListener

    override fun hashCode() = 31 * eventClass.hashCode() + System.identityHashCode(eventListener)
}

private class YospaceJavaListenerKey(
    private val eventClass: Class<out YospacePlayerEvent>,
    private val listener: YospacePlayerEventListener<out YospacePlayerEvent>
) {
    override fun equals(other: Any?) =
        other is YospaceJavaListenerKey && eventClass == other.eventClass && listener === other.listener

    override fun hashCode() = 31 * eventClass.hashCode() + System.identityHashCode(listener)
}
