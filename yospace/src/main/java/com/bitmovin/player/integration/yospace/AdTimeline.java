package com.bitmovin.player.integration.yospace;

import com.yospace.android.hls.analytic.advert.Advert;

import java.util.ArrayList;
import java.util.List;

public class AdTimeline {
    private List<AdBreak> adBreaks = new ArrayList<AdBreak>();

    public AdTimeline(List<com.yospace.android.hls.analytic.advert.AdBreak> adBreaks) {
        double count = 0;

        for (com.yospace.android.hls.analytic.advert.AdBreak adbreak : adBreaks) {
            AdBreak entry = new AdBreak("unknown", adbreak.getStartMillis() - count, adbreak.getDuration(), adbreak.getStartMillis(), adbreak.getStartMillis() + adbreak.getDuration());
            count += adbreak.getDuration();

            for (Advert advert : adbreak.getAdverts()) {
                Ad ad = new Ad(advert.getIdentifier(), entry.getRelativeStart(), advert.getDuration(), advert.getStartMillis(), advert.getStartMillis() + advert.getDuration(), advert.hasLinearInteractiveUnit());
                entry.appendAd(ad);
            }
            this.adBreaks.add(entry);
        }
    }

    @Override
    public String toString() {
        String str = adBreaks.size() + " ad breaks ";
        for (AdBreak entry : adBreaks) {
            str += " [" + entry.getRelativeStart() + " - " + entry.getDuration() + "] ";
        }
        return str;
    }

    public AdBreak currentAdBreak(double time) {
        time = time * 1000;
        AdBreak currentAdBreak = null;

        for (AdBreak adBreak : adBreaks) {
            if (adBreak.getAbsoluteStart() < time && (adBreak.getAbsoluteStart() + adBreak.getDuration()) > time) {
                currentAdBreak = adBreak;
                break;
            }
        }
        return currentAdBreak;
    }

    public Ad currentAd(double time) {
        time = time * 1000;

        AdBreak currentAdBreak = null;
        Ad currentAd = null;

        for (AdBreak adBreak : adBreaks) {
            if (adBreak.getAbsoluteStart() < time && (adBreak.getAbsoluteStart() + adBreak.getDuration()) > time) {
                currentAdBreak = adBreak;
                break;
            }
        }

        if (currentAdBreak == null) {
            return null;
        }

        List<Ad> ads = currentAdBreak.getAds();

        for (Ad ad : ads) {
            if (ad.getAbsoluteStart() < time && (ad.getAbsoluteEnd() > time)) {
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

        for (AdBreak entry : adBreaks) {
            if (entry.getAbsoluteEnd() < time) {
                passedAdBreakDurations += entry.getDuration();
            }
        }

        if (currentAdBreak != null) {
            return (currentAdBreak.getAbsoluteStart() - passedAdBreakDurations) / 1000;
        }

        return (time - passedAdBreakDurations) / 1000;
    }

    public double relativeToAbsolute(double time) {
        time = time * 1000;
        double passedAdBreakDurations = 0;

        for (AdBreak entry : adBreaks) {
            if (entry.getRelativeStart() < time) {
                passedAdBreakDurations += entry.getDuration();
            }
        }

        return (time + passedAdBreakDurations) / 1000;
    }

    public double totalAdBreakDurations() {
        double breakDurations = 0;

        for (AdBreak entry : adBreaks) {
            breakDurations += entry.getDuration();
        }

        return breakDurations / 1000;
    }

    public List<AdBreak> getAdBreaks() {
        return adBreaks;
    }
}
