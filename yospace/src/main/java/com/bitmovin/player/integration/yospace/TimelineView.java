/*
 * COPYRIGHT 2019-2022 YOSPACE TECHNOLOGIES LTD. ALL RIGHTS RESERVED.
 * The contents of this file are proprietary and confidential.
 * Unauthorised copying of this file, via any medium is strictly prohibited.
 */

package com.bitmovin.player.integration.yospace;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.yospace.admanagement.AdBreak;
import com.yospace.admanagement.Session;
import com.yospace.admanagement.SessionLive;

import java.util.ArrayList;
import java.util.List;

public class TimelineView extends View
{
    private ShapeDrawable mTimeline;
    private ShapeDrawable mPlayheadView;
    private SimpleExoPlayer mPlayer;
    private Session mSession;

    private TextView mPlayheadTextView;
    private TextView mDurationTextView;

    List<AdBreak> mAdbreakList;
    List<ShapeDrawable> mAdbreakView;
    private long mStartTime = 0;
    private long mTotalTime;
    private long mCurrentPlayhead;

    private int mBarWidth;
    private static final int X_PADDING = 20;
    private static final int Y_OFFSET = 0;
    private static final int PLAYHEAD_WIDTH = 10;
    private static final int BAR_HEIGHT = 60;

    @SuppressWarnings("deprecation")
    private void init(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = Math.max(size.x, size.y);
        mBarWidth = width - X_PADDING;

        mTimeline = new ShapeDrawable(new RectShape());
        mTimeline.getPaint().setColor(Color.WHITE);
        mTimeline.setBounds(X_PADDING, Y_OFFSET, mBarWidth, Y_OFFSET+BAR_HEIGHT);

        mPlayheadView = new ShapeDrawable(new RectShape());
        mPlayheadView.getPaint().setColor(Color.RED);
        mPlayheadView.setBounds(X_PADDING, Y_OFFSET, X_PADDING + PLAYHEAD_WIDTH, Y_OFFSET + BAR_HEIGHT);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mTimeline.draw(canvas);

        if (mAdbreakList != null && !mAdbreakList.isEmpty()) {
            // draw any active and inactive breaks
            for (int i = 0; i < mAdbreakList.size(); i++) {
                AdBreak adbreak = mAdbreakList.get(i);
                ShapeDrawable abview = mAdbreakView.get(i);
                int start = timeToX(adbreak.getStart());
                int end = timeToX(adbreak.getStart() + adbreak.getDuration());
                abview.setBounds(Math.max(X_PADDING + start, X_PADDING), Y_OFFSET, Math.min(X_PADDING + end, mBarWidth), Y_OFFSET + BAR_HEIGHT);
                if (adbreak.isActive()) {
                    abview.getPaint().setColor(Color.BLUE);
                } else {
                    abview.getPaint().setColor(Color.DKGRAY);
                }
                abview.draw(canvas);
            }
        }

        mPlayheadView.draw(canvas);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            long time = xToTime(x);

            // check with the policy adapter if we can seek
            if ((mSession != null) && (mStartTime != Session.INVALID_WINDOW)
                    // don't seek if Live, or if we're in an active ad break
                    && !(mSession instanceof SessionLive))

            {
                mPlayer.seekTo(mSession.willSeekTo(time - mStartTime));
            }
        }
        return true;
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mPlayheadTextView != null) {
            mPlayheadTextView.setVisibility(visibility);
        }
        if (mDurationTextView != null) {
            mDurationTextView.setVisibility(visibility);
        }
    }

    public void setPlayer(SimpleExoPlayer player) {
        mPlayer = player;
    }

    public void setSession(Session session) {
        mSession = session;
    }

    public void setLabels(TextView playhead, TextView duration) {
        mPlayheadTextView = playhead;
        mDurationTextView = duration;
    }

    public void setPlayhead(long playhead) {
        int position = timeToX(playhead);
        if (position < Y_OFFSET) {
            position = Y_OFFSET;
        } else if (position > mBarWidth - X_PADDING - PLAYHEAD_WIDTH) {
            position = mBarWidth  - X_PADDING - PLAYHEAD_WIDTH;
        }

        mCurrentPlayhead = playhead; // for seek

        mPlayheadView.setBounds(X_PADDING + position, Y_OFFSET, X_PADDING + PLAYHEAD_WIDTH + position, Y_OFFSET + BAR_HEIGHT);
        this.invalidate();
    }

    public TimelineView(Context context) {
        super(context);
        init(context);
    }

    public TimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TimelineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void updateTimeline(List<AdBreak> adbreakList, long start, long end) {

        mAdbreakList = adbreakList;
        // pre-allocate the adbreak views, so we don't have to do it in onDraw
        mAdbreakView = new ArrayList<>();
        for (int i = 0; i < mAdbreakList.size(); i++) {
            mAdbreakView.add(new ShapeDrawable(new RectShape()));
        }
        mStartTime = start;
        mTotalTime = end;
        this.invalidate();
    }

    /////

    private int timeToX(long time) {
        return (int)Math.round(((double)(time - mStartTime)/ (double)(mTotalTime - mStartTime)) * (double)mBarWidth);
    }

    private long xToTime(float val) {
        return Math.round ((((double)val * (double)(mTotalTime - mStartTime)) / (double)mBarWidth) + mStartTime);
    }
}
