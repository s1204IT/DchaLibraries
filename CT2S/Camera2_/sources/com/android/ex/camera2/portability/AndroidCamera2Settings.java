package com.android.ex.camera2.portability;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.location.Location;
import android.os.SystemProperties;
import android.support.v4.util.TimeUtils;
import android.support.v4.widget.ViewDragHelper;
import android.text.TextUtils;
import android.util.Range;
import com.android.camera.ButtonManager;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.utils.Camera2RequestSettingsSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AndroidCamera2Settings extends CameraSettings {
    private static final int A51_TEST_MODE = 1;
    private static final int B52_TEST_MODE = 2;
    private static final Log.Tag TAG = new Log.Tag("AndCam2Set");
    private final Rect mActiveArray;
    private final Rect mCropRectangle;
    private final Camera2RequestSettingsSet mRequestSettings;
    private final CaptureRequest.Builder mTemplateSettings;
    private Rect mVisiblePreviewRectangle;

    public AndroidCamera2Settings(CameraDevice camera, int template, Rect activeArray, Size preview, Size photo) throws CameraAccessException {
        if (camera == null) {
            throw new NullPointerException("camera must not be null");
        }
        if (activeArray == null) {
            throw new NullPointerException("activeArray must not be null");
        }
        this.mTemplateSettings = camera.createCaptureRequest(template);
        this.mRequestSettings = new Camera2RequestSettingsSet();
        this.mActiveArray = activeArray;
        this.mCropRectangle = new Rect(0, 0, activeArray.width(), activeArray.height());
        this.mSizesLocked = false;
        Range<Integer> previewFpsRange = (Range) this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
        if (previewFpsRange != null) {
            setPreviewFpsRange(((Integer) previewFpsRange.getLower()).intValue(), ((Integer) previewFpsRange.getUpper()).intValue());
        }
        setPreviewSize(preview);
        setPhotoSize(photo);
        this.mJpegCompressQuality = ((Byte) queryTemplateDefaultOrMakeOneUp(CaptureRequest.JPEG_QUALITY, (byte) 0)).byteValue();
        this.mCurrentZoomRatio = 1.0f;
        this.mExposureCompensationIndex = ((Integer) queryTemplateDefaultOrMakeOneUp(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0)).intValue();
        this.mCurrentFlashMode = flashModeFromRequest();
        Integer currentFocusMode = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_AF_MODE);
        if (currentFocusMode != null) {
            this.mCurrentFocusMode = AndroidCamera2Capabilities.focusModeFromInt(currentFocusMode.intValue());
        }
        Integer currentSceneMode = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_SCENE_MODE);
        if (currentSceneMode != null) {
            this.mCurrentSceneMode = AndroidCamera2Capabilities.sceneModeFromInt(currentSceneMode.intValue());
        }
        Integer whiteBalance = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_AWB_MODE);
        if (whiteBalance != null) {
            this.mWhiteBalance = AndroidCamera2Capabilities.whiteBalanceFromInt(whiteBalance.intValue());
        }
        Integer colorEffect = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_EFFECT_MODE);
        if (colorEffect != null) {
            this.mColorEffect = AndroidCamera2Capabilities.colorEffectFromInt(colorEffect.intValue());
        }
        Integer antiBanding = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE);
        if (antiBanding != null) {
            this.mAntiBanding = AndroidCamera2Capabilities.bandingFilterFromInt(antiBanding.intValue());
        }
        Integer isoMode = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_ISO_MODE);
        if (isoMode != null) {
            this.mIsoMode = AndroidCamera2Capabilities.isoModeFromInt(isoMode.intValue());
        }
        Integer burstMode = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_BURST_CAPTURE_MODE);
        if (burstMode != null) {
            this.mBurstCaptureMode = AndroidCamera2Capabilities.burstCaptureModeFromInt(isoMode.intValue());
        }
        this.mContrast = ((Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_CURRENT_CONTRAST)).intValue();
        this.mSaturation = ((Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_CURRENT_SATURATION)).intValue();
        this.mBrightness = ((Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_CURRENT_BRIGHTNESS)).intValue();
        this.mSharpness = ((Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_CURRENT_SHARPNESS)).intValue();
        ((Integer) this.mTemplateSettings.get(CaptureRequest.SENSOR_SENSITIVITY)).intValue();
        this.mIsoMode = CameraCapabilities.IsoMode.ISOAUTO;
        this.mVideoStabilizationEnabled = ((Integer) queryTemplateDefaultOrMakeOneUp(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, 0)).intValue() == 1;
        this.mAutoExposureLocked = ((Boolean) queryTemplateDefaultOrMakeOneUp(CaptureRequest.CONTROL_AE_LOCK, false)).booleanValue();
        this.mAutoWhiteBalanceLocked = ((Boolean) queryTemplateDefaultOrMakeOneUp(CaptureRequest.CONTROL_AWB_LOCK, false)).booleanValue();
        android.util.Size exifThumbnailSize = (android.util.Size) this.mTemplateSettings.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);
        if (exifThumbnailSize != null) {
            this.mExifThumbnailSize = new Size(exifThumbnailSize.getWidth(), exifThumbnailSize.getHeight());
        }
    }

    public AndroidCamera2Settings(AndroidCamera2Settings other) {
        super(other);
        this.mTemplateSettings = other.mTemplateSettings;
        this.mRequestSettings = new Camera2RequestSettingsSet(other.mRequestSettings);
        this.mActiveArray = other.mActiveArray;
        this.mCropRectangle = new Rect(other.mCropRectangle);
    }

    @Override
    public CameraSettings copy() {
        return new AndroidCamera2Settings(this);
    }

    private <T> T queryTemplateDefaultOrMakeOneUp(CaptureRequest.Key<T> key, T t) {
        T t2 = (T) this.mTemplateSettings.get(key);
        if (t2 == null) {
            this.mTemplateSettings.set(key, t);
            return t;
        }
        return t2;
    }

    private CameraCapabilities.FlashMode flashModeFromRequest() {
        Integer autoExposure = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_MODE);
        if (autoExposure != null) {
            switch (autoExposure.intValue()) {
                case 1:
                    return CameraCapabilities.FlashMode.OFF;
                case 2:
                    return CameraCapabilities.FlashMode.AUTO;
                case 3:
                    if (((Integer) this.mTemplateSettings.get(CaptureRequest.FLASH_MODE)).intValue() == 2) {
                        return CameraCapabilities.FlashMode.TORCH;
                    }
                    return CameraCapabilities.FlashMode.ON;
                case 4:
                    return CameraCapabilities.FlashMode.RED_EYE;
            }
        }
        return null;
    }

    @Override
    public void setZoomRatio(float ratio) {
        super.setZoomRatio(ratio);
        this.mCropRectangle.set(0, 0, toIntConstrained(this.mActiveArray.width() / this.mCurrentZoomRatio, 0, this.mActiveArray.width()), toIntConstrained(this.mActiveArray.height() / this.mCurrentZoomRatio, 0, this.mActiveArray.height()));
        this.mCropRectangle.offsetTo((this.mActiveArray.width() - this.mCropRectangle.width()) / 2, (this.mActiveArray.height() - this.mCropRectangle.height()) / 2);
        this.mVisiblePreviewRectangle = effectiveCropRectFromRequested(this.mCropRectangle, this.mCurrentPreviewSize);
    }

    private boolean matchesTemplateDefault(CaptureRequest.Key<?> setting) {
        boolean z = false;
        if (setting == CaptureRequest.CONTROL_AE_REGIONS) {
            return this.mMeteringAreas.size() == 0;
        }
        if (setting == CaptureRequest.CONTROL_AF_REGIONS) {
            return this.mFocusAreas.size() == 0;
        }
        if (setting == CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE) {
            Range<Integer> defaultFpsRange = (Range) this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
            if (this.mPreviewFpsRangeMin == 0 && this.mPreviewFpsRangeMax == 0) {
                return true;
            }
            return defaultFpsRange != null && this.mPreviewFpsRangeMin == ((Integer) defaultFpsRange.getLower()).intValue() && this.mPreviewFpsRangeMax == ((Integer) defaultFpsRange.getUpper()).intValue();
        }
        if (setting == CaptureRequest.JPEG_QUALITY) {
            return Objects.equals(Byte.valueOf(this.mJpegCompressQuality), this.mTemplateSettings.get(CaptureRequest.JPEG_QUALITY));
        }
        if (setting == CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) {
            return Objects.equals(Integer.valueOf(this.mExposureCompensationIndex), this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION));
        }
        if (setting == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE) {
            Integer videoStabilization = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE);
            if ((videoStabilization != null && this.mVideoStabilizationEnabled && videoStabilization.intValue() == 1) || (!this.mVideoStabilizationEnabled && videoStabilization.intValue() == 0)) {
                z = true;
            }
            return z;
        }
        if (setting == CaptureRequest.CONTROL_AE_LOCK) {
            return Objects.equals(Boolean.valueOf(this.mAutoExposureLocked), this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_LOCK));
        }
        if (setting == CaptureRequest.CONTROL_AWB_LOCK) {
            return Objects.equals(Boolean.valueOf(this.mAutoWhiteBalanceLocked), this.mTemplateSettings.get(CaptureRequest.CONTROL_AWB_LOCK));
        }
        if (setting == CaptureRequest.JPEG_THUMBNAIL_SIZE) {
            if (this.mExifThumbnailSize == null) {
                return false;
            }
            android.util.Size defaultThumbnailSize = (android.util.Size) this.mTemplateSettings.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);
            if (this.mExifThumbnailSize.width() == 0 && this.mExifThumbnailSize.height() == 0) {
                return true;
            }
            return defaultThumbnailSize != null && this.mExifThumbnailSize.width() == defaultThumbnailSize.getWidth() && this.mExifThumbnailSize.height() == defaultThumbnailSize.getHeight();
        }
        Log.w(TAG, "Settings implementation checked default of unhandled option key");
        return true;
    }

    private <T> void updateRequestSettingOrForceToDefault(CaptureRequest.Key<T> setting, T possibleChoice) {
        Camera2RequestSettingsSet camera2RequestSettingsSet = this.mRequestSettings;
        if (matchesTemplateDefault(setting)) {
            possibleChoice = null;
        }
        camera2RequestSettingsSet.set(setting, possibleChoice);
    }

    public Camera2RequestSettingsSet getRequestSettings() {
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AE_REGIONS, legacyAreasToMeteringRectangles(this.mMeteringAreas));
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AF_REGIONS, legacyAreasToMeteringRectangles(this.mFocusAreas));
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range(Integer.valueOf(this.mPreviewFpsRangeMin), Integer.valueOf(this.mPreviewFpsRangeMax)));
        updateRequestSettingOrForceToDefault(CaptureRequest.JPEG_QUALITY, Byte.valueOf(this.mJpegCompressQuality));
        this.mRequestSettings.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRectangle);
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.valueOf(this.mExposureCompensationIndex));
        updateRequestFlashMode();
        updateRequestFocusMode();
        updateRequestSceneMode();
        updateRequestWhiteBalance();
        updateRequestColorEffect();
        updateRequestBandingFilter();
        updateRequestBurstCapture();
        this.mRequestSettings.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(AndroidCamera2Capabilities.isoModeToInt(this.mIsoMode)));
        if (this.mIsFaceBeautify) {
            this.mRequestSettings.set(CaptureRequest.CONTROL_FACE_BEAUTY, 1);
        } else {
            this.mRequestSettings.set(CaptureRequest.CONTROL_FACE_BEAUTY, 0);
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_BURST_CAPTURE_NUM, Integer.valueOf(this.mBurstCaptureNum));
        this.mRequestSettings.set(CaptureRequest.CONTROL_CURRENT_CONTRAST, Integer.valueOf(this.mContrast));
        this.mRequestSettings.set(CaptureRequest.CONTROL_CURRENT_SATURATION, Integer.valueOf(this.mSaturation));
        this.mRequestSettings.set(CaptureRequest.CONTROL_CURRENT_BRIGHTNESS, Integer.valueOf(this.mBrightness));
        this.mRequestSettings.set(CaptureRequest.CONTROL_CURRENT_SHARPNESS, Integer.valueOf(this.mSharpness));
        boolean enableIspParams = !"0".equals(SystemProperties.get("persist.service.camera.ispparam", "0"));
        boolean enableRawDump = !"0".equals(SystemProperties.get("persist.service.camera.rawdump", "0"));
        int rawDumpMode = 0;
        if (enableRawDump) {
            rawDumpMode = Integer.parseInt(SystemProperties.get("persist.service.camera.rawdump", "0"));
        }
        if (enableIspParams) {
            this.mRequestSettings.set(CaptureRequest.SENSOR_SHUTTER_SPEED, Float.valueOf((float) this.mExposureTime));
            this.mRequestSettings.set(CaptureRequest.LENS_FOCUS_POSITION, Integer.valueOf(this.mFocusPosition));
            if (rawDumpMode == 1) {
                double[] adispArray = {this.mAGain, this.mDGain, this.mIspGain};
                this.mRequestSettings.set(CaptureRequest.COLOR_CORRECTION_ISP_GAINS, adispArray);
            } else if (rawDumpMode == 2) {
                double[] adispArray2 = {this.mAGain, this.mBGain, this.mGbGain, this.mGrGain, this.mRGain};
                this.mRequestSettings.set(CaptureRequest.COLOR_CORRECTION_ISP_GAINS, adispArray2);
            }
            int[] yuvDns = new int[3];
            if (this.mYDns) {
                yuvDns[0] = 1;
            } else {
                yuvDns[0] = 0;
            }
            if (this.mYDns) {
                yuvDns[1] = 1;
            } else {
                yuvDns[1] = 0;
            }
            yuvDns[2] = this.mDnsTimes;
            this.mRequestSettings.set(CaptureRequest.JPEG_YUV_DNS, yuvDns);
        }
        this.mRequestSettings.set(CaptureRequest.SENSOR_EXPOSURE_MODE, Integer.valueOf(this.mExposureMode));
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, Integer.valueOf(this.mVideoStabilizationEnabled ? 1 : 0));
        this.mRequestSettings.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, this.mVideoStabilizationEnabled ? 0 : null);
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(this.mAutoExposureLocked));
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(this.mAutoWhiteBalanceLocked));
        updateRequestGpsData();
        if (this.mExifThumbnailSize != null) {
            updateRequestSettingOrForceToDefault(CaptureRequest.JPEG_THUMBNAIL_SIZE, new android.util.Size(this.mExifThumbnailSize.width(), this.mExifThumbnailSize.height()));
        } else {
            updateRequestSettingOrForceToDefault(CaptureRequest.JPEG_THUMBNAIL_SIZE, null);
        }
        return this.mRequestSettings;
    }

    private MeteringRectangle[] legacyAreasToMeteringRectangles(List<Camera.Area> reference) {
        MeteringRectangle[] transformed = null;
        if (reference.size() > 0) {
            transformed = new MeteringRectangle[reference.size()];
            for (int index = 0; index < reference.size(); index++) {
                Camera.Area source = reference.get(index);
                Rect rectangle = source.rect;
                double oldLeft = ((double) (rectangle.left + 1000)) / 2000.0d;
                double oldTop = ((double) (rectangle.top + 1000)) / 2000.0d;
                double oldRight = ((double) (rectangle.right + 1000)) / 2000.0d;
                double oldBottom = ((double) (rectangle.bottom + 1000)) / 2000.0d;
                int left = this.mCropRectangle.left + toIntConstrained(((double) this.mCropRectangle.width()) * oldLeft, 0, this.mCropRectangle.width() - 1);
                int top = this.mCropRectangle.top + toIntConstrained(((double) this.mCropRectangle.height()) * oldTop, 0, this.mCropRectangle.height() - 1);
                int right = this.mCropRectangle.left + toIntConstrained(((double) this.mCropRectangle.width()) * oldRight, 0, this.mCropRectangle.width() - 1);
                int bottom = this.mCropRectangle.top + toIntConstrained(((double) this.mCropRectangle.height()) * oldBottom, 0, this.mCropRectangle.height() - 1);
                transformed[index] = new MeteringRectangle(left, top, right - left, bottom - top, source.weight);
            }
        }
        return transformed;
    }

    private int toIntConstrained(double original, int min, int max) {
        return (int) Math.min(Math.max(original, min), max);
    }

    private void updateRequestFlashMode() {
        Integer aeMode = null;
        Integer flashMode = null;
        if (this.mCurrentFlashMode != null) {
            switch (this.mCurrentFlashMode) {
                case AUTO:
                    aeMode = 2;
                    break;
                case OFF:
                    aeMode = 1;
                    flashMode = 0;
                    break;
                case ON:
                    aeMode = 3;
                    flashMode = 1;
                    break;
                case TORCH:
                    flashMode = 2;
                    break;
                case RED_EYE:
                    aeMode = 4;
                    break;
                default:
                    Log.w(TAG, "Unable to convert to API 2 flash mode: " + this.mCurrentFlashMode);
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_AE_MODE, aeMode);
        this.mRequestSettings.set(CaptureRequest.FLASH_MODE, flashMode);
    }

    private void updateRequestFocusMode() {
        Integer mode = null;
        if (this.mCurrentFocusMode != null) {
            switch (this.mCurrentFocusMode) {
                case AUTO:
                    mode = 1;
                    break;
                case CONTINUOUS_PICTURE:
                    mode = 4;
                    break;
                case CONTINUOUS_VIDEO:
                    mode = 3;
                    break;
                case EXTENDED_DOF:
                    mode = 5;
                    break;
                case FIXED:
                    mode = 0;
                    break;
                case MACRO:
                    mode = 2;
                    break;
                case MANUAL:
                    mode = 6;
                    break;
                default:
                    Log.w(TAG, "Unable to convert to API 2 focus mode: " + this.mCurrentFocusMode);
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_AF_MODE, mode);
    }

    private void updateRequestSceneMode() {
        Integer mode = null;
        Integer controlMode = null;
        if (this.mCurrentSceneMode != null) {
            switch (AnonymousClass1.$SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[this.mCurrentSceneMode.ordinal()]) {
                case 1:
                    mode = 0;
                    break;
                case 2:
                    mode = 2;
                    break;
                case 3:
                    mode = 16;
                    break;
                case 4:
                    mode = 8;
                    break;
                case 5:
                    mode = 15;
                    break;
                case 6:
                    mode = 12;
                    break;
                case 7:
                    mode = Integer.valueOf(LegacyVendorTags.CONTROL_SCENE_MODE_HDR);
                    break;
                case 8:
                    mode = 4;
                    break;
                case 9:
                    mode = 5;
                    break;
                case 10:
                    mode = 14;
                    break;
                case 11:
                    mode = 3;
                    break;
                case 12:
                    mode = 9;
                    break;
                case ButtonManager.BUTTON_AUTO_FOCUS:
                    mode = 13;
                    break;
                case 14:
                    mode = 11;
                    break;
                case ViewDragHelper.EDGE_ALL:
                    mode = 10;
                    break;
                case 16:
                    mode = 7;
                    break;
                case 17:
                    mode = 19;
                    break;
                case 18:
                    mode = 1;
                    break;
                case TimeUtils.HUNDRED_DAY_FIELD_LEN:
                    mode = 20;
                    break;
                case 20:
                    mode = 21;
                    break;
                case 21:
                    mode = 22;
                    break;
                default:
                    Log.w(TAG, "Unable to convert to API 2 scene mode: " + this.mCurrentSceneMode);
                    break;
            }
            switch (this.mCurrentSceneMode) {
                case AUTO:
                    controlMode = 1;
                    break;
                default:
                    controlMode = 2;
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_SCENE_MODE, mode);
        this.mRequestSettings.set(CaptureRequest.CONTROL_MODE, controlMode);
    }

    private void updateRequestWhiteBalance() {
        Integer mode = null;
        if (this.mWhiteBalance != null) {
            switch (this.mWhiteBalance) {
                case AUTO:
                    mode = 1;
                    break;
                case CLOUDY_DAYLIGHT:
                    mode = 6;
                    break;
                case DAYLIGHT:
                    mode = 5;
                    break;
                case FLUORESCENT:
                    mode = 3;
                    break;
                case INCANDESCENT:
                    mode = 2;
                    break;
                case SHADE:
                    mode = 8;
                    break;
                case TWILIGHT:
                    mode = 7;
                    break;
                case WARM_FLUORESCENT:
                    mode = 4;
                    break;
                default:
                    Log.w(TAG, "Unable to convert to API 2 white balance: " + this.mWhiteBalance);
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_AWB_MODE, mode);
    }

    private void updateRequestColorEffect() {
        Integer mode = null;
        if (this.mColorEffect != null) {
            switch (AnonymousClass1.$SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[this.mColorEffect.ordinal()]) {
                case 1:
                    mode = 0;
                    break;
                case 2:
                    mode = 1;
                    break;
                case 3:
                    mode = 2;
                    break;
                case 4:
                    mode = 3;
                    break;
                case 5:
                    mode = 4;
                    break;
                case 6:
                    mode = 5;
                    break;
                case 7:
                    mode = 6;
                    break;
                case 8:
                    mode = 7;
                    break;
                case 9:
                    mode = 8;
                    break;
                case 10:
                    mode = 9;
                    break;
                case 11:
                    mode = 10;
                    break;
                case 12:
                    mode = 11;
                    break;
                case ButtonManager.BUTTON_AUTO_FOCUS:
                    mode = 12;
                    break;
                case 14:
                    mode = 13;
                    break;
                case ViewDragHelper.EDGE_ALL:
                    mode = 14;
                    break;
                case 16:
                    mode = 15;
                    break;
                case 17:
                    mode = 16;
                    break;
                default:
                    Log.w(TAG, "Unable to convert to API 2 color effect: " + this.mColorEffect);
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_EFFECT_MODE, mode);
        if (this.mColorEffectParam != null) {
            int[] colorEffectParam = new int[3];
            List<String> substrings = split(this.mColorEffectParam);
            int i = 0;
            for (String s : substrings) {
                if (i < 3) {
                    colorEffectParam[i] = Integer.parseInt(s);
                    Log.i(TAG, "colorEffectParam [" + i + "] = " + colorEffectParam[i]);
                    i++;
                }
            }
            this.mRequestSettings.set(CaptureRequest.CONTROL_EFFECT_MODE_PARAM, colorEffectParam);
            Log.i(TAG, "mColorEffectParam = " + this.mColorEffectParam);
        }
    }

    private ArrayList<String> split(String str) {
        if (str == null) {
            return null;
        }
        TextUtils.StringSplitter<String> splitter = new TextUtils.SimpleStringSplitter(',');
        splitter.setString(str);
        ArrayList<String> substrings = new ArrayList<>();
        for (String s : splitter) {
            substrings.add(s);
        }
        return substrings;
    }

    private void updateRequestIsoMode() {
        Integer mode = null;
        if (this.mIsoMode != null) {
            switch (this.mIsoMode) {
                case ISOAUTO:
                    mode = 0;
                    break;
                case ISO50:
                    mode = 1;
                    break;
                case ISO100:
                    mode = 2;
                    break;
                case ISO200:
                    mode = 3;
                    break;
                case ISO400:
                    mode = 4;
                    break;
                case ISO800:
                    mode = 5;
                    break;
                case ISO1600:
                    mode = 6;
                    break;
                case ISO3200:
                    mode = 7;
                    break;
                default:
                    Log.w(TAG, "Unable to convert to API 2 iso mode: " + this.mIsoMode);
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_ISO_MODE, mode);
    }

    private void updateRequestBandingFilter() {
        Integer mode = null;
        if (this.mAntiBanding != null) {
            switch (this.mAntiBanding) {
                case ABAUTO:
                    mode = 3;
                    break;
                case AB50HZ:
                    mode = 1;
                    break;
                case AB60HZ:
                    mode = 2;
                    break;
                case ABOFF:
                    mode = 0;
                    break;
                default:
                    Log.w(TAG, "Unable to convert to API 2 banding filter: " + this.mAntiBanding);
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, mode);
    }

    private void updateRequestBurstCapture() {
        Integer mode = null;
        if (this.mBurstCaptureMode != null) {
            switch (this.mBurstCaptureMode) {
                case OFF:
                    mode = 0;
                    break;
                case INFINITE:
                    mode = 1;
                    break;
                case FAST:
                    mode = 2;
                    break;
                default:
                    Log.w(TAG, "Unable to convert to API 2 burst capture: " + this.mBurstCaptureMode);
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_BURST_CAPTURE_MODE, mode);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect;

        static {
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$BurstCapture[CameraCapabilities.BurstCapture.OFF.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$BurstCapture[CameraCapabilities.BurstCapture.INFINITE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$BurstCapture[CameraCapabilities.BurstCapture.FAST.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$AntiBanding = new int[CameraCapabilities.AntiBanding.values().length];
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$AntiBanding[CameraCapabilities.AntiBanding.ABAUTO.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$AntiBanding[CameraCapabilities.AntiBanding.AB50HZ.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$AntiBanding[CameraCapabilities.AntiBanding.AB60HZ.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$AntiBanding[CameraCapabilities.AntiBanding.ABOFF.ordinal()] = 4;
            } catch (NoSuchFieldError e7) {
            }
            $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode = new int[CameraCapabilities.IsoMode.values().length];
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode[CameraCapabilities.IsoMode.ISOAUTO.ordinal()] = 1;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode[CameraCapabilities.IsoMode.ISO50.ordinal()] = 2;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode[CameraCapabilities.IsoMode.ISO100.ordinal()] = 3;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode[CameraCapabilities.IsoMode.ISO200.ordinal()] = 4;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode[CameraCapabilities.IsoMode.ISO400.ordinal()] = 5;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode[CameraCapabilities.IsoMode.ISO800.ordinal()] = 6;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode[CameraCapabilities.IsoMode.ISO1600.ordinal()] = 7;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$IsoMode[CameraCapabilities.IsoMode.ISO3200.ordinal()] = 8;
            } catch (NoSuchFieldError e15) {
            }
            $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect = new int[CameraCapabilities.ColorEffect.values().length];
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.NONE.ordinal()] = 1;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.MONO.ordinal()] = 2;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.NEGATIVE.ordinal()] = 3;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.SOLARIZE.ordinal()] = 4;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.SEPIA.ordinal()] = 5;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.POSTERIZE.ordinal()] = 6;
            } catch (NoSuchFieldError e21) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.WHITE_BOARD.ordinal()] = 7;
            } catch (NoSuchFieldError e22) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.BLACK_BOARD.ordinal()] = 8;
            } catch (NoSuchFieldError e23) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.AQUA.ordinal()] = 9;
            } catch (NoSuchFieldError e24) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.OLD_MOVIE.ordinal()] = 10;
            } catch (NoSuchFieldError e25) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.TOON_SHADING.ordinal()] = 11;
            } catch (NoSuchFieldError e26) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.PENCIL_SKETCH.ordinal()] = 12;
            } catch (NoSuchFieldError e27) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.GLOW.ordinal()] = 13;
            } catch (NoSuchFieldError e28) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.TWIST.ordinal()] = 14;
            } catch (NoSuchFieldError e29) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.VIVID.ordinal()] = 15;
            } catch (NoSuchFieldError e30) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.FRAME.ordinal()] = 16;
            } catch (NoSuchFieldError e31) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$ColorEffect[CameraCapabilities.ColorEffect.SUNSHINE.ordinal()] = 17;
            } catch (NoSuchFieldError e32) {
            }
            $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance = new int[CameraCapabilities.WhiteBalance.values().length];
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance[CameraCapabilities.WhiteBalance.AUTO.ordinal()] = 1;
            } catch (NoSuchFieldError e33) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance[CameraCapabilities.WhiteBalance.CLOUDY_DAYLIGHT.ordinal()] = 2;
            } catch (NoSuchFieldError e34) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance[CameraCapabilities.WhiteBalance.DAYLIGHT.ordinal()] = 3;
            } catch (NoSuchFieldError e35) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance[CameraCapabilities.WhiteBalance.FLUORESCENT.ordinal()] = 4;
            } catch (NoSuchFieldError e36) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance[CameraCapabilities.WhiteBalance.INCANDESCENT.ordinal()] = 5;
            } catch (NoSuchFieldError e37) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance[CameraCapabilities.WhiteBalance.SHADE.ordinal()] = 6;
            } catch (NoSuchFieldError e38) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance[CameraCapabilities.WhiteBalance.TWILIGHT.ordinal()] = 7;
            } catch (NoSuchFieldError e39) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$WhiteBalance[CameraCapabilities.WhiteBalance.WARM_FLUORESCENT.ordinal()] = 8;
            } catch (NoSuchFieldError e40) {
            }
            $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode = new int[CameraCapabilities.SceneMode.values().length];
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.AUTO.ordinal()] = 1;
            } catch (NoSuchFieldError e41) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.ACTION.ordinal()] = 2;
            } catch (NoSuchFieldError e42) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.BARCODE.ordinal()] = 3;
            } catch (NoSuchFieldError e43) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.BEACH.ordinal()] = 4;
            } catch (NoSuchFieldError e44) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.CANDLELIGHT.ordinal()] = 5;
            } catch (NoSuchFieldError e45) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.FIREWORKS.ordinal()] = 6;
            } catch (NoSuchFieldError e46) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.HDR.ordinal()] = 7;
            } catch (NoSuchFieldError e47) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.LANDSCAPE.ordinal()] = 8;
            } catch (NoSuchFieldError e48) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.NIGHT.ordinal()] = 9;
            } catch (NoSuchFieldError e49) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.PARTY.ordinal()] = 10;
            } catch (NoSuchFieldError e50) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.PORTRAIT.ordinal()] = 11;
            } catch (NoSuchFieldError e51) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.SNOW.ordinal()] = 12;
            } catch (NoSuchFieldError e52) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.SPORTS.ordinal()] = 13;
            } catch (NoSuchFieldError e53) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.STEADYPHOTO.ordinal()] = 14;
            } catch (NoSuchFieldError e54) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.SUNSET.ordinal()] = 15;
            } catch (NoSuchFieldError e55) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.THEATRE.ordinal()] = 16;
            } catch (NoSuchFieldError e56) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.SCENE_DETECT.ordinal()] = 17;
            } catch (NoSuchFieldError e57) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.FACEDETECT.ordinal()] = 18;
            } catch (NoSuchFieldError e58) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.SMILE.ordinal()] = 19;
            } catch (NoSuchFieldError e59) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.BEST_SHOT.ordinal()] = 20;
            } catch (NoSuchFieldError e60) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$SceneMode[CameraCapabilities.SceneMode.TEST.ordinal()] = 21;
            } catch (NoSuchFieldError e61) {
            }
            $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FocusMode = new int[CameraCapabilities.FocusMode.values().length];
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FocusMode[CameraCapabilities.FocusMode.AUTO.ordinal()] = 1;
            } catch (NoSuchFieldError e62) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FocusMode[CameraCapabilities.FocusMode.CONTINUOUS_PICTURE.ordinal()] = 2;
            } catch (NoSuchFieldError e63) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FocusMode[CameraCapabilities.FocusMode.CONTINUOUS_VIDEO.ordinal()] = 3;
            } catch (NoSuchFieldError e64) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FocusMode[CameraCapabilities.FocusMode.EXTENDED_DOF.ordinal()] = 4;
            } catch (NoSuchFieldError e65) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FocusMode[CameraCapabilities.FocusMode.FIXED.ordinal()] = 5;
            } catch (NoSuchFieldError e66) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FocusMode[CameraCapabilities.FocusMode.MACRO.ordinal()] = 6;
            } catch (NoSuchFieldError e67) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FocusMode[CameraCapabilities.FocusMode.MANUAL.ordinal()] = 7;
            } catch (NoSuchFieldError e68) {
            }
            $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FlashMode = new int[CameraCapabilities.FlashMode.values().length];
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FlashMode[CameraCapabilities.FlashMode.AUTO.ordinal()] = 1;
            } catch (NoSuchFieldError e69) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FlashMode[CameraCapabilities.FlashMode.OFF.ordinal()] = 2;
            } catch (NoSuchFieldError e70) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FlashMode[CameraCapabilities.FlashMode.ON.ordinal()] = 3;
            } catch (NoSuchFieldError e71) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FlashMode[CameraCapabilities.FlashMode.TORCH.ordinal()] = 4;
            } catch (NoSuchFieldError e72) {
            }
            try {
                $SwitchMap$com$android$ex$camera2$portability$CameraCapabilities$FlashMode[CameraCapabilities.FlashMode.RED_EYE.ordinal()] = 5;
            } catch (NoSuchFieldError e73) {
            }
        }
    }

    private void updateRequestGpsData() {
        if (this.mGpsData == null || this.mGpsData.processingMethod == null) {
            this.mRequestSettings.set(CaptureRequest.JPEG_GPS_LOCATION, null);
            return;
        }
        Location location = new Location(this.mGpsData.processingMethod);
        location.setTime(this.mGpsData.timeStamp);
        location.setAltitude(this.mGpsData.altitude);
        location.setLatitude(this.mGpsData.latitude);
        location.setLongitude(this.mGpsData.longitude);
        this.mRequestSettings.set(CaptureRequest.JPEG_GPS_LOCATION, location);
    }

    private static Rect effectiveCropRectFromRequested(Rect requestedCrop, Size previewSize) {
        float cropWidth;
        float cropHeight;
        float aspectRatioArray = (requestedCrop.width() * 1.0f) / requestedCrop.height();
        float aspectRatioPreview = (previewSize.width() * 1.0f) / previewSize.height();
        if (aspectRatioPreview < aspectRatioArray) {
            cropHeight = requestedCrop.height();
            cropWidth = cropHeight * aspectRatioPreview;
        } else {
            cropWidth = requestedCrop.width();
            cropHeight = cropWidth / aspectRatioPreview;
        }
        Matrix translateMatrix = new Matrix();
        RectF cropRect = new RectF(0.0f, 0.0f, cropWidth, cropHeight);
        translateMatrix.setTranslate(requestedCrop.exactCenterX(), requestedCrop.exactCenterY());
        translateMatrix.postTranslate(-cropRect.centerX(), -cropRect.centerY());
        translateMatrix.mapRect(cropRect);
        Rect result = new Rect();
        cropRect.roundOut(result);
        return result;
    }

    public void setFaceDetection(int faceDetectionType) {
        if (faceDetectionType == 4) {
            this.mCurrentSceneMode = CameraCapabilities.SceneMode.SMILE;
        } else if (faceDetectionType == 2) {
            this.mCurrentSceneMode = CameraCapabilities.SceneMode.FACEDETECT;
        }
    }
}
