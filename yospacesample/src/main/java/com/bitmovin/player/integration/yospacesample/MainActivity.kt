package com.bitmovin.player.integration.yospacesample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bitmovin.player.api.event.listener.*
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.drm.WidevineConfiguration
import com.bitmovin.player.config.media.HLSSource
import com.bitmovin.player.config.media.SourceConfiguration
import com.bitmovin.player.config.media.SourceItem
import com.bitmovin.player.integration.yospace.*
import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration
import com.bitmovin.player.model.MediaType
import com.bitmovin.player.model.buffer.BufferType
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var listAdapter: ListAdapter

    private lateinit var bitmovinYospacePlayer: BitmovinYospacePlayer

    private var adStartedCount: Int = 0
    private var adBreakStartedCount: Int = 0
    private var adFinishedCount: Int = 0
    private var adBreakFinishedCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initBitmovinYospacePlayer()
        initListUI()
        setUIListeners()
        addEventListeners()

        player_view.player = bitmovinYospacePlayer
    }

    override fun onResume() {
        super.onResume()
        player_view.onResume()
        bitmovinYospacePlayer.play()
    }

    override fun onPause() {
        bitmovinYospacePlayer.pause()
        player_view.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        player_view.onDestroy()
        super.onDestroy()
    }

    private fun initBitmovinYospacePlayer() {
        val playerConfiguration = PlayerConfiguration()
        val yospaceConfiguration = YospaceConfiguration(isDebug = true)
        bitmovinYospacePlayer = BitmovinYospacePlayer(this, playerConfiguration, yospaceConfiguration).apply {
            config.playbackConfiguration.isAutoplayEnabled = true
            setPlayerPolicy(BitmovinYospacePolicy(this))
        }
    }

    private fun initListUI() {
        listAdapter = ListAdapter()
        recycler_view.adapter = listAdapter
        recycler_view.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler_view.layoutManager = LinearLayoutManager(this)
    }

    private fun setUIListeners() {
        load_unload_button.setOnClickListener {
            if (load_unload_button.text == getString(R.string.load)) {
                when (stream_spinner.selectedItemPosition) {
                    0 -> loadLiveCNN()
                    1 -> loadLiveTBSE()
                    2 -> loadVod()
                    3 -> loadTruex()
                    4 -> loadNonYospace()
                }
            } else {
                bitmovinYospacePlayer.unload()
            }
            resetUI()
        }

        seek_button.setOnClickListener {
            bitmovinYospacePlayer.seek((seek_edit_text.text.toString()).toDouble())
        }
    }

    private fun addEventListeners() {
        with(bitmovinYospacePlayer) {
            addEventListener(OnSourceLoadedListener {
                load_unload_button.text = getString(R.string.unload)
            })

            addEventListener(OnSourceUnloadedListener {
                load_unload_button.text = getString(R.string.load)
            })

            addEventListener(OnReadyListener {
                logMessage("Ad Breaks: ${bitmovinYospacePlayer.adTimeline}")
                showListAdBreaks()
            })

            addEventListener(OnTimeChangedListener {
                updateBufferUI()
                updateAdCounterUI()
            })

            addEventListener(OnAdBreakStartedListener {
                listAdapter.clear()
                ad_counter_text_view.visibility = View.VISIBLE
                abs_text_view.text = String.format(Locale.US, "ABS: %d", ++adBreakStartedCount)
                val adBreak = it.adBreak as AdBreak?
                showListAds(adBreak?.ads ?: emptyList())
                logMessage("Ad Break Started: id=${adBreak?.id}, duration=${adBreak?.duration}")
            })

            addEventListener(OnAdBreakFinishedListener {
                listAdapter.clear()
                ad_counter_text_view.visibility = View.GONE
                abf_text_view.text = String.format(Locale.US, "ABF: %d", ++adBreakFinishedCount)
                val adBreak = it.adBreak as AdBreak?
                showListAdBreaks()
                logMessage("Ad Break Finished: id=${adBreak?.id}, duration=${adBreak?.duration}")
            })

            addEventListener(OnAdStartedListener {
                as_text_view.text = String.format(Locale.US, "AS: %d", ++adStartedCount)
                val ad = it.ad as Ad?
                logMessage("Ad Started: id=${ad?.id}, duration=${ad?.duration}")
            })

            addEventListener(OnAdFinishedListener {
                af_text_view.text = String.format(Locale.US, "AF: %d", ++adFinishedCount)
                val ad = it.ad as Ad?
                logMessage("Ad Finished: id=${ad?.id}, duration=${ad?.duration}")
            })

            addEventListener(OnAdClickedListener {
                logMessage("Ad Clicked: clickThroughUrl=${it.clickThroughUrl}")
            })

            addEventListener(OnAdErrorListener {
                logMessage("Ad Error: code=${it.code}")
            })

            addEventListener(OnAdSkippedListener {
                val ad = it.ad as Ad?
                logMessage("Ad Skipped: id=${ad?.id}, duration=${ad?.duration}")
            })

            addEventListener(OnErrorListener {
                resetUI()
                logMessage("Error: code=${it.code}, message=${it.message}")
            })
        }
    }

    private fun loadLiveTBSE() {
        val drmConfiguration = WidevineConfiguration("https://widevine-stage.license.istreamplanet.com/widevine/api/license/de4c1d30-ac22-4669-8824-19ba9a1dc128")
        val hlsSource = HLSSource("https://live-media-aka-qa.warnermediacdn.com/csmp/cmaf/live/2011916/tbseast-cenc-stage/master_wv.m3u8?yo.pdt=true&_fw_ae=53da17a30bd0d3c946a41c86cb5873f1&_fw_ar=1&afid=180483280&conf_csid=tbs.com_desktop_live_east&nw=42448&prof=48804:tbs_web_live&yo.vp=true&yo.ad=true")
        val sourceItem = SourceItem(hlsSource).apply { addDRMConfiguration(drmConfiguration) }
        val sourceConfig = SourceConfiguration().apply { addSourceItem(sourceItem) }
        val yospaceSourceConfiguration = YospaceSourceConfiguration(YospaceAssetType.LINEAR)
        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration)
    }

    private fun loadLiveCNN() {
        val sourceItem = SourceItem(HLSSource("https://live-manifests-aka-qa.warnermediacdn.com/csmp/cmaf/live/2000073/cnn-clear/master.m3u8"))
        val sourceConfig = SourceConfiguration().apply { addSourceItem(sourceItem) }
        val yospaceSourceConfiguration = YospaceSourceConfiguration(YospaceAssetType.LINEAR)
        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration)
    }

    private fun loadVod() {
        val sourceItem = SourceItem(HLSSource("https://vod-manifests-aka-qa.warnermediacdn.com/csm/tcm/clear/3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c/master_cl.m3u8?afid=222591187&caid=2100555&conf_csid=tbs.com_videopage&context=182883174&nw=42448&prof=48804%3Atbs_web_vod&vdur=1800&yo.vp=false"))
        val sourceConfig = SourceConfiguration().apply { addSourceItem(sourceItem) }
        val yospaceSourceConfiguration = YospaceSourceConfiguration(YospaceAssetType.VOD)
        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration)
    }

    private fun loadTruex() {
        val sourceItem = SourceItem(HLSSource("https://vod-manifests-aka-qa.warnermediacdn.com/csm/tcm/clear/3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c/master_cl.m3u8?_fw_nielsen_app_id=P923E8EA9-9B1B-4F15-A180-F5A4FD01FE38&afid=222591187&caid=2100555&conf_csid=tbs.com_mobile_androidphone&context=182883174&nw=42448&prof=48804%3Amp4_plus_vast_truex&vdur=1800&yo.vp=true"))
        val sourceConfig = SourceConfiguration().apply { addSourceItem(sourceItem) }
        val yospaceSourceConfiguration = YospaceSourceConfiguration(YospaceAssetType.VOD)
        val truexConfiguration = TruexConfiguration(player_view)
        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfiguration, truexConfiguration)
    }

    private fun loadNonYospace() {
        val sourceItem = SourceItem(HLSSource("https://hls.pro34.lv3.cdn.hbo.com/av/videos/series/watchmen/videos/trailer/trailer-47867523_PRO34/base_index.m3u8"))
        val sourceConfig = SourceConfiguration().apply { addSourceItem(sourceItem) }
        bitmovinYospacePlayer.load(sourceConfig)
    }

    private fun updateBufferUI() {
        val backwardBuffer = bitmovinYospacePlayer.buffer.getLevel(BufferType.BACKWARD_DURATION, MediaType.VIDEO).level
        val forwardBuffer = bitmovinYospacePlayer.buffer.getLevel(BufferType.FORWARD_DURATION, MediaType.VIDEO).level
        buffer_text_view.text = String.format(Locale.getDefault(), "Buffer: [%.2f %.2f]", backwardBuffer, forwardBuffer)
    }

    private fun updateAdCounterUI() {
        val activeAdBreak = bitmovinYospacePlayer.activeAdBreak
        val activeAd = bitmovinYospacePlayer.activeAd
        if (activeAdBreak != null && activeAd != null) {
            val adBreakSecsRemaining = (activeAdBreak.duration - (bitmovinYospacePlayer.currentTimeWithAds() - activeAdBreak.absoluteStart)).toInt()
            val adSecsRemaining = (activeAd.duration - bitmovinYospacePlayer.currentTime).toInt()
            val totalAds = activeAdBreak.ads.size
            val adIndex = activeAdBreak.ads.indexOfFirst { it.id == activeAd.id } + 1
            ad_counter_text_view.text = String.format(
                Locale.US, "AdBreak: %ds - Ad: %d of %d - %ds", adBreakSecsRemaining, adIndex, totalAds, adSecsRemaining
            )
        }
    }

    private fun resetUI() {
        ad_counter_text_view.visibility = View.GONE
        listAdapter.clear()
        adBreakStartedCount = 0
        adStartedCount = 0
        adFinishedCount = 0
        adBreakFinishedCount = 0
        abs_text_view.text = getString(R.string.abs_default)
        as_text_view.text = getString(R.string.as_default)
        af_text_view.text = getString(R.string.af_default)
        abf_text_view.text = getString(R.string.abf_default)
        load_unload_button.text = getString(R.string.load)
    }

    private fun showListAdBreaks() {
        bitmovinYospacePlayer.adTimeline?.let {
            listAdapter.add(ListItem("Seq", "Start", "Duration", "Ads"))
            val adBreaks = it.adBreaks
            adBreaks.forEachIndexed { i, adBreak ->
                val listItem = ListItem(
                    (i + 1).toString(),
                    adBreak.absoluteStart.toString(),
                    adBreak.duration.toString(),
                    adBreak.ads.size.toString()
                )
                listAdapter.add(listItem)
            }
        }
    }

    private fun showListAds(ads: List<com.bitmovin.player.model.advertising.Ad>) {
        if (ads.isNotEmpty()) {
            listAdapter.add(ListItem("Seq", "Id", "Duration", "TrueX"))
            ads.forEach {
                val item = it as Ad
                val listItem = ListItem(
                    item.sequence.toString(),
                    item.id ?: "",
                    item.duration.toString(),
                    item.isTruex.toString()
                )
                listAdapter.add(listItem)
            }
        }
    }

    private fun logMessage(message: String) {
        Log.d(TAG, message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
