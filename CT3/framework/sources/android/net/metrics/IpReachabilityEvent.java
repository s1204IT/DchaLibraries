package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;

public final class IpReachabilityEvent extends IpConnectivityEvent implements Parcelable {
    public static final Parcelable.Creator<IpReachabilityEvent> CREATOR = new Parcelable.Creator<IpReachabilityEvent>() {
        @Override
        public IpReachabilityEvent createFromParcel(Parcel in) {
            return new IpReachabilityEvent(in, (IpReachabilityEvent) null);
        }

        @Override
        public IpReachabilityEvent[] newArray(int size) {
            return new IpReachabilityEvent[size];
        }
    };
    public static final int NUD_FAILED = 512;
    public static final int PROBE = 256;
    public static final int PROVISIONING_LOST = 768;
    public final int eventType;
    public final String ifName;

    IpReachabilityEvent(Parcel in, IpReachabilityEvent ipReachabilityEvent) {
        this(in);
    }

    private IpReachabilityEvent(String ifName, int eventType) {
        this.ifName = ifName;
        this.eventType = eventType;
    }

    private IpReachabilityEvent(Parcel in) {
        this.ifName = in.readString();
        this.eventType = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.ifName);
        out.writeInt(this.eventType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static void logProbeEvent(String ifName, int nlErrorCode) {
        logEvent(new IpReachabilityEvent(ifName, (nlErrorCode & 255) | 256));
    }

    public static void logNudFailed(String ifName) {
        logEvent(new IpReachabilityEvent(ifName, 512));
    }

    public static void logProvisioningLost(String ifName) {
        logEvent(new IpReachabilityEvent(ifName, 768));
    }

    public String toString() {
        return String.format("IpReachabilityEvent(%s, %s)", this.ifName, Decoder.constants.get(this.eventType));
    }

    static final class Decoder {
        static final SparseArray<String> constants = MessageUtils.findMessageNames(new Class[]{IpReachabilityEvent.class}, new String[]{"PROBE", "PROVISIONING_", "NUD_"});

        Decoder() {
        }
    }
}
