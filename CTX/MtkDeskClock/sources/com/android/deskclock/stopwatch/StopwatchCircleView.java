package com.android.deskclock.stopwatch;

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
import com.android.deskclock.data.DataModel;
import com.android.deskclock.data.Lap;
import com.android.deskclock.data.Stopwatch;
import java.util.List;

public final class StopwatchCircleView extends View {
    private final RectF mArcRect;
    private final int mCompletedColor;
    private final float mDotRadius;
    private final Paint mFill;
    private final float mMarkerStrokeSize;
    private final Paint mPaint;
    private final float mRadiusOffset;
    private final int mRemainderColor;
    private final float mScreenDensity;
    private final float mStrokeSize;

    public StopwatchCircleView(Context context) {
        this(context, null);
    }

    public StopwatchCircleView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaint = new Paint();
        this.mFill = new Paint();
        this.mArcRect = new RectF();
        Resources resources = context.getResources();
        float dimension = resources.getDimension(R.dimen.circletimer_dot_size);
        this.mDotRadius = dimension / 2.0f;
        this.mScreenDensity = resources.getDisplayMetrics().density;
        this.mStrokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        this.mMarkerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
        this.mRadiusOffset = Utils.calculateRadiusOffset(this.mStrokeSize, dimension, this.mMarkerStrokeSize);
        this.mRemainderColor = -1;
        this.mCompletedColor = ThemeUtils.resolveColor(context, R.attr.colorAccent);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mFill.setAntiAlias(true);
        this.mFill.setColor(this.mCompletedColor);
        this.mFill.setStyle(Paint.Style.FILL);
    }

    void update() {
        postInvalidateOnAnimation();
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth() / 2;
        int height = getHeight() / 2;
        float fMin = Math.min(width, height) - this.mRadiusOffset;
        this.mPaint.setColor(this.mRemainderColor);
        this.mPaint.setStrokeWidth(this.mStrokeSize);
        List<Lap> laps = getLaps();
        if (laps.isEmpty() || !DataModel.getDataModel().canAddMoreLaps()) {
            canvas.drawCircle(width, height, fMin, this.mPaint);
            return;
        }
        Stopwatch stopwatch = getStopwatch();
        int size = laps.size();
        Lap lap = laps.get(size - 1);
        Lap lap2 = laps.get(0);
        long lapTime = lap.getLapTime();
        long totalTime = stopwatch.getTotalTime() - lap2.getAccumulatedTime();
        float f = height;
        this.mArcRect.top = f - fMin;
        this.mArcRect.bottom = f + fMin;
        float f2 = width;
        this.mArcRect.left = f2 - fMin;
        this.mArcRect.right = f2 + fMin;
        float f3 = lapTime;
        float f4 = totalTime / f3;
        float f5 = 1.0f - (f4 > 1.0f ? 1.0f : f4);
        canvas.drawArc(this.mArcRect, 270.0f + ((1.0f - f5) * 360.0f), f5 * 360.0f, false, this.mPaint);
        this.mPaint.setColor(this.mCompletedColor);
        canvas.drawArc(this.mArcRect, 270.0f, f4 * 360.0f, false, this.mPaint);
        if (size > 1) {
            this.mPaint.setColor(this.mRemainderColor);
            this.mPaint.setStrokeWidth(this.mMarkerStrokeSize);
            canvas.drawArc(this.mArcRect, 270.0f + ((lap2.getLapTime() / f3) * 360.0f), this.mScreenDensity * ((float) (360.0d / (((double) fMin) * 3.141592653589793d))), false, this.mPaint);
        }
        double radians = Math.toRadians(270.0f + r15);
        double d = fMin;
        canvas.drawCircle(f2 + ((float) (Math.cos(radians) * d)), f + ((float) (d * Math.sin(radians))), this.mDotRadius, this.mFill);
        if (stopwatch.isRunning()) {
            postInvalidateOnAnimation();
        }
    }

    private Stopwatch getStopwatch() {
        return DataModel.getDataModel().getStopwatch();
    }

    private List<Lap> getLaps() {
        return DataModel.getDataModel().getLaps();
    }
}
