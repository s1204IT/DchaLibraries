package com.mediatek.camera.common.mode.photo.device;

import com.mediatek.camera.common.mode.photo.DeviceInfo;
import com.mediatek.camera.common.utils.Size;

public interface IDeviceController {

    public interface CaptureDataCallback {
        void onDataReceived(DataCallbackInfo dataCallbackInfo);

        void onPostViewCallback(byte[] bArr);
    }

    public static class DataCallbackInfo {
        public byte[] data;
        public int imageHeight;
        public int imageWidth;
        public int mBufferFormat = 256;
        public boolean needRestartPreview;
        public boolean needUpdateThumbnail;
    }

    public interface DeviceCallback {
        void afterStopPreview();

        void beforeCloseCamera();

        void onCameraOpened(String str);

        void onPreviewCallback(byte[] bArr, int i);
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

    void setFormat(String str);

    void setPictureSize(Size size);

    void setPreviewSizeReadyCallback(PreviewSizeCallback previewSizeCallback);

    void startPreview();

    void stopPreview();

    void takePicture(CaptureDataCallback captureDataCallback);

    void updateGSensorOrientation(int i);

    void updatePreviewSurface(Object obj);
}
