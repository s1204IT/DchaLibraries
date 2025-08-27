package com.android.launcher3.badge;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import com.android.launcher3.graphics.ShadowGenerator;

/* loaded from: classes.dex */
public class BadgeRenderer {
    private static final float DOT_SCALE = 0.6f;
    private static final float OFFSET_PERCENTAGE = 0.02f;
    private static final float SIZE_PERCENTAGE = 0.38f;
    private static final String TAG = "BadgeRenderer";
    private final Bitmap mBackgroundWithShadow;
    private final float mBitmapOffset;
    private final Paint mCirclePaint = new Paint(3);
    private final float mCircleRadius;
    private final float mDotCenterOffset;
    private final int mOffset;

    public BadgeRenderer(int i) {
        float f = i;
        this.mDotCenterOffset = SIZE_PERCENTAGE * f;
        this.mOffset = (int) (OFFSET_PERCENTAGE * f);
        int i2 = (int) (DOT_SCALE * this.mDotCenterOffset);
        ShadowGenerator.Builder builder = new ShadowGenerator.Builder(0);
        this.mBackgroundWithShadow = builder.setupBlurForSize(i2).createPill(i2, i2);
        this.mCircleRadius = builder.radius;
        this.mBitmapOffset = (-this.mBackgroundWithShadow.getHeight()) * 0.5f;
    }

    public void draw(Canvas canvas, int i, Rect rect, float f, Point point) {
        if (rect == null || point == null) {
            Log.e(TAG, "Invalid null argument(s) passed in call to draw.");
            return;
        }
        canvas.save();
        canvas.translate((rect.right - (this.mDotCenterOffset / 2.0f)) + Math.min(this.mOffset, point.x), (rect.top + (this.mDotCenterOffset / 2.0f)) - Math.min(this.mOffset, point.y));
        canvas.scale(f, f);
        this.mCirclePaint.setColor(ViewCompat.MEASURED_STATE_MASK);
        canvas.drawBitmap(this.mBackgroundWithShadow, this.mBitmapOffset, this.mBitmapOffset, this.mCirclePaint);
        this.mCirclePaint.setColor(i);
        canvas.drawCircle(0.0f, 0.0f, this.mCircleRadius, this.mCirclePaint);
        canvas.restore();
    }
}
