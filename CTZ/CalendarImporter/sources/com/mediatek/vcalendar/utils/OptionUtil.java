package com.mediatek.vcalendar.utils;

import android.os.SystemProperties;
import com.mediatek.vcalendar.component.VCalendar;
import com.mediatek.vcalendar.parameter.TzId;
import com.mediatek.vcalendar.property.Version;
import com.mediatek.vcalendar.valuetype.DateTime;

public class OptionUtil {
    private static final String OP01 = "OP01";
    private static final String RO_OPERATOR_OPTR = "ro.vendor.operator.optr";
    private static final String TAG = "OptionUtil";

    public static boolean supportOp01() {
        return OP01.equals(SystemProperties.get(RO_OPERATOR_OPTR));
    }

    public static String getLocalTimezone(TzId tzId, String str) {
        if (supportOp01()) {
            return getLocalTimezoneOp01(tzId, str);
        }
        return getLocalTimezoneCommon(tzId, str);
    }

    private static String getLocalTimezoneCommon(TzId tzId, String str) {
        String possibleTimezone;
        if (tzId == null) {
            if (VCalendar.getVCalendarVersion().contains(Version.VERSION10) && VCalendar.getV10TimeZone() != null) {
                possibleTimezone = DateTime.getPossibleTimezoneV1(VCalendar.getV10TimeZone());
            } else {
                possibleTimezone = DateTime.UTC;
            }
        } else {
            possibleTimezone = DateTime.getPossibleTimezone(str, tzId.getValue());
        }
        LogUtil.d(TAG, "getLocalTimezoneCommon(): Local time(from tzid or tz) is " + possibleTimezone);
        return possibleTimezone;
    }

    private static String getLocalTimezoneOp01(TzId tzId, String str) {
        String value;
        if (tzId == null) {
            if (VCalendar.getVCalendarVersion().contains(Version.VERSION10) && VCalendar.getV10TimeZone() != null) {
                value = DateTime.getPossibleTimezoneV1(VCalendar.getV10TimeZone());
            } else {
                value = DateTime.UTC;
            }
        } else {
            value = tzId.getValue();
        }
        LogUtil.d(TAG, "getLocalTimezoneOp01(): Local time(from tzid or tz) is " + value);
        return value;
    }
}
