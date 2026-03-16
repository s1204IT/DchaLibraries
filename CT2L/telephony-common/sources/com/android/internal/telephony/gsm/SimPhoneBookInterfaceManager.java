package com.android.internal.telephony.gsm;

import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.IccPhoneBookInterfaceManager;
import com.android.internal.telephony.uicc.IccFileHandler;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimPhoneBookInterfaceManager extends IccPhoneBookInterfaceManager {
    static final String LOG_TAG = "SimPhoneBookIM";

    public SimPhoneBookInterfaceManager(GSMPhone phone) {
        super(phone);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    protected void finalize() {
        try {
            super.finalize();
        } catch (Throwable throwable) {
            Rlog.e(LOG_TAG, "Error while finalizing:", throwable);
        }
        Rlog.d(LOG_TAG, "SimPhoneBookInterfaceManager finalized");
    }

    @Override
    public int[] getAdnRecordsSize(int efid) {
        logd("getAdnRecordsSize: efid=" + efid);
        synchronized (this.mLock) {
            checkThread();
            this.mRecordSize = new int[3];
            AtomicBoolean status = new AtomicBoolean(false);
            Message response = this.mBaseHandler.obtainMessage(1, status);
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh != null) {
                fh.getEFLinearRecordSize(efid, response);
                waitForResult(status);
            }
        }
        return this.mRecordSize;
    }

    @Override
    public int getTotalAdnRecordsSize() {
        int efid = updateEfForIccType(28474);
        if (efid == 20272) {
            return this.mAdnCache.getTotalAdnRecordsSize(efid);
        }
        synchronized (this.mLock) {
            checkThread();
            this.mRecordSize = new int[3];
            AtomicBoolean status = new AtomicBoolean(false);
            Message response = this.mBaseHandler.obtainMessage(1, status);
            IccFileHandler fh = this.mPhone.getIccFileHandler();
            if (fh != null) {
                fh.getEFLinearRecordSize(efid, response);
                waitForResult(status);
            }
        }
        return this.mRecordSize[2];
    }

    @Override
    protected void logd(String msg) {
        Rlog.d(LOG_TAG, "[SimPbInterfaceManager] " + msg);
    }

    @Override
    protected void loge(String msg) {
        Rlog.e(LOG_TAG, "[SimPbInterfaceManager] " + msg);
    }
}
