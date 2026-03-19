package android.bluetooth.le;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class PeriodicAdvertisingReport implements Parcelable {
    public static final Parcelable.Creator<PeriodicAdvertisingReport> CREATOR = new Parcelable.Creator<PeriodicAdvertisingReport>() {
        @Override
        public PeriodicAdvertisingReport createFromParcel(Parcel parcel) {
            return new PeriodicAdvertisingReport(parcel);
        }

        @Override
        public PeriodicAdvertisingReport[] newArray(int i) {
            return new PeriodicAdvertisingReport[i];
        }
    };
    public static final int DATA_COMPLETE = 0;
    public static final int DATA_INCOMPLETE_TRUNCATED = 2;
    private ScanRecord mData;
    private int mDataStatus;
    private int mRssi;
    private int mSyncHandle;
    private long mTimestampNanos;
    private int mTxPower;

    public PeriodicAdvertisingReport(int i, int i2, int i3, int i4, ScanRecord scanRecord) {
        this.mSyncHandle = i;
        this.mTxPower = i2;
        this.mRssi = i3;
        this.mDataStatus = i4;
        this.mData = scanRecord;
    }

    private PeriodicAdvertisingReport(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSyncHandle);
        parcel.writeInt(this.mTxPower);
        parcel.writeInt(this.mRssi);
        parcel.writeInt(this.mDataStatus);
        if (this.mData != null) {
            parcel.writeInt(1);
            parcel.writeByteArray(this.mData.getBytes());
        } else {
            parcel.writeInt(0);
        }
    }

    private void readFromParcel(Parcel parcel) {
        this.mSyncHandle = parcel.readInt();
        this.mTxPower = parcel.readInt();
        this.mRssi = parcel.readInt();
        this.mDataStatus = parcel.readInt();
        if (parcel.readInt() == 1) {
            this.mData = ScanRecord.parseFromBytes(parcel.createByteArray());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public int getSyncHandle() {
        return this.mSyncHandle;
    }

    public int getTxPower() {
        return this.mTxPower;
    }

    public int getRssi() {
        return this.mRssi;
    }

    public int getDataStatus() {
        return this.mDataStatus;
    }

    public ScanRecord getData() {
        return this.mData;
    }

    public long getTimestampNanos() {
        return this.mTimestampNanos;
    }

    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mSyncHandle), Integer.valueOf(this.mTxPower), Integer.valueOf(this.mRssi), Integer.valueOf(this.mDataStatus), this.mData, Long.valueOf(this.mTimestampNanos));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PeriodicAdvertisingReport periodicAdvertisingReport = (PeriodicAdvertisingReport) obj;
        if (this.mSyncHandle == periodicAdvertisingReport.mSyncHandle && this.mTxPower == periodicAdvertisingReport.mTxPower && this.mRssi == periodicAdvertisingReport.mRssi && this.mDataStatus == periodicAdvertisingReport.mDataStatus && Objects.equals(this.mData, periodicAdvertisingReport.mData) && this.mTimestampNanos == periodicAdvertisingReport.mTimestampNanos) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "PeriodicAdvertisingReport{syncHandle=" + this.mSyncHandle + ", txPower=" + this.mTxPower + ", rssi=" + this.mRssi + ", dataStatus=" + this.mDataStatus + ", data=" + Objects.toString(this.mData) + ", timestampNanos=" + this.mTimestampNanos + '}';
    }
}
