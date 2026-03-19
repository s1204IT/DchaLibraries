package android.net.ip;

import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.TrafficStats;
import android.net.util.InterfaceParams;
import android.net.util.NetworkConstants;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.util.Log;
import com.android.bluetooth.opp.BluetoothShare;
import com.android.internal.annotations.GuardedBy;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import libcore.io.IoBridge;

public class RouterAdvertisementDaemon {
    private static final int DAY_IN_SECONDS = 86400;
    private static final int DEFAULT_LIFETIME = 3600;
    private static final int MAX_RTR_ADV_INTERVAL_SEC = 600;
    private static final int MAX_URGENT_RTR_ADVERTISEMENTS = 5;
    private static final int MIN_DELAY_BETWEEN_RAS_SEC = 3;
    private static final int MIN_RA_HEADER_SIZE = 16;
    private static final int MIN_RTR_ADV_INTERVAL_SEC = 300;
    private final InetSocketAddress mAllNodes;
    private final InterfaceParams mInterface;
    private volatile MulticastTransmitter mMulticastTransmitter;

    @GuardedBy("mLock")
    private int mRaLength;

    @GuardedBy("mLock")
    private RaParams mRaParams;
    private volatile FileDescriptor mSocket;
    private volatile UnicastResponder mUnicastResponder;
    private static final String TAG = RouterAdvertisementDaemon.class.getSimpleName();
    private static final byte ICMPV6_ND_ROUTER_SOLICIT = asByte(133);
    private static final byte ICMPV6_ND_ROUTER_ADVERT = asByte(134);
    private static final byte[] ALL_NODES = {-1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final byte[] mRA = new byte[NetworkConstants.IPV6_MIN_MTU];

    @GuardedBy("mLock")
    private final DeprecatedInfoTracker mDeprecatedInfoTracker = new DeprecatedInfoTracker();

    public static class RaParams {
        public HashSet<Inet6Address> dnses;
        public boolean hasDefaultRoute;
        public int mtu;
        public HashSet<IpPrefix> prefixes;

        public RaParams() {
            this.hasDefaultRoute = false;
            this.mtu = NetworkConstants.IPV6_MIN_MTU;
            this.prefixes = new HashSet<>();
            this.dnses = new HashSet<>();
        }

        public RaParams(RaParams raParams) {
            this.hasDefaultRoute = raParams.hasDefaultRoute;
            this.mtu = raParams.mtu;
            this.prefixes = (HashSet) raParams.prefixes.clone();
            this.dnses = (HashSet) raParams.dnses.clone();
        }

        public static RaParams getDeprecatedRaParams(RaParams raParams, RaParams raParams2) {
            RaParams raParams3 = new RaParams();
            if (raParams != null) {
                for (IpPrefix ipPrefix : raParams.prefixes) {
                    if (raParams2 == null || !raParams2.prefixes.contains(ipPrefix)) {
                        raParams3.prefixes.add(ipPrefix);
                    }
                }
                for (Inet6Address inet6Address : raParams.dnses) {
                    if (raParams2 == null || !raParams2.dnses.contains(inet6Address)) {
                        raParams3.dnses.add(inet6Address);
                    }
                }
            }
            return raParams3;
        }
    }

    private static class DeprecatedInfoTracker {
        private final HashMap<Inet6Address, Integer> mDnses;
        private final HashMap<IpPrefix, Integer> mPrefixes;

        private DeprecatedInfoTracker() {
            this.mPrefixes = new HashMap<>();
            this.mDnses = new HashMap<>();
        }

        Set<IpPrefix> getPrefixes() {
            return this.mPrefixes.keySet();
        }

        void putPrefixes(Set<IpPrefix> set) {
            Iterator<IpPrefix> it = set.iterator();
            while (it.hasNext()) {
                this.mPrefixes.put(it.next(), 5);
            }
        }

        void removePrefixes(Set<IpPrefix> set) {
            Iterator<IpPrefix> it = set.iterator();
            while (it.hasNext()) {
                this.mPrefixes.remove(it.next());
            }
        }

        Set<Inet6Address> getDnses() {
            return this.mDnses.keySet();
        }

        void putDnses(Set<Inet6Address> set) {
            Iterator<Inet6Address> it = set.iterator();
            while (it.hasNext()) {
                this.mDnses.put(it.next(), 5);
            }
        }

        void removeDnses(Set<Inet6Address> set) {
            Iterator<Inet6Address> it = set.iterator();
            while (it.hasNext()) {
                this.mDnses.remove(it.next());
            }
        }

        boolean isEmpty() {
            return this.mPrefixes.isEmpty() && this.mDnses.isEmpty();
        }

        private boolean decrementCounters() {
            return decrementCounter(this.mPrefixes) | decrementCounter(this.mDnses);
        }

        private <T> boolean decrementCounter(HashMap<T, Integer> map) {
            Iterator<Map.Entry<T, Integer>> it = map.entrySet().iterator();
            boolean z = false;
            while (it.hasNext()) {
                Map.Entry<T, Integer> next = it.next();
                if (next.getValue().intValue() == 0) {
                    it.remove();
                    z = true;
                } else {
                    next.setValue(Integer.valueOf(next.getValue().intValue() - 1));
                }
            }
            return z;
        }
    }

    public RouterAdvertisementDaemon(InterfaceParams interfaceParams) {
        this.mInterface = interfaceParams;
        this.mAllNodes = new InetSocketAddress(getAllNodesForScopeId(this.mInterface.index), 0);
    }

    public void buildNewRa(RaParams raParams, RaParams raParams2) {
        synchronized (this.mLock) {
            if (raParams != null) {
                try {
                    this.mDeprecatedInfoTracker.putPrefixes(raParams.prefixes);
                    this.mDeprecatedInfoTracker.putDnses(raParams.dnses);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (raParams2 != null) {
                this.mDeprecatedInfoTracker.removePrefixes(raParams2.prefixes);
                this.mDeprecatedInfoTracker.removeDnses(raParams2.dnses);
            }
            this.mRaParams = raParams2;
            assembleRaLocked();
        }
        maybeNotifyMulticastTransmitter();
    }

    public boolean start() {
        if (!createSocket()) {
            return false;
        }
        this.mMulticastTransmitter = new MulticastTransmitter();
        this.mMulticastTransmitter.start();
        this.mUnicastResponder = new UnicastResponder();
        this.mUnicastResponder.start();
        return true;
    }

    public void stop() {
        closeSocket();
        if (this.mMulticastTransmitter != null) {
            this.mMulticastTransmitter.hup();
            this.mMulticastTransmitter = null;
        }
        this.mUnicastResponder = null;
    }

    @GuardedBy("mLock")
    private void assembleRaLocked() {
        boolean z;
        boolean z2;
        ByteBuffer byteBufferWrap = ByteBuffer.wrap(this.mRA);
        byteBufferWrap.order(ByteOrder.BIG_ENDIAN);
        try {
            z = true;
            putHeader(byteBufferWrap, this.mRaParams != null && this.mRaParams.hasDefaultRoute);
            putSlla(byteBufferWrap, this.mInterface.macAddr.toByteArray());
            this.mRaLength = byteBufferWrap.position();
            if (this.mRaParams == null) {
                z2 = false;
            } else {
                putMtu(byteBufferWrap, this.mRaParams.mtu);
                this.mRaLength = byteBufferWrap.position();
                Iterator<IpPrefix> it = this.mRaParams.prefixes.iterator();
                z2 = false;
                while (it.hasNext()) {
                    try {
                        putPio(byteBufferWrap, it.next(), DEFAULT_LIFETIME, DEFAULT_LIFETIME);
                        this.mRaLength = byteBufferWrap.position();
                        z2 = true;
                    } catch (BufferOverflowException e) {
                        e = e;
                        z = z2;
                        Log.e(TAG, "Could not construct new RA: " + e);
                    }
                }
                if (this.mRaParams.dnses.size() > 0) {
                    putRdnss(byteBufferWrap, this.mRaParams.dnses, DEFAULT_LIFETIME);
                    this.mRaLength = byteBufferWrap.position();
                    z2 = true;
                }
            }
            Iterator<IpPrefix> it2 = this.mDeprecatedInfoTracker.getPrefixes().iterator();
            while (it2.hasNext()) {
                putPio(byteBufferWrap, it2.next(), 0, 0);
                this.mRaLength = byteBufferWrap.position();
                z2 = true;
            }
            Set<Inet6Address> dnses = this.mDeprecatedInfoTracker.getDnses();
            if (!dnses.isEmpty()) {
                putRdnss(byteBufferWrap, dnses, 0);
                this.mRaLength = byteBufferWrap.position();
            } else {
                z = z2;
            }
        } catch (BufferOverflowException e2) {
            e = e2;
            z = false;
        }
        if (!z) {
            this.mRaLength = 0;
        }
    }

    private void maybeNotifyMulticastTransmitter() {
        MulticastTransmitter multicastTransmitter = this.mMulticastTransmitter;
        if (multicastTransmitter != null) {
            multicastTransmitter.hup();
        }
    }

    private static Inet6Address getAllNodesForScopeId(int i) {
        try {
            return Inet6Address.getByAddress("ff02::1", ALL_NODES, i);
        } catch (UnknownHostException e) {
            Log.wtf(TAG, "Failed to construct ff02::1 InetAddress: " + e);
            return null;
        }
    }

    private static byte asByte(int i) {
        return (byte) i;
    }

    private static short asShort(int i) {
        return (short) i;
    }

    private static void putHeader(ByteBuffer byteBuffer, boolean z) {
        byteBuffer.put(ICMPV6_ND_ROUTER_ADVERT).put(asByte(0)).putShort(asShort(0)).put((byte) 64).put(z ? asByte(8) : asByte(0)).putShort(z ? asShort(DEFAULT_LIFETIME) : asShort(0)).putInt(0).putInt(0);
    }

    private static void putSlla(ByteBuffer byteBuffer, byte[] bArr) {
        if (bArr == null || bArr.length != 6) {
            return;
        }
        byteBuffer.put((byte) 1).put((byte) 1).put(bArr);
    }

    private static void putExpandedFlagsOption(ByteBuffer byteBuffer) {
        byteBuffer.put((byte) 26).put((byte) 1).putShort(asShort(0)).putInt(0);
    }

    private static void putMtu(ByteBuffer byteBuffer, int i) {
        ByteBuffer byteBufferPutShort = byteBuffer.put((byte) 5).put((byte) 1).putShort(asShort(0));
        if (i < 1280) {
            i = 1280;
        }
        byteBufferPutShort.putInt(i);
    }

    private static void putPio(ByteBuffer byteBuffer, IpPrefix ipPrefix, int i, int i2) {
        int prefixLength = ipPrefix.getPrefixLength();
        if (prefixLength != 64) {
            return;
        }
        if (i < 0) {
            i = 0;
        }
        if (i2 < 0) {
            i2 = 0;
        }
        if (i2 > i) {
            i2 = i;
        }
        byteBuffer.put((byte) 3).put((byte) 4).put(asByte(prefixLength)).put(asByte(BluetoothShare.STATUS_RUNNING)).putInt(i).putInt(i2).putInt(0).put(ipPrefix.getAddress().getAddress());
    }

    private static void putRio(ByteBuffer byteBuffer, IpPrefix ipPrefix) {
        int i;
        int prefixLength = ipPrefix.getPrefixLength();
        if (prefixLength > 64) {
            return;
        }
        if (prefixLength == 0) {
            i = 1;
        } else {
            i = prefixLength <= 8 ? 2 : 3;
        }
        byte bAsByte = asByte(i);
        byte[] address = ipPrefix.getAddress().getAddress();
        byteBuffer.put((byte) 24).put(bAsByte).put(asByte(prefixLength)).put(asByte(24)).putInt(DEFAULT_LIFETIME);
        if (prefixLength > 0) {
            byteBuffer.put(address, 0, prefixLength > 64 ? 16 : 8);
        }
    }

    private static void putRdnss(ByteBuffer byteBuffer, Set<Inet6Address> set, int i) {
        HashSet hashSet = new HashSet();
        for (Inet6Address inet6Address : set) {
            if (new LinkAddress(inet6Address, 64).isGlobalPreferred()) {
                hashSet.add(inet6Address);
            }
        }
        if (hashSet.isEmpty()) {
            return;
        }
        byteBuffer.put((byte) 25).put(asByte((set.size() * 2) + 1)).putShort(asShort(0)).putInt(i);
        Iterator it = hashSet.iterator();
        while (it.hasNext()) {
            byteBuffer.put(((Inet6Address) it.next()).getAddress());
        }
    }

    private boolean createSocket() {
        int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-189);
        try {
            try {
                this.mSocket = Os.socket(OsConstants.AF_INET6, OsConstants.SOCK_RAW, OsConstants.IPPROTO_ICMPV6);
                Os.setsockoptTimeval(this.mSocket, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(300L));
                Os.setsockoptIfreq(this.mSocket, OsConstants.SOL_SOCKET, OsConstants.SO_BINDTODEVICE, this.mInterface.name);
                NetworkUtils.protectFromVpn(this.mSocket);
                NetworkUtils.setupRaSocket(this.mSocket, this.mInterface.index);
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                return true;
            } catch (ErrnoException | IOException e) {
                Log.e(TAG, "Failed to create RA daemon socket: " + e);
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                return false;
            }
        } catch (Throwable th) {
            TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
            throw th;
        }
    }

    private void closeSocket() {
        if (this.mSocket != null) {
            try {
                IoBridge.closeAndSignalBlockedThreads(this.mSocket);
            } catch (IOException e) {
            }
        }
        this.mSocket = null;
    }

    private boolean isSocketValid() {
        FileDescriptor fileDescriptor = this.mSocket;
        return fileDescriptor != null && fileDescriptor.valid();
    }

    private boolean isSuitableDestination(InetSocketAddress inetSocketAddress) {
        if (this.mAllNodes.equals(inetSocketAddress)) {
            return true;
        }
        ?? address = inetSocketAddress.getAddress();
        return (address instanceof Inet6Address) && address.isLinkLocalAddress() && address.getScopeId() == this.mInterface.index;
    }

    private void maybeSendRA(InetSocketAddress inetSocketAddress) {
        if (inetSocketAddress == null || !isSuitableDestination(inetSocketAddress)) {
            inetSocketAddress = this.mAllNodes;
        }
        try {
            synchronized (this.mLock) {
                if (this.mRaLength < 16) {
                    return;
                }
                Os.sendto(this.mSocket, this.mRA, 0, this.mRaLength, 0, inetSocketAddress);
                Log.d(TAG, "RA sendto " + inetSocketAddress.getAddress().getHostAddress());
            }
        } catch (ErrnoException | SocketException e) {
            if (isSocketValid()) {
                Log.e(TAG, "sendto error: " + e);
            }
        }
    }

    private final class UnicastResponder extends Thread {
        private final byte[] mSolication;
        private final InetSocketAddress solicitor;

        private UnicastResponder() {
            this.solicitor = new InetSocketAddress();
            this.mSolication = new byte[NetworkConstants.IPV6_MIN_MTU];
        }

        @Override
        public void run() {
            while (RouterAdvertisementDaemon.this.isSocketValid()) {
                try {
                    if (Os.recvfrom(RouterAdvertisementDaemon.this.mSocket, this.mSolication, 0, this.mSolication.length, 0, this.solicitor) >= 1 && this.mSolication[0] == RouterAdvertisementDaemon.ICMPV6_ND_ROUTER_SOLICIT) {
                        RouterAdvertisementDaemon.this.maybeSendRA(this.solicitor);
                    }
                } catch (ErrnoException | SocketException e) {
                    if (RouterAdvertisementDaemon.this.isSocketValid()) {
                        Log.e(RouterAdvertisementDaemon.TAG, "recvfrom error: " + e);
                    }
                }
            }
        }
    }

    private final class MulticastTransmitter extends Thread {
        private final Random mRandom;
        private final AtomicInteger mUrgentAnnouncements;

        private MulticastTransmitter() {
            this.mRandom = new Random();
            this.mUrgentAnnouncements = new AtomicInteger(0);
        }

        @Override
        public void run() {
            while (RouterAdvertisementDaemon.this.isSocketValid()) {
                try {
                    Thread.sleep(getNextMulticastTransmitDelayMs());
                } catch (InterruptedException e) {
                }
                RouterAdvertisementDaemon.this.maybeSendRA(RouterAdvertisementDaemon.this.mAllNodes);
                synchronized (RouterAdvertisementDaemon.this.mLock) {
                    if (RouterAdvertisementDaemon.this.mDeprecatedInfoTracker.decrementCounters()) {
                        RouterAdvertisementDaemon.this.assembleRaLocked();
                    }
                }
            }
        }

        public void hup() {
            this.mUrgentAnnouncements.set(4);
            interrupt();
        }

        private int getNextMulticastTransmitDelaySec() {
            synchronized (RouterAdvertisementDaemon.this.mLock) {
                if (RouterAdvertisementDaemon.this.mRaLength < 16) {
                    return RouterAdvertisementDaemon.DAY_IN_SECONDS;
                }
                boolean z = !RouterAdvertisementDaemon.this.mDeprecatedInfoTracker.isEmpty();
                if (this.mUrgentAnnouncements.getAndDecrement() > 0 || z) {
                    return 3;
                }
                return RouterAdvertisementDaemon.MIN_RTR_ADV_INTERVAL_SEC + this.mRandom.nextInt(RouterAdvertisementDaemon.MIN_RTR_ADV_INTERVAL_SEC);
            }
        }

        private long getNextMulticastTransmitDelayMs() {
            return 1000 * ((long) getNextMulticastTransmitDelaySec());
        }
    }
}
