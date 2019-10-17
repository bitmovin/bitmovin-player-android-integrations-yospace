package com.bitmovin.player.integration.yospace

import java.util.ArrayList

data class AdBreak(
        val identifier: String = "unknown",
        val relativeStart: Double,
        val duration: Double,
        val absoluteStart: Double,
        val absoluteEnd: Double
) {
    val ads = ArrayList<Ad>()

    fun appendAd(ad: Ad) {
        ads.add(ad)
    }
}