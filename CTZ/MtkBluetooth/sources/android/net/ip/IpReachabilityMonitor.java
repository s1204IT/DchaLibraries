package android.net.ip;

import android.content.Context;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.net.ip.IpNeighborMonitor;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.IpReachabilityEvent;
import android.net.util.InterfaceParams;
import android.net.util.MultinetworkPolicyTracker;
import android.net.util.SharedLog;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.DumpUtils;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IpReachabilityMonitor {
    private static final boolean DBG = !Build.IS_USER;
    private static final String TAG = "IpReachabilityMonitor";
    private static final boolean VDBG = false;
    private final Callback mCallback;
    private final Dependencies mDependencies;
    private final InterfaceParams mInterfaceParams;
    private final IpNeighborMonitor mIpNeighborMonitor;
    private volatile long mLastProbeTimeMs;
    private LinkProperties mLinkProperties;
    private final SharedLog mLog;
    private final IpConnectivityLog mMetricsLog;
    private final MultinetworkPolicyTracker mMultinetworkPolicyTracker;
    private Map<InetAddress, IpNeighborMonitor.NeighborEvent> mNeighborWatchList;

    public interface Callback {
        void notifyLost(InetAddress inetAddress, String str);
    }

    interface Dependencies {
        void acquireWakeLock(long j);

        static Dependencies makeDefault(Context context, String str) {
            final PowerManager.WakeLock wakeLockNewWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "IpReachabilityMonitor." + str);
            return new Dependencies() {
                @Override
                public void acquireWakeLock(long j) {
                    wakeLockNewWakeLock.acquire(j);
                }
            };
        }
    }

    public IpReachabilityMonitor(Context context, InterfaceParams interfaceParams, Handler handler, SharedLog sharedLog, Callback callback, MultinetworkPolicyTracker multinetworkPolicyTracker) {
        this(interfaceParams, handler, sharedLog, callback, multinetworkPolicyTracker, Dependencies.makeDefault(context, interfaceParams.name));
    }

    @VisibleForTesting
    IpReachabilityMonitor(InterfaceParams interfaceParams, Handler handler, SharedLog sharedLog, Callback callback, MultinetworkPolicyTracker multinetworkPolicyTracker, Dependencies dependencies) {
        this.mMetricsLog = new IpConnectivityLog();
        this.mLinkProperties = new LinkProperties();
        this.mNeighborWatchList = new HashMap();
        if (interfaceParams == null) {
            throw new IllegalArgumentException("null InterfaceParams");
        }
        this.mInterfaceParams = interfaceParams;
        this.mLog = sharedLog.forSubComponent(TAG);
        this.mCallback = callback;
        this.mMultinetworkPolicyTracker = multinetworkPolicyTracker;
        this.mDependencies = dependencies;
        this.mIpNeighborMonitor = new IpNeighborMonitor(handler, this.mLog, new IpNeighborMonitor.NeighborEventConsumer() {
            @Override
            public final void accept(IpNeighborMonitor.NeighborEvent neighborEvent) {
                IpReachabilityMonitor.lambda$new$0(this.f$0, neighborEvent);
            }
        });
        this.mIpNeighborMonitor.start();
    }

    public static void lambda$new$0(IpReachabilityMonitor ipReachabilityMonitor, IpNeighborMonitor.NeighborEvent neighborEvent) {
        if (ipReachabilityMonitor.mInterfaceParams.index == neighborEvent.ifindex && ipReachabilityMonitor.mNeighborWatchList.containsKey(neighborEvent.ip)) {
            IpNeighborMonitor.NeighborEvent neighborEventPut = ipReachabilityMonitor.mNeighborWatchList.put(neighborEvent.ip, neighborEvent);
            if (neighborEvent.nudState == 32) {
                ipReachabilityMonitor.mLog.w("ALERT neighbor went from: " + neighborEventPut + " to: " + neighborEvent);
                ipReachabilityMonitor.handleNeighborLost(neighborEvent);
            }
        }
    }

    public void stop() {
        this.mIpNeighborMonitor.stop();
        clearLinkProperties();
    }

    public void dump(PrintWriter printWriter) {
        DumpUtils.dumpAsync(this.mIpNeighborMonitor.getHandler(), new DumpUtils.Dump() {
            public void dump(PrintWriter printWriter2, String str) {
                printWriter2.println(IpReachabilityMonitor.this.describeWatchList("\n"));
            }
        }, printWriter, "", 1000L);
    }

    private String describeWatchList() {
        return describeWatchList(" ");
    }

    private String describeWatchList(String str) {
        StringBuilder sb = new StringBuilder();
        sb.append("iface{" + this.mInterfaceParams + "}," + str);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("ntable=[");
        sb2.append(str);
        sb.append(sb2.toString());
        String string = "";
        for (Map.Entry<InetAddress, IpNeighborMonitor.NeighborEvent> entry : this.mNeighborWatchList.entrySet()) {
            sb.append(string);
            sb.append(entry.getKey().getHostAddress() + "/" + entry.getValue());
            StringBuilder sb3 = new StringBuilder();
            sb3.append(",");
            sb3.append(str);
            string = sb3.toString();
        }
        sb.append("]");
        return sb.toString();
    }

    private static boolean isOnLink(List<RouteInfo> list, InetAddress inetAddress) {
        for (RouteInfo routeInfo : list) {
            if (!routeInfo.hasGateway() && routeInfo.matches(inetAddress)) {
                return true;
            }
        }
        return false;
    }

    public void updateLinkProperties(LinkProperties linkProperties) {
        if (!this.mInterfaceParams.name.equals(linkProperties.getInterfaceName())) {
            Log.wtf(TAG, "requested LinkProperties interface '" + linkProperties.getInterfaceName() + "' does not match: " + this.mInterfaceParams.name);
            return;
        }
        this.mLinkProperties = new LinkProperties(linkProperties);
        HashMap map = new HashMap();
        List<RouteInfo> routes = this.mLinkProperties.getRoutes();
        for (RouteInfo routeInfo : routes) {
            if (routeInfo.hasGateway()) {
                InetAddress gateway = routeInfo.getGateway();
                if (isOnLink(routes, gateway)) {
                    map.put(gateway, this.mNeighborWatchList.getOrDefault(gateway, null));
                }
            }
        }
        for (InetAddress inetAddress : linkProperties.getDnsServers()) {
            if (isOnLink(routes, inetAddress)) {
                map.put(inetAddress, this.mNeighborWatchList.getOrDefault(inetAddress, null));
            }
        }
        this.mNeighborWatchList = map;
        if (DBG) {
            Log.d(TAG, "watch: " + describeWatchList());
        }
    }

    public void clearLinkProperties() {
        this.mLinkProperties.clear();
        this.mNeighborWatchList.clear();
        if (DBG) {
            Log.d(TAG, "clear: " + describeWatchList());
        }
    }

    private void handleNeighborLost(IpNeighborMonitor.NeighborEvent neighborEvent) {
        LinkProperties linkProperties = new LinkProperties(this.mLinkProperties);
        InetAddress key = null;
        for (Map.Entry<InetAddress, IpNeighborMonitor.NeighborEvent> entry : this.mNeighborWatchList.entrySet()) {
            if (entry.getValue().nudState == 32) {
                key = entry.getKey();
                for (RouteInfo routeInfo : this.mLinkProperties.getRoutes()) {
                    if (key.equals(routeInfo.getGateway())) {
                        linkProperties.removeRoute(routeInfo);
                    }
                }
                if (avoidingBadLinks() || !(key instanceof Inet6Address)) {
                    linkProperties.removeDnsServer(key);
                }
            }
        }
        LinkProperties.ProvisioningChange provisioningChangeCompareProvisioning = LinkProperties.compareProvisioning(this.mLinkProperties, linkProperties);
        if (provisioningChangeCompareProvisioning == LinkProperties.ProvisioningChange.LOST_PROVISIONING) {
            String str = "FAILURE: LOST_PROVISIONING, " + neighborEvent;
            Log.w(TAG, str);
            if (this.mCallback != null) {
                this.mCallback.notifyLost(key, str);
            }
        }
        logNudFailed(provisioningChangeCompareProvisioning);
    }

    private boolean avoidingBadLinks() {
        return this.mMultinetworkPolicyTracker == null || this.mMultinetworkPolicyTracker.getAvoidBadWifi();
    }

    public void probeAll() {
        ArrayList<InetAddress> arrayList = new ArrayList(this.mNeighborWatchList.keySet());
        if (!arrayList.isEmpty()) {
            this.mDependencies.acquireWakeLock(getProbeWakeLockDuration());
        }
        for (InetAddress inetAddress : arrayList) {
            int iStartKernelNeighborProbe = IpNeighborMonitor.startKernelNeighborProbe(this.mInterfaceParams.index, inetAddress);
            this.mLog.log(String.format("put neighbor %s into NUD_PROBE state (rval=%d)", inetAddress.getHostAddress(), Integer.valueOf(iStartKernelNeighborProbe)));
            logEvent(256, iStartKernelNeighborProbe);
        }
        this.mLastProbeTimeMs = SystemClock.elapsedRealtime();
    }

    private static long getProbeWakeLockDuration() {
        return 3500L;
    }

    private void logEvent(int i, int i2) {
        this.mMetricsLog.log(this.mInterfaceParams.name, new IpReachabilityEvent(i | (i2 & 255)));
    }

    private void logNudFailed(LinkProperties.ProvisioningChange provisioningChange) {
        this.mMetricsLog.log(this.mInterfaceParams.name, new IpReachabilityEvent(IpReachabilityEvent.nudFailureEventType(SystemClock.elapsedRealtime() - this.mLastProbeTimeMs < getProbeWakeLockDuration(), provisioningChange == LinkProperties.ProvisioningChange.LOST_PROVISIONING)));
    }
}
