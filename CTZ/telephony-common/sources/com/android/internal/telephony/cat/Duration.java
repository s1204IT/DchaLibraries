package com.android.internal.telephony.cat;

import android.os.Parcel;
import android.os.Parcelable;

public class Duration implements Parcelable {
    public static final Parcelable.Creator<Duration> CREATOR = new Parcelable.Creator<Duration>() {
        @Override
        public Duration createFromParcel(Parcel parcel) {
            return new Duration(parcel);
        }

        @Override
        public Duration[] newArray(int i) {
            return new Duration[i];
        }
    };
    public int timeInterval;
    public TimeUnit timeUnit;

    public enum TimeUnit {
        MINUTE(0),
        SECOND(1),
        TENTH_SECOND(2);

        private int mValue;

        TimeUnit(int i) {
            this.mValue = i;
        }

        public int value() {
            return this.mValue;
        }
    }

    public Duration(int i, TimeUnit timeUnit) {
        this.timeInterval = i;
        this.timeUnit = timeUnit;
    }

    private Duration(Parcel parcel) {
        this.timeInterval = parcel.readInt();
        this.timeUnit = TimeUnit.values()[parcel.readInt()];
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.timeInterval);
        parcel.writeInt(this.timeUnit.ordinal());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
