package com.mediatek.camera.common;

import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.mediatek.camera.common.utils.Size;

public interface IAppUiListener {

    public interface ISurfaceStatusListener {
        void surfaceAvailable(Object obj, int i, int i2);

        void surfaceChanged(Object obj, int i, int i2);

        void surfaceDestroyed(Object obj, int i, int i2);
    }

    public interface OnGestureListener {
        boolean onDoubleTap(float f, float f2);

        boolean onDown(MotionEvent motionEvent);

        boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);

        boolean onLongPress(float f, float f2);

        boolean onScale(ScaleGestureDetector scaleGestureDetector);

        boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector);

        boolean onScaleEnd(ScaleGestureDetector scaleGestureDetector);

        boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);

        boolean onSingleTapConfirmed(float f, float f2);

        boolean onSingleTapUp(float f, float f2);

        boolean onUp(MotionEvent motionEvent);
    }

    public interface OnModeChangeListener {
        void onModeSelected(String str);
    }

    public interface OnPreviewAreaChangedListener {
        void onPreviewAreaChanged(RectF rectF, Size size);
    }

    public interface OnShutterButtonListener {
        boolean onShutterButtonClick();

        boolean onShutterButtonFocus(boolean z);

        boolean onShutterButtonLongPressed();
    }

    public interface OnThumbnailClickedListener {
        void onThumbnailClicked();
    }
}
