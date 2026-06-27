package com.bitmovin.player.integration.yospace

import com.bitmovin.player.api.advertising.AdQuartile as PlayerAdQuartile
import com.bitmovin.player.api.advertising.AdSourceType

/**
 * Yospace integration-owned ad lifecycle events.
 *
 * These are emitted by [BitmovinYospacePlayer] through its [BitmovinYospacePlayer.yospace] namespace
 * (`player.yospace.on<...>`) and carry integration-owned payloads such as [Ad] and [AdBreak]. They
 * intentionally do not extend the
 * Bitmovin Player [com.bitmovin.player.api.event.Event] hierarchy, so the non-serializable payloads
 * never reach the Player UI.
 */
sealed class YospacePlayerEvent {
    /**
     * Emitted when a Yospace ad break starts.
     */
    data class AdBreakStarted(val adBreak: AdBreak?) : YospacePlayerEvent()

    /**
     * Emitted when a Yospace ad break finishes.
     */
    data class AdBreakFinished(val adBreak: AdBreak?) : YospacePlayerEvent()

    /**
     * Emitted when a Yospace ad starts.
     */
    data class AdStarted(
        val ad: Ad?,
        val companionAds: List<CompanionAd> = emptyList(),
        val clientType: AdSourceType = AdSourceType.Unknown,
        val clickThroughUrl: String = "",
        val indexInQueue: Int = 0,
        val duration: Double = 0.0,
        val timeOffset: Double = 0.0,
        val skipOffset: Double = 0.0,
    ) : YospacePlayerEvent()

    /**
     * Emitted when a Yospace ad finishes.
     */
    data class AdFinished(val ad: Ad?) : YospacePlayerEvent()

    /**
     * Emitted when a Yospace ad is skipped.
     */
    data class AdSkipped(val ad: Ad?) : YospacePlayerEvent()

    /**
     * Emitted when a Yospace ad quartile is reached.
     */
    data class AdQuartile(val quartile: PlayerAdQuartile) : YospacePlayerEvent()
}
