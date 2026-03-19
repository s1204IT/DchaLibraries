package com.android.music;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class RepeatingImageButton extends ImageButton {
    private long mInterval;
    private RepeatListener mListener;
    private int mRepeatCount;
    private Runnable mRepeater;
    private long mStartTime;

    public interface RepeatListener {
        void onRepeat(View view, long j, int i);
    }

    public RepeatingImageButton(Context context) {
        this(context, null);
    }

    public RepeatingImageButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.imageButtonStyle);
    }

    public RepeatingImageButton(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mInterval = 500L;
        this.mRepeater = new Runnable() {
            @Override
            public void run() {
                RepeatingImageButton.this.doRepeat(false);
                if (RepeatingImageButton.this.isPressed()) {
                    RepeatingImageButton.this.postDelayed(this, RepeatingImageButton.this.mInterval);
                }
            }
        };
        setFocusable(true);
        setLongClickable(true);
    }

    public void setRepeatListener(RepeatListener repeatListener, long j) {
        this.mListener = repeatListener;
        this.mInterval = j;
    }

    @Override
    public boolean performLongClick() {
        this.mStartTime = SystemClock.elapsedRealtime();
        this.mRepeatCount = 0;
        post(this.mRepeater);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 1) {
            removeCallbacks(this.mRepeater);
            if (this.mStartTime != 0) {
                doRepeat(true);
                this.mStartTime = 0L;
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 23 || i == 66) {
            super.onKeyDown(i, keyEvent);
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (i == 23 || i == 66) {
            removeCallbacks(this.mRepeater);
            if (this.mStartTime != 0) {
                doRepeat(true);
                this.mStartTime = 0L;
            }
        }
        return super.onKeyUp(i, keyEvent);
    }

    private void doRepeat(boolean z) {
        int i;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (this.mListener != null) {
            RepeatListener repeatListener = this.mListener;
            long j = jElapsedRealtime - this.mStartTime;
            if (z) {
                i = -1;
            } else {
                i = this.mRepeatCount;
                this.mRepeatCount = i + 1;
            }
            repeatListener.onRepeat(this, j, i);
        }
    }
}
