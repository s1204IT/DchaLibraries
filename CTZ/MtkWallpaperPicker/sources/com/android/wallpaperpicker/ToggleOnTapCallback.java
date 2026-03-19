package com.android.wallpaperpicker;

import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import com.android.wallpaperpicker.CropView;

public class ToggleOnTapCallback implements CropView.TouchCallback {
    private ViewPropertyAnimator mAnim;
    private boolean mIgnoreNextTap;
    private final View mViewtoToggle;

    public ToggleOnTapCallback(View view) {
        this.mViewtoToggle = view;
    }

    @Override
    public void onTouchDown() {
        if (this.mAnim != null) {
            this.mAnim.cancel();
        }
        if (this.mViewtoToggle.getAlpha() == 1.0f) {
            this.mIgnoreNextTap = true;
        }
        this.mAnim = this.mViewtoToggle.animate();
        this.mAnim.alpha(0.0f).setDuration(150L).withEndAction(new Runnable() {
            @Override
            public void run() {
                ToggleOnTapCallback.this.mViewtoToggle.setVisibility(4);
            }
        });
        this.mAnim.setInterpolator(new AccelerateInterpolator(0.75f));
        this.mAnim.start();
    }

    @Override
    public void onTouchUp() {
        this.mIgnoreNextTap = false;
    }

    @Override
    public void onTap() {
        boolean z = this.mIgnoreNextTap;
        this.mIgnoreNextTap = false;
        if (!z) {
            if (this.mAnim != null) {
                this.mAnim.cancel();
            }
            this.mViewtoToggle.setVisibility(0);
            this.mAnim = this.mViewtoToggle.animate();
            this.mAnim.alpha(1.0f).setDuration(150L).setInterpolator(new DecelerateInterpolator(0.75f));
            this.mAnim.start();
        }
    }
}
