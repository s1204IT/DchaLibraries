package com.android.launcher3.uioverrides;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.view.animation.Interpolator;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertySetter;
import com.android.quickstep.QuickScrubController;
import com.android.quickstep.views.LauncherRecentsView;
import com.android.quickstep.views.RecentsViewContainer;
import java.util.Objects;

@TargetApi(26)
public class RecentsViewStateController implements LauncherStateManager.StateHandler {
    private final Launcher mLauncher;
    private final LauncherRecentsView mRecentsView;
    private final RecentsViewContainer mRecentsViewContainer;

    public RecentsViewStateController(Launcher launcher) {
        this.mLauncher = launcher;
        this.mRecentsView = (LauncherRecentsView) launcher.getOverviewPanel();
        this.mRecentsViewContainer = (RecentsViewContainer) launcher.getOverviewPanelContainer();
    }

    @Override
    public void setState(LauncherState launcherState) {
        this.mRecentsViewContainer.setContentAlpha(launcherState.overviewUi ? 1.0f : 0.0f);
        float[] overviewScaleAndTranslationYFactor = launcherState.getOverviewScaleAndTranslationYFactor(this.mLauncher);
        LauncherAnimUtils.SCALE_PROPERTY.set(this.mRecentsView, Float.valueOf(overviewScaleAndTranslationYFactor[0]));
        this.mRecentsView.setTranslationYFactor(overviewScaleAndTranslationYFactor[1]);
        if (launcherState.overviewUi) {
            this.mRecentsView.updateEmptyMessage();
            this.mRecentsView.resetTaskVisuals();
        }
    }

    @Override
    public void setStateWithAnimation(LauncherState launcherState, AnimatorSetBuilder animatorSetBuilder, LauncherStateManager.AnimationConfig animationConfig) {
        if (!animationConfig.playAtomicComponent()) {
            return;
        }
        PropertySetter propertySetter = animationConfig.getPropertySetter(animatorSetBuilder);
        float[] overviewScaleAndTranslationYFactor = launcherState.getOverviewScaleAndTranslationYFactor(this.mLauncher);
        Interpolator interpolator = animatorSetBuilder.getInterpolator(3, Interpolators.LINEAR);
        if (this.mLauncher.getStateManager().getState() == LauncherState.OVERVIEW && launcherState == LauncherState.FAST_OVERVIEW) {
            interpolator = Interpolators.clampToProgress(QuickScrubController.QUICK_SCRUB_START_INTERPOLATOR, 0.0f, 0.8333333f);
        }
        propertySetter.setFloat(this.mRecentsView, LauncherAnimUtils.SCALE_PROPERTY, overviewScaleAndTranslationYFactor[0], interpolator);
        propertySetter.setFloat(this.mRecentsView, LauncherRecentsView.TRANSLATION_Y_FACTOR, overviewScaleAndTranslationYFactor[1], interpolator);
        propertySetter.setFloat(this.mRecentsViewContainer, RecentsViewContainer.CONTENT_ALPHA, launcherState.overviewUi ? 1.0f : 0.0f, animatorSetBuilder.getInterpolator(4, Interpolators.AGGRESSIVE_EASE_IN_OUT));
        if (!launcherState.overviewUi) {
            final LauncherRecentsView launcherRecentsView = this.mRecentsView;
            Objects.requireNonNull(launcherRecentsView);
            animatorSetBuilder.addOnFinishRunnable(new Runnable() {
                @Override
                public final void run() {
                    launcherRecentsView.resetTaskVisuals();
                }
            });
        }
        if (launcherState.overviewUi) {
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    this.f$0.mRecentsView.loadVisibleTaskData();
                }
            });
            valueAnimatorOfFloat.setDuration(animationConfig.duration);
            animatorSetBuilder.play(valueAnimatorOfFloat);
            this.mRecentsView.updateEmptyMessage();
        }
    }
}
