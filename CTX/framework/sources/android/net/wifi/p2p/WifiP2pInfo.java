package android.net.wifi.p2p;

import android.os.Parcel;
import android.os.Parcelable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class WifiP2pInfo implements Parcelable {
    public static final Parcelable.Creator<WifiP2pInfo> CREATOR = new Parcelable.Creator<WifiP2pInfo>() {
        @Override
        public WifiP2pInfo createFromParcel(Parcel parcel) {
            WifiP2pInfo wifiP2pInfo = new WifiP2pInfo();
            wifiP2pInfo.groupFormed = parcel.readByte() == 1;
            wifiP2pInfo.isGroupOwner = parcel.readByte() == 1;
            if (parcel.readByte() == 1) {
                try {
                    wifiP2pInfo.groupOwnerAddress = InetAddress.getByAddress(parcel.createByteArray());
                } catch (UnknownHostException e) {
                }
            }
            return wifiP2pInfo;
        }

        @Override
        public WifiP2pInfo[] newArray(int i) {
            return new WifiP2pInfo[i];
        }
    };
    public boolean groupFormed;
    public InetAddress groupOwnerAddress;
    public boolean isGroupOwner;

    public WifiP2pInfo() {
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("groupFormed: ");
        stringBuffer.append(this.groupFormed);
        stringBuffer.append(" isGroupOwner: ");
        stringBuffer.append(this.isGroupOwner);
        stringBuffer.append(" groupOwnerAddress: ");
        stringBuffer.append(this.groupOwnerAddress);
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public WifiP2pInfo(WifiP2pInfo wifiP2pInfo) {
        if (wifiP2pInfo != null) {
            this.groupFormed = wifiP2pInfo.groupFormed;
            this.isGroupOwner = wifiP2pInfo.isGroupOwner;
            this.groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
        }
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte(this.groupFormed ? (byte) 1 : (byte) 0);
        parcel.writeByte(this.isGroupOwner ? (byte) 1 : (byte) 0);
        if (this.groupOwnerAddress != null) {
            parcel.writeByte((byte) 1);
            parcel.writeByteArray(this.groupOwnerAddress.getAddress());
        } else {
            parcel.writeByte((byte) 0);
        }
    }
}
