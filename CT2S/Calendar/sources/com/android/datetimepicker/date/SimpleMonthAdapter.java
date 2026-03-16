package com.android.datetimepicker.date;

import android.content.Context;

public class SimpleMonthAdapter extends MonthAdapter {
    public SimpleMonthAdapter(Context context, DatePickerController controller) {
        super(context, controller);
    }

    @Override
    public MonthView createMonthView(Context context) {
        MonthView monthView = new SimpleMonthView(context);
        monthView.setDatePickerController(this.mController);
        return monthView;
    }
}
