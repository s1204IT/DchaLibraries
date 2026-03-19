package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class CellSignalStrengthLte extends CellSignalStrength implements Parcelable {
    public static final Parcelable.Creator<CellSignalStrengthLte> CREATOR = new Parcelable.Creator<CellSignalStrengthLte>() {
        @Override
        public CellSignalStrengthLte createFromParcel(Parcel parcel) {
            return new CellSignalStrengthLte(parcel);
        }

        @Override
        public CellSignalStrengthLte[] newArray(int i) {
            return new CellSignalStrengthLte[i];
        }
    };
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CellSignalStrengthLte";
    private int mCqi;
    private int mRsrp;
    private int mRsrq;
    private int mRssnr;
    private int mSignalStrength;
    private int mTimingAdvance;

    public CellSignalStrengthLte() {
        setDefaultValues();
    }

    public CellSignalStrengthLte(int i, int i2, int i3, int i4, int i5, int i6) {
        this.mSignalStrength = i;
        this.mRsrp = i2;
        this.mRsrq = i3;
        this.mRssnr = i4;
        this.mCqi = i5;
        this.mTimingAdvance = i6;
    }

    public CellSignalStrengthLte(CellSignalStrengthLte cellSignalStrengthLte) {
        copyFrom(cellSignalStrengthLte);
    }

    protected void copyFrom(CellSignalStrengthLte cellSignalStrengthLte) {
        this.mSignalStrength = cellSignalStrengthLte.mSignalStrength;
        this.mRsrp = cellSignalStrengthLte.mRsrp;
        this.mRsrq = cellSignalStrengthLte.mRsrq;
        this.mRssnr = cellSignalStrengthLte.mRssnr;
        this.mCqi = cellSignalStrengthLte.mCqi;
        this.mTimingAdvance = cellSignalStrengthLte.mTimingAdvance;
    }

    @Override
    public CellSignalStrengthLte copy() {
        return new CellSignalStrengthLte(this);
    }

    @Override
    public void setDefaultValues() {
        this.mSignalStrength = Integer.MAX_VALUE;
        this.mRsrp = Integer.MAX_VALUE;
        this.mRsrq = Integer.MAX_VALUE;
        this.mRssnr = Integer.MAX_VALUE;
        this.mCqi = Integer.MAX_VALUE;
        this.mTimingAdvance = Integer.MAX_VALUE;
    }

    @Override
    public int getLevel() {
        int i = 1;
        int i2 = this.mRsrp == Integer.MAX_VALUE ? 0 : this.mRsrp >= -95 ? 4 : this.mRsrp >= -105 ? 3 : this.mRsrp >= -115 ? 2 : 1;
        if (this.mRssnr == Integer.MAX_VALUE) {
            i = 0;
        } else if (this.mRssnr >= 45) {
            i = 4;
        } else if (this.mRssnr >= 10) {
            i = 3;
        } else if (this.mRssnr >= -30) {
            i = 2;
        }
        return (this.mRsrp != Integer.MAX_VALUE && (this.mRssnr == Integer.MAX_VALUE || i >= i2)) ? i2 : i;
    }

    public int getRsrq() {
        return this.mRsrq;
    }

    public int getRssnr() {
        return this.mRssnr;
    }

    public int getRsrp() {
        return this.mRsrp;
    }

    public int getCqi() {
        return this.mCqi;
    }

    @Override
    public int getDbm() {
        return this.mRsrp;
    }

    @Override
    public int getAsuLevel() {
        int dbm = getDbm();
        if (dbm == Integer.MAX_VALUE) {
            return 99;
        }
        if (dbm <= -140) {
            return 0;
        }
        if (dbm >= -43) {
            return 97;
        }
        return dbm + 140;
    }

    public int getTimingAdvance() {
        return this.mTimingAdvance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mSignalStrength), Integer.valueOf(this.mRsrp), Integer.valueOf(this.mRsrq), Integer.valueOf(this.mRssnr), Integer.valueOf(this.mCqi), Integer.valueOf(this.mTimingAdvance));
    }

    @Override
    public boolean equals(Object obj) {
        try {
            CellSignalStrengthLte cellSignalStrengthLte = (CellSignalStrengthLte) obj;
            return obj != null && this.mSignalStrength == cellSignalStrengthLte.mSignalStrength && this.mRsrp == cellSignalStrengthLte.mRsrp && this.mRsrq == cellSignalStrengthLte.mRsrq && this.mRssnr == cellSignalStrengthLte.mRssnr && this.mCqi == cellSignalStrengthLte.mCqi && this.mTimingAdvance == cellSignalStrengthLte.mTimingAdvance;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        return "CellSignalStrengthLte: ss=" + this.mSignalStrength + " rsrp=" + this.mRsrp + " rsrq=" + this.mRsrq + " rssnr=" + this.mRssnr + " cqi=" + this.mCqi + " ta=" + this.mTimingAdvance;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSignalStrength);
        parcel.writeInt(this.mRsrp * (this.mRsrp != Integer.MAX_VALUE ? -1 : 1));
        parcel.writeInt(this.mRsrq * (this.mRsrq != Integer.MAX_VALUE ? -1 : 1));
        parcel.writeInt(this.mRssnr);
        parcel.writeInt(this.mCqi);
        parcel.writeInt(this.mTimingAdvance);
    }

    private CellSignalStrengthLte(Parcel parcel) {
        this.mSignalStrength = parcel.readInt();
        this.mRsrp = parcel.readInt();
        if (this.mRsrp != Integer.MAX_VALUE) {
            this.mRsrp *= -1;
        }
        this.mRsrq = parcel.readInt();
        if (this.mRsrq != Integer.MAX_VALUE) {
            this.mRsrq *= -1;
        }
        this.mRssnr = parcel.readInt();
        this.mCqi = parcel.readInt();
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
