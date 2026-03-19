package com.mediatek.gallerybasic.dynamic;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.mediatek.gallerybasic.dynamic.DownUpDetector;

public class PlayGestureRecognizer {
    private static final String TAG = "MtkGallery2/PlayGestureRecognizer";
    private final DownUpDetector mDownUpDetector;
    private final GestureDetector mGestureDetector;
    private final Listener mListener;
    private final ScaleGestureDetector mScaleDetector;
    private boolean mScaleEventResult = false;

    public interface Listener {
        boolean onDoubleTap(float f, float f2);

        void onDown(float f, float f2);

        boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);

        boolean onScale(float f, float f2, float f3);

        boolean onScaleBegin(float f, float f2);

        void onScaleEnd();

        boolean onScroll(float f, float f2, float f3, float f4);

        boolean onSingleTapUp(float f, float f2);

        void onUp();
    }

    public PlayGestureRecognizer(Context context, Listener listener) {
        this.mListener = listener;
        this.mGestureDetector = new GestureDetector(context, new MyGestureListener(), null, true);
        this.mScaleDetector = new ScaleGestureDetector(context, new MyScaleListener());
        this.mDownUpDetector = new DownUpDetector(new MyDownUpListener());
    }

    public boolean onTouch(MotionEvent motionEvent) {
        this.mScaleEventResult = false;
        boolean zOnTouchEvent = this.mGestureDetector.onTouchEvent(motionEvent);
        this.mScaleDetector.onTouchEvent(motionEvent);
        this.mDownUpDetector.onTouchEvent(motionEvent);
        return zOnTouchEvent || this.mScaleEventResult;
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private MyGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            return PlayGestureRecognizer.this.mListener.onSingleTapUp(motionEvent.getX(), motionEvent.getY());
        }

        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            return PlayGestureRecognizer.this.mListener.onDoubleTap(motionEvent.getX(), motionEvent.getY());
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return PlayGestureRecognizer.this.mListener.onScroll(f, f2, motionEvent2.getX() - motionEvent.getX(), motionEvent2.getY() - motionEvent.getY());
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            return PlayGestureRecognizer.this.mListener.onFling(motionEvent, motionEvent2, f, f2);
        }
    }

    private class MyScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private MyScaleListener() {
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            PlayGestureRecognizer.this.mScaleEventResult = PlayGestureRecognizer.this.mListener.onScaleBegin(scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY());
            return PlayGestureRecognizer.this.mScaleEventResult;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            PlayGestureRecognizer.this.mScaleEventResult = PlayGestureRecognizer.this.mListener.onScale(scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY(), scaleGestureDetector.getScaleFactor());
            return PlayGestureRecognizer.this.mScaleEventResult;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            PlayGestureRecognizer.this.mListener.onScaleEnd();
        }
    }

    private class MyDownUpListener implements DownUpDetector.DownUpListener {
        private MyDownUpListener() {
        }

        @Override
        public void onDown(MotionEvent motionEvent) {
            PlayGestureRecognizer.this.mListener.onDown(motionEvent.getX(), motionEvent.getY());
        }

        @Override
        public void onUp(MotionEvent motionEvent) {
            PlayGestureRecognizer.this.mListener.onUp();
        }
    }
}
