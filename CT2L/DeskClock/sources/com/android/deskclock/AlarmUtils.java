package com.android.deskclock;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.widget.Toast;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import java.util.Calendar;
import java.util.Locale;

public class AlarmUtils {
    public static String getFormattedTime(Context context, Calendar time) {
        String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return (String) DateFormat.format(pattern, time);
    }

    public static String getAlarmText(Context context, AlarmInstance instance) {
        String alarmTimeStr = getFormattedTime(context, instance.getAlarmTime());
        return !instance.mLabel.isEmpty() ? alarmTimeStr + " - " + instance.mLabel : alarmTimeStr;
    }

    public static void showTimeEditDialog(Fragment fragment, Alarm alarm) {
        FragmentManager manager = fragment.getFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        Fragment prev = manager.findFragmentByTag("time_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();
        TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTargetFragment(fragment, 0);
        timePickerFragment.setOnTimeSetListener((TimePickerDialog.OnTimeSetListener) fragment);
        timePickerFragment.setAlarm(alarm);
        timePickerFragment.show(manager, "time_dialog");
    }

    private static String formatToast(Context context, long timeInMillis) {
        long delta = timeInMillis - System.currentTimeMillis();
        long hours = delta / 3600000;
        long minutes = (delta / 60000) % 60;
        long days = hours / 24;
        long hours2 = hours % 24;
        String daySeq = days == 0 ? "" : days == 1 ? context.getString(R.string.day) : context.getString(R.string.days, Long.toString(days));
        String minSeq = minutes == 0 ? "" : minutes == 1 ? context.getString(R.string.minute) : context.getString(R.string.minutes, Long.toString(minutes));
        String hourSeq = hours2 == 0 ? "" : hours2 == 1 ? context.getString(R.string.hour) : context.getString(R.string.hours, Long.toString(hours2));
        boolean dispDays = days > 0;
        boolean dispHour = hours2 > 0;
        boolean dispMinute = minutes > 0;
        int index = (dispDays ? 1 : 0) | (dispHour ? 2 : 0) | (dispMinute ? 4 : 0);
        String[] formats = context.getResources().getStringArray(R.array.alarm_set);
        return String.format(formats[index], daySeq, hourSeq, minSeq);
    }

    public static void popAlarmSetToast(Context context, long timeInMillis) {
        String toastText = formatToast(context, timeInMillis);
        Toast toast = Toast.makeText(context, toastText, 1);
        ToastMaster.setToast(toast);
        toast.show();
    }
}
