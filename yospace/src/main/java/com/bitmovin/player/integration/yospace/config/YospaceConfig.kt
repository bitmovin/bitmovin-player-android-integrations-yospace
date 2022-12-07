package com.bitmovin.player.integration.yospace.config

import com.bitmovin.player.integration.yospace.YospaceLiveInitialisationType

data class YospaceConfig(
    val userAgent: String? = null,
    val readTimeout: Int = 25_000,
    val connectTimeout: Int = 25_000,
    val requestTimeout: Int = 25_000,
    val liveInitialisationType: YospaceLiveInitialisationType = YospaceLiveInitialisationType.DIRECT,
    val isDebug: Boolean = true,
    val filterMetadataType: MetadataType? = MetadataType.ID3
)

public enum class MetadataType{
    ID3, EMSG
}