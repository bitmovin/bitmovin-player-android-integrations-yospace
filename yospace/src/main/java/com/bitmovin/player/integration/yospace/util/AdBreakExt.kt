package com.bitmovin.player.integration.yospace.util

import com.bitmovin.player.integration.yospace.AdBreak
import com.bitmovin.player.integration.yospace.SlotType
import com.yospace.android.hls.analytic.advert.AdBreak as YsAdBreak

fun List<YsAdBreak>.toAdBreaks(): List<AdBreak> {
    var relativeOffset = 0.0
    return map { ysAdBreak ->
        // Convert YsAdBreak to AdBreak
        val adBreak = ysAdBreak.toAdBreak(relativeOffset)
        // Convert YSAds to Ads
        adBreak.ads = ysAdBreak.adverts.map { it.toAd(adBreak.relativeStart) }.toMutableList()
        relativeOffset += ysAdBreak.duration.toDouble()
        adBreak
    }
}

fun YsAdBreak.toAdBreak(relativeOffset: Double): AdBreak = AdBreak(
    relativeStart = (startMillis - relativeOffset),
    duration = duration / 1000.0,
    absoluteStart = startMillis / 1000.0,
    absoluteEnd = (startMillis + duration) / 1000.0
)

fun AdBreak.slotType(): SlotType = when (relativeStart) {
    0.0 -> SlotType.PREROLL
    else -> SlotType.MIDROLL
}

