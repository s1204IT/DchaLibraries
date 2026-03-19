package com.mediatek.vcalendar.valuetype;

import android.text.format.Time;
import android.util.TimeFormatException;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.component.VCalendar;
import com.mediatek.vcalendar.component.VTimezone;
import com.mediatek.vcalendar.property.Property;
import com.mediatek.vcalendar.utils.LogUtil;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.TimeZone;

public final class DateTime {
    public static String getPossibleTimezone(String str, String str2) {
        int offsetMillis;
        LogUtil.i("DateTime", "getPossibleTimezone(): dateTimeString=" + str + ";tzId=" + str2);
        if (str2 != null && str != null) {
            offsetMillis = getOffsetMillis(str, str2);
        } else {
            offsetMillis = Integer.MIN_VALUE;
        }
        if (offsetMillis == Integer.MIN_VALUE) {
            return "UTC";
        }
        Time time = new Time();
        String[] availableIDs = TimeZone.getAvailableIDs(offsetMillis);
        String str3 = availableIDs.length > 0 ? availableIDs[0] : "UTC";
        LogUtil.d("DateTime", "getPossibleTimezone(): offset =" + offsetMillis + "; TZID = " + str3);
        int length = availableIDs.length;
        for (int i = 0; i < length; i++) {
            String str4 = availableIDs[i];
            try {
                time.switchTimezone(str4);
                time.parse(str);
                time.normalize(false);
                int offset = TimeZone.getTimeZone(str4).getOffset(time.toMillis(false));
                LogUtil.d("DateTime", "getPossibleTimezone(): offset2 = " + offset);
                if (offset == offsetMillis) {
                    LogUtil.d("DateTime", "getPossibleTimezone(): after adjust TZID = " + str4);
                    return str4;
                }
            } catch (TimeFormatException e) {
                LogUtil.e("DateTime", "getPossibleTimezone(): parse time error, just stop it. time: " + time);
                return str3;
            }
        }
        return str3;
    }

    public static long getUtcDateMillis(String str) {
        int iIntValue = Integer.valueOf(str.substring(0, 4)).intValue();
        int iIntValue2 = Integer.valueOf(str.substring(4, 6)).intValue();
        int iIntValue3 = Integer.valueOf(str.substring(6, 8)).intValue();
        Calendar gregorianCalendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        gregorianCalendar.set(iIntValue, iIntValue2 - 1, iIntValue3, 0, 0, 0);
        return (gregorianCalendar.getTimeInMillis() / 1000) * 1000;
    }

    private static int getOffsetMillis(String str, String str2) {
        for (VTimezone vTimezone : VCalendar.TIMEZONE_LIST) {
            Property firstProperty = vTimezone.getFirstProperty("TZID");
            StringBuilder sb = new StringBuilder();
            sb.append("getOffsetMillis(): the current vtimezone: ");
            sb.append(firstProperty != null ? firstProperty.getValue() : "null");
            LogUtil.d("DateTime", sb.toString());
            if (firstProperty != null && str2.equalsIgnoreCase(firstProperty.getValue())) {
                LogUtil.i("DateTime", "getOffsetMillis(): has found the vtimezone: " + str2);
                return getUtcOffsetMillis(getOffsetString(vTimezone, str));
            }
        }
        return Integer.MIN_VALUE;
    }

    private static String getOffsetString(VTimezone vTimezone, String str) {
        Property firstProperty;
        if (vTimezone == null) {
            return null;
        }
        Iterator<Component> it = vTimezone.getComponents().iterator();
        String value = str;
        Component component = null;
        Component next = null;
        while (it.hasNext()) {
            next = it.next();
            Property firstProperty2 = next.getFirstProperty("DTSTART");
            if (firstProperty2 == null) {
                LogUtil.e("DateTime", "getOffsetString(): The given tz's sub-component do not contains dtstart property");
            } else if (value.compareToIgnoreCase(firstProperty2.getValue()) >= 0 && value.compareToIgnoreCase(str) <= 0) {
                value = firstProperty2.getValue();
                component = next;
            }
        }
        LogUtil.d("DateTime", "getOffsetString(): dtstart=" + str + "; tempDtStart = " + value);
        if (component == null) {
            LogUtil.e("DateTime", "getOffsetString(): The given dtStart are not contained in any Daylight or Standard Component.");
            component = next;
        }
        if (component == null || (firstProperty = component.getFirstProperty("TZOFFSETTO")) == null) {
            return null;
        }
        return firstProperty.getValue();
    }

    private static int getUtcOffsetMillis(String str) {
        if (str == null || str.length() < 5) {
            LogUtil.w("DateTime", "Invalid UTC offset [" + str + "] - must be of the form: (+/-)HHMM[SS]");
            return Integer.MIN_VALUE;
        }
        boolean z = str.charAt(0) == '-';
        if (!z && str.charAt(0) != '+') {
            throw new IllegalArgumentException("UTC offset value must be signed");
        }
        int i = (int) (((long) ((int) (((long) 0) + (((long) Integer.parseInt(str.substring(1, 3))) * 60 * 60000)))) + (((long) Integer.parseInt(str.substring(3, 5))) * 60000));
        try {
            i = (int) (((long) i) + (((long) Integer.parseInt(str.substring(5, 7))) * 1000));
        } catch (IndexOutOfBoundsException e) {
            LogUtil.i("DateTime", "getUtcOffsetMillis(): Seconds not specified: " + e.getMessage());
        }
        if (z) {
            return -i;
        }
        return i;
    }

    public static String getPossibleTimezoneV1(String str) {
        String[] availableIDs = TimeZone.getAvailableIDs(getTzOffsetMillis(str));
        return availableIDs.length > 0 ? availableIDs[0] : "UTC";
    }

    private static int getTzOffsetMillis(String str) {
        boolean z = str.charAt(0) == '-';
        String[] strArrSplit = str.replace("+", "").replace("-", "").split(":");
        try {
            i = strArrSplit.length >= 1 ? (int) (((long) 0) + (((long) Integer.parseInt(strArrSplit[0])) * 60 * 60000)) : 0;
            if (strArrSplit.length >= 2) {
                i = (int) (((long) i) + (((long) Integer.parseInt(strArrSplit[1])) * 60000));
            }
        } catch (NumberFormatException e) {
            LogUtil.w("DateTime", "NumberFormatException in getTzOffsetMillis(): " + e.getMessage());
        }
        if (z) {
            return -i;
        }
        return i;
    }
}
