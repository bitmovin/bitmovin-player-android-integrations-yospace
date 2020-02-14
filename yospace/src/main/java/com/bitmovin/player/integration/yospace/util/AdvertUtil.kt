package com.bitmovin.player.integration.yospace.util

import com.yospace.android.hls.analytic.advert.Advert

object AdvertUtil {

    fun Advert.adClickThroughUrl(): String = linearCreative?.videoClicks?.clickThroughUrl ?: ""

    fun Advert.adMimeType(): String = linearCreative?.interactiveUnit?.mimeType ?: ""

    fun Advert.isTruex(): Boolean = adSystem?.adSystemType == "trueX"
}
