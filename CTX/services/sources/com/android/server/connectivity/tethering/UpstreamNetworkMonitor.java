package com.android.server.connectivity.tethering;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IpPrefix;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkState;
import android.net.util.PrefixUtils;
import android.net.util.SharedLog;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.StateMachine;
import com.android.server.UiModeManagerService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class UpstreamNetworkMonitor {
    private static final int CALLBACK_LISTEN_ALL = 1;
    private static final int CALLBACK_MOBILE_REQUEST = 3;
    private static final int CALLBACK_TRACK_DEFAULT = 2;
    private static final boolean DBG = false;
    public static final int EVENT_ON_AVAILABLE = 1;
    public static final int EVENT_ON_CAPABILITIES = 2;
    public static final int EVENT_ON_LINKPROPERTIES = 3;
    public static final int EVENT_ON_LOST = 4;
    public static final int NOTIFY_LOCAL_PREFIXES = 10;
    private static final String TAG = UpstreamNetworkMonitor.class.getSimpleName();
    private static final boolean VDBG = !Build.IS_USER;
    private ConnectivityManager mCM;
    private final Context mContext;
    private Network mDefaultInternetNetwork;
    private ConnectivityManager.NetworkCallback mDefaultNetworkCallback;
    private boolean mDunRequired;
    private final Handler mHandler;
    private ConnectivityManager.NetworkCallback mListenAllCallback;
    private HashSet<IpPrefix> mLocalPrefixes;
    private final SharedLog mLog;
    private ConnectivityManager.NetworkCallback mMobileNetworkCallback;
    private final HashMap<Network, NetworkState> mNetworkMap;
    private final StateMachine mTarget;
    private Network mTetheringUpstreamNetwork;
    private final int mWhat;

    public UpstreamNetworkMonitor(Context context, StateMachine stateMachine, SharedLog sharedLog, int i) {
        this.mNetworkMap = new HashMap<>();
        this.mContext = context;
        this.mTarget = stateMachine;
        this.mHandler = this.mTarget.getHandler();
        this.mLog = sharedLog.forSubComponent(TAG);
        this.mWhat = i;
        this.mLocalPrefixes = new HashSet<>();
    }

    @VisibleForTesting
    public UpstreamNetworkMonitor(ConnectivityManager connectivityManager, StateMachine stateMachine, SharedLog sharedLog, int i) {
        this((Context) null, stateMachine, sharedLog, i);
        this.mCM = connectivityManager;
    }

    public void start() {
        stop();
        NetworkRequest networkRequestBuild = new NetworkRequest.Builder().clearCapabilities().build();
        this.mListenAllCallback = new UpstreamNetworkCallback(1);
        cm().registerNetworkCallback(networkRequestBuild, this.mListenAllCallback, this.mHandler);
        this.mDefaultNetworkCallback = new UpstreamNetworkCallback(2);
        cm().registerDefaultNetworkCallback(this.mDefaultNetworkCallback, this.mHandler);
    }

    public void stop() {
        releaseMobileNetworkRequest();
        releaseCallback(this.mDefaultNetworkCallback);
        this.mDefaultNetworkCallback = null;
        this.mDefaultInternetNetwork = null;
        releaseCallback(this.mListenAllCallback);
        this.mListenAllCallback = null;
        this.mTetheringUpstreamNetwork = null;
        this.mNetworkMap.clear();
    }

    public void updateMobileRequiresDun(boolean z) {
        boolean z2 = this.mDunRequired != z;
        this.mDunRequired = z;
        if (z2 && mobileNetworkRequested()) {
            releaseMobileNetworkRequest();
            registerMobileNetworkRequest();
        }
    }

    public boolean mobileNetworkRequested() {
        return this.mMobileNetworkCallback != null;
    }

    public void registerMobileNetworkRequest() {
        if (this.mMobileNetworkCallback != null) {
            this.mLog.e("registerMobileNetworkRequest() already registered");
            return;
        }
        int i = this.mDunRequired ? 4 : 5;
        NetworkRequest networkRequestBuild = new NetworkRequest.Builder().setCapabilities(ConnectivityManager.networkCapabilitiesForType(i)).build();
        this.mMobileNetworkCallback = new UpstreamNetworkCallback(3);
        this.mLog.i("requesting mobile upstream network: " + networkRequestBuild);
        cm().requestNetwork(networkRequestBuild, this.mMobileNetworkCallback, 0, i, this.mHandler);
    }

    public void releaseMobileNetworkRequest() {
        if (this.mMobileNetworkCallback == null) {
            return;
        }
        cm().unregisterNetworkCallback(this.mMobileNetworkCallback);
        this.mMobileNetworkCallback = null;
    }

    public NetworkState selectPreferredUpstreamType(Iterable<Integer> iterable) {
        TypeStatePair typeStatePairFindFirstAvailableUpstreamByType = findFirstAvailableUpstreamByType(this.mNetworkMap.values(), iterable);
        this.mLog.log("preferred upstream type: " + ConnectivityManager.getNetworkTypeName(typeStatePairFindFirstAvailableUpstreamByType.type));
        int i = typeStatePairFindFirstAvailableUpstreamByType.type;
        if (i != -1) {
            switch (i) {
                case 4:
                case 5:
                    registerMobileNetworkRequest();
                    break;
                default:
                    releaseMobileNetworkRequest();
                    break;
            }
        }
        return typeStatePairFindFirstAvailableUpstreamByType.ns;
    }

    public void setCurrentUpstream(Network network) {
        this.mTetheringUpstreamNetwork = network;
    }

    public Set<IpPrefix> getLocalPrefixes() {
        return (Set) this.mLocalPrefixes.clone();
    }

    private void handleAvailable(int i, Network network) {
        if (VDBG) {
            Log.d(TAG, "EVENT_ON_AVAILABLE for " + network);
        }
        if (!this.mNetworkMap.containsKey(network)) {
            this.mNetworkMap.put(network, new NetworkState((NetworkInfo) null, (LinkProperties) null, (NetworkCapabilities) null, network, (String) null, (String) null));
        }
        switch (i) {
            case 2:
                if (this.mDefaultNetworkCallback == null) {
                    return;
                }
                this.mDefaultInternetNetwork = network;
                break;
                break;
            case 3:
                if (this.mMobileNetworkCallback == null) {
                    return;
                }
                break;
        }
        notifyTarget(1, network);
    }

    private void handleNetCap(Network network, NetworkCapabilities networkCapabilities) {
        NetworkState networkState = this.mNetworkMap.get(network);
        if (networkState == null || networkCapabilities.equals(networkState.networkCapabilities)) {
            return;
        }
        if (VDBG) {
            Log.d(TAG, String.format("EVENT_ON_CAPABILITIES for %s: %s", network, networkCapabilities));
        }
        if (network.equals(this.mTetheringUpstreamNetwork) && networkCapabilities.hasSignalStrength()) {
            int signalStrength = networkCapabilities.getSignalStrength();
            this.mLog.logf("upstream network signal strength: %s -> %s", getSignalStrength(networkState.networkCapabilities), Integer.valueOf(signalStrength));
        }
        this.mNetworkMap.put(network, new NetworkState((NetworkInfo) null, networkState.linkProperties, networkCapabilities, network, (String) null, (String) null));
        notifyTarget(2, network);
    }

    private void handleLinkProp(Network network, LinkProperties linkProperties) {
        NetworkState networkState = this.mNetworkMap.get(network);
        if (networkState == null || linkProperties.equals(networkState.linkProperties)) {
            return;
        }
        if (VDBG) {
            Log.d(TAG, String.format("EVENT_ON_LINKPROPERTIES for %s: %s", network, linkProperties));
        }
        this.mNetworkMap.put(network, new NetworkState((NetworkInfo) null, linkProperties, networkState.networkCapabilities, network, (String) null, (String) null));
        notifyTarget(3, network);
    }

    private void handleSuspended(int i, Network network) {
        if (i == 1 && network.equals(this.mTetheringUpstreamNetwork)) {
            this.mLog.log("SUSPENDED current upstream: " + network);
        }
    }

    private void handleResumed(int i, Network network) {
        if (i == 1 && network.equals(this.mTetheringUpstreamNetwork)) {
            this.mLog.log("RESUMED current upstream: " + network);
        }
    }

    private void handleLost(int i, Network network) {
        if (i == 2) {
            this.mDefaultInternetNetwork = null;
            return;
        }
        if (!this.mNetworkMap.containsKey(network)) {
            return;
        }
        if (VDBG) {
            Log.d(TAG, "EVENT_ON_LOST for " + network);
        }
        notifyTarget(4, this.mNetworkMap.remove(network));
    }

    private void recomputeLocalPrefixes() {
        HashSet<IpPrefix> hashSetAllLocalPrefixes = allLocalPrefixes(this.mNetworkMap.values());
        if (!this.mLocalPrefixes.equals(hashSetAllLocalPrefixes)) {
            this.mLocalPrefixes = hashSetAllLocalPrefixes;
            notifyTarget(10, hashSetAllLocalPrefixes.clone());
        }
    }

    private ConnectivityManager cm() {
        if (this.mCM == null) {
            this.mCM = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mCM;
    }

    private class UpstreamNetworkCallback extends ConnectivityManager.NetworkCallback {
        private final int mCallbackType;

        UpstreamNetworkCallback(int i) {
            this.mCallbackType = i;
        }

        @Override
        public void onAvailable(Network network) {
            UpstreamNetworkMonitor.this.handleAvailable(this.mCallbackType, network);
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            UpstreamNetworkMonitor.this.handleNetCap(network, networkCapabilities);
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            UpstreamNetworkMonitor.this.handleLinkProp(network, linkProperties);
            UpstreamNetworkMonitor.this.recomputeLocalPrefixes();
        }

        public void onNetworkSuspended(Network network) {
            UpstreamNetworkMonitor.this.handleSuspended(this.mCallbackType, network);
        }

        public void onNetworkResumed(Network network) {
            UpstreamNetworkMonitor.this.handleResumed(this.mCallbackType, network);
        }

        @Override
        public void onLost(Network network) {
            UpstreamNetworkMonitor.this.handleLost(this.mCallbackType, network);
            UpstreamNetworkMonitor.this.recomputeLocalPrefixes();
        }
    }

    private void releaseCallback(ConnectivityManager.NetworkCallback networkCallback) {
        if (networkCallback != null) {
            cm().unregisterNetworkCallback(networkCallback);
        }
    }

    private void notifyTarget(int i, Network network) {
        notifyTarget(i, this.mNetworkMap.get(network));
    }

    private void notifyTarget(int i, Object obj) {
        this.mTarget.sendMessage(this.mWhat, i, 0, obj);
    }

    private static class TypeStatePair {
        public NetworkState ns;
        public int type;

        private TypeStatePair() {
            this.type = -1;
            this.ns = null;
        }
    }

    private static TypeStatePair findFirstAvailableUpstreamByType(Iterable<NetworkState> iterable, Iterable<Integer> iterable2) {
        TypeStatePair typeStatePair = new TypeStatePair();
        Iterator<Integer> it = iterable2.iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            try {
                NetworkCapabilities networkCapabilitiesNetworkCapabilitiesForType = ConnectivityManager.networkCapabilitiesForType(iIntValue);
                networkCapabilitiesNetworkCapabilitiesForType.setSingleUid(Process.myUid());
                for (NetworkState networkState : iterable) {
                    if (networkCapabilitiesNetworkCapabilitiesForType.satisfiedByNetworkCapabilities(networkState.networkCapabilities)) {
                        typeStatePair.type = iIntValue;
                        typeStatePair.ns = networkState;
                        return typeStatePair;
                    }
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "No NetworkCapabilities mapping for legacy type: " + ConnectivityManager.getNetworkTypeName(iIntValue));
            }
        }
        return typeStatePair;
    }

    private static HashSet<IpPrefix> allLocalPrefixes(Iterable<NetworkState> iterable) {
        HashSet<IpPrefix> hashSet = new HashSet<>();
        Iterator<NetworkState> it = iterable.iterator();
        while (it.hasNext()) {
            LinkProperties linkProperties = it.next().linkProperties;
            if (linkProperties != null) {
                hashSet.addAll(PrefixUtils.localPrefixesFrom(linkProperties));
            }
        }
        return hashSet;
    }

    private static String getSignalStrength(NetworkCapabilities networkCapabilities) {
        if (networkCapabilities == null || !networkCapabilities.hasSignalStrength()) {
            return UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
        }
        return Integer.toString(networkCapabilities.getSignalStrength());
    }
}
