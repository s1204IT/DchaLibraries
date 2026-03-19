package com.mediatek.camera.feature.mode.slowmotion;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.mediatek.camera.common.IAppUiListener;

public class SlowMotionGestureImpl implements IAppUiListener.OnGestureListener {
    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(float f, float f2) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(float f, float f2) {
        return true;
    }

    @Override
    public boolean onDoubleTap(float f, float f2) {
        return true;
    }

    @Override
    public boolean onLongPress(float f, float f2) {
        return false;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }

    @Override
    public boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
        return false;
    }
}
