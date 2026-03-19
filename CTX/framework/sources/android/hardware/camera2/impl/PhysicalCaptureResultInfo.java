package android.hardware.camera2.impl;

import android.os.Parcel;
import android.os.Parcelable;

public class PhysicalCaptureResultInfo implements Parcelable {
    public static final Parcelable.Creator<PhysicalCaptureResultInfo> CREATOR = new Parcelable.Creator<PhysicalCaptureResultInfo>() {
        @Override
        public PhysicalCaptureResultInfo createFromParcel(Parcel parcel) {
            return new PhysicalCaptureResultInfo(parcel);
        }

        @Override
        public PhysicalCaptureResultInfo[] newArray(int i) {
            return new PhysicalCaptureResultInfo[i];
        }
    };
    private String cameraId;
    private CameraMetadataNative cameraMetadata;

    private PhysicalCaptureResultInfo(Parcel parcel) {
        readFromParcel(parcel);
    }

    public PhysicalCaptureResultInfo(String str, CameraMetadataNative cameraMetadataNative) {
        this.cameraId = str;
        this.cameraMetadata = cameraMetadataNative;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.cameraId);
        this.cameraMetadata.writeToParcel(parcel, i);
    }

    public void readFromParcel(Parcel parcel) {
        this.cameraId = parcel.readString();
        this.cameraMetadata = new CameraMetadataNative();
        this.cameraMetadata.readFromParcel(parcel);
    }

    public String getCameraId() {
        return this.cameraId;
    }

    public CameraMetadataNative getCameraMetadata() {
        return this.cameraMetadata;
    }
}
