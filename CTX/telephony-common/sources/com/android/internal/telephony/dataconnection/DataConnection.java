package com.android.internal.telephony.dataconnection;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.hardware.radio.V1_0.RadioAccessFamily;
import android.hardware.radio.V1_2.ScanIntervalRange;
import android.net.KeepalivePacketData;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkMisc;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StringNetworkSpecifier;
import android.os.AsyncResult;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.data.DataCallResponse;
import android.telephony.data.DataProfile;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Pair;
import android.util.SparseArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.LinkCapacityEstimate;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class DataConnection extends StateMachine {
    protected static final int BASE = 262144;
    protected static final int CMD_TO_STRING_COUNT = 24;
    private static final boolean DBG = true;
    protected static final int EVENT_BW_REFRESH_RESPONSE = 262158;
    protected static final int EVENT_CONNECT = 262144;
    protected static final int EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED = 262155;
    protected static final int EVENT_DATA_CONNECTION_OVERRIDE_CHANGED = 262161;
    protected static final int EVENT_DATA_CONNECTION_ROAM_OFF = 262157;
    protected static final int EVENT_DATA_CONNECTION_ROAM_ON = 262156;
    protected static final int EVENT_DATA_CONNECTION_VOICE_CALL_ENDED = 262160;
    protected static final int EVENT_DATA_CONNECTION_VOICE_CALL_STARTED = 262159;
    public static final int EVENT_DATA_STATE_CHANGED = 262151;
    protected static final int EVENT_DEACTIVATE_DONE = 262147;
    protected static final int EVENT_DISCONNECT = 262148;
    protected static final int EVENT_DISCONNECT_ALL = 262150;
    protected static final int EVENT_KEEPALIVE_STARTED = 262163;
    protected static final int EVENT_KEEPALIVE_START_REQUEST = 262165;
    protected static final int EVENT_KEEPALIVE_STATUS = 262162;
    protected static final int EVENT_KEEPALIVE_STOPPED = 262164;
    protected static final int EVENT_KEEPALIVE_STOP_REQUEST = 262166;
    protected static final int EVENT_LINK_CAPACITY_CHANGED = 262167;
    public static final int EVENT_LOST_CONNECTION = 262153;
    public static final int EVENT_RIL_CONNECTED = 262149;
    protected static final int EVENT_SETUP_DATA_CONNECTION_DONE = 262145;
    protected static final int EVENT_TEAR_DOWN_NOW = 262152;
    protected static final String NETWORK_TYPE = "MOBILE";
    private static final String NULL_IP = "0.0.0.0";
    private static final String TCP_BUFFER_SIZES_1XRTT = "16384,32768,131072,4096,16384,102400";
    private static final String TCP_BUFFER_SIZES_EDGE = "4093,26280,70800,4096,16384,70800";
    private static final String TCP_BUFFER_SIZES_EHRPD = "131072,262144,1048576,4096,16384,524288";
    private static final String TCP_BUFFER_SIZES_EVDO = "4094,87380,262144,4096,16384,262144";
    private static final String TCP_BUFFER_SIZES_GPRS = "4092,8760,48000,4096,8760,48000";
    private static final String TCP_BUFFER_SIZES_HSDPA = "61167,367002,1101005,8738,52429,262114";
    private static final String TCP_BUFFER_SIZES_HSPA = "40778,244668,734003,16777,100663,301990";
    private static final String TCP_BUFFER_SIZES_HSPAP = "122334,734003,2202010,32040,192239,576717";
    public static String TCP_BUFFER_SIZES_LTE = null;
    private static final String TCP_BUFFER_SIZES_UMTS = "58254,349525,1048576,58254,349525,1048576";
    private static final boolean VDBG = true;
    protected static AtomicInteger mInstanceNumber = new AtomicInteger(0);
    protected static String[] sCmdToString = new String[24];
    protected AsyncChannel mAc;
    protected DcActivatingState mActivatingState;
    protected DcActiveState mActiveState;
    public HashMap<ApnContext, ConnectionParams> mApnContexts;
    protected ApnSetting mApnSetting;
    public int mCid;
    protected ConnectionParams mConnectionParams;
    protected long mCreateTime;
    protected int mDataRegState;
    protected DataServiceManager mDataServiceManager;
    protected DcController mDcController;
    protected DcFailCause mDcFailCause;
    protected DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    protected DcTracker mDct;
    protected DcDefaultState mDefaultState;
    protected DisconnectParams mDisconnectParams;
    protected DcDisconnectionErrorCreatingConnection mDisconnectingErrorCreatingConnection;
    protected DcDisconnectingState mDisconnectingState;
    protected int mId;
    protected DcInactiveState mInactiveState;
    protected DcFailCause mLastFailCause;
    protected long mLastFailTime;
    protected LinkProperties mLinkProperties;
    private LocalLog mNetCapsLocalLog;
    protected DcNetworkAgent mNetworkAgent;
    protected NetworkInfo mNetworkInfo;
    protected String[] mPcscfAddr;
    protected Phone mPhone;
    protected PendingIntent mReconnectIntent;
    protected boolean mRestrictedNetworkOverride;
    protected int mRilRat;
    protected int mSubscriptionOverride;
    public int mTag;
    protected Object mUserData;

    static {
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
        TCP_BUFFER_SIZES_LTE = "524288,1048576,2097152,262144,524288,1048576";
    }

    public static class ConnectionParams {
        public ApnContext mApnContext;
        public final int mConnectionGeneration;
        public Message mOnCompletedMsg;
        public int mProfileId;
        public int mRilRat;
        public int mTag;
        public final boolean mUnmeteredUseOnly;

        ConnectionParams(ApnContext apnContext, int i, int i2, boolean z, Message message, int i3) {
            this.mApnContext = apnContext;
            this.mProfileId = i;
            this.mRilRat = i2;
            this.mUnmeteredUseOnly = z;
            this.mOnCompletedMsg = message;
            this.mConnectionGeneration = i3;
        }

        public String toString() {
            return "{mTag=" + this.mTag + " mApnContext=" + this.mApnContext + " mProfileId=" + this.mProfileId + " mRat=" + this.mRilRat + " mUnmeteredUseOnly=" + this.mUnmeteredUseOnly + " mOnCompletedMsg=" + DataConnection.msgToString(this.mOnCompletedMsg) + "}";
        }
    }

    public static class DisconnectParams {
        public ApnContext mApnContext;
        public Message mOnCompletedMsg;
        public String mReason;
        public int mTag;

        public DisconnectParams(ApnContext apnContext, String str, Message message) {
            this.mApnContext = apnContext;
            this.mReason = str;
            this.mOnCompletedMsg = message;
        }

        public String toString() {
            return "{mTag=" + this.mTag + " mApnContext=" + this.mApnContext + " mReason=" + this.mReason + " mOnCompletedMsg=" + DataConnection.msgToString(this.mOnCompletedMsg) + "}";
        }
    }

    protected static String cmdToString(int i) {
        String strCmdToString;
        int i2 = i - InboundSmsTracker.DEST_PORT_FLAG_3GPP2;
        if (i2 >= 0 && i2 < sCmdToString.length) {
            strCmdToString = sCmdToString[i2];
        } else {
            strCmdToString = DcAsyncChannel.cmdToString(i2 + InboundSmsTracker.DEST_PORT_FLAG_3GPP2);
        }
        if (strCmdToString == null) {
            return "0x" + Integer.toHexString(i2 + InboundSmsTracker.DEST_PORT_FLAG_3GPP2);
        }
        return strCmdToString;
    }

    public static DataConnection makeDataConnection(Phone phone, int i, DcTracker dcTracker, DataServiceManager dataServiceManager, DcTesterFailBringUpAll dcTesterFailBringUpAll, DcController dcController) {
        DataConnection dataConnectionMakeDataConnection = TelephonyComponentFactory.getInstance().makeDataConnection(phone, "DC-" + mInstanceNumber.incrementAndGet(), i, dcTracker, dataServiceManager, dcTesterFailBringUpAll, dcController);
        dataConnectionMakeDataConnection.start();
        dataConnectionMakeDataConnection.log("Made " + dataConnectionMakeDataConnection.getName());
        return dataConnectionMakeDataConnection;
    }

    void dispose() {
        log("dispose: call quiteNow()");
        quitNow();
    }

    LinkProperties getCopyLinkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    boolean isInactive() {
        return getCurrentState() == this.mInactiveState;
    }

    boolean isDisconnecting() {
        return getCurrentState() == this.mDisconnectingState;
    }

    boolean isActive() {
        return getCurrentState() == this.mActiveState;
    }

    boolean isActivating() {
        return getCurrentState() == this.mActivatingState;
    }

    int getCid() {
        return this.mCid;
    }

    ApnSetting getApnSetting() {
        return this.mApnSetting;
    }

    public void setLinkPropertiesHttpProxy(ProxyInfo proxyInfo) {
        this.mLinkProperties.setHttpProxy(proxyInfo);
    }

    public static class UpdateLinkPropertyResult {
        public LinkProperties newLp;
        public LinkProperties oldLp;
        public SetupResult setupResult = SetupResult.SUCCESS;

        public UpdateLinkPropertyResult(LinkProperties linkProperties) {
            this.oldLp = linkProperties;
            this.newLp = linkProperties;
        }
    }

    public enum SetupResult {
        SUCCESS,
        ERROR_RADIO_NOT_AVAILABLE,
        ERROR_INVALID_ARG,
        ERROR_STALE,
        ERROR_DATA_SERVICE_SPECIFIC_ERROR;

        public DcFailCause mFailCause = DcFailCause.fromInt(0);

        SetupResult() {
        }

        @Override
        public String toString() {
            return name() + "  SetupResult.mFailCause=" + this.mFailCause;
        }
    }

    public boolean isIpv4Connected() {
        for (InetAddress inetAddress : this.mLinkProperties.getAddresses()) {
            if (inetAddress instanceof Inet4Address) {
                Inet4Address inet4Address = (Inet4Address) inetAddress;
                if (!inet4Address.isAnyLocalAddress() && !inet4Address.isLinkLocalAddress() && !inet4Address.isLoopbackAddress() && !inet4Address.isMulticastAddress()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isIpv6Connected() {
        for (InetAddress inetAddress : this.mLinkProperties.getAddresses()) {
            if (inetAddress instanceof Inet6Address) {
                Inet6Address inet6Address = (Inet6Address) inetAddress;
                if (!inet6Address.isAnyLocalAddress() && !inet6Address.isLinkLocalAddress() && !inet6Address.isLoopbackAddress() && !inet6Address.isMulticastAddress()) {
                    return true;
                }
            }
        }
        return false;
    }

    @VisibleForTesting
    public UpdateLinkPropertyResult updateLinkProperty(DataCallResponse dataCallResponse) {
        UpdateLinkPropertyResult updateLinkPropertyResult = new UpdateLinkPropertyResult(this.mLinkProperties);
        if (dataCallResponse == null) {
            return updateLinkPropertyResult;
        }
        updateLinkPropertyResult.newLp = new LinkProperties();
        updateLinkPropertyResult.setupResult = setLinkProperties(dataCallResponse, updateLinkPropertyResult.newLp);
        if (updateLinkPropertyResult.setupResult != SetupResult.SUCCESS) {
            log("updateLinkProperty failed : " + updateLinkPropertyResult.setupResult);
            return updateLinkPropertyResult;
        }
        updateLinkPropertyResult.newLp.setHttpProxy(this.mLinkProperties.getHttpProxy());
        checkSetMtu(this.mApnSetting, updateLinkPropertyResult.newLp);
        this.mLinkProperties = updateLinkPropertyResult.newLp;
        updateTcpBufferSizes(this.mRilRat);
        if (!updateLinkPropertyResult.oldLp.equals(updateLinkPropertyResult.newLp)) {
            log("updateLinkProperty old LP=" + updateLinkPropertyResult.oldLp);
            log("updateLinkProperty new LP=" + updateLinkPropertyResult.newLp);
        }
        if (!updateLinkPropertyResult.newLp.equals(updateLinkPropertyResult.oldLp) && this.mNetworkAgent != null) {
            this.mNetworkAgent.sendLinkProperties(mtkGetLinkProperties());
        }
        return updateLinkPropertyResult;
    }

    protected void checkSetMtu(ApnSetting apnSetting, LinkProperties linkProperties) {
        if (linkProperties == null || apnSetting == null || linkProperties == null) {
            return;
        }
        if (linkProperties.getMtu() != 0) {
            log("MTU set by call response to: " + linkProperties.getMtu());
            return;
        }
        if (apnSetting != null && apnSetting.mtu != 0) {
            linkProperties.setMtu(apnSetting.mtu);
            log("MTU set by APN to: " + apnSetting.mtu);
            return;
        }
        int integer = this.mPhone.getContext().getResources().getInteger(R.integer.config_defaultPreventScreenTimeoutForMillis);
        if (integer != 0) {
            linkProperties.setMtu(integer);
            log("MTU set by config resource to: " + integer);
        }
    }

    public DataConnection(Phone phone, String str, int i, DcTracker dcTracker, DataServiceManager dataServiceManager, DcTesterFailBringUpAll dcTesterFailBringUpAll, DcController dcController) {
        super(str, dcController.getHandler());
        this.mDct = null;
        this.mLinkProperties = new LinkProperties();
        this.mRilRat = KeepaliveStatus.INVALID_HANDLE;
        this.mDataRegState = KeepaliveStatus.INVALID_HANDLE;
        this.mNetCapsLocalLog = new LocalLog(50);
        this.mApnContexts = null;
        this.mReconnectIntent = null;
        this.mRestrictedNetworkOverride = false;
        this.mDefaultState = new DcDefaultState();
        this.mInactiveState = new DcInactiveState();
        this.mActivatingState = new DcActivatingState();
        this.mActiveState = new DcActiveState();
        this.mDisconnectingState = new DcDisconnectingState();
        this.mDisconnectingErrorCreatingConnection = new DcDisconnectionErrorCreatingConnection();
        setLogRecSize(ScanIntervalRange.MAX);
        setLogOnlyTransitions(true);
        log("DataConnection created");
        this.mPhone = phone;
        this.mDct = dcTracker;
        this.mDataServiceManager = dataServiceManager;
        this.mDcTesterFailBringUpAll = dcTesterFailBringUpAll;
        this.mDcController = dcController;
        this.mId = i;
        this.mCid = -1;
        ServiceState serviceState = this.mPhone.getServiceState();
        this.mRilRat = serviceState.getRilDataRadioTechnology();
        this.mDataRegState = this.mPhone.getServiceState().getDataRegState();
        int dataNetworkType = serviceState.getDataNetworkType();
        this.mNetworkInfo = new NetworkInfo(0, dataNetworkType, NETWORK_TYPE, TelephonyManager.getNetworkTypeName(dataNetworkType));
        this.mNetworkInfo.setRoaming(serviceState.getDataRoaming());
        this.mNetworkInfo.setIsAvailable(true);
        mtkReplaceStates();
        addState(this.mDefaultState);
        addState(this.mInactiveState, this.mDefaultState);
        addState(this.mActivatingState, this.mDefaultState);
        addState(this.mActiveState, this.mDefaultState);
        addState(this.mDisconnectingState, this.mDefaultState);
        addState(this.mDisconnectingErrorCreatingConnection, this.mDefaultState);
        setInitialState(this.mInactiveState);
        this.mApnContexts = new HashMap<>();
    }

    protected void onConnect(ConnectionParams connectionParams) {
        log("onConnect: carrier='" + this.mApnSetting.carrier + "' APN='" + this.mApnSetting.apn + "' proxy='" + this.mApnSetting.proxy + "' port='" + this.mApnSetting.port + "'");
        if (connectionParams.mApnContext != null) {
            connectionParams.mApnContext.requestLog("DataConnection.onConnect");
        }
        boolean z = true;
        if (this.mDcTesterFailBringUpAll.getDcFailBringUp().mCounter > 0) {
            DataCallResponse dataCallResponse = new DataCallResponse(this.mDcTesterFailBringUpAll.getDcFailBringUp().mFailCause.getErrorCode(), this.mDcTesterFailBringUpAll.getDcFailBringUp().mSuggestedRetryTime, 0, 0, "", "", (List) null, (List) null, (List) null, (List) null, 0);
            Message messageObtainMessage = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, connectionParams);
            AsyncResult.forMessage(messageObtainMessage, dataCallResponse, (Throwable) null);
            sendMessage(messageObtainMessage);
            log("onConnect: FailBringUpAll=" + this.mDcTesterFailBringUpAll.getDcFailBringUp() + " send error response=" + dataCallResponse);
            DcFailBringUp dcFailBringUp = this.mDcTesterFailBringUpAll.getDcFailBringUp();
            dcFailBringUp.mCounter = dcFailBringUp.mCounter - 1;
            return;
        }
        this.mCreateTime = -1L;
        this.mLastFailTime = -1L;
        this.mLastFailCause = DcFailCause.NONE;
        Message messageObtainMessage2 = obtainMessage(EVENT_SETUP_DATA_CONNECTION_DONE, connectionParams);
        messageObtainMessage2.obj = connectionParams;
        DataProfile dataProfileCreateDataProfile = DcTracker.createDataProfile(this.mApnSetting, connectionParams.mProfileId);
        boolean dataRoamingFromRegistration = this.mPhone.getServiceState().getDataRoamingFromRegistration();
        if (!this.mPhone.getDataRoamingEnabled() && (!dataRoamingFromRegistration || this.mPhone.getServiceState().getDataRoaming())) {
            z = false;
        }
        this.mDataServiceManager.setupDataCall(ServiceState.rilRadioTechnologyToAccessNetworkType(connectionParams.mRilRat), dataProfileCreateDataProfile, dataRoamingFromRegistration, z, 1, null, messageObtainMessage2);
        TelephonyMetrics.getInstance().writeSetupDataCall(this.mPhone.getPhoneId(), connectionParams.mRilRat, dataProfileCreateDataProfile.getProfileId(), dataProfileCreateDataProfile.getApn(), dataProfileCreateDataProfile.getProtocol());
    }

    public void onSubscriptionOverride(int i, int i2) {
        this.mSubscriptionOverride = (i & i2) | (this.mSubscriptionOverride & (~i));
        sendMessage(obtainMessage(EVENT_DATA_CONNECTION_OVERRIDE_CHANGED));
    }

    protected void tearDownData(Object obj) {
        ApnContext apnContext;
        int i = 1;
        if (obj != null && (obj instanceof DisconnectParams)) {
            DisconnectParams disconnectParams = (DisconnectParams) obj;
            apnContext = disconnectParams.mApnContext;
            if (TextUtils.equals(disconnectParams.mReason, PhoneInternalInterface.REASON_RADIO_TURNED_OFF) || TextUtils.equals(disconnectParams.mReason, PhoneInternalInterface.REASON_PDP_RESET)) {
                i = 2;
            }
        } else {
            apnContext = null;
        }
        String str = "tearDownData. mCid=" + this.mCid + ", reason=" + i;
        log(str);
        if (apnContext != null) {
            apnContext.requestLog(str);
        }
        this.mDataServiceManager.deactivateDataCall(this.mCid, i, obtainMessage(EVENT_DEACTIVATE_DONE, this.mTag, 0, obj));
    }

    protected void notifyAllWithEvent(ApnContext apnContext, int i, String str) {
        this.mNetworkInfo.setDetailedState(this.mNetworkInfo.getDetailedState(), str, this.mNetworkInfo.getExtraInfo());
        for (ConnectionParams connectionParams : this.mApnContexts.values()) {
            ApnContext apnContext2 = connectionParams.mApnContext;
            if (apnContext2 != apnContext) {
                if (str != null) {
                    apnContext2.setReason(str);
                }
                Message messageObtainMessage = this.mDct.obtainMessage(i, new Pair(apnContext2, Integer.valueOf(connectionParams.mConnectionGeneration)));
                AsyncResult.forMessage(messageObtainMessage);
                messageObtainMessage.sendToTarget();
            }
        }
    }

    protected void notifyAllOfConnected(String str) {
        notifyAllWithEvent(null, 270336, str);
    }

    private void notifyAllOfDisconnectDcRetrying(String str) {
        notifyAllWithEvent(null, 270370, str);
    }

    private void notifyAllDisconnectCompleted(DcFailCause dcFailCause) {
        notifyAllWithEvent(null, 270351, dcFailCause.toString());
    }

    protected void notifyConnectCompleted(ConnectionParams connectionParams, DcFailCause dcFailCause, boolean z) {
        ApnContext apnContext = null;
        if (connectionParams != null && connectionParams.mOnCompletedMsg != null) {
            Message message = connectionParams.mOnCompletedMsg;
            connectionParams.mOnCompletedMsg = null;
            apnContext = connectionParams.mApnContext;
            long jCurrentTimeMillis = System.currentTimeMillis();
            message.arg1 = this.mCid;
            if (dcFailCause == DcFailCause.NONE) {
                this.mCreateTime = jCurrentTimeMillis;
                AsyncResult.forMessage(message);
            } else {
                this.mLastFailCause = dcFailCause;
                this.mLastFailTime = jCurrentTimeMillis;
                if (dcFailCause == null) {
                    dcFailCause = DcFailCause.UNKNOWN;
                }
                AsyncResult.forMessage(message, dcFailCause, new Throwable(dcFailCause.toString()));
            }
            log("notifyConnectCompleted at " + jCurrentTimeMillis + " cause=" + dcFailCause + " connectionCompletedMsg=" + msgToString(message));
            message.sendToTarget();
        }
        if (z) {
            log("Send to all. " + apnContext + " " + dcFailCause.toString());
            notifyAllWithEvent(apnContext, 270371, dcFailCause.toString());
        }
    }

    protected void notifyDisconnectCompleted(DisconnectParams disconnectParams, boolean z) {
        ApnContext apnContext;
        log("NotifyDisconnectCompleted");
        String string = null;
        ApnContext apnContext2 = null;
        if (disconnectParams == null || disconnectParams.mOnCompletedMsg == null) {
            apnContext = null;
        } else {
            Message message = disconnectParams.mOnCompletedMsg;
            disconnectParams.mOnCompletedMsg = null;
            if (message.obj instanceof ApnContext) {
                apnContext2 = (ApnContext) message.obj;
                mtkSetApnContextReason(apnContext2, disconnectParams.mReason);
            }
            String str = disconnectParams.mReason;
            Object[] objArr = new Object[2];
            objArr[0] = message.toString();
            objArr[1] = message.obj instanceof String ? (String) message.obj : "<no-reason>";
            log(String.format("msg=%s msg.obj=%s", objArr));
            AsyncResult.forMessage(message);
            message.sendToTarget();
            apnContext = apnContext2;
            string = str;
        }
        if (z) {
            if (string == null) {
                string = DcFailCause.UNKNOWN.toString();
            }
            notifyAllWithEvent(apnContext, 270351, string);
        }
        log("NotifyDisconnectCompleted DisconnectParams=" + disconnectParams);
    }

    public int getDataConnectionId() {
        return this.mId;
    }

    protected void clearSettings() {
        log("clearSettings");
        this.mCreateTime = -1L;
        this.mLastFailTime = -1L;
        this.mLastFailCause = DcFailCause.NONE;
        this.mCid = -1;
        this.mPcscfAddr = new String[5];
        this.mLinkProperties = new LinkProperties();
        this.mApnContexts.clear();
        this.mApnSetting = null;
        this.mDcFailCause = null;
    }

    protected SetupResult onSetupConnectionCompleted(int i, DataCallResponse dataCallResponse, ConnectionParams connectionParams) {
        if (connectionParams.mTag != this.mTag) {
            log("onSetupConnectionCompleted stale cp.tag=" + connectionParams.mTag + ", mtag=" + this.mTag);
            return SetupResult.ERROR_STALE;
        }
        if (i == 4) {
            SetupResult setupResult = SetupResult.ERROR_RADIO_NOT_AVAILABLE;
            setupResult.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
            return setupResult;
        }
        if (dataCallResponse.getStatus() != 0) {
            if (dataCallResponse.getStatus() == DcFailCause.RADIO_NOT_AVAILABLE.getErrorCode()) {
                SetupResult setupResult2 = SetupResult.ERROR_RADIO_NOT_AVAILABLE;
                setupResult2.mFailCause = DcFailCause.RADIO_NOT_AVAILABLE;
                return setupResult2;
            }
            SetupResult setupResult3 = SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
            setupResult3.mFailCause = DcFailCause.fromInt(dataCallResponse.getStatus());
            return setupResult3;
        }
        log("onSetupConnectionCompleted received successful DataCallResponse");
        this.mCid = dataCallResponse.getCallId();
        this.mPcscfAddr = (String[]) dataCallResponse.getPcscfs().toArray(new String[dataCallResponse.getPcscfs().size()]);
        return updateLinkProperty(dataCallResponse).setupResult;
    }

    private boolean isDnsOk(String[] strArr) {
        if (!NULL_IP.equals(strArr[0]) || !NULL_IP.equals(strArr[1]) || this.mPhone.isDnsCheckDisabled() || (this.mApnSetting.types[0].equals("mms") && isIpAddress(this.mApnSetting.mmsProxy))) {
            return true;
        }
        log(String.format("isDnsOk: return false apn.types[0]=%s APN_TYPE_MMS=%s isIpAddress(%s)=%s", this.mApnSetting.types[0], "mms", this.mApnSetting.mmsProxy, Boolean.valueOf(isIpAddress(this.mApnSetting.mmsProxy))));
        return false;
    }

    protected void updateTcpBufferSizes(int i) {
        String str;
        if (i == 19) {
            i = 14;
        }
        String lowerCase = ServiceState.rilRadioTechnologyToString(i).toLowerCase(Locale.ROOT);
        if (i == 7 || i == 8 || i == 12) {
            lowerCase = "evdo";
        }
        String[] stringArray = this.mPhone.getContext().getResources().getStringArray(R.array.config_clockTickVibePattern);
        int i2 = 0;
        while (true) {
            if (i2 < stringArray.length) {
                String[] strArrSplit = stringArray[i2].split(":");
                if (!lowerCase.equals(strArrSplit[0]) || strArrSplit.length != 2) {
                    i2++;
                } else {
                    str = strArrSplit[1];
                    break;
                }
            } else {
                str = null;
                break;
            }
        }
        if (str == null) {
            if (i != 19) {
                switch (i) {
                    case 1:
                        str = TCP_BUFFER_SIZES_GPRS;
                        break;
                    case 2:
                        str = TCP_BUFFER_SIZES_EDGE;
                        break;
                    case 3:
                        str = TCP_BUFFER_SIZES_UMTS;
                        break;
                    default:
                        switch (i) {
                            case 6:
                                str = TCP_BUFFER_SIZES_1XRTT;
                                break;
                            case 7:
                            case 8:
                            case 12:
                                str = TCP_BUFFER_SIZES_EVDO;
                                break;
                            case 9:
                                str = TCP_BUFFER_SIZES_HSDPA;
                                break;
                            case 10:
                            case 11:
                                str = TCP_BUFFER_SIZES_HSPA;
                                break;
                            case 13:
                                str = TCP_BUFFER_SIZES_EHRPD;
                                break;
                            case 14:
                                str = TCP_BUFFER_SIZES_LTE;
                                break;
                            case 15:
                                str = TCP_BUFFER_SIZES_HSPAP;
                                break;
                        }
                        break;
                }
            }
        }
        this.mLinkProperties.setTcpBufferSizes(str);
    }

    protected void setNetworkRestriction() {
        this.mRestrictedNetworkOverride = false;
        Iterator<ApnContext> it = this.mApnContexts.keySet().iterator();
        boolean zHasNoRestrictedRequests = true;
        while (it.hasNext()) {
            zHasNoRestrictedRequests &= it.next().hasNoRestrictedRequests(true);
        }
        if (zHasNoRestrictedRequests || !this.mApnSetting.isMetered(this.mPhone)) {
            return;
        }
        this.mRestrictedNetworkOverride = !this.mDct.isDataEnabled();
    }

    protected NetworkCapabilities getNetworkCapabilities() {
        NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        networkCapabilities.addTransportType(0);
        if (this.mApnSetting != null) {
            for (String str : this.mApnSetting.types) {
                if (this.mRestrictedNetworkOverride || this.mConnectionParams == null || !this.mConnectionParams.mUnmeteredUseOnly || !ApnSetting.isMeteredApnType(str, this.mPhone)) {
                    switch (str) {
                        case "*":
                            networkCapabilities.addCapability(12);
                            networkCapabilities.addCapability(0);
                            networkCapabilities.addCapability(1);
                            networkCapabilities.addCapability(3);
                            networkCapabilities.addCapability(4);
                            networkCapabilities.addCapability(5);
                            networkCapabilities.addCapability(7);
                            networkCapabilities.addCapability(2);
                            break;
                        case "default":
                            networkCapabilities.addCapability(12);
                            break;
                        case "mms":
                            networkCapabilities.addCapability(0);
                            break;
                        case "supl":
                            networkCapabilities.addCapability(1);
                            break;
                        case "dun":
                            networkCapabilities.addCapability(2);
                            break;
                        case "fota":
                            networkCapabilities.addCapability(3);
                            break;
                        case "ims":
                            networkCapabilities.addCapability(4);
                            break;
                        case "cbs":
                            networkCapabilities.addCapability(5);
                            break;
                        case "ia":
                            networkCapabilities.addCapability(7);
                            break;
                        case "emergency":
                            networkCapabilities.addCapability(10);
                            break;
                    }
                } else {
                    log("Dropped the metered " + str + " for the unmetered data call.");
                }
            }
            if ((this.mConnectionParams == null || !this.mConnectionParams.mUnmeteredUseOnly || this.mRestrictedNetworkOverride) && this.mApnSetting.isMetered(this.mPhone)) {
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
        int i = this.mRilRat;
        int i2 = 1843;
        int i3 = 102400;
        if (i != 19) {
            switch (i) {
                case 1:
                    i2 = 80;
                    i3 = 80;
                    break;
                case 2:
                    i2 = 59;
                    i3 = 236;
                    break;
                case 3:
                    i2 = 384;
                    i3 = 384;
                    break;
                case 4:
                case 5:
                default:
                    i2 = 14;
                    i3 = 14;
                    break;
                case 6:
                    i2 = 100;
                    i3 = 100;
                    break;
                case 7:
                    i3 = 2457;
                    i2 = 153;
                    break;
                case 8:
                    i3 = 3174;
                    break;
                case 9:
                    i2 = RadioAccessFamily.HSPA;
                    i3 = 14336;
                    break;
                case 10:
                    i2 = 5898;
                    i3 = 14336;
                    break;
                case 11:
                    i2 = 5898;
                    i3 = 14336;
                    break;
                case 12:
                    i3 = 5017;
                    break;
                case 13:
                    i3 = 2516;
                    i2 = 153;
                    break;
                case 14:
                    i2 = 51200;
                    break;
                case 15:
                    i2 = 11264;
                    i3 = 43008;
                    break;
            }
        }
        networkCapabilities.setLinkUpstreamBandwidthKbps(i2);
        networkCapabilities.setLinkDownstreamBandwidthKbps(i3);
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

    @VisibleForTesting
    public static boolean isIpAddress(String str) {
        if (str == null) {
            return false;
        }
        return InetAddress.isNumeric(str);
    }

    protected SetupResult setLinkProperties(DataCallResponse dataCallResponse, LinkProperties linkProperties) {
        SetupResult setupResult;
        String str = "net." + dataCallResponse.getIfname() + ".";
        String[] strArr = {SystemProperties.get(str + "dns1"), SystemProperties.get(str + "dns2")};
        boolean zIsDnsOk = isDnsOk(strArr);
        linkProperties.clear();
        if (dataCallResponse.getStatus() == DcFailCause.NONE.getErrorCode()) {
            try {
                linkProperties.setInterfaceName(dataCallResponse.getIfname());
                if (dataCallResponse.getAddresses().size() > 0) {
                    for (LinkAddress linkAddress : dataCallResponse.getAddresses()) {
                        if (!linkAddress.getAddress().isAnyLocalAddress()) {
                            log("addr/pl=" + linkAddress.getAddress() + "/" + linkAddress.getNetworkPrefixLength());
                            linkProperties.addLinkAddress(linkAddress);
                        }
                    }
                    if (dataCallResponse.getDnses().size() > 0) {
                        for (InetAddress inetAddress : dataCallResponse.getDnses()) {
                            if (!inetAddress.isAnyLocalAddress()) {
                                linkProperties.addDnsServer(inetAddress);
                            }
                        }
                    } else {
                        if (!zIsDnsOk) {
                            throw new UnknownHostException("Empty dns response and no system default dns");
                        }
                        for (String str2 : strArr) {
                            String strTrim = str2.trim();
                            if (!strTrim.isEmpty()) {
                                try {
                                    InetAddress inetAddressNumericToInetAddress = NetworkUtils.numericToInetAddress(strTrim);
                                    if (!inetAddressNumericToInetAddress.isAnyLocalAddress()) {
                                        linkProperties.addDnsServer(inetAddressNumericToInetAddress);
                                    }
                                } catch (IllegalArgumentException e) {
                                    throw new UnknownHostException("Non-numeric dns addr=" + strTrim);
                                }
                            }
                        }
                    }
                    Iterator it = dataCallResponse.getGateways().iterator();
                    while (it.hasNext()) {
                        linkProperties.addRoute(new RouteInfo((InetAddress) it.next()));
                    }
                    linkProperties.setMtu(dataCallResponse.getMtu());
                    setupResult = SetupResult.SUCCESS;
                } else {
                    throw new UnknownHostException("no address for ifname=" + dataCallResponse.getIfname());
                }
            } catch (UnknownHostException e2) {
                log("setLinkProperties: UnknownHostException " + e2);
                setupResult = SetupResult.ERROR_INVALID_ARG;
            }
        } else {
            setupResult = SetupResult.ERROR_DATA_SERVICE_SPECIFIC_ERROR;
        }
        if (setupResult != SetupResult.SUCCESS) {
            log("setLinkProperties: error clearing LinkProperties status=" + dataCallResponse.getStatus() + " result=" + setupResult);
            linkProperties.clear();
        }
        return setupResult;
    }

    protected boolean initConnection(ConnectionParams connectionParams) {
        ApnContext apnContext = connectionParams.mApnContext;
        if (this.mApnSetting == null) {
            this.mApnSetting = apnContext.getApnSetting();
        }
        if (this.mApnSetting == null || !this.mApnSetting.canHandleType(apnContext.getApnType())) {
            log("initConnection: incompatible apnSetting in ConnectionParams cp=" + connectionParams + " dc=" + this);
            return false;
        }
        this.mTag++;
        this.mConnectionParams = connectionParams;
        this.mConnectionParams.mTag = this.mTag;
        mtkCheckDefaultApnRefCount(apnContext);
        this.mApnContexts.put(apnContext, connectionParams);
        log("initConnection:  RefCount=" + this.mApnContexts.size() + " mApnList=" + this.mApnContexts + " mConnectionParams=" + this.mConnectionParams);
        return true;
    }

    protected class DcDefaultState extends State {
        protected DcDefaultState() {
        }

        public void enter() {
            DataConnection.this.log("DcDefaultState: enter");
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED, null);
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRoamingOn(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_ROAM_ON, null);
            DataConnection.this.mPhone.getServiceStateTracker().registerForDataRoamingOff(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF, null, true);
            DataConnection.this.mDcController.addDc(DataConnection.this);
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
            DataConnection.this.mDataServiceManager = null;
            DataConnection.this.mLinkProperties = null;
            DataConnection.this.mLastFailCause = null;
            DataConnection.this.mUserData = null;
            DataConnection.this.mDcController = null;
            DataConnection.this.mDcTesterFailBringUpAll = null;
        }

        public boolean processMessage(Message message) {
            DataConnection.this.log("DcDefault msg=" + DataConnection.this.getWhatToString(message.what) + " RefCount=" + DataConnection.this.mApnContexts.size());
            switch (message.what) {
                case 69633:
                    if (DataConnection.this.mAc != null) {
                        DataConnection.this.log("Disconnecting to previous connection mAc=" + DataConnection.this.mAc);
                        DataConnection.this.mAc.replyToMessage(message, 69634, 3);
                    } else {
                        DataConnection.this.mAc = new AsyncChannel();
                        DataConnection.this.mAc.connected((Context) null, DataConnection.this.getHandler(), message.replyTo);
                        DataConnection.this.log("DcDefaultState: FULL_CONNECTION reply connected");
                        DataConnection.this.mAc.replyToMessage(message, 69634, 0, DataConnection.this.mId, "hi");
                    }
                    return true;
                case 69636:
                    DataConnection.this.log("DcDefault: CMD_CHANNEL_DISCONNECTED before quiting call dump");
                    DataConnection.this.dumpToLog();
                    DataConnection.this.quit();
                    return true;
                case InboundSmsTracker.DEST_PORT_FLAG_3GPP2:
                    DataConnection.this.log("DcDefaultState: msg.what=EVENT_CONNECT, fail not expected");
                    DataConnection.this.notifyConnectCompleted((ConnectionParams) message.obj, DcFailCause.UNKNOWN, false);
                    return true;
                case DataConnection.EVENT_DISCONNECT:
                    DataConnection.this.log("DcDefaultState deferring msg.what=EVENT_DISCONNECT RefCount=" + DataConnection.this.mApnContexts.size());
                    DataConnection.this.deferMessage(message);
                    return true;
                case DataConnection.EVENT_DISCONNECT_ALL:
                    DataConnection.this.log("DcDefaultState deferring msg.what=EVENT_DISCONNECT_ALL RefCount=" + DataConnection.this.mApnContexts.size());
                    DataConnection.this.deferMessage(message);
                    return true;
                case DataConnection.EVENT_TEAR_DOWN_NOW:
                    DataConnection.this.log("DcDefaultState EVENT_TEAR_DOWN_NOW");
                    DataConnection.this.mDataServiceManager.deactivateDataCall(DataConnection.this.mCid, 1, null);
                    return true;
                case DataConnection.EVENT_LOST_CONNECTION:
                    DataConnection.this.logAndAddLogRec("DcDefaultState ignore EVENT_LOST_CONNECTION tag=" + message.arg1 + ":mTag=" + DataConnection.this.mTag);
                    return true;
                case DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED:
                    Pair pair = (Pair) ((AsyncResult) message.obj).result;
                    DataConnection.this.mDataRegState = ((Integer) pair.first).intValue();
                    if (DataConnection.this.mRilRat != ((Integer) pair.second).intValue()) {
                        DataConnection.this.updateTcpBufferSizes(((Integer) pair.second).intValue());
                    }
                    DataConnection.this.mRilRat = ((Integer) pair.second).intValue();
                    DataConnection.this.log("DcDefaultState: EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED drs=" + DataConnection.this.mDataRegState + " mRilRat=" + DataConnection.this.mRilRat);
                    DataConnection.this.updateNetworkInfo();
                    DataConnection.this.updateNetworkInfoSuspendState();
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.getNetworkCapabilities());
                        DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                        DataConnection.this.mNetworkAgent.sendLinkProperties(DataConnection.this.mtkGetLinkProperties());
                    }
                    return true;
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_ON:
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF:
                case DataConnection.EVENT_DATA_CONNECTION_OVERRIDE_CHANGED:
                    DataConnection.this.updateNetworkInfo();
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.getNetworkCapabilities());
                        DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                    }
                    return true;
                case DataConnection.EVENT_KEEPALIVE_START_REQUEST:
                case DataConnection.EVENT_KEEPALIVE_STOP_REQUEST:
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.onPacketKeepaliveEvent(message.arg1, -20);
                    }
                    return true;
                case 266240:
                    boolean zIsInactive = DataConnection.this.isInactive();
                    DataConnection.this.log("REQ_IS_INACTIVE  isInactive=" + zIsInactive);
                    DataConnection.this.mAc.replyToMessage(message, DcAsyncChannel.RSP_IS_INACTIVE, zIsInactive ? 1 : 0);
                    return true;
                case DcAsyncChannel.REQ_GET_CID:
                    int cid = DataConnection.this.getCid();
                    DataConnection.this.log("REQ_GET_CID  cid=" + cid);
                    DataConnection.this.mAc.replyToMessage(message, DcAsyncChannel.RSP_GET_CID, cid);
                    return true;
                case DcAsyncChannel.REQ_GET_APNSETTING:
                    ApnSetting apnSetting = DataConnection.this.getApnSetting();
                    DataConnection.this.log("REQ_GET_APNSETTING  mApnSetting=" + apnSetting);
                    DataConnection.this.mAc.replyToMessage(message, DcAsyncChannel.RSP_GET_APNSETTING, apnSetting);
                    return true;
                case DcAsyncChannel.REQ_GET_LINK_PROPERTIES:
                    LinkProperties copyLinkProperties = DataConnection.this.getCopyLinkProperties();
                    DataConnection.this.log("REQ_GET_LINK_PROPERTIES linkProperties" + copyLinkProperties);
                    DataConnection.this.mAc.replyToMessage(message, DcAsyncChannel.RSP_GET_LINK_PROPERTIES, copyLinkProperties);
                    return true;
                case DcAsyncChannel.REQ_SET_LINK_PROPERTIES_HTTP_PROXY:
                    ProxyInfo proxyInfo = (ProxyInfo) message.obj;
                    DataConnection.this.log("REQ_SET_LINK_PROPERTIES_HTTP_PROXY proxy=" + proxyInfo);
                    DataConnection.this.setLinkPropertiesHttpProxy(proxyInfo);
                    DataConnection.this.mAc.replyToMessage(message, DcAsyncChannel.RSP_SET_LINK_PROPERTIES_HTTP_PROXY);
                    if (DataConnection.this.mNetworkAgent != null) {
                        DataConnection.this.mNetworkAgent.sendLinkProperties(DataConnection.this.mtkGetLinkProperties());
                    }
                    return true;
                case DcAsyncChannel.REQ_GET_NETWORK_CAPABILITIES:
                    NetworkCapabilities networkCapabilities = DataConnection.this.getNetworkCapabilities();
                    DataConnection.this.log("REQ_GET_NETWORK_CAPABILITIES networkCapabilities" + networkCapabilities);
                    DataConnection.this.mAc.replyToMessage(message, DcAsyncChannel.RSP_GET_NETWORK_CAPABILITIES, networkCapabilities);
                    return true;
                case DcAsyncChannel.REQ_RESET:
                    DataConnection.this.log("DcDefaultState: msg.what=REQ_RESET");
                    DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                    return true;
                default:
                    DataConnection.this.log("DcDefaultState: shouldn't happen but ignore msg.what=" + DataConnection.this.getWhatToString(message.what));
                    return true;
            }
        }
    }

    protected void updateNetworkInfo() {
        ServiceState serviceState = this.mPhone.getServiceState();
        int dataNetworkType = serviceState.getDataNetworkType();
        this.mNetworkInfo.setSubtype(dataNetworkType, TelephonyManager.getNetworkTypeName(dataNetworkType));
        this.mNetworkInfo.setRoaming(serviceState.getDataRoaming());
    }

    protected void updateNetworkInfoSuspendState() {
        if (this.mNetworkAgent == null) {
            Rlog.e(getName(), "Setting suspend state without a NetworkAgent");
        }
        ServiceStateTracker serviceStateTracker = this.mPhone.getServiceStateTracker();
        if (serviceStateTracker.getCurrentDataConnectionState() != 0) {
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, null, this.mNetworkInfo.getExtraInfo());
        } else if (!serviceStateTracker.isConcurrentVoiceAndDataAllowed() && this.mPhone.getCallTracker().getState() != PhoneConstants.State.IDLE) {
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.SUSPENDED, null, this.mNetworkInfo.getExtraInfo());
        } else {
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, this.mNetworkInfo.getExtraInfo());
        }
    }

    protected class DcInactiveState extends State {
        protected DcInactiveState() {
        }

        public void setEnterNotificationParams(ConnectionParams connectionParams, DcFailCause dcFailCause) {
            DataConnection.this.log("DcInactiveState: setEnterNotificationParams cp,cause");
            DataConnection.this.mConnectionParams = connectionParams;
            DataConnection.this.mDisconnectParams = null;
            DataConnection.this.mDcFailCause = dcFailCause;
        }

        public void setEnterNotificationParams(DisconnectParams disconnectParams) {
            DataConnection.this.log("DcInactiveState: setEnterNotificationParams dp");
            DataConnection.this.mConnectionParams = null;
            DataConnection.this.mDisconnectParams = disconnectParams;
            DataConnection.this.mDcFailCause = DcFailCause.NONE;
        }

        public void setEnterNotificationParams(DcFailCause dcFailCause) {
            DataConnection.this.mConnectionParams = null;
            DataConnection.this.mDisconnectParams = null;
            DataConnection.this.mDcFailCause = dcFailCause;
        }

        public void enter() {
            DataConnection.this.mTag++;
            DataConnection.this.log("DcInactiveState: enter() mTag=" + DataConnection.this.mTag);
            StatsLog.write(75, 1, DataConnection.this.mPhone.getPhoneId(), DataConnection.this.mId, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.typesBitmap : 0L, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.canHandleType("default") : false);
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

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 262144) {
                DataConnection.this.log("DcInactiveState: mag.what=EVENT_CONNECT");
                ConnectionParams connectionParams = (ConnectionParams) message.obj;
                if (DataConnection.this.initConnection(connectionParams)) {
                    DataConnection.this.onConnect(DataConnection.this.mConnectionParams);
                    DataConnection.this.transitionTo(DataConnection.this.mActivatingState);
                    return true;
                }
                DataConnection.this.log("DcInactiveState: msg.what=EVENT_CONNECT initConnection failed");
                DataConnection.this.notifyConnectCompleted(connectionParams, DcFailCause.UNACCEPTABLE_NETWORK_PARAMETER, false);
                return true;
            }
            if (i == DataConnection.EVENT_DISCONNECT) {
                DataConnection.this.log("DcInactiveState: msg.what=EVENT_DISCONNECT");
                DataConnection.this.notifyDisconnectCompleted((DisconnectParams) message.obj, false);
                return true;
            }
            if (i == DataConnection.EVENT_DISCONNECT_ALL) {
                DataConnection.this.log("DcInactiveState: msg.what=EVENT_DISCONNECT_ALL");
                DataConnection.this.notifyDisconnectCompleted((DisconnectParams) message.obj, false);
                return true;
            }
            if (i == 266252) {
                DataConnection.this.log("DcInactiveState: msg.what=RSP_RESET, ignore we're already reset");
                return true;
            }
            DataConnection.this.log("DcInactiveState nothandled msg.what=" + DataConnection.this.getWhatToString(message.what));
            return false;
        }
    }

    protected class DcActivatingState extends State {
        protected DcActivatingState() {
        }

        public void enter() {
            StatsLog.write(75, 2, DataConnection.this.mPhone.getPhoneId(), DataConnection.this.mId, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.typesBitmap : 0L, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.canHandleType("default") : false);
        }

        public boolean processMessage(Message message) {
            DataConnection.this.log("DcActivatingState: msg=" + DataConnection.msgToString(message));
            int i = message.what;
            if (i != DataConnection.EVENT_DATA_CONNECTION_DRS_OR_RAT_CHANGED) {
                switch (i) {
                    case InboundSmsTracker.DEST_PORT_FLAG_3GPP2:
                        break;
                    case DataConnection.EVENT_SETUP_DATA_CONNECTION_DONE:
                        ConnectionParams connectionParams = (ConnectionParams) message.obj;
                        DataCallResponse parcelable = message.getData().getParcelable(DataServiceManager.DATA_CALL_RESPONSE);
                        SetupResult setupResultOnSetupConnectionCompleted = DataConnection.this.onSetupConnectionCompleted(message.arg1, parcelable, connectionParams);
                        if (setupResultOnSetupConnectionCompleted != SetupResult.ERROR_STALE && DataConnection.this.mConnectionParams != connectionParams) {
                            DataConnection.this.loge("DcActivatingState: WEIRD mConnectionsParams:" + DataConnection.this.mConnectionParams + " != cp:" + connectionParams);
                        }
                        DataConnection.this.log("DcActivatingState onSetupConnectionCompleted result=" + setupResultOnSetupConnectionCompleted + " dc=" + DataConnection.this);
                        if (connectionParams.mApnContext != null) {
                            connectionParams.mApnContext.requestLog("onSetupConnectionCompleted result=" + setupResultOnSetupConnectionCompleted);
                        }
                        switch (setupResultOnSetupConnectionCompleted) {
                            case SUCCESS:
                                DataConnection.this.mDcFailCause = DcFailCause.NONE;
                                DataConnection.this.transitionTo(DataConnection.this.mActiveState);
                                return true;
                            case ERROR_RADIO_NOT_AVAILABLE:
                                DataConnection.this.mInactiveState.setEnterNotificationParams(connectionParams, setupResultOnSetupConnectionCompleted.mFailCause);
                                DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                                return true;
                            case ERROR_INVALID_ARG:
                                DataConnection.this.tearDownData(connectionParams);
                                DataConnection.this.transitionTo(DataConnection.this.mDisconnectingErrorCreatingConnection);
                                return true;
                            case ERROR_DATA_SERVICE_SPECIFIC_ERROR:
                                long suggestedRetryDelay = DataConnection.this.getSuggestedRetryDelay(parcelable);
                                connectionParams.mApnContext.setModemSuggestedDelay(suggestedRetryDelay);
                                String str = "DcActivatingState: ERROR_DATA_SERVICE_SPECIFIC_ERROR  delay=" + suggestedRetryDelay + " result=" + setupResultOnSetupConnectionCompleted + " result.isRestartRadioFail=" + setupResultOnSetupConnectionCompleted.mFailCause.isRestartRadioFail(DataConnection.this.mPhone.getContext(), DataConnection.this.mPhone.getSubId()) + " isPermanentFailure=" + DataConnection.this.mDct.isPermanentFailure(setupResultOnSetupConnectionCompleted.mFailCause);
                                DataConnection.this.log(str);
                                if (connectionParams.mApnContext != null) {
                                    connectionParams.mApnContext.requestLog(str);
                                }
                                DataConnection.this.mInactiveState.setEnterNotificationParams(connectionParams, setupResultOnSetupConnectionCompleted.mFailCause);
                                DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                                return true;
                            case ERROR_STALE:
                                DataConnection.this.loge("DcActivatingState: stale EVENT_SETUP_DATA_CONNECTION_DONE tag:" + connectionParams.mTag + " != mTag:" + DataConnection.this.mTag);
                                return true;
                            default:
                                throw new RuntimeException("Unknown SetupResult, should not happen");
                        }
                    default:
                        DataConnection.this.log("DcActivatingState not handled msg.what=" + DataConnection.this.getWhatToString(message.what) + " RefCount=" + DataConnection.this.mApnContexts.size());
                        return false;
                }
            }
            DataConnection.this.deferMessage(message);
            return true;
        }
    }

    protected class DcActiveState extends State {
        protected DcActiveState() {
        }

        public void enter() {
            DataConnection.this.log("DcActiveState: enter dc=" + DataConnection.this);
            StatsLog.write(75, 3, DataConnection.this.mPhone.getPhoneId(), DataConnection.this.mId, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.typesBitmap : 0L, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.canHandleType("default") : false);
            DataConnection.this.updateNetworkInfo();
            DataConnection.this.notifyAllOfConnected(PhoneInternalInterface.REASON_CONNECTED);
            DataConnection.this.mPhone.getCallTracker().registerForVoiceCallStarted(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_STARTED, null);
            DataConnection.this.mPhone.getCallTracker().registerForVoiceCallEnded(DataConnection.this.getHandler(), DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_ENDED, null);
            DataConnection.this.mDcController.addActiveDcByCid(DataConnection.this);
            DataConnection.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, DataConnection.this.mNetworkInfo.getReason(), null);
            DataConnection.this.mNetworkInfo.setExtraInfo(DataConnection.this.mApnSetting.apn);
            DataConnection.this.updateTcpBufferSizes(DataConnection.this.mRilRat);
            NetworkMisc networkMisc = new NetworkMisc();
            if (DataConnection.this.mPhone.getCarrierSignalAgent().hasRegisteredReceivers("com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED")) {
                networkMisc.provisioningNotificationDisabled = true;
            }
            networkMisc.subscriberId = DataConnection.this.mPhone.getSubscriberId();
            DataConnection.this.setNetworkRestriction();
            DataConnection.this.log("mRestrictedNetworkOverride = " + DataConnection.this.mRestrictedNetworkOverride);
            DataConnection.this.mNetworkAgent = DataConnection.this.new DcNetworkAgent(DataConnection.this.getHandler().getLooper(), DataConnection.this.mPhone.getContext(), "DcNetworkAgent", DataConnection.this.mNetworkInfo, DataConnection.this.getNetworkCapabilities(), DataConnection.this.mLinkProperties, 50, networkMisc);
            DataConnection.this.mPhone.mCi.registerForNattKeepaliveStatus(DataConnection.this.getHandler(), DataConnection.EVENT_KEEPALIVE_STATUS, null);
            DataConnection.this.mPhone.mCi.registerForLceInfo(DataConnection.this.getHandler(), DataConnection.EVENT_LINK_CAPACITY_CHANGED, null);
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
            DataConnection.this.mPhone.getCallTracker().unregisterForVoiceCallStarted(DataConnection.this.getHandler());
            DataConnection.this.mPhone.getCallTracker().unregisterForVoiceCallEnded(DataConnection.this.getHandler());
            DataConnection.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, reason, DataConnection.this.mNetworkInfo.getExtraInfo());
            DataConnection.this.mPhone.mCi.unregisterForNattKeepaliveStatus(DataConnection.this.getHandler());
            DataConnection.this.mPhone.mCi.unregisterForLceInfo(DataConnection.this.getHandler());
            if (DataConnection.this.mNetworkAgent != null) {
                DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                DataConnection.this.mNetworkAgent = null;
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 262144) {
                ConnectionParams connectionParams = (ConnectionParams) message.obj;
                DataConnection.this.mApnContexts.put(connectionParams.mApnContext, connectionParams);
                DataConnection.this.log("DcActiveState: EVENT_CONNECT cp=" + connectionParams + " dc=" + DataConnection.this);
                DataConnection.this.notifyConnectCompleted(connectionParams, DcFailCause.NONE, false);
                return true;
            }
            if (i == DataConnection.EVENT_DISCONNECT) {
                DisconnectParams disconnectParams = (DisconnectParams) message.obj;
                DataConnection.this.log("DcActiveState: EVENT_DISCONNECT dp=" + disconnectParams + " dc=" + DataConnection.this);
                if (DataConnection.this.mApnContexts.containsKey(disconnectParams.mApnContext)) {
                    DataConnection.this.log("DcActiveState msg.what=EVENT_DISCONNECT RefCount=" + DataConnection.this.mApnContexts.size());
                    if (DataConnection.this.mApnContexts.size() == 1) {
                        DataConnection.this.mApnContexts.clear();
                        DataConnection.this.mDisconnectParams = disconnectParams;
                        DataConnection.this.mConnectionParams = null;
                        disconnectParams.mTag = DataConnection.this.mTag;
                        DataConnection.this.tearDownData(disconnectParams);
                        DataConnection.this.transitionTo(DataConnection.this.mDisconnectingState);
                        return true;
                    }
                    DataConnection.this.mApnContexts.remove(disconnectParams.mApnContext);
                    DataConnection.this.notifyDisconnectCompleted(disconnectParams, false);
                    return true;
                }
                DataConnection.this.log("DcActiveState ERROR no such apnContext=" + disconnectParams.mApnContext + " in this dc=" + DataConnection.this);
                DataConnection.this.notifyDisconnectCompleted(disconnectParams, false);
                return true;
            }
            if (i == DataConnection.EVENT_DISCONNECT_ALL) {
                DataConnection.this.log("DcActiveState EVENT_DISCONNECT clearing apn contexts, dc=" + DataConnection.this);
                DisconnectParams disconnectParams2 = (DisconnectParams) message.obj;
                DataConnection.this.mDisconnectParams = disconnectParams2;
                DataConnection.this.mConnectionParams = null;
                disconnectParams2.mTag = DataConnection.this.mTag;
                DataConnection.this.tearDownData(disconnectParams2);
                DataConnection.this.transitionTo(DataConnection.this.mDisconnectingState);
                return true;
            }
            if (i == 262153) {
                DataConnection.this.log("DcActiveState EVENT_LOST_CONNECTION dc=" + DataConnection.this);
                DataConnection.this.mInactiveState.setEnterNotificationParams(DcFailCause.LOST_CONNECTION);
                DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                return true;
            }
            switch (i) {
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_ON:
                case DataConnection.EVENT_DATA_CONNECTION_ROAM_OFF:
                case DataConnection.EVENT_DATA_CONNECTION_OVERRIDE_CHANGED:
                    DataConnection.this.updateNetworkInfo();
                    if (DataConnection.this.mNetworkAgent == null) {
                        return true;
                    }
                    DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.getNetworkCapabilities());
                    DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                    return true;
                case DataConnection.EVENT_BW_REFRESH_RESPONSE:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception != null) {
                        DataConnection.this.log("EVENT_BW_REFRESH_RESPONSE: error ignoring, e=" + asyncResult.exception);
                        return true;
                    }
                    LinkCapacityEstimate linkCapacityEstimate = (LinkCapacityEstimate) asyncResult.result;
                    NetworkCapabilities networkCapabilities = DataConnection.this.getNetworkCapabilities();
                    if (DataConnection.this.mPhone.getLceStatus() != 1) {
                        return true;
                    }
                    networkCapabilities.setLinkDownstreamBandwidthKbps(linkCapacityEstimate.downlinkCapacityKbps);
                    if (DataConnection.this.mNetworkAgent == null) {
                        return true;
                    }
                    DataConnection.this.mNetworkAgent.sendNetworkCapabilities(networkCapabilities);
                    return true;
                case DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_STARTED:
                case DataConnection.EVENT_DATA_CONNECTION_VOICE_CALL_ENDED:
                    DataConnection.this.updateNetworkInfo();
                    DataConnection.this.updateNetworkInfoSuspendState();
                    if (DataConnection.this.mNetworkAgent == null) {
                        return true;
                    }
                    DataConnection.this.mNetworkAgent.sendNetworkCapabilities(DataConnection.this.getNetworkCapabilities());
                    DataConnection.this.mNetworkAgent.sendNetworkInfo(DataConnection.this.mNetworkInfo);
                    return true;
                case DataConnection.EVENT_KEEPALIVE_STATUS:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    if (asyncResult2.exception != null) {
                        DataConnection.this.loge("EVENT_KEEPALIVE_STATUS: error in keepalive, e=" + asyncResult2.exception);
                    }
                    if (asyncResult2.result == null) {
                        return true;
                    }
                    DataConnection.this.mNetworkAgent.keepaliveTracker.handleKeepaliveStatus((KeepaliveStatus) asyncResult2.result);
                    return true;
                case DataConnection.EVENT_KEEPALIVE_STARTED:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    int i2 = message.arg1;
                    if (asyncResult3.exception != null || asyncResult3.result == null) {
                        DataConnection.this.loge("EVENT_KEEPALIVE_STARTED: error starting keepalive, e=" + asyncResult3.exception);
                        DataConnection.this.mNetworkAgent.onPacketKeepaliveEvent(i2, -31);
                        return true;
                    }
                    KeepaliveStatus keepaliveStatus = (KeepaliveStatus) asyncResult3.result;
                    if (keepaliveStatus == null) {
                        DataConnection.this.loge("Null KeepaliveStatus received!");
                        return true;
                    }
                    DataConnection.this.mNetworkAgent.keepaliveTracker.handleKeepaliveStarted(i2, keepaliveStatus);
                    return true;
                case DataConnection.EVENT_KEEPALIVE_STOPPED:
                    AsyncResult asyncResult4 = (AsyncResult) message.obj;
                    int i3 = message.arg1;
                    int i4 = message.arg2;
                    if (asyncResult4.exception != null) {
                        DataConnection.this.loge("EVENT_KEEPALIVE_STOPPED: error stopping keepalive for handle=" + i3 + " e=" + asyncResult4.exception);
                        DataConnection.this.mNetworkAgent.keepaliveTracker.handleKeepaliveStatus(new KeepaliveStatus(3));
                        return true;
                    }
                    DataConnection.this.log("Keepalive Stop Requested for handle=" + i3);
                    DataConnection.this.mNetworkAgent.keepaliveTracker.handleKeepaliveStatus(new KeepaliveStatus(i3, 1));
                    return true;
                case DataConnection.EVENT_KEEPALIVE_START_REQUEST:
                    KeepalivePacketData keepalivePacketData = (KeepalivePacketData) message.obj;
                    int i5 = message.arg1;
                    int i6 = message.arg2 * 1000;
                    if (DataConnection.this.mDataServiceManager.getTransportType() == 1) {
                        DataConnection.this.mPhone.mCi.startNattKeepalive(DataConnection.this.mCid, keepalivePacketData, i6, DataConnection.this.obtainMessage(DataConnection.EVENT_KEEPALIVE_STARTED, i5, 0, null));
                        return true;
                    }
                    if (DataConnection.this.mNetworkAgent == null) {
                        return true;
                    }
                    DataConnection.this.mNetworkAgent.onPacketKeepaliveEvent(message.arg1, -20);
                    return true;
                case DataConnection.EVENT_KEEPALIVE_STOP_REQUEST:
                    int i7 = message.arg1;
                    int handleForSlot = DataConnection.this.mNetworkAgent.keepaliveTracker.getHandleForSlot(i7);
                    if (handleForSlot < 0) {
                        DataConnection.this.loge("No slot found for stopPacketKeepalive! " + i7);
                        return true;
                    }
                    DataConnection.this.logd("Stopping keepalive with handle: " + handleForSlot);
                    DataConnection.this.mPhone.mCi.stopNattKeepalive(handleForSlot, DataConnection.this.obtainMessage(DataConnection.EVENT_KEEPALIVE_STOPPED, handleForSlot, i7, null));
                    return true;
                case DataConnection.EVENT_LINK_CAPACITY_CHANGED:
                    AsyncResult asyncResult5 = (AsyncResult) message.obj;
                    if (asyncResult5.exception != null) {
                        DataConnection.this.loge("EVENT_LINK_CAPACITY_CHANGED e=" + asyncResult5.exception);
                        return true;
                    }
                    LinkCapacityEstimate linkCapacityEstimate2 = (LinkCapacityEstimate) asyncResult5.result;
                    NetworkCapabilities networkCapabilities2 = DataConnection.this.getNetworkCapabilities();
                    if (linkCapacityEstimate2.downlinkCapacityKbps != -1) {
                        networkCapabilities2.setLinkDownstreamBandwidthKbps(linkCapacityEstimate2.downlinkCapacityKbps);
                    }
                    if (linkCapacityEstimate2.uplinkCapacityKbps != -1) {
                        networkCapabilities2.setLinkUpstreamBandwidthKbps(linkCapacityEstimate2.uplinkCapacityKbps);
                    }
                    if (DataConnection.this.mNetworkAgent == null) {
                        return true;
                    }
                    DataConnection.this.mNetworkAgent.sendNetworkCapabilities(networkCapabilities2);
                    return true;
                default:
                    DataConnection.this.log("DcActiveState not handled msg.what=" + DataConnection.this.getWhatToString(message.what));
                    return false;
            }
        }
    }

    protected class DcDisconnectingState extends State {
        protected DcDisconnectingState() {
        }

        public void enter() {
            StatsLog.write(75, 4, DataConnection.this.mPhone.getPhoneId(), DataConnection.this.mId, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.typesBitmap : 0L, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.canHandleType("default") : false);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 262144) {
                DataConnection.this.log("DcDisconnectingState msg.what=EVENT_CONNECT. Defer. RefCount = " + DataConnection.this.mApnContexts.size());
                DataConnection.this.deferMessage(message);
                return true;
            }
            if (i == DataConnection.EVENT_DEACTIVATE_DONE) {
                DisconnectParams disconnectParams = (DisconnectParams) message.obj;
                String str = "DcDisconnectingState msg.what=EVENT_DEACTIVATE_DONE RefCount=" + DataConnection.this.mApnContexts.size();
                DataConnection.this.log(str);
                if (disconnectParams.mApnContext != null) {
                    disconnectParams.mApnContext.requestLog(str);
                }
                if (disconnectParams.mTag == DataConnection.this.mTag) {
                    DataConnection.this.mInactiveState.setEnterNotificationParams(disconnectParams);
                    DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                    return true;
                }
                DataConnection.this.log("DcDisconnectState stale EVENT_DEACTIVATE_DONE dp.tag=" + disconnectParams.mTag + " mTag=" + DataConnection.this.mTag);
                return true;
            }
            DataConnection.this.log("DcDisconnectingState not handled msg.what=" + DataConnection.this.getWhatToString(message.what));
            return false;
        }
    }

    protected class DcDisconnectionErrorCreatingConnection extends State {
        protected DcDisconnectionErrorCreatingConnection() {
        }

        public void enter() {
            StatsLog.write(75, 5, DataConnection.this.mPhone.getPhoneId(), DataConnection.this.mId, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.typesBitmap : 0L, DataConnection.this.mApnSetting != null ? DataConnection.this.mApnSetting.canHandleType("default") : false);
        }

        public boolean processMessage(Message message) {
            if (message.what == DataConnection.EVENT_DEACTIVATE_DONE) {
                ConnectionParams connectionParams = (ConnectionParams) message.obj;
                if (connectionParams.mTag == DataConnection.this.mTag) {
                    DataConnection.this.log("DcDisconnectionErrorCreatingConnection msg.what=EVENT_DEACTIVATE_DONE");
                    if (connectionParams.mApnContext != null) {
                        connectionParams.mApnContext.requestLog("DcDisconnectionErrorCreatingConnection msg.what=EVENT_DEACTIVATE_DONE");
                    }
                    DataConnection.this.mInactiveState.setEnterNotificationParams(connectionParams, DcFailCause.UNACCEPTABLE_NETWORK_PARAMETER);
                    DataConnection.this.transitionTo(DataConnection.this.mInactiveState);
                } else {
                    DataConnection.this.log("DcDisconnectionErrorCreatingConnection stale EVENT_DEACTIVATE_DONE dp.tag=" + connectionParams.mTag + ", mTag=" + DataConnection.this.mTag);
                }
                return true;
            }
            DataConnection.this.log("DcDisconnectionErrorCreatingConnection not handled msg.what=" + DataConnection.this.getWhatToString(message.what));
            return false;
        }
    }

    protected class DcNetworkAgent extends NetworkAgent {
        public final DcKeepaliveTracker keepaliveTracker;
        private NetworkCapabilities mNetworkCapabilities;

        public DcNetworkAgent(Looper looper, Context context, String str, NetworkInfo networkInfo, NetworkCapabilities networkCapabilities, LinkProperties linkProperties, int i, NetworkMisc networkMisc) {
            super(looper, context, str, networkInfo, networkCapabilities, linkProperties, i, networkMisc);
            this.keepaliveTracker = new DcKeepaliveTracker();
            DataConnection.this.mNetCapsLocalLog.log("New network agent created. capabilities=" + networkCapabilities);
            this.mNetworkCapabilities = networkCapabilities;
        }

        protected void unwanted() {
            if (DataConnection.this.mNetworkAgent != this) {
                log("DcNetworkAgent: unwanted found mNetworkAgent=" + DataConnection.this.mNetworkAgent + ", which isn't me.  Aborting unwanted");
                return;
            }
            if (DataConnection.this.mApnContexts == null) {
                return;
            }
            for (ConnectionParams connectionParams : DataConnection.this.mApnContexts.values()) {
                ApnContext apnContext = connectionParams.mApnContext;
                Pair pair = new Pair(apnContext, Integer.valueOf(connectionParams.mConnectionGeneration));
                log("DcNetworkAgent: [unwanted]: disconnect apnContext=" + apnContext);
                DataConnection.this.sendMessage(DataConnection.this.obtainMessage(DataConnection.EVENT_DISCONNECT, new DisconnectParams(apnContext, apnContext.getReason(), DataConnection.this.mDct.obtainMessage(270351, pair))));
            }
        }

        protected void pollLceData() {
            if (DataConnection.this.mPhone.getLceStatus() == 1) {
                DataConnection.this.mPhone.mCi.pullLceData(DataConnection.this.obtainMessage(DataConnection.EVENT_BW_REFRESH_RESPONSE));
            }
        }

        protected void networkStatus(int i, String str) {
            if (!TextUtils.isEmpty(str)) {
                log("validation status: " + i + " with redirection URL: " + str);
                DataConnection.this.mDct.obtainMessage(270380, str).sendToTarget();
            }
        }

        public void sendNetworkCapabilities(NetworkCapabilities networkCapabilities) {
            if (!networkCapabilities.equals(this.mNetworkCapabilities)) {
                String str = "Changed from " + this.mNetworkCapabilities + " to " + networkCapabilities + ", Data RAT=" + DataConnection.this.mPhone.getServiceState().getRilDataRadioTechnology() + ", mApnSetting=" + DataConnection.this.mApnSetting;
                DataConnection.this.mNetCapsLocalLog.log(str);
                log(str);
                this.mNetworkCapabilities = networkCapabilities;
            }
            super.sendNetworkCapabilities(networkCapabilities);
        }

        protected void startPacketKeepalive(Message message) {
            DataConnection.this.obtainMessage(DataConnection.EVENT_KEEPALIVE_START_REQUEST, message.arg1, message.arg2, message.obj).sendToTarget();
        }

        protected void stopPacketKeepalive(Message message) {
            DataConnection.this.obtainMessage(DataConnection.EVENT_KEEPALIVE_STOP_REQUEST, message.arg1, message.arg2, message.obj).sendToTarget();
        }

        private class DcKeepaliveTracker {
            private final SparseArray<KeepaliveRecord> mKeepalives;

            private DcKeepaliveTracker() {
                this.mKeepalives = new SparseArray<>();
            }

            private class KeepaliveRecord {
                public int currentStatus;
                public int slotId;

                KeepaliveRecord(int i, int i2) {
                    this.slotId = i;
                    this.currentStatus = i2;
                }
            }

            int getHandleForSlot(int i) {
                for (int i2 = 0; i2 < this.mKeepalives.size(); i2++) {
                    if (this.mKeepalives.valueAt(i2).slotId == i) {
                        return this.mKeepalives.keyAt(i2);
                    }
                }
                return -1;
            }

            int keepaliveStatusErrorToPacketKeepaliveError(int i) {
                switch (i) {
                    case 0:
                        return 0;
                    case 1:
                        return -30;
                    default:
                        return -31;
                }
            }

            void handleKeepaliveStarted(int i, KeepaliveStatus keepaliveStatus) {
                switch (keepaliveStatus.statusCode) {
                    case 0:
                        DcNetworkAgent.this.onPacketKeepaliveEvent(i, 0);
                        break;
                    case 1:
                        DcNetworkAgent.this.onPacketKeepaliveEvent(i, keepaliveStatusErrorToPacketKeepaliveError(keepaliveStatus.errorCode));
                        return;
                    case 2:
                        break;
                    default:
                        DataConnection.this.loge("Invalid KeepaliveStatus Code: " + keepaliveStatus.statusCode);
                        return;
                }
                DcNetworkAgent.this.log("Adding keepalive handle=" + keepaliveStatus.sessionHandle + " slot = " + i);
                this.mKeepalives.put(keepaliveStatus.sessionHandle, new KeepaliveRecord(i, keepaliveStatus.statusCode));
            }

            void handleKeepaliveStatus(KeepaliveStatus keepaliveStatus) {
                KeepaliveRecord keepaliveRecord = this.mKeepalives.get(keepaliveStatus.sessionHandle);
                if (keepaliveRecord == null) {
                    DcNetworkAgent.this.log("Discarding keepalive event for different data connection:" + keepaliveStatus);
                }
                switch (keepaliveRecord.currentStatus) {
                    case 0:
                        switch (keepaliveStatus.statusCode) {
                            case 0:
                            case 2:
                                DataConnection.this.loge("Active Keepalive received invalid status!");
                                break;
                            case 1:
                                DataConnection.this.loge("Keepalive received stopped status!");
                                DcNetworkAgent.this.onPacketKeepaliveEvent(keepaliveRecord.slotId, 0);
                                keepaliveRecord.currentStatus = 1;
                                this.mKeepalives.remove(keepaliveStatus.sessionHandle);
                                break;
                            default:
                                DataConnection.this.loge("Invalid Keepalive Status received, " + keepaliveStatus.statusCode);
                                break;
                        }
                        break;
                    case 1:
                        DataConnection.this.loge("Inactive Keepalive received status!");
                        DcNetworkAgent.this.onPacketKeepaliveEvent(keepaliveRecord.slotId, -31);
                        break;
                    case 2:
                        switch (keepaliveStatus.statusCode) {
                            case 0:
                                DcNetworkAgent.this.log("Pending Keepalive received active status!");
                                keepaliveRecord.currentStatus = 0;
                                DcNetworkAgent.this.onPacketKeepaliveEvent(keepaliveRecord.slotId, 0);
                                break;
                            case 1:
                                DcNetworkAgent.this.onPacketKeepaliveEvent(keepaliveRecord.slotId, keepaliveStatusErrorToPacketKeepaliveError(keepaliveStatus.errorCode));
                                keepaliveRecord.currentStatus = 1;
                                this.mKeepalives.remove(keepaliveStatus.sessionHandle);
                                break;
                            case 2:
                                DataConnection.this.loge("Invalid unsolicied Keepalive Pending Status!");
                                break;
                            default:
                                DataConnection.this.loge("Invalid Keepalive Status received, " + keepaliveStatus.statusCode);
                                break;
                        }
                        break;
                    default:
                        DataConnection.this.loge("Invalid Keepalive Status received, " + keepaliveRecord.currentStatus);
                        break;
                }
            }
        }
    }

    void tearDownNow() {
        log("tearDownNow()");
        sendMessage(obtainMessage(EVENT_TEAR_DOWN_NOW));
    }

    protected long getSuggestedRetryDelay(DataCallResponse dataCallResponse) {
        if (dataCallResponse.getSuggestedRetryTime() < 0) {
            log("No suggested retry delay.");
            return -2L;
        }
        if (dataCallResponse.getSuggestedRetryTime() == Integer.MAX_VALUE) {
            log("Modem suggested not retrying.");
            return -1L;
        }
        return dataCallResponse.getSuggestedRetryTime();
    }

    protected String getWhatToString(int i) {
        return cmdToString(i);
    }

    protected static String msgToString(Message message) {
        if (message == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{what=");
        sb.append(cmdToString(message.what));
        sb.append(" when=");
        TimeUtils.formatDuration(message.getWhen() - SystemClock.uptimeMillis(), sb);
        if (message.arg1 != 0) {
            sb.append(" arg1=");
            sb.append(message.arg1);
        }
        if (message.arg2 != 0) {
            sb.append(" arg2=");
            sb.append(message.arg2);
        }
        if (message.obj != null) {
            sb.append(" obj=");
            sb.append(message.obj);
        }
        sb.append(" target=");
        sb.append(message.getTarget());
        sb.append(" replyTo=");
        sb.append(message.replyTo);
        sb.append("}");
        return sb.toString();
    }

    static void slog(String str) {
        Rlog.d("DC", str);
    }

    public void log(String str) {
        Rlog.d(getName(), str);
    }

    protected void logd(String str) {
        Rlog.d(getName(), str);
    }

    protected void logv(String str) {
        Rlog.v(getName(), str);
    }

    protected void logi(String str) {
        Rlog.i(getName(), str);
    }

    protected void logw(String str) {
        Rlog.w(getName(), str);
    }

    protected void loge(String str) {
        Rlog.e(getName(), str);
    }

    protected void loge(String str, Throwable th) {
        Rlog.e(getName(), str, th);
    }

    public String toStringSimple() {
        return getName() + ": State=" + getCurrentState().getName() + " mApnSetting=" + this.mApnSetting + " RefCount=" + this.mApnContexts.size() + " mCid=" + this.mCid + " mCreateTime=" + this.mCreateTime + " mLastastFailTime=" + this.mLastFailTime + " mLastFailCause=" + this.mLastFailCause + " mTag=" + this.mTag + " mLinkProperties=" + this.mLinkProperties + " linkCapabilities=" + getNetworkCapabilities() + " mRestrictedNetworkOverride=" + this.mRestrictedNetworkOverride;
    }

    public String toString() {
        return "{" + toStringSimple() + " mApnContexts=" + this.mApnContexts + "}";
    }

    private void dumpToLog() {
        dump(null, new PrintWriter(new StringWriter(0)) {
            @Override
            public void println(String str) {
                DataConnection.this.logd(str);
            }

            @Override
            public void flush() {
            }
        }, null);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, " ");
        indentingPrintWriter.print("DataConnection ");
        super.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.flush();
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println("mApnContexts.size=" + this.mApnContexts.size());
        indentingPrintWriter.println("mApnContexts=" + this.mApnContexts);
        indentingPrintWriter.println("mDataConnectionTracker=" + this.mDct);
        indentingPrintWriter.println("mApnSetting=" + this.mApnSetting);
        indentingPrintWriter.println("mTag=" + this.mTag);
        indentingPrintWriter.println("mCid=" + this.mCid);
        indentingPrintWriter.println("mConnectionParams=" + this.mConnectionParams);
        indentingPrintWriter.println("mDisconnectParams=" + this.mDisconnectParams);
        indentingPrintWriter.println("mDcFailCause=" + this.mDcFailCause);
        indentingPrintWriter.println("mPhone=" + this.mPhone);
        indentingPrintWriter.println("mLinkProperties=" + this.mLinkProperties);
        indentingPrintWriter.flush();
        indentingPrintWriter.println("mDataRegState=" + this.mDataRegState);
        indentingPrintWriter.println("mRilRat=" + this.mRilRat);
        indentingPrintWriter.println("mNetworkCapabilities=" + getNetworkCapabilities());
        indentingPrintWriter.println("mCreateTime=" + TimeUtils.logTimeOfDay(this.mCreateTime));
        indentingPrintWriter.println("mLastFailTime=" + TimeUtils.logTimeOfDay(this.mLastFailTime));
        indentingPrintWriter.println("mLastFailCause=" + this.mLastFailCause);
        indentingPrintWriter.println("mUserData=" + this.mUserData);
        indentingPrintWriter.println("mSubscriptionOverride=" + Integer.toHexString(this.mSubscriptionOverride));
        indentingPrintWriter.println("mInstanceNumber=" + mInstanceNumber);
        indentingPrintWriter.println("mAc=" + this.mAc);
        indentingPrintWriter.println("Network capabilities changed history:");
        indentingPrintWriter.increaseIndent();
        this.mNetCapsLocalLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        indentingPrintWriter.flush();
    }

    protected void mtkReplaceStates() {
    }

    protected LinkProperties mtkGetLinkProperties() {
        return this.mLinkProperties;
    }

    protected void mtkSetApnContextReason(ApnContext apnContext, String str) {
    }

    protected void mtkCheckDefaultApnRefCount(ApnContext apnContext) {
    }
}
