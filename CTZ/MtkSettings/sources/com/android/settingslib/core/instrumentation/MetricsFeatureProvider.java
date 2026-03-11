package com.android.settingslib.core.instrumentation;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Pair;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MetricsFeatureProvider {
    private List<LogWriter> mLoggerWriters = new ArrayList();

    public MetricsFeatureProvider() {
        installLogWriters();
    }

    protected void installLogWriters() {
        this.mLoggerWriters.add(new EventLogWriter());
    }

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

    public void actionWithSource(Context context, int i, int i2) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().actionWithSource(context, i, i2);
        }
    }

    public void action(VisibilityLoggerMixin visibilityLoggerMixin, int i, boolean z) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().action(i, z, sinceVisibleTaggedData(visibilityLoggerMixin.elapsedTimeSinceVisible()));
        }
    }

    public void action(Context context, int i, Pair<Integer, Object>... pairArr) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().action(context, i, pairArr);
        }
    }

    @Deprecated
    public void action(Context context, int i, int i2) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().action(context, i, i2);
        }
    }

    @Deprecated
    public void action(Context context, int i, boolean z) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().action(context, i, z);
        }
    }

    public void action(Context context, int i, String str, Pair<Integer, Object>... pairArr) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().action(context, i, str, pairArr);
        }
    }

    public void count(Context context, String str, int i) {
        Iterator<LogWriter> it = this.mLoggerWriters.iterator();
        while (it.hasNext()) {
            it.next().count(context, str, i);
        }
    }

    public int getMetricsCategory(Object obj) {
        if (obj == null || !(obj instanceof Instrumentable)) {
            return 0;
        }
        return ((Instrumentable) obj).getMetricsCategory();
    }

    public void logDashboardStartIntent(Context context, Intent intent, int i) {
        if (intent == null) {
            return;
        }
        ComponentName component = intent.getComponent();
        if (component == null) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                return;
            }
            action(context, 830, action, Pair.create(833, Integer.valueOf(i)));
            return;
        }
        if (TextUtils.equals(component.getPackageName(), context.getPackageName())) {
            return;
        }
        action(context, 830, component.flattenToString(), Pair.create(833, Integer.valueOf(i)));
    }

    private Pair<Integer, Object> sinceVisibleTaggedData(long j) {
        return Pair.create(794, Long.valueOf(j));
    }
}
