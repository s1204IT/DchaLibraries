package android.os.health;

import android.os.Parcel;
import android.os.Parcelable;

public final class TimerStat implements Parcelable {
    public static final Parcelable.Creator<TimerStat> CREATOR = new Parcelable.Creator<TimerStat>() {
        @Override
        public TimerStat createFromParcel(Parcel parcel) {
            return new TimerStat(parcel);
        }

        @Override
        public TimerStat[] newArray(int i) {
            return new TimerStat[i];
        }
    };
    private int mCount;
    private long mTime;

    public TimerStat() {
    }

    public TimerStat(int i, long j) {
        this.mCount = i;
        this.mTime = j;
    }

    public TimerStat(Parcel parcel) {
        this.mCount = parcel.readInt();
        this.mTime = parcel.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mCount);
        parcel.writeLong(this.mTime);
    }

    public void setCount(int i) {
        this.mCount = i;
    }

    public int getCount() {
        return this.mCount;
    }

    public void setTime(long j) {
        this.mTime = j;
    }

    public long getTime() {
        return this.mTime;
    }
}
