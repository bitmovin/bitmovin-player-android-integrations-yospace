package com.bitmovin.player.integration.yospace.config

import kotlin.math.roundToInt

data class YospaceConfig(
    val userAgent: String? = null,
    @Deprecated(
        message = "Use requestTimeoutSeconds instead, which is expressed in seconds like all other " +
            "time-based configuration. Divide the milliseconds passed here by 1000."
    )
    val requestTimeout: Int = DEFAULT_REQUEST_TIMEOUT_MILLISECONDS,
    val liveInitializationType: YospaceLiveInitializationType = YospaceLiveInitializationType.DIRECT,
    val isDebug: Boolean = false,
    val filterMetadataType: MetadataType? = MetadataType.EMSG,
    val yospaceDebugMode: YospaceDebugMode = YospaceDebugMode.NONE,
    /**
     * How far into a VOD advert, in seconds, playback may start before Bitmovin Analytics reports
     * the ad as joined mid-ad, meaning its quartiles do not reflect what the viewer saw.
     */
    val midAdvertJoinTolerance: Double = 1.0,
    /**
     * Timeout for requests to the Yospace ad management service, in seconds. Takes precedence over
     * the deprecated [requestTimeout] when set.
     */
    val requestTimeoutSeconds: Double? = null
) {
    /**
     * The effective request timeout in milliseconds, as expected by the Yospace SDK.
     */
    @Suppress("DEPRECATION")
    internal val effectiveRequestTimeoutMilliseconds: Int
        get() = requestTimeoutSeconds?.let { (it * 1000).roundToInt() } ?: requestTimeout
}

private const val DEFAULT_REQUEST_TIMEOUT_MILLISECONDS = 25_000

public enum class MetadataType{
    ID3, EMSG
}
