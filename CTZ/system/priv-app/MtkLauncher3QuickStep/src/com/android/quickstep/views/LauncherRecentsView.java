package com.android.quickstep.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.view.View;
import android.view.ViewDebug;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.allapps.AllAppsTransitionController;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.views.ScrimView;
import com.android.quickstep.OverviewInteractionState;
import com.android.quickstep.util.ClipAnimationHelper;
import com.android.quickstep.util.LayoutUtils;

@TargetApi(26)
/* loaded from: classes.dex */
public class LauncherRecentsView extends RecentsView<Launcher> {
    public static final FloatProperty<LauncherRecentsView> TRANSLATION_Y_FACTOR = new FloatProperty<LauncherRecentsView>("translationYFactor") { // from class: com.android.quickstep.views.LauncherRecentsView.1
        /* JADX DEBUG: Method merged with bridge method: setValue(Ljava/lang/Object;F)V */
        @Override // android.util.FloatProperty
        public void setValue(LauncherRecentsView launcherRecentsView, float f) {
            launcherRecentsView.setTranslationYFactor(f);
        }

        /* JADX DEBUG: Method merged with bridge method: get(Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.util.Property
        public Float get(LauncherRecentsView launcherRecentsView) {
            return Float.valueOf(launcherRecentsView.mTranslationYFactor);
        }
    };

    @ViewDebug.ExportedProperty(category = "launcher")
    private float mTranslationYFactor;

    public LauncherRecentsView(Context context) {
        this(context, null);
    }

    public LauncherRecentsView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public LauncherRecentsView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        setContentAlpha(0.0f);
    }

    @Override // com.android.quickstep.views.RecentsView
    protected void onAllTasksRemoved() {
        ((Launcher) this.mActivity).getStateManager().goToState(LauncherState.NORMAL);
    }

    @Override // com.android.quickstep.views.RecentsView, com.android.launcher3.PagedView, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        setTranslationYFactor(this.mTranslationYFactor);
    }

    public void setTranslationYFactor(float f) {
        this.mTranslationYFactor = f;
        setTranslationY(computeTranslationYForFactor(this.mTranslationYFactor));
    }

    public float computeTranslationYForFactor(float f) {
        return f * (getPaddingBottom() - getPaddingTop());
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        maybeDrawEmptyMessage(canvas);
        super.draw(canvas);
    }

    @Override // com.android.quickstep.views.RecentsView, com.android.launcher3.PagedView, android.view.ViewGroup
    public void onViewAdded(View view) {
        super.onViewAdded(view);
        updateEmptyMessage();
    }

    @Override // com.android.quickstep.views.RecentsView
    protected void onTaskStackUpdated() {
        updateEmptyMessage();
    }

    @Override // com.android.quickstep.views.RecentsView
    public AnimatorSet createAdjacentPageAnimForTaskLaunch(TaskView taskView, ClipAnimationHelper clipAnimationHelper) {
        AnimatorSet animatorSetCreateAdjacentPageAnimForTaskLaunch = super.createAdjacentPageAnimForTaskLaunch(taskView, clipAnimationHelper);
        if (!OverviewInteractionState.getInstance(this.mActivity).isSwipeUpGestureEnabled()) {
            return animatorSetCreateAdjacentPageAnimForTaskLaunch;
        }
        float shiftRange = 1.3059858f;
        if ((((Launcher) this.mActivity).getStateManager().getState().getVisibleElements((Launcher) this.mActivity) & 8) != 0) {
            float f = ((Launcher) this.mActivity).getDeviceProfile().heightPx;
            shiftRange = 1.0f + ((f - ((Launcher) this.mActivity).getAllAppsController().getShiftRange()) / f);
        }
        animatorSetCreateAdjacentPageAnimForTaskLaunch.play(ObjectAnimator.ofFloat(((Launcher) this.mActivity).getAllAppsController(), AllAppsTransitionController.ALL_APPS_PROGRESS, shiftRange));
        ObjectAnimator objectAnimatorOfInt = ObjectAnimator.ofInt((ScrimView) ((Launcher) this.mActivity).findViewById(R.id.scrim_view), ScrimView.DRAG_HANDLE_ALPHA, 0);
        objectAnimatorOfInt.setInterpolator(Interpolators.ACCEL_2);
        animatorSetCreateAdjacentPageAnimForTaskLaunch.play(objectAnimatorOfInt);
        return animatorSetCreateAdjacentPageAnimForTaskLaunch;
    }

    @Override // com.android.quickstep.views.RecentsView
    protected void getTaskSize(DeviceProfile deviceProfile, Rect rect) throws Resources.NotFoundException {
        LayoutUtils.calculateLauncherTaskSize(getContext(), deviceProfile, rect);
    }

    @Override // com.android.quickstep.views.RecentsView
    protected void onTaskLaunched(boolean z) {
        if (z) {
            ((Launcher) this.mActivity).getStateManager().goToState(LauncherState.NORMAL, false);
        } else {
            ((Launcher) this.mActivity).getAllAppsController().setState(((Launcher) this.mActivity).getStateManager().getState());
        }
        super.onTaskLaunched(z);
    }

    @Override // com.android.quickstep.views.RecentsView
    public boolean shouldUseMultiWindowTaskSizeStrategy() {
        return ((Launcher) this.mActivity).isInMultiWindowModeCompat();
    }
}
