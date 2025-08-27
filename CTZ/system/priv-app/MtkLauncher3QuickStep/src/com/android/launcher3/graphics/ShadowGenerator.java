package com.android.launcher3.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import com.android.launcher3.LauncherAppState;

/* loaded from: classes.dex */
public class ShadowGenerator {
    private static final int AMBIENT_SHADOW_ALPHA = 30;
    public static final float BLUR_FACTOR = 0.010416667f;
    private static final float HALF_DISTANCE = 0.5f;
    private static final int KEY_SHADOW_ALPHA = 61;
    public static final float KEY_SHADOW_DISTANCE = 0.020833334f;
    private final BlurMaskFilter mDefaultBlurMaskFilter;
    private final int mIconSize;
    private final Paint mBlurPaint = new Paint(3);
    private final Paint mDrawPaint = new Paint(3);

    public ShadowGenerator(Context context) {
        this.mIconSize = LauncherAppState.getIDP(context).iconBitmapSize;
        this.mDefaultBlurMaskFilter = new BlurMaskFilter(this.mIconSize * 0.010416667f, BlurMaskFilter.Blur.NORMAL);
    }

    public synchronized void recreateIcon(Bitmap bitmap, Canvas canvas) {
        recreateIcon(bitmap, this.mDefaultBlurMaskFilter, 30, KEY_SHADOW_ALPHA, canvas);
    }

    public synchronized void recreateIcon(Bitmap bitmap, BlurMaskFilter blurMaskFilter, int i, int i2, Canvas canvas) {
        this.mBlurPaint.setMaskFilter(blurMaskFilter);
        Bitmap bitmapExtractAlpha = bitmap.extractAlpha(this.mBlurPaint, new int[2]);
        this.mDrawPaint.setAlpha(i);
        canvas.drawBitmap(bitmapExtractAlpha, r0[0], r0[1], this.mDrawPaint);
        this.mDrawPaint.setAlpha(i2);
        canvas.drawBitmap(bitmapExtractAlpha, r0[0], r0[1] + (0.020833334f * this.mIconSize), this.mDrawPaint);
        this.mDrawPaint.setAlpha(255);
        canvas.drawBitmap(bitmap, 0.0f, 0.0f, this.mDrawPaint);
    }

    public static float getScaleForBounds(RectF rectF) {
        float f;
        float fMin = Math.min(Math.min(rectF.left, rectF.right), rectF.top);
        if (fMin < 0.010416667f) {
            f = 0.48958334f / (0.5f - fMin);
        } else {
            f = 1.0f;
        }
        if (rectF.bottom < 0.03125f) {
            return Math.min(f, 0.46875f / (0.5f - rectF.bottom));
        }
        return f;
    }

    public static class Builder {
        public final int color;
        public float keyShadowDistance;
        public float radius;
        public float shadowBlur;
        public final RectF bounds = new RectF();
        public int ambientShadowAlpha = 30;
        public int keyShadowAlpha = ShadowGenerator.KEY_SHADOW_ALPHA;

        public Builder(int i) {
            this.color = i;
        }

        public Builder setupBlurForSize(int i) {
            float f = i * 1.0f;
            this.shadowBlur = f / 32.0f;
            this.keyShadowDistance = f / 16.0f;
            return this;
        }

        public Bitmap createPill(int i, int i2) {
            this.radius = i2 / 2;
            int iMax = Math.max(Math.round((i / 2) + this.shadowBlur), Math.round(this.radius + this.shadowBlur + this.keyShadowDistance));
            this.bounds.set(0.0f, 0.0f, i, i2);
            this.bounds.offsetTo(iMax - r1, iMax - r0);
            int i3 = iMax * 2;
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i3, i3, Bitmap.Config.ARGB_8888);
            drawShadow(new Canvas(bitmapCreateBitmap));
            return bitmapCreateBitmap;
        }

        public void drawShadow(Canvas canvas) {
            Paint paint = new Paint(3);
            paint.setColor(this.color);
            paint.setShadowLayer(this.shadowBlur, 0.0f, this.keyShadowDistance, ColorUtils.setAlphaComponent(ViewCompat.MEASURED_STATE_MASK, this.keyShadowAlpha));
            canvas.drawRoundRect(this.bounds, this.radius, this.radius, paint);
            paint.setShadowLayer(this.shadowBlur, 0.0f, 0.0f, ColorUtils.setAlphaComponent(ViewCompat.MEASURED_STATE_MASK, this.ambientShadowAlpha));
            canvas.drawRoundRect(this.bounds, this.radius, this.radius, paint);
            if (Color.alpha(this.color) < 255) {
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                paint.clearShadowLayer();
                paint.setColor(ViewCompat.MEASURED_STATE_MASK);
                canvas.drawRoundRect(this.bounds, this.radius, this.radius, paint);
                paint.setXfermode(null);
                paint.setColor(this.color);
                canvas.drawRoundRect(this.bounds, this.radius, this.radius, paint);
            }
        }
    }
}
