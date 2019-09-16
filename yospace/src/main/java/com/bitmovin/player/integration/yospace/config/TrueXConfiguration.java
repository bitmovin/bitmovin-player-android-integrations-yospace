package com.bitmovin.player.integration.yospace.config;

import android.view.ViewGroup;

public class TrueXConfiguration {
    private ViewGroup viewGroup;
    private String userId;
    private String vastConfigUrl;

    public TrueXConfiguration(ViewGroup viewGroup) {
        this.viewGroup = viewGroup;
    }

    public ViewGroup getViewGroup() {
        return viewGroup;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVastConfigUrl() {
        return vastConfigUrl;
    }

    public void setVastConfigUrl(String vastConfigUrl) {
        this.vastConfigUrl = vastConfigUrl;
    }
}
