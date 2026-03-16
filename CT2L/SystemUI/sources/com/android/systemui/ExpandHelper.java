package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.policy.ScrollAdapter;

public class ExpandHelper {
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private Callback mCallback;
    private Context mContext;
    private float mCurrentHeight;
    private View mEventSource;
    private boolean mExpanding;
    private FlingAnimationUtils mFlingAnimationUtils;
    private boolean mHasPopped;
    private float mInitialTouchFocusY;
    private float mInitialTouchSpan;
    private float mInitialTouchY;
    private int mLargeSize;
    private float mLastFocusY;
    private float mLastMotionY;
    private float mLastSpanY;
    private float mMaximumStretch;
    private float mNaturalHeight;
    private float mOldHeight;
    private boolean mOnlyMovements;
    private int mPopDuration;
    private float mPullGestureMinXSpan;
    private ExpandableView mResizedView;
    private ScaleGestureDetector mSGD;
    private ScrollAdapter mScrollAdapter;
    private int mSmallSize;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private Vibrator mVibrator;
    private boolean mWatchingForPull;
    private int mExpansionStyle = 0;
    private boolean mEnabled = true;
    private ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            ExpandHelper.this.startExpanding(ExpandHelper.this.mResizedView, 4);
            return ExpandHelper.this.mExpanding;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        }
    };
    private ViewScaler mScaler = new ViewScaler();
    private int mGravity = 48;
    private ObjectAnimator mScaleAnimation = ObjectAnimator.ofFloat(this.mScaler, "height", 0.0f);

    public interface Callback {
        boolean canChildBeExpanded(View view);

        void expansionStateChanged(boolean z);

        ExpandableView getChildAtPosition(float f, float f2);

        ExpandableView getChildAtRawPosition(float f, float f2);

        void setUserExpandedChild(View view, boolean z);

        void setUserLockedChild(View view, boolean z);
    }

    private class ViewScaler {
        ExpandableView mView;

        public ViewScaler() {
        }

        public void setView(ExpandableView v) {
            this.mView = v;
        }

        public void setHeight(float h) {
            this.mView.setActualHeight((int) h);
            ExpandHelper.this.mCurrentHeight = h;
        }

        public float getHeight() {
            return this.mView.getActualHeight();
        }

        public int getNaturalHeight(int maximum) {
            return Math.min(maximum, this.mView.getMaxHeight());
        }
    }

    public ExpandHelper(Context context, Callback callback, int small, int large) {
        this.mSmallSize = small;
        this.mMaximumStretch = this.mSmallSize * 2.0f;
        this.mLargeSize = large;
        this.mContext = context;
        this.mCallback = callback;
        this.mPopDuration = this.mContext.getResources().getInteger(R.integer.blinds_pop_duration_ms);
        this.mPullGestureMinXSpan = this.mContext.getResources().getDimension(R.dimen.pull_span_min);
        ViewConfiguration configuration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = configuration.getScaledTouchSlop();
        this.mSGD = new ScaleGestureDetector(context, this.mScaleGestureListener);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
    }

    private void updateExpansion() {
        float span = (this.mSGD.getCurrentSpan() - this.mInitialTouchSpan) * 1.0f;
        float drag = (this.mSGD.getFocusY() - this.mInitialTouchFocusY) * 1.0f * (this.mGravity == 80 ? -1.0f : 1.0f);
        float pull = Math.abs(drag) + Math.abs(span) + 1.0f;
        float hand = ((Math.abs(drag) * drag) / pull) + ((Math.abs(span) * span) / pull);
        float target = hand + this.mOldHeight;
        float newHeight = clamp(target);
        this.mScaler.setHeight(newHeight);
        this.mLastFocusY = this.mSGD.getFocusY();
        this.mLastSpanY = this.mSGD.getCurrentSpan();
    }

    private float clamp(float target) {
        float out = target;
        if (out < this.mSmallSize) {
            out = this.mSmallSize;
        } else if (out > this.mLargeSize) {
            out = this.mLargeSize;
        }
        return out > this.mNaturalHeight ? this.mNaturalHeight : out;
    }

    private ExpandableView findView(float x, float y) {
        if (this.mEventSource != null) {
            int[] location = new int[2];
            this.mEventSource.getLocationOnScreen(location);
            ExpandableView v = this.mCallback.getChildAtRawPosition(x + location[0], y + location[1]);
            return v;
        }
        ExpandableView v2 = this.mCallback.getChildAtPosition(x, y);
        return v2;
    }

    private boolean isInside(View v, float x, float y) {
        boolean inside;
        if (v == null) {
            return false;
        }
        if (this.mEventSource != null) {
            int[] location = new int[2];
            this.mEventSource.getLocationOnScreen(location);
            x += location[0];
            y += location[1];
        }
        int[] location2 = new int[2];
        v.getLocationOnScreen(location2);
        float x2 = x - location2[0];
        float y2 = y - location2[1];
        if (x2 <= 0.0f || y2 <= 0.0f) {
            inside = false;
        } else {
            if ((x2 < ((float) v.getWidth())) & (y2 < ((float) v.getHeight()))) {
                inside = true;
            }
        }
        return inside;
    }

    public void setEventSource(View eventSource) {
        this.mEventSource = eventSource;
    }

    public void setScrollAdapter(ScrollAdapter adapter) {
        this.mScrollAdapter = adapter;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        trackVelocity(ev);
        int action = ev.getAction();
        this.mSGD.onTouchEvent(ev);
        int x = (int) this.mSGD.getFocusX();
        int y = (int) this.mSGD.getFocusY();
        this.mInitialTouchFocusY = y;
        this.mInitialTouchSpan = this.mSGD.getCurrentSpan();
        this.mLastFocusY = this.mInitialTouchFocusY;
        this.mLastSpanY = this.mInitialTouchSpan;
        if (this.mExpanding) {
            this.mLastMotionY = ev.getRawY();
            maybeRecycleVelocityTracker(ev);
            return true;
        }
        if (action == 2 && (this.mExpansionStyle & 1) != 0) {
            return true;
        }
        switch (action & 255) {
            case 0:
                this.mWatchingForPull = this.mScrollAdapter != null && isInside(this.mScrollAdapter.getHostView(), (float) x, (float) y) && this.mScrollAdapter.isScrolledToTop();
                this.mResizedView = findView(x, y);
                this.mInitialTouchY = ev.getY();
                break;
            case 1:
            case 3:
                finishExpanding(false, getCurrentVelocity());
                clearView();
                break;
            case 2:
                float xspan = this.mSGD.getCurrentSpanX();
                if (xspan > this.mPullGestureMinXSpan && xspan > this.mSGD.getCurrentSpanY() && !this.mExpanding) {
                    startExpanding(this.mResizedView, 2);
                    this.mWatchingForPull = false;
                }
                if (this.mWatchingForPull) {
                    float yDiff = ev.getRawY() - this.mInitialTouchY;
                    if (yDiff > this.mTouchSlop) {
                        this.mWatchingForPull = false;
                        if (this.mResizedView != null && !isFullyExpanded(this.mResizedView) && startExpanding(this.mResizedView, 1)) {
                            this.mLastMotionY = ev.getRawY();
                            this.mInitialTouchY = ev.getRawY();
                            this.mHasPopped = false;
                        }
                    }
                }
                break;
        }
        this.mLastMotionY = ev.getRawY();
        maybeRecycleVelocityTracker(ev);
        return this.mExpanding;
    }

    private void trackVelocity(MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case 0:
                if (this.mVelocityTracker == null) {
                    this.mVelocityTracker = VelocityTracker.obtain();
                } else {
                    this.mVelocityTracker.clear();
                }
                this.mVelocityTracker.addMovement(event);
                break;
            case 2:
                if (this.mVelocityTracker == null) {
                    this.mVelocityTracker = VelocityTracker.obtain();
                }
                this.mVelocityTracker.addMovement(event);
                break;
        }
    }

    private void maybeRecycleVelocityTracker(MotionEvent event) {
        if (this.mVelocityTracker != null) {
            if (event.getActionMasked() == 3 || event.getActionMasked() == 1) {
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
            }
        }
    }

    private float getCurrentVelocity() {
        if (this.mVelocityTracker == null) {
            return 0.0f;
        }
        this.mVelocityTracker.computeCurrentVelocity(1000);
        return this.mVelocityTracker.getYVelocity();
    }

    public void setEnabled(boolean enable) {
        this.mEnabled = enable;
    }

    private boolean isEnabled() {
        return this.mEnabled;
    }

    private boolean isFullyExpanded(ExpandableView underFocus) {
        return underFocus.getIntrinsicHeight() == underFocus.getMaxHeight();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        trackVelocity(ev);
        int action = ev.getActionMasked();
        this.mSGD.onTouchEvent(ev);
        int x = (int) this.mSGD.getFocusX();
        int y = (int) this.mSGD.getFocusY();
        if (this.mOnlyMovements) {
            this.mLastMotionY = ev.getRawY();
            return false;
        }
        switch (action) {
            case 0:
                this.mWatchingForPull = this.mScrollAdapter != null && isInside(this.mScrollAdapter.getHostView(), (float) x, (float) y);
                this.mResizedView = findView(x, y);
                this.mInitialTouchY = ev.getY();
                break;
            case 1:
            case 3:
                finishExpanding(false, getCurrentVelocity());
                clearView();
                break;
            case 2:
                if (this.mWatchingForPull) {
                    float yDiff = ev.getRawY() - this.mInitialTouchY;
                    if (yDiff > this.mTouchSlop) {
                        this.mWatchingForPull = false;
                        if (this.mResizedView != null && !isFullyExpanded(this.mResizedView) && startExpanding(this.mResizedView, 1)) {
                            this.mInitialTouchY = ev.getRawY();
                            this.mLastMotionY = ev.getRawY();
                            this.mHasPopped = false;
                        }
                    }
                }
                if (this.mExpanding && (this.mExpansionStyle & 1) != 0) {
                    float rawHeight = (ev.getRawY() - this.mLastMotionY) + this.mCurrentHeight;
                    float newHeight = clamp(rawHeight);
                    boolean isFinished = false;
                    boolean expanded = false;
                    if (rawHeight > this.mNaturalHeight) {
                        isFinished = true;
                        expanded = true;
                    }
                    if (rawHeight < this.mSmallSize) {
                        isFinished = true;
                        expanded = false;
                    }
                    if (!this.mHasPopped) {
                        vibrate(this.mPopDuration);
                        this.mHasPopped = true;
                    }
                    this.mScaler.setHeight(newHeight);
                    this.mLastMotionY = ev.getRawY();
                    if (isFinished) {
                        this.mCallback.setUserExpandedChild(this.mResizedView, expanded);
                        this.mCallback.expansionStateChanged(false);
                        return false;
                    }
                    this.mCallback.expansionStateChanged(true);
                    return true;
                }
                if (this.mExpanding) {
                    updateExpansion();
                    this.mLastMotionY = ev.getRawY();
                    return true;
                }
                break;
            case 5:
            case 6:
                this.mInitialTouchY += this.mSGD.getFocusY() - this.mLastFocusY;
                this.mInitialTouchSpan += this.mSGD.getCurrentSpan() - this.mLastSpanY;
                break;
        }
        this.mLastMotionY = ev.getRawY();
        maybeRecycleVelocityTracker(ev);
        return this.mResizedView != null;
    }

    private boolean startExpanding(ExpandableView v, int expandType) {
        if (!(v instanceof ExpandableNotificationRow)) {
            return false;
        }
        this.mExpansionStyle = expandType;
        if (this.mExpanding && v == this.mResizedView) {
            return true;
        }
        this.mExpanding = true;
        this.mCallback.expansionStateChanged(true);
        this.mCallback.setUserLockedChild(v, true);
        this.mScaler.setView(v);
        this.mOldHeight = this.mScaler.getHeight();
        this.mCurrentHeight = this.mOldHeight;
        if (this.mCallback.canChildBeExpanded(v)) {
            this.mNaturalHeight = this.mScaler.getNaturalHeight(this.mLargeSize);
            return true;
        }
        this.mNaturalHeight = this.mOldHeight;
        return true;
    }

    private void finishExpanding(boolean force, float velocity) {
        float targetHeight;
        if (this.mExpanding) {
            float currentHeight = this.mScaler.getHeight();
            float f = this.mSmallSize;
            this.mScaler.getHeight();
            boolean wasClosed = this.mOldHeight == ((float) this.mSmallSize);
            if (wasClosed) {
                targetHeight = (force || currentHeight > ((float) this.mSmallSize)) ? this.mNaturalHeight : this.mSmallSize;
            } else {
                targetHeight = (force || currentHeight < this.mNaturalHeight) ? this.mSmallSize : this.mNaturalHeight;
            }
            if (this.mScaleAnimation.isRunning()) {
                this.mScaleAnimation.cancel();
            }
            this.mCallback.setUserExpandedChild(this.mResizedView, targetHeight == this.mNaturalHeight);
            this.mCallback.expansionStateChanged(false);
            if (targetHeight != currentHeight) {
                this.mScaleAnimation.setFloatValues(targetHeight);
                this.mScaleAnimation.setupStartValues();
                final View scaledView = this.mResizedView;
                this.mScaleAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ExpandHelper.this.mCallback.setUserLockedChild(scaledView, false);
                        ExpandHelper.this.mScaleAnimation.removeListener(this);
                    }
                });
                this.mFlingAnimationUtils.apply(this.mScaleAnimation, currentHeight, targetHeight, velocity);
                this.mScaleAnimation.start();
            } else {
                this.mCallback.setUserLockedChild(this.mResizedView, false);
            }
            this.mExpanding = false;
            this.mExpansionStyle = 0;
        }
    }

    private void clearView() {
        this.mResizedView = null;
    }

    public void cancel() {
        finishExpanding(true, 0.0f);
        clearView();
        this.mSGD = new ScaleGestureDetector(this.mContext, this.mScaleGestureListener);
    }

    public void onlyObserveMovements(boolean onlyMovements) {
        this.mOnlyMovements = onlyMovements;
    }

    private synchronized void vibrate(long duration) {
        if (this.mVibrator == null) {
            this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        }
        this.mVibrator.vibrate(duration, VIBRATION_ATTRIBUTES);
    }
}
