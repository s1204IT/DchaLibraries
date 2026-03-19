package com.android.packageinstaller.permission.utils;

import android.metrics.LogMaker;
import com.android.internal.logging.MetricsLogger;

public class EventLogger {
    private static final MetricsLogger sMetricsLogger = new MetricsLogger();

    public static void logPermission(int i, String str, String str2) {
        LogMaker logMaker = new LogMaker(i);
        logMaker.setPackageName(str2);
        logMaker.addTaggedData(1241, str);
        sMetricsLogger.write(logMaker);
    }
}
