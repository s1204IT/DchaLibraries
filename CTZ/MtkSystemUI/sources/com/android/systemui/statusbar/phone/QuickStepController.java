package com.android.systemui.statusbar.phone;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.RemoteException;
import android.util.FloatProperty;
import android.util.Log;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManagerGlobal;
import com.android.internal.graphics.ColorUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.OverviewProxyService;
import com.android.systemui.R;
import com.android.systemui.plugins.statusbar.phone.NavGesture;
import com.android.systemui.shared.recents.IOverviewProxy;
import com.android.systemui.shared.recents.utilities.Utilities;
import com.android.systemui.shared.system.NavigationBarCompat;

public class QuickStepController implements NavGesture.GestureHelper {
    private boolean mAllowGestureDetection;
    private final Context mContext;
    private View mCurrentNavigationBarView;
    private float mDarkIntensity;
    private int mDarkTrackColor;
    private boolean mDragPositive;
    private ButtonDispatcher mHitTarget;
    private boolean mIsRTL;
    private boolean mIsVertical;
    private int mLightTrackColor;
    private NavigationBarView mNavigationBarView;
    private final OverviewProxyService mOverviewEventSender;
    private boolean mQuickScrubActive;
    private boolean mQuickStepStarted;
    private int mTouchDownX;
    private int mTouchDownY;
    private float mTrackAlpha;
    private AnimatorSet mTrackAnimator;
    private final Drawable mTrackDrawable;
    private final int mTrackEndPadding;
    private final int mTrackThickness;
    private float mTrackScale = 0.95f;
    private final Handler mHandler = new Handler();
    private final Rect mTrackRect = new Rect();
    private final Matrix mTransformGlobalMatrix = new Matrix();
    private final Matrix mTransformLocalMatrix = new Matrix();
    private final ArgbEvaluator mTrackColorEvaluator = new ArgbEvaluator();
    private final FloatProperty<QuickStepController> mTrackAlphaProperty = new FloatProperty<QuickStepController>("TrackAlpha") {
        @Override
        public void setValue(QuickStepController quickStepController, float f) {
            QuickStepController.this.mTrackAlpha = f;
            QuickStepController.this.mNavigationBarView.invalidate();
        }

        @Override
        public Float get(QuickStepController quickStepController) {
            return Float.valueOf(QuickStepController.this.mTrackAlpha);
        }
    };
    private final FloatProperty<QuickStepController> mTrackScaleProperty = new FloatProperty<QuickStepController>("TrackScale") {
        @Override
        public void setValue(QuickStepController quickStepController, float f) {
            QuickStepController.this.mTrackScale = f;
            QuickStepController.this.mNavigationBarView.invalidate();
        }

        @Override
        public Float get(QuickStepController quickStepController) {
            return Float.valueOf(QuickStepController.this.mTrackScale);
        }
    };
    private final FloatProperty<QuickStepController> mNavBarAlphaProperty = new FloatProperty<QuickStepController>("NavBarAlpha") {
        @Override
        public void setValue(QuickStepController quickStepController, float f) {
            if (QuickStepController.this.mCurrentNavigationBarView != null) {
                QuickStepController.this.mCurrentNavigationBarView.setAlpha(f);
            }
        }

        @Override
        public Float get(QuickStepController quickStepController) {
            if (QuickStepController.this.mCurrentNavigationBarView != null) {
                return Float.valueOf(QuickStepController.this.mCurrentNavigationBarView.getAlpha());
            }
            return Float.valueOf(1.0f);
        }
    };
    private AnimatorListenerAdapter mQuickScrubEndListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animator) {
            QuickStepController.this.resetQuickScrub();
        }
    };

    public QuickStepController(Context context) {
        Resources resources = context.getResources();
        this.mContext = context;
        this.mOverviewEventSender = (OverviewProxyService) Dependency.get(OverviewProxyService.class);
        this.mTrackThickness = resources.getDimensionPixelSize(R.dimen.nav_quick_scrub_track_thickness);
        this.mTrackEndPadding = resources.getDimensionPixelSize(R.dimen.nav_quick_scrub_track_edge_padding);
        this.mTrackDrawable = context.getDrawable(R.drawable.qs_scrubber_track).mutate();
    }

    public void setComponents(NavigationBarView navigationBarView) {
        this.mNavigationBarView = navigationBarView;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return handleTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return (motionEvent.getAction() == 0 && this.mOverviewEventSender.getProxy() != null) || handleTouchEvent(motionEvent);
    }

    private boolean handleTouchEvent(MotionEvent motionEvent) {
        boolean z;
        boolean z2;
        int i;
        int iHeight;
        int iWidth;
        if (this.mOverviewEventSender.getProxy() == null || !(this.mNavigationBarView.isQuickScrubEnabled() || this.mNavigationBarView.isQuickStepSwipeUpEnabled())) {
            return false;
        }
        this.mNavigationBarView.requestUnbufferedDispatch(motionEvent);
        int actionMasked = motionEvent.getActionMasked();
        switch (actionMasked) {
            case 0:
                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();
                if (this.mTrackAnimator != null) {
                    this.mTrackAnimator.end();
                    this.mTrackAnimator = null;
                }
                this.mCurrentNavigationBarView = this.mNavigationBarView.getCurrentView();
                this.mHitTarget = this.mNavigationBarView.getButtonAtPosition(x, y);
                if (this.mHitTarget != null) {
                    this.mHitTarget.setDelayTouchFeedback(true);
                }
                this.mTouchDownX = x;
                this.mTouchDownY = y;
                this.mTransformGlobalMatrix.set(Matrix.IDENTITY_MATRIX);
                this.mTransformLocalMatrix.set(Matrix.IDENTITY_MATRIX);
                this.mNavigationBarView.transformMatrixToGlobal(this.mTransformGlobalMatrix);
                this.mNavigationBarView.transformMatrixToLocal(this.mTransformLocalMatrix);
                this.mQuickStepStarted = false;
                this.mAllowGestureDetection = true;
                break;
            case 1:
            case 3:
                endQuickScrub(true);
                break;
            case 2:
                if (!this.mQuickStepStarted && this.mAllowGestureDetection) {
                    int x2 = (int) motionEvent.getX();
                    int y2 = (int) motionEvent.getY();
                    int iAbs = Math.abs(x2 - this.mTouchDownX);
                    int iAbs2 = Math.abs(y2 - this.mTouchDownY);
                    if (this.mIsVertical) {
                        boolean z3 = iAbs2 > NavigationBarCompat.getQuickScrubTouchSlopPx() && iAbs2 > iAbs;
                        z2 = iAbs > NavigationBarCompat.getQuickStepTouchSlopPx() && iAbs > iAbs2;
                        i = this.mTouchDownY;
                        iHeight = y2 - this.mTrackRect.top;
                        iWidth = this.mTrackRect.height();
                        z = z3;
                        x2 = y2;
                    } else {
                        z = iAbs > NavigationBarCompat.getQuickScrubTouchSlopPx() && iAbs > iAbs2;
                        z2 = iAbs2 > NavigationBarCompat.getQuickStepTouchSlopPx() && iAbs2 > iAbs;
                        i = this.mTouchDownX;
                        iHeight = x2 - this.mTrackRect.left;
                        iWidth = this.mTrackRect.width();
                    }
                    if (!this.mQuickScrubActive && z2) {
                        if (this.mNavigationBarView.isQuickStepSwipeUpEnabled()) {
                            startQuickStep(motionEvent);
                        }
                    } else if (this.mNavigationBarView.isQuickScrubEnabled()) {
                        if (!this.mDragPositive) {
                            iHeight -= this.mIsVertical ? this.mTrackRect.height() : this.mTrackRect.width();
                        }
                        boolean z4 = this.mDragPositive ? !(iHeight < 0 || x2 <= i) : !(iHeight >= 0 || x2 >= i);
                        float fClamp = Utilities.clamp((Math.abs(iHeight) * 1.0f) / iWidth, 0.0f, 1.0f);
                        if (z4 && !this.mQuickScrubActive && z) {
                            startQuickScrub();
                        }
                        if (this.mQuickScrubActive && ((this.mDragPositive && iHeight >= 0) || (!this.mDragPositive && iHeight <= 0))) {
                            try {
                                this.mOverviewEventSender.getProxy().onQuickScrubProgress(fClamp);
                            } catch (RemoteException e) {
                                Log.e("QuickStepController", "Failed to send progress of quick scrub.", e);
                            }
                        }
                    }
                }
                break;
        }
        if (!this.mQuickScrubActive && (this.mAllowGestureDetection || actionMasked == 3 || actionMasked == 1)) {
            proxyMotionEvents(motionEvent);
        }
        return this.mQuickScrubActive || this.mQuickStepStarted;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (!this.mNavigationBarView.isQuickScrubEnabled()) {
            return;
        }
        this.mTrackDrawable.setTint(ColorUtils.setAlphaComponent(((Integer) this.mTrackColorEvaluator.evaluate(this.mDarkIntensity, Integer.valueOf(this.mLightTrackColor), Integer.valueOf(this.mDarkTrackColor))).intValue(), (int) (Color.alpha(r0) * this.mTrackAlpha)));
        canvas.save();
        canvas.scale(this.mTrackScale / this.mNavigationBarView.getScaleX(), 1.0f / this.mNavigationBarView.getScaleY(), this.mTrackRect.centerX(), this.mTrackRect.centerY());
        this.mTrackDrawable.draw(canvas);
        canvas.restore();
    }

    @Override
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int i5;
        int i6;
        int paddingStart;
        int i7;
        int paddingLeft = this.mNavigationBarView.getPaddingLeft();
        int paddingTop = this.mNavigationBarView.getPaddingTop();
        int paddingRight = ((i3 - i) - this.mNavigationBarView.getPaddingRight()) - paddingLeft;
        int paddingBottom = ((i4 - i2) - this.mNavigationBarView.getPaddingBottom()) - paddingTop;
        if (this.mIsVertical) {
            int i8 = ((paddingRight - this.mTrackThickness) / 2) + paddingLeft;
            int i9 = this.mTrackThickness + i8;
            i5 = paddingTop + this.mTrackEndPadding;
            i6 = (paddingBottom + i5) - (2 * this.mTrackEndPadding);
            i7 = i9;
            paddingStart = i8;
        } else {
            i5 = paddingTop + ((paddingBottom - this.mTrackThickness) / 2);
            i6 = i5 + this.mTrackThickness;
            paddingStart = this.mNavigationBarView.getPaddingStart() + this.mTrackEndPadding;
            i7 = (paddingRight + paddingStart) - (2 * this.mTrackEndPadding);
        }
        this.mTrackRect.set(paddingStart, i5, i7, i6);
        this.mTrackDrawable.setBounds(this.mTrackRect);
    }

    @Override
    public void onDarkIntensityChange(float f) {
        this.mDarkIntensity = f;
        this.mNavigationBarView.invalidate();
    }

    @Override
    public void setBarState(boolean z, boolean z2) {
        boolean z3 = false;
        if ((this.mIsVertical == z && this.mIsRTL == z2) ? false : true) {
            endQuickScrub(false);
        }
        this.mIsVertical = z;
        this.mIsRTL = z2;
        try {
            int navBarPosition = WindowManagerGlobal.getWindowManagerService().getNavBarPosition();
            if (navBarPosition == 1 || navBarPosition == 4) {
                z3 = true;
            }
            this.mDragPositive = z3;
            if (z2) {
                this.mDragPositive = !this.mDragPositive;
            }
        } catch (RemoteException e) {
            Slog.e("QuickStepController", "Failed to get nav bar position.", e);
        }
    }

    @Override
    public void onNavigationButtonLongPress(View view) {
        this.mAllowGestureDetection = false;
        this.mHandler.removeCallbacksAndMessages(null);
    }

    private void startQuickStep(MotionEvent motionEvent) {
        this.mQuickStepStarted = true;
        motionEvent.transform(this.mTransformGlobalMatrix);
        try {
            try {
                this.mOverviewEventSender.getProxy().onQuickStep(motionEvent);
            } catch (RemoteException e) {
                Log.e("QuickStepController", "Failed to send quick step started.", e);
            }
            this.mOverviewEventSender.notifyQuickStepStarted();
            this.mHandler.removeCallbacksAndMessages(null);
            if (this.mHitTarget != null) {
                this.mHitTarget.abortCurrentGesture();
            }
            if (this.mQuickScrubActive) {
                animateEnd();
            }
        } finally {
            motionEvent.transform(this.mTransformLocalMatrix);
        }
    }

    private void startQuickScrub() {
        if (!this.mQuickScrubActive) {
            this.mQuickScrubActive = true;
            this.mLightTrackColor = this.mContext.getColor(R.color.quick_step_track_background_light);
            this.mDarkTrackColor = this.mContext.getColor(R.color.quick_step_track_background_dark);
            ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat(this.mTrackAlphaProperty, 1.0f), PropertyValuesHolder.ofFloat(this.mTrackScaleProperty, 1.0f));
            objectAnimatorOfPropertyValuesHolder.setInterpolator(Interpolators.ALPHA_IN);
            objectAnimatorOfPropertyValuesHolder.setDuration(150L);
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, this.mNavBarAlphaProperty, 0.0f);
            objectAnimatorOfFloat.setInterpolator(Interpolators.ALPHA_OUT);
            objectAnimatorOfFloat.setDuration(134L);
            this.mTrackAnimator = new AnimatorSet();
            this.mTrackAnimator.playTogether(objectAnimatorOfPropertyValuesHolder, objectAnimatorOfFloat);
            this.mTrackAnimator.start();
            try {
                this.mOverviewEventSender.getProxy().onQuickScrubStart();
            } catch (RemoteException e) {
                Log.e("QuickStepController", "Failed to send start of quick scrub.", e);
            }
            this.mOverviewEventSender.notifyQuickScrubStarted();
            if (this.mHitTarget != null) {
                this.mHitTarget.abortCurrentGesture();
            }
        }
    }

    private void endQuickScrub(boolean z) {
        if (this.mQuickScrubActive) {
            animateEnd();
            try {
                this.mOverviewEventSender.getProxy().onQuickScrubEnd();
            } catch (RemoteException e) {
                Log.e("QuickStepController", "Failed to send end of quick scrub.", e);
            }
        }
        if (!z && this.mTrackAnimator != null) {
            this.mTrackAnimator.end();
            this.mTrackAnimator = null;
        }
    }

    private void animateEnd() {
        if (this.mTrackAnimator != null) {
            this.mTrackAnimator.cancel();
        }
        ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofFloat(this.mTrackAlphaProperty, 0.0f), PropertyValuesHolder.ofFloat(this.mTrackScaleProperty, 0.95f));
        objectAnimatorOfPropertyValuesHolder.setInterpolator(Interpolators.ALPHA_OUT);
        objectAnimatorOfPropertyValuesHolder.setDuration(134L);
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, this.mNavBarAlphaProperty, 1.0f);
        objectAnimatorOfFloat.setInterpolator(Interpolators.ALPHA_IN);
        objectAnimatorOfFloat.setDuration(150L);
        this.mTrackAnimator = new AnimatorSet();
        this.mTrackAnimator.playTogether(objectAnimatorOfPropertyValuesHolder, objectAnimatorOfFloat);
        this.mTrackAnimator.addListener(this.mQuickScrubEndListener);
        this.mTrackAnimator.start();
    }

    private void resetQuickScrub() {
        this.mQuickScrubActive = false;
        this.mAllowGestureDetection = false;
        this.mCurrentNavigationBarView = null;
    }

    private boolean proxyMotionEvents(MotionEvent motionEvent) {
        IOverviewProxy proxy = this.mOverviewEventSender.getProxy();
        motionEvent.transform(this.mTransformGlobalMatrix);
        try {
            try {
                if (motionEvent.getActionMasked() == 0) {
                    proxy.onPreMotionEvent(this.mNavigationBarView.getDownHitTarget());
                }
                proxy.onMotionEvent(motionEvent);
                return true;
            } catch (RemoteException e) {
                Log.e("QuickStepController", "Callback failed", e);
                motionEvent.transform(this.mTransformLocalMatrix);
                return false;
            }
        } finally {
            motionEvent.transform(this.mTransformLocalMatrix);
        }
    }
}
