package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.gsm.SimTlv;
import com.android.internal.telephony.uicc.IccRecords;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;

public class IsimUiccRecords extends IccRecords implements IsimRecords {
    private static final boolean DBG = true;
    protected static final boolean DUMP_RECORDS = false;
    private static final int EVENT_APP_READY = 1;
    private static final int EVENT_ISIM_AUTHENTICATE_DONE = 91;
    public static final String INTENT_ISIM_REFRESH = "com.android.intent.isim_refresh";
    protected static final String LOG_TAG = "IsimUiccRecords";
    private static final int TAG_ISIM_VALUE = 128;
    private static final boolean VDBG = false;
    private String auth_rsp;
    private String mIsimDomain;
    private String mIsimImpi;
    private String[] mIsimImpu;
    protected String mIsimIst;
    private String[] mIsimPcscf;
    private final Object mLock;

    @Override
    public String toString() {
        return "IsimUiccRecords: " + super.toString() + "";
    }

    public IsimUiccRecords(UiccCardApplication uiccCardApplication, Context context, CommandsInterface commandsInterface) {
        super(uiccCardApplication, context, commandsInterface);
        this.mLock = new Object();
        this.mRecordsRequested = false;
        this.mLockedRecordsReqReason = 0;
        this.mRecordsToLoad = 0;
        resetRecords();
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
    public void handleMessage(Message message) {
        if (this.mDestroyed.get()) {
            Rlog.e(LOG_TAG, "Received message " + message + "[" + message.what + "] while being destroyed. Ignoring.");
            return;
        }
        loge("IsimUiccRecords: handleMessage " + message + "[" + message.what + "] ");
        try {
            int i = message.what;
            if (i == 1) {
                onReady();
                return;
            }
            if (i == 31) {
                broadcastRefresh();
                super.handleMessage(message);
                return;
            }
            if (i == 91) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                log("EVENT_ISIM_AUTHENTICATE_DONE");
                if (asyncResult.exception != null) {
                    log("Exception ISIM AKA: " + asyncResult.exception);
                } else {
                    try {
                        this.auth_rsp = (String) asyncResult.result;
                        log("ISIM AKA: auth_rsp = " + this.auth_rsp);
                    } catch (Exception e) {
                        log("Failed to parse ISIM AKA contents: " + e);
                    }
                }
                synchronized (this.mLock) {
                    this.mLock.notifyAll();
                }
                return;
            }
            super.handleMessage(message);
        } catch (RuntimeException e2) {
            Rlog.w(LOG_TAG, "Exception parsing SIM record", e2);
        }
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
        this.mLockedRecordsReqReason = 0;
        this.mLoaded.set(false);
    }

    public class EfIsimImpiLoaded implements IccRecords.IccRecordLoaded {
        public EfIsimImpiLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_IMPI";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            byte[] bArr = (byte[]) asyncResult.result;
            IsimUiccRecords.this.mIsimImpi = IsimUiccRecords.isimTlvToString(bArr);
        }
    }

    public class EfIsimImpuLoaded implements IccRecords.IccRecordLoaded {
        public EfIsimImpuLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_IMPU";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            ArrayList arrayList = (ArrayList) asyncResult.result;
            IsimUiccRecords.this.log("EF_IMPU record count: " + arrayList.size());
            IsimUiccRecords.this.mIsimImpu = new String[arrayList.size()];
            Iterator it = arrayList.iterator();
            int i = 0;
            while (it.hasNext()) {
                IsimUiccRecords.this.mIsimImpu[i] = IsimUiccRecords.isimTlvToString((byte[]) it.next());
                i++;
            }
        }
    }

    public class EfIsimDomainLoaded implements IccRecords.IccRecordLoaded {
        public EfIsimDomainLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_DOMAIN";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            byte[] bArr = (byte[]) asyncResult.result;
            IsimUiccRecords.this.mIsimDomain = IsimUiccRecords.isimTlvToString(bArr);
        }
    }

    public class EfIsimIstLoaded implements IccRecords.IccRecordLoaded {
        public EfIsimIstLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_IST";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            byte[] bArr = (byte[]) asyncResult.result;
            IsimUiccRecords.this.mIsimIst = IccUtils.bytesToHexString(bArr);
        }
    }

    public class EfIsimPcscfLoaded implements IccRecords.IccRecordLoaded {
        public EfIsimPcscfLoaded() {
        }

        @Override
        public String getEfName() {
            return "EF_ISIM_PCSCF";
        }

        @Override
        public void onRecordLoaded(AsyncResult asyncResult) {
            ArrayList arrayList = (ArrayList) asyncResult.result;
            IsimUiccRecords.this.log("EF_PCSCF record count: " + arrayList.size());
            IsimUiccRecords.this.mIsimPcscf = new String[arrayList.size()];
            Iterator it = arrayList.iterator();
            int i = 0;
            while (it.hasNext()) {
                IsimUiccRecords.this.mIsimPcscf[i] = IsimUiccRecords.isimTlvToString((byte[]) it.next());
                i++;
            }
        }
    }

    private static String isimTlvToString(byte[] bArr) {
        SimTlv simTlv = new SimTlv(bArr, 0, bArr.length);
        while (simTlv.getTag() != 128) {
            if (!simTlv.nextObject()) {
                return null;
            }
        }
        return new String(simTlv.getData(), Charset.forName("UTF-8"));
    }

    @Override
    protected void onRecordLoaded() {
        this.mRecordsToLoad--;
        log("onRecordLoaded " + this.mRecordsToLoad + " requested: " + this.mRecordsRequested);
        if (getRecordsLoaded()) {
            onAllRecordsLoaded();
            return;
        }
        if (getLockedRecordsLoaded() || getNetworkLockedRecordsLoaded()) {
            onLockedAllRecordsLoaded();
        } else if (this.mRecordsToLoad < 0) {
            loge("recordsToLoad <0, programmer error suspected");
            this.mRecordsToLoad = 0;
        }
    }

    private void onLockedAllRecordsLoaded() {
        log("SIM locked; record load complete");
        if (this.mLockedRecordsReqReason == 1) {
            this.mLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            return;
        }
        if (this.mLockedRecordsReqReason == 2) {
            this.mNetworkLockedRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            return;
        }
        loge("onLockedAllRecordsLoaded: unexpected mLockedRecordsReqReason " + this.mLockedRecordsReqReason);
    }

    @Override
    protected void onAllRecordsLoaded() {
        log("record load complete");
        this.mLoaded.set(true);
        this.mRecordsLoadedRegistrants.notifyRegistrants(new AsyncResult((Object) null, (Object) null, (Throwable) null));
    }

    @Override
    protected void handleFileUpdate(int i) {
        if (i == 28423) {
            this.mFh.loadEFTransparent(IccConstants.EF_IST, obtainMessage(100, new EfIsimIstLoaded()));
            this.mRecordsToLoad++;
            return;
        }
        if (i != 28425) {
            switch (i) {
                case IccConstants.EF_IMPI:
                    this.mFh.loadEFTransparent(IccConstants.EF_IMPI, obtainMessage(100, new EfIsimImpiLoaded()));
                    this.mRecordsToLoad++;
                    break;
                case IccConstants.EF_DOMAIN:
                    this.mFh.loadEFTransparent(IccConstants.EF_DOMAIN, obtainMessage(100, new EfIsimDomainLoaded()));
                    this.mRecordsToLoad++;
                    break;
                case IccConstants.EF_IMPU:
                    this.mFh.loadEFLinearFixedAll(IccConstants.EF_IMPU, obtainMessage(100, new EfIsimImpuLoaded()));
                    this.mRecordsToLoad++;
                    break;
            }
            return;
        }
        this.mFh.loadEFLinearFixedAll(IccConstants.EF_PCSCF, obtainMessage(100, new EfIsimPcscfLoaded()));
        this.mRecordsToLoad++;
        fetchIsimRecords();
    }

    private void broadcastRefresh() {
        Intent intent = new Intent(INTENT_ISIM_REFRESH);
        log("send ISim REFRESH: com.android.intent.isim_refresh");
        this.mContext.sendBroadcast(intent);
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
    public int getDisplayRule(ServiceState serviceState) {
        return 0;
    }

    @Override
    public void onReady() {
        fetchIsimRecords();
    }

    @Override
    public void onRefresh(boolean z, int[] iArr) {
        if (z) {
            fetchIsimRecords();
        }
    }

    @Override
    public void setVoiceMailNumber(String str, String str2, Message message) {
    }

    @Override
    public void setVoiceMessageWaiting(int i, int i2) {
    }

    @Override
    protected void log(String str) {
        Rlog.d(LOG_TAG, "[ISIM] " + str);
    }

    @Override
    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[ISIM] " + str);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("IsimRecords: " + this);
        printWriter.println(" extends:");
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.flush();
    }

    @Override
    public int getVoiceMessageCount() {
        return 0;
    }
}
