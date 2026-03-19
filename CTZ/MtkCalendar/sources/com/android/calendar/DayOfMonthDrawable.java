package com.android.calendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

public class DayOfMonthDrawable extends Drawable {
    private static float mTextSize = 14.0f;
    private final Paint mPaint;
    private String mDayOfMonth = "1";
    private final Rect mTextBounds = new Rect();

    public DayOfMonthDrawable(Context context) {
        mTextSize = context.getResources().getDimension(R.dimen.today_icon_text_size);
        this.mPaint = new Paint();
        this.mPaint.setAlpha(255);
        this.mPaint.setColor(-8947849);
        this.mPaint.setTypeface(Typeface.DEFAULT_BOLD);
        this.mPaint.setTextSize(mTextSize);
        this.mPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    public void draw(Canvas canvas) {
        this.mPaint.getTextBounds(this.mDayOfMonth, 0, this.mDayOfMonth.length(), this.mTextBounds);
        int i = this.mTextBounds.bottom - this.mTextBounds.top;
        Rect bounds = getBounds();
        canvas.drawText(this.mDayOfMonth, bounds.right / 2, ((bounds.bottom + i) + 1.0f) / 2.0f, this.mPaint);
    }

    @Override
    public void setAlpha(int i) {
        this.mPaint.setAlpha(i);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        return 0;
    }

    public void setDayOfMonth(int i) {
        this.mDayOfMonth = Integer.toString(i);
        invalidateSelf();
    }
}
