package com.android.camera.ui;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.android.camera.debug.Log;

public class FilmstripGestureRecognizer {
    private static final Log.Tag TAG = new Log.Tag("FStripGestureRecog");
    private final GestureDetector mGestureDetector;
    private final Listener mListener;
    private final ScaleGestureDetector mScaleDetector;

    public interface Listener {
        boolean onDoubleTap(float f, float f2);

        boolean onDown(float f, float f2);

        boolean onFling(float f, float f2);

        void onLongPress(float f, float f2);

        boolean onScale(float f, float f2, float f3);

        boolean onScaleBegin(float f, float f2);

        void onScaleEnd();

        boolean onScroll(float f, float f2, float f3, float f4);

        boolean onSingleTapUp(float f, float f2);

        boolean onUp(float f, float f2);
    }

    public FilmstripGestureRecognizer(Context context, Listener listener) {
        this.mListener = listener;
        this.mGestureDetector = new GestureDetector(context, new MyGestureListener(), null, true);
        this.mGestureDetector.setOnDoubleTapListener(new MyDoubleTapListener());
        this.mScaleDetector = new ScaleGestureDetector(context, new MyScaleListener());
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean gestureProcessed = this.mGestureDetector.onTouchEvent(event);
        boolean scaleProcessed = this.mScaleDetector.onTouchEvent(event);
        if (event.getAction() == 1) {
            this.mListener.onUp(event.getX(), event.getY());
        }
        return gestureProcessed | scaleProcessed;
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private MyGestureListener() {
        }

        @Override
        public void onLongPress(MotionEvent e) {
            FilmstripGestureRecognizer.this.mListener.onLongPress(e.getX(), e.getY());
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return FilmstripGestureRecognizer.this.mListener.onScroll(e2.getX(), e2.getY(), dx, dy);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return FilmstripGestureRecognizer.this.mListener.onFling(velocityX, velocityY);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            FilmstripGestureRecognizer.this.mListener.onDown(e.getX(), e.getY());
            return super.onDown(e);
        }
    }

    private class MyDoubleTapListener implements GestureDetector.OnDoubleTapListener {
        private MyDoubleTapListener() {
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return FilmstripGestureRecognizer.this.mListener.onSingleTapUp(e.getX(), e.getY());
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return FilmstripGestureRecognizer.this.mListener.onDoubleTap(e.getX(), e.getY());
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return true;
        }
    }

    private class MyScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private MyScaleListener() {
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return FilmstripGestureRecognizer.this.mListener.onScaleBegin(detector.getFocusX(), detector.getFocusY());
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return FilmstripGestureRecognizer.this.mListener.onScale(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            FilmstripGestureRecognizer.this.mListener.onScaleEnd();
        }
    }
}
