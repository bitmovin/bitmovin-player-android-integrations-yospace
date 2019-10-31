package com.bitmovin.player.integration.yospace.config;

import android.view.ViewGroup;

import com.bitmovin.player.integration.yospace.YospaceInitialisationType;

public class YospaceConfigurationBuilder {
    private String userAgent;
    private int readTimeout;
    private int connectTimeout;
    private int requestTimeout;
    private YospaceInitialisationType initialisationType = YospaceInitialisationType.PROXY;
    private boolean debug = false;

    public YospaceConfigurationBuilder() {
    }

    public YospaceConfigurationBuilder setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public YospaceConfigurationBuilder setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public YospaceConfigurationBuilder setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public YospaceConfigurationBuilder setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
        return this;
    }

    public YospaceConfigurationBuilder setInitialisationType(YospaceInitialisationType initialisationType) {
        this.initialisationType = initialisationType;
        return this;
    }

    public YospaceConfigurationBuilder setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public YospaceConfiguration build() {
        return new YospaceConfiguration(userAgent, readTimeout, connectTimeout, requestTimeout, initialisationType, debug);
    }
}
