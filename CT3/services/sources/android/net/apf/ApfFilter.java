package android.net.apf;

import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.net.apf.ApfGenerator;
import android.net.dhcp.DhcpPacket;
import android.net.ip.IpManager;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.PacketSocketAddress;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.HexDump;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.controllers.JobStatus;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import libcore.io.IoBridge;

public class ApfFilter {
    private static final boolean DBG = true;
    private static final int DHCP_CLIENT_MAC_OFFSET = 50;
    private static final int DHCP_CLIENT_PORT = 68;
    private static final int ETH_DEST_ADDR_OFFSET = 0;
    private static final int ETH_ETHERTYPE_OFFSET = 12;
    private static final int ETH_HEADER_LEN = 14;
    private static final int FRACTION_OF_LIFETIME_TO_FILTER = 6;
    private static final int ICMP6_NEIGHBOR_ANNOUNCEMENT = 136;
    private static final int ICMP6_TYPE_OFFSET = 54;
    private static final int IPV4_DEST_ADDR_OFFSET = 30;
    private static final int IPV4_FRAGMENT_OFFSET_MASK = 8191;
    private static final int IPV4_FRAGMENT_OFFSET_OFFSET = 20;
    private static final int IPV4_PROTOCOL_OFFSET = 23;
    private static final int IPV6_DEST_ADDR_OFFSET = 38;
    private static final int IPV6_HEADER_LEN = 40;
    private static final int IPV6_NEXT_HEADER_OFFSET = 20;
    private static final int IPV6_SRC_ADDR_OFFSET = 22;
    private static final long MAX_PROGRAM_LIFETIME_WORTH_REFRESHING = 30;
    private static final int MAX_RAS = 10;
    private static final String TAG = "ApfFilter";
    private static final int UDP_DESTINATION_PORT_OFFSET = 16;
    private static final int UDP_HEADER_LEN = 8;
    private static final boolean VDBG = true;
    private final ApfCapabilities mApfCapabilities;
    byte[] mHardwareAddress;

    @GuardedBy("this")
    private byte[] mIPv4Address;
    private final IpManager.Callback mIpManagerCallback;

    @GuardedBy("this")
    private byte[] mLastInstalledProgram;

    @GuardedBy("this")
    private long mLastInstalledProgramMinLifetime;

    @GuardedBy("this")
    private long mLastTimeInstalledProgram;

    @GuardedBy("this")
    private boolean mMulticastFilter;
    private final NetworkInterface mNetworkInterface;

    @GuardedBy("this")
    private int mNumProgramUpdates;

    @GuardedBy("this")
    private ArrayList<Ra> mRas = new ArrayList<>();
    ReceiveThread mReceiveThread;

    @GuardedBy("this")
    private long mUniqueCounter;
    private static final byte[] ETH_BROADCAST_MAC_ADDRESS = {-1, -1, -1, -1, -1, -1};
    private static final byte[] IPV6_ALL_NODES_ADDRESS = {-1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    private static int ARP_HEADER_OFFSET = 14;
    private static final byte[] ARP_IPV4_REQUEST_HEADER = {0, 1, 8, 0, 6, 4, 0, 1};
    private static int ARP_TARGET_IP_ADDRESS_OFFSET = 38;

    class ReceiveThread extends Thread {
        private final byte[] mPacket = new byte[1514];
        private final FileDescriptor mSocket;
        private volatile boolean mStopped;

        public ReceiveThread(FileDescriptor socket) {
            this.mSocket = socket;
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
                    int length = Os.read(this.mSocket, this.mPacket, 0, this.mPacket.length);
                    ApfFilter.this.processRa(this.mPacket, length);
                } catch (ErrnoException | IOException e) {
                    if (!this.mStopped) {
                        Log.e(ApfFilter.TAG, "Read error", e);
                    }
                }
            }
        }
    }

    ApfFilter(ApfCapabilities apfCapabilities, NetworkInterface networkInterface, IpManager.Callback ipManagerCallback, boolean multicastFilter) {
        this.mApfCapabilities = apfCapabilities;
        this.mIpManagerCallback = ipManagerCallback;
        this.mNetworkInterface = networkInterface;
        this.mMulticastFilter = multicastFilter;
        maybeStartFilter();
    }

    private void log(String s) {
        Log.d(TAG, "(" + this.mNetworkInterface.getName() + "): " + s);
    }

    @GuardedBy("this")
    private long getUniqueNumberLocked() {
        long j = this.mUniqueCounter;
        this.mUniqueCounter = 1 + j;
        return j;
    }

    void maybeStartFilter() {
        try {
            this.mHardwareAddress = this.mNetworkInterface.getHardwareAddress();
            synchronized (this) {
                installNewProgramLocked();
            }
            FileDescriptor socket = Os.socket(OsConstants.AF_PACKET, OsConstants.SOCK_RAW, OsConstants.ETH_P_IPV6);
            PacketSocketAddress addr = new PacketSocketAddress((short) OsConstants.ETH_P_IPV6, this.mNetworkInterface.getIndex());
            Os.bind(socket, addr);
            NetworkUtils.attachRaFilter(socket, this.mApfCapabilities.apfPacketFormat);
            this.mReceiveThread = new ReceiveThread(socket);
            this.mReceiveThread.start();
        } catch (ErrnoException | SocketException e) {
            Log.e(TAG, "Error starting filter", e);
        }
    }

    private static long curTime() {
        return System.currentTimeMillis() / 1000;
    }

    private class Ra {
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

        private String IPv6AddresstoString(int pos) {
            try {
                byte[] array = this.mPacket.array();
                if (pos < 0 || pos + 16 > array.length || pos + 16 < pos) {
                    return "???";
                }
                byte[] addressBytes = Arrays.copyOfRange(array, pos, pos + 16);
                InetAddress address = (Inet6Address) InetAddress.getByAddress(addressBytes);
                return address.getHostAddress();
            } catch (ClassCastException | UnknownHostException e) {
                return "???";
            } catch (UnsupportedOperationException e2) {
                return "???";
            }
        }

        private int uint8(byte b) {
            return b & 255;
        }

        private int uint16(short s) {
            return 65535 & s;
        }

        private long uint32(int s) {
            return s & (-1);
        }

        private void prefixOptionToString(StringBuffer sb, int offset) {
            String prefix = IPv6AddresstoString(offset + 16);
            int length = uint8(this.mPacket.get(offset + 2));
            long valid = this.mPacket.getInt(offset + 4);
            long preferred = this.mPacket.getInt(offset + 8);
            sb.append(String.format("%s/%d %ds/%ds ", prefix, Integer.valueOf(length), Long.valueOf(valid), Long.valueOf(preferred)));
        }

        private void rdnssOptionToString(StringBuffer sb, int offset) {
            int optLen = uint8(this.mPacket.get(offset + 1)) * 8;
            if (optLen < 24) {
                return;
            }
            long lifetime = uint32(this.mPacket.getInt(offset + 4));
            int numServers = (optLen - 8) / 16;
            sb.append("DNS ").append(lifetime).append("s");
            for (int server = 0; server < numServers; server++) {
                sb.append(" ").append(IPv6AddresstoString(offset + 8 + (server * 16)));
            }
        }

        public String toString() {
            try {
                StringBuffer sb = new StringBuffer();
                sb.append(String.format("RA %s -> %s %ds ", IPv6AddresstoString(22), IPv6AddresstoString(38), Integer.valueOf(uint16(this.mPacket.getShort(60)))));
                Iterator i$iterator = this.mPrefixOptionOffsets.iterator();
                while (i$iterator.hasNext()) {
                    int i = ((Integer) i$iterator.next()).intValue();
                    prefixOptionToString(sb, i);
                }
                Iterator i$iterator2 = this.mRdnssOptionOffsets.iterator();
                while (i$iterator2.hasNext()) {
                    int i2 = ((Integer) i$iterator2.next()).intValue();
                    rdnssOptionToString(sb, i2);
                }
                return sb.toString();
            } catch (IndexOutOfBoundsException | BufferUnderflowException e) {
                return "<Malformed RA>";
            }
        }

        private int addNonLifetime(int lastNonLifetimeStart, int lifetimeOffset, int lifetimeLength) {
            int lifetimeOffset2 = lifetimeOffset + this.mPacket.position();
            this.mNonLifetimes.add(new Pair<>(Integer.valueOf(lastNonLifetimeStart), Integer.valueOf(lifetimeOffset2 - lastNonLifetimeStart)));
            return lifetimeOffset2 + lifetimeLength;
        }

        Ra(byte[] packet, int length) {
            this.mPacket = ByteBuffer.allocate(length).put(ByteBuffer.wrap(packet, 0, length));
            this.mPacket.clear();
            this.mLastSeen = ApfFilter.curTime();
            int lastNonLifetimeStart = addNonLifetime(0, 56, 2);
            int lastNonLifetimeStart2 = addNonLifetime(lastNonLifetimeStart, 60, 2);
            this.mPacket.position(70);
            while (this.mPacket.hasRemaining()) {
                int optionType = this.mPacket.get(this.mPacket.position()) & 255;
                int optionLength = (this.mPacket.get(this.mPacket.position() + 1) & 255) * 8;
                switch (optionType) {
                    case 3:
                        lastNonLifetimeStart2 = addNonLifetime(addNonLifetime(lastNonLifetimeStart2, 4, 4), 8, 4);
                        this.mPrefixOptionOffsets.add(Integer.valueOf(this.mPacket.position()));
                        break;
                    case 25:
                        this.mRdnssOptionOffsets.add(Integer.valueOf(this.mPacket.position()));
                    case 24:
                    case 31:
                        lastNonLifetimeStart2 = addNonLifetime(lastNonLifetimeStart2, 4, 4);
                        break;
                }
                if (optionLength <= 0) {
                    throw new IllegalArgumentException(String.format("Invalid option length opt=%d len=%d", Integer.valueOf(optionType), Integer.valueOf(optionLength)));
                }
                this.mPacket.position(this.mPacket.position() + optionLength);
            }
            addNonLifetime(lastNonLifetimeStart2, 0, 0);
            this.mMinLifetime = minLifetime(packet, length);
        }

        boolean matches(byte[] packet, int length) {
            if (length != this.mPacket.capacity()) {
                return false;
            }
            byte[] referencePacket = this.mPacket.array();
            for (Pair<Integer, Integer> nonLifetime : this.mNonLifetimes) {
                for (int i = ((Integer) nonLifetime.first).intValue(); i < ((Integer) nonLifetime.second).intValue() + ((Integer) nonLifetime.first).intValue(); i++) {
                    if (packet[i] != referencePacket[i]) {
                        return false;
                    }
                }
            }
            return true;
        }

        long minLifetime(byte[] packet, int length) {
            long val;
            long minLifetime = JobStatus.NO_LATEST_RUNTIME;
            ByteBuffer byteBuffer = ByteBuffer.wrap(packet);
            for (int i = 0; i + 1 < this.mNonLifetimes.size(); i++) {
                int offset = ((Integer) this.mNonLifetimes.get(i).first).intValue() + ((Integer) this.mNonLifetimes.get(i).second).intValue();
                if (offset != 56) {
                    int lifetimeLength = ((Integer) this.mNonLifetimes.get(i + 1).first).intValue() - offset;
                    switch (lifetimeLength) {
                        case 2:
                            val = byteBuffer.getShort(offset);
                            break;
                        case 3:
                        default:
                            throw new IllegalStateException("bogus lifetime size " + length);
                        case 4:
                            val = byteBuffer.getInt(offset);
                            break;
                    }
                    minLifetime = Math.min(minLifetime, val & ((1 << (lifetimeLength * 8)) - 1));
                }
            }
            return minLifetime;
        }

        long currentLifetime() {
            return this.mMinLifetime - (ApfFilter.curTime() - this.mLastSeen);
        }

        boolean isExpired() {
            return currentLifetime() <= 0;
        }

        @GuardedBy("ApfFilter.this")
        long generateFilterLocked(ApfGenerator gen) throws ApfGenerator.IllegalInstructionException {
            String nextFilterLabel = "Ra" + ApfFilter.this.getUniqueNumberLocked();
            gen.addLoadFromMemory(ApfGenerator.Register.R0, 14);
            gen.addJumpIfR0NotEquals(this.mPacket.capacity(), nextFilterLabel);
            int filterLifetime = (int) (currentLifetime() / 6);
            gen.addLoadFromMemory(ApfGenerator.Register.R0, 15);
            gen.addJumpIfR0GreaterThan(filterLifetime, nextFilterLabel);
            for (int i = 0; i < this.mNonLifetimes.size(); i++) {
                Pair<Integer, Integer> nonLifetime = this.mNonLifetimes.get(i);
                if (((Integer) nonLifetime.second).intValue() != 0) {
                    gen.addLoadImmediate(ApfGenerator.Register.R0, ((Integer) nonLifetime.first).intValue());
                    gen.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, Arrays.copyOfRange(this.mPacket.array(), ((Integer) nonLifetime.first).intValue(), ((Integer) nonLifetime.second).intValue() + ((Integer) nonLifetime.first).intValue()), nextFilterLabel);
                }
                if (i + 1 < this.mNonLifetimes.size()) {
                    Pair<Integer, Integer> nextNonLifetime = this.mNonLifetimes.get(i + 1);
                    int offset = ((Integer) nonLifetime.first).intValue() + ((Integer) nonLifetime.second).intValue();
                    if (offset == 56) {
                        continue;
                    } else {
                        int length = ((Integer) nextNonLifetime.first).intValue() - offset;
                        switch (length) {
                            case 2:
                                gen.addLoad16(ApfGenerator.Register.R0, offset);
                                break;
                            case 3:
                            default:
                                throw new IllegalStateException("bogus lifetime size " + length);
                            case 4:
                                gen.addLoad32(ApfGenerator.Register.R0, offset);
                                break;
                        }
                        gen.addJumpIfR0LessThan(filterLifetime, nextFilterLabel);
                    }
                }
            }
            gen.addJump(ApfGenerator.DROP_LABEL);
            gen.defineLabel(nextFilterLabel);
            return filterLifetime;
        }
    }

    @GuardedBy("this")
    private void generateArpFilterLocked(ApfGenerator gen) throws ApfGenerator.IllegalInstructionException {
        if (this.mIPv4Address != null) {
            gen.addLoadImmediate(ApfGenerator.Register.R0, ARP_HEADER_OFFSET);
            gen.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ARP_IPV4_REQUEST_HEADER, ApfGenerator.PASS_LABEL);
            gen.addLoadImmediate(ApfGenerator.Register.R0, ARP_TARGET_IP_ADDRESS_OFFSET);
            gen.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mIPv4Address, ApfGenerator.DROP_LABEL);
        }
        gen.addJump(ApfGenerator.PASS_LABEL);
    }

    @GuardedBy("this")
    private void generateIPv4FilterLocked(ApfGenerator gen) throws ApfGenerator.IllegalInstructionException {
        if (this.mMulticastFilter) {
            gen.addLoad8(ApfGenerator.Register.R0, 30);
            gen.addAnd(240);
            gen.addJumpIfR0Equals(224, ApfGenerator.DROP_LABEL);
            gen.addLoadImmediate(ApfGenerator.Register.R0, 0);
            gen.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ETH_BROADCAST_MAC_ADDRESS, ApfGenerator.PASS_LABEL);
            gen.addLoad8(ApfGenerator.Register.R0, 23);
            gen.addJumpIfR0NotEquals(OsConstants.IPPROTO_UDP, ApfGenerator.DROP_LABEL);
            gen.addLoad16(ApfGenerator.Register.R0, 20);
            gen.addJumpIfR0AnyBitsSet(IPV4_FRAGMENT_OFFSET_MASK, ApfGenerator.DROP_LABEL);
            gen.addLoadFromMemory(ApfGenerator.Register.R1, 13);
            gen.addLoad16Indexed(ApfGenerator.Register.R0, 16);
            gen.addJumpIfR0NotEquals(68, ApfGenerator.DROP_LABEL);
            gen.addLoadImmediate(ApfGenerator.Register.R0, 50);
            gen.addAddR1();
            gen.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, this.mHardwareAddress, ApfGenerator.DROP_LABEL);
        }
        gen.addJump(ApfGenerator.PASS_LABEL);
    }

    @GuardedBy("this")
    private void generateIPv6FilterLocked(ApfGenerator gen) throws ApfGenerator.IllegalInstructionException {
        gen.addLoad8(ApfGenerator.Register.R0, 20);
        if (this.mMulticastFilter) {
            gen.addJumpIfR0Equals(OsConstants.IPPROTO_ICMPV6, "skipIPv6MulticastFilter");
            gen.addLoad8(ApfGenerator.Register.R0, 38);
            gen.addJumpIfR0Equals(DhcpPacket.MAX_OPTION_LEN, ApfGenerator.DROP_LABEL);
            gen.addJump(ApfGenerator.PASS_LABEL);
            gen.defineLabel("skipIPv6MulticastFilter");
        } else {
            gen.addJumpIfR0NotEquals(OsConstants.IPPROTO_ICMPV6, ApfGenerator.PASS_LABEL);
        }
        gen.addLoad8(ApfGenerator.Register.R0, 54);
        gen.addJumpIfR0NotEquals(ICMP6_NEIGHBOR_ANNOUNCEMENT, "skipUnsolicitedMulticastNA");
        gen.addLoadImmediate(ApfGenerator.Register.R0, 38);
        gen.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, IPV6_ALL_NODES_ADDRESS, "skipUnsolicitedMulticastNA");
        gen.addJump(ApfGenerator.DROP_LABEL);
        gen.defineLabel("skipUnsolicitedMulticastNA");
    }

    @GuardedBy("this")
    private ApfGenerator beginProgramLocked() throws ApfGenerator.IllegalInstructionException {
        ApfGenerator gen = new ApfGenerator();
        gen.setApfVersion(this.mApfCapabilities.apfVersionSupported);
        gen.addLoad16(ApfGenerator.Register.R0, 12);
        gen.addJumpIfR0NotEquals(OsConstants.ETH_P_ARP, "skipArpFilters");
        generateArpFilterLocked(gen);
        gen.defineLabel("skipArpFilters");
        gen.addJumpIfR0NotEquals(OsConstants.ETH_P_IP, "skipIPv4Filters");
        generateIPv4FilterLocked(gen);
        gen.defineLabel("skipIPv4Filters");
        gen.addJumpIfR0Equals(OsConstants.ETH_P_IPV6, "IPv6Filters");
        gen.addLoadImmediate(ApfGenerator.Register.R0, 0);
        gen.addJumpIfBytesNotEqual(ApfGenerator.Register.R0, ETH_BROADCAST_MAC_ADDRESS, ApfGenerator.PASS_LABEL);
        gen.addJump(ApfGenerator.DROP_LABEL);
        gen.defineLabel("IPv6Filters");
        generateIPv6FilterLocked(gen);
        return gen;
    }

    @GuardedBy("this")
    void installNewProgramLocked() {
        purgeExpiredRasLocked();
        long programMinLifetime = JobStatus.NO_LATEST_RUNTIME;
        try {
            ApfGenerator gen = beginProgramLocked();
            ArrayList<Ra> rasToFilter = new ArrayList<>();
            for (Ra ra : this.mRas) {
                ra.generateFilterLocked(gen);
                if (gen.programLengthOverEstimate() > this.mApfCapabilities.maximumApfProgramSize) {
                    break;
                } else {
                    rasToFilter.add(ra);
                }
            }
            ApfGenerator gen2 = beginProgramLocked();
            Iterator ra$iterator = rasToFilter.iterator();
            while (ra$iterator.hasNext()) {
                programMinLifetime = Math.min(programMinLifetime, ((Ra) ra$iterator.next()).generateFilterLocked(gen2));
            }
            byte[] program = gen2.generate();
            this.mLastTimeInstalledProgram = curTime();
            this.mLastInstalledProgramMinLifetime = programMinLifetime;
            this.mLastInstalledProgram = program;
            this.mNumProgramUpdates++;
            hexDump("Installing filter: ", program, program.length);
            this.mIpManagerCallback.installPacketFilter(program);
        } catch (ApfGenerator.IllegalInstructionException e) {
            Log.e(TAG, "Program failed to generate: ", e);
        }
    }

    @GuardedBy("this")
    private void maybeInstallNewProgramLocked() {
        if (this.mRas.size() == 0) {
            return;
        }
        long expiry = this.mLastTimeInstalledProgram + this.mLastInstalledProgramMinLifetime;
        if (expiry >= curTime() + MAX_PROGRAM_LIFETIME_WORTH_REFRESHING) {
            return;
        }
        installNewProgramLocked();
    }

    private void hexDump(String msg, byte[] packet, int length) {
        log(msg + HexDump.toHexString(packet, 0, length, false));
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

    private synchronized void processRa(byte[] packet, int length) {
        hexDump("Read packet = ", packet, length);
        for (int i = 0; i < this.mRas.size(); i++) {
            Ra ra = this.mRas.get(i);
            if (ra.matches(packet, length)) {
                log("matched RA " + ra);
                ra.mLastSeen = curTime();
                ra.mMinLifetime = ra.minLifetime(packet, length);
                ra.seenCount++;
                this.mRas.add(0, this.mRas.remove(i));
                maybeInstallNewProgramLocked();
                return;
            }
        }
        purgeExpiredRasLocked();
        if (this.mRas.size() >= 10) {
            return;
        }
        try {
            Ra ra2 = new Ra(packet, length);
            if (ra2.isExpired()) {
                return;
            }
            log("Adding " + ra2);
            this.mRas.add(ra2);
            installNewProgramLocked();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing RA: " + e);
        }
    }

    public static ApfFilter maybeCreate(ApfCapabilities apfCapabilities, NetworkInterface networkInterface, IpManager.Callback ipManagerCallback, boolean multicastFilter) {
        if (apfCapabilities == null || networkInterface == null || apfCapabilities.apfVersionSupported == 0) {
            return null;
        }
        if (apfCapabilities.maximumApfProgramSize < 512) {
            Log.e(TAG, "Unacceptably small APF limit: " + apfCapabilities.maximumApfProgramSize);
            return null;
        }
        if (apfCapabilities.apfPacketFormat != OsConstants.ARPHRD_ETHER) {
            return null;
        }
        if (!new ApfGenerator().setApfVersion(apfCapabilities.apfVersionSupported)) {
            Log.e(TAG, "Unsupported APF version: " + apfCapabilities.apfVersionSupported);
            return null;
        }
        return new ApfFilter(apfCapabilities, networkInterface, ipManagerCallback, multicastFilter);
    }

    public synchronized void shutdown() {
        if (this.mReceiveThread != null) {
            log("shutting down");
            this.mReceiveThread.halt();
            this.mReceiveThread = null;
        }
        this.mRas.clear();
    }

    public synchronized void setMulticastFilter(boolean enabled) {
        if (this.mMulticastFilter != enabled) {
            this.mMulticastFilter = enabled;
            installNewProgramLocked();
        }
    }

    private static byte[] findIPv4Address(LinkProperties lp) {
        byte[] ipv4Address = null;
        for (InetAddress inetAddr : lp.getAddresses()) {
            byte[] addr = inetAddr.getAddress();
            if (addr.length == 4) {
                if (ipv4Address != null && !Arrays.equals(ipv4Address, addr)) {
                    return null;
                }
                ipv4Address = addr;
            }
        }
        return ipv4Address;
    }

    public synchronized void setLinkProperties(LinkProperties lp) {
        byte[] ipv4Address = findIPv4Address(lp);
        if (Arrays.equals(ipv4Address, this.mIPv4Address)) {
            return;
        }
        this.mIPv4Address = ipv4Address;
        installNewProgramLocked();
    }

    public synchronized void dump(IndentingPrintWriter pw) {
        pw.println("Capabilities: " + this.mApfCapabilities);
        pw.println("Receive thread: " + (this.mReceiveThread != null ? "RUNNING" : "STOPPED"));
        pw.println("Multicast: " + (this.mMulticastFilter ? "DROP" : "ALLOW"));
        try {
            pw.println("IPv4 address: " + InetAddress.getByAddress(this.mIPv4Address).getHostAddress());
        } catch (NullPointerException | UnknownHostException e) {
        }
        if (this.mLastTimeInstalledProgram == 0) {
            pw.println("No program installed.");
            return;
        }
        pw.println("Program updates: " + this.mNumProgramUpdates);
        pw.println(String.format("Last program length %d, installed %ds ago, lifetime %ds", Integer.valueOf(this.mLastInstalledProgram.length), Long.valueOf(curTime() - this.mLastTimeInstalledProgram), Long.valueOf(this.mLastInstalledProgramMinLifetime)));
        pw.println("RA filters:");
        pw.increaseIndent();
        for (Ra ra : this.mRas) {
            pw.println(ra);
            pw.increaseIndent();
            pw.println(String.format("Seen: %d, last %ds ago", Integer.valueOf(ra.seenCount), Long.valueOf(curTime() - ra.mLastSeen)));
            pw.println("Last match:");
            pw.increaseIndent();
            pw.println(ra.getLastMatchingPacket());
            pw.decreaseIndent();
            pw.decreaseIndent();
        }
        pw.decreaseIndent();
        pw.println("Last program:");
        pw.increaseIndent();
        pw.println(HexDump.toHexString(this.mLastInstalledProgram, false));
        pw.decreaseIndent();
    }
}
