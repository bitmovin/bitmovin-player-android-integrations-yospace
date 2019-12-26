package com.bitmovin.player.integration.yospace;

public class AdData implements com.bitmovin.player.model.advertising.AdData {
    private int bitrate;
    private int minBitrate;
    private int maxBitrate;
    private String mimeType;

    public AdData(String mimeType) {
        this.mimeType = mimeType;

        // Properties we do not currently support
        this.bitrate = -1;
        this.minBitrate = -1;
        this.maxBitrate = -1;
    }

    @Override
    public Integer getBitrate() {
        return bitrate;
    }

    @Override
    public Integer getMaxBitrate() {
        return maxBitrate;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public Integer getMinBitrate() {
        return minBitrate;
    }
}
