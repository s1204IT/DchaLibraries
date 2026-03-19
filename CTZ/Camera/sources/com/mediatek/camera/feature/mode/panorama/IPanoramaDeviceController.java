package com.mediatek.camera.feature.mode.panorama;

import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.utils.Size;

public interface IPanoramaDeviceController {

    public interface CameraStateCallback {
        void beforeCloseCamera();

        void onCameraOpened();

        void onCameraPreviewStarted();

        void onCameraPreviewStopped();
    }

    public interface PreviewCallback {
        void onPreviewCallback(byte[] bArr, int i);
    }

    public interface PreviewSizeCallback {
        void onPreviewSizeReady(Size size);
    }

    void closeCamera(boolean z);

    void configParameters();

    void destroyDeviceController();

    Size getPreviewSize(double d);

    void openCamera(PanoramaDeviceInfo panoramaDeviceInfo);

    void queryCameraDeviceManager();

    void setAutoRamaCallback(CameraProxy.VendorDataCallback vendorDataCallback);

    void setCameraStateCallback(CameraStateCallback cameraStateCallback);

    void setPreviewCallback(PreviewCallback previewCallback);

    void setPreviewSizeReadyCallback(PreviewSizeCallback previewSizeCallback);

    void startAutoRama(int i);

    void stopAutoRama(boolean z);

    void stopPreview();

    void updateGSensorOrientation(int i);

    void updatePreviewSurface(Object obj);
}
