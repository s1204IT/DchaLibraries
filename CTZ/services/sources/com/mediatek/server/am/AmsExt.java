package com.mediatek.server.am;

import android.content.Context;
import android.content.Intent;
import com.android.server.am.ActivityRecord;
import com.android.server.am.ContentProviderRecord;
import com.android.server.am.ProcessRecord;
import java.io.PrintWriter;
import java.util.ArrayList;

public class AmsExt {
    public static final int COLLECT_PSS_FG_MSG = 2;

    public void onAddErrorToDropBox(String str, String str2, int i) {
    }

    public void enableMtkAmsLog() {
    }

    public void onSystemReady(Context context) {
    }

    public void onBeforeActivitySwitch(ActivityRecord activityRecord, ActivityRecord activityRecord2, boolean z, int i) {
    }

    public void onAfterActivityResumed(ActivityRecord activityRecord) {
    }

    public void onUpdateSleep(boolean z, boolean z2) {
    }

    public void setAalMode(int i) {
    }

    public void setAalEnabled(boolean z) {
    }

    public int amsAalDump(PrintWriter printWriter, String[] strArr, int i) {
        return i;
    }

    public void onStartProcess(String str, String str2) {
    }

    public void onEndOfActivityIdle(Context context, Intent intent) {
    }

    public void onWakefulnessChanged(int i) {
    }

    public void addDuraSpeedService() {
    }

    public void startDuraSpeedService(Context context) {
    }

    public String onReadyToStartComponent(String str, int i, String str2) {
        return null;
    }

    public boolean onBeforeStartProcessForStaticReceiver(String str) {
        return false;
    }

    public void addToSuppressRestartList(String str) {
    }

    public boolean notRemoveAlarm(String str) {
        return false;
    }

    public void enableAmsLog(ArrayList<ProcessRecord> arrayList) {
    }

    public void enableAmsLog(PrintWriter printWriter, String[] strArr, int i, ArrayList<ProcessRecord> arrayList) {
    }

    public void updateLMKDForBrowser(boolean z) {
    }

    public void resetLMKDForBrowserIfNeed(ProcessRecord processRecord) {
    }

    public boolean IsBuildInApp() {
        return true;
    }

    public boolean shouldKilledByAm(String str, String str2) {
        return true;
    }

    public void checkAppInLaunchingProvider(ProcessRecord processRecord, int i, int i2, ArrayList<ContentProviderRecord> arrayList) {
    }

    public void setAppInLaunchingProviderAdj(ProcessRecord processRecord, int i, int i2) {
    }
}
