package android.os;

import android.os.Parcelable;

public class BatteryProperty implements Parcelable {
    public static final Parcelable.Creator<BatteryProperty> CREATOR = new Parcelable.Creator<BatteryProperty>() {
        @Override
        public BatteryProperty createFromParcel(Parcel p) {
            return new BatteryProperty(p);
        }

        @Override
        public BatteryProperty[] newArray(int size) {
            return new BatteryProperty[size];
        }
    };
    private long mValueLong;

    public BatteryProperty() {
        this.mValueLong = Long.MIN_VALUE;
    }

    public long getLong() {
        return this.mValueLong;
    }

    private BatteryProperty(Parcel p) {
        readFromParcel(p);
    }

    public void readFromParcel(Parcel p) {
        this.mValueLong = p.readLong();
    }

    @Override
    public void writeToParcel(Parcel p, int flags) {
        p.writeLong(this.mValueLong);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
