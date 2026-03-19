package android.net.ip;

import android.content.Context;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;
import android.net.apf.ApfFilter;
import android.net.dhcp.Dhcp6Client;
import android.net.dhcp.DhcpClient;
import android.net.ip.IpReachabilityMonitor;
import android.net.metrics.IpManagerEvent;
import android.net.wifi.WifiConfiguration;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.internal.util.WakeupMessage;
import com.android.server.net.NetlinkTracker;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringJoiner;

public class IpManager extends StateMachine {

    private static final int[] f0androidnetLinkProperties$ProvisioningChangeSwitchesValues = null;
    private static final String CLAT_PREFIX = "v4-";
    private static final int CMD_CONFIRM = 3;
    private static final int CMD_SET_MULTICAST_FILTER = 8;
    private static final int CMD_START = 2;
    private static final int CMD_STOP = 1;
    private static final int CMD_UPDATE_HTTP_PROXY = 7;
    private static final int CMD_UPDATE_TCP_BUFFER_SIZES = 6;
    private static final boolean DBG = true;
    public static final String DUMP_ARG = "ipmanager";
    private static final int EVENT_DHCPACTION_TIMEOUT = 10;
    private static final int EVENT_NETLINK_LINKPROPERTIES_CHANGED = 5;
    private static final int EVENT_PRE_DHCP_ACTION_COMPLETE = 4;
    private static final int EVENT_PROVISIONING_TIMEOUT = 9;
    private static final int MAX_LOG_RECORDS = 500;
    private static final boolean NO_CALLBACKS = false;
    private static final boolean SEND_CALLBACKS = true;
    private static final boolean VDBG = true;
    private ApfFilter mApfFilter;
    protected final Callback mCallback;
    private final String mClatInterfaceName;
    private ProvisioningConfiguration mConfiguration;
    private final Context mContext;
    private Dhcp6Client mDhcp6Client;
    private final WakeupMessage mDhcpActionTimeoutAlarm;
    private DhcpClient mDhcpClient;
    private DhcpResults mDhcpResults;
    private ArrayList<InetAddress> mDnsV6Servers;
    private ProxyInfo mHttpProxy;
    private final String mInterfaceName;
    private IpReachabilityMonitor mIpReachabilityMonitor;
    private boolean mIsStartCalled;
    private boolean mIsWifiSMStarted;
    private LinkProperties mLinkProperties;
    private final LocalLog mLocalLog;
    private boolean mMulticastFiltering;
    private final NetlinkTracker mNetlinkTracker;
    private NetworkInterface mNetworkInterface;
    private final INetworkManagementService mNwService;
    private DhcpResults mPastSuccessedDhcpResult;
    private final WakeupMessage mProvisioningTimeoutAlarm;
    private long mStartTimeMillis;
    private final State mStartedState;
    private final State mStoppedState;
    private final State mStoppingState;
    private final String mTag;
    private String mTcpBufferSizes;
    private static final Class[] sMessageClasses = {IpManager.class, DhcpClient.class};
    private static final SparseArray<String> sWhatToString = MessageUtils.findMessageNames(sMessageClasses);
    private static final boolean sMtkDhcpv6cWifi = SystemProperties.get("ro.mtk_dhcpv6c_wifi").equals("1");

    private static int[] m115getandroidnetLinkProperties$ProvisioningChangeSwitchesValues() {
        if (f0androidnetLinkProperties$ProvisioningChangeSwitchesValues != null) {
            return f0androidnetLinkProperties$ProvisioningChangeSwitchesValues;
        }
        int[] iArr = new int[LinkProperties.ProvisioningChange.values().length];
        try {
            iArr[LinkProperties.ProvisioningChange.GAINED_PROVISIONING.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[LinkProperties.ProvisioningChange.LOST_PROVISIONING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[LinkProperties.ProvisioningChange.STILL_NOT_PROVISIONED.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[LinkProperties.ProvisioningChange.STILL_PROVISIONED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f0androidnetLinkProperties$ProvisioningChangeSwitchesValues = iArr;
        return iArr;
    }

    public static class Callback {
        public void onPreDhcpAction() {
        }

        public void onPostDhcpAction() {
        }

        public void onNewDhcpResults(DhcpResults dhcpResults) {
        }

        public void onProvisioningSuccess(LinkProperties newLp) {
        }

        public void onProvisioningFailure(LinkProperties newLp) {
        }

        public void onLinkPropertiesChange(LinkProperties newLp) {
        }

        public void onReachabilityLost(String logMsg) {
        }

        public void onQuit() {
        }

        public void installPacketFilter(byte[] filter) {
        }

        public void setFallbackMulticastFilter(boolean enabled) {
        }

        public void setNeighborDiscoveryOffload(boolean enable) {
        }
    }

    public static class WaitForProvisioningCallback extends Callback {
        private LinkProperties mCallbackLinkProperties;

        public LinkProperties waitForProvisioning() {
            LinkProperties linkProperties;
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
                linkProperties = this.mCallbackLinkProperties;
            }
            return linkProperties;
        }

        @Override
        public void onProvisioningSuccess(LinkProperties newLp) {
            synchronized (this) {
                this.mCallbackLinkProperties = newLp;
                notify();
            }
        }

        @Override
        public void onProvisioningFailure(LinkProperties newLp) {
            synchronized (this) {
                this.mCallbackLinkProperties = null;
                notify();
            }
        }
    }

    private class LoggingCallbackWrapper extends Callback {
        private static final String PREFIX = "INVOKE ";
        private Callback mCallback;

        public LoggingCallbackWrapper(Callback callback) {
            this.mCallback = callback;
        }

        private void log(String msg) {
            IpManager.this.mLocalLog.log(PREFIX + msg);
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
        public void onProvisioningSuccess(LinkProperties newLp) {
            this.mCallback.onProvisioningSuccess(newLp);
            log("onProvisioningSuccess({" + newLp + "})");
        }

        @Override
        public void onProvisioningFailure(LinkProperties newLp) {
            this.mCallback.onProvisioningFailure(newLp);
            log("onProvisioningFailure({" + newLp + "})");
        }

        @Override
        public void onLinkPropertiesChange(LinkProperties newLp) {
            this.mCallback.onLinkPropertiesChange(newLp);
            log("onLinkPropertiesChange({" + newLp + "})");
        }

        @Override
        public void onReachabilityLost(String logMsg) {
            this.mCallback.onReachabilityLost(logMsg);
            log("onReachabilityLost(" + logMsg + ")");
        }

        @Override
        public void onQuit() {
            this.mCallback.onQuit();
            log("onQuit()");
        }

        @Override
        public void installPacketFilter(byte[] filter) {
            this.mCallback.installPacketFilter(filter);
            log("installPacketFilter(byte[" + filter.length + "])");
        }

        @Override
        public void setFallbackMulticastFilter(boolean enabled) {
            this.mCallback.setFallbackMulticastFilter(enabled);
            log("setFallbackMulticastFilter(" + enabled + ")");
        }

        @Override
        public void setNeighborDiscoveryOffload(boolean enable) {
            this.mCallback.setNeighborDiscoveryOffload(enable);
            log("setNeighborDiscoveryOffload(" + enable + ")");
        }
    }

    public static class ProvisioningConfiguration {
        private static final int DEFAULT_TIMEOUT_MS = 36000;
        ApfCapabilities mApfCapabilities;
        boolean mEnableIPv4;
        boolean mEnableIPv6;
        int mProvisioningTimeoutMs;
        int mRequestedPreDhcpActionMs;
        StaticIpConfiguration mStaticIpConfig;
        boolean mUsingIpReachabilityMonitor;

        public static class Builder {
            private ProvisioningConfiguration mConfig = new ProvisioningConfiguration();

            public Builder withoutIPv4() {
                this.mConfig.mEnableIPv4 = false;
                return this;
            }

            public Builder withoutIPv6() {
                this.mConfig.mEnableIPv6 = false;
                return this;
            }

            public Builder withoutIpReachabilityMonitor() {
                this.mConfig.mUsingIpReachabilityMonitor = false;
                return this;
            }

            public Builder withPreDhcpAction() {
                this.mConfig.mRequestedPreDhcpActionMs = ProvisioningConfiguration.DEFAULT_TIMEOUT_MS;
                return this;
            }

            public Builder withPreDhcpAction(int dhcpActionTimeoutMs) {
                this.mConfig.mRequestedPreDhcpActionMs = dhcpActionTimeoutMs;
                return this;
            }

            public Builder withStaticConfiguration(StaticIpConfiguration staticConfig) {
                this.mConfig.mStaticIpConfig = staticConfig;
                return this;
            }

            public Builder withApfCapabilities(ApfCapabilities apfCapabilities) {
                this.mConfig.mApfCapabilities = apfCapabilities;
                return this;
            }

            public Builder withProvisioningTimeoutMs(int timeoutMs) {
                this.mConfig.mProvisioningTimeoutMs = timeoutMs;
                return this;
            }

            public ProvisioningConfiguration build() {
                return new ProvisioningConfiguration(this.mConfig);
            }
        }

        public ProvisioningConfiguration() {
            this.mEnableIPv4 = true;
            this.mEnableIPv6 = true;
            this.mUsingIpReachabilityMonitor = true;
            this.mProvisioningTimeoutMs = DEFAULT_TIMEOUT_MS;
        }

        public ProvisioningConfiguration(ProvisioningConfiguration other) {
            this.mEnableIPv4 = true;
            this.mEnableIPv6 = true;
            this.mUsingIpReachabilityMonitor = true;
            this.mProvisioningTimeoutMs = DEFAULT_TIMEOUT_MS;
            this.mEnableIPv4 = other.mEnableIPv4;
            this.mEnableIPv6 = other.mEnableIPv6;
            this.mUsingIpReachabilityMonitor = other.mUsingIpReachabilityMonitor;
            this.mRequestedPreDhcpActionMs = other.mRequestedPreDhcpActionMs;
            this.mStaticIpConfig = other.mStaticIpConfig;
            this.mApfCapabilities = other.mApfCapabilities;
            this.mProvisioningTimeoutMs = other.mProvisioningTimeoutMs;
        }

        public String toString() {
            return new StringJoiner(", ", getClass().getSimpleName() + "{", "}").add("mEnableIPv4: " + this.mEnableIPv4).add("mEnableIPv6: " + this.mEnableIPv6).add("mUsingIpReachabilityMonitor: " + this.mUsingIpReachabilityMonitor).add("mRequestedPreDhcpActionMs: " + this.mRequestedPreDhcpActionMs).add("mStaticIpConfig: " + this.mStaticIpConfig).add("mApfCapabilities: " + this.mApfCapabilities).add("mProvisioningTimeoutMs: " + this.mProvisioningTimeoutMs).toString();
        }
    }

    public IpManager(Context context, String ifName, Callback callback) throws IllegalArgumentException {
        this(context, ifName, callback, INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management")));
    }

    public IpManager(Context context, String ifName, Callback callback, INetworkManagementService nwService) throws IllegalArgumentException {
        super(IpManager.class.getSimpleName() + "." + ifName);
        this.mStoppedState = new StoppedState();
        this.mStoppingState = new StoppingState();
        this.mStartedState = new StartedState();
        this.mIsWifiSMStarted = false;
        this.mIsStartCalled = false;
        this.mTag = getName();
        this.mContext = context;
        this.mInterfaceName = ifName;
        this.mClatInterfaceName = CLAT_PREFIX + ifName;
        this.mCallback = new LoggingCallbackWrapper(callback);
        this.mNwService = nwService;
        this.mNetlinkTracker = new NetlinkTracker(this.mInterfaceName, new NetlinkTracker.Callback() {
            public void update() {
                if (!IpManager.this.mIsStartCalled) {
                    return;
                }
                IpManager.this.sendMessage(5);
            }
        }) {
            public void interfaceAdded(String iface) {
                super.interfaceAdded(iface);
                if (!IpManager.this.mClatInterfaceName.equals(iface)) {
                    return;
                }
                IpManager.this.mCallback.setNeighborDiscoveryOffload(false);
            }

            public void interfaceRemoved(String iface) {
                super.interfaceRemoved(iface);
                if (!IpManager.this.mClatInterfaceName.equals(iface)) {
                    return;
                }
                IpManager.this.mCallback.setNeighborDiscoveryOffload(true);
            }
        };
        try {
            this.mNwService.registerObserver(this.mNetlinkTracker);
        } catch (RemoteException e) {
            Log.e(this.mTag, "Couldn't register NetlinkTracker: " + e.toString());
        }
        resetLinkProperties();
        this.mProvisioningTimeoutAlarm = new WakeupMessage(this.mContext, getHandler(), this.mTag + ".EVENT_PROVISIONING_TIMEOUT", 9);
        this.mDhcpActionTimeoutAlarm = new WakeupMessage(this.mContext, getHandler(), this.mTag + ".EVENT_DHCPACTION_TIMEOUT", 10);
        addState(this.mStoppedState);
        addState(this.mStartedState);
        addState(this.mStoppingState);
        setInitialState(this.mStoppedState);
        this.mLocalLog = new LocalLog(500);
        super.start();
        this.mIsStartCalled = true;
    }

    protected void onQuitting() {
        this.mCallback.onQuit();
    }

    public void shutdown() {
        stop();
        quit();
    }

    public static ProvisioningConfiguration.Builder buildProvisioningConfiguration() {
        return new ProvisioningConfiguration.Builder();
    }

    public void startProvisioning(ProvisioningConfiguration req) {
        getNetworkInterface();
        this.mCallback.setNeighborDiscoveryOffload(true);
        sendMessage(2, new ProvisioningConfiguration(req));
    }

    public void startProvisioning(StaticIpConfiguration staticIpConfig) {
        startProvisioning(buildProvisioningConfiguration().withStaticConfiguration(staticIpConfig).build());
    }

    public void startProvisioning() {
        startProvisioning(new ProvisioningConfiguration());
    }

    public void stop() {
        sendMessage(1);
    }

    public void confirmConfiguration() {
        sendMessage(3);
    }

    public void completedPreDhcpAction() {
        sendMessage(4);
    }

    public void completedPreDhcpAction(WifiConfiguration wifiConfig) {
        sendMessage(4, wifiConfig);
    }

    public void setTcpBufferSizes(String tcpBufferSizes) {
        sendMessage(6, tcpBufferSizes);
    }

    public void setHttpProxy(ProxyInfo proxyInfo) {
        sendMessage(7, proxyInfo);
    }

    public void setMulticastFilter(boolean enabled) {
        sendMessage(8, Boolean.valueOf(enabled));
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        IndentingPrintWriter pw = new IndentingPrintWriter(writer, "  ");
        pw.println("APF dump:");
        pw.increaseIndent();
        ApfFilter apfFilter = this.mApfFilter;
        if (apfFilter != null) {
            apfFilter.dump(pw);
        } else {
            pw.println("No apf support");
        }
        pw.decreaseIndent();
        pw.println();
        pw.println("StateMachine dump:");
        pw.increaseIndent();
        this.mLocalLog.readOnlyLocalLog().dump(fd, pw, args);
        pw.decreaseIndent();
    }

    protected String getWhatToString(int what) {
        return sWhatToString.get(what, "UNKNOWN: " + Integer.toString(what));
    }

    protected String getLogRecString(Message msg) {
        Object[] objArr = new Object[5];
        objArr[0] = this.mInterfaceName;
        objArr[1] = Integer.valueOf(this.mNetworkInterface == null ? -1 : this.mNetworkInterface.getIndex());
        objArr[2] = Integer.valueOf(msg.arg1);
        objArr[3] = Integer.valueOf(msg.arg2);
        objArr[4] = Objects.toString(msg.obj);
        String logLine = String.format("%s/%d %d %d %s", objArr);
        String richerLogLine = getWhatToString(msg.what) + " " + logLine;
        this.mLocalLog.log(richerLogLine);
        Log.d(this.mTag, richerLogLine);
        return logLine;
    }

    protected boolean recordLogRec(Message msg) {
        return msg.what != 5;
    }

    private void getNetworkInterface() {
        try {
            this.mNetworkInterface = NetworkInterface.getByName(this.mInterfaceName);
        } catch (NullPointerException | SocketException e) {
            Log.e(this.mTag, "ALERT: Failed to get interface object: ", e);
        }
    }

    private void resetLinkProperties() {
        this.mNetlinkTracker.clearLinkProperties();
        this.mConfiguration = null;
        this.mDhcpResults = null;
        this.mTcpBufferSizes = "";
        this.mHttpProxy = null;
        this.mDnsV6Servers = null;
        this.mLinkProperties = new LinkProperties();
        this.mLinkProperties.setInterfaceName(this.mInterfaceName);
    }

    private void recordMetric(int type) {
        if (this.mStartTimeMillis <= 0) {
            Log.d(this.mTag, "Start time undefined!");
        }
        IpManagerEvent.logEvent(type, this.mInterfaceName, SystemClock.elapsedRealtime() - this.mStartTimeMillis);
    }

    private static boolean isProvisioned(LinkProperties lp) {
        if (lp.isProvisioned()) {
            return true;
        }
        return lp.hasIPv4Address();
    }

    private static LinkProperties.ProvisioningChange compareProvisioning(LinkProperties oldLp, LinkProperties newLp) {
        LinkProperties.ProvisioningChange delta;
        boolean wasProvisioned = isProvisioned(oldLp);
        boolean isProvisioned = isProvisioned(newLp);
        if (!wasProvisioned && isProvisioned) {
            delta = LinkProperties.ProvisioningChange.GAINED_PROVISIONING;
        } else if (wasProvisioned && isProvisioned) {
            delta = LinkProperties.ProvisioningChange.STILL_PROVISIONED;
        } else if (!wasProvisioned && !isProvisioned) {
            delta = LinkProperties.ProvisioningChange.STILL_NOT_PROVISIONED;
        } else {
            delta = LinkProperties.ProvisioningChange.LOST_PROVISIONING;
        }
        Log.d("IpManager", "compareProvisioning: " + delta);
        if ((oldLp.hasIPv4Address() && !newLp.hasIPv4Address()) || (oldLp.isIPv6Provisioned() && !newLp.isIPv6Provisioned())) {
            delta = LinkProperties.ProvisioningChange.LOST_PROVISIONING;
            Log.d("IpManager", "compareProvisioning: " + delta + " due to hasIPv4Address/isIPv6Provisioned lost");
        }
        if (oldLp.hasGlobalIPv6Address() && oldLp.hasIPv6DefaultRoute() && !newLp.hasIPv6DefaultRoute()) {
            LinkProperties.ProvisioningChange delta2 = LinkProperties.ProvisioningChange.LOST_PROVISIONING;
            Log.d("IpManager", "compareProvisioning: " + delta2 + " due to IPv6DefaultRoute lost");
            return delta2;
        }
        return delta;
    }

    private void dispatchCallback(LinkProperties.ProvisioningChange delta, LinkProperties newLp) {
        switch (m115getandroidnetLinkProperties$ProvisioningChangeSwitchesValues()[delta.ordinal()]) {
            case 1:
                Log.d(this.mTag, "onProvisioningSuccess()");
                recordMetric(1);
                this.mCallback.onProvisioningSuccess(newLp);
                break;
            case 2:
                Log.d(this.mTag, "onProvisioningFailure()");
                recordMetric(2);
                this.mCallback.onProvisioningFailure(newLp);
                break;
            default:
                Log.d(this.mTag, "onLinkPropertiesChange()");
                this.mCallback.onLinkPropertiesChange(newLp);
                break;
        }
    }

    private LinkProperties.ProvisioningChange setLinkProperties(LinkProperties newLp) {
        Log.d(this.mTag, "setLinkProperties newLp = " + newLp);
        if (this.mApfFilter != null) {
            this.mApfFilter.setLinkProperties(newLp);
        }
        if (this.mIpReachabilityMonitor != null) {
            this.mIpReachabilityMonitor.updateLinkProperties(newLp);
        }
        LinkProperties.ProvisioningChange delta = compareProvisioning(this.mLinkProperties, newLp);
        this.mLinkProperties = new LinkProperties(newLp);
        if (delta == LinkProperties.ProvisioningChange.GAINED_PROVISIONING) {
            this.mProvisioningTimeoutAlarm.cancel();
        }
        return delta;
    }

    private boolean linkPropertiesUnchanged(LinkProperties newLp) {
        return Objects.equals(newLp, this.mLinkProperties);
    }

    private LinkProperties assembleLinkProperties() {
        LinkProperties newLp = new LinkProperties();
        newLp.setInterfaceName(this.mInterfaceName);
        LinkProperties netlinkLinkProperties = this.mNetlinkTracker.getLinkProperties();
        newLp.setLinkAddresses(netlinkLinkProperties.getLinkAddresses());
        for (RouteInfo route : netlinkLinkProperties.getRoutes()) {
            newLp.addRoute(route);
        }
        for (InetAddress dns : netlinkLinkProperties.getDnsServers()) {
            if (newLp.isReachable(dns)) {
                newLp.addDnsServer(dns);
            }
        }
        if (this.mDhcpResults != null) {
            for (RouteInfo route2 : this.mDhcpResults.getRoutes(this.mInterfaceName)) {
                newLp.addRoute(route2);
            }
            Iterator dns$iterator = this.mDhcpResults.dnsServers.iterator();
            while (dns$iterator.hasNext()) {
                newLp.addDnsServer((InetAddress) dns$iterator.next());
            }
            newLp.setDomains(this.mDhcpResults.domains);
            if (this.mDhcpResults.mtu != 0) {
                newLp.setMtu(this.mDhcpResults.mtu);
            }
        }
        if (!TextUtils.isEmpty(this.mTcpBufferSizes)) {
            newLp.setTcpBufferSizes(this.mTcpBufferSizes);
        }
        if (this.mHttpProxy != null) {
            newLp.setHttpProxy(this.mHttpProxy);
        }
        if (this.mDnsV6Servers != null) {
            for (InetAddress dnsV6 : this.mDnsV6Servers) {
                if (newLp.isReachable(dnsV6)) {
                    newLp.addDnsServer(dnsV6);
                }
            }
        }
        Log.d(this.mTag, "newLp{" + newLp + "}");
        return newLp;
    }

    private boolean handleLinkPropertiesUpdate(boolean sendCallbacks) {
        LinkProperties newLp = assembleLinkProperties();
        if (linkPropertiesUnchanged(newLp)) {
            Log.d(this.mTag, "linkPropertiesUnchanged");
            return true;
        }
        LinkProperties.ProvisioningChange delta = setLinkProperties(newLp);
        Log.d(this.mTag, "handleLinkPropertiesUpdate delta = " + delta);
        if (sendCallbacks) {
            dispatchCallback(delta, newLp);
        }
        return delta != LinkProperties.ProvisioningChange.LOST_PROVISIONING;
    }

    private boolean setIPv4Address(LinkAddress address) {
        InterfaceConfiguration ifcg = new InterfaceConfiguration();
        ifcg.setLinkAddress(address);
        try {
            this.mNwService.setInterfaceConfig(this.mInterfaceName, ifcg);
            Log.d(this.mTag, "IPv4 configuration succeeded");
            return true;
        } catch (RemoteException | IllegalStateException e) {
            Log.e(this.mTag, "IPv4 configuration failed: ", e);
            return false;
        }
    }

    private void clearIPv4Address() {
        try {
            InterfaceConfiguration ifcg = new InterfaceConfiguration();
            ifcg.setLinkAddress(new LinkAddress("0.0.0.0/0"));
            this.mNwService.setInterfaceConfig(this.mInterfaceName, ifcg);
        } catch (RemoteException | IllegalStateException e) {
            Log.e(this.mTag, "ALERT: Failed to clear IPv4 address on interface " + this.mInterfaceName, e);
        }
    }

    private void handleIPv4Success(DhcpResults dhcpResults) {
        this.mDhcpResults = new DhcpResults(dhcpResults);
        LinkProperties newLp = assembleLinkProperties();
        LinkProperties.ProvisioningChange delta = setLinkProperties(newLp);
        Log.d(this.mTag, "handleIPv4Success delta = " + delta);
        Log.d(this.mTag, "onNewDhcpResults(" + Objects.toString(dhcpResults) + ")");
        this.mCallback.onNewDhcpResults(dhcpResults);
        dispatchCallback(delta, newLp);
    }

    private void handleIPv4Failure() {
        clearIPv4Address();
        this.mDhcpResults = null;
        Log.d(this.mTag, "onNewDhcpResults(null)");
        this.mCallback.onNewDhcpResults(null);
        handleProvisioningFailure();
    }

    private void handleProvisioningFailure() {
        LinkProperties newLp = assembleLinkProperties();
        LinkProperties.ProvisioningChange delta = setLinkProperties(newLp);
        if (delta == LinkProperties.ProvisioningChange.STILL_NOT_PROVISIONED) {
            delta = LinkProperties.ProvisioningChange.LOST_PROVISIONING;
        }
        dispatchCallback(delta, newLp);
        if (delta != LinkProperties.ProvisioningChange.LOST_PROVISIONING) {
            return;
        }
        transitionTo(this.mStoppingState);
    }

    private boolean startIPv4() {
        if (this.mConfiguration.mStaticIpConfig != null) {
            if (setIPv4Address(this.mConfiguration.mStaticIpConfig.ipAddress)) {
                handleIPv4Success(new DhcpResults(this.mConfiguration.mStaticIpConfig));
                return true;
            }
            Log.d(this.mTag, "onProvisioningFailure()");
            recordMetric(2);
            this.mCallback.onProvisioningFailure(new LinkProperties(this.mLinkProperties));
            return false;
        }
        this.mDhcpClient = DhcpClient.makeDhcpClient(this.mContext, this, this.mInterfaceName);
        this.mDhcpClient.registerForPreDhcpNotification();
        this.mDhcpClient.sendMessage(DhcpClient.CMD_START_DHCP, this.mPastSuccessedDhcpResult);
        if (sMtkDhcpv6cWifi) {
            this.mDhcp6Client = Dhcp6Client.makeDhcp6Client(this.mContext, this, this.mInterfaceName);
            this.mDhcp6Client.registerForPreDhcpNotification();
            this.mDhcp6Client.sendMessage(Dhcp6Client.CMD_START_DHCP);
        }
        if (this.mConfiguration.mProvisioningTimeoutMs > 0) {
            long alarmTime = SystemClock.elapsedRealtime() + ((long) this.mConfiguration.mProvisioningTimeoutMs);
            this.mProvisioningTimeoutAlarm.schedule(alarmTime);
            return true;
        }
        return true;
    }

    private boolean isDhcp6Support(Dhcp6Client client) {
        return sMtkDhcpv6cWifi && client != null;
    }

    private boolean startIPv6() {
        try {
            this.mNwService.setInterfaceIpv6PrivacyExtensions(this.mInterfaceName, true);
            this.mNwService.enableIpv6(this.mInterfaceName);
            return true;
        } catch (RemoteException re) {
            Log.e(this.mTag, "Unable to change interface settings: " + re);
            return false;
        } catch (IllegalStateException ie) {
            Log.e(this.mTag, "Unable to change interface settings: " + ie);
            return false;
        }
    }

    class StoppedState extends State {
        StoppedState() {
        }

        public void enter() {
            Log.d(IpManager.this.mTag, "[StoppedState] Enter");
            try {
                IpManager.this.mNwService.disableIpv6(IpManager.this.mInterfaceName);
                IpManager.this.mNwService.clearInterfaceAddresses(IpManager.this.mInterfaceName);
            } catch (Exception e) {
                Log.e(IpManager.this.mTag, "Failed to clear addresses or disable IPv6" + e);
            }
            IpManager.this.resetLinkProperties();
            Log.d(IpManager.this.mTag, "resetLinkProperties");
            if (IpManager.this.mIsWifiSMStarted) {
                IpManager.this.mCallback.onLinkPropertiesChange(IpManager.this.mLinkProperties);
            }
            if (IpManager.this.mStartTimeMillis <= 0) {
                return;
            }
            IpManager.this.recordMetric(3);
            IpManager.this.mStartTimeMillis = 0L;
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    return true;
                case 2:
                    IpManager.this.mIsWifiSMStarted = true;
                    IpManager.this.mConfiguration = (ProvisioningConfiguration) msg.obj;
                    IpManager.this.transitionTo(IpManager.this.mStartedState);
                    return true;
                case 5:
                    IpManager.this.handleLinkPropertiesUpdate(false);
                    return true;
                case 6:
                    IpManager.this.mTcpBufferSizes = (String) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(false);
                    return true;
                case 7:
                    IpManager.this.mHttpProxy = (ProxyInfo) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(false);
                    return true;
                case 8:
                    IpManager.this.mMulticastFiltering = ((Boolean) msg.obj).booleanValue();
                    return true;
                case DhcpClient.CMD_ON_QUIT:
                    Log.e(IpManager.this.mTag, "Unexpected CMD_ON_QUIT (already stopped).");
                    return true;
                default:
                    Log.d(IpManager.this.mTag, "StoppedState NOT_HANDLED what = " + msg.what);
                    return false;
            }
        }
    }

    class StoppingState extends State {
        StoppingState() {
        }

        public void enter() {
            Log.d(IpManager.this.mTag, "[StoppingState] Enter");
            if (IpManager.this.mDhcpClient != null) {
                return;
            }
            IpManager.this.transitionTo(IpManager.this.mStoppedState);
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case DhcpClient.CMD_ON_QUIT:
                    IpManager.this.mDhcpClient = null;
                    IpManager.this.transitionTo(IpManager.this.mStoppedState);
                    break;
                case DhcpClient.CMD_PRE_DHCP_ACTION_COMPLETE:
                default:
                    Log.d(IpManager.this.mTag, "deferMessage what = " + msg.what);
                    IpManager.this.deferMessage(msg);
                    break;
                case DhcpClient.CMD_CLEAR_LINKADDRESS:
                    IpManager.this.clearIPv4Address();
                    break;
            }
            return true;
        }
    }

    class StartedState extends State {
        private boolean mDhcpActionInFlight;

        StartedState() {
        }

        public void enter() {
            Log.d(IpManager.this.mTag, "[StartedState] Enter");
            IpManager.this.mStartTimeMillis = SystemClock.elapsedRealtime();
            IpManager.this.mApfFilter = ApfFilter.maybeCreate(IpManager.this.mConfiguration.mApfCapabilities, IpManager.this.mNetworkInterface, IpManager.this.mCallback, IpManager.this.mMulticastFiltering);
            if (IpManager.this.mApfFilter == null) {
                IpManager.this.mCallback.setFallbackMulticastFilter(IpManager.this.mMulticastFiltering);
            }
            if (IpManager.this.mConfiguration.mEnableIPv6) {
                IpManager.this.startIPv6();
            }
            if (IpManager.this.mConfiguration.mUsingIpReachabilityMonitor) {
                try {
                    IpManager.this.mIpReachabilityMonitor = new IpReachabilityMonitor(IpManager.this.mContext, IpManager.this.mInterfaceName, new IpReachabilityMonitor.Callback() {
                        @Override
                        public void notifyLost(InetAddress ip, String logMsg) {
                            IpManager.this.mCallback.onReachabilityLost(logMsg);
                        }
                    });
                } catch (IllegalArgumentException e) {
                    Log.e(IpManager.this.mTag, "mIpReachabilityMonitor:" + e);
                }
            }
            if (!IpManager.this.mConfiguration.mEnableIPv4 || IpManager.this.startIPv4()) {
                return;
            }
            IpManager.this.transitionTo(IpManager.this.mStoppingState);
        }

        public void exit() {
            IpManager.this.mProvisioningTimeoutAlarm.cancel();
            stopDhcpAction();
            if (IpManager.this.mIpReachabilityMonitor != null) {
                IpManager.this.mIpReachabilityMonitor.stop();
                IpManager.this.mIpReachabilityMonitor = null;
            }
            if (IpManager.this.mDhcpClient != null) {
                IpManager.this.mDhcpClient.sendMessage(DhcpClient.CMD_STOP_DHCP);
                IpManager.this.mDhcpClient.doQuit();
            }
            if (IpManager.this.isDhcp6Support(IpManager.this.mDhcp6Client)) {
                IpManager.this.mDhcp6Client.sendMessage(Dhcp6Client.CMD_STOP_DHCP);
                IpManager.this.mDhcp6Client.doQuit();
            }
            if (IpManager.this.mApfFilter != null) {
                IpManager.this.mApfFilter.shutdown();
                IpManager.this.mApfFilter = null;
            }
            IpManager.this.resetLinkProperties();
        }

        private void ensureDhcpAction() {
            if (this.mDhcpActionInFlight) {
                return;
            }
            IpManager.this.mCallback.onPreDhcpAction();
            this.mDhcpActionInFlight = true;
            long alarmTime = SystemClock.elapsedRealtime() + ((long) IpManager.this.mConfiguration.mRequestedPreDhcpActionMs);
            IpManager.this.mDhcpActionTimeoutAlarm.schedule(alarmTime);
        }

        private void stopDhcpAction() {
            IpManager.this.mDhcpActionTimeoutAlarm.cancel();
            if (!this.mDhcpActionInFlight) {
                return;
            }
            IpManager.this.mCallback.onPostDhcpAction();
            this.mDhcpActionInFlight = false;
        }

        public boolean processMessage(Message msg) throws Throwable {
            switch (msg.what) {
                case 1:
                    IpManager.this.transitionTo(IpManager.this.mStoppingState);
                    return true;
                case 2:
                    Log.e(IpManager.this.mTag, "ALERT: START received in StartedState. Please fix caller.");
                    return true;
                case 3:
                    if (IpManager.this.mIpReachabilityMonitor != null) {
                        IpManager.this.mIpReachabilityMonitor.probeAll();
                    }
                    return true;
                case 4:
                    if (IpManager.this.mDhcpClient != null) {
                        if (msg.obj != null) {
                            WifiConfiguration wifiConfig = (WifiConfiguration) msg.obj;
                            IpManager.this.mDhcpClient.sendMessage(DhcpClient.CMD_PRE_DHCP_ACTION_COMPLETE, wifiConfig);
                        } else {
                            IpManager.this.mDhcpClient.sendMessage(DhcpClient.CMD_PRE_DHCP_ACTION_COMPLETE);
                        }
                    }
                    if (IpManager.this.isDhcp6Support(IpManager.this.mDhcp6Client)) {
                        IpManager.this.mDhcp6Client.sendMessage(Dhcp6Client.CMD_PRE_DHCP_ACTION_COMPLETE);
                    }
                    return true;
                case 5:
                    if (!IpManager.this.handleLinkPropertiesUpdate(true)) {
                        IpManager.this.transitionTo(IpManager.this.mStoppingState);
                    }
                    return true;
                case 6:
                    IpManager.this.mTcpBufferSizes = (String) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(true);
                    return true;
                case 7:
                    IpManager.this.mHttpProxy = (ProxyInfo) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(true);
                    return true;
                case 8:
                    IpManager.this.mMulticastFiltering = ((Boolean) msg.obj).booleanValue();
                    if (IpManager.this.mApfFilter != null) {
                        IpManager.this.mApfFilter.setMulticastFilter(IpManager.this.mMulticastFiltering);
                    } else {
                        IpManager.this.mCallback.setFallbackMulticastFilter(IpManager.this.mMulticastFiltering);
                    }
                    return true;
                case 9:
                    IpManager.this.handleProvisioningFailure();
                    return true;
                case 10:
                    stopDhcpAction();
                    return true;
                case DhcpClient.CMD_PRE_DHCP_ACTION:
                    if (IpManager.this.mConfiguration.mRequestedPreDhcpActionMs > 0) {
                        ensureDhcpAction();
                    } else {
                        IpManager.this.sendMessage(4);
                    }
                    return true;
                case DhcpClient.CMD_POST_DHCP_ACTION:
                    stopDhcpAction();
                    switch (msg.arg1) {
                        case 1:
                            IpManager.this.handleIPv4Success((DhcpResults) msg.obj);
                            return true;
                        case 2:
                            IpManager.this.handleIPv4Failure();
                            return true;
                        default:
                            Log.e(IpManager.this.mTag, "Unknown CMD_POST_DHCP_ACTION status:" + msg.arg1);
                            return true;
                    }
                case DhcpClient.CMD_ON_QUIT:
                    Log.e(IpManager.this.mTag, "Unexpected CMD_ON_QUIT.");
                    IpManager.this.mDhcpClient = null;
                    return true;
                case DhcpClient.CMD_CLEAR_LINKADDRESS:
                    if (IpManager.this.mConfiguration.mStaticIpConfig != null) {
                        Log.e(IpManager.this.mTag, "static Ip is configured, ignore clearIPv4Address");
                        return true;
                    }
                    IpManager.this.clearIPv4Address();
                    return true;
                case DhcpClient.CMD_CONFIGURE_LINKADDRESS:
                    LinkAddress ipAddress = (LinkAddress) msg.obj;
                    if (IpManager.this.setIPv4Address(ipAddress)) {
                        IpManager.this.mDhcpClient.sendMessage(DhcpClient.EVENT_LINKADDRESS_CONFIGURED);
                    } else {
                        Log.e(IpManager.this.mTag, "Failed to set IPv4 address!");
                        IpManager.this.dispatchCallback(LinkProperties.ProvisioningChange.LOST_PROVISIONING, new LinkProperties(IpManager.this.mLinkProperties));
                        IpManager.this.transitionTo(IpManager.this.mStoppingState);
                    }
                    return true;
                case Dhcp6Client.CMD_ON_QUIT:
                    Log.e(IpManager.this.mTag, "Unexpected v6 CMD_ON_QUIT.");
                    IpManager.this.mDhcp6Client = null;
                    IpManager.this.mDnsV6Servers = null;
                    return true;
                case Dhcp6Client.CMD_CONFIGURE_DNSV6:
                    IpManager.this.mDnsV6Servers = (ArrayList) msg.obj;
                    IpManager.this.handleLinkPropertiesUpdate(true);
                    return true;
                default:
                    return false;
            }
        }
    }

    public void updatePastSuccessedDhcpResult(DhcpResults result) {
        this.mPastSuccessedDhcpResult = result;
    }
}
