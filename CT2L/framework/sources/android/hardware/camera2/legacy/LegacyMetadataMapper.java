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
    static final int UNKNOWN_MODE = -1;
    private static final String TAG = "LegacyMetadataMapper";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static final String[] sLegacySceneModes = {"auto", Camera.Parameters.SCENE_MODE_ACTION, Camera.Parameters.SCENE_MODE_PORTRAIT, Camera.Parameters.SCENE_MODE_LANDSCAPE, Camera.Parameters.SCENE_MODE_NIGHT, Camera.Parameters.SCENE_MODE_NIGHT_PORTRAIT, Camera.Parameters.SCENE_MODE_THEATRE, Camera.Parameters.SCENE_MODE_BEACH, Camera.Parameters.SCENE_MODE_SNOW, Camera.Parameters.SCENE_MODE_SUNSET, Camera.Parameters.SCENE_MODE_STEADYPHOTO, Camera.Parameters.SCENE_MODE_FIREWORKS, Camera.Parameters.SCENE_MODE_SPORTS, Camera.Parameters.SCENE_MODE_PARTY, Camera.Parameters.SCENE_MODE_CANDLELIGHT, Camera.Parameters.SCENE_MODE_BARCODE, Camera.Parameters.SCENE_MODE_HDR, Camera.Parameters.SCENE_MODE_DETECT, Camera.Parameters.SCENE_MODE_BESTSHOT, Camera.Parameters.SCENE_MODE_TEST};
    private static final int[] sSceneModes = {0, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 19, 21, 22};
    private static final String[] sLegacyEffectMode = {"none", Camera.Parameters.EFFECT_MONO, Camera.Parameters.EFFECT_NEGATIVE, Camera.Parameters.EFFECT_SOLARIZE, Camera.Parameters.EFFECT_SEPIA, Camera.Parameters.EFFECT_POSTERIZE, Camera.Parameters.EFFECT_WHITEBOARD, Camera.Parameters.EFFECT_BLACKBOARD, Camera.Parameters.EFFECT_AQUA, Camera.Parameters.EFFECT_OLDMOVIE, Camera.Parameters.EFFECT_TOONSHADING, Camera.Parameters.EFFECT_PENCILSKETCH, Camera.Parameters.EFFECT_GLOW, Camera.Parameters.EFFECT_TWIST, Camera.Parameters.EFFECT_VIVID, Camera.Parameters.EFFECT_FRAME, Camera.Parameters.EFFECT_SUNSHINE};
    private static final int[] sEffectModes = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static final int[] sAllowedTemplates = {1, 2, 3};

    public static CameraCharacteristics createCharacteristics(Camera.Parameters parameters, Camera.CameraInfo info) {
        Preconditions.checkNotNull(parameters, "parameters must not be null");
        Preconditions.checkNotNull(info, "info must not be null");
        String paramStr = parameters.flatten();
        CameraInfo outerInfo = new CameraInfo();
        outerInfo.info = info;
        return createCharacteristics(paramStr, outerInfo);
    }

    public static CameraCharacteristics createCharacteristics(String parameters, CameraInfo info) {
        Preconditions.checkNotNull(parameters, "parameters must not be null");
        Preconditions.checkNotNull(info, "info must not be null");
        Preconditions.checkNotNull(info.info, "info.info must not be null");
        CameraMetadataNative m = new CameraMetadataNative();
        mapCharacteristicsFromInfo(m, info.info);
        Camera.Parameters params = Camera.getEmptyParameters();
        params.unflatten(parameters);
        mapCharacteristicsFromParameters(m, params);
        if (VERBOSE) {
            Log.v(TAG, "createCharacteristics metadata:");
            Log.v(TAG, "--------------------------------------------------- (start)");
            m.dumpToLog();
            Log.v(TAG, "--------------------------------------------------- (end)");
        }
        return new CameraCharacteristics(m);
    }

    private static void mapCharacteristicsFromInfo(CameraMetadataNative m, Camera.CameraInfo i) {
        m.set(CameraCharacteristics.LENS_FACING, Integer.valueOf(i.facing == 0 ? 1 : 0));
        m.set(CameraCharacteristics.SENSOR_ORIENTATION, Integer.valueOf(i.orientation));
    }

    private static void mapCharacteristicsFromParameters(CameraMetadataNative m, Camera.Parameters p) {
        m.set(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES, new int[]{1});
        mapControlAe(m, p);
        mapControlAf(m, p);
        mapControlAwb(m, p);
        mapControlOther(m, p);
        mapLens(m, p);
        mapFlash(m, p);
        mapJpeg(m, p);
        m.set(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES, new int[]{1});
        mapScaler(m, p);
        mapSensor(m, p);
        mapStatistics(m, p);
        mapSync(m, p);
        m.set((CameraCharacteristics.Key<int>) CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, 2);
        mapScalerStreamConfigs(m, p);
        mapRequest(m, p);
    }

    private static void mapScalerStreamConfigs(CameraMetadataNative m, Camera.Parameters p) {
        ArrayList<StreamConfiguration> availableStreamConfigs = new ArrayList<>();
        List<Camera.Size> previewSizes = p.getSupportedPreviewSizes();
        List<Camera.Size> jpegSizes = p.getSupportedPictureSizes();
        SizeAreaComparator areaComparator = new SizeAreaComparator();
        Collections.sort(previewSizes, areaComparator);
        Camera.Size maxJpegSize = SizeAreaComparator.findLargestByArea(jpegSizes);
        float jpegAspectRatio = (maxJpegSize.width * 1.0f) / maxJpegSize.height;
        if (VERBOSE) {
            Log.v(TAG, String.format("mapScalerStreamConfigs - largest JPEG area %dx%d, AR=%f", Integer.valueOf(maxJpegSize.width), Integer.valueOf(maxJpegSize.height), Float.valueOf(jpegAspectRatio)));
        }
        if (0 != 0) {
            while (!previewSizes.isEmpty()) {
                int index = previewSizes.size() - 1;
                Camera.Size size = previewSizes.get(index);
                float previewAspectRatio = (size.width * 1.0f) / size.height;
                if (Math.abs(jpegAspectRatio - previewAspectRatio) < PREVIEW_ASPECT_RATIO_TOLERANCE) {
                    break;
                }
                previewSizes.remove(index);
                if (VERBOSE) {
                    Log.v(TAG, String.format("mapScalerStreamConfigs - removed preview size %dx%d, AR=%f was not the same", Integer.valueOf(size.width), Integer.valueOf(size.height), Float.valueOf(previewAspectRatio)));
                }
            }
        }
        if (previewSizes.isEmpty()) {
            Log.w(TAG, "mapScalerStreamConfigs - failed to find any preview size matching JPEG aspect ratio " + jpegAspectRatio);
            previewSizes = p.getSupportedPreviewSizes();
        }
        Collections.sort(previewSizes, Collections.reverseOrder(areaComparator));
        appendStreamConfig(availableStreamConfigs, 34, previewSizes);
        appendStreamConfig(availableStreamConfigs, 35, previewSizes);
        Iterator<Integer> it = p.getSupportedPreviewFormats().iterator();
        while (it.hasNext()) {
            int format = it.next().intValue();
            if (ImageFormat.isPublicFormat(format) && format != 17) {
                appendStreamConfig(availableStreamConfigs, format, previewSizes);
            } else if (VERBOSE) {
                Log.v(TAG, String.format("mapStreamConfigs - Skipping format %x", Integer.valueOf(format)));
            }
        }
        appendStreamConfig(availableStreamConfigs, 33, p.getSupportedPictureSizes());
        m.set(CameraCharacteristics.SCALER_AVAILABLE_STREAM_CONFIGURATIONS, availableStreamConfigs.toArray(new StreamConfiguration[0]));
        m.set(CameraCharacteristics.SCALER_AVAILABLE_MIN_FRAME_DURATIONS, new StreamConfigurationDuration[0]);
        StreamConfigurationDuration[] jpegStalls = new StreamConfigurationDuration[jpegSizes.size()];
        int i = 0;
        long longestStallDuration = -1;
        for (Camera.Size s : jpegSizes) {
            long stallDuration = calculateJpegStallDuration(s);
            int i2 = i + 1;
            jpegStalls[i] = new StreamConfigurationDuration(33, s.width, s.height, stallDuration);
            if (longestStallDuration < stallDuration) {
                longestStallDuration = stallDuration;
            }
            i = i2;
        }
        m.set(CameraCharacteristics.SCALER_AVAILABLE_STALL_DURATIONS, jpegStalls);
        m.set(CameraCharacteristics.SENSOR_INFO_MAX_FRAME_DURATION, Long.valueOf(longestStallDuration));
    }

    private static void mapControlAe(CameraMetadataNative m, Camera.Parameters p) {
        List<String> antiBandingModes = p.getSupportedAntibanding();
        if (antiBandingModes != null && antiBandingModes.size() > 0) {
            int[] modes = new int[antiBandingModes.size()];
            int j = 0;
            for (String mode : antiBandingModes) {
                int convertedMode = convertAntiBandingMode(mode);
                if (VERBOSE && convertedMode == -1) {
                    StringBuilder sbAppend = new StringBuilder().append("Antibanding mode ");
                    if (mode == null) {
                        mode = WifiEnterpriseConfig.EMPTY_VALUE;
                    }
                    Log.v(TAG, sbAppend.append(mode).append(" not supported, skipping...").toString());
                } else {
                    modes[j] = convertedMode;
                    j++;
                }
            }
            m.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES, Arrays.copyOf(modes, j));
        } else {
            m.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES, new int[0]);
        }
        List<int[]> fpsRanges = p.getSupportedPreviewFpsRange();
        if (fpsRanges == null) {
            throw new AssertionError("Supported FPS ranges cannot be null.");
        }
        int rangesSize = fpsRanges.size();
        if (rangesSize <= 0) {
            throw new AssertionError("At least one FPS range must be supported.");
        }
        Range<Integer>[] ranges = new Range[rangesSize];
        int i = 0;
        for (int[] r : fpsRanges) {
            ranges[i] = Range.create(Integer.valueOf(r[0]), Integer.valueOf(r[1]));
            i++;
        }
        m.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES, ranges);
        List<String> flashModes = p.getSupportedFlashModes();
        String[] flashModeStrings = {"off", "auto", Camera.Parameters.FLASH_MODE_ON, Camera.Parameters.FLASH_MODE_RED_EYE, Camera.Parameters.FLASH_MODE_TORCH};
        int[] flashModeInts = {1, 2, 3, 4};
        int[] aeAvail = ArrayUtils.convertStringListToIntArray(flashModes, flashModeStrings, flashModeInts);
        if (aeAvail == null || aeAvail.length == 0) {
            aeAvail = new int[]{1};
        }
        m.set(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, aeAvail);
        int min = p.getMinExposureCompensation();
        int max = p.getMaxExposureCompensation();
        m.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE, Range.create(Integer.valueOf(min), Integer.valueOf(max)));
        float step = p.getExposureCompensationStep();
        m.set(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP, ParamsUtils.createRational(step));
    }

    private static void mapControlAf(CameraMetadataNative m, Camera.Parameters p) {
        List<String> focusModes = p.getSupportedFocusModes();
        String[] focusModeStrings = {"auto", Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO, Camera.Parameters.FOCUS_MODE_EDOF, Camera.Parameters.FOCUS_MODE_INFINITY, "macro", Camera.Parameters.FOCUS_MODE_MANUAL, Camera.Parameters.FOCUS_MODE_FIXED};
        int[] focusModeInts = {1, 4, 3, 5, 0, 2, 6, 0};
        List<Integer> afAvail = ArrayUtils.convertStringListToIntList(focusModes, focusModeStrings, focusModeInts);
        if (afAvail == null || afAvail.size() == 0) {
            Log.w(TAG, "No AF modes supported (HAL bug); defaulting to AF_MODE_OFF only");
            afAvail = new ArrayList<>(1);
            afAvail.add(0);
        }
        m.set(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, ArrayUtils.toIntArray(afAvail));
        if (VERBOSE) {
            Log.v(TAG, "mapControlAf - control.afAvailableModes set to " + ListUtils.listToString(afAvail));
        }
    }

    private static void mapControlAwb(CameraMetadataNative m, Camera.Parameters p) {
        List<String> wbModes = p.getSupportedWhiteBalance();
        String[] wbModeStrings = {"auto", Camera.Parameters.WHITE_BALANCE_INCANDESCENT, Camera.Parameters.WHITE_BALANCE_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT, Camera.Parameters.WHITE_BALANCE_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT, Camera.Parameters.WHITE_BALANCE_TWILIGHT, Camera.Parameters.WHITE_BALANCE_SHADE};
        int[] wbModeInts = {1, 2, 3, 4, 5, 6, 7, 8};
        List<Integer> awbAvail = ArrayUtils.convertStringListToIntList(wbModes, wbModeStrings, wbModeInts);
        if (awbAvail == null || awbAvail.size() == 0) {
            Log.w(TAG, "No AWB modes supported (HAL bug); defaulting to AWB_MODE_AUTO only");
            awbAvail = new ArrayList<>(1);
            awbAvail.add(1);
        }
        m.set(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, ArrayUtils.toIntArray(awbAvail));
        if (VERBOSE) {
            Log.v(TAG, "mapControlAwb - control.awbAvailableModes set to " + ListUtils.listToString(awbAvail));
        }
    }

    private static void mapControlOther(CameraMetadataNative m, Camera.Parameters p) {
        int[] stabModes = p.isVideoStabilizationSupported() ? new int[]{0, 1} : new int[]{0};
        m.set(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES, stabModes);
        int[] maxRegions = {p.getMaxNumMeteringAreas(), 0, p.getMaxNumFocusAreas()};
        m.set(CameraCharacteristics.CONTROL_MAX_REGIONS, maxRegions);
        List<String> effectModes = p.getSupportedColorEffects();
        int[] supportedEffectModes = effectModes == null ? new int[0] : ArrayUtils.convertStringListToIntArray(effectModes, sLegacyEffectMode, sEffectModes);
        m.set(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS, supportedEffectModes);
        List<String> isoModes = p.getSupportedIsoModes();
        if (isoModes != null && isoModes.size() > 0) {
            int[] modes = new int[isoModes.size()];
            int j = 0;
            for (String mode : isoModes) {
                int convertedMode = convertIsoMode(mode);
                if (convertedMode == -1) {
                    StringBuilder sbAppend = new StringBuilder().append("Iso mode ");
                    if (mode == null) {
                        mode = WifiEnterpriseConfig.EMPTY_VALUE;
                    }
                    Log.w(TAG, sbAppend.append(mode).append(" not supported, skipping...").toString());
                } else {
                    modes[j] = convertedMode;
                    j++;
                }
            }
            m.set(CameraCharacteristics.CONTROL_AVAILABLE_ISO_MODES, Arrays.copyOf(modes, j));
        } else {
            m.set(CameraCharacteristics.CONTROL_AVAILABLE_ISO_MODES, new int[0]);
        }
        List<String> sceneModes = p.getSupportedSceneModes();
        List<Integer> supportedSceneModes = ArrayUtils.convertStringListToIntList(sceneModes, sLegacySceneModes, sSceneModes);
        if (supportedSceneModes == null) {
            supportedSceneModes = new ArrayList<>();
            supportedSceneModes.add(0);
        }
        if (p.getMaxNumDetectedFaces() > 0) {
            supportedSceneModes.add(1);
        }
        m.set(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES, ArrayUtils.toIntArray(supportedSceneModes));
        int maxContrast = p.getMaxContrast();
        m.set(CameraCharacteristics.CONTROL_MAX_CONTRAST, Integer.valueOf(maxContrast));
        int minContrast = p.getMinContrast();
        m.set(CameraCharacteristics.CONTROL_MIN_CONTRAST, Integer.valueOf(minContrast));
        int maxSaturation = p.getMaxSaturation();
        m.set(CameraCharacteristics.CONTROL_MAX_SATURATION, Integer.valueOf(maxSaturation));
        int minSaturation = p.getMinSaturation();
        m.set(CameraCharacteristics.CONTROL_MIN_SATURATION, Integer.valueOf(minSaturation));
        int maxBrightness = p.getMaxBrightness();
        m.set(CameraCharacteristics.CONTROL_MAX_BRIGHTNESS, Integer.valueOf(maxBrightness));
        int minBrightness = p.getMinBrightness();
        m.set(CameraCharacteristics.CONTROL_MIN_BRIGHTNESS, Integer.valueOf(minBrightness));
        int maxSharpness = p.getMaxSharpness();
        m.set(CameraCharacteristics.CONTROL_MAX_SHARPNESS, Integer.valueOf(maxSharpness));
        int minSharpness = p.getMinSharpness();
        m.set(CameraCharacteristics.CONTROL_MIN_SHARPNESS, Integer.valueOf(minSharpness));
    }

    private static void mapLens(CameraMetadataNative m, Camera.Parameters p) {
        if (VERBOSE) {
            Log.v(TAG, "mapLens - focus-mode='" + p.getFocusMode() + "'");
        }
        if (Camera.Parameters.FOCUS_MODE_FIXED.equals(p.getFocusMode())) {
            m.set(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE, Float.valueOf(0.0f));
            if (VERBOSE) {
                Log.v(TAG, "mapLens - lens.info.minimumFocusDistance = 0");
            }
        } else if (VERBOSE) {
            Log.v(TAG, "mapLens - lens.info.minimumFocusDistance is unknown");
        }
        float[] focalLengths = {p.getFocalLength()};
        m.set(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS, focalLengths);
    }

    private static void mapFlash(CameraMetadataNative m, Camera.Parameters p) {
        boolean flashAvailable = false;
        List<String> supportedFlashModes = p.getSupportedFlashModes();
        if (supportedFlashModes != null) {
            flashAvailable = !ListUtils.listElementsEqualTo(supportedFlashModes, "off");
        }
        m.set(CameraCharacteristics.FLASH_INFO_AVAILABLE, Boolean.valueOf(flashAvailable));
    }

    private static void mapJpeg(CameraMetadataNative m, Camera.Parameters p) {
        List<Camera.Size> thumbnailSizes = p.getSupportedJpegThumbnailSizes();
        if (thumbnailSizes != null) {
            Size[] sizes = ParameterUtils.convertSizeListToArray(thumbnailSizes);
            Arrays.sort(sizes, new android.hardware.camera2.utils.SizeAreaComparator());
            m.set(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES, sizes);
        }
    }

    private static void mapRequest(CameraMetadataNative m, Camera.Parameters p) {
        int[] capabilities = {0};
        m.set(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, capabilities);
        List<CameraCharacteristics.Key<?>> characteristicsKeys = new ArrayList<>(Arrays.asList(CameraCharacteristics.COLOR_CORRECTION_AVAILABLE_ABERRATION_MODES, CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES, CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES, CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES, CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE, CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP, CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES, CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS, CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES, CameraCharacteristics.CONTROL_AVAILABLE_ISO_MODES, CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES, CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES, CameraCharacteristics.CONTROL_MAX_REGIONS, CameraCharacteristics.FLASH_INFO_AVAILABLE, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL, CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES, CameraCharacteristics.LENS_FACING, CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS, CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES, CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES, CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS, CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT, CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH, CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, CameraCharacteristics.SCALER_CROPPING_TYPE, CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES, CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE, CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE, CameraCharacteristics.SENSOR_ORIENTATION, CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES, CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT, CameraCharacteristics.SYNC_MAX_LATENCY));
        if (m.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) != null) {
            characteristicsKeys.add(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        }
        m.set(CameraCharacteristics.REQUEST_AVAILABLE_CHARACTERISTICS_KEYS, getTagsForKeys((CameraCharacteristics.Key<?>[]) characteristicsKeys.toArray(new CameraCharacteristics.Key[0])));
        ArrayList<CaptureRequest.Key<?>> availableKeys = new ArrayList<>(Arrays.asList(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE, CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, CaptureRequest.CONTROL_AE_LOCK, CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AWB_LOCK, CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.FLASH_MODE, CaptureRequest.JPEG_GPS_COORDINATES, CaptureRequest.JPEG_GPS_PROCESSING_METHOD, CaptureRequest.JPEG_GPS_TIMESTAMP, CaptureRequest.JPEG_ORIENTATION, CaptureRequest.JPEG_QUALITY, CaptureRequest.JPEG_THUMBNAIL_QUALITY, CaptureRequest.JPEG_THUMBNAIL_SIZE, CaptureRequest.LENS_FOCAL_LENGTH, CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.SCALER_CROP_REGION, CaptureRequest.STATISTICS_FACE_DETECT_MODE));
        if (p.getMaxNumMeteringAreas() > 0) {
            availableKeys.add(CaptureRequest.CONTROL_AE_REGIONS);
        }
        if (p.getMaxNumFocusAreas() > 0) {
            availableKeys.add(CaptureRequest.CONTROL_AF_REGIONS);
        }
        CaptureRequest.Key<?>[] availableRequestKeys = new CaptureRequest.Key[availableKeys.size()];
        availableKeys.toArray(availableRequestKeys);
        m.set(CameraCharacteristics.REQUEST_AVAILABLE_REQUEST_KEYS, getTagsForKeys(availableRequestKeys));
        List<CaptureResult.Key<?>> availableKeys2 = new ArrayList<>(Arrays.asList(CaptureResult.COLOR_CORRECTION_ABERRATION_MODE, CaptureResult.CONTROL_AE_ANTIBANDING_MODE, CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION, CaptureResult.CONTROL_AE_LOCK, CaptureResult.CONTROL_AE_MODE, CaptureResult.CONTROL_AF_MODE, CaptureResult.CONTROL_AF_STATE, CaptureResult.CONTROL_AWB_MODE, CaptureResult.CONTROL_AWB_LOCK, CaptureResult.CONTROL_MODE, CaptureResult.FLASH_MODE, CaptureResult.JPEG_GPS_COORDINATES, CaptureResult.JPEG_GPS_PROCESSING_METHOD, CaptureResult.JPEG_GPS_TIMESTAMP, CaptureResult.JPEG_ORIENTATION, CaptureResult.JPEG_QUALITY, CaptureResult.JPEG_THUMBNAIL_QUALITY, CaptureResult.LENS_FOCAL_LENGTH, CaptureResult.NOISE_REDUCTION_MODE, CaptureResult.REQUEST_PIPELINE_DEPTH, CaptureResult.SCALER_CROP_REGION, CaptureResult.SENSOR_TIMESTAMP, CaptureResult.STATISTICS_FACE_DETECT_MODE));
        if (p.getMaxNumMeteringAreas() > 0) {
            availableKeys2.add(CaptureResult.CONTROL_AE_REGIONS);
        }
        if (p.getMaxNumFocusAreas() > 0) {
            availableKeys2.add(CaptureResult.CONTROL_AF_REGIONS);
        }
        CaptureResult.Key<?>[] availableResultKeys = new CaptureResult.Key[availableKeys2.size()];
        availableKeys2.toArray(availableResultKeys);
        m.set(CameraCharacteristics.REQUEST_AVAILABLE_RESULT_KEYS, getTagsForKeys(availableResultKeys));
        int[] outputStreams = {0, 3, 1};
        m.set(CameraCharacteristics.REQUEST_MAX_NUM_OUTPUT_STREAMS, outputStreams);
        m.set((CameraCharacteristics.Key<int>) CameraCharacteristics.REQUEST_MAX_NUM_INPUT_STREAMS, 0);
        m.set((CameraCharacteristics.Key<int>) CameraCharacteristics.REQUEST_PARTIAL_RESULT_COUNT, 1);
        m.set((CameraCharacteristics.Key<byte>) CameraCharacteristics.REQUEST_PIPELINE_MAX_DEPTH, (byte) 6);
    }

    private static void mapScaler(CameraMetadataNative m, Camera.Parameters p) {
        m.set(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM, Float.valueOf(ParameterUtils.getMaxZoomRatio(p)));
        m.set((CameraCharacteristics.Key<int>) CameraCharacteristics.SCALER_CROPPING_TYPE, 0);
    }

    private static void mapSensor(CameraMetadataNative m, Camera.Parameters p) {
        Size largestJpegSize = ParameterUtils.getLargestSupportedJpegSizeByArea(p);
        Rect activeArrayRect = ParamsUtils.createRect(largestJpegSize);
        m.set(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE, activeArrayRect);
        m.set(CameraCharacteristics.SENSOR_AVAILABLE_TEST_PATTERN_MODES, new int[]{0});
        m.set(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE, largestJpegSize);
        float focalLength = p.getFocalLength();
        double angleHor = (((double) p.getHorizontalViewAngle()) * 3.141592653589793d) / 180.0d;
        double angleVer = (((double) p.getVerticalViewAngle()) * 3.141592653589793d) / 180.0d;
        float height = (float) Math.abs(((double) (2.0f * focalLength)) * Math.tan(angleVer / 2.0d));
        float width = (float) Math.abs(((double) (2.0f * focalLength)) * Math.tan(angleHor / 2.0d));
        m.set(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE, new SizeF(width, height));
        m.set((CameraCharacteristics.Key<int>) CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE, 0);
        List<String> isoModes = p.getSupportedIsoModes();
        int max = 0;
        if (isoModes != null && isoModes.size() > 0) {
            int[] iArr = new int[isoModes.size()];
            for (String mode : isoModes) {
                int convertedMode = convertSensitivity(mode);
                if (convertedMode == -1) {
                    StringBuilder sbAppend = new StringBuilder().append("Iso mode ");
                    if (mode == null) {
                        mode = WifiEnterpriseConfig.EMPTY_VALUE;
                    }
                    Log.w(TAG, sbAppend.append(mode).append(" not supported, skipping...").toString());
                } else if (convertedMode > max) {
                    max = convertedMode;
                }
            }
        }
        m.set(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE, Range.create(0, Integer.valueOf(max)));
    }

    private static void mapStatistics(CameraMetadataNative m, Camera.Parameters p) {
        int[] fdModes;
        if (p.getMaxNumDetectedFaces() > 0) {
            fdModes = new int[]{0, 1};
        } else {
            fdModes = new int[]{0};
        }
        m.set(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES, fdModes);
        m.set(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT, Integer.valueOf(p.getMaxNumDetectedFaces()));
    }

    private static void mapSync(CameraMetadataNative m, Camera.Parameters p) {
        m.set((CameraCharacteristics.Key<int>) CameraCharacteristics.SYNC_MAX_LATENCY, -1);
    }

    private static void appendStreamConfig(ArrayList<StreamConfiguration> configs, int format, List<Camera.Size> sizes) {
        for (Camera.Size size : sizes) {
            StreamConfiguration config = new StreamConfiguration(format, size.width, size.height, false);
            configs.add(config);
        }
    }

    static int convertSceneModeFromLegacy(String mode) {
        if (mode == null) {
            return 0;
        }
        int index = ArrayUtils.getArrayIndex(sLegacySceneModes, mode);
        if (index < 0) {
            return -1;
        }
        return sSceneModes[index];
    }

    static String convertSceneModeToLegacy(int mode) {
        if (mode == 1) {
            return "auto";
        }
        int index = ArrayUtils.getArrayIndex(sSceneModes, mode);
        if (index < 0) {
            return null;
        }
        return sLegacySceneModes[index];
    }

    static int convertEffectModeFromLegacy(String mode) {
        if (mode == null) {
            return 0;
        }
        int index = ArrayUtils.getArrayIndex(sLegacyEffectMode, mode);
        if (index < 0) {
            return -1;
        }
        return sEffectModes[index];
    }

    static String convertEffectModeToLegacy(int mode) {
        int index = ArrayUtils.getArrayIndex(sEffectModes, mode);
        if (index < 0) {
            return null;
        }
        return sLegacyEffectMode[index];
    }

    private static int convertAntiBandingMode(String mode) {
        if (mode == null) {
            return -1;
        }
        switch (mode) {
            case "off":
                break;
            case "50hz":
                break;
            case "60hz":
                break;
            case "auto":
                break;
            default:
                Log.w(TAG, "convertAntiBandingMode - Unknown antibanding mode " + mode);
                break;
        }
        return -1;
    }

    static String convertBurstCaptureMode(int mode) {
        if (mode == -1) {
            return null;
        }
        switch (mode) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            default:
                Log.w(TAG, "convertBurstCaptureMode - Unknown burst capture mode " + mode);
                break;
        }
        return null;
    }

    private static int convertBurstCaptureMode(String mode) {
        if (mode == null) {
            return -1;
        }
        switch (mode) {
            case "off-burst":
                break;
            case "infinite-burst":
                break;
            case "fast-burst":
                break;
            default:
                Log.w(TAG, "convertBurstCaptureMode - Unknown burst capture  mode " + mode);
                break;
        }
        return -1;
    }

    static String convertAntiBandingMode(int mode) {
        if (mode == -1) {
            return null;
        }
        switch (mode) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                Log.w(TAG, "convertAntiBandingMode - Unknown antibanding mode " + mode);
                break;
        }
        return null;
    }

    static int convertIsoMode(String mode) {
        if (mode == null) {
            return -1;
        }
        switch (mode) {
            case "auto":
                break;
            case "50":
                break;
            case "100":
                break;
            case "200":
                break;
            case "400":
                break;
            case "800":
                break;
            case "1600":
                break;
            case "3200":
                break;
            default:
                Log.w(TAG, "convertIsoMode - Unknown iso mode " + mode);
                break;
        }
        return -1;
    }

    static int convertSensitivity(String mode) {
        if (mode == null) {
            return -1;
        }
        switch (mode) {
            case "auto":
                break;
            case "50":
                break;
            case "100":
                break;
            case "200":
                break;
            case "400":
                break;
            case "800":
                break;
            case "1600":
                break;
            case "3200":
                break;
            default:
                Log.w(TAG, "convertSensitivity - Unknown iso mode " + mode);
                break;
        }
        return -1;
    }

    static String convertIsoModeToLegacy(int mode) {
        if (mode == -1) {
            return null;
        }
        switch (mode) {
            case 0:
                break;
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
            default:
                Log.w(TAG, "convertIsoMode - Unknown iso mode " + mode);
                break;
        }
        return null;
    }

    static String convertSensitivityToLegacy(int mode) {
        if (mode == -1) {
            return null;
        }
        switch (mode) {
            case 0:
                break;
            case 50:
                break;
            case 100:
                break;
            case 200:
                break;
            case 400:
                break;
            case 800:
                break;
            case 1600:
                break;
            case 3200:
                break;
            default:
                Log.w(TAG, "convertSensitivity - Unknown sensitivity " + mode);
                break;
        }
        return null;
    }

    static int convertAntiBandingModeOrDefault(String mode) {
        int antiBandingMode = convertAntiBandingMode(mode);
        if (antiBandingMode == -1) {
            return 0;
        }
        return antiBandingMode;
    }

    private static int[] convertAeFpsRangeToLegacy(Range<Integer> fpsRange) {
        int[] legacyFps = {((Integer) fpsRange.getLower()).intValue(), ((Integer) fpsRange.getUpper()).intValue()};
        return legacyFps;
    }

    private static long calculateJpegStallDuration(Camera.Size size) {
        long area = ((long) size.width) * ((long) size.height);
        return (area * 71) + 200000000;
    }

    public static void convertRequestMetadata(LegacyRequest request) {
        LegacyRequestMapper.convertRequestMetadata(request);
    }

    public static CameraMetadataNative createRequestTemplate(CameraCharacteristics c, int templateId, Camera.Parameters templateParam) {
        int captureIntent;
        int afMode;
        if (!ArrayUtils.contains(sAllowedTemplates, templateId)) {
            throw new IllegalArgumentException("templateId out of range");
        }
        CameraMetadataNative m = new CameraMetadataNative();
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AWB_MODE, 1);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, 3);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
        m.set((CaptureRequest.Key<boolean>) CaptureRequest.CONTROL_AE_LOCK, false);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, 0);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AF_TRIGGER, 0);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AWB_MODE, 1);
        m.set((CaptureRequest.Key<boolean>) CaptureRequest.CONTROL_AWB_LOCK, false);
        Rect activeArray = (Rect) c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        MeteringRectangle[] activeRegions = {new MeteringRectangle(0, 0, activeArray.width() - 1, activeArray.height() - 1, 0)};
        m.set(CaptureRequest.CONTROL_AE_REGIONS, activeRegions);
        m.set(CaptureRequest.CONTROL_AWB_REGIONS, activeRegions);
        m.set(CaptureRequest.CONTROL_AF_REGIONS, activeRegions);
        switch (templateId) {
            case 1:
                captureIntent = 1;
                break;
            case 2:
                captureIntent = 2;
                break;
            case 3:
                captureIntent = 3;
                break;
            default:
                throw new AssertionError("Impossible; keep in sync with sAllowedTemplates");
        }
        m.set(CaptureRequest.CONTROL_CAPTURE_INTENT, Integer.valueOf(captureIntent));
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_AE_MODE, 1);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_MODE, 1);
        Float minimumFocusDistance = (Float) c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        templateParam.getFocusMode();
        if (minimumFocusDistance != null && minimumFocusDistance.floatValue() == 0.0f) {
            afMode = 0;
        } else {
            afMode = 1;
            if (templateId == 3 || templateId == 4) {
                if (ArrayUtils.contains((int[]) c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES), 3)) {
                    afMode = 3;
                }
            } else if ((templateId == 1 || templateId == 2) && ArrayUtils.contains((int[]) c.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES), 4)) {
                afMode = 4;
            }
        }
        if (VERBOSE) {
            Log.v(TAG, "createRequestTemplate (templateId=" + templateId + "), afMode=" + afMode + ", minimumFocusDistance=" + minimumFocusDistance);
        }
        m.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(afMode));
        Range<Integer>[] availableFpsRange = (Range[]) c.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        Range<Integer> bestRange = availableFpsRange[0];
        for (Range<Integer> r : availableFpsRange) {
            if (((Integer) bestRange.getUpper()).intValue() < ((Integer) r.getUpper()).intValue()) {
                bestRange = r;
            } else if (bestRange.getUpper() == r.getUpper() && ((Integer) bestRange.getLower()).intValue() < ((Integer) r.getLower()).intValue()) {
                bestRange = r;
            }
        }
        m.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, bestRange);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_SCENE_MODE, 0);
        m.set((CaptureRequest.Key<int>) CaptureRequest.STATISTICS_FACE_DETECT_MODE, 0);
        m.set((CaptureRequest.Key<int>) CaptureRequest.SENSOR_SENSITIVITY, 0);
        m.set((CaptureRequest.Key<int>) CaptureRequest.FLASH_MODE, 0);
        m.set((CaptureRequest.Key<int>) CaptureRequest.NOISE_REDUCTION_MODE, 1);
        m.set(CaptureRequest.LENS_FOCAL_LENGTH, Float.valueOf(((float[]) c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS))[0]));
        Size[] sizes = (Size[]) c.get(CameraCharacteristics.JPEG_AVAILABLE_THUMBNAIL_SIZES);
        m.set(CaptureRequest.JPEG_THUMBNAIL_SIZE, sizes.length > 1 ? sizes[1] : sizes[0]);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_EFFECT_MODE, 0);
        m.set((CaptureRequest.Key<int>) CaptureRequest.CONTROL_ISO_MODE, 0);
        int defaultContrast = templateParam.getContrast();
        int defaultBrightness = templateParam.getBrightness();
        int defaultSharpness = templateParam.getSharpness();
        int defaultSaturation = templateParam.getSaturation();
        m.set(CaptureRequest.CONTROL_CURRENT_CONTRAST, Integer.valueOf(defaultContrast));
        m.set(CaptureRequest.CONTROL_CURRENT_SATURATION, Integer.valueOf(defaultSaturation));
        m.set(CaptureRequest.CONTROL_CURRENT_BRIGHTNESS, Integer.valueOf(defaultBrightness));
        m.set(CaptureRequest.CONTROL_CURRENT_SHARPNESS, Integer.valueOf(defaultSharpness));
        return m;
    }

    private static int[] getTagsForKeys(CameraCharacteristics.Key<?>[] keys) {
        int[] tags = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            tags[i] = keys[i].getNativeKey().getTag();
        }
        return tags;
    }

    private static int[] getTagsForKeys(CaptureRequest.Key<?>[] keys) {
        int[] tags = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            tags[i] = keys[i].getNativeKey().getTag();
        }
        return tags;
    }

    private static int[] getTagsForKeys(CaptureResult.Key<?>[] keys) {
        int[] tags = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            tags[i] = keys[i].getNativeKey().getTag();
        }
        return tags;
    }

    static String convertAfModeToLegacy(int mode, List<String> supportedFocusModes) {
        if (supportedFocusModes == null || supportedFocusModes.isEmpty()) {
            Log.w(TAG, "No focus modes supported; API1 bug");
            return null;
        }
        String param = null;
        switch (mode) {
            case 0:
                if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                    param = Camera.Parameters.FOCUS_MODE_FIXED;
                } else {
                    param = Camera.Parameters.FOCUS_MODE_INFINITY;
                }
                break;
            case 1:
                param = "auto";
                break;
            case 2:
                param = "macro";
                break;
            case 3:
                param = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
                break;
            case 4:
                param = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
                break;
            case 5:
                param = Camera.Parameters.FOCUS_MODE_EDOF;
                break;
            case 6:
                param = Camera.Parameters.FOCUS_MODE_MANUAL;
                break;
        }
        if (!supportedFocusModes.contains(param)) {
            String defaultMode = supportedFocusModes.get(0);
            Log.w(TAG, String.format("convertAfModeToLegacy - ignoring unsupported mode %d, defaulting to %s", Integer.valueOf(mode), defaultMode));
            return defaultMode;
        }
        return param;
    }

    static int convertLegacyToAfMode(String param, List<String> supportedFocusModes) {
        if (supportedFocusModes == null || supportedFocusModes.isEmpty()) {
            Log.w(TAG, "No focus modes supported; API1 bug");
            return -1;
        }
        if (param == null || !supportedFocusModes.contains(param)) {
            param = supportedFocusModes.get(0);
            Log.w(TAG, String.format("convertLegacyToAfMode - ignoring unsupported modedefaulting to %s", param));
        }
        switch (param) {
        }
        return -1;
    }

    static float[] convertIspInfoToFloat(Camera.IspInfo ispInfo) {
        float[] ispInfoList = {ispInfo.b_gain, ispInfo.dvs_cropH, ispInfo.dvs_cropW, ispInfo.dvs_cropX, ispInfo.dvs_cropY, ispInfo.exposure_time, ispInfo.focus_position, ispInfo.g_gain, ispInfo.r_gain, ispInfo.sensor_gain, ispInfo.shift_b, ispInfo.shift_g, ispInfo.shift_r, ispInfo.y_value};
        return ispInfoList;
    }
}
