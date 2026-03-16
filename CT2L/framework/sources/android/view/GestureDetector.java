package android.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

public class GestureDetector {
    private static final int LONG_PRESS = 2;
    private static final int SHOW_PRESS = 1;
    private static final int TAP = 3;
    private boolean mAlwaysInBiggerTapRegion;
    private boolean mAlwaysInTapRegion;
    private MotionEvent mCurrentDownEvent;
    private boolean mDeferConfirmSingleTap;
    private OnDoubleTapListener mDoubleTapListener;
    private int mDoubleTapSlopSquare;
    private int mDoubleTapTouchSlopSquare;
    private float mDownFocusX;
    private float mDownFocusY;
    private final Handler mHandler;
    private boolean mInLongPress;
    private final InputEventConsistencyVerifier mInputEventConsistencyVerifier;
    private boolean mIsDoubleTapping;
    private boolean mIsLongpressEnabled;
    private float mLastFocusX;
    private float mLastFocusY;
    private final OnGestureListener mListener;
    private int mMaximumFlingVelocity;
    private int mMinimumFlingVelocity;
    private MotionEvent mPreviousUpEvent;
    private boolean mStillDown;
    private int mTouchSlopSquare;
    private VelocityTracker mVelocityTracker;
    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout();
    private static final int DOUBLE_TAP_TIMEOUT = ViewConfiguration.getDoubleTapTimeout();
    private static final int DOUBLE_TAP_MIN_TIME = ViewConfiguration.getDoubleTapMinTime();

    public interface OnDoubleTapListener {
        boolean onDoubleTap(MotionEvent motionEvent);

        boolean onDoubleTapEvent(MotionEvent motionEvent);

        boolean onSingleTapConfirmed(MotionEvent motionEvent);
    }

    public interface OnGestureListener {
        boolean onDown(MotionEvent motionEvent);

        boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);

        void onLongPress(MotionEvent motionEvent);

        boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float f, float f2);

        void onShowPress(MotionEvent motionEvent);

        boolean onSingleTapUp(MotionEvent motionEvent);
    }

    public static class SimpleOnGestureListener implements OnGestureListener, OnDoubleTapListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }
    }

    private class GestureHandler extends Handler {
        GestureHandler() {
        }

        GestureHandler(Handler handler) {
            super(handler.getLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    GestureDetector.this.mListener.onShowPress(GestureDetector.this.mCurrentDownEvent);
                    return;
                case 2:
                    GestureDetector.this.dispatchLongPress();
                    return;
                case 3:
                    if (GestureDetector.this.mDoubleTapListener != null) {
                        if (!GestureDetector.this.mStillDown) {
                            GestureDetector.this.mDoubleTapListener.onSingleTapConfirmed(GestureDetector.this.mCurrentDownEvent);
                            return;
                        } else {
                            GestureDetector.this.mDeferConfirmSingleTap = true;
                            return;
                        }
                    }
                    return;
                default:
                    throw new RuntimeException("Unknown message " + msg);
            }
        }
    }

    @Deprecated
    public GestureDetector(OnGestureListener listener, Handler handler) {
        this(null, listener, handler);
    }

    @Deprecated
    public GestureDetector(OnGestureListener listener) {
        this(null, listener, null);
    }

    public GestureDetector(Context context, OnGestureListener listener) {
        this(context, listener, null);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler) {
        this.mInputEventConsistencyVerifier = InputEventConsistencyVerifier.isInstrumentationEnabled() ? new InputEventConsistencyVerifier(this, 0) : null;
        if (handler != null) {
            this.mHandler = new GestureHandler(handler);
        } else {
            this.mHandler = new GestureHandler();
        }
        this.mListener = listener;
        if (listener instanceof OnDoubleTapListener) {
            setOnDoubleTapListener((OnDoubleTapListener) listener);
        }
        init(context);
    }

    public GestureDetector(Context context, OnGestureListener listener, Handler handler, boolean unused) {
        this(context, listener, handler);
    }

    private void init(Context context) {
        int touchSlop;
        int doubleTapTouchSlop;
        int doubleTapSlop;
        if (this.mListener == null) {
            throw new NullPointerException("OnGestureListener must not be null");
        }
        this.mIsLongpressEnabled = true;
        if (context == null) {
            touchSlop = ViewConfiguration.getTouchSlop();
            doubleTapTouchSlop = touchSlop;
            doubleTapSlop = ViewConfiguration.getDoubleTapSlop();
            this.mMinimumFlingVelocity = ViewConfiguration.getMinimumFlingVelocity();
            this.mMaximumFlingVelocity = ViewConfiguration.getMaximumFlingVelocity();
        } else {
            ViewConfiguration configuration = ViewConfiguration.get(context);
            touchSlop = configuration.getScaledTouchSlop();
            doubleTapTouchSlop = configuration.getScaledDoubleTapTouchSlop();
            doubleTapSlop = configuration.getScaledDoubleTapSlop();
            this.mMinimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
            this.mMaximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        }
        this.mTouchSlopSquare = touchSlop * touchSlop;
        this.mDoubleTapTouchSlopSquare = doubleTapTouchSlop * doubleTapTouchSlop;
        this.mDoubleTapSlopSquare = doubleTapSlop * doubleTapSlop;
    }

    public void setOnDoubleTapListener(OnDoubleTapListener onDoubleTapListener) {
        this.mDoubleTapListener = onDoubleTapListener;
    }

    public void setIsLongpressEnabled(boolean isLongpressEnabled) {
        this.mIsLongpressEnabled = isLongpressEnabled;
    }

    public boolean isLongpressEnabled() {
        return this.mIsLongpressEnabled;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onTouchEvent(ev, 0);
        }
        int action = ev.getAction();
        if (this.mVelocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        }
        this.mVelocityTracker.addMovement(ev);
        boolean pointerUp = (action & 255) == 6;
        int skipIndex = pointerUp ? ev.getActionIndex() : -1;
        float sumX = 0.0f;
        float sumY = 0.0f;
        int count = ev.getPointerCount();
        for (int i = 0; i < count; i++) {
            if (skipIndex != i) {
                sumX += ev.getX(i);
                sumY += ev.getY(i);
            }
        }
        int div = pointerUp ? count - 1 : count;
        float focusX = sumX / div;
        float focusY = sumY / div;
        boolean handled = false;
        switch (action & 255) {
            case 0:
                if (this.mDoubleTapListener != null) {
                    boolean hadTapMessage = this.mHandler.hasMessages(3);
                    if (hadTapMessage) {
                        this.mHandler.removeMessages(3);
                    }
                    if (this.mCurrentDownEvent != null && this.mPreviousUpEvent != null && hadTapMessage && isConsideredDoubleTap(this.mCurrentDownEvent, this.mPreviousUpEvent, ev)) {
                        this.mIsDoubleTapping = true;
                        handled = false | this.mDoubleTapListener.onDoubleTap(this.mCurrentDownEvent) | this.mDoubleTapListener.onDoubleTapEvent(ev);
                    } else {
                        this.mHandler.sendEmptyMessageDelayed(3, DOUBLE_TAP_TIMEOUT);
                    }
                }
                this.mLastFocusX = focusX;
                this.mDownFocusX = focusX;
                this.mLastFocusY = focusY;
                this.mDownFocusY = focusY;
                if (this.mCurrentDownEvent != null) {
                    this.mCurrentDownEvent.recycle();
                }
                this.mCurrentDownEvent = MotionEvent.obtain(ev);
                this.mAlwaysInTapRegion = true;
                this.mAlwaysInBiggerTapRegion = true;
                this.mStillDown = true;
                this.mInLongPress = false;
                this.mDeferConfirmSingleTap = false;
                if (this.mIsLongpressEnabled) {
                    this.mHandler.removeMessages(2);
                    this.mHandler.sendEmptyMessageAtTime(2, this.mCurrentDownEvent.getDownTime() + ((long) TAP_TIMEOUT) + ((long) LONGPRESS_TIMEOUT));
                }
                this.mHandler.sendEmptyMessageAtTime(1, this.mCurrentDownEvent.getDownTime() + ((long) TAP_TIMEOUT));
                handled |= this.mListener.onDown(ev);
                break;
            case 1:
                this.mStillDown = false;
                MotionEvent currentUpEvent = MotionEvent.obtain(ev);
                if (this.mIsDoubleTapping) {
                    handled = false | this.mDoubleTapListener.onDoubleTapEvent(ev);
                } else if (this.mInLongPress) {
                    this.mHandler.removeMessages(3);
                    this.mInLongPress = false;
                } else if (this.mAlwaysInTapRegion) {
                    handled = this.mListener.onSingleTapUp(ev);
                    if (this.mDeferConfirmSingleTap && this.mDoubleTapListener != null) {
                        this.mDoubleTapListener.onSingleTapConfirmed(ev);
                    }
                } else {
                    VelocityTracker velocityTracker = this.mVelocityTracker;
                    int pointerId = ev.getPointerId(0);
                    velocityTracker.computeCurrentVelocity(1000, this.mMaximumFlingVelocity);
                    float velocityY = velocityTracker.getYVelocity(pointerId);
                    float velocityX = velocityTracker.getXVelocity(pointerId);
                    if (Math.abs(velocityY) > this.mMinimumFlingVelocity || Math.abs(velocityX) > this.mMinimumFlingVelocity) {
                        handled = this.mListener.onFling(this.mCurrentDownEvent, ev, velocityX, velocityY);
                    }
                }
                if (this.mPreviousUpEvent != null) {
                    this.mPreviousUpEvent.recycle();
                }
                this.mPreviousUpEvent = currentUpEvent;
                if (this.mVelocityTracker != null) {
                    this.mVelocityTracker.recycle();
                    this.mVelocityTracker = null;
                }
                this.mIsDoubleTapping = false;
                this.mDeferConfirmSingleTap = false;
                this.mHandler.removeMessages(1);
                this.mHandler.removeMessages(2);
                break;
            case 2:
                if (!this.mInLongPress) {
                    float scrollX = this.mLastFocusX - focusX;
                    float scrollY = this.mLastFocusY - focusY;
                    if (this.mIsDoubleTapping) {
                        handled = false | this.mDoubleTapListener.onDoubleTapEvent(ev);
                    } else if (this.mAlwaysInTapRegion) {
                        int deltaX = (int) (focusX - this.mDownFocusX);
                        int deltaY = (int) (focusY - this.mDownFocusY);
                        int distance = (deltaX * deltaX) + (deltaY * deltaY);
                        if (distance > this.mTouchSlopSquare) {
                            handled = this.mListener.onScroll(this.mCurrentDownEvent, ev, scrollX, scrollY);
                            this.mLastFocusX = focusX;
                            this.mLastFocusY = focusY;
                            this.mAlwaysInTapRegion = false;
                            this.mHandler.removeMessages(3);
                            this.mHandler.removeMessages(1);
                            this.mHandler.removeMessages(2);
                        }
                        if (distance > this.mDoubleTapTouchSlopSquare) {
                            this.mAlwaysInBiggerTapRegion = false;
                        }
                    } else if (Math.abs(scrollX) >= 1.0f || Math.abs(scrollY) >= 1.0f) {
                        handled = this.mListener.onScroll(this.mCurrentDownEvent, ev, scrollX, scrollY);
                        this.mLastFocusX = focusX;
                        this.mLastFocusY = focusY;
                    }
                }
                break;
            case 3:
                cancel();
                break;
            case 5:
                this.mLastFocusX = focusX;
                this.mDownFocusX = focusX;
                this.mLastFocusY = focusY;
                this.mDownFocusY = focusY;
                cancelTaps();
                break;
            case 6:
                this.mLastFocusX = focusX;
                this.mDownFocusX = focusX;
                this.mLastFocusY = focusY;
                this.mDownFocusY = focusY;
                this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumFlingVelocity);
                int upIndex = ev.getActionIndex();
                int id1 = ev.getPointerId(upIndex);
                float x1 = this.mVelocityTracker.getXVelocity(id1);
                float y1 = this.mVelocityTracker.getYVelocity(id1);
                int i2 = 0;
                while (true) {
                    if (i2 < count) {
                        if (i2 != upIndex) {
                            int id2 = ev.getPointerId(i2);
                            float x = x1 * this.mVelocityTracker.getXVelocity(id2);
                            float y = y1 * this.mVelocityTracker.getYVelocity(id2);
                            float dot = x + y;
                            if (dot < 0.0f) {
                                this.mVelocityTracker.clear();
                                break;
                            }
                        }
                        i2++;
                    }
                    break;
                }
                break;
        }
        if (!handled && this.mInputEventConsistencyVerifier != null) {
            this.mInputEventConsistencyVerifier.onUnhandledEvent(ev, 0);
        }
        return handled;
    }

    private void cancel() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        this.mVelocityTracker.recycle();
        this.mVelocityTracker = null;
        this.mIsDoubleTapping = false;
        this.mStillDown = false;
        this.mAlwaysInTapRegion = false;
        this.mAlwaysInBiggerTapRegion = false;
        this.mDeferConfirmSingleTap = false;
        if (this.mInLongPress) {
            this.mInLongPress = false;
        }
    }

    private void cancelTaps() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        this.mIsDoubleTapping = false;
        this.mAlwaysInTapRegion = false;
        this.mAlwaysInBiggerTapRegion = false;
        this.mDeferConfirmSingleTap = false;
        if (this.mInLongPress) {
            this.mInLongPress = false;
        }
    }

    private boolean isConsideredDoubleTap(MotionEvent firstDown, MotionEvent firstUp, MotionEvent secondDown) {
        if (!this.mAlwaysInBiggerTapRegion) {
            return false;
        }
        long deltaTime = secondDown.getEventTime() - firstUp.getEventTime();
        if (deltaTime > DOUBLE_TAP_TIMEOUT || deltaTime < DOUBLE_TAP_MIN_TIME) {
            return false;
        }
        int deltaX = ((int) firstDown.getX()) - ((int) secondDown.getX());
        int deltaY = ((int) firstDown.getY()) - ((int) secondDown.getY());
        return (deltaX * deltaX) + (deltaY * deltaY) < this.mDoubleTapSlopSquare;
    }

    private void dispatchLongPress() {
        this.mHandler.removeMessages(3);
        this.mDeferConfirmSingleTap = false;
        this.mInLongPress = true;
        this.mListener.onLongPress(this.mCurrentDownEvent);
    }
}
