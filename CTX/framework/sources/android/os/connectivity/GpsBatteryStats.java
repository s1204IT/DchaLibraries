package android.os.connectivity;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class GpsBatteryStats implements Parcelable {
    public static final Parcelable.Creator<GpsBatteryStats> CREATOR = new Parcelable.Creator<GpsBatteryStats>() {
        @Override
        public GpsBatteryStats createFromParcel(Parcel parcel) {
            return new GpsBatteryStats(parcel);
        }

        @Override
        public GpsBatteryStats[] newArray(int i) {
            return new GpsBatteryStats[i];
        }
    };
    private long mEnergyConsumedMaMs;
    private long mLoggingDurationMs;
    private long[] mTimeInGpsSignalQualityLevel;

    public GpsBatteryStats() {
        initialize();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mLoggingDurationMs);
        parcel.writeLong(this.mEnergyConsumedMaMs);
        parcel.writeLongArray(this.mTimeInGpsSignalQualityLevel);
    }

    public void readFromParcel(Parcel parcel) {
        this.mLoggingDurationMs = parcel.readLong();
        this.mEnergyConsumedMaMs = parcel.readLong();
        parcel.readLongArray(this.mTimeInGpsSignalQualityLevel);
    }

    public long getLoggingDurationMs() {
        return this.mLoggingDurationMs;
    }

    public long getEnergyConsumedMaMs() {
        return this.mEnergyConsumedMaMs;
    }

    public long[] getTimeInGpsSignalQualityLevel() {
        return this.mTimeInGpsSignalQualityLevel;
    }

    public void setLoggingDurationMs(long j) {
        this.mLoggingDurationMs = j;
    }

    public void setEnergyConsumedMaMs(long j) {
        this.mEnergyConsumedMaMs = j;
    }

    public void setTimeInGpsSignalQualityLevel(long[] jArr) {
        this.mTimeInGpsSignalQualityLevel = Arrays.copyOfRange(jArr, 0, Math.min(jArr.length, 2));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private GpsBatteryStats(Parcel parcel) {
        initialize();
        readFromParcel(parcel);
    }

    private void initialize() {
        this.mLoggingDurationMs = 0L;
        this.mEnergyConsumedMaMs = 0L;
        this.mTimeInGpsSignalQualityLevel = new long[2];
    }
}
