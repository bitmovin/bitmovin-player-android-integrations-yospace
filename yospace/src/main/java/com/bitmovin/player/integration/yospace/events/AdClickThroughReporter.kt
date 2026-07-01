package com.bitmovin.player.integration.yospace.events

import com.bitmovin.player.integration.yospace.advertising.Ad
import com.yospace.admanagement.Advert

internal fun interface AdClickThroughHandler {
    fun clickThroughUrlOpened(ad: Ad)
}

internal class AdClickThroughReporter(
    private val emitter: YospaceEventEmitter
) : AdClickThroughHandler {
    private var activeAd: Ad? = null
    private var activeAdvert: Advert? = null

    fun activate(ad: Ad?, advert: Advert) {
        clear()
        activeAd = ad
        activeAdvert = advert
        ad?.clickThroughHandler = this
    }

    fun deactivate(ad: Ad?) {
        if (ad === activeAd) {
            clear()
        } else {
            ad?.clickThroughHandler = null
        }
    }

    fun clear() {
        activeAd?.clickThroughHandler = null
        activeAd = null
        activeAdvert = null
    }

    override fun clickThroughUrlOpened(ad: Ad) {
        if (ad !== activeAd) {
            return
        }

        activeAdvert?.linearCreative?.onClickThrough()
        emitter.emit(YospacePlayerEvent.AdClicked(ad.clickThroughUrl))
    }
}
