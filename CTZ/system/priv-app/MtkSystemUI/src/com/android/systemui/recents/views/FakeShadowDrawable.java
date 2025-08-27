package com.android.systemui.recents.views;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsConfiguration;

/* loaded from: classes.dex */
class FakeShadowDrawable extends Drawable {
    static final double COS_45 = Math.cos(Math.toRadians(45.0d));
    final RectF mCardBounds;
    float mCornerRadius;
    Paint mCornerShadowPaint;
    Path mCornerShadowPath;
    Paint mEdgeShadowPaint;
    final float mInsetShadow;
    float mMaxShadowSize;
    float mRawMaxShadowSize;
    float mRawShadowSize;
    private final int mShadowEndColor;
    float mShadowSize;
    private final int mShadowStartColor;
    private boolean mDirty = true;
    private boolean mAddPaddingForCorners = true;
    private boolean mPrintedShadowClipWarning = false;

    public FakeShadowDrawable(Resources resources, RecentsConfiguration recentsConfiguration) {
        float dimensionPixelSize;
        this.mShadowStartColor = resources.getColor(R.color.fake_shadow_start_color);
        this.mShadowEndColor = resources.getColor(R.color.fake_shadow_end_color);
        this.mInsetShadow = resources.getDimension(R.dimen.fake_shadow_inset);
        setShadowSize(resources.getDimensionPixelSize(R.dimen.fake_shadow_size), resources.getDimensionPixelSize(R.dimen.fake_shadow_size));
        this.mCornerShadowPaint = new Paint(5);
        this.mCornerShadowPaint.setStyle(Paint.Style.FILL);
        this.mCornerShadowPaint.setDither(true);
        if (Recents.getConfiguration().isGridEnabled) {
            dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.recents_grid_task_view_rounded_corners_radius);
        } else {
            dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.recents_task_view_rounded_corners_radius);
        }
        this.mCornerRadius = dimensionPixelSize;
        this.mCardBounds = new RectF();
        this.mEdgeShadowPaint = new Paint(this.mCornerShadowPaint);
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        this.mCornerShadowPaint.setAlpha(i);
        this.mEdgeShadowPaint.setAlpha(i);
    }

    @Override // android.graphics.drawable.Drawable
    protected void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        this.mDirty = true;
    }

    void setShadowSize(float f, float f2) {
        if (f < 0.0f || f2 < 0.0f) {
            throw new IllegalArgumentException("invalid shadow size");
        }
        if (f > f2) {
            if (!this.mPrintedShadowClipWarning) {
                Log.w("CardView", "Shadow size is being clipped by the max shadow size. See {CardView#setMaxCardElevation}.");
                this.mPrintedShadowClipWarning = true;
            }
            f = f2;
        }
        if (this.mRawShadowSize == f && this.mRawMaxShadowSize == f2) {
            return;
        }
        this.mRawShadowSize = f;
        this.mRawMaxShadowSize = f2;
        this.mShadowSize = (f * 1.5f) + this.mInsetShadow;
        this.mMaxShadowSize = f2 + this.mInsetShadow;
        this.mDirty = true;
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public boolean getPadding(Rect rect) {
        int iCeil = (int) Math.ceil(calculateVerticalPadding(this.mRawMaxShadowSize, this.mCornerRadius, this.mAddPaddingForCorners));
        int iCeil2 = (int) Math.ceil(calculateHorizontalPadding(this.mRawMaxShadowSize, this.mCornerRadius, this.mAddPaddingForCorners));
        rect.set(iCeil2, iCeil, iCeil2, iCeil);
        return true;
    }

    static float calculateVerticalPadding(float f, float f2, boolean z) {
        if (z) {
            return (float) ((f * 1.5f) + ((1.0d - COS_45) * f2));
        }
        return f * 1.5f;
    }

    static float calculateHorizontalPadding(float f, float f2, boolean z) {
        if (z) {
            return (float) (f + ((1.0d - COS_45) * f2));
        }
        return f;
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.mCornerShadowPaint.setColorFilter(colorFilter);
        this.mEdgeShadowPaint.setColorFilter(colorFilter);
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -1;
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        if (this.mDirty) {
            buildComponents(getBounds());
            this.mDirty = false;
        }
        canvas.translate(0.0f, this.mRawShadowSize / 4.0f);
        drawShadow(canvas);
        canvas.translate(0.0f, (-this.mRawShadowSize) / 4.0f);
    }

    private void drawShadow(Canvas canvas) {
        float f = (-this.mCornerRadius) - this.mShadowSize;
        float f2 = this.mCornerRadius + this.mInsetShadow + (this.mRawShadowSize / 2.0f);
        float f3 = 2.0f * f2;
        boolean z = this.mCardBounds.width() - f3 > 0.0f;
        boolean z2 = this.mCardBounds.height() - f3 > 0.0f;
        int iSave = canvas.save();
        canvas.translate(this.mCardBounds.left + f2, this.mCardBounds.top + f2);
        canvas.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (z) {
            canvas.drawRect(0.0f, f, this.mCardBounds.width() - f3, -this.mCornerRadius, this.mEdgeShadowPaint);
        }
        canvas.restoreToCount(iSave);
        int iSave2 = canvas.save();
        canvas.translate(this.mCardBounds.right - f2, this.mCardBounds.bottom - f2);
        canvas.rotate(180.0f);
        canvas.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (z) {
            canvas.drawRect(0.0f, f, this.mCardBounds.width() - f3, (-this.mCornerRadius) + this.mShadowSize, this.mEdgeShadowPaint);
        }
        canvas.restoreToCount(iSave2);
        int iSave3 = canvas.save();
        canvas.translate(this.mCardBounds.left + f2, this.mCardBounds.bottom - f2);
        canvas.rotate(270.0f);
        canvas.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (z2) {
            canvas.drawRect(0.0f, f, this.mCardBounds.height() - f3, -this.mCornerRadius, this.mEdgeShadowPaint);
        }
        canvas.restoreToCount(iSave3);
        int iSave4 = canvas.save();
        canvas.translate(this.mCardBounds.right - f2, this.mCardBounds.top + f2);
        canvas.rotate(90.0f);
        canvas.drawPath(this.mCornerShadowPath, this.mCornerShadowPaint);
        if (z2) {
            canvas.drawRect(0.0f, f, this.mCardBounds.height() - f3, -this.mCornerRadius, this.mEdgeShadowPaint);
        }
        canvas.restoreToCount(iSave4);
    }

    private void buildShadowCorners() {
        RectF rectF = new RectF(-this.mCornerRadius, -this.mCornerRadius, this.mCornerRadius, this.mCornerRadius);
        RectF rectF2 = new RectF(rectF);
        rectF2.inset(-this.mShadowSize, -this.mShadowSize);
        if (this.mCornerShadowPath == null) {
            this.mCornerShadowPath = new Path();
        } else {
            this.mCornerShadowPath.reset();
        }
        this.mCornerShadowPath.setFillType(Path.FillType.EVEN_ODD);
        this.mCornerShadowPath.moveTo(-this.mCornerRadius, 0.0f);
        this.mCornerShadowPath.rLineTo(-this.mShadowSize, 0.0f);
        this.mCornerShadowPath.arcTo(rectF2, 180.0f, 90.0f, false);
        this.mCornerShadowPath.arcTo(rectF, 270.0f, -90.0f, false);
        this.mCornerShadowPath.close();
        this.mCornerShadowPaint.setShader(new RadialGradient(0.0f, 0.0f, this.mCornerRadius + this.mShadowSize, new int[]{this.mShadowStartColor, this.mShadowStartColor, this.mShadowEndColor}, new float[]{0.0f, this.mCornerRadius / (this.mCornerRadius + this.mShadowSize), 1.0f}, Shader.TileMode.CLAMP));
        this.mEdgeShadowPaint.setShader(new LinearGradient(0.0f, (-this.mCornerRadius) + this.mShadowSize, 0.0f, (-this.mCornerRadius) - this.mShadowSize, new int[]{this.mShadowStartColor, this.mShadowStartColor, this.mShadowEndColor}, new float[]{0.0f, 0.5f, 1.0f}, Shader.TileMode.CLAMP));
    }

    private void buildComponents(Rect rect) {
        float f = this.mMaxShadowSize * 1.5f;
        this.mCardBounds.set(rect.left + this.mMaxShadowSize, rect.top + f, rect.right - this.mMaxShadowSize, rect.bottom - f);
        buildShadowCorners();
    }
}
