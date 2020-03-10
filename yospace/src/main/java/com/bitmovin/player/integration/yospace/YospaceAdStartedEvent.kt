package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.event.data.AdStartedEvent
import com.bitmovin.player.config.advertising.AdSourceType
import com.bitmovin.player.model.advertising.Ad

class YospaceAdStartedEvent(
    clientType: AdSourceType = AdSourceType.UNKNOWN,
    clickThroughUrl: String,
    indexInQueue: Int,
    duration: Double,
    timeOffset: Double = 0.0,
    position: String = "position",
    skipOffset: Double = 0.0,
    val isTruex: Boolean = false,
    ad: Ad? = null
) : AdStartedEvent(clientType, clickThroughUrl, indexInQueue, duration, timeOffset, position, skipOffset, ad)
