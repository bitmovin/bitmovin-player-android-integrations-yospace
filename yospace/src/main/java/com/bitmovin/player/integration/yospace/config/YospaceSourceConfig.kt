package com.bitmovin.player.integration.yospace.config

data class YospaceSourceConfig(
    val assetType: YospaceAssetType,
    val retryExcludingYospace: Boolean = false
)
