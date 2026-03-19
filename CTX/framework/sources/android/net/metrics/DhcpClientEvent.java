package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;

public final class DhcpClientEvent implements Parcelable {
    public static final Parcelable.Creator<DhcpClientEvent> CREATOR = new Parcelable.Creator<DhcpClientEvent>() {
        @Override
        public DhcpClientEvent createFromParcel(Parcel parcel) {
            return new DhcpClientEvent(parcel);
        }

        @Override
        public DhcpClientEvent[] newArray(int i) {
            return new DhcpClientEvent[i];
        }
    };
    public static final String INITIAL_BOUND = "InitialBoundState";
    public static final String RENEWING_BOUND = "RenewingBoundState";
    public final int durationMs;
    public final String msg;

    public DhcpClientEvent(String str, int i) {
        this.msg = str;
        this.durationMs = i;
    }

    private DhcpClientEvent(Parcel parcel) {
        this.msg = parcel.readString();
        this.durationMs = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.msg);
        parcel.writeInt(this.durationMs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return String.format("DhcpClientEvent(%s, %dms)", this.msg, Integer.valueOf(this.durationMs));
    }
}
