package android.bluetooth.le;

import android.os.Parcel;
import android.os.Parcelable;

public final class AdvertisingSetParameters implements Parcelable {
    public static final Parcelable.Creator<AdvertisingSetParameters> CREATOR = new Parcelable.Creator<AdvertisingSetParameters>() {
        @Override
        public AdvertisingSetParameters[] newArray(int i) {
            return new AdvertisingSetParameters[i];
        }

        @Override
        public AdvertisingSetParameters createFromParcel(Parcel parcel) {
            return new AdvertisingSetParameters(parcel);
        }
    };
    public static final int INTERVAL_HIGH = 1600;
    public static final int INTERVAL_LOW = 160;
    public static final int INTERVAL_MAX = 16777215;
    public static final int INTERVAL_MEDIUM = 400;
    public static final int INTERVAL_MIN = 160;
    private static final int LIMITED_ADVERTISING_MAX_MILLIS = 180000;
    public static final int TX_POWER_HIGH = 1;
    public static final int TX_POWER_LOW = -15;
    public static final int TX_POWER_MAX = 1;
    public static final int TX_POWER_MEDIUM = -7;
    public static final int TX_POWER_MIN = -127;
    public static final int TX_POWER_ULTRA_LOW = -21;
    private final boolean mConnectable;
    private final boolean mIncludeTxPower;
    private final int mInterval;
    private final boolean mIsAnonymous;
    private final boolean mIsLegacy;
    private final int mPrimaryPhy;
    private final boolean mScannable;
    private final int mSecondaryPhy;
    private final int mTxPowerLevel;

    private AdvertisingSetParameters(boolean z, boolean z2, boolean z3, boolean z4, boolean z5, int i, int i2, int i3, int i4) {
        this.mConnectable = z;
        this.mScannable = z2;
        this.mIsLegacy = z3;
        this.mIsAnonymous = z4;
        this.mIncludeTxPower = z5;
        this.mPrimaryPhy = i;
        this.mSecondaryPhy = i2;
        this.mInterval = i3;
        this.mTxPowerLevel = i4;
    }

    private AdvertisingSetParameters(Parcel parcel) {
        this.mConnectable = parcel.readInt() != 0;
        this.mScannable = parcel.readInt() != 0;
        this.mIsLegacy = parcel.readInt() != 0;
        this.mIsAnonymous = parcel.readInt() != 0;
        this.mIncludeTxPower = parcel.readInt() != 0;
        this.mPrimaryPhy = parcel.readInt();
        this.mSecondaryPhy = parcel.readInt();
        this.mInterval = parcel.readInt();
        this.mTxPowerLevel = parcel.readInt();
    }

    public boolean isConnectable() {
        return this.mConnectable;
    }

    public boolean isScannable() {
        return this.mScannable;
    }

    public boolean isLegacy() {
        return this.mIsLegacy;
    }

    public boolean isAnonymous() {
        return this.mIsAnonymous;
    }

    public boolean includeTxPower() {
        return this.mIncludeTxPower;
    }

    public int getPrimaryPhy() {
        return this.mPrimaryPhy;
    }

    public int getSecondaryPhy() {
        return this.mSecondaryPhy;
    }

    public int getInterval() {
        return this.mInterval;
    }

    public int getTxPowerLevel() {
        return this.mTxPowerLevel;
    }

    public String toString() {
        return "AdvertisingSetParameters [connectable=" + this.mConnectable + ", isLegacy=" + this.mIsLegacy + ", isAnonymous=" + this.mIsAnonymous + ", includeTxPower=" + this.mIncludeTxPower + ", primaryPhy=" + this.mPrimaryPhy + ", secondaryPhy=" + this.mSecondaryPhy + ", interval=" + this.mInterval + ", txPowerLevel=" + this.mTxPowerLevel + "]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mConnectable ? 1 : 0);
        parcel.writeInt(this.mScannable ? 1 : 0);
        parcel.writeInt(this.mIsLegacy ? 1 : 0);
        parcel.writeInt(this.mIsAnonymous ? 1 : 0);
        parcel.writeInt(this.mIncludeTxPower ? 1 : 0);
        parcel.writeInt(this.mPrimaryPhy);
        parcel.writeInt(this.mSecondaryPhy);
        parcel.writeInt(this.mInterval);
        parcel.writeInt(this.mTxPowerLevel);
    }

    public static final class Builder {
        private boolean mConnectable = false;
        private boolean mScannable = false;
        private boolean mIsLegacy = false;
        private boolean mIsAnonymous = false;
        private boolean mIncludeTxPower = false;
        private int mPrimaryPhy = 1;
        private int mSecondaryPhy = 1;
        private int mInterval = 160;
        private int mTxPowerLevel = -7;

        public Builder setConnectable(boolean z) {
            this.mConnectable = z;
            return this;
        }

        public Builder setScannable(boolean z) {
            this.mScannable = z;
            return this;
        }

        public Builder setLegacyMode(boolean z) {
            this.mIsLegacy = z;
            return this;
        }

        public Builder setAnonymous(boolean z) {
            this.mIsAnonymous = z;
            return this;
        }

        public Builder setIncludeTxPower(boolean z) {
            this.mIncludeTxPower = z;
            return this;
        }

        public Builder setPrimaryPhy(int i) {
            if (i != 1 && i != 3) {
                throw new IllegalArgumentException("bad primaryPhy " + i);
            }
            this.mPrimaryPhy = i;
            return this;
        }

        public Builder setSecondaryPhy(int i) {
            if (i != 1 && i != 2 && i != 3) {
                throw new IllegalArgumentException("bad secondaryPhy " + i);
            }
            this.mSecondaryPhy = i;
            return this;
        }

        public Builder setInterval(int i) {
            if (i < 160 || i > 16777215) {
                throw new IllegalArgumentException("unknown interval " + i);
            }
            this.mInterval = i;
            return this;
        }

        public Builder setTxPowerLevel(int i) {
            if (i < -127 || i > 1) {
                throw new IllegalArgumentException("unknown txPowerLevel " + i);
            }
            this.mTxPowerLevel = i;
            return this;
        }

        public AdvertisingSetParameters build() {
            if (this.mIsLegacy) {
                if (this.mIsAnonymous) {
                    throw new IllegalArgumentException("Legacy advertising can't be anonymous");
                }
                if (this.mConnectable && !this.mScannable) {
                    throw new IllegalStateException("Legacy advertisement can't be connectable and non-scannable");
                }
                if (this.mIncludeTxPower) {
                    throw new IllegalStateException("Legacy advertising can't include TX power level in header");
                }
            } else {
                if (this.mConnectable && this.mScannable) {
                    throw new IllegalStateException("Advertising can't be both connectable and scannable");
                }
                if (this.mIsAnonymous && this.mConnectable) {
                    throw new IllegalStateException("Advertising can't be both connectable and anonymous");
                }
            }
            return new AdvertisingSetParameters(this.mConnectable, this.mScannable, this.mIsLegacy, this.mIsAnonymous, this.mIncludeTxPower, this.mPrimaryPhy, this.mSecondaryPhy, this.mInterval, this.mTxPowerLevel);
        }
    }
}
