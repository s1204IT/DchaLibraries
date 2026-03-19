package com.mediatek.net.dhcp;

import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.net.metrics.DhcpErrorEvent;
import android.os.Build;
import android.os.SystemProperties;
import android.system.OsConstants;
import android.text.TextUtils;
import com.android.internal.annotations.VisibleForTesting;
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
import java.util.Iterator;
import java.util.List;

public abstract class DhcpPacket {
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

    protected DhcpPacket(int i, short s, Inet4Address inet4Address, Inet4Address inet4Address2, Inet4Address inet4Address3, Inet4Address inet4Address4, byte[] bArr, boolean z) {
        this.mTransId = i;
        this.mSecs = s;
        this.mClientIp = inet4Address;
        this.mYourIp = inet4Address2;
        this.mNextIp = inet4Address3;
        this.mRelayIp = inet4Address4;
        this.mClientMac = bArr;
        this.mBroadcast = z;
    }

    public int getTransactionId() {
        return this.mTransId;
    }

    public byte[] getClientMac() {
        return this.mClientMac;
    }

    public byte[] getClientId() {
        byte[] bArr = new byte[this.mClientMac.length + 1];
        bArr[0] = 1;
        System.arraycopy(this.mClientMac, 0, bArr, 1, this.mClientMac.length);
        return bArr;
    }

    protected void fillInPacket(int i, Inet4Address inet4Address, Inet4Address inet4Address2, short s, short s2, ByteBuffer byteBuffer, byte b, boolean z) {
        int iPosition;
        int iPosition2;
        int iPosition3;
        int iPosition4;
        int iPosition5;
        int iPosition6;
        int iPosition7;
        byte[] address = inet4Address.getAddress();
        byte[] address2 = inet4Address2.getAddress();
        byteBuffer.clear();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        if (i == 0) {
            byteBuffer.put(ETHER_BROADCAST);
            byteBuffer.put(this.mClientMac);
            byteBuffer.putShort((short) OsConstants.ETH_P_IP);
        }
        if (i <= 1) {
            iPosition3 = byteBuffer.position();
            byteBuffer.put(IP_VERSION_HEADER_LEN);
            byteBuffer.put(IP_TOS_LOWDELAY);
            iPosition4 = byteBuffer.position();
            byteBuffer.putShort((short) 0);
            byteBuffer.putShort((short) 0);
            byteBuffer.putShort(IP_FLAGS_OFFSET);
            byteBuffer.put(IP_TTL);
            byteBuffer.put(IP_TYPE_UDP);
            iPosition5 = byteBuffer.position();
            byteBuffer.putShort((short) 0);
            byteBuffer.put(address2);
            byteBuffer.put(address);
            iPosition = byteBuffer.position();
            iPosition2 = byteBuffer.position();
            byteBuffer.putShort(s2);
            byteBuffer.putShort(s);
            iPosition6 = byteBuffer.position();
            byteBuffer.putShort((short) 0);
            iPosition7 = byteBuffer.position();
            byteBuffer.putShort((short) 0);
        } else {
            iPosition = 0;
            iPosition2 = 0;
            iPosition3 = 0;
            iPosition4 = 0;
            iPosition5 = 0;
            iPosition6 = 0;
            iPosition7 = 0;
        }
        byteBuffer.put(b);
        byteBuffer.put((byte) 1);
        byteBuffer.put((byte) this.mClientMac.length);
        byteBuffer.put(DHCP_OPTION_PAD);
        byteBuffer.putInt(this.mTransId);
        byteBuffer.putShort(this.mSecs);
        if (z) {
            byteBuffer.putShort(Short.MIN_VALUE);
        } else {
            byteBuffer.putShort((short) 0);
        }
        byteBuffer.put(this.mClientIp.getAddress());
        byteBuffer.put(this.mYourIp.getAddress());
        byteBuffer.put(this.mNextIp.getAddress());
        byteBuffer.put(this.mRelayIp.getAddress());
        byteBuffer.put(this.mClientMac);
        byteBuffer.position(byteBuffer.position() + (16 - this.mClientMac.length) + 64 + 128);
        byteBuffer.putInt(DHCP_MAGIC_COOKIE);
        finishPacket(byteBuffer);
        if ((byteBuffer.position() & 1) == 1) {
            byteBuffer.put(DHCP_OPTION_PAD);
        }
        if (i <= 1) {
            short sPosition = (short) (byteBuffer.position() - iPosition2);
            byteBuffer.putShort(iPosition6, sPosition);
            byteBuffer.putShort(iPosition7, (short) checksum(byteBuffer, intAbs(byteBuffer.getShort(iPosition5 + 2)) + 0 + intAbs(byteBuffer.getShort(iPosition5 + 4)) + intAbs(byteBuffer.getShort(iPosition5 + 6)) + intAbs(byteBuffer.getShort(iPosition5 + 8)) + 17 + sPosition, iPosition2, byteBuffer.position()));
            byteBuffer.putShort(iPosition4, (short) (byteBuffer.position() - iPosition3));
            byteBuffer.putShort(iPosition5, (short) checksum(byteBuffer, 0, iPosition3, iPosition));
        }
    }

    private static int intAbs(short s) {
        return s & 65535;
    }

    private int checksum(ByteBuffer byteBuffer, int i, int i2, int i3) {
        int iPosition = byteBuffer.position();
        byteBuffer.position(i2);
        ShortBuffer shortBufferAsShortBuffer = byteBuffer.asShortBuffer();
        byteBuffer.position(iPosition);
        short[] sArr = new short[(i3 - i2) / 2];
        shortBufferAsShortBuffer.get(sArr);
        for (short s : sArr) {
            i += intAbs(s);
        }
        int length = i2 + (sArr.length * 2);
        if (i3 != length) {
            short s2 = byteBuffer.get(length);
            if (s2 < 0) {
                s2 = (short) (s2 + 256);
            }
            i += s2 * 256;
        }
        int i4 = ((i >> 16) & 65535) + (i & 65535);
        return intAbs((short) (~((i4 + ((i4 >> 16) & 65535)) & 65535)));
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, byte b2) {
        byteBuffer.put(b);
        byteBuffer.put((byte) 1);
        byteBuffer.put(b2);
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, byte[] bArr) {
        if (bArr != null) {
            if (bArr.length > 255) {
                throw new IllegalArgumentException("DHCP option too long: " + bArr.length + " vs. " + MAX_OPTION_LEN);
            }
            byteBuffer.put(b);
            byteBuffer.put((byte) bArr.length);
            byteBuffer.put(bArr);
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, Inet4Address inet4Address) {
        if (inet4Address != null) {
            addTlv(byteBuffer, b, inet4Address.getAddress());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, List<Inet4Address> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        int size = 4 * list.size();
        if (size > 255) {
            throw new IllegalArgumentException("DHCP option too long: " + size + " vs. " + MAX_OPTION_LEN);
        }
        byteBuffer.put(b);
        byteBuffer.put((byte) size);
        Iterator<Inet4Address> it = list.iterator();
        while (it.hasNext()) {
            byteBuffer.put(it.next().getAddress());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, Short sh) {
        if (sh != null) {
            byteBuffer.put(b);
            byteBuffer.put((byte) 2);
            byteBuffer.putShort(sh.shortValue());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, Integer num) {
        if (num != null) {
            byteBuffer.put(b);
            byteBuffer.put(DHCP_MESSAGE_TYPE_DECLINE);
            byteBuffer.putInt(num.intValue());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, byte b, String str) {
        try {
            addTlv(byteBuffer, b, str.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("String is not US-ASCII: " + str);
        }
    }

    protected static void addTlvEnd(ByteBuffer byteBuffer) {
        byteBuffer.put(DHCP_OPTION_END);
    }

    private String getVendorId() {
        if (testOverrideVendorId != null) {
            return testOverrideVendorId;
        }
        return "android-dhcp-" + Build.VERSION.RELEASE;
    }

    private String getHostname() {
        return testOverrideHostname != null ? testOverrideHostname : SystemProperties.get("net.hostname");
    }

    protected void addCommonClientTlvs(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, DHCP_MAX_MESSAGE_SIZE, (Short) 1500);
        addTlv(byteBuffer, DHCP_VENDOR_CLASS_ID, getVendorId());
        String hostname = getHostname();
        if (!TextUtils.isEmpty(hostname)) {
            addTlv(byteBuffer, DHCP_HOST_NAME, hostname);
        }
    }

    public static String macToString(byte[] bArr) {
        String str = "";
        for (int i = 0; i < bArr.length; i++) {
            str = str + ("0" + Integer.toHexString(bArr[i])).substring(r2.length() - 2);
            if (i != bArr.length - 1) {
                str = str + ":";
            }
        }
        return str;
    }

    public String toString() {
        return macToString(this.mClientMac);
    }

    private static Inet4Address readIpAddress(ByteBuffer byteBuffer) {
        byte[] bArr = new byte[4];
        byteBuffer.get(bArr);
        try {
            return (Inet4Address) Inet4Address.getByAddress(bArr);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static String readAsciiString(ByteBuffer byteBuffer, int i, boolean z) {
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr);
        int length = bArr.length;
        if (!z) {
            length = 0;
            while (length < bArr.length && bArr[length] != 0) {
                length++;
            }
        }
        return new String(bArr, 0, length, StandardCharsets.US_ASCII);
    }

    private static boolean isPacketToOrFromClient(short s, short s2) {
        return s == 68 || s2 == 68;
    }

    private static boolean isPacketServerToServer(short s, short s2) {
        return s == 67 && s2 == 67;
    }

    public static class ParseException extends Exception {
        public final int errorCode;

        public ParseException(int i, String str, Object... objArr) {
            super(String.format(str, objArr));
            this.errorCode = i;
        }
    }

    @VisibleForTesting
    static DhcpPacket decodeFullPacket(ByteBuffer byteBuffer, int i) throws ParseException {
        Inet4Address ipAddress;
        DhcpPacket dhcpInformPacket;
        int i2;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byte b = 6;
        if (i == 0) {
            if (byteBuffer.remaining() < 278) {
                throw new ParseException(DhcpErrorEvent.L2_TOO_SHORT, "L2 packet too short, %d < %d", Integer.valueOf(byteBuffer.remaining()), Integer.valueOf(MIN_PACKET_LENGTH_L2));
            }
            byteBuffer.get(new byte[6]);
            byteBuffer.get(new byte[6]);
            short s = byteBuffer.getShort();
            if (s != OsConstants.ETH_P_IP) {
                throw new ParseException(DhcpErrorEvent.L2_WRONG_ETH_TYPE, "Unexpected L2 type 0x%04x, expected 0x%04x", Short.valueOf(s), Integer.valueOf(OsConstants.ETH_P_IP));
            }
        }
        byte b2 = DHCP_DOMAIN_NAME;
        if (i > 1) {
            ipAddress = null;
        } else {
            if (byteBuffer.remaining() < 264) {
                throw new ParseException(DhcpErrorEvent.L3_TOO_SHORT, "L3 packet too short, %d < %d", Integer.valueOf(byteBuffer.remaining()), Integer.valueOf(MIN_PACKET_LENGTH_L3));
            }
            byte b3 = byteBuffer.get();
            int i3 = (b3 & 240) >> 4;
            if (i3 != 4) {
                throw new ParseException(DhcpErrorEvent.L3_NOT_IPV4, "Invalid IP version %d", Integer.valueOf(i3));
            }
            byteBuffer.get();
            byteBuffer.getShort();
            byteBuffer.getShort();
            byteBuffer.get();
            byteBuffer.get();
            byteBuffer.get();
            byte b4 = byteBuffer.get();
            byteBuffer.getShort();
            ipAddress = readIpAddress(byteBuffer);
            readIpAddress(byteBuffer);
            if (b4 != 17) {
                throw new ParseException(DhcpErrorEvent.L4_NOT_UDP, "Protocol not UDP: %d", Byte.valueOf(b4));
            }
            int i4 = (b3 & DHCP_DOMAIN_NAME) - 5;
            for (int i5 = 0; i5 < i4; i5++) {
                byteBuffer.getInt();
            }
            short s2 = byteBuffer.getShort();
            short s3 = byteBuffer.getShort();
            byteBuffer.getShort();
            byteBuffer.getShort();
            if (!isPacketToOrFromClient(s2, s3) && !isPacketServerToServer(s2, s3)) {
                throw new ParseException(DhcpErrorEvent.L4_WRONG_PORT, "Unexpected UDP ports %d->%d", Short.valueOf(s2), Short.valueOf(s3));
            }
        }
        if (i > 2 || byteBuffer.remaining() < 236) {
            throw new ParseException(DhcpErrorEvent.BOOTP_TOO_SHORT, "Invalid type or BOOTP packet too short, %d < %d", Integer.valueOf(byteBuffer.remaining()), Integer.valueOf(MIN_PACKET_LENGTH_BOOTP));
        }
        byteBuffer.get();
        byteBuffer.get();
        int length = byteBuffer.get() & DHCP_OPTION_END;
        byteBuffer.get();
        int i6 = byteBuffer.getInt();
        short s4 = byteBuffer.getShort();
        boolean z = (byteBuffer.getShort() & Short.MIN_VALUE) != 0;
        byte[] bArr = new byte[4];
        try {
            byteBuffer.get(bArr);
            Inet4Address inet4Address = (Inet4Address) Inet4Address.getByAddress(bArr);
            byteBuffer.get(bArr);
            Inet4Address inet4Address2 = (Inet4Address) Inet4Address.getByAddress(bArr);
            byteBuffer.get(bArr);
            Inet4Address inet4Address3 = (Inet4Address) Inet4Address.getByAddress(bArr);
            byteBuffer.get(bArr);
            Inet4Address inet4Address4 = (Inet4Address) Inet4Address.getByAddress(bArr);
            if (length > 16) {
                length = ETHER_BROADCAST.length;
            }
            byte[] bArr2 = new byte[length];
            byteBuffer.get(bArr2);
            byteBuffer.position(byteBuffer.position() + (16 - length) + 64 + 128);
            if (byteBuffer.remaining() < 4) {
                throw new ParseException(DhcpErrorEvent.DHCP_NO_COOKIE, "not a DHCP message", new Object[0]);
            }
            int i7 = byteBuffer.getInt();
            if (i7 != DHCP_MAGIC_COOKIE) {
                throw new ParseException(DhcpErrorEvent.DHCP_BAD_MAGIC_COOKIE, "Bad magic cookie 0x%08x, should be 0x%08x", Integer.valueOf(i7), Integer.valueOf(DHCP_MAGIC_COOKIE));
            }
            byte b5 = DHCP_OPTION_END;
            byte b6 = -1;
            Inet4Address ipAddress2 = null;
            String asciiString = null;
            String asciiString2 = null;
            Integer numValueOf = null;
            String asciiString3 = null;
            Short shValueOf = null;
            Inet4Address ipAddress3 = null;
            byte[] bArr3 = null;
            Inet4Address ipAddress4 = null;
            Inet4Address ipAddress5 = null;
            Short shValueOf2 = null;
            Integer numValueOf2 = null;
            Integer numValueOf3 = null;
            String asciiString4 = null;
            String asciiString5 = null;
            boolean z2 = true;
            while (byteBuffer.position() < byteBuffer.limit() && z2) {
                byte b7 = byteBuffer.get();
                if (b7 == b5) {
                    z2 = false;
                } else if (b7 == 0) {
                    continue;
                } else {
                    try {
                        int i8 = byteBuffer.get() & DHCP_OPTION_END;
                        if (b7 != 1) {
                            if (b7 == 3) {
                                i2 = 0;
                                while (i2 < i8) {
                                    arrayList2.add(readIpAddress(byteBuffer));
                                    i2 += 4;
                                }
                            } else if (b7 != b) {
                                if (b7 == 12) {
                                    asciiString2 = readAsciiString(byteBuffer, i8, false);
                                } else if (b7 != b2) {
                                    if (b7 == 26) {
                                        shValueOf = Short.valueOf(byteBuffer.getShort());
                                    } else if (b7 == 28) {
                                        ipAddress2 = readIpAddress(byteBuffer);
                                    } else if (b7 != 43) {
                                        switch (b7) {
                                            case 50:
                                                ipAddress3 = readIpAddress(byteBuffer);
                                                break;
                                            case 51:
                                                numValueOf = Integer.valueOf(byteBuffer.getInt());
                                                break;
                                            default:
                                                switch (b7) {
                                                    case 53:
                                                        b6 = byteBuffer.get();
                                                        i2 = 1;
                                                        break;
                                                    case MtkDhcp6Packet.MIN_PACKET_LENGTH_L2:
                                                        ipAddress4 = readIpAddress(byteBuffer);
                                                        break;
                                                    case 55:
                                                        byte[] bArr4 = new byte[i8];
                                                        byteBuffer.get(bArr4);
                                                        bArr3 = bArr4;
                                                        break;
                                                    case 56:
                                                        asciiString3 = readAsciiString(byteBuffer, i8, false);
                                                        break;
                                                    case 57:
                                                        shValueOf2 = Short.valueOf(byteBuffer.getShort());
                                                        break;
                                                    case 58:
                                                        numValueOf2 = Integer.valueOf(byteBuffer.getInt());
                                                        break;
                                                    case 59:
                                                        numValueOf3 = Integer.valueOf(byteBuffer.getInt());
                                                        break;
                                                    case 60:
                                                        asciiString4 = readAsciiString(byteBuffer, i8, true);
                                                        break;
                                                    case 61:
                                                        byteBuffer.get(new byte[i8]);
                                                        break;
                                                    default:
                                                        int i9 = 0;
                                                        for (int i10 = 0; i10 < i8; i10++) {
                                                            i9++;
                                                            byteBuffer.get();
                                                        }
                                                        i2 = i9;
                                                        break;
                                                }
                                                break;
                                        }
                                    } else {
                                        asciiString5 = readAsciiString(byteBuffer, i8, true);
                                    }
                                    i2 = 2;
                                } else {
                                    asciiString = readAsciiString(byteBuffer, i8, false);
                                }
                                i2 = i8;
                            } else {
                                i2 = 0;
                                while (i2 < i8) {
                                    arrayList.add(readIpAddress(byteBuffer));
                                    i2 += 4;
                                }
                            }
                            if (i2 == i8) {
                                throw new ParseException(DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.DHCP_INVALID_OPTION_LENGTH, b7), "Invalid length %d for option %d, expected %d", Integer.valueOf(i8), Byte.valueOf(b7), Integer.valueOf(i2));
                            }
                        } else {
                            ipAddress5 = readIpAddress(byteBuffer);
                        }
                        i2 = 4;
                        if (i2 == i8) {
                        }
                    } catch (BufferUnderflowException e) {
                        throw new ParseException(DhcpErrorEvent.errorCodeWithOption(DhcpErrorEvent.BUFFER_UNDERFLOW, b7), "BufferUnderflowException", new Object[0]);
                    }
                }
                b5 = DHCP_OPTION_END;
                b = 6;
                b2 = DHCP_DOMAIN_NAME;
            }
            if (b6 == -1) {
                throw new ParseException(DhcpErrorEvent.DHCP_NO_MSG_TYPE, "No DHCP message type option", new Object[0]);
            }
            if (b6 != 8) {
                switch (b6) {
                    case 1:
                        dhcpInformPacket = new DhcpDiscoverPacket(i6, s4, bArr2, z);
                        break;
                    case 2:
                        dhcpInformPacket = new DhcpOfferPacket(i6, s4, z, ipAddress, inet4Address, inet4Address2, bArr2);
                        break;
                    case 3:
                        dhcpInformPacket = new DhcpRequestPacket(i6, s4, inet4Address, bArr2, z);
                        break;
                    case 4:
                        dhcpInformPacket = new DhcpDeclinePacket(i6, s4, inet4Address, inet4Address2, inet4Address3, inet4Address4, bArr2);
                        break;
                    case 5:
                        dhcpInformPacket = new DhcpAckPacket(i6, s4, z, ipAddress, inet4Address, inet4Address2, bArr2);
                        break;
                    case 6:
                        dhcpInformPacket = new DhcpNakPacket(i6, s4, inet4Address, inet4Address2, inet4Address3, inet4Address4, bArr2);
                        break;
                    default:
                        throw new ParseException(DhcpErrorEvent.DHCP_UNKNOWN_MSG_TYPE, "Unimplemented DHCP type %d", Byte.valueOf(b6));
                }
            } else {
                dhcpInformPacket = new DhcpInformPacket(i6, s4, inet4Address, inet4Address2, inet4Address3, inet4Address4, bArr2);
            }
            dhcpInformPacket.mBroadcastAddress = ipAddress2;
            dhcpInformPacket.mDnsServers = arrayList;
            dhcpInformPacket.mDomainName = asciiString;
            dhcpInformPacket.mGateways = arrayList2;
            dhcpInformPacket.mHostName = asciiString2;
            dhcpInformPacket.mLeaseTime = numValueOf;
            dhcpInformPacket.mMessage = asciiString3;
            dhcpInformPacket.mMtu = shValueOf;
            dhcpInformPacket.mRequestedIp = ipAddress3;
            dhcpInformPacket.mRequestedParams = bArr3;
            dhcpInformPacket.mServerIdentifier = ipAddress4;
            dhcpInformPacket.mSubnetMask = ipAddress5;
            dhcpInformPacket.mMaxMessageSize = shValueOf2;
            dhcpInformPacket.mT1 = numValueOf2;
            dhcpInformPacket.mT2 = numValueOf3;
            dhcpInformPacket.mVendorId = asciiString4;
            dhcpInformPacket.mVendorInfo = asciiString5;
            return dhcpInformPacket;
        } catch (UnknownHostException e2) {
            throw new ParseException(DhcpErrorEvent.L3_INVALID_IP, "Invalid IPv4 address: %s", Arrays.toString(bArr));
        }
    }

    public static DhcpPacket decodeFullPacket(byte[] bArr, int i, int i2) throws ParseException {
        try {
            return decodeFullPacket(ByteBuffer.wrap(bArr, 0, i).order(ByteOrder.BIG_ENDIAN), i2);
        } catch (ParseException e) {
            throw e;
        } catch (Exception e2) {
            throw new ParseException(DhcpErrorEvent.PARSING_ERROR, e2.getMessage(), new Object[0]);
        }
    }

    public DhcpResults toDhcpResults() {
        int iNetmaskToPrefixLength;
        Inet4Address inet4Address = this.mYourIp;
        if (inet4Address.equals(Inet4Address.ANY)) {
            inet4Address = this.mClientIp;
            if (inet4Address.equals(Inet4Address.ANY)) {
                return null;
            }
        }
        if (this.mSubnetMask != null) {
            try {
                iNetmaskToPrefixLength = NetworkUtils.netmaskToPrefixLength(this.mSubnetMask);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            iNetmaskToPrefixLength = NetworkUtils.getImplicitNetmask(inet4Address);
        }
        DhcpResults dhcpResults = new DhcpResults();
        try {
            ((StaticIpConfiguration) dhcpResults).ipAddress = new LinkAddress(inet4Address, iNetmaskToPrefixLength);
            short sShortValue = 0;
            if (this.mGateways.size() > 0) {
                ((StaticIpConfiguration) dhcpResults).gateway = this.mGateways.get(0);
            }
            ((StaticIpConfiguration) dhcpResults).dnsServers.addAll(this.mDnsServers);
            ((StaticIpConfiguration) dhcpResults).domains = this.mDomainName;
            dhcpResults.serverAddress = this.mServerIdentifier;
            dhcpResults.vendorInfo = this.mVendorInfo;
            dhcpResults.leaseDuration = this.mLeaseTime != null ? this.mLeaseTime.intValue() : -1;
            if (this.mMtu != null && MIN_MTU <= this.mMtu.shortValue() && this.mMtu.shortValue() <= 1500) {
                sShortValue = this.mMtu.shortValue();
            }
            dhcpResults.mtu = sShortValue;
            return dhcpResults;
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

    public static ByteBuffer buildDiscoverPacket(int i, int i2, short s, byte[] bArr, boolean z, byte[] bArr2) {
        DhcpDiscoverPacket dhcpDiscoverPacket = new DhcpDiscoverPacket(i2, s, bArr, z);
        dhcpDiscoverPacket.mRequestedParams = bArr2;
        return dhcpDiscoverPacket.buildPacket(i, DHCP_SERVER, DHCP_CLIENT);
    }

    public static ByteBuffer buildOfferPacket(int i, int i2, boolean z, Inet4Address inet4Address, Inet4Address inet4Address2, byte[] bArr, Integer num, Inet4Address inet4Address3, Inet4Address inet4Address4, List<Inet4Address> list, List<Inet4Address> list2, Inet4Address inet4Address5, String str) {
        DhcpOfferPacket dhcpOfferPacket = new DhcpOfferPacket(i2, (short) 0, z, inet4Address, INADDR_ANY, inet4Address2, bArr);
        dhcpOfferPacket.mGateways = list;
        dhcpOfferPacket.mDnsServers = list2;
        dhcpOfferPacket.mLeaseTime = num;
        dhcpOfferPacket.mDomainName = str;
        dhcpOfferPacket.mServerIdentifier = inet4Address5;
        dhcpOfferPacket.mSubnetMask = inet4Address3;
        dhcpOfferPacket.mBroadcastAddress = inet4Address4;
        return dhcpOfferPacket.buildPacket(i, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildAckPacket(int i, int i2, boolean z, Inet4Address inet4Address, Inet4Address inet4Address2, byte[] bArr, Integer num, Inet4Address inet4Address3, Inet4Address inet4Address4, List<Inet4Address> list, List<Inet4Address> list2, Inet4Address inet4Address5, String str) {
        DhcpAckPacket dhcpAckPacket = new DhcpAckPacket(i2, (short) 0, z, inet4Address, INADDR_ANY, inet4Address2, bArr);
        dhcpAckPacket.mGateways = list;
        dhcpAckPacket.mDnsServers = list2;
        dhcpAckPacket.mLeaseTime = num;
        dhcpAckPacket.mDomainName = str;
        dhcpAckPacket.mSubnetMask = inet4Address3;
        dhcpAckPacket.mServerIdentifier = inet4Address5;
        dhcpAckPacket.mBroadcastAddress = inet4Address4;
        return dhcpAckPacket.buildPacket(i, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildNakPacket(int i, int i2, Inet4Address inet4Address, Inet4Address inet4Address2, byte[] bArr) {
        DhcpNakPacket dhcpNakPacket = new DhcpNakPacket(i2, (short) 0, inet4Address2, inet4Address, inet4Address, inet4Address, bArr);
        dhcpNakPacket.mMessage = "requested address not available";
        dhcpNakPacket.mRequestedIp = inet4Address2;
        return dhcpNakPacket.buildPacket(i, DHCP_CLIENT, DHCP_SERVER);
    }

    public static ByteBuffer buildRequestPacket(int i, int i2, short s, Inet4Address inet4Address, boolean z, byte[] bArr, Inet4Address inet4Address2, Inet4Address inet4Address3, byte[] bArr2, String str) {
        DhcpRequestPacket dhcpRequestPacket = new DhcpRequestPacket(i2, s, inet4Address, bArr, z);
        dhcpRequestPacket.mRequestedIp = inet4Address2;
        dhcpRequestPacket.mServerIdentifier = inet4Address3;
        dhcpRequestPacket.mHostName = str;
        dhcpRequestPacket.mRequestedParams = bArr2;
        return dhcpRequestPacket.buildPacket(i, DHCP_SERVER, DHCP_CLIENT);
    }
}
