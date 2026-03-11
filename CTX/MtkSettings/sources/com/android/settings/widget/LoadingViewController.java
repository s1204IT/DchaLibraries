package com.android.settings.widget;

import android.R;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class LoadingViewController {
    public final View mContentView;
    public final View mLoadingView;
    private Runnable mShowLoadingContainerRunnable = new Runnable() {
        @Override
        public void run() {
            LoadingViewController.this.handleLoadingContainer(false, false);
        }
    };
    public final Handler mFgHandler = new Handler(Looper.getMainLooper());

    public LoadingViewController(View view, View view2) {
        this.mLoadingView = view;
        this.mContentView = view2;
    }

    public void showContent(boolean z) {
        this.mFgHandler.removeCallbacks(this.mShowLoadingContainerRunnable);
        handleLoadingContainer(true, z);
    }

    public void showLoadingViewDelayed() {
        this.mFgHandler.postDelayed(this.mShowLoadingContainerRunnable, 100L);
    }

    public void handleLoadingContainer(boolean z, boolean z2) {
        handleLoadingContainer(this.mLoadingView, this.mContentView, z, z2);
    }

    public static void handleLoadingContainer(View view, View view2, boolean z, boolean z2) {
        setViewShown(view, !z, z2);
        setViewShown(view2, z, z2);
    }

    private static void setViewShown(final View view, boolean z, boolean z2) {
        if (z2) {
            Animation animationLoadAnimation = AnimationUtils.loadAnimation(view.getContext(), z ? R.anim.fade_in : R.anim.fade_out);
            if (z) {
                view.setVisibility(0);
            } else {
                animationLoadAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(4);
                    }
                });
            }
            view.startAnimation(animationLoadAnimation);
            return;
        }
        view.clearAnimation();
        view.setVisibility(z ? 0 : 4);
    }
}
