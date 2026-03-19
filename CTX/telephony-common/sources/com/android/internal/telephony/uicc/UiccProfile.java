package com.android.internal.telephony.uicc;

import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccCardStatus;
import com.android.internal.telephony.uicc.euicc.EuiccCard;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UiccProfile extends IccCard {
    protected static final boolean DBG = true;

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public static final int EVENT_APP_READY = 3;
    private static final int EVENT_CARRIER_CONFIG_CHANGED = 14;
    private static final int EVENT_CARRIER_PRIVILEGES_LOADED = 13;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 9;
    private static final int EVENT_EID_READY = 6;
    private static final int EVENT_ICC_LOCKED = 2;
    private static final int EVENT_ICC_RECORD_EVENTS = 7;
    private static final int EVENT_NETWORK_LOCKED = 5;
    private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 8;
    private static final int EVENT_RADIO_OFF_OR_UNAVAILABLE = 1;
    private static final int EVENT_RECORDS_LOADED = 4;
    private static final int EVENT_SIM_IO_DONE = 12;
    private static final int EVENT_TRANSMIT_APDU_BASIC_CHANNEL_DONE = 11;
    private static final int EVENT_TRANSMIT_APDU_LOGICAL_CHANNEL_DONE = 10;
    protected static final String LOG_TAG = "UiccProfile";
    private static final String OPERATOR_BRAND_OVERRIDE_PREFIX = "operator_branding_";
    private static final boolean VDBG = false;
    private UiccCarrierPrivilegeRules mCarrierPrivilegeRules;
    private CatService mCatService;
    private int mCdmaSubscriptionAppIndex;
    protected CommandsInterface mCi;
    protected Context mContext;
    private int mGsmUmtsSubscriptionAppIndex;
    private int mImsSubscriptionAppIndex;
    protected final Object mLock;
    private final int mPhoneId;
    protected TelephonyManager mTelephonyManager;
    protected final UiccCard mUiccCard;
    private IccCardStatus.PinState mUniversalPinState;
    private UiccCardApplication[] mUiccApplications = new UiccCardApplication[8];
    protected boolean mDisposed = false;
    private RegistrantList mCarrierPrivilegeRegistrants = new RegistrantList();
    private RegistrantList mOperatorBrandOverrideRegistrants = new RegistrantList();
    private RegistrantList mNetworkLockedRegistrants = new RegistrantList();
    protected int mCurrentAppType = 1;
    protected UiccCardApplication mUiccApplication = null;
    protected IccRecords mIccRecords = null;
    protected IccCardConstants.State mExternalState = IccCardConstants.State.UNKNOWN;
    private final ContentObserver mProvisionCompleteContentObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z) {
            UiccProfile.this.mContext.getContentResolver().unregisterContentObserver(this);
            Iterator it = UiccProfile.this.getUninstalledCarrierPackages().iterator();
            while (it.hasNext()) {
                InstallCarrierAppUtils.showNotification(UiccProfile.this.mContext, (String) it.next());
                InstallCarrierAppUtils.registerPackageInstallReceiver(UiccProfile.this.mContext);
            }
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                UiccProfile.this.mHandler.sendMessage(UiccProfile.this.mHandler.obtainMessage(14));
            }
        }
    };

    @VisibleForTesting
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (UiccProfile.this.mDisposed && message.what != 8 && message.what != 9 && message.what != 10 && message.what != 11 && message.what != 12) {
                UiccProfile.this.loge("handleMessage: Received " + message.what + " after dispose(); ignoring the message");
                return;
            }
            UiccProfile.this.loglocal("handleMessage: Received " + message.what + " for phoneId " + UiccProfile.this.mPhoneId);
            switch (message.what) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 6:
                    break;
                case 5:
                    UiccProfile.this.mNetworkLockedRegistrants.notifyRegistrants();
                    break;
                case 7:
                    if (UiccProfile.this.mCurrentAppType == 1 && UiccProfile.this.mIccRecords != null && ((Integer) ((AsyncResult) message.obj).result).intValue() == 2) {
                        UiccProfile.this.mTelephonyManager.setSimOperatorNameForPhone(UiccProfile.this.mPhoneId, UiccProfile.this.mIccRecords.getServiceProviderName());
                        return;
                    }
                    return;
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        UiccProfile.this.loglocal("handleMessage: Exception " + asyncResult.exception);
                        UiccProfile.this.log("handleMessage: Error in SIM access with exception" + asyncResult.exception);
                    }
                    AsyncResult.forMessage((Message) asyncResult.userObj, asyncResult.result, asyncResult.exception);
                    ((Message) asyncResult.userObj).sendToTarget();
                    return;
                case 13:
                    UiccProfile.this.onCarrierPrivilegesLoadedMessage();
                    UiccProfile.this.updateExternalState();
                    return;
                case 14:
                    UiccProfile.this.handleCarrierNameOverride();
                    return;
                default:
                    UiccProfile.this.loge("handleMessage: Unhandled message with number: " + message.what);
                    return;
            }
            UiccProfile.this.updateExternalState();
        }
    };

    public UiccProfile(Context context, CommandsInterface commandsInterface, IccCardStatus iccCardStatus, int i, UiccCard uiccCard, Object obj) {
        log("Creating profile");
        this.mLock = obj;
        this.mUiccCard = uiccCard;
        this.mPhoneId = i;
        Phone phone = PhoneFactory.getPhone(i);
        if (phone != null) {
            setCurrentAppType(phone.getPhoneType() == 1);
        }
        if (this.mUiccCard instanceof EuiccCard) {
            ((EuiccCard) this.mUiccCard).registerForEidReady(this.mHandler, 6, null);
        }
        update(context, commandsInterface, iccCardStatus);
        commandsInterface.registerForOffOrNotAvailable(this.mHandler, 1, null);
        resetProperties();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(this.mReceiver, intentFilter);
    }

    public void dispose() {
        log("Disposing profile");
        if (this.mUiccCard instanceof EuiccCard) {
            ((EuiccCard) this.mUiccCard).unregisterForEidReady(this.mHandler);
        }
        synchronized (this.mLock) {
            unregisterAllAppEvents();
            unregisterCurrAppEvents();
            InstallCarrierAppUtils.hideAllNotifications(this.mContext);
            InstallCarrierAppUtils.unregisterPackageInstallReceiver(this.mContext);
            this.mCi.unregisterForOffOrNotAvailable(this.mHandler);
            this.mContext.unregisterReceiver(this.mReceiver);
            if (this.mCatService != null) {
                this.mCatService.dispose();
            }
            for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
                if (uiccCardApplication != null) {
                    uiccCardApplication.dispose();
                }
            }
            this.mCatService = null;
            this.mUiccApplications = null;
            this.mCarrierPrivilegeRules = null;
            this.mDisposed = true;
        }
    }

    public void setVoiceRadioTech(int i) {
        synchronized (this.mLock) {
            log("Setting radio tech " + ServiceState.rilRadioTechnologyToString(i));
            setCurrentAppType(ServiceState.isGsm(i));
            updateIccAvailability(false);
        }
    }

    protected void setCurrentAppType(boolean z) {
        boolean z2;
        synchronized (this.mLock) {
            if (TelephonyManager.getLteOnCdmaModeStatic() != 1) {
                z2 = false;
            } else {
                z2 = true;
            }
            if (z || z2) {
                this.mCurrentAppType = 1;
            } else {
                this.mCurrentAppType = 2;
            }
        }
    }

    private void handleCarrierNameOverride() {
        SubscriptionController subscriptionController = SubscriptionController.getInstance();
        int subIdUsingPhoneId = subscriptionController.getSubIdUsingPhoneId(this.mPhoneId);
        if (subIdUsingPhoneId == -1) {
            loge("subId not valid for Phone " + this.mPhoneId);
            return;
        }
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            loge("Failed to load a Carrier Config");
            return;
        }
        PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(subIdUsingPhoneId);
        boolean z = configForSubId.getBoolean("carrier_name_override_bool", false);
        String string = configForSubId.getString("carrier_name_string");
        if (z || (TextUtils.isEmpty(getServiceProviderName()) && !TextUtils.isEmpty(string))) {
            if (this.mIccRecords != null) {
                this.mIccRecords.setServiceProviderName(string);
            }
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mPhoneId, string);
            this.mOperatorBrandOverrideRegistrants.notifyRegistrants();
        }
        updateCarrierNameForSubscription(subscriptionController, subIdUsingPhoneId);
    }

    private void updateCarrierNameForSubscription(SubscriptionController subscriptionController, int i) {
        SubscriptionInfo activeSubscriptionInfo = subscriptionController.getActiveSubscriptionInfo(i, this.mContext.getOpPackageName());
        if (activeSubscriptionInfo == null || activeSubscriptionInfo.getNameSource() == 2) {
            return;
        }
        CharSequence displayName = activeSubscriptionInfo.getDisplayName();
        String subscriptionDisplayName = getSubscriptionDisplayName(i, this.mContext);
        if (!TextUtils.isEmpty(subscriptionDisplayName) && !subscriptionDisplayName.equals(displayName)) {
            log("sim name[" + this.mPhoneId + "] = " + subscriptionDisplayName);
            subscriptionController.setDisplayName(subscriptionDisplayName, i);
        }
    }

    private void updateIccAvailability(boolean z) {
        synchronized (this.mLock) {
            IccRecords iccRecords = null;
            UiccCardApplication application = getApplication(this.mCurrentAppType);
            if (application != null) {
                iccRecords = application.getIccRecords();
            }
            if (z) {
                unregisterAllAppEvents();
                registerAllAppEvents();
            }
            if (this.mIccRecords != iccRecords || this.mUiccApplication != application) {
                log("Icc changed. Reregistering.");
                unregisterCurrAppEvents();
                this.mUiccApplication = application;
                this.mIccRecords = iccRecords;
                registerCurrAppEvents();
            }
            updateExternalState();
        }
    }

    void resetProperties() {
        if (this.mCurrentAppType == 1) {
            log("update icc_operator_numeric=");
            this.mTelephonyManager.setSimOperatorNumericForPhone(this.mPhoneId, "");
            this.mTelephonyManager.setSimCountryIsoForPhone(this.mPhoneId, "");
            this.mTelephonyManager.setSimOperatorNameForPhone(this.mPhoneId, "");
        }
    }

    @VisibleForTesting(visibility = VisibleForTesting.Visibility.PRIVATE)
    public void updateExternalState() {
        if (this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_ERROR) {
            setExternalState(IccCardConstants.State.CARD_IO_ERROR);
        }
        if (this.mUiccCard.getCardState() == IccCardStatus.CardState.CARDSTATE_RESTRICTED) {
            setExternalState(IccCardConstants.State.CARD_RESTRICTED);
            return;
        }
        if ((this.mUiccCard instanceof EuiccCard) && ((EuiccCard) this.mUiccCard).getEid() == null) {
            log("EID is not ready yet.");
            return;
        }
        if (this.mUiccApplication == null) {
            loge("updateExternalState: setting state to NOT_READY because mUiccApplication is null");
            setExternalState(IccCardConstants.State.NOT_READY);
            return;
        }
        IccCardConstants.State state = null;
        IccCardApplicationStatus.AppState state2 = this.mUiccApplication.getState();
        boolean z = true;
        if (this.mUiccApplication.getPin1State() == IccCardStatus.PinState.PINSTATE_ENABLED_PERM_BLOCKED) {
            state = IccCardConstants.State.PERM_DISABLED;
        } else if (state2 == IccCardApplicationStatus.AppState.APPSTATE_PIN) {
            state = IccCardConstants.State.PIN_REQUIRED;
        } else if (state2 == IccCardApplicationStatus.AppState.APPSTATE_PUK) {
            state = IccCardConstants.State.PUK_REQUIRED;
        } else if (state2 == IccCardApplicationStatus.AppState.APPSTATE_SUBSCRIPTION_PERSO && (this.mUiccApplication.getPersoSubState() == IccCardApplicationStatus.PersoSubState.PERSOSUBSTATE_SIM_NETWORK || isSupportAllNetworkLockCategory())) {
            state = IccCardConstants.State.NETWORK_LOCKED;
        } else {
            z = false;
        }
        if (z) {
            if (this.mIccRecords != null && (this.mIccRecords.getLockedRecordsLoaded() || this.mIccRecords.getNetworkLockedRecordsLoaded())) {
                setExternalState(state);
                return;
            } else {
                setExternalState(IccCardConstants.State.NOT_READY);
                return;
            }
        }
        switch (state2) {
            case APPSTATE_UNKNOWN:
                setExternalState(IccCardConstants.State.NOT_READY);
                break;
            case APPSTATE_READY:
                checkAndUpdateIfAnyAppToBeIgnored();
                if (areAllApplicationsReady()) {
                    if (areAllRecordsLoaded() && areCarrierPriviligeRulesLoaded()) {
                        setExternalState(IccCardConstants.State.LOADED);
                    } else {
                        setExternalState(IccCardConstants.State.READY);
                    }
                } else {
                    setExternalState(IccCardConstants.State.NOT_READY);
                }
                break;
        }
    }

    private void registerAllAppEvents() {
        for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
            if (uiccCardApplication != null) {
                uiccCardApplication.registerForReady(this.mHandler, 3, null);
                IccRecords iccRecords = uiccCardApplication.getIccRecords();
                if (iccRecords != null) {
                    iccRecords.registerForRecordsLoaded(this.mHandler, 4, null);
                    iccRecords.registerForRecordsEvents(this.mHandler, 7, null);
                }
            }
        }
    }

    private void unregisterAllAppEvents() {
        for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
            if (uiccCardApplication != null) {
                uiccCardApplication.unregisterForReady(this.mHandler);
                IccRecords iccRecords = uiccCardApplication.getIccRecords();
                if (iccRecords != null) {
                    iccRecords.unregisterForRecordsLoaded(this.mHandler);
                    iccRecords.unregisterForRecordsEvents(this.mHandler);
                }
            }
        }
    }

    protected void registerCurrAppEvents() {
        if (this.mIccRecords != null) {
            this.mIccRecords.registerForLockedRecordsLoaded(this.mHandler, 2, null);
            this.mIccRecords.registerForNetworkLockedRecordsLoaded(this.mHandler, 5, null);
        }
    }

    protected void unregisterCurrAppEvents() {
        if (this.mIccRecords != null) {
            this.mIccRecords.unregisterForLockedRecordsLoaded(this.mHandler);
            this.mIccRecords.unregisterForNetworkLockedRecordsLoaded(this.mHandler);
        }
    }

    protected void setExternalState(IccCardConstants.State state, boolean z) {
        synchronized (this.mLock) {
            if (!SubscriptionManager.isValidSlotIndex(this.mPhoneId)) {
                loge("setExternalState: mPhoneId=" + this.mPhoneId + " is invalid; Return!!");
                return;
            }
            if (!z && state == this.mExternalState) {
                log("setExternalState: !override and newstate unchanged from " + state);
                return;
            }
            this.mExternalState = state;
            if (this.mExternalState == IccCardConstants.State.LOADED && this.mIccRecords != null) {
                String operatorNumeric = this.mIccRecords.getOperatorNumeric();
                log("setExternalState: operator=" + operatorNumeric + " mPhoneId=" + this.mPhoneId);
                if (!TextUtils.isEmpty(operatorNumeric)) {
                    this.mTelephonyManager.setSimOperatorNumericForPhone(this.mPhoneId, operatorNumeric);
                    String strSubstring = operatorNumeric.substring(0, 3);
                    if (strSubstring != null) {
                        this.mTelephonyManager.setSimCountryIsoForPhone(this.mPhoneId, MccTable.countryCodeForMcc(Integer.parseInt(strSubstring)));
                    } else {
                        loge("setExternalState: state LOADED; Country code is null");
                    }
                } else {
                    loge("setExternalState: state LOADED; Operator name is null");
                }
            }
            log("setExternalState: set mPhoneId=" + this.mPhoneId + " mExternalState=" + this.mExternalState);
            this.mTelephonyManager.setSimStateForPhone(this.mPhoneId, getState().toString());
            UiccController.updateInternalIccState(getIccStateIntentString(this.mExternalState), getIccStateReason(this.mExternalState), this.mPhoneId);
        }
    }

    protected void setExternalState(IccCardConstants.State state) {
        setExternalState(state, false);
    }

    public boolean getIccRecordsLoaded() {
        synchronized (this.mLock) {
            if (this.mIccRecords != null) {
                return this.mIccRecords.getRecordsLoaded();
            }
            return false;
        }
    }

    static class AnonymousClass4 {
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
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.CARD_RESTRICTED.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.LOADED.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState = new int[IccCardApplicationStatus.AppState.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN.ordinal()] = 1;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$uicc$IccCardApplicationStatus$AppState[IccCardApplicationStatus.AppState.APPSTATE_READY.ordinal()] = 2;
            } catch (NoSuchFieldError e12) {
            }
        }
    }

    protected String getIccStateIntentString(IccCardConstants.State state) {
        switch (AnonymousClass4.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
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
            case 9:
                return "CARD_RESTRICTED";
            case 10:
                return "LOADED";
            default:
                return "UNKNOWN";
        }
    }

    protected String getIccStateReason(IccCardConstants.State state) {
        switch (AnonymousClass4.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
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
            case 9:
                return "CARD_RESTRICTED";
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
    public void registerForNetworkLocked(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            Registrant registrant = new Registrant(handler, i, obj);
            this.mNetworkLockedRegistrants.add(registrant);
            if (getState() == IccCardConstants.State.NETWORK_LOCKED) {
                registrant.notifyRegistrant();
            }
        }
    }

    @Override
    public void unregisterForNetworkLocked(Handler handler) {
        synchronized (this.mLock) {
            this.mNetworkLockedRegistrants.remove(handler);
        }
    }

    @Override
    public void supplyPin(String str, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPin(str, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = covertException("supplyPin");
                message.sendToTarget();
            }
        }
    }

    @Override
    public void supplyPuk(String str, String str2, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPuk(str, str2, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = covertException("supplyPuk");
                message.sendToTarget();
            }
        }
    }

    @Override
    public void supplyPin2(String str, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPin2(str, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = covertException("supplyPin2");
                message.sendToTarget();
            }
        }
    }

    @Override
    public void supplyPuk2(String str, String str2, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyPuk2(str, str2, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = covertException("supplyPuk2");
                message.sendToTarget();
            }
        }
    }

    @Override
    public void supplyNetworkDepersonalization(String str, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.supplyNetworkDepersonalization(str, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = new RuntimeException("CommandsInterface is not set.");
                message.sendToTarget();
            }
        }
    }

    @Override
    public boolean getIccLockEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mUiccApplication != null && this.mUiccApplication.getIccLockEnabled();
        }
        return z;
    }

    @Override
    public boolean getIccFdnEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mUiccApplication != null && this.mUiccApplication.getIccFdnEnabled();
        }
        return z;
    }

    @Override
    public boolean getIccPin2Blocked() {
        return this.mUiccApplication != null && this.mUiccApplication.getIccPin2Blocked();
    }

    @Override
    public boolean getIccPuk2Blocked() {
        return this.mUiccApplication != null && this.mUiccApplication.getIccPuk2Blocked();
    }

    @Override
    public void setIccLockEnabled(boolean z, String str, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.setIccLockEnabled(z, str, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = covertException("setIccLockEnabled");
                message.sendToTarget();
            }
        }
    }

    @Override
    public void setIccFdnEnabled(boolean z, String str, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.setIccFdnEnabled(z, str, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = covertException("setIccFdnEnabled");
                message.sendToTarget();
            }
        }
    }

    @Override
    public void changeIccLockPassword(String str, String str2, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.changeIccLockPassword(str, str2, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = covertException("changeIccLockPassword");
                message.sendToTarget();
            }
        }
    }

    @Override
    public void changeIccFdnPassword(String str, String str2, Message message) {
        synchronized (this.mLock) {
            if (this.mUiccApplication != null) {
                this.mUiccApplication.changeIccFdnPassword(str, str2, message);
            } else if (message != null) {
                AsyncResult.forMessage(message).exception = covertException("changeIccFdnPassword");
                message.sendToTarget();
            }
        }
    }

    @Override
    public String getServiceProviderName() {
        synchronized (this.mLock) {
            if (this.mIccRecords != null) {
                return this.mIccRecords.getServiceProviderName();
            }
            return null;
        }
    }

    @Override
    public boolean hasIccCard() {
        if (this.mUiccCard.getCardState() != IccCardStatus.CardState.CARDSTATE_ABSENT) {
            return true;
        }
        loge("hasIccCard: UiccProfile is not null but UiccCard is null or card state is ABSENT");
        return false;
    }

    public void update(Context context, CommandsInterface commandsInterface, IccCardStatus iccCardStatus) {
        synchronized (this.mLock) {
            this.mUniversalPinState = iccCardStatus.mUniversalPinState;
            this.mGsmUmtsSubscriptionAppIndex = iccCardStatus.mGsmUmtsSubscriptionAppIndex;
            this.mCdmaSubscriptionAppIndex = iccCardStatus.mCdmaSubscriptionAppIndex;
            this.mImsSubscriptionAppIndex = iccCardStatus.mImsSubscriptionAppIndex;
            this.mContext = context;
            this.mCi = commandsInterface;
            this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
            log(iccCardStatus.mApplications.length + " applications");
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] == null) {
                    if (i < iccCardStatus.mApplications.length) {
                        this.mUiccApplications[i] = makeUiccApplication(this, iccCardStatus.mApplications[i], this.mContext, this.mCi);
                    }
                } else if (i >= iccCardStatus.mApplications.length) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                } else {
                    this.mUiccApplications[i].update(iccCardStatus.mApplications[i], this.mContext, this.mCi);
                }
            }
            createAndUpdateCatServiceLocked();
            log("Before privilege rules: " + this.mCarrierPrivilegeRules + " : " + iccCardStatus.mCardState);
            if (this.mCarrierPrivilegeRules == null && iccCardStatus.mCardState == IccCardStatus.CardState.CARDSTATE_PRESENT) {
                this.mCarrierPrivilegeRules = new UiccCarrierPrivilegeRules(this, this.mHandler.obtainMessage(13));
            } else if (this.mCarrierPrivilegeRules != null && iccCardStatus.mCardState != IccCardStatus.CardState.CARDSTATE_PRESENT) {
                this.mCarrierPrivilegeRules = null;
            }
            sanitizeApplicationIndexesLocked();
            updateIccAvailability(true);
        }
    }

    private void createAndUpdateCatServiceLocked() {
        if (this.mUiccApplications.length > 0 && this.mUiccApplications[0] != null) {
            if (this.mCatService == null) {
                this.mCatService = CatService.getInstance(this.mCi, this.mContext, this, this.mPhoneId);
                return;
            } else {
                this.mCatService.update(this.mCi, this.mContext, this);
                return;
            }
        }
        if (this.mCatService != null) {
            this.mCatService.dispose();
        }
        this.mCatService = null;
    }

    protected void finalize() {
        log("UiccProfile finalized");
    }

    private void sanitizeApplicationIndexesLocked() {
        this.mGsmUmtsSubscriptionAppIndex = checkIndexLocked(this.mGsmUmtsSubscriptionAppIndex, IccCardApplicationStatus.AppType.APPTYPE_SIM, IccCardApplicationStatus.AppType.APPTYPE_USIM);
        this.mCdmaSubscriptionAppIndex = checkIndexLocked(this.mCdmaSubscriptionAppIndex, IccCardApplicationStatus.AppType.APPTYPE_RUIM, IccCardApplicationStatus.AppType.APPTYPE_CSIM);
        this.mImsSubscriptionAppIndex = checkIndexLocked(this.mImsSubscriptionAppIndex, IccCardApplicationStatus.AppType.APPTYPE_ISIM, null);
    }

    private boolean isSupportedApplication(UiccCardApplication uiccCardApplication) {
        if (uiccCardApplication.getType() != IccCardApplicationStatus.AppType.APPTYPE_USIM && uiccCardApplication.getType() != IccCardApplicationStatus.AppType.APPTYPE_CSIM && uiccCardApplication.getType() != IccCardApplicationStatus.AppType.APPTYPE_SIM && uiccCardApplication.getType() != IccCardApplicationStatus.AppType.APPTYPE_RUIM) {
            return false;
        }
        return true;
    }

    private void checkAndUpdateIfAnyAppToBeIgnored() {
        boolean[] zArr = new boolean[IccCardApplicationStatus.AppType.APPTYPE_ISIM.ordinal() + 1];
        for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
            if (uiccCardApplication != null && isSupportedApplication(uiccCardApplication) && uiccCardApplication.isReady()) {
                zArr[uiccCardApplication.getType().ordinal()] = true;
            }
        }
        for (UiccCardApplication uiccCardApplication2 : this.mUiccApplications) {
            if (uiccCardApplication2 != null && isSupportedApplication(uiccCardApplication2) && !uiccCardApplication2.isReady() && zArr[uiccCardApplication2.getType().ordinal()]) {
                uiccCardApplication2.setAppIgnoreState(true);
            }
        }
    }

    private boolean areAllApplicationsReady() {
        for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
            if (uiccCardApplication != null && isSupportedApplication(uiccCardApplication) && !uiccCardApplication.isReady() && !uiccCardApplication.isAppIgnored()) {
                return false;
            }
        }
        return this.mUiccApplication != null;
    }

    private boolean areAllRecordsLoaded() {
        IccRecords iccRecords;
        for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
            if (uiccCardApplication != null && isSupportedApplication(uiccCardApplication) && !uiccCardApplication.isAppIgnored() && ((iccRecords = uiccCardApplication.getIccRecords()) == null || !iccRecords.isLoaded())) {
                return false;
            }
        }
        return this.mUiccApplication != null;
    }

    private int checkIndexLocked(int i, IccCardApplicationStatus.AppType appType, IccCardApplicationStatus.AppType appType2) {
        if (this.mUiccApplications == null || i >= this.mUiccApplications.length) {
            loge("App index " + i + " is invalid since there are no applications");
            return -1;
        }
        if (i < 0) {
            return -1;
        }
        if (this.mUiccApplications[i].getType() != appType && this.mUiccApplications[i].getType() != appType2) {
            loge("App index " + i + " is invalid since it's not " + appType + " and not " + appType2);
            return -1;
        }
        return i;
    }

    public void registerForOpertorBrandOverride(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            this.mOperatorBrandOverrideRegistrants.add(new Registrant(handler, i, obj));
        }
    }

    public void registerForCarrierPrivilegeRulesLoaded(Handler handler, int i, Object obj) {
        synchronized (this.mLock) {
            Registrant registrant = new Registrant(handler, i, obj);
            this.mCarrierPrivilegeRegistrants.add(registrant);
            if (areCarrierPriviligeRulesLoaded()) {
                registrant.notifyRegistrant();
            }
        }
    }

    public void unregisterForCarrierPrivilegeRulesLoaded(Handler handler) {
        synchronized (this.mLock) {
            this.mCarrierPrivilegeRegistrants.remove(handler);
        }
    }

    public void unregisterForOperatorBrandOverride(Handler handler) {
        synchronized (this.mLock) {
            this.mOperatorBrandOverrideRegistrants.remove(handler);
        }
    }

    static boolean isPackageInstalled(Context context, String str) {
        try {
            context.getPackageManager().getPackageInfo(str, 1);
            Rlog.d(LOG_TAG, str + " is installed.");
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Rlog.d(LOG_TAG, str + " is not installed.");
            return false;
        }
    }

    protected void promptInstallCarrierApp(String str) {
        this.mContext.startActivity(InstallCarrierAppTrampolineActivity.get(this.mContext, str));
    }

    private void onCarrierPrivilegesLoadedMessage() {
        UsageStatsManager usageStatsManager = (UsageStatsManager) this.mContext.getSystemService("usagestats");
        if (usageStatsManager != null) {
            usageStatsManager.onCarrierPrivilegedAppsChanged();
        }
        InstallCarrierAppUtils.hideAllNotifications(this.mContext);
        InstallCarrierAppUtils.unregisterPackageInstallReceiver(this.mContext);
        synchronized (this.mLock) {
            this.mCarrierPrivilegeRegistrants.notifyRegistrants();
            boolean z = true;
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) != 1) {
                z = false;
            }
            if (z) {
                Iterator<String> it = getUninstalledCarrierPackages().iterator();
                while (it.hasNext()) {
                    promptInstallCarrierApp(it.next());
                }
            } else {
                this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mProvisionCompleteContentObserver);
            }
        }
    }

    private Set<String> getUninstalledCarrierPackages() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "carrier_app_whitelist");
        if (TextUtils.isEmpty(string)) {
            return Collections.emptySet();
        }
        Map<String, String> toCertificateToPackageMap = parseToCertificateToPackageMap(string);
        if (toCertificateToPackageMap.isEmpty()) {
            return Collections.emptySet();
        }
        ArraySet arraySet = new ArraySet();
        Iterator<UiccAccessRule> it = this.mCarrierPrivilegeRules.getAccessRules().iterator();
        while (it.hasNext()) {
            String str = toCertificateToPackageMap.get(it.next().getCertificateHexString().toUpperCase());
            if (!TextUtils.isEmpty(str) && !isPackageInstalled(this.mContext, str)) {
                arraySet.add(str);
            }
        }
        return arraySet;
    }

    @VisibleForTesting
    public static Map<String, String> parseToCertificateToPackageMap(String str) {
        List listAsList = Arrays.asList(str.split("\\s*;\\s*"));
        if (listAsList.isEmpty()) {
            return Collections.emptyMap();
        }
        ArrayMap arrayMap = new ArrayMap(listAsList.size());
        Iterator it = listAsList.iterator();
        while (it.hasNext()) {
            String[] strArrSplit = ((String) it.next()).split("\\s*:\\s*");
            if (strArrSplit.length == 2) {
                arrayMap.put(strArrSplit[0].toUpperCase(), strArrSplit[1]);
            } else {
                Rlog.d(LOG_TAG, "Incorrect length of key-value pair in carrier app whitelist map.  Length should be exactly 2");
            }
        }
        return arrayMap;
    }

    @Override
    public boolean isApplicationOnIcc(IccCardApplicationStatus.AppType appType) {
        synchronized (this.mLock) {
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] != null && this.mUiccApplications[i].getType() == appType) {
                    return true;
                }
            }
            return false;
        }
    }

    public IccCardStatus.PinState getUniversalPinState() {
        IccCardStatus.PinState pinState;
        synchronized (this.mLock) {
            pinState = this.mUniversalPinState;
        }
        return pinState;
    }

    public UiccCardApplication getApplication(int i) {
        synchronized (this.mLock) {
            int i2 = 8;
            try {
                switch (i) {
                    case 1:
                        i2 = this.mGsmUmtsSubscriptionAppIndex;
                        break;
                    case 2:
                        i2 = this.mCdmaSubscriptionAppIndex;
                        break;
                    case 3:
                        i2 = this.mImsSubscriptionAppIndex;
                        break;
                }
                if (i2 >= 0 && i2 < this.mUiccApplications.length) {
                    return this.mUiccApplications[i2];
                }
                return null;
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public UiccCardApplication getApplicationIndex(int i) {
        synchronized (this.mLock) {
            if (i >= 0) {
                try {
                    if (i < this.mUiccApplications.length) {
                        return this.mUiccApplications[i];
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            return null;
        }
    }

    public UiccCardApplication getApplicationByType(int i) {
        synchronized (this.mLock) {
            for (int i2 = 0; i2 < this.mUiccApplications.length; i2++) {
                if (this.mUiccApplications[i2] != null && this.mUiccApplications[i2].getType().ordinal() == i) {
                    return this.mUiccApplications[i2];
                }
            }
            return null;
        }
    }

    public boolean resetAppWithAid(String str) {
        boolean z;
        synchronized (this.mLock) {
            z = false;
            for (int i = 0; i < this.mUiccApplications.length; i++) {
                if (this.mUiccApplications[i] != null && (TextUtils.isEmpty(str) || str.equals(this.mUiccApplications[i].getAid()))) {
                    this.mUiccApplications[i].dispose();
                    this.mUiccApplications[i] = null;
                    z = true;
                }
            }
            if (TextUtils.isEmpty(str)) {
                if (this.mCarrierPrivilegeRules != null) {
                    this.mCarrierPrivilegeRules = null;
                    z = true;
                }
                if (this.mCatService != null) {
                    this.mCatService.dispose();
                    this.mCatService = null;
                    z = true;
                }
            }
        }
        return z;
    }

    public void iccOpenLogicalChannel(String str, int i, Message message) {
        loglocal("iccOpenLogicalChannel: " + str + " , " + i + " by pid:" + Binder.getCallingPid() + " uid:" + Binder.getCallingUid());
        this.mCi.iccOpenLogicalChannel(str, i, this.mHandler.obtainMessage(8, message));
    }

    public void iccCloseLogicalChannel(int i, Message message) {
        loglocal("iccCloseLogicalChannel: " + i);
        this.mCi.iccCloseLogicalChannel(i, this.mHandler.obtainMessage(9, message));
    }

    public void iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, String str, Message message) {
        this.mCi.iccTransmitApduLogicalChannel(i, i2, i3, i4, i5, i6, str, this.mHandler.obtainMessage(10, message));
    }

    public void iccTransmitApduBasicChannel(int i, int i2, int i3, int i4, int i5, String str, Message message) {
        this.mCi.iccTransmitApduBasicChannel(i, i2, i3, i4, i5, str, this.mHandler.obtainMessage(11, message));
    }

    public void iccExchangeSimIO(int i, int i2, int i3, int i4, int i5, String str, Message message) {
        this.mCi.iccIO(i2, i, str, i3, i4, i5, null, null, this.mHandler.obtainMessage(12, message));
    }

    public void sendEnvelopeWithStatus(String str, Message message) {
        this.mCi.sendEnvelopeWithStatus(str, message);
    }

    public int getNumApplications() {
        int i = 0;
        for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
            if (uiccCardApplication != null) {
                i++;
            }
        }
        return i;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public boolean areCarrierPriviligeRulesLoaded() {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        return carrierPrivilegeRules == null || carrierPrivilegeRules.areCarrierPriviligeRulesLoaded();
    }

    public boolean hasCarrierPrivilegeRules() {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        return carrierPrivilegeRules != null && carrierPrivilegeRules.hasCarrierPrivilegeRules();
    }

    public int getCarrierPrivilegeStatus(Signature signature, String str) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(signature, str);
    }

    public int getCarrierPrivilegeStatus(PackageManager packageManager, String str) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(packageManager, str);
    }

    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatus(packageInfo);
    }

    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatusForCurrentTransaction(packageManager);
    }

    public int getCarrierPrivilegeStatusForUid(PackageManager packageManager, int i) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return -1;
        }
        return carrierPrivilegeRules.getCarrierPrivilegeStatusForUid(packageManager, i);
    }

    public List<String> getCarrierPackageNamesForIntent(PackageManager packageManager, Intent intent) {
        UiccCarrierPrivilegeRules carrierPrivilegeRules = getCarrierPrivilegeRules();
        if (carrierPrivilegeRules == null) {
            return null;
        }
        return carrierPrivilegeRules.getCarrierPackageNamesForIntent(packageManager, intent);
    }

    private UiccCarrierPrivilegeRules getCarrierPrivilegeRules() {
        UiccCarrierPrivilegeRules uiccCarrierPrivilegeRules;
        synchronized (this.mLock) {
            uiccCarrierPrivilegeRules = this.mCarrierPrivilegeRules;
        }
        return uiccCarrierPrivilegeRules;
    }

    public boolean setOperatorBrandOverride(String str) {
        log("setOperatorBrandOverride: " + str);
        log("current iccId: " + SubscriptionInfo.givePrintableIccid(getIccId()));
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        String str2 = OPERATOR_BRAND_OVERRIDE_PREFIX + iccId;
        if (str == null) {
            editorEdit.remove(str2).commit();
        } else {
            editorEdit.putString(str2, str).commit();
        }
        this.mOperatorBrandOverrideRegistrants.notifyRegistrants();
        return true;
    }

    public String getOperatorBrandOverride() {
        String iccId = getIccId();
        if (TextUtils.isEmpty(iccId)) {
            return null;
        }
        return PreferenceManager.getDefaultSharedPreferences(this.mContext).getString(OPERATOR_BRAND_OVERRIDE_PREFIX + iccId, null);
    }

    public String getIccId() {
        IccRecords iccRecords;
        for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
            if (uiccCardApplication != null && (iccRecords = uiccCardApplication.getIccRecords()) != null && iccRecords.getIccId() != null) {
                return iccRecords.getIccId();
            }
        }
        return null;
    }

    protected void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    private void loglocal(String str) {
        UiccController.sLocalLog.log("UiccProfile[" + this.mPhoneId + "]: " + str);
    }

    @VisibleForTesting
    public void refresh() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(13));
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IccRecords iccRecords;
        printWriter.println("UiccProfile:");
        printWriter.println(" mCi=" + this.mCi);
        printWriter.println(" mCatService=" + this.mCatService);
        for (int i = 0; i < this.mCarrierPrivilegeRegistrants.size(); i++) {
            printWriter.println("  mCarrierPrivilegeRegistrants[" + i + "]=" + ((Registrant) this.mCarrierPrivilegeRegistrants.get(i)).getHandler());
        }
        for (int i2 = 0; i2 < this.mOperatorBrandOverrideRegistrants.size(); i2++) {
            printWriter.println("  mOperatorBrandOverrideRegistrants[" + i2 + "]=" + ((Registrant) this.mOperatorBrandOverrideRegistrants.get(i2)).getHandler());
        }
        printWriter.println(" mUniversalPinState=" + this.mUniversalPinState);
        printWriter.println(" mGsmUmtsSubscriptionAppIndex=" + this.mGsmUmtsSubscriptionAppIndex);
        printWriter.println(" mCdmaSubscriptionAppIndex=" + this.mCdmaSubscriptionAppIndex);
        printWriter.println(" mImsSubscriptionAppIndex=" + this.mImsSubscriptionAppIndex);
        printWriter.println(" mUiccApplications: length=" + this.mUiccApplications.length);
        for (int i3 = 0; i3 < this.mUiccApplications.length; i3++) {
            if (this.mUiccApplications[i3] == null) {
                printWriter.println("  mUiccApplications[" + i3 + "]=" + ((Object) null));
            } else {
                printWriter.println("  mUiccApplications[" + i3 + "]=" + this.mUiccApplications[i3].getType() + " " + this.mUiccApplications[i3]);
            }
        }
        printWriter.println();
        for (UiccCardApplication uiccCardApplication : this.mUiccApplications) {
            if (uiccCardApplication != null) {
                uiccCardApplication.dump(fileDescriptor, printWriter, strArr);
                printWriter.println();
            }
        }
        for (UiccCardApplication uiccCardApplication2 : this.mUiccApplications) {
            if (uiccCardApplication2 != null && (iccRecords = uiccCardApplication2.getIccRecords()) != null) {
                iccRecords.dump(fileDescriptor, printWriter, strArr);
                printWriter.println();
            }
        }
        if (this.mCarrierPrivilegeRules == null) {
            printWriter.println(" mCarrierPrivilegeRules: null");
        } else {
            printWriter.println(" mCarrierPrivilegeRules: " + this.mCarrierPrivilegeRules);
            this.mCarrierPrivilegeRules.dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println(" mCarrierPrivilegeRegistrants: size=" + this.mCarrierPrivilegeRegistrants.size());
        for (int i4 = 0; i4 < this.mCarrierPrivilegeRegistrants.size(); i4++) {
            printWriter.println("  mCarrierPrivilegeRegistrants[" + i4 + "]=" + ((Registrant) this.mCarrierPrivilegeRegistrants.get(i4)).getHandler());
        }
        printWriter.flush();
        printWriter.println(" mNetworkLockedRegistrants: size=" + this.mNetworkLockedRegistrants.size());
        for (int i5 = 0; i5 < this.mNetworkLockedRegistrants.size(); i5++) {
            printWriter.println("  mNetworkLockedRegistrants[" + i5 + "]=" + ((Registrant) this.mNetworkLockedRegistrants.get(i5)).getHandler());
        }
        printWriter.println(" mCurrentAppType=" + this.mCurrentAppType);
        printWriter.println(" mUiccCard=" + this.mUiccCard);
        printWriter.println(" mUiccApplication=" + this.mUiccApplication);
        printWriter.println(" mIccRecords=" + this.mIccRecords);
        printWriter.println(" mExternalState=" + this.mExternalState);
        printWriter.flush();
    }

    protected Exception covertException(String str) {
        return new RuntimeException("ICC card is absent.");
    }

    protected UiccCardApplication makeUiccApplication(UiccProfile uiccProfile, IccCardApplicationStatus iccCardApplicationStatus, Context context, CommandsInterface commandsInterface) {
        return new UiccCardApplication(uiccProfile, iccCardApplicationStatus, context, commandsInterface);
    }

    protected boolean isSupportAllNetworkLockCategory() {
        return false;
    }

    protected String getSubscriptionDisplayName(int i, Context context) {
        return this.mTelephonyManager.getSimOperatorName(i);
    }
}
