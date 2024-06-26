package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.RemotableViewMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;
import com.android.systemui.R;
@RemoteViews.RemoteView
/* loaded from: classes.dex */
public class AnimatedImageView extends ImageView {
    private boolean mAllowAnimation;
    AnimationDrawable mAnim;
    boolean mAttached;
    int mDrawableId;
    private final boolean mHasOverlappingRendering;

    public AnimatedImageView(Context context) {
        this(context, null);
    }

    public AnimatedImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mAllowAnimation = true;
        TypedArray obtainStyledAttributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.AnimatedImageView, 0, 0);
        try {
            this.mHasOverlappingRendering = obtainStyledAttributes.getBoolean(0, true);
        } finally {
            obtainStyledAttributes.recycle();
        }
    }

    public void setAllowAnimation(boolean z) {
        if (this.mAllowAnimation != z) {
            this.mAllowAnimation = z;
            updateAnim();
            if (!this.mAllowAnimation && this.mAnim != null) {
                this.mAnim.setVisible(getVisibility() == 0, true);
            }
        }
    }

    private void updateAnim() {
        Drawable drawable = getDrawable();
        if (this.mAttached && this.mAnim != null) {
            this.mAnim.stop();
        }
        if (drawable instanceof AnimationDrawable) {
            this.mAnim = (AnimationDrawable) drawable;
            if (isShown() && this.mAllowAnimation) {
                this.mAnim.start();
                return;
            }
            return;
        }
        this.mAnim = null;
    }

    @Override // android.widget.ImageView
    public void setImageDrawable(Drawable drawable) {
        if (drawable != null) {
            if (this.mDrawableId == drawable.hashCode()) {
                return;
            }
            this.mDrawableId = drawable.hashCode();
        } else {
            this.mDrawableId = 0;
        }
        super.setImageDrawable(drawable);
        updateAnim();
    }

    @Override // android.widget.ImageView
    @RemotableViewMethod
    public void setImageResource(int i) {
        if (this.mDrawableId == i) {
            return;
        }
        this.mDrawableId = i;
        super.setImageResource(i);
        updateAnim();
    }

    @Override // android.widget.ImageView, android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttached = true;
        updateAnim();
    }

    @Override // android.widget.ImageView, android.view.View
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mAnim != null) {
            this.mAnim.stop();
        }
        this.mAttached = false;
    }

    @Override // android.view.View
    protected void onVisibilityChanged(View view, int i) {
        super.onVisibilityChanged(view, i);
        if (this.mAnim != null) {
            if (isShown() && this.mAllowAnimation) {
                this.mAnim.start();
            } else {
                this.mAnim.stop();
            }
        }
    }

    @Override // android.widget.ImageView, android.view.View
    public boolean hasOverlappingRendering() {
        return this.mHasOverlappingRendering;
    }
}
