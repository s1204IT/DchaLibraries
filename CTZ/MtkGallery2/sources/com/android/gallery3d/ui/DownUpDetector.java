package com.android.gallery3d.ui;

import android.view.MotionEvent;

public class DownUpDetector {
    private DownUpListener mListener;
    private boolean mStillDown;

    public interface DownUpListener {
        void onDown(MotionEvent motionEvent);

        void onUp(MotionEvent motionEvent);
    }

    public DownUpDetector(DownUpListener downUpListener) {
        this.mListener = downUpListener;
    }

    private void setState(boolean z, MotionEvent motionEvent) {
        if (z == this.mStillDown) {
            return;
        }
        this.mStillDown = z;
        if (z) {
            this.mListener.onDown(motionEvent);
        } else {
            this.mListener.onUp(motionEvent);
        }
    }

    public void onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction() & 255;
        if (action != 3) {
            if (action != 5) {
                switch (action) {
                }
            }
            setState(true, motionEvent);
            return;
        }
        setState(false, motionEvent);
    }

    public boolean isDown() {
        return this.mStillDown;
    }
}
