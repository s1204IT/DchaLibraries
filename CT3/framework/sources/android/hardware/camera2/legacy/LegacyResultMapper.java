package android.hardware.camera2.legacy;

import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.legacy.ParameterUtils;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.utils.ParamsUtils;
import android.location.Location;
import android.util.Log;
import android.util.Size;
import java.util.ArrayList;
import java.util.List;

public class LegacyResultMapper {
    private static final boolean DEBUG = false;
    private static final String TAG = "LegacyResultMapper";
    private LegacyRequest mCachedRequest = null;
    private CameraMetadataNative mCachedResult = null;

    public CameraMetadataNative cachedConvertResultMetadata(LegacyRequest legacyRequest, long timestamp) {
        CameraMetadataNative result;
        if (this.mCachedRequest != null && legacyRequest.parameters.same(this.mCachedRequest.parameters) && legacyRequest.captureRequest.equals((Object) this.mCachedRequest.captureRequest)) {
            result = new CameraMetadataNative(this.mCachedResult);
        } else {
            result = convertResultMetadata(legacyRequest);
            this.mCachedRequest = legacyRequest;
            this.mCachedResult = new CameraMetadataNative(result);
        }
        result.set(CaptureResult.SENSOR_TIMESTAMP, Long.valueOf(timestamp));
        return result;
    }

    private static CameraMetadataNative convertResultMetadata(LegacyRequest legacyRequest) {
        int stabMode;
        CameraCharacteristics characteristics = legacyRequest.characteristics;
        CaptureRequest request = legacyRequest.captureRequest;
        Size previewSize = legacyRequest.previewSize;
        Camera.Parameters params = legacyRequest.parameters;
        CameraMetadataNative result = new CameraMetadataNative();
        Rect activeArraySize = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        ParameterUtils.ZoomData zoomData = ParameterUtils.convertScalerCropRegion(activeArraySize, (Rect) request.get(CaptureRequest.SCALER_CROP_REGION), previewSize, params);
        result.set(CaptureResult.COLOR_CORRECTION_ABERRATION_MODE, (Integer) request.get(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE));
        mapAe(result, characteristics, request, activeArraySize, zoomData, params);
        mapAf(result, activeArraySize, zoomData, params);
        mapAwb(result, params);
        int captureIntent = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_CAPTURE_INTENT, 1)).intValue();
        result.set(CaptureResult.CONTROL_CAPTURE_INTENT, Integer.valueOf(LegacyRequestMapper.filterSupportedCaptureIntent(captureIntent)));
        int controlMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_MODE, 1)).intValue();
        if (controlMode == 2) {
            result.set((CaptureResult.Key<int>) CaptureResult.CONTROL_MODE, 2);
        } else {
            result.set((CaptureResult.Key<int>) CaptureResult.CONTROL_MODE, 1);
        }
        String legacySceneMode = params.getSceneMode();
        int mode = LegacyMetadataMapper.convertSceneModeFromLegacy(legacySceneMode);
        if (mode != -1) {
            result.set(CaptureResult.CONTROL_SCENE_MODE, Integer.valueOf(mode));
        } else {
            Log.w(TAG, "Unknown scene mode " + legacySceneMode + " returned by camera HAL, setting to disabled.");
            result.set((CaptureResult.Key<int>) CaptureResult.CONTROL_SCENE_MODE, 0);
        }
        String legacyEffectMode = params.getColorEffect();
        int mode2 = LegacyMetadataMapper.convertEffectModeFromLegacy(legacyEffectMode);
        if (mode2 != -1) {
            result.set(CaptureResult.CONTROL_EFFECT_MODE, Integer.valueOf(mode2));
        } else {
            Log.w(TAG, "Unknown effect mode " + legacyEffectMode + " returned by camera HAL, setting to off.");
            result.set((CaptureResult.Key<int>) CaptureResult.CONTROL_EFFECT_MODE, 0);
        }
        if (params.isVideoStabilizationSupported() && params.getVideoStabilization()) {
            stabMode = 1;
        } else {
            stabMode = 0;
        }
        result.set(CaptureResult.CONTROL_VIDEO_STABILIZATION_MODE, Integer.valueOf(stabMode));
        if (Camera.Parameters.FOCUS_MODE_INFINITY.equals(params.getFocusMode())) {
            result.set(CaptureResult.LENS_FOCUS_DISTANCE, Float.valueOf(0.0f));
        }
        result.set(CaptureResult.LENS_FOCAL_LENGTH, Float.valueOf(params.getFocalLength()));
        result.set(CaptureResult.REQUEST_PIPELINE_DEPTH, (Byte) characteristics.get(CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH));
        mapScaler(result, zoomData, params);
        result.set((CaptureResult.Key<int>) CaptureResult.SENSOR_TEST_PATTERN_MODE, 0);
        result.set(CaptureResult.JPEG_GPS_LOCATION, (Location) request.get(CaptureRequest.JPEG_GPS_LOCATION));
        result.set(CaptureResult.JPEG_ORIENTATION, (Integer) request.get(CaptureRequest.JPEG_ORIENTATION));
        result.set(CaptureResult.JPEG_QUALITY, Byte.valueOf((byte) params.getJpegQuality()));
        result.set(CaptureResult.JPEG_THUMBNAIL_QUALITY, Byte.valueOf((byte) params.getJpegThumbnailQuality()));
        Camera.Size s = params.getJpegThumbnailSize();
        if (s != null) {
            result.set(CaptureResult.JPEG_THUMBNAIL_SIZE, ParameterUtils.convertSize(s));
        } else {
            Log.w(TAG, "Null thumbnail size received from parameters.");
        }
        result.set(CaptureResult.NOISE_REDUCTION_MODE, (Integer) request.get(CaptureRequest.NOISE_REDUCTION_MODE));
        return result;
    }

    private static void mapAe(CameraMetadataNative m, CameraCharacteristics characteristics, CaptureRequest request, Rect activeArray, ParameterUtils.ZoomData zoomData, Camera.Parameters p) {
        int antiBandingMode = LegacyMetadataMapper.convertAntiBandingModeOrDefault(p.getAntibanding());
        m.set(CaptureResult.CONTROL_AE_ANTIBANDING_MODE, Integer.valueOf(antiBandingMode));
        m.set(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.valueOf(p.getExposureCompensation()));
        boolean autoExposureLock = p.isAutoExposureLockSupported() ? p.getAutoExposureLock() : false;
        m.set(CaptureResult.CONTROL_AE_LOCK, Boolean.valueOf(autoExposureLock));
        Boolean requestLock = (Boolean) request.get(CaptureRequest.CONTROL_AE_LOCK);
        if (requestLock != null && requestLock.booleanValue() != autoExposureLock) {
            Log.w(TAG, "mapAe - android.control.aeLock was requested to " + requestLock + " but resulted in " + autoExposureLock);
        }
        mapAeAndFlashMode(m, characteristics, p);
        if (p.getMaxNumMeteringAreas() <= 0) {
            return;
        }
        MeteringRectangle[] meteringRectArray = getMeteringRectangles(activeArray, zoomData, p.getMeteringAreas(), "AE");
        m.set(CaptureResult.CONTROL_AE_REGIONS, meteringRectArray);
    }

    private static void mapAf(CameraMetadataNative m, Rect activeArray, ParameterUtils.ZoomData zoomData, Camera.Parameters p) {
        m.set(CaptureResult.CONTROL_AF_MODE, Integer.valueOf(convertLegacyAfMode(p.getFocusMode())));
        if (p.getMaxNumFocusAreas() <= 0) {
            return;
        }
        MeteringRectangle[] meteringRectArray = getMeteringRectangles(activeArray, zoomData, p.getFocusAreas(), "AF");
        m.set(CaptureResult.CONTROL_AF_REGIONS, meteringRectArray);
    }

    private static void mapAwb(CameraMetadataNative m, Camera.Parameters p) {
        m.set(CaptureResult.CONTROL_AWB_LOCK, Boolean.valueOf(p.isAutoWhiteBalanceLockSupported() ? p.getAutoWhiteBalanceLock() : false));
        int awbMode = convertLegacyAwbMode(p.getWhiteBalance());
        m.set(CaptureResult.CONTROL_AWB_MODE, Integer.valueOf(awbMode));
    }

    private static MeteringRectangle[] getMeteringRectangles(Rect activeArray, ParameterUtils.ZoomData zoomData, List<Camera.Area> meteringAreaList, String regionName) {
        List<MeteringRectangle> meteringRectList = new ArrayList<>();
        if (meteringAreaList != null) {
            for (Camera.Area area : meteringAreaList) {
                ParameterUtils.WeightedRectangle rect = ParameterUtils.convertCameraAreaToActiveArrayRectangle(activeArray, zoomData, area);
                meteringRectList.add(rect.toMetering());
            }
        }
        return (MeteringRectangle[]) meteringRectList.toArray(new MeteringRectangle[0]);
    }

    private static void mapAeAndFlashMode(CameraMetadataNative m, CameraCharacteristics characteristics, Camera.Parameters p) {
        int flashMode = 0;
        Integer num = ((Boolean) characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)).booleanValue() ? null : 0;
        int aeMode = 1;
        String flashModeSetting = p.getFlashMode();
        if (flashModeSetting != null && !flashModeSetting.equals("off")) {
            if (flashModeSetting.equals("auto")) {
                aeMode = 2;
            } else if (flashModeSetting.equals("on")) {
                flashMode = 1;
                aeMode = 3;
                num = 3;
            } else if (flashModeSetting.equals(Camera.Parameters.FLASH_MODE_RED_EYE)) {
                aeMode = 4;
            } else if (flashModeSetting.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                flashMode = 2;
                num = 3;
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignoring unknown flash mode " + p.getFlashMode());
            }
        }
        m.set(CaptureResult.FLASH_STATE, num);
        m.set(CaptureResult.FLASH_MODE, Integer.valueOf(flashMode));
        m.set(CaptureResult.CONTROL_AE_MODE, Integer.valueOf(aeMode));
    }

    private static int convertLegacyAfMode(String mode) {
        if (mode == null) {
            Log.w(TAG, "convertLegacyAfMode - no AF mode, default to OFF");
            return 0;
        }
        if (mode.equals("auto")) {
            return 1;
        }
        if (mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            return 4;
        }
        if (mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            return 3;
        }
        if (mode.equals(Camera.Parameters.FOCUS_MODE_EDOF)) {
            return 5;
        }
        if (mode.equals(Camera.Parameters.FOCUS_MODE_MACRO)) {
            return 2;
        }
        if (mode.equals(Camera.Parameters.FOCUS_MODE_FIXED) || mode.equals(Camera.Parameters.FOCUS_MODE_INFINITY)) {
            return 0;
        }
        Log.w(TAG, "convertLegacyAfMode - unknown mode " + mode + " , ignoring");
        return 0;
    }

    private static int convertLegacyAwbMode(String mode) {
        if (mode == null || mode.equals("auto")) {
            return 1;
        }
        if (mode.equals(Camera.Parameters.WHITE_BALANCE_INCANDESCENT)) {
            return 2;
        }
        if (mode.equals(Camera.Parameters.WHITE_BALANCE_FLUORESCENT)) {
            return 3;
        }
        if (mode.equals(Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT)) {
            return 4;
        }
        if (mode.equals(Camera.Parameters.WHITE_BALANCE_DAYLIGHT)) {
            return 5;
        }
        if (mode.equals(Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT)) {
            return 6;
        }
        if (mode.equals(Camera.Parameters.WHITE_BALANCE_TWILIGHT)) {
            return 7;
        }
        if (mode.equals(Camera.Parameters.WHITE_BALANCE_SHADE)) {
            return 8;
        }
        Log.w(TAG, "convertAwbMode - unrecognized WB mode " + mode);
        return 1;
    }

    private static void mapScaler(CameraMetadataNative m, ParameterUtils.ZoomData zoomData, Camera.Parameters p) {
        m.set(CaptureResult.SCALER_CROP_REGION, zoomData.reportedCrop);
    }
}
