package android.bluetooth.le;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

public final class ScanSettings implements Parcelable {
    public static final int CALLBACK_TYPE_ALL_MATCHES = 1;
    public static final int CALLBACK_TYPE_FIRST_MATCH = 2;
    public static final int CALLBACK_TYPE_MATCH_LOST = 4;
    public static final Parcelable.Creator<ScanSettings> CREATOR = new Parcelable.Creator<ScanSettings>() {
        @Override
        public ScanSettings[] newArray(int i) {
            return new ScanSettings[i];
        }

        @Override
        public ScanSettings createFromParcel(Parcel parcel) {
            return new ScanSettings(parcel);
        }
    };
    public static final int MATCH_MODE_AGGRESSIVE = 1;
    public static final int MATCH_MODE_STICKY = 2;
    public static final int MATCH_NUM_FEW_ADVERTISEMENT = 2;
    public static final int MATCH_NUM_MAX_ADVERTISEMENT = 3;
    public static final int MATCH_NUM_ONE_ADVERTISEMENT = 1;
    public static final int PHY_LE_ALL_SUPPORTED = 255;
    public static final int SCAN_MODE_BALANCED = 1;
    public static final int SCAN_MODE_LOW_LATENCY = 2;
    public static final int SCAN_MODE_LOW_POWER = 0;
    public static final int SCAN_MODE_OPPORTUNISTIC = -1;

    @SystemApi
    public static final int SCAN_RESULT_TYPE_ABBREVIATED = 1;

    @SystemApi
    public static final int SCAN_RESULT_TYPE_FULL = 0;
    private int mCallbackType;
    private boolean mLegacy;
    private int mMatchMode;
    private int mNumOfMatchesPerFilter;
    private int mPhy;
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

    public int getMatchMode() {
        return this.mMatchMode;
    }

    public int getNumOfMatches() {
        return this.mNumOfMatchesPerFilter;
    }

    public boolean getLegacy() {
        return this.mLegacy;
    }

    public int getPhy() {
        return this.mPhy;
    }

    public long getReportDelayMillis() {
        return this.mReportDelayMillis;
    }

    private ScanSettings(int i, int i2, int i3, long j, int i4, int i5, boolean z, int i6) {
        this.mScanMode = i;
        this.mCallbackType = i2;
        this.mScanResultType = i3;
        this.mReportDelayMillis = j;
        this.mNumOfMatchesPerFilter = i5;
        this.mMatchMode = i4;
        this.mLegacy = z;
        this.mPhy = i6;
    }

    private ScanSettings(Parcel parcel) {
        this.mScanMode = parcel.readInt();
        this.mCallbackType = parcel.readInt();
        this.mScanResultType = parcel.readInt();
        this.mReportDelayMillis = parcel.readLong();
        this.mMatchMode = parcel.readInt();
        this.mNumOfMatchesPerFilter = parcel.readInt();
        this.mLegacy = parcel.readInt() != 0;
        this.mPhy = parcel.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mScanMode);
        parcel.writeInt(this.mCallbackType);
        parcel.writeInt(this.mScanResultType);
        parcel.writeLong(this.mReportDelayMillis);
        parcel.writeInt(this.mMatchMode);
        parcel.writeInt(this.mNumOfMatchesPerFilter);
        parcel.writeInt(this.mLegacy ? 1 : 0);
        parcel.writeInt(this.mPhy);
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
        private int mMatchMode = 1;
        private int mNumOfMatchesPerFilter = 3;
        private boolean mLegacy = true;
        private int mPhy = 255;

        public Builder setScanMode(int i) {
            if (i < -1 || i > 2) {
                throw new IllegalArgumentException("invalid scan mode " + i);
            }
            this.mScanMode = i;
            return this;
        }

        public Builder setCallbackType(int i) {
            if (!isValidCallbackType(i)) {
                throw new IllegalArgumentException("invalid callback type - " + i);
            }
            this.mCallbackType = i;
            return this;
        }

        private boolean isValidCallbackType(int i) {
            return i == 1 || i == 2 || i == 4 || i == 6;
        }

        @SystemApi
        public Builder setScanResultType(int i) {
            if (i < 0 || i > 1) {
                throw new IllegalArgumentException("invalid scanResultType - " + i);
            }
            this.mScanResultType = i;
            return this;
        }

        public Builder setReportDelay(long j) {
            if (j < 0) {
                throw new IllegalArgumentException("reportDelay must be > 0");
            }
            this.mReportDelayMillis = j;
            return this;
        }

        public Builder setNumOfMatches(int i) {
            if (i < 1 || i > 3) {
                throw new IllegalArgumentException("invalid numOfMatches " + i);
            }
            this.mNumOfMatchesPerFilter = i;
            return this;
        }

        public Builder setMatchMode(int i) {
            if (i < 1 || i > 2) {
                throw new IllegalArgumentException("invalid matchMode " + i);
            }
            this.mMatchMode = i;
            return this;
        }

        public Builder setLegacy(boolean z) {
            this.mLegacy = z;
            return this;
        }

        public Builder setPhy(int i) {
            this.mPhy = i;
            return this;
        }

        public ScanSettings build() {
            return new ScanSettings(this.mScanMode, this.mCallbackType, this.mScanResultType, this.mReportDelayMillis, this.mMatchMode, this.mNumOfMatchesPerFilter, this.mLegacy, this.mPhy);
        }
    }
}
