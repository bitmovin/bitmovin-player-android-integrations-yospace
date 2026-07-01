package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.advertising.AdSourceType

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
    fun toYospaceAdStartedEvent() = YospaceAdStartedEvent(
        clientType = clientType,
        clickThroughUrl = clickThroughUrl,
        indexInQueue = indexInQueue,
        duration = duration,
        timeOffset = timeOffset,
        position = position,
        skipOffset = skipOffset,
        ad = ad,
        companionAds = companionAds
    )

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
