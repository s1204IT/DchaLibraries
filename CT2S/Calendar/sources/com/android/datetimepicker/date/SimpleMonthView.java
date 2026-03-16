package com.android.datetimepicker.date;

import android.content.Context;
import android.graphics.Canvas;

public class SimpleMonthView extends MonthView {
    public SimpleMonthView(Context context) {
        super(context);
    }

    @Override
    public void drawMonthDay(Canvas canvas, int year, int month, int day, int x, int y, int startX, int stopX, int startY, int stopY) {
        if (this.mSelectedDay == day) {
            canvas.drawCircle(x, y - (MINI_DAY_NUMBER_TEXT_SIZE / 3), DAY_SELECTED_CIRCLE_SIZE, this.mSelectedCirclePaint);
        }
        if (isOutOfRange(year, month, day)) {
            this.mMonthNumPaint.setColor(this.mDisabledDayTextColor);
        } else if (this.mHasToday && this.mToday == day) {
            this.mMonthNumPaint.setColor(this.mTodayNumberColor);
        } else {
            this.mMonthNumPaint.setColor(this.mDayTextColor);
        }
        canvas.drawText(String.format("%d", Integer.valueOf(day)), x, y, this.mMonthNumPaint);
    }
}
