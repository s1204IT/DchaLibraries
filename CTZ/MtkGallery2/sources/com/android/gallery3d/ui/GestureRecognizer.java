package com.android.gallery3d.ui;

import android.content.Context;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import com.android.gallery3d.ui.DownUpDetector;

public class GestureRecognizer {
    private static final String TAG = "Gallery2/GestureRecognizer";
    private final DownUpDetector mDownUpDetector;
    private GLRoot mGLRoot;
    private final GestureDetector mGestureDetector;
    private Listener mListener;
    private boolean mListenerAvaliable;
    private final ScaleGestureDetector mScaleDetector;

    public interface Listener {
        boolean onDoubleTap(float f, float f2);

        void onDown(float f, float f2);

        boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);

        boolean onScale(float f, float f2, float f3);

        boolean onScaleBegin(float f, float f2);

        void onScaleEnd();

        boolean onScroll(float f, float f2, float f3, float f4);

        boolean onSingleTapConfirmed(float f, float f2);

        boolean onSingleTapUp(float f, float f2);

        void onUp();
    }

    public GestureRecognizer(Context context, Listener listener) {
        this.mListener = listener;
        this.mGestureDetector = new GestureDetector(context, new MyGestureListener(), null, true);
        this.mScaleDetector = new ScaleGestureDetector(context, new MyScaleListener());
        this.mDownUpDetector = new DownUpDetector(new MyDownUpListener());
        this.mListenerAvaliable = true;
    }

    public GestureRecognizer(Context context, Listener listener, GLRoot gLRoot) {
        this(context, listener);
        this.mGLRoot = gLRoot;
    }

    public void onTouchEvent(MotionEvent motionEvent) {
        this.mGestureDetector.onTouchEvent(motionEvent);
        this.mScaleDetector.onTouchEvent(motionEvent);
        this.mDownUpDetector.onTouchEvent(motionEvent);
    }

    public boolean isDown() {
        return this.mDownUpDetector.isDown();
    }

    public void cancelScale() {
        long jUptimeMillis = SystemClock.uptimeMillis();
        MotionEvent motionEventObtain = MotionEvent.obtain(jUptimeMillis, jUptimeMillis, 3, 0.0f, 0.0f, 0);
        this.mScaleDetector.onTouchEvent(motionEventObtain);
        motionEventObtain.recycle();
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private MyGestureListener() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            if (GestureRecognizer.this.mListenerAvaliable) {
                return GestureRecognizer.this.mListener.onSingleTapUp(motionEvent.getX(), motionEvent.getY());
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent motionEvent) {
            if (GestureRecognizer.this.mListenerAvaliable) {
                return GestureRecognizer.this.mListener.onDoubleTap(motionEvent.getX(), motionEvent.getY());
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (!GestureRecognizer.this.mListenerAvaliable) {
                return true;
            }
            return GestureRecognizer.this.mListener.onScroll(f, f2, motionEvent2.getX() - motionEvent.getX(), motionEvent2.getY() - motionEvent.getY());
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            if (GestureRecognizer.this.mListenerAvaliable) {
                return GestureRecognizer.this.mListener.onFling(motionEvent, motionEvent2, f, f2);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            if (GestureRecognizer.this.mListenerAvaliable) {
                if (GestureRecognizer.this.mGLRoot != null) {
                    GestureRecognizer.this.mGLRoot.lockRenderThread();
                    try {
                        return GestureRecognizer.this.mListener.onSingleTapConfirmed(motionEvent.getX(), motionEvent.getY());
                    } finally {
                        GestureRecognizer.this.mGLRoot.unlockRenderThread();
                    }
                }
                return GestureRecognizer.this.mListener.onSingleTapConfirmed(motionEvent.getX(), motionEvent.getY());
            }
            return true;
        }
    }

    private class MyScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        private MyScaleListener() {
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            if (!GestureRecognizer.this.mListenerAvaliable) {
                return true;
            }
            return GestureRecognizer.this.mListener.onScaleBegin(scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY());
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            if (!GestureRecognizer.this.mListenerAvaliable) {
                return true;
            }
            return GestureRecognizer.this.mListener.onScale(scaleGestureDetector.getFocusX(), scaleGestureDetector.getFocusY(), scaleGestureDetector.getScaleFactor());
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
            if (GestureRecognizer.this.mListenerAvaliable) {
                GestureRecognizer.this.mListener.onScaleEnd();
            }
        }
    }

    private class MyDownUpListener implements DownUpDetector.DownUpListener {
        private MyDownUpListener() {
        }

        @Override
        public void onDown(MotionEvent motionEvent) {
            if (GestureRecognizer.this.mListenerAvaliable) {
                GestureRecognizer.this.mListener.onDown(motionEvent.getX(), motionEvent.getY());
            }
        }

        @Override
        public void onUp(MotionEvent motionEvent) {
            if (GestureRecognizer.this.mListenerAvaliable) {
                GestureRecognizer.this.mListener.onUp();
            }
        }
    }

    public void setAvaliable(boolean z) {
        this.mListenerAvaliable = z;
    }

    public Listener setGestureListener(Listener listener) {
        Listener listener2 = this.mListener;
        this.mListener = listener;
        return listener2;
    }
}
