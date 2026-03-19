package com.android.datetimepicker.time;

import android.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import java.text.DateFormatSymbols;

public class AmPmCirclesView extends View {
    private int mAmOrPm;
    private int mAmOrPmPressed;
    private int mAmPmCircleRadius;
    private float mAmPmCircleRadiusMultiplier;
    private int mAmPmTextColor;
    private int mAmPmYCenter;
    private String mAmText;
    private int mAmXCenter;
    private float mCircleRadiusMultiplier;
    private boolean mDrawValuesReady;
    private boolean mIsInitialized;
    private final Paint mPaint;
    private String mPmText;
    private int mPmXCenter;
    private int mSelectedAlpha;
    private int mSelectedColor;
    private int mUnselectedColor;

    public AmPmCirclesView(Context context) {
        super(context);
        this.mPaint = new Paint();
        this.mIsInitialized = false;
    }

    public void initialize(Context context, int i) {
        if (this.mIsInitialized) {
            Log.e("AmPmCirclesView", "AmPmCirclesView may only be initialized once.");
            return;
        }
        Resources resources = context.getResources();
        this.mUnselectedColor = resources.getColor(R.color.white);
        this.mSelectedColor = resources.getColor(com.android.datetimepicker.R.color.blue);
        this.mAmPmTextColor = resources.getColor(com.android.datetimepicker.R.color.ampm_text_color);
        this.mSelectedAlpha = 51;
        this.mPaint.setTypeface(Typeface.create(resources.getString(com.android.datetimepicker.R.string.sans_serif), 0));
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTextAlign(Paint.Align.CENTER);
        this.mCircleRadiusMultiplier = Float.parseFloat(resources.getString(com.android.datetimepicker.R.string.circle_radius_multiplier));
        this.mAmPmCircleRadiusMultiplier = Float.parseFloat(resources.getString(com.android.datetimepicker.R.string.ampm_circle_radius_multiplier));
        String[] amPmStrings = new DateFormatSymbols().getAmPmStrings();
        this.mAmText = amPmStrings[0];
        this.mPmText = amPmStrings[1];
        setAmOrPm(i);
        this.mAmOrPmPressed = -1;
        this.mIsInitialized = true;
    }

    void setTheme(Context context, boolean z) {
        Resources resources = context.getResources();
        if (z) {
            this.mUnselectedColor = resources.getColor(com.android.datetimepicker.R.color.dark_gray);
            this.mSelectedColor = resources.getColor(com.android.datetimepicker.R.color.red);
            this.mAmPmTextColor = resources.getColor(R.color.white);
            this.mSelectedAlpha = 102;
            return;
        }
        this.mUnselectedColor = resources.getColor(R.color.white);
        this.mSelectedColor = resources.getColor(com.android.datetimepicker.R.color.blue);
        this.mAmPmTextColor = resources.getColor(com.android.datetimepicker.R.color.ampm_text_color);
        this.mSelectedAlpha = 51;
    }

    public void setAmOrPm(int i) {
        this.mAmOrPm = i;
    }

    public void setAmOrPmPressed(int i) {
        this.mAmOrPmPressed = i;
    }

    public int getIsTouchingAmOrPm(float f, float f2) {
        if (!this.mDrawValuesReady) {
            return -1;
        }
        float f3 = (int) ((f2 - this.mAmPmYCenter) * (f2 - this.mAmPmYCenter));
        if (((int) Math.sqrt(((f - this.mAmXCenter) * (f - this.mAmXCenter)) + f3)) <= this.mAmPmCircleRadius) {
            return 0;
        }
        return ((int) Math.sqrt((double) (((f - ((float) this.mPmXCenter)) * (f - ((float) this.mPmXCenter))) + f3))) <= this.mAmPmCircleRadius ? 1 : -1;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int i;
        if (getWidth() == 0 || !this.mIsInitialized) {
            return;
        }
        if (!this.mDrawValuesReady) {
            int width = getWidth() / 2;
            int height = getHeight() / 2;
            int iMin = (int) (Math.min(width, height) * this.mCircleRadiusMultiplier);
            this.mAmPmCircleRadius = (int) (iMin * this.mAmPmCircleRadiusMultiplier);
            this.mPaint.setTextSize((this.mAmPmCircleRadius * 3) / 4);
            this.mAmPmYCenter = (height - (this.mAmPmCircleRadius / 2)) + iMin;
            this.mAmXCenter = (width - iMin) + this.mAmPmCircleRadius;
            this.mPmXCenter = (width + iMin) - this.mAmPmCircleRadius;
            this.mDrawValuesReady = true;
        }
        int i2 = this.mUnselectedColor;
        int i3 = this.mUnselectedColor;
        int i4 = 255;
        if (this.mAmOrPm == 0) {
            i2 = this.mSelectedColor;
            i4 = this.mSelectedAlpha;
            i = 255;
        } else if (this.mAmOrPm == 1) {
            i3 = this.mSelectedColor;
            i = this.mSelectedAlpha;
        } else {
            i = 255;
        }
        if (this.mAmOrPmPressed == 0) {
            i2 = this.mSelectedColor;
            i4 = this.mSelectedAlpha;
        } else if (this.mAmOrPmPressed == 1) {
            i3 = this.mSelectedColor;
            i = this.mSelectedAlpha;
        }
        this.mPaint.setColor(i2);
        this.mPaint.setAlpha(i4);
        canvas.drawCircle(this.mAmXCenter, this.mAmPmYCenter, this.mAmPmCircleRadius, this.mPaint);
        this.mPaint.setColor(i3);
        this.mPaint.setAlpha(i);
        canvas.drawCircle(this.mPmXCenter, this.mAmPmYCenter, this.mAmPmCircleRadius, this.mPaint);
        this.mPaint.setColor(this.mAmPmTextColor);
        float fDescent = this.mAmPmYCenter - (((int) (this.mPaint.descent() + this.mPaint.ascent())) / 2);
        canvas.drawText(this.mAmText, this.mAmXCenter, fDescent, this.mPaint);
        canvas.drawText(this.mPmText, this.mPmXCenter, fDescent, this.mPaint);
    }
}
