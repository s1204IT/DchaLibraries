package androidx.versionedparcelable;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes.dex */
public class ParcelImpl implements Parcelable {
    public static final Parcelable.Creator<ParcelImpl> CREATOR = new Parcelable.Creator<ParcelImpl>() { // from class: androidx.versionedparcelable.ParcelImpl.1
        /* JADX DEBUG: Method merged with bridge method: createFromParcel(Landroid/os/Parcel;)Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelImpl createFromParcel(Parcel in) {
            return new ParcelImpl(in);
        }

        /* JADX DEBUG: Method merged with bridge method: newArray(I)[Ljava/lang/Object; */
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ParcelImpl[] newArray(int size) {
            return new ParcelImpl[size];
        }
    };
    private final VersionedParcelable mParcel;

    protected ParcelImpl(Parcel in) {
        this.mParcel = new VersionedParcelParcel(in).readVersionedParcelable();
    }

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        VersionedParcelParcel parcel = new VersionedParcelParcel(dest);
        parcel.writeVersionedParcelable(this.mParcel);
    }
}
