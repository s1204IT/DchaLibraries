package com.mediatek.net.dhcp;

import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.util.Log;
import com.android.internal.util.HexDump;
import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

abstract class MtkDhcp6Packet {
    protected static final short CLIENT_ID_ETHER = 3;
    public static final int DHCPV6_SC_NOADDRAVAIL = 2;
    public static final int DHCPV6_SC_SUCCESS = 0;
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
    protected static final short OPTION_STATUS_CODE = 13;
    protected static final String TAG = "MtkDhcp6Packet";
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
    protected int mStatusCode;
    protected Inet6Address mSubnetMask;
    protected Integer mT1;
    protected Integer mT2;
    protected final byte[] mTransId;
    protected String mVendorId;
    protected static final Boolean DBG = false;
    public static final Inet6Address INADDR_ANY = (Inet6Address) Inet6Address.ANY;
    public static final Inet6Address INADDR_BROADCAST_ROUTER = (Inet6Address) NetworkUtils.hexToInet6Address("FF020000000000000000000000010002");
    public static final byte[] ETHER_BROADCAST = {-1, -1, -1, -1, -1, -1};
    private static final byte[] IPV6_VERSION_HEADER = {96, 0, 0, 0};

    public abstract ByteBuffer buildPacket(short s, short s2);

    abstract void finishPacket(ByteBuffer byteBuffer);

    protected MtkDhcp6Packet(byte[] bArr, Inet6Address inet6Address, Inet6Address inet6Address2, Inet6Address inet6Address3, byte[] bArr2) {
        this.mTransId = bArr;
        this.mServerIp = inet6Address;
        this.mNextIp = inet6Address2;
        this.mRelayIp = inet6Address3;
        this.mClientMac = bArr2;
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
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(14);
        byteBufferAllocate.clear();
        byteBufferAllocate.order(ByteOrder.BIG_ENDIAN);
        byteBufferAllocate.putShort(OPTION_CLIENTID);
        byteBufferAllocate.putShort(OPTION_CLIENTID);
        byteBufferAllocate.put(MtkDhcp6Client.getTimeStamp());
        byteBufferAllocate.put(this.mClientMac);
        return byteBufferAllocate.array();
    }

    private byte[] getIaNa() {
        return new byte[]{14, 0, DHCP_MESSAGE_TYPE_RELEASE, -54, 0, 0, 0, 0, 0, 0, 0, 0};
    }

    protected void fillInPacket(Inet6Address inet6Address, Inet6Address inet6Address2, short s, short s2, ByteBuffer byteBuffer, byte b) {
        inet6Address.getAddress();
        inet6Address2.getAddress();
        byteBuffer.clear();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byteBuffer.put(b);
        byteBuffer.put(this.mTransId);
        finishPacket(byteBuffer);
        if (DBG.booleanValue()) {
            Log.d(TAG, HexDump.toHexString(byteBuffer.array()));
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

    protected static void addTlv(ByteBuffer byteBuffer, short s, byte b) {
        byteBuffer.putShort(s);
        byteBuffer.putShort(OPTION_CLIENTID);
        byteBuffer.put(b);
    }

    protected static void addTlv(ByteBuffer byteBuffer, short s, byte[] bArr) {
        if (bArr != null) {
            if (bArr.length > 65025) {
                throw new IllegalArgumentException("DHCP option too long: " + bArr.length + " vs. " + MAX_OPTION_LEN);
            }
            byteBuffer.putShort(s);
            byteBuffer.putShort((short) bArr.length);
            byteBuffer.put(bArr);
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, short s, short[] sArr) {
        if (sArr != null) {
            if (sArr.length > 65025) {
                throw new IllegalArgumentException("DHCP option too long: " + sArr.length + " vs. " + MAX_OPTION_LEN);
            }
            byte[] bArr = new byte[sArr.length * 2];
            ByteBuffer.wrap(bArr).order(ByteOrder.BIG_ENDIAN).asShortBuffer().put(sArr);
            addTlv(byteBuffer, s, bArr);
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, short s, Inet6Address inet6Address) {
        if (inet6Address != null) {
            addTlv(byteBuffer, s, inet6Address.getAddress());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, short s, List<Inet6Address> list) {
        if (list == null || list.size() == 0) {
            return;
        }
        int size = 4 * list.size();
        if (size > 65025) {
            throw new IllegalArgumentException("DHCP option too long: " + size + " vs. " + MAX_OPTION_LEN);
        }
        byteBuffer.putShort(s);
        byteBuffer.put((byte) size);
        Iterator<Inet6Address> it = list.iterator();
        while (it.hasNext()) {
            byteBuffer.put(it.next().getAddress());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, short s, Short sh) {
        if (sh != null) {
            byteBuffer.putShort(s);
            byteBuffer.putShort(OPTION_SERVERID);
            byteBuffer.putShort(sh.shortValue());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, short s, Integer num) {
        if (num != null) {
            byteBuffer.putShort(s);
            byteBuffer.putShort(OPTION_IA_TA);
            byteBuffer.putInt(num.intValue());
        }
    }

    protected static void addTlv(ByteBuffer byteBuffer, short s, String str) {
        try {
            addTlv(byteBuffer, s, str.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("String is not US-ASCII: " + str);
        }
    }

    protected void addCommonClientTlvs(ByteBuffer byteBuffer) {
        addTlv(byteBuffer, (short) 3, getIaNa());
        addTlv(byteBuffer, OPTION_ELAPSED_TIME, Short.valueOf(DHCP_OPTION_PAD));
    }

    public static String macToString(byte[] bArr) {
        String str = "";
        if (bArr == null) {
            return "";
        }
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

    private static Inet6Address readIpAddress(ByteBuffer byteBuffer) {
        Inet6Address inet6Address;
        byte[] bArr = new byte[16];
        byteBuffer.get(bArr);
        try {
            inet6Address = (Inet6Address) Inet6Address.getByAddress(bArr);
        } catch (UnknownHostException e) {
            inet6Address = null;
        }
        if (DBG.booleanValue()) {
            Log.i(TAG, "readIpAddress:" + inet6Address);
        }
        return inet6Address;
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

    public static MtkDhcp6Packet decodeFullPacket(ByteBuffer byteBuffer) {
        MtkDhcp6Packet mtkDhcp6AdvertisePacket;
        boolean z;
        int i;
        ArrayList arrayList = new ArrayList();
        byteBuffer.order(ByteOrder.BIG_ENDIAN);
        byte b = byteBuffer.get();
        byte[] bArr = new byte[3];
        byteBuffer.get(bArr);
        boolean z2 = true;
        Inet6Address ipAddress = null;
        byte[] bArr2 = null;
        byte[] bArr3 = null;
        Integer numValueOf = null;
        Integer numValueOf2 = null;
        int i2 = 0;
        while (byteBuffer.position() < byteBuffer.limit() && z2) {
            try {
                short s = byteBuffer.getShort();
                if (DBG.booleanValue()) {
                    Log.d(TAG, "optionType:" + ((int) s));
                }
                if (s == 255) {
                    z2 = false;
                } else if (s != 0) {
                    int i3 = byteBuffer.getShort() & 65535;
                    if (s != 13) {
                        if (s != 23) {
                            switch (s) {
                                case 1:
                                    z = z2;
                                    byte[] bArr4 = new byte[i3];
                                    byteBuffer.get(bArr4);
                                    ByteBuffer byteBufferWrap = ByteBuffer.wrap(bArr4);
                                    short s2 = byteBufferWrap.getShort();
                                    if (s2 == 1 || s2 == 3) {
                                        if (byteBufferWrap.getShort() == 1) {
                                            if (s2 == 1) {
                                                byteBufferWrap.getInt();
                                            }
                                            byte[] bArr5 = new byte[6];
                                            byteBufferWrap.get(bArr5);
                                            bArr2 = bArr5;
                                        }
                                    }
                                    break;
                                case 2:
                                    z = z2;
                                    bArr3 = new byte[i3];
                                    byteBuffer.get(bArr3);
                                    i = i3;
                                    break;
                                case 3:
                                    byte[] bArr6 = new byte[i3];
                                    byteBuffer.get(bArr6);
                                    ByteBuffer byteBufferWrap2 = ByteBuffer.wrap(bArr6);
                                    numValueOf = Integer.valueOf(byteBufferWrap2.getInt(4));
                                    numValueOf2 = Integer.valueOf(byteBufferWrap2.getInt(8));
                                    StringBuilder sb = new StringBuilder();
                                    z = z2;
                                    sb.append("T1:");
                                    sb.append(numValueOf);
                                    Log.d(TAG, sb.toString());
                                    Log.d(TAG, "T2:" + numValueOf2);
                                    if (i3 > 12) {
                                        byteBufferWrap2.position(12);
                                        if (byteBufferWrap2.getShort() == 5) {
                                            byteBufferWrap2.getShort();
                                            ipAddress = readIpAddress(byteBufferWrap2);
                                            i = i3;
                                        } else {
                                            i = 0;
                                        }
                                    } else {
                                        i = 0;
                                    }
                                    break;
                                default:
                                    int i4 = 0;
                                    for (int i5 = 0; i5 < i3; i5++) {
                                        i4++;
                                        byteBuffer.get();
                                    }
                                    z = z2;
                                    i = i4;
                                    break;
                            }
                        } else {
                            z = z2;
                            i = 0;
                            while (i < i3) {
                                arrayList.add(readIpAddress(byteBuffer));
                                i += 16;
                            }
                        }
                        if (DBG.booleanValue()) {
                            Log.d(TAG, "expectedLen:" + i);
                            Log.d(TAG, "optionLen:" + i3);
                        }
                        if (i == i3) {
                            Log.e(TAG, "optionType:" + ((int) s));
                            return null;
                        }
                        z2 = z;
                    } else {
                        z = z2;
                        i2 = byteBuffer.getShort() & 65535;
                    }
                    i = i3;
                    if (DBG.booleanValue()) {
                    }
                    if (i == i3) {
                    }
                }
            } catch (BufferUnderflowException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e2) {
                e2.printStackTrace();
                return null;
            }
        }
        if (b == -1) {
            return null;
        }
        if (b == 2) {
            mtkDhcp6AdvertisePacket = new MtkDhcp6AdvertisePacket(bArr, null, ipAddress, bArr2);
        } else {
            if (b != 7) {
                Log.e(TAG, "Unimplemented type: " + ((int) b));
                return null;
            }
            mtkDhcp6AdvertisePacket = new MtkDhcp6ReplyPacket(bArr, null, ipAddress, bArr2);
        }
        mtkDhcp6AdvertisePacket.mRequestedIp = ipAddress;
        mtkDhcp6AdvertisePacket.mDnsServers = arrayList;
        mtkDhcp6AdvertisePacket.mServerIdentifier = bArr3;
        mtkDhcp6AdvertisePacket.mT1 = numValueOf;
        mtkDhcp6AdvertisePacket.mT2 = numValueOf2;
        mtkDhcp6AdvertisePacket.mLeaseTime = numValueOf;
        mtkDhcp6AdvertisePacket.mStatusCode = i2;
        return mtkDhcp6AdvertisePacket;
    }

    public static MtkDhcp6Packet decodeFullPacket(byte[] bArr, int i) {
        return decodeFullPacket(ByteBuffer.wrap(bArr, 0, i).order(ByteOrder.BIG_ENDIAN));
    }

    public DhcpResults toDhcpResults() {
        Inet6Address inet6Address = this.mRequestedIp;
        if (inet6Address == null || inet6Address.equals(Inet6Address.ANY)) {
            return null;
        }
        DhcpResults dhcpResults = new DhcpResults();
        try {
            ((StaticIpConfiguration) dhcpResults).ipAddress = new LinkAddress(inet6Address, 64);
            ((StaticIpConfiguration) dhcpResults).gateway = this.mGateway;
            ((StaticIpConfiguration) dhcpResults).dnsServers.addAll(this.mDnsServers);
            ((StaticIpConfiguration) dhcpResults).domains = this.mDomainName;
            dhcpResults.serverAddress = null;
            dhcpResults.leaseDuration = this.mLeaseTime != null ? this.mLeaseTime.intValue() : -1;
            return dhcpResults;
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

    public static ByteBuffer buildSolicitPacket(byte[] bArr, short s, byte[] bArr2, short[] sArr) {
        MtkDhcp6SolicitPacket mtkDhcp6SolicitPacket = new MtkDhcp6SolicitPacket(bArr, bArr2);
        mtkDhcp6SolicitPacket.mRequestedParams = sArr;
        return mtkDhcp6SolicitPacket.buildPacket(DHCP_SERVER, DHCP_CLIENT);
    }

    public static ByteBuffer buildRequestPacket(byte[] bArr, short s, Inet6Address inet6Address, byte[] bArr2, Inet6Address inet6Address2, byte[] bArr3, short[] sArr) {
        MtkDhcp6RequestPacket mtkDhcp6RequestPacket = new MtkDhcp6RequestPacket(bArr, bArr2);
        mtkDhcp6RequestPacket.mRequestedIp = inet6Address2;
        mtkDhcp6RequestPacket.mServerIdentifier = bArr3;
        mtkDhcp6RequestPacket.mRequestedParams = sArr;
        return mtkDhcp6RequestPacket.buildPacket(DHCP_SERVER, DHCP_CLIENT);
    }

    public static ByteBuffer buildInfoRequestPacket(byte[] bArr, short s, byte[] bArr2, short[] sArr) {
        MtkDhcp6InfoRequestPacket mtkDhcp6InfoRequestPacket = new MtkDhcp6InfoRequestPacket(bArr, bArr2);
        mtkDhcp6InfoRequestPacket.mRequestedParams = sArr;
        return mtkDhcp6InfoRequestPacket.buildPacket(DHCP_SERVER, DHCP_CLIENT);
    }

    public static ByteBuffer buildRenewPacket(byte[] bArr, short s, Inet6Address inet6Address, boolean z, byte[] bArr2, Inet6Address inet6Address2, byte[] bArr3, byte[] bArr4) {
        MtkDhcp6RenewPacket mtkDhcp6RenewPacket = new MtkDhcp6RenewPacket(bArr, bArr2);
        mtkDhcp6RenewPacket.mRequestedIp = inet6Address2;
        mtkDhcp6RenewPacket.mServerIdentifier = bArr3;
        return mtkDhcp6RenewPacket.buildPacket(DHCP_SERVER, DHCP_CLIENT);
    }

    public static ByteBuffer buildRebindPacket(byte[] bArr, short s, Inet6Address inet6Address, boolean z, byte[] bArr2, Inet6Address inet6Address2, byte[] bArr3, byte[] bArr4) {
        MtkDhcp6RebindPacket mtkDhcp6RebindPacket = new MtkDhcp6RebindPacket(bArr, bArr2);
        mtkDhcp6RebindPacket.mRequestedIp = inet6Address2;
        mtkDhcp6RebindPacket.mServerIdentifier = bArr3;
        return mtkDhcp6RebindPacket.buildPacket(DHCP_SERVER, DHCP_CLIENT);
    }
}
