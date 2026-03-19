package android.net.metrics;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.MessageUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class ValidationProbeEvent implements Parcelable {
    public static final Parcelable.Creator<ValidationProbeEvent> CREATOR = new Parcelable.Creator<ValidationProbeEvent>() {
        @Override
        public ValidationProbeEvent createFromParcel(Parcel parcel) {
            return new ValidationProbeEvent(parcel);
        }

        @Override
        public ValidationProbeEvent[] newArray(int i) {
            return new ValidationProbeEvent[i];
        }
    };
    public static final int DNS_FAILURE = 0;
    public static final int DNS_SUCCESS = 1;
    private static final int FIRST_VALIDATION = 256;
    public static final int PROBE_DNS = 0;
    public static final int PROBE_FALLBACK = 4;
    public static final int PROBE_HTTP = 1;
    public static final int PROBE_HTTPS = 2;
    public static final int PROBE_PAC = 3;
    private static final int REVALIDATION = 512;
    public long durationMs;
    public int probeType;
    public int returnCode;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ReturnCode {
    }

    public ValidationProbeEvent() {
    }

    private ValidationProbeEvent(Parcel parcel) {
        this.durationMs = parcel.readLong();
        this.probeType = parcel.readInt();
        this.returnCode = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.durationMs);
        parcel.writeInt(this.probeType);
        parcel.writeInt(this.returnCode);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static int makeProbeType(int i, boolean z) {
        return (i & 255) | (z ? 256 : 512);
    }

    public static String getProbeName(int i) {
        return Decoder.constants.get(i & 255, "PROBE_???");
    }

    public static String getValidationStage(int i) {
        return Decoder.constants.get(i & 65280, IccCardConstants.INTENT_VALUE_ICC_UNKNOWN);
    }

    public String toString() {
        return String.format("ValidationProbeEvent(%s:%d %s, %dms)", getProbeName(this.probeType), Integer.valueOf(this.returnCode), getValidationStage(this.probeType), Long.valueOf(this.durationMs));
    }

    static final class Decoder {
        static final SparseArray<String> constants = MessageUtils.findMessageNames(new Class[]{ValidationProbeEvent.class}, new String[]{"PROBE_", "FIRST_", "REVALIDATION"});

        Decoder() {
        }
    }
}
