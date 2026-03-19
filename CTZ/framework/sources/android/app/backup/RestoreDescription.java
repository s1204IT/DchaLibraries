package android.app.backup;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

@SystemApi
public class RestoreDescription implements Parcelable {
    public static final int TYPE_FULL_STREAM = 2;
    public static final int TYPE_KEY_VALUE = 1;
    private final int mDataType;
    private final String mPackageName;
    private static final String NO_MORE_PACKAGES_SENTINEL = "NO_MORE_PACKAGES";
    public static final RestoreDescription NO_MORE_PACKAGES = new RestoreDescription(NO_MORE_PACKAGES_SENTINEL, 0);
    public static final Parcelable.Creator<RestoreDescription> CREATOR = new Parcelable.Creator<RestoreDescription>() {
        @Override
        public RestoreDescription createFromParcel(Parcel parcel) {
            RestoreDescription restoreDescription = new RestoreDescription(parcel);
            if (!RestoreDescription.NO_MORE_PACKAGES_SENTINEL.equals(restoreDescription.mPackageName)) {
                return restoreDescription;
            }
            return RestoreDescription.NO_MORE_PACKAGES;
        }

        @Override
        public RestoreDescription[] newArray(int i) {
            return new RestoreDescription[i];
        }
    };

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RestoreDescription{");
        sb.append(this.mPackageName);
        sb.append(" : ");
        sb.append(this.mDataType == 1 ? "KEY_VALUE" : "STREAM");
        sb.append('}');
        return sb.toString();
    }

    public RestoreDescription(String str, int i) {
        this.mPackageName = str;
        this.mDataType = i;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getDataType() {
        return this.mDataType;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPackageName);
        parcel.writeInt(this.mDataType);
    }

    private RestoreDescription(Parcel parcel) {
        this.mPackageName = parcel.readString();
        this.mDataType = parcel.readInt();
    }
}
