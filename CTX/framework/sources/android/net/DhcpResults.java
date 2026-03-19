package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import java.net.Inet4Address;
import java.util.Objects;

public class DhcpResults extends StaticIpConfiguration {
    public static final Parcelable.Creator<DhcpResults> CREATOR = new Parcelable.Creator<DhcpResults>() {
        @Override
        public DhcpResults createFromParcel(Parcel parcel) {
            DhcpResults dhcpResults = new DhcpResults();
            DhcpResults.readFromParcel(dhcpResults, parcel);
            return dhcpResults;
        }

        @Override
        public DhcpResults[] newArray(int i) {
            return new DhcpResults[i];
        }
    };
    private static final String TAG = "DhcpResults";
    public int leaseDuration;
    public int mtu;
    public Inet4Address serverAddress;
    public String vendorInfo;

    public DhcpResults() {
    }

    public DhcpResults(StaticIpConfiguration staticIpConfiguration) {
        super(staticIpConfiguration);
    }

    public DhcpResults(DhcpResults dhcpResults) {
        super(dhcpResults);
        if (dhcpResults != null) {
            this.serverAddress = dhcpResults.serverAddress;
            this.vendorInfo = dhcpResults.vendorInfo;
            this.leaseDuration = dhcpResults.leaseDuration;
            this.mtu = dhcpResults.mtu;
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
        this.mtu = 0;
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer(super.toString());
        stringBuffer.append(" DHCP server ");
        stringBuffer.append(this.serverAddress);
        stringBuffer.append(" Vendor info ");
        stringBuffer.append(this.vendorInfo);
        stringBuffer.append(" lease ");
        stringBuffer.append(this.leaseDuration);
        stringBuffer.append(" seconds");
        if (this.mtu != 0) {
            stringBuffer.append(" MTU ");
            stringBuffer.append(this.mtu);
        }
        return stringBuffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DhcpResults)) {
            return false;
        }
        DhcpResults dhcpResults = (DhcpResults) obj;
        return super.equals((StaticIpConfiguration) obj) && Objects.equals(this.serverAddress, dhcpResults.serverAddress) && Objects.equals(this.vendorInfo, dhcpResults.vendorInfo) && this.leaseDuration == dhcpResults.leaseDuration && this.mtu == dhcpResults.mtu;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeInt(this.leaseDuration);
        parcel.writeInt(this.mtu);
        NetworkUtils.parcelInetAddress(parcel, this.serverAddress, i);
        parcel.writeString(this.vendorInfo);
    }

    private static void readFromParcel(DhcpResults dhcpResults, Parcel parcel) {
        StaticIpConfiguration.readFromParcel(dhcpResults, parcel);
        dhcpResults.leaseDuration = parcel.readInt();
        dhcpResults.mtu = parcel.readInt();
        dhcpResults.serverAddress = (Inet4Address) NetworkUtils.unparcelInetAddress(parcel);
        dhcpResults.vendorInfo = parcel.readString();
    }

    public boolean setIpAddress(String str, int i) {
        try {
            this.ipAddress = new LinkAddress((Inet4Address) NetworkUtils.numericToInetAddress(str), i);
            return false;
        } catch (ClassCastException | IllegalArgumentException e) {
            Log.e(TAG, "setIpAddress failed with addrString " + str + "/" + i);
            return true;
        }
    }

    public boolean setGateway(String str) {
        try {
            this.gateway = NetworkUtils.numericToInetAddress(str);
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "setGateway failed with addrString " + str);
            return true;
        }
    }

    public boolean addDns(String str) {
        if (!TextUtils.isEmpty(str)) {
            try {
                this.dnsServers.add(NetworkUtils.numericToInetAddress(str));
                return false;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "addDns failed with addrString " + str);
                return true;
            }
        }
        return false;
    }

    public boolean setServerAddress(String str) {
        try {
            this.serverAddress = (Inet4Address) NetworkUtils.numericToInetAddress(str);
            return false;
        } catch (ClassCastException | IllegalArgumentException e) {
            Log.e(TAG, "setServerAddress failed with addrString " + str);
            return true;
        }
    }

    public void setLeaseDuration(int i) {
        this.leaseDuration = i;
    }

    public void setVendorInfo(String str) {
        this.vendorInfo = str;
    }

    public void setDomains(String str) {
        this.domains = str;
    }
}
