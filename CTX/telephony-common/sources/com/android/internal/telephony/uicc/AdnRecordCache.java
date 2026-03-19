package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import com.android.internal.telephony.gsm.UsimPhoneBookManager;
import java.util.ArrayList;
import java.util.Iterator;

public class AdnRecordCache extends Handler implements IccConstants {
    protected static final int EVENT_LOAD_ALL_ADN_LIKE_DONE = 1;
    protected static final int EVENT_UPDATE_ADN_DONE = 2;
    protected IccFileHandler mFh;
    protected UsimPhoneBookManager mUsimPhoneBookManager;
    protected SparseArray<ArrayList<AdnRecord>> mAdnLikeFiles = new SparseArray<>();
    protected SparseArray<ArrayList<Message>> mAdnLikeWaiters = new SparseArray<>();
    protected SparseArray<Message> mUserWriteResponse = new SparseArray<>();

    public AdnRecordCache(IccFileHandler iccFileHandler) {
        this.mFh = iccFileHandler;
        this.mUsimPhoneBookManager = new UsimPhoneBookManager(this.mFh, this);
    }

    public void reset() {
        this.mAdnLikeFiles.clear();
        this.mUsimPhoneBookManager.reset();
        clearWaiters();
        clearUserWriters();
    }

    protected void clearWaiters() {
        int size = this.mAdnLikeWaiters.size();
        for (int i = 0; i < size; i++) {
            notifyWaiters(this.mAdnLikeWaiters.valueAt(i), new AsyncResult((Object) null, (Object) null, new RuntimeException("AdnCache reset")));
        }
        this.mAdnLikeWaiters.clear();
    }

    private void clearUserWriters() {
        int size = this.mUserWriteResponse.size();
        for (int i = 0; i < size; i++) {
            sendErrorResponse(this.mUserWriteResponse.valueAt(i), "AdnCace reset");
        }
        this.mUserWriteResponse.clear();
    }

    public ArrayList<AdnRecord> getRecordsIfLoaded(int i) {
        return this.mAdnLikeFiles.get(i);
    }

    public int extensionEfForEf(int i) {
        if (i == 20272) {
            return 0;
        }
        if (i == 28480) {
            return IccConstants.EF_EXT1;
        }
        if (i == 28489) {
            return IccConstants.EF_EXT3;
        }
        if (i == 28615) {
            return IccConstants.EF_EXT6;
        }
        switch (i) {
        }
        return IccConstants.EF_EXT1;
    }

    private void sendErrorResponse(Message message, String str) {
        if (message != null) {
            AsyncResult.forMessage(message).exception = new RuntimeException(str);
            message.sendToTarget();
        }
    }

    public void updateAdnByIndex(int i, AdnRecord adnRecord, int i2, String str, Message message) {
        int iExtensionEfForEf = extensionEfForEf(i);
        if (iExtensionEfForEf < 0) {
            sendErrorResponse(message, "EF is not known ADN-like EF:0x" + Integer.toHexString(i).toUpperCase());
            return;
        }
        if (this.mUserWriteResponse.get(i) != null) {
            sendErrorResponse(message, "Have pending update for EF:0x" + Integer.toHexString(i).toUpperCase());
            return;
        }
        this.mUserWriteResponse.put(i, message);
        new AdnRecordLoader(this.mFh).updateEF(adnRecord, i, iExtensionEfForEf, i2, str, obtainMessage(2, i, i2, adnRecord));
    }

    public void updateAdnBySearch(int i, AdnRecord adnRecord, AdnRecord adnRecord2, String str, Message message) {
        ArrayList<AdnRecord> recordsIfLoaded;
        int i2;
        int iExtensionEfForEf = extensionEfForEf(i);
        if (iExtensionEfForEf < 0) {
            sendErrorResponse(message, "EF is not known ADN-like EF:0x" + Integer.toHexString(i).toUpperCase());
            return;
        }
        if (i == 20272) {
            recordsIfLoaded = this.mUsimPhoneBookManager.loadEfFilesFromUsim();
        } else {
            recordsIfLoaded = getRecordsIfLoaded(i);
        }
        if (recordsIfLoaded == null) {
            sendErrorResponse(message, "Adn list not exist for EF:0x" + Integer.toHexString(i).toUpperCase());
            return;
        }
        Iterator<AdnRecord> it = recordsIfLoaded.iterator();
        int i3 = 1;
        while (true) {
            if (it.hasNext()) {
                if (adnRecord.isEqual(it.next())) {
                    break;
                } else {
                    i3++;
                }
            } else {
                i3 = -1;
                break;
            }
        }
        if (i3 == -1) {
            sendErrorResponse(message, "Adn record don't exist for " + adnRecord);
            return;
        }
        if (i == 20272) {
            AdnRecord adnRecord3 = recordsIfLoaded.get(i3 - 1);
            i2 = adnRecord3.mEfid;
            iExtensionEfForEf = adnRecord3.mExtRecord;
            int i4 = adnRecord3.mRecordNumber;
            adnRecord2.mEfid = i2;
            adnRecord2.mExtRecord = iExtensionEfForEf;
            adnRecord2.mRecordNumber = i4;
            i3 = i4;
        } else {
            i2 = i;
        }
        if (this.mUserWriteResponse.get(i2) == null) {
            this.mUserWriteResponse.put(i2, message);
            new AdnRecordLoader(this.mFh).updateEF(adnRecord2, i2, iExtensionEfForEf, i3, str, obtainMessage(2, i2, i3, adnRecord2));
            return;
        }
        sendErrorResponse(message, "Have pending update for EF:0x" + Integer.toHexString(i2).toUpperCase());
    }

    public void requestLoadAllAdnLike(int i, int i2, Message message) {
        ArrayList<AdnRecord> recordsIfLoaded;
        if (i == 20272) {
            recordsIfLoaded = this.mUsimPhoneBookManager.loadEfFilesFromUsim();
        } else {
            recordsIfLoaded = getRecordsIfLoaded(i);
        }
        if (recordsIfLoaded != null) {
            if (message != null) {
                AsyncResult.forMessage(message).result = recordsIfLoaded;
                message.sendToTarget();
                return;
            }
            return;
        }
        ArrayList<Message> arrayList = this.mAdnLikeWaiters.get(i);
        if (arrayList != null) {
            arrayList.add(message);
            return;
        }
        ArrayList<Message> arrayList2 = new ArrayList<>();
        arrayList2.add(message);
        this.mAdnLikeWaiters.put(i, arrayList2);
        if (i2 < 0) {
            if (message != null) {
                AsyncResult.forMessage(message).exception = new RuntimeException("EF is not known ADN-like EF:0x" + Integer.toHexString(i).toUpperCase());
                message.sendToTarget();
                return;
            }
            return;
        }
        new AdnRecordLoader(this.mFh).loadAllFromEF(i, i2, obtainMessage(1, i, 0));
    }

    protected void notifyWaiters(ArrayList<Message> arrayList, AsyncResult asyncResult) {
        if (arrayList == null) {
            return;
        }
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            Message message = arrayList.get(i);
            AsyncResult.forMessage(message, asyncResult.result, asyncResult.exception);
            message.sendToTarget();
        }
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                int i = message.arg1;
                ArrayList<Message> arrayList = this.mAdnLikeWaiters.get(i);
                this.mAdnLikeWaiters.delete(i);
                if (asyncResult.exception == null) {
                    this.mAdnLikeFiles.put(i, (ArrayList) asyncResult.result);
                }
                notifyWaiters(arrayList, asyncResult);
                break;
            case 2:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                int i2 = message.arg1;
                int i3 = message.arg2;
                AdnRecord adnRecord = (AdnRecord) asyncResult2.userObj;
                if (asyncResult2.exception == null) {
                    this.mAdnLikeFiles.get(i2).set(i3 - 1, adnRecord);
                    this.mUsimPhoneBookManager.invalidateCache();
                }
                Message message2 = this.mUserWriteResponse.get(i2);
                this.mUserWriteResponse.delete(i2);
                if (message2 != null) {
                    AsyncResult.forMessage(message2, (Object) null, asyncResult2.exception);
                    message2.sendToTarget();
                }
                break;
        }
    }
}
