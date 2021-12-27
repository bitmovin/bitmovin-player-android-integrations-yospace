package com.bitmovin.player.integration.yospace.config

import com.bitmovin.player.integration.yospace.YospaceLiveInitialisationType

data class YospaceConfiguration(
    val userAgent: String? = null,
    val readTimeout: Int = 25_000,
    val connectTimeout: Int = 25_000,
    val requestTimeout: Int = 25_000,
    val liveInitialisationType: YospaceLiveInitialisationType = YospaceLiveInitialisationType.DIRECT,
    val isDebug: Boolean = false,
    val filterMetadataType: MetadataType? = MetadataType.EMSG
)

public enum class MetadataType{
    ID3, EMSG
}