package android.telephony;

import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class CellSignalStrengthGsm extends CellSignalStrength implements Parcelable {
    public static final Parcelable.Creator<CellSignalStrengthGsm> CREATOR = new Parcelable.Creator<CellSignalStrengthGsm>() {
        @Override
        public CellSignalStrengthGsm createFromParcel(Parcel parcel) {
            return new CellSignalStrengthGsm(parcel);
        }

        @Override
        public CellSignalStrengthGsm[] newArray(int i) {
            return new CellSignalStrengthGsm[i];
        }
    };
    private static final boolean DBG = false;
    private static final int GSM_SIGNAL_STRENGTH_GOOD = 8;
    private static final int GSM_SIGNAL_STRENGTH_GREAT = 12;
    private static final int GSM_SIGNAL_STRENGTH_MODERATE = 5;
    private static final String LOG_TAG = "CellSignalStrengthGsm";
    private int mBitErrorRate;
    private int mSignalStrength;
    private int mTimingAdvance;

    public CellSignalStrengthGsm() {
        setDefaultValues();
    }

    public CellSignalStrengthGsm(int i, int i2) {
        this(i, i2, Integer.MAX_VALUE);
    }

    public CellSignalStrengthGsm(int i, int i2, int i3) {
        this.mSignalStrength = i;
        this.mBitErrorRate = i2;
        this.mTimingAdvance = i3;
    }

    public CellSignalStrengthGsm(CellSignalStrengthGsm cellSignalStrengthGsm) {
        copyFrom(cellSignalStrengthGsm);
    }

    protected void copyFrom(CellSignalStrengthGsm cellSignalStrengthGsm) {
        this.mSignalStrength = cellSignalStrengthGsm.mSignalStrength;
        this.mBitErrorRate = cellSignalStrengthGsm.mBitErrorRate;
        this.mTimingAdvance = cellSignalStrengthGsm.mTimingAdvance;
    }

    @Override
    public CellSignalStrengthGsm copy() {
        return new CellSignalStrengthGsm(this);
    }

    @Override
    public void setDefaultValues() {
        this.mSignalStrength = Integer.MAX_VALUE;
        this.mBitErrorRate = Integer.MAX_VALUE;
        this.mTimingAdvance = Integer.MAX_VALUE;
    }

    @Override
    public int getLevel() {
        int i = this.mSignalStrength;
        if (i <= 2 || i == 99) {
            return 0;
        }
        if (i >= 12) {
            return 4;
        }
        if (i >= 8) {
            return 3;
        }
        if (i >= 5) {
            return 2;
        }
        return 1;
    }

    public int getTimingAdvance() {
        return this.mTimingAdvance;
    }

    @Override
    public int getDbm() {
        int i = this.mSignalStrength;
        if (i == 99) {
            i = Integer.MAX_VALUE;
        }
        if (i == Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return PackageManager.INSTALL_FAILED_NO_MATCHING_ABIS + (2 * i);
    }

    @Override
    public int getAsuLevel() {
        return this.mSignalStrength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mSignalStrength), Integer.valueOf(this.mBitErrorRate), Integer.valueOf(this.mTimingAdvance));
    }

    @Override
    public boolean equals(Object obj) {
        try {
            CellSignalStrengthGsm cellSignalStrengthGsm = (CellSignalStrengthGsm) obj;
            return obj != null && this.mSignalStrength == cellSignalStrengthGsm.mSignalStrength && this.mBitErrorRate == cellSignalStrengthGsm.mBitErrorRate && cellSignalStrengthGsm.mTimingAdvance == this.mTimingAdvance;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        return "CellSignalStrengthGsm: ss=" + this.mSignalStrength + " ber=" + this.mBitErrorRate + " mTa=" + this.mTimingAdvance;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSignalStrength);
        parcel.writeInt(this.mBitErrorRate);
        parcel.writeInt(this.mTimingAdvance);
    }

    private CellSignalStrengthGsm(Parcel parcel) {
        this.mSignalStrength = parcel.readInt();
        this.mBitErrorRate = parcel.readInt();
        this.mTimingAdvance = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static void log(String str) {
        Rlog.w(LOG_TAG, str);
    }
}
