package com.android.datetimepicker.date;

import android.content.Context;

public class SimpleDayPickerView extends DayPickerView {
    public SimpleDayPickerView(Context context, DatePickerController controller) {
        super(context, controller);
    }

    @Override
    public MonthAdapter createMonthAdapter(Context context, DatePickerController controller) {
        return new SimpleMonthAdapter(context, controller);
    }
}
