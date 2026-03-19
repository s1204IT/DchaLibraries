package com.mediatek.camera.gesture;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.mediatek.camera.gesture.DownUpDetector;

public class GestureRecognizer {
    private DownUpDetector mDownUpDetector;
    private final GestureDetector mGestureDetector;
    private Listener mListener;
    private final ScaleGestureDetector mScaleDetector;

    public interface Listener extends GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener, DownUpDetector.DownUpListener {
    }

    public GestureRecognizer(Context context, Listener listener) {
        this.mListener = listener;
        this.mGestureDetector = new GestureDetector(context, new GestureListenerImpl(), null, true);
        this.mScaleDetector = new ScaleGestureDetector(context, new ScaleListenerImpl());
        this.mDownUpDetector = new DownUpDetector(new DownUpListenerImpl());
    }

    public void onTouchEvent(MotionEvent motionEvent) {
        this.mGestureDetector.onTouchEvent(motionEvent);
        this.mScaleDetector.onTouchEvent(motionEvent);
        this.mDownUpDetector.onTouchEvent(motionEvent);
    }

    private class GestureListenerImpl extends GestureDetector.SimpleOnGestureListener {
        private GestureListenerImpl() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return GestureRecognizer.this.mListener.onSingleTapUp(motionEvent);
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {
            GestureRecognizer.this.mListener.onLongPress(motionEvent);
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return GestureRecognizer.this.mListener.onScroll(motionEvent, motionEvent2, f, f2);
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return GestureRecognizer.this.mListener.onFling(motionEvent, motionEvent2, f, f2);
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {
            GestureRecognizer.this.mListener.onShowPress(motionEvent);
        }

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return GestureRecognizer.this.mListener.onDown(motionEvent);
        }

        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            return GestureRecognizer.this.mListener.onDoubleTap(motionEvent);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent motionEvent) {
            return GestureRecognizer.this.mListener.onDoubleTapEvent(motionEvent);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            return GestureRecognizer.this.mListener.onSingleTapConfirmed(motionEvent);
        }
    }

    private class ScaleListenerImpl extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private ScaleListenerImpl() {
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            return GestureRecognizer.this.mListener.onScale(scaleGestureDetector);
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return GestureRecognizer.this.mListener.onScaleBegin(scaleGestureDetector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            GestureRecognizer.this.mListener.onScaleEnd(scaleGestureDetector);
        }
    }

    private class DownUpListenerImpl implements DownUpDetector.DownUpListener {
        private DownUpListenerImpl() {
        }

        @Override
        public void onDownEvent(MotionEvent motionEvent) {
            GestureRecognizer.this.mListener.onDownEvent(motionEvent);
        }

        @Override
        public void onUpEvent(MotionEvent motionEvent) {
            GestureRecognizer.this.mListener.onUpEvent(motionEvent);
        }
    }
}
