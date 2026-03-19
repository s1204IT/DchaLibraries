package android.bluetooth.le;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class ScanResult implements Parcelable {
    public static final Parcelable.Creator<ScanResult> CREATOR = new Parcelable.Creator<ScanResult>() {
        @Override
        public ScanResult createFromParcel(Parcel parcel) {
            return new ScanResult(parcel);
        }

        @Override
        public ScanResult[] newArray(int i) {
            return new ScanResult[i];
        }
    };
    public static final int DATA_COMPLETE = 0;
    public static final int DATA_TRUNCATED = 2;
    private static final int ET_CONNECTABLE_MASK = 1;
    private static final int ET_LEGACY_MASK = 16;
    public static final int PERIODIC_INTERVAL_NOT_PRESENT = 0;
    public static final int PHY_UNUSED = 0;
    public static final int SID_NOT_PRESENT = 255;
    public static final int TX_POWER_NOT_PRESENT = 127;
    private int mAdvertisingSid;
    private BluetoothDevice mDevice;
    private int mEventType;
    private int mPeriodicAdvertisingInterval;
    private int mPrimaryPhy;
    private int mRssi;
    private ScanRecord mScanRecord;
    private int mSecondaryPhy;
    private long mTimestampNanos;
    private int mTxPower;

    @Deprecated
    public ScanResult(BluetoothDevice bluetoothDevice, ScanRecord scanRecord, int i, long j) {
        this.mDevice = bluetoothDevice;
        this.mScanRecord = scanRecord;
        this.mRssi = i;
        this.mTimestampNanos = j;
        this.mEventType = 17;
        this.mPrimaryPhy = 1;
        this.mSecondaryPhy = 0;
        this.mAdvertisingSid = 255;
        this.mTxPower = 127;
        this.mPeriodicAdvertisingInterval = 0;
    }

    public ScanResult(BluetoothDevice bluetoothDevice, int i, int i2, int i3, int i4, int i5, int i6, int i7, ScanRecord scanRecord, long j) {
        this.mDevice = bluetoothDevice;
        this.mEventType = i;
        this.mPrimaryPhy = i2;
        this.mSecondaryPhy = i3;
        this.mAdvertisingSid = i4;
        this.mTxPower = i5;
        this.mRssi = i6;
        this.mPeriodicAdvertisingInterval = i7;
        this.mScanRecord = scanRecord;
        this.mTimestampNanos = j;
    }

    private ScanResult(Parcel parcel) {
        readFromParcel(parcel);
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (this.mDevice != null) {
            parcel.writeInt(1);
            this.mDevice.writeToParcel(parcel, i);
        } else {
            parcel.writeInt(0);
        }
        if (this.mScanRecord != null) {
            parcel.writeInt(1);
            parcel.writeByteArray(this.mScanRecord.getBytes());
        } else {
            parcel.writeInt(0);
        }
        parcel.writeInt(this.mRssi);
        parcel.writeLong(this.mTimestampNanos);
        parcel.writeInt(this.mEventType);
        parcel.writeInt(this.mPrimaryPhy);
        parcel.writeInt(this.mSecondaryPhy);
        parcel.writeInt(this.mAdvertisingSid);
        parcel.writeInt(this.mTxPower);
        parcel.writeInt(this.mPeriodicAdvertisingInterval);
    }

    private void readFromParcel(Parcel parcel) {
        if (parcel.readInt() == 1) {
            this.mDevice = BluetoothDevice.CREATOR.createFromParcel(parcel);
        }
        if (parcel.readInt() == 1) {
            this.mScanRecord = ScanRecord.parseFromBytes(parcel.createByteArray());
        }
        this.mRssi = parcel.readInt();
        this.mTimestampNanos = parcel.readLong();
        this.mEventType = parcel.readInt();
        this.mPrimaryPhy = parcel.readInt();
        this.mSecondaryPhy = parcel.readInt();
        this.mAdvertisingSid = parcel.readInt();
        this.mTxPower = parcel.readInt();
        this.mPeriodicAdvertisingInterval = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public BluetoothDevice getDevice() {
        return this.mDevice;
    }

    public ScanRecord getScanRecord() {
        return this.mScanRecord;
    }

    public int getRssi() {
        return this.mRssi;
    }

    public long getTimestampNanos() {
        return this.mTimestampNanos;
    }

    public boolean isLegacy() {
        return (this.mEventType & 16) != 0;
    }

    public boolean isConnectable() {
        return (this.mEventType & 1) != 0;
    }

    public int getDataStatus() {
        return (this.mEventType >> 5) & 3;
    }

    public int getPrimaryPhy() {
        return this.mPrimaryPhy;
    }

    public int getSecondaryPhy() {
        return this.mSecondaryPhy;
    }

    public int getAdvertisingSid() {
        return this.mAdvertisingSid;
    }

    public int getTxPower() {
        return this.mTxPower;
    }

    public int getPeriodicAdvertisingInterval() {
        return this.mPeriodicAdvertisingInterval;
    }

    public int hashCode() {
        return Objects.hash(this.mDevice, Integer.valueOf(this.mRssi), this.mScanRecord, Long.valueOf(this.mTimestampNanos), Integer.valueOf(this.mEventType), Integer.valueOf(this.mPrimaryPhy), Integer.valueOf(this.mSecondaryPhy), Integer.valueOf(this.mAdvertisingSid), Integer.valueOf(this.mTxPower), Integer.valueOf(this.mPeriodicAdvertisingInterval));
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ScanResult scanResult = (ScanResult) obj;
        if (Objects.equals(this.mDevice, scanResult.mDevice) && this.mRssi == scanResult.mRssi && Objects.equals(this.mScanRecord, scanResult.mScanRecord) && this.mTimestampNanos == scanResult.mTimestampNanos && this.mEventType == scanResult.mEventType && this.mPrimaryPhy == scanResult.mPrimaryPhy && this.mSecondaryPhy == scanResult.mSecondaryPhy && this.mAdvertisingSid == scanResult.mAdvertisingSid && this.mTxPower == scanResult.mTxPower && this.mPeriodicAdvertisingInterval == scanResult.mPeriodicAdvertisingInterval) {
            return true;
        }
        return false;
    }

    public String toString() {
        return "ScanResult{device=" + this.mDevice + ", scanRecord=" + Objects.toString(this.mScanRecord) + ", rssi=" + this.mRssi + ", timestampNanos=" + this.mTimestampNanos + ", eventType=" + this.mEventType + ", primaryPhy=" + this.mPrimaryPhy + ", secondaryPhy=" + this.mSecondaryPhy + ", advertisingSid=" + this.mAdvertisingSid + ", txPower=" + this.mTxPower + ", periodicAdvertisingInterval=" + this.mPeriodicAdvertisingInterval + '}';
    }
}
