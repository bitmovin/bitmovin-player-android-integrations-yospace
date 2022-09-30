package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.advertising.Ad
import com.bitmovin.player.api.advertising.AdBreak

data class AdBreak(
    override var id: String,
    val absoluteStart: Double,
    val relativeStart: Double,
    val duration: Double,
    val absoluteEnd: Double,
    var position: AdBreakPosition = AdBreakPosition.UNKNOWN,
    override val replaceContentDuration: Double = 0.0,
    override var scheduleTime: Double = 0.0,
    override var ads: MutableList<Ad> = mutableListOf()
) : AdBreak {
    override fun toString() =
        "id=$id, " +
        "absoluteStart=$absoluteStart, " +
        "relativeStart=$relativeStart, " +
        "duration=$duration, " +
        "absoluteEnd=$absoluteEnd, " +
        "position=${position.name}, " +
        "replaceContentDuration=$replaceContentDuration, " +
        "scheduleTime=$scheduleTime, " +
        "ads=${ads.size}"
}
