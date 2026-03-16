package android.net;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public class ScoredNetwork implements Parcelable {
    public static final Parcelable.Creator<ScoredNetwork> CREATOR = new Parcelable.Creator<ScoredNetwork>() {
        @Override
        public ScoredNetwork createFromParcel(Parcel in) {
            return new ScoredNetwork(in);
        }

        @Override
        public ScoredNetwork[] newArray(int size) {
            return new ScoredNetwork[size];
        }
    };
    public final NetworkKey networkKey;
    public final RssiCurve rssiCurve;

    public ScoredNetwork(NetworkKey networkKey, RssiCurve rssiCurve) {
        this.networkKey = networkKey;
        this.rssiCurve = rssiCurve;
    }

    private ScoredNetwork(Parcel in) {
        this.networkKey = NetworkKey.CREATOR.createFromParcel(in);
        if (in.readByte() == 1) {
            this.rssiCurve = RssiCurve.CREATOR.createFromParcel(in);
        } else {
            this.rssiCurve = null;
        }
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
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScoredNetwork that = (ScoredNetwork) o;
        return Objects.equals(this.networkKey, that.networkKey) && Objects.equals(this.rssiCurve, that.rssiCurve);
    }

    public int hashCode() {
        return Objects.hash(this.networkKey, this.rssiCurve);
    }

    public String toString() {
        return "ScoredNetwork[key=" + this.networkKey + ",score=" + this.rssiCurve + "]";
    }
}
