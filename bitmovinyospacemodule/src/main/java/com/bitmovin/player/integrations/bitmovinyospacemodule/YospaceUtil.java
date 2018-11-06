package com.bitmovin.player.integrations.bitmovinyospacemodule;

import com.bitmovin.player.api.event.data.MetadataEvent;
import com.bitmovin.player.model.id3.BinaryFrame;
import com.yospace.hls.TimedMetadata;

public class YospaceUtil {

    public static TimedMetadata createTimedMetadata(MetadataEvent metadataEvent) {
        TimedMetadata timedMetadata = null;

        if (metadataEvent.getType() == "ID3") {
            com.bitmovin.player.model.Metadata metadata = metadataEvent.getMetadata();
            String ymid = null;
            String yseq = null;
            String ytyp = null;
            String ydur = null;
            String yprg = null;

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

            if (ymid != null && yseq != null && ytyp != null && ydur != null) {
                timedMetadata = TimedMetadata.createFromId3Tags(ymid, yseq, ytyp, ydur);
            } else if (yprg != null) {
                timedMetadata = TimedMetadata.createFromId3Tags(yprg, 0.0f);
            }
        }
        return timedMetadata;
    }
}
