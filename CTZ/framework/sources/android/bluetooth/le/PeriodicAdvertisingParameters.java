package android.bluetooth.le;

import android.os.Parcel;
import android.os.Parcelable;

public final class PeriodicAdvertisingParameters implements Parcelable {
    public static final Parcelable.Creator<PeriodicAdvertisingParameters> CREATOR = new Parcelable.Creator<PeriodicAdvertisingParameters>() {
        @Override
        public PeriodicAdvertisingParameters[] newArray(int i) {
            return new PeriodicAdvertisingParameters[i];
        }

        @Override
        public PeriodicAdvertisingParameters createFromParcel(Parcel parcel) {
            return new PeriodicAdvertisingParameters(parcel);
        }
    };
    private static final int INTERVAL_MAX = 65519;
    private static final int INTERVAL_MIN = 80;
    private final boolean mIncludeTxPower;
    private final int mInterval;

    private PeriodicAdvertisingParameters(boolean z, int i) {
        this.mIncludeTxPower = z;
        this.mInterval = i;
    }

    private PeriodicAdvertisingParameters(Parcel parcel) {
        this.mIncludeTxPower = parcel.readInt() != 0;
        this.mInterval = parcel.readInt();
    }

    public boolean getIncludeTxPower() {
        return this.mIncludeTxPower;
    }

    public int getInterval() {
        return this.mInterval;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mIncludeTxPower ? 1 : 0);
        parcel.writeInt(this.mInterval);
    }

    public static final class Builder {
        private boolean mIncludeTxPower = false;
        private int mInterval = PeriodicAdvertisingParameters.INTERVAL_MAX;

        public Builder setIncludeTxPower(boolean z) {
            this.mIncludeTxPower = z;
            return this;
        }

        public Builder setInterval(int i) {
            if (i < 80 || i > PeriodicAdvertisingParameters.INTERVAL_MAX) {
                throw new IllegalArgumentException("Invalid interval (must be 80-65519)");
            }
            this.mInterval = i;
            return this;
        }

        public PeriodicAdvertisingParameters build() {
            return new PeriodicAdvertisingParameters(this.mIncludeTxPower, this.mInterval);
        }
    }
}
