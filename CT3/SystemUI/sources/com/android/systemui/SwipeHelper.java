package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.util.Property;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.FlingAnimationUtils;
import java.util.HashMap;

public class SwipeHelper {
    private Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    private View mCurrView;
    private float mDensityScale;
    private boolean mDisableHwLayers;
    private boolean mDragging;
    private FalsingManager mFalsingManager;
    private int mFalsingThreshold;
    private FlingAnimationUtils mFlingAnimationUtils;
    private float mInitialTouchPos;
    private LongPressListener mLongPressListener;
    private boolean mLongPressSent;
    private float mPagingTouchSlop;
    private float mPerpendicularInitialTouchPos;
    private boolean mSnappingChild;
    private int mSwipeDirection;
    private boolean mTouchAboveFalsingThreshold;
    private Runnable mWatchLongPress;
    private float SWIPE_ESCAPE_VELOCITY = 100.0f;
    private int DEFAULT_ESCAPE_ANIMATION_DURATION = 200;
    private int MAX_ESCAPE_ANIMATION_DURATION = 400;
    private int MAX_DISMISS_VELOCITY = 4000;
    private float mMinSwipeProgress = 0.0f;
    private float mMaxSwipeProgress = 1.0f;
    private float mTranslation = 0.0f;
    private final int[] mTmpPos = new int[2];
    private HashMap<View, Animator> mDismissPendingMap = new HashMap<>();
    private Handler mHandler = new Handler();
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private long mLongPressTimeout = (long) (ViewConfiguration.getLongPressTimeout() * 1.5f);

    public interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        float getFalsingThresholdFactor();

        boolean isAntiFalsingNeeded();

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onChildSnappedBack(View view, float f);

        void onDragCancelled(View view);

        boolean updateSwipeProgress(View view, boolean z, float f);
    }

    public interface LongPressListener {
        boolean onLongPress(View view, int i, int i2);
    }

    public SwipeHelper(int swipeDirection, Callback callback, Context context) {
        this.mCallback = callback;
        this.mSwipeDirection = swipeDirection;
        this.mDensityScale = context.getResources().getDisplayMetrics().density;
        this.mPagingTouchSlop = ViewConfiguration.get(context).getScaledPagingTouchSlop();
        this.mFalsingThreshold = context.getResources().getDimensionPixelSize(R.dimen.swipe_helper_falsing_threshold);
        this.mFalsingManager = FalsingManager.getInstance(context);
        this.mFlingAnimationUtils = new FlingAnimationUtils(context, getMaxEscapeAnimDuration() / 1000.0f);
    }

    public void setLongPressListener(LongPressListener listener) {
        this.mLongPressListener = listener;
    }

    public void setDensityScale(float densityScale) {
        this.mDensityScale = densityScale;
    }

    public void setPagingTouchSlop(float pagingTouchSlop) {
        this.mPagingTouchSlop = pagingTouchSlop;
    }

    public void setDisableHardwareLayers(boolean disableHwLayers) {
        this.mDisableHwLayers = disableHwLayers;
    }

    private float getPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getX() : ev.getY();
    }

    private float getPerpendicularPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getY() : ev.getX();
    }

    protected float getTranslation(View v) {
        return this.mSwipeDirection == 0 ? v.getTranslationX() : v.getTranslationY();
    }

    private float getVelocity(VelocityTracker vt) {
        return this.mSwipeDirection == 0 ? vt.getXVelocity() : vt.getYVelocity();
    }

    protected ObjectAnimator createTranslationAnimation(View v, float newPos) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, (Property<View, Float>) (this.mSwipeDirection == 0 ? View.TRANSLATION_X : View.TRANSLATION_Y), newPos);
        return anim;
    }

    protected Animator getViewTranslationAnimator(View v, float target, ValueAnimator.AnimatorUpdateListener listener) {
        ObjectAnimator anim = createTranslationAnimation(v, target);
        if (listener != null) {
            anim.addUpdateListener(listener);
        }
        return anim;
    }

    protected void setTranslation(View v, float translate) {
        if (v == null) {
            return;
        }
        if (this.mSwipeDirection == 0) {
            v.setTranslationX(translate);
        } else {
            v.setTranslationY(translate);
        }
    }

    protected float getSize(View v) {
        return this.mSwipeDirection == 0 ? v.getMeasuredWidth() : v.getMeasuredHeight();
    }

    private float getSwipeProgressForOffset(View view, float translation) {
        float viewSize = getSize(view);
        float result = Math.abs(translation / viewSize);
        return Math.min(Math.max(this.mMinSwipeProgress, result), this.mMaxSwipeProgress);
    }

    private float getSwipeAlpha(float progress) {
        return Math.min(0.0f, Math.max(1.0f, progress / 0.5f));
    }

    public void updateSwipeProgressFromOffset(View animView, boolean dismissable) {
        updateSwipeProgressFromOffset(animView, dismissable, getTranslation(animView));
    }

    private void updateSwipeProgressFromOffset(View animView, boolean dismissable, float translation) {
        float swipeProgress = getSwipeProgressForOffset(animView, translation);
        if (!this.mCallback.updateSwipeProgress(animView, dismissable, swipeProgress) && dismissable) {
            if (!this.mDisableHwLayers) {
                if (swipeProgress != 0.0f && swipeProgress != 1.0f) {
                    animView.setLayerType(2, null);
                } else {
                    animView.setLayerType(0, null);
                }
            }
            animView.setAlpha(getSwipeAlpha(swipeProgress));
        }
        invalidateGlobalRegion(animView);
    }

    public static void invalidateGlobalRegion(View view) {
        invalidateGlobalRegion(view, new RectF(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
    }

    public static void invalidateGlobalRegion(View view, RectF childBounds) {
        while (view.getParent() != null && (view.getParent() instanceof View)) {
            view = (View) view.getParent();
            view.getMatrix().mapRect(childBounds);
            view.invalidate((int) Math.floor(childBounds.left), (int) Math.floor(childBounds.top), (int) Math.ceil(childBounds.right), (int) Math.ceil(childBounds.bottom));
        }
    }

    public void removeLongPressCallback() {
        if (this.mWatchLongPress == null) {
            return;
        }
        this.mHandler.removeCallbacks(this.mWatchLongPress);
        this.mWatchLongPress = null;
    }

    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case 0:
                this.mTouchAboveFalsingThreshold = false;
                this.mDragging = false;
                this.mSnappingChild = false;
                this.mLongPressSent = false;
                this.mVelocityTracker.clear();
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                if (this.mCurrView != null) {
                    onDownUpdate(this.mCurrView);
                    this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                    this.mVelocityTracker.addMovement(ev);
                    this.mInitialTouchPos = getPos(ev);
                    this.mPerpendicularInitialTouchPos = getPerpendicularPos(ev);
                    this.mTranslation = getTranslation(this.mCurrView);
                    if (this.mLongPressListener != null) {
                        if (this.mWatchLongPress == null) {
                            this.mWatchLongPress = new Runnable() {
                                @Override
                                public void run() {
                                    if (SwipeHelper.this.mCurrView == null || SwipeHelper.this.mLongPressSent) {
                                        return;
                                    }
                                    SwipeHelper.this.mLongPressSent = true;
                                    SwipeHelper.this.mCurrView.sendAccessibilityEvent(2);
                                    SwipeHelper.this.mCurrView.getLocationOnScreen(SwipeHelper.this.mTmpPos);
                                    int x = ((int) ev.getRawX()) - SwipeHelper.this.mTmpPos[0];
                                    int y = ((int) ev.getRawY()) - SwipeHelper.this.mTmpPos[1];
                                    SwipeHelper.this.mLongPressListener.onLongPress(SwipeHelper.this.mCurrView, x, y);
                                }
                            };
                        }
                        this.mHandler.postDelayed(this.mWatchLongPress, this.mLongPressTimeout);
                    }
                }
                break;
            case 1:
            case 3:
                boolean z = !this.mDragging ? this.mLongPressSent : true;
                this.mDragging = false;
                this.mCurrView = null;
                this.mLongPressSent = false;
                removeLongPressCallback();
                if (z) {
                    return true;
                }
                break;
            case 2:
                if (this.mCurrView != null && !this.mLongPressSent) {
                    this.mVelocityTracker.addMovement(ev);
                    float pos = getPos(ev);
                    float perpendicularPos = getPerpendicularPos(ev);
                    float delta = pos - this.mInitialTouchPos;
                    float deltaPerpendicular = perpendicularPos - this.mPerpendicularInitialTouchPos;
                    if (Math.abs(delta) > this.mPagingTouchSlop && Math.abs(delta) > Math.abs(deltaPerpendicular)) {
                        this.mCallback.onBeginDrag(this.mCurrView);
                        this.mDragging = true;
                        this.mInitialTouchPos = getPos(ev);
                        this.mTranslation = getTranslation(this.mCurrView);
                        removeLongPressCallback();
                    }
                }
                break;
        }
        if (this.mDragging) {
            return true;
        }
        return this.mLongPressSent;
    }

    public void dismissChild(View view, float velocity, boolean useAccelerateInterpolator) {
        dismissChild(view, velocity, null, 0L, useAccelerateInterpolator, 0L, false);
    }

    public void dismissChild(final View animView, float velocity, final Runnable endAction, long delay, boolean useAccelerateInterpolator, long fixedDuration, boolean isDismissAll) {
        boolean z;
        boolean animateLeft;
        float newPos;
        long duration;
        final boolean canBeDismissed = this.mCallback.canChildBeDismissed(animView);
        boolean isLayoutRtl = animView.getLayoutDirection() == 1;
        boolean animateUpForMenu = velocity == 0.0f && (getTranslation(animView) == 0.0f || isDismissAll) && this.mSwipeDirection == 1;
        if (velocity != 0.0f || (getTranslation(animView) != 0.0f && !isDismissAll)) {
            z = false;
        } else {
            z = isLayoutRtl;
        }
        if (velocity < 0.0f) {
            animateLeft = true;
        } else if (velocity != 0.0f || getTranslation(animView) >= 0.0f) {
            animateLeft = false;
        } else {
            animateLeft = !isDismissAll;
        }
        if (animateLeft || z || animateUpForMenu) {
            newPos = -getSize(animView);
        } else {
            newPos = getSize(animView);
        }
        if (fixedDuration == 0) {
            long duration2 = this.MAX_ESCAPE_ANIMATION_DURATION;
            if (velocity != 0.0f) {
                duration = Math.min(duration2, (int) ((Math.abs(newPos - getTranslation(animView)) * 1000.0f) / Math.abs(velocity)));
            } else {
                duration = this.DEFAULT_ESCAPE_ANIMATION_DURATION;
            }
        } else {
            duration = fixedDuration;
        }
        if (!this.mDisableHwLayers) {
            animView.setLayerType(2, null);
        }
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.onTranslationUpdate(animView, ((Float) animation.getAnimatedValue()).floatValue(), canBeDismissed);
            }
        };
        Animator anim = getViewTranslationAnimator(animView, newPos, updateListener);
        if (anim == null) {
            return;
        }
        if (useAccelerateInterpolator) {
            anim.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
            anim.setDuration(duration);
        } else {
            this.mFlingAnimationUtils.applyDismissing(anim, getTranslation(animView), newPos, velocity, getSize(animView));
        }
        if (delay > 0) {
            anim.setStartDelay(delay);
        }
        anim.addListener(new AnimatorListenerAdapter() {
            private boolean mCancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                this.mCancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                SwipeHelper.this.updateSwipeProgressFromOffset(animView, canBeDismissed);
                SwipeHelper.this.mDismissPendingMap.remove(animView);
                if (!this.mCancelled) {
                    SwipeHelper.this.mCallback.onChildDismissed(animView);
                }
                if (endAction != null) {
                    endAction.run();
                }
                if (SwipeHelper.this.mDisableHwLayers) {
                    return;
                }
                animView.setLayerType(0, null);
            }
        });
        prepareDismissAnimation(animView, anim);
        this.mDismissPendingMap.put(animView, anim);
        anim.start();
    }

    protected void prepareDismissAnimation(View view, Animator anim) {
    }

    public void snapChild(final View animView, final float targetLeft, float velocity) {
        final boolean canBeDismissed = this.mCallback.canChildBeDismissed(animView);
        ValueAnimator.AnimatorUpdateListener updateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.onTranslationUpdate(animView, ((Float) animation.getAnimatedValue()).floatValue(), canBeDismissed);
            }
        };
        Animator anim = getViewTranslationAnimator(animView, targetLeft, updateListener);
        if (anim == null) {
            return;
        }
        anim.setDuration(150L);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                SwipeHelper.this.mSnappingChild = false;
                SwipeHelper.this.updateSwipeProgressFromOffset(animView, canBeDismissed);
                SwipeHelper.this.mCallback.onChildSnappedBack(animView, targetLeft);
            }
        });
        prepareSnapBackAnimation(animView, anim);
        this.mSnappingChild = true;
        anim.start();
    }

    protected void prepareSnapBackAnimation(View view, Animator anim) {
    }

    public void onDownUpdate(View currView) {
    }

    protected void onMoveUpdate(View view, float totalTranslation, float delta) {
    }

    public void onTranslationUpdate(View animView, float value, boolean canBeDismissed) {
        updateSwipeProgressFromOffset(animView, canBeDismissed, value);
    }

    private void snapChildInstantly(View view) {
        boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        setTranslation(view, 0.0f);
        updateSwipeProgressFromOffset(view, canAnimViewBeDismissed);
    }

    public void snapChildIfNeeded(View view, boolean animate, float targetLeft) {
        if ((this.mDragging && this.mCurrView == view) || this.mSnappingChild) {
            return;
        }
        boolean needToSnap = false;
        Animator dismissPendingAnim = this.mDismissPendingMap.get(view);
        if (dismissPendingAnim != null) {
            needToSnap = true;
            dismissPendingAnim.cancel();
        } else if (getTranslation(view) != 0.0f) {
            needToSnap = true;
        }
        if (!needToSnap) {
            return;
        }
        if (animate) {
            snapChild(view, targetLeft, 0.0f);
        } else {
            snapChildInstantly(view);
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mLongPressSent) {
            return true;
        }
        if (!this.mDragging) {
            if (this.mCallback.getChildAtPosition(ev) != null) {
                onInterceptTouchEvent(ev);
                return true;
            }
            removeLongPressCallback();
            return false;
        }
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getAction();
        switch (action) {
            case 1:
            case 3:
                if (this.mCurrView != null) {
                    this.mVelocityTracker.computeCurrentVelocity(1000, getMaxVelocity());
                    float velocity = getVelocity(this.mVelocityTracker);
                    if (!handleUpEvent(ev, this.mCurrView, velocity, getTranslation(this.mCurrView))) {
                        if (isDismissGesture(ev)) {
                            dismissChild(this.mCurrView, velocity, !swipedFastEnough());
                        } else {
                            this.mCallback.onDragCancelled(this.mCurrView);
                            snapChild(this.mCurrView, 0.0f, velocity);
                        }
                        this.mCurrView = null;
                    }
                    this.mDragging = false;
                }
                return true;
            case 2:
            case 4:
                if (this.mCurrView != null) {
                    float delta = getPos(ev) - this.mInitialTouchPos;
                    float absDelta = Math.abs(delta);
                    if (absDelta >= getFalsingThreshold()) {
                        this.mTouchAboveFalsingThreshold = true;
                    }
                    if (!this.mCallback.canChildBeDismissed(this.mCurrView)) {
                        float size = getSize(this.mCurrView);
                        float maxScrollDistance = 0.25f * size;
                        if (absDelta >= size) {
                            delta = delta > 0.0f ? maxScrollDistance : -maxScrollDistance;
                        } else {
                            delta = maxScrollDistance * ((float) Math.sin(((double) (delta / size)) * 1.5707963267948966d));
                        }
                    }
                    setTranslation(this.mCurrView, this.mTranslation + delta);
                    updateSwipeProgressFromOffset(this.mCurrView, this.mCanCurrViewBeDimissed);
                    onMoveUpdate(this.mCurrView, this.mTranslation + delta, delta);
                }
                return true;
            default:
                return true;
        }
    }

    private int getFalsingThreshold() {
        float factor = this.mCallback.getFalsingThresholdFactor();
        return (int) (this.mFalsingThreshold * factor);
    }

    private float getMaxVelocity() {
        return this.MAX_DISMISS_VELOCITY * this.mDensityScale;
    }

    protected float getEscapeVelocity() {
        return getUnscaledEscapeVelocity() * this.mDensityScale;
    }

    protected float getUnscaledEscapeVelocity() {
        return this.SWIPE_ESCAPE_VELOCITY;
    }

    protected long getMaxEscapeAnimDuration() {
        return this.MAX_ESCAPE_ANIMATION_DURATION;
    }

    protected boolean swipedFarEnough() {
        float translation = getTranslation(this.mCurrView);
        return ((double) Math.abs(translation)) > ((double) getSize(this.mCurrView)) * 0.4d;
    }

    protected boolean isDismissGesture(MotionEvent ev) {
        boolean falsingDetected;
        boolean falsingDetected2 = this.mCallback.isAntiFalsingNeeded();
        if (this.mFalsingManager.isClassiferEnabled()) {
            falsingDetected = falsingDetected2 ? this.mFalsingManager.isFalseTouch() : false;
        } else {
            falsingDetected = falsingDetected2 && !this.mTouchAboveFalsingThreshold;
        }
        if (falsingDetected || ((!swipedFastEnough() && !swipedFarEnough()) || ev.getActionMasked() != 1)) {
            return false;
        }
        return this.mCallback.canChildBeDismissed(this.mCurrView);
    }

    protected boolean swipedFastEnough() {
        float velocity = getVelocity(this.mVelocityTracker);
        float translation = getTranslation(this.mCurrView);
        if (Math.abs(velocity) > getEscapeVelocity()) {
            return ((velocity > 0.0f ? 1 : (velocity == 0.0f ? 0 : -1)) > 0) == ((translation > 0.0f ? 1 : (translation == 0.0f ? 0 : -1)) > 0);
        }
        return false;
    }

    protected boolean handleUpEvent(MotionEvent ev, View animView, float velocity, float translation) {
        return false;
    }
}
