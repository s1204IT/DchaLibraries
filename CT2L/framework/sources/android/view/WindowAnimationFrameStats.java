package android.view;

import android.os.Parcel;
import android.os.Parcelable;

public final class WindowAnimationFrameStats extends FrameStats implements Parcelable {
    public static final Parcelable.Creator<WindowAnimationFrameStats> CREATOR = new Parcelable.Creator<WindowAnimationFrameStats>() {
        @Override
        public WindowAnimationFrameStats createFromParcel(Parcel parcel) {
            return new WindowAnimationFrameStats(parcel);
        }

        @Override
        public WindowAnimationFrameStats[] newArray(int size) {
            return new WindowAnimationFrameStats[size];
        }
    };

    public WindowAnimationFrameStats() {
    }

    public void init(long refreshPeriodNano, long[] framesPresentedTimeNano) {
        this.mRefreshPeriodNano = refreshPeriodNano;
        this.mFramesPresentedTimeNano = framesPresentedTimeNano;
    }

    private WindowAnimationFrameStats(Parcel parcel) {
        this.mRefreshPeriodNano = parcel.readLong();
        this.mFramesPresentedTimeNano = parcel.createLongArray();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(this.mRefreshPeriodNano);
        parcel.writeLongArray(this.mFramesPresentedTimeNano);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WindowAnimationFrameStats[");
        builder.append("frameCount:" + getFrameCount());
        builder.append(", fromTimeNano:" + getStartTimeNano());
        builder.append(", toTimeNano:" + getEndTimeNano());
        builder.append(']');
        return builder.toString();
    }
}
