package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.view.MotionEventCompat;
import android.view.View;
import com.android.camera2.R;

public class ProgressRenderer {
    private static final int SHOW_PROGRESS_X_ADDITIONAL_MS = 100;
    private final View mParentView;
    private final Paint mProgressBasePaint;
    private final Paint mProgressPaint;
    private final int mProgressRadius;
    private RectF mArcBounds = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
    private int mProgressAngleDegrees = 270;
    private boolean mVisible = false;
    private final Runnable mInvalidateParentViewRunnable = new Runnable() {
        @Override
        public void run() {
            ProgressRenderer.this.mParentView.invalidate();
        }
    };

    public ProgressRenderer(Context context, View parent) {
        this.mParentView = parent;
        this.mProgressRadius = context.getResources().getDimensionPixelSize(R.dimen.pie_progress_radius);
        int pieProgressWidth = context.getResources().getDimensionPixelSize(R.dimen.pie_progress_width);
        this.mProgressBasePaint = createProgressPaint(pieProgressWidth, 0.2f);
        this.mProgressPaint = createProgressPaint(pieProgressWidth, 1.0f);
    }

    public void setProgress(int percent) {
        int percent2 = Math.min(100, Math.max(percent, 0));
        this.mProgressAngleDegrees = (int) (3.6f * percent2);
        if (percent2 < 100) {
            this.mVisible = true;
        }
        this.mParentView.post(this.mInvalidateParentViewRunnable);
    }

    public void onDraw(Canvas canvas, int centerX, int centerY) {
        if (this.mVisible) {
            this.mArcBounds = new RectF(centerX - this.mProgressRadius, centerY - this.mProgressRadius, this.mProgressRadius + centerX, this.mProgressRadius + centerY);
            canvas.drawCircle(centerX, centerY, this.mProgressRadius, this.mProgressBasePaint);
            canvas.drawArc(this.mArcBounds, -90.0f, this.mProgressAngleDegrees, false, this.mProgressPaint);
            if (this.mProgressAngleDegrees == 360) {
                this.mVisible = false;
                this.mParentView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ProgressRenderer.this.mParentView.invalidate();
                    }
                }, 100L);
            }
        }
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    private static Paint createProgressPaint(int width, float alpha) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.argb((int) (255.0f * alpha), MotionEventCompat.ACTION_MASK, MotionEventCompat.ACTION_MASK, MotionEventCompat.ACTION_MASK));
        paint.setStrokeWidth(width);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }
}
