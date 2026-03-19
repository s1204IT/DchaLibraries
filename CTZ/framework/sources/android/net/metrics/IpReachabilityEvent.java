package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;

public final class IpReachabilityEvent implements Parcelable {
    public static final Parcelable.Creator<IpReachabilityEvent> CREATOR = new Parcelable.Creator<IpReachabilityEvent>() {
        @Override
        public IpReachabilityEvent createFromParcel(Parcel parcel) {
            return new IpReachabilityEvent(parcel);
        }

        @Override
        public IpReachabilityEvent[] newArray(int i) {
            return new IpReachabilityEvent[i];
        }
    };
    public static final int NUD_FAILED = 512;
    public static final int NUD_FAILED_ORGANIC = 1024;
    public static final int PROBE = 256;
    public static final int PROVISIONING_LOST = 768;
    public static final int PROVISIONING_LOST_ORGANIC = 1280;
    public final int eventType;

    public IpReachabilityEvent(int i) {
        this.eventType = i;
    }

    private IpReachabilityEvent(Parcel parcel) {
        this.eventType = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.eventType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static int nudFailureEventType(boolean z, boolean z2) {
        return z ? z2 ? 768 : 512 : z2 ? 1280 : 1024;
    }

    public String toString() {
        return String.format("IpReachabilityEvent(%s:%02x)", Decoder.constants.get(this.eventType & 65280), Integer.valueOf(this.eventType & 255));
    }

    static final class Decoder {
        static final SparseArray<String> constants = MessageUtils.findMessageNames(new Class[]{IpReachabilityEvent.class}, new String[]{"PROBE", "PROVISIONING_", "NUD_"});

        Decoder() {
        }
    }
}
