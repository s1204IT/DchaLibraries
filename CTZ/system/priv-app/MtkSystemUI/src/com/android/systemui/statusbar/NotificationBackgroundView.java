package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.util.ArrayUtils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;

/* loaded from: classes.dex */
public class NotificationBackgroundView extends View {
    private int mActualHeight;
    private float mActualWidth;
    private Drawable mBackground;
    private int mBackgroundTop;
    private boolean mBottomAmountClips;
    private boolean mBottomIsRounded;
    private int mClipBottomAmount;
    private int mClipTopAmount;
    private float[] mCornerRadii;
    private float mDistanceToTopRoundness;
    private final boolean mDontModifyCorners;
    private int mDrawableAlpha;
    private boolean mExpandAnimationRunning;
    private boolean mIsPressedAllowed;
    private int mTintColor;
    private boolean mTopAmountRounded;

    public NotificationBackgroundView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mCornerRadii = new float[8];
        this.mBottomAmountClips = true;
        this.mDrawableAlpha = 255;
        this.mDontModifyCorners = getResources().getBoolean(R.bool.config_clipNotificationsToOutline);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.mClipTopAmount + this.mClipBottomAmount < this.mActualHeight - this.mBackgroundTop || this.mExpandAnimationRunning) {
            canvas.save();
            if (!this.mExpandAnimationRunning) {
                canvas.clipRect(0, this.mClipTopAmount, getWidth(), this.mActualHeight - this.mClipBottomAmount);
            }
            draw(canvas, this.mBackground);
            canvas.restore();
        }
    }

    private void draw(Canvas canvas, Drawable drawable) {
        if (drawable != null) {
            int i = this.mBackgroundTop;
            int i2 = this.mActualHeight;
            if (this.mBottomIsRounded && this.mBottomAmountClips && !this.mExpandAnimationRunning) {
                i2 -= this.mClipBottomAmount;
            }
            int width = 0;
            int width2 = getWidth();
            if (this.mExpandAnimationRunning) {
                width = (int) ((getWidth() - this.mActualWidth) / 2.0f);
                width2 = (int) (width + this.mActualWidth);
            }
            if (this.mTopAmountRounded) {
                int i3 = (int) (this.mClipTopAmount - this.mDistanceToTopRoundness);
                i += i3;
                if (i3 >= 0) {
                    i2 += i3;
                }
            }
            drawable.setBounds(width, i, width2, i2);
            drawable.draw(canvas);
        }
    }

    @Override // android.view.View
    protected boolean verifyDrawable(Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.mBackground;
    }

    @Override // android.view.View
    protected void drawableStateChanged() {
        setState(getDrawableState());
    }

    @Override // android.view.View
    public void drawableHotspotChanged(float f, float f2) {
        if (this.mBackground != null) {
            this.mBackground.setHotspot(f, f2);
        }
    }

    public void setCustomBackground(Drawable drawable) {
        if (this.mBackground != null) {
            this.mBackground.setCallback(null);
            unscheduleDrawable(this.mBackground);
        }
        this.mBackground = drawable;
        this.mBackground.mutate();
        if (this.mBackground != null) {
            this.mBackground.setCallback(this);
            setTint(this.mTintColor);
        }
        if (this.mBackground instanceof RippleDrawable) {
            ((RippleDrawable) this.mBackground).setForceSoftware(true);
        }
        updateBackgroundRadii();
        invalidate();
    }

    public void setCustomBackground(int i) {
        setCustomBackground(this.mContext.getDrawable(i));
    }

    public void setTint(int i) {
        if (i != 0) {
            this.mBackground.setColorFilter(i, PorterDuff.Mode.SRC_ATOP);
        } else {
            this.mBackground.clearColorFilter();
        }
        this.mTintColor = i;
        invalidate();
    }

    public void setActualHeight(int i) {
        if (this.mExpandAnimationRunning) {
            return;
        }
        this.mActualHeight = i;
        invalidate();
    }

    public int getActualHeight() {
        return this.mActualHeight;
    }

    public void setClipTopAmount(int i) {
        this.mClipTopAmount = i;
        invalidate();
    }

    public void setClipBottomAmount(int i) {
        this.mClipBottomAmount = i;
        invalidate();
    }

    public void setDistanceToTopRoundness(float f) {
        if (f != this.mDistanceToTopRoundness) {
            this.mTopAmountRounded = f >= 0.0f;
            this.mDistanceToTopRoundness = f;
            invalidate();
        }
    }

    @Override // android.view.View
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setState(int[] iArr) {
        if (this.mBackground != null && this.mBackground.isStateful()) {
            if (!this.mIsPressedAllowed) {
                iArr = ArrayUtils.removeInt(iArr, android.R.attr.state_pressed);
            }
            this.mBackground.setState(iArr);
        }
    }

    public void setRippleColor(int i) {
        if (this.mBackground instanceof RippleDrawable) {
            ((RippleDrawable) this.mBackground).setColor(ColorStateList.valueOf(i));
        }
    }

    public void setDrawableAlpha(int i) {
        this.mDrawableAlpha = i;
        if (this.mExpandAnimationRunning) {
            return;
        }
        this.mBackground.setAlpha(i);
    }

    public void setRoundness(float f, float f2) {
        if (f == this.mCornerRadii[0] && f2 == this.mCornerRadii[4]) {
            return;
        }
        this.mBottomIsRounded = f2 != 0.0f;
        this.mCornerRadii[0] = f;
        this.mCornerRadii[1] = f;
        this.mCornerRadii[2] = f;
        this.mCornerRadii[3] = f;
        this.mCornerRadii[4] = f2;
        this.mCornerRadii[5] = f2;
        this.mCornerRadii[6] = f2;
        this.mCornerRadii[7] = f2;
        updateBackgroundRadii();
    }

    public void setBottomAmountClips(boolean z) {
        if (z != this.mBottomAmountClips) {
            this.mBottomAmountClips = z;
            invalidate();
        }
    }

    private void updateBackgroundRadii() {
        if (!this.mDontModifyCorners && (this.mBackground instanceof LayerDrawable)) {
            ((GradientDrawable) ((LayerDrawable) this.mBackground).getDrawable(0)).setCornerRadii(this.mCornerRadii);
        }
    }

    public void setBackgroundTop(int i) {
        this.mBackgroundTop = i;
        invalidate();
    }

    public void setExpandAnimationParams(ActivityLaunchAnimator.ExpandAnimationParameters expandAnimationParameters) {
        this.mActualHeight = expandAnimationParameters.getHeight();
        this.mActualWidth = expandAnimationParameters.getWidth();
        this.mBackground.setAlpha((int) (this.mDrawableAlpha * (1.0f - Interpolators.ALPHA_IN.getInterpolation(expandAnimationParameters.getProgress(67L, 200L)))));
        invalidate();
    }

    public void setExpandAnimationRunning(boolean z) {
        this.mExpandAnimationRunning = z;
        if (this.mBackground instanceof LayerDrawable) {
            GradientDrawable gradientDrawable = (GradientDrawable) ((LayerDrawable) this.mBackground).getDrawable(0);
            gradientDrawable.setXfermode(z ? new PorterDuffXfermode(PorterDuff.Mode.SRC) : null);
            gradientDrawable.setAntiAlias(!z);
        }
        if (!this.mExpandAnimationRunning) {
            setDrawableAlpha(this.mDrawableAlpha);
        }
        invalidate();
    }

    public void setPressedAllowed(boolean z) {
        this.mIsPressedAllowed = z;
    }
}
