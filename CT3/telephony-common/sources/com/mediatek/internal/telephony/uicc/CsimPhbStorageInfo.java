package com.mediatek.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.util.ArrayList;

public class CsimPhbStorageInfo extends Handler {
    static final String LOG_TAG = "CsimphbStorageInfo";
    static IccFileHandler sFh;
    static int[] sAdnRecordSize = {-1, -1, -1, -1};
    static int sMaxNameLength = 0;
    static int sMaxnumberLength = 20;

    public CsimPhbStorageInfo() {
        Rlog.d(LOG_TAG, " CsimphbStorageInfo constructor finished. ");
    }

    public static void setMaxNameLength(int mMaxNameLength) {
        sMaxNameLength = mMaxNameLength;
        Rlog.d(LOG_TAG, " [setMaxNameLength] sMaxNameLength = " + sMaxNameLength);
    }

    public static void setPhbRecordStorageInfo(int totalSize, int usedRecord) {
        sAdnRecordSize[0] = usedRecord;
        sAdnRecordSize[1] = totalSize;
        Rlog.d(LOG_TAG, " [setPhbRecordStorageInfo] usedRecord = " + usedRecord + " | totalSize =" + totalSize);
    }

    public static void checkPhbRecordInfo(Message response) {
        sAdnRecordSize[2] = 20;
        sAdnRecordSize[3] = sMaxNameLength;
        Rlog.d(LOG_TAG, " [getPhbRecordInfo] sAdnRecordSize[0] = " + sAdnRecordSize[0] + " sAdnRecordSize[1] = " + sAdnRecordSize[1] + " sAdnRecordSize[2] = " + sAdnRecordSize[2] + " sAdnRecordSize[3] = " + sAdnRecordSize[3]);
        if (response != null) {
            AsyncResult.forMessage(response).result = sAdnRecordSize;
            response.sendToTarget();
        }
    }

    public static int[] getPhbRecordStorageInfo() {
        return sAdnRecordSize;
    }

    public static void clearAdnRecordSize() {
        Rlog.d(LOG_TAG, " clearAdnRecordSize");
        if (sAdnRecordSize == null) {
            return;
        }
        for (int i = 0; i < sAdnRecordSize.length; i++) {
            sAdnRecordSize[i] = -1;
        }
    }

    public static boolean updatePhbStorageInfo(int update) {
        int[] stroageInfo = getPhbRecordStorageInfo();
        int used = stroageInfo[0];
        int total = stroageInfo[1];
        Rlog.d(LOG_TAG, " [updatePhbStorageInfo] used " + used + " | total : " + total + " | update : " + update);
        if (used > -1) {
            int newUsed = used + update;
            setPhbRecordStorageInfo(total, newUsed);
            return true;
        }
        Rlog.d(LOG_TAG, " the storage info is not ready return false");
        return false;
    }

    public static void checkPhbStorage(ArrayList<AdnRecord> adnList) {
        int[] stroageInfo = getPhbRecordStorageInfo();
        int usedStorage = stroageInfo[0];
        int totalStorage = stroageInfo[1];
        if (adnList == null) {
            return;
        }
        int totalSize = adnList.size();
        int usedRecord = 0;
        for (int i = 0; i < totalSize; i++) {
            if (!adnList.get(i).isEmpty()) {
                usedRecord++;
                Rlog.d(LOG_TAG, " print userRecord: " + adnList.get(i));
            }
        }
        Rlog.d(LOG_TAG, " checkPhbStorage totalSize = " + totalSize + " usedRecord = " + usedRecord);
        Rlog.d(LOG_TAG, " checkPhbStorage totalStorage = " + totalStorage + " usedStorage = " + usedStorage);
        if (totalStorage > -1) {
            int newUsed = usedRecord + usedStorage;
            int newTotal = totalStorage + totalSize;
            setPhbRecordStorageInfo(newTotal, newUsed);
            return;
        }
        setPhbRecordStorageInfo(totalSize, usedRecord);
    }
}
