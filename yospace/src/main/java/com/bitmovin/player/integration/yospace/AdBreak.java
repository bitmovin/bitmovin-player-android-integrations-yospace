package com.bitmovin.player.integration.yospace;

import java.util.ArrayList;
import java.util.List;

public class AdBreak {
    private double relativeStart = 0;
    private double duration = 0;
    private double absoluteStart = 0;
    private double absoluteEnd = 0;
    private String identifier = "unknown";
    private List<Ad> ads = new ArrayList<>();

    public AdBreak(String identifier, double relativeStart, double duration, double absoluteStart, double absoluteEnd) {
        this.relativeStart = relativeStart / 1000;
        this.duration = duration / 1000;
        this.absoluteStart = absoluteStart / 1000;
        this.absoluteEnd = absoluteEnd / 1000;
        this.identifier = identifier;
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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<Ad> getAds() {
        return ads;
    }

    public void appendAd(Ad ad) {
        ads.add(ad);
    }
}
