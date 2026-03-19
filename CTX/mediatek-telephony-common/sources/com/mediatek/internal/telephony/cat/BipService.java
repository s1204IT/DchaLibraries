package com.mediatek.internal.telephony.cat;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
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
import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.ComprehensionTlvTag;
import com.android.internal.telephony.cat.ResponseData;
import com.android.internal.telephony.cat.ResultCode;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkRIL;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.ppl.IPplSmsFilter;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class BipService {
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
    protected static final int MSG_ID_BIP_WAIT_DATA_READY_TIMEOUT = 23;
    protected static final int MSG_ID_CLOSE_CHANNEL_DONE = 16;
    protected static final int MSG_ID_EVENT_NOTIFY = 19;
    protected static final int MSG_ID_GET_CHANNEL_STATUS_DONE = 17;
    protected static final int MSG_ID_OPEN_CHANNEL_DONE = 13;
    protected static final int MSG_ID_RECEIVE_DATA_DONE = 15;
    protected static final int MSG_ID_RIL_MSG_DECODED = 20;
    protected static final int MSG_ID_SEND_DATA_DONE = 14;
    private static final String PROPERTY_IA_APN = "vendor.ril.radio.ia-apn";
    private static final String PROPERTY_PERSIST_IA_APN = "persist.vendor.radio.ia-apn";
    private static final int WAIT_DATA_IN_SERVICE_TIMEOUT = 5000;
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
    private CommandParams mCachedParams;
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
    protected volatile MtkCatCmdMessage mCurrentSetupEventCmd;
    private BipCmdMessage mCurrntCmd;
    boolean mDNSaddrequest;
    OtherAddress mDataDestinationAddress;
    private List<InetAddress> mDnsAddres;
    private Handler mHandler;
    private boolean mIsApnInserting;
    private boolean mIsCloseInProgress;
    protected boolean mIsConnectTimeout;
    private volatile boolean mIsListenChannelStatus;
    private volatile boolean mIsListenDataAvailable;
    private boolean mIsNetworkAvailableReceived;
    protected boolean mIsOpenChannelOverWifi;
    private boolean mIsOpenInProgress;
    private boolean mIsUpdateApnParams;
    int mLinkMode;
    OtherAddress mLocalAddress;
    String mLogin;
    private String mLoginDb;
    private MtkRIL mMtkCmdIf;
    private int mNeedRetryNum;
    private Network mNetwork;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private BroadcastReceiver mNetworkConnReceiver;
    private boolean mNetworkConnReceiverRegistered;
    private NetworkRequest mNetworkRequest;
    private String mNumeric;
    String mPassword;
    private String mPasswordDb;
    private int mPreviousKeepChannelId;
    private int mPreviousProtocolType;
    private final Object mReleaseNetworkLock;
    private int mSlotId;
    TransportProtocol mTransportProtocol;
    private Uri mUri;

    public BipService(Context context, Handler handler, int i) {
        this.mHandler = null;
        this.mCachedParams = null;
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
        this.mReleaseNetworkLock = new Object();
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
        this.mPreviousProtocolType = 0;
        this.isConnMgrIntentTimeout = false;
        this.mBipChannelManager = null;
        this.mBipMsgDecoder = null;
        this.mCurrntCmd = null;
        this.mCmdIf = null;
        this.mIsOpenInProgress = false;
        this.mIsCloseInProgress = false;
        this.mIsNetworkAvailableReceived = false;
        this.mIsOpenChannelOverWifi = false;
        this.mIsConnectTimeout = false;
        this.mNetworkRequest = null;
        this.mApnType = "bip";
        this.mIsUpdateApnParams = false;
        this.mNeedRetryNum = 4;
        this.mNumeric = "";
        this.mUri = null;
        this.mLoginDb = "";
        this.mPasswordDb = "";
        this.mApnTypeDb = "";
        this.mBipSrvHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                AsyncResult asyncResult;
                MtkCatLog.d(this, "handleMessage[" + message.what + "]");
                String str = null;
                switch (message.what) {
                    case 10:
                        MtkCatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CONN_MGR_TIMEOUT");
                        BipService.this.isConnMgrIntentTimeout = true;
                        BipService.this.disconnect();
                        return;
                    case 11:
                        MtkCatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CONN_DELAY_TIMEOUT");
                        BipService.this.acquireNetwork();
                        return;
                    case 12:
                        MtkCatLog.d("[BIP]", "handleMessage MSG_ID_BIP_DISCONNECT_TIMEOUT");
                        synchronized (BipService.this.mCloseLock) {
                            MtkCatLog.d("[BIP]", "mIsCloseInProgress: " + BipService.this.mIsCloseInProgress + " mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                            if (true == BipService.this.mIsCloseInProgress) {
                                BipService.this.mIsCloseInProgress = false;
                                BipService.this.mBipSrvHandler.sendMessage(BipService.this.mBipSrvHandler.obtainMessage(16, 0, 0, BipService.this.mCurrentCmd));
                            } else if (BipService.this.mPreviousKeepChannelId != 0) {
                                BipService.this.mPreviousKeepChannelId = 0;
                                BipService.this.openChannel((BipCmdMessage) message.obj, BipService.this.mBipSrvHandler.obtainMessage(13));
                            }
                            break;
                        }
                        return;
                    case 13:
                        int i2 = message.arg1;
                        BipCmdMessage bipCmdMessage = (BipCmdMessage) message.obj;
                        if (BipService.this.mCurrentCmd != null) {
                            if (BipService.this.mCurrentCmd != null && BipService.this.mCurrentCmd.mCmdDet.typeOfCommand != AppInterface.CommandType.OPEN_CHANNEL.value()) {
                                MtkCatLog.d("[BIP]", "SS-handleMessage: skip open channel response because current cmd type is not OPEN_CHANNEL");
                                return;
                            }
                            if (8 == (bipCmdMessage.getCmdQualifier() & 8)) {
                                if (i2 != 0) {
                                    BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.BIP_ERROR, true, 0, new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, bipCmdMessage.mDnsServerAddress));
                                    return;
                                }
                                bipCmdMessage.mChannelStatusData.mChannelStatus = 128;
                                bipCmdMessage.mChannelStatusData.isActivated = true;
                                bipCmdMessage.mChannelStatusData.mChannelId = BipService.this.mBipChannelManager.getFreeChannelId();
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.OK, false, 0, new OpenChannelResponseDataEx(bipCmdMessage.mChannelStatusData, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, bipCmdMessage.mDnsServerAddress));
                                return;
                            }
                            int i3 = bipCmdMessage.mTransportProtocol != null ? bipCmdMessage.mTransportProtocol.protocolType : 0;
                            if (i2 == 0) {
                                OpenChannelResponseDataEx openChannelResponseDataEx = new OpenChannelResponseDataEx(bipCmdMessage.mChannelStatusData, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i3);
                                MtkCatLog.d("[BIP]", "SS-handleMessage: open channel successfully");
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.OK, false, 0, openChannelResponseDataEx);
                                return;
                            }
                            if (i2 == 3) {
                                OpenChannelResponseDataEx openChannelResponseDataEx2 = new OpenChannelResponseDataEx(bipCmdMessage.mChannelStatusData, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i3);
                                MtkCatLog.d("[BIP]", "SS-handleMessage: Modified parameters");
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.PRFRMD_WITH_MODIFICATION, false, 0, openChannelResponseDataEx2);
                                return;
                            } else {
                                if (i2 != 6) {
                                    BipService.this.releaseRequest();
                                    BipService.this.resetLocked();
                                    OpenChannelResponseDataEx openChannelResponseDataEx3 = new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i3);
                                    MtkCatLog.d("[BIP]", "SS-handleMessage: open channel failed");
                                    if (!BipService.this.isSprintSupport() || !BipService.this.mIsConnectTimeout) {
                                        BipService.this.sendTerminalResponse(bipCmdMessage.mCmdDet, ResultCode.BIP_ERROR, true, 0, openChannelResponseDataEx3);
                                        return;
                                    } else {
                                        BipService.this.handleCommand(BipService.this.mCachedParams, true);
                                        return;
                                    }
                                }
                                OpenChannelResponseDataEx openChannelResponseDataEx4 = new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i3);
                                MtkCatLog.d("[BIP]", "SS-handleMessage: ME is busy on call");
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, openChannelResponseDataEx4);
                                return;
                            }
                        }
                        MtkCatLog.d("[BIP]", "SS-handleMessage: skip open channel response because current cmd is null");
                        return;
                    case 14:
                        int i4 = message.arg1;
                        int i5 = message.arg2;
                        BipCmdMessage bipCmdMessage2 = (BipCmdMessage) message.obj;
                        SendDataResponseData sendDataResponseData = new SendDataResponseData(i5);
                        if (i4 == 0) {
                            BipService.this.sendTerminalResponse(bipCmdMessage2.mCmdDet, ResultCode.OK, false, 0, sendDataResponseData);
                            return;
                        } else if (i4 == 7) {
                            BipService.this.sendTerminalResponse(bipCmdMessage2.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                            return;
                        } else {
                            BipService.this.sendTerminalResponse(bipCmdMessage2.mCmdDet, ResultCode.BIP_ERROR, true, 0, sendDataResponseData);
                            return;
                        }
                    case 15:
                        int i6 = message.arg1;
                        BipCmdMessage bipCmdMessage3 = (BipCmdMessage) message.obj;
                        ReceiveDataResponseData receiveDataResponseData = new ReceiveDataResponseData(bipCmdMessage3.mChannelData, bipCmdMessage3.mRemainingDataLength);
                        if (i6 == 0) {
                            BipService.this.sendTerminalResponse(bipCmdMessage3.mCmdDet, ResultCode.OK, false, 0, receiveDataResponseData);
                            return;
                        } else if (i6 == 9) {
                            BipService.this.sendTerminalResponse(bipCmdMessage3.mCmdDet, ResultCode.PRFRMD_WITH_MISSING_INFO, false, 0, receiveDataResponseData);
                            return;
                        } else {
                            BipService.this.sendTerminalResponse(bipCmdMessage3.mCmdDet, ResultCode.BIP_ERROR, true, 0, null);
                            return;
                        }
                    case 16:
                        BipCmdMessage bipCmdMessage4 = (BipCmdMessage) message.obj;
                        if (message.arg1 == 0) {
                            BipService.this.sendTerminalResponse(bipCmdMessage4.mCmdDet, ResultCode.OK, false, 0, null);
                            return;
                        } else if (message.arg1 == 7) {
                            BipService.this.sendTerminalResponse(bipCmdMessage4.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                            return;
                        } else {
                            if (message.arg1 == 8) {
                                BipService.this.sendTerminalResponse(bipCmdMessage4.mCmdDet, ResultCode.BIP_ERROR, true, 2, null);
                                return;
                            }
                            return;
                        }
                    case 17:
                        int i7 = message.arg1;
                        BipCmdMessage bipCmdMessage5 = (BipCmdMessage) message.obj;
                        ArrayList arrayList = (ArrayList) bipCmdMessage5.mChannelStatusList;
                        MtkCatLog.d("[BIP]", "SS-handleCmdResponse: MSG_ID_GET_CHANNEL_STATUS_DONE:" + arrayList.size());
                        BipService.this.sendTerminalResponse(bipCmdMessage5.mCmdDet, ResultCode.OK, false, 0, new GetMultipleChannelStatusResponseData(arrayList));
                        return;
                    case 18:
                    case 19:
                        MtkCatLog.d(this, "ril message arrived, slotid: " + BipService.this.mSlotId);
                        if (message.obj != null && (asyncResult = (AsyncResult) message.obj) != null && asyncResult.result != null) {
                            try {
                                str = (String) asyncResult.result;
                            } catch (ClassCastException e) {
                                return;
                            }
                            break;
                        }
                        BipService.this.mBipMsgDecoder.sendStartDecodingMessageParams(new MtkRilMessage(message.what, str));
                        return;
                    case 20:
                        BipService.this.handleRilMsg((MtkRilMessage) message.obj);
                        return;
                    case 21:
                        MtkCatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CHANNEL_KEEP_TIMEOUT");
                        MtkCatLog.d("[BIP]", "mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                        if (BipService.this.mPreviousKeepChannelId != 0) {
                            Channel channel = BipService.this.mBipChannelManager.getChannel(BipService.this.mPreviousKeepChannelId);
                            BipService.this.releaseRequest();
                            BipService.this.resetLocked();
                            if (channel != null) {
                                channel.closeChannel();
                            }
                            BipService.this.mBipChannelManager.removeChannel(BipService.this.mPreviousKeepChannelId);
                            BipService.this.deleteApnParams();
                            BipService.this.setPdnReuse("1");
                            BipService.this.mChannel = null;
                            BipService.this.mChannelStatus = 2;
                            BipService.this.mPreviousKeepChannelId = 0;
                            BipService.this.mApn = null;
                            BipService.this.mLogin = null;
                            BipService.this.mPassword = null;
                            return;
                        }
                        return;
                    case 22:
                        int i8 = message.arg1;
                        MtkCatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel id: " + i8);
                        if (i8 > 0 && i8 <= 7 && BipService.this.mBipChannelManager.isChannelIdOccupied(i8)) {
                            Channel channel2 = BipService.this.mBipChannelManager.getChannel(i8);
                            MtkCatLog.d("[BIP]", "channel protocolType:" + channel2.mProtocolType);
                            if (1 == channel2.mProtocolType || 2 == channel2.mProtocolType) {
                                channel2.closeChannel();
                                BipService.this.mBipChannelManager.removeChannel(i8);
                                return;
                            } else {
                                MtkCatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel type: " + channel2.mProtocolType);
                                return;
                            }
                        }
                        MtkCatLog.d("[BIP]", "channel already closed");
                        return;
                    case 23:
                        MtkCatLog.d("[BIP]", "MSG_ID_BIP_WAIT_DATA_READY_TIMEOUT");
                        BipService.this.handleCommand((CommandParams) message.obj, true);
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
                MtkCatLog.d("[BIP]", "mNetworkConnReceiver:" + BipService.this.mIsOpenInProgress + " , " + BipService.this.mIsCloseInProgress + " , " + BipService.this.isConnMgrIntentTimeout + " , " + BipService.this.mPreviousKeepChannelId);
                if (BipService.this.mBipChannelManager != null) {
                    MtkCatLog.d("[BIP]", "isClientChannelOpened:" + BipService.this.mBipChannelManager.isClientChannelOpened());
                }
                if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    if ((BipService.this.mIsOpenInProgress && !BipService.this.isConnMgrIntentTimeout) || true == BipService.this.mIsCloseInProgress || BipService.this.mPreviousKeepChannelId != 0) {
                        MtkCatLog.d("[BIP]", "Connectivity changed onReceive Enter");
                        new Thread(BipService.this.new ConnectivityChangeThread(intent)).start();
                        MtkCatLog.d("[BIP]", "Connectivity changed onReceive Leave");
                    }
                }
            }
        };
        MtkCatLog.d("[BIP]", "Construct BipService");
        if (context == null) {
            MtkCatLog.e("[BIP]", "Fail to construct BipService");
            return;
        }
        this.mContext = context;
        this.mSlotId = i;
        MtkCatLog.d("[BIP]", "Construct instance sim id: " + i);
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mHandler = handler;
        this.mBipChannelManager = new BipChannelManager();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mNetworkConnReceiver, intentFilter);
        this.mNetworkConnReceiverRegistered = true;
        newThreadToDelelteApn();
    }

    public BipService(Context context, Handler handler, int i, CommandsInterface commandsInterface, IccFileHandler iccFileHandler) {
        this.mHandler = null;
        this.mCachedParams = null;
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
        this.mReleaseNetworkLock = new Object();
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
        this.mPreviousProtocolType = 0;
        this.isConnMgrIntentTimeout = false;
        this.mBipChannelManager = null;
        this.mBipMsgDecoder = null;
        this.mCurrntCmd = null;
        this.mCmdIf = null;
        this.mIsOpenInProgress = false;
        this.mIsCloseInProgress = false;
        this.mIsNetworkAvailableReceived = false;
        this.mIsOpenChannelOverWifi = false;
        this.mIsConnectTimeout = false;
        this.mNetworkRequest = null;
        this.mApnType = "bip";
        this.mIsUpdateApnParams = false;
        this.mNeedRetryNum = 4;
        this.mNumeric = "";
        this.mUri = null;
        this.mLoginDb = "";
        this.mPasswordDb = "";
        this.mApnTypeDb = "";
        this.mBipSrvHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                AsyncResult asyncResult;
                MtkCatLog.d(this, "handleMessage[" + message.what + "]");
                String str = null;
                switch (message.what) {
                    case 10:
                        MtkCatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CONN_MGR_TIMEOUT");
                        BipService.this.isConnMgrIntentTimeout = true;
                        BipService.this.disconnect();
                        return;
                    case 11:
                        MtkCatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CONN_DELAY_TIMEOUT");
                        BipService.this.acquireNetwork();
                        return;
                    case 12:
                        MtkCatLog.d("[BIP]", "handleMessage MSG_ID_BIP_DISCONNECT_TIMEOUT");
                        synchronized (BipService.this.mCloseLock) {
                            MtkCatLog.d("[BIP]", "mIsCloseInProgress: " + BipService.this.mIsCloseInProgress + " mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                            if (true == BipService.this.mIsCloseInProgress) {
                                BipService.this.mIsCloseInProgress = false;
                                BipService.this.mBipSrvHandler.sendMessage(BipService.this.mBipSrvHandler.obtainMessage(16, 0, 0, BipService.this.mCurrentCmd));
                            } else if (BipService.this.mPreviousKeepChannelId != 0) {
                                BipService.this.mPreviousKeepChannelId = 0;
                                BipService.this.openChannel((BipCmdMessage) message.obj, BipService.this.mBipSrvHandler.obtainMessage(13));
                            }
                            break;
                        }
                        return;
                    case 13:
                        int i2 = message.arg1;
                        BipCmdMessage bipCmdMessage = (BipCmdMessage) message.obj;
                        if (BipService.this.mCurrentCmd != null) {
                            if (BipService.this.mCurrentCmd != null && BipService.this.mCurrentCmd.mCmdDet.typeOfCommand != AppInterface.CommandType.OPEN_CHANNEL.value()) {
                                MtkCatLog.d("[BIP]", "SS-handleMessage: skip open channel response because current cmd type is not OPEN_CHANNEL");
                                return;
                            }
                            if (8 == (bipCmdMessage.getCmdQualifier() & 8)) {
                                if (i2 != 0) {
                                    BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.BIP_ERROR, true, 0, new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, bipCmdMessage.mDnsServerAddress));
                                    return;
                                }
                                bipCmdMessage.mChannelStatusData.mChannelStatus = 128;
                                bipCmdMessage.mChannelStatusData.isActivated = true;
                                bipCmdMessage.mChannelStatusData.mChannelId = BipService.this.mBipChannelManager.getFreeChannelId();
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.OK, false, 0, new OpenChannelResponseDataEx(bipCmdMessage.mChannelStatusData, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, bipCmdMessage.mDnsServerAddress));
                                return;
                            }
                            int i3 = bipCmdMessage.mTransportProtocol != null ? bipCmdMessage.mTransportProtocol.protocolType : 0;
                            if (i2 == 0) {
                                OpenChannelResponseDataEx openChannelResponseDataEx = new OpenChannelResponseDataEx(bipCmdMessage.mChannelStatusData, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i3);
                                MtkCatLog.d("[BIP]", "SS-handleMessage: open channel successfully");
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.OK, false, 0, openChannelResponseDataEx);
                                return;
                            }
                            if (i2 == 3) {
                                OpenChannelResponseDataEx openChannelResponseDataEx2 = new OpenChannelResponseDataEx(bipCmdMessage.mChannelStatusData, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i3);
                                MtkCatLog.d("[BIP]", "SS-handleMessage: Modified parameters");
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.PRFRMD_WITH_MODIFICATION, false, 0, openChannelResponseDataEx2);
                                return;
                            } else {
                                if (i2 != 6) {
                                    BipService.this.releaseRequest();
                                    BipService.this.resetLocked();
                                    OpenChannelResponseDataEx openChannelResponseDataEx3 = new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i3);
                                    MtkCatLog.d("[BIP]", "SS-handleMessage: open channel failed");
                                    if (!BipService.this.isSprintSupport() || !BipService.this.mIsConnectTimeout) {
                                        BipService.this.sendTerminalResponse(bipCmdMessage.mCmdDet, ResultCode.BIP_ERROR, true, 0, openChannelResponseDataEx3);
                                        return;
                                    } else {
                                        BipService.this.handleCommand(BipService.this.mCachedParams, true);
                                        return;
                                    }
                                }
                                OpenChannelResponseDataEx openChannelResponseDataEx4 = new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i3);
                                MtkCatLog.d("[BIP]", "SS-handleMessage: ME is busy on call");
                                BipService.this.sendTerminalResponse(BipService.this.mCurrentCmd.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, openChannelResponseDataEx4);
                                return;
                            }
                        }
                        MtkCatLog.d("[BIP]", "SS-handleMessage: skip open channel response because current cmd is null");
                        return;
                    case 14:
                        int i4 = message.arg1;
                        int i5 = message.arg2;
                        BipCmdMessage bipCmdMessage2 = (BipCmdMessage) message.obj;
                        SendDataResponseData sendDataResponseData = new SendDataResponseData(i5);
                        if (i4 == 0) {
                            BipService.this.sendTerminalResponse(bipCmdMessage2.mCmdDet, ResultCode.OK, false, 0, sendDataResponseData);
                            return;
                        } else if (i4 == 7) {
                            BipService.this.sendTerminalResponse(bipCmdMessage2.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                            return;
                        } else {
                            BipService.this.sendTerminalResponse(bipCmdMessage2.mCmdDet, ResultCode.BIP_ERROR, true, 0, sendDataResponseData);
                            return;
                        }
                    case 15:
                        int i6 = message.arg1;
                        BipCmdMessage bipCmdMessage3 = (BipCmdMessage) message.obj;
                        ReceiveDataResponseData receiveDataResponseData = new ReceiveDataResponseData(bipCmdMessage3.mChannelData, bipCmdMessage3.mRemainingDataLength);
                        if (i6 == 0) {
                            BipService.this.sendTerminalResponse(bipCmdMessage3.mCmdDet, ResultCode.OK, false, 0, receiveDataResponseData);
                            return;
                        } else if (i6 == 9) {
                            BipService.this.sendTerminalResponse(bipCmdMessage3.mCmdDet, ResultCode.PRFRMD_WITH_MISSING_INFO, false, 0, receiveDataResponseData);
                            return;
                        } else {
                            BipService.this.sendTerminalResponse(bipCmdMessage3.mCmdDet, ResultCode.BIP_ERROR, true, 0, null);
                            return;
                        }
                    case 16:
                        BipCmdMessage bipCmdMessage4 = (BipCmdMessage) message.obj;
                        if (message.arg1 == 0) {
                            BipService.this.sendTerminalResponse(bipCmdMessage4.mCmdDet, ResultCode.OK, false, 0, null);
                            return;
                        } else if (message.arg1 == 7) {
                            BipService.this.sendTerminalResponse(bipCmdMessage4.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                            return;
                        } else {
                            if (message.arg1 == 8) {
                                BipService.this.sendTerminalResponse(bipCmdMessage4.mCmdDet, ResultCode.BIP_ERROR, true, 2, null);
                                return;
                            }
                            return;
                        }
                    case 17:
                        int i7 = message.arg1;
                        BipCmdMessage bipCmdMessage5 = (BipCmdMessage) message.obj;
                        ArrayList arrayList = (ArrayList) bipCmdMessage5.mChannelStatusList;
                        MtkCatLog.d("[BIP]", "SS-handleCmdResponse: MSG_ID_GET_CHANNEL_STATUS_DONE:" + arrayList.size());
                        BipService.this.sendTerminalResponse(bipCmdMessage5.mCmdDet, ResultCode.OK, false, 0, new GetMultipleChannelStatusResponseData(arrayList));
                        return;
                    case 18:
                    case 19:
                        MtkCatLog.d(this, "ril message arrived, slotid: " + BipService.this.mSlotId);
                        if (message.obj != null && (asyncResult = (AsyncResult) message.obj) != null && asyncResult.result != null) {
                            try {
                                str = (String) asyncResult.result;
                            } catch (ClassCastException e) {
                                return;
                            }
                            break;
                        }
                        BipService.this.mBipMsgDecoder.sendStartDecodingMessageParams(new MtkRilMessage(message.what, str));
                        return;
                    case 20:
                        BipService.this.handleRilMsg((MtkRilMessage) message.obj);
                        return;
                    case 21:
                        MtkCatLog.d("[BIP]", "handleMessage MSG_ID_BIP_CHANNEL_KEEP_TIMEOUT");
                        MtkCatLog.d("[BIP]", "mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                        if (BipService.this.mPreviousKeepChannelId != 0) {
                            Channel channel = BipService.this.mBipChannelManager.getChannel(BipService.this.mPreviousKeepChannelId);
                            BipService.this.releaseRequest();
                            BipService.this.resetLocked();
                            if (channel != null) {
                                channel.closeChannel();
                            }
                            BipService.this.mBipChannelManager.removeChannel(BipService.this.mPreviousKeepChannelId);
                            BipService.this.deleteApnParams();
                            BipService.this.setPdnReuse("1");
                            BipService.this.mChannel = null;
                            BipService.this.mChannelStatus = 2;
                            BipService.this.mPreviousKeepChannelId = 0;
                            BipService.this.mApn = null;
                            BipService.this.mLogin = null;
                            BipService.this.mPassword = null;
                            return;
                        }
                        return;
                    case 22:
                        int i8 = message.arg1;
                        MtkCatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel id: " + i8);
                        if (i8 > 0 && i8 <= 7 && BipService.this.mBipChannelManager.isChannelIdOccupied(i8)) {
                            Channel channel2 = BipService.this.mBipChannelManager.getChannel(i8);
                            MtkCatLog.d("[BIP]", "channel protocolType:" + channel2.mProtocolType);
                            if (1 == channel2.mProtocolType || 2 == channel2.mProtocolType) {
                                channel2.closeChannel();
                                BipService.this.mBipChannelManager.removeChannel(i8);
                                return;
                            } else {
                                MtkCatLog.d("[BIP]", "MSG_ID_BIP_CHANNEL_DELAYED_CLOSE: channel type: " + channel2.mProtocolType);
                                return;
                            }
                        }
                        MtkCatLog.d("[BIP]", "channel already closed");
                        return;
                    case 23:
                        MtkCatLog.d("[BIP]", "MSG_ID_BIP_WAIT_DATA_READY_TIMEOUT");
                        BipService.this.handleCommand((CommandParams) message.obj, true);
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
                MtkCatLog.d("[BIP]", "mNetworkConnReceiver:" + BipService.this.mIsOpenInProgress + " , " + BipService.this.mIsCloseInProgress + " , " + BipService.this.isConnMgrIntentTimeout + " , " + BipService.this.mPreviousKeepChannelId);
                if (BipService.this.mBipChannelManager != null) {
                    MtkCatLog.d("[BIP]", "isClientChannelOpened:" + BipService.this.mBipChannelManager.isClientChannelOpened());
                }
                if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    if ((BipService.this.mIsOpenInProgress && !BipService.this.isConnMgrIntentTimeout) || true == BipService.this.mIsCloseInProgress || BipService.this.mPreviousKeepChannelId != 0) {
                        MtkCatLog.d("[BIP]", "Connectivity changed onReceive Enter");
                        new Thread(BipService.this.new ConnectivityChangeThread(intent)).start();
                        MtkCatLog.d("[BIP]", "Connectivity changed onReceive Leave");
                    }
                }
            }
        };
        MtkCatLog.d("[BIP]", "Construct BipService " + i);
        if (context == null) {
            MtkCatLog.e("[BIP]", "Fail to construct BipService");
            return;
        }
        this.mContext = context;
        this.mSlotId = i;
        this.mCmdIf = commandsInterface;
        this.mMtkCmdIf = (MtkRIL) commandsInterface;
        this.mConnMgr = (ConnectivityManager) context.getSystemService("connectivity");
        this.mHandler = handler;
        this.mBipChannelManager = new BipChannelManager();
        this.mBipMsgDecoder = BipRilMessageDecoder.getInstance(this.mBipSrvHandler, iccFileHandler, this.mSlotId);
        if (this.mBipMsgDecoder == null) {
            MtkCatLog.d(this, "Null BipRilMessageDecoder instance");
            return;
        }
        this.mBipMsgDecoder.start();
        this.mMtkCmdIf.setOnBipProactiveCmd(this.mBipSrvHandler, 18, null);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mNetworkConnReceiver, intentFilter);
        this.mNetworkConnReceiverRegistered = true;
        newThreadToDelelteApn();
    }

    private ConnectivityManager getConnectivityManager() {
        if (this.mConnMgr == null) {
            this.mConnMgr = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mConnMgr;
    }

    public static BipService getInstance(Context context, Handler handler, int i) {
        MtkCatLog.d("[BIP]", "getInstance sim : " + i);
        if (mInstance == null) {
            mSimCount = TelephonyManager.getDefault().getSimCount();
            mInstance = new BipService[mSimCount];
            for (int i2 = 0; i2 < mSimCount; i2++) {
                mInstance[i2] = null;
            }
        }
        if (i < 0 || i > mSimCount) {
            MtkCatLog.d("[BIP]", "getInstance invalid sim : " + i);
            return null;
        }
        if (mInstance[i] == null) {
            mInstance[i] = new BipService(context, handler, i);
        }
        return mInstance[i];
    }

    public static BipService getInstance(Context context, Handler handler, int i, CommandsInterface commandsInterface, IccFileHandler iccFileHandler) {
        MtkCatLog.d("[BIP]", "getInstance sim : " + i);
        if (mInstance == null) {
            mSimCount = TelephonyManager.getDefault().getSimCount();
            mInstance = new BipService[mSimCount];
            for (int i2 = 0; i2 < mSimCount; i2++) {
                mInstance[i2] = null;
            }
        }
        if (i < 0 || i > mSimCount) {
            MtkCatLog.d("[BIP]", "getInstance invalid sim : " + i);
            return null;
        }
        if (mInstance[i] == null) {
            mInstance[i] = new BipService(context, handler, i, commandsInterface, iccFileHandler);
        }
        return mInstance[i];
    }

    public void dispose() {
        MtkCatLog.d("[BIP]", "Dispose slotId : " + this.mSlotId);
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
        if (this.mNetworkConnReceiverRegistered) {
            this.mContext.unregisterReceiver(this.mNetworkConnReceiver);
            this.mNetworkConnReceiverRegistered = false;
        }
        if (this.mBipSrvHandler != null) {
            this.mMtkCmdIf.unSetOnBipProactiveCmd(this.mBipSrvHandler);
        }
        if (this.mBipMsgDecoder != null) {
            this.mBipMsgDecoder.dispose();
        }
    }

    private void handleRilMsg(MtkRilMessage mtkRilMessage) {
        if (mtkRilMessage != null && mtkRilMessage.mId == 18) {
            try {
                CommandParams commandParams = (CommandParams) mtkRilMessage.mData;
                if (commandParams != null) {
                    if (mtkRilMessage.mResCode == ResultCode.OK) {
                        handleCommand(commandParams, true);
                    } else {
                        sendTerminalResponse(commandParams.mCmdDet, mtkRilMessage.mResCode, false, 0, null);
                    }
                }
            } catch (ClassCastException e) {
                MtkCatLog.d(this, "Fail to parse proactive command");
                if (this.mCurrntCmd != null) {
                    sendTerminalResponse(this.mCurrntCmd.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                }
            }
        }
    }

    private void checkPSEvent(MtkCatCmdMessage mtkCatCmdMessage) {
        this.mIsListenDataAvailable = false;
        this.mIsListenChannelStatus = false;
        for (int i : mtkCatCmdMessage.getSetEventList().eventList) {
            MtkCatLog.v(this, "Event: " + i);
            switch (i) {
                case 9:
                    this.mIsListenDataAvailable = true;
                    break;
                case 10:
                    this.mIsListenChannelStatus = true;
                    break;
            }
        }
    }

    void setSetupEventList(MtkCatCmdMessage mtkCatCmdMessage) {
        this.mCurrentSetupEventCmd = mtkCatCmdMessage;
        checkPSEvent(mtkCatCmdMessage);
    }

    boolean hasPsEvent(int i) {
        switch (i) {
            case 9:
                return this.mIsListenDataAvailable;
            case 10:
                return this.mIsListenChannelStatus;
            default:
                return false;
        }
    }

    private void handleCommand(CommandParams commandParams, boolean z) {
        int i;
        MtkCatLog.d(this, commandParams.getCommandType().name());
        BipCmdMessage bipCmdMessage = new BipCmdMessage(commandParams);
        switch (AnonymousClass6.$SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[commandParams.getCommandType().ordinal()]) {
            case 1:
                MtkCatLog.d(this, "SS-handleProactiveCommand: process OPEN_CHANNEL,slot id = " + this.mSlotId);
                PhoneConstants.State state = PhoneConstants.State.IDLE;
                CallManager callManager = CallManager.getInstance();
                int i2 = this.mSlotId;
                Phone phone = PhoneFactory.getPhone(i2);
                if (bipCmdMessage.mTransportProtocol != null) {
                    i = bipCmdMessage.mTransportProtocol.protocolType;
                } else {
                    i = 0;
                }
                if (phone == null) {
                    MtkCatLog.d(this, "myPhone is still null");
                    sendTerminalResponse(bipCmdMessage.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i));
                    return;
                }
                String str = SystemProperties.get("persist.vendor.ril.bip.disabled", "0");
                if (str != null && str.equals("1")) {
                    MtkCatLog.d(this, "BIP disabled");
                    sendTerminalResponse(bipCmdMessage.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 0, new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i));
                    return;
                }
                int voiceNetworkType = phone.getServiceState().getVoiceNetworkType();
                MtkCatLog.d(this, "networkType = " + voiceNetworkType);
                if ((voiceNetworkType <= 2 || voiceNetworkType == 16) && callManager != null) {
                    PhoneConstants.State state2 = callManager.getState();
                    MtkCatLog.d(this, "call_state" + state2);
                    if (state2 != PhoneConstants.State.IDLE) {
                        MtkCatLog.d(this, "SS-handleProactiveCommand: ME is busy on call");
                        bipCmdMessage.mChannelStatusData = new ChannelStatus(getFreeChannelId(), 0, 0);
                        bipCmdMessage.mChannelStatusData.mChannelStatus = 0;
                        this.mCurrentCmd = bipCmdMessage;
                        this.mBipSrvHandler.obtainMessage(13, 6, 0, bipCmdMessage).sendToTarget();
                        return;
                    }
                } else {
                    MtkCatLog.d(this, "SS-handleProactiveCommand: type:" + phone.getServiceState().getVoiceNetworkType() + ",or null callmgr");
                }
                if (isSprintSupport() && isWifiConnected() && !this.mIsConnectTimeout) {
                    this.mCachedParams = commandParams;
                    openChannelOverWifi(bipCmdMessage, this.mBipSrvHandler.obtainMessage(13));
                } else {
                    this.mIsConnectTimeout = false;
                    if (!isCurrentConnectionInService(i2)) {
                        if (isSprintSupport() && this.mNeedRetryNum != 0) {
                            MtkCatLog.d(this, "handleCommand: wait for data in service");
                            Message messageObtainMessage = this.mBipSrvHandler.obtainMessage(23);
                            messageObtainMessage.obj = commandParams;
                            this.mBipSrvHandler.sendMessageDelayed(messageObtainMessage, 5000L);
                            this.mNeedRetryNum--;
                            return;
                        }
                        this.mNeedRetryNum = 4;
                        sendTerminalResponse(bipCmdMessage.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 4, new OpenChannelResponseDataEx((ChannelStatus) null, bipCmdMessage.mBearerDesc, bipCmdMessage.mBufferSize, i));
                        return;
                    }
                    this.mNeedRetryNum = 4;
                    openChannel(bipCmdMessage, this.mBipSrvHandler.obtainMessage(13));
                }
                break;
                break;
            case 2:
                MtkCatLog.d(this, "SS-handleProactiveCommand: process CLOSE_CHANNEL");
                closeChannel(bipCmdMessage, this.mBipSrvHandler.obtainMessage(16));
                break;
            case 3:
                MtkCatLog.d(this, "SS-handleProactiveCommand: process RECEIVE_DATA");
                receiveData(bipCmdMessage, this.mBipSrvHandler.obtainMessage(15));
                break;
            case 4:
                MtkCatLog.d(this, "SS-handleProactiveCommand: process SEND_DATA");
                sendData(bipCmdMessage, this.mBipSrvHandler.obtainMessage(14));
                break;
            case 5:
                MtkCatLog.d(this, "SS-handleProactiveCommand: process GET_CHANNEL_STATUS");
                this.mCmdMessage = bipCmdMessage;
                getChannelStatus(bipCmdMessage, this.mBipSrvHandler.obtainMessage(17));
                break;
            default:
                MtkCatLog.d(this, "Unsupported command");
                return;
        }
        this.mCurrntCmd = bipCmdMessage;
    }

    static class AnonymousClass6 {
        static final int[] $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType = new int[AppInterface.CommandType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_DATA.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    private void sendTerminalResponse(CommandDetails commandDetails, ResultCode resultCode, boolean z, int i, ResponseData responseData) {
        if (commandDetails == null) {
            MtkCatLog.e(this, "SS-sendTR: cmdDet is null");
            return;
        }
        MtkCatLog.d(this, "SS-sendTR: command type is " + commandDetails.typeOfCommand);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int iValue = ComprehensionTlvTag.COMMAND_DETAILS.value();
        if (commandDetails.compRequired) {
            iValue |= 128;
        }
        byteArrayOutputStream.write(iValue);
        byteArrayOutputStream.write(3);
        byteArrayOutputStream.write(commandDetails.commandNumber);
        byteArrayOutputStream.write(commandDetails.typeOfCommand);
        byteArrayOutputStream.write(commandDetails.commandQualifier);
        byteArrayOutputStream.write(ComprehensionTlvTag.DEVICE_IDENTITIES.value() | 128);
        byteArrayOutputStream.write(2);
        byteArrayOutputStream.write(DEV_ID_TERMINAL);
        byteArrayOutputStream.write(DEV_ID_UICC);
        int iValue2 = ComprehensionTlvTag.RESULT.value();
        if (commandDetails.compRequired) {
            iValue2 |= 128;
        }
        byteArrayOutputStream.write(iValue2);
        byteArrayOutputStream.write(z ? 2 : 1);
        byteArrayOutputStream.write(resultCode.value());
        if (z) {
            byteArrayOutputStream.write(i);
        }
        if (responseData != null) {
            MtkCatLog.d(this, "SS-sendTR: write response data into TR");
            responseData.format(byteArrayOutputStream);
        } else {
            MtkCatLog.d(this, "SS-sendTR: null resp.");
        }
        String strBytesToHexString = IccUtils.bytesToHexString(byteArrayOutputStream.toByteArray());
        MtkCatLog.d(this, "TERMINAL RESPONSE: " + strBytesToHexString);
        this.mCmdIf.sendTerminalResponse(strBytesToHexString, (Message) null);
    }

    private void connect() {
        MtkCatLog.d("[BIP]", "establishConnect");
        this.mCurrentCmd.mChannelStatusData.isActivated = true;
        MtkCatLog.d("[BIP]", "requestNetwork: establish data channel");
        int iEstablishLink = establishLink();
        if (iEstablishLink != 10) {
            if (iEstablishLink == 0 || iEstablishLink == 3) {
                MtkCatLog.d("[BIP]", "1 channel is activated");
                updateCurrentChannelStatus(128);
            } else {
                MtkCatLog.d("[BIP]", "2 channel is un-activated");
                updateCurrentChannelStatus(0);
            }
            this.mIsOpenInProgress = false;
            this.mIsNetworkAvailableReceived = false;
            this.mBipSrvHandler.sendMessage(this.mBipSrvHandler.obtainMessage(13, iEstablishLink, 0, this.mCurrentCmd));
        }
    }

    private void sendDelayedCloseChannel(int i) {
        Message messageObtainMessage = this.mBipSrvHandler.obtainMessage(22);
        messageObtainMessage.arg1 = i;
        this.mBipSrvHandler.sendMessageDelayed(messageObtainMessage, 5000L);
    }

    private void disconnect() {
        MtkCatLog.d("[BIP]", "disconnect: opening ? " + this.mIsOpenInProgress);
        if (!this.mIsOpenChannelOverWifi) {
            deleteOrRestoreApnParams();
            setPdnReuse("1");
        } else {
            this.mIsOpenChannelOverWifi = false;
        }
        if (true == this.mIsOpenInProgress && this.mChannelStatus != 4) {
            Channel channel = this.mBipChannelManager.getChannel(this.mChannelId);
            if (channel != null) {
                channel.closeChannel();
                this.mBipChannelManager.removeChannel(this.mChannelId);
            } else if (this.mTransportProtocol != null) {
                this.mBipChannelManager.releaseChannelId(this.mChannelId, this.mTransportProtocol.protocolType);
            }
            releaseRequest();
            resetLocked();
            this.mChannelStatus = 2;
            MtkCatLog.d("[BIP]", "disconnect(): mCurrentCmd = " + this.mCurrentCmd);
            if (this.mCurrentCmd.mChannelStatusData != null) {
                this.mCurrentCmd.mChannelStatusData.mChannelStatus = 0;
                this.mCurrentCmd.mChannelStatusData.isActivated = false;
            }
            this.mIsOpenInProgress = false;
            this.mBipSrvHandler.sendMessage(this.mBipSrvHandler.obtainMessage(13, 2, 0, this.mCurrentCmd));
            return;
        }
        ArrayList arrayList = new ArrayList();
        MtkCatLog.d("[BIP]", "this is a drop link");
        this.mChannelStatus = 2;
        MtkCatResponseMessage mtkCatResponseMessage = new MtkCatResponseMessage(MtkCatCmdMessage.getCmdMsg(), 10);
        for (int i = 1; i <= 7; i++) {
            if (true == this.mBipChannelManager.isChannelIdOccupied(i)) {
                try {
                    Channel channel2 = this.mBipChannelManager.getChannel(i);
                    MtkCatLog.d("[BIP]", "channel protocolType:" + channel2.mProtocolType);
                    if (1 == channel2.mProtocolType || 2 == channel2.mProtocolType) {
                        releaseRequest();
                        resetLocked();
                        if (isVzWSupport()) {
                            this.mBipChannelManager.updateChannelStatus(channel2.mChannelId, 0);
                            this.mBipChannelManager.updateChannelStatusInfo(channel2.mChannelId, 5);
                            sendDelayedCloseChannel(i);
                        } else {
                            channel2.closeChannel();
                            this.mBipChannelManager.removeChannel(i);
                        }
                        arrayList.add((byte) -72);
                        arrayList.add((byte) 2);
                        arrayList.add(Byte.valueOf((byte) (channel2.mChannelId | 0)));
                        arrayList.add((byte) 5);
                    }
                } catch (NullPointerException e) {
                    MtkCatLog.e("[BIP]", "NPE, channel null.");
                    e.printStackTrace();
                }
            }
        }
        if (arrayList.size() > 0) {
            byte[] bArr = new byte[arrayList.size()];
            for (int i2 = 0; i2 < bArr.length; i2++) {
                bArr[i2] = ((Byte) arrayList.get(i2)).byteValue();
            }
            mtkCatResponseMessage.setSourceId(DEV_ID_TERMINAL);
            mtkCatResponseMessage.setDestinationId(DEV_ID_UICC);
            mtkCatResponseMessage.setAdditionalInfo(bArr);
            mtkCatResponseMessage.setOneShot(false);
            mtkCatResponseMessage.setEventDownload(10, bArr);
            MtkCatLog.d("[BIP]", "onEventDownload: for channel status");
            ((MtkCatService) this.mHandler).onEventDownload(mtkCatResponseMessage);
            return;
        }
        MtkCatLog.d("[BIP]", "onEventDownload: No client channels are opened.");
    }

    public void acquireNetwork() {
        this.mIsOpenInProgress = true;
        if (this.mNetwork != null && (this.mApn == null || !this.mApn.equals("web99.test-nfc1.com"))) {
            MtkCatLog.d("[BIP]", "acquireNetwork: already available");
            if (this.mBipChannelManager.getChannel(this.mChannelId) == null) {
                connect();
                return;
            }
            return;
        }
        MtkCatLog.d("[BIP]", "requestNetwork: slotId " + this.mSlotId);
        if (!this.mIsOpenChannelOverWifi) {
            newRequest();
        } else {
            newRequestOverWifi();
        }
    }

    public void openChannel(BipCmdMessage bipCmdMessage, Message message) {
        boolean z;
        String simOperator;
        String simOperator2;
        MtkCatLog.d("[BIP]", "BM-openChannel: enter");
        if (!checkDataCapability(bipCmdMessage)) {
            bipCmdMessage.mChannelStatusData = new ChannelStatus(0, 0, 0);
            message.arg1 = 5;
            message.obj = bipCmdMessage;
            this.mCurrentCmd = bipCmdMessage;
            this.mBipSrvHandler.sendMessage(message);
            return;
        }
        this.isConnMgrIntentTimeout = false;
        this.mBipSrvHandler.removeMessages(21);
        this.mBipSrvHandler.removeMessages(22);
        MtkCatLog.d("[BIP]", "BM-openChannel: getCmdQualifier:" + bipCmdMessage.getCmdQualifier());
        this.mDNSaddrequest = 8 == (bipCmdMessage.getCmdQualifier() & 8);
        MtkCatLog.d("[BIP]", "BM-openChannel: mDNSaddrequest:" + this.mDNSaddrequest);
        MtkCatLog.d("[BIP]", "BM-openChannel: cmdMsg.mApn:" + bipCmdMessage.mApn);
        MtkCatLog.v("[BIP]", "BM-openChannel: cmdMsg.mLogin:" + bipCmdMessage.mLogin);
        MtkCatLog.v("[BIP]", "BM-openChannel: cmdMsg.mPwd:" + bipCmdMessage.mPwd);
        String simOperator3 = null;
        if (!this.mDNSaddrequest && bipCmdMessage.mTransportProtocol != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: mPreviousKeepChannelId:" + this.mPreviousKeepChannelId + " mChannelStatus:" + this.mChannelStatus + " mApn:" + this.mApn);
            if (this.mPreviousKeepChannelId != 0 && 4 == this.mChannelStatus) {
                if ((this.mApn == null && bipCmdMessage.mApn == null) || (this.mApn != null && bipCmdMessage.mApn != null && true == this.mApn.equals(bipCmdMessage.mApn))) {
                    if (bipCmdMessage.mTransportProtocol.protocolType == this.mPreviousProtocolType) {
                        this.mChannelId = this.mPreviousKeepChannelId;
                        bipCmdMessage.mChannelStatusData = new ChannelStatus(this.mChannelId, 128, 0);
                        this.mCurrentCmd = bipCmdMessage;
                        message.arg1 = 0;
                        message.obj = bipCmdMessage;
                        this.mBipSrvHandler.sendMessage(message);
                        this.mPreviousKeepChannelId = 0;
                        return;
                    }
                    MtkCatLog.d("[BIP]", "BM-openChannel: channel protocol type changed!");
                    Channel channel = this.mBipChannelManager.getChannel(this.mPreviousKeepChannelId);
                    if (channel != null) {
                        channel.closeChannel();
                    }
                    this.mBipChannelManager.removeChannel(this.mPreviousKeepChannelId);
                    this.mChannel = null;
                    this.mChannelStatus = 2;
                    this.mChannelId = this.mBipChannelManager.acquireChannelId(bipCmdMessage.mTransportProtocol.protocolType);
                    if (this.mChannelId == 0) {
                        MtkCatLog.d("[BIP]", "BM-openChannel: acquire channel id = 0");
                        message.arg1 = 5;
                        message.obj = bipCmdMessage;
                        this.mCurrentCmd = bipCmdMessage;
                        this.mBipSrvHandler.sendMessage(message);
                        return;
                    }
                    this.mApn = bipCmdMessage.mApn;
                    this.mLogin = bipCmdMessage.mLogin;
                    this.mPassword = bipCmdMessage.mPwd;
                } else {
                    this.mCurrentCmd = bipCmdMessage;
                    releaseRequest();
                    resetLocked();
                    Channel channel2 = this.mBipChannelManager.getChannel(this.mPreviousKeepChannelId);
                    if (channel2 != null) {
                        channel2.closeChannel();
                    }
                    this.mBipChannelManager.removeChannel(this.mPreviousKeepChannelId);
                    deleteApnParams();
                    setPdnReuse("1");
                    this.mChannel = null;
                    this.mChannelStatus = 2;
                    this.mApn = null;
                    this.mLogin = null;
                    this.mPassword = null;
                    if (this.mPreviousKeepChannelId != 0) {
                        sendBipDisconnectTimeOutMsg(bipCmdMessage);
                        return;
                    }
                    return;
                }
            } else {
                this.mChannelId = this.mBipChannelManager.acquireChannelId(bipCmdMessage.mTransportProtocol.protocolType);
                if (this.mChannelId == 0) {
                    MtkCatLog.d("[BIP]", "BM-openChannel: acquire channel id = 0");
                    message.arg1 = 5;
                    message.obj = bipCmdMessage;
                    this.mCurrentCmd = bipCmdMessage;
                    this.mBipSrvHandler.sendMessage(message);
                    MtkCatLog.d("[BIP]", "BM-openChannel: channel id = 0. mCurrentCmd = " + this.mCurrentCmd);
                    return;
                }
                this.mApn = bipCmdMessage.mApn;
                this.mLogin = bipCmdMessage.mLogin;
                this.mPassword = bipCmdMessage.mPwd;
            }
        }
        bipCmdMessage.mChannelStatusData = new ChannelStatus(this.mChannelId, 0, 0);
        this.mCurrentCmd = bipCmdMessage;
        MtkCatLog.d("[BIP]", "BM-openChannel: mCurrentCmd = " + this.mCurrentCmd);
        this.mBearerDesc = bipCmdMessage.mBearerDesc;
        if (bipCmdMessage.mBearerDesc != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: bearer type " + bipCmdMessage.mBearerDesc.bearerType);
        } else {
            MtkCatLog.d("[BIP]", "BM-openChannel: bearer type is null");
        }
        this.mBufferSize = bipCmdMessage.mBufferSize;
        MtkCatLog.d("[BIP]", "BM-openChannel: buffer size " + bipCmdMessage.mBufferSize);
        this.mLocalAddress = bipCmdMessage.mLocalAddress;
        if (bipCmdMessage.mLocalAddress != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: local address " + bipCmdMessage.mLocalAddress.address.toString());
        } else {
            MtkCatLog.d("[BIP]", "BM-openChannel: local address is null");
        }
        if (bipCmdMessage.mTransportProtocol != null) {
            this.mTransportProtocol = bipCmdMessage.mTransportProtocol;
            MtkCatLog.d("[BIP]", "BM-openChannel: transport protocol type/port " + bipCmdMessage.mTransportProtocol.protocolType + "/" + bipCmdMessage.mTransportProtocol.portNumber);
        }
        this.mDataDestinationAddress = bipCmdMessage.mDataDestinationAddress;
        if (bipCmdMessage.mDataDestinationAddress != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: dest address " + bipCmdMessage.mDataDestinationAddress.address.toString());
        } else {
            MtkCatLog.d("[BIP]", "BM-openChannel: dest address is null");
        }
        this.mLinkMode = (bipCmdMessage.getCmdQualifier() & 1) == 1 ? 0 : 1;
        MtkCatLog.d("[BIP]", "BM-openChannel: mLinkMode " + this.mLinkMode);
        this.mAutoReconnected = (bipCmdMessage.getCmdQualifier() & 2) == 2;
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        Phone phone = PhoneFactory.getPhone(this.mSlotId);
        if (this.mBearerDesc != null) {
            if (this.mBearerDesc.bearerType == 3) {
                setPdnReuse(MtkGsmCdmaPhone.ACT_TYPE_UTRAN);
                if (this.mApn != null && this.mApn.length() > 0) {
                    setPdnReuse("1");
                } else {
                    if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                        simOperator2 = TelephonyManager.getDefault().getSimOperator(subId[0]);
                    } else {
                        simOperator2 = null;
                    }
                    MtkCatLog.d("[BIP]", "numeric: " + simOperator2);
                    if (simOperator2 != null && simOperator2.equals("00101")) {
                        String str = SystemProperties.get(PROPERTY_IA_APN);
                        if (str == null || str.length() <= 0) {
                            MtkCatLog.d("[BIP]", "no persist ia APN, get temp ia");
                            str = SystemProperties.get(PROPERTY_PERSIST_IA_APN);
                        }
                        setPdnNameReuse(str);
                        MtkCatLog.d("[BIP]", "set ia APN to reuse");
                    } else {
                        setPdnNameReuse("");
                    }
                }
            } else {
                setPdnReuse("0");
                if (this.mApn != null && this.mApn.length() > 0) {
                    if (phone != null) {
                        int dataNetworkType = phone.getServiceState().getDataNetworkType();
                        MtkCatLog.d("[BIP]", "dataNetworkType: " + dataNetworkType);
                        if (13 == dataNetworkType) {
                            String str2 = SystemProperties.get(PROPERTY_IA_APN);
                            if (SubscriptionManager.isValidSubscriptionId(subId[0])) {
                                simOperator = TelephonyManager.getDefault().getSimOperator(subId[0]);
                            } else {
                                simOperator = null;
                            }
                            MtkCatLog.d("[BIP]", "numeric: " + simOperator);
                            if (simOperator != null && !simOperator.equals("00101") && str2 != null && str2.length() > 0 && str2.equals(this.mApn)) {
                                setPdnReuse(MtkGsmCdmaPhone.ACT_TYPE_UTRAN);
                            }
                        }
                    } else {
                        MtkCatLog.e("[BIP]", "myPhone is null");
                    }
                    MtkCatLog.d("[BIP]", "BM-openChannel: override apn: " + this.mApn);
                    setOverrideApn(this.mApn);
                }
            }
        } else if (this.mTransportProtocol != null && 3 != this.mTransportProtocol.protocolType && 4 != this.mTransportProtocol.protocolType && 5 != this.mTransportProtocol.protocolType) {
            MtkCatLog.e("[BIP]", "BM-openChannel: unsupported transport protocol type !!!");
            message.arg1 = 5;
            message.obj = this.mCurrentCmd;
            this.mBipSrvHandler.sendMessage(message);
            return;
        }
        this.mApnType = "bip";
        if (this.mApn != null && this.mApn.length() > 0) {
            if (this.mApn.equals("VZWADMIN") || this.mApn.equals("vzwadmin")) {
                this.mApnType = "fota";
            } else if (this.mApn.equals("VZWINTERNET") || this.mApn.equals("vzwinternet")) {
                this.mApnType = "internet";
            } else if (this.mApn.equals("titi") || this.mApn.equals("web99.test-nfc1.com") || this.mApn.equals("otasn") || this.mApn.equals("OTASN")) {
                this.mApnType = "fota";
            } else {
                this.mApnType = "bip";
                setApnParams(this.mApn, this.mLogin, this.mPassword);
            }
        } else {
            if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
                simOperator3 = TelephonyManager.getDefault().getSimOperator(subId[0]);
            }
            MtkCatLog.d("[BIP]", "numeric: " + simOperator3);
            if (simOperator3 != null && simOperator3.equals("00101")) {
                String str3 = SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR);
                MtkCatLog.d("[BIP]", "Optr load: " + str3);
                if ((str3 != null && DataSubConstants.OPERATOR_OP01.equals(str3)) || true == isBipApnTypeSupport()) {
                    this.mApnType = "bip";
                } else {
                    this.mApnType = "default";
                }
            } else {
                this.mApnType = "default";
            }
            if (phone != null) {
                PersistableBundle configForSubId = ((CarrierConfigManager) phone.getContext().getSystemService("carrier_config")).getConfigForSubId(phone.getSubId());
                if (configForSubId != null) {
                    z = configForSubId.getBoolean("mtk_use_administrative_apn_bool");
                } else {
                    z = false;
                }
                if (z) {
                    MtkCatLog.v("[BIP]", "support KDDI feature");
                    int dataNetworkType2 = phone.getServiceState().getDataNetworkType();
                    MtkCatLog.d("[BIP]", "dataNetworkType: " + dataNetworkType2);
                    if (13 == dataNetworkType2) {
                        this.mApnType = "fota";
                    }
                }
            } else {
                MtkCatLog.e("[BIP]", "myPhone is null");
            }
        }
        MtkCatLog.d("[BIP]", "APN Type: " + this.mApnType);
        MtkCatLog.d("[BIP]", "MAXCHANNELID: 7");
        if (this.mTransportProtocol != null && 3 == this.mTransportProtocol.protocolType) {
            int iEstablishLink = establishLink();
            if (iEstablishLink == 0 || iEstablishLink == 3) {
                MtkCatLog.d("[BIP]", "BM-openChannel: channel is activated");
                bipCmdMessage.mChannelStatusData.mChannelStatus = this.mBipChannelManager.getChannel(this.mChannelId).mChannelStatusData.mChannelStatus;
            } else {
                MtkCatLog.d("[BIP]", "BM-openChannel: channel is un-activated");
                bipCmdMessage.mChannelStatusData.mChannelStatus = 0;
            }
            message.arg1 = iEstablishLink;
            message.obj = this.mCurrentCmd;
            this.mBipSrvHandler.sendMessage(message);
        } else if (true == this.mIsApnInserting) {
            MtkCatLog.d("[BIP]", "BM-openChannel: startUsingNetworkFeature delay trigger.");
            Message messageObtainMessage = this.mBipSrvHandler.obtainMessage(11);
            messageObtainMessage.obj = bipCmdMessage;
            this.mBipSrvHandler.sendMessageDelayed(messageObtainMessage, 5000L);
            this.mIsApnInserting = false;
        } else {
            acquireNetwork();
        }
        MtkCatLog.d("[BIP]", "BM-openChannel: exit");
    }

    public void openChannelOverWifi(BipCmdMessage bipCmdMessage, Message message) {
        MtkCatLog.d("[BIP]", "BM-openChannelOverWifi: enter");
        this.isConnMgrIntentTimeout = false;
        if (bipCmdMessage.mTransportProtocol == null) {
            MtkCatLog.e("[BIP]", "BM-openChannel: transport protocol is null");
            return;
        }
        this.mChannelId = this.mBipChannelManager.acquireChannelId(bipCmdMessage.mTransportProtocol.protocolType);
        if (this.mChannelId == 0) {
            MtkCatLog.d("[BIP]", "BM-openChannel: acquire channel id = 0");
            message.arg1 = 5;
            message.obj = bipCmdMessage;
            this.mCurrentCmd = bipCmdMessage;
            this.mBipSrvHandler.sendMessage(message);
            return;
        }
        bipCmdMessage.mChannelStatusData = new ChannelStatus(this.mChannelId, 0, 0);
        this.mCurrentCmd = bipCmdMessage;
        this.mBearerDesc = bipCmdMessage.mBearerDesc;
        if (bipCmdMessage.mBearerDesc != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: bearer type " + bipCmdMessage.mBearerDesc.bearerType);
        } else {
            MtkCatLog.d("[BIP]", "BM-openChannel: bearer type is null");
        }
        this.mBufferSize = bipCmdMessage.mBufferSize;
        MtkCatLog.d("[BIP]", "BM-openChannel: buffer size " + bipCmdMessage.mBufferSize);
        this.mLocalAddress = bipCmdMessage.mLocalAddress;
        if (bipCmdMessage.mLocalAddress != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: local address " + bipCmdMessage.mLocalAddress.address.toString());
        } else {
            MtkCatLog.d("[BIP]", "BM-openChannel: local address is null");
        }
        this.mTransportProtocol = bipCmdMessage.mTransportProtocol;
        if (bipCmdMessage.mTransportProtocol != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: transport protocol type/port " + bipCmdMessage.mTransportProtocol.protocolType + "/" + bipCmdMessage.mTransportProtocol.portNumber);
        } else {
            MtkCatLog.d("[BIP]", "BM-openChannel: transport protocol is null");
        }
        this.mDataDestinationAddress = bipCmdMessage.mDataDestinationAddress;
        if (bipCmdMessage.mDataDestinationAddress != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: dest address " + bipCmdMessage.mDataDestinationAddress.address.toString());
        } else {
            MtkCatLog.d("[BIP]", "BM-openChannel: dest address is null");
        }
        this.mApn = bipCmdMessage.mApn;
        if (bipCmdMessage.mApn != null) {
            MtkCatLog.d("[BIP]", "BM-openChannel: apn " + bipCmdMessage.mApn);
        } else {
            MtkCatLog.d("[BIP]", "BM-openChannel: apn is null.");
        }
        this.mLogin = bipCmdMessage.mLogin;
        MtkCatLog.d("[BIP]", "BM-openChannel: login " + bipCmdMessage.mLogin);
        this.mPassword = bipCmdMessage.mPwd;
        MtkCatLog.d("[BIP]", "BM-openChannel: password " + bipCmdMessage.mPwd);
        this.mLinkMode = (bipCmdMessage.getCmdQualifier() & 1) == 1 ? 0 : 1;
        MtkCatLog.d("[BIP]", "BM-openChannel: mLinkMode " + bipCmdMessage.getCmdQualifier());
        this.mAutoReconnected = (bipCmdMessage.getCmdQualifier() & 2) != 0;
        if (isSprintSupport() && isWifiConnected()) {
            this.mIsOpenChannelOverWifi = true;
        }
        MtkCatLog.d("[BIP]", "BM-openChannel: call startUsingNetworkFeature:" + this.mSlotId);
        MtkCatLog.d("[BIP]", "MAXCHANNELID :7");
        acquireNetwork();
        MtkCatLog.d("[BIP]", "BM-openChannelOverWifi: exit");
    }

    public void closeChannel(BipCmdMessage bipCmdMessage, Message message) {
        TcpServerChannel tcpServerChannel;
        MtkCatLog.d("[BIP]", "BM-closeChannel: enter");
        int i = bipCmdMessage.mCloseCid;
        message.arg1 = 0;
        this.mCurrentCmd = bipCmdMessage;
        if (i < 0 || 7 < i) {
            MtkCatLog.d("[BIP]", "BM-closeChannel: channel id:" + i + " is invalid !!!");
            message.arg1 = 7;
        } else {
            this.mPreviousKeepChannelId = 0;
            MtkCatLog.d("[BIP]", "BM-closeChannel: getBipChannelStatus:" + this.mBipChannelManager.getBipChannelStatus(i));
            try {
                if (this.mBipChannelManager.getBipChannelStatus(i) == 0) {
                    MtkCatLog.d("[BIP]", "BM-closeChannel: mDNSaddrequest:" + this.mDNSaddrequest);
                    if (true == this.mDNSaddrequest) {
                        message.arg1 = 0;
                    } else {
                        message.arg1 = 7;
                    }
                } else if (2 == this.mBipChannelManager.getBipChannelStatus(i)) {
                    message.arg1 = 8;
                } else {
                    Channel channel = this.mBipChannelManager.getChannel(i);
                    if (channel == null) {
                        MtkCatLog.d("[BIP]", "BM-closeChannel: channel has already been closed");
                        message.arg1 = 7;
                    } else {
                        MtkCatLog.d("[BIP]", "BM-closeChannel: mProtocolType:" + channel.mProtocolType + " getCmdQualifier:" + bipCmdMessage.getCmdQualifier());
                        if (3 == channel.mProtocolType) {
                            if (channel instanceof TcpServerChannel) {
                                tcpServerChannel = (TcpServerChannel) channel;
                                tcpServerChannel.setCloseBackToTcpListen(bipCmdMessage.mCloseBackToTcpListen);
                            } else {
                                tcpServerChannel = null;
                            }
                            message.arg1 = channel.closeChannel();
                        } else {
                            if (1 == (bipCmdMessage.getCmdQualifier() & 1)) {
                                this.mPreviousKeepChannelId = i;
                                this.mPreviousProtocolType = channel.mProtocolType;
                                MtkCatLog.d("[BIP]", "BM-closeChannel: mPreviousKeepChannelId:" + this.mPreviousKeepChannelId + " mPreviousProtocolType:" + this.mPreviousProtocolType);
                                message.arg1 = 0;
                            } else {
                                MtkCatLog.d("[BIP]", "BM-closeChannel: stop data connection");
                                this.mIsCloseInProgress = true;
                                releaseRequest();
                                resetLocked();
                                if (!this.mIsOpenChannelOverWifi) {
                                    deleteOrRestoreApnParams();
                                    setPdnReuse("1");
                                } else {
                                    this.mIsOpenChannelOverWifi = false;
                                }
                                message.arg1 = channel.closeChannel();
                            }
                            tcpServerChannel = null;
                        }
                        if (3 == channel.mProtocolType) {
                            if (tcpServerChannel != null && !tcpServerChannel.isCloseBackToTcpListen()) {
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
                MtkCatLog.e("[BIP]", "BM-closeChannel: IndexOutOfBoundsException cid=" + i);
                message.arg1 = 7;
            }
        }
        if (!this.mIsCloseInProgress) {
            message.obj = bipCmdMessage;
            this.mBipSrvHandler.sendMessage(message);
        } else {
            sendBipDisconnectTimeOutMsg(bipCmdMessage);
        }
        MtkCatLog.d("[BIP]", "BM-closeChannel: exit");
    }

    public void receiveData(BipCmdMessage bipCmdMessage, Message message) {
        int i;
        int i2 = bipCmdMessage.mChannelDataLength;
        ReceiveDataResult receiveDataResult = new ReceiveDataResult();
        int i3 = bipCmdMessage.mReceiveDataCid;
        Channel channel = this.mBipChannelManager.getChannel(i3);
        MtkCatLog.d("[BIP]", "BM-receiveData: receiveData enter");
        if (channel == null) {
            MtkCatLog.e("[BIP]", "lChannel is null cid=" + i3);
            message.arg1 = 5;
            message.obj = bipCmdMessage;
            this.mBipSrvHandler.sendMessage(message);
            return;
        }
        if (channel.mChannelStatus == 4 || channel.mChannelStatus == 3) {
            if (i2 > 237) {
                MtkCatLog.d("[BIP]", "BM-receiveData: Modify channel data length to MAX_APDU_SIZE");
                i = 237;
            } else {
                i = i2;
            }
            new Thread(new RecvDataRunnable(i, receiveDataResult, bipCmdMessage, message)).start();
            return;
        }
        MtkCatLog.d("[BIP]", "BM-receiveData: Channel status is invalid " + this.mChannelStatus);
        message.arg1 = 5;
        message.obj = bipCmdMessage;
        this.mBipSrvHandler.sendMessage(message);
    }

    public void sendData(BipCmdMessage bipCmdMessage, Message message) {
        MtkCatLog.d("[BIP]", "sendData: Enter");
        new Thread(new SendDataThread(bipCmdMessage, message)).start();
        MtkCatLog.d("[BIP]", "sendData: Leave");
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
                MtkCatLog.d("[BIP]", "NetworkCallbackListener.onAvailable, mChannelId: " + BipService.this.mChannelId + " , mIsOpenInProgress: " + BipService.this.mIsOpenInProgress + " , mIsNetworkAvailableReceived: " + BipService.this.mIsNetworkAvailableReceived);
                if (true != BipService.this.mDNSaddrequest || true != BipService.this.mIsOpenInProgress) {
                    if (true == BipService.this.mIsOpenInProgress && !BipService.this.mIsNetworkAvailableReceived) {
                        BipService.this.mIsNetworkAvailableReceived = true;
                        BipService.this.mNetwork = network;
                        BipService.this.connect();
                        return;
                    }
                    MtkCatLog.d("[BIP]", "Bip channel has been established.");
                    return;
                }
                BipService.this.queryDnsServerAddress(network);
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                if (!BipService.this.isConnMgrIntentTimeout) {
                    BipService.this.mBipSrvHandler.removeMessages(10);
                }
                BipService.this.mBipSrvHandler.removeMessages(21);
                BipService.this.mPreviousKeepChannelId = 0;
                MtkCatLog.d("[BIP]", "onLost: network:" + network + " mNetworkCallback:" + BipService.this.mNetworkCallback + " this:" + this);
                BipService.this.releaseRequest();
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
                MtkCatLog.d("[BIP]", "onUnavailable: mNetworkCallback:" + BipService.this.mNetworkCallback + " this:" + this);
                BipService.this.releaseRequest();
                BipService.this.resetLocked();
                BipService.this.disconnect();
            }
        };
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        int i = 12;
        if ((this.mApnType == null || !this.mApnType.equals("default")) && (this.mApnType == null || !this.mApnType.equals("internet"))) {
            if (this.mApnType != null && this.mApnType.equals("fota")) {
                i = 3;
            } else if (this.mApnType != null && this.mApnType.equals("supl")) {
                i = 1;
            } else {
                i = 27;
            }
        }
        if (subId != null && SubscriptionManager.from(this.mContext).isActiveSubId(subId[0])) {
            this.mNetworkRequest = new NetworkRequest.Builder().addTransportType(0).addCapability(i).setNetworkSpecifier(String.valueOf(subId[0])).build();
        } else {
            this.mNetworkRequest = new NetworkRequest.Builder().addTransportType(0).addCapability(i).build();
        }
        MtkCatLog.d("[BIP]", "Start request network timer.");
        sendBipConnTimeOutMsg(this.mCurrentCmd);
        MtkCatLog.d("[BIP]", "requestNetwork: mNetworkRequest:" + this.mNetworkRequest + " mNetworkCallback:" + this.mNetworkCallback);
        connectivityManager.requestNetwork(this.mNetworkRequest, this.mNetworkCallback, CONN_MGR_TIMEOUT);
    }

    private void newRequestOverWifi() {
        MtkCatLog.d("[BIP]", "Open channel over wifi");
        ConnectivityManager connectivityManager = getConnectivityManager();
        this.mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                if (!BipService.this.isConnMgrIntentTimeout) {
                    BipService.this.mBipSrvHandler.removeMessages(10);
                }
                Channel channel = BipService.this.mBipChannelManager.getChannel(BipService.this.mChannelId);
                MtkCatLog.d("[BIP]", "NetworkCallbackListener.onAvailable, mChannelId: " + BipService.this.mChannelId + " , mIsOpenInProgress: " + BipService.this.mIsOpenInProgress + " , mIsNetworkAvailableReceived: " + BipService.this.mIsNetworkAvailableReceived);
                if (channel == null) {
                    MtkCatLog.d("[BIP]", "Channel is null.");
                }
                if (true == BipService.this.mIsOpenInProgress && !BipService.this.mIsNetworkAvailableReceived) {
                    BipService.this.mIsNetworkAvailableReceived = true;
                    BipService.this.mNetwork = network;
                    BipService.this.connect();
                    return;
                }
                MtkCatLog.d("[BIP]", "Bip channel has been established.");
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                if (!BipService.this.isConnMgrIntentTimeout) {
                    BipService.this.mBipSrvHandler.removeMessages(10);
                }
                MtkCatLog.d("[BIP]", "NetworkCallbackListener.onLost: network=" + network);
                BipService.this.releaseRequest();
                BipService.this.resetLocked();
                BipService.this.disconnect();
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                if (!BipService.this.isConnMgrIntentTimeout) {
                    BipService.this.mBipSrvHandler.removeMessages(10);
                }
                MtkCatLog.d("[BIP]", "NetworkCallbackListener.onUnavailable");
                BipService.this.releaseRequest();
                BipService.this.resetLocked();
                BipService.this.disconnect();
            }
        };
        this.mNetworkRequest = new NetworkRequest.Builder().addTransportType(1).addCapability(12).build();
        connectivityManager.requestNetwork(this.mNetworkRequest, this.mNetworkCallback, CONN_MGR_TIMEOUT);
        MtkCatLog.d("[BIP]", "Start request network timer.");
        sendBipConnTimeOutMsg(this.mCurrentCmd);
    }

    private void resetLocked() {
        this.mNetwork = null;
    }

    private void releaseRequest() {
        if (this.mNetworkCallback != null) {
            synchronized (this.mReleaseNetworkLock) {
                if (this.mNetworkCallback != null) {
                    MtkCatLog.d("[BIP]", "releaseRequest");
                    getConnectivityManager().unregisterNetworkCallback(this.mNetworkCallback);
                    this.mNetworkCallback = null;
                } else {
                    MtkCatLog.d("[BIP]", "releaseRequest: networkCallback is null.");
                }
            }
        }
    }

    private void queryDnsServerAddress(Network network) {
        LinkProperties linkProperties = getConnectivityManager().getLinkProperties(network);
        if (linkProperties == null) {
            MtkCatLog.e("[BIP]", "curLinkProps is null !!!");
            sendOpenChannelDoneMsg(5);
            return;
        }
        List<InetAddress> dnsServers = linkProperties.getDnsServers();
        if (dnsServers == null || dnsServers.size() == 0) {
            MtkCatLog.e("[BIP]", "LinkProps has null dnsAddres !!!");
            sendOpenChannelDoneMsg(5);
            return;
        }
        if (this.mCurrentCmd != null && AppInterface.CommandType.OPEN_CHANNEL.value() == this.mCurrentCmd.mCmdDet.typeOfCommand) {
            this.mCurrentCmd.mDnsServerAddress = new DnsServerAddress();
            this.mCurrentCmd.mDnsServerAddress.dnsAddresses.clear();
            for (InetAddress inetAddress : dnsServers) {
                if (inetAddress != null) {
                    MtkCatLog.d("[BIP]", "DNS Server Address:" + inetAddress);
                    this.mCurrentCmd.mDnsServerAddress.dnsAddresses.add(inetAddress);
                }
            }
            this.mIsOpenInProgress = false;
            sendOpenChannelDoneMsg(0);
        }
    }

    private void sendOpenChannelDoneMsg(int i) {
        this.mBipSrvHandler.sendMessage(this.mBipSrvHandler.obtainMessage(13, i, 0, this.mCurrentCmd));
    }

    protected class SendDataThread implements Runnable {
        BipCmdMessage cmdMsg;
        Message response;

        SendDataThread(BipCmdMessage bipCmdMessage, Message message) {
            MtkCatLog.d("[BIP]", "SendDataThread Init");
            this.cmdMsg = bipCmdMessage;
            this.response = message;
        }

        @Override
        public void run() {
            MtkCatLog.d("[BIP]", "SendDataThread Run Enter");
            byte[] bArr = this.cmdMsg.mChannelData;
            int i = this.cmdMsg.mSendMode;
            Channel channel = BipService.this.mBipChannelManager.getChannel(this.cmdMsg.mSendDataCid);
            int iSendData = 7;
            if (channel == null) {
                MtkCatLog.d("[BIP]", "SendDataThread Run mChannelId != cmdMsg.mSendDataCid");
            } else if (channel.mChannelStatus == 4) {
                MtkCatLog.d("[BIP]", "SendDataThread Run mChannel.sendData");
                iSendData = channel.sendData(bArr, i);
                this.response.arg2 = channel.getTxAvailBufferSize();
            } else {
                MtkCatLog.d("[BIP]", "SendDataThread Run CHANNEL_ID_NOT_VALID");
            }
            this.response.arg1 = iSendData;
            this.response.obj = this.cmdMsg;
            MtkCatLog.d("[BIP]", "SendDataThread Run mBipSrvHandler.sendMessage(response);");
            BipService.this.mBipSrvHandler.sendMessage(this.response);
        }
    }

    public void getChannelStatus(BipCmdMessage bipCmdMessage, Message message) {
        ArrayList arrayList = new ArrayList();
        int i = 1;
        while (true) {
            try {
                BipChannelManager bipChannelManager = this.mBipChannelManager;
                if (i > 7) {
                    break;
                }
                if (true == this.mBipChannelManager.isChannelIdOccupied(i)) {
                    MtkCatLog.d("[BIP]", "getChannelStatus: cId:" + i);
                    arrayList.add(this.mBipChannelManager.getChannel(i).mChannelStatusData);
                }
                i++;
            } catch (NullPointerException e) {
                MtkCatLog.e("[BIP]", "getChannelStatus: NE");
                e.printStackTrace();
            }
        }
        bipCmdMessage.mChannelStatusList = arrayList;
        message.arg1 = 0;
        message.obj = bipCmdMessage;
        this.mBipSrvHandler.sendMessage(message);
    }

    private void sendBipConnTimeOutMsg(BipCmdMessage bipCmdMessage) {
        Message messageObtainMessage = this.mBipSrvHandler.obtainMessage(10);
        messageObtainMessage.obj = bipCmdMessage;
        this.mBipSrvHandler.sendMessageDelayed(messageObtainMessage, 50000L);
    }

    private void sendBipDisconnectTimeOutMsg(BipCmdMessage bipCmdMessage) {
        Message messageObtainMessage = this.mBipSrvHandler.obtainMessage(12);
        messageObtainMessage.obj = bipCmdMessage;
        this.mBipSrvHandler.sendMessageDelayed(messageObtainMessage, 5000L);
    }

    private void sendBipChannelKeepTimeOutMsg(BipCmdMessage bipCmdMessage) {
        Message messageObtainMessage = this.mBipSrvHandler.obtainMessage(21);
        messageObtainMessage.obj = bipCmdMessage;
        this.mBipSrvHandler.sendMessageDelayed(messageObtainMessage, 30000L);
    }

    private void updateCurrentChannelStatus(int i) {
        try {
            this.mBipChannelManager.updateChannelStatus(this.mChannelId, i);
            this.mCurrentCmd.mChannelStatusData.mChannelStatus = i;
        } catch (NullPointerException e) {
            MtkCatLog.e("[BIP]", "updateCurrentChannelStatus id:" + this.mChannelId + " is null");
            e.printStackTrace();
        }
    }

    private boolean requestRouteToHost() {
        MtkCatLog.d("[BIP]", "requestRouteToHost");
        if (this.mDataDestinationAddress != null) {
            byte[] address = this.mDataDestinationAddress.address.getAddress();
            return this.mConnMgr.requestRouteToHost(27, (address[0] & PplMessageManager.Type.INVALID) | ((address[3] & PplMessageManager.Type.INVALID) << 24) | ((address[2] & PplMessageManager.Type.INVALID) << 16) | ((address[1] & PplMessageManager.Type.INVALID) << 8));
        }
        MtkCatLog.d("[BIP]", "mDataDestinationAddress is null");
        return false;
    }

    private boolean checkNetworkInfo(NetworkInfo networkInfo, NetworkInfo.State state) {
        if (networkInfo == null) {
            return false;
        }
        int type = networkInfo.getType();
        NetworkInfo.State state2 = networkInfo.getState();
        StringBuilder sb = new StringBuilder();
        sb.append("network type is ");
        sb.append(type == 0 ? "MOBILE" : "WIFI");
        MtkCatLog.d("[BIP]", sb.toString());
        MtkCatLog.d("[BIP]", "network state is " + state2);
        if (type != 0 || state2 != state) {
            return false;
        }
        return true;
    }

    private int establishLink() {
        int iOpenChannel;
        if (this.mTransportProtocol == null) {
            MtkCatLog.e("[BIP]", "BM-establishLink: mTransportProtocol is null !!!");
            return 5;
        }
        if (this.mTransportProtocol.protocolType == 3) {
            MtkCatLog.d("[BIP]", "BM-establishLink: establish a TCPServer link");
            try {
                TcpServerChannel tcpServerChannel = new TcpServerChannel(this.mChannelId, this.mLinkMode, this.mTransportProtocol.protocolType, this.mTransportProtocol.portNumber, this.mBufferSize, (MtkCatService) this.mHandler, this);
                iOpenChannel = tcpServerChannel.openChannel(this.mCurrentCmd, this.mNetwork);
                if (iOpenChannel != 0 && iOpenChannel != 3) {
                    this.mBipChannelManager.releaseChannelId(this.mChannelId, 3);
                    this.mChannelStatus = 7;
                } else {
                    this.mChannelStatus = 4;
                    this.mBipChannelManager.addChannel(this.mChannelId, tcpServerChannel);
                }
            } catch (NullPointerException e) {
                MtkCatLog.e("[BIP]", "BM-establishLink: NE,new TCP server channel fail.");
                e.printStackTrace();
                return 5;
            }
        } else if (this.mTransportProtocol.protocolType == 2) {
            MtkCatLog.d("[BIP]", "BM-establishLink: establish a TCP link");
            try {
                TcpChannel tcpChannel = new TcpChannel(this.mChannelId, this.mLinkMode, this.mTransportProtocol.protocolType, this.mDataDestinationAddress.address, this.mTransportProtocol.portNumber, this.mBufferSize, (MtkCatService) this.mHandler, this);
                iOpenChannel = tcpChannel.openChannel(this.mCurrentCmd, this.mNetwork);
                if (iOpenChannel != 10) {
                    if (iOpenChannel != 0 && iOpenChannel != 3) {
                        this.mBipChannelManager.releaseChannelId(this.mChannelId, 2);
                        this.mChannelStatus = 7;
                    } else {
                        this.mChannelStatus = 4;
                        this.mBipChannelManager.addChannel(this.mChannelId, tcpChannel);
                    }
                }
            } catch (NullPointerException e2) {
                MtkCatLog.e("[BIP]", "BM-establishLink: NE,new TCP client channel fail.");
                e2.printStackTrace();
                return this.mDataDestinationAddress == null ? 9 : 5;
            }
        } else if (this.mTransportProtocol.protocolType == 1) {
            MtkCatLog.d("[BIP]", "BM-establishLink: establish a UDP link");
            try {
                UdpChannel udpChannel = new UdpChannel(this.mChannelId, this.mLinkMode, this.mTransportProtocol.protocolType, this.mDataDestinationAddress.address, this.mTransportProtocol.portNumber, this.mBufferSize, (MtkCatService) this.mHandler, this);
                iOpenChannel = udpChannel.openChannel(this.mCurrentCmd, this.mNetwork);
                if (iOpenChannel != 0 && iOpenChannel != 3) {
                    this.mBipChannelManager.releaseChannelId(this.mChannelId, 1);
                    this.mChannelStatus = 7;
                } else {
                    this.mChannelStatus = 4;
                    this.mBipChannelManager.addChannel(this.mChannelId, udpChannel);
                }
            } catch (NullPointerException e3) {
                MtkCatLog.e("[BIP]", "BM-establishLink: NE,new UDP client channel fail.");
                e3.printStackTrace();
                return 5;
            }
        } else {
            MtkCatLog.d("[BIP]", "BM-establishLink: unsupported channel type");
            this.mChannelStatus = 7;
            iOpenChannel = 4;
        }
        MtkCatLog.d("[BIP]", "BM-establishLink: ret:" + iOpenChannel);
        return iOpenChannel;
    }

    private Uri getUri(Uri uri, int i) {
        int[] subId = SubscriptionManager.getSubId(i);
        if (subId == null) {
            MtkCatLog.d("[BIP]", "BM-getUri: null subId.");
            return null;
        }
        if (SubscriptionManager.isValidSubscriptionId(subId[0])) {
            return Uri.withAppendedPath(uri, "/subId/" + subId[0]);
        }
        MtkCatLog.d("[BIP]", "BM-getUri: invalid subId.");
        return null;
    }

    private void newThreadToDelelteApn() {
        new Thread() {
            @Override
            public void run() {
                BipService.this.deleteApnParams();
            }
        }.start();
    }

    private void deleteApnParams() {
        MtkCatLog.d("[BIP]", "BM-deleteApnParams: enter. ");
        MtkCatLog.d("[BIP]", "BM-deleteApnParams:[" + this.mContext.getContentResolver().delete(Telephony.Carriers.CONTENT_URI, "name = '__M-BIP__'", null) + "] end");
    }

    private void setApnParams(String str, String str2, String str3) {
        String simOperator;
        boolean dataEnabled;
        MtkCatLog.d("[BIP]", "BM-setApnParams: enter");
        if (str == null) {
            MtkCatLog.d("[BIP]", "BM-setApnParams: No apn parameters");
            return;
        }
        String str4 = this.mApnType;
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
            simOperator = TelephonyManager.getDefault().getSimOperator(subId[0]);
        } else {
            MtkCatLog.e("[BIP]", "BM-setApnParams: Invalid subId !!!");
            simOperator = null;
        }
        if (simOperator != null && simOperator.length() >= 4) {
            String strSubstring = simOperator.substring(0, 3);
            String strSubstring2 = simOperator.substring(3);
            this.mNumeric = strSubstring + strSubstring2;
            String str5 = "apn = '" + str + "' COLLATE NOCASE and numeric = '" + strSubstring + strSubstring2 + "'";
            MtkCatLog.d("[BIP]", "BM-setApnParams: selection = " + str5);
            Cursor cursorQuery = this.mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, null, str5, null, null);
            if (cursorQuery != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("name", BIP_NAME);
                contentValues.put("apn", str);
                if (str2 != null) {
                    contentValues.put(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER, str2);
                }
                if (str3 != null) {
                    contentValues.put("password", str3);
                }
                contentValues.put(PplMessageManager.PendingMessage.KEY_TYPE, str4);
                contentValues.put("mcc", strSubstring);
                contentValues.put("mnc", strSubstring2);
                contentValues.put("numeric", strSubstring + strSubstring2);
                contentValues.put("sub_id", Integer.valueOf(subId[0]));
                if (str.equals("web99.test-nfc1.com")) {
                    contentValues.put("protocol", "IP");
                } else if (this.mDataDestinationAddress != null) {
                    if (this.mDataDestinationAddress.addressType == 87) {
                        contentValues.put("protocol", "IPV6");
                    } else {
                        contentValues.put("protocol", "IP");
                    }
                }
                int count = cursorQuery.getCount();
                if (count != 0) {
                    if (count >= 1) {
                        MtkCatLog.d("[BIP]", "BM-setApnParams: count  = " + count);
                        if (cursorQuery.moveToFirst()) {
                            while (true) {
                                if (count <= 0) {
                                    break;
                                }
                                this.mApnTypeDb = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(PplMessageManager.PendingMessage.KEY_TYPE));
                                if (this.mApnTypeDb.contains("default")) {
                                    MtkCatLog.d("[BIP]", "BM-setApnParams: find default apn type");
                                    break;
                                }
                                count--;
                                if (!cursorQuery.isLast()) {
                                    cursorQuery.moveToNext();
                                } else {
                                    cursorQuery.moveToFirst();
                                }
                            }
                            this.mUri = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, Integer.parseInt(cursorQuery.getString(cursorQuery.getColumnIndex("_id"))));
                            this.mApnTypeDb = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(PplMessageManager.PendingMessage.KEY_TYPE));
                            this.mLoginDb = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER));
                            this.mPasswordDb = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("password"));
                            TelephonyManager telephonyManager = TelephonyManager.getDefault();
                            if (telephonyManager != null) {
                                dataEnabled = telephonyManager.getDataEnabled(subId[0]);
                                MtkCatLog.d("[BIP]", "BM-setApnParams: dataEnabled = " + dataEnabled);
                            } else {
                                dataEnabled = true;
                            }
                            ContentValues contentValues2 = new ContentValues();
                            MtkCatLog.d("[BIP]", "BM-setApnParams: apn old value : " + this.mApnTypeDb);
                            if (this.mApnTypeDb != null && this.mApnTypeDb.contains("default") && dataEnabled) {
                                this.mApnType = "default";
                            } else if (this.mApnTypeDb != null && this.mApnTypeDb.contains("supl") && dataEnabled) {
                                this.mApnType = "supl";
                            } else if (this.mApnTypeDb != null && this.mApnTypeDb.contains("fota")) {
                                this.mApnType = "fota";
                            } else {
                                this.mApnType = "bip";
                            }
                            if (this.mApn.equals("orange") || this.mApn.equals("Orange")) {
                                if (this.mApnTypeDb != null && this.mApnTypeDb.contains("supl")) {
                                    this.mApnType = "supl";
                                } else {
                                    this.mApnType = "bip";
                                }
                            }
                            MtkCatLog.d("[BIP]", "BM-setApnParams: mApnType :" + this.mApnType);
                            if (this.mApnTypeDb != null && !this.mApnTypeDb.contains(this.mApnType)) {
                                String str6 = this.mApnTypeDb + "," + this.mApnType;
                                MtkCatLog.d("[BIP]", "BM-setApnParams: will update apn to :" + str6);
                                contentValues2.put(PplMessageManager.PendingMessage.KEY_TYPE, str6);
                            }
                            MtkCatLog.v("[BIP]", "BM-restoreApnParams: mLogin: " + this.mLogin + "mLoginDb:" + this.mLoginDb + "mPassword" + this.mPassword + "mPasswordDb" + this.mPasswordDb);
                            if ((this.mLogin != null && !this.mLogin.equals(this.mLoginDb)) || (this.mPassword != null && !this.mPassword.equals(this.mPasswordDb))) {
                                MtkCatLog.d("[BIP]", "BM-setApnParams: will update login and password");
                                contentValues2.put(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER, this.mLogin);
                                contentValues2.put("password", this.mPassword);
                            }
                            if (this.mUri != null && contentValues2.size() > 0) {
                                MtkCatLog.d("[BIP]", "BM-setApnParams: will update apn db");
                                this.mContext.getContentResolver().update(this.mUri, contentValues2, null, null);
                                this.mIsApnInserting = true;
                                this.mIsUpdateApnParams = true;
                            } else {
                                MtkCatLog.d("[BIP]", "No need update APN db");
                            }
                        }
                    } else {
                        MtkCatLog.d("[BIP]", "BM-setApnParams: do not update one record");
                    }
                } else {
                    MtkCatLog.d("[BIP]", "BM-setApnParams: insert one record");
                    if (this.mContext.getContentResolver().insert(Telephony.Carriers.CONTENT_URI, contentValues) != null) {
                        MtkCatLog.d("[BIP]", "BM-setApnParams: insert a new record into db");
                        this.mIsApnInserting = true;
                    } else {
                        MtkCatLog.d("[BIP]", "BM-setApnParams: Fail to insert new record into db");
                    }
                }
                cursorQuery.close();
            }
        }
        MtkCatLog.d("[BIP]", "BM-setApnParams: exit");
    }

    private void restoreApnParams() {
        Cursor cursorQuery;
        if (this.mUri != null) {
            cursorQuery = this.mContext.getContentResolver().query(this.mUri, null, null, null, null);
        } else {
            MtkCatLog.w("[BIP]", "restoreApnParams mUri is null!!!!");
            cursorQuery = null;
        }
        if (cursorQuery != null) {
            if (cursorQuery.moveToFirst()) {
                String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(PplMessageManager.PendingMessage.KEY_TYPE));
                MtkCatLog.d("[BIP]", "BM-restoreApnParams: apnTypeDb before = " + string);
                ContentValues contentValues = new ContentValues();
                if (string != null && !string.equals(this.mApnTypeDb) && string.contains(this.mApnType)) {
                    String strReplaceAll = string.replaceAll("," + this.mApnType, "");
                    MtkCatLog.d("[BIP]", "BM-restoreApnParams: apnTypeDb after = " + strReplaceAll);
                    contentValues.put(PplMessageManager.PendingMessage.KEY_TYPE, strReplaceAll);
                }
                MtkCatLog.v("[BIP]", "BM-restoreApnParams: mLogin: " + this.mLogin + "mLoginDb:" + this.mLoginDb + "mPassword" + this.mPassword + "mPasswordDb" + this.mPasswordDb);
                if ((this.mLogin != null && !this.mLogin.equals(this.mLoginDb)) || (this.mPassword != null && !this.mPassword.equals(this.mPasswordDb))) {
                    contentValues.put(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER, this.mLoginDb);
                    contentValues.put("password", this.mPasswordDb);
                }
                if (this.mUri != null && contentValues.size() > 0) {
                    this.mContext.getContentResolver().update(this.mUri, contentValues, null, null);
                    this.mUri = null;
                    this.mIsUpdateApnParams = false;
                }
            }
            cursorQuery.close();
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
            return subId[0];
        }
        MtkCatLog.d("[BIP]", "getCurrentSubId: invalid subId");
        return -1;
    }

    private boolean isCurrentConnectionInService(int i) {
        if (!SubscriptionManager.isValidPhoneId(i)) {
            MtkCatLog.d("[BIP]", "isCurrentConnectionInService(): invalid phone id");
            return false;
        }
        Phone phone = PhoneFactory.getPhone(i);
        if (phone == null) {
            MtkCatLog.d("[BIP]", "isCurrentConnectionInService(): phone null");
            return false;
        }
        ServiceStateTracker serviceStateTracker = phone.getServiceStateTracker();
        if (serviceStateTracker == null) {
            MtkCatLog.d("[BIP]", "isCurrentConnectionInService(): sst null");
            return false;
        }
        if (serviceStateTracker.getCurrentDataConnectionState() == 0) {
            MtkCatLog.d("[BIP]", "isCurrentConnectionInService(): in service");
            return true;
        }
        MtkCatLog.d("[BIP]", "isCurrentConnectionInService(): not in service");
        return false;
    }

    private boolean checkDataCapability(BipCmdMessage bipCmdMessage) {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        int i = 0;
        for (int i2 = 0; i2 < mSimCount; i2++) {
            if (telephonyManager.hasIccCard(i2)) {
                i++;
            }
        }
        int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
        int[] subId = SubscriptionManager.getSubId(this.mSlotId);
        if (subId != null) {
            int i3 = subId[0];
            MtkCatLog.d("[BIP]", "checkDataCapability: simInsertedCount:" + i + " currentSubId:" + i3 + " defaultDataSubId:" + defaultDataSubscriptionId);
            if (i >= 2 && bipCmdMessage.mBearerDesc != null && ((2 == bipCmdMessage.mBearerDesc.bearerType || 3 == bipCmdMessage.mBearerDesc.bearerType || 9 == bipCmdMessage.mBearerDesc.bearerType || 11 == bipCmdMessage.mBearerDesc.bearerType) && i3 != defaultDataSubscriptionId)) {
                MtkCatLog.d("[BIP]", "checkDataCapability: return false");
                return false;
            }
            MtkCatLog.d("[BIP]", "checkDataCapability: return true");
            return true;
        }
        MtkCatLog.d("[BIP]", "checkDataCapability: invalid subId");
        return false;
    }

    protected boolean isWifiConnected() {
        NetworkInfo activeNetworkInfo = getConnectivityManager().getActiveNetworkInfo();
        if (activeNetworkInfo == null) {
            MtkCatLog.d("[BIP]", "activeInfo is null !!!");
            return false;
        }
        MtkCatLog.d("[BIP]", "activeInfo getType:" + activeNetworkInfo.getType() + " isConnected:" + activeNetworkInfo.isConnected());
        if (activeNetworkInfo.isConnected() && 1 == activeNetworkInfo.getType()) {
            MtkCatLog.d("[BIP]", "Wifi connected!");
            return true;
        }
        MtkCatLog.d("[BIP]", "Wifi disconnected!");
        return false;
    }

    public int getChannelId() {
        MtkCatLog.d("[BIP]", "BM-getChannelId: channel id is " + this.mChannelId);
        return this.mChannelId;
    }

    public int getFreeChannelId() {
        return this.mBipChannelManager.getFreeChannelId();
    }

    public void openChannelCompleted(int i, Channel channel) {
        MtkCatLog.d("[BIP]", "BM-openChannelCompleted: ret: " + i);
        if (i == 3) {
            this.mCurrentCmd.mBufferSize = this.mBufferSize;
        }
        if (i == 0 || i == 3) {
            this.mChannelStatus = 4;
            this.mBipChannelManager.addChannel(this.mChannelId, channel);
        } else {
            this.mBipChannelManager.releaseChannelId(this.mChannelId, 2);
            this.mChannelStatus = 7;
        }
        this.mCurrentCmd.mChannelStatusData = channel.mChannelStatusData;
        if (true == this.mIsOpenInProgress && !this.isConnMgrIntentTimeout) {
            this.mIsOpenInProgress = false;
            this.mIsNetworkAvailableReceived = false;
            Message messageObtainMessage = this.mBipSrvHandler.obtainMessage(13, i, 0, this.mCurrentCmd);
            messageObtainMessage.arg1 = i;
            messageObtainMessage.obj = this.mCurrentCmd;
            this.mBipSrvHandler.sendMessage(messageObtainMessage);
        }
    }

    public BipChannelManager getBipChannelManager() {
        return this.mBipChannelManager;
    }

    private boolean isSprintSupport() {
        if ("OP20".equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, ""))) {
            MtkCatLog.d("[BIP]", "isSprintSupport: true");
            return true;
        }
        return false;
    }

    private boolean isVzWSupport() {
        if ("OP12".equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, ""))) {
            return true;
        }
        return false;
    }

    private boolean isBipApnTypeSupport() {
        String str = "apn = 'TestGp.rs' COLLATE NOCASE and numeric = '00101'";
        MtkCatLog.d("[BIP]", "isBipApnTypeSupport: selection = " + str);
        Cursor cursorQuery = this.mContext.getContentResolver().query(Telephony.Carriers.CONTENT_URI, null, str, null, null);
        if (cursorQuery != null) {
            if (cursorQuery.getCount() == 0) {
                MtkCatLog.d("[BIP]", "There is no bip type apn for test sim");
                cursorQuery.close();
                return true;
            }
            MtkCatLog.d("[BIP]", "TestGp.rs count = " + cursorQuery.getCount());
            if (cursorQuery.moveToFirst()) {
                String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(PplMessageManager.PendingMessage.KEY_TYPE));
                MtkCatLog.d("[BIP]", "test apn type in db : " + string);
                if (string != null && string.contains("default")) {
                    cursorQuery.close();
                    return false;
                }
            }
            cursorQuery.close();
        }
        return true;
    }

    private void setPdnReuse(String str) {
        MtkCatLog.d("[BIP]", "setPdnReuse to  " + str);
        this.mMtkCmdIf.setPdnReuse(str, null);
    }

    private void setOverrideApn(String str) {
        this.mMtkCmdIf.setOverrideApn(str, null);
    }

    private void setPdnNameReuse(String str) {
        this.mMtkCmdIf.setPdnNameReuse(str, null);
    }

    protected class ConnectivityChangeThread implements Runnable {
        Intent intent;

        ConnectivityChangeThread(Intent intent) {
            MtkCatLog.d("[BIP]", "ConnectivityChangeThread Init");
            this.intent = intent;
        }

        @Override
        public void run() {
            int i;
            MtkCatLog.d("[BIP]", "ConnectivityChangeThread Enter");
            MtkCatLog.d("[BIP]", "Connectivity changed");
            NetworkInfo networkInfo = (NetworkInfo) this.intent.getExtra("networkInfo");
            String stringExtra = this.intent.getStringExtra(IPplSmsFilter.KEY_SUB_ID);
            if (stringExtra == null) {
                MtkCatLog.d("[BIP]", "No subId in intet extra.");
                return;
            }
            try {
                i = Integer.parseInt(stringExtra);
            } catch (NumberFormatException e) {
                MtkCatLog.e("[BIP]", "Invalid long string. strSubId: " + stringExtra);
                i = 0;
            }
            if (!SubscriptionManager.isValidSubscriptionId(i)) {
                MtkCatLog.e("[BIP]", "Invalid subId: " + i);
                return;
            }
            int slotIndex = SubscriptionManager.getSlotIndex(i);
            MtkCatLog.d("[BIP]", "EXTRA_SIM_ID :" + slotIndex + ",mSlotId:" + BipService.this.mSlotId);
            if (networkInfo == null || slotIndex != BipService.this.mSlotId) {
                MtkCatLog.d("[BIP]", "receive CONN intent sim!=" + BipService.this.mSlotId);
                return;
            }
            MtkCatLog.d("[BIP]", "receive valid CONN intent");
            int type = networkInfo.getType();
            NetworkInfo.State state = networkInfo.getState();
            MtkCatLog.d("[BIP]", "network type is " + type);
            MtkCatLog.d("[BIP]", "network state is " + state);
            if ((BipService.this.mApnType.equals("bip") && type == 27) || ((BipService.this.mApnType.equals("default") && type == 0) || ((BipService.this.mApnType.equals("internet") && type == 0) || (BipService.this.mApnType.equals("fota") && type == 10)))) {
                if (!BipService.this.isConnMgrIntentTimeout) {
                    BipService.this.mBipSrvHandler.removeMessages(10);
                }
                if (state == NetworkInfo.State.CONNECTED) {
                    MtkCatLog.d("[BIP]", "network state - connected.");
                    return;
                }
                if (state == NetworkInfo.State.DISCONNECTED) {
                    MtkCatLog.d("[BIP]", "network state - disconnected");
                    synchronized (BipService.this.mCloseLock) {
                        MtkCatLog.d("[BIP]", "mIsCloseInProgress: " + BipService.this.mIsCloseInProgress + " mPreviousKeepChannelId:" + BipService.this.mPreviousKeepChannelId);
                        if (true != BipService.this.mIsCloseInProgress) {
                            if (BipService.this.mPreviousKeepChannelId != 0) {
                                BipService.this.mPreviousKeepChannelId = 0;
                                BipService.this.mBipSrvHandler.removeMessages(12);
                                BipService.this.openChannel(BipService.this.mCurrentCmd, BipService.this.mBipSrvHandler.obtainMessage(13));
                            }
                        } else {
                            MtkCatLog.d("[BIP]", "Return TR for close channel.");
                            BipService.this.mBipSrvHandler.removeMessages(12);
                            BipService.this.mIsCloseInProgress = false;
                            BipService.this.mBipSrvHandler.sendMessage(BipService.this.mBipSrvHandler.obtainMessage(16, 0, 0, BipService.this.mCurrentCmd));
                        }
                    }
                }
            }
        }
    }

    public void setConnMgrTimeoutFlag(boolean z) {
        this.isConnMgrIntentTimeout = z;
    }

    public void setOpenInProgressFlag(boolean z) {
        this.mIsOpenInProgress = z;
    }

    private class RecvDataRunnable implements Runnable {
        BipCmdMessage cmdMsg;
        int requestDataSize;
        Message response;
        ReceiveDataResult result;

        public RecvDataRunnable(int i, ReceiveDataResult receiveDataResult, BipCmdMessage bipCmdMessage, Message message) {
            this.requestDataSize = i;
            this.result = receiveDataResult;
            this.cmdMsg = bipCmdMessage;
            this.response = message;
        }

        @Override
        public void run() {
            int iReceiveData;
            MtkCatLog.d("[BIP]", "BM-receiveData: start to receive data");
            Channel channel = BipService.this.mBipChannelManager.getChannel(this.cmdMsg.mReceiveDataCid);
            if (channel == null) {
                iReceiveData = 5;
            } else {
                synchronized (channel.mLock) {
                    channel.isReceiveDataTRSent = false;
                }
                iReceiveData = channel.receiveData(this.requestDataSize, this.result);
            }
            this.cmdMsg.mChannelData = this.result.buffer;
            this.cmdMsg.mRemainingDataLength = this.result.remainingCount;
            this.response.arg1 = iReceiveData;
            this.response.obj = this.cmdMsg;
            BipService.this.mBipSrvHandler.sendMessage(this.response);
            if (channel != null) {
                synchronized (channel.mLock) {
                    channel.isReceiveDataTRSent = true;
                    if (channel.mRxBufferCount == 0) {
                        MtkCatLog.d("[BIP]", "BM-receiveData: notify waiting channel!");
                        channel.mLock.notify();
                    }
                }
            } else {
                MtkCatLog.e("[BIP]", "BM-receiveData: null channel.");
            }
            MtkCatLog.d("[BIP]", "BM-receiveData: end to receive data. Result code = " + iReceiveData);
        }
    }
}
