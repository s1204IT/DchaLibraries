package com.mediatek.camera.gesture;

import android.view.MotionEvent;

public class DownUpDetector {
    private DownUpListener mListener;
    private boolean mStillDown;

    public interface DownUpListener {
        void onDownEvent(MotionEvent motionEvent);

        void onUpEvent(MotionEvent motionEvent);
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
            this.mListener.onDownEvent(motionEvent);
        } else {
            this.mListener.onUpEvent(motionEvent);
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
}
