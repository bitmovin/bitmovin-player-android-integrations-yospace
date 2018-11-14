package com.bitmovin.player.integrations.bitmovinyospacemodule.config;

public class YospaceConfiguration {
    private MoatConfiguration moatConfiguration;
    private String userAgent;
    private int readTimeout;
    private int connectTimeout;
    private int requestTimeout;
    private boolean debug;

    public YospaceConfiguration(){
    }

    public YospaceConfiguration(String userAgent, MoatConfiguration moatConfiguration, int readTimeout, int connectTimeout, int requestTimeout, boolean debug) {
        this.moatConfiguration = moatConfiguration;
        this.userAgent = userAgent;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.debug = debug;
    }

    public MoatConfiguration getMoatConfiguration() {
        return moatConfiguration;
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
