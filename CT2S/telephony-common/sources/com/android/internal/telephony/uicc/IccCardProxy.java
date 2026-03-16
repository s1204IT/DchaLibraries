package com.android.internal.telephony.uicc;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class IccCardProxy extends Handler implements IccCard {
    private static final String ACTION_APN_CHANGE = "ApnMonitor.action.ApnChange";
    public static final String ACTION_INTERNAL_SIM_STATE_CHANGED = "android.intent.action.internal_sim_state_changed";
    private static final ComponentName APNSYNC_COMPONENT = new ComponentName("com.marvell.telephony", "com.marvell.telephony.ApnMonitor");
    private static final boolean DBG = true;
    private static final int EVENT_APP_READY = 6;
    private static final int EVENT_CARRIER_PRIVILIGES_LOADED = 503;
    private static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 11;
    private static final int EVENT_ICC_ABSENT = 4;
    private static final int EVENT_ICC_CHANGED = 3;
    private static final int EVENT_ICC_LOCKED = 5;
    private static final int EVENT_ICC_PLUG = 12;
    private static final int EVENT_ICC_RECORD_EVENTS = 500;
    private static final int EVENT_IMSI_READY = 8;
    private static final int EVENT_NETWORK_LOCKED = 9;
    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 1;
    private static final int EVENT_RADIO_ON = 2;
    private static final int EVENT_RECORDS_LOADED = 7;
    private static final int EVENT_SUBSCRIPTION_ACTIVATED = 501;
    private static final int EVENT_SUBSCRIPTION_DEACTIVATED = 502;
    private static final String LOG_TAG = "IccCardProxy";
    private CdmaSubscriptionSourceManager mCdmaSSM;
    private CommandsInterface mCi;
    private Context mContext;
    private Integer mPhoneId;
    private TelephonyManager mTelephonyManager;
    private UiccController mUiccController;
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
        ci.registerForOn(this, 2, null);
        ci.registerForOffOrNotAvailable(this, 1, null);
        resetProperties();
        setExternalState(IccCardConstants.State.NOT_READY, false);
    }

    public void dispose() {
        synchronized (this.mLock) {
            log("Disposing");
            this.mUiccController.unregisterForIccChanged(this);
            this.mUiccController = null;
            this.mCi.unregisterForOn(this);
            this.mCi.unregisterForOffOrNotAvailable(this);
            this.mCdmaSSM.dispose(this);
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
        boolean newQuietMode = false;
        synchronized (this.mLock) {
            boolean z = this.mQuietMode;
            int cdmaSource = -1;
            boolean isLteOnCdmaMode = TelephonyManager.getLteOnCdmaModeStatic() == 1;
            if (this.mCurrentAppType == 1) {
                newQuietMode = false;
                log("updateQuietMode: 3GPP subscription -> newQuietMode=false");
            } else {
                if (isLteOnCdmaMode) {
                    log("updateQuietMode: is cdma/lte device, force IccCardProxy into 3gpp mode");
                    this.mCurrentAppType = 1;
                }
                cdmaSource = this.mCdmaSSM != null ? this.mCdmaSSM.getCdmaSubscriptionSource() : -1;
                if (cdmaSource == 1 && this.mCurrentAppType == 2 && !isLteOnCdmaMode) {
                    newQuietMode = true;
                }
                log("updateQuietMode: cdmaSource=" + cdmaSource + " mCurrentAppType=" + this.mCurrentAppType + " isLteOnCdmaMode=" + isLteOnCdmaMode + " newQuietMode=" + newQuietMode);
            }
            if (!this.mQuietMode && newQuietMode) {
                log("Switching to QuietMode.");
                setExternalState(IccCardConstants.State.READY);
                this.mQuietMode = newQuietMode;
            } else if (this.mQuietMode && !newQuietMode) {
                log("updateQuietMode: Switching out from QuietMode. Force broadcast of current state=" + this.mExternalState);
                this.mQuietMode = newQuietMode;
                setExternalState(this.mExternalState, true);
            } else {
                log("updateQuietMode: no changes don't setExternalState");
            }
            log("updateQuietMode: QuietMode is " + this.mQuietMode + " (app_type=" + this.mCurrentAppType + " isLteOnCdmaMode=" + isLteOnCdmaMode + " cdmaSource=" + cdmaSource + ")");
            this.mInitialized = true;
            sendMessage(obtainMessage(3));
        }
    }

    @Override
    public void handleMessage(Message msg) {
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
                    updateIccAvailability();
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
                        log("update icc_operator_numeric=" + operator);
                        this.mTelephonyManager.setSimOperatorNumericForPhone(this.mPhoneId.intValue(), operator);
                        String countryCode = operator.substring(0, 3);
                        if (countryCode != null) {
                            this.mTelephonyManager.setSimCountryIsoForPhone(this.mPhoneId.intValue(), MccTable.countryCodeForMcc(Integer.parseInt(countryCode)));
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
                Intent intent = new Intent(ACTION_APN_CHANGE);
                intent.setComponent(APNSYNC_COMPONENT);
                IActivityManager am = ActivityManagerNative.getDefault();
                if (am != null) {
                    try {
                        am.startService((IApplicationThread) null, intent, (String) null, 0);
                    } catch (Exception e) {
                        loge("Exception when start ApnMonitor service " + e);
                    }
                }
                broadcastIccStateChangedIntent("IMSI", null);
                break;
            case 9:
                this.mNetworkLockedRegistrants.notifyRegistrants();
                setExternalState(IccCardConstants.State.NETWORK_LOCKED);
                break;
            case 11:
                updateQuietMode();
                break;
            case 12:
                AsyncResult ar = (AsyncResult) msg.obj;
                boolean simTrayPlug = ((Boolean) ar.result).booleanValue();
                broadcastIccPlugIntent(simTrayPlug);
                if (simTrayPlug) {
                    setExternalState(IccCardConstants.State.UNKNOWN);
                }
                break;
            case EVENT_ICC_RECORD_EVENTS:
                if (this.mCurrentAppType == 1 && this.mIccRecords != null) {
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    int eventCode = ((Integer) ar2.result).intValue();
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
        if (this.mCurrentAppType == 1) {
            log("update icc_operator_numeric=");
            this.mTelephonyManager.setSimOperatorNumericForPhone(this.mPhoneId.intValue(), "");
            this.mTelephonyManager.setSimCountryIsoForPhone(this.mPhoneId.intValue(), "");
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mPhoneId.intValue(), "");
        }
    }

    private void HandleDetectedState() {
    }

    private void updateExternalState() {
        if (this.mUiccCard == null || this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ABSENT) {
            if (this.mUiccCard != null && this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ABSENT) {
                setExternalState(IccCardConstants.State.ABSENT);
            } else {
                setExternalState(IccCardConstants.State.NOT_READY);
                return;
            }
        }
        if (this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ERROR) {
            setExternalState(IccCardConstants.State.CARD_IO_ERROR);
            return;
        }
        if (this.mUiccApplication == null) {
            setExternalState(IccCardConstants.State.NOT_READY);
            return;
        }
        switch (this.mUiccApplication.getState()) {
            case APPSTATE_UNKNOWN:
                setExternalState(IccCardConstants.State.UNKNOWN);
                break;
            case APPSTATE_DETECTED:
                HandleDetectedState();
                break;
            case APPSTATE_PIN:
                setExternalState(IccCardConstants.State.PIN_REQUIRED);
                break;
            case APPSTATE_PUK:
                setExternalState(IccCardConstants.State.PUK_REQUIRED);
                break;
            case APPSTATE_SUBSCRIPTION_PERSO:
                if (this.mUiccApplication.getPersoSubState() == IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK) {
                    setExternalState(IccCardConstants.State.NETWORK_LOCKED);
                } else {
                    setExternalState(IccCardConstants.State.UNKNOWN);
                }
                break;
            case APPSTATE_READY:
                setExternalState(IccCardConstants.State.READY);
                break;
        }
    }

    private void registerUiccCardEvents() {
        if (this.mUiccCard != null) {
            this.mUiccCard.registerForAbsent(this, 4, null);
            this.mUiccCard.registerForPlug(this, 12, null);
        }
        if (this.mUiccApplication != null) {
            this.mUiccApplication.registerForReady(this, 6, null);
            this.mUiccApplication.registerForLocked(this, 5, null);
            this.mUiccApplication.registerForNetworkLocked(this, 9, null);
        }
        if (this.mIccRecords != null) {
            this.mIccRecords.registerForImsiReady(this, 8, null);
            this.mIccRecords.registerForRecordsLoaded(this, 7, null);
            this.mIccRecords.registerForRecordsEvents(this, EVENT_ICC_RECORD_EVENTS, null);
        }
    }

    private void unregisterUiccCardEvents() {
        if (this.mUiccCard != null) {
            this.mUiccCard.unregisterForAbsent(this);
            this.mUiccCard.unregisterForPlug(this);
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

    private void broadcastIccPlugIntent(boolean simTrayPlug) {
        if (this.mPhoneId == null) {
            loge("broadcastIccStateChangedIntent: mPhoneId is not set; Return!!");
            return;
        }
        Intent intent = new Intent("android.intent.action.SIM_HOT_PLUGGED");
        intent.putExtra("slot", this.mPhoneId);
        intent.putExtra("simTrayPlug", simTrayPlug);
        log("Broadcasting intent ACTION_SIM_HOT_PLUGGED");
        ActivityManagerNative.broadcastStickyIntent(intent, "android.permission.READ_PHONE_STATE", -1);
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
            intent.addFlags(603979776);
            intent.putExtra("phoneName", "Phone");
            intent.putExtra("ss", value);
            intent.putExtra("reason", reason);
            intent.putExtra("phone", this.mPhoneId);
            log("Sending intent ACTION_INTERNAL_SIM_STATE_CHANGED for mPhoneId : " + this.mPhoneId);
            ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
        }
    }

    private void setExternalState(IccCardConstants.State newState, boolean override) {
        synchronized (this.mLock) {
            if (this.mPhoneId == null || !SubscriptionManager.isValidSlotId(this.mPhoneId.intValue())) {
                loge("setExternalState: mPhoneId=" + this.mPhoneId + " is invalid; Return!!");
                return;
            }
            if (!override && newState == this.mExternalState) {
                loge("setExternalState: !override and newstate unchanged from " + newState);
                return;
            }
            this.mExternalState = newState;
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
            if (this.mUiccApplication != null) {
                IccCardStatus.PinState pin1State = this.mUiccApplication.getPin1State();
                if (pin1State == IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
                    setExternalState(IccCardConstants.State.PERM_DISABLED);
                    return;
                }
                IccCardApplicationStatus.AppState appState = this.mUiccApplication.getState();
                switch (appState) {
                    case APPSTATE_PIN:
                        this.mPinLockedRegistrants.notifyRegistrants();
                        setExternalState(IccCardConstants.State.PIN_REQUIRED);
                        break;
                    case APPSTATE_PUK:
                        setExternalState(IccCardConstants.State.PUK_REQUIRED);
                        break;
                }
            }
        }
    }

    private void setExternalState(IccCardConstants.State newState) {
        setExternalState(newState, false);
    }

    public boolean getIccRecordsLoaded() {
        boolean recordsLoaded;
        synchronized (this.mLock) {
            recordsLoaded = this.mIccRecords != null ? this.mIccRecords.getRecordsLoaded() : false;
        }
        return recordsLoaded;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.ABSENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.READY.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.NOT_READY.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PERM_DISABLED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.CARD_IO_ERROR.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState = new int[IccCardApplicationStatus.AppState.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN.ordinal()] = 1;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[IccCardApplicationStatus.AppState.APPSTATE_DETECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[IccCardApplicationStatus.AppState.APPSTATE_PIN.ordinal()] = 3;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[IccCardApplicationStatus.AppState.APPSTATE_PUK.ordinal()] = 4;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO.ordinal()] = 5;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[IccCardApplicationStatus.AppState.APPSTATE_READY.ordinal()] = 6;
            } catch (NoSuchFieldError e14) {
            }
        }
    }

    private String getIccStateIntentString(IccCardConstants.State state) {
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
            case 1:
                return "ABSENT";
            case 2:
                return "LOCKED";
            case 3:
                return "LOCKED";
            case 4:
                return "LOCKED";
            case 5:
                return "READY";
            case 6:
                return "NOT_READY";
            case 7:
                return "LOCKED";
            case 8:
                return "CARD_IO_ERROR";
            default:
                return "UNKNOWN";
        }
    }

    private String getIccStateReason(IccCardConstants.State state) {
        switch (AnonymousClass1.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
            case 2:
                return "PIN";
            case 3:
                return "PUK";
            case 4:
                return "NETWORK";
            case 5:
            case 6:
            default:
                return null;
            case 7:
                return "PERM_DISABLED";
            case 8:
                return "CARD_IO_ERROR";
        }
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
        IccFileHandler iccFileHandler;
        synchronized (this.mLock) {
            iccFileHandler = this.mUiccApplication != null ? this.mUiccApplication.getIccFileHandler() : null;
        }
        return iccFileHandler;
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
                Exception e = new RuntimeException("ICC card is absent.");
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
                Exception e = new RuntimeException("ICC card is absent.");
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
                Exception e = new RuntimeException("ICC card is absent.");
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
                Exception e = new RuntimeException("ICC card is absent.");
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
                Exception e = new RuntimeException("ICC card is absent.");
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
                Exception e = new RuntimeException("ICC card is absent.");
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
                Exception e = new RuntimeException("ICC card is absent.");
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
                Exception e = new RuntimeException("ICC card is absent.");
                AsyncResult.forMessage(onComplete).exception = e;
                onComplete.sendToTarget();
            }
        }
    }

    @Override
    public String getServiceProviderName() {
        String serviceProviderName;
        synchronized (this.mLock) {
            serviceProviderName = this.mIccRecords != null ? this.mIccRecords.getServiceProviderName() : null;
        }
        return serviceProviderName;
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
        boolean z;
        synchronized (this.mLock) {
            z = (this.mUiccCard == null || this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ABSENT) ? false : true;
        }
        return z;
    }

    private void setSystemProperty(String property, String value) {
        TelephonyManager.setTelephonyProperty(this.mPhoneId.intValue(), property, value);
    }

    public IccRecords getIccRecord() {
        return this.mIccRecords;
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
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
}
