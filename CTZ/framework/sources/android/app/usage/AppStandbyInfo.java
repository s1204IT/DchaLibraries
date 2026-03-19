package android.app.usage;

import android.os.Parcel;
import android.os.Parcelable;

public final class AppStandbyInfo implements Parcelable {
    public static final Parcelable.Creator<AppStandbyInfo> CREATOR = new Parcelable.Creator<AppStandbyInfo>() {
        @Override
        public AppStandbyInfo createFromParcel(Parcel parcel) {
            return new AppStandbyInfo(parcel);
        }

        @Override
        public AppStandbyInfo[] newArray(int i) {
            return new AppStandbyInfo[i];
        }
    };
    public String mPackageName;
    public int mStandbyBucket;

    private AppStandbyInfo(Parcel parcel) {
        this.mPackageName = parcel.readString();
        this.mStandbyBucket = parcel.readInt();
    }

    public AppStandbyInfo(String str, int i) {
        this.mPackageName = str;
        this.mStandbyBucket = i;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.mPackageName);
        parcel.writeInt(this.mStandbyBucket);
    }
}
