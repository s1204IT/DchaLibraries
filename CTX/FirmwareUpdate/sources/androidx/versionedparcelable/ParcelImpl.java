package androidx.versionedparcelable;

import android.os.Parcel;
import android.os.Parcelable;

public class ParcelImpl implements Parcelable {
    public static final Parcelable.Creator<ParcelImpl> CREATOR = new Parcelable.Creator<ParcelImpl>() {
        @Override
        public ParcelImpl createFromParcel(Parcel parcel) {
            return new ParcelImpl(parcel);
        }

        @Override
        public ParcelImpl[] newArray(int i) {
            return new ParcelImpl[i];
        }
    };
    private final VersionedParcelable mParcel;

    protected ParcelImpl(Parcel parcel) {
        this.mParcel = new VersionedParcelParcel(parcel).readVersionedParcelable();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        new VersionedParcelParcel(parcel).writeVersionedParcelable(this.mParcel);
    }
}
