package com.mediatek.camera.common.relation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatusMonitorFactory {
    private final Map<String, StatusMonitor> mStatusMonitors = new ConcurrentHashMap();

    public StatusMonitor getStatusMonitor(String str) {
        StatusMonitor statusMonitor = this.mStatusMonitors.get(str);
        if (statusMonitor == null) {
            StatusMonitor statusMonitor2 = new StatusMonitor();
            this.mStatusMonitors.put(str, statusMonitor2);
            return statusMonitor2;
        }
        return statusMonitor;
    }
}
