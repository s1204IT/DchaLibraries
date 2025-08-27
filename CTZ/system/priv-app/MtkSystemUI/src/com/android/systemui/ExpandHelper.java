package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.ExpandableView;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.policy.ScrollAdapter;

/* loaded from: classes.dex */
public class ExpandHelper {
    private Callback mCallback;
    private Context mContext;
    private float mCurrentHeight;
    private View mEventSource;
    private boolean mExpanding;
    private FlingAnimationUtils mFlingAnimationUtils;
    private boolean mHasPopped;
    private float mInitialTouchFocusY;
    private float mInitialTouchSpan;
    private float mInitialTouchX;
    private float mInitialTouchY;
    private int mLargeSize;
    private float mLastFocusY;
    private float mLastMotionY;
    private float mLastSpanY;
    private float mMaximumStretch;
    private float mNaturalHeight;
    private float mOldHeight;
    private boolean mOnlyMovements;
    private float mPullGestureMinXSpan;
    private ExpandableView mResizedView;
    private ScaleGestureDetector mSGD;
    private ScrollAdapter mScrollAdapter;
    private int mSmallSize;
    private int mTouchSlop;
    private VelocityTracker mVelocityTracker;
    private boolean mWatchingForPull;
    private int mExpansionStyle = 0;
    private boolean mEnabled = true;
    private ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() { // from class: com.android.systemui.ExpandHelper.1
        @Override // android.view.ScaleGestureDetector.SimpleOnScaleGestureListener, android.view.ScaleGestureDetector.OnScaleGestureListener
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            if (!ExpandHelper.this.mOnlyMovements) {
                ExpandHelper.this.startExpanding(ExpandHelper.this.mResizedView, 4);
            }
            return ExpandHelper.this.mExpanding;
        }

        @Override // android.view.ScaleGestureDetector.SimpleOnScaleGestureListener, android.view.ScaleGestureDetector.OnScaleGestureListener
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }

        @Override // android.view.ScaleGestureDetector.SimpleOnScaleGestureListener, android.view.ScaleGestureDetector.OnScaleGestureListener
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
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

        int getMaxExpandHeight(ExpandableView expandableView);

        void setExpansionCancelled(View view);

        void setUserExpandedChild(View view, boolean z);

        void setUserLockedChild(View view, boolean z);
    }

    @VisibleForTesting
    ObjectAnimator getScaleAnimation() {
        return this.mScaleAnimation;
    }

    private class ViewScaler {
        ExpandableView mView;

        public ViewScaler() {
        }

        public void setView(ExpandableView expandableView) {
            this.mView = expandableView;
        }

        public void setHeight(float f) {
            this.mView.setActualHeight((int) f);
            ExpandHelper.this.mCurrentHeight = f;
        }

        public float getHeight() {
            return this.mView.getActualHeight();
        }

        public int getNaturalHeight() {
            return ExpandHelper.this.mCallback.getMaxExpandHeight(this.mView);
        }
    }

    public ExpandHelper(Context context, Callback callback, int i, int i2) {
        this.mSmallSize = i;
        this.mMaximumStretch = this.mSmallSize * 2.0f;
        this.mLargeSize = i2;
        this.mContext = context;
        this.mCallback = callback;
        this.mPullGestureMinXSpan = this.mContext.getResources().getDimension(R.dimen.pull_span_min);
        this.mTouchSlop = ViewConfiguration.get(this.mContext).getScaledTouchSlop();
        this.mSGD = new ScaleGestureDetector(context, this.mScaleGestureListener);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, 0.3f);
    }

    @VisibleForTesting
    void updateExpansion() {
        float currentSpan = (this.mSGD.getCurrentSpan() - this.mInitialTouchSpan) * 1.0f;
        float focusY = (this.mSGD.getFocusY() - this.mInitialTouchFocusY) * 1.0f * (this.mGravity == 80 ? -1.0f : 1.0f);
        float fAbs = Math.abs(focusY) + Math.abs(currentSpan) + 1.0f;
        this.mScaler.setHeight(clamp(((focusY * Math.abs(focusY)) / fAbs) + ((currentSpan * Math.abs(currentSpan)) / fAbs) + this.mOldHeight));
        this.mLastFocusY = this.mSGD.getFocusY();
        this.mLastSpanY = this.mSGD.getCurrentSpan();
    }

    private float clamp(float f) {
        if (f < this.mSmallSize) {
            f = this.mSmallSize;
        }
        return f > this.mNaturalHeight ? this.mNaturalHeight : f;
    }

    private ExpandableView findView(float f, float f2) {
        if (this.mEventSource != null) {
            this.mEventSource.getLocationOnScreen(new int[2]);
            return this.mCallback.getChildAtRawPosition(f + r0[0], f2 + r0[1]);
        }
        return this.mCallback.getChildAtPosition(f, f2);
    }

    private boolean isInside(View view, float f, float f2) {
        if (view == null) {
            return false;
        }
        if (this.mEventSource != null) {
            this.mEventSource.getLocationOnScreen(new int[2]);
            f += r1[0];
            f2 += r1[1];
        }
        view.getLocationOnScreen(new int[2]);
        float f3 = f - r1[0];
        float f4 = f2 - r1[1];
        if (f3 <= 0.0f || f4 <= 0.0f) {
            return false;
        }
        return ((f4 > ((float) view.getHeight()) ? 1 : (f4 == ((float) view.getHeight()) ? 0 : -1)) < 0) & ((f3 > ((float) view.getWidth()) ? 1 : (f3 == ((float) view.getWidth()) ? 0 : -1)) < 0);
    }

    public void setEventSource(View view) {
        this.mEventSource = view;
    }

    public void setScrollAdapter(ScrollAdapter scrollAdapter) {
        this.mScrollAdapter = scrollAdapter;
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled()) {
            return false;
        }
        trackVelocity(motionEvent);
        int action = motionEvent.getAction();
        this.mSGD.onTouchEvent(motionEvent);
        int focusX = (int) this.mSGD.getFocusX();
        float focusY = (int) this.mSGD.getFocusY();
        this.mInitialTouchFocusY = focusY;
        this.mInitialTouchSpan = this.mSGD.getCurrentSpan();
        this.mLastFocusY = this.mInitialTouchFocusY;
        this.mLastSpanY = this.mInitialTouchSpan;
        if (this.mExpanding) {
            this.mLastMotionY = motionEvent.getRawY();
            maybeRecycleVelocityTracker(motionEvent);
            return true;
        }
        if (action == 2 && (this.mExpansionStyle & 1) != 0) {
            return true;
        }
        switch (action & 255) {
            case 0:
                this.mWatchingForPull = this.mScrollAdapter != null && isInside(this.mScrollAdapter.getHostView(), (float) focusX, focusY) && this.mScrollAdapter.isScrolledToTop();
                this.mResizedView = findView(focusX, focusY);
                if (this.mResizedView != null && !this.mCallback.canChildBeExpanded(this.mResizedView)) {
                    this.mResizedView = null;
                    this.mWatchingForPull = false;
                }
                this.mInitialTouchY = motionEvent.getRawY();
                this.mInitialTouchX = motionEvent.getRawX();
                break;
            case 1:
            case 3:
                finishExpanding(motionEvent.getActionMasked() == 3, getCurrentVelocity());
                clearView();
                break;
            case 2:
                float currentSpanX = this.mSGD.getCurrentSpanX();
                if (currentSpanX > this.mPullGestureMinXSpan && currentSpanX > this.mSGD.getCurrentSpanY() && !this.mExpanding) {
                    startExpanding(this.mResizedView, 2);
                    this.mWatchingForPull = false;
                }
                if (this.mWatchingForPull) {
                    float rawY = motionEvent.getRawY() - this.mInitialTouchY;
                    float rawX = motionEvent.getRawX() - this.mInitialTouchX;
                    if (rawY > this.mTouchSlop && rawY > Math.abs(rawX)) {
                        this.mWatchingForPull = false;
                        if (this.mResizedView != null && !isFullyExpanded(this.mResizedView) && startExpanding(this.mResizedView, 1)) {
                            this.mLastMotionY = motionEvent.getRawY();
                            this.mInitialTouchY = motionEvent.getRawY();
                            this.mHasPopped = false;
                            break;
                        }
                    }
                }
                break;
        }
        this.mLastMotionY = motionEvent.getRawY();
        maybeRecycleVelocityTracker(motionEvent);
        return this.mExpanding;
    }

    private void trackVelocity(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 0) {
            if (this.mVelocityTracker == null) {
                this.mVelocityTracker = VelocityTracker.obtain();
            } else {
                this.mVelocityTracker.clear();
            }
            this.mVelocityTracker.addMovement(motionEvent);
            return;
        }
        if (actionMasked == 2) {
            if (this.mVelocityTracker == null) {
                this.mVelocityTracker = VelocityTracker.obtain();
            }
            this.mVelocityTracker.addMovement(motionEvent);
        }
    }

    private void maybeRecycleVelocityTracker(MotionEvent motionEvent) {
        if (this.mVelocityTracker != null) {
            if (motionEvent.getActionMasked() == 3 || motionEvent.getActionMasked() == 1) {
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
            }
        }
    }

    private float getCurrentVelocity() {
        if (this.mVelocityTracker != null) {
            this.mVelocityTracker.computeCurrentVelocity(1000);
            return this.mVelocityTracker.getYVelocity();
        }
        return 0.0f;
    }

    public void setEnabled(boolean z) {
        this.mEnabled = z;
    }

    private boolean isEnabled() {
        return this.mEnabled;
    }

    private boolean isFullyExpanded(ExpandableView expandableView) {
        return expandableView.getIntrinsicHeight() == expandableView.getMaxContentHeight() && (!expandableView.isSummaryWithChildren() || expandableView.areChildrenExpanded());
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled() && !this.mExpanding) {
            return false;
        }
        trackVelocity(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        this.mSGD.onTouchEvent(motionEvent);
        int focusX = (int) this.mSGD.getFocusX();
        int focusY = (int) this.mSGD.getFocusY();
        if (this.mOnlyMovements) {
            this.mLastMotionY = motionEvent.getRawY();
            return false;
        }
        switch (actionMasked) {
            case 0:
                this.mWatchingForPull = this.mScrollAdapter != null && isInside(this.mScrollAdapter.getHostView(), (float) focusX, (float) focusY);
                this.mResizedView = findView(focusX, focusY);
                this.mInitialTouchX = motionEvent.getRawX();
                this.mInitialTouchY = motionEvent.getRawY();
                break;
            case 1:
            case 3:
                finishExpanding(!isEnabled() || motionEvent.getActionMasked() == 3, getCurrentVelocity());
                clearView();
                break;
            case 2:
                if (this.mWatchingForPull) {
                    float rawY = motionEvent.getRawY() - this.mInitialTouchY;
                    float rawX = motionEvent.getRawX() - this.mInitialTouchX;
                    if (rawY > this.mTouchSlop && rawY > Math.abs(rawX)) {
                        this.mWatchingForPull = false;
                        if (this.mResizedView != null && !isFullyExpanded(this.mResizedView) && startExpanding(this.mResizedView, 1)) {
                            this.mInitialTouchY = motionEvent.getRawY();
                            this.mLastMotionY = motionEvent.getRawY();
                            this.mHasPopped = false;
                        }
                    }
                }
                if (this.mExpanding && (this.mExpansionStyle & 1) != 0) {
                    float rawY2 = (motionEvent.getRawY() - this.mLastMotionY) + this.mCurrentHeight;
                    float fClamp = clamp(rawY2);
                    boolean z = rawY2 > this.mNaturalHeight;
                    if (rawY2 < this.mSmallSize) {
                        z = true;
                    }
                    if (!this.mHasPopped) {
                        if (this.mEventSource != null) {
                            this.mEventSource.performHapticFeedback(1);
                        }
                        this.mHasPopped = true;
                    }
                    this.mScaler.setHeight(fClamp);
                    this.mLastMotionY = motionEvent.getRawY();
                    if (!z) {
                        this.mCallback.expansionStateChanged(true);
                    } else {
                        this.mCallback.expansionStateChanged(false);
                    }
                    return true;
                }
                if (this.mExpanding) {
                    updateExpansion();
                    this.mLastMotionY = motionEvent.getRawY();
                    return true;
                }
                break;
            case 5:
            case 6:
                this.mInitialTouchY += this.mSGD.getFocusY() - this.mLastFocusY;
                this.mInitialTouchSpan += this.mSGD.getCurrentSpan() - this.mLastSpanY;
                break;
        }
        this.mLastMotionY = motionEvent.getRawY();
        maybeRecycleVelocityTracker(motionEvent);
        return this.mResizedView != null;
    }

    @VisibleForTesting
    boolean startExpanding(ExpandableView expandableView, int i) {
        if (!(expandableView instanceof ExpandableNotificationRow)) {
            return false;
        }
        this.mExpansionStyle = i;
        if (this.mExpanding && expandableView == this.mResizedView) {
            return true;
        }
        this.mExpanding = true;
        this.mCallback.expansionStateChanged(true);
        this.mCallback.setUserLockedChild(expandableView, true);
        this.mScaler.setView(expandableView);
        this.mOldHeight = this.mScaler.getHeight();
        this.mCurrentHeight = this.mOldHeight;
        if (this.mCallback.canChildBeExpanded(expandableView)) {
            this.mNaturalHeight = this.mScaler.getNaturalHeight();
            this.mSmallSize = expandableView.getCollapsedHeight();
        } else {
            this.mNaturalHeight = this.mOldHeight;
        }
        return true;
    }

    @VisibleForTesting
    void finishExpanding(boolean z, float f) {
        finishExpanding(z, f, true);
    }

    private void finishExpanding(boolean z, float f, boolean z2) {
        final boolean z3;
        if (this.mExpanding) {
            float height = this.mScaler.getHeight();
            boolean z4 = this.mOldHeight == ((float) this.mSmallSize);
            if (!z) {
                z3 = (!z4 ? !(height >= this.mOldHeight || f > 0.0f) : !(height > this.mOldHeight && f >= 0.0f)) | (this.mNaturalHeight == ((float) this.mSmallSize));
            } else {
                z3 = !z4;
            }
            if (this.mScaleAnimation.isRunning()) {
                this.mScaleAnimation.cancel();
            }
            this.mCallback.expansionStateChanged(false);
            int naturalHeight = this.mScaler.getNaturalHeight();
            if (!z3) {
                naturalHeight = this.mSmallSize;
            }
            float f2 = naturalHeight;
            if (f2 != height && this.mEnabled && z2) {
                this.mScaleAnimation.setFloatValues(f2);
                this.mScaleAnimation.setupStartValues();
                final ExpandableView expandableView = this.mResizedView;
                this.mScaleAnimation.addListener(new AnimatorListenerAdapter() { // from class: com.android.systemui.ExpandHelper.2
                    public boolean mCancelled;

                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        if (!this.mCancelled) {
                            ExpandHelper.this.mCallback.setUserExpandedChild(expandableView, z3);
                            if (!ExpandHelper.this.mExpanding) {
                                ExpandHelper.this.mScaler.setView(null);
                            }
                        } else {
                            ExpandHelper.this.mCallback.setExpansionCancelled(expandableView);
                        }
                        ExpandHelper.this.mCallback.setUserLockedChild(expandableView, false);
                        ExpandHelper.this.mScaleAnimation.removeListener(this);
                    }

                    @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animator) {
                        this.mCancelled = true;
                    }
                });
                if (z3 != (f >= 0.0f)) {
                    f = 0.0f;
                }
                this.mFlingAnimationUtils.apply(this.mScaleAnimation, height, f2, f);
                this.mScaleAnimation.start();
            } else {
                if (f2 != height) {
                    this.mScaler.setHeight(f2);
                }
                this.mCallback.setUserExpandedChild(this.mResizedView, z3);
                this.mCallback.setUserLockedChild(this.mResizedView, false);
                this.mScaler.setView(null);
            }
            this.mExpanding = false;
            this.mExpansionStyle = 0;
        }
    }

    private void clearView() {
        this.mResizedView = null;
    }

    public void cancelImmediately() {
        cancel(false);
    }

    public void cancel() {
        cancel(true);
    }

    private void cancel(boolean z) {
        finishExpanding(true, 0.0f, z);
        clearView();
        this.mSGD = new ScaleGestureDetector(this.mContext, this.mScaleGestureListener);
    }

    public void onlyObserveMovements(boolean z) {
        this.mOnlyMovements = z;
    }
}
