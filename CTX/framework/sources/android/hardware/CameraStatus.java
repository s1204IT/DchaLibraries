package android.hardware;

import android.os.Parcel;
import android.os.Parcelable;

public class CameraStatus implements Parcelable {
    public static final Parcelable.Creator<CameraStatus> CREATOR = new Parcelable.Creator<CameraStatus>() {
        @Override
        public CameraStatus createFromParcel(Parcel parcel) {
            CameraStatus cameraStatus = new CameraStatus();
            cameraStatus.readFromParcel(parcel);
            return cameraStatus;
        }

        @Override
        public CameraStatus[] newArray(int i) {
            return new CameraStatus[i];
        }
    };
    public String cameraId;
    public int status;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.cameraId);
        parcel.writeInt(this.status);
    }

    public void readFromParcel(Parcel parcel) {
        this.cameraId = parcel.readString();
        this.status = parcel.readInt();
    }
}
