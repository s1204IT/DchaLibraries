package com.android.server.connectivity.tethering;

import android.net.INetd;
import android.net.INetworkStatsService;
import android.net.InterfaceConfiguration;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.ip.InterfaceController;
import android.net.ip.RouterAdvertisementDaemon;
import android.net.util.InterfaceParams;
import android.net.util.InterfaceSet;
import android.net.util.NetworkConstants;
import android.net.util.SharedLog;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class TetherInterfaceStateMachine extends StateMachine {
    private static final int BASE_IFACE = 327780;
    public static final int CMD_INTERFACE_DOWN = 327784;
    public static final int CMD_IPV6_TETHER_UPDATE = 327793;
    public static final int CMD_IP_FORWARDING_DISABLE_ERROR = 327788;
    public static final int CMD_IP_FORWARDING_ENABLE_ERROR = 327787;
    public static final int CMD_SET_DNS_FORWARDERS_ERROR = 327791;
    public static final int CMD_START_TETHERING_ERROR = 327789;
    public static final int CMD_STOP_TETHERING_ERROR = 327790;
    public static final int CMD_TETHER_CONNECTION_CHANGED = 327792;
    public static final int CMD_TETHER_REQUESTED = 327782;
    public static final int CMD_TETHER_UNREQUESTED = 327783;
    private static final boolean DBG = false;
    private static final byte DOUG_ADAMS = 42;
    private static final String TAG = "TetherInterfaceSM";
    private static final String USB_NEAR_IFACE_ADDR = "192.168.42.129";
    private static final int USB_PREFIX_LENGTH = 24;
    private static final boolean VDBG = false;
    private static final String WIFI_HOST_IFACE_ADDR = "192.168.43.1";
    private static final int WIFI_HOST_IFACE_PREFIX_LENGTH = 24;
    private final TetheringDependencies mDeps;
    private final String mIfaceName;
    private final State mInitialState;
    private final InterfaceController mInterfaceCtrl;
    private InterfaceParams mInterfaceParams;
    private final int mInterfaceType;
    private int mLastError;
    private LinkProperties mLastIPv6LinkProperties;
    private RouterAdvertisementDaemon.RaParams mLastRaParams;
    private final LinkProperties mLinkProperties;
    private final State mLocalHotspotState;
    private final SharedLog mLog;
    private final INetworkManagementService mNMService;
    private final INetd mNetd;
    private RouterAdvertisementDaemon mRaDaemon;
    private int mServingMode;
    private final INetworkStatsService mStatsService;
    private final IControlsTethering mTetherController;
    private final State mTetheredState;
    private final State mUnavailableState;
    private InterfaceSet mUpstreamIfaceSet;
    private static final IpPrefix LINK_LOCAL_PREFIX = new IpPrefix("fe80::/64");
    private static final Class[] messageClasses = {TetherInterfaceStateMachine.class};
    private static final SparseArray<String> sMagicDecoderRing = MessageUtils.findMessageNames(messageClasses);

    public TetherInterfaceStateMachine(String str, Looper looper, int i, SharedLog sharedLog, INetworkManagementService iNetworkManagementService, INetworkStatsService iNetworkStatsService, IControlsTethering iControlsTethering, TetheringDependencies tetheringDependencies) {
        super(str, looper);
        this.mLog = sharedLog.forSubComponent(str);
        this.mNMService = iNetworkManagementService;
        this.mNetd = tetheringDependencies.getNetdService();
        this.mStatsService = iNetworkStatsService;
        this.mTetherController = iControlsTethering;
        this.mInterfaceCtrl = new InterfaceController(str, iNetworkManagementService, this.mNetd, this.mLog);
        this.mIfaceName = str;
        this.mInterfaceType = i;
        this.mLinkProperties = new LinkProperties();
        this.mDeps = tetheringDependencies;
        resetLinkProperties();
        this.mLastError = 0;
        this.mServingMode = 1;
        this.mInitialState = new InitialState();
        this.mLocalHotspotState = new LocalHotspotState();
        this.mTetheredState = new TetheredState();
        this.mUnavailableState = new UnavailableState();
        addState(this.mInitialState);
        addState(this.mLocalHotspotState);
        addState(this.mTetheredState);
        addState(this.mUnavailableState);
        setInitialState(this.mInitialState);
    }

    public String interfaceName() {
        return this.mIfaceName;
    }

    public int interfaceType() {
        return this.mInterfaceType;
    }

    public int lastError() {
        return this.mLastError;
    }

    public int servingMode() {
        return this.mServingMode;
    }

    public LinkProperties linkProperties() {
        return new LinkProperties(this.mLinkProperties);
    }

    public void stop() {
        sendMessage(CMD_INTERFACE_DOWN);
    }

    public void unwanted() {
        sendMessage(CMD_TETHER_UNREQUESTED);
    }

    private boolean startIPv4() {
        return configureIPv4(true);
    }

    private void stopIPv4() {
        configureIPv4(false);
        this.mInterfaceCtrl.clearIPv4Address();
    }

    private boolean configureIPv4(boolean z) {
        String randomWifiIPv4Address;
        if (this.mInterfaceType == 1) {
            randomWifiIPv4Address = USB_NEAR_IFACE_ADDR;
        } else {
            if (this.mInterfaceType != 0) {
                return true;
            }
            randomWifiIPv4Address = getRandomWifiIPv4Address();
        }
        try {
            InterfaceConfiguration interfaceConfig = this.mNMService.getInterfaceConfig(this.mIfaceName);
            if (interfaceConfig == null) {
                this.mLog.e("Received null interface config");
                return false;
            }
            LinkAddress linkAddress = new LinkAddress(NetworkUtils.numericToInetAddress(randomWifiIPv4Address), 24);
            interfaceConfig.setLinkAddress(linkAddress);
            if (this.mInterfaceType == 0) {
                interfaceConfig.ignoreInterfaceUpDownStatus();
            } else if (z) {
                interfaceConfig.setInterfaceUp();
            } else {
                interfaceConfig.setInterfaceDown();
            }
            interfaceConfig.clearFlag("running");
            this.mNMService.setInterfaceConfig(this.mIfaceName, interfaceConfig);
            RouteInfo routeInfo = new RouteInfo(linkAddress);
            if (z) {
                this.mLinkProperties.addLinkAddress(linkAddress);
                this.mLinkProperties.addRoute(routeInfo);
            } else {
                this.mLinkProperties.removeLinkAddress(linkAddress);
                this.mLinkProperties.removeRoute(routeInfo);
            }
            return true;
        } catch (Exception e) {
            this.mLog.e("Error configuring interface " + e);
            return false;
        }
    }

    private String getRandomWifiIPv4Address() {
        try {
            byte[] address = NetworkUtils.numericToInetAddress(WIFI_HOST_IFACE_ADDR).getAddress();
            address[3] = getRandomSanitizedByte((byte) 42, NetworkConstants.asByte(0), NetworkConstants.asByte(1), NetworkConstants.FF);
            return InetAddress.getByAddress(address).getHostAddress();
        } catch (Exception e) {
            return WIFI_HOST_IFACE_ADDR;
        }
    }

    private boolean startIPv6() {
        this.mInterfaceParams = this.mDeps.getInterfaceParams(this.mIfaceName);
        if (this.mInterfaceParams == null) {
            this.mLog.e("Failed to find InterfaceParams");
            stopIPv6();
            return false;
        }
        this.mRaDaemon = this.mDeps.getRouterAdvertisementDaemon(this.mInterfaceParams);
        if (!this.mRaDaemon.start()) {
            stopIPv6();
            return false;
        }
        return true;
    }

    private void stopIPv6() {
        this.mInterfaceParams = null;
        setRaParams(null);
        if (this.mRaDaemon != null) {
            this.mRaDaemon.stop();
            this.mRaDaemon = null;
        }
    }

    private void updateUpstreamIPv6LinkProperties(LinkProperties linkProperties) {
        if (this.mRaDaemon == null || Objects.equals(this.mLastIPv6LinkProperties, linkProperties)) {
            return;
        }
        RouterAdvertisementDaemon.RaParams raParams = null;
        if (linkProperties != null) {
            raParams = new RouterAdvertisementDaemon.RaParams();
            raParams.mtu = linkProperties.getMtu();
            raParams.hasDefaultRoute = linkProperties.hasIPv6DefaultRoute();
            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                if (linkAddress.getPrefixLength() == 64) {
                    IpPrefix ipPrefix = new IpPrefix(linkAddress.getAddress(), linkAddress.getPrefixLength());
                    raParams.prefixes.add(ipPrefix);
                    Inet6Address localDnsIpFor = getLocalDnsIpFor(ipPrefix);
                    if (localDnsIpFor != null) {
                        raParams.dnses.add(localDnsIpFor);
                    }
                }
            }
        }
        setRaParams(raParams);
        this.mLastIPv6LinkProperties = linkProperties;
    }

    private void configureLocalIPv6Routes(HashSet<IpPrefix> hashSet, HashSet<IpPrefix> hashSet2) {
        if (!hashSet.isEmpty()) {
            ArrayList<RouteInfo> localRoutesFor = getLocalRoutesFor(this.mIfaceName, hashSet);
            try {
                int iRemoveRoutesFromLocalNetwork = this.mNMService.removeRoutesFromLocalNetwork(localRoutesFor);
                if (iRemoveRoutesFromLocalNetwork > 0) {
                    this.mLog.e(String.format("Failed to remove %d IPv6 routes from local table.", Integer.valueOf(iRemoveRoutesFromLocalNetwork)));
                }
            } catch (RemoteException e) {
                this.mLog.e("Failed to remove IPv6 routes from local table: " + e);
            }
            Iterator<RouteInfo> it = localRoutesFor.iterator();
            while (it.hasNext()) {
                this.mLinkProperties.removeRoute(it.next());
            }
        }
        if (hashSet2 != null && !hashSet2.isEmpty()) {
            HashSet hashSet3 = (HashSet) hashSet2.clone();
            if (this.mLastRaParams != null) {
                hashSet3.removeAll(this.mLastRaParams.prefixes);
            }
            if (this.mLastRaParams == null || this.mLastRaParams.prefixes.isEmpty()) {
                hashSet3.add(LINK_LOCAL_PREFIX);
            }
            if (!hashSet3.isEmpty()) {
                ArrayList<RouteInfo> localRoutesFor2 = getLocalRoutesFor(this.mIfaceName, hashSet3);
                try {
                    this.mNMService.addInterfaceToLocalNetwork(this.mIfaceName, localRoutesFor2);
                } catch (RemoteException e2) {
                    this.mLog.e("Failed to add IPv6 routes to local table: " + e2);
                }
                Iterator<RouteInfo> it2 = localRoutesFor2.iterator();
                while (it2.hasNext()) {
                    this.mLinkProperties.addRoute(it2.next());
                }
            }
        }
    }

    private void configureLocalIPv6Dns(HashSet<Inet6Address> hashSet, HashSet<Inet6Address> hashSet2) {
        if (this.mNetd == null) {
            if (hashSet2 != null) {
                hashSet2.clear();
            }
            this.mLog.e("No netd service instance available; not setting local IPv6 addresses");
            return;
        }
        if (!hashSet.isEmpty()) {
            for (Inet6Address inet6Address : hashSet) {
                if (!this.mInterfaceCtrl.removeAddress(inet6Address, 64)) {
                    this.mLog.e("Failed to remove local dns IP " + inet6Address);
                }
                this.mLinkProperties.removeLinkAddress(new LinkAddress(inet6Address, 64));
            }
        }
        if (hashSet2 != null && !hashSet2.isEmpty()) {
            HashSet<Inet6Address> hashSet3 = (HashSet) hashSet2.clone();
            if (this.mLastRaParams != null) {
                hashSet3.removeAll(this.mLastRaParams.dnses);
            }
            for (Inet6Address inet6Address2 : hashSet3) {
                if (!this.mInterfaceCtrl.addAddress(inet6Address2, 64)) {
                    this.mLog.e("Failed to add local dns IP " + inet6Address2);
                    hashSet2.remove(inet6Address2);
                }
                this.mLinkProperties.addLinkAddress(new LinkAddress(inet6Address2, 64));
            }
        }
        try {
            this.mNetd.tetherApplyDnsInterfaces();
        } catch (ServiceSpecificException | RemoteException e) {
            this.mLog.e("Failed to update local DNS caching server");
            if (hashSet2 != null) {
                hashSet2.clear();
            }
        }
    }

    private void setRaParams(RouterAdvertisementDaemon.RaParams raParams) {
        if (this.mRaDaemon != null) {
            RouterAdvertisementDaemon.RaParams deprecatedRaParams = RouterAdvertisementDaemon.RaParams.getDeprecatedRaParams(this.mLastRaParams, raParams);
            try {
                configureLocalIPv6Routes(deprecatedRaParams.prefixes, raParams != null ? raParams.prefixes : null);
                configureLocalIPv6Dns(deprecatedRaParams.dnses, raParams != null ? raParams.dnses : null);
                this.mRaDaemon.buildNewRa(deprecatedRaParams, raParams);
            } catch (Exception e) {
                this.mLog.e("Failed to setRaParams:" + e);
            }
        }
        this.mLastRaParams = raParams;
    }

    private void logMessage(State state, int i) {
        this.mLog.log(state.getName() + " got " + sMagicDecoderRing.get(i, Integer.toString(i)));
    }

    private void sendInterfaceState(int i) {
        this.mServingMode = i;
        this.mTetherController.updateInterfaceState(this, i, this.mLastError);
        sendLinkProperties();
    }

    private void sendLinkProperties() {
        this.mTetherController.updateLinkProperties(this, new LinkProperties(this.mLinkProperties));
    }

    private void resetLinkProperties() {
        this.mLinkProperties.clear();
        this.mLinkProperties.setInterfaceName(this.mIfaceName);
    }

    class InitialState extends State {
        InitialState() {
        }

        public void enter() {
            TetherInterfaceStateMachine.this.sendInterfaceState(1);
        }

        public boolean processMessage(Message message) {
            TetherInterfaceStateMachine.this.logMessage(this, message.what);
            int i = message.what;
            if (i != 327782) {
                if (i == 327784) {
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mUnavailableState);
                    return true;
                }
                if (i != 327793) {
                    return false;
                }
                TetherInterfaceStateMachine.this.updateUpstreamIPv6LinkProperties((LinkProperties) message.obj);
                return true;
            }
            TetherInterfaceStateMachine.this.mLastError = 0;
            switch (message.arg1) {
                case 2:
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mTetheredState);
                    return true;
                case 3:
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mLocalHotspotState);
                    return true;
                default:
                    TetherInterfaceStateMachine.this.mLog.e("Invalid tethering interface serving state specified.");
                    return true;
            }
        }
    }

    class BaseServingState extends State {
        BaseServingState() {
        }

        public void enter() {
            if (!TetherInterfaceStateMachine.this.startIPv4()) {
                TetherInterfaceStateMachine.this.mLastError = 10;
                return;
            }
            try {
                TetherInterfaceStateMachine.this.mNMService.tetherInterface(TetherInterfaceStateMachine.this.mIfaceName);
                if (!TetherInterfaceStateMachine.this.startIPv6()) {
                    TetherInterfaceStateMachine.this.mLog.e("Failed to startIPv6");
                }
            } catch (Exception e) {
                TetherInterfaceStateMachine.this.mLog.e("Error Tethering: " + e);
                TetherInterfaceStateMachine.this.mLastError = 6;
            }
        }

        public void exit() {
            TetherInterfaceStateMachine.this.stopIPv6();
            try {
                TetherInterfaceStateMachine.this.mNMService.untetherInterface(TetherInterfaceStateMachine.this.mIfaceName);
            } catch (Exception e) {
                TetherInterfaceStateMachine.this.mLastError = 7;
                TetherInterfaceStateMachine.this.mLog.e("Failed to untether interface: " + e);
            }
            TetherInterfaceStateMachine.this.stopIPv4();
            TetherInterfaceStateMachine.this.resetLinkProperties();
        }

        public boolean processMessage(Message message) {
            TetherInterfaceStateMachine.this.logMessage(this, message.what);
            switch (message.what) {
                case TetherInterfaceStateMachine.CMD_TETHER_UNREQUESTED:
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
                    return true;
                case TetherInterfaceStateMachine.CMD_INTERFACE_DOWN:
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mUnavailableState);
                    return true;
                case 327785:
                case 327786:
                case TetherInterfaceStateMachine.CMD_TETHER_CONNECTION_CHANGED:
                default:
                    return false;
                case TetherInterfaceStateMachine.CMD_IP_FORWARDING_ENABLE_ERROR:
                case TetherInterfaceStateMachine.CMD_IP_FORWARDING_DISABLE_ERROR:
                case TetherInterfaceStateMachine.CMD_START_TETHERING_ERROR:
                case TetherInterfaceStateMachine.CMD_STOP_TETHERING_ERROR:
                case TetherInterfaceStateMachine.CMD_SET_DNS_FORWARDERS_ERROR:
                    TetherInterfaceStateMachine.this.mLastError = 5;
                    TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
                    return true;
                case TetherInterfaceStateMachine.CMD_IPV6_TETHER_UPDATE:
                    TetherInterfaceStateMachine.this.updateUpstreamIPv6LinkProperties((LinkProperties) message.obj);
                    TetherInterfaceStateMachine.this.sendLinkProperties();
                    return true;
            }
        }
    }

    class LocalHotspotState extends BaseServingState {
        LocalHotspotState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            if (TetherInterfaceStateMachine.this.mLastError != 0) {
                TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
            }
            TetherInterfaceStateMachine.this.sendInterfaceState(3);
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            TetherInterfaceStateMachine.this.logMessage(this, message.what);
            int i = message.what;
            if (i == 327782) {
                TetherInterfaceStateMachine.this.mLog.e("CMD_TETHER_REQUESTED while in local-only hotspot mode.");
            } else if (i != 327792) {
                return false;
            }
            return true;
        }
    }

    class TetheredState extends BaseServingState {
        TetheredState() {
            super();
        }

        @Override
        public void enter() {
            super.enter();
            if (TetherInterfaceStateMachine.this.mLastError != 0) {
                TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
            }
            TetherInterfaceStateMachine.this.sendInterfaceState(2);
        }

        @Override
        public void exit() {
            cleanupUpstream();
            super.exit();
        }

        private void cleanupUpstream() {
            if (TetherInterfaceStateMachine.this.mUpstreamIfaceSet == null) {
                return;
            }
            Iterator<String> it = TetherInterfaceStateMachine.this.mUpstreamIfaceSet.ifnames.iterator();
            while (it.hasNext()) {
                cleanupUpstreamInterface(it.next());
            }
            TetherInterfaceStateMachine.this.mUpstreamIfaceSet = null;
        }

        private void cleanupUpstreamInterface(String str) {
            try {
                TetherInterfaceStateMachine.this.mStatsService.forceUpdate();
            } catch (Exception e) {
            }
            try {
                TetherInterfaceStateMachine.this.mNMService.stopInterfaceForwarding(TetherInterfaceStateMachine.this.mIfaceName, str);
            } catch (Exception e2) {
            }
            try {
                TetherInterfaceStateMachine.this.mNMService.disableNat(TetherInterfaceStateMachine.this.mIfaceName, str);
            } catch (Exception e3) {
            }
        }

        @Override
        public boolean processMessage(Message message) {
            if (super.processMessage(message)) {
                return true;
            }
            TetherInterfaceStateMachine.this.logMessage(this, message.what);
            int i = message.what;
            if (i == 327782) {
                TetherInterfaceStateMachine.this.mLog.e("CMD_TETHER_REQUESTED while already tethering.");
            } else if (i == 327792) {
                InterfaceSet interfaceSet = (InterfaceSet) message.obj;
                if (!noChangeInUpstreamIfaceSet(interfaceSet)) {
                    if (interfaceSet == null) {
                        cleanupUpstream();
                    } else {
                        Iterator<String> it = upstreamInterfacesRemoved(interfaceSet).iterator();
                        while (it.hasNext()) {
                            cleanupUpstreamInterface(it.next());
                        }
                        Set<String> setUpstreamInterfacesAdd = upstreamInterfacesAdd(interfaceSet);
                        TetherInterfaceStateMachine.this.mUpstreamIfaceSet = interfaceSet;
                        for (String str : setUpstreamInterfacesAdd) {
                            try {
                                TetherInterfaceStateMachine.this.mNMService.enableNat(TetherInterfaceStateMachine.this.mIfaceName, str);
                                TetherInterfaceStateMachine.this.mNMService.startInterfaceForwarding(TetherInterfaceStateMachine.this.mIfaceName, str);
                            } catch (Exception e) {
                                TetherInterfaceStateMachine.this.mLog.e("Exception enabling NAT: " + e);
                                cleanupUpstream();
                                TetherInterfaceStateMachine.this.mLastError = 8;
                                TetherInterfaceStateMachine.this.transitionTo(TetherInterfaceStateMachine.this.mInitialState);
                                return true;
                            }
                        }
                    }
                }
            } else {
                return false;
            }
            return true;
        }

        private boolean noChangeInUpstreamIfaceSet(InterfaceSet interfaceSet) {
            if (TetherInterfaceStateMachine.this.mUpstreamIfaceSet == null && interfaceSet == null) {
                return true;
            }
            if (TetherInterfaceStateMachine.this.mUpstreamIfaceSet != null && interfaceSet != null) {
                return TetherInterfaceStateMachine.this.mUpstreamIfaceSet.equals(interfaceSet);
            }
            return false;
        }

        private Set<String> upstreamInterfacesRemoved(InterfaceSet interfaceSet) {
            if (TetherInterfaceStateMachine.this.mUpstreamIfaceSet == null) {
                return new HashSet();
            }
            HashSet hashSet = new HashSet(TetherInterfaceStateMachine.this.mUpstreamIfaceSet.ifnames);
            hashSet.removeAll(interfaceSet.ifnames);
            return hashSet;
        }

        private Set<String> upstreamInterfacesAdd(InterfaceSet interfaceSet) {
            HashSet hashSet = new HashSet(interfaceSet.ifnames);
            if (TetherInterfaceStateMachine.this.mUpstreamIfaceSet != null) {
                hashSet.removeAll(TetherInterfaceStateMachine.this.mUpstreamIfaceSet.ifnames);
            }
            return hashSet;
        }
    }

    class UnavailableState extends State {
        UnavailableState() {
        }

        public void enter() {
            TetherInterfaceStateMachine.this.mLastError = 0;
            TetherInterfaceStateMachine.this.sendInterfaceState(0);
        }
    }

    private static ArrayList<RouteInfo> getLocalRoutesFor(String str, HashSet<IpPrefix> hashSet) {
        ArrayList<RouteInfo> arrayList = new ArrayList<>();
        Iterator<IpPrefix> it = hashSet.iterator();
        while (it.hasNext()) {
            arrayList.add(new RouteInfo(it.next(), null, str));
        }
        return arrayList;
    }

    private static Inet6Address getLocalDnsIpFor(IpPrefix ipPrefix) {
        byte[] rawAddress = ipPrefix.getRawAddress();
        rawAddress[rawAddress.length - 1] = getRandomSanitizedByte((byte) 42, NetworkConstants.asByte(0), NetworkConstants.asByte(1));
        try {
            return Inet6Address.getByAddress((String) null, rawAddress, 0);
        } catch (UnknownHostException e) {
            Slog.wtf(TAG, "Failed to construct Inet6Address from: " + ipPrefix);
            return null;
        }
    }

    private static byte getRandomSanitizedByte(byte b, byte... bArr) {
        byte bNextInt = (byte) new Random().nextInt();
        for (byte b2 : bArr) {
            if (bNextInt == b2) {
                return b;
            }
        }
        return bNextInt;
    }
}
