package com.bitmovin.player.integration.yospace.util

import com.bitmovin.player.integration.yospace.Ad
import com.yospace.android.hls.analytic.advert.Advert as YsAd


fun YsAd.adClickThroughUrl(): String = linearCreative?.videoClicks?.clickThroughUrl ?: ""

fun YsAd.adMimeType(): String = linearCreative?.interactiveUnit?.mimeType ?: ""

fun List<YsAd>.toAds(adBreakAbsoluteStart: Double, adBreakRelativeStart: Double): List<Ad> {
    var adAbsoluteStart = adBreakAbsoluteStart
    return map {
        val ad = Ad(
            it.id,
            adBreakRelativeStart,
            it.duration / 1000.0,
            adAbsoluteStart,
            adAbsoluteStart + it.duration / 1000.0,
            it.sequence,
            it.hasLinearInteractiveUnit(),
            it.hasLinearInteractiveUnit(),
            it.extensions,
            !it.hasLinearInteractiveUnit(),
            it.adClickThroughUrl()
        )
        adAbsoluteStart += it.duration / 1000.0
        ad
    }
}

fun YsAd.toAd(absoluteStart: Double = startMillis / 1000.0, relativeStart: Double): Ad = Ad(
    id = id,
    relativeStart = relativeStart,
    duration = duration / 1000.0,
    absoluteStart = absoluteStart,
    absoluteEnd = absoluteStart + (duration / 1000.0),
    sequence = sequence,
    hasInteractiveUnit = hasLinearInteractiveUnit(),
    isTruex = hasLinearInteractiveUnit(),
    extensions = extensions,
    isLinear = !hasLinearInteractiveUnit(),
    clickThroughUrl = adClickThroughUrl()
)
