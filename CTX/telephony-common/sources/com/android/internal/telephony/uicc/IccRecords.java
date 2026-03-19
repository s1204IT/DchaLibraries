package com.android.internal.telephony.uicc;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import com.android.internal.telephony.CommandsInterface;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class IccRecords extends Handler implements IccConstants {
    public static final int CALL_FORWARDING_STATUS_DISABLED = 0;
    public static final int CALL_FORWARDING_STATUS_ENABLED = 1;
    public static final int CALL_FORWARDING_STATUS_UNKNOWN = -1;
    protected static final boolean DBG = true;
    public static final int DEFAULT_VOICE_MESSAGE_COUNT = -2;
    protected static final int EVENT_AKA_AUTHENTICATE_DONE = 90;
    protected static final int EVENT_APP_READY = 1;
    public static final int EVENT_CFI = 1;
    public static final int EVENT_GET_ICC_RECORD_DONE = 100;
    public static final int EVENT_MWI = 0;
    public static final int EVENT_REFRESH = 31;
    public static final int EVENT_SPN = 2;
    protected static final int HANDLER_ACTION_BASE = 1238272;
    protected static final int HANDLER_ACTION_NONE = 1238272;
    protected static final int HANDLER_ACTION_SEND_RESPONSE = 1238273;
    protected static final int LOCKED_RECORDS_REQ_REASON_LOCKED = 1;
    protected static final int LOCKED_RECORDS_REQ_REASON_NETWORK_LOCKED = 2;
    protected static final int LOCKED_RECORDS_REQ_REASON_NONE = 0;
    public static final int SPN_RULE_SHOW_PLMN = 2;
    public static final int SPN_RULE_SHOW_SPN = 1;
    protected static final int UNINITIALIZED = -1;
    protected static final int UNKNOWN = 0;
    public static final int UNKNOWN_VOICE_MESSAGE_COUNT = -1;
    protected static final boolean VDBG = false;
    protected static AtomicInteger sNextRequestId = new AtomicInteger(1);
    private IccIoResult auth_rsp;
    protected AdnRecordCache mAdnCache;
    protected CommandsInterface mCi;
    protected Context mContext;
    protected String[] mEhplmns;
    protected IccFileHandler mFh;
    protected String[] mFplmns;
    protected String mFullIccId;
    protected String mGid1;
    protected String mGid2;
    protected PlmnActRecord[] mHplmnActRecords;
    protected String mIccId;
    protected String mImsi;
    protected PlmnActRecord[] mOplmnActRecords;
    protected UiccCardApplication mParentApp;
    protected PlmnActRecord[] mPlmnActRecords;
    protected String mPnnHomeName;
    protected String mPrefLang;
    protected int mRecordsToLoad;
    private String mSpn;
    protected TelephonyManager mTelephonyManager;
    protected AtomicBoolean mDestroyed = new AtomicBoolean(false);
    protected AtomicBoolean mLoaded = new AtomicBoolean(false);
    protected RegistrantList mRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mLockedRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mNetworkLockedRecordsLoadedRegistrants = new RegistrantList();
    protected RegistrantList mImsiReadyRegistrants = new RegistrantList();
    protected RegistrantList mRecordsEventsRegistrants = new RegistrantList();
    protected RegistrantList mNewSmsRegistrants = new RegistrantList();
    protected RegistrantList mNetworkSelectionModeAutomaticRegistrants = new RegistrantList();
    protected RegistrantList mSpnUpdatedRegistrants = new RegistrantList();
    protected RegistrantList mRecordsOverrideRegistrants = new RegistrantList();
    protected boolean mRecordsRequested = false;
    protected int mLockedRecordsReqReason = 0;
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
    protected final HashMap<Integer, Message> mPendingResponses = new HashMap<>();
    CarrierTestOverride mCarrierTestOverride = new CarrierTestOverride();

    public interface IccRecordLoaded {
        String getEfName();

        void onRecordLoaded(AsyncResult asyncResult);
    }

    public abstract int getDisplayRule(ServiceState serviceState);

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
        String str;
        String str2;
        String str3;
        String str4;
        String strGivePrintableIccid = SubscriptionInfo.givePrintableIccid(this.mFullIccId);
        StringBuilder sb = new StringBuilder();
        sb.append("mDestroyed=");
        sb.append(this.mDestroyed);
        sb.append(" mContext=");
        sb.append(this.mContext);
        sb.append(" mCi=");
        sb.append(this.mCi);
        sb.append(" mFh=");
        sb.append(this.mFh);
        sb.append(" mParentApp=");
        sb.append(this.mParentApp);
        sb.append(" recordsToLoad=");
        sb.append(this.mRecordsToLoad);
        sb.append(" adnCache=");
        sb.append(this.mAdnCache);
        sb.append(" recordsRequested=");
        sb.append(this.mRecordsRequested);
        sb.append(" lockedRecordsReqReason=");
        sb.append(this.mLockedRecordsReqReason);
        sb.append(" iccid=");
        sb.append(strGivePrintableIccid);
        if (this.mCarrierTestOverride.isInTestMode()) {
            str = "mFakeIccid=" + this.mCarrierTestOverride.getFakeIccid();
        } else {
            str = "";
        }
        sb.append(str);
        sb.append(" msisdnTag=");
        sb.append(this.mMsisdnTag);
        sb.append(" voiceMailNum=");
        sb.append(Rlog.pii(false, this.mVoiceMailNum));
        sb.append(" voiceMailTag=");
        sb.append(this.mVoiceMailTag);
        sb.append(" voiceMailNum=");
        sb.append(Rlog.pii(false, this.mNewVoiceMailNum));
        sb.append(" newVoiceMailTag=");
        sb.append(this.mNewVoiceMailTag);
        sb.append(" isVoiceMailFixed=");
        sb.append(this.mIsVoiceMailFixed);
        sb.append(" mImsi=");
        if (this.mImsi != null) {
            str2 = this.mImsi.substring(0, 6) + Rlog.pii(false, this.mImsi.substring(6));
        } else {
            str2 = "null";
        }
        sb.append(str2);
        if (this.mCarrierTestOverride.isInTestMode()) {
            str3 = " mFakeImsi=" + this.mCarrierTestOverride.getFakeIMSI();
        } else {
            str3 = "";
        }
        sb.append(str3);
        sb.append(" mncLength=");
        sb.append(this.mMncLength);
        sb.append(" mailboxIndex=");
        sb.append(this.mMailboxIndex);
        sb.append(" spn=");
        sb.append(this.mSpn);
        if (this.mCarrierTestOverride.isInTestMode()) {
            str4 = " mFakeSpn=" + this.mCarrierTestOverride.getFakeSpn();
        } else {
            str4 = "";
        }
        sb.append(str4);
        return sb.toString();
    }

    public IccRecords(UiccCardApplication uiccCardApplication, Context context, CommandsInterface commandsInterface) {
        this.mContext = context;
        this.mCi = commandsInterface;
        this.mFh = uiccCardApplication.getIccFileHandler();
        this.mParentApp = uiccCardApplication;
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mCi.registerForIccRefresh(this, 31, null);
    }

    public void setCarrierTestOverride(String str, String str2, String str3, String str4, String str5, String str6, String str7) {
        this.mCarrierTestOverride.override(str, str2, str3, str4, str5, str6, str7);
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mParentApp.getPhoneId(), str7);
        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mParentApp.getPhoneId(), str);
        this.mRecordsOverrideRegistrants.notifyRegistrants();
    }

    public void dispose() {
        this.mDestroyed.set(true);
        this.auth_rsp = null;
        synchronized (this.mLock) {
            this.mLock.notifyAll();
        }
        this.mCi.unregisterForIccRefresh(this);
        this.mParentApp = null;
        this.mFh = null;
        this.mCi = null;
        this.mContext = null;
        if (this.mAdnCache != null) {
            this.mAdnCache.reset();
        }
        this.mLoaded.set(false);
    }

    public AdnRecordCache getAdnCache() {
        return this.mAdnCache;
    }

    public int storePendingResponseMessage(Message message) {
        int andIncrement = sNextRequestId.getAndIncrement();
        synchronized (this.mPendingResponses) {
            this.mPendingResponses.put(Integer.valueOf(andIncrement), message);
        }
        return andIncrement;
    }

    public Message retrievePendingResponseMessage(Integer num) {
        Message messageRemove;
        synchronized (this.mPendingResponses) {
            messageRemove = this.mPendingResponses.remove(num);
        }
        return messageRemove;
    }

    public String getIccId() {
        if (this.mCarrierTestOverride.isInTestMode() && this.mCarrierTestOverride.getFakeIccid() != null) {
            return this.mCarrierTestOverride.getFakeIccid();
        }
        return this.mIccId;
    }

    public String getFullIccId() {
        return this.mFullIccId;
    }

    public void registerForRecordsLoaded(Handler handler, int i, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant registrant = new Registrant(handler, i, obj);
        this.mRecordsLoadedRegistrants.add(registrant);
        if (getRecordsLoaded()) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public void unregisterForRecordsLoaded(Handler handler) {
        this.mRecordsLoadedRegistrants.remove(handler);
    }

    public void unregisterForRecordsOverride(Handler handler) {
        this.mRecordsOverrideRegistrants.remove(handler);
    }

    public void registerForRecordsOverride(Handler handler, int i, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant registrant = new Registrant(handler, i, obj);
        this.mRecordsOverrideRegistrants.add(registrant);
        if (getRecordsLoaded()) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public void registerForLockedRecordsLoaded(Handler handler, int i, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant registrant = new Registrant(handler, i, obj);
        this.mLockedRecordsLoadedRegistrants.add(registrant);
        if (getLockedRecordsLoaded()) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public void unregisterForLockedRecordsLoaded(Handler handler) {
        this.mLockedRecordsLoadedRegistrants.remove(handler);
    }

    public void registerForNetworkLockedRecordsLoaded(Handler handler, int i, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant registrant = new Registrant(handler, i, obj);
        this.mNetworkLockedRecordsLoadedRegistrants.add(registrant);
        if (getNetworkLockedRecordsLoaded()) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public void unregisterForNetworkLockedRecordsLoaded(Handler handler) {
        this.mNetworkLockedRecordsLoadedRegistrants.remove(handler);
    }

    public void registerForImsiReady(Handler handler, int i, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant registrant = new Registrant(handler, i, obj);
        this.mImsiReadyRegistrants.add(registrant);
        if (getIMSI() != null) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public void unregisterForImsiReady(Handler handler) {
        this.mImsiReadyRegistrants.remove(handler);
    }

    public void registerForSpnUpdate(Handler handler, int i, Object obj) {
        if (this.mDestroyed.get()) {
            return;
        }
        Registrant registrant = new Registrant(handler, i, obj);
        this.mSpnUpdatedRegistrants.add(registrant);
        if (!TextUtils.isEmpty(this.mSpn)) {
            registrant.notifyRegistrant(new AsyncResult((Object) null, (Object) null, (Throwable) null));
        }
    }

    public void unregisterForSpnUpdate(Handler handler) {
        this.mSpnUpdatedRegistrants.remove(handler);
    }

    public void registerForRecordsEvents(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mRecordsEventsRegistrants.add(registrant);
        registrant.notifyResult(0);
        registrant.notifyResult(1);
    }

    public void unregisterForRecordsEvents(Handler handler) {
        this.mRecordsEventsRegistrants.remove(handler);
    }

    public void registerForNewSms(Handler handler, int i, Object obj) {
        this.mNewSmsRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForNewSms(Handler handler) {
        this.mNewSmsRegistrants.remove(handler);
    }

    public void registerForNetworkSelectionModeAutomatic(Handler handler, int i, Object obj) {
        this.mNetworkSelectionModeAutomaticRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForNetworkSelectionModeAutomatic(Handler handler) {
        this.mNetworkSelectionModeAutomaticRegistrants.remove(handler);
    }

    public String getIMSI() {
        if (this.mCarrierTestOverride.isInTestMode() && this.mCarrierTestOverride.getFakeIMSI() != null) {
            return this.mCarrierTestOverride.getFakeIMSI();
        }
        return this.mImsi;
    }

    public void setImsi(String str) {
        this.mImsi = str;
        this.mImsiReadyRegistrants.notifyRegistrants();
    }

    public String getNAI() {
        return null;
    }

    public String getMsisdnNumber() {
        return this.mMsisdn;
    }

    public String getGid1() {
        if (this.mCarrierTestOverride.isInTestMode() && this.mCarrierTestOverride.getFakeGid1() != null) {
            return this.mCarrierTestOverride.getFakeGid1();
        }
        return this.mGid1;
    }

    public String getGid2() {
        if (this.mCarrierTestOverride.isInTestMode() && this.mCarrierTestOverride.getFakeGid2() != null) {
            return this.mCarrierTestOverride.getFakeGid2();
        }
        return this.mGid2;
    }

    public String getPnnHomeName() {
        if (this.mCarrierTestOverride.isInTestMode() && this.mCarrierTestOverride.getFakePnnHomeName() != null) {
            return this.mCarrierTestOverride.getFakePnnHomeName();
        }
        return this.mPnnHomeName;
    }

    public void setMsisdnNumber(String str, String str2, Message message) {
        loge("setMsisdn() should not be invoked on base IccRecords");
        AsyncResult.forMessage(message).exception = new IccIoResult(106, 130, (byte[]) null).getException();
        message.sendToTarget();
    }

    public String getMsisdnAlphaTag() {
        return this.mMsisdnTag;
    }

    public String getVoiceMailNumber() {
        return this.mVoiceMailNum;
    }

    public String getServiceProviderName() {
        if (this.mCarrierTestOverride.isInTestMode() && this.mCarrierTestOverride.getFakeSpn() != null) {
            return this.mCarrierTestOverride.getFakeSpn();
        }
        String str = this.mSpn;
        UiccCardApplication uiccCardApplication = this.mParentApp;
        if (uiccCardApplication != null) {
            UiccProfile uiccProfile = uiccCardApplication.getUiccProfile();
            if (uiccProfile != null) {
                String operatorBrandOverride = uiccProfile.getOperatorBrandOverride();
                if (operatorBrandOverride != null) {
                    log("getServiceProviderName: override, providerName=" + str);
                    return operatorBrandOverride;
                }
                log("getServiceProviderName: no brandOverride, providerName=" + str);
                return str;
            }
            log("getServiceProviderName: card is null, providerName=" + str);
            return str;
        }
        log("getServiceProviderName: mParentApp is null, providerName=" + str);
        return str;
    }

    protected void setServiceProviderName(String str) {
        if (!TextUtils.equals(this.mSpn, str)) {
            this.mSpnUpdatedRegistrants.notifyRegistrants();
            this.mSpn = str;
        }
    }

    public String getVoiceMailAlphaTag() {
        return this.mVoiceMailTag;
    }

    public boolean getRecordsLoaded() {
        return this.mRecordsToLoad == 0 && this.mRecordsRequested;
    }

    protected boolean getLockedRecordsLoaded() {
        return this.mRecordsToLoad == 0 && this.mLockedRecordsReqReason == 1;
    }

    protected boolean getNetworkLockedRecordsLoaded() {
        return this.mRecordsToLoad == 0 && this.mLockedRecordsReqReason == 2;
    }

    @Override
    public void handleMessage(Message message) {
        int i = message.what;
        if (i == 31) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            log("Card REFRESH occurred: ");
            if (asyncResult.exception == null) {
                handleRefresh((IccRefreshResponse) asyncResult.result);
                return;
            }
            loge("Icc refresh Exception: " + asyncResult.exception);
            return;
        }
        if (i != EVENT_AKA_AUTHENTICATE_DONE) {
            try {
                if (i == 100) {
                    try {
                        AsyncResult asyncResult2 = (AsyncResult) message.obj;
                        IccRecordLoaded iccRecordLoaded = (IccRecordLoaded) asyncResult2.userObj;
                        log(iccRecordLoaded.getEfName() + " LOADED");
                        if (asyncResult2.exception != null) {
                            loge("Record Load Exception: " + asyncResult2.exception);
                        } else {
                            iccRecordLoaded.onRecordLoaded(asyncResult2);
                        }
                    } catch (RuntimeException e) {
                        loge("Exception parsing SIM record: " + e);
                    }
                    return;
                }
                super.handleMessage(message);
                return;
            } finally {
                onRecordLoaded();
            }
        }
        AsyncResult asyncResult3 = (AsyncResult) message.obj;
        this.auth_rsp = null;
        log("EVENT_AKA_AUTHENTICATE_DONE");
        if (asyncResult3.exception != null) {
            loge("Exception ICC SIM AKA: " + asyncResult3.exception);
        } else {
            try {
                this.auth_rsp = (IccIoResult) asyncResult3.result;
                log("ICC SIM AKA: auth_rsp = " + this.auth_rsp);
            } catch (Exception e2) {
                loge("Failed to parse ICC SIM AKA contents: " + e2);
            }
        }
        synchronized (this.mLock) {
            this.mLock.notifyAll();
        }
    }

    public String getSimLanguage() {
        return this.mPrefLang;
    }

    protected void setSimLanguage(byte[] bArr, byte[] bArr2) {
        String[] locales = this.mContext.getAssets().getLocales();
        try {
            this.mPrefLang = findBestLanguage(bArr, locales);
        } catch (UnsupportedEncodingException e) {
            log("Unable to parse EF-LI: " + Arrays.toString(bArr));
        }
        if (this.mPrefLang == null) {
            try {
                this.mPrefLang = findBestLanguage(bArr2, locales);
            } catch (UnsupportedEncodingException e2) {
                log("Unable to parse EF-PL: " + Arrays.toString(bArr));
            }
        }
    }

    protected static String findBestLanguage(byte[] bArr, String[] strArr) throws UnsupportedEncodingException {
        if (bArr == null || strArr == null) {
            return null;
        }
        for (int i = 0; i + 1 < bArr.length; i += 2) {
            String str = new String(bArr, i, 2, "ISO-8859-1");
            for (int i2 = 0; i2 < strArr.length; i2++) {
                if (strArr[i2] != null && strArr[i2].length() >= 2 && strArr[i2].substring(0, 2).equalsIgnoreCase(str)) {
                    return str;
                }
            }
        }
        return null;
    }

    protected void handleRefresh(IccRefreshResponse iccRefreshResponse) {
        if (iccRefreshResponse == null) {
            log("handleRefresh received without input");
            return;
        }
        if (!TextUtils.isEmpty(iccRefreshResponse.aid) && !iccRefreshResponse.aid.equals(this.mParentApp.getAid())) {
            return;
        }
        if (iccRefreshResponse.refreshResult == 0) {
            log("handleRefresh with SIM_FILE_UPDATED");
            handleFileUpdate(iccRefreshResponse.efId);
        } else {
            log("handleRefresh with unknown operation");
        }
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

    public void setVoiceCallForwardingFlag(int i, boolean z, String str) {
    }

    public boolean isLoaded() {
        return this.mLoaded.get();
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

    protected void setSystemProperty(String str, String str2) {
        TelephonyManager.getDefault();
        TelephonyManager.setTelephonyProperty(this.mParentApp.getPhoneId(), str, str2);
        log("[key, value]=" + str + ", " + str2);
    }

    public String getIccSimChallengeResponse(int i, String str) {
        log("getIccSimChallengeResponse:");
        try {
            synchronized (this.mLock) {
                CommandsInterface commandsInterface = this.mCi;
                UiccCardApplication uiccCardApplication = this.mParentApp;
                if (commandsInterface != null && uiccCardApplication != null) {
                    commandsInterface.requestIccSimAuthentication(i, str, uiccCardApplication.getAid(), obtainMessage(EVENT_AKA_AUTHENTICATE_DONE));
                    try {
                        this.mLock.wait();
                        if (this.auth_rsp == null) {
                            loge("getIccSimChallengeResponse: No authentication response");
                            return null;
                        }
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

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("IccRecords: " + this);
        printWriter.println(" mDestroyed=" + this.mDestroyed);
        printWriter.println(" mCi=" + this.mCi);
        printWriter.println(" mFh=" + this.mFh);
        printWriter.println(" mParentApp=" + this.mParentApp);
        printWriter.println(" recordsLoadedRegistrants: size=" + this.mRecordsLoadedRegistrants.size());
        for (int i = 0; i < this.mRecordsLoadedRegistrants.size(); i++) {
            printWriter.println("  recordsLoadedRegistrants[" + i + "]=" + ((Registrant) this.mRecordsLoadedRegistrants.get(i)).getHandler());
        }
        printWriter.println(" mLockedRecordsLoadedRegistrants: size=" + this.mLockedRecordsLoadedRegistrants.size());
        for (int i2 = 0; i2 < this.mLockedRecordsLoadedRegistrants.size(); i2++) {
            printWriter.println("  mLockedRecordsLoadedRegistrants[" + i2 + "]=" + ((Registrant) this.mLockedRecordsLoadedRegistrants.get(i2)).getHandler());
        }
        printWriter.println(" mNetworkLockedRecordsLoadedRegistrants: size=" + this.mNetworkLockedRecordsLoadedRegistrants.size());
        for (int i3 = 0; i3 < this.mNetworkLockedRecordsLoadedRegistrants.size(); i3++) {
            printWriter.println("  mLockedRecordsLoadedRegistrants[" + i3 + "]=" + ((Registrant) this.mNetworkLockedRecordsLoadedRegistrants.get(i3)).getHandler());
        }
        printWriter.println(" mImsiReadyRegistrants: size=" + this.mImsiReadyRegistrants.size());
        for (int i4 = 0; i4 < this.mImsiReadyRegistrants.size(); i4++) {
            printWriter.println("  mImsiReadyRegistrants[" + i4 + "]=" + ((Registrant) this.mImsiReadyRegistrants.get(i4)).getHandler());
        }
        printWriter.println(" mRecordsEventsRegistrants: size=" + this.mRecordsEventsRegistrants.size());
        for (int i5 = 0; i5 < this.mRecordsEventsRegistrants.size(); i5++) {
            printWriter.println("  mRecordsEventsRegistrants[" + i5 + "]=" + ((Registrant) this.mRecordsEventsRegistrants.get(i5)).getHandler());
        }
        printWriter.println(" mNewSmsRegistrants: size=" + this.mNewSmsRegistrants.size());
        for (int i6 = 0; i6 < this.mNewSmsRegistrants.size(); i6++) {
            printWriter.println("  mNewSmsRegistrants[" + i6 + "]=" + ((Registrant) this.mNewSmsRegistrants.get(i6)).getHandler());
        }
        printWriter.println(" mNetworkSelectionModeAutomaticRegistrants: size=" + this.mNetworkSelectionModeAutomaticRegistrants.size());
        for (int i7 = 0; i7 < this.mNetworkSelectionModeAutomaticRegistrants.size(); i7++) {
            printWriter.println("  mNetworkSelectionModeAutomaticRegistrants[" + i7 + "]=" + ((Registrant) this.mNetworkSelectionModeAutomaticRegistrants.get(i7)).getHandler());
        }
        printWriter.println(" mRecordsRequested=" + this.mRecordsRequested);
        printWriter.println(" mLockedRecordsReqReason=" + this.mLockedRecordsReqReason);
        printWriter.println(" mRecordsToLoad=" + this.mRecordsToLoad);
        printWriter.println(" mRdnCache=" + this.mAdnCache);
        printWriter.println(" iccid=" + SubscriptionInfo.givePrintableIccid(this.mFullIccId));
        printWriter.println(" mMsisdn=" + Rlog.pii(false, this.mMsisdn));
        printWriter.println(" mMsisdnTag=" + this.mMsisdnTag);
        printWriter.println(" mVoiceMailNum=" + Rlog.pii(false, this.mVoiceMailNum));
        printWriter.println(" mVoiceMailTag=" + this.mVoiceMailTag);
        printWriter.println(" mNewVoiceMailNum=" + Rlog.pii(false, this.mNewVoiceMailNum));
        printWriter.println(" mNewVoiceMailTag=" + this.mNewVoiceMailTag);
        printWriter.println(" mIsVoiceMailFixed=" + this.mIsVoiceMailFixed);
        StringBuilder sb = new StringBuilder();
        sb.append(" mImsi=");
        sb.append(this.mImsi != null ? this.mImsi.substring(0, 6) + Rlog.pii(false, this.mImsi.substring(6)) : "null");
        printWriter.println(sb.toString());
        if (this.mCarrierTestOverride.isInTestMode()) {
            printWriter.println(" mFakeImsi=" + this.mCarrierTestOverride.getFakeIMSI());
        }
        printWriter.println(" mMncLength=" + this.mMncLength);
        printWriter.println(" mMailboxIndex=" + this.mMailboxIndex);
        printWriter.println(" mSpn=" + this.mSpn);
        if (this.mCarrierTestOverride.isInTestMode()) {
            printWriter.println(" mFakeSpn=" + this.mCarrierTestOverride.getFakeSpn());
        }
        printWriter.flush();
    }

    public String getSpNameInEfSpn() {
        return null;
    }

    public String isOperatorMvnoForEfPnn() {
        return null;
    }

    public String getEfGbabp() {
        return null;
    }

    public void setEfGbabp(String str, Message message) {
        if (message == null) {
            return;
        }
        AsyncResult.forMessage(message, (Object) null, new IccException("Default setEfGbabp exception."));
        message.sendToTarget();
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
}
