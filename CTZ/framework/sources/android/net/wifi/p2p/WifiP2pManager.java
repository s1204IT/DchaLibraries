package android.net.wifi.p2p;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceResponse;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceResponse;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.util.AsyncChannel;
import dalvik.system.CloseGuard;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class WifiP2pManager {
    public static final int ADD_LOCAL_SERVICE = 139292;
    public static final int ADD_LOCAL_SERVICE_FAILED = 139293;
    public static final int ADD_LOCAL_SERVICE_SUCCEEDED = 139294;
    public static final int ADD_PERSISTENT_GROUP = 139361;
    public static final int ADD_PERSISTENT_GROUP_FAILED = 139362;
    public static final int ADD_PERSISTENT_GROUP_SUCCEEDED = 139363;
    public static final int ADD_SERVICE_REQUEST = 139301;
    public static final int ADD_SERVICE_REQUEST_FAILED = 139302;
    public static final int ADD_SERVICE_REQUEST_SUCCEEDED = 139303;
    private static final int BASE = 139264;
    public static final int BUSY = 2;
    public static final String CALLING_PACKAGE = "android.net.wifi.p2p.CALLING_PACKAGE";
    public static final int CANCEL_CONNECT = 139274;
    public static final int CANCEL_CONNECT_FAILED = 139275;
    public static final int CANCEL_CONNECT_SUCCEEDED = 139276;
    public static final int CLEAR_LOCAL_SERVICES = 139298;
    public static final int CLEAR_LOCAL_SERVICES_FAILED = 139299;
    public static final int CLEAR_LOCAL_SERVICES_SUCCEEDED = 139300;
    public static final int CLEAR_SERVICE_REQUESTS = 139307;
    public static final int CLEAR_SERVICE_REQUESTS_FAILED = 139308;
    public static final int CLEAR_SERVICE_REQUESTS_SUCCEEDED = 139309;
    public static final int CONNECT = 139271;
    public static final int CONNECT_FAILED = 139272;
    public static final int CONNECT_SUCCEEDED = 139273;
    public static final int CREATE_GROUP = 139277;
    public static final int CREATE_GROUP_FAILED = 139278;
    public static final int CREATE_GROUP_SUCCEEDED = 139279;
    public static final int DELETE_PERSISTENT_GROUP = 139318;
    public static final int DELETE_PERSISTENT_GROUP_FAILED = 139319;
    public static final int DELETE_PERSISTENT_GROUP_SUCCEEDED = 139320;
    public static final int DISCOVER_PEERS = 139265;
    public static final int DISCOVER_PEERS_FAILED = 139266;
    public static final int DISCOVER_PEERS_SUCCEEDED = 139267;
    public static final int DISCOVER_SERVICES = 139310;
    public static final int DISCOVER_SERVICES_FAILED = 139311;
    public static final int DISCOVER_SERVICES_SUCCEEDED = 139312;
    public static final int ERROR = 0;
    public static final String EXTRA_CLIENT_MESSAGE = "android.net.wifi.p2p.EXTRA_CLIENT_MESSAGE";
    public static final String EXTRA_DISCOVERY_STATE = "discoveryState";
    public static final String EXTRA_HANDOVER_MESSAGE = "android.net.wifi.p2p.EXTRA_HANDOVER_MESSAGE";
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_P2P_DEVICE_LIST = "wifiP2pDeviceList";
    public static final String EXTRA_PIN_CODE = "android.net.wifi.p2p.EXTRA_PIN_CODE";
    public static final String EXTRA_PIN_METHOD = "android.net.wifi.p2p.EXTRA_PIN_METHOD";
    public static final String EXTRA_WIFI_P2P_DEVICE = "wifiP2pDevice";
    public static final String EXTRA_WIFI_P2P_GROUP = "p2pGroupInfo";
    public static final String EXTRA_WIFI_P2P_INFO = "wifiP2pInfo";
    public static final String EXTRA_WIFI_STATE = "wifi_p2p_state";
    public static final int FREQ_CONFLICT_EX_RESULT = 139356;
    public static final int GET_HANDOVER_REQUEST = 139339;
    public static final int GET_HANDOVER_SELECT = 139340;
    public static final int INITIATOR_REPORT_NFC_HANDOVER = 139342;
    public static final int MIRACAST_DISABLED = 0;
    public static final int MIRACAST_SINK = 2;
    public static final int MIRACAST_SOURCE = 1;
    public static final int NO_SERVICE_REQUESTS = 3;
    public static final int P2P_UNSUPPORTED = 1;
    public static final int PEER_CONNECTION_USER_ACCEPT_FROM_OUTER = 139354;
    public static final int PEER_CONNECTION_USER_REJECT_FROM_OUTER = 139355;
    public static final int PING = 139313;
    public static final int REMOVE_CLIENT = 139357;
    public static final int REMOVE_CLIENT_FAILED = 139358;
    public static final int REMOVE_CLIENT_SUCCEEDED = 139359;
    public static final int REMOVE_GROUP = 139280;
    public static final int REMOVE_GROUP_FAILED = 139281;
    public static final int REMOVE_GROUP_SUCCEEDED = 139282;
    public static final int REMOVE_LOCAL_SERVICE = 139295;
    public static final int REMOVE_LOCAL_SERVICE_FAILED = 139296;
    public static final int REMOVE_LOCAL_SERVICE_SUCCEEDED = 139297;
    public static final int REMOVE_SERVICE_REQUEST = 139304;
    public static final int REMOVE_SERVICE_REQUEST_FAILED = 139305;
    public static final int REMOVE_SERVICE_REQUEST_SUCCEEDED = 139306;
    public static final int REPORT_NFC_HANDOVER_FAILED = 139345;
    public static final int REPORT_NFC_HANDOVER_SUCCEEDED = 139344;
    public static final int REQUEST_CONNECTION_INFO = 139285;
    public static final int REQUEST_GROUP_INFO = 139287;
    public static final int REQUEST_LINK_INFO = 139349;
    public static final int REQUEST_PEERS = 139283;
    public static final int REQUEST_PERSISTENT_GROUP_INFO = 139321;
    public static final int RESPONDER_REPORT_NFC_HANDOVER = 139343;
    public static final int RESPONSE_ADD_PERSISTENT_GROUP = 139364;
    public static final int RESPONSE_CONNECTION_INFO = 139286;
    public static final int RESPONSE_GET_HANDOVER_MESSAGE = 139341;
    public static final int RESPONSE_GROUP_INFO = 139288;
    public static final int RESPONSE_LINK_INFO = 139350;
    public static final int RESPONSE_PEERS = 139284;
    public static final int RESPONSE_PERSISTENT_GROUP_INFO = 139322;
    public static final int RESPONSE_SERVICE = 139314;
    public static final int SET_AUTO_CHANNEL_SELECT = 139351;
    public static final int SET_AUTO_CHANNEL_SELECT_FAILED = 139352;
    public static final int SET_AUTO_CHANNEL_SELECT_SUCCEEDED = 139353;
    public static final int SET_CHANNEL = 139335;
    public static final int SET_CHANNEL_FAILED = 139336;
    public static final int SET_CHANNEL_SUCCEEDED = 139337;
    public static final int SET_DEVICE_NAME = 139315;
    public static final int SET_DEVICE_NAME_FAILED = 139316;
    public static final int SET_DEVICE_NAME_SUCCEEDED = 139317;
    public static final int SET_WFD_INFO = 139323;
    public static final int SET_WFD_INFO_FAILED = 139324;
    public static final int SET_WFD_INFO_SUCCEEDED = 139325;
    public static final int START_LISTEN = 139329;
    public static final int START_LISTEN_FAILED = 139330;
    public static final int START_LISTEN_SUCCEEDED = 139331;
    public static final int START_WPS = 139326;
    public static final int START_WPS_FAILED = 139327;
    public static final int START_WPS_SUCCEEDED = 139328;
    public static final int STOP_DISCOVERY = 139268;
    public static final int STOP_DISCOVERY_FAILED = 139269;
    public static final int STOP_DISCOVERY_SUCCEEDED = 139270;
    public static final int STOP_LISTEN = 139332;
    public static final int STOP_LISTEN_FAILED = 139333;
    public static final int STOP_LISTEN_SUCCEEDED = 139334;
    private static final String TAG = "WifiP2pManager";
    public static final String WIFI_P2P_CONNECTION_CHANGED_ACTION = "android.net.wifi.p2p.CONNECTION_STATE_CHANGE";
    public static final String WIFI_P2P_DISCOVERY_CHANGED_ACTION = "android.net.wifi.p2p.DISCOVERY_STATE_CHANGE";
    public static final int WIFI_P2P_DISCOVERY_STARTED = 2;
    public static final int WIFI_P2P_DISCOVERY_STOPPED = 1;
    public static final String WIFI_P2P_PEERS_CHANGED_ACTION = "android.net.wifi.p2p.PEERS_CHANGED";
    public static final String WIFI_P2P_PERSISTENT_GROUPS_CHANGED_ACTION = "android.net.wifi.p2p.PERSISTENT_GROUPS_CHANGED";
    public static final String WIFI_P2P_STATE_CHANGED_ACTION = "android.net.wifi.p2p.STATE_CHANGED";
    public static final int WIFI_P2P_STATE_DISABLED = 1;
    public static final int WIFI_P2P_STATE_ENABLED = 2;
    public static final String WIFI_P2P_THIS_DEVICE_CHANGED_ACTION = "android.net.wifi.p2p.THIS_DEVICE_CHANGED";
    private static final Pattern macPattern = Pattern.compile("((?:[0-9a-f]{2}:){5}[0-9a-f]{2})");
    IWifiP2pManager mService;

    public interface ActionListener {
        void onFailure(int i);

        void onSuccess();
    }

    public interface AddPersistentGroupListener {
        void onAddPersistentGroupAdded(WifiP2pGroup wifiP2pGroup);
    }

    public interface ChannelListener {
        void onChannelDisconnected();
    }

    public interface ConnectionInfoListener {
        void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo);
    }

    public interface DnsSdServiceResponseListener {
        void onDnsSdServiceAvailable(String str, String str2, WifiP2pDevice wifiP2pDevice);
    }

    public interface DnsSdTxtRecordListener {
        void onDnsSdTxtRecordAvailable(String str, Map<String, String> map, WifiP2pDevice wifiP2pDevice);
    }

    public interface GroupInfoListener {
        void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup);
    }

    public interface HandoverMessageListener {
        void onHandoverMessageAvailable(String str);
    }

    public interface PeerListListener {
        void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList);
    }

    public interface PersistentGroupInfoListener {
        void onPersistentGroupInfoAvailable(WifiP2pGroupList wifiP2pGroupList);
    }

    public interface ServiceResponseListener {
        void onServiceAvailable(int i, byte[] bArr, WifiP2pDevice wifiP2pDevice);
    }

    public interface UpnpServiceResponseListener {
        void onUpnpServiceAvailable(List<String> list, WifiP2pDevice wifiP2pDevice);
    }

    public WifiP2pManager(IWifiP2pManager iWifiP2pManager) {
        this.mService = iWifiP2pManager;
    }

    public static class Channel implements AutoCloseable {
        private static final int INVALID_LISTENER_KEY = 0;
        final Binder mBinder;
        private ChannelListener mChannelListener;
        Context mContext;
        private DnsSdServiceResponseListener mDnsSdServRspListener;
        private DnsSdTxtRecordListener mDnsSdTxtListener;
        private P2pHandler mHandler;
        private final WifiP2pManager mP2pManager;
        private ServiceResponseListener mServRspListener;
        private UpnpServiceResponseListener mUpnpServRspListener;
        private HashMap<Integer, Object> mListenerMap = new HashMap<>();
        private final Object mListenerMapLock = new Object();
        private int mListenerKey = 0;
        private final CloseGuard mCloseGuard = CloseGuard.get();
        private AsyncChannel mAsyncChannel = new AsyncChannel();

        public Channel(Context context, Looper looper, ChannelListener channelListener, Binder binder, WifiP2pManager wifiP2pManager) {
            this.mHandler = new P2pHandler(looper);
            this.mChannelListener = channelListener;
            this.mContext = context;
            this.mBinder = binder;
            this.mP2pManager = wifiP2pManager;
            this.mCloseGuard.open("close");
        }

        @Override
        public void close() {
            Log.d(WifiP2pManager.TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
            new Throwable().printStackTrace();
            if (this.mP2pManager == null) {
                Log.w(WifiP2pManager.TAG, "Channel.close(): Null mP2pManager!?");
            } else {
                try {
                    this.mP2pManager.mService.close(this.mBinder);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            this.mAsyncChannel.disconnect();
            this.mCloseGuard.close();
        }

        protected void finalize() throws Throwable {
            try {
                if (this.mCloseGuard != null) {
                    this.mCloseGuard.warnIfOpen();
                }
                close();
            } finally {
                super.finalize();
            }
        }

        class P2pHandler extends Handler {
            P2pHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                Object listener = Channel.this.getListener(message.arg2);
                switch (message.what) {
                    case AsyncChannel.CMD_CHANNEL_DISCONNECTED:
                        if (Channel.this.mChannelListener != null) {
                            Channel.this.mChannelListener.onChannelDisconnected();
                            Channel.this.mChannelListener = null;
                        }
                        break;
                    case WifiP2pManager.DISCOVER_PEERS_FAILED:
                    case WifiP2pManager.STOP_DISCOVERY_FAILED:
                    case WifiP2pManager.CONNECT_FAILED:
                    case WifiP2pManager.CANCEL_CONNECT_FAILED:
                    case WifiP2pManager.CREATE_GROUP_FAILED:
                    case WifiP2pManager.REMOVE_GROUP_FAILED:
                    case WifiP2pManager.ADD_LOCAL_SERVICE_FAILED:
                    case WifiP2pManager.REMOVE_LOCAL_SERVICE_FAILED:
                    case WifiP2pManager.CLEAR_LOCAL_SERVICES_FAILED:
                    case WifiP2pManager.ADD_SERVICE_REQUEST_FAILED:
                    case WifiP2pManager.REMOVE_SERVICE_REQUEST_FAILED:
                    case WifiP2pManager.CLEAR_SERVICE_REQUESTS_FAILED:
                    case WifiP2pManager.DISCOVER_SERVICES_FAILED:
                    case WifiP2pManager.SET_DEVICE_NAME_FAILED:
                    case WifiP2pManager.DELETE_PERSISTENT_GROUP_FAILED:
                    case WifiP2pManager.SET_WFD_INFO_FAILED:
                    case WifiP2pManager.START_WPS_FAILED:
                    case WifiP2pManager.START_LISTEN_FAILED:
                    case WifiP2pManager.STOP_LISTEN_FAILED:
                    case WifiP2pManager.SET_CHANNEL_FAILED:
                    case WifiP2pManager.REPORT_NFC_HANDOVER_FAILED:
                    case WifiP2pManager.REMOVE_CLIENT_FAILED:
                    case WifiP2pManager.ADD_PERSISTENT_GROUP_FAILED:
                        if (listener != null) {
                            ((ActionListener) listener).onFailure(message.arg1);
                        }
                        break;
                    case WifiP2pManager.DISCOVER_PEERS_SUCCEEDED:
                    case WifiP2pManager.STOP_DISCOVERY_SUCCEEDED:
                    case WifiP2pManager.CONNECT_SUCCEEDED:
                    case WifiP2pManager.CANCEL_CONNECT_SUCCEEDED:
                    case WifiP2pManager.CREATE_GROUP_SUCCEEDED:
                    case WifiP2pManager.REMOVE_GROUP_SUCCEEDED:
                    case WifiP2pManager.ADD_LOCAL_SERVICE_SUCCEEDED:
                    case WifiP2pManager.REMOVE_LOCAL_SERVICE_SUCCEEDED:
                    case WifiP2pManager.CLEAR_LOCAL_SERVICES_SUCCEEDED:
                    case WifiP2pManager.ADD_SERVICE_REQUEST_SUCCEEDED:
                    case WifiP2pManager.REMOVE_SERVICE_REQUEST_SUCCEEDED:
                    case WifiP2pManager.CLEAR_SERVICE_REQUESTS_SUCCEEDED:
                    case WifiP2pManager.DISCOVER_SERVICES_SUCCEEDED:
                    case WifiP2pManager.SET_DEVICE_NAME_SUCCEEDED:
                    case WifiP2pManager.DELETE_PERSISTENT_GROUP_SUCCEEDED:
                    case WifiP2pManager.SET_WFD_INFO_SUCCEEDED:
                    case WifiP2pManager.START_WPS_SUCCEEDED:
                    case WifiP2pManager.START_LISTEN_SUCCEEDED:
                    case WifiP2pManager.STOP_LISTEN_SUCCEEDED:
                    case WifiP2pManager.SET_CHANNEL_SUCCEEDED:
                    case WifiP2pManager.REPORT_NFC_HANDOVER_SUCCEEDED:
                    case WifiP2pManager.REMOVE_CLIENT_SUCCEEDED:
                        if (listener != null) {
                            ((ActionListener) listener).onSuccess();
                        }
                        break;
                    case WifiP2pManager.RESPONSE_PEERS:
                        WifiP2pDeviceList wifiP2pDeviceList = (WifiP2pDeviceList) message.obj;
                        if (listener != null) {
                            ((PeerListListener) listener).onPeersAvailable(wifiP2pDeviceList);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_CONNECTION_INFO:
                        WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) message.obj;
                        if (listener != null) {
                            ((ConnectionInfoListener) listener).onConnectionInfoAvailable(wifiP2pInfo);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_GROUP_INFO:
                        WifiP2pGroup wifiP2pGroup = (WifiP2pGroup) message.obj;
                        if (listener != null) {
                            ((GroupInfoListener) listener).onGroupInfoAvailable(wifiP2pGroup);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_SERVICE:
                        Channel.this.handleServiceResponse((WifiP2pServiceResponse) message.obj);
                        break;
                    case WifiP2pManager.RESPONSE_PERSISTENT_GROUP_INFO:
                        WifiP2pGroupList wifiP2pGroupList = (WifiP2pGroupList) message.obj;
                        if (listener != null) {
                            ((PersistentGroupInfoListener) listener).onPersistentGroupInfoAvailable(wifiP2pGroupList);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_GET_HANDOVER_MESSAGE:
                        Bundle bundle = (Bundle) message.obj;
                        if (listener != null) {
                            ((HandoverMessageListener) listener).onHandoverMessageAvailable(bundle != null ? bundle.getString(WifiP2pManager.EXTRA_HANDOVER_MESSAGE) : null);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_ADD_PERSISTENT_GROUP:
                        WifiP2pGroup wifiP2pGroup2 = (WifiP2pGroup) message.obj;
                        if (listener != null) {
                            ((AddPersistentGroupListener) listener).onAddPersistentGroupAdded(wifiP2pGroup2);
                        }
                        break;
                    default:
                        Log.d(WifiP2pManager.TAG, "Ignored " + message);
                        break;
                }
            }
        }

        private void handleServiceResponse(WifiP2pServiceResponse wifiP2pServiceResponse) {
            if (wifiP2pServiceResponse instanceof WifiP2pDnsSdServiceResponse) {
                handleDnsSdServiceResponse((WifiP2pDnsSdServiceResponse) wifiP2pServiceResponse);
                return;
            }
            if (wifiP2pServiceResponse instanceof WifiP2pUpnpServiceResponse) {
                if (this.mUpnpServRspListener != null) {
                    handleUpnpServiceResponse((WifiP2pUpnpServiceResponse) wifiP2pServiceResponse);
                }
            } else if (this.mServRspListener != null) {
                this.mServRspListener.onServiceAvailable(wifiP2pServiceResponse.getServiceType(), wifiP2pServiceResponse.getRawData(), wifiP2pServiceResponse.getSrcDevice());
            }
        }

        private void handleUpnpServiceResponse(WifiP2pUpnpServiceResponse wifiP2pUpnpServiceResponse) {
            this.mUpnpServRspListener.onUpnpServiceAvailable(wifiP2pUpnpServiceResponse.getUniqueServiceNames(), wifiP2pUpnpServiceResponse.getSrcDevice());
        }

        private void handleDnsSdServiceResponse(WifiP2pDnsSdServiceResponse wifiP2pDnsSdServiceResponse) {
            if (wifiP2pDnsSdServiceResponse.getDnsType() == 12) {
                if (this.mDnsSdServRspListener != null) {
                    this.mDnsSdServRspListener.onDnsSdServiceAvailable(wifiP2pDnsSdServiceResponse.getInstanceName(), wifiP2pDnsSdServiceResponse.getDnsQueryName(), wifiP2pDnsSdServiceResponse.getSrcDevice());
                }
            } else if (wifiP2pDnsSdServiceResponse.getDnsType() == 16) {
                if (this.mDnsSdTxtListener != null) {
                    this.mDnsSdTxtListener.onDnsSdTxtRecordAvailable(wifiP2pDnsSdServiceResponse.getDnsQueryName(), wifiP2pDnsSdServiceResponse.getTxtRecord(), wifiP2pDnsSdServiceResponse.getSrcDevice());
                }
            } else {
                Log.e(WifiP2pManager.TAG, "Unhandled resp " + wifiP2pDnsSdServiceResponse);
            }
        }

        private int putListener(Object obj) {
            int i;
            if (obj == null) {
                return 0;
            }
            synchronized (this.mListenerMapLock) {
                do {
                    i = this.mListenerKey;
                    this.mListenerKey = i + 1;
                } while (i == 0);
                this.mListenerMap.put(Integer.valueOf(i), obj);
            }
            return i;
        }

        private Object getListener(int i) {
            Object objRemove;
            if (i == 0) {
                return null;
            }
            synchronized (this.mListenerMapLock) {
                objRemove = this.mListenerMap.remove(Integer.valueOf(i));
            }
            return objRemove;
        }

        private void clearListener() {
            synchronized (this.mListenerMapLock) {
                this.mListenerMap.clear();
            }
        }
    }

    private static void checkChannel(Channel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("Channel needs to be initialized");
        }
    }

    private static void checkServiceInfo(WifiP2pServiceInfo wifiP2pServiceInfo) {
        if (wifiP2pServiceInfo == null) {
            throw new IllegalArgumentException("service info is null");
        }
    }

    private static void checkServiceRequest(WifiP2pServiceRequest wifiP2pServiceRequest) {
        if (wifiP2pServiceRequest == null) {
            throw new IllegalArgumentException("service request is null");
        }
    }

    private static void checkP2pConfig(WifiP2pConfig wifiP2pConfig) {
        if (wifiP2pConfig == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (TextUtils.isEmpty(wifiP2pConfig.deviceAddress)) {
            throw new IllegalArgumentException("deviceAddress cannot be empty");
        }
    }

    private void checkMac(String str) {
        if (!macPattern.matcher(str).find()) {
            throw new IllegalArgumentException("MAC needs to be well-formed");
        }
    }

    public Channel initialize(Context context, Looper looper, ChannelListener channelListener) {
        Binder binder = new Binder();
        return initalizeChannel(context, looper, channelListener, getMessenger(binder), binder);
    }

    public Channel initializeInternal(Context context, Looper looper, ChannelListener channelListener) {
        return initalizeChannel(context, looper, channelListener, getP2pStateMachineMessenger(), null);
    }

    private Channel initalizeChannel(Context context, Looper looper, ChannelListener channelListener, Messenger messenger, Binder binder) {
        if (messenger == null) {
            return null;
        }
        Channel channel = new Channel(context, looper, channelListener, binder, this);
        if (channel.mAsyncChannel.connectSync(context, channel.mHandler, messenger) == 0) {
            return channel;
        }
        channel.close();
        return null;
    }

    public void discoverPeers(Channel channel, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(DISCOVER_PEERS, 0, channel.putListener(actionListener));
    }

    public void stopPeerDiscovery(Channel channel, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(STOP_DISCOVERY, 0, channel.putListener(actionListener));
    }

    public void connect(Channel channel, WifiP2pConfig wifiP2pConfig, ActionListener actionListener) {
        checkChannel(channel);
        checkP2pConfig(wifiP2pConfig);
        channel.mAsyncChannel.sendMessage(CONNECT, 0, channel.putListener(actionListener), wifiP2pConfig);
    }

    public void cancelConnect(Channel channel, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(CANCEL_CONNECT, 0, channel.putListener(actionListener));
    }

    public void createGroup(Channel channel, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(CREATE_GROUP, -2, channel.putListener(actionListener));
    }

    public void removeGroup(Channel channel, ActionListener actionListener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(REMOVE_GROUP, 0, channel.putListener(actionListener));
    }

    public void listen(Channel channel, boolean z, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(z ? START_LISTEN : STOP_LISTEN, 0, channel.putListener(actionListener));
    }

    public void setWifiP2pChannels(Channel channel, int i, int i2, ActionListener actionListener) {
        checkChannel(channel);
        Bundle bundle = new Bundle();
        bundle.putInt("lc", i);
        bundle.putInt("oc", i2);
        channel.mAsyncChannel.sendMessage(SET_CHANNEL, 0, channel.putListener(actionListener), bundle);
    }

    public void startWps(Channel channel, WpsInfo wpsInfo, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(START_WPS, 0, channel.putListener(actionListener), wpsInfo);
    }

    public void addLocalService(Channel channel, WifiP2pServiceInfo wifiP2pServiceInfo, ActionListener actionListener) {
        checkChannel(channel);
        checkServiceInfo(wifiP2pServiceInfo);
        channel.mAsyncChannel.sendMessage(ADD_LOCAL_SERVICE, 0, channel.putListener(actionListener), wifiP2pServiceInfo);
    }

    public void removeLocalService(Channel channel, WifiP2pServiceInfo wifiP2pServiceInfo, ActionListener actionListener) {
        checkChannel(channel);
        checkServiceInfo(wifiP2pServiceInfo);
        channel.mAsyncChannel.sendMessage(REMOVE_LOCAL_SERVICE, 0, channel.putListener(actionListener), wifiP2pServiceInfo);
    }

    public void clearLocalServices(Channel channel, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(CLEAR_LOCAL_SERVICES, 0, channel.putListener(actionListener));
    }

    public void setServiceResponseListener(Channel channel, ServiceResponseListener serviceResponseListener) {
        checkChannel(channel);
        channel.mServRspListener = serviceResponseListener;
    }

    public void setDnsSdResponseListeners(Channel channel, DnsSdServiceResponseListener dnsSdServiceResponseListener, DnsSdTxtRecordListener dnsSdTxtRecordListener) {
        checkChannel(channel);
        channel.mDnsSdServRspListener = dnsSdServiceResponseListener;
        channel.mDnsSdTxtListener = dnsSdTxtRecordListener;
    }

    public void setUpnpServiceResponseListener(Channel channel, UpnpServiceResponseListener upnpServiceResponseListener) {
        checkChannel(channel);
        channel.mUpnpServRspListener = upnpServiceResponseListener;
    }

    public void discoverServices(Channel channel, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(DISCOVER_SERVICES, 0, channel.putListener(actionListener));
    }

    public void addServiceRequest(Channel channel, WifiP2pServiceRequest wifiP2pServiceRequest, ActionListener actionListener) {
        checkChannel(channel);
        checkServiceRequest(wifiP2pServiceRequest);
        channel.mAsyncChannel.sendMessage(ADD_SERVICE_REQUEST, 0, channel.putListener(actionListener), wifiP2pServiceRequest);
    }

    public void removeServiceRequest(Channel channel, WifiP2pServiceRequest wifiP2pServiceRequest, ActionListener actionListener) {
        checkChannel(channel);
        checkServiceRequest(wifiP2pServiceRequest);
        channel.mAsyncChannel.sendMessage(REMOVE_SERVICE_REQUEST, 0, channel.putListener(actionListener), wifiP2pServiceRequest);
    }

    public void clearServiceRequests(Channel channel, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(CLEAR_SERVICE_REQUESTS, 0, channel.putListener(actionListener));
    }

    public void requestPeers(Channel channel, PeerListListener peerListListener) {
        checkChannel(channel);
        Bundle bundle = new Bundle();
        bundle.putString(CALLING_PACKAGE, channel.mContext.getOpPackageName());
        channel.mAsyncChannel.sendMessage(REQUEST_PEERS, 0, channel.putListener(peerListListener), bundle);
    }

    public void requestConnectionInfo(Channel channel, ConnectionInfoListener connectionInfoListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(REQUEST_CONNECTION_INFO, 0, channel.putListener(connectionInfoListener));
    }

    public void requestGroupInfo(Channel channel, GroupInfoListener groupInfoListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(REQUEST_GROUP_INFO, 0, channel.putListener(groupInfoListener));
    }

    public void setDeviceName(Channel channel, String str, ActionListener actionListener) {
        checkChannel(channel);
        WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
        wifiP2pDevice.deviceName = str;
        channel.mAsyncChannel.sendMessage(SET_DEVICE_NAME, 0, channel.putListener(actionListener), wifiP2pDevice);
    }

    public void setWFDInfo(Channel channel, WifiP2pWfdInfo wifiP2pWfdInfo, ActionListener actionListener) {
        checkChannel(channel);
        try {
            this.mService.checkConfigureWifiDisplayPermission();
        } catch (RemoteException e) {
            e.rethrowFromSystemServer();
        }
        channel.mAsyncChannel.sendMessage(SET_WFD_INFO, 0, channel.putListener(actionListener), wifiP2pWfdInfo);
    }

    public void deletePersistentGroup(Channel channel, int i, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(DELETE_PERSISTENT_GROUP, i, channel.putListener(actionListener));
    }

    public void requestPersistentGroupInfo(Channel channel, PersistentGroupInfoListener persistentGroupInfoListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(REQUEST_PERSISTENT_GROUP_INFO, 0, channel.putListener(persistentGroupInfoListener));
    }

    public void setMiracastMode(int i) {
        try {
            this.mService.setMiracastMode(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Messenger getMessenger(Binder binder) {
        try {
            return this.mService.getMessenger(binder);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Messenger getP2pStateMachineMessenger() {
        try {
            return this.mService.getP2pStateMachineMessenger();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void getNfcHandoverRequest(Channel channel, HandoverMessageListener handoverMessageListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(GET_HANDOVER_REQUEST, 0, channel.putListener(handoverMessageListener));
    }

    public void getNfcHandoverSelect(Channel channel, HandoverMessageListener handoverMessageListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(GET_HANDOVER_SELECT, 0, channel.putListener(handoverMessageListener));
    }

    public void initiatorReportNfcHandover(Channel channel, String str, ActionListener actionListener) {
        checkChannel(channel);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_HANDOVER_MESSAGE, str);
        channel.mAsyncChannel.sendMessage(INITIATOR_REPORT_NFC_HANDOVER, 0, channel.putListener(actionListener), bundle);
    }

    public void responderReportNfcHandover(Channel channel, String str, ActionListener actionListener) {
        checkChannel(channel);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_HANDOVER_MESSAGE, str);
        channel.mAsyncChannel.sendMessage(RESPONDER_REPORT_NFC_HANDOVER, 0, channel.putListener(actionListener), bundle);
    }

    public String getMacAddress() {
        try {
            return this.mService.getMacAddress();
        } catch (RemoteException e) {
            return null;
        }
    }

    public void deinitialize(Channel channel) {
        Log.i(TAG, "deinitialize()");
        checkChannel(channel);
        channel.clearListener();
    }

    public String getPeerIpAddress(String str) {
        try {
            return this.mService.getPeerIpAddress(str);
        } catch (RemoteException e) {
            return null;
        }
    }

    public void setGCInviteResult(Channel channel, boolean z, int i, ActionListener actionListener) {
        checkChannel(channel);
        if (true == z) {
            channel.mAsyncChannel.sendMessage(PEER_CONNECTION_USER_ACCEPT_FROM_OUTER, i, channel.putListener(actionListener));
        } else {
            channel.mAsyncChannel.sendMessage(PEER_CONNECTION_USER_REJECT_FROM_OUTER, -1, channel.putListener(actionListener));
        }
    }

    public void setGCInviteResult(Channel channel, boolean z, int i, int i2, String str, ActionListener actionListener) {
        checkChannel(channel);
        if (str == null) {
            throw new IllegalArgumentException("pinCode needs to be configured");
        }
        if (i2 != 2 && i2 != 1) {
            throw new IllegalArgumentException("pinMethod needs to be WpsInfo.KEYPAD/WpsInfo.DISPLAY");
        }
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_PIN_CODE, str);
        bundle.putInt(EXTRA_PIN_METHOD, i2);
        if (true == z) {
            channel.mAsyncChannel.sendMessage(PEER_CONNECTION_USER_ACCEPT_FROM_OUTER, i, channel.putListener(actionListener), bundle);
        } else {
            channel.mAsyncChannel.sendMessage(PEER_CONNECTION_USER_REJECT_FROM_OUTER, -1, channel.putListener(actionListener), bundle);
        }
    }

    public void setFreqConflictExResult(Channel channel, boolean z, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(FREQ_CONFLICT_EX_RESULT, z ? 1 : 0, channel.putListener(actionListener));
    }

    public void removeClient(Channel channel, String str, ActionListener actionListener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(channel);
        checkMac(str);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_CLIENT_MESSAGE, str);
        channel.mAsyncChannel.sendMessage(REMOVE_CLIENT, 0, channel.putListener(actionListener), bundle);
    }

    public void createGroup(Channel channel, int i, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(CREATE_GROUP, i, channel.putListener(actionListener));
    }

    public void discoverPeers(Channel channel, int i, ActionListener actionListener) {
        checkChannel(channel);
        channel.mAsyncChannel.sendMessage(DISCOVER_PEERS, i, channel.putListener(actionListener));
    }

    public void addPersistentGroup(Channel channel, Map<String, String> map, AddPersistentGroupListener addPersistentGroupListener) {
        checkChannel(channel);
        Bundle bundle = new Bundle();
        HashMap map2 = new HashMap();
        if (map != null && (map instanceof HashMap)) {
            map2 = (HashMap) map;
        } else if (map != null) {
            map2.putAll(map);
        }
        bundle.putSerializable("variables", map2);
        channel.mAsyncChannel.sendMessage(ADD_PERSISTENT_GROUP, 0, channel.putListener(addPersistentGroupListener), bundle);
    }
}
