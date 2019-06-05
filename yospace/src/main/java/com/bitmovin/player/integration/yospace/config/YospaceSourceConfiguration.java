package com.bitmovin.player.integration.yospace.config;

import com.bitmovin.player.integration.yospace.YospaceAssetType;

public class YospaceSourceConfiguration {
    private YospaceAssetType assetType;
    private boolean retryExcludingYospace = false;

    public YospaceSourceConfiguration(YospaceAssetType assetType) {
        this.assetType = assetType;
    }

    public YospaceSourceConfiguration(YospaceAssetType assetType, boolean retryExcludingYospace) {
        this.assetType = assetType;
        this.retryExcludingYospace = retryExcludingYospace;
    }


    public YospaceAssetType getAssetType() {
        return assetType;
    }

    public boolean isRetryExcludingYospace() {
        return retryExcludingYospace;
    }
}