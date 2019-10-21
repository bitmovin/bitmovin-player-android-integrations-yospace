package com.bitmovin.player.integration.yospace

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bitmovin.player.BitmovinPlayer
import com.bitmovin.player.api.event.data.*
import com.bitmovin.player.api.event.listener.*
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.drm.DRMSystems
import com.bitmovin.player.config.media.HLSSource
import com.bitmovin.player.config.media.SourceConfiguration
import com.bitmovin.player.config.media.SourceItem
import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration
import com.bitmovin.player.integration.yospace.util.emptyAdStartedEvent
import com.bitmovin.player.integration.yospace.util.getAdClickThroughUrl
import com.bitmovin.player.integration.yospace.util.toAdStartedEvent
import com.bitmovin.player.integration.yospace.util.toTimedMetadata
import com.truex.adrenderer.IEventHandler
import com.truex.adrenderer.TruexAdRenderer
import com.truex.adrenderer.TruexAdRendererConstants
import com.yospace.android.hls.analytic.*
import com.yospace.android.hls.analytic.advert.Advert
import com.yospace.android.xml.VastPayload
import com.yospace.android.xml.VmapPayload
import com.yospace.hls.TimedMetadata
import com.yospace.hls.player.PlaybackState
import com.yospace.hls.player.PlayerState
import com.yospace.util.YoLog
import com.yospace.util.event.EventListener
import com.yospace.util.event.EventSourceImpl
import org.apache.commons.lang3.Validate
import org.json.JSONException
import org.json.JSONObject

class BitmovinYospacePlayer(private val context: Context, playerConfiguration: PlayerConfiguration, private val yospaceConfiguration: YospaceConfiguration) : BitmovinPlayer(context, playerConfiguration) {

    var adTimeline: AdTimeline? = null

    private var liveAd: Ad? = null
    private var liveAdBreak: AdBreak? = null
    private var isYospaceAd: Boolean = false
    private var adFree: Boolean = false
    private var isTruexAdRendering: Boolean = false
    private var truexAdRenderer: TruexAdRenderer? = null

    private var truexConfiguration: TruexConfiguration? = null
    private lateinit var yospaceSourceConfiguration: YospaceSourceConfiguration
    private lateinit var sourceConfiguration: SourceConfiguration

    private val yospacePlayerPolicy: YospacePlayerPolicy = YospacePlayerPolicy(DefaultBitmovinYospacePlayerPolicy(this))
    private val yospaceEventEmitter: YospaceEventEmitter = YospaceEventEmitter()
    private val metadataSource: EventSourceImpl<TimedMetadata> = EventSourceImpl()
    private val stateSource: EventSourceImpl<PlayerState> = EventSourceImpl()

    private var session: Session? = null
    private var sessionFactory: SessionFactory? = null
    private var sessionProperties: Session.SessionProperties? = null
    private var sessionStatus = YospaceSesssionStatus.NOT_INITIALIZED
    private var sessionPrimaryUrl: String? = null

    private var loadState: LoadState = LoadState.UNKNOWN
    private val handler = Handler(Looper.getMainLooper())

    override fun unload() {
        loadState = LoadState.UNLOADING
        super.unload()
    }

    fun load(sourceConfiguration: SourceConfiguration, yospaceSourceConfiguration: YospaceSourceConfiguration, truexConfiguration: TruexConfiguration? = null) {
        Validate.notNull(sourceConfiguration, "SourceConfiguration must not be null")
        Validate.notNull(yospaceSourceConfiguration, "YospaceSourceConfiguration must not be null")

        loadState = LoadState.LOADING

        this.sourceConfiguration = sourceConfiguration
        this.yospaceSourceConfiguration = yospaceSourceConfiguration
        this.truexConfiguration = truexConfiguration

        if (truexConfiguration == null) {
            truexAdRenderer = null
        }

        val sourceItem = sourceConfiguration.firstSourceItem
        if (sourceItem == null) {
            yospaceEventEmitter.emit(ErrorEvent(YospaceErrorCodes.YOSPACE_INVALID_SOURCE, "Invalid Yospace source. You must provide an HLS source"))
            unload()
            return
        }

        val hlsSource = sourceItem.hlsSource
        if (hlsSource == null || hlsSource.url == null) {
            yospaceEventEmitter.emit(ErrorEvent(YospaceErrorCodes.YOSPACE_INVALID_SOURCE, "Invalid Yospace source. You must provide an HLS source"))
            unload()
            return
        }

        sessionPrimaryUrl = hlsSource.url
        sessionProperties = Session.SessionProperties(sessionPrimaryUrl)
            .readTimeout(yospaceConfiguration.readTimeout)
            .connectTimeout(yospaceConfiguration.connectTimeout)
            .requestTimeout(yospaceConfiguration.requestTimeout)

        yospaceConfiguration.userAgent?.let { userAgent ->
            sessionProperties!!.userAgent(userAgent)
        }

        if (yospaceConfiguration.isDebug) {
            sessionProperties!!.addDebugFlags(YoLog.DEBUG_POLLING or YoLog.DEBUG_ID3TAG or YoLog.DEBUG_PARSING or YoLog.DEBUG_REPORTS or YoLog.DEBUG_HTTP or YoLog.DEBUG_RAW_XML)
        }

        when (yospaceSourceConfiguration.assetType) {
            YospaceAssetType.LINEAR -> loadLive()
            YospaceAssetType.VOD -> loadVod()
            YospaceAssetType.LINEAR_START_OVER -> loadStartOver()
        }
    }

    fun getActiveAd(): Ad? = when {
        isLive -> liveAd
        else -> adTimeline?.currentAd(currentTimeWithAds())
    }

    fun currentTimeWithAds(): Double = currentTime

    fun clickThroughPressed() {
        session?.onLinearClickThrough()
    }

    fun setPlayerPolicy(bitmovinYospacePlayerPolicy: BitmovinYospacePlayerPolicy) {
        yospacePlayerPolicy.playerPolicy = bitmovinYospacePlayerPolicy
    }

    private fun loadLive() {
        sessionFactory = SessionFactory.createForLiveWithThread(sessionEventListener, sessionProperties)
        startPlayback(sessionFactory!!.playerUrl)
    }

    private fun loadVod() {
        SessionNonLinear.create(sessionEventListener, sessionProperties)
    }

    private fun loadStartOver() {
        SessionNonLinearStartOver.create(sessionEventListener, sessionProperties)
    }

    private fun resetYospaceSession() {
        sessionStatus = YospaceSesssionStatus.NOT_INITIALIZED
        session?.removeAnalyticListener(analyticEventListener)
        session?.shutdown()
        session = null
        isYospaceAd = false
        adFree = false
        liveAd = null
        liveAdBreak = null
        adTimeline = null
        isTruexAdRendering = false
        super.unload()
    }

    private fun handleYospaceSessionFailure(yospaceErrorCode: Int, message: String) {
        if (yospaceSourceConfiguration.retryExcludingYospace) {
            handler.post {
                yospaceEventEmitter.emit(WarningEvent(yospaceErrorCode, message))
                if (loadState != LoadState.UNLOADING) {
                    load(sourceConfiguration)
                }
            }
        } else {
            BitLog.i("Yospace Session failed, shutting down playback")
            handler.post { yospaceEventEmitter.emit(ErrorEvent(yospaceErrorCode, message)) }
        }
    }

    private fun startPlayback(playbackUrl: String) {
        if (loadState != LoadState.UNLOADING) {
            handler.post {
                val newSourceConfiguration = SourceConfiguration()
                val sourceItem = SourceItem(HLSSource(playbackUrl))
                val drmConfiguration = sourceConfiguration.firstSourceItem.getDrmConfiguration(DRMSystems.WIDEVINE_UUID)
                drmConfiguration?.let {
                    sourceItem.addDRMConfiguration(drmConfiguration)
                }
                newSourceConfiguration.addSourceItem(sourceItem)
                load(newSourceConfiguration)
            }
        }
    }

    private fun renderTruexAd(creativeURL: String, adParameters: String) {
        try {
            val adParams = JSONObject(adParameters)
            truexConfiguration?.let { config ->
                truexAdRenderer = TruexAdRenderer(context)
                truexAdRenderer!!.addEventListener(TruexAdRendererConstants.AD_STARTED, adStartedListener)
                truexAdRenderer!!.addEventListener(TruexAdRendererConstants.AD_COMPLETED, adCompletedListener)
                truexAdRenderer!!.addEventListener(TruexAdRendererConstants.AD_ERROR, adErrorListener)
                truexAdRenderer!!.addEventListener(TruexAdRendererConstants.NO_ADS_AVAILABLE, noAdsListener)
                truexAdRenderer!!.addEventListener(TruexAdRendererConstants.AD_FREE_POD, adFreeListener)
                truexAdRenderer!!.addEventListener(TruexAdRendererConstants.POPUP_WEBSITE, popupListener)
                truexAdRenderer!!.init(creativeURL, adParams, TruexAdRendererConstants.PREROLL)
                truexAdRenderer!!.start(config.viewGroup)
                isTruexAdRendering = true
                BitLog.d("trueX ad rendered successfully")
            }
        } catch (e: JSONException) {
            BitLog.e("Failed to render trueX ad: $e")
        }
    }

    private fun getYospaceTime(): Int = if (currentTime < 0) {
        0
    } else {
        (currentTime * 1000).toInt()
    }

    /**
     * Player Listeners
     */
    private val onSourceLoadedListener = OnSourceLoadedListener {
        BitLog.d("Sending Initialising Event: " + getYospaceTime())
        stateSource.notify(PlayerState(PlaybackState.INITIALISING, getYospaceTime(), false))
        if (session is SessionNonLinear) {
            val adBreaks = (session as SessionNonLinear).adBreaks
            adBreaks?.let {
                BitLog.d("Ad Breaks: $adBreaks")
                adTimeline = AdTimeline(adBreaks)
                BitLog.d(adTimeline.toString())
            }
        }
    }

    private val onSourceUnloadedListener = OnSourceUnloadedListener {
        if (sessionStatus != YospaceSesssionStatus.NOT_INITIALIZED) {
            BitLog.d("Sending Stopped Event: ${getYospaceTime()}")
            stateSource.notify(PlayerState(PlaybackState.STOPPED, getYospaceTime(), false))
            resetYospaceSession()
        }
    }

    private val onPlaybackFinishedListener = OnPlaybackFinishedListener {
        BitLog.d("Sending Stopped Event: ${getYospaceTime()}")
        stateSource.notify(PlayerState(PlaybackState.STOPPED, getYospaceTime(), false))
    }

    private val onPausedListener = OnPausedListener {
        BitLog.d("Sending Paused Event: ${getYospaceTime()}")
        stateSource.notify(PlayerState(PlaybackState.PAUSED, getYospaceTime(), false))
    }

    private val onPlayingListener = OnPlayingListener {
        BitLog.d("Sending Playing Event: ${getYospaceTime()}")
        stateSource.notify(PlayerState(PlaybackState.PLAYING, getYospaceTime(), false))
    }

    private val onReadyListener = OnReadyListener { sessionStatus = YospaceSesssionStatus.INITIALIZED }

    private val onStallStartedListener = OnStallStartedListener {
        BitLog.d("Sending Stall Started Event: ${getYospaceTime()}")
        stateSource.notify(PlayerState(PlaybackState.BUFFERING_START, getYospaceTime(), false))
    }

    private val onStallEndedListener = OnStallEndedListener {
        BitLog.d("Sending Stall Ended Event: ${getYospaceTime()}")
        stateSource.notify(PlayerState(PlaybackState.BUFFERING_END, getYospaceTime(), false))
    }

    private val onTimeChangedListener = OnTimeChangedListener { timeChangedEvent ->
        if (session != null && adTimeline != null) {
            if (session !is SessionLive) {
                // Notify Yospace of the time Update
                stateSource.notify(PlayerState(PlaybackState.PLAYHEAD_UPDATE, getYospaceTime(), false))
            }
            handler.post {
                adTimeline?.let { timeline ->
                    if (isYospaceAd) {
                        // If we are in a Yospace ad, send ad time
                        val adTime = timeline.adTime(timeChangedEvent.time)
                        yospaceEventEmitter.emit(TimeChangedEvent(adTime))
                    } else {
                        // If we are not in a Yospace ad, send converted relative time
                        val relativeTime = timeline.absoluteToRelative(timeChangedEvent.time)
                        yospaceEventEmitter.emit(TimeChangedEvent(relativeTime))
                    }
                }
            }
        } else {
            handler.post { yospaceEventEmitter.emit(timeChangedEvent) }
        }
    }

    private val onFullscreenEnterListener = OnFullscreenEnterListener { session?.onFullScreenModeChange(true) }

    private val onFullscreenExitListener = OnFullscreenExitListener { session?.onFullScreenModeChange(false) }

    private val onMetadataListener = OnMetadataListener { metadataEvent ->
        if (yospaceSourceConfiguration.assetType == YospaceAssetType.LINEAR) {
            metadataEvent.toTimedMetadata()?.let { timedMetadata ->
                BitLog.d("Sending Metadata Event: $timedMetadata")
                metadataSource.notify(timedMetadata)
            }
        }
    }

    /**
     * trueX Listeners
     */
    private val adStartedListener = IEventHandler {
        BitLog.d("trueX ad started")
        val activeAd = getActiveAd()
        val adStartedEvent = activeAd?.toAdStartedEvent() ?: emptyAdStartedEvent()
        yospaceEventEmitter.emit(AdBreakStartedEvent())
        yospaceEventEmitter.emit(adStartedEvent)
        isYospaceAd = true
        pause()
    }

    private val adCompletedListener = IEventHandler {
        BitLog.d("trueX ad completed")
        isYospaceAd = false
        if (!adFree) {
            BitLog.d("Emitting AdFinishedEvent")
            yospaceEventEmitter.emit(AdFinishedEvent())
            play()
        }
    }

    private val adErrorListener = IEventHandler {
        BitLog.d("trueX ad error")
        isYospaceAd = false
        play()
    }

    private val noAdsListener = IEventHandler {
        BitLog.d("No trueX ads found")
        isYospaceAd = false
        play()
    }

    private val popupListener = IEventHandler { BitLog.d("trueX popup") }

    private val adFreeListener = IEventHandler {
        BitLog.d("trueX ad free")
        adFree = true
        getActiveAd()?.let { ad ->
            BitLog.d("Emitting AdFinishedEvent")
            yospaceEventEmitter.emit(AdFinishedEvent())
            BitLog.d("Emitting AdBreakFinishedEvent")
            yospaceEventEmitter.emit(AdBreakFinishedEvent())
            // Seek to end of underlying non-trueX ad
            seek(ad.absoluteEnd + 1)
            play()
        }
    }

    /**
     * Yospace Listeners
     */
    private val analyticEventListener = object : AnalyticEventListener {

        override fun onAdvertBreakStart(adBreak: com.yospace.android.hls.analytic.advert.AdBreak) {
            if (adFree) {
                BitLog.d("Skipping ad break due to trueX ad free experience")
                seek(currentTime + 1)
            } else {
                truexConfiguration?.let { config ->
                    // Render trueX ad if found in ad break
                    adBreak.adverts.forEach { advert ->
                        if (advert.adSystem.adSystemType == "trueX") {
                            val creativeUrl = "https://qa-get.truex.com/07d5fe7cc7f9b5ab86112433cf0a83b6fb41b092/vast?dimension_2=0&stream_position=midroll&network_user_id=" + config.userId
                            val adParams = "{\"user_id\":\"" + config.userId + "\",\"placement_hash\":\"07d5fe7cc7f9b5ab86112433cf0a83b6fb41b092\",\"vast_config_url\":\"" + config.vastConfigUrl + "\"}"
                            BitLog.d("trueX Ad Found - Source:$creativeUrl")
                            BitLog.d("Rendering trueX Ad: $advert")
                            pause()
                            renderTruexAd(creativeUrl, adParams)
                        }
                    }
                }
                if (!isTruexAdRendering) {
                    // No trueX ad present in ad break, so handle ad events here
                    if (isLive) {
                        val adId = adBreak.toString() + System.currentTimeMillis()
                        val absoluteTime = this@BitmovinYospacePlayer.currentTime
                        liveAdBreak = AdBreak(adId, absoluteTime, adBreak.duration / 1000.0, absoluteTime, (adBreak.startMillis + adBreak.duration) / 1000.0)
                    }
                    handler.post {
                        BitLog.d("Sending AdBreakStartedEvent")
                        yospaceEventEmitter.emit(AdBreakStartedEvent())
                    }
                }
            }
        }

        override fun onAdvertBreakEnd(adBreak: com.yospace.android.hls.analytic.advert.AdBreak) {
            if (!isTruexAdRendering && !adFree) {
                handler.post {
                    BitLog.d("Sending AdBreakFinishedEvent")
                    yospaceEventEmitter.emit(AdBreakFinishedEvent())
                }
            }
            liveAdBreak = null
            isTruexAdRendering = false
        }

        override fun onAdvertStart(advert: Advert) {
            if (adFree) {
                BitLog.d("Skipping Ad Break due to trueX ad free experience")
                seek(currentTime + 1)
            } else {
                if (!isTruexAdRendering) {
                    isYospaceAd = true
                    val clickThroughUrl = advert.getAdClickThroughUrl()
                    val absoluteTime = this@BitmovinYospacePlayer.currentTime
                    val isTruex = advert.adSystem.adSystemType == "trueX"
                    liveAd = Ad(advert.identifier, absoluteTime, advert.duration / 1000.0, absoluteTime, (advert.startMillis + advert.duration) / 1000.0, advert.sequence, clickThroughUrl, advert.hasLinearInteractiveUnit(), isTruex)
                    handler.post {
                        BitLog.d("Emitting AdStartedEvent")
                        val adStartedEvent = advert.toAdStartedEvent()
                        yospaceEventEmitter.emit(adStartedEvent)
                    }
                }
            }
        }

        override fun onAdvertEnd(advert: Advert) {
            isYospaceAd = false
            if (!isTruexAdRendering && !adFree) {
                handler.post {
                    BitLog.d("Emitting AdFinishedEvent")
                    yospaceEventEmitter.emit(AdFinishedEvent())
                }
            }
            liveAd = null
            isTruexAdRendering = false
        }

        override fun onTimelineUpdateReceived(vmapPayload: VmapPayload) {
            BitLog.d("Timeline Update Received: ")
        }

        override fun onTrackingUrlCalled(advert: Advert, s: String, s1: String) {
            BitLog.d("Tracking Url Called: $s")
        }

        override fun onVastReceived(vastPayload: VastPayload) {
            BitLog.d("Vast Received: ${vastPayload.raw}")
        }
    }

    private val sessionEventListener = EventListener<Session> { event ->
        session = event.payload
        session?.let { session ->
            BitLog.d("state=${session.state}, resultCode=${session.resultCode}")
            when (session.state) {
                Session.State.INITIALISED -> {
                    BitLog.i("Yospace Session Initialized - playerUrl=${session.playerUrl}")
                    session.addAnalyticListener(analyticEventListener)
                    session.setPlayerStateSource(stateSource)
                    session.setPlayerPolicy(yospacePlayerPolicy)
                    if (session is SessionLive) {
                        session.setTimedMetadataSource(metadataSource)
                    } else {
                        startPlayback(session.playerUrl)
                    }
                }
                Session.State.NO_ANALYTICS -> {
                    handleYospaceSessionFailure(YospaceErrorCodes.YOSPACE_NO_ANALYTICS, "Source URL does not refer to a Yospace stream")
                }
                Session.State.NOT_INITIALISED -> {
                    handleYospaceSessionFailure(YospaceErrorCodes.YOSPACE_NOT_INITIALISED, "Failed to initialise Yospace stream.")
                }
            }
        }
    }

    init {
        BitLog.isEnabled = yospaceConfiguration.isDebug
        super.addEventListener(onPausedListener)
        super.addEventListener(onPlayingListener)
        super.addEventListener(onPlaybackFinishedListener)
        super.addEventListener(onSourceLoadedListener)
        super.addEventListener(onSourceUnloadedListener)
        super.addEventListener(onStallEndedListener)
        super.addEventListener(onStallStartedListener)
        super.addEventListener(onMetadataListener)
        super.addEventListener(onTimeChangedListener)
        super.addEventListener(onFullscreenEnterListener)
        super.addEventListener(onFullscreenExitListener)
        super.addEventListener(onReadyListener)
    }
}
