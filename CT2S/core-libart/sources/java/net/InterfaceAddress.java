package java.net;

public class InterfaceAddress {
    private final InetAddress address;
    private final InetAddress broadcastAddress;
    private final short prefixLength;

    InterfaceAddress(Inet4Address address, Inet4Address broadcastAddress, Inet4Address mask) {
        this.address = address;
        this.broadcastAddress = broadcastAddress;
        this.prefixLength = countPrefixLength(mask);
    }

    InterfaceAddress(Inet6Address address, short prefixLength) {
        this.address = address;
        this.broadcastAddress = null;
        this.prefixLength = prefixLength;
    }

    private static short countPrefixLength(Inet4Address mask) {
        short count = 0;
        byte[] arr$ = mask.ipaddress;
        for (byte b : arr$) {
            for (int i = 0; i < 8; i++) {
                if (((1 << i) & b) != 0) {
                    count = (short) (count + 1);
                }
            }
        }
        return count;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }
        InterfaceAddress rhs = (InterfaceAddress) obj;
        if (this.address != null ? this.address.equals(rhs.address) : rhs.address == null) {
            if (rhs.prefixLength == this.prefixLength) {
                if (this.broadcastAddress == null) {
                    if (rhs.broadcastAddress == null) {
                        return true;
                    }
                } else if (this.broadcastAddress.equals(rhs.broadcastAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int hashCode() {
        int hashCode = this.address == null ? 0 : -this.address.hashCode();
        return hashCode + (this.broadcastAddress != null ? this.broadcastAddress.hashCode() : 0) + this.prefixLength;
    }

    public String toString() {
        return this.address + "/" + ((int) this.prefixLength) + " [" + this.broadcastAddress + "]";
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public InetAddress getBroadcast() {
        return this.broadcastAddress;
    }

    public short getNetworkPrefixLength() {
        return this.prefixLength;
    }
}
