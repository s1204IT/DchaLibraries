package com.mediatek.internal.telephony;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RegistrantList;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.MtkRadioAccessFamily;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UssdResponse;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallForwardInfo;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.CarrierInfoManager;
import com.android.internal.telephony.CarrierKeyDownloadManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.OperatorInfo;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.PhoneNotifier;
import com.android.internal.telephony.RadioCapability;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneMmiCode;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.mediatek.ims.internal.MtkImsManager;
import com.mediatek.internal.telephony.MtkIccCardConstants;
import com.mediatek.internal.telephony.MtkOperatorUtils;
import com.mediatek.internal.telephony.cdma.MtkCdmaSubscriptionSourceManager;
import com.mediatek.internal.telephony.dataconnection.FdManager;
import com.mediatek.internal.telephony.dataconnection.MtkDcHelper;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.gsm.MtkGsmMmiCode;
import com.mediatek.internal.telephony.gsm.MtkSuppCrssNotification;
import com.mediatek.internal.telephony.imsphone.MtkImsPhone;
import com.mediatek.internal.telephony.phb.CsimPhbUtil;
import com.mediatek.internal.telephony.selfactivation.ISelfActivation;
import com.mediatek.internal.telephony.uicc.MtkSIMRecords;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import mediatek.telephony.MtkServiceState;

public class MtkGsmCdmaPhone extends GsmCdmaPhone {
    public static final String ACT_TYPE_GSM = "0";
    public static final String ACT_TYPE_LTE = "7";
    public static final String ACT_TYPE_UTRAN = "2";
    private static final String CFB_KEY = "CFB";
    private static final String CFNRC_KEY = "CFNRC";
    private static final String CFNR_KEY = "CFNR";
    private static final String CFU_TIME_SLOT = "persist.vendor.radio.cfu.timeslot.";
    private static final boolean DBG = true;
    protected static final int EVENT_CIPHER_INDICATION = 1000;
    protected static final int EVENT_CRSS_IND = 1002;
    protected static final int EVENT_GET_APC_INFO = 1001;
    public static final int EVENT_GET_CALL_BARRING_COMPLETE = 2006;
    public static final int EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE = 109;
    public static final int EVENT_GET_CALL_WAITING_DONE = 301;
    public static final int EVENT_GET_CLIR_COMPLETE = 2004;
    public static final int EVENT_IMS_UT_CSFB = 2001;
    public static final int EVENT_IMS_UT_DONE = 2000;
    protected static final int EVENT_MTK_BASE = 1000;
    public static final int EVENT_SET_CALL_BARRING_COMPLETE = 2005;
    public static final int EVENT_SET_CALL_FORWARD_TIME_SLOT_DONE = 110;
    public static final int EVENT_SET_CALL_WAITING_DONE = 302;
    protected static final int EVENT_SET_SS_PROPERTY = 1004;
    protected static final int EVENT_SPEECH_CODEC_INFO = 1003;
    public static final int EVENT_UNSOL_RADIO_CAPABILITY_CHANGED = 111;
    public static final int EVENT_USSI_CSFB = 2003;
    public static final String IMS_DEREG_OFF = "0";
    public static final String IMS_DEREG_ON = "1";
    public static final String IMS_DEREG_PROP = "vendor.gsm.radio.ss.imsdereg";
    public static final String LOG_TAG = "MtkGsmCdmaPhone";
    public static final String LTE_INDICATOR = "4G";
    public static final int MESSAGE_SET_CF = 1;
    public static final boolean MTK_SVLTE_SUPPORT;
    public static final int NT_MODE_LTE_GSM = 30;
    public static final int NT_MODE_LTE_TDD_ONLY = 31;
    private static final int OPERATION_TIME_OUT_MILLIS = 5000;
    private static final int PROPERTY_MODE_BOOL = 1;
    private static final int PROPERTY_MODE_INT = 0;
    private static final int PROPERTY_MODE_STRING = 2;
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE;
    private static final String PROPERTY_WFC_ENABLE = "persist.vendor.mtk.wfc.enable";
    private static final String PROP_MTK_CDMA_LTE_MODE = "ro.boot.opt_c2k_lte_mode";
    private static final String PROP_VZW_DEVICE_TYPE = "persist.vendor.vzw_device_type";
    public static final String REASON_CARRIER_CONFIG_LOADED = "carrierConfigLoaded";
    public static final String REASON_DATA_ALLOWED = "dataAllowed";
    public static final String REASON_FDN_DISABLED = "FdnDisabled";
    public static final String REASON_FDN_ENABLED = "FdnEnabled";
    public static final String REASON_MD_DATA_RETRY_COUNT_RESET = "modemDataCountReset";
    public static final String REASON_PCSCF_ADDRESS_FAILED = "pcscfFailed";
    public static final String REASON_RA_FAILED = "raFailed";
    public static final String REASON_RESUME_PENDING_DATA = "resumePendingData";
    private static final boolean SDBG;
    private static final String SS_SERVICE_CLASS_PROP = "vendor.gsm.radio.ss.sc";
    public static final int TBCW_NOT_VOLTE_USER = 2;
    public static final int TBCW_UNKNOWN = 0;
    public static final int TBCW_VOLTE_USER = 1;
    public static final int TBCW_WITH_CS = 3;
    static final int USSD_HANDLED_BY_STK = 3;
    static final int USSD_MODE_LOCAL_CLIENT = 3;
    static final int USSD_MODE_NOTIFY = 0;
    static final int USSD_MODE_NOT_SUPPORTED = 4;
    static final int USSD_MODE_NW_RELEASE = 2;
    static final int USSD_MODE_NW_TIMEOUT = 5;
    static final int USSD_MODE_REQUEST = 1;
    static final int USSD_NETWORK_TIMEOUT = 5;
    static final int USSD_OPERATION_NOT_SUPPORTED = 4;
    static final int USSD_SESSION_END = 2;
    public static final String UTRAN_INDICATOR = "3G";
    private BroadcastReceiver mBroadcastReceiver;
    private int mCSFallbackMode;
    private AsyncResult mCachedCrssn;
    private AsyncResult mCachedSsn;
    RegistrantList mCallRelatedSuppSvcRegistrants;
    private CountDownLatch mCallbackLatch;
    protected final RegistrantList mCdmaCallAcceptedRegistrants;
    protected final RegistrantList mCipherIndicationRegistrants;
    private FdManager mFdManager;
    public boolean mIsNetworkInitiatedUssr;
    private final Object mLock;
    public MtkRIL mMtkCi;
    private MtkSSRequestDecisionMaker mMtkSSReqDecisionMaker;
    public MtkServiceStateTracker mMtkSST;
    private int mNewVoiceTech;
    private ISelfActivation mSelfActInstance;
    private final RegistrantList mSpeechCodecInfoRegistrants;
    private int mTbcwMode;
    TelephonyDevController mTelDevController;
    private boolean mWifiIsEnabledBeforeE911;

    static {
        SDBG = !SystemProperties.get("ro.build.type").equals(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
        PROPERTY_RIL_FULL_UICC_TYPE = new String[]{"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
        MTK_SVLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 1;
    }

    private boolean hasC2kOverImsModem() {
        return (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasC2kOverImsModem()) ? false : true;
    }

    private static class Cfu {
        final Message mOnComplete;
        final int mServiceClass;
        final String mSetCfNumber;

        Cfu(String str, Message message, int i) {
            this.mSetCfNumber = str;
            this.mOnComplete = message;
            this.mServiceClass = i;
        }
    }

    public MtkGsmCdmaPhone(Context context, CommandsInterface commandsInterface, PhoneNotifier phoneNotifier, boolean z, int i, int i2, TelephonyComponentFactory telephonyComponentFactory) {
        super(context, commandsInterface, phoneNotifier, z, i, i2, telephonyComponentFactory);
        this.mNewVoiceTech = -1;
        this.mLock = new Object();
        this.mTelDevController = TelephonyDevController.getInstance();
        this.mCipherIndicationRegistrants = new RegistrantList();
        this.mCallRelatedSuppSvcRegistrants = new RegistrantList();
        this.mCachedSsn = null;
        this.mCachedCrssn = null;
        this.mSpeechCodecInfoRegistrants = new RegistrantList();
        this.mCdmaCallAcceptedRegistrants = new RegistrantList();
        this.mSelfActInstance = null;
        this.mTbcwMode = 0;
        this.mIsNetworkInitiatedUssr = false;
        this.mCSFallbackMode = 0;
        this.mWifiIsEnabledBeforeE911 = false;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                intent.getAction();
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    MtkGsmCdmaPhone.this.sendMessage(MtkGsmCdmaPhone.this.obtainMessage(43));
                }
            }
        };
        Rlog.d(LOG_TAG, "constructor: sub = " + i);
        this.mMtkCi = (MtkRIL) commandsInterface;
        this.mMtkSST = (MtkServiceStateTracker) this.mSST;
        this.mFdManager = FdManager.getInstance(this);
        this.mMtkSSReqDecisionMaker = new MtkSSRequestDecisionMaker(this.mContext, this);
        this.mMtkSSReqDecisionMaker.starThread();
        this.mMtkCi.registerForCipherIndication(this, 1000, null);
        this.mMtkCi.setOnSpeechCodecInfo(this, 1003, null);
        this.mSelfActInstance = OpTelephonyCustomizationUtils.getOpFactory(context).makeSelfActivationInstance(i);
        this.mSelfActInstance.setContext(context).setCommandsInterface(commandsInterface).buildParams();
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
    }

    public MtkGsmCdmaPhone(Context context, CommandsInterface commandsInterface, PhoneNotifier phoneNotifier, int i, int i2, TelephonyComponentFactory telephonyComponentFactory) {
        this(context, commandsInterface, phoneNotifier, false, i, i2, telephonyComponentFactory);
    }

    public ISelfActivation getSelfActivationInstance() {
        return this.mSelfActInstance;
    }

    public ServiceState getServiceState() {
        if ((this.mMtkSST == null || (this.mMtkSST.mSS.getState() != 0 && this.mMtkSST.mSS.getDataRegState() == 0)) && this.mImsPhone != null) {
            return MtkServiceState.mergeMtkServiceStates(this.mMtkSST == null ? new MtkServiceState() : this.mMtkSST.mSS, this.mImsPhone.getServiceState());
        }
        if (this.mSST != null) {
            return this.mSST.mSS;
        }
        return new MtkServiceState();
    }

    public PhoneConstants.DataState getDataConnectionState(String str) {
        PhoneConstants.DataState dataState;
        PhoneConstants.DataState dataState2 = PhoneConstants.DataState.DISCONNECTED;
        if (this.mSST == null) {
            dataState = PhoneConstants.DataState.DISCONNECTED;
        } else if (this.mSST.getCurrentDataConnectionState() != 0 && (isPhoneTypeCdma() || isPhoneTypeCdmaLte() || (isPhoneTypeGsm() && !str.equals("emergency")))) {
            dataState = PhoneConstants.DataState.DISCONNECTED;
        } else {
            switch (AnonymousClass2.$SwitchMap$com$android$internal$telephony$DctConstants$State[this.mDcTracker.getState(str).ordinal()]) {
                case 1:
                case 2:
                    if (MtkDcHelper.getInstance().isDataAllowedForConcurrent(getPhoneId())) {
                        dataState = PhoneConstants.DataState.CONNECTED;
                    } else {
                        dataState = PhoneConstants.DataState.SUSPENDED;
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

    static class AnonymousClass2 {
        static final int[] $SwitchMap$com$android$internal$telephony$DctConstants$State = new int[DctConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.DISCONNECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    protected void initOnce(CommandsInterface commandsInterface) {
        if (commandsInterface instanceof SimulatedRadioControl) {
            this.mSimulatedRadioControl = (SimulatedRadioControl) commandsInterface;
        }
        this.mCT = this.mTelephonyComponentFactory.makeGsmCdmaCallTracker(this);
        this.mIccPhoneBookIntManager = this.mTelephonyComponentFactory.makeIccPhoneBookInterfaceManager(this);
        this.mWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(1, LOG_TAG);
        this.mIccSmsInterfaceManager = this.mTelephonyComponentFactory.makeIccSmsInterfaceManager(this);
        this.mCi.registerForAvailable(this, 1, (Object) null);
        this.mCi.registerForOffOrNotAvailable(this, 8, (Object) null);
        this.mCi.registerForOn(this, 5, (Object) null);
        this.mCi.setOnSuppServiceNotification(this, 2, (Object) null);
        this.mCi.registerForRadioCapabilityChanged(this, 111, (Object) null);
        this.mCi.setOnUSSD(this, 7, (Object) null);
        this.mCi.setOnSs(this, 36, (Object) null);
        if (this.mMtkCi == null) {
            this.mMtkCi = (MtkRIL) commandsInterface;
        }
        this.mMtkCi.setOnCallRelatedSuppSvc(this, 1002, null);
        this.mCdmaSSM = this.mTelephonyComponentFactory.getCdmaSubscriptionSourceManagerInstance(this.mContext, this.mCi, this, 27, (Object) null);
        this.mEriManager = this.mTelephonyComponentFactory.makeEriManager(this, this.mContext, 0);
        this.mCi.setEmergencyCallbackMode(this, 25, (Object) null);
        this.mCi.registerForExitEmergencyCallbackMode(this, 26, (Object) null);
        this.mCi.registerForModemReset(this, 45, (Object) null);
        this.mCarrierOtaSpNumSchema = TelephonyManager.from(this.mContext).getOtaSpNumberSchemaForPhone(getPhoneId(), "");
        this.mResetModemOnRadioTechnologyChange = SystemProperties.getBoolean("persist.radio.reset_on_switch", false);
        this.mCi.registerForRilConnected(this, 41, (Object) null);
        this.mCi.registerForVoiceRadioTechChanged(this, 39, (Object) null);
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
        this.mCDM = new CarrierKeyDownloadManager(this);
        this.mCIM = new CarrierInfoManager();
    }

    protected void switchPhoneType(int i) {
        synchronized (this.mLock) {
            super.switchPhoneType(i);
        }
        if (this.mIccRecords.get() != null && i == 1) {
            logd("Re-register registerForIccRecordEvents due to phonetype change to GSM.");
            unregisterForIccRecordEvents();
            registerForIccRecordEvents();
        }
    }

    protected void onUpdateIccAvailability() {
        super.onUpdateIccAvailability();
        UiccCardApplication uiccCardApplication = getUiccCardApplication();
        UiccCardApplication uiccCardApplication2 = (UiccCardApplication) this.mUiccApplication.get();
        IccRecords iccRecords = uiccCardApplication != null ? uiccCardApplication.getIccRecords() : null;
        if (uiccCardApplication2 == uiccCardApplication && this.mIccRecords.get() != iccRecords) {
            if (uiccCardApplication2 != null) {
                logd("Removing stale icc objects.");
                if (this.mIccRecords.get() != null) {
                    unregisterForIccRecordEvents();
                    this.mIccPhoneBookIntManager.updateIccRecords((IccRecords) null);
                }
                this.mIccRecords.set(null);
                this.mUiccApplication.set(null);
            }
            if (uiccCardApplication != null) {
                logd("New Uicc application found. type = " + uiccCardApplication.getType());
                this.mUiccApplication.set(uiccCardApplication);
                this.mIccRecords.set(uiccCardApplication.getIccRecords());
                registerForIccRecordEvents();
                this.mIccPhoneBookIntManager.updateIccRecords((IccRecords) this.mIccRecords.get());
            }
        }
        Rlog.d(LOG_TAG, "isPhoneTypeCdmaLte:" + isPhoneTypeCdmaLte() + ", phoneId: " + getPhoneId() + " isCdmaWithoutLteCard: " + isCdmaWithoutLteCard() + " mNewVoiceTech: " + this.mNewVoiceTech);
        if (this.mNewVoiceTech != -1) {
            if ((isPhoneTypeCdmaLte() && isCdmaWithoutLteCard()) || (isPhoneTypeCdma() && !isCdmaWithoutLteCard())) {
                updatePhoneObject(this.mNewVoiceTech);
            }
        }
    }

    protected boolean correctPhoneTypeForCdma(boolean z, int i) {
        boolean z2;
        if (z && getPhoneType() == 2) {
            UiccProfile uiccProfile = getUiccProfile();
            if (uiccProfile != null) {
                uiccProfile.setVoiceRadioTech(i);
            }
            z2 = true;
        } else {
            z2 = false;
        }
        if ((!isPhoneTypeCdmaLte() || !isCdmaWithoutLteCard()) && (!isPhoneTypeCdma() || isCdmaWithoutLteCard())) {
            z2 = false;
        }
        Rlog.d(LOG_TAG, "correctPhoneTypeForCdma: change:" + z2 + " newVoiceRadioTech=" + i + " mActivePhone=" + getPhoneName());
        return z2;
    }

    protected void switchVoiceRadioTech(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("Switching Voice Phone : ");
        sb.append(getPhoneName());
        sb.append(" >>> ");
        sb.append(ServiceState.isGsm(i) ? "GSM" : "CDMA");
        logd(sb.toString());
        if (ServiceState.isCdma(i) && isCdmaWithoutLteCard()) {
            switchPhoneType(2);
        } else {
            super.switchVoiceRadioTech(i);
        }
    }

    protected void logd(String str) {
        Rlog.d(LOG_TAG, "[MtkGsmCdmaPhone] " + str);
    }

    private boolean isCdmaWithoutLteCard() {
        if (MtkTelephonyManagerEx.getDefault().getIccAppFamily(getPhoneId()) == 2) {
            return true;
        }
        return false;
    }

    public void triggerModeSwitchByEcc(int i, Message message) {
        this.mMtkCi.triggerModeSwitchByEcc(i, message);
    }

    public String getLocatedPlmn() {
        return this.mMtkSST.getLocatedPlmn();
    }

    public void sendSubscriptionSettings(boolean z) {
        if (this.mMtkSST != null) {
            this.mMtkSST.setDeviceRatMode(this.mPhoneId);
        }
        boolean z2 = !this.mContext.getResources().getBoolean(R.^attr-private.showAtTop);
        if (z && z2) {
            restoreSavedNetworkSelection(null);
        }
    }

    protected void setPreferredNetworkTypeIfSimLoaded() {
        if (SubscriptionManager.isValidSubscriptionId(getSubId()) && this.mMtkSST != null) {
            this.mMtkSST.setDeviceRatMode(this.mPhoneId);
        }
    }

    public void setPreferredNetworkType(int i, Message message) {
        if (i == 31) {
            this.mCi.setPreferredNetworkType(i, message);
            return;
        }
        int radioAccessFamily = getRadioAccessFamily();
        int rafFromNetworkType = MtkRadioAccessFamily.getRafFromNetworkType(i);
        if (radioAccessFamily == 1 || rafFromNetworkType == 1) {
            Rlog.d(LOG_TAG, "setPreferredNetworkType: Abort, unknown RAF: " + radioAccessFamily + " " + rafFromNetworkType);
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                message.sendToTarget();
                return;
            }
            return;
        }
        int i2 = rafFromNetworkType & radioAccessFamily;
        if (i2 <= 1) {
            i2 = radioAccessFamily;
        }
        int networkTypeFromRaf = MtkRadioAccessFamily.getNetworkTypeFromRaf(i2);
        Rlog.d(LOG_TAG, "setPreferredNetworkType: networkType = " + i + " modemRaf = " + radioAccessFamily + " rafFromType = " + rafFromNetworkType + " filteredType = " + networkTypeFromRaf);
        this.mCi.setPreferredNetworkType(networkTypeFromRaf, message);
    }

    public void selectNetworkManually(OperatorInfo operatorInfo, boolean z, Message message) {
        Phone.NetworkSelectMessage networkSelectMessage = new Phone.NetworkSelectMessage();
        networkSelectMessage.message = message;
        networkSelectMessage.operatorNumeric = operatorInfo.getOperatorNumeric();
        networkSelectMessage.operatorAlphaLong = operatorInfo.getOperatorAlphaLong();
        networkSelectMessage.operatorAlphaShort = operatorInfo.getOperatorAlphaShort();
        Message messageObtainMessage = obtainMessage(16, networkSelectMessage);
        if (getPhoneName().equals("GSM")) {
            Rlog.d(LOG_TAG, "MTK GSMPhone selectNetworkManuallyWithAct:" + operatorInfo);
            String str = "0";
            if (operatorInfo.getOperatorAlphaLong() != null && operatorInfo.getOperatorAlphaLong().endsWith(UTRAN_INDICATOR)) {
                str = ACT_TYPE_UTRAN;
            } else if (operatorInfo.getOperatorAlphaLong() != null && operatorInfo.getOperatorAlphaLong().endsWith(LTE_INDICATOR)) {
                str = ACT_TYPE_LTE;
            }
            this.mMtkCi.setNetworkSelectionModeManualWithAct(operatorInfo.getOperatorNumeric(), str, 0, messageObtainMessage);
        } else {
            this.mCi.setNetworkSelectionModeManual(operatorInfo.getOperatorNumeric(), messageObtainMessage);
        }
        if (z) {
            updateSavedNetworkOperator(networkSelectMessage);
        } else {
            clearSavedNetworkSelection();
        }
    }

    public void setMtkNetworkSelection(int i, OperatorInfo operatorInfo, Message message) {
        if (i == 1) {
            this.mMtkCi.setNetworkSelectionModeManualWithAct("00000", "0", 2, message);
        } else if (i == 2) {
            setNetworkSelectionModeSemiAutomatic(operatorInfo, message);
        }
    }

    public void setNetworkSelectionModeSemiAutomatic(OperatorInfo operatorInfo, Message message) {
        Phone.NetworkSelectMessage networkSelectMessage = new Phone.NetworkSelectMessage();
        networkSelectMessage.message = message;
        networkSelectMessage.operatorNumeric = "";
        networkSelectMessage.operatorAlphaLong = "";
        networkSelectMessage.operatorAlphaShort = "";
        Message messageObtainMessage = obtainMessage(17, networkSelectMessage);
        Rlog.d(LOG_TAG, "MTK GSMPhone setNetworkSelectionModeSemiAutomatic:" + operatorInfo);
        String str = "0";
        if (operatorInfo.getOperatorAlphaLong() != null && operatorInfo.getOperatorAlphaLong().endsWith(UTRAN_INDICATOR)) {
            str = ACT_TYPE_UTRAN;
        } else if (operatorInfo.getOperatorAlphaLong() != null && operatorInfo.getOperatorAlphaLong().endsWith(LTE_INDICATOR)) {
            str = ACT_TYPE_LTE;
        }
        this.mMtkCi.setNetworkSelectionModeManualWithAct(operatorInfo.getOperatorNumeric(), str, 1, messageObtainMessage);
    }

    public void getAvailableNetworks(Message message) {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            this.mMtkCi.getAvailableNetworksWithAct(message);
        } else {
            Rlog.d(LOG_TAG, "getAvailableNetworks: not possible in CDMA");
        }
    }

    public synchronized void cancelAvailableNetworks(Message message) {
        Rlog.d(LOG_TAG, "cancelAvailableNetworks");
        this.mMtkCi.cancelAvailableNetworks(message);
    }

    public void getFemtoCellList(Message message) {
        Rlog.d(LOG_TAG, "getFemtoCellList()");
        this.mMtkCi.getFemtoCellList(message);
    }

    public void abortFemtoCellList(Message message) {
        Rlog.d(LOG_TAG, "abortFemtoCellList()");
        this.mMtkCi.abortFemtoCellList(message);
    }

    public void selectFemtoCell(FemtoCellInfo femtoCellInfo, Message message) {
        Rlog.d(LOG_TAG, "selectFemtoCell(): " + femtoCellInfo);
        this.mMtkCi.selectFemtoCell(femtoCellInfo, message);
    }

    public void queryFemtoCellSystemSelectionMode(Message message) {
        Rlog.d(LOG_TAG, "queryFemtoCellSystemSelectionMode()");
        this.mMtkCi.queryFemtoCellSystemSelectionMode(message);
    }

    public void setFemtoCellSystemSelectionMode(int i, Message message) {
        Rlog.d(LOG_TAG, "setFemtoCellSystemSelectionMode(), mode=" + i);
        this.mMtkCi.setFemtoCellSystemSelectionMode(i, message);
    }

    protected Connection dialInternal(String str, PhoneInternalInterface.DialArgs dialArgs, ResultReceiver resultReceiver) throws CallStateException {
        String strStripSeparators = PhoneNumberUtils.stripSeparators(str);
        if (isPhoneTypeGsm()) {
            if (handleInCallMmiCommands(strStripSeparators)) {
                return null;
            }
            MtkGsmMmiCode mtkGsmMmiCodeNewFromDialString = MtkGsmMmiCode.newFromDialString(PhoneNumberUtils.extractNetworkPortionAlt(strStripSeparators), this, (UiccCardApplication) this.mUiccApplication.get(), resultReceiver);
            logd("dialInternal: dialing w/ mmi '" + mtkGsmMmiCodeNewFromDialString + "'...");
            if (mtkGsmMmiCodeNewFromDialString == null) {
                return this.mCT.dial(strStripSeparators, dialArgs.uusInfo, dialArgs.intentExtras);
            }
            if (mtkGsmMmiCodeNewFromDialString.isTemporaryModeCLIR()) {
                return this.mCT.dial(mtkGsmMmiCodeNewFromDialString.mDialingNumber, mtkGsmMmiCodeNewFromDialString.getCLIRMode(), dialArgs.uusInfo, dialArgs.intentExtras);
            }
            this.mPendingMMIs.add(mtkGsmMmiCodeNewFromDialString);
            Rlog.d(LOG_TAG, "dialInternal: " + str + ", mmi=" + mtkGsmMmiCodeNewFromDialString);
            dumpPendingMmi();
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mtkGsmMmiCodeNewFromDialString, (Throwable) null));
            mtkGsmMmiCodeNewFromDialString.processCode();
            return null;
        }
        return this.mCT.dial(strStripSeparators);
    }

    public void doGeneralSimAuthentication(int i, int i2, int i3, String str, String str2, Message message) {
        if (isPhoneTypeGsm()) {
            this.mMtkCi.doGeneralSimAuthentication(i, i2, i3, str, str2, message);
        }
    }

    public String getMvnoPattern(String str) {
        String gid1 = "";
        synchronized (this.mLock) {
            if (isPhoneTypeGsm() && this.mIccRecords.get() != null) {
                if (str.equals("spn")) {
                    gid1 = ((MtkSIMRecords) this.mIccRecords.get()).getSpNameInEfSpn();
                } else if (str.equals("imsi")) {
                    gid1 = ((MtkSIMRecords) this.mIccRecords.get()).isOperatorMvnoForImsi();
                } else if (str.equals("pnn")) {
                    gid1 = ((MtkSIMRecords) this.mIccRecords.get()).isOperatorMvnoForEfPnn();
                } else if (str.equals("gid")) {
                    gid1 = ((IccRecords) this.mIccRecords.get()).getGid1();
                } else {
                    Rlog.d(LOG_TAG, "getMvnoPattern: Wrong type = " + str);
                }
            }
        }
        return gid1;
    }

    public String getMvnoMatchType() {
        String mvnoMatchType = "";
        synchronized (this.mLock) {
            if (isPhoneTypeGsm()) {
                if (this.mIccRecords.get() != null) {
                    mvnoMatchType = ((MtkSIMRecords) this.mIccRecords.get()).getMvnoMatchType();
                }
                Rlog.d(LOG_TAG, "getMvnoMatchType: Type = " + mvnoMatchType);
            }
        }
        return mvnoMatchType;
    }

    protected void updateImsPhone() {
        Rlog.d(LOG_TAG, "updateImsPhone");
        MtkDcHelper mtkDcHelper = MtkDcHelper.getInstance();
        if (this.mImsServiceReady && this.mImsPhone == null) {
            super.updateImsPhone();
            if (mtkDcHelper != null) {
                mtkDcHelper.registerImsEvents(getPhoneId());
                return;
            }
            return;
        }
        if (!this.mImsServiceReady && this.mImsPhone != null) {
            if (mtkDcHelper != null) {
                mtkDcHelper.unregisterImsEvents(getPhoneId());
            }
            super.updateImsPhone();
        }
    }

    public void hangupAll() throws CallStateException {
        ((MtkGsmCdmaCallTracker) this.mCT).hangupAll();
    }

    public Call getCSRingingCall() {
        return this.mCT.mRingingCall;
    }

    boolean isInCSCall() {
        return getForegroundCall().getState().isAlive() || getBackgroundCall().getState().isAlive() || getCSRingingCall().getState().isAlive();
    }

    public void registerForCipherIndication(Handler handler, int i, Object obj) {
        this.mCipherIndicationRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForCipherIndication(Handler handler) {
        this.mCipherIndicationRegistrants.remove(handler);
    }

    public Connection dial(String str, PhoneInternalInterface.DialArgs dialArgs) throws CallStateException {
        if (!isPhoneTypeGsm() && dialArgs.uusInfo != null) {
            throw new CallStateException("Sending UUS information NOT supported in CDMA!");
        }
        boolean zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(getSubId(), str);
        if (hasC2kOverImsModem() && (!TelephonyManager.getDefault().hasIccCard(getPhoneId()) || getServiceState().getState() != 0)) {
            zIsEmergencyNumber = PhoneNumberUtils.isEmergencyNumber(str);
        }
        Phone phone = this.mImsPhone;
        tryTurnOffWifiForE911(zIsEmergencyNumber);
        boolean z = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(getSubId()).getBoolean("carrier_use_ims_first_for_emergency_bool");
        boolean z2 = isImsUseEnabled() && phone != null && (phone.isVolteEnabled() || phone.isWifiCallingEnabled() || (phone.isVideoEnabled() && VideoProfile.isVideo(dialArgs.videoState))) && phone.getServiceState().getState() == 0;
        boolean zIsLocalEmergencyNumber = PhoneNumberUtils.isLocalEmergencyNumber(getContext(), str);
        boolean z3 = phone != null && (zIsEmergencyNumber || zIsLocalEmergencyNumber) && z && ImsManager.getInstance(this.mContext, this.mPhoneId).isNonTtyOrTtyOnVolteEnabled() && phone.getServiceState().getState() != 3;
        if (hasC2kOverImsModem()) {
            Rlog.d(LOG_TAG, "keep AOSP");
        } else if (!MtkTelephonyManagerEx.getDefault().useVzwLogic() && !isPhoneTypeGsm()) {
            z3 = false;
        }
        if (this.mPhoneId != getMainCapabilityPhoneId() && !MtkImsManager.isSupportMims()) {
            z3 = false;
        }
        if (shouldProcessSelfActivation() || useImsForPCOChanged()) {
            logd("always use ImsPhone for self activation");
            z2 = true;
        }
        String strExtractNetworkPortionAlt = PhoneNumberUtils.extractNetworkPortionAlt(PhoneNumberUtils.stripSeparators(str));
        boolean z4 = (strExtractNetworkPortionAlt.startsWith("*") || strExtractNetworkPortionAlt.startsWith("#")) && strExtractNetworkPortionAlt.endsWith("#");
        boolean z5 = phone != null && phone.isUtEnabled();
        StringBuilder sb = new StringBuilder();
        sb.append("PhoneId = ");
        sb.append(this.mPhoneId);
        sb.append(", useImsForCall=");
        sb.append(z2);
        sb.append(", useImsForEmergency=");
        sb.append(z3);
        sb.append(", isLocalEmergency=");
        sb.append(zIsLocalEmergencyNumber);
        sb.append(", useImsForUt=");
        sb.append(z5);
        sb.append(", isUt=");
        sb.append(z4);
        sb.append(", imsPhone=");
        sb.append(phone);
        sb.append(", imsPhone.isVolteEnabled()=");
        sb.append(phone != null ? Boolean.valueOf(phone.isVolteEnabled()) : DataSubConstants.NO_SIM_VALUE);
        sb.append(", imsPhone.isVowifiEnabled()=");
        sb.append(phone != null ? Boolean.valueOf(phone.isWifiCallingEnabled()) : DataSubConstants.NO_SIM_VALUE);
        sb.append(", imsPhone.isVideoEnabled()=");
        sb.append(phone != null ? Boolean.valueOf(phone.isVideoEnabled()) : DataSubConstants.NO_SIM_VALUE);
        sb.append(", imsPhone.getServiceState().getState()=");
        sb.append(phone != null ? Integer.valueOf(phone.getServiceState().getState()) : DataSubConstants.NO_SIM_VALUE);
        logd(sb.toString());
        Phone.checkWfcWifiOnlyModeBeforeDial(this.mImsPhone, this.mPhoneId, this.mContext);
        if ((z2 && !z4) || ((z4 && z5) || z3)) {
            if (isInCSCall()) {
                Rlog.d(LOG_TAG, "has CS Call. Don't try IMS PS Call!");
            } else {
                try {
                    if (dialArgs.videoState == 0) {
                        logd("Trying IMS PS call");
                        return phone.dial(str, dialArgs);
                    }
                    if (SystemProperties.get("persist.vendor.vilte_support").equals("1")) {
                        logd("Trying IMS PS video call");
                        return phone.dial(str, dialArgs);
                    }
                    loge("Should not be here. (isInCSCall == false, videoState=" + dialArgs.videoState);
                } catch (CallStateException e) {
                    logd("IMS PS call exception " + e + "useImsForCall =" + z2 + ", imsPhone =" + phone);
                    tryTurnOnWifiForE911Finished();
                    if ("cs_fallback".equals(e.getMessage()) || (zIsEmergencyNumber && needRetryCsEmergency())) {
                        logi("IMS call failed with Exception: " + e.getMessage() + ". Falling back to CS.");
                    } else {
                        CallStateException callStateException = new CallStateException(e.getMessage());
                        callStateException.setStackTrace(e.getStackTrace());
                        throw callStateException;
                    }
                }
            }
        }
        if (SystemProperties.getInt("vendor.gsm.gcf.testmode", 0) != 2 && !isCdmaLessDevice() && this.mSST != null && this.mSST.mSS.getState() == 1 && this.mSST.mSS.getDataRegState() != 0 && !zIsEmergencyNumber) {
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

    public void handleMessage(Message message) {
        MtkSuppServHelper suppServHelper;
        String xCAPErrorMessageFromSysProp;
        MtkSuppServHelper suppServHelper2;
        String xCAPErrorMessageFromSysProp2;
        MtkSuppServHelper suppServHelper3;
        String xCAPErrorMessageFromSysProp3;
        MtkSuppServHelper suppServHelper4;
        String xCAPErrorMessageFromSysProp4;
        MtkSuppServHelper suppServHelper5;
        String xCAPErrorMessageFromSysProp5;
        MtkGsmCdmaConnection mtkGsmCdmaConnection;
        MtkSuppServHelper suppServHelper6;
        String xCAPErrorMessageFromSysProp6;
        MtkSuppServHelper suppServHelper7;
        String xCAPErrorMessageFromSysProp7;
        MtkSuppServHelper suppServHelper8;
        String xCAPErrorMessageFromSysProp8;
        switch (message.what) {
            case 2:
                logd("Event EVENT_SSN Received");
                if (isPhoneTypeGsm()) {
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (this.mSsnRegistrants.size() == 0) {
                        this.mCachedSsn = asyncResult;
                    }
                    this.mSsnRegistrants.notifyRegistrants(asyncResult);
                    return;
                }
                return;
            case 3:
                super.handleMessage(message);
                updateVoiceMail();
                return;
            case 7:
                String[] strArr = (String[]) ((AsyncResult) message.obj).result;
                if (strArr.length > 1) {
                    try {
                        onIncomingUSSD(Integer.parseInt(strArr[0]), strArr[1]);
                        return;
                    } catch (NumberFormatException e) {
                        Rlog.w(LOG_TAG, "error parsing USSD");
                        return;
                    }
                }
                return;
            case 12:
                if (supportMdAutoSetupIms()) {
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    IccRecords iccRecords = (IccRecords) this.mIccRecords.get();
                    Cfu cfu = (Cfu) asyncResult2.userObj;
                    if (asyncResult2.exception == null && iccRecords != null) {
                        if ((cfu.mServiceClass & 1) != 0) {
                            setVoiceCallForwardingFlag(1, message.arg1 == 1, cfu.mSetCfNumber);
                        }
                    } else if (supportMdAutoSetupIms()) {
                        CommandException commandException = asyncResult2.exception;
                        if (commandException.getCommandError() == CommandException.Error.OEM_ERROR_1 && commandException.getMessage() != null && commandException.getMessage().isEmpty() && (suppServHelper = MtkSuppServManager.getSuppServHelper(getPhoneId())) != null && (xCAPErrorMessageFromSysProp = suppServHelper.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1)) != null && !xCAPErrorMessageFromSysProp.isEmpty()) {
                            asyncResult2.exception = new CommandException(CommandException.Error.OEM_ERROR_1, xCAPErrorMessageFromSysProp);
                        }
                    }
                    if (cfu.mOnComplete != null) {
                        AsyncResult.forMessage(cfu.mOnComplete, asyncResult2.result, asyncResult2.exception);
                        cfu.mOnComplete.sendToTarget();
                        return;
                    }
                    return;
                }
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                IccRecords iccRecords2 = (IccRecords) this.mIccRecords.get();
                Cfu cfu2 = (Cfu) asyncResult3.userObj;
                if (asyncResult3.exception == null && iccRecords2 != null) {
                    if (queryCFUAgainAfterSet()) {
                        if (asyncResult3.result != null) {
                            CallForwardInfo[] callForwardInfoArr = (CallForwardInfo[]) asyncResult3.result;
                            if (callForwardInfoArr == null || callForwardInfoArr.length == 0) {
                                Rlog.d(LOG_TAG, "cfinfo is null or length is 0.");
                            } else {
                                Rlog.d(LOG_TAG, "[EVENT_SET_CALL_FORWARD_DONE] check cfinfo");
                                int i = 0;
                                while (true) {
                                    if (i < callForwardInfoArr.length) {
                                        if ((callForwardInfoArr[i].serviceClass & 1) == 0) {
                                            i++;
                                        } else {
                                            setVoiceCallForwardingFlag(1, callForwardInfoArr[i].status == 1, callForwardInfoArr[i].number);
                                        }
                                    }
                                }
                            }
                        } else {
                            Rlog.e(LOG_TAG, "EVENT_SET_CALL_FORWARD_DONE: ar.result is null.");
                        }
                    } else {
                        setVoiceCallForwardingFlag(1, message.arg1 == 1, cfu2.mSetCfNumber);
                    }
                }
                if (asyncResult3.exception != null && message.arg2 != 0) {
                    if (message.arg2 == 1) {
                        setSystemProperty("persist.vendor.radio.cfu.mode", "enabled_cfu_mode_on");
                    } else {
                        setSystemProperty("persist.vendor.radio.cfu.mode", "enabled_cfu_mode_off");
                    }
                }
                if (cfu2.mOnComplete != null) {
                    AsyncResult.forMessage(cfu2.mOnComplete, asyncResult3.result, asyncResult3.exception);
                    cfu2.mOnComplete.sendToTarget();
                    return;
                }
                return;
            case 13:
                Rlog.d(LOG_TAG, "mPhoneId= " + this.mPhoneId + "subId=" + getSubId());
                AsyncResult asyncResult4 = (AsyncResult) message.obj;
                if (asyncResult4.exception == null) {
                    handleCfuQueryResult((CallForwardInfo[]) asyncResult4.result);
                } else if (supportMdAutoSetupIms()) {
                    CommandException commandException2 = asyncResult4.exception;
                    if (commandException2.getCommandError() == CommandException.Error.OEM_ERROR_1 && commandException2.getMessage() != null && commandException2.getMessage().isEmpty() && (suppServHelper2 = MtkSuppServManager.getSuppServHelper(getPhoneId())) != null && (xCAPErrorMessageFromSysProp2 = suppServHelper2.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1)) != null && !xCAPErrorMessageFromSysProp2.isEmpty()) {
                        asyncResult4.exception = new CommandException(CommandException.Error.OEM_ERROR_1, xCAPErrorMessageFromSysProp2);
                    }
                }
                Message message2 = (Message) asyncResult4.userObj;
                if (message2 != null) {
                    AsyncResult.forMessage(message2, asyncResult4.result, asyncResult4.exception);
                    message2.sendToTarget();
                    return;
                }
                return;
            case 16:
                super.handleMessage(message);
                if (isPhoneTypeGsm()) {
                    AsyncResult asyncResult5 = (AsyncResult) message.obj;
                    boolean z = !this.mContext.getResources().getBoolean(R.^attr-private.showAtTop);
                    if (asyncResult5 != null && asyncResult5.exception != null) {
                        z = true;
                    }
                    Rlog.d(LOG_TAG, "EVENT_SET_NETWORK_MANUAL_COMPLETE, restoreSelection=" + z + " exception=" + z);
                    if (!z && z) {
                        setMtkNetworkSelection(1, null, null);
                        return;
                    }
                    return;
                }
                return;
            case 18:
                Rlog.d(LOG_TAG, "EVENT_SET_CLIR_COMPLETE");
                AsyncResult asyncResult6 = (AsyncResult) message.obj;
                if (asyncResult6.exception == null) {
                    saveClirSetting(message.arg1);
                }
                if (asyncResult6.exception != null && (asyncResult6.exception instanceof CommandException)) {
                    CommandException commandException3 = asyncResult6.exception;
                    Rlog.d(LOG_TAG, "EVENT_SET_CLIR_COMPLETE: cmdException error:" + commandException3.getCommandError());
                    if (supportMdAutoSetupIms() && commandException3 != null) {
                        if ((isOp(MtkOperatorUtils.OPID.OP01) || isOp(MtkOperatorUtils.OPID.OP02)) && isUtError(commandException3.getCommandError())) {
                            Rlog.d(LOG_TAG, "return REQUEST_NOT_SUPPORTED");
                            asyncResult6.exception = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                        } else if (commandException3.getCommandError() == CommandException.Error.OEM_ERROR_1) {
                            if (commandException3.getMessage() != null && commandException3.getMessage().isEmpty() && (suppServHelper3 = MtkSuppServManager.getSuppServHelper(getPhoneId())) != null && (xCAPErrorMessageFromSysProp3 = suppServHelper3.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1)) != null && !xCAPErrorMessageFromSysProp3.isEmpty()) {
                                asyncResult6.exception = new CommandException(CommandException.Error.OEM_ERROR_1, xCAPErrorMessageFromSysProp3);
                            }
                        } else {
                            Rlog.d(LOG_TAG, "return Original Error");
                        }
                    }
                }
                Message message3 = (Message) asyncResult6.userObj;
                if (message3 != null) {
                    AsyncResult.forMessage(message3, asyncResult6.result, asyncResult6.exception);
                    message3.sendToTarget();
                    return;
                }
                return;
            case 25:
                if (!isPhoneTypeGsm()) {
                    boolean zIsInEcm = isInEcm();
                    super.handleMessage(message);
                    if (!zIsInEcm) {
                        this.mDcTracker.setInternalDataEnabled(false);
                        notifyEmergencyCallRegistrants(true);
                        return;
                    }
                    return;
                }
                super.handleMessage(message);
                return;
            case 29:
                Rlog.d(LOG_TAG, "EVENT_ICC_RECORD_EVENTS");
                processIccRecordEvents(((Integer) ((AsyncResult) message.obj).result).intValue());
                MtkSuppServHelper suppServHelper9 = MtkSuppServManager.getSuppServHelper(getPhoneId());
                if (suppServHelper9 != null) {
                    suppServHelper9.setIccRecordsReady();
                    return;
                }
                return;
            case 109:
                Rlog.d(LOG_TAG, "mPhoneId = " + this.mPhoneId + ", subId = " + getSubId());
                AsyncResult asyncResult7 = (AsyncResult) message.obj;
                StringBuilder sb = new StringBuilder();
                sb.append("[EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE]ar.exception = ");
                sb.append(asyncResult7.exception);
                Rlog.d(LOG_TAG, sb.toString());
                if (asyncResult7.exception == null) {
                    handleCfuInTimeSlotQueryResult((MtkCallForwardInfo[]) asyncResult7.result);
                }
                Rlog.d(LOG_TAG, "[EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE]msg.arg1 = " + message.arg1);
                if (asyncResult7.exception != null && (asyncResult7.exception instanceof CommandException)) {
                    CommandException commandException4 = asyncResult7.exception;
                    Rlog.d(LOG_TAG, "[EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE] cmdException error:" + commandException4.getCommandError());
                    if (message.arg1 == 1 && commandException4 != null && commandException4.getCommandError() == CommandException.Error.REQUEST_NOT_SUPPORTED && this.mSST != null && this.mSST.mSS != null && this.mSST.mSS.getState() == 0) {
                        getCallForwardingOption(0, obtainMessage(13));
                    }
                    if (supportMdAutoSetupIms() && commandException4 != null && commandException4.getCommandError() == CommandException.Error.OEM_ERROR_2) {
                        Rlog.d(LOG_TAG, "return REQUEST_NOT_SUPPORTED");
                        asyncResult7.exception = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                    }
                }
                Message message4 = (Message) asyncResult7.userObj;
                if (message4 != null) {
                    AsyncResult.forMessage(message4, asyncResult7.result, asyncResult7.exception);
                    message4.sendToTarget();
                    return;
                }
                return;
            case 110:
                AsyncResult asyncResult8 = (AsyncResult) message.obj;
                IccRecords iccRecords3 = (IccRecords) this.mIccRecords.get();
                CfuEx cfuEx = (CfuEx) asyncResult8.userObj;
                if (asyncResult8.exception == null && iccRecords3 != null) {
                    iccRecords3.setVoiceCallForwardingFlag(1, message.arg1 == 1, cfuEx.mSetCfNumber);
                    saveTimeSlot(cfuEx.mSetTimeSlot);
                }
                if (cfuEx.mOnComplete != null) {
                    AsyncResult.forMessage(cfuEx.mOnComplete, asyncResult8.result, asyncResult8.exception);
                    cfuEx.mOnComplete.sendToTarget();
                    return;
                }
                return;
            case 111:
                AsyncResult asyncResult9 = (AsyncResult) message.obj;
                RadioCapability radioCapability = (RadioCapability) asyncResult9.result;
                if (asyncResult9.exception != null) {
                    Rlog.d(LOG_TAG, "RIL_UNSOL_RADIO_CAPABILITY fail, don't change capability");
                } else {
                    radioCapabilityUpdated(radioCapability);
                }
                Rlog.d(LOG_TAG, "EVENT_UNSOL_RADIO_CAPABILITY_CHANGED: rc: " + radioCapability);
                return;
            case EVENT_GET_CALL_WAITING_DONE:
                AsyncResult asyncResult10 = (AsyncResult) message.obj;
                Rlog.d(LOG_TAG, "[EVENT_GET_CALL_WAITING_]ar.exception = " + asyncResult10.exception);
                Message message5 = (Message) asyncResult10.userObj;
                if (asyncResult10.exception == null) {
                    int[] iArr = (int[]) asyncResult10.result;
                    try {
                        Rlog.d(LOG_TAG, "EVENT_GET_CALL_WAITING_DONE cwArray[0]:cwArray[1] = " + iArr[0] + ":" + iArr[1]);
                        if (iArr[0] == 1 && (iArr[1] & 1) == 1) {
                            z = true;
                        }
                        setTerminalBasedCallWaiting(z, null);
                        if (message5 != null) {
                            AsyncResult.forMessage(message5, asyncResult10.result, (Throwable) null);
                            message5.sendToTarget();
                            return;
                        }
                        return;
                    } catch (ArrayIndexOutOfBoundsException e2) {
                        Rlog.e(LOG_TAG, "EVENT_GET_CALL_WAITING_DONE: improper result: err =" + e2.getMessage());
                        if (message5 != null) {
                            AsyncResult.forMessage(message5, asyncResult10.result, (Throwable) null);
                            message5.sendToTarget();
                            return;
                        }
                        return;
                    }
                }
                if (supportMdAutoSetupIms()) {
                    CommandException commandException5 = asyncResult10.exception;
                    if (commandException5.getCommandError() == CommandException.Error.OEM_ERROR_1 && commandException5.getMessage() != null && commandException5.getMessage().isEmpty() && (suppServHelper4 = MtkSuppServManager.getSuppServHelper(getPhoneId())) != null && (xCAPErrorMessageFromSysProp4 = suppServHelper4.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1)) != null && !xCAPErrorMessageFromSysProp4.isEmpty()) {
                        asyncResult10.exception = new CommandException(CommandException.Error.OEM_ERROR_1, xCAPErrorMessageFromSysProp4);
                    }
                }
                if (message5 != null) {
                    AsyncResult.forMessage(message5, asyncResult10.result, asyncResult10.exception);
                    message5.sendToTarget();
                    return;
                }
                return;
            case EVENT_SET_CALL_WAITING_DONE:
                AsyncResult asyncResult11 = (AsyncResult) message.obj;
                Message message6 = (Message) asyncResult11.userObj;
                if (asyncResult11.exception != null) {
                    Rlog.d(LOG_TAG, "EVENT_SET_CALL_WAITING_DONE: ar.exception=" + asyncResult11.exception);
                    if (supportMdAutoSetupIms()) {
                        CommandException commandException6 = asyncResult11.exception;
                        if (commandException6.getCommandError() == CommandException.Error.OEM_ERROR_1 && commandException6.getMessage() != null && commandException6.getMessage().isEmpty() && (suppServHelper5 = MtkSuppServManager.getSuppServHelper(getPhoneId())) != null && (xCAPErrorMessageFromSysProp5 = suppServHelper5.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1)) != null && !xCAPErrorMessageFromSysProp5.isEmpty()) {
                            asyncResult11.exception = new CommandException(CommandException.Error.OEM_ERROR_1, xCAPErrorMessageFromSysProp5);
                        }
                    }
                    if (message6 != null) {
                        AsyncResult.forMessage(message6, asyncResult11.result, asyncResult11.exception);
                        message6.sendToTarget();
                        return;
                    }
                    return;
                }
                setTerminalBasedCallWaiting(message.arg1 == 1, message6);
                return;
            case 1001:
                Rlog.d(LOG_TAG, "handle EVENT_GET_APC_INFO");
                AsyncResult asyncResult12 = (AsyncResult) message.obj;
                PseudoCellInfoResult pseudoCellInfoResult = (PseudoCellInfoResult) asyncResult12.userObj;
                if (pseudoCellInfoResult == null) {
                    Rlog.e(LOG_TAG, "EVENT_GET_APC_INFO: result return null");
                    return;
                }
                synchronized (pseudoCellInfoResult.lockObj) {
                    if (asyncResult12.exception != null) {
                        Rlog.d(LOG_TAG, "EVENT_GET_APC_INFO: error ret null, e=" + asyncResult12.exception);
                        pseudoCellInfoResult.infos = null;
                    } else {
                        pseudoCellInfoResult.infos = new PseudoCellInfo((int[]) asyncResult12.result);
                    }
                    pseudoCellInfoResult.lockObj.notify();
                    break;
                }
                return;
            case 1002:
                AsyncResult asyncResult13 = (AsyncResult) message.obj;
                MtkSuppCrssNotification mtkSuppCrssNotification = (MtkSuppCrssNotification) asyncResult13.result;
                if (mtkSuppCrssNotification.code == 2) {
                    if (getRingingCall().getState() != Call.State.IDLE) {
                        List connections = getCSRingingCall().getConnections();
                        if (connections.isEmpty()) {
                            Rlog.d(LOG_TAG, "Cannot set number presentation to connection (CS conn empty): " + mtkSuppCrssNotification.cli_validity);
                        } else {
                            MtkGsmCdmaConnection mtkGsmCdmaConnection2 = (MtkGsmCdmaConnection) connections.get(0);
                            Rlog.d(LOG_TAG, "set number presentation to connection : " + mtkSuppCrssNotification.cli_validity);
                            switch (mtkSuppCrssNotification.cli_validity) {
                                case 1:
                                    mtkGsmCdmaConnection2.setNumberPresentation(2);
                                    break;
                                case 2:
                                    mtkGsmCdmaConnection2.setNumberPresentation(3);
                                    break;
                                case 3:
                                    mtkGsmCdmaConnection2.setNumberPresentation(4);
                                    break;
                                default:
                                    mtkGsmCdmaConnection2.setNumberPresentation(1);
                                    break;
                            }
                        }
                    }
                } else if (mtkSuppCrssNotification.code == 3) {
                    Rlog.d(LOG_TAG, "[COLP]noti.number = " + Rlog.pii(SDBG, mtkSuppCrssNotification.number));
                    if (getForegroundCall().getState() != Call.State.IDLE && (mtkGsmCdmaConnection = (MtkGsmCdmaConnection) getForegroundCall().getConnections().get(0)) != null && mtkGsmCdmaConnection.getAddress() != null && !mtkGsmCdmaConnection.getAddress().equals(mtkSuppCrssNotification.number)) {
                        mtkGsmCdmaConnection.setRedirectingAddress(mtkSuppCrssNotification.number);
                        Rlog.d(LOG_TAG, "[COLP]Redirecting address = " + Rlog.pii(SDBG, mtkGsmCdmaConnection.getRedirectingAddress()));
                    }
                }
                if (this.mCallRelatedSuppSvcRegistrants.size() == 0) {
                    this.mCachedCrssn = asyncResult13;
                }
                this.mCallRelatedSuppSvcRegistrants.notifyRegistrants(asyncResult13);
                return;
            case 1003:
                AsyncResult asyncResult14 = (AsyncResult) message.obj;
                Rlog.d(LOG_TAG, "handle EVENT_SPEECH_CODEC_INFO : " + ((Integer) asyncResult14.result).intValue());
                notifySpeechCodecInfo(((Integer) asyncResult14.result).intValue());
                return;
            case 1004:
                if (this.mCallbackLatch != null) {
                    this.mCallbackLatch.countDown();
                }
                Rlog.d(LOG_TAG, "EVENT_SET_SS_PROPERTY done");
                return;
            case EVENT_IMS_UT_DONE:
                Rlog.d(LOG_TAG, "EVENT_IMS_UT_DONE: Enter");
                handleImsUtDone(message);
                return;
            case 2001:
                handleImsUtCsfb(message);
                return;
            case EVENT_USSI_CSFB:
                handleUssiCsfb((String) message.obj);
                return;
            case EVENT_GET_CLIR_COMPLETE:
                Rlog.d(LOG_TAG, "EVENT_GET_CLIR_COMPLETE");
                AsyncResult asyncResult15 = (AsyncResult) message.obj;
                if (asyncResult15.exception != null && (asyncResult15.exception instanceof CommandException)) {
                    CommandException commandException7 = asyncResult15.exception;
                    Rlog.d(LOG_TAG, "EVENT_GET_CLIR_COMPLETE: cmdException error:" + commandException7.getCommandError());
                    if (supportMdAutoSetupIms() && commandException7 != null) {
                        if (isOp(MtkOperatorUtils.OPID.OP01) || isOp(MtkOperatorUtils.OPID.OP02)) {
                            if (isUtError(commandException7.getCommandError())) {
                                Rlog.d(LOG_TAG, "return REQUEST_NOT_SUPPORTED");
                                asyncResult15.exception = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                            } else {
                                Rlog.d(LOG_TAG, "return Original Error");
                            }
                        } else if (commandException7.getCommandError() == CommandException.Error.OEM_ERROR_1) {
                            Rlog.d(LOG_TAG, "cmdException.getMessage():" + commandException7.getMessage());
                            if (commandException7.getMessage() != null && commandException7.getMessage().isEmpty() && (suppServHelper6 = MtkSuppServManager.getSuppServHelper(getPhoneId())) != null && (xCAPErrorMessageFromSysProp6 = suppServHelper6.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1)) != null && !xCAPErrorMessageFromSysProp6.isEmpty()) {
                                asyncResult15.exception = new CommandException(CommandException.Error.OEM_ERROR_1, xCAPErrorMessageFromSysProp6);
                            }
                        }
                    }
                }
                Message message7 = (Message) asyncResult15.userObj;
                if (message7 != null) {
                    AsyncResult.forMessage(message7, asyncResult15.result, asyncResult15.exception);
                    message7.sendToTarget();
                    return;
                }
                return;
            case EVENT_SET_CALL_BARRING_COMPLETE:
                Rlog.d(LOG_TAG, "EVENT_SET_CALL_BARRING_COMPLETE");
                AsyncResult asyncResult16 = (AsyncResult) message.obj;
                if (asyncResult16.exception != null && (asyncResult16.exception instanceof CommandException)) {
                    CommandException commandException8 = asyncResult16.exception;
                    Rlog.d(LOG_TAG, "EVENT_SET_CALL_BARRING_COMPLETE: cmdException error:" + commandException8.getCommandError());
                    if (supportMdAutoSetupIms() && commandException8 != null) {
                        if (isOp(MtkOperatorUtils.OPID.OP01)) {
                            if (isUtError(commandException8.getCommandError())) {
                                Rlog.d(LOG_TAG, "return REQUEST_NOT_SUPPORTED");
                                asyncResult16.exception = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                            } else {
                                Rlog.d(LOG_TAG, "return Original Error");
                            }
                        } else if (commandException8.getCommandError() == CommandException.Error.OEM_ERROR_1 && commandException8.getMessage() != null && commandException8.getMessage().isEmpty() && (suppServHelper7 = MtkSuppServManager.getSuppServHelper(getPhoneId())) != null && (xCAPErrorMessageFromSysProp7 = suppServHelper7.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1)) != null && !xCAPErrorMessageFromSysProp7.isEmpty()) {
                            asyncResult16.exception = new CommandException(CommandException.Error.OEM_ERROR_1, xCAPErrorMessageFromSysProp7);
                        }
                    }
                }
                Message message8 = (Message) asyncResult16.userObj;
                if (message8 != null) {
                    AsyncResult.forMessage(message8, asyncResult16.result, asyncResult16.exception);
                    message8.sendToTarget();
                    return;
                }
                return;
            case EVENT_GET_CALL_BARRING_COMPLETE:
                Rlog.d(LOG_TAG, "EVENT_GET_CALL_BARRING_COMPLETE");
                AsyncResult asyncResult17 = (AsyncResult) message.obj;
                if (asyncResult17.exception != null && (asyncResult17.exception instanceof CommandException)) {
                    CommandException commandException9 = asyncResult17.exception;
                    Rlog.d(LOG_TAG, "EVENT_GET_CALL_BARRING_COMPLETE: cmdException error:" + commandException9.getCommandError());
                    if (supportMdAutoSetupIms() && commandException9 != null) {
                        if (isOp(MtkOperatorUtils.OPID.OP01) || isOp(MtkOperatorUtils.OPID.OP09)) {
                            if (isUtError(commandException9.getCommandError())) {
                                Rlog.d(LOG_TAG, "return REQUEST_NOT_SUPPORTED");
                                asyncResult17.exception = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
                            } else {
                                Rlog.d(LOG_TAG, "return Original Error");
                            }
                        } else if (commandException9.getCommandError() == CommandException.Error.OEM_ERROR_1 && commandException9.getMessage() != null && commandException9.getMessage().isEmpty() && (suppServHelper8 = MtkSuppServManager.getSuppServHelper(getPhoneId())) != null && (xCAPErrorMessageFromSysProp8 = suppServHelper8.getXCAPErrorMessageFromSysProp(CommandException.Error.OEM_ERROR_1)) != null && !xCAPErrorMessageFromSysProp8.isEmpty()) {
                            asyncResult17.exception = new CommandException(CommandException.Error.OEM_ERROR_1, xCAPErrorMessageFromSysProp8);
                        }
                    }
                }
                Message message9 = (Message) asyncResult17.userObj;
                if (message9 != null) {
                    AsyncResult.forMessage(message9, asyncResult17.result, asyncResult17.exception);
                    message9.sendToTarget();
                    return;
                }
                return;
            default:
                super.handleMessage(message);
                return;
        }
    }

    public void setApcMode(int i, boolean z, int i2) {
        if (isPhoneTypeGsm()) {
            this.mMtkCi.setApcMode(i, z, i2, null);
        } else {
            Rlog.d(LOG_TAG, "setApcMode: not possible in CDMA");
        }
    }

    private class PseudoCellInfoResult {
        PseudoCellInfo infos;
        Object lockObj;

        private PseudoCellInfoResult() {
            this.infos = null;
            this.lockObj = new Object();
        }
    }

    public PseudoCellInfo getApcInfo() {
        if (isPhoneTypeGsm()) {
            PseudoCellInfoResult pseudoCellInfoResult = new PseudoCellInfoResult();
            synchronized (pseudoCellInfoResult.lockObj) {
                pseudoCellInfoResult.infos = null;
                this.mMtkCi.getApcInfo(obtainMessage(1001, pseudoCellInfoResult));
                try {
                    pseudoCellInfoResult.lockObj.wait(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            synchronized (pseudoCellInfoResult.lockObj) {
                if (pseudoCellInfoResult.infos != null) {
                    Rlog.d(LOG_TAG, "getApcInfo return: list.size = " + pseudoCellInfoResult.infos.toString());
                    return pseudoCellInfoResult.infos;
                }
                Rlog.d(LOG_TAG, "getApcInfo return null");
            }
        } else {
            Rlog.d(LOG_TAG, "getApcInfo: not possible in CDMA");
        }
        return null;
    }

    public void registerForCrssSuppServiceNotification(Handler handler, int i, Object obj) {
        this.mCallRelatedSuppSvcRegistrants.addUnique(handler, i, obj);
        if (this.mCachedCrssn != null) {
            this.mCallRelatedSuppSvcRegistrants.notifyRegistrants(this.mCachedCrssn);
            this.mCachedCrssn = null;
        }
    }

    public void unregisterForCrssSuppServiceNotification(Handler handler) {
        this.mCallRelatedSuppSvcRegistrants.remove(handler);
        this.mCachedCrssn = null;
    }

    public void registerForSuppServiceNotification(Handler handler, int i, Object obj) {
        this.mSsnRegistrants.addUnique(handler, i, obj);
        if (this.mCachedSsn != null) {
            this.mSsnRegistrants.notifyRegistrants(this.mCachedSsn);
            this.mCachedSsn = null;
        }
    }

    public void unregisterForSuppServiceNotification(Handler handler) {
        this.mSsnRegistrants.remove(handler);
        this.mCachedSsn = null;
    }

    public boolean handleInCallMmiCommands(String str) throws CallStateException {
        if (!isPhoneTypeGsm()) {
            loge("method handleInCallMmiCommands is NOT supported in CDMA!");
            return false;
        }
        Phone phone = this.mImsPhone;
        if (phone != null && phone.getServiceState().getState() == 0 && !isInCSCall()) {
            return phone.handleInCallMmiCommands(str);
        }
        if (!isInCall() || TextUtils.isEmpty(str)) {
            return false;
        }
        switch (str.charAt(0)) {
            case '0':
                return handleUdubIncallSupplementaryService(str);
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

    private boolean handleUdubIncallSupplementaryService(String str) {
        if (str.length() > 1) {
            return false;
        }
        if (getRingingCall().getState() != Call.State.IDLE || getBackgroundCall().getState() != Call.State.IDLE) {
            Rlog.d(LOG_TAG, "MmiCode 0: hangupWaitingOrBackground");
            this.mCT.hangupWaitingOrBackground();
        }
        return true;
    }

    public boolean isVideoEnabled() {
        Phone phone = this.mImsPhone;
        if (phone != null && phone.getServiceState().getState() == 0) {
            return phone.isVideoEnabled();
        }
        return false;
    }

    public void registerForSpeechCodecInfo(Handler handler, int i, Object obj) {
        this.mSpeechCodecInfoRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForSpeechCodecInfo(Handler handler) {
        this.mSpeechCodecInfoRegistrants.remove(handler);
    }

    void notifySpeechCodecInfo(int i) {
        this.mSpeechCodecInfoRegistrants.notifyResult(Integer.valueOf(i));
    }

    public void queryPhbStorageInfo(int i, Message message) {
        if (!CsimPhbUtil.hasModemPhbEnhanceCapability(getIccFileHandler())) {
            CsimPhbUtil.getPhbRecordInfo(message);
        } else {
            this.mMtkCi.queryPhbStorageInfo(i, message);
        }
    }

    public void registerForNetworkInfo(Handler handler, int i, Object obj) {
        this.mMtkCi.registerForNetworkInfo(handler, i, obj);
    }

    public void unregisterForNetworkInfo(Handler handler) {
        this.mMtkCi.unregisterForNetworkInfo(handler);
    }

    public void registerForCdmaCallAccepted(Handler handler, int i, Object obj) {
        this.mCdmaCallAcceptedRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForCdmaCallAccepted(Handler handler) {
        this.mCdmaCallAcceptedRegistrants.remove(handler);
    }

    public void notifyCdmaCallAccepted() {
        Rlog.d(LOG_TAG, "notifyCdmaCallAccepted");
        this.mCdmaCallAcceptedRegistrants.notifyRegistrants(new AsyncResult((Object) null, this, (Throwable) null));
    }

    public void setRxTestConfig(int i, Message message) {
        Rlog.d(LOG_TAG, "set Rx Test Config");
        this.mMtkCi.setRxTestConfig(i, message);
    }

    public void getRxTestResult(Message message) {
        Rlog.d(LOG_TAG, "get Rx Test Result");
        this.mMtkCi.getRxTestResult(message);
    }

    public void getPolCapability(Message message) {
        this.mMtkCi.getPOLCapability(message);
    }

    public void getPol(Message message) {
        this.mMtkCi.getCurrentPOLList(message);
    }

    public void setPolEntry(NetworkInfoWithAcT networkInfoWithAcT, Message message) {
        this.mMtkCi.setPOLEntry(networkInfoWithAcT.getPriority(), networkInfoWithAcT.getOperatorNumeric(), networkInfoWithAcT.getAccessTechnology(), message);
    }

    public List<? extends MmiCode> getPendingMmiCodes() {
        Rlog.d(LOG_TAG, "getPendingMmiCodes");
        dumpPendingMmi();
        ImsPhone imsPhone = this.mImsPhone;
        ArrayList arrayList = new ArrayList();
        if (imsPhone != null && imsPhone.getServiceState().getState() == 0) {
            Iterator it = imsPhone.getPendingMmiCodes().iterator();
            while (it.hasNext()) {
                arrayList.add((ImsPhoneMmiCode) it.next());
            }
        }
        ArrayList arrayList2 = new ArrayList(this.mPendingMMIs);
        arrayList2.addAll(arrayList);
        Rlog.d(LOG_TAG, "allPendingMMIs.size() = " + arrayList2.size());
        int size = arrayList2.size();
        for (int i = 0; i < size; i++) {
            Rlog.d(LOG_TAG, "dump allPendingMMIs: " + arrayList2.get(i));
        }
        return arrayList2;
    }

    public void notifyCallForwardingIndicator() {
        int simState = TelephonyManager.from(this.mContext).getSimState(this.mPhoneId);
        Rlog.d(LOG_TAG, "notifyCallForwardingIndicator: sim state = " + simState);
        if (simState == 5) {
            this.mNotifier.notifyCallForwardingChanged(this);
        }
    }

    public void notifyCallForwardingIndicatorWithoutCheckSimState() {
        Rlog.d(LOG_TAG, "notifyCallForwardingIndicatorWithoutCheckSimState");
        this.mNotifier.notifyCallForwardingChanged(this);
    }

    public boolean handlePinMmi(String str) {
        MtkGsmMmiCode mtkGsmMmiCodeNewFromDialString;
        if (isPhoneTypeGsm()) {
            mtkGsmMmiCodeNewFromDialString = MtkGsmMmiCode.newFromDialString(str, this, (UiccCardApplication) this.mUiccApplication.get(), null);
        } else {
            UiccProfile uiccProfile = getUiccProfile();
            if (uiccProfile != null && uiccProfile.getApplication(1) != null) {
                mtkGsmMmiCodeNewFromDialString = CdmaMmiCode.newFromDialString(str, this, uiccProfile.getApplication(1));
            } else {
                mtkGsmMmiCodeNewFromDialString = CdmaMmiCode.newFromDialString(str, this, (UiccCardApplication) this.mUiccApplication.get());
            }
        }
        if (mtkGsmMmiCodeNewFromDialString != null && mtkGsmMmiCodeNewFromDialString.isPinPukCommand()) {
            this.mPendingMMIs.add(mtkGsmMmiCodeNewFromDialString);
            Rlog.d(LOG_TAG, "handlePinMmi: " + str + ", mmi=" + mtkGsmMmiCodeNewFromDialString);
            dumpPendingMmi();
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mtkGsmMmiCodeNewFromDialString, (Throwable) null));
            try {
                mtkGsmMmiCodeNewFromDialString.processCode();
            } catch (CallStateException e) {
            }
            return true;
        }
        loge("Mmi is null or unrecognized!");
        return false;
    }

    protected void sendUssdResponse(String str, CharSequence charSequence, int i, ResultReceiver resultReceiver) {
        UssdResponse ussdResponse = new UssdResponse(str, charSequence);
        Bundle bundle = new Bundle();
        Rlog.d(LOG_TAG, "sendUssdResponse with wrappedCallback");
        bundle.putParcelable("USSD_RESPONSE", ussdResponse);
        resultReceiver.send(i, bundle);
    }

    public void sendUssdResponse(String str) {
        if (isPhoneTypeGsm()) {
            MtkGsmMmiCode mtkGsmMmiCodeNewFromUssdUserInput = MtkGsmMmiCode.newFromUssdUserInput(str, this, (UiccCardApplication) this.mUiccApplication.get());
            this.mPendingMMIs.add(mtkGsmMmiCodeNewFromUssdUserInput);
            Rlog.d(LOG_TAG, "sendUssdResponse: " + str + ", mmi=" + mtkGsmMmiCodeNewFromUssdUserInput);
            dumpPendingMmi();
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mtkGsmMmiCodeNewFromUssdUserInput, (Throwable) null));
            mtkGsmMmiCodeNewFromUssdUserInput.sendUssd(str);
            return;
        }
        loge("sendUssdResponse: not possible in CDMA");
    }

    public void getCallForwardingOption(int i, Message message) {
        getCallForwardingOptionForServiceClass(i, 1, message);
    }

    public void getCallForwardingOptionForServiceClass(int i, int i2, Message message) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper != null) {
            suppServQueueHelper.getCallForwardingOptionForServiceClass(i, i2, message, getPhoneId());
        } else {
            Rlog.d(LOG_TAG, "ssQueueHelper not exist, getCallForwardingOptionForServiceClass");
            getCallForwardingOptionInternal(i, i2, message);
        }
    }

    public void getCallForwardingOptionInternal(int i, int i2, Message message) {
        Message messageObtainMessage;
        if (isPhoneTypeGsm() || isGsmSsPrefer()) {
            MtkImsPhone mtkImsPhone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "getCallForwardingOptionForServiceClass enter, CFReason:" + i + ", serviceClass:" + i2);
                if (mtkImsPhone != null && (mtkImsPhone.getServiceState().getState() == 0 || mtkImsPhone.isUtEnabled())) {
                    if (isOpReregisterForCF() && message.arg2 == 1) {
                        Rlog.d(LOG_TAG, "Set ims dereg to ON.");
                        SystemProperties.set(IMS_DEREG_PROP, "1");
                    }
                    mtkImsPhone.getCallForwardingOption(i, message);
                    return;
                }
                if (isValidCommandInterfaceCFReason(i)) {
                    logd("requesting call forwarding query.");
                    if (i == 0) {
                        message = obtainMessage(13, message);
                    }
                    this.mCi.queryCallForwardStatus(i, i2, (String) null, message);
                    return;
                }
                return;
            }
            if (getCsFallbackStatus() == 0 && mtkImsPhone != null && ((mtkImsPhone.getServiceState().getState() == 0 || mtkImsPhone.isUtEnabled()) && (mtkImsPhone.isVolteEnabled() || (mtkImsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(12, message);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i2);
                Message messageObtainMessage2 = obtainMessage(EVENT_IMS_UT_DONE, mtkSuppSrvRequestObtain);
                setServiceClass(i2);
                if (isOpReregisterForCF() && message.arg2 == 1) {
                    Rlog.d(LOG_TAG, "Set ims dereg to ON.");
                    SystemProperties.set(IMS_DEREG_PROP, "1");
                }
                mtkImsPhone.getCallForwardingOption(i, messageObtainMessage2);
                return;
            }
            if (isValidCommandInterfaceCFReason(i)) {
                logd("requesting call forwarding query.");
                if (i == 0) {
                    messageObtainMessage = obtainMessage(13, message);
                } else {
                    messageObtainMessage = message;
                }
                if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                    if (isInCSCall() && getPhoneType() == 2) {
                        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                        return;
                    } else {
                        this.mMtkSSReqDecisionMaker.queryCallForwardStatus(i, i2, null, messageObtainMessage);
                        return;
                    }
                }
                if (getCsFallbackStatus() == 1) {
                    setCsFallbackStatus(0);
                }
                if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                    sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                    return;
                } else if (isNotSupportUtToCS()) {
                    sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                    return;
                } else {
                    Rlog.d(LOG_TAG, "mCi.queryCallForwardStatus.");
                    this.mCi.queryCallForwardStatus(i, i2, (String) null, messageObtainMessage);
                    return;
                }
            }
            return;
        }
        loge("getCallForwardingOptionForServiceClass: not possible in CDMA");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    public void setCallForwardingOption(int i, int i2, String str, int i3, Message message) {
        setCallForwardingOptionForServiceClass(i, i2, str, i3, 1, message);
    }

    public void setCallForwardingOptionForServiceClass(int i, int i2, String str, int i3, int i4, Message message) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper != null) {
            suppServQueueHelper.setCallForwardingOptionForServiceClass(i, i2, str, i3, i4, message, getPhoneId());
        } else {
            Rlog.d(LOG_TAG, "ssQueueHelper not exist, setCallForwardingOptionForServiceClass");
            setCallForwardingOptionInternal(i, i2, str, i3, i4, message);
        }
    }

    public void setCallForwardingOptionInternal(int i, int i2, String str, int i3, int i4, Message message) {
        Message messageObtainMessage;
        Message messageObtainMessage2 = message;
        if (isPhoneTypeGsm() || isGsmSsPrefer()) {
            MtkImsPhone mtkImsPhone = this.mImsPhone;
            int i5 = 2;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "setCallForwardingOptionForServiceClass enter, CFAction:" + i + ", CFReason:" + i2 + ", dialingNumber:" + str + ", timerSeconds:" + i3 + ", serviceClass:" + i4);
                if (mtkImsPhone != null && (mtkImsPhone.getServiceState().getState() == 0 || mtkImsPhone.isUtEnabled())) {
                    mtkImsPhone.setCallForwardingOption(i, i2, str, i4, i3, messageObtainMessage2);
                    return;
                }
                if (isValidCommandInterfaceCFAction(i) && isValidCommandInterfaceCFReason(i2)) {
                    if (i2 == 0) {
                        String telephonyProperty = TelephonyManager.getTelephonyProperty(getPhoneId(), "persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
                        if (!"enabled_cfu_mode_on".equals(telephonyProperty)) {
                            if (!"enabled_cfu_mode_off".equals(telephonyProperty)) {
                                i5 = 0;
                            }
                        } else {
                            i5 = 1;
                        }
                        setSystemProperty("persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
                        messageObtainMessage2 = obtainMessage(12, isCfEnable(i) ? 1 : 0, i5, new Cfu(str, messageObtainMessage2, i4));
                    }
                    this.mCi.setCallForward(i, i2, i4, str, i3, messageObtainMessage2);
                    return;
                }
                return;
            }
            if (getCsFallbackStatus() == 0 && mtkImsPhone != null && ((mtkImsPhone.getServiceState().getState() == 0 || mtkImsPhone.isUtEnabled()) && (mtkImsPhone.isVolteEnabled() || (mtkImsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(11, messageObtainMessage2);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i2);
                mtkSuppSrvRequestObtain.mParcel.writeString(str);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i3);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i4);
                mtkImsPhone.setCallForwardingOption(i, i2, str, i4, i3, obtainMessage(EVENT_IMS_UT_DONE, mtkSuppSrvRequestObtain));
                return;
            }
            if (isValidCommandInterfaceCFAction(i) && isValidCommandInterfaceCFReason(i2)) {
                if (i2 == 0) {
                    String telephonyProperty2 = TelephonyManager.getTelephonyProperty(getPhoneId(), "persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
                    int i6 = "enabled_cfu_mode_on".equals(telephonyProperty2) ? 1 : "enabled_cfu_mode_off".equals(telephonyProperty2) ? 2 : 0;
                    setSystemProperty("persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
                    messageObtainMessage = obtainMessage(12, isCfEnable(i) ? 1 : 0, i6, new Cfu(str, messageObtainMessage2, i4));
                } else {
                    messageObtainMessage = messageObtainMessage2;
                }
                if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                    if (isInCSCall() && getPhoneType() == 2) {
                        sendErrorResponse(messageObtainMessage2, CommandException.Error.GENERIC_FAILURE);
                        return;
                    } else {
                        this.mMtkSSReqDecisionMaker.setCallForward(i, i2, i4, str, i3, messageObtainMessage);
                        return;
                    }
                }
                if (getCsFallbackStatus() == 1) {
                    setCsFallbackStatus(0);
                }
                if (isNotSupportUtToCS()) {
                    sendErrorResponse(messageObtainMessage2, CommandException.Error.OPERATION_NOT_ALLOWED);
                    return;
                } else if ((isDuringVoLteCall() || isDuringImsEccCall()) && messageObtainMessage2 != null) {
                    sendErrorResponse(messageObtainMessage2, CommandException.Error.GENERIC_FAILURE);
                    return;
                } else {
                    this.mCi.setCallForward(i, i2, i4, str, i3, messageObtainMessage);
                    return;
                }
            }
            return;
        }
        loge("setCallForwardingOption: not possible in CDMA");
        sendErrorResponse(messageObtainMessage2, CommandException.Error.GENERIC_FAILURE);
    }

    private static class CfuEx {
        final Message mOnComplete;
        final String mSetCfNumber;
        final long[] mSetTimeSlot;

        CfuEx(String str, long[] jArr, Message message) {
            this.mSetCfNumber = str;
            this.mSetTimeSlot = jArr;
            this.mOnComplete = message;
        }
    }

    public void saveTimeSlot(long[] jArr) {
        String str = CFU_TIME_SLOT + this.mPhoneId;
        String str2 = "";
        if (jArr != null && jArr.length == 2) {
            str2 = Long.toString(jArr[0]) + "," + Long.toString(jArr[1]);
        }
        SystemProperties.set(str, str2);
        Rlog.d(LOG_TAG, "timeSlotString = " + str2);
    }

    public long[] getTimeSlot() {
        long[] jArr;
        String str = SystemProperties.get(CFU_TIME_SLOT + this.mPhoneId, "");
        if (str != null && !str.equals("")) {
            String[] strArrSplit = str.split(",");
            if (strArrSplit.length == 2) {
                jArr = new long[2];
                for (int i = 0; i < 2; i++) {
                    jArr[i] = Long.parseLong(strArrSplit[i]);
                    Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                    calendar.setTimeInMillis(jArr[i]);
                    int i2 = calendar.get(11);
                    int i3 = calendar.get(12);
                    Calendar calendar2 = Calendar.getInstance(TimeZone.getDefault());
                    calendar2.set(11, i2);
                    calendar2.set(12, i3);
                    jArr[i] = calendar2.getTimeInMillis();
                }
            }
        } else {
            jArr = null;
        }
        Rlog.d(LOG_TAG, "timeSlot = " + Arrays.toString(jArr));
        return jArr;
    }

    public void getCallForwardInTimeSlot(int i, Message message) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "getCallForwardInTimeSlot enter, CFReason:" + i);
                if (imsPhone != null && (imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled())) {
                    ((MtkImsPhone) imsPhone).getCallForwardInTimeSlot(i, message);
                    return;
                } else {
                    if (i == 0) {
                        Rlog.d(LOG_TAG, "requesting call forwarding in time slot query.");
                        this.mMtkCi.queryCallForwardInTimeSlotStatus(i, 0, obtainMessage(109, message));
                        return;
                    }
                    return;
                }
            }
            if (getCsFallbackStatus() == 0 && imsPhone != null && ((imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled()) && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                ((MtkImsPhone) imsPhone).getCallForwardInTimeSlot(i, message);
                return;
            }
            if (i != 0) {
                if (message != null) {
                    sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                    return;
                }
                return;
            }
            Rlog.d(LOG_TAG, "requesting call forwarding in time slot query.");
            Message messageObtainMessage = obtainMessage(109, message);
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                setSystemProperty("persist.vendor.radio.cfu.mode", "disabled_cfu_mode");
                this.mMtkSSReqDecisionMaker.queryCallForwardInTimeSlotStatus(i, 1, messageObtainMessage);
                return;
            } else {
                sendErrorResponse(message, CommandException.Error.REQUEST_NOT_SUPPORTED);
                return;
            }
        }
        loge("method getCallForwardInTimeSlot is NOT supported in CDMA!");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    public void setCallForwardInTimeSlot(int i, int i2, String str, int i3, long[] jArr, Message message) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "setCallForwardInTimeSlot enter, CFReason:" + i2 + ", CFAction:" + i + ", dialingNumber:" + str + ", timerSeconds:" + i3);
                if (imsPhone != null && (imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled())) {
                    ((MtkImsPhone) imsPhone).setCallForwardInTimeSlot(i, i2, str, i3, jArr, message);
                    return;
                } else {
                    if (isValidCommandInterfaceCFAction(i) && i2 == 0) {
                        this.mMtkCi.setCallForwardInTimeSlot(i, i2, 1, str, i3, jArr, obtainMessage(110, isCfEnable(i) ? 1 : 0, 0, new CfuEx(str, jArr, message)));
                        return;
                    }
                    return;
                }
            }
            if (getCsFallbackStatus() == 0 && isOp(MtkOperatorUtils.OPID.OP01) && imsPhone != null && imsPhone.getServiceState().getState() == 0 && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(17, message);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i2);
                mtkSuppSrvRequestObtain.mParcel.writeString(str);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i3);
                ((MtkImsPhone) imsPhone).setCallForwardInTimeSlot(i, i2, str, i3, jArr, obtainMessage(EVENT_IMS_UT_DONE, mtkSuppSrvRequestObtain));
                return;
            }
            if (!isValidCommandInterfaceCFAction(i) || i2 != 0) {
                sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                return;
            }
            Message messageObtainMessage = obtainMessage(110, isCfEnable(i) ? 1 : 0, 0, new CfuEx(str, jArr, message));
            if (getCsFallbackStatus() != 0 || !isGsmUtSupport()) {
                sendErrorResponse(message, CommandException.Error.REQUEST_NOT_SUPPORTED);
                return;
            } else {
                this.mMtkSSReqDecisionMaker.setCallForwardInTimeSlot(i, i2, 1, str, i3, jArr, messageObtainMessage);
                return;
            }
        }
        loge("method setCallForwardInTimeSlot is NOT supported in CDMA!");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    private void handleCfuInTimeSlotQueryResult(MtkCallForwardInfo[] mtkCallForwardInfoArr) {
        if (((IccRecords) this.mIccRecords.get()) != null) {
            if (mtkCallForwardInfoArr == null || mtkCallForwardInfoArr.length == 0) {
                setVoiceCallForwardingFlag(1, false, null);
                return;
            }
            int length = mtkCallForwardInfoArr.length;
            for (int i = 0; i < length; i++) {
                if ((mtkCallForwardInfoArr[i].serviceClass & 1) != 0) {
                    setVoiceCallForwardingFlag(1, mtkCallForwardInfoArr[i].status == 1, mtkCallForwardInfoArr[i].number);
                    int i2 = mtkCallForwardInfoArr[i].status;
                    saveTimeSlot(mtkCallForwardInfoArr[i].timeSlot);
                    return;
                }
            }
        }
    }

    public int[] getSavedClirSetting() {
        int i;
        int i2 = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("clir_key" + getPhoneId(), -1);
        int i3 = 4;
        if (i2 != 0 && i2 != -1) {
            if (i2 == 1) {
                i3 = 3;
                i = 1;
            } else {
                i = 2;
            }
        } else {
            i = 0;
        }
        int[] iArr = {i, i3};
        Rlog.i(LOG_TAG, "getClirResult: " + i);
        Rlog.i(LOG_TAG, "presentationMode: " + i3);
        return iArr;
    }

    public void getOutgoingCallerIdDisplay(Message message) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper != null) {
            suppServQueueHelper.getOutgoingCallerIdDisplay(message, getPhoneId());
        } else {
            Rlog.d(LOG_TAG, "ssQueueHelper not exist, getOutgoingCallerIdDisplay");
            getOutgoingCallerIdDisplayInternal(message);
        }
    }

    public void getOutgoingCallerIdDisplayInternal(Message message) {
        if (isPhoneTypeGsm() || isGsmSsPrefer()) {
            Phone phone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "getOutgoingCallerIdDisplay enter");
                Message messageObtainMessage = obtainMessage(EVENT_GET_CLIR_COMPLETE, message);
                if (phone != null && phone.getServiceState().getState() == 0) {
                    phone.getOutgoingCallerIdDisplay(messageObtainMessage);
                    return;
                } else {
                    this.mCi.getCLIR(messageObtainMessage);
                    return;
                }
            }
            if (getCsFallbackStatus() == 0 && phone != null && ((phone.getServiceState().getState() == 0 || phone.isUtEnabled()) && (phone.isVolteEnabled() || (phone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                if (isOpNotSupportCallIdentity()) {
                    sendErrorResponse(message, CommandException.Error.REQUEST_NOT_SUPPORTED);
                    return;
                }
                if (isOpTbClir()) {
                    if (message != null) {
                        AsyncResult.forMessage(message, getSavedClirSetting(), (Throwable) null);
                        message.sendToTarget();
                        return;
                    }
                    return;
                }
                phone.getOutgoingCallerIdDisplay(obtainMessage(EVENT_IMS_UT_DONE, MtkSuppSrvRequest.obtain(4, message)));
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                if (isOpTbClir()) {
                    if (message != null) {
                        AsyncResult.forMessage(message, getSavedClirSetting(), (Throwable) null);
                        message.sendToTarget();
                        return;
                    }
                    return;
                }
                this.mMtkSSReqDecisionMaker.getCLIR(message);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            if (isNotSupportUtToCS()) {
                sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                return;
            } else if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                return;
            } else {
                this.mCi.getCLIR(message);
                return;
            }
        }
        loge("getOutgoingCallerIdDisplay: not possible in CDMA");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    public void setOutgoingCallerIdDisplay(int i, Message message) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper != null) {
            suppServQueueHelper.setOutgoingCallerIdDisplay(i, message, getPhoneId());
        } else {
            Rlog.d(LOG_TAG, "ssQueueHelper not exist, setOutgoingCallerIdDisplay");
            setOutgoingCallerIdDisplayInternal(i, message);
        }
    }

    public void setOutgoingCallerIdDisplayInternal(int i, Message message) {
        if (isPhoneTypeGsm() || isGsmSsPrefer()) {
            Phone phone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "setOutgoingCallerIdDisplay enter, CLIRmode:" + i);
                Message messageObtainMessage = obtainMessage(18, i, 0, message);
                if (phone != null && phone.getServiceState().getState() == 0) {
                    phone.setOutgoingCallerIdDisplay(i, messageObtainMessage);
                    return;
                } else {
                    this.mCi.setCLIR(i, messageObtainMessage);
                    return;
                }
            }
            if (getCsFallbackStatus() == 0 && phone != null && ((phone.getServiceState().getState() == 0 || phone.isUtEnabled()) && (phone.isVolteEnabled() || (phone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                if (isOpNotSupportCallIdentity()) {
                    sendErrorResponse(message, CommandException.Error.REQUEST_NOT_SUPPORTED);
                    return;
                }
                if (isOpTbClir()) {
                    if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                        return;
                    } else {
                        this.mCi.setCLIR(i, obtainMessage(18, i, 0, message));
                        return;
                    }
                }
                MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(3, message);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i);
                phone.setOutgoingCallerIdDisplay(i, obtainMessage(EVENT_IMS_UT_DONE, mtkSuppSrvRequestObtain));
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                if (isOpTbClir()) {
                    if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                        return;
                    } else {
                        this.mCi.setCLIR(i, obtainMessage(18, i, 0, message));
                        return;
                    }
                }
                this.mMtkSSReqDecisionMaker.setCLIR(i, obtainMessage(18, i, 0, message));
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            if (isNotSupportUtToCS()) {
                sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                return;
            } else if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                return;
            } else {
                this.mCi.setCLIR(i, obtainMessage(18, i, 0, message));
                return;
            }
        }
        loge("setOutgoingCallerIdDisplay: not possible in CDMA");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    private void initTbcwMode() {
        if (this.mTbcwMode != 0) {
            Rlog.d(LOG_TAG, "initTbcwMode, mTbcwMode is not UNKNOWN, no need to init");
            return;
        }
        IccCard iccCard = getIccCard();
        String iccCardType = iccCard.getIccCardType();
        if (iccCard == null || !iccCard.hasIccCard() || iccCardType == null || iccCardType.equals("")) {
            Rlog.d(LOG_TAG, "initTbcwMode, IccCard is not ready. mTbcwMode ramains UNKNOWN");
            return;
        }
        if (isOpTbcwWithCS()) {
            setTbcwMode(3);
            setTbcwToEnabledOnIfDisabled();
        } else if (!isUsimCard()) {
            setTbcwMode(2);
            setSSPropertyThroughHidl(getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw");
        }
        Rlog.d(LOG_TAG, "initTbcwMode: " + this.mTbcwMode);
    }

    public int getTbcwMode() {
        initTbcwMode();
        return this.mTbcwMode;
    }

    public void setTbcwMode(int i) {
        Rlog.d(LOG_TAG, "Set tbcwmode: " + i + ", phoneId: " + getPhoneId());
        this.mTbcwMode = i;
    }

    public void setTbcwToEnabledOnIfDisabled() {
        if ("disabled_tbcw".equals(TelephonyManager.getTelephonyProperty(getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw"))) {
            setSSPropertyThroughHidl(getPhoneId(), "persist.vendor.radio.terminal-based.cw", "enabled_tbcw_on");
        }
    }

    public void getTerminalBasedCallWaiting(Message message) {
        String telephonyProperty = TelephonyManager.getTelephonyProperty(getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw");
        Rlog.d(LOG_TAG, "getTerminalBasedCallWaiting(): tbcwMode = " + telephonyProperty + ", onComplete = " + message);
        if ("enabled_tbcw_on".equals(telephonyProperty)) {
            if (message != null) {
                AsyncResult.forMessage(message, new int[]{1, 1}, (Throwable) null);
                message.sendToTarget();
                return;
            }
            return;
        }
        if ("enabled_tbcw_off".equals(telephonyProperty)) {
            if (message != null) {
                AsyncResult.forMessage(message, new int[]{0, 0}, (Throwable) null);
                message.sendToTarget();
                return;
            }
            return;
        }
        Rlog.e(LOG_TAG, "getTerminalBasedCallWaiting(): ERROR: tbcwMode = " + telephonyProperty);
    }

    public void getCallWaiting(Message message) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper != null) {
            suppServQueueHelper.getCallWaiting(message, getPhoneId());
        } else {
            Rlog.d(LOG_TAG, "ssQueueHelper not exist, getCallWaiting");
            getCallWaitingInternal(message);
        }
    }

    public void getCallWaitingInternal(Message message) {
        if (isPhoneTypeGsm() || isGsmSsPrefer()) {
            Phone phone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "getCallWaiting enter");
                if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                    phone.getCallWaiting(message);
                    return;
                } else {
                    this.mCi.queryCallWaiting(0, message);
                    return;
                }
            }
            if (!isOpNwCW()) {
                if (this.mTbcwMode == 0) {
                    initTbcwMode();
                }
                Rlog.d(LOG_TAG, "getCallWaiting(): mTbcwMode = " + this.mTbcwMode + ", onComplete = " + message);
                switch (this.mTbcwMode) {
                    case 1:
                        getTerminalBasedCallWaiting(message);
                        break;
                    case 2:
                        if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                            sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                        } else if (isNotSupportUtToCS()) {
                            sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                        } else {
                            Rlog.d(LOG_TAG, "mCi.queryCallForwardStatus.");
                            this.mCi.queryCallWaiting(0, message);
                        }
                        break;
                    case 3:
                        if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                            sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                        } else {
                            this.mCi.queryCallWaiting(0, obtainMessage(EVENT_GET_CALL_WAITING_DONE, message));
                        }
                        break;
                }
            }
            if (getCsFallbackStatus() == 0 && phone != null && ((phone.getServiceState().getState() == 0 || phone.isUtEnabled()) && (phone.isVolteEnabled() || (phone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                if (isOpNwCW()) {
                    Rlog.d(LOG_TAG, "isOpNwCW(), getCallWaiting() by Ut interface");
                    phone.getCallWaiting(obtainMessage(EVENT_IMS_UT_DONE, MtkSuppSrvRequest.obtain(14, message)));
                    return;
                } else {
                    Rlog.d(LOG_TAG, "isOpTbCW(), getTerminalBasedCallWaiting");
                    setTbcwMode(1);
                    setTbcwToEnabledOnIfDisabled();
                    getTerminalBasedCallWaiting(message);
                    return;
                }
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                Rlog.d(LOG_TAG, "mMtkSSReqDecisionMaker.queryCallWaiting");
                this.mMtkSSReqDecisionMaker.queryCallWaiting(0, message);
                return;
            } else if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                return;
            } else if (isNotSupportUtToCS()) {
                sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                return;
            } else {
                Rlog.d(LOG_TAG, "mCi.queryCallForwardStatus.");
                this.mCi.queryCallWaiting(0, message);
                return;
            }
        }
        this.mCi.queryCallWaiting(1, message);
    }

    public void setTerminalBasedCallWaiting(boolean z, Message message) {
        String telephonyProperty = TelephonyManager.getTelephonyProperty(getPhoneId(), "persist.vendor.radio.terminal-based.cw", "disabled_tbcw");
        Rlog.d(LOG_TAG, "setTerminalBasedCallWaiting(): tbcwMode = " + telephonyProperty + ", enable = " + z);
        if ("enabled_tbcw_on".equals(telephonyProperty)) {
            if (!z) {
                setSSPropertyThroughHidl(getPhoneId(), "persist.vendor.radio.terminal-based.cw", "enabled_tbcw_off");
            }
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, (Throwable) null);
                message.sendToTarget();
                return;
            }
            return;
        }
        if ("enabled_tbcw_off".equals(telephonyProperty)) {
            if (z) {
                setSSPropertyThroughHidl(getPhoneId(), "persist.vendor.radio.terminal-based.cw", "enabled_tbcw_on");
            }
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, (Throwable) null);
                message.sendToTarget();
                return;
            }
            return;
        }
        Rlog.e(LOG_TAG, "setTerminalBasedCallWaiting(): ERROR: tbcwMode = " + telephonyProperty);
    }

    public void setSSPropertyThroughHidl(int i, String str, String str2) {
        String[] strArrSplit;
        String str3 = "";
        String str4 = SystemProperties.get(str);
        if (str2 == null) {
            str2 = "";
        }
        if (str4 != null) {
            strArrSplit = str4.split(",");
        } else {
            strArrSplit = null;
        }
        if (!SubscriptionManager.isValidPhoneId(i)) {
            Rlog.d(LOG_TAG, "setSSPropertyThroughHidl: invalid phoneId=" + i + " property=" + str + " value: " + str2 + " prop=" + str4);
            return;
        }
        for (int i2 = 0; i2 < i; i2++) {
            String str5 = "";
            if (strArrSplit != null && i2 < strArrSplit.length) {
                str5 = strArrSplit[i2];
            }
            str3 = str3 + str5 + ",";
        }
        String str6 = str3 + str2;
        if (strArrSplit != null) {
            for (int i3 = i + 1; i3 < strArrSplit.length; i3++) {
                str6 = str6 + "," + strArrSplit[i3];
            }
        }
        if (str6.length() > 91) {
            Rlog.d(LOG_TAG, "setSSPropertyThroughHidl: property too long phoneId=" + i + " property=" + str + " value: " + str2 + " propVal=" + str6);
            return;
        }
        setSuppServProperty(str, str6);
    }

    public void setCallWaiting(boolean z, Message message) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper != null) {
            suppServQueueHelper.setCallWaiting(z, message, getPhoneId());
        } else {
            Rlog.d(LOG_TAG, "ssQueueHelper not exist, setCallWaiting");
            setCallWaitingInternal(z, message);
        }
    }

    public void setCallWaitingInternal(boolean z, Message message) {
        if (isPhoneTypeGsm() || isGsmSsPrefer()) {
            Phone phone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "setCallWaiting enter, enable:" + z);
                if (phone != null && (phone.getServiceState().getState() == 0 || phone.isUtEnabled())) {
                    phone.setCallWaiting(z, message);
                    return;
                } else {
                    this.mCi.setCallWaiting(z, 1, message);
                    return;
                }
            }
            if (!isOpNwCW()) {
                if (this.mTbcwMode == 0) {
                    initTbcwMode();
                }
                Rlog.d(LOG_TAG, "setCallWaiting(): mTbcwMode = " + this.mTbcwMode + ", onComplete = " + message);
                switch (this.mTbcwMode) {
                    case 1:
                        setTerminalBasedCallWaiting(z, message);
                        break;
                    case 2:
                        if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                            sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                        } else if (isNotSupportUtToCS()) {
                            sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                        } else {
                            this.mCi.setCallWaiting(z, 1, message);
                        }
                        break;
                    case 3:
                        if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                            sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                        } else {
                            this.mCi.setCallWaiting(z, 1, obtainMessage(EVENT_SET_CALL_WAITING_DONE, z ? 1 : 0, 0, message));
                        }
                        break;
                }
            }
            if (getCsFallbackStatus() == 0 && phone != null && ((phone.getServiceState().getState() == 0 || phone.isUtEnabled()) && (phone.isVolteEnabled() || (phone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                if (isOpNwCW()) {
                    MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(13, message);
                    mtkSuppSrvRequestObtain.mParcel.writeInt(z ? 1 : 0);
                    phone.setCallWaiting(z, obtainMessage(EVENT_IMS_UT_DONE, mtkSuppSrvRequestObtain));
                    return;
                } else {
                    Rlog.d(LOG_TAG, "isOpTbCW(), setTerminalBasedCallWaiting(): IMS in service");
                    setTbcwMode(1);
                    setTbcwToEnabledOnIfDisabled();
                    setTerminalBasedCallWaiting(z, message);
                    return;
                }
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                Rlog.d(LOG_TAG, "mMtkSSReqDecisionMaker.setCallWaiting");
                this.mMtkSSReqDecisionMaker.setCallWaiting(z, 1, message);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                return;
            } else if (isNotSupportUtToCS()) {
                sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                return;
            } else {
                this.mCi.setCallWaiting(z, 1, message);
                return;
            }
        }
        loge("method setCallWaiting is NOT supported in CDMA!");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    public void getCallBarring(String str, String str2, Message message, int i) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper != null) {
            suppServQueueHelper.getCallBarring(str, str2, i, message, getPhoneId());
        } else {
            Rlog.d(LOG_TAG, "ssQueueHelper not exist, getCallBarringInternal");
            getCallBarringInternal(str, str2, message, i);
        }
    }

    public void getCallBarring(String str, String str2, Message message) {
        getCallBarring(str, str2, message, 1);
    }

    public void getCallBarringInternal(String str, String str2, Message message, int i) {
        if (isPhoneTypeGsm() || isGsmSsPrefer()) {
            ImsPhone imsPhone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "getCallBarringInternal enter, facility:" + str + ", serviceClass:" + i + ", password:" + str2);
                Message messageObtainMessage = obtainMessage(EVENT_GET_CALL_BARRING_COMPLETE, message);
                if (imsPhone != null && (imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled())) {
                    imsPhone.getCallBarring(str, str2, messageObtainMessage, i);
                    return;
                } else {
                    this.mCi.queryFacilityLock(str, str2, i, messageObtainMessage);
                    return;
                }
            }
            if (getCsFallbackStatus() == 0 && imsPhone != null && ((imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled()) && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                if (isOpNotSupportOCB(str)) {
                    sendErrorResponse(message, CommandException.Error.REQUEST_NOT_SUPPORTED);
                    return;
                }
                MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(10, message);
                mtkSuppSrvRequestObtain.mParcel.writeString(str);
                mtkSuppSrvRequestObtain.mParcel.writeString(str2);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i);
                imsPhone.getCallBarring(str, str2, obtainMessage(EVENT_IMS_UT_DONE, mtkSuppSrvRequestObtain), i);
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.queryFacilityLock(str, str2, i, message);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            if (isNotSupportUtToCS()) {
                sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                return;
            }
            if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                return;
            }
            CommandException commandExceptionCheckUiccApplicationForCB = checkUiccApplicationForCB();
            if (commandExceptionCheckUiccApplicationForCB != null && message != null) {
                sendErrorResponse(message, commandExceptionCheckUiccApplicationForCB.getCommandError());
                return;
            } else {
                this.mCi.queryFacilityLockForApp(str, str2, i, ((UiccCardApplication) this.mUiccApplication.get()).getAid(), message);
                return;
            }
        }
        loge("method getFacilityLock is NOT supported in CDMA!");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    public void setCallBarring(String str, boolean z, String str2, Message message, int i) {
        MtkSuppServQueueHelper suppServQueueHelper = MtkSuppServManager.getSuppServQueueHelper();
        if (suppServQueueHelper != null) {
            suppServQueueHelper.setCallBarring(str, z, str2, i, message, getPhoneId());
        } else {
            Rlog.d(LOG_TAG, "ssQueueHelper not exist, setCallBarring");
            setCallBarringInternal(str, z, str2, message, i);
        }
    }

    public void setCallBarring(String str, boolean z, String str2, Message message) {
        setCallBarring(str, z, str2, message, 1);
    }

    public void setCallBarringInternal(String str, boolean z, String str2, Message message, int i) {
        if (isPhoneTypeGsm() || isGsmSsPrefer()) {
            ImsPhone imsPhone = this.mImsPhone;
            if (supportMdAutoSetupIms()) {
                Rlog.d(LOG_TAG, "setCallBarring enter, facility:" + str + ", serviceClass:" + i + ", password:" + str2 + ", lockState:" + z);
                Message messageObtainMessage = obtainMessage(EVENT_SET_CALL_BARRING_COMPLETE, message);
                if (imsPhone != null && (imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled())) {
                    imsPhone.setCallBarring(str, z, str2, messageObtainMessage, i);
                    return;
                } else {
                    this.mCi.setFacilityLock(str, z, str2, i, messageObtainMessage);
                    return;
                }
            }
            if (getCsFallbackStatus() == 0 && imsPhone != null && ((imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled()) && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                if (isOpNotSupportOCB(str)) {
                    sendErrorResponse(message, CommandException.Error.REQUEST_NOT_SUPPORTED);
                    return;
                }
                MtkSuppSrvRequest mtkSuppSrvRequestObtain = MtkSuppSrvRequest.obtain(9, message);
                mtkSuppSrvRequestObtain.mParcel.writeString(str);
                mtkSuppSrvRequestObtain.mParcel.writeInt(z ? 1 : 0);
                mtkSuppSrvRequestObtain.mParcel.writeString(str2);
                mtkSuppSrvRequestObtain.mParcel.writeInt(i);
                imsPhone.setCallBarring(str, z, str2, obtainMessage(EVENT_IMS_UT_DONE, mtkSuppSrvRequestObtain), i);
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                this.mMtkSSReqDecisionMaker.setFacilityLock(str, z, str2, i, message);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            if (isNotSupportUtToCS()) {
                sendErrorResponse(message, CommandException.Error.OPERATION_NOT_ALLOWED);
                return;
            }
            if ((isDuringVoLteCall() || isDuringImsEccCall()) && message != null) {
                sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
                return;
            }
            CommandException commandExceptionCheckUiccApplicationForCB = checkUiccApplicationForCB();
            if (commandExceptionCheckUiccApplicationForCB != null && message != null) {
                sendErrorResponse(message, commandExceptionCheckUiccApplicationForCB.getCommandError());
                return;
            } else {
                this.mCi.setFacilityLockForApp(str, z, str2, i, ((UiccCardApplication) this.mUiccApplication.get()).getAid(), message);
                return;
            }
        }
        loge("method setFacilityLock is NOT supported in CDMA!");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    public CommandException checkUiccApplicationForCB() {
        if (this.mUiccApplication.get() == null) {
            Rlog.d(LOG_TAG, "checkUiccApplicationForCB: mUiccApplication.get() == null");
            if (!this.mCi.getRadioState().isAvailable() || !this.mCi.getRadioState().isOn()) {
                Rlog.d(LOG_TAG, "checkUiccApplicationForCB: radio not available");
                return new CommandException(CommandException.Error.RADIO_NOT_AVAILABLE);
            }
            return new CommandException(CommandException.Error.GENERIC_FAILURE);
        }
        return null;
    }

    public void changeCallBarringPassword(String str, String str2, String str3, Message message) {
        if (isPhoneTypeGsm()) {
            if (isDuringImsCall()) {
                if (message != null) {
                    AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                    message.sendToTarget();
                    return;
                }
                return;
            }
            this.mCi.changeBarringPassword(str, str2, str3, message);
            return;
        }
        loge("method changeBarringPassword is NOT supported in CDMA!");
        sendErrorResponse(message, CommandException.Error.GENERIC_FAILURE);
    }

    public MtkSSRequestDecisionMaker getMtkSSRequestDecisionMaker() {
        return this.mMtkSSReqDecisionMaker;
    }

    public boolean isDuringImsCall() {
        if (this.mImsPhone != null) {
            if (this.mImsPhone.getForegroundCall().getState().isAlive() || this.mImsPhone.getBackgroundCall().getState().isAlive() || this.mImsPhone.getRingingCall().getState().isAlive()) {
                Rlog.d(LOG_TAG, "During IMS call.");
                return true;
            }
        }
        return false;
    }

    public boolean isDuringVoLteCall() {
        boolean z = false;
        boolean z2 = this.mImsPhone != null && this.mImsPhone.isVolteEnabled();
        if (isDuringImsCall() && z2) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isDuringVoLteCall: " + z);
        return z;
    }

    public boolean isDuringImsEccCall() {
        boolean z = this.mImsPhone != null && this.mImsPhone.isInEmergencyCall();
        Rlog.d(LOG_TAG, "isInImsEccCall: " + z);
        return z;
    }

    private void handleImsUtDone(Message message) {
        AsyncResult asyncResult = (AsyncResult) message.obj;
        if (asyncResult == null) {
            Rlog.e(LOG_TAG, "EVENT_IMS_UT_DONE: Error AsyncResult null!");
            return;
        }
        MtkSuppSrvRequest mtkSuppSrvRequest = (MtkSuppSrvRequest) asyncResult.userObj;
        if (mtkSuppSrvRequest == null) {
            Rlog.e(LOG_TAG, "EVENT_IMS_UT_DONE: Error SuppSrvRequest null!");
            return;
        }
        CommandException commandException = null;
        if (asyncResult.exception != null && (asyncResult.exception instanceof CommandException)) {
            commandException = (CommandException) asyncResult.exception;
        }
        if (commandException != null && commandException.getCommandError() == CommandException.Error.OPERATION_NOT_ALLOWED) {
            setCsFallbackStatus(2);
            if (isNotSupportUtToCS()) {
                Rlog.d(LOG_TAG, "UT_XCAP_403_FORBIDDEN.");
                asyncResult.exception = new CommandException(CommandException.Error.OPERATION_NOT_ALLOWED);
                Message resultCallback = mtkSuppSrvRequest.getResultCallback();
                if (resultCallback != null) {
                    AsyncResult.forMessage(resultCallback, asyncResult.result, asyncResult.exception);
                    resultCallback.sendToTarget();
                }
                mtkSuppSrvRequest.mParcel.recycle();
                return;
            }
            Rlog.d(LOG_TAG, "Csfallback next_reboot.");
            sendMessage(obtainMessage(2001, mtkSuppSrvRequest));
            return;
        }
        if (commandException != null && commandException.getCommandError() == CommandException.Error.NO_NETWORK_FOUND) {
            if (isNotSupportUtToCS()) {
                Rlog.d(LOG_TAG, "CommandException.Error.UT_UNKNOWN_HOST.");
                asyncResult.exception = new CommandException(CommandException.Error.OPERATION_NOT_ALLOWED);
                Message resultCallback2 = mtkSuppSrvRequest.getResultCallback();
                if (resultCallback2 != null) {
                    AsyncResult.forMessage(resultCallback2, asyncResult.result, asyncResult.exception);
                    resultCallback2.sendToTarget();
                }
                mtkSuppSrvRequest.mParcel.recycle();
                return;
            }
            Rlog.d(LOG_TAG, "Csfallback once.");
            setCsFallbackStatus(1);
            sendMessage(obtainMessage(2001, mtkSuppSrvRequest));
            return;
        }
        if (commandException != null && commandException.getCommandError() == CommandException.Error.NO_SUCH_ELEMENT) {
            if (isOpTransferXcap404() && (mtkSuppSrvRequest.getRequestCode() == 10 || mtkSuppSrvRequest.getRequestCode() == 9)) {
                Rlog.d(LOG_TAG, "GSMPhone get UT_XCAP_404_NOT_FOUND.");
            } else {
                asyncResult.exception = new CommandException(CommandException.Error.GENERIC_FAILURE);
            }
        } else if (commandException != null && commandException.getCommandError() == CommandException.Error.OEM_ERROR_1) {
            if (!isEnableXcapHttpResponse409()) {
                Rlog.d(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT, return GENERIC_FAILURE");
                asyncResult.exception = new CommandException(CommandException.Error.GENERIC_FAILURE);
            } else {
                Rlog.d(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT.");
            }
        }
        Message resultCallback3 = mtkSuppSrvRequest.getResultCallback();
        if (resultCallback3 != null) {
            AsyncResult.forMessage(resultCallback3, asyncResult.result, asyncResult.exception);
            resultCallback3.sendToTarget();
        }
        mtkSuppSrvRequest.mParcel.recycle();
    }

    private void handleImsUtCsfb(Message message) {
        MtkSuppSrvRequest mtkSuppSrvRequest = (MtkSuppSrvRequest) message.obj;
        if (mtkSuppSrvRequest == null) {
            Rlog.e(LOG_TAG, "handleImsUtCsfb: Error MtkSuppSrvRequest null!");
            return;
        }
        if (isDuringVoLteCall() || isDuringImsEccCall()) {
            Message resultCallback = mtkSuppSrvRequest.getResultCallback();
            if (resultCallback != null) {
                AsyncResult.forMessage(resultCallback, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                resultCallback.sendToTarget();
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            mtkSuppSrvRequest.setResultCallback(null);
            mtkSuppSrvRequest.mParcel.recycle();
            return;
        }
        int requestCode = mtkSuppSrvRequest.getRequestCode();
        mtkSuppSrvRequest.mParcel.setDataPosition(0);
        switch (requestCode) {
            case 3:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CLIR");
                setOutgoingCallerIdDisplayInternal(mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.getResultCallback());
                break;
            case 4:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CLIR");
                getOutgoingCallerIdDisplayInternal(mtkSuppSrvRequest.getResultCallback());
                break;
            default:
                switch (requestCode) {
                    case 9:
                        Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CB");
                        setCallBarringInternal(mtkSuppSrvRequest.mParcel.readString(), mtkSuppSrvRequest.mParcel.readInt() != 0, mtkSuppSrvRequest.mParcel.readString(), mtkSuppSrvRequest.getResultCallback(), mtkSuppSrvRequest.mParcel.readInt());
                        break;
                    case 10:
                        Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CB");
                        getCallBarringInternal(mtkSuppSrvRequest.mParcel.readString(), mtkSuppSrvRequest.mParcel.readString(), mtkSuppSrvRequest.getResultCallback(), mtkSuppSrvRequest.mParcel.readInt());
                        break;
                    case 11:
                        Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CF");
                        setCallForwardingOptionInternal(mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.mParcel.readString(), mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.getResultCallback());
                        break;
                    case 12:
                        Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CF");
                        getCallForwardingOptionInternal(mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.mParcel.readInt(), mtkSuppSrvRequest.getResultCallback());
                        break;
                    case 13:
                        Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CW");
                        setCallWaitingInternal(mtkSuppSrvRequest.mParcel.readInt() != 0, mtkSuppSrvRequest.getResultCallback());
                        break;
                    case 14:
                        Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CW");
                        getCallWaitingInternal(mtkSuppSrvRequest.getResultCallback());
                        break;
                    case 15:
                        String string = mtkSuppSrvRequest.mParcel.readString();
                        Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_MMI_CODE: dialString = " + string);
                        try {
                            dial(string, new PhoneInternalInterface.DialArgs.Builder().build());
                        } catch (CallStateException e) {
                            Rlog.e(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_MMI_CODE: CallStateException!");
                            e.printStackTrace();
                        }
                        break;
                    default:
                        Rlog.e(LOG_TAG, "handleImsUtCsfb: invalid requestCode = " + requestCode);
                        break;
                }
                break;
        }
        mtkSuppSrvRequest.setResultCallback(null);
        mtkSuppSrvRequest.mParcel.recycle();
    }

    private void handleUssiCsfb(String str) {
        Rlog.d(LOG_TAG, "handleUssiCsfb: dialString=" + str);
        try {
            dial(str, new PhoneInternalInterface.DialArgs.Builder().build());
        } catch (CallStateException e) {
            Rlog.e(LOG_TAG, "handleUssiCsfb: CallStateException!");
            e.printStackTrace();
        }
    }

    public void onMMIDone(MmiCode mmiCode, Object obj) {
        logd("onMMIDone USSD Response:" + mmiCode + ", obj=" + obj);
        dumpPendingMmi();
        if (!this.mPendingMMIs.remove(mmiCode)) {
            if (isPhoneTypeGsm()) {
                if (!mmiCode.isUssdRequest() && !((MtkGsmMmiCode) mmiCode).isSsInfo()) {
                    return;
                }
            } else {
                return;
            }
        }
        ResultReceiver ussdCallbackReceiver = mmiCode.getUssdCallbackReceiver();
        if (ussdCallbackReceiver != null) {
            sendUssdResponse(mmiCode.getDialString(), mmiCode.getMessage(), mmiCode.getState() == MmiCode.State.COMPLETE ? 100 : -1, ussdCallbackReceiver);
        } else {
            this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult(obj, mmiCode, (Throwable) null));
        }
    }

    public void dumpPendingMmi() {
        int size = this.mPendingMMIs.size();
        if (size == 0) {
            Rlog.d(LOG_TAG, "dumpPendingMmi: none");
            return;
        }
        for (int i = 0; i < size; i++) {
            Rlog.d(LOG_TAG, "dumpPendingMmi: " + this.mPendingMMIs.get(i));
        }
    }

    protected void onNetworkInitiatedUssd(MmiCode mmiCode) {
        Rlog.d(LOG_TAG, "onNetworkInitiatedUssd: mMmiCompleteRegistrants.notifyRegistrants");
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmiCode, (Throwable) null));
    }

    protected void onIncomingUSSD(int i, String str) {
        if (!isPhoneTypeGsm()) {
            loge("onIncomingUSSD: not expected on GSM");
        }
        boolean z = i == 1;
        boolean z2 = i == 2;
        boolean z3 = i == 3;
        boolean z4 = i == 4 || i == 5;
        MtkGsmMmiCode mtkGsmMmiCode = null;
        Rlog.d(LOG_TAG, "USSD:mPendingMMIs= " + this.mPendingMMIs + " size=" + this.mPendingMMIs.size());
        int size = this.mPendingMMIs.size();
        int i2 = 0;
        while (true) {
            if (i2 >= size) {
                break;
            }
            Rlog.d(LOG_TAG, "i= " + i2 + " isPending=" + ((MtkGsmMmiCode) this.mPendingMMIs.get(i2)).isPendingUSSD());
            if (!((MtkGsmMmiCode) this.mPendingMMIs.get(i2)).isPendingUSSD()) {
                i2++;
            } else {
                mtkGsmMmiCode = (MtkGsmMmiCode) this.mPendingMMIs.get(i2);
                Rlog.d(LOG_TAG, "found = " + mtkGsmMmiCode);
                break;
            }
        }
        if (mtkGsmMmiCode != null) {
            Rlog.d(LOG_TAG, "setUserInitiatedMMI  TRUE");
            mtkGsmMmiCode.setUserInitiatedMMI(true);
            if (z2 && this.mIsNetworkInitiatedUssr) {
                Rlog.d(LOG_TAG, "onIncomingUSSD(): USSD_MODE_NW_RELEASE");
                mtkGsmMmiCode.onUssdRelease();
            } else if (z4) {
                mtkGsmMmiCode.onUssdFinishedError();
            } else if (z3) {
                mtkGsmMmiCode.onUssdStkHandling(str, z);
            } else {
                mtkGsmMmiCode.onUssdFinished(str, z);
            }
        } else {
            if (z) {
                Rlog.d(LOG_TAG, "The default value of UserInitiatedMMI is FALSE");
                this.mIsNetworkInitiatedUssr = true;
                Rlog.d(LOG_TAG, "onIncomingUSSD(): Network Initialized USSD Request");
            } else if (z3) {
                Rlog.d(LOG_TAG, "Do not return MMI object for ussd mode 3.");
                return;
            }
            if (!z4 && str != null) {
                onNetworkInitiatedUssd(MtkGsmMmiCode.newNetworkInitiatedUssd(str, z, this, (UiccCardApplication) this.mUiccApplication.get()));
            } else if (z4 && str != null) {
                onNetworkInitiatedUssd(MtkGsmMmiCode.newNetworkInitiatedUssdError(str, z, this, (UiccCardApplication) this.mUiccApplication.get()));
            }
        }
        if (z2 || z4) {
            this.mIsNetworkInitiatedUssr = false;
        }
    }

    public void setServiceClass(int i) {
        Rlog.d(LOG_TAG, "setServiceClass: " + i);
        SystemProperties.set(SS_SERVICE_CLASS_PROP, String.valueOf(i));
    }

    public boolean isGsmUtSupport() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isGsmUtSupport, ssConf is null, return false");
            return false;
        }
        if (!SystemProperties.get("persist.vendor.ims_support").equals("1") || !SystemProperties.get("persist.vendor.volte_support").equals("1") || !suppServConf.isGsmUtSupport(getOperatorNumeric()) || !isUsimCard()) {
            return false;
        }
        boolean z = this.mImsPhone != null && this.mImsPhone.isWifiCallingEnabled();
        boolean zIsWFCUtSupport = isWFCUtSupport();
        logd("in isGsmUtSupport isWfcEnable -->" + z + "isWfcUtSupport-->" + zIsWFCUtSupport);
        return !z || zIsWFCUtSupport;
    }

    public boolean isWFCUtSupport() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isWFCUtSupport, ssConf is null, return false");
            return false;
        }
        if (!SystemProperties.get("persist.vendor.mtk_ims_support").equals("1") || !SystemProperties.get("persist.vendor.mtk_wfc_support").equals("1") || suppServConf.isNotSupportWFCUt(getOperatorNumeric())) {
            return false;
        }
        return true;
    }

    private boolean isUsimCard() {
        if (isPhoneTypeGsm() && !isOp(MtkOperatorUtils.OPID.OP09)) {
            String iccCardType = PhoneFactory.getPhone(getPhoneId()).getIccCard().getIccCardType();
            boolean z = iccCardType != null && iccCardType.equals("USIM");
            Rlog.d(LOG_TAG, "isUsimCard: " + z + ", " + iccCardType);
            return z;
        }
        String[] strArrSplit = null;
        int slotIndex = SubscriptionManager.getSlotIndex(MtkSubscriptionManager.getSubIdUsingPhoneId(getPhoneId()));
        if (slotIndex < 0 || slotIndex >= PROPERTY_RIL_FULL_UICC_TYPE.length) {
            return false;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[slotIndex], "");
        if (!str.equals("") && str.length() > 0) {
            strArrSplit = str.split(",");
        }
        Rlog.d(LOG_TAG, "isUsimCard PhoneId = " + getPhoneId() + " cardType = " + Arrays.toString(strArrSplit));
        if (strArrSplit == null) {
            return false;
        }
        for (String str2 : strArrSplit) {
            if (str2.equals("USIM")) {
                return true;
            }
        }
        return false;
    }

    private boolean isOp(MtkOperatorUtils.OPID opid) {
        return MtkOperatorUtils.isOperator(getOperatorNumeric(), opid);
    }

    public boolean isOpNotSupportOCB(String str) {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isOpNotSupportOCB, ssConf is null, return false");
            return false;
        }
        if ((str.equals("AO") || str.equals("OI") || str.equals("OX")) && suppServConf.isNotSupportOCB(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isOpNotSupportOCB: " + z + ", facility=" + str);
        return z;
    }

    public boolean isOpTbcwWithCS() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isOpTbcwWithCS, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isNotSupportXcap(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isOpTbcwWithCS: " + z);
        return z;
    }

    public boolean isOpTbClir() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isOpTbClir, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isTbClir(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isOpTbClir: " + z);
        return z;
    }

    public boolean isOpNwCW() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isOpNwCW, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isImsNwCW(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isOpNwCW():" + z);
        return z;
    }

    public boolean isEnableXcapHttpResponse409() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isEnableXcapHttpResponse409, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isEnableXcapHttpResponse409(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isEnableXcapHttpResponse409: " + z);
        return z;
    }

    public boolean isOpTransferXcap404() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isOpTransferXcap404, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isTransferXcap404(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isOpTransferXcap404: " + z);
        return z;
    }

    public boolean isOpNotSupportCallIdentity() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isOpNotSupportCallIdentity, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isNotSupportCallIdentity(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isOpNotSupportCallIdentity: " + z);
        return z;
    }

    public boolean isOpReregisterForCF() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isOpReregisterForCF, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isReregisterForCF(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isOpReregisterForCF: " + z);
        return z;
    }

    private boolean isIccCardMncMccAvailable(int i) {
        IccRecords iccRecords = UiccController.getInstance().getIccRecords(i, 1);
        if (iccRecords != null) {
            String operatorNumeric = iccRecords.getOperatorNumeric();
            Rlog.d(LOG_TAG, "isIccCardMncMccAvailable(): mccMnc is " + operatorNumeric);
            if (operatorNumeric != null) {
                return true;
            }
            return false;
        }
        Rlog.d(LOG_TAG, "isIccCardMncMccAvailable(): false");
        return false;
    }

    public boolean isNotSupportUtToCS() {
        boolean z = false;
        if (((SystemProperties.getInt("persist.vendor.mtk_ct_volte_support", 0) != 0 && isOp(MtkOperatorUtils.OPID.OP09) && isUsimCard()) || isOp(MtkOperatorUtils.OPID.OP117)) && !getServiceState().getRoaming()) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isNotSupportUtToCS: " + z);
        return z;
    }

    private boolean supportMdAutoSetupIms() {
        if (SystemProperties.get("ro.vendor.md_auto_setup_ims").equals("1")) {
            return true;
        }
        return false;
    }

    private boolean isUtError(CommandException.Error error) {
        if (error == CommandException.Error.OEM_ERROR_1 || error == CommandException.Error.OEM_ERROR_3 || error == CommandException.Error.OEM_ERROR_4 || error == CommandException.Error.OEM_ERROR_6 || error == CommandException.Error.NO_NETWORK_FOUND) {
            return true;
        }
        return false;
    }

    public boolean isSupportSaveCFNumber() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isSupportSaveCFNumber, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isSupportSaveCFNumber(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isSupportSaveCFNumber: " + z);
        return z;
    }

    public void clearCFSharePreference(int i) {
        String str;
        switch (i) {
            case 1:
                str = "CFB_" + String.valueOf(this.mPhoneId);
                break;
            case 2:
                str = "CFNR_" + String.valueOf(this.mPhoneId);
                break;
            case 3:
                str = "CFNRC_" + String.valueOf(this.mPhoneId);
                break;
            default:
                Rlog.e(LOG_TAG, "No need to store cfreason: " + i);
                return;
        }
        Rlog.e(LOG_TAG, "Read to clear the key: " + str);
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editorEdit.remove(str);
        if (!editorEdit.commit()) {
            Rlog.e(LOG_TAG, "failed to commit the removal of CF preference: " + str);
            return;
        }
        Rlog.e(LOG_TAG, "Commit the removal of CF preference: " + str);
    }

    public boolean applyCFSharePreference(int i, String str) {
        String str2;
        switch (i) {
            case 1:
                str2 = "CFB_" + String.valueOf(this.mPhoneId);
                break;
            case 2:
                str2 = "CFNR_" + String.valueOf(this.mPhoneId);
                break;
            case 3:
                str2 = "CFNRC_" + String.valueOf(this.mPhoneId);
                break;
            default:
                Rlog.d(LOG_TAG, "No need to store cfreason: " + i);
                return false;
        }
        IccRecords iccRecords = (IccRecords) this.mIccRecords.get();
        if (iccRecords == null) {
            Rlog.d(LOG_TAG, "No iccRecords");
            return false;
        }
        String imsi = iccRecords.getIMSI();
        if (imsi == null || imsi.isEmpty()) {
            Rlog.d(LOG_TAG, "currentImsi is empty");
            return false;
        }
        if (str == null || str.isEmpty()) {
            Rlog.d(LOG_TAG, "setNumber is empty");
            return false;
        }
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        String str3 = imsi + ";" + str;
        if (str3 == null || str3.isEmpty()) {
            Rlog.e(LOG_TAG, "imsi or content are empty or null.");
            return false;
        }
        Rlog.e(LOG_TAG, "key: " + str2);
        Rlog.e(LOG_TAG, "content: " + str3);
        editorEdit.putString(str2, str3);
        editorEdit.apply();
        return true;
    }

    public String getCFPreviousDialNumber(int i) {
        String str;
        switch (i) {
            case 1:
                str = "CFB_" + String.valueOf(this.mPhoneId);
                break;
            case 2:
                str = "CFNR_" + String.valueOf(this.mPhoneId);
                break;
            case 3:
                str = "CFNRC_" + String.valueOf(this.mPhoneId);
                break;
            default:
                Rlog.d(LOG_TAG, "No need to do the reason: " + i);
                return null;
        }
        Rlog.d(LOG_TAG, "key: " + str);
        IccRecords iccRecords = (IccRecords) this.mIccRecords.get();
        if (iccRecords == null) {
            Rlog.d(LOG_TAG, "No iccRecords");
            return null;
        }
        String imsi = iccRecords.getIMSI();
        if (imsi == null || imsi.isEmpty()) {
            Rlog.d(LOG_TAG, "currentImsi is empty");
            return null;
        }
        Rlog.d(LOG_TAG, "currentImsi: " + imsi);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String string = defaultSharedPreferences.getString(str, null);
        if (string == null) {
            Rlog.d(LOG_TAG, "Sharedpref not with: " + str);
            return null;
        }
        String[] strArrSplit = string.split(";");
        if (strArrSplit == null || strArrSplit.length < 2) {
            Rlog.d(LOG_TAG, "infoAry.length < 2");
            return null;
        }
        String str2 = strArrSplit[0];
        String str3 = strArrSplit[1];
        if (str2 == null || str2.isEmpty()) {
            Rlog.d(LOG_TAG, "Sharedpref imsi is empty.");
            return null;
        }
        if (str3 == null || str3.isEmpty()) {
            Rlog.d(LOG_TAG, "Sharedpref number is empty.");
            return null;
        }
        Rlog.d(LOG_TAG, "Sharedpref imsi: " + str2);
        Rlog.d(LOG_TAG, "Sharedpref number: " + str3);
        if (imsi.equals(str2)) {
            Rlog.d(LOG_TAG, "Get dial number from sharepref: " + str3);
            return str3;
        }
        SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
        editorEdit.remove(str);
        if (!editorEdit.commit()) {
            Rlog.e(LOG_TAG, "failed to commit the removal of CF preference: " + str);
        }
        return null;
    }

    public boolean queryCFUAgainAfterSet() {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        boolean z = false;
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "queryCFUAgainAfterSet, ssConf is null, return false");
            return false;
        }
        if (suppServConf.isQueryCFUAgainAfterSet(getOperatorNumeric())) {
            z = true;
        }
        Rlog.d(LOG_TAG, "queryCFUAgainAfterSet: " + z);
        return z;
    }

    public boolean isSupportCFUTimeSlot() {
        boolean z;
        if (isOp(MtkOperatorUtils.OPID.OP01)) {
            z = true;
        } else {
            z = false;
        }
        Rlog.d(LOG_TAG, "isSupportCFUTimeSlot: " + z);
        return z;
    }

    public boolean isNotSupportUtToCSforCFUQuery() {
        return isNotSupportUtToCS();
    }

    public boolean isNoNeedToCSFBWhenIMSRegistered() {
        return isOp(MtkOperatorUtils.OPID.OP01) || isOp(MtkOperatorUtils.OPID.OP02);
    }

    public boolean isResetCSFBStatusAfterFlightMode() {
        return isOp(MtkOperatorUtils.OPID.OP02);
    }

    protected void processIccRecordEvents(int i) {
        if (i == 1) {
            Rlog.d(LOG_TAG, "processIccRecordEvents");
            notifyCallForwardingIndicator();
        } else {
            super.processIccRecordEvents(i);
        }
    }

    void sendErrorResponse(Message message, CommandException.Error error) {
        Rlog.d(LOG_TAG, "sendErrorResponse" + error);
        if (message != null) {
            AsyncResult.forMessage(message, (Object) null, new CommandException(error));
            message.sendToTarget();
        }
    }

    private boolean isAllowXcapIfDataRoaming(String str) {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isAllowXcapIfDataRoaming, ssConf is null, return false");
            return false;
        }
        if (!getServiceState().getDataRoaming()) {
            Rlog.d(LOG_TAG, "isAllowXcapIfDataRoaming: true (not roaming state)");
            return true;
        }
        if (!suppServConf.isNeedCheckDataRoaming(str)) {
            Rlog.d(LOG_TAG, "isAllowXcapIfDataRoaming: true (ignore roaming state)");
            return true;
        }
        Rlog.d(LOG_TAG, "isAllowXcapIfDataRoaming: false (roaming state, block SS)");
        return false;
    }

    private boolean isAllowXcapIfDataEnabled(String str) {
        MtkSuppServConf suppServConf = MtkSuppServManager.getSuppServConf(getPhoneId());
        if (suppServConf == null) {
            Rlog.d(LOG_TAG, "isAllowXcapIfDataEnabled, ssConf is null, return false");
            return false;
        }
        if (!suppServConf.isNeedCheckDataEnabled(str)) {
            return true;
        }
        if (isDataEnabled()) {
            Rlog.d(LOG_TAG, "isAllowXcapIfDataEnabled: true");
            return true;
        }
        Rlog.d(LOG_TAG, "isAllowXcapIfDataEnabled: false");
        return false;
    }

    public int getCsFallbackStatus() {
        if (!isAllowXcapIfDataEnabled(getOperatorNumeric())) {
            this.mCSFallbackMode = 1;
        }
        if (!isAllowXcapIfDataRoaming(getOperatorNumeric())) {
            this.mCSFallbackMode = 1;
        }
        Rlog.d(LOG_TAG, "getCsFallbackStatus is " + this.mCSFallbackMode);
        return this.mCSFallbackMode;
    }

    public void setCsFallbackStatus(int i) {
        Rlog.d(LOG_TAG, "setCsFallbackStatus to " + i);
        this.mCSFallbackMode = i;
    }

    private int getMainCapabilityPhoneId() {
        int i = SystemProperties.getInt("persist.vendor.radio.simswitch", 1) - 1;
        if (i < 0 || i >= TelephonyManager.getDefault().getPhoneCount()) {
            return -1;
        }
        return i;
    }

    public Connection dial(List<String> list, int i) throws CallStateException {
        if (!(this.mImsPhone instanceof MtkImsPhone)) {
            Rlog.d(LOG_TAG, "mImsPhone must be MtkImsPhone to make enhanced conference dial");
            return null;
        }
        MtkImsPhone mtkImsPhone = this.mImsPhone;
        boolean z = isImsUseEnabled() && mtkImsPhone != null && (mtkImsPhone.isVolteEnabled() || mtkImsPhone.isWifiCallingEnabled() || (mtkImsPhone.isVideoEnabled() && VideoProfile.isVideo(i))) && mtkImsPhone.getServiceState().getState() == 0;
        if (!z) {
            Rlog.w(LOG_TAG, "IMS is disabled and can not dial conference call directly.");
            return null;
        }
        if (mtkImsPhone != null) {
            Rlog.w(LOG_TAG, "service state = " + mtkImsPhone.getServiceState().getState());
        }
        if (z && mtkImsPhone != null && mtkImsPhone.getServiceState().getState() == 0) {
            try {
                Rlog.d(LOG_TAG, "Trying IMS PS conference call");
                return mtkImsPhone.dial(list, i);
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "IMS PS conference call exception " + e);
                if (!"cs_fallback".equals(e.getMessage())) {
                    CallStateException callStateException = new CallStateException(e.getMessage());
                    callStateException.setStackTrace(e.getStackTrace());
                    throw callStateException;
                }
            }
        }
        return null;
    }

    public int getCdmaSubscriptionActStatus() {
        if (this.mCdmaSSM != null) {
            return ((MtkCdmaSubscriptionSourceManager) this.mCdmaSSM).getActStatus();
        }
        return 0;
    }

    public void setRoamingEnable(int[] iArr, Message message) {
        Rlog.d(LOG_TAG, "set roaming enable");
        iArr[0] = this.mPhoneId;
        this.mMtkCi.setRoamingEnable(iArr, message);
    }

    public void getRoamingEnable(Message message) {
        Rlog.d(LOG_TAG, "get roaming enable");
        this.mMtkCi.getRoamingEnable(this.mPhoneId, message);
    }

    public boolean isGsmSsPrefer() {
        return (SystemProperties.getInt("persist.vendor.mtk_ct_volte_support", 0) != 0 && isOp(MtkOperatorUtils.OPID.OP09)) || isOp(MtkOperatorUtils.OPID.OP117);
    }

    protected void onCheckForNetworkSelectionModeAutomatic(Message message) {
        AsyncResult asyncResult = (AsyncResult) message.obj;
        Message message2 = (Message) asyncResult.userObj;
        boolean z = true;
        if (asyncResult.exception == null && asyncResult.result != null) {
            try {
                if (((int[]) asyncResult.result)[0] == 0) {
                    try {
                        if (getServiceState().getCellularRegState() != 0) {
                            asyncResult.exception = new CommandException(CommandException.Error.ABORTED);
                        }
                        z = false;
                    } catch (Exception e) {
                        z = false;
                    }
                }
            } catch (Exception e2) {
            }
        }
        Phone.NetworkSelectMessage networkSelectMessage = new Phone.NetworkSelectMessage();
        networkSelectMessage.message = message2;
        networkSelectMessage.operatorNumeric = "";
        networkSelectMessage.operatorAlphaLong = "";
        networkSelectMessage.operatorAlphaShort = "";
        if (z) {
            this.mCi.setNetworkSelectionModeAutomatic(obtainMessage(17, networkSelectMessage));
        } else {
            Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic - already auto, ignoring");
            asyncResult.userObj = networkSelectMessage;
            handleSetSelectNetwork(asyncResult);
        }
        updateSavedNetworkOperator(networkSelectMessage);
    }

    protected String getOperatorNumeric() {
        if (!isPhoneTypeGsm()) {
            this.mCdmaSubscriptionSource = this.mCdmaSSM.getCdmaSubscriptionSource();
        }
        return super.getOperatorNumeric();
    }

    public Message getCFCallbackMessage() {
        return obtainMessage(13);
    }

    public Message getCFTimeSlotCallbackMessage() {
        return obtainMessage(109);
    }

    public boolean isImsUseEnabled() {
        return (ImsManager.getInstance(this.mContext, this.mPhoneId).isVolteEnabledByPlatform() && MtkImsManager.isEnhanced4gLteModeSettingEnabledByUser(this.mContext, this.mPhoneId)) || (ImsManager.getInstance(this.mContext, this.mPhoneId).isWfcEnabledByPlatform() && MtkImsManager.isWfcEnabledByUser(this.mContext, this.mPhoneId) && MtkImsManager.isNonTtyOrTtyOnVolteEnabled(this.mContext, this.mPhoneId));
    }

    public void cleanCallForwardingIndicatorFromSharedPref() {
        setCallForwardingIndicatorInSharedPref(false);
    }

    public boolean shouldProcessSelfActivation() {
        int selfActivateState = getSelfActivationInstance().getSelfActivateState();
        Rlog.d(LOG_TAG, "shouldProcessSelfActivation() state: " + selfActivateState);
        return selfActivateState == 2;
    }

    public boolean useImsForPCOChanged() {
        int pCO520State = getSelfActivationInstance().getPCO520State();
        Rlog.d(LOG_TAG, "pcoState() state: " + pCO520State);
        return pCO520State == 1;
    }

    protected boolean needResetPhbIntMgr() {
        return false;
    }

    public String getFullIccSerialNumber() {
        String fullIccSerialNumber = super.getFullIccSerialNumber();
        if (fullIccSerialNumber != null) {
            return fullIccSerialNumber;
        }
        if (!isPhoneTypeGsm() && this.mUiccController != null) {
            IccRecords iccRecords = this.mUiccController.getIccRecords(this.mPhoneId, 1);
            fullIccSerialNumber = iccRecords != null ? iccRecords.getFullIccId() : null;
            if (fullIccSerialNumber != null) {
                return fullIccSerialNumber;
            }
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            SubscriptionInfo activeSubscriptionInfo = SubscriptionManager.from(getContext()).getActiveSubscriptionInfo(getSubId());
            if (activeSubscriptionInfo != null) {
                fullIccSerialNumber = activeSubscriptionInfo.getIccId();
            }
            return fullIccSerialNumber;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected void initRatSpecific(int i) {
        super.initRatSpecific(i);
        if (isPhoneTypeGsm()) {
            this.mIsPhoneInEcmState = getInEcmMode();
            if (this.mIsPhoneInEcmState) {
                this.mCi.exitEmergencyCallbackMode(obtainMessage(26));
            }
        }
    }

    public void exitEmergencyCallbackMode() {
        Rlog.d(LOG_TAG, "exitEmergencyCallbackMode: mImsPhone=" + this.mImsPhone + " isPhoneTypeGsm=" + isPhoneTypeGsm());
        Rlog.d(LOG_TAG, "exitEmergencyCallbackMode()");
        tryTurnOnWifiForE911Finished();
        if (isPhoneTypeGsm()) {
            if (this.mImsPhone != null) {
                this.mImsPhone.exitEmergencyCallbackMode();
            }
        } else {
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
            this.mCi.exitEmergencyCallbackMode((Message) null);
        }
    }

    public void sendExitEmergencyCallbackModeMessage() {
        Rlog.d(LOG_TAG, "sendExitEmergencyCallbackModeMessage()");
        Message messageObtainMessage = obtainMessage(26);
        AsyncResult.forMessage(messageObtainMessage);
        sendMessage(messageObtainMessage);
    }

    private void tryTurnOffWifiForE911(boolean z) {
        if (this.mContext == null) {
            return;
        }
        boolean z2 = ((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(getSubId()).getBoolean("mtk_carrier_turn_off_wifi_before_e911");
        boolean z3 = SystemProperties.getInt(PROPERTY_WFC_ENABLE, 0) == 1;
        Rlog.d(LOG_TAG, "tryTurnOffWifiForEcc() carrierConfig: " + z2 + " isECC: " + z + " isWfcEnable: " + z3);
        if (!z || !z2 || z3) {
            return;
        }
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        this.mWifiIsEnabledBeforeE911 = wifiManager.isWifiEnabled();
        Rlog.d(LOG_TAG, "tryTurnOffWifiForEcc() wifiEnabled: " + this.mWifiIsEnabledBeforeE911);
        if (this.mWifiIsEnabledBeforeE911) {
            wifiManager.setWifiEnabled(false);
        }
    }

    private void tryTurnOnWifiForE911Finished() {
        if (this.mContext == null || !((CarrierConfigManager) this.mContext.getSystemService("carrier_config")).getConfigForSubId(getSubId()).getBoolean("mtk_carrier_turn_off_wifi_before_e911")) {
            return;
        }
        WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        Rlog.d(LOG_TAG, "tryTurnOnWifiForEcbmFinished() wifiEnabled: " + this.mWifiIsEnabledBeforeE911);
        if (this.mWifiIsEnabledBeforeE911) {
            wifiManager.setWifiEnabled(true);
        }
    }

    public String getDeviceSvn() {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte() || isPhoneTypeCdma()) {
            return this.mImeiSv;
        }
        loge("getDeviceSvn(): return 0");
        return "0";
    }

    public void invokeOemRilRequestRaw(byte[] bArr, Message message) {
        this.mMtkCi.invokeOemRilRequestRaw(bArr, message);
    }

    protected void phoneObjectUpdater(int i) {
        this.mNewVoiceTech = i;
        super.phoneObjectUpdater(i);
    }

    public void setDisable2G(boolean z, Message message) {
        Rlog.d(LOG_TAG, "setDisable2G " + z);
        this.mMtkCi.setDisable2G(z, message);
    }

    public void getDisable2G(Message message) {
        Rlog.d(LOG_TAG, "getDisable2G");
        this.mMtkCi.getDisable2G(message);
    }

    public void setVoiceCallForwardingFlag(int i, boolean z, String str) {
        IccRecords iccRecords;
        super.setVoiceCallForwardingFlag(i, z, str);
        if (getPhoneType() == 2 && isGsmSsPrefer()) {
            if (this.mUiccController != null && (iccRecords = this.mUiccController.getIccRecords(this.mPhoneId, 1)) != null) {
                iccRecords.setVoiceCallForwardingFlag(i, z, str);
            }
            notifyCallForwardingIndicator();
        }
    }

    public boolean getCallForwardingIndicator() {
        int callForwardingIndicatorFromSharedPref;
        if (getPhoneType() == 2 && isGsmSsPrefer()) {
            IccRecords iccRecords = null;
            if (this.mUiccController != null) {
                iccRecords = this.mUiccController.getIccRecords(this.mPhoneId, 1);
            }
            if (iccRecords != null) {
                callForwardingIndicatorFromSharedPref = iccRecords.getVoiceCallForwardingFlag();
            } else {
                callForwardingIndicatorFromSharedPref = -1;
            }
            if (callForwardingIndicatorFromSharedPref == -1) {
                callForwardingIndicatorFromSharedPref = getCallForwardingIndicatorFromSharedPref();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("getCallForwardingIndicator: iccForwardingFlag=");
            sb.append(iccRecords != null ? Integer.valueOf(iccRecords.getVoiceCallForwardingFlag()) : "null");
            sb.append(", sharedPrefFlag=");
            sb.append(getCallForwardingIndicatorFromSharedPref());
            Rlog.v(LOG_TAG, sb.toString());
            return callForwardingIndicatorFromSharedPref == 1;
        }
        return super.getCallForwardingIndicator();
    }

    public boolean isCdmaLessDevice() {
        boolean z;
        if ("3".equals(SystemProperties.get(PROP_VZW_DEVICE_TYPE, "0")) || "4".equals(SystemProperties.get(PROP_VZW_DEVICE_TYPE, "0"))) {
            z = true;
        } else {
            z = false;
        }
        Rlog.d(LOG_TAG, "isCdmaLess: " + z);
        return z;
    }

    public CellLocation getCellLocation(WorkSource workSource) {
        synchronized (this.mLock) {
            if (!isPhoneTypeGsm()) {
                CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) this.mSST.mCellLoc;
                if (Settings.Secure.getInt(getContext().getContentResolver(), "location_mode", 0) == 0) {
                    CdmaCellLocation cdmaCellLocation2 = new CdmaCellLocation();
                    cdmaCellLocation2.setCellLocationData(cdmaCellLocation.getBaseStationId(), Integer.MAX_VALUE, Integer.MAX_VALUE, cdmaCellLocation.getSystemId(), cdmaCellLocation.getNetworkId());
                    cdmaCellLocation = cdmaCellLocation2;
                }
                return cdmaCellLocation;
            }
            return this.mSST.getCellLocation(workSource);
        }
    }

    public void invokeOemRilRequestStrings(String[] strArr, Message message) {
        this.mMtkCi.invokeOemRilRequestStrings(strArr, message);
    }

    public void setSuppServProperty(String str, String str2) {
        Rlog.d(LOG_TAG, "setSuppServProperty, name = " + str + ", value = " + str2);
        this.mCallbackLatch = new CountDownLatch(1);
        this.mMtkCi.setSuppServProperty(str, str2, obtainMessage(1004));
        if (!isCallbackDone()) {
            Rlog.e(LOG_TAG, "waitForCallback: callback is not done!");
        }
    }

    public void getSuppServProperty(String str) {
        Rlog.d(LOG_TAG, "getSuppServProperty, name = " + str);
        this.mMtkCi.getSuppServProperty(str, null);
    }

    private boolean isCallbackDone() {
        boolean zAwait;
        try {
            zAwait = this.mCallbackLatch.await(5000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            zAwait = false;
        }
        Rlog.d(LOG_TAG, "waitForCallback: isDone=" + zAwait);
        return zAwait;
    }

    public void setIsInEcm(boolean z) {
        super.setIsInEcm(z);
        setSystemProperty("vendor.ril.cdma.inecmmode_by_slot", String.valueOf(z));
    }

    public PhoneConstants.State getState() {
        PhoneConstants.State state;
        if (this.mImsPhone != null && (state = this.mImsPhone.getState()) != PhoneConstants.State.IDLE) {
            return state;
        }
        if (this.mCT.mState == PhoneConstants.State.IDLE && ((MtkGsmCdmaCallTracker) this.mCT).getHandoverConnectionSize() > 0) {
            return PhoneConstants.State.OFFHOOK;
        }
        return this.mCT.mState;
    }

    private boolean needRetryCsEmergency() {
        if (this.mImsPhone == null || !(this.mImsPhone instanceof ImsPhone)) {
            return true;
        }
        return !this.mImsPhone.isInCall();
    }

    public String getLine1PhoneNumber() {
        String msisdnNumber;
        if (isPhoneTypeGsm()) {
            String str = SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR);
            if (str != null && "OP20".equals(str)) {
                MtkIccCardConstants.CardType cdmaCardType = MtkTelephonyManagerEx.getDefault().getCdmaCardType(getPhoneId());
                boolean zIs4GCard = false;
                if (cdmaCardType != null) {
                    zIs4GCard = cdmaCardType.is4GCard();
                }
                if (zIs4GCard) {
                    RuimRecords iccRecords = UiccController.getInstance().getIccRecords(getPhoneId(), 2);
                    IccRecords iccRecords2 = (IccRecords) this.mIccRecords.get();
                    if (SDBG) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("getLine1PhoneNumber, number = ");
                        if (iccRecords != null && iccRecords.getMdn() != null && !iccRecords.getMdn().isEmpty()) {
                            msisdnNumber = iccRecords.getMdn();
                        } else if (iccRecords2 == null) {
                            msisdnNumber = null;
                        } else {
                            msisdnNumber = iccRecords2.getMsisdnNumber();
                        }
                        sb.append(msisdnNumber);
                        sb.append(", slot = ");
                        sb.append(getPhoneId());
                        logd(sb.toString());
                    }
                    if (iccRecords != null && iccRecords.getMdn() != null && !iccRecords.getMdn().isEmpty()) {
                        return iccRecords.getMdn();
                    }
                    if (iccRecords2 != null) {
                        return iccRecords2.getMsisdnNumber();
                    }
                    return null;
                }
                IccRecords iccRecords3 = (IccRecords) this.mIccRecords.get();
                if (iccRecords3 != null) {
                    return iccRecords3.getMsisdnNumber();
                }
                return null;
            }
            IccRecords iccRecords4 = (IccRecords) this.mIccRecords.get();
            if (iccRecords4 != null) {
                return iccRecords4.getMsisdnNumber();
            }
            return null;
        }
        return this.mSST.getMdnNumber();
    }
}
