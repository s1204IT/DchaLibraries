package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IccRecords extends Handler implements IccConstants {
    public static final int C2K_PHB_NOT_READY = 2;
    public static final int C2K_PHB_READY = 3;
    public static final int CALL_FORWARDING_STATUS_DISABLED = 0;
    public static final int CALL_FORWARDING_STATUS_ENABLED = 1;
    public static final int CALL_FORWARDING_STATUS_UNKNOWN = -1;
    protected static final boolean DBG = true;
    public static final int EF_RAT_FOR_OTHER_CASE = 512;
    public static final int EF_RAT_NOT_EXIST_IN_USIM = 256;
    public static final int EF_RAT_UNDEFINED = -256;
    private static final int EVENT_AKA_AUTHENTICATE_DONE = 90;
    protected static final int EVENT_APP_READY = 1;
    public static final int EVENT_CFI = 1;
    public static final int EVENT_GET_ICC_RECORD_DONE = 100;
    public static final int EVENT_MSISDN = 100;
    public static final int EVENT_MWI = 0;
    protected static final int EVENT_PHB_READY = 410;
    protected static final int EVENT_SET_MSISDN_DONE = 30;
    public static final int EVENT_SPN = 2;
    public static final int GSM_PHB_NOT_READY = 0;
    public static final int GSM_PHB_READY = 1;
    public static final int SPN_RULE_SHOW_PLMN = 2;
    public static final int SPN_RULE_SHOW_SPN = 1;
    protected static final int UNINITIALIZED = -1;
    protected static final int UNKNOWN = 0;
    protected static final boolean VDBG = false;
    private IccIoResult auth_rsp;
    protected AdnRecordCache mAdnCache;
    protected CommandsInterface mCi;
    protected Context mContext;
    protected IccFileHandler mFh;
    protected String mFullIccId;
    protected String mGid1;
    protected String mGid2;
    protected String mIccId;
    protected String mImsi;
    protected UiccCardApplication mParentApp;
    protected String mPrefLang;
    protected int mRecordsToLoad;
    private String mSpn;
    protected TelephonyManager mTelephonyManager;
    protected static final boolean ENGDEBUG = TextUtils.equals(Build.TYPE, "eng");
    protected static final String[] ICCRECORD_PROPERTY_ICCID = {"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
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
    protected int mMncLength = -1;
    protected int mMailboxIndex = 0;
    private final Object mLock = new Object();
    protected int mSubId = -1;
    protected String mOldMccMnc = UsimPBMemInfo.STRING_NOT_SET;

    public interface IccRecordLoaded {
        String getEfName();

        void onRecordLoaded(AsyncResult asyncResult);
    }

    protected abstract int getChildPhoneId();

    public abstract int getDisplayRule(String str);

    public abstract int getVoiceMessageCount();

    protected abstract void log(String str);

    protected abstract void loge(String str);

    protected abstract void onAllRecordsLoaded();

    public abstract void onReady();

    protected abstract void onRecordLoaded();

    public abstract void onRefresh(boolean z, int[] iArr);

    public abstract void setVoiceMailNumber(String str, String str2, Message message);

    public abstract void setVoiceMessageWaiting(int i, int i2);

    protected abstract void updatePHBStatus(int i, boolean z);

    @Override
    public String toString() {
        String iccIdToPrint = SubscriptionInfo.givePrintableIccid(this.mFullIccId);
        return "mDestroyed=" + this.mDestroyed + " mContext=" + this.mContext + " mCi=" + this.mCi + " mFh=" + this.mFh + " mParentApp=" + this.mParentApp + " recordsLoadedRegistrants=" + this.mRecordsLoadedRegistrants + " mImsiReadyRegistrants=" + this.mImsiReadyRegistrants + " mRecordsEventsRegistrants=" + this.mRecordsEventsRegistrants + " mNewSmsRegistrants=" + this.mNewSmsRegistrants + " mNetworkSelectionModeAutomaticRegistrants=" + this.mNetworkSelectionModeAutomaticRegistrants + " recordsToLoad=" + this.mRecordsToLoad + " adnCache=" + this.mAdnCache + " recordsRequested=" + this.mRecordsRequested + " iccid=" + iccIdToPrint + " msisdnTag=" + this.mMsisdnTag + " voiceMailNum=" + this.mVoiceMailNum + " voiceMailTag=" + this.mVoiceMailTag + " newVoiceMailNum=" + this.mNewVoiceMailNum + " newVoiceMailTag=" + this.mNewVoiceMailTag + " isVoiceMailFixed=" + this.mIsVoiceMailFixed + UsimPBMemInfo.STRING_NOT_SET + " mncLength=" + this.mMncLength + " mailboxIndex=" + this.mMailboxIndex + " spn=" + this.mSpn;
    }

    public IccRecords(UiccCardApplication app, Context c, CommandsInterface ci) {
        this.mContext = c;
        this.mCi = ci;
        this.mFh = app.getIccFileHandler();
        this.mParentApp = app;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
    }

    public void dispose() {
        this.mDestroyed.set(true);
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

    public String getFullIccId() {
        return this.mFullIccId;
    }

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

    public void unregisterForRecordsLoaded(Handler h) {
        this.mRecordsLoadedRegistrants.remove(h);
    }

    public void registerForImsiReady(Handler h, int what, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant r = new Registrant(h, what, obj);
        this.mImsiReadyRegistrants.add(r);
        if (this.mImsi == null) {
            return;
        }
        r.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
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
        return null;
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

    public String getGid2() {
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
                    log("getServiceProviderName: override, providerName=" + providerName);
                    return brandOverride;
                }
                log("getServiceProviderName: no brandOverride, providerName=" + providerName);
                return providerName;
            }
            log("getServiceProviderName: card is null, providerName=" + providerName);
            return providerName;
        }
        log("getServiceProviderName: mParentApp is null, providerName=" + providerName);
        return providerName;
    }

    protected void setServiceProviderName(String spn) {
        this.mSpn = spn;
    }

    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    protected void onIccRefreshInit() {
        this.mAdnCache.reset();
        UiccCardApplication parentApp = this.mParentApp;
        if (parentApp == null || parentApp.getState() != IccCardApplicationStatus.AppState.APPSTATE_READY) {
            return;
        }
        sendMessage(obtainMessage(1));
    }

    public boolean getRecordsLoaded() {
        return this.mRecordsToLoad == 0 && this.mRecordsRequested;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case EVENT_AKA_AUTHENTICATE_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    this.auth_rsp = null;
                    log("EVENT_AKA_AUTHENTICATE_DONE");
                    if (ar.exception != null) {
                        loge("Exception ICC SIM AKA: " + ar.exception);
                    } else {
                        try {
                            this.auth_rsp = (IccIoResult) ar.result;
                            log("ICC SIM AKA: auth_rsp = " + this.auth_rsp);
                        } catch (Exception e) {
                            loge("Failed to parse ICC SIM AKA contents: " + e);
                        }
                        break;
                    }
                    synchronized (this.mLock) {
                        this.mLock.notifyAll();
                    }
                    return;
                case 100:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    IccRecordLoaded recordLoaded = (IccRecordLoaded) ar2.userObj;
                    log(recordLoaded.getEfName() + " LOADED");
                    if (ar2.exception != null) {
                        loge("Record Load Exception: " + ar2.exception);
                    } else {
                        recordLoaded.onRecordLoaded(ar2);
                    }
                    return;
                case EVENT_PHB_READY:
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    if (ar3 == null || ar3.exception != null || ar3.result == null) {
                        return;
                    }
                    int[] isPhbReady = (int[]) ar3.result;
                    String strCurSimState = UsimPBMemInfo.STRING_NOT_SET;
                    int phoneId = getChildPhoneId();
                    String strAllSimState = SystemProperties.get("gsm.sim.state");
                    if (strAllSimState != null && strAllSimState.length() > 0) {
                        String[] values = strAllSimState.split(",");
                        if (phoneId >= 0 && phoneId < values.length && values[phoneId] != null) {
                            strCurSimState = values[phoneId];
                        }
                    }
                    boolean isSimLocked = !strCurSimState.equals("NETWORK_LOCKED") ? strCurSimState.equals("PIN_REQUIRED") : true;
                    log("isPhbReady=" + isPhbReady[0] + ",strCurSimState = " + strCurSimState + ", isSimLocked = " + isSimLocked);
                    updatePHBStatus(isPhbReady[0], isSimLocked);
                    updateIccFdnStatus();
                    return;
                default:
                    super.handleMessage(msg);
                    return;
            }
        } catch (RuntimeException exc) {
            loge("Exception parsing SIM record: " + exc);
            return;
        } finally {
        }
        onRecordLoaded();
    }

    public String getSimLanguage() {
        return this.mPrefLang;
    }

    protected void setSimLanguage(byte[] efLi, byte[] efPl) {
        String[] locales = this.mContext.getAssets().getLocales();
        try {
            this.mPrefLang = findBestLanguage(efLi, locales);
        } catch (UnsupportedEncodingException e) {
            log("Unable to parse EF-LI: " + Arrays.toString(efLi));
        }
        if (this.mPrefLang != null) {
            return;
        }
        try {
            this.mPrefLang = findBestLanguage(efPl, locales);
        } catch (UnsupportedEncodingException e2) {
            log("Unable to parse EF-PL: " + Arrays.toString(efLi));
        }
    }

    protected static String findBestLanguage(byte[] languages, String[] locales) throws UnsupportedEncodingException {
        if (languages == null || locales == null) {
            return null;
        }
        for (int i = 0; i + 1 < languages.length; i += 2) {
            String lang = new String(languages, i, 2, "ISO-8859-1");
            for (int j = 0; j < locales.length; j++) {
                if (locales[j] != null && locales[j].length() >= 2 && locales[j].substring(0, 2).equalsIgnoreCase(lang)) {
                    return lang;
                }
            }
        }
        return null;
    }

    public boolean isCspPlmnEnabled() {
        return false;
    }

    public String getOperatorNumeric() {
        return null;
    }

    public int getVoiceCallForwardingFlag() {
        return -1;
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

    public String getIccSimChallengeResponse(int authContext, String data) {
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
                        return Base64.encodeToString(this.auth_rsp.payload, 2);
                    } catch (InterruptedException e) {
                        loge("getIccSimChallengeResponse: Fail, interrupted while trying to request Icc Sim Auth");
                        return null;
                    }
                }
                loge("getIccSimChallengeResponse: Fail, ci or parentApp is null");
                return null;
            }
        } catch (Exception e2) {
            loge("getIccSimChallengeResponse: Fail while trying to request Icc Sim Auth");
            return null;
        }
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
        String iccIdToPrint = SubscriptionInfo.givePrintableIccid(this.mFullIccId);
        pw.println(" iccid=" + iccIdToPrint);
        if (TextUtils.isEmpty(this.mMsisdn)) {
            pw.println(" mMsisdn=null");
        } else {
            pw.println(" mMsisdn=XXX");
        }
        pw.println(" mMsisdnTag=" + this.mMsisdnTag);
        pw.println(" mVoiceMailNum=" + this.mVoiceMailNum);
        pw.println(" mVoiceMailTag=" + this.mVoiceMailTag);
        pw.println(" mNewVoiceMailNum=" + this.mNewVoiceMailNum);
        pw.println(" mNewVoiceMailTag=" + this.mNewVoiceMailTag);
        pw.println(" mIsVoiceMailFixed=" + this.mIsVoiceMailFixed);
        pw.println(" mMncLength=" + this.mMncLength);
        pw.println(" mMailboxIndex=" + this.mMailboxIndex);
        pw.println(" mSpn=" + this.mSpn);
        pw.flush();
    }

    public String getMenuTitleFromEf() {
        return null;
    }

    public int getEfRatBalancing() {
        return EF_RAT_UNDEFINED;
    }

    public String getSpNameInEfSpn() {
        return null;
    }

    public String isOperatorMvnoForImsi() {
        return null;
    }

    public String getFirstFullNameInEfPnn() {
        return null;
    }

    public String isOperatorMvnoForEfPnn() {
        return null;
    }

    public String getMvnoMatchType() {
        return null;
    }

    public String getSIMCPHSOns() {
        return null;
    }

    public String getEfGbabp() {
        return null;
    }

    public void setEfGbabp(String gbabp, Message onComplete) {
    }

    public byte[] getEfPsismsc() {
        return null;
    }

    public byte[] getEfSmsp() {
        return null;
    }

    public int getMncLength() {
        return 0;
    }

    enum IccServiceStatus {
        NOT_EXIST_IN_SIM,
        NOT_EXIST_IN_USIM,
        ACTIVATED,
        INACTIVATED,
        UNKNOWN;

        public static IccServiceStatus[] valuesCustom() {
            return values();
        }
    }

    enum IccService {
        CHV1_DISABLE_FUNCTION,
        SPN,
        PNN,
        OPL,
        MWIS,
        CFIS,
        SPDI,
        EPLMN,
        SMSP,
        FDN,
        UNSUPPORTED_SERVICE;


        private static final int[] f30x7311bed0 = null;

        private static int[] m478xd68dcb74() {
            if (f30x7311bed0 != null) {
                return f30x7311bed0;
            }
            int[] iArr = new int[valuesCustom().length];
            try {
                iArr[CFIS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[CHV1_DISABLE_FUNCTION.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[EPLMN.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[FDN.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[MWIS.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[OPL.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[PNN.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[SMSP.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[SPDI.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                iArr[SPN.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                iArr[UNSUPPORTED_SERVICE.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            f30x7311bed0 = iArr;
            return iArr;
        }

        public static IccService[] valuesCustom() {
            return values();
        }

        public int getIndex() {
            switch (m478xd68dcb74()[ordinal()]) {
                case 1:
                    return 5;
                case 2:
                    return 0;
                case 3:
                    return 7;
                case 4:
                    return 9;
                case 5:
                    return 4;
                case 6:
                    return 3;
                case 7:
                    return 2;
                case 8:
                    return 8;
                case 9:
                    return 6;
                case 10:
                    return 1;
                case 11:
                    return 10;
                default:
                    return -1;
            }
        }
    }

    public IccServiceStatus getSIMServiceStatus(IccService enService) {
        return IccServiceStatus.NOT_EXIST_IN_USIM;
    }

    public boolean isRadioAvailable() {
        return false;
    }

    public boolean isPhbReady() {
        return false;
    }

    protected void updateIccFdnStatus() {
    }
}
