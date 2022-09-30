package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.advertising.Ad
import com.bitmovin.player.api.advertising.vast.AdSystem
import com.yospace.admanagement.AdvertWrapper
import com.yospace.admanagement.XmlNode

data class Ad(
    override var id: String?,
    val creativeId: String?,
    val sequence: Int,
    val absoluteStart: Double,
    val relativeStart: Double,
    val duration: Double,
    val absoluteEnd: Double,
    val system: AdSystem?,
    val title: String?,
    val advertiser: String?,
    val hasInteractiveUnit: Boolean,
    val isFiller: Boolean,
    val lineage: AdvertWrapper?,
    val extensions: List<XmlNode>,
    override val isLinear: Boolean,
    override var clickThroughUrl: String? = null,
    override var data: AdData? = null,
    override var width: Int = -1,
    override var height: Int = -1,
    override var mediaFileUrl: String? = null
) : Ad {
    override fun toString() =
        "id=$id, " +
        "creativeId=$creativeId, " +
        "sequence=$sequence, " +
        "absoluteStart=$absoluteStart, " +
        "relativeStart=$relativeStart, " +
        "duration=$duration, " +
        "absoluteEnd=$absoluteEnd, " +
        "system=${system?.name}, " +
        "title=$title, " +
        "advertiser=$advertiser, " +
        "hasInteractiveUnit=$hasInteractiveUnit, " +
        "isFiller=$isFiller, " +
        "isLinear=$isLinear, " +
        "mediaFileUrl=$mediaFileUrl"
}
