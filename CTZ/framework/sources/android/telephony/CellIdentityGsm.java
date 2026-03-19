package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Objects;

public final class CellIdentityGsm extends CellIdentity {
    private static final boolean DBG = false;
    private final int mArfcn;
    private final int mBsic;
    private final int mCid;
    private final int mLac;
    private static final String TAG = CellIdentityGsm.class.getSimpleName();
    public static final Parcelable.Creator<CellIdentityGsm> CREATOR = new Parcelable.Creator<CellIdentityGsm>() {
        @Override
        public CellIdentityGsm createFromParcel(Parcel parcel) {
            parcel.readInt();
            return CellIdentityGsm.createFromParcelBody(parcel);
        }

        @Override
        public CellIdentityGsm[] newArray(int i) {
            return new CellIdentityGsm[i];
        }
    };

    public CellIdentityGsm() {
        super(TAG, 1, null, null, null, null);
        this.mLac = Integer.MAX_VALUE;
        this.mCid = Integer.MAX_VALUE;
        this.mArfcn = Integer.MAX_VALUE;
        this.mBsic = Integer.MAX_VALUE;
    }

    public CellIdentityGsm(int i, int i2, int i3, int i4) {
        this(i3, i4, Integer.MAX_VALUE, Integer.MAX_VALUE, String.valueOf(i), String.valueOf(i2), null, null);
    }

    public CellIdentityGsm(int i, int i2, int i3, int i4, int i5, int i6) {
        this(i3, i4, i5, i6, String.valueOf(i), String.valueOf(i2), null, null);
    }

    public CellIdentityGsm(int i, int i2, int i3, int i4, String str, String str2, String str3, String str4) {
        super(TAG, 1, str, str2, str3, str4);
        this.mLac = i;
        this.mCid = i2;
        this.mArfcn = i3;
        this.mBsic = i4 == 255 ? Integer.MAX_VALUE : i4;
    }

    private CellIdentityGsm(CellIdentityGsm cellIdentityGsm) {
        this(cellIdentityGsm.mLac, cellIdentityGsm.mCid, cellIdentityGsm.mArfcn, cellIdentityGsm.mBsic, cellIdentityGsm.mMccStr, cellIdentityGsm.mMncStr, cellIdentityGsm.mAlphaLong, cellIdentityGsm.mAlphaShort);
    }

    CellIdentityGsm copy() {
        return new CellIdentityGsm(this);
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

    public int getArfcn() {
        return this.mArfcn;
    }

    public int getBsic() {
        return this.mBsic;
    }

    public String getMobileNetworkOperator() {
        if (this.mMccStr == null || this.mMncStr == null) {
            return null;
        }
        return this.mMccStr + this.mMncStr;
    }

    public String getMccString() {
        return this.mMccStr;
    }

    public String getMncString() {
        return this.mMncStr;
    }

    @Override
    public int getChannelNumber() {
        return this.mArfcn;
    }

    @Deprecated
    public int getPsc() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mLac), Integer.valueOf(this.mCid), Integer.valueOf(super.hashCode()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CellIdentityGsm)) {
            return false;
        }
        CellIdentityGsm cellIdentityGsm = (CellIdentityGsm) obj;
        return this.mLac == cellIdentityGsm.mLac && this.mCid == cellIdentityGsm.mCid && this.mArfcn == cellIdentityGsm.mArfcn && this.mBsic == cellIdentityGsm.mBsic && TextUtils.equals(this.mMccStr, cellIdentityGsm.mMccStr) && TextUtils.equals(this.mMncStr, cellIdentityGsm.mMncStr) && super.equals(obj);
    }

    public String toString() {
        return TAG + ":{ mLac=" + this.mLac + " mCid=" + this.mCid + " mArfcn=" + this.mArfcn + " mBsic=0x" + Integer.toHexString(this.mBsic) + " mMcc=" + this.mMccStr + " mMnc=" + this.mMncStr + " mAlphaLong=" + this.mAlphaLong + " mAlphaShort=" + this.mAlphaShort + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, 1);
        parcel.writeInt(this.mLac);
        parcel.writeInt(this.mCid);
        parcel.writeInt(this.mArfcn);
        parcel.writeInt(this.mBsic);
    }

    private CellIdentityGsm(Parcel parcel) {
        super(TAG, 1, parcel);
        this.mLac = parcel.readInt();
        this.mCid = parcel.readInt();
        this.mArfcn = parcel.readInt();
        this.mBsic = parcel.readInt();
    }

    protected static CellIdentityGsm createFromParcelBody(Parcel parcel) {
        return new CellIdentityGsm(parcel);
    }
}
