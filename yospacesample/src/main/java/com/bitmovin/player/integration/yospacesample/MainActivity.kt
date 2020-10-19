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
                "CNN Live",
                "https://live-manifests-aka-qa.warnermediacdn.com/csmp/cmaf/live/2000073/cnn-clear-novpaid/master.m3u8",
                yospaceSourceConfig = YospaceSourceConfiguration(YospaceAssetType.LINEAR)
            ),
            Stream(
                "CNN Companion Ads",
                "https://live-media-aka-qa.warnermediacdn.com/csmp/cmaf/live/2011916/tbseast-cenc-stg-cmp/master_wv.m3u8?yo.pdt=true&yo.vp=false&yo.ad=true&caid=mml-false&conf_csid=ncaa.com_mmodplayer&context=243427194&nw=42448&playername=top-2.1.2-1&prof=48804:tbs_web_vod&vdur=361.5956&yo.vp=true&yo.av=2&yo.ad=true&&yo.ad=true&yo.dnt=false&yo.dr=true",
                "https://widevine-stage.license.istreamplanet.com/widevine/api/license/de4c1d30-ac22-4669-8824-19ba9a1dc128",
                YospaceSourceConfiguration(YospaceAssetType.LINEAR)
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
