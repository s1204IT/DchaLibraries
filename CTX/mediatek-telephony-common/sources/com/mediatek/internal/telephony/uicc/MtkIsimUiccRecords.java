package com.mediatek.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.IsimUiccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.uicc.IsimServiceTable;
import java.util.ArrayList;

public class MtkIsimUiccRecords extends IsimUiccRecords implements MtkIsimRecords {
    private static final int EVENT_GET_GBABP_DONE = 200;
    private static final int EVENT_GET_GBANL_DONE = 201;
    private static final int EVENT_GET_PSISMSC_DONE = 202;
    protected static final String LOG_TAG = "MtkIsimUiccRecords";
    ArrayList<byte[]> mEfGbanlList;
    byte[] mEfPsismsc;
    private int mIsimChannel;
    private String mIsimGbabp;
    IsimServiceTable mIsimServiceTable;
    private int mSlotId;
    protected UiccController mUiccController;

    static int access$308(MtkIsimUiccRecords mtkIsimUiccRecords) {
        int i = mtkIsimUiccRecords.mRecordsToLoad;
        mtkIsimUiccRecords.mRecordsToLoad = i + 1;
        return i;
    }

    static int access$508(MtkIsimUiccRecords mtkIsimUiccRecords) {
        int i = mtkIsimUiccRecords.mRecordsToLoad;
        mtkIsimUiccRecords.mRecordsToLoad = i + 1;
        return i;
    }

    public MtkIsimUiccRecords(UiccCardApplication uiccCardApplication, Context context, CommandsInterface commandsInterface) {
        super(uiccCardApplication, context, commandsInterface);
        this.mEfPsismsc = null;
        log("MtkIsimUiccRecords X ctor this=" + this);
        this.mSlotId = ((MtkUiccCardApplication) uiccCardApplication).getPhoneId();
        this.mUiccController = UiccController.getInstance();
    }

    public void handleMessage(Message message) throws Throwable {
        if (this.mDestroyed.get()) {
            Rlog.e(LOG_TAG, "Received message " + message + "[" + message.what + "] while being destroyed. Ignoring.");
            return;
        }
        loge("IsimUiccRecords: handleMessage " + message + "[" + message.what + "] ");
        boolean z = true;
        try {
            try {
                try {
                } catch (RuntimeException e) {
                    e = e;
                    Rlog.w(LOG_TAG, "Exception parsing SIM record", e);
                    if (!z) {
                        return;
                    }
                }
            } catch (Throwable th) {
                th = th;
                if (z) {
                    onRecordLoaded();
                }
                throw th;
            }
        } catch (RuntimeException e2) {
            e = e2;
            z = false;
        } catch (Throwable th2) {
            th = th2;
            z = false;
            if (z) {
            }
            throw th;
        }
        switch (message.what) {
            case EVENT_GET_GBABP_DONE:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception == null) {
                    this.mIsimGbabp = IccUtils.bytesToHexString((byte[]) asyncResult.result);
                } else {
                    loge("Error on GET_ISIM_GBABP with exp " + asyncResult.exception);
                }
                if (!z) {
                    return;
                }
                onRecordLoaded();
                return;
            case 201:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception == null) {
                    this.mEfGbanlList = (ArrayList) asyncResult2.result;
                    log("GET_ISIM_GBANL record count: " + this.mEfGbanlList.size());
                } else {
                    loge("Error on GET_ISIM_GBANL with exp " + asyncResult2.exception);
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            case 202:
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                byte[] bArr = (byte[]) asyncResult3.result;
                if (asyncResult3.exception == null) {
                    log("EF_PSISMSC: " + IccUtils.bytesToHexString(bArr));
                    if (bArr != null) {
                        this.mEfPsismsc = bArr;
                    }
                }
                if (!z) {
                }
                onRecordLoaded();
                return;
            default:
                super.handleMessage(message);
                z = false;
                if (!z) {
                }
                onRecordLoaded();
                return;
        }
    }

    protected void fetchIsimRecords() {
        this.mRecordsRequested = true;
        this.mFh.loadEFTransparent(28418, obtainMessage(100, new IsimUiccRecords.EfIsimImpiLoaded(this)));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(28420, obtainMessage(100, new IsimUiccRecords.EfIsimImpuLoaded(this)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(28419, obtainMessage(100, new IsimUiccRecords.EfIsimDomainLoaded(this)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(MtkIccConstants.EF_IMSI, obtainMessage(100, new MtkEfIsimIstLoaded()));
        this.mRecordsToLoad++;
        log("fetchIsimRecords " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
    }

    protected void fetchGbaParam() {
        if (this.mIsimServiceTable.isAvailable(IsimServiceTable.IsimService.GBA)) {
            this.mFh.loadEFTransparent(MtkIccConstants.EF_ISIM_GBABP, obtainMessage(EVENT_GET_GBABP_DONE));
            this.mRecordsToLoad++;
            this.mFh.loadEFLinearFixedAll(MtkIccConstants.EF_ISIM_GBANL, obtainMessage(201));
            this.mRecordsToLoad++;
        }
    }

    private class MtkEfIsimIstLoaded implements IccRecords.IccRecordLoaded {
        private MtkEfIsimIstLoaded() {
        }

        public String getEfName() {
            return "EF_ISIM_IST";
        }

        public void onRecordLoaded(AsyncResult asyncResult) {
            byte[] bArr = (byte[]) asyncResult.result;
            MtkIsimUiccRecords.this.mIsimIst = IccUtils.bytesToHexString(bArr);
            MtkIsimUiccRecords.this.mIsimServiceTable = new IsimServiceTable(bArr);
            MtkIsimUiccRecords.this.log("IST: " + MtkIsimUiccRecords.this.mIsimServiceTable);
            if (MtkIsimUiccRecords.this.mIsimServiceTable.isAvailable(IsimServiceTable.IsimService.PCSCF_ADDRESS) || MtkIsimUiccRecords.this.mIsimServiceTable.isAvailable(IsimServiceTable.IsimService.PCSCF_DISCOVERY)) {
                MtkIsimUiccRecords.this.mFh.loadEFLinearFixedAll(28425, MtkIsimUiccRecords.this.obtainMessage(100, new IsimUiccRecords.EfIsimPcscfLoaded(MtkIsimUiccRecords.this)));
                MtkIsimUiccRecords.access$308(MtkIsimUiccRecords.this);
            }
            if (MtkIsimUiccRecords.this.mIsimServiceTable.isAvailable(IsimServiceTable.IsimService.SM_OVER_IP)) {
                MtkIsimUiccRecords.this.mFh.loadEFLinearFixed(MtkIccConstants.EF_PSISMSC, 1, MtkIsimUiccRecords.this.obtainMessage(202));
                MtkIsimUiccRecords.access$508(MtkIsimUiccRecords.this);
            }
            MtkIsimUiccRecords.this.fetchGbaParam();
        }
    }

    public void registerForRecordsLoaded(Handler handler, int i, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant registrant = new Registrant(handler, i, obj);
        this.mRecordsLoadedRegistrants.add(registrant);
        if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public void unregisterForRecordsLoaded(Handler handler) {
        this.mRecordsLoadedRegistrants.remove(handler);
    }

    @Override
    public byte[] getEfPsismsc() {
        log("PSISMSC = " + IccUtils.bytesToHexString(this.mEfPsismsc));
        return this.mEfPsismsc;
    }

    @Override
    public String getIsimGbabp() {
        log("ISIM GBABP = " + this.mIsimGbabp);
        return this.mIsimGbabp;
    }

    @Override
    public void setIsimGbabp(String str, Message message) {
        this.mFh.updateEFTransparent(MtkIccConstants.EF_ISIM_GBABP, IccUtils.hexStringToBytes(str), message);
    }

    public String getIsimChallengeResponse(String str) {
        return "";
    }

    protected void log(String str) {
        Rlog.d(LOG_TAG, "[ISIM] " + str + " (slot " + this.mSlotId + ")");
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[ISIM] " + str + " (slot " + this.mSlotId + ")");
    }
}
