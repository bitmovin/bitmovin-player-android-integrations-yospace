package com.bitmovin.player.integration.yospace.config;

import com.bitmovin.player.integration.yospace.YospaceAssetType;

public class YospaceSourceConfiguration {
    private YospaceAssetType assetType;

    public YospaceSourceConfiguration(YospaceAssetType assetType) {
        this.assetType = assetType;
    }

    public YospaceAssetType getAssetType() {
        return assetType;
    }
}