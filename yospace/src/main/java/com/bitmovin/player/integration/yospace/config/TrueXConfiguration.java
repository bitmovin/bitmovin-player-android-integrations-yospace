package com.bitmovin.player.integration.yospace.config;

import android.view.ViewGroup;

public class TruexConfiguration {
    private ViewGroup viewGroup;
    private String userId;
    private String vastConfigUrl;

    public TruexConfiguration(ViewGroup viewGroup) {
        this.viewGroup = viewGroup;
    }

    public TruexConfiguration(ViewGroup viewGroup, String userId, String vastConfigUrl) {
        this.viewGroup = viewGroup;
        this.userId = userId;
        this.vastConfigUrl = vastConfigUrl;
    }

    public ViewGroup getViewGroup() {
        return viewGroup;
    }

    public String getUserId() {
        return userId;
    }

    public String getVastConfigUrl() {
        return vastConfigUrl;
    }

}
