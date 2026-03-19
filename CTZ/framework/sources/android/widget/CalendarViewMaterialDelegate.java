package android.widget;

import android.content.Context;
import android.graphics.Rect;
import android.icu.util.Calendar;
import android.util.AttributeSet;
import android.widget.CalendarView;
import android.widget.DayPickerView;

class CalendarViewMaterialDelegate extends CalendarView.AbstractCalendarViewDelegate {
    private final DayPickerView mDayPickerView;
    private CalendarView.OnDateChangeListener mOnDateChangeListener;
    private final DayPickerView.OnDaySelectedListener mOnDaySelectedListener;

    public CalendarViewMaterialDelegate(CalendarView calendarView, Context context, AttributeSet attributeSet, int i, int i2) {
        super(calendarView, context);
        this.mOnDaySelectedListener = new DayPickerView.OnDaySelectedListener() {
            @Override
            public void onDaySelected(DayPickerView dayPickerView, Calendar calendar) {
                if (CalendarViewMaterialDelegate.this.mOnDateChangeListener != null) {
                    CalendarViewMaterialDelegate.this.mOnDateChangeListener.onSelectedDayChange(CalendarViewMaterialDelegate.this.mDelegator, calendar.get(1), calendar.get(2), calendar.get(5));
                }
            }
        };
        this.mDayPickerView = new DayPickerView(context, attributeSet, i, i2);
        this.mDayPickerView.setOnDaySelectedListener(this.mOnDaySelectedListener);
        calendarView.addView(this.mDayPickerView);
    }

    @Override
    public void setWeekDayTextAppearance(int i) {
        this.mDayPickerView.setDayOfWeekTextAppearance(i);
    }

    @Override
    public int getWeekDayTextAppearance() {
        return this.mDayPickerView.getDayOfWeekTextAppearance();
    }

    @Override
    public void setDateTextAppearance(int i) {
        this.mDayPickerView.setDayTextAppearance(i);
    }

    @Override
    public int getDateTextAppearance() {
        return this.mDayPickerView.getDayTextAppearance();
    }

    @Override
    public void setMinDate(long j) {
        this.mDayPickerView.setMinDate(j);
    }

    @Override
    public long getMinDate() {
        return this.mDayPickerView.getMinDate();
    }

    @Override
    public void setMaxDate(long j) {
        this.mDayPickerView.setMaxDate(j);
    }

    @Override
    public long getMaxDate() {
        return this.mDayPickerView.getMaxDate();
    }

    @Override
    public void setFirstDayOfWeek(int i) {
        this.mDayPickerView.setFirstDayOfWeek(i);
    }

    @Override
    public int getFirstDayOfWeek() {
        return this.mDayPickerView.getFirstDayOfWeek();
    }

    @Override
    public void setDate(long j) {
        this.mDayPickerView.setDate(j, true);
    }

    @Override
    public void setDate(long j, boolean z, boolean z2) {
        this.mDayPickerView.setDate(j, z);
    }

    @Override
    public long getDate() {
        return this.mDayPickerView.getDate();
    }

    @Override
    public void setOnDateChangeListener(CalendarView.OnDateChangeListener onDateChangeListener) {
        this.mOnDateChangeListener = onDateChangeListener;
    }

    @Override
    public boolean getBoundsForDate(long j, Rect rect) {
        if (this.mDayPickerView.getBoundsForDate(j, rect)) {
            int[] iArr = new int[2];
            int[] iArr2 = new int[2];
            this.mDayPickerView.getLocationOnScreen(iArr);
            this.mDelegator.getLocationOnScreen(iArr2);
            int i = iArr[1] - iArr2[1];
            rect.top += i;
            rect.bottom += i;
            return true;
        }
        return false;
    }
}
