package com.bitmovin.player.integration.yospace.util

import com.bitmovin.player.integration.yospace.AdBreak
import com.bitmovin.player.integration.yospace.SlotType
import com.yospace.android.hls.analytic.advert.AdBreak as YsAdBreak

fun List<YsAdBreak>.toAdBreaks(): List<AdBreak> {
    var relativeOffset = 0.0
    return map { ysAdBreak ->
        // Convert YsAdBreak to AdBreak
        val adBreak = ysAdBreak.toAdBreak(relativeStart = ysAdBreak.startMillis / 1000.0 - relativeOffset)
        relativeOffset += ysAdBreak.duration / 1000.0
        adBreak
    }
}

fun YsAdBreak.toAdBreak(absoluteStart: Double = startMillis / 1000.0, relativeStart: Double): AdBreak = AdBreak(
    relativeStart = relativeStart,
    absoluteStart = absoluteStart,
    duration = duration / 1000.0,
    absoluteEnd = absoluteStart + (duration / 1000.0),
    ads = adverts.toAds(absoluteStart, relativeStart).toMutableList()
)

fun AdBreak.slotType(): SlotType = when (relativeStart) {
    0.0 -> SlotType.PREROLL
    else -> SlotType.MIDROLL
}

