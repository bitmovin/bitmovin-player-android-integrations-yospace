package com.bitmovin.player.integration.yospace.util

import com.bitmovin.player.config.advertising.AdSourceType
import com.bitmovin.player.integration.yospace.Ad
import com.bitmovin.player.integration.yospace.AdBreak
import com.bitmovin.player.integration.yospace.YospaceAdStartedEvent
import com.yospace.android.hls.analytic.advert.AdBreak as YospaceAdBreak
import com.yospace.android.hls.analytic.advert.Advert as YospaceAd

fun Ad.toAdStartedEvent(): YospaceAdStartedEvent =
    YospaceAdStartedEvent(AdSourceType.UNKNOWN, clickThroughUrl, sequence, duration / 1000, relativeStart / 1000, "position", 0.0, isTrueX)

fun emptyAdStartedEvent(): YospaceAdStartedEvent =
    YospaceAdStartedEvent(AdSourceType.UNKNOWN, "", 0, 0.0, 0.0, "0", 0.0, true)

fun List<YospaceAdBreak>.toAdBreaks(): List<AdBreak> {
    var offset = 0.0
    return map { yospaceAdBreak ->
        val adBreak = AdBreak(
            "unknown",
            (yospaceAdBreak.startMillis - offset) / 1000,
            yospaceAdBreak.duration / 1000.0,
            yospaceAdBreak.startMillis / 1000.0,
            (yospaceAdBreak.startMillis + yospaceAdBreak.duration) / 1000.0
        )
        offset += yospaceAdBreak.duration
        yospaceAdBreak.adverts.forEach { yospaceAd ->
            val ad = Ad(
                yospaceAd.identifier,
                adBreak.relativeStart / 1000,
                yospaceAd.duration / 1000.0,
                yospaceAd.startMillis / 1000.0,
                (yospaceAd.startMillis + yospaceAd.duration) / 1000.0,
                yospaceAd.sequence,
                yospaceAd.getAdClickThroughUrl(),
                yospaceAd.hasLinearInteractiveUnit(),
                yospaceAd.isTrueX()
            )
            adBreak.appendAd(ad)
        }
        adBreak
    }
}

fun YospaceAd.toAdStartedEvent(): YospaceAdStartedEvent =
    YospaceAdStartedEvent(AdSourceType.UNKNOWN, getAdClickThroughUrl(), sequence, duration / 1000.0, startMillis / 1000.0, "position", 0.0, isTrueX())

fun YospaceAd.getAdClickThroughUrl(): String =
    if (linearCreative != null && linearCreative.videoClicks != null) {
        linearCreative.videoClicks.clickThroughUrl
    } else {
        ""
    }

private fun YospaceAd.isTrueX(): Boolean = adSystem.adSystemType == "trueX"
