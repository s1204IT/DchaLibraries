package com.mediatek.camera.feature.mode.longexposure;

import com.mediatek.camera.common.utils.Size;

public interface ILongExposureDeviceController {

    public interface DeviceCallback {
        void afterStopPreview();

        void beforeCloseCamera();

        void onCameraOpened(String str);

        void onPreviewCallback(byte[] bArr, int i);
    }

    public interface JpegCallback {
        void onDataReceived(byte[] bArr);
    }

    public interface PreviewSizeCallback {
        void onPreviewSizeReady(Size size);
    }

    void closeCamera(boolean z);

    void destroyDeviceController();

    Size getPreviewSize(double d);

    boolean isReadyForCapture();

    void openCamera(DeviceInfo deviceInfo);

    void queryCameraDeviceManager();

    void setDeviceCallback(DeviceCallback deviceCallback);

    void setNeedWaitPictureDone(boolean z);

    void setPictureSize(Size size);

    void setPreviewSizeReadyCallback(PreviewSizeCallback previewSizeCallback);

    void startPreview();

    void stopCapture();

    void stopPreview();

    void takePicture(JpegCallback jpegCallback);

    void updateGSensorOrientation(int i);

    void updatePreviewSurface(Object obj);
}
