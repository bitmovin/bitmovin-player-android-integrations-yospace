package com.bitmovin.player.integration.yospace

enum class CompanionAdType {
    HTML, // dynamic, web view
    STATIC // static, image view
}

data class CompanionAdResource(
    val source: String?,
    val type: CompanionAdType
)

data class CompanionAd(
    val id: String?,
    val adSlotId: String?,
    val width: Int,
    val height: Int,
    val clickThroughUrl: String?,
    val resource: CompanionAdResource?
) {
    override fun toString() = "id=$id, width=$width, height=$height, type=${resource?.type?.name} sourceUrl=${resource?.source}"
}
