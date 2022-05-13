/*
 * COPYRIGHT 2019-2022 YOSPACE TECHNOLOGIES LTD. ALL RIGHTS RESERVED.
 * The contents of this file are proprietary and confidential.
 * Unauthorised copying of this file, via any medium is strictly prohibited.
 */

package com.bitmovin.player.integration.yospace;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaLoadData;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import com.yospace.admanagement.PlaybackEventHandler;
import com.yospace.admanagement.util.YoLog;
import com.yospace.util.Constant;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class PlayerAdapter implements Player.Listener, MediaSourceEventListener {

    private SimpleExoPlayer mPlayer;

    private final Activity mActivity;

    private final TimelineView mTimeline;

    private int mLastPlaybackState = Player.STATE_IDLE;
    private boolean mIsPaused = false;

    // The playback event listener.
    // This will be a Yospace Session
    PlaybackEventHandler mPlaybackEventListener;

    private Timer mPlayheadTimer;

    private long mInitialPlayerWindow = 0L;
    private long mCurrentPlayerWindow = 0L;

    PlayerAdapter(Activity activity, TimelineView timeline) {
        mTimeline = timeline;
        mActivity = activity;
    }

    void setVideoPlayer(SimpleExoPlayer player) {
        mPlayer = player;
    }

    // Sets the playback listener (a Yospace session).
    // Will be called from the player Activity
    void setPlaybackEventListener(PlaybackEventHandler listener) {
        mPlaybackEventListener = listener;
    }

    long getPlayerCurrentPosition() {
        long position = mPlayer.getCurrentPosition();
        Timeline tl = mPlayer.getCurrentTimeline();
        if (true) {
            if (tl != null && !tl.isEmpty()) {
                // Adjust position to be relative to start of period rather than window.
                position -= tl.getPeriod(mPlayer.getCurrentPeriodIndex(), new Timeline.Period())
                              .getPositionInWindowMs();
            }
        } else { // DASH
            if (tl != null && !tl.isEmpty()) {
                Timeline.Window win = new Timeline.Window();
                tl.getWindow(0, win);
                if (mInitialPlayerWindow == 0) {
                    mInitialPlayerWindow = win.windowStartTimeMs;
                }
                mCurrentPlayerWindow = win.windowStartTimeMs;
            }
            position += (mCurrentPlayerWindow - mInitialPlayerWindow);
        }
        return position;
    }

    /////

    private void addPlayheadObserver() {
        // NB: run only one observer at a time
        removePlayheadObserver();

        // start a polling timer
        // the Yospace session requires regular playhead updates, if it is VOD or NLSO
        mPlayheadTimer = new Timer();
        mPlayheadTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                mActivity.runOnUiThread(() -> {
                    long position = getPlayerCurrentPosition();

                    // update the playhead to the Yospace session
                    mPlaybackEventListener.onPlayheadUpdate(position);

                    // update the timeline position
                    if (mTimeline != null) {
                        mTimeline.setPlayhead(position);
                    }
                });
            }
        }, 0);
    }

    private void removePlayheadObserver() {
        if (mPlayheadTimer != null) {
            BitLog.INSTANCE.i("PlayerAdapter.removePlayheadObserver - called");
            mPlayheadTimer.cancel();
            mPlayheadTimer = null;
        }
    }

    private static String playbackStateToString(int playbackState) {
        if (playbackState == 1) {
            return "IDLE";
        } else if (playbackState == 2) {
            return "BUFFERING";
        } else if (playbackState == 3) {
            return "READY";
        } else if (playbackState == 4) {
            return "ENDED";
        }
        return "UNKNOWN";
    }

    //////////////////////////////////////
    // Player.EventListener implementation

    @Override
    public void onIsLoadingChanged(boolean isLoading) {
        // do nothing
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason) {
        YoLog.d(YoLog.DEBUG_LIFECYCLE, Constant.getLogTag(), "New Player SessionResult: " + (playWhenReady ? " (playWhenReady:TRUE)" : " (playWhenReady:FALSE)"));

        // do nothing if there is no Yospace session
        if (mPlaybackEventListener == null) {
            return;
        }

        if (mPlayer.getPlaybackState() == Player.STATE_READY) {
            PlaybackEventHandler.PlayerEvent event;
            if (playWhenReady) {
                // keep track of paused - so we send RESUME only when paused, START otherwise.
                if (mIsPaused) {
                    event = PlaybackEventHandler.PlayerEvent.RESUME;
                    mIsPaused = false;
                } else {
                    event = PlaybackEventHandler.PlayerEvent.START;
                }
            } else {
                event = PlaybackEventHandler.PlayerEvent.PAUSE;
                mIsPaused = true;
            }

            long playheadPosition = getPlayerCurrentPosition();
            mPlaybackEventListener.onPlayerEvent(event, playheadPosition);
            addRemovePlayheadObserver(event);
        }
    }

    // helper function for onPlayWhenReadyChanged()
    private void addRemovePlayheadObserver(PlaybackEventHandler.PlayerEvent event) {
        // if we are playing a VOD / NLSO stream then set up a playhead observer
        if (mTimeline != null) {
            if (event == PlaybackEventHandler.PlayerEvent.START || event == PlaybackEventHandler.PlayerEvent.RESUME) {
                addPlayheadObserver();
            } else {
                removePlayheadObserver();
            }
        }
    }

    @Override
    public void onPlaybackStateChanged(@Player.State int playbackState) {
        YoLog.d(YoLog.DEBUG_LIFECYCLE, Constant.getLogTag(), "New Player SessionResult: " + playbackStateToString(playbackState));

        // do nothing if we are not playing, or if there is no Yospace session

        if (playbackState == Player.STATE_IDLE) {
            return;
        }

        if (mPlaybackEventListener == null) {
            return;
        }

        long playheadPosition = getPlayerCurrentPosition();

        // inform the Yospace Session of the event and the playhead position

        if (playbackState == Player.STATE_ENDED) {
            removePlayheadObserver();
            mPlaybackEventListener.onPlayerEvent(PlaybackEventHandler.PlayerEvent.STOP, playheadPosition);
        } else if (playbackState == Player.STATE_BUFFERING && mLastPlaybackState != Player.STATE_BUFFERING) {
            // Pass on buffering transitions
            mPlaybackEventListener.onPlayerEvent(PlaybackEventHandler.PlayerEvent.STALL, playheadPosition);
        } else if (playbackState == Player.STATE_READY && mLastPlaybackState == Player.STATE_BUFFERING) {
            mPlaybackEventListener.onPlayerEvent(PlaybackEventHandler.PlayerEvent.CONTINUE, playheadPosition);
        }
        mLastPlaybackState = playbackState;


        // Pass on Play / Pause events
        onPlayWhenReadyChanged(mPlayer.getPlayWhenReady(), Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE);
    }

    @Override
    public void onPositionDiscontinuity(@NonNull Player.PositionInfo oldPosition,
                                        @NonNull Player.PositionInfo newPosition,
                                        @Player.DiscontinuityReason int reason) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            long position = getPlayerCurrentPosition();

            // inform the Yospace Session of the event and the playhead position
            mPlaybackEventListener.onPlayerEvent(PlaybackEventHandler.PlayerEvent.SEEK, position);

            // update the timeline position
            if (mTimeline != null) {
                mTimeline.setPlayhead(position);
            }
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {
        BitLog.INSTANCE.d("Event onRepeatModeChanged");
    }

    @Override
    public void onPlaybackParametersChanged(@NonNull PlaybackParameters playbackParameters) {
        BitLog.INSTANCE.d("Event onPlaybackParametersChanged");
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
        BitLog.INSTANCE.d("Event onShuffleModeEnabledChanged");
    }

    @Override
    public void onTimelineChanged(@NonNull Timeline timeline, @Player.TimelineChangeReason int reason) {
        BitLog.INSTANCE.d("Event onTimelineChanged");
    }

    @Override
    public void onPlayerError(@NonNull ExoPlaybackException e) {
    }

    @Override
    public void onTracksChanged(@NonNull TrackGroupArray trackGroups,
                                @NonNull TrackSelectionArray trackSelections) {
        BitLog.INSTANCE.d("Event onTracksChanged");
    }

    //////////////////////////////////////
    // MediaSourceEventListener implementation

    @Override
    public void onLoadStarted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId,
                              LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        BitLog.INSTANCE.d("Event onLoadStarted");
    }

    @Override
    public void onLoadError(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId,
                            LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData,
                            IOException error, boolean wasCanceled) {
        BitLog.INSTANCE.d("Event onLoadError");
    }

    @Override
    public void onLoadCanceled(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId,
                               LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        BitLog.INSTANCE.d("Event onLoadCanceled");
    }

    @Override
    public void onLoadCompleted(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId,
                                LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
        BitLog.INSTANCE.d("Event onLoadCompleted");
    }

    @Override
    public void onUpstreamDiscarded(int windowIndex, MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
        BitLog.INSTANCE.d("Event onUpstreamDiscarded");
    }

    @Override
    public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
        BitLog.INSTANCE.d("Event onDownstreamFormatChanged");
    }
}
