package com.bitmovin.player.integration.yospacesample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.bitmovin.player.api.event.listener.*
import com.bitmovin.player.config.PlayerConfiguration
import com.bitmovin.player.config.media.HLSSource
import com.bitmovin.player.config.media.SourceConfiguration
import com.bitmovin.player.config.media.SourceItem
import com.bitmovin.player.integration.yospace.BitmovinYospacePlayer
import com.bitmovin.player.integration.yospace.YospaceAdStartedEvent
import com.bitmovin.player.integration.yospace.YospaceAssetType
import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = "MainActivity"

    private lateinit var bitmovinYospacePlayer: BitmovinYospacePlayer
    private lateinit var truexConfiguration: TruexConfiguration

    private var currentClickThroughUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vodButton.setOnClickListener(this)
        liveButton.setOnClickListener(this)
        unloadButton.setOnClickListener(this)
        customButton.setOnClickListener(this)
        truexButton.setOnClickListener(this)
        clickThroughButton.setOnClickListener(this)
        defaultButton.setOnClickListener(this)

        val playerConfiguration = PlayerConfiguration()
        val yospaceConfiguration = YospaceConfiguration(readTimeout = 25_000, connectTimeout = 25_000, requestTimeout = 25_000)
        truexConfiguration = TruexConfiguration(bitmovinPlayerView, "turner_bm_ys_tester_001", "qa-get.truex.com/07d5fe7cc7f9b5ab86112433cf0a83b6fb41b092/vast/config?asnw=&cpx_url=&dimension_2=0&flag=%2Bamcb%2Bemcr%2Bslcb%2Bvicb%2Baeti-exvt&fw_key_values=&metr=0&network_user_id=turner_bm_ys_tester_001&prof=g_as3_truex&ptgt=a&pvrn=&resp=vmap1&slid=fw_truex&ssnw=&stream_position=midroll&vdur=&vprn=")

        bitmovinYospacePlayer = BitmovinYospacePlayer(applicationContext, playerConfiguration, yospaceConfiguration)
        bitmovinYospacePlayer.config.playbackConfiguration.isAutoplayEnabled = true
        bitmovinYospacePlayer.addEventListener(onAdBreakStartedListener)
        bitmovinYospacePlayer.addEventListener(onAdBreakFinishedListener)
        bitmovinYospacePlayer.addEventListener(onAdStartedListener)
        bitmovinYospacePlayer.addEventListener(onAdFinishedListener)
        bitmovinYospacePlayer.addEventListener(onAdClickedListener)
        bitmovinYospacePlayer.addEventListener(onAdErrorListener)
        bitmovinYospacePlayer.addEventListener(onAdSkippedListener)
        bitmovinYospacePlayer.addEventListener(onErrorListener)
        bitmovinYospacePlayer.addEventListener(onReadyListener)
        bitmovinYospacePlayer.addEventListener(onWarningListener)
        bitmovinYospacePlayer.setPlayerPolicy(BitmovinYospacePolicy(bitmovinYospacePlayer))

        bitmovinPlayerView.player = bitmovinYospacePlayer
    }

    override fun onResume() {
        super.onResume()
        bitmovinPlayerView.onResume()
        bitmovinYospacePlayer.play()
    }

    override fun onPause() {
        bitmovinYospacePlayer.pause()
        bitmovinPlayerView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        bitmovinPlayerView.onDestroy()
        super.onDestroy()
    }

    override fun onClick(view: View) {
        when (view) {
            liveButton -> loadLive()
            vodButton -> loadVod()
            unloadButton -> unload()
            clickThroughButton -> clickThroughPressed()
            customButton -> loadCustomUrl()
            truexButton -> loadTruex()
            defaultButton -> loadDefault()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var handled = false

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A ->
                // ... handle selections
                handled = true
            KeyEvent.KEYCODE_DPAD_LEFT ->
                // ... handle left action
                handled = true
            KeyEvent.KEYCODE_DPAD_RIGHT ->
                // ... handle right action
                handled = true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun unload() = bitmovinYospacePlayer.unload()

    private fun loadLive() {
        val sourceConfig = createSourceConfiguration("http://csm-e.cds1.yospace.com/csm/live/158519304.m3u8?yo.ac=false")
        val yospaceSourceConfig = YospaceSourceConfiguration(YospaceAssetType.LINEAR)

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfig)
    }

    private fun loadVod() {
        val sourceConfig = createSourceConfiguration("https://turnercmaf.cdn.turner.com/csm/qa/cmaf_advanced_fmp4_from_inter/prog_seg/bones_RADS1008071800025944_v12/clear/3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c/master_cl_ifp.m3u8?context=525947592")
        val yospaceSourceConfig = YospaceSourceConfiguration(YospaceAssetType.VOD)

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfig)
    }

    private fun loadDefault() {
        val sourceConfig = createSourceConfiguration("http://csm-e.cds1.yospace.com/access/d/400/u/0/1/130782300?f=0000130753172&format=vmap")
        val yospaceSourceConfig = YospaceSourceConfiguration(YospaceAssetType.VOD)

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfig)
    }

    private fun loadTruex() {
        val sourceConfig = createSourceConfiguration("https://turnercmaf.warnermediacdn.com/csm/qa/cmaf_advanced_fmp4_from_inter/prog_seg/bones_RADS1008071800025944_v12/clear/3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c3c/master_cl_ifp.m3u8?context=525955018")
        val yospaceSourceConfig = YospaceSourceConfiguration(YospaceAssetType.VOD)

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfig, truexConfiguration)
    }

    private fun loadCustomUrl() {
        val url = urlEditText.text.toString()
        val sourceConfig = createSourceConfiguration(url)

        val yospaceSourceConfig = if (assetTypeSpinner.selectedItemPosition == 0) {
            YospaceSourceConfiguration(YospaceAssetType.LINEAR)
        } else {
            YospaceSourceConfiguration(YospaceAssetType.VOD)
        }

        bitmovinYospacePlayer.load(sourceConfig, yospaceSourceConfig)
    }

    private fun createSourceConfiguration(url: String) : SourceConfiguration {
        val sourceItem = SourceItem(HLSSource(url))
        val sourceConfig = SourceConfiguration()
        sourceConfig.addSourceItem(sourceItem)
        return sourceConfig
    }

    private fun clickThroughPressed() {
        bitmovinYospacePlayer.clickThroughPressed()

        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentClickThroughUrl))
        startActivity(browserIntent)
    }

    private val onReadyListener = OnReadyListener { Log.d(TAG, "Ad breaks - ${bitmovinYospacePlayer.adTimeline}") }

    private val onAdBreakStartedListener = OnAdBreakStartedListener { Toast.makeText(this, "Ad break started", Toast.LENGTH_SHORT).show() }

    private val onAdBreakFinishedListener = OnAdBreakFinishedListener { Toast.makeText(this, "Ad break finished", Toast.LENGTH_SHORT).show() }

    private val onAdStartedListener = OnAdStartedListener { adStartedEvent ->
        if (adStartedEvent !is YospaceAdStartedEvent) {
            return@OnAdStartedListener
        }
        Log.d(TAG, "Ad started - true[X]=${adStartedEvent.isTruex}")

        currentClickThroughUrl = adStartedEvent.getClickThroughUrl()

        val url = currentClickThroughUrl

        if (url != null && url.isNotEmpty()) {
            clickThroughButton.isEnabled = true
            clickThroughButton.isClickable = true
        }
        bitmovinYospacePlayer.getActiveAd()?.let { ad ->
            Toast.makeText(this, "Ad started - id=${ad.identifier}", Toast.LENGTH_SHORT).show()
        }
    }

    private val onAdFinishedListener = OnAdFinishedListener {
        clickThroughButton.isEnabled = false
        clickThroughButton.isClickable = false
        Toast.makeText(this, "Ad finished", Toast.LENGTH_SHORT).show()
    }

    private val onAdSkippedListener = OnAdSkippedListener { Toast.makeText(this, "Ad skipped", Toast.LENGTH_SHORT).show() }

    private val onAdClickedListener = OnAdClickedListener { Toast.makeText(this, "Ad clicked", Toast.LENGTH_SHORT).show() }

    private val onAdErrorListener = OnAdErrorListener { adErrorEvent ->  Toast.makeText(this, "Ad error - message=${adErrorEvent.message}", Toast.LENGTH_SHORT).show() }

    private val onErrorListener = OnErrorListener { errorEvent -> Toast.makeText(this, "Error - code=${errorEvent.code}, message=${errorEvent.message}", Toast.LENGTH_SHORT).show() }

    private val onWarningListener = OnWarningListener { warningEvent ->  Toast.makeText(this, "Warning - message=${warningEvent.message}", Toast.LENGTH_SHORT).show() }
}
