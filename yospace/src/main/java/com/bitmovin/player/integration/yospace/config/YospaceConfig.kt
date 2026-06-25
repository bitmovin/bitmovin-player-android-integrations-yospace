package com.bitmovin.player.integration.yospace.config

import com.bitmovin.player.integration.yospace.YospaceLiveInitializationType

data class YospaceConfig(
    val userAgent: String? = null,
    val readTimeout: Int = 25_000,
    val connectTimeout: Int = 25_000,
    val requestTimeout: Int = 25_000,
    val liveInitializationType: YospaceLiveInitializationType = YospaceLiveInitializationType.DIRECT,
    val isDebug: Boolean = false,
    val filterMetadataType: MetadataType? = MetadataType.EMSG,
    val yospaceDebugMode: YospaceDebugMode = YospaceDebugMode.NONE
)

public enum class MetadataType{
    ID3, EMSG
}
