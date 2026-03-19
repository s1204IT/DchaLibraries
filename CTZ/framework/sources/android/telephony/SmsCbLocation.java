package android.telephony;

import android.os.Parcel;
import android.os.Parcelable;

public class SmsCbLocation implements Parcelable {
    public static final Parcelable.Creator<SmsCbLocation> CREATOR = new Parcelable.Creator<SmsCbLocation>() {
        @Override
        public SmsCbLocation createFromParcel(Parcel parcel) {
            return new SmsCbLocation(parcel);
        }

        @Override
        public SmsCbLocation[] newArray(int i) {
            return new SmsCbLocation[i];
        }
    };
    private final int mCid;
    private final int mLac;
    private final String mPlmn;

    public SmsCbLocation() {
        this.mPlmn = "";
        this.mLac = -1;
        this.mCid = -1;
    }

    public SmsCbLocation(String str) {
        this.mPlmn = str;
        this.mLac = -1;
        this.mCid = -1;
    }

    public SmsCbLocation(String str, int i, int i2) {
        this.mPlmn = str;
        this.mLac = i;
        this.mCid = i2;
    }

    public SmsCbLocation(Parcel parcel) {
        this.mPlmn = parcel.readString();
        this.mLac = parcel.readInt();
        this.mCid = parcel.readInt();
    }

    public String getPlmn() {
        return this.mPlmn;
    }

    public int getLac() {
        return this.mLac;
    }

    public int getCid() {
        return this.mCid;
    }

    public int hashCode() {
        return (((this.mPlmn.hashCode() * 31) + this.mLac) * 31) + this.mCid;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof SmsCbLocation)) {
            return false;
        }
        SmsCbLocation smsCbLocation = (SmsCbLocation) obj;
        if (this.mPlmn.equals(smsCbLocation.mPlmn) && this.mLac == smsCbLocation.mLac && this.mCid == smsCbLocation.mCid) {
            return true;
        }
        return false;
    }

    public String toString() {
        return '[' + this.mPlmn + ',' + this.mLac + ',' + this.mCid + ']';
    }

    public boolean isInLocationArea(SmsCbLocation smsCbLocation) {
        if (this.mCid != -1 && this.mCid != smsCbLocation.mCid) {
            return false;
        }
        if (this.mLac == -1 || this.mLac == smsCbLocation.mLac) {
            return this.mPlmn.equals(smsCbLocation.mPlmn);
        }
        return false;
    }

    public boolean isInLocationArea(String str, int i, int i2) {
        if (!this.mPlmn.equals(str)) {
            return false;
        }
        if (this.mLac == -1 || this.mLac == i) {
            return this.mCid == -1 || this.mCid == i2;
        }
        return false;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPlmn);
        parcel.writeInt(this.mLac);
        parcel.writeInt(this.mCid);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
