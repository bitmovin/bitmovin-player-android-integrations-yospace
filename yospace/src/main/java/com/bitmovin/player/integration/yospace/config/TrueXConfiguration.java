package com.bitmovin.player.integration.yospace.config;

import android.view.ViewGroup;

public class TrueXConfiguration {
    private ViewGroup viewGroup;

    public TrueXConfiguration(ViewGroup viewGroup) {
        this.viewGroup = viewGroup;
    }

    public ViewGroup getViewGroup() {
        return viewGroup;
    }
}
