package com.bitmovin.player.integration.yospace.analytics

import android.os.Build
import com.bitmovin.analytics.api.ssai.SsaiAdBreakMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdMetadata
import com.bitmovin.analytics.api.ssai.SsaiAdPosition
import com.bitmovin.analytics.api.ssai.SsaiAdQuartile
import com.bitmovin.player.api.analytics.AnalyticsApi
import com.bitmovin.player.integration.yospace.advertising.AdBreakPosition

/**
 * Reports Yospace ads through the SSAI API of the Bitmovin Analytics collector.
 *
 * [analytics] is resolved per call because the analytics license is resolved asynchronously.
 */
internal class AnalyticsSsaiAdTracker(private val analytics: () -> AnalyticsApi?) : SsaiAdTracker {

    override fun adBreakStart(adBreak: SsaiAdBreakInfo) {
        analytics()?.ssai?.adBreakStart(
            SsaiAdBreakMetadata(
                adPosition = adBreak.position.toSsaiAdPosition(),
                expectedPaidAds = adBreak.paidAds,
                expectedSlates = adBreak.slates
            )
        )
    }

    override fun adStart(ad: SsaiAdInfo) {
        analytics()?.ssai?.adStart(ad.toSsaiAdMetadata())
    }

    override fun adQuartileFinished(quartile: SsaiQuartile) {
        analytics()?.ssai?.adQuartileFinished(quartile.toSsaiAdQuartile())
    }

    override fun adBreakEnd() {
        analytics()?.ssai?.adBreakEnd()
    }
}

private fun AdBreakPosition.toSsaiAdPosition(): SsaiAdPosition? = when (this) {
    AdBreakPosition.PREROLL -> SsaiAdPosition.PREROLL
    AdBreakPosition.MIDROLL -> SsaiAdPosition.MIDROLL
    AdBreakPosition.POSTROLL -> SsaiAdPosition.POSTROLL
    AdBreakPosition.UNKNOWN -> null
}

private fun SsaiQuartile.toSsaiAdQuartile(): SsaiAdQuartile = when (this) {
    SsaiQuartile.FIRST -> SsaiAdQuartile.FIRST
    SsaiQuartile.MIDPOINT -> SsaiAdQuartile.MIDPOINT
    SsaiQuartile.THIRD -> SsaiAdQuartile.THIRD
    SsaiQuartile.COMPLETED -> SsaiAdQuartile.COMPLETED
}

private fun SsaiAdInfo.toSsaiAdMetadata(): SsaiAdMetadata {
    val builder = SsaiAdMetadata.Builder()
        .setAdId(adId)
        .setAdSystem(adSystem)
        .setIsSlate(isSlate)

    // SsaiAdMetadata.duration is a java.time.Duration, which is unavailable below API 26.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && durationMs > 0) {
        SsaiAdDuration.applyTo(builder, durationMs)
    }

    return builder.build()
}

/**
 * Isolates the `java.time.Duration` reference so the class is only loaded on API 26 and above.
 */
private object SsaiAdDuration {
    fun applyTo(builder: SsaiAdMetadata.Builder, durationMs: Long) {
        builder.setDuration(java.time.Duration.ofMillis(durationMs))
    }
}
