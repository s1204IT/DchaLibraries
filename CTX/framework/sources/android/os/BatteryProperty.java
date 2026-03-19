package android.os;

import android.os.Parcelable;

public class BatteryProperty implements Parcelable {
    public static final Parcelable.Creator<BatteryProperty> CREATOR = new Parcelable.Creator<BatteryProperty>() {
        @Override
        public BatteryProperty createFromParcel(Parcel parcel) {
            return new BatteryProperty(parcel);
        }

        @Override
        public BatteryProperty[] newArray(int i) {
            return new BatteryProperty[i];
        }
    };
    private long mValueLong;

    public BatteryProperty() {
        this.mValueLong = Long.MIN_VALUE;
    }

    public long getLong() {
        return this.mValueLong;
    }

    public void setLong(long j) {
        this.mValueLong = j;
    }

    private BatteryProperty(Parcel parcel) {
        readFromParcel(parcel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mValueLong = parcel.readLong();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mValueLong);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
