package com.bitmovin.player.integration.yospacesample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.player.api.PlayerConfig
import com.bitmovin.player.api.PlaybackConfig
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.TweaksConfig
import com.bitmovin.player.api.drm.WidevineConfig
import com.bitmovin.player.api.event.SourceEvent
import com.bitmovin.player.api.event.on
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import com.bitmovin.player.integration.yospace.BitLog
import com.bitmovin.player.integration.yospace.BitmovinYospacePlayer
import com.bitmovin.player.integration.yospace.YospaceAssetType
import com.bitmovin.player.integration.yospace.config.YospaceConfig
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfig
import com.bitmovin.player.integration.yospacesample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var player: BitmovinYospacePlayer
    private lateinit var binding: ActivityMainBinding

    private val streams by lazy {
        listOf(
            Stream(
                "Yospace Live",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yospace02,hlssample42.m3u8?yo.br=true&yo.av=4",
                yospaceSourceConfig = YospaceSourceConfig(YospaceAssetType.LINEAR)
            ),
            Stream(
                "Yospace Companion Ads",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yospace02,hlssample42.m3u8?yo.br=true&yo.lp=true&yo.av=4",
                "https://widevine-stage.license.istreamplanet.com/widevine/api/license/de4c1d30-ac22-4669-8824-19ba9a1dc128",
                YospaceSourceConfig(YospaceAssetType.LINEAR)
            ),
            Stream(
                "Yospace VOD",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/access/207411697/c2FtcGxlL21hc3Rlci5tM3U4?yo.av=3",
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
        binding.playerView.onDestroy()
        super.onDestroy()
    }

    private fun setupPlayer() {
        val playerConfig = PlayerConfig(
            playbackConfig = PlaybackConfig(isAutoplayEnabled = true),
            tweaksConfig = TweaksConfig(useFiletypeExtractorFallbackForHls = true)
        )

        player = BitmovinYospacePlayer(this, playerConfig, yospaceConfig = YospaceConfig())
        player.on<SourceEvent.Load> {
            BitLog.d("Change button")
            binding.loadUnloadButton.text = getString(R.string.unload)
        }
        player.on<SourceEvent.Unloaded> {
            binding.loadUnloadButton.text = getString(R.string.load)
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

    private fun loadStream(stream: Stream) {
        val sourceConfig = SourceConfig(stream.contentUrl, SourceType.Hls)
        sourceConfig.drmConfig = WidevineConfig(stream.drmUrl)

        player.load(sourceConfig, stream.yospaceSourceConfig, stream.truexConfig)
    }
}
