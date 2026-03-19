package android.telephony;

import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class CellSignalStrengthWcdma extends CellSignalStrength implements Parcelable {
    public static final Parcelable.Creator<CellSignalStrengthWcdma> CREATOR = new Parcelable.Creator<CellSignalStrengthWcdma>() {
        @Override
        public CellSignalStrengthWcdma createFromParcel(Parcel parcel) {
            return new CellSignalStrengthWcdma(parcel);
        }

        @Override
        public CellSignalStrengthWcdma[] newArray(int i) {
            return new CellSignalStrengthWcdma[i];
        }
    };
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CellSignalStrengthWcdma";
    private static final int WCDMA_SIGNAL_STRENGTH_GOOD = 8;
    private static final int WCDMA_SIGNAL_STRENGTH_GREAT = 12;
    private static final int WCDMA_SIGNAL_STRENGTH_MODERATE = 5;
    private int mBitErrorRate;
    private int mSignalStrength;

    public CellSignalStrengthWcdma() {
        setDefaultValues();
    }

    public CellSignalStrengthWcdma(int i, int i2) {
        this.mSignalStrength = i;
        this.mBitErrorRate = i2;
    }

    public CellSignalStrengthWcdma(CellSignalStrengthWcdma cellSignalStrengthWcdma) {
        copyFrom(cellSignalStrengthWcdma);
    }

    protected void copyFrom(CellSignalStrengthWcdma cellSignalStrengthWcdma) {
        this.mSignalStrength = cellSignalStrengthWcdma.mSignalStrength;
        this.mBitErrorRate = cellSignalStrengthWcdma.mBitErrorRate;
    }

    @Override
    public CellSignalStrengthWcdma copy() {
        return new CellSignalStrengthWcdma(this);
    }

    @Override
    public void setDefaultValues() {
        this.mSignalStrength = Integer.MAX_VALUE;
        this.mBitErrorRate = Integer.MAX_VALUE;
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
        return Objects.hash(Integer.valueOf(this.mSignalStrength), Integer.valueOf(this.mBitErrorRate));
    }

    @Override
    public boolean equals(Object obj) {
        try {
            CellSignalStrengthWcdma cellSignalStrengthWcdma = (CellSignalStrengthWcdma) obj;
            return obj != null && this.mSignalStrength == cellSignalStrengthWcdma.mSignalStrength && this.mBitErrorRate == cellSignalStrengthWcdma.mBitErrorRate;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        return "CellSignalStrengthWcdma: ss=" + this.mSignalStrength + " ber=" + this.mBitErrorRate;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSignalStrength);
        parcel.writeInt(this.mBitErrorRate);
    }

    private CellSignalStrengthWcdma(Parcel parcel) {
        this.mSignalStrength = parcel.readInt();
        this.mBitErrorRate = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static void log(String str) {
        Rlog.w(LOG_TAG, str);
    }
}
