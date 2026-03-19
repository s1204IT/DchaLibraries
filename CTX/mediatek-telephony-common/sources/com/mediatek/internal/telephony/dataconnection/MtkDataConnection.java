package com.mediatek.internal.telephony.dataconnection;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.INetworkManagementEventObserver;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.StringNetworkSpecifier;
import android.os.AsyncResult;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.system.OsConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.data.DataCallResponse;
import android.text.TextUtils;
import android.util.Pair;
import android.util.StatsLog;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.dataconnection.DataServiceManager;
import com.android.internal.telephony.dataconnection.DcController;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.android.internal.telephony.dataconnection.DcTesterFailBringUpAll;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.server.net.BaseNetworkObserver;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkHardwareConfig;
import com.mediatek.internal.telephony.MtkTelephonyDevController;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.dataconnection.DcFailCauseManager;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimConstants;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class MtkDataConnection extends DataConnection {
    private static final boolean DBG = true;
    static final int EVENT_ADDRESS_REMOVED = 262170;
    static final int EVENT_FALLBACK_RETRY_CONNECTION = 262172;
    static final int EVENT_GET_DATA_CALL_LIST = 262174;
    static final int EVENT_IPV4_ADDRESS_REMOVED = 262168;
    static final int EVENT_IPV6_ADDRESS_REMOVED = 262169;
    static final int EVENT_IPV6_ADDRESS_UPDATED = 262173;
    static final int EVENT_VOICE_CALL = 262171;
    private static final String INTENT_RETRY_ALARM_TAG = "tag";
    private static final String INTENT_RETRY_ALARM_WHAT = "what";
    private static final int RA_GET_IPV6_VALID_FAIL = -1000;
    private static final int RA_INITIAL_FAIL = -1;
    private static final int RA_REFRESH_FAIL = -2;
    private static final boolean VDBG;
    private String mActionRetry;
    private AlarmManager mAlarmManager;
    private INetworkManagementEventObserver mAlertObserver;
    private IDataConnectionExt mDataConnectionExt;
    protected DcFailCauseManager mDcFcMgr;
    private AddressInfo mGlobalV6AddrInfo;
    private BroadcastReceiver mIntentReceiver;
    private String mInterfaceName;
    private boolean mIsInVoiceCall;
    private boolean mIsOp20;
    private boolean mIsSupportConcurrent;
    private final INetworkManagementService mNetworkManager;
    private int mRat;
    private int mRetryCount;
    private SubscriptionController mSubController;
    private TelephonyDevController mTelDevController;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;
    private long mValid;

    static int access$13108(MtkDataConnection mtkDataConnection) {
        int i = mtkDataConnection.mRetryCount;
        mtkDataConnection.mRetryCount = i + 1;
        return i;
    }

    static {
        VDBG = SystemProperties.get("ro.build.type").equals("eng");
        sCmdToString = new String[31];
        sCmdToString[0] = "EVENT_CONNECT";
        sCmdToString[1] = "EVENT_SETUP_DATA_CONNECTION_DONE";
        sCmdToString[3] = "EVENT_DEACTIVATE_DONE";
        sCmdToString[4] = "EVENT_DISCONNECT";
        sCmdToString[5] = "EVENT_RIL_CONNECTED";
        sCmdToString[6] = "EVENT_DISCONNECT_ALL";
        sCmdToString[7] = "EVENT_DATA_STATE_CHANGED";
        sCmdToString[8] = "EVENT_TEAR_DOWN_NOW";
        sCmdToString[9] = "EVENT_LOST_CONNECTION";
        sCmdToString[11] = "EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED";
        sCmdToString[12] = "EVENT_DATA_CONNECTION_ROAM_ON";
        sCmdToString[13] = "EVENT_DATA_CONNECTION_ROAM_OFF";
        sCmdToString[14] = "EVENT_BW_REFRESH_RESPONSE";
        sCmdToString[15] = "EVENT_DATA_CONNECTION_VOICE_CALL_STARTED";
        sCmdToString[16] = "EVENT_DATA_CONNECTION_VOICE_CALL_ENDED";
        sCmdToString[17] = "EVENT_DATA_CONNECTION_OVERRIDE_CHANGED";
        sCmdToString[18] = "EVENT_KEEPALIVE_STATUS";
        sCmdToString[19] = "EVENT_KEEPALIVE_STARTED";
        sCmdToString[20] = "EVENT_KEEPALIVE_STOPPED";
        sCmdToString[21] = "EVENT_KEEPALIVE_START_REQUEST";
        sCmdToString[22] = "EVENT_KEEPALIVE_STOP_REQUEST";
        sCmdToString[23] = "EVENT_LINK_CAPACITY_CHANGED";
        sCmdToString[24] = "EVENT_IPV4_ADDRESS_REMOVED";
        sCmdToString[25] = "EVENT_IPV6_ADDRESS_REMOVED";
        sCmdToString[26] = "EVENT_ADDRESS_REMOVED";
        sCmdToString[27] = "EVENT_VOICE_CALL";
        sCmdToString[28] = "EVENT_FALLBACK_RETRY_CONNECTION";
        sCmdToString[29] = "EVENT_IPV6_ADDRESS_UPDATED";
        sCmdToString[30] = "EVENT_GET_DATA_CALL_LIST";
    }

    public MtkDataConnection(Phone phone, String str, int i, DcTracker dcTracker, DataServiceManager dataServiceManager, DcTesterFailBringUpAll dcTesterFailBringUpAll, DcController dcController) {
        super(phone, str, i, dcTracker, dataServiceManager, dcTesterFailBringUpAll, dcController);
        this.mTelephonyCustomizationFactory = null;
        this.mDataConnectionExt = null;
        this.mSubController = SubscriptionController.getInstance();
        boolean z = false;
        this.mRetryCount = 0;
        this.mInterfaceName = null;
        this.mIsInVoiceCall = false;
        this.mIsSupportConcurrent = false;
        this.mGlobalV6AddrInfo = null;
        this.mTelDevController = MtkTelephonyDevController.getInstance();
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!TextUtils.isEmpty(action)) {
                    if (TextUtils.equals(action, MtkDataConnection.this.mActionRetry)) {
                        if (!intent.hasExtra(MtkDataConnection.INTENT_RETRY_ALARM_WHAT)) {
                            throw new RuntimeException(MtkDataConnection.this.mActionRetry + " has no INTENT_RETRY_ALRAM_WHAT");
                        }
                        if (!intent.hasExtra(MtkDataConnection.INTENT_RETRY_ALARM_TAG)) {
                            throw new RuntimeException(MtkDataConnection.this.mActionRetry + " has no INTENT_RETRY_ALRAM_TAG");
                        }
                        int intExtra = intent.getIntExtra(MtkDataConnection.INTENT_RETRY_ALARM_WHAT, Integer.MAX_VALUE);
                        int intExtra2 = intent.getIntExtra(MtkDataConnection.INTENT_RETRY_ALARM_TAG, Integer.MAX_VALUE);
                        MtkDataConnection.this.log("onReceive: action=" + action + " sendMessage(what:" + MtkDataConnection.this.getWhatToString(intExtra) + ", tag:" + intExtra2 + ")");
                        MtkDataConnection.this.sendMessage(MtkDataConnection.this.obtainMessage(intExtra, intExtra2, 0));
                        return;
                    }
                    if (TextUtils.equals(action, "com.mediatek.common.carrierexpress.operator_config_changed")) {
                        MtkDataConnection.this.mIsOp20 = "OP20".equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, ""));
                        return;
                    }
                    MtkDataConnection.this.log("onReceive: unknown action=" + action);
                    return;
                }
                MtkDataConnection.this.log("onReceive: ignore empty action='" + action + "'");
            }
        };
        if ("OP20".equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "")) || ("OP20".equals(SystemProperties.get("ril.fwk.test.optr", "")) && "eng".equals(SystemProperties.get("ro.build.type", "")))) {
            z = true;
        }
        this.mIsOp20 = z;
        this.mAlertObserver = new BaseNetworkObserver() {
            public void addressRemoved(String str2, LinkAddress linkAddress) {
                MtkDataConnection.this.sendMessageForSM(MtkDataConnection.this.getEventByAddress(false, linkAddress), str2, linkAddress);
            }

            public void addressUpdated(String str2, LinkAddress linkAddress) {
                MtkDataConnection.this.sendMessageForSM(MtkDataConnection.this.getEventByAddress(true, linkAddress), str2, linkAddress);
            }
        };
        setConnectionRat(1, "construct instance");
        try {
            this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(this.mPhone.getContext());
            this.mDataConnectionExt = this.mTelephonyCustomizationFactory.makeDataConnectionExt(this.mPhone.getContext());
        } catch (Exception e) {
            log("mDataConnectionExt init fail");
            e.printStackTrace();
        }
        this.mDcFcMgr = DcFailCauseManager.getInstance(this.mPhone);
        log("get INetworkManagementService");
        this.mNetworkManager = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        this.mAlarmManager = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        this.mActionRetry = getClass().getCanonicalName() + "." + getName() + ".action_retry";
        resetRetryCount();
    }

    protected void tearDownData(Object obj) {
        ApnContext apnContext;
        int i = 1;
        if (obj != null && (obj instanceof DataConnection.DisconnectParams)) {
            DataConnection.DisconnectParams disconnectParams = (DataConnection.DisconnectParams) obj;
            apnContext = disconnectParams.mApnContext;
            if (TextUtils.equals(disconnectParams.mReason, "radioTurnedOff") || TextUtils.equals(disconnectParams.mReason, "pdpReset")) {
                i = 2;
            } else if (TextUtils.equals(disconnectParams.mReason, MtkGsmCdmaPhone.REASON_RA_FAILED)) {
                if (this.mValid == -1) {
                    i = ExternalSimConstants.MSG_ID_CAPABILITY_SWITCH_DONE;
                } else if (this.mValid == -2) {
                    i = MtkGsmCdmaPhone.EVENT_GET_CLIR_COMPLETE;
                }
            } else if (TextUtils.equals(disconnectParams.mReason, MtkGsmCdmaPhone.REASON_PCSCF_ADDRESS_FAILED)) {
                i = MtkGsmCdmaPhone.EVENT_USSI_CSFB;
            } else if (TextUtils.equals(disconnectParams.mReason, "apnChanged")) {
                i = MtkGsmCdmaPhone.EVENT_SET_CALL_BARRING_COMPLETE;
            }
        } else {
            apnContext = null;
        }
        String str = "tearDownData. mCid=" + this.mCid + ", reason=" + i;
        log(str);
        if (apnContext != null) {
            apnContext.requestLog(str);
        }
        this.mDataServiceManager.deactivateDataCall(this.mCid, i, obtainMessage(262147, this.mTag, 0, obj));
    }

    protected void clearSettings() {
        super.clearSettings();
        log("clearSettings");
        this.mGlobalV6AddrInfo = null;
        resetRetryCount();
        setConnectionRat(1, "clear setting");
    }

    protected DataConnection.SetupResult onSetupConnectionCompleted(int i, DataCallResponse dataCallResponse, DataConnection.ConnectionParams connectionParams) {
        if (connectionParams.mTag != this.mTag) {
            log("onSetupConnectionCompleted stale cp.tag=" + connectionParams.mTag + ", mtag=" + this.mTag);
            return DataConnection.SetupResult.ERROR_STALE;
        }
        if (i == 4) {
            DataConnection.SetupResult setupResult = DataConnection.SetupResult.ERROR_RADIO_NOT_AVAILABLE;
            setupResult.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
            return setupResult;
        }
        if (dataCallResponse.getStatus() != 0) {
            if (dataCallResponse.getStatus() == DcFailCause.RADIO_NOT_AVAILABLE.getErrorCode()) {
                DataConnection.SetupResult setupResult2 = DataConnection.SetupResult.ERROR_RADIO_NOT_AVAILABLE;
                setupResult2.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
                return setupResult2;
            }
            DataConnection.SetupResult setupResult3 = DataConnection.SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
            setupResult3.mFailCause = DcFailCause.fromInt(dataCallResponse.getStatus());
            return setupResult3;
        }
        log("onSetupConnectionCompleted received successful DataCallResponse");
        this.mCid = dataCallResponse.getCallId();
        this.mPcscfAddr = (String[]) dataCallResponse.getPcscfs().toArray(new String[dataCallResponse.getPcscfs().size()]);
        setConnectionRat(decodeRat(dataCallResponse.getActive()), "data call response");
        DataConnection.SetupResult setupResult4 = updateLinkProperty(dataCallResponse).setupResult;
        this.mInterfaceName = dataCallResponse.getIfname();
        log("onSetupConnectionCompleted: ifname-" + this.mInterfaceName);
        return setupResult4;
    }

    protected NetworkCapabilities getNetworkCapabilities() {
        int i;
        int i2;
        int i3;
        byte b;
        boolean z;
        ArrayList<ApnSetting> wifiApns;
        NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        boolean z2 = false;
        networkCapabilities.addTransportType(0);
        ApnSetting apnSetting = this.mApnSetting;
        if (this.mConnectionParams != null && this.mConnectionParams.mApnContext != null && this.mRat == 2 && (wifiApns = ((MtkApnContext) this.mConnectionParams.mApnContext).getWifiApns()) != null) {
            for (ApnSetting apnSetting2 : wifiApns) {
                if (apnSetting2 != null && !apnSetting2.apn.equals("")) {
                    log("makeNetworkCapabilities: apn: " + apnSetting2.apn);
                    apnSetting = apnSetting2;
                }
            }
        }
        if (apnSetting != null) {
            String[] strArr = apnSetting.types;
            int length = strArr.length;
            int i4 = 0;
            while (i4 < length) {
                String str = strArr[i4];
                if (this.mRestrictedNetworkOverride || this.mConnectionParams == null || !this.mConnectionParams.mUnmeteredUseOnly || !MtkApnSetting.isMeteredApnType(str, this.mPhone)) {
                    switch (str.hashCode()) {
                        case -318686769:
                            b = str.equals("preempt") ? (byte) 15 : (byte) -1;
                            break;
                        case 42:
                            if (str.equals("*")) {
                                b = 0;
                                break;
                            }
                            break;
                        case 3352:
                            if (str.equals("ia")) {
                                b = 8;
                                break;
                            }
                            break;
                        case 97545:
                            if (str.equals("bip")) {
                                b = 13;
                                break;
                            }
                            break;
                        case 98292:
                            if (str.equals("cbs")) {
                                b = 7;
                                break;
                            }
                            break;
                        case 99837:
                            if (str.equals("dun")) {
                                b = 4;
                                break;
                            }
                            break;
                        case 104399:
                            if (str.equals("ims")) {
                                b = 6;
                                break;
                            }
                            break;
                        case 108243:
                            if (str.equals("mms")) {
                                b = 2;
                                break;
                            }
                            break;
                        case 112738:
                            if (str.equals("rcs")) {
                                b = 12;
                                break;
                            }
                            break;
                        case 117478:
                            if (str.equals("wap")) {
                                b = 10;
                                break;
                            }
                            break;
                        case 3149046:
                            if (str.equals("fota")) {
                                b = 5;
                                break;
                            }
                            break;
                        case 3541982:
                            if (str.equals("supl")) {
                                b = 3;
                                break;
                            }
                            break;
                        case 3629217:
                            if (str.equals("vsim")) {
                                b = 14;
                                break;
                            }
                            break;
                        case 3673178:
                            if (str.equals("xcap")) {
                                b = PplMessageManager.Type.INSTRUCTION_DESCRIPTION2;
                                break;
                            }
                            break;
                        case 1544803905:
                            if (str.equals("default")) {
                                b = 1;
                                break;
                            }
                            break;
                        case 1629013393:
                            if (str.equals("emergency")) {
                                b = 9;
                                break;
                            }
                            break;
                    }
                    switch (b) {
                        case 0:
                            if (isDefaultDataSubPhone(this.mPhone)) {
                                networkCapabilities.addCapability(12);
                            }
                            z = false;
                            networkCapabilities.addCapability(0);
                            networkCapabilities.addCapability(1);
                            networkCapabilities.addCapability(3);
                            networkCapabilities.addCapability(5);
                            networkCapabilities.addCapability(7);
                            networkCapabilities.addCapability(2);
                            networkCapabilities.addCapability(25);
                            networkCapabilities.addCapability(9);
                            networkCapabilities.addCapability(8);
                            networkCapabilities.addCapability(27);
                            networkCapabilities.addCapability(26);
                            continue;
                        case 1:
                            if (isDefaultDataSubPhone(this.mPhone)) {
                                networkCapabilities.addCapability(12);
                            }
                            break;
                        case 2:
                            networkCapabilities.addCapability(0);
                            break;
                        case 3:
                            networkCapabilities.addCapability(1);
                            break;
                        case 4:
                            networkCapabilities.addCapability(2);
                            break;
                        case 5:
                            networkCapabilities.addCapability(3);
                            break;
                        case 6:
                            networkCapabilities.addCapability(4);
                            break;
                        case 7:
                            networkCapabilities.addCapability(5);
                            break;
                        case 8:
                            networkCapabilities.addCapability(7);
                            break;
                        case 9:
                            networkCapabilities.addCapability(10);
                            break;
                        case 10:
                            networkCapabilities.addCapability(25);
                            break;
                        case 11:
                            networkCapabilities.addCapability(9);
                            break;
                        case 12:
                            networkCapabilities.addCapability(8);
                            break;
                        case 13:
                            networkCapabilities.addCapability(27);
                            break;
                        case 14:
                            networkCapabilities.addCapability(26);
                            break;
                        case 15:
                            Iterator it = this.mApnContexts.keySet().iterator();
                            while (true) {
                                if (!it.hasNext()) {
                                    break;
                                } else if (TextUtils.equals("preempt", ((ApnContext) it.next()).getApnType())) {
                                    networkCapabilities.addCapability(12);
                                    break;
                                }
                            }
                            break;
                    }
                    z = false;
                } else {
                    log("Dropped the metered " + str + " for the unmetered data call.");
                    z = z2;
                }
                i4++;
                z2 = z;
            }
            addInternetCapForDunOnlyType(apnSetting, networkCapabilities);
            if ((this.mConnectionParams == null || !this.mConnectionParams.mUnmeteredUseOnly || this.mRestrictedNetworkOverride) && apnSetting.isMetered(this.mPhone)) {
                networkCapabilities.removeCapability(11);
            } else {
                networkCapabilities.addCapability(11);
            }
            networkCapabilities.maybeMarkCapabilitiesRestricted();
        }
        if (this.mRestrictedNetworkOverride) {
            networkCapabilities.removeCapability(13);
            networkCapabilities.removeCapability(2);
        }
        int i5 = this.mRilRat;
        if (i5 != 19) {
            switch (i5) {
                case 1:
                    i = 80;
                    i2 = 80;
                    break;
                case 2:
                    i = 59;
                    i2 = 236;
                    break;
                case 3:
                    i = 384;
                    i2 = 384;
                    break;
                case 4:
                case 5:
                default:
                    i2 = 14;
                    i = 14;
                    break;
                case 6:
                    i = 100;
                    i2 = 100;
                    break;
                case 7:
                    i = 153;
                    i2 = 2457;
                    break;
                case 8:
                    i = 1843;
                    i2 = 3174;
                    break;
                case 9:
                    i3 = 2048;
                    i = i3;
                    i2 = 14336;
                    break;
                case 10:
                    i3 = 5898;
                    i = i3;
                    i2 = 14336;
                    break;
                case 11:
                    i3 = 5898;
                    i = i3;
                    i2 = 14336;
                    break;
                case 12:
                    i = 1843;
                    i2 = 5017;
                    break;
                case 13:
                    i = 153;
                    i2 = 2516;
                    break;
                case 14:
                    i = 51200;
                    i2 = 102400;
                    break;
                case 15:
                    i = 11264;
                    i2 = 43008;
                    break;
            }
        } else {
            i = 51200;
            i2 = 102400;
        }
        networkCapabilities.setLinkUpstreamBandwidthKbps(i);
        networkCapabilities.setLinkDownstreamBandwidthKbps(i2);
        networkCapabilities.setNetworkSpecifier(new StringNetworkSpecifier(Integer.toString(this.mPhone.getSubId())));
        networkCapabilities.setCapability(18, !this.mPhone.getServiceState().getDataRoaming());
        networkCapabilities.addCapability(20);
        if ((this.mSubscriptionOverride & 1) != 0) {
            networkCapabilities.addCapability(11);
        }
        if ((this.mSubscriptionOverride & 2) != 0) {
            networkCapabilities.removeCapability(20);
        }
        return networkCapabilities;
    }

    private void addInternetCapForDunOnlyType(ApnSetting apnSetting, NetworkCapabilities networkCapabilities) {
        if (this.mIsOp20 && apnSetting.types.length == 1) {
            boolean zEquals = TextUtils.equals("dun", apnSetting.types[0]);
            boolean zContains = apnSetting.apn.contains("pamsn");
            if (zEquals && zContains) {
                int i = this.mRilRat;
                if (i != 12) {
                    switch (i) {
                    }
                }
                networkCapabilities.addCapability(12);
            }
        }
    }

    protected void updateNetworkInfoSuspendState() {
        NetworkInfo.DetailedState detailedState = this.mNetworkInfo.getDetailedState();
        if (this.mNetworkAgent == null) {
            Rlog.e(getName(), "Setting suspend state without a NetworkAgent");
        }
        ServiceStateTracker serviceStateTracker = this.mPhone.getServiceStateTracker();
        boolean zIsNwNeedSuspended = isNwNeedSuspended();
        log("updateNetworkInfoSuspendState: oldState = " + detailedState + ", currentDataConnectionState = " + serviceStateTracker.getCurrentDataConnectionState() + ", bNwNeedSuspended = " + zIsNwNeedSuspended);
        if (serviceStateTracker.getCurrentDataConnectionState() != 0) {
            if (!this.mIsInVoiceCall && !MtkDcHelper.isImsOrEmergencyApn(getApnType()) && !MtkDcHelper.hasVsimApn(getApnType())) {
                this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, null, this.mNetworkInfo.getExtraInfo());
                return;
            } else {
                this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, this.mNetworkInfo.getExtraInfo());
                return;
            }
        }
        if (zIsNwNeedSuspended) {
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, null, this.mNetworkInfo.getExtraInfo());
        } else {
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, this.mNetworkInfo.getExtraInfo());
        }
    }

    private class MtkDcDefaultState extends DataConnection.DcDefaultState {
        private MtkDcDefaultState() {
            super(MtkDataConnection.this);
        }

        public void enter() {
            MtkDataConnection.this.log("DcDefaultState: enter");
            MtkDataConnection.this.mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(MtkDataConnection.this.getHandler(), 262155, (Object) null);
            MtkDataConnection.this.mPhone.getServiceStateTracker().registerForDataRoamingOn(MtkDataConnection.this.getHandler(), 262156, (Object) null);
            MtkDataConnection.this.mPhone.getServiceStateTracker().registerForDataRoamingOff(MtkDataConnection.this.getHandler(), 262157, (Object) null, true);
            if (MtkDataConnection.this.mTelDevController != null && MtkDataConnection.this.mTelDevController.getModem(0) != null && !((MtkHardwareConfig) MtkDataConnection.this.mTelDevController.getModem(0)).hasRaCapability()) {
                MtkDataConnection.this.registerNetworkAlertObserver();
            }
            MtkDataConnection.this.mDcController.addDc(MtkDataConnection.this);
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MtkDataConnection.this.mActionRetry);
            MtkDataConnection.this.log("DcDefaultState: register for intent action=" + MtkDataConnection.this.mActionRetry);
            intentFilter.addAction("com.mediatek.common.carrierexpress.operator_config_changed");
            MtkDataConnection.this.mPhone.getContext().registerReceiver(MtkDataConnection.this.mIntentReceiver, intentFilter, null, MtkDataConnection.this.getHandler());
        }

        public void exit() {
            MtkDataConnection.this.log("DcDefaultState: exit");
            MtkDataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(MtkDataConnection.this.getHandler());
            MtkDataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRoamingOn(MtkDataConnection.this.getHandler());
            MtkDataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(MtkDataConnection.this.getHandler());
            MtkDataConnection.this.mDcController.removeDc(MtkDataConnection.this);
            if (MtkDataConnection.this.mAc != null) {
                MtkDataConnection.this.mAc.disconnected();
                MtkDataConnection.this.mAc = null;
            }
            MtkDataConnection.this.mApnContexts = null;
            MtkDataConnection.this.mReconnectIntent = null;
            MtkDataConnection.this.mDct = null;
            MtkDataConnection.this.mApnSetting = null;
            MtkDataConnection.this.mPhone = null;
            MtkDataConnection.this.mDataServiceManager = null;
            MtkDataConnection.this.mLinkProperties = null;
            MtkDataConnection.this.mLastFailCause = null;
            MtkDataConnection.this.mUserData = null;
            MtkDataConnection.this.mDcController = null;
            MtkDataConnection.this.mDcTesterFailBringUpAll = null;
            if (MtkDataConnection.this.mTelDevController != null && MtkDataConnection.this.mTelDevController.getModem(0) != null && !((MtkHardwareConfig) MtkDataConnection.this.mTelDevController.getModem(0)).hasRaCapability()) {
                MtkDataConnection.this.unregisterNetworkAlertObserver();
            }
            MtkDataConnection.this.mPhone.getContext().unregisterReceiver(MtkDataConnection.this.mIntentReceiver);
        }

        public boolean processMessage(Message message) {
            if (MtkDataConnection.VDBG) {
                MtkDataConnection.this.log("DcDefault msg=" + MtkDataConnection.this.getWhatToString(message.what) + " RefCount=" + MtkDataConnection.this.mApnContexts.size());
            }
            int i = message.what;
            if (i == 262155) {
                if (MtkDataConnection.this.mIsInVoiceCall) {
                    MtkDataConnection.this.mIsSupportConcurrent = MtkDcHelper.getInstance().isDataSupportConcurrent(MtkDataConnection.this.mPhone.getPhoneId());
                }
                return super.processMessage(message);
            }
            if (i == MtkDataConnection.EVENT_IPV6_ADDRESS_UPDATED) {
                MtkDataConnection.this.log("DcDefaultState: ignore EVENT_IPV6_ADDRESS_UPDATED not in ActiveState");
                return true;
            }
            if (i == 266254) {
                String[] apnType = MtkDataConnection.this.getApnType();
                if (MtkDataConnection.VDBG) {
                    MtkDataConnection.this.log("REQ_GET_APNTYPE  aryApnType=" + Arrays.toString(apnType));
                }
                MtkDataConnection.this.mAc.replyToMessage(message, MtkDcAsyncChannel.RSP_GET_APNTYPE, apnType);
                return true;
            }
            switch (i) {
                case MtkDataConnection.EVENT_IPV4_ADDRESS_REMOVED:
                    MtkDataConnection.this.log("DcDefaultState: ignore EVENT_IPV4_ADDRESS_REMOVED not in ActiveState");
                    break;
                case MtkDataConnection.EVENT_IPV6_ADDRESS_REMOVED:
                    MtkDataConnection.this.log("DcDefaultState: ignore EVENT_IPV6_ADDRESS_REMOVED not in ActiveState");
                    break;
                case MtkDataConnection.EVENT_ADDRESS_REMOVED:
                    MtkDataConnection.this.log("DcDefaultState: " + MtkDataConnection.this.getWhatToString(message.what));
                    break;
                case MtkDataConnection.EVENT_VOICE_CALL:
                    MtkDataConnection.this.mIsInVoiceCall = message.arg1 != 0;
                    MtkDataConnection.this.mIsSupportConcurrent = message.arg2 != 0;
                    break;
            }
            return super.processMessage(message);
        }
    }

    private class MtkDcActivatingState extends DataConnection.DcActivatingState {
        private MtkDcActivatingState() {
            super(MtkDataConnection.this);
        }

        public void enter() {
            MtkDataConnection.this.log("DcActivatingState: enter dc=" + MtkDataConnection.this);
            super.enter();
        }

        public void exit() {
            MtkDataConnection.this.log("DcActivatingState: exit dc=" + MtkDataConnection.this);
            super.exit();
        }

        public boolean processMessage(Message message) {
            MtkDataConnection.this.log("DcActivatingState: msg=" + MtkDataConnection.msgToString(message));
            switch (message.what) {
                case 262144:
                    DataConnection.ConnectionParams connectionParams = (DataConnection.ConnectionParams) message.obj;
                    MtkDataConnection.this.mApnContexts.put(connectionParams.mApnContext, connectionParams);
                    MtkDataConnection.this.log("DcActivatingState: mApnContexts size=" + MtkDataConnection.this.mApnContexts.size());
                    break;
                case 262145:
                    DataConnection.ConnectionParams connectionParams2 = (DataConnection.ConnectionParams) message.obj;
                    DataCallResponse parcelable = message.getData().getParcelable("data_call_response");
                    DataConnection.SetupResult setupResultOnSetupConnectionCompleted = MtkDataConnection.this.onSetupConnectionCompleted(message.arg1, parcelable, connectionParams2);
                    if (setupResultOnSetupConnectionCompleted != DataConnection.SetupResult.ERROR_STALE && MtkDataConnection.this.mConnectionParams != connectionParams2) {
                        MtkDataConnection.this.loge("DcActivatingState: WEIRD mConnectionsParams:" + MtkDataConnection.this.mConnectionParams + " != cp:" + connectionParams2);
                    }
                    MtkDataConnection.this.log("DcActivatingState onSetupConnectionCompleted result=" + setupResultOnSetupConnectionCompleted + " dc=" + MtkDataConnection.this);
                    if (connectionParams2.mApnContext != null) {
                        connectionParams2.mApnContext.requestLog("onSetupConnectionCompleted result=" + setupResultOnSetupConnectionCompleted);
                    }
                    switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$dataconnection$DataConnection$SetupResult[setupResultOnSetupConnectionCompleted.ordinal()]) {
                        case 1:
                            MtkDataConnection.this.mDcFailCause = DcFailCause.NONE;
                            MtkDataConnection.this.resetRetryCount();
                            MtkDataConnection.this.transitionTo(MtkDataConnection.this.mActiveState);
                            return true;
                        case 2:
                            MtkDataConnection.this.mInactiveState.setEnterNotificationParams(connectionParams2, setupResultOnSetupConnectionCompleted.mFailCause);
                            MtkDataConnection.this.transitionTo(MtkDataConnection.this.mInactiveState);
                            return true;
                        case 3:
                            MtkDataConnection.this.tearDownData(connectionParams2);
                            MtkDataConnection.this.transitionTo(MtkDataConnection.this.mDisconnectingErrorCreatingConnection);
                            return true;
                        case 4:
                            long suggestedRetryDelay = MtkDataConnection.this.getSuggestedRetryDelay(parcelable);
                            connectionParams2.mApnContext.setModemSuggestedDelay(suggestedRetryDelay);
                            String str = "DcActivatingState: ERROR_DATA_SERVICE_SPECIFIC_ERROR  delay=" + suggestedRetryDelay + " result=" + setupResultOnSetupConnectionCompleted + " result.isRestartRadioFail=" + setupResultOnSetupConnectionCompleted.mFailCause.isRestartRadioFail(MtkDataConnection.this.mPhone.getContext(), MtkDataConnection.this.mPhone.getSubId()) + " isPermanentFailure=" + MtkDataConnection.this.mDct.isPermanentFailure(setupResultOnSetupConnectionCompleted.mFailCause);
                            MtkDataConnection.this.log(str);
                            if (connectionParams2.mApnContext != null) {
                                connectionParams2.mApnContext.requestLog(str);
                            }
                            if (setupResultOnSetupConnectionCompleted.mFailCause == DcFailCause.MTK_PDP_FAIL_FALLBACK_RETRY) {
                                MtkDataConnection.this.onSetupFallbackConnection(parcelable, connectionParams2);
                                MtkDataConnection.this.mDcFailCause = DcFailCause.MTK_PDP_FAIL_FALLBACK_RETRY;
                                MtkDataConnection.this.deferMessage(MtkDataConnection.this.obtainMessage(MtkDataConnection.EVENT_FALLBACK_RETRY_CONNECTION, MtkDataConnection.this.mTag));
                                MtkDataConnection.this.transitionTo(MtkDataConnection.this.mActiveState);
                                return true;
                            }
                            MtkDataConnection.this.mInactiveState.setEnterNotificationParams(connectionParams2, setupResultOnSetupConnectionCompleted.mFailCause);
                            MtkDataConnection.this.transitionTo(MtkDataConnection.this.mInactiveState);
                            return true;
                        case 5:
                            MtkDataConnection.this.loge("DcActivatingState: stale EVENT_SETUP_DATA_CONNECTION_DONE tag:" + connectionParams2.mTag + " != mTag:" + MtkDataConnection.this.mTag);
                            return true;
                        default:
                            throw new RuntimeException("Unknown SetupResult, should not happen");
                    }
                case 262148:
                    DataConnection.DisconnectParams disconnectParams = (DataConnection.DisconnectParams) message.obj;
                    if (!MtkDataConnection.this.mApnContexts.containsKey(disconnectParams.mApnContext)) {
                        MtkDataConnection.this.log("DcActivatingState ERROR no such apnContext=" + disconnectParams.mApnContext + " in this dc=" + MtkDataConnection.this);
                        MtkDataConnection.this.notifyDisconnectCompleted(disconnectParams, false);
                        return true;
                    }
                    MtkDataConnection.this.deferMessage(message);
                    return true;
                case 262155:
                    break;
                case MtkDataConnection.EVENT_IPV4_ADDRESS_REMOVED:
                case MtkDataConnection.EVENT_IPV6_ADDRESS_REMOVED:
                case MtkDataConnection.EVENT_IPV6_ADDRESS_UPDATED:
                    MtkDataConnection.this.log("DcActivatingState deferMsg: " + MtkDataConnection.this.getWhatToString(message.what) + ", address info: " + ((AddressInfo) message.obj));
                    MtkDataConnection.this.deferMessage(message);
                    return true;
                default:
                    return super.processMessage(message);
            }
            MtkDataConnection.this.deferMessage(message);
            return true;
        }
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$internal$telephony$dataconnection$DataConnection$SetupResult = new int[DataConnection.SetupResult.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$dataconnection$DataConnection$SetupResult[DataConnection.SetupResult.SUCCESS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$dataconnection$DataConnection$SetupResult[DataConnection.SetupResult.ERROR_RADIO_NOT_AVAILABLE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$dataconnection$DataConnection$SetupResult[DataConnection.SetupResult.ERROR_INVALID_ARG.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$dataconnection$DataConnection$SetupResult[DataConnection.SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$dataconnection$DataConnection$SetupResult[DataConnection.SetupResult.ERROR_STALE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private class MtkDcActiveState extends DataConnection.DcActiveState {
        private MtkDcActiveState() {
            super(MtkDataConnection.this);
        }

        public void enter() {
            long j;
            boolean zCanHandleType;
            int phoneId = MtkDataConnection.this.mPhone.getPhoneId();
            int i = MtkDataConnection.this.mId;
            if (MtkDataConnection.this.mApnSetting != null) {
                j = MtkDataConnection.this.mApnSetting.typesBitmap;
            } else {
                j = 0;
            }
            long j2 = j;
            if (MtkDataConnection.this.mApnSetting != null) {
                zCanHandleType = MtkDataConnection.this.mApnSetting.canHandleType("default");
            } else {
                zCanHandleType = false;
            }
            StatsLog.write(75, 3, phoneId, i, j2, zCanHandleType);
            Iterator it = MtkDataConnection.this.mApnContexts.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ApnContext apnContext = ((DataConnection.ConnectionParams) it.next()).mApnContext;
                if (TextUtils.equals(apnContext.getApnType(), "default")) {
                    apnContext.setReason("connected");
                    apnContext.setState(DctConstants.State.CONNECTED);
                    MtkDataConnection.this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
                    MtkDataConnection.this.log("DcActiveState: notifyDataConnection in advance for default apn type.");
                    break;
                }
            }
            MtkDataConnection.this.updateNetworkInfo();
            MtkDataConnection.this.notifyAllOfConnected("connected");
            MtkDataConnection.this.mDcController.addActiveDcByCid(MtkDataConnection.this);
            String[] strArr = null;
            if (MtkDataConnection.this.isNwNeedSuspended()) {
                MtkDataConnection.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, MtkDataConnection.this.mNetworkInfo.getReason(), null);
            } else {
                MtkDataConnection.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, MtkDataConnection.this.mNetworkInfo.getReason(), null);
            }
            MtkDataConnection.this.mNetworkInfo.setExtraInfo(MtkDataConnection.this.mApnSetting.apn);
            MtkDataConnection.this.updateTcpBufferSizes(MtkDataConnection.this.mRilRat);
            NetworkMisc networkMisc = new NetworkMisc();
            if (MtkDataConnection.this.mPhone.getCarrierSignalAgent().hasRegisteredReceivers("com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED")) {
                networkMisc.provisioningNotificationDisabled = true;
            }
            networkMisc.subscriberId = MtkDataConnection.this.mPhone.getSubscriberId();
            MtkDataConnection.this.logi("DcActiveState: enter dc = " + MtkDataConnection.this + " mNetworkInfo = " + MtkDataConnection.this.mNetworkInfo);
            MtkDataConnection.this.setNetworkRestriction();
            MtkDataConnection.this.log("mRestrictedNetworkOverride = " + MtkDataConnection.this.mRestrictedNetworkOverride);
            MtkDataConnection.this.mNetworkAgent = new DataConnection.DcNetworkAgent(MtkDataConnection.this, MtkDataConnection.this.getHandler().getLooper(), MtkDataConnection.this.mPhone.getContext(), "DcNetworkAgent", MtkDataConnection.this.mNetworkInfo, MtkDataConnection.this.getNetworkCapabilities(), MtkDataConnection.this.mLinkProperties, 50, networkMisc);
            MtkDataConnection.this.mPhone.mCi.registerForNattKeepaliveStatus(MtkDataConnection.this.getHandler(), 262162, (Object) null);
            MtkDataConnection.this.mPhone.mCi.registerForLceInfo(MtkDataConnection.this.getHandler(), 262167, (Object) null);
            try {
                IDataConnectionExt iDataConnectionExt = MtkDataConnection.this.mDataConnectionExt;
                if (MtkDataConnection.this.mApnSetting != null) {
                    strArr = MtkDataConnection.this.mApnSetting.types;
                }
                iDataConnectionExt.onDcActivated(strArr, MtkDataConnection.this.mLinkProperties == null ? "" : MtkDataConnection.this.mLinkProperties.getInterfaceName());
            } catch (Exception e) {
                MtkDataConnection.this.loge("onDcActivated fail!");
                e.printStackTrace();
            }
        }

        public void exit() {
            try {
                MtkDataConnection.this.mDataConnectionExt.onDcDeactivated(MtkDataConnection.this.mApnSetting == null ? null : MtkDataConnection.this.mApnSetting.types, MtkDataConnection.this.mLinkProperties == null ? "" : MtkDataConnection.this.mLinkProperties.getInterfaceName());
            } catch (Exception e) {
                MtkDataConnection.this.loge("onDcDeactivated fail!");
                e.printStackTrace();
            }
            super.exit();
        }

        public boolean processMessage(Message message) {
            switch (message.what) {
                case 262144:
                    DataConnection.ConnectionParams connectionParams = (DataConnection.ConnectionParams) message.obj;
                    MtkDataConnection.this.mApnContexts.put(connectionParams.mApnContext, connectionParams);
                    MtkDataConnection.this.log("DcActiveState: EVENT_CONNECT cp=" + connectionParams + " dc=" + MtkDataConnection.this);
                    if (MtkDataConnection.this.mNetworkAgent != null) {
                        NetworkCapabilities networkCapabilities = MtkDataConnection.this.getNetworkCapabilities();
                        MtkDataConnection.this.mNetworkAgent.sendNetworkCapabilities(networkCapabilities);
                        MtkDataConnection.this.log("DcActiveState update Capabilities:" + networkCapabilities);
                    }
                    MtkDataConnection.this.checkIfDefaultApnReferenceCountChanged();
                    MtkDataConnection.this.notifyConnectCompleted(connectionParams, DcFailCause.NONE, false);
                    return true;
                case 262145:
                    DataConnection.ConnectionParams connectionParams2 = (DataConnection.ConnectionParams) message.obj;
                    DataConnection.SetupResult setupResultOnSetupConnectionCompleted = MtkDataConnection.this.onSetupConnectionCompleted(message.arg1, message.getData().getParcelable("data_call_response"), connectionParams2);
                    if (setupResultOnSetupConnectionCompleted != DataConnection.SetupResult.ERROR_STALE && MtkDataConnection.this.mConnectionParams != connectionParams2) {
                        MtkDataConnection.this.loge("DcActiveState_FALLBACK_Retry: WEIRD mConnectionsParams:" + MtkDataConnection.this.mConnectionParams + " != cp:" + connectionParams2);
                    }
                    MtkDataConnection.this.log("DcActiveState_FALLBACK_Retry onSetupConnectionCompleted result=" + setupResultOnSetupConnectionCompleted + " dc=" + MtkDataConnection.this);
                    int i = AnonymousClass3.$SwitchMap$com$android$internal$telephony$dataconnection$DataConnection$SetupResult[setupResultOnSetupConnectionCompleted.ordinal()];
                    if (i == 1) {
                        MtkDataConnection.this.mDcFailCause = DcFailCause.NONE;
                        MtkDataConnection.this.resetRetryCount();
                        return true;
                    }
                    switch (i) {
                        case 4:
                            MtkDataConnection.this.log("DcActiveState_FALLBACK_Retry: ERROR_DATA_SERVICE_SPECIFIC_ERROR result=" + setupResultOnSetupConnectionCompleted + " result.isRestartRadioFail=" + setupResultOnSetupConnectionCompleted.mFailCause.isRestartRadioFail(MtkDataConnection.this.mPhone.getContext(), MtkDataConnection.this.mPhone.getSubId()) + " result.isPermanentFailure=" + MtkDataConnection.this.mDct.isPermanentFailure(setupResultOnSetupConnectionCompleted.mFailCause));
                            if (setupResultOnSetupConnectionCompleted.mFailCause == DcFailCause.MTK_PDP_FAIL_FALLBACK_RETRY) {
                                if (MtkDataConnection.this.mDcFcMgr == null || !MtkDataConnection.this.mDcFcMgr.isSpecificNetworkAndSimOperator(DcFailCauseManager.Operator.OP19)) {
                                    return true;
                                }
                                MtkDataConnection.access$13108(MtkDataConnection.this);
                                long retryTimeByIndex = MtkDataConnection.this.mDcFcMgr.getRetryTimeByIndex(MtkDataConnection.this.mRetryCount, DcFailCauseManager.Operator.OP19);
                                if (retryTimeByIndex >= 0) {
                                    MtkDataConnection.this.mDcFailCause = DcFailCause.MTK_PDP_FAIL_FALLBACK_RETRY;
                                    MtkDataConnection.this.startRetryAlarm(MtkDataConnection.EVENT_FALLBACK_RETRY_CONNECTION, MtkDataConnection.this.mTag, retryTimeByIndex);
                                    return true;
                                }
                                MtkDataConnection.this.log("DcActiveState_FALLBACK_Retry: No retry but at least one IPv4 or IPv6 is accepted");
                                MtkDataConnection.this.mDcFailCause = DcFailCause.NONE;
                                return true;
                            }
                            MtkDataConnection.this.log("DcActiveState_FALLBACK_Retry: ERROR_DATA_SERVICE_SPECIFIC_ERROR Not retry anymore");
                            return true;
                        case 5:
                            MtkDataConnection.this.loge("DcActiveState_FALLBACK_Retry: stale EVENT_SETUP_DATA_CONNECTION_DONE tag:" + connectionParams2.mTag + " != mTag:" + MtkDataConnection.this.mTag + " Not retry anymore");
                            return true;
                        default:
                            MtkDataConnection.this.log("DcActiveState_FALLBACK_Retry: Another error cause, Not retry anymore");
                            return true;
                    }
                case 262148:
                    DataConnection.DisconnectParams disconnectParams = (DataConnection.DisconnectParams) message.obj;
                    MtkDataConnection.this.log("DcActiveState: EVENT_DISCONNECT dp=" + disconnectParams + " dc=" + MtkDataConnection.this);
                    if (MtkDataConnection.this.mApnContexts.containsKey(disconnectParams.mApnContext)) {
                        MtkDataConnection.this.log("DcActiveState msg.what=EVENT_DISCONNECT RefCount=" + MtkDataConnection.this.mApnContexts.size());
                        if (MtkDataConnection.this.mApnContexts.size() == 1) {
                            if (!MtkDataConnection.this.hasMdAutoSetupImsCapability()) {
                                MtkDataConnection.this.handlePcscfErrorCause(disconnectParams);
                            }
                            MtkDataConnection.this.mApnContexts.clear();
                            MtkDataConnection.this.mDisconnectParams = disconnectParams;
                            MtkDataConnection.this.mConnectionParams = null;
                            disconnectParams.mTag = MtkDataConnection.this.mTag;
                            MtkDataConnection.this.tearDownData(disconnectParams);
                            MtkDataConnection.this.transitionTo(MtkDataConnection.this.mDisconnectingState);
                            return true;
                        }
                        MtkDataConnection.this.mApnContexts.remove(disconnectParams.mApnContext);
                        if (MtkDataConnection.this.mNetworkAgent != null) {
                            NetworkCapabilities networkCapabilities2 = MtkDataConnection.this.getNetworkCapabilities();
                            MtkDataConnection.this.mNetworkAgent.sendNetworkCapabilities(networkCapabilities2);
                            MtkDataConnection.this.log("DcActiveState update Capabilities:" + networkCapabilities2);
                        }
                        MtkDataConnection.this.checkIfDefaultApnReferenceCountChanged();
                        MtkDataConnection.this.notifyDisconnectCompleted(disconnectParams, false);
                        return true;
                    }
                    MtkDataConnection.this.log("DcActiveState ERROR no such apnContext=" + disconnectParams.mApnContext + " in this dc=" + MtkDataConnection.this);
                    MtkDataConnection.this.notifyDisconnectCompleted(disconnectParams, false);
                    return true;
                case MtkDataConnection.EVENT_IPV4_ADDRESS_REMOVED:
                    AddressInfo addressInfo = (AddressInfo) message.obj;
                    MtkDataConnection.this.log("DcActiveState: " + MtkDataConnection.this.getWhatToString(message.what) + ": " + addressInfo);
                    return true;
                case MtkDataConnection.EVENT_IPV6_ADDRESS_REMOVED:
                    AddressInfo addressInfo2 = (AddressInfo) message.obj;
                    MtkDataConnection.this.log("DcActiveState: " + MtkDataConnection.this.getWhatToString(message.what) + ": " + addressInfo2);
                    if (MtkDataConnection.this.mInterfaceName != null && MtkDataConnection.this.mInterfaceName.equals(addressInfo2.mIntfName)) {
                        String hostAddress = addressInfo2.mLinkAddr.getAddress().getHostAddress();
                        MtkDataConnection.this.log("strAddress: " + hostAddress);
                        if (hostAddress.equalsIgnoreCase("FE80::5A:5A:5A:23")) {
                            MtkDataConnection.this.mValid = -1L;
                        } else if (hostAddress.equalsIgnoreCase("FE80::5A:5A:5A:22")) {
                            MtkDataConnection.this.mValid = -2L;
                        } else {
                            MtkDataConnection.this.mValid = -1000L;
                        }
                        if (MtkDataConnection.this.mValid == -1 || MtkDataConnection.this.mValid == -2) {
                            MtkDataConnection.this.log("DcActiveState: RA initial or refresh fail, valid:" + MtkDataConnection.this.mValid);
                            MtkDataConnection.this.onAddressRemoved();
                        }
                    }
                    if (MtkDataConnection.this.mGlobalV6AddrInfo == null || !MtkDataConnection.this.mGlobalV6AddrInfo.mIntfName.equals(addressInfo2.mIntfName)) {
                        return true;
                    }
                    MtkDataConnection.this.mGlobalV6AddrInfo = null;
                    return true;
                case MtkDataConnection.EVENT_VOICE_CALL:
                    MtkDataConnection.this.mIsInVoiceCall = message.arg1 != 0;
                    MtkDataConnection.this.mIsSupportConcurrent = message.arg2 != 0;
                    MtkDataConnection.this.updateNetworkInfoSuspendState();
                    if (MtkDataConnection.this.mNetworkAgent == null) {
                        return true;
                    }
                    MtkDataConnection.this.mNetworkAgent.sendNetworkInfo(MtkDataConnection.this.mNetworkInfo);
                    return true;
                case MtkDataConnection.EVENT_FALLBACK_RETRY_CONNECTION:
                    if (message.arg1 == MtkDataConnection.this.mTag) {
                        if (MtkDataConnection.this.mDataRegState != 0) {
                            MtkDataConnection.this.log("DcActiveState: EVENT_FALLBACK_RETRY_CONNECTION not in service");
                            return true;
                        }
                        MtkDataConnection.this.log("DcActiveState EVENT_FALLBACK_RETRY_CONNECTION mConnectionParams=" + MtkDataConnection.this.mConnectionParams);
                        MtkDataConnection.this.onConnect(MtkDataConnection.this.mConnectionParams);
                        return true;
                    }
                    MtkDataConnection.this.log("DcActiveState stale EVENT_FALLBACK_RETRY_CONNECTION tag:" + message.arg1 + " != mTag:" + MtkDataConnection.this.mTag);
                    return true;
                case MtkDataConnection.EVENT_IPV6_ADDRESS_UPDATED:
                    AddressInfo addressInfo3 = (AddressInfo) message.obj;
                    if (MtkDataConnection.this.mInterfaceName == null || !MtkDataConnection.this.mInterfaceName.equals(addressInfo3.mIntfName)) {
                        return true;
                    }
                    int scope = addressInfo3.mLinkAddr.getScope();
                    int flags = addressInfo3.mLinkAddr.getFlags();
                    MtkDataConnection.this.log("EVENT_IPV6_ADDRESS_UPDATED, scope: " + scope + ", flag: " + flags);
                    if (OsConstants.RT_SCOPE_UNIVERSE == scope && (flags & 1) != OsConstants.IFA_F_TEMPORARY && MtkDataConnection.this.mNetworkAgent != null) {
                        MtkDataConnection.this.mGlobalV6AddrInfo = addressInfo3;
                        MtkDataConnection.this.mNetworkAgent.sendLinkProperties(MtkDataConnection.this.getLinkProperties());
                        MtkDataConnection.this.log("EVENT_IPV6_ADDRESS_UPDATED, notify global ipv6 address update");
                        return true;
                    }
                    MtkDataConnection.this.log("EVENT_IPV6_ADDRESS_UPDATED, not notify global ipv6 address update");
                    return true;
                default:
                    return super.processMessage(message);
            }
        }
    }

    protected long getSuggestedRetryDelay(DataCallResponse dataCallResponse) {
        if (dataCallResponse.getSuggestedRetryTime() < 0) {
            log("No suggested retry delay.");
            DcFailCause dcFailCauseFromInt = DcFailCause.fromInt(dataCallResponse.getStatus());
            if (this.mDcFcMgr == null) {
                return -2L;
            }
            return this.mDcFcMgr.getSuggestedRetryDelayByOp(dcFailCauseFromInt);
        }
        if (dataCallResponse.getSuggestedRetryTime() == Integer.MAX_VALUE) {
            log("Modem suggested not retrying.");
            return -1L;
        }
        return dataCallResponse.getSuggestedRetryTime();
    }

    String[] getApnType() {
        log("getApnType: mApnContexts.size() = " + this.mApnContexts.size());
        if (this.mApnContexts.size() == 0) {
            return null;
        }
        String[] strArr = new String[this.mApnContexts.values().size()];
        int i = 0;
        Iterator it = this.mApnContexts.values().iterator();
        while (it.hasNext()) {
            String apnType = ((DataConnection.ConnectionParams) it.next()).mApnContext.getApnType();
            log("getApnType: apnType = " + apnType);
            strArr[i] = new String(apnType);
            i++;
        }
        return strArr;
    }

    private void notifyDefaultApnReferenceCountChanged(int i, int i2) {
        Message messageObtainMessage = this.mDct.obtainMessage(i2);
        messageObtainMessage.arg1 = i;
        AsyncResult.forMessage(messageObtainMessage);
        messageObtainMessage.sendToTarget();
    }

    private void onSetupFallbackConnection(DataCallResponse dataCallResponse, DataConnection.ConnectionParams connectionParams) {
        if (connectionParams.mTag != this.mTag) {
            log("onSetupFallbackConnection stale cp.tag=" + connectionParams.mTag + ", mtag=" + this.mTag);
            DataConnection.SetupResult setupResult = DataConnection.SetupResult.ERROR_STALE;
            return;
        }
        log("onSetupFallbackConnection received successful DataCallResponse");
        this.mCid = dataCallResponse.getCallId();
        this.mPcscfAddr = (String[]) dataCallResponse.getPcscfs().toArray(new String[dataCallResponse.getPcscfs().size()]);
        setConnectionRat(decodeRat(dataCallResponse.getActive()), "setup fallback");
        DataConnection.SetupResult setupResult2 = updateLinkProperty(dataCallResponse).setupResult;
        this.mInterfaceName = dataCallResponse.getIfname();
        log("onSetupFallbackConnection: ifname-" + this.mInterfaceName);
    }

    private boolean isAddCapabilityByDataOption() {
        boolean zIsUserDataEnabled = this.mDct.isUserDataEnabled();
        boolean dataRoamingEnabled = this.mDct.getDataRoamingEnabled();
        log("addCapabilityByDataOption");
        if (zIsUserDataEnabled) {
            return !this.mPhone.getServiceState().getDataRoaming() || dataRoamingEnabled;
        }
        return false;
    }

    private LinkProperties getLinkProperties() {
        if (this.mGlobalV6AddrInfo == null) {
            return this.mLinkProperties;
        }
        LinkProperties linkProperties = new LinkProperties(this.mLinkProperties);
        Iterator<LinkAddress> it = linkProperties.getLinkAddresses().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            LinkAddress next = it.next();
            if (next.getAddress() instanceof Inet6Address) {
                linkProperties.removeLinkAddress(next);
                break;
            }
        }
        linkProperties.addLinkAddress(this.mGlobalV6AddrInfo.mLinkAddr);
        return linkProperties;
    }

    private boolean isNwNeedSuspended() {
        boolean zIsWifiCallingEnabled;
        boolean zIsImsOrEmergencyApn = MtkDcHelper.isImsOrEmergencyApn(getApnType());
        if (this.mIsInVoiceCall) {
            zIsWifiCallingEnabled = MtkDcHelper.getInstance().isWifiCallingEnabled();
        } else {
            zIsWifiCallingEnabled = false;
        }
        log("isNwNeedSuspended: mIsInVoiceCall = " + this.mIsInVoiceCall + ", mIsSupportConcurrent = " + this.mIsSupportConcurrent + ", bImsOrEmergencyApn = " + zIsImsOrEmergencyApn + ", bWifiCallingEnabled = " + zIsWifiCallingEnabled);
        return (!this.mIsInVoiceCall || this.mIsSupportConcurrent || zIsImsOrEmergencyApn || zIsWifiCallingEnabled) ? false : true;
    }

    private int getEventByAddress(boolean z, LinkAddress linkAddress) {
        InetAddress address = linkAddress.getAddress();
        if (!z) {
            if (address instanceof Inet6Address) {
                return EVENT_IPV6_ADDRESS_REMOVED;
            }
            if (address instanceof Inet4Address) {
                return EVENT_IPV4_ADDRESS_REMOVED;
            }
            loge("unknown address type, linkAddr: " + linkAddress);
        } else {
            if (address instanceof Inet6Address) {
                return EVENT_IPV6_ADDRESS_UPDATED;
            }
            loge("unknown address type, linkAddr: " + linkAddress);
        }
        return -1;
    }

    private void sendMessageForSM(int i, String str, LinkAddress linkAddress) {
        if (i < 0) {
            loge("sendMessageForSM: Skip notify!!!");
            return;
        }
        AddressInfo addressInfo = new AddressInfo(str, linkAddress);
        log("sendMessageForSM: " + cmdToString(i) + ", addressInfo: " + addressInfo);
        sendMessage(obtainMessage(i, addressInfo));
    }

    private void onAddressRemoved() {
        if (("IPV6".equals(this.mApnSetting.protocol) || "IPV4V6".equals(this.mApnSetting.protocol)) && !isIpv4Connected()) {
            log("onAddressRemoved: IPv6 RA failed and didn't connect with IPv4");
            if (this.mApnContexts != null) {
                log("onAddressRemoved: mApnContexts size: " + this.mApnContexts.size());
                for (DataConnection.ConnectionParams connectionParams : this.mApnContexts.values()) {
                    ApnContext apnContext = connectionParams.mApnContext;
                    apnContext.getApnType();
                    if (apnContext.getState() == DctConstants.State.CONNECTED) {
                        log("onAddressRemoved: send message EVENT_DISCONNECT_ALL");
                        sendMessage(obtainMessage(262150, new DataConnection.DisconnectParams(apnContext, MtkGsmCdmaPhone.REASON_RA_FAILED, this.mDct.obtainMessage(270351, new Pair(apnContext, Integer.valueOf(connectionParams.mConnectionGeneration))))));
                        return;
                    }
                }
                return;
            }
            return;
        }
        log("onAddressRemoved: no need to remove");
    }

    void checkIfDefaultApnReferenceCountChanged() {
        Iterator it = this.mApnContexts.values().iterator();
        boolean z = false;
        int i = 0;
        while (it.hasNext()) {
            ApnContext apnContext = ((DataConnection.ConnectionParams) it.next()).mApnContext;
            if (!TextUtils.equals("default", apnContext.getApnType()) || !DctConstants.State.CONNECTED.equals(apnContext.getState())) {
                if (DctConstants.State.CONNECTED.equals(apnContext.getState())) {
                    i++;
                }
            } else {
                z = true;
            }
        }
        if (z) {
            log("refCount = " + this.mApnContexts.size() + ", non-default refCount = " + i);
            notifyDefaultApnReferenceCountChanged(i + 1, 270848);
        }
    }

    private boolean isDefaultDataSubPhone(Phone phone) {
        int phoneId = this.mSubController.getPhoneId(this.mSubController.getDefaultDataSubId());
        int phoneId2 = phone.getPhoneId();
        if (phoneId != phoneId2) {
            log("Current phone is not default phone: curPhoneId = " + phoneId2 + ", defaultDataPhoneId = " + phoneId);
            return false;
        }
        return true;
    }

    private void registerNetworkAlertObserver() {
        if (this.mNetworkManager != null) {
            log("registerNetworkAlertObserver X");
            try {
                this.mNetworkManager.registerObserver(this.mAlertObserver);
                log("registerNetworkAlertObserver E");
            } catch (RemoteException e) {
                loge("registerNetworkAlertObserver failed E");
            }
        }
    }

    private void unregisterNetworkAlertObserver() {
        if (this.mNetworkManager != null) {
            log("unregisterNetworkAlertObserver X");
            try {
                this.mNetworkManager.unregisterObserver(this.mAlertObserver);
                log("unregisterNetworkAlertObserver E");
            } catch (RemoteException e) {
                loge("unregisterNetworkAlertObserver failed E");
            }
            this.mInterfaceName = null;
        }
    }

    private class AddressInfo {
        String mIntfName;
        LinkAddress mLinkAddr;

        public AddressInfo(String str, LinkAddress linkAddress) {
            this.mIntfName = str;
            this.mLinkAddr = linkAddress;
        }

        public String toString() {
            return "interfaceName: " + this.mIntfName + "/" + this.mLinkAddr;
        }
    }

    public void startRetryAlarm(int i, int i2, long j) {
        Intent intent = new Intent(this.mActionRetry);
        intent.putExtra(INTENT_RETRY_ALARM_WHAT, i);
        intent.putExtra(INTENT_RETRY_ALARM_TAG, i2);
        log("startRetryAlarm: next attempt in " + (j / 1000) + "s what=" + i + " tag=" + i2);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + j, PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728));
    }

    public void startRetryAlarmExact(int i, int i2, long j) {
        Intent intent = new Intent(this.mActionRetry);
        intent.addFlags(268435456);
        intent.putExtra(INTENT_RETRY_ALARM_WHAT, i);
        intent.putExtra(INTENT_RETRY_ALARM_TAG, i2);
        log("startRetryAlarmExact: next attempt in " + (j / 1000) + "s what=" + i + " tag=" + i2);
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + j, PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728));
    }

    private void resetRetryCount() {
        this.mRetryCount = 0;
        log("resetRetryCount: " + this.mRetryCount);
    }

    public void handlePcscfErrorCause(DataConnection.DisconnectParams disconnectParams) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        int subId = this.mPhone.getSubId();
        if (carrierConfigManager == null) {
            loge("handlePcscfErrorCause() null configMgr!");
            return;
        }
        PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(subId);
        if (configForSubId == null) {
            loge("handlePcscfErrorCause() null config!");
            return;
        }
        boolean z = configForSubId.getBoolean("ims_pdn_sync_fail_cause_to_modem_bool");
        log("handlePcscfErrorCause() syncFailCause: " + z + ", subId: " + subId);
        if (z && TextUtils.equals(disconnectParams.mApnContext.getApnType(), "ims")) {
            if (this.mPcscfAddr == null || this.mPcscfAddr.length <= 0) {
                disconnectParams.mReason = MtkGsmCdmaPhone.REASON_PCSCF_ADDRESS_FAILED;
                log("Disconnect with empty P-CSCF address");
            }
        }
    }

    private boolean hasMdAutoSetupImsCapability() {
        if (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasMdAutoSetupImsCapability()) {
            return false;
        }
        log("hasMdAutoSetupImsCapability: true");
        return true;
    }

    void setConnectionRat(int i, String str) {
        if (i + 1 > 4 || i < 0) {
            loge("setConnectionRat invalid newRat: " + i);
            return;
        }
        log("setConnectionRat newRat: " + i + " mRat: " + this.mRat + " reason: " + str);
        this.mRat = i;
    }

    int decodeRat(int i) {
        if (i < 0) {
            loge("decodeRat invalid param: " + i);
            return -1;
        }
        return (i / 1000) + 1;
    }

    protected void mtkReplaceStates() {
        this.mDefaultState = new MtkDcDefaultState();
        this.mActivatingState = new MtkDcActivatingState();
        this.mActiveState = new MtkDcActiveState();
    }

    protected LinkProperties mtkGetLinkProperties() {
        return getLinkProperties();
    }

    protected void mtkSetApnContextReason(ApnContext apnContext, String str) {
        Iterator it = this.mApnContexts.values().iterator();
        while (it.hasNext()) {
            ApnContext apnContext2 = ((DataConnection.ConnectionParams) it.next()).mApnContext;
            if (apnContext2 == apnContext && MtkGsmCdmaPhone.REASON_RA_FAILED.equals(str)) {
                log("set reason:" + str);
                apnContext2.setReason(str);
            }
        }
    }

    protected void mtkCheckDefaultApnRefCount(ApnContext apnContext) {
        if (!this.mApnContexts.containsKey(apnContext)) {
            checkIfDefaultApnReferenceCountChanged();
        }
    }
}
