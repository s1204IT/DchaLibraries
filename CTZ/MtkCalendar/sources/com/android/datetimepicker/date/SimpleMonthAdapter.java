package com.android.datetimepicker.date;

import android.content.Context;

public class SimpleMonthAdapter extends MonthAdapter {
    public SimpleMonthAdapter(Context context, DatePickerController datePickerController) {
        super(context, datePickerController);
    }

    @Override
    public MonthView createMonthView(Context context) {
        SimpleMonthView simpleMonthView = new SimpleMonthView(context);
        simpleMonthView.setDatePickerController(this.mController);
        return simpleMonthView;
    }
}
