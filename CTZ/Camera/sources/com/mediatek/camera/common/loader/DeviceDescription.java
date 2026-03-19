package com.mediatek.camera.common.loader;

import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Size;
import java.util.ArrayList;
import java.util.Iterator;

public class DeviceDescription {
    private CameraCharacteristics mCameraCharacteristics;
    private final Camera.CameraInfo mCameraInfo;
    private boolean mCshotSupport;
    private boolean mIsFlashCalibrationSupported;
    private boolean mIsFlashCustomizedAvailable;
    private CameraCharacteristics.Key<int[]> mKeyAisAvailableModes;
    private CaptureRequest.Key<int[]> mKeyAisRequestMode;
    private CaptureResult.Key<int[]> mKeyAisResult;
    private CameraCharacteristics.Key<int[]> mKeyAsdAvailableModes;
    private CaptureRequest.Key<int[]> mKeyAsdRequestMode;
    private CaptureResult.Key<int[]> mKeyAsdResult;
    private CaptureRequest.Key<int[]> mKeyCshotRequestMode;
    private CaptureRequest.Key<int[]> mKeyEisSessionParameter;
    private CaptureRequest.Key<int[]> mKeyFlashCalibrationRequest;
    private CaptureResult.Key<int[]> mKeyFlashCalibrationResult;
    private CaptureResult.Key<byte[]> mKeyFlashCustomizedResult;
    private CameraCharacteristics.Key<int[]> mKeyHdrAvailablePhotoModes;
    private CameraCharacteristics.Key<int[]> mKeyHdrAvailableVideoModes;
    private CaptureResult.Key<int[]> mKeyHdrDetectionResult;
    private CaptureRequest.Key<int[]> mKeyHdrRequestMode;
    private CaptureRequest.Key<int[]> mKeyHdrRequsetSessionMode;
    private CaptureRequest.Key<int[]> mKeyIsoRequestMode;
    private CaptureRequest.Key<int[]> mKeyP2NotificationRequestMode;
    private CaptureResult.Key<int[]> mKeyP2NotificationResult;
    private CaptureRequest.Key<int[]> mKeyPostViewRequestSizeMode;
    private CameraCharacteristics.Key<int[]> mKeyThumbnailAvailableModes;
    private ArrayList<Size> mKeyThumbnailSizes = new ArrayList<>();
    private CaptureRequest.Key<byte[]> mKeyZslMode;
    private Camera.Parameters mParameters;
    private boolean mSpeedUpSupported;
    private boolean mThumbnailPostViewSupport;
    private boolean mZslSupport;

    public DeviceDescription(Camera.CameraInfo cameraInfo) {
        this.mCameraInfo = cameraInfo;
    }

    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mCameraCharacteristics = cameraCharacteristics;
    }

    public void setParameters(Camera.Parameters parameters) {
        this.mParameters = parameters;
    }

    public CameraCharacteristics getCameraCharacteristics() {
        return this.mCameraCharacteristics;
    }

    public Camera.Parameters getParameters() {
        return this.mParameters;
    }

    public void storeCameraCharacKeys(CameraCharacteristics cameraCharacteristics) {
        for (Size size : (Size[]) cameraCharacteristics.get(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES)) {
            this.mKeyThumbnailSizes.add(size);
        }
        Iterator<CameraCharacteristics.Key<?>> it = cameraCharacteristics.getKeys().iterator();
        while (it.hasNext()) {
            CameraCharacteristics.Key<int[]> key = (CameraCharacteristics.Key) it.next();
            if (key.getName().equals("com.mediatek.hdrfeature.availableHdrModesPhoto")) {
                this.mKeyHdrAvailablePhotoModes = key;
            } else if (key.getName().equals("com.mediatek.hdrfeature.availableHdrModesVideo")) {
                this.mKeyHdrAvailableVideoModes = key;
            } else if (key.getName().equals("com.mediatek.control.capture.availablepostviewmodes")) {
                this.mKeyThumbnailAvailableModes = key;
                int[] iArr = (int[]) cameraCharacteristics.get(this.mKeyThumbnailAvailableModes);
                if (iArr != null) {
                    int length = iArr.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        if (iArr[i] != 1) {
                            i++;
                        } else {
                            this.mThumbnailPostViewSupport = true;
                            break;
                        }
                    }
                }
            } else if (key.getName().equals("com.mediatek.mfnrfeature.availablemfbmodes")) {
                this.mKeyAisAvailableModes = key;
            } else if (key.getName().equals("com.mediatek.flashfeature.customization.available")) {
                byte[] bArr = (byte[]) cameraCharacteristics.get(key);
                int length2 = bArr.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length2) {
                        break;
                    }
                    if (bArr[i2] != 1) {
                        i2++;
                    } else {
                        this.mIsFlashCustomizedAvailable = true;
                        break;
                    }
                }
            } else if (key.getName().equals("com.mediatek.cshotfeature.availableCShotModes")) {
                int[] iArr2 = (int[]) cameraCharacteristics.get(key);
                int length3 = iArr2.length;
                int i3 = 0;
                while (true) {
                    if (i3 >= length3) {
                        break;
                    }
                    if (iArr2[i3] != 1) {
                        i3++;
                    } else {
                        this.mCshotSupport = true;
                        break;
                    }
                }
            } else if (key.getName().equals("com.mediatek.control.capture.early.notification.support")) {
                int[] iArr3 = (int[]) cameraCharacteristics.get(key);
                int length4 = iArr3.length;
                int i4 = 0;
                while (true) {
                    if (i4 >= length4) {
                        break;
                    }
                    if (iArr3[i4] != 1) {
                        i4++;
                    } else {
                        this.mSpeedUpSupported = true;
                        break;
                    }
                }
            } else if (key.getName().equals("com.mediatek.facefeature.availableasdmodes")) {
                this.mKeyAsdAvailableModes = key;
            } else if (key.getName().equals("com.mediatek.control.capture.available.zsl.modes")) {
                byte[] bArr2 = (byte[]) cameraCharacteristics.get(key);
                int length5 = bArr2.length;
                int i5 = 0;
                while (true) {
                    if (i5 >= length5) {
                        break;
                    }
                    if (bArr2[i5] != 1) {
                        i5++;
                    } else {
                        this.mZslSupport = true;
                        break;
                    }
                }
            } else if (key.getName().equals("com.mediatek.flashfeature.calibration.available")) {
                int[] iArr4 = (int[]) cameraCharacteristics.get(key);
                int length6 = iArr4.length;
                int i6 = 0;
                while (true) {
                    if (i6 >= length6) {
                        break;
                    }
                    if (iArr4[i6] != 1) {
                        i6++;
                    } else {
                        this.mIsFlashCalibrationSupported = true;
                        break;
                    }
                }
            }
        }
        Iterator<CaptureResult.Key<?>> it2 = cameraCharacteristics.getAvailableCaptureResultKeys().iterator();
        while (it2.hasNext()) {
            CaptureResult.Key<byte[]> key2 = (CaptureResult.Key) it2.next();
            if (key2.getName().equals("com.mediatek.hdrfeature.hdrDetectionResult")) {
                this.mKeyHdrDetectionResult = key2;
            } else if (key2.getName().equals("com.mediatek.mfnrfeature.mfbresult")) {
                this.mKeyAisResult = key2;
            } else if (key2.getName().equals("com.mediatek.flashfeature.customizedResult")) {
                this.mKeyFlashCustomizedResult = key2;
            } else if (key2.getName().equals("com.mediatek.control.capture.next.ready")) {
                this.mKeyP2NotificationResult = key2;
            } else if (key2.getName().equals("com.mediatek.facefeature.asdresult")) {
                this.mKeyAsdResult = key2;
            } else if (key2.getName().equals("com.mediatek.flashfeature.calibration.result")) {
                this.mKeyFlashCalibrationResult = key2;
            }
        }
        Iterator<CaptureRequest.Key<?>> it3 = cameraCharacteristics.getAvailableCaptureRequestKeys().iterator();
        while (it3.hasNext()) {
            CaptureRequest.Key<byte[]> key3 = (CaptureRequest.Key) it3.next();
            if (key3.getName().equals("com.mediatek.hdrfeature.hdrMode")) {
                this.mKeyHdrRequestMode = key3;
            } else if (key3.getName().equals("com.mediatek.hdrfeature.SessionParamhdrMode")) {
                this.mKeyHdrRequsetSessionMode = key3;
            } else if (key3.getName().equals("com.mediatek.eisfeature.eismode")) {
                this.mKeyEisSessionParameter = key3;
            } else if (key3.getName().equals("com.mediatek.control.capture.zsl.mode")) {
                this.mKeyZslMode = key3;
            } else if (key3.getName().equals("com.mediatek.mfnrfeature.mfbmode")) {
                this.mKeyAisRequestMode = key3;
            } else if (key3.getName().equals("com.mediatek.3afeature.aeIsoSpeed")) {
                this.mKeyIsoRequestMode = key3;
            } else if (key3.getName().equals("com.mediatek.cshotfeature.capture")) {
                this.mKeyCshotRequestMode = key3;
            } else if (key3.getName().equals("com.mediatek.control.capture.early.notification.trigger")) {
                this.mKeyP2NotificationRequestMode = key3;
            } else if (key3.getName().equals("com.mediatek.facefeature.asdmode")) {
                this.mKeyAsdRequestMode = key3;
            } else if (key3.getName().equals("com.mediatek.control.capture.postviewsize")) {
                this.mKeyPostViewRequestSizeMode = key3;
            } else if (key3.getName().equals("com.mediatek.flashfeature.calibration.enable")) {
                this.mKeyFlashCalibrationRequest = key3;
            }
        }
    }

    public ArrayList<Size> getAvailableThumbnailSizes() {
        return this.mKeyThumbnailSizes;
    }

    public CameraCharacteristics.Key<int[]> getKeyHdrAvailablePhotoModes() {
        return this.mKeyHdrAvailablePhotoModes;
    }

    public CameraCharacteristics.Key<int[]> getKeyHdrAvailableVideoModes() {
        return this.mKeyHdrAvailableVideoModes;
    }

    public CaptureResult.Key<int[]> getKeyHdrDetectionResult() {
        return this.mKeyHdrDetectionResult;
    }

    public CaptureRequest.Key<int[]> getKeyHdrRequestMode() {
        return this.mKeyHdrRequestMode;
    }

    public CaptureRequest.Key<int[]> getKeyHdrRequsetSessionMode() {
        return this.mKeyHdrRequsetSessionMode;
    }

    public CaptureRequest.Key<int[]> getKeyEisRequsetSessionParameter() {
        return this.mKeyEisSessionParameter;
    }

    public CameraCharacteristics.Key<int[]> getKeyAisAvailableModes() {
        return this.mKeyAisAvailableModes;
    }

    public CaptureRequest.Key<int[]> getKeyAisRequestMode() {
        return this.mKeyAisRequestMode;
    }

    public CaptureResult.Key<int[]> getKeyAisResult() {
        return this.mKeyAisResult;
    }

    public CaptureRequest.Key<int[]> getKeyIsoRequestMode() {
        return this.mKeyIsoRequestMode;
    }

    public boolean isFlashCustomizedAvailable() {
        return this.mIsFlashCustomizedAvailable;
    }

    public CaptureResult.Key<byte[]> getKeyFlashCustomizedResult() {
        return this.mKeyFlashCustomizedResult;
    }

    public Boolean isThumbnailPostViewSupport() {
        return Boolean.valueOf(this.mThumbnailPostViewSupport);
    }

    public CaptureRequest.Key<int[]> getKeyPostViewRequestSizeMode() {
        return this.mKeyPostViewRequestSizeMode;
    }

    public Boolean isZslSupport() {
        return Boolean.valueOf(this.mZslSupport);
    }

    public CaptureRequest.Key<byte[]> getKeyZslRequestKey() {
        return this.mKeyZslMode;
    }

    public Boolean isCshotSupport() {
        return Boolean.valueOf(this.mCshotSupport);
    }

    public CaptureRequest.Key<int[]> getKeyCshotRequestMode() {
        return this.mKeyCshotRequestMode;
    }

    public Boolean isSpeedUpSupport() {
        return Boolean.valueOf(this.mSpeedUpSupported);
    }

    public CaptureRequest.Key<int[]> getKeyP2NotificationRequestMode() {
        return this.mKeyP2NotificationRequestMode;
    }

    public CaptureResult.Key<int[]> getKeyP2NotificationResult() {
        return this.mKeyP2NotificationResult;
    }

    public CameraCharacteristics.Key<int[]> getKeyAsdAvailableModes() {
        return this.mKeyAsdAvailableModes;
    }

    public CaptureRequest.Key<int[]> getKeyAsdRequestMode() {
        return this.mKeyAsdRequestMode;
    }

    public CaptureResult.Key<int[]> getKeyAsdResult() {
        return this.mKeyAsdResult;
    }

    public boolean isFlashCalibrationSupported() {
        return this.mIsFlashCalibrationSupported;
    }

    public CaptureRequest.Key<int[]> getKeyFlashCalibrationRequest() {
        return this.mKeyFlashCalibrationRequest;
    }

    public CaptureResult.Key<int[]> getKeyFlashCalibrationResult() {
        return this.mKeyFlashCalibrationResult;
    }
}
