package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class HotspotClient implements Parcelable {
    public static final Parcelable.Creator<HotspotClient> CREATOR = new Parcelable.Creator<HotspotClient>() {
        @Override
        public HotspotClient createFromParcel(Parcel in) {
            HotspotClient result = new HotspotClient(in.readString(), in.readByte() == 1);
            return result;
        }

        @Override
        public HotspotClient[] newArray(int size) {
            return new HotspotClient[size];
        }
    };
    public String deviceAddress;
    public boolean isBlocked;

    public HotspotClient(String address, boolean blocked) {
        this.isBlocked = false;
        this.deviceAddress = address;
        this.isBlocked = blocked;
    }

    public HotspotClient(HotspotClient source) {
        this.isBlocked = false;
        if (source == null) {
            return;
        }
        this.deviceAddress = source.deviceAddress;
        this.isBlocked = source.isBlocked;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(" deviceAddress: ").append(this.deviceAddress);
        sbuf.append('\n');
        sbuf.append(" isBlocked: ").append(this.isBlocked);
        sbuf.append("\n");
        return sbuf.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.deviceAddress);
        dest.writeByte(this.isBlocked ? (byte) 1 : (byte) 0);
    }
}
