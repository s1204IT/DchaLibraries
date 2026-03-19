package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class PhysicalChannelConfig implements Parcelable {
    public static final int CONNECTION_PRIMARY_SERVING = 1;
    public static final int CONNECTION_SECONDARY_SERVING = 2;
    public static final int CONNECTION_UNKNOWN = Integer.MAX_VALUE;
    public static final Parcelable.Creator<PhysicalChannelConfig> CREATOR = new Parcelable.Creator<PhysicalChannelConfig>() {
        @Override
        public PhysicalChannelConfig createFromParcel(Parcel parcel) {
            return new PhysicalChannelConfig(parcel);
        }

        @Override
        public PhysicalChannelConfig[] newArray(int i) {
            return new PhysicalChannelConfig[i];
        }
    };
    private int mCellBandwidthDownlinkKhz;
    private int mCellConnectionStatus;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionStatus {
    }

    public PhysicalChannelConfig(int i, int i2) {
        this.mCellConnectionStatus = i;
        this.mCellBandwidthDownlinkKhz = i2;
    }

    public PhysicalChannelConfig(Parcel parcel) {
        this.mCellConnectionStatus = parcel.readInt();
        this.mCellBandwidthDownlinkKhz = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mCellConnectionStatus);
        parcel.writeInt(this.mCellBandwidthDownlinkKhz);
    }

    public int getCellBandwidthDownlink() {
        return this.mCellBandwidthDownlinkKhz;
    }

    public int getConnectionStatus() {
        return this.mCellConnectionStatus;
    }

    private String getConnectionStatusString() {
        int i = this.mCellConnectionStatus;
        if (i != Integer.MAX_VALUE) {
            switch (i) {
                case 1:
                    return "PrimaryServing";
                case 2:
                    return "SecondaryServing";
                default:
                    return "Invalid(" + this.mCellConnectionStatus + ")";
            }
        }
        return "Unknown";
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PhysicalChannelConfig)) {
            return false;
        }
        PhysicalChannelConfig physicalChannelConfig = (PhysicalChannelConfig) obj;
        return this.mCellConnectionStatus == physicalChannelConfig.mCellConnectionStatus && this.mCellBandwidthDownlinkKhz == physicalChannelConfig.mCellBandwidthDownlinkKhz;
    }

    public int hashCode() {
        return (this.mCellBandwidthDownlinkKhz * 29) + (this.mCellConnectionStatus * 31);
    }

    public String toString() {
        return "{mConnectionStatus=" + getConnectionStatusString() + ",mCellBandwidthDownlinkKhz=" + this.mCellBandwidthDownlinkKhz + "}";
    }
}
