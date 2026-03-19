package android.hardware.camera2.legacy;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.CameraInfo;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.impl.CameraMetadataNative;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfiguration;
import android.hardware.camera2.params.StreamConfigurationDuration;
import android.hardware.camera2.utils.ArrayUtils;
import android.hardware.camera2.utils.ListUtils;
import android.hardware.camera2.utils.ParamsUtils;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;
import com.android.internal.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class LegacyMetadataMapper {
    private static final long APPROXIMATE_CAPTURE_DELAY_MS = 200;
    private static final long APPROXIMATE_JPEG_ENCODE_TIME_MS = 600;
    private static final long APPROXIMATE_SENSOR_AREA_PX = 8388608;
    public static final int HAL_PIXEL_FORMAT_BGRA_8888 = 5;
    public static final int HAL_PIXEL_FORMAT_BLOB = 33;
    public static final int HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED = 34;
    public static final int HAL_PIXEL_FORMAT_RGBA_8888 = 1;
    private static final float LENS_INFO_MINIMUM_FOCUS_DISTANCE_FIXED_FOCUS = 0.0f;
    static final boolean LIE_ABOUT_AE_MAX_REGIONS = false;
    static final boolean LIE_ABOUT_AE_STATE = false;
    static final boolean LIE_ABOUT_AF = false;
    static final boolean LIE_ABOUT_AF_MAX_REGIONS = false;
    static final boolean LIE_ABOUT_AWB = false;
    static final boolean LIE_ABOUT_AWB_STATE = false;
    private static final long NS_PER_MS = 1000000;
    private static final float PREVIEW_ASPECT_RATIO_TOLERANCE = 0.01f;
    private static final int REQUEST_MAX_NUM_INPUT_STREAMS_COUNT = 0;
    private static final int REQUEST_MAX_NUM_OUTPUT_STREAMS_COUNT_PROC = 3;
    private static final int REQUEST_MAX_NUM_OUTPUT_STREAMS_COUNT_PROC_STALL = 1;
    private static final int REQUEST_MAX_NUM_OUTPUT_STREAMS_COUNT_RAW = 0;
    private static final int REQUEST_PIPELINE_MAX_DEPTH_HAL1 = 3;
    private static final int REQUEST_PIPELINE_MAX_DEPTH_OURS = 3;
    private static final String TAG = "LegacyMetadataMapper";
    static final int UNKNOWN_MODE = -1;
    private static final boolean DEBUG = ParameterUtils.DEBUG;
    private static final boolean DEBUG_DUMP = ParameterUtils.DEBUG_DUMP;
    private static final String[] sLegacySceneModes = {"auto", "action", Camera.Parameters.SCENE_MODE_PORTRAIT, Camera.Parameters.SCENE_MODE_LANDSCAPE, Camera.Parameters.SCENE_MODE_NIGHT, Camera.Parameters.SCENE_MODE_NIGHT_PORTRAIT, Camera.Parameters.SCENE_MODE_THEATRE, Camera.Parameters.SCENE_MODE_BEACH, Camera.Parameters.SCENE_MODE_SNOW, Camera.Parameters.SCENE_MODE_SUNSET, Camera.Parameters.SCENE_MODE_STEADYPHOTO, Camera.Parameters.SCENE_MODE_FIREWORKS, Camera.Parameters.SCENE_MODE_SPORTS, Camera.Parameters.SCENE_MODE_PARTY, Camera.Parameters.SCENE_MODE_CANDLELIGHT, Camera.Parameters.SCENE_MODE_BARCODE, Camera.Parameters.SCENE_MODE_HDR};
    private static final int[] sSceneModes = {0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18};
    private static final String[] sLegacyEffectMode = {"none", Camera.Parameters.EFFECT_MONO, Camera.Parameters.EFFECT_NEGATIVE, Camera.Parameters.EFFECT_SOLARIZE, Camera.Parameters.EFFECT_SEPIA, Camera.Parameters.EFFECT_POSTERIZE, Camera.Parameters.EFFECT_WHITEBOARD, Camera.Parameters.EFFECT_BLACKBOARD, Camera.Parameters.EFFECT_AQUA};
    private static final int[] sEffectModes = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] sAllowedTemplates = {1, 2, 3};

    public static CameraCharacteristics createCharacteristics(Camera.Parameters parameters, Camera.CameraInfo cameraInfo) {
        Preconditions.checkNotNull(parameters, "parameters must not be null");
        Preconditions.checkNotNull(cameraInfo, "info must not be null");
        String strFlatten = parameters.flatten();
        CameraInfo cameraInfo2 = new CameraInfo();
        cameraInfo2.info = cameraInfo;
        return createCharacteristics(strFlatten, cameraInfo2);
    }

    public static CameraCharacteristics createCharacteristics(String str, CameraInfo cameraInfo) {
        Preconditions.checkNotNull(str, "parameters must not be null");
        Preconditions.checkNotNull(cameraInfo, "info must not be null");
        Preconditions.checkNotNull(cameraInfo.info, "info.info must not be null");
        CameraMetadataNative cameraMetadataNative = new CameraMetadataNative();
        mapCharacteristicsFromInfo(cameraMetadataNative, cameraInfo.info);
        Camera.Parameters emptyParameters = Camera.getEmptyParameters();
        emptyParameters.unflatten(str);
        mapCharacteristicsFromParameters(cameraMetadataNative, emptyParameters);
        if (DEBUG_DUMP) {
            Log.v(TAG, "createCharacteristics metadata:");
            Log.v(TAG, "--------------------------------------------------- (start)");
            cameraMetadataNative.dumpToLog();
            Log.v(TAG, "--------------------------------------------------- (end)");
        }
        return new CameraCharacteristics(cameraMetadataNative);
    }

    private static void mapCharacteristicsFromInfo(CameraMetadataNative cameraMetadataNative, Camera.CameraInfo cameraInfo) {
        cameraMetadataNative.set(CameraCharacteristics.LENS_FACING, Integer.valueOf(cameraInfo.facing == 0 ? 1 : 0));
        cameraMetadataNative.set(CameraCharacteristics.SENSOR_ORIENTATION, Integer.valueOf(cameraInfo.orientation));
    }

    private static void mapCharacteristicsFromParameters(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        cameraMetadataNative.set(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES, new int[]{1, 2});
        mapControlAe(cameraMetadataNative, parameters);
        mapControlAf(cameraMetadataNative, parameters);
        mapControlAwb(cameraMetadataNative, parameters);
        mapControlOther(cameraMetadataNative, parameters);
        mapLens(cameraMetadataNative, parameters);
        mapFlash(cameraMetadataNative, parameters);
        mapJpeg(cameraMetadataNative, parameters);
        cameraMetadataNative.set(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES, new int[]{1, 2});
        mapScaler(cameraMetadataNative, parameters);
        mapSensor(cameraMetadataNative, parameters);
        mapStatistics(cameraMetadataNative, parameters);
        mapSync(cameraMetadataNative, parameters);
        cameraMetadataNative.set((CameraCharacteristics.Key<int>) CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, 2);
        mapScalerStreamConfigs(cameraMetadataNative, parameters);
        mapRequest(cameraMetadataNative, parameters);
    }

    private static void mapScalerStreamConfigs(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        ArrayList arrayList = new ArrayList();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        SizeAreaComparator sizeAreaComparator = new SizeAreaComparator();
        Collections.sort(supportedPreviewSizes, sizeAreaComparator);
        Camera.Size sizeFindLargestByArea = SizeAreaComparator.findLargestByArea(supportedPictureSizes);
        float f = 1.0f;
        float f2 = (sizeFindLargestByArea.width * 1.0f) / sizeFindLargestByArea.height;
        int i = 0;
        if (DEBUG) {
            Log.v(TAG, String.format("mapScalerStreamConfigs - largest JPEG area %dx%d, AR=%f", Integer.valueOf(sizeFindLargestByArea.width), Integer.valueOf(sizeFindLargestByArea.height), Float.valueOf(f2)));
        }
        while (!supportedPreviewSizes.isEmpty()) {
            int size = supportedPreviewSizes.size() - 1;
            Camera.Size size2 = supportedPreviewSizes.get(size);
            float f3 = (size2.width * f) / size2.height;
            if (Math.abs(f2 - f3) < PREVIEW_ASPECT_RATIO_TOLERANCE) {
                break;
            }
            supportedPreviewSizes.remove(size);
            if (DEBUG) {
                Log.v(TAG, String.format("mapScalerStreamConfigs - removed preview size %dx%d, AR=%f was not the same", Integer.valueOf(size2.width), Integer.valueOf(size2.height), Float.valueOf(f3)));
            }
            f = 1.0f;
        }
        if (supportedPreviewSizes.isEmpty()) {
            Log.w(TAG, "mapScalerStreamConfigs - failed to find any preview size matching JPEG aspect ratio " + f2);
            supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        }
        Collections.sort(supportedPreviewSizes, Collections.reverseOrder(sizeAreaComparator));
        appendStreamConfig(arrayList, 34, supportedPreviewSizes);
        appendStreamConfig(arrayList, 35, supportedPreviewSizes);
        Iterator<Integer> it = parameters.getSupportedPreviewFormats().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            if (ImageFormat.isPublicFormat(iIntValue) && iIntValue != 17) {
                appendStreamConfig(arrayList, iIntValue, supportedPreviewSizes);
            } else if (DEBUG) {
                Log.v(TAG, String.format("mapStreamConfigs - Skipping format %x", Integer.valueOf(iIntValue)));
            }
        }
        appendStreamConfig(arrayList, 33, parameters.getSupportedPictureSizes());
        cameraMetadataNative.set(CameraCharacteristics.SCALER_AVAILABLE_STREAM_CONFIGURATIONS, (StreamConfiguration[]) arrayList.toArray(new StreamConfiguration[0]));
        cameraMetadataNative.set(CameraCharacteristics.SCALER_AVAILABLE_MIN_FRAME_DURATIONS, new StreamConfigurationDuration[0]);
        StreamConfigurationDuration[] streamConfigurationDurationArr = new StreamConfigurationDuration[supportedPictureSizes.size()];
        long j = -1;
        for (Camera.Size size3 : supportedPictureSizes) {
            long jCalculateJpegStallDuration = calculateJpegStallDuration(size3);
            int i2 = i + 1;
            streamConfigurationDurationArr[i] = new StreamConfigurationDuration(33, size3.width, size3.height, jCalculateJpegStallDuration);
            if (j < jCalculateJpegStallDuration) {
                j = jCalculateJpegStallDuration;
            }
            i = i2;
        }
        cameraMetadataNative.set(CameraCharacteristics.SCALER_AVAILABLE_STALL_DURATIONS, streamConfigurationDurationArr);
        cameraMetadataNative.set(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION, Long.valueOf(j));
    }

    private static void mapControlAe(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        List<String> supportedAntibanding = parameters.getSupportedAntibanding();
        if (supportedAntibanding == null || supportedAntibanding.size() <= 0) {
            cameraMetadataNative.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES, new int[0]);
        } else {
            int[] iArr = new int[supportedAntibanding.size()];
            int i = 0;
            for (String str : supportedAntibanding) {
                int iConvertAntiBandingMode = convertAntiBandingMode(str);
                if (DEBUG && iConvertAntiBandingMode == -1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Antibanding mode ");
                    if (str == null) {
                        str = WifiEnterpriseConfig.EMPTY_VALUE;
                    }
                    sb.append(str);
                    sb.append(" not supported, skipping...");
                    Log.v(TAG, sb.toString());
                } else {
                    iArr[i] = iConvertAntiBandingMode;
                    i++;
                }
            }
            cameraMetadataNative.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES, Arrays.copyOf(iArr, i));
        }
        List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();
        if (supportedPreviewFpsRange == null) {
            throw new AssertionError("Supported FPS ranges cannot be null.");
        }
        int size = supportedPreviewFpsRange.size();
        if (size <= 0) {
            throw new AssertionError("At least one FPS range must be supported.");
        }
        Range[] rangeArr = new Range[size];
        int i2 = 0;
        for (int[] iArr2 : supportedPreviewFpsRange) {
            rangeArr[i2] = Range.create(Integer.valueOf((int) Math.floor(((double) iArr2[0]) / 1000.0d)), Integer.valueOf((int) Math.ceil(((double) iArr2[1]) / 1000.0d)));
            i2++;
        }
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES, rangeArr);
        int[] iArrConvertStringListToIntArray = ArrayUtils.convertStringListToIntArray(parameters.getSupportedFlashModes(), new String[]{"off", "auto", Camera.Parameters.FLASH_MODE_ON, Camera.Parameters.FLASH_MODE_RED_EYE, Camera.Parameters.FLASH_MODE_TORCH}, new int[]{1, 2, 3, 4});
        if (iArrConvertStringListToIntArray == null || iArrConvertStringListToIntArray.length == 0) {
            iArrConvertStringListToIntArray = new int[]{1};
        }
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, iArrConvertStringListToIntArray);
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE, Range.create(Integer.valueOf(parameters.getMinExposureCompensation()), Integer.valueOf(parameters.getMaxExposureCompensation())));
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP, ParamsUtils.createRational(parameters.getExposureCompensationStep()));
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE, Boolean.valueOf(parameters.isAutoExposureLockSupported()));
    }

    private static void mapControlAf(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        List listConvertStringListToIntList = ArrayUtils.convertStringListToIntList(parameters.getSupportedFocusModes(), new String[]{"auto", Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO, Camera.Parameters.FOCUS_MODE_EDOF, Camera.Parameters.FOCUS_MODE_INFINITY, Camera.Parameters.FOCUS_MODE_MACRO, Camera.Parameters.FOCUS_MODE_FIXED}, new int[]{1, 4, 3, 5, 0, 2, 0});
        if (listConvertStringListToIntList == null || listConvertStringListToIntList.size() == 0) {
            Log.w(TAG, "No AF modes supported (HAL bug); defaulting to AF_MODE_OFF only");
            listConvertStringListToIntList = new ArrayList(1);
            listConvertStringListToIntList.add(0);
        }
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, ArrayUtils.toIntArray(listConvertStringListToIntList));
        if (DEBUG) {
            Log.v(TAG, "mapControlAf - control.afAvailableModes set to " + ListUtils.listToString(listConvertStringListToIntList));
        }
    }

    private static void mapControlAwb(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        List listConvertStringListToIntList = ArrayUtils.convertStringListToIntList(parameters.getSupportedWhiteBalance(), new String[]{"auto", Camera.Parameters.WHITE_BALANCE_INCANDESCENT, Camera.Parameters.WHITE_BALANCE_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_TWILIGHT, Camera.Parameters.WHITE_BALANCE_SHADE}, new int[]{1, 2, 3, 4, 5, 6, 7, 8});
        if (listConvertStringListToIntList == null || listConvertStringListToIntList.size() == 0) {
            Log.w(TAG, "No AWB modes supported (HAL bug); defaulting to AWB_MODE_AUTO only");
            listConvertStringListToIntList = new ArrayList(1);
            listConvertStringListToIntList.add(1);
        }
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, ArrayUtils.toIntArray(listConvertStringListToIntList));
        if (DEBUG) {
            Log.v(TAG, "mapControlAwb - control.awbAvailableModes set to " + ListUtils.listToString(listConvertStringListToIntList));
        }
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE, Boolean.valueOf(parameters.isAutoWhiteBalanceLockSupported()));
    }

    private static void mapControlOther(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        int[] iArr;
        int[] iArr2;
        if (parameters.isVideoStabilizationSupported()) {
            iArr = new int[]{0, 1};
        } else {
            iArr = new int[]{0};
        }
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES, iArr);
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_MAX_REGIONS, new int[]{parameters.getMaxNumMeteringAreas(), 0, parameters.getMaxNumFocusAreas()});
        List<String> supportedColorEffects = parameters.getSupportedColorEffects();
        cameraMetadataNative.set(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS, supportedColorEffects == null ? new int[0] : ArrayUtils.convertStringListToIntArray(supportedColorEffects, sLegacyEffectMode, sEffectModes));
        int maxNumDetectedFaces = parameters.getMaxNumDetectedFaces();
        List<String> supportedSceneModes = parameters.getSupportedSceneModes();
        List<Integer> listConvertStringListToIntList = ArrayUtils.convertStringListToIntList(supportedSceneModes, sLegacySceneModes, sSceneModes);
        if (supportedSceneModes != null && supportedSceneModes.size() == 1 && supportedSceneModes.get(0).equals("auto")) {
            listConvertStringListToIntList = null;
        }
        boolean z = (listConvertStringListToIntList == null && maxNumDetectedFaces == 0) ? false : true;
        if (z) {
            if (listConvertStringListToIntList == null) {
                listConvertStringListToIntList = new ArrayList<>();
            }
            if (maxNumDetectedFaces > 0) {
                listConvertStringListToIntList.add(1);
            }
            if (listConvertStringListToIntList.contains(0)) {
                while (listConvertStringListToIntList.remove(new Integer(0))) {
                }
            }
            cameraMetadataNative.set(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES, ArrayUtils.toIntArray(listConvertStringListToIntList));
        } else {
            cameraMetadataNative.set(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES, new int[]{0});
        }
        CameraCharacteristics.Key<int[]> key = CameraCharacteristics.CONTROL_AVAILABLE_MODES;
        if (z) {
            iArr2 = new int[]{1, 2};
        } else {
            iArr2 = new int[]{1};
        }
        cameraMetadataNative.set(key, iArr2);
    }

    private static void mapLens(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        if (DEBUG) {
            Log.v(TAG, "mapLens - focus-mode='" + parameters.getFocusMode() + "'");
        }
        if (Camera.Parameters.FOCUS_MODE_FIXED.equals(parameters.getFocusMode())) {
            cameraMetadataNative.set(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE, Float.valueOf(0.0f));
            if (DEBUG) {
                Log.v(TAG, "mapLens - lens.info.minimumFocusDistance = 0");
            }
        } else if (DEBUG) {
            Log.v(TAG, "mapLens - lens.info.minimumFocusDistance is unknown");
        }
        cameraMetadataNative.set(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS, new float[]{parameters.getFocalLength()});
    }

    private static void mapFlash(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        cameraMetadataNative.set(CameraCharacteristics.FLASH_INFO_AVAILABLE, Boolean.valueOf(parameters.getSupportedFlashModes() != null ? !ListUtils.listElementsEqualTo(r2, "off") : false));
    }

    private static void mapJpeg(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        List<Camera.Size> supportedJpegThumbnailSizes = parameters.getSupportedJpegThumbnailSizes();
        if (supportedJpegThumbnailSizes != null) {
            Size[] sizeArrConvertSizeListToArray = ParameterUtils.convertSizeListToArray(supportedJpegThumbnailSizes);
            Arrays.sort(sizeArrConvertSizeListToArray, new android.hardware.camera2.utils.SizeAreaComparator());
            cameraMetadataNative.set(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES, sizeArrConvertSizeListToArray);
        }
    }

    private static void mapRequest(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        cameraMetadataNative.set(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, new int[]{0});
        ArrayList arrayList = new ArrayList(Arrays.asList(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES, CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES, CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES, CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE, CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP, CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE, CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS, CameraCharacteristics.CONTROL_AVAILABLE_MODES, CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES, CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES, CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE, CameraCharacteristics.CONTROL_MAX_REGIONS, CameraCharacteristics.FLASH_INFO_AVAILABLE, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES, CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS, CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES, CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS, CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT, CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH, CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, CameraCharacteristics.SCALER_CROPPING_TYPE, CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES, CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE, CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE, CameraCharacteristics.SENSOR_ORIENTATION, CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES, CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT, CameraCharacteristics.SYNC_MAX_LATENCY));
        if (cameraMetadataNative.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) != null) {
            arrayList.add(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        }
        cameraMetadataNative.set(CameraCharacteristics.REQUEST_AVAILABLE_CHARACTERISTICS_KEYS, getTagsForKeys((CameraCharacteristics.Key<?>[]) arrayList.toArray(new CameraCharacteristics.Key[0])));
        ArrayList arrayList2 = new ArrayList(Arrays.asList(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, CaptureRequest.CONTROL_AE_LOCK, CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AWB_LOCK, CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.FLASH_MODE, CaptureRequest.JPEG_GPS_COORDINATES, CaptureRequest.JPEG_GPS_PROCESSING_METHOD, CaptureRequest.JPEG_GPS_TIMESTAMP, CaptureRequest.JPEG_ORIENTATION, CaptureRequest.JPEG_QUALITY, CaptureRequest.JPEG_THUMBNAIL_QUALITY, CaptureRequest.JPEG_THUMBNAIL_SIZE, CaptureRequest.LENS_FOCAL_LENGTH, CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.SCALER_CROP_REGION, CaptureRequest.STATISTICS_FACE_DETECT_MODE));
        if (parameters.getMaxNumMeteringAreas() > 0) {
            arrayList2.add(CaptureRequest.CONTROL_AE_REGIONS);
        }
        if (parameters.getMaxNumFocusAreas() > 0) {
            arrayList2.add(CaptureRequest.CONTROL_AF_REGIONS);
        }
        CaptureRequest.Key[] keyArr = new CaptureRequest.Key[arrayList2.size()];
        arrayList2.toArray(keyArr);
        cameraMetadataNative.set(CameraCharacteristics.REQUEST_AVAILABLE_REQUEST_KEYS, getTagsForKeys((CaptureRequest.Key<?>[]) keyArr));
        ArrayList arrayList3 = new ArrayList(Arrays.asList(CaptureResult.COLOR_CORRECTION_ABERRATION_MODE, CaptureResult.CONTROL_AE_ANTIBANDING_MODE, CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION, CaptureResult.CONTROL_AE_LOCK, CaptureResult.CONTROL_AE_MODE, CaptureResult.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_STATE, CaptureResult.CONTROL_AWB_MODE, CaptureResult.CONTROL_AWB_LOCK, CaptureResult.CONTROL_MODE, CaptureResult.FLASH_MODE, CaptureResult.JPEG_GPS_COORDINATES, CaptureResult.JPEG_GPS_PROCESSING_METHOD, CaptureResult.JPEG_GPS_TIMESTAMP, CaptureResult.JPEG_ORIENTATION, CaptureResult.JPEG_QUALITY, CaptureResult.JPEG_THUMBNAIL_QUALITY, CaptureResult.LENS_FOCAL_LENGTH, CaptureResult.NOISE_REDUCTION_MODE, CaptureResult.REQUEST_PIPELINE_DEPTH, CaptureResult.SCALER_CROP_REGION, CaptureResult.SENSOR_TIMESTAMP, CaptureResult.STATISTICS_FACE_DETECT_MODE));
        if (parameters.getMaxNumMeteringAreas() > 0) {
            arrayList3.add(CaptureResult.CONTROL_AE_REGIONS);
        }
        if (parameters.getMaxNumFocusAreas() > 0) {
            arrayList3.add(CaptureResult.CONTROL_AF_REGIONS);
        }
        CaptureResult.Key[] keyArr2 = new CaptureResult.Key[arrayList3.size()];
        arrayList3.toArray(keyArr2);
        cameraMetadataNative.set(CameraCharacteristics.REQUEST_AVAILABLE_RESULT_KEYS, getTagsForKeys((CaptureResult.Key<?>[]) keyArr2));
        cameraMetadataNative.set(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS, new int[]{0, 3, 1});
        cameraMetadataNative.set((CameraCharacteristics.Key<int>) CameraCharacteristics.REQUEST_MAX_NUM_INPUT_STREAMS, 0);
        cameraMetadataNative.set((CameraCharacteristics.Key<int>) CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT, 1);
        cameraMetadataNative.set((CameraCharacteristics.Key<byte>) CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH, (byte) 6);
    }

    private static void mapScaler(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        cameraMetadataNative.set(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, Float.valueOf(ParameterUtils.getMaxZoomRatio(parameters)));
        cameraMetadataNative.set((CameraCharacteristics.Key<int>) CameraCharacteristics.SCALER_CROPPING_TYPE, 0);
    }

    private static void mapSensor(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        Size largestSupportedJpegSizeByArea = ParameterUtils.getLargestSupportedJpegSizeByArea(parameters);
        cameraMetadataNative.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, ParamsUtils.createRect(largestSupportedJpegSizeByArea));
        cameraMetadataNative.set(CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES, new int[]{0});
        cameraMetadataNative.set(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE, largestSupportedJpegSizeByArea);
        float focalLength = parameters.getFocalLength();
        double d = 2.0f * focalLength;
        cameraMetadataNative.set(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, new SizeF((float) Math.abs(d * Math.tan(((((double) parameters.getHorizontalViewAngle()) * 3.141592653589793d) / 180.0d) / 2.0d)), (float) Math.abs(Math.tan(((((double) parameters.getVerticalViewAngle()) * 3.141592653589793d) / 180.0d) / 2.0d) * d)));
        cameraMetadataNative.set((CameraCharacteristics.Key<int>) CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE, 0);
    }

    private static void mapStatistics(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        int[] iArr;
        if (parameters.getMaxNumDetectedFaces() > 0) {
            iArr = new int[]{0, 1};
        } else {
            iArr = new int[]{0};
        }
        cameraMetadataNative.set(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES, iArr);
        cameraMetadataNative.set(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT, Integer.valueOf(parameters.getMaxNumDetectedFaces()));
    }

    private static void mapSync(CameraMetadataNative cameraMetadataNative, Camera.Parameters parameters) {
        cameraMetadataNative.set((CameraCharacteristics.Key<int>) CameraCharacteristics.SYNC_MAX_LATENCY, -1);
    }

    private static void appendStreamConfig(ArrayList<StreamConfiguration> arrayList, int i, List<Camera.Size> list) {
        for (Camera.Size size : list) {
            arrayList.add(new StreamConfiguration(i, size.width, size.height, false));
        }
    }

    static int convertSceneModeFromLegacy(String str) {
        if (str == null) {
            return 0;
        }
        int arrayIndex = ArrayUtils.getArrayIndex(sLegacySceneModes, str);
        if (arrayIndex < 0) {
            return -1;
        }
        return sSceneModes[arrayIndex];
    }

    static String convertSceneModeToLegacy(int i) {
        if (i == 1) {
            return "auto";
        }
        int arrayIndex = ArrayUtils.getArrayIndex(sSceneModes, i);
        if (arrayIndex < 0) {
            return null;
        }
        return sLegacySceneModes[arrayIndex];
    }

    static int convertEffectModeFromLegacy(String str) {
        if (str == null) {
            return 0;
        }
        int arrayIndex = ArrayUtils.getArrayIndex(sLegacyEffectMode, str);
        if (arrayIndex < 0) {
            return -1;
        }
        return sEffectModes[arrayIndex];
    }

    static String convertEffectModeToLegacy(int i) {
        int arrayIndex = ArrayUtils.getArrayIndex(sEffectModes, i);
        if (arrayIndex < 0) {
            return null;
        }
        return sLegacyEffectMode[arrayIndex];
    }

    private static int convertAntiBandingMode(String str) {
        byte b;
        if (str == null) {
            return -1;
        }
        int iHashCode = str.hashCode();
        if (iHashCode != 109935) {
            if (iHashCode != 1628397) {
                if (iHashCode != 1658188) {
                    b = (iHashCode == 3005871 && str.equals("auto")) ? (byte) 3 : (byte) -1;
                } else if (str.equals(Camera.Parameters.ANTIBANDING_60HZ)) {
                    b = 2;
                }
            } else if (str.equals(Camera.Parameters.ANTIBANDING_50HZ)) {
                b = 1;
            }
        } else if (str.equals("off")) {
            b = 0;
        }
        switch (b) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.w(TAG, "convertAntiBandingMode - Unknown antibanding mode " + str);
                break;
        }
        return -1;
    }

    static int convertAntiBandingModeOrDefault(String str) {
        int iConvertAntiBandingMode = convertAntiBandingMode(str);
        if (iConvertAntiBandingMode == -1) {
            return 0;
        }
        return iConvertAntiBandingMode;
    }

    private static int[] convertAeFpsRangeToLegacy(Range<Integer> range) {
        return new int[]{((Integer) range.getLower()).intValue(), ((Integer) range.getUpper()).intValue()};
    }

    private static long calculateJpegStallDuration(Camera.Size size) {
        return 200000000 + (((long) size.width) * ((long) size.height) * 71);
    }

    public static void convertRequestMetadata(LegacyRequest legacyRequest) {
        LegacyRequestMapper.convertRequestMetadata(legacyRequest);
    }

    public static CameraMetadataNative createRequestTemplate(CameraCharacteristics cameraCharacteristics, int i) {
        int i2;
        if (!ArrayUtils.contains(sAllowedTemplates, i)) {
            throw new IllegalArgumentException("templateId out of range");
        }
        CameraMetadataNative cameraMetadataNative = new CameraMetadataNative();
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AWB_MODE, 1);
        int i3 = 3;
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, 3);
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
        cameraMetadataNative.set((CaptureRequest.Key<boolean>) CaptureRequest.CONTROL_AE_LOCK, false);
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, 0);
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AF_TRIGGER, 0);
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AWB_MODE, 1);
        cameraMetadataNative.set((CaptureRequest.Key<boolean>) CaptureRequest.CONTROL_AWB_LOCK, false);
        Rect rect = (Rect) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        MeteringRectangle[] meteringRectangleArr = {new MeteringRectangle(0, 0, rect.width() - 1, rect.height() - 1, 0)};
        cameraMetadataNative.set(CaptureRequest.CONTROL_AE_REGIONS, meteringRectangleArr);
        cameraMetadataNative.set(CaptureRequest.CONTROL_AWB_REGIONS, meteringRectangleArr);
        cameraMetadataNative.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangleArr);
        switch (i) {
            case 1:
                i2 = 1;
                break;
            case 2:
                i2 = 2;
                break;
            case 3:
                i2 = 3;
                break;
            default:
                throw new AssertionError("Impossible; keep in sync with sAllowedTemplates");
        }
        cameraMetadataNative.set(CaptureRequest.CONTROL_CAPTURE_INTENT, Integer.valueOf(i2));
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AE_MODE, 1);
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_MODE, 1);
        Float f = (Float) cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        if (f == null || f.floatValue() != 0.0f) {
            if (i == 3 || i == 4) {
                if (!ArrayUtils.contains((int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES), 3)) {
                    i3 = 1;
                }
            } else if ((i == 1 || i == 2) && ArrayUtils.contains((int[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES), 4)) {
                i3 = 4;
            }
        } else {
            i3 = 0;
        }
        if (DEBUG) {
            Log.v(TAG, "createRequestTemplate (templateId=" + i + "), afMode=" + i3 + ", minimumFocusDistance=" + f);
        }
        cameraMetadataNative.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(i3));
        Range[] rangeArr = (Range[]) cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Range range = rangeArr[0];
        for (Range range2 : rangeArr) {
            if (((Integer) range.getUpper()).intValue() < ((Integer) range2.getUpper()).intValue() || (range.getUpper() == range2.getUpper() && ((Integer) range.getLower()).intValue() < ((Integer) range2.getLower()).intValue())) {
                range = range2;
            }
        }
        cameraMetadataNative.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range);
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_SCENE_MODE, 0);
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.STATISTICS_FACE_DETECT_MODE, 0);
        cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.FLASH_MODE, 0);
        if (i == 2) {
            cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.NOISE_REDUCTION_MODE, 2);
        } else {
            cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.NOISE_REDUCTION_MODE, 1);
        }
        if (i == 2) {
            cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, 2);
        } else {
            cameraMetadataNative.set((CaptureRequest.Key<int>) CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, 1);
        }
        cameraMetadataNative.set(CaptureRequest.LENS_FOCAL_LENGTH, Float.valueOf(((float[]) cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS))[0]));
        Size[] sizeArr = (Size[]) cameraCharacteristics.get(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES);
        cameraMetadataNative.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, sizeArr.length > 1 ? sizeArr[1] : sizeArr[0]);
        return cameraMetadataNative;
    }

    private static int[] getTagsForKeys(CameraCharacteristics.Key<?>[] keyArr) {
        int[] iArr = new int[keyArr.length];
        for (int i = 0; i < keyArr.length; i++) {
            iArr[i] = keyArr[i].getNativeKey().getTag();
        }
        return iArr;
    }

    private static int[] getTagsForKeys(CaptureRequest.Key<?>[] keyArr) {
        int[] iArr = new int[keyArr.length];
        for (int i = 0; i < keyArr.length; i++) {
            iArr[i] = keyArr[i].getNativeKey().getTag();
        }
        return iArr;
    }

    private static int[] getTagsForKeys(CaptureResult.Key<?>[] keyArr) {
        int[] iArr = new int[keyArr.length];
        for (int i = 0; i < keyArr.length; i++) {
            iArr[i] = keyArr[i].getNativeKey().getTag();
        }
        return iArr;
    }

    static String convertAfModeToLegacy(int i, List<String> list) {
        String str = null;
        if (list == null || list.isEmpty()) {
            Log.w(TAG, "No focus modes supported; API1 bug");
            return null;
        }
        switch (i) {
            case 0:
                if (list.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                    str = Camera.Parameters.FOCUS_MODE_FIXED;
                } else {
                    str = Camera.Parameters.FOCUS_MODE_INFINITY;
                }
                break;
            case 1:
                str = "auto";
                break;
            case 2:
                str = Camera.Parameters.FOCUS_MODE_MACRO;
                break;
            case 3:
                str = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
                break;
            case 4:
                str = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
                break;
            case 5:
                str = Camera.Parameters.FOCUS_MODE_EDOF;
                break;
        }
        if (!list.contains(str)) {
            String str2 = list.get(0);
            Log.w(TAG, String.format("convertAfModeToLegacy - ignoring unsupported mode %d, defaulting to %s", Integer.valueOf(i), str2));
            return str2;
        }
        return str;
    }
}
