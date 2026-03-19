package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import java.util.ArrayList;

public class AdnRecordLoader extends Handler {
    protected static final int EVENT_ADN_LOAD_ALL_DONE = 3;
    protected static final int EVENT_ADN_LOAD_DONE = 1;
    protected static final int EVENT_EF_LINEAR_RECORD_SIZE_DONE = 4;
    protected static final int EVENT_EXT_RECORD_LOAD_DONE = 2;
    protected static final int EVENT_UPDATE_RECORD_DONE = 5;
    static final String LOG_TAG = "AdnRecordLoader";
    protected static final boolean VDBG = false;
    protected ArrayList<AdnRecord> mAdns;
    protected int mEf;
    protected int mExtensionEF;
    protected IccFileHandler mFh;
    protected int mPendingExtLoads;
    protected String mPin2;
    protected int mRecordNumber;
    protected Object mResult;
    protected Message mUserResponse;

    public AdnRecordLoader(IccFileHandler iccFileHandler) {
        super(Looper.getMainLooper());
        this.mFh = iccFileHandler;
    }

    protected String getEFPath(int i) {
        if (i == 28474) {
            return "3F007F10";
        }
        return null;
    }

    public void loadFromEF(int i, int i2, int i3, Message message) {
        this.mEf = i;
        this.mExtensionEF = i2;
        this.mRecordNumber = i3;
        this.mUserResponse = message;
        this.mFh.loadEFLinearFixed(i, getEFPath(i), i3, obtainMessage(1));
    }

    public void loadAllFromEF(int i, int i2, Message message) {
        this.mEf = i;
        this.mExtensionEF = i2;
        this.mUserResponse = message;
        this.mFh.loadEFLinearFixedAll(i, getEFPath(i), obtainMessage(3));
    }

    public void updateEF(AdnRecord adnRecord, int i, int i2, int i3, String str, Message message) {
        this.mEf = i;
        this.mExtensionEF = i2;
        this.mRecordNumber = i3;
        this.mUserResponse = message;
        this.mPin2 = str;
        this.mFh.getEFLinearRecordSize(i, getEFPath(i), obtainMessage(4, adnRecord));
    }

    @Override
    public void handleMessage(Message message) {
        try {
            int i = 0;
            switch (message.what) {
                case 1:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    byte[] bArr = (byte[]) asyncResult.result;
                    if (asyncResult.exception != null) {
                        throw new RuntimeException("load failed", asyncResult.exception);
                    }
                    AdnRecord adnRecord = new AdnRecord(this.mEf, this.mRecordNumber, bArr);
                    this.mResult = adnRecord;
                    if (adnRecord.hasExtendedRecord()) {
                        this.mPendingExtLoads = 1;
                        this.mFh.loadEFLinearFixed(this.mExtensionEF, adnRecord.mExtRecord, obtainMessage(2, adnRecord));
                    }
                    break;
                    break;
                case 2:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    byte[] bArr2 = (byte[]) asyncResult2.result;
                    AdnRecord adnRecord2 = (AdnRecord) asyncResult2.userObj;
                    if (asyncResult2.exception == null) {
                        Rlog.d(LOG_TAG, "ADN extension EF: 0x" + Integer.toHexString(this.mExtensionEF) + ":" + adnRecord2.mExtRecord + "\n" + IccUtils.bytesToHexString(bArr2));
                        adnRecord2.appendExtRecord(bArr2);
                    } else {
                        Rlog.e(LOG_TAG, "Failed to read ext record. Clear the number now.");
                        adnRecord2.setNumber("");
                    }
                    this.mPendingExtLoads--;
                    break;
                case 3:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    ArrayList arrayList = (ArrayList) asyncResult3.result;
                    if (asyncResult3.exception != null) {
                        throw new RuntimeException("load failed", asyncResult3.exception);
                    }
                    this.mAdns = new ArrayList<>(arrayList.size());
                    this.mResult = this.mAdns;
                    this.mPendingExtLoads = 0;
                    int size = arrayList.size();
                    while (i < size) {
                        int i2 = 1 + i;
                        AdnRecord adnRecord3 = new AdnRecord(this.mEf, i2, (byte[]) arrayList.get(i));
                        this.mAdns.add(adnRecord3);
                        if (adnRecord3.hasExtendedRecord()) {
                            this.mPendingExtLoads++;
                            this.mFh.loadEFLinearFixed(this.mExtensionEF, adnRecord3.mExtRecord, obtainMessage(2, adnRecord3));
                        }
                        i = i2;
                    }
                    break;
                    break;
                case 4:
                    AsyncResult asyncResult4 = (AsyncResult) message.obj;
                    AdnRecord adnRecord4 = (AdnRecord) asyncResult4.userObj;
                    if (asyncResult4.exception != null) {
                        throw new RuntimeException("get EF record size failed", asyncResult4.exception);
                    }
                    int[] iArr = (int[]) asyncResult4.result;
                    if (iArr.length != 3 || this.mRecordNumber > iArr[2]) {
                        throw new RuntimeException("get wrong EF record size format", asyncResult4.exception);
                    }
                    byte[] bArrBuildAdnString = adnRecord4.buildAdnString(iArr[0]);
                    if (bArrBuildAdnString == null) {
                        throw new RuntimeException("wrong ADN format", asyncResult4.exception);
                    }
                    this.mFh.updateEFLinearFixed(this.mEf, getEFPath(this.mEf), this.mRecordNumber, bArrBuildAdnString, this.mPin2, obtainMessage(5));
                    this.mPendingExtLoads = 1;
                    break;
                    break;
                case 5:
                    AsyncResult asyncResult5 = (AsyncResult) message.obj;
                    if (asyncResult5.exception != null) {
                        throw new RuntimeException("update EF adn record failed", asyncResult5.exception);
                    }
                    this.mPendingExtLoads = 0;
                    this.mResult = null;
                    break;
                    break;
            }
            if (this.mUserResponse != null && this.mPendingExtLoads == 0) {
                AsyncResult.forMessage(this.mUserResponse).result = this.mResult;
                this.mUserResponse.sendToTarget();
                this.mUserResponse = null;
            }
        } catch (RuntimeException e) {
            if (this.mUserResponse != null) {
                AsyncResult.forMessage(this.mUserResponse).exception = e;
                this.mUserResponse.sendToTarget();
                this.mUserResponse = null;
            }
        }
    }
}
