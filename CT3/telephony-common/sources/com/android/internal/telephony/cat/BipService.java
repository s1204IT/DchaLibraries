package com.android.internal.telephony.cat;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BipService {

    private static final int[] f15x72eb89a2 = null;
    static final int ADDITIONAL_INFO_FOR_BIP_CHANNEL_CLOSED = 2;
    static final int ADDITIONAL_INFO_FOR_BIP_CHANNEL_ID_NOT_AVAILABLE = 3;
    static final int ADDITIONAL_INFO_FOR_BIP_NO_CHANNEL_AVAILABLE = 1;
    static final int ADDITIONAL_INFO_FOR_BIP_NO_SPECIFIC_CAUSE = 0;
    static final int ADDITIONAL_INFO_FOR_BIP_REQUESTED_BUFFER_SIZE_NOT_AVAILABLE = 4;
    static final int ADDITIONAL_INFO_FOR_BIP_REQUESTED_INTERFACE_TRANSPORT_LEVEL_NOT_AVAILABLE = 6;
    static final int ADDITIONAL_INFO_FOR_BIP_SECURITY_ERROR = 5;
    private static final String BIP_NAME = "__M-BIP__";
    private static final int CHANNEL_KEEP_TIMEOUT = 30000;
    private static final int CONN_DELAY_TIMEOUT = 5000;
    private static final int CONN_MGR_TIMEOUT = 50000;
    private static final boolean DBG = true;
    private static final int DELAYED_CLOSE_CHANNEL_TIMEOUT = 5000;
    private static final int DEV_ID_DISPLAY = 2;
    private static final int DEV_ID_KEYPAD = 1;
    private static final int DEV_ID_NETWORK = 131;
    private static final int DEV_ID_TERMINAL = 130;
    private static final int DEV_ID_UICC = 129;
    protected static final int MSG_ID_BIP_CHANNEL_DELAYED_CLOSE = 22;
    protected static final int MSG_ID_BIP_CHANNEL_KEEP_TIMEOUT = 21;
    protected static final int MSG_ID_BIP_CONN_DELAY_TIMEOUT = 11;
    protected static final int MSG_ID_BIP_CONN_MGR_TIMEOUT = 10;
    protected static final int MSG_ID_BIP_DISCONNECT_TIMEOUT = 12;
    protected static final int MSG_ID_BIP_PROACTIVE_COMMAND = 18;
    protected static final int MSG_ID_CLOSE_CHANNEL_DONE = 16;
    protected static final int MSG_ID_EVENT_NOTIFY = 19;
    protected static final int MSG_ID_GET_CHANNEL_STATUS_DONE = 17;
    protected static final int MSG_ID_OPEN_CHANNEL_DONE = 13;
    protected static final int MSG_ID_RECEIVE_DATA_DONE = 15;
    protected static final int MSG_ID_RIL_MSG_DECODED = 20;
    protected static final int MSG_ID_SEND_DATA_DONE = 14;
    private static final String PROPERTY_IA_APN = "ril.radio.ia-apn";
    private static final String PROPERTY_OVERRIDE_APN = "ril.pdn.overrideApn";
    private static final String PROPERTY_PDN_NAME_REUSE = "ril.pdn.name.reuse";
    private static final String PROPERTY_PDN_REUSE = "ril.pdn.reuse";
    private static BipService[] mInstance = null;
    private static int mSimCount = 0;
    final int NETWORK_TYPE;
    private boolean isConnMgrIntentTimeout;
    String mApn;
    private String mApnType;
    private String mApnTypeDb;
    boolean mAutoReconnected;
    BearerDesc mBearerDesc;
    private BipChannelManager mBipChannelManager;
    private BipRilMessageDecoder mBipMsgDecoder;
    private Handler mBipSrvHandler;
    int mBufferSize;
    private Channel mChannel;
    private int mChannelId;
    private int mChannelStatus;
    private ChannelStatus mChannelStatusDataObject;
    private final Object mCloseLock;
    private CommandsInterface mCmdIf;
    private BipCmdMessage mCmdMessage;
    private ConnectivityManager mConnMgr;
    private Context mContext;
    private BipCmdMessage mCurrentCmd;
    protected volatile CatCmdMessage mCurrentSetupEventCmd;
    private BipCmdMessage mCurrntCmd;
    boolean mDNSaddrequest;
    OtherAddress mDataDestinationAddress;
    private List<InetAddress> mDnsAddres;
    private Handler mHandler;
    private boolean mIsApnInserting;
    private boolean mIsCloseInProgress;
    private volatile boolean mIsListenChannelStatus;
    private volatile boolean mIsListenDataAvailable;
    private boolean mIsNetworkAvailableReceived;
    private boolean mIsOpenInProgress;
    private boolean mIsUpdateApnParams;
    int mLinkMode;
    OtherAddress mLocalAddress;
    String mLogin;
    private String mLoginDb;
    private Network mNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private BroadcastReceiver mNetworkConnReceiver;
    private boolean mNetworkConnReceiverRegistered;
    private NetworkRequest mNetworkRequest;
    private String mNumeric;
    String mPassword;
    private String mPasswordDb;
    private int mPreviousKeepChannelId;
    private int mSlotId;
    TransportProtocol mTransportProtocol;

    private static int[] m193xe796fd46() {
        if (f15x72eb89a2 != null) {
            return f15x72eb89a2;
        }
        int[] iArr = new int[AppInterface.CommandType.valuesCustom().length];
        try {
            iArr[AppInterface.CommandType.ACTIVATE.ordinal()] = 6;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[AppInterface.CommandType.CALLCTRL_RSP_MSG.ordinal()] = 7;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 1;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[AppInterface.CommandType.DECLARE_SERVICE.ordinal()] = 8;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_MULTIMEDIA_MESSAGE.ordinal()] = 9;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[AppInterface.CommandType.DISPLAY_TEXT.ordinal()] = 10;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 2;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[AppInterface.CommandType.GET_FRAME_STATUS.ordinal()] = 11;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INKEY.ordinal()] = 12;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[AppInterface.CommandType.GET_INPUT.ordinal()] = 13;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[AppInterface.CommandType.GET_READER_STATUS.ordinal()] = 14;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[AppInterface.CommandType.GET_SERVICE_INFORMATION.ordinal()] = 15;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[AppInterface.CommandType.LANGUAGE_NOTIFICATION.ordinal()] = 16;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[AppInterface.CommandType.LAUNCH_BROWSER.ordinal()] = 17;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[AppInterface.CommandType.MORE_TIME.ordinal()] = 18;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 3;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[AppInterface.CommandType.PERFORM_CARD_APDU.ordinal()] = 19;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[AppInterface.CommandType.PLAY_TONE.ordinal()] = 20;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[AppInterface.CommandType.POLLING_OFF.ordinal()] = 21;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[AppInterface.CommandType.POLL_INTERVAL.ordinal()] = 22;
        } catch (NoSuchFieldError e20) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_OFF_CARD.ordinal()] = 23;
        } catch (NoSuchFieldError e21) {
        }
        try {
            iArr[AppInterface.CommandType.POWER_ON_CARD.ordinal()] = 24;
        } catch (NoSuchFieldError e22) {
        }
        try {
            iArr[AppInterface.CommandType.PROVIDE_LOCAL_INFORMATION.ordinal()] = 25;
        } catch (NoSuchFieldError e23) {
        }
        try {
            iArr[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 4;
        } catch (NoSuchFieldError e24) {
        }
        try {
            iArr[AppInterface.CommandType.REFRESH.ordinal()] = 26;
        } catch (NoSuchFieldError e25) {
        }
        try {
            iArr[AppInterface.CommandType.RETRIEVE_MULTIMEDIA_MESSAGE.ordinal()] = 27;
        } catch (NoSuchFieldError e26) {
        }
        try {
            iArr[AppInterface.CommandType.RUN_AT_COMMAND.ordinal()] = 28;
        } catch (NoSuchFieldError e27) {
        }
        try {
            iArr[AppInterface.CommandType.SELECT_ITEM.ordinal()] = 29;
        } catch (NoSuchFieldError e28) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DATA.ordinal()] = 5;
        } catch (NoSuchFieldError e29) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_DTMF.ordinal()] = 30;
        } catch (NoSuchFieldError e30) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SMS.ordinal()] = 31;
        } catch (NoSuchFieldError e31) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_SS.ordinal()] = 32;
        } catch (NoSuchFieldError e32) {
        }
        try {
            iArr[AppInterface.CommandType.SEND_USSD.ordinal()] = 33;
        } catch (NoSuchFieldError e33) {
        }
        try {
            iArr[AppInterface.CommandType.SERVICE_SEARCH.ordinal()] = 34;
        } catch (NoSuchFieldError e34) {
        }
        try {
            iArr[AppInterface.CommandType.SET_FRAME.ordinal()] = 35;
        } catch (NoSuchFieldError e35) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_CALL.ordinal()] = 36;
        } catch (NoSuchFieldError e36) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_EVENT_LIST.ordinal()] = 37;
        } catch (NoSuchFieldError e37) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_IDLE_MODE_TEXT.ordinal()] = 38;
        } catch (NoSuchFieldError e38) {
        }
        try {
            iArr[AppInterface.CommandType.SET_UP_MENU.ordinal()] = 39;
        } catch (NoSuchFieldError e39) {
        }
        try {
            iArr[AppInterface.CommandType.SUBMIT_MULTIMEDIA_MESSAGE.ordinal()] = 40;
        } catch (NoSuchFieldError e40) {
        }
        try {
            iArr[AppInterface.CommandType.TIMER_MANAGEMENT.ordinal()] = 41;
        } catch (NoSuchFieldError e41) {
        }
        f15x72eb89a2 = iArr;
        return iArr;
    }

    public BipService(Context context, Handler handler, int sim_id) {
        this.mHandler = null;
        this.mCurrentCmd = null;
        this.mCmdMessage = null;
        this.mContext = null;
        this.mConnMgr = null;
        this.mBearerDesc = null;
        this.mBufferSize = 0;
        this.mLocalAddress = null;
        this.mTransportProtocol = null;
        this.mDataDestinationAddress = null;
        this.mLinkMode = 0;
        this.mAutoReconnected = false;
        this.mDNSaddrequest = false;
        this.mDnsAddres = new ArrayList();
        this.mCloseLock = new Object();
        this.mApn = null;
        this.mLogin = null;
        this.mPassword = null;
        this.NETWORK_TYPE = 0;
        this.mChannelStatus = 0;
        this.mChannelId = 0;
        this.mChannel = null;
        this.mChannelStatusDataObject = null;
        this.mSlotId = -1;
        this.mIsApnInserting = false;
        this.mIsListenDataAvailable = false;
        this.mIsListenChannelStatus = false;
        this.mCurrentSetupEventCmd = null;
        this.mPreviousKeepChannelId = 0;
        this.isConnMgrIntentTimeout = false;
        this.mBipChannelManager = null;
        this.mBipMsgDecoder = null;
        this.mCurrntCmd = null;
        this.mCmdIf = null;
        this.mIsOpenInProgress = false;
        this.mIsCloseInProgress = false;
        this.mIsNetworkAvailableReceived = false;
        this.mNetworkRequest = null;
        this.mApnType = "bip";
        this.mIsUpdateApnParams = false;
        this.mNumeric = UsimPBMemInfo.STRING_NOT_SET;
        this.mLoginDb = UsimPBMemInfo.STRING_NOT_SET;
        this.mPasswordDb = UsimPBMemInfo.STRING_NOT_SET;
        this.mApnTypeDb = UsimPBMemInfo.STRING_NOT_SET;
        this.mBipSrvHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int protocolType;
                AsyncResult ar;
                CatLog.d(this, "handleMessage[" + msg.what + "]");
                switch (msg.what) {
                    case 10:
                        CatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CONN_MGR_TIMEOUT");
                        BipService.this.isConnMgrIntentTimeout = true;
                        BipService.this.disconnect();
                        return;
                    case 11:
                        CatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CONN_DELAY_TIMEOUT");
                        BipService.this.acquireNetwork();
                        return;
                    case 12:
                        CatLog.d("[BIP]", "handleMessage MSG_ID_BIP_DISCONNECT_TIMEOUT");
                        synchronized (BipService.this.mCloseLock) {
                            CatLog.d("[BIP]", "mIsCloseInProgress: " + BipService.this.mIsCloseInProgress + " mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                            if (BipService.this.mIsCloseInProgress) {
                                BipService.this.mIsCloseInProgress = false;
                                Message timerMsg = BipService.this.mBipSrvHandler.obtainMessage(16, 0, 0, BipService.this.mCurrentCmd);
                                BipService.this.mBipSrvHandler.sendMessage(timerMsg);
                            } else if (BipService.this.mPreviousKeepChannelId != 0) {
                                BipService.this.mPreviousKeepChannelId = 0;
                                BipCmdMessage cmd = (BipCmdMessage) msg.obj;
                                Message response = BipService.this.mBipSrvHandler.obtainMessage(13);
                                BipService.this.openChannel(cmd, response);
                            }
                        }
                        return;
                    case 13:
                        int ret = msg.arg1;
                        BipCmdMessage cmd2 = (BipCmdMessage) msg.obj;
                        if (BipService.this.mCurrentCmd == null) {
                            CatLog.d("[BIP]", "SS-handleMessage: skip open channel response because current cmd is null");
                            return;
                        }
                        if (BipService.this.mCurrentCmd != null && BipService.this.mCurrentCmd.mCmdDet.typeOfCommand != AppInterface.CommandType.OPEN_CHANNEL.value()) {
                            CatLog.d("[BIP]", "SS-handleMessage: skip open channel response because current cmd type is not OPEN_CHANNEL");
                            return;
                        }
                        if (8 == (cmd2.getCmdQualifier() & 8)) {
                            if (ret == 0) {
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.OK, false, 0, new OpenChannelResponseDataEx(cmd2.mChannelStatusData, cmd2.mBearerDesc, cmd2.mBufferSize, cmd2.mDnsServerAddress));
                                return;
                            } else {
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.BIP_ERROR, true, 0, new OpenChannelResponseDataEx((ChannelStatus) null, cmd2.mBearerDesc, cmd2.mBufferSize, cmd2.mDnsServerAddress));
                                return;
                            }
                        }
                        if (cmd2.mTransportProtocol != null) {
                            protocolType = cmd2.mTransportProtocol.protocolType;
                        } else {
                            protocolType = 0;
                        }
                        if (ret == 0) {
                            ResponseData resp = new OpenChannelResponseDataEx(cmd2.mChannelStatusData, cmd2.mBearerDesc, cmd2.mBufferSize, protocolType);
                            CatLog.d("[BIP]", "SS-handleMessage: open channel successfully");
                            BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.OK, false, 0, resp);
                            return;
                        }
                        if (ret == 3) {
                            ResponseData resp2 = new OpenChannelResponseDataEx(cmd2.mChannelStatusData, cmd2.mBearerDesc, cmd2.mBufferSize, protocolType);
                            CatLog.d("[BIP]", "SS-handleMessage: Modified parameters");
                            BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.PRFRMD_WITH_MODIFICATION, false, 0, resp2);
                            return;
                        } else if (ret == 6) {
                            ResponseData resp3 = new OpenChannelResponseDataEx((ChannelStatus) null, cmd2.mBearerDesc, cmd2.mBufferSize, protocolType);
                            CatLog.d("[BIP]", "SS-handleMessage: ME is busy on call");
                            BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, resp3);
                            return;
                        } else {
                            if (BipService.this.mNetworkCallback != null) {
                                BipService.this.releaseRequest(BipService.this.mNetworkCallback);
                            }
                            ResponseData resp4 = new OpenChannelResponseDataEx((ChannelStatus) null, cmd2.mBearerDesc, cmd2.mBufferSize, protocolType);
                            CatLog.d("[BIP]", "SS-handleMessage: open channel failed");
                            BipService.this.sendTerminalResponse(cmd2.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp4);
                            return;
                        }
                    case 14:
                        int ret2 = msg.arg1;
                        int size = msg.arg2;
                        BipCmdMessage cmd3 = (BipCmdMessage) msg.obj;
                        ResponseData resp5 = new SendDataResponseData(size);
                        if (ret2 == 0) {
                            BipService.this.sendTerminalResponse(cmd3.mCmdDet, ResultCode.OK, false, 0, resp5);
                            return;
                        } else if (ret2 == 7) {
                            BipService.this.sendTerminalResponse(cmd3.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                            return;
                        } else {
                            BipService.this.sendTerminalResponse(cmd3.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp5);
                            return;
                        }
                    case 15:
                        int ret3 = msg.arg1;
                        BipCmdMessage cmd4 = (BipCmdMessage) msg.obj;
                        byte[] buffer = cmd4.mChannelData;
                        int remainingCount = cmd4.mRemainingDataLength;
                        ResponseData resp6 = new ReceiveDataResponseData(buffer, remainingCount);
                        if (ret3 == 0) {
                            BipService.this.sendTerminalResponse(cmd4.mCmdDet, ResultCode.OK, false, 0, resp6);
                            return;
                        } else if (ret3 == 9) {
                            BipService.this.sendTerminalResponse(cmd4.mCmdDet, ResultCode.PRFRMD_WITH_MISSING_INFO, false, 0, resp6);
                            return;
                        } else {
                            BipService.this.sendTerminalResponse(cmd4.mCmdDet, ResultCode.BIP_ERROR, true, 0, null);
                            return;
                        }
                    case 16:
                        BipCmdMessage cmd5 = (BipCmdMessage) msg.obj;
                        if (msg.arg1 == 0) {
                            BipService.this.sendTerminalResponse(cmd5.mCmdDet, ResultCode.OK, false, 0, null);
                            return;
                        } else if (msg.arg1 == 7) {
                            BipService.this.sendTerminalResponse(cmd5.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                            return;
                        } else {
                            if (msg.arg1 != 8) {
                                return;
                            }
                            BipService.this.sendTerminalResponse(cmd5.mCmdDet, ResultCode.BIP_ERROR, true, 2, null);
                            return;
                        }
                    case 17:
                        int i = msg.arg1;
                        BipCmdMessage cmd6 = (BipCmdMessage) msg.obj;
                        ArrayList arrList = (ArrayList) cmd6.mChannelStatusList;
                        CatLog.d("[BIP]", "SS-handleCmdResponse: MSG_ID_GET_CHANNEL_STATUS_DONE:" + arrList.size());
                        BipService.this.sendTerminalResponse(cmd6.mCmdDet, ResultCode.OK, false, 0, new GetMultipleChannelStatusResponseData(arrList));
                        return;
                    case 18:
                    case 19:
                        CatLog.d(this, "ril message arrived, slotid: " + BipService.this.mSlotId);
                        String data = null;
                        if (msg.obj != null && (ar = (AsyncResult) msg.obj) != null && ar.result != null) {
                            try {
                                data = (String) ar.result;
                            } catch (ClassCastException e) {
                                return;
                            }
                            break;
                        }
                        BipService.this.mBipMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, data));
                        return;
                    case 20:
                        BipService.this.handleRilMsg((RilMessage) msg.obj);
                        return;
                    case 21:
                        CatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CHANNEL_KEEP_TIMEOUT");
                        CatLog.d("[BIP]", "mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                        if (BipService.this.mPreviousKeepChannelId == 0) {
                            return;
                        }
                        int cId = BipService.this.mPreviousKeepChannelId;
                        Channel channel = BipService.this.mBipChannelManager.getChannel(cId);
                        if (BipService.this.mNetworkCallback != null) {
                            BipService.this.releaseRequest(BipService.this.mNetworkCallback);
                        }
                        BipService.this.resetLocked();
                        if (channel != null) {
                            channel.closeChannel();
                        }
                        BipService.this.mBipChannelManager.removeChannel(BipService.this.mPreviousKeepChannelId);
                        BipService.this.deleteApnParams();
                        SystemProperties.set(BipService.PROPERTY_PDN_REUSE, "1");
                        SystemProperties.set("ril.stk.bip", "0");
                        BipService.this.mChannel = null;
                        BipService.this.mChannelStatus = 2;
                        BipService.this.mPreviousKeepChannelId = 0;
                        BipService.this.mApn = null;
                        BipService.this.mLogin = null;
                        BipService.this.mPassword = null;
                        return;
                    case 22:
                        int channelId = msg.arg1;
                        CatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel id: " + channelId);
                        if (channelId > 0 && channelId <= 7 && BipService.this.mBipChannelManager.isChannelIdOccupied(channelId)) {
                            Channel channel2 = BipService.this.mBipChannelManager.getChannel(channelId);
                            CatLog.d("[BIP]", "channel protocolType:" + channel2.mProtocolType);
                            if (1 == channel2.mProtocolType || 2 == channel2.mProtocolType) {
                                channel2.closeChannel();
                                BipService.this.mBipChannelManager.removeChannel(channelId);
                                return;
                            } else {
                                CatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel type: " + channel2.mProtocolType);
                                return;
                            }
                        }
                        CatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel already closed");
                        return;
                    default:
                        return;
                }
            }
        };
        this.mNetworkConnReceiverRegistered = false;
        this.mNetworkConnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                CatLog.d("[BIP]", "mNetworkConnReceiver:" + BipService.this.mIsOpenInProgress + " , " + BipService.this.mIsCloseInProgress + " , " + BipService.this.isConnMgrIntentTimeout + " , " + BipService.this.mPreviousKeepChannelId);
                if (BipService.this.mBipChannelManager != null) {
                    CatLog.d("[BIP]", "isClientChannelOpened:" + BipService.this.mBipChannelManager.isClientChannelOpened());
                }
                if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    if ((!BipService.this.mIsOpenInProgress || BipService.this.isConnMgrIntentTimeout) && !BipService.this.mIsCloseInProgress && BipService.this.mPreviousKeepChannelId == 0) {
                        return;
                    }
                    CatLog.d("[BIP]", "Connectivity changed onReceive Enter");
                    Thread rt = new Thread(BipService.this.new ConnectivityChangeThread(intent));
                    rt.start();
                    CatLog.d("[BIP]", "Connectivity changed onReceive Leave");
                }
            }
        };
        CatLog.d("[BIP]", "Construct BipService");
        if (context == null) {
            CatLog.e("[BIP]", "Fail to construct BipService");
            return;
        }
        this.mContext = context;
        this.mSlotId = sim_id;
        CatLog.d("[BIP]", "Construct instance sim id: " + sim_id);
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mHandler = handler;
        this.mBipChannelManager = new BipChannelManager();
        IntentFilter connFilter = new IntentFilter();
        connFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mNetworkConnReceiver, connFilter);
        this.mNetworkConnReceiverRegistered = true;
        Thread t = new Thread() {
            @Override
            public void run() {
                BipService.this.deleteApnParams();
            }
        };
        t.start();
        SystemProperties.set(PROPERTY_PDN_REUSE, "1");
    }

    public BipService(Context context, Handler handler, int sim_id, CommandsInterface cmdIf, IccFileHandler fh) {
        this.mHandler = null;
        this.mCurrentCmd = null;
        this.mCmdMessage = null;
        this.mContext = null;
        this.mConnMgr = null;
        this.mBearerDesc = null;
        this.mBufferSize = 0;
        this.mLocalAddress = null;
        this.mTransportProtocol = null;
        this.mDataDestinationAddress = null;
        this.mLinkMode = 0;
        this.mAutoReconnected = false;
        this.mDNSaddrequest = false;
        this.mDnsAddres = new ArrayList();
        this.mCloseLock = new Object();
        this.mApn = null;
        this.mLogin = null;
        this.mPassword = null;
        this.NETWORK_TYPE = 0;
        this.mChannelStatus = 0;
        this.mChannelId = 0;
        this.mChannel = null;
        this.mChannelStatusDataObject = null;
        this.mSlotId = -1;
        this.mIsApnInserting = false;
        this.mIsListenDataAvailable = false;
        this.mIsListenChannelStatus = false;
        this.mCurrentSetupEventCmd = null;
        this.mPreviousKeepChannelId = 0;
        this.isConnMgrIntentTimeout = false;
        this.mBipChannelManager = null;
        this.mBipMsgDecoder = null;
        this.mCurrntCmd = null;
        this.mCmdIf = null;
        this.mIsOpenInProgress = false;
        this.mIsCloseInProgress = false;
        this.mIsNetworkAvailableReceived = false;
        this.mNetworkRequest = null;
        this.mApnType = "bip";
        this.mIsUpdateApnParams = false;
        this.mNumeric = UsimPBMemInfo.STRING_NOT_SET;
        this.mLoginDb = UsimPBMemInfo.STRING_NOT_SET;
        this.mPasswordDb = UsimPBMemInfo.STRING_NOT_SET;
        this.mApnTypeDb = UsimPBMemInfo.STRING_NOT_SET;
        this.mBipSrvHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int protocolType;
                AsyncResult ar;
                CatLog.d(this, "handleMessage[" + msg.what + "]");
                switch (msg.what) {
                    case 10:
                        CatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CONN_MGR_TIMEOUT");
                        BipService.this.isConnMgrIntentTimeout = true;
                        BipService.this.disconnect();
                        return;
                    case 11:
                        CatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CONN_DELAY_TIMEOUT");
                        BipService.this.acquireNetwork();
                        return;
                    case 12:
                        CatLog.d("[BIP]", "handleMessage MSG_ID_BIP_DISCONNECT_TIMEOUT");
                        synchronized (BipService.this.mCloseLock) {
                            CatLog.d("[BIP]", "mIsCloseInProgress: " + BipService.this.mIsCloseInProgress + " mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                            if (BipService.this.mIsCloseInProgress) {
                                BipService.this.mIsCloseInProgress = false;
                                Message timerMsg = BipService.this.mBipSrvHandler.obtainMessage(16, 0, 0, BipService.this.mCurrentCmd);
                                BipService.this.mBipSrvHandler.sendMessage(timerMsg);
                            } else if (BipService.this.mPreviousKeepChannelId != 0) {
                                BipService.this.mPreviousKeepChannelId = 0;
                                BipCmdMessage cmd = (BipCmdMessage) msg.obj;
                                Message response = BipService.this.mBipSrvHandler.obtainMessage(13);
                                BipService.this.openChannel(cmd, response);
                            }
                        }
                        return;
                    case 13:
                        int ret = msg.arg1;
                        BipCmdMessage cmd2 = (BipCmdMessage) msg.obj;
                        if (BipService.this.mCurrentCmd == null) {
                            CatLog.d("[BIP]", "SS-handleMessage: skip open channel response because current cmd is null");
                            return;
                        }
                        if (BipService.this.mCurrentCmd != null && BipService.this.mCurrentCmd.mCmdDet.typeOfCommand != AppInterface.CommandType.OPEN_CHANNEL.value()) {
                            CatLog.d("[BIP]", "SS-handleMessage: skip open channel response because current cmd type is not OPEN_CHANNEL");
                            return;
                        }
                        if (8 == (cmd2.getCmdQualifier() & 8)) {
                            if (ret == 0) {
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.OK, false, 0, new OpenChannelResponseDataEx(cmd2.mChannelStatusData, cmd2.mBearerDesc, cmd2.mBufferSize, cmd2.mDnsServerAddress));
                                return;
                            } else {
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.BIP_ERROR, true, 0, new OpenChannelResponseDataEx((ChannelStatus) null, cmd2.mBearerDesc, cmd2.mBufferSize, cmd2.mDnsServerAddress));
                                return;
                            }
                        }
                        if (cmd2.mTransportProtocol != null) {
                            protocolType = cmd2.mTransportProtocol.protocolType;
                        } else {
                            protocolType = 0;
                        }
                        if (ret == 0) {
                            ResponseData resp = new OpenChannelResponseDataEx(cmd2.mChannelStatusData, cmd2.mBearerDesc, cmd2.mBufferSize, protocolType);
                            CatLog.d("[BIP]", "SS-handleMessage: open channel successfully");
                            BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.OK, false, 0, resp);
                            return;
                        }
                        if (ret == 3) {
                            ResponseData resp2 = new OpenChannelResponseDataEx(cmd2.mChannelStatusData, cmd2.mBearerDesc, cmd2.mBufferSize, protocolType);
                            CatLog.d("[BIP]", "SS-handleMessage: Modified parameters");
                            BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.PRFRMD_WITH_MODIFICATION, false, 0, resp2);
                            return;
                        } else if (ret == 6) {
                            ResponseData resp3 = new OpenChannelResponseDataEx((ChannelStatus) null, cmd2.mBearerDesc, cmd2.mBufferSize, protocolType);
                            CatLog.d("[BIP]", "SS-handleMessage: ME is busy on call");
                            BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, resp3);
                            return;
                        } else {
                            if (BipService.this.mNetworkCallback != null) {
                                BipService.this.releaseRequest(BipService.this.mNetworkCallback);
                            }
                            ResponseData resp4 = new OpenChannelResponseDataEx((ChannelStatus) null, cmd2.mBearerDesc, cmd2.mBufferSize, protocolType);
                            CatLog.d("[BIP]", "SS-handleMessage: open channel failed");
                            BipService.this.sendTerminalResponse(cmd2.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp4);
                            return;
                        }
                    case 14:
                        int ret2 = msg.arg1;
                        int size = msg.arg2;
                        BipCmdMessage cmd3 = (BipCmdMessage) msg.obj;
                        ResponseData resp5 = new SendDataResponseData(size);
                        if (ret2 == 0) {
                            BipService.this.sendTerminalResponse(cmd3.mCmdDet, ResultCode.OK, false, 0, resp5);
                            return;
                        } else if (ret2 == 7) {
                            BipService.this.sendTerminalResponse(cmd3.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                            return;
                        } else {
                            BipService.this.sendTerminalResponse(cmd3.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp5);
                            return;
                        }
                    case 15:
                        int ret3 = msg.arg1;
                        BipCmdMessage cmd4 = (BipCmdMessage) msg.obj;
                        byte[] buffer = cmd4.mChannelData;
                        int remainingCount = cmd4.mRemainingDataLength;
                        ResponseData resp6 = new ReceiveDataResponseData(buffer, remainingCount);
                        if (ret3 == 0) {
                            BipService.this.sendTerminalResponse(cmd4.mCmdDet, ResultCode.OK, false, 0, resp6);
                            return;
                        } else if (ret3 == 9) {
                            BipService.this.sendTerminalResponse(cmd4.mCmdDet, ResultCode.PRFRMD_WITH_MISSING_INFO, false, 0, resp6);
                            return;
                        } else {
                            BipService.this.sendTerminalResponse(cmd4.mCmdDet, ResultCode.BIP_ERROR, true, 0, null);
                            return;
                        }
                    case 16:
                        BipCmdMessage cmd5 = (BipCmdMessage) msg.obj;
                        if (msg.arg1 == 0) {
                            BipService.this.sendTerminalResponse(cmd5.mCmdDet, ResultCode.OK, false, 0, null);
                            return;
                        } else if (msg.arg1 == 7) {
                            BipService.this.sendTerminalResponse(cmd5.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                            return;
                        } else {
                            if (msg.arg1 != 8) {
                                return;
                            }
                            BipService.this.sendTerminalResponse(cmd5.mCmdDet, ResultCode.BIP_ERROR, true, 2, null);
                            return;
                        }
                    case 17:
                        int i = msg.arg1;
                        BipCmdMessage cmd6 = (BipCmdMessage) msg.obj;
                        ArrayList arrList = (ArrayList) cmd6.mChannelStatusList;
                        CatLog.d("[BIP]", "SS-handleCmdResponse: MSG_ID_GET_CHANNEL_STATUS_DONE:" + arrList.size());
                        BipService.this.sendTerminalResponse(cmd6.mCmdDet, ResultCode.OK, false, 0, new GetMultipleChannelStatusResponseData(arrList));
                        return;
                    case 18:
                    case 19:
                        CatLog.d(this, "ril message arrived, slotid: " + BipService.this.mSlotId);
                        String data = null;
                        if (msg.obj != null && (ar = (AsyncResult) msg.obj) != null && ar.result != null) {
                            try {
                                data = (String) ar.result;
                            } catch (ClassCastException e) {
                                return;
                            }
                            break;
                        }
                        BipService.this.mBipMsgDecoder.sendStartDecodingMessageParams(new RilMessage(msg.what, data));
                        return;
                    case 20:
                        BipService.this.handleRilMsg((RilMessage) msg.obj);
                        return;
                    case 21:
                        CatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CHANNEL_KEEP_TIMEOUT");
                        CatLog.d("[BIP]", "mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                        if (BipService.this.mPreviousKeepChannelId == 0) {
                            return;
                        }
                        int cId = BipService.this.mPreviousKeepChannelId;
                        Channel channel = BipService.this.mBipChannelManager.getChannel(cId);
                        if (BipService.this.mNetworkCallback != null) {
                            BipService.this.releaseRequest(BipService.this.mNetworkCallback);
                        }
                        BipService.this.resetLocked();
                        if (channel != null) {
                            channel.closeChannel();
                        }
                        BipService.this.mBipChannelManager.removeChannel(BipService.this.mPreviousKeepChannelId);
                        BipService.this.deleteApnParams();
                        SystemProperties.set(BipService.PROPERTY_PDN_REUSE, "1");
                        SystemProperties.set("ril.stk.bip", "0");
                        BipService.this.mChannel = null;
                        BipService.this.mChannelStatus = 2;
                        BipService.this.mPreviousKeepChannelId = 0;
                        BipService.this.mApn = null;
                        BipService.this.mLogin = null;
                        BipService.this.mPassword = null;
                        return;
                    case 22:
                        int channelId = msg.arg1;
                        CatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel id: " + channelId);
                        if (channelId > 0 && channelId <= 7 && BipService.this.mBipChannelManager.isChannelIdOccupied(channelId)) {
                            Channel channel2 = BipService.this.mBipChannelManager.getChannel(channelId);
                            CatLog.d("[BIP]", "channel protocolType:" + channel2.mProtocolType);
                            if (1 == channel2.mProtocolType || 2 == channel2.mProtocolType) {
                                channel2.closeChannel();
                                BipService.this.mBipChannelManager.removeChannel(channelId);
                                return;
                            } else {
                                CatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel type: " + channel2.mProtocolType);
                                return;
                            }
                        }
                        CatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel already closed");
                        return;
                    default:
                        return;
                }
            }
        };
        this.mNetworkConnReceiverRegistered = false;
        this.mNetworkConnReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                CatLog.d("[BIP]", "mNetworkConnReceiver:" + BipService.this.mIsOpenInProgress + " , " + BipService.this.mIsCloseInProgress + " , " + BipService.this.isConnMgrIntentTimeout + " , " + BipService.this.mPreviousKeepChannelId);
                if (BipService.this.mBipChannelManager != null) {
                    CatLog.d("[BIP]", "isClientChannelOpened:" + BipService.this.mBipChannelManager.isClientChannelOpened());
                }
                if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    if ((!BipService.this.mIsOpenInProgress || BipService.this.isConnMgrIntentTimeout) && !BipService.this.mIsCloseInProgress && BipService.this.mPreviousKeepChannelId == 0) {
                        return;
                    }
                    CatLog.d("[BIP]", "Connectivity changed onReceive Enter");
                    Thread rt = new Thread(BipService.this.new ConnectivityChangeThread(intent));
                    rt.start();
                    CatLog.d("[BIP]", "Connectivity changed onReceive Leave");
                }
            }
        };
        CatLog.d("[BIP]", "Construct BipService " + sim_id);
        if (context == null) {
            CatLog.e("[BIP]", "Fail to construct BipService");
            return;
        }
        this.mContext = context;
        this.mSlotId = sim_id;
        this.mCmdIf = cmdIf;
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mHandler = handler;
        this.mBipChannelManager = new BipChannelManager();
        this.mBipMsgDecoder = BipRilMessageDecoder.getInstance(this.mBipSrvHandler, fh, this.mSlotId);
        if (this.mBipMsgDecoder == null) {
            CatLog.d(this, "Null BipRilMessageDecoder instance");
            return;
        }
        this.mBipMsgDecoder.start();
        cmdIf.setOnBipProactiveCmd(this.mBipSrvHandler, 18, null);
        IntentFilter connFilter = new IntentFilter();
        connFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mNetworkConnReceiver, connFilter);
        this.mNetworkConnReceiverRegistered = true;
        Thread t = new Thread() {
            @Override
            public void run() {
                BipService.this.deleteApnParams();
            }
        };
        t.start();
        SystemProperties.set(PROPERTY_PDN_REUSE, "1");
    }

    private ConnectivityManager getConnectivityManager() {
        if (this.mConnMgr == null) {
            this.mConnMgr = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mConnMgr;
    }

    public static BipService getInstance(Context context, Handler handler, int simId) {
        CatLog.d("[BIP]", "getInstance sim : " + simId);
        if (mInstance == null) {
            mSimCount = TelephonyManager.getDefault().getSimCount();
            mInstance = new BipService[mSimCount];
            for (int i = 0; i < mSimCount; i++) {
                mInstance[i] = null;
            }
        }
        if (simId < 0 || simId > mSimCount) {
            CatLog.d("[BIP]", "getInstance invalid sim : " + simId);
            return null;
        }
        if (mInstance[simId] == null) {
            mInstance[simId] = new BipService(context, handler, simId);
        }
        return mInstance[simId];
    }

    public static BipService getInstance(Context context, Handler handler, int simId, CommandsInterface cmdIf, IccFileHandler fh) {
        CatLog.d("[BIP]", "getInstance sim : " + simId);
        if (mInstance == null) {
            mSimCount = TelephonyManager.getDefault().getSimCount();
            mInstance = new BipService[mSimCount];
            for (int i = 0; i < mSimCount; i++) {
                mInstance[i] = null;
            }
        }
        if (simId < 0 || simId > mSimCount) {
            CatLog.d("[BIP]", "getInstance invalid sim : " + simId);
            return null;
        }
        if (mInstance[simId] == null) {
            mInstance[simId] = new BipService(context, handler, simId, cmdIf, fh);
        }
        return mInstance[simId];
    }

    public void dispose() {
        CatLog.d("[BIP]", "dispose slotId : " + this.mSlotId);
        if (mInstance != null) {
            if (mInstance[this.mSlotId] != null) {
                mInstance[this.mSlotId] = null;
            }
            int i = 0;
            while (i < mSimCount && mInstance[i] == null) {
                i++;
            }
            if (i == mSimCount) {
                mInstance = null;
            }
        }
        if (!this.mNetworkConnReceiverRegistered) {
            return;
        }
        this.mContext.unregisterReceiver(this.mNetworkConnReceiver);
        this.mNetworkConnReceiverRegistered = false;
    }

    private void handleRilMsg(RilMessage rilMsg) {
        if (rilMsg == null) {
        }
        switch (rilMsg.mId) {
            case 18:
                try {
                    CommandParams cmdParams = (CommandParams) rilMsg.mData;
                    if (cmdParams != null) {
                        if (rilMsg.mResCode == ResultCode.OK) {
                            handleCommand(cmdParams, true);
                        } else {
                            sendTerminalResponse(cmdParams.mCmdDet, rilMsg.mResCode, false, 0, null);
                        }
                    }
                } catch (ClassCastException e) {
                    CatLog.d(this, "Fail to parse proactive command");
                    if (this.mCurrntCmd == null) {
                        return;
                    }
                    sendTerminalResponse(this.mCurrntCmd.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                    return;
                }
                break;
        }
    }

    private void checkPSEvent(CatCmdMessage cmdMsg) {
        this.mIsListenDataAvailable = false;
        this.mIsListenChannelStatus = false;
        for (int eventVal : cmdMsg.getSetEventList().eventList) {
            CatLog.d(this, "Event: " + eventVal);
            switch (eventVal) {
                case 9:
                    this.mIsListenDataAvailable = true;
                    break;
                case 10:
                    this.mIsListenChannelStatus = true;
                    break;
            }
        }
    }

    void setSetupEventList(CatCmdMessage cmdMsg) {
        this.mCurrentSetupEventCmd = cmdMsg;
        checkPSEvent(cmdMsg);
    }

    boolean hasPsEvent(int eventId) {
        switch (eventId) {
            case 9:
                return this.mIsListenDataAvailable;
            case 10:
                return this.mIsListenChannelStatus;
            default:
                return false;
        }
    }

    private void handleCommand(CommandParams cmdParams, boolean isProactiveCmd) {
        int protocolType;
        CatLog.d(this, cmdParams.getCommandType().name());
        BipCmdMessage cmdMsg = new BipCmdMessage(cmdParams);
        switch (m193xe796fd46()[cmdParams.getCommandType().ordinal()]) {
            case 1:
                CatLog.d(this, "SS-handleProactiveCommand: process CLOSE_CHANNEL");
                Message response = this.mBipSrvHandler.obtainMessage(16);
                closeChannel(cmdMsg, response);
                break;
            case 2:
                CatLog.d(this, "SS-handleProactiveCommand: process GET_CHANNEL_STATUS");
                this.mCmdMessage = cmdMsg;
                Message response2 = this.mBipSrvHandler.obtainMessage(17);
                getChannelStatus(cmdMsg, response2);
                break;
            case 3:
                CatLog.d(this, "SS-handleProactiveCommand: process OPEN_CHANNEL");
                PhoneConstants.State state = PhoneConstants.State.IDLE;
                CallManager callmgr = CallManager.getInstance();
                int[] subId = SubscriptionManager.getSubId(this.mSlotId);
                int phoneId = -1;
                if (subId != null) {
                    phoneId = SubscriptionManager.getPhoneId(subId[0]);
                }
                Phone myPhone = PhoneFactory.getPhone(phoneId);
                if (cmdMsg.mTransportProtocol != null) {
                    protocolType = cmdMsg.mTransportProtocol.protocolType;
                } else {
                    protocolType = 0;
                }
                if (myPhone == null) {
                    CatLog.d(this, "myPhone is still null");
                    ResponseData resp = new OpenChannelResponseDataEx((ChannelStatus) null, cmdMsg.mBearerDesc, cmdMsg.mBufferSize, protocolType);
                    sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, resp);
                    return;
                }
                String bipDisabled = SystemProperties.get("persist.ril.bip.disabled", "0");
                if (bipDisabled != null && bipDisabled.equals("1")) {
                    CatLog.d(this, "BIP disabled");
                    ResponseData resp2 = new OpenChannelResponseDataEx((ChannelStatus) null, cmdMsg.mBearerDesc, cmdMsg.mBufferSize, protocolType);
                    sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, resp2);
                    return;
                }
                int currentSubId = getCurrentSubId();
                if (SubscriptionManager.isValidSubscriptionId(currentSubId) && !isCurrentConnectionInService(currentSubId)) {
                    ResponseData resp3 = new OpenChannelResponseDataEx((ChannelStatus) null, cmdMsg.mBearerDesc, cmdMsg.mBufferSize, protocolType);
                    sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 4, resp3);
                    return;
                }
                if (myPhone.getServiceState().getNetworkType() <= 2 && callmgr != null) {
                    PhoneConstants.State call_state = callmgr.getState();
                    CatLog.d(this, "call_state" + call_state);
                    if (call_state != PhoneConstants.State.IDLE) {
                        CatLog.d(this, "SS-handleProactiveCommand: ME is busy on call");
                        cmdMsg.mChannelStatusData = new ChannelStatus(getFreeChannelId(), 0, 0);
                        cmdMsg.mChannelStatusData.mChannelStatus = 0;
                        this.mCurrentCmd = cmdMsg;
                        Message response3 = this.mBipSrvHandler.obtainMessage(13, 6, 0, cmdMsg);
                        response3.sendToTarget();
                        return;
                    }
                } else {
                    CatLog.d(this, "SS-handleProactiveCommand: type:" + myPhone.getServiceState().getNetworkType() + ",or null callmgr");
                }
                Message response4 = this.mBipSrvHandler.obtainMessage(13);
                openChannel(cmdMsg, response4);
                break;
                break;
            case 4:
                CatLog.d(this, "SS-handleProactiveCommand: process RECEIVE_DATA");
                Message response5 = this.mBipSrvHandler.obtainMessage(15);
                receiveData(cmdMsg, response5);
                break;
            case 5:
                CatLog.d(this, "SS-handleProactiveCommand: process SEND_DATA");
                Message response6 = this.mBipSrvHandler.obtainMessage(14);
                sendData(cmdMsg, response6);
                break;
            default:
                CatLog.d(this, "Unsupported command");
                return;
        }
        this.mCurrntCmd = cmdMsg;
    }

    private void sendTerminalResponse(CommandDetails cmdDet, ResultCode resultCode, boolean includeAdditionalInfo, int additionalInfo, ResponseData resp) {
        if (cmdDet == null) {
            CatLog.e(this, "SS-sendTR: cmdDet is null");
            return;
        }
        CatLog.d(this, "SS-sendTR: command type is " + cmdDet.typeOfCommand);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int tag = ComprehensionTlvTag.COMMAND_DETAILS.value();
        if (cmdDet.compRequired) {
            tag |= 128;
        }
        buf.write(tag);
        buf.write(3);
        buf.write(cmdDet.commandNumber);
        buf.write(cmdDet.typeOfCommand);
        buf.write(cmdDet.commandQualifier);
        buf.write(ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128);
        buf.write(2);
        buf.write(130);
        buf.write(129);
        int tag2 = ComprehensionTlvTag.RESULT.value();
        if (cmdDet.compRequired) {
            tag2 |= 128;
        }
        buf.write(tag2);
        int length = includeAdditionalInfo ? 2 : 1;
        buf.write(length);
        buf.write(resultCode.value());
        if (includeAdditionalInfo) {
            buf.write(additionalInfo);
        }
        if (resp != null) {
            CatLog.d(this, "SS-sendTR: write response data into TR");
            resp.format(buf);
        } else {
            CatLog.d(this, "SS-sendTR: null resp.");
        }
        byte[] rawData = buf.toByteArray();
        String hexString = IccUtils.bytesToHexString(rawData);
        CatLog.d(this, "TERMINAL RESPONSE: " + hexString);
        this.mCmdIf.sendTerminalResponse(hexString, null);
    }

    private int getDataConnectionFromSetting() {
        int currentDataConnectionSimId = Settings.System.getInt(this.mContext.getContentResolver(), "gprs_connection_setting", -4) - 1;
        CatLog.d("[BIP]", "Default Data Setting value=" + currentDataConnectionSimId);
        return currentDataConnectionSimId;
    }

    private void connect() {
        CatLog.d("[BIP]", "establishConnect");
        this.mCurrentCmd.mChannelStatusData.isActivated = true;
        CatLog.d("[BIP]", "requestNetwork: establish data channel");
        int ret = establishLink();
        if (ret == 10) {
            return;
        }
        if (ret == 0 || ret == 3) {
            CatLog.d("[BIP]", "1 channel is activated");
            updateCurrentChannelStatus(128);
        } else {
            CatLog.d("[BIP]", "2 channel is un-activated");
            updateCurrentChannelStatus(0);
        }
        this.mIsOpenInProgress = false;
        this.mIsNetworkAvailableReceived = false;
        Message response = this.mBipSrvHandler.obtainMessage(13, ret, 0, this.mCurrentCmd);
        this.mBipSrvHandler.sendMessage(response);
    }

    private void sendDelayedCloseChannel(int channelId) {
        Message bipTimerMsg = this.mBipSrvHandler.obtainMessage(22);
        bipTimerMsg.arg1 = channelId;
        this.mBipSrvHandler.sendMessageDelayed(bipTimerMsg, 5000L);
    }

    private void disconnect() {
        CatLog.d("[BIP]", "disconnect: opening ? " + this.mIsOpenInProgress);
        deleteOrRestoreApnParams();
        SystemProperties.set(PROPERTY_PDN_REUSE, "1");
        if (this.mIsOpenInProgress && this.mChannelStatus != 4) {
            Channel channel = this.mBipChannelManager.getChannel(this.mChannelId);
            if (channel != null) {
                channel.closeChannel();
                this.mBipChannelManager.removeChannel(this.mChannelId);
            } else if (this.mTransportProtocol != null) {
                this.mBipChannelManager.releaseChannelId(this.mChannelId, this.mTransportProtocol.protocolType);
            }
            if (this.mNetworkCallback != null) {
                releaseRequest(this.mNetworkCallback);
            }
            this.mChannelStatus = 2;
            CatLog.d("[BIP]", "disconnect(): mCurrentCmd = " + this.mCurrentCmd);
            if (this.mCurrentCmd.mChannelStatusData != null) {
                this.mCurrentCmd.mChannelStatusData.mChannelStatus = 0;
                this.mCurrentCmd.mChannelStatusData.isActivated = false;
            }
            this.mIsOpenInProgress = false;
            Message response = this.mBipSrvHandler.obtainMessage(13, 2, 0, this.mCurrentCmd);
            this.mBipSrvHandler.sendMessage(response);
            return;
        }
        ArrayList<Byte> alByte = new ArrayList<>();
        CatLog.d("[BIP]", "this is a drop link");
        this.mChannelStatus = 2;
        CatResponseMessage resMsg = new CatResponseMessage(10);
        for (int i = 1; i <= 7; i++) {
            if (this.mBipChannelManager.isChannelIdOccupied(i)) {
                try {
                    Channel channel2 = this.mBipChannelManager.getChannel(i);
                    CatLog.d("[BIP]", "channel protocolType:" + channel2.mProtocolType);
                    if (1 == channel2.mProtocolType || 2 == channel2.mProtocolType) {
                        releaseRequest(this.mNetworkCallback);
                        resetLocked();
                        if (isVzWSupport()) {
                            this.mBipChannelManager.updateChannelStatus(channel2.mChannelId, 0);
                            this.mBipChannelManager.updateChannelStatusInfo(channel2.mChannelId, 5);
                            sendDelayedCloseChannel(i);
                        } else {
                            channel2.closeChannel();
                            this.mBipChannelManager.removeChannel(i);
                        }
                        alByte.add((byte) -72);
                        alByte.add((byte) 2);
                        alByte.add(Byte.valueOf((byte) (channel2.mChannelId | 0)));
                        alByte.add((byte) 5);
                    }
                } catch (NullPointerException ne) {
                    CatLog.e("[BIP]", "NPE, channel null.");
                    ne.printStackTrace();
                }
            }
        }
        if (alByte.size() > 0) {
            byte[] additionalInfo = new byte[alByte.size()];
            for (int i2 = 0; i2 < additionalInfo.length; i2++) {
                additionalInfo[i2] = alByte.get(i2).byteValue();
            }
            resMsg.setSourceId(130);
            resMsg.setDestinationId(129);
            resMsg.setAdditionalInfo(additionalInfo);
            resMsg.setOneShot(false);
            resMsg.setEventDownload(10, additionalInfo);
            CatLog.d("[BIP]", "onEventDownload: for channel status");
            ((CatService) this.mHandler).onEventDownload(resMsg);
            return;
        }
        CatLog.d("[BIP]", "onEventDownload: No client channels are opened.");
    }

    public void acquireNetwork() {
        this.mIsOpenInProgress = true;
        if (this.mNetwork != null) {
            CatLog.d("[BIP]", "acquireNetwork: already available");
            Channel channel = this.mBipChannelManager.getChannel(this.mChannelId);
            if (channel == null) {
                connect();
                return;
            }
            return;
        }
        CatLog.d("[BIP]", "requestNetwork: slotId " + this.mSlotId);
        newRequest();
    }

    public void openChannel(BipCmdMessage cmdMsg, Message response) {
        CatLog.d("[BIP]", "BM-openChannel: enter");
        if (!checkDataCapability(cmdMsg)) {
            cmdMsg.mChannelStatusData = new ChannelStatus(0, 0, 0);
            response.arg1 = 5;
            response.obj = cmdMsg;
            this.mCurrentCmd = cmdMsg;
            this.mBipSrvHandler.sendMessage(response);
            return;
        }
        this.isConnMgrIntentTimeout = false;
        this.mBipSrvHandler.removeMessages(21);
        this.mBipSrvHandler.removeMessages(22);
        CatLog.d("[BIP]", "BM-openChannel: getCmdQualifier:" + cmdMsg.getCmdQualifier());
        this.mDNSaddrequest = 8 == (cmdMsg.getCmdQualifier() & 8);
        CatLog.d("[BIP]", "BM-openChannel: mDNSaddrequest:" + this.mDNSaddrequest);
        CatLog.d("[BIP]", "BM-openChannel: cmdMsg.mApn:" + cmdMsg.mApn);
        CatLog.d("[BIP]", "BM-openChannel: cmdMsg.mLogin:" + cmdMsg.mLogin);
        CatLog.d("[BIP]", "BM-openChannel: cmdMsg.mPwd:" + cmdMsg.mPwd);
        if (!this.mDNSaddrequest && cmdMsg.mTransportProtocol != null) {
            CatLog.d("[BIP]", "BM-openChannel: mPreviousKeepChannelId:" + this.mPreviousKeepChannelId + " mChannelStatus:" + this.mChannelStatus + " mApn:" + this.mApn);
            if (this.mPreviousKeepChannelId != 0 && 4 == this.mChannelStatus) {
                if ((this.mApn == null && cmdMsg.mApn == null) || (this.mApn != null && cmdMsg.mApn != null && this.mApn.equals(cmdMsg.mApn))) {
                    this.mChannelId = this.mPreviousKeepChannelId;
                    cmdMsg.mChannelStatusData = new ChannelStatus(this.mChannelId, 128, 0);
                    this.mCurrentCmd = cmdMsg;
                    response.arg1 = 0;
                    response.obj = cmdMsg;
                    this.mBipSrvHandler.sendMessage(response);
                    this.mPreviousKeepChannelId = 0;
                    return;
                }
                this.mCurrentCmd = cmdMsg;
                if (this.mNetworkCallback != null) {
                    releaseRequest(this.mNetworkCallback);
                }
                resetLocked();
                Channel pchannel = this.mBipChannelManager.getChannel(this.mPreviousKeepChannelId);
                if (pchannel != null) {
                    pchannel.closeChannel();
                }
                this.mBipChannelManager.removeChannel(this.mPreviousKeepChannelId);
                deleteApnParams();
                SystemProperties.set(PROPERTY_PDN_REUSE, "1");
                SystemProperties.set("ril.stk.bip", "0");
                this.mChannel = null;
                this.mChannelStatus = 2;
                this.mApn = null;
                this.mLogin = null;
                this.mPassword = null;
                if (this.mPreviousKeepChannelId != 0) {
                    sendBipDisconnectTimeOutMsg(cmdMsg);
                    return;
                }
                return;
            }
            this.mChannelId = this.mBipChannelManager.acquireChannelId(cmdMsg.mTransportProtocol.protocolType);
            if (this.mChannelId == 0) {
                CatLog.d("[BIP]", "BM-openChannel: acquire channel id = 0");
                response.arg1 = 5;
                response.obj = cmdMsg;
                this.mCurrentCmd = cmdMsg;
                this.mBipSrvHandler.sendMessage(response);
                CatLog.d("[BIP]", "BM-openChannel: channel id = 0. mCurrentCmd = " + this.mCurrentCmd);
                return;
            }
            this.mApn = cmdMsg.mApn;
            this.mLogin = cmdMsg.mLogin;
            this.mPassword = cmdMsg.mPwd;
        }
        cmdMsg.mChannelStatusData = new ChannelStatus(this.mChannelId, 0, 0);
        this.mCurrentCmd = cmdMsg;
        CatLog.d("[BIP]", "BM-openChannel: mCurrentCmd = " + this.mCurrentCmd);
        this.mBearerDesc = cmdMsg.mBearerDesc;
        if (cmdMsg.mBearerDesc != null) {
            CatLog.d("[BIP]", "BM-openChannel: bearer type " + cmdMsg.mBearerDesc.bearerType);
        } else {
            CatLog.d("[BIP]", "BM-openChannel: bearer type is null");
        }
        this.mBufferSize = cmdMsg.mBufferSize;
        CatLog.d("[BIP]", "BM-openChannel: buffer size " + cmdMsg.mBufferSize);
        this.mLocalAddress = cmdMsg.mLocalAddress;
        if (cmdMsg.mLocalAddress != null) {
            CatLog.d("[BIP]", "BM-openChannel: local address " + cmdMsg.mLocalAddress.address.toString());
        } else {
            CatLog.d("[BIP]", "BM-openChannel: local address is null");
        }
        if (cmdMsg.mTransportProtocol != null) {
            this.mTransportProtocol = cmdMsg.mTransportProtocol;
            CatLog.d("[BIP]", "BM-openChannel: transport protocol type/port " + cmdMsg.mTransportProtocol.protocolType + "/" + cmdMsg.mTransportProtocol.portNumber);
        }
        this.mDataDestinationAddress = cmdMsg.mDataDestinationAddress;
        if (cmdMsg.mDataDestinationAddress != null) {
            CatLog.d("[BIP]", "BM-openChannel: dest address " + cmdMsg.mDataDestinationAddress.address.toString());
        } else {
            CatLog.d("[BIP]", "BM-openChannel: dest address is null");
        }
        this.mLinkMode = (cmdMsg.getCmdQualifier() & 1) == 1 ? 0 : 1;
        CatLog.d("[BIP]", "BM-openChannel: mLinkMode " + this.mLinkMode);
        this.mAutoReconnected = (cmdMsg.getCmdQualifier() & 2) == 2;
        String isPdnReuse = SystemProperties.get(PROPERTY_PDN_REUSE);
        CatLog.d("[BIP]", "BM-openChannel: isPdnReuse: " + isPdnReuse);
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        int phoneId = subId != null ? SubscriptionManager.getPhoneId(subId[0]) : -1;
        Phone myPhone = PhoneFactory.getPhone(phoneId);
        if (this.mBearerDesc != null) {
            if (this.mBearerDesc.bearerType == 3) {
                SystemProperties.set(PROPERTY_PDN_REUSE, Phone.ACT_TYPE_UTRAN);
                if (this.mApn == null || this.mApn.length() <= 0) {
                    String numeric = null;
                    if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                        numeric = TelephonyManager.getDefault().getSimOperator(subId[0]);
                    }
                    CatLog.d("[BIP]", "numeric: " + numeric);
                    if (numeric == null || !numeric.equals("00101")) {
                        SystemProperties.set(PROPERTY_PDN_NAME_REUSE, UsimPBMemInfo.STRING_NOT_SET);
                    } else {
                        String iaApn = SystemProperties.get(PROPERTY_IA_APN);
                        SystemProperties.set(PROPERTY_PDN_NAME_REUSE, iaApn);
                        CatLog.d("[BIP]", "set ia APN to reuse: " + iaApn);
                    }
                } else {
                    SystemProperties.set(PROPERTY_PDN_NAME_REUSE, this.mApn);
                }
            } else {
                SystemProperties.set(PROPERTY_PDN_REUSE, "0");
                if (this.mApn != null && this.mApn.length() > 0) {
                    if (myPhone != null) {
                        int dataNetworkType = myPhone.getServiceState().getDataNetworkType();
                        CatLog.d("[BIP]", "dataNetworkType: " + dataNetworkType);
                        if (13 == dataNetworkType) {
                            String iaApn2 = SystemProperties.get(PROPERTY_IA_APN);
                            CatLog.d("[BIP]", "iaApn: " + iaApn2);
                            String numeric2 = SubscriptionManager.isValidSubscriptionId(subId[0]) ? TelephonyManager.getDefault().getSimOperator(subId[0]) : null;
                            CatLog.d("[BIP]", "numeric: " + numeric2);
                            if (numeric2 != null && !numeric2.equals("00101") && iaApn2 != null && iaApn2.length() > 0 && iaApn2.equals(this.mApn)) {
                                SystemProperties.set(PROPERTY_PDN_REUSE, Phone.ACT_TYPE_UTRAN);
                            }
                        }
                    } else {
                        CatLog.e("[BIP]", "myPhone is null");
                    }
                    CatLog.d("[BIP]", "BM-openChannel: override apn: " + this.mApn);
                    SystemProperties.set(PROPERTY_OVERRIDE_APN, this.mApn);
                }
            }
        } else if (this.mTransportProtocol != null && 3 != this.mTransportProtocol.protocolType && 4 != this.mTransportProtocol.protocolType && 5 != this.mTransportProtocol.protocolType) {
            CatLog.e("[BIP]", "BM-openChannel: unsupported transport protocol type !!!");
            response.arg1 = 5;
            response.obj = this.mCurrentCmd;
            this.mBipSrvHandler.sendMessage(response);
            return;
        }
        this.mApnType = "bip";
        if (this.mApn == null || this.mApn.length() <= 0) {
            String numeric3 = null;
            if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                numeric3 = TelephonyManager.getDefault().getSimOperator(subId[0]);
            }
            CatLog.d("[BIP]", "numeric: " + numeric3);
            if (numeric3 == null || !numeric3.equals("00101")) {
                this.mApnType = "default";
            } else {
                this.mApnType = "bip";
            }
            if (myPhone != null) {
                CarrierConfigManager configMgr = (CarrierConfigManager) myPhone.getContext().getSystemService("carrier_config");
                PersistableBundle b = configMgr.getConfigForSubId(myPhone.getSubId());
                boolean needSupport = b != null ? b.getBoolean("use_administrative_apn_bool") : false;
                if (needSupport) {
                    CatLog.d("[BIP]", "support KDDI feature");
                    int dataNetworkType2 = myPhone.getServiceState().getDataNetworkType();
                    CatLog.d("[BIP]", "dataNetworkType: " + dataNetworkType2);
                    if (13 == dataNetworkType2) {
                        this.mApnType = "fota";
                    }
                }
            } else {
                CatLog.e("[BIP]", "myPhone is null");
            }
        } else if (this.mApn.equals("VZWADMIN") || this.mApn.equals("vzwadmin")) {
            this.mApnType = "fota";
        } else if (this.mApn.equals("VZWINTERNET") || this.mApn.equals("vzwinternet")) {
            this.mApnType = "internet";
        } else if (this.mApn.equals("titi") || this.mApn.equals("web99.test-nfc1.com")) {
            this.mApnType = "fota";
        } else {
            this.mApnType = "bip";
            setApnParams(this.mApn, this.mLogin, this.mPassword);
        }
        CatLog.d("[BIP]", "APN Type: " + this.mApnType);
        SystemProperties.set("gsm.stk.bip", "1");
        CatLog.d("[BIP]", "MAXCHANNELID: 7");
        if (this.mTransportProtocol != null && 3 == this.mTransportProtocol.protocolType) {
            int ret = establishLink();
            if (ret == 0 || ret == 3) {
                CatLog.d("[BIP]", "BM-openChannel: channel is activated");
                Channel channel = this.mBipChannelManager.getChannel(this.mChannelId);
                cmdMsg.mChannelStatusData.mChannelStatus = channel.mChannelStatusData.mChannelStatus;
            } else {
                CatLog.d("[BIP]", "BM-openChannel: channel is un-activated");
                cmdMsg.mChannelStatusData.mChannelStatus = 0;
            }
            response.arg1 = ret;
            response.obj = this.mCurrentCmd;
            this.mBipSrvHandler.sendMessage(response);
        } else if (this.mIsApnInserting) {
            CatLog.d("[BIP]", "BM-openChannel: startUsingNetworkFeature delay trigger.");
            Message timerMsg = this.mBipSrvHandler.obtainMessage(11);
            timerMsg.obj = cmdMsg;
            this.mBipSrvHandler.sendMessageDelayed(timerMsg, 5000L);
            this.mIsApnInserting = false;
        } else {
            acquireNetwork();
        }
        CatLog.d("[BIP]", "BM-openChannel: exit");
    }

    public void closeChannel(BipCmdMessage bipCmdMessage, Message message) {
        CatLog.d("[BIP]", "BM-closeChannel: enter");
        int i = bipCmdMessage.mCloseCid;
        message.arg1 = 0;
        this.mCurrentCmd = bipCmdMessage;
        if (i < 0 || 7 < i) {
            CatLog.d("[BIP]", "BM-closeChannel: channel id:" + i + " is invalid !!!");
            message.arg1 = 7;
        } else {
            this.mPreviousKeepChannelId = 0;
            CatLog.d("[BIP]", "BM-closeChannel: getBipChannelStatus:" + this.mBipChannelManager.getBipChannelStatus(i));
            try {
                if (this.mBipChannelManager.getBipChannelStatus(i) == 0) {
                    message.arg1 = 7;
                } else if (2 == this.mBipChannelManager.getBipChannelStatus(i)) {
                    message.arg1 = 8;
                } else {
                    Channel channel = this.mBipChannelManager.getChannel(i);
                    if (channel == null) {
                        CatLog.d("[BIP]", "BM-closeChannel: channel has already been closed");
                        message.arg1 = 7;
                    } else {
                        ?? r4 = 0;
                        ?? r42 = 0;
                        r4 = 0;
                        CatLog.d("[BIP]", "BM-closeChannel: mProtocolType:" + channel.mProtocolType + " getCmdQualifier:" + bipCmdMessage.getCmdQualifier());
                        if (3 == channel.mProtocolType) {
                            if (channel instanceof TcpServerChannel) {
                                ?? r43 = channel;
                                r43.setCloseBackToTcpListen(bipCmdMessage.mCloseBackToTcpListen);
                                r42 = r43;
                            }
                            message.arg1 = channel.closeChannel();
                            r4 = r42;
                        } else if (1 == (bipCmdMessage.getCmdQualifier() & 1)) {
                            this.mPreviousKeepChannelId = i;
                            CatLog.d("[BIP]", "BM-closeChannel: mPreviousKeepChannelId:" + this.mPreviousKeepChannelId);
                            message.arg1 = 0;
                        } else {
                            CatLog.d("[BIP]", "BM-closeChannel: stop data connection");
                            this.mIsCloseInProgress = true;
                            releaseRequest(this.mNetworkCallback);
                            resetLocked();
                            deleteOrRestoreApnParams();
                            SystemProperties.set(PROPERTY_PDN_REUSE, "1");
                            message.arg1 = channel.closeChannel();
                        }
                        if (3 == channel.mProtocolType) {
                            if (r4 != 0 && !r4.isCloseBackToTcpListen()) {
                                this.mBipChannelManager.removeChannel(i);
                            }
                            this.mChannel = null;
                            this.mChannelStatus = 2;
                        } else if (1 == (bipCmdMessage.getCmdQualifier() & 1)) {
                            sendBipChannelKeepTimeOutMsg(bipCmdMessage);
                        } else {
                            this.mBipChannelManager.removeChannel(i);
                            this.mChannel = null;
                            this.mChannelStatus = 2;
                            this.mApn = null;
                            this.mLogin = null;
                            this.mPassword = null;
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                CatLog.e("[BIP]", "BM-closeChannel: IndexOutOfBoundsException cid=" + i);
                message.arg1 = 7;
            }
        }
        if (this.mIsCloseInProgress) {
            sendBipDisconnectTimeOutMsg(bipCmdMessage);
        } else {
            message.obj = bipCmdMessage;
            this.mBipSrvHandler.sendMessage(message);
        }
        CatLog.d("[BIP]", "BM-closeChannel: exit");
    }

    public void receiveData(BipCmdMessage cmdMsg, Message response) {
        int requestCount = cmdMsg.mChannelDataLength;
        ReceiveDataResult result = new ReceiveDataResult();
        int cId = cmdMsg.mReceiveDataCid;
        Channel lChannel = this.mBipChannelManager.getChannel(cId);
        CatLog.d("[BIP]", "BM-receiveData: receiveData enter");
        if (lChannel == null) {
            CatLog.e("[BIP]", "lChannel is null cid=" + cId);
            response.arg1 = 5;
            response.obj = cmdMsg;
            this.mBipSrvHandler.sendMessage(response);
            return;
        }
        if (lChannel.mChannelStatus == 4 || lChannel.mChannelStatus == 3) {
            if (requestCount > 237) {
                CatLog.d("[BIP]", "BM-receiveData: Modify channel data length to MAX_APDU_SIZE");
                requestCount = BipUtils.MAX_APDU_SIZE;
            }
            Thread recvThread = new Thread(new RecvDataRunnable(requestCount, result, cmdMsg, response));
            recvThread.start();
            return;
        }
        CatLog.d("[BIP]", "BM-receiveData: Channel status is invalid " + this.mChannelStatus);
        response.arg1 = 5;
        response.obj = cmdMsg;
        this.mBipSrvHandler.sendMessage(response);
    }

    public void sendData(BipCmdMessage cmdMsg, Message response) {
        CatLog.d("[BIP]", "sendData: Enter");
        Thread rt = new Thread(new SendDataThread(cmdMsg, response));
        rt.start();
        CatLog.d("[BIP]", "sendData: Leave");
    }

    private void newRequest() {
        ConnectivityManager connectivityManager = getConnectivityManager();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                if (!BipService.this.isConnMgrIntentTimeout) {
                    BipService.this.mBipSrvHandler.removeMessages(10);
                }
                CatLog.d("[BIP]", "NetworkCallbackListener.onAvailable, mChannelId: " + BipService.this.mChannelId + " , mIsOpenInProgress: " + BipService.this.mIsOpenInProgress + " , mIsNetworkAvailableReceived: " + BipService.this.mIsNetworkAvailableReceived);
                if (BipService.this.mDNSaddrequest && BipService.this.mIsOpenInProgress) {
                    BipService.this.queryDnsServerAddress(network);
                    return;
                }
                if (!BipService.this.mIsOpenInProgress || BipService.this.mIsNetworkAvailableReceived) {
                    CatLog.d("[BIP]", "Bip channel has been established.");
                    return;
                }
                BipService.this.mIsNetworkAvailableReceived = true;
                BipService.this.mNetwork = network;
                BipService.this.connect();
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                if (!BipService.this.isConnMgrIntentTimeout) {
                    BipService.this.mBipSrvHandler.removeMessages(10);
                }
                BipService.this.mBipSrvHandler.removeMessages(21);
                BipService.this.mPreviousKeepChannelId = 0;
                CatLog.d("[BIP]", "onLost: network:" + network + " mNetworkCallback:" + BipService.this.mNetworkCallback + " this:" + this);
                BipService.this.releaseRequest(this);
                if (BipService.this.mNetworkCallback != this) {
                    return;
                }
                BipService.this.resetLocked();
                BipService.this.disconnect();
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                if (!BipService.this.isConnMgrIntentTimeout) {
                    BipService.this.mBipSrvHandler.removeMessages(10);
                }
                BipService.this.mBipSrvHandler.removeMessages(21);
                BipService.this.mPreviousKeepChannelId = 0;
                CatLog.d("[BIP]", "onUnavailable: mNetworkCallback:" + BipService.this.mNetworkCallback + " this:" + this);
                BipService.this.releaseRequest(this);
                if (BipService.this.mNetworkCallback != this) {
                    return;
                }
                BipService.this.resetLocked();
                BipService.this.disconnect();
            }
        };
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        int networkCapability = 27;
        if (this.mApnType != null && this.mApnType.equals("default")) {
            networkCapability = 12;
        } else if (this.mApnType != null && this.mApnType.equals("internet")) {
            networkCapability = 12;
        } else if (this.mApnType != null && this.mApnType.equals("fota")) {
            networkCapability = 3;
        }
        if (subId != null && SubscriptionManager.from(this.mContext).isActiveSubId(subId[0])) {
            this.mNetworkRequest = new NetworkRequest.Builder().addTransportType(0).addCapability(networkCapability).setNetworkSpecifier(String.valueOf(subId[0])).build();
        } else {
            this.mNetworkRequest = new NetworkRequest.Builder().addTransportType(0).addCapability(networkCapability).build();
        }
        CatLog.d("[BIP]", "Start request network timer.");
        sendBipConnTimeOutMsg(this.mCurrentCmd);
        CatLog.d("[BIP]", "requestNetwork: mNetworkRequest:" + this.mNetworkRequest + " mNetworkCallback:" + this.mNetworkCallback);
        connectivityManager.requestNetwork(this.mNetworkRequest, this.mNetworkCallback, CONN_MGR_TIMEOUT);
    }

    private void resetLocked() {
        this.mNetworkCallback = null;
        this.mNetwork = null;
    }

    private void releaseRequest(ConnectivityManager.NetworkCallback callback) {
        if (callback != null) {
            CatLog.d("[BIP]", "releaseRequest");
            ConnectivityManager connectivityManager = getConnectivityManager();
            connectivityManager.unregisterNetworkCallback(callback);
            return;
        }
        CatLog.d("[BIP]", "releaseRequest: networkCallback is null.");
    }

    private void queryDnsServerAddress(Network network) {
        ConnectivityManager connectivityManager = getConnectivityManager();
        LinkProperties curLinkProps = connectivityManager.getLinkProperties(network);
        if (curLinkProps == null) {
            CatLog.e("[BIP]", "curLinkProps is null !!!");
            sendOpenChannelDoneMsg(5);
            return;
        }
        Collection<InetAddress> dnsAddres = curLinkProps.getDnsServers();
        if (dnsAddres == null || dnsAddres.size() == 0) {
            CatLog.e("[BIP]", "LinkProps has null dnsAddres !!!");
            sendOpenChannelDoneMsg(5);
            return;
        }
        if (this.mCurrentCmd == null || AppInterface.CommandType.OPEN_CHANNEL.value() != this.mCurrentCmd.mCmdDet.typeOfCommand) {
            return;
        }
        this.mCurrentCmd.mDnsServerAddress = new DnsServerAddress();
        this.mCurrentCmd.mDnsServerAddress.dnsAddresses.clear();
        for (InetAddress addr : dnsAddres) {
            if (addr != null) {
                CatLog.d("[BIP]", "DNS Server Address:" + addr);
                this.mCurrentCmd.mDnsServerAddress.dnsAddresses.add(addr);
            }
        }
        this.mIsOpenInProgress = false;
        sendOpenChannelDoneMsg(0);
    }

    private void sendOpenChannelDoneMsg(int result) {
        Message msg = this.mBipSrvHandler.obtainMessage(13, result, 0, this.mCurrentCmd);
        this.mBipSrvHandler.sendMessage(msg);
    }

    protected class SendDataThread implements Runnable {
        BipCmdMessage cmdMsg;
        Message response;

        SendDataThread(BipCmdMessage Msg, Message resp) {
            CatLog.d("[BIP]", "SendDataThread Init");
            this.cmdMsg = Msg;
            this.response = resp;
        }

        @Override
        public void run() {
            int ret;
            CatLog.d("[BIP]", "SendDataThread Run Enter");
            byte[] buffer = this.cmdMsg.mChannelData;
            int mode = this.cmdMsg.mSendMode;
            int cId = this.cmdMsg.mSendDataCid;
            Channel lChannel = BipService.this.mBipChannelManager.getChannel(cId);
            if (lChannel == null) {
                CatLog.d("[BIP]", "SendDataThread Run mChannelId != cmdMsg.mSendDataCid");
                ret = 7;
            } else if (lChannel.mChannelStatus == 4) {
                CatLog.d("[BIP]", "SendDataThread Run mChannel.sendData");
                ret = lChannel.sendData(buffer, mode);
                this.response.arg2 = lChannel.getTxAvailBufferSize();
            } else {
                CatLog.d("[BIP]", "SendDataThread Run CHANNEL_ID_NOT_VALID");
                ret = 7;
            }
            this.response.arg1 = ret;
            this.response.obj = this.cmdMsg;
            CatLog.d("[BIP]", "SendDataThread Run mBipSrvHandler.sendMessage(response);");
            BipService.this.mBipSrvHandler.sendMessage(this.response);
        }
    }

    public void getChannelStatus(BipCmdMessage cmdMsg, Message response) {
        List<ChannelStatus> csList = new ArrayList<>();
        for (int cId = 1; cId <= 7; cId++) {
            try {
                if (this.mBipChannelManager.isChannelIdOccupied(cId)) {
                    CatLog.d("[BIP]", "getChannelStatus: cId:" + cId);
                    csList.add(this.mBipChannelManager.getChannel(cId).mChannelStatusData);
                }
            } catch (NullPointerException ne) {
                CatLog.e("[BIP]", "getChannelStatus: NE");
                ne.printStackTrace();
            }
        }
        cmdMsg.mChannelStatusList = csList;
        response.arg1 = 0;
        response.obj = cmdMsg;
        this.mBipSrvHandler.sendMessage(response);
    }

    private void sendBipConnTimeOutMsg(BipCmdMessage cmdMsg) {
        Message bipTimerMsg = this.mBipSrvHandler.obtainMessage(10);
        bipTimerMsg.obj = cmdMsg;
        this.mBipSrvHandler.sendMessageDelayed(bipTimerMsg, 50000L);
    }

    private void sendBipDisconnectTimeOutMsg(BipCmdMessage cmdMsg) {
        Message bipTimerMsg = this.mBipSrvHandler.obtainMessage(12);
        bipTimerMsg.obj = cmdMsg;
        this.mBipSrvHandler.sendMessageDelayed(bipTimerMsg, 5000L);
    }

    private void sendBipChannelKeepTimeOutMsg(BipCmdMessage cmdMsg) {
        Message bipTimerMsg = this.mBipSrvHandler.obtainMessage(21);
        bipTimerMsg.obj = cmdMsg;
        this.mBipSrvHandler.sendMessageDelayed(bipTimerMsg, 30000L);
    }

    private void updateCurrentChannelStatus(int status) {
        try {
            this.mBipChannelManager.updateChannelStatus(this.mChannelId, status);
            this.mCurrentCmd.mChannelStatusData.mChannelStatus = status;
        } catch (NullPointerException ne) {
            CatLog.e("[BIP]", "updateCurrentChannelStatus id:" + this.mChannelId + " is null");
            ne.printStackTrace();
        }
    }

    private boolean requestRouteToHost() {
        CatLog.d("[BIP]", "requestRouteToHost");
        if (this.mDataDestinationAddress == null) {
            CatLog.d("[BIP]", "mDataDestinationAddress is null");
            return false;
        }
        byte[] addressBytes = this.mDataDestinationAddress.address.getAddress();
        int addr = ((addressBytes[3] & PplMessageManager.Type.INVALID) << 24) | ((addressBytes[2] & PplMessageManager.Type.INVALID) << 16) | ((addressBytes[1] & PplMessageManager.Type.INVALID) << 8) | (addressBytes[0] & PplMessageManager.Type.INVALID);
        return this.mConnMgr.requestRouteToHost(42, addr);
    }

    private boolean checkNetworkInfo(NetworkInfo nwInfo, NetworkInfo.State exState) {
        if (nwInfo == null) {
            return false;
        }
        int type = nwInfo.getType();
        NetworkInfo.State state = nwInfo.getState();
        CatLog.d("[BIP]", "network type is " + (type == 0 ? "MOBILE" : "WIFI"));
        CatLog.d("[BIP]", "network state is " + state);
        return type == 0 && state == exState;
    }

    private int establishLink() {
        int ret;
        if (this.mTransportProtocol == null) {
            CatLog.e("[BIP]", "BM-establishLink: mTransportProtocol is null !!!");
            return 5;
        }
        if (this.mTransportProtocol.protocolType == 3) {
            CatLog.d("[BIP]", "BM-establishLink: establish a TCPServer link");
            try {
                Channel lChannel = new TcpServerChannel(this.mChannelId, this.mLinkMode, this.mTransportProtocol.protocolType, this.mTransportProtocol.portNumber, this.mBufferSize, (CatService) this.mHandler, this);
                ret = lChannel.openChannel(this.mCurrentCmd, this.mNetwork);
                if (ret == 0 || ret == 3) {
                    this.mChannelStatus = 4;
                    this.mBipChannelManager.addChannel(this.mChannelId, lChannel);
                } else {
                    this.mBipChannelManager.releaseChannelId(this.mChannelId, 3);
                    this.mChannelStatus = 7;
                }
            } catch (NullPointerException ne) {
                CatLog.e("[BIP]", "BM-establishLink: NE,new TCP server channel fail.");
                ne.printStackTrace();
                return 5;
            }
        } else if (this.mTransportProtocol.protocolType == 2) {
            CatLog.d("[BIP]", "BM-establishLink: establish a TCP link");
            try {
                Channel lChannel2 = new TcpChannel(this.mChannelId, this.mLinkMode, this.mTransportProtocol.protocolType, this.mDataDestinationAddress.address, this.mTransportProtocol.portNumber, this.mBufferSize, (CatService) this.mHandler, this);
                ret = lChannel2.openChannel(this.mCurrentCmd, this.mNetwork);
                if (ret != 10) {
                    if (ret == 0 || ret == 3) {
                        this.mChannelStatus = 4;
                        this.mBipChannelManager.addChannel(this.mChannelId, lChannel2);
                    } else {
                        this.mBipChannelManager.releaseChannelId(this.mChannelId, 2);
                        this.mChannelStatus = 7;
                    }
                }
            } catch (NullPointerException ne2) {
                CatLog.e("[BIP]", "BM-establishLink: NE,new TCP client channel fail.");
                ne2.printStackTrace();
                return this.mDataDestinationAddress == null ? 9 : 5;
            }
        } else if (this.mTransportProtocol.protocolType == 1) {
            CatLog.d("[BIP]", "BM-establishLink: establish a UDP link");
            try {
                Channel lChannel3 = new UdpChannel(this.mChannelId, this.mLinkMode, this.mTransportProtocol.protocolType, this.mDataDestinationAddress.address, this.mTransportProtocol.portNumber, this.mBufferSize, (CatService) this.mHandler, this);
                ret = lChannel3.openChannel(this.mCurrentCmd, this.mNetwork);
                if (ret == 0 || ret == 3) {
                    this.mChannelStatus = 4;
                    this.mBipChannelManager.addChannel(this.mChannelId, lChannel3);
                } else {
                    this.mBipChannelManager.releaseChannelId(this.mChannelId, 1);
                    this.mChannelStatus = 7;
                }
            } catch (NullPointerException ne3) {
                CatLog.e("[BIP]", "BM-establishLink: NE,new UDP client channel fail.");
                ne3.printStackTrace();
                return 5;
            }
        } else {
            CatLog.d("[BIP]", "BM-establishLink: unsupported channel type");
            ret = 4;
            this.mChannelStatus = 7;
        }
        CatLog.d("[BIP]", "BM-establishLink: ret:" + ret);
        return ret;
    }

    private Uri getUri(Uri uri, int slodId) {
        int[] subId = SubscriptionManager.getSubId(slodId);
        if (subId == null) {
            CatLog.d("[BIP]", "BM-getUri: null subId.");
            return null;
        }
        if (SubscriptionManager.isValidSubscriptionId(subId[0])) {
            return Uri.withAppendedPath(uri, "/subId/" + subId[0]);
        }
        CatLog.d("[BIP]", "BM-getUri: invalid subId.");
        return null;
    }

    private void deleteApnParams() {
        CatLog.d("[BIP]", "BM-deleteApnParams: enter. ");
        int rows = this.mContext.getContentResolver().delete(Telephony.Carriers.CONTENT_URI, "name = '__M-BIP__'", null);
        CatLog.d("[BIP]", "BM-deleteApnParams:[" + rows + "] end");
    }

    private void setApnParams(String apn, String user, String pwd) {
        CatLog.d("[BIP]", "BM-setApnParams: enter");
        if (apn == null) {
            CatLog.d("[BIP]", "BM-setApnParams: No apn parameters");
            return;
        }
        String numeric = null;
        String apnType = this.mApnType;
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        if (subId == null || !SubscriptionManager.isValidSubscriptionId(subId[0])) {
            CatLog.e("[BIP]", "BM-setApnParams: Invalid subId !!!");
        } else {
            numeric = TelephonyManager.getDefault().getSimOperator(subId[0]);
        }
        if (numeric != null && numeric.length() >= 4) {
            String mcc = numeric.substring(0, 3);
            String mnc = numeric.substring(3);
            this.mNumeric = mcc + mnc;
            String selection = "apn = '" + apn + "' COLLATE NOCASE and " + Telephony.Carriers.NUMERIC + " = '" + mcc + mnc + "'";
            CatLog.d("[BIP]", "BM-setApnParams: selection = " + selection);
            Cursor cursor = this.mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, null, selection, null, null);
            if (cursor != null) {
                ContentValues values = new ContentValues();
                values.put("name", BIP_NAME);
                values.put("apn", apn);
                if (user != null) {
                    values.put("user", user);
                }
                if (pwd != null) {
                    values.put(Telephony.Carriers.PASSWORD, pwd);
                }
                values.put("type", apnType);
                values.put(Telephony.Carriers.MCC, mcc);
                values.put(Telephony.Carriers.MNC, mnc);
                values.put(Telephony.Carriers.NUMERIC, mcc + mnc);
                values.put("sub_id", Integer.valueOf(subId[0]));
                values.put("protocol", "IPV4V6");
                if (cursor.getCount() == 0) {
                    CatLog.d("[BIP]", "BM-setApnParams: insert one record");
                    Uri newRow = this.mContext.getContentResolver().insert(Telephony.Carriers.CONTENT_URI, values);
                    if (newRow != null) {
                        CatLog.d("[BIP]", "BM-setApnParams: insert a new record into db");
                        this.mIsApnInserting = true;
                    } else {
                        CatLog.d("[BIP]", "BM-setApnParams: Fail to insert new record into db");
                    }
                } else if (cursor.getCount() >= 1) {
                    CatLog.d("[BIP]", "BM-setApnParams: count = " + cursor.getCount());
                    if (cursor.moveToFirst()) {
                        this.mApnTypeDb = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                        this.mLoginDb = cursor.getString(cursor.getColumnIndexOrThrow("user"));
                        this.mPasswordDb = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD));
                        ContentValues updateValues = new ContentValues();
                        CatLog.d("[BIP]", "BM-setApnParams: apn old value : " + this.mApnTypeDb);
                        if (this.mApnTypeDb != null && this.mApnTypeDb.contains("default")) {
                            this.mApnType = "default";
                        } else if (this.mApnTypeDb == null || !this.mApnTypeDb.contains("supl")) {
                            this.mApnType = "bip";
                        } else {
                            this.mApnType = "supl";
                        }
                        CatLog.d("[BIP]", "BM-setApnParams: mApnType :" + this.mApnType);
                        if (this.mApnTypeDb != null && !this.mApnTypeDb.contains(this.mApnType)) {
                            String apnTypeDbNew = this.mApnTypeDb + "," + this.mApnType;
                            CatLog.d("[BIP]", "BM-setApnParams: will update apn to :" + apnTypeDbNew);
                            updateValues.put("type", apnTypeDbNew);
                        }
                        CatLog.d("[BIP]", "BM-restoreApnParams: mLogin: " + this.mLogin + "mLoginDb:" + this.mLoginDb + "mPassword" + this.mPassword + "mPasswordDb" + this.mPasswordDb);
                        if ((this.mLogin != null && !this.mLogin.equals(this.mLoginDb)) || (this.mPassword != null && !this.mPassword.equals(this.mPasswordDb))) {
                            CatLog.d("[BIP]", "BM-setApnParams: will update login and password");
                            updateValues.put("user", this.mLogin);
                            updateValues.put(Telephony.Carriers.PASSWORD, this.mPassword);
                        }
                        if (updateValues.size() > 0) {
                            CatLog.d("[BIP]", "BM-setApnParams: will update apn db");
                            this.mContext.getContentResolver().update(Telephony.Carriers.CONTENT_URI, updateValues, selection, null);
                            this.mIsApnInserting = true;
                            this.mIsUpdateApnParams = true;
                        } else {
                            CatLog.d("[BIP]", "No need update APN db");
                        }
                    }
                } else {
                    CatLog.d("[BIP]", "BM-setApnParams: do not update one record");
                }
                cursor.close();
            }
        }
        CatLog.d("[BIP]", "BM-setApnParams: exit");
    }

    private void restoreApnParams() {
        String selection = "apn = '" + this.mApn + "' COLLATE NOCASE and " + Telephony.Carriers.NUMERIC + " = '" + this.mNumeric + "'";
        CatLog.d("[BIP]", "BM-restoreApnParams: selection = " + selection);
        Cursor cursor = this.mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String apnTypeDb = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                CatLog.d("[BIP]", "BM-restoreApnParams: apnTypeDb before = " + apnTypeDb);
                ContentValues updateValues = new ContentValues();
                if (apnTypeDb != null && !apnTypeDb.equals(this.mApnTypeDb) && apnTypeDb.contains(this.mApnType)) {
                    String apnTypeDb2 = apnTypeDb.replaceAll("," + this.mApnType, UsimPBMemInfo.STRING_NOT_SET);
                    CatLog.d("[BIP]", "BM-restoreApnParams: apnTypeDb after = " + apnTypeDb2);
                    updateValues.put("type", apnTypeDb2);
                }
                CatLog.d("[BIP]", "BM-restoreApnParams: mLogin: " + this.mLogin + "mLoginDb:" + this.mLoginDb + "mPassword" + this.mPassword + "mPasswordDb" + this.mPasswordDb);
                if ((this.mLogin != null && !this.mLogin.equals(this.mLoginDb)) || (this.mPassword != null && !this.mPassword.equals(this.mPasswordDb))) {
                    updateValues.put("user", this.mLoginDb);
                    updateValues.put(Telephony.Carriers.PASSWORD, this.mPasswordDb);
                }
                if (updateValues.size() > 0) {
                    this.mContext.getContentResolver().update(Telephony.Carriers.CONTENT_URI, updateValues, selection, null);
                    this.mIsUpdateApnParams = false;
                }
            }
            cursor.close();
        }
    }

    private void deleteOrRestoreApnParams() {
        if (this.mIsUpdateApnParams) {
            restoreApnParams();
        } else {
            deleteApnParams();
        }
    }

    private int getCurrentSubId() {
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        if (subId != null) {
            int currentSubId = subId[0];
            return currentSubId;
        }
        CatLog.d("[BIP]", "getCurrentSubId: invalid subId");
        return -1;
    }

    private boolean isCurrentConnectionInService(int currentSubId) {
        int phoneId = SubscriptionManager.getPhoneId(currentSubId);
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            CatLog.d("[BIP]", "isCurrentConnectionInService(): invalid phone id");
            return false;
        }
        Phone myPhone = PhoneFactory.getPhone(phoneId);
        if (myPhone == null) {
            CatLog.d("[BIP]", "isCurrentConnectionInService(): phone null");
            return false;
        }
        ServiceStateTracker sst = myPhone.getServiceStateTracker();
        if (sst == null) {
            CatLog.d("[BIP]", "isCurrentConnectionInService(): sst null");
            return false;
        }
        if (sst.getCurrentDataConnectionState() == 0) {
            CatLog.d("[BIP]", "isCurrentConnectionInService(): in service");
            return true;
        }
        CatLog.d("[BIP]", "isCurrentConnectionInService(): not in service");
        return false;
    }

    private boolean checkDataCapability(BipCmdMessage cmdMsg) {
        TelephonyManager mTelMan = (TelephonyManager) this.mContext.getSystemService("phone");
        int simInsertedCount = 0;
        for (int i = 0; i < mSimCount; i++) {
            if (mTelMan.hasIccCard(i)) {
                simInsertedCount++;
            }
        }
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        if (subId != null) {
            int currentSubId = subId[0];
            CatLog.d("[BIP]", "checkDataCapability: simInsertedCount:" + simInsertedCount + " currentSubId:" + currentSubId + " defaultDataSubId:" + defaultDataSubId);
            if (simInsertedCount >= 2 && cmdMsg.mBearerDesc != null && ((2 == cmdMsg.mBearerDesc.bearerType || 3 == cmdMsg.mBearerDesc.bearerType || 9 == cmdMsg.mBearerDesc.bearerType || 11 == cmdMsg.mBearerDesc.bearerType) && currentSubId != defaultDataSubId)) {
                CatLog.d("[BIP]", "checkDataCapability: return false");
                return false;
            }
            CatLog.d("[BIP]", "checkDataCapability: return true");
            return true;
        }
        CatLog.d("[BIP]", "checkDataCapability: invalid subId");
        return false;
    }

    public int getChannelId() {
        CatLog.d("[BIP]", "BM-getChannelId: channel id is " + this.mChannelId);
        return this.mChannelId;
    }

    public int getFreeChannelId() {
        return this.mBipChannelManager.getFreeChannelId();
    }

    public void openChannelCompleted(int ret, Channel lChannel) {
        CatLog.d("[BIP]", "BM-openChannelCompleted: ret: " + ret);
        if (ret == 3) {
            this.mCurrentCmd.mBufferSize = this.mBufferSize;
        }
        if (ret == 0 || ret == 3) {
            this.mChannelStatus = 4;
            this.mBipChannelManager.addChannel(this.mChannelId, lChannel);
        } else {
            this.mBipChannelManager.releaseChannelId(this.mChannelId, 2);
            this.mChannelStatus = 7;
        }
        this.mCurrentCmd.mChannelStatusData = lChannel.mChannelStatusData;
        if (!this.mIsOpenInProgress || this.isConnMgrIntentTimeout) {
            return;
        }
        this.mIsOpenInProgress = false;
        this.mIsNetworkAvailableReceived = false;
        Message response = this.mBipSrvHandler.obtainMessage(13, ret, 0, this.mCurrentCmd);
        response.arg1 = ret;
        response.obj = this.mCurrentCmd;
        this.mBipSrvHandler.sendMessage(response);
    }

    public BipChannelManager getBipChannelManager() {
        return this.mBipChannelManager;
    }

    public boolean isTestSim() {
        CatLog.d("[BIP]", "isTestSim slotId: " + this.mSlotId);
        if (this.mSlotId == 0 && SystemProperties.get("gsm.sim.ril.testsim", "0").equals("1")) {
            return true;
        }
        if (1 == this.mSlotId && SystemProperties.get("gsm.sim.ril.testsim.2", "0").equals("1")) {
            return true;
        }
        if (2 == this.mSlotId && SystemProperties.get("gsm.sim.ril.testsim.3", "0").equals("1")) {
            return true;
        }
        return 3 == this.mSlotId && SystemProperties.get("gsm.sim.ril.testsim.4", "0").equals("1");
    }

    private boolean isVzWSupport() {
        if ("OP12".equals(SystemProperties.get("ro.operator.optr", UsimPBMemInfo.STRING_NOT_SET))) {
            return true;
        }
        return false;
    }

    protected class ConnectivityChangeThread implements Runnable {
        Intent intent;

        ConnectivityChangeThread(Intent in) {
            CatLog.d("[BIP]", "ConnectivityChangeThread Init");
            this.intent = in;
        }

        @Override
        public void run() {
            CatLog.d("[BIP]", "ConnectivityChangeThread Enter");
            CatLog.d("[BIP]", "Connectivity changed");
            NetworkInfo info = (NetworkInfo) this.intent.getExtra("networkInfo");
            String strSubId = this.intent.getStringExtra(IPplSmsFilter.KEY_SUB_ID);
            int subId = 0;
            if (strSubId == null) {
                CatLog.d("[BIP]", "No subId in intet extra.");
                return;
            }
            try {
                subId = Integer.parseInt(strSubId);
            } catch (NumberFormatException e) {
                CatLog.e("[BIP]", "Invalid long string. strSubId: " + strSubId);
            }
            if (!SubscriptionManager.isValidSubscriptionId(subId)) {
                CatLog.e("[BIP]", "Invalid subId: " + subId);
                return;
            }
            int simId = SubscriptionManager.getSlotId(subId);
            CatLog.d("[BIP]", "EXTRA_SIM_ID :" + simId + ",mSlotId:" + BipService.this.mSlotId);
            if (info == null || simId != BipService.this.mSlotId) {
                CatLog.d("[BIP]", "receive CONN intent sim!=" + BipService.this.mSlotId);
                return;
            }
            CatLog.d("[BIP]", "receive valid CONN intent");
            int type = info.getType();
            NetworkInfo.State state = info.getState();
            CatLog.d("[BIP]", "network type is " + type);
            CatLog.d("[BIP]", "network state is " + state);
            if ((!BipService.this.mApnType.equals("bip") || type != 42) && ((!BipService.this.mApnType.equals("default") || type != 0) && ((!BipService.this.mApnType.equals("internet") || type != 0) && (!BipService.this.mApnType.equals("fota") || type != 10)))) {
                return;
            }
            if (!BipService.this.isConnMgrIntentTimeout) {
                BipService.this.mBipSrvHandler.removeMessages(10);
            }
            if (state == NetworkInfo.State.CONNECTED) {
                CatLog.d("[BIP]", "network state - connected.");
                return;
            }
            if (state != NetworkInfo.State.DISCONNECTED) {
                return;
            }
            CatLog.d("[BIP]", "network state - disconnected");
            synchronized (BipService.this.mCloseLock) {
                CatLog.d("[BIP]", "mIsCloseInProgress: " + BipService.this.mIsCloseInProgress + " mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                if (BipService.this.mIsCloseInProgress) {
                    CatLog.d("[BIP]", "Return TR for close channel.");
                    BipService.this.mBipSrvHandler.removeMessages(12);
                    BipService.this.mIsCloseInProgress = false;
                    Message response = BipService.this.mBipSrvHandler.obtainMessage(16, 0, 0, BipService.this.mCurrentCmd);
                    BipService.this.mBipSrvHandler.sendMessage(response);
                } else if (BipService.this.mPreviousKeepChannelId != 0) {
                    BipService.this.mPreviousKeepChannelId = 0;
                    BipService.this.mBipSrvHandler.removeMessages(12);
                    Message response2 = BipService.this.mBipSrvHandler.obtainMessage(13);
                    BipService.this.openChannel(BipService.this.mCurrentCmd, response2);
                }
            }
        }
    }

    public void setConnMgrTimeoutFlag(boolean flag) {
        this.isConnMgrIntentTimeout = flag;
    }

    public void setOpenInProgressFlag(boolean flag) {
        this.mIsOpenInProgress = flag;
    }

    private class RecvDataRunnable implements Runnable {
        BipCmdMessage cmdMsg;
        int requestDataSize;
        Message response;
        ReceiveDataResult result;

        public RecvDataRunnable(int size, ReceiveDataResult result, BipCmdMessage cmdMsg, Message response) {
            this.requestDataSize = size;
            this.result = result;
            this.cmdMsg = cmdMsg;
            this.response = response;
        }

        @Override
        public void run() {
            int errCode;
            CatLog.d("[BIP]", "BM-receiveData: start to receive data");
            Channel lChannel = BipService.this.mBipChannelManager.getChannel(this.cmdMsg.mReceiveDataCid);
            if (lChannel == null) {
                errCode = 5;
            } else {
                synchronized (lChannel.mLock) {
                    lChannel.isReceiveDataTRSent = false;
                }
                errCode = lChannel.receiveData(this.requestDataSize, this.result);
            }
            this.cmdMsg.mChannelData = this.result.buffer;
            this.cmdMsg.mRemainingDataLength = this.result.remainingCount;
            this.response.arg1 = errCode;
            this.response.obj = this.cmdMsg;
            BipService.this.mBipSrvHandler.sendMessage(this.response);
            if (lChannel != null) {
                synchronized (lChannel.mLock) {
                    lChannel.isReceiveDataTRSent = true;
                    if (lChannel.mRxBufferCount == 0) {
                        CatLog.d("[BIP]", "BM-receiveData: notify waiting channel!");
                        lChannel.mLock.notify();
                    }
                }
            } else {
                CatLog.e("[BIP]", "BM-receiveData: null channel.");
            }
            CatLog.d("[BIP]", "BM-receiveData: end to receive data. Result code = " + errCode);
        }
    }
}
