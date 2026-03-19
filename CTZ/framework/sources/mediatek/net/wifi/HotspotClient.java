package mediatek.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;

public class HotspotClient implements Parcelable {
    public static final Parcelable.Creator<HotspotClient> CREATOR = new Parcelable.Creator<HotspotClient>() {
        @Override
        public HotspotClient createFromParcel(Parcel parcel) {
            return new HotspotClient(parcel.readString(), parcel.readByte() == 1, parcel.readString());
        }

        @Override
        public HotspotClient[] newArray(int i) {
            return new HotspotClient[i];
        }
    };
    public String deviceAddress;
    public boolean isBlocked;
    public String name;

    public HotspotClient(String str, boolean z) {
        this.isBlocked = false;
        this.deviceAddress = str;
        this.isBlocked = z;
    }

    public HotspotClient(String str, boolean z, String str2) {
        this.isBlocked = false;
        this.deviceAddress = str;
        this.isBlocked = z;
        this.name = str2;
    }

    public HotspotClient(HotspotClient hotspotClient) {
        this.isBlocked = false;
        if (hotspotClient != null) {
            this.deviceAddress = hotspotClient.deviceAddress;
            this.isBlocked = hotspotClient.isBlocked;
            this.name = hotspotClient.name;
        }
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(" deviceAddress: ");
        stringBuffer.append(this.deviceAddress);
        stringBuffer.append('\n');
        stringBuffer.append(" isBlocked: ");
        stringBuffer.append(this.isBlocked);
        stringBuffer.append("\n");
        stringBuffer.append(" name: ");
        stringBuffer.append(this.name);
        stringBuffer.append("\n");
        return stringBuffer.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.deviceAddress);
        parcel.writeByte(this.isBlocked ? (byte) 1 : (byte) 0);
        parcel.writeString(this.name);
    }
}
