package com.bitmovin.player.integration.yospace.config;

import com.bitmovin.player.integration.yospace.YospaceLiveInitialisationType;

public class YospaceConfiguration {
    private String userAgent;
    private int readTimeout;
    private int connectTimeout;
    private int requestTimeout;
    private YospaceLiveInitialisationType liveInitialisationType;
    private boolean debug;

    public YospaceConfiguration() {
    }

    public YospaceConfiguration(String userAgent, int readTimeout, int connectTimeout, int requestTimeout, YospaceLiveInitialisationType liveInitialisationType, boolean debug) {
        this.userAgent = userAgent;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.liveInitialisationType = liveInitialisationType;
        this.debug = debug;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public YospaceLiveInitialisationType getLiveInitialisationType() {
        return liveInitialisationType;
    }

    public boolean isDebug() {
        return debug;
    }

}
