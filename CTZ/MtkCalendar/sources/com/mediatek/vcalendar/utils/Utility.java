package com.mediatek.vcalendar.utils;

import android.text.format.Time;
import android.util.TimeFormatException;
import com.mediatek.vcalendar.parameter.TzId;

public class Utility {
    public static long getTimeInMillis(TzId tzId, String str) {
        Time time = new Time(getLocalTimezone(tzId, str));
        try {
            time.parse(str);
        } catch (TimeFormatException e) {
            LogUtil.e("Utility", "getTimeInMillis(): wrong time format, time: " + str);
        }
        LogUtil.d("Utility", "getTimeInMillis(): time=" + time);
        return time.toMillis(false);
    }

    public static String getLocalTimezone(TzId tzId, String str) {
        return OptionUtil.getLocalTimezone(tzId, str);
    }

    public static boolean needQpEncode() {
        return OptionUtil.supportOp01();
    }
}
