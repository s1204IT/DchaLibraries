package android.hardware.hdmi;

import android.os.Parcel;
import android.os.Parcelable;

public final class HdmiHotplugEvent implements Parcelable {
    public static final Parcelable.Creator<HdmiHotplugEvent> CREATOR = new Parcelable.Creator<HdmiHotplugEvent>() {
        @Override
        public HdmiHotplugEvent createFromParcel(Parcel p) {
            int port = p.readInt();
            boolean connected = p.readByte() == 1;
            return new HdmiHotplugEvent(port, connected);
        }

        @Override
        public HdmiHotplugEvent[] newArray(int size) {
            return new HdmiHotplugEvent[size];
        }
    };
    private final boolean mConnected;
    private final int mPort;

    public HdmiHotplugEvent(int port, boolean connected) {
        this.mPort = port;
        this.mConnected = connected;
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
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mPort);
        dest.writeByte((byte) (this.mConnected ? 1 : 0));
    }
}
