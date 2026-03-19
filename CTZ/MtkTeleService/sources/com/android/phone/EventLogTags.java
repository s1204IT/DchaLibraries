package com.android.phone;

import android.util.EventLog;

public class EventLogTags {
    public static final int PHONE_UI_BUTTON_CLICK = 70303;
    public static final int PHONE_UI_ENTER = 70301;
    public static final int PHONE_UI_EXIT = 70302;
    public static final int PHONE_UI_MULTIPLE_QUERY = 70305;
    public static final int PHONE_UI_RINGER_QUERY_ELAPSED = 70304;

    private EventLogTags() {
    }

    public static void writePhoneUiEnter() {
        EventLog.writeEvent(PHONE_UI_ENTER, new Object[0]);
    }

    public static void writePhoneUiExit() {
        EventLog.writeEvent(PHONE_UI_EXIT, new Object[0]);
    }

    public static void writePhoneUiButtonClick(String str) {
        EventLog.writeEvent(PHONE_UI_BUTTON_CLICK, str);
    }

    public static void writePhoneUiRingerQueryElapsed() {
        EventLog.writeEvent(PHONE_UI_RINGER_QUERY_ELAPSED, new Object[0]);
    }

    public static void writePhoneUiMultipleQuery() {
        EventLog.writeEvent(PHONE_UI_MULTIPLE_QUERY, new Object[0]);
    }
}
