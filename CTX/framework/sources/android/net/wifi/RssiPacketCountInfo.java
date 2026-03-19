package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class RssiPacketCountInfo implements Parcelable {
    public static final Parcelable.Creator<RssiPacketCountInfo> CREATOR = new Parcelable.Creator<RssiPacketCountInfo>() {
        @Override
        public RssiPacketCountInfo createFromParcel(Parcel parcel) {
            return new RssiPacketCountInfo(parcel);
        }

        @Override
        public RssiPacketCountInfo[] newArray(int i) {
            return new RssiPacketCountInfo[i];
        }
    };
    public int rssi;
    public int rxgood;
    public int txbad;
    public int txgood;

    public RssiPacketCountInfo() {
        this.rxgood = 0;
        this.txbad = 0;
        this.txgood = 0;
        this.rssi = 0;
    }

    private RssiPacketCountInfo(Parcel parcel) {
        this.rssi = parcel.readInt();
        this.txgood = parcel.readInt();
        this.txbad = parcel.readInt();
        this.rxgood = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.rssi);
        parcel.writeInt(this.txgood);
        parcel.writeInt(this.txbad);
        parcel.writeInt(this.rxgood);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
