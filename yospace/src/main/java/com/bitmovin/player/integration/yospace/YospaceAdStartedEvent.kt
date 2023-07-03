package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.advertising.AdSourceType

class YospaceAdStartedEvent(
    clientType: AdSourceType = AdSourceType.Unknown,
    clickThroughUrl: String,
    indexInQueue: Int,
    duration: Double,
    timeOffset: Double = 0.0,
    position: String = "position",
    skipOffset: Double = 0.0,
    ad: Ad? = null,
    val companionAds: List<CompanionAd> = emptyList()
): CustomEvent()
