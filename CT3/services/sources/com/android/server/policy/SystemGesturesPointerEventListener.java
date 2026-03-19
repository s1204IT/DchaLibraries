package com.android.server.policy;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManagerPolicy;
import android.widget.OverScroller;
import com.mediatek.datashaping.DataShapingUtils;

public class SystemGesturesPointerEventListener implements WindowManagerPolicy.PointerEventListener {
    private static final boolean DEBUG = false;
    private static final int MAX_FLING_TIME_MILLIS = 5000;
    private static final int MAX_TRACKED_POINTERS = 32;
    private static final int SWIPE_FROM_BOTTOM = 2;
    private static final int SWIPE_FROM_RIGHT = 3;
    private static final int SWIPE_FROM_TOP = 1;
    private static final int SWIPE_NONE = 0;
    private static final long SWIPE_TIMEOUT_MS = 500;
    private static final String TAG = "SystemGestures";
    private static final int UNTRACKED_POINTER = -1;
    private final Callbacks mCallbacks;
    private final Context mContext;
    private boolean mDebugFireable;
    private int mDownPointers;
    private GestureDetector mGestureDetector;
    private long mLastFlingTime;
    private boolean mMouseHoveringAtEdge;
    private OverScroller mOverscroller;
    private final int mSwipeDistanceThreshold;
    private boolean mSwipeFireable;
    private final int mSwipeStartThreshold;
    int screenHeight;
    int screenWidth;
    private final int[] mDownPointerId = new int[32];
    private final float[] mDownX = new float[32];
    private final float[] mDownY = new float[32];
    private final long[] mDownTime = new long[32];

    interface Callbacks {
        void onDebug();

        void onDown();

        void onFling(int i);

        void onMouseHoverAtBottom();

        void onMouseHoverAtTop();

        void onMouseLeaveFromEdge();

        void onSwipeFromBottom();

        void onSwipeFromRight();

        void onSwipeFromTop();

        void onUpOrCancel();
    }

    public SystemGesturesPointerEventListener(Context context, Callbacks callbacks) {
        this.mContext = context;
        this.mCallbacks = (Callbacks) checkNull("callbacks", callbacks);
        this.mSwipeStartThreshold = ((Context) checkNull("context", context)).getResources().getDimensionPixelSize(R.dimen.accessibility_touch_slop);
        this.mSwipeDistanceThreshold = this.mSwipeStartThreshold;
    }

    private static <T> T checkNull(String name, T arg) {
        if (arg == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return arg;
    }

    public void systemReady() {
        Handler h = new Handler(Looper.myLooper());
        this.mGestureDetector = new GestureDetector(this.mContext, new FlingGestureDetector(this, null), h);
        this.mOverscroller = new OverScroller(this.mContext);
    }

    public void onPointerEvent(MotionEvent event) {
        if (this.mGestureDetector != null && event.isTouchEvent()) {
            this.mGestureDetector.onTouchEvent(event);
        }
        switch (event.getActionMasked()) {
            case 0:
                this.mSwipeFireable = true;
                this.mDebugFireable = true;
                this.mDownPointers = 0;
                captureDown(event, 0);
                if (this.mMouseHoveringAtEdge) {
                    this.mMouseHoveringAtEdge = false;
                    this.mCallbacks.onMouseLeaveFromEdge();
                }
                this.mCallbacks.onDown();
                break;
            case 1:
            case 3:
                this.mSwipeFireable = false;
                this.mDebugFireable = false;
                this.mCallbacks.onUpOrCancel();
                break;
            case 2:
                if (this.mSwipeFireable) {
                    int swipe = detectSwipe(event);
                    this.mSwipeFireable = swipe == 0;
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
                    this.mDebugFireable = event.getPointerCount() < 5;
                    if (!this.mDebugFireable) {
                        this.mCallbacks.onDebug();
                    }
                }
                break;
            case 7:
                if (event.isFromSource(8194)) {
                    if (!this.mMouseHoveringAtEdge && event.getY() == 0.0f) {
                        this.mCallbacks.onMouseHoverAtTop();
                        this.mMouseHoveringAtEdge = true;
                        break;
                    } else if (!this.mMouseHoveringAtEdge && event.getY() >= this.screenHeight - 1) {
                        this.mCallbacks.onMouseHoverAtBottom();
                        this.mMouseHoveringAtEdge = true;
                        break;
                    } else if (this.mMouseHoveringAtEdge && event.getY() > 0.0f && event.getY() < this.screenHeight - 1) {
                        this.mCallbacks.onMouseLeaveFromEdge();
                        this.mMouseHoveringAtEdge = false;
                        break;
                    }
                }
                break;
        }
    }

    private void captureDown(MotionEvent event, int pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        int i = findIndex(pointerId);
        if (i == -1) {
            return;
        }
        this.mDownX[i] = event.getX(pointerIndex);
        this.mDownY[i] = event.getY(pointerIndex);
        this.mDownTime[i] = event.getEventTime();
    }

    private int findIndex(int pointerId) {
        for (int i = 0; i < this.mDownPointers; i++) {
            if (this.mDownPointerId[i] == pointerId) {
                return i;
            }
        }
        if (this.mDownPointers == 32 || pointerId == -1) {
            return -1;
        }
        int[] iArr = this.mDownPointerId;
        int i2 = this.mDownPointers;
        this.mDownPointers = i2 + 1;
        iArr[i2] = pointerId;
        return this.mDownPointers - 1;
    }

    private int detectSwipe(MotionEvent move) {
        int historySize = move.getHistorySize();
        int pointerCount = move.getPointerCount();
        for (int p = 0; p < pointerCount; p++) {
            int pointerId = move.getPointerId(p);
            int i = findIndex(pointerId);
            if (i != -1) {
                for (int h = 0; h < historySize; h++) {
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
        return 0;
    }

    private int detectSwipe(int i, long time, float x, float y) {
        float fromX = this.mDownX[i];
        float fromY = this.mDownY[i];
        long elapsed = time - this.mDownTime[i];
        if (fromY <= this.mSwipeStartThreshold && y > this.mSwipeDistanceThreshold + fromY && elapsed < 500) {
            return 1;
        }
        if (fromY >= this.screenHeight - this.mSwipeStartThreshold && y < fromY - this.mSwipeDistanceThreshold && elapsed < 500) {
            return 2;
        }
        if (fromX >= this.screenWidth - this.mSwipeStartThreshold && x < fromX - this.mSwipeDistanceThreshold && elapsed < 500) {
            return 3;
        }
        return 0;
    }

    private final class FlingGestureDetector extends GestureDetector.SimpleOnGestureListener {
        FlingGestureDetector(SystemGesturesPointerEventListener this$0, FlingGestureDetector flingGestureDetector) {
            this();
        }

        private FlingGestureDetector() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (!SystemGesturesPointerEventListener.this.mOverscroller.isFinished()) {
                SystemGesturesPointerEventListener.this.mOverscroller.forceFinished(true);
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent down, MotionEvent up, float velocityX, float velocityY) {
            SystemGesturesPointerEventListener.this.mOverscroller.computeScrollOffset();
            long now = SystemClock.uptimeMillis();
            if (SystemGesturesPointerEventListener.this.mLastFlingTime != 0 && now > SystemGesturesPointerEventListener.this.mLastFlingTime + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC) {
                SystemGesturesPointerEventListener.this.mOverscroller.forceFinished(true);
            }
            SystemGesturesPointerEventListener.this.mOverscroller.fling(0, 0, (int) velocityX, (int) velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            int duration = SystemGesturesPointerEventListener.this.mOverscroller.getDuration();
            if (duration > SystemGesturesPointerEventListener.MAX_FLING_TIME_MILLIS) {
                duration = SystemGesturesPointerEventListener.MAX_FLING_TIME_MILLIS;
            }
            SystemGesturesPointerEventListener.this.mLastFlingTime = now;
            SystemGesturesPointerEventListener.this.mCallbacks.onFling(duration);
            return true;
        }
    }
}
