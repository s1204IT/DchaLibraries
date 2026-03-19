package com.mediatek.server.anr;

import android.os.Handler;
import android.os.Message;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityRecord;
import com.android.server.am.ProcessRecord;

public class AnrManager {
    public static final int EVENT_BOOT_COMPLETED = 9001;

    public void AddAnrManagerService() {
    }

    public void startAnrManagerService(int i) {
    }

    public boolean isAnrDeferrable() {
        return false;
    }

    public boolean delayMessage(Handler handler, Message message, int i, int i2) {
        return false;
    }

    public void writeEvent(int i) {
    }

    public void sendBroadcastMonitorMessage(long j, long j2) {
    }

    public void removeBroadcastMonitorMessage() {
    }

    public void sendServiceMonitorMessage() {
    }

    public void removeServiceMonitorMessage() {
    }

    public boolean startAnrDump(ActivityManagerService activityManagerService, ProcessRecord processRecord, ActivityRecord activityRecord, ActivityRecord activityRecord2, boolean z, String str, boolean z2) {
        return false;
    }
}
