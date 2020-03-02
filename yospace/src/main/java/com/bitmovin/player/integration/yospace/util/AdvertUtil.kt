package com.bitmovin.player.integration.yospace.util

import com.bitmovin.player.integration.yospace.Ad
import com.yospace.android.hls.analytic.advert.Advert as YsAd


fun YsAd.adClickThroughUrl(): String = linearCreative?.videoClicks?.clickThroughUrl ?: ""

fun YsAd.adMimeType(): String = linearCreative?.interactiveUnit?.mimeType ?: ""

fun YsAd.isTruex(): Boolean = adSystem?.adSystemType == "trueX"

fun YsAd.toAd(adBreakRelativeStart: Double): Ad = Ad(
    id = identifier,
    relativeStart = adBreakRelativeStart / 1000,
    duration = duration / 1000.0,
    absoluteStart = startMillis / 1000.0,
    absoluteEnd = (startMillis + duration) / 1000.0,
    sequence = sequence,
    isHasInteractiveUnit = hasLinearInteractiveUnit(),
    isTruex = isTruex(),
    isLinear = !isTruex(),
    clickThroughUrl = adClickThroughUrl()
)
