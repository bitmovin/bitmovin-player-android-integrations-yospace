package com.bitmovin.player.integrations.bitmovinyospacemodule;

import com.bitmovin.player.config.media.SourceConfiguration;

public class YoSpaceSourceConfiguration extends SourceConfiguration {
    private YoSpaceAssetType yoSpaceAssetType = YoSpaceAssetType.LINEAR;

    public YoSpaceAssetType getYoSpaceAssetType() {
        return yoSpaceAssetType;
    }

    public void setYoSpaceAssetType(YoSpaceAssetType yoSpaceAssetType) {
        this.yoSpaceAssetType = yoSpaceAssetType;
    }
}
