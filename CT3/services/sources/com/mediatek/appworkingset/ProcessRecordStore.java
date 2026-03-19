package com.mediatek.appworkingset;

import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.app.procstats.ProcessStats;
import com.android.server.SystemService;
import com.mediatek.am.IAWSProcessRecord;

class ProcessRecordStore implements IAWSProcessRecord {
    static final boolean DEBUG = false;
    static final int SAMPLE_REFRESH_TIME = 30000;
    private static int SERVICE_ADJ = SystemService.PHASE_SYSTEM_SERVICES_READY;
    private static int SERVICE_B_ADJ = 800;
    static final String TAG = "AWSPRStore";
    int adj;
    boolean killed;
    boolean killedByAm;
    int lastAdj;
    long lastSampleTime;
    long lastSampledMemory;
    ArrayMap<String, Long> launchingMemory;
    String packageName;
    int packageVer;
    int pid;
    ArrayMap<String, ProcessStats.ProcessStateHolder> pkgList;
    String processName;
    int procstats;
    int uid;
    String waitingToKill;

    ProcessRecordStore(IAWSProcessRecord iAWSProcessRecord) {
        this.pkgList = new ArrayMap<>();
        this.launchingMemory = new ArrayMap<>();
        this.lastSampledMemory = 0L;
        update(iAWSProcessRecord);
        this.lastSampledMemory = 0L;
    }

    ProcessRecordStore(String str, int i) {
        this.pkgList = new ArrayMap<>();
        this.launchingMemory = new ArrayMap<>();
        this.lastSampledMemory = 0L;
        this.processName = str;
        this.uid = i;
    }

    ProcessRecordStore(String str, int i, int i2) {
        this.pkgList = new ArrayMap<>();
        this.launchingMemory = new ArrayMap<>();
        this.lastSampledMemory = 0L;
        this.processName = str;
        this.pid = i2;
        this.uid = i;
    }

    ProcessRecordStore(String str, int i, String str2, long j) {
        this.pkgList = new ArrayMap<>();
        this.launchingMemory = new ArrayMap<>();
        this.lastSampledMemory = 0L;
        this.processName = str;
        this.uid = i;
        updateLaunchMem(str2, j);
    }

    public void update(IAWSProcessRecord iAWSProcessRecord) {
        this.processName = iAWSProcessRecord.getProcName();
        this.packageName = iAWSProcessRecord.getPkgName();
        this.packageVer = iAWSProcessRecord.getPkgVer();
        this.pid = iAWSProcessRecord.getPid();
        this.uid = iAWSProcessRecord.getUid();
        this.pkgList = iAWSProcessRecord.getpkgList();
        this.adj = iAWSProcessRecord.getAdj();
        this.procstats = iAWSProcessRecord.getprocState();
        this.killedByAm = iAWSProcessRecord.isKilledByAm();
        this.killed = iAWSProcessRecord.isKilled();
        this.waitingToKill = iAWSProcessRecord.getWaitingToKill();
    }

    public String getProcName() {
        return this.processName;
    }

    public String getPkgName() {
        return this.packageName;
    }

    public int getPkgVer() {
        return this.packageVer;
    }

    public int getPid() {
        return this.pid;
    }

    public int getUid() {
        return this.uid;
    }

    public ArrayMap<String, ProcessStats.ProcessStateHolder> getpkgList() {
        return this.pkgList;
    }

    public int getAdj() {
        return this.adj;
    }

    public int getprocState() {
        return this.procstats;
    }

    public boolean isKilled() {
        return this.killed;
    }

    public boolean isKilledByAm() {
        return this.killedByAm;
    }

    public String getWaitingToKill() {
        return this.waitingToKill;
    }

    public void setPid(int i) {
        this.pid = i;
    }

    public void setUid(int i) {
        this.uid = i;
    }

    public void setAdj(int i) {
        this.adj = i;
    }

    public void setprocState(int i) {
        this.procstats = i;
    }

    public void updateLaunchMem(String str, long j) {
        this.launchingMemory.put(str, Long.valueOf((getLaunchMem(str) + j) / 2));
    }

    public void updateSampledMem(long j) {
        this.lastSampledMemory = j;
        this.lastSampleTime = SystemClock.uptimeMillis();
        this.lastAdj = this.adj;
    }

    public long getLaunchMem(String str) {
        if (this.launchingMemory.containsKey(str)) {
            return this.launchingMemory.get(str).longValue();
        }
        return 0L;
    }

    public long getSampledMem() {
        if (this.adj == SERVICE_ADJ && this.lastAdj == SERVICE_B_ADJ) {
            return this.lastSampledMemory;
        }
        if (this.adj == SERVICE_B_ADJ && this.lastAdj == SERVICE_ADJ) {
            return this.lastSampledMemory;
        }
        long jUptimeMillis = SystemClock.uptimeMillis() - this.lastSampleTime;
        if (this.adj != this.lastAdj) {
            return 0L;
        }
        if (jUptimeMillis <= 30000) {
            return this.lastSampledMemory;
        }
        return 0L;
    }

    protected void dump() {
        Log.v(TAG, "Dump ProcessRecordStore:+ " + this.processName + "(" + this.pid + ":" + this.uid + ")launchMem:" + this.launchingMemory + "sampleMem:" + this.lastSampledMemory);
    }
}
