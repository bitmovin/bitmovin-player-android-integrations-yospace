package com.bitmovin.player.integrations.bitmovinyospacemodule.config;

import android.app.Application;
import android.view.View;

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