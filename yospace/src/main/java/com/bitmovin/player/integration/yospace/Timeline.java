package com.bitmovin.player.integration.yospace;

import com.yospace.android.hls.analytic.advert.AdBreak;
import com.yospace.android.hls.analytic.advert.Advert;

import java.util.ArrayList;
import java.util.List;

public class Timeline {

    private List<TimelineEntry> entries = new ArrayList<TimelineEntry>();
    private List<AdBreak> adBreaks;

    public Timeline(List<AdBreak> adBreaks) {
        double count = 0;
        this.adBreaks = adBreaks;

        for (AdBreak adbreak : adBreaks) {
            TimelineEntry entry = new TimelineEntry();
            entry.setAbsoluteStart(adbreak.getStartMillis());
            entry.setAbsoluteEnd(adbreak.getStartMillis() + adbreak.getDuration());
            entry.setDuration(adbreak.getDuration());
            entry.setRelativeStart(adbreak.getStartMillis() - count);
            count += adbreak.getDuration();
            entries.add(entry);
        }
    }

    @Override
    public String toString() {
        String str = entries.size() + " ad breaks ";
        for (TimelineEntry entry : entries) {
            str += " [" + entry.getRelativeStart() + " - " + entry.getDuration() + "] ";
        }
        return str;
    }

    public AdBreak currentAdBreak(double time) {
        time = time * 1000;
        AdBreak currentAdBreak = null;

        for (AdBreak adBreak : adBreaks) {
            if (adBreak.getStartMillis() < time && (adBreak.getStartMillis() + adBreak.getDuration()) > time) {
                currentAdBreak = adBreak;
                break;
            }
        }

        return currentAdBreak;
    }

    public Advert currentAd(double time) {
        time = time * 1000;
        AdBreak currentAdBreak = currentAdBreak(time);
        Advert currentAd = null;

        if (currentAdBreak == null) {
            return null;
        }

        List<Advert> ads = currentAdBreak.getAdverts();

        for (Advert ad : ads) {
            if (ad.getStartMillis() < time && (ad.getStartMillis() + ad.getDuration()) > time) {
                currentAd = ad;
                break;
            }
        }

        return currentAd;
    }

    public double absoluteToRelative(double time) {
        time = time * 1000;
        double passedAdBreakDurations = 0;
        AdBreak currentAdBreak = currentAdBreak(time);

        for (TimelineEntry entry : entries) {
            if (entry.getAbsoluteEnd() < time) {
                passedAdBreakDurations += entry.getDuration();
            }
        }

        if (currentAdBreak != null) {
            return (currentAdBreak.getStartMillis() - passedAdBreakDurations) / 1000;
        }

        return (time - passedAdBreakDurations) / 1000;
    }

    public double relativeToAbsolute(double time) {
        time = time * 1000;
        double passedAdBreakDurations = 0;

        for (TimelineEntry entry : entries) {
            if (entry.getRelativeStart() < time) {
                passedAdBreakDurations += entry.getDuration();
            }
        }

        return (time + passedAdBreakDurations) / 1000;
    }

    public double totalAdBreakDurations() {
        double breakDurations = 0;

        for (TimelineEntry entry : entries) {
            breakDurations += entry.getDuration();
        }

        return breakDurations / 1000;
    }
}
