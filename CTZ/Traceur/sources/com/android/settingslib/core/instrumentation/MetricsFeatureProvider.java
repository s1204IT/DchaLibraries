package com.android.settingslib.core.instrumentation;

import android.content.Context;
import java.util.Iterator;
import java.util.List;

public class MetricsFeatureProvider {
    private List<LogWriter> mLoggerWriters;

    public void visible(Context context, int i, int i2) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().visible(context, i, i2);
        }
    }

    public void hidden(Context context, int i) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().hidden(context, i);
        }
    }
}
