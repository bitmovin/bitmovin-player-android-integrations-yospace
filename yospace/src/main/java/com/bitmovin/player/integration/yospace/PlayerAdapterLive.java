/*
 * COPYRIGHT 2019-2022 YOSPACE TECHNOLOGIES LTD. ALL RIGHTS RESERVED.
 * The contents of this file are proprietary and confidential.
 * Unauthorised copying of this file, via any medium is strictly prohibited.
 */

package com.bitmovin.player.integration.yospace;

import android.app.Activity;

import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.emsg.EventMessage;
import com.google.android.exoplayer2.metadata.id3.BinaryFrame;

import com.yospace.admanagement.TimedMetadata;

public class PlayerAdapterLive extends PlayerAdapter implements MetadataOutput {

    // helper class for converting metadata from Exoplayer to Yospace format
    private static class MetadataHelper {
        private String mYmid;
        private String mYseq;
        private String mYtyp;
        private String mYdur;

        MetadataHelper() {
            mYmid = null;
            mYseq = null;
            mYtyp = null;
            mYdur = null;
        }

        public void setYmid(String str) { mYmid = str; }
        public void setYseq(String str) { mYseq = str; }
        public void setYtyp(String str) { mYtyp = str; }
        public void setYdur(String str) { mYdur = str; }

        public boolean isValid() {
            return (mYmid != null && mYseq != null && mYtyp != null && mYdur != null);
        }

        public TimedMetadata createMetadata(long pos) {
            return TimedMetadata.createFromMetadata(mYmid, mYseq, mYtyp, mYdur, pos);
        }
    }

    MetadataHelper mMetadata = new MetadataHelper();

    PlayerAdapterLive(Activity activity, TimelineView timeline) {
        super(activity, timeline);
    }

    // see com.google.android.exoplayer2.metadata.MetadataOutput#onMetadata(MetaData)
    @Override
    public void onMetadata(Metadata metadata) {

        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry entry = metadata.get(i);
            if (entry instanceof BinaryFrame) {
                getBinaryFrameMetadata((BinaryFrame)entry);
            } else if (entry instanceof EventMessage) {
                getEventMessageMetadata((EventMessage)entry);
            }
        }

        // convert the timed metadata from ExoPlayer to Yospace format
        TimedMetadata timedMetadata = null;
        if (mMetadata.isValid()) {
            timedMetadata = mMetadata.createMetadata(getPlayerCurrentPosition());
        }

        // ... and send it to the Yospace session
        if (timedMetadata != null) {
            mPlaybackEventListener.onTimedMetadata(timedMetadata);
        }
    }

    // helper functions

    public void getBinaryFrameMetadata(BinaryFrame binFrame) {
        String data = new String(binFrame.data);
        switch (binFrame.id) {
            case "YMID":
                mMetadata.setYmid(data);
                break;
            case "YSEQ":
                mMetadata.setYseq(data);
                break;
            case "YTYP":
                mMetadata.setYtyp(data);
                break;
            case "YDUR":
                mMetadata.setYdur(data);
                break;
            default:
                break;
        }
    }

    public void getEventMessageMetadata(EventMessage msg) {
        if (msg.schemeIdUri.equalsIgnoreCase("urn:yospace:a:id3:2016")) {
            String msgData = new String(msg.messageData);
            String[] frames = msgData.split(",");
            for (String frame : frames) {
                String[] keys = frame.split("=");
                switch (keys[0].toUpperCase()) {
                    case "YMID":
                        mMetadata.setYmid(keys[1]);
                        break;
                    case "YSEQ":
                        mMetadata.setYseq(keys[1]);
                        break;
                    case "YTYP":
                        mMetadata.setYtyp(keys[1]);
                        break;
                    case "YDUR":
                        mMetadata.setYdur(keys[1]);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
