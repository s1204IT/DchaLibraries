package com.android.internal.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.AdnRecord;
import com.android.internal.telephony.uicc.AdnRecordCache;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccConstants;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class IccPhoneBookInterfaceManager {
    protected static final boolean ALLOW_SIM_OP_IN_UI_THREAD = false;
    protected static final boolean DBG = true;
    protected static final int EVENT_GET_SIZE_DONE = 1;
    protected static final int EVENT_LOAD_DONE = 2;
    protected static final int EVENT_UPDATE_DONE = 3;
    static final String LOG_TAG = "IccPhoneBookIM";
    protected AdnRecordCache mAdnCache;
    protected Phone mPhone;
    protected int[] mRecordSize;
    protected List<AdnRecord> mRecords;
    protected boolean mSuccess;
    private UiccCardApplication mCurrentApp = null;
    protected final Object mLock = new Object();
    private boolean mIs3gCard = false;
    protected Handler mBaseHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (asyncResult.exception == null) {
                            IccPhoneBookInterfaceManager.this.mRecordSize = (int[]) asyncResult.result;
                            IccPhoneBookInterfaceManager.this.logd("GET_RECORD_SIZE Size " + IccPhoneBookInterfaceManager.this.mRecordSize[0] + " total " + IccPhoneBookInterfaceManager.this.mRecordSize[1] + " #record " + IccPhoneBookInterfaceManager.this.mRecordSize[2]);
                        }
                        notifyPending(asyncResult);
                        break;
                    }
                    return;
                case 2:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        if (asyncResult2.exception == null) {
                            IccPhoneBookInterfaceManager.this.mRecords = (List) asyncResult2.result;
                        } else {
                            IccPhoneBookInterfaceManager.this.logd("Cannot load ADN records");
                            IccPhoneBookInterfaceManager.this.mRecords = null;
                        }
                        notifyPending(asyncResult2);
                        break;
                    }
                    return;
                case 3:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    synchronized (IccPhoneBookInterfaceManager.this.mLock) {
                        IccPhoneBookInterfaceManager.this.mSuccess = asyncResult3.exception == null;
                        notifyPending(asyncResult3);
                        break;
                    }
                    return;
                default:
                    return;
            }
        }

        private void notifyPending(AsyncResult asyncResult) {
            if (asyncResult.userObj != null) {
                ((AtomicBoolean) asyncResult.userObj).set(true);
            }
            IccPhoneBookInterfaceManager.this.mLock.notifyAll();
        }
    };

    public IccPhoneBookInterfaceManager(Phone phone) {
        this.mPhone = phone;
        IccRecords iccRecords = phone.getIccRecords();
        if (iccRecords != null) {
            this.mAdnCache = iccRecords.getAdnCache();
        }
    }

    public void dispose() {
    }

    public void updateIccRecords(IccRecords iccRecords) {
        if (iccRecords != null) {
            this.mAdnCache = iccRecords.getAdnCache();
        } else {
            this.mAdnCache = null;
        }
    }

    protected void logd(String str) {
        Rlog.d(LOG_TAG, "[IccPbInterfaceManager] " + str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[IccPbInterfaceManager] " + str);
    }

    public boolean updateAdnRecordsInEfBySearch(int i, String str, String str2, String str3, String str4, String str5) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        logd("updateAdnRecordsInEfBySearch: efid=0x" + Integer.toHexString(i).toUpperCase() + " (" + Rlog.pii(LOG_TAG, str) + "," + Rlog.pii(LOG_TAG, str2) + ")==> (" + Rlog.pii(LOG_TAG, str3) + "," + Rlog.pii(LOG_TAG, str4) + ") pin2=" + Rlog.pii(LOG_TAG, str5));
        int iUpdateEfForIccType = updateEfForIccType(i);
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = false;
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            Message messageObtainMessage = this.mBaseHandler.obtainMessage(3, atomicBoolean);
            AdnRecord adnRecord = new AdnRecord(str, str2);
            AdnRecord adnRecord2 = new AdnRecord(str3, str4);
            if (this.mAdnCache != null) {
                this.mAdnCache.updateAdnBySearch(iUpdateEfForIccType, adnRecord, adnRecord2, str5, messageObtainMessage);
                waitForResult(atomicBoolean);
            } else {
                loge("Failure while trying to update by search due to uninitialised adncache");
            }
        }
        return this.mSuccess;
    }

    public boolean updateAdnRecordsInEfByIndex(int i, String str, String str2, int i2, String str3) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.WRITE_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.WRITE_CONTACTS permission");
        }
        logd("updateAdnRecordsInEfByIndex: efid=0x" + Integer.toHexString(i).toUpperCase() + " Index=" + i2 + " ==> (" + Rlog.pii(LOG_TAG, str) + "," + Rlog.pii(LOG_TAG, str2) + ") pin2=" + Rlog.pii(LOG_TAG, str3));
        synchronized (this.mLock) {
            checkThread();
            this.mSuccess = false;
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            Message messageObtainMessage = this.mBaseHandler.obtainMessage(3, atomicBoolean);
            AdnRecord adnRecord = new AdnRecord(str, str2);
            if (this.mAdnCache != null) {
                this.mAdnCache.updateAdnByIndex(i, adnRecord, i2, str3, messageObtainMessage);
                waitForResult(atomicBoolean);
            } else {
                loge("Failure while trying to update by index due to uninitialised adncache");
            }
        }
        return this.mSuccess;
    }

    public int[] getAdnRecordsSize(int i) {
        logd("getAdnRecordsSize: efid=" + i);
        synchronized (this.mLock) {
            checkThread();
            this.mRecordSize = new int[3];
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            Message messageObtainMessage = this.mBaseHandler.obtainMessage(1, atomicBoolean);
            IccFileHandler iccFileHandler = this.mPhone.getIccFileHandler();
            if (iccFileHandler != null) {
                iccFileHandler.getEFLinearRecordSize(i, messageObtainMessage);
                waitForResult(atomicBoolean);
            }
        }
        return this.mRecordSize;
    }

    public List<AdnRecord> getAdnRecordsInEf(int i) {
        if (this.mPhone.getContext().checkCallingOrSelfPermission("android.permission.READ_CONTACTS") != 0) {
            throw new SecurityException("Requires android.permission.READ_CONTACTS permission");
        }
        int iUpdateEfForIccType = updateEfForIccType(i);
        logd("getAdnRecordsInEF: efid=0x" + Integer.toHexString(iUpdateEfForIccType).toUpperCase());
        synchronized (this.mLock) {
            checkThread();
            AtomicBoolean atomicBoolean = new AtomicBoolean(false);
            Message messageObtainMessage = this.mBaseHandler.obtainMessage(2, atomicBoolean);
            if (this.mAdnCache != null) {
                this.mAdnCache.requestLoadAllAdnLike(iUpdateEfForIccType, this.mAdnCache.extensionEfForEf(iUpdateEfForIccType), messageObtainMessage);
                waitForResult(atomicBoolean);
            } else {
                loge("Failure while trying to load from SIM due to uninitialised adncache");
            }
        }
        return this.mRecords;
    }

    protected void checkThread() {
        if (this.mBaseHandler.getLooper().equals(Looper.myLooper())) {
            loge("query() called on the main UI thread!");
            throw new IllegalStateException("You cannot call query on this provder from the main UI thread.");
        }
    }

    protected void waitForResult(AtomicBoolean atomicBoolean) {
        while (!atomicBoolean.get()) {
            try {
                this.mLock.wait();
            } catch (InterruptedException e) {
                logd("interrupted while trying to update by search");
            }
        }
    }

    protected int updateEfForIccType(int i) {
        if (i == 28474 && this.mPhone.getCurrentUiccAppType() == IccCardApplicationStatus.AppType.APPTYPE_USIM) {
            return IccConstants.EF_PBR;
        }
        return i;
    }
}
