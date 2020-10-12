package com.bitmovin.player.integration.yospacesample

import com.bitmovin.player.integration.yospace.config.TruexConfiguration
import com.bitmovin.player.integration.yospace.config.YospaceSourceConfiguration

data class Stream(
    val title: String,
    val contentUrl: String,
    val drmUrl: String? = null,
    val yospaceSourceConfig: YospaceSourceConfiguration,
    val truexConfig: TruexConfiguration? = null
)
