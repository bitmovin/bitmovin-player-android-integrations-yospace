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

class BitmovinTruexRenderer(private val context: Context, private val configuration: TruexConfiguration, var eventListener: TruexAdRendererEventListener? = null) {

    private var renderer: TruexAdRenderer? = null
    private var interactiveUnit: InteractiveUnit? = null
    private var slotType: SlotType = SlotType.PREROLL
    private var isAdFree: Boolean = false
    private var isSessionAdFree: Boolean = false

    fun renderAd(ad: Advert, slotType: SlotType) {
        this.slotType = slotType
        interactiveUnit = ad.linearCreative?.interactiveUnit?.apply {
            let {
                BitLog.d("Rendering TrueX ad: $source")
                try {
                    val adParams = JSONObject(adParameters).apply {
                        putOpt("user_id", configuration.userId)
                        putOpt("vast_config_url", configuration.vastConfigUrl)
                    }
                    renderer = TruexAdRenderer(context).apply {
                        addEventListeners(this)
                        init(source, adParams, slotType.type)
                        start(configuration.viewGroup)
                    }
                    BitLog.d("TrueX rendering completed")
                } catch (e: JSONException) {
                    BitLog.e("TrueX rendering failed $e")

                    // Treat as normal error
                    handleError()
                }
            }
        }
    }

    fun stopRenderer() {
        // Reset state
        renderer?.stop()
        interactiveUnit = null
        slotType = SlotType.PREROLL
        isAdFree = false
        isSessionAdFree = false
    }

    private fun addEventListeners(renderer: TruexAdRenderer) = with(renderer) {
        addEventListener(TruexAdRendererConstants.AD_STARTED) {
            BitLog.d("TrueX ad started: ${it?.get("campaignName")?.toString().orEmpty()}")

            // Reset ad free state
            isAdFree = false

            // Notify YoSpace for ad tracking
            interactiveUnit?.notifyAdStarted()
            interactiveUnit?.notifyAdVideoStart()
            interactiveUnit?.notifyAdImpression()
        }

        addEventListener(TruexAdRendererConstants.AD_COMPLETED) {
            BitLog.d("TrueX ad completed with ${it?.get("timeSpentOnEngagement")
                ?: "0"} seconds spent on engagement")

            // Notify YoSpace for ad tracking
            interactiveUnit?.notifyAdVideoComplete()
            interactiveUnit?.notifyAdStopped()
            interactiveUnit?.notifyAdUserClose()

            // Skip current ad break if:
            //   1. Pre-roll ad free has been satisfied
            //   2. Mid-roll ad free has been satisfied
            if (isSessionAdFree || isAdFree) {
                eventListener?.onSkipAdBreak()
            } else {
                eventListener?.onSkipTruexAd()
            }

            // Reset state
            finishRendering()
        }

        addEventListener(TruexAdRendererConstants.AD_FREE_POD) {
            BitLog.d("TrueX ad free")

            isAdFree = true

            // We are session ad free if ad free is fired on a pre-roll
            if (!isSessionAdFree) {
                isSessionAdFree = (slotType == SlotType.PREROLL)
                if (isSessionAdFree) {
                    eventListener?.onSessionAdFree()
                }
            }
        }

        addEventListener(TruexAdRendererConstants.OPT_IN) {
            BitLog.d("TrueX user opt in: ${it?.get("campaignName")?.toString().orEmpty()}, creativeId=${it?.get("creativeID")}")
        }

        addEventListener(TruexAdRendererConstants.OPT_OUT) {
            BitLog.d("TrueX user opt out")
        }

        addEventListener(TruexAdRendererConstants.AD_ERROR) {
            BitLog.d("TrueX ad error: ${it?.get("message")?.toString().orEmpty()}")
            handleError()
        }

        addEventListener(TruexAdRendererConstants.NO_ADS_AVAILABLE) {
            BitLog.d("No TrueX ads available")
            handleError()
        }

        addEventListener(TruexAdRendererConstants.SKIP_CARD_SHOWN) {
            BitLog.d("TrueX skip card shown")
        }

        addEventListener(TruexAdRendererConstants.USER_CANCEL) {
            BitLog.d("TrueX user cancelled")
        }
    }

    private fun handleError() {
        BitLog.d("Handling TrueX error...")

        // Treat error state like complete state
        if (isSessionAdFree) {
            // Skip ad break as pre-roll ad free has been satisfied
            eventListener?.onSkipAdBreak()
        } else {
            // Skip TrueX ad filler and show linear ads
            eventListener?.onSkipTruexAd()
        }

        // Reset state
        finishRendering()
    }

    private fun finishRendering() {
        renderer?.stop()
        interactiveUnit = null
    }
}
