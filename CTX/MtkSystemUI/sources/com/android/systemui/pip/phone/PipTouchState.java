package com.android.systemui.pip.phone;

import android.graphics.PointF;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import com.android.internal.annotations.VisibleForTesting;
import java.io.PrintWriter;

public class PipTouchState {

    @VisibleForTesting
    static final long DOUBLE_TAP_TIMEOUT = 200;
    private int mActivePointerId;
    private final Runnable mDoubleTapTimeoutCallback;
    private final Handler mHandler;
    private VelocityTracker mVelocityTracker;
    private final ViewConfiguration mViewConfig;
    private long mDownTouchTime = 0;
    private long mLastDownTouchTime = 0;
    private long mUpTouchTime = 0;
    private final PointF mDownTouch = new PointF();
    private final PointF mDownDelta = new PointF();
    private final PointF mLastTouch = new PointF();
    private final PointF mLastDelta = new PointF();
    private final PointF mVelocity = new PointF();
    private boolean mAllowTouches = true;
    private boolean mIsUserInteracting = false;
    private boolean mIsDoubleTap = false;
    private boolean mIsWaitingForDoubleTap = false;
    private boolean mIsDragging = false;
    private boolean mPreviouslyDragging = false;
    private boolean mStartedDragging = false;
    private boolean mAllowDraggingOffscreen = false;

    public PipTouchState(ViewConfiguration viewConfiguration, Handler handler, Runnable runnable) {
        this.mViewConfig = viewConfiguration;
        this.mHandler = handler;
        this.mDoubleTapTimeoutCallback = runnable;
    }

    public void reset() {
        this.mAllowDraggingOffscreen = false;
        this.mIsDragging = false;
        this.mStartedDragging = false;
        this.mIsUserInteracting = false;
    }

    public void onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        z = false;
        z = false;
        boolean z = false;
        if (action != 6) {
            switch (action) {
                case 0:
                    if (!this.mAllowTouches) {
                        return;
                    }
                    initOrResetVelocityTracker();
                    this.mActivePointerId = motionEvent.getPointerId(0);
                    this.mLastTouch.set(motionEvent.getX(), motionEvent.getY());
                    this.mDownTouch.set(this.mLastTouch);
                    this.mAllowDraggingOffscreen = true;
                    this.mIsUserInteracting = true;
                    this.mDownTouchTime = motionEvent.getEventTime();
                    this.mIsDoubleTap = !this.mPreviouslyDragging && this.mDownTouchTime - this.mLastDownTouchTime < DOUBLE_TAP_TIMEOUT;
                    this.mIsWaitingForDoubleTap = false;
                    this.mLastDownTouchTime = this.mDownTouchTime;
                    if (this.mDoubleTapTimeoutCallback != null) {
                        this.mHandler.removeCallbacks(this.mDoubleTapTimeoutCallback);
                        return;
                    }
                    return;
                case 1:
                    if (this.mIsUserInteracting) {
                        this.mVelocityTracker.addMovement(motionEvent);
                        this.mVelocityTracker.computeCurrentVelocity(1000, this.mViewConfig.getScaledMaximumFlingVelocity());
                        this.mVelocity.set(this.mVelocityTracker.getXVelocity(), this.mVelocityTracker.getYVelocity());
                        int iFindPointerIndex = motionEvent.findPointerIndex(this.mActivePointerId);
                        if (iFindPointerIndex == -1) {
                            Log.e("PipTouchHandler", "Invalid active pointer id on UP: " + this.mActivePointerId);
                            return;
                        }
                        this.mUpTouchTime = motionEvent.getEventTime();
                        this.mLastTouch.set(motionEvent.getX(iFindPointerIndex), motionEvent.getY(iFindPointerIndex));
                        this.mPreviouslyDragging = this.mIsDragging;
                        if (!this.mIsDoubleTap && !this.mIsDragging && this.mUpTouchTime - this.mDownTouchTime < DOUBLE_TAP_TIMEOUT) {
                            z = true;
                        }
                        this.mIsWaitingForDoubleTap = z;
                    } else {
                        return;
                    }
                    break;
                case 2:
                    if (this.mIsUserInteracting) {
                        this.mVelocityTracker.addMovement(motionEvent);
                        int iFindPointerIndex2 = motionEvent.findPointerIndex(this.mActivePointerId);
                        if (iFindPointerIndex2 == -1) {
                            Log.e("PipTouchHandler", "Invalid active pointer id on MOVE: " + this.mActivePointerId);
                            return;
                        }
                        float x = motionEvent.getX(iFindPointerIndex2);
                        float y = motionEvent.getY(iFindPointerIndex2);
                        this.mLastDelta.set(x - this.mLastTouch.x, y - this.mLastTouch.y);
                        this.mDownDelta.set(x - this.mDownTouch.x, y - this.mDownTouch.y);
                        Object[] objArr = this.mDownDelta.length() > ((float) this.mViewConfig.getScaledTouchSlop());
                        if (!this.mIsDragging) {
                            if (objArr != false) {
                                this.mIsDragging = true;
                                this.mStartedDragging = true;
                            }
                        } else {
                            this.mStartedDragging = false;
                        }
                        this.mLastTouch.set(x, y);
                        return;
                    }
                    return;
                case 3:
                    break;
                default:
                    return;
            }
            recycleVelocityTracker();
            return;
        }
        if (this.mIsUserInteracting) {
            this.mVelocityTracker.addMovement(motionEvent);
            int actionIndex = motionEvent.getActionIndex();
            if (motionEvent.getPointerId(actionIndex) == this.mActivePointerId) {
                int i = actionIndex == 0 ? 1 : 0;
                this.mActivePointerId = motionEvent.getPointerId(i);
                this.mLastTouch.set(motionEvent.getX(i), motionEvent.getY(i));
            }
        }
    }

    public PointF getVelocity() {
        return this.mVelocity;
    }

    public PointF getLastTouchPosition() {
        return this.mLastTouch;
    }

    public PointF getLastTouchDelta() {
        return this.mLastDelta;
    }

    public PointF getDownTouchPosition() {
        return this.mDownTouch;
    }

    public boolean isDragging() {
        return this.mIsDragging;
    }

    public boolean isUserInteracting() {
        return this.mIsUserInteracting;
    }

    public boolean startedDragging() {
        return this.mStartedDragging;
    }

    public void setAllowTouches(boolean z) {
        this.mAllowTouches = z;
        if (this.mIsUserInteracting) {
            reset();
        }
    }

    public boolean allowDraggingOffscreen() {
        return this.mAllowDraggingOffscreen;
    }

    public boolean isDoubleTap() {
        return this.mIsDoubleTap;
    }

    public boolean isWaitingForDoubleTap() {
        return this.mIsWaitingForDoubleTap;
    }

    public void scheduleDoubleTapTimeoutCallback() {
        if (this.mIsWaitingForDoubleTap) {
            long doubleTapTimeoutCallbackDelay = getDoubleTapTimeoutCallbackDelay();
            this.mHandler.removeCallbacks(this.mDoubleTapTimeoutCallback);
            this.mHandler.postDelayed(this.mDoubleTapTimeoutCallback, doubleTapTimeoutCallbackDelay);
        }
    }

    @VisibleForTesting
    long getDoubleTapTimeoutCallbackDelay() {
        if (this.mIsWaitingForDoubleTap) {
            return Math.max(0L, DOUBLE_TAP_TIMEOUT - (this.mUpTouchTime - this.mDownTouchTime));
        }
        return -1L;
    }

    private void initOrResetVelocityTracker() {
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            this.mVelocityTracker.clear();
        }
    }

    private void recycleVelocityTracker() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.recycle();
            this.mVelocityTracker = null;
        }
    }

    public void dump(PrintWriter printWriter, String str) {
        String str2 = str + "  ";
        printWriter.println(str + "PipTouchHandler");
        printWriter.println(str2 + "mAllowTouches=" + this.mAllowTouches);
        printWriter.println(str2 + "mActivePointerId=" + this.mActivePointerId);
        printWriter.println(str2 + "mDownTouch=" + this.mDownTouch);
        printWriter.println(str2 + "mDownDelta=" + this.mDownDelta);
        printWriter.println(str2 + "mLastTouch=" + this.mLastTouch);
        printWriter.println(str2 + "mLastDelta=" + this.mLastDelta);
        printWriter.println(str2 + "mVelocity=" + this.mVelocity);
        printWriter.println(str2 + "mIsUserInteracting=" + this.mIsUserInteracting);
        printWriter.println(str2 + "mIsDragging=" + this.mIsDragging);
        printWriter.println(str2 + "mStartedDragging=" + this.mStartedDragging);
        printWriter.println(str2 + "mAllowDraggingOffscreen=" + this.mAllowDraggingOffscreen);
    }
}
