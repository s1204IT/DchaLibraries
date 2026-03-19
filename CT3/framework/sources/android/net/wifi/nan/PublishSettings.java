package android.net.wifi.nan;

import android.os.Parcel;
import android.os.Parcelable;

public class PublishSettings implements Parcelable {
    public static final Parcelable.Creator<PublishSettings> CREATOR = new Parcelable.Creator<PublishSettings>() {
        @Override
        public PublishSettings[] newArray(int size) {
            return new PublishSettings[size];
        }

        @Override
        public PublishSettings createFromParcel(Parcel in) {
            int publishType = in.readInt();
            int publishCount = in.readInt();
            int ttlSec = in.readInt();
            return new PublishSettings(publishType, publishCount, ttlSec, null);
        }
    };
    public static final int PUBLISH_TYPE_SOLICITED = 1;
    public static final int PUBLISH_TYPE_UNSOLICITED = 0;
    public final int mPublishCount;
    public final int mPublishType;
    public final int mTtlSec;

    PublishSettings(int publishType, int publichCount, int ttlSec, PublishSettings publishSettings) {
        this(publishType, publichCount, ttlSec);
    }

    private PublishSettings(int publishType, int publichCount, int ttlSec) {
        this.mPublishType = publishType;
        this.mPublishCount = publichCount;
        this.mTtlSec = ttlSec;
    }

    public String toString() {
        return "PublishSettings [mPublishType=" + this.mPublishType + ", mPublishCount=" + this.mPublishCount + ", mTtlSec=" + this.mTtlSec + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mPublishType);
        dest.writeInt(this.mPublishCount);
        dest.writeInt(this.mTtlSec);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PublishSettings)) {
            return false;
        }
        if (this.mPublishType == obj.mPublishType && this.mPublishCount == obj.mPublishCount) {
            return this.mTtlSec == obj.mTtlSec;
        }
        return false;
    }

    public int hashCode() {
        int result = this.mPublishType + 527;
        return (((result * 31) + this.mPublishCount) * 31) + this.mTtlSec;
    }

    public static final class Builder {
        int mPublishCount;
        int mPublishType;
        int mTtlSec;

        public Builder setPublishType(int publishType) {
            if (publishType < 0 || publishType > 1) {
                throw new IllegalArgumentException("Invalid publishType - " + publishType);
            }
            this.mPublishType = publishType;
            return this;
        }

        public Builder setPublishCount(int publishCount) {
            if (publishCount < 0) {
                throw new IllegalArgumentException("Invalid publishCount - must be non-negative");
            }
            this.mPublishCount = publishCount;
            return this;
        }

        public Builder setTtlSec(int ttlSec) {
            if (ttlSec < 0) {
                throw new IllegalArgumentException("Invalid ttlSec - must be non-negative");
            }
            this.mTtlSec = ttlSec;
            return this;
        }

        public PublishSettings build() {
            return new PublishSettings(this.mPublishType, this.mPublishCount, this.mTtlSec, null);
        }
    }
}
