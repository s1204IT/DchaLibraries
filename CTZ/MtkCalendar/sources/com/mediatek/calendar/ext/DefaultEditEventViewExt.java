package com.mediatek.calendar.ext;

import android.app.Activity;
import android.text.format.Time;
import android.widget.Button;
import com.android.datetimepicker.date.DatePickerDialog;

public class DefaultEditEventViewExt implements IEditEventViewExt {
    @Override
    public void setDatePickerSwitchUi(Activity activity, Object obj, Button button, Button button2, String str, Time time, Time time2) {
    }

    @Override
    public String getDateStringFromMillis(Activity activity, long j) {
        return "";
    }

    @Override
    public DatePickerDialog createDatePickerDialog(Activity activity, DatePickerDialog.OnDateSetListener onDateSetListener, int i, int i2, int i3) {
        return DatePickerDialog.newInstance(onDateSetListener, i, i2, i3);
    }
}
