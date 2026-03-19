package android.hardware.hdmi;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public final class HdmiHotplugEvent implements Parcelable {
    public static final Parcelable.Creator<HdmiHotplugEvent> CREATOR = new Parcelable.Creator<HdmiHotplugEvent>() {
        @Override
        public HdmiHotplugEvent createFromParcel(Parcel parcel) {
            return new HdmiHotplugEvent(parcel.readInt(), parcel.readByte() == 1);
        }

        @Override
        public HdmiHotplugEvent[] newArray(int i) {
            return new HdmiHotplugEvent[i];
        }
    };
    private final boolean mConnected;
    private final int mPort;

    public HdmiHotplugEvent(int i, boolean z) {
        this.mPort = i;
        this.mConnected = z;
    }

    public int getPort() {
        return this.mPort;
    }

    public boolean isConnected() {
        return this.mConnected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mPort);
        parcel.writeByte(this.mConnected ? (byte) 1 : (byte) 0);
    }
}
