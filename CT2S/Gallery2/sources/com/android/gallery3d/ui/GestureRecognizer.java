package com.android.gallery3d.ui;

import android.content.Context;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.android.gallery3d.ui.DownUpDetector;

public class GestureRecognizer {
    private final DownUpDetector mDownUpDetector;
    private final GestureDetector mGestureDetector;
    private final Listener mListener;
    private final ScaleGestureDetector mScaleDetector;

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

    public GestureRecognizer(Context context, Listener listener) {
        this.mListener = listener;
        this.mGestureDetector = new GestureDetector(context, new MyGestureListener(), null, true);
        this.mScaleDetector = new ScaleGestureDetector(context, new MyScaleListener());
        this.mDownUpDetector = new DownUpDetector(new MyDownUpListener());
    }

    public void onTouchEvent(MotionEvent event) {
        this.mGestureDetector.onTouchEvent(event);
        this.mScaleDetector.onTouchEvent(event);
        this.mDownUpDetector.onTouchEvent(event);
    }

    public void cancelScale() {
        long now = SystemClock.uptimeMillis();
        MotionEvent cancelEvent = MotionEvent.obtain(now, now, 3, 0.0f, 0.0f, 0);
        this.mScaleDetector.onTouchEvent(cancelEvent);
        cancelEvent.recycle();
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private MyGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return GestureRecognizer.this.mListener.onSingleTapUp(e.getX(), e.getY());
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return GestureRecognizer.this.mListener.onDoubleTap(e.getX(), e.getY());
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return GestureRecognizer.this.mListener.onScroll(dx, dy, e2.getX() - e1.getX(), e2.getY() - e1.getY());
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return GestureRecognizer.this.mListener.onFling(e1, e2, velocityX, velocityY);
        }
    }

    private class MyScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private MyScaleListener() {
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return GestureRecognizer.this.mListener.onScaleBegin(detector.getFocusX(), detector.getFocusY());
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return GestureRecognizer.this.mListener.onScale(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            GestureRecognizer.this.mListener.onScaleEnd();
        }
    }

    private class MyDownUpListener implements DownUpDetector.DownUpListener {
        private MyDownUpListener() {
        }

        @Override
        public void onDown(MotionEvent e) {
            GestureRecognizer.this.mListener.onDown(e.getX(), e.getY());
        }

        @Override
        public void onUp(MotionEvent e) {
            GestureRecognizer.this.mListener.onUp();
        }
    }
}
