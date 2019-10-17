package com.bitmovin.player.integration.yospace.util

import com.bitmovin.player.config.advertising.AdSourceType
import com.bitmovin.player.integration.yospace.Ad
import com.bitmovin.player.integration.yospace.YospaceAdStartedEvent
import com.yospace.android.hls.analytic.advert.Advert as YospaceAd

fun Ad.toAdStartedEvent() : YospaceAdStartedEvent =
        YospaceAdStartedEvent(AdSourceType.UNKNOWN, clickThroughUrl, sequence, duration / 1000, relativeStart / 1000, "position", 0.0, isTrueX)

fun buildEmptyAdStartedEvent() : YospaceAdStartedEvent =
        YospaceAdStartedEvent(AdSourceType.UNKNOWN, "", 0, 0.0, 0.0, "0", 0.0, true)

fun YospaceAd.toAdStartedEvent() : YospaceAdStartedEvent =
        YospaceAdStartedEvent(AdSourceType.UNKNOWN, getAdClickThroughUrl(), sequence, duration / 1000.0, startMillis / 1000.0, "position", 0.0, isTrueX())

fun YospaceAd.getAdClickThroughUrl(): String =
        if (linearCreative != null && linearCreative.videoClicks != null) {
            linearCreative.videoClicks.clickThroughUrl
        } else {
            ""
        }

private fun YospaceAd.isTrueX() : Boolean = adSystem.adSystemType == "trueX"