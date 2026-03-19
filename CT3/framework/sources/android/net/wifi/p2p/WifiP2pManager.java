package android.net.wifi.p2p;

import android.content.Context;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.link.WifiP2pLinkInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceResponse;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.net.wifi.p2p.nsd.WifiP2pUpnpServiceResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
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
    public static final int BEAM_DISCOVERY_TIMEOUT = 123;
    public static final int BEAM_GO_MODE_DISABLE = 3;
    public static final int BEAM_GO_MODE_ENABLE = 1;
    public static final int BEAM_MODE_DISABLE = 2;
    public static final int BEAM_MODE_ENABLE = 0;
    public static final int BUSY = 2;
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
    public static final int STOP_P2P_FIND_ONLY = 139360;
    private static final String TAG = "WifiP2pManager";
    public static final int UPPER_BOUND = 139392;
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

    public interface WifiP2pLinkInfoListener {
        void onLinkInfoAvailable(WifiP2pLinkInfo wifiP2pLinkInfo);
    }

    public WifiP2pManager(IWifiP2pManager service) {
        this.mService = service;
    }

    public static class Channel {
        private static final int INVALID_LISTENER_KEY = 0;
        private ChannelListener mChannelListener;
        Context mContext;
        private DnsSdServiceResponseListener mDnsSdServRspListener;
        private DnsSdTxtRecordListener mDnsSdTxtListener;
        private P2pHandler mHandler;
        private ServiceResponseListener mServRspListener;
        private UpnpServiceResponseListener mUpnpServRspListener;
        private HashMap<Integer, Object> mListenerMap = new HashMap<>();
        private Object mListenerMapLock = new Object();
        private int mListenerKey = 0;
        private AsyncChannel mAsyncChannel = new AsyncChannel();

        Channel(Context context, Looper looper, ChannelListener l) {
            this.mHandler = new P2pHandler(looper);
            this.mChannelListener = l;
            this.mContext = context;
        }

        class P2pHandler extends Handler {
            P2pHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message message) {
                String string;
                Object listener = Channel.this.getListener(message.arg2);
                switch (message.what) {
                    case 69636:
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
                        WifiP2pDeviceList peers = (WifiP2pDeviceList) message.obj;
                        if (listener != null) {
                            ((PeerListListener) listener).onPeersAvailable(peers);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_CONNECTION_INFO:
                        WifiP2pInfo wifiP2pInfo = (WifiP2pInfo) message.obj;
                        if (listener != null) {
                            ((ConnectionInfoListener) listener).onConnectionInfoAvailable(wifiP2pInfo);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_GROUP_INFO:
                        WifiP2pGroup group = (WifiP2pGroup) message.obj;
                        if (listener != null) {
                            ((GroupInfoListener) listener).onGroupInfoAvailable(group);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_SERVICE:
                        WifiP2pServiceResponse resp = (WifiP2pServiceResponse) message.obj;
                        Channel.this.handleServiceResponse(resp);
                        break;
                    case WifiP2pManager.RESPONSE_PERSISTENT_GROUP_INFO:
                        WifiP2pGroupList groups = (WifiP2pGroupList) message.obj;
                        if (listener != null) {
                            ((PersistentGroupInfoListener) listener).onPersistentGroupInfoAvailable(groups);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_GET_HANDOVER_MESSAGE:
                        Bundle handoverBundle = (Bundle) message.obj;
                        if (listener != null) {
                            if (handoverBundle != null) {
                                string = handoverBundle.getString(WifiP2pManager.EXTRA_HANDOVER_MESSAGE);
                            } else {
                                string = null;
                            }
                            ((HandoverMessageListener) listener).onHandoverMessageAvailable(string);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_LINK_INFO:
                        WifiP2pLinkInfo s = (WifiP2pLinkInfo) message.obj;
                        if (listener != null) {
                            ((WifiP2pLinkInfoListener) listener).onLinkInfoAvailable(s);
                        }
                        break;
                    case WifiP2pManager.RESPONSE_ADD_PERSISTENT_GROUP:
                        WifiP2pGroup group2 = (WifiP2pGroup) message.obj;
                        if (listener != null) {
                            ((AddPersistentGroupListener) listener).onAddPersistentGroupAdded(group2);
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
                handleDnsSdServiceResponse(wifiP2pServiceResponse);
                return;
            }
            if (wifiP2pServiceResponse instanceof WifiP2pUpnpServiceResponse) {
                if (this.mUpnpServRspListener != null) {
                    handleUpnpServiceResponse(wifiP2pServiceResponse);
                }
            } else {
                if (this.mServRspListener == null) {
                    return;
                }
                this.mServRspListener.onServiceAvailable(wifiP2pServiceResponse.getServiceType(), wifiP2pServiceResponse.getRawData(), wifiP2pServiceResponse.getSrcDevice());
            }
        }

        private void handleUpnpServiceResponse(WifiP2pUpnpServiceResponse resp) {
            this.mUpnpServRspListener.onUpnpServiceAvailable(resp.getUniqueServiceNames(), resp.getSrcDevice());
        }

        private void handleDnsSdServiceResponse(WifiP2pDnsSdServiceResponse resp) {
            if (resp.getDnsType() == 12) {
                if (this.mDnsSdServRspListener == null) {
                    return;
                }
                this.mDnsSdServRspListener.onDnsSdServiceAvailable(resp.getInstanceName(), resp.getDnsQueryName(), resp.getSrcDevice());
            } else {
                if (resp.getDnsType() == 16) {
                    if (this.mDnsSdTxtListener == null) {
                        return;
                    }
                    this.mDnsSdTxtListener.onDnsSdTxtRecordAvailable(resp.getDnsQueryName(), resp.getTxtRecord(), resp.getSrcDevice());
                    return;
                }
                Log.e(WifiP2pManager.TAG, "Unhandled resp " + resp);
            }
        }

        private int putListener(Object listener) {
            int key;
            if (listener == null) {
                return 0;
            }
            synchronized (this.mListenerMapLock) {
                do {
                    key = this.mListenerKey;
                    this.mListenerKey = key + 1;
                } while (key == 0);
                this.mListenerMap.put(Integer.valueOf(key), listener);
            }
            return key;
        }

        private Object getListener(int key) {
            Object objRemove;
            if (key == 0) {
                return null;
            }
            synchronized (this.mListenerMapLock) {
                objRemove = this.mListenerMap.remove(Integer.valueOf(key));
            }
            return objRemove;
        }

        private void clearListener() {
            synchronized (this.mListenerMapLock) {
                this.mListenerMap.clear();
            }
        }
    }

    private static void checkChannel(Channel c) {
        if (c == null) {
            throw new IllegalArgumentException("Channel needs to be initialized");
        }
    }

    private static void checkServiceInfo(WifiP2pServiceInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("service info is null");
        }
    }

    private static void checkServiceRequest(WifiP2pServiceRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("service request is null");
        }
    }

    private static void checkP2pConfig(WifiP2pConfig c) {
        if (c == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        if (!TextUtils.isEmpty(c.deviceAddress)) {
        } else {
            throw new IllegalArgumentException("deviceAddress cannot be empty");
        }
    }

    private void checkMac(String mac) {
        Matcher match = macPattern.matcher(mac);
        if (match.find()) {
        } else {
            throw new IllegalArgumentException("MAC needs to be well-formed");
        }
    }

    public Channel initialize(Context srcContext, Looper srcLooper, ChannelListener listener) {
        return initalizeChannel(srcContext, srcLooper, listener, getMessenger());
    }

    public Channel initializeInternal(Context srcContext, Looper srcLooper, ChannelListener listener) {
        return initalizeChannel(srcContext, srcLooper, listener, getP2pStateMachineMessenger());
    }

    private Channel initalizeChannel(Context srcContext, Looper srcLooper, ChannelListener listener, Messenger messenger) {
        if (messenger == null) {
            return null;
        }
        Channel c = new Channel(srcContext, srcLooper, listener);
        if (c.mAsyncChannel.connectSync(srcContext, c.mHandler, messenger) == 0) {
            return c;
        }
        return null;
    }

    public void discoverPeers(Channel c, ActionListener listener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(c);
        c.mAsyncChannel.sendMessage(DISCOVER_PEERS, 0, c.putListener(listener));
    }

    public void stopPeerDiscovery(Channel c, ActionListener listener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(c);
        c.mAsyncChannel.sendMessage(STOP_DISCOVERY, 0, c.putListener(listener));
    }

    public void connect(Channel c, WifiP2pConfig config, ActionListener listener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(c);
        checkP2pConfig(config);
        c.mAsyncChannel.sendMessage(CONNECT, 0, c.putListener(listener), config);
    }

    public void cancelConnect(Channel c, ActionListener listener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(c);
        c.mAsyncChannel.sendMessage(CANCEL_CONNECT, 0, c.putListener(listener));
    }

    public void createGroup(Channel c, ActionListener listener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(c);
        c.mAsyncChannel.sendMessage(CREATE_GROUP, -2, c.putListener(listener));
    }

    public void removeGroup(Channel c, ActionListener listener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(c);
        c.mAsyncChannel.sendMessage(REMOVE_GROUP, 0, c.putListener(listener));
    }

    public void listen(Channel c, boolean enable, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(enable ? START_LISTEN : STOP_LISTEN, 0, c.putListener(listener));
    }

    public void setWifiP2pChannels(Channel c, int lc, int oc, ActionListener listener) {
        checkChannel(c);
        Bundle p2pChannels = new Bundle();
        p2pChannels.putInt("lc", lc);
        p2pChannels.putInt("oc", oc);
        c.mAsyncChannel.sendMessage(SET_CHANNEL, 0, c.putListener(listener), p2pChannels);
    }

    public void startWps(Channel c, WpsInfo wps, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(START_WPS, 0, c.putListener(listener), wps);
    }

    public void addLocalService(Channel c, WifiP2pServiceInfo servInfo, ActionListener listener) {
        checkChannel(c);
        checkServiceInfo(servInfo);
        c.mAsyncChannel.sendMessage(ADD_LOCAL_SERVICE, 0, c.putListener(listener), servInfo);
    }

    public void removeLocalService(Channel c, WifiP2pServiceInfo servInfo, ActionListener listener) {
        checkChannel(c);
        checkServiceInfo(servInfo);
        c.mAsyncChannel.sendMessage(REMOVE_LOCAL_SERVICE, 0, c.putListener(listener), servInfo);
    }

    public void clearLocalServices(Channel c, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(CLEAR_LOCAL_SERVICES, 0, c.putListener(listener));
    }

    public void setServiceResponseListener(Channel c, ServiceResponseListener listener) {
        checkChannel(c);
        c.mServRspListener = listener;
    }

    public void setDnsSdResponseListeners(Channel c, DnsSdServiceResponseListener servListener, DnsSdTxtRecordListener txtListener) {
        checkChannel(c);
        c.mDnsSdServRspListener = servListener;
        c.mDnsSdTxtListener = txtListener;
    }

    public void setUpnpServiceResponseListener(Channel c, UpnpServiceResponseListener listener) {
        checkChannel(c);
        c.mUpnpServRspListener = listener;
    }

    public void discoverServices(Channel c, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(DISCOVER_SERVICES, 0, c.putListener(listener));
    }

    public void addServiceRequest(Channel c, WifiP2pServiceRequest req, ActionListener listener) {
        checkChannel(c);
        checkServiceRequest(req);
        c.mAsyncChannel.sendMessage(ADD_SERVICE_REQUEST, 0, c.putListener(listener), req);
    }

    public void removeServiceRequest(Channel c, WifiP2pServiceRequest req, ActionListener listener) {
        checkChannel(c);
        checkServiceRequest(req);
        c.mAsyncChannel.sendMessage(REMOVE_SERVICE_REQUEST, 0, c.putListener(listener), req);
    }

    public void clearServiceRequests(Channel c, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(CLEAR_SERVICE_REQUESTS, 0, c.putListener(listener));
    }

    public void requestPeers(Channel c, PeerListListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(REQUEST_PEERS, 0, c.putListener(listener));
    }

    public void requestConnectionInfo(Channel c, ConnectionInfoListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(REQUEST_CONNECTION_INFO, 0, c.putListener(listener));
    }

    public void requestGroupInfo(Channel c, GroupInfoListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(REQUEST_GROUP_INFO, 0, c.putListener(listener));
    }

    public void setDeviceName(Channel c, String devName, ActionListener listener) {
        checkChannel(c);
        WifiP2pDevice d = new WifiP2pDevice();
        d.deviceName = devName;
        c.mAsyncChannel.sendMessage(SET_DEVICE_NAME, 0, c.putListener(listener), d);
    }

    public void setWFDInfo(Channel c, WifiP2pWfdInfo wfdInfo, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(SET_WFD_INFO, 0, c.putListener(listener), wfdInfo);
    }

    public void deletePersistentGroup(Channel c, int netId, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(DELETE_PERSISTENT_GROUP, netId, c.putListener(listener));
    }

    public void requestPersistentGroupInfo(Channel c, PersistentGroupInfoListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(REQUEST_PERSISTENT_GROUP_INFO, 0, c.putListener(listener));
    }

    public void setMiracastMode(int mode) {
        try {
            this.mService.setMiracastMode(mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setMiracastMode(int mode, int freq) {
        try {
            this.mService.setMiracastModeEx(mode, freq);
        } catch (RemoteException e) {
        }
    }

    public void setCrossmountMode(int mode, int freq) {
        try {
            this.mService.setMiracastModeEx(mode, freq);
        } catch (RemoteException e) {
        }
    }

    public Messenger getMessenger() {
        try {
            return this.mService.getMessenger();
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

    public void getNfcHandoverRequest(Channel c, HandoverMessageListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(GET_HANDOVER_REQUEST, 0, c.putListener(listener));
    }

    public void getNfcHandoverSelect(Channel c, HandoverMessageListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(GET_HANDOVER_SELECT, 0, c.putListener(listener));
    }

    public void initiatorReportNfcHandover(Channel c, String handoverSelect, ActionListener listener) {
        checkChannel(c);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_HANDOVER_MESSAGE, handoverSelect);
        c.mAsyncChannel.sendMessage(INITIATOR_REPORT_NFC_HANDOVER, 0, c.putListener(listener), bundle);
    }

    public void responderReportNfcHandover(Channel c, String handoverRequest, ActionListener listener) {
        checkChannel(c);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_HANDOVER_MESSAGE, handoverRequest);
        c.mAsyncChannel.sendMessage(RESPONDER_REPORT_NFC_HANDOVER, 0, c.putListener(listener), bundle);
    }

    public String getMacAddress() {
        try {
            return this.mService.getMacAddress();
        } catch (RemoteException e) {
            return null;
        }
    }

    public void requestWifiP2pLinkInfo(Channel c, String interfaceAddress, WifiP2pLinkInfoListener listener) {
        WifiP2pLinkInfo info = new WifiP2pLinkInfo();
        info.interfaceAddress = interfaceAddress;
        checkChannel(c);
        c.mAsyncChannel.sendMessage(REQUEST_LINK_INFO, 0, c.putListener(listener), info);
    }

    public void setP2pAutoChannel(Channel c, boolean enable, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(SET_AUTO_CHANNEL_SELECT, enable ? 1 : 0, c.putListener(listener));
    }

    public void deinitialize(Channel c) {
        Log.i(TAG, "deinitialize()");
        checkChannel(c);
        c.clearListener();
    }

    public String getPeerIpAddress(String peerMacAddress) {
        try {
            return this.mService.getPeerIpAddress(peerMacAddress);
        } catch (RemoteException e) {
            return null;
        }
    }

    public void setGCInviteResult(Channel c, boolean accept, int goIntent, ActionListener listener) {
        checkChannel(c);
        if (accept) {
            c.mAsyncChannel.sendMessage(PEER_CONNECTION_USER_ACCEPT_FROM_OUTER, goIntent, c.putListener(listener));
        } else {
            c.mAsyncChannel.sendMessage(PEER_CONNECTION_USER_REJECT_FROM_OUTER, -1, c.putListener(listener));
        }
    }

    public void setGCInviteResult(Channel c, boolean accept, int goIntent, int pinMethod, String pinCode, ActionListener listener) {
        checkChannel(c);
        if (pinCode == null) {
            throw new IllegalArgumentException("pinCode needs to be configured");
        }
        if (pinMethod != 2 && pinMethod != 1) {
            throw new IllegalArgumentException("pinMethod needs to be WpsInfo.KEYPAD/WpsInfo.DISPLAY");
        }
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_PIN_CODE, pinCode);
        bundle.putInt(EXTRA_PIN_METHOD, pinMethod);
        if (accept) {
            c.mAsyncChannel.sendMessage(PEER_CONNECTION_USER_ACCEPT_FROM_OUTER, goIntent, c.putListener(listener), bundle);
        } else {
            c.mAsyncChannel.sendMessage(PEER_CONNECTION_USER_REJECT_FROM_OUTER, -1, c.putListener(listener), bundle);
        }
    }

    public void setFreqConflictExResult(Channel c, boolean accept, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(FREQ_CONFLICT_EX_RESULT, accept ? 1 : 0, c.putListener(listener));
    }

    public void removeClient(Channel c, String mac, ActionListener listener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(c);
        checkMac(mac);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_CLIENT_MESSAGE, mac);
        c.mAsyncChannel.sendMessage(REMOVE_CLIENT, 0, c.putListener(listener), bundle);
    }

    public void setCrossMountIE(boolean isAdd, String hexData) {
        try {
            this.mService.setCrossMountIE(isAdd, hexData);
        } catch (RemoteException e) {
        }
    }

    public void stopP2pFindOnly(Channel c, ActionListener listener) {
        Log.d(TAG, Thread.currentThread().getStackTrace()[2].getMethodName() + "(), pid: " + Process.myPid() + ", tid: " + Process.myTid() + ", uid: " + Process.myUid());
        checkChannel(c);
        c.mAsyncChannel.sendMessage(STOP_P2P_FIND_ONLY, 0, c.putListener(listener));
    }

    public void createGroup(Channel c, int netId, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(CREATE_GROUP, netId, c.putListener(listener));
    }

    public void discoverPeers(Channel c, int timeout, ActionListener listener) {
        checkChannel(c);
        c.mAsyncChannel.sendMessage(DISCOVER_PEERS, timeout, c.putListener(listener));
    }

    public void addPersistentGroup(Channel c, Map<String, String> variables, AddPersistentGroupListener listener) {
        checkChannel(c);
        Bundle bundle = new Bundle();
        HashMap<String, String> hVariables = new HashMap<>();
        if (variables != null && (variables instanceof HashMap)) {
            hVariables = (HashMap) variables;
        } else if (variables != null) {
            hVariables.putAll(variables);
        }
        bundle.putSerializable("variables", hVariables);
        c.mAsyncChannel.sendMessage(ADD_PERSISTENT_GROUP, 0, c.putListener(listener), bundle);
    }

    public void setBeamMode(int mode) {
        try {
            this.mService.setBeamMode(mode);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
