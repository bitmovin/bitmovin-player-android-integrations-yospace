package com.bitmovin.player.integration.yospace

import android.content.Context
import com.bitmovin.player.integration.yospace.AdBreakPosition.*
import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.truex.adrenderer.TruexAdRenderer
import com.truex.adrenderer.TruexAdRendererConstants
import com.yospace.android.hls.analytic.advert.Advert
import com.yospace.android.hls.analytic.advert.InteractiveUnit
import org.json.JSONException
import org.json.JSONObject

interface BitmovinTruexAdRendererListener {
    fun onAdCompleted()
    fun onAdFree()
    fun onSessionAdFree()
}

class BitmovinTruexAdRenderer(
    private val context: Context,
    private val configuration: TruexConfiguration
) {

    var listener: BitmovinTruexAdRendererListener? = null
    private var renderer: TruexAdRenderer? = null
    private var interactiveUnit: InteractiveUnit? = null
    private var adBreakPosition = PREROLL
    private var isAdFree = false
    private var isSessionAdFree = false

    fun renderAd(ad: Advert, adBreakPosition: AdBreakPosition) {
        this.adBreakPosition = adBreakPosition
        this.interactiveUnit = ad.linearCreative?.interactiveUnit

        interactiveUnit?.let { interactiveUnit ->
            BitLog.d("Rendering ad: ${interactiveUnit.source}")
            try {
                val adParams = JSONObject(interactiveUnit.adParameters).apply {
                    putOpt("user_id", configuration.userId)
                    putOpt("vast_config_url", configuration.vastConfigUrl)
                }
                renderer = TruexAdRenderer(context).apply {
                    addEventListeners(this)
                    init(interactiveUnit.source, adParams, adBreakPosition.value)
                    start(configuration.viewGroup)
                }
            } catch (e: JSONException) {
                BitLog.e("Failed to render ad: $e")

                // Treat as normal error
                handleError()
            }
        }
    }

    private fun addEventListeners(renderer: TruexAdRenderer) = with(renderer) {
        addEventListener(TruexAdRendererConstants.AD_STARTED) {
            BitLog.d("Ad started: ${it?.get("campaignName")?.toString().orEmpty()}")

            // Reset ad free state
            isAdFree = false

            // Notify YoSpace for ad tracking
            interactiveUnit?.notifyAdStarted()
            interactiveUnit?.notifyAdVideoStart()
            interactiveUnit?.notifyAdImpression()
        }

        addEventListener(TruexAdRendererConstants.AD_COMPLETED) {
            BitLog.d("Ad completed with ${it?.get("timeSpentOnEngagement") ?: "0"} seconds spent on engagement")

            // Notify YoSpace for ad tracking
            interactiveUnit?.notifyAdVideoComplete()
            interactiveUnit?.notifyAdStopped()

            // Skip current ad break if:
            //   1. Pre-roll ad free has been satisfied
            //   2. Mid-roll ad free has been satisfied
            if (isSessionAdFree || isAdFree) {
                listener?.onAdFree()
            } else {
                listener?.onAdCompleted()
            }

            // Reset state
            finish()
        }

        addEventListener(TruexAdRendererConstants.AD_FREE_POD) {
            BitLog.d("Ad free")

            isAdFree = true

            // We are session ad free if ad free is fired on a pre-roll
            if (!isSessionAdFree) {
                isSessionAdFree = (adBreakPosition == PREROLL)
                if (isSessionAdFree) {
                    listener?.onSessionAdFree()
                }
            }
        }

        addEventListener(TruexAdRendererConstants.OPT_IN) {
            BitLog.d("User opt in: ${it?.get("campaignName")?.toString().orEmpty()}, creativeId=${it?.get("creativeID")}")
        }

        addEventListener(TruexAdRendererConstants.OPT_OUT) {
            BitLog.d("User opt out")
        }

        addEventListener(TruexAdRendererConstants.AD_ERROR) {
            BitLog.d("Ad error: ${it?.get("message")?.toString().orEmpty()}")
            handleError()
        }

        addEventListener(TruexAdRendererConstants.NO_ADS_AVAILABLE) {
            BitLog.d("No ads available")
            handleError()
        }

        addEventListener(TruexAdRendererConstants.SKIP_CARD_SHOWN) {
            BitLog.d("Skip card shown")
        }

        addEventListener(TruexAdRendererConstants.USER_CANCEL) {
            BitLog.d("Ad cancelled")
        }

        addEventListener(TruexAdRendererConstants.POPUP_WEBSITE) {
            BitLog.d("Popup website")
        }
    }

    private fun handleError() {
        // Treat error state like complete state
        if (isSessionAdFree) {
            // Skip ad break as pre-roll ad free has been satisfied
            listener?.onAdFree()
        } else {
            // Skip TrueX ad filler and show linear ads
            listener?.onAdCompleted()
        }

        // Reset state
        finish()
    }

    private fun finish() {
        renderer?.stop()
        interactiveUnit = null
    }

    fun stop() {
        // Reset state
        renderer?.stop()
        interactiveUnit = null
        adBreakPosition = PREROLL
        isAdFree = false
        isSessionAdFree = false
    }
}

///////////////////////////////////////////////////////////////////////////
// Extensions
///////////////////////////////////////////////////////////////////////////

private fun InteractiveUnit.notifyAdStarted() = onTrackingEvent("creativeView")

private fun InteractiveUnit.notifyAdStopped() = onTrackingEvent("vpaidstopped")

private fun InteractiveUnit.notifyAdImpression() = onTrackingEvent("impression")

private fun InteractiveUnit.notifyAdVideoStart() = onTrackingEvent("start")

private fun InteractiveUnit.notifyAdVideoComplete() = onTrackingEvent("complete")
