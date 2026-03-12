package com.android.systemui.recents.views;

import android.app.Activity;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.recents.RecentsConfiguration;

public class SystemBarScrimViews {
    RecentsConfiguration mConfig;
    boolean mHasNavBarScrim;
    boolean mHasStatusBarScrim;
    View mNavBarScrimView;
    boolean mShouldAnimateNavBarScrim;
    boolean mShouldAnimateStatusBarScrim;
    View mStatusBarScrimView;

    public SystemBarScrimViews(Activity activity, RecentsConfiguration config) {
        this.mConfig = config;
        this.mStatusBarScrimView = activity.findViewById(R.id.status_bar_scrim);
        this.mNavBarScrimView = activity.findViewById(R.id.nav_bar_scrim);
    }

    public void prepareEnterRecentsAnimation() {
        this.mHasNavBarScrim = this.mConfig.hasNavBarScrim();
        this.mShouldAnimateNavBarScrim = this.mConfig.shouldAnimateNavBarScrim();
        this.mHasStatusBarScrim = this.mConfig.hasStatusBarScrim();
        this.mShouldAnimateStatusBarScrim = this.mConfig.shouldAnimateStatusBarScrim();
        this.mNavBarScrimView.setVisibility((!this.mHasNavBarScrim || this.mShouldAnimateNavBarScrim) ? 4 : 0);
        this.mStatusBarScrimView.setVisibility((!this.mHasStatusBarScrim || this.mShouldAnimateStatusBarScrim) ? 4 : 0);
    }

    public void startEnterRecentsAnimation() {
        if (this.mHasStatusBarScrim && this.mShouldAnimateStatusBarScrim) {
            this.mStatusBarScrimView.setTranslationY(-this.mStatusBarScrimView.getMeasuredHeight());
            this.mStatusBarScrimView.animate().translationY(0.0f).setStartDelay(this.mConfig.launchedFromHome ? this.mConfig.transitionEnterFromHomeDelay : this.mConfig.transitionEnterFromAppDelay).setDuration(this.mConfig.navBarScrimEnterDuration).setInterpolator(this.mConfig.quintOutInterpolator).withStartAction(new Runnable() {
                @Override
                public void run() {
                    SystemBarScrimViews.this.mStatusBarScrimView.setVisibility(0);
                }
            }).start();
        }
        if (this.mHasNavBarScrim && this.mShouldAnimateNavBarScrim) {
            this.mNavBarScrimView.setTranslationY(this.mNavBarScrimView.getMeasuredHeight());
            this.mNavBarScrimView.animate().translationY(0.0f).setStartDelay(this.mConfig.launchedFromHome ? this.mConfig.transitionEnterFromHomeDelay : this.mConfig.transitionEnterFromAppDelay).setDuration(this.mConfig.navBarScrimEnterDuration).setInterpolator(this.mConfig.quintOutInterpolator).withStartAction(new Runnable() {
                @Override
                public void run() {
                    SystemBarScrimViews.this.mNavBarScrimView.setVisibility(0);
                }
            }).start();
        }
    }

    public void startExitRecentsAnimation() {
        if (this.mHasStatusBarScrim && this.mShouldAnimateStatusBarScrim) {
            this.mStatusBarScrimView.animate().translationY(-this.mStatusBarScrimView.getMeasuredHeight()).setStartDelay(0L).setDuration(this.mConfig.taskViewExitToAppDuration).setInterpolator(this.mConfig.fastOutSlowInInterpolator).start();
        }
        if (this.mHasNavBarScrim && this.mShouldAnimateNavBarScrim) {
            this.mNavBarScrimView.animate().translationY(this.mNavBarScrimView.getMeasuredHeight()).setStartDelay(0L).setDuration(this.mConfig.taskViewExitToAppDuration).setInterpolator(this.mConfig.fastOutSlowInInterpolator).start();
        }
    }
}
