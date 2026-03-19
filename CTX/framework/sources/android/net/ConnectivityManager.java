package android.net;

import android.Manifest;
import android.annotation.SystemApi;
import android.app.PendingIntent;
import android.content.Context;
import android.net.INetworkPolicyManager;
import android.net.NetworkRequest;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkActivityListener;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.provider.Settings;
import android.security.keystore.KeyProperties;
import android.telephony.SubscriptionManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.internal.R;
import com.android.internal.telephony.ITelephony;
import com.android.internal.util.Preconditions;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import libcore.net.event.NetworkEventDispatcher;

public class ConnectivityManager {

    @Deprecated
    public static final String ACTION_BACKGROUND_DATA_SETTING_CHANGED = "android.net.conn.BACKGROUND_DATA_SETTING_CHANGED";
    public static final String ACTION_CAPTIVE_PORTAL_SIGN_IN = "android.net.conn.CAPTIVE_PORTAL";
    public static final String ACTION_CAPTIVE_PORTAL_TEST_COMPLETED = "android.net.conn.CAPTIVE_PORTAL_TEST_COMPLETED";
    public static final String ACTION_DATA_ACTIVITY_CHANGE = "android.net.conn.DATA_ACTIVITY_CHANGE";
    public static final String ACTION_PROMPT_LOST_VALIDATION = "android.net.conn.PROMPT_LOST_VALIDATION";
    public static final String ACTION_PROMPT_UNVALIDATED = "android.net.conn.PROMPT_UNVALIDATED";
    public static final String ACTION_RESTRICT_BACKGROUND_CHANGED = "android.net.conn.RESTRICT_BACKGROUND_CHANGED";
    public static final String ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED";
    private static final int BASE = 524288;
    public static final int CALLBACK_AVAILABLE = 524290;
    public static final int CALLBACK_CAP_CHANGED = 524294;
    public static final int CALLBACK_IP_CHANGED = 524295;
    public static final int CALLBACK_LOSING = 524291;
    public static final int CALLBACK_LOST = 524292;
    public static final int CALLBACK_PRECHECK = 524289;
    public static final int CALLBACK_RESUMED = 524298;
    public static final int CALLBACK_SUSPENDED = 524297;
    public static final int CALLBACK_UNAVAIL = 524293;

    @Deprecated
    public static final String CONNECTIVITY_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    public static final String CONNECTIVITY_ACTION_SUPL = "android.net.conn.CONNECTIVITY_CHANGE_SUPL";

    @Deprecated
    public static final int DEFAULT_NETWORK_PREFERENCE = 1;
    private static final int EXPIRE_LEGACY_REQUEST = 524296;
    public static final String EXTRA_ACTIVE_LOCAL_ONLY = "localOnlyArray";
    public static final String EXTRA_ACTIVE_TETHER = "tetherArray";
    public static final String EXTRA_ADD_TETHER_TYPE = "extraAddTetherType";
    public static final String EXTRA_AVAILABLE_TETHER = "availableArray";
    public static final String EXTRA_CAPTIVE_PORTAL = "android.net.extra.CAPTIVE_PORTAL";
    public static final String EXTRA_CAPTIVE_PORTAL_PROBE_SPEC = "android.net.extra.CAPTIVE_PORTAL_PROBE_SPEC";
    public static final String EXTRA_CAPTIVE_PORTAL_URL = "android.net.extra.CAPTIVE_PORTAL_URL";
    public static final String EXTRA_CAPTIVE_PORTAL_USER_AGENT = "android.net.extra.CAPTIVE_PORTAL_USER_AGENT";
    public static final String EXTRA_DEVICE_TYPE = "deviceType";
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
    public static final String EXTRA_PROVISION_CALLBACK = "extraProvisionCallback";
    public static final String EXTRA_REALTIME_NS = "tsNanos";
    public static final String EXTRA_REASON = "reason";
    public static final String EXTRA_REM_TETHER_TYPE = "extraRemTetherType";
    public static final String EXTRA_RUN_PROVISION = "extraRunProvision";
    public static final String EXTRA_SET_ALARM = "extraSetAlarm";
    public static final String INET_CONDITION_ACTION = "android.net.conn.INET_CONDITION_ACTION";
    private static final int LISTEN = 1;
    public static final int MAX_AOSP_NETWORK_TYPE = 17;
    public static final int MAX_NETWORK_TYPE = 29;
    public static final int MAX_RADIO_TYPE = 17;
    private static final int MIN_NETWORK_TYPE = 0;
    public static final int MULTIPATH_PREFERENCE_HANDOVER = 1;
    public static final int MULTIPATH_PREFERENCE_PERFORMANCE = 4;
    public static final int MULTIPATH_PREFERENCE_RELIABILITY = 2;
    public static final int MULTIPATH_PREFERENCE_UNMETERED = 7;
    public static final int NETID_UNSET = 0;
    public static final String PRIVATE_DNS_DEFAULT_MODE_FALLBACK = "opportunistic";
    public static final String PRIVATE_DNS_MODE_OFF = "off";
    public static final String PRIVATE_DNS_MODE_OPPORTUNISTIC = "opportunistic";
    public static final String PRIVATE_DNS_MODE_PROVIDER_HOSTNAME = "hostname";
    private static final int REQUEST = 2;
    public static final int REQUEST_ID_UNSET = 0;
    public static final int RESTRICT_BACKGROUND_STATUS_DISABLED = 1;
    public static final int RESTRICT_BACKGROUND_STATUS_ENABLED = 3;
    public static final int RESTRICT_BACKGROUND_STATUS_WHITELISTED = 2;
    private static final String TAG = "ConnectivityManager";

    @SystemApi
    public static final int TETHERING_BLUETOOTH = 2;
    public static final int TETHERING_INVALID = -1;

    @SystemApi
    public static final int TETHERING_USB = 1;

    @SystemApi
    public static final int TETHERING_WIFI = 0;
    public static final int TETHER_ERROR_DISABLE_NAT_ERROR = 9;
    public static final int TETHER_ERROR_ENABLE_NAT_ERROR = 8;
    public static final int TETHER_ERROR_IFACE_CFG_ERROR = 10;
    public static final int TETHER_ERROR_MASTER_ERROR = 5;
    public static final int TETHER_ERROR_NO_ERROR = 0;
    public static final int TETHER_ERROR_PROVISION_FAILED = 11;
    public static final int TETHER_ERROR_SERVICE_UNAVAIL = 2;
    public static final int TETHER_ERROR_TETHER_IFACE_ERROR = 6;
    public static final int TETHER_ERROR_UNAVAIL_IFACE = 4;
    public static final int TETHER_ERROR_UNKNOWN_IFACE = 1;
    public static final int TETHER_ERROR_UNSUPPORTED = 3;
    public static final int TETHER_ERROR_UNTETHER_IFACE_ERROR = 7;

    @Deprecated
    public static final int TYPE_BLUETOOTH = 7;

    @Deprecated
    public static final int TYPE_DUMMY = 8;

    @Deprecated
    public static final int TYPE_ETHERNET = 9;

    @Deprecated
    public static final int TYPE_MOBILE = 0;
    public static final int TYPE_MOBILE_BIP = 27;

    @Deprecated
    public static final int TYPE_MOBILE_CBS = 12;

    @Deprecated
    public static final int TYPE_MOBILE_DUN = 4;

    @Deprecated
    public static final int TYPE_MOBILE_EMERGENCY = 15;

    @Deprecated
    public static final int TYPE_MOBILE_FOTA = 10;

    @Deprecated
    public static final int TYPE_MOBILE_HIPRI = 5;

    @Deprecated
    public static final int TYPE_MOBILE_IA = 14;

    @Deprecated
    public static final int TYPE_MOBILE_IMS = 11;

    @Deprecated
    public static final int TYPE_MOBILE_MMS = 2;
    public static final int TYPE_MOBILE_PREEMPT = 29;
    public static final int TYPE_MOBILE_RCS = 26;

    @Deprecated
    public static final int TYPE_MOBILE_SUPL = 3;
    public static final int TYPE_MOBILE_VSIM = 28;
    public static final int TYPE_MOBILE_WAP = 21;
    public static final int TYPE_MOBILE_XCAP = 25;
    public static final int TYPE_NONE = -1;

    @Deprecated
    public static final int TYPE_PROXY = 16;

    @Deprecated
    public static final int TYPE_VPN = 17;

    @Deprecated
    public static final int TYPE_WIFI = 1;

    @Deprecated
    public static final int TYPE_WIFI_P2P = 13;

    @Deprecated
    public static final int TYPE_WIMAX = 6;
    private static CallbackHandler sCallbackHandler;
    private static final HashMap<NetworkRequest, NetworkCallback> sCallbacks;
    private static ConnectivityManager sInstance;
    private static final SparseIntArray sLegacyTypeToCapability;
    private final Context mContext;
    private INetworkManagementService mNMService;
    private INetworkPolicyManager mNPManager;
    private final ArrayMap<OnNetworkActiveListener, INetworkActivityListener> mNetworkActivityListeners = new ArrayMap<>();
    private final IConnectivityManager mService;
    private static final NetworkRequest ALREADY_UNREGISTERED = new NetworkRequest.Builder().clearCapabilities().build();
    private static HashMap<NetworkCapabilities, LegacyRequest> sLegacyRequests = new HashMap<>();
    private static final SparseIntArray sLegacyTypeToTransport = new SparseIntArray();

    public interface Errors {
        public static final int TOO_MANY_REQUESTS = 1;
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface MultipathPreference {
    }

    public interface OnNetworkActiveListener {
        void onNetworkActive();
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RestrictBackgroundStatus {
    }

    public static class TooManyRequestsException extends RuntimeException {
    }

    static {
        sLegacyTypeToTransport.put(0, 0);
        sLegacyTypeToTransport.put(12, 0);
        sLegacyTypeToTransport.put(4, 0);
        sLegacyTypeToTransport.put(10, 0);
        sLegacyTypeToTransport.put(5, 0);
        sLegacyTypeToTransport.put(11, 0);
        sLegacyTypeToTransport.put(2, 0);
        sLegacyTypeToTransport.put(3, 0);
        sLegacyTypeToTransport.put(1, 1);
        sLegacyTypeToTransport.put(13, 1);
        sLegacyTypeToTransport.put(7, 2);
        sLegacyTypeToTransport.put(9, 3);
        sLegacyTypeToCapability = new SparseIntArray();
        sLegacyTypeToCapability.put(12, 5);
        sLegacyTypeToCapability.put(4, 2);
        sLegacyTypeToCapability.put(10, 3);
        sLegacyTypeToCapability.put(11, 4);
        sLegacyTypeToCapability.put(2, 0);
        sLegacyTypeToCapability.put(3, 1);
        sLegacyTypeToCapability.put(13, 6);
        sCallbacks = new HashMap<>();
    }

    @Deprecated
    public static boolean isNetworkTypeValid(int i) {
        return (i >= 0 && i <= 17) || (21 <= i && i <= 29);
    }

    @Deprecated
    public static String getNetworkTypeName(int i) {
        switch (i) {
            case -1:
                return KeyProperties.DIGEST_NONE;
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
            case 17:
                return "VPN";
            default:
                return Integer.toString(i);
        }
    }

    @Deprecated
    public static boolean isNetworkTypeMobile(int i) {
        switch (i) {
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

    @Deprecated
    public static boolean isNetworkTypeWifi(int i) {
        if (i == 1 || i == 13) {
            return true;
        }
        return false;
    }

    @Deprecated
    public void setNetworkPreference(int i) {
    }

    @Deprecated
    public int getNetworkPreference() {
        return -1;
    }

    public NetworkInfo getActiveNetworkInfo() {
        try {
            return this.mService.getActiveNetworkInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Network getActiveNetwork() {
        try {
            return this.mService.getActiveNetwork();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Network getActiveNetworkForUid(int i) {
        return getActiveNetworkForUid(i, false);
    }

    public Network getActiveNetworkForUid(int i, boolean z) {
        try {
            return this.mService.getActiveNetworkForUid(i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAlwaysOnVpnPackageSupportedForUser(int i, String str) {
        try {
            return this.mService.isAlwaysOnVpnPackageSupported(i, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean setAlwaysOnVpnPackageForUser(int i, String str, boolean z) {
        try {
            return this.mService.setAlwaysOnVpnPackage(i, str, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getAlwaysOnVpnPackageForUser(int i) {
        try {
            return this.mService.getAlwaysOnVpnPackage(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkInfo getActiveNetworkInfoForUid(int i) {
        return getActiveNetworkInfoForUid(i, false);
    }

    public NetworkInfo getActiveNetworkInfoForUid(int i, boolean z) {
        try {
            return this.mService.getActiveNetworkInfoForUid(i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public NetworkInfo getNetworkInfo(int i) {
        try {
            return this.mService.getNetworkInfo(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkInfo getNetworkInfo(Network network) {
        return getNetworkInfoForUid(network, Process.myUid(), false);
    }

    public NetworkInfo getNetworkInfoForUid(Network network, int i, boolean z) {
        try {
            return this.mService.getNetworkInfoForUid(network, i, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public NetworkInfo[] getAllNetworkInfo() {
        try {
            return this.mService.getAllNetworkInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public Network getNetworkForType(int i) {
        try {
            return this.mService.getNetworkForType(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public Network[] getAllNetworks() {
        try {
            return this.mService.getAllNetworks();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkCapabilities[] getDefaultNetworkCapabilitiesForUser(int i) {
        try {
            return this.mService.getDefaultNetworkCapabilitiesForUser(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LinkProperties getActiveLinkProperties() {
        try {
            return this.mService.getActiveLinkProperties();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public LinkProperties getLinkProperties(int i) {
        try {
            return this.mService.getLinkPropertiesForType(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public LinkProperties getLinkProperties(Network network) {
        try {
            return this.mService.getLinkProperties(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public NetworkCapabilities getNetworkCapabilities(Network network) {
        try {
            return this.mService.getNetworkCapabilities(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public String getCaptivePortalServerUrl() {
        try {
            return this.mService.getCaptivePortalServerUrl();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int startUsingNetworkFeature(int i, String str) {
        checkLegacyRoutingApiAccess();
        NetworkCapabilities networkCapabilitiesNetworkCapabilitiesForFeature = networkCapabilitiesForFeature(i, str);
        if (networkCapabilitiesNetworkCapabilitiesForFeature == null) {
            Log.d(TAG, "Can't satisfy startUsingNetworkFeature for " + i + ", " + str);
            return 3;
        }
        synchronized (sLegacyRequests) {
            LegacyRequest legacyRequest = sLegacyRequests.get(networkCapabilitiesNetworkCapabilitiesForFeature);
            if (legacyRequest != null) {
                Log.d(TAG, "renewing startUsingNetworkFeature request " + legacyRequest.networkRequest);
                renewRequestLocked(legacyRequest);
                if (legacyRequest.currentNetwork == null) {
                    return 1;
                }
                return 0;
            }
            NetworkRequest networkRequestRequestNetworkForFeatureLocked = requestNetworkForFeatureLocked(networkCapabilitiesNetworkCapabilitiesForFeature);
            if (networkRequestRequestNetworkForFeatureLocked != null) {
                Log.d(TAG, "starting startUsingNetworkFeature for request " + networkRequestRequestNetworkForFeatureLocked);
                return 1;
            }
            Log.d(TAG, " request Failed");
            return 3;
        }
    }

    @Deprecated
    public int stopUsingNetworkFeature(int i, String str) {
        checkLegacyRoutingApiAccess();
        NetworkCapabilities networkCapabilitiesNetworkCapabilitiesForFeature = networkCapabilitiesForFeature(i, str);
        if (networkCapabilitiesNetworkCapabilitiesForFeature == null) {
            Log.d(TAG, "Can't satisfy stopUsingNetworkFeature for " + i + ", " + str);
            return -1;
        }
        if (removeRequestForFeature(networkCapabilitiesNetworkCapabilitiesForFeature)) {
            Log.d(TAG, "stopUsingNetworkFeature for " + i + ", " + str);
            return 1;
        }
        return 1;
    }

    private NetworkCapabilities networkCapabilitiesForFeature(int i, String str) {
        if (i == 0) {
            switch (str) {
                case "enableCBS":
                    return networkCapabilitiesForType(12);
                case "enableDUN":
                case "enableDUNAlways":
                    return networkCapabilitiesForType(4);
                case "enableFOTA":
                    return networkCapabilitiesForType(10);
                case "enableHIPRI":
                    return networkCapabilitiesForType(5);
                case "enableIMS":
                    return networkCapabilitiesForType(11);
                case "enableMMS":
                    return networkCapabilitiesForType(2);
                case "enableSUPL":
                    return networkCapabilitiesForType(3);
                default:
                    return null;
            }
        }
        if (i != 1 || !"p2p".equals(str)) {
            return null;
        }
        return networkCapabilitiesForType(13);
    }

    private int inferLegacyTypeForNetworkCapabilities(NetworkCapabilities networkCapabilities) {
        int i;
        if (networkCapabilities == null || !networkCapabilities.hasTransport(0) || !networkCapabilities.hasCapability(1)) {
            return -1;
        }
        String str = null;
        if (networkCapabilities.hasCapability(5)) {
            str = "enableCBS";
            i = 12;
        } else if (networkCapabilities.hasCapability(4)) {
            str = "enableIMS";
            i = 11;
        } else if (networkCapabilities.hasCapability(3)) {
            str = "enableFOTA";
            i = 10;
        } else if (networkCapabilities.hasCapability(2)) {
            str = "enableDUN";
            i = 4;
        } else if (networkCapabilities.hasCapability(1)) {
            str = "enableSUPL";
            i = 3;
        } else if (networkCapabilities.hasCapability(12)) {
            str = "enableHIPRI";
            i = 5;
        } else {
            i = -1;
        }
        if (str != null) {
            NetworkCapabilities networkCapabilitiesNetworkCapabilitiesForFeature = networkCapabilitiesForFeature(0, str);
            if (networkCapabilitiesNetworkCapabilitiesForFeature.equalsNetCapabilities(networkCapabilities) && networkCapabilitiesNetworkCapabilitiesForFeature.equalsTransportTypes(networkCapabilities)) {
                return i;
            }
        }
        return -1;
    }

    private int legacyTypeForNetworkCapabilities(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities == null) {
            return -1;
        }
        if (networkCapabilities.hasCapability(5)) {
            return 12;
        }
        if (networkCapabilities.hasCapability(4)) {
            return 11;
        }
        if (networkCapabilities.hasCapability(3)) {
            return 10;
        }
        if (networkCapabilities.hasCapability(2)) {
            return 4;
        }
        if (networkCapabilities.hasCapability(1)) {
            return 3;
        }
        if (networkCapabilities.hasCapability(0)) {
            return 2;
        }
        if (networkCapabilities.hasCapability(12)) {
            return 5;
        }
        if (!networkCapabilities.hasCapability(6)) {
            return -1;
        }
        return 13;
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

    private NetworkRequest findRequestForFeature(NetworkCapabilities networkCapabilities) {
        synchronized (sLegacyRequests) {
            LegacyRequest legacyRequest = sLegacyRequests.get(networkCapabilities);
            if (legacyRequest == null) {
                return null;
            }
            return legacyRequest.networkRequest;
        }
    }

    private void renewRequestLocked(LegacyRequest legacyRequest) {
        legacyRequest.expireSequenceNumber++;
        Log.d(TAG, "renewing request to seqNum " + legacyRequest.expireSequenceNumber);
        sendExpireMsgForFeature(legacyRequest.networkCapabilities, legacyRequest.expireSequenceNumber, legacyRequest.delay);
    }

    private void expireRequest(NetworkCapabilities networkCapabilities, int i) {
        synchronized (sLegacyRequests) {
            LegacyRequest legacyRequest = sLegacyRequests.get(networkCapabilities);
            if (legacyRequest == null) {
                return;
            }
            int i2 = legacyRequest.expireSequenceNumber;
            if (legacyRequest.expireSequenceNumber == i) {
                removeRequestForFeature(networkCapabilities);
            }
            Log.d(TAG, "expireRequest with " + i2 + ", " + i);
        }
    }

    private NetworkRequest requestNetworkForFeatureLocked(NetworkCapabilities networkCapabilities) {
        int iLegacyTypeForNetworkCapabilities = legacyTypeForNetworkCapabilities(networkCapabilities);
        try {
            int restoreDefaultNetworkDelay = this.mService.getRestoreDefaultNetworkDelay(iLegacyTypeForNetworkCapabilities);
            LegacyRequest legacyRequest = new LegacyRequest();
            legacyRequest.networkCapabilities = networkCapabilities;
            legacyRequest.delay = restoreDefaultNetworkDelay;
            legacyRequest.expireSequenceNumber = 0;
            legacyRequest.networkRequest = sendRequestForNetwork(networkCapabilities, legacyRequest.networkCallback, 0, 2, iLegacyTypeForNetworkCapabilities, getDefaultHandler());
            if (legacyRequest.networkRequest == null) {
                return null;
            }
            sLegacyRequests.put(networkCapabilities, legacyRequest);
            sendExpireMsgForFeature(networkCapabilities, legacyRequest.expireSequenceNumber, restoreDefaultNetworkDelay);
            return legacyRequest.networkRequest;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private void sendExpireMsgForFeature(NetworkCapabilities networkCapabilities, int i, int i2) {
        if (i2 >= 0) {
            Log.d(TAG, "sending expire msg with seqNum " + i + " and delay " + i2);
            CallbackHandler defaultHandler = getDefaultHandler();
            defaultHandler.sendMessageDelayed(defaultHandler.obtainMessage(EXPIRE_LEGACY_REQUEST, i, 0, networkCapabilities), (long) i2);
        }
    }

    private boolean removeRequestForFeature(NetworkCapabilities networkCapabilities) {
        LegacyRequest legacyRequestRemove;
        synchronized (sLegacyRequests) {
            legacyRequestRemove = sLegacyRequests.remove(networkCapabilities);
        }
        if (legacyRequestRemove == null) {
            return false;
        }
        unregisterNetworkCallback(legacyRequestRemove.networkCallback);
        legacyRequestRemove.clearDnsBinding();
        return true;
    }

    public static NetworkCapabilities networkCapabilitiesForType(int i) {
        NetworkCapabilities networkCapabilities = new NetworkCapabilities();
        int i2 = sLegacyTypeToTransport.get(i, -1);
        Preconditions.checkArgument(i2 != -1, "unknown legacy type: " + i);
        networkCapabilities.addTransportType(i2);
        networkCapabilities.addCapability(sLegacyTypeToCapability.get(i, 12));
        networkCapabilities.maybeMarkCapabilitiesRestricted();
        return networkCapabilities;
    }

    public static class PacketKeepaliveCallback {
        public void onStarted() {
        }

        public void onStopped() {
        }

        public void onError(int i) {
        }
    }

    public class PacketKeepalive {
        public static final int BINDER_DIED = -10;
        public static final int ERROR_HARDWARE_ERROR = -31;
        public static final int ERROR_HARDWARE_UNSUPPORTED = -30;
        public static final int ERROR_INVALID_INTERVAL = -24;
        public static final int ERROR_INVALID_IP_ADDRESS = -21;
        public static final int ERROR_INVALID_LENGTH = -23;
        public static final int ERROR_INVALID_NETWORK = -20;
        public static final int ERROR_INVALID_PORT = -22;
        public static final int MIN_INTERVAL = 10;
        public static final int NATT_PORT = 4500;
        public static final int NO_KEEPALIVE = -1;
        public static final int SUCCESS = 0;
        private static final String TAG = "PacketKeepalive";
        private final PacketKeepaliveCallback mCallback;
        private final Looper mLooper;
        private final Messenger mMessenger;
        private final Network mNetwork;
        private volatile Integer mSlot;

        void stopLooper() {
            this.mLooper.quit();
        }

        public void stop() {
            try {
                ConnectivityManager.this.mService.stopKeepalive(this.mNetwork, this.mSlot.intValue());
            } catch (RemoteException e) {
                Log.e(TAG, "Error stopping packet keepalive: ", e);
                stopLooper();
            }
        }

        private PacketKeepalive(Network network, PacketKeepaliveCallback packetKeepaliveCallback) {
            Preconditions.checkNotNull(network, "network cannot be null");
            Preconditions.checkNotNull(packetKeepaliveCallback, "callback cannot be null");
            this.mNetwork = network;
            this.mCallback = packetKeepaliveCallback;
            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();
            this.mLooper = handlerThread.getLooper();
            this.mMessenger = new Messenger(new Handler(this.mLooper) {
                @Override
                public void handleMessage(Message message) {
                    if (message.what == 528397) {
                        int i = message.arg2;
                        try {
                            if (i == 0) {
                                if (PacketKeepalive.this.mSlot != null) {
                                    PacketKeepalive.this.mSlot = null;
                                    PacketKeepalive.this.stopLooper();
                                    PacketKeepalive.this.mCallback.onStopped();
                                } else {
                                    PacketKeepalive.this.mSlot = Integer.valueOf(message.arg1);
                                    PacketKeepalive.this.mCallback.onStarted();
                                }
                            } else {
                                PacketKeepalive.this.stopLooper();
                                PacketKeepalive.this.mCallback.onError(i);
                            }
                            return;
                        } catch (Exception e) {
                            Log.e(PacketKeepalive.TAG, "Exception in keepalive callback(" + i + ")", e);
                            return;
                        }
                    }
                    Log.e(PacketKeepalive.TAG, "Unhandled message " + Integer.toHexString(message.what));
                }
            });
        }
    }

    public PacketKeepalive startNattKeepalive(Network network, int i, PacketKeepaliveCallback packetKeepaliveCallback, InetAddress inetAddress, int i2, InetAddress inetAddress2) {
        PacketKeepalive packetKeepalive = new PacketKeepalive(network, packetKeepaliveCallback);
        try {
            this.mService.startNattKeepalive(network, i, packetKeepalive.mMessenger, new Binder(), inetAddress.getHostAddress(), i2, inetAddress2.getHostAddress());
            return packetKeepalive;
        } catch (RemoteException e) {
            Log.e(TAG, "Error starting packet keepalive: ", e);
            packetKeepalive.stopLooper();
            return null;
        }
    }

    @Deprecated
    public boolean requestRouteToHost(int i, int i2) {
        return requestRouteToHostAddress(i, NetworkUtils.intToInetAddress(i2));
    }

    @Deprecated
    public boolean requestRouteToHostAddress(int i, InetAddress inetAddress) {
        checkLegacyRoutingApiAccess();
        try {
            return this.mService.requestRouteToHostAddress(i, inetAddress.getAddress());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean getBackgroundDataSetting() {
        return true;
    }

    @Deprecated
    public void setBackgroundDataSetting(boolean z) {
    }

    @Deprecated
    public NetworkQuotaInfo getActiveNetworkQuotaInfo() {
        try {
            return this.mService.getActiveNetworkQuotaInfo();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public boolean getMobileDataEnabled() {
        IBinder service = ServiceManager.getService("phone");
        if (service != null) {
            try {
                ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(service);
                int defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId();
                Log.d(TAG, "getMobileDataEnabled()+ subId=" + defaultDataSubscriptionId);
                boolean zIsUserDataEnabled = iTelephonyAsInterface.isUserDataEnabled(defaultDataSubscriptionId);
                Log.d(TAG, "getMobileDataEnabled()- subId=" + defaultDataSubscriptionId + " retVal=" + zIsUserDataEnabled);
                return zIsUserDataEnabled;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        Log.d(TAG, "getMobileDataEnabled()- remote exception retVal=false");
        return false;
    }

    private INetworkManagementService getNetworkManagementService() {
        synchronized (this) {
            if (this.mNMService != null) {
                return this.mNMService;
            }
            this.mNMService = INetworkManagementService.Stub.asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
            return this.mNMService;
        }
    }

    public void addDefaultNetworkActiveListener(final OnNetworkActiveListener onNetworkActiveListener) {
        INetworkActivityListener.Stub stub = new INetworkActivityListener.Stub() {
            @Override
            public void onNetworkActive() throws RemoteException {
                onNetworkActiveListener.onNetworkActive();
            }
        };
        try {
            getNetworkManagementService().registerNetworkActivityListener(stub);
            this.mNetworkActivityListeners.put(onNetworkActiveListener, stub);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeDefaultNetworkActiveListener(OnNetworkActiveListener onNetworkActiveListener) {
        INetworkActivityListener iNetworkActivityListener = this.mNetworkActivityListeners.get(onNetworkActiveListener);
        Preconditions.checkArgument(iNetworkActivityListener != null, "Listener was not registered.");
        try {
            getNetworkManagementService().unregisterNetworkActivityListener(iNetworkActivityListener);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isDefaultNetworkActive() {
        try {
            return getNetworkManagementService().isNetworkActive();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ConnectivityManager(Context context, IConnectivityManager iConnectivityManager) {
        this.mContext = (Context) Preconditions.checkNotNull(context, "missing context");
        this.mService = (IConnectivityManager) Preconditions.checkNotNull(iConnectivityManager, "missing IConnectivityManager");
        sInstance = this;
    }

    public static ConnectivityManager from(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static final void enforceChangePermission(Context context) {
        int callingUid = Binder.getCallingUid();
        Settings.checkAndNoteChangeNetworkStateOperation(context, callingUid, Settings.getPackageNameForUid(context, callingUid), true);
    }

    public static final void enforceTetherChangePermission(Context context, String str) {
        Preconditions.checkNotNull(context, "Context cannot be null");
        Preconditions.checkNotNull(str, "callingPkg cannot be null");
        if (context.getResources().getStringArray(R.array.config_mobile_hotspot_provision_app).length == 2) {
            context.enforceCallingOrSelfPermission(Manifest.permission.TETHER_PRIVILEGED, "ConnectivityService");
        } else {
            Settings.checkAndNoteWriteSettingsOperation(context, Binder.getCallingUid(), str, true);
        }
    }

    @Deprecated
    static ConnectivityManager getInstanceOrNull() {
        return sInstance;
    }

    @Deprecated
    private static ConnectivityManager getInstance() {
        if (getInstanceOrNull() == null) {
            throw new IllegalStateException("No ConnectivityManager yet constructed");
        }
        return getInstanceOrNull();
    }

    public String[] getTetherableIfaces() {
        try {
            return this.mService.getTetherableIfaces();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetheredIfaces() {
        try {
            return this.mService.getTetheredIfaces();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetheringErroredIfaces() {
        try {
            return this.mService.getTetheringErroredIfaces();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetheredDhcpRanges() {
        try {
            return this.mService.getTetheredDhcpRanges();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int tether(String str) {
        try {
            String opPackageName = this.mContext.getOpPackageName();
            Log.i(TAG, "tether caller:" + opPackageName);
            return this.mService.tether(str, opPackageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int untether(String str) {
        try {
            String opPackageName = this.mContext.getOpPackageName();
            Log.i(TAG, "untether caller:" + opPackageName);
            return this.mService.untether(str, opPackageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public boolean isTetheringSupported() {
        try {
            return this.mService.isTetheringSupported(this.mContext.getOpPackageName());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (SecurityException e2) {
            return false;
        }
    }

    @SystemApi
    public static abstract class OnStartTetheringCallback {
        public void onTetheringStarted() {
        }

        public void onTetheringFailed() {
        }
    }

    @SystemApi
    public void startTethering(int i, boolean z, OnStartTetheringCallback onStartTetheringCallback) {
        startTethering(i, z, onStartTetheringCallback, null);
    }

    @SystemApi
    public void startTethering(int i, boolean z, final OnStartTetheringCallback onStartTetheringCallback, Handler handler) {
        Preconditions.checkNotNull(onStartTetheringCallback, "OnStartTetheringCallback cannot be null.");
        ResultReceiver resultReceiver = new ResultReceiver(handler) {
            @Override
            protected void onReceiveResult(int i2, Bundle bundle) {
                if (i2 == 0) {
                    onStartTetheringCallback.onTetheringStarted();
                } else {
                    onStartTetheringCallback.onTetheringFailed();
                }
            }
        };
        try {
            String opPackageName = this.mContext.getOpPackageName();
            Log.i(TAG, "startTethering caller:" + opPackageName);
            this.mService.startTethering(i, resultReceiver, z, opPackageName);
        } catch (RemoteException e) {
            Log.e(TAG, "Exception trying to start tethering.", e);
            resultReceiver.send(2, null);
        }
    }

    @SystemApi
    public void stopTethering(int i) {
        try {
            String opPackageName = this.mContext.getOpPackageName();
            Log.i(TAG, "stopTethering caller:" + opPackageName);
            this.mService.stopTethering(i, opPackageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetherableUsbRegexs() {
        try {
            return this.mService.getTetherableUsbRegexs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetherableWifiRegexs() {
        try {
            return this.mService.getTetherableWifiRegexs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String[] getTetherableBluetoothRegexs() {
        try {
            return this.mService.getTetherableBluetoothRegexs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int setUsbTethering(boolean z) {
        try {
            String opPackageName = this.mContext.getOpPackageName();
            Log.i(TAG, "setUsbTethering caller:" + opPackageName);
            return this.mService.setUsbTethering(z, opPackageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getLastTetherError(String str) {
        try {
            return this.mService.getLastTetherError(str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reportInetCondition(int i, int i2) {
        try {
            this.mService.reportInetCondition(i, i2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void reportBadNetwork(Network network) {
        try {
            this.mService.reportNetworkConnectivity(network, true);
            this.mService.reportNetworkConnectivity(network, false);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void reportNetworkConnectivity(Network network, boolean z) {
        try {
            this.mService.reportNetworkConnectivity(network, z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setGlobalProxy(ProxyInfo proxyInfo) {
        try {
            this.mService.setGlobalProxy(proxyInfo);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ProxyInfo getGlobalProxy() {
        try {
            return this.mService.getGlobalProxy();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ProxyInfo getProxyForNetwork(Network network) {
        try {
            return this.mService.getProxyForNetwork(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ProxyInfo getDefaultProxy() {
        return getProxyForNetwork(getBoundNetworkForProcess());
    }

    @Deprecated
    public boolean isNetworkSupported(int i) {
        try {
            return this.mService.isNetworkSupported(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isActiveNetworkMetered() {
        try {
            return this.mService.isActiveNetworkMetered();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean updateLockdownVpn() {
        try {
            return this.mService.updateLockdownVpn();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int checkMobileProvisioning(int i) {
        try {
            return this.mService.checkMobileProvisioning(i);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public String getMobileProvisioningUrl() {
        try {
            return this.mService.getMobileProvisioningUrl();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public void setProvisioningNotificationVisible(boolean z, int i, String str) {
        try {
            this.mService.setProvisioningNotificationVisible(z, i, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setAirplaneMode(boolean z) {
        try {
            if (!Build.IS_USER) {
                Thread.dumpStack();
            }
            this.mService.setAirplaneMode(z);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void registerNetworkFactory(Messenger messenger, String str) {
        try {
            this.mService.registerNetworkFactory(messenger, str);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterNetworkFactory(Messenger messenger) {
        try {
            this.mService.unregisterNetworkFactory(messenger);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int registerNetworkAgent(Messenger messenger, NetworkInfo networkInfo, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i, NetworkMisc networkMisc) {
        try {
            return this.mService.registerNetworkAgent(messenger, networkInfo, linkProperties, networkCapabilities, i, networkMisc);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static class NetworkCallback {
        private NetworkRequest networkRequest;

        public void onPreCheck(Network network) {
        }

        public void onAvailable(Network network, NetworkCapabilities networkCapabilities, LinkProperties linkProperties) {
            onAvailable(network);
            if (!networkCapabilities.hasCapability(21)) {
                onNetworkSuspended(network);
            }
            onCapabilitiesChanged(network, networkCapabilities);
            onLinkPropertiesChanged(network, linkProperties);
        }

        public void onAvailable(Network network) {
        }

        public void onLosing(Network network, int i) {
        }

        public void onLost(Network network) {
        }

        public void onUnavailable() {
        }

        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        }

        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        }

        public void onNetworkSuspended(Network network) {
        }

        public void onNetworkResumed(Network network) {
        }
    }

    private static RuntimeException convertServiceException(ServiceSpecificException serviceSpecificException) {
        if (serviceSpecificException.errorCode == 1) {
            return new TooManyRequestsException();
        }
        Log.w(TAG, "Unknown service error code " + serviceSpecificException.errorCode);
        return new RuntimeException(serviceSpecificException);
    }

    public static String getCallbackName(int i) {
        switch (i) {
            case CALLBACK_PRECHECK:
                return "CALLBACK_PRECHECK";
            case 524290:
                return "CALLBACK_AVAILABLE";
            case 524291:
                return "CALLBACK_LOSING";
            case 524292:
                return "CALLBACK_LOST";
            case CALLBACK_UNAVAIL:
                return "CALLBACK_UNAVAIL";
            case CALLBACK_CAP_CHANGED:
                return "CALLBACK_CAP_CHANGED";
            case CALLBACK_IP_CHANGED:
                return "CALLBACK_IP_CHANGED";
            case EXPIRE_LEGACY_REQUEST:
                return "EXPIRE_LEGACY_REQUEST";
            case CALLBACK_SUSPENDED:
                return "CALLBACK_SUSPENDED";
            case CALLBACK_RESUMED:
                return "CALLBACK_RESUMED";
            default:
                return Integer.toString(i);
        }
    }

    private class CallbackHandler extends Handler {
        private static final boolean DBG = false;
        private static final String TAG = "ConnectivityManager.CallbackHandler";

        CallbackHandler(Looper looper) {
            super(looper);
        }

        CallbackHandler(ConnectivityManager connectivityManager, Handler handler) {
            this(((Handler) Preconditions.checkNotNull(handler, "Handler cannot be null.")).getLooper());
        }

        @Override
        public void handleMessage(Message message) {
            NetworkCallback networkCallback;
            if (message.what == ConnectivityManager.EXPIRE_LEGACY_REQUEST) {
                ConnectivityManager.this.expireRequest((NetworkCapabilities) message.obj, message.arg1);
                return;
            }
            NetworkRequest networkRequest = (NetworkRequest) getObject(message, NetworkRequest.class);
            Network network = (Network) getObject(message, Network.class);
            synchronized (ConnectivityManager.sCallbacks) {
                networkCallback = (NetworkCallback) ConnectivityManager.sCallbacks.get(networkRequest);
            }
            if (networkCallback == null) {
                Log.w(TAG, "callback not found for " + ConnectivityManager.getCallbackName(message.what) + " message");
                return;
            }
            switch (message.what) {
                case ConnectivityManager.CALLBACK_PRECHECK:
                    networkCallback.onPreCheck(network);
                    return;
                case 524290:
                    networkCallback.onAvailable(network, (NetworkCapabilities) getObject(message, NetworkCapabilities.class), (LinkProperties) getObject(message, LinkProperties.class));
                    return;
                case 524291:
                    networkCallback.onLosing(network, message.arg1);
                    return;
                case 524292:
                    networkCallback.onLost(network);
                    return;
                case ConnectivityManager.CALLBACK_UNAVAIL:
                    networkCallback.onUnavailable();
                    return;
                case ConnectivityManager.CALLBACK_CAP_CHANGED:
                    networkCallback.onCapabilitiesChanged(network, (NetworkCapabilities) getObject(message, NetworkCapabilities.class));
                    return;
                case ConnectivityManager.CALLBACK_IP_CHANGED:
                    networkCallback.onLinkPropertiesChanged(network, (LinkProperties) getObject(message, LinkProperties.class));
                    return;
                case ConnectivityManager.EXPIRE_LEGACY_REQUEST:
                default:
                    return;
                case ConnectivityManager.CALLBACK_SUSPENDED:
                    networkCallback.onNetworkSuspended(network);
                    return;
                case ConnectivityManager.CALLBACK_RESUMED:
                    networkCallback.onNetworkResumed(network);
                    return;
            }
        }

        private <T> T getObject(Message message, Class<T> cls) {
            return (T) message.getData().getParcelable(cls.getSimpleName());
        }
    }

    private CallbackHandler getDefaultHandler() {
        CallbackHandler callbackHandler;
        synchronized (sCallbacks) {
            if (sCallbackHandler == null) {
                sCallbackHandler = new CallbackHandler(ConnectivityThread.getInstanceLooper());
            }
            callbackHandler = sCallbackHandler;
        }
        return callbackHandler;
    }

    private NetworkRequest sendRequestForNetwork(NetworkCapabilities networkCapabilities, NetworkCallback networkCallback, int i, int i2, int i3, CallbackHandler callbackHandler) {
        NetworkRequest networkRequestRequestNetwork;
        checkCallbackNotNull(networkCallback);
        Preconditions.checkArgument(i2 == 2 || networkCapabilities != null, "null NetworkCapabilities");
        try {
            synchronized (sCallbacks) {
                if (networkCallback.networkRequest != null && networkCallback.networkRequest != ALREADY_UNREGISTERED) {
                    Log.e(TAG, "NetworkCallback was already registered");
                }
                Messenger messenger = new Messenger(callbackHandler);
                Binder binder = new Binder();
                if (i2 == 1) {
                    networkRequestRequestNetwork = this.mService.listenForNetwork(networkCapabilities, messenger, binder);
                } else {
                    networkRequestRequestNetwork = this.mService.requestNetwork(networkCapabilities, messenger, i, binder, i3);
                }
                if (networkRequestRequestNetwork != null) {
                    sCallbacks.put(networkRequestRequestNetwork, networkCallback);
                }
                networkCallback.networkRequest = networkRequestRequestNetwork;
            }
            return networkRequestRequestNetwork;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw convertServiceException(e2);
        }
    }

    public void requestNetwork(NetworkRequest networkRequest, NetworkCallback networkCallback, int i, int i2, Handler handler) {
        sendRequestForNetwork(networkRequest.networkCapabilities, networkCallback, i, 2, i2, new CallbackHandler(this, handler));
    }

    public void requestNetwork(NetworkRequest networkRequest, NetworkCallback networkCallback) {
        requestNetwork(networkRequest, networkCallback, getDefaultHandler());
    }

    public void requestNetwork(NetworkRequest networkRequest, NetworkCallback networkCallback, Handler handler) {
        requestNetwork(networkRequest, networkCallback, 0, inferLegacyTypeForNetworkCapabilities(networkRequest.networkCapabilities), new CallbackHandler(this, handler));
    }

    public void requestNetwork(NetworkRequest networkRequest, NetworkCallback networkCallback, int i) {
        checkTimeout(i);
        requestNetwork(networkRequest, networkCallback, i, inferLegacyTypeForNetworkCapabilities(networkRequest.networkCapabilities), getDefaultHandler());
    }

    public void requestNetwork(NetworkRequest networkRequest, NetworkCallback networkCallback, Handler handler, int i) {
        checkTimeout(i);
        requestNetwork(networkRequest, networkCallback, i, inferLegacyTypeForNetworkCapabilities(networkRequest.networkCapabilities), new CallbackHandler(this, handler));
    }

    public void requestNetwork(NetworkRequest networkRequest, PendingIntent pendingIntent) {
        checkPendingIntentNotNull(pendingIntent);
        try {
            this.mService.pendingRequestForNetwork(networkRequest.networkCapabilities, pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw convertServiceException(e2);
        }
    }

    public void releaseNetworkRequest(PendingIntent pendingIntent) {
        checkPendingIntentNotNull(pendingIntent);
        try {
            this.mService.releasePendingNetworkRequest(pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static void checkPendingIntentNotNull(PendingIntent pendingIntent) {
        Preconditions.checkNotNull(pendingIntent, "PendingIntent cannot be null.");
    }

    private static void checkCallbackNotNull(NetworkCallback networkCallback) {
        Preconditions.checkNotNull(networkCallback, "null NetworkCallback");
    }

    private static void checkTimeout(int i) {
        Preconditions.checkArgumentPositive(i, "timeoutMs must be strictly positive.");
    }

    public void registerNetworkCallback(NetworkRequest networkRequest, NetworkCallback networkCallback) {
        registerNetworkCallback(networkRequest, networkCallback, getDefaultHandler());
    }

    public void registerNetworkCallback(NetworkRequest networkRequest, NetworkCallback networkCallback, Handler handler) {
        sendRequestForNetwork(networkRequest.networkCapabilities, networkCallback, 0, 1, -1, new CallbackHandler(this, handler));
    }

    public void registerNetworkCallback(NetworkRequest networkRequest, PendingIntent pendingIntent) {
        checkPendingIntentNotNull(pendingIntent);
        try {
            this.mService.pendingListenForNetwork(networkRequest.networkCapabilities, pendingIntent);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        } catch (ServiceSpecificException e2) {
            throw convertServiceException(e2);
        }
    }

    public void registerDefaultNetworkCallback(NetworkCallback networkCallback) {
        registerDefaultNetworkCallback(networkCallback, getDefaultHandler());
    }

    public void registerDefaultNetworkCallback(NetworkCallback networkCallback, Handler handler) {
        sendRequestForNetwork(null, networkCallback, 0, 2, -1, new CallbackHandler(this, handler));
    }

    public boolean requestBandwidthUpdate(Network network) {
        try {
            return this.mService.requestBandwidthUpdate(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void unregisterNetworkCallback(NetworkCallback networkCallback) {
        checkCallbackNotNull(networkCallback);
        ArrayList<NetworkRequest> arrayList = new ArrayList();
        synchronized (sCallbacks) {
            Preconditions.checkArgument(networkCallback.networkRequest != null, "NetworkCallback was not registered");
            Preconditions.checkArgument(networkCallback.networkRequest != ALREADY_UNREGISTERED, "NetworkCallback was already unregistered");
            for (Map.Entry<NetworkRequest, NetworkCallback> entry : sCallbacks.entrySet()) {
                if (entry.getValue() == networkCallback) {
                    arrayList.add(entry.getKey());
                }
            }
            for (NetworkRequest networkRequest : arrayList) {
                try {
                    this.mService.releaseNetworkRequest(networkRequest);
                    sCallbacks.remove(networkRequest);
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
            }
            networkCallback.networkRequest = ALREADY_UNREGISTERED;
        }
    }

    public void unregisterNetworkCallback(PendingIntent pendingIntent) {
        checkPendingIntentNotNull(pendingIntent);
        releaseNetworkRequest(pendingIntent);
    }

    public void setAcceptUnvalidated(Network network, boolean z, boolean z2) {
        try {
            this.mService.setAcceptUnvalidated(network, z, z2);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void setAvoidUnvalidated(Network network) {
        try {
            this.mService.setAvoidUnvalidated(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void startCaptivePortalApp(Network network) {
        try {
            this.mService.startCaptivePortalApp(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getMultipathPreference(Network network) {
        try {
            return this.mService.getMultipathPreference(network);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void factoryReset() {
        try {
            this.mService.factoryReset();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean bindProcessToNetwork(Network network) {
        return setProcessDefaultNetwork(network);
    }

    @Deprecated
    public static boolean setProcessDefaultNetwork(Network network) {
        int i;
        if (network != null) {
            i = network.netId;
        } else {
            i = 0;
        }
        if (i == NetworkUtils.getBoundNetworkForProcess()) {
            return true;
        }
        if (!NetworkUtils.bindProcessToNetwork(i)) {
            return false;
        }
        try {
            Proxy.setHttpProxySystemProperty(getInstance().getDefaultProxy());
        } catch (SecurityException e) {
            Log.e(TAG, "Can't set proxy properties", e);
        }
        InetAddress.clearDnsCache();
        NetworkEventDispatcher.getInstance().onNetworkConfigurationChanged();
        return true;
    }

    public Network getBoundNetworkForProcess() {
        return getProcessDefaultNetwork();
    }

    @Deprecated
    public static Network getProcessDefaultNetwork() {
        int boundNetworkForProcess = NetworkUtils.getBoundNetworkForProcess();
        if (boundNetworkForProcess == 0) {
            return null;
        }
        return new Network(boundNetworkForProcess);
    }

    private void unsupportedStartingFrom(int i) {
        if (Process.myUid() != 1000 && this.mContext.getApplicationInfo().targetSdkVersion >= i) {
            throw new UnsupportedOperationException("This method is not supported in target SDK version " + i + " and above");
        }
    }

    private void checkLegacyRoutingApiAccess() {
        if (this.mContext.checkCallingOrSelfPermission("com.android.permission.INJECT_OMADM_SETTINGS") == 0) {
            return;
        }
        unsupportedStartingFrom(23);
    }

    @Deprecated
    public static boolean setProcessDefaultNetworkForHostResolution(Network network) {
        return NetworkUtils.bindProcessToNetworkForHostResolution(network == null ? 0 : network.netId);
    }

    private INetworkPolicyManager getNetworkPolicyManager() {
        synchronized (this) {
            if (this.mNPManager != null) {
                return this.mNPManager;
            }
            this.mNPManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
            return this.mNPManager;
        }
    }

    public int getRestrictBackgroundStatus() {
        try {
            return getNetworkPolicyManager().getRestrictBackgroundByCaller();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public byte[] getNetworkWatchlistConfigHash() {
        try {
            return this.mService.getNetworkWatchlistConfigHash();
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to get watchlist config hash");
            throw e.rethrowFromSystemServer();
        }
    }
}
