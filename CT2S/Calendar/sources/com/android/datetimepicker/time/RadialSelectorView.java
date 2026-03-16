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

    public void initialize(Context context, boolean is24HourMode, boolean hasInnerCircle, boolean disappearsOut, int selectionDegrees, boolean isInnerCircle) {
        if (this.mIsInitialized) {
            Log.e("RadialSelectorView", "This RadialSelectorView may only be initialized once.");
            return;
        }
        Resources res = context.getResources();
        int blue = res.getColor(R.color.blue);
        this.mPaint.setColor(blue);
        this.mPaint.setAntiAlias(true);
        this.mSelectionAlpha = 51;
        this.mIs24HourMode = is24HourMode;
        if (is24HourMode) {
            this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(R.string.circle_radius_multiplier_24HourMode));
        } else {
            this.mCircleRadiusMultiplier = Float.parseFloat(res.getString(R.string.circle_radius_multiplier));
            this.mAmPmCircleRadiusMultiplier = Float.parseFloat(res.getString(R.string.ampm_circle_radius_multiplier));
        }
        this.mHasInnerCircle = hasInnerCircle;
        if (hasInnerCircle) {
            this.mInnerNumbersRadiusMultiplier = Float.parseFloat(res.getString(R.string.numbers_radius_multiplier_inner));
            this.mOuterNumbersRadiusMultiplier = Float.parseFloat(res.getString(R.string.numbers_radius_multiplier_outer));
        } else {
            this.mNumbersRadiusMultiplier = Float.parseFloat(res.getString(R.string.numbers_radius_multiplier_normal));
        }
        this.mSelectionRadiusMultiplier = Float.parseFloat(res.getString(R.string.selection_radius_multiplier));
        this.mAnimationRadiusMultiplier = 1.0f;
        this.mTransitionMidRadiusMultiplier = ((disappearsOut ? -1 : 1) * 0.05f) + 1.0f;
        this.mTransitionEndRadiusMultiplier = (0.3f * (disappearsOut ? 1 : -1)) + 1.0f;
        this.mInvalidateUpdateListener = new InvalidateUpdateListener();
        setSelection(selectionDegrees, isInnerCircle, false);
        this.mIsInitialized = true;
    }

    void setTheme(Context context, boolean themeDark) {
        int color;
        Resources res = context.getResources();
        if (themeDark) {
            color = res.getColor(R.color.red);
            this.mSelectionAlpha = 102;
        } else {
            color = res.getColor(R.color.blue);
            this.mSelectionAlpha = 51;
        }
        this.mPaint.setColor(color);
    }

    public void setSelection(int selectionDegrees, boolean isInnerCircle, boolean forceDrawDot) {
        this.mSelectionDegrees = selectionDegrees;
        this.mSelectionRadians = (((double) selectionDegrees) * 3.141592653589793d) / 180.0d;
        this.mForceDrawDot = forceDrawDot;
        if (this.mHasInnerCircle) {
            if (isInnerCircle) {
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

    public void setAnimationRadiusMultiplier(float animationRadiusMultiplier) {
        this.mAnimationRadiusMultiplier = animationRadiusMultiplier;
    }

    public int getDegreesFromCoords(float pointX, float pointY, boolean forceLegal, Boolean[] isInnerCircle) {
        if (!this.mDrawValuesReady) {
            return -1;
        }
        double hypotenuse = Math.sqrt(((pointY - this.mYCenter) * (pointY - this.mYCenter)) + ((pointX - this.mXCenter) * (pointX - this.mXCenter)));
        if (this.mHasInnerCircle) {
            if (forceLegal) {
                int innerNumberRadius = (int) (this.mCircleRadius * this.mInnerNumbersRadiusMultiplier);
                int distanceToInnerNumber = (int) Math.abs(hypotenuse - ((double) innerNumberRadius));
                int outerNumberRadius = (int) (this.mCircleRadius * this.mOuterNumbersRadiusMultiplier);
                int distanceToOuterNumber = (int) Math.abs(hypotenuse - ((double) outerNumberRadius));
                isInnerCircle[0] = Boolean.valueOf(distanceToInnerNumber <= distanceToOuterNumber);
            } else {
                int minAllowedHypotenuseForInnerNumber = ((int) (this.mCircleRadius * this.mInnerNumbersRadiusMultiplier)) - this.mSelectionRadius;
                int maxAllowedHypotenuseForOuterNumber = ((int) (this.mCircleRadius * this.mOuterNumbersRadiusMultiplier)) + this.mSelectionRadius;
                int halfwayHypotenusePoint = (int) (this.mCircleRadius * ((this.mOuterNumbersRadiusMultiplier + this.mInnerNumbersRadiusMultiplier) / 2.0f));
                if (hypotenuse >= minAllowedHypotenuseForInnerNumber && hypotenuse <= halfwayHypotenusePoint) {
                    isInnerCircle[0] = true;
                } else if (hypotenuse <= maxAllowedHypotenuseForOuterNumber && hypotenuse >= halfwayHypotenusePoint) {
                    isInnerCircle[0] = false;
                } else {
                    return -1;
                }
            }
        } else if (!forceLegal) {
            int distanceToNumber = (int) Math.abs(hypotenuse - ((double) this.mLineLength));
            int maxAllowedDistance = (int) (this.mCircleRadius * (1.0f - this.mNumbersRadiusMultiplier));
            if (distanceToNumber > maxAllowedDistance) {
                return -1;
            }
        }
        float opposite = Math.abs(pointY - this.mYCenter);
        double radians = Math.asin(((double) opposite) / hypotenuse);
        int degrees = (int) ((180.0d * radians) / 3.141592653589793d);
        boolean rightSide = pointX > ((float) this.mXCenter);
        boolean topSide = pointY < ((float) this.mYCenter);
        if (rightSide && topSide) {
            return 90 - degrees;
        }
        if (rightSide && !topSide) {
            return degrees + 90;
        }
        if (!rightSide && !topSide) {
            return 270 - degrees;
        }
        if (!rightSide && topSide) {
            return degrees + 270;
        }
        return degrees;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int viewWidth = getWidth();
        if (viewWidth != 0 && this.mIsInitialized) {
            if (!this.mDrawValuesReady) {
                this.mXCenter = getWidth() / 2;
                this.mYCenter = getHeight() / 2;
                this.mCircleRadius = (int) (Math.min(this.mXCenter, this.mYCenter) * this.mCircleRadiusMultiplier);
                if (!this.mIs24HourMode) {
                    int amPmCircleRadius = (int) (this.mCircleRadius * this.mAmPmCircleRadiusMultiplier);
                    this.mYCenter -= amPmCircleRadius / 2;
                }
                this.mSelectionRadius = (int) (this.mCircleRadius * this.mSelectionRadiusMultiplier);
                this.mDrawValuesReady = true;
            }
            this.mLineLength = (int) (this.mCircleRadius * this.mNumbersRadiusMultiplier * this.mAnimationRadiusMultiplier);
            int pointX = this.mXCenter + ((int) (((double) this.mLineLength) * Math.sin(this.mSelectionRadians)));
            int pointY = this.mYCenter - ((int) (((double) this.mLineLength) * Math.cos(this.mSelectionRadians)));
            this.mPaint.setAlpha(this.mSelectionAlpha);
            canvas.drawCircle(pointX, pointY, this.mSelectionRadius, this.mPaint);
            if ((this.mSelectionDegrees % 30 != 0) | this.mForceDrawDot) {
                this.mPaint.setAlpha(255);
                canvas.drawCircle(pointX, pointY, (this.mSelectionRadius * 2) / 7, this.mPaint);
            } else {
                int lineLength = this.mLineLength - this.mSelectionRadius;
                pointX = this.mXCenter + ((int) (((double) lineLength) * Math.sin(this.mSelectionRadians)));
                pointY = this.mYCenter - ((int) (((double) lineLength) * Math.cos(this.mSelectionRadians)));
            }
            this.mPaint.setAlpha(255);
            this.mPaint.setStrokeWidth(1.0f);
            canvas.drawLine(this.mXCenter, this.mYCenter, pointX, pointY, this.mPaint);
        }
    }

    public ObjectAnimator getDisappearAnimator() {
        if (!this.mIsInitialized || !this.mDrawValuesReady) {
            Log.e("RadialSelectorView", "RadialSelectorView was not ready for animation.");
            return null;
        }
        Keyframe kf0 = Keyframe.ofFloat(0.0f, 1.0f);
        Keyframe kf1 = Keyframe.ofFloat(0.2f, this.mTransitionMidRadiusMultiplier);
        Keyframe kf2 = Keyframe.ofFloat(1.0f, this.mTransitionEndRadiusMultiplier);
        PropertyValuesHolder radiusDisappear = PropertyValuesHolder.ofKeyframe("animationRadiusMultiplier", kf0, kf1, kf2);
        Keyframe kf02 = Keyframe.ofFloat(0.0f, 1.0f);
        Keyframe kf12 = Keyframe.ofFloat(1.0f, 0.0f);
        PropertyValuesHolder fadeOut = PropertyValuesHolder.ofKeyframe("alpha", kf02, kf12);
        ObjectAnimator disappearAnimator = ObjectAnimator.ofPropertyValuesHolder(this, radiusDisappear, fadeOut).setDuration(500);
        disappearAnimator.addUpdateListener(this.mInvalidateUpdateListener);
        return disappearAnimator;
    }

    public ObjectAnimator getReappearAnimator() {
        if (!this.mIsInitialized || !this.mDrawValuesReady) {
            Log.e("RadialSelectorView", "RadialSelectorView was not ready for animation.");
            return null;
        }
        float totalDurationMultiplier = 1.0f + 0.25f;
        int totalDuration = (int) (500 * totalDurationMultiplier);
        float delayPoint = (500 * 0.25f) / totalDuration;
        float midwayPoint = 1.0f - ((1.0f - delayPoint) * 0.2f);
        Keyframe kf0 = Keyframe.ofFloat(0.0f, this.mTransitionEndRadiusMultiplier);
        Keyframe kf1 = Keyframe.ofFloat(delayPoint, this.mTransitionEndRadiusMultiplier);
        Keyframe kf2 = Keyframe.ofFloat(midwayPoint, this.mTransitionMidRadiusMultiplier);
        Keyframe kf3 = Keyframe.ofFloat(1.0f, 1.0f);
        PropertyValuesHolder radiusReappear = PropertyValuesHolder.ofKeyframe("animationRadiusMultiplier", kf0, kf1, kf2, kf3);
        Keyframe kf02 = Keyframe.ofFloat(0.0f, 0.0f);
        Keyframe kf12 = Keyframe.ofFloat(delayPoint, 0.0f);
        Keyframe kf22 = Keyframe.ofFloat(1.0f, 1.0f);
        PropertyValuesHolder fadeIn = PropertyValuesHolder.ofKeyframe("alpha", kf02, kf12, kf22);
        ObjectAnimator reappearAnimator = ObjectAnimator.ofPropertyValuesHolder(this, radiusReappear, fadeIn).setDuration(totalDuration);
        reappearAnimator.addUpdateListener(this.mInvalidateUpdateListener);
        return reappearAnimator;
    }

    private class InvalidateUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private InvalidateUpdateListener() {
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            RadialSelectorView.this.invalidate();
        }
    }
}
