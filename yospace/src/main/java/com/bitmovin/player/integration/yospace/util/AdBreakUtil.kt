package com.bitmovin.player.integration.yospace.util

import com.bitmovin.player.integration.yospace.Ad
import com.bitmovin.player.integration.yospace.AdBreak
import java.util.ArrayList
import com.yospace.android.hls.analytic.advert.AdBreak as YsAdBreak

fun List<YsAdBreak>.toAdBreaks(): List<AdBreak> {
    var relativeOffset = 0.0
    return map { ysAdBreak ->
        // Convert YsAdBreak to AdBreak
        val adBreak = ysAdBreak.toAdBreak(relativeOffset)
        // Convert YSAds to Ads
        adBreak.ads = ysAdBreak.adverts.map { it.toAd(adBreak.relativeStart) }
        relativeOffset += ysAdBreak.duration.toDouble()
        adBreak
    }
}

fun YsAdBreak.toAdBreak(relativeOffset: Double): AdBreak = AdBreak(
    "unknown",
    (startMillis - relativeOffset) / 1000,
    duration / 1000.0,
    startMillis / 1000.0,
    (startMillis + duration) / 1000.0,
    0.0,
    0.0,
    ArrayList<Ad>()
)
