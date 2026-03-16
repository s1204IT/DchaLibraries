package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Objects;

public final class RouteInfo implements Parcelable {
    public static final Parcelable.Creator<RouteInfo> CREATOR = new Parcelable.Creator<RouteInfo>() {
        @Override
        public RouteInfo createFromParcel(Parcel in) {
            IpPrefix dest = (IpPrefix) in.readParcelable(null);
            InetAddress gateway = null;
            byte[] addr = in.createByteArray();
            try {
                gateway = InetAddress.getByAddress(addr);
            } catch (UnknownHostException e) {
            }
            String iface = in.readString();
            int type = in.readInt();
            return new RouteInfo(dest, gateway, iface, type);
        }

        @Override
        public RouteInfo[] newArray(int size) {
            return new RouteInfo[size];
        }
    };
    public static final int RTN_THROW = 9;
    public static final int RTN_UNICAST = 1;
    public static final int RTN_UNREACHABLE = 7;
    private final IpPrefix mDestination;
    private final InetAddress mGateway;
    private final boolean mHasGateway;
    private final String mInterface;
    private final boolean mIsHost;
    private final int mType;

    public RouteInfo(IpPrefix destination, InetAddress gateway, String iface, int type) {
        switch (type) {
            case 1:
            case 7:
            case 9:
                if (destination == null) {
                    if (gateway != null) {
                        if (gateway instanceof Inet4Address) {
                            destination = new IpPrefix(Inet4Address.ANY, 0);
                        } else {
                            destination = new IpPrefix(Inet6Address.ANY, 0);
                        }
                    } else {
                        throw new IllegalArgumentException("Invalid arguments passed in: " + gateway + "," + destination);
                    }
                }
                if (gateway == null) {
                    if (destination.getAddress() instanceof Inet4Address) {
                        gateway = Inet4Address.ANY;
                    } else {
                        gateway = Inet6Address.ANY;
                    }
                }
                this.mHasGateway = gateway.isAnyLocalAddress() ? false : true;
                if (((destination.getAddress() instanceof Inet4Address) && !(gateway instanceof Inet4Address)) || ((destination.getAddress() instanceof Inet6Address) && !(gateway instanceof Inet6Address))) {
                    throw new IllegalArgumentException("address family mismatch in RouteInfo constructor");
                }
                this.mDestination = destination;
                this.mGateway = gateway;
                this.mInterface = iface;
                this.mType = type;
                this.mIsHost = isHost();
                return;
            default:
                throw new IllegalArgumentException("Unknown route type " + type);
        }
    }

    public RouteInfo(IpPrefix destination, InetAddress gateway, String iface) {
        this(destination, gateway, iface, 1);
    }

    public RouteInfo(LinkAddress destination, InetAddress gateway, String iface) {
        this(destination == null ? null : new IpPrefix(destination.getAddress(), destination.getPrefixLength()), gateway, iface);
    }

    public RouteInfo(IpPrefix destination, InetAddress gateway) {
        this(destination, gateway, (String) null);
    }

    public RouteInfo(LinkAddress destination, InetAddress gateway) {
        this(destination, gateway, (String) null);
    }

    public RouteInfo(InetAddress gateway) {
        this((IpPrefix) null, gateway, (String) null);
    }

    public RouteInfo(IpPrefix destination) {
        this(destination, (InetAddress) null, (String) null);
    }

    public RouteInfo(LinkAddress destination) {
        this(destination, (InetAddress) null, (String) null);
    }

    public RouteInfo(IpPrefix destination, int type) {
        this(destination, null, null, type);
    }

    public static RouteInfo makeHostRoute(InetAddress host, String iface) {
        return makeHostRoute(host, null, iface);
    }

    public static RouteInfo makeHostRoute(InetAddress host, InetAddress gateway, String iface) {
        if (host == null) {
            return null;
        }
        if (host instanceof Inet4Address) {
            return new RouteInfo(new IpPrefix(host, 32), gateway, iface);
        }
        return new RouteInfo(new IpPrefix(host, 128), gateway, iface);
    }

    private boolean isHost() {
        return ((this.mDestination.getAddress() instanceof Inet4Address) && this.mDestination.getPrefixLength() == 32) || ((this.mDestination.getAddress() instanceof Inet6Address) && this.mDestination.getPrefixLength() == 128);
    }

    public IpPrefix getDestination() {
        return this.mDestination;
    }

    public LinkAddress getDestinationLinkAddress() {
        return new LinkAddress(this.mDestination.getAddress(), this.mDestination.getPrefixLength());
    }

    public InetAddress getGateway() {
        return this.mGateway;
    }

    public String getInterface() {
        return this.mInterface;
    }

    public int getType() {
        return this.mType;
    }

    public boolean isDefaultRoute() {
        return this.mType == 1 && this.mDestination.getPrefixLength() == 0;
    }

    public boolean isIPv4Default() {
        return isDefaultRoute() && (this.mDestination.getAddress() instanceof Inet4Address);
    }

    public boolean isIPv6Default() {
        return isDefaultRoute() && (this.mDestination.getAddress() instanceof Inet6Address);
    }

    public boolean isHostRoute() {
        return this.mIsHost;
    }

    public boolean hasGateway() {
        return this.mHasGateway;
    }

    public boolean matches(InetAddress destination) {
        if (destination == null) {
            return false;
        }
        InetAddress dstNet = NetworkUtils.getNetworkPart(destination, this.mDestination.getPrefixLength());
        return this.mDestination.getAddress().equals(dstNet);
    }

    public static RouteInfo selectBestRoute(Collection<RouteInfo> routes, InetAddress dest) {
        if (routes == null || dest == null) {
            return null;
        }
        RouteInfo bestRoute = null;
        for (RouteInfo route : routes) {
            if (NetworkUtils.addressTypeMatches(route.mDestination.getAddress(), dest) && (bestRoute == null || bestRoute.mDestination.getPrefixLength() < route.mDestination.getPrefixLength())) {
                if (route.matches(dest)) {
                    bestRoute = route;
                }
            }
        }
        return bestRoute;
    }

    public String toString() {
        String val = ProxyInfo.LOCAL_EXCL_LIST;
        if (this.mDestination != null) {
            val = this.mDestination.toString();
        }
        if (this.mType == 7) {
            return val + " unreachable";
        }
        if (this.mType == 9) {
            return val + " throw";
        }
        String val2 = val + " ->";
        if (this.mGateway != null) {
            val2 = val2 + " " + this.mGateway.getHostAddress();
        }
        if (this.mInterface != null) {
            val2 = val2 + " " + this.mInterface;
        }
        if (this.mType != 1) {
            return val2 + " unknown type " + this.mType;
        }
        return val2;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RouteInfo)) {
            return false;
        }
        RouteInfo target = (RouteInfo) obj;
        return Objects.equals(this.mDestination, target.getDestination()) && Objects.equals(this.mGateway, target.getGateway()) && Objects.equals(this.mInterface, target.getInterface()) && this.mType == target.getType();
    }

    public int hashCode() {
        return (this.mGateway == null ? 0 : this.mGateway.hashCode() * 47) + (this.mDestination.hashCode() * 41) + (this.mInterface != null ? this.mInterface.hashCode() * 67 : 0) + (this.mType * 71);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mDestination, flags);
        byte[] gatewayBytes = this.mGateway == null ? null : this.mGateway.getAddress();
        dest.writeByteArray(gatewayBytes);
        dest.writeString(this.mInterface);
        dest.writeInt(this.mType);
    }
}
