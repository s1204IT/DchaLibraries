package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.internal.logging.nano.MetricsProto;

public class PackageCleanItem implements Parcelable {
    public static final Parcelable.Creator<PackageCleanItem> CREATOR = new Parcelable.Creator<PackageCleanItem>() {
        @Override
        public PackageCleanItem createFromParcel(Parcel parcel) {
            return new PackageCleanItem(parcel);
        }

        @Override
        public PackageCleanItem[] newArray(int i) {
            return new PackageCleanItem[i];
        }
    };
    public final boolean andCode;
    public final String packageName;
    public final int userId;

    public PackageCleanItem(int i, String str, boolean z) {
        this.userId = i;
        this.packageName = str;
        this.andCode = z;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null) {
            try {
                PackageCleanItem packageCleanItem = (PackageCleanItem) obj;
                if (this.userId == packageCleanItem.userId && this.packageName.equals(packageCleanItem.packageName)) {
                    if (this.andCode == packageCleanItem.andCode) {
                        return true;
                    }
                }
                return false;
            } catch (ClassCastException e) {
            }
        }
        return false;
    }

    public int hashCode() {
        return (31 * (((MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + this.userId) * 31) + this.packageName.hashCode())) + (this.andCode ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.userId);
        parcel.writeString(this.packageName);
        parcel.writeInt(this.andCode ? 1 : 0);
    }

    private PackageCleanItem(Parcel parcel) {
        this.userId = parcel.readInt();
        this.packageName = parcel.readString();
        this.andCode = parcel.readInt() != 0;
    }
}
