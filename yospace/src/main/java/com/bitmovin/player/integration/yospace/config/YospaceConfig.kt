package com.bitmovin.player.integration.yospace.config

data class YospaceConfig(
    val userAgent: String? = null,
    val requestTimeout: Int = 25_000,
    val liveInitializationType: YospaceLiveInitializationType = YospaceLiveInitializationType.DIRECT,
    val isDebug: Boolean = false,
    val filterMetadataType: MetadataType? = MetadataType.EMSG,
    val yospaceDebugMode: YospaceDebugMode = YospaceDebugMode.NONE,
    /**
     * How far into a VOD advert playback may start before Bitmovin Analytics reports the ad as
     * joined mid-ad, meaning its quartiles do not reflect what the viewer saw. Milliseconds.
     */
    val midAdvertJoinToleranceMs: Long = 1_000
)

public enum class MetadataType{
    ID3, EMSG
}
