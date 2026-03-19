package android.net.ip;

import android.content.Context;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.net.metrics.IpReachabilityEvent;
import android.net.netlink.NetlinkConstants;
import android.net.netlink.NetlinkErrorMessage;
import android.net.netlink.NetlinkMessage;
import android.net.netlink.NetlinkSocket;
import android.net.netlink.RtNetlinkNeighborMessage;
import android.net.netlink.StructNdMsg;
import android.os.PowerManager;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.NetlinkSocketAddress;
import android.system.OsConstants;
import android.util.Log;
import com.android.internal.annotations.GuardedBy;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IpReachabilityMonitor {
    private static final boolean DBG = true;
    private static final String TAG = "IpReachabilityMonitor";
    private static final boolean VDBG = true;
    private final Callback mCallback;
    private final int mInterfaceIndex;
    private final String mInterfaceName;

    @GuardedBy("mLock")
    private int mIpWatchListVersion;
    private final NetlinkSocketObserver mNetlinkSocketObserver;
    private final Thread mObserverThread;

    @GuardedBy("mLock")
    private boolean mRunning;
    private final PowerManager.WakeLock mWakeLock;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private LinkProperties mLinkProperties = new LinkProperties();

    @GuardedBy("mLock")
    private Map<InetAddress, Short> mIpWatchList = new HashMap();

    public interface Callback {
        void notifyLost(InetAddress inetAddress, String str);
    }

    private static int probeNeighbor(int ifIndex, InetAddress ip) throws Throwable {
        NetlinkSocket nlSocket;
        Throwable th;
        String errmsg;
        String msgSnippet = "probing ip=" + ip.getHostAddress() + "%" + ifIndex;
        Log.d(TAG, msgSnippet);
        byte[] msg = RtNetlinkNeighborMessage.newNewNeighborMessage(1, ip, (short) 16, ifIndex, null);
        int errno = -OsConstants.EPROTO;
        Throwable th2 = null;
        try {
            try {
                nlSocket = new NetlinkSocket(OsConstants.NETLINK_ROUTE);
                try {
                    nlSocket.connectToKernel();
                    nlSocket.sendMessage(msg, 0, msg.length, 300L);
                    ByteBuffer bytes = nlSocket.recvMessage(300L);
                    NetlinkMessage response = NetlinkMessage.parse(bytes);
                    if (response == null || !(response instanceof NetlinkErrorMessage) || ((NetlinkErrorMessage) response).getNlMsgError() == null) {
                        if (response == null) {
                            bytes.position(0);
                            errmsg = "raw bytes: " + NetlinkConstants.hexify(bytes);
                        } else {
                            errmsg = response.toString();
                        }
                        Log.e(TAG, "Error " + msgSnippet + ", errmsg=" + errmsg);
                    } else {
                        errno = ((NetlinkErrorMessage) response).getNlMsgError().error;
                        if (errno != 0) {
                            Log.e(TAG, "Error " + msgSnippet + ", errmsg=" + response.toString());
                        }
                    }
                    if (nlSocket != null) {
                        try {
                            nlSocket.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 != null) {
                        throw th2;
                    }
                    return errno;
                } catch (Throwable th4) {
                    th = th4;
                    try {
                        throw th;
                    } catch (Throwable th5) {
                        th = th;
                        th = th5;
                        if (nlSocket != null) {
                            try {
                                nlSocket.close();
                            } catch (Throwable th6) {
                                if (th == null) {
                                    th = th6;
                                } else if (th != th6) {
                                    th.addSuppressed(th6);
                                }
                            }
                        }
                        if (th == null) {
                            throw th;
                        }
                        throw th;
                    }
                }
            } catch (ErrnoException e) {
                Log.e(TAG, "Error " + msgSnippet, e);
                int errno2 = -e.errno;
                return errno2;
            } catch (InterruptedIOException e2) {
                Log.e(TAG, "Error " + msgSnippet, e2);
                int errno3 = -OsConstants.ETIMEDOUT;
                return errno3;
            } catch (SocketException e3) {
                Log.e(TAG, "Error " + msgSnippet, e3);
                int errno4 = -OsConstants.EIO;
                return errno4;
            }
        } catch (Throwable th7) {
            th = th7;
            nlSocket = null;
        }
    }

    public IpReachabilityMonitor(Context context, String ifName, Callback callback) throws IllegalArgumentException {
        this.mInterfaceName = ifName;
        try {
            NetworkInterface netIf = NetworkInterface.getByName(ifName);
            this.mInterfaceIndex = netIf.getIndex();
            this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, "IpReachabilityMonitor." + this.mInterfaceName);
            this.mCallback = callback;
            this.mNetlinkSocketObserver = new NetlinkSocketObserver(this, null);
            this.mObserverThread = new Thread(this.mNetlinkSocketObserver);
            this.mObserverThread.start();
        } catch (NullPointerException | SocketException e) {
            throw new IllegalArgumentException("invalid interface '" + ifName + "': ", e);
        }
    }

    public void stop() {
        synchronized (this.mLock) {
            this.mRunning = false;
        }
        clearLinkProperties();
        this.mNetlinkSocketObserver.clearNetlinkSocket();
    }

    private String describeWatchList() {
        StringBuilder sb = new StringBuilder();
        synchronized (this.mLock) {
            sb.append("iface{").append(this.mInterfaceName).append("/").append(this.mInterfaceIndex).append("}, ");
            sb.append("v{").append(this.mIpWatchListVersion).append("}, ");
            sb.append("ntable=[");
            boolean firstTime = true;
            for (Map.Entry<InetAddress, Short> entry : this.mIpWatchList.entrySet()) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    sb.append(", ");
                }
                sb.append(entry.getKey().getHostAddress()).append("/").append(StructNdMsg.stringForNudState(entry.getValue().shortValue()));
            }
            sb.append("]");
        }
        return sb.toString();
    }

    private boolean isWatching(InetAddress ip) {
        boolean zContainsKey;
        synchronized (this.mLock) {
            zContainsKey = this.mRunning ? this.mIpWatchList.containsKey(ip) : false;
        }
        return zContainsKey;
    }

    private boolean stillRunning() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mRunning;
        }
        return z;
    }

    private static boolean isOnLink(List<RouteInfo> routes, InetAddress ip) {
        for (RouteInfo route : routes) {
            if (!route.hasGateway() && route.matches(ip)) {
                return true;
            }
        }
        return false;
    }

    private short getNeighborStateLocked(InetAddress ip) {
        if (this.mIpWatchList.containsKey(ip)) {
            return this.mIpWatchList.get(ip).shortValue();
        }
        return (short) 0;
    }

    public void updateLinkProperties(LinkProperties lp) {
        if (!this.mInterfaceName.equals(lp.getInterfaceName())) {
            Log.wtf(TAG, "requested LinkProperties interface '" + lp.getInterfaceName() + "' does not match: " + this.mInterfaceName);
            return;
        }
        synchronized (this.mLock) {
            this.mLinkProperties = new LinkProperties(lp);
            Map<InetAddress, Short> newIpWatchList = new HashMap<>();
            List<RouteInfo> routes = this.mLinkProperties.getRoutes();
            for (RouteInfo route : routes) {
                if (route.hasGateway()) {
                    InetAddress gw = route.getGateway();
                    if (isOnLink(routes, gw)) {
                        newIpWatchList.put(gw, Short.valueOf(getNeighborStateLocked(gw)));
                    }
                }
            }
            for (InetAddress nameserver : lp.getDnsServers()) {
                if (isOnLink(routes, nameserver)) {
                    newIpWatchList.put(nameserver, Short.valueOf(getNeighborStateLocked(nameserver)));
                }
            }
            this.mIpWatchList = newIpWatchList;
            this.mIpWatchListVersion++;
        }
        Log.d(TAG, "watch: " + describeWatchList());
    }

    public void clearLinkProperties() {
        synchronized (this.mLock) {
            this.mLinkProperties.clear();
            this.mIpWatchList.clear();
            this.mIpWatchListVersion++;
        }
        Log.d(TAG, "clear: " + describeWatchList());
    }

    private void handleNeighborLost(String msg) {
        LinkProperties.ProvisioningChange delta;
        InetAddress ip = null;
        synchronized (this.mLock) {
            LinkProperties whatIfLp = new LinkProperties(this.mLinkProperties);
            for (Map.Entry<InetAddress, Short> entry : this.mIpWatchList.entrySet()) {
                if (entry.getValue().shortValue() == 32) {
                    ip = entry.getKey();
                    for (RouteInfo route : this.mLinkProperties.getRoutes()) {
                        if (ip.equals(route.getGateway())) {
                            whatIfLp.removeRoute(route);
                        }
                    }
                    whatIfLp.removeDnsServer(ip);
                }
            }
            delta = LinkProperties.compareProvisioning(this.mLinkProperties, whatIfLp);
        }
        if (delta == LinkProperties.ProvisioningChange.LOST_PROVISIONING) {
            IpReachabilityEvent.logProvisioningLost(this.mInterfaceName);
            String logMsg = "FAILURE: LOST_PROVISIONING, " + msg;
            Log.w(TAG, logMsg);
            if (this.mCallback == null) {
                return;
            }
            this.mCallback.notifyLost(ip, logMsg);
            return;
        }
        IpReachabilityEvent.logNudFailed(this.mInterfaceName);
    }

    public void probeAll() throws Throwable {
        Set<InetAddress> ipProbeList = new HashSet<>();
        synchronized (this.mLock) {
            ipProbeList.addAll(this.mIpWatchList.keySet());
        }
        if (!ipProbeList.isEmpty() && stillRunning()) {
            this.mWakeLock.acquire(getProbeWakeLockDuration());
        }
        for (InetAddress target : ipProbeList) {
            if (!stillRunning()) {
                return;
            }
            int returnValue = probeNeighbor(this.mInterfaceIndex, target);
            IpReachabilityEvent.logProbeEvent(this.mInterfaceName, returnValue);
            if (returnValue != 0) {
                IpReachabilityEvent.logProbeEvent(this.mInterfaceName, 0);
                sendUdpBroadcast(target);
            }
        }
    }

    private void sendUdpBroadcast(InetAddress target) {
        Log.d(TAG, "sendUdpBroadcast with " + target);
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] data = {-1};
            DatagramPacket packet = new DatagramPacket(data, data.length, target, 19191);
            socket.send(packet);
        } catch (Exception e) {
            Log.e(TAG, "sendUdpBroadcast: " + e);
        }
    }

    private long getProbeWakeLockDuration() {
        return 3500L;
    }

    private final class NetlinkSocketObserver implements Runnable {
        private NetlinkSocket mSocket;

        NetlinkSocketObserver(IpReachabilityMonitor this$0, NetlinkSocketObserver netlinkSocketObserver) {
            this();
        }

        private NetlinkSocketObserver() {
        }

        @Override
        public void run() {
            Log.d(IpReachabilityMonitor.TAG, "Starting observing thread.");
            synchronized (IpReachabilityMonitor.this.mLock) {
                IpReachabilityMonitor.this.mRunning = true;
            }
            try {
                setupNetlinkSocket();
            } catch (ErrnoException | SocketException e) {
                Log.e(IpReachabilityMonitor.TAG, "Failed to suitably initialize a netlink socket", e);
                synchronized (IpReachabilityMonitor.this.mLock) {
                    IpReachabilityMonitor.this.mRunning = false;
                }
            }
            while (IpReachabilityMonitor.this.stillRunning()) {
                try {
                    ByteBuffer byteBuffer = recvKernelReply();
                    long whenMs = SystemClock.elapsedRealtime();
                    if (byteBuffer != null) {
                        parseNetlinkMessageBuffer(byteBuffer, whenMs);
                    }
                } catch (ErrnoException e2) {
                    if (IpReachabilityMonitor.this.stillRunning()) {
                        Log.w(IpReachabilityMonitor.TAG, "ErrnoException: ", e2);
                    }
                }
            }
            clearNetlinkSocket();
            synchronized (IpReachabilityMonitor.this.mLock) {
                IpReachabilityMonitor.this.mRunning = false;
            }
            Log.d(IpReachabilityMonitor.TAG, "Finishing observing thread.");
        }

        private void clearNetlinkSocket() {
            if (this.mSocket == null) {
                return;
            }
            this.mSocket.close();
        }

        private void setupNetlinkSocket() throws SocketException, ErrnoException {
            clearNetlinkSocket();
            this.mSocket = new NetlinkSocket(OsConstants.NETLINK_ROUTE);
            NetlinkSocketAddress listenAddr = new NetlinkSocketAddress(0, OsConstants.RTMGRP_NEIGH);
            this.mSocket.bind(listenAddr);
            NetlinkSocketAddress nlAddr = this.mSocket.getLocalAddress();
            Log.d(IpReachabilityMonitor.TAG, "bound to sockaddr_nl{" + (nlAddr.getPortId() & (-1)) + ", " + nlAddr.getGroupsMask() + "}");
        }

        private ByteBuffer recvKernelReply() throws ErrnoException {
            try {
                return this.mSocket.recvMessage(0L);
            } catch (ErrnoException e) {
                if (e.errno != OsConstants.EAGAIN) {
                    throw e;
                }
                return null;
            } catch (InterruptedIOException e2) {
                return null;
            }
        }

        private void parseNetlinkMessageBuffer(ByteBuffer byteBuffer, long whenMs) {
            while (byteBuffer.remaining() > 0) {
                int position = byteBuffer.position();
                NetlinkMessage nlMsg = NetlinkMessage.parse(byteBuffer);
                if (nlMsg == null || nlMsg.getHeader() == null) {
                    byteBuffer.position(position);
                    Log.e(IpReachabilityMonitor.TAG, "unparsable netlink msg: " + NetlinkConstants.hexify(byteBuffer));
                    return;
                }
                int srcPortId = nlMsg.getHeader().nlmsg_pid;
                if (srcPortId != 0) {
                    Log.e(IpReachabilityMonitor.TAG, "non-kernel source portId: " + (srcPortId & (-1)));
                    return;
                } else if (nlMsg instanceof NetlinkErrorMessage) {
                    Log.e(IpReachabilityMonitor.TAG, "netlink error: " + nlMsg);
                } else if (!(nlMsg instanceof RtNetlinkNeighborMessage)) {
                    Log.d(IpReachabilityMonitor.TAG, "non-rtnetlink neighbor msg: " + nlMsg);
                } else {
                    evaluateRtNetlinkNeighborMessage((RtNetlinkNeighborMessage) nlMsg, whenMs);
                }
            }
        }

        private void evaluateRtNetlinkNeighborMessage(RtNetlinkNeighborMessage neighMsg, long whenMs) {
            StructNdMsg ndMsg = neighMsg.getNdHeader();
            if (ndMsg == null || ndMsg.ndm_ifindex != IpReachabilityMonitor.this.mInterfaceIndex) {
                return;
            }
            InetAddress destination = neighMsg.getDestination();
            if (IpReachabilityMonitor.this.isWatching(destination)) {
                short msgType = neighMsg.getHeader().nlmsg_type;
                short nudState = ndMsg.ndm_state;
                String eventMsg = "NeighborEvent{elapsedMs=" + whenMs + ", " + destination.getHostAddress() + ", [" + NetlinkConstants.hexify(neighMsg.getLinkLayerAddress()) + "], " + NetlinkConstants.stringForNlMsgType(msgType) + ", " + StructNdMsg.stringForNudState(nudState) + "}";
                Log.d(IpReachabilityMonitor.TAG, neighMsg.toString());
                synchronized (IpReachabilityMonitor.this.mLock) {
                    if (IpReachabilityMonitor.this.mIpWatchList.containsKey(destination)) {
                        IpReachabilityMonitor.this.mIpWatchList.put(destination, Short.valueOf(msgType == 29 ? (short) 0 : nudState));
                    }
                }
                if (nudState == 32) {
                    Log.w(IpReachabilityMonitor.TAG, "ALERT: " + eventMsg);
                    IpReachabilityMonitor.this.handleNeighborLost(eventMsg);
                }
            }
        }
    }
}
