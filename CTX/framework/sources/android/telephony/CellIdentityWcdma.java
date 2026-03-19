package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Objects;

public final class CellIdentityWcdma extends CellIdentity {
    private static final boolean DBG = false;
    private final int mCid;
    private final int mLac;
    private final int mPsc;
    private final int mUarfcn;
    private static final String TAG = CellIdentityWcdma.class.getSimpleName();
    public static final Parcelable.Creator<CellIdentityWcdma> CREATOR = new Parcelable.Creator<CellIdentityWcdma>() {
        @Override
        public CellIdentityWcdma createFromParcel(Parcel parcel) {
            parcel.readInt();
            return CellIdentityWcdma.createFromParcelBody(parcel);
        }

        @Override
        public CellIdentityWcdma[] newArray(int i) {
            return new CellIdentityWcdma[i];
        }
    };

    public CellIdentityWcdma() {
        super(TAG, 5, null, null, null, null);
        this.mLac = Integer.MAX_VALUE;
        this.mCid = Integer.MAX_VALUE;
        this.mPsc = Integer.MAX_VALUE;
        this.mUarfcn = Integer.MAX_VALUE;
    }

    public CellIdentityWcdma(int i, int i2, int i3, int i4, int i5) {
        this(i3, i4, i5, Integer.MAX_VALUE, String.valueOf(i), String.valueOf(i2), null, null);
    }

    public CellIdentityWcdma(int i, int i2, int i3, int i4, int i5, int i6) {
        this(i3, i4, i5, i6, String.valueOf(i), String.valueOf(i2), null, null);
    }

    public CellIdentityWcdma(int i, int i2, int i3, int i4, String str, String str2, String str3, String str4) {
        super(TAG, 4, str, str2, str3, str4);
        this.mLac = i;
        this.mCid = i2;
        this.mPsc = i3;
        this.mUarfcn = i4;
    }

    private CellIdentityWcdma(CellIdentityWcdma cellIdentityWcdma) {
        this(cellIdentityWcdma.mLac, cellIdentityWcdma.mCid, cellIdentityWcdma.mPsc, cellIdentityWcdma.mUarfcn, cellIdentityWcdma.mMccStr, cellIdentityWcdma.mMncStr, cellIdentityWcdma.mAlphaLong, cellIdentityWcdma.mAlphaShort);
    }

    CellIdentityWcdma copy() {
        return new CellIdentityWcdma(this);
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

    public int getLac() {
        return this.mLac;
    }

    public int getCid() {
        return this.mCid;
    }

    public int getPsc() {
        return this.mPsc;
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
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mLac), Integer.valueOf(this.mCid), Integer.valueOf(this.mPsc), Integer.valueOf(super.hashCode()));
    }

    public int getUarfcn() {
        return this.mUarfcn;
    }

    @Override
    public int getChannelNumber() {
        return this.mUarfcn;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CellIdentityWcdma)) {
            return false;
        }
        CellIdentityWcdma cellIdentityWcdma = (CellIdentityWcdma) obj;
        return this.mLac == cellIdentityWcdma.mLac && this.mCid == cellIdentityWcdma.mCid && this.mPsc == cellIdentityWcdma.mPsc && this.mUarfcn == cellIdentityWcdma.mUarfcn && TextUtils.equals(this.mMccStr, cellIdentityWcdma.mMccStr) && TextUtils.equals(this.mMncStr, cellIdentityWcdma.mMncStr) && super.equals(obj);
    }

    public String toString() {
        return TAG + ":{ mLac=" + this.mLac + " mCid=" + this.mCid + " mPsc=" + this.mPsc + " mUarfcn=" + this.mUarfcn + " mMcc=" + this.mMccStr + " mMnc=" + this.mMncStr + " mAlphaLong=" + this.mAlphaLong + " mAlphaShort=" + this.mAlphaShort + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, 4);
        parcel.writeInt(this.mLac);
        parcel.writeInt(this.mCid);
        parcel.writeInt(this.mPsc);
        parcel.writeInt(this.mUarfcn);
    }

    private CellIdentityWcdma(Parcel parcel) {
        super(TAG, 4, parcel);
        this.mLac = parcel.readInt();
        this.mCid = parcel.readInt();
        this.mPsc = parcel.readInt();
        this.mUarfcn = parcel.readInt();
    }

    protected static CellIdentityWcdma createFromParcelBody(Parcel parcel) {
        return new CellIdentityWcdma(parcel);
    }
}
