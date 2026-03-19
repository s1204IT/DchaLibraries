package android.os.connectivity;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;

public final class WifiBatteryStats implements Parcelable {
    public static final Parcelable.Creator<WifiBatteryStats> CREATOR = new Parcelable.Creator<WifiBatteryStats>() {
        @Override
        public WifiBatteryStats createFromParcel(Parcel parcel) {
            return new WifiBatteryStats(parcel);
        }

        @Override
        public WifiBatteryStats[] newArray(int i) {
            return new WifiBatteryStats[i];
        }
    };
    private long mEnergyConsumedMaMs;
    private long mIdleTimeMs;
    private long mKernelActiveTimeMs;
    private long mLoggingDurationMs;
    private long mNumAppScanRequest;
    private long mNumBytesRx;
    private long mNumBytesTx;
    private long mNumPacketsRx;
    private long mNumPacketsTx;
    private long mRxTimeMs;
    private long mScanTimeMs;
    private long mSleepTimeMs;
    private long[] mTimeInRxSignalStrengthLevelMs;
    private long[] mTimeInStateMs;
    private long[] mTimeInSupplicantStateMs;
    private long mTxTimeMs;

    public WifiBatteryStats() {
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
        parcel.writeLong(this.mScanTimeMs);
        parcel.writeLong(this.mIdleTimeMs);
        parcel.writeLong(this.mRxTimeMs);
        parcel.writeLong(this.mTxTimeMs);
        parcel.writeLong(this.mEnergyConsumedMaMs);
        parcel.writeLong(this.mNumAppScanRequest);
        parcel.writeLongArray(this.mTimeInStateMs);
        parcel.writeLongArray(this.mTimeInRxSignalStrengthLevelMs);
        parcel.writeLongArray(this.mTimeInSupplicantStateMs);
    }

    public void readFromParcel(Parcel parcel) {
        this.mLoggingDurationMs = parcel.readLong();
        this.mKernelActiveTimeMs = parcel.readLong();
        this.mNumPacketsTx = parcel.readLong();
        this.mNumBytesTx = parcel.readLong();
        this.mNumPacketsRx = parcel.readLong();
        this.mNumBytesRx = parcel.readLong();
        this.mSleepTimeMs = parcel.readLong();
        this.mScanTimeMs = parcel.readLong();
        this.mIdleTimeMs = parcel.readLong();
        this.mRxTimeMs = parcel.readLong();
        this.mTxTimeMs = parcel.readLong();
        this.mEnergyConsumedMaMs = parcel.readLong();
        this.mNumAppScanRequest = parcel.readLong();
        parcel.readLongArray(this.mTimeInStateMs);
        parcel.readLongArray(this.mTimeInRxSignalStrengthLevelMs);
        parcel.readLongArray(this.mTimeInSupplicantStateMs);
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

    public long getScanTimeMs() {
        return this.mScanTimeMs;
    }

    public long getIdleTimeMs() {
        return this.mIdleTimeMs;
    }

    public long getRxTimeMs() {
        return this.mRxTimeMs;
    }

    public long getTxTimeMs() {
        return this.mTxTimeMs;
    }

    public long getEnergyConsumedMaMs() {
        return this.mEnergyConsumedMaMs;
    }

    public long getNumAppScanRequest() {
        return this.mNumAppScanRequest;
    }

    public long[] getTimeInStateMs() {
        return this.mTimeInStateMs;
    }

    public long[] getTimeInRxSignalStrengthLevelMs() {
        return this.mTimeInRxSignalStrengthLevelMs;
    }

    public long[] getTimeInSupplicantStateMs() {
        return this.mTimeInSupplicantStateMs;
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

    public void setScanTimeMs(long j) {
        this.mScanTimeMs = j;
    }

    public void setIdleTimeMs(long j) {
        this.mIdleTimeMs = j;
    }

    public void setRxTimeMs(long j) {
        this.mRxTimeMs = j;
    }

    public void setTxTimeMs(long j) {
        this.mTxTimeMs = j;
    }

    public void setEnergyConsumedMaMs(long j) {
        this.mEnergyConsumedMaMs = j;
    }

    public void setNumAppScanRequest(long j) {
        this.mNumAppScanRequest = j;
    }

    public void setTimeInStateMs(long[] jArr) {
        this.mTimeInStateMs = Arrays.copyOfRange(jArr, 0, Math.min(jArr.length, 8));
    }

    public void setTimeInRxSignalStrengthLevelMs(long[] jArr) {
        this.mTimeInRxSignalStrengthLevelMs = Arrays.copyOfRange(jArr, 0, Math.min(jArr.length, 5));
    }

    public void setTimeInSupplicantStateMs(long[] jArr) {
        this.mTimeInSupplicantStateMs = Arrays.copyOfRange(jArr, 0, Math.min(jArr.length, 13));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private WifiBatteryStats(Parcel parcel) {
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
        this.mScanTimeMs = 0L;
        this.mIdleTimeMs = 0L;
        this.mRxTimeMs = 0L;
        this.mTxTimeMs = 0L;
        this.mEnergyConsumedMaMs = 0L;
        this.mNumAppScanRequest = 0L;
        this.mTimeInStateMs = new long[8];
        Arrays.fill(this.mTimeInStateMs, 0L);
        this.mTimeInRxSignalStrengthLevelMs = new long[5];
        Arrays.fill(this.mTimeInRxSignalStrengthLevelMs, 0L);
        this.mTimeInSupplicantStateMs = new long[13];
        Arrays.fill(this.mTimeInSupplicantStateMs, 0L);
    }
}
