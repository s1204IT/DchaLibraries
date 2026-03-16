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
import android.net.wifi.WifiEnterpriseConfig;
import android.os.BatteryStats;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class LegacyRequestMapper {
    private static final int A51_TEST_MODE = 1;
    private static final int B52_TEST_MODE = 2;
    private static final byte DEFAULT_JPEG_QUALITY = 85;
    private static final String TAG = "LegacyRequestMapper";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);

    public static void convertRequestMetadata(LegacyRequest legacyRequest) {
        String modeToSet;
        CameraCharacteristics characteristics = legacyRequest.characteristics;
        CaptureRequest request = legacyRequest.captureRequest;
        Size previewSize = legacyRequest.previewSize;
        Camera.Parameters params = legacyRequest.parameters;
        Rect activeArray = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        ParameterUtils.ZoomData zoomData = ParameterUtils.convertScalerCropRegion(activeArray, (Rect) request.get(CaptureRequest.SCALER_CROP_REGION), previewSize, params);
        if (params.isZoomSupported()) {
            params.setZoom(zoomData.zoomIndex);
        } else if (VERBOSE) {
            Log.v(TAG, "convertRequestToMetadata - zoom is not supported");
        }
        int aberrationMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, 1)).intValue();
        if (aberrationMode != 1) {
            Log.w(TAG, "convertRequestToMetadata - Ignoring unsupported colorCorrection.aberrationMode = " + aberrationMode);
        }
        boolean enableRawDump = !WifiEnterpriseConfig.ENGINE_DISABLE.equals(SystemProperties.get("persist.service.camera.rawdump", WifiEnterpriseConfig.ENGINE_DISABLE));
        int rawDumpMode = 0;
        if (enableRawDump) {
            rawDumpMode = Integer.parseInt(SystemProperties.get("persist.service.camera.rawdump", WifiEnterpriseConfig.ENGINE_DISABLE));
        }
        if (rawDumpMode == 1) {
            double[] dArr = new double[3];
            double[] adispArray = (double[]) ParamsUtils.getOrDefault(request, CaptureRequest.COLOR_CORRECTION_ISP_GAINS, new double[]{-1.0d, -1.0d, -1.0d});
            double aGain = adispArray[0];
            double dGain = adispArray[1];
            double ispGain = adispArray[2];
            if (aGain > 0.0d && params.getSceneMode().equals(Camera.Parameters.SCENE_MODE_TEST)) {
                params.setAGain(aGain);
                params.setDGain(dGain);
                params.setIspGain(ispGain);
            }
        } else if (rawDumpMode == 2) {
            double[] adispArrayDefault = new double[5];
            for (int i = 0; i < 5; i++) {
                adispArrayDefault[i] = -1.0d;
            }
            double[] dArr2 = new double[5];
            double[] adispArray2 = (double[]) ParamsUtils.getOrDefault(request, CaptureRequest.COLOR_CORRECTION_ISP_GAINS, adispArrayDefault);
            double aGain2 = adispArray2[0];
            double bGain = adispArray2[1];
            double gbGain = adispArray2[2];
            double grGain = adispArray2[3];
            double rGain = adispArray2[4];
            if (aGain2 > 0.0d && params.getSceneMode().equals(Camera.Parameters.SCENE_MODE_TEST)) {
                params.setAGain(aGain2);
                params.setBGain(bGain);
                params.setGbGain(gbGain);
                params.setGrGain(grGain);
                params.setRGain(rGain);
            }
        }
        Integer antiBandingMode = (Integer) request.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE);
        String legacyMode = antiBandingMode != null ? convertAeAntiBandingModeToLegacy(antiBandingMode.intValue()) : (String) ListUtils.listSelectFirstFrom(params.getSupportedAntibanding(), new String[]{"auto", "off", Camera.Parameters.ANTIBANDING_50HZ, Camera.Parameters.ANTIBANDING_60HZ});
        if (legacyMode != null) {
            params.setAntibanding(legacyMode);
        }
        MeteringRectangle[] aeRegions = (MeteringRectangle[]) request.get(CaptureRequest.CONTROL_AE_REGIONS);
        if (request.get(CaptureRequest.CONTROL_AWB_REGIONS) != null) {
            Log.w(TAG, "convertRequestMetadata - control.awbRegions setting is not supported, ignoring value");
        }
        int maxNumMeteringAreas = params.getMaxNumMeteringAreas();
        List<Camera.Area> meteringAreaList = convertMeteringRegionsToLegacy(activeArray, zoomData, aeRegions, maxNumMeteringAreas, "AE");
        if (maxNumMeteringAreas > 0) {
            params.setMeteringAreas(meteringAreaList);
        }
        MeteringRectangle[] afRegions = (MeteringRectangle[]) request.get(CaptureRequest.CONTROL_AF_REGIONS);
        int maxNumFocusAreas = params.getMaxNumFocusAreas();
        List<Camera.Area> focusAreaList = convertMeteringRegionsToLegacy(activeArray, zoomData, afRegions, maxNumFocusAreas, "AF");
        if (maxNumFocusAreas > 0) {
            params.setFocusAreas(focusAreaList);
        }
        Range<Integer> aeFpsRange = (Range) request.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
        if (aeFpsRange != null) {
            int[] legacyFps = convertAeFpsRangeToLegacy(aeFpsRange);
            boolean supported = false;
            Iterator<int[]> it = params.getSupportedPreviewFpsRange().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                int[] range = it.next();
                if (legacyFps[0] == range[0] && legacyFps[1] == range[1]) {
                    supported = true;
                    break;
                }
            }
            if (supported) {
                params.setPreviewFpsRange(legacyFps[0], legacyFps[1]);
            } else {
                Log.w(TAG, "Unsupported FPS range set [" + legacyFps[0] + "," + legacyFps[1] + "]");
            }
        }
        Range<Integer> compensationRange = (Range) characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        int compensation = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)).intValue();
        if (!compensationRange.contains(Integer.valueOf(compensation))) {
            Log.w(TAG, "convertRequestMetadata - control.aeExposureCompensation is out of range, ignoring value");
            compensation = 0;
        }
        params.setExposureCompensation(compensation);
        Boolean aeLock = (Boolean) getIfSupported(request, CaptureRequest.CONTROL_AE_LOCK, false, params.isAutoExposureLockSupported(), false);
        if (aeLock != null) {
            params.setAutoExposureLock(aeLock.booleanValue());
        }
        if (VERBOSE) {
            Log.v(TAG, "convertRequestToMetadata - control.aeLock set to " + aeLock);
        }
        mapAeAndFlashMode(request, params);
        int afMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_AF_MODE, 0)).intValue();
        String focusMode = LegacyMetadataMapper.convertAfModeToLegacy(afMode, params.getSupportedFocusModes());
        if (focusMode != null) {
            params.setFocusMode(focusMode);
        }
        if (VERBOSE) {
            Log.v(TAG, "convertRequestToMetadata - control.afMode " + afMode + " mapped to " + focusMode);
        }
        Integer awbMode = (Integer) getIfSupported(request, CaptureRequest.CONTROL_AWB_MODE, 1, params.getSupportedWhiteBalance() != null, 1);
        String whiteBalanceMode = null;
        if (awbMode != null) {
            whiteBalanceMode = convertAwbModeToLegacy(awbMode.intValue());
            params.setWhiteBalance(whiteBalanceMode);
        }
        if (VERBOSE) {
            Log.v(TAG, "convertRequestToMetadata - control.awbMode " + awbMode + " mapped to " + whiteBalanceMode);
        }
        Boolean awbLock = (Boolean) getIfSupported(request, CaptureRequest.CONTROL_AWB_LOCK, false, params.isAutoWhiteBalanceLockSupported(), false);
        if (awbLock != null) {
            params.setAutoWhiteBalanceLock(awbLock.booleanValue());
        }
        int captureIntent = filterSupportedCaptureIntent(((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_CAPTURE_INTENT, 1)).intValue());
        params.setRecordingHint(captureIntent == 3 || captureIntent == 4);
        Integer stabMode = (Integer) getIfSupported(request, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 0, params.isVideoStabilizationSupported(), 0);
        if (stabMode != null) {
            params.setVideoStabilization(stabMode.intValue() == 1);
        }
        boolean infinityFocusSupported = ListUtils.listContains(params.getSupportedFocusModes(), Camera.Parameters.FOCUS_MODE_INFINITY);
        Float focusDistance = (Float) getIfSupported(request, CaptureRequest.LENS_FOCUS_DISTANCE, Float.valueOf(0.0f), infinityFocusSupported, Float.valueOf(0.0f));
        if (focusDistance == null || focusDistance.floatValue() != 0.0f) {
            Log.w(TAG, "convertRequestToMetadata - Ignoring android.lens.focusDistance " + infinityFocusSupported + ", only 0.0f is supported");
        }
        Integer focusPosition = (Integer) ParamsUtils.getOrDefault(request, CaptureRequest.LENS_FOCUS_POSITION, -1);
        if (focusPosition.intValue() >= 0 && params.getFocusMode() == Camera.Parameters.FOCUS_MODE_MANUAL) {
            params.setFocusPosition(focusPosition.intValue());
        }
        if (params.getSupportedSceneModes() != null) {
            int controlMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_MODE, 1)).intValue();
            switch (controlMode) {
                case 1:
                    modeToSet = "auto";
                    break;
                case 2:
                    int sceneMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_SCENE_MODE, 0)).intValue();
                    String legacySceneMode = LegacyMetadataMapper.convertSceneModeToLegacy(sceneMode);
                    if (legacySceneMode != null) {
                        modeToSet = legacySceneMode;
                    } else {
                        modeToSet = "auto";
                        Log.w(TAG, "Skipping unknown requested scene mode: " + sceneMode);
                    }
                    break;
                default:
                    Log.w(TAG, "Control mode " + controlMode + " is unsupported, defaulting to AUTO");
                    modeToSet = "auto";
                    break;
            }
            params.setSceneMode(modeToSet);
        }
        if (params.getSupportedColorEffects() != null) {
            int effectMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_EFFECT_MODE, 0)).intValue();
            String legacyEffectMode = LegacyMetadataMapper.convertEffectModeToLegacy(effectMode);
            if (legacyEffectMode != null) {
                params.setColorEffect(legacyEffectMode);
            } else {
                params.setColorEffect("none");
                Log.w(TAG, "Skipping unknown requested effect mode: " + effectMode);
            }
            int[] effectParamDefault = {-1, -1, -1};
            int[] effectParam = (int[]) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_EFFECT_MODE_PARAM, effectParamDefault);
            if (effectParam[0] != -1) {
                String colorEffectParam = effectParam[0] + "," + effectParam[1] + "," + effectParam[2];
                params.setColorEffectParam(legacyEffectMode, colorEffectParam);
            }
        }
        if (params.getSupportedAntibanding() != null) {
            int abMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, 3)).intValue();
            String legacyabMode = LegacyMetadataMapper.convertAntiBandingMode(abMode);
            if (legacyabMode != null) {
                params.setAntibanding(legacyabMode);
            } else {
                params.setAntibanding("auto");
                Log.w(TAG, "Skipping unknown requested anti banding: " + abMode);
            }
        }
        if (params.getSupportedBurstCaptureModes() != null) {
            int abMode2 = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_BURST_CAPTURE_MODE, 0)).intValue();
            String legacyabMode2 = LegacyMetadataMapper.convertBurstCaptureMode(abMode2);
            if (legacyabMode2 != null) {
                params.setBurstCaptureMode(legacyabMode2);
                int burstNum = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_BURST_CAPTURE_NUM, 0)).intValue();
                params.setBurstCaptureNum(burstNum);
            } else {
                params.setBurstCaptureMode(Camera.Parameters.BURST_CAPTURE_MODE_OFF);
                Log.w(TAG, "Skipping unknown requested burst capture: " + abMode2);
            }
        }
        int defaultContrast = params.getContrast();
        int contrast = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_CURRENT_CONTRAST, Integer.valueOf(defaultContrast))).intValue();
        if (contrast > params.getMaxContrast()) {
            params.setContrast(params.getMaxContrast());
            Log.w(TAG, "Skipping unknown requested Contrast: " + contrast);
        } else if (contrast < params.getMinContrast()) {
            params.setContrast(params.getMinContrast());
            Log.w(TAG, "Skipping unknown requested contrast: " + contrast);
        } else {
            params.setContrast(contrast);
        }
        int defaultSaturation = params.getSaturation();
        int saturation = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_CURRENT_SATURATION, Integer.valueOf(defaultSaturation))).intValue();
        if (saturation > params.getMaxSaturation()) {
            params.setSaturation(params.getMaxSaturation());
            Log.w(TAG, "Skipping unknown requested Saturation: " + saturation);
        } else if (saturation < params.getMinSaturation()) {
            params.setSaturation(params.getMinSaturation());
            Log.w(TAG, "Skipping unknown requested Saturation: " + saturation);
        } else {
            params.setSaturation(saturation);
        }
        int defaultBrightness = params.getBrightness();
        int brightness = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_CURRENT_BRIGHTNESS, Integer.valueOf(defaultBrightness))).intValue();
        if (brightness > params.getMaxBrightness()) {
            params.setBrightness(params.getMaxBrightness());
            Log.w(TAG, "Skipping unknown requested Brightness: " + brightness);
        } else if (brightness < params.getMinBrightness()) {
            params.setBrightness(params.getMinBrightness());
            Log.w(TAG, "Skipping unknown requested Brightness: " + brightness);
        } else {
            params.setBrightness(brightness);
        }
        int defaultSharpness = params.getSharpness();
        int sharpness = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_CURRENT_SHARPNESS, Integer.valueOf(defaultSharpness))).intValue();
        if (sharpness > params.getMaxSharpness()) {
            params.setSharpness(params.getMaxSharpness());
            Log.w(TAG, "Skipping unknown requested Sharpness: " + sharpness);
        } else if (sharpness < params.getMinSharpness()) {
            params.setSharpness(params.getMinSharpness());
            Log.w(TAG, "Skipping unknown requested Sharpness: " + sharpness);
        } else {
            params.setSharpness(sharpness);
        }
        boolean isFaceBeautyOn = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.CONTROL_FACE_BEAUTY, 0)).intValue() == 1;
        params.setFaceBeautify(isFaceBeautyOn);
        int testPatternMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.SENSOR_TEST_PATTERN_MODE, 0)).intValue();
        if (testPatternMode != 0) {
            Log.w(TAG, "convertRequestToMetadata - ignoring sensor.testPatternMode " + testPatternMode + "; only OFF is supported");
        }
        if (params.getSupportedIsoModes() != null) {
            int isoMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.SENSOR_SENSITIVITY, 0)).intValue();
            String legacyIsoMode = LegacyMetadataMapper.convertSensitivityToLegacy(isoMode);
            if (legacyIsoMode != null) {
                params.setIsoMode(legacyIsoMode);
            } else {
                params.setIsoMode("auto");
                Log.w(TAG, "Skipping unknown requested iso mode: " + isoMode);
            }
        }
        int exposureMode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.SENSOR_EXPOSURE_MODE, -1)).intValue();
        if (exposureMode > 0) {
            params.setExposureMode(exposureMode);
        }
        if (!WifiEnterpriseConfig.ENGINE_DISABLE.equals(SystemProperties.get("persist.service.camera.ispparam", WifiEnterpriseConfig.ENGINE_DISABLE))) {
            int[] yuvDnsDefault = {-1, -1, -1};
            int[] iArr = new int[3];
            int[] yuvDns = (int[]) ParamsUtils.getOrDefault(request, CaptureRequest.JPEG_YUV_DNS, yuvDnsDefault);
            if (yuvDns[0] > 0) {
                params.setYDns(yuvDns[0] != 0);
                params.setUVDns(yuvDns[1] != 0);
                params.setDnsTimes(yuvDns[2]);
            }
        }
        double shutterSpeed = ((Float) ParamsUtils.getOrDefault(request, CaptureRequest.SENSOR_SHUTTER_SPEED, Float.valueOf(-0.1f))).floatValue();
        if (shutterSpeed > 0.0d && ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.SENSOR_EXPOSURE_MODE, -1)).intValue() == 4) {
            params.setShutterSpeed(shutterSpeed);
        }
        Location location = (Location) request.get(CaptureRequest.JPEG_GPS_LOCATION);
        if (location != null) {
            if (checkForCompleteGpsData(location)) {
                params.setGpsAltitude(location.getAltitude());
                params.setGpsLatitude(location.getLatitude());
                params.setGpsLongitude(location.getLongitude());
                params.setGpsProcessingMethod(location.getProvider().toUpperCase());
                params.setGpsTimestamp(location.getTime());
            } else {
                Log.w(TAG, "Incomplete GPS parameters provided in location " + location);
            }
        } else {
            params.removeGpsData();
        }
        Integer orientation = (Integer) request.get(CaptureRequest.JPEG_ORIENTATION);
        params.setRotation(((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.JPEG_ORIENTATION, Integer.valueOf(orientation == null ? 0 : orientation.intValue()))).intValue());
        params.setJpegQuality(((Byte) ParamsUtils.getOrDefault(request, CaptureRequest.JPEG_QUALITY, Byte.valueOf(DEFAULT_JPEG_QUALITY))).byteValue() & BatteryStats.HistoryItem.CMD_NULL);
        params.setJpegThumbnailQuality(((Byte) ParamsUtils.getOrDefault(request, CaptureRequest.JPEG_THUMBNAIL_QUALITY, Byte.valueOf(DEFAULT_JPEG_QUALITY))).byteValue() & BatteryStats.HistoryItem.CMD_NULL);
        List<Camera.Size> sizes = params.getSupportedJpegThumbnailSizes();
        if (sizes != null && sizes.size() > 0) {
            Size s = (Size) request.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);
            boolean invalidSize = (s == null || ParameterUtils.containsSize(sizes, s.getWidth(), s.getHeight())) ? false : true;
            if (invalidSize) {
                Log.w(TAG, "Invalid JPEG thumbnail size set " + s + ", skipping thumbnail...");
            }
            if (s == null || invalidSize) {
                params.setJpegThumbnailSize(0, 0);
            } else {
                params.setJpegThumbnailSize(s.getWidth(), s.getHeight());
            }
        }
        int mode = ((Integer) ParamsUtils.getOrDefault(request, CaptureRequest.NOISE_REDUCTION_MODE, 1)).intValue();
        if (mode != 1) {
            Log.w(TAG, "convertRequestToMetadata - Ignoring unsupported noiseReduction.mode = " + mode);
        }
    }

    private static boolean checkForCompleteGpsData(Location location) {
        return (location == null || location.getProvider() == null || location.getTime() == 0) ? false : true;
    }

    static int filterSupportedCaptureIntent(int captureIntent) {
        switch (captureIntent) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return captureIntent;
            case 5:
            case 6:
                Log.w(TAG, "Unsupported control.captureIntent value 1; default to PREVIEW");
                break;
        }
        Log.w(TAG, "Unknown control.captureIntent value 1; default to PREVIEW");
        return 1;
    }

    private static List<Camera.Area> convertMeteringRegionsToLegacy(Rect activeArray, ParameterUtils.ZoomData zoomData, MeteringRectangle[] meteringRegions, int maxNumMeteringAreas, String regionName) {
        if (meteringRegions == null || maxNumMeteringAreas <= 0) {
            if (maxNumMeteringAreas > 0) {
                return Arrays.asList(ParameterUtils.CAMERA_AREA_DEFAULT);
            }
            return null;
        }
        List<MeteringRectangle> meteringRectangleList = new ArrayList<>();
        for (MeteringRectangle rect : meteringRegions) {
            if (rect.getMeteringWeight() != 0) {
                meteringRectangleList.add(rect);
            }
        }
        if (meteringRectangleList.size() == 0) {
            Log.w(TAG, "Only received metering rectangles with weight 0.");
            return Arrays.asList(ParameterUtils.CAMERA_AREA_DEFAULT);
        }
        int countMeteringAreas = Math.min(maxNumMeteringAreas, meteringRectangleList.size());
        List<Camera.Area> meteringAreaList = new ArrayList<>(countMeteringAreas);
        for (int i = 0; i < countMeteringAreas; i++) {
            ParameterUtils.MeteringData meteringData = ParameterUtils.convertMeteringRectangleToLegacy(activeArray, meteringRectangleList.get(i), zoomData);
            meteringAreaList.add(meteringData.meteringArea);
        }
        if (maxNumMeteringAreas < meteringRectangleList.size()) {
            Log.w(TAG, "convertMeteringRegionsToLegacy - Too many requested " + regionName + " regions, ignoring all beyond the first " + maxNumMeteringAreas);
        }
        if (VERBOSE) {
            Log.v(TAG, "convertMeteringRegionsToLegacy - " + regionName + " areas = " + ParameterUtils.stringFromAreaList(meteringAreaList));
            return meteringAreaList;
        }
        return meteringAreaList;
    }

    private static void mapAeAndFlashMode(CaptureRequest r, Camera.Parameters p) {
        int flashMode = ((Integer) ParamsUtils.getOrDefault(r, CaptureRequest.FLASH_MODE, 0)).intValue();
        int aeMode = ((Integer) ParamsUtils.getOrDefault(r, CaptureRequest.CONTROL_AE_MODE, 1)).intValue();
        List<String> supportedFlashModes = p.getSupportedFlashModes();
        String flashModeSetting = null;
        if (ListUtils.listContains(supportedFlashModes, "off")) {
            flashModeSetting = "off";
        }
        if (aeMode == 1) {
            if (flashMode == 2) {
                if (ListUtils.listContains(supportedFlashModes, Camera.Parameters.FLASH_MODE_TORCH)) {
                    flashModeSetting = Camera.Parameters.FLASH_MODE_TORCH;
                } else {
                    Log.w(TAG, "mapAeAndFlashMode - Ignore flash.mode == TORCH;camera does not support it");
                }
            } else if (flashMode == 1) {
                if (ListUtils.listContains(supportedFlashModes, Camera.Parameters.FLASH_MODE_ON)) {
                    flashModeSetting = Camera.Parameters.FLASH_MODE_ON;
                } else {
                    Log.w(TAG, "mapAeAndFlashMode - Ignore flash.mode == SINGLE;camera does not support it");
                }
            }
        } else if (aeMode == 3) {
            if (ListUtils.listContains(supportedFlashModes, Camera.Parameters.FLASH_MODE_ON)) {
                flashModeSetting = Camera.Parameters.FLASH_MODE_ON;
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_ALWAYS_FLASH;camera does not support it");
            }
        } else if (aeMode == 2) {
            if (ListUtils.listContains(supportedFlashModes, "auto")) {
                flashModeSetting = "auto";
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_AUTO_FLASH;camera does not support it");
            }
        } else if (aeMode == 4) {
            if (ListUtils.listContains(supportedFlashModes, Camera.Parameters.FLASH_MODE_RED_EYE)) {
                flashModeSetting = Camera.Parameters.FLASH_MODE_RED_EYE;
            } else {
                Log.w(TAG, "mapAeAndFlashMode - Ignore control.aeMode == ON_AUTO_FLASH_REDEYE;camera does not support it");
            }
        }
        if (flashModeSetting != null) {
            p.setFlashMode(flashModeSetting);
        }
        if (VERBOSE) {
            Log.v(TAG, "mapAeAndFlashMode - set flash.mode (api1) to " + flashModeSetting + ", requested (api2) " + flashMode + ", supported (api1) " + ListUtils.listToString(supportedFlashModes));
        }
    }

    private static String convertAeAntiBandingModeToLegacy(int mode) {
        switch (mode) {
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

    private static int[] convertAeFpsRangeToLegacy(Range<Integer> fpsRange) {
        int[] legacyFps = {((Integer) fpsRange.getLower()).intValue(), ((Integer) fpsRange.getUpper()).intValue()};
        return legacyFps;
    }

    private static String convertAwbModeToLegacy(int mode) {
        switch (mode) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            case 8:
                break;
            default:
                Log.w(TAG, "convertAwbModeToLegacy - unrecognized control.awbMode" + mode);
                break;
        }
        return "auto";
    }

    private static <T> T getIfSupported(CaptureRequest captureRequest, CaptureRequest.Key<T> key, T t, boolean z, T t2) {
        T t3 = (T) ParamsUtils.getOrDefault(captureRequest, key, t);
        if (!z) {
            if (!Objects.equals(t3, t2)) {
                Log.w(TAG, key.getName() + " is not supported; ignoring requested value " + t3);
            }
            return null;
        }
        return t3;
    }
}
