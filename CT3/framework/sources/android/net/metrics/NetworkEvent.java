package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;

public final class NetworkEvent extends IpConnectivityEvent implements Parcelable {
    public static final Parcelable.Creator<NetworkEvent> CREATOR = new Parcelable.Creator<NetworkEvent>() {
        @Override
        public NetworkEvent createFromParcel(Parcel in) {
            return new NetworkEvent(in, null);
        }

        @Override
        public NetworkEvent[] newArray(int size) {
            return new NetworkEvent[size];
        }
    };
    public static final int NETWORK_CAPTIVE_PORTAL_FOUND = 4;
    public static final int NETWORK_CONNECTED = 1;
    public static final int NETWORK_DISCONNECTED = 7;
    public static final int NETWORK_LINGER = 5;
    public static final int NETWORK_UNLINGER = 6;
    public static final int NETWORK_VALIDATED = 2;
    public static final int NETWORK_VALIDATION_FAILED = 3;
    public final long durationMs;
    public final int eventType;
    public final int netId;

    NetworkEvent(Parcel in, NetworkEvent networkEvent) {
        this(in);
    }

    private NetworkEvent(int netId, int eventType, long durationMs) {
        this.netId = netId;
        this.eventType = eventType;
        this.durationMs = durationMs;
    }

    private NetworkEvent(Parcel in) {
        this.netId = in.readInt();
        this.eventType = in.readInt();
        this.durationMs = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(this.netId);
        out.writeInt(this.eventType);
        out.writeLong(this.durationMs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static void logEvent(int netId, int eventType) {
        logEvent(new NetworkEvent(netId, eventType, 0L));
    }

    public static void logValidated(int netId, long durationMs) {
        logEvent(new NetworkEvent(netId, 2, durationMs));
    }

    public static void logCaptivePortalFound(int netId, long durationMs) {
        logEvent(new NetworkEvent(netId, 4, durationMs));
    }

    public String toString() {
        return String.format("NetworkEvent(%d, %s, %dms)", Integer.valueOf(this.netId), Decoder.constants.get(this.eventType), Long.valueOf(this.durationMs));
    }

    static final class Decoder {
        static final SparseArray<String> constants = MessageUtils.findMessageNames(new Class[]{NetworkEvent.class}, new String[]{"NETWORK_"});

        Decoder() {
        }
    }
}
