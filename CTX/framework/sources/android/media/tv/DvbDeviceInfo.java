package android.media.tv;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public final class DvbDeviceInfo implements Parcelable {
    public static final Parcelable.Creator<DvbDeviceInfo> CREATOR = new Parcelable.Creator<DvbDeviceInfo>() {
        @Override
        public DvbDeviceInfo createFromParcel(Parcel parcel) {
            try {
                return new DvbDeviceInfo(parcel);
            } catch (Exception e) {
                Log.e(DvbDeviceInfo.TAG, "Exception creating DvbDeviceInfo from parcel", e);
                return null;
            }
        }

        @Override
        public DvbDeviceInfo[] newArray(int i) {
            return new DvbDeviceInfo[i];
        }
    };
    static final String TAG = "DvbDeviceInfo";
    private final int mAdapterId;
    private final int mDeviceId;

    private DvbDeviceInfo(Parcel parcel) {
        this.mAdapterId = parcel.readInt();
        this.mDeviceId = parcel.readInt();
    }

    public DvbDeviceInfo(int i, int i2) {
        this.mAdapterId = i;
        this.mDeviceId = i2;
    }

    public int getAdapterId() {
        return this.mAdapterId;
    }

    public int getDeviceId() {
        return this.mDeviceId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.mAdapterId);
        parcel.writeInt(this.mDeviceId);
    }
}
