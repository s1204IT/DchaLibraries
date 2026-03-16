package com.android.server.wifi.p2p;

import android.R;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.DhcpResults;
import android.net.DhcpStateMachine;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.wifi.WifiMonitor;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.WifiStateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.ksoap2.kdom.Node;

public final class WifiP2pServiceImpl extends IWifiP2pManager.Stub {
    private static final int BASE = 143360;
    public static final int BLOCK_DISCOVERY = 143375;
    private static final int DHCP_SERVER_NEW_LEASE = 143380;
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
    private static final String NETWORKTYPE = "WIFI_P2P";
    public static final int P2P_CONNECTION_CHANGED = 143371;
    private static final int PEER_CONNECTION_USER_ACCEPT = 143362;
    private static final int PEER_CONNECTION_USER_REJECT = 143363;
    private static final String SERVER_ADDRESS = "192.168.49.1";
    public static final int SET_COUNTRY_CODE = 143376;
    public static final int SET_MIRACAST_MODE = 143374;
    private static final String TAG = "WifiP2pService";
    private boolean mAutonomousGroup;
    private ClientHandler mClientHandler;
    private Context mContext;
    private DhcpStateMachine mDhcpStateMachine;
    private boolean mDiscoveryBlocked;
    private boolean mDiscoveryStarted;
    private boolean mJoinExistingGroup;
    private String mLastSetCountryCode;
    private Notification mNotification;
    INetworkManagementService mNwService;
    private P2pStateMachine mP2pStateMachine;
    private final boolean mP2pSupported;
    private String mServiceDiscReqId;
    private AsyncChannel mWifiChannel;
    private static final Boolean JOIN_GROUP = true;
    private static final boolean DBG = false;
    private static final Boolean FORM_GROUP = Boolean.valueOf(DBG);
    private static final Boolean RELOAD = true;
    private static final Boolean NO_RELOAD = Boolean.valueOf(DBG);
    private static int mGroupCreatingTimeoutIndex = 0;
    private static int mDisableP2pTimeoutIndex = 0;
    private AsyncChannel mReplyChannel = new AsyncChannel();
    private WifiP2pDevice mThisDevice = new WifiP2pDevice();
    private boolean mDiscoveryPostponed = DBG;
    private boolean mTemporarilyDisconnectedWifi = DBG;
    private byte mServiceTransactionId = 0;
    private HashMap<Messenger, ClientInfo> mClientInfoList = new HashMap<>();
    private String mInterface = "p2p0";
    private NetworkInfo mNetworkInfo = new NetworkInfo(13, 0, NETWORKTYPE, "");

    static byte access$12704(WifiP2pServiceImpl x0) {
        byte b = (byte) (x0.mServiceTransactionId + 1);
        x0.mServiceTransactionId = b;
        return b;
    }

    static int access$1504() {
        int i = mDisableP2pTimeoutIndex + 1;
        mDisableP2pTimeoutIndex = i;
        return i;
    }

    static int access$7404() {
        int i = mGroupCreatingTimeoutIndex + 1;
        mGroupCreatingTimeoutIndex = i;
        return i;
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
        UNKNOWN;

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
                case Node.ENTITY_REF:
                    return PREVIOUS_PROTOCOL_ERROR;
                case Node.IGNORABLE_WHITESPACE:
                    return NO_COMMON_CHANNEL;
                case Node.PROCESSING_INSTRUCTION:
                    return UNKNOWN_P2P_GROUP;
                case Node.COMMENT:
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
        this.mThisDevice.primaryDeviceType = this.mContext.getResources().getString(R.string.config_helpPackageNameValue);
        HandlerThread wifiP2pThread = new HandlerThread(TAG);
        wifiP2pThread.start();
        this.mClientHandler = new ClientHandler(wifiP2pThread.getLooper());
        this.mP2pStateMachine = new P2pStateMachine(TAG, wifiP2pThread.getLooper(), this.mP2pSupported);
        this.mP2pStateMachine.start();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.TETHER_LEASE_CHANGED");
        this.mContext.registerReceiver(new WifiStateReceiver(), filter);
    }

    public void connectivityServiceReady() {
        IBinder b = ServiceManager.getService("network_management");
        this.mNwService = INetworkManagementService.Stub.asInterface(b);
    }

    public class DhcpLease {
        public String mIp;
        public String mMac;

        public DhcpLease() {
        }
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        private WifiStateReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.conn.TETHER_LEASE_CHANGED".equals(action)) {
                Slog.d(WifiP2pServiceImpl.TAG, "ACTION_TETHER_LEASE_CHANGE received.");
                DhcpLease lease = WifiP2pServiceImpl.this.new DhcpLease();
                String event = intent.getStringExtra("dhcpEvent");
                lease.mMac = intent.getStringExtra("dhcpMac").toUpperCase();
                lease.mIp = intent.getStringExtra("dhcpIp");
                if (event.equals("add") || event.equals("update")) {
                    WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.DHCP_SERVER_NEW_LEASE, lease);
                }
            }
        }
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
        private WifiP2pGroup mSavedP2pGroup;
        private WifiP2pConfig mSavedPeerConfig;
        private UserAuthorizingInviteRequestState mUserAuthorizingInviteRequestState;
        private UserAuthorizingJoinState mUserAuthorizingJoinState;
        private UserAuthorizingNegotiationRequestState mUserAuthorizingNegotiationRequestState;
        private WifiMonitor mWifiMonitor;
        private WifiNative mWifiNative;
        private final WifiP2pInfo mWifiP2pInfo;
        private String wpsAddr;
        private String wpsPin;

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
            this.mWifiNative = new WifiNative(WifiP2pServiceImpl.this.mInterface);
            this.mWifiMonitor = new WifiMonitor(this, this.mWifiNative);
            this.mPeers = new WifiP2pDeviceList();
            this.mPeersLostDuringConnection = new WifiP2pDeviceList();
            this.mGroups = new WifiP2pGroupList((WifiP2pGroupList) null, new WifiP2pGroupList.GroupDeleteListener() {
                public void onDeleteGroup(int netId) {
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
        }

        class DefaultState extends State {
            DefaultState() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 69632:
                        if (message.arg1 == 0) {
                            WifiP2pServiceImpl.this.mWifiChannel = (AsyncChannel) message.obj;
                        } else {
                            P2pStateMachine.this.loge("Full connection failure, error = " + message.arg1);
                            WifiP2pServiceImpl.this.mWifiChannel = null;
                        }
                        break;
                    case 69633:
                        AsyncChannel ac = new AsyncChannel();
                        ac.connect(WifiP2pServiceImpl.this.mContext, P2pStateMachine.this.getHandler(), message.replyTo);
                        break;
                    case 69636:
                        if (message.arg1 == 2) {
                            P2pStateMachine.this.loge("Send failed, client connection lost");
                        } else {
                            P2pStateMachine.this.loge("Client connection lost with reason: " + message.arg1);
                        }
                        WifiP2pServiceImpl.this.mWifiChannel = null;
                        break;
                    case WifiStateMachine.CMD_ENABLE_P2P:
                    case 139329:
                    case 139332:
                    case 139335:
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT:
                    case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT:
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT:
                    case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE:
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE:
                    case WifiP2pServiceImpl.SET_COUNTRY_CODE:
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
                    case 196612:
                    case 196613:
                    case 196614:
                        break;
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
                        break;
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        break;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 2);
                        break;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 2);
                        break;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 2);
                        break;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 2);
                        break;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 2);
                        break;
                    case 139283:
                        P2pStateMachine.this.replyToMessage(message, 139284, new WifiP2pDeviceList(P2pStateMachine.this.mPeers));
                        break;
                    case 139285:
                        P2pStateMachine.this.replyToMessage(message, 139286, new WifiP2pInfo(P2pStateMachine.this.mWifiP2pInfo));
                        break;
                    case 139287:
                        P2pStateMachine.this.replyToMessage(message, 139288, P2pStateMachine.this.mGroup != null ? new WifiP2pGroup(P2pStateMachine.this.mGroup) : null);
                        break;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 2);
                        break;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 2);
                        break;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 2);
                        break;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 2);
                        break;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 2);
                        break;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 2);
                        break;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        break;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 2);
                        break;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139318, 2);
                        break;
                    case 139321:
                        P2pStateMachine.this.replyToMessage(message, 139322, new WifiP2pGroupList(P2pStateMachine.this.mGroups, (WifiP2pGroupList.GroupDeleteListener) null));
                        break;
                    case 139323:
                        P2pStateMachine.this.replyToMessage(message, 139324, 2);
                        break;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 2);
                        break;
                    case 139339:
                    case 139340:
                        P2pStateMachine.this.replyToMessage(message, 139341, (Object) null);
                        break;
                    case 139342:
                    case 139343:
                        P2pStateMachine.this.replyToMessage(message, 139345, 2);
                        break;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                        WifiP2pServiceImpl.this.mDiscoveryBlocked = message.arg1 == 1;
                        WifiP2pServiceImpl.this.mDiscoveryPostponed = WifiP2pServiceImpl.DBG;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            try {
                                StateMachine m = (StateMachine) message.obj;
                                m.sendMessage(message.arg2);
                            } catch (Exception e) {
                                P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                            }
                        }
                        break;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                        P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                        P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        break;
                    default:
                        P2pStateMachine.this.loge("Unhandled message " + message);
                        return WifiP2pServiceImpl.DBG;
                }
                return true;
            }
        }

        class P2pNotSupportedState extends State {
            P2pNotSupportedState() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 1);
                        break;
                    case 139268:
                        P2pStateMachine.this.replyToMessage(message, 139269, 1);
                        break;
                    case 139271:
                        P2pStateMachine.this.replyToMessage(message, 139272, 1);
                        break;
                    case 139274:
                        P2pStateMachine.this.replyToMessage(message, 139275, 1);
                        break;
                    case 139277:
                        P2pStateMachine.this.replyToMessage(message, 139278, 1);
                        break;
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139281, 1);
                        break;
                    case 139292:
                        P2pStateMachine.this.replyToMessage(message, 139293, 1);
                        break;
                    case 139295:
                        P2pStateMachine.this.replyToMessage(message, 139296, 1);
                        break;
                    case 139298:
                        P2pStateMachine.this.replyToMessage(message, 139299, 1);
                        break;
                    case 139301:
                        P2pStateMachine.this.replyToMessage(message, 139302, 1);
                        break;
                    case 139304:
                        P2pStateMachine.this.replyToMessage(message, 139305, 1);
                        break;
                    case 139307:
                        P2pStateMachine.this.replyToMessage(message, 139308, 1);
                        break;
                    case 139310:
                        P2pStateMachine.this.replyToMessage(message, 139311, 1);
                        break;
                    case 139315:
                        P2pStateMachine.this.replyToMessage(message, 139316, 1);
                        break;
                    case 139318:
                        P2pStateMachine.this.replyToMessage(message, 139318, 1);
                        break;
                    case 139323:
                        P2pStateMachine.this.replyToMessage(message, 139324, 1);
                        break;
                    case 139326:
                        P2pStateMachine.this.replyToMessage(message, 139327, 1);
                        break;
                    case 139329:
                        P2pStateMachine.this.replyToMessage(message, 139330, 1);
                        break;
                    case 139332:
                        P2pStateMachine.this.replyToMessage(message, 139333, 1);
                        break;
                }
                return true;
            }
        }

        class P2pDisablingState extends State {
            P2pDisablingState() {
            }

            public void enter() {
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT, WifiP2pServiceImpl.access$1504(), 0), 5000L);
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P:
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case WifiP2pServiceImpl.DISABLE_P2P_TIMED_OUT:
                        if (WifiP2pServiceImpl.mDisableP2pTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.loge("P2p disable timed out");
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        }
                        return true;
                    case WifiMonitor.SUP_DISCONNECTION_EVENT:
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        return true;
                    default:
                        return WifiP2pServiceImpl.DBG;
                }
            }

            public void exit() {
                try {
                    WifiP2pServiceImpl.this.mNwService.setInterfaceDown(WifiP2pServiceImpl.this.mInterface);
                } catch (Exception e) {
                    P2pStateMachine.this.loge("Unable to change interface " + WifiP2pServiceImpl.this.mInterface + " settings: " + e);
                }
                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiStateMachine.CMD_DISABLE_P2P_RSP);
            }
        }

        class P2pDisabledState extends State {
            P2pDisabledState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P:
                        try {
                            WifiP2pServiceImpl.this.mNwService.setInterfaceUp(WifiP2pServiceImpl.this.mInterface);
                            break;
                        } catch (RemoteException re) {
                            P2pStateMachine.this.loge("Unable to change interface settings: " + re);
                        } catch (IllegalStateException ie) {
                            P2pStateMachine.this.loge("Unable to change interface settings: " + ie);
                        }
                        P2pStateMachine.this.mWifiMonitor.startMonitoring();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pEnablingState);
                        return true;
                    default:
                        return WifiP2pServiceImpl.DBG;
                }
            }
        }

        class P2pEnablingState extends State {
            P2pEnablingState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case WifiStateMachine.CMD_ENABLE_P2P:
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case WifiMonitor.SUP_CONNECTION_EVENT:
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiMonitor.SUP_DISCONNECTION_EVENT:
                        P2pStateMachine.this.loge("P2p socket connection failed");
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisabledState);
                        return true;
                    default:
                        return WifiP2pServiceImpl.DBG;
                }
            }
        }

        class P2pEnabledState extends State {
            P2pEnabledState() {
            }

            public void enter() {
                P2pStateMachine.this.sendP2pStateChangedBroadcast(true);
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(true);
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                P2pStateMachine.this.initializeP2pSettings();
            }

            public boolean processMessage(Message message) {
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
                        P2pStateMachine.this.mWifiMonitor.stopMonitoring();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mP2pDisablingState);
                        return true;
                    case 139265:
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        } else {
                            P2pStateMachine.this.clearSupplicantServiceRequest();
                            if (P2pStateMachine.this.mWifiNative.p2pFind(120)) {
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
                        return true;
                    case 139292:
                        WifiP2pServiceInfo servInfo = (WifiP2pServiceInfo) message.obj;
                        if (P2pStateMachine.this.addLocalService(message.replyTo, servInfo)) {
                            P2pStateMachine.this.replyToMessage(message, 139294);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139293);
                        }
                        return true;
                    case 139295:
                        WifiP2pServiceInfo servInfo2 = (WifiP2pServiceInfo) message.obj;
                        P2pStateMachine.this.removeLocalService(message.replyTo, servInfo2);
                        P2pStateMachine.this.replyToMessage(message, 139297);
                        return true;
                    case 139298:
                        P2pStateMachine.this.clearLocalServices(message.replyTo);
                        P2pStateMachine.this.replyToMessage(message, 139300);
                        return true;
                    case 139301:
                        if (!P2pStateMachine.this.addServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj)) {
                            P2pStateMachine.this.replyToMessage(message, 139302);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139303);
                        }
                        return true;
                    case 139304:
                        P2pStateMachine.this.removeServiceRequest(message.replyTo, (WifiP2pServiceRequest) message.obj);
                        P2pStateMachine.this.replyToMessage(message, 139306);
                        return true;
                    case 139307:
                        P2pStateMachine.this.clearServiceRequests(message.replyTo);
                        P2pStateMachine.this.replyToMessage(message, 139309);
                        return true;
                    case 139310:
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked) {
                            P2pStateMachine.this.replyToMessage(message, 139311, 2);
                        } else if (!P2pStateMachine.this.updateSupplicantServiceRequest()) {
                            P2pStateMachine.this.replyToMessage(message, 139311, 3);
                        } else if (P2pStateMachine.this.mWifiNative.p2pFind(120)) {
                            P2pStateMachine.this.replyToMessage(message, 139312);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139311, 0);
                        }
                        return true;
                    case 139315:
                        WifiP2pDevice d = (WifiP2pDevice) message.obj;
                        if (d == null || !P2pStateMachine.this.setAndPersistDeviceName(d.deviceName)) {
                            P2pStateMachine.this.replyToMessage(message, 139316, 0);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139317);
                        }
                        return true;
                    case 139318:
                        P2pStateMachine.this.mGroups.remove(message.arg1);
                        P2pStateMachine.this.replyToMessage(message, 139320);
                        return true;
                    case 139323:
                        WifiP2pWfdInfo d2 = (WifiP2pWfdInfo) message.obj;
                        if (d2 == null || !P2pStateMachine.this.setWfdInfo(d2)) {
                            P2pStateMachine.this.replyToMessage(message, 139324, 0);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139325);
                        }
                        return true;
                    case 139329:
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139331);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139330);
                        }
                        return true;
                    case 139332:
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(WifiP2pServiceImpl.DBG, 0, 0)) {
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
                        if (P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                            P2pStateMachine.this.replyToMessage(message, 139337);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139336);
                        }
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
                    case WifiP2pServiceImpl.SET_MIRACAST_MODE:
                        P2pStateMachine.this.mWifiNative.setMiracastMode(message.arg1);
                        return true;
                    case WifiP2pServiceImpl.BLOCK_DISCOVERY:
                        boolean blocked = message.arg1 == 1 ? true : WifiP2pServiceImpl.DBG;
                        if (WifiP2pServiceImpl.this.mDiscoveryBlocked != blocked) {
                            WifiP2pServiceImpl.this.mDiscoveryBlocked = blocked;
                            if (blocked && WifiP2pServiceImpl.this.mDiscoveryStarted) {
                                P2pStateMachine.this.mWifiNative.p2pStopFind();
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = true;
                            }
                            if (!blocked && WifiP2pServiceImpl.this.mDiscoveryPostponed) {
                                WifiP2pServiceImpl.this.mDiscoveryPostponed = WifiP2pServiceImpl.DBG;
                                P2pStateMachine.this.mWifiNative.p2pFind(120);
                            }
                            if (blocked) {
                                try {
                                    StateMachine m = (StateMachine) message.obj;
                                    m.sendMessage(message.arg2);
                                } catch (Exception e) {
                                    P2pStateMachine.this.loge("unable to send BLOCK_DISCOVERY response: " + e);
                                }
                            }
                            break;
                        }
                        return true;
                    case WifiP2pServiceImpl.SET_COUNTRY_CODE:
                        String countryCode = ((String) message.obj).toUpperCase(Locale.ROOT);
                        if ((WifiP2pServiceImpl.this.mLastSetCountryCode == null || !countryCode.equals(WifiP2pServiceImpl.this.mLastSetCountryCode)) && P2pStateMachine.this.mWifiNative.setCountryCode(countryCode)) {
                            WifiP2pServiceImpl.this.mLastSetCountryCode = countryCode;
                        }
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
                        }
                        return true;
                    case WifiMonitor.P2P_DEVICE_LOST_EVENT:
                        if (P2pStateMachine.this.mPeers.remove(((WifiP2pDevice) message.obj).deviceAddress) != null) {
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        }
                        return true;
                    case WifiMonitor.P2P_FIND_STOPPED_EVENT:
                        P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(WifiP2pServiceImpl.DBG);
                        return true;
                    case WifiMonitor.P2P_SERV_DISC_RESP_EVENT:
                        List<WifiP2pServiceResponse> sdRespList = (List) message.obj;
                        for (WifiP2pServiceResponse resp : sdRespList) {
                            WifiP2pDevice dev = P2pStateMachine.this.mPeers.get(resp.getSrcDevice().deviceAddress);
                            resp.setSrcDevice(dev);
                            P2pStateMachine.this.sendServiceResponse(resp);
                        }
                        return true;
                    default:
                        return WifiP2pServiceImpl.DBG;
                }
            }

            public void exit() {
                P2pStateMachine.this.sendP2pDiscoveryChangedBroadcast(WifiP2pServiceImpl.DBG);
                P2pStateMachine.this.sendP2pStateChangedBroadcast(WifiP2pServiceImpl.DBG);
                WifiP2pServiceImpl.this.mNetworkInfo.setIsAvailable(WifiP2pServiceImpl.DBG);
                WifiP2pServiceImpl.this.mLastSetCountryCode = null;
            }
        }

        class InactiveState extends State {
            InactiveState() {
            }

            public void enter() {
                P2pStateMachine.this.mSavedPeerConfig.invalidate();
            }

            public boolean processMessage(Message message) {
                boolean ret;
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
                        WifiP2pConfig config = (WifiP2pConfig) message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(config)) {
                            WifiP2pServiceImpl.this.mAutonomousGroup = WifiP2pServiceImpl.DBG;
                            P2pStateMachine.this.mWifiNative.p2pStopFind();
                            if (P2pStateMachine.this.reinvokePersistentGroup(config)) {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            } else {
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mProvisionDiscoveryState);
                            }
                            P2pStateMachine.this.mSavedPeerConfig = config;
                            P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                            P2pStateMachine.this.replyToMessage(message, 139273);
                        } else {
                            P2pStateMachine.this.loge("Dropping connect requeset " + config);
                            P2pStateMachine.this.replyToMessage(message, 139272);
                        }
                        return true;
                    case 139277:
                        WifiP2pServiceImpl.this.mAutonomousGroup = true;
                        if (message.arg1 == -2) {
                            int netId = P2pStateMachine.this.mGroups.getNetworkId(WifiP2pServiceImpl.this.mThisDevice.deviceAddress);
                            ret = netId != -1 ? P2pStateMachine.this.mWifiNative.p2pGroupAdd(netId) : P2pStateMachine.this.mWifiNative.p2pGroupAdd(true);
                        } else {
                            ret = P2pStateMachine.this.mWifiNative.p2pGroupAdd(WifiP2pServiceImpl.DBG);
                        }
                        if (ret) {
                            P2pStateMachine.this.replyToMessage(message, 139279);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139278, 0);
                        }
                        return true;
                    case 139329:
                        P2pStateMachine.this.mWifiNative.p2pFlush();
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(true, 500, 500)) {
                            P2pStateMachine.this.replyToMessage(message, 139331);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139330);
                        }
                        return true;
                    case 139332:
                        if (P2pStateMachine.this.mWifiNative.p2pExtListen(WifiP2pServiceImpl.DBG, 0, 0)) {
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
                        if (P2pStateMachine.this.mWifiNative.p2pSetChannel(lc, oc)) {
                            P2pStateMachine.this.replyToMessage(message, 139337);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139336);
                        }
                        return true;
                    case 139342:
                        String handoverSelect = null;
                        if (message.obj != null) {
                            handoverSelect = ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE");
                        }
                        if (handoverSelect == null || !P2pStateMachine.this.mWifiNative.initiatorReportNfcHandover(handoverSelect)) {
                            P2pStateMachine.this.replyToMessage(message, 139345);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139344);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                        }
                        return true;
                    case 139343:
                        String handoverRequest = null;
                        if (message.obj != null) {
                            handoverRequest = ((Bundle) message.obj).getString("android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE");
                        }
                        if (handoverRequest == null || !P2pStateMachine.this.mWifiNative.responderReportNfcHandover(handoverRequest)) {
                            P2pStateMachine.this.replyToMessage(message, 139345);
                        } else {
                            P2pStateMachine.this.replyToMessage(message, 139344);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatingState);
                        }
                        return true;
                    case WifiMonitor.P2P_GO_NEGOTIATION_REQUEST_EVENT:
                        WifiP2pConfig config2 = (WifiP2pConfig) message.obj;
                        if (!P2pStateMachine.this.isConfigInvalid(config2)) {
                            P2pStateMachine.this.mSavedPeerConfig = config2;
                            if (P2pStateMachine.this.wpsPin != null && P2pStateMachine.this.wpsAddr != null && P2pStateMachine.this.wpsPin.length() == 8 && P2pStateMachine.this.wpsAddr.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress)) {
                                P2pStateMachine.this.mSavedPeerConfig.wps.pin = P2pStateMachine.this.wpsPin;
                            }
                            P2pStateMachine.this.notifyRtspPort(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, P2pStateMachine.this.wpsPin);
                            WifiP2pServiceImpl.this.mAutonomousGroup = WifiP2pServiceImpl.DBG;
                            WifiP2pServiceImpl.this.mJoinExistingGroup = WifiP2pServiceImpl.DBG;
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                        } else {
                            P2pStateMachine.this.loge("Dropping GO neg request " + config2);
                        }
                        return true;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                        if (P2pStateMachine.this.mGroup.getNetworkId() == -2) {
                            WifiP2pServiceImpl.this.mAutonomousGroup = WifiP2pServiceImpl.DBG;
                            P2pStateMachine.this.deferMessage(message);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        } else {
                            P2pStateMachine.this.loge("Unexpected group creation, remove " + P2pStateMachine.this.mGroup);
                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        }
                        return true;
                    case WifiMonitor.P2P_INVITATION_RECEIVED_EVENT:
                        WifiP2pGroup group = (WifiP2pGroup) message.obj;
                        WifiP2pDevice owner = group.getOwner();
                        if (owner == null) {
                            P2pStateMachine.this.loge("Ignored invitation from null owner");
                        } else {
                            WifiP2pConfig config3 = new WifiP2pConfig();
                            config3.deviceAddress = group.getOwner().deviceAddress;
                            if (!P2pStateMachine.this.isConfigInvalid(config3)) {
                                P2pStateMachine.this.mSavedPeerConfig = config3;
                                WifiP2pDevice owner2 = P2pStateMachine.this.mPeers.get(owner.deviceAddress);
                                if (owner2 != null) {
                                    if (owner2.wpsPbcSupported()) {
                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 0;
                                    } else if (owner2.wpsKeypadSupported()) {
                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 2;
                                    } else if (owner2.wpsDisplaySupported()) {
                                        P2pStateMachine.this.mSavedPeerConfig.wps.setup = 1;
                                    }
                                }
                                WifiP2pServiceImpl.this.mAutonomousGroup = WifiP2pServiceImpl.DBG;
                                WifiP2pServiceImpl.this.mJoinExistingGroup = true;
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingInviteRequestState);
                            } else {
                                P2pStateMachine.this.loge("Dropping invitation request " + config3);
                            }
                        }
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                        P2pStateMachine.this.wpsPin = null;
                        P2pStateMachine.this.wpsAddr = null;
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message.obj;
                        P2pStateMachine.this.wpsPin = provDisc.pin;
                        P2pStateMachine.this.wpsAddr = provDisc.device.deviceAddress;
                        AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle("WPS Pin : " + P2pStateMachine.this.wpsPin).setPositiveButton("OK", (DialogInterface.OnClickListener) null).create();
                        dialog.getWindow().setType(2003);
                        dialog.show();
                        return true;
                    default:
                        return WifiP2pServiceImpl.DBG;
                }
            }
        }

        class GroupCreatingState extends State {
            GroupCreatingState() {
            }

            public void enter() {
                P2pStateMachine.this.sendMessageDelayed(P2pStateMachine.this.obtainMessage(WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT, WifiP2pServiceImpl.access$7404(), 0), 120000L);
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 139265:
                        P2pStateMachine.this.replyToMessage(message, 139266, 2);
                        break;
                    case 139274:
                        P2pStateMachine.this.mWifiNative.p2pCancelConnect();
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        P2pStateMachine.this.replyToMessage(message, 139276);
                        break;
                    case WifiP2pServiceImpl.GROUP_CREATING_TIMED_OUT:
                        if (WifiP2pServiceImpl.mGroupCreatingTimeoutIndex == message.arg1) {
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        }
                        break;
                    case WifiMonitor.P2P_DEVICE_LOST_EVENT:
                        WifiP2pDevice device = (WifiP2pDevice) message.obj;
                        if (P2pStateMachine.this.mSavedPeerConfig.deviceAddress.equals(device.deviceAddress)) {
                            P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                            break;
                        }
                        break;
                    case WifiMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                        WifiP2pServiceImpl.this.mAutonomousGroup = WifiP2pServiceImpl.DBG;
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                }
                return true;
            }
        }

        class UserAuthorizingNegotiationRequestState extends State {
            UserAuthorizingNegotiationRequestState() {
            }

            public void enter() {
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                }
                return true;
            }

            public void exit() {
            }
        }

        class UserAuthorizingInviteRequestState extends State {
            UserAuthorizingInviteRequestState() {
            }

            public void enter() {
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (!P2pStateMachine.this.reinvokePersistentGroup(P2pStateMachine.this.mSavedPeerConfig)) {
                            P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                        }
                        P2pStateMachine.this.mPeers.updateStatus(P2pStateMachine.this.mSavedPeerConfig.deviceAddress, 1);
                        P2pStateMachine.this.sendPeersChangedBroadcast();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                }
                return true;
            }

            public void exit() {
            }
        }

        class ProvisionDiscoveryState extends State {
            ProvisionDiscoveryState() {
            }

            public void enter() {
                P2pStateMachine.this.mWifiNative.p2pProvisionDiscovery(P2pStateMachine.this.mSavedPeerConfig);
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case WifiMonitor.P2P_PROV_DISC_PBC_RSP_EVENT:
                        if (((WifiP2pProvDiscEvent) message.obj).device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                            P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        }
                        break;
                    case WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                        if (((WifiP2pProvDiscEvent) message.obj).device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 2) {
                            if (TextUtils.isEmpty(P2pStateMachine.this.mSavedPeerConfig.wps.pin)) {
                                WifiP2pServiceImpl.this.mJoinExistingGroup = WifiP2pServiceImpl.DBG;
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mUserAuthorizingNegotiationRequestState);
                            } else {
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                                P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                            }
                        }
                        break;
                    case WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message.obj;
                        WifiP2pDevice device = provDisc.device;
                        if (device.deviceAddress.equals(P2pStateMachine.this.mSavedPeerConfig.deviceAddress) && P2pStateMachine.this.mSavedPeerConfig.wps.setup == 1) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.pin = provDisc.pin;
                            P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            P2pStateMachine.this.notifyInvitationSent(provDisc.pin, device.deviceAddress);
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        }
                        break;
                    case WifiMonitor.P2P_FIND_STOPPED_EVENT:
                    case WifiMonitor.P2P_SERV_DISC_RESP_EVENT:
                    default:
                        return WifiP2pServiceImpl.DBG;
                    case WifiMonitor.P2P_PROV_DISC_FAILURE_EVENT:
                        P2pStateMachine.this.loge("provision discovery failed");
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                }
                return true;
            }
        }

        class GroupNegotiationState extends State {
            GroupNegotiationState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case WifiMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                    case WifiMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT:
                        return true;
                    case WifiMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                        if (((P2pStatus) message.obj) == P2pStatus.NO_COMMON_CHANNEL) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                        } else {
                            P2pStateMachine.this.handleGroupCreationFailure();
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        }
                        return true;
                    case WifiMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                        if (((P2pStatus) message.obj) == P2pStatus.NO_COMMON_CHANNEL) {
                            P2pStateMachine.this.transitionTo(P2pStateMachine.this.mFrequencyConflictState);
                        }
                        return true;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.mGroup = (WifiP2pGroup) message.obj;
                        if (P2pStateMachine.this.mGroup.getNetworkId() == -2) {
                            P2pStateMachine.this.updatePersistentNetworks(WifiP2pServiceImpl.NO_RELOAD.booleanValue());
                            String devAddr = P2pStateMachine.this.mGroup.getOwner().deviceAddress;
                            P2pStateMachine.this.mGroup.setNetworkId(P2pStateMachine.this.mGroups.getNetworkId(devAddr, P2pStateMachine.this.mGroup.getNetworkName()));
                        }
                        if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                            if (!WifiP2pServiceImpl.this.mAutonomousGroup) {
                                P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                            }
                            P2pStateMachine.this.startDhcpServer(P2pStateMachine.this.mGroup.getInterface());
                        } else {
                            P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 10);
                            WifiP2pServiceImpl.this.mDhcpStateMachine = DhcpStateMachine.makeDhcpStateMachine(WifiP2pServiceImpl.this.mContext, P2pStateMachine.this, P2pStateMachine.this.mGroup.getInterface());
                            P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), WifiP2pServiceImpl.DBG);
                            WifiP2pServiceImpl.this.mDhcpStateMachine.sendMessage(196609);
                            WifiP2pDevice groupOwner = P2pStateMachine.this.mGroup.getOwner();
                            WifiP2pDevice peer = P2pStateMachine.this.mPeers.get(groupOwner.deviceAddress);
                            if (peer == null) {
                                P2pStateMachine.this.logw("Unknown group owner " + groupOwner);
                            } else {
                                groupOwner.updateSupplicantDetails(peer);
                                P2pStateMachine.this.mPeers.updateStatus(groupOwner.deviceAddress, 0);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                            }
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        return true;
                    case WifiMonitor.P2P_GROUP_REMOVED_EVENT:
                        break;
                    case WifiMonitor.P2P_INVITATION_RECEIVED_EVENT:
                    default:
                        return WifiP2pServiceImpl.DBG;
                    case WifiMonitor.P2P_INVITATION_RESULT_EVENT:
                        P2pStatus status = (P2pStatus) message.obj;
                        if (status != P2pStatus.SUCCESS) {
                            P2pStateMachine.this.loge("Invitation result " + status);
                            if (status == P2pStatus.UNKNOWN_P2P_GROUP) {
                                int netId = P2pStateMachine.this.mSavedPeerConfig.netId;
                                if (netId >= 0) {
                                    P2pStateMachine.this.removeClientFromList(netId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, true);
                                }
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            } else if (status == P2pStatus.INFORMATION_IS_CURRENTLY_UNAVAILABLE) {
                                P2pStateMachine.this.mSavedPeerConfig.netId = -2;
                                P2pStateMachine.this.p2pConnectWithPinDisplay(P2pStateMachine.this.mSavedPeerConfig);
                            } else if (status == P2pStatus.NO_COMMON_CHANNEL) {
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
                notifyFrequencyConflict();
            }

            private void notifyFrequencyConflict() {
                P2pStateMachine.this.logd("Notify frequency conflict");
                Resources r = Resources.getSystem();
                AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setMessage(r.getString(R.string.imei, P2pStateMachine.this.getDeviceName(P2pStateMachine.this.mSavedPeerConfig.deviceAddress))).setPositiveButton(r.getString(R.string.indeterminate_progress_39), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog2, int which) {
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT);
                    }
                }).setNegativeButton(r.getString(R.string.ime_action_default), new DialogInterface.OnClickListener() {
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
                switch (message.what) {
                    case WifiP2pServiceImpl.DROP_WIFI_USER_ACCEPT:
                        WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 1);
                        WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = true;
                        break;
                    case WifiP2pServiceImpl.DROP_WIFI_USER_REJECT:
                        P2pStateMachine.this.handleGroupCreationFailure();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        break;
                    case WifiP2pServiceImpl.DISCONNECT_WIFI_RESPONSE:
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                        break;
                    case WifiMonitor.P2P_GO_NEGOTIATION_SUCCESS_EVENT:
                    case WifiMonitor.P2P_GROUP_FORMATION_SUCCESS_EVENT:
                        P2pStateMachine.this.loge(getName() + "group sucess during freq conflict!");
                        break;
                    case WifiMonitor.P2P_GO_NEGOTIATION_FAILURE_EVENT:
                    case WifiMonitor.P2P_GROUP_FORMATION_FAILURE_EVENT:
                    case WifiMonitor.P2P_GROUP_REMOVED_EVENT:
                        break;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.loge(getName() + "group started after freq conflict, handle anyway");
                        P2pStateMachine.this.deferMessage(message);
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupNegotiationState);
                        break;
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
                P2pStateMachine.this.mSavedPeerConfig.invalidate();
                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                P2pStateMachine.this.updateThisDevice(0);
                if (P2pStateMachine.this.mGroup.isGroupOwner()) {
                    P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS), null);
                }
                if (WifiP2pServiceImpl.this.mAutonomousGroup) {
                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                }
            }

            public boolean processMessage(Message message) {
                int netId;
                switch (message.what) {
                    case WifiStateMachine.CMD_DISABLE_P2P_REQ:
                        P2pStateMachine.this.sendMessage(139280);
                        P2pStateMachine.this.deferMessage(message);
                        return true;
                    case 139271:
                        WifiP2pConfig config = (WifiP2pConfig) message.obj;
                        if (P2pStateMachine.this.isConfigInvalid(config)) {
                            P2pStateMachine.this.loge("Dropping connect requeset " + config);
                            P2pStateMachine.this.replyToMessage(message, 139272);
                        } else {
                            P2pStateMachine.this.logd("Inviting device : " + config.deviceAddress);
                            P2pStateMachine.this.mSavedPeerConfig = config;
                            if (P2pStateMachine.this.mWifiNative.p2pInvite(P2pStateMachine.this.mGroup, config.deviceAddress)) {
                                P2pStateMachine.this.mPeers.updateStatus(config.deviceAddress, 1);
                                P2pStateMachine.this.sendPeersChangedBroadcast();
                                P2pStateMachine.this.replyToMessage(message, 139273);
                            } else {
                                P2pStateMachine.this.replyToMessage(message, 139272, 0);
                            }
                        }
                        return true;
                    case 139280:
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
                        WpsInfo wps = (WpsInfo) message.obj;
                        if (wps == null) {
                            P2pStateMachine.this.replyToMessage(message, 139327);
                        } else {
                            boolean ret = true;
                            if (wps.setup == 0) {
                                ret = P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                            } else if (wps.pin == null) {
                                String pin = P2pStateMachine.this.mWifiNative.startWpsPinDisplay(P2pStateMachine.this.mGroup.getInterface());
                                try {
                                    Integer.parseInt(pin);
                                    P2pStateMachine.this.notifyInvitationSent(pin, "any");
                                } catch (NumberFormatException e) {
                                    ret = WifiP2pServiceImpl.DBG;
                                }
                            } else {
                                ret = P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), wps.pin);
                            }
                            P2pStateMachine.this.replyToMessage(message, ret ? 139328 : 139327);
                            break;
                        }
                        return true;
                    case WifiP2pServiceImpl.DHCP_SERVER_NEW_LEASE:
                        DhcpLease lease = (DhcpLease) message.obj;
                        for (WifiP2pDevice c : P2pStateMachine.this.mGroup.getClientList()) {
                            if (c.deviceAddress.toLowerCase().substring(3, 10).equals(lease.mMac.toLowerCase().substring(3, 10)) && c.deviceAddress.toLowerCase().substring(15, 16).equals(lease.mMac.toLowerCase().substring(15, 16))) {
                                P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(NetworkUtils.numericToInetAddress(WifiP2pServiceImpl.SERVER_ADDRESS), NetworkUtils.numericToInetAddress(lease.mIp));
                                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                            }
                        }
                        return true;
                    case WifiMonitor.P2P_DEVICE_LOST_EVENT:
                        WifiP2pDevice device = (WifiP2pDevice) message.obj;
                        if (P2pStateMachine.this.mGroup.contains(device)) {
                            P2pStateMachine.this.mPeersLostDuringConnection.updateSupplicantDetails(device);
                            return true;
                        }
                        return WifiP2pServiceImpl.DBG;
                    case WifiMonitor.P2P_GROUP_STARTED_EVENT:
                        P2pStateMachine.this.loge("Duplicate group creation event notice, ignore");
                        return true;
                    case WifiMonitor.P2P_GROUP_REMOVED_EVENT:
                        P2pStateMachine.this.handleGroupRemoved();
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mInactiveState);
                        return true;
                    case WifiMonitor.P2P_INVITATION_RESULT_EVENT:
                        P2pStatus status = (P2pStatus) message.obj;
                        if (status != P2pStatus.SUCCESS) {
                            P2pStateMachine.this.loge("Invitation result " + status);
                            if (status == P2pStatus.UNKNOWN_P2P_GROUP && (netId = P2pStateMachine.this.mGroup.getNetworkId()) >= 0) {
                                if (P2pStateMachine.this.removeClientFromList(netId, P2pStateMachine.this.mSavedPeerConfig.deviceAddress, WifiP2pServiceImpl.DBG)) {
                                    P2pStateMachine.this.sendMessage(139271, P2pStateMachine.this.mSavedPeerConfig);
                                } else {
                                    P2pStateMachine.this.loge("Already removed the client, ignore");
                                }
                            }
                        }
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        WifiP2pProvDiscEvent provDisc = (WifiP2pProvDiscEvent) message.obj;
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
                    case WifiMonitor.AP_STA_DISCONNECTED_EVENT:
                        WifiP2pDevice device2 = (WifiP2pDevice) message.obj;
                        String deviceAddress = device2.deviceAddress;
                        if (deviceAddress != null) {
                            P2pStateMachine.this.mPeers.updateStatus(deviceAddress, 3);
                            if (P2pStateMachine.this.mGroup.removeClient(deviceAddress)) {
                                if (WifiP2pServiceImpl.this.mAutonomousGroup || !P2pStateMachine.this.mGroup.isClientListEmpty()) {
                                    P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                                } else {
                                    P2pStateMachine.this.logd("Client list empty, remove non-persistent p2p group");
                                    P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                                }
                            } else {
                                for (WifiP2pDevice wifiP2pDevice : P2pStateMachine.this.mGroup.getClientList()) {
                                }
                            }
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        } else {
                            P2pStateMachine.this.loge("Disconnect on unknown device: " + device2);
                        }
                        return true;
                    case WifiMonitor.AP_STA_CONNECTED_EVENT:
                        String deviceAddress2 = ((WifiP2pDevice) message.obj).deviceAddress;
                        P2pStateMachine.this.mWifiNative.setP2pGroupIdle(P2pStateMachine.this.mGroup.getInterface(), 0);
                        if (deviceAddress2 != null) {
                            if (P2pStateMachine.this.mPeers.get(deviceAddress2) != null) {
                                P2pStateMachine.this.mGroup.addClient(P2pStateMachine.this.mPeers.get(deviceAddress2));
                            } else {
                                P2pStateMachine.this.mGroup.addClient(deviceAddress2);
                            }
                            P2pStateMachine.this.mPeers.updateStatus(deviceAddress2, 0);
                            P2pStateMachine.this.sendPeersChangedBroadcast();
                        } else {
                            P2pStateMachine.this.loge("Connect on null device address, ignore");
                        }
                        P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                        return true;
                    case 196613:
                        DhcpResults dhcpResults = (DhcpResults) message.obj;
                        if (message.arg1 == 1 && dhcpResults != null) {
                            P2pStateMachine.this.setWifiP2pInfoOnGroupFormation(dhcpResults.serverAddress, null);
                            P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
                            P2pStateMachine.this.mWifiNative.setP2pPowerSave(P2pStateMachine.this.mGroup.getInterface(), true);
                            try {
                                String iface = P2pStateMachine.this.mGroup.getInterface();
                                WifiP2pServiceImpl.this.mNwService.addInterfaceToLocalNetwork(iface, dhcpResults.getRoutes(iface));
                            } catch (RemoteException e2) {
                                P2pStateMachine.this.loge("Failed to add iface to local network " + e2);
                            }
                            break;
                        } else {
                            P2pStateMachine.this.loge("DHCP failed");
                            P2pStateMachine.this.mWifiNative.p2pGroupRemove(P2pStateMachine.this.mGroup.getInterface());
                        }
                        return true;
                    default:
                        return WifiP2pServiceImpl.DBG;
                }
            }

            public void exit() {
                P2pStateMachine.this.updateThisDevice(3);
                P2pStateMachine.this.resetWifiP2pInfo();
                WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, null);
                P2pStateMachine.this.sendP2pConnectionChangedBroadcast();
            }
        }

        class UserAuthorizingJoinState extends State {
            UserAuthorizingJoinState() {
            }

            public void enter() {
                P2pStateMachine.this.notifyInvitationReceived();
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT:
                        P2pStateMachine.this.mWifiNative.p2pStopFind();
                        if (P2pStateMachine.this.mSavedPeerConfig.wps.setup == 0) {
                            P2pStateMachine.this.mWifiNative.startWpsPbc(P2pStateMachine.this.mGroup.getInterface(), null);
                        } else {
                            P2pStateMachine.this.mWifiNative.startWpsPinKeypad(P2pStateMachine.this.mGroup.getInterface(), P2pStateMachine.this.mSavedPeerConfig.wps.pin);
                        }
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        return true;
                    case WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT:
                        P2pStateMachine.this.transitionTo(P2pStateMachine.this.mGroupCreatedState);
                        return true;
                    case WifiMonitor.P2P_PROV_DISC_PBC_REQ_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_ENTER_PIN_EVENT:
                    case WifiMonitor.P2P_PROV_DISC_SHOW_PIN_EVENT:
                        return true;
                    default:
                        return WifiP2pServiceImpl.DBG;
                }
            }

            public void exit() {
            }
        }

        class OngoingGroupRemovalState extends State {
            OngoingGroupRemovalState() {
            }

            public void enter() {
            }

            public boolean processMessage(Message message) {
                switch (message.what) {
                    case 139280:
                        P2pStateMachine.this.replyToMessage(message, 139282);
                        return true;
                    default:
                        return WifiP2pServiceImpl.DBG;
                }
            }
        }

        public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            super.dump(fd, pw, args);
            pw.println("mWifiP2pInfo " + this.mWifiP2pInfo);
            pw.println("mGroup " + this.mGroup);
            pw.println("mSavedPeerConfig " + this.mSavedPeerConfig);
            pw.println("mSavedP2pGroup " + this.mSavedP2pGroup);
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
            if (WifiP2pServiceImpl.this.mDiscoveryStarted != started) {
                WifiP2pServiceImpl.this.mDiscoveryStarted = started;
                Intent intent = new Intent("android.net.wifi.p2p.DISCOVERY_STATE_CHANGE");
                intent.addFlags(67108864);
                intent.putExtra("discoveryState", started ? 2 : 1);
                WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            }
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
            Intent intent = new Intent("android.net.wifi.p2p.CONNECTION_STATE_CHANGE");
            intent.addFlags(603979776);
            intent.putExtra("wifiP2pInfo", new WifiP2pInfo(this.mWifiP2pInfo));
            intent.putExtra("networkInfo", new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
            intent.putExtra("p2pGroupInfo", new WifiP2pGroup(this.mGroup));
            WifiP2pServiceImpl.this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.P2P_CONNECTION_CHANGED, new NetworkInfo(WifiP2pServiceImpl.this.mNetworkInfo));
        }

        private void sendP2pPersistentGroupsChangedBroadcast() {
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
                String[] arr$ = WifiP2pServiceImpl.this.mNwService.listTetheredInterfaces();
                int len$ = arr$.length;
                int i$ = 0;
                while (true) {
                    if (i$ < len$) {
                        String temp = arr$[i$];
                        logd("List all interfaces " + temp);
                        if (temp.compareTo(intf) == 0) {
                            i$++;
                        } else {
                            logd("Found other tethering interfaces, so keep tethering alive");
                            break;
                        }
                    } else {
                        WifiP2pServiceImpl.this.mNwService.stopTethering();
                        logd("Stopped Dhcp server");
                        break;
                    }
                }
            } catch (Exception e) {
                loge("Error stopping Dhcp server" + e);
            } finally {
                logd("Stopped Dhcp server");
            }
        }

        private void notifyP2pEnableFailure() {
            Resources r = Resources.getSystem();
            AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(R.string.imProtocolYahoo)).setMessage(r.getString(R.string.imTypeHome)).setPositiveButton(r.getString(R.string.ok), (DialogInterface.OnClickListener) null).create();
            dialog.getWindow().setType(2003);
            dialog.show();
        }

        private void addRowToDialog(ViewGroup group, int stringId, String value) {
            Resources r = Resources.getSystem();
            View row = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.notification_template_messaging_group, group, WifiP2pServiceImpl.DBG);
            ((TextView) row.findViewById(R.id.maps)).setText(r.getString(stringId));
            ((TextView) row.findViewById(R.id.fitEnd)).setText(value);
            group.addView(row);
        }

        private void notifyRtspPort(String peerAddress, String pin) {
            for (WifiP2pDevice d : this.mPeers.getDeviceList()) {
                if (d.deviceAddress.equals(peerAddress) && d.wfdInfo != null) {
                    Intent intent = new Intent("com.android.wifidisply.WPS_MESSAGE");
                    intent.addFlags(1073741824);
                    intent.putExtra("pinCode", pin);
                    intent.putExtra("peerAddress", peerAddress);
                    intent.putExtra("peerRTSPPort", d.wfdInfo.getControlPort());
                    WifiP2pServiceImpl.this.mContext.sendStickyBroadcast(intent);
                    return;
                }
            }
        }

        private void notifyInvitationSent(String pin, String peerAddress) {
            Resources r = Resources.getSystem();
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.notification_template_material_progress, (ViewGroup) null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(R.id.group_divider);
            addRowToDialog(group, R.string.ime_action_previous, getDeviceName(peerAddress));
            addRowToDialog(group, R.string.ime_action_send, pin);
            AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(R.string.ime_action_done)).setView(textEntryView).setPositiveButton(r.getString(R.string.ok), (DialogInterface.OnClickListener) null).create();
            dialog.getWindow().setType(2003);
            dialog.show();
        }

        private void notifyInvitationReceived() {
            if (this.mSavedPeerConfig.wps.pin != null && this.mSavedPeerConfig.wps.pin.length() == 8) {
                this.mSavedPeerConfig.wps.setup = 1;
                sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                return;
            }
            Resources r = Resources.getSystem();
            final WpsInfo wps = this.mSavedPeerConfig.wps;
            View textEntryView = LayoutInflater.from(WifiP2pServiceImpl.this.mContext).inflate(R.layout.notification_template_material_progress, (ViewGroup) null);
            ViewGroup group = (ViewGroup) textEntryView.findViewById(R.id.group_divider);
            addRowToDialog(group, R.string.ime_action_next, getDeviceName(this.mSavedPeerConfig.deviceAddress));
            final EditText pin = (EditText) textEntryView.findViewById(R.id.map);
            AlertDialog dialog = new AlertDialog.Builder(WifiP2pServiceImpl.this.mContext).setTitle(r.getString(R.string.ime_action_go)).setView(textEntryView).setPositiveButton(r.getString(R.string.image_wallpaper_component), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    if (wps.setup == 2) {
                        if (P2pStateMachine.this.mSavedPeerConfig != null) {
                            P2pStateMachine.this.mSavedPeerConfig.wps.pin = pin.getText().toString();
                        } else {
                            return;
                        }
                    }
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                }
            }).setNegativeButton(r.getString(R.string.ime_action_default), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog2, int which) {
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_REJECT);
                }
            }).create();
            switch (wps.setup) {
                case 1:
                    addRowToDialog(group, R.string.ime_action_send, wps.pin);
                    break;
                case 2:
                    textEntryView.findViewById(R.id.ltr).setVisibility(0);
                    break;
            }
            if ((r.getConfiguration().uiMode & 5) == 5) {
                dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialog2, int keyCode, KeyEvent event) {
                        if (keyCode != 164) {
                            return WifiP2pServiceImpl.DBG;
                        }
                        P2pStateMachine.this.sendMessage(WifiP2pServiceImpl.PEER_CONNECTION_USER_ACCEPT);
                        dialog2.dismiss();
                        return true;
                    }
                });
            }
            dialog.getWindow().setType(2003);
            dialog.show();
        }

        private void updatePersistentNetworks(boolean reload) {
            String listStr = this.mWifiNative.listNetworks();
            if (listStr != null) {
                boolean isSaveRequired = WifiP2pServiceImpl.DBG;
                String[] lines = listStr.split("\n");
                if (lines != null) {
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
                                        this.mWifiNative.removeNetwork(netId);
                                        isSaveRequired = true;
                                    } else if (!this.mGroups.contains(netId)) {
                                        WifiP2pGroup group = new WifiP2pGroup();
                                        group.setNetworkId(netId);
                                        group.setNetworkName(ssid);
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
                    if (reload || isSaveRequired) {
                        this.mWifiNative.saveConfig();
                        sendP2pPersistentGroupsChangedBroadcast();
                    }
                }
            }
        }

        private boolean isConfigInvalid(WifiP2pConfig config) {
            if (config == null || TextUtils.isEmpty(config.deviceAddress) || this.mPeers.get(config.deviceAddress) == null) {
                return true;
            }
            return WifiP2pServiceImpl.DBG;
        }

        private WifiP2pDevice fetchCurrentDeviceDetails(WifiP2pConfig config) {
            int gc = this.mWifiNative.getGroupCapability(config.deviceAddress);
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
            WifiP2pDevice dev = fetchCurrentDeviceDetails(config);
            boolean join = dev.isGroupOwner();
            String ssid = this.mWifiNative.p2pGetSsid(dev.deviceAddress);
            if (join && dev.isGroupLimit()) {
                join = WifiP2pServiceImpl.DBG;
            } else if (join && (netId = this.mGroups.getNetworkId(dev.deviceAddress, ssid)) >= 0) {
                if (this.mWifiNative.p2pGroupAdd(netId)) {
                    return true;
                }
                return WifiP2pServiceImpl.DBG;
            }
            if (!join && dev.isDeviceLimit()) {
                loge("target device reaches the device limit.");
                return WifiP2pServiceImpl.DBG;
            }
            if (join || !dev.isInvitationCapable()) {
                return WifiP2pServiceImpl.DBG;
            }
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
            if (netId2 < 0) {
                return WifiP2pServiceImpl.DBG;
            }
            if (this.mWifiNative.p2pReinvoke(netId2, dev.deviceAddress)) {
                config.netId = netId2;
                return true;
            }
            loge("p2pReinvoke() failed, update networks");
            updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
            return WifiP2pServiceImpl.DBG;
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
            boolean isClientRemoved = WifiP2pServiceImpl.DBG;
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
                this.mGroups.remove(netId);
                return true;
            }
            if (!isClientRemoved) {
                return WifiP2pServiceImpl.DBG;
            }
            if (modifiedClientList.length() == 0) {
                modifiedClientList.append("\"\"");
            }
            this.mWifiNative.setNetworkVariable(netId, "p2p_client_list", modifiedClientList.toString());
            this.mWifiNative.saveConfig();
            return true;
        }

        private void setWifiP2pInfoOnGroupFormation(InetAddress serverInetAddress, InetAddress clientAddress) {
            this.mWifiP2pInfo.groupFormed = true;
            this.mWifiP2pInfo.isGroupOwner = this.mGroup.isGroupOwner();
            this.mWifiP2pInfo.groupOwnerAddress = serverInetAddress;
            this.mWifiP2pInfo.clientAddress = clientAddress;
            this.mWifiP2pInfo.passphase = this.mGroup.getPassphrase();
            this.mWifiP2pInfo.frequency = this.mGroup.getFrequency();
            this.mWifiP2pInfo.interfaceName = this.mGroup.getInterface();
            this.mWifiP2pInfo.networkName = this.mGroup.getNetworkName();
        }

        private void resetWifiP2pInfo() {
            this.mWifiP2pInfo.groupFormed = WifiP2pServiceImpl.DBG;
            this.mWifiP2pInfo.isGroupOwner = WifiP2pServiceImpl.DBG;
            this.mWifiP2pInfo.groupOwnerAddress = null;
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
                return WifiP2pServiceImpl.DBG;
            }
            if (this.mWifiNative.setDeviceName(devName)) {
                WifiP2pServiceImpl.this.mThisDevice.deviceName = devName;
                this.mWifiNative.setP2pSsidPostfix("-" + WifiP2pServiceImpl.this.mThisDevice.deviceName);
                Settings.Global.putString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_p2p_device_name", devName);
                sendThisDeviceChangedBroadcast();
                return true;
            }
            loge("Failed to set device name " + devName);
            return WifiP2pServiceImpl.DBG;
        }

        private boolean setWfdInfo(WifiP2pWfdInfo wfdInfo) {
            boolean success;
            if (!wfdInfo.isWfdEnabled()) {
                success = this.mWifiNative.setWfdEnable(WifiP2pServiceImpl.DBG);
            } else {
                success = this.mWifiNative.setWfdEnable(true) && this.mWifiNative.setWfdDeviceInfo(wfdInfo.getDeviceInfoHex());
            }
            if (success) {
                WifiP2pServiceImpl.this.mThisDevice.wfdInfo = wfdInfo;
                sendThisDeviceChangedBroadcast();
                return true;
            }
            loge("Failed to set wfd properties");
            return WifiP2pServiceImpl.DBG;
        }

        private void initializeP2pSettings() {
            this.mWifiNative.setPersistentReconnect(true);
            WifiP2pServiceImpl.this.mThisDevice.deviceName = getPersistedDeviceName();
            this.mWifiNative.setDeviceName(WifiP2pServiceImpl.this.mThisDevice.deviceName);
            this.mWifiNative.setP2pSsidPostfix("-" + WifiP2pServiceImpl.this.mThisDevice.deviceName);
            this.mWifiNative.setDeviceType(WifiP2pServiceImpl.this.mThisDevice.primaryDeviceType);
            this.mWifiNative.setConfigMethods("virtual_push_button physical_display keypad");
            this.mWifiNative.setConcurrencyPriority("sta");
            WifiP2pServiceImpl.this.mThisDevice.deviceAddress = this.mWifiNative.p2pGetDeviceAddress();
            updateThisDevice(3);
            WifiP2pServiceImpl.this.mClientInfoList.clear();
            this.mWifiNative.p2pFlush();
            this.mWifiNative.p2pServiceFlush();
            WifiP2pServiceImpl.this.mServiceTransactionId = (byte) 0;
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            String countryCode = Settings.Global.getString(WifiP2pServiceImpl.this.mContext.getContentResolver(), "wifi_country_code");
            if (countryCode != null && !countryCode.isEmpty()) {
                WifiP2pServiceImpl.this.mP2pStateMachine.sendMessage(WifiP2pServiceImpl.SET_COUNTRY_CODE, countryCode);
            }
            updatePersistentNetworks(WifiP2pServiceImpl.RELOAD.booleanValue());
        }

        private void updateThisDevice(int status) {
            WifiP2pServiceImpl.this.mThisDevice.status = status;
            sendThisDeviceChangedBroadcast();
        }

        private void handleGroupCreationFailure() {
            resetWifiP2pInfo();
            WifiP2pServiceImpl.this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.FAILED, null, null);
            sendP2pConnectionChangedBroadcast();
            boolean peersChanged = this.mPeers.remove(this.mPeersLostDuringConnection);
            if (!TextUtils.isEmpty(this.mSavedPeerConfig.deviceAddress) && this.mPeers.remove(this.mSavedPeerConfig.deviceAddress) != null) {
                peersChanged = true;
            }
            if (peersChanged) {
                sendPeersChangedBroadcast();
            }
            this.mPeersLostDuringConnection.clear();
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            sendMessage(139265);
        }

        private void handleGroupRemoved() {
            if (!this.mGroup.isGroupOwner()) {
                WifiP2pServiceImpl.this.mDhcpStateMachine.sendMessage(196610);
                WifiP2pServiceImpl.this.mDhcpStateMachine.doQuit();
                WifiP2pServiceImpl.this.mDhcpStateMachine = null;
                try {
                    WifiP2pServiceImpl.this.mNwService.removeInterfaceFromLocalNetwork(this.mGroup.getInterface());
                } catch (RemoteException e) {
                    loge("Failed to remove iface from local network " + e);
                }
            } else {
                stopDhcpServer(this.mGroup.getInterface());
            }
            try {
                WifiP2pServiceImpl.this.mNwService.clearInterfaceAddresses(this.mGroup.getInterface());
            } catch (Exception e2) {
                loge("Failed to clear addresses " + e2);
            }
            NetworkUtils.resetConnections(this.mGroup.getInterface(), 3);
            this.mWifiNative.setP2pGroupIdle(this.mGroup.getInterface(), 0);
            boolean peersChanged = WifiP2pServiceImpl.DBG;
            for (WifiP2pDevice d : this.mGroup.getClientList()) {
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
            WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            if (WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi) {
                WifiP2pServiceImpl.this.mWifiChannel.sendMessage(WifiP2pServiceImpl.DISCONNECT_WIFI_REQUEST, 0);
                WifiP2pServiceImpl.this.mTemporarilyDisconnectedWifi = WifiP2pServiceImpl.DBG;
            }
        }

        private void replyToMessage(Message msg, int what) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private void replyToMessage(Message msg, int what, int arg1) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                dstMsg.arg1 = arg1;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private void replyToMessage(Message msg, int what, Object obj) {
            if (msg.replyTo != null) {
                Message dstMsg = obtainMessage(msg);
                dstMsg.what = what;
                dstMsg.obj = obj;
                WifiP2pServiceImpl.this.mReplyChannel.replyToMessage(msg, dstMsg);
            }
        }

        private Message obtainMessage(Message srcMsg) {
            Message msg = Message.obtain();
            msg.arg2 = srcMsg.arg2;
            return msg;
        }

        protected void logd(String s) {
            Slog.d(WifiP2pServiceImpl.TAG, s);
        }

        protected void loge(String s) {
            Slog.e(WifiP2pServiceImpl.TAG, s);
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
                return WifiP2pServiceImpl.DBG;
            }
            WifiP2pServiceImpl.this.mServiceDiscReqId = this.mWifiNative.p2pServDiscReq("00:00:00:00:00:00", sb.toString());
            if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                return true;
            }
            return WifiP2pServiceImpl.DBG;
        }

        private void clearSupplicantServiceRequest() {
            if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                this.mWifiNative.p2pServDiscCancelReq(WifiP2pServiceImpl.this.mServiceDiscReqId);
                WifiP2pServiceImpl.this.mServiceDiscReqId = null;
            }
        }

        private boolean addServiceRequest(Messenger m, WifiP2pServiceRequest req) {
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(m, true);
            if (clientInfo == null) {
                return WifiP2pServiceImpl.DBG;
            }
            WifiP2pServiceImpl.access$12704(WifiP2pServiceImpl.this);
            if (WifiP2pServiceImpl.this.mServiceTransactionId == 0) {
                WifiP2pServiceImpl.access$12704(WifiP2pServiceImpl.this);
            }
            req.setTransactionId(WifiP2pServiceImpl.this.mServiceTransactionId);
            clientInfo.mReqList.put(WifiP2pServiceImpl.this.mServiceTransactionId, req);
            if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                return updateSupplicantServiceRequest();
            }
            return true;
        }

        private void removeServiceRequest(Messenger m, WifiP2pServiceRequest req) {
            ClientInfo clientInfo = getClientInfo(m, WifiP2pServiceImpl.DBG);
            if (clientInfo != null) {
                boolean removed = WifiP2pServiceImpl.DBG;
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
                if (!removed) {
                    return;
                }
                if (clientInfo.mReqList.size() == 0 && clientInfo.mServList.size() == 0) {
                    WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
                }
                if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                    updateSupplicantServiceRequest();
                }
            }
        }

        private void clearServiceRequests(Messenger m) {
            ClientInfo clientInfo = getClientInfo(m, WifiP2pServiceImpl.DBG);
            if (clientInfo == null || clientInfo.mReqList.size() == 0) {
                return;
            }
            clientInfo.mReqList.clear();
            if (clientInfo.mServList.size() == 0) {
                WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
            }
            if (WifiP2pServiceImpl.this.mServiceDiscReqId != null) {
                updateSupplicantServiceRequest();
            }
        }

        private boolean addLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
            clearClientDeadChannels();
            ClientInfo clientInfo = getClientInfo(m, true);
            if (clientInfo == null || !clientInfo.mServList.add(servInfo)) {
                return WifiP2pServiceImpl.DBG;
            }
            if (this.mWifiNative.p2pServiceAdd(servInfo)) {
                return true;
            }
            clientInfo.mServList.remove(servInfo);
            return WifiP2pServiceImpl.DBG;
        }

        private void removeLocalService(Messenger m, WifiP2pServiceInfo servInfo) {
            ClientInfo clientInfo = getClientInfo(m, WifiP2pServiceImpl.DBG);
            if (clientInfo != null) {
                this.mWifiNative.p2pServiceDel(servInfo);
                clientInfo.mServList.remove(servInfo);
                if (clientInfo.mReqList.size() != 0 || clientInfo.mServList.size() != 0) {
                    return;
                }
                WifiP2pServiceImpl.this.mClientInfoList.remove(clientInfo.mMessenger);
            }
        }

        private void clearLocalServices(Messenger m) {
            ClientInfo clientInfo = getClientInfo(m, WifiP2pServiceImpl.DBG);
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
                    deadClients.add(c.mMessenger);
                }
            }
            for (Messenger m : deadClients) {
                clearClientInfo(m);
            }
        }

        private ClientInfo getClientInfo(Messenger m, boolean createIfNotExist) {
            ClientInfo clientInfo = (ClientInfo) WifiP2pServiceImpl.this.mClientInfoList.get(m);
            if (clientInfo == null && createIfNotExist) {
                ClientInfo clientInfo2 = new ClientInfo(m);
                WifiP2pServiceImpl.this.mClientInfoList.put(m, clientInfo2);
                return clientInfo2;
            }
            return clientInfo;
        }
    }

    private class ClientInfo {
        private Messenger mMessenger;
        private SparseArray<WifiP2pServiceRequest> mReqList;
        private List<WifiP2pServiceInfo> mServList;

        private ClientInfo(Messenger m) {
            this.mMessenger = m;
            this.mReqList = new SparseArray<>();
            this.mServList = new ArrayList();
        }
    }
}
