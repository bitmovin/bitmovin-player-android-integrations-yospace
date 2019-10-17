package com.bitmovin.player.integration.yospace.config

import com.bitmovin.player.integration.yospace.YospaceAssetType

data class YospaceSourceConfiguration(
        val assetType: YospaceAssetType,
        val retryExcludingYospace: Boolean = false
)