package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import com.android.internal.util.MessageUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class IpManagerEvent implements Parcelable {
    public static final int COMPLETE_LIFECYCLE = 3;
    public static final Parcelable.Creator<IpManagerEvent> CREATOR = new Parcelable.Creator<IpManagerEvent>() {
        @Override
        public IpManagerEvent createFromParcel(Parcel parcel) {
            return new IpManagerEvent(parcel);
        }

        @Override
        public IpManagerEvent[] newArray(int i) {
            return new IpManagerEvent[i];
        }
    };
    public static final int ERROR_INTERFACE_NOT_FOUND = 8;
    public static final int ERROR_INVALID_PROVISIONING = 7;
    public static final int ERROR_STARTING_IPREACHABILITYMONITOR = 6;
    public static final int ERROR_STARTING_IPV4 = 4;
    public static final int ERROR_STARTING_IPV6 = 5;
    public static final int PROVISIONING_FAIL = 2;
    public static final int PROVISIONING_OK = 1;
    public final long durationMs;
    public final int eventType;

    @Retention(RetentionPolicy.SOURCE)
    public @interface EventType {
    }

    public IpManagerEvent(int i, long j) {
        this.eventType = i;
        this.durationMs = j;
    }

    private IpManagerEvent(Parcel parcel) {
        this.eventType = parcel.readInt();
        this.durationMs = parcel.readLong();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.eventType);
        parcel.writeLong(this.durationMs);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        return String.format("IpManagerEvent(%s, %dms)", Decoder.constants.get(this.eventType), Long.valueOf(this.durationMs));
    }

    static final class Decoder {
        static final SparseArray<String> constants = MessageUtils.findMessageNames(new Class[]{IpManagerEvent.class}, new String[]{"PROVISIONING_", "COMPLETE_", "ERROR_"});

        Decoder() {
        }
    }
}
