package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

public class SwipeHelper {
    private Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    private View mCurrAnimView;
    private View mCurrView;
    private float mDensityScale;
    private boolean mDragging;
    private int mFalsingThreshold;
    private final Interpolator mFastOutLinearInInterpolator;
    private float mInitialTouchPos;
    private LongPressListener mLongPressListener;
    private boolean mLongPressSent;
    private float mPagingTouchSlop;
    private int mSwipeDirection;
    private boolean mTouchAboveFalsingThreshold;
    private Runnable mWatchLongPress;
    private static LinearInterpolator sLinearInterpolator = new LinearInterpolator();
    public static float SWIPE_PROGRESS_FADE_START = 0.0f;
    private float SWIPE_ESCAPE_VELOCITY = 100.0f;
    private int DEFAULT_ESCAPE_ANIMATION_DURATION = 200;
    private int MAX_ESCAPE_ANIMATION_DURATION = 400;
    private int MAX_DISMISS_VELOCITY = 2000;
    private float mMinSwipeProgress = 0.0f;
    private float mMaxSwipeProgress = 1.0f;
    private final int[] mTmpPos = new int[2];
    private Handler mHandler = new Handler();
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private long mLongPressTimeout = (long) (ViewConfiguration.getLongPressTimeout() * 1.5f);

    public interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        View getChildContentView(View view);

        float getFalsingThresholdFactor();

        boolean isAntiFalsingNeeded();

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onChildSnappedBack(View view);

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
        this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_linear_in);
        this.mFalsingThreshold = context.getResources().getDimensionPixelSize(R.dimen.swipe_helper_falsing_threshold);
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

    private float getPos(MotionEvent ev) {
        return this.mSwipeDirection == 0 ? ev.getX() : ev.getY();
    }

    private float getTranslation(View v) {
        return this.mSwipeDirection == 0 ? v.getTranslationX() : v.getTranslationY();
    }

    private float getVelocity(VelocityTracker vt) {
        return this.mSwipeDirection == 0 ? vt.getXVelocity() : vt.getYVelocity();
    }

    private ObjectAnimator createTranslationAnimation(View v, float newPos) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, this.mSwipeDirection == 0 ? "translationX" : "translationY", newPos);
        return anim;
    }

    private float getPerpendicularVelocity(VelocityTracker vt) {
        return this.mSwipeDirection == 0 ? vt.getYVelocity() : vt.getXVelocity();
    }

    private void setTranslation(View v, float translate) {
        if (this.mSwipeDirection == 0) {
            v.setTranslationX(translate);
        } else {
            v.setTranslationY(translate);
        }
    }

    private float getSize(View v) {
        return this.mSwipeDirection == 0 ? v.getMeasuredWidth() : v.getMeasuredHeight();
    }

    public void setMinSwipeProgress(float minSwipeProgress) {
        this.mMinSwipeProgress = minSwipeProgress;
    }

    public void setMaxSwipeProgress(float maxSwipeProgress) {
        this.mMaxSwipeProgress = maxSwipeProgress;
    }

    private float getSwipeProgressForOffset(View view) {
        float viewSize = getSize(view);
        float fadeSize = 0.5f * viewSize;
        float result = 1.0f;
        float pos = getTranslation(view);
        if (pos >= SWIPE_PROGRESS_FADE_START * viewSize) {
            result = 1.0f - ((pos - (SWIPE_PROGRESS_FADE_START * viewSize)) / fadeSize);
        } else if (pos < (1.0f - SWIPE_PROGRESS_FADE_START) * viewSize) {
            result = 1.0f + (((SWIPE_PROGRESS_FADE_START * viewSize) + pos) / fadeSize);
        }
        return Math.min(Math.max(this.mMinSwipeProgress, result), this.mMaxSwipeProgress);
    }

    public void updateSwipeProgressFromOffset(View animView, boolean dismissable) {
        float swipeProgress = getSwipeProgressForOffset(animView);
        if (!this.mCallback.updateSwipeProgress(animView, dismissable, swipeProgress) && dismissable) {
            if (swipeProgress != 0.0f && swipeProgress != 1.0f) {
                animView.setLayerType(2, null);
            } else {
                animView.setLayerType(0, null);
            }
            animView.setAlpha(getSwipeProgressForOffset(animView));
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
        if (this.mWatchLongPress != null) {
            this.mHandler.removeCallbacks(this.mWatchLongPress);
            this.mWatchLongPress = null;
        }
    }

    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case 0:
                this.mTouchAboveFalsingThreshold = false;
                this.mDragging = false;
                this.mLongPressSent = false;
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                this.mVelocityTracker.clear();
                if (this.mCurrView != null) {
                    this.mCurrAnimView = this.mCallback.getChildContentView(this.mCurrView);
                    this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                    this.mVelocityTracker.addMovement(ev);
                    this.mInitialTouchPos = getPos(ev);
                    if (this.mLongPressListener != null) {
                        if (this.mWatchLongPress == null) {
                            this.mWatchLongPress = new Runnable() {
                                @Override
                                public void run() {
                                    if (SwipeHelper.this.mCurrView != null && !SwipeHelper.this.mLongPressSent) {
                                        SwipeHelper.this.mLongPressSent = true;
                                        SwipeHelper.this.mCurrView.sendAccessibilityEvent(2);
                                        SwipeHelper.this.mCurrView.getLocationOnScreen(SwipeHelper.this.mTmpPos);
                                        int x = ((int) ev.getRawX()) - SwipeHelper.this.mTmpPos[0];
                                        int y = ((int) ev.getRawY()) - SwipeHelper.this.mTmpPos[1];
                                        SwipeHelper.this.mLongPressListener.onLongPress(SwipeHelper.this.mCurrView, x, y);
                                    }
                                }
                            };
                        }
                        this.mHandler.postDelayed(this.mWatchLongPress, this.mLongPressTimeout);
                    }
                }
                break;
            case 1:
            case 3:
                boolean captured = this.mDragging || this.mLongPressSent;
                this.mDragging = false;
                this.mCurrView = null;
                this.mCurrAnimView = null;
                this.mLongPressSent = false;
                removeLongPressCallback();
                if (captured) {
                    return true;
                }
                break;
            case 2:
                if (this.mCurrView != null && !this.mLongPressSent) {
                    this.mVelocityTracker.addMovement(ev);
                    float pos = getPos(ev);
                    float delta = pos - this.mInitialTouchPos;
                    if (Math.abs(delta) > this.mPagingTouchSlop) {
                        this.mCallback.onBeginDrag(this.mCurrView);
                        this.mDragging = true;
                        this.mInitialTouchPos = getPos(ev) - getTranslation(this.mCurrAnimView);
                        removeLongPressCallback();
                    }
                }
                break;
        }
        return this.mDragging || this.mLongPressSent;
    }

    public void dismissChild(View view, float velocity) {
        dismissChild(view, velocity, null, 0L, false, 0L);
    }

    public void dismissChild(final View view, float velocity, final Runnable endAction, long delay, boolean useAccelerateInterpolator, long fixedDuration) {
        float newPos;
        long duration;
        final View animView = this.mCallback.getChildContentView(view);
        final boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        boolean isLayoutRtl = view.getLayoutDirection() == 1;
        if (velocity < 0.0f || ((velocity == 0.0f && getTranslation(animView) < 0.0f) || ((velocity == 0.0f && getTranslation(animView) == 0.0f && this.mSwipeDirection == 1) || (velocity == 0.0f && getTranslation(animView) == 0.0f && isLayoutRtl)))) {
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
        animView.setLayerType(2, null);
        ObjectAnimator anim = createTranslationAnimation(animView, newPos);
        if (useAccelerateInterpolator) {
            anim.setInterpolator(this.mFastOutLinearInInterpolator);
        } else {
            anim.setInterpolator(sLinearInterpolator);
        }
        anim.setDuration(duration);
        if (delay > 0) {
            anim.setStartDelay(delay);
        }
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SwipeHelper.this.mCallback.onChildDismissed(view);
                if (endAction != null) {
                    endAction.run();
                }
                animView.setLayerType(0, null);
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.updateSwipeProgressFromOffset(animView, canAnimViewBeDismissed);
            }
        });
        anim.start();
    }

    public void snapChild(View view, float velocity) {
        final View animView = this.mCallback.getChildContentView(view);
        final boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(animView);
        ObjectAnimator anim = createTranslationAnimation(animView, 0.0f);
        anim.setDuration(150);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                SwipeHelper.this.updateSwipeProgressFromOffset(animView, canAnimViewBeDismissed);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                SwipeHelper.this.updateSwipeProgressFromOffset(animView, canAnimViewBeDismissed);
                SwipeHelper.this.mCallback.onChildSnappedBack(animView);
            }
        });
        anim.start();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean childSwipedFastEnough;
        boolean dismissChild;
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
                    float maxVelocity = this.MAX_DISMISS_VELOCITY * this.mDensityScale;
                    this.mVelocityTracker.computeCurrentVelocity(1000, maxVelocity);
                    float escapeVelocity = this.SWIPE_ESCAPE_VELOCITY * this.mDensityScale;
                    float velocity = getVelocity(this.mVelocityTracker);
                    float perpendicularVelocity = getPerpendicularVelocity(this.mVelocityTracker);
                    boolean childSwipedFarEnough = ((double) Math.abs(getTranslation(this.mCurrAnimView))) > 0.4d * ((double) getSize(this.mCurrAnimView));
                    if (Math.abs(velocity) > escapeVelocity && Math.abs(velocity) > Math.abs(perpendicularVelocity)) {
                        if ((velocity > 0.0f) == (getTranslation(this.mCurrAnimView) > 0.0f)) {
                            childSwipedFastEnough = true;
                        }
                        if (this.mCallback.isAntiFalsingNeeded()) {
                            if (this.mCallback.canChildBeDismissed(this.mCurrView)) {
                                if (!dismissChild) {
                                }
                            }
                        }
                    } else {
                        childSwipedFastEnough = false;
                        boolean falsingDetected = (this.mCallback.isAntiFalsingNeeded() || this.mTouchAboveFalsingThreshold) ? false : true;
                        dismissChild = (this.mCallback.canChildBeDismissed(this.mCurrView) || falsingDetected || (!childSwipedFastEnough && !childSwipedFarEnough) || ev.getActionMasked() != 1) ? false : true;
                        if (!dismissChild) {
                            View view = this.mCurrView;
                            if (!childSwipedFastEnough) {
                                velocity = 0.0f;
                            }
                            dismissChild(view, velocity);
                        } else {
                            this.mCallback.onDragCancelled(this.mCurrView);
                            snapChild(this.mCurrView, velocity);
                        }
                    }
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
                        float size = getSize(this.mCurrAnimView);
                        float maxScrollDistance = 0.15f * size;
                        if (absDelta >= size) {
                            delta = delta > 0.0f ? maxScrollDistance : -maxScrollDistance;
                        } else {
                            delta = maxScrollDistance * ((float) Math.sin(((double) (delta / size)) * 1.5707963267948966d));
                        }
                    }
                    setTranslation(this.mCurrAnimView, delta);
                    updateSwipeProgressFromOffset(this.mCurrAnimView, this.mCanCurrViewBeDimissed);
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
}
