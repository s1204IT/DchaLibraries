package android.net.dhcp;

import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.metrics.DhcpErrorEvent;
import android.os.Build;
import android.os.SystemProperties;
import android.system.OsConstants;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.wm.WindowManagerService;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class DhcpPacket {
    protected static final byte CLIENT_ID_ETHER = 1;
    protected static final byte DHCP_BOOTREPLY = 2;
    protected static final byte DHCP_BOOTREQUEST = 1;
    protected static final byte DHCP_BROADCAST_ADDRESS = 28;
    static final short DHCP_CLIENT = 68;
    protected static final byte DHCP_CLIENT_IDENTIFIER = 61;
    protected static final byte DHCP_DNS_SERVER = 6;
    protected static final byte DHCP_DOMAIN_NAME = 15;
    protected static final byte DHCP_HOST_NAME = 12;
    protected static final byte DHCP_LEASE_TIME = 51;
    private static final int DHCP_MAGIC_COOKIE = 1669485411;
    protected static final byte DHCP_MAX_MESSAGE_SIZE = 57;
    protected static final byte DHCP_MESSAGE = 56;
    protected static final byte DHCP_MESSAGE_TYPE = 53;
    protected static final byte DHCP_MESSAGE_TYPE_ACK = 5;
    protected static final byte DHCP_MESSAGE_TYPE_DECLINE = 4;
    protected static final byte DHCP_MESSAGE_TYPE_DISCOVER = 1;
    protected static final byte DHCP_MESSAGE_TYPE_INFORM = 8;
    protected static final byte DHCP_MESSAGE_TYPE_NAK = 6;
    protected static final byte DHCP_MESSAGE_TYPE_OFFER = 2;
    protected static final byte DHCP_MESSAGE_TYPE_REQUEST = 3;
    protected static final byte DHCP_MTU = 26;
    protected static final byte DHCP_OPTION_PAD = 0;
    protected static final byte DHCP_PARAMETER_LIST = 55;
    protected static final byte DHCP_REBINDING_TIME = 59;
    protected static final byte DHCP_RENEWAL_TIME = 58;
    protected static final byte DHCP_REQUESTED_IP = 50;
    protected static final byte DHCP_ROUTER = 3;
    static final short DHCP_SERVER = 67;
    protected static final byte DHCP_SERVER_IDENTIFIER = 54;
    protected static final byte DHCP_STATIC_ROUTE = 33;
    protected static final byte DHCP_SUBNET_MASK = 1;
    protected static final byte DHCP_VENDOR_CLASS_ID = 60;
    protected static final byte DHCP_VENDOR_INFO = 43;
    public static final int ENCAP_BOOTP = 2;
    public static final int ENCAP_L2 = 0;
    public static final int ENCAP_L3 = 1;
    public static final int HWADDR_LEN = 16;
    public static final int INFINITE_LEASE = -1;
    private static final short IP_FLAGS_OFFSET = 16384;
    private static final byte IP_TOS_LOWDELAY = 16;
    private static final byte IP_TTL = 64;
    private static final byte IP_TYPE_UDP = 17;
    private static final byte IP_VERSION_HEADER_LEN = 69;
    protected static final int MAX_LENGTH = 1500;
    private static final int MAX_MTU = 1500;
    public static final int MAX_OPTION_LEN = 255;
    public static final int MINIMUM_LEASE = 60;
    private static final int MIN_MTU = 1280;
    public static final int MIN_PACKET_LENGTH_BOOTP = 236;
    public static final int MIN_PACKET_LENGTH_L2 = 278;
    public static final int MIN_PACKET_LENGTH_L3 = 264;
    protected static final String TAG = "DhcpPacket";
    protected boolean mBroadcast;
    protected Inet4Address mBroadcastAddress;
    protected final Inet4Address mClientIp;
    protected final byte[] mClientMac;
    protected List<Inet4Address> mDnsServers;
    protected String mDomainName;
    protected List<Inet4Address> mGateways;
    protected String mHostName;
    protected Integer mLeaseTime;
    protected Short mMaxMessageSize;
    protected String mMessage;
    protected Short mMtu;
    private final Inet4Address mNextIp;
    private final Inet4Address mRelayIp;
    protected Inet4Address mRequestedIp;
    protected byte[] mRequestedParams;
    protected final short mSecs;
    protected Inet4Address mServerIdentifier;
    protected Inet4Address mStaticGateway;
    protected Inet4Address mSubnetMask;
    protected Integer mT1;
    protected Integer mT2;
    protected final int mTransId;
    protected String mVendorId;
    protected String mVendorInfo;
    protected final Inet4Address mYourIp;
    public static final Inet4Address INADDR_ANY = (Inet4Address) Inet4Address.ANY;
    public static final Inet4Address INADDR_BROADCAST = (Inet4Address) Inet4Address.ALL;
    protected static final byte DHCP_OPTION_END = -1;
    public static final byte[] ETHER_BROADCAST = {DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END, DHCP_OPTION_END};
    static String testOverrideVendorId = null;
    static String testOverrideHostname = null;

    public abstract ByteBuffer buildPacket(int i, short s, short s2);

    abstract void finishPacket(ByteBuffer byteBuffer);

    protected DhcpPacket(int transId, short secs, Inet4Address clientIp, Inet4Address yourIp, Inet4Address nextIp, Inet4Address relayIp, byte[] clientMac, boolean broadcast) {
        this.mTransId = transId;
        this.mSecs = secs;
        this.mClientIp = clientIp;
        this.mYourIp = yourIp;
        this.mNextIp = nextIp;
        this.mRelayIp = relayIp;
        this.mClientMac = clientMac;
        this.mBroadcast = broadcast;
    }

    public int getTransactionId() {
        return this.mTransId;
    }

    public byte[] getClientMac() {
        return this.mClientMac;
    }

    public byte[] getClientId() {
        byte[] clientId = new byte[this.mClientMac.length + 1];
        clientId[0] = 1;
        System.arraycopy(this.mClientMac, 0, clientId, 1, this.mClientMac.length);
        return clientId;
    }

    protected void fillInPacket(int encap, Inet4Address destIp, Inet4Address srcIp, short destUdp, short srcUdp, ByteBuffer buf, byte requestCode, boolean broadcast) {
        byte[] destIpArray = destIp.getAddress();
        byte[] srcIpArray = srcIp.getAddress();
        int ipHeaderOffset = 0;
        int ipLengthOffset = 0;
        int ipChecksumOffset = 0;
        int endIpHeader = 0;
        int udpHeaderOffset = 0;
        int udpLengthOffset = 0;
        int udpChecksumOffset = 0;
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        if (encap == 0) {
            buf.put(ETHER_BROADCAST);
            buf.put(this.mClientMac);
            buf.putShort((short) OsConstants.ETH_P_IP);
        }
        if (encap <= 1) {
            ipHeaderOffset = buf.position();
            buf.put(IP_VERSION_HEADER_LEN);
            buf.put(IP_TOS_LOWDELAY);
            ipLengthOffset = buf.position();
            buf.putShort((short) 0);
            buf.putShort((short) 0);
            buf.putShort(IP_FLAGS_OFFSET);
            buf.put(IP_TTL);
            buf.put(IP_TYPE_UDP);
            ipChecksumOffset = buf.position();
            buf.putShort((short) 0);
            buf.put(srcIpArray);
            buf.put(destIpArray);
            endIpHeader = buf.position();
            udpHeaderOffset = buf.position();
            buf.putShort(srcUdp);
            buf.putShort(destUdp);
            udpLengthOffset = buf.position();
            buf.putShort((short) 0);
            udpChecksumOffset = buf.position();
            buf.putShort((short) 0);
        }
        buf.put(requestCode);
        buf.put((byte) 1);
        buf.put((byte) this.mClientMac.length);
        buf.put(DHCP_OPTION_PAD);
        buf.putInt(this.mTransId);
        buf.putShort(this.mSecs);
        if (broadcast) {
            buf.putShort(Short.MIN_VALUE);
        } else {
            buf.putShort((short) 0);
        }
        buf.put(this.mClientIp.getAddress());
        buf.put(this.mYourIp.getAddress());
        buf.put(this.mNextIp.getAddress());
        buf.put(this.mRelayIp.getAddress());
        buf.put(this.mClientMac);
        buf.position(buf.position() + (16 - this.mClientMac.length) + 64 + 128);
        buf.putInt(DHCP_MAGIC_COOKIE);
        finishPacket(buf);
        if ((buf.position() & 1) == 1) {
            buf.put(DHCP_OPTION_PAD);
        }
        if (encap > 1) {
            return;
        }
        short udpLen = (short) (buf.position() - udpHeaderOffset);
        buf.putShort(udpLengthOffset, udpLen);
        int udpSeed = intAbs(buf.getShort(ipChecksumOffset + 2)) + 0;
        buf.putShort(udpChecksumOffset, (short) checksum(buf, udpSeed + intAbs(buf.getShort(ipChecksumOffset + 4)) + intAbs(buf.getShort(ipChecksumOffset + 6)) + intAbs(buf.getShort(ipChecksumOffset + 8)) + 17 + udpLen, udpHeaderOffset, buf.position()));
        buf.putShort(ipLengthOffset, (short) (buf.position() - ipHeaderOffset));
        buf.putShort(ipChecksumOffset, (short) checksum(buf, 0, ipHeaderOffset, endIpHeader));
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

    protected static void addTlv(ByteBuffer buf, byte type, byte value) {
        buf.put(type);
        buf.put((byte) 1);
        buf.put(value);
    }

    protected static void addTlv(ByteBuffer buf, byte type, byte[] payload) {
        if (payload == null) {
            return;
        }
        if (payload.length > 255) {
            throw new IllegalArgumentException("DHCP option too long: " + payload.length + " vs. " + MAX_OPTION_LEN);
        }
        buf.put(type);
        buf.put((byte) payload.length);
        buf.put(payload);
    }

    protected static void addTlv(ByteBuffer buf, byte type, Inet4Address addr) {
        if (addr == null) {
            return;
        }
        addTlv(buf, type, addr.getAddress());
    }

    protected static void addTlv(ByteBuffer buf, byte type, List<Inet4Address> addrs) {
        if (addrs == null || addrs.size() == 0) {
            return;
        }
        int optionLen = addrs.size() * 4;
        if (optionLen > 255) {
            throw new IllegalArgumentException("DHCP option too long: " + optionLen + " vs. " + MAX_OPTION_LEN);
        }
        buf.put(type);
        buf.put((byte) optionLen);
        for (Inet4Address addr : addrs) {
            buf.put(addr.getAddress());
        }
    }

    protected static void addTlv(ByteBuffer buf, byte type, Short value) {
        if (value == null) {
            return;
        }
        buf.put(type);
        buf.put((byte) 2);
        buf.putShort(value.shortValue());
    }

    protected static void addTlv(ByteBuffer buf, byte type, Integer value) {
        if (value == null) {
            return;
        }
        buf.put(type);
        buf.put(DHCP_MESSAGE_TYPE_DECLINE);
        buf.putInt(value.intValue());
    }

    protected static void addTlv(ByteBuffer buf, byte type, String str) {
        try {
            addTlv(buf, type, str.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("String is not US-ASCII: " + str);
        }
    }

    protected static void addTlvEnd(ByteBuffer buf) {
        buf.put(DHCP_OPTION_END);
    }

    private String getVendorId() {
        return testOverrideVendorId != null ? testOverrideVendorId : "android-dhcp-" + Build.VERSION.RELEASE;
    }

    private String getHostname() {
        return testOverrideHostname != null ? testOverrideHostname : SystemProperties.get("net.hostname");
    }

    protected void addCommonClientTlvs(ByteBuffer buf) {
        addTlv(buf, DHCP_MAX_MESSAGE_SIZE, (Short) 1500);
        addTlv(buf, DHCP_VENDOR_CLASS_ID, getVendorId());
        addTlv(buf, DHCP_HOST_NAME, getHostname());
    }

    public static String macToString(byte[] mac) {
        String macAddr = "";
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

    private static Inet4Address readIpAddress(ByteBuffer packet) {
        byte[] ipAddr = new byte[4];
        packet.get(ipAddr);
        try {
            Inet4Address result = (Inet4Address) Inet4Address.getByAddress(ipAddr);
            return result;
        } catch (UnknownHostException e) {
            return null;
        }
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

    private static boolean isPacketToOrFromClient(short udpSrcPort, short udpDstPort) {
        return udpSrcPort == 68 || udpDstPort == 68;
    }

    private static boolean isPacketServerToServer(short udpSrcPort, short udpDstPort) {
        return udpSrcPort == 67 && udpDstPort == 67;
    }

    public static class ParseException extends Exception {
        public final int errorCode;

        public ParseException(int errorCode, String msg, Object... args) {
            super(String.format(msg, args));
            this.errorCode = errorCode;
        }
    }

    public static DhcpPacket decodeFullPacket(ByteBuffer packet, int pktType) throws ParseException {
        DhcpPacket newPacket;
        List<Inet4Address> dnsServers = new ArrayList<>();
        List<Inet4Address> gateways = new ArrayList<>();
        Inet4Address serverIdentifier = null;
        Inet4Address netMask = null;
        String message = null;
        String vendorId = null;
        String vendorInfo = null;
        byte[] expectedParams = null;
        String hostName = null;
        String domainName = null;
        Inet4Address ipSrc = null;
        Inet4Address bcAddr = null;
        Inet4Address requestedIp = null;
        Short mtu = null;
        Short maxMessageSize = null;
        Integer leaseTime = null;
        Integer T1 = null;
        Integer T2 = null;
        byte dhcpType = DHCP_OPTION_END;
        packet.order(ByteOrder.BIG_ENDIAN);
        if (pktType == 0) {
            if (packet.remaining() < 278) {
                throw new ParseException(DhcpErrorEvent.L2_TOO_SHORT, "L2 packet too short, %d < %d", Integer.valueOf(packet.remaining()), Integer.valueOf(MIN_PACKET_LENGTH_L2));
            }
            byte[] l2dst = new byte[6];
            byte[] l2src = new byte[6];
            packet.get(l2dst);
            packet.get(l2src);
            short l2type = packet.getShort();
            if (l2type != OsConstants.ETH_P_IP) {
                throw new ParseException(DhcpErrorEvent.L2_WRONG_ETH_TYPE, "Unexpected L2 type 0x%04x, expected 0x%04x", Short.valueOf(l2type), Integer.valueOf(OsConstants.ETH_P_IP));
            }
        }
        if (pktType <= 1) {
            if (packet.remaining() < 264) {
                throw new ParseException(DhcpErrorEvent.L3_TOO_SHORT, "L3 packet too short, %d < %d", Integer.valueOf(packet.remaining()), Integer.valueOf(MIN_PACKET_LENGTH_L3));
            }
            byte ipTypeAndLength = packet.get();
            int ipVersion = (ipTypeAndLength & 240) >> 4;
            if (ipVersion != 4) {
                throw new ParseException(DhcpErrorEvent.L3_NOT_IPV4, "Invalid IP version %d", Integer.valueOf(ipVersion));
            }
            packet.get();
            packet.getShort();
            packet.getShort();
            packet.get();
            packet.get();
            packet.get();
            byte ipProto = packet.get();
            packet.getShort();
            ipSrc = readIpAddress(packet);
            readIpAddress(packet);
            if (ipProto != 17) {
                throw new ParseException(DhcpErrorEvent.L4_NOT_UDP, "Protocol not UDP: %d", Byte.valueOf(ipProto));
            }
            int optionWords = (ipTypeAndLength & DHCP_DOMAIN_NAME) - 5;
            for (int i = 0; i < optionWords; i++) {
                packet.getInt();
            }
            short udpSrcPort = packet.getShort();
            short udpDstPort = packet.getShort();
            packet.getShort();
            packet.getShort();
            if (!isPacketToOrFromClient(udpSrcPort, udpDstPort) && !isPacketServerToServer(udpSrcPort, udpDstPort)) {
                throw new ParseException(DhcpErrorEvent.L4_WRONG_PORT, "Unexpected UDP ports %d->%d", Short.valueOf(udpSrcPort), Short.valueOf(udpDstPort));
            }
        }
        if (pktType > 2 || packet.remaining() < 236) {
            throw new ParseException(DhcpErrorEvent.BOOTP_TOO_SHORT, "Invalid type or BOOTP packet too short, %d < %d", Integer.valueOf(packet.remaining()), Integer.valueOf(MIN_PACKET_LENGTH_BOOTP));
        }
        packet.get();
        packet.get();
        int addrLen = packet.get() & DHCP_OPTION_END;
        packet.get();
        int transactionId = packet.getInt();
        short secs = packet.getShort();
        short bootpFlags = packet.getShort();
        boolean broadcast = (Short.MIN_VALUE & bootpFlags) != 0;
        byte[] ipv4addr = new byte[4];
        try {
            packet.get(ipv4addr);
            Inet4Address clientIp = (Inet4Address) Inet4Address.getByAddress(ipv4addr);
            packet.get(ipv4addr);
            Inet4Address yourIp = (Inet4Address) Inet4Address.getByAddress(ipv4addr);
            packet.get(ipv4addr);
            Inet4Address nextIp = (Inet4Address) Inet4Address.getByAddress(ipv4addr);
            packet.get(ipv4addr);
            Inet4Address relayIp = (Inet4Address) Inet4Address.getByAddress(ipv4addr);
            if (addrLen > 16) {
                addrLen = ETHER_BROADCAST.length;
            }
            if (addrLen > 16) {
                addrLen = ETHER_BROADCAST.length;
            }
            byte[] clientMac = new byte[addrLen];
            packet.get(clientMac);
            packet.position(packet.position() + (16 - addrLen) + 64 + 128);
            int dhcpMagicCookie = packet.getInt();
            if (dhcpMagicCookie != DHCP_MAGIC_COOKIE) {
                throw new ParseException(DhcpErrorEvent.DHCP_BAD_MAGIC_COOKIE, "Bad magic cookie 0x%08x, should be 0x%08x", Integer.valueOf(dhcpMagicCookie), Integer.valueOf(DHCP_MAGIC_COOKIE));
            }
            boolean notFinishedOptions = true;
            while (packet.position() < packet.limit() && notFinishedOptions) {
                byte optionType = packet.get();
                if (optionType == -1) {
                    notFinishedOptions = false;
                } else if (optionType != 0) {
                    try {
                        int optionLen = packet.get() & DHCP_OPTION_END;
                        int expectedLen = 0;
                        switch (optionType) {
                            case 1:
                                netMask = readIpAddress(packet);
                                expectedLen = 4;
                                break;
                            case 3:
                                expectedLen = 0;
                                while (expectedLen < optionLen) {
                                    gateways.add(readIpAddress(packet));
                                    expectedLen += 4;
                                }
                                break;
                            case 6:
                                expectedLen = 0;
                                while (expectedLen < optionLen) {
                                    dnsServers.add(readIpAddress(packet));
                                    expectedLen += 4;
                                }
                                break;
                            case 12:
                                expectedLen = optionLen;
                                hostName = readAsciiString(packet, optionLen, false);
                                break;
                            case 15:
                                expectedLen = optionLen;
                                domainName = readAsciiString(packet, optionLen, false);
                                break;
                            case WindowManagerService.H.DO_ANIMATION_CALLBACK:
                                expectedLen = 2;
                                mtu = Short.valueOf(packet.getShort());
                                break;
                            case WindowManagerService.H.DO_DISPLAY_REMOVED:
                                bcAddr = readIpAddress(packet);
                                expectedLen = 4;
                                break;
                            case 43:
                                expectedLen = optionLen;
                                vendorInfo = readAsciiString(packet, optionLen, true);
                                break;
                            case 50:
                                requestedIp = readIpAddress(packet);
                                expectedLen = 4;
                                break;
                            case 51:
                                leaseTime = Integer.valueOf(packet.getInt());
                                expectedLen = 4;
                                break;
                            case 53:
                                dhcpType = packet.get();
                                expectedLen = 1;
                                break;
                            case 54:
                                serverIdentifier = readIpAddress(packet);
                                expectedLen = 4;
                                break;
                            case 55:
                                expectedParams = new byte[optionLen];
                                packet.get(expectedParams);
                                expectedLen = optionLen;
                                break;
                            case HdmiCecKeycode.CEC_KEYCODE_PAGE_DOWN:
                                expectedLen = optionLen;
                                message = readAsciiString(packet, optionLen, false);
                                break;
                            case 57:
                                expectedLen = 2;
                                maxMessageSize = Short.valueOf(packet.getShort());
                                break;
                            case 58:
                                expectedLen = 4;
                                T1 = Integer.valueOf(packet.getInt());
                                break;
                            case 59:
                                expectedLen = 4;
                                T2 = Integer.valueOf(packet.getInt());
                                break;
                            case 60:
                                expectedLen = optionLen;
                                vendorId = readAsciiString(packet, optionLen, true);
                                break;
                            case 61:
                                byte[] id = new byte[optionLen];
                                packet.get(id);
                                expectedLen = optionLen;
                                break;
                            default:
                                for (int i2 = 0; i2 < optionLen; i2++) {
                                    expectedLen++;
                                    packet.get();
                                }
                                break;
                        }
                        if (expectedLen != optionLen) {
                            int errorCode = DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.DHCP_INVALID_OPTION_LENGTH, optionType);
                            throw new ParseException(errorCode, "Invalid length %d for option %d, expected %d", Integer.valueOf(optionLen), Byte.valueOf(optionType), Integer.valueOf(expectedLen));
                        }
                    } catch (BufferUnderflowException e) {
                        int errorCode2 = DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.BUFFER_UNDERFLOW, optionType);
                        throw new ParseException(errorCode2, "BufferUnderflowException", new Object[0]);
                    }
                } else {
                    continue;
                }
            }
            switch (dhcpType) {
                case -1:
                    throw new ParseException(DhcpErrorEvent.DHCP_NO_MSG_TYPE, "No DHCP message type option", new Object[0]);
                case 0:
                case 7:
                default:
                    throw new ParseException(DhcpErrorEvent.DHCP_UNKNOWN_MSG_TYPE, "Unimplemented DHCP type %d", Byte.valueOf(dhcpType));
                case 1:
                    newPacket = new DhcpDiscoverPacket(transactionId, secs, clientMac, broadcast);
                    break;
                case 2:
                    newPacket = new DhcpOfferPacket(transactionId, secs, broadcast, ipSrc, clientIp, yourIp, clientMac);
                    break;
                case 3:
                    newPacket = new DhcpRequestPacket(transactionId, secs, clientIp, clientMac, broadcast);
                    break;
                case 4:
                    newPacket = new DhcpDeclinePacket(transactionId, secs, clientIp, yourIp, nextIp, relayIp, clientMac);
                    break;
                case 5:
                    newPacket = new DhcpAckPacket(transactionId, secs, broadcast, ipSrc, clientIp, yourIp, clientMac);
                    break;
                case 6:
                    newPacket = new DhcpNakPacket(transactionId, secs, clientIp, yourIp, nextIp, relayIp, clientMac);
                    break;
                case 8:
                    newPacket = new DhcpInformPacket(transactionId, secs, clientIp, yourIp, nextIp, relayIp, clientMac);
                    break;
            }
            newPacket.mBroadcastAddress = bcAddr;
            newPacket.mDnsServers = dnsServers;
            newPacket.mDomainName = domainName;
            newPacket.mGateways = gateways;
            newPacket.mHostName = hostName;
            newPacket.mLeaseTime = leaseTime;
            newPacket.mMessage = message;
            newPacket.mMtu = mtu;
            newPacket.mRequestedIp = requestedIp;
            newPacket.mRequestedParams = expectedParams;
            newPacket.mServerIdentifier = serverIdentifier;
            newPacket.mSubnetMask = netMask;
            newPacket.mMaxMessageSize = maxMessageSize;
            newPacket.mT1 = T1;
            newPacket.mT2 = T2;
            newPacket.mVendorId = vendorId;
            newPacket.mVendorInfo = vendorInfo;
            return newPacket;
        } catch (UnknownHostException e2) {
            throw new ParseException(DhcpErrorEvent.L3_INVALID_IP, "Invalid IPv4 address: %s", Arrays.toString(ipv4addr));
        }
    }

    public static DhcpPacket decodeFullPacket(byte[] packet, int length, int pktType) throws ParseException {
        ByteBuffer buffer = ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN);
        return decodeFullPacket(buffer, pktType);
    }

    public DhcpResults toDhcpResults() {
        int prefixLength;
        Inet4Address ipAddress = this.mYourIp;
        if (ipAddress.equals(Inet4Address.ANY)) {
            ipAddress = this.mClientIp;
            if (ipAddress.equals(Inet4Address.ANY)) {
                return null;
            }
        }
        if (this.mSubnetMask != null) {
            try {
                prefixLength = NetworkUtils.netmaskToPrefixLength(this.mSubnetMask);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            prefixLength = NetworkUtils.getImplicitNetmask(ipAddress);
        }
        DhcpResults results = new DhcpResults();
        try {
            results.ipAddress = new LinkAddress(ipAddress, prefixLength);
            if (this.mGateways.size() > 0) {
                results.gateway = this.mGateways.get(0);
            }
            results.dnsServers.addAll(this.mDnsServers);
            results.domains = this.mDomainName;
            results.serverAddress = this.mServerIdentifier;
            results.vendorInfo = this.mVendorInfo;
            results.leaseDuration = this.mLeaseTime != null ? this.mLeaseTime.intValue() : -1;
            results.mtu = (this.mMtu == null || MIN_MTU > this.mMtu.shortValue() || this.mMtu.shortValue() > 1500) ? (short) 0 : this.mMtu.shortValue();
            return results;
        } catch (IllegalArgumentException e2) {
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

    public static ByteBuffer buildDiscoverPacket(int encap, int transactionId, short secs, byte[] clientMac, boolean broadcast, byte[] expectedParams) {
        DhcpPacket pkt = new DhcpDiscoverPacket(transactionId, secs, clientMac, broadcast);
        pkt.mRequestedParams = expectedParams;
        return pkt.buildPacket(encap, DHCP_SERVER, (short) 68);
    }

    public static ByteBuffer buildOfferPacket(int encap, int transactionId, boolean broadcast, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac, Integer timeout, Inet4Address netMask, Inet4Address bcAddr, List<Inet4Address> gateways, List<Inet4Address> dnsServers, Inet4Address dhcpServerIdentifier, String domainName) {
        DhcpPacket pkt = new DhcpOfferPacket(transactionId, (short) 0, broadcast, serverIpAddr, INADDR_ANY, clientIpAddr, mac);
        pkt.mGateways = gateways;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mSubnetMask = netMask;
        pkt.mBroadcastAddress = bcAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildAckPacket(int encap, int transactionId, boolean broadcast, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac, Integer timeout, Inet4Address netMask, Inet4Address bcAddr, List<Inet4Address> gateways, List<Inet4Address> dnsServers, Inet4Address dhcpServerIdentifier, String domainName) {
        DhcpPacket pkt = new DhcpAckPacket(transactionId, (short) 0, broadcast, serverIpAddr, INADDR_ANY, clientIpAddr, mac);
        pkt.mGateways = gateways;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mSubnetMask = netMask;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mBroadcastAddress = bcAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildNakPacket(int encap, int transactionId, Inet4Address serverIpAddr, Inet4Address clientIpAddr, byte[] mac) {
        DhcpPacket pkt = new DhcpNakPacket(transactionId, (short) 0, clientIpAddr, serverIpAddr, serverIpAddr, serverIpAddr, mac);
        pkt.mMessage = "requested address not available";
        pkt.mRequestedIp = clientIpAddr;
        return pkt.buildPacket(encap, (short) 68, DHCP_SERVER);
    }

    public static ByteBuffer buildRequestPacket(int encap, int transactionId, short secs, Inet4Address clientIp, boolean broadcast, byte[] clientMac, Inet4Address requestedIpAddress, Inet4Address serverIdentifier, byte[] requestedParams, String hostName) {
        DhcpPacket pkt = new DhcpRequestPacket(transactionId, secs, clientIp, clientMac, broadcast);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        pkt.mHostName = hostName;
        pkt.mRequestedParams = requestedParams;
        ByteBuffer result = pkt.buildPacket(encap, DHCP_SERVER, (short) 68);
        return result;
    }
}
