package com.bitmovin.player.integration.yospace.events

import com.bitmovin.player.api.advertising.AdQuartile as PlayerAdQuartile
import com.bitmovin.player.api.advertising.AdSourceType
import com.bitmovin.player.api.deficiency.DeficiencyData
import com.bitmovin.player.api.deficiency.ErrorEvent
import com.bitmovin.player.api.deficiency.WarningEvent
import com.bitmovin.player.integration.yospace.advertising.Ad
import com.bitmovin.player.integration.yospace.advertising.AdBreak
import com.bitmovin.player.integration.yospace.advertising.CompanionAd
import com.bitmovin.player.integration.yospace.deficiency.YospaceErrorCode
import com.bitmovin.player.integration.yospace.deficiency.YospaceWarningCode

/**
 * Yospace integration-owned ad lifecycle events.
 *
 * These are emitted by [com.bitmovin.player.integration.yospace.BitmovinYospacePlayer] and can be
 * observed via the integration `on`/`next`/`off`
 * extensions. They intentionally do not extend the Bitmovin Player
 * [com.bitmovin.player.api.event.Event] hierarchy, so the non-serializable payloads never reach the
 * Player UI.
 */
sealed class YospacePlayerEvent {
    /**
     * Emitted when a Yospace integration error occurred.
     */
    data class Error(
        override val code: YospaceErrorCode,
        override val message: String,
        override val data: Any? = null,
        override val deficiencyData: DeficiencyData? = null
    ) : ErrorEvent, YospacePlayerEvent()

    /**
     * Emitted when a Yospace integration warning occurred.
     */
    data class Warning(
        override val code: YospaceWarningCode,
        override val message: String,
        override val deficiencyData: DeficiencyData? = null
    ) : WarningEvent, YospacePlayerEvent()

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
        val position: String? = null,
        val skipOffset: Double = 0.0,
    ) : YospacePlayerEvent()

    /**
     * Emitted when a Yospace ad was clicked.
     */
    data class AdClicked(val clickThroughUrl: String?) : YospacePlayerEvent()

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

    /**
     * Emitted when TrueX marks the session as ad-free.
     */
    object TruexAdFree : YospacePlayerEvent()
}
