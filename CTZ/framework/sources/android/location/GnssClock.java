package android.location;

import android.os.Parcel;
import android.os.Parcelable;

public final class GnssClock implements Parcelable {
    public static final Parcelable.Creator<GnssClock> CREATOR = new Parcelable.Creator<GnssClock>() {
        @Override
        public GnssClock createFromParcel(Parcel parcel) {
            GnssClock gnssClock = new GnssClock();
            gnssClock.mFlags = parcel.readInt();
            gnssClock.mLeapSecond = parcel.readInt();
            gnssClock.mTimeNanos = parcel.readLong();
            gnssClock.mTimeUncertaintyNanos = parcel.readDouble();
            gnssClock.mFullBiasNanos = parcel.readLong();
            gnssClock.mBiasNanos = parcel.readDouble();
            gnssClock.mBiasUncertaintyNanos = parcel.readDouble();
            gnssClock.mDriftNanosPerSecond = parcel.readDouble();
            gnssClock.mDriftUncertaintyNanosPerSecond = parcel.readDouble();
            gnssClock.mHardwareClockDiscontinuityCount = parcel.readInt();
            return gnssClock;
        }

        @Override
        public GnssClock[] newArray(int i) {
            return new GnssClock[i];
        }
    };
    private static final int HAS_BIAS = 8;
    private static final int HAS_BIAS_UNCERTAINTY = 16;
    private static final int HAS_DRIFT = 32;
    private static final int HAS_DRIFT_UNCERTAINTY = 64;
    private static final int HAS_FULL_BIAS = 4;
    private static final int HAS_LEAP_SECOND = 1;
    private static final int HAS_NO_FLAGS = 0;
    private static final int HAS_TIME_UNCERTAINTY = 2;
    private double mBiasNanos;
    private double mBiasUncertaintyNanos;
    private double mDriftNanosPerSecond;
    private double mDriftUncertaintyNanosPerSecond;
    private int mFlags;
    private long mFullBiasNanos;
    private int mHardwareClockDiscontinuityCount;
    private int mLeapSecond;
    private long mTimeNanos;
    private double mTimeUncertaintyNanos;

    public GnssClock() {
        initialize();
    }

    public void set(GnssClock gnssClock) {
        this.mFlags = gnssClock.mFlags;
        this.mLeapSecond = gnssClock.mLeapSecond;
        this.mTimeNanos = gnssClock.mTimeNanos;
        this.mTimeUncertaintyNanos = gnssClock.mTimeUncertaintyNanos;
        this.mFullBiasNanos = gnssClock.mFullBiasNanos;
        this.mBiasNanos = gnssClock.mBiasNanos;
        this.mBiasUncertaintyNanos = gnssClock.mBiasUncertaintyNanos;
        this.mDriftNanosPerSecond = gnssClock.mDriftNanosPerSecond;
        this.mDriftUncertaintyNanosPerSecond = gnssClock.mDriftUncertaintyNanosPerSecond;
        this.mHardwareClockDiscontinuityCount = gnssClock.mHardwareClockDiscontinuityCount;
    }

    public void reset() {
        initialize();
    }

    public boolean hasLeapSecond() {
        return isFlagSet(1);
    }

    public int getLeapSecond() {
        return this.mLeapSecond;
    }

    public void setLeapSecond(int i) {
        setFlag(1);
        this.mLeapSecond = i;
    }

    public void resetLeapSecond() {
        resetFlag(1);
        this.mLeapSecond = Integer.MIN_VALUE;
    }

    public long getTimeNanos() {
        return this.mTimeNanos;
    }

    public void setTimeNanos(long j) {
        this.mTimeNanos = j;
    }

    public boolean hasTimeUncertaintyNanos() {
        return isFlagSet(2);
    }

    public double getTimeUncertaintyNanos() {
        return this.mTimeUncertaintyNanos;
    }

    public void setTimeUncertaintyNanos(double d) {
        setFlag(2);
        this.mTimeUncertaintyNanos = d;
    }

    public void resetTimeUncertaintyNanos() {
        resetFlag(2);
        this.mTimeUncertaintyNanos = Double.NaN;
    }

    public boolean hasFullBiasNanos() {
        return isFlagSet(4);
    }

    public long getFullBiasNanos() {
        return this.mFullBiasNanos;
    }

    public void setFullBiasNanos(long j) {
        setFlag(4);
        this.mFullBiasNanos = j;
    }

    public void resetFullBiasNanos() {
        resetFlag(4);
        this.mFullBiasNanos = Long.MIN_VALUE;
    }

    public boolean hasBiasNanos() {
        return isFlagSet(8);
    }

    public double getBiasNanos() {
        return this.mBiasNanos;
    }

    public void setBiasNanos(double d) {
        setFlag(8);
        this.mBiasNanos = d;
    }

    public void resetBiasNanos() {
        resetFlag(8);
        this.mBiasNanos = Double.NaN;
    }

    public boolean hasBiasUncertaintyNanos() {
        return isFlagSet(16);
    }

    public double getBiasUncertaintyNanos() {
        return this.mBiasUncertaintyNanos;
    }

    public void setBiasUncertaintyNanos(double d) {
        setFlag(16);
        this.mBiasUncertaintyNanos = d;
    }

    public void resetBiasUncertaintyNanos() {
        resetFlag(16);
        this.mBiasUncertaintyNanos = Double.NaN;
    }

    public boolean hasDriftNanosPerSecond() {
        return isFlagSet(32);
    }

    public double getDriftNanosPerSecond() {
        return this.mDriftNanosPerSecond;
    }

    public void setDriftNanosPerSecond(double d) {
        setFlag(32);
        this.mDriftNanosPerSecond = d;
    }

    public void resetDriftNanosPerSecond() {
        resetFlag(32);
        this.mDriftNanosPerSecond = Double.NaN;
    }

    public boolean hasDriftUncertaintyNanosPerSecond() {
        return isFlagSet(64);
    }

    public double getDriftUncertaintyNanosPerSecond() {
        return this.mDriftUncertaintyNanosPerSecond;
    }

    public void setDriftUncertaintyNanosPerSecond(double d) {
        setFlag(64);
        this.mDriftUncertaintyNanosPerSecond = d;
    }

    public void resetDriftUncertaintyNanosPerSecond() {
        resetFlag(64);
        this.mDriftUncertaintyNanosPerSecond = Double.NaN;
    }

    public int getHardwareClockDiscontinuityCount() {
        return this.mHardwareClockDiscontinuityCount;
    }

    public void setHardwareClockDiscontinuityCount(int i) {
        this.mHardwareClockDiscontinuityCount = i;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mFlags);
        parcel.writeInt(this.mLeapSecond);
        parcel.writeLong(this.mTimeNanos);
        parcel.writeDouble(this.mTimeUncertaintyNanos);
        parcel.writeLong(this.mFullBiasNanos);
        parcel.writeDouble(this.mBiasNanos);
        parcel.writeDouble(this.mBiasUncertaintyNanos);
        parcel.writeDouble(this.mDriftNanosPerSecond);
        parcel.writeDouble(this.mDriftUncertaintyNanosPerSecond);
        parcel.writeInt(this.mHardwareClockDiscontinuityCount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("GnssClock:\n");
        Object[] objArr = new Object[2];
        objArr[0] = "LeapSecond";
        objArr[1] = hasLeapSecond() ? Integer.valueOf(this.mLeapSecond) : null;
        sb.append(String.format("   %-15s = %s\n", objArr));
        Object[] objArr2 = new Object[4];
        objArr2[0] = "TimeNanos";
        objArr2[1] = Long.valueOf(this.mTimeNanos);
        objArr2[2] = "TimeUncertaintyNanos";
        objArr2[3] = hasTimeUncertaintyNanos() ? Double.valueOf(this.mTimeUncertaintyNanos) : null;
        sb.append(String.format("   %-15s = %-25s   %-26s = %s\n", objArr2));
        Object[] objArr3 = new Object[2];
        objArr3[0] = "FullBiasNanos";
        objArr3[1] = hasFullBiasNanos() ? Long.valueOf(this.mFullBiasNanos) : null;
        sb.append(String.format("   %-15s = %s\n", objArr3));
        Object[] objArr4 = new Object[4];
        objArr4[0] = "BiasNanos";
        objArr4[1] = hasBiasNanos() ? Double.valueOf(this.mBiasNanos) : null;
        objArr4[2] = "BiasUncertaintyNanos";
        objArr4[3] = hasBiasUncertaintyNanos() ? Double.valueOf(this.mBiasUncertaintyNanos) : null;
        sb.append(String.format("   %-15s = %-25s   %-26s = %s\n", objArr4));
        Object[] objArr5 = new Object[4];
        objArr5[0] = "DriftNanosPerSecond";
        objArr5[1] = hasDriftNanosPerSecond() ? Double.valueOf(this.mDriftNanosPerSecond) : null;
        objArr5[2] = "DriftUncertaintyNanosPerSecond";
        objArr5[3] = hasDriftUncertaintyNanosPerSecond() ? Double.valueOf(this.mDriftUncertaintyNanosPerSecond) : null;
        sb.append(String.format("   %-15s = %-25s   %-26s = %s\n", objArr5));
        sb.append(String.format("   %-15s = %s\n", "HardwareClockDiscontinuityCount", Integer.valueOf(this.mHardwareClockDiscontinuityCount)));
        return sb.toString();
    }

    private void initialize() {
        this.mFlags = 0;
        resetLeapSecond();
        setTimeNanos(Long.MIN_VALUE);
        resetTimeUncertaintyNanos();
        resetFullBiasNanos();
        resetBiasNanos();
        resetBiasUncertaintyNanos();
        resetDriftNanosPerSecond();
        resetDriftUncertaintyNanosPerSecond();
        setHardwareClockDiscontinuityCount(Integer.MIN_VALUE);
    }

    private void setFlag(int i) {
        this.mFlags = i | this.mFlags;
    }

    private void resetFlag(int i) {
        this.mFlags = (~i) & this.mFlags;
    }

    private boolean isFlagSet(int i) {
        return (this.mFlags & i) == i;
    }
}
