package com.bitmovin.player.integration.yospace;

import com.bitmovin.player.model.advertising.Ad;

import java.util.ArrayList;
import java.util.List;

public class AdBreak implements com.bitmovin.player.model.advertising.AdBreak {
    private String id;
    private double relativeStart;
    private double duration;
    private double absoluteStart;
    private double absoluteEnd;
    private double replaceContentDuration;
    private List<Ad> ads = new ArrayList<>();

    public AdBreak(String id, double relativeStart, double duration, double absoluteStart, double absoluteEnd, double replaceContentDuration) {
        this.id = id;
        this.relativeStart = relativeStart;
        this.duration = duration;
        this.absoluteStart = absoluteStart;
        this.absoluteEnd = absoluteEnd;
        this.replaceContentDuration = replaceContentDuration;
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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<Ad> getAds() {
        return ads;
    }

    @Override
    public double getScheduleTime() {
        return absoluteStart;
    }

    @Override
    public Double getReplaceContentDuration() {
        return replaceContentDuration;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void appendAd(Ad ad) {
        ads.add(ad);
    }
}
