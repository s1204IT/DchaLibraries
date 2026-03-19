package com.android.server.wifi.aware;

import android.content.Context;
import android.hardware.wifi.V1_2.NanDataPathChannelInfo;
import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.MacAddress;
import android.net.MatchAllNetworkSpecifier;
import android.net.NetworkAgent;
import android.net.NetworkCapabilities;
import android.net.NetworkFactory;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.RouteInfo;
import android.net.wifi.aware.WifiAwareAgentNetworkSpecifier;
import android.net.wifi.aware.WifiAwareNetworkSpecifier;
import android.net.wifi.aware.WifiAwareUtils;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.aware.WifiAwareDiscoverySessionState;
import com.android.server.wifi.util.WifiPermissionsUtil;
import com.android.server.wifi.util.WifiPermissionsWrapper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import libcore.util.HexEncoding;

public class WifiAwareDataPathStateManager {
    private static final String AGENT_TAG_PREFIX = "WIFI_AWARE_AGENT_";
    private static final String AWARE_INTERFACE_PREFIX = "aware_data";
    private static final int NETWORK_FACTORY_BANDWIDTH_AVAIL = 1;
    private static final int NETWORK_FACTORY_SCORE_AVAIL = 1;
    private static final int NETWORK_FACTORY_SIGNAL_STRENGTH_AVAIL = 1;
    private static final String NETWORK_TAG = "WIFI_AWARE_FACTORY";
    private static final String TAG = "WifiAwareDataPathStMgr";
    private static final boolean VDBG = false;
    private static final NetworkCapabilities sNetworkCapabilitiesFilter = new NetworkCapabilities();
    private WifiAwareMetrics mAwareMetrics;
    private Context mContext;
    private Looper mLooper;
    private final WifiAwareStateManager mMgr;
    private WifiAwareNetworkFactory mNetworkFactory;
    public INetworkManagementService mNwService;
    private WifiPermissionsWrapper mPermissionsWrapper;
    private WifiPermissionsUtil mWifiPermissionsUtil;
    boolean mDbg = false;
    public NetworkInterfaceWrapper mNiWrapper = new NetworkInterfaceWrapper();
    private final Set<String> mInterfaces = new HashSet();
    private final Map<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> mNetworkRequestsCache = new ArrayMap();
    boolean mAllowNdpResponderFromAnyOverride = false;

    public WifiAwareDataPathStateManager(WifiAwareStateManager wifiAwareStateManager) {
        this.mMgr = wifiAwareStateManager;
    }

    public void start(Context context, Looper looper, WifiAwareMetrics wifiAwareMetrics, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper wifiPermissionsWrapper) {
        this.mContext = context;
        this.mAwareMetrics = wifiAwareMetrics;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
        this.mPermissionsWrapper = wifiPermissionsWrapper;
        this.mLooper = looper;
        sNetworkCapabilitiesFilter.clearAll();
        sNetworkCapabilitiesFilter.addTransportType(5);
        sNetworkCapabilitiesFilter.addCapability(15).addCapability(11).addCapability(18).addCapability(20).addCapability(13).addCapability(14);
        sNetworkCapabilitiesFilter.setNetworkSpecifier(new MatchAllNetworkSpecifier());
        sNetworkCapabilitiesFilter.setLinkUpstreamBandwidthKbps(1);
        sNetworkCapabilitiesFilter.setLinkDownstreamBandwidthKbps(1);
        sNetworkCapabilitiesFilter.setSignalStrength(1);
        this.mNetworkFactory = new WifiAwareNetworkFactory(looper, context, sNetworkCapabilitiesFilter);
        this.mNetworkFactory.setScoreFilter(1);
        this.mNetworkFactory.register();
        this.mNwService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
    }

    private Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> getNetworkRequestByNdpId(int i) {
        for (Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if (entry.getValue().ndpId == i) {
                return entry;
            }
        }
        return null;
    }

    private Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> getNetworkRequestByCanonicalDescriptor(CanonicalConnectionInfo canonicalConnectionInfo) {
        for (Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> entry : this.mNetworkRequestsCache.entrySet()) {
            if (entry.getValue().getCanonicalDescriptor().matches(canonicalConnectionInfo)) {
                return entry;
            }
        }
        return null;
    }

    public void createAllInterfaces() {
        if (this.mMgr.getCapabilities() == null) {
            Log.e(TAG, "createAllInterfaces: capabilities aren't initialized yet!");
            return;
        }
        for (int i = 0; i < this.mMgr.getCapabilities().maxNdiInterfaces; i++) {
            String str = AWARE_INTERFACE_PREFIX + i;
            if (this.mInterfaces.contains(str)) {
                Log.e(TAG, "createAllInterfaces(): interface already up, " + str + ", possibly failed to delete - deleting/creating again to be safe");
                this.mMgr.deleteDataPathInterface(str);
                this.mInterfaces.remove(str);
            }
            this.mMgr.createDataPathInterface(str);
        }
    }

    public void deleteAllInterfaces() {
        onAwareDownCleanupDataPaths();
        if (this.mMgr.getCapabilities() == null) {
            Log.e(TAG, "deleteAllInterfaces: capabilities aren't initialized yet!");
            return;
        }
        for (int i = 0; i < this.mMgr.getCapabilities().maxNdiInterfaces; i++) {
            this.mMgr.deleteDataPathInterface(AWARE_INTERFACE_PREFIX + i);
        }
        this.mMgr.releaseAwareInterface();
    }

    public void onInterfaceCreated(String str) {
        if (this.mInterfaces.contains(str)) {
            Log.w(TAG, "onInterfaceCreated: already contains interface -- " + str);
        }
        this.mInterfaces.add(str);
    }

    public void onInterfaceDeleted(String str) {
        if (!this.mInterfaces.contains(str)) {
            Log.w(TAG, "onInterfaceDeleted: interface not on list -- " + str);
        }
        this.mInterfaces.remove(str);
    }

    public void onDataPathInitiateSuccess(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier, int i) {
        AwareNetworkRequestInformation awareNetworkRequestInformation = this.mNetworkRequestsCache.get(wifiAwareNetworkSpecifier);
        if (awareNetworkRequestInformation == null) {
            Log.w(TAG, "onDataPathInitiateSuccess: network request not found for networkSpecifier=" + wifiAwareNetworkSpecifier);
            this.mMgr.endDataPath(i);
            return;
        }
        if (awareNetworkRequestInformation.state != 103) {
            Log.w(TAG, "onDataPathInitiateSuccess: network request in incorrect state: state=" + awareNetworkRequestInformation.state);
            this.mNetworkRequestsCache.remove(wifiAwareNetworkSpecifier);
            this.mMgr.endDataPath(i);
            return;
        }
        awareNetworkRequestInformation.state = ISupplicantStaIfaceCallback.StatusCode.MAF_LIMIT_EXCEEDED;
        awareNetworkRequestInformation.ndpId = i;
    }

    public void onDataPathInitiateFail(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier, int i) {
        AwareNetworkRequestInformation awareNetworkRequestInformationRemove = this.mNetworkRequestsCache.remove(wifiAwareNetworkSpecifier);
        if (awareNetworkRequestInformationRemove == null) {
            Log.w(TAG, "onDataPathInitiateFail: network request not found for networkSpecifier=" + wifiAwareNetworkSpecifier);
            return;
        }
        if (awareNetworkRequestInformationRemove.state != 103) {
            Log.w(TAG, "onDataPathInitiateFail: network request in incorrect state: state=" + awareNetworkRequestInformationRemove.state);
        }
        this.mAwareMetrics.recordNdpStatus(i, wifiAwareNetworkSpecifier.isOutOfBand(), awareNetworkRequestInformationRemove.startTimestamp);
    }

    public WifiAwareNetworkSpecifier onDataPathRequest(int i, byte[] bArr, int i2) {
        WifiAwareNetworkSpecifier key;
        AwareNetworkRequestInformation value;
        Iterator<Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation>> it = this.mNetworkRequestsCache.entrySet().iterator();
        while (true) {
            if (it.hasNext()) {
                Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> next = it.next();
                if (next.getValue().pubSubId == 0 || next.getValue().pubSubId == i) {
                    if (next.getValue().peerDiscoveryMac == null || Arrays.equals(next.getValue().peerDiscoveryMac, bArr)) {
                        if (next.getValue().state == 104) {
                            key = next.getKey();
                            value = next.getValue();
                            break;
                        }
                    }
                }
            } else {
                key = null;
                value = null;
                break;
            }
        }
        if (value == null) {
            Log.w(TAG, "onDataPathRequest: can't find a request with specified pubSubId=" + i + ", mac=" + String.valueOf(HexEncoding.encode(bArr)));
            this.mMgr.respondToDataPathRequest(false, i2, "", null, null, false);
            return null;
        }
        if (value.peerDiscoveryMac == null) {
            value.peerDiscoveryMac = bArr;
        }
        value.interfaceName = selectInterfaceForRequest(value);
        if (value.interfaceName == null) {
            Log.w(TAG, "onDataPathRequest: request " + key + " no interface available");
            this.mMgr.respondToDataPathRequest(false, i2, "", null, null, false);
            this.mNetworkRequestsCache.remove(key);
            return null;
        }
        value.state = ISupplicantStaIfaceCallback.StatusCode.ENABLEMENT_DENIED;
        value.ndpId = i2;
        value.startTimestamp = SystemClock.elapsedRealtime();
        this.mMgr.respondToDataPathRequest(true, i2, value.interfaceName, value.networkSpecifier.pmk, value.networkSpecifier.passphrase, value.networkSpecifier.isOutOfBand());
        return key;
    }

    public void onRespondToDataPathRequest(int i, boolean z, int i2) {
        WifiAwareNetworkSpecifier key;
        AwareNetworkRequestInformation value;
        Iterator<Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation>> it = this.mNetworkRequestsCache.entrySet().iterator();
        while (true) {
            key = null;
            if (it.hasNext()) {
                Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> next = it.next();
                if (next.getValue().ndpId == i) {
                    key = next.getKey();
                    value = next.getValue();
                    break;
                }
            } else {
                value = null;
                break;
            }
        }
        if (value == null) {
            Log.w(TAG, "onRespondToDataPathRequest: can't find a request with specified ndpId=" + i);
            return;
        }
        if (!z) {
            Log.w(TAG, "onRespondToDataPathRequest: request " + key + " failed responding");
            this.mMgr.endDataPath(i);
            this.mNetworkRequestsCache.remove(key);
            this.mAwareMetrics.recordNdpStatus(i2, key.isOutOfBand(), value.startTimestamp);
            return;
        }
        if (value.state != 105) {
            Log.w(TAG, "onRespondToDataPathRequest: request " + key + " is incorrect state=" + value.state);
            this.mMgr.endDataPath(i);
            this.mNetworkRequestsCache.remove(key);
            return;
        }
        value.state = ISupplicantStaIfaceCallback.StatusCode.MAF_LIMIT_EXCEEDED;
    }

    public WifiAwareNetworkSpecifier onDataPathConfirm(int i, byte[] bArr, boolean z, int i2, byte[] bArr2, List<NanDataPathChannelInfo> list) {
        Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> networkRequestByNdpId = getNetworkRequestByNdpId(i);
        if (networkRequestByNdpId == null) {
            Log.w(TAG, "onDataPathConfirm: network request not found for ndpId=" + i);
            if (z) {
                this.mMgr.endDataPath(i);
                return null;
            }
            return null;
        }
        WifiAwareNetworkSpecifier key = networkRequestByNdpId.getKey();
        AwareNetworkRequestInformation value = networkRequestByNdpId.getValue();
        if (value.state != 101) {
            Log.w(TAG, "onDataPathConfirm: invalid state=" + value.state);
            this.mNetworkRequestsCache.remove(key);
            if (z) {
                this.mMgr.endDataPath(i);
            }
            return key;
        }
        if (z) {
            value.state = ISupplicantStaIfaceCallback.StatusCode.MCCA_TRACK_LIMIT_EXCEEDED;
            value.peerDataMac = bArr;
            value.channelInfo = list;
            NetworkInfo networkInfo = new NetworkInfo(-1, 0, NETWORK_TAG, "");
            NetworkCapabilities networkCapabilities = new NetworkCapabilities(sNetworkCapabilitiesFilter);
            LinkProperties linkProperties = new LinkProperties();
            if (!isInterfaceUpAndUsedByAnotherNdp(value)) {
                try {
                    this.mNwService.setInterfaceUp(value.interfaceName);
                    this.mNwService.enableIpv6(value.interfaceName);
                } catch (Exception e) {
                    Log.e(TAG, "onDataPathConfirm: ACCEPT nnri=" + value + ": can't configure network - " + e);
                    this.mMgr.endDataPath(i);
                    value.state = ISupplicantStaIfaceCallback.StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                    return key;
                }
            }
            if (!this.mNiWrapper.configureAgentProperties(value, value.equivalentSpecifiers, i, networkInfo, networkCapabilities, linkProperties)) {
                return key;
            }
            value.networkAgent = new WifiAwareNetworkAgent(this.mLooper, this.mContext, AGENT_TAG_PREFIX + value.ndpId, new NetworkInfo(-1, 0, NETWORK_TAG, ""), networkCapabilities, linkProperties, 1, value);
            value.networkAgent.sendNetworkInfo(networkInfo);
            this.mAwareMetrics.recordNdpStatus(0, key.isOutOfBand(), value.startTimestamp);
            value.startTimestamp = SystemClock.elapsedRealtime();
            this.mAwareMetrics.recordNdpCreation(value.uid, this.mNetworkRequestsCache);
        } else {
            this.mNetworkRequestsCache.remove(key);
            this.mAwareMetrics.recordNdpStatus(i2, key.isOutOfBand(), value.startTimestamp);
        }
        return key;
    }

    public void onDataPathEnd(int i) {
        Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> networkRequestByNdpId = getNetworkRequestByNdpId(i);
        if (networkRequestByNdpId == null) {
            return;
        }
        tearDownInterfaceIfPossible(networkRequestByNdpId.getValue());
        if (networkRequestByNdpId.getValue().state == 102 || networkRequestByNdpId.getValue().state == 106) {
            this.mAwareMetrics.recordNdpSessionDuration(networkRequestByNdpId.getValue().startTimestamp);
        }
        this.mNetworkRequestsCache.remove(networkRequestByNdpId.getKey());
        this.mNetworkFactory.tickleConnectivityIfWaiting();
    }

    public void onDataPathSchedUpdate(byte[] bArr, List<Integer> list, List<NanDataPathChannelInfo> list2) {
        Iterator<Integer> it = list.iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation> networkRequestByNdpId = getNetworkRequestByNdpId(iIntValue);
            if (networkRequestByNdpId == null) {
                Log.e(TAG, "onDataPathSchedUpdate: ndpId=" + iIntValue + " - not found");
            } else if (!Arrays.equals(bArr, networkRequestByNdpId.getValue().peerDiscoveryMac)) {
                Log.e(TAG, "onDataPathSchedUpdate: ndpId=" + iIntValue + ", report NMI=" + MacAddress.fromBytes(bArr).toString() + " doesn't match NDP NMI=" + MacAddress.fromBytes(networkRequestByNdpId.getValue().peerDiscoveryMac).toString());
            } else {
                networkRequestByNdpId.getValue().channelInfo = list2;
            }
        }
    }

    public void onAwareDownCleanupDataPaths() {
        Iterator<Map.Entry<WifiAwareNetworkSpecifier, AwareNetworkRequestInformation>> it = this.mNetworkRequestsCache.entrySet().iterator();
        while (it.hasNext()) {
            tearDownInterfaceIfPossible(it.next().getValue());
            it.remove();
        }
    }

    public void handleDataPathTimeout(NetworkSpecifier networkSpecifier) {
        if (this.mDbg) {
            Log.v(TAG, "handleDataPathTimeout: networkSpecifier=" + networkSpecifier);
        }
        AwareNetworkRequestInformation awareNetworkRequestInformationRemove = this.mNetworkRequestsCache.remove(networkSpecifier);
        if (awareNetworkRequestInformationRemove == null) {
            if (this.mDbg) {
                Log.v(TAG, "handleDataPathTimeout: network request not found for networkSpecifier=" + networkSpecifier);
                return;
            }
            return;
        }
        this.mAwareMetrics.recordNdpStatus(1, awareNetworkRequestInformationRemove.networkSpecifier.isOutOfBand(), awareNetworkRequestInformationRemove.startTimestamp);
        this.mMgr.endDataPath(awareNetworkRequestInformationRemove.ndpId);
        awareNetworkRequestInformationRemove.state = ISupplicantStaIfaceCallback.StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
    }

    private class WifiAwareNetworkFactory extends NetworkFactory {
        private boolean mWaitingForTermination;

        WifiAwareNetworkFactory(Looper looper, Context context, NetworkCapabilities networkCapabilities) {
            super(looper, context, WifiAwareDataPathStateManager.NETWORK_TAG, networkCapabilities);
            this.mWaitingForTermination = false;
        }

        public void tickleConnectivityIfWaiting() {
            if (this.mWaitingForTermination) {
                this.mWaitingForTermination = false;
                reevaluateAllRequests();
            }
        }

        public boolean acceptRequest(NetworkRequest networkRequest, int i) {
            if (!WifiAwareDataPathStateManager.this.mMgr.isUsageEnabled()) {
                return false;
            }
            if (WifiAwareDataPathStateManager.this.mInterfaces.isEmpty()) {
                Log.w(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + " -- No Aware interfaces are up");
                return false;
            }
            NetworkSpecifier networkSpecifier = networkRequest.networkCapabilities.getNetworkSpecifier();
            if (!(networkSpecifier instanceof WifiAwareNetworkSpecifier)) {
                Log.w(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + " - not a WifiAwareNetworkSpecifier");
                return false;
            }
            WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifier;
            AwareNetworkRequestInformation awareNetworkRequestInformation = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(wifiAwareNetworkSpecifier);
            if (awareNetworkRequestInformation != null) {
                if (awareNetworkRequestInformation.state != 106) {
                    return true;
                }
                this.mWaitingForTermination = true;
                return false;
            }
            AwareNetworkRequestInformation awareNetworkRequestInformationProcessNetworkSpecifier = AwareNetworkRequestInformation.processNetworkSpecifier(wifiAwareNetworkSpecifier, WifiAwareDataPathStateManager.this.mMgr, WifiAwareDataPathStateManager.this.mWifiPermissionsUtil, WifiAwareDataPathStateManager.this.mPermissionsWrapper, WifiAwareDataPathStateManager.this.mAllowNdpResponderFromAnyOverride);
            if (awareNetworkRequestInformationProcessNetworkSpecifier != null) {
                Map.Entry networkRequestByCanonicalDescriptor = WifiAwareDataPathStateManager.this.getNetworkRequestByCanonicalDescriptor(awareNetworkRequestInformationProcessNetworkSpecifier.getCanonicalDescriptor());
                if (networkRequestByCanonicalDescriptor == null) {
                    WifiAwareDataPathStateManager.this.mNetworkRequestsCache.put(wifiAwareNetworkSpecifier, awareNetworkRequestInformationProcessNetworkSpecifier);
                    return true;
                }
                if (((AwareNetworkRequestInformation) networkRequestByCanonicalDescriptor.getValue()).state == 106) {
                    this.mWaitingForTermination = true;
                } else {
                    ((AwareNetworkRequestInformation) networkRequestByCanonicalDescriptor.getValue()).updateToSupportNewRequest(wifiAwareNetworkSpecifier);
                }
                return false;
            }
            Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.acceptRequest: request=" + networkRequest + " - can't parse network specifier");
            return false;
        }

        protected void needNetworkFor(NetworkRequest networkRequest, int i) {
            WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier;
            NetworkSpecifier networkSpecifier = networkRequest.networkCapabilities.getNetworkSpecifier();
            if (networkSpecifier instanceof WifiAwareNetworkSpecifier) {
                wifiAwareNetworkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifier;
            } else {
                wifiAwareNetworkSpecifier = null;
            }
            WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier2 = wifiAwareNetworkSpecifier;
            AwareNetworkRequestInformation awareNetworkRequestInformation = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(wifiAwareNetworkSpecifier2);
            if (awareNetworkRequestInformation == null) {
                Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.needNetworkFor: networkRequest=" + networkRequest + " not in cache!?");
                return;
            }
            if (awareNetworkRequestInformation.state != 100) {
                return;
            }
            if (awareNetworkRequestInformation.networkSpecifier.role == 0) {
                awareNetworkRequestInformation.interfaceName = WifiAwareDataPathStateManager.this.selectInterfaceForRequest(awareNetworkRequestInformation);
                if (awareNetworkRequestInformation.interfaceName == null) {
                    Log.w(WifiAwareDataPathStateManager.TAG, "needNetworkFor: request " + wifiAwareNetworkSpecifier2 + " no interface available");
                    WifiAwareDataPathStateManager.this.mNetworkRequestsCache.remove(wifiAwareNetworkSpecifier2);
                    return;
                }
                WifiAwareDataPathStateManager.this.mMgr.initiateDataPathSetup(wifiAwareNetworkSpecifier2, awareNetworkRequestInformation.peerInstanceId, 0, WifiAwareDataPathStateManager.this.selectChannelForRequest(awareNetworkRequestInformation), awareNetworkRequestInformation.peerDiscoveryMac, awareNetworkRequestInformation.interfaceName, awareNetworkRequestInformation.networkSpecifier.pmk, awareNetworkRequestInformation.networkSpecifier.passphrase, awareNetworkRequestInformation.networkSpecifier.isOutOfBand());
                awareNetworkRequestInformation.state = ISupplicantStaIfaceCallback.StatusCode.DENIED_DUE_TO_SPECTRUM_MANAGEMENT;
                awareNetworkRequestInformation.startTimestamp = SystemClock.elapsedRealtime();
                return;
            }
            awareNetworkRequestInformation.state = ISupplicantStaIfaceCallback.StatusCode.ASSOC_DENIED_NO_VHT;
        }

        protected void releaseNetworkFor(NetworkRequest networkRequest) {
            WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier;
            NetworkSpecifier networkSpecifier = networkRequest.networkCapabilities.getNetworkSpecifier();
            if (networkSpecifier instanceof WifiAwareNetworkSpecifier) {
                wifiAwareNetworkSpecifier = (WifiAwareNetworkSpecifier) networkSpecifier;
            } else {
                wifiAwareNetworkSpecifier = null;
            }
            AwareNetworkRequestInformation awareNetworkRequestInformation = (AwareNetworkRequestInformation) WifiAwareDataPathStateManager.this.mNetworkRequestsCache.get(wifiAwareNetworkSpecifier);
            if (awareNetworkRequestInformation == null) {
                Log.e(WifiAwareDataPathStateManager.TAG, "WifiAwareNetworkFactory.releaseNetworkFor: networkRequest=" + networkRequest + " not in cache!?");
                return;
            }
            if (awareNetworkRequestInformation.networkAgent != null) {
                return;
            }
            awareNetworkRequestInformation.removeSupportForRequest(wifiAwareNetworkSpecifier);
            if (awareNetworkRequestInformation.equivalentSpecifiers.isEmpty()) {
                if (awareNetworkRequestInformation.ndpId != 0) {
                    WifiAwareDataPathStateManager.this.mMgr.endDataPath(awareNetworkRequestInformation.ndpId);
                    awareNetworkRequestInformation.state = ISupplicantStaIfaceCallback.StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                } else {
                    WifiAwareDataPathStateManager.this.mNetworkRequestsCache.remove(wifiAwareNetworkSpecifier);
                }
            }
        }
    }

    private class WifiAwareNetworkAgent extends NetworkAgent {
        private AwareNetworkRequestInformation mAwareNetworkRequestInfo;
        private NetworkInfo mNetworkInfo;

        WifiAwareNetworkAgent(Looper looper, Context context, String str, NetworkInfo networkInfo, NetworkCapabilities networkCapabilities, LinkProperties linkProperties, int i, AwareNetworkRequestInformation awareNetworkRequestInformation) {
            super(looper, context, str, networkInfo, networkCapabilities, linkProperties, i);
            this.mNetworkInfo = networkInfo;
            this.mAwareNetworkRequestInfo = awareNetworkRequestInformation;
        }

        protected void unwanted() {
            WifiAwareDataPathStateManager.this.mMgr.endDataPath(this.mAwareNetworkRequestInfo.ndpId);
            this.mAwareNetworkRequestInfo.state = ISupplicantStaIfaceCallback.StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
        }

        void reconfigureAgentAsDisconnected() {
            this.mNetworkInfo.setDetailedState(NetworkInfo.DetailedState.DISCONNECTED, null, "");
            sendNetworkInfo(this.mNetworkInfo);
        }
    }

    private void tearDownInterfaceIfPossible(AwareNetworkRequestInformation awareNetworkRequestInformation) {
        if (!TextUtils.isEmpty(awareNetworkRequestInformation.interfaceName) && !isInterfaceUpAndUsedByAnotherNdp(awareNetworkRequestInformation)) {
            try {
                this.mNwService.setInterfaceDown(awareNetworkRequestInformation.interfaceName);
            } catch (Exception e) {
                Log.e(TAG, "tearDownInterfaceIfPossible: nnri=" + awareNetworkRequestInformation + ": can't bring interface down - " + e);
            }
        }
        if (awareNetworkRequestInformation.networkAgent != null) {
            awareNetworkRequestInformation.networkAgent.reconfigureAgentAsDisconnected();
        }
    }

    private boolean isInterfaceUpAndUsedByAnotherNdp(AwareNetworkRequestInformation awareNetworkRequestInformation) {
        for (AwareNetworkRequestInformation awareNetworkRequestInformation2 : this.mNetworkRequestsCache.values()) {
            if (awareNetworkRequestInformation2 != awareNetworkRequestInformation && awareNetworkRequestInformation.interfaceName.equals(awareNetworkRequestInformation2.interfaceName) && (awareNetworkRequestInformation2.state == 102 || awareNetworkRequestInformation2.state == 106)) {
                return true;
            }
        }
        return false;
    }

    private String selectInterfaceForRequest(AwareNetworkRequestInformation awareNetworkRequestInformation) {
        TreeSet<String> treeSet = new TreeSet(this.mInterfaces);
        HashSet hashSet = new HashSet();
        for (AwareNetworkRequestInformation awareNetworkRequestInformation2 : this.mNetworkRequestsCache.values()) {
            if (awareNetworkRequestInformation2 != awareNetworkRequestInformation && Arrays.equals(awareNetworkRequestInformation.peerDiscoveryMac, awareNetworkRequestInformation2.peerDiscoveryMac)) {
                hashSet.add(awareNetworkRequestInformation2.interfaceName);
            }
        }
        for (String str : treeSet) {
            if (!hashSet.contains(str)) {
                return str;
            }
        }
        Log.e(TAG, "selectInterfaceForRequest: req=" + awareNetworkRequestInformation + " - no interfaces available!");
        return null;
    }

    private int selectChannelForRequest(AwareNetworkRequestInformation awareNetworkRequestInformation) {
        return 2437;
    }

    @VisibleForTesting
    public static class AwareNetworkRequestInformation {
        static final int STATE_CONFIRMED = 102;
        static final int STATE_IDLE = 100;
        static final int STATE_INITIATOR_WAIT_FOR_REQUEST_RESPONSE = 103;
        static final int STATE_RESPONDER_WAIT_FOR_REQUEST = 104;
        static final int STATE_RESPONDER_WAIT_FOR_RESPOND_RESPONSE = 105;
        static final int STATE_TERMINATING = 106;
        static final int STATE_WAIT_FOR_CONFIRM = 101;
        public List<NanDataPathChannelInfo> channelInfo;
        public String interfaceName;
        public WifiAwareNetworkAgent networkAgent;
        public WifiAwareNetworkSpecifier networkSpecifier;
        public byte[] peerDataMac;
        public int state;
        public int uid;
        public int pubSubId = 0;
        public int peerInstanceId = 0;
        public byte[] peerDiscoveryMac = null;
        public int ndpId = 0;
        public long startTimestamp = 0;
        public Set<WifiAwareNetworkSpecifier> equivalentSpecifiers = new HashSet();

        void updateToSupportNewRequest(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier) {
            if (this.equivalentSpecifiers.add(wifiAwareNetworkSpecifier) && this.state == 102) {
                if (this.networkAgent == null) {
                    Log.wtf(WifiAwareDataPathStateManager.TAG, "updateToSupportNewRequest: null agent in CONFIRMED state!?");
                } else {
                    this.networkAgent.sendNetworkCapabilities(getNetworkCapabilities());
                }
            }
        }

        void removeSupportForRequest(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier) {
            this.equivalentSpecifiers.remove(wifiAwareNetworkSpecifier);
        }

        private NetworkCapabilities getNetworkCapabilities() {
            NetworkCapabilities networkCapabilities = new NetworkCapabilities(WifiAwareDataPathStateManager.sNetworkCapabilitiesFilter);
            networkCapabilities.setNetworkSpecifier(new WifiAwareAgentNetworkSpecifier((WifiAwareNetworkSpecifier[]) this.equivalentSpecifiers.toArray(new WifiAwareNetworkSpecifier[this.equivalentSpecifiers.size()])));
            return networkCapabilities;
        }

        CanonicalConnectionInfo getCanonicalDescriptor() {
            return new CanonicalConnectionInfo(this.peerDiscoveryMac, this.networkSpecifier.pmk, this.networkSpecifier.sessionId, this.networkSpecifier.passphrase);
        }

        static AwareNetworkRequestInformation processNetworkSpecifier(WifiAwareNetworkSpecifier wifiAwareNetworkSpecifier, WifiAwareStateManager wifiAwareStateManager, WifiPermissionsUtil wifiPermissionsUtil, WifiPermissionsWrapper wifiPermissionsWrapper, boolean z) {
            int i;
            byte[] bArr = wifiAwareNetworkSpecifier.peerMac;
            if (wifiAwareNetworkSpecifier.type < 0 || wifiAwareNetworkSpecifier.type > 3) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + ", invalid 'type' value");
                return null;
            }
            if (wifiAwareNetworkSpecifier.role != 0 && wifiAwareNetworkSpecifier.role != 1) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid 'role' value");
                return null;
            }
            if (wifiAwareNetworkSpecifier.role == 0 && wifiAwareNetworkSpecifier.type != 0 && wifiAwareNetworkSpecifier.type != 2) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid 'type' value for INITIATOR (only IB and OOB are permitted)");
                return null;
            }
            WifiAwareClientState client = wifiAwareStateManager.getClient(wifiAwareNetworkSpecifier.clientId);
            if (client == null) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- not client with this id -- clientId=" + wifiAwareNetworkSpecifier.clientId);
                return null;
            }
            int uid = client.getUid();
            if (!z && !wifiPermissionsUtil.isLegacyVersion(client.getCallingPackage(), 28) && wifiAwareNetworkSpecifier.type != 0 && wifiAwareNetworkSpecifier.type != 2) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- no ANY specifications allowed for this API level");
                return null;
            }
            int pubSubId = 0;
            if (wifiAwareNetworkSpecifier.type == 0 || wifiAwareNetworkSpecifier.type == 1) {
                WifiAwareDiscoverySessionState session = client.getSession(wifiAwareNetworkSpecifier.sessionId);
                if (session == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- no session with this id -- sessionId=" + wifiAwareNetworkSpecifier.sessionId);
                    return null;
                }
                if ((session.isPublishSession() && wifiAwareNetworkSpecifier.role != 1) || (!session.isPublishSession() && wifiAwareNetworkSpecifier.role != 0)) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid role for session type");
                    return null;
                }
                if (wifiAwareNetworkSpecifier.type == 0) {
                    pubSubId = session.getPubSubId();
                    WifiAwareDiscoverySessionState.PeerInfo peerInfo = session.getPeerInfo(wifiAwareNetworkSpecifier.peerId);
                    if (peerInfo == null) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- no peer info associated with this peer id -- peerId=" + wifiAwareNetworkSpecifier.peerId);
                        return null;
                    }
                    i = peerInfo.mInstanceId;
                    try {
                        bArr = peerInfo.mMac;
                        if (bArr != null && bArr.length == 6) {
                        }
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid peer MAC address");
                        return null;
                    } catch (IllegalArgumentException e) {
                        Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier + " -- invalid peer MAC address -- e=" + e);
                        return null;
                    }
                }
                i = 0;
            }
            if (wifiAwareNetworkSpecifier.requestorUid != uid) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier.toString() + " -- UID mismatch to clientId's uid=" + uid);
                return null;
            }
            if (wifiAwareNetworkSpecifier.pmk != null && wifiAwareNetworkSpecifier.pmk.length != 0 && wifiPermissionsWrapper.getUidPermission("android.permission.CONNECTIVITY_INTERNAL", wifiAwareNetworkSpecifier.requestorUid) != 0) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier.toString() + " -- UID doesn't have permission to use PMK API");
                return null;
            }
            if (!TextUtils.isEmpty(wifiAwareNetworkSpecifier.passphrase) && !WifiAwareUtils.validatePassphrase(wifiAwareNetworkSpecifier.passphrase)) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier.toString() + " -- invalid passphrase length: " + wifiAwareNetworkSpecifier.passphrase.length());
                return null;
            }
            if (wifiAwareNetworkSpecifier.pmk != null && !WifiAwareUtils.validatePmk(wifiAwareNetworkSpecifier.pmk)) {
                Log.e(WifiAwareDataPathStateManager.TAG, "processNetworkSpecifier: networkSpecifier=" + wifiAwareNetworkSpecifier.toString() + " -- invalid pmk length: " + wifiAwareNetworkSpecifier.pmk.length);
                return null;
            }
            AwareNetworkRequestInformation awareNetworkRequestInformation = new AwareNetworkRequestInformation();
            awareNetworkRequestInformation.state = 100;
            awareNetworkRequestInformation.uid = uid;
            awareNetworkRequestInformation.pubSubId = pubSubId;
            awareNetworkRequestInformation.peerInstanceId = i;
            awareNetworkRequestInformation.peerDiscoveryMac = bArr;
            awareNetworkRequestInformation.networkSpecifier = wifiAwareNetworkSpecifier;
            awareNetworkRequestInformation.equivalentSpecifiers.add(wifiAwareNetworkSpecifier);
            return awareNetworkRequestInformation;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("AwareNetworkRequestInformation: ");
            sb.append("state=");
            sb.append(this.state);
            sb.append(", ns=");
            sb.append(this.networkSpecifier);
            sb.append(", uid=");
            sb.append(this.uid);
            sb.append(", interfaceName=");
            sb.append(this.interfaceName);
            sb.append(", pubSubId=");
            sb.append(this.pubSubId);
            sb.append(", peerInstanceId=");
            sb.append(this.peerInstanceId);
            sb.append(", peerDiscoveryMac=");
            sb.append(this.peerDiscoveryMac == null ? "" : String.valueOf(HexEncoding.encode(this.peerDiscoveryMac)));
            sb.append(", ndpId=");
            sb.append(this.ndpId);
            sb.append(", peerDataMac=");
            sb.append(this.peerDataMac == null ? "" : String.valueOf(HexEncoding.encode(this.peerDataMac)));
            sb.append(", startTimestamp=");
            sb.append(this.startTimestamp);
            sb.append(", channelInfo=");
            sb.append(this.channelInfo);
            sb.append(", equivalentSpecifiers=[");
            Iterator<WifiAwareNetworkSpecifier> it = this.equivalentSpecifiers.iterator();
            while (it.hasNext()) {
                sb.append(it.next().toString());
                sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    static class CanonicalConnectionInfo {
        public final String passphrase;
        public final byte[] peerDiscoveryMac;
        public final byte[] pmk;
        public final int sessionId;

        CanonicalConnectionInfo(byte[] bArr, byte[] bArr2, int i, String str) {
            this.peerDiscoveryMac = bArr;
            this.pmk = bArr2;
            this.sessionId = i;
            this.passphrase = str;
        }

        public boolean matches(CanonicalConnectionInfo canonicalConnectionInfo) {
            return (canonicalConnectionInfo.peerDiscoveryMac == null || Arrays.equals(this.peerDiscoveryMac, canonicalConnectionInfo.peerDiscoveryMac)) && Arrays.equals(this.pmk, canonicalConnectionInfo.pmk) && TextUtils.equals(this.passphrase, canonicalConnectionInfo.passphrase) && (TextUtils.isEmpty(this.passphrase) || this.sessionId == canonicalConnectionInfo.sessionId);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("CanonicalConnectionInfo: [");
            sb.append("peerDiscoveryMac=");
            sb.append(this.peerDiscoveryMac == null ? "" : String.valueOf(HexEncoding.encode(this.peerDiscoveryMac)));
            sb.append(", pmk=");
            sb.append(this.pmk == null ? "" : "*");
            sb.append(", sessionId=");
            sb.append(this.sessionId);
            sb.append(", passphrase=");
            sb.append(this.passphrase == null ? "" : "*");
            sb.append("]");
            return sb.toString();
        }
    }

    @VisibleForTesting
    public class NetworkInterfaceWrapper {
        public NetworkInterfaceWrapper() {
        }

        public boolean configureAgentProperties(AwareNetworkRequestInformation awareNetworkRequestInformation, Set<WifiAwareNetworkSpecifier> set, int i, NetworkInfo networkInfo, NetworkCapabilities networkCapabilities, LinkProperties linkProperties) {
            InetAddress inetAddressNextElement;
            try {
                NetworkInterface byName = NetworkInterface.getByName(awareNetworkRequestInformation.interfaceName);
                if (byName == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + awareNetworkRequestInformation + ": can't get network interface (null)");
                    WifiAwareDataPathStateManager.this.mMgr.endDataPath(i);
                    awareNetworkRequestInformation.state = ISupplicantStaIfaceCallback.StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                    return false;
                }
                Enumeration<InetAddress> inetAddresses = byName.getInetAddresses();
                while (true) {
                    if (inetAddresses.hasMoreElements()) {
                        inetAddressNextElement = inetAddresses.nextElement();
                        if ((inetAddressNextElement instanceof Inet6Address) && inetAddressNextElement.isLinkLocalAddress()) {
                            break;
                        }
                    } else {
                        inetAddressNextElement = null;
                        break;
                    }
                }
                if (inetAddressNextElement == null) {
                    Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + awareNetworkRequestInformation + ": no link local addresses");
                    WifiAwareDataPathStateManager.this.mMgr.endDataPath(i);
                    awareNetworkRequestInformation.state = ISupplicantStaIfaceCallback.StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                    return false;
                }
                networkInfo.setIsAvailable(true);
                networkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, null, null);
                networkCapabilities.setNetworkSpecifier(new WifiAwareAgentNetworkSpecifier((WifiAwareNetworkSpecifier[]) set.toArray(new WifiAwareNetworkSpecifier[0])));
                linkProperties.setInterfaceName(awareNetworkRequestInformation.interfaceName);
                linkProperties.addLinkAddress(new LinkAddress(inetAddressNextElement, 64));
                linkProperties.addRoute(new RouteInfo(new IpPrefix("fe80::/64"), null, awareNetworkRequestInformation.interfaceName));
                return true;
            } catch (SocketException e) {
                Log.e(WifiAwareDataPathStateManager.TAG, "onDataPathConfirm: ACCEPT nnri=" + awareNetworkRequestInformation + ": can't get network interface - " + e);
                WifiAwareDataPathStateManager.this.mMgr.endDataPath(i);
                awareNetworkRequestInformation.state = ISupplicantStaIfaceCallback.StatusCode.RESTRICTION_FROM_AUTHORIZED_GDB;
                return false;
            }
        }
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("WifiAwareDataPathStateManager:");
        printWriter.println("  mInterfaces: " + this.mInterfaces);
        printWriter.println("  sNetworkCapabilitiesFilter: " + sNetworkCapabilitiesFilter);
        printWriter.println("  mNetworkRequestsCache: " + this.mNetworkRequestsCache);
        printWriter.println("  mNetworkFactory:");
        this.mNetworkFactory.dump(fileDescriptor, printWriter, strArr);
    }
}
