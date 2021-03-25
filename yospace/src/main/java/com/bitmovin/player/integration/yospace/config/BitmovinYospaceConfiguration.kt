package com.bitmovin.player.integration.yospace.config

import com.bitmovin.player.config.PlayerConfiguration

data class BitmovinYospaceConfiguration(
    val playerConfiguration: PlayerConfiguration,
    val yospaceConfiguration: YospaceConfiguration,
    val debug: Boolean = false
)
