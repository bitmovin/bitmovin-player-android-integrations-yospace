package com.bitmovin.player.integrations.bitmovinyospacemodule;

public class YospaceSourceConfiguration {
    private YospaceAssetType assetType;

    public YospaceSourceConfiguration(YospaceAssetType assetType) {
        this.assetType = assetType;
    }

    public YospaceAssetType getAssetType() {
        return assetType;
    }
}