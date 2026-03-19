package com.android.server.connectivity.tethering;

import android.content.ContentResolver;
import android.net.ITetheringStatsProvider;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkStats;
import android.net.RouteInfo;
import android.net.netlink.ConntrackMessage;
import android.net.netlink.NetlinkConstants;
import android.net.netlink.NetlinkSocket;
import android.net.util.IpUtils;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.text.TextUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.tethering.OffloadController;
import com.android.server.connectivity.tethering.OffloadHardwareInterface;
import com.android.server.job.controllers.JobStatus;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class OffloadController {
    private static final String ANYIP = "0.0.0.0";
    private static final boolean DBG = false;
    private boolean mConfigInitialized;
    private final ContentResolver mContentResolver;
    private boolean mControlInitialized;
    private final Handler mHandler;
    private final OffloadHardwareInterface mHwInterface;
    private final SharedLog mLog;
    private int mNatUpdateCallbacksReceived;
    private int mNatUpdateNetlinkErrors;
    private final INetworkManagementService mNms;
    private LinkProperties mUpstreamLinkProperties;
    private static final String TAG = OffloadController.class.getSimpleName();
    private static final OffloadHardwareInterface.ForwardedStats EMPTY_STATS = new OffloadHardwareInterface.ForwardedStats();
    private ConcurrentHashMap<String, OffloadHardwareInterface.ForwardedStats> mForwardedStats = new ConcurrentHashMap<>(16, 0.75f, 1);
    private HashMap<String, Long> mInterfaceQuotas = new HashMap<>();
    private final ITetheringStatsProvider mStatsProvider = new OffloadTetheringStatsProvider();
    private final HashMap<String, LinkProperties> mDownstreams = new HashMap<>();
    private Set<IpPrefix> mExemptPrefixes = new HashSet();
    private Set<String> mLastLocalPrefixStrs = new HashSet();

    private enum UpdateType {
        IF_NEEDED,
        FORCE
    }

    public OffloadController(Handler handler, OffloadHardwareInterface offloadHardwareInterface, ContentResolver contentResolver, INetworkManagementService iNetworkManagementService, SharedLog sharedLog) {
        this.mHandler = handler;
        this.mHwInterface = offloadHardwareInterface;
        this.mContentResolver = contentResolver;
        this.mNms = iNetworkManagementService;
        this.mLog = sharedLog.forSubComponent(TAG);
        try {
            this.mNms.registerTetheringStatsProvider(this.mStatsProvider, getClass().getSimpleName());
        } catch (RemoteException e) {
            this.mLog.e("Cannot register offload stats provider: " + e);
        }
    }

    public boolean start() {
        if (started()) {
            return true;
        }
        if (isOffloadDisabled()) {
            this.mLog.i("tethering offload disabled");
            return false;
        }
        if (!this.mConfigInitialized) {
            this.mConfigInitialized = this.mHwInterface.initOffloadConfig();
            if (!this.mConfigInitialized) {
                this.mLog.i("tethering offload config not supported");
                stop();
                return false;
            }
        }
        this.mControlInitialized = this.mHwInterface.initOffloadControl(new OffloadHardwareInterface.ControlCallback() {
            @Override
            public void onStarted() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onStarted");
                }
            }

            @Override
            public void onStoppedError() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onStoppedError");
                }
            }

            @Override
            public void onStoppedUnsupported() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onStoppedUnsupported");
                    OffloadController.this.updateStatsForAllUpstreams();
                    OffloadController.this.forceTetherStatsPoll();
                }
            }

            @Override
            public void onSupportAvailable() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onSupportAvailable");
                    OffloadController.this.updateStatsForAllUpstreams();
                    OffloadController.this.forceTetherStatsPoll();
                    OffloadController.this.computeAndPushLocalPrefixes(UpdateType.FORCE);
                    OffloadController.this.pushAllDownstreamState();
                    OffloadController.this.pushUpstreamParameters(null);
                }
            }

            @Override
            public void onStoppedLimitReached() {
                if (OffloadController.this.started()) {
                    OffloadController.this.mLog.log("onStoppedLimitReached");
                    OffloadController.this.updateStatsForCurrentUpstream();
                    OffloadController.this.forceTetherStatsPoll();
                }
            }

            @Override
            public void onNatTimeoutUpdate(int i, String str, int i2, String str2, int i3) {
                if (OffloadController.this.started()) {
                    OffloadController.this.updateNatTimeout(i, str, i2, str2, i3);
                }
            }
        });
        boolean zStarted = started();
        if (!zStarted) {
            this.mLog.i("tethering offload control not supported");
            stop();
        } else {
            this.mLog.log("tethering offload started");
            this.mNatUpdateCallbacksReceived = 0;
            this.mNatUpdateNetlinkErrors = 0;
        }
        return zStarted;
    }

    public void stop() {
        boolean zStarted = started();
        updateStatsForCurrentUpstream();
        this.mUpstreamLinkProperties = null;
        this.mHwInterface.stopOffloadControl();
        this.mControlInitialized = false;
        this.mConfigInitialized = false;
        if (zStarted) {
            this.mLog.log("tethering offload stopped");
        }
    }

    private boolean started() {
        return this.mConfigInitialized && this.mControlInitialized;
    }

    private class OffloadTetheringStatsProvider extends ITetheringStatsProvider.Stub {
        private OffloadTetheringStatsProvider() {
        }

        public NetworkStats getTetherStats(int i) {
            Runnable runnable = new Runnable() {
                @Override
                public final void run() {
                    OffloadController.this.updateStatsForCurrentUpstream();
                }
            };
            if (Looper.myLooper() != OffloadController.this.mHandler.getLooper()) {
                OffloadController.this.mHandler.post(runnable);
            } else {
                runnable.run();
            }
            NetworkStats networkStats = new NetworkStats(SystemClock.elapsedRealtime(), 0);
            NetworkStats.Entry entry = new NetworkStats.Entry();
            entry.set = 0;
            entry.tag = 0;
            entry.uid = i == 1 ? -5 : -1;
            for (Map.Entry entry2 : OffloadController.this.mForwardedStats.entrySet()) {
                OffloadHardwareInterface.ForwardedStats forwardedStats = (OffloadHardwareInterface.ForwardedStats) entry2.getValue();
                entry.iface = (String) entry2.getKey();
                entry.rxBytes = forwardedStats.rxBytes;
                entry.txBytes = forwardedStats.txBytes;
                networkStats.addValues(entry);
            }
            return networkStats;
        }

        public void setInterfaceQuota(final String str, final long j) {
            OffloadController.this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    OffloadController.OffloadTetheringStatsProvider.lambda$setInterfaceQuota$1(this.f$0, j, str);
                }
            });
        }

        public static void lambda$setInterfaceQuota$1(OffloadTetheringStatsProvider offloadTetheringStatsProvider, long j, String str) {
            if (j == -1) {
                OffloadController.this.mInterfaceQuotas.remove(str);
            } else {
                OffloadController.this.mInterfaceQuotas.put(str, Long.valueOf(j));
            }
            OffloadController.this.maybeUpdateDataLimit(str);
        }
    }

    private String currentUpstreamInterface() {
        if (this.mUpstreamLinkProperties != null) {
            return this.mUpstreamLinkProperties.getInterfaceName();
        }
        return null;
    }

    private void maybeUpdateStats(String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        OffloadHardwareInterface.ForwardedStats forwardedStats = this.mHwInterface.getForwardedStats(str);
        OffloadHardwareInterface.ForwardedStats forwardedStats2 = this.mForwardedStats.get(str);
        if (forwardedStats2 != null) {
            forwardedStats.add(forwardedStats2);
        }
        this.mForwardedStats.put(str, forwardedStats);
    }

    private boolean maybeUpdateDataLimit(String str) {
        if (!started() || !TextUtils.equals(str, currentUpstreamInterface())) {
            return true;
        }
        Long lValueOf = this.mInterfaceQuotas.get(str);
        if (lValueOf == null) {
            lValueOf = Long.valueOf(JobStatus.NO_LATEST_RUNTIME);
        }
        return this.mHwInterface.setDataLimit(str, lValueOf.longValue());
    }

    private void updateStatsForCurrentUpstream() {
        maybeUpdateStats(currentUpstreamInterface());
    }

    private void updateStatsForAllUpstreams() {
        Iterator<Map.Entry<String, OffloadHardwareInterface.ForwardedStats>> it = this.mForwardedStats.entrySet().iterator();
        while (it.hasNext()) {
            maybeUpdateStats(it.next().getKey());
        }
    }

    private void forceTetherStatsPoll() {
        try {
            this.mNms.tetherLimitReached(this.mStatsProvider);
        } catch (RemoteException e) {
            this.mLog.e("Cannot report data limit reached: " + e);
        }
    }

    public void setUpstreamLinkProperties(LinkProperties linkProperties) {
        if (!started() || Objects.equals(this.mUpstreamLinkProperties, linkProperties)) {
            return;
        }
        String strCurrentUpstreamInterface = currentUpstreamInterface();
        this.mUpstreamLinkProperties = linkProperties != null ? new LinkProperties(linkProperties) : null;
        String strCurrentUpstreamInterface2 = currentUpstreamInterface();
        if (!TextUtils.isEmpty(strCurrentUpstreamInterface2)) {
            this.mForwardedStats.putIfAbsent(strCurrentUpstreamInterface2, EMPTY_STATS);
        }
        computeAndPushLocalPrefixes(UpdateType.IF_NEEDED);
        pushUpstreamParameters(strCurrentUpstreamInterface);
    }

    public void setLocalPrefixes(Set<IpPrefix> set) {
        this.mExemptPrefixes = set;
        if (started()) {
            computeAndPushLocalPrefixes(UpdateType.IF_NEEDED);
        }
    }

    public void notifyDownstreamLinkProperties(LinkProperties linkProperties) {
        LinkProperties linkPropertiesPut = this.mDownstreams.put(linkProperties.getInterfaceName(), new LinkProperties(linkProperties));
        if (!Objects.equals(linkPropertiesPut, linkProperties) && started()) {
            pushDownstreamState(linkPropertiesPut, linkProperties);
        }
    }

    private void pushDownstreamState(LinkProperties linkProperties, LinkProperties linkProperties2) {
        String interfaceName = linkProperties2.getInterfaceName();
        List<RouteInfo> routes = linkProperties != null ? linkProperties.getRoutes() : Collections.EMPTY_LIST;
        List<RouteInfo> routes2 = linkProperties2.getRoutes();
        for (RouteInfo routeInfo : routes) {
            if (!shouldIgnoreDownstreamRoute(routeInfo) && !routes2.contains(routeInfo)) {
                this.mHwInterface.removeDownstreamPrefix(interfaceName, routeInfo.getDestination().toString());
            }
        }
        for (RouteInfo routeInfo2 : routes2) {
            if (!shouldIgnoreDownstreamRoute(routeInfo2) && !routes.contains(routeInfo2)) {
                this.mHwInterface.addDownstreamPrefix(interfaceName, routeInfo2.getDestination().toString());
            }
        }
    }

    private void pushAllDownstreamState() {
        Iterator<LinkProperties> it = this.mDownstreams.values().iterator();
        while (it.hasNext()) {
            pushDownstreamState(null, it.next());
        }
    }

    public void removeDownstreamInterface(String str) {
        LinkProperties linkPropertiesRemove = this.mDownstreams.remove(str);
        if (linkPropertiesRemove != null && started()) {
            for (RouteInfo routeInfo : linkPropertiesRemove.getRoutes()) {
                if (!shouldIgnoreDownstreamRoute(routeInfo)) {
                    this.mHwInterface.removeDownstreamPrefix(str, routeInfo.getDestination().toString());
                }
            }
        }
    }

    private boolean isOffloadDisabled() {
        return Settings.Global.getInt(this.mContentResolver, "tether_offload_disabled", this.mHwInterface.getDefaultTetherOffloadDisabled()) != 0;
    }

    private boolean pushUpstreamParameters(String str) {
        String hostAddress;
        String strCurrentUpstreamInterface = currentUpstreamInterface();
        if (TextUtils.isEmpty(strCurrentUpstreamInterface)) {
            boolean upstreamParameters = this.mHwInterface.setUpstreamParameters(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, ANYIP, ANYIP, null);
            maybeUpdateStats(str);
            return upstreamParameters;
        }
        ArrayList<String> arrayList = new ArrayList<>();
        Iterator it = this.mUpstreamLinkProperties.getAddresses().iterator();
        while (true) {
            if (it.hasNext()) {
                InetAddress inetAddress = (InetAddress) it.next();
                if (inetAddress instanceof Inet4Address) {
                    hostAddress = inetAddress.getHostAddress();
                    break;
                }
            } else {
                hostAddress = null;
                break;
            }
        }
        String str2 = null;
        for (RouteInfo routeInfo : this.mUpstreamLinkProperties.getRoutes()) {
            if (routeInfo.hasGateway()) {
                String hostAddress2 = routeInfo.getGateway().getHostAddress();
                if (!routeInfo.isIPv4Default()) {
                    if (routeInfo.isIPv6Default()) {
                        arrayList.add(hostAddress2);
                    }
                } else {
                    str2 = hostAddress2;
                }
            }
        }
        OffloadHardwareInterface offloadHardwareInterface = this.mHwInterface;
        if (arrayList.isEmpty()) {
            arrayList = null;
        }
        boolean upstreamParameters2 = offloadHardwareInterface.setUpstreamParameters(strCurrentUpstreamInterface, hostAddress, str2, arrayList);
        if (!upstreamParameters2) {
            return upstreamParameters2;
        }
        maybeUpdateStats(str);
        boolean zMaybeUpdateDataLimit = maybeUpdateDataLimit(strCurrentUpstreamInterface);
        if (!zMaybeUpdateDataLimit) {
            this.mLog.log("Setting data limit for " + strCurrentUpstreamInterface + " failed, disabling offload.");
            stop();
        }
        return zMaybeUpdateDataLimit;
    }

    private boolean computeAndPushLocalPrefixes(UpdateType updateType) {
        boolean z;
        if (updateType != UpdateType.FORCE) {
            z = false;
        } else {
            z = true;
        }
        Set<String> setComputeLocalPrefixStrings = computeLocalPrefixStrings(this.mExemptPrefixes, this.mUpstreamLinkProperties);
        if (!z && this.mLastLocalPrefixStrs.equals(setComputeLocalPrefixStrings)) {
            return true;
        }
        this.mLastLocalPrefixStrs = setComputeLocalPrefixStrings;
        return this.mHwInterface.setLocalPrefixes(new ArrayList<>(setComputeLocalPrefixStrings));
    }

    private static Set<String> computeLocalPrefixStrings(Set<IpPrefix> set, LinkProperties linkProperties) {
        HashSet hashSet = new HashSet(set);
        if (linkProperties != null) {
            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                if (linkAddress.isGlobalPreferred()) {
                    InetAddress address = linkAddress.getAddress();
                    if (address instanceof Inet6Address) {
                        hashSet.add(new IpPrefix(address, 128));
                    }
                }
            }
        }
        HashSet hashSet2 = new HashSet();
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            hashSet2.add(((IpPrefix) it.next()).toString());
        }
        return hashSet2;
    }

    private static boolean shouldIgnoreDownstreamRoute(RouteInfo routeInfo) {
        return !routeInfo.getDestinationLinkAddress().isGlobalPreferred();
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        if (isOffloadDisabled()) {
            indentingPrintWriter.println("Offload disabled");
            return;
        }
        boolean zStarted = started();
        StringBuilder sb = new StringBuilder();
        sb.append("Offload HALs ");
        sb.append(zStarted ? "started" : "not started");
        indentingPrintWriter.println(sb.toString());
        LinkProperties linkProperties = this.mUpstreamLinkProperties;
        indentingPrintWriter.println("Current upstream: " + (linkProperties != null ? linkProperties.getInterfaceName() : null));
        indentingPrintWriter.println("Exempt prefixes: " + this.mLastLocalPrefixStrs);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("NAT timeout update callbacks received during the ");
        sb2.append(zStarted ? "current" : "last");
        sb2.append(" offload session: ");
        sb2.append(this.mNatUpdateCallbacksReceived);
        indentingPrintWriter.println(sb2.toString());
        StringBuilder sb3 = new StringBuilder();
        sb3.append("NAT timeout update netlink errors during the ");
        sb3.append(zStarted ? "current" : "last");
        sb3.append(" offload session: ");
        sb3.append(this.mNatUpdateNetlinkErrors);
        indentingPrintWriter.println(sb3.toString());
    }

    private void updateNatTimeout(int i, String str, int i2, String str2, int i3) {
        String strProtoNameFor = protoNameFor(i);
        if (strProtoNameFor == null) {
            this.mLog.e("Unknown NAT update callback protocol: " + i);
            return;
        }
        Inet4Address iPv4Address = parseIPv4Address(str);
        if (iPv4Address == null) {
            this.mLog.e("Failed to parse IPv4 address: " + str);
            return;
        }
        if (!IpUtils.isValidUdpOrTcpPort(i2)) {
            this.mLog.e("Invalid src port: " + i2);
            return;
        }
        Inet4Address iPv4Address2 = parseIPv4Address(str2);
        if (iPv4Address2 == null) {
            this.mLog.e("Failed to parse IPv4 address: " + str2);
            return;
        }
        if (!IpUtils.isValidUdpOrTcpPort(i3)) {
            this.mLog.e("Invalid dst port: " + i3);
            return;
        }
        this.mNatUpdateCallbacksReceived++;
        String str3 = String.format("%s (%s, %s) -> (%s, %s)", strProtoNameFor, str, Integer.valueOf(i2), str2, Integer.valueOf(i3));
        byte[] bArrNewIPv4TimeoutUpdateRequest = ConntrackMessage.newIPv4TimeoutUpdateRequest(i, iPv4Address, i2, iPv4Address2, i3, connectionTimeoutUpdateSecondsFor(i));
        try {
            NetlinkSocket.sendOneShotKernelMessage(OsConstants.NETLINK_NETFILTER, bArrNewIPv4TimeoutUpdateRequest);
        } catch (ErrnoException e) {
            this.mNatUpdateNetlinkErrors++;
            this.mLog.e("Error updating NAT conntrack entry >" + str3 + "<: " + e + ", msg: " + NetlinkConstants.hexify(bArrNewIPv4TimeoutUpdateRequest));
            SharedLog sharedLog = this.mLog;
            StringBuilder sb = new StringBuilder();
            sb.append("NAT timeout update callbacks received: ");
            sb.append(this.mNatUpdateCallbacksReceived);
            sharedLog.log(sb.toString());
            this.mLog.log("NAT timeout update netlink errors: " + this.mNatUpdateNetlinkErrors);
        }
    }

    private static Inet4Address parseIPv4Address(String str) {
        try {
            InetAddress numericAddress = InetAddress.parseNumericAddress(str);
            if (numericAddress instanceof Inet4Address) {
                return (Inet4Address) numericAddress;
            }
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String protoNameFor(int i) {
        if (i == OsConstants.IPPROTO_UDP) {
            return "UDP";
        }
        if (i == OsConstants.IPPROTO_TCP) {
            return "TCP";
        }
        return null;
    }

    private static int connectionTimeoutUpdateSecondsFor(int i) {
        if (i == OsConstants.IPPROTO_TCP) {
            return 432000;
        }
        return 180;
    }
}
