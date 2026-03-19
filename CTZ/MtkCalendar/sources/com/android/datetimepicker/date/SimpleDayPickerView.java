package com.android.datetimepicker.date;

import android.content.Context;

public class SimpleDayPickerView extends DayPickerView {
    public SimpleDayPickerView(Context context, DatePickerController datePickerController) {
        super(context, datePickerController);
    }

    @Override
    public MonthAdapter createMonthAdapter(Context context, DatePickerController datePickerController) {
        return new SimpleMonthAdapter(context, datePickerController);
    }
}
