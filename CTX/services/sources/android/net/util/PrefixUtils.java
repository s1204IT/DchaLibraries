package android.net.util;

import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PrefixUtils {
    private static final IpPrefix[] MIN_NON_FORWARDABLE_PREFIXES = {pfx("127.0.0.0/8"), pfx("169.254.0.0/16"), pfx("::/3"), pfx("fe80::/64"), pfx("fc00::/7"), pfx("ff02::/8")};
    public static final IpPrefix DEFAULT_WIFI_P2P_PREFIX = pfx("192.168.49.0/24");

    public static Set<IpPrefix> getNonForwardablePrefixes() {
        HashSet hashSet = new HashSet();
        addNonForwardablePrefixes(hashSet);
        return hashSet;
    }

    public static void addNonForwardablePrefixes(Set<IpPrefix> set) {
        Collections.addAll(set, MIN_NON_FORWARDABLE_PREFIXES);
    }

    public static Set<IpPrefix> localPrefixesFrom(LinkProperties linkProperties) {
        HashSet hashSet = new HashSet();
        if (linkProperties == null) {
            return hashSet;
        }
        for (LinkAddress linkAddress : linkProperties.getAllLinkAddresses()) {
            if (!linkAddress.getAddress().isLinkLocalAddress()) {
                hashSet.add(asIpPrefix(linkAddress));
            }
        }
        return hashSet;
    }

    public static IpPrefix asIpPrefix(LinkAddress linkAddress) {
        return new IpPrefix(linkAddress.getAddress(), linkAddress.getPrefixLength());
    }

    public static IpPrefix ipAddressAsPrefix(InetAddress inetAddress) {
        int i;
        if (inetAddress instanceof Inet4Address) {
            i = 32;
        } else {
            i = 128;
        }
        return new IpPrefix(inetAddress, i);
    }

    private static IpPrefix pfx(String str) {
        return new IpPrefix(str);
    }
}
