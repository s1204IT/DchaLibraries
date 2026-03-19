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
        public WindowAnimationFrameStats[] newArray(int i) {
            return new WindowAnimationFrameStats[i];
        }
    };

    public WindowAnimationFrameStats() {
    }

    public void init(long j, long[] jArr) {
        this.mRefreshPeriodNano = j;
        this.mFramesPresentedTimeNano = jArr;
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
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mRefreshPeriodNano);
        parcel.writeLongArray(this.mFramesPresentedTimeNano);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("WindowAnimationFrameStats[");
        sb.append("frameCount:" + getFrameCount());
        sb.append(", fromTimeNano:" + getStartTimeNano());
        sb.append(", toTimeNano:" + getEndTimeNano());
        sb.append(']');
        return sb.toString();
    }
}
