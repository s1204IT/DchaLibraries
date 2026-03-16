package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.uicc.IccRecords;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

public final class IsimUiccRecords extends IccRecords implements IsimRecords {
    private static final boolean DBG = true;
    private static final boolean DUMP_RECORDS = true;
    private static final int EVENT_AKA_AUTHENTICATE_DONE = 90;
    private static final int EVENT_APP_READY = 1;
    private static final int EVENT_ISIM_REFRESH = 31;
    public static final String INTENT_ISIM_REFRESH = "com.android.intent.isim_refresh";
    protected static final String LOG_TAG = "IsimUiccRecords";
    private static final int TAG_ISIM_VALUE = 128;
    private String auth_rsp;
    private String mIsimDomain;
    private String mIsimImpi;
    private String[] mIsimImpu;
    private String mIsimIst;
    private String[] mIsimPcscf;
    private final Object mLock;

    @Override
    public String toString() {
        return "IsimUiccRecords: " + super.toString() + " mIsimImpi=" + this.mIsimImpi + " mIsimDomain=" + this.mIsimDomain + " mIsimImpu=" + this.mIsimImpu + " mIsimIst=" + this.mIsimIst + " mIsimPcscf=" + this.mIsimPcscf;
    }

    public IsimUiccRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        super(app, c, ci);
        this.mLock = new Object();
        this.mRecordsRequested = false;
        this.mRecordsToLoad = 0;
        resetRecords();
        this.mCi.registerForIccRefresh(this, 31, null);
        this.mParentApp.registerForReady(this, 1, null);
        log("IsimUiccRecords X ctor this=" + this);
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
        if (this.mDestroyed.get()) {
            Rlog.e(LOG_TAG, "Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
            return;
        }
        loge("IsimUiccRecords: handleMessage " + msg + "[" + msg.what + "] ");
        try {
            switch (msg.what) {
                case 1:
                    onReady();
                    return;
                case 31:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    loge("ISim REFRESH(EVENT_ISIM_REFRESH) with exception: " + ar.exception);
                    if (ar.exception == null) {
                        Intent intent = new Intent(INTENT_ISIM_REFRESH);
                        loge("send ISim REFRESH: com.android.intent.isim_refresh");
                        this.mContext.sendBroadcast(intent);
                        handleIsimRefresh((IccRefreshResponse) ar.result);
                        return;
                    }
                    return;
                case EVENT_AKA_AUTHENTICATE_DONE:
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
                        break;
                    }
                    synchronized (this.mLock) {
                        this.mLock.notifyAll();
                        break;
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        } catch (RuntimeException exc) {
            Rlog.w(LOG_TAG, "Exception parsing SIM record", exc);
        }
        Rlog.w(LOG_TAG, "Exception parsing SIM record", exc);
    }

    protected void fetchIsimRecords() {
        this.mRecordsRequested = true;
        this.mFh.loadEFTransparent(IccConstants.EF_IMPI, obtainMessage(100, new EfIsimImpiLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_IMPU, obtainMessage(100, new EfIsimImpuLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_DOMAIN, obtainMessage(100, new EfIsimDomainLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFTransparent(IccConstants.EF_IST, obtainMessage(100, new EfIsimIstLoaded()));
        this.mRecordsToLoad++;
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_PCSCF, obtainMessage(100, new EfIsimPcscfLoaded()));
        this.mRecordsToLoad++;
        log("fetchIsimRecords " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
    }

    protected void resetRecords() {
        this.mIsimImpi = null;
        this.mIsimDomain = null;
        this.mIsimImpu = null;
        this.mIsimIst = null;
        this.mIsimPcscf = null;
        this.auth_rsp = null;
        this.mRecordsRequested = false;
    }

    private class EfIsimImpiLoaded implements IccRecords.IccRecordLoaded {
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
            IsimUiccRecords.this.log("EF_IMPI=" + IsimUiccRecords.this.mIsimImpi);
        }
    }

    private class EfIsimImpuLoaded implements IccRecords.IccRecordLoaded {
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
                IsimUiccRecords.this.log("EF_IMPU[" + i + "]=" + impu);
                IsimUiccRecords.this.mIsimImpu[i] = impu;
                i++;
            }
        }
    }

    private class EfIsimDomainLoaded implements IccRecords.IccRecordLoaded {
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
            IsimUiccRecords.this.log("EF_DOMAIN=" + IsimUiccRecords.this.mIsimDomain);
        }
    }

    private class EfIsimIstLoaded implements IccRecords.IccRecordLoaded {
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
            IsimUiccRecords.this.log("EF_IST=" + IsimUiccRecords.this.mIsimIst);
        }
    }

    private class EfIsimPcscfLoaded implements IccRecords.IccRecordLoaded {
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
                IsimUiccRecords.this.log("EF_PCSCF[" + i + "]=" + pcscf);
                IsimUiccRecords.this.mIsimPcscf[i] = pcscf;
                i++;
            }
        }
    }

    private static String isimTlvToString(byte[] record) {
        SimTlv tlv = new SimTlv(record, 0, record.length);
        while (tlv.getTag() != 128) {
            if (!tlv.nextObject()) {
                Rlog.e(LOG_TAG, "[ISIM] can't find TLV tag in ISIM record, returning null");
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
        } else if (this.mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    @Override
    protected void onAllRecordsLoaded() {
        log("record load complete");
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
    }

    @Override
    protected void handleFileUpdate(int efid) {
        this.mAdnCache.reset();
        switch (efid) {
            case IccConstants.EF_IMPI:
                this.mFh.loadEFTransparent(IccConstants.EF_IMPI, obtainMessage(100, new EfIsimImpiLoaded()));
                this.mRecordsToLoad++;
                return;
            case IccConstants.EF_DOMAIN:
                this.mFh.loadEFTransparent(IccConstants.EF_DOMAIN, obtainMessage(100, new EfIsimDomainLoaded()));
                this.mRecordsToLoad++;
                return;
            case IccConstants.EF_IMPU:
                this.mFh.loadEFLinearFixedAll(IccConstants.EF_IMPU, obtainMessage(100, new EfIsimImpuLoaded()));
                this.mRecordsToLoad++;
                return;
            case IccConstants.EF_IST:
                this.mFh.loadEFTransparent(IccConstants.EF_IST, obtainMessage(100, new EfIsimIstLoaded()));
                this.mRecordsToLoad++;
                return;
            case IccConstants.EF_PCSCF:
                this.mFh.loadEFLinearFixedAll(IccConstants.EF_PCSCF, obtainMessage(100, new EfIsimPcscfLoaded()));
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
                this.mCi.requestIsimAuthentication(nonce, obtainMessage(EVENT_AKA_AUTHENTICATE_DONE));
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
    public int getDisplayRule(String plmn) {
        return 0;
    }

    @Override
    public void onReady() {
        fetchIsimRecords();
    }

    @Override
    public void onRefresh(boolean fileChanged, int[] fileList) {
        if (fileChanged) {
            fetchIsimRecords();
        }
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceNumber, Message onComplete) {
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
    }

    @Override
    protected void log(String s) {
        Rlog.d(LOG_TAG, "[ISIM] " + s);
    }

    @Override
    protected void loge(String s) {
        Rlog.e(LOG_TAG, "[ISIM] " + s);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("IsimRecords: " + this);
        pw.println(" extends:");
        super.dump(fd, pw, args);
        pw.println(" mIsimImpi=" + this.mIsimImpi);
        pw.println(" mIsimDomain=" + this.mIsimDomain);
        pw.println(" mIsimImpu[]=" + Arrays.toString(this.mIsimImpu));
        pw.println(" mIsimIst" + this.mIsimIst);
        pw.println(" mIsimPcscf" + this.mIsimPcscf);
        pw.flush();
    }

    @Override
    public int getVoiceMessageCount() {
        return 0;
    }
}
