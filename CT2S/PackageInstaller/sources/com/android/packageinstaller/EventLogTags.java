package com.android.packageinstaller;

import android.util.EventLog;

public class EventLogTags {
    public static void writeInstallPackageAttempt(int resultAndFlags, int totalTime, int timeTillPkgInfoObtained, int timeTillInstallClicked, String packageDigest) {
        EventLog.writeEvent(90300, Integer.valueOf(resultAndFlags), Integer.valueOf(totalTime), Integer.valueOf(timeTillPkgInfoObtained), Integer.valueOf(timeTillInstallClicked), packageDigest);
    }
}
