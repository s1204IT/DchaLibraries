package android.net.dhcp;

import android.net.ProxyInfo;
import android.net.wifi.WifiEnterpriseConfig;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
    protected static final byte DHCP_MESSAGE = 56;
    protected static final byte DHCP_MESSAGE_TYPE = 53;
    protected static final byte DHCP_MESSAGE_TYPE_ACK = 5;
    protected static final byte DHCP_MESSAGE_TYPE_DECLINE = 4;
    protected static final byte DHCP_MESSAGE_TYPE_DISCOVER = 1;
    protected static final byte DHCP_MESSAGE_TYPE_INFORM = 8;
    protected static final byte DHCP_MESSAGE_TYPE_NAK = 6;
    protected static final byte DHCP_MESSAGE_TYPE_OFFER = 2;
    protected static final byte DHCP_MESSAGE_TYPE_REQUEST = 3;
    protected static final byte DHCP_PARAMETER_LIST = 55;
    protected static final byte DHCP_RENEWAL_TIME = 58;
    protected static final byte DHCP_REQUESTED_IP = 50;
    protected static final byte DHCP_ROUTER = 3;
    static final short DHCP_SERVER = 67;
    protected static final byte DHCP_SERVER_IDENTIFIER = 54;
    protected static final byte DHCP_SUBNET_MASK = 1;
    protected static final byte DHCP_VENDOR_CLASS_ID = 60;
    public static final int ENCAP_BOOTP = 2;
    public static final int ENCAP_L2 = 0;
    public static final int ENCAP_L3 = 1;
    private static final short IP_FLAGS_OFFSET = 16384;
    private static final byte IP_TOS_LOWDELAY = 16;
    private static final byte IP_TTL = 64;
    private static final byte IP_TYPE_UDP = 17;
    private static final byte IP_VERSION_HEADER_LEN = 69;
    protected static final int MAX_LENGTH = 1500;
    protected static final String TAG = "DhcpPacket";
    protected boolean mBroadcast;
    protected InetAddress mBroadcastAddress;
    protected final InetAddress mClientIp;
    protected final byte[] mClientMac;
    protected List<InetAddress> mDnsServers;
    protected String mDomainName;
    protected InetAddress mGateway;
    protected String mHostName;
    protected Integer mLeaseTime;
    protected String mMessage;
    private final InetAddress mNextIp;
    private final InetAddress mRelayIp;
    protected InetAddress mRequestedIp;
    protected byte[] mRequestedParams;
    protected InetAddress mServerIdentifier;
    protected InetAddress mSubnetMask;
    protected final int mTransId;
    protected final InetAddress mYourIp;

    public abstract ByteBuffer buildPacket(int i, short s, short s2);

    public abstract void doNextOp(DhcpStateMachine dhcpStateMachine);

    abstract void finishPacket(ByteBuffer byteBuffer);

    protected DhcpPacket(int transId, InetAddress clientIp, InetAddress yourIp, InetAddress nextIp, InetAddress relayIp, byte[] clientMac, boolean broadcast) {
        this.mTransId = transId;
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

    protected void fillInPacket(int encap, InetAddress destIp, InetAddress srcIp, short destUdp, short srcUdp, ByteBuffer buf, byte requestCode, boolean broadcast) {
        byte[] destIpArray = destIp.getAddress();
        byte[] srcIpArray = srcIp.getAddress();
        int ipLengthOffset = 0;
        int ipChecksumOffset = 0;
        int endIpHeader = 0;
        int udpHeaderOffset = 0;
        int udpLengthOffset = 0;
        int udpChecksumOffset = 0;
        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        if (encap == 1) {
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
        buf.put((byte) 0);
        buf.putInt(this.mTransId);
        buf.putShort((short) 0);
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
        buf.putInt(1669485411);
        finishPacket(buf);
        if ((buf.position() & 1) == 1) {
            buf.put((byte) 0);
        }
        if (encap == 1) {
            short udpLen = (short) (buf.position() - udpHeaderOffset);
            buf.putShort(udpLengthOffset, udpLen);
            int udpSeed = 0 + intAbs(buf.getShort(ipChecksumOffset + 2));
            buf.putShort(udpChecksumOffset, (short) checksum(buf, udpSeed + intAbs(buf.getShort(ipChecksumOffset + 4)) + intAbs(buf.getShort(ipChecksumOffset + 6)) + intAbs(buf.getShort(ipChecksumOffset + 8)) + 17 + udpLen, udpHeaderOffset, buf.position()));
            buf.putShort(ipLengthOffset, (short) buf.position());
            buf.putShort(ipChecksumOffset, (short) checksum(buf, 0, 0, endIpHeader));
        }
    }

    private int intAbs(short v) {
        return v < 0 ? v + 65536 : v;
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
        int sum2 = ((sum >> 16) & 65535) + (65535 & sum);
        int negated = ((((sum2 >> 16) & 65535) + sum2) & 65535) ^ (-1);
        return intAbs((short) negated);
    }

    protected void addTlv(ByteBuffer buf, byte type, byte value) {
        buf.put(type);
        buf.put((byte) 1);
        buf.put(value);
    }

    protected void addTlv(ByteBuffer buf, byte type, byte[] payload) {
        if (payload != null) {
            buf.put(type);
            buf.put((byte) payload.length);
            buf.put(payload);
        }
    }

    protected void addTlv(ByteBuffer buf, byte type, InetAddress addr) {
        if (addr != null) {
            addTlv(buf, type, addr.getAddress());
        }
    }

    protected void addTlv(ByteBuffer buf, byte type, List<InetAddress> addrs) {
        if (addrs != null && addrs.size() > 0) {
            buf.put(type);
            buf.put((byte) (addrs.size() * 4));
            for (InetAddress addr : addrs) {
                buf.put(addr.getAddress());
            }
        }
    }

    protected void addTlv(ByteBuffer buf, byte type, Integer value) {
        if (value != null) {
            buf.put(type);
            buf.put((byte) 4);
            buf.putInt(value.intValue());
        }
    }

    protected void addTlv(ByteBuffer buf, byte type, String str) {
        if (str != null) {
            buf.put(type);
            buf.put((byte) str.length());
            for (int i = 0; i < str.length(); i++) {
                buf.put((byte) str.charAt(i));
            }
        }
    }

    protected void addTlvEnd(ByteBuffer buf) {
        buf.put((byte) -1);
    }

    public static String macToString(byte[] mac) {
        String macAddr = ProxyInfo.LOCAL_EXCL_LIST;
        for (int i = 0; i < mac.length; i++) {
            String hexString = WifiEnterpriseConfig.ENGINE_DISABLE + Integer.toHexString(mac[i]);
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

    private static InetAddress readIpAddress(ByteBuffer packet) {
        byte[] ipAddr = new byte[4];
        packet.get(ipAddr);
        try {
            InetAddress result = InetAddress.getByAddress(ipAddr);
            return result;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static String readAsciiString(ByteBuffer buf, int byteCount) {
        byte[] bytes = new byte[byteCount];
        buf.get(bytes);
        return new String(bytes, 0, bytes.length, StandardCharsets.US_ASCII);
    }

    public static DhcpPacket decodeFullPacket(ByteBuffer packet, int pktType) {
        DhcpPacket newPacket;
        List<InetAddress> dnsServers = new ArrayList<>();
        InetAddress gateway = null;
        Integer leaseTime = null;
        InetAddress serverIdentifier = null;
        InetAddress netMask = null;
        String message = null;
        byte[] expectedParams = null;
        String hostName = null;
        String domainName = null;
        InetAddress ipSrc = null;
        InetAddress bcAddr = null;
        InetAddress requestedIp = null;
        byte dhcpType = -1;
        packet.order(ByteOrder.BIG_ENDIAN);
        if (pktType == 0) {
            byte[] l2dst = new byte[6];
            byte[] l2src = new byte[6];
            packet.get(l2dst);
            packet.get(l2src);
            short l2type = packet.getShort();
            if (l2type != 2048) {
                return null;
            }
        }
        if (pktType == 0 || pktType == 1) {
            packet.get();
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
                return null;
            }
            short udpSrcPort = packet.getShort();
            packet.getShort();
            packet.getShort();
            packet.getShort();
            if (udpSrcPort != 67 && udpSrcPort != 68) {
                return null;
            }
        }
        packet.get();
        packet.get();
        int i = packet.get();
        packet.get();
        int transactionId = packet.getInt();
        packet.getShort();
        short bootpFlags = packet.getShort();
        boolean broadcast = (Short.MIN_VALUE & bootpFlags) != 0;
        byte[] ipv4addr = new byte[4];
        try {
            packet.get(ipv4addr);
            InetAddress clientIp = InetAddress.getByAddress(ipv4addr);
            packet.get(ipv4addr);
            InetAddress yourIp = InetAddress.getByAddress(ipv4addr);
            packet.get(ipv4addr);
            InetAddress nextIp = InetAddress.getByAddress(ipv4addr);
            packet.get(ipv4addr);
            InetAddress relayIp = InetAddress.getByAddress(ipv4addr);
            byte[] clientMac = new byte[i];
            packet.get(clientMac);
            packet.position(packet.position() + (16 - i) + 64 + 128);
            int dhcpMagicCookie = packet.getInt();
            if (dhcpMagicCookie != 1669485411) {
                return null;
            }
            boolean notFinishedOptions = true;
            while (packet.position() < packet.limit() && notFinishedOptions) {
                byte optionType = packet.get();
                if (optionType == -1) {
                    notFinishedOptions = false;
                } else {
                    int i2 = packet.get();
                    int expectedLen = 0;
                    switch (optionType) {
                        case 1:
                            netMask = readIpAddress(packet);
                            expectedLen = 4;
                            break;
                        case 3:
                            gateway = readIpAddress(packet);
                            expectedLen = 4;
                            break;
                        case 6:
                            expectedLen = 0;
                            while (expectedLen < i2) {
                                dnsServers.add(readIpAddress(packet));
                                expectedLen += 4;
                            }
                            break;
                        case 12:
                            expectedLen = i2;
                            hostName = readAsciiString(packet, i2);
                            break;
                        case 15:
                            expectedLen = i2;
                            domainName = readAsciiString(packet, i2);
                            break;
                        case 28:
                            bcAddr = readIpAddress(packet);
                            expectedLen = 4;
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
                            expectedParams = new byte[i2];
                            packet.get(expectedParams);
                            expectedLen = i2;
                            break;
                        case 56:
                            expectedLen = i2;
                            message = readAsciiString(packet, i2);
                            break;
                        case 60:
                            expectedLen = i2;
                            readAsciiString(packet, i2);
                            break;
                        case 61:
                            byte[] id = new byte[i2];
                            packet.get(id);
                            expectedLen = i2;
                            break;
                        default:
                            for (int i3 = 0; i3 < i2; i3++) {
                                expectedLen++;
                                packet.get();
                            }
                            break;
                    }
                    if (expectedLen != i2) {
                        return null;
                    }
                }
            }
            switch (dhcpType) {
                case -1:
                    return null;
                case 0:
                case 7:
                default:
                    System.out.println("Unimplemented type: " + ((int) dhcpType));
                    return null;
                case 1:
                    newPacket = new DhcpDiscoverPacket(transactionId, clientMac, broadcast);
                    break;
                case 2:
                    newPacket = new DhcpOfferPacket(transactionId, broadcast, ipSrc, yourIp, clientMac);
                    break;
                case 3:
                    newPacket = new DhcpRequestPacket(transactionId, clientIp, clientMac, broadcast);
                    break;
                case 4:
                    newPacket = new DhcpDeclinePacket(transactionId, clientIp, yourIp, nextIp, relayIp, clientMac);
                    break;
                case 5:
                    newPacket = new DhcpAckPacket(transactionId, broadcast, ipSrc, yourIp, clientMac);
                    break;
                case 6:
                    newPacket = new DhcpNakPacket(transactionId, clientIp, yourIp, nextIp, relayIp, clientMac);
                    break;
                case 8:
                    newPacket = new DhcpInformPacket(transactionId, clientIp, yourIp, nextIp, relayIp, clientMac);
                    break;
            }
            newPacket.mBroadcastAddress = bcAddr;
            newPacket.mDnsServers = dnsServers;
            newPacket.mDomainName = domainName;
            newPacket.mGateway = gateway;
            newPacket.mHostName = hostName;
            newPacket.mLeaseTime = leaseTime;
            newPacket.mMessage = message;
            newPacket.mRequestedIp = requestedIp;
            newPacket.mRequestedParams = expectedParams;
            newPacket.mServerIdentifier = serverIdentifier;
            newPacket.mSubnetMask = netMask;
            return newPacket;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static DhcpPacket decodeFullPacket(byte[] packet, int pktType) {
        ByteBuffer buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN);
        return decodeFullPacket(buffer, pktType);
    }

    public static ByteBuffer buildDiscoverPacket(int encap, int transactionId, byte[] clientMac, boolean broadcast, byte[] expectedParams) {
        DhcpPacket pkt = new DhcpDiscoverPacket(transactionId, clientMac, broadcast);
        pkt.mRequestedParams = expectedParams;
        return pkt.buildPacket(encap, DHCP_SERVER, DHCP_CLIENT);
    }

    public static ByteBuffer buildOfferPacket(int encap, int transactionId, boolean broadcast, InetAddress serverIpAddr, InetAddress clientIpAddr, byte[] mac, Integer timeout, InetAddress netMask, InetAddress bcAddr, InetAddress gateway, List<InetAddress> dnsServers, InetAddress dhcpServerIdentifier, String domainName) {
        DhcpPacket pkt = new DhcpOfferPacket(transactionId, broadcast, serverIpAddr, clientIpAddr, mac);
        pkt.mGateway = gateway;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mSubnetMask = netMask;
        pkt.mBroadcastAddress = bcAddr;
        return pkt.buildPacket(encap, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildAckPacket(int encap, int transactionId, boolean broadcast, InetAddress serverIpAddr, InetAddress clientIpAddr, byte[] mac, Integer timeout, InetAddress netMask, InetAddress bcAddr, InetAddress gateway, List<InetAddress> dnsServers, InetAddress dhcpServerIdentifier, String domainName) {
        DhcpPacket pkt = new DhcpAckPacket(transactionId, broadcast, serverIpAddr, clientIpAddr, mac);
        pkt.mGateway = gateway;
        pkt.mDnsServers = dnsServers;
        pkt.mLeaseTime = timeout;
        pkt.mDomainName = domainName;
        pkt.mSubnetMask = netMask;
        pkt.mServerIdentifier = dhcpServerIdentifier;
        pkt.mBroadcastAddress = bcAddr;
        return pkt.buildPacket(encap, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildNakPacket(int encap, int transactionId, InetAddress serverIpAddr, InetAddress clientIpAddr, byte[] mac) {
        DhcpPacket pkt = new DhcpNakPacket(transactionId, clientIpAddr, serverIpAddr, serverIpAddr, serverIpAddr, mac);
        pkt.mMessage = "requested address not available";
        pkt.mRequestedIp = clientIpAddr;
        return pkt.buildPacket(encap, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildRequestPacket(int encap, int transactionId, InetAddress clientIp, boolean broadcast, byte[] clientMac, InetAddress requestedIpAddress, InetAddress serverIdentifier, byte[] requestedParams, String hostName) {
        DhcpPacket pkt = new DhcpRequestPacket(transactionId, clientIp, clientMac, broadcast);
        pkt.mRequestedIp = requestedIpAddress;
        pkt.mServerIdentifier = serverIdentifier;
        pkt.mHostName = hostName;
        pkt.mRequestedParams = requestedParams;
        ByteBuffer result = pkt.buildPacket(encap, DHCP_SERVER, DHCP_CLIENT);
        return result;
    }
}
