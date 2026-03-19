package com.android.deskclock.widget;

import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import com.android.deskclock.Utils;

public final class EmptyViewController {
    private static final int ANIMATION_DURATION = 300;
    private static final boolean USE_TRANSITION_FRAMEWORK = Utils.isLOrLater();
    private final View mContentView;
    private final View mEmptyView;
    private final Transition mEmptyViewTransition;
    private boolean mIsEmpty;
    private final ViewGroup mMainLayout;

    public EmptyViewController(ViewGroup viewGroup, View view, View view2) {
        this.mMainLayout = viewGroup;
        this.mContentView = view;
        this.mEmptyView = view2;
        if (USE_TRANSITION_FRAMEWORK) {
            this.mEmptyViewTransition = new TransitionSet().setOrdering(1).addTarget(view).addTarget(view2).addTransition(new Fade(2)).addTransition(new Fade(1)).setDuration(300L);
        } else {
            this.mEmptyViewTransition = null;
        }
    }

    public void setEmpty(boolean z) {
        if (this.mIsEmpty == z) {
            return;
        }
        this.mIsEmpty = z;
        if (USE_TRANSITION_FRAMEWORK) {
            TransitionManager.beginDelayedTransition(this.mMainLayout, this.mEmptyViewTransition);
        }
        this.mEmptyView.setVisibility(this.mIsEmpty ? 0 : 8);
        this.mContentView.setVisibility(this.mIsEmpty ? 8 : 0);
    }
}
