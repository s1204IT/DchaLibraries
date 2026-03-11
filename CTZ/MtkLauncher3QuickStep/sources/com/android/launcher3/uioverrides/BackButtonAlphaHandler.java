package com.android.launcher3.uioverrides;

import android.animation.ValueAnimator;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.quickstep.OverviewInteractionState;

public class BackButtonAlphaHandler implements LauncherStateManager.StateHandler {
    private static final String TAG = "BackButtonAlphaHandler";
    private final Launcher mLauncher;
    private final OverviewInteractionState mOverviewInteractionState;

    public BackButtonAlphaHandler(Launcher launcher) {
        this.mLauncher = launcher;
        this.mOverviewInteractionState = OverviewInteractionState.getInstance(this.mLauncher);
    }

    @Override
    public void setState(LauncherState launcherState) {
        UiFactory.onLauncherStateOrFocusChanged(this.mLauncher);
    }

    @Override
    public void setStateWithAnimation(LauncherState launcherState, AnimatorSetBuilder animatorSetBuilder, LauncherStateManager.AnimationConfig animationConfig) {
        if (!animationConfig.playNonAtomicComponent()) {
            return;
        }
        float backButtonAlpha = this.mOverviewInteractionState.getBackButtonAlpha();
        float f = launcherState.hideBackButton ? 0.0f : 1.0f;
        if (Float.compare(backButtonAlpha, f) != 0) {
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(backButtonAlpha, f);
            valueAnimatorOfFloat.setDuration(animationConfig.duration);
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                    this.f$0.mOverviewInteractionState.setBackButtonAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue(), false);
                }
            });
            animatorSetBuilder.play(valueAnimatorOfFloat);
        }
    }
}
