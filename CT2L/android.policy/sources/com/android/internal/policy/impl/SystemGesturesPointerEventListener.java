package com.android.internal.policy.impl;

import android.R;
import android.content.Context;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy;

public class SystemGesturesPointerEventListener implements WindowManagerPolicy.PointerEventListener {
    private static final boolean DEBUG = false;
    private static final int MAX_TRACKED_POINTERS = 32;
    private static final int SWIPE_FROM_BOTTOM = 2;
    private static final int SWIPE_FROM_RIGHT = 3;
    private static final int SWIPE_FROM_TOP = 1;
    private static final int SWIPE_NONE = 0;
    private static final long SWIPE_TIMEOUT_MS = 500;
    private static final String TAG = "SystemGestures";
    private static final int UNTRACKED_POINTER = -1;
    private final Callbacks mCallbacks;
    private boolean mDebugFireable;
    private int mDownPointers;
    private final int mSwipeDistanceThreshold;
    private boolean mSwipeFireable;
    private final int mSwipeStartThreshold;
    int screenHeight;
    int screenWidth;
    private final int[] mDownPointerId = new int[MAX_TRACKED_POINTERS];
    private final float[] mDownX = new float[MAX_TRACKED_POINTERS];
    private final float[] mDownY = new float[MAX_TRACKED_POINTERS];
    private final long[] mDownTime = new long[MAX_TRACKED_POINTERS];

    interface Callbacks {
        void onDebug();

        void onSwipeFromBottom();

        void onSwipeFromRight();

        void onSwipeFromTop();
    }

    public SystemGesturesPointerEventListener(Context context, Callbacks callbacks) {
        this.mCallbacks = (Callbacks) checkNull("callbacks", callbacks);
        this.mSwipeStartThreshold = ((Context) checkNull("context", context)).getResources().getDimensionPixelSize(R.dimen.accessibility_focus_highlight_stroke_width);
        this.mSwipeDistanceThreshold = this.mSwipeStartThreshold;
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return arg;
    }

    public void onPointerEvent(MotionEvent event) {
        boolean z = DEBUG;
        switch (event.getActionMasked()) {
            case SWIPE_NONE:
                this.mSwipeFireable = true;
                this.mDebugFireable = true;
                this.mDownPointers = SWIPE_NONE;
                captureDown(event, SWIPE_NONE);
                break;
            case 1:
            case 3:
                this.mSwipeFireable = DEBUG;
                this.mDebugFireable = DEBUG;
                break;
            case 2:
                if (this.mSwipeFireable) {
                    int swipe = detectSwipe(event);
                    if (swipe == 0) {
                        z = true;
                    }
                    this.mSwipeFireable = z;
                    if (swipe == 1) {
                        this.mCallbacks.onSwipeFromTop();
                    } else if (swipe == 2) {
                        this.mCallbacks.onSwipeFromBottom();
                    } else if (swipe == 3) {
                        this.mCallbacks.onSwipeFromRight();
                    }
                }
                break;
            case 5:
                captureDown(event, event.getActionIndex());
                if (this.mDebugFireable) {
                    this.mDebugFireable = event.getPointerCount() >= 5 ? SWIPE_NONE : true;
                    if (!this.mDebugFireable) {
                        this.mCallbacks.onDebug();
                    }
                }
                break;
        }
    }

    private void captureDown(MotionEvent event, int pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        int i = findIndex(pointerId);
        if (i != UNTRACKED_POINTER) {
            this.mDownX[i] = event.getX(pointerIndex);
            this.mDownY[i] = event.getY(pointerIndex);
            this.mDownTime[i] = event.getEventTime();
        }
    }

    private int findIndex(int pointerId) {
        for (int i = SWIPE_NONE; i < this.mDownPointers; i++) {
            if (this.mDownPointerId[i] == pointerId) {
                return i;
            }
        }
        if (this.mDownPointers == MAX_TRACKED_POINTERS || pointerId == UNTRACKED_POINTER) {
            return UNTRACKED_POINTER;
        }
        int[] iArr = this.mDownPointerId;
        int i2 = this.mDownPointers;
        this.mDownPointers = i2 + 1;
        iArr[i2] = pointerId;
        int i3 = this.mDownPointers + UNTRACKED_POINTER;
        return i3;
    }

    private int detectSwipe(MotionEvent move) {
        int historySize = move.getHistorySize();
        int pointerCount = move.getPointerCount();
        for (int p = SWIPE_NONE; p < pointerCount; p++) {
            int pointerId = move.getPointerId(p);
            int i = findIndex(pointerId);
            if (i != UNTRACKED_POINTER) {
                for (int h = SWIPE_NONE; h < historySize; h++) {
                    long time = move.getHistoricalEventTime(h);
                    float x = move.getHistoricalX(p, h);
                    float y = move.getHistoricalY(p, h);
                    int swipe = detectSwipe(i, time, x, y);
                    if (swipe != 0) {
                        return swipe;
                    }
                }
                int swipe2 = detectSwipe(i, move.getEventTime(), move.getX(p), move.getY(p));
                if (swipe2 != 0) {
                    return swipe2;
                }
            }
        }
        return SWIPE_NONE;
    }

    private int detectSwipe(int i, long time, float x, float y) {
        float fromX = this.mDownX[i];
        float fromY = this.mDownY[i];
        long elapsed = time - this.mDownTime[i];
        if (fromY <= this.mSwipeStartThreshold && y > this.mSwipeDistanceThreshold + fromY && elapsed < SWIPE_TIMEOUT_MS) {
            return 1;
        }
        if (fromY >= this.screenHeight - this.mSwipeStartThreshold && y < fromY - this.mSwipeDistanceThreshold && elapsed < SWIPE_TIMEOUT_MS) {
            return 2;
        }
        if (fromX >= this.screenWidth - this.mSwipeStartThreshold && x < fromX - this.mSwipeDistanceThreshold && elapsed < SWIPE_TIMEOUT_MS) {
            return 3;
        }
        return SWIPE_NONE;
    }
}
