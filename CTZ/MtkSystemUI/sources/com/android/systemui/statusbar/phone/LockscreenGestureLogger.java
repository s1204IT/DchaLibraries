package com.android.systemui.statusbar.phone;

import android.metrics.LogMaker;
import android.util.ArrayMap;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.EventLogConstants;
import com.android.systemui.EventLogTags;

public class LockscreenGestureLogger {
    private LogMaker mLogMaker = new LogMaker(0).setType(4);
    private final MetricsLogger mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
    private ArrayMap<Integer, Integer> mLegacyMap = new ArrayMap<>(EventLogConstants.METRICS_GESTURE_TYPE_MAP.length);

    public LockscreenGestureLogger() {
        for (int i = 0; i < EventLogConstants.METRICS_GESTURE_TYPE_MAP.length; i++) {
            this.mLegacyMap.put(Integer.valueOf(EventLogConstants.METRICS_GESTURE_TYPE_MAP[i]), Integer.valueOf(i));
        }
    }

    public void write(int i, int i2, int i3) {
        this.mMetricsLogger.write(this.mLogMaker.setCategory(i).setType(4).addTaggedData(826, Integer.valueOf(i2)).addTaggedData(827, Integer.valueOf(i3)));
        EventLogTags.writeSysuiLockscreenGesture(safeLookup(i), i2, i3);
    }

    public void writeAtFractionalPosition(int i, int i2, int i3, int i4) {
        this.mMetricsLogger.write(this.mLogMaker.setCategory(i).setType(4).addTaggedData(1326, Integer.valueOf(i2)).addTaggedData(1327, Integer.valueOf(i3)).addTaggedData(1329, Integer.valueOf(i4)));
    }

    private int safeLookup(int i) {
        Integer num = this.mLegacyMap.get(Integer.valueOf(i));
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }
}
