package com.android.managedprovisioning.analytics;

import android.content.Context;
import com.android.internal.logging.MetricsLogger;

public class MetricsLoggerWrapper {
    public void logAction(Context context, int i, String str) {
        logd("MetricsLoggerWrapper, category:" + i + ", value: " + str);
        if (i != 0) {
            MetricsLogger.action(context, i, str);
        }
    }

    public void logAction(Context context, int i, int i2) {
        logd("MetricsLoggerWrapper, category:" + i + ", value: " + i2);
        if (i != 0) {
            MetricsLogger.action(context, i, i2);
        }
    }

    public void logAction(Context context, int i) {
        logd("MetricsLoggerWrapper, category:" + i);
        if (i != 0) {
            MetricsLogger.action(context, i);
        }
    }

    private void logd(String str) {
    }
}
