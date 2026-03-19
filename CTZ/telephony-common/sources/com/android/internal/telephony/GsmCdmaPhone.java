package com.android.internal.telephony;

import android.R;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UssdResponse;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.Log;
import com.android.ims.ImsManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.cdma.EriManager;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccException;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccVmNotSupportedException;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.IsimUiccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.telephony.uicc.UiccSlot;
import com.google.android.mms.pdu.CharacterSets;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GsmCdmaPhone extends Phone {
    public static final int CANCEL_ECM_TIMER = 1;
    private static final boolean DBG = true;
    private static final int DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;
    private static final int INVALID_SYSTEM_SELECTION_CODE = -1;
    private static final String IS683A_FEATURE_CODE = "*228";
    private static final int IS683A_FEATURE_CODE_NUM_DIGITS = 4;
    private static final int IS683A_SYS_SEL_CODE_NUM_DIGITS = 2;
    private static final int IS683A_SYS_SEL_CODE_OFFSET = 4;
    private static final int IS683_CONST_1900MHZ_A_BLOCK = 2;
    private static final int IS683_CONST_1900MHZ_B_BLOCK = 3;
    private static final int IS683_CONST_1900MHZ_C_BLOCK = 4;
    private static final int IS683_CONST_1900MHZ_D_BLOCK = 5;
    private static final int IS683_CONST_1900MHZ_E_BLOCK = 6;
    private static final int IS683_CONST_1900MHZ_F_BLOCK = 7;
    private static final int IS683_CONST_800MHZ_A_BAND = 0;
    private static final int IS683_CONST_800MHZ_B_BAND = 1;
    public static final String LOG_TAG = "GsmCdmaPhone";
    public static final String PROPERTY_CDMA_HOME_OPERATOR_NUMERIC = "ro.cdma.home.operator.numeric";
    private static final int REPORTING_HYSTERESIS_DB = 2;
    private static final int REPORTING_HYSTERESIS_KBPS = 50;
    private static final int REPORTING_HYSTERESIS_MILLIS = 3000;
    public static final int RESTART_ECM_TIMER = 0;
    private static final boolean VDBG = false;
    private static final String VM_NUMBER = "vm_number_key";
    private static final String VM_NUMBER_CDMA = "vm_number_key_cdma";
    private static final String VM_SIM_IMSI = "vm_sim_imsi_key";
    private static Pattern pOtaSpNumSchema = Pattern.compile("[,\\s]+");
    private boolean mBroadcastEmergencyCallStateChanges;
    private BroadcastReceiver mBroadcastReceiver;
    protected CarrierKeyDownloadManager mCDM;
    protected CarrierInfoManager mCIM;
    public GsmCdmaCallTracker mCT;
    private CarrierIdentifier mCarrerIdentifier;
    protected String mCarrierOtaSpNumSchema;
    protected CdmaSubscriptionSourceManager mCdmaSSM;
    public int mCdmaSubscriptionSource;
    private Registrant mEcmExitRespRegistrant;
    private final RegistrantList mEcmTimerResetRegistrants;
    private final RegistrantList mEriFileLoadedRegistrants;
    public EriManager mEriManager;
    private String mEsn;
    private Runnable mExitEcmRunnable;
    protected IccPhoneBookInterfaceManager mIccPhoneBookIntManager;
    protected IccSmsInterfaceManager mIccSmsInterfaceManager;
    private String mImei;
    protected String mImeiSv;
    private IsimUiccRecords mIsimUiccRecords;
    private String mMeid;
    protected ArrayList<MmiCode> mPendingMMIs;
    private int mPrecisePhoneType;
    protected boolean mResetModemOnRadioTechnologyChange;
    protected int mRilVersion;
    public ServiceStateTracker mSST;
    private SIMRecords mSimRecords;
    protected RegistrantList mSsnRegistrants;
    private String mVmNumber;
    protected PowerManager.WakeLock mWakeLock;

    private static class Cfu {
        final Message mOnComplete;
        final String mSetCfNumber;

        Cfu(String str, Message message) {
            this.mSetCfNumber = str;
            this.mOnComplete = message;
        }
    }

    public GsmCdmaPhone(Context context, CommandsInterface commandsInterface, PhoneNotifier phoneNotifier, int i, int i2, TelephonyComponentFactory telephonyComponentFactory) {
        this(context, commandsInterface, phoneNotifier, false, i, i2, telephonyComponentFactory);
    }

    public GsmCdmaPhone(Context context, CommandsInterface commandsInterface, PhoneNotifier phoneNotifier, boolean z, int i, int i2, TelephonyComponentFactory telephonyComponentFactory) {
        super(i2 == 1 ? "GSM" : "CDMA", phoneNotifier, context, commandsInterface, z, i, telephonyComponentFactory);
        this.mSsnRegistrants = new RegistrantList();
        this.mCdmaSubscriptionSource = -1;
        this.mEriFileLoadedRegistrants = new RegistrantList();
        this.mExitEcmRunnable = new Runnable() {
            @Override
            public void run() {
                GsmCdmaPhone.this.exitEmergencyCallbackMode();
            }
        };
        this.mPendingMMIs = new ArrayList<>();
        this.mEcmTimerResetRegistrants = new RegistrantList();
        this.mResetModemOnRadioTechnologyChange = false;
        this.mBroadcastEmergencyCallStateChanges = false;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Rlog.d(GsmCdmaPhone.LOG_TAG, "mBroadcastReceiver: action " + intent.getAction());
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    GsmCdmaPhone.this.sendMessage(GsmCdmaPhone.this.obtainMessage(43));
                }
            }
        };
        this.mPrecisePhoneType = i2;
        initOnce(commandsInterface);
        initRatSpecific(i2);
        this.mCarrierActionAgent = this.mTelephonyComponentFactory.makeCarrierActionAgent(this);
        this.mCarrierSignalAgent = this.mTelephonyComponentFactory.makeCarrierSignalAgent(this);
        this.mSST = this.mTelephonyComponentFactory.makeServiceStateTracker(this, this.mCi);
        this.mDcTracker = this.mTelephonyComponentFactory.makeDcTracker(this);
        this.mCarrerIdentifier = this.mTelephonyComponentFactory.makeCarrierIdentifier(this);
        this.mSST.registerForNetworkAttached(this, 19, null);
        this.mDeviceStateMonitor = this.mTelephonyComponentFactory.makeDeviceStateMonitor(this);
        logd("GsmCdmaPhone: constructor: sub = " + this.mPhoneId);
    }

    protected void initOnce(CommandsInterface commandsInterface) {
        if (commandsInterface instanceof SimulatedRadioControl) {
            this.mSimulatedRadioControl = (SimulatedRadioControl) commandsInterface;
        }
        this.mCT = this.mTelephonyComponentFactory.makeGsmCdmaCallTracker(this);
        this.mIccPhoneBookIntManager = this.mTelephonyComponentFactory.makeIccPhoneBookInterfaceManager(this);
        this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, LOG_TAG);
        this.mIccSmsInterfaceManager = this.mTelephonyComponentFactory.makeIccSmsInterfaceManager(this);
        this.mCi.registerForAvailable(this, 1, null);
        this.mCi.registerForOffOrNotAvailable(this, 8, null);
        this.mCi.registerForOn(this, 5, null);
        this.mCi.setOnSuppServiceNotification(this, 2, null);
        this.mCi.setOnUSSD(this, 7, null);
        this.mCi.setOnSs(this, 36, null);
        this.mCdmaSSM = this.mTelephonyComponentFactory.getCdmaSubscriptionSourceManagerInstance(this.mContext, this.mCi, this, 27, null);
        this.mEriManager = this.mTelephonyComponentFactory.makeEriManager(this, this.mContext, 0);
        this.mCi.setEmergencyCallbackMode(this, 25, null);
        this.mCi.registerForExitEmergencyCallbackMode(this, 26, null);
        this.mCi.registerForModemReset(this, 45, null);
        this.mCarrierOtaSpNumSchema = TelephonyManager.from(this.mContext).getOtaSpNumberSchemaForPhone(getPhoneId(), "");
        this.mResetModemOnRadioTechnologyChange = SystemProperties.getBoolean("persist.radio.reset_on_switch", false);
        this.mCi.registerForRilConnected(this, 41, null);
        this.mCi.registerForVoiceRadioTechChanged(this, 39, null);
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
        this.mCDM = new CarrierKeyDownloadManager(this);
        this.mCIM = new CarrierInfoManager();
    }

    protected void initRatSpecific(int i) {
        this.mPendingMMIs.clear();
        if (needResetPhbIntMgr()) {
            this.mIccPhoneBookIntManager.updateIccRecords(null);
        }
        this.mEsn = null;
        this.mMeid = null;
        this.mPrecisePhoneType = i;
        logd("Precise phone type " + this.mPrecisePhoneType);
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(this.mContext);
        UiccProfile uiccProfile = getUiccProfile();
        if (isPhoneTypeGsm()) {
            this.mCi.setPhoneType(1);
            telephonyManagerFrom.setPhoneType(getPhoneId(), 1);
            if (uiccProfile != null) {
                uiccProfile.setVoiceRadioTech(3);
                return;
            }
            return;
        }
        this.mCdmaSubscriptionSource = this.mCdmaSSM.getCdmaSubscriptionSource();
        this.mIsPhoneInEcmState = getInEcmMode();
        if (this.mIsPhoneInEcmState) {
            this.mCi.exitEmergencyCallbackMode(obtainMessage(26));
        }
        this.mCi.setPhoneType(2);
        telephonyManagerFrom.setPhoneType(getPhoneId(), 2);
        if (uiccProfile != null) {
            uiccProfile.setVoiceRadioTech(6);
        }
        String str = SystemProperties.get("ro.cdma.home.operator.alpha");
        String str2 = SystemProperties.get(PROPERTY_CDMA_HOME_OPERATOR_NUMERIC);
        logd("init: operatorAlpha='" + str + "' operatorNumeric='" + str2 + "'");
        if (!TextUtils.isEmpty(str)) {
            logd("init: set 'gsm.sim.operator.alpha' to operator='" + str + "'");
            telephonyManagerFrom.setSimOperatorNameForPhone(this.mPhoneId, str);
        }
        if (!TextUtils.isEmpty(str2)) {
            logd("init: set 'gsm.sim.operator.numeric' to operator='" + str2 + "'");
            StringBuilder sb = new StringBuilder();
            sb.append("update icc_operator_numeric=");
            sb.append(str2);
            logd(sb.toString());
            telephonyManagerFrom.setSimOperatorNumericForPhone(this.mPhoneId, str2);
            SubscriptionController.getInstance().setMccMnc(str2, getSubId());
            setIsoCountryProperty(str2);
            logd("update mccmnc=" + str2);
            MccTable.updateMccMncConfiguration(this.mContext, str2, false);
        }
        updateCurrentCarrierInProvider(str2);
    }

    private void setIsoCountryProperty(String str) {
        String strCountryCodeForMcc;
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(this.mContext);
        if (TextUtils.isEmpty(str)) {
            logd("setIsoCountryProperty: clear 'gsm.sim.operator.iso-country'");
            telephonyManagerFrom.setSimCountryIsoForPhone(this.mPhoneId, "");
            return;
        }
        try {
            strCountryCodeForMcc = MccTable.countryCodeForMcc(Integer.parseInt(str.substring(0, 3)));
        } catch (NumberFormatException e) {
            Rlog.e(LOG_TAG, "setIsoCountryProperty: countryCodeForMcc error", e);
            strCountryCodeForMcc = "";
        } catch (StringIndexOutOfBoundsException e2) {
            Rlog.e(LOG_TAG, "setIsoCountryProperty: countryCodeForMcc error", e2);
            strCountryCodeForMcc = "";
        }
        logd("setIsoCountryProperty: set 'gsm.sim.operator.iso-country' to iso=" + strCountryCodeForMcc);
        telephonyManagerFrom.setSimCountryIsoForPhone(this.mPhoneId, strCountryCodeForMcc);
    }

    public boolean isPhoneTypeGsm() {
        return this.mPrecisePhoneType == 1;
    }

    public boolean isPhoneTypeCdma() {
        return this.mPrecisePhoneType == 2;
    }

    public boolean isPhoneTypeCdmaLte() {
        return this.mPrecisePhoneType == 6;
    }

    protected void switchPhoneType(int i) {
        removeCallbacks(this.mExitEcmRunnable);
        initRatSpecific(i);
        this.mSST.updatePhoneType();
        setPhoneName(i == 1 ? "GSM" : "CDMA");
        onUpdateIccAvailability();
        this.mCT.updatePhoneType();
        CommandsInterface.RadioState radioState = this.mCi.getRadioState();
        if (radioState.isAvailable()) {
            handleRadioAvailable();
            if (radioState.isOn()) {
                handleRadioOn();
            }
        }
        if (!radioState.isAvailable() || !radioState.isOn()) {
            handleRadioOffOrNotAvailable();
        }
    }

    protected void finalize() {
        logd("GsmCdmaPhone finalized");
        if (this.mWakeLock != null && this.mWakeLock.isHeld()) {
            Rlog.e(LOG_TAG, "UNEXPECTED; mWakeLock is held when finalizing.");
            this.mWakeLock.release();
        }
    }

    @Override
    public ServiceState getServiceState() {
        if ((this.mSST == null || this.mSST.mSS.getState() != 0) && this.mImsPhone != null) {
            return ServiceState.mergeServiceStates(this.mSST == null ? new ServiceState() : this.mSST.mSS, this.mImsPhone.getServiceState());
        }
        if (this.mSST != null) {
            return this.mSST.mSS;
        }
        return new ServiceState();
    }

    @Override
    public CellLocation getCellLocation(WorkSource workSource) {
        if (isPhoneTypeGsm()) {
            return this.mSST.getCellLocation(workSource);
        }
        CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) this.mSST.mCellLoc;
        if (Settings.Secure.getInt(getContext().getContentResolver(), "location_mode", 0) != 0) {
            return cdmaCellLocation;
        }
        CdmaCellLocation cdmaCellLocation2 = new CdmaCellLocation();
        cdmaCellLocation2.setCellLocationData(cdmaCellLocation.getBaseStationId(), KeepaliveStatus.INVALID_HANDLE, KeepaliveStatus.INVALID_HANDLE, cdmaCellLocation.getSystemId(), cdmaCellLocation.getNetworkId());
        return cdmaCellLocation2;
    }

    @Override
    public PhoneConstants.State getState() {
        PhoneConstants.State state;
        if (this.mImsPhone != null && (state = this.mImsPhone.getState()) != PhoneConstants.State.IDLE) {
            return state;
        }
        return this.mCT.mState;
    }

    @Override
    public int getPhoneType() {
        return this.mPrecisePhoneType == 1 ? 1 : 2;
    }

    @Override
    public ServiceStateTracker getServiceStateTracker() {
        return this.mSST;
    }

    @Override
    public CallTracker getCallTracker() {
        return this.mCT;
    }

    @Override
    public void updateVoiceMail() {
        if (isPhoneTypeGsm()) {
            int storedVoiceMessageCount = 0;
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                storedVoiceMessageCount = iccRecords.getVoiceMessageCount();
            }
            if (storedVoiceMessageCount == -2) {
                storedVoiceMessageCount = getStoredVoiceMessageCount();
            }
            logd("updateVoiceMail countVoiceMessages = " + storedVoiceMessageCount + " subId " + getSubId());
            setVoiceMessageCount(storedVoiceMessageCount);
            return;
        }
        setVoiceMessageCount(getStoredVoiceMessageCount());
    }

    @Override
    public List<? extends MmiCode> getPendingMmiCodes() {
        return this.mPendingMMIs;
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String str) {
        PhoneConstants.DataState dataState;
        PhoneConstants.DataState dataState2 = PhoneConstants.DataState.DISCONNECTED;
        if (this.mSST == null) {
            dataState = PhoneConstants.DataState.DISCONNECTED;
        } else if (this.mSST.getCurrentDataConnectionState() != 0 && (isPhoneTypeCdma() || isPhoneTypeCdmaLte() || (isPhoneTypeGsm() && !str.equals("emergency")))) {
            dataState = PhoneConstants.DataState.DISCONNECTED;
        } else {
            switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$DctConstants$State[this.mDcTracker.getState(str).ordinal()]) {
                case 1:
                case 2:
                    if (this.mCT.mState != PhoneConstants.State.IDLE && !this.mSST.isConcurrentVoiceAndDataAllowed()) {
                        dataState = PhoneConstants.DataState.SUSPENDED;
                    } else {
                        dataState = PhoneConstants.DataState.CONNECTED;
                    }
                    break;
                case 3:
                    dataState = PhoneConstants.DataState.CONNECTING;
                    break;
                default:
                    dataState = PhoneConstants.DataState.DISCONNECTED;
                    break;
            }
        }
        logd("getDataConnectionState apnType=" + str + " ret=" + dataState);
        return dataState;
    }

    @Override
    public PhoneInternalInterface.DataActivityState getDataActivityState() {
        PhoneInternalInterface.DataActivityState dataActivityState = PhoneInternalInterface.DataActivityState.NONE;
        if (this.mSST.getCurrentDataConnectionState() == 0) {
            switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$DctConstants$Activity[this.mDcTracker.getActivity().ordinal()]) {
                case 1:
                    return PhoneInternalInterface.DataActivityState.DATAIN;
                case 2:
                    return PhoneInternalInterface.DataActivityState.DATAOUT;
                case 3:
                    return PhoneInternalInterface.DataActivityState.DATAINANDOUT;
                case 4:
                    return PhoneInternalInterface.DataActivityState.DORMANT;
                default:
                    return PhoneInternalInterface.DataActivityState.NONE;
            }
        }
        return dataActivityState;
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$internal$telephony$DctConstants$Activity = new int[DctConstants.Activity.values().length];
        static final int[] $SwitchMap$com$android$internal$telephony$DctConstants$State;

        static {
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$Activity[DctConstants.Activity.DATAIN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$Activity[DctConstants.Activity.DATAOUT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$Activity[DctConstants.Activity.DATAINANDOUT.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$Activity[DctConstants.Activity.DORMANT.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            $SwitchMap$com$android$internal$telephony$DctConstants$State = new int[DctConstants.State.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.DISCONNECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTING.ordinal()] = 3;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    public void notifyPhoneStateChanged() {
        this.mNotifier.notifyPhoneState(this);
    }

    public void notifyPreciseCallStateChanged() {
        super.notifyPreciseCallStateChangedP();
    }

    public void notifyNewRingingConnection(Connection connection) {
        super.notifyNewRingingConnectionP(connection);
    }

    public void notifyDisconnect(Connection connection) {
        this.mDisconnectRegistrants.notifyResult(connection);
        this.mNotifier.notifyDisconnectCause(connection.getDisconnectCause(), connection.getPreciseDisconnectCause());
    }

    public void notifyUnknownConnection(Connection connection) {
        super.notifyUnknownConnectionP(connection);
    }

    @Override
    public boolean isInEmergencyCall() {
        if (isPhoneTypeGsm()) {
            return false;
        }
        return this.mCT.isInEmergencyCall();
    }

    @Override
    protected void setIsInEmergencyCall() {
        if (!isPhoneTypeGsm()) {
            this.mCT.setIsInEmergencyCall();
        }
    }

    private void sendEmergencyCallbackModeChange() {
        Intent intent = new Intent("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        intent.putExtra("phoneinECMState", isInEcm());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        ActivityManager.broadcastStickyIntent(intent, -1);
        logd("sendEmergencyCallbackModeChange");
    }

    @Override
    public void sendEmergencyCallStateChange(boolean z) {
        if (this.mBroadcastEmergencyCallStateChanges) {
            Intent intent = new Intent("android.intent.action.EMERGENCY_CALL_STATE_CHANGED");
            intent.putExtra("phoneInEmergencyCall", z);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
            ActivityManager.broadcastStickyIntent(intent, -1);
            Rlog.d(LOG_TAG, "sendEmergencyCallStateChange: callActive " + z);
        }
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean z) {
        this.mBroadcastEmergencyCallStateChanges = z;
    }

    public void notifySuppServiceFailed(PhoneInternalInterface.SuppService suppService) {
        this.mSuppServiceFailedRegistrants.notifyResult(suppService);
    }

    public void notifyServiceStateChanged(ServiceState serviceState) {
        super.notifyServiceStateChangedP(serviceState);
    }

    public void notifyLocationChanged() {
        this.mNotifier.notifyCellLocation(this);
    }

    @Override
    public void notifyCallForwardingIndicator() {
        this.mNotifier.notifyCallForwardingChanged(this);
    }

    @Override
    public void registerForSuppServiceNotification(Handler handler, int i, Object obj) {
        this.mSsnRegistrants.addUnique(handler, i, obj);
        if (this.mSsnRegistrants.size() == 1) {
            this.mCi.setSuppServiceNotifications(true, null);
        }
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler handler) {
        this.mSsnRegistrants.remove(handler);
        if (this.mSsnRegistrants.size() == 0) {
            this.mCi.setSuppServiceNotifications(false, null);
        }
    }

    @Override
    public void registerForSimRecordsLoaded(Handler handler, int i, Object obj) {
        this.mSimRecordsLoadedRegistrants.addUnique(handler, i, obj);
    }

    @Override
    public void unregisterForSimRecordsLoaded(Handler handler) {
        this.mSimRecordsLoadedRegistrants.remove(handler);
    }

    @Override
    public void acceptCall(int i) throws CallStateException {
        Phone phone = this.mImsPhone;
        if (phone != null && phone.getRingingCall().isRinging()) {
            phone.acceptCall(i);
        } else {
            this.mCT.acceptCall();
        }
    }

    @Override
    public void rejectCall() throws CallStateException {
        this.mCT.rejectCall();
    }

    @Override
    public void switchHoldingAndActive() throws CallStateException {
        this.mCT.switchWaitingOrHoldingAndActive();
    }

    @Override
    public String getIccSerialNumber() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (!isPhoneTypeGsm() && iccRecords == null) {
            iccRecords = this.mUiccController.getIccRecords(this.mPhoneId, 1);
        }
        if (iccRecords != null) {
            return iccRecords.getIccId();
        }
        return null;
    }

    @Override
    public String getFullIccSerialNumber() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (!isPhoneTypeGsm() && iccRecords == null) {
            iccRecords = this.mUiccController.getIccRecords(this.mPhoneId, 1);
        }
        if (iccRecords != null) {
            return iccRecords.getFullIccId();
        }
        return null;
    }

    @Override
    public boolean canConference() {
        if (this.mImsPhone != null && this.mImsPhone.canConference()) {
            return true;
        }
        if (isPhoneTypeGsm()) {
            return this.mCT.canConference();
        }
        loge("canConference: not possible in CDMA");
        return false;
    }

    @Override
    public void conference() {
        if (this.mImsPhone != null && this.mImsPhone.canConference()) {
            logd("conference() - delegated to IMS phone");
            try {
                this.mImsPhone.conference();
                return;
            } catch (CallStateException e) {
                loge(e.toString());
                return;
            }
        }
        if (isPhoneTypeGsm()) {
            this.mCT.conference();
        } else {
            loge("conference: not possible in CDMA");
        }
    }

    @Override
    public void enableEnhancedVoicePrivacy(boolean z, Message message) {
        if (isPhoneTypeGsm()) {
            loge("enableEnhancedVoicePrivacy: not expected on GSM");
        } else {
            this.mCi.setPreferredVoicePrivacy(z, message);
        }
    }

    @Override
    public void getEnhancedVoicePrivacy(Message message) {
        if (isPhoneTypeGsm()) {
            loge("getEnhancedVoicePrivacy: not expected on GSM");
        } else {
            this.mCi.getPreferredVoicePrivacy(message);
        }
    }

    @Override
    public void clearDisconnected() {
        this.mCT.clearDisconnected();
    }

    @Override
    public boolean canTransfer() {
        if (isPhoneTypeGsm()) {
            return this.mCT.canTransfer();
        }
        loge("canTransfer: not possible in CDMA");
        return false;
    }

    @Override
    public void explicitCallTransfer() {
        if (isPhoneTypeGsm()) {
            this.mCT.explicitCallTransfer();
        } else {
            loge("explicitCallTransfer: not possible in CDMA");
        }
    }

    @Override
    public GsmCdmaCall getForegroundCall() {
        return this.mCT.mForegroundCall;
    }

    @Override
    public GsmCdmaCall getBackgroundCall() {
        return this.mCT.mBackgroundCall;
    }

    @Override
    public Call getRingingCall() {
        Phone phone = this.mImsPhone;
        if (phone != null && phone.getRingingCall().isRinging()) {
            return phone.getRingingCall();
        }
        return this.mCT.mRingingCall;
    }

    private boolean handleCallDeflectionIncallSupplementaryService(String str) {
        if (str.length() > 1) {
            return false;
        }
        if (getRingingCall().getState() != Call.State.IDLE) {
            logd("MmiCode 0: rejectCall");
            try {
                this.mCT.rejectCall();
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "reject failed", e);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.REJECT);
            }
        } else if (getBackgroundCall().getState() != Call.State.IDLE) {
            logd("MmiCode 0: hangupWaitingOrBackground");
            this.mCT.hangupWaitingOrBackground();
        }
        return true;
    }

    protected boolean handleCallWaitingIncallSupplementaryService(String str) {
        int length = str.length();
        if (length > 2) {
            return false;
        }
        GsmCdmaCall foregroundCall = getForegroundCall();
        try {
            if (length > 1) {
                int iCharAt = str.charAt(1) - '0';
                if (iCharAt >= 1 && iCharAt <= 19) {
                    logd("MmiCode 1: hangupConnectionByIndex " + iCharAt);
                    this.mCT.hangupConnectionByIndex(foregroundCall, iCharAt);
                }
            } else if (foregroundCall.getState() != Call.State.IDLE) {
                logd("MmiCode 1: hangup foreground");
                this.mCT.hangup(foregroundCall);
            } else {
                logd("MmiCode 1: switchWaitingOrHoldingAndActive");
                this.mCT.switchWaitingOrHoldingAndActive();
            }
        } catch (CallStateException e) {
            Rlog.d(LOG_TAG, "hangup failed", e);
            notifySuppServiceFailed(PhoneInternalInterface.SuppService.HANGUP);
        }
        return true;
    }

    protected boolean handleCallHoldIncallSupplementaryService(String str) {
        int length = str.length();
        if (length > 2) {
            return false;
        }
        GsmCdmaCall foregroundCall = getForegroundCall();
        if (length > 1) {
            try {
                int iCharAt = str.charAt(1) - '0';
                GsmCdmaConnection connectionByIndex = this.mCT.getConnectionByIndex(foregroundCall, iCharAt);
                if (connectionByIndex != null && iCharAt >= 1 && iCharAt <= 19) {
                    logd("MmiCode 2: separate call " + iCharAt);
                    this.mCT.separate(connectionByIndex);
                } else {
                    logd("separate: invalid call index " + iCharAt);
                    notifySuppServiceFailed(PhoneInternalInterface.SuppService.SEPARATE);
                }
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "separate failed", e);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.SEPARATE);
            }
        } else {
            try {
                if (getRingingCall().getState() != Call.State.IDLE) {
                    logd("MmiCode 2: accept ringing call");
                    this.mCT.acceptCall();
                } else {
                    logd("MmiCode 2: switchWaitingOrHoldingAndActive");
                    this.mCT.switchWaitingOrHoldingAndActive();
                }
            } catch (CallStateException e2) {
                Rlog.d(LOG_TAG, "switch failed", e2);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.SWITCH);
            }
        }
        return true;
    }

    protected boolean handleMultipartyIncallSupplementaryService(String str) {
        if (str.length() > 1) {
            return false;
        }
        logd("MmiCode 3: merge calls");
        conference();
        return true;
    }

    protected boolean handleEctIncallSupplementaryService(String str) {
        if (str.length() != 1) {
            return false;
        }
        logd("MmiCode 4: explicit call transfer");
        explicitCallTransfer();
        return true;
    }

    protected boolean handleCcbsIncallSupplementaryService(String str) {
        if (str.length() > 1) {
            return false;
        }
        Rlog.i(LOG_TAG, "MmiCode 5: CCBS not supported!");
        notifySuppServiceFailed(PhoneInternalInterface.SuppService.UNKNOWN);
        return true;
    }

    @Override
    public boolean handleInCallMmiCommands(String str) throws CallStateException {
        if (!isPhoneTypeGsm()) {
            loge("method handleInCallMmiCommands is NOT supported in CDMA!");
            return false;
        }
        Phone phone = this.mImsPhone;
        if (phone != null && phone.getServiceState().getState() == 0) {
            return phone.handleInCallMmiCommands(str);
        }
        if (!isInCall() || TextUtils.isEmpty(str)) {
            return false;
        }
        switch (str.charAt(0)) {
            case '0':
                return handleCallDeflectionIncallSupplementaryService(str);
            case '1':
                return handleCallWaitingIncallSupplementaryService(str);
            case '2':
                return handleCallHoldIncallSupplementaryService(str);
            case '3':
                return handleMultipartyIncallSupplementaryService(str);
            case '4':
                return handleEctIncallSupplementaryService(str);
            case '5':
                return handleCcbsIncallSupplementaryService(str);
            default:
                return false;
        }
    }

    public boolean isInCall() {
        return getForegroundCall().getState().isAlive() || getBackgroundCall().getState().isAlive() || getRingingCall().getState().isAlive();
    }

    @Override
    public Connection dial(String str, PhoneInternalInterface.DialArgs dialArgs) throws CallStateException {
        if (!isPhoneTypeGsm() && dialArgs.uusInfo != null) {
            throw new CallStateException("Sending UUS information NOT supported in CDMA!");
        }
        boolean zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(getSubId(), str);
        Phone phone = this.mImsPhone;
        boolean z = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(getSubId()).getBoolean("carrier_use_ims_first_for_emergency_bool");
        boolean z2 = false;
        boolean z3 = isImsUseEnabled() && phone != null && (phone.isVolteEnabled() || phone.isWifiCallingEnabled() || (phone.isVideoEnabled() && VideoProfile.isVideo(dialArgs.videoState))) && phone.getServiceState().getState() == 0;
        boolean z4 = phone != null && zIsEmergencyNumber && z && ImsManager.getInstance(this.mContext, this.mPhoneId).isNonTtyOrTtyOnVolteEnabled() && phone.isImsAvailable();
        String strExtractNetworkPortionAlt = PhoneNumberUtils.extractNetworkPortionAlt(PhoneNumberUtils.stripSeparators(str));
        boolean z5 = (strExtractNetworkPortionAlt.startsWith(CharacterSets.MIMENAME_ANY_CHARSET) || strExtractNetworkPortionAlt.startsWith("#")) && strExtractNetworkPortionAlt.endsWith("#");
        if (phone != null && phone.isUtEnabled()) {
            z2 = true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("useImsForCall=");
        sb.append(z3);
        sb.append(", useImsForEmergency=");
        sb.append(z4);
        sb.append(", useImsForUt=");
        sb.append(z2);
        sb.append(", isUt=");
        sb.append(z5);
        sb.append(", imsPhone=");
        sb.append(phone);
        sb.append(", imsPhone.isVolteEnabled()=");
        sb.append(phone != null ? Boolean.valueOf(phone.isVolteEnabled()) : "N/A");
        sb.append(", imsPhone.isVowifiEnabled()=");
        sb.append(phone != null ? Boolean.valueOf(phone.isWifiCallingEnabled()) : "N/A");
        sb.append(", imsPhone.isVideoEnabled()=");
        sb.append(phone != null ? Boolean.valueOf(phone.isVideoEnabled()) : "N/A");
        sb.append(", imsPhone.getServiceState().getState()=");
        sb.append(phone != null ? Integer.valueOf(phone.getServiceState().getState()) : "N/A");
        logd(sb.toString());
        Phone.checkWfcWifiOnlyModeBeforeDial(this.mImsPhone, this.mPhoneId, this.mContext);
        if ((z3 && !z5) || ((z5 && z2) || z4)) {
            try {
                logd("Trying IMS PS call");
                return phone.dial(str, dialArgs);
            } catch (CallStateException e) {
                logd("IMS PS call exception " + e + "useImsForCall =" + z3 + ", imsPhone =" + phone);
                if (Phone.CS_FALLBACK.equals(e.getMessage()) || zIsEmergencyNumber) {
                    logi("IMS call failed with Exception: " + e.getMessage() + ". Falling back to CS.");
                } else {
                    CallStateException callStateException = new CallStateException(e.getMessage());
                    callStateException.setStackTrace(e.getStackTrace());
                    throw callStateException;
                }
            }
        }
        if (this.mSST != null && this.mSST.mSS.getState() == 1 && this.mSST.mSS.getDataRegState() != 0 && !zIsEmergencyNumber) {
            throw new CallStateException("cannot dial in current state");
        }
        if (this.mSST != null && this.mSST.mSS.getState() == 3 && !VideoProfile.isVideo(dialArgs.videoState) && !zIsEmergencyNumber) {
            throw new CallStateException(2, "cannot dial voice call in airplane mode");
        }
        if (this.mSST != null && this.mSST.mSS.getState() == 1 && ((this.mSST.mSS.getDataRegState() != 0 || !ServiceState.isLte(this.mSST.mSS.getRilDataRadioTechnology())) && !VideoProfile.isVideo(dialArgs.videoState) && !zIsEmergencyNumber)) {
            throw new CallStateException(1, "cannot dial voice call in out of service");
        }
        logd("Trying (non-IMS) CS call");
        if (isPhoneTypeGsm()) {
            return dialInternal(str, new PhoneInternalInterface.DialArgs.Builder().setIntentExtras(dialArgs.intentExtras).build());
        }
        return dialInternal(str, dialArgs);
    }

    public boolean isNotificationOfWfcCallRequired(String str) {
        PersistableBundle configForSubId = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(getSubId());
        if (!(configForSubId != null && configForSubId.getBoolean("notify_international_call_on_wfc_bool"))) {
            return false;
        }
        Phone phone = this.mImsPhone;
        return isImsUseEnabled() && phone != null && !phone.isVolteEnabled() && phone.isWifiCallingEnabled() && !PhoneNumberUtils.isEmergencyNumber(getSubId(), str) && PhoneNumberUtils.isInternationalNumber(str, getCountryIso());
    }

    @Override
    protected Connection dialInternal(String str, PhoneInternalInterface.DialArgs dialArgs) throws CallStateException {
        return dialInternal(str, dialArgs, null);
    }

    protected Connection dialInternal(String str, PhoneInternalInterface.DialArgs dialArgs, ResultReceiver resultReceiver) throws CallStateException {
        String strStripSeparators = PhoneNumberUtils.stripSeparators(str);
        if (isPhoneTypeGsm()) {
            if (handleInCallMmiCommands(strStripSeparators)) {
                return null;
            }
            GsmMmiCode gsmMmiCodeNewFromDialString = GsmMmiCode.newFromDialString(PhoneNumberUtils.extractNetworkPortionAlt(strStripSeparators), this, this.mUiccApplication.get(), resultReceiver);
            logd("dialInternal: dialing w/ mmi '" + gsmMmiCodeNewFromDialString + "'...");
            if (gsmMmiCodeNewFromDialString == null) {
                return this.mCT.dial(strStripSeparators, dialArgs.uusInfo, dialArgs.intentExtras);
            }
            if (gsmMmiCodeNewFromDialString.isTemporaryModeCLIR()) {
                return this.mCT.dial(gsmMmiCodeNewFromDialString.mDialingNumber, gsmMmiCodeNewFromDialString.getCLIRMode(), dialArgs.uusInfo, dialArgs.intentExtras);
            }
            this.mPendingMMIs.add(gsmMmiCodeNewFromDialString);
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, gsmMmiCodeNewFromDialString, (Throwable) null));
            gsmMmiCodeNewFromDialString.processCode();
            return null;
        }
        return this.mCT.dial(strStripSeparators);
    }

    @Override
    public boolean handlePinMmi(String str) {
        MmiCode mmiCodeNewFromDialString;
        if (isPhoneTypeGsm()) {
            mmiCodeNewFromDialString = GsmMmiCode.newFromDialString(str, this, this.mUiccApplication.get());
        } else {
            mmiCodeNewFromDialString = CdmaMmiCode.newFromDialString(str, this, this.mUiccApplication.get());
        }
        if (mmiCodeNewFromDialString != null && mmiCodeNewFromDialString.isPinPukCommand()) {
            this.mPendingMMIs.add(mmiCodeNewFromDialString);
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmiCodeNewFromDialString, (Throwable) null));
            try {
                mmiCodeNewFromDialString.processCode();
                return true;
            } catch (CallStateException e) {
                return true;
            }
        }
        loge("Mmi is null or unrecognized!");
        return false;
    }

    protected void sendUssdResponse(String str, CharSequence charSequence, int i, ResultReceiver resultReceiver) {
        UssdResponse ussdResponse = new UssdResponse(str, charSequence);
        Bundle bundle = new Bundle();
        bundle.putParcelable("USSD_RESPONSE", ussdResponse);
        resultReceiver.send(i, bundle);
    }

    @Override
    public boolean handleUssdRequest(String str, ResultReceiver resultReceiver) {
        if (!isPhoneTypeGsm() || this.mPendingMMIs.size() > 0) {
            sendUssdResponse(str, null, -1, resultReceiver);
            return true;
        }
        Phone phone = this.mImsPhone;
        if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
            try {
                logd("handleUssdRequest: attempting over IMS");
                return phone.handleUssdRequest(str, resultReceiver);
            } catch (CallStateException e) {
                if (!Phone.CS_FALLBACK.equals(e.getMessage())) {
                    return false;
                }
                logd("handleUssdRequest: fallback to CS required");
            }
        }
        try {
            dialInternal(str, new PhoneInternalInterface.DialArgs.Builder().build(), resultReceiver);
            return true;
        } catch (Exception e2) {
            logd("handleUssdRequest: exception" + e2);
            return false;
        }
    }

    @Override
    public void sendUssdResponse(String str) {
        if (isPhoneTypeGsm()) {
            GsmMmiCode gsmMmiCodeNewFromUssdUserInput = GsmMmiCode.newFromUssdUserInput(str, this, this.mUiccApplication.get());
            this.mPendingMMIs.add(gsmMmiCodeNewFromUssdUserInput);
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, gsmMmiCodeNewFromUssdUserInput, (Throwable) null));
            gsmMmiCodeNewFromUssdUserInput.sendUssd(str);
            return;
        }
        loge("sendUssdResponse: not possible in CDMA");
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("sendDtmf called with invalid character '" + c + "'");
            return;
        }
        if (this.mCT.mState == PhoneConstants.State.OFFHOOK) {
            this.mCi.sendDtmf(c, null);
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("startDtmf called with invalid character '" + c + "'");
            return;
        }
        this.mCi.startDtmf(c, null);
    }

    @Override
    public void stopDtmf() {
        this.mCi.stopDtmf(null);
    }

    @Override
    public void sendBurstDtmf(String str, int i, int i2, Message message) {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] sendBurstDtmf() is a CDMA method");
            return;
        }
        boolean z = false;
        int i3 = 0;
        while (true) {
            if (i3 < str.length()) {
                if (PhoneNumberUtils.is12Key(str.charAt(i3))) {
                    i3++;
                } else {
                    Rlog.e(LOG_TAG, "sendDtmf called with invalid character '" + str.charAt(i3) + "'");
                    break;
                }
            } else {
                z = true;
                break;
            }
        }
        if (this.mCT.mState == PhoneConstants.State.OFFHOOK && z) {
            this.mCi.sendBurstDtmf(str, i, i2, message);
        }
    }

    @Override
    public void setRadioPower(boolean z) {
        this.mSST.setRadioPower(z);
    }

    private void storeVoiceMailNumber(String str) {
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        if (isPhoneTypeGsm()) {
            editorEdit.putString(VM_NUMBER + getPhoneId(), str);
            editorEdit.apply();
            setVmSimImsi(getSubscriberId());
            return;
        }
        editorEdit.putString(VM_NUMBER_CDMA + getPhoneId(), str);
        editorEdit.apply();
    }

    @Override
    public String getVoiceMailNumber() {
        String string;
        PersistableBundle config;
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            string = iccRecords != null ? iccRecords.getVoiceMailNumber() : "";
            if (TextUtils.isEmpty(string)) {
                string = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(VM_NUMBER + getPhoneId(), null);
            }
        } else {
            string = PreferenceManager.getDefaultSharedPreferences(getContext()).getString(VM_NUMBER_CDMA + getPhoneId(), null);
        }
        if (TextUtils.isEmpty(string) && (config = ((CarrierConfigManager) getContext().getSystemService("carrier_config")).getConfig()) != null) {
            string = config.getString("default_vm_number_string");
            String string2 = config.getString("default_vm_number_roaming_string");
            if (!TextUtils.isEmpty(string2) && this.mSST.mSS.getRoaming()) {
                string = string2;
            }
        }
        if (!isPhoneTypeGsm() && TextUtils.isEmpty(string)) {
            PersistableBundle config2 = ((CarrierConfigManager) getContext().getSystemService("carrier_config")).getConfig();
            if (config2 != null && config2.getBoolean("config_telephony_use_own_number_for_voicemail_bool")) {
                return getLine1Number();
            }
            return "*86";
        }
        return string;
    }

    private String getVmSimImsi() {
        return PreferenceManager.getDefaultSharedPreferences(getContext()).getString(VM_SIM_IMSI + getPhoneId(), null);
    }

    private void setVmSimImsi(String str) {
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editorEdit.putString(VM_SIM_IMSI + getPhoneId(), str);
        editorEdit.apply();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        String voiceMailAlphaTag = "";
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            voiceMailAlphaTag = iccRecords != null ? iccRecords.getVoiceMailAlphaTag() : "";
        }
        if (voiceMailAlphaTag == null || voiceMailAlphaTag.length() == 0) {
            return this.mContext.getText(R.string.defaultVoiceMailAlphaTag).toString();
        }
        return voiceMailAlphaTag;
    }

    @Override
    public String getDeviceId() {
        if (isPhoneTypeGsm()) {
            return this.mImei;
        }
        if (((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(getSubId()).getBoolean("force_imei_bool")) {
            return this.mImei;
        }
        String meid = getMeid();
        if (meid == null || meid.matches("^0*$")) {
            loge("getDeviceId(): MEID is not initialized use ESN");
            return getEsn();
        }
        return meid;
    }

    @Override
    public String getDeviceSvn() {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            return this.mImeiSv;
        }
        loge("getDeviceSvn(): return 0");
        return "0";
    }

    @Override
    public IsimRecords getIsimRecords() {
        return this.mIsimUiccRecords;
    }

    @Override
    public String getImei() {
        return this.mImei;
    }

    @Override
    public String getEsn() {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] getEsn() is a CDMA method");
            return "0";
        }
        return this.mEsn;
    }

    @Override
    public String getMeid() {
        return this.mMeid;
    }

    @Override
    public String getNai() {
        IccRecords iccRecords = this.mUiccController.getIccRecords(this.mPhoneId, 2);
        if (Log.isLoggable(LOG_TAG, 2)) {
            Rlog.v(LOG_TAG, "IccRecords is " + iccRecords);
        }
        if (iccRecords != null) {
            return iccRecords.getNAI();
        }
        return null;
    }

    @Override
    public String getSubscriberId() {
        if (isPhoneTypeCdma()) {
            return this.mSST.getImsi();
        }
        IccRecords iccRecords = this.mUiccController.getIccRecords(this.mPhoneId, 1);
        if (iccRecords != null) {
            return iccRecords.getIMSI();
        }
        return null;
    }

    @Override
    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int i) {
        return CarrierInfoManager.getCarrierInfoForImsiEncryption(i, this.mContext);
    }

    @Override
    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo) {
        CarrierInfoManager.setCarrierInfoForImsiEncryption(imsiEncryptionInfo, this.mContext, this.mPhoneId);
    }

    @Override
    public int getCarrierId() {
        return this.mCarrerIdentifier.getCarrierId();
    }

    @Override
    public String getCarrierName() {
        return this.mCarrerIdentifier.getCarrierName();
    }

    @Override
    public int getCarrierIdListVersion() {
        return this.mCarrerIdentifier.getCarrierListVersion();
    }

    @Override
    public void resetCarrierKeysForImsiEncryption() {
        this.mCIM.resetCarrierKeysForImsiEncryption(this.mContext, this.mPhoneId);
    }

    @Override
    public void setCarrierTestOverride(String str, String str2, String str3, String str4, String str5, String str6, String str7) {
        IccRecords iccRecords;
        if (isPhoneTypeGsm()) {
            iccRecords = this.mIccRecords.get();
        } else if (isPhoneTypeCdmaLte()) {
            iccRecords = this.mSimRecords;
        } else {
            loge("setCarrierTestOverride fails in CDMA only");
            iccRecords = null;
        }
        IccRecords iccRecords2 = iccRecords;
        if (iccRecords2 != null) {
            iccRecords2.setCarrierTestOverride(str, str2, str3, str4, str5, str6, str7);
        }
    }

    @Override
    public String getGroupIdLevel1() {
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                return iccRecords.getGid1();
            }
            return null;
        }
        if (!isPhoneTypeCdma()) {
            return this.mSimRecords != null ? this.mSimRecords.getGid1() : "";
        }
        loge("GID1 is not available in CDMA");
        return null;
    }

    @Override
    public String getGroupIdLevel2() {
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                return iccRecords.getGid2();
            }
            return null;
        }
        if (!isPhoneTypeCdma()) {
            return this.mSimRecords != null ? this.mSimRecords.getGid2() : "";
        }
        loge("GID2 is not available in CDMA");
        return null;
    }

    @Override
    public String getLine1Number() {
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                return iccRecords.getMsisdnNumber();
            }
            return null;
        }
        return this.mSST.getMdnNumber();
    }

    @Override
    public String getPlmn() {
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                return iccRecords.getPnnHomeName();
            }
            return null;
        }
        if (isPhoneTypeCdma()) {
            loge("Plmn is not available in CDMA");
            return null;
        }
        if (this.mSimRecords != null) {
            return this.mSimRecords.getPnnHomeName();
        }
        return null;
    }

    @Override
    public String getCdmaPrlVersion() {
        return this.mSST.getPrlVersion();
    }

    @Override
    public String getCdmaMin() {
        return this.mSST.getCdmaMin();
    }

    @Override
    public boolean isMinInfoReady() {
        return this.mSST.isMinInfoReady();
    }

    @Override
    public String getMsisdn() {
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                return iccRecords.getMsisdnNumber();
            }
            return null;
        }
        if (isPhoneTypeCdmaLte()) {
            if (this.mSimRecords != null) {
                return this.mSimRecords.getMsisdnNumber();
            }
            return null;
        }
        loge("getMsisdn: not expected on CDMA");
        return null;
    }

    @Override
    public String getLine1AlphaTag() {
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                return iccRecords.getMsisdnAlphaTag();
            }
            return null;
        }
        loge("getLine1AlphaTag: not possible in CDMA");
        return null;
    }

    @Override
    public boolean setLine1Number(String str, String str2, Message message) {
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords == null) {
                return false;
            }
            iccRecords.setMsisdnNumber(str, str2, message);
            return true;
        }
        loge("setLine1Number: not possible in CDMA");
        return false;
    }

    @Override
    public void setVoiceMailNumber(String str, String str2, Message message) {
        this.mVmNumber = str2;
        Message messageObtainMessage = obtainMessage(20, 0, 0, message);
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            iccRecords.setVoiceMailNumber(str, this.mVmNumber, messageObtainMessage);
        }
    }

    protected boolean isValidCommandInterfaceCFReason(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    @Override
    public String getSystemProperty(String str, String str2) {
        if (getUnitTestMode()) {
            return null;
        }
        return TelephonyManager.getTelephonyProperty(this.mPhoneId, str, str2);
    }

    protected boolean isValidCommandInterfaceCFAction(int i) {
        switch (i) {
            case 0:
            case 1:
            case 3:
            case 4:
                return true;
            case 2:
            default:
                return false;
        }
    }

    protected boolean isCfEnable(int i) {
        return i == 1 || i == 3;
    }

    @Override
    public void getCallForwardingOption(int i, Message message) {
        if (isPhoneTypeGsm()) {
            Phone phone = this.mImsPhone;
            if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                phone.getCallForwardingOption(i, message);
                return;
            } else {
                if (isValidCommandInterfaceCFReason(i)) {
                    logd("requesting call forwarding query.");
                    if (i == 0) {
                        message = obtainMessage(13, message);
                    }
                    this.mCi.queryCallForwardStatus(i, 1, null, message);
                    return;
                }
                return;
            }
        }
        loge("getCallForwardingOption: not possible in CDMA");
    }

    @Override
    public void setCallForwardingOption(int i, int i2, String str, int i3, Message message) {
        if (isPhoneTypeGsm()) {
            Phone phone = this.mImsPhone;
            if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                phone.setCallForwardingOption(i, i2, str, i3, message);
                return;
            }
            if (isValidCommandInterfaceCFAction(i) && isValidCommandInterfaceCFReason(i2)) {
                if (i2 == 0) {
                    message = obtainMessage(12, isCfEnable(i) ? 1 : 0, 0, new Cfu(str, message));
                }
                this.mCi.setCallForward(i, i2, 1, str, i3, message);
                return;
            }
            return;
        }
        loge("setCallForwardingOption: not possible in CDMA");
    }

    @Override
    public void getCallBarring(String str, String str2, Message message, int i) {
        if (isPhoneTypeGsm()) {
            Phone phone = this.mImsPhone;
            if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                phone.getCallBarring(str, str2, message, i);
                return;
            } else {
                this.mCi.queryFacilityLock(str, str2, i, message);
                return;
            }
        }
        loge("getCallBarringOption: not possible in CDMA");
    }

    @Override
    public void setCallBarring(String str, boolean z, String str2, Message message, int i) {
        if (isPhoneTypeGsm()) {
            Phone phone = this.mImsPhone;
            if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                phone.setCallBarring(str, z, str2, message, i);
                return;
            } else {
                this.mCi.setFacilityLock(str, z, str2, i, message);
                return;
            }
        }
        loge("setCallBarringOption: not possible in CDMA");
    }

    public void changeCallBarringPassword(String str, String str2, String str3, Message message) {
        if (isPhoneTypeGsm()) {
            this.mCi.changeBarringPassword(str, str2, str3, message);
        } else {
            loge("changeCallBarringPassword: not possible in CDMA");
        }
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message message) {
        if (isPhoneTypeGsm()) {
            Phone phone = this.mImsPhone;
            if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                phone.getOutgoingCallerIdDisplay(message);
                return;
            } else {
                this.mCi.getCLIR(message);
                return;
            }
        }
        loge("getOutgoingCallerIdDisplay: not possible in CDMA");
    }

    @Override
    public void setOutgoingCallerIdDisplay(int i, Message message) {
        if (isPhoneTypeGsm()) {
            Phone phone = this.mImsPhone;
            if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                phone.setOutgoingCallerIdDisplay(i, message);
                return;
            } else {
                this.mCi.setCLIR(i, obtainMessage(18, i, 0, message));
                return;
            }
        }
        loge("setOutgoingCallerIdDisplay: not possible in CDMA");
    }

    @Override
    public void getCallWaiting(Message message) {
        if (isPhoneTypeGsm()) {
            Phone phone = this.mImsPhone;
            if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                phone.getCallWaiting(message);
                return;
            } else {
                this.mCi.queryCallWaiting(0, message);
                return;
            }
        }
        this.mCi.queryCallWaiting(1, message);
    }

    @Override
    public void setCallWaiting(boolean z, Message message) {
        if (isPhoneTypeGsm()) {
            Phone phone = this.mImsPhone;
            if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                phone.setCallWaiting(z, message);
                return;
            } else {
                this.mCi.setCallWaiting(z, 1, message);
                return;
            }
        }
        loge("method setCallWaiting is NOT supported in CDMA!");
    }

    @Override
    public void getAvailableNetworks(Message message) {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            this.mCi.getAvailableNetworks(message);
        } else {
            loge("getAvailableNetworks: not possible in CDMA");
        }
    }

    @Override
    public void startNetworkScan(NetworkScanRequest networkScanRequest, Message message) {
        this.mCi.startNetworkScan(networkScanRequest, message);
    }

    @Override
    public void stopNetworkScan(Message message) {
        this.mCi.stopNetworkScan(message);
    }

    @Override
    public void getNeighboringCids(Message message, WorkSource workSource) {
        if (isPhoneTypeGsm()) {
            this.mCi.getNeighboringCids(message, workSource);
        } else if (message != null) {
            AsyncResult.forMessage(message).exception = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
            message.sendToTarget();
        }
    }

    @Override
    public void setTTYMode(int i, Message message) {
        super.setTTYMode(i, message);
        if (this.mImsPhone != null) {
            this.mImsPhone.setTTYMode(i, message);
        }
    }

    @Override
    public void setUiTTYMode(int i, Message message) {
        if (this.mImsPhone != null) {
            this.mImsPhone.setUiTTYMode(i, message);
        }
    }

    @Override
    public void setMute(boolean z) {
        this.mCT.setMute(z);
    }

    @Override
    public boolean getMute() {
        return this.mCT.getMute();
    }

    @Override
    public void updateServiceLocation() {
        this.mSST.enableSingleLocationUpdate();
    }

    @Override
    public void enableLocationUpdates() {
        this.mSST.enableLocationUpdates();
    }

    @Override
    public void disableLocationUpdates() {
        this.mSST.disableLocationUpdates();
    }

    @Override
    public boolean getDataRoamingEnabled() {
        return this.mDcTracker.getDataRoamingEnabled();
    }

    @Override
    public void setDataRoamingEnabled(boolean z) {
        this.mDcTracker.setDataRoamingEnabledByUser(z);
    }

    @Override
    public void registerForCdmaOtaStatusChange(Handler handler, int i, Object obj) {
        this.mCi.registerForCdmaOtaProvision(handler, i, obj);
    }

    @Override
    public void unregisterForCdmaOtaStatusChange(Handler handler) {
        this.mCi.unregisterForCdmaOtaProvision(handler);
    }

    @Override
    public void registerForSubscriptionInfoReady(Handler handler, int i, Object obj) {
        this.mSST.registerForSubscriptionInfoReady(handler, i, obj);
    }

    @Override
    public void unregisterForSubscriptionInfoReady(Handler handler) {
        this.mSST.unregisterForSubscriptionInfoReady(handler);
    }

    @Override
    public void setOnEcbModeExitResponse(Handler handler, int i, Object obj) {
        this.mEcmExitRespRegistrant = new Registrant(handler, i, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler handler) {
        this.mEcmExitRespRegistrant.clear();
    }

    @Override
    public void registerForCallWaiting(Handler handler, int i, Object obj) {
        this.mCT.registerForCallWaiting(handler, i, obj);
    }

    @Override
    public void unregisterForCallWaiting(Handler handler) {
        this.mCT.unregisterForCallWaiting(handler);
    }

    @Override
    public boolean isUserDataEnabled() {
        return this.mDcTracker.isUserDataEnabled();
    }

    @Override
    public boolean isDataEnabled() {
        return this.mDcTracker.isDataEnabled();
    }

    @Override
    public void setUserDataEnabled(boolean z) {
        this.mDcTracker.setUserDataEnabled(z);
    }

    public void onMMIDone(MmiCode mmiCode) {
        if (this.mPendingMMIs.remove(mmiCode) || (isPhoneTypeGsm() && (mmiCode.isUssdRequest() || ((GsmMmiCode) mmiCode).isSsInfo()))) {
            ResultReceiver ussdCallbackReceiver = mmiCode.getUssdCallbackReceiver();
            if (ussdCallbackReceiver != null) {
                Rlog.i(LOG_TAG, "onMMIDone: invoking callback: " + mmiCode);
                sendUssdResponse(mmiCode.getDialString(), mmiCode.getMessage(), mmiCode.getState() == MmiCode.State.COMPLETE ? 100 : -1, ussdCallbackReceiver);
                return;
            }
            Rlog.i(LOG_TAG, "onMMIDone: notifying registrants: " + mmiCode);
            this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmiCode, (Throwable) null));
            return;
        }
        Rlog.i(LOG_TAG, "onMMIDone: invalid response or already handled; ignoring: " + mmiCode);
    }

    public boolean supports3gppCallForwardingWhileRoaming() {
        PersistableBundle config = ((CarrierConfigManager) getContext().getSystemService("carrier_config")).getConfig();
        if (config != null) {
            return config.getBoolean("support_3gpp_call_forwarding_while_roaming_bool", true);
        }
        return true;
    }

    protected void onNetworkInitiatedUssd(MmiCode mmiCode) {
        Rlog.v(LOG_TAG, "onNetworkInitiatedUssd: mmi=" + mmiCode);
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmiCode, (Throwable) null));
    }

    protected void onIncomingUSSD(int i, String str) {
        if (!isPhoneTypeGsm()) {
            loge("onIncomingUSSD: not expected on GSM");
        }
        int i2 = 0;
        boolean z = i == 1;
        boolean z2 = (i == 0 || i == 1) ? false : true;
        boolean z3 = i == 2;
        GsmMmiCode gsmMmiCode = null;
        int size = this.mPendingMMIs.size();
        while (true) {
            if (i2 >= size) {
                break;
            }
            if (!((GsmMmiCode) this.mPendingMMIs.get(i2)).isPendingUSSD()) {
                i2++;
            } else {
                gsmMmiCode = (GsmMmiCode) this.mPendingMMIs.get(i2);
                break;
            }
        }
        if (gsmMmiCode != null) {
            if (z3) {
                gsmMmiCode.onUssdRelease();
                return;
            } else if (z2) {
                gsmMmiCode.onUssdFinishedError();
                return;
            } else {
                gsmMmiCode.onUssdFinished(str, z);
                return;
            }
        }
        if (!z2 && str != null) {
            onNetworkInitiatedUssd(GsmMmiCode.newNetworkInitiatedUssd(str, z, this, this.mUiccApplication.get()));
        }
    }

    protected void syncClirSetting() {
        int i = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(Phone.CLIR_KEY + getPhoneId(), -1);
        Rlog.i(LOG_TAG, "syncClirSetting: clir_key" + getPhoneId() + "=" + i);
        if (i >= 0) {
            this.mCi.setCLIR(i, null);
        }
    }

    private void handleRadioAvailable() {
        this.mCi.getBasebandVersion(obtainMessage(6));
        this.mCi.getDeviceIdentity(obtainMessage(21));
        this.mCi.getRadioCapability(obtainMessage(35));
        startLceAfterRadioIsAvailable();
    }

    private void handleRadioOn() {
        this.mCi.getVoiceRadioTechnology(obtainMessage(40));
        if (!isPhoneTypeGsm()) {
            this.mCdmaSubscriptionSource = this.mCdmaSSM.getCdmaSubscriptionSource();
        }
        setPreferredNetworkTypeIfSimLoaded();
    }

    private void handleRadioOffOrNotAvailable() {
        if (isPhoneTypeGsm()) {
            for (int size = this.mPendingMMIs.size() - 1; size >= 0; size--) {
                if (((GsmMmiCode) this.mPendingMMIs.get(size)).isPendingUSSD()) {
                    ((GsmMmiCode) this.mPendingMMIs.get(size)).onUssdFinishedError();
                }
            }
        }
        this.mRadioOffOrNotAvailableRegistrants.notifyRegistrants();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
                handleRadioAvailable();
                break;
            case 2:
                logd("Event EVENT_SSN Received");
                if (isPhoneTypeGsm()) {
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    this.mSsnRegistrants.notifyRegistrants(asyncResult);
                }
                break;
            case 3:
                updateCurrentCarrierInProvider();
                String vmSimImsi = getVmSimImsi();
                String subscriberId = getSubscriberId();
                if ((!isPhoneTypeGsm() || vmSimImsi != null) && subscriberId != null && !subscriberId.equals(vmSimImsi)) {
                    storeVoiceMailNumber(null);
                    setVmSimImsi(null);
                }
                this.mSimRecordsLoadedRegistrants.notifyRegistrants();
                break;
            case 4:
            case 11:
            case 14:
            case 15:
            case 16:
            case 17:
            case 23:
            case 24:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 37:
            case 38:
            default:
                super.handleMessage(message);
                break;
            case 5:
                logd("Event EVENT_RADIO_ON Received");
                handleRadioOn();
                break;
            case 6:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception == null) {
                    logd("Baseband version: " + asyncResult2.result);
                    TelephonyManager.from(this.mContext).setBasebandVersionForPhone(getPhoneId(), (String) asyncResult2.result);
                    break;
                }
                break;
            case 7:
                String[] strArr = (String[]) ((AsyncResult) message.obj).result;
                if (strArr.length > 1) {
                    try {
                        onIncomingUSSD(Integer.parseInt(strArr[0]), strArr[1]);
                    } catch (NumberFormatException e) {
                        Rlog.w(LOG_TAG, "error parsing USSD");
                        return;
                    }
                }
                break;
            case 8:
                logd("Event EVENT_RADIO_OFF_OR_NOT_AVAILABLE Received");
                handleRadioOffOrNotAvailable();
                break;
            case 9:
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                if (asyncResult3.exception == null) {
                    this.mImei = (String) asyncResult3.result;
                    break;
                }
                break;
            case 10:
                AsyncResult asyncResult4 = (AsyncResult) message.obj;
                if (asyncResult4.exception == null) {
                    this.mImeiSv = (String) asyncResult4.result;
                    break;
                }
                break;
            case 12:
                AsyncResult asyncResult5 = (AsyncResult) message.obj;
                IccRecords iccRecords = this.mIccRecords.get();
                Cfu cfu = (Cfu) asyncResult5.userObj;
                if (asyncResult5.exception == null && iccRecords != null) {
                    setVoiceCallForwardingFlag(1, message.arg1 == 1, cfu.mSetCfNumber);
                }
                if (cfu.mOnComplete != null) {
                    AsyncResult.forMessage(cfu.mOnComplete, asyncResult5.result, asyncResult5.exception);
                    cfu.mOnComplete.sendToTarget();
                }
                break;
            case 13:
                AsyncResult asyncResult6 = (AsyncResult) message.obj;
                if (asyncResult6.exception == null) {
                    handleCfuQueryResult((CallForwardInfo[]) asyncResult6.result);
                }
                Message message2 = (Message) asyncResult6.userObj;
                if (message2 != null) {
                    AsyncResult.forMessage(message2, asyncResult6.result, asyncResult6.exception);
                    message2.sendToTarget();
                }
                break;
            case 18:
                AsyncResult asyncResult7 = (AsyncResult) message.obj;
                if (asyncResult7.exception == null) {
                    saveClirSetting(message.arg1);
                }
                Message message3 = (Message) asyncResult7.userObj;
                if (message3 != null) {
                    AsyncResult.forMessage(message3, asyncResult7.result, asyncResult7.exception);
                    message3.sendToTarget();
                }
                break;
            case 19:
                logd("Event EVENT_REGISTERED_TO_NETWORK Received");
                if (isPhoneTypeGsm()) {
                    syncClirSetting();
                }
                break;
            case 20:
                AsyncResult asyncResult8 = (AsyncResult) message.obj;
                if ((isPhoneTypeGsm() && IccVmNotSupportedException.class.isInstance(asyncResult8.exception)) || (!isPhoneTypeGsm() && IccException.class.isInstance(asyncResult8.exception))) {
                    storeVoiceMailNumber(this.mVmNumber);
                    asyncResult8.exception = null;
                }
                Message message4 = (Message) asyncResult8.userObj;
                if (message4 != null) {
                    AsyncResult.forMessage(message4, asyncResult8.result, asyncResult8.exception);
                    message4.sendToTarget();
                }
                break;
            case 21:
                AsyncResult asyncResult9 = (AsyncResult) message.obj;
                if (asyncResult9.exception == null) {
                    String[] strArr2 = (String[]) asyncResult9.result;
                    this.mImei = strArr2[0];
                    this.mImeiSv = strArr2[1];
                    this.mEsn = strArr2[2];
                    this.mMeid = strArr2[3];
                    break;
                }
                break;
            case 22:
                logd("Event EVENT_RUIM_RECORDS_LOADED Received");
                updateCurrentCarrierInProvider();
                break;
            case 25:
                handleEnterEmergencyCallbackMode(message);
                break;
            case 26:
                handleExitEmergencyCallbackMode(message);
                break;
            case 27:
                logd("EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED");
                this.mCdmaSubscriptionSource = this.mCdmaSSM.getCdmaSubscriptionSource();
                break;
            case 28:
                AsyncResult asyncResult10 = (AsyncResult) message.obj;
                if (this.mSST.mSS.getIsManualSelection()) {
                    setNetworkSelectionModeAutomatic((Message) asyncResult10.result);
                    logd("SET_NETWORK_SELECTION_AUTOMATIC: set to automatic");
                } else {
                    logd("SET_NETWORK_SELECTION_AUTOMATIC: already automatic, ignore");
                }
                break;
            case 29:
                processIccRecordEvents(((Integer) ((AsyncResult) message.obj).result).intValue());
                break;
            case 35:
                AsyncResult asyncResult11 = (AsyncResult) message.obj;
                RadioCapability radioCapability = (RadioCapability) asyncResult11.result;
                if (asyncResult11.exception != null) {
                    Rlog.d(LOG_TAG, "get phone radio capability fail, no need to change mRadioCapability");
                } else {
                    radioCapabilityUpdated(radioCapability);
                }
                Rlog.d(LOG_TAG, "EVENT_GET_RADIO_CAPABILITY: phone rc: " + radioCapability);
                break;
            case 36:
                AsyncResult asyncResult12 = (AsyncResult) message.obj;
                logd("Event EVENT_SS received");
                if (isPhoneTypeGsm()) {
                    new GsmMmiCode(this, this.mUiccApplication.get()).processSsData(asyncResult12);
                }
                break;
            case 39:
            case 40:
                String str = message.what == 39 ? "EVENT_VOICE_RADIO_TECH_CHANGED" : "EVENT_REQUEST_VOICE_RADIO_TECH_DONE";
                AsyncResult asyncResult13 = (AsyncResult) message.obj;
                if (asyncResult13.exception != null) {
                    loge(str + ": exception=" + asyncResult13.exception);
                } else if (asyncResult13.result == null || ((int[]) asyncResult13.result).length == 0) {
                    loge(str + ": has no tech!");
                } else {
                    int i = ((int[]) asyncResult13.result)[0];
                    logd(str + ": newVoiceTech=" + i);
                    phoneObjectUpdater(i);
                }
                break;
            case 41:
                AsyncResult asyncResult14 = (AsyncResult) message.obj;
                if (asyncResult14.exception == null && asyncResult14.result != null) {
                    this.mRilVersion = ((Integer) asyncResult14.result).intValue();
                } else {
                    logd("Unexpected exception on EVENT_RIL_CONNECTED");
                    this.mRilVersion = -1;
                }
                break;
            case 42:
                phoneObjectUpdater(message.arg1);
                break;
            case 43:
                if (!this.mContext.getResources().getBoolean(R.^attr-private.pointerIconHelp)) {
                    this.mCi.getVoiceRadioTechnology(obtainMessage(40));
                }
                ImsManager imsManager = ImsManager.getInstance(this.mContext, this.mPhoneId);
                if (imsManager.isServiceAvailable()) {
                    imsManager.updateImsServiceConfig(true);
                } else {
                    logd("ImsManager is not available to update CarrierConfig.");
                }
                PersistableBundle configForSubId = ((CarrierConfigManager) getContext().getSystemService("carrier_config")).getConfigForSubId(getSubId());
                if (configForSubId != null) {
                    boolean z = configForSubId.getBoolean("broadcast_emergency_call_state_changes_bool");
                    logd("broadcastEmergencyCallStateChanges = " + z);
                    setBroadcastEmergencyCallStateChanges(z);
                } else {
                    loge("didn't get broadcastEmergencyCallStateChanges from carrier config");
                }
                if (configForSubId != null) {
                    int i2 = configForSubId.getInt("cdma_roaming_mode_int");
                    int i3 = Settings.Global.getInt(getContext().getContentResolver(), "roaming_settings", -1);
                    switch (i2) {
                        case -1:
                            if (i3 != i2) {
                                logd("cdma_roaming_mode is going to changed to " + i3);
                                setCdmaRoamingPreference(i3, obtainMessage(44));
                            }
                            loge("Invalid cdma_roaming_mode settings: " + i2);
                            break;
                        case 0:
                        case 1:
                        case 2:
                            logd("cdma_roaming_mode is going to changed to " + i2);
                            setCdmaRoamingPreference(i2, obtainMessage(44));
                            break;
                        default:
                            loge("Invalid cdma_roaming_mode settings: " + i2);
                            break;
                    }
                } else {
                    loge("didn't get the cdma_roaming_mode changes from the carrier config.");
                }
                prepareEri();
                this.mSST.pollState();
                break;
            case 44:
                logd("cdma_roaming_mode change is done");
                break;
            case 45:
                logd("Event EVENT_MODEM_RESET Received isInEcm = " + isInEcm() + " isPhoneTypeGsm = " + isPhoneTypeGsm() + " mImsPhone = " + this.mImsPhone);
                if (isInEcm()) {
                    if (isPhoneTypeGsm()) {
                        if (this.mImsPhone != null) {
                            this.mImsPhone.handleExitEmergencyCallbackMode();
                        }
                    } else {
                        handleExitEmergencyCallbackMode(message);
                    }
                }
                break;
        }
    }

    public UiccCardApplication getUiccCardApplication() {
        if (isPhoneTypeGsm()) {
            return this.mUiccController.getUiccCardApplication(this.mPhoneId, 1);
        }
        return this.mUiccController.getUiccCardApplication(this.mPhoneId, 2);
    }

    @Override
    protected void onUpdateIccAvailability() {
        IsimUiccRecords isimUiccRecords;
        SIMRecords sIMRecords;
        if (this.mUiccController == null) {
            return;
        }
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            UiccCardApplication uiccCardApplication = this.mUiccController.getUiccCardApplication(this.mPhoneId, 3);
            if (uiccCardApplication != null) {
                isimUiccRecords = (IsimUiccRecords) uiccCardApplication.getIccRecords();
                logd("New ISIM application found");
            } else {
                isimUiccRecords = null;
            }
            this.mIsimUiccRecords = isimUiccRecords;
        }
        if (this.mSimRecords != null) {
            this.mSimRecords.unregisterForRecordsLoaded(this);
        }
        if (isPhoneTypeCdmaLte() || isPhoneTypeCdma()) {
            UiccCardApplication uiccCardApplication2 = this.mUiccController.getUiccCardApplication(this.mPhoneId, 1);
            if (uiccCardApplication2 != null) {
                sIMRecords = (SIMRecords) uiccCardApplication2.getIccRecords();
            } else {
                sIMRecords = null;
            }
            this.mSimRecords = sIMRecords;
            if (this.mSimRecords != null) {
                this.mSimRecords.registerForRecordsLoaded(this, 3, null);
            }
        } else {
            this.mSimRecords = null;
        }
        UiccCardApplication uiccCardApplication3 = getUiccCardApplication();
        if (!isPhoneTypeGsm() && uiccCardApplication3 == null) {
            logd("can't find 3GPP2 application; trying APP_FAM_3GPP");
            uiccCardApplication3 = this.mUiccController.getUiccCardApplication(this.mPhoneId, 1);
        }
        UiccCardApplication uiccCardApplication4 = this.mUiccApplication.get();
        if (uiccCardApplication4 != uiccCardApplication3) {
            if (uiccCardApplication4 != null) {
                logd("Removing stale icc objects.");
                if (this.mIccRecords.get() != null) {
                    unregisterForIccRecordEvents();
                    this.mIccPhoneBookIntManager.updateIccRecords(null);
                }
                this.mIccRecords.set(null);
                this.mUiccApplication.set(null);
            }
            if (uiccCardApplication3 != null) {
                logd("New Uicc application found. type = " + uiccCardApplication3.getType());
                this.mUiccApplication.set(uiccCardApplication3);
                this.mIccRecords.set(uiccCardApplication3.getIccRecords());
                registerForIccRecordEvents();
                this.mIccPhoneBookIntManager.updateIccRecords(this.mIccRecords.get());
            }
        }
    }

    protected void processIccRecordEvents(int i) {
        if (i == 1) {
            logi("processIccRecordEvents: EVENT_CFI");
            notifyCallForwardingIndicator();
        }
    }

    @Override
    public boolean updateCurrentCarrierInProvider() {
        long defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        String operatorNumeric = getOperatorNumeric();
        logd("updateCurrentCarrierInProvider: mSubId = " + getSubId() + " currentDds = " + defaultDataSubscriptionId + " operatorNumeric = " + operatorNumeric);
        if (!TextUtils.isEmpty(operatorNumeric) && getSubId() == defaultDataSubscriptionId) {
            try {
                Uri uriWithAppendedPath = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current");
                ContentValues contentValues = new ContentValues();
                contentValues.put("numeric", operatorNumeric);
                this.mContext.getContentResolver().insert(uriWithAppendedPath, contentValues);
                return true;
            } catch (SQLException e) {
                Rlog.e(LOG_TAG, "Can't store current operator", e);
                return false;
            }
        }
        return false;
    }

    private boolean updateCurrentCarrierInProvider(String str) {
        if (isPhoneTypeCdma() || (isPhoneTypeCdmaLte() && this.mUiccController.getUiccCardApplication(this.mPhoneId, 1) == null)) {
            logd("CDMAPhone: updateCurrentCarrierInProvider called");
            if (!TextUtils.isEmpty(str)) {
                try {
                    Uri uriWithAppendedPath = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current");
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("numeric", str);
                    logd("updateCurrentCarrierInProvider from system: numeric=" + str);
                    getContext().getContentResolver().insert(uriWithAppendedPath, contentValues);
                    logd("update mccmnc=" + str);
                    MccTable.updateMccMncConfiguration(this.mContext, str, false);
                    return true;
                } catch (SQLException e) {
                    Rlog.e(LOG_TAG, "Can't store current operator", e);
                }
            }
            return false;
        }
        logd("updateCurrentCarrierInProvider not updated X retVal=true");
        return true;
    }

    protected void handleCfuQueryResult(CallForwardInfo[] callForwardInfoArr) {
        if (this.mIccRecords.get() != null) {
            if (callForwardInfoArr == null || callForwardInfoArr.length == 0) {
                setVoiceCallForwardingFlag(1, false, null);
                return;
            }
            int length = callForwardInfoArr.length;
            for (int i = 0; i < length; i++) {
                if ((callForwardInfoArr[i].serviceClass & 1) != 0) {
                    setVoiceCallForwardingFlag(1, callForwardInfoArr[i].status == 1, callForwardInfoArr[i].number);
                    return;
                }
            }
        }
    }

    @Override
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return this.mIccPhoneBookIntManager;
    }

    public void registerForEriFileLoaded(Handler handler, int i, Object obj) {
        this.mEriFileLoadedRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForEriFileLoaded(Handler handler) {
        this.mEriFileLoadedRegistrants.remove(handler);
    }

    public void prepareEri() {
        if (this.mEriManager == null) {
            Rlog.e(LOG_TAG, "PrepareEri: Trying to access stale objects");
            return;
        }
        this.mEriManager.loadEriFile();
        if (this.mEriManager.isEriFileLoaded()) {
            logd("ERI read, notify registrants");
            this.mEriFileLoadedRegistrants.notifyRegistrants();
        }
    }

    public boolean isEriFileLoaded() {
        return this.mEriManager.isEriFileLoaded();
    }

    @Override
    public void activateCellBroadcastSms(int i, Message message) {
        loge("[GsmCdmaPhone] activateCellBroadcastSms() is obsolete; use SmsManager");
        message.sendToTarget();
    }

    @Override
    public void getCellBroadcastSmsConfig(Message message) {
        loge("[GsmCdmaPhone] getCellBroadcastSmsConfig() is obsolete; use SmsManager");
        message.sendToTarget();
    }

    @Override
    public void setCellBroadcastSmsConfig(int[] iArr, Message message) {
        loge("[GsmCdmaPhone] setCellBroadcastSmsConfig() is obsolete; use SmsManager");
        message.sendToTarget();
    }

    @Override
    public boolean needsOtaServiceProvisioning() {
        return (isPhoneTypeGsm() || this.mSST.getOtasp() == 3) ? false : true;
    }

    @Override
    public boolean isCspPlmnEnabled() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            return iccRecords.isCspPlmnEnabled();
        }
        return false;
    }

    public boolean shouldForceAutoNetworkSelect() {
        int i = Phone.PREFERRED_NT_MODE;
        int subId = getSubId();
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        int i2 = Settings.Global.getInt(this.mContext.getContentResolver(), "preferred_network_mode" + subId, i);
        logd("shouldForceAutoNetworkSelect in mode = " + i2);
        if (isManualSelProhibitedInGlobalMode() && (i2 == 10 || i2 == 7)) {
            logd("Should force auto network select mode = " + i2);
            return true;
        }
        logd("Should not force auto network select mode = " + i2);
        return false;
    }

    private boolean isManualSelProhibitedInGlobalMode() {
        String[] strArrSplit;
        String string = getContext().getResources().getString(R.string.keyguard_password_enter_password_code);
        boolean z = false;
        if (!TextUtils.isEmpty(string) && (strArrSplit = string.split(";")) != null && ((strArrSplit.length == 1 && strArrSplit[0].equalsIgnoreCase("true")) || (strArrSplit.length == 2 && !TextUtils.isEmpty(strArrSplit[1]) && strArrSplit[0].equalsIgnoreCase("true") && isMatchGid(strArrSplit[1])))) {
            z = true;
        }
        logd("isManualNetSelAllowedInGlobal in current carrier is " + z);
        return z;
    }

    protected void registerForIccRecordEvents() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords == null) {
            return;
        }
        if (isPhoneTypeGsm()) {
            iccRecords.registerForNetworkSelectionModeAutomatic(this, 28, null);
            iccRecords.registerForRecordsEvents(this, 29, null);
            iccRecords.registerForRecordsLoaded(this, 3, null);
        } else {
            iccRecords.registerForRecordsLoaded(this, 22, null);
            if (isPhoneTypeCdmaLte()) {
                iccRecords.registerForRecordsLoaded(this, 3, null);
            }
        }
    }

    protected void unregisterForIccRecordEvents() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords == null) {
            return;
        }
        iccRecords.unregisterForNetworkSelectionModeAutomatic(this);
        iccRecords.unregisterForRecordsEvents(this);
        iccRecords.unregisterForRecordsLoaded(this);
    }

    @Override
    public void exitEmergencyCallbackMode() {
        Rlog.d(LOG_TAG, "exitEmergencyCallbackMode: mImsPhone=" + this.mImsPhone + " isPhoneTypeGsm=" + isPhoneTypeGsm());
        if (isPhoneTypeGsm()) {
            if (this.mImsPhone != null) {
                this.mImsPhone.exitEmergencyCallbackMode();
            }
        } else {
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
            this.mCi.exitEmergencyCallbackMode(obtainMessage(26));
        }
    }

    private void handleEnterEmergencyCallbackMode(Message message) {
        Rlog.d(LOG_TAG, "handleEnterEmergencyCallbackMode, isInEcm()=" + isInEcm());
        if (!isInEcm()) {
            setIsInEcm(true);
            sendEmergencyCallbackModeChange();
            postDelayed(this.mExitEcmRunnable, SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L));
            this.mWakeLock.acquire();
        }
    }

    protected void handleExitEmergencyCallbackMode(Message message) {
        AsyncResult asyncResult = (AsyncResult) message.obj;
        Rlog.d(LOG_TAG, "handleExitEmergencyCallbackMode,ar.exception , isInEcm=" + asyncResult.exception + isInEcm());
        removeCallbacks(this.mExitEcmRunnable);
        if (this.mEcmExitRespRegistrant != null) {
            this.mEcmExitRespRegistrant.notifyRegistrant(asyncResult);
        }
        if (asyncResult.exception == null) {
            if (isInEcm()) {
                setIsInEcm(false);
            }
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
            sendEmergencyCallbackModeChange();
            this.mDcTracker.setInternalDataEnabled(true);
            notifyEmergencyCallRegistrants(false);
        }
    }

    public void notifyEmergencyCallRegistrants(boolean z) {
        this.mEmergencyCallToggledRegistrants.notifyResult(Integer.valueOf(z ? 1 : 0));
    }

    public void handleTimerInEmergencyCallbackMode(int i) {
        switch (i) {
            case 0:
                postDelayed(this.mExitEcmRunnable, SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L));
                this.mEcmTimerResetRegistrants.notifyResult(Boolean.FALSE);
                break;
            case 1:
                removeCallbacks(this.mExitEcmRunnable);
                this.mEcmTimerResetRegistrants.notifyResult(Boolean.TRUE);
                break;
            default:
                Rlog.e(LOG_TAG, "handleTimerInEmergencyCallbackMode, unsupported action " + i);
                break;
        }
    }

    private static boolean isIs683OtaSpDialStr(String str) {
        if (str.length() != 4) {
            switch (extractSelCodeFromOtaSpNum(str)) {
            }
            return true;
        }
        if (str.equals(IS683A_FEATURE_CODE)) {
            return true;
        }
        return false;
    }

    private static int extractSelCodeFromOtaSpNum(String str) {
        int i;
        int length = str.length();
        if (str.regionMatches(0, IS683A_FEATURE_CODE, 0, 4) && length >= 6) {
            i = Integer.parseInt(str.substring(4, 6));
        } else {
            i = -1;
        }
        Rlog.d(LOG_TAG, "extractSelCodeFromOtaSpNum " + i);
        return i;
    }

    private static boolean checkOtaSpNumBasedOnSysSelCode(int i, String[] strArr) {
        try {
            int i2 = Integer.parseInt(strArr[1]);
            for (int i3 = 0; i3 < i2; i3++) {
                int i4 = i3 + 2;
                if (!TextUtils.isEmpty(strArr[i4])) {
                    int i5 = i3 + 3;
                    if (TextUtils.isEmpty(strArr[i5])) {
                        continue;
                    } else {
                        int i6 = Integer.parseInt(strArr[i4]);
                        int i7 = Integer.parseInt(strArr[i5]);
                        if (i >= i6 && i <= i7) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (NumberFormatException e) {
            Rlog.e(LOG_TAG, "checkOtaSpNumBasedOnSysSelCode, error", e);
            return false;
        }
    }

    private boolean isCarrierOtaSpNum(String str) {
        int iExtractSelCodeFromOtaSpNum = extractSelCodeFromOtaSpNum(str);
        if (iExtractSelCodeFromOtaSpNum == -1) {
            return false;
        }
        if (!TextUtils.isEmpty(this.mCarrierOtaSpNumSchema)) {
            Matcher matcher = pOtaSpNumSchema.matcher(this.mCarrierOtaSpNumSchema);
            Rlog.d(LOG_TAG, "isCarrierOtaSpNum,schema" + this.mCarrierOtaSpNumSchema);
            if (matcher.find()) {
                String[] strArrSplit = pOtaSpNumSchema.split(this.mCarrierOtaSpNumSchema);
                if (!TextUtils.isEmpty(strArrSplit[0]) && strArrSplit[0].equals("SELC")) {
                    if (iExtractSelCodeFromOtaSpNum != -1) {
                        return checkOtaSpNumBasedOnSysSelCode(iExtractSelCodeFromOtaSpNum, strArrSplit);
                    }
                    Rlog.d(LOG_TAG, "isCarrierOtaSpNum,sysSelCodeInt is invalid");
                    return false;
                }
                if (!TextUtils.isEmpty(strArrSplit[0]) && strArrSplit[0].equals("FC")) {
                    if (str.regionMatches(0, strArrSplit[2], 0, Integer.parseInt(strArrSplit[1]))) {
                        return true;
                    }
                    Rlog.d(LOG_TAG, "isCarrierOtaSpNum,not otasp number");
                    return false;
                }
                Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema not supported" + strArrSplit[0]);
                return false;
            }
            Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema pattern not right" + this.mCarrierOtaSpNumSchema);
            return false;
        }
        Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema pattern empty");
        return false;
    }

    @Override
    public boolean isOtaSpNumber(String str) {
        if (isPhoneTypeGsm()) {
            return super.isOtaSpNumber(str);
        }
        boolean zIsIs683OtaSpDialStr = false;
        String strExtractNetworkPortionAlt = PhoneNumberUtils.extractNetworkPortionAlt(str);
        if (strExtractNetworkPortionAlt != null && !(zIsIs683OtaSpDialStr = isIs683OtaSpDialStr(strExtractNetworkPortionAlt))) {
            zIsIs683OtaSpDialStr = isCarrierOtaSpNum(strExtractNetworkPortionAlt);
        }
        Rlog.d(LOG_TAG, "isOtaSpNumber " + zIsIs683OtaSpDialStr);
        return zIsIs683OtaSpDialStr;
    }

    @Override
    public int getCdmaEriIconIndex() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriIconIndex();
        }
        return getServiceState().getCdmaEriIconIndex();
    }

    @Override
    public int getCdmaEriIconMode() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriIconMode();
        }
        return getServiceState().getCdmaEriIconMode();
    }

    @Override
    public String getCdmaEriText() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriText();
        }
        return this.mEriManager.getCdmaEriText(getServiceState().getCdmaRoamingIndicator(), getServiceState().getCdmaDefaultRoamingIndicator());
    }

    protected void phoneObjectUpdater(int i) {
        logd("phoneObjectUpdater: newVoiceRadioTech=" + i);
        if (ServiceState.isLte(i) || i == 0) {
            PersistableBundle configForSubId = ((CarrierConfigManager) getContext().getSystemService("carrier_config")).getConfigForSubId(getSubId());
            if (configForSubId != null) {
                int i2 = configForSubId.getInt("volte_replacement_rat_int");
                logd("phoneObjectUpdater: volteReplacementRat=" + i2);
                if (i2 != 0) {
                    i = i2;
                }
            } else {
                loge("phoneObjectUpdater: didn't get volteReplacementRat from carrier config");
            }
        }
        if (this.mRilVersion == 6 && getLteOnCdmaMode() == 1) {
            if (getPhoneType() == 2) {
                logd("phoneObjectUpdater: LTE ON CDMA property is set. Use CDMA Phone newVoiceRadioTech=" + i + " mActivePhone=" + getPhoneName());
                return;
            }
            logd("phoneObjectUpdater: LTE ON CDMA property is set. Switch to CDMALTEPhone newVoiceRadioTech=" + i + " mActivePhone=" + getPhoneName());
            i = 6;
        } else {
            if (isShuttingDown()) {
                logd("Device is shutting down. No need to switch phone now.");
                return;
            }
            boolean zIsCdma = ServiceState.isCdma(i);
            boolean zIsGsm = ServiceState.isGsm(i);
            if (((zIsCdma && getPhoneType() == 2) || (zIsGsm && getPhoneType() == 1)) && !correctPhoneTypeForCdma(zIsCdma, i)) {
                logd("phoneObjectUpdater: No change ignore, newVoiceRadioTech=" + i + " mActivePhone=" + getPhoneName());
                return;
            }
            if (!zIsCdma && !zIsGsm) {
                loge("phoneObjectUpdater: newVoiceRadioTech=" + i + " doesn't match either CDMA or GSM - error! No phone change");
                return;
            }
        }
        if (i == 0) {
            logd("phoneObjectUpdater: Unknown rat ignore,  newVoiceRadioTech=Unknown. mActivePhone=" + getPhoneName());
            return;
        }
        boolean z = false;
        if (this.mResetModemOnRadioTechnologyChange && this.mCi.getRadioState().isOn()) {
            logd("phoneObjectUpdater: Setting Radio Power to Off");
            this.mCi.setRadioPower(false, null);
            z = true;
        }
        switchVoiceRadioTech(i);
        if (this.mResetModemOnRadioTechnologyChange && z) {
            logd("phoneObjectUpdater: Resetting Radio");
            this.mCi.setRadioPower(z, null);
        }
        UiccProfile uiccProfile = getUiccProfile();
        if (uiccProfile != null) {
            uiccProfile.setVoiceRadioTech(i);
        }
        Intent intent = new Intent("android.intent.action.RADIO_TECHNOLOGY");
        intent.putExtra("phoneName", getPhoneName());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhoneId);
        ActivityManager.broadcastStickyIntent(intent, -1);
    }

    protected void switchVoiceRadioTech(int i) {
        String phoneName = getPhoneName();
        StringBuilder sb = new StringBuilder();
        sb.append("Switching Voice Phone : ");
        sb.append(phoneName);
        sb.append(" >>> ");
        sb.append(ServiceState.isGsm(i) ? "GSM" : "CDMA");
        logd(sb.toString());
        if (ServiceState.isCdma(i)) {
            UiccCardApplication uiccCardApplication = this.mUiccController.getUiccCardApplication(this.mPhoneId, 2);
            if (uiccCardApplication != null && uiccCardApplication.getType() == IccCardApplicationStatus.AppType.APPTYPE_RUIM) {
                switchPhoneType(2);
                return;
            } else {
                switchPhoneType(6);
                return;
            }
        }
        if (ServiceState.isGsm(i)) {
            switchPhoneType(1);
            return;
        }
        loge("deleteAndCreatePhone: newVoiceRadioTech=" + i + " is not CDMA or GSM (error) - aborting!");
    }

    @Override
    public void setSignalStrengthReportingCriteria(int[] iArr, int i) {
        this.mCi.setSignalStrengthReportingCriteria(REPORTING_HYSTERESIS_MILLIS, 2, iArr, i, null);
    }

    @Override
    public void setLinkCapacityReportingCriteria(int[] iArr, int[] iArr2, int i) {
        this.mCi.setLinkCapacityReportingCriteria(REPORTING_HYSTERESIS_MILLIS, 50, 50, iArr, iArr2, i, null);
    }

    @Override
    public IccSmsInterfaceManager getIccSmsInterfaceManager() {
        return this.mIccSmsInterfaceManager;
    }

    @Override
    public void updatePhoneObject(int i) {
        logd("updatePhoneObject: radioTechnology=" + i);
        sendMessage(obtainMessage(42, i, 0, null));
    }

    @Override
    public void setImsRegistrationState(boolean z) {
        this.mSST.setImsRegistrationState(z);
    }

    @Override
    public boolean getIccRecordsLoaded() {
        UiccProfile uiccProfile = getUiccProfile();
        return uiccProfile != null && uiccProfile.getIccRecordsLoaded();
    }

    @Override
    public IccCard getIccCard() {
        UiccProfile uiccProfile = getUiccProfile();
        if (uiccProfile != null) {
            return uiccProfile;
        }
        UiccSlot uiccSlotForPhone = this.mUiccController.getUiccSlotForPhone(this.mPhoneId);
        boolean zIsNetworkSupported = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).isNetworkSupported(0);
        if ((uiccSlotForPhone == null || uiccSlotForPhone.isStateUnknown()) && zIsNetworkSupported) {
            return new IccCard(IccCardConstants.State.UNKNOWN);
        }
        return new IccCard(IccCardConstants.State.ABSENT);
    }

    protected UiccProfile getUiccProfile() {
        return UiccController.getInstance().getUiccProfileForPhone(this.mPhoneId);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("GsmCdmaPhone extends:");
        super.dump(fileDescriptor, printWriter, strArr);
        printWriter.println(" mPrecisePhoneType=" + this.mPrecisePhoneType);
        printWriter.println(" mCT=" + this.mCT);
        printWriter.println(" mSST=" + this.mSST);
        printWriter.println(" mPendingMMIs=" + this.mPendingMMIs);
        printWriter.println(" mIccPhoneBookIntManager=" + this.mIccPhoneBookIntManager);
        printWriter.println(" mCdmaSSM=" + this.mCdmaSSM);
        printWriter.println(" mCdmaSubscriptionSource=" + this.mCdmaSubscriptionSource);
        printWriter.println(" mEriManager=" + this.mEriManager);
        printWriter.println(" mWakeLock=" + this.mWakeLock);
        printWriter.println(" isInEcm()=" + isInEcm());
        printWriter.println(" mCarrierOtaSpNumSchema=" + this.mCarrierOtaSpNumSchema);
        if (!isPhoneTypeGsm()) {
            printWriter.println(" getCdmaEriIconIndex()=" + getCdmaEriIconIndex());
            printWriter.println(" getCdmaEriIconMode()=" + getCdmaEriIconMode());
            printWriter.println(" getCdmaEriText()=" + getCdmaEriText());
            printWriter.println(" isMinInfoReady()=" + isMinInfoReady());
        }
        printWriter.println(" isCspPlmnEnabled()=" + isCspPlmnEnabled());
        printWriter.flush();
    }

    @Override
    public boolean setOperatorBrandOverride(String str) {
        UiccCard uiccCard;
        if (this.mUiccController == null || (uiccCard = this.mUiccController.getUiccCard(getPhoneId())) == null) {
            return false;
        }
        boolean operatorBrandOverride = uiccCard.setOperatorBrandOverride(str);
        if (operatorBrandOverride) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                TelephonyManager.from(this.mContext).setSimOperatorNameForPhone(getPhoneId(), iccRecords.getServiceProviderName());
            }
            if (this.mSST != null) {
                this.mSST.pollState();
            }
        }
        return operatorBrandOverride;
    }

    protected String getOperatorNumeric() {
        IccRecords iccRecords;
        String rUIMOperatorNumeric;
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords2 = this.mIccRecords.get();
            if (iccRecords2 != null) {
                return iccRecords2.getOperatorNumeric();
            }
            return null;
        }
        if (this.mCdmaSubscriptionSource == 1) {
            rUIMOperatorNumeric = SystemProperties.get(PROPERTY_CDMA_HOME_OPERATOR_NUMERIC);
            iccRecords = null;
        } else if (this.mCdmaSubscriptionSource == 0) {
            UiccCardApplication uiccCardApplication = this.mUiccApplication.get();
            if (uiccCardApplication != null && uiccCardApplication.getType() == IccCardApplicationStatus.AppType.APPTYPE_RUIM) {
                logd("Legacy RUIM app present");
                iccRecords = this.mIccRecords.get();
            } else {
                iccRecords = this.mSimRecords;
            }
            if (iccRecords != null && iccRecords == this.mSimRecords) {
                rUIMOperatorNumeric = iccRecords.getOperatorNumeric();
            } else {
                iccRecords = this.mIccRecords.get();
                if (iccRecords != null && (iccRecords instanceof RuimRecords)) {
                    rUIMOperatorNumeric = ((RuimRecords) iccRecords).getRUIMOperatorNumeric();
                } else {
                    rUIMOperatorNumeric = null;
                }
            }
        } else {
            iccRecords = null;
            rUIMOperatorNumeric = null;
        }
        if (rUIMOperatorNumeric == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("getOperatorNumeric: Cannot retrieve operatorNumeric: mCdmaSubscriptionSource = ");
            sb.append(this.mCdmaSubscriptionSource);
            sb.append(" mIccRecords = ");
            sb.append(iccRecords != null ? Boolean.valueOf(iccRecords.getRecordsLoaded()) : null);
            loge(sb.toString());
        }
        logd("getOperatorNumeric: mCdmaSubscriptionSource = " + this.mCdmaSubscriptionSource + " operatorNumeric = " + rUIMOperatorNumeric);
        return rUIMOperatorNumeric;
    }

    public String getCountryIso() {
        SubscriptionInfo activeSubscriptionInfo = SubscriptionManager.from(getContext()).getActiveSubscriptionInfo(getSubId());
        if (activeSubscriptionInfo == null) {
            return null;
        }
        return activeSubscriptionInfo.getCountryIso().toUpperCase();
    }

    public void notifyEcbmTimerReset(Boolean bool) {
        this.mEcmTimerResetRegistrants.notifyResult(bool);
    }

    @Override
    public void registerForEcmTimerReset(Handler handler, int i, Object obj) {
        this.mEcmTimerResetRegistrants.addUnique(handler, i, obj);
    }

    @Override
    public void unregisterForEcmTimerReset(Handler handler) {
        this.mEcmTimerResetRegistrants.remove(handler);
    }

    @Override
    public void setVoiceMessageWaiting(int i, int i2) {
        if (isPhoneTypeGsm()) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                iccRecords.setVoiceMessageWaiting(i, i2);
                return;
            } else {
                logd("SIM Records not found, MWI not updated");
                return;
            }
        }
        setVoiceMessageCount(i2);
    }

    protected void logd(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhoneId + "] " + str);
    }

    protected void logi(String str) {
        Rlog.i(LOG_TAG, "[" + this.mPhoneId + "] " + str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhoneId + "] " + str);
    }

    @Override
    public boolean isUtEnabled() {
        Phone phone = this.mImsPhone;
        if (phone != null) {
            return phone.isUtEnabled();
        }
        logd("isUtEnabled: called for GsmCdma");
        return false;
    }

    public String getDtmfToneDelayKey() {
        if (isPhoneTypeGsm()) {
            return "gsm_dtmf_tone_delay_int";
        }
        return "cdma_dtmf_tone_delay_int";
    }

    @VisibleForTesting
    public PowerManager.WakeLock getWakeLock() {
        return this.mWakeLock;
    }

    @Override
    public int getLteOnCdmaMode() {
        int lteOnCdmaMode = super.getLteOnCdmaMode();
        UiccCardApplication uiccCardApplication = this.mUiccController.getUiccCardApplication(this.mPhoneId, 2);
        if (uiccCardApplication != null && uiccCardApplication.getType() == IccCardApplicationStatus.AppType.APPTYPE_RUIM && lteOnCdmaMode == 1) {
            return 0;
        }
        return lteOnCdmaMode;
    }

    protected boolean correctPhoneTypeForCdma(boolean z, int i) {
        return false;
    }

    protected boolean needResetPhbIntMgr() {
        return true;
    }
}
