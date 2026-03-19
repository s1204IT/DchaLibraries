package com.mediatek.calendar.ext;

import android.app.Activity;
import android.text.format.Time;
import android.widget.Button;
import com.android.datetimepicker.date.DatePickerDialog;

public interface IEditEventViewExt {
    DatePickerDialog createDatePickerDialog(Activity activity, DatePickerDialog.OnDateSetListener onDateSetListener, int i, int i2, int i3);

    String getDateStringFromMillis(Activity activity, long j);

    void setDatePickerSwitchUi(Activity activity, Object obj, Button button, Button button2, String str, Time time, Time time2);
}
