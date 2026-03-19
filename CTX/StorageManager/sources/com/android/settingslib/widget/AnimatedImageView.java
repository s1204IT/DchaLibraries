package com.android.settingslib.widget;

import android.content.Context;
import android.graphics.drawable.AnimatedRotateDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class AnimatedImageView extends ImageView {
    private boolean mAnimating;
    private AnimatedRotateDrawable mDrawable;

    public AnimatedImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    private void updateDrawable() {
        if (isShown() && this.mDrawable != null) {
            this.mDrawable.stop();
        }
        AnimatedRotateDrawable drawable = getDrawable();
        if (drawable instanceof AnimatedRotateDrawable) {
            this.mDrawable = drawable;
            this.mDrawable.setFramesCount(56);
            this.mDrawable.setFramesDuration(32);
            if (isShown() && this.mAnimating) {
                this.mDrawable.start();
                return;
            }
            return;
        }
        this.mDrawable = null;
    }

    private void updateAnimating() {
        if (this.mDrawable != null) {
            if (getVisibility() == 0 && this.mAnimating) {
                this.mDrawable.start();
            } else {
                this.mDrawable.stop();
            }
        }
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        updateDrawable();
    }

    @Override
    public void setImageResource(int i) {
        super.setImageResource(i);
        updateDrawable();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimating();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        updateAnimating();
    }

    @Override
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        updateAnimating();
    }
}
