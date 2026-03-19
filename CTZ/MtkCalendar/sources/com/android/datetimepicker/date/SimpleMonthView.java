package com.android.datetimepicker.date;

import android.content.Context;
import android.graphics.Canvas;

public class SimpleMonthView extends MonthView {
    public SimpleMonthView(Context context) {
        super(context);
    }

    @Override
    public void drawMonthDay(Canvas canvas, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
        if (this.mSelectedDay == i3) {
            canvas.drawCircle(i4, i5 - (MINI_DAY_NUMBER_TEXT_SIZE / 3), DAY_SELECTED_CIRCLE_SIZE, this.mSelectedCirclePaint);
        }
        if (isOutOfRange(i, i2, i3)) {
            this.mMonthNumPaint.setColor(this.mDisabledDayTextColor);
        } else if (this.mHasToday && this.mToday == i3) {
            this.mMonthNumPaint.setColor(this.mTodayNumberColor);
        } else {
            this.mMonthNumPaint.setColor(this.mDayTextColor);
        }
        canvas.drawText(String.format("%d", Integer.valueOf(i3)), i4, i5, this.mMonthNumPaint);
    }
}
