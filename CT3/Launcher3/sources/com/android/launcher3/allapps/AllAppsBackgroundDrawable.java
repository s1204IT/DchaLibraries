package com.android.launcher3.allapps;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.android.launcher3.R;

public class AllAppsBackgroundDrawable extends Drawable {
    private ObjectAnimator mBackgroundAnim;
    private final TransformedImageDrawable mHand;
    private final int mHeight;
    private final TransformedImageDrawable[] mIcons;
    private final int mWidth;

    public AllAppsBackgroundDrawable(Context context) {
        Resources res = context.getResources();
        this.mHand = new TransformedImageDrawable(res, R.drawable.ic_all_apps_bg_hand, 0.575f, 0.1f, 1);
        this.mIcons = new TransformedImageDrawable[4];
        this.mIcons[0] = new TransformedImageDrawable(res, R.drawable.ic_all_apps_bg_icon_1, 0.375f, 0.0f, 1);
        this.mIcons[1] = new TransformedImageDrawable(res, R.drawable.ic_all_apps_bg_icon_2, 0.3125f, 0.25f, 1);
        this.mIcons[2] = new TransformedImageDrawable(res, R.drawable.ic_all_apps_bg_icon_3, 0.475f, 0.4f, 1);
        this.mIcons[3] = new TransformedImageDrawable(res, R.drawable.ic_all_apps_bg_icon_4, 0.7f, 0.125f, 1);
        this.mWidth = res.getDimensionPixelSize(R.dimen.all_apps_background_canvas_width);
        this.mHeight = res.getDimensionPixelSize(R.dimen.all_apps_background_canvas_height);
    }

    public void animateBgAlpha(float finalAlpha, int duration) {
        int finalAlphaI = (int) (255.0f * finalAlpha);
        if (getAlpha() == finalAlphaI) {
            return;
        }
        this.mBackgroundAnim = cancelAnimator(this.mBackgroundAnim);
        this.mBackgroundAnim = ObjectAnimator.ofInt(this, "alpha", finalAlphaI);
        this.mBackgroundAnim.setDuration(duration);
        this.mBackgroundAnim.start();
    }

    public void setBgAlpha(float finalAlpha) {
        int finalAlphaI = (int) (255.0f * finalAlpha);
        if (getAlpha() == finalAlphaI) {
            return;
        }
        this.mBackgroundAnim = cancelAnimator(this.mBackgroundAnim);
        setAlpha(finalAlphaI);
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mHeight;
    }

    @Override
    public void draw(Canvas canvas) {
        this.mHand.draw(canvas);
        for (int i = 0; i < this.mIcons.length; i++) {
            this.mIcons[i].draw(canvas);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        this.mHand.updateBounds(bounds);
        for (int i = 0; i < this.mIcons.length; i++) {
            this.mIcons[i].updateBounds(bounds);
        }
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        this.mHand.setAlpha(alpha);
        for (int i = 0; i < this.mIcons.length; i++) {
            this.mIcons[i].setAlpha(alpha);
        }
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return this.mHand.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    private ObjectAnimator cancelAnimator(ObjectAnimator animator) {
        if (animator != null) {
            animator.removeAllListeners();
            animator.cancel();
        }
        return null;
    }
}
