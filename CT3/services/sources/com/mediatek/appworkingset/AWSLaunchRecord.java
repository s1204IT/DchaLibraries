package com.mediatek.appworkingset;

import com.mediatek.am.IAWSProcessRecord;
import java.util.ArrayList;

public class AWSLaunchRecord {
    static final int HOME_ACTIVITY_TYPE = 1;
    static final int RECENTS_ACTIVITY_TYPE = 2;
    private int mNextActType;
    private String mNextPkgName;
    private String mPrevPkgName;
    private int mPrevtActType;
    private ArrayList<IAWSProcessRecord> mRunningProcessesRecords = new ArrayList<>();
    private int mWaitProcessPID;

    AWSLaunchRecord(String str, String str2, int i, int i2, int i3, ArrayList<IAWSProcessRecord> arrayList) {
        this.mPrevPkgName = str;
        this.mNextPkgName = str2;
        this.mPrevtActType = i;
        this.mNextActType = i2;
        this.mWaitProcessPID = i3;
        for (IAWSProcessRecord iAWSProcessRecord : arrayList) {
            if (iAWSProcessRecord != null && !iAWSProcessRecord.isKilled() && !iAWSProcessRecord.isKilledByAm() && iAWSProcessRecord.getWaitingToKill() == null) {
                this.mRunningProcessesRecords.add(iAWSProcessRecord);
            }
        }
    }

    public String getNextPkgName() {
        return this.mNextPkgName;
    }

    public String getPrevPkgName() {
        return this.mPrevPkgName;
    }

    public boolean isLaunchingFromHome() {
        return this.mPrevtActType == 1;
    }

    public boolean isLaunchingFromRecentApp() {
        if (this.mPrevtActType == 2) {
            return true;
        }
        return false;
    }

    public boolean isLaunchingToHome() {
        return this.mNextActType == 1;
    }

    public boolean isLaunchingToRecentApp() {
        if (this.mNextActType == 2) {
            return true;
        }
        return false;
    }

    public int getWaitProcessPID() {
        return this.mWaitProcessPID;
    }

    public ArrayList<IAWSProcessRecord> getRunningProcessesRecords() {
        return this.mRunningProcessesRecords;
    }
}
