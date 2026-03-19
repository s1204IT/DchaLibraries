package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import java.util.Objects;

public final class CellIdentityTdscdma extends CellIdentity {
    private static final boolean DBG = false;
    private final int mCid;
    private final int mCpid;
    private final int mLac;
    private static final String TAG = CellIdentityTdscdma.class.getSimpleName();
    public static final Parcelable.Creator<CellIdentityTdscdma> CREATOR = new Parcelable.Creator<CellIdentityTdscdma>() {
        @Override
        public CellIdentityTdscdma createFromParcel(Parcel parcel) {
            parcel.readInt();
            return CellIdentityTdscdma.createFromParcelBody(parcel);
        }

        @Override
        public CellIdentityTdscdma[] newArray(int i) {
            return new CellIdentityTdscdma[i];
        }
    };

    public CellIdentityTdscdma() {
        super(TAG, 5, null, null, null, null);
        this.mLac = Integer.MAX_VALUE;
        this.mCid = Integer.MAX_VALUE;
        this.mCpid = Integer.MAX_VALUE;
    }

    public CellIdentityTdscdma(int i, int i2, int i3, int i4, int i5) {
        this(String.valueOf(i), String.valueOf(i2), i3, i4, i5, null, null);
    }

    public CellIdentityTdscdma(String str, String str2, int i, int i2, int i3) {
        super(TAG, 5, str, str2, null, null);
        this.mLac = i;
        this.mCid = i2;
        this.mCpid = i3;
    }

    public CellIdentityTdscdma(String str, String str2, int i, int i2, int i3, String str3, String str4) {
        super(TAG, 5, str, str2, str3, str4);
        this.mLac = i;
        this.mCid = i2;
        this.mCpid = i3;
    }

    private CellIdentityTdscdma(CellIdentityTdscdma cellIdentityTdscdma) {
        this(cellIdentityTdscdma.mMccStr, cellIdentityTdscdma.mMncStr, cellIdentityTdscdma.mLac, cellIdentityTdscdma.mCid, cellIdentityTdscdma.mCpid, cellIdentityTdscdma.mAlphaLong, cellIdentityTdscdma.mAlphaShort);
    }

    CellIdentityTdscdma copy() {
        return new CellIdentityTdscdma(this);
    }

    public String getMccString() {
        return this.mMccStr;
    }

    public String getMncString() {
        return this.mMncStr;
    }

    public int getLac() {
        return this.mLac;
    }

    public int getCid() {
        return this.mCid;
    }

    public int getCpid() {
        return this.mCpid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mLac), Integer.valueOf(this.mCid), Integer.valueOf(this.mCpid), Integer.valueOf(super.hashCode()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CellIdentityTdscdma)) {
            return false;
        }
        CellIdentityTdscdma cellIdentityTdscdma = (CellIdentityTdscdma) obj;
        return TextUtils.equals(this.mMccStr, cellIdentityTdscdma.mMccStr) && TextUtils.equals(this.mMncStr, cellIdentityTdscdma.mMncStr) && this.mLac == cellIdentityTdscdma.mLac && this.mCid == cellIdentityTdscdma.mCid && this.mCpid == cellIdentityTdscdma.mCpid && super.equals(obj);
    }

    public String toString() {
        return TAG + ":{ mMcc=" + this.mMccStr + " mMnc=" + this.mMncStr + " mLac=" + this.mLac + " mCid=" + this.mCid + " mCpid=" + this.mCpid + " mAlphaLong=" + this.mAlphaLong + " mAlphaShort=" + this.mAlphaShort + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, 5);
        parcel.writeInt(this.mLac);
        parcel.writeInt(this.mCid);
        parcel.writeInt(this.mCpid);
    }

    private CellIdentityTdscdma(Parcel parcel) {
        super(TAG, 5, parcel);
        this.mLac = parcel.readInt();
        this.mCid = parcel.readInt();
        this.mCpid = parcel.readInt();
    }

    protected static CellIdentityTdscdma createFromParcelBody(Parcel parcel) {
        return new CellIdentityTdscdma(parcel);
    }
}
