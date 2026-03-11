package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

@RemoteViews.RemoteView
public class AnimatedImageView extends ImageView {
    AnimationDrawable mAnim;
    boolean mAttached;

    public AnimatedImageView(Context context) {
        super(context);
    }

    public AnimatedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void updateAnim() {
        Drawable drawable = getDrawable();
        if (this.mAttached && this.mAnim != null) {
            this.mAnim.stop();
        }
        if (drawable instanceof AnimationDrawable) {
            this.mAnim = (AnimationDrawable) drawable;
            if (isShown()) {
                this.mAnim.start();
                return;
            }
            return;
        }
        this.mAnim = null;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        updateAnim();
    }

    @Override
    @RemotableViewMethod
    public void setImageResource(int resid) {
        super.setImageResource(resid);
        updateAnim();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttached = true;
        updateAnim();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAnim != null) {
            this.mAnim.stop();
        }
        this.mAttached = false;
    }

    @Override
    protected void onVisibilityChanged(View changedView, int vis) {
        super.onVisibilityChanged(changedView, vis);
        if (this.mAnim != null) {
            if (isShown()) {
                this.mAnim.start();
            } else {
                this.mAnim.stop();
            }
        }
    }
}
