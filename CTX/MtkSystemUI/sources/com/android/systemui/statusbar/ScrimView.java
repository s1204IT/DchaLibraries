package com.android.systemui.statusbar;

import android.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.colorextraction.drawable.GradientDrawable;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.ConfigurationController;

public class ScrimView extends View implements ConfigurationController.ConfigurationListener {
    private ValueAnimator mAlphaAnimator;
    private ValueAnimator.AnimatorUpdateListener mAlphaUpdateListener;
    private Runnable mChangeRunnable;
    private AnimatorListenerAdapter mClearAnimatorListener;
    private PorterDuffColorFilter mColorFilter;
    private final ColorExtractor.GradientColors mColors;
    private int mCornerRadius;
    private int mDensity;
    private boolean mDrawAsSrc;
    private Drawable mDrawable;
    private Rect mExcludedRect;
    private boolean mHasExcludedArea;
    private int mTintColor;
    private float mViewAlpha;

    public static void lambda$new$0(ScrimView scrimView, ValueAnimator valueAnimator) {
        if (scrimView.mDrawable == null) {
            Log.w("ScrimView", "Trying to animate null drawable");
        } else {
            scrimView.mDrawable.setAlpha((int) (255.0f * ((Float) valueAnimator.getAnimatedValue()).floatValue()));
        }
    }

    public ScrimView(Context context) {
        this(context, null);
    }

    public ScrimView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ScrimView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public ScrimView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mViewAlpha = 1.0f;
        this.mExcludedRect = new Rect();
        this.mAlphaUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                ScrimView.lambda$new$0(this.f$0, valueAnimator);
            }
        };
        this.mClearAnimatorListener = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                ScrimView.this.mAlphaAnimator = null;
            }
        };
        this.mDrawable = new GradientDrawable(context);
        this.mDrawable.setCallback(this);
        this.mColors = new ColorExtractor.GradientColors();
        updateScreenSize();
        updateColorWithTint(false);
        initView();
        this.mDensity = this.mContext.getResources().getConfiguration().densityDpi;
    }

    private void initView() {
        this.mCornerRadius = getResources().getDimensionPixelSize(Utils.getThemeAttr(this.mContext, R.attr.dialogCornerRadius));
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        int i = configuration.densityDpi;
        if (this.mDensity != i) {
            this.mDensity = i;
            initView();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mDrawAsSrc || this.mDrawable.getAlpha() > 0) {
            if (!this.mHasExcludedArea) {
                this.mDrawable.draw(canvas);
                return;
            }
            if (this.mExcludedRect.top > 0) {
                canvas.save();
                canvas.clipRect(0, 0, getWidth(), this.mExcludedRect.top);
                this.mDrawable.draw(canvas);
                canvas.restore();
            }
            if (this.mExcludedRect.left > 0) {
                canvas.save();
                canvas.clipRect(0, this.mExcludedRect.top, this.mExcludedRect.left, this.mExcludedRect.bottom);
                this.mDrawable.draw(canvas);
                canvas.restore();
            }
            if (this.mExcludedRect.right < getWidth()) {
                canvas.save();
                canvas.clipRect(this.mExcludedRect.right, this.mExcludedRect.top, getWidth(), this.mExcludedRect.bottom);
                this.mDrawable.draw(canvas);
                canvas.restore();
            }
            if (this.mExcludedRect.bottom < getHeight()) {
                canvas.save();
                canvas.clipRect(0, this.mExcludedRect.bottom, getWidth(), getHeight());
                this.mDrawable.draw(canvas);
                canvas.restore();
            }
            canvas.save();
            canvas.clipRect(this.mExcludedRect.left, this.mExcludedRect.top, this.mExcludedRect.left + this.mCornerRadius, this.mExcludedRect.top + this.mCornerRadius);
            this.mDrawable.draw(canvas);
            canvas.restore();
            canvas.save();
            canvas.clipRect(this.mExcludedRect.right - this.mCornerRadius, this.mExcludedRect.top, this.mExcludedRect.right, this.mExcludedRect.top + this.mCornerRadius);
            this.mDrawable.draw(canvas);
            canvas.restore();
            canvas.save();
            canvas.clipRect(this.mExcludedRect.left, this.mExcludedRect.bottom - this.mCornerRadius, this.mExcludedRect.left + this.mCornerRadius, this.mExcludedRect.bottom);
            this.mDrawable.draw(canvas);
            canvas.restore();
            canvas.save();
            canvas.clipRect(this.mExcludedRect.right - this.mCornerRadius, this.mExcludedRect.bottom - this.mCornerRadius, this.mExcludedRect.right, this.mExcludedRect.bottom);
            this.mDrawable.draw(canvas);
            canvas.restore();
        }
    }

    public void setDrawable(Drawable drawable) {
        this.mDrawable = drawable;
        this.mDrawable.setCallback(this);
        this.mDrawable.setBounds(getLeft(), getTop(), getRight(), getBottom());
        this.mDrawable.setAlpha((int) (255.0f * this.mViewAlpha));
        setDrawAsSrc(this.mDrawAsSrc);
        updateScreenSize();
        invalidate();
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        super.invalidateDrawable(drawable);
        if (drawable == this.mDrawable) {
            invalidate();
        }
    }

    public void setDrawAsSrc(boolean z) {
        this.mDrawAsSrc = z;
        this.mDrawable.setXfermode(new PorterDuffXfermode(z ? PorterDuff.Mode.SRC : PorterDuff.Mode.SRC_OVER));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (z) {
            this.mDrawable.setBounds(i, i2, i3, i4);
            invalidate();
        }
    }

    public void setColors(ColorExtractor.GradientColors gradientColors, boolean z) {
        if (gradientColors == null) {
            throw new IllegalArgumentException("Colors cannot be null");
        }
        if (this.mColors.equals(gradientColors)) {
            return;
        }
        this.mColors.set(gradientColors);
        updateColorWithTint(z);
    }

    @VisibleForTesting
    Drawable getDrawable() {
        return this.mDrawable;
    }

    public ColorExtractor.GradientColors getColors() {
        return this.mColors;
    }

    public void setTint(int i) {
        setTint(i, false);
    }

    public void setTint(int i, boolean z) {
        if (this.mTintColor == i) {
            return;
        }
        this.mTintColor = i;
        updateColorWithTint(z);
    }

    private void updateColorWithTint(boolean z) {
        if (this.mDrawable instanceof GradientDrawable) {
            float fAlpha = Color.alpha(this.mTintColor) / 255.0f;
            this.mDrawable.setColors(ColorUtils.blendARGB(this.mColors.getMainColor(), this.mTintColor, fAlpha), ColorUtils.blendARGB(this.mColors.getSecondaryColor(), this.mTintColor, fAlpha), z);
        } else {
            if (this.mColorFilter == null) {
                this.mColorFilter = new PorterDuffColorFilter(this.mTintColor, PorterDuff.Mode.SRC_OVER);
            } else {
                this.mColorFilter.setColor(this.mTintColor);
            }
            this.mDrawable.setColorFilter(Color.alpha(this.mTintColor) == 0 ? null : this.mColorFilter);
            this.mDrawable.invalidateSelf();
        }
        if (this.mChangeRunnable != null) {
            this.mChangeRunnable.run();
        }
    }

    public int getTint() {
        return this.mTintColor;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setViewAlpha(float f) {
        if (f != this.mViewAlpha) {
            this.mViewAlpha = f;
            if (this.mAlphaAnimator != null) {
                this.mAlphaAnimator.cancel();
            }
            this.mDrawable.setAlpha((int) (255.0f * f));
            if (this.mChangeRunnable != null) {
                this.mChangeRunnable.run();
            }
        }
    }

    public float getViewAlpha() {
        return this.mViewAlpha;
    }

    public void setExcludedArea(Rect rect) {
        boolean z = false;
        if (rect != null) {
            int iMax = Math.max(rect.left, 0);
            int iMax2 = Math.max(rect.top, 0);
            int iMin = Math.min(rect.right, getWidth());
            int iMin2 = Math.min(rect.bottom, getHeight());
            this.mExcludedRect.set(iMax, iMax2, iMin, iMin2);
            if (iMax < iMin && iMax2 < iMin2) {
                z = true;
            }
            this.mHasExcludedArea = z;
            invalidate();
            return;
        }
        this.mHasExcludedArea = false;
        invalidate();
    }

    public void setChangeRunnable(Runnable runnable) {
        this.mChangeRunnable = runnable;
    }

    @Override
    public void onConfigChanged(Configuration configuration) {
        updateScreenSize();
    }

    private void updateScreenSize() {
        if (this.mDrawable instanceof GradientDrawable) {
            WindowManager windowManager = (WindowManager) this.mContext.getSystemService(WindowManager.class);
            if (windowManager == null) {
                Log.w("ScrimView", "Can't resize gradient drawable to fit the screen");
                return;
            }
            Display defaultDisplay = windowManager.getDefaultDisplay();
            if (defaultDisplay != null) {
                Point point = new Point();
                defaultDisplay.getRealSize(point);
                this.mDrawable.setScreenSize(point.x, point.y);
            }
        }
    }
}
