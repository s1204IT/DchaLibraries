package android.net.util;

import android.net.MacAddress;
import android.net.dhcp.DhcpPacket;
import android.support.v4.os.EnvironmentCompat;
import android.system.OsConstants;
import com.android.bluetooth.map.BluetoothMapContentObserver;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.StringJoiner;

public class ConnectivityPacketSummary {
    private static final String TAG = ConnectivityPacketSummary.class.getSimpleName();
    private final byte[] mBytes;
    private final byte[] mHwAddr;
    private final int mLength;
    private final ByteBuffer mPacket;
    private final String mSummary;

    public static String summarize(MacAddress macAddress, byte[] bArr) {
        return summarize(macAddress, bArr, bArr.length);
    }

    public static String summarize(MacAddress macAddress, byte[] bArr, int i) {
        if (macAddress == null || bArr == null) {
            return null;
        }
        return new ConnectivityPacketSummary(macAddress, bArr, Math.min(i, bArr.length)).toString();
    }

    private ConnectivityPacketSummary(MacAddress macAddress, byte[] bArr, int i) {
        this.mHwAddr = macAddress.toByteArray();
        this.mBytes = bArr;
        this.mLength = Math.min(i, this.mBytes.length);
        this.mPacket = ByteBuffer.wrap(this.mBytes, 0, this.mLength);
        this.mPacket.order(ByteOrder.BIG_ENDIAN);
        StringJoiner stringJoiner = new StringJoiner(" ");
        parseEther(stringJoiner);
        this.mSummary = stringJoiner.toString();
    }

    public String toString() {
        return this.mSummary;
    }

    private void parseEther(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 14) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
            return;
        }
        this.mPacket.position(6);
        ByteBuffer byteBuffer = (ByteBuffer) this.mPacket.slice().limit(6);
        stringJoiner.add(ByteBuffer.wrap(this.mHwAddr).equals(byteBuffer) ? "TX" : "RX");
        stringJoiner.add(getMacAddressString(byteBuffer));
        this.mPacket.position(0);
        stringJoiner.add(">").add(getMacAddressString((ByteBuffer) this.mPacket.slice().limit(6)));
        this.mPacket.position(12);
        int iAsUint = NetworkConstants.asUint(this.mPacket.getShort());
        if (iAsUint == 2048) {
            stringJoiner.add("ipv4");
            parseIPv4(stringJoiner);
        } else if (iAsUint == 2054) {
            stringJoiner.add("arp");
            parseARP(stringJoiner);
        } else if (iAsUint == 34525) {
            stringJoiner.add("ipv6");
            parseIPv6(stringJoiner);
        } else {
            stringJoiner.add("ethtype").add(NetworkConstants.asString(iAsUint));
        }
    }

    private void parseARP(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 28) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
            return;
        }
        if (NetworkConstants.asUint(this.mPacket.getShort()) != 1 || NetworkConstants.asUint(this.mPacket.getShort()) != 2048 || NetworkConstants.asUint(this.mPacket.get()) != 6 || NetworkConstants.asUint(this.mPacket.get()) != 4) {
            stringJoiner.add("unexpected header");
            return;
        }
        int iAsUint = NetworkConstants.asUint(this.mPacket.getShort());
        String macAddressString = getMacAddressString(this.mPacket);
        String iPv4AddressString = getIPv4AddressString(this.mPacket);
        getMacAddressString(this.mPacket);
        String iPv4AddressString2 = getIPv4AddressString(this.mPacket);
        if (iAsUint == 1) {
            stringJoiner.add("who-has").add(iPv4AddressString2);
        } else if (iAsUint == 2) {
            stringJoiner.add("reply").add(iPv4AddressString).add(macAddressString);
        } else {
            stringJoiner.add("unknown opcode").add(NetworkConstants.asString(iAsUint));
        }
    }

    private void parseIPv4(StringJoiner stringJoiner) {
        if (!this.mPacket.hasRemaining()) {
            stringJoiner.add("runt");
            return;
        }
        int iPosition = this.mPacket.position();
        int i = (this.mPacket.get(iPosition) & 15) * 4;
        if (this.mPacket.remaining() < i || this.mPacket.remaining() < 20) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
            return;
        }
        int i2 = i + iPosition;
        this.mPacket.position(iPosition + 6);
        boolean z = (NetworkConstants.asUint(this.mPacket.getShort()) & NetworkConstants.IPV4_FRAGMENT_MASK) != 0;
        this.mPacket.position(iPosition + 9);
        int iAsUint = NetworkConstants.asUint(this.mPacket.get());
        this.mPacket.position(iPosition + 12);
        String iPv4AddressString = getIPv4AddressString(this.mPacket);
        this.mPacket.position(iPosition + 16);
        stringJoiner.add(iPv4AddressString).add(">").add(getIPv4AddressString(this.mPacket));
        this.mPacket.position(i2);
        if (iAsUint == OsConstants.IPPROTO_UDP) {
            stringJoiner.add("udp");
            if (!z) {
                parseUDP(stringJoiner);
                return;
            } else {
                stringJoiner.add("fragment");
                return;
            }
        }
        stringJoiner.add("proto").add(NetworkConstants.asString(iAsUint));
        if (z) {
            stringJoiner.add("fragment");
        }
    }

    private void parseIPv6(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 40) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
            return;
        }
        int iPosition = this.mPacket.position();
        this.mPacket.position(iPosition + 6);
        int iAsUint = NetworkConstants.asUint(this.mPacket.get());
        this.mPacket.position(iPosition + 8);
        String iPv6AddressString = getIPv6AddressString(this.mPacket);
        stringJoiner.add(iPv6AddressString).add(">").add(getIPv6AddressString(this.mPacket));
        this.mPacket.position(iPosition + 40);
        if (iAsUint == OsConstants.IPPROTO_ICMPV6) {
            stringJoiner.add("icmp6");
            parseICMPv6(stringJoiner);
        } else {
            stringJoiner.add("proto").add(NetworkConstants.asString(iAsUint));
        }
    }

    private void parseICMPv6(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 4) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
        }
        int iAsUint = NetworkConstants.asUint(this.mPacket.get());
        int iAsUint2 = NetworkConstants.asUint(this.mPacket.get());
        this.mPacket.getShort();
        switch (iAsUint) {
            case 133:
                stringJoiner.add("rs");
                parseICMPv6RouterSolicitation(stringJoiner);
                break;
            case 134:
                stringJoiner.add("ra");
                parseICMPv6RouterAdvertisement(stringJoiner);
                break;
            case 135:
                stringJoiner.add("ns");
                parseICMPv6NeighborMessage(stringJoiner);
                break;
            case 136:
                stringJoiner.add("na");
                parseICMPv6NeighborMessage(stringJoiner);
                break;
            default:
                stringJoiner.add(BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE).add(NetworkConstants.asString(iAsUint));
                stringJoiner.add("code").add(NetworkConstants.asString(iAsUint2));
                break;
        }
    }

    private void parseICMPv6RouterSolicitation(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 4) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
        } else {
            this.mPacket.position(this.mPacket.position() + 4);
            parseICMPv6NeighborDiscoveryOptions(stringJoiner);
        }
    }

    private void parseICMPv6RouterAdvertisement(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 12) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
        } else {
            this.mPacket.position(this.mPacket.position() + 12);
            parseICMPv6NeighborDiscoveryOptions(stringJoiner);
        }
    }

    private void parseICMPv6NeighborMessage(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 20) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
            return;
        }
        this.mPacket.position(this.mPacket.position() + 4);
        stringJoiner.add(getIPv6AddressString(this.mPacket));
        parseICMPv6NeighborDiscoveryOptions(stringJoiner);
    }

    private void parseICMPv6NeighborDiscoveryOptions(StringJoiner stringJoiner) {
        while (this.mPacket.remaining() >= 8) {
            int iAsUint = NetworkConstants.asUint(this.mPacket.get());
            int iAsUint2 = (NetworkConstants.asUint(this.mPacket.get()) * 8) - 2;
            if (iAsUint2 < 0 || iAsUint2 > this.mPacket.remaining()) {
                stringJoiner.add("<malformed>");
                return;
            }
            int iPosition = this.mPacket.position();
            if (iAsUint != 5) {
                switch (iAsUint) {
                    case 1:
                        stringJoiner.add("slla");
                        stringJoiner.add(getMacAddressString(this.mPacket));
                        break;
                    case 2:
                        stringJoiner.add("tlla");
                        stringJoiner.add(getMacAddressString(this.mPacket));
                        break;
                }
            } else {
                stringJoiner.add("mtu");
                this.mPacket.getShort();
                stringJoiner.add(NetworkConstants.asString(this.mPacket.getInt()));
            }
            this.mPacket.position(iPosition + iAsUint2);
        }
    }

    private void parseUDP(StringJoiner stringJoiner) {
        if (this.mPacket.remaining() < 8) {
            stringJoiner.add("runt:").add(NetworkConstants.asString(this.mPacket.remaining()));
            return;
        }
        int iPosition = this.mPacket.position();
        int iAsUint = NetworkConstants.asUint(this.mPacket.getShort());
        int iAsUint2 = NetworkConstants.asUint(this.mPacket.getShort());
        stringJoiner.add(NetworkConstants.asString(iAsUint)).add(">").add(NetworkConstants.asString(iAsUint2));
        this.mPacket.position(iPosition + 8);
        if (iAsUint == 68 || iAsUint2 == 68) {
            stringJoiner.add("dhcp4");
            parseDHCPv4(stringJoiner);
        }
    }

    private void parseDHCPv4(StringJoiner stringJoiner) {
        try {
            stringJoiner.add(DhcpPacket.decodeFullPacket(this.mBytes, this.mLength, 0).toString());
        } catch (DhcpPacket.ParseException e) {
            stringJoiner.add("parse error: " + e);
        }
    }

    private static String getIPv4AddressString(ByteBuffer byteBuffer) {
        return getIpAddressString(byteBuffer, 4);
    }

    private static String getIPv6AddressString(ByteBuffer byteBuffer) {
        return getIpAddressString(byteBuffer, 16);
    }

    private static String getIpAddressString(ByteBuffer byteBuffer, int i) {
        if (byteBuffer == null || byteBuffer.remaining() < i) {
            return "invalid";
        }
        byte[] bArr = new byte[i];
        byteBuffer.get(bArr, 0, i);
        try {
            return InetAddress.getByAddress(bArr).getHostAddress();
        } catch (UnknownHostException e) {
            return EnvironmentCompat.MEDIA_UNKNOWN;
        }
    }

    private static String getMacAddressString(ByteBuffer byteBuffer) {
        if (byteBuffer == null || byteBuffer.remaining() < 6) {
            return "invalid";
        }
        byte[] bArr = new byte[6];
        int i = 0;
        byteBuffer.get(bArr, 0, bArr.length);
        Object[] objArr = new Object[bArr.length];
        int length = bArr.length;
        int i2 = 0;
        while (i < length) {
            objArr[i2] = new Byte(bArr[i]);
            i++;
            i2++;
        }
        return String.format("%02x:%02x:%02x:%02x:%02x:%02x", objArr);
    }
}
