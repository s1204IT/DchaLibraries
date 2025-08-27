package com.android.systemui.charging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.settingslib.Utils;
import com.android.systemui.Interpolators;
import com.android.systemui.R;

/* loaded from: classes.dex */
final class WirelessChargingView extends View {
    private double mAngleOffset;
    private long mAnimationStartTime;
    private int mCenterX;
    private int mCenterY;
    private Context mContext;
    private double mCurrAngleOffset;
    private int mCurrDotRadius;
    private int mDotsRadiusEnd;
    private int mDotsRadiusStart;
    private boolean mFinishedAnimatingDots;
    private float mInterpolatedPathGone;
    private Interpolator mInterpolator;
    private int mMainCircleCurrentRadius;
    private int mMainCircleEndRadius;
    private int mMainCircleStartRadius;
    private int mNumDots;
    private Paint mPaint;
    private float mPathGone;
    private long mScaleDotsDuration;

    public WirelessChargingView(Context context) {
        super(context);
        this.mFinishedAnimatingDots = false;
        init(context, null);
    }

    public WirelessChargingView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFinishedAnimatingDots = false;
        init(context, attributeSet);
    }

    public WirelessChargingView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mFinishedAnimatingDots = false;
        init(context, attributeSet);
    }

    public void init(Context context, AttributeSet attributeSet) {
        this.mContext = context;
        this.mDotsRadiusStart = context.getResources().getDimensionPixelSize(R.dimen.wireless_charging_dots_radius_start);
        this.mDotsRadiusEnd = context.getResources().getDimensionPixelSize(R.dimen.wireless_charging_dots_radius_end);
        this.mMainCircleStartRadius = context.getResources().getDimensionPixelSize(R.dimen.wireless_charging_circle_radius_start);
        this.mMainCircleEndRadius = context.getResources().getDimensionPixelSize(R.dimen.wireless_charging_circle_radius_end);
        this.mMainCircleCurrentRadius = this.mMainCircleStartRadius;
        this.mAngleOffset = context.getResources().getInteger(R.integer.wireless_charging_angle_offset);
        this.mScaleDotsDuration = context.getResources().getInteger(R.integer.wireless_charging_scale_dots_duration);
        this.mNumDots = context.getResources().getInteger(R.integer.wireless_charging_num_dots);
        setupPaint();
        this.mInterpolator = Interpolators.LINEAR_OUT_SLOW_IN;
    }

    private void setupPaint() {
        this.mPaint = new Paint();
        this.mPaint.setColor(Utils.getColorAttr(this.mContext, R.attr.wallpaperTextColor));
    }

    public void setPaintColor(int i) {
        this.mPaint.setColor(i);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mAnimationStartTime == 0) {
            this.mAnimationStartTime = System.currentTimeMillis();
        }
        updateDrawingParameters();
        drawCircles(canvas);
        if (!this.mFinishedAnimatingDots) {
            invalidate();
        }
    }

    private void drawCircles(Canvas canvas) {
        this.mCenterX = canvas.getWidth() / 2;
        this.mCenterY = canvas.getHeight() / 2;
        for (int i = 0; i < this.mNumDots; i++) {
            double d = ((this.mCurrAngleOffset * 3.141592653589793d) / 180.0d) + (i * (6.283185307179586d / this.mNumDots));
            canvas.drawCircle((int) (this.mCenterX + (Math.cos(d) * (this.mMainCircleCurrentRadius + this.mCurrDotRadius))), (int) (this.mCenterY + (Math.sin(d) * (this.mMainCircleCurrentRadius + this.mCurrDotRadius))), this.mCurrDotRadius, this.mPaint);
        }
        if (this.mMainCircleCurrentRadius >= this.mMainCircleEndRadius) {
            this.mFinishedAnimatingDots = true;
        }
    }

    private void updateDrawingParameters() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        long j = jCurrentTimeMillis - this.mAnimationStartTime;
        this.mPathGone = getPathGone(jCurrentTimeMillis);
        this.mInterpolatedPathGone = this.mInterpolator.getInterpolation(this.mPathGone);
        if (this.mPathGone < 1.0f) {
            this.mMainCircleCurrentRadius = this.mMainCircleStartRadius + ((int) (this.mInterpolatedPathGone * (this.mMainCircleEndRadius - this.mMainCircleStartRadius)));
        } else {
            this.mMainCircleCurrentRadius = this.mMainCircleEndRadius;
        }
        if (j < this.mScaleDotsDuration) {
            this.mCurrDotRadius = this.mDotsRadiusStart + ((int) (this.mInterpolator.getInterpolation(j / this.mScaleDotsDuration) * (this.mDotsRadiusEnd - this.mDotsRadiusStart)));
        } else {
            this.mCurrDotRadius = this.mDotsRadiusEnd;
        }
        this.mCurrAngleOffset = this.mAngleOffset * this.mPathGone;
    }

    private float getPathGone(long j) {
        return (j - this.mAnimationStartTime) / 1133.0f;
    }
}
