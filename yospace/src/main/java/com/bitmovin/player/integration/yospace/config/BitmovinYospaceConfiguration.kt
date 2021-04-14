package com.bitmovin.player.integration.yospace.config

import com.bitmovin.analytics.BitmovinAnalyticsConfig
import com.bitmovin.player.config.PlayerConfiguration

data class BitmovinYospaceConfiguration(
    val playerConfiguration: PlayerConfiguration,
    val yospaceConfiguration: YospaceConfiguration,
    val analyticsConfiguration: BitmovinAnalyticsConfig? = null,
    val debug: Boolean = false
)
