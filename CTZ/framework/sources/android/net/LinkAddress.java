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
        public LinkAddress createFromParcel(Parcel parcel) {
            InetAddress byAddress;
            try {
                byAddress = InetAddress.getByAddress(parcel.createByteArray());
            } catch (UnknownHostException e) {
                byAddress = null;
            }
            return new LinkAddress(byAddress, parcel.readInt(), parcel.readInt(), parcel.readInt());
        }

        @Override
        public LinkAddress[] newArray(int i) {
            return new LinkAddress[i];
        }
    };
    private InetAddress address;
    private int flags;
    private int prefixLength;
    private int scope;

    private static int scopeForUnicastAddress(InetAddress inetAddress) {
        if (inetAddress.isAnyLocalAddress()) {
            return OsConstants.RT_SCOPE_HOST;
        }
        if (inetAddress.isLoopbackAddress() || inetAddress.isLinkLocalAddress()) {
            return OsConstants.RT_SCOPE_LINK;
        }
        if (!(inetAddress instanceof Inet4Address) && inetAddress.isSiteLocalAddress()) {
            return OsConstants.RT_SCOPE_SITE;
        }
        return OsConstants.RT_SCOPE_UNIVERSE;
    }

    private boolean isIPv6ULA() {
        return isIPv6() && (this.address.getAddress()[0] & (-2)) == -4;
    }

    public boolean isIPv6() {
        return this.address instanceof Inet6Address;
    }

    public boolean isIPv4() {
        return this.address instanceof Inet4Address;
    }

    private void init(InetAddress inetAddress, int i, int i2, int i3) {
        if (inetAddress == null || inetAddress.isMulticastAddress() || i < 0 || (((inetAddress instanceof Inet4Address) && i > 32) || i > 128)) {
            throw new IllegalArgumentException("Bad LinkAddress params " + inetAddress + "/" + i);
        }
        this.address = inetAddress;
        this.prefixLength = i;
        this.flags = i2;
        this.scope = i3;
    }

    public LinkAddress(InetAddress inetAddress, int i, int i2, int i3) {
        init(inetAddress, i, i2, i3);
    }

    public LinkAddress(InetAddress inetAddress, int i) {
        this(inetAddress, i, 0, 0);
        this.scope = scopeForUnicastAddress(inetAddress);
    }

    public LinkAddress(InterfaceAddress interfaceAddress) {
        this(interfaceAddress.getAddress(), interfaceAddress.getNetworkPrefixLength());
    }

    public LinkAddress(String str) {
        this(str, 0, 0);
        this.scope = scopeForUnicastAddress(this.address);
    }

    public LinkAddress(String str, int i, int i2) {
        Pair<InetAddress, Integer> ipAndMask = NetworkUtils.parseIpAndMask(str);
        init(ipAndMask.first, ipAndMask.second.intValue(), i, i2);
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
        return this.address.hashCode() + (11 * this.prefixLength) + (19 * this.flags) + (43 * this.scope);
    }

    public boolean isSameAddressAs(LinkAddress linkAddress) {
        return this.address.equals(linkAddress.address) && this.prefixLength == linkAddress.prefixLength;
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

    public boolean isGlobalPreferred() {
        return this.scope == OsConstants.RT_SCOPE_UNIVERSE && !isIPv6ULA() && ((long) (this.flags & (OsConstants.IFA_F_DADFAILED | OsConstants.IFA_F_DEPRECATED))) == 0 && (((long) (this.flags & OsConstants.IFA_F_TENTATIVE)) == 0 || ((long) (this.flags & OsConstants.IFA_F_OPTIMISTIC)) != 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.address.getAddress());
        parcel.writeInt(this.prefixLength);
        parcel.writeInt(this.flags);
        parcel.writeInt(this.scope);
    }
}
