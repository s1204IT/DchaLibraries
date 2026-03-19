package android.security.keymaster;

import android.os.Parcel;
import android.os.Parcelable;

public class KeyAttestationApplicationId implements Parcelable {
    public static final Parcelable.Creator<KeyAttestationApplicationId> CREATOR = new Parcelable.Creator<KeyAttestationApplicationId>() {
        @Override
        public KeyAttestationApplicationId createFromParcel(Parcel parcel) {
            return new KeyAttestationApplicationId(parcel);
        }

        @Override
        public KeyAttestationApplicationId[] newArray(int i) {
            return new KeyAttestationApplicationId[i];
        }
    };
    private final KeyAttestationPackageInfo[] mAttestationPackageInfos;

    public KeyAttestationApplicationId(KeyAttestationPackageInfo[] keyAttestationPackageInfoArr) {
        this.mAttestationPackageInfos = keyAttestationPackageInfoArr;
    }

    public KeyAttestationPackageInfo[] getAttestationPackageInfos() {
        return this.mAttestationPackageInfos;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedArray(this.mAttestationPackageInfos, i);
    }

    KeyAttestationApplicationId(Parcel parcel) {
        this.mAttestationPackageInfos = (KeyAttestationPackageInfo[]) parcel.createTypedArray(KeyAttestationPackageInfo.CREATOR);
    }
}
