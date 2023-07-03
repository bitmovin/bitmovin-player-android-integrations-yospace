package com.bitmovin.player.integration.yospacesample

import com.bitmovin.player.integration.yospace.config.TruexConfig
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfig

data class Stream(
    val title: String,
    val contentUrl: String,
    val drmUrl: String? = null,
    val yospaceSourceConfig: YospaceSourceConfig,
    val truexConfig: TruexConfig? = null
)
