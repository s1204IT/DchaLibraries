package com.mediatek.appworkingset;

import com.mediatek.am.IAWSProcessRecord;
import com.mediatek.am.IAWSStoreRecord;
import java.util.ArrayList;

public class AWSStoreRecord implements IAWSStoreRecord {
    long mExtraVal;
    IAWSProcessRecord mRecord;
    ArrayList<IAWSProcessRecord> mRecords;
    String mTopPkgName;

    AWSStoreRecord(IAWSStoreRecord iAWSStoreRecord) {
        this.mRecord = iAWSStoreRecord.getRecord();
        this.mTopPkgName = iAWSStoreRecord.getTopPkgName();
        this.mExtraVal = iAWSStoreRecord.getExtraVal();
        ArrayList<IAWSProcessRecord> records = iAWSStoreRecord.getRecords();
        this.mRecords = new ArrayList<>();
        for (IAWSProcessRecord iAWSProcessRecord : records) {
            if (iAWSProcessRecord != null && !iAWSProcessRecord.isKilled() && !iAWSProcessRecord.isKilledByAm() && iAWSProcessRecord.getWaitingToKill() == null) {
                this.mRecords.add(iAWSProcessRecord);
            }
        }
    }

    public ArrayList<IAWSProcessRecord> getRecords() {
        return this.mRecords;
    }

    public IAWSProcessRecord getRecord() {
        return this.mRecord;
    }

    public String getTopPkgName() {
        return this.mTopPkgName;
    }

    public long getExtraVal() {
        return this.mExtraVal;
    }
}
