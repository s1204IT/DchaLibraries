package com.android.deskclock;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.Snackbar;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Toast;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.widget.toast.SnackbarManager;
import com.android.deskclock.widget.toast.ToastManager;
import java.util.Calendar;
import java.util.Locale;

public class AlarmUtils {
    public static String getFormattedTime(Context context, Calendar calendar) {
        return (String) DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), DateFormat.is24HourFormat(context) ? "EHm" : "Ehma"), calendar);
    }

    public static String getFormattedTime(Context context, long j) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        return getFormattedTime(context, calendar);
    }

    public static String getAlarmText(Context context, AlarmInstance alarmInstance, boolean z) {
        String formattedTime = getFormattedTime(context, alarmInstance.getAlarmTime());
        if (alarmInstance.mLabel.isEmpty() || !z) {
            return formattedTime;
        }
        return formattedTime + " - " + alarmInstance.mLabel;
    }

    @VisibleForTesting
    static String formatElapsedTimeUntilAlarm(Context context, long j) {
        String[] stringArray = context.getResources().getStringArray(R.array.alarm_set);
        if (j < 60000) {
            return stringArray[0];
        }
        long j2 = j % 60000;
        long j3 = 0;
        if (j2 != 0) {
            j3 = 60000 - j2;
        }
        int i = (int) (j + j3);
        int i2 = i / 3600000;
        int i3 = (i / 60000) % 60;
        int i4 = i2 / 24;
        int i5 = i2 % 24;
        String numberFormattedQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.days, i4);
        String numberFormattedQuantityString2 = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes, i3);
        String numberFormattedQuantityString3 = Utils.getNumberFormattedQuantityString(context, R.plurals.hours, i5);
        char c = i4 > 0 ? (char) 1 : (char) 0;
        boolean z = i5 > 0;
        boolean z2 = i3 > 0;
        return String.format(stringArray[(z2 ? 4 : 0) | (z ? (char) 2 : (char) 0) | c], numberFormattedQuantityString, numberFormattedQuantityString3, numberFormattedQuantityString2);
    }

    public static void popAlarmSetToast(Context context, long j) {
        Toast toastMakeText = Toast.makeText(context, formatElapsedTimeUntilAlarm(context, j - System.currentTimeMillis()), 1);
        ToastManager.setToast(toastMakeText);
        toastMakeText.show();
    }

    public static void popAlarmSetSnackbar(View view, long j) {
        String elapsedTimeUntilAlarm = formatElapsedTimeUntilAlarm(view.getContext(), j - System.currentTimeMillis());
        SnackbarManager.show(Snackbar.make(view, elapsedTimeUntilAlarm, -1));
        view.announceForAccessibility(elapsedTimeUntilAlarm);
    }
}
