package android.net.wifi.nan;

import android.os.Parcel;
import android.os.Parcelable;

public class SubscribeSettings implements Parcelable {
    public static final Parcelable.Creator<SubscribeSettings> CREATOR = new Parcelable.Creator<SubscribeSettings>() {
        @Override
        public SubscribeSettings[] newArray(int size) {
            return new SubscribeSettings[size];
        }

        @Override
        public SubscribeSettings createFromParcel(Parcel in) {
            int subscribeType = in.readInt();
            int subscribeCount = in.readInt();
            int ttlSec = in.readInt();
            return new SubscribeSettings(subscribeType, subscribeCount, ttlSec, null);
        }
    };
    public static final int SUBSCRIBE_TYPE_ACTIVE = 1;
    public static final int SUBSCRIBE_TYPE_PASSIVE = 0;
    public final int mSubscribeCount;
    public final int mSubscribeType;
    public final int mTtlSec;

    SubscribeSettings(int subscribeType, int publichCount, int ttlSec, SubscribeSettings subscribeSettings) {
        this(subscribeType, publichCount, ttlSec);
    }

    private SubscribeSettings(int subscribeType, int publichCount, int ttlSec) {
        this.mSubscribeType = subscribeType;
        this.mSubscribeCount = publichCount;
        this.mTtlSec = ttlSec;
    }

    public String toString() {
        return "SubscribeSettings [mSubscribeType=" + this.mSubscribeType + ", mSubscribeCount=" + this.mSubscribeCount + ", mTtlSec=" + this.mTtlSec + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSubscribeType);
        dest.writeInt(this.mSubscribeCount);
        dest.writeInt(this.mTtlSec);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubscribeSettings)) {
            return false;
        }
        if (this.mSubscribeType == obj.mSubscribeType && this.mSubscribeCount == obj.mSubscribeCount) {
            return this.mTtlSec == obj.mTtlSec;
        }
        return false;
    }

    public int hashCode() {
        int result = this.mSubscribeType + 527;
        return (((result * 31) + this.mSubscribeCount) * 31) + this.mTtlSec;
    }

    public static final class Builder {
        int mSubscribeCount;
        int mSubscribeType;
        int mTtlSec;

        public Builder setSubscribeType(int subscribeType) {
            if (subscribeType < 0 || subscribeType > 1) {
                throw new IllegalArgumentException("Invalid subscribeType - " + subscribeType);
            }
            this.mSubscribeType = subscribeType;
            return this;
        }

        public Builder setSubscribeCount(int subscribeCount) {
            if (subscribeCount < 0) {
                throw new IllegalArgumentException("Invalid subscribeCount - must be non-negative");
            }
            this.mSubscribeCount = subscribeCount;
            return this;
        }

        public Builder setTtlSec(int ttlSec) {
            if (ttlSec < 0) {
                throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
            }
            this.mTtlSec = ttlSec;
            return this;
        }

        public SubscribeSettings build() {
            return new SubscribeSettings(this.mSubscribeType, this.mSubscribeCount, this.mTtlSec, null);
        }
    }
}
