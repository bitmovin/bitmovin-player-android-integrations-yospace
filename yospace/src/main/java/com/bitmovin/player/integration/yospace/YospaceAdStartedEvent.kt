package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.event.data.AdStartedEvent
import com.bitmovin.player.config.advertising.AdSourceType

class YospaceAdStartedEvent(
    clientType: AdSourceType,
    clickThroughUrl: String,
    indexInQueue: Int,
    duration: Double,
    timeOffset: Double,
    position: String,
    skipOffset: Double,
    val isTrueX: Boolean
) : AdStartedEvent(clientType, clickThroughUrl, indexInQueue, duration, timeOffset, position, skipOffset)
