package com.bitmovin.player.integration.yospace.config;

public class YospaceConfiguration {
    private String userAgent;
    private int readTimeout;
    private int connectTimeout;
    private int requestTimeout;
    private boolean debug;

    public YospaceConfiguration() {
    }

    public YospaceConfiguration(String userAgent, int readTimeout, int connectTimeout, int requestTimeout, boolean debug) {
        this.userAgent = userAgent;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
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

    public boolean isDebug() {
        return debug;
    }
}
