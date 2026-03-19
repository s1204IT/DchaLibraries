package com.android.internal.telephony.dataconnection;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.INetworkManagementEventObserver;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.ProxyInfo;
import android.os.AsyncResult;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Patterns;
import android.util.TimeUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyEventLog;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.dataconnection.DataCallResponse;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.net.BaseNetworkObserver;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IGsmDCTExt;
import com.mediatek.internal.telephony.ImsSwitchController;
import com.mediatek.internal.telephony.dataconnection.DcFailCauseManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class DataConnection extends StateMachine {
    static final int BASE = 262144;
    private static final boolean BSP_PACKAGE;
    private static final int CMD_TO_STRING_COUNT = 21;
    private static final boolean DBG = true;
    static final int EVENT_ADDRESS_REMOVED = 262162;
    static final int EVENT_BW_REFRESH_RESPONSE = 262158;
    static final int EVENT_CONNECT = 262144;
    static final int EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED = 262155;
    static final int EVENT_DATA_CONNECTION_ROAM_OFF = 262157;
    static final int EVENT_DATA_CONNECTION_ROAM_ON = 262156;
    static final int EVENT_DATA_STATE_CHANGED = 262151;
    static final int EVENT_DATA_STATE_CHANGED_FOR_LOADED = 262159;
    static final int EVENT_DEACTIVATE_DONE = 262147;
    static final int EVENT_DISCONNECT = 262148;
    static final int EVENT_DISCONNECT_ALL = 262150;
    static final int EVENT_FALLBACK_RETRY_CONNECTION = 262164;
    static final int EVENT_GET_LAST_FAIL_DONE = 262146;
    static final int EVENT_IPV4_ADDRESS_REMOVED = 262160;
    static final int EVENT_IPV6_ADDRESS_REMOVED = 262161;
    static final int EVENT_LOST_CONNECTION = 262153;
    static final int EVENT_RIL_CONNECTED = 262149;
    static final int EVENT_SETUP_DATA_CONNECTION_DONE = 262145;
    static final int EVENT_TEAR_DOWN_NOW = 262152;
    static final int EVENT_VOICE_CALL = 262163;
    private static final String INTENT_RETRY_ALARM_TAG = "tag";
    private static final String INTENT_RETRY_ALARM_WHAT = "what";
    private static final String NETWORK_TYPE = "MOBILE";
    private static final String NULL_IP = "0.0.0.0";
    private static final int RA_GET_IPV6_VALID_FAIL = -1000;
    private static final int RA_INITIAL_FAIL = -1;
    private static final int RA_LIFE_TIME_EXPIRED = 0;
    private static final int RA_REFRESH_FAIL = -2;
    private static final String TCP_BUFFER_SIZES_1XRTT = "16384,32768,131072,4096,16384,102400";
    private static final String TCP_BUFFER_SIZES_EDGE = "4093,26280,70800,4096,16384,70800";
    private static final String TCP_BUFFER_SIZES_EHRPD = "131072,262144,1048576,4096,16384,524288";
    private static final String TCP_BUFFER_SIZES_EVDO = "4094,87380,262144,4096,16384,262144";
    private static final String TCP_BUFFER_SIZES_GPRS = "4092,8760,48000,4096,8760,48000";
    private static final String TCP_BUFFER_SIZES_HSDPA = "61167,367002,1101005,8738,52429,262114";
    private static final String TCP_BUFFER_SIZES_HSPA = "40778,244668,734003,16777,100663,301990";
    private static final String TCP_BUFFER_SIZES_HSPAP = "122334,734003,2202010,32040,192239,576717";
    private static final String TCP_BUFFER_SIZES_LTE = "2097152,4194304,8388608,262144,524288,1048576";
    private static final String TCP_BUFFER_SIZES_UMTS = "58254,349525,1048576,58254,349525,1048576";
    private static final boolean VDBG;
    private static AtomicInteger mInstanceNumber;
    private static String[] sCmdToString;
    private AsyncChannel mAc;
    private String mActionRetry;
    private DcActivatingState mActivatingState;
    private DcActiveState mActiveState;
    private AlarmManager mAlarmManager;
    private INetworkManagementEventObserver mAlertObserver;
    public HashMap<ApnContext, ConnectionParams> mApnContexts;
    private ApnSetting mApnSetting;
    public int mCid;
    private ConnectionParams mConnectionParams;
    private long mCreateTime;
    private int mDataRegState;
    private DcController mDcController;
    private DcFailCause mDcFailCause;
    protected DcFailCauseManager mDcFcMgr;
    private DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    private DcTracker mDct;
    private DcDefaultState mDefaultState;
    private DisconnectParams mDisconnectParams;
    private DcDisconnectionErrorCreatingConnection mDisconnectingErrorCreatingConnection;
    private DcDisconnectingState mDisconnectingState;
    private IGsmDCTExt mGsmDCTExt;
    private int mId;
    private DcInactiveState mInactiveState;
    private BroadcastReceiver mIntentReceiver;
    private String mInterfaceName;
    private boolean mIsInVoiceCall;
    private boolean mIsSupportConcurrent;
    private DcFailCause mLastFailCause;
    private long mLastFailTime;
    private LinkProperties mLinkProperties;
    private NetworkAgent mNetworkAgent;
    private NetworkInfo mNetworkInfo;
    private final INetworkManagementService mNetworkManager;
    protected String[] mPcscfAddr;
    private Phone mPhone;
    int mRat;
    PendingIntent mReconnectIntent;
    private int mRetryCount;
    private int mRilRat;
    private SubscriptionController mSubController;
    int mTag;
    private Object mUserData;
    private long mValid;

    static {
        VDBG = SystemProperties.get("ro.build.type").equals("eng");
        mInstanceNumber = new AtomicInteger(0);
        BSP_PACKAGE = SystemProperties.getBoolean("ro.mtk_bsp_package", false);
        sCmdToString = new String[21];
        sCmdToString[0] = "EVENT_CONNECT";
        sCmdToString[1] = "EVENT_SETUP_DATA_CONNECTION_DONE";
        sCmdToString[2] = "EVENT_GET_LAST_FAIL_DONE";
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
        sCmdToString[15] = "EVENT_DATA_STATE_CHANGED_FOR_LOADED";
        sCmdToString[16] = "EVENT_IPV4_ADDRESS_REMOVED";
        sCmdToString[17] = "EVENT_IPV6_ADDRESS_REMOVED";
        sCmdToString[18] = "EVENT_ADDRESS_REMOVED";
        sCmdToString[19] = "EVENT_VOICE_CALL";
        sCmdToString[20] = "EVENT_FALLBACK_RETRY_CONNECTION";
    }

    public static class ConnectionParams {
        ApnContext mApnContext;
        final int mConnectionGeneration;
        Message mOnCompletedMsg;
        int mProfileId;
        int mRilRat;
        int mTag;

        ConnectionParams(ApnContext apnContext, int profileId, int rilRadioTechnology, Message onCompletedMsg, int connectionGeneration) {
            this.mApnContext = apnContext;
            this.mProfileId = profileId;
            this.mRilRat = rilRadioTechnology;
            this.mOnCompletedMsg = onCompletedMsg;
            this.mConnectionGeneration = connectionGeneration;
        }

        public String toString() {
            return "{mTag=" + this.mTag + " mApnContext=" + this.mApnContext + " mProfileId=" + this.mProfileId + " mRat=" + this.mRilRat + " mOnCompletedMsg=" + DataConnection.msgToString(this.mOnCompletedMsg) + "}";
        }
    }

    public static class DisconnectParams {
        public ApnContext mApnContext;
        Message mOnCompletedMsg;
        String mReason;
        int mTag;

        DisconnectParams(ApnContext apnContext, String reason, Message onCompletedMsg) {
            this.mApnContext = apnContext;
            this.mReason = reason;
            this.mOnCompletedMsg = onCompletedMsg;
        }

        public String toString() {
            return "{mTag=" + this.mTag + " mApnContext=" + this.mApnContext + " mReason=" + this.mReason + " mOnCompletedMsg=" + DataConnection.msgToString(this.mOnCompletedMsg) + "}";
        }
    }

    static String cmdToString(int cmd) {
        String value;
        int cmd2 = cmd - SmsEnvelope.TELESERVICE_MWI;
        if (cmd2 >= 0 && cmd2 < sCmdToString.length) {
            value = sCmdToString[cmd2];
        } else {
            value = DcAsyncChannel.cmdToString(cmd2 + SmsEnvelope.TELESERVICE_MWI);
        }
        if (value == null) {
            String value2 = "0x" + Integer.toHexString(cmd2 + SmsEnvelope.TELESERVICE_MWI);
            return value2;
        }
        return value;
    }

    public static DataConnection makeDataConnection(Phone phone, int id, DcTracker dct, DcTesterFailBringUpAll failBringUpAll, DcController dcc) {
        DataConnection dc = new DataConnection(phone, "DC-" + mInstanceNumber.incrementAndGet(), id, dct, failBringUpAll, dcc);
        dc.start();
        dc.log("Made " + dc.getName());
        return dc;
    }

    void dispose() {
        log("dispose: call quiteNow()");
        quitNow();
    }

    NetworkCapabilities getCopyNetworkCapabilities() {
        return makeNetworkCapabilities();
    }

    LinkProperties getCopyLinkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    boolean getIsInactive() {
        return getCurrentState() == this.mInactiveState;
    }

    int getCid() {
        return this.mCid;
    }

    ApnSetting getApnSetting() {
        return this.mApnSetting;
    }

    String[] getApnType() {
        log("getApnType: mApnContexts.size() = " + this.mApnContexts.size());
        if (this.mApnContexts.size() == 0) {
            return null;
        }
        String[] aryApnType = new String[this.mApnContexts.values().size()];
        int i = 0;
        for (ConnectionParams cp : this.mApnContexts.values()) {
            ApnContext apnContext = cp.mApnContext;
            String apnType = apnContext.getApnType();
            log("getApnType: apnType = " + apnType);
            aryApnType[i] = new String(apnType);
            i++;
        }
        return aryApnType;
    }

    void setLinkPropertiesHttpProxy(ProxyInfo proxy) {
        this.mLinkProperties.setHttpProxy(proxy);
    }

    public static class UpdateLinkPropertyResult {
        public LinkProperties newLp;
        public LinkProperties oldLp;
        public DataCallResponse.SetupResult setupResult = DataCallResponse.SetupResult.SUCCESS;

        public UpdateLinkPropertyResult(LinkProperties curLp) {
            this.oldLp = curLp;
            this.newLp = curLp;
        }
    }

    public boolean isIpv4Connected() {
        Collection<InetAddress> addresses = this.mLinkProperties.getAddresses();
        for (InetAddress addr : addresses) {
            log("isIpv4Connected(), addr:" + addr);
            if (addr instanceof Inet4Address) {
                Inet4Address i4addr = (Inet4Address) addr;
                log("isAnyLocalAddress:" + i4addr.isAnyLocalAddress() + "/isLinkLocalAddress()" + i4addr.isLinkLocalAddress() + "/isLoopbackAddress()" + i4addr.isLoopbackAddress() + "/isMulticastAddress()" + i4addr.isMulticastAddress());
                if (!i4addr.isAnyLocalAddress() && !i4addr.isLinkLocalAddress() && !i4addr.isLoopbackAddress() && !i4addr.isMulticastAddress()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isIpv6Connected() {
        Collection<InetAddress> addresses = this.mLinkProperties.getAddresses();
        for (InetAddress addr : addresses) {
            log("isIpv6Connected(), addr:" + addr);
            if (addr instanceof Inet6Address) {
                Inet6Address i6addr = (Inet6Address) addr;
                if (!i6addr.isAnyLocalAddress() && !i6addr.isLinkLocalAddress() && !i6addr.isLoopbackAddress() && !i6addr.isMulticastAddress()) {
                    return true;
                }
            }
        }
        return false;
    }

    public UpdateLinkPropertyResult updateLinkProperty(DataCallResponse newState) {
        UpdateLinkPropertyResult result = new UpdateLinkPropertyResult(this.mLinkProperties);
        if (newState == null) {
            return result;
        }
        result.newLp = new LinkProperties();
        result.setupResult = setLinkProperties(newState, result.newLp);
        if (result.setupResult != DataCallResponse.SetupResult.SUCCESS) {
            log("updateLinkProperty failed : " + result.setupResult);
            return result;
        }
        result.newLp.setHttpProxy(this.mLinkProperties.getHttpProxy());
        checkSetMtu(this.mApnSetting, result.newLp);
        this.mLinkProperties = result.newLp;
        updateTcpBufferSizes(this.mRilRat);
        if (!result.oldLp.equals(result.newLp)) {
            log("updateLinkProperty old LP=" + result.oldLp);
            log("updateLinkProperty new LP=" + result.newLp);
        }
        if (!result.newLp.equals(result.oldLp) && this.mNetworkAgent != null) {
            this.mNetworkAgent.sendLinkProperties(this.mLinkProperties);
        }
        return result;
    }

    private void checkSetMtu(ApnSetting apn, LinkProperties lp) {
        if (lp == null || apn == null || lp == null) {
            return;
        }
        if (lp.getMtu() != 0) {
            log("MTU set by call response to: " + lp.getMtu());
            return;
        }
        if (apn != null && apn.mtu != 0) {
            lp.setMtu(apn.mtu);
            log("MTU set by APN to: " + apn.mtu);
            return;
        }
        int mtu = this.mPhone.getContext().getResources().getInteger(R.integer.config_doubleTapPowerGestureMultiTargetDefaultAction);
        if (mtu == 0) {
            return;
        }
        lp.setMtu(mtu);
        log("MTU set by config resource to: " + mtu);
    }

    private DataConnection(Phone phone, String str, int i, DcTracker dcTracker, DcTesterFailBringUpAll dcTesterFailBringUpAll, DcController dcController) {
        super(str, dcController.getHandler());
        this.mDct = null;
        this.mSubController = SubscriptionController.getInstance();
        this.mRetryCount = 0;
        this.mInterfaceName = null;
        this.mLinkProperties = new LinkProperties();
        this.mRilRat = Integer.MAX_VALUE;
        this.mDataRegState = Integer.MAX_VALUE;
        this.mIsInVoiceCall = false;
        this.mIsSupportConcurrent = false;
        this.mApnContexts = null;
        this.mReconnectIntent = null;
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (TextUtils.isEmpty(action)) {
                    DataConnection.this.log("onReceive: ignore empty action='" + action + "'");
                    return;
                }
                if (!TextUtils.equals(action, DataConnection.this.mActionRetry)) {
                    DataConnection.this.log("onReceive: unknown action=" + action);
                    return;
                }
                if (!intent.hasExtra(DataConnection.INTENT_RETRY_ALARM_WHAT)) {
                    throw new RuntimeException(DataConnection.this.mActionRetry + " has no INTENT_RETRY_ALRAM_WHAT");
                }
                if (!intent.hasExtra(DataConnection.INTENT_RETRY_ALARM_TAG)) {
                    throw new RuntimeException(DataConnection.this.mActionRetry + " has no INTENT_RETRY_ALRAM_TAG");
                }
                int what = intent.getIntExtra(DataConnection.INTENT_RETRY_ALARM_WHAT, Integer.MAX_VALUE);
                int tag = intent.getIntExtra(DataConnection.INTENT_RETRY_ALARM_TAG, Integer.MAX_VALUE);
                DataConnection.this.log("onReceive: action=" + action + " sendMessage(what:" + DataConnection.this.getWhatToString(what) + ", tag:" + tag + ")");
                DataConnection.this.sendMessage(DataConnection.this.obtainMessage(what, tag, 0));
            }
        };
        this.mDefaultState = new DcDefaultState(this, null);
        this.mInactiveState = new DcInactiveState(this, 0 == true ? 1 : 0);
        this.mActivatingState = new DcActivatingState(this, 0 == true ? 1 : 0);
        this.mActiveState = new DcActiveState(this, 0 == true ? 1 : 0);
        this.mDisconnectingState = new DcDisconnectingState(this, 0 == true ? 1 : 0);
        this.mDisconnectingErrorCreatingConnection = new DcDisconnectionErrorCreatingConnection(this, 0 == true ? 1 : 0);
        this.mAlertObserver = new BaseNetworkObserver() {
            public void addressRemoved(String iface, LinkAddress address) {
                int event = DataConnection.this.getEventByAddress(false, address);
                DataConnection.this.sendMessageForSM(event, iface, address);
            }
        };
        setLogRecSize(300);
        setLogOnlyTransitions(true);
        log("DataConnection created");
        this.mPhone = phone;
        this.mDct = dcTracker;
        this.mDcTesterFailBringUpAll = dcTesterFailBringUpAll;
        this.mDcController = dcController;
        this.mId = i;
        this.mCid = -1;
        this.mRat = 1;
        ServiceState serviceState = this.mPhone.getServiceState();
        this.mRilRat = serviceState.getRilDataRadioTechnology();
        this.mDataRegState = this.mPhone.getServiceState().getDataRegState();
        int dataNetworkType = serviceState.getDataNetworkType();
        this.mNetworkInfo = new NetworkInfo(0, dataNetworkType, NETWORK_TYPE, TelephonyManager.getNetworkTypeName(dataNetworkType));
        this.mNetworkInfo.setRoaming(serviceState.getDataRoaming());
        this.mNetworkInfo.setIsAvailable(true);
        if (!BSP_PACKAGE) {
            try {
                this.mGsmDCTExt = (IGsmDCTExt) MPlugin.createInstance(IGsmDCTExt.class.getName(), this.mPhone.getContext());
            } catch (Exception e) {
                log("mGsmDCTExt init fail");
                e.printStackTrace();
            }
        }
        addState(this.mDefaultState);
        addState(this.mInactiveState, this.mDefaultState);
        addState(this.mActivatingState, this.mDefaultState);
        addState(this.mActiveState, this.mDefaultState);
        addState(this.mDisconnectingState, this.mDefaultState);
        addState(this.mDisconnectingErrorCreatingConnection, this.mDefaultState);
        setInitialState(this.mInactiveState);
        this.mApnContexts = new HashMap<>();
        log("get INetworkManagementService");
        this.mNetworkManager = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        this.mDcFcMgr = DcFailCauseManager.getInstance(this.mPhone);
        this.mAlarmManager = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        this.mActionRetry = getClass().getCanonicalName() + "." + getName() + ".action_retry";
        resetRetryCount();
    }

    private void onConnect(ConnectionParams cp) {
        log("onConnect: carrier='" + this.mApnSetting.carrier + "' APN='" + this.mApnSetting.apn + "' proxy='" + this.mApnSetting.proxy + "' port='" + this.mApnSetting.port + "'");
        if (cp.mApnContext != null) {
            cp.mApnContext.requestLog("DataConnection.onConnect");
        }
        if (this.mDcTesterFailBringUpAll.getDcFailBringUp().mCounter <= 0) {
            this.mCreateTime = -1L;
            this.mLastFailTime = -1L;
            this.mLastFailCause = DcFailCause.NONE;
            Message msg = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, cp);
            msg.obj = cp;
            int authType = this.mApnSetting.authType;
            if (authType == -1) {
                authType = TextUtils.isEmpty(this.mApnSetting.user) ? 0 : 3;
            }
            String protocol = this.mPhone.getServiceState().getDataRoamingFromRegistration() ? this.mApnSetting.roamingProtocol : this.mApnSetting.protocol;
            this.mPhone.mCi.setupDataCall(cp.mRilRat, cp.mProfileId, this.mApnSetting.apn, this.mApnSetting.user, this.mApnSetting.password, authType, protocol, this.mId + 1, msg);
            return;
        }
        DataCallResponse response = new DataCallResponse();
        response.version = this.mPhone.mCi.getRilVersion();
        response.status = this.mDcTesterFailBringUpAll.getDcFailBringUp().mFailCause.getErrorCode();
        response.cid = 0;
        response.active = 0;
        response.type = UsimPBMemInfo.STRING_NOT_SET;
        response.ifname = UsimPBMemInfo.STRING_NOT_SET;
        response.addresses = new String[0];
        response.dnses = new String[0];
        response.gateways = new String[0];
        response.suggestedRetryTime = this.mDcTesterFailBringUpAll.getDcFailBringUp().mSuggestedRetryTime;
        response.pcscf = new String[0];
        response.mtu = 0;
        Message msg2 = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, cp);
        AsyncResult.forMessage(msg2, response, (Throwable) null);
        sendMessage(msg2);
        log("onConnect: FailBringUpAll=" + this.mDcTesterFailBringUpAll.getDcFailBringUp() + " send error response=" + response);
        DcFailBringUp dcFailBringUp = this.mDcTesterFailBringUpAll.getDcFailBringUp();
        dcFailBringUp.mCounter--;
    }

    private void tearDownData(Object o) {
        int discReason = 0;
        ApnContext apnContext = null;
        if (o != null && (o instanceof DisconnectParams)) {
            DisconnectParams dp = (DisconnectParams) o;
            apnContext = dp.mApnContext;
            if (TextUtils.equals(dp.mReason, PhoneInternalInterface.REASON_RADIO_TURNED_OFF)) {
                discReason = 1;
            } else if (TextUtils.equals(dp.mReason, PhoneInternalInterface.REASON_PDP_RESET)) {
                discReason = 2;
            } else if (TextUtils.equals(dp.mReason, PhoneInternalInterface.REASON_RA_FAILED)) {
                if (this.mValid == 0) {
                    discReason = TelephonyEventLog.TAG_IMS_CALL_START;
                } else if (this.mValid == -1) {
                    discReason = TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE;
                } else if (this.mValid == -2) {
                    discReason = TelephonyEventLog.TAG_IMS_CALL_ACCEPT;
                }
            } else if (TextUtils.equals(dp.mReason, PhoneInternalInterface.REASON_PCSCF_ADDRESS_FAILED)) {
                discReason = TelephonyEventLog.TAG_IMS_CALL_RECEIVE;
            }
        }
        if (this.mPhone.mCi.getRadioState().isOn() || this.mPhone.getServiceState().getRilDataRadioTechnology() == 18) {
            log("tearDownData radio is on, call deactivateDataCall");
            if (apnContext != null) {
                apnContext.requestLog("tearDownData radio is on, call deactivateDataCall");
            }
            this.mPhone.mCi.deactivateDataCall(this.mCid, discReason, obtainMessage(EVENT_DEACTIVATE_DONE, this.mTag, 0, o));
            return;
        }
        if (apnContext != null) {
            log("tearDownData: check PDN type");
            String apnType = apnContext.getApnType();
            if (apnContext.getState() == DctConstants.State.CONNECTED && (TextUtils.equals(apnType, ImsSwitchController.IMS_SERVICE) || TextUtils.equals(apnType, "emergency"))) {
                log("tearDownData: ims pdn");
                log("tearDownData radio is on, call deactivateDataCall");
                if (apnContext != null) {
                    apnContext.requestLog("tearDownData radio is on, call deactivateDataCall");
                }
                this.mPhone.mCi.deactivateDataCall(this.mCid, discReason, obtainMessage(EVENT_DEACTIVATE_DONE, this.mTag, 0, o));
                return;
            }
        }
        log("tearDownData radio is off sendMessage EVENT_DEACTIVATE_DONE immediately");
        if (apnContext != null) {
            apnContext.requestLog("tearDownData radio is off sendMessage EVENT_DEACTIVATE_DONE immediately");
        }
        AsyncResult ar = new AsyncResult(o, (Object) null, (Throwable) null);
        sendMessage(obtainMessage(EVENT_DEACTIVATE_DONE, this.mTag, 0, ar));
    }

    private void notifyAllWithEvent(ApnContext alreadySent, int event, String reason) {
        this.mNetworkInfo.setDetailedState(this.mNetworkInfo.getDetailedState(), reason, this.mNetworkInfo.getExtraInfo());
        for (ConnectionParams cp : this.mApnContexts.values()) {
            ApnContext apnContext = cp.mApnContext;
            if (apnContext != alreadySent) {
                if (reason != null) {
                    apnContext.setReason(reason);
                }
                Pair<ApnContext, Integer> pair = new Pair<>(apnContext, Integer.valueOf(cp.mConnectionGeneration));
                Message msg = this.mDct.obtainMessage(event, pair);
                AsyncResult.forMessage(msg);
                msg.sendToTarget();
            }
        }
    }

    private void notifyAllOfConnected(String reason) {
        notifyAllWithEvent(null, 270336, reason);
    }

    private void notifyAllOfDisconnectDcRetrying(String reason) {
        notifyAllWithEvent(null, 270370, reason);
    }

    private void notifyAllDisconnectCompleted(DcFailCause cause) {
        notifyAllWithEvent(null, 270351, cause.toString());
    }

    private void notifyDefaultApnReferenceCountChanged(int refCount, int event) {
        Message msg = this.mDct.obtainMessage(event);
        msg.arg1 = refCount;
        AsyncResult.forMessage(msg);
        msg.sendToTarget();
    }

    private void notifyConnectCompleted(ConnectionParams cp, DcFailCause cause, boolean sendAll) {
        ApnContext alreadySent = null;
        if (cp != null && cp.mOnCompletedMsg != null) {
            Message connectionCompletedMsg = cp.mOnCompletedMsg;
            cp.mOnCompletedMsg = null;
            alreadySent = cp.mApnContext;
            long timeStamp = System.currentTimeMillis();
            connectionCompletedMsg.arg1 = this.mCid;
            if (cause == DcFailCause.NONE) {
                this.mCreateTime = timeStamp;
                AsyncResult.forMessage(connectionCompletedMsg);
            } else {
                this.mLastFailCause = cause;
                this.mLastFailTime = timeStamp;
                if (cause == null) {
                    cause = DcFailCause.UNKNOWN;
                }
                AsyncResult.forMessage(connectionCompletedMsg, cause, new Throwable(cause.toString()));
            }
            log("notifyConnectCompleted at " + timeStamp + " cause=" + cause + " connectionCompletedMsg=" + msgToString(connectionCompletedMsg));
            connectionCompletedMsg.sendToTarget();
        }
        if (!sendAll) {
            return;
        }
        log("Send to all. " + alreadySent + " " + cause.toString());
        notifyAllWithEvent(alreadySent, 270371, cause.toString());
    }

    private void notifyDisconnectCompleted(DisconnectParams dp, boolean sendAll) {
        if (VDBG) {
            log("NotifyDisconnectCompleted");
        }
        ApnContext alreadySent = null;
        String reason = null;
        if (dp != null && dp.mOnCompletedMsg != null) {
            Message msg = dp.mOnCompletedMsg;
            dp.mOnCompletedMsg = null;
            if (msg.obj instanceof ApnContext) {
                alreadySent = (ApnContext) msg.obj;
                for (ConnectionParams cp : this.mApnContexts.values()) {
                    ApnContext apnContext = cp.mApnContext;
                    if (apnContext == alreadySent && PhoneInternalInterface.REASON_RA_FAILED.equals(dp.mReason)) {
                        log("set reason:" + dp.mReason);
                        apnContext.setReason(dp.mReason);
                    }
                }
            }
            reason = dp.mReason;
            if (VDBG) {
                Object[] objArr = new Object[2];
                objArr[0] = msg.toString();
                objArr[1] = msg.obj instanceof String ? (String) msg.obj : "<no-reason>";
                log(String.format("msg=%s msg.obj=%s", objArr));
            }
            AsyncResult.forMessage(msg);
            msg.sendToTarget();
        }
        if (sendAll) {
            if (reason == null) {
                reason = DcFailCause.UNKNOWN.toString();
            }
            notifyAllWithEvent(alreadySent, 270351, reason);
        }
        log("NotifyDisconnectCompleted DisconnectParams=" + dp);
    }

    public int getDataConnectionId() {
        return this.mId;
    }

    private void clearSettings() {
        log("clearSettings");
        resetRetryCount();
        this.mCreateTime = -1L;
        this.mLastFailTime = -1L;
        this.mLastFailCause = DcFailCause.NONE;
        this.mCid = -1;
        this.mRat = 1;
        this.mPcscfAddr = new String[5];
        this.mLinkProperties = new LinkProperties();
        this.mApnContexts.clear();
        this.mApnSetting = null;
        this.mDcFailCause = null;
    }

    private DataCallResponse.SetupResult onSetupConnectionCompleted(AsyncResult ar) {
        DataCallResponse response = (DataCallResponse) ar.result;
        ConnectionParams cp = (ConnectionParams) ar.userObj;
        if (cp.mTag != this.mTag) {
            log("onSetupConnectionCompleted stale cp.tag=" + cp.mTag + ", mtag=" + this.mTag);
            DataCallResponse.SetupResult result = DataCallResponse.SetupResult.ERR_Stale;
            return result;
        }
        if (ar.exception != null) {
            log("onSetupConnectionCompleted failed, ar.exception=" + ar.exception + " response=" + response);
            if ((ar.exception instanceof CommandException) && ((CommandException) ar.exception).getCommandError() == CommandException.Error.RADIO_NOT_AVAILABLE) {
                DataCallResponse.SetupResult result2 = DataCallResponse.SetupResult.ERR_BadCommand;
                result2.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
                return result2;
            }
            if (response == null || response.version < 4) {
                DataCallResponse.SetupResult result3 = DataCallResponse.SetupResult.ERR_GetLastErrorFromRil;
                return result3;
            }
            DataCallResponse.SetupResult result4 = DataCallResponse.SetupResult.ERR_RilError;
            result4.mFailCause = DcFailCause.fromInt(response.status);
            return result4;
        }
        if (response.status != 0) {
            DataCallResponse.SetupResult result5 = DataCallResponse.SetupResult.ERR_RilError;
            result5.mFailCause = DcFailCause.fromInt(response.status);
            return result5;
        }
        log("onSetupConnectionCompleted received successful DataCallResponse");
        this.mCid = response.cid;
        this.mRat = response.rat;
        this.mPcscfAddr = response.pcscf;
        DataCallResponse.SetupResult result6 = updateLinkProperty(response).setupResult;
        this.mInterfaceName = response.ifname;
        log("onSetupConnectionCompleted: ifname-" + this.mInterfaceName);
        return result6;
    }

    private DataCallResponse.SetupResult onSetupFallbackConnection(AsyncResult ar) {
        DataCallResponse response = (DataCallResponse) ar.result;
        ConnectionParams cp = (ConnectionParams) ar.userObj;
        if (cp.mTag != this.mTag) {
            log("onSetupFallbackConnection stale cp.tag=" + cp.mTag + ", mtag=" + this.mTag);
            DataCallResponse.SetupResult result = DataCallResponse.SetupResult.ERR_Stale;
            return result;
        }
        log("onSetupFallbackConnection received DataCallResponse: " + response);
        this.mCid = response.cid;
        this.mRat = response.rat;
        this.mPcscfAddr = response.pcscf;
        int tempStatus = response.status;
        response.status = DcFailCause.NONE.getErrorCode();
        DataCallResponse.SetupResult result2 = updateLinkProperty(response).setupResult;
        response.status = tempStatus;
        this.mInterfaceName = response.ifname;
        log("onSetupConnectionCompleted: ifname-" + this.mInterfaceName);
        return result2;
    }

    private boolean isDnsOk(String[] domainNameServers) {
        if (!NULL_IP.equals(domainNameServers[0]) || !NULL_IP.equals(domainNameServers[1]) || this.mPhone.isDnsCheckDisabled() || (this.mApnSetting.types[0].equals("mms") && isIpAddress(this.mApnSetting.mmsProxy))) {
            return true;
        }
        log(String.format("isDnsOk: return false apn.types[0]=%s APN_TYPE_MMS=%s isIpAddress(%s)=%s", this.mApnSetting.types[0], "mms", this.mApnSetting.mmsProxy, Boolean.valueOf(isIpAddress(this.mApnSetting.mmsProxy))));
        return false;
    }

    private void updateTcpBufferSizes(int rilRat) {
        String sizes = null;
        String ratName = ServiceState.rilRadioTechnologyToString(rilRat).toLowerCase(Locale.ROOT);
        if (rilRat == 7 || rilRat == 8 || rilRat == 12) {
            ratName = "evdo";
        }
        String[] configOverride = this.mPhone.getContext().getResources().getStringArray(R.array.config_cell_retries_per_error_code);
        int i = 0;
        while (true) {
            if (i >= configOverride.length) {
                break;
            }
            String[] split = configOverride[i].split(":");
            if (!ratName.equals(split[0]) || split.length != 2) {
                i++;
            } else {
                sizes = split[1];
                break;
            }
        }
        if (sizes == null) {
            switch (rilRat) {
                case 1:
                    sizes = TCP_BUFFER_SIZES_GPRS;
                    break;
                case 2:
                    sizes = TCP_BUFFER_SIZES_EDGE;
                    break;
                case 3:
                    sizes = TCP_BUFFER_SIZES_UMTS;
                    break;
                case 6:
                    sizes = TCP_BUFFER_SIZES_1XRTT;
                    break;
                case 7:
                case 8:
                case 12:
                    sizes = TCP_BUFFER_SIZES_EVDO;
                    break;
                case 9:
                    sizes = TCP_BUFFER_SIZES_HSDPA;
                    break;
                case 10:
                case 11:
                    sizes = TCP_BUFFER_SIZES_HSPA;
                    break;
                case 13:
                    sizes = TCP_BUFFER_SIZES_EHRPD;
                    break;
                case 14:
                    sizes = TCP_BUFFER_SIZES_LTE;
                    break;
                case 15:
                    sizes = TCP_BUFFER_SIZES_HSPAP;
                    break;
            }
        }
        this.mLinkProperties.setTcpBufferSizes(sizes);
    }

    private NetworkCapabilities makeNetworkCapabilities() {
        ArrayList<ApnSetting> wifiApnList;
        NetworkCapabilities result = new NetworkCapabilities();
        ApnSetting apnSetting = this.mApnSetting;
        result.addTransportType(0);
        if (this.mConnectionParams != null && this.mConnectionParams.mApnContext != null && this.mRat == 2 && (wifiApnList = this.mConnectionParams.mApnContext.getWifiApns()) != null) {
            for (ApnSetting tApnSetting : wifiApnList) {
                if (tApnSetting != null && !tApnSetting.apn.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                    log("makeNetworkCapabilities: apn: " + tApnSetting.apn);
                    apnSetting = tApnSetting;
                }
            }
        }
        boolean isDataEnabled = this.mDct.getDataEnabled();
        log("makeNetworkCapabilities: check data enable:" + isDataEnabled);
        if (apnSetting != null) {
            for (String type : apnSetting.types) {
                if (type.equals(CharacterSets.MIMENAME_ANY_CHARSET)) {
                    if (isDataEnabled && isDefaultDataSubPhone(this.mPhone)) {
                        result.addCapability(12);
                    }
                    if (isVsimActive()) {
                        result.addCapability(26);
                        result.removeCapability(12);
                    }
                    result.addCapability(0);
                    result.addCapability(1);
                    result.addCapability(3);
                    result.addCapability(4);
                    result.addCapability(10);
                    result.addCapability(5);
                    result.addCapability(7);
                    result.addCapability(20);
                    result.addCapability(21);
                    result.addCapability(22);
                    result.addCapability(23);
                    result.addCapability(24);
                    result.addCapability(25);
                    result.addCapability(9);
                    result.addCapability(8);
                    result.addCapability(27);
                } else if (type.equals("default")) {
                    if (isDataEnabled && isDefaultDataSubPhone(this.mPhone)) {
                        result.addCapability(12);
                    }
                    if (isVsimActive()) {
                        result.addCapability(26);
                        result.removeCapability(12);
                    }
                } else if (type.equals("mms")) {
                    result.addCapability(0);
                } else if (type.equals("supl")) {
                    result.addCapability(1);
                } else if (type.equals("dun")) {
                    ApnSetting securedDunApn = this.mDct.fetchDunApn();
                    if (securedDunApn == null || securedDunApn.equals(this.mApnSetting)) {
                        result.addCapability(2);
                    }
                } else if (type.equals("fota")) {
                    result.addCapability(3);
                } else if (type.equals(ImsSwitchController.IMS_SERVICE)) {
                    result.addCapability(4);
                } else if (type.equals("cbs")) {
                    result.addCapability(5);
                } else if (type.equals("ia")) {
                    result.addCapability(7);
                } else if (type.equals("emergency")) {
                    result.addCapability(10);
                } else if (type.equals("dm")) {
                    result.addCapability(20);
                } else if (type.equals("wap")) {
                    result.addCapability(21);
                } else if (type.equals("net")) {
                    result.addCapability(22);
                } else if (type.equals("cmmail")) {
                    result.addCapability(23);
                } else if (type.equals("tethering")) {
                    result.addCapability(24);
                } else if (type.equals("rcse")) {
                    result.addCapability(25);
                } else if (type.equals("xcap")) {
                    result.addCapability(9);
                } else if (type.equals("rcs")) {
                    result.addCapability(8);
                } else if (type.equals("bip")) {
                    result.addCapability(27);
                }
            }
            if (!this.mApnSetting.isMetered(this.mPhone.getContext(), this.mPhone.getSubId(), this.mPhone.getServiceState().getDataRoaming())) {
                result.addCapability(11);
            }
            result.maybeMarkCapabilitiesRestricted();
            if (isImsOrEmergencyApn(this.mApnSetting.types)) {
                result.removeCapability(13);
                log("makeNetworkCapabilities: IMS/EIMS APN shall be restricted");
            }
        }
        int up = 14;
        int down = 14;
        switch (this.mRilRat) {
            case 1:
                up = 80;
                down = 80;
                break;
            case 2:
                up = 59;
                down = 236;
                break;
            case 3:
                up = 384;
                down = 384;
                break;
            case 4:
            case 5:
                up = 14;
                down = 14;
                break;
            case 6:
                up = 100;
                down = 100;
                break;
            case 7:
                up = 153;
                down = 2457;
                break;
            case 8:
                up = 1843;
                down = 3174;
                break;
            case 9:
                up = 2048;
                down = 14336;
                break;
            case 10:
                up = 5898;
                down = 14336;
                break;
            case 11:
                up = 5898;
                down = 14336;
                break;
            case 12:
                up = 1843;
                down = 5017;
                break;
            case 13:
                up = 153;
                down = 2516;
                break;
            case 14:
                up = 51200;
                down = 102400;
                break;
            case 15:
                up = 11264;
                down = 43008;
                break;
        }
        result.setLinkUpstreamBandwidthKbps(up);
        result.setLinkDownstreamBandwidthKbps(down);
        result.setNetworkSpecifier(Integer.toString(this.mPhone.getSubId()));
        return result;
    }

    private static boolean isImsOrEmergencyApn(String[] apnTypes) {
        if (apnTypes.length == 0) {
            return false;
        }
        for (String type : apnTypes) {
            if (!ImsSwitchController.IMS_SERVICE.equals(type) && !"emergency".equals(type)) {
                return false;
            }
        }
        return true;
    }

    private boolean isIpAddress(String address) {
        if (address == null) {
            return false;
        }
        return Patterns.IP_ADDRESS.matcher(address).matches();
    }

    private DataCallResponse.SetupResult setLinkProperties(DataCallResponse response, LinkProperties lp) {
        String propertyPrefix = "net." + response.ifname + ".";
        String[] dnsServers = {SystemProperties.get(propertyPrefix + "dns1"), SystemProperties.get(propertyPrefix + "dns2")};
        boolean okToUseSystemPropertyDns = isDnsOk(dnsServers);
        return response.setLinkProperties(lp, okToUseSystemPropertyDns);
    }

    private boolean initConnection(ConnectionParams cp) {
        ApnContext apnContext = cp.mApnContext;
        if (this.mApnSetting == null) {
            this.mApnSetting = apnContext.getApnSetting();
        }
        if (this.mApnSetting == null || !this.mApnSetting.canHandleType(apnContext.getApnType())) {
            log("initConnection: incompatible apnSetting in ConnectionParams cp=" + cp + " dc=" + this);
            return false;
        }
        this.mTag++;
        this.mConnectionParams = cp;
        this.mConnectionParams.mTag = this.mTag;
        if (!this.mApnContexts.containsKey(apnContext)) {
            checkIfDefaultApnReferenceCountChanged();
        }
        this.mApnContexts.put(apnContext, cp);
        log("initConnection:  RefCount=" + this.mApnContexts.size() + " mApnList=" + this.mApnContexts + " mConnectionParams=" + this.mConnectionParams);
        return true;
    }

    private class DcDefaultState extends State {
        DcDefaultState(DataConnection this$0, DcDefaultState dcDefaultState) {
            this();
        }

        private DcDefaultState() {
        }

        public void enter() {
            DataConnection.this.log("DcDefaultState: enter");
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED, null);
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRoamingOn(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_ROAM_ON, null);
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRoamingOff(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF, null);
            DataConnection.this.registerNetworkAlertObserver();
            DataConnection.this.mDcController.addDc(DataConnection.this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(DataConnection.this.mActionRetry);
            DataConnection.this.log("DcDefaultState: register for intent action=" + DataConnection.this.mActionRetry);
            DataConnection.this.mPhone.getContext().registerReceiver(DataConnection.this.mIntentReceiver, filter, null, DataConnection.this.getHandler());
        }

        public void exit() {
            DataConnection.this.log("DcDefaultState: exit");
            DataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(DataConnection.this.getHandler());
            DataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRoamingOn(DataConnection.this.getHandler());
            DataConnection.this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(DataConnection.this.getHandler());
            DataConnection.this.mDcController.removeDc(DataConnection.this);
            if (DataConnection.this.mAc != null) {
                DataConnection.this.mAc.disconnected();
                DataConnection.this.mAc = null;
            }
            DataConnection.this.mApnContexts = null;
            DataConnection.this.mReconnectIntent = null;
            DataConnection.this.mDct = null;
            DataConnection.this.mApnSetting = null;
            DataConnection.this.mPhone = null;
            DataConnection.this.mLinkProperties = null;
            DataConnection.this.mLastFailCause = null;
            DataConnection.this.mUserData = null;
            DataConnection.this.mDcController = null;
            DataConnection.this.mDcTesterFailBringUpAll = null;
            DataConnection.this.unregisterNetworkAlertObserver();
            DataConnection.this.mPhone.getContext().unregisterReceiver(DataConnection.this.mIntentReceiver);
        }

        public boolean processMessage(Message msg) {
            if (DataConnection.VDBG) {
                DataConnection.this.log("DcDefault msg=" + DataConnection.this.getWhatToString(msg.what) + " RefCount=" + DataConnection.this.mApnContexts.size());
            }
            switch (msg.what) {
                case 69633:
                    if (DataConnection.this.mAc != null) {
                        if (DataConnection.VDBG) {
                            DataConnection.this.log("Disconnecting to previous connection mAc=" + DataConnection.this.mAc);
                        }
                        DataConnection.this.mAc.replyToMessage(msg, 69634, 3);
                    } else {
                        DataConnection.this.mAc = new AsyncChannel();
                        DataConnection.this.mAc.connected((Context) null, DataConnection.this.getHandler(), msg.replyTo);
                        if (DataConnection.VDBG) {
                            DataConnection.this.log("DcDefaultState: FULL_CONNECTION reply connected");
                        }
                        DataConnection.this.mAc.replyToMessage(msg, 69634, 0, DataConnection.this.mId, "hi");
                    }
                    return true;
                case 69636:
                    DataConnection.this.log("DcDefault: CMD_CHANNEL_DISCONNECTED before quiting call dump");
                    DataConnection.this.dumpToLog();
                    DataConnection.this.quit();
                    return true;
                case SmsEnvelope.TELESERVICE_MWI:
                    DataConnection.this.log("DcDefaultState: msg.what=EVENT_CONNECT, fail not expected");
                    ConnectionParams cp = (ConnectionParams) msg.obj;
                    DataConnection.this.notifyConnectCompleted(cp, DcFailCause.UNKNOWN, false);
                    return true;
                case DataConnection.EVENT_DISCONNECT:
                    DataConnection.this.log("DcDefaultState deferring msg.what=EVENT_DISCONNECT RefCount=" + DataConnection.this.mApnContexts.size());
                    DataConnection.this.deferMessage(msg);
                    return true;
                case DataConnection.EVENT_DISCONNECT_ALL:
                    DataConnection.this.log("DcDefaultState deferring msg.what=EVENT_DISCONNECT_ALL RefCount=" + DataConnection.this.mApnContexts.size());
                    DataConnection.this.deferMessage(msg);
                    return true;
                case DataConnection.EVENT_TEAR_DOWN_NOW:
                    DataConnection.this.log("DcDefaultState EVENT_TEAR_DOWN_NOW");
                    DataConnection.this.mPhone.mCi.deactivateDataCall(DataConnection.this.mCid, 0, null);
                    return true;
                case DataConnection.EVENT_LOST_CONNECTION:
                    String s = "DcDefaultState ignore EVENT_LOST_CONNECTION tag=" + msg.arg1 + ":mTag=" + DataConnection.this.mTag;
                    DataConnection.this.logAndAddLogRec(s);
                    return true;
                case DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    Pair<Integer, Integer> drsRatPair = (Pair) ar.result;
                    DataConnection.this.mDataRegState = ((Integer) drsRatPair.first).intValue();
                    if (DataConnection.this.mRilRat != ((Integer) drsRatPair.second).intValue()) {
                        DataConnection.this.updateTcpBufferSizes(((Integer) drsRatPair.second).intValue());
                    }
                    DataConnection.this.mRilRat = ((Integer) drsRatPair.second).intValue();
                    DataConnection.this.log("DcDefaultState: EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED drs=" + DataConnection.this.mDataRegState + " mRilRat=" + DataConnection.this.mRilRat);
                    ServiceState ss = DataConnection.this.mPhone.getServiceState();
                    int networkType = ss.getDataNetworkType();
                    DataConnection.this.mNetworkInfo.setSubtype(networkType, TelephonyManager.getNetworkTypeName(networkType));
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.updateNetworkInfoSuspendState();
                        DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.makeNetworkCapabilities());
                        DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                        DataConnection.this.mNetworkAgent.sendLinkProperties(DataConnection.this.mLinkProperties);
                    }
                    return true;
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_ON:
                    DataConnection.this.mNetworkInfo.setRoaming(true);
                    return true;
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF:
                    DataConnection.this.mNetworkInfo.setRoaming(false);
                    return true;
                case DataConnection.EVENT_IPV4_ADDRESS_REMOVED:
                    DataConnection.this.log("DcDefaultState: ignore EVENT_IPV4_ADDRESS_REMOVED not in ActiveState");
                    return true;
                case DataConnection.EVENT_IPV6_ADDRESS_REMOVED:
                    DataConnection.this.log("DcDefaultState: ignore EVENT_IPV6_ADDRESS_REMOVED not in ActiveState");
                    return true;
                case DataConnection.EVENT_ADDRESS_REMOVED:
                    DataConnection.this.log("DcDefaultState: " + DataConnection.this.getWhatToString(msg.what));
                    return true;
                case DataConnection.EVENT_VOICE_CALL:
                    DataConnection.this.mIsInVoiceCall = msg.arg1 != 0;
                    DataConnection.this.mIsSupportConcurrent = msg.arg2 != 0;
                    return true;
                case 266240:
                    boolean val = DataConnection.this.getIsInactive();
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("REQ_IS_INACTIVE  isInactive=" + val);
                    }
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_IS_INACTIVE, val ? 1 : 0);
                    return true;
                case DcAsyncChannel.REQ_GET_CID:
                    int cid = DataConnection.this.getCid();
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("REQ_GET_CID  cid=" + cid);
                    }
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_CID, cid);
                    return true;
                case DcAsyncChannel.REQ_GET_APNSETTING:
                    ApnSetting apnSetting = DataConnection.this.getApnSetting();
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("REQ_GET_APNSETTING  mApnSetting=" + apnSetting);
                    }
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_APNSETTING, apnSetting);
                    return true;
                case DcAsyncChannel.REQ_GET_LINK_PROPERTIES:
                    LinkProperties lp = DataConnection.this.getCopyLinkProperties();
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("REQ_GET_LINK_PROPERTIES linkProperties" + lp);
                    }
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_LINK_PROPERTIES, lp);
                    return true;
                case DcAsyncChannel.REQ_SET_LINK_PROPERTIES_HTTP_PROXY:
                    ProxyInfo proxy = (ProxyInfo) msg.obj;
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("REQ_SET_LINK_PROPERTIES_HTTP_PROXY proxy=" + proxy);
                    }
                    DataConnection.this.setLinkPropertiesHttpProxy(proxy);
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_SET_LINK_PROPERTIES_HTTP_PROXY);
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendLinkProperties(DataConnection.this.mLinkProperties);
                    }
                    return true;
                case DcAsyncChannel.REQ_GET_NETWORK_CAPABILITIES:
                    NetworkCapabilities nc = DataConnection.this.getCopyNetworkCapabilities();
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("REQ_GET_NETWORK_CAPABILITIES networkCapabilities" + nc);
                    }
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_NETWORK_CAPABILITIES, nc);
                    return true;
                case DcAsyncChannel.REQ_RESET:
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("DcDefaultState: msg.what=REQ_RESET");
                    }
                    DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                    return true;
                case DcAsyncChannel.REQ_GET_APNTYPE:
                    String[] aryApnType = DataConnection.this.getApnType();
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("REQ_GET_APNTYPE  aryApnType=" + aryApnType);
                    }
                    DataConnection.this.mAc.replyToMessage(msg, DcAsyncChannel.RSP_GET_APNTYPE, aryApnType);
                    return true;
                default:
                    DataConnection.this.log("DcDefaultState: shouldn't happen but ignore msg.what=" + DataConnection.this.getWhatToString(msg.what));
                    return true;
            }
        }
    }

    private boolean updateNetworkInfoSuspendState() {
        NetworkInfo.DetailedState oldState = this.mNetworkInfo.getDetailedState();
        log("updateNetworkInfoSuspendState: oldState = " + oldState + ", mIsInVoiceCall = " + this.mIsInVoiceCall + ", mIsSupportConcurrent = " + this.mIsSupportConcurrent);
        if (this.mNetworkAgent == null) {
            Rlog.e(getName(), "Setting suspend state without a NetworkAgent");
        }
        ServiceStateTracker sst = this.mPhone.getServiceStateTracker();
        if (sst.getCurrentDataConnectionState() != 0) {
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, null, this.mNetworkInfo.getExtraInfo());
        } else {
            if (this.mIsInVoiceCall && !this.mIsSupportConcurrent) {
                this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, null, this.mNetworkInfo.getExtraInfo());
                return oldState != NetworkInfo.DetailedState.SUSPENDED;
            }
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, this.mNetworkInfo.getExtraInfo());
        }
        return oldState != this.mNetworkInfo.getDetailedState();
    }

    private class DcInactiveState extends State {
        DcInactiveState(DataConnection this$0, DcInactiveState dcInactiveState) {
            this();
        }

        private DcInactiveState() {
        }

        public void setEnterNotificationParams(ConnectionParams cp, DcFailCause cause) {
            if (DataConnection.VDBG) {
                DataConnection.this.log("DcInactiveState: setEnterNotificationParams cp,cause");
            }
            DataConnection.this.mConnectionParams = cp;
            DataConnection.this.mDisconnectParams = null;
            DataConnection.this.mDcFailCause = cause;
        }

        public void setEnterNotificationParams(DisconnectParams dp) {
            if (DataConnection.VDBG) {
                DataConnection.this.log("DcInactiveState: setEnterNotificationParams dp");
            }
            DataConnection.this.mConnectionParams = null;
            DataConnection.this.mDisconnectParams = dp;
            DataConnection.this.mDcFailCause = DcFailCause.NONE;
        }

        public void setEnterNotificationParams(DcFailCause cause) {
            DataConnection.this.mConnectionParams = null;
            DataConnection.this.mDisconnectParams = null;
            DataConnection.this.mDcFailCause = cause;
        }

        public void enter() {
            DataConnection.this.mTag++;
            DataConnection.this.log("DcInactiveState: enter() mTag=" + DataConnection.this.mTag);
            if (DataConnection.this.mConnectionParams != null) {
                DataConnection.this.log("DcInactiveState: enter notifyConnectCompleted +ALL failCause=" + DataConnection.this.mDcFailCause);
                DataConnection.this.notifyConnectCompleted(DataConnection.this.mConnectionParams, DataConnection.this.mDcFailCause, true);
            }
            if (DataConnection.this.mDisconnectParams != null) {
                DataConnection.this.log("DcInactiveState: enter notifyDisconnectCompleted +ALL failCause=" + DataConnection.this.mDcFailCause);
                DataConnection.this.notifyDisconnectCompleted(DataConnection.this.mDisconnectParams, true);
            }
            if (DataConnection.this.mDisconnectParams == null && DataConnection.this.mConnectionParams == null && DataConnection.this.mDcFailCause != null) {
                DataConnection.this.log("DcInactiveState: enter notifyAllDisconnectCompleted failCause=" + DataConnection.this.mDcFailCause);
                DataConnection.this.notifyAllDisconnectCompleted(DataConnection.this.mDcFailCause);
            }
            DataConnection.this.mDcController.removeActiveDcByCid(DataConnection.this);
            DataConnection.this.clearSettings();
        }

        public void exit() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case SmsEnvelope.TELESERVICE_MWI:
                    DataConnection.this.log("DcInactiveState: mag.what=EVENT_CONNECT");
                    ConnectionParams cp = (ConnectionParams) msg.obj;
                    if (DataConnection.this.initConnection(cp)) {
                        DataConnection.this.onConnect(DataConnection.this.mConnectionParams);
                        DataConnection.this.transitionTo(DataConnection.this.mActivatingState);
                    } else {
                        DataConnection.this.log("DcInactiveState: msg.what=EVENT_CONNECT initConnection failed");
                        DataConnection.this.notifyConnectCompleted(cp, DcFailCause.UNACCEPTABLE_NETWORK_PARAMETER, false);
                    }
                    break;
                case DataConnection.EVENT_DISCONNECT:
                    DataConnection.this.log("DcInactiveState: msg.what=EVENT_DISCONNECT");
                    DataConnection.this.notifyDisconnectCompleted((DisconnectParams) msg.obj, false);
                    break;
                case DataConnection.EVENT_DISCONNECT_ALL:
                    DataConnection.this.log("DcInactiveState: msg.what=EVENT_DISCONNECT_ALL");
                    DataConnection.this.notifyDisconnectCompleted((DisconnectParams) msg.obj, false);
                    break;
                case DcAsyncChannel.REQ_RESET:
                    DataConnection.this.log("DcInactiveState: msg.what=RSP_RESET, ignore we're already reset");
                    break;
                default:
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("DcInactiveState nothandled msg.what=" + DataConnection.this.getWhatToString(msg.what));
                    }
                    break;
            }
            return true;
        }
    }

    private class DcActivatingState extends State {

        private static final int[] f21x5f2cdfda = null;
        final int[] $SWITCH_TABLE$com$android$internal$telephony$dataconnection$DataCallResponse$SetupResult;

        private static int[] m328xa73f2fb6() {
            if (f21x5f2cdfda != null) {
                return f21x5f2cdfda;
            }
            int[] iArr = new int[DataCallResponse.SetupResult.valuesCustom().length];
            try {
                iArr[DataCallResponse.SetupResult.ERR_BadCommand.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[DataCallResponse.SetupResult.ERR_GetLastErrorFromRil.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[DataCallResponse.SetupResult.ERR_RilError.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[DataCallResponse.SetupResult.ERR_Stale.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[DataCallResponse.SetupResult.ERR_UnacceptableParameter.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[DataCallResponse.SetupResult.SUCCESS.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            f21x5f2cdfda = iArr;
            return iArr;
        }

        DcActivatingState(DataConnection this$0, DcActivatingState dcActivatingState) {
            this();
        }

        private DcActivatingState() {
        }

        public void enter() {
            DataConnection.this.log("DcActivatingState: enter dc=" + DataConnection.this);
        }

        public void exit() {
            DataConnection.this.log("DcActivatingState: exit dc=" + this);
        }

        public boolean processMessage(Message msg) {
            DataConnection.this.log("DcActivatingState: msg=" + DataConnection.msgToString(msg));
            switch (msg.what) {
                case SmsEnvelope.TELESERVICE_MWI:
                    ConnectionParams cp = (ConnectionParams) msg.obj;
                    DataConnection.this.mApnContexts.put(cp.mApnContext, cp);
                    DataConnection.this.log("DcActivatingState: mApnContexts size=" + DataConnection.this.mApnContexts.size());
                    break;
                case DataConnection.EVENT_SETUP_DATA_CONNECTION_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    ConnectionParams cp2 = (ConnectionParams) ar.userObj;
                    DataCallResponse.SetupResult result = DataConnection.this.onSetupConnectionCompleted(ar);
                    if (result != DataCallResponse.SetupResult.ERR_Stale && DataConnection.this.mConnectionParams != cp2) {
                        DataConnection.this.loge("DcActivatingState: WEIRD mConnectionsParams:" + DataConnection.this.mConnectionParams + " != cp:" + cp2);
                    }
                    DataConnection.this.log("DcActivatingState onSetupConnectionCompleted result=" + result + " dc=" + DataConnection.this);
                    if (cp2.mApnContext != null) {
                        cp2.mApnContext.requestLog("onSetupConnectionCompleted result=" + result);
                    }
                    switch (m328xa73f2fb6()[result.ordinal()]) {
                        case 1:
                            DataConnection.this.mInactiveState.setEnterNotificationParams(cp2, result.mFailCause);
                            DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                            return true;
                        case 2:
                            DataConnection.this.mPhone.mCi.getLastDataCallFailCause(DataConnection.this.obtainMessage(DataConnection.EVENT_GET_LAST_FAIL_DONE, cp2));
                            return true;
                        case 3:
                            long delay = DataConnection.this.getSuggestedRetryDelay(ar);
                            cp2.mApnContext.setModemSuggestedDelay(delay);
                            String str = "DcActivatingState: ERR_RilError  delay=" + delay + " result=" + result + " result.isRestartRadioFail=" + result.mFailCause.isRestartRadioFail() + " result.isPermanentFail=" + DataConnection.this.mDct.isPermanentFail(result.mFailCause);
                            DataConnection.this.log(str);
                            if (cp2.mApnContext != null) {
                                cp2.mApnContext.requestLog(str);
                            }
                            if (result.mFailCause == DcFailCause.PDP_FAIL_FALLBACK_RETRY) {
                                DataConnection.this.onSetupFallbackConnection(ar);
                                DataConnection.this.mDcFailCause = DcFailCause.PDP_FAIL_FALLBACK_RETRY;
                                if (DataConnection.this.mDcFcMgr == null || !DataConnection.this.mDcFcMgr.isSpecificNetworkAndSimOperator(DcFailCauseManager.Operator.OP19)) {
                                    DataConnection.this.deferMessage(DataConnection.this.obtainMessage(DataConnection.EVENT_FALLBACK_RETRY_CONNECTION, DataConnection.this.mTag));
                                } else {
                                    DataConnection.this.mRetryCount++;
                                    long retryTime = DataConnection.this.mDcFcMgr.getRetryTimeByIndex(DataConnection.this.mRetryCount, DcFailCauseManager.Operator.OP19);
                                    if (retryTime < 0) {
                                        DataConnection.this.log("DcActiveState_FALLBACK_Retry: No retry but at least one IPv4 or IPv6 is accepted");
                                        DataConnection.this.mDcFailCause = DcFailCause.NONE;
                                        DataConnection.this.resetRetryCount();
                                    } else {
                                        DataConnection.this.startRetryAlarm(DataConnection.EVENT_FALLBACK_RETRY_CONNECTION, DataConnection.this.mTag, retryTime);
                                    }
                                }
                                DataConnection.this.transitionTo(DataConnection.this.mActiveState);
                            } else {
                                DataConnection.this.mInactiveState.setEnterNotificationParams(cp2, result.mFailCause);
                                DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                            }
                            return true;
                        case 4:
                            DataConnection.this.loge("DcActivatingState: stale EVENT_SETUP_DATA_CONNECTION_DONE tag:" + cp2.mTag + " != mTag:" + DataConnection.this.mTag);
                            return true;
                        case 5:
                            DataConnection.this.tearDownData(cp2);
                            DataConnection.this.transitionTo(DataConnection.this.mDisconnectingErrorCreatingConnection);
                            return true;
                        case 6:
                            DataConnection.this.mDcFailCause = DcFailCause.NONE;
                            DataConnection.this.resetRetryCount();
                            DataConnection.this.transitionTo(DataConnection.this.mActiveState);
                            return true;
                        default:
                            throw new RuntimeException("Unknown SetupResult, should not happen");
                    }
                case DataConnection.EVENT_GET_LAST_FAIL_DONE:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    ConnectionParams cp3 = (ConnectionParams) ar2.userObj;
                    if (cp3.mTag == DataConnection.this.mTag) {
                        if (DataConnection.this.mConnectionParams != cp3) {
                            DataConnection.this.loge("DcActivatingState: WEIRD mConnectionsParams:" + DataConnection.this.mConnectionParams + " != cp:" + cp3);
                        }
                        DcFailCause cause = DcFailCause.UNKNOWN;
                        if (ar2.exception == null) {
                            int rilFailCause = ((int[]) ar2.result)[0];
                            cause = DcFailCause.fromInt(rilFailCause);
                            if (cause == DcFailCause.NONE) {
                                DataConnection.this.log("DcActivatingState msg.what=EVENT_GET_LAST_FAIL_DONE BAD: error was NONE, change to UNKNOWN");
                                cause = DcFailCause.UNKNOWN;
                            }
                        }
                        DataConnection.this.mDcFailCause = cause;
                        DataConnection.this.log("DcActivatingState msg.what=EVENT_GET_LAST_FAIL_DONE cause=" + cause + " dc=" + DataConnection.this);
                        DataConnection.this.mInactiveState.setEnterNotificationParams(cp3, cause);
                        DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                    } else {
                        DataConnection.this.loge("DcActivatingState: stale EVENT_GET_LAST_FAIL_DONE tag:" + cp3.mTag + " != mTag:" + DataConnection.this.mTag);
                    }
                    return true;
                case DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED:
                    break;
                case DataConnection.EVENT_IPV4_ADDRESS_REMOVED:
                case DataConnection.EVENT_IPV6_ADDRESS_REMOVED:
                    DataConnection.this.log("DcActivatingState deferMsg: " + DataConnection.this.getWhatToString(msg.what) + ", address info: " + ((AddressInfo) msg.obj));
                    DataConnection.this.deferMessage(msg);
                    return true;
                default:
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("DcActivatingState not handled msg.what=" + DataConnection.this.getWhatToString(msg.what) + " RefCount=" + DataConnection.this.mApnContexts.size());
                    }
                    return false;
            }
            DataConnection.this.deferMessage(msg);
            return true;
        }
    }

    private class DcActiveState extends State {

        private static final int[] f22x5f2cdfda = null;
        final int[] $SWITCH_TABLE$com$android$internal$telephony$dataconnection$DataCallResponse$SetupResult;

        private static int[] m329xa73f2fb6() {
            if (f22x5f2cdfda != null) {
                return f22x5f2cdfda;
            }
            int[] iArr = new int[DataCallResponse.SetupResult.valuesCustom().length];
            try {
                iArr[DataCallResponse.SetupResult.ERR_BadCommand.ordinal()] = 4;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[DataCallResponse.SetupResult.ERR_GetLastErrorFromRil.ordinal()] = 5;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[DataCallResponse.SetupResult.ERR_RilError.ordinal()] = 1;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[DataCallResponse.SetupResult.ERR_Stale.ordinal()] = 2;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[DataCallResponse.SetupResult.ERR_UnacceptableParameter.ordinal()] = 6;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[DataCallResponse.SetupResult.SUCCESS.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            f22x5f2cdfda = iArr;
            return iArr;
        }

        DcActiveState(DataConnection this$0, DcActiveState dcActiveState) {
            this();
        }

        private DcActiveState() {
        }

        public void enter() {
            DataConnection.this.log("DcActiveState: enter dc=" + DataConnection.this);
            boolean createNetworkAgent = true;
            if (((DataConnection.this.hasMessages(DataConnection.EVENT_DISCONNECT) || DataConnection.this.hasDeferredMessages(DataConnection.EVENT_DISCONNECT)) && DataConnection.this.mApnContexts.size() == 1) || DataConnection.this.hasMessages(DataConnection.EVENT_DISCONNECT_ALL) || DataConnection.this.hasDeferredMessages(DataConnection.EVENT_DISCONNECT_ALL)) {
                DataConnection.this.log("DcActiveState: skipping notifyAllOfConnected()");
                createNetworkAgent = false;
            } else {
                DataConnection.this.notifyAllOfConnected("connected");
            }
            DataConnection.this.mDcController.addActiveDcByCid(DataConnection.this);
            if (DataConnection.this.mIsInVoiceCall && !DataConnection.this.mIsSupportConcurrent) {
                DataConnection.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, DataConnection.this.mNetworkInfo.getReason(), null);
            } else {
                DataConnection.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, DataConnection.this.mNetworkInfo.getReason(), null);
            }
            DataConnection.this.mNetworkInfo.setExtraInfo(DataConnection.this.mApnSetting.apn);
            DataConnection.this.updateTcpBufferSizes(DataConnection.this.mRilRat);
            NetworkMisc misc = new NetworkMisc();
            misc.subscriberId = DataConnection.this.mPhone.getSubscriberId();
            if (createNetworkAgent) {
                DataConnection.this.mNetworkAgent = DataConnection.this.new DcNetworkAgent(DataConnection.this.getHandler().getLooper(), DataConnection.this.mPhone.getContext(), "DcNetworkAgent", DataConnection.this.mNetworkInfo, DataConnection.this.makeNetworkCapabilities(), DataConnection.this.mLinkProperties, 50, misc);
            }
            if (DataConnection.BSP_PACKAGE) {
                return;
            }
            try {
                DataConnection.this.mGsmDCTExt.onDcActivated(DataConnection.this.mApnSetting == null ? null : DataConnection.this.mApnSetting.types, DataConnection.this.mLinkProperties == null ? UsimPBMemInfo.STRING_NOT_SET : DataConnection.this.mLinkProperties.getInterfaceName());
            } catch (Exception e) {
                DataConnection.this.loge("onDcActivated fail!");
                e.printStackTrace();
            }
        }

        public void exit() {
            DataConnection.this.log("DcActiveState: exit dc=" + this);
            String reason = DataConnection.this.mNetworkInfo.getReason();
            if (DataConnection.this.mDcController.isExecutingCarrierChange()) {
                reason = PhoneInternalInterface.REASON_CARRIER_CHANGE;
            } else if (DataConnection.this.mDisconnectParams != null && DataConnection.this.mDisconnectParams.mReason != null) {
                reason = DataConnection.this.mDisconnectParams.mReason;
            } else if (DataConnection.this.mDcFailCause != null) {
                reason = DataConnection.this.mDcFailCause.toString();
            }
            if (!DataConnection.BSP_PACKAGE) {
                try {
                    DataConnection.this.mGsmDCTExt.onDcDeactivated(DataConnection.this.mApnSetting == null ? null : DataConnection.this.mApnSetting.types, DataConnection.this.mLinkProperties == null ? UsimPBMemInfo.STRING_NOT_SET : DataConnection.this.mLinkProperties.getInterfaceName());
                } catch (Exception e) {
                    DataConnection.this.loge("onDcDeactivated fail!");
                    e.printStackTrace();
                }
            }
            DataConnection.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, reason, DataConnection.this.mNetworkInfo.getExtraInfo());
            if (DataConnection.this.mNetworkAgent == null) {
                return;
            }
            DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
            DataConnection.this.mNetworkAgent = null;
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case SmsEnvelope.TELESERVICE_MWI:
                    ConnectionParams cp = (ConnectionParams) msg.obj;
                    DataConnection.this.mApnContexts.put(cp.mApnContext, cp);
                    DataConnection.this.log("DcActiveState: EVENT_CONNECT cp=" + cp + " dc=" + DataConnection.this);
                    DataConnection.this.checkIfDefaultApnReferenceCountChanged();
                    if (DataConnection.this.mNetworkAgent != null) {
                        NetworkCapabilities cap = DataConnection.this.makeNetworkCapabilities();
                        DataConnection.this.mNetworkAgent.sendNetworkCapabilities(cap);
                        DataConnection.this.log("DcActiveState update Capabilities:" + cap);
                    }
                    DataConnection.this.notifyConnectCompleted(cp, DcFailCause.NONE, false);
                    break;
                case DataConnection.EVENT_SETUP_DATA_CONNECTION_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    ConnectionParams cp2 = (ConnectionParams) ar.userObj;
                    DataCallResponse.SetupResult result = DataConnection.this.onSetupConnectionCompleted(ar);
                    if (result != DataCallResponse.SetupResult.ERR_Stale && DataConnection.this.mConnectionParams != cp2) {
                        DataConnection.this.loge("DcActiveState_FALLBACK_Retry: WEIRD mConnectionsParams:" + DataConnection.this.mConnectionParams + " != cp:" + cp2);
                    }
                    DataConnection.this.log("DcActiveState_FALLBACK_Retry onSetupConnectionCompleted result=" + result + " dc=" + DataConnection.this);
                    switch (m329xa73f2fb6()[result.ordinal()]) {
                        case 1:
                            String str = "DcActiveState_FALLBACK_Retry: ERR_RilError  result=" + result + " result.isRestartRadioFail=" + result.mFailCause.isRestartRadioFail() + " result.isPermanentFail=" + DataConnection.this.mDct.isPermanentFail(result.mFailCause);
                            DataConnection.this.log(str);
                            if (result.mFailCause != DcFailCause.PDP_FAIL_FALLBACK_RETRY) {
                                DataConnection.this.log("DcActiveState_FALLBACK_Retry: ERR_RilError Not retry anymore");
                            } else if (DataConnection.this.mDcFcMgr != null && DataConnection.this.mDcFcMgr.isSpecificNetworkAndSimOperator(DcFailCauseManager.Operator.OP19)) {
                                DataConnection.this.mRetryCount++;
                                long retryTime = DataConnection.this.mDcFcMgr.getRetryTimeByIndex(DataConnection.this.mRetryCount, DcFailCauseManager.Operator.OP19);
                                if (retryTime >= 0) {
                                    DataConnection.this.mDcFailCause = DcFailCause.PDP_FAIL_FALLBACK_RETRY;
                                    DataConnection.this.startRetryAlarm(DataConnection.EVENT_FALLBACK_RETRY_CONNECTION, DataConnection.this.mTag, retryTime);
                                } else {
                                    DataConnection.this.log("DcActiveState_FALLBACK_Retry: No retry but at least one IPv4 or IPv6 is accepted");
                                    DataConnection.this.mDcFailCause = DcFailCause.NONE;
                                }
                            }
                            break;
                        case 2:
                            DataConnection.this.loge("DcActiveState_FALLBACK_Retry: stale EVENT_SETUP_DATA_CONNECTION_DONE tag:" + cp2.mTag + " != mTag:" + DataConnection.this.mTag + " Not retry anymore");
                            break;
                        case 3:
                            DataConnection.this.mDcFailCause = DcFailCause.NONE;
                            DataConnection.this.resetRetryCount();
                            break;
                        default:
                            DataConnection.this.log("DcActiveState_FALLBACK_Retry: Another error cause, Not retry anymore");
                            break;
                    }
                    break;
                case DataConnection.EVENT_GET_LAST_FAIL_DONE:
                case DataConnection.EVENT_DEACTIVATE_DONE:
                case DataConnection.EVENT_RIL_CONNECTED:
                case DataConnection.EVENT_DATA_STATE_CHANGED:
                case DataConnection.EVENT_TEAR_DOWN_NOW:
                case 262154:
                case DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED:
                case DataConnection.EVENT_DATA_STATE_CHANGED_FOR_LOADED:
                case DataConnection.EVENT_ADDRESS_REMOVED:
                default:
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("DcActiveState not handled msg.what=" + DataConnection.this.getWhatToString(msg.what));
                    }
                    break;
                case DataConnection.EVENT_DISCONNECT:
                    DisconnectParams dp = (DisconnectParams) msg.obj;
                    DataConnection.this.log("DcActiveState: EVENT_DISCONNECT dp=" + dp + " dc=" + DataConnection.this);
                    if (!DataConnection.this.mApnContexts.containsKey(dp.mApnContext)) {
                        DataConnection.this.log("DcActiveState ERROR no such apnContext=" + dp.mApnContext + " in this dc=" + DataConnection.this);
                        DataConnection.this.notifyDisconnectCompleted(dp, false);
                    } else {
                        DataConnection.this.log("DcActiveState msg.what=EVENT_DISCONNECT RefCount=" + DataConnection.this.mApnContexts.size());
                        if (DataConnection.this.mApnContexts.size() != 1) {
                            DataConnection.this.mApnContexts.remove(dp.mApnContext);
                            if (DataConnection.this.mNetworkAgent != null) {
                                NetworkCapabilities cap2 = DataConnection.this.makeNetworkCapabilities();
                                DataConnection.this.mNetworkAgent.sendNetworkCapabilities(cap2);
                                DataConnection.this.log("DcActiveState update Capabilities:" + cap2);
                            }
                            DataConnection.this.checkIfDefaultApnReferenceCountChanged();
                            DataConnection.this.notifyDisconnectCompleted(dp, false);
                        } else {
                            DataConnection.this.handlePcscfErrorCause(dp);
                            DataConnection.this.mApnContexts.clear();
                            DataConnection.this.mDisconnectParams = dp;
                            DataConnection.this.mConnectionParams = null;
                            dp.mTag = DataConnection.this.mTag;
                            DataConnection.this.tearDownData(dp);
                            DataConnection.this.transitionTo(DataConnection.this.mDisconnectingState);
                        }
                    }
                    break;
                case DataConnection.EVENT_DISCONNECT_ALL:
                    DataConnection.this.log("DcActiveState EVENT_DISCONNECT_ALL clearing apn contexts, dc=" + DataConnection.this);
                    DisconnectParams dp2 = (DisconnectParams) msg.obj;
                    DataConnection.this.mDisconnectParams = dp2;
                    DataConnection.this.mConnectionParams = null;
                    dp2.mTag = DataConnection.this.mTag;
                    DataConnection.this.tearDownData(dp2);
                    DataConnection.this.transitionTo(DataConnection.this.mDisconnectingState);
                    break;
                case DataConnection.EVENT_LOST_CONNECTION:
                    DataConnection.this.log("DcActiveState EVENT_LOST_CONNECTION dc=" + DataConnection.this);
                    DataConnection.this.mInactiveState.setEnterNotificationParams(DcFailCause.LOST_CONNECTION);
                    DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                    break;
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_ON:
                    DataConnection.this.mNetworkInfo.setRoaming(true);
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                    }
                    break;
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF:
                    DataConnection.this.mNetworkInfo.setRoaming(false);
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                    }
                    break;
                case DataConnection.EVENT_BW_REFRESH_RESPONSE:
                    AsyncResult ar2 = (AsyncResult) msg.obj;
                    if (ar2.exception == null) {
                        ArrayList<Integer> capInfo = (ArrayList) ar2.result;
                        int lceBwDownKbps = capInfo.get(0).intValue();
                        NetworkCapabilities nc = DataConnection.this.makeNetworkCapabilities();
                        if (DataConnection.this.mPhone.getLceStatus() == 1) {
                            nc.setLinkDownstreamBandwidthKbps(lceBwDownKbps);
                            if (DataConnection.this.mNetworkAgent != null) {
                                DataConnection.this.mNetworkAgent.sendNetworkCapabilities(nc);
                            }
                        }
                    } else {
                        DataConnection.this.log("EVENT_BW_REFRESH_RESPONSE: error ignoring, e=" + ar2.exception);
                    }
                    break;
                case DataConnection.EVENT_IPV4_ADDRESS_REMOVED:
                    AddressInfo addrV4Info = (AddressInfo) msg.obj;
                    DataConnection.this.log("DcActiveState: " + DataConnection.this.getWhatToString(msg.what) + ": " + addrV4Info);
                    break;
                case DataConnection.EVENT_IPV6_ADDRESS_REMOVED:
                    AddressInfo addrV6Info = (AddressInfo) msg.obj;
                    DataConnection.this.log("DcActiveState: " + DataConnection.this.getWhatToString(msg.what) + ": " + addrV6Info);
                    if (DataConnection.this.mInterfaceName != null && DataConnection.this.mInterfaceName.equals(addrV6Info.mIntfName) && !DataConnection.BSP_PACKAGE) {
                        try {
                            DataConnection.this.mValid = DataConnection.this.mGsmDCTExt.getIPv6Valid(addrV6Info.mLinkAddr);
                        } catch (Exception e) {
                            DataConnection.this.loge("DcActiveState: getIPv6Valid fail!");
                            DataConnection.this.mValid = -1000L;
                            e.printStackTrace();
                        }
                        if (DataConnection.this.mValid == 0 || DataConnection.this.mValid == -1 || DataConnection.this.mValid == -2) {
                            DataConnection.this.log("DcActiveState: RA is failed or life time expired, valid:" + DataConnection.this.mValid);
                            DataConnection.this.onAddressRemoved();
                        }
                    }
                    break;
                case DataConnection.EVENT_VOICE_CALL:
                    DataConnection.this.mIsInVoiceCall = msg.arg1 != 0;
                    DataConnection.this.mIsSupportConcurrent = msg.arg2 != 0;
                    if (DataConnection.this.updateNetworkInfoSuspendState() && DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                    }
                    break;
                case DataConnection.EVENT_FALLBACK_RETRY_CONNECTION:
                    if (msg.arg1 != DataConnection.this.mTag) {
                        DataConnection.this.log("DcActiveState stale EVENT_FALLBACK_RETRY_CONNECTION tag:" + msg.arg1 + " != mTag:" + DataConnection.this.mTag);
                    } else if (DataConnection.this.mDataRegState == 0) {
                        DataConnection.this.log("DcActiveState EVENT_FALLBACK_RETRY_CONNECTION mConnectionParams=" + DataConnection.this.mConnectionParams);
                        DataConnection.this.onConnect(DataConnection.this.mConnectionParams);
                    } else {
                        DataConnection.this.log("DcActiveState: EVENT_FALLBACK_RETRY_CONNECTION not in service");
                    }
                    break;
            }
            return true;
        }
    }

    private class DcDisconnectingState extends State {
        DcDisconnectingState(DataConnection this$0, DcDisconnectingState dcDisconnectingState) {
            this();
        }

        private DcDisconnectingState() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case SmsEnvelope.TELESERVICE_MWI:
                    DataConnection.this.log("DcDisconnectingState msg.what=EVENT_CONNECT. Defer. RefCount = " + DataConnection.this.mApnContexts.size());
                    DataConnection.this.deferMessage(msg);
                    return true;
                case DataConnection.EVENT_SETUP_DATA_CONNECTION_DONE:
                case DataConnection.EVENT_GET_LAST_FAIL_DONE:
                default:
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("DcDisconnectingState not handled msg.what=" + DataConnection.this.getWhatToString(msg.what));
                    }
                    return false;
                case DataConnection.EVENT_DEACTIVATE_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    DisconnectParams dp = (DisconnectParams) ar.userObj;
                    String str = "DcDisconnectingState msg.what=EVENT_DEACTIVATE_DONE RefCount=" + DataConnection.this.mApnContexts.size();
                    DataConnection.this.log(str);
                    if (dp.mApnContext != null) {
                        dp.mApnContext.requestLog(str);
                    }
                    if (dp.mTag == DataConnection.this.mTag) {
                        DataConnection.this.mInactiveState.setEnterNotificationParams((DisconnectParams) ar.userObj);
                        DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                    } else {
                        DataConnection.this.log("DcDisconnectState stale EVENT_DEACTIVATE_DONE dp.tag=" + dp.mTag + " mTag=" + DataConnection.this.mTag);
                    }
                    return true;
            }
        }
    }

    private class DcDisconnectionErrorCreatingConnection extends State {
        DcDisconnectionErrorCreatingConnection(DataConnection this$0, DcDisconnectionErrorCreatingConnection dcDisconnectionErrorCreatingConnection) {
            this();
        }

        private DcDisconnectionErrorCreatingConnection() {
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case DataConnection.EVENT_DEACTIVATE_DONE:
                    AsyncResult ar = (AsyncResult) msg.obj;
                    ConnectionParams cp = (ConnectionParams) ar.userObj;
                    if (cp.mTag == DataConnection.this.mTag) {
                        DataConnection.this.log("DcDisconnectionErrorCreatingConnection msg.what=EVENT_DEACTIVATE_DONE");
                        if (cp.mApnContext != null) {
                            cp.mApnContext.requestLog("DcDisconnectionErrorCreatingConnection msg.what=EVENT_DEACTIVATE_DONE");
                        }
                        DataConnection.this.mInactiveState.setEnterNotificationParams(cp, DcFailCause.UNACCEPTABLE_NETWORK_PARAMETER);
                        DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                    } else {
                        DataConnection.this.log("DcDisconnectionErrorCreatingConnection stale EVENT_DEACTIVATE_DONE dp.tag=" + cp.mTag + ", mTag=" + DataConnection.this.mTag);
                    }
                    return true;
                default:
                    if (DataConnection.VDBG) {
                        DataConnection.this.log("DcDisconnectionErrorCreatingConnection not handled msg.what=" + DataConnection.this.getWhatToString(msg.what));
                    }
                    return false;
            }
        }
    }

    private class DcNetworkAgent extends NetworkAgent {
        public DcNetworkAgent(Looper l, Context c, String TAG, NetworkInfo ni, NetworkCapabilities nc, LinkProperties lp, int score, NetworkMisc misc) {
            super(l, c, TAG, ni, nc, lp, score, misc);
        }

        protected void unwanted() {
            if (DataConnection.this.mNetworkAgent != this) {
                log("DcNetworkAgent: unwanted found mNetworkAgent=" + DataConnection.this.mNetworkAgent + ", which isn't me.  Aborting unwanted");
                return;
            }
            log("DcNetworkAgent unwanted!");
            if (DataConnection.this.mApnContexts == null) {
                return;
            }
            for (ConnectionParams cp : DataConnection.this.mApnContexts.values()) {
                ApnContext apnContext = cp.mApnContext;
                Pair<ApnContext, Integer> pair = new Pair<>(apnContext, Integer.valueOf(cp.mConnectionGeneration));
                log("DcNetworkAgent: [unwanted]: disconnect apnContext=" + apnContext);
                Message msg = DataConnection.this.mDct.obtainMessage(270351, pair);
                DisconnectParams dp = new DisconnectParams(apnContext, apnContext.getReason(), msg);
                DataConnection.this.sendMessage(DataConnection.this.obtainMessage(DataConnection.EVENT_DISCONNECT, dp));
            }
        }

        protected void pollLceData() {
            if (DataConnection.this.mPhone.getLceStatus() != 1) {
                return;
            }
            DataConnection.this.mPhone.mCi.pullLceData(DataConnection.this.obtainMessage(DataConnection.EVENT_BW_REFRESH_RESPONSE));
        }

        protected void networkStatus(int status, String redirectUrl) {
            if (TextUtils.isEmpty(redirectUrl)) {
                return;
            }
            log("validation status: " + status + " with redirection URL: " + redirectUrl);
            Message msg = DataConnection.this.mDct.obtainMessage(270380, redirectUrl);
            AsyncResult.forMessage(msg, DataConnection.this.mApnContexts, (Throwable) null);
            msg.sendToTarget();
        }
    }

    void tearDownNow() {
        log("tearDownNow()");
        sendMessage(obtainMessage(EVENT_TEAR_DOWN_NOW));
    }

    private long getSuggestedRetryDelay(AsyncResult ar) {
        DataCallResponse response = (DataCallResponse) ar.result;
        if (response.suggestedRetryTime < 0) {
            log("No suggested retry delay.");
            DcFailCause cause = DcFailCause.fromInt(response.status);
            if (this.mDcFcMgr == null) {
                return -2L;
            }
            long delay = this.mDcFcMgr.getSuggestedRetryDelayByOp(cause);
            return delay;
        }
        if (response.suggestedRetryTime == Integer.MAX_VALUE) {
            log("Modem suggested not retrying.");
            return -1L;
        }
        return response.suggestedRetryTime;
    }

    protected String getWhatToString(int what) {
        return cmdToString(what);
    }

    private static String msgToString(Message msg) {
        if (msg == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append("{what=");
        b.append(cmdToString(msg.what));
        b.append(" when=");
        TimeUtils.formatDuration(msg.getWhen() - SystemClock.uptimeMillis(), b);
        if (msg.arg1 != 0) {
            b.append(" arg1=");
            b.append(msg.arg1);
        }
        if (msg.arg2 != 0) {
            b.append(" arg2=");
            b.append(msg.arg2);
        }
        if (msg.obj != null) {
            b.append(" obj=");
            b.append(msg.obj);
        }
        b.append(" target=");
        b.append(msg.getTarget());
        b.append(" replyTo=");
        b.append(msg.replyTo);
        b.append("}");
        String retVal = b.toString();
        return retVal;
    }

    static void slog(String s) {
        Rlog.d("DC", s);
    }

    protected void log(String s) {
        Rlog.d(getName(), s);
    }

    protected void logd(String s) {
        Rlog.d(getName(), s);
    }

    protected void logv(String s) {
        Rlog.v(getName(), s);
    }

    protected void logi(String s) {
        Rlog.i(getName(), s);
    }

    protected void logw(String s) {
        Rlog.w(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }

    protected void loge(String s, Throwable e) {
        Rlog.e(getName(), s, e);
    }

    public String toStringSimple() {
        return getName() + ": State=" + getCurrentState().getName() + " mApnSetting=" + this.mApnSetting + " RefCount=" + this.mApnContexts.size() + " mCid=" + this.mCid + " mCreateTime=" + this.mCreateTime + " mLastastFailTime=" + this.mLastFailTime + " mLastFailCause=" + this.mLastFailCause + " mTag=" + this.mTag + " mLinkProperties=" + this.mLinkProperties + " linkCapabilities=" + makeNetworkCapabilities();
    }

    public String toString() {
        return "{" + toStringSimple() + " mApnContexts=" + this.mApnContexts + "}";
    }

    private void dumpToLog() {
        dump(null, new PrintWriter(new StringWriter(0)) {
            @Override
            public void println(String s) {
                DataConnection.this.logd(s);
            }

            @Override
            public void flush() {
            }
        }, null);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("DataConnection ");
        super.dump(fd, pw, args);
        pw.println(" mApnContexts.size=" + this.mApnContexts.size());
        pw.println(" mApnContexts=" + this.mApnContexts);
        pw.flush();
        pw.println(" mDataConnectionTracker=" + this.mDct);
        pw.println(" mApnSetting=" + this.mApnSetting);
        pw.println(" mTag=" + this.mTag);
        pw.println(" mCid=" + this.mCid);
        pw.println(" mConnectionParams=" + this.mConnectionParams);
        pw.println(" mDisconnectParams=" + this.mDisconnectParams);
        pw.println(" mDcFailCause=" + this.mDcFailCause);
        pw.flush();
        pw.println(" mPhone=" + this.mPhone);
        pw.flush();
        pw.println(" mLinkProperties=" + this.mLinkProperties);
        pw.flush();
        pw.println(" mDataRegState=" + this.mDataRegState);
        pw.println(" mRilRat=" + this.mRilRat);
        pw.println(" mNetworkCapabilities=" + makeNetworkCapabilities());
        pw.println(" mCreateTime=" + TimeUtils.logTimeOfDay(this.mCreateTime));
        pw.println(" mLastFailTime=" + TimeUtils.logTimeOfDay(this.mLastFailTime));
        pw.println(" mLastFailCause=" + this.mLastFailCause);
        pw.flush();
        pw.println(" mUserData=" + this.mUserData);
        pw.println(" mInstanceNumber=" + mInstanceNumber);
        pw.println(" mAc=" + this.mAc);
        pw.flush();
    }

    boolean isApnTypeImsOrEmergency(String apnType) {
        if (TextUtils.equals(apnType, ImsSwitchController.IMS_SERVICE) || TextUtils.equals(apnType, "emergency")) {
            return true;
        }
        return false;
    }

    private int getEventByAddress(boolean bUpdated, LinkAddress linkAddr) {
        InetAddress addr = linkAddr.getAddress();
        if (bUpdated) {
            return -1;
        }
        if (addr instanceof Inet6Address) {
            return EVENT_IPV6_ADDRESS_REMOVED;
        }
        if (addr instanceof Inet4Address) {
            return EVENT_IPV4_ADDRESS_REMOVED;
        }
        loge("unknown address type, linkAddr: " + linkAddr);
        return -1;
    }

    private void sendMessageForSM(int event, String iface, LinkAddress address) {
        if (event < 0) {
            loge("sendMessageForSM: Skip notify!!!");
            return;
        }
        AddressInfo addrInfo = new AddressInfo(iface, address);
        log("sendMessageForSM: " + cmdToString(event) + ", addressInfo: " + addrInfo);
        sendMessage(obtainMessage(event, addrInfo));
    }

    private void onAddressRemoved() {
        if (("IPV6".equals(this.mApnSetting.protocol) || "IPV4V6".equals(this.mApnSetting.protocol)) && !isIpv4Connected()) {
            log("onAddressRemoved: IPv6 RA failed and didn't connect with IPv4");
            if (this.mApnContexts == null) {
                return;
            }
            log("onAddressRemoved: mApnContexts size: " + this.mApnContexts.size());
            for (ConnectionParams cp : this.mApnContexts.values()) {
                ApnContext apnContext = cp.mApnContext;
                String apnType = apnContext.getApnType();
                if (apnContext.getState() == DctConstants.State.CONNECTED) {
                    if (isApnTypeImsOrEmergency(apnType)) {
                        log("apnType: " + apnType + ", skip disconnect while onAddressRemoved!!");
                    } else {
                        log("onAddressRemoved: send message EVENT_DISCONNECT_ALL");
                        Pair<ApnContext, Integer> pair = new Pair<>(apnContext, Integer.valueOf(cp.mConnectionGeneration));
                        Message msg = this.mDct.obtainMessage(270351, pair);
                        DisconnectParams dp = new DisconnectParams(apnContext, PhoneInternalInterface.REASON_RA_FAILED, msg);
                        sendMessage(obtainMessage(EVENT_DISCONNECT_ALL, dp));
                        return;
                    }
                }
            }
            return;
        }
        log("onAddressRemoved: no need to remove");
    }

    void checkIfDefaultApnReferenceCountChanged() {
        boolean IsDefaultExisted = false;
        int sizeOfOthers = 0;
        for (ConnectionParams cp : this.mApnContexts.values()) {
            ApnContext apnContext = cp.mApnContext;
            if (TextUtils.equals("default", apnContext.getApnType()) && DctConstants.State.CONNECTED.equals(apnContext.getState())) {
                IsDefaultExisted = true;
            } else if (DctConstants.State.CONNECTED.equals(apnContext.getState())) {
                sizeOfOthers++;
            }
        }
        if (!IsDefaultExisted) {
            return;
        }
        log("refCount = " + this.mApnContexts.size() + ", non-default refCount = " + sizeOfOthers);
        notifyDefaultApnReferenceCountChanged(sizeOfOthers + 1, 270846);
    }

    private boolean isDefaultDataSubPhone(Phone phone) {
        int defaultDataPhoneId = this.mSubController.getPhoneId(this.mSubController.getDefaultDataSubId());
        int curPhoneId = phone.getPhoneId();
        if (defaultDataPhoneId != curPhoneId) {
            log("Current phone is not default phone: curPhoneId = " + curPhoneId + ", defaultDataPhoneId = " + defaultDataPhoneId);
            return false;
        }
        return true;
    }

    private void registerNetworkAlertObserver() {
        if (this.mNetworkManager == null) {
            return;
        }
        log("registerNetworkAlertObserver X");
        try {
            this.mNetworkManager.registerObserver(this.mAlertObserver);
            log("registerNetworkAlertObserver E");
        } catch (RemoteException e) {
            loge("registerNetworkAlertObserver failed E");
        }
    }

    private void unregisterNetworkAlertObserver() {
        if (this.mNetworkManager == null) {
            return;
        }
        log("unregisterNetworkAlertObserver X");
        try {
            this.mNetworkManager.unregisterObserver(this.mAlertObserver);
            log("unregisterNetworkAlertObserver E");
        } catch (RemoteException e) {
            loge("unregisterNetworkAlertObserver failed E");
        }
        this.mInterfaceName = null;
    }

    private boolean isVsimActive() {
        int phoneId = this.mPhone.getPhoneId();
        return this.mDct.isVsimActive(phoneId);
    }

    private class AddressInfo {
        String mIntfName;
        LinkAddress mLinkAddr;

        public AddressInfo(String intfName, LinkAddress linkAddr) {
            this.mIntfName = intfName;
            this.mLinkAddr = linkAddr;
        }

        public String toString() {
            return "interfaceName: " + this.mIntfName + "/" + this.mLinkAddr;
        }
    }

    public void startRetryAlarm(int what, int tag, long delay) {
        Intent intent = new Intent(this.mActionRetry);
        intent.putExtra(INTENT_RETRY_ALARM_WHAT, what);
        intent.putExtra(INTENT_RETRY_ALARM_TAG, tag);
        log("startRetryAlarm: next attempt in " + (delay / 1000) + "s what=" + what + " tag=" + tag);
        PendingIntent retryIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + delay, retryIntent);
    }

    public void startRetryAlarmExact(int what, int tag, long delay) {
        Intent intent = new Intent(this.mActionRetry);
        intent.addFlags(268435456);
        intent.putExtra(INTENT_RETRY_ALARM_WHAT, what);
        intent.putExtra(INTENT_RETRY_ALARM_TAG, tag);
        log("startRetryAlarmExact: next attempt in " + (delay / 1000) + "s what=" + what + " tag=" + tag);
        PendingIntent retryIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + delay, retryIntent);
    }

    public void resetRetryCount() {
        this.mRetryCount = 0;
        log("resetRetryCount: " + this.mRetryCount);
    }

    public void handlePcscfErrorCause(DisconnectParams dp) {
        CarrierConfigManager configMgr = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        int subId = this.mPhone.getSubId();
        if (configMgr == null) {
            loge("handlePcscfErrorCause() null configMgr!");
            return;
        }
        PersistableBundle b = configMgr.getConfigForSubId(subId);
        if (b == null) {
            loge("handlePcscfErrorCause() null config!");
            return;
        }
        boolean syncFailCause = b.getBoolean("ims_pdn_sync_fail_cause_to_modem_bool");
        log("handlePcscfErrorCause() syncFailCause: " + syncFailCause + " mPcscfAddr: " + this.mPcscfAddr + " with subId: " + subId);
        if (syncFailCause) {
            String apnType = dp.mApnContext.getApnType();
            if (TextUtils.equals(apnType, ImsSwitchController.IMS_SERVICE)) {
                if (this.mPcscfAddr == null || this.mPcscfAddr.length <= 0) {
                    dp.mReason = PhoneInternalInterface.REASON_PCSCF_ADDRESS_FAILED;
                    log("DcActiveState: Disconnect with empty P-CSCF address");
                }
            }
        }
    }
}
