package com.mediatek.camera.feature.mode.vsdof.photo.device;

import com.mediatek.camera.common.mode.photo.device.IDeviceController;
import com.mediatek.camera.common.utils.Size;
import com.mediatek.camera.feature.mode.vsdof.photo.DeviceInfo;

public interface ISdofPhotoDeviceController {

    public interface DeviceCallback {
        void beforeCloseCamera();

        void onCameraOpened(String str);

        void onPreviewCallback(byte[] bArr, int i);
    }

    public interface PreviewSizeCallback {
        void onPreviewSizeReady(Size size);
    }

    public interface StereoWarningCallback {
        void onWarning(int i);
    }

    void closeCamera(boolean z);

    void destroyDeviceController();

    Size getPreviewSize(double d);

    boolean isReadyForCapture();

    void openCamera(DeviceInfo deviceInfo);

    void queryCameraDeviceManager();

    void setDeviceCallback(DeviceCallback deviceCallback);

    void setPictureSize(Size size);

    void setPreviewSizeReadyCallback(PreviewSizeCallback previewSizeCallback);

    void setStereoWarningCallback(StereoWarningCallback stereoWarningCallback);

    void setVsDofLevelParameter(int i);

    void startPreview();

    void stopPreview();

    void takePicture(IDeviceController.CaptureDataCallback captureDataCallback);

    void updateGSensorOrientation(int i);

    void updatePreviewSurface(Object obj);
}
