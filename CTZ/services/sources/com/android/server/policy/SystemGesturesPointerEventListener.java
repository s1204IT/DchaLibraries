package com.android.server.policy;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import android.widget.OverScroller;
import com.android.server.usb.descriptors.UsbACInterface;

public class SystemGesturesPointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    private static final boolean DEBUG = false;
    private static final int MAX_FLING_TIME_MILLIS = 5000;
    private static final int MAX_TRACKED_POINTERS = 32;
    private static final int SWIPE_FROM_BOTTOM = 2;
    private static final int SWIPE_FROM_LEFT = 4;
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

        void onSwipeFromLeft();

        void onSwipeFromRight();

        void onSwipeFromTop();

        void onUpOrCancel();
    }

    public SystemGesturesPointerEventListener(Context context, Callbacks callbacks) {
        this.mContext = context;
        this.mCallbacks = (Callbacks) checkNull("callbacks", callbacks);
        this.mSwipeStartThreshold = ((Context) checkNull("context", context)).getResources().getDimensionPixelSize(R.dimen.floating_window_z);
        this.mSwipeDistanceThreshold = this.mSwipeStartThreshold;
    }

    private static <T> T checkNull(String str, T t) {
        if (t == null) {
            throw new IllegalArgumentException(str + " must not be null");
        }
        return t;
    }

    public void systemReady() {
        this.mGestureDetector = new GestureDetector(this.mContext, new FlingGestureDetector(), new Handler(Looper.myLooper()));
        this.mOverscroller = new OverScroller(this.mContext);
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        if (this.mGestureDetector != null && motionEvent.isTouchEvent()) {
            this.mGestureDetector.onTouchEvent(motionEvent);
        }
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 5) {
            captureDown(motionEvent, motionEvent.getActionIndex());
            if (this.mDebugFireable) {
                this.mDebugFireable = motionEvent.getPointerCount() < 5;
                if (!this.mDebugFireable) {
                    this.mCallbacks.onDebug();
                    return;
                }
                return;
            }
            return;
        }
        if (actionMasked != 7) {
            switch (actionMasked) {
                case 0:
                    this.mSwipeFireable = true;
                    this.mDebugFireable = true;
                    this.mDownPointers = 0;
                    captureDown(motionEvent, 0);
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
                        int iDetectSwipe = detectSwipe(motionEvent);
                        this.mSwipeFireable = iDetectSwipe == 0;
                        if (iDetectSwipe == 1) {
                            this.mCallbacks.onSwipeFromTop();
                        } else if (iDetectSwipe == 2) {
                            this.mCallbacks.onSwipeFromBottom();
                        } else if (iDetectSwipe == 3) {
                            this.mCallbacks.onSwipeFromRight();
                        } else if (iDetectSwipe == 4) {
                            this.mCallbacks.onSwipeFromLeft();
                        }
                    }
                    break;
            }
            return;
        }
        if (motionEvent.isFromSource(UsbACInterface.FORMAT_III_IEC1937_MPEG1_Layer1)) {
            if (!this.mMouseHoveringAtEdge && motionEvent.getY() == 0.0f) {
                this.mCallbacks.onMouseHoverAtTop();
                this.mMouseHoveringAtEdge = true;
                return;
            }
            if (!this.mMouseHoveringAtEdge && motionEvent.getY() >= this.screenHeight - 1) {
                this.mCallbacks.onMouseHoverAtBottom();
                this.mMouseHoveringAtEdge = true;
            } else if (this.mMouseHoveringAtEdge && motionEvent.getY() > 0.0f && motionEvent.getY() < this.screenHeight - 1) {
                this.mCallbacks.onMouseLeaveFromEdge();
                this.mMouseHoveringAtEdge = false;
            }
        }
    }

    private void captureDown(MotionEvent motionEvent, int i) {
        int iFindIndex = findIndex(motionEvent.getPointerId(i));
        if (iFindIndex != -1) {
            this.mDownX[iFindIndex] = motionEvent.getX(i);
            this.mDownY[iFindIndex] = motionEvent.getY(i);
            this.mDownTime[iFindIndex] = motionEvent.getEventTime();
        }
    }

    private int findIndex(int i) {
        for (int i2 = 0; i2 < this.mDownPointers; i2++) {
            if (this.mDownPointerId[i2] == i) {
                return i2;
            }
        }
        if (this.mDownPointers == 32 || i == -1) {
            return -1;
        }
        int[] iArr = this.mDownPointerId;
        int i3 = this.mDownPointers;
        this.mDownPointers = i3 + 1;
        iArr[i3] = i;
        return this.mDownPointers - 1;
    }

    private int detectSwipe(MotionEvent motionEvent) {
        int historySize = motionEvent.getHistorySize();
        int pointerCount = motionEvent.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            int iFindIndex = findIndex(motionEvent.getPointerId(i));
            if (iFindIndex != -1) {
                for (int i2 = 0; i2 < historySize; i2++) {
                    int iDetectSwipe = detectSwipe(iFindIndex, motionEvent.getHistoricalEventTime(i2), motionEvent.getHistoricalX(i, i2), motionEvent.getHistoricalY(i, i2));
                    if (iDetectSwipe != 0) {
                        return iDetectSwipe;
                    }
                }
                int iDetectSwipe2 = detectSwipe(iFindIndex, motionEvent.getEventTime(), motionEvent.getX(i), motionEvent.getY(i));
                if (iDetectSwipe2 != 0) {
                    return iDetectSwipe2;
                }
            }
        }
        return 0;
    }

    private int detectSwipe(int i, long j, float f, float f2) {
        float f3 = this.mDownX[i];
        float f4 = this.mDownY[i];
        long j2 = j - this.mDownTime[i];
        if (f4 <= this.mSwipeStartThreshold && f2 > this.mSwipeDistanceThreshold + f4 && j2 < 500) {
            return 1;
        }
        if (f4 >= this.screenHeight - this.mSwipeStartThreshold && f2 < f4 - this.mSwipeDistanceThreshold && j2 < 500) {
            return 2;
        }
        if (f3 >= this.screenWidth - this.mSwipeStartThreshold && f < f3 - this.mSwipeDistanceThreshold && j2 < 500) {
            return 3;
        }
        if (f3 <= this.mSwipeStartThreshold && f > f3 + this.mSwipeDistanceThreshold && j2 < 500) {
            return 4;
        }
        return 0;
    }

    private final class FlingGestureDetector extends GestureDetector.SimpleOnGestureListener {
        private FlingGestureDetector() {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            if (!SystemGesturesPointerEventListener.this.mOverscroller.isFinished()) {
                SystemGesturesPointerEventListener.this.mOverscroller.forceFinished(true);
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2) {
            SystemGesturesPointerEventListener.this.mOverscroller.computeScrollOffset();
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (SystemGesturesPointerEventListener.this.mLastFlingTime != 0 && jUptimeMillis > SystemGesturesPointerEventListener.this.mLastFlingTime + 5000) {
                SystemGesturesPointerEventListener.this.mOverscroller.forceFinished(true);
            }
            SystemGesturesPointerEventListener.this.mOverscroller.fling(0, 0, (int) f, (int) f2, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
            int duration = SystemGesturesPointerEventListener.this.mOverscroller.getDuration();
            if (duration > SystemGesturesPointerEventListener.MAX_FLING_TIME_MILLIS) {
                duration = SystemGesturesPointerEventListener.MAX_FLING_TIME_MILLIS;
            }
            SystemGesturesPointerEventListener.this.mLastFlingTime = jUptimeMillis;
            SystemGesturesPointerEventListener.this.mCallbacks.onFling(duration);
            return true;
        }
    }
}
