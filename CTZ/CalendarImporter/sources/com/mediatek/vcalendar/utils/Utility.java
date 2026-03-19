package com.mediatek.vcalendar.utils;

import android.text.format.Time;
import android.util.TimeFormatException;
import com.mediatek.vcalendar.parameter.TzId;

public class Utility {
    private static final String TAG = "Utility";

    public static long getTimeInMillis(TzId tzId, String str) {
        Time time = new Time(getLocalTimezone(tzId, str));
        try {
            time.parse(str);
        } catch (TimeFormatException e) {
            LogUtil.e(TAG, "getTimeInMillis(): wrong time format, time: " + str);
        }
        LogUtil.d(TAG, "getTimeInMillis(): time=" + time);
        return time.toMillis(false);
    }

    public static String getLocalTimezone(TzId tzId, String str) {
        return OptionUtil.getLocalTimezone(tzId, str);
    }

    public static boolean needQpEncode() {
        return OptionUtil.supportOp01();
    }

    public static boolean needTzIdParameter() {
        return OptionUtil.supportOp01();
    }
}
