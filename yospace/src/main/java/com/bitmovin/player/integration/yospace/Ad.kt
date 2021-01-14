package com.bitmovin.player.integration.yospace

import com.bitmovin.player.model.advertising.AdData
import com.yospace.android.hls.analytic.advert.AdSystem
import com.yospace.android.hls.analytic.advert.AdvertWrapper
import com.yospace.android.xml.XmlNode
import com.bitmovin.player.model.advertising.Ad as BitmovinAd

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
) : BitmovinAd {
    override fun toString() =
        "id=$id, " +
        "creativeId=$creativeId, " +
        "sequence=$sequence, " +
        "absoluteStart=$absoluteStart, " +
        "relativeStart=$relativeStart, " +
        "duration=$duration, " +
        "absoluteEnd=$absoluteEnd, " +
        "system=${system?.adSystemType}, " +
        "title=$title, " +
        "advertiser=$advertiser, " +
        "hasInteractiveUnit=$hasInteractiveUnit, " +
        "isFiller=$isFiller, " +
        "isLinear=$isLinear, " +
        "mediaFileUrl=$mediaFileUrl"
}
