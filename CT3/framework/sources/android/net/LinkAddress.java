package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.system.OsConstants;
import android.util.Pair;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;

public class LinkAddress implements Parcelable {
    public static final Parcelable.Creator<LinkAddress> CREATOR = new Parcelable.Creator<LinkAddress>() {
        @Override
        public LinkAddress createFromParcel(Parcel in) {
            InetAddress address = null;
            try {
                address = InetAddress.getByAddress(in.createByteArray());
            } catch (UnknownHostException e) {
            }
            int prefixLength = in.readInt();
            int flags = in.readInt();
            int scope = in.readInt();
            long valid = in.readLong();
            return new LinkAddress(address, prefixLength, flags, scope, valid);
        }

        @Override
        public LinkAddress[] newArray(int size) {
            return new LinkAddress[size];
        }
    };
    private InetAddress address;
    private int flags;
    private int prefixLength;
    private int scope;
    private long valid;

    static int scopeForUnicastAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress()) {
            return OsConstants.RT_SCOPE_HOST;
        }
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
            return OsConstants.RT_SCOPE_LINK;
        }
        if (!(addr instanceof Inet4Address) && addr.isSiteLocalAddress()) {
            return OsConstants.RT_SCOPE_SITE;
        }
        return OsConstants.RT_SCOPE_UNIVERSE;
    }

    private boolean isIPv6ULA() {
        if (this.address == null || !(this.address instanceof Inet6Address)) {
            return false;
        }
        byte[] bytes = this.address.getAddress();
        return (bytes[0] & (-4)) == -4;
    }

    private void init(InetAddress address, int prefixLength, int flags, int scope) {
        if (address == null || address.isMulticastAddress() || prefixLength < 0 || (((address instanceof Inet4Address) && prefixLength > 32) || prefixLength > 128)) {
            throw new IllegalArgumentException("Bad LinkAddress params " + address + "/" + prefixLength);
        }
        this.address = address;
        this.prefixLength = prefixLength;
        this.flags = flags;
        this.scope = scope;
    }

    public LinkAddress(InetAddress address, int prefixLength, int flags, int scope) {
        init(address, prefixLength, flags, scope);
    }

    public LinkAddress(InetAddress address, int prefixLength, int flags, int scope, long valid) {
        init(address, prefixLength, flags, scope);
        this.valid = valid;
    }

    public LinkAddress(InetAddress address, int prefixLength) {
        this(address, prefixLength, 0, 0);
        this.scope = scopeForUnicastAddress(address);
    }

    public LinkAddress(InterfaceAddress interfaceAddress) {
        this(interfaceAddress.getAddress(), interfaceAddress.getNetworkPrefixLength());
    }

    public LinkAddress(String address) {
        this(address, 0, 0);
        this.scope = scopeForUnicastAddress(this.address);
    }

    public LinkAddress(String address, int flags, int scope) {
        Pair<InetAddress, Integer> ipAndMask = NetworkUtils.parseIpAndMask(address);
        init((InetAddress) ipAndMask.first, ((Integer) ipAndMask.second).intValue(), flags, scope);
    }

    public LinkAddress(String address, int flags, int scope, long valid) {
        Pair<InetAddress, Integer> ipAndMask = NetworkUtils.parseIpAndMask(address);
        init((InetAddress) ipAndMask.first, ((Integer) ipAndMask.second).intValue(), flags, scope);
        this.valid = valid;
    }

    public String toString() {
        return this.address.getHostAddress() + "/" + this.prefixLength;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LinkAddress)) {
            return false;
        }
        LinkAddress linkAddress = (LinkAddress) obj;
        return this.address.equals(linkAddress.address) && this.prefixLength == linkAddress.prefixLength && this.flags == linkAddress.flags && this.scope == linkAddress.scope;
    }

    public int hashCode() {
        return this.address.hashCode() + (this.prefixLength * 11) + (this.flags * 19) + (this.scope * 43);
    }

    public boolean isSameAddressAs(LinkAddress other) {
        return this.address.equals(other.address) && this.prefixLength == other.prefixLength;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public int getPrefixLength() {
        return this.prefixLength;
    }

    public int getNetworkPrefixLength() {
        return getPrefixLength();
    }

    public int getFlags() {
        return this.flags;
    }

    public int getScope() {
        return this.scope;
    }

    public long getValid() {
        return this.valid;
    }

    public boolean isGlobalPreferred() {
        if (this.scope == OsConstants.RT_SCOPE_UNIVERSE && !isIPv6ULA() && (this.flags & (OsConstants.IFA_F_DADFAILED | OsConstants.IFA_F_DEPRECATED)) == 0) {
            return ((long) (this.flags & OsConstants.IFA_F_TENTATIVE)) == 0 || ((long) (this.flags & OsConstants.IFA_F_OPTIMISTIC)) != 0;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(this.address.getAddress());
        dest.writeInt(this.prefixLength);
        dest.writeInt(this.flags);
        dest.writeInt(this.scope);
        dest.writeLong(this.valid);
    }
}
