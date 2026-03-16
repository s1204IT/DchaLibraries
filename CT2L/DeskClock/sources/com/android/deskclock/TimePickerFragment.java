package com.android.deskclock;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import com.android.deskclock.provider.Alarm;
import java.util.Calendar;

public class TimePickerFragment extends DialogFragment {
    private Alarm mAlarm;
    private TimePickerDialog.OnTimeSetListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int hour;
        int minute;
        if (this.mAlarm == null) {
            Calendar c = Calendar.getInstance();
            hour = c.get(11);
            minute = c.get(12);
        } else {
            hour = this.mAlarm.hour;
            minute = this.mAlarm.minutes;
        }
        return new TimePickerDialog(getActivity(), R.style.TimePickerTheme, this.mListener, hour, minute, DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (getTargetFragment() instanceof TimePickerDialog.OnTimeSetListener) {
            setOnTimeSetListener((TimePickerDialog.OnTimeSetListener) getTargetFragment());
        }
    }

    public void setOnTimeSetListener(TimePickerDialog.OnTimeSetListener listener) {
        this.mListener = listener;
    }

    public void setAlarm(Alarm alarm) {
        this.mAlarm = alarm;
    }
}
