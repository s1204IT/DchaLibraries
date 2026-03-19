package android.net.apf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.net.apf.ApfGenerator;
import android.net.ip.IpClient;
import android.net.metrics.ApfProgramEvent;
import android.net.metrics.ApfStats;
import android.net.metrics.IpConnectivityLog;
import android.net.metrics.RaEvent;
import android.net.util.InterfaceParams;
import android.net.util.NetworkConstants;
import android.os.PowerManager;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.BitUtils;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.controllers.JobStatus;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.ToIntFunction;
import libcore.io.IoBridge;

public class ApfFilter {
    private static final int APF_MAX_ETH_TYPE_BLACK_LIST_LEN = 20;
    private static final int APF_PROGRAM_EVENT_LIFETIME_THRESHOLD = 2;
    private static final int ARP_HEADER_OFFSET = 14;
    private static final int ARP_OPCODE_OFFSET = 20;
    private static final short ARP_OPCODE_REPLY = 2;
    private static final short ARP_OPCODE_REQUEST = 1;
    private static final int ARP_TARGET_IP_ADDRESS_OFFSET = 38;
    private static final boolean DBG = true;
    private static final int DHCP_CLIENT_MAC_OFFSET = 50;
    private static final int DHCP_CLIENT_PORT = 68;
    private static final int ETH_DEST_ADDR_OFFSET = 0;
    private static final int ETH_ETHERTYPE_OFFSET = 12;
    private static final int ETH_HEADER_LEN = 14;
    private static final int ETH_TYPE_MAX = 65535;
    private static final int ETH_TYPE_MIN = 1536;
    private static final int FRACTION_OF_LIFETIME_TO_FILTER = 6;
    private static final int ICMP6_TYPE_OFFSET = 54;
    private static final int IPV4_ANY_HOST_ADDRESS = 0;
    private static final int IPV4_BROADCAST_ADDRESS = -1;
    private static final int IPV4_DEST_ADDR_OFFSET = 30;
    private static final int IPV4_FRAGMENT_OFFSET_MASK = 8191;
    private static final int IPV4_FRAGMENT_OFFSET_OFFSET = 20;
    private static final int IPV4_PROTOCOL_OFFSET = 23;
    private static final int IPV6_DEST_ADDR_OFFSET = 38;
    private static final int IPV6_FLOW_LABEL_LEN = 3;
    private static final int IPV6_FLOW_LABEL_OFFSET = 15;
    private static final int IPV6_HEADER_LEN = 40;
    private static final int IPV6_NEXT_HEADER_OFFSET = 20;
    private static final int IPV6_SRC_ADDR_OFFSET = 22;
    private static final long MAX_PROGRAM_LIFETIME_WORTH_REFRESHING = 30;
    private static final int MAX_RAS = 10;
    private static final String TAG = "ApfFilter";
    private static final int UDP_DESTINATION_PORT_OFFSET = 16;
    private static final int UDP_HEADER_LEN = 8;
    private static final boolean VDBG = false;
    private final ApfCapabilities mApfCapabilities;
    private final Context mContext;
    private final String mCountAndDropLabel;
    private final String mCountAndPassLabel;

    @GuardedBy("this")
    private byte[] mDataSnapshot;
    private final boolean mDrop802_3Frames;
    private final int[] mEthTypeBlackList;

    @VisibleForTesting
    byte[] mHardwareAddress;

    @GuardedBy("this")
    private byte[] mIPv4Address;

    @GuardedBy("this")
    private int mIPv4PrefixLength;

    @GuardedBy("this")
    private boolean mInDozeMode;
    private final InterfaceParams mInterfaceParams;
    private final IpClient.Callback mIpClientCallback;

    @GuardedBy("this")
    private ApfProgramEvent mLastInstallEvent;

    @GuardedBy("this")
    private byte[] mLastInstalledProgram;

    @GuardedBy("this")
    private long mLastInstalledProgramMinLifetime;

    @GuardedBy("this")
    private long mLastTimeInstalledProgram;
    private final IpConnectivityLog mMetricsLog;

    @GuardedBy("this")
    private boolean mMulticastFilter;

    @VisibleForTesting
    ReceiveThread mReceiveThread;

    @GuardedBy("this")
    private long mUniqueCounter;
    private static final byte[] ETH_BROADCAST_MAC_ADDRESS = {-1, -1, -1, -1, -1, -1};
    private static final byte[] IPV6_ALL_NODES_ADDRESS = {-1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    private static final byte[] ARP_IPV4_HEADER = {0, 1, 8, 0, 6, 4};
    private final BroadcastReceiver mDeviceIdleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.os.action.DEVICE_IDLE_MODE_CHANGED")) {
                ApfFilter.this.setDozeMode(((PowerManager) context.getSystemService("power")).isDeviceIdleMode());
            }
        }
    };

    @GuardedBy("this")
    private ArrayList<Ra> mRas = new ArrayList<>();

    @GuardedBy("this")
    private int mNumProgramUpdates = 0;

    @GuardedBy("this")
    private int mNumProgramUpdatesAllowingMulticast = 0;

    public static class ApfConfiguration {
        public ApfCapabilities apfCapabilities;
        public int[] ethTypeBlackList;
        public boolean ieee802_3Filter;
        public boolean multicastFilter;
    }

    private enum ProcessRaResult {
        MATCH,
        DROPPED,
        PARSE_ERROR,
        ZERO_LIFETIME,
        UPDATE_NEW_RA,
        UPDATE_EXPIRY
    }

    @VisibleForTesting
    private enum Counter {
        RESERVED_OOB,
        TOTAL_PACKETS,
        PASSED_ARP,
        PASSED_DHCP,
        PASSED_IPV4,
        PASSED_IPV6_NON_ICMP,
        PASSED_IPV4_UNICAST,
        PASSED_IPV6_ICMP,
        PASSED_IPV6_UNICAST_NON_ICMP,
        PASSED_ARP_NON_IPV4,
        PASSED_ARP_UNKNOWN,
        PASSED_ARP_UNICAST_REPLY,
        PASSED_NON_IP_UNICAST,
        DROPPED_ETH_BROADCAST,
        DROPPED_RA,
        DROPPED_GARP_REPLY,
        DROPPED_ARP_OTHER_HOST,
        DROPPED_IPV4_L2_BROADCAST,
        DROPPED_IPV4_BROADCAST_ADDR,
        DROPPED_IPV4_BROADCAST_NET,
        DROPPED_IPV4_MULTICAST,
        DROPPED_IPV6_ROUTER_SOLICITATION,
        DROPPED_IPV6_MULTICAST_NA,
        DROPPED_IPV6_MULTICAST,
        DROPPED_IPV6_MULTICAST_PING,
        DROPPED_IPV6_NON_ICMP_MULTICAST,
        DROPPED_802_3_FRAME,
        DROPPED_ETHERTYPE_BLACKLISTED;

        public int offset() {
            return (-ordinal()) * 4;
        }

        public static int totalSize() {
            return (((Counter[]) Counter.class.getEnumConstants()).length - 1) * 4;
        }
    }

    private void maybeSetCounter(ApfGenerator apfGenerator, Counter counter) {
        if (this.mApfCapabilities.hasDataAccess()) {
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R1, counter.offset());
        }
    }

    @VisibleForTesting
    class ReceiveThread extends Thread {
        private final FileDescriptor mSocket;
        private volatile boolean mStopped;
        private final byte[] mPacket = new byte[1514];
        private final long mStart = SystemClock.elapsedRealtime();
        private final ApfStats mStats = new ApfStats();

        public ReceiveThread(FileDescriptor fileDescriptor) {
            this.mSocket = fileDescriptor;
        }

        public void halt() {
            this.mStopped = true;
            try {
                IoBridge.closeAndSignalBlockedThreads(this.mSocket);
            } catch (IOException e) {
            }
        }

        @Override
        public void run() {
            ApfFilter.this.log("begin monitoring");
            while (!this.mStopped) {
                try {
                    updateStats(ApfFilter.this.processRa(this.mPacket, Os.read(this.mSocket, this.mPacket, 0, this.mPacket.length)));
                } catch (ErrnoException | IOException e) {
                    if (!this.mStopped) {
                        Log.e(ApfFilter.TAG, "Read error", e);
                    }
                }
            }
            logStats();
        }

        private void updateStats(ProcessRaResult processRaResult) {
            this.mStats.receivedRas++;
            switch (AnonymousClass2.$SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[processRaResult.ordinal()]) {
                case 1:
                    this.mStats.matchingRas++;
                    break;
                case 2:
                    this.mStats.droppedRas++;
                    break;
                case 3:
                    this.mStats.parseErrors++;
                    break;
                case 4:
                    this.mStats.zeroLifetimeRas++;
                    break;
                case 5:
                    this.mStats.matchingRas++;
                    this.mStats.programUpdates++;
                    break;
                case 6:
                    this.mStats.programUpdates++;
                    break;
            }
        }

        private void logStats() {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            synchronized (this) {
                this.mStats.durationMs = jElapsedRealtime - this.mStart;
                this.mStats.maxProgramSize = ApfFilter.this.mApfCapabilities.maximumApfProgramSize;
                this.mStats.programUpdatesAll = ApfFilter.this.mNumProgramUpdates;
                this.mStats.programUpdatesAllowingMulticast = ApfFilter.this.mNumProgramUpdatesAllowingMulticast;
                ApfFilter.this.mMetricsLog.log(this.mStats);
                ApfFilter.this.logApfProgramEventLocked(jElapsedRealtime / 1000);
            }
        }
    }

    static class AnonymousClass2 {
        static final int[] $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult = new int[ProcessRaResult.values().length];

        static {
            try {
                $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.MATCH.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.DROPPED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.PARSE_ERROR.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.ZERO_LIFETIME.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.UPDATE_EXPIRY.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$android$net$apf$ApfFilter$ProcessRaResult[ProcessRaResult.UPDATE_NEW_RA.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    @VisibleForTesting
    ApfFilter(Context context, ApfConfiguration apfConfiguration, InterfaceParams interfaceParams, IpClient.Callback callback, IpConnectivityLog ipConnectivityLog) {
        this.mApfCapabilities = apfConfiguration.apfCapabilities;
        this.mIpClientCallback = callback;
        this.mInterfaceParams = interfaceParams;
        this.mMulticastFilter = apfConfiguration.multicastFilter;
        this.mDrop802_3Frames = apfConfiguration.ieee802_3Filter;
        this.mContext = context;
        if (this.mApfCapabilities.hasDataAccess()) {
            this.mCountAndPassLabel = "countAndPass";
            this.mCountAndDropLabel = "countAndDrop";
        } else {
            this.mCountAndPassLabel = ApfGenerator.PASS_LABEL;
            this.mCountAndDropLabel = ApfGenerator.DROP_LABEL;
        }
        this.mEthTypeBlackList = filterEthTypeBlackList(apfConfiguration.ethTypeBlackList);
        this.mMetricsLog = ipConnectivityLog;
        maybeStartFilter();
        this.mContext.registerReceiver(this.mDeviceIdleReceiver, new IntentFilter("android.os.action.DEVICE_IDLE_MODE_CHANGED"));
    }

    public synchronized void setDataSnapshot(byte[] bArr) {
        this.mDataSnapshot = bArr;
    }

    private void log(String str) {
        Log.d(TAG, "(" + this.mInterfaceParams.name + "): " + str);
    }

    @GuardedBy("this")
    private long getUniqueNumberLocked() {
        long j = this.mUniqueCounter;
        this.mUniqueCounter = 1 + j;
        return j;
    }

    @GuardedBy("this")
    private static int[] filterEthTypeBlackList(int[] iArr) {
        ArrayList arrayList = new ArrayList();
        int length = iArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            int i2 = iArr[i];
            if (i2 >= 1536 && i2 <= 65535 && !arrayList.contains(Integer.valueOf(i2))) {
                if (arrayList.size() == 20) {
                    Log.w(TAG, "Passed EthType Black List size too large (" + arrayList.size() + ") using top 20 protocols");
                    break;
                }
                arrayList.add(Integer.valueOf(i2));
            }
            i++;
        }
        return arrayList.stream().mapToInt(new ToIntFunction() {
            @Override
            public final int applyAsInt(Object obj) {
                return ((Integer) obj).intValue();
            }
        }).toArray();
    }

    @VisibleForTesting
    void maybeStartFilter() {
        try {
            this.mHardwareAddress = this.mInterfaceParams.macAddr.toByteArray();
            synchronized (this) {
                if (this.mApfCapabilities.hasDataAccess()) {
                    this.mIpClientCallback.installPacketFilter(new byte[this.mApfCapabilities.maximumApfProgramSize]);
                }
                installNewProgramLocked();
            }
            FileDescriptor fileDescriptorSocket = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_IPV6);
            Os.bind(fileDescriptorSocket, new PacketSocketAddress((short) OsConstants.ETH_P_IPV6, this.mInterfaceParams.index));
            NetworkUtils.attachRaFilter(fileDescriptorSocket, this.mApfCapabilities.apfPacketFormat);
            this.mReceiveThread = new ReceiveThread(fileDescriptorSocket);
            this.mReceiveThread.start();
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error starting filter", e);
        }
    }

    @VisibleForTesting
    protected long currentTimeSeconds() {
        return SystemClock.elapsedRealtime() / 1000;
    }

    public static class InvalidRaException extends Exception {
        public InvalidRaException(String str) {
            super(str);
        }
    }

    @VisibleForTesting
    class Ra {
        private static final int ICMP6_4_BYTE_LIFETIME_LEN = 4;
        private static final int ICMP6_4_BYTE_LIFETIME_OFFSET = 4;
        private static final int ICMP6_DNSSL_OPTION_TYPE = 31;
        private static final int ICMP6_PREFIX_OPTION_LEN = 32;
        private static final int ICMP6_PREFIX_OPTION_PREFERRED_LIFETIME_LEN = 4;
        private static final int ICMP6_PREFIX_OPTION_PREFERRED_LIFETIME_OFFSET = 8;
        private static final int ICMP6_PREFIX_OPTION_TYPE = 3;
        private static final int ICMP6_PREFIX_OPTION_VALID_LIFETIME_LEN = 4;
        private static final int ICMP6_PREFIX_OPTION_VALID_LIFETIME_OFFSET = 4;
        private static final int ICMP6_RA_CHECKSUM_LEN = 2;
        private static final int ICMP6_RA_CHECKSUM_OFFSET = 56;
        private static final int ICMP6_RA_HEADER_LEN = 16;
        private static final int ICMP6_RA_OPTION_OFFSET = 70;
        private static final int ICMP6_RA_ROUTER_LIFETIME_LEN = 2;
        private static final int ICMP6_RA_ROUTER_LIFETIME_OFFSET = 60;
        private static final int ICMP6_RDNSS_OPTION_TYPE = 25;
        private static final int ICMP6_ROUTE_INFO_OPTION_TYPE = 24;
        long mLastSeen;
        long mMinLifetime;
        private final ByteBuffer mPacket;
        private final ArrayList<Pair<Integer, Integer>> mNonLifetimes = new ArrayList<>();
        private final ArrayList<Integer> mPrefixOptionOffsets = new ArrayList<>();
        private final ArrayList<Integer> mRdnssOptionOffsets = new ArrayList<>();
        int seenCount = 0;

        String getLastMatchingPacket() {
            return HexDump.toHexString(this.mPacket.array(), 0, this.mPacket.capacity(), false);
        }

        private String IPv6AddresstoString(int i) {
            int i2;
            try {
                byte[] bArrArray = this.mPacket.array();
                if (i >= 0 && (i2 = i + 16) <= bArrArray.length && i2 >= i) {
                    return ((Inet6Address) InetAddress.getByAddress(Arrays.copyOfRange(bArrArray, i, i2))).getHostAddress();
                }
                return "???";
            } catch (ClassCastException | UnknownHostException e) {
                return "???";
            } catch (UnsupportedOperationException e2) {
                return "???";
            }
        }

        private void prefixOptionToString(StringBuffer stringBuffer, int i) {
            stringBuffer.append(String.format("%s/%d %ds/%ds ", IPv6AddresstoString(i + 16), Integer.valueOf(BitUtils.getUint8(this.mPacket, i + 2)), Long.valueOf(BitUtils.getUint32(this.mPacket, i + 4)), Long.valueOf(BitUtils.getUint32(this.mPacket, i + 8))));
        }

        private void rdnssOptionToString(StringBuffer stringBuffer, int i) {
            int uint8 = BitUtils.getUint8(this.mPacket, i + 1) * 8;
            if (uint8 < 24) {
                return;
            }
            long uint32 = BitUtils.getUint32(this.mPacket, i + 4);
            int i2 = (uint8 - 8) / 16;
            stringBuffer.append("DNS ");
            stringBuffer.append(uint32);
            stringBuffer.append("s");
            for (int i3 = 0; i3 < i2; i3++) {
                stringBuffer.append(" ");
                stringBuffer.append(IPv6AddresstoString(i + 8 + (16 * i3)));
            }
        }

        public String toString() {
            try {
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(String.format("RA %s -> %s %ds ", IPv6AddresstoString(22), IPv6AddresstoString(38), Integer.valueOf(BitUtils.getUint16(this.mPacket, 60))));
                Iterator<Integer> it = this.mPrefixOptionOffsets.iterator();
                while (it.hasNext()) {
                    prefixOptionToString(stringBuffer, it.next().intValue());
                }
                Iterator<Integer> it2 = this.mRdnssOptionOffsets.iterator();
                while (it2.hasNext()) {
                    rdnssOptionToString(stringBuffer, it2.next().intValue());
                }
                return stringBuffer.toString();
            } catch (IndexOutOfBoundsException | BufferUnderflowException e) {
                return "<Malformed RA>";
            }
        }

        private int addNonLifetime(int i, int i2, int i3) {
            int iPosition = i2 + this.mPacket.position();
            this.mNonLifetimes.add(new Pair<>(Integer.valueOf(i), Integer.valueOf(iPosition - i)));
            return iPosition + i3;
        }

        private int addNonLifetimeU32(int i) {
            return addNonLifetime(i, 4, 4);
        }

        Ra(byte[] bArr, int i) throws InvalidRaException {
            if (i < 70) {
                throw new InvalidRaException("Not an ICMP6 router advertisement");
            }
            this.mPacket = ByteBuffer.wrap(Arrays.copyOf(bArr, i));
            this.mLastSeen = ApfFilter.this.currentTimeSeconds();
            if (BitUtils.getUint16(this.mPacket, 12) != OsConstants.ETH_P_IPV6 || BitUtils.getUint8(this.mPacket, 20) != OsConstants.IPPROTO_ICMPV6 || BitUtils.getUint8(this.mPacket, 54) != 134) {
                throw new InvalidRaException("Not an ICMP6 router advertisement");
            }
            RaEvent.Builder builder = new RaEvent.Builder();
            int iAddNonLifetime = addNonLifetime(addNonLifetime(addNonLifetime(0, 15, 3), 56, 2), 60, 2);
            builder.updateRouterLifetime(BitUtils.getUint16(this.mPacket, 60));
            this.mPacket.position(70);
            while (this.mPacket.hasRemaining()) {
                int iPosition = this.mPacket.position();
                int uint8 = BitUtils.getUint8(this.mPacket, iPosition);
                int uint82 = BitUtils.getUint8(this.mPacket, iPosition + 1) * 8;
                if (uint8 == 3) {
                    int iAddNonLifetime2 = addNonLifetime(iAddNonLifetime, 4, 4);
                    builder.updatePrefixValidLifetime(BitUtils.getUint32(this.mPacket, iPosition + 4));
                    iAddNonLifetime = addNonLifetime(iAddNonLifetime2, 8, 4);
                    builder.updatePrefixPreferredLifetime(BitUtils.getUint32(this.mPacket, iPosition + 8));
                    this.mPrefixOptionOffsets.add(Integer.valueOf(iPosition));
                } else if (uint8 != 31) {
                    switch (uint8) {
                        case 24:
                            iAddNonLifetime = addNonLifetimeU32(iAddNonLifetime);
                            builder.updateRouteInfoLifetime(BitUtils.getUint32(this.mPacket, iPosition + 4));
                            break;
                        case 25:
                            this.mRdnssOptionOffsets.add(Integer.valueOf(iPosition));
                            iAddNonLifetime = addNonLifetimeU32(iAddNonLifetime);
                            builder.updateRdnssLifetime(BitUtils.getUint32(this.mPacket, iPosition + 4));
                            break;
                    }
                } else {
                    iAddNonLifetime = addNonLifetimeU32(iAddNonLifetime);
                    builder.updateDnsslLifetime(BitUtils.getUint32(this.mPacket, iPosition + 4));
                }
                if (uint82 <= 0) {
                    throw new InvalidRaException(String.format("Invalid option length opt=%d len=%d", Integer.valueOf(uint8), Integer.valueOf(uint82)));
                }
                this.mPacket.position(iPosition + uint82);
            }
            addNonLifetime(iAddNonLifetime, 0, 0);
            this.mMinLifetime = minLifetime(bArr, i);
            ApfFilter.this.mMetricsLog.log(builder.build());
        }

        boolean matches(byte[] bArr, int i) {
            if (i != this.mPacket.capacity()) {
                return false;
            }
            byte[] bArrArray = this.mPacket.array();
            for (Pair<Integer, Integer> pair : this.mNonLifetimes) {
                for (int iIntValue = ((Integer) pair.first).intValue(); iIntValue < ((Integer) pair.first).intValue() + ((Integer) pair.second).intValue(); iIntValue++) {
                    if (bArr[iIntValue] != bArrArray[iIntValue]) {
                        return false;
                    }
                }
            }
            return true;
        }

        long minLifetime(byte[] bArr, int i) {
            long uint16;
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr);
            long jMin = JobStatus.NO_LATEST_RUNTIME;
            int i2 = 0;
            while (true) {
                int i3 = i2 + 1;
                if (i3 < this.mNonLifetimes.size()) {
                    int iIntValue = ((Integer) this.mNonLifetimes.get(i2).first).intValue() + ((Integer) this.mNonLifetimes.get(i2).second).intValue();
                    if (iIntValue != 15 && iIntValue != 56) {
                        int iIntValue2 = ((Integer) this.mNonLifetimes.get(i3).first).intValue() - iIntValue;
                        if (iIntValue2 == 2) {
                            uint16 = BitUtils.getUint16(byteBufferWrap, iIntValue);
                        } else if (iIntValue2 == 4) {
                            uint16 = BitUtils.getUint32(byteBufferWrap, iIntValue);
                        } else {
                            throw new IllegalStateException("bogus lifetime size " + iIntValue2);
                        }
                        jMin = Math.min(jMin, uint16);
                    }
                    i2 = i3;
                } else {
                    return jMin;
                }
            }
        }

        long currentLifetime() {
            return this.mMinLifetime - (ApfFilter.this.currentTimeSeconds() - this.mLastSeen);
        }

        boolean isExpired() {
            return currentLifetime() <= 0;
        }

        @GuardedBy("ApfFilter.this")
        long generateFilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
            String str = "Ra" + ApfFilter.this.getUniqueNumberLocked();
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R0, 14);
            apfGenerator.addJumpIfR0NotEquals(this.mPacket.capacity(), str);
            int iCurrentLifetime = (int) (currentLifetime() / 6);
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R0, 15);
            apfGenerator.addJumpIfR0GreaterThan(iCurrentLifetime, str);
            int i = 0;
            while (i < this.mNonLifetimes.size()) {
                Pair<Integer, Integer> pair = this.mNonLifetimes.get(i);
                if (((Integer) pair.second).intValue() != 0) {
                    apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, ((Integer) pair.first).intValue());
                    apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, Arrays.copyOfRange(this.mPacket.array(), ((Integer) pair.first).intValue(), ((Integer) pair.first).intValue() + ((Integer) pair.second).intValue()), str);
                }
                i++;
                if (i < this.mNonLifetimes.size()) {
                    Pair<Integer, Integer> pair2 = this.mNonLifetimes.get(i);
                    int iIntValue = ((Integer) pair.first).intValue() + ((Integer) pair.second).intValue();
                    if (iIntValue != 15 && iIntValue != 56) {
                        int iIntValue2 = ((Integer) pair2.first).intValue() - iIntValue;
                        if (iIntValue2 == 2) {
                            apfGenerator.addLoad16(ApfGenerator.Register.R0, iIntValue);
                        } else if (iIntValue2 == 4) {
                            apfGenerator.addLoad32(ApfGenerator.Register.R0, iIntValue);
                        } else {
                            throw new IllegalStateException("bogus lifetime size " + iIntValue2);
                        }
                        apfGenerator.addJumpIfR0LessThan(iCurrentLifetime, str);
                    }
                }
            }
            ApfFilter.this.maybeSetCounter(apfGenerator, Counter.DROPPED_RA);
            apfGenerator.addJump(ApfFilter.this.mCountAndDropLabel);
            apfGenerator.defineLabel(str);
            return iCurrentLifetime;
        }
    }

    @GuardedBy("this")
    private void generateArpFilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 14);
        maybeSetCounter(apfGenerator, Counter.PASSED_ARP_NON_IPV4);
        apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ARP_IPV4_HEADER, this.mCountAndPassLabel);
        apfGenerator.addLoad16(ApfGenerator.Register.R0, 20);
        apfGenerator.addJumpIfR0Equals(1, "checkTargetIPv4");
        maybeSetCounter(apfGenerator, Counter.PASSED_ARP_UNKNOWN);
        apfGenerator.addJumpIfR0NotEquals(2, this.mCountAndPassLabel);
        apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 0);
        maybeSetCounter(apfGenerator, Counter.PASSED_ARP_UNICAST_REPLY);
        apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ETH_BROADCAST_MAC_ADDRESS, this.mCountAndPassLabel);
        apfGenerator.defineLabel("checkTargetIPv4");
        if (this.mIPv4Address == null) {
            apfGenerator.addLoad32(ApfGenerator.Register.R0, 38);
            maybeSetCounter(apfGenerator, Counter.DROPPED_GARP_REPLY);
            apfGenerator.addJumpIfR0Equals(0, this.mCountAndDropLabel);
        } else {
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 38);
            maybeSetCounter(apfGenerator, Counter.DROPPED_ARP_OTHER_HOST);
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mIPv4Address, this.mCountAndDropLabel);
        }
        maybeSetCounter(apfGenerator, Counter.PASSED_ARP);
        apfGenerator.addJump(this.mCountAndPassLabel);
    }

    @GuardedBy("this")
    private void generateIPv4FilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        if (this.mMulticastFilter) {
            apfGenerator.addLoad8(ApfGenerator.Register.R0, 23);
            apfGenerator.addJumpIfR0NotEquals(OsConstants.IPPROTO_UDP, "skip_dhcp_v4_filter");
            apfGenerator.addLoad16(ApfGenerator.Register.R0, 20);
            apfGenerator.addJumpIfR0AnyBitsSet(8191, "skip_dhcp_v4_filter");
            apfGenerator.addLoadFromMemory(ApfGenerator.Register.R1, 13);
            apfGenerator.addLoad16Indexed(ApfGenerator.Register.R0, 16);
            apfGenerator.addJumpIfR0NotEquals(68, "skip_dhcp_v4_filter");
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 50);
            apfGenerator.addAddR1();
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mHardwareAddress, "skip_dhcp_v4_filter");
            maybeSetCounter(apfGenerator, Counter.PASSED_DHCP);
            apfGenerator.addJump(this.mCountAndPassLabel);
            apfGenerator.defineLabel("skip_dhcp_v4_filter");
            apfGenerator.addLoad8(ApfGenerator.Register.R0, 30);
            apfGenerator.addAnd(240);
            maybeSetCounter(apfGenerator, Counter.DROPPED_IPV4_MULTICAST);
            apfGenerator.addJumpIfR0Equals(UsbDescriptor.CLASSID_WIRELESS, this.mCountAndDropLabel);
            maybeSetCounter(apfGenerator, Counter.DROPPED_IPV4_BROADCAST_ADDR);
            apfGenerator.addLoad32(ApfGenerator.Register.R0, 30);
            apfGenerator.addJumpIfR0Equals(-1, this.mCountAndDropLabel);
            if (this.mIPv4Address != null && this.mIPv4PrefixLength < 31) {
                maybeSetCounter(apfGenerator, Counter.DROPPED_IPV4_BROADCAST_NET);
                apfGenerator.addJumpIfR0Equals(ipv4BroadcastAddress(this.mIPv4Address, this.mIPv4PrefixLength), this.mCountAndDropLabel);
            }
            maybeSetCounter(apfGenerator, Counter.PASSED_IPV4_UNICAST);
            apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 0);
            apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ETH_BROADCAST_MAC_ADDRESS, this.mCountAndPassLabel);
            maybeSetCounter(apfGenerator, Counter.DROPPED_IPV4_L2_BROADCAST);
            apfGenerator.addJump(this.mCountAndDropLabel);
        }
        maybeSetCounter(apfGenerator, Counter.PASSED_IPV4);
        apfGenerator.addJump(this.mCountAndPassLabel);
    }

    @GuardedBy("this")
    private void generateIPv6FilterLocked(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        apfGenerator.addLoad8(ApfGenerator.Register.R0, 20);
        if (this.mMulticastFilter) {
            if (this.mInDozeMode) {
                apfGenerator.addJumpIfR0NotEquals(OsConstants.IPPROTO_ICMPV6, "dropAllIPv6Multicast");
                apfGenerator.addLoad8(ApfGenerator.Register.R0, 54);
                apfGenerator.addJumpIfR0NotEquals(128, "skipIPv6MulticastFilter");
            } else {
                apfGenerator.addJumpIfR0Equals(OsConstants.IPPROTO_ICMPV6, "skipIPv6MulticastFilter");
            }
            apfGenerator.defineLabel("dropAllIPv6Multicast");
            maybeSetCounter(apfGenerator, Counter.DROPPED_IPV6_NON_ICMP_MULTICAST);
            apfGenerator.addLoad8(ApfGenerator.Register.R0, 38);
            apfGenerator.addJumpIfR0Equals(255, this.mCountAndDropLabel);
            maybeSetCounter(apfGenerator, Counter.PASSED_IPV6_UNICAST_NON_ICMP);
            apfGenerator.addJump(this.mCountAndPassLabel);
            apfGenerator.defineLabel("skipIPv6MulticastFilter");
        } else {
            maybeSetCounter(apfGenerator, Counter.PASSED_IPV6_NON_ICMP);
            apfGenerator.addJumpIfR0NotEquals(OsConstants.IPPROTO_ICMPV6, this.mCountAndPassLabel);
        }
        apfGenerator.addLoad8(ApfGenerator.Register.R0, 54);
        maybeSetCounter(apfGenerator, Counter.DROPPED_IPV6_ROUTER_SOLICITATION);
        apfGenerator.addJumpIfR0Equals(NetworkConstants.ICMPV6_ROUTER_SOLICITATION, this.mCountAndDropLabel);
        apfGenerator.addJumpIfR0NotEquals(NetworkConstants.ICMPV6_NEIGHBOR_ADVERTISEMENT, "skipUnsolicitedMulticastNA");
        apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 38);
        apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, IPV6_ALL_NODES_ADDRESS, "skipUnsolicitedMulticastNA");
        maybeSetCounter(apfGenerator, Counter.DROPPED_IPV6_MULTICAST_NA);
        apfGenerator.addJump(this.mCountAndDropLabel);
        apfGenerator.defineLabel("skipUnsolicitedMulticastNA");
    }

    @GuardedBy("this")
    private ApfGenerator emitPrologueLocked() throws ApfGenerator.IllegalInstructionException {
        ApfGenerator apfGenerator = new ApfGenerator(this.mApfCapabilities.apfVersionSupported);
        if (this.mApfCapabilities.hasDataAccess()) {
            maybeSetCounter(apfGenerator, Counter.TOTAL_PACKETS);
            apfGenerator.addLoadData(ApfGenerator.Register.R0, 0);
            apfGenerator.addAdd(1);
            apfGenerator.addStoreData(ApfGenerator.Register.R0, 0);
        }
        apfGenerator.addLoad16(ApfGenerator.Register.R0, 12);
        if (this.mDrop802_3Frames) {
            maybeSetCounter(apfGenerator, Counter.DROPPED_802_3_FRAME);
            apfGenerator.addJumpIfR0LessThan(1536, this.mCountAndDropLabel);
        }
        maybeSetCounter(apfGenerator, Counter.DROPPED_ETHERTYPE_BLACKLISTED);
        for (int i : this.mEthTypeBlackList) {
            apfGenerator.addJumpIfR0Equals(i, this.mCountAndDropLabel);
        }
        apfGenerator.addJumpIfR0NotEquals(OsConstants.ETH_P_ARP, "skipArpFilters");
        generateArpFilterLocked(apfGenerator);
        apfGenerator.defineLabel("skipArpFilters");
        apfGenerator.addJumpIfR0NotEquals(OsConstants.ETH_P_IP, "skipIPv4Filters");
        generateIPv4FilterLocked(apfGenerator);
        apfGenerator.defineLabel("skipIPv4Filters");
        apfGenerator.addJumpIfR0Equals(OsConstants.ETH_P_IPV6, "IPv6Filters");
        apfGenerator.addLoadImmediate(ApfGenerator.Register.R0, 0);
        maybeSetCounter(apfGenerator, Counter.PASSED_NON_IP_UNICAST);
        apfGenerator.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ETH_BROADCAST_MAC_ADDRESS, this.mCountAndPassLabel);
        maybeSetCounter(apfGenerator, Counter.DROPPED_ETH_BROADCAST);
        apfGenerator.addJump(this.mCountAndDropLabel);
        apfGenerator.defineLabel("IPv6Filters");
        generateIPv6FilterLocked(apfGenerator);
        return apfGenerator;
    }

    @GuardedBy("this")
    private void emitEpilogue(ApfGenerator apfGenerator) throws ApfGenerator.IllegalInstructionException {
        if (this.mApfCapabilities.hasDataAccess()) {
            maybeSetCounter(apfGenerator, Counter.PASSED_IPV6_ICMP);
            apfGenerator.defineLabel(this.mCountAndPassLabel);
            apfGenerator.addLoadData(ApfGenerator.Register.R0, 0);
            apfGenerator.addAdd(1);
            apfGenerator.addStoreData(ApfGenerator.Register.R0, 0);
            apfGenerator.addJump(ApfGenerator.PASS_LABEL);
            apfGenerator.defineLabel(this.mCountAndDropLabel);
            apfGenerator.addLoadData(ApfGenerator.Register.R0, 0);
            apfGenerator.addAdd(1);
            apfGenerator.addStoreData(ApfGenerator.Register.R0, 0);
            apfGenerator.addJump(ApfGenerator.DROP_LABEL);
        }
    }

    @GuardedBy("this")
    @VisibleForTesting
    void installNewProgramLocked() {
        purgeExpiredRasLocked();
        ArrayList arrayList = new ArrayList();
        long j = this.mApfCapabilities.maximumApfProgramSize;
        if (this.mApfCapabilities.hasDataAccess()) {
            j -= (long) Counter.totalSize();
        }
        try {
            ApfGenerator apfGeneratorEmitPrologueLocked = emitPrologueLocked();
            emitEpilogue(apfGeneratorEmitPrologueLocked);
            if (apfGeneratorEmitPrologueLocked.programLengthOverEstimate() > j) {
                Log.e(TAG, "Program exceeds maximum size " + j);
                return;
            }
            for (Ra ra : this.mRas) {
                ra.generateFilterLocked(apfGeneratorEmitPrologueLocked);
                if (apfGeneratorEmitPrologueLocked.programLengthOverEstimate() > j) {
                    break;
                } else {
                    arrayList.add(ra);
                }
            }
            ApfGenerator apfGeneratorEmitPrologueLocked2 = emitPrologueLocked();
            Iterator it = arrayList.iterator();
            long jMin = JobStatus.NO_LATEST_RUNTIME;
            while (it.hasNext()) {
                jMin = Math.min(jMin, ((Ra) it.next()).generateFilterLocked(apfGeneratorEmitPrologueLocked2));
            }
            emitEpilogue(apfGeneratorEmitPrologueLocked2);
            byte[] bArrGenerate = apfGeneratorEmitPrologueLocked2.generate();
            long jCurrentTimeSeconds = currentTimeSeconds();
            this.mLastTimeInstalledProgram = jCurrentTimeSeconds;
            this.mLastInstalledProgramMinLifetime = jMin;
            this.mLastInstalledProgram = bArrGenerate;
            this.mNumProgramUpdates++;
            this.mIpClientCallback.installPacketFilter(bArrGenerate);
            logApfProgramEventLocked(jCurrentTimeSeconds);
            this.mLastInstallEvent = new ApfProgramEvent();
            this.mLastInstallEvent.lifetime = jMin;
            this.mLastInstallEvent.filteredRas = arrayList.size();
            this.mLastInstallEvent.currentRas = this.mRas.size();
            this.mLastInstallEvent.programLength = bArrGenerate.length;
            this.mLastInstallEvent.flags = ApfProgramEvent.flagsFor(this.mIPv4Address != null, this.mMulticastFilter);
        } catch (ApfGenerator.IllegalInstructionException | IllegalStateException e) {
            Log.e(TAG, "Failed to generate APF program.", e);
        }
    }

    @GuardedBy("this")
    private void logApfProgramEventLocked(long j) {
        if (this.mLastInstallEvent == null) {
            return;
        }
        ApfProgramEvent apfProgramEvent = this.mLastInstallEvent;
        this.mLastInstallEvent = null;
        apfProgramEvent.actualLifetime = j - this.mLastTimeInstalledProgram;
        if (apfProgramEvent.actualLifetime < 2) {
            return;
        }
        this.mMetricsLog.log(apfProgramEvent);
    }

    private boolean shouldInstallnewProgram() {
        return this.mLastTimeInstalledProgram + this.mLastInstalledProgramMinLifetime < currentTimeSeconds() + MAX_PROGRAM_LIFETIME_WORTH_REFRESHING;
    }

    private void hexDump(String str, byte[] bArr, int i) {
        log(str + HexDump.toHexString(bArr, 0, i, false));
    }

    @GuardedBy("this")
    private void purgeExpiredRasLocked() {
        int i = 0;
        while (i < this.mRas.size()) {
            if (this.mRas.get(i).isExpired()) {
                log("Expiring " + this.mRas.get(i));
                this.mRas.remove(i);
            } else {
                i++;
            }
        }
    }

    @VisibleForTesting
    synchronized ProcessRaResult processRa(byte[] bArr, int i) {
        for (int i2 = 0; i2 < this.mRas.size(); i2++) {
            Ra ra = this.mRas.get(i2);
            if (ra.matches(bArr, i)) {
                ra.mLastSeen = currentTimeSeconds();
                ra.mMinLifetime = ra.minLifetime(bArr, i);
                ra.seenCount++;
                this.mRas.add(0, this.mRas.remove(i2));
                if (shouldInstallnewProgram()) {
                    installNewProgramLocked();
                    return ProcessRaResult.UPDATE_EXPIRY;
                }
                return ProcessRaResult.MATCH;
            }
        }
        purgeExpiredRasLocked();
        if (this.mRas.size() >= 10) {
            return ProcessRaResult.DROPPED;
        }
        try {
            Ra ra2 = new Ra(bArr, i);
            if (ra2.isExpired()) {
                return ProcessRaResult.ZERO_LIFETIME;
            }
            log("Adding " + ra2);
            this.mRas.add(ra2);
            installNewProgramLocked();
            return ProcessRaResult.UPDATE_NEW_RA;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing RA", e);
            return ProcessRaResult.PARSE_ERROR;
        }
    }

    public static ApfFilter maybeCreate(Context context, ApfConfiguration apfConfiguration, InterfaceParams interfaceParams, IpClient.Callback callback) {
        ApfCapabilities apfCapabilities;
        if (context == null || apfConfiguration == null || interfaceParams == null || (apfCapabilities = apfConfiguration.apfCapabilities) == null || apfCapabilities.apfVersionSupported == 0) {
            return null;
        }
        if (apfCapabilities.maximumApfProgramSize < 512) {
            Log.e(TAG, "Unacceptably small APF limit: " + apfCapabilities.maximumApfProgramSize);
            return null;
        }
        if (apfCapabilities.apfPacketFormat != OsConstants.ARPHRD_ETHER) {
            return null;
        }
        if (!ApfGenerator.supportsVersion(apfCapabilities.apfVersionSupported)) {
            Log.e(TAG, "Unsupported APF version: " + apfCapabilities.apfVersionSupported);
            return null;
        }
        return new ApfFilter(context, apfConfiguration, interfaceParams, callback, new IpConnectivityLog());
    }

    public synchronized void shutdown() {
        if (this.mReceiveThread != null) {
            log("shutting down");
            this.mReceiveThread.halt();
            this.mReceiveThread = null;
        }
        this.mRas.clear();
        this.mContext.unregisterReceiver(this.mDeviceIdleReceiver);
    }

    public synchronized void setMulticastFilter(boolean z) {
        if (this.mMulticastFilter == z) {
            return;
        }
        this.mMulticastFilter = z;
        if (!z) {
            this.mNumProgramUpdatesAllowingMulticast++;
        }
        installNewProgramLocked();
    }

    @VisibleForTesting
    public synchronized void setDozeMode(boolean z) {
        if (this.mInDozeMode == z) {
            return;
        }
        this.mInDozeMode = z;
        installNewProgramLocked();
    }

    private static LinkAddress findIPv4LinkAddress(LinkProperties linkProperties) {
        LinkAddress linkAddress = null;
        for (LinkAddress linkAddress2 : linkProperties.getLinkAddresses()) {
            if (linkAddress2.getAddress() instanceof Inet4Address) {
                if (linkAddress != null && !linkAddress.isSameAddressAs(linkAddress2)) {
                    return null;
                }
                linkAddress = linkAddress2;
            }
        }
        return linkAddress;
    }

    public synchronized void setLinkProperties(LinkProperties linkProperties) {
        LinkAddress linkAddressFindIPv4LinkAddress = findIPv4LinkAddress(linkProperties);
        byte[] address = linkAddressFindIPv4LinkAddress != null ? linkAddressFindIPv4LinkAddress.getAddress().getAddress() : null;
        int prefixLength = linkAddressFindIPv4LinkAddress != null ? linkAddressFindIPv4LinkAddress.getPrefixLength() : 0;
        if (prefixLength == this.mIPv4PrefixLength && Arrays.equals(address, this.mIPv4Address)) {
            return;
        }
        this.mIPv4Address = address;
        this.mIPv4PrefixLength = prefixLength;
        installNewProgramLocked();
    }

    public static long counterValue(byte[] bArr, Counter counter) throws ArrayIndexOutOfBoundsException {
        int iOffset = counter.offset();
        if (iOffset < 0) {
            iOffset += bArr.length;
        }
        long j = 0;
        for (int i = 0; i < 4; i++) {
            j = (j << 8) | ((long) (bArr[iOffset] & 255));
            iOffset++;
        }
        return j;
    }

    public synchronized void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("Capabilities: " + this.mApfCapabilities);
        StringBuilder sb = new StringBuilder();
        sb.append("Receive thread: ");
        sb.append(this.mReceiveThread != null ? "RUNNING" : "STOPPED");
        indentingPrintWriter.println(sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Multicast: ");
        sb2.append(this.mMulticastFilter ? "DROP" : "ALLOW");
        indentingPrintWriter.println(sb2.toString());
        try {
            indentingPrintWriter.println("IPv4 address: " + InetAddress.getByAddress(this.mIPv4Address).getHostAddress());
        } catch (NullPointerException | UnknownHostException e) {
        }
        if (this.mLastTimeInstalledProgram == 0) {
            indentingPrintWriter.println("No program installed.");
            return;
        }
        indentingPrintWriter.println("Program updates: " + this.mNumProgramUpdates);
        indentingPrintWriter.println(String.format("Last program length %d, installed %ds ago, lifetime %ds", Integer.valueOf(this.mLastInstalledProgram.length), Long.valueOf(currentTimeSeconds() - this.mLastTimeInstalledProgram), Long.valueOf(this.mLastInstalledProgramMinLifetime)));
        indentingPrintWriter.println("RA filters:");
        indentingPrintWriter.increaseIndent();
        for (Ra ra : this.mRas) {
            indentingPrintWriter.println(ra);
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println(String.format("Seen: %d, last %ds ago", Integer.valueOf(ra.seenCount), Long.valueOf(currentTimeSeconds() - ra.mLastSeen)));
            indentingPrintWriter.println("Last match:");
            indentingPrintWriter.increaseIndent();
            indentingPrintWriter.println(ra.getLastMatchingPacket());
            indentingPrintWriter.decreaseIndent();
            indentingPrintWriter.decreaseIndent();
        }
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("Last program:");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println(HexDump.toHexString(this.mLastInstalledProgram, false));
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("APF packet counters: ");
        indentingPrintWriter.increaseIndent();
        if (!this.mApfCapabilities.hasDataAccess()) {
            indentingPrintWriter.println("APF counters not supported");
        } else if (this.mDataSnapshot == null) {
            indentingPrintWriter.println("No last snapshot.");
        } else {
            try {
                Counter[] counterArr = (Counter[]) Counter.class.getEnumConstants();
                for (Counter counter : Arrays.asList(counterArr).subList(1, counterArr.length)) {
                    long jCounterValue = counterValue(this.mDataSnapshot, counter);
                    if (jCounterValue != 0) {
                        indentingPrintWriter.println(counter.toString() + ": " + jCounterValue);
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e2) {
                indentingPrintWriter.println("Uh-oh: " + e2);
            }
        }
        indentingPrintWriter.decreaseIndent();
    }

    @VisibleForTesting
    public static int ipv4BroadcastAddress(byte[] bArr, int i) {
        return BitUtils.bytesToBEInt(bArr) | ((int) (BitUtils.uint32(-1) >>> i));
    }
}
