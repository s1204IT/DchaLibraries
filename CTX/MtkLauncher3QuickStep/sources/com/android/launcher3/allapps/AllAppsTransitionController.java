package com.android.launcher3.allapps;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.util.Property;
import android.view.animation.Interpolator;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.R;
import com.android.launcher3.anim.AnimationSuccessListener;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertySetter;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ScrimView;

public class AllAppsTransitionController implements LauncherStateManager.StateHandler, DeviceProfile.OnDeviceProfileChangeListener {
    public static final Property<AllAppsTransitionController, Float> ALL_APPS_PROGRESS = new Property<AllAppsTransitionController, Float>(Float.class, "allAppsProgress") {
        @Override
        public Float get(AllAppsTransitionController allAppsTransitionController) {
            return Float.valueOf(allAppsTransitionController.mProgress);
        }

        @Override
        public void set(AllAppsTransitionController allAppsTransitionController, Float f) {
            allAppsTransitionController.setProgress(f.floatValue());
        }
    };
    private AllAppsContainerView mAppsView;
    private final boolean mIsDarkTheme;
    private boolean mIsVerticalLayout;
    private final Launcher mLauncher;
    private ScrimView mScrimView;
    private float mShiftRange;
    private float mScrollRangeDelta = 0.0f;
    private float mProgress = 1.0f;

    public AllAppsTransitionController(Launcher launcher) {
        this.mLauncher = launcher;
        this.mShiftRange = this.mLauncher.getDeviceProfile().heightPx;
        this.mIsDarkTheme = Themes.getAttrBoolean(this.mLauncher, R.attr.isMainColorDark);
        this.mIsVerticalLayout = this.mLauncher.getDeviceProfile().isVerticalBarLayout();
        this.mLauncher.addOnDeviceProfileChangeListener(this);
    }

    public float getShiftRange() {
        return this.mShiftRange;
    }

    public void onProgressAnimationStart() {
        this.mAppsView.setVisibility(0);
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile deviceProfile) {
        this.mIsVerticalLayout = deviceProfile.isVerticalBarLayout();
        setScrollRangeDelta(this.mScrollRangeDelta);
        if (this.mIsVerticalLayout) {
            this.mAppsView.setAlpha(1.0f);
            this.mLauncher.getHotseat().setTranslationY(0.0f);
            this.mLauncher.getWorkspace().getPageIndicator().setTranslationY(0.0f);
        }
    }

    public void setProgress(float f) {
        this.mProgress = f;
        this.mScrimView.setProgress(f);
        float f2 = f * this.mShiftRange;
        this.mAppsView.setTranslationY(f2);
        float f3 = (-this.mShiftRange) + f2;
        if (!this.mIsVerticalLayout) {
            this.mLauncher.getHotseat().setTranslationY(f3);
            this.mLauncher.getWorkspace().getPageIndicator().setTranslationY(f3);
        }
        if (f2 - ((float) this.mScrimView.getDragHandleSize()) <= ((float) (this.mLauncher.getDeviceProfile().getInsets().top / 2))) {
            this.mLauncher.getSystemUiController().updateUiState(1, !this.mIsDarkTheme);
        } else {
            this.mLauncher.getSystemUiController().updateUiState(1, 0);
        }
    }

    public float getProgress() {
        return this.mProgress;
    }

    @Override
    public void setState(LauncherState launcherState) {
        setProgress(launcherState.getVerticalProgress(this.mLauncher));
        setAlphas(launcherState, PropertySetter.NO_ANIM_PROPERTY_SETTER);
        onProgressAnimationEnd();
    }

    @Override
    public void setStateWithAnimation(LauncherState launcherState, AnimatorSetBuilder animatorSetBuilder, LauncherStateManager.AnimationConfig animationConfig) {
        Interpolator interpolator;
        float verticalProgress = launcherState.getVerticalProgress(this.mLauncher);
        if (Float.compare(this.mProgress, verticalProgress) == 0) {
            setAlphas(launcherState, animationConfig.getPropertySetter(animatorSetBuilder));
            onProgressAnimationEnd();
            return;
        }
        if (!animationConfig.playNonAtomicComponent()) {
            return;
        }
        if (animationConfig.userControlled) {
            interpolator = Interpolators.LINEAR;
        } else if (launcherState == LauncherState.OVERVIEW) {
            interpolator = animatorSetBuilder.getInterpolator(3, Interpolators.FAST_OUT_SLOW_IN);
        } else {
            interpolator = Interpolators.FAST_OUT_SLOW_IN;
        }
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, ALL_APPS_PROGRESS, this.mProgress, verticalProgress);
        objectAnimatorOfFloat.setDuration(animationConfig.duration);
        objectAnimatorOfFloat.setInterpolator(animatorSetBuilder.getInterpolator(0, interpolator));
        objectAnimatorOfFloat.addListener(getProgressAnimatorListener());
        animatorSetBuilder.play(objectAnimatorOfFloat);
        setAlphas(launcherState, animationConfig.getPropertySetter(animatorSetBuilder));
    }

    private void setAlphas(LauncherState launcherState, PropertySetter propertySetter) {
        int visibleElements = launcherState.getVisibleElements(this.mLauncher);
        boolean z = (visibleElements & 4) != 0;
        boolean z2 = (visibleElements & 8) != 0;
        boolean z3 = (visibleElements & 16) != 0;
        if (this.mAppsView.getSearchView() != null) {
            propertySetter.setViewAlpha(this.mAppsView.getSearchView(), z ? 1.0f : 0.0f, Interpolators.LINEAR);
        }
        propertySetter.setViewAlpha(this.mAppsView.getContentView(), z3 ? 1.0f : 0.0f, Interpolators.LINEAR);
        propertySetter.setViewAlpha(this.mAppsView.getScrollBar(), z3 ? 1.0f : 0.0f, Interpolators.LINEAR);
        this.mAppsView.getFloatingHeaderView().setContentVisibility(z2, z3, propertySetter);
        propertySetter.setInt(this.mScrimView, ScrimView.DRAG_HANDLE_ALPHA, (visibleElements & 32) != 0 ? 255 : 0, Interpolators.LINEAR);
    }

    public AnimatorListenerAdapter getProgressAnimatorListener() {
        return new AnimationSuccessListener() {
            @Override
            public void onAnimationSuccess(Animator animator) {
                AllAppsTransitionController.this.onProgressAnimationEnd();
            }

            @Override
            public void onAnimationStart(Animator animator) {
                AllAppsTransitionController.this.onProgressAnimationStart();
            }
        };
    }

    public void setupViews(AllAppsContainerView allAppsContainerView) {
        this.mAppsView = allAppsContainerView;
        this.mScrimView = (ScrimView) this.mLauncher.findViewById(R.id.scrim_view);
    }

    public void setScrollRangeDelta(float f) {
        this.mScrollRangeDelta = f;
        this.mShiftRange = this.mLauncher.getDeviceProfile().heightPx - this.mScrollRangeDelta;
        if (this.mScrimView != null) {
            this.mScrimView.reInitUi();
        }
    }

    public void onProgressAnimationEnd() {
        if (Float.compare(this.mProgress, 1.0f) == 0) {
            this.mAppsView.setVisibility(4);
            this.mAppsView.reset(false);
        } else if (Float.compare(this.mProgress, 0.0f) == 0) {
            this.mAppsView.setVisibility(0);
            this.mAppsView.onScrollUpEnd();
        } else {
            this.mAppsView.setVisibility(0);
        }
    }
}
