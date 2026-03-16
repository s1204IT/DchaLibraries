package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Objects;

public class DhcpResults extends StaticIpConfiguration {
    public static final Parcelable.Creator<DhcpResults> CREATOR = new Parcelable.Creator<DhcpResults>() {
        @Override
        public DhcpResults createFromParcel(Parcel in) {
            DhcpResults dhcpResults = new DhcpResults();
            DhcpResults.readFromParcel(dhcpResults, in);
            return dhcpResults;
        }

        @Override
        public DhcpResults[] newArray(int size) {
            return new DhcpResults[size];
        }
    };
    private static final String TAG = "DhcpResults";
    public int leaseDuration;
    public InetAddress serverAddress;
    public String vendorInfo;

    public DhcpResults() {
    }

    public DhcpResults(StaticIpConfiguration source) {
        super(source);
    }

    public DhcpResults(DhcpResults source) {
        super(source);
        if (source != null) {
            this.serverAddress = source.serverAddress;
            this.vendorInfo = source.vendorInfo;
            this.leaseDuration = source.leaseDuration;
        }
    }

    public void updateFromDhcpRequest(DhcpResults orig) {
        if (orig != null) {
            if (this.gateway == null) {
                this.gateway = orig.gateway;
            }
            if (this.dnsServers.size() == 0) {
                this.dnsServers.addAll(orig.dnsServers);
            }
        }
    }

    public void updateFromDhcpIpv6Info(DhcpResults ipv6results) {
        if (ipv6results != null && this.dnsServers.size() == 0) {
            this.dnsServers.addAll(ipv6results.dnsServers);
        }
    }

    public boolean hasMeteredHint() {
        if (this.vendorInfo != null) {
            return this.vendorInfo.contains("ANDROID_METERED");
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        this.vendorInfo = null;
        this.leaseDuration = 0;
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer(super.toString());
        str.append(" DHCP server ").append(this.serverAddress);
        str.append(" Vendor info ").append(this.vendorInfo);
        str.append(" lease ").append(this.leaseDuration).append(" seconds");
        return str.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DhcpResults)) {
            return false;
        }
        DhcpResults target = (DhcpResults) obj;
        return super.equals((StaticIpConfiguration) obj) && Objects.equals(this.serverAddress, target.serverAddress) && Objects.equals(this.vendorInfo, target.vendorInfo) && this.leaseDuration == target.leaseDuration;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.leaseDuration);
        NetworkUtils.parcelInetAddress(dest, this.serverAddress, flags);
        dest.writeString(this.vendorInfo);
    }

    private static void readFromParcel(DhcpResults dhcpResults, Parcel in) {
        StaticIpConfiguration.readFromParcel(dhcpResults, in);
        dhcpResults.leaseDuration = in.readInt();
        dhcpResults.serverAddress = NetworkUtils.unparcelInetAddress(in);
        dhcpResults.vendorInfo = in.readString();
    }

    public boolean setIpAddress(String addrString, int prefixLength) {
        try {
            InetAddress addr = NetworkUtils.numericToInetAddress(addrString);
            if (addr instanceof Inet4Address) {
                this.ipAddress = new LinkAddress(addr, prefixLength);
            } else if (addr instanceof Inet6Address) {
                this.ipv6Address = new LinkAddress(addr, prefixLength);
            }
            return false;
        } catch (ClassCastException | IllegalArgumentException e) {
            Log.e(TAG, "setIpAddress failed with addrString " + addrString + "/" + prefixLength);
            return true;
        }
    }

    public boolean setGateway(String addrString) {
        try {
            this.gateway = NetworkUtils.numericToInetAddress(addrString);
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "setGateway failed with addrString " + addrString);
            return true;
        }
    }

    public boolean addDns(String addrString) {
        if (!TextUtils.isEmpty(addrString)) {
            try {
                this.dnsServers.add(NetworkUtils.numericToInetAddress(addrString));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "addDns failed with addrString " + addrString);
                return true;
            }
        }
        return false;
    }

    public boolean setServerAddress(String addrString) {
        try {
            this.serverAddress = NetworkUtils.numericToInetAddress(addrString);
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "setServerAddress failed with addrString " + addrString);
            return true;
        }
    }

    public void setLeaseDuration(int duration) {
        this.leaseDuration = duration;
    }

    public void setVendorInfo(String info) {
        this.vendorInfo = info;
    }

    public void setDomains(String newDomains) {
        this.domains = newDomains;
    }
}
