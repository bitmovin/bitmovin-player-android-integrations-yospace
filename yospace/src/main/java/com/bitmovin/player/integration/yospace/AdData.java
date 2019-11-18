package com.bitmovin.player.integration.yospace;

public class AdData implements com.bitmovin.player.model.advertising.AdData {
    private int bitrate;
    private int minBitrate;
    private int maxBitrate;
    private String mimeType;

    public AdData(int bitrate, int minBitrate, int maxBitrate, String mimeType) {
        this.bitrate = bitrate;
        this.minBitrate = minBitrate;
        this.maxBitrate = maxBitrate;
        this.mimeType = mimeType;
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
