package com.bitmovin.player.integration.yospacesample

import com.bitmovin.player.api.source.SourceType
import com.bitmovin.player.integration.yospace.config.TruexConfig
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfig

data class Stream(
    val title: String,
    val contentUrl: String,
    val sourceType: SourceType = SourceType.Hls,
    val drmUrl: String? = null,
    val yospaceSourceConfig: YospaceSourceConfig,
    val truexConfig: TruexConfig? = null
)
