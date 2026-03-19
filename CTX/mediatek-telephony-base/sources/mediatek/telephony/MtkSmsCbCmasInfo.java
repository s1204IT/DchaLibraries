package mediatek.telephony;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.SmsCbCmasInfo;

public class MtkSmsCbCmasInfo extends SmsCbCmasInfo {
    public static final long CMAS_EXPIRATION_UNKNOWN = 0;
    public static final Parcelable.Creator<SmsCbCmasInfo> CREATOR = new Parcelable.Creator<SmsCbCmasInfo>() {
        @Override
        public SmsCbCmasInfo createFromParcel(Parcel parcel) {
            return new MtkSmsCbCmasInfo(parcel);
        }

        @Override
        public SmsCbCmasInfo[] newArray(int i) {
            return new MtkSmsCbCmasInfo[i];
        }
    };
    private long mExpiration;

    public MtkSmsCbCmasInfo(int i, int i2, int i3, int i4, int i5, int i6, long j) {
        super(i, i2, i3, i4, i5, i6);
        this.mExpiration = j;
    }

    public MtkSmsCbCmasInfo(Parcel parcel) {
        super(parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
        this.mExpiration = parcel.readLong();
    }

    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeLong(this.mExpiration);
    }

    public String toString() {
        return super.toString() + "{" + this.mExpiration + "}";
    }

    public long getExpiration() {
        return this.mExpiration;
    }
}
