package com.bitmovin.player.integration.yospace

data class CompanionAd(
    val id: String?,
    val width: Int?,
    val height: Int?,
    val source: String?
) {
    override fun toString() = "id=$id, width=$width, height=$height, sourceUrl=$source"
}
