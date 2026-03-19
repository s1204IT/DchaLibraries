package android.hardware;

import android.hardware.Camera;
import android.os.Parcel;
import android.os.Parcelable;

public class CameraInfo implements Parcelable {
    public static final Parcelable.Creator<CameraInfo> CREATOR = new Parcelable.Creator<CameraInfo>() {
        @Override
        public CameraInfo createFromParcel(Parcel parcel) {
            CameraInfo cameraInfo = new CameraInfo();
            cameraInfo.readFromParcel(parcel);
            return cameraInfo;
        }

        @Override
        public CameraInfo[] newArray(int i) {
            return new CameraInfo[i];
        }
    };
    public Camera.CameraInfo info = new Camera.CameraInfo();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.info.facing);
        parcel.writeInt(this.info.orientation);
    }

    public void readFromParcel(Parcel parcel) {
        this.info.facing = parcel.readInt();
        this.info.orientation = parcel.readInt();
    }
}
