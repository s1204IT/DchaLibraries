package com.android.phone;

import android.util.Log;
import android.view.Window;

public class Profiler {
    private static final String LOG_TAG = "PhoneGlobals";
    private static final boolean PROFILE = false;
    static long sTimeCallScreenCreated;
    static long sTimeCallScreenOnCreate;
    static long sTimeCallScreenRequested;
    static long sTimeIncomingCallPanelCreated;
    static long sTimeIncomingCallPanelOnCreate;
    static long sTimeIncomingCallPanelRequested;

    private Profiler() {
    }

    static void profileViewCreate(Window window, String str) {
    }

    static void callScreenRequested() {
    }

    static void callScreenOnCreate() {
    }

    static void callScreenCreated() {
    }

    private static void dumpCallScreenStat() {
    }

    static void incomingCallPanelRequested() {
    }

    static void incomingCallPanelOnCreate() {
    }

    static void incomingCallPanelCreated() {
    }

    private static void dumpIncomingCallPanelStat() {
    }

    private static void log(String str) {
        Log.d("PhoneGlobals", "[Profiler] " + str);
    }
}
