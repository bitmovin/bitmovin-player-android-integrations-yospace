package com.bitmovin.player.integrations.bitmovinyospacemodule.config;

public class YospaceConfigurationBuilder {
    private MoatConfiguration moatConfiguration;
    private String userAgent;
    private int readTimeout;
    private int connectTimeout;
    private int requestTimeout;
    private boolean debug = false;


    public YospaceConfigurationBuilder() {
    }

    public YospaceConfigurationBuilder setMoatConfiguration(MoatConfiguration moatConfiguration) {
        this.moatConfiguration = moatConfiguration;
        return this;
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

    public YospaceConfigurationBuilder setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public YospaceConfiguration build() {
        return new YospaceConfiguration(userAgent, moatConfiguration, readTimeout, connectTimeout, requestTimeout, debug);
    }
}
