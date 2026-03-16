package com.android.deskclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircleTimerView extends View {
    private int mAccentColor;
    private long mAccumulatedTime;
    private boolean mAnimate;
    private final RectF mArcRect;
    private long mCurrentIntervalTime;
    private final Paint mFill;
    private long mIntervalStartTime;
    private long mIntervalTime;
    private long mMarkerTime;
    private final Paint mPaint;
    private boolean mPaused;
    private float mRadiusOffset;
    private float mScreenDensity;
    private boolean mTimerMode;
    private int mWhiteColor;
    private static float mStrokeSize = 4.0f;
    private static float mDotRadius = 6.0f;
    private static float mMarkerStrokeSize = 2.0f;

    public CircleTimerView(Context context) {
        this(context, null);
    }

    public CircleTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIntervalTime = 0L;
        this.mIntervalStartTime = -1L;
        this.mMarkerTime = -1L;
        this.mCurrentIntervalTime = 0L;
        this.mAccumulatedTime = 0L;
        this.mPaused = false;
        this.mAnimate = false;
        this.mPaint = new Paint();
        this.mFill = new Paint();
        this.mArcRect = new RectF();
        this.mTimerMode = false;
        init(context);
    }

    public void setIntervalTime(long t) {
        this.mIntervalTime = t;
        postInvalidate();
    }

    public void setMarkerTime(long t) {
        this.mMarkerTime = t;
        postInvalidate();
    }

    public void reset() {
        this.mIntervalStartTime = -1L;
        this.mMarkerTime = -1L;
        postInvalidate();
    }

    public void startIntervalAnimation() {
        this.mIntervalStartTime = Utils.getTimeNow();
        this.mAnimate = true;
        invalidate();
        this.mPaused = false;
    }

    public void stopIntervalAnimation() {
        this.mAnimate = false;
        this.mIntervalStartTime = -1L;
        this.mAccumulatedTime = 0L;
    }

    public boolean isAnimating() {
        return this.mIntervalStartTime != -1;
    }

    public void pauseIntervalAnimation() {
        this.mAnimate = false;
        this.mAccumulatedTime += Utils.getTimeNow() - this.mIntervalStartTime;
        this.mPaused = true;
    }

    public void abortIntervalAnimation() {
        this.mAnimate = false;
    }

    public void setPassedTime(long time, boolean drawRed) {
        this.mAccumulatedTime = time;
        this.mCurrentIntervalTime = time;
        if (drawRed) {
            this.mIntervalStartTime = Utils.getTimeNow();
        }
        postInvalidate();
    }

    private void init(Context c) {
        Resources resources = c.getResources();
        mStrokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        float dotDiameter = resources.getDimension(R.dimen.circletimer_dot_size);
        mMarkerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
        this.mRadiusOffset = Utils.calculateRadiusOffset(mStrokeSize, dotDiameter, mMarkerStrokeSize);
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.STROKE);
        this.mWhiteColor = resources.getColor(R.color.clock_white);
        this.mAccentColor = resources.getColor(R.color.hot_pink);
        this.mScreenDensity = resources.getDisplayMetrics().density;
        this.mFill.setAntiAlias(true);
        this.mFill.setStyle(Paint.Style.FILL);
        this.mFill.setColor(this.mAccentColor);
        mDotRadius = dotDiameter / 2.0f;
    }

    public void setTimerMode(boolean mode) {
        this.mTimerMode = mode;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int xCenter = (getWidth() / 2) + 1;
        int yCenter = getHeight() / 2;
        this.mPaint.setStrokeWidth(mStrokeSize);
        float radius = Math.min(xCenter, yCenter) - this.mRadiusOffset;
        if (this.mIntervalStartTime == -1) {
            this.mPaint.setColor(this.mWhiteColor);
            canvas.drawCircle(xCenter, yCenter, radius, this.mPaint);
            if (this.mTimerMode) {
                drawRedDot(canvas, 0.0f, xCenter, yCenter, radius);
            }
        } else {
            if (this.mAnimate) {
                this.mCurrentIntervalTime = (Utils.getTimeNow() - this.mIntervalStartTime) + this.mAccumulatedTime;
            }
            this.mArcRect.top = yCenter - radius;
            this.mArcRect.bottom = yCenter + radius;
            this.mArcRect.left = xCenter - radius;
            this.mArcRect.right = xCenter + radius;
            float redPercent = this.mCurrentIntervalTime / this.mIntervalTime;
            if (redPercent > 1.0f && this.mTimerMode) {
                redPercent = 1.0f;
            }
            float whitePercent = 1.0f - (redPercent > 1.0f ? 1.0f : redPercent);
            this.mPaint.setColor(this.mAccentColor);
            if (this.mTimerMode) {
                canvas.drawArc(this.mArcRect, 270.0f, (-redPercent) * 360.0f, false, this.mPaint);
            } else {
                canvas.drawArc(this.mArcRect, 270.0f, redPercent * 360.0f, false, this.mPaint);
            }
            this.mPaint.setStrokeWidth(mStrokeSize);
            this.mPaint.setColor(this.mWhiteColor);
            if (this.mTimerMode) {
                canvas.drawArc(this.mArcRect, 270.0f, whitePercent * 360.0f, false, this.mPaint);
            } else {
                canvas.drawArc(this.mArcRect, 270.0f + ((1.0f - whitePercent) * 360.0f), whitePercent * 360.0f, false, this.mPaint);
            }
            if (this.mMarkerTime != -1 && radius > 0.0f && this.mIntervalTime != 0) {
                this.mPaint.setStrokeWidth(mMarkerStrokeSize);
                float angle = ((this.mMarkerTime % this.mIntervalTime) / this.mIntervalTime) * 360.0f;
                canvas.drawArc(this.mArcRect, 270.0f + angle, this.mScreenDensity * ((float) (360.0d / (((double) radius) * 3.141592653589793d))), false, this.mPaint);
            }
            drawRedDot(canvas, redPercent, xCenter, yCenter, radius);
        }
        if (this.mAnimate) {
            invalidate();
        }
    }

    protected void drawRedDot(Canvas canvas, float degrees, int xCenter, int yCenter, float radius) {
        float dotPercent;
        this.mPaint.setColor(this.mAccentColor);
        if (this.mTimerMode) {
            dotPercent = 270.0f - (degrees * 360.0f);
        } else {
            dotPercent = 270.0f + (degrees * 360.0f);
        }
        double dotRadians = Math.toRadians(dotPercent);
        canvas.drawCircle(xCenter + ((float) (((double) radius) * Math.cos(dotRadians))), yCenter + ((float) (((double) radius) * Math.sin(dotRadians))), mDotRadius, this.mFill);
    }

    public void writeToSharedPref(SharedPreferences prefs, String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key + "_ctv_paused", this.mPaused);
        editor.putLong(key + "_ctv_interval", this.mIntervalTime);
        editor.putLong(key + "_ctv_interval_start", this.mIntervalStartTime);
        editor.putLong(key + "_ctv_current_interval", this.mCurrentIntervalTime);
        editor.putLong(key + "_ctv_accum_time", this.mAccumulatedTime);
        editor.putLong(key + "_ctv_marker_time", this.mMarkerTime);
        editor.putBoolean(key + "_ctv_timer_mode", this.mTimerMode);
        editor.apply();
    }

    public void readFromSharedPref(SharedPreferences prefs, String key) {
        boolean z = false;
        this.mPaused = prefs.getBoolean(key + "_ctv_paused", false);
        this.mIntervalTime = prefs.getLong(key + "_ctv_interval", 0L);
        this.mIntervalStartTime = prefs.getLong(key + "_ctv_interval_start", -1L);
        this.mCurrentIntervalTime = prefs.getLong(key + "_ctv_current_interval", 0L);
        this.mAccumulatedTime = prefs.getLong(key + "_ctv_accum_time", 0L);
        this.mMarkerTime = prefs.getLong(key + "_ctv_marker_time", -1L);
        this.mTimerMode = prefs.getBoolean(key + "_ctv_timer_mode", false);
        if (this.mIntervalStartTime != -1 && !this.mPaused) {
            z = true;
        }
        this.mAnimate = z;
    }

    public void clearSharedPref(SharedPreferences prefs, String key) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("sw_start_time");
        editor.remove("sw_accum_time");
        editor.remove("sw_state");
        editor.remove(key + "_ctv_paused");
        editor.remove(key + "_ctv_interval");
        editor.remove(key + "_ctv_interval_start");
        editor.remove(key + "_ctv_current_interval");
        editor.remove(key + "_ctv_accum_time");
        editor.remove(key + "_ctv_marker_time");
        editor.remove(key + "_ctv_timer_mode");
        editor.apply();
    }
}
