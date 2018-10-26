package com.bitmovin.player.integrations.bitmovinyospacemodule;

import com.bitmovin.player.config.media.SourceConfiguration;

public class YoSpaceSourceConfiguration {
    private SourceConfiguration sourceConfiguration;
    private YoSpaceAssetType assetType;

    public YoSpaceSourceConfiguration(SourceConfiguration sourceConfiguration, YoSpaceAssetType assetType){
        this.sourceConfiguration = sourceConfiguration;
        this.assetType = assetType;
    }

    public YoSpaceAssetType getAssetType() {
        return assetType;
    }

    public SourceConfiguration getSourceConfiguration() {
        return sourceConfiguration;
    }
}
