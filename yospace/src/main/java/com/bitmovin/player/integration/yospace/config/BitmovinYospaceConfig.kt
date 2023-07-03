package com.bitmovin.player.integration.yospace.config

import com.bitmovin.player.api.PlayerConfig

data class BitmovinYospaceConfig(
    val playerConfiguration: PlayerConfig,
    val yospaceConfiguration: YospaceConfig,
    val debug: Boolean = false
)
