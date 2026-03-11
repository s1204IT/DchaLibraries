package com.android.launcher3.allapps;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Property;
import android.view.ContextThemeWrapper;
import com.android.launcher3.LauncherAnimUtils;
import com.android.launcher3.R;
import com.android.launcher3.util.Themes;

public class AllAppsBackgroundDrawable extends Drawable {
    private ObjectAnimator mBackgroundAnim;
    protected final TransformedImageDrawable mHand;
    private final int mHeight;
    protected final TransformedImageDrawable[] mIcons;
    private final int mWidth;

    protected static class TransformedImageDrawable {
        private int mAlpha;
        private int mGravity;
        private Drawable mImage;
        private float mXPercent;
        private float mYPercent;

        public TransformedImageDrawable(Context context, int i, float f, float f2, int i2) {
            this.mImage = context.getDrawable(i);
            this.mXPercent = f;
            this.mYPercent = f2;
            this.mGravity = i2;
        }

        public void setAlpha(int i) {
            this.mImage.setAlpha(i);
            this.mAlpha = i;
        }

        public int getAlpha() {
            return this.mAlpha;
        }

        public void updateBounds(Rect rect) {
            int intrinsicWidth = this.mImage.getIntrinsicWidth();
            int intrinsicHeight = this.mImage.getIntrinsicHeight();
            int iWidth = rect.left + ((int) (this.mXPercent * rect.width()));
            int iHeight = rect.top + ((int) (this.mYPercent * rect.height()));
            if ((this.mGravity & 1) == 1) {
                iWidth -= intrinsicWidth / 2;
            }
            if ((this.mGravity & 16) == 16) {
                iHeight -= intrinsicHeight / 2;
            }
            this.mImage.setBounds(iWidth, iHeight, intrinsicWidth + iWidth, intrinsicHeight + iHeight);
        }

        public void draw(Canvas canvas) {
            this.mImage.draw(canvas);
        }

        public Rect getBounds() {
            return this.mImage.getBounds();
        }
    }

    public AllAppsBackgroundDrawable(Context context) {
        int i;
        Resources resources = context.getResources();
        this.mWidth = resources.getDimensionPixelSize(R.dimen.all_apps_background_canvas_width);
        this.mHeight = resources.getDimensionPixelSize(R.dimen.all_apps_background_canvas_height);
        if (Themes.getAttrBoolean(context, R.attr.isMainColorDark)) {
            i = 2131886081;
        } else {
            i = R.style.AllAppsEmptySearchBackground;
        }
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, i);
        this.mHand = new TransformedImageDrawable(contextThemeWrapper, R.drawable.ic_all_apps_bg_hand, 0.575f, 0.0f, 1);
        this.mIcons = new TransformedImageDrawable[4];
        this.mIcons[0] = new TransformedImageDrawable(contextThemeWrapper, R.drawable.ic_all_apps_bg_icon_1, 0.375f, 0.0f, 1);
        this.mIcons[1] = new TransformedImageDrawable(contextThemeWrapper, R.drawable.ic_all_apps_bg_icon_2, 0.3125f, 0.2f, 1);
        this.mIcons[2] = new TransformedImageDrawable(contextThemeWrapper, R.drawable.ic_all_apps_bg_icon_3, 0.475f, 0.26f, 1);
        this.mIcons[3] = new TransformedImageDrawable(contextThemeWrapper, R.drawable.ic_all_apps_bg_icon_4, 0.7f, 0.125f, 1);
    }

    public void animateBgAlpha(float f, int i) {
        int i2 = (int) (f * 255.0f);
        if (getAlpha() != i2) {
            this.mBackgroundAnim = cancelAnimator(this.mBackgroundAnim);
            this.mBackgroundAnim = ObjectAnimator.ofInt(this, (Property<AllAppsBackgroundDrawable, Integer>) LauncherAnimUtils.DRAWABLE_ALPHA, i2);
            this.mBackgroundAnim.setDuration(i);
            this.mBackgroundAnim.start();
        }
    }

    public void setBgAlpha(float f) {
        int i = (int) (f * 255.0f);
        if (getAlpha() != i) {
            this.mBackgroundAnim = cancelAnimator(this.mBackgroundAnim);
            setAlpha(i);
        }
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
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        this.mHand.updateBounds(rect);
        for (int i = 0; i < this.mIcons.length; i++) {
            this.mIcons[i].updateBounds(rect);
        }
        invalidateSelf();
    }

    @Override
    public void setAlpha(int i) {
        this.mHand.setAlpha(i);
        for (int i2 = 0; i2 < this.mIcons.length; i2++) {
            this.mIcons[i2].setAlpha(i);
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

    private ObjectAnimator cancelAnimator(ObjectAnimator objectAnimator) {
        if (objectAnimator != null) {
            objectAnimator.cancel();
            return null;
        }
        return null;
    }
}
