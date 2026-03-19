package android.app.backup;

import android.os.Parcel;
import android.os.Parcelable;

public class RestoreDescription implements Parcelable {
    private static final String NO_MORE_PACKAGES_SENTINEL = "";
    public static final int TYPE_FULL_STREAM = 2;
    public static final int TYPE_KEY_VALUE = 1;
    private final int mDataType;
    private final String mPackageName;
    public static final RestoreDescription NO_MORE_PACKAGES = new RestoreDescription("", 0);
    public static final Parcelable.Creator<RestoreDescription> CREATOR = new Parcelable.Creator<RestoreDescription>() {
        @Override
        public RestoreDescription createFromParcel(Parcel in) {
            RestoreDescription unparceled = new RestoreDescription(in, (RestoreDescription) null);
            if (!"".equals(unparceled.mPackageName)) {
                return unparceled;
            }
            return RestoreDescription.NO_MORE_PACKAGES;
        }

        @Override
        public RestoreDescription[] newArray(int size) {
            return new RestoreDescription[size];
        }
    };

    RestoreDescription(Parcel in, RestoreDescription restoreDescription) {
        this(in);
    }

    public String toString() {
        return "RestoreDescription{" + this.mPackageName + " : " + (this.mDataType == 1 ? "KEY_VALUE" : "STREAM") + '}';
    }

    public RestoreDescription(String packageName, int dataType) {
        this.mPackageName = packageName;
        this.mDataType = dataType;
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
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.mPackageName);
        out.writeInt(this.mDataType);
    }

    private RestoreDescription(Parcel in) {
        this.mPackageName = in.readString();
        this.mDataType = in.readInt();
    }
}
