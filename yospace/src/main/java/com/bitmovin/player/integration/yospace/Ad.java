package com.bitmovin.player.integration.yospace;

import com.bitmovin.player.model.advertising.AdData;

public class Ad implements com.bitmovin.player.model.advertising.Ad {
    private String id;
    private double relativeStart;
    private double duration;
    private double absoluteStart;
    private double absoluteEnd;
    private int sequence;
    private int width;
    private int height;
    private String clickThroughUrl;
    private String mediaFileUrl;
    private boolean hasInteractiveUnit;
    private boolean isTruex;
    private boolean isLinear;
    private AdData adData;

    public Ad(String id, double relativeStart, double duration, double absoluteStart, double absoluteEnd, int sequence, String clickThroughUrl, String mediaFileUrl, boolean isLinear, boolean hasInteractiveUnit, boolean isTruex, int width, int height, AdData adData) {
        this.id = id;
        this.relativeStart = relativeStart;
        this.duration = duration;
        this.absoluteStart = absoluteStart;
        this.absoluteEnd = absoluteEnd;
        this.sequence = sequence;
        this.clickThroughUrl = clickThroughUrl;
        this.mediaFileUrl = mediaFileUrl;
        this.isLinear = isLinear;
        this.hasInteractiveUnit = hasInteractiveUnit;
        this.isTruex = isTruex;
        this.width = width;
        this.height = height;
        this.adData = adData;
    }

    public double getRelativeStart() {
        return relativeStart;
    }

    public double getDuration() {
        return duration;
    }

    public double getAbsoluteStart() {
        return absoluteStart;
    }

    public double getAbsoluteEnd() {
        return absoluteEnd;
    }

    public int getSequence() {
        return sequence;
    }

    public String getClickThroughUrl() {
        return clickThroughUrl;
    }

    public boolean isHasInteractiveUnit() {
        return hasInteractiveUnit;
    }

    public boolean isTruex() {
        return isTruex;
    }

    @Override
    public void setClickThroughUrl(String clickThroughUrl) {
        this.clickThroughUrl = clickThroughUrl;
    }

    @Override
    public AdData getData() {
        return adData;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isLinear() {
        return isLinear;
    }

    @Override
    public String getMediaFileUrl() {
        return mediaFileUrl;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setData(AdData adData) {
        this.adData = adData;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }


    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setMediaFileUrl(String mediaFileUrl) {
        this.mediaFileUrl = mediaFileUrl;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }
}
