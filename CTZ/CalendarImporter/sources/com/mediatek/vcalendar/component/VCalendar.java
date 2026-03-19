package com.mediatek.vcalendar.component;

import com.mediatek.vcalendar.property.ProdId;
import com.mediatek.vcalendar.property.Version;
import java.util.ArrayList;

public final class VCalendar {
    private static final String UTC_TZ = "BEGIN:VTIMEZONE\r\nTZID:UTC\r\nBEGIN:STANDARD\r\nDTSTART:16010101T000000\r\nTZOFFSETFROM:+0000\r\nTZOFFSETTO:+0000\r\nEND:STANDARD\r\nBEGIN:DAYLIGHT\r\nDTSTART:16010101T000000\r\nTZOFFSETFROM:+0000\r\nTZOFFSETTO:+0000\r\nEND:DAYLIGHT\r\nEND:VTIMEZONE";
    public static final String VCALENDAR_BEGIN = "BEGIN:VCALENDAR";
    public static final String VCALENDAR_END = "END:VCALENDAR";
    public static final ArrayList<VTimezone> TIMEZONE_LIST = new ArrayList<>();
    private static String sVersion = "VERSION:2.0";
    private static String sTz = null;

    private VCalendar() {
    }

    public static String getVCalendarHead() {
        return Component.BEGIN + ":VCALENDAR" + Component.NEWLINE + new ProdId().toString() + Component.NEWLINE + new Version().toString() + Component.NEWLINE + UTC_TZ + Component.NEWLINE;
    }

    public static String getVCalendarTail() {
        return Component.END + ":VCALENDAR" + Component.NEWLINE;
    }

    public static String getV10TimeZone() {
        return sTz;
    }

    public static void setV10TimeZone(String str) {
        sTz = str;
    }

    public static String getVCalendarVersion() {
        return sVersion;
    }

    public static void setVCalendarVersion(String str) {
        sVersion = str;
    }
}
