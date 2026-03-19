package android.hardware.camera2.legacy;

import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.legacy.ParameterUtils;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.utils.ListUtils;
import android.hardware.camera2.utils.ParamsUtils;
import android.location.Location;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class LegacyRequestMapper {
    private static final boolean DEBUG = ParameterUtils.DEBUG;
    private static final byte DEFAULT_JPEG_QUALITY = 85;
    private static final String TAG = "LegacyRequestMapper";

    public static void convertRequestMetadata(LegacyRequest legacyRequest) {
        String strConvertAeAntiBandingModeToLegacy;
        String strConvertSceneModeToLegacy;
        int[] next;
        CameraCharacteristics cameraCharacteristics = legacyRequest.characteristics;
        CaptureRequest captureRequest = legacyRequest.captureRequest;
        Size size = legacyRequest.previewSize;
        Camera.Parameters parameters = legacyRequest.parameters;
        Rect rect = (Rect) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        ParameterUtils.ZoomData zoomDataConvertScalerCropRegion = ParameterUtils.convertScalerCropRegion(rect, (Rect) captureRequest.get(CaptureRequest.SCALER_CROP_REGION), size, parameters);
        if (parameters.isZoomSupported()) {
            parameters.setZoom(zoomDataConvertScalerCropRegion.zoomIndex);
        } else if (DEBUG) {
            Log.v(TAG, "convertRequestToMetadata - zoom is not supported");
        }
        int iIntValue = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, 1)).intValue();
        if (iIntValue != 1 && iIntValue != 2) {
            Log.w(TAG, "convertRequestToMetadata - Ignoring unsupported colorCorrection.aberrationMode = " + iIntValue);
        }
        Integer num = (Integer) captureRequest.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE);
        if (num != null) {
            strConvertAeAntiBandingModeToLegacy = convertAeAntiBandingModeToLegacy(num.intValue());
        } else {
            strConvertAeAntiBandingModeToLegacy = (String) ListUtils.listSelectFirstFrom(parameters.getSupportedAntibanding(), new String[]{"auto", "off", Camera.Parameters.ANTIBANDING_50HZ, Camera.Parameters.ANTIBANDING_60HZ});
        }
        if (strConvertAeAntiBandingModeToLegacy != null) {
            parameters.setAntibanding(strConvertAeAntiBandingModeToLegacy);
        }
        MeteringRectangle[] meteringRectangleArr = (MeteringRectangle[]) captureRequest.get(CaptureRequest.CONTROL_AE_REGIONS);
        if (captureRequest.get(CaptureRequest.CONTROL_AWB_REGIONS) != null) {
            Log.w(TAG, "convertRequestMetadata - control.awbRegions setting is not supported, ignoring value");
        }
        int maxNumMeteringAreas = parameters.getMaxNumMeteringAreas();
        List<Camera.Area> listConvertMeteringRegionsToLegacy = convertMeteringRegionsToLegacy(rect, zoomDataConvertScalerCropRegion, meteringRectangleArr, maxNumMeteringAreas, "AE");
        if (maxNumMeteringAreas > 0) {
            parameters.setMeteringAreas(listConvertMeteringRegionsToLegacy);
        }
        MeteringRectangle[] meteringRectangleArr2 = (MeteringRectangle[]) captureRequest.get(CaptureRequest.CONTROL_AF_REGIONS);
        int maxNumFocusAreas = parameters.getMaxNumFocusAreas();
        List<Camera.Area> listConvertMeteringRegionsToLegacy2 = convertMeteringRegionsToLegacy(rect, zoomDataConvertScalerCropRegion, meteringRectangleArr2, maxNumFocusAreas, "AF");
        if (maxNumFocusAreas > 0) {
            parameters.setFocusAreas(listConvertMeteringRegionsToLegacy2);
        }
        Range range = (Range) captureRequest.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
        String strConvertAwbModeToLegacy = null;
        if (range != null) {
            int[] iArrConvertAeFpsRangeToLegacy = convertAeFpsRangeToLegacy(range);
            Iterator<int[]> it = parameters.getSupportedPreviewFpsRange().iterator();
            while (true) {
                if (it.hasNext()) {
                    next = it.next();
                    int iFloor = ((int) Math.floor(((double) next[0]) / 1000.0d)) * 1000;
                    int iCeil = ((int) Math.ceil(((double) next[1]) / 1000.0d)) * 1000;
                    if (iArrConvertAeFpsRangeToLegacy[0] == iFloor && iArrConvertAeFpsRangeToLegacy[1] == iCeil) {
                        break;
                    }
                } else {
                    next = null;
                    break;
                }
            }
            if (next != null) {
                parameters.setPreviewFpsRange(next[0], next[1]);
            } else {
                Log.w(TAG, "Unsupported FPS range set [" + iArrConvertAeFpsRangeToLegacy[0] + "," + iArrConvertAeFpsRangeToLegacy[1] + "]");
            }
        }
        Range range2 = (Range) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        int iIntValue2 = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)).intValue();
        if (!range2.contains(Integer.valueOf(iIntValue2))) {
            Log.w(TAG, "convertRequestMetadata - control.aeExposureCompensation is out of range, ignoring value");
            iIntValue2 = 0;
        }
        parameters.setExposureCompensation(iIntValue2);
        Boolean bool = (Boolean) getIfSupported(captureRequest, CaptureRequest.CONTROL_AE_LOCK, false, parameters.isAutoExposureLockSupported(), false);
        if (bool != null) {
            parameters.setAutoExposureLock(bool.booleanValue());
        }
        if (DEBUG) {
            Log.v(TAG, "convertRequestToMetadata - control.aeLock set to " + bool);
        }
        mapAeAndFlashMode(captureRequest, parameters);
        int iIntValue3 = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_AF_MODE, 0)).intValue();
        String strConvertAfModeToLegacy = LegacyMetadataMapper.convertAfModeToLegacy(iIntValue3, parameters.getSupportedFocusModes());
        if (strConvertAfModeToLegacy != null) {
            parameters.setFocusMode(strConvertAfModeToLegacy);
        }
        if (DEBUG) {
            Log.v(TAG, "convertRequestToMetadata - control.afMode " + iIntValue3 + " mapped to " + strConvertAfModeToLegacy);
        }
        Integer num2 = (Integer) getIfSupported(captureRequest, CaptureRequest.CONTROL_AWB_MODE, 1, parameters.getSupportedWhiteBalance() != null, 1);
        if (num2 != null) {
            strConvertAwbModeToLegacy = convertAwbModeToLegacy(num2.intValue());
            parameters.setWhiteBalance(strConvertAwbModeToLegacy);
        }
        if (DEBUG) {
            Log.v(TAG, "convertRequestToMetadata - control.awbMode " + num2 + " mapped to " + strConvertAwbModeToLegacy);
        }
        Boolean bool2 = (Boolean) getIfSupported(captureRequest, CaptureRequest.CONTROL_AWB_LOCK, false, parameters.isAutoWhiteBalanceLockSupported(), false);
        if (bool2 != null) {
            parameters.setAutoWhiteBalanceLock(bool2.booleanValue());
        }
        int iFilterSupportedCaptureIntent = filterSupportedCaptureIntent(((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_CAPTURE_INTENT, 1)).intValue());
        parameters.setRecordingHint(iFilterSupportedCaptureIntent == 3 || iFilterSupportedCaptureIntent == 4);
        Integer num3 = (Integer) getIfSupported(captureRequest, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 0, parameters.isVideoStabilizationSupported(), 0);
        if (num3 != null) {
            parameters.setVideoStabilization(num3.intValue() == 1);
        }
        boolean zListContains = ListUtils.listContains(parameters.getSupportedFocusModes(), Camera.Parameters.FOCUS_MODE_INFINITY);
        Float f = (Float) getIfSupported(captureRequest, CaptureRequest.LENS_FOCUS_DISTANCE, Float.valueOf(0.0f), zListContains, Float.valueOf(0.0f));
        if (f == null || f.floatValue() != 0.0f) {
            Log.w(TAG, "convertRequestToMetadata - Ignoring android.lens.focusDistance " + zListContains + ", only 0.0f is supported");
        }
        if (parameters.getSupportedSceneModes() != null) {
            int iIntValue4 = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_MODE, 1)).intValue();
            switch (iIntValue4) {
                case 1:
                    strConvertSceneModeToLegacy = "auto";
                    break;
                case 2:
                    int iIntValue5 = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_SCENE_MODE, 0)).intValue();
                    strConvertSceneModeToLegacy = LegacyMetadataMapper.convertSceneModeToLegacy(iIntValue5);
                    if (strConvertSceneModeToLegacy == null) {
                        strConvertSceneModeToLegacy = "auto";
                        Log.w(TAG, "Skipping unknown requested scene mode: " + iIntValue5);
                    }
                    break;
                default:
                    Log.w(TAG, "Control mode " + iIntValue4 + " is unsupported, defaulting to AUTO");
                    strConvertSceneModeToLegacy = "auto";
                    break;
            }
            parameters.setSceneMode(strConvertSceneModeToLegacy);
        }
        if (parameters.getSupportedColorEffects() != null) {
            int iIntValue6 = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_EFFECT_MODE, 0)).intValue();
            String strConvertEffectModeToLegacy = LegacyMetadataMapper.convertEffectModeToLegacy(iIntValue6);
            if (strConvertEffectModeToLegacy != null) {
                parameters.setColorEffect(strConvertEffectModeToLegacy);
            } else {
                parameters.setColorEffect("none");
                Log.w(TAG, "Skipping unknown requested effect mode: " + iIntValue6);
            }
        }
        int iIntValue7 = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.SENSOR_TEST_PATTERN_MODE, 0)).intValue();
        if (iIntValue7 != 0) {
            Log.w(TAG, "convertRequestToMetadata - ignoring sensor.testPatternMode " + iIntValue7 + "; only OFF is supported");
        }
        Location location = (Location) captureRequest.get(CaptureRequest.JPEG_GPS_LOCATION);
        if (location != null) {
            if (checkForCompleteGpsData(location)) {
                parameters.setGpsAltitude(location.getAltitude());
                parameters.setGpsLatitude(location.getLatitude());
                parameters.setGpsLongitude(location.getLongitude());
                parameters.setGpsProcessingMethod(location.getProvider().toUpperCase());
                parameters.setGpsTimestamp(location.getTime());
            } else {
                Log.w(TAG, "Incomplete GPS parameters provided in location " + location);
            }
        } else {
            parameters.removeGpsData();
        }
        Integer num4 = (Integer) captureRequest.get(CaptureRequest.JPEG_ORIENTATION);
        parameters.setRotation(((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(num4 == null ? 0 : num4.intValue()))).intValue());
        parameters.setJpegQuality(((Byte) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.JPEG_QUALITY, Byte.valueOf(DEFAULT_JPEG_QUALITY))).byteValue() & 255);
        parameters.setJpegThumbnailQuality(((Byte) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.JPEG_THUMBNAIL_QUALITY, Byte.valueOf(DEFAULT_JPEG_QUALITY))).byteValue() & 255);
        List<Camera.Size> supportedJpegThumbnailSizes = parameters.getSupportedJpegThumbnailSizes();
        if (supportedJpegThumbnailSizes != null && supportedJpegThumbnailSizes.size() > 0) {
            Size size2 = (Size) captureRequest.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);
            boolean z = (size2 == null || ParameterUtils.containsSize(supportedJpegThumbnailSizes, size2.getWidth(), size2.getHeight())) ? false : true;
            if (z) {
                Log.w(TAG, "Invalid JPEG thumbnail size set " + size2 + ", skipping thumbnail...");
            }
            if (size2 == null || z) {
                parameters.setJpegThumbnailSize(0, 0);
            } else {
                parameters.setJpegThumbnailSize(size2.getWidth(), size2.getHeight());
            }
        }
        int iIntValue8 = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.NOISE_REDUCTION_MODE, 1)).intValue();
        if (iIntValue8 != 1 && iIntValue8 != 2) {
            Log.w(TAG, "convertRequestToMetadata - Ignoring unsupported noiseReduction.mode = " + iIntValue8);
        }
    }

    private static boolean checkForCompleteGpsData(Location location) {
        return (location == null || location.getProvider() == null || location.getTime() == 0) ? false : true;
    }

    static int filterSupportedCaptureIntent(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return i;
            case 5:
            case 6:
                Log.w(TAG, "Unsupported control.captureIntent value 1; default to PREVIEW");
                break;
        }
        Log.w(TAG, "Unknown control.captureIntent value 1; default to PREVIEW");
        return 1;
    }

    private static List<Camera.Area> convertMeteringRegionsToLegacy(Rect rect, ParameterUtils.ZoomData zoomData, MeteringRectangle[] meteringRectangleArr, int i, String str) {
        if (meteringRectangleArr == null || i <= 0) {
            if (i > 0) {
                return Arrays.asList(ParameterUtils.CAMERA_AREA_DEFAULT);
            }
            return null;
        }
        ArrayList arrayList = new ArrayList();
        for (MeteringRectangle meteringRectangle : meteringRectangleArr) {
            if (meteringRectangle.getMeteringWeight() != 0) {
                arrayList.add(meteringRectangle);
            }
        }
        if (arrayList.size() == 0) {
            Log.w(TAG, "Only received metering rectangles with weight 0.");
            return Arrays.asList(ParameterUtils.CAMERA_AREA_DEFAULT);
        }
        int iMin = Math.min(i, arrayList.size());
        ArrayList arrayList2 = new ArrayList(iMin);
        for (int i2 = 0; i2 < iMin; i2++) {
            arrayList2.add(ParameterUtils.convertMeteringRectangleToLegacy(rect, (MeteringRectangle) arrayList.get(i2), zoomData).meteringArea);
        }
        if (i < arrayList.size()) {
            Log.w(TAG, "convertMeteringRegionsToLegacy - Too many requested " + str + " regions, ignoring all beyond the first " + i);
        }
        if (DEBUG) {
            Log.v(TAG, "convertMeteringRegionsToLegacy - " + str + " areas = " + ParameterUtils.stringFromAreaList(arrayList2));
        }
        return arrayList2;
    }

    private static void mapAeAndFlashMode(CaptureRequest captureRequest, Camera.Parameters parameters) {
        String str;
        int iIntValue = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.FLASH_MODE, 0)).intValue();
        int iIntValue2 = ((Integer) ParamsUtils.getOrDefault(captureRequest, CaptureRequest.CONTROL_AE_MODE, 1)).intValue();
        List<String> supportedFlashModes = parameters.getSupportedFlashModes();
        if (ListUtils.listContains(supportedFlashModes, "off")) {
            str = "off";
        } else {
            str = null;
        }
        if (iIntValue2 == 1) {
            if (iIntValue == 2) {
                if (ListUtils.listContains(supportedFlashModes, Camera.Parameters.FLASH_MODE_TORCH)) {
                    str = Camera.Parameters.FLASH_MODE_TORCH;
                } else {
                    Log.w(TAG, "mapAeAndFlashMode - Ignore flash.mode == TORCH;camera does not support it");
                }
            } else if (iIntValue == 1) {
                if (ListUtils.listContains(supportedFlashModes, Camera.Parameters.FLASH_MODE_ON)) {
                    str = Camera.Parameters.FLASH_MODE_ON;
                } else {
                    Log.w(TAG, "mapAeAndFlashMode - Ignore flash.mode == SINGLE;camera does not support it");
                }
            }
        } else if (iIntValue2 == 3) {
            if (ListUtils.listContains(supportedFlashModes, Camera.Parameters.FLASH_MODE_ON)) {
                str = Camera.Parameters.FLASH_MODE_ON;
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_ALWAYS_FLASH;camera does not support it");
            }
        } else if (iIntValue2 == 2) {
            if (ListUtils.listContains(supportedFlashModes, "auto")) {
                str = "auto";
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_AUTO_FLASH;camera does not support it");
            }
        } else if (iIntValue2 == 4) {
            if (ListUtils.listContains(supportedFlashModes, Camera.Parameters.FLASH_MODE_RED_EYE)) {
                str = Camera.Parameters.FLASH_MODE_RED_EYE;
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_AUTO_FLASH_REDEYE;camera does not support it");
            }
        }
        if (str != null) {
            parameters.setFlashMode(str);
        }
        if (DEBUG) {
            Log.v(TAG, "mapAeAndFlashMode - set flash.mode (api1) to " + str + ", requested (api2) " + iIntValue + ", supported (api1) " + ListUtils.listToString(supportedFlashModes));
        }
    }

    private static String convertAeAntiBandingModeToLegacy(int i) {
        switch (i) {
            case 0:
                return "off";
            case 1:
                return Camera.Parameters.ANTIBANDING_50HZ;
            case 2:
                return Camera.Parameters.ANTIBANDING_60HZ;
            case 3:
                return "auto";
            default:
                return null;
        }
    }

    private static int[] convertAeFpsRangeToLegacy(Range<Integer> range) {
        return new int[]{((Integer) range.getLower()).intValue() * 1000, ((Integer) range.getUpper()).intValue() * 1000};
    }

    private static String convertAwbModeToLegacy(int i) {
        switch (i) {
            case 1:
                return "auto";
            case 2:
                return Camera.Parameters.WHITE_BALANCE_INCANDESCENT;
            case 3:
                return Camera.Parameters.WHITE_BALANCE_FLUORESCENT;
            case 4:
                return Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT;
            case 5:
                return Camera.Parameters.WHITE_BALANCE_DAYLIGHT;
            case 6:
                return Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT;
            case 7:
                return Camera.Parameters.WHITE_BALANCE_TWILIGHT;
            case 8:
                return Camera.Parameters.WHITE_BALANCE_SHADE;
            default:
                Log.w(TAG, "convertAwbModeToLegacy - unrecognized control.awbMode" + i);
                return "auto";
        }
    }

    private static <T> T getIfSupported(CaptureRequest captureRequest, CaptureRequest.Key<T> key, T t, boolean z, T t2) {
        T t3 = (T) ParamsUtils.getOrDefault(captureRequest, key, t);
        if (!z) {
            if (!Objects.equals(t3, t2)) {
                Log.w(TAG, key.getName() + " is not supported; ignoring requested value " + t3);
                return null;
            }
            return null;
        }
        return t3;
    }
}
