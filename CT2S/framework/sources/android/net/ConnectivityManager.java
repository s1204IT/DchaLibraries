package android.net;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkActivityListener;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.R;
import com.android.internal.telephony.ITelephony;
import com.android.internal.util.Preconditions;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.net.event.NetworkEventDispatcher;

public class ConnectivityManager {

    @Deprecated
    public static final String ACTION_BACKGROUND_DATA_SETTING_CHANGED = "android.net.conn.BACKGROUND_DATA_SETTING_CHANGED";
    public static final String ACTION_CAPTIVE_PORTAL_TEST_COMPLETED = "android.net.conn.CAPTIVE_PORTAL_TEST_COMPLETED";
    public static final String ACTION_DATA_ACTIVITY_CHANGE = "android.net.conn.DATA_ACTIVITY_CHANGE";
    public static final String ACTION_TETHER_LEASE_CHANGED = "android.net.conn.TETHER_LEASE_CHANGED";
    public static final String ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED";
    private static final int BASE = 524288;
    public static final int CALLBACK_AVAILABLE = 524290;
    public static final int CALLBACK_CAP_CHANGED = 524294;
    public static final int CALLBACK_EXIT = 524297;
    public static final int CALLBACK_IP_CHANGED = 524295;
    public static final int CALLBACK_LOSING = 524291;
    public static final int CALLBACK_LOST = 524292;
    public static final int CALLBACK_PRECHECK = 524289;
    public static final int CALLBACK_RELEASED = 524296;
    public static final int CALLBACK_UNAVAIL = 524293;
    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String CONNECTIVITY_ACTION_IMMEDIATE = "android.net.conn.CONNECTIVITY_CHANGE_IMMEDIATE";

    @Deprecated
    public static final int DEFAULT_NETWORK_PREFERENCE = 1;
    public static final String DHCP_EVENT_ADD = "add";
    public static final String DHCP_EVENT_DEL = "del";
    public static final String DHCP_EVENT_UPDATE = "update";
    private static final int EXPIRE_LEGACY_REQUEST = 524298;
    public static final String EXTRA_ACTIVE_TETHER = "activeArray";
    public static final String EXTRA_AVAILABLE_TETHER = "availableArray";
    public static final String EXTRA_DEVICE_TYPE = "deviceType";
    public static final String EXTRA_DHCP_EVENT = "dhcpEvent";
    public static final String EXTRA_DHCP_IP = "dhcpIp";
    public static final String EXTRA_DHCP_MAC = "dhcpMac";
    public static final String EXTRA_ERRORED_TETHER = "erroredArray";
    public static final String EXTRA_EXTRA_INFO = "extraInfo";
    public static final String EXTRA_INET_CONDITION = "inetCondition";
    public static final String EXTRA_IS_ACTIVE = "isActive";
    public static final String EXTRA_IS_CAPTIVE_PORTAL = "captivePortal";
    public static final String EXTRA_IS_FAILOVER = "isFailover";
    public static final String EXTRA_NETWORK = "android.net.extra.NETWORK";

    @Deprecated
    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_NETWORK_REQUEST = "android.net.extra.NETWORK_REQUEST";
    public static final String EXTRA_NETWORK_TYPE = "networkType";
    public static final String EXTRA_NO_CONNECTIVITY = "noConnectivity";
    public static final String EXTRA_OTHER_NETWORK_INFO = "otherNetwork";
    public static final String EXTRA_REALTIME_NS = "tsNanos";
    public static final String EXTRA_REASON = "reason";
    public static final String INET_CONDITION_ACTION = "android.net.conn.INET_CONDITION_ACTION";
    private static final int LISTEN = 1;
    public static final int MAX_NETWORK_REQUEST_TIMEOUT_MS = 6000000;
    public static final int MAX_NETWORK_TYPE = 17;
    public static final int MAX_RADIO_TYPE = 17;
    public static final int NETID_UNSET = 0;
    private static final int REQUEST = 2;
    public static final int REQUEST_ID_UNSET = 0;
    private static final String TAG = "ConnectivityManager";
    public static final int TETHER_ERROR_DISABLE_NAT_ERROR = 9;
    public static final int TETHER_ERROR_ENABLE_NAT_ERROR = 8;
    public static final int TETHER_ERROR_IFACE_CFG_ERROR = 10;
    public static final int TETHER_ERROR_MASTER_ERROR = 5;
    public static final int TETHER_ERROR_NO_ERROR = 0;
    public static final int TETHER_ERROR_SERVICE_UNAVAIL = 2;
    public static final int TETHER_ERROR_TETHER_IFACE_ERROR = 6;
    public static final int TETHER_ERROR_UNAVAIL_IFACE = 4;
    public static final int TETHER_ERROR_UNKNOWN_IFACE = 1;
    public static final int TETHER_ERROR_UNSUPPORTED = 3;
    public static final int TETHER_ERROR_UNTETHER_IFACE_ERROR = 7;
    public static final int TYPE_BLUETOOTH = 7;
    public static final int TYPE_DUMMY = 8;
    public static final int TYPE_ETHERNET = 9;
    public static final int TYPE_MOBILE = 0;
    public static final int TYPE_MOBILE_CBS = 12;
    public static final int TYPE_MOBILE_DUN = 4;
    public static final int TYPE_MOBILE_EMERGENCY = 15;
    public static final int TYPE_MOBILE_FOTA = 10;
    public static final int TYPE_MOBILE_HIPRI = 5;
    public static final int TYPE_MOBILE_IA = 14;
    public static final int TYPE_MOBILE_IMS = 11;
    public static final int TYPE_MOBILE_MMS = 2;
    public static final int TYPE_MOBILE_SUPL = 3;
    public static final int TYPE_NONE = -1;
    public static final int TYPE_PROXY = 16;
    public static final int TYPE_VPN = 17;
    public static final int TYPE_WIFI = 1;
    public static final int TYPE_WIFI_P2P = 13;
    public static final int TYPE_WIMAX = 6;
    public static final String WIFI_VOIP_EVENT_CHANGED = "android.net.conn.WIFI_VOIP_SWITCH_EVENT";
    private static ConnectivityManager sInstance;
    private INetworkManagementService mNMService;
    private final ArrayMap<OnNetworkActiveListener, INetworkActivityListener> mNetworkActivityListeners = new ArrayMap<>();
    private final IConnectivityManager mService;
    private static HashMap<NetworkCapabilities, LegacyRequest> sLegacyRequests = new HashMap<>();
    static final HashMap<NetworkRequest, NetworkCallback> sNetworkCallback = new HashMap<>();
    static final AtomicInteger sCallbackRefCount = new AtomicInteger(0);
    static CallbackHandler sCallbackHandler = null;

    public interface OnNetworkActiveListener {
        void onNetworkActive();
    }

    public static boolean isNetworkTypeValid(int networkType) {
        return networkType >= 0 && networkType <= 17;
    }

    public static String getNetworkTypeName(int type) {
        switch (type) {
            case 0:
                return "MOBILE";
            case 1:
                return "WIFI";
            case 2:
                return "MOBILE_MMS";
            case 3:
                return "MOBILE_SUPL";
            case 4:
                return "MOBILE_DUN";
            case 5:
                return "MOBILE_HIPRI";
            case 6:
                return "WIMAX";
            case 7:
                return "BLUETOOTH";
            case 8:
                return "DUMMY";
            case 9:
                return "ETHERNET";
            case 10:
                return "MOBILE_FOTA";
            case 11:
                return "MOBILE_IMS";
            case 12:
                return "MOBILE_CBS";
            case 13:
                return "WIFI_P2P";
            case 14:
                return "MOBILE_IA";
            case 15:
                return "MOBILE_EMERGENCY";
            case 16:
                return "PROXY";
            default:
                return Integer.toString(type);
        }
    }

    public static boolean isNetworkTypeMobile(int networkType) {
        switch (networkType) {
            case 0:
            case 2:
            case 3:
            case 4:
            case 5:
            case 10:
            case 11:
            case 12:
            case 14:
            case 15:
                return true;
            case 1:
            case 6:
            case 7:
            case 8:
            case 9:
            case 13:
            default:
                return false;
        }
    }

    public static boolean isNetworkTypeWifi(int networkType) {
        switch (networkType) {
            case 1:
            case 13:
                return true;
            default:
                return false;
        }
    }

    public void setNetworkPreference(int preference) {
    }

    public int getNetworkPreference() {
        return -1;
    }

    public NetworkInfo getActiveNetworkInfo() {
        try {
            return this.mService.getActiveNetworkInfo();
        } catch (RemoteException e) {
            return null;
        }
    }

    public NetworkInfo getActiveNetworkInfoForUid(int uid) {
        try {
            return this.mService.getActiveNetworkInfoForUid(uid);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NetworkInfo getNetworkInfo(int networkType) {
        try {
            return this.mService.getNetworkInfo(networkType);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NetworkInfo getNetworkInfo(Network network) {
        try {
            return this.mService.getNetworkInfoForNetwork(network);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NetworkInfo[] getAllNetworkInfo() {
        try {
            return this.mService.getAllNetworkInfo();
        } catch (RemoteException e) {
            return null;
        }
    }

    public Network getNetworkForType(int networkType) {
        try {
            return this.mService.getNetworkForType(networkType);
        } catch (RemoteException e) {
            return null;
        }
    }

    public Network[] getAllNetworks() {
        try {
            return this.mService.getAllNetworks();
        } catch (RemoteException e) {
            return null;
        }
    }

    public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int userId) {
        try {
            return this.mService.getDefaultNetworkCapabilitiesForUser(userId);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NetworkInfo getProvisioningOrActiveNetworkInfo() {
        try {
            return this.mService.getProvisioningOrActiveNetworkInfo();
        } catch (RemoteException e) {
            return null;
        }
    }

    public LinkProperties getActiveLinkProperties() {
        try {
            return this.mService.getActiveLinkProperties();
        } catch (RemoteException e) {
            return null;
        }
    }

    public LinkProperties getLinkProperties(int networkType) {
        try {
            return this.mService.getLinkPropertiesForType(networkType);
        } catch (RemoteException e) {
            return null;
        }
    }

    public LinkProperties getLinkProperties(Network network) {
        try {
            return this.mService.getLinkProperties(network);
        } catch (RemoteException e) {
            return null;
        }
    }

    public NetworkCapabilities getNetworkCapabilities(Network network) {
        try {
            return this.mService.getNetworkCapabilities(network);
        } catch (RemoteException e) {
            return null;
        }
    }

    public int startUsingNetworkFeature(int networkType, String feature) {
        int i = 3;
        NetworkCapabilities netCap = networkCapabilitiesForFeature(networkType, feature);
        if (netCap == null) {
            Log.d(TAG, "Can't satisfy startUsingNetworkFeature for " + networkType + ", " + feature);
        } else {
            synchronized (sLegacyRequests) {
                LegacyRequest l = sLegacyRequests.get(netCap);
                if (l != null) {
                    Log.d(TAG, "renewing startUsingNetworkFeature request " + l.networkRequest);
                    renewRequestLocked(l);
                    i = l.currentNetwork != null ? 0 : 1;
                } else {
                    NetworkRequest request = requestNetworkForFeatureLocked(netCap);
                    if (request != null) {
                        Log.d(TAG, "starting startUsingNetworkFeature for request " + request);
                        i = 1;
                    } else {
                        Log.d(TAG, " request Failed");
                    }
                }
            }
        }
        return i;
    }

    public int stopUsingNetworkFeature(int networkType, String feature) {
        NetworkCapabilities netCap = networkCapabilitiesForFeature(networkType, feature);
        if (netCap == null) {
            Log.d(TAG, "Can't satisfy stopUsingNetworkFeature for " + networkType + ", " + feature);
            return -1;
        }
        if (removeRequestForFeature(netCap)) {
            Log.d(TAG, "stopUsingNetworkFeature for " + networkType + ", " + feature);
        }
        return 1;
    }

    public static void maybeMarkCapabilitiesRestricted(NetworkCapabilities nc) {
        int[] arr$ = nc.getCapabilities();
        for (int capability : arr$) {
            switch (capability) {
                case 2:
                case 3:
                case 4:
                case 5:
                case 7:
                case 8:
                case 9:
                case 10:
                case 13:
                    break;
                case 6:
                case 11:
                case 12:
                default:
                    return;
            }
        }
        nc.removeCapability(13);
    }

    private NetworkCapabilities networkCapabilitiesForFeature(int networkType, String feature) {
        int cap;
        if (networkType == 0) {
            if ("enableMMS".equals(feature)) {
                cap = 0;
            } else if ("enableSUPL".equals(feature)) {
                cap = 1;
            } else if ("enableDUN".equals(feature) || "enableDUNAlways".equals(feature)) {
                cap = 2;
            } else if ("enableHIPRI".equals(feature)) {
                cap = 12;
            } else if ("enableFOTA".equals(feature)) {
                cap = 3;
            } else if ("enableIMS".equals(feature)) {
                cap = 4;
            } else {
                if (!"enableCBS".equals(feature)) {
                    return null;
                }
                cap = 5;
            }
            NetworkCapabilities netCap = new NetworkCapabilities();
            netCap.addTransportType(0).addCapability(cap);
            maybeMarkCapabilitiesRestricted(netCap);
            return netCap;
        }
        if (networkType != 1 || !"p2p".equals(feature)) {
            return null;
        }
        NetworkCapabilities netCap2 = new NetworkCapabilities();
        netCap2.addTransportType(1);
        netCap2.addCapability(6);
        maybeMarkCapabilitiesRestricted(netCap2);
        return netCap2;
    }

    private int inferLegacyTypeForNetworkCapabilities(NetworkCapabilities netCap) {
        if (netCap != null && netCap.hasTransport(0)) {
            String type = null;
            int result = -1;
            if (netCap.hasCapability(5)) {
                type = "enableCBS";
                result = 12;
            } else if (netCap.hasCapability(4)) {
                type = "enableIMS";
                result = 11;
            } else if (netCap.hasCapability(3)) {
                type = "enableFOTA";
                result = 10;
            } else if (netCap.hasCapability(2)) {
                type = "enableDUN";
                result = 4;
            } else if (netCap.hasCapability(1)) {
                type = "enableSUPL";
                result = 3;
            } else if (netCap.hasCapability(0)) {
                type = "enableMMS";
                result = 2;
            } else if (netCap.hasCapability(12)) {
                type = "enableHIPRI";
                result = 5;
            }
            if (type != null) {
                NetworkCapabilities testCap = networkCapabilitiesForFeature(0, type);
                if (testCap.equalsNetCapabilities(netCap) && testCap.equalsTransportTypes(netCap)) {
                    return result;
                }
            }
            return -1;
        }
        return -1;
    }

    private int legacyTypeForNetworkCapabilities(NetworkCapabilities netCap) {
        if (netCap == null) {
            return -1;
        }
        if (netCap.hasCapability(5)) {
            return 12;
        }
        if (netCap.hasCapability(4)) {
            return 11;
        }
        if (netCap.hasCapability(3)) {
            return 10;
        }
        if (netCap.hasCapability(2)) {
            return 4;
        }
        if (netCap.hasCapability(1)) {
            return 3;
        }
        if (netCap.hasCapability(0)) {
            return 2;
        }
        if (netCap.hasCapability(12)) {
            return 5;
        }
        return netCap.hasCapability(6) ? 13 : -1;
    }

    private static class LegacyRequest {
        Network currentNetwork;
        int delay;
        int expireSequenceNumber;
        NetworkCallback networkCallback;
        NetworkCapabilities networkCapabilities;
        NetworkRequest networkRequest;

        private LegacyRequest() {
            this.delay = -1;
            this.networkCallback = new NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    LegacyRequest.this.currentNetwork = network;
                    Log.d(ConnectivityManager.TAG, "startUsingNetworkFeature got Network:" + network);
                    ConnectivityManager.setProcessDefaultNetworkForHostResolution(network);
                }

                @Override
                public void onLost(Network network) {
                    if (network.equals(LegacyRequest.this.currentNetwork)) {
                        LegacyRequest.this.clearDnsBinding();
                    }
                    Log.d(ConnectivityManager.TAG, "startUsingNetworkFeature lost Network:" + network);
                }
            };
        }

        private void clearDnsBinding() {
            if (this.currentNetwork != null) {
                this.currentNetwork = null;
                ConnectivityManager.setProcessDefaultNetworkForHostResolution(null);
            }
        }
    }

    private NetworkRequest findRequestForFeature(NetworkCapabilities netCap) {
        synchronized (sLegacyRequests) {
            LegacyRequest l = sLegacyRequests.get(netCap);
            if (l != null) {
                return l.networkRequest;
            }
            return null;
        }
    }

    private void renewRequestLocked(LegacyRequest l) {
        l.expireSequenceNumber++;
        Log.d(TAG, "renewing request to seqNum " + l.expireSequenceNumber);
        sendExpireMsgForFeature(l.networkCapabilities, l.expireSequenceNumber, l.delay);
    }

    private void expireRequest(NetworkCapabilities netCap, int sequenceNum) {
        synchronized (sLegacyRequests) {
            LegacyRequest l = sLegacyRequests.get(netCap);
            if (l != null) {
                int ourSeqNum = l.expireSequenceNumber;
                if (l.expireSequenceNumber == sequenceNum) {
                    removeRequestForFeature(netCap);
                }
                Log.d(TAG, "expireRequest with " + ourSeqNum + ", " + sequenceNum);
            }
        }
    }

    private NetworkRequest requestNetworkForFeatureLocked(NetworkCapabilities netCap) {
        int delay = -1;
        int type = legacyTypeForNetworkCapabilities(netCap);
        try {
            delay = this.mService.getRestoreDefaultNetworkDelay(type);
        } catch (RemoteException e) {
        }
        LegacyRequest l = new LegacyRequest();
        l.networkCapabilities = netCap;
        l.delay = delay;
        l.expireSequenceNumber = 0;
        l.networkRequest = sendRequestForNetwork(netCap, l.networkCallback, 0, 2, type);
        if (l.networkRequest == null) {
            return null;
        }
        sLegacyRequests.put(netCap, l);
        sendExpireMsgForFeature(netCap, l.expireSequenceNumber, delay);
        return l.networkRequest;
    }

    private void sendExpireMsgForFeature(NetworkCapabilities netCap, int seqNum, int delay) {
        if (delay >= 0) {
            Log.d(TAG, "sending expire msg with seqNum " + seqNum + " and delay " + delay);
            Message msg = sCallbackHandler.obtainMessage(EXPIRE_LEGACY_REQUEST, seqNum, 0, netCap);
            sCallbackHandler.sendMessageDelayed(msg, delay);
        }
    }

    private boolean removeRequestForFeature(NetworkCapabilities netCap) {
        LegacyRequest l;
        synchronized (sLegacyRequests) {
            l = sLegacyRequests.remove(netCap);
        }
        if (l == null) {
            return false;
        }
        unregisterNetworkCallback(l.networkCallback);
        l.clearDnsBinding();
        return true;
    }

    public boolean requestRouteToHost(int networkType, int hostAddress) {
        return requestRouteToHostAddress(networkType, NetworkUtils.intToInetAddress(hostAddress));
    }

    public boolean requestRouteToHostAddress(int networkType, InetAddress hostAddress) {
        try {
            return this.mService.requestRouteToHostAddress(networkType, hostAddress.getAddress());
        } catch (RemoteException e) {
            return false;
        }
    }

    @Deprecated
    public boolean getBackgroundDataSetting() {
        return true;
    }

    @Deprecated
    public void setBackgroundDataSetting(boolean allowBackgroundData) {
    }

    public NetworkQuotaInfo getActiveNetworkQuotaInfo() {
        try {
            return this.mService.getActiveNetworkQuotaInfo();
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean getMobileDataEnabled() {
        IBinder b = ServiceManager.getService("phone");
        if (b != null) {
            try {
                ITelephony it = ITelephony.Stub.asInterface(b);
                int subId = SubscriptionManager.getDefaultDataSubId();
                Log.d(TAG, "getMobileDataEnabled()+ subId=" + subId);
                boolean retVal = it.getDataEnabled(subId);
                Log.d(TAG, "getMobileDataEnabled()- subId=" + subId + " retVal=" + retVal);
                return retVal;
            } catch (RemoteException e) {
            }
        }
        Log.d(TAG, "getMobileDataEnabled()- remote exception retVal=false");
        return false;
    }

    private INetworkManagementService getNetworkManagementService() {
        INetworkManagementService iNetworkManagementService;
        synchronized (this) {
            if (this.mNMService != null) {
                iNetworkManagementService = this.mNMService;
            } else {
                IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
                this.mNMService = INetworkManagementService.Stub.asInterface(b);
                iNetworkManagementService = this.mNMService;
            }
        }
        return iNetworkManagementService;
    }

    public void addDefaultNetworkActiveListener(final OnNetworkActiveListener l) {
        INetworkActivityListener rl = new INetworkActivityListener.Stub() {
            @Override
            public void onNetworkActive() throws RemoteException {
                l.onNetworkActive();
            }
        };
        try {
            getNetworkManagementService().registerNetworkActivityListener(rl);
            this.mNetworkActivityListeners.put(l, rl);
        } catch (RemoteException e) {
        }
    }

    public void removeDefaultNetworkActiveListener(OnNetworkActiveListener l) {
        INetworkActivityListener rl = this.mNetworkActivityListeners.get(l);
        if (rl == null) {
            throw new IllegalArgumentException("Listener not registered: " + l);
        }
        try {
            getNetworkManagementService().unregisterNetworkActivityListener(rl);
        } catch (RemoteException e) {
        }
    }

    public boolean isDefaultNetworkActive() {
        try {
            return getNetworkManagementService().isNetworkActive();
        } catch (RemoteException e) {
            return false;
        }
    }

    public ConnectivityManager(IConnectivityManager service) {
        this.mService = (IConnectivityManager) Preconditions.checkNotNull(service, "missing IConnectivityManager");
        sInstance = this;
    }

    public static ConnectivityManager from(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static final void enforceTetherChangePermission(Context context) {
        if (context.getResources().getStringArray(R.array.config_mobile_hotspot_provision_app).length == 2) {
            context.enforceCallingOrSelfPermission(Manifest.permission.CONNECTIVITY_INTERNAL, "ConnectivityService");
        } else {
            context.enforceCallingOrSelfPermission(Manifest.permission.CHANGE_NETWORK_STATE, "ConnectivityService");
        }
    }

    public static ConnectivityManager getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("No ConnectivityManager yet constructed");
        }
        return sInstance;
    }

    public String[] getTetherableIfaces() {
        try {
            return this.mService.getTetherableIfaces();
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public String[] getTetheredIfaces() {
        try {
            return this.mService.getTetheredIfaces();
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public String[] getTetheringErroredIfaces() {
        try {
            return this.mService.getTetheringErroredIfaces();
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public String[] getTetheredDhcpRanges() {
        try {
            return this.mService.getTetheredDhcpRanges();
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public int tether(String iface) {
        try {
            return this.mService.tether(iface);
        } catch (RemoteException e) {
            return 2;
        }
    }

    public int untether(String iface) {
        try {
            return this.mService.untether(iface);
        } catch (RemoteException e) {
            return 2;
        }
    }

    public boolean isTetheringSupported() {
        try {
            return this.mService.isTetheringSupported();
        } catch (RemoteException e) {
            return false;
        }
    }

    public String[] getTetherableUsbRegexs() {
        try {
            return this.mService.getTetherableUsbRegexs();
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public String[] getTetherableWifiRegexs() {
        try {
            return this.mService.getTetherableWifiRegexs();
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public String[] getTetherableBluetoothRegexs() {
        try {
            return this.mService.getTetherableBluetoothRegexs();
        } catch (RemoteException e) {
            return new String[0];
        }
    }

    public int setUsbTethering(boolean enable) {
        try {
            return this.mService.setUsbTethering(enable);
        } catch (RemoteException e) {
            return 2;
        }
    }

    public int getLastTetherError(String iface) {
        try {
            return this.mService.getLastTetherError(iface);
        } catch (RemoteException e) {
            return 2;
        }
    }

    public void reportInetCondition(int networkType, int percentage) {
        try {
            this.mService.reportInetCondition(networkType, percentage);
        } catch (RemoteException e) {
        }
    }

    public void reportBadNetwork(Network network) {
        try {
            this.mService.reportBadNetwork(network);
        } catch (RemoteException e) {
        }
    }

    public void setGlobalProxy(ProxyInfo p) {
        try {
            this.mService.setGlobalProxy(p);
        } catch (RemoteException e) {
        }
    }

    public ProxyInfo getGlobalProxy() {
        try {
            return this.mService.getGlobalProxy();
        } catch (RemoteException e) {
            return null;
        }
    }

    public ProxyInfo getDefaultProxy() {
        Network network = getProcessDefaultNetwork();
        if (network != null) {
            ProxyInfo globalProxy = getGlobalProxy();
            if (globalProxy == null) {
                LinkProperties lp = getLinkProperties(network);
                if (lp == null) {
                    return null;
                }
                return lp.getHttpProxy();
            }
            return globalProxy;
        }
        try {
            return this.mService.getDefaultProxy();
        } catch (RemoteException e) {
            return null;
        }
    }

    public void setDataDependency(int networkType, boolean met) {
        try {
            this.mService.setDataDependency(networkType, met);
        } catch (RemoteException e) {
        }
    }

    public boolean isNetworkSupported(int networkType) {
        try {
            return this.mService.isNetworkSupported(networkType);
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean isActiveNetworkMetered() {
        try {
            return this.mService.isActiveNetworkMetered();
        } catch (RemoteException e) {
            return false;
        }
    }

    public boolean updateLockdownVpn() {
        try {
            return this.mService.updateLockdownVpn();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void captivePortalCheckCompleted(NetworkInfo info, boolean isCaptivePortal) {
        try {
            this.mService.captivePortalCheckCompleted(info, isCaptivePortal);
        } catch (RemoteException e) {
        }
    }

    public void supplyMessenger(int networkType, Messenger messenger) {
        try {
            this.mService.supplyMessenger(networkType, messenger);
        } catch (RemoteException e) {
        }
    }

    public int checkMobileProvisioning(int suggestedTimeOutMs) {
        try {
            int timeOutMs = this.mService.checkMobileProvisioning(suggestedTimeOutMs);
            return timeOutMs;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public String getMobileProvisioningUrl() {
        try {
            return this.mService.getMobileProvisioningUrl();
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getMobileRedirectedProvisioningUrl() {
        try {
            return this.mService.getMobileRedirectedProvisioningUrl();
        } catch (RemoteException e) {
            return null;
        }
    }

    public void setProvisioningNotificationVisible(boolean visible, int networkType, String action) {
        try {
            this.mService.setProvisioningNotificationVisible(visible, networkType, action);
        } catch (RemoteException e) {
        }
    }

    public void setAirplaneMode(boolean enable) {
        try {
            this.mService.setAirplaneMode(enable);
        } catch (RemoteException e) {
        }
    }

    public void registerNetworkFactory(Messenger messenger, String name) {
        try {
            this.mService.registerNetworkFactory(messenger, name);
        } catch (RemoteException e) {
        }
    }

    public void unregisterNetworkFactory(Messenger messenger) {
        try {
            this.mService.unregisterNetworkFactory(messenger);
        } catch (RemoteException e) {
        }
    }

    public void registerNetworkAgent(Messenger messenger, NetworkInfo ni, LinkProperties lp, NetworkCapabilities nc, int score, NetworkMisc misc) {
        try {
            this.mService.registerNetworkAgent(messenger, ni, lp, nc, score, misc);
        } catch (RemoteException e) {
        }
    }

    public static class NetworkCallback {
        public static final int AVAILABLE = 2;
        public static final int CANCELED = 8;
        public static final int CAP_CHANGED = 6;
        public static final int LOSING = 3;
        public static final int LOST = 4;
        public static final int PRECHECK = 1;
        public static final int PROP_CHANGED = 7;
        public static final int UNAVAIL = 5;
        private NetworkRequest networkRequest;

        public void onPreCheck(Network network) {
        }

        public void onAvailable(Network network) {
        }

        public void onLosing(Network network, int maxMsToLive) {
        }

        public void onLost(Network network) {
        }

        public void onUnavailable() {
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        }
    }

    private class CallbackHandler extends Handler {
        private static final String TAG = "ConnectivityManager.CallbackHandler";
        private final HashMap<NetworkRequest, NetworkCallback> mCallbackMap;
        private final ConnectivityManager mCm;
        private final AtomicInteger mRefCount;

        CallbackHandler(Looper looper, HashMap<NetworkRequest, NetworkCallback> callbackMap, AtomicInteger refCount, ConnectivityManager cm) {
            super(looper);
            this.mCallbackMap = callbackMap;
            this.mRefCount = refCount;
            this.mCm = cm;
        }

        @Override
        public void handleMessage(Message message) {
            NetworkCallback callbacks;
            NetworkCallback callbacks2;
            Log.d(TAG, "CM callback handler got msg " + message.what);
            switch (message.what) {
                case ConnectivityManager.CALLBACK_PRECHECK:
                    NetworkRequest request = (NetworkRequest) getObject(message, NetworkRequest.class);
                    NetworkCallback callbacks3 = getCallbacks(request);
                    if (callbacks3 != null) {
                        callbacks3.onPreCheck((Network) getObject(message, Network.class));
                        return;
                    } else {
                        Log.e(TAG, "callback not found for PRECHECK message");
                        return;
                    }
                case ConnectivityManager.CALLBACK_AVAILABLE:
                    NetworkRequest request2 = (NetworkRequest) getObject(message, NetworkRequest.class);
                    NetworkCallback callbacks4 = getCallbacks(request2);
                    if (callbacks4 != null) {
                        callbacks4.onAvailable((Network) getObject(message, Network.class));
                        return;
                    } else {
                        Log.e(TAG, "callback not found for AVAILABLE message");
                        return;
                    }
                case ConnectivityManager.CALLBACK_LOSING:
                    NetworkRequest request3 = (NetworkRequest) getObject(message, NetworkRequest.class);
                    NetworkCallback callbacks5 = getCallbacks(request3);
                    if (callbacks5 != null) {
                        callbacks5.onLosing((Network) getObject(message, Network.class), message.arg1);
                        return;
                    } else {
                        Log.e(TAG, "callback not found for LOSING message");
                        return;
                    }
                case ConnectivityManager.CALLBACK_LOST:
                    NetworkRequest request4 = (NetworkRequest) getObject(message, NetworkRequest.class);
                    NetworkCallback callbacks6 = getCallbacks(request4);
                    if (callbacks6 != null) {
                        callbacks6.onLost((Network) getObject(message, Network.class));
                        return;
                    } else {
                        Log.e(TAG, "callback not found for LOST message");
                        return;
                    }
                case ConnectivityManager.CALLBACK_UNAVAIL:
                    NetworkRequest request5 = (NetworkRequest) getObject(message, NetworkRequest.class);
                    synchronized (this.mCallbackMap) {
                        callbacks2 = this.mCallbackMap.get(request5);
                        break;
                    }
                    if (callbacks2 != null) {
                        callbacks2.onUnavailable();
                        return;
                    } else {
                        Log.e(TAG, "callback not found for UNAVAIL message");
                        return;
                    }
                case ConnectivityManager.CALLBACK_CAP_CHANGED:
                    NetworkRequest request6 = (NetworkRequest) getObject(message, NetworkRequest.class);
                    NetworkCallback callbacks7 = getCallbacks(request6);
                    if (callbacks7 != null) {
                        Network network = (Network) getObject(message, Network.class);
                        NetworkCapabilities cap = (NetworkCapabilities) getObject(message, NetworkCapabilities.class);
                        callbacks7.onCapabilitiesChanged(network, cap);
                        return;
                    }
                    Log.e(TAG, "callback not found for CAP_CHANGED message");
                    return;
                case ConnectivityManager.CALLBACK_IP_CHANGED:
                    NetworkRequest request7 = (NetworkRequest) getObject(message, NetworkRequest.class);
                    NetworkCallback callbacks8 = getCallbacks(request7);
                    if (callbacks8 != null) {
                        Network network2 = (Network) getObject(message, Network.class);
                        LinkProperties lp = (LinkProperties) getObject(message, LinkProperties.class);
                        callbacks8.onLinkPropertiesChanged(network2, lp);
                        return;
                    }
                    Log.e(TAG, "callback not found for IP_CHANGED message");
                    return;
                case ConnectivityManager.CALLBACK_RELEASED:
                    NetworkRequest req = (NetworkRequest) getObject(message, NetworkRequest.class);
                    synchronized (this.mCallbackMap) {
                        callbacks = this.mCallbackMap.remove(req);
                        break;
                    }
                    if (callbacks != null) {
                        synchronized (this.mRefCount) {
                            if (this.mRefCount.decrementAndGet() == 0) {
                                getLooper().quit();
                            }
                            break;
                        }
                        return;
                    }
                    Log.e(TAG, "callback not found for CANCELED message");
                    return;
                case ConnectivityManager.CALLBACK_EXIT:
                    Log.d(TAG, "Listener quiting");
                    getLooper().quit();
                    return;
                case ConnectivityManager.EXPIRE_LEGACY_REQUEST:
                    ConnectivityManager.this.expireRequest((NetworkCapabilities) message.obj, message.arg1);
                    return;
                default:
                    return;
            }
        }

        private Object getObject(Message msg, Class c) {
            return msg.getData().getParcelable(c.getSimpleName());
        }

        private NetworkCallback getCallbacks(NetworkRequest req) {
            NetworkCallback networkCallback;
            synchronized (this.mCallbackMap) {
                networkCallback = this.mCallbackMap.get(req);
            }
            return networkCallback;
        }
    }

    private void incCallbackHandlerRefCount() {
        synchronized (sCallbackRefCount) {
            if (sCallbackRefCount.incrementAndGet() == 1) {
                HandlerThread callbackThread = new HandlerThread(TAG);
                callbackThread.start();
                sCallbackHandler = new CallbackHandler(callbackThread.getLooper(), sNetworkCallback, sCallbackRefCount, this);
            }
        }
    }

    private void decCallbackHandlerRefCount() {
        synchronized (sCallbackRefCount) {
            if (sCallbackRefCount.decrementAndGet() == 0) {
                sCallbackHandler.obtainMessage(CALLBACK_EXIT).sendToTarget();
                sCallbackHandler = null;
            }
        }
    }

    private NetworkRequest sendRequestForNetwork(NetworkCapabilities need, NetworkCallback networkCallback, int timeoutSec, int action, int legacyType) {
        if (networkCallback == null) {
            throw new IllegalArgumentException("null NetworkCallback");
        }
        if (need == null) {
            throw new IllegalArgumentException("null NetworkCapabilities");
        }
        try {
            incCallbackHandlerRefCount();
            synchronized (sNetworkCallback) {
                if (action == 1) {
                    networkCallback.networkRequest = this.mService.listenForNetwork(need, new Messenger(sCallbackHandler), new Binder());
                } else {
                    networkCallback.networkRequest = this.mService.requestNetwork(need, new Messenger(sCallbackHandler), timeoutSec, new Binder(), legacyType);
                }
                if (networkCallback.networkRequest != null) {
                    sNetworkCallback.put(networkCallback.networkRequest, networkCallback);
                }
            }
        } catch (RemoteException e) {
        }
        if (networkCallback.networkRequest == null) {
            decCallbackHandlerRefCount();
        }
        return networkCallback.networkRequest;
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback) {
        sendRequestForNetwork(request.networkCapabilities, networkCallback, 0, 2, inferLegacyTypeForNetworkCapabilities(request.networkCapabilities));
    }

    public void requestNetwork(NetworkRequest request, NetworkCallback networkCallback, int timeoutMs) {
        sendRequestForNetwork(request.networkCapabilities, networkCallback, timeoutMs, 2, inferLegacyTypeForNetworkCapabilities(request.networkCapabilities));
    }

    public void requestNetwork(NetworkRequest request, PendingIntent operation) {
        checkPendingIntent(operation);
        try {
            this.mService.pendingRequestForNetwork(request.networkCapabilities, operation);
        } catch (RemoteException e) {
        }
    }

    public void releaseNetworkRequest(PendingIntent operation) {
        checkPendingIntent(operation);
        try {
            this.mService.releasePendingNetworkRequest(operation);
        } catch (RemoteException e) {
        }
    }

    private void checkPendingIntent(PendingIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("PendingIntent cannot be null.");
        }
    }

    public void registerNetworkCallback(NetworkRequest request, NetworkCallback networkCallback) {
        sendRequestForNetwork(request.networkCapabilities, networkCallback, 0, 1, -1);
    }

    public void unregisterNetworkCallback(NetworkCallback networkCallback) {
        if (networkCallback != null && networkCallback.networkRequest != null && networkCallback.networkRequest.requestId != 0) {
            try {
                this.mService.releaseNetworkRequest(networkCallback.networkRequest);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid NetworkCallback");
    }

    public static boolean setProcessDefaultNetwork(Network network) {
        int netId = network == null ? 0 : network.netId;
        if (netId == NetworkUtils.getNetworkBoundToProcess()) {
            return true;
        }
        if (!NetworkUtils.bindProcessToNetwork(netId)) {
            return false;
        }
        Proxy.setHttpProxySystemProperty(getInstance().getDefaultProxy());
        InetAddress.clearDnsCache();
        NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
        return true;
    }

    public static Network getProcessDefaultNetwork() {
        int netId = NetworkUtils.getNetworkBoundToProcess();
        if (netId == 0) {
            return null;
        }
        return new Network(netId);
    }

    public static boolean setProcessDefaultNetworkForHostResolution(Network network) {
        return NetworkUtils.bindProcessToNetworkForHostResolution(network == null ? 0 : network.netId);
    }
}
