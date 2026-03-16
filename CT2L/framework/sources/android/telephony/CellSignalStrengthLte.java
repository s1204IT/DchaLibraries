package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public final class CellSignalStrengthLte extends CellSignalStrength implements Parcelable {
    public static final Parcelable.Creator<CellSignalStrengthLte> CREATOR = new Parcelable.Creator<CellSignalStrengthLte>() {
        @Override
        public CellSignalStrengthLte createFromParcel(Parcel in) {
            return new CellSignalStrengthLte(in);
        }

        @Override
        public CellSignalStrengthLte[] newArray(int size) {
            return new CellSignalStrengthLte[size];
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

    public CellSignalStrengthLte(int signalStrength, int rsrp, int rsrq, int rssnr, int cqi, int timingAdvance) {
        initialize(signalStrength, rsrp, rsrq, rssnr, cqi, timingAdvance);
    }

    public CellSignalStrengthLte(CellSignalStrengthLte s) {
        copyFrom(s);
    }

    public void initialize(int lteSignalStrength, int rsrp, int rsrq, int rssnr, int cqi, int timingAdvance) {
        this.mSignalStrength = lteSignalStrength;
        this.mRsrp = rsrp;
        this.mRsrq = rsrq;
        this.mRssnr = rssnr;
        this.mCqi = cqi;
        this.mTimingAdvance = timingAdvance;
    }

    public void initialize(SignalStrength ss, int timingAdvance) {
        this.mSignalStrength = ss.getLteSignalStrength();
        this.mRsrp = ss.getLteRsrp();
        this.mRsrq = ss.getLteRsrq();
        this.mRssnr = ss.getLteRssnr();
        this.mCqi = ss.getLteCqi();
        this.mTimingAdvance = timingAdvance;
    }

    protected void copyFrom(CellSignalStrengthLte s) {
        this.mSignalStrength = s.mSignalStrength;
        this.mRsrp = s.mRsrp;
        this.mRsrq = s.mRsrq;
        this.mRssnr = s.mRssnr;
        this.mCqi = s.mCqi;
        this.mTimingAdvance = s.mTimingAdvance;
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
        int levelRsrp;
        int levelRssnr;
        if (this.mRsrp == Integer.MAX_VALUE) {
            levelRsrp = 0;
        } else if (this.mRsrp >= -95) {
            levelRsrp = 4;
        } else if (this.mRsrp >= -105) {
            levelRsrp = 3;
        } else {
            levelRsrp = this.mRsrp >= -115 ? 2 : 1;
        }
        if (this.mRssnr == Integer.MAX_VALUE) {
            levelRssnr = 0;
        } else if (this.mRssnr >= 45) {
            levelRssnr = 4;
        } else if (this.mRssnr >= 10) {
            levelRssnr = 3;
        } else {
            levelRssnr = this.mRssnr >= -30 ? 2 : 1;
        }
        if (this.mRsrp == Integer.MAX_VALUE) {
            int level = levelRssnr;
            return level;
        }
        if (this.mRssnr == Integer.MAX_VALUE) {
            int level2 = levelRsrp;
            return level2;
        }
        if (levelRssnr < levelRsrp) {
            int level3 = levelRssnr;
            return level3;
        }
        int level4 = levelRsrp;
        return level4;
    }

    @Override
    public int getDbm() {
        return this.mRsrp;
    }

    @Override
    public int getAsuLevel() {
        int lteDbm = getDbm();
        if (lteDbm <= -140) {
            return 0;
        }
        if (lteDbm >= -43) {
            return 97;
        }
        int lteAsuLevel = lteDbm + 140;
        return lteAsuLevel;
    }

    public int getTimingAdvance() {
        return this.mTimingAdvance;
    }

    @Override
    public int hashCode() {
        return (this.mSignalStrength * 31) + (this.mRsrp * 31) + (this.mRsrq * 31) + (this.mRssnr * 31) + (this.mCqi * 31) + (this.mTimingAdvance * 31);
    }

    @Override
    public boolean equals(Object o) {
        try {
            CellSignalStrengthLte s = (CellSignalStrengthLte) o;
            return o != null && this.mSignalStrength == s.mSignalStrength && this.mRsrp == s.mRsrp && this.mRsrq == s.mRsrq && this.mRssnr == s.mRssnr && this.mCqi == s.mCqi && this.mTimingAdvance == s.mTimingAdvance;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        return "CellSignalStrengthLte: ss=" + this.mSignalStrength + " rsrp=" + this.mRsrp + " rsrq=" + this.mRsrq + " rssnr=" + this.mRssnr + " cqi=" + this.mCqi + " ta=" + this.mTimingAdvance;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mSignalStrength);
        dest.writeInt(this.mRsrp * (-1));
        dest.writeInt(this.mRsrq * (-1));
        dest.writeInt(this.mRssnr);
        dest.writeInt(this.mCqi);
        dest.writeInt(this.mTimingAdvance);
    }

    private CellSignalStrengthLte(Parcel in) {
        this.mSignalStrength = in.readInt();
        this.mRsrp = in.readInt() * (-1);
        this.mRsrq = in.readInt() * (-1);
        this.mRssnr = in.readInt();
        this.mCqi = in.readInt();
        this.mTimingAdvance = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static void log(String s) {
        Rlog.w(LOG_TAG, s);
    }
}
