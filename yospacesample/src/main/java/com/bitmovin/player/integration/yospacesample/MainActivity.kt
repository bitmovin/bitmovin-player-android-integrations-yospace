package com.bitmovin.player.integration.yospacesample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.CustomData
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.TweaksConfig
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import com.bitmovin.player.integration.yospace.BitLog
import com.bitmovin.player.integration.yospace.BitmovinYospacePlayer
import com.bitmovin.player.integration.yospace.config.YospaceAssetType
import com.bitmovin.player.integration.yospace.config.YospaceConfig
import com.bitmovin.player.integration.yospace.config.YospaceDebugMode
import com.bitmovin.player.integration.yospace.config.YospaceLiveInitializationType
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfig
import com.bitmovin.player.integration.yospace.events.YospacePlayerEvent
import com.bitmovin.player.integration.yospace.events.on
import com.bitmovin.player.integration.yospacesample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private companion object {
        private const val VALIDATION_TAG = "YospaceValidation"
        private const val EXTRA_VALIDATION_MODE = "validationMode"
        private const val EXTRA_VALIDATION_ASSET = "asset"
        private const val EXTRA_VALIDATION_INITIALIZATION_TYPE = "initializationType"
        private const val EXTRA_VALIDATION_TEST_CASE = "testCase"
        private const val TWO_SESSIONS_PLAYBACK_CONFIRMATION_MS = 5_000L
        private const val BETWEEN_SESSIONS_DELAY_MS = 1_000L
        private const val AD_BREAK_TEST_TIMEOUT_MS = 15 * 60 * 1_000L
        private const val TWO_SESSIONS_TEST_TIMEOUT_MS = 5 * 60 * 1_000L

        private const val ENABLE_INTEGRATION_LOGS = true
        private const val ENABLE_YOSPACE_VALIDATION_LOGS = true

        // Replace with your own Bitmovin Analytics license key.
        private const val ANALYTICS_LICENSE_KEY = "YOUR-ANALYTICS-LICENSE-KEY"
        private val LIVE_INITIALIZATION_TYPE = YospaceLiveInitializationType.DIRECT
    }

    private lateinit var player: BitmovinYospacePlayer
    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val validationConfig by lazy { intent.toValidationConfig() }
    private var validationRunner: ValidationRunner? = null

    private val streams by lazy {
        listOf(
            Stream(
                "Yospace HLS Live (${selectedLiveInitializationType.name})",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yosdk02,hls-ts-pre.m3u8?yo.br=false&yo.av=4&yo.lp=true&yo.pdt=true&yo.lpa=dur",
                yospaceSourceConfig = YospaceSourceConfig(YospaceAssetType.LINEAR_START_OVER)
            ),
            Stream(
                "Yospace DASH Live (${selectedLiveInitializationType.name})",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yosdk02,dash-mp4-pre.mpd?yo.br=false&yo.av=4&yo.lp=true&yo.pdt=true&yo.lpa=dur",
                sourceType = SourceType.Dash,
                yospaceSourceConfig = YospaceSourceConfig(YospaceAssetType.LINEAR_START_OVER)
            ),
            Stream(
                "Yospace HLS VOD",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/access/156611618/c2FtcGxlL21hc3Rlci5tM3U4?yo.av=3",
                yospaceSourceConfig = YospaceSourceConfig(
                    assetType = YospaceAssetType.VOD,
                    sourceMetadata = SourceMetadata(
                        title = "Yospace HLS VOD",
                        videoId = "yospace-hls-vod",
                        customData = CustomData(customData1 = "yospace-sample")
                    )
                )
            ),
            Stream(
                "Yospace DASH VOD",
                "https://csm-e-sdk-validation-eb.bln1.yospace.com/csm/access/671396777/ZGFzaC9tYW5pZmVzdC5tcGQ=?yo.av=4",
                sourceType = SourceType.Dash,
                yospaceSourceConfig = YospaceSourceConfig(YospaceAssetType.VOD)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinner()
        setupPlayer()
        addUIListeners()
        setupValidationUi()

        validationConfig?.let {
            validationRunner = ValidationRunner(it)
            validationRunner?.start()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.onResume()
        player.play()
    }

    override fun onPause() {
        player.pause()
        binding.playerView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        binding.playerView.onDestroy()
        super.onDestroy()
    }

    private fun setupPlayer() {
        val playerConfig = PlayerConfig(
            playbackConfig = PlaybackConfig(isAutoplayEnabled = true),
            tweaksConfig = TweaksConfig(useFiletypeExtractorFallbackForHls = true)
        )

        player = BitmovinYospacePlayer(
            this,
            playerConfig,
            analyticsConfig = AnalyticsPlayerConfig.Enabled(
                AnalyticsConfig(
                    licenseKey = ANALYTICS_LICENSE_KEY,
                    // Required for SSAI ad quartile tracking.
                    ssaiEngagementTrackingEnabled = true
                )
            ),
            yospaceConfig = YospaceConfig(
                liveInitializationType = selectedLiveInitializationType,
                isDebug = ENABLE_INTEGRATION_LOGS && validationConfig == null,
                yospaceDebugMode = if (ENABLE_YOSPACE_VALIDATION_LOGS || validationConfig != null) {
                    YospaceDebugMode.VALIDATION
                } else {
                    YospaceDebugMode.NONE
                }
            )
        )
        player.on<SourceEvent.Load> {
            BitLog.d("Change button")
            binding.loadUnloadButton.text = getString(R.string.unload)
        }
        player.on<SourceEvent.Unloaded> {
            binding.loadUnloadButton.text = getString(R.string.load)
            validationRunner?.onStreamUnloaded()
        }
        player.on<PlayerEvent.Playing> {
            validationRunner?.onPlaybackStarted()
        }
        player.on<PlayerEvent.Error> {
            validationRunner?.fail("player-error")
        }
        player.on<YospacePlayerEvent.AdBreakStarted> {
            validationRunner?.onAdBreakStarted()
        }
        player.on<YospacePlayerEvent.AdBreakFinished> {
            validationRunner?.onAdBreakFinished()
        }
        binding.playerView.player = player
        BitLog.d("Setup player")
    }

    private fun setupSpinner() {
        binding.streamSpinner.adapter =
            StreamSpinnerAdapter(this, streams, android.R.layout.simple_spinner_item)
    }

    private fun addUIListeners() {
        binding.loadUnloadButton.setOnClickListener {
            if (player.isPlaying) {
                BitLog.d("unload stream")
                player.unload()
            } else {
                BitLog.d("Button clicked, load stream")
                loadStream(streams[binding.streamSpinner.selectedItemPosition])
            }
        }
    }

    private fun setupValidationUi() {
        val config = validationConfig ?: return

        title = getString(R.string.automatic_validation_run)
        binding.streamSpinner.visibility = View.GONE
        binding.loadUnloadButton.visibility = View.GONE
        binding.validationStatusTextView.visibility = View.VISIBLE
        binding.validationStatusTextView.text = config.statusLabel

        (binding.playerView.layoutParams as ConstraintLayout.LayoutParams).apply {
            topToBottom = binding.validationStatusTextView.id
            binding.playerView.layoutParams = this
        }
    }

    private fun loadStream(stream: Stream) {
        val sourceConfig = SourceConfig(stream.contentUrl, stream.sourceType)
        sourceConfig.drmConfig = WidevineConfig(stream.drmUrl)

        player.load(sourceConfig, stream.yospaceSourceConfig, stream.truexConfig)
    }

    private val selectedLiveInitializationType: YospaceLiveInitializationType
        get() = validationConfig?.liveInitializationType ?: LIVE_INITIALIZATION_TYPE

    private fun validationStream(asset: ValidationAsset) = when (asset) {
        ValidationAsset.VOD -> streams.first { it.yospaceSourceConfig.assetType == YospaceAssetType.VOD }
        ValidationAsset.DVR_LIVE -> streams.first { it.yospaceSourceConfig.assetType == YospaceAssetType.LINEAR_START_OVER }
    }

    private fun logValidation(message: String) {
        Log.i(VALIDATION_TAG, message)
    }

    private fun failValidation(message: String) {
        Log.e(VALIDATION_TAG, "FAIL reason=$message")
    }

    private fun android.content.Intent.toValidationConfig(): ValidationConfig? {
        if (!getBooleanExtra(EXTRA_VALIDATION_MODE, false)) {
            return null
        }

        val asset = enumValueOrNull<ValidationAsset>(getStringExtra(EXTRA_VALIDATION_ASSET))
            ?: ValidationAsset.DVR_LIVE
        val testCase = enumValueOrNull<ValidationTestCase>(getStringExtra(EXTRA_VALIDATION_TEST_CASE))
            ?: ValidationTestCase.AD_BREAK
        val liveInitializationType = enumValueOrNull<YospaceLiveInitializationType>(
            getStringExtra(EXTRA_VALIDATION_INITIALIZATION_TYPE)
        ) ?: YospaceLiveInitializationType.DIRECT

        return ValidationConfig(asset, liveInitializationType, testCase)
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private data class ValidationConfig(
        val asset: ValidationAsset,
        val liveInitializationType: YospaceLiveInitializationType,
        val testCase: ValidationTestCase
    )

    private enum class ValidationAsset {
        VOD,
        DVR_LIVE
    }

    private enum class ValidationTestCase {
        AD_BREAK,
        TWO_SESSIONS
    }

    private inner class ValidationRunner(private val config: ValidationConfig) {
        private var sessionIndex = 0
        private var playbackStartedForSession = false
        private var adBreakStarted = false
        private var inAdBreak = false
        private var pendingUnload = false
        private var awaitingUnload = false
        private var completed = false
        private val timeoutRunnable = Runnable {
            fail("timeout")
        }

        fun start() {
            logValidation(
                "START asset=${config.asset} init=${config.initializationLabel} testCase=${config.testCase}"
            )
            handler.postDelayed(
                timeoutRunnable,
                when (config.testCase) {
                    ValidationTestCase.AD_BREAK -> AD_BREAK_TEST_TIMEOUT_MS
                    ValidationTestCase.TWO_SESSIONS -> TWO_SESSIONS_TEST_TIMEOUT_MS
                }
            )
            loadNextSession()
        }

        fun onPlaybackStarted() {
            if (completed || playbackStartedForSession) {
                return
            }

            playbackStartedForSession = true
            logValidation("PLAYBACK_STARTED session=$sessionIndex")

            if (config.testCase == ValidationTestCase.TWO_SESSIONS) {
                handler.postDelayed(
                    { requestUnload("playback-confirmed") },
                    TWO_SESSIONS_PLAYBACK_CONFIRMATION_MS
                )
            }
        }

        fun onAdBreakStarted() {
            if (completed) {
                return
            }

            inAdBreak = true
            adBreakStarted = true
            logValidation("AD_BREAK_STARTED session=$sessionIndex")
        }

        fun onAdBreakFinished() {
            if (completed) {
                return
            }

            inAdBreak = false
            logValidation("AD_BREAK_FINISHED session=$sessionIndex")

            if (config.testCase == ValidationTestCase.AD_BREAK && adBreakStarted) {
                requestUnload("ad-break-finished")
                return
            }

            if (pendingUnload) {
                requestUnload("pending-after-ad-break")
            }
        }

        fun onStreamUnloaded() {
            if (completed || !awaitingUnload) {
                return
            }

            awaitingUnload = false
            logValidation("STREAM_UNLOADED session=$sessionIndex")

            when (config.testCase) {
                ValidationTestCase.AD_BREAK -> pass()
                ValidationTestCase.TWO_SESSIONS -> {
                    if (sessionIndex < 2) {
                        handler.postDelayed({ loadNextSession() }, BETWEEN_SESSIONS_DELAY_MS)
                    } else {
                        pass()
                    }
                }
            }
        }

        private fun loadNextSession() {
            if (completed) {
                return
            }

            sessionIndex += 1
            playbackStartedForSession = false
            adBreakStarted = false
            inAdBreak = false
            pendingUnload = false
            awaitingUnload = false
            logValidation("LOAD_STREAM session=$sessionIndex")
            loadStream(validationStream(config.asset))
        }

        private fun requestUnload(reason: String) {
            if (completed || awaitingUnload) {
                return
            }

            if (inAdBreak) {
                pendingUnload = true
                logValidation("WAITING_FOR_AD_BREAK_END session=$sessionIndex reason=$reason")
                return
            }

            pendingUnload = false
            awaitingUnload = true
            logValidation("UNLOAD_STREAM session=$sessionIndex reason=$reason")
            player.unload()
        }

        private fun pass() {
            completed = true
            handler.removeCallbacks(timeoutRunnable)
            logValidation("PASS testCase=${config.testCase}")
        }

        fun fail(reason: String) {
            if (completed) {
                return
            }

            completed = true
            handler.removeCallbacks(timeoutRunnable)
            failValidation("$reason asset=${config.asset} init=${config.initializationLabel} testCase=${config.testCase}")
        }
    }

    private val ValidationConfig.initializationLabel: String
        get() = when (asset) {
            ValidationAsset.VOD -> "N/A"
            ValidationAsset.DVR_LIVE -> liveInitializationType.name
        }

    private val ValidationConfig.statusLabel: String
        get() = "${asset.displayName}, ${testCase.displayName}"

    private val ValidationAsset.displayName: String
        get() = when (this) {
            ValidationAsset.VOD -> "VOD"
            ValidationAsset.DVR_LIVE -> "DVR Live"
        }

    private val ValidationTestCase.displayName: String
        get() = when (this) {
            ValidationTestCase.AD_BREAK -> "Test 1"
            ValidationTestCase.TWO_SESSIONS -> "Test 2"
        }
}
