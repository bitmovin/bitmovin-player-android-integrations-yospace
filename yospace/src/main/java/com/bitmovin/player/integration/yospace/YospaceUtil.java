package com.bitmovin.player.integration.yospace;

import com.bitmovin.player.api.event.data.MetadataEvent;
import com.bitmovin.player.config.advertising.AdSourceType;
import com.bitmovin.player.model.emsg.EventMessage;
import com.bitmovin.player.model.id3.BinaryFrame;
import com.yospace.android.hls.analytic.advert.Advert;
import com.yospace.hls.TimedMetadata;

public class YospaceUtil {

    public static TimedMetadata createTimedMetadata(MetadataEvent metadataEvent) {
        TimedMetadata timedMetadata = null;

        if (metadataEvent.getType() == "EMSG") {
            return convertEmsgToId3(metadataEvent);
        } else if (metadataEvent.getType() == "ID3") {
            return processId3(metadataEvent);
        }

        return timedMetadata;
    }

    private static TimedMetadata processId3(MetadataEvent metadataEvent) {
        String ymid = null;
        String yseq = null;
        String ytyp = null;
        String ydur = null;
        String yprg = null;
        com.bitmovin.player.model.Metadata metadata = metadataEvent.getMetadata();
        for (int i = 0; i < metadata.length(); i++) {
            com.bitmovin.player.model.Metadata.Entry entry = metadata.get(i);
            if (entry instanceof BinaryFrame) {
                BinaryFrame binFrame = (BinaryFrame) entry;

                if ("YMID".equals(binFrame.id)) {
                    ymid = new String(binFrame.data);
                } else if ("YSEQ".equals(binFrame.id)) {
                    yseq = new String(binFrame.data);
                } else if ("YTYP".equals(binFrame.id)) {
                    ytyp = new String(binFrame.data);
                } else if ("YDUR".equals(binFrame.id)) {
                    ydur = new String(binFrame.data);
                } else if ("YPRG".equals(binFrame.id)) {
                    yprg = new String(binFrame.data);
                }
            }
        }

        return generateTimedMetadata(ymid, yseq, ytyp, ydur, yprg);
    }

    private static TimedMetadata convertEmsgToId3(MetadataEvent metadataEvent) {
        String ymid = null;
        String yseq = null;
        String ytyp = null;
        String ydur = null;
        String yprg = null;

        com.bitmovin.player.model.Metadata metadata = metadataEvent.getMetadata();

        for (int i = 0; i < metadata.length(); i++) {
            EventMessage message = (EventMessage) metadata.get(i);
            String[] data = new String(message.messageData).split(",");
            for (int j = 0; j < data.length; j++) {
                String key = null;
                String value = null;
                String[] entry = data[j].split("=");
                if (entry.length > 1) {
                    key = entry[0];
                    value = entry[1];
                } else {
                    continue;
                }

                BitLog.d("Key: " + key + " Value: " + value);

                if (key.equals("YMID")) {
                    ymid = value;
                } else if (key.equals("YSEQ")) {
                    yseq = value;
                } else if (key.equals("YTYP")) {
                    ytyp = value;
                } else if (key.equals("YDUR")) {
                    ydur = value;
                } else if (key.equals("YPRG")) {
                    yprg = value;
                }
            }
        }

        return generateTimedMetadata(ymid, yseq, ytyp, ydur, yprg);
    }

    private static TimedMetadata generateTimedMetadata(String ymid, String yseq, String ytyp, String ydur, String yprg) {
        if (ymid != null && yseq != null && ytyp != null && ydur != null) {
            return TimedMetadata.createFromId3Tags(ymid, yseq, ytyp, ydur);
        } else if (yprg != null) {
            return TimedMetadata.createFromId3Tags(yprg, 0.0f);
        }

        return null;
    }

    public static YospaceAdStartedEvent createAdStartEvent(AdSourceType clientType, String clickThroughUrl, int indexInQueue, double duration, double timeOffset, String position, double skipOffset, boolean isTrueX) {
        return new YospaceAdStartedEvent(clientType, clickThroughUrl, indexInQueue, duration / 1000, timeOffset / 1000, position, skipOffset / 1000, isTrueX);
    }

    public static String getAdClickThroughUrl(Advert advert) {
        String clickThroughUrl = "";
        if (advert.getLinearCreative() != null && advert.getLinearCreative().getVideoClicks() != null) {
            clickThroughUrl = advert.getLinearCreative().getVideoClicks().getClickThroughUrl();
        }
        return clickThroughUrl;
    }
}
