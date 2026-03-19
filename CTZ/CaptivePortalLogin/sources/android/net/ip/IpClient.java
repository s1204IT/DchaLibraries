package android.net.ip;

import android.R;
import android.content.Context;
import android.net.DhcpResults;
import android.net.INetd;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;
import android.net.apf.ApfFilter;
import android.net.dhcp.DhcpClient;
import android.net.ip.IpClient;
import android.net.ip.IpReachabilityMonitor;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.IpManagerEvent;
import android.net.util.InterfaceParams;
import android.net.util.MultinetworkPolicyTracker;
import android.net.util.NetdService;
import android.net.util.SharedLog;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IState;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.Preconditions;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.net.NetlinkTracker;
import dalvik.system.PathClassLoader;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IpClient extends StateMachine {
    private final ConditionVariable mApfDataSnapshotComplete;
    private ApfFilter mApfFilter;

    @VisibleForTesting
    protected final Callback mCallback;
    private final String mClatInterfaceName;
    private ProvisioningConfiguration mConfiguration;
    private final LocalLog mConnectivityPacketLog;
    private final Context mContext;
    private final Dependencies mDependencies;
    private final WakeupMessage mDhcpActionTimeoutAlarm;
    private DhcpClient mDhcpClient;
    private DhcpResults mDhcpResults;
    private ProxyInfo mHttpProxy;
    private final InterfaceController mInterfaceCtrl;
    private final String mInterfaceName;
    private InterfaceParams mInterfaceParams;
    private IpReachabilityMonitor mIpReachabilityMonitor;
    private LinkProperties mLinkProperties;
    private final SharedLog mLog;
    private final IpConnectivityLog mMetricsLog;
    private final MessageHandlingLogger mMsgStateLogger;
    private boolean mMulticastFiltering;
    private MultinetworkPolicyTracker mMultinetworkPolicyTracker;
    private final NetlinkTracker mNetlinkTracker;
    private final INetworkManagementService mNwService;
    private final WakeupMessage mProvisioningTimeoutAlarm;
    private State mRunningState;
    private final CountDownLatch mShutdownLatch;
    private long mStartTimeMillis;
    private final State mStartedState;
    private State mStoppedState;
    private final State mStoppingState;
    private final String mTag;
    private String mTcpBufferSizes;
    private static final boolean DBG = !Build.IS_USER;
    private static final Class[] sMessageClasses = {IpClient.class, DhcpClient.class};
    private static final SparseArray<String> sWhatToString = MessageUtils.findMessageNames(sMessageClasses);
    private static final ConcurrentHashMap<String, SharedLog> sSmLogs = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LocalLog> sPktLogs = new ConcurrentHashMap<>();

    public static class Callback {
        public void onPreDhcpAction() {
        }

        public void onPostDhcpAction() {
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
        }

        public void onProvisioningSuccess(LinkProperties linkProperties) {
        }

        public void onProvisioningFailure(LinkProperties linkProperties) {
        }

        public void onLinkPropertiesChange(LinkProperties linkProperties) {
        }

        public void onReachabilityLost(String str) {
        }

        public void onQuit() {
        }

        public void installPacketFilter(byte[] bArr) {
        }

        public void startReadPacketFilter() {
        }

        public void setFallbackMulticastFilter(boolean z) {
        }

        public void setNeighborDiscoveryOffload(boolean z) {
        }
    }

    private class LoggingCallbackWrapper extends Callback {
        private Callback mCallback;

        public LoggingCallbackWrapper(Callback callback) {
            this.mCallback = callback;
        }

        private void log(String str) {
            IpClient.this.mLog.log("INVOKE " + str);
        }

        @Override
        public void onPreDhcpAction() {
            this.mCallback.onPreDhcpAction();
            log("onPreDhcpAction()");
        }

        @Override
        public void onPostDhcpAction() {
            this.mCallback.onPostDhcpAction();
            log("onPostDhcpAction()");
        }

        @Override
        public void onNewDhcpResults(DhcpResults dhcpResults) {
            this.mCallback.onNewDhcpResults(dhcpResults);
            log("onNewDhcpResults({" + dhcpResults + "})");
        }

        @Override
        public void onProvisioningSuccess(LinkProperties linkProperties) {
            this.mCallback.onProvisioningSuccess(linkProperties);
            log("onProvisioningSuccess({" + linkProperties + "})");
        }

        @Override
        public void onProvisioningFailure(LinkProperties linkProperties) {
            this.mCallback.onProvisioningFailure(linkProperties);
            log("onProvisioningFailure({" + linkProperties + "})");
        }

        @Override
        public void onLinkPropertiesChange(LinkProperties linkProperties) {
            this.mCallback.onLinkPropertiesChange(linkProperties);
            log("onLinkPropertiesChange({" + linkProperties + "})");
        }

        @Override
        public void onReachabilityLost(String str) {
            this.mCallback.onReachabilityLost(str);
            log("onReachabilityLost(" + str + ")");
        }

        @Override
        public void onQuit() {
            this.mCallback.onQuit();
            log("onQuit()");
        }

        @Override
        public void installPacketFilter(byte[] bArr) {
            this.mCallback.installPacketFilter(bArr);
            log("installPacketFilter(byte[" + bArr.length + "])");
        }

        @Override
        public void startReadPacketFilter() {
            this.mCallback.startReadPacketFilter();
            log("startReadPacketFilter()");
        }

        @Override
        public void setFallbackMulticastFilter(boolean z) {
            this.mCallback.setFallbackMulticastFilter(z);
            log("setFallbackMulticastFilter(" + z + ")");
        }

        @Override
        public void setNeighborDiscoveryOffload(boolean z) {
            this.mCallback.setNeighborDiscoveryOffload(z);
            log("setNeighborDiscoveryOffload(" + z + ")");
        }
    }

    public static class ProvisioningConfiguration {
        ApfCapabilities mApfCapabilities;
        InitialConfiguration mInitialConfig;
        int mRequestedPreDhcpActionMs;
        StaticIpConfiguration mStaticIpConfig;
        boolean mEnableIPv4 = true;
        boolean mEnableIPv6 = true;
        boolean mUsingMultinetworkPolicyTracker = true;
        boolean mUsingIpReachabilityMonitor = true;
        int mProvisioningTimeoutMs = 36000;
        int mIPv6AddrGenMode = 2;
        Network mNetwork = null;
        String mDisplayName = null;

        public String toString() {
            return new StringJoiner(", ", getClass().getSimpleName() + "{", "}").add("mEnableIPv4: " + this.mEnableIPv4).add("mEnableIPv6: " + this.mEnableIPv6).add("mUsingMultinetworkPolicyTracker: " + this.mUsingMultinetworkPolicyTracker).add("mUsingIpReachabilityMonitor: " + this.mUsingIpReachabilityMonitor).add("mRequestedPreDhcpActionMs: " + this.mRequestedPreDhcpActionMs).add("mInitialConfig: " + this.mInitialConfig).add("mStaticIpConfig: " + this.mStaticIpConfig).add("mApfCapabilities: " + this.mApfCapabilities).add("mProvisioningTimeoutMs: " + this.mProvisioningTimeoutMs).add("mIPv6AddrGenMode: " + this.mIPv6AddrGenMode).add("mNetwork: " + this.mNetwork).add("mDisplayName: " + this.mDisplayName).toString();
        }
    }

    public static class InitialConfiguration {
        public final Set<IpPrefix> directlyConnectedRoutes;
        public final Set<InetAddress> dnsServers;
        public Inet4Address gateway;
        public final Set<LinkAddress> ipAddresses;

        public String toString() {
            return String.format("InitialConfiguration(IPs: {%s}, prefixes: {%s}, DNS: {%s}, v4 gateway: %s)", IpClient.join(", ", this.ipAddresses), IpClient.join(", ", this.directlyConnectedRoutes), IpClient.join(", ", this.dnsServers), this.gateway);
        }

        public boolean isProvisionedBy(List<LinkAddress> list, List<RouteInfo> list2) {
            if (this.ipAddresses.isEmpty()) {
                return false;
            }
            for (final LinkAddress linkAddress : this.ipAddresses) {
                if (!IpClient.any(list, new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return linkAddress.isSameAddressAs((LinkAddress) obj);
                    }
                })) {
                    return false;
                }
            }
            if (list2 != null) {
                for (final IpPrefix ipPrefix : this.directlyConnectedRoutes) {
                    if (!IpClient.any(list2, new Predicate() {
                        @Override
                        public final boolean test(Object obj) {
                            return IpClient.InitialConfiguration.isDirectlyConnectedRoute((RouteInfo) obj, ipPrefix);
                        }
                    })) {
                        return false;
                    }
                }
                return true;
            }
            return true;
        }

        private static boolean isDirectlyConnectedRoute(RouteInfo routeInfo, IpPrefix ipPrefix) {
            return !routeInfo.hasGateway() && ipPrefix.equals(routeInfo.getDestination());
        }
    }

    public static class Dependencies {
        public INetworkManagementService getNMS() {
            return INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        }

        public INetd getNetd() {
            return NetdService.getInstance();
        }
    }

    class AnonymousClass1 extends Dependencies {
        final INetworkManagementService val$nwService;

        @Override
        public INetworkManagementService getNMS() {
            return this.val$nwService;
        }
    }

    @VisibleForTesting
    IpClient(Context context, String str, Callback callback, Dependencies dependencies) {
        super(IpClient.class.getSimpleName() + "." + str);
        this.mStoppingState = new StoppingState();
        this.mStartedState = new StartedState();
        this.mMetricsLog = new IpConnectivityLog();
        this.mApfDataSnapshotComplete = new ConditionVariable();
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(callback);
        this.mTag = getName();
        this.mContext = context;
        this.mInterfaceName = str;
        this.mClatInterfaceName = "v4-" + str;
        this.mCallback = new LoggingCallbackWrapper(callback);
        this.mDependencies = dependencies;
        this.mShutdownLatch = new CountDownLatch(1);
        this.mNwService = dependencies.getNMS();
        sSmLogs.putIfAbsent(this.mInterfaceName, new SharedLog(500, this.mTag));
        this.mLog = sSmLogs.get(this.mInterfaceName);
        sPktLogs.putIfAbsent(this.mInterfaceName, new LocalLog(100));
        this.mConnectivityPacketLog = sPktLogs.get(this.mInterfaceName);
        this.mMsgStateLogger = new MessageHandlingLogger(null);
        this.mInterfaceCtrl = new InterfaceController(this.mInterfaceName, this.mNwService, dependencies.getNetd(), this.mLog);
        this.mNetlinkTracker = new AnonymousClass3(this.mInterfaceName, new NetlinkTracker.Callback() {
            public void update() {
                IpClient.this.sendMessage(6);
            }
        });
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
        this.mProvisioningTimeoutAlarm = new WakeupMessage(this.mContext, getHandler(), this.mTag + ".EVENT_PROVISIONING_TIMEOUT", 10);
        this.mDhcpActionTimeoutAlarm = new WakeupMessage(this.mContext, getHandler(), this.mTag + ".EVENT_DHCPACTION_TIMEOUT", 11);
        try {
            Constructor<?> constructor = new PathClassLoader("/system/framework/mediatek-framework-net.jar", this.mContext.getClassLoader()).loadClass("com.mediatek.net.ip.MtkIpRunningState").getConstructor(Context.class, IpClient.class, String.class, NetlinkTracker.class, State.class);
            constructor.setAccessible(true);
            this.mRunningState = (State) constructor.newInstance(context, this, str, this.mNetlinkTracker, new RunningState());
        } catch (Throwable th) {
            logError("No MtkIpRunningState! Used AOSP for instead! %s", th);
            this.mRunningState = new RunningState();
        }
        try {
            Constructor<?> constructor2 = new PathClassLoader("/system/framework/mediatek-framework-net.jar", this.mContext.getClassLoader()).loadClass("com.mediatek.net.ip.MtkIpStoppedState").getConstructor(Context.class, IpClient.class, String.class, State.class);
            constructor2.setAccessible(true);
            this.mStoppedState = (State) constructor2.newInstance(context, this, str, new StoppedState());
        } catch (Throwable th2) {
            logError("No mtkStoppedClass! Used AOSP for instead! %s", th2);
            this.mStoppedState = new StoppedState();
        }
        configureAndStartStateMachine();
        startStateMachineUpdaters();
    }

    class AnonymousClass3 extends NetlinkTracker {
        AnonymousClass3(String str, NetlinkTracker.Callback callback) {
            super(str, callback);
        }

        public void interfaceAdded(String str) {
            super.interfaceAdded(str);
            if (!IpClient.this.mClatInterfaceName.equals(str)) {
                if (!IpClient.this.mInterfaceName.equals(str)) {
                    return;
                }
            } else {
                IpClient.this.mCallback.setNeighborDiscoveryOffload(false);
            }
            logMsg("interfaceAdded(" + str + ")");
        }

        public void interfaceRemoved(String str) {
            super.interfaceRemoved(str);
            if (!IpClient.this.mClatInterfaceName.equals(str)) {
                if (!IpClient.this.mInterfaceName.equals(str)) {
                    return;
                }
            } else {
                IpClient.this.mCallback.setNeighborDiscoveryOffload(true);
            }
            logMsg("interfaceRemoved(" + str + ")");
        }

        private void logMsg(final String str) {
            Log.d(IpClient.this.mTag, str);
            IpClient.this.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    IpClient.this.mLog.log("OBSERVED " + str);
                }
            });
        }
    }

    private void configureAndStartStateMachine() {
        addState(this.mStoppedState);
        addState(this.mStartedState);
        addState(this.mRunningState, this.mStartedState);
        addState(this.mStoppingState);
        setInitialState(this.mStoppedState);
        super.start();
    }

    private void startStateMachineUpdaters() {
        try {
            this.mNwService.registerObserver(this.mNetlinkTracker);
        } catch (RemoteException e) {
            logError("Couldn't register NetlinkTracker: %s", e);
        }
    }

    private void stopStateMachineUpdaters() {
        try {
            this.mNwService.unregisterObserver(this.mNetlinkTracker);
        } catch (RemoteException e) {
            logError("Couldn't unregister NetlinkTracker: %s", e);
        }
    }

    protected void onQuitting() {
        this.mCallback.onQuit();
        this.mShutdownLatch.countDown();
    }

    public void confirmConfiguration() {
        sendMessage(4);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (strArr != null && strArr.length > 0 && "confirm".equals(strArr[0])) {
            confirmConfiguration();
            return;
        }
        ApfFilter apfFilter = this.mApfFilter;
        ProvisioningConfiguration provisioningConfiguration = this.mConfiguration;
        ApfCapabilities apfCapabilities = provisioningConfiguration != null ? provisioningConfiguration.mApfCapabilities : null;
        PrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println(this.mTag + " APF dump:");
        indentingPrintWriter.increaseIndent();
        if (apfFilter != null) {
            if (apfCapabilities.hasDataAccess()) {
                this.mApfDataSnapshotComplete.close();
                this.mCallback.startReadPacketFilter();
                if (!this.mApfDataSnapshotComplete.block(1000L)) {
                    indentingPrintWriter.print("TIMEOUT: DUMPING STALE APF SNAPSHOT");
                }
            }
            apfFilter.dump(indentingPrintWriter);
        } else {
            indentingPrintWriter.print("No active ApfFilter; ");
            if (provisioningConfiguration == null) {
                indentingPrintWriter.println("IpClient not yet started.");
            } else if (apfCapabilities == null || apfCapabilities.apfVersionSupported == 0) {
                indentingPrintWriter.println("Hardware does not support APF.");
            } else {
                indentingPrintWriter.println("ApfFilter not yet started, APF capabilities: " + apfCapabilities);
            }
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        indentingPrintWriter.println(this.mTag + " current ProvisioningConfiguration:");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println(Objects.toString(provisioningConfiguration, "N/A"));
        indentingPrintWriter.decreaseIndent();
        IpReachabilityMonitor ipReachabilityMonitor = this.mIpReachabilityMonitor;
        if (ipReachabilityMonitor != null) {
            indentingPrintWriter.println();
            indentingPrintWriter.println(this.mTag + " current IpReachabilityMonitor state:");
            indentingPrintWriter.increaseIndent();
            ipReachabilityMonitor.dump(indentingPrintWriter);
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.println();
        indentingPrintWriter.println(this.mTag + " StateMachine dump:");
        indentingPrintWriter.increaseIndent();
        this.mLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println();
        indentingPrintWriter.println(this.mTag + " connectivity packet log:");
        indentingPrintWriter.println();
        indentingPrintWriter.println("Debug with python and scapy via:");
        indentingPrintWriter.println("shell$ python");
        indentingPrintWriter.println(">>> from scapy import all as scapy");
        indentingPrintWriter.println(">>> scapy.Ether(\"<paste_hex_string>\".decode(\"hex\")).show2()");
        indentingPrintWriter.println();
        indentingPrintWriter.increaseIndent();
        this.mConnectivityPacketLog.readOnlyLocalLog().dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
    }

    protected String getWhatToString(int i) {
        return sWhatToString.get(i, "UNKNOWN: " + Integer.toString(i));
    }

    protected String getLogRecString(Message message) {
        Object[] objArr = new Object[6];
        objArr[0] = this.mInterfaceName;
        objArr[1] = Integer.valueOf(this.mInterfaceParams == null ? -1 : this.mInterfaceParams.index);
        objArr[2] = Integer.valueOf(message.arg1);
        objArr[3] = Integer.valueOf(message.arg2);
        objArr[4] = Objects.toString(message.obj);
        objArr[5] = this.mMsgStateLogger;
        String str = String.format("%s/%d %d %d %s [%s]", objArr);
        String str2 = getWhatToString(message.what) + " " + str;
        this.mLog.log(str2);
        if (DBG) {
            Log.d(this.mTag, str2);
        }
        this.mMsgStateLogger.reset();
        return str;
    }

    protected boolean recordLogRec(Message message) {
        boolean z = message.what != 6;
        if (!z) {
            this.mMsgStateLogger.reset();
        }
        return z;
    }

    private void logError(String str, Object... objArr) {
        String str2 = "ERROR " + String.format(str, objArr);
        Log.e(this.mTag, str2);
        this.mLog.log(str2);
    }

    private void resetLinkProperties() {
        this.mNetlinkTracker.clearLinkProperties();
        this.mConfiguration = null;
        this.mDhcpResults = null;
        this.mTcpBufferSizes = "";
        this.mHttpProxy = null;
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
    }

    private void recordMetric(int i) {
        this.mMetricsLog.log(this.mInterfaceName, new IpManagerEvent(i, this.mStartTimeMillis > 0 ? SystemClock.elapsedRealtime() - this.mStartTimeMillis : 0L));
    }

    @VisibleForTesting
    static boolean isProvisioned(LinkProperties linkProperties, InitialConfiguration initialConfiguration) {
        if (linkProperties.hasIPv4Address() || linkProperties.isProvisioned()) {
            return true;
        }
        if (initialConfiguration == null) {
            return false;
        }
        return initialConfiguration.isProvisionedBy(linkProperties.getLinkAddresses(), linkProperties.getRoutes());
    }

    private LinkProperties.ProvisioningChange compareProvisioning(LinkProperties linkProperties, LinkProperties linkProperties2) {
        LinkProperties.ProvisioningChange provisioningChange;
        InitialConfiguration initialConfiguration = this.mConfiguration != null ? this.mConfiguration.mInitialConfig : null;
        boolean zIsProvisioned = isProvisioned(linkProperties, initialConfiguration);
        boolean zIsProvisioned2 = isProvisioned(linkProperties2, initialConfiguration);
        if (!zIsProvisioned && zIsProvisioned2) {
            provisioningChange = LinkProperties.ProvisioningChange.GAINED_PROVISIONING;
        } else if (zIsProvisioned && zIsProvisioned2) {
            provisioningChange = LinkProperties.ProvisioningChange.STILL_PROVISIONED;
        } else if (!zIsProvisioned && !zIsProvisioned2) {
            provisioningChange = LinkProperties.ProvisioningChange.STILL_NOT_PROVISIONED;
        } else {
            provisioningChange = LinkProperties.ProvisioningChange.LOST_PROVISIONING;
        }
        boolean z = false;
        boolean z2 = linkProperties.isIPv6Provisioned() && !linkProperties2.isIPv6Provisioned();
        boolean z3 = linkProperties.hasIPv4Address() && !linkProperties2.hasIPv4Address();
        boolean z4 = linkProperties.hasIPv6DefaultRoute() && !linkProperties2.hasIPv6DefaultRoute();
        if (this.mMultinetworkPolicyTracker != null && !this.mMultinetworkPolicyTracker.getAvoidBadWifi()) {
            z = true;
        }
        if (z3 || (z2 && !z)) {
            provisioningChange = LinkProperties.ProvisioningChange.LOST_PROVISIONING;
        }
        if (linkProperties.hasGlobalIPv6Address() && z4 && !z) {
            return LinkProperties.ProvisioningChange.LOST_PROVISIONING;
        }
        return provisioningChange;
    }

    static class AnonymousClass5 {
        static final int[] $SwitchMap$android$net$LinkProperties$ProvisioningChange = new int[LinkProperties.ProvisioningChange.values().length];

        static {
            try {
                $SwitchMap$android$net$LinkProperties$ProvisioningChange[LinkProperties.ProvisioningChange.GAINED_PROVISIONING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$LinkProperties$ProvisioningChange[LinkProperties.ProvisioningChange.LOST_PROVISIONING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private void dispatchCallback(LinkProperties.ProvisioningChange provisioningChange, LinkProperties linkProperties) {
        switch (AnonymousClass5.$SwitchMap$android$net$LinkProperties$ProvisioningChange[provisioningChange.ordinal()]) {
            case 1:
                if (DBG) {
                    Log.d(this.mTag, "onProvisioningSuccess()");
                }
                recordMetric(1);
                this.mCallback.onProvisioningSuccess(linkProperties);
                break;
            case 2:
                if (DBG) {
                    Log.d(this.mTag, "onProvisioningFailure()");
                }
                recordMetric(2);
                this.mCallback.onProvisioningFailure(linkProperties);
                break;
            default:
                if (DBG) {
                    Log.d(this.mTag, "onLinkPropertiesChange()");
                }
                this.mCallback.onLinkPropertiesChange(linkProperties);
                break;
        }
    }

    private LinkProperties.ProvisioningChange setLinkProperties(LinkProperties linkProperties) {
        if (this.mApfFilter != null) {
            this.mApfFilter.setLinkProperties(linkProperties);
        }
        if (this.mIpReachabilityMonitor != null) {
            this.mIpReachabilityMonitor.updateLinkProperties(linkProperties);
        }
        LinkProperties.ProvisioningChange provisioningChangeCompareProvisioning = compareProvisioning(this.mLinkProperties, linkProperties);
        this.mLinkProperties = new LinkProperties(linkProperties);
        if (provisioningChangeCompareProvisioning == LinkProperties.ProvisioningChange.GAINED_PROVISIONING) {
            this.mProvisioningTimeoutAlarm.cancel();
        }
        return provisioningChangeCompareProvisioning;
    }

    private LinkProperties assembleLinkProperties() {
        LinkProperties linkProperties = new LinkProperties();
        linkProperties.setInterfaceName(this.mInterfaceName);
        LinkProperties linkProperties2 = this.mNetlinkTracker.getLinkProperties();
        linkProperties.setLinkAddresses(linkProperties2.getLinkAddresses());
        Iterator<RouteInfo> it = linkProperties2.getRoutes().iterator();
        while (it.hasNext()) {
            linkProperties.addRoute(it.next());
        }
        addAllReachableDnsServers(linkProperties, linkProperties2.getDnsServers());
        if (this.mDhcpResults != null) {
            Iterator it2 = this.mDhcpResults.getRoutes(this.mInterfaceName).iterator();
            while (it2.hasNext()) {
                linkProperties.addRoute((RouteInfo) it2.next());
            }
            addAllReachableDnsServers(linkProperties, ((StaticIpConfiguration) this.mDhcpResults).dnsServers);
            linkProperties.setDomains(((StaticIpConfiguration) this.mDhcpResults).domains);
            if (this.mDhcpResults.mtu != 0) {
                linkProperties.setMtu(this.mDhcpResults.mtu);
            }
        }
        if (!TextUtils.isEmpty(this.mTcpBufferSizes)) {
            linkProperties.setTcpBufferSizes(this.mTcpBufferSizes);
        }
        if (this.mHttpProxy != null) {
            linkProperties.setHttpProxy(this.mHttpProxy);
        }
        if (this.mConfiguration != null && this.mConfiguration.mInitialConfig != null) {
            InitialConfiguration initialConfiguration = this.mConfiguration.mInitialConfig;
            if (initialConfiguration.isProvisionedBy(linkProperties.getLinkAddresses(), null)) {
                Iterator<IpPrefix> it3 = initialConfiguration.directlyConnectedRoutes.iterator();
                while (it3.hasNext()) {
                    linkProperties.addRoute(new RouteInfo(it3.next(), null, this.mInterfaceName));
                }
            }
            addAllReachableDnsServers(linkProperties, initialConfiguration.dnsServers);
        }
        LinkProperties linkProperties3 = this.mLinkProperties;
        if (DBG) {
            Log.d(this.mTag, String.format("Netlink-seen LPs: %s, new LPs: %s; old LPs: %s", linkProperties2, linkProperties, linkProperties3));
        }
        return linkProperties;
    }

    private static void addAllReachableDnsServers(LinkProperties linkProperties, Iterable<InetAddress> iterable) {
        for (InetAddress inetAddress : iterable) {
            if (!inetAddress.isAnyLocalAddress() && linkProperties.isReachable(inetAddress)) {
                linkProperties.addDnsServer(inetAddress);
            }
        }
    }

    private boolean handleLinkPropertiesUpdate(boolean z) {
        LinkProperties linkPropertiesAssembleLinkProperties = assembleLinkProperties();
        if (Objects.equals(linkPropertiesAssembleLinkProperties, this.mLinkProperties)) {
            return true;
        }
        LinkProperties.ProvisioningChange linkProperties = setLinkProperties(linkPropertiesAssembleLinkProperties);
        if (z) {
            dispatchCallback(linkProperties, linkPropertiesAssembleLinkProperties);
        }
        return linkProperties != LinkProperties.ProvisioningChange.LOST_PROVISIONING;
    }

    private void handleIPv4Success(DhcpResults dhcpResults) {
        this.mDhcpResults = new DhcpResults(dhcpResults);
        LinkProperties linkPropertiesAssembleLinkProperties = assembleLinkProperties();
        LinkProperties.ProvisioningChange linkProperties = setLinkProperties(linkPropertiesAssembleLinkProperties);
        if (DBG) {
            Log.d(this.mTag, "onNewDhcpResults(" + Objects.toString(dhcpResults) + ")");
        }
        this.mCallback.onNewDhcpResults(dhcpResults);
        dispatchCallback(linkProperties, linkPropertiesAssembleLinkProperties);
    }

    private void handleIPv4Failure() {
        this.mInterfaceCtrl.clearIPv4Address();
        this.mDhcpResults = null;
        if (DBG) {
            Log.d(this.mTag, "onNewDhcpResults(null)");
        }
        this.mCallback.onNewDhcpResults(null);
        handleProvisioningFailure();
    }

    private void handleProvisioningFailure() {
        LinkProperties linkPropertiesAssembleLinkProperties = assembleLinkProperties();
        LinkProperties.ProvisioningChange linkProperties = setLinkProperties(linkPropertiesAssembleLinkProperties);
        if (linkProperties == LinkProperties.ProvisioningChange.STILL_NOT_PROVISIONED) {
            linkProperties = LinkProperties.ProvisioningChange.LOST_PROVISIONING;
        }
        dispatchCallback(linkProperties, linkPropertiesAssembleLinkProperties);
        if (linkProperties == LinkProperties.ProvisioningChange.LOST_PROVISIONING) {
            transitionTo(this.mStoppingState);
        }
    }

    private void doImmediateProvisioningFailure(int i) {
        logError("onProvisioningFailure(): %s", Integer.valueOf(i));
        recordMetric(i);
        this.mCallback.onProvisioningFailure(new LinkProperties(this.mLinkProperties));
    }

    private boolean startIPv4() {
        if (this.mConfiguration.mStaticIpConfig != null) {
            if (this.mInterfaceCtrl.setIPv4Address(this.mConfiguration.mStaticIpConfig.ipAddress)) {
                handleIPv4Success(new DhcpResults(this.mConfiguration.mStaticIpConfig));
                return true;
            }
            return false;
        }
        this.mDhcpClient = DhcpClient.makeDhcpClient(this.mContext, this, this.mInterfaceParams);
        this.mDhcpClient.registerForPreDhcpNotification();
        this.mDhcpClient.sendMessage(196609);
        return true;
    }

    private boolean startIPv6() {
        return this.mInterfaceCtrl.setIPv6PrivacyExtensions(true) && this.mInterfaceCtrl.setIPv6AddrGenModeIfSupported(this.mConfiguration.mIPv6AddrGenMode) && this.mInterfaceCtrl.enableIPv6();
    }

    private boolean applyInitialConfig(InitialConfiguration initialConfiguration) {
        Iterator it = findAll(initialConfiguration.ipAddresses, new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return ((LinkAddress) obj).isIPv6();
            }
        }).iterator();
        while (it.hasNext()) {
            if (!this.mInterfaceCtrl.addAddress((LinkAddress) it.next())) {
                return false;
            }
        }
        return true;
    }

    private boolean startIpReachabilityMonitor() {
        try {
            this.mIpReachabilityMonitor = new IpReachabilityMonitor(this.mContext, this.mInterfaceParams, getHandler(), this.mLog, new IpReachabilityMonitor.Callback() {
                @Override
                public void notifyLost(InetAddress inetAddress, String str) {
                    IpClient.this.mCallback.onReachabilityLost(str);
                }
            }, this.mMultinetworkPolicyTracker);
        } catch (IllegalArgumentException e) {
            logError("IpReachabilityMonitor failure: %s", e);
            this.mIpReachabilityMonitor = null;
        }
        return this.mIpReachabilityMonitor != null;
    }

    private void stopAllIP() {
        this.mInterfaceCtrl.disableIPv6();
        this.mInterfaceCtrl.clearAllAddresses();
    }

    class StoppedState extends State {
        StoppedState() {
        }

        public void enter() {
            IpClient.this.stopAllIP();
            IpClient.this.resetLinkProperties();
            if (IpClient.this.mStartTimeMillis > 0) {
                IpClient.this.recordMetric(3);
                IpClient.this.mStartTimeMillis = 0L;
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 196613) {
                switch (i) {
                    case 1:
                        IpClient.this.stopStateMachineUpdaters();
                        IpClient.this.quit();
                        break;
                    case 2:
                        break;
                    case 3:
                        IpClient.this.mConfiguration = (ProvisioningConfiguration) message.obj;
                        IpClient.this.transitionTo(IpClient.this.mStartedState);
                        break;
                    default:
                        switch (i) {
                            case 6:
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case 7:
                                IpClient.this.mTcpBufferSizes = (String) message.obj;
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case 8:
                                IpClient.this.mHttpProxy = (ProxyInfo) message.obj;
                                IpClient.this.handleLinkPropertiesUpdate(false);
                                break;
                            case 9:
                                IpClient.this.mMulticastFiltering = ((Boolean) message.obj).booleanValue();
                                break;
                            default:
                                return false;
                        }
                        break;
                }
            } else {
                IpClient.this.logError("Unexpected CMD_ON_QUIT (already stopped).", new Object[0]);
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    class StoppingState extends State {
        StoppingState() {
        }

        public void enter() {
            if (IpClient.this.mDhcpClient == null) {
                IpClient.this.transitionTo(IpClient.this.mStoppedState);
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 2) {
                if (i == 196613) {
                    IpClient.this.mDhcpClient = null;
                    IpClient.this.transitionTo(IpClient.this.mStoppedState);
                } else if (i == 196615) {
                    IpClient.this.mInterfaceCtrl.clearIPv4Address();
                } else {
                    IpClient.this.deferMessage(message);
                }
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    class StartedState extends State {
        StartedState() {
        }

        public void enter() {
            IpClient.this.mStartTimeMillis = SystemClock.elapsedRealtime();
            if (IpClient.this.mConfiguration.mProvisioningTimeoutMs > 0) {
                IpClient.this.mProvisioningTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) IpClient.this.mConfiguration.mProvisioningTimeoutMs));
            }
            if (readyToProceed()) {
                IpClient.this.transitionTo(IpClient.this.mRunningState);
            } else {
                IpClient.this.stopAllIP();
            }
        }

        public void exit() {
            IpClient.this.mProvisioningTimeoutAlarm.cancel();
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 2) {
                IpClient.this.transitionTo(IpClient.this.mStoppingState);
            } else if (i == 6) {
                IpClient.this.handleLinkPropertiesUpdate(false);
                if (readyToProceed()) {
                    IpClient.this.transitionTo(IpClient.this.mRunningState);
                }
            } else if (i == 10) {
                IpClient.this.handleProvisioningFailure();
            } else {
                IpClient.this.deferMessage(message);
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }

        boolean readyToProceed() {
            return (IpClient.this.mLinkProperties.hasIPv4Address() || IpClient.this.mLinkProperties.hasGlobalIPv6Address()) ? false : true;
        }
    }

    class RunningState extends State {
        private boolean mDhcpActionInFlight;
        private ConnectivityPacketTracker mPacketTracker;

        RunningState() {
        }

        public void enter() {
            ApfFilter.ApfConfiguration apfConfiguration = new ApfFilter.ApfConfiguration();
            apfConfiguration.apfCapabilities = IpClient.this.mConfiguration.mApfCapabilities;
            apfConfiguration.multicastFilter = IpClient.this.mMulticastFiltering;
            apfConfiguration.ieee802_3Filter = IpClient.this.mContext.getResources().getBoolean(R.^attr-private.backgroundRequestDetail);
            apfConfiguration.ethTypeBlackList = IpClient.this.mContext.getResources().getIntArray(R.array.config_allowedSystemInstantAppSettings);
            IpClient.this.mApfFilter = ApfFilter.maybeCreate(IpClient.this.mContext, apfConfiguration, IpClient.this.mInterfaceParams, IpClient.this.mCallback);
            if (IpClient.this.mApfFilter == null) {
                IpClient.this.mCallback.setFallbackMulticastFilter(IpClient.this.mMulticastFiltering);
            }
            this.mPacketTracker = createPacketTracker();
            if (this.mPacketTracker != null) {
                this.mPacketTracker.start(IpClient.this.mConfiguration.mDisplayName);
            }
            if (!IpClient.this.mConfiguration.mEnableIPv6 || IpClient.this.startIPv6()) {
                if (!IpClient.this.mConfiguration.mEnableIPv4 || IpClient.this.startIPv4()) {
                    InitialConfiguration initialConfiguration = IpClient.this.mConfiguration.mInitialConfig;
                    if (initialConfiguration == null || IpClient.this.applyInitialConfig(initialConfiguration)) {
                        if (IpClient.this.mConfiguration.mUsingMultinetworkPolicyTracker) {
                            IpClient.this.mMultinetworkPolicyTracker = new MultinetworkPolicyTracker(IpClient.this.mContext, IpClient.this.getHandler(), new Runnable() {
                                @Override
                                public final void run() {
                                    IpClient.this.mLog.log("OBSERVED AvoidBadWifi changed");
                                }
                            });
                            IpClient.this.mMultinetworkPolicyTracker.start();
                        }
                        if (IpClient.this.mConfiguration.mUsingIpReachabilityMonitor && !IpClient.this.startIpReachabilityMonitor()) {
                            IpClient.this.doImmediateProvisioningFailure(6);
                            IpClient.this.transitionTo(IpClient.this.mStoppingState);
                            return;
                        }
                        return;
                    }
                    IpClient.this.doImmediateProvisioningFailure(7);
                    IpClient.this.transitionTo(IpClient.this.mStoppingState);
                    return;
                }
                IpClient.this.doImmediateProvisioningFailure(4);
                IpClient.this.transitionTo(IpClient.this.mStoppingState);
                return;
            }
            IpClient.this.doImmediateProvisioningFailure(5);
            IpClient.this.transitionTo(IpClient.this.mStoppingState);
        }

        public void exit() {
            stopDhcpAction();
            if (IpClient.this.mIpReachabilityMonitor != null) {
                IpClient.this.mIpReachabilityMonitor.stop();
                IpClient.this.mIpReachabilityMonitor = null;
            }
            if (IpClient.this.mMultinetworkPolicyTracker != null) {
                IpClient.this.mMultinetworkPolicyTracker.shutdown();
                IpClient.this.mMultinetworkPolicyTracker = null;
            }
            if (IpClient.this.mDhcpClient != null) {
                IpClient.this.mDhcpClient.sendMessage(196610);
                IpClient.this.mDhcpClient.doQuit();
            }
            if (this.mPacketTracker != null) {
                this.mPacketTracker.stop();
                this.mPacketTracker = null;
            }
            if (IpClient.this.mApfFilter != null) {
                IpClient.this.mApfFilter.shutdown();
                IpClient.this.mApfFilter = null;
            }
            IpClient.this.resetLinkProperties();
        }

        private ConnectivityPacketTracker createPacketTracker() {
            try {
                return new ConnectivityPacketTracker(IpClient.this.getHandler(), IpClient.this.mInterfaceParams, IpClient.this.mConnectivityPacketLog);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private void ensureDhcpAction() {
            if (!this.mDhcpActionInFlight) {
                IpClient.this.mCallback.onPreDhcpAction();
                this.mDhcpActionInFlight = true;
                IpClient.this.mDhcpActionTimeoutAlarm.schedule(SystemClock.elapsedRealtime() + ((long) IpClient.this.mConfiguration.mRequestedPreDhcpActionMs));
            }
        }

        private void stopDhcpAction() {
            IpClient.this.mDhcpActionTimeoutAlarm.cancel();
            if (this.mDhcpActionInFlight) {
                IpClient.this.mCallback.onPostDhcpAction();
                this.mDhcpActionInFlight = false;
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            switch (i) {
                case 2:
                    IpClient.this.transitionTo(IpClient.this.mStoppingState);
                    break;
                case 3:
                    IpClient.this.logError("ALERT: START received in StartedState. Please fix caller.", new Object[0]);
                    break;
                case 4:
                    if (IpClient.this.mIpReachabilityMonitor != null) {
                        IpClient.this.mIpReachabilityMonitor.probeAll();
                    }
                    break;
                case 5:
                    if (IpClient.this.mDhcpClient != null) {
                        IpClient.this.mDhcpClient.sendMessage(196614);
                    }
                    break;
                case 6:
                    if (!IpClient.this.handleLinkPropertiesUpdate(true)) {
                        IpClient.this.transitionTo(IpClient.this.mStoppingState);
                    }
                    break;
                case 7:
                    IpClient.this.mTcpBufferSizes = (String) message.obj;
                    IpClient.this.handleLinkPropertiesUpdate(true);
                    break;
                case 8:
                    IpClient.this.mHttpProxy = (ProxyInfo) message.obj;
                    IpClient.this.handleLinkPropertiesUpdate(true);
                    break;
                case 9:
                    IpClient.this.mMulticastFiltering = ((Boolean) message.obj).booleanValue();
                    if (IpClient.this.mApfFilter != null) {
                        IpClient.this.mApfFilter.setMulticastFilter(IpClient.this.mMulticastFiltering);
                    } else {
                        IpClient.this.mCallback.setFallbackMulticastFilter(IpClient.this.mMulticastFiltering);
                    }
                    break;
                default:
                    switch (i) {
                        case 11:
                            stopDhcpAction();
                            break;
                        case 12:
                            if (IpClient.this.mApfFilter != null) {
                                IpClient.this.mApfFilter.setDataSnapshot((byte[]) message.obj);
                            }
                            IpClient.this.mApfDataSnapshotComplete.open();
                            break;
                        default:
                            switch (i) {
                                case 196611:
                                    if (IpClient.this.mConfiguration.mRequestedPreDhcpActionMs > 0) {
                                        ensureDhcpAction();
                                    } else {
                                        IpClient.this.sendMessage(5);
                                    }
                                    break;
                                case 196612:
                                    stopDhcpAction();
                                    switch (message.arg1) {
                                        case 1:
                                            IpClient.this.handleIPv4Success((DhcpResults) message.obj);
                                            break;
                                        case 2:
                                            IpClient.this.handleIPv4Failure();
                                            break;
                                        default:
                                            IpClient.this.logError("Unknown CMD_POST_DHCP_ACTION status: %s", Integer.valueOf(message.arg1));
                                            break;
                                    }
                                    break;
                                case 196613:
                                    IpClient.this.logError("Unexpected CMD_ON_QUIT.", new Object[0]);
                                    IpClient.this.mDhcpClient = null;
                                    break;
                                default:
                                    switch (i) {
                                        case 196615:
                                            IpClient.this.mInterfaceCtrl.clearIPv4Address();
                                            break;
                                        case 196616:
                                            if (IpClient.this.mInterfaceCtrl.setIPv4Address((LinkAddress) message.obj)) {
                                                IpClient.this.mDhcpClient.sendMessage(196617);
                                            } else {
                                                IpClient.this.logError("Failed to set IPv4 address.", new Object[0]);
                                                IpClient.this.dispatchCallback(LinkProperties.ProvisioningChange.LOST_PROVISIONING, new LinkProperties(IpClient.this.mLinkProperties));
                                                IpClient.this.transitionTo(IpClient.this.mStoppingState);
                                            }
                                            break;
                                        default:
                                            return false;
                                    }
                                    break;
                            }
                            break;
                    }
                    break;
            }
            IpClient.this.mMsgStateLogger.handled(this, IpClient.this.getCurrentState());
            return true;
        }
    }

    private static class MessageHandlingLogger {
        public String processedInState;
        public String receivedInState;

        private MessageHandlingLogger() {
        }

        MessageHandlingLogger(AnonymousClass1 anonymousClass1) {
            this();
        }

        public void reset() {
            this.processedInState = null;
            this.receivedInState = null;
        }

        public void handled(State state, IState iState) {
            this.processedInState = state.getClass().getSimpleName();
            this.receivedInState = iState.getName();
        }

        public String toString() {
            return String.format("rcvd_in=%s, proc_in=%s", this.receivedInState, this.processedInState);
        }
    }

    static <T> boolean any(Iterable<T> iterable, Predicate<T> predicate) {
        Iterator<T> it = iterable.iterator();
        while (it.hasNext()) {
            if (predicate.test(it.next())) {
                return true;
            }
        }
        return false;
    }

    static <T> String join(String str, Collection<T> collection) {
        return (String) collection.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return obj.toString();
            }
        }).collect(Collectors.joining(str));
    }

    static <T> List<T> findAll(Collection<T> collection, Predicate<T> predicate) {
        return (List) collection.stream().filter(predicate).collect(Collectors.toList());
    }
}
