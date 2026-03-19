package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Objects;

public final class CellIdentityCdma extends CellIdentity {
    private static final boolean DBG = false;
    private final int mBasestationId;
    private final int mLatitude;
    private final int mLongitude;
    private final int mNetworkId;
    private final int mSystemId;
    private static final String TAG = CellIdentityCdma.class.getSimpleName();
    public static final Parcelable.Creator<CellIdentityCdma> CREATOR = new Parcelable.Creator<CellIdentityCdma>() {
        @Override
        public CellIdentityCdma createFromParcel(Parcel parcel) {
            parcel.readInt();
            return CellIdentityCdma.createFromParcelBody(parcel);
        }

        @Override
        public CellIdentityCdma[] newArray(int i) {
            return new CellIdentityCdma[i];
        }
    };

    public CellIdentityCdma() {
        super(TAG, 2, null, null, null, null);
        this.mNetworkId = Integer.MAX_VALUE;
        this.mSystemId = Integer.MAX_VALUE;
        this.mBasestationId = Integer.MAX_VALUE;
        this.mLongitude = Integer.MAX_VALUE;
        this.mLatitude = Integer.MAX_VALUE;
    }

    public CellIdentityCdma(int i, int i2, int i3, int i4, int i5) {
        this(i, i2, i3, i4, i5, null, null);
    }

    public CellIdentityCdma(int i, int i2, int i3, int i4, int i5, String str, String str2) {
        super(TAG, 2, null, null, str, str2);
        this.mNetworkId = i;
        this.mSystemId = i2;
        this.mBasestationId = i3;
        if (!isNullIsland(i5, i4)) {
            this.mLongitude = i4;
            this.mLatitude = i5;
        } else {
            this.mLatitude = Integer.MAX_VALUE;
            this.mLongitude = Integer.MAX_VALUE;
        }
    }

    private CellIdentityCdma(CellIdentityCdma cellIdentityCdma) {
        this(cellIdentityCdma.mNetworkId, cellIdentityCdma.mSystemId, cellIdentityCdma.mBasestationId, cellIdentityCdma.mLongitude, cellIdentityCdma.mLatitude, cellIdentityCdma.mAlphaLong, cellIdentityCdma.mAlphaShort);
    }

    CellIdentityCdma copy() {
        return new CellIdentityCdma(this);
    }

    private boolean isNullIsland(int i, int i2) {
        return Math.abs(i) <= 1 && Math.abs(i2) <= 1;
    }

    public int getNetworkId() {
        return this.mNetworkId;
    }

    public int getSystemId() {
        return this.mSystemId;
    }

    public int getBasestationId() {
        return this.mBasestationId;
    }

    public int getLongitude() {
        return this.mLongitude;
    }

    public int getLatitude() {
        return this.mLatitude;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Integer.valueOf(this.mNetworkId), Integer.valueOf(this.mSystemId), Integer.valueOf(this.mBasestationId), Integer.valueOf(this.mLatitude), Integer.valueOf(this.mLongitude), Integer.valueOf(super.hashCode()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CellIdentityCdma)) {
            return false;
        }
        CellIdentityCdma cellIdentityCdma = (CellIdentityCdma) obj;
        return this.mNetworkId == cellIdentityCdma.mNetworkId && this.mSystemId == cellIdentityCdma.mSystemId && this.mBasestationId == cellIdentityCdma.mBasestationId && this.mLatitude == cellIdentityCdma.mLatitude && this.mLongitude == cellIdentityCdma.mLongitude && super.equals(obj);
    }

    public String toString() {
        return TAG + ":{ mNetworkId=" + this.mNetworkId + " mSystemId=" + this.mSystemId + " mBasestationId=" + this.mBasestationId + " mLongitude=" + this.mLongitude + " mLatitude=" + this.mLatitude + " mAlphaLong=" + this.mAlphaLong + " mAlphaShort=" + this.mAlphaShort + "}";
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, 2);
        parcel.writeInt(this.mNetworkId);
        parcel.writeInt(this.mSystemId);
        parcel.writeInt(this.mBasestationId);
        parcel.writeInt(this.mLongitude);
        parcel.writeInt(this.mLatitude);
    }

    private CellIdentityCdma(Parcel parcel) {
        super(TAG, 2, parcel);
        this.mNetworkId = parcel.readInt();
        this.mSystemId = parcel.readInt();
        this.mBasestationId = parcel.readInt();
        this.mLongitude = parcel.readInt();
        this.mLatitude = parcel.readInt();
    }

    protected static CellIdentityCdma createFromParcelBody(Parcel parcel) {
        return new CellIdentityCdma(parcel);
    }
}
