package com.bitmovin.player.integrations.bitmovinyospacemodule.config;

import android.app.Application;
import android.view.View;

public class MoatConfiguration {
    private String partnerCode;
    private Application application;
    private View playerView;
    private boolean disableAdIdCollection = false;
    private boolean autoTrackGMAInterstitials = false;
    private boolean disableLocationServices = false;
    private boolean loggingEnabled = false;

    public MoatConfiguration(String partnerCode, Application application, View playerView) {
        this.partnerCode = partnerCode;
        this.application = application;
        this.playerView = playerView;
    }

    public MoatConfiguration(String partnerCode, Application application, View playerView, boolean disableAdIdCollection, boolean autoTrackGMAInterstitials, boolean disableLocationServices, boolean loggingEnabled) {
        this.partnerCode = partnerCode;
        this.application = application;
        this.playerView = playerView;
        this.disableAdIdCollection = disableAdIdCollection;
        this.autoTrackGMAInterstitials = autoTrackGMAInterstitials;
        this.disableLocationServices = disableLocationServices;
        this.loggingEnabled = loggingEnabled;
    }

    public String getPartnerCode() {
        return partnerCode;
    }

    public Application getApplication() {
        return application;
    }

    public View getPlayerView() {
        return playerView;
    }

    public boolean isDisableAdIdCollection() {
        return disableAdIdCollection;
    }

    public boolean isAutoTrackGMAInterstitials() {
        return autoTrackGMAInterstitials;
    }

    public boolean isDisableLocationServices() {
        return disableLocationServices;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }
}
