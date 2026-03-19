package android.net;

import android.os.Parcel;
import android.util.Log;
import android.util.Pair;
import java.io.FileDescriptor;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    public static native void attachControlPacketFilter(FileDescriptor fileDescriptor, int i) throws SocketException;

    public static native void attachDhcpFilter(FileDescriptor fileDescriptor) throws SocketException;

    public static native void attachRaFilter(FileDescriptor fileDescriptor, int i) throws SocketException;

    public static native boolean bindProcessToNetwork(int i);

    @Deprecated
    public static native boolean bindProcessToNetworkForHostResolution(int i);

    public static native int bindSocketToNetwork(int i, int i2);

    public static native int getBoundNetworkForProcess();

    public static native boolean protectFromVpn(int i);

    public static native boolean queryUserAccess(int i, int i2);

    public static native void setupRaSocket(FileDescriptor fileDescriptor, int i) throws SocketException;

    public static boolean protectFromVpn(FileDescriptor fileDescriptor) {
        return protectFromVpn(fileDescriptor.getInt$());
    }

    public static InetAddress intToInetAddress(int i) {
        try {
            return InetAddress.getByAddress(new byte[]{(byte) (255 & i), (byte) ((i >> 8) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 24) & 255)});
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    public static int inetAddressToInt(Inet4Address inet4Address) throws IllegalArgumentException {
        byte[] address = inet4Address.getAddress();
        return (address[0] & 255) | ((address[3] & 255) << 24) | ((address[2] & 255) << 16) | ((address[1] & 255) << 8);
    }

    public static int prefixLengthToNetmaskInt(int i) throws IllegalArgumentException {
        if (i < 0 || i > 32) {
            throw new IllegalArgumentException("Invalid prefix length (0 <= prefix <= 32)");
        }
        return Integer.reverseBytes((-1) << (32 - i));
    }

    public static int netmaskIntToPrefixLength(int i) {
        return Integer.bitCount(i);
    }

    public static int netmaskToPrefixLength(Inet4Address inet4Address) {
        int iReverseBytes = Integer.reverseBytes(inetAddressToInt(inet4Address));
        int iBitCount = Integer.bitCount(iReverseBytes);
        if (Integer.numberOfTrailingZeros(iReverseBytes) != 32 - iBitCount) {
            throw new IllegalArgumentException("Non-contiguous netmask: " + Integer.toHexString(iReverseBytes));
        }
        return iBitCount;
    }

    public static InetAddress numericToInetAddress(String str) throws IllegalArgumentException {
        return InetAddress.parseNumericAddress(str);
    }

    protected static void parcelInetAddress(Parcel parcel, InetAddress inetAddress, int i) {
        parcel.writeByteArray(inetAddress != null ? inetAddress.getAddress() : null);
    }

    protected static InetAddress unparcelInetAddress(Parcel parcel) {
        byte[] bArrCreateByteArray = parcel.createByteArray();
        if (bArrCreateByteArray == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(bArrCreateByteArray);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static void maskRawAddress(byte[] bArr, int i) {
        if (i < 0 || i > bArr.length * 8) {
            throw new RuntimeException("IP address with " + bArr.length + " bytes has invalid prefix length " + i);
        }
        int i2 = i / 8;
        byte b = (byte) (255 << (8 - (i % 8)));
        if (i2 < bArr.length) {
            bArr[i2] = (byte) (b & bArr[i2]);
        }
        while (true) {
            i2++;
            if (i2 < bArr.length) {
                bArr[i2] = 0;
            } else {
                return;
            }
        }
    }

    public static InetAddress getNetworkPart(InetAddress inetAddress, int i) {
        byte[] address = inetAddress.getAddress();
        maskRawAddress(address, i);
        try {
            return InetAddress.getByAddress(address);
        } catch (UnknownHostException e) {
            throw new RuntimeException("getNetworkPart error - " + e.toString());
        }
    }

    public static int getImplicitNetmask(Inet4Address inet4Address) {
        int i = inet4Address.getAddress()[0] & 255;
        if (i < 128) {
            return 8;
        }
        if (i < 192) {
            return 16;
        }
        if (i < 224) {
            return 24;
        }
        return 32;
    }

    public static Pair<InetAddress, Integer> parseIpAndMask(String str) {
        int i;
        InetAddress numericAddress = null;
        try {
            String[] strArrSplit = str.split("/", 2);
            i = Integer.parseInt(strArrSplit[1]);
            try {
                numericAddress = InetAddress.parseNumericAddress(strArrSplit[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
            } catch (NullPointerException e2) {
            } catch (NumberFormatException e3) {
            } catch (IllegalArgumentException e4) {
            }
        } catch (ArrayIndexOutOfBoundsException e5) {
            i = -1;
        } catch (NullPointerException e6) {
            i = -1;
        } catch (NumberFormatException e7) {
            i = -1;
        } catch (IllegalArgumentException e8) {
            i = -1;
        }
        if (numericAddress == null || i == -1) {
            throw new IllegalArgumentException("Invalid IP address and mask " + str);
        }
        return new Pair<>(numericAddress, Integer.valueOf(i));
    }

    public static boolean addressTypeMatches(InetAddress inetAddress, InetAddress inetAddress2) {
        return ((inetAddress instanceof Inet4Address) && (inetAddress2 instanceof Inet4Address)) || ((inetAddress instanceof Inet6Address) && (inetAddress2 instanceof Inet6Address));
    }

    public static InetAddress hexToInet6Address(String str) throws IllegalArgumentException {
        try {
            return numericToInetAddress(String.format(Locale.US, "%s:%s:%s:%s:%s:%s:%s:%s", str.substring(0, 4), str.substring(4, 8), str.substring(8, 12), str.substring(12, 16), str.substring(16, 20), str.substring(20, 24), str.substring(24, 28), str.substring(28, 32)));
        } catch (Exception e) {
            Log.e(TAG, "error in hexToInet6Address(" + str + "): " + e);
            throw new IllegalArgumentException(e);
        }
    }

    public static String[] makeStrings(Collection<InetAddress> collection) {
        String[] strArr = new String[collection.size()];
        Iterator<InetAddress> it = collection.iterator();
        int i = 0;
        while (it.hasNext()) {
            strArr[i] = it.next().getHostAddress();
            i++;
        }
        return strArr;
    }

    public static String trimV4AddrZeros(String str) {
        if (str == null) {
            return null;
        }
        String[] strArrSplit = str.split("\\.");
        if (strArrSplit.length != 4) {
            return str;
        }
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 4; i++) {
            try {
                if (strArrSplit[i].length() > 3) {
                    return str;
                }
                sb.append(Integer.parseInt(strArrSplit[i]));
                if (i < 3) {
                    sb.append('.');
                }
            } catch (NumberFormatException e) {
                return str;
            }
        }
        return sb.toString();
    }

    private static TreeSet<IpPrefix> deduplicatePrefixSet(TreeSet<IpPrefix> treeSet) {
        TreeSet<IpPrefix> treeSet2 = new TreeSet<>(treeSet.comparator());
        for (IpPrefix ipPrefix : treeSet) {
            Iterator<IpPrefix> it = treeSet2.iterator();
            while (true) {
                if (it.hasNext()) {
                    if (it.next().containsPrefix(ipPrefix)) {
                        break;
                    }
                } else {
                    treeSet2.add(ipPrefix);
                    break;
                }
            }
        }
        return treeSet2;
    }

    public static long routedIPv4AddressCount(TreeSet<IpPrefix> treeSet) {
        long prefixLength = 0;
        for (IpPrefix ipPrefix : deduplicatePrefixSet(treeSet)) {
            if (!ipPrefix.isIPv4()) {
                Log.wtf(TAG, "Non-IPv4 prefix in routedIPv4AddressCount");
            }
            prefixLength += 1 << (32 - ipPrefix.getPrefixLength());
        }
        return prefixLength;
    }

    public static BigInteger routedIPv6AddressCount(TreeSet<IpPrefix> treeSet) {
        BigInteger bigIntegerAdd = BigInteger.ZERO;
        for (IpPrefix ipPrefix : deduplicatePrefixSet(treeSet)) {
            if (!ipPrefix.isIPv6()) {
                Log.wtf(TAG, "Non-IPv6 prefix in routedIPv6AddressCount");
            }
            bigIntegerAdd = bigIntegerAdd.add(BigInteger.ONE.shiftLeft(128 - ipPrefix.getPrefixLength()));
        }
        return bigIntegerAdd;
    }
}
