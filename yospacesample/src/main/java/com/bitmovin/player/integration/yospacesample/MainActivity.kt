package com.bitmovin.player.integration.yospacesample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.player.api.event.listener.OnSourceLoadedListener
import com.bitmovin.player.api.event.listener.OnSourceUnloadedListener
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.drm.WidevineConfiguration
import com.bitmovin.player.config.media.HLSSource
import com.bitmovin.player.config.media.SourceConfiguration
import com.bitmovin.player.config.media.SourceItem
import com.bitmovin.player.integration.yospace.BitmovinYospacePlayer
import com.bitmovin.player.integration.yospace.YospaceAssetType
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var player: BitmovinYospacePlayer

    private val streams by lazy {
        listOf(
            Stream(
                "Yospace Live",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yospace02,hlssample42.m3u8?yo.br=true&yo.av=4",
                yospaceSourceConfig = YospaceSourceConfiguration(YospaceAssetType.LINEAR)
            ),
            Stream(
                "Yospace Companion Ads",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/extlive/yospace02,hlssample42.m3u8?yo.br=true&yo.lp=true&yo.av=4",
                "https://widevine-stage.license.istreamplanet.com/widevine/api/license/de4c1d30-ac22-4669-8824-19ba9a1dc128",
                YospaceSourceConfiguration(YospaceAssetType.LINEAR)
            ),
            Stream(
                "Yospace VOD",
                "https://csm-e-sdk-validation.bln1.yospace.com/csm/access/156611618/c2FtcGxlL21hc3Rlci5tM3U4?yo.av=4",
                yospaceSourceConfig = YospaceSourceConfiguration(YospaceAssetType.VOD)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupSpinner()
        setupPlayer()
        addUIListeners()
    }

    override fun onResume() {
        super.onResume()
        playerView.onResume()
        player.play()
    }

    override fun onPause() {
        player.pause()
        playerView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        playerView.onDestroy()
        super.onDestroy()
    }

    private fun setupPlayer() {
        val playerConfig = PlayerConfiguration().apply {
            playbackConfiguration?.isAutoplayEnabled = true
            tweaksConfiguration?.useFiletypeExtractorFallbackForHls = true
        }

        player = BitmovinYospacePlayer(this, playerConfig, YospaceConfiguration()).apply {
            addEventListener(OnSourceLoadedListener {
                loadUnloadButton.text = getString(R.string.unload)
            })
            addEventListener(OnSourceUnloadedListener {
                loadUnloadButton.text = getString(R.string.load)
            })
            playerView.player = this
        }
    }

    private fun setupSpinner() {
        streamSpinner.adapter = StreamSpinnerAdapter(this, streams, android.R.layout.simple_spinner_item)
    }

    private fun addUIListeners() {
        loadUnloadButton.setOnClickListener {
            if (player.isPlaying) {
                player.unload()
            } else {
                loadStream(streams[streamSpinner.selectedItemPosition])
            }
        }
    }

    private fun loadStream(stream: Stream) {
        val sourceItem = SourceItem(
            HLSSource(stream.contentUrl)
        )

        stream.drmUrl?.let {
            sourceItem.addDRMConfiguration(WidevineConfiguration(it))
        }

        val sourceConfig = SourceConfiguration().apply {
            addSourceItem(sourceItem)
        }

        player.load(sourceConfig, stream.yospaceSourceConfig, stream.truexConfig)
    }
}
