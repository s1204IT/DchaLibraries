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

    public RepeatingImageButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.imageButtonStyle);
    }

    public RepeatingImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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

    public void setRepeatListener(RepeatListener l, long interval) {
        this.mListener = l;
        this.mInterval = interval;
    }

    @Override
    public boolean performLongClick() {
        this.mStartTime = SystemClock.elapsedRealtime();
        this.mRepeatCount = 0;
        post(this.mRepeater);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == 1) {
            removeCallbacks(this.mRepeater);
            if (this.mStartTime != 0) {
                doRepeat(true);
                this.mStartTime = 0L;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 23:
            case 66:
                super.onKeyDown(keyCode, event);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 23:
            case 66:
                removeCallbacks(this.mRepeater);
                if (this.mStartTime != 0) {
                    doRepeat(true);
                    this.mStartTime = 0L;
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void doRepeat(boolean last) {
        int i;
        long now = SystemClock.elapsedRealtime();
        if (this.mListener != null) {
            RepeatListener repeatListener = this.mListener;
            long j = now - this.mStartTime;
            if (last) {
                i = -1;
            } else {
                i = this.mRepeatCount;
                this.mRepeatCount = i + 1;
            }
            repeatListener.onRepeat(this, j, i);
        }
    }
}
