package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public class DhcpInfo implements Parcelable {
    public static final Parcelable.Creator<DhcpInfo> CREATOR = new Parcelable.Creator<DhcpInfo>() {
        @Override
        public DhcpInfo createFromParcel(Parcel parcel) {
            DhcpInfo dhcpInfo = new DhcpInfo();
            dhcpInfo.ipAddress = parcel.readInt();
            dhcpInfo.gateway = parcel.readInt();
            dhcpInfo.netmask = parcel.readInt();
            dhcpInfo.dns1 = parcel.readInt();
            dhcpInfo.dns2 = parcel.readInt();
            dhcpInfo.serverAddress = parcel.readInt();
            dhcpInfo.leaseDuration = parcel.readInt();
            return dhcpInfo;
        }

        @Override
        public DhcpInfo[] newArray(int i) {
            return new DhcpInfo[i];
        }
    };
    public int dns1;
    public int dns2;
    public int gateway;
    public int ipAddress;
    public int leaseDuration;
    public int netmask;
    public int serverAddress;

    public DhcpInfo() {
    }

    public DhcpInfo(DhcpInfo dhcpInfo) {
        if (dhcpInfo != null) {
            this.ipAddress = dhcpInfo.ipAddress;
            this.gateway = dhcpInfo.gateway;
            this.netmask = dhcpInfo.netmask;
            this.dns1 = dhcpInfo.dns1;
            this.dns2 = dhcpInfo.dns2;
            this.serverAddress = dhcpInfo.serverAddress;
            this.leaseDuration = dhcpInfo.leaseDuration;
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("ipaddr ");
        putAddress(stringBuffer, this.ipAddress);
        stringBuffer.append(" gateway ");
        putAddress(stringBuffer, this.gateway);
        stringBuffer.append(" netmask ");
        putAddress(stringBuffer, this.netmask);
        stringBuffer.append(" dns1 ");
        putAddress(stringBuffer, this.dns1);
        stringBuffer.append(" dns2 ");
        putAddress(stringBuffer, this.dns2);
        stringBuffer.append(" DHCP server ");
        putAddress(stringBuffer, this.serverAddress);
        stringBuffer.append(" lease ");
        stringBuffer.append(this.leaseDuration);
        stringBuffer.append(" seconds");
        return stringBuffer.toString();
    }

    private static void putAddress(StringBuffer stringBuffer, int i) {
        stringBuffer.append(NetworkUtils.intToInetAddress(i).getHostAddress());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.ipAddress);
        parcel.writeInt(this.gateway);
        parcel.writeInt(this.netmask);
        parcel.writeInt(this.dns1);
        parcel.writeInt(this.dns2);
        parcel.writeInt(this.serverAddress);
        parcel.writeInt(this.leaseDuration);
    }
}
