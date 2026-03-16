package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class WifiP2pInfo implements Parcelable {
    public static final Parcelable.Creator<WifiP2pInfo> CREATOR = new Parcelable.Creator<WifiP2pInfo>() {
        @Override
        public WifiP2pInfo createFromParcel(Parcel in) {
            WifiP2pInfo info = new WifiP2pInfo();
            info.groupFormed = in.readByte() == 1;
            info.isGroupOwner = in.readByte() == 1;
            if (in.readByte() == 1) {
                try {
                    info.groupOwnerAddress = InetAddress.getByAddress(in.createByteArray());
                } catch (UnknownHostException e) {
                }
            }
            if (in.readByte() == 1) {
                try {
                    info.clientAddress = InetAddress.getByAddress(in.createByteArray());
                } catch (UnknownHostException e2) {
                }
            }
            info.passphase = in.readString();
            info.frequency = in.readString();
            info.networkName = in.readString();
            info.interfaceName = in.readString();
            return info;
        }

        @Override
        public WifiP2pInfo[] newArray(int size) {
            return new WifiP2pInfo[size];
        }
    };
    public InetAddress clientAddress;
    public String frequency;
    public boolean groupFormed;
    public InetAddress groupOwnerAddress;
    public String interfaceName;
    public boolean isGroupOwner;
    public String networkName;
    public String passphase;

    public WifiP2pInfo() {
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(" groupFormed: ").append(this.groupFormed).append("\n isGroupOwner: ").append(this.isGroupOwner).append("\n groupOwnerAddress: ").append(this.groupOwnerAddress).append("\n clientAddress: ").append(this.clientAddress).append("\n passphase: ").append(this.passphase).append("\n frequency: ").append(this.frequency).append("\n networkName: ").append(this.networkName).append("\n interfaceName: ").append(this.interfaceName);
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiP2pInfo(WifiP2pInfo source) {
        if (source != null) {
            this.groupFormed = source.groupFormed;
            this.isGroupOwner = source.isGroupOwner;
            this.groupOwnerAddress = source.groupOwnerAddress;
            this.clientAddress = source.clientAddress;
            this.passphase = source.passphase;
            this.frequency = source.frequency;
            this.networkName = source.networkName;
            this.interfaceName = source.interfaceName;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.groupFormed ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isGroupOwner ? (byte) 1 : (byte) 0);
        if (this.groupOwnerAddress != null) {
            dest.writeByte((byte) 1);
            dest.writeByteArray(this.groupOwnerAddress.getAddress());
        } else {
            dest.writeByte((byte) 0);
        }
        if (this.clientAddress != null) {
            dest.writeByte((byte) 1);
            dest.writeByteArray(this.clientAddress.getAddress());
        } else {
            dest.writeByte((byte) 0);
        }
        dest.writeString(this.passphase);
        dest.writeString(this.frequency);
        dest.writeString(this.networkName);
        dest.writeString(this.interfaceName);
    }
}
