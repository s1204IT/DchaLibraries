package com.android.datetimepicker.time;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

public class CircleView extends View {
    private float mAmPmCircleRadiusMultiplier;
    private int mCircleColor;
    private int mCircleRadius;
    private float mCircleRadiusMultiplier;
    private int mDotColor;
    private boolean mDrawValuesReady;
    private boolean mIs24HourMode;
    private boolean mIsInitialized;
    private final Paint mPaint;
    private int mXCenter;
    private int mYCenter;

    public CircleView(Context context) {
        super(context);
        this.mPaint = new Paint();
        Resources resources = context.getResources();
        this.mCircleColor = resources.getColor(R.color.white);
        this.mDotColor = resources.getColor(com.android.datetimepicker.R.color.numbers_text_color);
        this.mPaint.setAntiAlias(true);
        this.mIsInitialized = false;
    }

    public void initialize(Context context, boolean z) {
        if (this.mIsInitialized) {
            Log.e("CircleView", "CircleView may only be initialized once.");
            return;
        }
        Resources resources = context.getResources();
        this.mIs24HourMode = z;
        if (z) {
            this.mCircleRadiusMultiplier = Float.parseFloat(resources.getString(com.android.datetimepicker.R.string.circle_radius_multiplier_24HourMode));
        } else {
            this.mCircleRadiusMultiplier = Float.parseFloat(resources.getString(com.android.datetimepicker.R.string.circle_radius_multiplier));
            this.mAmPmCircleRadiusMultiplier = Float.parseFloat(resources.getString(com.android.datetimepicker.R.string.ampm_circle_radius_multiplier));
        }
        this.mIsInitialized = true;
    }

    void setTheme(Context context, boolean z) {
        Resources resources = context.getResources();
        if (z) {
            this.mCircleColor = resources.getColor(com.android.datetimepicker.R.color.dark_gray);
            this.mDotColor = resources.getColor(com.android.datetimepicker.R.color.light_gray);
        } else {
            this.mCircleColor = resources.getColor(R.color.white);
            this.mDotColor = resources.getColor(com.android.datetimepicker.R.color.numbers_text_color);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (getWidth() == 0 || !this.mIsInitialized) {
            return;
        }
        if (!this.mDrawValuesReady) {
            this.mXCenter = getWidth() / 2;
            this.mYCenter = getHeight() / 2;
            this.mCircleRadius = (int) (Math.min(this.mXCenter, this.mYCenter) * this.mCircleRadiusMultiplier);
            if (!this.mIs24HourMode) {
                this.mYCenter -= ((int) (this.mCircleRadius * this.mAmPmCircleRadiusMultiplier)) / 2;
            }
            this.mDrawValuesReady = true;
        }
        this.mPaint.setColor(this.mCircleColor);
        canvas.drawCircle(this.mXCenter, this.mYCenter, this.mCircleRadius, this.mPaint);
        this.mPaint.setColor(this.mDotColor);
        canvas.drawCircle(this.mXCenter, this.mYCenter, 2.0f, this.mPaint);
    }
}
