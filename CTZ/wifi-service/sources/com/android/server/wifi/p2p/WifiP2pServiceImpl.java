package com.android.server.wifi.p2p;

import android.R;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.ip.IpClient;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.IWifiP2pManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pGroupList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.os.Binder;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.HalDeviceManager;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiStateMachine;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.util.WifiAsyncChannel;
import com.android.server.wifi.util.WifiHandler;
import com.mediatek.server.wifi.WifiNvRamAgent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class WifiP2pServiceImpl extends IWifiP2pManager.Stub {
    private static final int BASE = 143360;
    public static final int BLOCK_DISCOVERY = 143375;
    private static final int CONNECTED_DISCOVER_TIMEOUT_S = 25;
    private static final boolean DBG = true;
    private static final String DHCP_INFO_FILE = "/data/misc/dhcp/dnsmasq.p2p0.leases";
    public static final int DISABLED = 0;
    public static final int DISABLE_P2P = 143377;
    public static final int DISABLE_P2P_TIMED_OUT = 143366;
    private static final int DISABLE_P2P_WAIT_TIME_MS = 5000;
    public static final int DISCONNECT_WIFI_REQUEST = 143372;
    public static final int DISCONNECT_WIFI_RESPONSE = 143373;
    private static final int DISCOVER_TIMEOUT_S = 120;
    private static final int DROP_WIFI_USER_ACCEPT = 143364;
    private static final int DROP_WIFI_USER_REJECT = 143365;
    private static final String EMPTY_DEVICE_ADDRESS = "00:00:00:00:00:00";
    public static final int ENABLED = 1;
    public static final int ENABLE_P2P = 143376;
    public static final int GROUP_CREATING_TIMED_OUT = 143361;
    private static final int GROUP_CREATING_WAIT_TIME_MS = 120000;
    private static final int GROUP_IDLE_TIME_S = 10;
    private static final int IPC_DHCP_RESULTS = 143392;
    private static final int IPC_POST_DHCP_ACTION = 143391;
    private static final int IPC_PRE_DHCP_ACTION = 143390;
    private static final int IPC_PROVISIONING_FAILURE = 143394;
    private static final int IPC_PROVISIONING_SUCCESS = 143393;
    private static final String NETWORKTYPE = "WIFI_P2P";
    public static final int P2P_CONNECTION_CHANGED = 143371;
    private static final int PEER_CONNECTION_USER_ACCEPT = 143362;
    private static final int PEER_CONNECTION_USER_REJECT = 143363;
    private static final String SERVER_ADDRESS = "192.168.49.1";
    public static final int SET_MIRACAST_MODE = 143374;
    private static final String TAG = "WifiP2pService";
    private boolean mAutonomousGroup;
    private ClientHandler mClientHandler;
    private Context mContext;
    private int mDeviceCapa;
    private DhcpResults mDhcpResults;
    private boolean mDiscoveryBlocked;
    private boolean mDiscoveryStarted;
    private String mInterface;
    private IpClient mIpClient;
    private boolean mJoinExistingGroup;
    INetworkManagementService mNwService;
    private P2pStateMachine mP2pStateMachine;
    private final boolean mP2pSupported;
    private String mServiceDiscReqId;
    private String mWfdSourceAddr;
    private AsyncChannel mWifiChannel;
    private WifiInjector mWifiInjector;
    private WifiManager mWifiManager;
    private static final Boolean JOIN_GROUP = true;
    private static final Boolean FORM_GROUP = false;
    private static final Boolean RELOAD = true;
    private static final Boolean NO_RELOAD = false;
    private static int sGroupCreatingTimeoutIndex = 0;
    private static int sDisableP2pTimeoutIndex = 0;
    private static final Boolean WFD_DONGLE_USE_P2P_INVITE = true;
    private AsyncChannel mReplyChannel = new WifiAsyncChannel(TAG);
    private WifiP2pDevice mThisDevice = new WifiP2pDevice();
    private boolean mDiscoveryPostponed = false;
    private boolean mTemporarilyDisconnectedWifi = false;
    private byte mServiceTransactionId = 0;
    private HashMap<Messenger, ClientInfo> mClientInfoList = new HashMap<>();
    private P2pStatus mGroupRemoveReason = P2pStatus.UNKNOWN;
    private int mMiracastMode = 0;
    private int mP2pOperFreq = -1;
    boolean mNegoChannelConflict = false;
    private boolean mConnectToPeer = false;
    private boolean mMccSupport = false;
    private Object mLock = new Object();
    private final Map<IBinder, DeathHandlerData> mDeathDataByBinder = new HashMap();
    private NetworkInfo mNetworkInfo = new NetworkInfo(13, 0, NETWORKTYPE, "");

    static int access$1804() {
        int i = sDisableP2pTimeoutIndex + 1;
        sDisableP2pTimeoutIndex = i;
        return i;
    }

    static int access$6204() {
        int i = sGroupCreatingTimeoutIndex + 1;
        sGroupCreatingTimeoutIndex = i;
        return i;
    }

    static byte access$9904(WifiP2pServiceImpl wifiP2pServiceImpl) {
        byte b = (byte) (wifiP2pServiceImpl.mServiceTransactionId + 1);
        wifiP2pServiceImpl.mServiceTransactionId = b;
        return b;
    }

    public enum P2pStatus {
        SUCCESS,
        INFORMATION_IS_CURRENTLY_UNAVAILABLE,
        INCOMPATIBLE_PARAMETERS,
        LIMIT_REACHED,
        INVALID_PARAMETER,
        UNABLE_TO_ACCOMMODATE_REQUEST,
        PREVIOUS_PROTOCOL_ERROR,
        NO_COMMON_CHANNEL,
        UNKNOWN_P2P_GROUP,
        BOTH_GO_INTENT_15,
        INCOMPATIBLE_PROVISIONING_METHOD,
        REJECTED_BY_USER,
        MTK_EXPAND_01,
        MTK_EXPAND_02,
        UNKNOWN;

        public static P2pStatus valueOf(int i) {
            switch (i) {
                case 0:
                    return SUCCESS;
                case 1:
                    return INFORMATION_IS_CURRENTLY_UNAVAILABLE;
                case 2:
                    return INCOMPATIBLE_PARAMETERS;
                case 3:
                    return LIMIT_REACHED;
                case 4:
                    return INVALID_PARAMETER;
                case 5:
                    return UNABLE_TO_ACCOMMODATE_REQUEST;
                case 6:
                    return PREVIOUS_PROTOCOL_ERROR;
                case 7:
                    return NO_COMMON_CHANNEL;
                case 8:
                    return UNKNOWN_P2P_GROUP;
                case 9:
                    return BOTH_GO_INTENT_15;
                case 10:
                    return INCOMPATIBLE_PROVISIONING_METHOD;
                case 11:
                    return REJECTED_BY_USER;
                default:
                    return UNKNOWN;
            }
        }
    }

    private class ClientHandler extends WifiHandler {
        ClientHandler(String str, Looper looper) {
            super(str, looper);
        }

        @Override
        public void handleMessage(Message message) {
            super.handleMessage(message);
            int i = message.what;
            switch (i) {
                case 139354:
                case 139355:
                case 139356:
                case 139357:
                    break;
                default:
                    switch (i) {
                        case 139265:
                        case 139268:
                        case 139271:
                        case 139274:
                        case 139277:
                        case 139280:
                        case 139283:
                        case 139285:
                        case 139287:
                        case 139292:
                        case 139295:
                        case 139298:
                        case 139301:
                        case 139304:
                        case 139307:
                        case 139310:
                        case 139315:
                        case 139318:
                        case 139321:
                        case 139323:
                        case 139326:
                        case 139329:
                        case 139332:
                        case 139335:
                        case 139361:
                            break;
                        default:
                            Slog.d(WifiP2pServiceImpl.TAG, "ClientHandler.handleMessage ignoring msg=" + message);
                            break;
                    }
            }
            WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(Message.obtain(message));
        }
    }

    private class DeathHandlerData {
        IBinder.DeathRecipient mDeathRecipient;
        Messenger mMessenger;

        DeathHandlerData(IBinder.DeathRecipient deathRecipient, Messenger messenger) {
            this.mDeathRecipient = deathRecipient;
            this.mMessenger = messenger;
        }

        public String toString() {
            return "deathRecipient=" + this.mDeathRecipient + ", messenger=" + this.mMessenger;
        }
    }

    public WifiP2pServiceImpl(Context context) {
        this.mContext = context;
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mThisDevice.primaryDeviceType = this.mContext.getResources().getString(R.string.app_category_productivity);
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mClientHandler = new ClientHandler(TAG, handlerThread.getLooper());
        this.mP2pStateMachine = new P2pStateMachine(TAG, handlerThread.getLooper(), this.mP2pSupported);
        this.mP2pStateMachine.start();
    }

    public void connectivityServiceReady() {
        this.mNwService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
    }

    private void enforceAccessPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.ACCESS_WIFI_STATE", TAG);
    }

    private void enforceChangePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CHANGE_WIFI_STATE", TAG);
    }

    private void enforceConnectivityInternalPermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
    }

    private int checkConnectivityInternalPermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL");
    }

    private int checkLocationHardwarePermission() {
        return this.mContext.checkCallingOrSelfPermission("android.permission.LOCATION_HARDWARE");
    }

    private void enforceConnectivityInternalOrLocationHardwarePermission() {
        if (checkConnectivityInternalPermission() != 0 && checkLocationHardwarePermission() != 0) {
            enforceConnectivityInternalPermission();
        }
    }

    private void stopIpClient() {
        if (this.mIpClient != null) {
            this.mIpClient.stop();
            this.mIpClient = null;
        }
        this.mDhcpResults = null;
    }

    private void startIpClient(String str) {
        stopIpClient();
        this.mIpClient = new IpClient(this.mContext, str, new IpClient.Callback() {
            public void onPreDhcpAction() {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION);
            }

            public void onPostDhcpAction() {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_POST_DHCP_ACTION);
            }

            public void onNewDhcpResults(DhcpResults dhcpResults) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_DHCP_RESULTS, dhcpResults);
            }

            public void onProvisioningSuccess(LinkProperties linkProperties) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS);
            }

            public void onProvisioningFailure(LinkProperties linkProperties) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE);
            }
        }, this.mNwService);
        IpClient ipClient = this.mIpClient;
        this.mIpClient.startProvisioning(IpClient.buildProvisioningConfiguration().withoutIPv6().withoutIpReachabilityMonitor().withPreDhcpAction(WifiStateMachine.LAST_SELECTED_NETWORK_EXPIRATION_AGE_MILLIS).withProvisioningTimeoutMs(36000).build());
    }

    public Messenger getMessenger(final IBinder iBinder) {
        Messenger messenger;
        enforceAccessPermission();
        enforceChangePermission();
        synchronized (this.mLock) {
            messenger = new Messenger(this.mClientHandler);
            Log.d(TAG, "getMessenger: uid=" + getCallingUid() + ", binder=" + iBinder + ", messenger=" + messenger);
            IBinder.DeathRecipient deathRecipient = new IBinder.DeathRecipient() {
                @Override
                public final void binderDied() {
                    WifiP2pServiceImpl.lambda$getMessenger$0(this.f$0, iBinder);
                }
            };
            try {
                iBinder.linkToDeath(deathRecipient, 0);
                this.mDeathDataByBinder.put(iBinder, new DeathHandlerData(deathRecipient, messenger));
            } catch (RemoteException e) {
                Log.e(TAG, "Error on linkToDeath: e=" + e);
            }
            this.mP2pStateMachine.sendMessage(ENABLE_P2P);
        }
        return messenger;
    }

    public static void lambda$getMessenger$0(WifiP2pServiceImpl wifiP2pServiceImpl, IBinder iBinder) {
        Log.d(TAG, "binderDied: binder=" + iBinder);
        wifiP2pServiceImpl.close(iBinder);
    }

    public Messenger getP2pStateMachineMessenger() {
        enforceConnectivityInternalOrLocationHardwarePermission();
        enforceAccessPermission();
        enforceChangePermission();
        return new Messenger(this.mP2pStateMachine.getHandler());
    }

    public void close(IBinder iBinder) {
        enforceAccessPermission();
        enforceChangePermission();
        synchronized (this.mLock) {
            DeathHandlerData deathHandlerData = this.mDeathDataByBinder.get(iBinder);
            if (deathHandlerData == null) {
                Log.w(TAG, "close(): no death recipient for binder");
                return;
            }
            iBinder.unlinkToDeath(deathHandlerData.mDeathRecipient, 0);
            this.mDeathDataByBinder.remove(iBinder);
            if (deathHandlerData.mMessenger != null && this.mDeathDataByBinder.isEmpty()) {
                try {
                    deathHandlerData.mMessenger.send(this.mClientHandler.obtainMessage(139268));
                    deathHandlerData.mMessenger.send(this.mClientHandler.obtainMessage(139280));
                } catch (RemoteException e) {
                    Log.e(TAG, "close: Failed sending clean-up commands: e=" + e);
                }
                this.mP2pStateMachine.sendMessage(DISABLE_P2P);
            }
        }
    }

    public void setMiracastMode(int i) {
        enforceConnectivityInternalPermission();
        checkConfigureWifiDisplayPermission();
        this.mP2pStateMachine.sendMessage(SET_MIRACAST_MODE, i);
    }

    public void checkConfigureWifiDisplayPermission() {
        if (!getWfdPermission(Binder.getCallingUid())) {
            throw new SecurityException("Wifi Display Permission denied for uid = " + Binder.getCallingUid());
        }
    }

    private boolean getWfdPermission(int i) {
        if (this.mWifiInjector == null) {
            this.mWifiInjector = WifiInjector.getInstance();
        }
        return this.mWifiInjector.getWifiPermissionsWrapper().getUidPermission("android.permission.CONFIGURE_WIFI_DISPLAY", i) != -1;
    }

    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            printWriter.println("Permission Denial: can't dump WifiP2pService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        this.mP2pStateMachine.dump(fileDescriptor, printWriter, strArr);
        printWriter.println("mAutonomousGroup " + this.mAutonomousGroup);
        printWriter.println("mJoinExistingGroup " + this.mJoinExistingGroup);
        printWriter.println("mDiscoveryStarted " + this.mDiscoveryStarted);
        printWriter.println("mNetworkInfo " + this.mNetworkInfo);
        printWriter.println("mTemporarilyDisconnectedWifi " + this.mTemporarilyDisconnectedWifi);
        printWriter.println("mServiceDiscReqId " + this.mServiceDiscReqId);
        printWriter.println("mDeathDataByBinder " + this.mDeathDataByBinder);
        printWriter.println();
        IpClient ipClient = this.mIpClient;
        if (ipClient != null) {
            printWriter.println("mIpClient:");
            ipClient.dump(fileDescriptor, printWriter, strArr);
        }
    }

    public String getMacAddress() {
        String str;
        StringBuilder sb;
        Log.d(TAG, "getMacAddress(): before retriving from NVRAM = " + this.mThisDevice.deviceAddress);
        try {
            try {
                byte[] fileByName = WifiNvRamAgent.Stub.asInterface(ServiceManager.getService("NvRAMAgent")).readFileByName("/data/nvram/APCFG/APRDEB/WIFI");
                if (fileByName != null) {
                    String str2 = String.format("%02x:%02x:%02x:%02x:%02x:%02x", Integer.valueOf(fileByName[4] | 2), Byte.valueOf(fileByName[5]), Byte.valueOf(fileByName[6]), Byte.valueOf(fileByName[7]), Byte.valueOf(fileByName[8]), Byte.valueOf(fileByName[9]));
                    if (!TextUtils.isEmpty(str2)) {
                        this.mThisDevice.deviceAddress = str2;
                    }
                }
                str = TAG;
                sb = new StringBuilder();
            } catch (RemoteException e) {
                e.printStackTrace();
                str = TAG;
                sb = new StringBuilder();
            } catch (IndexOutOfBoundsException e2) {
                e2.printStackTrace();
                str = TAG;
                sb = new StringBuilder();
            }
            sb.append("getMacAddress(): after retriving from NVRAM = ");
            sb.append(this.mThisDevice.deviceAddress);
            Log.d(str, sb.toString());
            return this.mThisDevice.deviceAddress;
        } catch (Throwable th) {
            Log.d(TAG, "getMacAddress(): after retriving from NVRAM = " + this.mThisDevice.deviceAddress);
            throw th;
        }
    }

    public String getPeerIpAddress(String str) {
        return this.mP2pStateMachine.getPeerIpAddress(str);
    }

    private class P2pStateMachine extends StateMachine {
        private DefaultState mDefaultState;
        private FrequencyConflictState mFrequencyConflictState;
        private WifiP2pGroup mGroup;
        private GroupCreatedState mGroupCreatedState;
        private GroupCreatingState mGroupCreatingState;
        private GroupNegotiationState mGroupNegotiationState;
        private final WifiP2pGroupList mGroups;
        private InactiveState mInactiveState;
        private String mInterfaceName;
        private boolean mIsInterfaceAvailable;
        private boolean mIsWifiEnabled;
        private OngoingGroupRemovalState mOngoingGroupRemovalState;
        private P2pDisabledState mP2pDisabledState;
        private P2pDisablingState mP2pDisablingState;
        private P2pEnabledState mP2pEnabledState;
        private P2pNotSupportedState mP2pNotSupportedState;
        private final WifiP2pDeviceList mPeers;
        private final WifiP2pDeviceList mPeersLostDuringConnection;
        private ProvisionDiscoveryState mProvisionDiscoveryState;
        private WifiP2pConfig mSavedPeerConfig;
        private UserAuthorizingInviteRequestState mUserAuthorizingInviteRequestState;
        private UserAuthorizingJoinState mUserAuthorizingJoinState;
        private UserAuthorizingNegotiationRequestState mUserAuthorizingNegotiationRequestState;
        private WifiInjector mWifiInjector;
        private WifiP2pMonitor mWifiMonitor;
        private WifiP2pNative mWifiNative;
        private final WifiP2pInfo mWifiP2pInfo;

        P2pStateMachine(String str, Looper looper, boolean z) {
            super(str, looper);
            this.mDefaultState = new DefaultState();
            this.mP2pNotSupportedState = new P2pNotSupportedState();
            this.mP2pDisablingState = new P2pDisablingState();
            this.mP2pDisabledState = new P2pDisabledState();
            this.mP2pEnabledState = new P2pEnabledState();
            this.mInactiveState = new InactiveState();
            this.mGroupCreatingState = new GroupCreatingState();
            this.mUserAuthorizingInviteRequestState = new UserAuthorizingInviteRequestState();
            this.mUserAuthorizingNegotiationRequestState = new UserAuthorizingNegotiationRequestState();
            this.mProvisionDiscoveryState = new ProvisionDiscoveryState();
            this.mGroupNegotiationState = new GroupNegotiationState();
            this.mFrequencyConflictState = new FrequencyConflictState();
            this.mGroupCreatedState = new GroupCreatedState();
            this.mUserAuthorizingJoinState = new UserAuthorizingJoinState();
            this.mOngoingGroupRemovalState = new OngoingGroupRemovalState();
            this.mWifiNative = WifiInjector.getInstance().getWifiP2pNative();
            this.mWifiMonitor = WifiInjector.getInstance().getWifiP2pMonitor();
            this.mPeers = new WifiP2pDeviceList();
            this.mPeersLostDuringConnection = new WifiP2pDeviceList();
            this.mGroups = new WifiP2pGroupList((WifiP2pGroupList) null, new WifiP2pGroupList.GroupDeleteListener() {
                public void onDeleteGroup(int i) {
                    P2pStateMachine.this.logd("called onDeleteGroup() netId=" + i);
                    P2pStateMachine.this.mWifiNative.removeP2pNetwork(i);
                    P2pStateMachine.this.mWifiNative.saveConfig();
                    P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
                }
            });
            this.mWifiP2pInfo = new WifiP2pInfo();
            this.mIsInterfaceAvailable = false;
            this.mIsWifiEnabled = false;
            this.mSavedPeerConfig = new WifiP2pConfig();
            addState(this.mDefaultState);
            addState(this.mP2pNotSupportedState, this.mDefaultState);
            addState(this.mP2pDisablingState, this.mDefaultState);
            addState(this.mP2pDisabledState, this.mDefaultState);
            addState(this.mP2pEnabledState, this.mDefaultState);
            addState(this.mInactiveState, this.mP2pEnabledState);
            addState(this.mGroupCreatingState, this.mP2pEnabledState);
            addState(this.mUserAuthorizingInviteRequestState, this.mGroupCreatingState);
            addState(this.mUserAuthorizingNegotiationRequestState, this.mGroupCreatingState);
            addState(this.mProvisionDiscoveryState, this.mGroupCreatingState);
            addState(this.mGroupNegotiationState, this.mGroupCreatingState);
            addState(this.mFrequencyConflictState, this.mGroupCreatingState);
            addState(this.mGroupCreatedState, this.mP2pEnabledState);
            addState(this.mUserAuthorizingJoinState, this.mGroupCreatedState);
            addState(this.mOngoingGroupRemovalState, this.mGroupCreatedState);
            if (z) {
                setInitialState(this.mP2pDisabledState);
            } else {
                setInitialState(this.mP2pNotSupportedState);
            }
            setLogRecSize(50);
            setLogOnlyTransitions(true);
            if (z) {
                WifiP2pServiceImpl.this.mContext.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getIntExtra("wifi_state", 4) == 3) {
                            P2pStateMachine.this.mIsWifiEnabled = true;
                            P2pStateMachine.this.checkAndReEnableP2p();
                        } else {
                            P2pStateMachine.this.mIsWifiEnabled = false;
                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DISABLE_P2P);
                        }
                        P2pStateMachine.this.checkAndSendP2pStateChangedBroadcast();
                    }
                }, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
                this.mWifiNative.registerInterfaceAvailableListener(new HalDeviceManager.InterfaceAvailableForRequestListener() {
                    @Override
                    public final void onAvailabilityChanged(boolean z2) {
                        WifiP2pServiceImpl.P2pStateMachine.lambda$new$0(this.f$0, z2);
                    }
                }, getHandler());
            }
        }

        public static void lambda$new$0(P2pStateMachine p2pStateMachine, boolean z) {
            p2pStateMachine.mIsInterfaceAvailable = z;
            if (z) {
                p2pStateMachine.checkAndReEnableP2p();
            }
            p2pStateMachine.checkAndSendP2pStateChangedBroadcast();
        }

        public void registerForWifiMonitorEvents() {
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147498, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147497, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_DEVICE_LOST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_FIND_STOPPED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_GROUP_STARTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_PBC_RSP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147457, getHandler());
            this.mWifiMonitor.registerHandler(this.mInterfaceName, 147458, getHandler());
            this.mWifiMonitor.startMonitoring(this.mInterfaceName);
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 69632:
                        if (message.arg1 == 0) {
                            P2pStateMachine.this.logd("Full connection with WifiStateMachine established");
                            WifiP2pServiceImpl.this.mWifiChannel = (AsyncChannel) message.obj;
                        } else {
                            P2pStateMachine.this.loge("Full connection failure, error = " + message.arg1);
                            WifiP2pServiceImpl.this.mWifiChannel = null;
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        }
                        return true;
                    case 69633:
                        new WifiAsyncChannel(WifiP2pServiceImpl.TAG).connect(WifiP2pServiceImpl.this.mContext, P2pStateMachine.this.getHandler(), message.replyTo);
                        return true;
                    case 69636:
                        if (message.arg1 == 2) {
                            P2pStateMachine.this.loge("Send failed, client connection lost");
                        } else {
                            P2pStateMachine.this.loge("Client connection lost with reason: " + message.arg1);
                        }
                        WifiP2pServiceImpl.this.mWifiChannel = null;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        return true;
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        return true;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 2);
                        return true;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 2);
                        return true;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 2);
                        return true;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 2);
                        return true;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 2);
                        return true;
                    case 139283:
                        P2pStateMachine.this.replyToMessage(message, 139284, P2pStateMachine.this.getPeers((Bundle) message.obj, message.sendingUid));
                        return true;
                    case 139285:
                        P2pStateMachine.this.replyToMessage(message, 139286, new WifiP2pInfo(P2pStateMachine.this.mWifiP2pInfo));
                        return true;
                    case 139287:
                        P2pStateMachine.this.replyToMessage(message, 139288, P2pStateMachine.this.mGroup != null ? new WifiP2pGroup(P2pStateMachine.this.mGroup) : null);
                        return true;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 2);
                        return true;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 2);
                        return true;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 2);
                        return true;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 2);
                        return true;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 2);
                        return true;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 2);
                        return true;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        return true;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 2);
                        return true;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139318, 2);
                        return true;
                    case 139321:
                        P2pStateMachine.this.replyToMessage(message, 139322, new WifiP2pGroupList(P2pStateMachine.this.mGroups, (WifiP2pGroupList.GroupDeleteListener) null));
                        return true;
                    case 139323:
                        if (!WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139324, 2);
                        }
                        return true;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 2);
                        return true;
                    case 139329:
                    case 139332:
                    case 139335:
                    case 139354:
                    case 139355:
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT:
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT:
                    case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE:
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE:
                    case WifiP2pServiceImpl.ENABLE_P2P:
                    case WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION:
                    case WifiP2pServiceImpl.IPC_POST_DHCP_ACTION:
                    case WifiP2pServiceImpl.IPC_DHCP_RESULTS:
                    case WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS:
                    case WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE:
                    case 147457:
                    case 147458:
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT:
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT:
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT:
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT:
                    case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT:
                    case WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT:
                    case WifiP2pMonitor.P2P_PROV_DISC_FAILURE_EVENT:
                        return true;
                    case 139339:
                    case 139340:
                        P2pStateMachine.this.replyToMessage(message, 139341, (Object) null);
                        return true;
                    case 139342:
                    case 139343:
                        P2pStateMachine.this.replyToMessage(message, 139345, 2);
                        return true;
                    case 139357:
                        P2pStateMachine.this.replyToMessage(message, 139358, 2);
                        return true;
                    case 139361:
                        P2pStateMachine.this.replyToMessage(message, 139361, 2);
                        return true;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                        WifiP2pServiceImpl.this.mDiscoveryBlocked = message.arg1 == 1;
                        WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            if (message.obj == null) {
                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            } else {
                                try {
                                    ((StateMachine) message.obj).sendMessage(message.arg2);
                                } catch (Exception e) {
                                    P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                                }
                            }
                            break;
                        }
                        return true;
                    case WifiP2pServiceImpl.DISABLE_P2P:
                        if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
                        } else {
                            P2pStateMachine.this.loge("Unexpected disable request when WifiChannel is null");
                        }
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                        if (message.obj != null) {
                            P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        } else {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                        }
                        return true;
                    default:
                        P2pStateMachine.this.loge("Unhandled message " + message);
                        return false;
                }
            }
        }

        class P2pNotSupportedState extends State {
            P2pNotSupportedState() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 1);
                        return true;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 1);
                        return true;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 1);
                        return true;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 1);
                        return true;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 1);
                        return true;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 1);
                        return true;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 1);
                        return true;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 1);
                        return true;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 1);
                        return true;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 1);
                        return true;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 1);
                        return true;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 1);
                        return true;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 1);
                        return true;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 1);
                        return true;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139318, 1);
                        return true;
                    case 139323:
                        if (!WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139324, 1);
                        }
                        return true;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 1);
                        return true;
                    case 139329:
                        P2pStateMachine.this.replyToMessage(message, 139330, 1);
                        return true;
                    case 139332:
                        P2pStateMachine.this.replyToMessage(message, 139333, 1);
                        return true;
                    case 139354:
                    case 139355:
                        return true;
                    case 139357:
                        P2pStateMachine.this.replyToMessage(message, 139358, 1);
                        return true;
                    case 139361:
                        P2pStateMachine.this.replyToMessage(message, 139361, 1);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class P2pDisablingState extends State {
            P2pDisablingState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT, WifiP2pServiceImpl.access$1804(), 0), 5000L);
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT:
                        if (WifiP2pServiceImpl.sDisableP2pTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.loge("P2p disable timed out");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                            return true;
                        }
                        return true;
                    case WifiP2pServiceImpl.ENABLE_P2P:
                    case WifiP2pServiceImpl.DISABLE_P2P:
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case 147458:
                        P2pStateMachine.this.logd("p2p socket connection lost");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
                } else {
                    P2pStateMachine.this.loge("P2pDisablingState exit(): WifiChannel is null");
                }
            }
        }

        class P2pDisabledState extends State {
            P2pDisabledState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                if (message.what == 143376) {
                    if (!P2pStateMachine.this.mIsWifiEnabled) {
                        Log.e(WifiP2pServiceImpl.TAG, "Ignore P2P enable since wifi is disabled");
                        return true;
                    }
                    P2pStateMachine.this.mInterfaceName = P2pStateMachine.this.mWifiNative.setupInterface(new HalDeviceManager.InterfaceDestroyedListener() {
                        @Override
                        public final void onDestroyed(String str) {
                            WifiP2pServiceImpl.P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DISABLE_P2P);
                        }
                    }, P2pStateMachine.this.getHandler());
                    if (P2pStateMachine.this.mInterfaceName != null) {
                        try {
                            WifiP2pServiceImpl.this.mNwService.setInterfaceUp(P2pStateMachine.this.mInterfaceName);
                        } catch (RemoteException e) {
                            P2pStateMachine.this.loge("Unable to change interface settings: " + e);
                        } catch (IllegalStateException e2) {
                            P2pStateMachine.this.loge("Unable to change interface settings: " + e2);
                        }
                        P2pStateMachine.this.registerForWifiMonitorEvents();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    }
                    Log.e(WifiP2pServiceImpl.TAG, "Failed to setup interface for P2P");
                    return true;
                }
                return false;
            }
        }

        class P2pEnabledState extends State {
            P2pEnabledState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(true);
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                P2pStateMachine.this.initializeP2pSettings();
            }

            public boolean processMessage(Message message) {
                boolean zP2pFind;
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139265:
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        } else {
                            P2pStateMachine.this.clearSupplicantServiceRequest();
                            if (P2pStateMachine.this.isWfdSinkEnabled()) {
                                P2pStateMachine.this.p2pConfigWfdSink();
                                zP2pFind = P2pStateMachine.this.mWifiNative.p2pFind();
                            } else {
                                zP2pFind = P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S);
                            }
                            if (zP2pFind) {
                                P2pStateMachine.this.replyToMessage(message, 139267);
                                P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(true);
                            } else {
                                P2pStateMachine.this.replyToMessage(message, 139266, 0);
                            }
                        }
                        return true;
                    case 139268:
                        if (P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.replyToMessage(message, 139270);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139269, 0);
                        }
                        if (P2pStateMachine.this.isWfdSinkEnabled()) {
                            P2pStateMachine.this.p2pUnconfigWfdSink();
                        }
                        return true;
                    case 139292:
                        P2pStateMachine.this.logd(getName() + " add service");
                        if (P2pStateMachine.this.addLocalService(message.replyTo, (WifiP2pServiceInfo) message.obj)) {
                            P2pStateMachine.this.replyToMessage(message, 139294);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139293);
                        }
                        return true;
                    case 139295:
                        P2pStateMachine.this.logd(getName() + " remove service");
                        P2pStateMachine.this.removeLocalService(message.replyTo, (WifiP2pServiceInfo) message.obj);
                        P2pStateMachine.this.replyToMessage(message, 139297);
                        return true;
                    case 139298:
                        P2pStateMachine.this.logd(getName() + " clear service");
                        P2pStateMachine.this.clearLocalServices(message.replyTo);
                        P2pStateMachine.this.replyToMessage(message, 139300);
                        return true;
                    case 139301:
                        P2pStateMachine.this.logd(getName() + " add service request");
                        if (!P2pStateMachine.this.addServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj)) {
                            P2pStateMachine.this.replyToMessage(message, 139302);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139303);
                        }
                        return true;
                    case 139304:
                        P2pStateMachine.this.logd(getName() + " remove service request");
                        P2pStateMachine.this.removeServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj);
                        P2pStateMachine.this.replyToMessage(message, 139306);
                        return true;
                    case 139307:
                        P2pStateMachine.this.logd(getName() + " clear service request");
                        P2pStateMachine.this.clearServiceRequests(message.replyTo);
                        P2pStateMachine.this.replyToMessage(message, 139309);
                        return true;
                    case 139310:
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        } else {
                            P2pStateMachine.this.logd(getName() + " discover services");
                            if (!P2pStateMachine.this.updateSupplicantServiceRequest()) {
                                P2pStateMachine.this.replyToMessage(message, 139311, 3);
                            } else if (P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S)) {
                                P2pStateMachine.this.replyToMessage(message, 139312);
                            } else {
                                P2pStateMachine.this.replyToMessage(message, 139311, 0);
                            }
                        }
                        return true;
                    case 139315:
                        WifiP2pDevice wifiP2pDevice = (WifiP2pDevice) message.obj;
                        if (wifiP2pDevice == null || !P2pStateMachine.this.setAndPersistDeviceName(wifiP2pDevice.deviceName)) {
                            P2pStateMachine.this.replyToMessage(message, 139316, 0);
                        } else {
                            P2pStateMachine.this.logd("set device name " + wifiP2pDevice.deviceName);
                            P2pStateMachine.this.replyToMessage(message, 139317);
                        }
                        return true;
                    case 139318:
                        P2pStateMachine.this.logd(getName() + " delete persistent group");
                        P2pStateMachine.this.mGroups.remove(message.arg1);
                        P2pStateMachine.this.replyToMessage(message, 139320);
                        return true;
                    case 139323:
                        WifiP2pWfdInfo wifiP2pWfdInfo = (WifiP2pWfdInfo) message.obj;
                        if (WifiP2pServiceImpl.this.getWfdPermission(message.sendingUid) && wifiP2pWfdInfo != null && P2pStateMachine.this.setWfdInfo(wifiP2pWfdInfo)) {
                            P2pStateMachine.this.replyToMessage(message, 139325);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                        }
                        return true;
                    case 139329:
                        P2pStateMachine.this.logd(getName() + " start listen mode");
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139331);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139330);
                        }
                        return true;
                    case 139332:
                        P2pStateMachine.this.logd(getName() + " stop listen mode");
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(false, 0, 0)) {
                            P2pStateMachine.this.replyToMessage(message, 139334);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139333);
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        return true;
                    case 139335:
                        Bundle bundle = (Bundle) message.obj;
                        int i = bundle.getInt("lc", 0);
                        int i2 = bundle.getInt("oc", 0);
                        P2pStateMachine.this.logd(getName() + " set listen and operating channel");
                        if (P2pStateMachine.this.mWifiNative.p2pSetChannel(i, i2)) {
                            P2pStateMachine.this.replyToMessage(message, 139337);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139336);
                        }
                        return true;
                    case 139339:
                        Bundle bundle2 = new Bundle();
                        bundle2.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverRequest());
                        P2pStateMachine.this.replyToMessage(message, 139341, bundle2);
                        return true;
                    case 139340:
                        Bundle bundle3 = new Bundle();
                        bundle3.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverSelect());
                        P2pStateMachine.this.replyToMessage(message, 139341, bundle3);
                        return true;
                    case 139361:
                        P2pStateMachine.this.logd(getName() + " ADD_PERSISTENT_GROUP");
                        HashMap map = (HashMap) ((Bundle) message.obj).getSerializable("variables");
                        if (map != null) {
                            P2pStateMachine.this.replyToMessage(message, 139364, new WifiP2pGroup(P2pStateMachine.this.addPersistentGroup(map)));
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139362, 0);
                        }
                        return true;
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE:
                        P2pStateMachine.this.mWifiNative.setMiracastMode(message.arg1);
                        return true;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                        boolean z = message.arg1 == 1;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked != z) {
                            WifiP2pServiceImpl.this.mDiscoveryBlocked = z;
                            if (z && WifiP2pServiceImpl.this.mDiscoveryStarted) {
                                P2pStateMachine.this.mWifiNative.p2pStopFind();
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = true;
                            }
                            if (!z && WifiP2pServiceImpl.this.mDiscoveryPostponed) {
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                                P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S);
                            }
                            if (z) {
                                if (message.obj == null) {
                                    Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                                } else {
                                    try {
                                        ((StateMachine) message.obj).sendMessage(message.arg2);
                                    } catch (Exception e) {
                                        P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                                    }
                                }
                            }
                            break;
                        }
                        return true;
                    case WifiP2pServiceImpl.ENABLE_P2P:
                        return true;
                    case WifiP2pServiceImpl.DISABLE_P2P:
                        if (P2pStateMachine.this.mPeers.clear()) {
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        if (P2pStateMachine.this.mGroups.clear()) {
                            P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
                        }
                        P2pStateMachine.this.mWifiMonitor.stopMonitoring(P2pStateMachine.this.mInterfaceName);
                        P2pStateMachine.this.mWifiNative.teardownInterface();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisablingState);
                        return true;
                    case 147458:
                        P2pStateMachine.this.loge("Unexpected loss of p2p socket connection");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        return true;
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        } else {
                            WifiP2pDevice wifiP2pDevice2 = (WifiP2pDevice) message.obj;
                            if (!WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(wifiP2pDevice2.deviceAddress)) {
                                P2pStateMachine.this.mPeers.updateSupplicantDetails(wifiP2pDevice2);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                            }
                        }
                        return true;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        } else if (P2pStateMachine.this.mPeers.remove(((WifiP2pDevice) message.obj).deviceAddress) != null) {
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        return true;
                    case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT:
                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                        return true;
                    case WifiP2pMonitor.P2P_SERV_DISC_RESP_EVENT:
                        P2pStateMachine.this.logd(getName() + " receive service response");
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        } else {
                            for (WifiP2pServiceResponse wifiP2pServiceResponse : (List) message.obj) {
                                wifiP2pServiceResponse.setSrcDevice(P2pStateMachine.this.mPeers.get(wifiP2pServiceResponse.getSrcDevice().deviceAddress));
                                P2pStateMachine.this.sendServiceResponse(wifiP2pServiceResponse);
                            }
                        }
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
                P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(false);
            }
        }

        class InactiveState extends State {
            InactiveState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.mSavedPeerConfig.invalidate();
            }

            public boolean processMessage(Message message) {
                boolean zP2pGroupAdd;
                String string;
                WifiP2pConfig wifiP2pConfig;
                WifiP2pDevice wifiP2pDevice;
                String ownerAddr;
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139268:
                        if (P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.mWifiNative.p2pFlush();
                            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
                            P2pStateMachine.this.replyToMessage(message, 139270);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139269, 0);
                        }
                        return true;
                    case 139271:
                        P2pStateMachine.this.logd(getName() + " sending connect");
                        WifiP2pConfig wifiP2pConfig2 = (WifiP2pConfig) message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(wifiP2pConfig2)) {
                            WifiP2pServiceImpl.this.mAutonomousGroup = false;
                            P2pStateMachine.this.mWifiNative.p2pStopFind();
                            WifiP2pServiceImpl.this.mConnectToPeer = true;
                            if ((WifiP2pServiceImpl.this.mMiracastMode != 1 || WifiP2pServiceImpl.WFD_DONGLE_USE_P2P_INVITE.booleanValue()) && P2pStateMachine.this.reinvokePersistentGroup(wifiP2pConfig2)) {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            } else {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mProvisionDiscoveryState);
                            }
                            P2pStateMachine.this.mSavedPeerConfig = wifiP2pConfig2;
                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            P2pStateMachine.this.replyToMessage(message, 139273);
                        } else {
                            P2pStateMachine.this.loge("Dropping connect requeset " + wifiP2pConfig2);
                            P2pStateMachine.this.replyToMessage(message, 139272);
                        }
                        return true;
                    case 139277:
                        WifiP2pServiceImpl.this.mAutonomousGroup = true;
                        int networkId = message.arg1;
                        if (networkId == -2) {
                            networkId = P2pStateMachine.this.mGroups.getNetworkId(WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                            zP2pGroupAdd = networkId != -1 ? P2pStateMachine.this.mWifiNative.p2pGroupAdd(networkId) : P2pStateMachine.this.mWifiNative.p2pGroupAdd(true);
                        } else {
                            zP2pGroupAdd = false;
                        }
                        if (networkId <= -1 || !P2pStateMachine.this.mGroups.contains(networkId)) {
                            zP2pGroupAdd = P2pStateMachine.this.mWifiNative.p2pGroupAdd(false);
                        } else if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(P2pStateMachine.this.mGroups.getOwnerAddr(networkId))) {
                            zP2pGroupAdd = P2pStateMachine.this.mWifiNative.p2pGroupAdd(networkId);
                        }
                        if (zP2pGroupAdd) {
                            P2pStateMachine.this.replyToMessage(message, 139279);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139278, 0);
                        }
                        return true;
                    case 139329:
                        P2pStateMachine.this.logd(getName() + " start listen mode");
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139331);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139330);
                        }
                        return true;
                    case 139332:
                        P2pStateMachine.this.logd(getName() + " stop listen mode");
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(false, 0, 0)) {
                            P2pStateMachine.this.replyToMessage(message, 139334);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139333);
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        return true;
                    case 139335:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments(s)");
                        } else {
                            Bundle bundle = (Bundle) message.obj;
                            int i = bundle.getInt("lc", 0);
                            int i2 = bundle.getInt("oc", 0);
                            P2pStateMachine.this.logd(getName() + " set listen and operating channel");
                            if (P2pStateMachine.this.mWifiNative.p2pSetChannel(i, i2)) {
                                P2pStateMachine.this.replyToMessage(message, 139337);
                            } else {
                                P2pStateMachine.this.replyToMessage(message, 139336);
                            }
                        }
                        return true;
                    case 139342:
                        string = message.obj != null ? ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE") : null;
                        if (string == null || !P2pStateMachine.this.mWifiNative.initiatorReportNfcHandover(string)) {
                            P2pStateMachine.this.replyToMessage(message, 139345);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139344);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                        }
                        return true;
                    case 139343:
                        string = message.obj != null ? ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE") : null;
                        if (string == null || !P2pStateMachine.this.mWifiNative.responderReportNfcHandover(string)) {
                            P2pStateMachine.this.replyToMessage(message, 139345);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139344);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                        }
                        return true;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT:
                        WifiP2pConfig wifiP2pConfig3 = (WifiP2pConfig) message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(wifiP2pConfig3)) {
                            P2pStateMachine.this.mSavedPeerConfig = wifiP2pConfig3;
                            WifiP2pServiceImpl.this.mAutonomousGroup = false;
                            WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                        } else {
                            P2pStateMachine.this.loge("Dropping GO neg request " + wifiP2pConfig3);
                        }
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                        if (message.obj != null) {
                            P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            P2pStateMachine.this.logd(getName() + " group started");
                            if (P2pStateMachine.this.mGroup.isGroupOwner() && WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(P2pStateMachine.this.mGroup.getOwner().deviceAddress)) {
                                P2pStateMachine.this.mGroup.getOwner().deviceAddress = WifiP2pServiceImpl.this.mThisDevice.deviceAddress;
                            }
                            if (P2pStateMachine.this.mGroup.getNetworkId() == -2) {
                                WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                P2pStateMachine.this.deferMessage(message);
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            } else {
                                P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                                P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                            }
                        } else {
                            Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                        }
                        return true;
                    case WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                        } else {
                            WifiP2pGroup wifiP2pGroup = (WifiP2pGroup) message.obj;
                            WifiP2pDevice owner = wifiP2pGroup.getOwner();
                            if (owner == null) {
                                int networkId2 = wifiP2pGroup.getNetworkId();
                                if (networkId2 >= 0 && (ownerAddr = P2pStateMachine.this.mGroups.getOwnerAddr(networkId2)) != null) {
                                    wifiP2pGroup.setOwner(new WifiP2pDevice(ownerAddr));
                                    owner = wifiP2pGroup.getOwner();
                                    wifiP2pConfig = new WifiP2pConfig();
                                    wifiP2pConfig.deviceAddress = wifiP2pGroup.getOwner().deviceAddress;
                                    if (P2pStateMachine.this.isConfigInvalid(wifiP2pConfig)) {
                                    }
                                } else {
                                    P2pStateMachine.this.loge("Ignored invitation from null owner");
                                }
                            } else {
                                wifiP2pConfig = new WifiP2pConfig();
                                wifiP2pConfig.deviceAddress = wifiP2pGroup.getOwner().deviceAddress;
                                if (P2pStateMachine.this.isConfigInvalid(wifiP2pConfig)) {
                                    P2pStateMachine.this.mSavedPeerConfig = wifiP2pConfig;
                                    if (owner != null && (wifiP2pDevice = P2pStateMachine.this.mPeers.get(owner.deviceAddress)) != null) {
                                        if (wifiP2pDevice.wpsPbcSupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                        } else if (wifiP2pDevice.wpsKeypadSupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                                        } else if (wifiP2pDevice.wpsDisplaySupported()) {
                                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                        }
                                    }
                                    WifiP2pServiceImpl.this.mAutonomousGroup = false;
                                    WifiP2pServiceImpl.this.mJoinExistingGroup = true;
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingInviteRequestState);
                                } else {
                                    P2pStateMachine.this.loge("Dropping invitation request " + wifiP2pConfig);
                                }
                            }
                        }
                        return true;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                        return true;
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        } else {
                            WifiP2pProvDiscEvent wifiP2pProvDiscEvent = (WifiP2pProvDiscEvent) message.obj;
                            WifiP2pDevice wifiP2pDevice2 = wifiP2pProvDiscEvent.device;
                            if (wifiP2pDevice2 != null) {
                                P2pStateMachine.this.notifyP2pProvDiscShowPinRequest(wifiP2pProvDiscEvent.pin, wifiP2pDevice2.deviceAddress);
                                P2pStateMachine.this.mPeers.updateStatus(wifiP2pDevice2.deviceAddress, 1);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            } else {
                                P2pStateMachine.this.loge("Device entry is null");
                            }
                        }
                        return true;
                    default:
                        return false;
                }
            }
        }

        class GroupCreatingState extends State {
            GroupCreatingState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT, WifiP2pServiceImpl.access$6204(), 0), 120000L);
                WifiP2pServiceImpl.this.mP2pOperFreq = -1;
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        break;
                    case 139268:
                        P2pStateMachine.this.logd("defer STOP_DISCOVERY@GroupCreatingState");
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case 139274:
                        boolean z = P2pStateMachine.this.mWifiNative.p2pCancelConnect() || P2pStateMachine.this.mWifiNative.p2pGroupRemove(WifiP2pServiceImpl.this.mInterface);
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        if (z) {
                            P2pStateMachine.this.replyToMessage(message, 139276);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139275);
                        }
                        break;
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT:
                        if (WifiP2pServiceImpl.sGroupCreatingTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.logd("Group negotiation timed out");
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        }
                        break;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                        P2pStateMachine.this.logd("defer BLOCK_DISCOVERY@GroupCreatingState");
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT:
                        WifiP2pDevice wifiP2pDevice = (WifiP2pDevice) message.obj;
                        if (!WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
                            if (P2pStateMachine.this.mSavedPeerConfig != null && P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
                                wifiP2pDevice.status = 1;
                            }
                            P2pStateMachine.this.mPeers.update(wifiP2pDevice);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        break;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        } else {
                            WifiP2pDevice wifiP2pDevice2 = (WifiP2pDevice) message.obj;
                            if (!P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(wifiP2pDevice2.deviceAddress)) {
                                P2pStateMachine.this.logd("mSavedPeerConfig " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress + "device " + wifiP2pDevice2.deviceAddress);
                                return false;
                            }
                            P2pStateMachine.this.logd("Add device to lost list " + wifiP2pDevice2);
                            P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(wifiP2pDevice2);
                        }
                        break;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                        WifiP2pServiceImpl.this.mAutonomousGroup = false;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case WifiP2pMonitor.P2P_FIND_STOPPED_EVENT:
                        P2pStateMachine.this.logd("defer P2P_FIND_STOPPED_EVENT@GroupCreatingState");
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }

        class UserAuthorizingNegotiationRequestState extends State {
            UserAuthorizingNegotiationRequestState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                if (P2pStateMachine.this.isWfdSinkEnabled()) {
                    P2pStateMachine.this.sendP2pGOandGCRequestConnectBroadcast();
                } else {
                    P2pStateMachine.this.notifyInvitationReceived();
                }
            }

            public boolean processMessage(Message message) {
                WifiInfo wifiConnectionInfo;
                WifiP2pDevice wifiP2pDevice;
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139354:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        if (WifiP2pServiceImpl.this.mNegoChannelConflict) {
                            WifiP2pServiceImpl.this.mNegoChannelConflict = false;
                            P2pStateMachine.this.logd("PEER_CONNECTION_USER_ACCEPT_FROM_OUTER,switch to FrequencyConflictState");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                        } else {
                            P2pStateMachine.this.logd("isWfdSinkEnabled()=" + P2pStateMachine.this.isWfdSinkEnabled());
                            if (P2pStateMachine.this.isWfdSinkEnabled() && (wifiConnectionInfo = P2pStateMachine.this.getWifiConnectionInfo()) != null) {
                                P2pStateMachine.this.logd("wifiInfo=" + wifiConnectionInfo);
                                if (wifiConnectionInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                                    P2pStateMachine.this.logd("wifiInfo.getSupplicantState() == SupplicantState.COMPLETED");
                                    P2pStateMachine.this.logd("wifiInfo.getFrequency()=" + wifiConnectionInfo.getFrequency());
                                    P2pStateMachine.this.mSavedPeerConfig.setPreferOperFreq(wifiConnectionInfo.getFrequency());
                                }
                            }
                            P2pStateMachine.this.mWifiNative.p2pStopFind();
                            P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        }
                        return true;
                    case 139355:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                        P2pStateMachine.this.logd("User rejected negotiation " + P2pStateMachine.this.mSavedPeerConfig);
                        if (P2pStateMachine.this.mSavedPeerConfig != null && (wifiP2pDevice = P2pStateMachine.this.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) != null && wifiP2pDevice.status == 1) {
                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 3);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
            }
        }

        class UserAuthorizingInviteRequestState extends State {
            UserAuthorizingInviteRequestState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                if (!P2pStateMachine.this.isWfdSinkEnabled()) {
                    P2pStateMachine.this.notifyInvitationReceived();
                } else {
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139354:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (!P2pStateMachine.this.reinvokePersistentGroup(P2pStateMachine.this.mSavedPeerConfig)) {
                            P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        }
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        return true;
                    case 139355:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                        P2pStateMachine.this.logd("User rejected invitation " + P2pStateMachine.this.mSavedPeerConfig);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
            }
        }

        class ProvisionDiscoveryState extends State {
            ProvisionDiscoveryState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.mWifiNative.p2pProvisionDiscovery(P2pStateMachine.this.mSavedPeerConfig);
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                int i = message.what;
                if (i == 139274) {
                    boolean zP2pGroupRemove = P2pStateMachine.this.mWifiNative.p2pGroupRemove(WifiP2pServiceImpl.this.mInterface);
                    P2pStateMachine.this.handleGroupCreationFailure();
                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                    if (zP2pGroupRemove) {
                        P2pStateMachine.this.replyToMessage(message, 139276);
                    } else {
                        P2pStateMachine.this.replyToMessage(message, 139275);
                    }
                } else if (i != 147495) {
                    switch (i) {
                        case WifiP2pMonitor.P2P_PROV_DISC_PBC_RSP_EVENT:
                            if (message.obj == null) {
                                Log.e(WifiP2pServiceImpl.TAG, "Invalid argument(s)");
                            } else {
                                WifiP2pDevice wifiP2pDevice = ((WifiP2pProvDiscEvent) message.obj).device;
                                if ((wifiP2pDevice == null || wifiP2pDevice.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                                    P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                    P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                }
                            }
                            break;
                        case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                            if (message.obj == null) {
                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            } else {
                                WifiP2pDevice wifiP2pDevice2 = ((WifiP2pProvDiscEvent) message.obj).device;
                                if ((wifiP2pDevice2 == null || wifiP2pDevice2.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 2) {
                                    P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                    if (TextUtils.isEmpty(P2pStateMachine.this.mSavedPeerConfig.wps.pin)) {
                                        WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                                    } else {
                                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                    }
                                }
                            }
                            break;
                        case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                            if (message.obj == null) {
                                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            } else {
                                WifiP2pProvDiscEvent wifiP2pProvDiscEvent = (WifiP2pProvDiscEvent) message.obj;
                                WifiP2pDevice wifiP2pDevice3 = wifiP2pProvDiscEvent.device;
                                if (wifiP2pDevice3 != null) {
                                    if (wifiP2pDevice3.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 1) {
                                        P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                        P2pStateMachine.this.mSavedPeerConfig.wps.pin = wifiP2pProvDiscEvent.pin;
                                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                        P2pStateMachine.this.notifyInvitationSent(wifiP2pProvDiscEvent.pin, wifiP2pDevice3.deviceAddress);
                                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                    }
                                } else {
                                    Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                                }
                            }
                            break;
                        default:
                            return false;
                    }
                } else {
                    P2pStateMachine.this.loge("provision discovery failed");
                    P2pStateMachine.this.handleGroupCreationFailure();
                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                }
                return true;
            }
        }

        class GroupNegotiationState extends State {
            GroupNegotiationState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT:
                        P2pStateMachine.this.logd(getName() + " go success");
                        return true;
                    case WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                        if (((P2pStatus) message.obj) == P2pStatus.NO_COMMON_CHANNEL) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                        } else {
                            P2pStateMachine.this.logd(getName() + " go failure");
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        }
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                        if (((P2pStatus) message.obj) == P2pStatus.NO_COMMON_CHANNEL) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                        }
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        } else {
                            P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                            P2pStateMachine.this.logd(getName() + " group started");
                            if (P2pStateMachine.this.mGroup.isGroupOwner() && WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS.equals(P2pStateMachine.this.mGroup.getOwner().deviceAddress)) {
                                P2pStateMachine.this.mGroup.getOwner().deviceAddress = WifiP2pServiceImpl.this.mThisDevice.deviceAddress;
                            }
                            if (P2pStateMachine.this.mGroup.getNetworkId() == -2) {
                                P2pStateMachine.this.updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                                P2pStateMachine.this.mGroup.setNetworkId(P2pStateMachine.this.mGroups.getNetworkId(P2pStateMachine.this.mGroup.getOwner().deviceAddress, P2pStateMachine.this.mGroup.getNetworkName()));
                                if (WifiP2pServiceImpl.this.mMiracastMode == 1 && !WifiP2pServiceImpl.WFD_DONGLE_USE_P2P_INVITE.booleanValue()) {
                                    WifiP2pServiceImpl.this.mWfdSourceAddr = P2pStateMachine.this.mGroup.getOwner().deviceAddress;
                                    P2pStateMachine.this.logd("wfd source case: mWfdSourceAddr = " + WifiP2pServiceImpl.this.mWfdSourceAddr);
                                }
                            }
                            if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                                if (!WifiP2pServiceImpl.this.mAutonomousGroup) {
                                    P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                                }
                                P2pStateMachine.this.startDhcpServer(P2pStateMachine.this.mGroup.getInterface());
                            } else {
                                P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                                WifiP2pServiceImpl.this.startIpClient(P2pStateMachine.this.mGroup.getInterface());
                                WifiP2pDevice owner = P2pStateMachine.this.mGroup.getOwner();
                                WifiP2pDevice wifiP2pDevice = P2pStateMachine.this.mPeers.get(owner.deviceAddress);
                                if (wifiP2pDevice != null) {
                                    owner.updateSupplicantDetails(wifiP2pDevice);
                                    P2pStateMachine.this.mPeers.updateStatus(owner.deviceAddress, 0);
                                    P2pStateMachine.this.sendPeersChangedBroadcast();
                                } else {
                                    P2pStateMachine.this.logw("Unknown group owner " + owner);
                                }
                            }
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        }
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT:
                        break;
                    case WifiP2pMonitor.P2P_INVITATION_RECEIVED_EVENT:
                    default:
                        return false;
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT:
                        P2pStatus p2pStatus = (P2pStatus) message.obj;
                        if (p2pStatus != P2pStatus.SUCCESS) {
                            P2pStateMachine.this.loge("Invitation result " + p2pStatus);
                            if (p2pStatus == P2pStatus.UNKNOWN_P2P_GROUP) {
                                int i = P2pStateMachine.this.mSavedPeerConfig.netId;
                                if (i >= 0) {
                                    P2pStateMachine.this.logd("Remove unknown client from the list");
                                    P2pStateMachine.this.removeClientFromList(i, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, true);
                                }
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            } else if (p2pStatus == P2pStatus.INFORMATION_IS_CURRENTLY_UNAVAILABLE) {
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            } else if (p2pStatus == P2pStatus.NO_COMMON_CHANNEL) {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                            } else {
                                P2pStateMachine.this.handleGroupCreationFailure();
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            }
                        }
                        return true;
                }
            }
        }

        class FrequencyConflictState extends State {
            private AlertDialog mFrequencyConflictDialog;

            FrequencyConflictState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                if (WifiP2pServiceImpl.this.mMccSupport || WifiP2pServiceImpl.this.mMiracastMode != 1) {
                    if (WifiP2pServiceImpl.this.mMiracastMode != 2) {
                        if (WifiP2pServiceImpl.this.mMccSupport) {
                            if (WifiP2pServiceImpl.this.mConnectToPeer) {
                                P2pStateMachine.this.logd(getName() + " SCC->MCC, mConnectToPeer=" + WifiP2pServiceImpl.this.mConnectToPeer + "\tP2pOperFreq=" + WifiP2pServiceImpl.this.mP2pOperFreq);
                                P2pStateMachine.this.mSavedPeerConfig.setPreferOperFreq(WifiP2pServiceImpl.this.mP2pOperFreq);
                                if (!P2pStateMachine.this.reinvokePersistentGroup(P2pStateMachine.this.mSavedPeerConfig)) {
                                    P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                }
                            } else {
                                P2pStateMachine.this.logd(getName() + " SCC->MCC, mConnectToPeer=" + WifiP2pServiceImpl.this.mConnectToPeer + "\tdo p2p_connect/p2p_invite again!");
                                WifiP2pServiceImpl.this.mP2pOperFreq = -1;
                                P2pStateMachine.this.mSavedPeerConfig.setPreferOperFreq(WifiP2pServiceImpl.this.mP2pOperFreq);
                                if (!P2pStateMachine.this.reinvokePersistentGroup(P2pStateMachine.this.mSavedPeerConfig)) {
                                    P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                }
                            }
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            return;
                        }
                        notifyFrequencyConflict();
                        return;
                    }
                    P2pStateMachine.this.logd("[sink] channel conflict, disconnecting wifi by app layer");
                    P2pStateMachine.this.sendMessage(139356, 1);
                    return;
                }
                P2pStateMachine.this.sendP2pOPChannelBroadcast();
            }

            private void notifyFrequencyConflict() {
                P2pStateMachine.this.logd("Notify frequency conflict");
                Resources system = Resources.getSystem();
                AlertDialog alertDialogCreate = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setMessage(system.getString(R.string.notification_channel_voice_mail, P2pStateMachine.this.getDeviceName(P2pStateMachine.this.mSavedPeerConfig.deviceAddress))).setPositiveButton(system.getString(R.string.bugreport_option_interactive_title), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT);
                    }
                }).setNegativeButton(system.getString(R.string.badPuk), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).create();
                alertDialogCreate.setCanceledOnTouchOutside(false);
                alertDialogCreate.getWindow().setType(2003);
                WindowManager.LayoutParams attributes = alertDialogCreate.getWindow().getAttributes();
                attributes.privateFlags = 16;
                alertDialogCreate.getWindow().setAttributes(attributes);
                alertDialogCreate.show();
                this.mFrequencyConflictDialog = alertDialogCreate;
            }

            private void notifyFrequencyConflictEx() {
                P2pStateMachine.this.logd("Notify frequency conflict enhancement! mP2pOperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
                Resources system = Resources.getSystem();
                String string = "";
                if (WifiP2pServiceImpl.this.mP2pOperFreq > 0) {
                    if (WifiP2pServiceImpl.this.mP2pOperFreq < 5000) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("2.4G band-");
                        sb.append(new String("" + WifiP2pServiceImpl.this.mP2pOperFreq));
                        sb.append(" MHz");
                        string = sb.toString();
                    } else {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("5G band-");
                        sb2.append(new String("" + WifiP2pServiceImpl.this.mP2pOperFreq));
                        sb2.append(" MHz");
                        string = sb2.toString();
                    }
                } else {
                    P2pStateMachine.this.loge(getName() + " in-valid OP channel: " + WifiP2pServiceImpl.this.mP2pOperFreq);
                }
                AlertDialog alertDialogCreate = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setMessage(system.getString(134545665, P2pStateMachine.this.getDeviceName(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), string)).setPositiveButton(system.getString(R.string.bugreport_option_interactive_title), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT);
                    }
                }).setNegativeButton(system.getString(R.string.badPuk), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).create();
                alertDialogCreate.getWindow().setType(2003);
                alertDialogCreate.show();
                this.mFrequencyConflictDialog = alertDialogCreate;
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                int i = message.what;
                if (i == 139356) {
                    int i2 = message.arg1;
                    P2pStateMachine.this.logd(getName() + " frequency confliect enhancement decision: " + i2 + ", and mP2pOperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
                    if (1 == i2) {
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        P2pStateMachine.this.mSavedPeerConfig.setPreferOperFreq(WifiP2pServiceImpl.this.mP2pOperFreq);
                        P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                    } else {
                        notifyFrequencyConflictEx();
                    }
                } else if (i != 143373) {
                    switch (i) {
                        case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT:
                            if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 1);
                            } else {
                                P2pStateMachine.this.loge("DROP_WIFI_USER_ACCEPT message received when WifiChannel is null");
                            }
                            WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = true;
                            break;
                        case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT:
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            break;
                        default:
                            switch (i) {
                                case WifiP2pMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                                case WifiP2pMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT:
                                    P2pStateMachine.this.loge(getName() + "group sucess during freq conflict!");
                                    break;
                                case WifiP2pMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                                case WifiP2pMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                                case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT:
                                    break;
                                case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                                    P2pStateMachine.this.loge(getName() + "group started after freq conflict, handle anyway");
                                    P2pStateMachine.this.deferMessage(message);
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                    break;
                                default:
                                    return false;
                            }
                            break;
                    }
                } else {
                    P2pStateMachine.this.logd(getName() + "Wifi disconnected, retry p2p");
                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                    WifiP2pServiceImpl.this.mP2pOperFreq = -1;
                    P2pStateMachine.this.mSavedPeerConfig.setPreferOperFreq(WifiP2pServiceImpl.this.mP2pOperFreq);
                    P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                }
                return true;
            }

            public void exit() {
                if (this.mFrequencyConflictDialog != null) {
                    this.mFrequencyConflictDialog.dismiss();
                }
            }
        }

        class GroupCreatedState extends State {
            GroupCreatedState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.mSavedPeerConfig.invalidate();
                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                P2pStateMachine.this.updateThisDevice(0);
                if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                    P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS));
                } else if (P2pStateMachine.this.isWfdSinkConnected()) {
                    P2pStateMachine.this.logd(getName() + " [wfd sink] stop scan@GC, to avoid packet lost");
                    P2pStateMachine.this.mWifiNative.p2pStopFind();
                }
                if (WifiP2pServiceImpl.this.mAutonomousGroup) {
                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                }
            }

            public boolean processMessage(Message message) {
                boolean zStartWpsPinKeypad;
                int networkId;
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.clearSupplicantServiceRequest();
                        if (P2pStateMachine.this.mWifiNative.p2pFind(25)) {
                            P2pStateMachine.this.replyToMessage(message, 139267);
                            P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(true);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139266, 0);
                        }
                        return true;
                    case 139271:
                        WifiP2pConfig wifiP2pConfig = (WifiP2pConfig) message.obj;
                        if (P2pStateMachine.this.isConfigInvalid(wifiP2pConfig)) {
                            P2pStateMachine.this.loge("Dropping connect request " + wifiP2pConfig);
                            P2pStateMachine.this.replyToMessage(message, 139272);
                        } else {
                            P2pStateMachine.this.logd("Inviting device : " + wifiP2pConfig.deviceAddress);
                            P2pStateMachine.this.mSavedPeerConfig = wifiP2pConfig;
                            WifiP2pServiceImpl.this.mConnectToPeer = true;
                            if (P2pStateMachine.this.mWifiNative.p2pInvite(P2pStateMachine.this.mGroup, wifiP2pConfig.deviceAddress)) {
                                P2pStateMachine.this.mPeers.updateStatus(wifiP2pConfig.deviceAddress, 1);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                P2pStateMachine.this.replyToMessage(message, 139273);
                            } else {
                                P2pStateMachine.this.replyToMessage(message, 139272, 0);
                            }
                        }
                        return true;
                    case 139280:
                        P2pStateMachine.this.logd(getName() + " remove group");
                        if (P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface())) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mOngoingGroupRemovalState);
                            P2pStateMachine.this.replyToMessage(message, 139282);
                        } else {
                            P2pStateMachine.this.handleGroupRemoved();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            P2pStateMachine.this.replyToMessage(message, 139281, 0);
                        }
                        return true;
                    case 139326:
                        WpsInfo wpsInfo = (WpsInfo) message.obj;
                        if (wpsInfo == null) {
                            P2pStateMachine.this.replyToMessage(message, 139327);
                        } else {
                            if (wpsInfo.setup == 0) {
                                zStartWpsPinKeypad = P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                            } else if (wpsInfo.pin != null) {
                                zStartWpsPinKeypad = P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), wpsInfo.pin);
                            } else {
                                String strStartWpsPinDisplay = P2pStateMachine.this.mWifiNative.startWpsPinDisplay(P2pStateMachine.this.mGroup.getInterface(), null);
                                try {
                                    Integer.parseInt(strStartWpsPinDisplay);
                                    P2pStateMachine.this.notifyInvitationSent(strStartWpsPinDisplay, "any");
                                    zStartWpsPinKeypad = true;
                                } catch (NumberFormatException e) {
                                    zStartWpsPinKeypad = false;
                                }
                            }
                            P2pStateMachine.this.replyToMessage(message, zStartWpsPinKeypad ? 139328 : 139327);
                        }
                        return true;
                    case 139357:
                        String string = message.obj != null ? ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_CLIENT_MESSAGE") : null;
                        P2pStateMachine.this.logd("remove client, am I GO? " + P2pStateMachine.this.mGroup.getOwner().deviceAddress.equals(WifiP2pServiceImpl.this.mThisDevice.deviceAddress) + ", ths client is " + string);
                        if (P2pStateMachine.this.mGroup.getOwner().deviceAddress.equals(WifiP2pServiceImpl.this.mThisDevice.deviceAddress) && P2pStateMachine.this.p2pRemoveClient(P2pStateMachine.this.mGroup.getInterface(), string)) {
                            P2pStateMachine.this.replyToMessage(message, 139359);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139358, 0);
                        }
                        return true;
                    case WifiP2pServiceImpl.DISABLE_P2P:
                        P2pStateMachine.this.sendMessage(139280);
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case WifiP2pServiceImpl.IPC_PRE_DHCP_ACTION:
                        P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), false);
                        WifiP2pServiceImpl.this.mIpClient.completedPreDhcpAction();
                        return true;
                    case WifiP2pServiceImpl.IPC_POST_DHCP_ACTION:
                        P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), true);
                        return true;
                    case WifiP2pServiceImpl.IPC_DHCP_RESULTS:
                        WifiP2pServiceImpl.this.mDhcpResults = (DhcpResults) message.obj;
                        return true;
                    case WifiP2pServiceImpl.IPC_PROVISIONING_SUCCESS:
                        P2pStateMachine.this.logd("mDhcpResults: " + WifiP2pServiceImpl.this.mDhcpResults);
                        if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                            P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(WifiP2pServiceImpl.this.mDhcpResults.serverAddress);
                        }
                        try {
                            String str = P2pStateMachine.this.mGroup.getInterface();
                            if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                                WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork(str, WifiP2pServiceImpl.this.mDhcpResults.getRoutes(str));
                            }
                            break;
                        } catch (RemoteException e2) {
                            P2pStateMachine.this.loge("Failed to add iface to local network " + e2);
                        }
                        if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                            if (WifiP2pServiceImpl.this.mDhcpResults.serverAddress != null && WifiP2pServiceImpl.this.mDhcpResults.serverAddress.toString().startsWith("/")) {
                                P2pStateMachine.this.mGroup.getOwner().deviceIP = WifiP2pServiceImpl.this.mDhcpResults.serverAddress.toString().substring(1);
                            } else {
                                P2pStateMachine.this.mGroup.getOwner().deviceIP = "" + WifiP2pServiceImpl.this.mDhcpResults.serverAddress;
                            }
                        }
                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                        return true;
                    case WifiP2pServiceImpl.IPC_PROVISIONING_FAILURE:
                        P2pStateMachine.this.loge("IP provisioning failed");
                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        return true;
                    case 147458:
                        P2pStateMachine.this.loge("Supplicant close unexpected, send fake Group Remove event");
                        P2pStateMachine.this.sendMessage(WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT);
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case WifiP2pMonitor.P2P_DEVICE_FOUND_EVENT:
                        WifiP2pDevice wifiP2pDevice = (WifiP2pDevice) message.obj;
                        if (!WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(wifiP2pDevice.deviceAddress)) {
                            if (P2pStateMachine.this.mGroup.contains(wifiP2pDevice)) {
                                wifiP2pDevice.status = 0;
                            }
                            P2pStateMachine.this.mPeers.update(wifiP2pDevice);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        return true;
                    case WifiP2pMonitor.P2P_DEVICE_LOST_EVENT:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                            return false;
                        }
                        WifiP2pDevice wifiP2pDevice2 = (WifiP2pDevice) message.obj;
                        if (!P2pStateMachine.this.mGroup.contains(wifiP2pDevice2)) {
                            return false;
                        }
                        P2pStateMachine.this.logd("Add device to lost list " + wifiP2pDevice2);
                        P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(wifiP2pDevice2);
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.loge("Duplicate group creation event notice, ignore");
                        return true;
                    case WifiP2pMonitor.P2P_GROUP_REMOVED_EVENT:
                        P2pStateMachine.this.logd(getName() + " group removed");
                        P2pStateMachine.this.handleGroupRemoved();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiP2pMonitor.P2P_INVITATION_RESULT_EVENT:
                        P2pStatus p2pStatus = (P2pStatus) message.obj;
                        if (p2pStatus != P2pStatus.SUCCESS) {
                            P2pStateMachine.this.loge("Invitation result " + p2pStatus);
                            if (p2pStatus == P2pStatus.UNKNOWN_P2P_GROUP && (networkId = P2pStateMachine.this.mGroup.getNetworkId()) >= 0) {
                                P2pStateMachine.this.logd("Remove unknown client from the list");
                                P2pStateMachine.this.removeClientFromList(networkId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, false);
                                P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                            }
                        }
                        return true;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        WifiP2pProvDiscEvent wifiP2pProvDiscEvent = (WifiP2pProvDiscEvent) message.obj;
                        P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                        if (wifiP2pProvDiscEvent != null && wifiP2pProvDiscEvent.device != null) {
                            P2pStateMachine.this.mSavedPeerConfig.deviceAddress = wifiP2pProvDiscEvent.device.deviceAddress;
                        }
                        if (message.what == 147491) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                        } else if (message.what == 147492) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                            P2pStateMachine.this.mSavedPeerConfig.wps.pin = wifiP2pProvDiscEvent.pin;
                        } else {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingJoinState);
                        return true;
                    case 147497:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        } else {
                            WifiP2pDevice wifiP2pDevice3 = (WifiP2pDevice) message.obj;
                            String str2 = wifiP2pDevice3.deviceAddress;
                            if (str2 != null) {
                                P2pStateMachine.this.mPeers.updateStatus(str2, 3);
                                if (P2pStateMachine.this.mGroup.removeClient(str2)) {
                                    P2pStateMachine.this.logd("Removed client " + str2);
                                    if (WifiP2pServiceImpl.this.mAutonomousGroup || !P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                    } else {
                                        P2pStateMachine.this.logd("Client list empty, remove non-persistent p2p group");
                                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                    }
                                } else {
                                    P2pStateMachine.this.logd("Failed to remove client " + str2);
                                    for (WifiP2pDevice wifiP2pDevice4 : P2pStateMachine.this.mGroup.getClientList()) {
                                        P2pStateMachine.this.logd("client " + wifiP2pDevice4.deviceAddress);
                                    }
                                }
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                P2pStateMachine.this.logd(getName() + " ap sta disconnected");
                            } else {
                                P2pStateMachine.this.loge("Disconnect on unknown device: " + wifiP2pDevice3);
                            }
                        }
                        return true;
                    case 147498:
                        if (message.obj == null) {
                            Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                        } else {
                            WifiP2pDevice wifiP2pDevice5 = (WifiP2pDevice) message.obj;
                            String str3 = wifiP2pDevice5.deviceAddress;
                            P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 0);
                            if (str3 != null) {
                                if (P2pStateMachine.this.mPeers.get(str3) != null) {
                                    P2pStateMachine.this.mGroup.addClient(P2pStateMachine.this.mPeers.get(str3));
                                } else {
                                    P2pStateMachine.this.mGroup.addClient(str3);
                                }
                                WifiP2pDevice wifiP2pDevice6 = P2pStateMachine.this.mPeers.get(str3);
                                wifiP2pDevice6.interfaceAddress = wifiP2pDevice5.interfaceAddress;
                                P2pStateMachine.this.mPeers.update(wifiP2pDevice6);
                                P2pStateMachine.this.mPeers.updateStatus(str3, 0);
                                P2pStateMachine.this.logd(getName() + " ap sta connected");
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                if (P2pStateMachine.this.isWfdSinkConnected()) {
                                    P2pStateMachine.this.logd(getName() + " [wfd sink] stop scan@GO, to avoid packet lost");
                                    P2pStateMachine.this.mWifiNative.p2pStopFind();
                                }
                            } else {
                                P2pStateMachine.this.loge("Connect on null device address, ignore");
                            }
                            P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                        }
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
                P2pStateMachine.this.updateThisDevice(3);
                P2pStateMachine.this.resetWifiP2pInfo();
                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast(WifiP2pServiceImpl.this.mGroupRemoveReason);
            }
        }

        class UserAuthorizingJoinState extends State {
            UserAuthorizingJoinState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                if (!P2pStateMachine.this.isWfdSinkEnabled()) {
                    P2pStateMachine.this.notifyInvitationReceived();
                } else {
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139354:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                            P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                        } else {
                            P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), P2pStateMachine.this.mSavedPeerConfig.wps.pin);
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        return true;
                    case 139355:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                        P2pStateMachine.this.logd("User rejected incoming request");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        return true;
                    case WifiP2pMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiP2pMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                    case WifiP2pMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
            }
        }

        class OngoingGroupRemovalState extends State {
            OngoingGroupRemovalState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                int i = message.what;
                if (i == 139280) {
                    P2pStateMachine.this.replyToMessage(message, 139282);
                    return true;
                }
                if (i == 139357) {
                    P2pStateMachine.this.replyToMessage(message, 139359);
                    return true;
                }
                return false;
            }
        }

        public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            super.dump(fileDescriptor, printWriter, strArr);
            printWriter.println("mWifiP2pInfo " + this.mWifiP2pInfo);
            printWriter.println("mGroup " + this.mGroup);
            printWriter.println("mSavedPeerConfig " + this.mSavedPeerConfig);
            printWriter.println("mGroups" + this.mGroups);
            printWriter.println();
        }

        private void checkAndReEnableP2p() {
            Log.d(WifiP2pServiceImpl.TAG, "Wifi enabled=" + this.mIsWifiEnabled + ", P2P Interface availability=" + this.mIsInterfaceAvailable + ", Number of clients=" + WifiP2pServiceImpl.this.mDeathDataByBinder.size());
            if (this.mIsWifiEnabled && this.mIsInterfaceAvailable && !WifiP2pServiceImpl.this.mDeathDataByBinder.isEmpty()) {
                sendMessage(WifiP2pServiceImpl.ENABLE_P2P);
            }
        }

        private void checkAndSendP2pStateChangedBroadcast() {
            Log.d(WifiP2pServiceImpl.TAG, "Wifi enabled=" + this.mIsWifiEnabled + ", P2P Interface availability=" + this.mIsInterfaceAvailable);
            sendP2pStateChangedBroadcast(this.mIsWifiEnabled && this.mIsInterfaceAvailable);
        }

        private void sendP2pStateChangedBroadcast(boolean z) {
            Intent intent = new Intent("android.net.wifi.p2p.STATE_CHANGED");
            intent.addFlags(67108864);
            if (z) {
                intent.putExtra("wifi_p2p_state", 2);
            } else {
                intent.putExtra("wifi_p2p_state", 1);
            }
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pDiscoveryChangedBroadcast(boolean z) {
            int i;
            if (WifiP2pServiceImpl.this.mDiscoveryStarted == z) {
                return;
            }
            WifiP2pServiceImpl.this.mDiscoveryStarted = z;
            logd("discovery change broadcast " + z);
            Intent intent = new Intent("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
            intent.addFlags(67108864);
            if (z) {
                i = 2;
            } else {
                i = 1;
            }
            intent.putExtra("discoveryState", i);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendThisDeviceChangedBroadcast() {
            Intent intent = new Intent("android.net.wifi.p2p.THIS_DEVICE_CHANGED");
            intent.addFlags(67108864);
            intent.putExtra("wifiP2pDevice", new WifiP2pDevice(WifiP2pServiceImpl.this.mThisDevice));
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendPeersChangedBroadcast() {
            Intent intent = new Intent("android.net.wifi.p2p.PEERS_CHANGED");
            intent.putExtra("wifiP2pDeviceList", new WifiP2pDeviceList(this.mPeers));
            intent.addFlags(67108864);
            WifiP2pServiceImpl.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pConnectionChangedBroadcast() {
            logd("sending p2p connection changed broadcast");
            Intent intent = new Intent("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("wifiP2pInfo", new WifiP2pInfo(this.mWifiP2pInfo));
            intent.putExtra("networkInfo", new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            intent.putExtra("p2pGroupInfo", new WifiP2pGroup(this.mGroup));
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_CONNECTION_CHANGED, new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            } else {
                loge("sendP2pConnectionChangedBroadcast(): WifiChannel is null");
            }
        }

        private void sendP2pPersistentGroupsChangedBroadcast() {
            logd("sending p2p persistent groups changed broadcast");
            Intent intent = new Intent("android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED");
            intent.addFlags(67108864);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void startDhcpServer(String str) {
            try {
                InterfaceConfiguration interfaceConfig = WifiP2pServiceImpl.this.mNwService.getInterfaceConfig(str);
                interfaceConfig.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS), 24));
                interfaceConfig.setInterfaceUp();
                WifiP2pServiceImpl.this.mNwService.setInterfaceConfig(str, interfaceConfig);
                String[] tetheredDhcpRanges = ((ConnectivityManager) WifiP2pServiceImpl.this.mContext.getSystemService("connectivity")).getTetheredDhcpRanges();
                if (WifiP2pServiceImpl.this.mNwService.isTetheringStarted()) {
                    logd("Stop existing tethering and restart it");
                    WifiP2pServiceImpl.this.mNwService.stopTethering();
                }
                WifiP2pServiceImpl.this.mNwService.tetherInterface(str);
                WifiP2pServiceImpl.this.mNwService.startTethering(tetheredDhcpRanges);
                logd("Started Dhcp server on " + str);
            } catch (Exception e) {
                loge("Error configuring interface " + str + ", :" + e);
            }
        }

        private void stopDhcpServer(String str) {
            try {
                WifiP2pServiceImpl.this.mNwService.untetherInterface(str);
                for (String str2 : WifiP2pServiceImpl.this.mNwService.listTetheredInterfaces()) {
                    logd("List all interfaces " + str2);
                    if (str2.compareTo(str) != 0) {
                        logd("Found other tethering interfaces, so keep tethering alive");
                        return;
                    }
                }
                WifiP2pServiceImpl.this.mNwService.stopTethering();
            } catch (Exception e) {
                loge("Error stopping Dhcp server" + e);
            } finally {
                logd("Stopped Dhcp server");
            }
        }

        private void notifyP2pEnableFailure() {
            Resources system = Resources.getSystem();
            AlertDialog alertDialogCreate = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(system.getString(R.string.notification_channel_sms)).setMessage(system.getString(R.string.notification_channel_usb)).setPositiveButton(system.getString(R.string.ok), (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            alertDialogCreate.getWindow().setType(2003);
            WindowManager.LayoutParams attributes = alertDialogCreate.getWindow().getAttributes();
            attributes.privateFlags = 16;
            alertDialogCreate.getWindow().setAttributes(attributes);
            alertDialogCreate.show();
        }

        private void addRowToDialog(ViewGroup viewGroup, int i, String str) {
            Resources system = Resources.getSystem();
            View viewInflate = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.preferences, viewGroup, false);
            ((TextView) viewInflate.findViewById(R.id.flagNoFullscreen)).setText(system.getString(i));
            ((TextView) viewInflate.findViewById(R.id.remote_checked_change_listener_tag)).setText(str);
            viewGroup.addView(viewInflate);
        }

        private void notifyInvitationSent(String str, String str2) {
            Resources system = Resources.getSystem();
            View viewInflate = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.preference_widget_switch, (ViewGroup) null);
            ViewGroup viewGroup = (ViewGroup) viewInflate.findViewById(R.id.conversation_icon_badge);
            addRowToDialog(viewGroup, R.string.notification_content_long_running_fgs, getDeviceName(str2));
            addRowToDialog(viewGroup, R.string.notification_content_abusive_bg_apps, str);
            AlertDialog alertDialogCreate = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(system.getString(R.string.notification_channel_wfc)).setView(viewInflate).setPositiveButton(system.getString(R.string.ok), (DialogInterface.OnClickListener) null).create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            alertDialogCreate.getWindow().setType(2003);
            WindowManager.LayoutParams attributes = alertDialogCreate.getWindow().getAttributes();
            attributes.privateFlags = 16;
            alertDialogCreate.getWindow().setAttributes(attributes);
            alertDialogCreate.show();
        }

        private void notifyP2pProvDiscShowPinRequest(final String str, final String str2) {
            Resources system = Resources.getSystem();
            View viewInflate = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.preference_widget_switch, (ViewGroup) null);
            ViewGroup viewGroup = (ViewGroup) viewInflate.findViewById(R.id.conversation_icon_badge);
            addRowToDialog(viewGroup, R.string.notification_content_long_running_fgs, getDeviceName(str2));
            addRowToDialog(viewGroup, R.string.notification_content_abusive_bg_apps, str);
            AlertDialog alertDialogCreate = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(system.getString(R.string.notification_channel_wfc)).setView(viewInflate).setPositiveButton(system.getString(R.string.config_systemSupervision), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                    P2pStateMachine.this.mSavedPeerConfig.deviceAddress = str2;
                    P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                    P2pStateMachine.this.mSavedPeerConfig.wps.pin = str;
                    P2pStateMachine.this.mWifiNative.p2pConnect(P2pStateMachine.this.mSavedPeerConfig, WifiP2pServiceImpl.FORM_GROUP.booleanValue());
                }
            }).create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            alertDialogCreate.getWindow().setType(2003);
            WindowManager.LayoutParams attributes = alertDialogCreate.getWindow().getAttributes();
            attributes.privateFlags = 16;
            alertDialogCreate.getWindow().setAttributes(attributes);
            alertDialogCreate.show();
        }

        private void notifyInvitationReceived() {
            Resources system = Resources.getSystem();
            final WpsInfo wpsInfo = this.mSavedPeerConfig.wps;
            View viewInflate = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.preference_widget_switch, (ViewGroup) null);
            ViewGroup viewGroup = (ViewGroup) viewInflate.findViewById(R.id.conversation_icon_badge);
            addRowToDialog(viewGroup, R.string.notification_channel_vpn, getDeviceName(this.mSavedPeerConfig.deviceAddress));
            final EditText editText = (EditText) viewInflate.findViewById(R.id.resolver_empty_state_subtitle);
            AlertDialog alertDialogCreate = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(system.getString(R.string.notification_compact_heads_up_reply)).setView(viewInflate).setPositiveButton(system.getString(R.string.config_systemSupervision), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (wpsInfo.setup == 2) {
                        P2pStateMachine.this.mSavedPeerConfig.wps.pin = editText.getText().toString();
                    }
                    P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " accept invitation " + P2pStateMachine.this.mSavedPeerConfig);
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                }
            }).setNegativeButton(system.getString(R.string.badPuk), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " ignore connect");
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " ignore connect");
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }).create();
            alertDialogCreate.setCanceledOnTouchOutside(false);
            switch (wpsInfo.setup) {
                case 1:
                    logd("Shown pin section visible");
                    addRowToDialog(viewGroup, R.string.notification_content_abusive_bg_apps, wpsInfo.pin);
                    break;
                case 2:
                    logd("Enter pin section visible");
                    viewInflate.findViewById(R.id.autofill_save_yes).setVisibility(0);
                    break;
            }
            if ((system.getConfiguration().uiMode & 5) == 5) {
                alertDialogCreate.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                        if (i == 164) {
                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                            dialogInterface.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
            }
            alertDialogCreate.getWindow().setType(2003);
            WindowManager.LayoutParams attributes = alertDialogCreate.getWindow().getAttributes();
            attributes.privateFlags = 16;
            alertDialogCreate.getWindow().setAttributes(attributes);
            alertDialogCreate.show();
        }

        private void updatePersistentNetworks(boolean z) {
            if (z) {
                this.mGroups.clear();
            }
            if (this.mWifiNative.p2pListNetworks(this.mGroups) || z) {
                for (WifiP2pGroup wifiP2pGroup : this.mGroups.getGroupList()) {
                    if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(wifiP2pGroup.getOwner().deviceAddress)) {
                        wifiP2pGroup.setOwner(WifiP2pServiceImpl.this.mThisDevice);
                    }
                }
                this.mWifiNative.saveConfig();
                sendP2pPersistentGroupsChangedBroadcast();
            }
        }

        private boolean isConfigInvalid(WifiP2pConfig wifiP2pConfig) {
            if (wifiP2pConfig == null || TextUtils.isEmpty(wifiP2pConfig.deviceAddress) || this.mPeers.get(wifiP2pConfig.deviceAddress) == null) {
                return true;
            }
            return false;
        }

        private WifiP2pDevice fetchCurrentDeviceDetails(WifiP2pConfig wifiP2pConfig) {
            if (wifiP2pConfig == null) {
                return null;
            }
            this.mPeers.updateGroupCapability(wifiP2pConfig.deviceAddress, this.mWifiNative.getGroupCapability(wifiP2pConfig.deviceAddress));
            return this.mPeers.get(wifiP2pConfig.deviceAddress);
        }

        private void p2pConnectWithPinDisplay(WifiP2pConfig wifiP2pConfig) {
            if (wifiP2pConfig == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            WifiP2pDevice wifiP2pDeviceFetchCurrentDeviceDetails = fetchCurrentDeviceDetails(wifiP2pConfig);
            if (wifiP2pDeviceFetchCurrentDeviceDetails == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                return;
            }
            String strP2pConnect = this.mWifiNative.p2pConnect(wifiP2pConfig, wifiP2pDeviceFetchCurrentDeviceDetails.isGroupOwner());
            try {
                Integer.parseInt(strP2pConnect);
                notifyInvitationSent(strP2pConnect, wifiP2pConfig.deviceAddress);
            } catch (NumberFormatException e) {
            }
        }

        private boolean reinvokePersistentGroup(WifiP2pConfig wifiP2pConfig) {
            int networkId;
            if (wifiP2pConfig == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return false;
            }
            WifiP2pDevice wifiP2pDeviceFetchCurrentDeviceDetails = fetchCurrentDeviceDetails(wifiP2pConfig);
            if (wifiP2pDeviceFetchCurrentDeviceDetails == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Invalid device");
                return false;
            }
            boolean zIsGroupOwner = wifiP2pDeviceFetchCurrentDeviceDetails.isGroupOwner();
            String strP2pGetSsid = this.mWifiNative.p2pGetSsid(wifiP2pDeviceFetchCurrentDeviceDetails.deviceAddress);
            logd("target ssid is " + strP2pGetSsid + " join:" + zIsGroupOwner);
            if (zIsGroupOwner && wifiP2pDeviceFetchCurrentDeviceDetails.isGroupLimit()) {
                logd("target device reaches group limit.");
                zIsGroupOwner = false;
            } else if (zIsGroupOwner && (networkId = this.mGroups.getNetworkId(wifiP2pDeviceFetchCurrentDeviceDetails.deviceAddress, strP2pGetSsid)) >= 0) {
                if (!this.mWifiNative.p2pGroupAdd(networkId)) {
                    return false;
                }
                return true;
            }
            if (!zIsGroupOwner && wifiP2pDeviceFetchCurrentDeviceDetails.isDeviceLimit()) {
                loge("target device reaches the device limit.");
                return false;
            }
            if (!zIsGroupOwner && wifiP2pDeviceFetchCurrentDeviceDetails.isInvitationCapable()) {
                int networkId2 = -2;
                if (wifiP2pConfig.netId >= 0) {
                    if (wifiP2pConfig.deviceAddress.equals(this.mGroups.getOwnerAddr(wifiP2pConfig.netId))) {
                        networkId2 = wifiP2pConfig.netId;
                    }
                } else {
                    networkId2 = this.mGroups.getNetworkId(wifiP2pDeviceFetchCurrentDeviceDetails.deviceAddress);
                }
                if (networkId2 < 0) {
                    networkId2 = getNetworkIdFromClientList(wifiP2pDeviceFetchCurrentDeviceDetails.deviceAddress);
                }
                logd("netId related with " + wifiP2pDeviceFetchCurrentDeviceDetails.deviceAddress + " = " + networkId2);
                if (networkId2 >= 0) {
                    if (this.mWifiNative.p2pReinvoke(networkId2, wifiP2pDeviceFetchCurrentDeviceDetails.deviceAddress)) {
                        wifiP2pConfig.netId = networkId2;
                        return true;
                    }
                    loge("p2pReinvoke() failed, update networks");
                    updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                    return false;
                }
            }
            return false;
        }

        private int getNetworkIdFromClientList(String str) {
            if (str == null) {
                return -1;
            }
            Iterator it = this.mGroups.getGroupList().iterator();
            while (it.hasNext()) {
                int networkId = ((WifiP2pGroup) it.next()).getNetworkId();
                String[] clientList = getClientList(networkId);
                if (clientList != null) {
                    for (String str2 : clientList) {
                        if (str.equalsIgnoreCase(str2)) {
                            return networkId;
                        }
                    }
                }
            }
            return -1;
        }

        private String[] getClientList(int i) {
            String p2pClientList = this.mWifiNative.getP2pClientList(i);
            if (p2pClientList == null) {
                return null;
            }
            return p2pClientList.split(" ");
        }

        private boolean removeClientFromList(int i, String str, boolean z) {
            boolean z2;
            StringBuilder sb = new StringBuilder();
            String[] clientList = getClientList(i);
            if (clientList != null) {
                z2 = false;
                for (String str2 : clientList) {
                    if (str2.equalsIgnoreCase(str)) {
                        z2 = true;
                    } else {
                        sb.append(" ");
                        sb.append(str2);
                    }
                }
            } else {
                z2 = false;
            }
            if (sb.length() == 0 && z) {
                logd("Remove unknown network");
                this.mGroups.remove(i);
                return true;
            }
            if (!z2) {
                return false;
            }
            logd("Modified client list: " + ((Object) sb));
            if (sb.length() == 0) {
                sb.append("\"\"");
            }
            this.mWifiNative.setP2pClientList(i, sb.toString());
            this.mWifiNative.saveConfig();
            return true;
        }

        private void setWifiP2pInfoOnGroupFormation(InetAddress inetAddress) {
            this.mWifiP2pInfo.groupFormed = true;
            this.mWifiP2pInfo.isGroupOwner = this.mGroup.isGroupOwner();
            this.mWifiP2pInfo.groupOwnerAddress = inetAddress;
        }

        private void resetWifiP2pInfo() {
            this.mWifiP2pInfo.groupFormed = false;
            this.mWifiP2pInfo.isGroupOwner = false;
            this.mWifiP2pInfo.groupOwnerAddress = null;
            WifiP2pServiceImpl.this.mNegoChannelConflict = false;
        }

        private String getDeviceName(String str) {
            WifiP2pDevice wifiP2pDevice = this.mPeers.get(str);
            if (wifiP2pDevice != null) {
                return wifiP2pDevice.deviceName;
            }
            return str;
        }

        private String getPersistedDeviceName() {
            String string = Settings.Global.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name");
            if (string == null) {
                return "Android_" + Settings.Secure.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "android_id").substring(0, 4);
            }
            return string;
        }

        private boolean setAndPersistDeviceName(String str) {
            if (str == null) {
                return false;
            }
            if (this.mWifiNative.setDeviceName(str)) {
                WifiP2pServiceImpl.this.mThisDevice.deviceName = str;
                this.mWifiNative.setP2pSsidPostfix("-" + WifiP2pServiceImpl.this.mThisDevice.deviceName);
                Settings.Global.putString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name", str);
                sendThisDeviceChangedBroadcast();
                return true;
            }
            loge("Failed to set device name " + str);
            return false;
        }

        private boolean setWfdInfo(WifiP2pWfdInfo wifiP2pWfdInfo) {
            boolean wfdEnable;
            if (!wifiP2pWfdInfo.isWfdEnabled()) {
                wfdEnable = this.mWifiNative.setWfdEnable(false);
            } else {
                wfdEnable = this.mWifiNative.setWfdEnable(true) && this.mWifiNative.setWfdDeviceInfo(wifiP2pWfdInfo.getDeviceInfoHex());
            }
            if (wfdEnable) {
                WifiP2pServiceImpl.this.mThisDevice.wfdInfo = wifiP2pWfdInfo;
                sendThisDeviceChangedBroadcast();
                return true;
            }
            loge("Failed to set wfd properties");
            return false;
        }

        private void initializeP2pSettings() {
            WifiP2pServiceImpl.this.mThisDevice.deviceName = getPersistedDeviceName();
            this.mWifiNative.setP2pDeviceName(WifiP2pServiceImpl.this.mThisDevice.deviceName);
            this.mWifiNative.setP2pSsidPostfix("-" + WifiP2pServiceImpl.this.mThisDevice.deviceName);
            this.mWifiNative.setP2pDeviceType(WifiP2pServiceImpl.this.mThisDevice.primaryDeviceType);
            this.mWifiNative.setConfigMethods("virtual_push_button physical_display keypad");
            WifiP2pServiceImpl.this.mThisDevice.deviceAddress = this.mWifiNative.p2pGetDeviceAddress();
            updateThisDevice(3);
            logd("DeviceAddress: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
            WifiP2pServiceImpl.this.mClientInfoList.clear();
            this.mWifiNative.p2pFlush();
            this.mWifiNative.p2pServiceFlush();
            WifiP2pServiceImpl.this.mServiceTransactionId = (byte) 0;
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
        }

        private void updateThisDevice(int i) {
            WifiP2pServiceImpl.this.mThisDevice.status = i;
            sendThisDeviceChangedBroadcast();
        }

        private void handleGroupCreationFailure() {
            resetWifiP2pInfo();
            WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.FAILED, null, null);
            if (WifiP2pServiceImpl.this.mGroupRemoveReason != P2pStatus.UNKNOWN) {
                sendP2pConnectionChangedBroadcast(WifiP2pServiceImpl.this.mGroupRemoveReason);
            } else {
                sendP2pConnectionChangedBroadcast();
            }
            boolean zRemove = this.mPeers.remove(this.mPeersLostDuringConnection);
            if (!TextUtils.isEmpty(this.mSavedPeerConfig.deviceAddress) && this.mPeers.remove(this.mSavedPeerConfig.deviceAddress) != null) {
                zRemove = true;
            }
            if (zRemove) {
                sendPeersChangedBroadcast();
            }
            this.mPeersLostDuringConnection.clear();
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            sendMessage(139265);
        }

        private void handleGroupRemoved() {
            if (this.mGroup.isGroupOwner()) {
                stopDhcpServer(this.mGroup.getInterface());
            } else {
                logd("stop IpClient");
                WifiP2pServiceImpl.this.stopIpClient();
                try {
                    WifiP2pServiceImpl.this.mNwService.removeInterfaceFromLocalNetwork(this.mGroup.getInterface());
                } catch (RemoteException e) {
                    loge("Failed to remove iface from local network " + e);
                }
            }
            try {
                WifiP2pServiceImpl.this.mNwService.clearInterfaceAddresses(this.mGroup.getInterface());
            } catch (Exception e2) {
                loge("Failed to clear addresses " + e2);
            }
            this.mWifiNative.setP2pGroupIdle(this.mGroup.getInterface(), 0);
            Iterator<WifiP2pDevice> it = this.mGroup.getClientList().iterator();
            boolean z = false;
            while (it.hasNext()) {
                if (this.mPeers.remove(it.next())) {
                    z = true;
                }
            }
            if (this.mPeers.remove(this.mGroup.getOwner())) {
                z = true;
            }
            if (this.mPeers.remove(this.mPeersLostDuringConnection)) {
                z = true;
            }
            if (z) {
                sendPeersChangedBroadcast();
            }
            this.mGroup = null;
            this.mPeersLostDuringConnection.clear();
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            if (WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi) {
                if (WifiP2pServiceImpl.this.mWifiChannel != null) {
                    WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 0);
                } else {
                    loge("handleGroupRemoved(): WifiChannel is null");
                }
                WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = false;
            }
        }

        private void replyToMessage(Message message, int i) {
            if (message.replyTo == null) {
                return;
            }
            Message messageObtainMessage = obtainMessage(message);
            messageObtainMessage.what = i;
            WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(message, messageObtainMessage);
        }

        private void replyToMessage(Message message, int i, int i2) {
            if (message.replyTo == null) {
                return;
            }
            Message messageObtainMessage = obtainMessage(message);
            messageObtainMessage.what = i;
            messageObtainMessage.arg1 = i2;
            WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(message, messageObtainMessage);
        }

        private void replyToMessage(Message message, int i, Object obj) {
            if (message.replyTo == null) {
                return;
            }
            Message messageObtainMessage = obtainMessage(message);
            messageObtainMessage.what = i;
            messageObtainMessage.obj = obj;
            WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(message, messageObtainMessage);
        }

        private Message obtainMessage(Message message) {
            Message messageObtain = Message.obtain();
            messageObtain.arg2 = message.arg2;
            return messageObtain;
        }

        protected void logd(String str) {
            Slog.d(WifiP2pServiceImpl.TAG, str);
        }

        protected void loge(String str) {
            Slog.e(WifiP2pServiceImpl.TAG, str);
        }

        private boolean updateSupplicantServiceRequest() {
            clearSupplicantServiceRequest();
            StringBuffer stringBuffer = new StringBuffer();
            Iterator it = WifiP2pServiceImpl.this.mClientInfoList.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ClientInfo clientInfo = (ClientInfo) it.next();
                for (int i = 0; i < clientInfo.mReqList.size(); i++) {
                    WifiP2pServiceRequest wifiP2pServiceRequest = (WifiP2pServiceRequest) clientInfo.mReqList.valueAt(i);
                    if (wifiP2pServiceRequest != null) {
                        stringBuffer.append(wifiP2pServiceRequest.getSupplicantQuery());
                    }
                }
            }
            if (stringBuffer.length() == 0) {
                return false;
            }
            WifiP2pServiceImpl.this.mServiceDiscReqId = this.mWifiNative.p2pServDiscReq(WifiP2pServiceImpl.EMPTY_DEVICE_ADDRESS, stringBuffer.toString());
            return WifiP2pServiceImpl.this.mServiceDiscReqId != null;
        }

        private void clearSupplicantServiceRequest() {
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return;
            }
            this.mWifiNative.p2pServDiscCancelReq(WifiP2pServiceImpl.this.mServiceDiscReqId);
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
        }

        private boolean addServiceRequest(Messenger messenger, WifiP2pServiceRequest wifiP2pServiceRequest) {
            if (messenger == null || wifiP2pServiceRequest == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return false;
            }
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(messenger, true);
            if (clientInfo == null) {
                return false;
            }
            WifiP2pServiceImpl.access$9904(WifiP2pServiceImpl.this);
            if (WifiP2pServiceImpl.this.mServiceTransactionId == 0) {
                WifiP2pServiceImpl.access$9904(WifiP2pServiceImpl.this);
            }
            wifiP2pServiceRequest.setTransactionId(WifiP2pServiceImpl.this.mServiceTransactionId);
            clientInfo.mReqList.put(WifiP2pServiceImpl.this.mServiceTransactionId, wifiP2pServiceRequest);
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return true;
            }
            return updateSupplicantServiceRequest();
        }

        private void removeServiceRequest(Messenger messenger, WifiP2pServiceRequest wifiP2pServiceRequest) {
            if (messenger == null || wifiP2pServiceRequest == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
            }
            boolean z = false;
            ClientInfo clientInfo = getClientInfo(messenger, false);
            if (clientInfo == null) {
                return;
            }
            int i = 0;
            while (true) {
                if (i >= clientInfo.mReqList.size()) {
                    break;
                }
                if (!wifiP2pServiceRequest.equals(clientInfo.mReqList.valueAt(i))) {
                    i++;
                } else {
                    clientInfo.mReqList.removeAt(i);
                    z = true;
                    break;
                }
            }
            if (z) {
                if (clientInfo.mReqList.size() == 0 && clientInfo.mServList.size() == 0) {
                    logd("remove client information from framework");
                    WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                }
                if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                    return;
                }
                updateSupplicantServiceRequest();
            }
        }

        private void clearServiceRequests(Messenger messenger) {
            if (messenger == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            ClientInfo clientInfo = getClientInfo(messenger, false);
            if (clientInfo == null || clientInfo.mReqList.size() == 0) {
                return;
            }
            clientInfo.mReqList.clear();
            if (clientInfo.mServList.size() == 0) {
                logd("remove channel information from framework");
                WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
            }
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return;
            }
            updateSupplicantServiceRequest();
        }

        private boolean addLocalService(Messenger messenger, WifiP2pServiceInfo wifiP2pServiceInfo) {
            if (messenger == null || wifiP2pServiceInfo == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                return false;
            }
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(messenger, true);
            if (clientInfo == null || !clientInfo.mServList.add(wifiP2pServiceInfo)) {
                return false;
            }
            if (this.mWifiNative.p2pServiceAdd(wifiP2pServiceInfo)) {
                return true;
            }
            clientInfo.mServList.remove(wifiP2pServiceInfo);
            return false;
        }

        private void removeLocalService(Messenger messenger, WifiP2pServiceInfo wifiP2pServiceInfo) {
            if (messenger == null || wifiP2pServiceInfo == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal arguments");
                return;
            }
            ClientInfo clientInfo = getClientInfo(messenger, false);
            if (clientInfo == null) {
                return;
            }
            this.mWifiNative.p2pServiceDel(wifiP2pServiceInfo);
            clientInfo.mServList.remove(wifiP2pServiceInfo);
            if (clientInfo.mReqList.size() == 0 && clientInfo.mServList.size() == 0) {
                logd("remove client information from framework");
                WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
            }
        }

        private void clearLocalServices(Messenger messenger) {
            if (messenger == null) {
                Log.e(WifiP2pServiceImpl.TAG, "Illegal argument(s)");
                return;
            }
            ClientInfo clientInfo = getClientInfo(messenger, false);
            if (clientInfo == null) {
                return;
            }
            Iterator it = clientInfo.mServList.iterator();
            while (it.hasNext()) {
                this.mWifiNative.p2pServiceDel((WifiP2pServiceInfo) it.next());
            }
            clientInfo.mServList.clear();
            if (clientInfo.mReqList.size() == 0) {
                logd("remove client information from framework");
                WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
            }
        }

        private void clearClientInfo(Messenger messenger) {
            clearLocalServices(messenger);
            clearServiceRequests(messenger);
        }

        private void sendServiceResponse(WifiP2pServiceResponse wifiP2pServiceResponse) {
            if (wifiP2pServiceResponse != null) {
                for (ClientInfo clientInfo : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                    if (((WifiP2pServiceRequest) clientInfo.mReqList.get(wifiP2pServiceResponse.getTransactionId())) != null) {
                        Message messageObtain = Message.obtain();
                        messageObtain.what = 139314;
                        messageObtain.arg1 = 0;
                        messageObtain.arg2 = 0;
                        messageObtain.obj = wifiP2pServiceResponse;
                        if (clientInfo.mMessenger == null) {
                            continue;
                        } else {
                            try {
                                clientInfo.mMessenger.send(messageObtain);
                            } catch (RemoteException e) {
                                logd("detect dead channel");
                                clearClientInfo(clientInfo.mMessenger);
                                return;
                            }
                        }
                    }
                }
                return;
            }
            Log.e(WifiP2pServiceImpl.TAG, "sendServiceResponse with null response");
        }

        private void clearClientDeadChannels() {
            ArrayList arrayList = new ArrayList();
            for (ClientInfo clientInfo : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                Message messageObtain = Message.obtain();
                messageObtain.what = 139313;
                messageObtain.arg1 = 0;
                messageObtain.arg2 = 0;
                messageObtain.obj = null;
                if (clientInfo.mMessenger != null) {
                    try {
                        clientInfo.mMessenger.send(messageObtain);
                    } catch (RemoteException e) {
                        logd("detect dead channel");
                        arrayList.add(clientInfo.mMessenger);
                    }
                }
            }
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                clearClientInfo((Messenger) it.next());
            }
        }

        private ClientInfo getClientInfo(Messenger messenger, boolean z) {
            ClientInfo clientInfo = (ClientInfo) WifiP2pServiceImpl.this.mClientInfoList.get(messenger);
            if (clientInfo == null && z) {
                logd("add a new client");
                ClientInfo clientInfo2 = new ClientInfo(messenger);
                WifiP2pServiceImpl.this.mClientInfoList.put(messenger, clientInfo2);
                return clientInfo2;
            }
            return clientInfo;
        }

        private WifiP2pDeviceList getPeers(Bundle bundle, int i) {
            String string = bundle.getString("android.net.wifi.p2p.CALLING_PACKAGE");
            if (this.mWifiInjector == null) {
                this.mWifiInjector = WifiInjector.getInstance();
            }
            try {
                this.mWifiInjector.getWifiPermissionsUtil().enforceCanAccessScanResults(string, i);
                return new WifiP2pDeviceList(this.mPeers);
            } catch (SecurityException e) {
                Log.v(WifiP2pServiceImpl.TAG, "Security Exception, cannot access peer list");
                return new WifiP2pDeviceList();
            }
        }

        private void sendP2pConnectionChangedBroadcast(P2pStatus p2pStatus) {
            logd("sending p2p connection changed broadcast, reason = " + p2pStatus + ", mGroup: " + this.mGroup + ", mP2pOperFreq: " + WifiP2pServiceImpl.this.mP2pOperFreq);
            Intent intent = new Intent("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("wifiP2pInfo", new WifiP2pInfo(this.mWifiP2pInfo));
            intent.putExtra("networkInfo", new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            intent.putExtra("p2pGroupInfo", new WifiP2pGroup(this.mGroup));
            intent.putExtra("p2pOperFreq", WifiP2pServiceImpl.this.mP2pOperFreq);
            if (p2pStatus == P2pStatus.NO_COMMON_CHANNEL) {
                intent.putExtra("reason=", 7);
            } else if (p2pStatus == P2pStatus.MTK_EXPAND_02) {
                logd("channel conflict, user decline, broadcast with reason=-3");
                intent.putExtra("reason=", -3);
            } else if (p2pStatus == P2pStatus.MTK_EXPAND_01) {
                logd("[wfd sink/source] broadcast with reason=-2");
                intent.putExtra("reason=", -2);
            } else {
                intent.putExtra("reason=", -1);
            }
            WifiP2pServiceImpl.this.mGroupRemoveReason = P2pStatus.UNKNOWN;
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_CONNECTION_CHANGED, new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
        }

        private void sendP2pGOandGCRequestConnectBroadcast() {
            logd("sendP2pGOandGCRequestConnectBroadcast");
            Intent intent = new Intent("com.mediatek.wifi.p2p.GO.GCrequest.connect");
            intent.addFlags(603979776);
            WifiP2pDevice wifiP2pDevice = this.mPeers.get(this.mSavedPeerConfig.deviceAddress);
            if (wifiP2pDevice != null && wifiP2pDevice.deviceName != null) {
                intent.putExtra("deviceName", wifiP2pDevice.deviceName);
            } else {
                intent.putExtra("deviceName", "wifidisplay source");
            }
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pOPChannelBroadcast() {
            logd("sendP2pOPChannelBroadcast: OperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
            Intent intent = new Intent("com.mediatek.wifi.p2p.OP.channel");
            intent.addFlags(603979776);
            intent.putExtra("p2pOperFreq", WifiP2pServiceImpl.this.mP2pOperFreq);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pFreqConflictBroadcast() {
            logd("sendP2pFreqConflictBroadcast");
            Intent intent = new Intent("com.mediatek.wifi.p2p.freq.conflict");
            intent.addFlags(603979776);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private int nameValueAssign(String[] strArr, int i, int i2) {
            if (strArr == null || strArr.length != 2 || strArr[1] == null || i2 == 0) {
                return 0;
            }
            return Integer.parseInt(strArr[1].trim(), i2);
        }

        public WifiInfo getWifiConnectionInfo() {
            if (WifiP2pServiceImpl.this.mWifiManager == null) {
                WifiP2pServiceImpl.this.mWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi");
            }
            return WifiP2pServiceImpl.this.mWifiManager.getConnectionInfo();
        }

        private String getInterfaceAddress(String str) {
            logd("getInterfaceAddress(): deviceAddress=" + str);
            WifiP2pDevice wifiP2pDevice = this.mPeers.get(str);
            if (wifiP2pDevice == null || str.equals(wifiP2pDevice.interfaceAddress)) {
                return str;
            }
            logd("getInterfaceAddress(): interfaceAddress=" + wifiP2pDevice.interfaceAddress);
            return wifiP2pDevice.interfaceAddress;
        }

        public String getPeerIpAddress(String str) throws Throwable {
            FileInputStream fileInputStream;
            logd("getPeerIpAddress(): input address=" + str);
            if (str == null) {
                return null;
            }
            if (this.mGroup == null) {
                loge("getPeerIpAddress(): mGroup is null!");
                return null;
            }
            ?? IsGroupOwner = this.mGroup.isGroupOwner();
            if (IsGroupOwner == 0) {
                if (this.mGroup.getOwner().deviceAddress != null && str.equals(this.mGroup.getOwner().deviceAddress)) {
                    logd("getPeerIpAddress(): GO device address case, goIpAddress=" + this.mGroup.getOwner().deviceIP);
                    return this.mGroup.getOwner().deviceIP;
                }
                if (this.mGroup.getOwner().interfaceAddress == null || !str.equals(this.mGroup.getOwner().interfaceAddress)) {
                    loge("getPeerIpAddress(): no match GO address case, goIpAddress is null");
                    return null;
                }
                logd("getPeerIpAddress(): GO interface address case, goIpAddress=" + this.mGroup.getOwner().deviceIP);
                return this.mGroup.getOwner().deviceIP;
            }
            String interfaceAddress = getInterfaceAddress(str);
            try {
                try {
                    fileInputStream = new FileInputStream(WifiP2pServiceImpl.DHCP_INFO_FILE);
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new DataInputStream(fileInputStream)));
                        for (String line = bufferedReader.readLine(); line != null && line.length() != 0; line = bufferedReader.readLine()) {
                            String[] strArrSplit = line.split(" ");
                            String str2 = strArrSplit.length > 3 ? strArrSplit[2] : null;
                            if (str2 != null && strArrSplit[1] != null && strArrSplit[1].indexOf(interfaceAddress) != -1) {
                                logd("getPeerIpAddress(): getClientIp() mac matched, get IP address = " + str2);
                                try {
                                    fileInputStream.close();
                                } catch (IOException e) {
                                    loge("getPeerIpAddress(): getClientIp() close file met IOException: " + e);
                                }
                                return str2;
                            }
                        }
                        loge("getPeerIpAddress(): getClientIp() dhcp client " + interfaceAddress + " had not connected up!");
                        try {
                            fileInputStream.close();
                        } catch (IOException e2) {
                            loge("getPeerIpAddress(): getClientIp() close file met IOException: " + e2);
                        }
                        return null;
                    } catch (IOException e3) {
                        e = e3;
                        loge("getPeerIpAddress(): getClientIp(): " + e);
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e4) {
                                loge("getPeerIpAddress(): getClientIp() close file met IOException: " + e4);
                            }
                        }
                        loge("getPeerIpAddress(): found nothing");
                        return null;
                    }
                } catch (Throwable th) {
                    th = th;
                    if (IsGroupOwner != 0) {
                        try {
                            IsGroupOwner.close();
                        } catch (IOException e5) {
                            loge("getPeerIpAddress(): getClientIp() close file met IOException: " + e5);
                        }
                    }
                    throw th;
                }
            } catch (IOException e6) {
                e = e6;
                fileInputStream = null;
            } catch (Throwable th2) {
                th = th2;
                IsGroupOwner = 0;
                if (IsGroupOwner != 0) {
                }
                throw th;
            }
        }

        private void resetWifiP2pConn() {
            if (this.mGroup != null) {
                this.mWifiNative.p2pGroupRemove(WifiP2pServiceImpl.this.mInterface);
            } else if (getHandler().hasMessages(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT)) {
                sendMessage(139274);
            }
        }

        private void p2pConfigWfdSink() {
            resetWifiP2pConn();
            this.mWifiNative.setP2pDeviceType("8-0050F204-2");
            logd("[wfd sink] p2pConfigWfdSink() ori deviceCapa = " + WifiP2pServiceImpl.this.mDeviceCapa);
        }

        private void p2pUnconfigWfdSink() {
            resetWifiP2pConn();
            this.mWifiNative.setP2pDeviceType(WifiP2pServiceImpl.this.mThisDevice.primaryDeviceType);
        }

        private boolean isWfdSinkEnabled() {
            if (SystemProperties.get("ro.vendor.mtk_wfd_sink_support").equals("1")) {
                if (WifiP2pServiceImpl.this.mThisDevice.wfdInfo != null) {
                    if (WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType() == 1 || WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType() == 3) {
                        return true;
                    }
                    logd("[wfd sink] isWfdSinkEnabled, type :" + WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType());
                    return false;
                }
                logd("[wfd sink] isWfdSinkEnabled, device wfdInfo unset");
                return false;
            }
            logd("[wfd sink] isWfdSinkEnabled, property unset");
            return false;
        }

        private boolean isWfdSinkConnected() {
            if (isWfdSinkEnabled() && this.mGroup != null) {
                return !this.mGroup.isGroupOwner() || this.mGroup.getClientAmount() == 1;
            }
            return false;
        }

        private boolean p2pRemoveClient(String str, String str2) {
            return true;
        }

        private WifiP2pGroup addPersistentGroup(HashMap<String, String> map) {
            logd("addPersistentGroup");
            return null;
        }
    }

    private class ClientInfo {
        private Messenger mMessenger;
        private SparseArray<WifiP2pServiceRequest> mReqList;
        private List<WifiP2pServiceInfo> mServList;

        private ClientInfo(Messenger messenger) {
            this.mMessenger = messenger;
            this.mReqList = new SparseArray<>();
            this.mServList = new ArrayList();
        }
    }
}
