package com.bitmovin.player.integration.yospace

import android.content.Context
import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.bitmovin.player.integration.yospace.util.*
import com.truex.adrenderer.TruexAdRenderer
import com.truex.adrenderer.TruexAdRendererConstants
import com.yospace.android.hls.analytic.advert.Advert
import com.yospace.android.hls.analytic.advert.InteractiveUnit
import org.json.JSONException
import org.json.JSONObject

class BitmovinTruexRenderer(private val configuration: TruexConfiguration, var rendererListener: BitmovinTruexRendererListener? = null, private val context: Context) {

    private var renderer: TruexAdRenderer? = null
    private var interactiveUnit: InteractiveUnit? = null
    private var isAdFree: Boolean = false

    fun renderAd(ad: Advert) {
        interactiveUnit = ad.linearCreative?.interactiveUnit?.apply {
            let {
                BitLog.d("TrueX - ad found: $source")
                BitLog.d("TrueX - rendering ad: $ad")
                try {
                    val adParams = JSONObject(adParameters).apply {
                        putOpt("user_id", configuration.userId)
                        putOpt("vast_config_url", configuration.vastConfigUrl)
                    }
                    val slotType = TruexAdRendererConstants.MIDROLL
                    renderer = TruexAdRenderer(context).apply {
                        addEventListeners(this)
                        init(source, adParams, slotType)
                        start(configuration.viewGroup)
                    }
                    BitLog.d("TrueX - ad rendering completed")
                } catch (e: JSONException) {
                    BitLog.d("TrueX - ad rendering failed")
                }
            }
        }
    }

    fun stop() {
        renderer?.stop()
        interactiveUnit = null
    }

    private fun addEventListeners(renderer: TruexAdRenderer) = with(renderer) {
        addEventListener(TruexAdRendererConstants.AD_STARTED) {
            BitLog.d("TrueX - ad started: ${it?.get("campaignName")?.toString().orEmpty()}")
            interactiveUnit?.notifyAdStarted()
            interactiveUnit?.notifyAdVideoStart()
            interactiveUnit?.notifyAdImpression()
        }
        addEventListener(TruexAdRendererConstants.AD_COMPLETED) {
            BitLog.d("TrueX - ad completed")
            interactiveUnit?.notifyAdVideoComplete()
            interactiveUnit?.notifyAdStopped()
            interactiveUnit?.notifyAdUserClose()
            rendererListener?.onAdFinished(isAdFree)
            stop()
        }
        addEventListener(TruexAdRendererConstants.AD_ERROR) {
            BitLog.d("TrueX - ad error: ${it?.get("message")?.toString().orEmpty()}")
            interactiveUnit?.notifyAdStopped()
            rendererListener?.onAdError()
            stop()
        }
        addEventListener(TruexAdRendererConstants.NO_ADS_AVAILABLE) {
            BitLog.d("TrueX - no ads available")
            interactiveUnit?.notifyAdStopped()
            rendererListener?.onAdError()
            stop()
        }
        addEventListener(TruexAdRendererConstants.AD_FREE_POD) {
            BitLog.d("TrueX - ad free: ${it?.get("timeSpentOnEngagement")
                ?: "0"} seconds spent on engagement")
            isAdFree = true
        }
        addEventListener(TruexAdRendererConstants.SKIP_CARD_SHOWN) {
            BitLog.d("TrueX - skip card shown")
        }
        addEventListener(TruexAdRendererConstants.USER_CANCEL) {
            BitLog.d("TrueX - user cancel")
        }
    }
}
