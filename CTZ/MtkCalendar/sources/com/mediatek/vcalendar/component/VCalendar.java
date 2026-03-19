package com.mediatek.vcalendar.component;

import java.util.ArrayList;

public final class VCalendar {
    public static final ArrayList<VTimezone> TIMEZONE_LIST = new ArrayList<>();
    private static String sVersion = "VERSION:2.0";
    private static String sTz = null;

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
