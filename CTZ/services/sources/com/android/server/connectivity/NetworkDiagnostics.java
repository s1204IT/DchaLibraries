package com.android.server.connectivity;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.TrafficStats;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructTimeval;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.usb.descriptors.UsbDescriptor;
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
import java.util.Iterator;
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

    public enum DnsResponseCode {
        NOERROR,
        FORMERR,
        SERVFAIL,
        NXDOMAIN,
        NOTIMP,
        REFUSED
    }

    private static final long now() {
        return SystemClock.elapsedRealtime();
    }

    public class Measurement {
        private static final String FAILED = "FAILED";
        private static final String SUCCEEDED = "SUCCEEDED";
        long finishTime;
        long startTime;
        private boolean succeeded;
        Thread thread;
        String description = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        String result = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;

        public Measurement() {
        }

        public boolean checkSucceeded() {
            return this.succeeded;
        }

        void recordSuccess(String str) {
            maybeFixupTimes();
            this.succeeded = true;
            this.result = "SUCCEEDED: " + str;
            if (NetworkDiagnostics.this.mCountDownLatch != null) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
            }
        }

        void recordFailure(String str) {
            maybeFixupTimes();
            this.succeeded = false;
            this.result = "FAILED: " + str;
            if (NetworkDiagnostics.this.mCountDownLatch != null) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
            }
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

    public NetworkDiagnostics(Network network, LinkProperties linkProperties, long j) {
        this.mNetwork = network;
        this.mLinkProperties = linkProperties;
        this.mInterfaceIndex = getInterfaceIndex(this.mLinkProperties.getInterfaceName());
        this.mTimeoutMs = j;
        this.mDeadlineTime = this.mStartTime + this.mTimeoutMs;
        if (this.mLinkProperties.isReachable(TEST_DNS4)) {
            this.mLinkProperties.addDnsServer(TEST_DNS4);
        }
        if (this.mLinkProperties.hasGlobalIPv6Address() || this.mLinkProperties.hasIPv6DefaultRoute()) {
            this.mLinkProperties.addDnsServer(TEST_DNS6);
        }
        for (RouteInfo routeInfo : this.mLinkProperties.getRoutes()) {
            if (routeInfo.hasGateway()) {
                InetAddress gateway = routeInfo.getGateway();
                prepareIcmpMeasurement(gateway);
                if (routeInfo.isIPv6Default()) {
                    prepareExplicitSourceIcmpMeasurements(gateway);
                }
            }
        }
        for (InetAddress inetAddress : this.mLinkProperties.getDnsServers()) {
            prepareIcmpMeasurement(inetAddress);
            prepareDnsMeasurement(inetAddress);
        }
        this.mCountDownLatch = new CountDownLatch(totalMeasurementCount());
        startMeasurements();
        this.mDescription = "ifaces{" + TextUtils.join(",", this.mLinkProperties.getAllInterfaceNames()) + "} index{" + this.mInterfaceIndex + "} network{" + this.mNetwork + "} nethandle{" + this.mNetwork.getNetworkHandle() + "}";
    }

    private static Integer getInterfaceIndex(String str) {
        try {
            return Integer.valueOf(NetworkInterface.getByName(str).getIndex());
        } catch (NullPointerException | SocketException e) {
            return null;
        }
    }

    private void prepareIcmpMeasurement(InetAddress inetAddress) {
        if (!this.mIcmpChecks.containsKey(inetAddress)) {
            Measurement measurement = new Measurement();
            measurement.thread = new Thread(new IcmpCheck(this, inetAddress, measurement));
            this.mIcmpChecks.put(inetAddress, measurement);
        }
    }

    private void prepareExplicitSourceIcmpMeasurements(InetAddress inetAddress) {
        for (LinkAddress linkAddress : this.mLinkProperties.getLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if ((address instanceof Inet6Address) && linkAddress.isGlobalPreferred()) {
                Pair<InetAddress, InetAddress> pair = new Pair<>(address, inetAddress);
                if (!this.mExplicitSourceIcmpChecks.containsKey(pair)) {
                    Measurement measurement = new Measurement();
                    measurement.thread = new Thread(new IcmpCheck(address, inetAddress, measurement));
                    this.mExplicitSourceIcmpChecks.put(pair, measurement);
                }
            }
        }
    }

    private void prepareDnsMeasurement(InetAddress inetAddress) {
        if (!this.mDnsUdpChecks.containsKey(inetAddress)) {
            Measurement measurement = new Measurement();
            measurement.thread = new Thread(new DnsUdpCheck(inetAddress, measurement));
            this.mDnsUdpChecks.put(inetAddress, measurement);
        }
    }

    private int totalMeasurementCount() {
        return this.mIcmpChecks.size() + this.mExplicitSourceIcmpChecks.size() + this.mDnsUdpChecks.size();
    }

    private void startMeasurements() {
        Iterator<Measurement> it = this.mIcmpChecks.values().iterator();
        while (it.hasNext()) {
            it.next().thread.start();
        }
        Iterator<Measurement> it2 = this.mExplicitSourceIcmpChecks.values().iterator();
        while (it2.hasNext()) {
            it2.next().thread.start();
        }
        Iterator<Measurement> it3 = this.mDnsUdpChecks.values().iterator();
        while (it3.hasNext()) {
            it3.next().thread.start();
        }
    }

    public void waitForMeasurements() {
        try {
            this.mCountDownLatch.await(this.mDeadlineTime - now(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    public List<Measurement> getMeasurements() {
        ArrayList arrayList = new ArrayList(totalMeasurementCount());
        for (Map.Entry<InetAddress, Measurement> entry : this.mIcmpChecks.entrySet()) {
            if (entry.getKey() instanceof Inet4Address) {
                arrayList.add(entry.getValue());
            }
        }
        for (Map.Entry<Pair<InetAddress, InetAddress>, Measurement> entry2 : this.mExplicitSourceIcmpChecks.entrySet()) {
            if (entry2.getKey().first instanceof Inet4Address) {
                arrayList.add(entry2.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry3 : this.mDnsUdpChecks.entrySet()) {
            if (entry3.getKey() instanceof Inet4Address) {
                arrayList.add(entry3.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry4 : this.mIcmpChecks.entrySet()) {
            if (entry4.getKey() instanceof Inet6Address) {
                arrayList.add(entry4.getValue());
            }
        }
        for (Map.Entry<Pair<InetAddress, InetAddress>, Measurement> entry5 : this.mExplicitSourceIcmpChecks.entrySet()) {
            if (entry5.getKey().first instanceof Inet6Address) {
                arrayList.add(entry5.getValue());
            }
        }
        for (Map.Entry<InetAddress, Measurement> entry6 : this.mDnsUdpChecks.entrySet()) {
            if (entry6.getKey() instanceof Inet6Address) {
                arrayList.add(entry6.getValue());
            }
        }
        return arrayList;
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("NetworkDiagnostics:" + this.mDescription);
        long count = this.mCountDownLatch.getCount();
        if (count > 0) {
            indentingPrintWriter.println("WARNING: countdown wait incomplete: " + count + " unfinished measurements");
        }
        indentingPrintWriter.increaseIndent();
        for (Measurement measurement : getMeasurements()) {
            indentingPrintWriter.println((measurement.checkSucceeded() ? "." : "F") + "  " + measurement.toString());
        }
        indentingPrintWriter.decreaseIndent();
    }

    private class SimpleSocketCheck implements Closeable {
        protected final int mAddressFamily;
        protected FileDescriptor mFileDescriptor;
        protected final Measurement mMeasurement;
        protected SocketAddress mSocketAddress;
        protected final InetAddress mSource;
        protected final InetAddress mTarget;

        protected SimpleSocketCheck(InetAddress inetAddress, InetAddress inetAddress2, Measurement measurement) {
            InetAddress byAddress;
            this.mMeasurement = measurement;
            if (inetAddress2 instanceof Inet6Address) {
                if (inetAddress2.isLinkLocalAddress() && NetworkDiagnostics.this.mInterfaceIndex != null) {
                    try {
                        byAddress = Inet6Address.getByAddress((String) null, inetAddress2.getAddress(), NetworkDiagnostics.this.mInterfaceIndex.intValue());
                    } catch (UnknownHostException e) {
                        this.mMeasurement.recordFailure(e.toString());
                        byAddress = null;
                    }
                    this.mTarget = byAddress == null ? inetAddress2 : byAddress;
                    this.mAddressFamily = OsConstants.AF_INET6;
                } else {
                    byAddress = null;
                    this.mTarget = byAddress == null ? inetAddress2 : byAddress;
                    this.mAddressFamily = OsConstants.AF_INET6;
                }
            } else {
                this.mTarget = inetAddress2;
                this.mAddressFamily = OsConstants.AF_INET;
            }
            this.mSource = inetAddress;
        }

        protected SimpleSocketCheck(NetworkDiagnostics networkDiagnostics, InetAddress inetAddress, Measurement measurement) {
            this(null, inetAddress, measurement);
        }

        protected void setupSocket(int i, int i2, long j, long j2, int i3) throws IOException, ErrnoException {
            int andSetThreadStatsTag = TrafficStats.getAndSetThreadStatsTag(-190);
            try {
                this.mFileDescriptor = Os.socket(this.mAddressFamily, i, i2);
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                Os.setsockoptTimeval(this.mFileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_SNDTIMEO, StructTimeval.fromMillis(j));
                Os.setsockoptTimeval(this.mFileDescriptor, OsConstants.SOL_SOCKET, OsConstants.SO_RCVTIMEO, StructTimeval.fromMillis(j2));
                NetworkDiagnostics.this.mNetwork.bindSocket(this.mFileDescriptor);
                if (this.mSource != null) {
                    Os.bind(this.mFileDescriptor, this.mSource, 0);
                }
                Os.connect(this.mFileDescriptor, this.mTarget, i3);
                this.mSocketAddress = Os.getsockname(this.mFileDescriptor);
            } catch (Throwable th) {
                TrafficStats.setThreadStatsTag(andSetThreadStatsTag);
                throw th;
            }
        }

        protected String getSocketAddressString() {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) this.mSocketAddress;
            InetAddress address = inetSocketAddress.getAddress();
            return String.format(address instanceof Inet6Address ? "[%s]:%d" : "%s:%d", address.getHostAddress(), Integer.valueOf(inetSocketAddress.getPort()));
        }

        @Override
        public void close() {
            IoUtils.closeQuietly(this.mFileDescriptor);
        }
    }

    private class IcmpCheck extends SimpleSocketCheck implements Runnable {
        private static final int PACKET_BUFSIZE = 512;
        private static final int TIMEOUT_RECV = 300;
        private static final int TIMEOUT_SEND = 100;
        private final int mIcmpType;
        private final int mProtocol;

        public IcmpCheck(InetAddress inetAddress, InetAddress inetAddress2, Measurement measurement) {
            super(inetAddress, inetAddress2, measurement);
            if (this.mAddressFamily == OsConstants.AF_INET6) {
                this.mProtocol = OsConstants.IPPROTO_ICMPV6;
                this.mIcmpType = 128;
                this.mMeasurement.description = "ICMPv6";
            } else {
                this.mProtocol = OsConstants.IPPROTO_ICMP;
                this.mIcmpType = 8;
                this.mMeasurement.description = "ICMPv4";
            }
            StringBuilder sb = new StringBuilder();
            Measurement measurement2 = this.mMeasurement;
            sb.append(measurement2.description);
            sb.append(" dst{");
            sb.append(this.mTarget.getHostAddress());
            sb.append("}");
            measurement2.description = sb.toString();
        }

        public IcmpCheck(NetworkDiagnostics networkDiagnostics, InetAddress inetAddress, Measurement measurement) {
            this(null, inetAddress, measurement);
        }

        @Override
        public void run() {
            if (this.mMeasurement.finishTime > 0) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
                return;
            }
            try {
                setupSocket(OsConstants.SOCK_DGRAM, this.mProtocol, 100L, 300L, 0);
                StringBuilder sb = new StringBuilder();
                Measurement measurement = this.mMeasurement;
                sb.append(measurement.description);
                sb.append(" src{");
                sb.append(getSocketAddressString());
                sb.append("}");
                measurement.description = sb.toString();
                byte[] bArr = new byte[8];
                bArr[0] = (byte) this.mIcmpType;
                bArr[1] = 0;
                bArr[2] = 0;
                bArr[3] = 0;
                bArr[4] = 0;
                bArr[5] = 0;
                bArr[6] = 0;
                bArr[7] = 0;
                this.mMeasurement.startTime = NetworkDiagnostics.now();
                int i = 0;
                while (NetworkDiagnostics.now() < NetworkDiagnostics.this.mDeadlineTime - 400) {
                    i++;
                    bArr[bArr.length - 1] = (byte) i;
                    try {
                        Os.write(this.mFileDescriptor, bArr, 0, bArr.length);
                        try {
                            Os.read(this.mFileDescriptor, ByteBuffer.allocate(512));
                            this.mMeasurement.recordSuccess("1/" + i);
                            break;
                        } catch (ErrnoException | InterruptedIOException e) {
                        }
                    } catch (ErrnoException | InterruptedIOException e2) {
                        this.mMeasurement.recordFailure(e2.toString());
                    }
                }
                if (this.mMeasurement.finishTime == 0) {
                    this.mMeasurement.recordFailure("0/" + i);
                }
                close();
            } catch (ErrnoException | IOException e3) {
                this.mMeasurement.recordFailure(e3.toString());
            }
        }
    }

    private class DnsUdpCheck extends SimpleSocketCheck implements Runnable {
        private static final int PACKET_BUFSIZE = 512;
        private static final int RR_TYPE_A = 1;
        private static final int RR_TYPE_AAAA = 28;
        private static final int TIMEOUT_RECV = 500;
        private static final int TIMEOUT_SEND = 100;
        private final int mQueryType;
        private final Random mRandom;

        private String responseCodeStr(int i) {
            try {
                return DnsResponseCode.values()[i].toString();
            } catch (IndexOutOfBoundsException e) {
                return String.valueOf(i);
            }
        }

        public DnsUdpCheck(InetAddress inetAddress, Measurement measurement) {
            super(NetworkDiagnostics.this, inetAddress, measurement);
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
            String str;
            if (this.mMeasurement.finishTime > 0) {
                NetworkDiagnostics.this.mCountDownLatch.countDown();
                return;
            }
            try {
                setupSocket(OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_UDP, 100L, 500L, 53);
                StringBuilder sb = new StringBuilder();
                Measurement measurement = this.mMeasurement;
                sb.append(measurement.description);
                sb.append(" src{");
                sb.append(getSocketAddressString());
                sb.append("}");
                measurement.description = sb.toString();
                String strValueOf = String.valueOf(this.mRandom.nextInt(900000) + 100000);
                StringBuilder sb2 = new StringBuilder();
                Measurement measurement2 = this.mMeasurement;
                sb2.append(measurement2.description);
                sb2.append(" qtype{");
                sb2.append(this.mQueryType);
                sb2.append("} qname{");
                sb2.append(strValueOf);
                sb2.append("-android-ds.metric.gstatic.com}");
                measurement2.description = sb2.toString();
                byte[] dnsQueryPacket = getDnsQueryPacket(strValueOf);
                this.mMeasurement.startTime = NetworkDiagnostics.now();
                int i = 0;
                while (NetworkDiagnostics.now() < NetworkDiagnostics.this.mDeadlineTime - 1000) {
                    i++;
                    try {
                        Os.write(this.mFileDescriptor, dnsQueryPacket, 0, dnsQueryPacket.length);
                        try {
                            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(512);
                            Os.read(this.mFileDescriptor, byteBufferAllocate);
                            if (byteBufferAllocate.limit() > 3) {
                                str = " " + responseCodeStr(byteBufferAllocate.get(3) & UsbDescriptor.DESCRIPTORTYPE_BOS);
                            } else {
                                str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                            }
                            this.mMeasurement.recordSuccess("1/" + i + str);
                            break;
                        } catch (ErrnoException | InterruptedIOException e) {
                        }
                    } catch (ErrnoException | InterruptedIOException e2) {
                        this.mMeasurement.recordFailure(e2.toString());
                    }
                }
                if (this.mMeasurement.finishTime == 0) {
                    this.mMeasurement.recordFailure("0/" + i);
                }
                close();
            } catch (ErrnoException | IOException e3) {
                this.mMeasurement.recordFailure(e3.toString());
            }
        }

        private byte[] getDnsQueryPacket(String str) {
            byte[] bytes = str.getBytes(StandardCharsets.US_ASCII);
            return new byte[]{(byte) this.mRandom.nextInt(), (byte) this.mRandom.nextInt(), 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 17, bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], 45, 97, 110, 100, 114, 111, 105, 100, 45, 100, 115, 6, 109, 101, 116, 114, 105, 99, 7, 103, 115, 116, 97, 116, 105, 99, 3, 99, 111, 109, 0, 0, (byte) this.mQueryType, 0, 1};
        }
    }
}
