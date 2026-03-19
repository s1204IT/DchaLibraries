package android.net;

import android.os.Parcel;
import android.os.Parcelable;

public final class MatchAllNetworkSpecifier extends NetworkSpecifier implements Parcelable {
    public static final Parcelable.Creator<MatchAllNetworkSpecifier> CREATOR = new Parcelable.Creator<MatchAllNetworkSpecifier>() {
        @Override
        public MatchAllNetworkSpecifier createFromParcel(Parcel parcel) {
            return new MatchAllNetworkSpecifier();
        }

        @Override
        public MatchAllNetworkSpecifier[] newArray(int i) {
            return new MatchAllNetworkSpecifier[i];
        }
    };

    public static void checkNotMatchAllNetworkSpecifier(NetworkSpecifier networkSpecifier) {
        if (networkSpecifier instanceof MatchAllNetworkSpecifier) {
            throw new IllegalArgumentException("A MatchAllNetworkSpecifier is not permitted");
        }
    }

    @Override
    public boolean satisfiedBy(NetworkSpecifier networkSpecifier) {
        throw new IllegalStateException("MatchAllNetworkSpecifier must not be used in NetworkRequests");
    }

    public boolean equals(Object obj) {
        return obj instanceof MatchAllNetworkSpecifier;
    }

    public int hashCode() {
        return 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
    }
}
