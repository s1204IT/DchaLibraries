package com.mediatek.server.anr;

import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityRecord;
import com.android.server.am.ProcessRecord;

public class AnrManagerImpl extends AnrManager {
    private AnrManagerService mService = AnrManagerService.getInstance();

    public void AddAnrManagerService() {
        ServiceManager.addService("anrmanager", this.mService, true);
    }

    public void startAnrManagerService(int i) {
        this.mService.startAnrManagerService(i);
    }

    public boolean isAnrDeferrable() {
        return this.mService.isAnrDeferrable();
    }

    public boolean delayMessage(Handler handler, Message message, int i, int i2) {
        if (isAnrDeferrable()) {
            Message messageObtainMessage = handler.obtainMessage(i);
            messageObtainMessage.obj = message.obj;
            handler.sendMessageDelayed(messageObtainMessage, i2);
            return true;
        }
        return false;
    }

    public void writeEvent(int i) {
        this.mService.writeEvent(i);
    }

    public void sendBroadcastMonitorMessage(long j, long j2) {
        this.mService.sendBroadcastMonitorMessage(j, j2);
    }

    public void removeBroadcastMonitorMessage() {
        this.mService.removeBroadcastMonitorMessage();
    }

    public void sendServiceMonitorMessage() {
        this.mService.sendServiceMonitorMessage();
    }

    public void removeServiceMonitorMessage() {
        this.mService.removeServiceMonitorMessage();
    }

    public boolean startAnrDump(ActivityManagerService activityManagerService, ProcessRecord processRecord, ActivityRecord activityRecord, ActivityRecord activityRecord2, boolean z, String str, boolean z2) {
        try {
            return this.mService.startAnrDump(activityManagerService, processRecord, activityRecord, activityRecord2, z, str, z2);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
