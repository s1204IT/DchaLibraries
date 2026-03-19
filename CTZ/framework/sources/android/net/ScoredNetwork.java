package android.net;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

@SystemApi
public class ScoredNetwork implements Parcelable {
    public static final String ATTRIBUTES_KEY_BADGING_CURVE = "android.net.attributes.key.BADGING_CURVE";
    public static final String ATTRIBUTES_KEY_HAS_CAPTIVE_PORTAL = "android.net.attributes.key.HAS_CAPTIVE_PORTAL";
    public static final String ATTRIBUTES_KEY_RANKING_SCORE_OFFSET = "android.net.attributes.key.RANKING_SCORE_OFFSET";
    public static final Parcelable.Creator<ScoredNetwork> CREATOR = new Parcelable.Creator<ScoredNetwork>() {
        @Override
        public ScoredNetwork createFromParcel(Parcel parcel) {
            return new ScoredNetwork(parcel);
        }

        @Override
        public ScoredNetwork[] newArray(int i) {
            return new ScoredNetwork[i];
        }
    };
    public final Bundle attributes;
    public final boolean meteredHint;
    public final NetworkKey networkKey;
    public final RssiCurve rssiCurve;

    public ScoredNetwork(NetworkKey networkKey, RssiCurve rssiCurve) {
        this(networkKey, rssiCurve, false);
    }

    public ScoredNetwork(NetworkKey networkKey, RssiCurve rssiCurve, boolean z) {
        this(networkKey, rssiCurve, z, null);
    }

    public ScoredNetwork(NetworkKey networkKey, RssiCurve rssiCurve, boolean z, Bundle bundle) {
        this.networkKey = networkKey;
        this.rssiCurve = rssiCurve;
        this.meteredHint = z;
        this.attributes = bundle;
    }

    private ScoredNetwork(Parcel parcel) {
        this.networkKey = NetworkKey.CREATOR.createFromParcel(parcel);
        if (parcel.readByte() == 1) {
            this.rssiCurve = RssiCurve.CREATOR.createFromParcel(parcel);
        } else {
            this.rssiCurve = null;
        }
        this.meteredHint = parcel.readByte() == 1;
        this.attributes = parcel.readBundle();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.networkKey.writeToParcel(parcel, i);
        if (this.rssiCurve != null) {
            parcel.writeByte((byte) 1);
            this.rssiCurve.writeToParcel(parcel, i);
        } else {
            parcel.writeByte((byte) 0);
        }
        parcel.writeByte(this.meteredHint ? (byte) 1 : (byte) 0);
        parcel.writeBundle(this.attributes);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ScoredNetwork scoredNetwork = (ScoredNetwork) obj;
        if (Objects.equals(this.networkKey, scoredNetwork.networkKey) && Objects.equals(this.rssiCurve, scoredNetwork.rssiCurve) && Objects.equals(Boolean.valueOf(this.meteredHint), Boolean.valueOf(scoredNetwork.meteredHint)) && bundleEquals(this.attributes, scoredNetwork.attributes)) {
            return true;
        }
        return false;
    }

    private boolean bundleEquals(Bundle bundle, Bundle bundle2) {
        if (bundle == bundle2) {
            return true;
        }
        if (bundle == null || bundle2 == null || bundle.size() != bundle2.size()) {
            return false;
        }
        for (String str : bundle.keySet()) {
            if (!Objects.equals(bundle.get(str), bundle2.get(str))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(this.networkKey, this.rssiCurve, Boolean.valueOf(this.meteredHint), this.attributes);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("ScoredNetwork{networkKey=" + this.networkKey + ", rssiCurve=" + this.rssiCurve + ", meteredHint=" + this.meteredHint);
        if (this.attributes != null && !this.attributes.isEmpty()) {
            sb.append(", attributes=" + this.attributes);
        }
        sb.append('}');
        return sb.toString();
    }

    public boolean hasRankingScore() {
        return this.rssiCurve != null || (this.attributes != null && this.attributes.containsKey(ATTRIBUTES_KEY_RANKING_SCORE_OFFSET));
    }

    public int calculateRankingScore(int i) throws UnsupportedOperationException {
        int i2;
        if (!hasRankingScore()) {
            throw new UnsupportedOperationException("Either rssiCurve or rankingScoreOffset is required to calculate the ranking score");
        }
        if (this.attributes != null) {
            i2 = this.attributes.getInt(ATTRIBUTES_KEY_RANKING_SCORE_OFFSET, 0) + 0;
        } else {
            i2 = 0;
        }
        int iLookupScore = this.rssiCurve != null ? this.rssiCurve.lookupScore(i) << 8 : 0;
        try {
            return Math.addExact(iLookupScore, i2);
        } catch (ArithmeticException e) {
            return iLookupScore < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
    }

    public int calculateBadge(int i) {
        if (this.attributes != null && this.attributes.containsKey(ATTRIBUTES_KEY_BADGING_CURVE)) {
            return ((RssiCurve) this.attributes.getParcelable(ATTRIBUTES_KEY_BADGING_CURVE)).lookupScore(i);
        }
        return 0;
    }
}
