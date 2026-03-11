package com.android.launcher3;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import com.android.launcher3.FastBitmapDrawable;

class PreloadIconDrawable extends Drawable {
    private static final Rect sTempRect = new Rect();
    private ObjectAnimator mAnimator;
    private Drawable mBgDrawable;
    final Drawable mIcon;
    private boolean mIndicatorRectDirty;
    private int mRingOutset;
    private final RectF mIndicatorRect = new RectF();
    private int mIndicatorColor = 0;
    private int mProgress = 0;
    private float mAnimationProgress = -1.0f;
    private final Paint mPaint = new Paint(1);

    public PreloadIconDrawable(Drawable icon, Resources.Theme theme) {
        this.mIcon = icon;
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mPaint.setStrokeCap(Paint.Cap.ROUND);
        setBounds(icon.getBounds());
        applyPreloaderTheme(theme);
        onLevelChange(0);
    }

    public void applyPreloaderTheme(Resources.Theme t) {
        TypedArray ta = t.obtainStyledAttributes(R.styleable.PreloadIconDrawable);
        this.mBgDrawable = ta.getDrawable(0);
        this.mBgDrawable.setFilterBitmap(true);
        this.mPaint.setStrokeWidth(ta.getDimension(2, 0.0f));
        this.mRingOutset = ta.getDimensionPixelSize(1, 0);
        ta.recycle();
        onBoundsChange(getBounds());
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        this.mIcon.setBounds(bounds);
        if (this.mBgDrawable != null) {
            sTempRect.set(bounds);
            sTempRect.inset(-this.mRingOutset, -this.mRingOutset);
            this.mBgDrawable.setBounds(sTempRect);
        }
        this.mIndicatorRectDirty = true;
    }

    public int getOutset() {
        return this.mRingOutset;
    }

    private void initIndicatorRect() {
        Drawable d = this.mBgDrawable;
        Rect bounds = d.getBounds();
        d.getPadding(sTempRect);
        float paddingScaleX = bounds.width() / d.getIntrinsicWidth();
        float paddingScaleY = bounds.height() / d.getIntrinsicHeight();
        this.mIndicatorRect.set(bounds.left + (sTempRect.left * paddingScaleX), bounds.top + (sTempRect.top * paddingScaleY), bounds.right - (sTempRect.right * paddingScaleX), bounds.bottom - (sTempRect.bottom * paddingScaleY));
        float inset = this.mPaint.getStrokeWidth() / 2.0f;
        this.mIndicatorRect.inset(inset, inset);
        this.mIndicatorRectDirty = false;
    }

    @Override
    public void draw(Canvas canvas) {
        float iconScale;
        Rect r = new Rect(getBounds());
        if (canvas.getClipBounds(sTempRect) && !Rect.intersects(sTempRect, r)) {
            return;
        }
        if (this.mIndicatorRectDirty) {
            initIndicatorRect();
        }
        if (this.mAnimationProgress >= 0.0f && this.mAnimationProgress < 1.0f) {
            this.mPaint.setAlpha((int) ((1.0f - this.mAnimationProgress) * 255.0f));
            this.mBgDrawable.setAlpha(this.mPaint.getAlpha());
            this.mBgDrawable.draw(canvas);
            canvas.drawOval(this.mIndicatorRect, this.mPaint);
            iconScale = 0.5f + (this.mAnimationProgress * 0.5f);
        } else if (this.mAnimationProgress == -1.0f) {
            this.mPaint.setAlpha(255);
            iconScale = 0.5f;
            this.mBgDrawable.setAlpha(255);
            this.mBgDrawable.draw(canvas);
            if (this.mProgress >= 100) {
                canvas.drawOval(this.mIndicatorRect, this.mPaint);
            } else if (this.mProgress > 0) {
                canvas.drawArc(this.mIndicatorRect, -90.0f, 3.6f * this.mProgress, false, this.mPaint);
            }
        } else {
            iconScale = 1.0f;
        }
        canvas.save();
        canvas.scale(iconScale, iconScale, r.exactCenterX(), r.exactCenterY());
        this.mIcon.draw(canvas);
        canvas.restore();
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setAlpha(int alpha) {
        this.mIcon.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        this.mIcon.setColorFilter(cf);
    }

    @Override
    protected boolean onLevelChange(int level) {
        this.mProgress = level;
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
            this.mAnimator = null;
        }
        this.mAnimationProgress = -1.0f;
        if (level > 0) {
            this.mPaint.setColor(getIndicatorColor());
        }
        if (this.mIcon instanceof FastBitmapDrawable) {
            ((FastBitmapDrawable) this.mIcon).setState(level <= 0 ? FastBitmapDrawable.State.DISABLED : FastBitmapDrawable.State.NORMAL);
        }
        invalidateSelf();
        return true;
    }

    public void maybePerformFinishedAnimation() {
        if (this.mAnimationProgress > -1.0f) {
            return;
        }
        if (this.mAnimator != null) {
            this.mAnimator.cancel();
        }
        setAnimationProgress(0.0f);
        this.mAnimator = ObjectAnimator.ofFloat(this, "animationProgress", 0.0f, 1.0f);
        this.mAnimator.start();
    }

    public void setAnimationProgress(float progress) {
        if (progress == this.mAnimationProgress) {
            return;
        }
        this.mAnimationProgress = progress;
        invalidateSelf();
    }

    public float getAnimationProgress() {
        return this.mAnimationProgress;
    }

    public boolean hasNotCompleted() {
        return this.mAnimationProgress < 1.0f;
    }

    @Override
    public int getIntrinsicHeight() {
        return this.mIcon.getIntrinsicHeight();
    }

    @Override
    public int getIntrinsicWidth() {
        return this.mIcon.getIntrinsicWidth();
    }

    private int getIndicatorColor() {
        if (this.mIndicatorColor != 0) {
            return this.mIndicatorColor;
        }
        if (!(this.mIcon instanceof FastBitmapDrawable)) {
            this.mIndicatorColor = -16738680;
            return this.mIndicatorColor;
        }
        this.mIndicatorColor = Utilities.findDominantColorByHue(((FastBitmapDrawable) this.mIcon).getBitmap(), 20);
        float[] hsv = new float[3];
        Color.colorToHSV(this.mIndicatorColor, hsv);
        if (hsv[1] < 0.2f) {
            this.mIndicatorColor = -16738680;
            return this.mIndicatorColor;
        }
        hsv[2] = Math.max(0.6f, hsv[2]);
        this.mIndicatorColor = Color.HSVToColor(hsv);
        return this.mIndicatorColor;
    }
}
