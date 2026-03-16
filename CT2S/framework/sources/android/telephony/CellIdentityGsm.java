package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public final class CellIdentityGsm implements Parcelable {
    public static final Parcelable.Creator<CellIdentityGsm> CREATOR = new Parcelable.Creator<CellIdentityGsm>() {
        @Override
        public CellIdentityGsm createFromParcel(Parcel in) {
            return new CellIdentityGsm(in);
        }

        @Override
        public CellIdentityGsm[] newArray(int size) {
            return new CellIdentityGsm[size];
        }
    };
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CellIdentityGsm";
    private final int mCid;
    private final int mLac;
    private final int mMcc;
    private final int mMnc;

    public CellIdentityGsm() {
        this.mMcc = Integer.MAX_VALUE;
        this.mMnc = Integer.MAX_VALUE;
        this.mLac = Integer.MAX_VALUE;
        this.mCid = Integer.MAX_VALUE;
    }

    public CellIdentityGsm(int mcc, int mnc, int lac, int cid) {
        this.mMcc = mcc;
        this.mMnc = mnc;
        this.mLac = lac;
        this.mCid = cid;
    }

    private CellIdentityGsm(CellIdentityGsm cid) {
        this.mMcc = cid.mMcc;
        this.mMnc = cid.mMnc;
        this.mLac = cid.mLac;
        this.mCid = cid.mCid;
    }

    CellIdentityGsm copy() {
        return new CellIdentityGsm(this);
    }

    public int getMcc() {
        return this.mMcc;
    }

    public int getMnc() {
        return this.mMnc;
    }

    public int getLac() {
        return this.mLac;
    }

    public int getCid() {
        return this.mCid;
    }

    @Deprecated
    public int getPsc() {
        return Integer.MAX_VALUE;
    }

    public int hashCode() {
        return (this.mMcc * 31) + (this.mMnc * 31) + (this.mLac * 31) + (this.mCid * 31);
    }

    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }
        try {
            CellIdentityGsm o = (CellIdentityGsm) other;
            if (this.mMcc == o.mMcc && this.mMnc == o.mMnc && this.mLac == o.mLac) {
                return this.mCid == o.mCid;
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("CellIdentityGsm:{");
        sb.append(" mMcc=").append(this.mMcc);
        sb.append(" mMnc=").append(this.mMnc);
        sb.append(" mLac=").append(this.mLac);
        sb.append(" mCid=").append(this.mCid);
        sb.append("}");
        return sb.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mMcc);
        dest.writeInt(this.mMnc);
        dest.writeInt(this.mLac);
        dest.writeInt(this.mCid);
    }

    private CellIdentityGsm(Parcel in) {
        this.mMcc = in.readInt();
        this.mMnc = in.readInt();
        this.mLac = in.readInt();
        this.mCid = in.readInt();
    }

    private static void log(String s) {
        Rlog.w(LOG_TAG, s);
    }
}
