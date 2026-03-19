package com.mediatek.camera.common.debug.profiler;

import com.mediatek.camera.common.debug.LogUtil;
import java.util.HashMap;

public class PerformanceTracker {
    private static HashMap<String, String> sTrackerMap = new HashMap<>();

    public static IPerformanceProfile create(LogUtil.Tag tag, String str) {
        return new PerformanceProfile(ProfilerWriters.getLogWriter(), tag, str);
    }
}
