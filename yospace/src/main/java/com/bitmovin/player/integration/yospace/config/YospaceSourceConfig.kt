package com.bitmovin.player.integration.yospace.config

import com.bitmovin.player.integration.yospace.YospaceAssetType

data class YospaceSourceConfig(
    val assetType: YospaceAssetType,
    val retryExcludingYospace: Boolean = false
)
