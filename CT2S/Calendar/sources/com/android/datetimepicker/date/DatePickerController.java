package com.android.datetimepicker.date;

import com.android.datetimepicker.date.DatePickerDialog;
import com.android.datetimepicker.date.MonthAdapter;
import java.util.Calendar;

public interface DatePickerController {
    int getFirstDayOfWeek();

    Calendar getMaxDate();

    int getMaxYear();

    Calendar getMinDate();

    int getMinYear();

    MonthAdapter.CalendarDay getSelectedDay();

    void onDayOfMonthSelected(int i, int i2, int i3);

    void onYearSelected(int i);

    void registerOnDateChangedListener(DatePickerDialog.OnDateChangedListener onDateChangedListener);

    void tryVibrate();
}
