package com.android.ex.camera2.portability;

import android.hardware.Camera;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.debug.Log;

public class AndroidCameraSettings extends CameraSettings {
    private static final String RECORDING_HINT = "recording-hint";
    private static final Log.Tag TAG = new Log.Tag("AndCamSet");
    private static final String TRUE = "true";

    public AndroidCameraSettings(CameraCapabilities capabilities, Camera.Parameters params) {
        if (params == null) {
            Log.w(TAG, "Settings ctor requires a non-null Camera.Parameters.");
            return;
        }
        CameraCapabilities.Stringifier stringifier = capabilities.getStringifier();
        setSizesLocked(false);
        Camera.Size paramPreviewSize = params.getPreviewSize();
        setPreviewSize(new Size(paramPreviewSize.width, paramPreviewSize.height));
        setPreviewFrameRate(params.getPreviewFrameRate());
        int[] previewFpsRange = new int[2];
        params.getPreviewFpsRange(previewFpsRange);
        setPreviewFpsRange(previewFpsRange[0], previewFpsRange[1]);
        setPreviewFormat(params.getPreviewFormat());
        if (capabilities.supports(CameraCapabilities.Feature.ZOOM)) {
            setZoomRatio(params.getZoomRatios().get(params.getZoom()).intValue() / 100.0f);
        } else {
            setZoomRatio(1.0f);
        }
        setExposureCompensationIndex(params.getExposureCompensation());
        setFlashMode(stringifier.flashModeFromString(params.getFlashMode()));
        setFocusMode(stringifier.focusModeFromString(params.getFocusMode()));
        if (TRUE.equals(params.get(RECORDING_HINT))) {
            setSceneMode(CameraCapabilities.Stringifier.sceneModeFromString(params.getVideoSceneMode()));
        } else {
            setSceneMode(CameraCapabilities.Stringifier.sceneModeFromString(params.getSceneMode()));
        }
        setWhiteBalance(stringifier.whiteBalanceFromString(params.getWhiteBalance()));
        setBurstCaptureMode(stringifier.burstCaptureFromString(params.getBurstCaptureMode()));
        setBurstCaptureNum(params.getBurstCaptureNum());
        setColorEffect(stringifier.colorEffectFromString(params.getColorEffect()));
        setAntibanding(stringifier.antiBandingFromString(params.getAntibanding()));
        setIsoMode(stringifier.isoModeFromString(params.getIsoMode()));
        setContrast(params.getContrast());
        setBrightness(params.getBrightness());
        setSaturation(params.getSaturation());
        setSharpness(params.getSharpness());
        if (capabilities.supports(CameraCapabilities.Feature.VIDEO_STABILIZATION)) {
            setVideoStabilization(isVideoStabilizationEnabled());
        }
        if (capabilities.supports(CameraCapabilities.Feature.VIDEO_TNR)) {
            setVideoTNR(isVideoTNREnabled());
        }
        setRecordingHintEnabled(TRUE.equals(params.get(RECORDING_HINT)));
        setPhotoJpegCompressionQuality(params.getJpegQuality());
        Camera.Size paramPictureSize = params.getPictureSize();
        setPhotoSize(new Size(paramPictureSize.width, paramPictureSize.height));
        setPhotoFormat(params.getPictureFormat());
    }

    public AndroidCameraSettings(AndroidCameraSettings other) {
        super(other);
    }

    @Override
    public CameraSettings copy() {
        return new AndroidCameraSettings(this);
    }
}
