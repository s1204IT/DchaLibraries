package com.android.server.connectivity.tethering;

import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkState;
import android.net.RouteInfo;
import android.net.util.NetworkConstants;
import android.net.util.SharedLog;
import android.util.Log;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class IPv6TetheringCoordinator {
    private static final boolean DBG = false;
    private static final String TAG = IPv6TetheringCoordinator.class.getSimpleName();
    private static final boolean VDBG = true;
    private final LinkedList<Downstream> mActiveDownstreams;
    private final SharedLog mLog;
    private short mNextSubnetId;
    private final ArrayList<TetherInterfaceStateMachine> mNotifyList;
    private final byte[] mUniqueLocalPrefix;
    private NetworkState mUpstreamNetworkState;

    private static class Downstream {
        public final int mode;
        public final short subnetId;
        public final TetherInterfaceStateMachine tism;

        Downstream(TetherInterfaceStateMachine tetherInterfaceStateMachine, int i, short s) {
            this.tism = tetherInterfaceStateMachine;
            this.mode = i;
            this.subnetId = s;
        }
    }

    public IPv6TetheringCoordinator() {
        this.mNotifyList = null;
        this.mLog = null;
        this.mActiveDownstreams = null;
        this.mUniqueLocalPrefix = null;
    }

    public IPv6TetheringCoordinator(ArrayList<TetherInterfaceStateMachine> arrayList, SharedLog sharedLog) {
        this.mNotifyList = arrayList;
        this.mLog = sharedLog.forSubComponent(TAG);
        this.mActiveDownstreams = new LinkedList<>();
        this.mUniqueLocalPrefix = generateUniqueLocalPrefix();
        this.mNextSubnetId = (short) 0;
    }

    public void addActiveDownstream(TetherInterfaceStateMachine tetherInterfaceStateMachine, int i) {
        if (findDownstream(tetherInterfaceStateMachine) == null) {
            if (this.mActiveDownstreams.offer(new Downstream(tetherInterfaceStateMachine, i, this.mNextSubnetId))) {
                this.mNextSubnetId = (short) Math.max(0, this.mNextSubnetId + 1);
            }
            updateIPv6TetheringInterfaces();
        }
    }

    public void removeActiveDownstream(TetherInterfaceStateMachine tetherInterfaceStateMachine) {
        stopIPv6TetheringOn(tetherInterfaceStateMachine);
        if (this.mActiveDownstreams.remove(findDownstream(tetherInterfaceStateMachine))) {
            updateIPv6TetheringInterfaces();
        }
        if (this.mNotifyList.isEmpty()) {
            if (!this.mActiveDownstreams.isEmpty()) {
                Log.wtf(TAG, "Tethering notify list empty, IPv6 downstreams non-empty.");
            }
            this.mNextSubnetId = (short) 0;
        }
    }

    public void updateUpstreamNetworkState(NetworkState networkState) {
        Log.d(TAG, "updateUpstreamNetworkState: " + toDebugString(networkState));
        if (TetheringInterfaceUtils.getIPv6Interface(networkState) == null) {
            stopIPv6TetheringOnAllInterfaces();
            setUpstreamNetworkState(null);
            return;
        }
        if (this.mUpstreamNetworkState != null && !networkState.network.equals(this.mUpstreamNetworkState.network)) {
            stopIPv6TetheringOnAllInterfaces();
        }
        setUpstreamNetworkState(networkState);
        updateIPv6TetheringInterfaces();
    }

    private void stopIPv6TetheringOnAllInterfaces() {
        Iterator<TetherInterfaceStateMachine> it = this.mNotifyList.iterator();
        while (it.hasNext()) {
            stopIPv6TetheringOn(it.next());
        }
    }

    private void setUpstreamNetworkState(NetworkState networkState) {
        if (networkState == null) {
            this.mUpstreamNetworkState = null;
        } else {
            this.mUpstreamNetworkState = new NetworkState((NetworkInfo) null, new LinkProperties(networkState.linkProperties), new NetworkCapabilities(networkState.networkCapabilities), new Network(networkState.network), (String) null, (String) null);
        }
        this.mLog.log("setUpstreamNetworkState: " + toDebugString(this.mUpstreamNetworkState));
    }

    private void updateIPv6TetheringInterfaces() {
        Iterator<TetherInterfaceStateMachine> it = this.mNotifyList.iterator();
        if (it.hasNext()) {
            TetherInterfaceStateMachine next = it.next();
            next.sendMessage(TetherInterfaceStateMachine.CMD_IPV6_TETHER_UPDATE, 0, 0, getInterfaceIPv6LinkProperties(next));
        }
    }

    private LinkProperties getInterfaceIPv6LinkProperties(TetherInterfaceStateMachine tetherInterfaceStateMachine) {
        Downstream downstreamFindDownstream;
        Downstream downstreamPeek;
        if (tetherInterfaceStateMachine.interfaceType() == 2 || (downstreamFindDownstream = findDownstream(tetherInterfaceStateMachine)) == null) {
            return null;
        }
        if (downstreamFindDownstream.mode == 3) {
            return getUniqueLocalConfig(this.mUniqueLocalPrefix, downstreamFindDownstream.subnetId);
        }
        if (this.mUpstreamNetworkState != null && this.mUpstreamNetworkState.linkProperties != null && (downstreamPeek = this.mActiveDownstreams.peek()) != null && downstreamPeek.tism == tetherInterfaceStateMachine) {
            LinkProperties iPv6OnlyLinkProperties = getIPv6OnlyLinkProperties(this.mUpstreamNetworkState.linkProperties);
            if (iPv6OnlyLinkProperties.hasIPv6DefaultRoute() && iPv6OnlyLinkProperties.hasGlobalIPv6Address()) {
                return iPv6OnlyLinkProperties;
            }
        }
        return null;
    }

    Downstream findDownstream(TetherInterfaceStateMachine tetherInterfaceStateMachine) {
        for (Downstream downstream : this.mActiveDownstreams) {
            if (downstream.tism == tetherInterfaceStateMachine) {
                return downstream;
            }
        }
        return null;
    }

    private static LinkProperties getIPv6OnlyLinkProperties(LinkProperties linkProperties) {
        LinkProperties linkProperties2 = new LinkProperties();
        if (linkProperties == null) {
            return linkProperties2;
        }
        linkProperties2.setInterfaceName(linkProperties.getInterfaceName());
        linkProperties2.setMtu(linkProperties.getMtu());
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            if (linkAddress.isGlobalPreferred() && linkAddress.getPrefixLength() == 64) {
                linkProperties2.addLinkAddress(linkAddress);
            }
        }
        for (RouteInfo routeInfo : linkProperties.getRoutes()) {
            IpPrefix destination = routeInfo.getDestination();
            if ((destination.getAddress() instanceof Inet6Address) && destination.getPrefixLength() <= 64) {
                linkProperties2.addRoute(routeInfo);
            }
        }
        for (InetAddress inetAddress : linkProperties.getDnsServers()) {
            if (isIPv6GlobalAddress(inetAddress)) {
                linkProperties2.addDnsServer(inetAddress);
            }
        }
        linkProperties2.setDomains(linkProperties.getDomains());
        return linkProperties2;
    }

    private static boolean isIPv6GlobalAddress(InetAddress inetAddress) {
        return (!(inetAddress instanceof Inet6Address) || inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress() || inetAddress.isSiteLocalAddress() || inetAddress.isMulticastAddress()) ? false : true;
    }

    private static LinkProperties getUniqueLocalConfig(byte[] bArr, short s) {
        LinkProperties linkProperties = new LinkProperties();
        linkProperties.addRoute(new RouteInfo(makeUniqueLocalPrefix(bArr, (short) 0, 48), null, null));
        linkProperties.addLinkAddress(new LinkAddress(makeUniqueLocalPrefix(bArr, s, 64).getAddress(), 64));
        linkProperties.setMtu(NetworkConstants.ETHER_MTU);
        return linkProperties;
    }

    private static IpPrefix makeUniqueLocalPrefix(byte[] bArr, short s, int i) {
        byte[] bArrCopyOf = Arrays.copyOf(bArr, bArr.length);
        bArrCopyOf[7] = (byte) (s >> 8);
        bArrCopyOf[8] = (byte) s;
        return new IpPrefix(bArrCopyOf, i);
    }

    private static byte[] generateUniqueLocalPrefix() {
        byte[] bArr = new byte[6];
        new Random().nextBytes(bArr);
        byte[] bArrCopyOf = Arrays.copyOf(bArr, 16);
        bArrCopyOf[0] = -3;
        return bArrCopyOf;
    }

    private static String toDebugString(NetworkState networkState) {
        if (networkState == null) {
            return "NetworkState{null}";
        }
        return String.format("NetworkState{%s, %s, %s}", networkState.network, networkState.networkCapabilities, networkState.linkProperties);
    }

    private static void stopIPv6TetheringOn(TetherInterfaceStateMachine tetherInterfaceStateMachine) {
        tetherInterfaceStateMachine.sendMessage(TetherInterfaceStateMachine.CMD_IPV6_TETHER_UPDATE, 0, 0, null);
    }
}
