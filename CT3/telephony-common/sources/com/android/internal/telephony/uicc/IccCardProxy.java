package com.android.internal.telephony.uicc;

import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class IccCardProxy extends Handler implements IccCard {

    private static final int[] f27x8dbfd0b5 = null;

    private static final int[] f28x3dee1264 = null;

    private static final int[] f29x16bf601e = null;
    public static final String ACTION_INTERNAL_SIM_STATE_CHANGED = "android.intent.action.internal_sim_state_changed";
    private static final String COMMON_SLOT_PROPERTY = "";
    private static final boolean DBG = true;
    private static final int EVENT_APP_READY = 6;
    private static final int EVENT_CARRIER_PRIVILIGES_LOADED = 503;
    private static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 11;
    private static final int EVENT_ICC_ABSENT = 4;
    private static final int EVENT_ICC_CHANGED = 3;
    private static final int EVENT_ICC_FDN_CHANGED = 101;
    private static final int EVENT_ICC_LOCKED = 5;
    private static final int EVENT_ICC_RECORD_EVENTS = 500;
    private static final int EVENT_ICC_RECOVERY = 100;
    private static final int EVENT_IMSI_READY = 8;
    private static final int EVENT_NETWORK_LOCKED = 9;
    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 1;
    private static final int EVENT_RADIO_ON = 2;
    private static final int EVENT_RECORDS_LOADED = 7;
    private static final int EVENT_SUBSCRIPTION_ACTIVATED = 501;
    private static final int EVENT_SUBSCRIPTION_DEACTIVATED = 502;
    private static final String ICCID_STRING_FOR_NO_SIM = "N/A";
    private static final String LOG_TAG = "IccCardProxy";
    private CdmaSubscriptionSourceManager mCdmaSSM;
    private CommandsInterface mCi;
    private Context mContext;
    private Integer mPhoneId;
    private TelephonyManager mTelephonyManager;
    private UiccController mUiccController;
    private static Intent sInternalIntent = null;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};
    private RegistrantList mRecoveryRegistrants = new RegistrantList();
    private RegistrantList mFdnChangedRegistrants = new RegistrantList();
    private IccCardApplicationStatus.PersoSubState mNetworkLockState = IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_UNKNOWN;
    private String[] PROPERTY_ICCID_SIM = {"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
    private final Object mLock = new Object();
    private RegistrantList mAbsentRegistrants = new RegistrantList();
    private RegistrantList mPinLockedRegistrants = new RegistrantList();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();
    private int mCurrentAppType = 1;
    private UiccCard mUiccCard = null;
    private UiccCardApplication mUiccApplication = null;
    private IccRecords mIccRecords = null;
    private boolean mRadioOn = false;
    private boolean mQuietMode = false;
    private boolean mInitialized = false;
    private IccCardConstants.State mExternalState = IccCardConstants.State.UNKNOWN;
    private final BroadcastReceiver sReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            IccCardProxy.this.log("[Receiver]+");
            String action = intent.getAction();
            IccCardProxy.this.log("Action: " + action);
            if (action.equals(IWorldPhone.ACTION_SHUTDOWN_IPO) && IccCardProxy.sInternalIntent != null) {
                IccCardProxy.this.mContext.removeStickyBroadcastAsUser(IccCardProxy.sInternalIntent, UserHandle.ALL);
            }
            IccCardProxy.this.log("[Receiver]-");
        }
    };

    private static int[] m474xf663cf59() {
        if (f27x8dbfd0b5 != null) {
            return f27x8dbfd0b5;
        }
        int[] iArr = new int[IccCardConstants.State.values().length];
        try {
            iArr[IccCardConstants.State.ABSENT.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IccCardConstants.State.CARD_IO_ERROR.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IccCardConstants.State.NOT_READY.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[IccCardConstants.State.PERM_DISABLED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[IccCardConstants.State.READY.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[IccCardConstants.State.UNKNOWN.ordinal()] = 20;
        } catch (NoSuchFieldError e9) {
        }
        f27x8dbfd0b5 = iArr;
        return iArr;
    }

    private static int[] m475x37a84908() {
        if (f28x3dee1264 != null) {
            return f28x3dee1264;
        }
        int[] iArr = new int[IccCardApplicationStatus.AppState.valuesCustom().length];
        try {
            iArr[IccCardApplicationStatus.AppState.APPSTATE_DETECTED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IccCardApplicationStatus.AppState.APPSTATE_PIN.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IccCardApplicationStatus.AppState.APPSTATE_PUK.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IccCardApplicationStatus.AppState.APPSTATE_READY.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        f28x3dee1264 = iArr;
        return iArr;
    }

    private static int[] m476x5ed1affa() {
        if (f29x16bf601e != null) {
            return f29x16bf601e;
        }
        int[] iArr = new int[IccCardApplicationStatus.PersoSubState.valuesCustom().length];
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_IN_PROGRESS.ordinal()] = 20;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_READY.ordinal()] = 21;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_CORPORATE.ordinal()] = 22;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_CORPORATE_PUK.ordinal()] = 23;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_HRPD.ordinal()] = 24;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_HRPD_PUK.ordinal()] = 25;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1.ordinal()] = 26;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_NETWORK1_PUK.ordinal()] = 27;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2.ordinal()] = 28;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_NETWORK2_PUK.ordinal()] = 29;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_RUIM.ordinal()] = 30;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_RUIM_PUK.ordinal()] = 31;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER.ordinal()] = 32;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_RUIM_SERVICE_PROVIDER_PUK.ordinal()] = 33;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_CORPORATE.ordinal()] = 1;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_CORPORATE_PUK.ordinal()] = 34;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK.ordinal()] = 2;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK_PUK.ordinal()] = 35;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET.ordinal()] = 3;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK.ordinal()] = 36;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER.ordinal()] = 4;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SERVICE_PROVIDER_PUK.ordinal()] = 37;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SIM.ordinal()] = 5;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_SIM_PUK.ordinal()] = 38;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_UNKNOWN.ordinal()] = 39;
        } catch (NoSuchFieldError e25) {
        }
        f29x16bf601e = iArr;
        return iArr;
    }

    public IccCardProxy(Context context, CommandsInterface ci, int phoneId) {
        this.mPhoneId = null;
        this.mUiccController = null;
        this.mCdmaSSM = null;
        log("ctor: ci=" + ci + " phoneId=" + phoneId);
        this.mContext = context;
        this.mCi = ci;
        this.mPhoneId = Integer.valueOf(phoneId);
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(context, ci, this, 11, null);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 3, null);
        this.mUiccController.registerForIccRecovery(this, 100, null);
        ci.registerForOn(this, 2, null);
        ci.registerForOffOrNotAvailable(this, 1, null);
        setExternalState(IccCardConstants.State.NOT_READY);
        IntentFilter intentFilter = new IntentFilter(IWorldPhone.ACTION_SHUTDOWN_IPO);
        this.mContext.registerReceiver(this.sReceiver, intentFilter);
        resetProperties();
        setExternalState(IccCardConstants.State.NOT_READY, false);
    }

    public void dispose() {
        synchronized (this.mLock) {
            log("Disposing");
            this.mUiccController.unregisterForIccChanged(this);
            this.mUiccController.unregisterForIccRecovery(this);
            this.mUiccController = null;
            this.mCi.unregisterForOn(this);
            this.mCi.unregisterForOffOrNotAvailable(this);
            if (this.mCdmaSSM != null) {
                this.mCdmaSSM.dispose(this);
                this.mCdmaSSM = null;
            }
        }
    }

    public void setVoiceRadioTech(int radioTech) {
        synchronized (this.mLock) {
            log("Setting radio tech " + ServiceState.rilRadioTechnologyToString(radioTech));
            if (ServiceState.isGsm(radioTech)) {
                this.mCurrentAppType = 1;
            } else {
                this.mCurrentAppType = 2;
            }
            updateQuietMode();
        }
    }

    private void updateQuietMode() {
        boolean newQuietMode;
        synchronized (this.mLock) {
            int cdmaSource = -1;
            if (this.mCurrentAppType == 1) {
                newQuietMode = false;
                log("updateQuietMode: 3GPP subscription -> newQuietMode=false");
            } else {
                boolean isLteOnCdmaMode = TelephonyManager.getLteOnCdmaModeStatic() == 1;
                if (isLteOnCdmaMode && !isCdmaOnly()) {
                    log("updateQuietMode: is cdma/lte device, force IccCardProxy into 3gpp mode");
                    this.mCurrentAppType = 1;
                }
                cdmaSource = this.mCdmaSSM != null ? this.mCdmaSSM.getCdmaSubscriptionSource() : -1;
                newQuietMode = cdmaSource == 1 && this.mCurrentAppType == 2;
            }
            if (!this.mQuietMode && newQuietMode) {
                log("Switching to QuietMode.");
                setExternalState(IccCardConstants.State.READY);
                this.mQuietMode = newQuietMode;
            } else if (!this.mQuietMode || newQuietMode) {
                log("updateQuietMode: no changes don't setExternalState");
            } else {
                log("updateQuietMode: Switching out from QuietMode. Force broadcast of current state=" + this.mExternalState);
                this.mQuietMode = newQuietMode;
                setExternalState(this.mExternalState, true);
            }
            log("updateQuietMode: QuietMode is " + this.mQuietMode + " (app_type=" + this.mCurrentAppType + " cdmaSource=" + cdmaSource + ")");
            this.mInitialized = true;
            sendMessage(obtainMessage(3));
        }
    }

    @Override
    public void handleMessage(Message msg) {
        log("receive message " + msg.what);
        switch (msg.what) {
            case 1:
                this.mRadioOn = false;
                if (CommandsInterface.RadioState.RADIO_UNAVAILABLE == this.mCi.getRadioState()) {
                    setExternalState(IccCardConstants.State.NOT_READY);
                }
                break;
            case 2:
                this.mRadioOn = true;
                if (!this.mInitialized) {
                    updateQuietMode();
                }
                break;
            case 3:
                if (this.mInitialized) {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    int index = this.mPhoneId.intValue();
                    if (ar != null && (ar.result instanceof Integer)) {
                        index = ((Integer) ar.result).intValue();
                        log("handleMessage (EVENT_ICC_CHANGED) , index = " + index);
                    }
                    if (index == this.mPhoneId.intValue()) {
                        updateIccAvailability();
                    }
                }
                break;
            case 4:
                this.mAbsentRegistrants.notifyRegistrants();
                setExternalState(IccCardConstants.State.ABSENT);
                break;
            case 5:
                processLockedState();
                break;
            case 6:
                setExternalState(IccCardConstants.State.READY);
                break;
            case 7:
                if (this.mIccRecords != null) {
                    String operator = this.mIccRecords.getOperatorNumeric();
                    log("operator=" + operator + " mPhoneId=" + this.mPhoneId);
                    if (operator != null) {
                        String countryCode = operator.substring(0, 3);
                        if (countryCode != null) {
                            try {
                                this.mTelephonyManager.setSimCountryIsoForPhone(this.mPhoneId.intValue(), MccTable.countryCodeForMcc(Integer.parseInt(countryCode)));
                            } catch (NumberFormatException e) {
                                loge("Not number format: " + countryCode);
                            }
                        } else {
                            loge("EVENT_RECORDS_LOADED Country code is null");
                        }
                    } else {
                        loge("EVENT_RECORDS_LOADED Operator name is null");
                    }
                }
                if (this.mUiccCard != null && !this.mUiccCard.areCarrierPriviligeRulesLoaded()) {
                    this.mUiccCard.registerForCarrierPrivilegeRulesLoaded(this, EVENT_CARRIER_PRIVILIGES_LOADED, null);
                } else {
                    onRecordsLoaded();
                }
                break;
            case 8:
                broadcastIccStateChangedIntent("IMSI", null);
                break;
            case 9:
                if (this.mUiccApplication == null) {
                    loge("getIccStateReason: NETWORK_LOCKED but mUiccApplication is null!");
                } else {
                    this.mNetworkLockedRegistrants.notifyRegistrants();
                    setExternalState(IccCardConstants.State.NETWORK_LOCKED);
                }
                break;
            case 11:
                updateQuietMode();
                break;
            case 100:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                Integer index2 = (Integer) ar2.result;
                log("handleMessage (EVENT_ICC_RECOVERY) , index = " + index2);
                if (index2 == this.mPhoneId) {
                    log("mRecoveryRegistrants notify");
                    this.mRecoveryRegistrants.notifyRegistrants();
                }
                break;
            case 101:
                this.mFdnChangedRegistrants.notifyRegistrants();
                break;
            case EVENT_ICC_RECORD_EVENTS:
                if (this.mCurrentAppType == 1 && this.mIccRecords != null) {
                    AsyncResult ar3 = (AsyncResult) msg.obj;
                    int eventCode = ((Integer) ar3.result).intValue();
                    if (eventCode == 2) {
                        this.mTelephonyManager.setSimOperatorNameForPhone(this.mPhoneId.intValue(), this.mIccRecords.getServiceProviderName());
                    }
                    break;
                }
                break;
            case EVENT_SUBSCRIPTION_ACTIVATED:
                log("EVENT_SUBSCRIPTION_ACTIVATED");
                onSubscriptionActivated();
                break;
            case EVENT_SUBSCRIPTION_DEACTIVATED:
                log("EVENT_SUBSCRIPTION_DEACTIVATED");
                onSubscriptionDeactivated();
                break;
            case EVENT_CARRIER_PRIVILIGES_LOADED:
                log("EVENT_CARRIER_PRIVILEGES_LOADED");
                if (this.mUiccCard != null) {
                    this.mUiccCard.unregisterForCarrierPrivilegeRulesLoaded(this);
                }
                onRecordsLoaded();
                break;
            default:
                loge("Unhandled message with number: " + msg.what);
                break;
        }
    }

    private void onSubscriptionActivated() {
        updateIccAvailability();
        updateStateProperty();
    }

    private void onSubscriptionDeactivated() {
        resetProperties();
        updateIccAvailability();
        updateStateProperty();
    }

    private void onRecordsLoaded() {
        broadcastInternalIccStateChangedIntent("LOADED", null);
    }

    private void updateIccAvailability() {
        synchronized (this.mLock) {
            UiccCard newCard = this.mUiccController.getUiccCard(this.mPhoneId.intValue());
            IccCardStatus.CardState cardState = IccCardStatus.CardState.CARDSTATE_ABSENT;
            UiccCardApplication newApp = null;
            IccRecords newRecords = null;
            if (newCard != null) {
                newCard.getCardState();
                newApp = newCard.getApplication(this.mCurrentAppType);
                if (newApp != null) {
                    newRecords = newApp.getIccRecords();
                }
            }
            if (this.mIccRecords != newRecords || this.mUiccApplication != newApp || this.mUiccCard != newCard) {
                log("Icc changed. Reregestering.");
                unregisterUiccCardEvents();
                this.mUiccCard = newCard;
                this.mUiccApplication = newApp;
                this.mIccRecords = newRecords;
                registerUiccCardEvents();
            }
            updateExternalState();
        }
    }

    void resetProperties() {
        if (this.mCurrentAppType != 1) {
            return;
        }
        log("update icc_operator_numeric=");
        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mPhoneId.intValue(), "");
        this.mTelephonyManager.setSimCountryIsoForPhone(this.mPhoneId.intValue(), "");
        this.mTelephonyManager.setSimOperatorNameForPhone(this.mPhoneId.intValue(), "");
    }

    private void HandleDetectedState() {
    }

    private void updateExternalState() {
        if (this.mUiccCard == null) {
            log("updateExternalState, broadcast NOT_READY because UiccCard is null!");
            setExternalState(IccCardConstants.State.NOT_READY);
        }
        if (this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ABSENT) {
            log("updateExternalState, broadcast ABSENT because card state is absent!");
            setExternalState(IccCardConstants.State.ABSENT);
            return;
        }
        if (this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ERROR) {
            setExternalState(IccCardConstants.State.CARD_IO_ERROR);
            return;
        }
        if (this.mUiccApplication == null) {
            log("updateExternalState, broadcast NOT_READY because mUiccApplication is null!");
            setExternalState(IccCardConstants.State.NOT_READY);
            return;
        }
        switch (m475x37a84908()[this.mUiccApplication.getState().ordinal()]) {
            case 1:
                HandleDetectedState();
                break;
            case 2:
                setExternalState(IccCardConstants.State.PIN_REQUIRED);
                break;
            case 3:
                setExternalState(IccCardConstants.State.PUK_REQUIRED);
                break;
            case 4:
                setExternalState(IccCardConstants.State.READY);
                break;
            case 5:
                setExternalState(IccCardConstants.State.NETWORK_LOCKED);
                break;
            case 6:
                setExternalState(IccCardConstants.State.UNKNOWN);
                break;
            default:
                setExternalState(IccCardConstants.State.UNKNOWN);
                break;
        }
    }

    private void registerUiccCardEvents() {
        if (this.mUiccCard != null) {
            this.mUiccCard.registerForAbsent(this, 4, null);
        }
        if (this.mUiccApplication != null) {
            this.mUiccApplication.registerForReady(this, 6, null);
            this.mUiccApplication.registerForLocked(this, 5, null);
            this.mUiccApplication.registerForNetworkLocked(this, 9, null);
            this.mUiccApplication.registerForFdnChanged(this, 101, null);
        }
        if (this.mIccRecords == null) {
            return;
        }
        this.mIccRecords.registerForImsiReady(this, 8, null);
        this.mIccRecords.registerForRecordsLoaded(this, 7, null);
        this.mIccRecords.registerForRecordsEvents(this, EVENT_ICC_RECORD_EVENTS, null);
    }

    private void unregisterUiccCardEvents() {
        if (this.mUiccCard != null) {
            this.mUiccCard.unregisterForAbsent(this);
        }
        if (this.mUiccApplication != null) {
            this.mUiccApplication.unregisterForReady(this);
        }
        if (this.mUiccApplication != null) {
            this.mUiccApplication.unregisterForLocked(this);
        }
        if (this.mUiccApplication != null) {
            this.mUiccApplication.unregisterForNetworkLocked(this);
        }
        if (this.mIccRecords != null) {
            this.mIccRecords.unregisterForImsiReady(this);
        }
        if (this.mIccRecords != null) {
            this.mIccRecords.unregisterForRecordsLoaded(this);
        }
        if (this.mIccRecords != null) {
            this.mIccRecords.unregisterForRecordsEvents(this);
        }
    }

    private void updateStateProperty() {
        this.mTelephonyManager.setSimStateForPhone(this.mPhoneId.intValue(), getState().toString());
    }

    private void broadcastIccStateChangedIntent(String value, String reason) {
        synchronized (this.mLock) {
            if (this.mPhoneId == null || !SubscriptionManager.isValidSlotId(this.mPhoneId.intValue())) {
                loge("broadcastIccStateChangedIntent: mPhoneId=" + this.mPhoneId + " is invalid; Return!!");
                return;
            }
            if (this.mQuietMode) {
                log("broadcastIccStateChangedIntent: QuietMode NOT Broadcasting intent ACTION_SIM_STATE_CHANGED  value=" + value + " reason=" + reason);
                return;
            }
            Intent intent = new Intent("android.intent.action.SIM_STATE_CHANGED");
            intent.addFlags(67108864);
            intent.putExtra("phoneName", "Phone");
            intent.putExtra("ss", value);
            intent.putExtra("reason", reason);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhoneId.intValue());
            log("broadcastIccStateChangedIntent intent ACTION_SIM_STATE_CHANGED value=" + value + " reason=" + reason + " for mPhoneId=" + this.mPhoneId);
            ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
        }
    }

    private void broadcastInternalIccStateChangedIntent(String value, String reason) {
        synchronized (this.mLock) {
            if (this.mPhoneId == null) {
                loge("broadcastInternalIccStateChangedIntent: Card Index is not set; Return!!");
                return;
            }
            Intent intent = new Intent(ACTION_INTERNAL_SIM_STATE_CHANGED);
            intent.addFlags(67108864);
            intent.putExtra("phoneName", "Phone");
            intent.putExtra("ss", value);
            intent.putExtra("reason", reason);
            intent.putExtra("phone", this.mPhoneId);
            log("Sending intent ACTION_INTERNAL_SIM_STATE_CHANGED for mPhoneId : " + this.mPhoneId);
            sInternalIntent = intent;
            ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
        }
    }

    private void setExternalState(IccCardConstants.State newState, boolean override) {
        synchronized (this.mLock) {
            if (this.mPhoneId == null || !SubscriptionManager.isValidSlotId(this.mPhoneId.intValue())) {
                loge("setExternalState: mPhoneId=" + this.mPhoneId + " is invalid; Return!!");
                return;
            }
            log("setExternalState(): mExternalState = " + this.mExternalState + " newState =  " + newState + " override = " + override);
            if (!override && newState == this.mExternalState) {
                if (newState == IccCardConstants.State.NETWORK_LOCKED && this.mNetworkLockState != getNetworkPersoType()) {
                    log("NetworkLockState =  " + this.mNetworkLockState);
                } else {
                    loge("setExternalState: !override and newstate unchanged from " + newState);
                    return;
                }
            }
            this.mExternalState = newState;
            this.mNetworkLockState = getNetworkPersoType();
            loge("setExternalState: set mPhoneId=" + this.mPhoneId + " mExternalState=" + this.mExternalState);
            this.mTelephonyManager.setSimStateForPhone(this.mPhoneId.intValue(), getState().toString());
            if ("LOCKED".equals(getIccStateIntentString(this.mExternalState))) {
                broadcastInternalIccStateChangedIntent(getIccStateIntentString(this.mExternalState), getIccStateReason(this.mExternalState));
            } else {
                broadcastIccStateChangedIntent(getIccStateIntentString(this.mExternalState), getIccStateReason(this.mExternalState));
            }
            if (IccCardConstants.State.ABSENT == this.mExternalState) {
                this.mAbsentRegistrants.notifyRegistrants();
            }
        }
    }

    private void processLockedState() {
        synchronized (this.mLock) {
            if (this.mUiccApplication == null) {
                return;
            }
            IccCardStatus.PinState pin1State = this.mUiccApplication.getPin1State();
            if (pin1State == IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                setExternalState(IccCardConstants.State.PERM_DISABLED);
                return;
            }
            IccCardApplicationStatus.AppState appState = this.mUiccApplication.getState();
            switch (m475x37a84908()[appState.ordinal()]) {
                case 2:
                    this.mPinLockedRegistrants.notifyRegistrants();
                    setExternalState(IccCardConstants.State.PIN_REQUIRED);
                    break;
                case 3:
                    setExternalState(IccCardConstants.State.PUK_REQUIRED);
                    break;
            }
        }
    }

    private void setExternalState(IccCardConstants.State newState) {
        if (newState == IccCardConstants.State.PIN_REQUIRED && this.mUiccApplication != null) {
            IccCardStatus.PinState pin1State = this.mUiccApplication.getPin1State();
            if (pin1State == IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                log("setExternalState(): PERM_DISABLED");
                setExternalState(IccCardConstants.State.PERM_DISABLED);
                return;
            }
        }
        setExternalState(newState, false);
    }

    public boolean getIccRecordsLoaded() {
        synchronized (this.mLock) {
            if (this.mIccRecords != null) {
                return this.mIccRecords.getRecordsLoaded();
            }
            return false;
        }
    }

    private String getIccStateIntentString(IccCardConstants.State state) {
        switch (m474xf663cf59()[state.ordinal()]) {
            case 1:
                return "ABSENT";
            case 2:
                return "CARD_IO_ERROR";
            case 3:
                return "LOCKED";
            case 4:
                return "NOT_READY";
            case 5:
                return "LOCKED";
            case 6:
                return "LOCKED";
            case 7:
                return "LOCKED";
            case 8:
                return "READY";
            default:
                return "UNKNOWN";
        }
    }

    private java.lang.String getIccStateReason(com.android.internal.telephony.IccCardConstants.State r4) {
        switch (m474xf663cf59()[r4.ordinal()]) {
            case 3:
                switch (m476x5ed1affa()[r3.mUiccApplication.getPersoSubState().ordinal()]) {
                }
        }
        return null;
    }

    @Override
    public IccCardConstants.State getState() {
        IccCardConstants.State state;
        synchronized (this.mLock) {
            state = this.mExternalState;
        }
        return state;
    }

    @Override
    public IccRecords getIccRecords() {
        IccRecords iccRecords;
        synchronized (this.mLock) {
            iccRecords = this.mIccRecords;
        }
        return iccRecords;
    }

    @Override
    public IccFileHandler getIccFileHandler() {
        synchronized (this.mLock) {
            if (this.mUiccApplication == null) {
                return null;
            }
            return this.mUiccApplication.getIccFileHandler();
        }
    }

    @Override
    public void registerForAbsent(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mAbsentRegistrants.add(r);
            if (getState() == IccCardConstants.State.ABSENT) {
                r.notifyRegistrant();
            }
        }
    }

    @Override
    public void unregisterForAbsent(Handler h) {
        synchronized (this.mLock) {
            this.mAbsentRegistrants.remove(h);
        }
    }

    @Override
    public void registerForNetworkLocked(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mNetworkLockedRegistrants.add(r);
            if (getState() == IccCardConstants.State.NETWORK_LOCKED) {
                r.notifyRegistrant();
            }
        }
    }

    @Override
    public void unregisterForNetworkLocked(Handler h) {
        synchronized (this.mLock) {
            this.mNetworkLockedRegistrants.remove(h);
        }
    }

    @Override
    public void registerForLocked(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mPinLockedRegistrants.add(r);
            if (getState().isPinLocked()) {
                r.notifyRegistrant();
            }
        }
    }

    @Override
    public void unregisterForLocked(Handler h) {
        synchronized (this.mLock) {
            this.mPinLockedRegistrants.remove(h);
        }
    }

    @Override
    public void supplyPin(String pin, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPin(pin, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to supplyPin, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void supplyPuk(String puk, String newPin, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPuk(puk, newPin, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to supplyPuk, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void supplyPin2(String pin2, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPin2(pin2, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to supplyPin2, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void supplyPuk2(String puk2, String newPin2, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPuk2(puk2, newPin2, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to supplyPuk2, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void supplyNetworkDepersonalization(String pin, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyNetworkDepersonalization(pin, onComplete);
            } else if (onComplete != null) {
                Exception e = new RuntimeException("CommandsInterface is not set.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public boolean getIccLockEnabled() {
        boolean zBooleanValue;
        synchronized (this.mLock) {
            Boolean retValue = Boolean.valueOf(this.mUiccApplication != null ? this.mUiccApplication.getIccLockEnabled() : false);
            zBooleanValue = retValue.booleanValue();
        }
        return zBooleanValue;
    }

    @Override
    public boolean getIccFdnEnabled() {
        boolean zBooleanValue;
        synchronized (this.mLock) {
            Boolean retValue = Boolean.valueOf(this.mUiccApplication != null ? this.mUiccApplication.getIccFdnEnabled() : false);
            zBooleanValue = retValue.booleanValue();
        }
        return zBooleanValue;
    }

    @Override
    public boolean getIccFdnAvailable() {
        if (this.mUiccApplication == null) {
            return false;
        }
        boolean retValue = this.mUiccApplication.getIccFdnAvailable();
        return retValue;
    }

    @Override
    public boolean getIccPin2Blocked() {
        Boolean retValue = Boolean.valueOf(this.mUiccApplication != null ? this.mUiccApplication.getIccPin2Blocked() : false);
        return retValue.booleanValue();
    }

    @Override
    public boolean getIccPuk2Blocked() {
        Boolean retValue = Boolean.valueOf(this.mUiccApplication != null ? this.mUiccApplication.getIccPuk2Blocked() : false);
        return retValue.booleanValue();
    }

    @Override
    public void setIccLockEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.setIccLockEnabled(enabled, password, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to setIccLockEnabled, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void setIccFdnEnabled(boolean enabled, String password, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.setIccFdnEnabled(enabled, password, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to setIccFdnEnabled, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void changeIccLockPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.changeIccLockPassword(oldPassword, newPassword, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to changeIccLockPassword, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void changeIccFdnPassword(String oldPassword, String newPassword, Message onComplete) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.changeIccFdnPassword(oldPassword, newPassword, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to changeIccFdnPassword, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public String getServiceProviderName() {
        synchronized (this.mLock) {
            if (this.mIccRecords == null) {
                return null;
            }
            return this.mIccRecords.getServiceProviderName();
        }
    }

    @Override
    public boolean isApplicationOnIcc(IccCardApplicationStatus.AppType type) {
        boolean zBooleanValue;
        synchronized (this.mLock) {
            Boolean retValue = Boolean.valueOf(this.mUiccCard != null ? this.mUiccCard.isApplicationOnIcc(type) : false);
            zBooleanValue = retValue.booleanValue();
        }
        return zBooleanValue;
    }

    @Override
    public boolean hasIccCard() {
        boolean isSimInsert;
        synchronized (this.mLock) {
            isSimInsert = false;
            String iccId = SystemProperties.get(this.PROPERTY_ICCID_SIM[this.mPhoneId.intValue()]);
            if (iccId != null && !iccId.equals("") && !iccId.equals(ICCID_STRING_FOR_NO_SIM)) {
                isSimInsert = true;
            }
            if (!isSimInsert && this.mUiccCard != null && this.mUiccCard.getCardState() != IccCardStatus.CardState.CARDSTATE_ABSENT) {
                isSimInsert = true;
            }
            log("hasIccCard(): isSimInsert =  " + isSimInsert + " ,CardState = " + (this.mUiccCard != null ? this.mUiccCard.getCardState() : "") + ", iccId = " + SubscriptionInfo.givePrintableIccid(iccId));
        }
        return isSimInsert;
    }

    private void setSystemProperty(String property, String value) {
        TelephonyManager.setTelephonyProperty(this.mPhoneId.intValue(), property, value);
    }

    public IccRecords getIccRecord() {
        return this.mIccRecords;
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s + " (slot " + this.mPhoneId + ")");
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg + " (slot " + this.mPhoneId + ")");
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("IccCardProxy: " + this);
        pw.println(" mContext=" + this.mContext);
        pw.println(" mCi=" + this.mCi);
        pw.println(" mAbsentRegistrants: size=" + this.mAbsentRegistrants.size());
        for (int i = 0; i < this.mAbsentRegistrants.size(); i++) {
            pw.println("  mAbsentRegistrants[" + i + "]=" + ((Registrant) this.mAbsentRegistrants.get(i)).getHandler());
        }
        pw.println(" mPinLockedRegistrants: size=" + this.mPinLockedRegistrants.size());
        for (int i2 = 0; i2 < this.mPinLockedRegistrants.size(); i2++) {
            pw.println("  mPinLockedRegistrants[" + i2 + "]=" + ((Registrant) this.mPinLockedRegistrants.get(i2)).getHandler());
        }
        pw.println(" mNetworkLockedRegistrants: size=" + this.mNetworkLockedRegistrants.size());
        for (int i3 = 0; i3 < this.mNetworkLockedRegistrants.size(); i3++) {
            pw.println("  mNetworkLockedRegistrants[" + i3 + "]=" + ((Registrant) this.mNetworkLockedRegistrants.get(i3)).getHandler());
        }
        pw.println(" mCurrentAppType=" + this.mCurrentAppType);
        pw.println(" mUiccController=" + this.mUiccController);
        pw.println(" mUiccCard=" + this.mUiccCard);
        pw.println(" mUiccApplication=" + this.mUiccApplication);
        pw.println(" mIccRecords=" + this.mIccRecords);
        pw.println(" mCdmaSSM=" + this.mCdmaSSM);
        pw.println(" mRadioOn=" + this.mRadioOn);
        pw.println(" mQuietMode=" + this.mQuietMode);
        pw.println(" mInitialized=" + this.mInitialized);
        pw.println(" mExternalState=" + this.mExternalState);
        pw.flush();
    }

    @Override
    public IccCardApplicationStatus.PersoSubState getNetworkPersoType() {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                return this.mUiccApplication.getPersoSubState();
            }
            return IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_UNKNOWN;
        }
    }

    @Override
    public void queryIccNetworkLock(int category, Message onComplete) {
        log("queryIccNetworkLock(): category =  " + category);
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.queryIccNetworkLock(category, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to queryIccNetworkLock, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void setIccNetworkLockEnabled(int category, int lockop, String password, String data_imsi, String gid1, String gid2, Message onComplete) {
        log("SetIccNetworkEnabled(): category = " + category + " lockop = " + lockop + " password = " + password + " data_imsi = " + data_imsi + " gid1 = " + gid1 + " gid2 = " + gid2);
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.setIccNetworkLockEnabled(category, lockop, password, data_imsi, gid1, gid2, onComplete);
            } else if (onComplete != null) {
                Exception e = CommandException.fromRilErrno(1);
                log("Fail to setIccNetworkLockEnabled, hasIccCard = " + hasIccCard());
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public void repollIccStateForModemSmlChangeFeatrue(boolean needIntent) {
        log("repollIccStateForModemSmlChangeFeatrue, needIntent = " + needIntent);
        synchronized (this.mLock) {
            this.mUiccController.repollIccStateForModemSmlChangeFeatrue(this.mPhoneId.intValue(), needIntent);
        }
    }

    @Override
    public String getIccCardType() {
        synchronized (this.mLock) {
            if (this.mUiccCard != null && this.mUiccCard.getCardState() != IccCardStatus.CardState.CARDSTATE_ABSENT) {
                return this.mUiccCard.getIccCardType();
            }
            return "";
        }
    }

    public void registerForRecovery(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            Registrant r = new Registrant(h, what, obj);
            this.mRecoveryRegistrants.add(r);
            if (getState() == IccCardConstants.State.READY) {
                r.notifyRegistrant();
            }
        }
    }

    public void unregisterForRecovery(Handler h) {
        synchronized (this.mLock) {
            this.mRecoveryRegistrants.remove(h);
        }
    }

    @Override
    public void registerForFdnChanged(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            synchronized (this.mLock) {
                Registrant r = new Registrant(h, what, obj);
                this.mFdnChangedRegistrants.add(r);
                if (getIccFdnEnabled()) {
                    r.notifyRegistrant();
                }
            }
        }
    }

    @Override
    public void unregisterForFdnChanged(Handler h) {
        synchronized (this.mLock) {
            this.mFdnChangedRegistrants.remove(h);
        }
    }

    public boolean isCdmaOnly() {
        String[] values = null;
        if (this.mPhoneId.intValue() < 0 || this.mPhoneId.intValue() >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            log("isCdmaOnly: invalid PhoneId " + this.mPhoneId);
            return false;
        }
        String prop = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[this.mPhoneId.intValue()]);
        if (prop != null && prop.length() > 0) {
            values = prop.split(",");
        }
        log("isCdmaOnly PhoneId " + this.mPhoneId + ", prop value= " + prop + ", size= " + (values != null ? values.length : 0));
        return (values == null || Arrays.asList(values).contains("USIM") || Arrays.asList(values).contains("SIM")) ? false : true;
    }
}
