package android.app.admin;

import android.os.Parcel;
import android.os.Parcelable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class ConnectEvent extends NetworkEvent implements Parcelable {
    public static final Parcelable.Creator<ConnectEvent> CREATOR = new Parcelable.Creator<ConnectEvent>() {
        @Override
        public ConnectEvent createFromParcel(Parcel parcel) {
            if (parcel.readInt() != 2) {
                return null;
            }
            return new ConnectEvent(parcel);
        }

        @Override
        public ConnectEvent[] newArray(int i) {
            return new ConnectEvent[i];
        }
    };
    private final String mIpAddress;
    private final int mPort;

    public ConnectEvent(String str, int i, String str2, long j) {
        super(str2, j);
        this.mIpAddress = str;
        this.mPort = i;
    }

    private ConnectEvent(Parcel parcel) {
        this.mIpAddress = parcel.readString();
        this.mPort = parcel.readInt();
        this.mPackageName = parcel.readString();
        this.mTimestamp = parcel.readLong();
        this.mId = parcel.readLong();
    }

    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByName(this.mIpAddress);
        } catch (UnknownHostException e) {
            return InetAddress.getLoopbackAddress();
        }
    }

    public int getPort() {
        return this.mPort;
    }

    public String toString() {
        return String.format("ConnectEvent(%d, %s, %d, %d, %s)", Long.valueOf(this.mId), this.mIpAddress, Integer.valueOf(this.mPort), Long.valueOf(this.mTimestamp), this.mPackageName);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(2);
        parcel.writeString(this.mIpAddress);
        parcel.writeInt(this.mPort);
        parcel.writeString(this.mPackageName);
        parcel.writeLong(this.mTimestamp);
        parcel.writeLong(this.mId);
    }
}
