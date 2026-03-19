package android.speech.tts;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.logging.nano.MetricsProto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Voice implements Parcelable {
    public static final Parcelable.Creator<Voice> CREATOR = new Parcelable.Creator<Voice>() {
        @Override
        public Voice createFromParcel(Parcel parcel) {
            return new Voice(parcel);
        }

        @Override
        public Voice[] newArray(int i) {
            return new Voice[i];
        }
    };
    public static final int LATENCY_HIGH = 400;
    public static final int LATENCY_LOW = 200;
    public static final int LATENCY_NORMAL = 300;
    public static final int LATENCY_VERY_HIGH = 500;
    public static final int LATENCY_VERY_LOW = 100;
    public static final int QUALITY_HIGH = 400;
    public static final int QUALITY_LOW = 200;
    public static final int QUALITY_NORMAL = 300;
    public static final int QUALITY_VERY_HIGH = 500;
    public static final int QUALITY_VERY_LOW = 100;
    private final Set<String> mFeatures;
    private final int mLatency;
    private final Locale mLocale;
    private final String mName;
    private final int mQuality;
    private final boolean mRequiresNetworkConnection;

    public Voice(String str, Locale locale, int i, int i2, boolean z, Set<String> set) {
        this.mName = str;
        this.mLocale = locale;
        this.mQuality = i;
        this.mLatency = i2;
        this.mRequiresNetworkConnection = z;
        this.mFeatures = set;
    }

    private Voice(Parcel parcel) {
        this.mName = parcel.readString();
        this.mLocale = (Locale) parcel.readSerializable();
        this.mQuality = parcel.readInt();
        this.mLatency = parcel.readInt();
        this.mRequiresNetworkConnection = parcel.readByte() == 1;
        this.mFeatures = new HashSet();
        Collections.addAll(this.mFeatures, parcel.readStringArray());
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mName);
        parcel.writeSerializable(this.mLocale);
        parcel.writeInt(this.mQuality);
        parcel.writeInt(this.mLatency);
        parcel.writeByte(this.mRequiresNetworkConnection ? (byte) 1 : (byte) 0);
        parcel.writeStringList(new ArrayList(this.mFeatures));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public Locale getLocale() {
        return this.mLocale;
    }

    public int getQuality() {
        return this.mQuality;
    }

    public int getLatency() {
        return this.mLatency;
    }

    public boolean isNetworkConnectionRequired() {
        return this.mRequiresNetworkConnection;
    }

    public String getName() {
        return this.mName;
    }

    public Set<String> getFeatures() {
        return this.mFeatures;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("Voice[Name: ");
        sb.append(this.mName);
        sb.append(", locale: ");
        sb.append(this.mLocale);
        sb.append(", quality: ");
        sb.append(this.mQuality);
        sb.append(", latency: ");
        sb.append(this.mLatency);
        sb.append(", requiresNetwork: ");
        sb.append(this.mRequiresNetworkConnection);
        sb.append(", features: ");
        sb.append(this.mFeatures.toString());
        sb.append("]");
        return sb.toString();
    }

    public int hashCode() {
        return (31 * ((((((((((this.mFeatures == null ? 0 : this.mFeatures.hashCode()) + 31) * 31) + this.mLatency) * 31) + (this.mLocale == null ? 0 : this.mLocale.hashCode())) * 31) + (this.mName != null ? this.mName.hashCode() : 0)) * 31) + this.mQuality)) + (this.mRequiresNetworkConnection ? MetricsProto.MetricsEvent.AUTOFILL_SERVICE_DISABLED_APP : MetricsProto.MetricsEvent.ANOMALY_TYPE_UNOPTIMIZED_BT);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Voice voice = (Voice) obj;
        if (this.mFeatures == null) {
            if (voice.mFeatures != null) {
                return false;
            }
        } else if (!this.mFeatures.equals(voice.mFeatures)) {
            return false;
        }
        if (this.mLatency != voice.mLatency) {
            return false;
        }
        if (this.mLocale == null) {
            if (voice.mLocale != null) {
                return false;
            }
        } else if (!this.mLocale.equals(voice.mLocale)) {
            return false;
        }
        if (this.mName == null) {
            if (voice.mName != null) {
                return false;
            }
        } else if (!this.mName.equals(voice.mName)) {
            return false;
        }
        if (this.mQuality == voice.mQuality && this.mRequiresNetworkConnection == voice.mRequiresNetworkConnection) {
            return true;
        }
        return false;
    }
}
