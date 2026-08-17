package com.bitmovin.player.integration.yospace.analytics

import com.bitmovin.player.integration.yospace.advertising.AdBreakPosition

/**
 * Ad break details reported to Bitmovin Analytics at the start of a Yospace ad break.
 */
internal data class SsaiAdBreakInfo(
    val position: AdBreakPosition,
    /** Number of paid ads in the break, or `null` if not known yet. */
    val paidAds: Int?,
    /** Number of slates (Yospace fillers) in the break, or `null` if not known yet. */
    val slates: Int?
)

/**
 * Ad details reported to Bitmovin Analytics at the start of a Yospace advert.
 */
internal data class SsaiAdInfo(
    val adId: String?,
    val adSystem: String?,
    val isSlate: Boolean,
    val durationMs: Long
)

internal enum class SsaiQuartile { FIRST, MIDPOINT, THIRD, COMPLETED }

/**
 * Seam over the Bitmovin Analytics SSAI API, so [YospaceSsaiTracker] can be tested without a player.
 */
internal interface SsaiAdTracker {
    fun adBreakStart(adBreak: SsaiAdBreakInfo)
    fun adStart(ad: SsaiAdInfo)
    fun adQuartileFinished(quartile: SsaiQuartile)
    fun adBreakEnd()
}
