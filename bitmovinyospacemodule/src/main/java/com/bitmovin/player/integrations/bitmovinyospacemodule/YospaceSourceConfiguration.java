package com.bitmovin.player.integrations.bitmovinyospacemodule;

public class YospaceSourceConfiguration {
    private YospaceAssetType assetType;
    public String userAgent = "BitmovinPlayer";
    public int readTimeout = 5000;
    public int connectTimeout = 5000;
    public int requestTimeout = 5000;
    public boolean debug = true;

    public YospaceSourceConfiguration(YospaceAssetType assetType) {
        this.assetType = assetType;
    }

    public YospaceAssetType getAssetType() {
        return assetType;
    }
}