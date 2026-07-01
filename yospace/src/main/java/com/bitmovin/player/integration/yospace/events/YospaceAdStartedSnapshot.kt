package com.bitmovin.player.integration.yospace.events

import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.integration.yospace.advertising.Ad
import com.bitmovin.player.integration.yospace.advertising.CompanionAd

internal data class YospaceAdStartedSnapshot(
    val ad: Ad?,
    val companionAds: List<CompanionAd>,
    val clientType: AdSourceType,
    val clickThroughUrl: String,
    val indexInQueue: Int,
    val duration: Double,
    val timeOffset: Double,
    val position: String,
    val skipOffset: Double = 0.0
) {
    fun toYospacePlayerEvent() = YospacePlayerEvent.AdStarted(
        ad = ad,
        companionAds = companionAds,
        clientType = clientType,
        clickThroughUrl = clickThroughUrl,
        indexInQueue = indexInQueue,
        duration = duration,
        timeOffset = timeOffset,
        position = position,
        skipOffset = skipOffset
    )
}
