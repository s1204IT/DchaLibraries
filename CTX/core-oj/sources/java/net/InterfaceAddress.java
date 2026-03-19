package java.net;

public class InterfaceAddress {
    private InetAddress address;
    private Inet4Address broadcast;
    private short maskLength;

    InterfaceAddress() {
        this.address = null;
        this.broadcast = null;
        this.maskLength = (short) 0;
    }

    InterfaceAddress(InetAddress inetAddress, Inet4Address inet4Address, InetAddress inetAddress2) {
        this.address = null;
        this.broadcast = null;
        this.maskLength = (short) 0;
        this.address = inetAddress;
        this.broadcast = inet4Address;
        this.maskLength = countPrefixLength(inetAddress2);
    }

    private short countPrefixLength(InetAddress inetAddress) {
        short s = 0;
        for (byte b : inetAddress.getAddress()) {
            while (b != 0) {
                b = (byte) (b << 1);
                s = (short) (s + 1);
            }
        }
        return s;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public InetAddress getBroadcast() {
        return this.broadcast;
    }

    public short getNetworkPrefixLength() {
        return this.maskLength;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof InterfaceAddress)) {
            return false;
        }
        InterfaceAddress interfaceAddress = (InterfaceAddress) obj;
        if (this.address != null ? !this.address.equals(interfaceAddress.address) : interfaceAddress.address != null) {
            return false;
        }
        if (this.broadcast != null ? this.broadcast.equals(interfaceAddress.broadcast) : interfaceAddress.broadcast == null) {
            return this.maskLength == interfaceAddress.maskLength;
        }
        return false;
    }

    public int hashCode() {
        return this.address.hashCode() + (this.broadcast != null ? this.broadcast.hashCode() : 0) + this.maskLength;
    }

    public String toString() {
        return ((Object) this.address) + "/" + ((int) this.maskLength) + " [" + ((Object) this.broadcast) + "]";
    }
}
