package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class ScoredNetwork implements Parcelable {
    public static final Parcelable.Creator<ScoredNetwork> CREATOR = new Parcelable.Creator<ScoredNetwork>() {
        @Override
        public ScoredNetwork createFromParcel(Parcel in) {
            return new ScoredNetwork(in, (ScoredNetwork) null);
        }

        @Override
        public ScoredNetwork[] newArray(int size) {
            return new ScoredNetwork[size];
        }
    };
    public final boolean meteredHint;
    public final NetworkKey networkKey;
    public final RssiCurve rssiCurve;

    ScoredNetwork(Parcel in, ScoredNetwork scoredNetwork) {
        this(in);
    }

    public ScoredNetwork(NetworkKey networkKey, RssiCurve rssiCurve) {
        this(networkKey, rssiCurve, false);
    }

    public ScoredNetwork(NetworkKey networkKey, RssiCurve rssiCurve, boolean meteredHint) {
        this.networkKey = networkKey;
        this.rssiCurve = rssiCurve;
        this.meteredHint = meteredHint;
    }

    private ScoredNetwork(Parcel in) {
        this.networkKey = NetworkKey.CREATOR.createFromParcel(in);
        if (in.readByte() == 1) {
            this.rssiCurve = RssiCurve.CREATOR.createFromParcel(in);
        } else {
            this.rssiCurve = null;
        }
        this.meteredHint = in.readByte() != 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        this.networkKey.writeToParcel(out, flags);
        if (this.rssiCurve != null) {
            out.writeByte((byte) 1);
            this.rssiCurve.writeToParcel(out, flags);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeByte((byte) (this.meteredHint ? 1 : 0));
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScoredNetwork that = (ScoredNetwork) o;
        if (Objects.equals(this.networkKey, that.networkKey) && Objects.equals(this.rssiCurve, that.rssiCurve)) {
            return Objects.equals(Boolean.valueOf(this.meteredHint), Boolean.valueOf(that.meteredHint));
        }
        return false;
    }

    public int hashCode() {
        return Objects.hash(this.networkKey, this.rssiCurve, Boolean.valueOf(this.meteredHint));
    }

    public String toString() {
        return "ScoredNetwork[key=" + this.networkKey + ",score=" + this.rssiCurve + ",meteredHint=" + this.meteredHint + "]";
    }
}
