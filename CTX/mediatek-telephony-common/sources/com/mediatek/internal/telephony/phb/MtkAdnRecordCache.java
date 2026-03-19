package com.mediatek.internal.telephony.phb;

import android.hardware.radio.V1_0.RadioError;
import android.os.AsyncResult;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.util.SparseArray;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.uicc.MtkUiccCardApplication;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MtkAdnRecordCache extends AdnRecordCache {
    private static final int ADN_FILE_SIZE = 250;
    private static final boolean DBG;
    private static final String LOG_TAG = "MtkAdnRecordCache";
    public static final int MAX_PHB_NAME_LENGTH = 60;
    public static final int MAX_PHB_NUMBER_ANR_COUNT = 1;
    public static final int MAX_PHB_NUMBER_ANR_LENGTH = 20;
    public static final int MAX_PHB_NUMBER_LENGTH = 40;
    private static final String PROP_FORCE_DEBUG_KEY = "persist.vendor.log.tel_dbg";
    private CommandsInterface mCi;
    private UiccCardApplication mCurrentApp;
    private final Object mLock;
    protected SparseArray<ArrayList<MtkAdnRecord>> mMtkAdnLikeFiles;
    private boolean mNeedToWait;
    private int mSlotId;
    private boolean mSuccess;
    private MtkUsimPhoneBookManager mUsimPhoneBookManager;

    static {
        boolean z = false;
        if (SystemProperties.getInt(PROP_FORCE_DEBUG_KEY, 0) == 1 && !SystemProperties.get("ro.build.type").equals(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER)) {
            z = true;
        }
        DBG = z;
    }

    public MtkAdnRecordCache(IccFileHandler iccFileHandler, CommandsInterface commandsInterface, UiccCardApplication uiccCardApplication) {
        super(iccFileHandler);
        this.mSlotId = -1;
        this.mMtkAdnLikeFiles = new SparseArray<>();
        this.mLock = new Object();
        this.mSuccess = false;
        this.mNeedToWait = false;
        this.mCi = commandsInterface;
        this.mCurrentApp = uiccCardApplication;
        this.mUsimPhoneBookManager = new MtkUsimPhoneBookManager(this.mFh, this, commandsInterface, uiccCardApplication);
        if (uiccCardApplication != null) {
            this.mSlotId = ((MtkUiccCardApplication) uiccCardApplication).getPhoneId();
        }
    }

    public void reset() {
        logi("reset");
        this.mMtkAdnLikeFiles.clear();
        this.mUsimPhoneBookManager.reset();
        clearWaiters();
        clearUserWriters();
        if (!CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
            CsimPhbUtil.clearAdnRecordSize();
        }
    }

    public int getSlotId() {
        return this.mSlotId;
    }

    private void clearUserWriters() {
        int i;
        logi("clearUserWriters");
        synchronized (this.mLock) {
            logi("mNeedToWait " + this.mNeedToWait);
            if (this.mNeedToWait) {
                this.mNeedToWait = false;
                this.mLock.notifyAll();
            }
        }
        int size = this.mUserWriteResponse.size();
        for (i = 0; i < size; i++) {
            sendErrorResponse((Message) this.mUserWriteResponse.valueAt(i), "AdnCace reset " + this.mUserWriteResponse.valueAt(i));
        }
        this.mUserWriteResponse.clear();
    }

    private void sendErrorResponse(Message message, String str) {
        sendErrorResponse(message, str, 2);
    }

    private void sendErrorResponse(Message message, String str, int i) {
        CommandException commandExceptionFromRilErrno = CommandException.fromRilErrno(i);
        if (message != null) {
            logw(str);
            AsyncResult.forMessage(message).exception = commandExceptionFromRilErrno;
            message.sendToTarget();
        }
    }

    public synchronized void updateAdnByIndex(int i, MtkAdnRecord mtkAdnRecord, int i2, String str, Message message) {
        int i3;
        int i4;
        MtkAdnRecord mtkAdnRecord2;
        Object obj;
        logd("updateAdnByIndex efid:" + i + ", pin2:" + str + ", recordIndex:" + i2 + ", adn [" + mtkAdnRecord + "]");
        int iExtensionEfForEf = extensionEfForEf(i);
        if (iExtensionEfForEf < 0) {
            sendErrorResponse(message, "EF is not known ADN-like EF:0x" + Integer.toHexString(i).toUpperCase());
            return;
        }
        if (mtkAdnRecord.mAlphaTag.length() > 60) {
            sendErrorResponse(message, "the input length of mAlphaTag is too long: " + mtkAdnRecord.mAlphaTag, RadioError.OEM_ERROR_2);
            return;
        }
        int length = mtkAdnRecord.mNumber.length();
        if (mtkAdnRecord.mNumber.indexOf(43) != -1) {
            length--;
        }
        if (length > 40) {
            sendErrorResponse(message, "the input length of phoneNumber is too long: " + mtkAdnRecord.mNumber, RadioError.OEM_ERROR_1);
            return;
        }
        for (int i5 = 0; i5 < 1; i5++) {
            String additionalNumber = mtkAdnRecord.getAdditionalNumber(i5);
            if (additionalNumber != null) {
                int length2 = additionalNumber.length();
                if (additionalNumber.indexOf(43) != -1) {
                    length2--;
                }
                if (length2 > 20) {
                    sendErrorResponse(message, "the input length of additional number is too long: " + additionalNumber, RadioError.OEM_ERROR_5);
                    return;
                }
            }
        }
        if (!this.mUsimPhoneBookManager.checkEmailLength(mtkAdnRecord.mEmails)) {
            sendErrorResponse(message, "the email string is too long", RadioError.OEM_ERROR_9);
            return;
        }
        if (i == 20272) {
            ArrayList<MtkAdnRecord> arrayListLoadEfFilesFromUsim = this.mUsimPhoneBookManager.loadEfFilesFromUsim(null);
            if (arrayListLoadEfFilesFromUsim == null) {
                sendErrorResponse(message, "Adn list not exist for EF:" + i, RadioError.OEM_ERROR_7);
                return;
            }
            mtkAdnRecord2 = arrayListLoadEfFilesFromUsim.get(i2 - 1);
            int i6 = mtkAdnRecord2.mEfid;
            int i7 = mtkAdnRecord2.mExtRecord;
            mtkAdnRecord.mEfid = i6;
            i3 = i6;
            i4 = i7;
        } else {
            i3 = i;
            i4 = iExtensionEfForEf;
            mtkAdnRecord2 = null;
        }
        if (!this.mUsimPhoneBookManager.checkEmailCapacityFree(i2, mtkAdnRecord.mEmails, mtkAdnRecord2)) {
            sendErrorResponse(message, "drop the email for the limitation of the SIM card", RadioError.OEM_ERROR_8);
            return;
        }
        for (int i8 = 0; i8 < 1; i8++) {
            String additionalNumber2 = mtkAdnRecord.getAdditionalNumber(i8);
            if (!this.mUsimPhoneBookManager.isAnrCapacityFree(additionalNumber2, i2, i8, mtkAdnRecord2)) {
                sendErrorResponse(message, "drop the additional number for the update fail: " + additionalNumber2, RadioError.OEM_ERROR_6);
                return;
            }
        }
        if (!this.mUsimPhoneBookManager.checkSneCapacityFree(i2, mtkAdnRecord.mSne, mtkAdnRecord2)) {
            sendErrorResponse(message, "drop the sne for the limitation of the SIM card", RadioError.OEM_ERROR_10);
            return;
        }
        if (((Message) this.mUserWriteResponse.get(i3)) != null) {
            sendErrorResponse(message, "Have pending update for EF:0x" + Integer.toHexString(i3).toUpperCase());
            return;
        }
        this.mUserWriteResponse.put(i3, message);
        if ((i3 == 28474 || i3 == 20272 || i3 == 20282 || i3 == 20283 || i3 == 20284 || i3 == 20285) && mtkAdnRecord.mAlphaTag.length() == 0 && mtkAdnRecord.mNumber.length() == 0) {
            this.mUsimPhoneBookManager.removeContactGroup(i2);
        }
        if (this.mUserWriteResponse.size() == 0) {
            return;
        }
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                try {
                    this.mSuccess = false;
                    this.mNeedToWait = true;
                    obj = obj2;
                    int i9 = i3;
                    new MtkAdnRecordLoader(this.mFh).updateEF(mtkAdnRecord, i3, i4, i2, str, obtainMessage(2, i3, i2, mtkAdnRecord));
                    while (this.mNeedToWait) {
                        try {
                            this.mLock.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    if (this.mSuccess) {
                        if (i9 == 28474 || i9 == 20272 || i9 == 20282 || i9 == 20283 || i9 == 20284 || i9 == 20285) {
                            try {
                                int iUpdateSneByAdnIndex = this.mUsimPhoneBookManager.updateSneByAdnIndex(mtkAdnRecord.mSne, i2, mtkAdnRecord2);
                                if (-30 == iUpdateSneByAdnIndex) {
                                    sendErrorResponse(message, "drop the SNE for the limitation of the SIM card", RadioError.OEM_ERROR_10);
                                } else if (-40 == iUpdateSneByAdnIndex) {
                                    sendErrorResponse(message, "the sne string is too long", RadioError.OEM_ERROR_11);
                                } else {
                                    for (int i10 = 0; i10 < 1; i10++) {
                                        this.mUsimPhoneBookManager.updateAnrByAdnIndex(mtkAdnRecord.getAdditionalNumber(i10), i2, i10, mtkAdnRecord2);
                                    }
                                    int iUpdateEmailsByAdnIndex = this.mUsimPhoneBookManager.updateEmailsByAdnIndex(mtkAdnRecord.mEmails, i2, mtkAdnRecord2);
                                    if (-30 == iUpdateEmailsByAdnIndex) {
                                        sendErrorResponse(message, "drop the email for the limitation of the SIM card", RadioError.OEM_ERROR_8);
                                    } else if (-40 == iUpdateEmailsByAdnIndex) {
                                        sendErrorResponse(message, "the email string is too long", RadioError.OEM_ERROR_9);
                                    } else if (-50 == iUpdateEmailsByAdnIndex) {
                                        sendErrorResponse(message, "Unkown error occurs when update email", 2);
                                    } else {
                                        AsyncResult.forMessage(message, (Object) null, (Throwable) null);
                                        message.sendToTarget();
                                    }
                                }
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                        } else if (i9 == 28475) {
                            AsyncResult.forMessage(message, (Object) null, (Throwable) null);
                            message.sendToTarget();
                        }
                    }
                } catch (Throwable th) {
                    th = th;
                    obj = obj2;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public synchronized int updateAdnBySearch(int i, MtkAdnRecord mtkAdnRecord, MtkAdnRecord mtkAdnRecord2, String str, Message message, Object obj) {
        ArrayList<MtkAdnRecord> recordsIfLoaded;
        int i2;
        int i3;
        MtkAdnRecord mtkAdnRecord3;
        Object obj2;
        int i4 = i;
        synchronized (this) {
            logd("updateAdnBySearch efid:" + i4 + ", pin2:" + str + ", oldAdn [" + mtkAdnRecord + "], new Adn[" + mtkAdnRecord2 + "]");
            int iExtensionEfForEf = extensionEfForEf(i);
            if (iExtensionEfForEf < 0) {
                sendErrorResponse(message, "EF is not known ADN-like EF:0x" + Integer.toHexString(i).toUpperCase());
                return -1;
            }
            if (mtkAdnRecord2.mAlphaTag.length() > 60) {
                sendErrorResponse(message, "the input length of mAlphaTag is too long: " + mtkAdnRecord2.mAlphaTag, RadioError.OEM_ERROR_2);
                return -1;
            }
            int length = mtkAdnRecord2.mNumber.length();
            if (mtkAdnRecord2.mNumber.indexOf(43) != -1) {
                length--;
            }
            if (length > 40) {
                sendErrorResponse(message, "the input length of phoneNumber is too long: " + mtkAdnRecord2.mNumber, RadioError.OEM_ERROR_1);
                return -1;
            }
            for (int i5 = 0; i5 < 1; i5++) {
                String additionalNumber = mtkAdnRecord2.getAdditionalNumber(i5);
                if (additionalNumber != null) {
                    int length2 = additionalNumber.length();
                    if (additionalNumber.indexOf(43) != -1) {
                        length2--;
                    }
                    if (length2 > 20) {
                        sendErrorResponse(message, "the input length of additional number is too long: " + additionalNumber, RadioError.OEM_ERROR_5);
                        return -1;
                    }
                }
            }
            if (!this.mUsimPhoneBookManager.checkEmailLength(mtkAdnRecord2.mEmails)) {
                sendErrorResponse(message, "the email string is too long", RadioError.OEM_ERROR_9);
                return -1;
            }
            if (i4 == 20272) {
                recordsIfLoaded = this.mUsimPhoneBookManager.loadEfFilesFromUsim(null);
            } else {
                recordsIfLoaded = getRecordsIfLoaded(i4, null);
            }
            if (recordsIfLoaded == null) {
                sendErrorResponse(message, "Adn list not exist for EF:" + i4, RadioError.OEM_ERROR_7);
                return -1;
            }
            Iterator<MtkAdnRecord> it = recordsIfLoaded.iterator();
            int i6 = 1;
            while (true) {
                if (it.hasNext()) {
                    if (mtkAdnRecord.isEqual(it.next())) {
                        break;
                    }
                    i6++;
                } else {
                    i6 = -1;
                    break;
                }
            }
            logi("updateAdnBySearch index " + i6);
            if (i6 == -1) {
                if (mtkAdnRecord.mAlphaTag.length() == 0 && mtkAdnRecord.mNumber.length() == 0) {
                    sendErrorResponse(message, "Adn record don't exist for " + mtkAdnRecord, RadioError.OEM_ERROR_3);
                } else {
                    sendErrorResponse(message, "Adn record don't exist for " + mtkAdnRecord);
                }
                return i6;
            }
            if (i4 == 20272) {
                MtkAdnRecord mtkAdnRecord4 = recordsIfLoaded.get(i6 - 1);
                int i7 = mtkAdnRecord4.mEfid;
                int i8 = mtkAdnRecord4.mExtRecord;
                int i9 = mtkAdnRecord4.mRecordNumber;
                mtkAdnRecord2.mEfid = i7;
                mtkAdnRecord2.mExtRecord = i8;
                mtkAdnRecord2.mRecordNumber = i9;
                mtkAdnRecord3 = mtkAdnRecord4;
                i4 = i7;
                i2 = i8;
                i3 = i9;
            } else {
                i2 = iExtensionEfForEf;
                i3 = i6;
                mtkAdnRecord3 = null;
            }
            if (((Message) this.mUserWriteResponse.get(i4)) != null) {
                sendErrorResponse(message, "Have pending update for EF:0x" + Integer.toHexString(i4).toUpperCase());
                return i3;
            }
            if (i4 == 0) {
                sendErrorResponse(message, "Abnormal efid: " + i4);
                return i3;
            }
            if (!this.mUsimPhoneBookManager.checkEmailCapacityFree(i3, mtkAdnRecord2.mEmails, mtkAdnRecord3)) {
                sendErrorResponse(message, "drop the email for the limitation of the SIM card", RadioError.OEM_ERROR_8);
                return i3;
            }
            for (int i10 = 0; i10 < 1; i10++) {
                String additionalNumber2 = mtkAdnRecord2.getAdditionalNumber(i10);
                if (!this.mUsimPhoneBookManager.isAnrCapacityFree(additionalNumber2, i3, i10, mtkAdnRecord3)) {
                    sendErrorResponse(message, "drop the additional number for the write fail: " + additionalNumber2, RadioError.OEM_ERROR_6);
                    return i3;
                }
            }
            if (!this.mUsimPhoneBookManager.checkSneCapacityFree(i3, mtkAdnRecord2.mSne, mtkAdnRecord3)) {
                sendErrorResponse(message, "drop the sne for the limitation of the SIM card", RadioError.OEM_ERROR_10);
                return i3;
            }
            this.mUserWriteResponse.put(i4, message);
            Object obj3 = this.mLock;
            try {
                synchronized (obj3) {
                    try {
                        this.mSuccess = false;
                        this.mNeedToWait = true;
                        obj2 = obj3;
                        MtkAdnRecord mtkAdnRecord5 = mtkAdnRecord3;
                        new MtkAdnRecordLoader(this.mFh).updateEF(mtkAdnRecord2, i4, i2, i3, str, obtainMessage(2, i4, i3, mtkAdnRecord2));
                        while (this.mNeedToWait) {
                            try {
                                this.mLock.wait();
                            } catch (InterruptedException e) {
                                return i3;
                            }
                        }
                        if (!this.mSuccess) {
                            loge("updateAdnBySearch mSuccess:" + this.mSuccess);
                            return i3;
                        }
                        if (i4 == 28474 || i4 == 20272 || i4 == 20282 || i4 == 20283 || i4 == 20284 || i4 == 20285) {
                            int iUpdateSneByAdnIndex = this.mUsimPhoneBookManager.updateSneByAdnIndex(mtkAdnRecord2.mSne, i3, mtkAdnRecord5);
                            if (-30 == iUpdateSneByAdnIndex) {
                                sendErrorResponse(message, "drop the SNE for the limitation of the SIM card", RadioError.OEM_ERROR_10);
                            } else if (-40 == iUpdateSneByAdnIndex) {
                                sendErrorResponse(message, "the sne string is too long", RadioError.OEM_ERROR_11);
                            } else {
                                for (int i11 = 0; i11 < 1; i11++) {
                                    this.mUsimPhoneBookManager.updateAnrByAdnIndex(mtkAdnRecord2.getAdditionalNumber(i11), i3, i11, mtkAdnRecord5);
                                }
                                int iUpdateEmailsByAdnIndex = this.mUsimPhoneBookManager.updateEmailsByAdnIndex(mtkAdnRecord2.mEmails, i3, mtkAdnRecord5);
                                if (-30 == iUpdateEmailsByAdnIndex) {
                                    sendErrorResponse(message, "drop the email for the limitation of the SIM card", RadioError.OEM_ERROR_8);
                                } else if (-40 == iUpdateEmailsByAdnIndex) {
                                    sendErrorResponse(message, "the email string is too long", RadioError.OEM_ERROR_9);
                                } else if (-50 == iUpdateEmailsByAdnIndex) {
                                    sendErrorResponse(message, "Unkown error occurs when update email", 2);
                                } else {
                                    logd("updateAdnBySearch response:" + message);
                                    AsyncResult.forMessage(message, (Object) null, (Throwable) null);
                                    message.sendToTarget();
                                }
                            }
                        } else if (i4 == 28475) {
                            logd("updateAdnBySearch FDN response:" + message);
                            AsyncResult.forMessage(message, (Object) null, (Throwable) null);
                            message.sendToTarget();
                        }
                        return i3;
                    } catch (Throwable th) {
                        th = th;
                        obj2 = obj3;
                        throw th;
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    public void requestLoadAllAdnLike(int i, int i2, Message message) {
        ArrayList<MtkAdnRecord> recordsIfLoaded;
        logd("requestLoadAllAdnLike efid = " + i + ", extensionEf = " + i2);
        if (i == 20272) {
            recordsIfLoaded = this.mUsimPhoneBookManager.loadEfFilesFromUsim(null);
        } else {
            recordsIfLoaded = getRecordsIfLoaded(i, null);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("requestLoadAllAdnLike efid = ");
        sb.append(i);
        sb.append(", result = null ?");
        sb.append(recordsIfLoaded == null);
        logi(sb.toString());
        if (recordsIfLoaded != null) {
            if (message != null) {
                AsyncResult.forMessage(message).result = recordsIfLoaded;
                message.sendToTarget();
                return;
            }
            return;
        }
        if (recordsIfLoaded == null && i == 20272) {
            sendErrorResponse(message, "Error occurs when query PBR", 2);
            return;
        }
        ArrayList arrayList = (ArrayList) this.mAdnLikeWaiters.get(i);
        if (arrayList != null) {
            arrayList.add(message);
            return;
        }
        ArrayList arrayList2 = new ArrayList();
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
        new MtkAdnRecordLoader(this.mFh).loadAllFromEF(i, i2, obtainMessage(1, i, 0));
    }

    protected void notifyWaiters(ArrayList<Message> arrayList, AsyncResult asyncResult) {
        if (arrayList == null) {
            return;
        }
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            Message message = arrayList.get(i);
            if (message != null) {
                logi("NotifyWaiters: " + message);
                AsyncResult.forMessage(message, asyncResult.result, asyncResult.exception);
                message.sendToTarget();
            }
        }
    }

    public ArrayList<MtkAdnRecord> getRecordsIfLoaded(int i, Object obj) {
        return this.mMtkAdnLikeFiles.get(i);
    }

    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                int i = message.arg1;
                ArrayList<Message> arrayList = (ArrayList) this.mAdnLikeWaiters.get(i);
                this.mAdnLikeWaiters.delete(i);
                if (asyncResult.exception == null) {
                    this.mMtkAdnLikeFiles.put(i, (ArrayList) asyncResult.result);
                } else {
                    Rlog.w(LOG_TAG, "EVENT_LOAD_ALL_ADN_LIKE_DONE exception(slot " + this.mSlotId + ")", asyncResult.exception);
                }
                notifyWaiters(arrayList, asyncResult);
                return;
            case 2:
                logd("EVENT_UPDATE_ADN_DONE");
                synchronized (this.mLock) {
                    if (this.mNeedToWait) {
                        AsyncResult asyncResult2 = (AsyncResult) message.obj;
                        int i2 = message.arg1;
                        int i3 = message.arg2;
                        MtkAdnRecord mtkAdnRecord = (MtkAdnRecord) asyncResult2.userObj;
                        boolean z = true;
                        if (asyncResult2.exception == null && mtkAdnRecord != null) {
                            mtkAdnRecord.setRecordIndex(i3);
                            if (mtkAdnRecord.mEfid <= 0) {
                                mtkAdnRecord.mEfid = i2;
                            }
                            logd("mMtkAdnLikeFiles changed index:" + i3 + ",adn:" + mtkAdnRecord + "  efid:" + i2);
                            if (this.mMtkAdnLikeFiles != null && this.mMtkAdnLikeFiles.get(i2) != null) {
                                if (i2 == 20283 && !CsimPhbUtil.hasModemPhbEnhanceCapability(this.mFh)) {
                                    i3 -= 250;
                                }
                                this.mMtkAdnLikeFiles.get(i2).set(i3 - 1, mtkAdnRecord);
                                logd(" index:" + i3 + "   efid:" + i2);
                            }
                            if (this.mUsimPhoneBookManager != null && i2 != 28475) {
                                if (i2 == 20283) {
                                    i3 += 250;
                                    logd(" index2:" + i3);
                                }
                                this.mUsimPhoneBookManager.updateUsimPhonebookRecordsList(i3 - 1, mtkAdnRecord);
                            }
                        }
                        Message message2 = (Message) this.mUserWriteResponse.get(i2);
                        this.mUserWriteResponse.delete(i2);
                        logi("MtkAdnRecordCache: " + asyncResult2.exception);
                        if (asyncResult2.exception != null && message2 != null) {
                            AsyncResult.forMessage(message2, (Object) null, asyncResult2.exception);
                            message2.sendToTarget();
                        }
                        if (asyncResult2.exception != null) {
                            z = false;
                        }
                        this.mSuccess = z;
                        this.mNeedToWait = false;
                        this.mLock.notifyAll();
                    }
                    break;
                }
                return;
            default:
                return;
        }
    }

    protected void logd(String str) {
        if (DBG) {
            Rlog.d(LOG_TAG, str + "(slot " + this.mSlotId + ")");
        }
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, str + "(slot " + this.mSlotId + ")");
    }

    protected void logi(String str) {
        Rlog.i(LOG_TAG, str + "(slot " + this.mSlotId + ")");
    }

    protected void logw(String str) {
        Rlog.w(LOG_TAG, str + "(slot " + this.mSlotId + ")");
    }

    public List<UsimGroup> getUsimGroups() {
        return this.mUsimPhoneBookManager.getUsimGroups();
    }

    public String getUsimGroupById(int i) {
        return this.mUsimPhoneBookManager.getUsimGroupById(i);
    }

    public boolean removeUsimGroupById(int i) {
        return this.mUsimPhoneBookManager.removeUsimGroupById(i);
    }

    public int insertUsimGroup(String str) {
        return this.mUsimPhoneBookManager.insertUsimGroup(str);
    }

    public int updateUsimGroup(int i, String str) {
        return this.mUsimPhoneBookManager.updateUsimGroup(i, str);
    }

    public boolean addContactToGroup(int i, int i2) {
        return this.mUsimPhoneBookManager.addContactToGroup(i, i2);
    }

    public boolean removeContactFromGroup(int i, int i2) {
        return this.mUsimPhoneBookManager.removeContactFromGroup(i, i2);
    }

    public boolean updateContactToGroups(int i, int[] iArr) {
        return this.mUsimPhoneBookManager.updateContactToGroups(i, iArr);
    }

    public boolean moveContactFromGroupsToGroups(int i, int[] iArr, int[] iArr2) {
        return this.mUsimPhoneBookManager.moveContactFromGroupsToGroups(i, iArr, iArr2);
    }

    public int hasExistGroup(String str) {
        return this.mUsimPhoneBookManager.hasExistGroup(str);
    }

    public int getUsimGrpMaxNameLen() {
        return this.mUsimPhoneBookManager.getUsimGrpMaxNameLen();
    }

    public int getUsimGrpMaxCount() {
        return this.mUsimPhoneBookManager.getUsimGrpMaxCount();
    }

    private void dumpAdnLikeFile() {
        int size = this.mMtkAdnLikeFiles.size();
        logd("dumpAdnLikeFile size " + size);
        for (int i = 0; i < size; i++) {
            int iKeyAt = this.mMtkAdnLikeFiles.keyAt(i);
            ArrayList<MtkAdnRecord> arrayList = this.mMtkAdnLikeFiles.get(iKeyAt);
            logd("dumpAdnLikeFile index " + i + " key " + iKeyAt + "records size " + arrayList.size());
            for (int i2 = 0; i2 < arrayList.size(); i2++) {
                logd("mMtkAdnLikeFiles[" + i2 + "]=" + arrayList.get(i2));
            }
        }
    }

    public ArrayList<AlphaTag> getUsimAasList() {
        return this.mUsimPhoneBookManager.getUsimAasList();
    }

    public String getUsimAasById(int i) {
        return this.mUsimPhoneBookManager.getUsimAasById(i, 0);
    }

    public boolean removeUsimAasById(int i, int i2) {
        return this.mUsimPhoneBookManager.removeUsimAasById(i, i2);
    }

    public int insertUsimAas(String str) {
        return this.mUsimPhoneBookManager.insertUsimAas(str);
    }

    public boolean updateUsimAas(int i, int i2, String str) {
        return this.mUsimPhoneBookManager.updateUsimAas(i, i2, str);
    }

    public boolean updateAdnAas(int i, int i2) {
        return this.mUsimPhoneBookManager.updateAdnAas(i, i2);
    }

    public int getAnrCount() {
        return this.mUsimPhoneBookManager.getAnrCount();
    }

    public int getEmailCount() {
        return this.mUsimPhoneBookManager.getEmailCount();
    }

    public int getUsimAasMaxCount() {
        return this.mUsimPhoneBookManager.getUsimAasMaxCount();
    }

    public int getUsimAasMaxNameLen() {
        return this.mUsimPhoneBookManager.getUsimAasMaxNameLen();
    }

    public boolean hasSne() {
        return this.mUsimPhoneBookManager.hasSne();
    }

    public int getSneRecordLen() {
        return this.mUsimPhoneBookManager.getSneRecordLen();
    }

    public boolean isAdnAccessible() {
        return this.mUsimPhoneBookManager.isAdnAccessible();
    }

    public boolean isUsimPhbEfAndNeedReset(int i) {
        return this.mUsimPhoneBookManager.isUsimPhbEfAndNeedReset(i);
    }

    public UsimPBMemInfo[] getPhonebookMemStorageExt() {
        return this.mUsimPhoneBookManager.getPhonebookMemStorageExt();
    }

    public int getUpbDone() {
        return this.mUsimPhoneBookManager.getUpbDone();
    }

    public int[] getAdnRecordsCapacity() {
        return this.mUsimPhoneBookManager.getAdnRecordsCapacity();
    }
}
