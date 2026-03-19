package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class CellSignalStrengthCdma extends CellSignalStrength implements Parcelable {
    public static final Parcelable.Creator<CellSignalStrengthCdma> CREATOR = new Parcelable.Creator<CellSignalStrengthCdma>() {
        @Override
        public CellSignalStrengthCdma createFromParcel(Parcel parcel) {
            return new CellSignalStrengthCdma(parcel);
        }

        @Override
        public CellSignalStrengthCdma[] newArray(int i) {
            return new CellSignalStrengthCdma[i];
        }
    };
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CellSignalStrengthCdma";
    private int mCdmaDbm;
    private int mCdmaEcio;
    private int mEvdoDbm;
    private int mEvdoEcio;
    private int mEvdoSnr;

    public CellSignalStrengthCdma() {
        setDefaultValues();
    }

    public CellSignalStrengthCdma(int i, int i2, int i3, int i4, int i5) {
        this.mCdmaDbm = (i <= 0 || i >= 120) ? Integer.MAX_VALUE : -i;
        this.mCdmaEcio = (i2 <= 0 || i2 >= 160) ? Integer.MAX_VALUE : -i2;
        this.mEvdoDbm = (i3 <= 0 || i3 >= 120) ? Integer.MAX_VALUE : -i3;
        this.mEvdoEcio = (i4 <= 0 || i4 >= 160) ? Integer.MAX_VALUE : -i4;
        this.mEvdoSnr = (i5 <= 0 || i5 > 8) ? Integer.MAX_VALUE : i5;
    }

    public CellSignalStrengthCdma(CellSignalStrengthCdma cellSignalStrengthCdma) {
        copyFrom(cellSignalStrengthCdma);
    }

    protected void copyFrom(CellSignalStrengthCdma cellSignalStrengthCdma) {
        this.mCdmaDbm = cellSignalStrengthCdma.mCdmaDbm;
        this.mCdmaEcio = cellSignalStrengthCdma.mCdmaEcio;
        this.mEvdoDbm = cellSignalStrengthCdma.mEvdoDbm;
        this.mEvdoEcio = cellSignalStrengthCdma.mEvdoEcio;
        this.mEvdoSnr = cellSignalStrengthCdma.mEvdoSnr;
    }

    @Override
    public CellSignalStrengthCdma copy() {
        return new CellSignalStrengthCdma(this);
    }

    @Override
    public void setDefaultValues() {
        this.mCdmaDbm = Integer.MAX_VALUE;
        this.mCdmaEcio = Integer.MAX_VALUE;
        this.mEvdoDbm = Integer.MAX_VALUE;
        this.mEvdoEcio = Integer.MAX_VALUE;
        this.mEvdoSnr = Integer.MAX_VALUE;
    }

    @Override
    public int getLevel() {
        int cdmaLevel = getCdmaLevel();
        int evdoLevel = getEvdoLevel();
        if (evdoLevel == 0) {
            return getCdmaLevel();
        }
        if (cdmaLevel == 0) {
            return getEvdoLevel();
        }
        return cdmaLevel < evdoLevel ? cdmaLevel : evdoLevel;
    }

    @Override
    public int getAsuLevel() {
        int i;
        int cdmaDbm = getCdmaDbm();
        int cdmaEcio = getCdmaEcio();
        int i2 = 99;
        if (cdmaDbm != Integer.MAX_VALUE) {
            i = cdmaDbm >= -75 ? 16 : cdmaDbm >= -82 ? 8 : cdmaDbm >= -90 ? 4 : cdmaDbm >= -95 ? 2 : cdmaDbm >= -100 ? 1 : 99;
        }
        if (cdmaEcio != Integer.MAX_VALUE) {
            if (cdmaEcio >= -90) {
                i2 = 16;
            } else if (cdmaEcio >= -100) {
                i2 = 8;
            } else if (cdmaEcio >= -115) {
                i2 = 4;
            } else if (cdmaEcio >= -130) {
                i2 = 2;
            } else if (cdmaEcio >= -150) {
                i2 = 1;
            }
        }
        return i < i2 ? i : i2;
    }

    public int getCdmaLevel() {
        int i;
        int cdmaDbm = getCdmaDbm();
        int cdmaEcio = getCdmaEcio();
        int i2 = 0;
        if (cdmaDbm != Integer.MAX_VALUE) {
            i = cdmaDbm >= -75 ? 4 : cdmaDbm >= -85 ? 3 : cdmaDbm >= -95 ? 2 : cdmaDbm >= -100 ? 1 : 0;
        }
        if (cdmaEcio != Integer.MAX_VALUE) {
            if (cdmaEcio >= -90) {
                i2 = 4;
            } else if (cdmaEcio >= -110) {
                i2 = 3;
            } else if (cdmaEcio >= -130) {
                i2 = 2;
            } else if (cdmaEcio >= -150) {
                i2 = 1;
            }
        }
        return i < i2 ? i : i2;
    }

    public int getEvdoLevel() {
        int i;
        int evdoDbm = getEvdoDbm();
        int evdoSnr = getEvdoSnr();
        int i2 = 0;
        if (evdoDbm != Integer.MAX_VALUE) {
            i = evdoDbm >= -65 ? 4 : evdoDbm >= -75 ? 3 : evdoDbm >= -90 ? 2 : evdoDbm >= -105 ? 1 : 0;
        }
        if (evdoSnr != Integer.MAX_VALUE) {
            if (evdoSnr >= 7) {
                i2 = 4;
            } else if (evdoSnr >= 5) {
                i2 = 3;
            } else if (evdoSnr >= 3) {
                i2 = 2;
            } else if (evdoSnr >= 1) {
                i2 = 1;
            }
        }
        return i < i2 ? i : i2;
    }

    @Override
    public int getDbm() {
        int cdmaDbm = getCdmaDbm();
        int evdoDbm = getEvdoDbm();
        return cdmaDbm < evdoDbm ? cdmaDbm : evdoDbm;
    }

    public int getCdmaDbm() {
        return this.mCdmaDbm;
    }

    public void setCdmaDbm(int i) {
        this.mCdmaDbm = i;
    }

    public int getCdmaEcio() {
        return this.mCdmaEcio;
    }

    public void setCdmaEcio(int i) {
        this.mCdmaEcio = i;
    }

    public int getEvdoDbm() {
        return this.mEvdoDbm;
    }

    public void setEvdoDbm(int i) {
        this.mEvdoDbm = i;
    }

    public int getEvdoEcio() {
        return this.mEvdoEcio;
    }

    public void setEvdoEcio(int i) {
        this.mEvdoEcio = i;
    }

    public int getEvdoSnr() {
        return this.mEvdoSnr;
    }

    public void setEvdoSnr(int i) {
        this.mEvdoSnr = i;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mCdmaDbm), Integer.valueOf(this.mCdmaEcio), Integer.valueOf(this.mEvdoDbm), Integer.valueOf(this.mEvdoEcio), Integer.valueOf(this.mEvdoSnr));
    }

    @Override
    public boolean equals(Object obj) {
        try {
            CellSignalStrengthCdma cellSignalStrengthCdma = (CellSignalStrengthCdma) obj;
            return obj != null && this.mCdmaDbm == cellSignalStrengthCdma.mCdmaDbm && this.mCdmaEcio == cellSignalStrengthCdma.mCdmaEcio && this.mEvdoDbm == cellSignalStrengthCdma.mEvdoDbm && this.mEvdoEcio == cellSignalStrengthCdma.mEvdoEcio && this.mEvdoSnr == cellSignalStrengthCdma.mEvdoSnr;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        return "CellSignalStrengthCdma: cdmaDbm=" + this.mCdmaDbm + " cdmaEcio=" + this.mCdmaEcio + " evdoDbm=" + this.mEvdoDbm + " evdoEcio=" + this.mEvdoEcio + " evdoSnr=" + this.mEvdoSnr;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mCdmaDbm);
        parcel.writeInt(this.mCdmaEcio);
        parcel.writeInt(this.mEvdoDbm);
        parcel.writeInt(this.mEvdoEcio);
        parcel.writeInt(this.mEvdoSnr);
    }

    private CellSignalStrengthCdma(Parcel parcel) {
        this.mCdmaDbm = parcel.readInt();
        this.mCdmaEcio = parcel.readInt();
        this.mEvdoDbm = parcel.readInt();
        this.mEvdoEcio = parcel.readInt();
        this.mEvdoSnr = parcel.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static void log(String str) {
        Rlog.w(LOG_TAG, str);
    }
}
