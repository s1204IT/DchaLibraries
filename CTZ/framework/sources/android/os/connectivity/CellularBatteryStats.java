package android.os.connectivity;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class CellularBatteryStats implements Parcelable {
    public static final Parcelable.Creator<CellularBatteryStats> CREATOR = new Parcelable.Creator<CellularBatteryStats>() {
        @Override
        public CellularBatteryStats createFromParcel(Parcel parcel) {
            return new CellularBatteryStats(parcel);
        }

        @Override
        public CellularBatteryStats[] newArray(int i) {
            return new CellularBatteryStats[i];
        }
    };
    private long mEnergyConsumedMaMs;
    private long mIdleTimeMs;
    private long mKernelActiveTimeMs;
    private long mLoggingDurationMs;
    private long mNumBytesRx;
    private long mNumBytesTx;
    private long mNumPacketsRx;
    private long mNumPacketsTx;
    private long mRxTimeMs;
    private long mSleepTimeMs;
    private long[] mTimeInRatMs;
    private long[] mTimeInRxSignalStrengthLevelMs;
    private long[] mTxTimeMs;

    public CellularBatteryStats() {
        initialize();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeLong(this.mLoggingDurationMs);
        parcel.writeLong(this.mKernelActiveTimeMs);
        parcel.writeLong(this.mNumPacketsTx);
        parcel.writeLong(this.mNumBytesTx);
        parcel.writeLong(this.mNumPacketsRx);
        parcel.writeLong(this.mNumBytesRx);
        parcel.writeLong(this.mSleepTimeMs);
        parcel.writeLong(this.mIdleTimeMs);
        parcel.writeLong(this.mRxTimeMs);
        parcel.writeLong(this.mEnergyConsumedMaMs);
        parcel.writeLongArray(this.mTimeInRatMs);
        parcel.writeLongArray(this.mTimeInRxSignalStrengthLevelMs);
        parcel.writeLongArray(this.mTxTimeMs);
    }

    public void readFromParcel(Parcel parcel) {
        this.mLoggingDurationMs = parcel.readLong();
        this.mKernelActiveTimeMs = parcel.readLong();
        this.mNumPacketsTx = parcel.readLong();
        this.mNumBytesTx = parcel.readLong();
        this.mNumPacketsRx = parcel.readLong();
        this.mNumBytesRx = parcel.readLong();
        this.mSleepTimeMs = parcel.readLong();
        this.mIdleTimeMs = parcel.readLong();
        this.mRxTimeMs = parcel.readLong();
        this.mEnergyConsumedMaMs = parcel.readLong();
        parcel.readLongArray(this.mTimeInRatMs);
        parcel.readLongArray(this.mTimeInRxSignalStrengthLevelMs);
        parcel.readLongArray(this.mTxTimeMs);
    }

    public long getLoggingDurationMs() {
        return this.mLoggingDurationMs;
    }

    public long getKernelActiveTimeMs() {
        return this.mKernelActiveTimeMs;
    }

    public long getNumPacketsTx() {
        return this.mNumPacketsTx;
    }

    public long getNumBytesTx() {
        return this.mNumBytesTx;
    }

    public long getNumPacketsRx() {
        return this.mNumPacketsRx;
    }

    public long getNumBytesRx() {
        return this.mNumBytesRx;
    }

    public long getSleepTimeMs() {
        return this.mSleepTimeMs;
    }

    public long getIdleTimeMs() {
        return this.mIdleTimeMs;
    }

    public long getRxTimeMs() {
        return this.mRxTimeMs;
    }

    public long getEnergyConsumedMaMs() {
        return this.mEnergyConsumedMaMs;
    }

    public long[] getTimeInRatMs() {
        return this.mTimeInRatMs;
    }

    public long[] getTimeInRxSignalStrengthLevelMs() {
        return this.mTimeInRxSignalStrengthLevelMs;
    }

    public long[] getTxTimeMs() {
        return this.mTxTimeMs;
    }

    public void setLoggingDurationMs(long j) {
        this.mLoggingDurationMs = j;
    }

    public void setKernelActiveTimeMs(long j) {
        this.mKernelActiveTimeMs = j;
    }

    public void setNumPacketsTx(long j) {
        this.mNumPacketsTx = j;
    }

    public void setNumBytesTx(long j) {
        this.mNumBytesTx = j;
    }

    public void setNumPacketsRx(long j) {
        this.mNumPacketsRx = j;
    }

    public void setNumBytesRx(long j) {
        this.mNumBytesRx = j;
    }

    public void setSleepTimeMs(long j) {
        this.mSleepTimeMs = j;
    }

    public void setIdleTimeMs(long j) {
        this.mIdleTimeMs = j;
    }

    public void setRxTimeMs(long j) {
        this.mRxTimeMs = j;
    }

    public void setEnergyConsumedMaMs(long j) {
        this.mEnergyConsumedMaMs = j;
    }

    public void setTimeInRatMs(long[] jArr) {
        this.mTimeInRatMs = Arrays.copyOfRange(jArr, 0, Math.min(jArr.length, 21));
    }

    public void setTimeInRxSignalStrengthLevelMs(long[] jArr) {
        this.mTimeInRxSignalStrengthLevelMs = Arrays.copyOfRange(jArr, 0, Math.min(jArr.length, 5));
    }

    public void setTxTimeMs(long[] jArr) {
        this.mTxTimeMs = Arrays.copyOfRange(jArr, 0, Math.min(jArr.length, 5));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private CellularBatteryStats(Parcel parcel) {
        initialize();
        readFromParcel(parcel);
    }

    private void initialize() {
        this.mLoggingDurationMs = 0L;
        this.mKernelActiveTimeMs = 0L;
        this.mNumPacketsTx = 0L;
        this.mNumBytesTx = 0L;
        this.mNumPacketsRx = 0L;
        this.mNumBytesRx = 0L;
        this.mSleepTimeMs = 0L;
        this.mIdleTimeMs = 0L;
        this.mRxTimeMs = 0L;
        this.mEnergyConsumedMaMs = 0L;
        this.mTimeInRatMs = new long[21];
        Arrays.fill(this.mTimeInRatMs, 0L);
        this.mTimeInRxSignalStrengthLevelMs = new long[5];
        Arrays.fill(this.mTimeInRxSignalStrengthLevelMs, 0L);
        this.mTxTimeMs = new long[5];
        Arrays.fill(this.mTxTimeMs, 0L);
    }
}
