package android.bluetooth.le;

import android.os.Parcel;
import android.os.Parcelable;

public final class ScanSettings implements Parcelable {
    public static final int CALLBACK_TYPE_ALL_MATCHES = 1;
    public static final int CALLBACK_TYPE_FIRST_MATCH = 2;
    public static final int CALLBACK_TYPE_MATCH_LOST = 4;
    public static final Parcelable.Creator<ScanSettings> CREATOR = new Parcelable.Creator<ScanSettings>() {
        @Override
        public ScanSettings[] newArray(int size) {
            return new ScanSettings[size];
        }

        @Override
        public ScanSettings createFromParcel(Parcel in) {
            return new ScanSettings(in);
        }
    };
    public static final int SCAN_MODE_BALANCED = 1;
    public static final int SCAN_MODE_LOW_LATENCY = 2;
    public static final int SCAN_MODE_LOW_POWER = 0;
    public static final int SCAN_RESULT_TYPE_ABBREVIATED = 1;
    public static final int SCAN_RESULT_TYPE_FULL = 0;
    private int mCallbackType;
    private long mReportDelayMillis;
    private int mScanMode;
    private int mScanResultType;

    public int getScanMode() {
        return this.mScanMode;
    }

    public int getCallbackType() {
        return this.mCallbackType;
    }

    public int getScanResultType() {
        return this.mScanResultType;
    }

    public long getReportDelayMillis() {
        return this.mReportDelayMillis;
    }

    private ScanSettings(int scanMode, int callbackType, int scanResultType, long reportDelayMillis) {
        this.mScanMode = scanMode;
        this.mCallbackType = callbackType;
        this.mScanResultType = scanResultType;
        this.mReportDelayMillis = reportDelayMillis;
    }

    private ScanSettings(Parcel in) {
        this.mScanMode = in.readInt();
        this.mCallbackType = in.readInt();
        this.mScanResultType = in.readInt();
        this.mReportDelayMillis = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mScanMode);
        dest.writeInt(this.mCallbackType);
        dest.writeInt(this.mScanResultType);
        dest.writeLong(this.mReportDelayMillis);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final class Builder {
        private int mScanMode = 0;
        private int mCallbackType = 1;
        private int mScanResultType = 0;
        private long mReportDelayMillis = 0;

        public Builder setScanMode(int scanMode) {
            if (scanMode < 0 || scanMode > 2) {
                throw new IllegalArgumentException("invalid scan mode " + scanMode);
            }
            this.mScanMode = scanMode;
            return this;
        }

        public Builder setCallbackType(int callbackType) {
            if (!isValidCallbackType(callbackType)) {
                throw new IllegalArgumentException("invalid callback type - " + callbackType);
            }
            this.mCallbackType = callbackType;
            return this;
        }

        private boolean isValidCallbackType(int callbackType) {
            return callbackType == 1 || callbackType == 2 || callbackType == 4 || callbackType == 6;
        }

        public Builder setScanResultType(int scanResultType) {
            if (scanResultType < 0 || scanResultType > 1) {
                throw new IllegalArgumentException("invalid scanResultType - " + scanResultType);
            }
            this.mScanResultType = scanResultType;
            return this;
        }

        public Builder setReportDelay(long reportDelayMillis) {
            if (reportDelayMillis < 0) {
                throw new IllegalArgumentException("reportDelay must be > 0");
            }
            this.mReportDelayMillis = reportDelayMillis;
            return this;
        }

        public ScanSettings build() {
            return new ScanSettings(this.mScanMode, this.mCallbackType, this.mScanResultType, this.mReportDelayMillis);
        }
    }
}
