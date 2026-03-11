package com.android.systemui.recents.views;

import android.content.Context;
import android.view.View;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsActivity;
import com.android.systemui.recents.events.activity.ConfigurationChangedEvent;
import com.android.systemui.recents.events.activity.DismissRecentsToHomeAnimationStarted;
import com.android.systemui.recents.events.activity.EnterRecentsWindowAnimationCompletedEvent;
import com.android.systemui.recents.events.activity.MultiWindowStateChangedEvent;
import com.android.systemui.recents.events.ui.DismissAllTaskViewsEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndCancelledEvent;
import com.android.systemui.recents.events.ui.dragndrop.DragEndEvent;
import com.android.systemui.recents.model.TaskStack;

public class SystemBarScrimViews {
    private Context mContext;
    private boolean mHasDockedTasks;
    private boolean mHasNavBarScrim;
    private boolean mHasTransposedNavBar;
    private int mNavBarScrimEnterDuration;
    private View mNavBarScrimView;
    private boolean mShouldAnimateNavBarScrim;

    public SystemBarScrimViews(RecentsActivity activity) {
        this.mContext = activity;
        this.mNavBarScrimView = activity.findViewById(R.id.nav_bar_scrim);
        this.mNavBarScrimView.forceHasOverlappingRendering(false);
        this.mNavBarScrimEnterDuration = activity.getResources().getInteger(R.integer.recents_nav_bar_scrim_enter_duration);
        this.mHasNavBarScrim = Recents.getSystemServices().hasTransposedNavigationBar();
        this.mHasDockedTasks = Recents.getSystemServices().hasDockedTask();
    }

    public void updateNavBarScrim(boolean animateNavBarScrim, boolean hasStackTasks, AnimationProps animation) {
        prepareEnterRecentsAnimation(isNavBarScrimRequired(hasStackTasks), animateNavBarScrim);
        if (!animateNavBarScrim || animation == null) {
            return;
        }
        animateNavBarScrimVisibility(true, animation);
    }

    private void prepareEnterRecentsAnimation(boolean hasNavBarScrim, boolean animateNavBarScrim) {
        this.mHasNavBarScrim = hasNavBarScrim;
        this.mShouldAnimateNavBarScrim = animateNavBarScrim;
        this.mNavBarScrimView.setVisibility((!this.mHasNavBarScrim || this.mShouldAnimateNavBarScrim) ? 4 : 0);
    }

    private void animateNavBarScrimVisibility(boolean visible, AnimationProps animation) {
        int toY = 0;
        if (visible) {
            this.mNavBarScrimView.setVisibility(0);
            this.mNavBarScrimView.setTranslationY(this.mNavBarScrimView.getMeasuredHeight());
        } else {
            toY = this.mNavBarScrimView.getMeasuredHeight();
        }
        if (animation != AnimationProps.IMMEDIATE) {
            this.mNavBarScrimView.animate().translationY(toY).setDuration(animation.getDuration(6)).setInterpolator(animation.getInterpolator(6)).start();
        } else {
            this.mNavBarScrimView.setTranslationY(toY);
        }
    }

    private boolean isNavBarScrimRequired(boolean hasStackTasks) {
        return (!hasStackTasks || this.mHasTransposedNavBar || this.mHasDockedTasks) ? false : true;
    }

    public final void onBusEvent(EnterRecentsWindowAnimationCompletedEvent event) {
        AnimationProps animation;
        if (!this.mHasNavBarScrim) {
            return;
        }
        if (this.mShouldAnimateNavBarScrim) {
            animation = new AnimationProps().setDuration(6, this.mNavBarScrimEnterDuration).setInterpolator(6, Interpolators.DECELERATE_QUINT);
        } else {
            animation = AnimationProps.IMMEDIATE;
        }
        animateNavBarScrimVisibility(true, animation);
    }

    public final void onBusEvent(DismissRecentsToHomeAnimationStarted event) {
        if (!this.mHasNavBarScrim) {
            return;
        }
        AnimationProps animation = createBoundsAnimation(200);
        animateNavBarScrimVisibility(false, animation);
    }

    public final void onBusEvent(DismissAllTaskViewsEvent event) {
        if (!this.mHasNavBarScrim) {
            return;
        }
        AnimationProps animation = createBoundsAnimation(200);
        animateNavBarScrimVisibility(false, animation);
    }

    public final void onBusEvent(ConfigurationChangedEvent event) {
        if (event.fromDeviceOrientationChange) {
            this.mHasNavBarScrim = Recents.getSystemServices().hasTransposedNavigationBar();
        }
        animateScrimToCurrentNavBarState(event.hasStackTasks);
    }

    public final void onBusEvent(MultiWindowStateChangedEvent event) {
        this.mHasDockedTasks = event.inMultiWindow;
        animateScrimToCurrentNavBarState(event.stack.getStackTaskCount() > 0);
    }

    public final void onBusEvent(DragEndEvent event) {
        if (!(event.dropTarget instanceof TaskStack.DockState)) {
            return;
        }
        animateScrimToCurrentNavBarState(false);
    }

    public final void onBusEvent(DragEndCancelledEvent event) {
        animateScrimToCurrentNavBarState(event.stack.getStackTaskCount() > 0);
    }

    private void animateScrimToCurrentNavBarState(boolean hasStackTasks) {
        AnimationProps animation;
        boolean hasNavBarScrim = isNavBarScrimRequired(hasStackTasks);
        if (this.mHasNavBarScrim != hasNavBarScrim) {
            if (hasNavBarScrim) {
                animation = createBoundsAnimation(150);
            } else {
                animation = AnimationProps.IMMEDIATE;
            }
            animateNavBarScrimVisibility(hasNavBarScrim, animation);
        }
        this.mHasNavBarScrim = hasNavBarScrim;
    }

    private AnimationProps createBoundsAnimation(int duration) {
        return new AnimationProps().setDuration(6, duration).setInterpolator(6, Interpolators.FAST_OUT_SLOW_IN);
    }
}
