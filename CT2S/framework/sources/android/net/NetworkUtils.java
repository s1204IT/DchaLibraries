package android.net;

import android.os.BatteryStats;
import android.os.Parcel;
import android.util.Log;
import android.util.Pair;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Locale;

public class NetworkUtils {
    public static final int RESET_ALL_ADDRESSES = 3;
    public static final int RESET_IPV4_ADDRESSES = 1;
    public static final int RESET_IPV6_ADDRESSES = 2;
    private static final String TAG = "NetworkUtils";

    public static native boolean bindProcessToNetwork(int i);

    public static native boolean bindProcessToNetworkForHostResolution(int i);

    public static native int bindSocketToNetwork(int i, int i2);

    public static native String getDhcpError();

    public static native boolean getIPv6Info(String str, DhcpResults dhcpResults);

    public static native int getNetworkBoundToProcess();

    public static native boolean protectFromVpn(int i);

    public static native boolean releaseDhcpLease(String str);

    public static native int resetConnections(String str, int i);

    public static native boolean runDhcp(String str, DhcpResults dhcpResults);

    public static native boolean runDhcpRenew(String str, DhcpResults dhcpResults);

    public static native boolean stopDhcp(String str);

    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = {(byte) (hostAddress & 255), (byte) ((hostAddress >> 8) & 255), (byte) ((hostAddress >> 16) & 255), (byte) ((hostAddress >> 24) & 255)};
        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    public static int inetAddressToInt(Inet4Address inetAddr) throws IllegalArgumentException {
        byte[] addr = inetAddr.getAddress();
        return ((addr[3] & BatteryStats.HistoryItem.CMD_NULL) << 24) | ((addr[2] & BatteryStats.HistoryItem.CMD_NULL) << 16) | ((addr[1] & BatteryStats.HistoryItem.CMD_NULL) << 8) | (addr[0] & BatteryStats.HistoryItem.CMD_NULL);
    }

    public static int prefixLengthToNetmaskInt(int prefixLength) throws IllegalArgumentException {
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("Invalid prefix length (0 <= prefix <= 32)");
        }
        int value = (-1) << (32 - prefixLength);
        return Integer.reverseBytes(value);
    }

    public static int netmaskIntToPrefixLength(int netmask) {
        return Integer.bitCount(netmask);
    }

    public static InetAddress numericToInetAddress(String addrString) throws IllegalArgumentException {
        return InetAddress.parseNumericAddress(addrString);
    }

    protected static void parcelInetAddress(Parcel parcel, InetAddress address, int flags) {
        byte[] addressArray = address != null ? address.getAddress() : null;
        parcel.writeByteArray(addressArray);
    }

    protected static InetAddress unparcelInetAddress(Parcel in) {
        byte[] addressArray = in.createByteArray();
        if (addressArray == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(addressArray);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static void maskRawAddress(byte[] array, int prefixLength) {
        if (prefixLength < 0 || prefixLength > array.length * 8) {
            throw new RuntimeException("IP address with " + array.length + " bytes has invalid prefix length " + prefixLength);
        }
        int offset = prefixLength / 8;
        int remainder = prefixLength % 8;
        byte mask = (byte) (255 << (8 - remainder));
        if (offset < array.length) {
            array[offset] = (byte) (array[offset] & mask);
        }
        while (true) {
            offset++;
            if (offset < array.length) {
                array[offset] = 0;
            } else {
                return;
            }
        }
    }

    public static InetAddress getNetworkPart(InetAddress address, int prefixLength) {
        byte[] array = address.getAddress();
        maskRawAddress(array, prefixLength);
        try {
            InetAddress netPart = InetAddress.getByAddress(array);
            return netPart;
        } catch (UnknownHostException e) {
            throw new RuntimeException("getNetworkPart error - " + e.toString());
        }
    }

    public static Pair<InetAddress, Integer> parseIpAndMask(String ipAndMaskString) {
        InetAddress address = null;
        int prefixLength = -1;
        try {
            String[] pieces = ipAndMaskString.split("/", 2);
            prefixLength = Integer.parseInt(pieces[1]);
            address = InetAddress.parseNumericAddress(pieces[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
        } catch (NumberFormatException e2) {
        } catch (IllegalArgumentException e3) {
        } catch (NullPointerException e4) {
        }
        if (address == null || prefixLength == -1) {
            throw new IllegalArgumentException("Invalid IP address and mask " + ipAndMaskString);
        }
        return new Pair<>(address, Integer.valueOf(prefixLength));
    }

    public static boolean addressTypeMatches(InetAddress left, InetAddress right) {
        return ((left instanceof Inet4Address) && (right instanceof Inet4Address)) || ((left instanceof Inet6Address) && (right instanceof Inet6Address));
    }

    public static InetAddress hexToInet6Address(String addrHexString) throws IllegalArgumentException {
        try {
            return numericToInetAddress(String.format(Locale.US, "%s:%s:%s:%s:%s:%s:%s:%s", addrHexString.substring(0, 4), addrHexString.substring(4, 8), addrHexString.substring(8, 12), addrHexString.substring(12, 16), addrHexString.substring(16, 20), addrHexString.substring(20, 24), addrHexString.substring(24, 28), addrHexString.substring(28, 32)));
        } catch (Exception e) {
            Log.e(TAG, "error in hexToInet6Address(" + addrHexString + "): " + e);
            throw new IllegalArgumentException(e);
        }
    }

    public static String[] makeStrings(Collection<InetAddress> addrs) {
        String[] result = new String[addrs.size()];
        int i = 0;
        for (InetAddress addr : addrs) {
            result[i] = addr.getHostAddress();
            i++;
        }
        return result;
    }

    public static String trimV4AddrZeros(String addr) {
        if (addr == null) {
            return null;
        }
        String[] octets = addr.split("\\.");
        if (octets.length == 4) {
            StringBuilder builder = new StringBuilder(16);
            for (int i = 0; i < 4; i++) {
                try {
                    if (octets[i].length() <= 3) {
                        builder.append(Integer.parseInt(octets[i]));
                        if (i < 3) {
                            builder.append('.');
                        }
                    } else {
                        return addr;
                    }
                } catch (NumberFormatException e) {
                    return addr;
                }
            }
            String result = builder.toString();
            return result;
        }
        return addr;
    }
}
