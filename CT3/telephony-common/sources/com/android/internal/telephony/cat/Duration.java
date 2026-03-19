package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class Duration implements Parcelable {
    public static final Parcelable.Creator<Duration> CREATOR = new Parcelable.Creator<Duration>() {
        @Override
        public Duration createFromParcel(Parcel in) {
            return new Duration(in, (Duration) null);
        }

        @Override
        public Duration[] newArray(int size) {
            return new Duration[size];
        }
    };
    public int timeInterval;
    public TimeUnit timeUnit;

    Duration(Parcel in, Duration duration) {
        this(in);
    }

    public enum TimeUnit {
        MINUTE(0),
        SECOND(1),
        TENTH_SECOND(2);

        private int mValue;

        public static TimeUnit[] valuesCustom() {
            return values();
        }

        TimeUnit(int value) {
            this.mValue = value;
        }

        public int value() {
            return this.mValue;
        }
    }

    public Duration(int timeInterval, TimeUnit timeUnit) {
        this.timeInterval = timeInterval;
        this.timeUnit = timeUnit;
    }

    private Duration(Parcel in) {
        this.timeInterval = in.readInt();
        this.timeUnit = TimeUnit.valuesCustom()[in.readInt()];
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.timeInterval);
        dest.writeInt(this.timeUnit.ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
