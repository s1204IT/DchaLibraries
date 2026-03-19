package android.net.wifi.nan;

import android.os.Parcel;
import android.os.Parcelable;

public class ConfigRequest implements Parcelable {
    public static final int CLUSTER_ID_MAX = 65535;
    public static final int CLUSTER_ID_MIN = 0;
    public static final Parcelable.Creator<ConfigRequest> CREATOR = new Parcelable.Creator<ConfigRequest>() {
        @Override
        public ConfigRequest[] newArray(int size) {
            return new ConfigRequest[size];
        }

        @Override
        public ConfigRequest createFromParcel(Parcel in) {
            boolean support5gBand = in.readInt() != 0;
            int masterPreference = in.readInt();
            int clusterLow = in.readInt();
            int clusterHigh = in.readInt();
            return new ConfigRequest(support5gBand, masterPreference, clusterLow, clusterHigh, null);
        }
    };
    public final int mClusterHigh;
    public final int mClusterLow;
    public final int mMasterPreference;
    public final boolean mSupport5gBand;

    ConfigRequest(boolean support5gBand, int masterPreference, int clusterLow, int clusterHigh, ConfigRequest configRequest) {
        this(support5gBand, masterPreference, clusterLow, clusterHigh);
    }

    private ConfigRequest(boolean support5gBand, int masterPreference, int clusterLow, int clusterHigh) {
        this.mSupport5gBand = support5gBand;
        this.mMasterPreference = masterPreference;
        this.mClusterLow = clusterLow;
        this.mClusterHigh = clusterHigh;
    }

    public String toString() {
        return "ConfigRequest [mSupport5gBand=" + this.mSupport5gBand + ", mMasterPreference=" + this.mMasterPreference + ", mClusterLow=" + this.mClusterLow + ", mClusterHigh=" + this.mClusterHigh + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSupport5gBand ? 1 : 0);
        dest.writeInt(this.mMasterPreference);
        dest.writeInt(this.mClusterLow);
        dest.writeInt(this.mClusterHigh);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ConfigRequest)) {
            return false;
        }
        if (this.mSupport5gBand == obj.mSupport5gBand && this.mMasterPreference == obj.mMasterPreference && this.mClusterLow == obj.mClusterLow) {
            return this.mClusterHigh == obj.mClusterHigh;
        }
        return false;
    }

    public int hashCode() {
        int result = (this.mSupport5gBand ? 1 : 0) + 527;
        return (((((result * 31) + this.mMasterPreference) * 31) + this.mClusterLow) * 31) + this.mClusterHigh;
    }

    public static final class Builder {
        private boolean mSupport5gBand = false;
        private int mMasterPreference = 0;
        private int mClusterLow = 0;
        private int mClusterHigh = 65535;

        public Builder setSupport5gBand(boolean support5gBand) {
            this.mSupport5gBand = support5gBand;
            return this;
        }

        public Builder setMasterPreference(int masterPreference) {
            if (masterPreference < 0) {
                throw new IllegalArgumentException("Master Preference specification must be non-negative");
            }
            if (masterPreference == 1 || masterPreference == 255 || masterPreference > 255) {
                throw new IllegalArgumentException("Master Preference specification must not exceed 255 or use 1 or 255 (reserved values)");
            }
            this.mMasterPreference = masterPreference;
            return this;
        }

        public Builder setClusterLow(int clusterLow) {
            if (clusterLow < 0) {
                throw new IllegalArgumentException("Cluster specification must be non-negative");
            }
            if (clusterLow > 65535) {
                throw new IllegalArgumentException("Cluster specification must not exceed 0xFFFF");
            }
            this.mClusterLow = clusterLow;
            return this;
        }

        public Builder setClusterHigh(int clusterHigh) {
            if (clusterHigh < 0) {
                throw new IllegalArgumentException("Cluster specification must be non-negative");
            }
            if (clusterHigh > 65535) {
                throw new IllegalArgumentException("Cluster specification must not exceed 0xFFFF");
            }
            this.mClusterHigh = clusterHigh;
            return this;
        }

        public ConfigRequest build() {
            if (this.mClusterLow > this.mClusterHigh) {
                throw new IllegalArgumentException("Invalid argument combination - must have Cluster Low <= Cluster High");
            }
            return new ConfigRequest(this.mSupport5gBand, this.mMasterPreference, this.mClusterLow, this.mClusterHigh, null);
        }
    }
}
