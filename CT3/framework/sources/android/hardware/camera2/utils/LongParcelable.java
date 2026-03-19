package android.hardware.camera2.utils;

import android.os.Parcel;
import android.os.Parcelable;

public class LongParcelable implements Parcelable {
    public static final Parcelable.Creator<LongParcelable> CREATOR = new Parcelable.Creator<LongParcelable>() {
        @Override
        public LongParcelable createFromParcel(Parcel in) {
            return new LongParcelable(in, null);
        }

        @Override
        public LongParcelable[] newArray(int size) {
            return new LongParcelable[size];
        }
    };
    private long number;

    LongParcelable(Parcel in, LongParcelable longParcelable) {
        this(in);
    }

    public LongParcelable() {
        this.number = 0L;
    }

    public LongParcelable(long number) {
        this.number = number;
    }

    private LongParcelable(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.number);
    }

    public void readFromParcel(Parcel in) {
        this.number = in.readLong();
    }

    public long getNumber() {
        return this.number;
    }

    public void setNumber(long number) {
        this.number = number;
    }
}
