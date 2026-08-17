package com.bitmovin.player.integration.yospace.config

import com.bitmovin.analytics.api.SourceMetadata

data class YospaceSourceConfig(
    val assetType: YospaceAssetType,
    val retryExcludingYospace: Boolean = false,
    /**
     * Bitmovin Analytics metadata for the source. When [SourceMetadata.isLive] is not set, it is
     * derived from [assetType].
     */
    val sourceMetadata: SourceMetadata? = null
)
