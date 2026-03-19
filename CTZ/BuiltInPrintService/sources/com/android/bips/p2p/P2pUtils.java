package com.android.bips.p2p;

import android.net.Uri;
import com.android.bips.BuiltInPrintService;
import com.android.bips.discovery.DiscoveredPrinter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.BitSet;
import java.util.regex.Pattern;

public class P2pUtils {
    private static final Pattern IPV4_PATTERN = Pattern.compile("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    static boolean isOnInterface(String str, Uri uri) {
        NetworkInterface networkInterface = toNetworkInterface(str);
        Inet4Address inet4Address = toInet4Address(uri);
        return (networkInterface == null || inet4Address == null || !isOnInterface(networkInterface, inet4Address)) ? false : true;
    }

    private static Inet4Address toInet4Address(Uri uri) {
        if (!IPV4_PATTERN.matcher(uri.getHost()).find()) {
            return null;
        }
        try {
            return (Inet4Address) InetAddress.getByName(uri.getHost());
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static NetworkInterface toNetworkInterface(String str) {
        if (str == null) {
            return null;
        }
        try {
            return NetworkInterface.getByName(str);
        } catch (SocketException e) {
            return null;
        }
    }

    public static boolean isP2p(DiscoveredPrinter discoveredPrinter) {
        return discoveredPrinter.path.getScheme().equals("p2p");
    }

    public static boolean isOnConnectedInterface(BuiltInPrintService builtInPrintService, DiscoveredPrinter discoveredPrinter) {
        String connectedInterface = builtInPrintService.getP2pMonitor().getConnectedInterface();
        return connectedInterface != null && isOnInterface(connectedInterface, discoveredPrinter.path);
    }

    static boolean isOnInterface(NetworkInterface networkInterface, Inet4Address inet4Address) {
        long j = toLong(inet4Address);
        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
            if (interfaceAddress.getAddress() instanceof Inet4Address) {
                Inet4Address inet4Address2 = (Inet4Address) interfaceAddress.getAddress();
                BitSet bitSet = new BitSet(32);
                bitSet.set(32 - interfaceAddress.getNetworkPrefixLength(), 32);
                long j2 = bitSet.toLongArray()[0];
                if ((toLong(inet4Address2) & j2) == (j2 & j)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long toLong(Inet4Address inet4Address) {
        byte[] address = inet4Address.getAddress();
        return ((((long) address[0]) & 255) << 24) + ((((long) address[1]) & 255) << 16) + ((((long) address[2]) & 255) << 8) + (255 & ((long) address[3]));
    }
}
