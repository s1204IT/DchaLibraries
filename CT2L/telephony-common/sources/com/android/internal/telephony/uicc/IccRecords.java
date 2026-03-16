package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IccRecords extends Handler implements IccConstants {
    protected static final boolean DBG = true;
    private static final int EVENT_AKA_AUTHENTICATE_DONE = 90;
    protected static final int EVENT_APP_READY = 1;
    public static final int EVENT_CFI = 1;
    public static final int EVENT_GET_ICC_RECORD_DONE = 100;
    public static final int EVENT_MWI = 0;
    public static final int EVENT_REFRESH = 31;
    protected static final int EVENT_SET_MSISDN_DONE = 30;
    public static final int EVENT_SPN = 2;
    public static final int SPN_RULE_SHOW_PLMN = 2;
    public static final int SPN_RULE_SHOW_SPN = 1;
    protected static final int UNINITIALIZED = -1;
    protected static final int UNKNOWN = 0;
    private IccIoResult auth_rsp;
    protected AdnRecordCache mAdnCache;
    protected CommandsInterface mCi;
    protected Context mContext;
    protected IccFileHandler mFh;
    protected String mGid1;
    protected String mIccId;
    protected UiccCardApplication mParentApp;
    protected int mRecordsToLoad;
    private String mSpn;
    protected TelephonyManager mTelephonyManager;
    protected AtomicBoolean mDestroyed = new AtomicBoolean(false);
    protected RegistrantList mRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mImsiReadyRegistrants = new RegistrantList();
    protected RegistrantList mRecordsEventsRegistrants = new RegistrantList();
    protected RegistrantList mNewSmsRegistrants = new RegistrantList();
    protected RegistrantList mNetworkSelectionModeAutomaticRegistrants = new RegistrantList();
    protected boolean mRecordsRequested = false;
    protected String mMsisdn = null;
    protected String mMsisdnTag = null;
    protected String mNewMsisdn = null;
    protected String mNewMsisdnTag = null;
    protected String mVoiceMailNum = null;
    protected String mVoiceMailTag = null;
    protected String mNewVoiceMailNum = null;
    protected String mNewVoiceMailTag = null;
    protected boolean mIsVoiceMailFixed = false;
    protected String mImsi = null;
    protected boolean mIsIccActivation = false;
    protected boolean mImsiChanged = false;
    protected int mMncLength = -1;
    protected int mMailboxIndex = 0;
    private final Object mLock = new Object();

    public interface IccRecordLoaded {
        String getEfName();

        void onRecordLoaded(AsyncResult asyncResult);
    }

    public abstract int getDisplayRule(String str);

    public abstract int getVoiceMessageCount();

    protected abstract void handleFileUpdate(int i);

    protected abstract void log(String str);

    protected abstract void loge(String str);

    protected abstract void onAllRecordsLoaded();

    public abstract void onReady();

    protected abstract void onRecordLoaded();

    public abstract void onRefresh(boolean z, int[] iArr);

    public abstract void setVoiceMailNumber(String str, String str2, Message message);

    public abstract void setVoiceMessageWaiting(int i, int i2);

    @Override
    public String toString() {
        return "mDestroyed=" + this.mDestroyed + " mContext=" + this.mContext + " mCi=" + this.mCi + " mFh=" + this.mFh + " mParentApp=" + this.mParentApp + " recordsLoadedRegistrants=" + this.mRecordsLoadedRegistrants + " mImsiReadyRegistrants=" + this.mImsiReadyRegistrants + " mRecordsEventsRegistrants=" + this.mRecordsEventsRegistrants + " mNewSmsRegistrants=" + this.mNewSmsRegistrants + " mNetworkSelectionModeAutomaticRegistrants=" + this.mNetworkSelectionModeAutomaticRegistrants + " recordsToLoad=" + this.mRecordsToLoad + " adnCache=" + this.mAdnCache + " recordsRequested=" + this.mRecordsRequested + " iccid=" + this.mIccId + " msisdnTag=" + this.mMsisdnTag + " voiceMailNum=" + this.mVoiceMailNum + " voiceMailTag=" + this.mVoiceMailTag + " newVoiceMailNum=" + this.mNewVoiceMailNum + " newVoiceMailTag=" + this.mNewVoiceMailTag + " isVoiceMailFixed=" + this.mIsVoiceMailFixed + " mImsi=" + this.mImsi + " mncLength=" + this.mMncLength + " mailboxIndex=" + this.mMailboxIndex + " spn=" + this.mSpn + " isIccActivation=" + this.mIsIccActivation;
    }

    public IccRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        this.mContext = c;
        this.mCi = ci;
        this.mFh = app.getIccFileHandler();
        this.mParentApp = app;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mCi.registerForIccRefresh(this, 31, null);
    }

    public void dispose() {
        this.mDestroyed.set(true);
        this.mCi.unregisterForIccRefresh(this);
        this.mParentApp = null;
        this.mFh = null;
        this.mCi = null;
        this.mContext = null;
    }

    public AdnRecordCache getAdnCache() {
        return this.mAdnCache;
    }

    public String getIccId() {
        return this.mIccId;
    }

    public void registerForRecordsLoaded(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mRecordsLoadedRegistrants.add(r);
            if (this.mRecordsToLoad == 0 && this.mRecordsRequested) {
                r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    public void unregisterForRecordsLoaded(Handler h) {
        this.mRecordsLoadedRegistrants.remove(h);
    }

    public void registerForImsiReady(Handler h, int what, Object obj) {
        if (!this.mDestroyed.get()) {
            Registrant r = new Registrant(h, what, obj);
            this.mImsiReadyRegistrants.add(r);
            if (this.mImsi != null) {
                r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
            }
        }
    }

    public void unregisterForImsiReady(Handler h) {
        this.mImsiReadyRegistrants.remove(h);
    }

    public void registerForRecordsEvents(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mRecordsEventsRegistrants.add(r);
        r.notifyResult(0);
        r.notifyResult(1);
    }

    public void unregisterForRecordsEvents(Handler h) {
        this.mRecordsEventsRegistrants.remove(h);
    }

    public void registerForNewSms(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mNewSmsRegistrants.add(r);
    }

    public void unregisterForNewSms(Handler h) {
        this.mNewSmsRegistrants.remove(h);
    }

    public void registerForNetworkSelectionModeAutomatic(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mNetworkSelectionModeAutomaticRegistrants.add(r);
    }

    public void unregisterForNetworkSelectionModeAutomatic(Handler h) {
        this.mNetworkSelectionModeAutomaticRegistrants.remove(h);
    }

    public String getIMSI() {
        return this.mImsi;
    }

    public boolean getImsiChanged() {
        return this.mImsiChanged;
    }

    public void setImsi(String imsi) {
        this.mImsi = imsi;
        this.mImsiReadyRegistrants.notifyRegistrants();
    }

    public String getNAI() {
        return null;
    }

    public String getMsisdnNumber() {
        return this.mMsisdn;
    }

    public String getGid1() {
        return null;
    }

    public void setMsisdnNumber(String alphaTag, String number, Message onComplete) {
        this.mMsisdn = number;
        this.mMsisdnTag = alphaTag;
        log("Set MSISDN: " + this.mMsisdnTag + " " + this.mMsisdn);
        AdnRecord adn = new AdnRecord(this.mMsisdnTag, this.mMsisdn);
        new AdnRecordLoader(this.mFh).updateEF(adn, IccConstants.EF_MSISDN, IccConstants.EF_EXT1, 1, null, obtainMessage(30, onComplete));
    }

    public String getMsisdnAlphaTag() {
        return this.mMsisdnTag;
    }

    public String getVoiceMailNumber() {
        return this.mVoiceMailNum;
    }

    public String getServiceProviderName() {
        String providerName = this.mSpn;
        UiccCardApplication parentApp = this.mParentApp;
        if (parentApp != null) {
            UiccCard card = parentApp.getUiccCard();
            if (card != null) {
                String brandOverride = card.getOperatorBrandOverride();
                if (brandOverride != null) {
                    log("getServiceProviderName: override");
                    providerName = brandOverride;
                } else {
                    log("getServiceProviderName: no brandOverride");
                }
            } else {
                log("getServiceProviderName: card is null");
            }
        } else {
            log("getServiceProviderName: mParentApp is null");
        }
        log("getServiceProviderName: providerName=" + providerName);
        return providerName;
    }

    protected void setServiceProviderName(String spn) {
        this.mSpn = spn;
    }

    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    protected void onIccRefreshInit() {
        if (this.mAdnCache != null) {
            this.mAdnCache.reset();
        }
        UiccCardApplication parentApp = this.mParentApp;
        if (parentApp != null && parentApp.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY) {
            sendMessage(obtainMessage(1));
        }
    }

    public boolean getRecordsLoaded() {
        return this.mRecordsToLoad == 0 && this.mRecordsRequested;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case 31:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    log("Card REFRESH occurred: ");
                    if (ar.exception == null) {
                        handleRefresh((IccRefreshResponse) ar.result);
                        return;
                    } else {
                        loge("Icc refresh Exception: " + ar.exception);
                        return;
                    }
                case EVENT_AKA_AUTHENTICATE_DONE:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    this.auth_rsp = null;
                    log("EVENT_AKA_AUTHENTICATE_DONE");
                    if (ar2.exception != null) {
                        loge("Exception ICC SIM AKA: " + ar2.exception);
                    } else {
                        try {
                            this.auth_rsp = (IccIoResult) ar2.result;
                            log("ICC SIM AKA: auth_rsp = " + this.auth_rsp);
                        } catch (Exception e) {
                            loge("Failed to parse ICC SIM AKA contents: " + e);
                        }
                        break;
                    }
                    synchronized (this.mLock) {
                        this.mLock.notifyAll();
                        break;
                    }
                    return;
                case 100:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    IccRecordLoaded recordLoaded = (IccRecordLoaded) ar3.userObj;
                    log(recordLoaded.getEfName() + " LOADED");
                    if (ar3.exception != null) {
                        loge("Record Load Exception: " + ar3.exception);
                    } else {
                        recordLoaded.onRecordLoaded(ar3);
                    }
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        } catch (RuntimeException exc) {
            loge("Exception parsing SIM record: " + exc);
            return;
        } finally {
            onRecordLoaded();
        }
        onRecordLoaded();
    }

    private void handleRefresh(IccRefreshResponse refreshResponse) {
        if (refreshResponse == null) {
            log("handleSimRefresh received without input");
        }
        if (refreshResponse.aid == null || TextUtils.isEmpty(this.mParentApp.getAid()) || refreshResponse.aid.equals(this.mParentApp.getAid())) {
            if (this.mMsisdn == null) {
                log("ICC Activation happen");
                this.mIsIccActivation = true;
            }
            switch (refreshResponse.refreshResult) {
                case 0:
                    log("handleSimRefresh with SIM_FILE_UPDATED");
                    handleFileUpdate(refreshResponse.efId);
                    break;
                case 1:
                    log("handleSimRefresh with SIM_REFRESH_INIT");
                    onIccRefreshInit();
                    break;
                case 2:
                    log("handleSimRefresh with SIM_REFRESH_RESET");
                    break;
                default:
                    log("handleSimRefresh with unknown operation");
                    break;
            }
        }
    }

    public boolean isCspPlmnEnabled() {
        return false;
    }

    public String getOperatorNumeric() {
        return null;
    }

    public boolean getVoiceCallForwardingFlag() {
        return false;
    }

    public void setVoiceCallForwardingFlag(int line, boolean enable, String number) {
    }

    public boolean isProvisioned() {
        return true;
    }

    public IsimRecords getIsimRecords() {
        return null;
    }

    public UsimServiceTable getUsimServiceTable() {
        return null;
    }

    protected void setSystemProperty(String key, String val) {
        TelephonyManager.getDefault();
        TelephonyManager.setTelephonyProperty(this.mParentApp.getPhoneId(), key, val);
        log("[key, value]=" + key + ", " + val);
    }

    protected String getSystemProperty(String key, String defaultVal) {
        TelephonyManager.getDefault();
        return TelephonyManager.getTelephonyProperty(this.mParentApp.getPhoneId(), key, defaultVal);
    }

    public String getIccSimChallengeResponse(int authContext, String data) {
        String strEncodeToString = null;
        log("getIccSimChallengeResponse:");
        try {
            synchronized (this.mLock) {
                CommandsInterface ci = this.mCi;
                UiccCardApplication parentApp = this.mParentApp;
                if (ci != null && parentApp != null) {
                    ci.requestIccSimAuthentication(authContext, data, parentApp.getAid(), obtainMessage(EVENT_AKA_AUTHENTICATE_DONE));
                    try {
                        this.mLock.wait();
                        log("getIccSimChallengeResponse: return auth_rsp");
                        strEncodeToString = Base64.encodeToString(this.auth_rsp.payload, 2);
                    } catch (InterruptedException e) {
                        loge("getIccSimChallengeResponse: Fail, interrupted while trying to request Icc Sim Auth");
                    }
                } else {
                    loge("getIccSimChallengeResponse: Fail, ci or parentApp is null");
                }
            }
        } catch (Exception e2) {
            loge("getIccSimChallengeResponse: Fail while trying to request Icc Sim Auth");
        }
        return strEncodeToString;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("IccRecords: " + this);
        pw.println(" mDestroyed=" + this.mDestroyed);
        pw.println(" mCi=" + this.mCi);
        pw.println(" mFh=" + this.mFh);
        pw.println(" mParentApp=" + this.mParentApp);
        pw.println(" recordsLoadedRegistrants: size=" + this.mRecordsLoadedRegistrants.size());
        for (int i = 0; i < this.mRecordsLoadedRegistrants.size(); i++) {
            pw.println("  recordsLoadedRegistrants[" + i + "]=" + ((Registrant) this.mRecordsLoadedRegistrants.get(i)).getHandler());
        }
        pw.println(" mImsiReadyRegistrants: size=" + this.mImsiReadyRegistrants.size());
        for (int i2 = 0; i2 < this.mImsiReadyRegistrants.size(); i2++) {
            pw.println("  mImsiReadyRegistrants[" + i2 + "]=" + ((Registrant) this.mImsiReadyRegistrants.get(i2)).getHandler());
        }
        pw.println(" mRecordsEventsRegistrants: size=" + this.mRecordsEventsRegistrants.size());
        for (int i3 = 0; i3 < this.mRecordsEventsRegistrants.size(); i3++) {
            pw.println("  mRecordsEventsRegistrants[" + i3 + "]=" + ((Registrant) this.mRecordsEventsRegistrants.get(i3)).getHandler());
        }
        pw.println(" mNewSmsRegistrants: size=" + this.mNewSmsRegistrants.size());
        for (int i4 = 0; i4 < this.mNewSmsRegistrants.size(); i4++) {
            pw.println("  mNewSmsRegistrants[" + i4 + "]=" + ((Registrant) this.mNewSmsRegistrants.get(i4)).getHandler());
        }
        pw.println(" mNetworkSelectionModeAutomaticRegistrants: size=" + this.mNetworkSelectionModeAutomaticRegistrants.size());
        for (int i5 = 0; i5 < this.mNetworkSelectionModeAutomaticRegistrants.size(); i5++) {
            pw.println("  mNetworkSelectionModeAutomaticRegistrants[" + i5 + "]=" + ((Registrant) this.mNetworkSelectionModeAutomaticRegistrants.get(i5)).getHandler());
        }
        pw.println(" mRecordsRequested=" + this.mRecordsRequested);
        pw.println(" mRecordsToLoad=" + this.mRecordsToLoad);
        pw.println(" mRdnCache=" + this.mAdnCache);
        pw.println(" iccid=" + this.mIccId);
        pw.println(" mMsisdn=" + this.mMsisdn);
        pw.println(" mMsisdnTag=" + this.mMsisdnTag);
        pw.println(" mVoiceMailNum=" + this.mVoiceMailNum);
        pw.println(" mVoiceMailTag=" + this.mVoiceMailTag);
        pw.println(" mNewVoiceMailNum=" + this.mNewVoiceMailNum);
        pw.println(" mNewVoiceMailTag=" + this.mNewVoiceMailTag);
        pw.println(" mIsVoiceMailFixed=" + this.mIsVoiceMailFixed);
        pw.println(" mImsi=" + this.mImsi);
        pw.println(" mMncLength=" + this.mMncLength);
        pw.println(" mMailboxIndex=" + this.mMailboxIndex);
        pw.println(" mSpn=" + this.mSpn);
        pw.println(" mIsIccActivation=" + this.mIsIccActivation);
        pw.flush();
    }
}
