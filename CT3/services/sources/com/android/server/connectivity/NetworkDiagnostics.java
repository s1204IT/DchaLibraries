package com.android.server.connectivity;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.util.IndentingPrintWriter;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class NetworkDiagnostics {
    private static final String TAG = "NetworkDiagnostics";
    private static final InetAddress TEST_DNS4 = NetworkUtils.numericToInetAddress("8.8.8.8");
    private static final InetAddress TEST_DNS6 = NetworkUtils.numericToInetAddress("2001:4860:4860::8888");
    private final CountDownLatch mCountDownLatch;
    private final long mDeadlineTime;
    private final String mDescription;
    private final Integer mInterfaceIndex;
    private final LinkProperties mLinkProperties;
    private final Network mNetwork;
    private final long mTimeoutMs;
    private final Map<InetAddress, Measurement> mIcmpChecks = new HashMap();
    private final Map<Pair<InetAddress, InetAddress>, Measurement> mExplicitSourceIcmpChecks = new HashMap();
    private final Map<InetAddress, Measurement> mDnsUdpChecks = new HashMap();
    private final long mStartTime = now();

    private static final long now() {
        return SystemClock.elapsedRealtime();
    }

    public enum DnsResponseCode {
        NOERROR,
        FORMERR,
        SERVFAIL,
        NXDOMAIN,
        NOTIMP,
        REFUSED;

        public static DnsResponseCode[] valuesCustom() {
            return values();
        }
    }

    public class Measurement {
        private static final String FAILED = "FAILED";
        private static final String SUCCEEDED = "SUCCEEDED";
        long finishTime;
        long startTime;
        private boolean succeeded;
        Thread thread;
        String description = "";
        String result = "";

        public Measurement() {
        }

        public boolean checkSucceeded() {
            return this.succeeded;
        }

        void recordSuccess(String msg) {
            maybeFixupTimes();
            this.succeeded = true;
            this.result = "SUCCEEDED: " + msg;
            if (NetworkDiagnostics.this.mCountDownLatch == null) {
                return;
            }
            NetworkDiagnostics.this.mCountDownLatch.countDown();
        }

        void recordFailure(String msg) {
            maybeFixupTimes();
            this.succeeded = false;
            this.result = "FAILED: " + msg;
            if (NetworkDiagnostics.this.mCountDownLatch == null) {
                return;
            }
            NetworkDiagnostics.this.mCountDownLatch.countDown();
        }

        private void maybeFixupTimes() {
            if (this.finishTime == 0) {
                this.finishTime = NetworkDiagnostics.now();
            }
            if (this.startTime == 0) {
                this.startTime = this.finishTime;
            }
        }

        public String toString() {
            return this.description + ": " + this.result + " (" + (this.finishTime - this.startTime) + "ms)";
        }
    }

    public NetworkDiagnostics(Network network, LinkProperties lp, long timeoutMs) {
        this.mNetwork = network;
        this.mLinkProperties = lp;
        this.mInterfaceIndex = getInterfaceIndex(this.mLinkProperties.getInterfaceName());
        this.mTimeoutMs = timeoutMs;
        this.mDeadlineTime = this.mStartTime + this.mTimeoutMs;
        if (this.mLinkProperties.isReachable(TEST_DNS4)) {
            this.mLinkProperties.addDnsServer(TEST_DNS4);
        }
        if (this.mLinkProperties.hasGlobalIPv6Address() || this.mLinkProperties.hasIPv6DefaultRoute()) {
            this.mLinkProperties.addDnsServer(TEST_DNS6);
        }
        for (RouteInfo route : this.mLinkProperties.getRoutes()) {
            if (route.hasGateway()) {
                InetAddress gateway = route.getGateway();
                prepareIcmpMeasurement(gateway);
                if (route.isIPv6Default()) {
                    prepareExplicitSourceIcmpMeasurements(gateway);
                }
            }
        }
        for (InetAddress nameserver : this.mLinkProperties.getDnsServers()) {
            prepareIcmpMeasurement(nameserver);
            prepareDnsMeasurement(nameserver);
        }
        this.mCountDownLatch = new CountDownLatch(totalMeasurementCount());
        startMeasurements();
        this.mDescription = "ifaces{" + TextUtils.join(",", this.mLinkProperties.getAllInterfaceNames()) + "} index{" + this.mInterfaceIndex + "} network{" + this.mNetwork + "} nethandle{" + this.mNetwork.getNetworkHandle() + "}";
    }

    private static Integer getInterfaceIndex(String ifname) {
        try {
            NetworkInterface ni = NetworkInterface.getByName(ifname);
            return Integer.valueOf(ni.getIndex());
        } catch (NullPointerException | SocketException e) {
            return null;
        }
    }

    private void prepareIcmpMeasurement(InetAddress target) {
        if (this.mIcmpChecks.containsKey(target)) {
            return;
        }
        Measurement measurement = new Measurement();
        measurement.thread = new Thread(new IcmpCheck(this, target, measurement));
        this.mIcmpChecks.put(target, measurement);
    }

    private void prepareExplicitSourceIcmpMeasurements(InetAddress target) {
        for (LinkAddress l : this.mLinkProperties.getLinkAddresses()) {
            InetAddress source = l.getAddress();
            if ((source instanceof Inet6Address) && l.isGlobalPreferred()) {
                Pair<InetAddress, InetAddress> srcTarget = new Pair<>(source, target);
                if (!this.mExplicitSourceIcmpChecks.containsKey(srcTarget)) {
                    Measurement measurement = new Measurement();
                    measurement.thread = new Thread(new IcmpCheck(source, target, measurement));
                    this.mExplicitSourceIcmpChecks.put(srcTarget, measurement);
                }
            }
        }
    }

    private void prepareDnsMeasurement(InetAddress target) {
        if (this.mDnsUdpChecks.containsKey(target)) {
            return;
        }
        Measurement measurement = new Measurement();
        measurement.thread = new Thread(new DnsUdpCheck(target, measurement));
        this.mDnsUdpChecks.put(target, measurement);
    }

    private int totalMeasurementCount() {
        return this.mIcmpChecks.size() + this.mExplicitSourceIcmpChecks.size() + this.mDnsUdpChecks.size();
    }

    private void startMeasurements() {
        for (Measurement measurement : this.mIcmpChecks.values()) {
            measurement.thread.start();
        }
        for (Measurement measurement2 : this.mExplicitSourceIcmpChecks.values()) {
            measurement2.thread.start();
        }
        for (Measurement measurement3 : this.mDnsUdpChecks.values()) {
            measurement3.thread.start();
        }
    }

    public void waitForMeasurements() {
        try {
            this.mCountDownLatch.await(this.mDeadlineTime - now(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    public List<Measurement> getMeasurements() {
        ArrayList<Measurement> measurements = new ArrayList<>(totalMeasurementCount());
        for (Map.Entry<InetAddress, Measurement> entry : this.mIcmpChecks.entrySet()) {
            if (entry.getKey() instanceof Inet4Address) {
                measurements.add(entry.getValue());
            }
        }
        for (Map.Entry<Pair<InetAddress, InetAddress>, Measurement> entry2 : this.mExplicitSourceIcmpChecks.entrySet()) {
            if (entry2.getKey().first instanceof Inet4Address) {
                measurements.add(entry2.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry3 : this.mDnsUdpChecks.entrySet()) {
            if (entry3.getKey() instanceof Inet4Address) {
                measurements.add(entry3.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry4 : this.mIcmpChecks.entrySet()) {
            if (entry4.getKey() instanceof Inet6Address) {
                measurements.add(entry4.getValue());
            }
        }
        for (Map.Entry<Pair<InetAddress, InetAddress>, Measurement> entry5 : this.mExplicitSourceIcmpChecks.entrySet()) {
            if (entry5.getKey().first instanceof Inet6Address) {
                measurements.add(entry5.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry6 : this.mDnsUdpChecks.entrySet()) {
            if (entry6.getKey() instanceof Inet6Address) {
                measurements.add(entry6.getValue());
            }
        }
        return measurements;
    }

    public void dump(IndentingPrintWriter pw) {
        pw.println("NetworkDiagnostics:" + this.mDescription);
        long unfinished = this.mCountDownLatch.getCount();
        if (unfinished > 0) {
            pw.println("WARNING: countdown wait incomplete: " + unfinished + " unfinished measurements");
        }
        pw.increaseIndent();
        for (Measurement m : getMeasurements()) {
            String prefix = m.checkSucceeded() ? "." : "F";
            pw.println(prefix + "  " + m.toString());
        }
        pw.decreaseIndent();
    }

    private class SimpleSocketCheck implements Closeable {
        protected final int mAddressFamily;
        protected FileDescriptor mFileDescriptor;
        protected final Measurement mMeasurement;
        protected SocketAddress mSocketAddress;
        protected final InetAddress mSource;
        protected final InetAddress mTarget;

        protected SimpleSocketCheck(InetAddress source, InetAddress target, Measurement measurement) {
            this.mMeasurement = measurement;
            if (target instanceof Inet6Address) {
                InetAddress byAddress = null;
                if (target.isLinkLocalAddress() && NetworkDiagnostics.this.mInterfaceIndex != null) {
                    try {
                        byAddress = Inet6Address.getByAddress((String) null, target.getAddress(), NetworkDiagnostics.this.mInterfaceIndex.intValue());
                    } catch (UnknownHostException e) {
                        this.mMeasurement.recordFailure(e.toString());
                    }
                }
                this.mTarget = byAddress == null ? target : byAddress;
                this.mAddressFamily = OsConstants.AF_INET6;
            } else {
                this.mTarget = target;
                this.mAddressFamily = OsConstants.AF_INET;
            }
            this.mSource = source;
        }

        protected SimpleSocketCheck(NetworkDiagnostics this$0, InetAddress target, Measurement measurement) {
            this(null, target, measurement);
        }

        protected void setupSocket(int sockType, int protocol, long writeTimeout, long readTimeout, int dstPort) throws IOException, ErrnoException {
            this.mFileDescriptor = Os.socket(this.mAddressFamily, sockType, protocol);
            Os.setsockoptTimeval(this.mFileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(writeTimeout));
            Os.setsockoptTimeval(this.mFileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(readTimeout));
            NetworkDiagnostics.this.mNetwork.bindSocket(this.mFileDescriptor);
            if (this.mSource != null) {
                Os.bind(this.mFileDescriptor, this.mSource, 0);
            }
            Os.connect(this.mFileDescriptor, this.mTarget, dstPort);
            this.mSocketAddress = Os.getsockname(this.mFileDescriptor);
        }

        protected String getSocketAddressString() {
            InetSocketAddress inetSockAddr = (InetSocketAddress) this.mSocketAddress;
            InetAddress localAddr = inetSockAddr.getAddress();
            return String.format(localAddr instanceof Inet6Address ? "[%s]:%d" : "%s:%d", localAddr.getHostAddress(), Integer.valueOf(inetSockAddr.getPort()));
        }

        @Override
        public void close() {
            IoUtils.closeQuietly(this.mFileDescriptor);
        }
    }

    private class IcmpCheck extends SimpleSocketCheck implements Runnable {
        private static final int ICMPV4_ECHO_REQUEST = 8;
        private static final int ICMPV6_ECHO_REQUEST = 128;
        private static final int PACKET_BUFSIZE = 512;
        private static final int TIMEOUT_RECV = 300;
        private static final int TIMEOUT_SEND = 100;
        private final int mIcmpType;
        private final int mProtocol;

        public IcmpCheck(InetAddress source, InetAddress target, Measurement measurement) {
            super(source, target, measurement);
            if (this.mAddressFamily == OsConstants.AF_INET6) {
                this.mProtocol = OsConstants.IPPROTO_ICMPV6;
                this.mIcmpType = 128;
                this.mMeasurement.description = "ICMPv6";
            } else {
                this.mProtocol = OsConstants.IPPROTO_ICMP;
                this.mIcmpType = 8;
                this.mMeasurement.description = "ICMPv4";
            }
            this.mMeasurement.description += " dst{" + this.mTarget.getHostAddress() + "}";
        }

        public IcmpCheck(NetworkDiagnostics this$0, InetAddress target, Measurement measurement) {
            this(null, target, measurement);
        }

        @Override
        public void run() {
            if (this.mMeasurement.finishTime > 0) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
                return;
            }
            try {
                setupSocket(OsConstants.SOCK_DGRAM, this.mProtocol, 100L, 300L, 0);
                this.mMeasurement.description += " src{" + getSocketAddressString() + "}";
                byte[] icmpPacket = new byte[8];
                icmpPacket[0] = (byte) this.mIcmpType;
                icmpPacket[1] = 0;
                icmpPacket[2] = 0;
                icmpPacket[3] = 0;
                icmpPacket[4] = 0;
                icmpPacket[5] = 0;
                icmpPacket[6] = 0;
                icmpPacket[7] = 0;
                int count = 0;
                this.mMeasurement.startTime = NetworkDiagnostics.now();
                while (NetworkDiagnostics.now() < NetworkDiagnostics.this.mDeadlineTime - 400) {
                    count++;
                    icmpPacket[icmpPacket.length - 1] = (byte) count;
                    try {
                        Os.write(this.mFileDescriptor, icmpPacket, 0, icmpPacket.length);
                        try {
                            ByteBuffer reply = ByteBuffer.allocate(512);
                            Os.read(this.mFileDescriptor, reply);
                            this.mMeasurement.recordSuccess("1/" + count);
                            break;
                        } catch (ErrnoException | InterruptedIOException e) {
                        }
                    } catch (ErrnoException | InterruptedIOException e2) {
                        this.mMeasurement.recordFailure(e2.toString());
                    }
                }
                if (this.mMeasurement.finishTime == 0) {
                    this.mMeasurement.recordFailure("0/" + count);
                }
                close();
            } catch (ErrnoException | IOException e3) {
                this.mMeasurement.recordFailure(e3.toString());
            }
        }
    }

    private class DnsUdpCheck extends SimpleSocketCheck implements Runnable {
        private static final int DNS_SERVER_PORT = 53;
        private static final int PACKET_BUFSIZE = 512;
        private static final int RR_TYPE_A = 1;
        private static final int RR_TYPE_AAAA = 28;
        private static final int TIMEOUT_RECV = 500;
        private static final int TIMEOUT_SEND = 100;
        private final int mQueryType;
        private final Random mRandom;

        private String responseCodeStr(int rcode) {
            try {
                return DnsResponseCode.valuesCustom()[rcode].toString();
            } catch (IndexOutOfBoundsException e) {
                return String.valueOf(rcode);
            }
        }

        public DnsUdpCheck(InetAddress target, Measurement measurement) {
            super(NetworkDiagnostics.this, target, measurement);
            this.mRandom = new Random();
            if (this.mAddressFamily == OsConstants.AF_INET6) {
                this.mQueryType = 28;
            } else {
                this.mQueryType = 1;
            }
            this.mMeasurement.description = "DNS UDP dst{" + this.mTarget.getHostAddress() + "}";
        }

        @Override
        public void run() {
            String rcodeStr;
            if (this.mMeasurement.finishTime > 0) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
                return;
            }
            try {
                setupSocket(OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP, 100L, 500L, 53);
                this.mMeasurement.description += " src{" + getSocketAddressString() + "}";
                String sixRandomDigits = String.valueOf(this.mRandom.nextInt(900000) + 100000);
                this.mMeasurement.description += " qtype{" + this.mQueryType + "} qname{" + sixRandomDigits + "-android-ds.metric.gstatic.com}";
                byte[] dnsPacket = getDnsQueryPacket(sixRandomDigits);
                int count = 0;
                this.mMeasurement.startTime = NetworkDiagnostics.now();
                while (NetworkDiagnostics.now() < NetworkDiagnostics.this.mDeadlineTime - 1000) {
                    count++;
                    try {
                        Os.write(this.mFileDescriptor, dnsPacket, 0, dnsPacket.length);
                        try {
                            ByteBuffer reply = ByteBuffer.allocate(512);
                            Os.read(this.mFileDescriptor, reply);
                            if (reply.limit() > 3) {
                                rcodeStr = " " + responseCodeStr(reply.get(3) & 15);
                            } else {
                                rcodeStr = "";
                            }
                            this.mMeasurement.recordSuccess("1/" + count + rcodeStr);
                            break;
                        } catch (ErrnoException | InterruptedIOException e) {
                        }
                    } catch (ErrnoException | InterruptedIOException e2) {
                        this.mMeasurement.recordFailure(e2.toString());
                    }
                }
                if (this.mMeasurement.finishTime == 0) {
                    this.mMeasurement.recordFailure("0/" + count);
                }
                close();
            } catch (ErrnoException | IOException e3) {
                this.mMeasurement.recordFailure(e3.toString());
            }
        }

        private byte[] getDnsQueryPacket(String sixRandomDigits) {
            byte[] rnd = sixRandomDigits.getBytes(StandardCharsets.US_ASCII);
            return new byte[]{(byte) this.mRandom.nextInt(), (byte) this.mRandom.nextInt(), 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 17, rnd[0], rnd[1], rnd[2], rnd[3], rnd[4], rnd[5], 45, 97, 110, 100, 114, 111, 105, 100, 45, 100, 115, 6, 109, 101, 116, 114, 105, 99, 7, 103, 115, 116, 97, 116, 105, 99, 3, 99, 111, 109, 0, 0, (byte) this.mQueryType, 0, 1};
        }
    }
}
