package com.android.server.power.batterysaver;

import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.server.slice.SliceClientPermissions;
import java.util.Map;

public class CpuFrequencies {
    private static final String TAG = "CpuFrequencies";
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final ArrayMap<Integer, Long> mCoreAndFrequencies = new ArrayMap<>();

    public CpuFrequencies parseString(String str) {
        int i;
        synchronized (this.mLock) {
            this.mCoreAndFrequencies.clear();
            try {
            } catch (IllegalArgumentException e) {
                Slog.wtf(TAG, "Invalid configuration: '" + str + "'");
            }
            for (String str2 : str.split(SliceClientPermissions.SliceAuthority.DELIMITER)) {
                String strTrim = str2.trim();
                if (strTrim.length() != 0) {
                    String[] strArrSplit = strTrim.split(":", 2);
                    if (strArrSplit.length != 2) {
                        throw new IllegalArgumentException("Wrong format");
                    }
                    this.mCoreAndFrequencies.put(Integer.valueOf(Integer.parseInt(strArrSplit[0])), Long.valueOf(Long.parseLong(strArrSplit[1])));
                }
            }
        }
        return this;
    }

    public ArrayMap<String, String> toSysFileMap() {
        ArrayMap<String, String> arrayMap = new ArrayMap<>();
        addToSysFileMap(arrayMap);
        return arrayMap;
    }

    public void addToSysFileMap(Map<String, String> map) {
        synchronized (this.mLock) {
            int size = this.mCoreAndFrequencies.size();
            for (int i = 0; i < size; i++) {
                map.put("/sys/devices/system/cpu/cpu" + Integer.toString(this.mCoreAndFrequencies.keyAt(i).intValue()) + "/cpufreq/scaling_max_freq", Long.toString(this.mCoreAndFrequencies.valueAt(i).longValue()));
            }
        }
    }
}
