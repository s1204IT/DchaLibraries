package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;

public final class DhcpClientEvent extends IpConnectivityEvent implements Parcelable {
    public static final Parcelable.Creator<DhcpClientEvent> CREATOR = new Parcelable.Creator<DhcpClientEvent>() {
        @Override
        public DhcpClientEvent createFromParcel(Parcel in) {
            return new DhcpClientEvent(in, (DhcpClientEvent) null);
        }

        @Override
        public DhcpClientEvent[] newArray(int size) {
            return new DhcpClientEvent[size];
        }
    };
    public final String ifName;
    public final String msg;

    DhcpClientEvent(Parcel in, DhcpClientEvent dhcpClientEvent) {
        this(in);
    }

    private DhcpClientEvent(String ifName, String msg) {
        this.ifName = ifName;
        this.msg = msg;
    }

    private DhcpClientEvent(Parcel in) {
        this.ifName = in.readString();
        this.msg = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.ifName);
        out.writeString(this.msg);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return String.format("DhcpClientEvent(%s, %s)", this.ifName, this.msg);
    }

    public static void logStateEvent(String ifName, String state) {
        logEvent(new DhcpClientEvent(ifName, state));
    }
}
