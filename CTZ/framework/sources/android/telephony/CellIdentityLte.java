package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Objects;

public final class CellIdentityLte extends CellIdentity {
    private static final boolean DBG = false;
    private final int mBandwidth;
    private final int mCi;
    private final int mEarfcn;
    private final int mPci;
    private final int mTac;
    private static final String TAG = CellIdentityLte.class.getSimpleName();
    public static final Parcelable.Creator<CellIdentityLte> CREATOR = new Parcelable.Creator<CellIdentityLte>() {
        @Override
        public CellIdentityLte createFromParcel(Parcel parcel) {
            parcel.readInt();
            return CellIdentityLte.createFromParcelBody(parcel);
        }

        @Override
        public CellIdentityLte[] newArray(int i) {
            return new CellIdentityLte[i];
        }
    };

    public CellIdentityLte() {
        super(TAG, 3, null, null, null, null);
        this.mCi = Integer.MAX_VALUE;
        this.mPci = Integer.MAX_VALUE;
        this.mTac = Integer.MAX_VALUE;
        this.mEarfcn = Integer.MAX_VALUE;
        this.mBandwidth = Integer.MAX_VALUE;
    }

    public CellIdentityLte(int i, int i2, int i3, int i4, int i5) {
        this(i3, i4, i5, Integer.MAX_VALUE, Integer.MAX_VALUE, String.valueOf(i), String.valueOf(i2), null, null);
    }

    public CellIdentityLte(int i, int i2, int i3, int i4, int i5, int i6) {
        this(i3, i4, i5, i6, Integer.MAX_VALUE, String.valueOf(i), String.valueOf(i2), null, null);
    }

    public CellIdentityLte(int i, int i2, int i3, int i4, int i5, String str, String str2, String str3, String str4) {
        super(TAG, 3, str, str2, str3, str4);
        this.mCi = i;
        this.mPci = i2;
        this.mTac = i3;
        this.mEarfcn = i4;
        this.mBandwidth = i5;
    }

    private CellIdentityLte(CellIdentityLte cellIdentityLte) {
        this(cellIdentityLte.mCi, cellIdentityLte.mPci, cellIdentityLte.mTac, cellIdentityLte.mEarfcn, cellIdentityLte.mBandwidth, cellIdentityLte.mMccStr, cellIdentityLte.mMncStr, cellIdentityLte.mAlphaLong, cellIdentityLte.mAlphaShort);
    }

    CellIdentityLte copy() {
        return new CellIdentityLte(this);
    }

    @Deprecated
    public int getMcc() {
        if (this.mMccStr != null) {
            return Integer.valueOf(this.mMccStr).intValue();
        }
        return Integer.MAX_VALUE;
    }

    @Deprecated
    public int getMnc() {
        if (this.mMncStr != null) {
            return Integer.valueOf(this.mMncStr).intValue();
        }
        return Integer.MAX_VALUE;
    }

    public int getCi() {
        return this.mCi;
    }

    public int getPci() {
        return this.mPci;
    }

    public int getTac() {
        return this.mTac;
    }

    public int getEarfcn() {
        return this.mEarfcn;
    }

    public int getBandwidth() {
        return this.mBandwidth;
    }

    public String getMccString() {
        return this.mMccStr;
    }

    public String getMncString() {
        return this.mMncStr;
    }

    public String getMobileNetworkOperator() {
        if (this.mMccStr == null || this.mMncStr == null) {
            return null;
        }
        return this.mMccStr + this.mMncStr;
    }

    @Override
    public int getChannelNumber() {
        return this.mEarfcn;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mCi), Integer.valueOf(this.mPci), Integer.valueOf(this.mTac), Integer.valueOf(super.hashCode()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CellIdentityLte)) {
            return false;
        }
        CellIdentityLte cellIdentityLte = (CellIdentityLte) obj;
        return this.mCi == cellIdentityLte.mCi && this.mPci == cellIdentityLte.mPci && this.mTac == cellIdentityLte.mTac && this.mEarfcn == cellIdentityLte.mEarfcn && this.mBandwidth == cellIdentityLte.mBandwidth && TextUtils.equals(this.mMccStr, cellIdentityLte.mMccStr) && TextUtils.equals(this.mMncStr, cellIdentityLte.mMncStr) && super.equals(obj);
    }

    public String toString() {
        return TAG + ":{ mCi=" + this.mCi + " mPci=" + this.mPci + " mTac=" + this.mTac + " mEarfcn=" + this.mEarfcn + " mBandwidth=" + this.mBandwidth + " mMcc=" + this.mMccStr + " mMnc=" + this.mMncStr + " mAlphaLong=" + this.mAlphaLong + " mAlphaShort=" + this.mAlphaShort + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, 3);
        parcel.writeInt(this.mCi);
        parcel.writeInt(this.mPci);
        parcel.writeInt(this.mTac);
        parcel.writeInt(this.mEarfcn);
        parcel.writeInt(this.mBandwidth);
    }

    private CellIdentityLte(Parcel parcel) {
        super(TAG, 3, parcel);
        this.mCi = parcel.readInt();
        this.mPci = parcel.readInt();
        this.mTac = parcel.readInt();
        this.mEarfcn = parcel.readInt();
        this.mBandwidth = parcel.readInt();
    }

    protected static CellIdentityLte createFromParcelBody(Parcel parcel) {
        return new CellIdentityLte(parcel);
    }
}
