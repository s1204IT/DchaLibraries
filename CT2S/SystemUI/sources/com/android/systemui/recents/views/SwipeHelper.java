package com.android.systemui.recents.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Property;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.android.systemui.recents.RecentsConfiguration;

public class SwipeHelper {
    Callback mCallback;
    private boolean mCanCurrViewBeDimissed;
    private View mCurrView;
    private float mDensityScale;
    private boolean mDragging;
    private float mInitialTouchPos;
    private float mPagingTouchSlop;
    private boolean mRtl;
    private int mSwipeDirection;
    private static LinearInterpolator sLinearInterpolator = new LinearInterpolator();
    public static float ALPHA_FADE_START = 0.15f;
    private float SWIPE_ESCAPE_VELOCITY = 100.0f;
    private int DEFAULT_ESCAPE_ANIMATION_DURATION = 75;
    private int MAX_ESCAPE_ANIMATION_DURATION = 150;
    private float mMinAlpha = 0.0f;
    public boolean mAllowSwipeTowardsStart = true;
    public boolean mAllowSwipeTowardsEnd = true;
    private VelocityTracker mVelocityTracker = VelocityTracker.obtain();

    public interface Callback {
        boolean canChildBeDismissed(View view);

        View getChildAtPosition(MotionEvent motionEvent);

        void onBeginDrag(View view);

        void onChildDismissed(View view);

        void onDragCancelled(View view);

        void onSnapBackCompleted(View view);

        void onSwipeChanged(View view, float f);
    }

    public SwipeHelper(int swipeDirection, Callback callback, float densityScale, float pagingTouchSlop) {
        this.mCallback = callback;
        this.mSwipeDirection = swipeDirection;
        this.mDensityScale = densityScale;
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
        ObjectAnimator anim = ObjectAnimator.ofFloat(v, (Property<View, Float>) (this.mSwipeDirection == 0 ? View.TRANSLATION_X : View.TRANSLATION_Y), newPos);
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
        DisplayMetrics dm = v.getContext().getResources().getDisplayMetrics();
        return this.mSwipeDirection == 0 ? dm.widthPixels : dm.heightPixels;
    }

    public void setMinAlpha(float minAlpha) {
        this.mMinAlpha = minAlpha;
    }

    float getAlphaForOffset(View view) {
        float viewSize = getSize(view);
        float fadeSize = 0.65f * viewSize;
        float result = 1.0f;
        float pos = getTranslation(view);
        if (pos >= ALPHA_FADE_START * viewSize) {
            result = 1.0f - ((pos - (ALPHA_FADE_START * viewSize)) / fadeSize);
        } else if (pos < (1.0f - ALPHA_FADE_START) * viewSize) {
            result = 1.0f + (((ALPHA_FADE_START * viewSize) + pos) / fadeSize);
        }
        return Math.max(this.mMinAlpha, Math.max(Math.min(result, 1.0f), 0.0f));
    }

    @TargetApi(17)
    public static boolean isLayoutRtl(View view) {
        return Build.VERSION.SDK_INT >= 17 && 1 == view.getLayoutDirection();
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case 0:
                this.mDragging = false;
                this.mCurrView = this.mCallback.getChildAtPosition(ev);
                this.mVelocityTracker.clear();
                if (this.mCurrView != null) {
                    this.mRtl = isLayoutRtl(this.mCurrView);
                    this.mCanCurrViewBeDimissed = this.mCallback.canChildBeDismissed(this.mCurrView);
                    this.mVelocityTracker.addMovement(ev);
                    this.mInitialTouchPos = getPos(ev);
                } else {
                    this.mCanCurrViewBeDimissed = false;
                }
                break;
            case 1:
            case 3:
                this.mDragging = false;
                this.mCurrView = null;
                break;
            case 2:
                if (this.mCurrView != null) {
                    this.mVelocityTracker.addMovement(ev);
                    float pos = getPos(ev);
                    float delta = pos - this.mInitialTouchPos;
                    if (Math.abs(delta) > this.mPagingTouchSlop) {
                        this.mCallback.onBeginDrag(this.mCurrView);
                        this.mDragging = true;
                        this.mInitialTouchPos = pos - getTranslation(this.mCurrView);
                    }
                }
                break;
        }
        return this.mDragging;
    }

    private void dismissChild(final View view, float velocity) {
        float newPos;
        int duration;
        final boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        if (velocity < 0.0f || ((velocity == 0.0f && getTranslation(view) < 0.0f) || (velocity == 0.0f && getTranslation(view) == 0.0f && this.mSwipeDirection == 1))) {
            newPos = -getSize(view);
        } else {
            newPos = getSize(view);
        }
        int duration2 = this.MAX_ESCAPE_ANIMATION_DURATION;
        if (velocity != 0.0f) {
            duration = Math.min(duration2, (int) ((Math.abs(newPos - getTranslation(view)) * 1000.0f) / Math.abs(velocity)));
        } else {
            duration = this.DEFAULT_ESCAPE_ANIMATION_DURATION;
        }
        ValueAnimator anim = createTranslationAnimation(view, newPos);
        anim.setInterpolator(sLinearInterpolator);
        anim.setDuration(duration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                SwipeHelper.this.mCallback.onChildDismissed(view);
                if (canAnimViewBeDismissed) {
                    view.setAlpha(1.0f);
                }
            }
        });
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (canAnimViewBeDismissed) {
                    view.setAlpha(SwipeHelper.this.getAlphaForOffset(view));
                }
            }
        });
        anim.start();
    }

    private void snapChild(final View view, float velocity) {
        final boolean canAnimViewBeDismissed = this.mCallback.canChildBeDismissed(view);
        ValueAnimator anim = createTranslationAnimation(view, 0.0f);
        anim.setDuration(250);
        anim.setInterpolator(RecentsConfiguration.getInstance().linearOutSlowInInterpolator);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (canAnimViewBeDismissed) {
                    view.setAlpha(SwipeHelper.this.getAlphaForOffset(view));
                }
                SwipeHelper.this.mCallback.onSwipeChanged(SwipeHelper.this.mCurrView, view.getTranslationX());
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (canAnimViewBeDismissed) {
                    view.setAlpha(1.0f);
                }
                SwipeHelper.this.mCallback.onSnapBackCompleted(view);
            }
        });
        anim.start();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.mDragging && !onInterceptTouchEvent(ev)) {
            return this.mCanCurrViewBeDimissed;
        }
        this.mVelocityTracker.addMovement(ev);
        int action = ev.getAction();
        switch (action) {
            case 1:
            case 3:
                if (this.mCurrView != null) {
                    endSwipe(this.mVelocityTracker);
                }
                return true;
            case 2:
            case 4:
                if (this.mCurrView != null) {
                    float delta = getPos(ev) - this.mInitialTouchPos;
                    setSwipeAmount(delta);
                    this.mCallback.onSwipeChanged(this.mCurrView, delta);
                }
                return true;
            default:
                return true;
        }
    }

    private void setSwipeAmount(float amount) {
        if (!isValidSwipeDirection(amount) || !this.mCallback.canChildBeDismissed(this.mCurrView)) {
            float size = getSize(this.mCurrView);
            float maxScrollDistance = 0.15f * size;
            if (Math.abs(amount) >= size) {
                amount = amount > 0.0f ? maxScrollDistance : -maxScrollDistance;
            } else {
                amount = maxScrollDistance * ((float) Math.sin(((double) (amount / size)) * 1.5707963267948966d));
            }
        }
        setTranslation(this.mCurrView, amount);
        if (this.mCanCurrViewBeDimissed) {
            float alpha = getAlphaForOffset(this.mCurrView);
            this.mCurrView.setAlpha(alpha);
        }
    }

    private boolean isValidSwipeDirection(float amount) {
        if (this.mSwipeDirection == 0) {
            return this.mRtl ? amount <= 0.0f ? this.mAllowSwipeTowardsEnd : this.mAllowSwipeTowardsStart : amount <= 0.0f ? this.mAllowSwipeTowardsStart : this.mAllowSwipeTowardsEnd;
        }
        return true;
    }

    private void endSwipe(VelocityTracker velocityTracker) {
        boolean childSwipedFastEnough;
        velocityTracker.computeCurrentVelocity(1000);
        float velocity = getVelocity(velocityTracker);
        float perpendicularVelocity = getPerpendicularVelocity(velocityTracker);
        float escapeVelocity = this.SWIPE_ESCAPE_VELOCITY * this.mDensityScale;
        float translation = getTranslation(this.mCurrView);
        boolean childSwipedFarEnough = ((double) Math.abs(translation)) > 0.6d * ((double) getSize(this.mCurrView));
        if (Math.abs(velocity) <= escapeVelocity || Math.abs(velocity) <= Math.abs(perpendicularVelocity)) {
            childSwipedFastEnough = false;
        } else {
            if ((velocity > 0.0f) == (translation > 0.0f)) {
                childSwipedFastEnough = true;
            }
        }
        boolean dismissChild = this.mCallback.canChildBeDismissed(this.mCurrView) && isValidSwipeDirection(translation) && (childSwipedFastEnough || childSwipedFarEnough);
        if (dismissChild) {
            View view = this.mCurrView;
            if (!childSwipedFastEnough) {
                velocity = 0.0f;
            }
            dismissChild(view, velocity);
            return;
        }
        this.mCallback.onDragCancelled(this.mCurrView);
        snapChild(this.mCurrView, velocity);
    }
}
