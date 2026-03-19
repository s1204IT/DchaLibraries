package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {
    private final AppColorAnimationListener mAppColorAnimationListener = new AppColorAnimationListener();
    private ValueAnimator mAppColorAnimator;
    private ColorDrawable mBackground;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        getWindow().getDecorView().setSystemUiVisibility(1792);
        adjustAppColor(ThemeUtils.resolveColor(this, android.R.attr.windowBackground), false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adjustAppColor(ThemeUtils.resolveColor(this, android.R.attr.windowBackground), false);
    }

    protected void adjustAppColor(@ColorInt int i, boolean z) {
        if (this.mBackground == null) {
            this.mBackground = new ColorDrawable(i);
            getWindow().setBackgroundDrawable(this.mBackground);
        }
        if (this.mAppColorAnimator != null) {
            this.mAppColorAnimator.cancel();
        }
        int color = this.mBackground.getColor();
        if (color != i) {
            if (z) {
                this.mAppColorAnimator = ValueAnimator.ofObject(AnimatorUtils.ARGB_EVALUATOR, Integer.valueOf(color), Integer.valueOf(i)).setDuration(3000L);
                this.mAppColorAnimator.addUpdateListener(this.mAppColorAnimationListener);
                this.mAppColorAnimator.addListener(this.mAppColorAnimationListener);
                this.mAppColorAnimator.start();
                return;
            }
            setAppColor(i);
        }
    }

    private void setAppColor(@ColorInt int i) {
        this.mBackground.setColor(i);
    }

    private final class AppColorAnimationListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
        private AppColorAnimationListener() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            BaseActivity.this.setAppColor(((Integer) valueAnimator.getAnimatedValue()).intValue());
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if (BaseActivity.this.mAppColorAnimator == animator) {
                BaseActivity.this.mAppColorAnimator = null;
            }
        }
    }
}
