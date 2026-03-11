package com.android.settingslib.core.instrumentation;

import android.content.Context;
import android.metrics.LogMaker;
import android.util.Pair;
import com.android.internal.logging.MetricsLogger;

public class EventLogWriter implements LogWriter {
    private final MetricsLogger mMetricsLogger = new MetricsLogger();

    @Override
    public void visible(Context context, int i, int i2) {
        MetricsLogger.action(new LogMaker(i2).setType(1).addTaggedData(833, Integer.valueOf(i)));
    }

    @Override
    public void hidden(Context context, int i) {
        MetricsLogger.hidden(context, i);
    }

    public void action(int i, int i2, Pair<Integer, Object>... pairArr) {
        if (pairArr == null || pairArr.length == 0) {
            this.mMetricsLogger.action(i, i2);
            return;
        }
        LogMaker subtype = new LogMaker(i).setType(4).setSubtype(i2);
        for (Pair<Integer, Object> pair : pairArr) {
            subtype.addTaggedData(((Integer) pair.first).intValue(), pair.second);
        }
        this.mMetricsLogger.write(subtype);
    }

    @Override
    public void action(int i, boolean z, Pair<Integer, Object>... pairArr) {
        action(i, z ? 1 : 0, pairArr);
    }

    @Override
    public void action(Context context, int i, Pair<Integer, Object>... pairArr) {
        action(context, i, "", pairArr);
    }

    @Override
    public void actionWithSource(Context context, int i, int i2) {
        LogMaker type = new LogMaker(i2).setType(4);
        if (i != 0) {
            type.addTaggedData(833, Integer.valueOf(i));
        }
        MetricsLogger.action(type);
    }

    @Override
    @Deprecated
    public void action(Context context, int i, int i2) {
        MetricsLogger.action(context, i, i2);
    }

    @Override
    @Deprecated
    public void action(Context context, int i, boolean z) {
        MetricsLogger.action(context, i, z);
    }

    @Override
    public void action(Context context, int i, String str, Pair<Integer, Object>... pairArr) {
        if (pairArr == null || pairArr.length == 0) {
            MetricsLogger.action(context, i, str);
            return;
        }
        LogMaker packageName = new LogMaker(i).setType(4).setPackageName(str);
        for (Pair<Integer, Object> pair : pairArr) {
            packageName.addTaggedData(((Integer) pair.first).intValue(), pair.second);
        }
        MetricsLogger.action(packageName);
    }

    @Override
    public void count(Context context, String str, int i) {
        MetricsLogger.count(context, str, i);
    }
}
