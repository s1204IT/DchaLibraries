package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.android.deskclock.R;
import com.android.deskclock.ThemeUtils;
import com.android.deskclock.Utils;
import com.android.deskclock.data.Timer;

public final class TimerCircleView extends View {
    private final RectF mArcRect;
    private final int mCompletedColor;
    private final float mDotRadius;
    private final Paint mFill;
    private final Paint mPaint;
    private final float mRadiusOffset;
    private final int mRemainderColor;
    private final float mStrokeSize;
    private Timer mTimer;

    public TimerCircleView(Context context) {
        this(context, null);
    }

    public TimerCircleView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
        this.mFill = new Paint();
        this.mArcRect = new RectF();
        Resources resources = context.getResources();
        float dimension = resources.getDimension(R.dimen.circletimer_dot_size);
        this.mDotRadius = dimension / 2.0f;
        this.mStrokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        this.mRadiusOffset = Utils.calculateRadiusOffset(this.mStrokeSize, dimension, 0.0f);
        this.mRemainderColor = -1;
        this.mCompletedColor = ThemeUtils.resolveColor(context, R.attr.colorAccent);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mFill.setAntiAlias(true);
        this.mFill.setColor(this.mCompletedColor);
        this.mFill.setStyle(Paint.Style.FILL);
    }

    void update(Timer timer) {
        if (this.mTimer != timer) {
            this.mTimer = timer;
            postInvalidateOnAnimation();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.mTimer == null) {
            return;
        }
        int width = getWidth() / 2;
        int height = getHeight() / 2;
        float fMin = Math.min(width, height) - this.mRadiusOffset;
        this.mPaint.setColor(this.mRemainderColor);
        this.mPaint.setStrokeWidth(this.mStrokeSize);
        float f = 1.0f;
        if (this.mTimer.isReset()) {
            canvas.drawCircle(width, height, fMin, this.mPaint);
            f = 0.0f;
        } else if (this.mTimer.isExpired()) {
            this.mPaint.setColor(this.mCompletedColor);
            canvas.drawCircle(width, height, fMin, this.mPaint);
        } else {
            float f2 = height;
            this.mArcRect.top = f2 - fMin;
            this.mArcRect.bottom = f2 + fMin;
            float f3 = width;
            this.mArcRect.left = f3 - fMin;
            this.mArcRect.right = f3 + fMin;
            float fMin2 = Math.min(1.0f, this.mTimer.getElapsedTime() / this.mTimer.getTotalLength());
            canvas.drawArc(this.mArcRect, 270.0f, (1.0f - fMin2) * 360.0f, false, this.mPaint);
            this.mPaint.setColor(this.mCompletedColor);
            canvas.drawArc(this.mArcRect, 270.0f, (-fMin2) * 360.0f, false, this.mPaint);
            f = fMin2;
        }
        double radians = Math.toRadians(270.0f - (f * 360.0f));
        double d = fMin;
        canvas.drawCircle(width + ((float) (Math.cos(radians) * d)), height + ((float) (d * Math.sin(radians))), this.mDotRadius, this.mFill);
        if (this.mTimer.isRunning()) {
            postInvalidateOnAnimation();
        }
    }
}
