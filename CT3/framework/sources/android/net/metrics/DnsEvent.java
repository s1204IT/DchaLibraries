package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;

public final class DnsEvent extends IpConnectivityEvent implements Parcelable {
    public static final Parcelable.Creator<DnsEvent> CREATOR = new Parcelable.Creator<DnsEvent>() {
        @Override
        public DnsEvent createFromParcel(Parcel in) {
            return new DnsEvent(in, null);
        }

        @Override
        public DnsEvent[] newArray(int size) {
            return new DnsEvent[size];
        }
    };
    public final byte[] eventTypes;
    public final int[] latenciesMs;
    public final int netId;
    public final byte[] returnCodes;

    DnsEvent(Parcel in, DnsEvent dnsEvent) {
        this(in);
    }

    private DnsEvent(int netId, byte[] eventTypes, byte[] returnCodes, int[] latenciesMs) {
        this.netId = netId;
        this.eventTypes = eventTypes;
        this.returnCodes = returnCodes;
        this.latenciesMs = latenciesMs;
    }

    private DnsEvent(Parcel in) {
        this.netId = in.readInt();
        this.eventTypes = in.createByteArray();
        this.returnCodes = in.createByteArray();
        this.latenciesMs = in.createIntArray();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.netId);
        out.writeByteArray(this.eventTypes);
        out.writeByteArray(this.returnCodes);
        out.writeIntArray(this.latenciesMs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return String.format("DnsEvent(%d, %d events)", Integer.valueOf(this.netId), Integer.valueOf(this.eventTypes.length));
    }

    public static void logEvent(int netId, byte[] eventTypes, byte[] returnCodes, int[] latenciesMs) {
        logEvent(new DnsEvent(netId, eventTypes, returnCodes, latenciesMs));
    }
}
