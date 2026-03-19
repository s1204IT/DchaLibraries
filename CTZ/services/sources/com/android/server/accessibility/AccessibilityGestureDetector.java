package com.android.server.accessibility;

import android.content.Context;
import android.gesture.GesturePoint;
import android.graphics.PointF;
import android.util.Slog;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import java.util.ArrayList;

class AccessibilityGestureDetector extends GestureDetector.SimpleOnGestureListener {
    private static final float ANGLE_THRESHOLD = 0.0f;
    private static final long CANCEL_ON_PAUSE_THRESHOLD_NOT_STARTED_MS = 150;
    private static final long CANCEL_ON_PAUSE_THRESHOLD_STARTED_MS = 300;
    private static final boolean DEBUG = false;
    private static final int[][] DIRECTIONS_TO_GESTURE_ID = {new int[]{3, 5, 9, 10}, new int[]{6, 4, 11, 12}, new int[]{13, 14, 1, 7}, new int[]{15, 16, 8, 2}};
    private static final int DOWN = 3;
    private static final int GESTURE_CONFIRM_MM = 10;
    private static final int LEFT = 0;
    private static final String LOG_TAG = "AccessibilityGestureDetector";
    private static final float MIN_INCHES_BETWEEN_SAMPLES = 0.1f;
    private static final float MIN_PREDICTION_SCORE = 2.0f;
    private static final int RIGHT = 1;
    private static final int TOUCH_TOLERANCE = 3;
    private static final int UP = 2;
    private long mBaseTime;
    private float mBaseX;
    private float mBaseY;
    private final Context mContext;
    private boolean mDoubleTapDetected;
    private boolean mFirstTapDetected;
    private final float mGestureDetectionThreshold;
    protected GestureDetector mGestureDetector;
    private boolean mGestureStarted;
    private final Listener mListener;
    private final float mMinPixelsBetweenSamplesX;
    private final float mMinPixelsBetweenSamplesY;
    private int mPolicyFlags;
    private float mPreviousGestureX;
    private float mPreviousGestureY;
    private boolean mRecognizingGesture;
    private boolean mSecondFingerDoubleTap;
    private long mSecondPointerDownTime;
    private final ArrayList<GesturePoint> mStrokeBuffer = new ArrayList<>(100);

    public interface Listener {
        boolean onDoubleTap(MotionEvent motionEvent, int i);

        void onDoubleTapAndHold(MotionEvent motionEvent, int i);

        boolean onGestureCancelled(MotionEvent motionEvent, int i);

        boolean onGestureCompleted(int i);

        boolean onGestureStarted();
    }

    AccessibilityGestureDetector(Context context, Listener listener) {
        this.mListener = listener;
        this.mContext = context;
        this.mGestureDetectionThreshold = TypedValue.applyDimension(5, 1.0f, context.getResources().getDisplayMetrics()) * 10.0f;
        float f = context.getResources().getDisplayMetrics().xdpi;
        float f2 = context.getResources().getDisplayMetrics().ydpi;
        this.mMinPixelsBetweenSamplesX = f * MIN_INCHES_BETWEEN_SAMPLES;
        this.mMinPixelsBetweenSamplesY = MIN_INCHES_BETWEEN_SAMPLES * f2;
    }

    public boolean onMotionEvent(MotionEvent motionEvent, int i) {
        long j;
        if (this.mGestureDetector == null) {
            this.mGestureDetector = new GestureDetector(this.mContext, this);
            this.mGestureDetector.setOnDoubleTapListener(this);
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        long eventTime = motionEvent.getEventTime();
        this.mPolicyFlags = i;
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.mDoubleTapDetected = false;
                this.mSecondFingerDoubleTap = false;
                this.mRecognizingGesture = true;
                this.mGestureStarted = false;
                this.mPreviousGestureX = x;
                this.mPreviousGestureY = y;
                this.mStrokeBuffer.clear();
                this.mStrokeBuffer.add(new GesturePoint(x, y, eventTime));
                this.mBaseX = x;
                this.mBaseY = y;
                this.mBaseTime = eventTime;
                break;
            case 1:
                if (this.mDoubleTapDetected) {
                    return finishDoubleTap(motionEvent, i);
                }
                if (this.mGestureStarted) {
                    float fAbs = Math.abs(x - this.mPreviousGestureX);
                    float fAbs2 = Math.abs(y - this.mPreviousGestureY);
                    if (fAbs >= this.mMinPixelsBetweenSamplesX || fAbs2 >= this.mMinPixelsBetweenSamplesY) {
                        this.mStrokeBuffer.add(new GesturePoint(x, y, eventTime));
                    }
                    return recognizeGesture(motionEvent, i);
                }
                break;
            case 2:
                if (this.mRecognizingGesture) {
                    if (Math.hypot(this.mBaseX - x, this.mBaseY - y) > this.mGestureDetectionThreshold) {
                        this.mBaseX = x;
                        this.mBaseY = y;
                        this.mBaseTime = eventTime;
                        this.mFirstTapDetected = false;
                        this.mDoubleTapDetected = false;
                        if (!this.mGestureStarted) {
                            this.mGestureStarted = true;
                            return this.mListener.onGestureStarted();
                        }
                    } else if (!this.mFirstTapDetected) {
                        long j2 = eventTime - this.mBaseTime;
                        if (this.mGestureStarted) {
                            j = CANCEL_ON_PAUSE_THRESHOLD_STARTED_MS;
                        } else {
                            j = CANCEL_ON_PAUSE_THRESHOLD_NOT_STARTED_MS;
                        }
                        if (j2 > j) {
                            cancelGesture();
                            return this.mListener.onGestureCancelled(motionEvent, i);
                        }
                    }
                    float fAbs3 = Math.abs(x - this.mPreviousGestureX);
                    float fAbs4 = Math.abs(y - this.mPreviousGestureY);
                    if (fAbs3 >= this.mMinPixelsBetweenSamplesX || fAbs4 >= this.mMinPixelsBetweenSamplesY) {
                        this.mPreviousGestureX = x;
                        this.mPreviousGestureY = y;
                        this.mStrokeBuffer.add(new GesturePoint(x, y, eventTime));
                    }
                }
                break;
            case 3:
                clear();
                break;
            case 5:
                cancelGesture();
                if (motionEvent.getPointerCount() == 2) {
                    this.mSecondFingerDoubleTap = true;
                    this.mSecondPointerDownTime = eventTime;
                } else {
                    this.mSecondFingerDoubleTap = false;
                }
                break;
            case 6:
                if (this.mSecondFingerDoubleTap && this.mDoubleTapDetected) {
                    return finishDoubleTap(motionEvent, i);
                }
                break;
        }
        if (this.mSecondFingerDoubleTap) {
            MotionEvent motionEventMapSecondPointerToFirstPointer = mapSecondPointerToFirstPointer(motionEvent);
            if (motionEventMapSecondPointerToFirstPointer == null) {
                return false;
            }
            boolean zOnTouchEvent = this.mGestureDetector.onTouchEvent(motionEventMapSecondPointerToFirstPointer);
            motionEventMapSecondPointerToFirstPointer.recycle();
            return zOnTouchEvent;
        }
        if (this.mRecognizingGesture) {
            return this.mGestureDetector.onTouchEvent(motionEvent);
        }
        return false;
    }

    public void clear() {
        this.mFirstTapDetected = false;
        this.mDoubleTapDetected = false;
        this.mSecondFingerDoubleTap = false;
        this.mGestureStarted = false;
        this.mGestureDetector.onTouchEvent(MotionEvent.obtain(0L, 0L, 3, ANGLE_THRESHOLD, ANGLE_THRESHOLD, 0));
        cancelGesture();
    }

    public boolean firstTapDetected() {
        return this.mFirstTapDetected;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        maybeSendLongPress(motionEvent, this.mPolicyFlags);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        this.mFirstTapDetected = true;
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        clear();
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        this.mDoubleTapDetected = true;
        return false;
    }

    private void maybeSendLongPress(MotionEvent motionEvent, int i) {
        if (!this.mDoubleTapDetected) {
            return;
        }
        clear();
        this.mListener.onDoubleTapAndHold(motionEvent, i);
    }

    private boolean finishDoubleTap(MotionEvent motionEvent, int i) {
        clear();
        return this.mListener.onDoubleTap(motionEvent, i);
    }

    private void cancelGesture() {
        this.mRecognizingGesture = false;
        this.mGestureStarted = false;
        this.mStrokeBuffer.clear();
    }

    private boolean recognizeGesture(MotionEvent motionEvent, int i) {
        float f;
        float f2;
        if (this.mStrokeBuffer.size() < 2) {
            return this.mListener.onGestureCancelled(motionEvent, i);
        }
        ArrayList<PointF> arrayList = new ArrayList<>();
        PointF pointF = new PointF(this.mStrokeBuffer.get(0).x, this.mStrokeBuffer.get(0).y);
        arrayList.add(pointF);
        PointF pointF2 = pointF;
        PointF pointF3 = new PointF();
        int i2 = 0;
        int i3 = 1;
        float f3 = ANGLE_THRESHOLD;
        float f4 = ANGLE_THRESHOLD;
        float fSqrt = ANGLE_THRESHOLD;
        while (i3 < this.mStrokeBuffer.size()) {
            pointF3 = new PointF(this.mStrokeBuffer.get(i3).x, this.mStrokeBuffer.get(i3).y);
            if (i2 > 0) {
                float f5 = i2;
                float f6 = f3 / f5;
                float f7 = f4 / f5;
                PointF pointF4 = new PointF((fSqrt * f6) + pointF2.x, (fSqrt * f7) + pointF2.y);
                float f8 = pointF3.x - pointF4.x;
                float f9 = pointF3.y - pointF4.y;
                f = f3;
                f2 = f4;
                float fSqrt2 = (float) Math.sqrt((f8 * f8) + (f9 * f9));
                if ((f6 * (f8 / fSqrt2)) + (f7 * (f9 / fSqrt2)) < ANGLE_THRESHOLD) {
                    arrayList.add(pointF4);
                    pointF2 = pointF4;
                    f = 0.0f;
                    f2 = 0.0f;
                    i2 = 0;
                }
            } else {
                f = f3;
                f2 = f4;
            }
            float f10 = pointF3.x - pointF2.x;
            float f11 = pointF3.y - pointF2.y;
            fSqrt = (float) Math.sqrt((f10 * f10) + (f11 * f11));
            i2++;
            f4 = f2 + (f11 / fSqrt);
            i3++;
            f3 = f + (f10 / fSqrt);
        }
        arrayList.add(pointF3);
        Slog.i(LOG_TAG, "path=" + arrayList.toString());
        return recognizeGesturePath(motionEvent, i, arrayList);
    }

    private boolean recognizeGesturePath(MotionEvent motionEvent, int i, ArrayList<PointF> arrayList) {
        if (arrayList.size() == 2) {
            PointF pointF = arrayList.get(0);
            PointF pointF2 = arrayList.get(1);
            switch (toDirection(pointF2.x - pointF.x, pointF2.y - pointF.y)) {
                case 0:
                    return this.mListener.onGestureCompleted(3);
                case 1:
                    return this.mListener.onGestureCompleted(4);
                case 2:
                    return this.mListener.onGestureCompleted(1);
                case 3:
                    return this.mListener.onGestureCompleted(2);
            }
        }
        if (arrayList.size() == 3) {
            PointF pointF3 = arrayList.get(0);
            PointF pointF4 = arrayList.get(1);
            PointF pointF5 = arrayList.get(2);
            float f = pointF4.x - pointF3.x;
            float f2 = pointF4.y - pointF3.y;
            float f3 = pointF5.x - pointF4.x;
            float f4 = pointF5.y - pointF4.y;
            int direction = toDirection(f, f2);
            return this.mListener.onGestureCompleted(DIRECTIONS_TO_GESTURE_ID[direction][toDirection(f3, f4)]);
        }
        return this.mListener.onGestureCancelled(motionEvent, i);
    }

    private static int toDirection(float f, float f2) {
        return Math.abs(f) > Math.abs(f2) ? f < ANGLE_THRESHOLD ? 0 : 1 : f2 < ANGLE_THRESHOLD ? 2 : 3;
    }

    private MotionEvent mapSecondPointerToFirstPointer(MotionEvent motionEvent) {
        int i;
        if (motionEvent.getPointerCount() == 2) {
            if (motionEvent.getActionMasked() != 5 && motionEvent.getActionMasked() != 6 && motionEvent.getActionMasked() != 2) {
                return null;
            }
            int actionMasked = motionEvent.getActionMasked();
            if (actionMasked == 5) {
                actionMasked = 0;
            } else {
                if (actionMasked == 6) {
                    i = 1;
                }
                return MotionEvent.obtain(this.mSecondPointerDownTime, motionEvent.getEventTime(), i, motionEvent.getX(1), motionEvent.getY(1), motionEvent.getPressure(1), motionEvent.getSize(1), motionEvent.getMetaState(), motionEvent.getXPrecision(), motionEvent.getYPrecision(), motionEvent.getDeviceId(), motionEvent.getEdgeFlags());
            }
            i = actionMasked;
            return MotionEvent.obtain(this.mSecondPointerDownTime, motionEvent.getEventTime(), i, motionEvent.getX(1), motionEvent.getY(1), motionEvent.getPressure(1), motionEvent.getSize(1), motionEvent.getMetaState(), motionEvent.getXPrecision(), motionEvent.getYPrecision(), motionEvent.getDeviceId(), motionEvent.getEdgeFlags());
        }
        return null;
    }
}
