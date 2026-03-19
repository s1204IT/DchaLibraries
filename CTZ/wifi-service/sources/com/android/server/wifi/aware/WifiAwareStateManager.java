package com.android.server.wifi.aware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.wifi.V1_2.NanDataPathChannelInfo;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.aware.Characteristics;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.IWifiAwareEventCallback;
import android.net.wifi.aware.IWifiAwareMacAddressProvider;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.wifi.aware.WifiAwareDiscoverySessionState;
import com.android.server.wifi.aware.WifiAwareShellCommand;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiAwareStateManager implements WifiAwareShellCommand.DelegatedShellCommand {
    private static final int COMMAND_TYPE_CONNECT = 100;
    private static final int COMMAND_TYPE_CREATE_ALL_DATA_PATH_INTERFACES = 112;
    private static final int COMMAND_TYPE_CREATE_DATA_PATH_INTERFACE = 114;
    private static final int COMMAND_TYPE_DELAYED_INITIALIZATION = 121;
    private static final int COMMAND_TYPE_DELETE_ALL_DATA_PATH_INTERFACES = 113;
    private static final int COMMAND_TYPE_DELETE_DATA_PATH_INTERFACE = 115;
    private static final int COMMAND_TYPE_DISABLE_USAGE = 109;
    private static final int COMMAND_TYPE_DISCONNECT = 101;
    private static final int COMMAND_TYPE_ENABLE_USAGE = 108;
    private static final int COMMAND_TYPE_END_DATA_PATH = 118;
    private static final int COMMAND_TYPE_ENQUEUE_SEND_MESSAGE = 107;
    private static final int COMMAND_TYPE_GET_AWARE = 122;
    private static final int COMMAND_TYPE_GET_CAPABILITIES = 111;
    private static final int COMMAND_TYPE_INITIATE_DATA_PATH_SETUP = 116;
    private static final int COMMAND_TYPE_PUBLISH = 103;
    private static final int COMMAND_TYPE_RECONFIGURE = 120;
    private static final int COMMAND_TYPE_RELEASE_AWARE = 123;
    private static final int COMMAND_TYPE_RESPOND_TO_DATA_PATH_SETUP_REQUEST = 117;
    private static final int COMMAND_TYPE_SUBSCRIBE = 105;
    private static final int COMMAND_TYPE_TERMINATE_SESSION = 102;
    private static final int COMMAND_TYPE_TRANSMIT_NEXT_MESSAGE = 119;
    private static final int COMMAND_TYPE_UPDATE_PUBLISH = 104;
    private static final int COMMAND_TYPE_UPDATE_SUBSCRIBE = 106;

    @VisibleForTesting
    public static final String HAL_COMMAND_TIMEOUT_TAG = "WifiAwareStateManager HAL Command Timeout";

    @VisibleForTesting
    public static final String HAL_DATA_PATH_CONFIRM_TIMEOUT_TAG = "WifiAwareStateManager HAL Data Path Confirm Timeout";

    @VisibleForTesting
    public static final String HAL_SEND_MESSAGE_TIMEOUT_TAG = "WifiAwareStateManager HAL Send Message Timeout";
    private static final String MESSAGE_BUNDLE_KEY_CALLING_PACKAGE = "calling_package";
    private static final String MESSAGE_BUNDLE_KEY_CHANNEL = "channel";
    private static final String MESSAGE_BUNDLE_KEY_CHANNEL_REQ_TYPE = "channel_request_type";
    private static final String MESSAGE_BUNDLE_KEY_CONFIG = "config";
    private static final String MESSAGE_BUNDLE_KEY_FILTER_DATA = "filter_data";
    private static final String MESSAGE_BUNDLE_KEY_INTERFACE_NAME = "interface_name";
    private static final String MESSAGE_BUNDLE_KEY_MAC_ADDRESS = "mac_address";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE = "message";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE_ARRIVAL_SEQ = "message_arrival_seq";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE_DATA = "message_data";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE_ID = "message_id";
    private static final String MESSAGE_BUNDLE_KEY_MESSAGE_PEER_ID = "message_peer_id";
    private static final String MESSAGE_BUNDLE_KEY_NDP_IDS = "ndp_ids";
    private static final String MESSAGE_BUNDLE_KEY_NOTIFY_IDENTITY_CHANGE = "notify_identity_chg";
    private static final String MESSAGE_BUNDLE_KEY_OOB = "out_of_band";
    private static final String MESSAGE_BUNDLE_KEY_PASSPHRASE = "passphrase";
    private static final String MESSAGE_BUNDLE_KEY_PEER_ID = "peer_id";
    private static final String MESSAGE_BUNDLE_KEY_PID = "pid";
    private static final String MESSAGE_BUNDLE_KEY_PMK = "pmk";
    private static final String MESSAGE_BUNDLE_KEY_REQ_INSTANCE_ID = "req_instance_id";
    private static final String MESSAGE_BUNDLE_KEY_RETRY_COUNT = "retry_count";
    private static final String MESSAGE_BUNDLE_KEY_SEND_MESSAGE_ENQUEUE_TIME = "message_queue_time";
    private static final String MESSAGE_BUNDLE_KEY_SENT_MESSAGE = "send_message";
    private static final String MESSAGE_BUNDLE_KEY_SESSION_ID = "session_id";
    private static final String MESSAGE_BUNDLE_KEY_SESSION_TYPE = "session_type";
    private static final String MESSAGE_BUNDLE_KEY_SSI_DATA = "ssi_data";
    private static final String MESSAGE_BUNDLE_KEY_STATUS_CODE = "status_code";
    private static final String MESSAGE_BUNDLE_KEY_SUCCESS_FLAG = "success_flag";
    private static final String MESSAGE_BUNDLE_KEY_UID = "uid";
    private static final String MESSAGE_RANGE_MM = "range_mm";
    private static final String MESSAGE_RANGING_INDICATION = "ranging_indication";
    private static final int MESSAGE_TYPE_COMMAND = 1;
    private static final int MESSAGE_TYPE_DATA_PATH_TIMEOUT = 6;
    private static final int MESSAGE_TYPE_NOTIFICATION = 3;
    private static final int MESSAGE_TYPE_RESPONSE = 2;
    private static final int MESSAGE_TYPE_RESPONSE_TIMEOUT = 4;
    private static final int MESSAGE_TYPE_SEND_MESSAGE_TIMEOUT = 5;
    private static final int NOTIFICATION_TYPE_AWARE_DOWN = 306;
    private static final int NOTIFICATION_TYPE_CLUSTER_CHANGE = 302;
    private static final int NOTIFICATION_TYPE_INTERFACE_CHANGE = 301;
    private static final int NOTIFICATION_TYPE_MATCH = 303;
    private static final int NOTIFICATION_TYPE_MESSAGE_RECEIVED = 305;
    private static final int NOTIFICATION_TYPE_ON_DATA_PATH_CONFIRM = 310;
    private static final int NOTIFICATION_TYPE_ON_DATA_PATH_END = 311;
    private static final int NOTIFICATION_TYPE_ON_DATA_PATH_REQUEST = 309;
    private static final int NOTIFICATION_TYPE_ON_DATA_PATH_SCHED_UPDATE = 312;
    private static final int NOTIFICATION_TYPE_ON_MESSAGE_SEND_FAIL = 308;
    private static final int NOTIFICATION_TYPE_ON_MESSAGE_SEND_SUCCESS = 307;
    private static final int NOTIFICATION_TYPE_SESSION_TERMINATED = 304;
    public static final String PARAM_ON_IDLE_DISABLE_AWARE = "on_idle_disable_aware";
    public static final int PARAM_ON_IDLE_DISABLE_AWARE_DEFAULT = 1;
    private static final int RESPONSE_TYPE_ON_CAPABILITIES_UPDATED = 206;
    private static final int RESPONSE_TYPE_ON_CONFIG_FAIL = 201;
    private static final int RESPONSE_TYPE_ON_CONFIG_SUCCESS = 200;
    private static final int RESPONSE_TYPE_ON_CREATE_INTERFACE = 207;
    private static final int RESPONSE_TYPE_ON_DELETE_INTERFACE = 208;
    private static final int RESPONSE_TYPE_ON_DISABLE = 213;
    private static final int RESPONSE_TYPE_ON_END_DATA_PATH = 212;
    private static final int RESPONSE_TYPE_ON_INITIATE_DATA_PATH_FAIL = 210;
    private static final int RESPONSE_TYPE_ON_INITIATE_DATA_PATH_SUCCESS = 209;
    private static final int RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_FAIL = 205;
    private static final int RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_SUCCESS = 204;
    private static final int RESPONSE_TYPE_ON_RESPOND_TO_DATA_PATH_SETUP_REQUEST = 211;
    private static final int RESPONSE_TYPE_ON_SESSION_CONFIG_FAIL = 203;
    private static final int RESPONSE_TYPE_ON_SESSION_CONFIG_SUCCESS = 202;
    private static final String TAG = "WifiAwareStateManager";
    private static final boolean VDBG = false;
    private static final boolean VVDBG = false;
    private WifiAwareMetrics mAwareMetrics;
    private volatile Capabilities mCapabilities;
    private Context mContext;
    public WifiAwareDataPathStateManager mDataPathMgr;
    private LocationManager mLocationManager;
    private PowerManager mPowerManager;
    private WifiAwareStateMachine mSm;
    private WifiAwareNativeApi mWifiAwareNativeApi;
    private WifiAwareNativeManager mWifiAwareNativeManager;
    private WifiManager mWifiManager;
    private static final SparseArray<String> sSmToString = MessageUtils.findMessageNames(new Class[]{WifiAwareStateManager.class}, new String[]{"MESSAGE_TYPE", "COMMAND_TYPE", "RESPONSE_TYPE", "NOTIFICATION_TYPE"});
    private static final byte[] ALL_ZERO_MAC = {0, 0, 0, 0, 0, 0};
    boolean mDbg = false;
    private volatile boolean mUsageEnabled = false;
    private volatile Characteristics mCharacteristics = null;
    private final SparseArray<WifiAwareClientState> mClients = new SparseArray<>();
    private ConfigRequest mCurrentAwareConfiguration = null;
    private boolean mCurrentIdentityNotification = false;
    private byte[] mCurrentDiscoveryInterfaceMac = ALL_ZERO_MAC;
    private Map<String, Integer> mSettableParameters = new HashMap();

    public WifiAwareStateManager() {
        onReset();
    }

    public void setNative(WifiAwareNativeManager wifiAwareNativeManager, WifiAwareNativeApi wifiAwareNativeApi) {
        this.mWifiAwareNativeManager = wifiAwareNativeManager;
        this.mWifiAwareNativeApi = wifiAwareNativeApi;
    }

    @Override
    public int onCommand(ShellCommand shellCommand) {
        byte b;
        PrintWriter errPrintWriter = shellCommand.getErrPrintWriter();
        PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
        String nextArgRequired = shellCommand.getNextArgRequired();
        int iHashCode = nextArgRequired.hashCode();
        if (iHashCode != -1212873217) {
            if (iHashCode != 102230) {
                if (iHashCode != 113762) {
                    b = (iHashCode == 1060304561 && nextArgRequired.equals("allow_ndp_any")) ? (byte) 3 : (byte) -1;
                } else if (nextArgRequired.equals("set")) {
                    b = 0;
                }
            } else if (nextArgRequired.equals("get")) {
                b = 1;
            }
        } else if (nextArgRequired.equals("get_capabilities")) {
            b = 2;
        }
        switch (b) {
            case 0:
                String nextArgRequired2 = shellCommand.getNextArgRequired();
                if (!this.mSettableParameters.containsKey(nextArgRequired2)) {
                    errPrintWriter.println("Unknown parameter name -- '" + nextArgRequired2 + "'");
                    return -1;
                }
                String nextArgRequired3 = shellCommand.getNextArgRequired();
                try {
                    this.mSettableParameters.put(nextArgRequired2, Integer.valueOf(Integer.valueOf(nextArgRequired3).intValue()));
                    return 0;
                } catch (NumberFormatException e) {
                    errPrintWriter.println("Can't convert value to integer -- '" + nextArgRequired3 + "'");
                    return -1;
                }
            case 1:
                String nextArgRequired4 = shellCommand.getNextArgRequired();
                if (!this.mSettableParameters.containsKey(nextArgRequired4)) {
                    errPrintWriter.println("Unknown parameter name -- '" + nextArgRequired4 + "'");
                    return -1;
                }
                outPrintWriter.println(this.mSettableParameters.get(nextArgRequired4).intValue());
                return 0;
            case 2:
                JSONObject jSONObject = new JSONObject();
                if (this.mCapabilities != null) {
                    try {
                        jSONObject.put("maxConcurrentAwareClusters", this.mCapabilities.maxConcurrentAwareClusters);
                        jSONObject.put("maxPublishes", this.mCapabilities.maxPublishes);
                        jSONObject.put("maxSubscribes", this.mCapabilities.maxSubscribes);
                        jSONObject.put("maxServiceNameLen", this.mCapabilities.maxServiceNameLen);
                        jSONObject.put("maxMatchFilterLen", this.mCapabilities.maxMatchFilterLen);
                        jSONObject.put("maxTotalMatchFilterLen", this.mCapabilities.maxTotalMatchFilterLen);
                        jSONObject.put("maxServiceSpecificInfoLen", this.mCapabilities.maxServiceSpecificInfoLen);
                        jSONObject.put("maxExtendedServiceSpecificInfoLen", this.mCapabilities.maxExtendedServiceSpecificInfoLen);
                        jSONObject.put("maxNdiInterfaces", this.mCapabilities.maxNdiInterfaces);
                        jSONObject.put("maxNdpSessions", this.mCapabilities.maxNdpSessions);
                        jSONObject.put("maxAppInfoLen", this.mCapabilities.maxAppInfoLen);
                        jSONObject.put("maxQueuedTransmitMessages", this.mCapabilities.maxQueuedTransmitMessages);
                        jSONObject.put("maxSubscribeInterfaceAddresses", this.mCapabilities.maxSubscribeInterfaceAddresses);
                        jSONObject.put("supportedCipherSuites", this.mCapabilities.supportedCipherSuites);
                    } catch (JSONException e2) {
                        Log.e(TAG, "onCommand: get_capabilities e=" + e2);
                    }
                    break;
                }
                outPrintWriter.println(jSONObject.toString());
                return 0;
            case 3:
                String nextArgRequired5 = shellCommand.getNextArgRequired();
                if (this.mDataPathMgr == null) {
                    errPrintWriter.println("Null Aware data-path manager - can't configure");
                    return -1;
                }
                if (TextUtils.equals("true", nextArgRequired5)) {
                    this.mDataPathMgr.mAllowNdpResponderFromAnyOverride = true;
                } else {
                    if (!TextUtils.equals("false", nextArgRequired5)) {
                        errPrintWriter.println("Unknown configuration flag for 'allow_ndp_any' - true|false expected -- '" + nextArgRequired5 + "'");
                        return -1;
                    }
                    this.mDataPathMgr.mAllowNdpResponderFromAnyOverride = false;
                }
                break;
                break;
        }
        errPrintWriter.println("Unknown 'wifiaware state_mgr <cmd>'");
        return -1;
    }

    @Override
    public void onReset() {
        this.mSettableParameters.put(PARAM_ON_IDLE_DISABLE_AWARE, 1);
        if (this.mDataPathMgr != null) {
            this.mDataPathMgr.mAllowNdpResponderFromAnyOverride = false;
        }
    }

    @Override
    public void onHelp(String str, ShellCommand shellCommand) {
        PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
        outPrintWriter.println("  " + str);
        outPrintWriter.println("    set <name> <value>: sets named parameter to value. Names: " + this.mSettableParameters.keySet());
        outPrintWriter.println("    get <name>: gets named parameter value. Names: " + this.mSettableParameters.keySet());
        outPrintWriter.println("    get_capabilities: prints out the capabilities as a JSON string");
        outPrintWriter.println("    allow_ndp_any true|false: configure whether Responders can be specified to accept requests from ANY requestor (null peer spec)");
    }

    public void start(Context context, Looper looper, WifiAwareMetrics wifiAwareMetrics, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper wifiPermissionsWrapper) {
        Log.i(TAG, "start()");
        this.mContext = context;
        this.mAwareMetrics = wifiAwareMetrics;
        this.mSm = new WifiAwareStateMachine(TAG, looper);
        this.mSm.setDbg(false);
        this.mSm.start();
        this.mDataPathMgr = new WifiAwareDataPathStateManager(this);
        this.mDataPathMgr.start(this.mContext, this.mSm.getHandler().getLooper(), wifiAwareMetrics, wifiPermissionsUtil, wifiPermissionsWrapper);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService(PowerManager.class);
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON") || action.equals("android.intent.action.SCREEN_OFF")) {
                    WifiAwareStateManager.this.reconfigure();
                }
                if (action.equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                    if (((Integer) WifiAwareStateManager.this.mSettableParameters.get(WifiAwareStateManager.PARAM_ON_IDLE_DISABLE_AWARE)).intValue() != 0) {
                        if (WifiAwareStateManager.this.mPowerManager.isDeviceIdleMode()) {
                            WifiAwareStateManager.this.disableUsage();
                            return;
                        } else {
                            WifiAwareStateManager.this.enableUsage();
                            return;
                        }
                    }
                    WifiAwareStateManager.this.reconfigure();
                }
            }
        }, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.location.MODE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (WifiAwareStateManager.this.mDbg) {
                    Log.v(WifiAwareStateManager.TAG, "onReceive: MODE_CHANGED_ACTION: intent=" + intent);
                }
                if (WifiAwareStateManager.this.mLocationManager.isLocationEnabled()) {
                    WifiAwareStateManager.this.enableUsage();
                } else {
                    WifiAwareStateManager.this.disableUsage();
                }
            }
        }, intentFilter2);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent.getIntExtra("wifi_state", 4) == 3) {
                    WifiAwareStateManager.this.enableUsage();
                } else {
                    WifiAwareStateManager.this.disableUsage();
                }
            }
        }, intentFilter3);
    }

    public void startLate() {
        delayedInitialization();
    }

    WifiAwareClientState getClient(int i) {
        return this.mClients.get(i);
    }

    public Capabilities getCapabilities() {
        return this.mCapabilities;
    }

    public Characteristics getCharacteristics() {
        if (this.mCharacteristics == null && this.mCapabilities != null) {
            this.mCharacteristics = this.mCapabilities.toPublicCharacteristics();
        }
        return this.mCharacteristics;
    }

    public void requestMacAddresses(final int i, final List<Integer> list, final IWifiAwareMacAddressProvider iWifiAwareMacAddressProvider) {
        this.mSm.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                WifiAwareStateManager.lambda$requestMacAddresses$0(this.f$0, i, list, iWifiAwareMacAddressProvider);
            }
        });
    }

    public static void lambda$requestMacAddresses$0(WifiAwareStateManager wifiAwareStateManager, int i, List list, IWifiAwareMacAddressProvider iWifiAwareMacAddressProvider) {
        HashMap map = new HashMap();
        for (int i2 = 0; i2 < wifiAwareStateManager.mClients.size(); i2++) {
            WifiAwareClientState wifiAwareClientStateValueAt = wifiAwareStateManager.mClients.valueAt(i2);
            if (wifiAwareClientStateValueAt.getUid() == i) {
                SparseArray<WifiAwareDiscoverySessionState> sessions = wifiAwareClientStateValueAt.getSessions();
                for (int i3 = 0; i3 < sessions.size(); i3++) {
                    WifiAwareDiscoverySessionState wifiAwareDiscoverySessionStateValueAt = sessions.valueAt(i3);
                    Iterator it = list.iterator();
                    while (it.hasNext()) {
                        int iIntValue = ((Integer) it.next()).intValue();
                        WifiAwareDiscoverySessionState.PeerInfo peerInfo = wifiAwareDiscoverySessionStateValueAt.getPeerInfo(iIntValue);
                        if (peerInfo != null) {
                            map.put(Integer.valueOf(iIntValue), peerInfo.mMac);
                        }
                    }
                }
            }
        }
        try {
            iWifiAwareMacAddressProvider.macAddress(map);
        } catch (RemoteException e) {
            Log.e(TAG, "requestMacAddress (sync): exception on callback -- " + e);
        }
    }

    public void delayedInitialization() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_DELAYED_INITIALIZATION;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void getAwareInterface() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_GET_AWARE;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void releaseAwareInterface() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_RELEASE_AWARE;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void connect(int i, int i2, int i3, String str, IWifiAwareEventCallback iWifiAwareEventCallback, ConfigRequest configRequest, boolean z) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 100;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = iWifiAwareEventCallback;
        messageObtainMessage.getData().putParcelable(MESSAGE_BUNDLE_KEY_CONFIG, configRequest);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_UID, i2);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_PID, i3);
        messageObtainMessage.getData().putString(MESSAGE_BUNDLE_KEY_CALLING_PACKAGE, str);
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_NOTIFY_IDENTITY_CHANGE, z);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void disconnect(int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 101;
        messageObtainMessage.arg2 = i;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void reconfigure() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_RECONFIGURE;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void terminateSession(int i, int i2) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 102;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = Integer.valueOf(i2);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void publish(int i, PublishConfig publishConfig, IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 103;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = iWifiAwareDiscoverySessionCallback;
        messageObtainMessage.getData().putParcelable(MESSAGE_BUNDLE_KEY_CONFIG, publishConfig);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void updatePublish(int i, int i2, PublishConfig publishConfig) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 104;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = publishConfig;
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_SESSION_ID, i2);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void subscribe(int i, SubscribeConfig subscribeConfig, IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 105;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = iWifiAwareDiscoverySessionCallback;
        messageObtainMessage.getData().putParcelable(MESSAGE_BUNDLE_KEY_CONFIG, subscribeConfig);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void updateSubscribe(int i, int i2, SubscribeConfig subscribeConfig) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 106;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = subscribeConfig;
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_SESSION_ID, i2);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void sendMessage(int i, int i2, int i3, byte[] bArr, int i4, int i5) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 107;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_SESSION_ID, i2);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_MESSAGE_PEER_ID, i3);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MESSAGE, bArr);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_MESSAGE_ID, i4);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_RETRY_COUNT, i5);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void enableUsage() {
        if (this.mSettableParameters.get(PARAM_ON_IDLE_DISABLE_AWARE).intValue() != 0 && this.mPowerManager.isDeviceIdleMode()) {
            if (this.mDbg) {
                Log.d(TAG, "enableUsage(): while device is in IDLE mode - ignoring");
            }
        } else if (!this.mLocationManager.isLocationEnabled()) {
            if (this.mDbg) {
                Log.d(TAG, "enableUsage(): while location is disabled - ignoring");
            }
        } else if (this.mWifiManager.getWifiState() != 3) {
            if (this.mDbg) {
                Log.d(TAG, "enableUsage(): while Wi-Fi is disabled - ignoring");
            }
        } else {
            Message messageObtainMessage = this.mSm.obtainMessage(1);
            messageObtainMessage.arg1 = COMMAND_TYPE_ENABLE_USAGE;
            this.mSm.sendMessage(messageObtainMessage);
        }
    }

    public void disableUsage() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_DISABLE_USAGE;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public boolean isUsageEnabled() {
        return this.mUsageEnabled;
    }

    public void queryCapabilities() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_GET_CAPABILITIES;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void createAllDataPathInterfaces() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 112;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void deleteAllDataPathInterfaces() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = 113;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void createDataPathInterface(String str) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_CREATE_DATA_PATH_INTERFACE;
        messageObtainMessage.obj = str;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void deleteDataPathInterface(String str) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_DELETE_DATA_PATH_INTERFACE;
        messageObtainMessage.obj = str;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void initiateDataPathSetup(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier, int i, int i2, int i3, byte[] bArr, String str, byte[] bArr2, String str2, boolean z) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_INITIATE_DATA_PATH_SETUP;
        messageObtainMessage.obj = wifiAwareNetworkSpecifier;
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_PEER_ID, i);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_CHANNEL_REQ_TYPE, i2);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_CHANNEL, i3);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, bArr);
        messageObtainMessage.getData().putString(MESSAGE_BUNDLE_KEY_INTERFACE_NAME, str);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_PMK, bArr2);
        messageObtainMessage.getData().putString(MESSAGE_BUNDLE_KEY_PASSPHRASE, str2);
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_OOB, z);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void respondToDataPathRequest(boolean z, int i, String str, byte[] bArr, String str2, boolean z2) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_RESPOND_TO_DATA_PATH_SETUP_REQUEST;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = Boolean.valueOf(z);
        messageObtainMessage.getData().putString(MESSAGE_BUNDLE_KEY_INTERFACE_NAME, str);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_PMK, bArr);
        messageObtainMessage.getData().putString(MESSAGE_BUNDLE_KEY_PASSPHRASE, str2);
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_OOB, z2);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void endDataPath(int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_END_DATA_PATH;
        messageObtainMessage.arg2 = i;
        this.mSm.sendMessage(messageObtainMessage);
    }

    private void transmitNextMessage() {
        Message messageObtainMessage = this.mSm.obtainMessage(1);
        messageObtainMessage.arg1 = COMMAND_TYPE_TRANSMIT_NEXT_MESSAGE;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onConfigSuccessResponse(short s) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = 200;
        messageObtainMessage.arg2 = s;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onConfigFailedResponse(short s, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_CONFIG_FAIL;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = Integer.valueOf(i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onDisableResponse(short s, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_DISABLE;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = Integer.valueOf(i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onSessionConfigSuccessResponse(short s, boolean z, byte b) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_SESSION_CONFIG_SUCCESS;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = Byte.valueOf(b);
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_SESSION_TYPE, z);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onSessionConfigFailResponse(short s, boolean z, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_SESSION_CONFIG_FAIL;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = Integer.valueOf(i);
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_SESSION_TYPE, z);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onMessageSendQueuedSuccessResponse(short s) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_SUCCESS;
        messageObtainMessage.arg2 = s;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onMessageSendQueuedFailResponse(short s, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_FAIL;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = Integer.valueOf(i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onCapabilitiesUpdateResponse(short s, Capabilities capabilities) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_CAPABILITIES_UPDATED;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = capabilities;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onCreateDataPathInterfaceResponse(short s, boolean z, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_CREATE_INTERFACE;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, z);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onDeleteDataPathInterfaceResponse(short s, boolean z, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_DELETE_INTERFACE;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, z);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onInitiateDataPathResponseSuccess(short s, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_INITIATE_DATA_PATH_SUCCESS;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = Integer.valueOf(i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onInitiateDataPathResponseFail(short s, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_INITIATE_DATA_PATH_FAIL;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = Integer.valueOf(i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onRespondToDataPathSetupRequestResponse(short s, boolean z, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_RESPOND_TO_DATA_PATH_SETUP_REQUEST;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, z);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onEndDataPathResponse(short s, boolean z, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(2);
        messageObtainMessage.arg1 = RESPONSE_TYPE_ON_END_DATA_PATH;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, z);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onInterfaceAddressChangeNotification(byte[] bArr) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_INTERFACE_CHANGE;
        messageObtainMessage.obj = bArr;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onClusterChangeNotification(int i, byte[] bArr) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_CLUSTER_CHANGE;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = bArr;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onMatchNotification(int i, int i2, byte[] bArr, byte[] bArr2, byte[] bArr3, int i3, int i4) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_MATCH;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_REQ_INSTANCE_ID, i2);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, bArr);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_SSI_DATA, bArr2);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_FILTER_DATA, bArr3);
        messageObtainMessage.getData().putInt(MESSAGE_RANGING_INDICATION, i3);
        messageObtainMessage.getData().putInt(MESSAGE_RANGE_MM, i4);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onSessionTerminatedNotification(int i, int i2, boolean z) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_SESSION_TERMINATED;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = Integer.valueOf(i2);
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_SESSION_TYPE, z);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onMessageReceivedNotification(int i, int i2, byte[] bArr, byte[] bArr2) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_MESSAGE_RECEIVED;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = Integer.valueOf(i2);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, bArr);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MESSAGE_DATA, bArr2);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onAwareDownNotification(int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_AWARE_DOWN;
        messageObtainMessage.arg2 = i;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onMessageSendSuccessNotification(short s) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_ON_MESSAGE_SEND_SUCCESS;
        messageObtainMessage.arg2 = s;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onMessageSendFailNotification(short s, int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_ON_MESSAGE_SEND_FAIL;
        messageObtainMessage.arg2 = s;
        messageObtainMessage.obj = Integer.valueOf(i);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onDataPathRequestNotification(int i, byte[] bArr, int i2) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_ON_DATA_PATH_REQUEST;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.obj = Integer.valueOf(i2);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, bArr);
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onDataPathConfirmNotification(int i, byte[] bArr, boolean z, int i2, byte[] bArr2, List<NanDataPathChannelInfo> list) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_ON_DATA_PATH_CONFIRM;
        messageObtainMessage.arg2 = i;
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, bArr);
        messageObtainMessage.getData().putBoolean(MESSAGE_BUNDLE_KEY_SUCCESS_FLAG, z);
        messageObtainMessage.getData().putInt(MESSAGE_BUNDLE_KEY_STATUS_CODE, i2);
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MESSAGE_DATA, bArr2);
        messageObtainMessage.obj = list;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onDataPathEndNotification(int i) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_ON_DATA_PATH_END;
        messageObtainMessage.arg2 = i;
        this.mSm.sendMessage(messageObtainMessage);
    }

    public void onDataPathScheduleUpdateNotification(byte[] bArr, ArrayList<Integer> arrayList, List<NanDataPathChannelInfo> list) {
        Message messageObtainMessage = this.mSm.obtainMessage(3);
        messageObtainMessage.arg1 = NOTIFICATION_TYPE_ON_DATA_PATH_SCHED_UPDATE;
        messageObtainMessage.getData().putByteArray(MESSAGE_BUNDLE_KEY_MAC_ADDRESS, bArr);
        messageObtainMessage.getData().putIntegerArrayList(MESSAGE_BUNDLE_KEY_NDP_IDS, arrayList);
        messageObtainMessage.obj = list;
        this.mSm.sendMessage(messageObtainMessage);
    }

    @VisibleForTesting
    class WifiAwareStateMachine extends StateMachine {
        private static final long AWARE_SEND_MESSAGE_TIMEOUT = 10000;
        private static final long AWARE_WAIT_FOR_DP_CONFIRM_TIMEOUT = 20000;
        private static final int TRANSACTION_ID_IGNORE = 0;
        private Message mCurrentCommand;
        private short mCurrentTransactionId;
        private final Map<WifiAwareNetworkSpecifier, WakeupMessage> mDataPathConfirmTimeoutMessages;
        private DefaultState mDefaultState;
        private final Map<Short, Message> mFwQueuedSendMessages;
        private final SparseArray<Message> mHostQueuedSendMessages;
        public int mNextSessionId;
        private short mNextTransactionId;
        private int mSendArrivalSequenceCounter;
        private WakeupMessage mSendMessageTimeoutMessage;
        private boolean mSendQueueBlocked;
        private WaitForResponseState mWaitForResponseState;
        private WaitState mWaitState;

        WifiAwareStateMachine(String str, Looper looper) {
            super(str, looper);
            this.mDefaultState = new DefaultState();
            this.mWaitState = new WaitState();
            this.mWaitForResponseState = new WaitForResponseState();
            this.mNextTransactionId = (short) 1;
            this.mNextSessionId = 1;
            this.mCurrentTransactionId = (short) 0;
            this.mSendArrivalSequenceCounter = 0;
            this.mSendQueueBlocked = false;
            this.mHostQueuedSendMessages = new SparseArray<>();
            this.mFwQueuedSendMessages = new LinkedHashMap();
            this.mSendMessageTimeoutMessage = new WakeupMessage(WifiAwareStateManager.this.mContext, getHandler(), WifiAwareStateManager.HAL_SEND_MESSAGE_TIMEOUT_TAG, 5);
            this.mDataPathConfirmTimeoutMessages = new ArrayMap();
            addState(this.mDefaultState);
            addState(this.mWaitState, this.mDefaultState);
            addState(this.mWaitForResponseState, this.mDefaultState);
            setInitialState(this.mWaitState);
        }

        public void onAwareDownCleanupSendQueueState() {
            this.mSendQueueBlocked = false;
            this.mHostQueuedSendMessages.clear();
            this.mFwQueuedSendMessages.clear();
        }

        private class DefaultState extends State {
            private DefaultState() {
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != 3) {
                    switch (i) {
                        case 5:
                            WifiAwareStateMachine.this.processSendMessageTimeout();
                            return true;
                        case 6:
                            WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = (WifiAwareNetworkSpecifier) message.obj;
                            if (WifiAwareStateManager.this.mDbg) {
                                Log.v(WifiAwareStateManager.TAG, "MESSAGE_TYPE_DATA_PATH_TIMEOUT: networkSpecifier=" + wifiAwareNetworkSpecifier);
                            }
                            WifiAwareStateManager.this.mDataPathMgr.handleDataPathTimeout(wifiAwareNetworkSpecifier);
                            WifiAwareStateMachine.this.mDataPathConfirmTimeoutMessages.remove(wifiAwareNetworkSpecifier);
                            return true;
                        default:
                            Log.wtf(WifiAwareStateManager.TAG, "DefaultState: should not get non-NOTIFICATION in this state: msg=" + message);
                            return false;
                    }
                }
                WifiAwareStateMachine.this.processNotification(message);
                return true;
            }
        }

        private class WaitState extends State {
            private WaitState() {
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i != 4) {
                    switch (i) {
                        case 1:
                            if (WifiAwareStateMachine.this.processCommand(message)) {
                                WifiAwareStateMachine.this.transitionTo(WifiAwareStateMachine.this.mWaitForResponseState);
                            }
                            break;
                    }
                    return true;
                }
                WifiAwareStateMachine.this.deferMessage(message);
                return true;
            }
        }

        private class WaitForResponseState extends State {
            private static final long AWARE_COMMAND_TIMEOUT = 5000;
            private WakeupMessage mTimeoutMessage;

            private WaitForResponseState() {
            }

            public void enter() {
                this.mTimeoutMessage = new WakeupMessage(WifiAwareStateManager.this.mContext, WifiAwareStateMachine.this.getHandler(), WifiAwareStateManager.HAL_COMMAND_TIMEOUT_TAG, 4, WifiAwareStateMachine.this.mCurrentCommand.arg1, WifiAwareStateMachine.this.mCurrentTransactionId);
                this.mTimeoutMessage.schedule(SystemClock.elapsedRealtime() + AWARE_COMMAND_TIMEOUT);
            }

            public void exit() {
                this.mTimeoutMessage.cancel();
            }

            public boolean processMessage(Message message) {
                int i = message.what;
                if (i == 4) {
                    if (message.arg2 == WifiAwareStateMachine.this.mCurrentTransactionId) {
                        WifiAwareStateMachine.this.processTimeout(message);
                        WifiAwareStateMachine.this.transitionTo(WifiAwareStateMachine.this.mWaitState);
                    } else {
                        Log.w(WifiAwareStateManager.TAG, "WaitForResponseState: processMessage: non-matching transaction ID on RESPONSE_TIMEOUT (either a non-cancelled timeout or a race condition with cancel) -- msg=" + message);
                    }
                    return true;
                }
                switch (i) {
                    case 1:
                        WifiAwareStateMachine.this.deferMessage(message);
                        break;
                    case 2:
                        if (message.arg2 == WifiAwareStateMachine.this.mCurrentTransactionId) {
                            WifiAwareStateMachine.this.processResponse(message);
                            WifiAwareStateMachine.this.transitionTo(WifiAwareStateMachine.this.mWaitState);
                        } else {
                            Log.w(WifiAwareStateManager.TAG, "WaitForResponseState: processMessage: non-matching transaction ID on RESPONSE (a very late response) -- msg=" + message);
                        }
                        break;
                }
                return true;
            }
        }

        private void processNotification(Message message) {
            WakeupMessage wakeupMessageRemove;
            switch (message.arg1) {
                case WifiAwareStateManager.NOTIFICATION_TYPE_INTERFACE_CHANGE:
                    WifiAwareStateManager.this.onInterfaceAddressChangeLocal((byte[]) message.obj);
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_CLUSTER_CHANGE:
                    WifiAwareStateManager.this.onClusterChangeLocal(message.arg2, (byte[]) message.obj);
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_MATCH:
                    WifiAwareStateManager.this.onMatchLocal(message.arg2, message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_REQ_INSTANCE_ID), message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SSI_DATA), message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_FILTER_DATA), message.getData().getInt(WifiAwareStateManager.MESSAGE_RANGING_INDICATION), message.getData().getInt(WifiAwareStateManager.MESSAGE_RANGE_MM));
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_SESSION_TERMINATED:
                    WifiAwareStateManager.this.onSessionTerminatedLocal(message.arg2, message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_TYPE), ((Integer) message.obj).intValue());
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_MESSAGE_RECEIVED:
                    WifiAwareStateManager.this.onMessageReceivedLocal(message.arg2, ((Integer) message.obj).intValue(), message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_DATA));
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_AWARE_DOWN:
                    int i = message.arg2;
                    WifiAwareStateManager.this.onAwareDownLocal();
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_MESSAGE_SEND_SUCCESS:
                    short s = (short) message.arg2;
                    Message message2 = this.mFwQueuedSendMessages.get(Short.valueOf(s));
                    if (message2 == null) {
                        Log.w(WifiAwareStateManager.TAG, "processNotification: NOTIFICATION_TYPE_ON_MESSAGE_SEND_SUCCESS: transactionId=" + ((int) s) + " - no such queued send command (timed-out?)");
                    } else {
                        this.mFwQueuedSendMessages.remove(Short.valueOf(s));
                        updateSendMessageTimeout();
                        WifiAwareStateManager.this.onMessageSendSuccessLocal(message2);
                    }
                    this.mSendQueueBlocked = false;
                    WifiAwareStateManager.this.transmitNextMessage();
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_MESSAGE_SEND_FAIL:
                    short s2 = (short) message.arg2;
                    int iIntValue = ((Integer) message.obj).intValue();
                    Message message3 = this.mFwQueuedSendMessages.get(Short.valueOf(s2));
                    if (message3 == null) {
                        Log.w(WifiAwareStateManager.TAG, "processNotification: NOTIFICATION_TYPE_ON_MESSAGE_SEND_FAIL: transactionId=" + ((int) s2) + " - no such queued send command (timed-out?)");
                    } else {
                        this.mFwQueuedSendMessages.remove(Short.valueOf(s2));
                        updateSendMessageTimeout();
                        int i2 = message3.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_RETRY_COUNT);
                        if (i2 <= 0 || iIntValue != 9) {
                            WifiAwareStateManager.this.onMessageSendFailLocal(message3, iIntValue);
                        } else {
                            message3.getData().putInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_RETRY_COUNT, i2 - 1);
                            this.mHostQueuedSendMessages.put(message3.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_ARRIVAL_SEQ), message3);
                        }
                        this.mSendQueueBlocked = false;
                        WifiAwareStateManager.this.transmitNextMessage();
                    }
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_DATA_PATH_REQUEST:
                    WifiAwareNetworkSpecifier wifiAwareNetworkSpecifierOnDataPathRequest = WifiAwareStateManager.this.mDataPathMgr.onDataPathRequest(message.arg2, message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), ((Integer) message.obj).intValue());
                    if (wifiAwareNetworkSpecifierOnDataPathRequest != null) {
                        WakeupMessage wakeupMessage = new WakeupMessage(WifiAwareStateManager.this.mContext, getHandler(), WifiAwareStateManager.HAL_DATA_PATH_CONFIRM_TIMEOUT_TAG, 6, 0, 0, wifiAwareNetworkSpecifierOnDataPathRequest);
                        this.mDataPathConfirmTimeoutMessages.put(wifiAwareNetworkSpecifierOnDataPathRequest, wakeupMessage);
                        wakeupMessage.schedule(SystemClock.elapsedRealtime() + AWARE_WAIT_FOR_DP_CONFIRM_TIMEOUT);
                    }
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_DATA_PATH_CONFIRM:
                    WifiAwareNetworkSpecifier wifiAwareNetworkSpecifierOnDataPathConfirm = WifiAwareStateManager.this.mDataPathMgr.onDataPathConfirm(message.arg2, message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE), message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_DATA), (List) message.obj);
                    if (wifiAwareNetworkSpecifierOnDataPathConfirm != null && (wakeupMessageRemove = this.mDataPathConfirmTimeoutMessages.remove(wifiAwareNetworkSpecifierOnDataPathConfirm)) != null) {
                        wakeupMessageRemove.cancel();
                        break;
                    }
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_DATA_PATH_END:
                    WifiAwareStateManager.this.mDataPathMgr.onDataPathEnd(message.arg2);
                    break;
                case WifiAwareStateManager.NOTIFICATION_TYPE_ON_DATA_PATH_SCHED_UPDATE:
                    WifiAwareStateManager.this.mDataPathMgr.onDataPathSchedUpdate(message.getData().getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), message.getData().getIntegerArrayList(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_NDP_IDS), (List) message.obj);
                    break;
                default:
                    Log.wtf(WifiAwareStateManager.TAG, "processNotification: this isn't a NOTIFICATION -- msg=" + message);
                    break;
            }
        }

        private boolean processCommand(Message message) {
            boolean zConnectLocal;
            if (this.mCurrentCommand != null) {
                Log.wtf(WifiAwareStateManager.TAG, "processCommand: receiving a command (msg=" + message + ") but current (previous) command isn't null (prev_msg=" + this.mCurrentCommand + ")");
                this.mCurrentCommand = null;
            }
            short s = this.mNextTransactionId;
            this.mNextTransactionId = (short) (s + 1);
            this.mCurrentTransactionId = s;
            switch (message.arg1) {
                case 100:
                    zConnectLocal = WifiAwareStateManager.this.connectLocal(this.mCurrentTransactionId, message.arg2, message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_UID), message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PID), message.getData().getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CALLING_PACKAGE), (IWifiAwareEventCallback) message.obj, message.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CONFIG), message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_NOTIFY_IDENTITY_CHANGE));
                    break;
                case 101:
                    zConnectLocal = WifiAwareStateManager.this.disconnectLocal(this.mCurrentTransactionId, message.arg2);
                    break;
                case 102:
                    WifiAwareStateManager.this.terminateSessionLocal(message.arg2, ((Integer) message.obj).intValue());
                    break;
                case 103:
                    zConnectLocal = WifiAwareStateManager.this.publishLocal(this.mCurrentTransactionId, message.arg2, (PublishConfig) message.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CONFIG), (IWifiAwareDiscoverySessionCallback) message.obj);
                    break;
                case 104:
                    zConnectLocal = WifiAwareStateManager.this.updatePublishLocal(this.mCurrentTransactionId, message.arg2, message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_ID), (PublishConfig) message.obj);
                    break;
                case 105:
                    zConnectLocal = WifiAwareStateManager.this.subscribeLocal(this.mCurrentTransactionId, message.arg2, (SubscribeConfig) message.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CONFIG), (IWifiAwareDiscoverySessionCallback) message.obj);
                    break;
                case 106:
                    zConnectLocal = WifiAwareStateManager.this.updateSubscribeLocal(this.mCurrentTransactionId, message.arg2, message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_ID), (SubscribeConfig) message.obj);
                    break;
                case 107:
                    Message messageObtainMessage = obtainMessage(message.what);
                    messageObtainMessage.copyFrom(message);
                    messageObtainMessage.getData().putInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_ARRIVAL_SEQ, this.mSendArrivalSequenceCounter);
                    this.mHostQueuedSendMessages.put(this.mSendArrivalSequenceCounter, messageObtainMessage);
                    this.mSendArrivalSequenceCounter++;
                    if (!this.mSendQueueBlocked) {
                        WifiAwareStateManager.this.transmitNextMessage();
                    }
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_ENABLE_USAGE:
                    WifiAwareStateManager.this.enableUsageLocal();
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DISABLE_USAGE:
                    zConnectLocal = WifiAwareStateManager.this.disableUsageLocal(this.mCurrentTransactionId);
                    break;
                case 110:
                default:
                    Log.wtf(WifiAwareStateManager.TAG, "processCommand: this isn't a COMMAND -- msg=" + message);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_GET_CAPABILITIES:
                    zConnectLocal = WifiAwareStateManager.this.mCapabilities == null ? WifiAwareStateManager.this.mWifiAwareNativeApi.getCapabilities(this.mCurrentTransactionId) : false;
                    break;
                case 112:
                    WifiAwareStateManager.this.mDataPathMgr.createAllInterfaces();
                    break;
                case 113:
                    WifiAwareStateManager.this.mDataPathMgr.deleteAllInterfaces();
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_CREATE_DATA_PATH_INTERFACE:
                    zConnectLocal = WifiAwareStateManager.this.mWifiAwareNativeApi.createAwareNetworkInterface(this.mCurrentTransactionId, (String) message.obj);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DELETE_DATA_PATH_INTERFACE:
                    zConnectLocal = WifiAwareStateManager.this.mWifiAwareNativeApi.deleteAwareNetworkInterface(this.mCurrentTransactionId, (String) message.obj);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_INITIATE_DATA_PATH_SETUP:
                    Bundle data = message.getData();
                    WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = (WifiAwareNetworkSpecifier) message.obj;
                    zConnectLocal = WifiAwareStateManager.this.initiateDataPathSetupLocal(this.mCurrentTransactionId, wifiAwareNetworkSpecifier, data.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PEER_ID), data.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CHANNEL_REQ_TYPE), data.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_CHANNEL), data.getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MAC_ADDRESS), data.getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_INTERFACE_NAME), data.getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PMK), data.getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PASSPHRASE), data.getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_OOB));
                    if (zConnectLocal) {
                        WakeupMessage wakeupMessage = new WakeupMessage(WifiAwareStateManager.this.mContext, getHandler(), WifiAwareStateManager.HAL_DATA_PATH_CONFIRM_TIMEOUT_TAG, 6, 0, 0, wifiAwareNetworkSpecifier);
                        this.mDataPathConfirmTimeoutMessages.put(wifiAwareNetworkSpecifier, wakeupMessage);
                        wakeupMessage.schedule(SystemClock.elapsedRealtime() + AWARE_WAIT_FOR_DP_CONFIRM_TIMEOUT);
                    }
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RESPOND_TO_DATA_PATH_SETUP_REQUEST:
                    Bundle data2 = message.getData();
                    zConnectLocal = WifiAwareStateManager.this.respondToDataPathRequestLocal(this.mCurrentTransactionId, ((Boolean) message.obj).booleanValue(), message.arg2, data2.getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_INTERFACE_NAME), data2.getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PMK), data2.getString(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_PASSPHRASE), data2.getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_OOB));
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_END_DATA_PATH:
                    zConnectLocal = WifiAwareStateManager.this.endDataPathLocal(this.mCurrentTransactionId, message.arg2);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_TRANSMIT_NEXT_MESSAGE:
                    if (!this.mSendQueueBlocked && this.mHostQueuedSendMessages.size() != 0) {
                        Message messageValueAt = this.mHostQueuedSendMessages.valueAt(0);
                        this.mHostQueuedSendMessages.removeAt(0);
                        Bundle data3 = messageValueAt.getData();
                        int i = messageValueAt.arg2;
                        int i2 = messageValueAt.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_ID);
                        int i3 = data3.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_PEER_ID);
                        byte[] byteArray = data3.getByteArray(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE);
                        int i4 = data3.getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_ID);
                        message.getData().putParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE, messageValueAt);
                        zConnectLocal = WifiAwareStateManager.this.sendFollowonMessageLocal(this.mCurrentTransactionId, i, i2, i3, byteArray, i4);
                        break;
                    }
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RECONFIGURE:
                    zConnectLocal = WifiAwareStateManager.this.reconfigureLocal(this.mCurrentTransactionId);
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_DELAYED_INITIALIZATION:
                    WifiAwareStateManager.this.mWifiAwareNativeManager.start(getHandler());
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_GET_AWARE:
                    WifiAwareStateManager.this.mWifiAwareNativeManager.tryToGetAware();
                    break;
                case WifiAwareStateManager.COMMAND_TYPE_RELEASE_AWARE:
                    WifiAwareStateManager.this.mWifiAwareNativeManager.releaseAware();
                    break;
            }
            if (!zConnectLocal) {
                this.mCurrentTransactionId = (short) 0;
            } else {
                this.mCurrentCommand = obtainMessage(message.what);
                this.mCurrentCommand.copyFrom(message);
            }
            return zConnectLocal;
        }

        private void processResponse(Message message) {
            if (this.mCurrentCommand != null) {
                switch (message.arg1) {
                    case 200:
                        WifiAwareStateManager.this.onConfigCompletedLocal(this.mCurrentCommand);
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_CONFIG_FAIL:
                        WifiAwareStateManager.this.onConfigFailedLocal(this.mCurrentCommand, ((Integer) message.obj).intValue());
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_SESSION_CONFIG_SUCCESS:
                        WifiAwareStateManager.this.onSessionConfigSuccessLocal(this.mCurrentCommand, ((Byte) message.obj).byteValue(), message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_TYPE));
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_SESSION_CONFIG_FAIL:
                        WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SESSION_TYPE), ((Integer) message.obj).intValue());
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_SUCCESS:
                        Message message2 = (Message) this.mCurrentCommand.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE);
                        message2.getData().putLong(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SEND_MESSAGE_ENQUEUE_TIME, SystemClock.elapsedRealtime());
                        this.mFwQueuedSendMessages.put(Short.valueOf(this.mCurrentTransactionId), message2);
                        updateSendMessageTimeout();
                        if (!this.mSendQueueBlocked) {
                            WifiAwareStateManager.this.transmitNextMessage();
                        }
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_MESSAGE_SEND_QUEUED_FAIL:
                        if (((Integer) message.obj).intValue() != 11) {
                            WifiAwareStateManager.this.onMessageSendFailLocal((Message) this.mCurrentCommand.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE), 1);
                            if (!this.mSendQueueBlocked) {
                                WifiAwareStateManager.this.transmitNextMessage();
                            }
                        } else {
                            Message message3 = (Message) this.mCurrentCommand.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE);
                            this.mHostQueuedSendMessages.put(message3.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_MESSAGE_ARRIVAL_SEQ), message3);
                            this.mSendQueueBlocked = true;
                        }
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_CAPABILITIES_UPDATED:
                        WifiAwareStateManager.this.onCapabilitiesUpdatedResponseLocal((Capabilities) message.obj);
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_CREATE_INTERFACE:
                        WifiAwareStateManager.this.onCreateDataPathInterfaceResponseLocal(this.mCurrentCommand, message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE));
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_DELETE_INTERFACE:
                        WifiAwareStateManager.this.onDeleteDataPathInterfaceResponseLocal(this.mCurrentCommand, message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE));
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_INITIATE_DATA_PATH_SUCCESS:
                        WifiAwareStateManager.this.onInitiateDataPathResponseSuccessLocal(this.mCurrentCommand, ((Integer) message.obj).intValue());
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_INITIATE_DATA_PATH_FAIL:
                        WifiAwareStateManager.this.onInitiateDataPathResponseFailLocal(this.mCurrentCommand, ((Integer) message.obj).intValue());
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_RESPOND_TO_DATA_PATH_SETUP_REQUEST:
                        WifiAwareStateManager.this.onRespondToDataPathSetupRequestResponseLocal(this.mCurrentCommand, message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE));
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_END_DATA_PATH:
                        WifiAwareStateManager.this.onEndPathEndResponseLocal(this.mCurrentCommand, message.getData().getBoolean(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SUCCESS_FLAG), message.getData().getInt(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_STATUS_CODE));
                        break;
                    case WifiAwareStateManager.RESPONSE_TYPE_ON_DISABLE:
                        WifiAwareStateManager.this.onDisableResponseLocal(this.mCurrentCommand, ((Integer) message.obj).intValue());
                        break;
                    default:
                        Log.wtf(WifiAwareStateManager.TAG, "processResponse: this isn't a RESPONSE -- msg=" + message);
                        this.mCurrentCommand = null;
                        this.mCurrentTransactionId = (short) 0;
                        return;
                }
                this.mCurrentCommand = null;
                this.mCurrentTransactionId = (short) 0;
                return;
            }
            Log.wtf(WifiAwareStateManager.TAG, "processResponse: no existing command stored!? msg=" + message);
            this.mCurrentTransactionId = (short) 0;
        }

        private void processTimeout(Message message) {
            if (WifiAwareStateManager.this.mDbg) {
                Log.v(WifiAwareStateManager.TAG, "processTimeout: msg=" + message);
            }
            if (this.mCurrentCommand != null) {
                switch (message.arg1) {
                    case 100:
                        WifiAwareStateManager.this.onConfigFailedLocal(this.mCurrentCommand, 1);
                        break;
                    case 101:
                        WifiAwareStateManager.this.onConfigFailedLocal(this.mCurrentCommand, 1);
                        break;
                    case 102:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: TERMINATE_SESSION - shouldn't be waiting!");
                        break;
                    case 103:
                        WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, true, 1);
                        break;
                    case 104:
                        WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, true, 1);
                        break;
                    case 105:
                        WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, false, 1);
                        break;
                    case 106:
                        WifiAwareStateManager.this.onSessionConfigFailLocal(this.mCurrentCommand, false, 1);
                        break;
                    case 107:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: ENQUEUE_SEND_MESSAGE - shouldn't be waiting!");
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_ENABLE_USAGE:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: ENABLE_USAGE - shouldn't be waiting!");
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_DISABLE_USAGE:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: DISABLE_USAGE - shouldn't be waiting!");
                        break;
                    case 110:
                    default:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: this isn't a COMMAND -- msg=" + message);
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_GET_CAPABILITIES:
                        Log.e(WifiAwareStateManager.TAG, "processTimeout: GET_CAPABILITIES timed-out - strange, will try again when next enabled!?");
                        break;
                    case 112:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: CREATE_ALL_DATA_PATH_INTERFACES - shouldn't be waiting!");
                        break;
                    case 113:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: DELETE_ALL_DATA_PATH_INTERFACES - shouldn't be waiting!");
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_CREATE_DATA_PATH_INTERFACE:
                        WifiAwareStateManager.this.onCreateDataPathInterfaceResponseLocal(this.mCurrentCommand, false, 0);
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_DELETE_DATA_PATH_INTERFACE:
                        WifiAwareStateManager.this.onDeleteDataPathInterfaceResponseLocal(this.mCurrentCommand, false, 0);
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_INITIATE_DATA_PATH_SETUP:
                        WifiAwareStateManager.this.onInitiateDataPathResponseFailLocal(this.mCurrentCommand, 0);
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_RESPOND_TO_DATA_PATH_SETUP_REQUEST:
                        WifiAwareStateManager.this.onRespondToDataPathSetupRequestResponseLocal(this.mCurrentCommand, false, 0);
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_END_DATA_PATH:
                        WifiAwareStateManager.this.onEndPathEndResponseLocal(this.mCurrentCommand, false, 0);
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_TRANSMIT_NEXT_MESSAGE:
                        WifiAwareStateManager.this.onMessageSendFailLocal((Message) this.mCurrentCommand.getData().getParcelable(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SENT_MESSAGE), 1);
                        this.mSendQueueBlocked = false;
                        WifiAwareStateManager.this.transmitNextMessage();
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_RECONFIGURE:
                        WifiAwareStateManager.this.onConfigFailedLocal(this.mCurrentCommand, 1);
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_DELAYED_INITIALIZATION:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: COMMAND_TYPE_DELAYED_INITIALIZATION - shouldn't be waiting!");
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_GET_AWARE:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: COMMAND_TYPE_GET_AWARE - shouldn't be waiting!");
                        break;
                    case WifiAwareStateManager.COMMAND_TYPE_RELEASE_AWARE:
                        Log.wtf(WifiAwareStateManager.TAG, "processTimeout: COMMAND_TYPE_RELEASE_AWARE - shouldn't be waiting!");
                        break;
                }
                this.mCurrentCommand = null;
                this.mCurrentTransactionId = (short) 0;
                return;
            }
            Log.wtf(WifiAwareStateManager.TAG, "processTimeout: no existing command stored!? msg=" + message);
            this.mCurrentTransactionId = (short) 0;
        }

        private void updateSendMessageTimeout() {
            Iterator<Message> it = this.mFwQueuedSendMessages.values().iterator();
            if (it.hasNext()) {
                this.mSendMessageTimeoutMessage.schedule(it.next().getData().getLong(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SEND_MESSAGE_ENQUEUE_TIME) + AWARE_SEND_MESSAGE_TIMEOUT);
            } else {
                this.mSendMessageTimeoutMessage.cancel();
            }
        }

        private void processSendMessageTimeout() {
            if (WifiAwareStateManager.this.mDbg) {
                Log.v(WifiAwareStateManager.TAG, "processSendMessageTimeout: mHostQueuedSendMessages.size()=" + this.mHostQueuedSendMessages.size() + ", mFwQueuedSendMessages.size()=" + this.mFwQueuedSendMessages.size() + ", mSendQueueBlocked=" + this.mSendQueueBlocked);
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            Iterator<Map.Entry<Short, Message>> it = this.mFwQueuedSendMessages.entrySet().iterator();
            boolean z = true;
            while (it.hasNext()) {
                Map.Entry<Short, Message> next = it.next();
                short sShortValue = next.getKey().shortValue();
                Message value = next.getValue();
                long j = value.getData().getLong(WifiAwareStateManager.MESSAGE_BUNDLE_KEY_SEND_MESSAGE_ENQUEUE_TIME);
                if (!z && AWARE_SEND_MESSAGE_TIMEOUT + j > jElapsedRealtime) {
                    break;
                }
                if (WifiAwareStateManager.this.mDbg) {
                    Log.v(WifiAwareStateManager.TAG, "processSendMessageTimeout: expiring - transactionId=" + ((int) sShortValue) + ", message=" + value + ", due to messageEnqueueTime=" + j + ", currentTime=" + jElapsedRealtime);
                }
                WifiAwareStateManager.this.onMessageSendFailLocal(value, 1);
                it.remove();
                z = false;
            }
            updateSendMessageTimeout();
            this.mSendQueueBlocked = false;
            WifiAwareStateManager.this.transmitNextMessage();
        }

        protected String getLogRecString(Message message) {
            StringBuilder sb = new StringBuilder(WifiAwareStateManager.messageToString(message));
            if (message.what == 1 && this.mCurrentTransactionId != 0) {
                sb.append(" (Transaction ID=");
                sb.append((int) this.mCurrentTransactionId);
                sb.append(")");
            }
            return sb.toString();
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            printWriter.println("WifiAwareStateMachine:");
            printWriter.println("  mNextTransactionId: " + ((int) this.mNextTransactionId));
            printWriter.println("  mNextSessionId: " + this.mNextSessionId);
            printWriter.println("  mCurrentCommand: " + this.mCurrentCommand);
            printWriter.println("  mCurrentTransaction: " + ((int) this.mCurrentTransactionId));
            printWriter.println("  mSendQueueBlocked: " + this.mSendQueueBlocked);
            printWriter.println("  mSendArrivalSequenceCounter: " + this.mSendArrivalSequenceCounter);
            printWriter.println("  mHostQueuedSendMessages: [" + this.mHostQueuedSendMessages + "]");
            printWriter.println("  mFwQueuedSendMessages: [" + this.mFwQueuedSendMessages + "]");
            super.dump(fileDescriptor, printWriter, strArr);
        }
    }

    private void sendAwareStateChangedBroadcast(boolean z) {
        Intent intent = new Intent("android.net.wifi.aware.action.WIFI_AWARE_STATE_CHANGED");
        intent.addFlags(1073741824);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean connectLocal(short s, int i, int i2, int i3, String str, IWifiAwareEventCallback iWifiAwareEventCallback, ConfigRequest configRequest, boolean z) {
        if (!this.mUsageEnabled) {
            Log.w(TAG, "connect(): called with mUsageEnabled=false");
            try {
                iWifiAwareEventCallback.onConnectFail(1);
                this.mAwareMetrics.recordAttachStatus(1);
            } catch (RemoteException e) {
                Log.w(TAG, "connectLocal onConnectFail(): RemoteException (FYI): " + e);
            }
            return false;
        }
        if (this.mClients.get(i) != null) {
            Log.e(TAG, "connectLocal: entry already exists for clientId=" + i);
        }
        ConfigRequest configRequestMergeConfigRequests = mergeConfigRequests(configRequest);
        if (configRequestMergeConfigRequests == null) {
            Log.e(TAG, "connectLocal: requested configRequest=" + configRequest + ", incompatible with current configurations");
            try {
                iWifiAwareEventCallback.onConnectFail(1);
                this.mAwareMetrics.recordAttachStatus(1);
            } catch (RemoteException e2) {
                Log.w(TAG, "connectLocal onConnectFail(): RemoteException (FYI): " + e2);
            }
            return false;
        }
        if (this.mCurrentAwareConfiguration != null && this.mCurrentAwareConfiguration.equals(configRequestMergeConfigRequests) && (this.mCurrentIdentityNotification || !z)) {
            try {
                iWifiAwareEventCallback.onConnectSuccess(i);
            } catch (RemoteException e3) {
                Log.w(TAG, "connectLocal onConnectSuccess(): RemoteException (FYI): " + e3);
            }
            WifiAwareClientState wifiAwareClientState = new WifiAwareClientState(this.mContext, i, i2, i3, str, iWifiAwareEventCallback, configRequest, z, SystemClock.elapsedRealtime());
            wifiAwareClientState.mDbg = this.mDbg;
            wifiAwareClientState.onInterfaceAddressChange(this.mCurrentDiscoveryInterfaceMac);
            this.mClients.append(i, wifiAwareClientState);
            this.mAwareMetrics.recordAttachSession(i2, z, this.mClients);
            return false;
        }
        boolean z2 = doesAnyClientNeedIdentityChangeNotifications() || z;
        if (this.mCurrentAwareConfiguration == null) {
            this.mWifiAwareNativeManager.tryToGetAware();
        }
        boolean zEnableAndConfigure = this.mWifiAwareNativeApi.enableAndConfigure(s, configRequestMergeConfigRequests, z2, this.mCurrentAwareConfiguration == null, this.mPowerManager.isInteractive(), this.mPowerManager.isDeviceIdleMode());
        if (!zEnableAndConfigure) {
            try {
                iWifiAwareEventCallback.onConnectFail(1);
                this.mAwareMetrics.recordAttachStatus(1);
            } catch (RemoteException e4) {
                Log.w(TAG, "connectLocal onConnectFail(): RemoteException (FYI):  " + e4);
            }
        }
        return zEnableAndConfigure;
    }

    private boolean disconnectLocal(short s, int i) {
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i);
        if (wifiAwareClientState == null) {
            Log.e(TAG, "disconnectLocal: no entry for clientId=" + i);
            return false;
        }
        this.mClients.delete(i);
        this.mAwareMetrics.recordAttachSessionDuration(wifiAwareClientState.getCreationTime());
        SparseArray<WifiAwareDiscoverySessionState> sessions = wifiAwareClientState.getSessions();
        for (int i2 = 0; i2 < sessions.size(); i2++) {
            this.mAwareMetrics.recordDiscoverySessionDuration(sessions.valueAt(i2).getCreationTime(), sessions.valueAt(i2).isPublishSession());
        }
        wifiAwareClientState.destroy();
        if (this.mClients.size() == 0) {
            this.mCurrentAwareConfiguration = null;
            deleteAllDataPathInterfaces();
            return this.mWifiAwareNativeApi.disable(s);
        }
        ConfigRequest configRequestMergeConfigRequests = mergeConfigRequests(null);
        if (configRequestMergeConfigRequests == null) {
            Log.wtf(TAG, "disconnectLocal: got an incompatible merge on remaining configs!?");
            return false;
        }
        boolean zDoesAnyClientNeedIdentityChangeNotifications = doesAnyClientNeedIdentityChangeNotifications();
        if (configRequestMergeConfigRequests.equals(this.mCurrentAwareConfiguration) && this.mCurrentIdentityNotification == zDoesAnyClientNeedIdentityChangeNotifications) {
            return false;
        }
        return this.mWifiAwareNativeApi.enableAndConfigure(s, configRequestMergeConfigRequests, zDoesAnyClientNeedIdentityChangeNotifications, false, this.mPowerManager.isInteractive(), this.mPowerManager.isDeviceIdleMode());
    }

    private boolean reconfigureLocal(short s) {
        if (this.mClients.size() == 0) {
            return false;
        }
        return this.mWifiAwareNativeApi.enableAndConfigure(s, this.mCurrentAwareConfiguration, doesAnyClientNeedIdentityChangeNotifications(), false, this.mPowerManager.isInteractive(), this.mPowerManager.isDeviceIdleMode());
    }

    private void terminateSessionLocal(int i, int i2) {
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i);
        if (wifiAwareClientState == null) {
            Log.e(TAG, "terminateSession: no client exists for clientId=" + i);
            return;
        }
        WifiAwareDiscoverySessionState wifiAwareDiscoverySessionStateTerminateSession = wifiAwareClientState.terminateSession(i2);
        if (wifiAwareDiscoverySessionStateTerminateSession != null) {
            this.mAwareMetrics.recordDiscoverySessionDuration(wifiAwareDiscoverySessionStateTerminateSession.getCreationTime(), wifiAwareDiscoverySessionStateTerminateSession.isPublishSession());
        }
    }

    private boolean publishLocal(short s, int i, PublishConfig publishConfig, IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback) {
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i);
        if (wifiAwareClientState != null) {
            boolean zPublish = this.mWifiAwareNativeApi.publish(s, (byte) 0, publishConfig);
            if (!zPublish) {
                try {
                    iWifiAwareDiscoverySessionCallback.onSessionConfigFail(1);
                } catch (RemoteException e) {
                    Log.w(TAG, "publishLocal onSessionConfigFail(): RemoteException (FYI): " + e);
                }
                this.mAwareMetrics.recordDiscoveryStatus(wifiAwareClientState.getUid(), 1, true);
            }
            return zPublish;
        }
        Log.e(TAG, "publishLocal: no client exists for clientId=" + i);
        return false;
    }

    private boolean updatePublishLocal(short s, int i, int i2, PublishConfig publishConfig) {
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i);
        if (wifiAwareClientState == null) {
            Log.e(TAG, "updatePublishLocal: no client exists for clientId=" + i);
            return false;
        }
        WifiAwareDiscoverySessionState session = wifiAwareClientState.getSession(i2);
        if (session == null) {
            Log.e(TAG, "updatePublishLocal: no session exists for clientId=" + i + ", sessionId=" + i2);
            return false;
        }
        boolean zUpdatePublish = session.updatePublish(s, publishConfig);
        if (!zUpdatePublish) {
            this.mAwareMetrics.recordDiscoveryStatus(wifiAwareClientState.getUid(), 1, true);
        }
        return zUpdatePublish;
    }

    private boolean subscribeLocal(short s, int i, SubscribeConfig subscribeConfig, IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback) {
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i);
        if (wifiAwareClientState != null) {
            boolean zSubscribe = this.mWifiAwareNativeApi.subscribe(s, (byte) 0, subscribeConfig);
            if (!zSubscribe) {
                try {
                    iWifiAwareDiscoverySessionCallback.onSessionConfigFail(1);
                } catch (RemoteException e) {
                    Log.w(TAG, "subscribeLocal onSessionConfigFail(): RemoteException (FYI): " + e);
                }
                this.mAwareMetrics.recordDiscoveryStatus(wifiAwareClientState.getUid(), 1, false);
            }
            return zSubscribe;
        }
        Log.e(TAG, "subscribeLocal: no client exists for clientId=" + i);
        return false;
    }

    private boolean updateSubscribeLocal(short s, int i, int i2, SubscribeConfig subscribeConfig) {
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i);
        if (wifiAwareClientState == null) {
            Log.e(TAG, "updateSubscribeLocal: no client exists for clientId=" + i);
            return false;
        }
        WifiAwareDiscoverySessionState session = wifiAwareClientState.getSession(i2);
        if (session == null) {
            Log.e(TAG, "updateSubscribeLocal: no session exists for clientId=" + i + ", sessionId=" + i2);
            return false;
        }
        boolean zUpdateSubscribe = session.updateSubscribe(s, subscribeConfig);
        if (!zUpdateSubscribe) {
            this.mAwareMetrics.recordDiscoveryStatus(wifiAwareClientState.getUid(), 1, false);
        }
        return zUpdateSubscribe;
    }

    private boolean sendFollowonMessageLocal(short s, int i, int i2, int i3, byte[] bArr, int i4) {
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i);
        if (wifiAwareClientState == null) {
            Log.e(TAG, "sendFollowonMessageLocal: no client exists for clientId=" + i);
            return false;
        }
        WifiAwareDiscoverySessionState session = wifiAwareClientState.getSession(i2);
        if (session == null) {
            Log.e(TAG, "sendFollowonMessageLocal: no session exists for clientId=" + i + ", sessionId=" + i2);
            return false;
        }
        return session.sendMessage(s, i3, bArr, i4);
    }

    private void enableUsageLocal() {
        if (this.mCapabilities == null) {
            getAwareInterface();
            queryCapabilities();
            releaseAwareInterface();
        }
        if (this.mUsageEnabled) {
            return;
        }
        this.mUsageEnabled = true;
        sendAwareStateChangedBroadcast(true);
        this.mAwareMetrics.recordEnableUsage();
    }

    private boolean disableUsageLocal(short s) {
        if (!this.mUsageEnabled) {
            return false;
        }
        onAwareDownLocal();
        this.mUsageEnabled = false;
        boolean zDisable = this.mWifiAwareNativeApi.disable(s);
        sendAwareStateChangedBroadcast(false);
        this.mAwareMetrics.recordDisableUsage();
        return zDisable;
    }

    private boolean initiateDataPathSetupLocal(short s, WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier, int i, int i2, int i3, byte[] bArr, String str, byte[] bArr2, String str2, boolean z) {
        boolean zInitiateDataPath = this.mWifiAwareNativeApi.initiateDataPath(s, i, i2, i3, bArr, str, bArr2, str2, z, this.mCapabilities);
        if (!zInitiateDataPath) {
            this.mDataPathMgr.onDataPathInitiateFail(wifiAwareNetworkSpecifier, 1);
        }
        return zInitiateDataPath;
    }

    private boolean respondToDataPathRequestLocal(short s, boolean z, int i, String str, byte[] bArr, String str2, boolean z2) {
        boolean zRespondToDataPathRequest = this.mWifiAwareNativeApi.respondToDataPathRequest(s, z, i, str, bArr, str2, z2, this.mCapabilities);
        if (!zRespondToDataPathRequest) {
            this.mDataPathMgr.onRespondToDataPathRequest(i, false, 1);
        }
        return zRespondToDataPathRequest;
    }

    private boolean endDataPathLocal(short s, int i) {
        return this.mWifiAwareNativeApi.endDataPath(s, i);
    }

    private void onConfigCompletedLocal(Message message) {
        if (message.arg1 != 100) {
            if (message.arg1 != 101 && message.arg1 != COMMAND_TYPE_RECONFIGURE) {
                Log.wtf(TAG, "onConfigCompletedLocal: unexpected completedCommand=" + message);
                return;
            }
        } else {
            Bundle data = message.getData();
            int i = message.arg2;
            IWifiAwareEventCallback iWifiAwareEventCallback = (IWifiAwareEventCallback) message.obj;
            ConfigRequest parcelable = data.getParcelable(MESSAGE_BUNDLE_KEY_CONFIG);
            int i2 = data.getInt(MESSAGE_BUNDLE_KEY_UID);
            int i3 = data.getInt(MESSAGE_BUNDLE_KEY_PID);
            boolean z = data.getBoolean(MESSAGE_BUNDLE_KEY_NOTIFY_IDENTITY_CHANGE);
            WifiAwareClientState wifiAwareClientState = new WifiAwareClientState(this.mContext, i, i2, i3, data.getString(MESSAGE_BUNDLE_KEY_CALLING_PACKAGE), iWifiAwareEventCallback, parcelable, z, SystemClock.elapsedRealtime());
            wifiAwareClientState.mDbg = this.mDbg;
            this.mClients.put(i, wifiAwareClientState);
            this.mAwareMetrics.recordAttachSession(i2, z, this.mClients);
            try {
                iWifiAwareEventCallback.onConnectSuccess(i);
            } catch (RemoteException e) {
                Log.w(TAG, "onConfigCompletedLocal onConnectSuccess(): RemoteException (FYI): " + e);
            }
            wifiAwareClientState.onInterfaceAddressChange(this.mCurrentDiscoveryInterfaceMac);
        }
        if (this.mCurrentAwareConfiguration == null) {
            createAllDataPathInterfaces();
        }
        this.mCurrentAwareConfiguration = mergeConfigRequests(null);
        if (this.mCurrentAwareConfiguration == null) {
            Log.wtf(TAG, "onConfigCompletedLocal: got a null merged configuration after config!?");
        }
        this.mCurrentIdentityNotification = doesAnyClientNeedIdentityChangeNotifications();
    }

    private void onConfigFailedLocal(Message message, int i) {
        if (message.arg1 == 100) {
            try {
                ((IWifiAwareEventCallback) message.obj).onConnectFail(i);
                this.mAwareMetrics.recordAttachStatus(i);
                return;
            } catch (RemoteException e) {
                Log.w(TAG, "onConfigFailedLocal onConnectFail(): RemoteException (FYI): " + e);
                return;
            }
        }
        if (message.arg1 != 101 && message.arg1 != COMMAND_TYPE_RECONFIGURE) {
            Log.wtf(TAG, "onConfigFailedLocal: unexpected failedCommand=" + message);
        }
    }

    private void onDisableResponseLocal(Message message, int i) {
        if (i != 0) {
            Log.e(TAG, "onDisableResponseLocal: FAILED!? command=" + message + ", reason=" + i);
        }
        this.mAwareMetrics.recordDisableAware();
    }

    private void onSessionConfigSuccessLocal(Message message, byte b, boolean z) {
        int i;
        int i2;
        boolean z2;
        int i3;
        if (message.arg1 == 103 || message.arg1 == 105) {
            int i4 = message.arg2;
            IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback = (IWifiAwareDiscoverySessionCallback) message.obj;
            WifiAwareClientState wifiAwareClientState = this.mClients.get(i4);
            if (wifiAwareClientState == null) {
                Log.e(TAG, "onSessionConfigSuccessLocal: no client exists for clientId=" + i4);
                return;
            }
            WifiAwareStateMachine wifiAwareStateMachine = this.mSm;
            int i5 = wifiAwareStateMachine.mNextSessionId;
            wifiAwareStateMachine.mNextSessionId = i5 + 1;
            try {
                iWifiAwareDiscoverySessionCallback.onSessionStarted(i5);
                if (message.arg1 == 103) {
                    z2 = ((PublishConfig) message.getData().getParcelable(MESSAGE_BUNDLE_KEY_CONFIG)).mEnableRanging;
                    i3 = -1;
                    i2 = -1;
                } else {
                    SubscribeConfig subscribeConfig = (SubscribeConfig) message.getData().getParcelable(MESSAGE_BUNDLE_KEY_CONFIG);
                    boolean z3 = subscribeConfig.mMinDistanceMmSet || subscribeConfig.mMaxDistanceMmSet;
                    if (subscribeConfig.mMinDistanceMmSet) {
                        i = subscribeConfig.mMinDistanceMm;
                    } else {
                        i = -1;
                    }
                    i2 = subscribeConfig.mMaxDistanceMmSet ? subscribeConfig.mMaxDistanceMm : -1;
                    z2 = z3;
                    i3 = i;
                }
                WifiAwareDiscoverySessionState wifiAwareDiscoverySessionState = new WifiAwareDiscoverySessionState(this.mWifiAwareNativeApi, i5, b, iWifiAwareDiscoverySessionCallback, z, z2, SystemClock.elapsedRealtime());
                wifiAwareDiscoverySessionState.mDbg = this.mDbg;
                wifiAwareClientState.addSession(wifiAwareDiscoverySessionState);
                if (z2) {
                    this.mAwareMetrics.recordDiscoverySessionWithRanging(wifiAwareClientState.getUid(), message.arg1 != 103, i3, i2, this.mClients);
                } else {
                    this.mAwareMetrics.recordDiscoverySession(wifiAwareClientState.getUid(), this.mClients);
                }
                this.mAwareMetrics.recordDiscoveryStatus(wifiAwareClientState.getUid(), 0, message.arg1 == 103);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "onSessionConfigSuccessLocal: onSessionStarted() RemoteException=" + e);
                return;
            }
        }
        if (message.arg1 == 104 || message.arg1 == 106) {
            int i6 = message.arg2;
            int i7 = message.getData().getInt(MESSAGE_BUNDLE_KEY_SESSION_ID);
            WifiAwareClientState wifiAwareClientState2 = this.mClients.get(i6);
            if (wifiAwareClientState2 == null) {
                Log.e(TAG, "onSessionConfigSuccessLocal: no client exists for clientId=" + i6);
                return;
            }
            WifiAwareDiscoverySessionState session = wifiAwareClientState2.getSession(i7);
            if (session == null) {
                Log.e(TAG, "onSessionConfigSuccessLocal: no session exists for clientId=" + i6 + ", sessionId=" + i7);
                return;
            }
            try {
                session.getCallback().onSessionConfigSuccess();
            } catch (RemoteException e2) {
                Log.e(TAG, "onSessionConfigSuccessLocal: onSessionConfigSuccess() RemoteException=" + e2);
            }
            this.mAwareMetrics.recordDiscoveryStatus(wifiAwareClientState2.getUid(), 0, message.arg1 == 104);
            return;
        }
        Log.wtf(TAG, "onSessionConfigSuccessLocal: unexpected completedCommand=" + message);
    }

    private void onSessionConfigFailLocal(Message message, boolean z, int i) {
        if (message.arg1 == 103 || message.arg1 == 105) {
            int i2 = message.arg2;
            IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback = (IWifiAwareDiscoverySessionCallback) message.obj;
            WifiAwareClientState wifiAwareClientState = this.mClients.get(i2);
            if (wifiAwareClientState == null) {
                Log.e(TAG, "onSessionConfigFailLocal: no client exists for clientId=" + i2);
                return;
            }
            try {
                iWifiAwareDiscoverySessionCallback.onSessionConfigFail(i);
            } catch (RemoteException e) {
                Log.w(TAG, "onSessionConfigFailLocal onSessionConfigFail(): RemoteException (FYI): " + e);
            }
            this.mAwareMetrics.recordDiscoveryStatus(wifiAwareClientState.getUid(), i, message.arg1 == 103);
            return;
        }
        if (message.arg1 == 104 || message.arg1 == 106) {
            int i3 = message.arg2;
            int i4 = message.getData().getInt(MESSAGE_BUNDLE_KEY_SESSION_ID);
            WifiAwareClientState wifiAwareClientState2 = this.mClients.get(i3);
            if (wifiAwareClientState2 == null) {
                Log.e(TAG, "onSessionConfigFailLocal: no client exists for clientId=" + i3);
                return;
            }
            WifiAwareDiscoverySessionState session = wifiAwareClientState2.getSession(i4);
            if (session == null) {
                Log.e(TAG, "onSessionConfigFailLocal: no session exists for clientId=" + i3 + ", sessionId=" + i4);
                return;
            }
            try {
                session.getCallback().onSessionConfigFail(i);
            } catch (RemoteException e2) {
                Log.e(TAG, "onSessionConfigFailLocal: onSessionConfigFail() RemoteException=" + e2);
            }
            this.mAwareMetrics.recordDiscoveryStatus(wifiAwareClientState2.getUid(), i, message.arg1 == 104);
            if (i == 3) {
                wifiAwareClientState2.removeSession(i4);
                return;
            }
            return;
        }
        Log.wtf(TAG, "onSessionConfigFailLocal: unexpected failedCommand=" + message);
    }

    private void onMessageSendSuccessLocal(Message message) {
        int i = message.arg2;
        int i2 = message.getData().getInt(MESSAGE_BUNDLE_KEY_SESSION_ID);
        int i3 = message.getData().getInt(MESSAGE_BUNDLE_KEY_MESSAGE_ID);
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i);
        if (wifiAwareClientState == null) {
            Log.e(TAG, "onMessageSendSuccessLocal: no client exists for clientId=" + i);
            return;
        }
        WifiAwareDiscoverySessionState session = wifiAwareClientState.getSession(i2);
        if (session == null) {
            Log.e(TAG, "onMessageSendSuccessLocal: no session exists for clientId=" + i + ", sessionId=" + i2);
            return;
        }
        try {
            session.getCallback().onMessageSendSuccess(i3);
        } catch (RemoteException e) {
            Log.w(TAG, "onMessageSendSuccessLocal: RemoteException (FYI): " + e);
        }
    }

    private void onMessageSendFailLocal(Message message, int i) {
        int i2 = message.arg2;
        int i3 = message.getData().getInt(MESSAGE_BUNDLE_KEY_SESSION_ID);
        int i4 = message.getData().getInt(MESSAGE_BUNDLE_KEY_MESSAGE_ID);
        WifiAwareClientState wifiAwareClientState = this.mClients.get(i2);
        if (wifiAwareClientState == null) {
            Log.e(TAG, "onMessageSendFailLocal: no client exists for clientId=" + i2);
            return;
        }
        WifiAwareDiscoverySessionState session = wifiAwareClientState.getSession(i3);
        if (session == null) {
            Log.e(TAG, "onMessageSendFailLocal: no session exists for clientId=" + i2 + ", sessionId=" + i3);
            return;
        }
        try {
            session.getCallback().onMessageSendFail(i4, i);
        } catch (RemoteException e) {
            Log.e(TAG, "onMessageSendFailLocal: onMessageSendFail RemoteException=" + e);
        }
    }

    private void onCapabilitiesUpdatedResponseLocal(Capabilities capabilities) {
        this.mCapabilities = capabilities;
        this.mCharacteristics = null;
    }

    private void onCreateDataPathInterfaceResponseLocal(Message message, boolean z, int i) {
        if (z) {
            this.mDataPathMgr.onInterfaceCreated((String) message.obj);
            return;
        }
        Log.e(TAG, "onCreateDataPathInterfaceResponseLocal: failed when trying to create interface " + message.obj + ". Reason code=" + i);
    }

    private void onDeleteDataPathInterfaceResponseLocal(Message message, boolean z, int i) {
        if (z) {
            this.mDataPathMgr.onInterfaceDeleted((String) message.obj);
            return;
        }
        Log.e(TAG, "onDeleteDataPathInterfaceResponseLocal: failed when trying to delete interface " + message.obj + ". Reason code=" + i);
    }

    private void onInitiateDataPathResponseSuccessLocal(Message message, int i) {
        this.mDataPathMgr.onDataPathInitiateSuccess((WifiAwareNetworkSpecifier) message.obj, i);
    }

    private void onInitiateDataPathResponseFailLocal(Message message, int i) {
        this.mDataPathMgr.onDataPathInitiateFail((WifiAwareNetworkSpecifier) message.obj, i);
    }

    private void onRespondToDataPathSetupRequestResponseLocal(Message message, boolean z, int i) {
        this.mDataPathMgr.onRespondToDataPathRequest(message.arg2, z, i);
    }

    private void onEndPathEndResponseLocal(Message message, boolean z, int i) {
    }

    private void onInterfaceAddressChangeLocal(byte[] bArr) {
        this.mCurrentDiscoveryInterfaceMac = bArr;
        for (int i = 0; i < this.mClients.size(); i++) {
            this.mClients.valueAt(i).onInterfaceAddressChange(bArr);
        }
        this.mAwareMetrics.recordEnableAware();
    }

    private void onClusterChangeLocal(int i, byte[] bArr) {
        for (int i2 = 0; i2 < this.mClients.size(); i2++) {
            this.mClients.valueAt(i2).onClusterChange(i, bArr, this.mCurrentDiscoveryInterfaceMac);
        }
        this.mAwareMetrics.recordEnableAware();
    }

    private void onMatchLocal(int i, int i2, byte[] bArr, byte[] bArr2, byte[] bArr3, int i3, int i4) {
        Pair<WifiAwareClientState, WifiAwareDiscoverySessionState> clientSessionForPubSubId = getClientSessionForPubSubId(i);
        if (clientSessionForPubSubId == null) {
            Log.e(TAG, "onMatch: no session found for pubSubId=" + i);
            return;
        }
        if (((WifiAwareDiscoverySessionState) clientSessionForPubSubId.second).isRangingEnabled()) {
            this.mAwareMetrics.recordMatchIndicationForRangeEnabledSubscribe(i3 != 0);
        }
        ((WifiAwareDiscoverySessionState) clientSessionForPubSubId.second).onMatch(i2, bArr, bArr2, bArr3, i3, i4);
    }

    private void onSessionTerminatedLocal(int i, boolean z, int i2) {
        Pair<WifiAwareClientState, WifiAwareDiscoverySessionState> clientSessionForPubSubId = getClientSessionForPubSubId(i);
        if (clientSessionForPubSubId == null) {
            Log.e(TAG, "onSessionTerminatedLocal: no session found for pubSubId=" + i);
            return;
        }
        try {
            ((WifiAwareDiscoverySessionState) clientSessionForPubSubId.second).getCallback().onSessionTerminated(i2);
        } catch (RemoteException e) {
            Log.w(TAG, "onSessionTerminatedLocal onSessionTerminated(): RemoteException (FYI): " + e);
        }
        ((WifiAwareClientState) clientSessionForPubSubId.first).removeSession(((WifiAwareDiscoverySessionState) clientSessionForPubSubId.second).getSessionId());
        this.mAwareMetrics.recordDiscoverySessionDuration(((WifiAwareDiscoverySessionState) clientSessionForPubSubId.second).getCreationTime(), ((WifiAwareDiscoverySessionState) clientSessionForPubSubId.second).isPublishSession());
    }

    private void onMessageReceivedLocal(int i, int i2, byte[] bArr, byte[] bArr2) {
        Pair<WifiAwareClientState, WifiAwareDiscoverySessionState> clientSessionForPubSubId = getClientSessionForPubSubId(i);
        if (clientSessionForPubSubId == null) {
            Log.e(TAG, "onMessageReceivedLocal: no session found for pubSubId=" + i);
            return;
        }
        ((WifiAwareDiscoverySessionState) clientSessionForPubSubId.second).onMessageReceived(i2, bArr, bArr2);
    }

    private void onAwareDownLocal() {
        if (this.mCurrentAwareConfiguration == null) {
            return;
        }
        for (int i = 0; i < this.mClients.size(); i++) {
            this.mAwareMetrics.recordAttachSessionDuration(this.mClients.valueAt(i).getCreationTime());
            SparseArray<WifiAwareDiscoverySessionState> sessions = this.mClients.valueAt(i).getSessions();
            for (int i2 = 0; i2 < sessions.size(); i2++) {
                this.mAwareMetrics.recordDiscoverySessionDuration(sessions.valueAt(i).getCreationTime(), sessions.valueAt(i).isPublishSession());
            }
        }
        this.mAwareMetrics.recordDisableAware();
        this.mClients.clear();
        this.mCurrentAwareConfiguration = null;
        this.mSm.onAwareDownCleanupSendQueueState();
        this.mDataPathMgr.onAwareDownCleanupDataPaths();
        this.mCurrentDiscoveryInterfaceMac = ALL_ZERO_MAC;
        deleteAllDataPathInterfaces();
    }

    private Pair<WifiAwareClientState, WifiAwareDiscoverySessionState> getClientSessionForPubSubId(int i) {
        for (int i2 = 0; i2 < this.mClients.size(); i2++) {
            WifiAwareClientState wifiAwareClientStateValueAt = this.mClients.valueAt(i2);
            WifiAwareDiscoverySessionState awareSessionStateForPubSubId = wifiAwareClientStateValueAt.getAwareSessionStateForPubSubId(i);
            if (awareSessionStateForPubSubId != null) {
                return new Pair<>(wifiAwareClientStateValueAt, awareSessionStateForPubSubId);
            }
        }
        return null;
    }

    private ConfigRequest mergeConfigRequests(ConfigRequest configRequest) {
        int[] iArr;
        boolean z;
        int i;
        int iMax;
        boolean z2;
        if (this.mClients.size() == 0 && configRequest == null) {
            Log.e(TAG, "mergeConfigRequests: invalid state - called with 0 clients registered!");
            return null;
        }
        int i2 = Constants.SHORT_MASK;
        int[] iArr2 = {-1, -1};
        if (configRequest == null) {
            iArr = iArr2;
            z = false;
            i = 0;
            iMax = 0;
            z2 = false;
        } else {
            boolean z3 = configRequest.mSupport5gBand;
            int i3 = configRequest.mMasterPreference;
            i = configRequest.mClusterLow;
            int i4 = configRequest.mClusterHigh;
            iArr = configRequest.mDiscoveryWindowInterval;
            z2 = true;
            z = z3;
            i2 = i4;
            iMax = i3;
        }
        boolean z4 = z2;
        int i5 = i2;
        for (int i6 = 0; i6 < this.mClients.size(); i6++) {
            ConfigRequest configRequest2 = this.mClients.valueAt(i6).getConfigRequest();
            if (configRequest2.mSupport5gBand) {
                z = true;
            }
            iMax = Math.max(iMax, configRequest2.mMasterPreference);
            if (!z4) {
                i = configRequest2.mClusterLow;
                i5 = configRequest2.mClusterHigh;
                z4 = true;
            } else if (i != configRequest2.mClusterLow || i5 != configRequest2.mClusterHigh) {
                return null;
            }
            for (int i7 = 0; i7 <= 1; i7++) {
                if (iArr[i7] == -1) {
                    iArr[i7] = configRequest2.mDiscoveryWindowInterval[i7];
                } else if (configRequest2.mDiscoveryWindowInterval[i7] != -1) {
                    if (iArr[i7] == 0) {
                        iArr[i7] = configRequest2.mDiscoveryWindowInterval[i7];
                    } else if (configRequest2.mDiscoveryWindowInterval[i7] != 0) {
                        iArr[i7] = Math.min(iArr[i7], configRequest2.mDiscoveryWindowInterval[i7]);
                    }
                }
            }
        }
        ConfigRequest.Builder clusterHigh = new ConfigRequest.Builder().setSupport5gBand(z).setMasterPreference(iMax).setClusterLow(i).setClusterHigh(i5);
        for (int i8 = 0; i8 <= 1; i8++) {
            if (iArr[i8] != -1) {
                clusterHigh.setDiscoveryWindowInterval(i8, iArr[i8]);
            }
        }
        return clusterHigh.build();
    }

    private boolean doesAnyClientNeedIdentityChangeNotifications() {
        for (int i = 0; i < this.mClients.size(); i++) {
            if (this.mClients.valueAt(i).getNotifyIdentityChange()) {
                return true;
            }
        }
        return false;
    }

    private static String messageToString(Message message) {
        StringBuilder sb = new StringBuilder();
        String str = sSmToString.get(message.what);
        if (str == null) {
            str = "<unknown>";
        }
        sb.append(str);
        sb.append("/");
        if (message.what == 3 || message.what == 1 || message.what == 2) {
            String str2 = sSmToString.get(message.arg1);
            if (str2 == null) {
                str2 = "<unknown>";
            }
            sb.append(str2);
        }
        if (message.what == 2 || message.what == 4) {
            sb.append(" (Transaction ID=");
            sb.append(message.arg2);
            sb.append(")");
        }
        return sb.toString();
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("AwareStateManager:");
        printWriter.println("  mClients: [" + this.mClients + "]");
        StringBuilder sb = new StringBuilder();
        sb.append("  mUsageEnabled: ");
        sb.append(this.mUsageEnabled);
        printWriter.println(sb.toString());
        printWriter.println("  mCapabilities: [" + this.mCapabilities + "]");
        StringBuilder sb2 = new StringBuilder();
        sb2.append("  mCurrentAwareConfiguration: ");
        sb2.append(this.mCurrentAwareConfiguration);
        printWriter.println(sb2.toString());
        printWriter.println("  mCurrentIdentityNotification: " + this.mCurrentIdentityNotification);
        for (int i = 0; i < this.mClients.size(); i++) {
            this.mClients.valueAt(i).dump(fileDescriptor, printWriter, strArr);
        }
        printWriter.println("  mSettableParameters: " + this.mSettableParameters);
        this.mSm.dump(fileDescriptor, printWriter, strArr);
        this.mDataPathMgr.dump(fileDescriptor, printWriter, strArr);
        this.mWifiAwareNativeApi.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("mAwareMetrics:");
        this.mAwareMetrics.dump(fileDescriptor, printWriter, strArr);
    }
}
