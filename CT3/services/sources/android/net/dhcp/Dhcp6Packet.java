package android.net.dhcp;

import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.util.Log;
import com.android.internal.util.HexDump;
import com.android.server.wm.WindowManagerService;
import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

abstract class Dhcp6Packet {
    protected static final short CLIENT_ID_ETHER = 3;
    static final short DHCP_CLIENT = 546;
    protected static final byte DHCP_MESSAGE_TYPE = 53;
    protected static final byte DHCP_MESSAGE_TYPE_ADVERTISE = 2;
    protected static final byte DHCP_MESSAGE_TYPE_CONFIRM = 4;
    protected static final byte DHCP_MESSAGE_TYPE_DECLINE = 9;
    protected static final byte DHCP_MESSAGE_TYPE_INFO_REQUEST = 11;
    protected static final byte DHCP_MESSAGE_TYPE_REBIND = 6;
    protected static final byte DHCP_MESSAGE_TYPE_RELEASE = 8;
    protected static final byte DHCP_MESSAGE_TYPE_RENEW = 5;
    protected static final byte DHCP_MESSAGE_TYPE_REPLY = 7;
    protected static final byte DHCP_MESSAGE_TYPE_REQUEST = 3;
    protected static final byte DHCP_MESSAGE_TYPE_SOLICIT = 1;
    protected static final short DHCP_OPTION_END = 255;
    protected static final short DHCP_OPTION_PAD = 0;
    static final short DHCP_SERVER = 547;
    protected static final byte DUID_EN_TYPE = 2;
    protected static final byte DUID_LLT_TYPE = 1;
    protected static final byte DUID_LL_TYPE = 3;
    public static final int HWADDR_LEN = 16;
    public static final int INFINITE_LEASE = -1;
    private static final byte IPV6_HOT_LIMIT = 1;
    private static final byte IP_TYPE_UDP = 17;
    protected static final int MAX_LENGTH = 1500;
    public static final int MAX_OPTION_LEN = 65025;
    public static final int MINIMUM_LEASE = 60;
    public static final int MIN_PACKET_LENGTH_L2 = 54;
    public static final int MIN_PACKET_LENGTH_L3 = 40;
    protected static final short OPTION_CLIENTID = 1;
    protected static final short OPTION_DNS_SERVERS = 23;
    protected static final short OPTION_DOMAIN_LIST = 24;
    protected static final short OPTION_ELAPSED_TIME = 8;
    protected static final short OPTION_IAADDR = 5;
    protected static final short OPTION_IA_NA = 3;
    protected static final short OPTION_IA_TA = 4;
    protected static final short OPTION_ORO = 6;
    protected static final short OPTION_PREFERENCE = 7;
    protected static final short OPTION_SERVERID = 2;
    protected static final String TAG = "Dhcp6Packet";
    protected final byte[] mClientMac;
    protected List<Inet6Address> mDnsServers;
    protected String mDomainName;
    protected Inet6Address mGateway;
    protected byte[] mIana;
    protected Integer mLeaseTime;
    protected Short mMtu;
    private final Inet6Address mNextIp;
    private final Inet6Address mRelayIp;
    protected Inet6Address mRequestedIp;
    protected short[] mRequestedParams;
    protected Inet6Address mServerAddress;
    protected byte[] mServerIdentifier;
    protected final Inet6Address mServerIp;
    protected Inet6Address mSubnetMask;
    protected Integer mT1;
    protected Integer mT2;
    protected final byte[] mTransId;
    protected String mVendorId;
    protected static final Boolean DBG = true;
    public static final Inet6Address INADDR_ANY = (Inet6Address) Inet6Address.ANY;
    public static final Inet6Address INADDR_BROADCAST_ROUTER = (Inet6Address) NetworkUtils.hexToInet6Address("FF020000000000000000000000010002");
    public static final byte[] ETHER_BROADCAST = {-1, -1, -1, -1, -1, -1};
    private static final byte[] IPV6_VERSION_HEADER = {96, 0, 0, 0};

    public abstract ByteBuffer buildPacket(short s, short s2);

    abstract void finishPacket(ByteBuffer byteBuffer);

    protected Dhcp6Packet(byte[] transId, Inet6Address sourceIP, Inet6Address nextIp, Inet6Address relayIp, byte[] clientMac) {
        this.mTransId = transId;
        this.mServerIp = sourceIP;
        this.mNextIp = nextIp;
        this.mRelayIp = relayIp;
        this.mClientMac = clientMac;
    }

    public byte[] getTransactionId() {
        return this.mTransId;
    }

    public byte[] getClientMac() {
        if (this.mClientMac != null) {
            return this.mClientMac;
        }
        return null;
    }

    public byte[] getClientId() {
        ByteBuffer buffer = ByteBuffer.allocate(14);
        buffer.clear();
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort((short) 1);
        buffer.putShort((short) 1);
        buffer.put(Dhcp6Client.getTimeStamp());
        buffer.put(this.mClientMac);
        return buffer.array();
    }

    private byte[] getIaNa() {
        byte[] data = {14, 0, DHCP_MESSAGE_TYPE_RELEASE, -54, 0, 0, 0, 0, 0, 0, 0, 0};
        return data;
    }

    protected void fillInPacket(Inet6Address destIp, Inet6Address srcIp, short destUdp, short srcUdp, ByteBuffer buf, byte requestCode) {
        destIp.getAddress();
        srcIp.getAddress();
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put(requestCode);
        buf.put(this.mTransId);
        finishPacket(buf);
        if (!DBG.booleanValue()) {
            return;
        }
        Log.d(TAG, HexDump.toHexString(buf.array()));
    }

    private static int intAbs(short v) {
        return 65535 & v;
    }

    private int checksum(ByteBuffer buf, int seed, int start, int end) {
        int sum = seed;
        int bufPosition = buf.position();
        buf.position(start);
        ShortBuffer shortBuf = buf.asShortBuffer();
        buf.position(bufPosition);
        short[] shortArray = new short[(end - start) / 2];
        shortBuf.get(shortArray);
        for (short s : shortArray) {
            sum += intAbs(s);
        }
        int start2 = start + (shortArray.length * 2);
        if (end != start2) {
            short b = buf.get(start2);
            if (b < 0) {
                b = (short) (b + 256);
            }
            sum += b * 256;
        }
        int sum2 = ((sum >> 16) & 65535) + (sum & 65535);
        int negated = ~((((sum2 >> 16) & 65535) + sum2) & 65535);
        return intAbs((short) negated);
    }

    protected static void addTlv(ByteBuffer buf, short type, byte value) {
        buf.putShort(type);
        buf.putShort((short) 1);
        buf.put(value);
    }

    protected static void addTlv(ByteBuffer buf, short type, byte[] payload) {
        if (payload == null) {
            return;
        }
        if (payload.length > 65025) {
            throw new IllegalArgumentException("DHCP option too long: " + payload.length + " vs. " + MAX_OPTION_LEN);
        }
        buf.putShort(type);
        buf.putShort((short) payload.length);
        buf.put(payload);
    }

    protected static void addTlv(ByteBuffer buf, short type, short[] payload) {
        if (payload == null) {
            return;
        }
        if (payload.length > 65025) {
            throw new IllegalArgumentException("DHCP option too long: " + payload.length + " vs. " + MAX_OPTION_LEN);
        }
        byte[] rawBtyes = new byte[payload.length * 2];
        ByteBuffer.wrap(rawBtyes).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(payload);
        addTlv(buf, type, rawBtyes);
    }

    protected static void addTlv(ByteBuffer buf, short type, Inet6Address addr) {
        if (addr == null) {
            return;
        }
        addTlv(buf, type, addr.getAddress());
    }

    protected static void addTlv(ByteBuffer buf, short type, List<Inet6Address> addrs) {
        if (addrs == null || addrs.size() == 0) {
            return;
        }
        int optionLen = addrs.size() * 4;
        if (optionLen > 65025) {
            throw new IllegalArgumentException("DHCP option too long: " + optionLen + " vs. " + MAX_OPTION_LEN);
        }
        buf.putShort(type);
        buf.put((byte) optionLen);
        for (Inet6Address addr : addrs) {
            buf.put(addr.getAddress());
        }
    }

    protected static void addTlv(ByteBuffer buf, short type, Short value) {
        if (value == null) {
            return;
        }
        buf.putShort(type);
        buf.putShort((short) 2);
        buf.putShort(value.shortValue());
    }

    protected static void addTlv(ByteBuffer buf, short type, Integer value) {
        if (value == null) {
            return;
        }
        buf.putShort(type);
        buf.putShort((short) 4);
        buf.putInt(value.intValue());
    }

    protected static void addTlv(ByteBuffer buf, short type, String str) {
        try {
            addTlv(buf, type, str.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("String is not US-ASCII: " + str);
        }
    }

    protected void addCommonClientTlvs(ByteBuffer buf) {
        addTlv(buf, (short) 3, getIaNa());
        addTlv(buf, (short) 8, (Short) 0);
    }

    public static String macToString(byte[] mac) {
        String macAddr = "";
        if (mac == null) {
            return "";
        }
        for (int i = 0; i < mac.length; i++) {
            String hexString = "0" + Integer.toHexString(mac[i]);
            macAddr = macAddr + hexString.substring(hexString.length() - 2);
            if (i != mac.length - 1) {
                macAddr = macAddr + ":";
            }
        }
        return macAddr;
    }

    public String toString() {
        String macAddr = macToString(this.mClientMac);
        return macAddr;
    }

    private static Inet6Address readIpAddress(ByteBuffer packet) {
        Inet6Address result;
        byte[] ipAddr = new byte[16];
        packet.get(ipAddr);
        try {
            result = (Inet6Address) Inet6Address.getByAddress(ipAddr);
        } catch (UnknownHostException e) {
            result = null;
        }
        Log.i(TAG, "readIpAddress:" + result);
        return result;
    }

    private static String readAsciiString(ByteBuffer buf, int byteCount, boolean nullOk) {
        byte[] bytes = new byte[byteCount];
        buf.get(bytes);
        int length = bytes.length;
        if (!nullOk) {
            length = 0;
            while (length < bytes.length && bytes[length] != 0) {
                length++;
            }
        }
        return new String(bytes, 0, length, StandardCharsets.US_ASCII);
    }

    public static Dhcp6Packet decodeFullPacket(ByteBuffer packet) {
        Dhcp6Packet newPacket;
        byte[] clientMac = null;
        List<Inet6Address> dnsServers = new ArrayList<>();
        byte[] serverIdentifier = null;
        Inet6Address requestedAddress = null;
        Integer T1 = null;
        Integer T2 = null;
        packet.order(ByteOrder.BIG_ENDIAN);
        byte dhcpType = packet.get();
        byte[] transactionId = new byte[3];
        packet.get(transactionId);
        boolean notFinishedOptions = true;
        while (packet.position() < packet.limit() && notFinishedOptions) {
            try {
                short optionType = packet.getShort();
                Log.d(TAG, "optionType:" + ((int) optionType));
                if (optionType == 255) {
                    notFinishedOptions = false;
                } else if (optionType != 0) {
                    int optionLen = packet.getShort() & 65535;
                    int expectedLen = 0;
                    switch (optionType) {
                        case 1:
                            byte[] id = new byte[optionLen];
                            packet.get(id);
                            expectedLen = optionLen;
                            ByteBuffer buf = ByteBuffer.wrap(id);
                            short duidType = buf.getShort();
                            if (duidType == 1 || duidType == 3) {
                                short hwType = buf.getShort();
                                if (hwType == 1) {
                                    if (duidType == 1) {
                                        buf.getInt();
                                    }
                                    clientMac = new byte[6];
                                    buf.get(clientMac);
                                }
                            }
                            break;
                        case 2:
                            serverIdentifier = new byte[optionLen];
                            packet.get(serverIdentifier);
                            expectedLen = optionLen;
                            break;
                        case 3:
                            byte[] iana = new byte[optionLen];
                            packet.get(iana);
                            ByteBuffer buf2 = ByteBuffer.wrap(iana);
                            T1 = Integer.valueOf(buf2.getInt(4));
                            T2 = Integer.valueOf(buf2.getInt(8));
                            Log.d(TAG, "T1:" + T1);
                            Log.d(TAG, "T2:" + T2);
                            if (optionLen > 12) {
                                buf2.position(12);
                                short iaAddress = buf2.getShort();
                                if (iaAddress == 5) {
                                    int i = buf2.getShort() & 65535;
                                    requestedAddress = readIpAddress(buf2);
                                    expectedLen = optionLen;
                                }
                            }
                            break;
                        case WindowManagerService.H.BOOT_TIMEOUT:
                            expectedLen = 0;
                            while (expectedLen < optionLen) {
                                dnsServers.add(readIpAddress(packet));
                                expectedLen += 16;
                            }
                            break;
                        default:
                            for (int i2 = 0; i2 < optionLen; i2++) {
                                expectedLen++;
                                packet.get();
                            }
                            break;
                    }
                    Log.d(TAG, "expectedLen:" + expectedLen);
                    Log.d(TAG, "optionLen:" + optionLen);
                    if (expectedLen != optionLen) {
                        Log.e(TAG, "optionType:" + ((int) optionType));
                        return null;
                    }
                } else {
                    continue;
                }
            } catch (BufferUnderflowException e) {
                e.printStackTrace();
                return null;
            } catch (Exception ee) {
                ee.printStackTrace();
                return null;
            }
        }
        switch (dhcpType) {
            case -1:
                return null;
            case 2:
                newPacket = new Dhcp6AdvertisePacket(transactionId, null, requestedAddress, clientMac);
                break;
            case 7:
                newPacket = new Dhcp6ReplyPacket(transactionId, null, requestedAddress, clientMac);
                break;
            default:
                Log.e(TAG, "Unimplemented type: " + ((int) dhcpType));
                return null;
        }
        newPacket.mRequestedIp = requestedAddress;
        newPacket.mDnsServers = dnsServers;
        newPacket.mServerIdentifier = serverIdentifier;
        newPacket.mT1 = T1;
        newPacket.mT2 = T2;
        newPacket.mLeaseTime = T1;
        return newPacket;
    }

    public static Dhcp6Packet decodeFullPacket(byte[] packet, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN);
        return decodeFullPacket(buffer);
    }

    public DhcpResults toDhcpResults() {
        Inet6Address ipAddress = this.mRequestedIp;
        if (ipAddress == null || ipAddress.equals(Inet6Address.ANY)) {
            return null;
        }
        DhcpResults results = new DhcpResults();
        try {
            results.ipAddress = new LinkAddress(ipAddress, 64);
            results.gateway = this.mGateway;
            results.dnsServers.addAll(this.mDnsServers);
            results.domains = this.mDomainName;
            results.serverAddress = null;
            results.leaseDuration = this.mLeaseTime != null ? this.mLeaseTime.intValue() : -1;
            return results;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public long getLeaseTimeMillis() {
        if (this.mLeaseTime == null || this.mLeaseTime.intValue() == -1) {
            return 0L;
        }
        if (this.mLeaseTime.intValue() >= 0 && this.mLeaseTime.intValue() < 60) {
            return 60000L;
        }
        return (((long) this.mLeaseTime.intValue()) & 4294967295L) * 1000;
    }

    public static ByteBuffer buildSolicitPacket(byte[] transactionId, short secs, byte[] clientMac, short[] expectedParams) {
        Dhcp6Packet pkt = new Dhcp6SolicitPacket(transactionId, clientMac);
        pkt.mRequestedParams = expectedParams;
        return pkt.buildPacket(DHCP_SERVER, DHCP_CLIENT);
    }

    public static ByteBuffer buildRequestPacket(byte[] transactionId, short secs, Inet6Address clientIp, byte[] clientMac, Inet6Address requestedIpAddress, byte[] serverIdentifier, short[] requestedParams) {
        Dhcp6Packet pkt = new Dhcp6RequestPacket(transactionId, clientMac);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        ByteBuffer result = pkt.buildPacket(DHCP_SERVER, DHCP_CLIENT);
        return result;
    }

    public static ByteBuffer buildInfoRequestPacket(byte[] transactionId, short secs, byte[] clientMac, short[] expectedParams) {
        Dhcp6Packet pkt = new Dhcp6InfoRequestPacket(transactionId, clientMac);
        pkt.mRequestedParams = expectedParams;
        ByteBuffer result = pkt.buildPacket(DHCP_SERVER, DHCP_CLIENT);
        return result;
    }

    public static ByteBuffer buildRenewPacket(byte[] transactionId, short secs, Inet6Address clientIp, boolean broadcast, byte[] clientMac, Inet6Address requestedIpAddress, byte[] serverIdentifier, byte[] requestedParams) {
        Dhcp6Packet pkt = new Dhcp6RenewPacket(transactionId, clientMac);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        ByteBuffer result = pkt.buildPacket(DHCP_SERVER, DHCP_CLIENT);
        return result;
    }

    public static ByteBuffer buildRebindPacket(byte[] transactionId, short secs, Inet6Address clientIp, boolean broadcast, byte[] clientMac, Inet6Address requestedIpAddress, byte[] serverIdentifier, byte[] requestedParams) {
        Dhcp6Packet pkt = new Dhcp6RebindPacket(transactionId, clientMac);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        ByteBuffer result = pkt.buildPacket(DHCP_SERVER, DHCP_CLIENT);
        return result;
    }
}
