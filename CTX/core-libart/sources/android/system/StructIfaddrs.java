package android.system;

import java.net.InetAddress;

public final class StructIfaddrs {
    public final byte[] hwaddr;
    public final InetAddress ifa_addr;
    public final InetAddress ifa_broadaddr;
    public final int ifa_flags;
    public final String ifa_name;
    public final InetAddress ifa_netmask;

    public StructIfaddrs(String str, int i, InetAddress inetAddress, InetAddress inetAddress2, InetAddress inetAddress3, byte[] bArr) {
        this.ifa_name = str;
        this.ifa_flags = i;
        this.ifa_addr = inetAddress;
        this.ifa_netmask = inetAddress2;
        this.ifa_broadaddr = inetAddress3;
        this.hwaddr = bArr;
    }
}
