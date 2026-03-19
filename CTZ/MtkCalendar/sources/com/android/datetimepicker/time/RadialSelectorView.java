package com.android.datetimepicker.time;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;
import com.android.datetimepicker.R;

public class RadialSelectorView extends View {
    private float mAmPmCircleRadiusMultiplier;
    private float mAnimationRadiusMultiplier;
    private int mCircleRadius;
    private float mCircleRadiusMultiplier;
    private boolean mDrawValuesReady;
    private boolean mForceDrawDot;
    private boolean mHasInnerCircle;
    private float mInnerNumbersRadiusMultiplier;
    private InvalidateUpdateListener mInvalidateUpdateListener;
    private boolean mIs24HourMode;
    private boolean mIsInitialized;
    private int mLineLength;
    private float mNumbersRadiusMultiplier;
    private float mOuterNumbersRadiusMultiplier;
    private final Paint mPaint;
    private int mSelectionAlpha;
    private int mSelectionDegrees;
    private double mSelectionRadians;
    private int mSelectionRadius;
    private float mSelectionRadiusMultiplier;
    private float mTransitionEndRadiusMultiplier;
    private float mTransitionMidRadiusMultiplier;
    private int mXCenter;
    private int mYCenter;

    public RadialSelectorView(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mIsInitialized = false;
    }

    public void initialize(Context context, boolean z, boolean z2, boolean z3, int i, boolean z4) {
        if (this.mIsInitialized) {
            Log.e("RadialSelectorView", "This RadialSelectorView may only be initialized once.");
            return;
        }
        Resources resources = context.getResources();
        this.mPaint.setColor(resources.getColor(R.color.blue));
        this.mPaint.setAntiAlias(true);
        this.mSelectionAlpha = 51;
        this.mIs24HourMode = z;
        if (z) {
            this.mCircleRadiusMultiplier = Float.parseFloat(resources.getString(R.string.circle_radius_multiplier_24HourMode));
        } else {
            this.mCircleRadiusMultiplier = Float.parseFloat(resources.getString(R.string.circle_radius_multiplier));
            this.mAmPmCircleRadiusMultiplier = Float.parseFloat(resources.getString(R.string.ampm_circle_radius_multiplier));
        }
        this.mHasInnerCircle = z2;
        if (z2) {
            this.mInnerNumbersRadiusMultiplier = Float.parseFloat(resources.getString(R.string.numbers_radius_multiplier_inner));
            this.mOuterNumbersRadiusMultiplier = Float.parseFloat(resources.getString(R.string.numbers_radius_multiplier_outer));
        } else {
            this.mNumbersRadiusMultiplier = Float.parseFloat(resources.getString(R.string.numbers_radius_multiplier_normal));
        }
        this.mSelectionRadiusMultiplier = Float.parseFloat(resources.getString(R.string.selection_radius_multiplier));
        this.mAnimationRadiusMultiplier = 1.0f;
        this.mTransitionMidRadiusMultiplier = (0.05f * (z3 ? -1 : 1)) + 1.0f;
        this.mTransitionEndRadiusMultiplier = 1.0f + (0.3f * (z3 ? 1 : -1));
        this.mInvalidateUpdateListener = new InvalidateUpdateListener();
        setSelection(i, z4, false);
        this.mIsInitialized = true;
    }

    void setTheme(Context context, boolean z) {
        int color;
        Resources resources = context.getResources();
        if (z) {
            color = resources.getColor(R.color.red);
            this.mSelectionAlpha = 102;
        } else {
            color = resources.getColor(R.color.blue);
            this.mSelectionAlpha = 51;
        }
        this.mPaint.setColor(color);
    }

    public void setSelection(int i, boolean z, boolean z2) {
        this.mSelectionDegrees = i;
        this.mSelectionRadians = (((double) i) * 3.141592653589793d) / 180.0d;
        this.mForceDrawDot = z2;
        if (this.mHasInnerCircle) {
            if (z) {
                this.mNumbersRadiusMultiplier = this.mInnerNumbersRadiusMultiplier;
            } else {
                this.mNumbersRadiusMultiplier = this.mOuterNumbersRadiusMultiplier;
            }
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setAnimationRadiusMultiplier(float f) {
        this.mAnimationRadiusMultiplier = f;
    }

    public int getDegreesFromCoords(float f, float f2, boolean z, Boolean[] boolArr) {
        if (!this.mDrawValuesReady) {
            return -1;
        }
        double dSqrt = Math.sqrt(((f2 - this.mYCenter) * (f2 - this.mYCenter)) + ((f - this.mXCenter) * (f - this.mXCenter)));
        if (this.mHasInnerCircle) {
            if (z) {
                boolArr[0] = Boolean.valueOf(((int) Math.abs(dSqrt - ((double) ((int) (((float) this.mCircleRadius) * this.mInnerNumbersRadiusMultiplier))))) <= ((int) Math.abs(dSqrt - ((double) ((int) (((float) this.mCircleRadius) * this.mOuterNumbersRadiusMultiplier))))));
            } else {
                int i = ((int) (this.mCircleRadius * this.mInnerNumbersRadiusMultiplier)) - this.mSelectionRadius;
                int i2 = ((int) (this.mCircleRadius * this.mOuterNumbersRadiusMultiplier)) + this.mSelectionRadius;
                int i3 = (int) (this.mCircleRadius * ((this.mOuterNumbersRadiusMultiplier + this.mInnerNumbersRadiusMultiplier) / 2.0f));
                if (dSqrt >= i && dSqrt <= i3) {
                    boolArr[0] = true;
                } else {
                    if (dSqrt > i2 || dSqrt < i3) {
                        return -1;
                    }
                    boolArr[0] = false;
                }
            }
        } else if (!z && ((int) Math.abs(dSqrt - ((double) this.mLineLength))) > ((int) (this.mCircleRadius * (1.0f - this.mNumbersRadiusMultiplier)))) {
            return -1;
        }
        int iAsin = (int) ((Math.asin(((double) Math.abs(f2 - this.mYCenter)) / dSqrt) * 180.0d) / 3.141592653589793d);
        boolean z2 = f > ((float) this.mXCenter);
        boolean z3 = f2 < ((float) this.mYCenter);
        if (z2 && z3) {
            return 90 - iAsin;
        }
        if (z2 && !z3) {
            return iAsin + 90;
        }
        if (!z2 && !z3) {
            return 270 - iAsin;
        }
        if (!z2 && z3) {
            return iAsin + 270;
        }
        return iAsin;
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
            this.mSelectionRadius = (int) (this.mCircleRadius * this.mSelectionRadiusMultiplier);
            this.mDrawValuesReady = true;
        }
        this.mLineLength = (int) (this.mCircleRadius * this.mNumbersRadiusMultiplier * this.mAnimationRadiusMultiplier);
        int iSin = this.mXCenter + ((int) (((double) this.mLineLength) * Math.sin(this.mSelectionRadians)));
        int iCos = this.mYCenter - ((int) (((double) this.mLineLength) * Math.cos(this.mSelectionRadians)));
        this.mPaint.setAlpha(this.mSelectionAlpha);
        float f = iSin;
        float f2 = iCos;
        canvas.drawCircle(f, f2, this.mSelectionRadius, this.mPaint);
        if ((this.mSelectionDegrees % 30 != 0) | this.mForceDrawDot) {
            this.mPaint.setAlpha(255);
            canvas.drawCircle(f, f2, (this.mSelectionRadius * 2) / 7, this.mPaint);
        } else {
            double d = this.mLineLength - this.mSelectionRadius;
            iSin = ((int) (Math.sin(this.mSelectionRadians) * d)) + this.mXCenter;
            iCos = this.mYCenter - ((int) (d * Math.cos(this.mSelectionRadians)));
        }
        this.mPaint.setAlpha(255);
        this.mPaint.setStrokeWidth(1.0f);
        canvas.drawLine(this.mXCenter, this.mYCenter, iSin, iCos, this.mPaint);
    }

    public ObjectAnimator getDisappearAnimator() {
        if (!this.mIsInitialized || !this.mDrawValuesReady) {
            Log.e("RadialSelectorView", "RadialSelectorView was not ready for animation.");
            return null;
        }
        ObjectAnimator duration = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofKeyframe("animationRadiusMultiplier", Keyframe.ofFloat(0.0f, 1.0f), Keyframe.ofFloat(0.2f, this.mTransitionMidRadiusMultiplier), Keyframe.ofFloat(1.0f, this.mTransitionEndRadiusMultiplier)), PropertyValuesHolder.ofKeyframe("alpha", Keyframe.ofFloat(0.0f, 1.0f), Keyframe.ofFloat(1.0f, 0.0f))).setDuration(500);
        duration.addUpdateListener(this.mInvalidateUpdateListener);
        return duration;
    }

    public ObjectAnimator getReappearAnimator() {
        if (!this.mIsInitialized || !this.mDrawValuesReady) {
            Log.e("RadialSelectorView", "RadialSelectorView was not ready for animation.");
            return null;
        }
        float f = 500;
        int i = (int) (1.25f * f);
        float f2 = (0.25f * f) / i;
        ObjectAnimator duration = ObjectAnimator.ofPropertyValuesHolder(this, PropertyValuesHolder.ofKeyframe("animationRadiusMultiplier", Keyframe.ofFloat(0.0f, this.mTransitionEndRadiusMultiplier), Keyframe.ofFloat(f2, this.mTransitionEndRadiusMultiplier), Keyframe.ofFloat(1.0f - (0.2f * (1.0f - f2)), this.mTransitionMidRadiusMultiplier), Keyframe.ofFloat(1.0f, 1.0f)), PropertyValuesHolder.ofKeyframe("alpha", Keyframe.ofFloat(0.0f, 0.0f), Keyframe.ofFloat(f2, 0.0f), Keyframe.ofFloat(1.0f, 1.0f))).setDuration(i);
        duration.addUpdateListener(this.mInvalidateUpdateListener);
        return duration;
    }

    private class InvalidateUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private InvalidateUpdateListener() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            RadialSelectorView.this.invalidate();
        }
    }
}
