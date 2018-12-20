package com.bitmovin.player.integrations.bitmovinyospacemodule.config;

import com.bitmovin.player.integrations.bitmovinyospacemodule.YospaceAssetType;

public class YospaceSourceConfiguration {
    private YospaceAssetType assetType;

    public YospaceSourceConfiguration(YospaceAssetType assetType) {
        this.assetType = assetType;
    }

    public YospaceAssetType getAssetType() {
        return assetType;
    }
}