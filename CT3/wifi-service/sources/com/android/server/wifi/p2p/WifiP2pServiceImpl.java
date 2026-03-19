package com.android.server.wifi.p2p;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.net.ip.IpManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
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
import android.net.wifi.p2p.link.WifiP2pLinkInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Toast;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiLastResortWatchdog;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachine;
import com.mediatek.server.wifi.WifiNvRamAgent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class WifiP2pServiceImpl extends IWifiP2pManager.Stub {
    private static final int BASE = 143360;
    public static final int BLOCK_DISCOVERY = 143375;
    private static final int CONNECTED_DISCOVER_TIMEOUT_S = 25;
    private static final boolean DBG = true;
    private static final String DHCP_INFO_FILE = "/data/misc/dhcp/dnsmasq.p2p0.leases";
    public static final int DISABLED = 0;
    public static final int DISABLE_P2P_TIMED_OUT = 143366;
    private static final int DISABLE_P2P_WAIT_TIME_MS = 5000;
    public static final int DISCONNECT_WIFI_REQUEST = 143372;
    public static final int DISCONNECT_WIFI_RESPONSE = 143373;
    private static final int DISCOVER_TIMEOUT_S = 120;
    private static final int DROP_WIFI_USER_ACCEPT = 143364;
    private static final int DROP_WIFI_USER_REJECT = 143365;
    public static final int ENABLED = 1;
    public static final int GROUP_CREATING_TIMED_OUT = 143361;
    private static final int GROUP_CREATING_WAIT_TIME_MS = 120000;
    private static final int GROUP_IDLE_TIME_S = 10;
    private static final int IPM_DHCP_RESULTS = 143392;
    private static final int IPM_POST_DHCP_ACTION = 143391;
    private static final int IPM_PRE_DHCP_ACTION = 143390;
    private static final int IPM_PROVISIONING_FAILURE = 143394;
    private static final int IPM_PROVISIONING_SUCCESS = 143393;
    private static final int M_P2P_CONN_FOR_INVITE_RES_INFO_UNAVAILABLE = 143381;
    private static final int M_P2P_DEVICE_FOUND_INVITATION = 143380;
    private static final String NETWORKTYPE = "WIFI_P2P";
    public static final int P2P_ACTIVE = 0;
    public static final int P2P_CONNECTION_CHANGED = 143371;
    public static final int P2P_FAST_PS = 2;
    public static final int P2P_MAX_PS = 1;
    private static final int PEER_CONNECTION_USER_ACCEPT = 143362;
    private static final int PEER_CONNECTION_USER_REJECT = 143363;
    private static final int RECONN_FOR_INVITE_RES_INFO_UNAVAILABLE_TIME_MS = 120000;
    private static final String SERVER_ADDRESS = "192.168.49.1";
    private static final int SET_BEAM_MODE = 143382;
    public static final int SET_MIRACAST_MODE = 143374;
    private static final String STATIC_CLIENT_ADDRESS = "192.168.49.2";
    private static final int STOP_P2P_MONITOR_WAIT_TIME_MS = 5000;
    private static final String TAG = "WifiP2pService";
    private static final String UNKNOWN_COMMAND = "UNKNOWN COMMAND";
    private static final int VENDOR_IE_ALL_FRAME_TAG = 99;
    private static final int VENDOR_IE_FRAME_ID_AMOUNTS = 12;
    private static final String VENDOR_IE_MTK_OUI = "000ce7";
    private static final String VENDOR_IE_OUI_TYPE__CROSSMOUNT = "33";
    private static final String VENDOR_IE_TAG = "dd";
    private boolean mAutonomousGroup;
    private ClientHandler mClientHandler;
    private Context mContext;
    private int mDeviceCapa;
    private DhcpResults mDhcpResults;
    private boolean mDiscoveryBlocked;
    private boolean mDiscoveryStarted;
    private String mInterface;
    private IpManager mIpManager;
    private boolean mJoinExistingGroup;
    INetworkManagementService mNwService;
    private P2pStateMachine mP2pStateMachine;
    private final boolean mP2pSupported;
    private String mServiceDiscReqId;
    private String mWfdSourceAddr;
    private AsyncChannel mWifiChannel;
    private WifiManager mWifiManager;
    private static final Boolean JOIN_GROUP = true;
    private static final Boolean FORM_GROUP = false;
    private static final Boolean RELOAD = true;
    private static final Boolean NO_RELOAD = false;
    private static int mGroupCreatingTimeoutIndex = 0;
    private static int mDisableP2pTimeoutIndex = 0;
    private static final Boolean WFD_DONGLE_USE_P2P_INVITE = true;
    private AsyncChannel mReplyChannel = new AsyncChannel();
    private WifiP2pDevice mThisDevice = new WifiP2pDevice();
    private boolean mDiscoveryPostponed = false;
    private boolean mTemporarilyDisconnectedWifi = false;
    private byte mServiceTransactionId = 0;
    private HashMap<Messenger, ClientInfo> mClientInfoList = new HashMap<>();
    private boolean mGcIgnoresDhcpReq = false;
    private P2pStatus mGroupRemoveReason = P2pStatus.UNKNOWN;
    private int mMiracastMode = 0;
    private int mStopP2pMonitorTimeoutIndex = 0;
    private int mP2pOperFreq = -1;
    boolean mNegoChannelConflict = false;
    private boolean mConnectToPeer = false;
    private boolean mMccSupport = false;
    private boolean mDelayReconnectForInfoUnavailable = true;
    private boolean mUpdatePeerForInvited = false;
    private boolean mCrossmountIEAdded = false;
    private boolean mCrossmountEventReceived = false;
    private String mCrossmountSessionInfo = "";
    private NetworkInfo mNetworkInfo = new NetworkInfo(13, 0, NETWORKTYPE, "");

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

        public static P2pStatus[] valuesCustom() {
            return values();
        }

        public static P2pStatus valueOf(int error) {
            switch (error) {
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
                case 12:
                    return MTK_EXPAND_01;
                case 13:
                    return MTK_EXPAND_02;
                default:
                    return UNKNOWN;
            }
        }
    }

    private class ClientHandler extends Handler {
        ClientHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
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
                case 139339:
                case 139340:
                case 139342:
                case 139343:
                case 139349:
                case 139351:
                case 139354:
                case 139355:
                case 139356:
                case 139357:
                case 139360:
                case 139361:
                    WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(Message.obtain(msg));
                    break;
                default:
                    Slog.d(WifiP2pServiceImpl.TAG, "ClientHandler.handleMessage ignoring msg=" + msg);
                    break;
            }
        }
    }

    public WifiP2pServiceImpl(Context context) {
        this.mContext = context;
        this.mP2pSupported = this.mContext.getPackageManager().hasSystemFeature("android.hardware.wifi.direct");
        this.mThisDevice.primaryDeviceType = this.mContext.getResources().getString(R.string.config_systemVisualIntelligence);
        HandlerThread wifiP2pThread = new HandlerThread(TAG);
        wifiP2pThread.start();
        this.mClientHandler = new ClientHandler(wifiP2pThread.getLooper());
        this.mP2pStateMachine = new P2pStateMachine(TAG, wifiP2pThread.getLooper(), this.mP2pSupported);
        this.mP2pStateMachine.start();
    }

    public void connectivityServiceReady() {
        IBinder b = ServiceManager.getService("network_management");
        this.mNwService = INetworkManagementService.Stub.asInterface(b);
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
        if (checkConnectivityInternalPermission() == 0 || checkLocationHardwarePermission() == 0) {
            return;
        }
        enforceConnectivityInternalPermission();
    }

    private void stopIpManager() {
        if (this.mIpManager != null) {
            this.mIpManager.stop();
            this.mIpManager = null;
        }
        this.mDhcpResults = null;
    }

    private void startIpManager(String ifname) {
        stopIpManager();
        this.mIpManager = new IpManager(this.mContext, ifname, new IpManager.Callback() {
            public void onPreDhcpAction() {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_PRE_DHCP_ACTION);
            }

            public void onPostDhcpAction() {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_POST_DHCP_ACTION);
            }

            public void onNewDhcpResults(DhcpResults dhcpResults) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_DHCP_RESULTS, dhcpResults);
            }

            public void onProvisioningSuccess(LinkProperties newLp) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_PROVISIONING_SUCCESS);
            }

            public void onProvisioningFailure(LinkProperties newLp) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.IPM_PROVISIONING_FAILURE);
            }
        }, this.mNwService);
        IpManager ipManager = this.mIpManager;
        IpManager.ProvisioningConfiguration config = IpManager.buildProvisioningConfiguration().withoutIPv6().withoutIpReachabilityMonitor().withPreDhcpAction(30000).withProvisioningTimeoutMs(36000).build();
        this.mIpManager.startProvisioning(config);
    }

    public Messenger getMessenger() {
        enforceAccessPermission();
        enforceChangePermission();
        return new Messenger(this.mClientHandler);
    }

    public Messenger getP2pStateMachineMessenger() {
        enforceConnectivityInternalOrLocationHardwarePermission();
        enforceAccessPermission();
        enforceChangePermission();
        return new Messenger(this.mP2pStateMachine.getHandler());
    }

    public void setMiracastMode(int mode) {
        enforceConnectivityInternalPermission();
        this.mP2pStateMachine.sendMessage(SET_MIRACAST_MODE, mode);
    }

    public void setMiracastModeEx(int mode, int freq) {
        this.mP2pStateMachine.sendMessage(SET_MIRACAST_MODE, mode, freq);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump WifiP2pService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        this.mP2pStateMachine.dump(fd, pw, args);
        pw.println("mAutonomousGroup " + this.mAutonomousGroup);
        pw.println("mJoinExistingGroup " + this.mJoinExistingGroup);
        pw.println("mDiscoveryStarted " + this.mDiscoveryStarted);
        pw.println("mNetworkInfo " + this.mNetworkInfo);
        pw.println("mTemporarilyDisconnectedWifi " + this.mTemporarilyDisconnectedWifi);
        pw.println("mServiceDiscReqId " + this.mServiceDiscReqId);
        pw.println();
        IpManager ipManager = this.mIpManager;
        if (ipManager != null) {
            pw.println("mIpManager:");
            ipManager.dump(fd, pw, args);
        }
    }

    public String getMacAddress() {
        Log.d(TAG, "getMacAddress(): before retriving from NVRAM = " + this.mThisDevice.deviceAddress);
        try {
            WifiNvRamAgent agent = WifiNvRamAgent.Stub.asInterface(ServiceManager.getService("NvRAMAgent"));
            byte[] buff = agent.readFileByName("/data/nvram/APCFG/APRDEB/WIFI");
            if (buff != null) {
                String macFromNVRam = String.format("%02x:%02x:%02x:%02x:%02x:%02x", Integer.valueOf(buff[4] | 2), Byte.valueOf(buff[5]), Byte.valueOf(buff[6]), Byte.valueOf(buff[7]), Byte.valueOf(buff[8]), Byte.valueOf(buff[9]));
                if (!TextUtils.isEmpty(macFromNVRam)) {
                    this.mThisDevice.deviceAddress = macFromNVRam;
                }
            }
        } catch (RemoteException re) {
            re.printStackTrace();
        } catch (IndexOutOfBoundsException iobe) {
            iobe.printStackTrace();
        } finally {
            Log.d(TAG, "getMacAddress(): after retriving from NVRAM = " + this.mThisDevice.deviceAddress);
        }
        return this.mThisDevice.deviceAddress;
    }

    public String getPeerIpAddress(String peerMacAddress) {
        return this.mP2pStateMachine.getPeerIpAddress(peerMacAddress);
    }

    public void setCrossMountIE(boolean isAdd, String hexData) {
        this.mP2pStateMachine.setCrossMountIE(isAdd, hexData);
    }

    public void setBeamMode(int mode) {
        enforceConnectivityInternalPermission();
        this.mP2pStateMachine.sendMessage(SET_BEAM_MODE, mode);
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
        private OngoingGroupRemovalState mOngoingGroupRemovalState;
        private P2pDisabledState mP2pDisabledState;
        private P2pDisablingState mP2pDisablingState;
        private P2pEnabledState mP2pEnabledState;
        private P2pEnablingState mP2pEnablingState;
        private P2pNotSupportedState mP2pNotSupportedState;
        private final WifiP2pDeviceList mPeers;
        private final WifiP2pDeviceList mPeersLostDuringConnection;
        private ProvisionDiscoveryState mProvisionDiscoveryState;
        private WifiP2pConfig mSavedPeerConfig;
        private UserAuthorizingInviteRequestState mUserAuthorizingInviteRequestState;
        private UserAuthorizingJoinState mUserAuthorizingJoinState;
        private UserAuthorizingNegotiationRequestState mUserAuthorizingNegotiationRequestState;
        private WifiMonitor mWifiMonitor;
        private WifiNative mWifiNative;
        private final WifiP2pInfo mWifiP2pInfo;

        P2pStateMachine(String name, Looper looper, boolean p2pSupported) {
            super(name, looper);
            this.mDefaultState = new DefaultState();
            this.mP2pNotSupportedState = new P2pNotSupportedState();
            this.mP2pDisablingState = new P2pDisablingState();
            this.mP2pDisabledState = new P2pDisabledState();
            this.mP2pEnablingState = new P2pEnablingState();
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
            this.mWifiNative = WifiNative.getP2pNativeInterface();
            this.mWifiMonitor = WifiMonitor.getInstance();
            this.mPeers = new WifiP2pDeviceList();
            this.mPeersLostDuringConnection = new WifiP2pDeviceList();
            this.mGroups = new WifiP2pGroupList((WifiP2pGroupList) null, new WifiP2pGroupList.GroupDeleteListener() {
                public void onDeleteGroup(int netId) {
                    P2pStateMachine.this.logd("called onDeleteGroup() netId=" + netId);
                    P2pStateMachine.this.mWifiNative.removeNetwork(netId);
                    P2pStateMachine.this.mWifiNative.saveConfig();
                    P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
                }
            });
            this.mWifiP2pInfo = new WifiP2pInfo();
            this.mSavedPeerConfig = new WifiP2pConfig();
            addState(this.mDefaultState);
            addState(this.mP2pNotSupportedState, this.mDefaultState);
            addState(this.mP2pDisablingState, this.mDefaultState);
            addState(this.mP2pDisabledState, this.mDefaultState);
            addState(this.mP2pEnablingState, this.mDefaultState);
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
            if (p2pSupported) {
                setInitialState(this.mP2pDisabledState);
            } else {
                setInitialState(this.mP2pNotSupportedState);
            }
            setLogRecSize(50);
            setLogOnlyTransitions(true);
            String interfaceName = this.mWifiNative.getInterfaceName();
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.AP_STA_CONNECTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.AP_STA_DISCONNECTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.AUTHENTICATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.NETWORK_CONNECTION_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.NETWORK_DISCONNECTION_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_DEVICE_FOUND_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_DEVICE_LOST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_FIND_STOPPED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_GROUP_REMOVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_GROUP_STARTED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_INVITATION_RECEIVED_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_INVITATION_RESULT_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_PROV_DISC_FAILURE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_PROV_DISC_PBC_REQ_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_PROV_DISC_PBC_RSP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.P2P_SERV_DISC_RESP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.SCAN_RESULTS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.SUP_CONNECTION_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.SUP_DISCONNECTION_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.WPS_FAIL_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.WPS_OVERLAP_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.WPS_SUCCESS_EVENT, getHandler());
            this.mWifiMonitor.registerHandler(interfaceName, WifiMonitor.WPS_TIMEOUT_EVENT, getHandler());
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
                        }
                        return true;
                    case 69633:
                        AsyncChannel ac = new AsyncChannel();
                        ac.connect(WifiP2pServiceImpl.this.mContext, P2pStateMachine.this.getHandler(), message.replyTo);
                        return true;
                    case 69636:
                        if (message.arg1 == 2) {
                            P2pStateMachine.this.loge("Send failed, client connection lost");
                        } else {
                            P2pStateMachine.this.loge("Client connection lost with reason: " + message.arg1);
                        }
                        WifiP2pServiceImpl.this.mWifiChannel = null;
                        return true;
                    case WifiStateMachine.CMD_ENABLE_P2P:
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
                    case WifiP2pServiceImpl.SET_BEAM_MODE:
                    case WifiP2pServiceImpl.IPM_PRE_DHCP_ACTION:
                    case WifiP2pServiceImpl.IPM_POST_DHCP_ACTION:
                    case WifiP2pServiceImpl.IPM_DHCP_RESULTS:
                    case WifiP2pServiceImpl.IPM_PROVISIONING_SUCCESS:
                    case WifiP2pServiceImpl.IPM_PROVISIONING_FAILURE:
                    case WifiMonitor.SUP_CONNECTION_EVENT:
                    case WifiMonitor.SUP_DISCONNECTION_EVENT:
                    case WifiMonitor.NETWORK_CONNECTION_EVENT:
                    case WifiMonitor.NETWORK_DISCONNECTION_EVENT:
                    case WifiMonitor.SCAN_RESULTS_EVENT:
                    case WifiMonitor.SUPPLICANT_STATE_CHANGE_EVENT:
                    case WifiMonitor.AUTHENTICATION_FAILURE_EVENT:
                    case WifiMonitor.WPS_SUCCESS_EVENT:
                    case WifiMonitor.WPS_FAIL_EVENT:
                    case WifiMonitor.WPS_OVERLAP_EVENT:
                    case WifiMonitor.WPS_TIMEOUT_EVENT:
                    case WifiMonitor.P2P_DEVICE_FOUND_EVENT:
                    case WifiMonitor.P2P_DEVICE_LOST_EVENT:
                    case WifiMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                    case WifiMonitor.P2P_GROUP_REMOVED_EVENT:
                    case WifiMonitor.P2P_INVITATION_RESULT_EVENT:
                    case WifiMonitor.P2P_FIND_STOPPED_EVENT:
                    case WifiMonitor.P2P_SERV_DISC_RESP_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_FAILURE_EVENT:
                        return true;
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
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
                        P2pStateMachine.this.replyToMessage(message, 139284, new WifiP2pDeviceList(P2pStateMachine.this.mPeers));
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
                        P2pStateMachine.this.replyToMessage(message, 139324, 2);
                        return true;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 2);
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
                    case 139360:
                        P2pStateMachine.this.replyToMessage(message, 139269, 2);
                        return true;
                    case 139361:
                        P2pStateMachine.this.replyToMessage(message, 139361, 2);
                        return true;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                        WifiP2pServiceImpl.this.mDiscoveryBlocked = message.arg1 == 1;
                        WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            try {
                                StateMachine m = (StateMachine) message.obj;
                                m.sendMessage(message.arg2);
                            } catch (Exception e) {
                                P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                            }
                            break;
                        }
                        return true;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                        P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
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
                        P2pStateMachine.this.replyToMessage(message, 139324, 1);
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
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT, WifiP2pServiceImpl.mDisableP2pTimeoutIndex++, 0), 5000L);
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P:
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT:
                        if (WifiP2pServiceImpl.mDisableP2pTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.loge("P2p disable timed out");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                            return true;
                        }
                        return true;
                    case WifiMonitor.SUP_DISCONNECTION_EVENT:
                        P2pStateMachine.this.logd("p2p socket connection lost");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
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
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P:
                        try {
                            WifiP2pServiceImpl.this.mNwService.setInterfaceUp(P2pStateMachine.this.mWifiNative.getInterfaceName());
                            break;
                        } catch (RemoteException re) {
                            P2pStateMachine.this.loge("Unable to change interface settings: " + re);
                        } catch (IllegalStateException ie) {
                            P2pStateMachine.this.loge("Unable to change interface settings: " + ie);
                        }
                        P2pStateMachine.this.mWifiMonitor.startMonitoring(P2pStateMachine.this.mWifiNative.getInterfaceName());
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pEnablingState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class P2pEnablingState extends State {
            P2pEnablingState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P:
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case WifiMonitor.SUP_CONNECTION_EVENT:
                        P2pStateMachine.this.logd("P2p socket connection successful");
                        P2pStateMachine.this.mWifiNative.startDriver();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiMonitor.SUP_DISCONNECTION_EVENT:
                        P2pStateMachine.this.loge("P2p socket connection failed");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        return true;
                    default:
                        return false;
                }
            }
        }

        class P2pEnabledState extends State {
            P2pEnabledState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.sendP2pStateChangedBroadcast(true);
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(true);
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                P2pStateMachine.this.initializeP2pSettings();
            }

            public boolean processMessage(Message message) {
                boolean retP2pFind;
                if (message.what == 143375) {
                    P2pStateMachine.this.logd("BLOCK_DISCOVERY");
                } else {
                    P2pStateMachine.this.logd(getName() + message.toString());
                }
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P:
                        return true;
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        if (P2pStateMachine.this.mPeers.clear()) {
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        if (P2pStateMachine.this.mGroups.clear()) {
                            P2pStateMachine.this.sendP2pPersistentGroupsChangedBroadcast();
                        }
                        P2pStateMachine.this.mWifiMonitor.stopMonitoring(P2pStateMachine.this.mWifiNative.getInterfaceName());
                        P2pStateMachine.this.mWifiNative.stopDriver();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisablingState);
                        return true;
                    case 139265:
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            P2pStateMachine.this.logd("DiscoveryBlocked");
                            P2pStateMachine.this.replyToMessage(message, 139266, 2);
                            return true;
                        }
                        P2pStateMachine.this.clearSupplicantServiceRequest();
                        int timeout = message.arg1;
                        if (P2pStateMachine.this.isWfdSinkEnabled()) {
                            P2pStateMachine.this.p2pConfigWfdSink();
                            retP2pFind = P2pStateMachine.this.mWifiNative.p2pFind();
                        } else if (timeout == 123) {
                            retP2pFind = P2pStateMachine.this.mWifiNative.p2pFind(123);
                        } else {
                            retP2pFind = P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S);
                        }
                        if (retP2pFind) {
                            P2pStateMachine.this.replyToMessage(message, 139267);
                            P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(true);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139266, 0);
                        return true;
                    case 139268:
                        if (P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.replyToMessage(message, 139270);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139269, 0);
                        }
                        if (P2pStateMachine.this.isWfdSinkEnabled()) {
                            P2pStateMachine.this.p2pUnconfigWfdSink();
                            return true;
                        }
                        return true;
                    case 139292:
                        P2pStateMachine.this.logd(getName() + " add service");
                        WifiP2pServiceInfo servInfo = (WifiP2pServiceInfo) message.obj;
                        if (P2pStateMachine.this.addLocalService(message.replyTo, servInfo)) {
                            P2pStateMachine.this.replyToMessage(message, 139294);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139293);
                        return true;
                    case 139295:
                        P2pStateMachine.this.logd(getName() + " remove service");
                        WifiP2pServiceInfo servInfo2 = (WifiP2pServiceInfo) message.obj;
                        P2pStateMachine.this.removeLocalService(message.replyTo, servInfo2);
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
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139303);
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
                            return true;
                        }
                        P2pStateMachine.this.logd(getName() + " discover services");
                        if (!P2pStateMachine.this.updateSupplicantServiceRequest()) {
                            P2pStateMachine.this.replyToMessage(message, 139311, 3);
                            return true;
                        }
                        if (P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S)) {
                            P2pStateMachine.this.replyToMessage(message, 139312);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139311, 0);
                        return true;
                    case 139315:
                        WifiP2pDevice d = (WifiP2pDevice) message.obj;
                        if (d != null && P2pStateMachine.this.setAndPersistDeviceName(d.deviceName)) {
                            P2pStateMachine.this.logd("set device name " + d.deviceName);
                            P2pStateMachine.this.replyToMessage(message, 139317);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139316, 0);
                        return true;
                    case 139318:
                        P2pStateMachine.this.logd(getName() + " delete persistent group");
                        P2pStateMachine.this.mGroups.remove(message.arg1);
                        P2pStateMachine.this.replyToMessage(message, 139320);
                        return true;
                    case 139323:
                        WifiP2pWfdInfo d2 = (WifiP2pWfdInfo) message.obj;
                        if (d2 != null && P2pStateMachine.this.setWfdInfo(d2)) {
                            P2pStateMachine.this.replyToMessage(message, 139325);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                        }
                        if (WifiP2pServiceImpl.this.mThisDevice.wfdInfo != null && WifiP2pServiceImpl.this.mThisDevice.wfdInfo.mCrossmountLoaned) {
                            WifiP2pServiceImpl.this.mThisDevice.wfdInfo = null;
                            P2pStateMachine.this.logd("[crossmount] reset wfd info in wifi p2p framework");
                            return true;
                        }
                        return true;
                    case 139329:
                        P2pStateMachine.this.logd(getName() + " start listen mode");
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139331);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139330);
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
                        Bundle p2pChannels = (Bundle) message.obj;
                        int lc = p2pChannels.getInt("lc", 0);
                        int oc = p2pChannels.getInt("oc", 0);
                        P2pStateMachine.this.logd(getName() + " set listen and operating channel");
                        if (P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                            P2pStateMachine.this.replyToMessage(message, 139337);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139336);
                        return true;
                    case 139339:
                        Bundle requestBundle = new Bundle();
                        requestBundle.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverRequest());
                        P2pStateMachine.this.replyToMessage(message, 139341, requestBundle);
                        return true;
                    case 139340:
                        Bundle selectBundle = new Bundle();
                        selectBundle.putString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE", P2pStateMachine.this.mWifiNative.getNfcHandoverSelect());
                        P2pStateMachine.this.replyToMessage(message, 139341, selectBundle);
                        return true;
                    case 139349:
                        WifiP2pLinkInfo info = (WifiP2pLinkInfo) message.obj;
                        info.linkInfo = P2pStateMachine.this.p2pLinkStatics(info.interfaceAddress);
                        P2pStateMachine.this.logd("Wifi P2p link info is " + info.toString());
                        P2pStateMachine.this.replyToMessage(message, 139350, new WifiP2pLinkInfo(info));
                        return true;
                    case 139351:
                        int enable = message.arg1;
                        P2pStateMachine.this.p2pAutoChannel(enable);
                        P2pStateMachine.this.replyToMessage(message, 139353);
                        return true;
                    case 139360:
                        if (P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.replyToMessage(message, 139270);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139269, 0);
                        return true;
                    case 139361:
                        P2pStateMachine.this.logd(getName() + " ADD_PERSISTENT_GROUP");
                        Bundle bVariables = (Bundle) message.obj;
                        HashMap<String, String> hVariables = (HashMap) bVariables.getSerializable("variables");
                        if (hVariables != null) {
                            WifiP2pGroup group = P2pStateMachine.this.addPersistentGroup(hVariables);
                            P2pStateMachine.this.replyToMessage(message, 139364, new WifiP2pGroup(group));
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139362, 0);
                        return true;
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE:
                        if (message.arg2 != 0) {
                            P2pStateMachine.this.mWifiNative.setMiracastMode(message.arg1, message.arg2);
                        } else {
                            P2pStateMachine.this.mWifiNative.setMiracastMode(message.arg1);
                        }
                        WifiP2pServiceImpl.this.mMiracastMode = message.arg1;
                        return true;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                        boolean blocked = message.arg1 == 1;
                        P2pStateMachine.this.logd("blocked:" + blocked + ", mDiscoveryBlocked:" + WifiP2pServiceImpl.this.mDiscoveryBlocked);
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked != blocked) {
                            WifiP2pServiceImpl.this.mDiscoveryBlocked = blocked;
                            if (blocked && WifiP2pServiceImpl.this.mDiscoveryStarted) {
                                P2pStateMachine.this.mWifiNative.p2pStopFind();
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = true;
                            }
                            if (!blocked && WifiP2pServiceImpl.this.mDiscoveryPostponed) {
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = false;
                                P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S);
                            }
                            if (blocked) {
                                try {
                                    StateMachine m = (StateMachine) message.obj;
                                    m.sendMessage(message.arg2);
                                    return true;
                                } catch (Exception e) {
                                    P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                                    return true;
                                }
                            }
                            return true;
                        }
                        return true;
                    case WifiP2pServiceImpl.SET_BEAM_MODE:
                        P2pStateMachine.this.logd(getName() + " SET_BEAM_MODE");
                        P2pStateMachine.this.setBeamMode(message.arg1);
                        return true;
                    case WifiMonitor.SUP_DISCONNECTION_EVENT:
                        P2pStateMachine.this.loge("Unexpected loss of p2p socket connection");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        return true;
                    case WifiMonitor.P2P_DEVICE_FOUND_EVENT:
                        WifiP2pDevice device = (WifiP2pDevice) message.obj;
                        if (!WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(device.deviceAddress)) {
                            P2pStateMachine.this.mPeers.updateSupplicantDetails(device);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            if (WifiP2pServiceImpl.this.mUpdatePeerForInvited && P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(device.deviceAddress)) {
                                WifiP2pServiceImpl.this.mUpdatePeerForInvited = false;
                                P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.M_P2P_DEVICE_FOUND_INVITATION);
                                return true;
                            }
                            return true;
                        }
                        return true;
                    case WifiMonitor.P2P_DEVICE_LOST_EVENT:
                        if (P2pStateMachine.this.mPeers.remove(((WifiP2pDevice) message.obj).deviceAddress) != null) {
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            return true;
                        }
                        return true;
                    case WifiMonitor.P2P_FIND_STOPPED_EVENT:
                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                        return true;
                    case WifiMonitor.P2P_SERV_DISC_RESP_EVENT:
                        P2pStateMachine.this.logd(getName() + " receive service response");
                        List<WifiP2pServiceResponse> sdRespList = (List) message.obj;
                        for (WifiP2pServiceResponse resp : sdRespList) {
                            WifiP2pDevice dev = P2pStateMachine.this.mPeers.get(resp.getSrcDevice().deviceAddress);
                            resp.setSrcDevice(dev);
                            P2pStateMachine.this.sendServiceResponse(resp);
                        }
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
                P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(false);
                P2pStateMachine.this.sendP2pStateChangedBroadcast(false);
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(false);
            }
        }

        class InactiveState extends State {
            InactiveState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139268:
                        if (!P2pStateMachine.this.mWifiNative.p2pStopFind()) {
                            P2pStateMachine.this.replyToMessage(message, 139269, 0);
                            return true;
                        }
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        P2pStateMachine.this.clearSupplicantServiceRequest();
                        P2pStateMachine.this.replyToMessage(message, 139270);
                        return true;
                    case 139271:
                        P2pStateMachine.this.logd(getName() + " sending connect:" + ((WifiP2pConfig) message.obj));
                        WifiP2pConfig config = (WifiP2pConfig) message.obj;
                        if (P2pStateMachine.this.isConfigInvalid(config)) {
                            P2pStateMachine.this.loge("Dropping connect request " + config);
                            P2pStateMachine.this.replyToMessage(message, 139272);
                            return true;
                        }
                        WifiP2pServiceImpl.this.mAutonomousGroup = false;
                        WifiP2pServiceImpl.this.mConnectToPeer = true;
                        if ((WifiP2pServiceImpl.this.mMiracastMode != 1 || WifiP2pServiceImpl.WFD_DONGLE_USE_P2P_INVITE.booleanValue()) && P2pStateMachine.this.reinvokePersistentGroup(config)) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        } else {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mProvisionDiscoveryState);
                        }
                        P2pStateMachine.this.mSavedPeerConfig = config;
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.replyToMessage(message, 139273);
                        return true;
                    case 139277:
                        WifiP2pServiceImpl.this.mAutonomousGroup = true;
                        int netId = message.arg1;
                        boolean ret = false;
                        if (netId == -2) {
                            netId = P2pStateMachine.this.mGroups.getNetworkId(WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                            ret = netId != -1 ? P2pStateMachine.this.mWifiNative.p2pGroupAdd(netId) : P2pStateMachine.this.mWifiNative.p2pGroupAdd(true);
                        }
                        if (netId <= -1 || !P2pStateMachine.this.mGroups.contains(netId)) {
                            ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(false);
                        } else if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(P2pStateMachine.this.mGroups.getOwnerAddr(netId))) {
                            ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(netId);
                        }
                        if (!ret) {
                            P2pStateMachine.this.replyToMessage(message, 139278, 0);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139279);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        return true;
                    case 139335:
                        Bundle p2pChannels = (Bundle) message.obj;
                        int lc = p2pChannels.getInt("lc", 0);
                        int oc = p2pChannels.getInt("oc", 0);
                        P2pStateMachine.this.logd(getName() + " set listen and operating channel");
                        if (P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                            P2pStateMachine.this.replyToMessage(message, 139337);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139336);
                        return true;
                    case 139342:
                        String handoverSelect = message.obj != null ? ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE") : null;
                        if (handoverSelect == null || !P2pStateMachine.this.mWifiNative.initiatorReportNfcHandover(handoverSelect)) {
                            P2pStateMachine.this.replyToMessage(message, 139345);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139344);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                        return true;
                    case 139343:
                        String handoverRequest = message.obj != null ? ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE") : null;
                        if (handoverRequest == null || !P2pStateMachine.this.mWifiNative.responderReportNfcHandover(handoverRequest)) {
                            P2pStateMachine.this.replyToMessage(message, 139345);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139344);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                        return true;
                    case WifiP2pServiceImpl.M_P2P_DEVICE_FOUND_INVITATION:
                        WifiP2pDevice owner02 = P2pStateMachine.this.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                        if (owner02 != null) {
                            if (owner02.wpsPbcSupported()) {
                                P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                            } else if (owner02.wpsKeypadSupported()) {
                                P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                            } else if (owner02.wpsDisplaySupported()) {
                                P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                            }
                        }
                        P2pStateMachine.this.updateCrossMountInfo(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                        WifiP2pServiceImpl.this.mAutonomousGroup = false;
                        WifiP2pServiceImpl.this.mJoinExistingGroup = true;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingInviteRequestState);
                        return true;
                    case WifiP2pServiceImpl.M_P2P_CONN_FOR_INVITE_RES_INFO_UNAVAILABLE:
                        if (!WifiP2pServiceImpl.this.mDelayReconnectForInfoUnavailable) {
                            return true;
                        }
                        P2pStateMachine.this.logd(getName() + " mDelayReconnectForInfoUnavailable:" + WifiP2pServiceImpl.this.mDelayReconnectForInfoUnavailable + " M_P2P_CONN_FOR_INVITE_RES_INFO_UNAVAILABLE:" + ((WifiP2pConfig) message.obj));
                        WifiP2pConfig config2 = (WifiP2pConfig) message.obj;
                        P2pStateMachine.this.mSavedPeerConfig = config2;
                        if (!P2pStateMachine.this.mPeers.containsPeer(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                            P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.M_P2P_CONN_FOR_INVITE_RES_INFO_UNAVAILABLE);
                            return true;
                        }
                        P2pStateMachine.this.p2pConnectWithPinDisplay(config2);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        return true;
                    case WifiMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT:
                        WifiP2pConfig config3 = (WifiP2pConfig) message.obj;
                        if (P2pStateMachine.this.isConfigInvalid(config3)) {
                            P2pStateMachine.this.loge("Dropping GO neg request " + config3);
                            return true;
                        }
                        P2pStateMachine.this.mSavedPeerConfig = config3;
                        WifiP2pServiceImpl.this.mAutonomousGroup = false;
                        WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                        return true;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                        P2pStateMachine.this.logd(getName() + " group started");
                        if (P2pStateMachine.this.mGroup.getNetworkId() != -2) {
                            P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                            return true;
                        }
                        WifiP2pServiceImpl.this.mAutonomousGroup = false;
                        P2pStateMachine.this.deferMessage(message);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        return true;
                    case WifiMonitor.P2P_INVITATION_RECEIVED_EVENT:
                        WifiP2pGroup group = (WifiP2pGroup) message.obj;
                        WifiP2pDevice owner = group.getOwner();
                        if (owner == null) {
                            P2pStateMachine.this.loge("Ignored invitation from null owner");
                            return true;
                        }
                        WifiP2pConfig config4 = new WifiP2pConfig();
                        config4.deviceAddress = group.getOwner().deviceAddress;
                        if (WifiP2pServiceImpl.this.mCrossmountIEAdded) {
                            P2pStateMachine.this.mWifiNative.doCustomSupplicantCommand("P2P_FIND 120 type=progressive dev_id=" + config4.deviceAddress);
                            WifiP2pServiceImpl.this.mUpdatePeerForInvited = true;
                        } else {
                            if (P2pStateMachine.this.isConfigInvalid(config4)) {
                                P2pStateMachine.this.loge("Dropping invitation request " + config4);
                                return true;
                            }
                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.M_P2P_DEVICE_FOUND_INVITATION);
                        }
                        P2pStateMachine.this.mSavedPeerConfig = config4;
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message.obj;
                        P2pStateMachine.this.updateCrossMountInfo(provDisc.device.deviceAddress);
                        if (message.what != 147492) {
                            return true;
                        }
                        P2pStateMachine.this.logd("Show PIN passively");
                        WifiP2pConfig config5 = new WifiP2pConfig();
                        config5.deviceAddress = provDisc.device.deviceAddress;
                        P2pStateMachine.this.mSavedPeerConfig = config5;
                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                        P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                        if (P2pStateMachine.this.isAppHandledConnection()) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                            return true;
                        }
                        if (P2pStateMachine.this.mPeers.get(config5.deviceAddress) == null) {
                            P2pStateMachine.this.loge("peer device is not in our scan result, drop this pd. " + config5.deviceAddress);
                            return true;
                        }
                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        P2pStateMachine.this.notifyInvitationSent(provDisc.pin, P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
                if (!WifiP2pServiceImpl.this.mDelayReconnectForInfoUnavailable) {
                    return;
                }
                P2pStateMachine.this.logd(getName() + " mDelayReconnectForInfoUnavailable:" + WifiP2pServiceImpl.this.mDelayReconnectForInfoUnavailable + ", remove M_P2P_CONN_FOR_INVITE_RES_INFO_UNAVAILABLE");
                P2pStateMachine.this.removeMessages(WifiP2pServiceImpl.M_P2P_CONN_FOR_INVITE_RES_INFO_UNAVAILABLE);
            }
        }

        class GroupCreatingState extends State {
            GroupCreatingState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT, WifiP2pServiceImpl.mGroupCreatingTimeoutIndex++, 0), 120000L);
                P2pStateMachine.this.sendP2pTxBroadcast(true);
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
                        boolean success = false;
                        if (P2pStateMachine.this.mWifiNative.p2pCancelConnect() || P2pStateMachine.this.mWifiNative.p2pGroupRemove(WifiP2pServiceImpl.this.mInterface)) {
                            success = true;
                        }
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        if (success) {
                            P2pStateMachine.this.replyToMessage(message, 139276);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139275);
                        }
                        break;
                    case 139360:
                        P2pStateMachine.this.logd("defer STOP_P2P_FIND_ONLY@GroupCreatingState");
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT:
                        if (WifiP2pServiceImpl.mGroupCreatingTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.logd("Group negotiation timed out");
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        }
                        break;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                        P2pStateMachine.this.logd("defer BLOCK_DISCOVERY@GroupCreatingState");
                        P2pStateMachine.this.deferMessage(message);
                        break;
                    case WifiMonitor.P2P_DEVICE_FOUND_EVENT:
                        WifiP2pDevice peerDevice = (WifiP2pDevice) message.obj;
                        if (!WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(peerDevice.deviceAddress)) {
                            if (P2pStateMachine.this.mSavedPeerConfig != null && P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(peerDevice.deviceAddress)) {
                                peerDevice.status = 1;
                            }
                            P2pStateMachine.this.mPeers.update(peerDevice);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        break;
                    case WifiMonitor.P2P_DEVICE_LOST_EVENT:
                        WifiP2pDevice device = (WifiP2pDevice) message.obj;
                        if (!P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(device.deviceAddress)) {
                            P2pStateMachine.this.logd("mSavedPeerConfig " + P2pStateMachine.this.mSavedPeerConfig.deviceAddress + "device " + device.deviceAddress);
                        } else {
                            P2pStateMachine.this.logd("Add device to lost list " + device);
                            P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                        }
                        break;
                    case WifiMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                        WifiP2pServiceImpl.this.mAutonomousGroup = false;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case WifiMonitor.P2P_FIND_STOPPED_EVENT:
                        P2pStateMachine.this.logd("defer P2P_FIND_STOPPED_EVENT@GroupCreatingState");
                        P2pStateMachine.this.deferMessage(message);
                        break;
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
                } else if (WifiP2pServiceImpl.this.mCrossmountEventReceived) {
                    P2pStateMachine.this.sendP2pCrossmountIntentionBroadcast();
                } else {
                    P2pStateMachine.this.notifyInvitationReceived();
                }
            }

            public boolean processMessage(Message message) {
                WifiP2pDevice peerDevice02;
                WifiInfo wifiInfo;
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139354:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        if (message.what == 139354 && P2pStateMachine.this.isAppHandledConnection()) {
                            P2pStateMachine.this.p2pUserAuthPreprocess(message);
                            P2pStateMachine.this.p2pOverwriteWpsPin("[crossmount] USER_ACCEPT@UserAuthorizingNegotiationRequestState", message.obj);
                        }
                        P2pStateMachine.this.logd("User accept negotiation: mSavedPeerConfig = " + P2pStateMachine.this.mSavedPeerConfig);
                        if (WifiP2pServiceImpl.this.mNegoChannelConflict) {
                            WifiP2pServiceImpl.this.mNegoChannelConflict = false;
                            P2pStateMachine.this.logd("PEER_CONNECTION_USER_ACCEPT_FROM_OUTER,switch to FrequencyConflictState");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                        } else {
                            P2pStateMachine.this.logd("isWfdSinkEnabled()=" + P2pStateMachine.this.isWfdSinkEnabled());
                            if (P2pStateMachine.this.isWfdSinkEnabled() && (wifiInfo = P2pStateMachine.this.getWifiConnectionInfo()) != null) {
                                P2pStateMachine.this.logd("wifiInfo=" + wifiInfo);
                                if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                                    P2pStateMachine.this.logd("wifiInfo.getSupplicantState() == SupplicantState.COMPLETED");
                                    P2pStateMachine.this.logd("wifiInfo.getFrequency()=" + wifiInfo.getFrequency());
                                    P2pStateMachine.this.mSavedPeerConfig.setPreferOperFreq(wifiInfo.getFrequency());
                                }
                            }
                            P2pStateMachine.this.p2pUpdateScanList(P2pStateMachine.this.mSavedPeerConfig.deviceAddress);
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
                        if (P2pStateMachine.this.mSavedPeerConfig != null && (peerDevice02 = P2pStateMachine.this.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) != null && peerDevice02.status == 1) {
                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 3);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                        if (message.arg1 != 0) {
                            WifiP2pServiceImpl.this.mP2pOperFreq = message.arg1;
                        }
                        P2pStatus status = (P2pStatus) message.obj;
                        P2pStateMachine.this.loge("go negotiation failed@UserAuthorizingNegotiationRequestState, status = " + status + "\tmP2pOperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
                        if (status == P2pStatus.NO_COMMON_CHANNEL) {
                            WifiP2pServiceImpl.this.mNegoChannelConflict = true;
                        } else {
                            P2pStateMachine.this.loge("other kinds of negotiation errors");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        }
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
                if (P2pStateMachine.this.isWfdSinkEnabled()) {
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                } else if (WifiP2pServiceImpl.this.mCrossmountEventReceived) {
                    P2pStateMachine.this.sendP2pCrossmountIntentionBroadcast();
                } else {
                    P2pStateMachine.this.notifyInvitationReceived();
                }
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139354:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        if (message.what == 139354 && WifiP2pServiceImpl.this.mCrossmountEventReceived) {
                            P2pStateMachine.this.p2pUserAuthPreprocess(message);
                            P2pStateMachine.this.p2pOverwriteWpsPin("[crossmount] USER_ACCEPT@UserAuthorizingInviteRequestState", message.obj);
                        }
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
                switch (message.what) {
                    case 139274:
                        boolean success = P2pStateMachine.this.mWifiNative.p2pGroupRemove(WifiP2pServiceImpl.this.mInterface);
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        if (success) {
                            P2pStateMachine.this.replyToMessage(message, 139276);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139275);
                        }
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_PBC_RSP_EVENT:
                        WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message.obj;
                        if (provDisc.device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                            P2pStateMachine.this.updateCrossMountInfo(provDisc.device.deviceAddress);
                            if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                                P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            }
                        }
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                        WifiP2pProvDiscEvent provDisc2 = (WifiP2pProvDiscEvent) message.obj;
                        if (provDisc2.device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                            P2pStateMachine.this.updateCrossMountInfo(provDisc2.device.deviceAddress);
                            if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 2) {
                                P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                if (!TextUtils.isEmpty(P2pStateMachine.this.mSavedPeerConfig.wps.pin)) {
                                    P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                } else {
                                    WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                                }
                            }
                        }
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        WifiP2pProvDiscEvent provDisc3 = (WifiP2pProvDiscEvent) message.obj;
                        WifiP2pDevice device = provDisc3.device;
                        if (device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                            P2pStateMachine.this.updateCrossMountInfo(provDisc3.device.deviceAddress);
                            if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 1) {
                                P2pStateMachine.this.logd("Found a match " + P2pStateMachine.this.mSavedPeerConfig);
                                if (!WifiP2pServiceImpl.this.mCrossmountEventReceived) {
                                    P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc3.pin;
                                    P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                    P2pStateMachine.this.notifyInvitationSent(provDisc3.pin, device.deviceAddress);
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                                } else {
                                    P2pStateMachine.this.logd("[crossmount] PD rsp: SHOW_PIN, move process to UserAuthorizingNegotiationRequestState");
                                    WifiP2pServiceImpl.this.mJoinExistingGroup = false;
                                    P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                                }
                            }
                        }
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_FAILURE_EVENT:
                        P2pStateMachine.this.loge("provision discovery failed");
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    default:
                        return false;
                }
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
                    case WifiMonitor.WPS_OVERLAP_EVENT:
                        Toast.makeText(WifiP2pServiceImpl.this.mContext, 134545543, 0).show();
                        return true;
                    case WifiMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                    case WifiMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT:
                        P2pStateMachine.this.logd(getName() + " go success");
                        return true;
                    case WifiMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                        if (message.arg1 != 0) {
                            WifiP2pServiceImpl.this.mP2pOperFreq = message.arg1;
                        }
                        P2pStatus status = (P2pStatus) message.obj;
                        P2pStateMachine.this.loge("go negotiation failed, status = " + status + "\tmP2pOperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
                        if (status == P2pStatus.NO_COMMON_CHANNEL) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                            return true;
                        }
                        break;
                    case WifiMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                        if (message.arg1 != 0) {
                            WifiP2pServiceImpl.this.mP2pOperFreq = message.arg1;
                        }
                        P2pStatus status2 = (P2pStatus) message.obj;
                        P2pStateMachine.this.loge("group formation failed, status = " + status2 + "\tmP2pOperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
                        if (status2 == P2pStatus.NO_COMMON_CHANNEL) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                            return true;
                        }
                        return true;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                        P2pStateMachine.this.logd(getName() + " group started");
                        if (P2pStateMachine.this.mGroup.getNetworkId() == -2) {
                            P2pStateMachine.this.updatePersistentNetworks(WifiP2pServiceImpl.NO_RELOAD.booleanValue());
                            String devAddr = P2pStateMachine.this.mGroup.getOwner().deviceAddress;
                            P2pStateMachine.this.mGroup.setNetworkId(P2pStateMachine.this.mGroups.getNetworkId(devAddr, P2pStateMachine.this.mGroup.getNetworkName()));
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
                            if (WifiP2pServiceImpl.this.mGcIgnoresDhcpReq) {
                                P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS));
                                String intf = P2pStateMachine.this.mGroup.getInterface();
                                try {
                                    InterfaceConfiguration ifcg = WifiP2pServiceImpl.this.mNwService.getInterfaceConfig(intf);
                                    ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.STATIC_CLIENT_ADDRESS), 24));
                                    ifcg.setInterfaceUp();
                                    WifiP2pServiceImpl.this.mNwService.setInterfaceConfig(intf, ifcg);
                                    StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
                                    staticIpConfiguration.ipAddress = new LinkAddress(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.STATIC_CLIENT_ADDRESS), 24);
                                    WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork(intf, staticIpConfiguration.getRoutes(intf));
                                } catch (RemoteException re) {
                                    P2pStateMachine.this.loge("Error! Configuring static IP to " + intf + ", :" + re);
                                }
                            } else {
                                P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                                WifiP2pServiceImpl.this.startIpManager(P2pStateMachine.this.mGroup.getInterface());
                            }
                            P2pStateMachine.this.setP2pPowerSaveMtk(P2pStateMachine.this.mGroup.getInterface(), 2);
                            WifiP2pDevice groupOwner = P2pStateMachine.this.mGroup.getOwner();
                            WifiP2pDevice peer = P2pStateMachine.this.mPeers.get(groupOwner.deviceAddress);
                            if (peer != null) {
                                groupOwner.updateSupplicantDetails(peer);
                                P2pStateMachine.this.mPeers.updateStatus(groupOwner.deviceAddress, 0);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                            } else {
                                P2pStateMachine.this.logw("Unknown group owner " + groupOwner);
                            }
                            break;
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        return true;
                    case WifiMonitor.P2P_GROUP_REMOVED_EVENT:
                        break;
                    case WifiMonitor.P2P_INVITATION_RESULT_EVENT:
                        P2pStatus status3 = (P2pStatus) message.obj;
                        if (status3 != P2pStatus.SUCCESS) {
                            P2pStateMachine.this.loge("Invitation result " + status3);
                            if (status3 == P2pStatus.UNKNOWN_P2P_GROUP) {
                                int netId = P2pStateMachine.this.mSavedPeerConfig.netId;
                                if (netId >= 0) {
                                    P2pStateMachine.this.logd("Remove unknown client from the list");
                                    P2pStateMachine.this.removeClientFromList(netId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, true);
                                }
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                return true;
                            }
                            if (status3 == P2pStatus.INFORMATION_IS_CURRENTLY_UNAVAILABLE) {
                                if (WifiP2pServiceImpl.this.mDelayReconnectForInfoUnavailable) {
                                    P2pStateMachine.this.logd(getName() + " mDelayReconnectForInfoUnavailable:" + WifiP2pServiceImpl.this.mDelayReconnectForInfoUnavailable);
                                    WifiP2pDevice dev = P2pStateMachine.this.fetchCurrentDeviceDetails(P2pStateMachine.this.mSavedPeerConfig);
                                    if ((dev.groupCapability & 32) == 0) {
                                        P2pStateMachine.this.logd(getName() + "Persistent Reconnect=0, wait for peer re-invite or reconnect peer 120s later");
                                        WifiP2pConfig peerConfig = new WifiP2pConfig(P2pStateMachine.this.mSavedPeerConfig);
                                        P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.M_P2P_CONN_FOR_INVITE_RES_INFO_UNAVAILABLE, peerConfig), 120000L);
                                        if (P2pStateMachine.this.mWifiNative.p2pFind(WifiP2pServiceImpl.DISCOVER_TIMEOUT_S)) {
                                            P2pStateMachine.this.logd(getName() + "Sart p2pFind for waiting peer invitation");
                                            P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(true);
                                        }
                                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                                        return true;
                                    }
                                    P2pStateMachine.this.logd(getName() + "Persistent Reconnect=1, connect to peer directly");
                                }
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                return true;
                            }
                            if (status3 == P2pStatus.NO_COMMON_CHANNEL) {
                                if (message.arg1 != 0) {
                                    WifiP2pServiceImpl.this.mP2pOperFreq = message.arg1;
                                }
                                P2pStateMachine.this.logd("Invitation mP2pOperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                                return true;
                            }
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            return true;
                        }
                        return true;
                    default:
                        return false;
                }
                P2pStateMachine.this.logd(getName() + " go failure");
                P2pStateMachine.this.handleGroupCreationFailure();
                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                return true;
            }
        }

        class FrequencyConflictState extends State {
            private AlertDialog mFrequencyConflictDialog;

            FrequencyConflictState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                if (WifiP2pServiceImpl.this.mMccSupport) {
                    P2pStateMachine.this.p2pSetCCMode(1);
                }
                if (!WifiP2pServiceImpl.this.mMccSupport && WifiP2pServiceImpl.this.mMiracastMode == 1) {
                    P2pStateMachine.this.sendP2pOPChannelBroadcast();
                    return;
                }
                if (WifiP2pServiceImpl.this.mMiracastMode == 2) {
                    P2pStateMachine.this.logd("[sink] channel conflict, disconnecting wifi by app layer");
                    P2pStateMachine.this.sendMessage(139356, 1);
                    return;
                }
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
            }

            private void notifyFrequencyConflict() {
                P2pStateMachine.this.logd("Notify frequency conflict");
                Resources r = Resources.getSystem();
                AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setMessage(r.getString(R.string.face_acquired_recalibrate, P2pStateMachine.this.getDeviceName(P2pStateMachine.this.mSavedPeerConfig.deviceAddress))).setPositiveButton(r.getString(R.string.face_error_vendor_unknown), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog2, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT);
                    }
                }).setNegativeButton(r.getString(R.string.face_acquired_insufficient), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog2, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).create();
                dialog.getWindow().setType(2003);
                WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
                attrs.privateFlags = 16;
                dialog.getWindow().setAttributes(attrs);
                dialog.show();
                this.mFrequencyConflictDialog = dialog;
            }

            private void notifyFrequencyConflictEx() {
                P2pStateMachine.this.logd("Notify frequency conflict enhancement! mP2pOperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
                Resources r = Resources.getSystem();
                String localFreq = "";
                if (WifiP2pServiceImpl.this.mP2pOperFreq > 0) {
                    localFreq = WifiP2pServiceImpl.this.mP2pOperFreq < 5000 ? "2.4G band-" + new String("" + WifiP2pServiceImpl.this.mP2pOperFreq) + " MHz" : "5G band-" + new String("" + WifiP2pServiceImpl.this.mP2pOperFreq) + " MHz";
                } else {
                    P2pStateMachine.this.loge(getName() + " in-valid OP channel: " + WifiP2pServiceImpl.this.mP2pOperFreq);
                }
                AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setMessage(r.getString(134545665, P2pStateMachine.this.getDeviceName(P2pStateMachine.this.mSavedPeerConfig.deviceAddress), localFreq)).setPositiveButton(r.getString(R.string.face_error_vendor_unknown), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog2, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT);
                    }
                }).setNegativeButton(r.getString(R.string.face_acquired_insufficient), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog2, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface arg0) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_REJECT);
                    }
                }).create();
                dialog.getWindow().setType(2003);
                dialog.show();
                this.mFrequencyConflictDialog = dialog;
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139356:
                        int accept = message.arg1;
                        P2pStateMachine.this.logd(getName() + " frequency confliect enhancement decision: " + accept + ", and mP2pOperFreq = " + WifiP2pServiceImpl.this.mP2pOperFreq);
                        if (1 == accept) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                            P2pStateMachine.this.mSavedPeerConfig.setPreferOperFreq(WifiP2pServiceImpl.this.mP2pOperFreq);
                            P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                        } else {
                            notifyFrequencyConflictEx();
                        }
                        return true;
                    case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT:
                        WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 1);
                        WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = true;
                        return true;
                    case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT:
                        WifiP2pServiceImpl.this.mGroupRemoveReason = P2pStatus.MTK_EXPAND_02;
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE:
                        P2pStateMachine.this.logd(getName() + "Wifi disconnected, retry p2p");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        WifiP2pServiceImpl.this.mP2pOperFreq = -1;
                        P2pStateMachine.this.mSavedPeerConfig.setPreferOperFreq(WifiP2pServiceImpl.this.mP2pOperFreq);
                        P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                        return true;
                    case WifiMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                    case WifiMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT:
                        P2pStateMachine.this.loge(getName() + "group sucess during freq conflict!");
                        return true;
                    case WifiMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                    case WifiMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                    case WifiMonitor.P2P_GROUP_REMOVED_EVENT:
                        return true;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.loge(getName() + "group started after freq conflict, handle anyway");
                        P2pStateMachine.this.deferMessage(message);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        return true;
                    default:
                        return false;
                }
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
                if (!WifiP2pServiceImpl.this.mGcIgnoresDhcpReq) {
                    return;
                }
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        P2pStateMachine.this.sendMessage(139280);
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case 139265:
                        P2pStateMachine.this.clearSupplicantServiceRequest();
                        if (!P2pStateMachine.this.mWifiNative.p2pFind(25)) {
                            P2pStateMachine.this.replyToMessage(message, 139266, 0);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139267);
                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(true);
                        return true;
                    case 139271:
                        WifiP2pConfig config = (WifiP2pConfig) message.obj;
                        if (P2pStateMachine.this.isConfigInvalid(config)) {
                            P2pStateMachine.this.loge("Dropping connect requeset " + config);
                            P2pStateMachine.this.replyToMessage(message, 139272);
                            return true;
                        }
                        P2pStateMachine.this.logd("Inviting device : " + config.deviceAddress);
                        P2pStateMachine.this.mSavedPeerConfig = config;
                        WifiP2pServiceImpl.this.mConnectToPeer = true;
                        if (!P2pStateMachine.this.mWifiNative.p2pInvite(P2pStateMachine.this.mGroup, config.deviceAddress)) {
                            P2pStateMachine.this.replyToMessage(message, 139272, 0);
                            return true;
                        }
                        P2pStateMachine.this.mPeers.updateStatus(config.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.replyToMessage(message, 139273);
                        return true;
                    case 139280:
                        P2pStateMachine.this.logd(getName() + " remove group");
                        if (P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface())) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mOngoingGroupRemovalState);
                            P2pStateMachine.this.replyToMessage(message, 139282);
                            return true;
                        }
                        P2pStateMachine.this.handleGroupRemoved();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        P2pStateMachine.this.replyToMessage(message, 139281, 0);
                        return true;
                    case 139326:
                        WpsInfo wps = (WpsInfo) message.obj;
                        if (wps == null) {
                            P2pStateMachine.this.replyToMessage(message, 139327);
                            return true;
                        }
                        boolean ret = true;
                        if (wps.setup != 0) {
                            if (wps.pin == null) {
                                String pin = P2pStateMachine.this.mWifiNative.startWpsPinDisplay(P2pStateMachine.this.mGroup.getInterface());
                                try {
                                    Integer.parseInt(pin);
                                    P2pStateMachine.this.notifyInvitationSent(pin, WifiLastResortWatchdog.BSSID_ANY);
                                } catch (NumberFormatException e) {
                                    ret = false;
                                }
                            } else {
                                ret = P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), wps.pin);
                            }
                            break;
                        } else {
                            ret = P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                        }
                        P2pStateMachine.this.replyToMessage(message, ret ? 139328 : 139327);
                        return true;
                    case 139357:
                        String mac = message.obj != null ? ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_CLIENT_MESSAGE") : null;
                        P2pStateMachine.this.logd("remove client, am I GO? " + P2pStateMachine.this.mGroup.getOwner().deviceAddress.equals(WifiP2pServiceImpl.this.mThisDevice.deviceAddress) + ", ths client is " + mac);
                        if (!P2pStateMachine.this.mGroup.getOwner().deviceAddress.equals(WifiP2pServiceImpl.this.mThisDevice.deviceAddress)) {
                            P2pStateMachine.this.replyToMessage(message, 139358, 0);
                            return true;
                        }
                        if (P2pStateMachine.this.p2pRemoveClient(P2pStateMachine.this.mGroup.getInterface(), mac)) {
                            P2pStateMachine.this.replyToMessage(message, 139359);
                            return true;
                        }
                        P2pStateMachine.this.replyToMessage(message, 139358, 0);
                        return true;
                    case WifiP2pServiceImpl.IPM_PRE_DHCP_ACTION:
                        P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), false);
                        WifiP2pServiceImpl.this.mIpManager.completedPreDhcpAction();
                        return true;
                    case WifiP2pServiceImpl.IPM_POST_DHCP_ACTION:
                        return true;
                    case WifiP2pServiceImpl.IPM_DHCP_RESULTS:
                        WifiP2pServiceImpl.this.mDhcpResults = (DhcpResults) message.obj;
                        return true;
                    case WifiP2pServiceImpl.IPM_PROVISIONING_SUCCESS:
                        P2pStateMachine.this.logd("mDhcpResults: " + WifiP2pServiceImpl.this.mDhcpResults);
                        P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(WifiP2pServiceImpl.this.mDhcpResults.serverAddress);
                        try {
                            String ifname = P2pStateMachine.this.mGroup.getInterface();
                            WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork(ifname, WifiP2pServiceImpl.this.mDhcpResults.getRoutes(ifname));
                            break;
                        } catch (RemoteException e2) {
                            P2pStateMachine.this.loge("Failed to add iface to local network " + e2);
                        } catch (IllegalStateException ie) {
                            P2pStateMachine.this.loge("Failed to add iface to local network: IllegalStateException=" + ie);
                        }
                        if (WifiP2pServiceImpl.this.mDhcpResults != null) {
                            if (WifiP2pServiceImpl.this.mDhcpResults.serverAddress == null || !WifiP2pServiceImpl.this.mDhcpResults.serverAddress.toString().startsWith("/")) {
                                P2pStateMachine.this.mGroup.getOwner().deviceIP = "" + WifiP2pServiceImpl.this.mDhcpResults.serverAddress;
                            } else {
                                P2pStateMachine.this.mGroup.getOwner().deviceIP = WifiP2pServiceImpl.this.mDhcpResults.serverAddress.toString().substring(1);
                            }
                        }
                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                        return true;
                    case WifiP2pServiceImpl.IPM_PROVISIONING_FAILURE:
                        P2pStateMachine.this.loge("IP provisioning failed");
                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        return true;
                    case WifiMonitor.SUP_DISCONNECTION_EVENT:
                        P2pStateMachine.this.loge("Supplicant close unexpected, send fake Group Remove event");
                        P2pStateMachine.this.sendMessage(WifiMonitor.P2P_GROUP_REMOVED_EVENT);
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case WifiMonitor.P2P_DEVICE_FOUND_EVENT:
                        WifiP2pDevice peerDevice = (WifiP2pDevice) message.obj;
                        if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(peerDevice.deviceAddress)) {
                            return true;
                        }
                        if (P2pStateMachine.this.mGroup.contains(peerDevice)) {
                            peerDevice.status = 0;
                        }
                        P2pStateMachine.this.mPeers.update(peerDevice);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        return true;
                    case WifiMonitor.P2P_DEVICE_LOST_EVENT:
                        WifiP2pDevice device = (WifiP2pDevice) message.obj;
                        if (!P2pStateMachine.this.mGroup.contains(device)) {
                            return false;
                        }
                        P2pStateMachine.this.logd("Add device to lost list " + device);
                        P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                        return true;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.loge("Duplicate group creation event notice, ignore");
                        return true;
                    case WifiMonitor.P2P_GROUP_REMOVED_EVENT:
                        if (message.arg1 != 0) {
                            WifiP2pServiceImpl.this.mP2pOperFreq = message.arg1;
                        }
                        WifiP2pServiceImpl.this.mGroupRemoveReason = (P2pStatus) message.obj;
                        P2pStateMachine.this.logd(getName() + " group removed, reason: " + WifiP2pServiceImpl.this.mGroupRemoveReason + ", mP2pOperFreq: " + WifiP2pServiceImpl.this.mP2pOperFreq);
                        P2pStateMachine.this.handleGroupRemoved();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiMonitor.P2P_INVITATION_RESULT_EVENT:
                        P2pStatus status = (P2pStatus) message.obj;
                        P2pStateMachine.this.logd("===> INVITATION RESULT EVENT : " + status + ",\tis GO ? : " + P2pStateMachine.this.mGroup.getOwner().deviceAddress.equals(WifiP2pServiceImpl.this.mThisDevice.deviceAddress));
                        boolean inviteDone = status == P2pStatus.SUCCESS;
                        P2pStateMachine.this.loge("Invitation result " + status + ",\tis GO ? : " + P2pStateMachine.this.mGroup.getOwner().deviceAddress.equals(WifiP2pServiceImpl.this.mThisDevice.deviceAddress));
                        if (status == P2pStatus.UNKNOWN_P2P_GROUP) {
                            int netId = P2pStateMachine.this.mGroup.getNetworkId();
                            if (netId >= 0) {
                                P2pStateMachine.this.logd("Remove unknown client from the list");
                                if (!P2pStateMachine.this.removeClientFromList(netId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, false)) {
                                    P2pStateMachine.this.loge("Already removed the client, ignore");
                                    return true;
                                }
                                P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                            }
                        } else {
                            if (status == P2pStatus.NO_COMMON_CHANNEL && WifiP2pServiceImpl.this.mMccSupport) {
                                P2pStateMachine.this.p2pSetCCMode(1);
                            }
                            inviteDone = true;
                        }
                        if (!inviteDone || P2pStateMachine.this.mGroup.getOwner().deviceAddress.equals(WifiP2pServiceImpl.this.mThisDevice.deviceAddress) || !P2pStateMachine.this.mPeers.remove(P2pStateMachine.this.mPeers.get(P2pStateMachine.this.mSavedPeerConfig.deviceAddress))) {
                            return true;
                        }
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message.obj;
                        if (!TextUtils.isEmpty(provDisc.device.deviceName)) {
                            P2pStateMachine.this.mPeers.update(provDisc.device);
                        }
                        P2pStateMachine.this.updateCrossMountInfo(provDisc.device.deviceAddress);
                        P2pStateMachine.this.mSavedPeerConfig = new WifiP2pConfig();
                        P2pStateMachine.this.mSavedPeerConfig.deviceAddress = provDisc.device.deviceAddress;
                        if (message.what == 147491) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                        } else if (message.what == 147492) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                            P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                        } else {
                            P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingJoinState);
                        return true;
                    case WifiMonitor.P2P_PEER_DISCONNECT_EVENT:
                        int IEEE802_11_ReasonCode = -1;
                        if (message.obj != null) {
                            try {
                                IEEE802_11_ReasonCode = Integer.valueOf((String) message.obj).intValue();
                                if (IEEE802_11_ReasonCode == WifiP2pServiceImpl.VENDOR_IE_ALL_FRAME_TAG) {
                                    WifiP2pServiceImpl.this.mGroupRemoveReason = P2pStatus.NO_COMMON_CHANNEL;
                                }
                            } catch (NumberFormatException e3) {
                                P2pStateMachine.this.loge("Error! Format unexpected");
                            }
                            break;
                        }
                        if (message.arg1 != 0) {
                            WifiP2pServiceImpl.this.mP2pOperFreq = message.arg1;
                        }
                        P2pStateMachine.this.loge(getName() + " I'm GC and has been disconnected by GO. IEEE 802.11 reason code: " + IEEE802_11_ReasonCode + ", mP2pOperFreq: " + WifiP2pServiceImpl.this.mP2pOperFreq);
                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        P2pStateMachine.this.handleGroupRemoved();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiMonitor.AP_STA_DISCONNECTED_EVENT:
                        WifiP2pDevice device2 = (WifiP2pDevice) message.obj;
                        String deviceAddress = device2.deviceAddress;
                        if (deviceAddress == null) {
                            P2pStateMachine.this.loge("Disconnect on unknown device: " + device2);
                            return true;
                        }
                        P2pStateMachine.this.mPeers.updateStatus(deviceAddress, 3);
                        if (P2pStateMachine.this.mGroup.removeClient(deviceAddress)) {
                            P2pStateMachine.this.logd("Removed client " + deviceAddress);
                            if (WifiP2pServiceImpl.this.mAutonomousGroup || !P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                            } else {
                                P2pStateMachine.this.logd("Client list empty, remove non-persistent p2p group");
                                P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                            }
                        } else {
                            P2pStateMachine.this.logd("Failed to remove client " + deviceAddress);
                            for (WifiP2pDevice c : P2pStateMachine.this.mGroup.getClientList()) {
                                P2pStateMachine.this.logd("client " + c.deviceAddress);
                            }
                        }
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.logd(getName() + " ap sta disconnected");
                        return true;
                    case WifiMonitor.AP_STA_CONNECTED_EVENT:
                        WifiP2pDevice device3 = (WifiP2pDevice) message.obj;
                        String deviceAddress2 = device3.deviceAddress;
                        P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 0);
                        if (deviceAddress2 != null) {
                            if (P2pStateMachine.this.mPeers.get(deviceAddress2) != null) {
                                P2pStateMachine.this.mGroup.addClient(P2pStateMachine.this.mPeers.get(deviceAddress2));
                            } else {
                                device3 = P2pStateMachine.this.p2pGoGetSta(device3, deviceAddress2);
                                P2pStateMachine.this.mGroup.addClient(device3);
                                P2pStateMachine.this.mPeers.update(device3);
                            }
                            WifiP2pDevice gcLocal = P2pStateMachine.this.mPeers.get(deviceAddress2);
                            gcLocal.interfaceAddress = device3.interfaceAddress;
                            P2pStateMachine.this.mPeers.update(gcLocal);
                            P2pStateMachine.this.mPeers.updateStatus(deviceAddress2, 0);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            if (P2pStateMachine.this.isWfdSinkConnected()) {
                                P2pStateMachine.this.logd(getName() + " [wfd sink] stop scan@GO, to avoid packet lost");
                                P2pStateMachine.this.mWifiNative.p2pStopFind();
                            }
                        } else {
                            P2pStateMachine.this.loge("Connect on null device address, ignore");
                        }
                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                        return true;
                    default:
                        return false;
                }
            }

            public void exit() {
                P2pStateMachine.this.updateThisDevice(3);
                P2pStateMachine.this.resetWifiP2pInfo();
                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
                if (P2pStateMachine.this.mGroup != null) {
                    P2pStateMachine.this.logd("[wfd sink/source] [crossmount]  {1} isGroupOwner: " + P2pStateMachine.this.mGroup.isGroupOwner() + " {2} getClientAmount: " + P2pStateMachine.this.mGroup.getClientAmount() + " {3} isGroupRemoved(): " + P2pStateMachine.this.isGroupRemoved() + " {4} mCrossmountIEAdded: " + WifiP2pServiceImpl.this.mCrossmountIEAdded);
                }
                if (P2pStateMachine.this.isWfdSinkConnected()) {
                    P2pStateMachine.this.logd("[wfd sink/source] don't bother wfd framework, case 1");
                    return;
                }
                if (P2pStateMachine.this.isWfdSourceConnected()) {
                    P2pStateMachine.this.logd("[wfd sink/source] don't bother wfd framework, case 2");
                } else if (P2pStateMachine.this.isCrossMountGOwithMultiGC()) {
                    P2pStateMachine.this.logd("[crossmount] don't bother crossmount framework, case 3");
                } else {
                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast(WifiP2pServiceImpl.this.mGroupRemoveReason);
                }
            }
        }

        class UserAuthorizingJoinState extends State {
            UserAuthorizingJoinState() {
            }

            public void enter() {
                P2pStateMachine.this.logd(getName());
                if (P2pStateMachine.this.isWfdSinkEnabled()) {
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                } else if (WifiP2pServiceImpl.this.mCrossmountEventReceived) {
                    P2pStateMachine.this.sendP2pCrossmountIntentionBroadcast();
                } else {
                    P2pStateMachine.this.notifyInvitationReceived();
                }
            }

            public boolean processMessage(Message message) {
                P2pStateMachine.this.logd(getName() + message.toString());
                switch (message.what) {
                    case 139354:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        if (message.what == 139354) {
                            P2pStateMachine.this.p2pOverwriteWpsPin("[crossmount] USER_ACCEPT@UserAuthorizingJoinState", message.obj);
                        }
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                            P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                            return true;
                        }
                        P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), P2pStateMachine.this.mSavedPeerConfig.wps.pin);
                        return true;
                    case 139355:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                        P2pStateMachine.this.logd("User rejected incoming request");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        return true;
                    case WifiMonitor.WPS_FAIL_EVENT:
                    case WifiMonitor.WPS_OVERLAP_EVENT:
                    case WifiMonitor.WPS_TIMEOUT_EVENT:
                        P2pStateMachine.this.logd("incoming request connect failed!");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        return true;
                    case WifiMonitor.AP_STA_CONNECTED_EVENT:
                        P2pStateMachine.this.logd("incoming request is connected!");
                        P2pStateMachine.this.deferMessage(message);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
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
                switch (message.what) {
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139282);
                        return true;
                    case 139357:
                        P2pStateMachine.this.replyToMessage(message, 139359);
                        return true;
                    default:
                        return false;
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            super.dump(fd, pw, args);
            pw.println("mWifiP2pInfo " + this.mWifiP2pInfo);
            pw.println("mGroup " + this.mGroup);
            pw.println("mSavedPeerConfig " + this.mSavedPeerConfig);
            pw.println();
        }

        private void sendP2pStateChangedBroadcast(boolean enabled) {
            Intent intent = new Intent("android.net.wifi.p2p.STATE_CHANGED");
            intent.addFlags(67108864);
            if (enabled) {
                intent.putExtra("wifi_p2p_state", 2);
            } else {
                intent.putExtra("wifi_p2p_state", 1);
            }
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pDiscoveryChangedBroadcast(boolean started) {
            int i;
            if (WifiP2pServiceImpl.this.mDiscoveryStarted == started) {
                return;
            }
            WifiP2pServiceImpl.this.mDiscoveryStarted = started;
            logd("discovery change broadcast " + started);
            Intent intent = new Intent("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
            intent.addFlags(67108864);
            if (started) {
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
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pConnectionChangedBroadcast() {
            logd("sending p2p connection changed broadcast, mGroup: " + this.mGroup);
            Intent intent = new Intent("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("wifiP2pInfo", new WifiP2pInfo(this.mWifiP2pInfo));
            intent.putExtra("networkInfo", new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            intent.putExtra("p2pGroupInfo", new WifiP2pGroup(this.mGroup));
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_CONNECTION_CHANGED, new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
        }

        private void sendP2pPersistentGroupsChangedBroadcast() {
            logd("sending p2p persistent groups changed broadcast");
            Intent intent = new Intent("android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED");
            intent.addFlags(67108864);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void startDhcpServer(String intf) {
            try {
                InterfaceConfiguration ifcg = WifiP2pServiceImpl.this.mNwService.getInterfaceConfig(intf);
                ifcg.setLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS), 24));
                ifcg.setInterfaceUp();
                WifiP2pServiceImpl.this.mNwService.setInterfaceConfig(intf, ifcg);
                ConnectivityManager cm = (ConnectivityManager) WifiP2pServiceImpl.this.mContext.getSystemService("connectivity");
                String[] tetheringDhcpRanges = cm.getTetheredDhcpRanges();
                if (WifiP2pServiceImpl.this.mNwService.isTetheringStarted()) {
                    logd("Stop existing tethering and restart it");
                    WifiP2pServiceImpl.this.mNwService.stopTethering();
                }
                WifiP2pServiceImpl.this.mNwService.tetherInterface(intf);
                WifiP2pServiceImpl.this.mNwService.startTethering(tetheringDhcpRanges);
                logd("Started Dhcp server on " + intf);
            } catch (Exception e) {
                loge("Error configuring interface " + intf + ", :" + e);
            }
        }

        private void stopDhcpServer(String intf) {
            try {
                WifiP2pServiceImpl.this.mNwService.untetherInterface(intf);
                for (String temp : WifiP2pServiceImpl.this.mNwService.listTetheredInterfaces()) {
                    logd("List all interfaces " + temp);
                    if (temp.compareTo(intf) != 0) {
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
            Resources r = Resources.getSystem();
            AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(R.string.ext_media_unmounting_notification_title)).setMessage(r.getString(R.string.ext_media_unsupported_notification_title)).setPositiveButton(r.getString(R.string.ok), (DialogInterface.OnClickListener) null).create();
            dialog.getWindow().setType(2003);
            WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void addRowToDialog(ViewGroup group, int stringId, String value) {
            Resources r = Resources.getSystem();
            View row = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.preference_child_holo, group, false);
            ((TextView) row.findViewById(R.id.option1)).setText(r.getString(stringId));
            ((TextView) row.findViewById(R.id.input_method_nav_ends_group)).setText(value);
            group.addView(row);
        }

        private void notifyInvitationSent(String pin, String peerAddress) {
            Resources r = Resources.getSystem();
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.preference_child, (ViewGroup) null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(R.id.opaque);
            addRowToDialog(group, R.string.face_acquired_obscured, getDeviceName(peerAddress));
            addRowToDialog(group, R.string.face_acquired_poor_gaze, pin);
            AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(R.string.face_acquired_mouth_covering_detected)).setView(textEntryView).setPositiveButton(r.getString(R.string.ok), (DialogInterface.OnClickListener) null).create();
            dialog.getWindow().setType(2003);
            WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void notifyInvitationReceived() {
            Resources r = Resources.getSystem();
            final WpsInfo wps = this.mSavedPeerConfig.wps;
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.preference_child, (ViewGroup) null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(R.id.opaque);
            addRowToDialog(group, R.string.face_acquired_not_detected, getDeviceName(this.mSavedPeerConfig.deviceAddress));
            final EditText pin = (EditText) textEntryView.findViewById(R.id.opticalBounds);
            AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(R.string.face_acquired_mouth_covering_detected_alt)).setView(textEntryView).setPositiveButton(r.getString(R.string.face_acquired_dark_glasses_detected_alt), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    if (wps.setup == 2) {
                        P2pStateMachine.this.mSavedPeerConfig.wps.pin = pin.getText().toString();
                    }
                    P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " accept invitation " + P2pStateMachine.this.mSavedPeerConfig);
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                }
            }).setNegativeButton(r.getString(R.string.face_acquired_insufficient), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " ignore connect");
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    P2pStateMachine.this.logd(P2pStateMachine.this.getName() + " ignore connect");
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }).create();
            switch (wps.setup) {
                case 1:
                    logd("Shown pin section visible");
                    addRowToDialog(group, R.string.face_acquired_poor_gaze, wps.pin);
                    break;
                case 2:
                    logd("Enter pin section visible");
                    textEntryView.findViewById(R.id.open_cross_profile).setVisibility(0);
                    break;
            }
            if ((r.getConfiguration().uiMode & 5) == 5) {
                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog2, int keyCode, KeyEvent event) {
                        if (keyCode == 164) {
                            P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                            dialog2.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
            }
            dialog.getWindow().setType(2003);
            WindowManager.LayoutParams attrs = dialog.getWindow().getAttributes();
            attrs.privateFlags = 16;
            dialog.getWindow().setAttributes(attrs);
            dialog.show();
        }

        private void updatePersistentNetworks(boolean reload) {
            String listStr = this.mWifiNative.listNetworks();
            if (listStr == null) {
                return;
            }
            boolean isSaveRequired = false;
            String[] lines = listStr.split("\n");
            if (lines == null) {
                return;
            }
            if (reload) {
                this.mGroups.clear();
            }
            for (int i = 1; i < lines.length; i++) {
                String[] result = lines[i].split("\t");
                if (result != null && result.length >= 4) {
                    String ssid = result[1];
                    String bssid = result[2];
                    String flags = result[3];
                    try {
                        int netId = Integer.parseInt(result[0]);
                        if (flags.indexOf("[CURRENT]") == -1) {
                            if (flags.indexOf("[P2P-PERSISTENT]") == -1) {
                                logd("clean up the unused persistent group. netId=" + netId);
                                this.mWifiNative.removeNetwork(netId);
                                isSaveRequired = true;
                            } else if (!this.mGroups.contains(netId)) {
                                WifiP2pGroup group = new WifiP2pGroup();
                                group.setNetworkId(netId);
                                WifiSsid wifiSssid = WifiSsid.createFromAsciiEncoded(ssid);
                                group.setNetworkName(wifiSssid.toString());
                                String mode = this.mWifiNative.getNetworkVariable(netId, "mode");
                                if (mode != null && mode.equals("3")) {
                                    group.setIsGroupOwner(true);
                                }
                                if (bssid.equalsIgnoreCase(WifiP2pServiceImpl.this.mThisDevice.deviceAddress)) {
                                    group.setOwner(WifiP2pServiceImpl.this.mThisDevice);
                                } else {
                                    WifiP2pDevice device = new WifiP2pDevice();
                                    device.deviceAddress = bssid;
                                    group.setOwner(device);
                                }
                                this.mGroups.add(group);
                                isSaveRequired = true;
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!reload && !isSaveRequired) {
                return;
            }
            this.mWifiNative.saveConfig();
            sendP2pPersistentGroupsChangedBroadcast();
        }

        private boolean isConfigInvalid(WifiP2pConfig config) {
            return config == null || TextUtils.isEmpty(config.deviceAddress) || this.mPeers.get(config.deviceAddress) == null;
        }

        private WifiP2pDevice fetchCurrentDeviceDetails(WifiP2pConfig config) {
            int gc = this.mWifiNative.getGroupCapability(config.deviceAddress);
            if (getCurrentState() instanceof UserAuthorizingInviteRequestState) {
                gc |= 1;
            }
            this.mPeers.updateGroupCapability(config.deviceAddress, gc);
            return this.mPeers.get(config.deviceAddress);
        }

        private void p2pConnectWithPinDisplay(WifiP2pConfig config) {
            WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
            String pin = this.mWifiNative.p2pConnect(config, dev.isGroupOwner());
            try {
                Integer.parseInt(pin);
                notifyInvitationSent(pin, config.deviceAddress);
            } catch (NumberFormatException e) {
            }
        }

        private boolean reinvokePersistentGroup(WifiP2pConfig config) {
            int netId;
            if (config.netId == -1) {
                return false;
            }
            WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
            boolean join = dev.isGroupOwner();
            String ssid = this.mWifiNative.p2pGetSsid(dev.deviceAddress);
            logd("target ssid is " + ssid + " join:" + join);
            if (join && dev.isGroupLimit()) {
                logd("target device reaches group limit.");
                join = false;
            } else if (join && (netId = this.mGroups.getNetworkId(dev.deviceAddress, ssid)) >= 0) {
                return this.mWifiNative.p2pReinvoke(netId, dev.deviceAddress);
            }
            if (!join && dev.isDeviceLimit()) {
                loge("target device reaches the device limit.");
                return false;
            }
            if (!join && dev.isInvitationCapable()) {
                int netId2 = -2;
                if (config.netId >= 0) {
                    if (config.deviceAddress.equals(this.mGroups.getOwnerAddr(config.netId))) {
                        netId2 = config.netId;
                    }
                } else {
                    netId2 = this.mGroups.getNetworkId(dev.deviceAddress);
                }
                if (netId2 < 0) {
                    netId2 = getNetworkIdFromClientList(dev.deviceAddress);
                }
                logd("netId related with " + dev.deviceAddress + " = " + netId2);
                if (netId2 >= 0) {
                    if (this.mWifiNative.p2pReinvoke(netId2, dev.deviceAddress)) {
                        config.netId = netId2;
                        return true;
                    }
                    loge("p2pReinvoke() failed, update networks");
                    updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
                    return false;
                }
            }
            return false;
        }

        private int getNetworkIdFromClientList(String deviceAddress) {
            if (deviceAddress == null) {
                return -1;
            }
            Collection<WifiP2pGroup> groups = this.mGroups.getGroupList();
            for (WifiP2pGroup group : groups) {
                int netId = group.getNetworkId();
                String[] p2pClientList = getClientList(netId);
                if (p2pClientList != null) {
                    for (String client : p2pClientList) {
                        if (deviceAddress.equalsIgnoreCase(client)) {
                            return netId;
                        }
                    }
                }
            }
            return -1;
        }

        private String[] getClientList(int netId) {
            String p2pClients = this.mWifiNative.getNetworkVariable(netId, "p2p_client_list");
            if (p2pClients == null) {
                return null;
            }
            return p2pClients.split(" ");
        }

        private boolean removeClientFromList(int netId, String addr, boolean isRemovable) {
            StringBuilder modifiedClientList = new StringBuilder();
            String[] currentClientList = getClientList(netId);
            boolean isClientRemoved = false;
            if (currentClientList != null) {
                for (String client : currentClientList) {
                    if (!client.equalsIgnoreCase(addr)) {
                        modifiedClientList.append(" ");
                        modifiedClientList.append(client);
                    } else {
                        isClientRemoved = true;
                    }
                }
            }
            if (modifiedClientList.length() == 0 && isRemovable) {
                logd("Remove unknown network");
                this.mGroups.remove(netId);
                return true;
            }
            if (!isClientRemoved) {
                return false;
            }
            logd("Modified client list: " + ((Object) modifiedClientList));
            if (modifiedClientList.length() == 0) {
                modifiedClientList.append("\"\"");
            }
            this.mWifiNative.setNetworkVariable(netId, "p2p_client_list", modifiedClientList.toString());
            this.mWifiNative.saveConfig();
            return true;
        }

        private void setWifiP2pInfoOnGroupFormation(InetAddress serverInetAddress) {
            this.mWifiP2pInfo.groupFormed = true;
            this.mWifiP2pInfo.isGroupOwner = this.mGroup.isGroupOwner();
            this.mWifiP2pInfo.groupOwnerAddress = serverInetAddress;
        }

        private void resetWifiP2pInfo() {
            WifiP2pServiceImpl.this.mGcIgnoresDhcpReq = false;
            this.mWifiP2pInfo.groupFormed = false;
            this.mWifiP2pInfo.isGroupOwner = false;
            this.mWifiP2pInfo.groupOwnerAddress = null;
            sendP2pTxBroadcast(false);
            WifiP2pServiceImpl.this.mNegoChannelConflict = false;
            if (WifiP2pServiceImpl.this.mMccSupport) {
                p2pSetCCMode(0);
            }
            WifiP2pServiceImpl.this.mConnectToPeer = false;
            WifiP2pServiceImpl.this.mCrossmountEventReceived = false;
            WifiP2pServiceImpl.this.mCrossmountSessionInfo = "";
            WifiP2pServiceImpl.this.mUpdatePeerForInvited = false;
        }

        private String getDeviceName(String deviceAddress) {
            WifiP2pDevice d = this.mPeers.get(deviceAddress);
            if (d != null) {
                return d.deviceName;
            }
            return deviceAddress;
        }

        private String getPersistedDeviceName() {
            String deviceName = Settings.Global.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name");
            if (deviceName == null) {
                String id = Settings.Secure.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "android_id");
                return "Android_" + id.substring(0, 4);
            }
            return deviceName;
        }

        private boolean setAndPersistDeviceName(String devName) {
            if (devName == null) {
                return false;
            }
            if (!this.mWifiNative.setDeviceName(devName)) {
                loge("Failed to set device name " + devName);
                return false;
            }
            WifiP2pServiceImpl.this.mThisDevice.deviceName = devName;
            this.mWifiNative.setP2pSsidPostfix("-" + WifiP2pServiceImpl.this.getSsidPostfix(WifiP2pServiceImpl.this.mThisDevice.deviceName));
            Settings.Global.putString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name", devName);
            sendThisDeviceChangedBroadcast();
            return true;
        }

        private boolean setWfdInfo(WifiP2pWfdInfo wfdInfo) {
            boolean success;
            if (!wfdInfo.isWfdEnabled()) {
                success = this.mWifiNative.setWfdEnable(false);
            } else if (!this.mWifiNative.setWfdEnable(true)) {
                success = false;
            } else {
                success = this.mWifiNative.setWfdDeviceInfo(wfdInfo.getDeviceInfoHex());
            }
            if (!success) {
                loge("Failed to set wfd properties, Device Info part");
                return false;
            }
            if (wfdInfo.getExtendedCapability() != 0) {
                setWfdExtCapability(wfdInfo.getExtCapaHex());
            }
            WifiP2pServiceImpl.this.mThisDevice.wfdInfo = wfdInfo;
            sendThisDeviceChangedBroadcast();
            return true;
        }

        private void initializeP2pSettings() {
            this.mWifiNative.setPersistentReconnect(true);
            WifiP2pServiceImpl.this.mThisDevice.deviceName = getPersistedDeviceName();
            this.mWifiNative.setDeviceName(WifiP2pServiceImpl.this.mThisDevice.deviceName);
            this.mWifiNative.setP2pSsidPostfix("-" + WifiP2pServiceImpl.this.getSsidPostfix(WifiP2pServiceImpl.this.mThisDevice.deviceName));
            this.mWifiNative.setDeviceType(WifiP2pServiceImpl.this.mThisDevice.primaryDeviceType);
            this.mWifiNative.setConfigMethods("virtual_push_button physical_display keypad");
            this.mWifiNative.setConcurrencyPriority("sta");
            logd("old DeviceAddress: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
            WifiP2pServiceImpl.this.mThisDevice.deviceAddress = this.mWifiNative.p2pGetDeviceAddress();
            logd("new DeviceAddress: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
            updateThisDevice(3);
            logd("DeviceAddress: " + WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
            WifiP2pServiceImpl.this.mClientInfoList.clear();
            this.mWifiNative.p2pFlush();
            this.mWifiNative.p2pServiceFlush();
            WifiP2pServiceImpl.this.mServiceTransactionId = (byte) 0;
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
            WifiP2pServiceImpl.this.mMccSupport = SystemProperties.get("ro.mtk_wifi_mcc_support").equals("1");
            logd("is Mcc Supported: " + WifiP2pServiceImpl.this.mMccSupport);
            if (!WifiP2pServiceImpl.this.mMccSupport) {
                return;
            }
            p2pSetCCMode(0);
        }

        private void updateThisDevice(int status) {
            WifiP2pServiceImpl.this.mThisDevice.status = status;
            sendThisDeviceChangedBroadcast();
        }

        private void handleGroupCreationFailure() {
            resetWifiP2pInfo();
            WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.FAILED, null, null);
            if (WifiP2pServiceImpl.this.mGroupRemoveReason == P2pStatus.UNKNOWN) {
                sendP2pConnectionChangedBroadcast();
            } else {
                sendP2pConnectionChangedBroadcast(WifiP2pServiceImpl.this.mGroupRemoveReason);
            }
            boolean peersChanged = this.mPeers.remove(this.mPeersLostDuringConnection);
            if (!TextUtils.isEmpty(this.mSavedPeerConfig.deviceAddress) && this.mPeers.remove(this.mSavedPeerConfig.deviceAddress) != null) {
                peersChanged = true;
            }
            if (peersChanged) {
                sendPeersChangedBroadcast();
            }
            this.mPeersLostDuringConnection.clear();
            clearSupplicantServiceRequest();
            if (!isWfdSinkEnabled()) {
                sendMessage(139265);
            }
            if (!WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi) {
                return;
            }
            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 0);
            WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = false;
        }

        private void handleGroupRemoved() {
            if (this.mGroup.isGroupOwner()) {
                stopDhcpServer(this.mGroup.getInterface());
                File dhcpFile = new File(WifiP2pServiceImpl.DHCP_INFO_FILE);
                logd("DHCP file exists=" + dhcpFile.exists());
                if (dhcpFile.exists()) {
                    boolean b = dhcpFile.delete();
                    if (b) {
                        logd("Delete p2p0 dhcp info file OK!");
                    }
                }
            } else {
                logd("stop IpManager");
                WifiP2pServiceImpl.this.stopIpManager();
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
            if (!TextUtils.isEmpty(WifiP2pServiceImpl.this.mWfdSourceAddr)) {
                logd("wfd source case: mWfdSourceAddr = " + WifiP2pServiceImpl.this.mWfdSourceAddr);
                while (this.mGroups.contains(this.mGroups.getNetworkId(WifiP2pServiceImpl.this.mWfdSourceAddr))) {
                    this.mGroups.remove(this.mGroups.getNetworkId(WifiP2pServiceImpl.this.mWfdSourceAddr));
                }
                WifiP2pServiceImpl.this.mWfdSourceAddr = null;
            }
            boolean peersChanged = false;
            for (WifiP2pDevice d : this.mGroup.getClientList()) {
                if (d != null) {
                    logd("handleGroupRemoved, call mPeers.remove - d.deviceName = " + d.deviceName + " d.deviceAddress = " + d.deviceAddress);
                }
                if (this.mPeers.remove(d)) {
                    peersChanged = true;
                }
            }
            if (this.mPeers.remove(this.mGroup.getOwner())) {
                peersChanged = true;
            }
            if (this.mPeers.remove(this.mPeersLostDuringConnection)) {
                peersChanged = true;
            }
            if (peersChanged) {
                sendPeersChangedBroadcast();
            }
            this.mGroup = null;
            this.mPeersLostDuringConnection.clear();
            clearSupplicantServiceRequest();
            if (WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi) {
                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 0);
                WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = false;
            }
            this.mWifiNative.p2pFlush();
        }

        private void replyToMessage(Message msg, int what) {
            if (msg.replyTo == null) {
                return;
            }
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
        }

        private void replyToMessage(Message msg, int what, int arg1) {
            if (msg.replyTo == null) {
                return;
            }
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            dstMsg.arg1 = arg1;
            WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
        }

        private void replyToMessage(Message msg, int what, Object obj) {
            if (msg.replyTo == null) {
                return;
            }
            Message dstMsg = obtainMessage(msg);
            dstMsg.what = what;
            dstMsg.obj = obj;
            WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
        }

        private Message obtainMessage(Message srcMsg) {
            Message msg = Message.obtain();
            msg.arg2 = srcMsg.arg2;
            return msg;
        }

        protected void logd(String s) {
            Log.d(WifiP2pServiceImpl.TAG, s);
        }

        protected void loge(String s) {
            Log.e(WifiP2pServiceImpl.TAG, s);
        }

        private boolean updateSupplicantServiceRequest() {
            clearSupplicantServiceRequest();
            StringBuffer sb = new StringBuffer();
            for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                for (int i = 0; i < c.mReqList.size(); i++) {
                    WifiP2pServiceRequest req = (WifiP2pServiceRequest) c.mReqList.valueAt(i);
                    if (req != null) {
                        sb.append(req.getSupplicantQuery());
                    }
                }
            }
            if (sb.length() == 0) {
                return false;
            }
            WifiP2pServiceImpl.this.mServiceDiscReqId = this.mWifiNative.p2pServDiscReq("00:00:00:00:00:00", sb.toString());
            return WifiP2pServiceImpl.this.mServiceDiscReqId != null;
        }

        private void clearSupplicantServiceRequest() {
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return;
            }
            this.mWifiNative.p2pServDiscCancelReq(WifiP2pServiceImpl.this.mServiceDiscReqId);
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
        }

        private boolean addServiceRequest(Messenger m, WifiP2pServiceRequest req) {
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(m, true);
            if (clientInfo == null) {
                return false;
            }
            WifiP2pServiceImpl wifiP2pServiceImpl = WifiP2pServiceImpl.this;
            wifiP2pServiceImpl.mServiceTransactionId = (byte) (wifiP2pServiceImpl.mServiceTransactionId + 1);
            if (WifiP2pServiceImpl.this.mServiceTransactionId == 0) {
                WifiP2pServiceImpl wifiP2pServiceImpl2 = WifiP2pServiceImpl.this;
                wifiP2pServiceImpl2.mServiceTransactionId = (byte) (wifiP2pServiceImpl2.mServiceTransactionId + 1);
            }
            int localSevID = WifiP2pServiceImpl.this.mServiceTransactionId & 255;
            req.setTransactionId(localSevID);
            clientInfo.mReqList.put(localSevID, req);
            if (WifiP2pServiceImpl.this.mServiceDiscReqId == null) {
                return true;
            }
            return updateSupplicantServiceRequest();
        }

        private void removeServiceRequest(Messenger m, WifiP2pServiceRequest req) {
            ClientInfo clientInfo = getClientInfo(m, false);
            if (clientInfo == null) {
                return;
            }
            boolean removed = false;
            int i = 0;
            while (true) {
                if (i >= clientInfo.mReqList.size()) {
                    break;
                }
                if (!req.equals(clientInfo.mReqList.valueAt(i))) {
                    i++;
                } else {
                    removed = true;
                    clientInfo.mReqList.removeAt(i);
                    break;
                }
            }
            if (removed) {
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

        private void clearServiceRequests(Messenger m) {
            ClientInfo clientInfo = getClientInfo(m, false);
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

        private boolean addLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(m, true);
            if (clientInfo == null || !clientInfo.mServList.add(servInfo)) {
                return false;
            }
            if (this.mWifiNative.p2pServiceAdd(servInfo)) {
                return true;
            }
            clientInfo.mServList.remove(servInfo);
            return false;
        }

        private void removeLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
            ClientInfo clientInfo = getClientInfo(m, false);
            if (clientInfo == null) {
                return;
            }
            this.mWifiNative.p2pServiceDel(servInfo);
            clientInfo.mServList.remove(servInfo);
            if (clientInfo.mReqList.size() != 0 || clientInfo.mServList.size() != 0) {
                return;
            }
            logd("remove client information from framework");
            WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
        }

        private void clearLocalServices(Messenger m) {
            ClientInfo clientInfo = getClientInfo(m, false);
            if (clientInfo == null) {
                return;
            }
            for (WifiP2pServiceInfo servInfo : clientInfo.mServList) {
                this.mWifiNative.p2pServiceDel(servInfo);
            }
            clientInfo.mServList.clear();
            if (clientInfo.mReqList.size() != 0) {
                return;
            }
            logd("remove client information from framework");
            WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
        }

        private void clearClientInfo(Messenger m) {
            clearLocalServices(m);
            clearServiceRequests(m);
        }

        private void sendServiceResponse(WifiP2pServiceResponse resp) {
            for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                WifiP2pServiceRequest req = (WifiP2pServiceRequest) c.mReqList.get(resp.getTransactionId());
                if (req != null) {
                    Message msg = Message.obtain();
                    msg.what = 139314;
                    msg.arg1 = 0;
                    msg.arg2 = 0;
                    msg.obj = resp;
                    try {
                        c.mMessenger.send(msg);
                    } catch (RemoteException e) {
                        logd("detect dead channel");
                        clearClientInfo(c.mMessenger);
                        return;
                    }
                }
            }
        }

        private void clearClientDeadChannels() {
            ArrayList<Messenger> deadClients = new ArrayList<>();
            for (ClientInfo c : WifiP2pServiceImpl.this.mClientInfoList.values()) {
                Message msg = Message.obtain();
                msg.what = 139313;
                msg.arg1 = 0;
                msg.arg2 = 0;
                msg.obj = null;
                try {
                    c.mMessenger.send(msg);
                } catch (RemoteException e) {
                    logd("detect dead channel");
                    deadClients.add(c.mMessenger);
                }
            }
            for (Messenger m : deadClients) {
                clearClientInfo(m);
            }
        }

        private ClientInfo getClientInfo(Messenger m, boolean createIfNotExist) {
            ClientInfo clientInfo = null;
            ClientInfo clientInfo2 = (ClientInfo) WifiP2pServiceImpl.this.mClientInfoList.get(m);
            if (clientInfo2 == null && createIfNotExist) {
                logd("add a new client");
                ClientInfo clientInfo3 = new ClientInfo(WifiP2pServiceImpl.this, m, clientInfo);
                WifiP2pServiceImpl.this.mClientInfoList.put(m, clientInfo3);
                return clientInfo3;
            }
            return clientInfo2;
        }

        private void sendP2pConnectionChangedBroadcast(P2pStatus reason) {
            logd("sending p2p connection changed broadcast, reason = " + reason + ", mGroup: " + this.mGroup + ", mP2pOperFreq: " + WifiP2pServiceImpl.this.mP2pOperFreq);
            Intent intent = new Intent("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("wifiP2pInfo", new WifiP2pInfo(this.mWifiP2pInfo));
            intent.putExtra("networkInfo", new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            intent.putExtra("p2pGroupInfo", new WifiP2pGroup(this.mGroup));
            intent.putExtra("p2pOperFreq", WifiP2pServiceImpl.this.mP2pOperFreq);
            if (reason == P2pStatus.NO_COMMON_CHANNEL) {
                intent.putExtra("reason=", 7);
            } else if (reason == P2pStatus.MTK_EXPAND_02) {
                logd("channel conflict, user decline, broadcast with reason=-3");
                intent.putExtra("reason=", -3);
            } else if (reason == P2pStatus.MTK_EXPAND_01) {
                logd("[wfd sink/source] broadcast with reason=-2");
                intent.putExtra("reason=", -2);
            } else {
                intent.putExtra("reason=", -1);
            }
            WifiP2pServiceImpl.this.mGroupRemoveReason = P2pStatus.UNKNOWN;
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_CONNECTION_CHANGED, new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
        }

        private void sendP2pTxBroadcast(boolean bStart) {
            logd("sending p2p Tx broadcast: " + bStart);
            Intent intent = new Intent("com.mediatek.wifi.p2p.Tx");
            intent.addFlags(603979776);
            intent.putExtra("start", bStart);
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }

        private void sendP2pGOandGCRequestConnectBroadcast() {
            logd("sendP2pGOandGCRequestConnectBroadcast");
            Intent intent = new Intent("com.mediatek.wifi.p2p.GO.GCrequest.connect");
            intent.addFlags(603979776);
            WifiP2pDevice dev = this.mPeers.get(this.mSavedPeerConfig.deviceAddress);
            if (dev != null && dev.deviceName != null) {
                intent.putExtra("deviceName", dev.deviceName);
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

        private void sendP2pCrossmountIntentionBroadcast() {
            logd("sendP2pCrossmountIntentionBroadcast, session info:" + WifiP2pServiceImpl.this.mCrossmountSessionInfo + ", wps method=" + this.mSavedPeerConfig.wps.setup);
            Intent intent = new Intent("com.mediatek.wifi.p2p.crossmount.intention");
            intent.addFlags(603979776);
            WifiP2pDevice dev = this.mPeers.get(this.mSavedPeerConfig.deviceAddress);
            if (dev == null || dev.deviceName == null) {
                intent.putExtra("deviceName", "crossmount source");
                intent.putExtra("deviceAddress", "crossmount source");
                intent.putExtra("sessionInfo", "63726f73736d6f756e7420736f75726365");
            } else {
                intent.putExtra("deviceName", dev.deviceName);
                intent.putExtra("deviceAddress", dev.deviceAddress);
                intent.putExtra("sessionInfo", WifiP2pServiceImpl.this.mCrossmountSessionInfo);
            }
            intent.putExtra("wpsMethod", Integer.toString(this.mSavedPeerConfig.wps.setup));
            WifiP2pServiceImpl.this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }

        private WifiP2pDevice p2pGoGetSta(WifiP2pDevice p2pDev, String p2pMAC) {
            if (p2pMAC == null || p2pDev == null) {
                loge("gc or gc mac is null");
                return null;
            }
            p2pDev.deviceAddress = p2pMAC;
            String p2pSta = p2pGoGetSta(p2pMAC);
            if (p2pSta == null) {
                return p2pDev;
            }
            logd("p2pGoGetSta() return: " + p2pSta);
            String[] tokens = p2pSta.split("\n");
            for (String token : tokens) {
                if (token.startsWith("p2p_device_name=")) {
                    String[] nameValue = token.split("=");
                    p2pDev.deviceName = nameValueAssign(nameValue, p2pDev.deviceName);
                } else if (token.startsWith("p2p_primary_device_type=")) {
                    String[] nameValue2 = token.split("=");
                    p2pDev.primaryDeviceType = nameValueAssign(nameValue2, p2pDev.primaryDeviceType);
                } else if (token.startsWith("p2p_group_capab=")) {
                    String[] nameValue3 = token.split("=");
                    p2pDev.groupCapability = nameValueAssign(nameValue3, p2pDev.groupCapability);
                } else if (token.startsWith("p2p_dev_capab=")) {
                    String[] nameValue4 = token.split("=");
                    p2pDev.deviceCapability = nameValueAssign(nameValue4, p2pDev.deviceCapability);
                } else if (token.startsWith("p2p_config_methods=")) {
                    String[] nameValue5 = token.split("=");
                    p2pDev.wpsConfigMethodsSupported = nameValueAssign(nameValue5, p2pDev.wpsConfigMethodsSupported);
                }
            }
            return p2pDev;
        }

        private String nameValueAssign(String[] nameValue, String string) {
            if (nameValue == null || nameValue.length != 2) {
                return null;
            }
            return nameValue[1];
        }

        private int nameValueAssign(String[] nameValue, int integer) {
            if (nameValue == null || nameValue.length != 2 || nameValue[1] == null) {
                return 0;
            }
            return WifiP2pDevice.parseHex(nameValue[1]);
        }

        private int nameValueAssign(String[] nameValue, int integer, int base) {
            if (nameValue == null || nameValue.length != 2 || nameValue[1] == null || base == 0) {
                return 0;
            }
            return Integer.parseInt(nameValue[1], base);
        }

        private void setWifiOn_WifiAPOff() {
            if (WifiP2pServiceImpl.this.mWifiManager == null) {
                WifiP2pServiceImpl.this.mWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi");
            }
            int wifiApState = WifiP2pServiceImpl.this.mWifiManager.getWifiApState();
            if (wifiApState == 12 || wifiApState == 13) {
                WifiP2pServiceImpl.this.mWifiManager.setWifiApEnabled(null, false);
            }
            logd("call WifiManager.stopReconnectAndScan() and WifiManager.setWifiEnabled()");
            WifiP2pServiceImpl.this.mWifiManager.stopReconnectAndScan(true, 0);
            WifiP2pServiceImpl.this.mWifiManager.setWifiEnabled(true);
        }

        public WifiInfo getWifiConnectionInfo() {
            if (WifiP2pServiceImpl.this.mWifiManager == null) {
                WifiP2pServiceImpl.this.mWifiManager = (WifiManager) WifiP2pServiceImpl.this.mContext.getSystemService("wifi");
            }
            return WifiP2pServiceImpl.this.mWifiManager.getConnectionInfo();
        }

        private String getInterfaceAddress(String deviceAddress) {
            logd("getInterfaceAddress(): deviceAddress=" + deviceAddress);
            WifiP2pDevice d = this.mPeers.get(deviceAddress);
            if (d == null || deviceAddress.equals(d.interfaceAddress)) {
                return deviceAddress;
            }
            logd("getInterfaceAddress(): interfaceAddress=" + d.interfaceAddress);
            return d.interfaceAddress;
        }

        public String getPeerIpAddress(String inputAddress) throws Throwable {
            FileInputStream fileStream;
            logd("getPeerIpAddress(): input address=" + inputAddress);
            if (inputAddress == null) {
                return null;
            }
            if (this.mGroup == null) {
                loge("getPeerIpAddress(): mGroup is null!");
                return null;
            }
            if (!this.mGroup.isGroupOwner()) {
                if (this.mGroup.getOwner().deviceAddress != null && inputAddress.equals(this.mGroup.getOwner().deviceAddress)) {
                    logd("getPeerIpAddress(): GO device address case, goIpAddress=" + this.mGroup.getOwner().deviceIP);
                    return this.mGroup.getOwner().deviceIP;
                }
                if (this.mGroup.getOwner().interfaceAddress == null || !inputAddress.equals(this.mGroup.getOwner().interfaceAddress)) {
                    loge("getPeerIpAddress(): no match GO address case, goIpAddress is null");
                    return null;
                }
                logd("getPeerIpAddress(): GO interface address case, goIpAddress=" + this.mGroup.getOwner().deviceIP);
                return this.mGroup.getOwner().deviceIP;
            }
            String intrerfaceAddress = getInterfaceAddress(inputAddress);
            FileInputStream fileStream2 = null;
            try {
                try {
                    fileStream = new FileInputStream(WifiP2pServiceImpl.DHCP_INFO_FILE);
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
            try {
                DataInputStream in = new DataInputStream(fileStream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                for (String str = br.readLine(); str != null && str.length() != 0; str = br.readLine()) {
                    String[] fields = str.split(" ");
                    String str2 = fields.length > 3 ? fields[2] : null;
                    if (str2 != null && fields[1] != null && fields[1].indexOf(intrerfaceAddress) != -1) {
                        logd("getPeerIpAddress(): getClientIp() mac matched, get IP address = " + str2);
                        if (fileStream != null) {
                            try {
                                fileStream.close();
                            } catch (IOException e2) {
                                loge("getPeerIpAddress(): getClientIp() close file met IOException: " + e2);
                            }
                        }
                        return str2;
                    }
                }
                loge("getPeerIpAddress(): getClientIp() dhcp client " + intrerfaceAddress + " had not connected up!");
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e3) {
                        loge("getPeerIpAddress(): getClientIp() close file met IOException: " + e3);
                    }
                }
                return null;
            } catch (IOException e4) {
                e = e4;
                fileStream2 = fileStream;
                loge("getPeerIpAddress(): getClientIp(): " + e);
                if (fileStream2 != null) {
                    try {
                        fileStream2.close();
                    } catch (IOException e5) {
                        loge("getPeerIpAddress(): getClientIp() close file met IOException: " + e5);
                    }
                }
                loge("getPeerIpAddress(): found nothing");
                return null;
            } catch (Throwable th2) {
                th = th2;
                fileStream2 = fileStream;
                if (fileStream2 != null) {
                    try {
                        fileStream2.close();
                    } catch (IOException e6) {
                        loge("getPeerIpAddress(): getClientIp() close file met IOException: " + e6);
                    }
                }
                throw th;
            }
        }

        public void setCrossMountIE(boolean isAdd, String hexData) {
            WifiP2pServiceImpl.this.mCrossmountIEAdded = isAdd;
            setVendorElemIE(isAdd, WifiP2pServiceImpl.VENDOR_IE_ALL_FRAME_TAG, hexData);
        }

        public String getCrossMountIE(String hexData) {
            int indexCrossMountTag = hexData.indexOf("000ce733");
            if (indexCrossMountTag < WifiP2pServiceImpl.VENDOR_IE_TAG.length() + 2) {
                loge("getCrossMountIE(): bad index: indexCrossMountTag=" + indexCrossMountTag);
                return "";
            }
            String strLenIE = hexData.substring(indexCrossMountTag - 2, indexCrossMountTag);
            if (TextUtils.isEmpty(strLenIE)) {
                loge("getCrossMountIE(): bad strLenIE: " + strLenIE);
                return "";
            }
            int lenIE = Integer.valueOf(strLenIE, 16).intValue();
            String hexCrossMountIE = hexData.substring(indexCrossMountTag + 8, indexCrossMountTag + 8 + ((lenIE - 4) * 2));
            loge("getCrossMountIE(): hexCrossMountIE=" + hexCrossMountIE);
            return hexCrossMountIE;
        }

        private boolean isGroupRemoved() {
            boolean removed = true;
            for (WifiP2pDevice d : this.mPeers.getDeviceList()) {
                if (!WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(d.deviceAddress) && d.status == 0) {
                    removed = false;
                }
            }
            logd("isGroupRemoved(): " + removed);
            return removed;
        }

        private void resetWifiP2pConn() {
            if (this.mGroup != null) {
                this.mWifiNative.p2pGroupRemove(WifiP2pServiceImpl.this.mInterface);
            } else {
                if (!getHandler().hasMessages(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT)) {
                    return;
                }
                sendMessage(139274);
            }
        }

        private void p2pConfigWfdSink() {
            resetWifiP2pConn();
            this.mWifiNative.setDeviceType("8-0050F204-2");
            String result = p2pGetDeviceCapa();
            if (result.startsWith("p2p_dev_capa=")) {
                String[] nameValue = result.split("=");
                WifiP2pServiceImpl.this.mDeviceCapa = nameValueAssign(nameValue, WifiP2pServiceImpl.this.mDeviceCapa, 10);
            } else {
                WifiP2pServiceImpl.this.mDeviceCapa = -1;
            }
            logd("[wfd sink] p2pConfigWfdSink() ori deviceCapa = " + WifiP2pServiceImpl.this.mDeviceCapa);
            if (WifiP2pServiceImpl.this.mDeviceCapa <= 0) {
                return;
            }
            String DeviceCapa_local = (Integer.valueOf(WifiP2pServiceImpl.this.mDeviceCapa).intValue() & 223) + "";
            p2pSetDeviceCapa(DeviceCapa_local);
            logd("[wfd sink] p2pConfigWfdSink() after: " + p2pGetDeviceCapa());
        }

        private void p2pUnconfigWfdSink() {
            resetWifiP2pConn();
            this.mWifiNative.setDeviceType(WifiP2pServiceImpl.this.mThisDevice.primaryDeviceType);
            if (WifiP2pServiceImpl.this.mDeviceCapa <= 0) {
                return;
            }
            p2pSetDeviceCapa(WifiP2pServiceImpl.this.mDeviceCapa + "");
            logd("[wfd sink] p2pUnconfigWfdSink(): " + p2pGetDeviceCapa());
        }

        private boolean isWfdSinkEnabled() {
            if (!SystemProperties.get("ro.mtk_wfd_sink_support").equals("1")) {
                logd("[wfd sink] isWfdSinkEnabled, property unset");
                return false;
            }
            if (WifiP2pServiceImpl.this.mThisDevice.wfdInfo == null) {
                logd("[wfd sink] isWfdSinkEnabled, device wfdInfo unset");
                return false;
            }
            if (WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType() == 1 || WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType() == 3) {
                return true;
            }
            logd("[wfd sink] isWfdSinkEnabled, type :" + WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType());
            return false;
        }

        private boolean isWfdSinkConnected() {
            boolean basicCondition = isWfdSinkEnabled() && this.mGroup != null;
            if (basicCondition) {
                return !this.mGroup.isGroupOwner() || this.mGroup.getClientAmount() == 1;
            }
            return false;
        }

        private boolean isCrossMountGOwithMultiGC() {
            boolean basicCondition = WifiP2pServiceImpl.this.mCrossmountIEAdded && this.mGroup != null;
            return basicCondition && this.mGroup.isGroupOwner() && this.mGroup.getClientAmount() > 0;
        }

        private boolean isWfdSourceConnected() {
            if (WifiP2pServiceImpl.this.mThisDevice.wfdInfo == null) {
                logd("[wfd source] isWfdSourceConnected, device wfdInfo unset");
            } else if (WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType() != 0 && WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType() != 3) {
                logd("[wfd source] isWfdSourceConnected, type :" + WifiP2pServiceImpl.this.mThisDevice.wfdInfo.getDeviceType());
            } else if (isGroupRemoved()) {
                logd("[wfd source] isWfdSourceConnected, GroupRemoved");
            } else {
                return true;
            }
            return false;
        }

        private void setVendorElemIE(boolean isAdd, int frameId, String hexData) {
            logd("setVendorElemIE(): isAdd=" + isAdd + ", frameId=" + frameId + ", hexData=" + hexData);
            String ieBuf = "000ce733" + hexData;
            int len = ieBuf.length() / 2;
            String ieBuf2 = WifiP2pServiceImpl.VENDOR_IE_TAG + String.format("%02x", Integer.valueOf(len & 255)) + ieBuf;
            if (isAdd) {
                if (frameId == WifiP2pServiceImpl.VENDOR_IE_ALL_FRAME_TAG) {
                    for (int i = 0; i <= 12; i++) {
                        vendorIEAdd(i, ieBuf2);
                    }
                    return;
                }
                vendorIEAdd(frameId, ieBuf2);
                return;
            }
            if (frameId == WifiP2pServiceImpl.VENDOR_IE_ALL_FRAME_TAG) {
                for (int i2 = 0; i2 <= 12; i2++) {
                    vendorIERemove(i2, null);
                }
                return;
            }
            vendorIERemove(frameId, null);
        }

        private void updateCrossMountInfo(String peerAddress) {
            logd("updateCrossMountInfo(), peerAddress=" + peerAddress);
            String peerVendorIE = this.mWifiNative.p2pGetVendorElems(peerAddress);
            WifiP2pServiceImpl.this.mCrossmountEventReceived = false;
            if (!TextUtils.isEmpty(peerVendorIE) && !peerVendorIE.equals(WifiP2pServiceImpl.UNKNOWN_COMMAND) && peerVendorIE.contains("000ce733")) {
                WifiP2pServiceImpl.this.mCrossmountEventReceived = true;
                WifiP2pServiceImpl.this.mCrossmountSessionInfo = getCrossMountIE(peerVendorIE);
                return;
            }
            logd("updateCrossMountInfo(): check crossmount IE myself!");
            for (int i = 0; i <= 12; i++) {
                String myVendorIE = vendorIEGet(i);
                if (!TextUtils.isEmpty(myVendorIE) && !myVendorIE.equals(WifiP2pServiceImpl.UNKNOWN_COMMAND) && myVendorIE.contains("000ce733")) {
                    WifiP2pServiceImpl.this.mCrossmountEventReceived = true;
                    WifiP2pServiceImpl.this.mCrossmountSessionInfo = "";
                    return;
                }
            }
        }

        private void p2pUpdateScanList(String peerAddress) {
            if (WifiP2pServiceImpl.this.mThisDevice.deviceAddress.equals(peerAddress) || this.mPeers.get(peerAddress) != null) {
                return;
            }
            String peerInfo = this.mWifiNative.p2pPeer(peerAddress);
            if (TextUtils.isEmpty(peerInfo)) {
                return;
            }
            logd("p2pUpdateScanList(): " + peerAddress + "  isn't in framework scan list but existed in supplicant's list");
            WifiP2pDevice device = new WifiP2pDevice();
            device.deviceAddress = peerAddress;
            String[] tokens = peerInfo.split("\n");
            for (String token : tokens) {
                if (token.startsWith("device_name=")) {
                    device.deviceName = nameValueAssign(token.split("="), device.deviceName);
                } else if (token.startsWith("pri_dev_type=")) {
                    device.primaryDeviceType = nameValueAssign(token.split("="), device.primaryDeviceType);
                } else if (token.startsWith("config_methods=")) {
                    device.wpsConfigMethodsSupported = nameValueAssign(token.split("="), device.wpsConfigMethodsSupported);
                } else if (token.startsWith("dev_capab=")) {
                    device.deviceCapability = nameValueAssign(token.split("="), device.deviceCapability);
                } else if (token.startsWith("group_capab=")) {
                    device.groupCapability = nameValueAssign(token.split("="), device.groupCapability);
                }
            }
            this.mPeers.updateSupplicantDetails(device);
        }

        private void p2pOverwriteWpsPin(String caller, Object obj) {
            if (obj == null) {
                return;
            }
            int pinMethod = ((Bundle) obj).getInt("android.net.wifi.p2p.EXTRA_PIN_METHOD");
            String pinCode = ((Bundle) obj).getString("android.net.wifi.p2p.EXTRA_PIN_CODE");
            this.mSavedPeerConfig.wps.setup = pinMethod;
            this.mSavedPeerConfig.wps.pin = pinCode;
            logd("p2pOverwriteWpsPin(): " + caller + ", wps pin code: " + pinCode + ", pin method: " + pinMethod);
        }

        private void p2pUserAuthPreprocess(Message message) {
            if (message.arg1 >= 0 && message.arg1 <= 15) {
                this.mSavedPeerConfig.groupOwnerIntent = message.arg1;
            }
            this.mSavedPeerConfig.netId = -1;
            if (this.mSavedPeerConfig.wps.setup != 1 || WifiP2pServiceImpl.this.mCrossmountEventReceived) {
                return;
            }
            notifyInvitationSent(this.mSavedPeerConfig.wps.pin, this.mSavedPeerConfig.deviceAddress);
        }

        private boolean isAppHandledConnection() {
            if (isWfdSinkEnabled()) {
                return true;
            }
            return WifiP2pServiceImpl.this.mCrossmountEventReceived;
        }

        private boolean p2pRemoveClient(String iface, String mac) {
            String ret = this.mWifiNative.doCustomSupplicantCommand("IFNAME=" + iface + " P2P_REMOVE_CLIENT " + mac);
            if (TextUtils.isEmpty(ret)) {
                return false;
            }
            return ret.startsWith("OK");
        }

        private String p2pSetCCMode(int ccMode) {
            return this.mWifiNative.doCustomSupplicantCommand("DRIVER p2p_use_mcc=" + ccMode);
        }

        private String p2pGetDeviceCapa() {
            return this.mWifiNative.doCustomSupplicantCommand("DRIVER p2p_get_cap p2p_dev_capa");
        }

        private String p2pSetDeviceCapa(String strDecimal) {
            return this.mWifiNative.doCustomSupplicantCommand("DRIVER p2p_set_cap p2p_dev_capa " + strDecimal);
        }

        private void setP2pPowerSaveMtk(String iface, int mode) {
            this.mWifiNative.doCustomSupplicantCommand("DRIVER p2p_set_power_save " + mode);
        }

        private void setWfdExtCapability(String hex) {
            this.mWifiNative.doCustomSupplicantCommand("WFD_SUBELEM_SET 7 " + hex);
        }

        private void p2pBeamPlusGO(int reserve) {
            if (reserve == 0) {
                this.mWifiNative.doCustomSupplicantCommand("DRIVER BEAMPLUS_GO_RESERVE_END");
            } else {
                if (1 != reserve) {
                    return;
                }
                this.mWifiNative.doCustomSupplicantCommand("DRIVER BEAMPLUS_GO_RESERVE_START");
            }
        }

        private void p2pBeamPlus(int state) {
            if (state == 0) {
                this.mWifiNative.doCustomSupplicantCommand("DRIVER BEAMPLUS_STOP");
            } else {
                if (1 != state) {
                    return;
                }
                this.mWifiNative.doCustomSupplicantCommand("DRIVER BEAMPLUS_START");
            }
        }

        private void p2pSetBssid(int id, String bssid) {
            this.mWifiNative.doCustomSupplicantCommand("IFNAME=" + this.mWifiNative.getInterfaceName() + " SET_NETWORK " + id + " bssid " + bssid);
        }

        private String p2pLinkStatics(String interfaceAddress) {
            return this.mWifiNative.doCustomSupplicantCommand("DRIVER GET_STA_STATISTICS " + interfaceAddress);
        }

        private String p2pGoGetSta(String deviceAddress) {
            return this.mWifiNative.doCustomSupplicantCommand("IFNAME=" + this.mWifiNative.getInterfaceName() + " STA " + deviceAddress);
        }

        public void p2pAutoChannel(int enable) {
            this.mWifiNative.doCustomSupplicantCommand("enable_channel_selection " + enable);
        }

        private void vendorIEAdd(int frameId, String hex) {
            this.mWifiNative.doCustomSupplicantCommand("IFNAME=" + this.mWifiNative.getInterfaceName() + " VENDOR_ELEM_ADD " + frameId + " " + hex);
        }

        private String vendorIEGet(int frameId) {
            return this.mWifiNative.doCustomSupplicantCommand("IFNAME=" + this.mWifiNative.getInterfaceName() + " VENDOR_ELEM_GET " + frameId);
        }

        private void vendorIERemove(int frameId, String hex) {
            if (hex == null) {
                this.mWifiNative.doCustomSupplicantCommand("IFNAME=" + this.mWifiNative.getInterfaceName() + " VENDOR_ELEM_REMOVE " + frameId + " *");
            } else {
                this.mWifiNative.doCustomSupplicantCommand("IFNAME=" + this.mWifiNative.getInterfaceName() + " VENDOR_ELEM_REMOVE " + frameId + " " + hex);
            }
        }

        private WifiP2pGroup addPersistentGroup(HashMap<String, String> variables) {
            logd("addPersistentGroup");
            int netId = this.mWifiNative.addNetwork();
            for (String key : variables.keySet()) {
                logd("addPersistentGroup variable=" + key + " : " + variables.get(key));
                this.mWifiNative.setNetworkVariable(netId, key, variables.get(key));
            }
            updatePersistentNetworks(true);
            Collection<WifiP2pGroup> groups = this.mGroups.getGroupList();
            for (WifiP2pGroup group : groups) {
                if (netId == group.getNetworkId()) {
                    return group;
                }
            }
            logd("addPersistentGroup failed.");
            return null;
        }

        public void setBeamMode(int mode) {
            logd("setBeamMode mode=" + mode);
            switch (mode) {
                case 0:
                    p2pBeamPlus(1);
                    WifiP2pServiceImpl.this.mGcIgnoresDhcpReq = true;
                    break;
                case 1:
                    p2pBeamPlusGO(1);
                    break;
                case 2:
                    p2pBeamPlus(0);
                    WifiP2pServiceImpl.this.mGcIgnoresDhcpReq = false;
                    break;
                case 3:
                    p2pBeamPlusGO(0);
                    break;
            }
        }
    }

    private class ClientInfo {
        private Messenger mMessenger;
        private SparseArray<WifiP2pServiceRequest> mReqList;
        private List<WifiP2pServiceInfo> mServList;

        ClientInfo(WifiP2pServiceImpl this$0, Messenger m, ClientInfo clientInfo) {
            this(m);
        }

        private ClientInfo(Messenger m) {
            this.mMessenger = m;
            this.mReqList = new SparseArray<>();
            this.mServList = new ArrayList();
        }
    }

    private String getSsidPostfix(String deviceName) {
        int utfCount = 0;
        int strLen = 0;
        byte[] bChar = deviceName.getBytes();
        if (TextUtils.isEmpty(deviceName) || bChar.length <= 22) {
            return deviceName;
        }
        int i = 0;
        while (true) {
            if (i > deviceName.length()) {
                break;
            }
            byte b0 = bChar[utfCount];
            Log.d(TAG, "b0=" + ((int) b0) + ", i=" + i + ", utfCount=" + utfCount);
            if ((b0 & 128) == 0) {
                utfCount++;
            } else if (b0 >= -4 && b0 <= -3) {
                utfCount += 6;
            } else if (b0 >= -8) {
                utfCount += 5;
            } else if (b0 >= -16) {
                utfCount += 4;
            } else if (b0 >= -32) {
                utfCount += 3;
            } else if (b0 >= -64) {
                utfCount += 2;
            }
            if (utfCount <= 22) {
                i++;
            } else {
                strLen = i;
                Log.d(TAG, "break: utfCount=" + utfCount + ", strLen=" + strLen);
                break;
            }
        }
        return deviceName.substring(0, strLen);
    }
}
