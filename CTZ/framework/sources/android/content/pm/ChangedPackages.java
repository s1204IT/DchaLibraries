package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;

public final class ChangedPackages implements Parcelable {
    public static final Parcelable.Creator<ChangedPackages> CREATOR = new Parcelable.Creator<ChangedPackages>() {
        @Override
        public ChangedPackages createFromParcel(Parcel parcel) {
            return new ChangedPackages(parcel);
        }

        @Override
        public ChangedPackages[] newArray(int i) {
            return new ChangedPackages[i];
        }
    };
    private final List<String> mPackageNames;
    private final int mSequenceNumber;

    public ChangedPackages(int i, List<String> list) {
        this.mSequenceNumber = i;
        this.mPackageNames = list;
    }

    protected ChangedPackages(Parcel parcel) {
        this.mSequenceNumber = parcel.readInt();
        this.mPackageNames = parcel.createStringArrayList();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mSequenceNumber);
        parcel.writeStringList(this.mPackageNames);
    }

    public int getSequenceNumber() {
        return this.mSequenceNumber;
    }

    public List<String> getPackageNames() {
        return this.mPackageNames;
    }
}
