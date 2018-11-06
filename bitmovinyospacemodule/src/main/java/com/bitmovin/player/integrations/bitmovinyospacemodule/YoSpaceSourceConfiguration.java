package com.bitmovin.player.integrations.bitmovinyospacemodule;

public class YoSpaceSourceConfiguration {
    private YospaceAssetType assetType;

    public YoSpaceSourceConfiguration(YospaceAssetType assetType) {
        this.assetType = assetType;
    }

    public YospaceAssetType getAssetType() {
        return assetType;
    }
}