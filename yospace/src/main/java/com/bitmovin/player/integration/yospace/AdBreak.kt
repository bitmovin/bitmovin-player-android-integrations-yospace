package com.bitmovin.player.integration.yospace

import com.bitmovin.player.model.advertising.Ad

import java.util.ArrayList

data class AdBreak(
    override var id: String = "unknown",
    val relativeStart: Double,
    val duration: Double,
    val absoluteStart: Double,
    val absoluteEnd: Double,
    override val replaceContentDuration: Double = 0.0,
    override var scheduleTime: Double = 0.0,
    override var ads: MutableList<Ad> = mutableListOf()
) : com.bitmovin.player.model.advertising.AdBreak
