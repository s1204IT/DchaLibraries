package com.android.camera.app;

import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraExceptionHandler;

public interface CameraProvider {
    CameraDeviceInfo.Characteristics getCharacteristics(int i);

    int getCurrentCameraId();

    int getFirstBackCameraId();

    int getFirstFrontCameraId();

    int getNumberOfCameras();

    boolean isBackFacingCamera(int i);

    boolean isFrontFacingCamera(int i);

    void releaseCamera(int i);

    void requestCamera(int i);

    void requestCamera(int i, boolean z);

    void setCameraExceptionHandler(CameraExceptionHandler cameraExceptionHandler);

    boolean waitingForCamera();
}
