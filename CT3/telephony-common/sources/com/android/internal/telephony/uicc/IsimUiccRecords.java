package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.uicc.IccRecords;
import com.mediatek.internal.telephony.uicc.IsimServiceTable;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class IsimUiccRecords extends IccRecords implements IsimRecords {
    private static final boolean DBG = true;
    private static final boolean DUMP_RECORDS = false;
    private static final int EVENT_AKA_AUTHENTICATE_DONE = 91;
    private static final int EVENT_APP_READY = 1;
    private static final int EVENT_GET_GBABP_DONE = 200;
    private static final int EVENT_GET_GBANL_DONE = 201;
    private static final int EVENT_GET_PSISMSC_DONE = 202;
    private static final int EVENT_ISIM_REFRESH = 31;
    public static final String INTENT_ISIM_REFRESH = "com.android.intent.isim_refresh";
    protected static final String LOG_TAG = "IsimUiccRecords";
    private static final int TAG_ISIM_VALUE = 128;
    private static final boolean VDBG = false;
    private String auth_rsp;
    ArrayList<byte[]> mEfGbanlList;
    byte[] mEfPsismsc;
    private int mIsimChannel;
    private String mIsimDomain;
    private String mIsimGbabp;
    private String[] mIsimGbanl;
    private String mIsimImpi;
    private String[] mIsimImpu;
    private String mIsimIst;
    private String[] mIsimPcscf;
    IsimServiceTable mIsimServiceTable;
    private final Object mLock;
    private int mSlotId;
    protected UiccController mUiccController;

    @Override
    public String toString() {
        return "IsimUiccRecords: " + super.toString() + UsimPBMemInfo.STRING_NOT_SET;
    }

    public IsimUiccRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);
        this.mEfPsismsc = null;
        this.mLock = new Object();
        this.mRecordsRequested = DUMP_RECORDS;
        this.mRecordsToLoad = 0;
        resetRecords();
        this.mCi.registerForIccRefresh(this, 31, null);
        this.mParentApp.registerForReady(this, 1, null);
        log("IsimUiccRecords X ctor this=" + this);
        this.mSlotId = app.getSlotId();
        this.mUiccController = UiccController.getInstance();
    }

    @Override
    public void dispose() {
        log("Disposing " + this);
        this.mCi.unregisterForIccRefresh(this);
        this.mParentApp.unregisterForReady(this);
        resetRecords();
        super.dispose();
    }

    @Override
    public void handleMessage(Message msg) {
        boolean isRecordLoadResponse = DUMP_RECORDS;
        if (this.mDestroyed.get()) {
            Rlog.e(LOG_TAG, "Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
            return;
        }
        loge("IsimUiccRecords: handleMessage " + msg + "[" + msg.what + "] ");
        try {
            try {
                switch (msg.what) {
                    case 1:
                        onReady();
                        break;
                    case 31:
                        AsyncResult ar = (AsyncResult) msg.obj;
                        loge("ISim REFRESH(EVENT_ISIM_REFRESH) with exception: " + ar.exception);
                        if (ar.exception == null) {
                            Intent intent = new Intent(INTENT_ISIM_REFRESH);
                            loge("send ISim REFRESH: com.android.intent.isim_refresh");
                            this.mContext.sendBroadcast(intent);
                            handleIsimRefresh((IccRefreshResponse) ar.result);
                        }
                        break;
                    case 91:
                        AsyncResult ar2 = (AsyncResult) msg.obj;
                        log("EVENT_AKA_AUTHENTICATE_DONE");
                        if (ar2.exception != null) {
                            log("Exception ISIM AKA: " + ar2.exception);
                        } else {
                            try {
                                this.auth_rsp = (String) ar2.result;
                                log("ISIM AKA: auth_rsp = " + this.auth_rsp);
                            } catch (Exception e) {
                                log("Failed to parse ISIM AKA contents: " + e);
                            }
                        }
                        synchronized (this.mLock) {
                            this.mLock.notifyAll();
                            break;
                        }
                        break;
                    case 200:
                        isRecordLoadResponse = true;
                        AsyncResult ar3 = (AsyncResult) msg.obj;
                        if (ar3.exception != null) {
                            loge("Error on GET_ISIM_GBABP with exp " + ar3.exception);
                        } else {
                            this.mIsimGbabp = IccUtils.bytesToHexString((byte[]) ar3.result);
                        }
                        break;
                    case 201:
                        isRecordLoadResponse = true;
                        AsyncResult ar4 = (AsyncResult) msg.obj;
                        if (ar4.exception != null) {
                            loge("Error on GET_ISIM_GBANL with exp " + ar4.exception);
                        } else {
                            this.mEfGbanlList = (ArrayList) ar4.result;
                            log("GET_ISIM_GBANL record count: " + this.mEfGbanlList.size());
                        }
                        break;
                    case EVENT_GET_PSISMSC_DONE:
                        isRecordLoadResponse = true;
                        AsyncResult ar5 = (AsyncResult) msg.obj;
                        byte[] data = (byte[]) ar5.result;
                        if (ar5.exception == null) {
                            log("EF_PSISMSC: " + IccUtils.bytesToHexString(data));
                            if (data != null) {
                                this.mEfPsismsc = data;
                            }
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
                if (isRecordLoadResponse) {
                    onRecordLoaded();
                }
            } catch (RuntimeException exc) {
                Rlog.w(LOG_TAG, "Exception parsing SIM record", exc);
                if (0 != 0) {
                    onRecordLoaded();
                }
            }
        } catch (Throwable th) {
            if (0 != 0) {
                onRecordLoaded();
            }
            throw th;
        }
    }

    protected void fetchIsimRecords() {
        this.mRecordsRequested = true;
        this.mFh.loadEFTransparent(IccConstants.EF_IMPI, obtainMessage(100, new EfIsimImpiLoaded(this, null)));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_IMPU, obtainMessage(100, new EfIsimImpuLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_DOMAIN, obtainMessage(100, new EfIsimDomainLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(28423, obtainMessage(100, new EfIsimIstLoaded(this, 0 == true ? 1 : 0)));
        this.mRecordsToLoad++;
        log("fetchIsimRecords " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
    }

    private void fetchGbaParam() {
        if (!this.mIsimServiceTable.isAvailable(IsimServiceTable.IsimService.GBA)) {
            return;
        }
        this.mFh.loadEFTransparent(IccConstants.EF_ISIM_GBABP, obtainMessage(200));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_ISIM_GBANL, obtainMessage(201));
        this.mRecordsToLoad++;
    }

    protected void resetRecords() {
        this.mIsimImpi = null;
        this.mIsimDomain = null;
        this.mIsimImpu = null;
        this.mIsimIst = null;
        this.mIsimPcscf = null;
        this.auth_rsp = null;
        this.mRecordsRequested = DUMP_RECORDS;
    }

    private class EfIsimImpiLoaded implements IccRecords.IccRecordLoaded {
        EfIsimImpiLoaded(IsimUiccRecords this$0, EfIsimImpiLoaded efIsimImpiLoaded) {
            this();
        }

        private EfIsimImpiLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_IMPI";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            IsimUiccRecords.this.mIsimImpi = IsimUiccRecords.isimTlvToString(data);
        }
    }

    private class EfIsimImpuLoaded implements IccRecords.IccRecordLoaded {
        EfIsimImpuLoaded(IsimUiccRecords this$0, EfIsimImpuLoaded efIsimImpuLoaded) {
            this();
        }

        private EfIsimImpuLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_IMPU";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            ArrayList<byte[]> impuList = (ArrayList) ar.result;
            IsimUiccRecords.this.log("EF_IMPU record count: " + impuList.size());
            IsimUiccRecords.this.mIsimImpu = new String[impuList.size()];
            int i = 0;
            for (byte[] identity : impuList) {
                String impu = IsimUiccRecords.isimTlvToString(identity);
                IsimUiccRecords.this.mIsimImpu[i] = impu;
                i++;
            }
        }
    }

    private class EfIsimDomainLoaded implements IccRecords.IccRecordLoaded {
        EfIsimDomainLoaded(IsimUiccRecords this$0, EfIsimDomainLoaded efIsimDomainLoaded) {
            this();
        }

        private EfIsimDomainLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_DOMAIN";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            IsimUiccRecords.this.mIsimDomain = IsimUiccRecords.isimTlvToString(data);
        }
    }

    private class EfIsimIstLoaded implements IccRecords.IccRecordLoaded {
        EfIsimIstLoaded(IsimUiccRecords this$0, EfIsimIstLoaded efIsimIstLoaded) {
            this();
        }

        private EfIsimIstLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_IST";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            byte[] data = (byte[]) ar.result;
            IsimUiccRecords.this.mIsimIst = IccUtils.bytesToHexString(data);
            IsimUiccRecords.this.mIsimServiceTable = new IsimServiceTable(data);
            IsimUiccRecords.this.log("IST: " + IsimUiccRecords.this.mIsimServiceTable);
            if (IsimUiccRecords.this.mIsimServiceTable.isAvailable(IsimServiceTable.IsimService.PCSCF_ADDRESS) || IsimUiccRecords.this.mIsimServiceTable.isAvailable(IsimServiceTable.IsimService.PCSCF_DISCOVERY)) {
                IsimUiccRecords.this.mFh.loadEFLinearFixedAll(IccConstants.EF_PCSCF, IsimUiccRecords.this.obtainMessage(100, new EfIsimPcscfLoaded(IsimUiccRecords.this, null)));
                IsimUiccRecords.this.mRecordsToLoad++;
            }
            if (IsimUiccRecords.this.mIsimServiceTable.isAvailable(IsimServiceTable.IsimService.SM_OVER_IP)) {
                IsimUiccRecords.this.mFh.loadEFLinearFixed(28645, 1, IsimUiccRecords.this.obtainMessage(IsimUiccRecords.EVENT_GET_PSISMSC_DONE));
                IsimUiccRecords.this.mRecordsToLoad++;
            }
            IsimUiccRecords.this.fetchGbaParam();
        }
    }

    private class EfIsimPcscfLoaded implements IccRecords.IccRecordLoaded {
        EfIsimPcscfLoaded(IsimUiccRecords this$0, EfIsimPcscfLoaded efIsimPcscfLoaded) {
            this();
        }

        private EfIsimPcscfLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_PCSCF";
        }

        @Override
        public void onRecordLoaded(AsyncResult ar) {
            ArrayList<byte[]> pcscflist = (ArrayList) ar.result;
            IsimUiccRecords.this.log("EF_PCSCF record count: " + pcscflist.size());
            IsimUiccRecords.this.mIsimPcscf = new String[pcscflist.size()];
            int i = 0;
            for (byte[] identity : pcscflist) {
                String pcscf = IsimUiccRecords.isimTlvToString(identity);
                IsimUiccRecords.this.mIsimPcscf[i] = pcscf;
                i++;
            }
        }
    }

    private static String isimTlvToString(byte[] record) {
        SimTlv tlv = new SimTlv(record, 0, record.length);
        while (tlv.getTag() != 128) {
            if (!tlv.nextObject()) {
                return null;
            }
        }
        return new String(tlv.getData(), Charset.forName("UTF-8"));
    }

    @Override
    protected void onRecordLoaded() {
        this.mRecordsToLoad--;
        log("onRecordLoaded " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
        if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
            onAllRecordsLoaded();
        } else {
            if (this.mRecordsToLoad >= 0) {
                return;
            }
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    @Override
    protected void onAllRecordsLoaded() {
        log("record load complete");
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
    }

    private void handleFileUpdate(int i) {
        EfIsimImpiLoaded efIsimImpiLoaded = null;
        Object[] objArr = 0;
        Object[] objArr2 = 0;
        Object[] objArr3 = 0;
        Object[] objArr4 = 0;
        switch (i) {
            case IccConstants.EF_IMPI:
                this.mFh.loadEFTransparent(IccConstants.EF_IMPI, obtainMessage(100, new EfIsimImpiLoaded(this, efIsimImpiLoaded)));
                this.mRecordsToLoad++;
                return;
            case IccConstants.EF_DOMAIN:
                this.mFh.loadEFTransparent(IccConstants.EF_DOMAIN, obtainMessage(100, new EfIsimDomainLoaded(this, objArr3 == true ? 1 : 0)));
                this.mRecordsToLoad++;
                return;
            case IccConstants.EF_IMPU:
                this.mFh.loadEFLinearFixedAll(IccConstants.EF_IMPU, obtainMessage(100, new EfIsimImpuLoaded(this, objArr4 == true ? 1 : 0)));
                this.mRecordsToLoad++;
                return;
            case 28423:
                this.mFh.loadEFTransparent(28423, obtainMessage(100, new EfIsimIstLoaded(this, objArr2 == true ? 1 : 0)));
                this.mRecordsToLoad++;
                return;
            case IccConstants.EF_PCSCF:
                this.mFh.loadEFLinearFixedAll(IccConstants.EF_PCSCF, obtainMessage(100, new EfIsimPcscfLoaded(this, objArr == true ? 1 : 0)));
                this.mRecordsToLoad++;
                break;
        }
        fetchIsimRecords();
    }

    private void handleIsimRefresh(IccRefreshResponse refreshResponse) {
        if (refreshResponse == null) {
            log("handleIsimRefresh received without input");
        }
        if (refreshResponse.aid != null && !refreshResponse.aid.equals(this.mParentApp.getAid())) {
            log("handleIsimRefresh received different app");
            return;
        }
        switch (refreshResponse.refreshResult) {
            case 0:
                log("handleIsimRefresh with REFRESH_RESULT_FILE_UPDATE");
                handleFileUpdate(refreshResponse.efId);
                break;
            case 1:
                log("handleIsimRefresh with REFRESH_RESULT_INIT");
                fetchIsimRecords();
                break;
            case 2:
                log("handleIsimRefresh with REFRESH_RESULT_RESET");
                break;
            default:
                log("handleIsimRefresh with unknown operation");
                break;
        }
    }

    @Override
    public void registerForRecordsLoaded(Handler h, int what, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant r = new Registrant(h, what, obj);
        this.mRecordsLoadedRegistrants.add(r);
        if (this.mRecordsToLoad != 0 || !this.mRecordsRequested) {
            return;
        }
        r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
    }

    @Override
    public void unregisterForRecordsLoaded(Handler h) {
        this.mRecordsLoadedRegistrants.remove(h);
    }

    @Override
    public String getIsimImpi() {
        return this.mIsimImpi;
    }

    @Override
    public String getIsimDomain() {
        return this.mIsimDomain;
    }

    @Override
    public String[] getIsimImpu() {
        if (this.mIsimImpu != null) {
            return (String[]) this.mIsimImpu.clone();
        }
        return null;
    }

    @Override
    public String getIsimIst() {
        return this.mIsimIst;
    }

    @Override
    public String[] getIsimPcscf() {
        if (this.mIsimPcscf != null) {
            return (String[]) this.mIsimPcscf.clone();
        }
        return null;
    }

    @Override
    public String getIsimChallengeResponse(String nonce) {
        log("getIsimChallengeResponse-nonce:" + nonce);
        try {
            synchronized (this.mLock) {
                this.mCi.requestIsimAuthentication(nonce, obtainMessage(91));
                try {
                    this.mLock.wait();
                } catch (InterruptedException e) {
                    log("interrupted while trying to request Isim Auth");
                }
            }
            log("getIsimChallengeResponse-auth_rsp" + this.auth_rsp);
            return this.auth_rsp;
        } catch (Exception e2) {
            log("Fail while trying to request Isim Auth");
            return null;
        }
    }

    @Override
    public byte[] getEfPsismsc() {
        log("PSISMSC = " + this.mEfPsismsc);
        return this.mEfPsismsc;
    }

    @Override
    public String getIsimGbabp() {
        log("ISIM GBABP = " + this.mIsimGbabp);
        return this.mIsimGbabp;
    }

    @Override
    public void setIsimGbabp(String gbabp, Message onComplete) {
        byte[] data = IccUtils.hexStringToBytes(gbabp);
        this.mFh.updateEFTransparent(IccConstants.EF_ISIM_GBABP, data, onComplete);
    }

    @Override
    public int getDisplayRule(String plmn) {
        return 0;
    }

    @Override
    public void onReady() {
        fetchIsimRecords();
    }

    @Override
    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (!fileChanged) {
            return;
        }
        fetchIsimRecords();
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceNumber, Message onComplete) {
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[ISIM] " + s + " (slot " + this.mSlotId + ")");
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[ISIM] " + s + " (slot " + this.mSlotId + ")");
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("IsimRecords: " + this);
        pw.println(" extends:");
        super.dump(fd, pw, args);
        pw.flush();
    }

    @Override
    public int getVoiceMessageCount() {
        return 0;
    }

    @Override
    protected int getChildPhoneId() {
        log("[getChildPhoneId] return -1 for iSimUiccRecords");
        return -1;
    }

    @Override
    protected void updatePHBStatus(int status, boolean isSimLocked) {
    }
}
