package com.android.ex.camera2.portability;

import android.hardware.Camera;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.debug.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class CameraSettings {
    public static final int CAMERA_FACIALDETECT_FACE = 2;
    public static final int CAMERA_FACIALDETECT_SMILE = 4;
    private static final int MAX_JPEG_COMPRESSION_QUALITY = 100;
    private static final int MIN_JPEG_COMPRESSION_QUALITY = 1;
    private static final Log.Tag TAG = new Log.Tag("CamSet");
    public double mAGain;
    protected CameraCapabilities.AntiBanding mAntiBanding;
    protected boolean mAutoExposureLocked;
    protected boolean mAutoWhiteBalanceLocked;
    protected double mBGain;
    protected int mBestIndex;
    protected int mBrightness;
    protected CameraCapabilities.BurstCapture mBurstCaptureMode;
    protected int mBurstCaptureNum;
    protected CameraCapabilities.ColorEffect mColorEffect;
    protected String mColorEffectParam;
    protected int mContrast;
    protected CameraCapabilities.FlashMode mCurrentFlashMode;
    protected CameraCapabilities.FocusMode mCurrentFocusMode;
    protected int mCurrentPhotoFormat;
    protected Size mCurrentPhotoSize;
    private int mCurrentPreviewFormat;
    protected Size mCurrentPreviewSize;
    protected CameraCapabilities.SceneMode mCurrentSceneMode;
    protected float mCurrentZoomRatio;
    protected double mDGain;
    protected int mDnsTimes;
    protected Size mExifThumbnailSize;
    protected int mExposureCompensationIndex;
    protected int mExposureMode;
    public double mExposureTime;
    protected int mFaceDetectionType;
    public int mFocusPosition;
    protected double mGbGain;
    protected GpsData mGpsData;
    protected double mGrGain;
    protected boolean mIsFaceBeautify;
    protected CameraCapabilities.IsoMode mIsoMode;
    protected double mIspGain;
    protected byte mJpegCompressQuality;
    protected int mPreviewFpsRangeMax;
    protected int mPreviewFpsRangeMin;
    protected int mPreviewFrameRate;
    protected double mRGain;
    protected boolean mRecordingHintEnabled;
    protected int mSaturation;
    protected int mSharpness;
    protected boolean mSizesLocked;
    protected boolean mUVDns;
    protected boolean mVideoStabilizationEnabled;
    protected boolean mVideoTnrEnabled;
    protected CameraCapabilities.WhiteBalance mWhiteBalance;
    protected boolean mYDns;
    protected final Map<String, String> mGeneralSetting = new TreeMap();
    protected final List<Camera.Area> mMeteringAreas = new ArrayList();
    protected final List<Camera.Area> mFocusAreas = new ArrayList();

    public abstract CameraSettings copy();

    public static class GpsData {
        public final double altitude;
        public final double latitude;
        public final double longitude;
        public final String processingMethod;
        public final long timeStamp;

        public GpsData(double latitude, double longitude, double altitude, long timeStamp, String processingMethod) {
            if (processingMethod == null && (latitude != 0.0d || longitude != 0.0d || altitude != 0.0d)) {
                Log.w(CameraSettings.TAG, "GpsData's nonzero data will be ignored due to null processingMethod");
            }
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.timeStamp = timeStamp;
            this.processingMethod = processingMethod;
        }

        public GpsData(GpsData src) {
            this.latitude = src.latitude;
            this.longitude = src.longitude;
            this.altitude = src.altitude;
            this.timeStamp = src.timeStamp;
            this.processingMethod = src.processingMethod;
        }
    }

    protected CameraSettings() {
    }

    protected CameraSettings(CameraSettings src) {
        this.mGeneralSetting.putAll(src.mGeneralSetting);
        this.mMeteringAreas.addAll(src.mMeteringAreas);
        this.mFocusAreas.addAll(src.mFocusAreas);
        this.mSizesLocked = src.mSizesLocked;
        this.mPreviewFpsRangeMin = src.mPreviewFpsRangeMin;
        this.mPreviewFpsRangeMax = src.mPreviewFpsRangeMax;
        this.mPreviewFrameRate = src.mPreviewFrameRate;
        this.mCurrentPreviewSize = src.mCurrentPreviewSize == null ? null : new Size(src.mCurrentPreviewSize);
        this.mCurrentPreviewFormat = src.mCurrentPreviewFormat;
        this.mCurrentPhotoSize = src.mCurrentPhotoSize != null ? new Size(src.mCurrentPhotoSize) : null;
        this.mJpegCompressQuality = src.mJpegCompressQuality;
        this.mCurrentPhotoFormat = src.mCurrentPhotoFormat;
        this.mCurrentZoomRatio = src.mCurrentZoomRatio;
        this.mExposureCompensationIndex = src.mExposureCompensationIndex;
        this.mCurrentFlashMode = src.mCurrentFlashMode;
        this.mCurrentFocusMode = src.mCurrentFocusMode;
        this.mCurrentSceneMode = src.mCurrentSceneMode;
        this.mWhiteBalance = src.mWhiteBalance;
        this.mColorEffect = src.mColorEffect;
        this.mColorEffectParam = src.mColorEffectParam;
        this.mIsoMode = src.mIsoMode;
        this.mBurstCaptureMode = src.mBurstCaptureMode;
        this.mBurstCaptureNum = src.mBurstCaptureNum;
        this.mAntiBanding = src.mAntiBanding;
        this.mVideoStabilizationEnabled = src.mVideoStabilizationEnabled;
        this.mAutoExposureLocked = src.mAutoExposureLocked;
        this.mAutoWhiteBalanceLocked = src.mAutoWhiteBalanceLocked;
        this.mRecordingHintEnabled = src.mRecordingHintEnabled;
        this.mGpsData = src.mGpsData;
        this.mExifThumbnailSize = src.mExifThumbnailSize;
        this.mContrast = src.mContrast;
        this.mSaturation = src.mSaturation;
        this.mBrightness = src.mBrightness;
        this.mSharpness = src.mSharpness;
        this.mFaceDetectionType = src.mFaceDetectionType;
        this.mBestIndex = src.mBestIndex;
        this.mIsFaceBeautify = src.mIsFaceBeautify;
        this.mExposureTime = src.mExposureTime;
        this.mExposureMode = src.mExposureMode;
        this.mFocusPosition = src.mFocusPosition;
        this.mAGain = src.mAGain;
        this.mDGain = src.mDGain;
        this.mIspGain = src.mIspGain;
        this.mBGain = src.mBGain;
        this.mGbGain = src.mGbGain;
        this.mGrGain = src.mGrGain;
        this.mRGain = src.mRGain;
        this.mYDns = src.mYDns;
        this.mUVDns = src.mUVDns;
        this.mDnsTimes = src.mDnsTimes;
    }

    @Deprecated
    public void setSetting(String key, String value) {
        this.mGeneralSetting.put(key, value);
    }

    void setSizesLocked(boolean locked) {
        this.mSizesLocked = locked;
    }

    public void setPreviewFpsRange(int min, int max) {
        if (min > max) {
            max = min;
            min = max;
        }
        this.mPreviewFpsRangeMax = max;
        this.mPreviewFpsRangeMin = min;
        this.mPreviewFrameRate = -1;
    }

    public int getPreviewFpsRangeMin() {
        return this.mPreviewFpsRangeMin;
    }

    public int getPreviewFpsRangeMax() {
        return this.mPreviewFpsRangeMax;
    }

    public void setPreviewFrameRate(int frameRate) {
        if (frameRate > 0) {
            this.mPreviewFrameRate = frameRate;
            this.mPreviewFpsRangeMax = frameRate;
            this.mPreviewFpsRangeMin = frameRate;
        }
    }

    public int getPreviewFrameRate() {
        return this.mPreviewFrameRate;
    }

    public Size getCurrentPreviewSize() {
        return new Size(this.mCurrentPreviewSize);
    }

    public boolean setPreviewSize(Size previewSize) {
        if (this.mSizesLocked) {
            Log.w(TAG, "Attempt to change preview size while locked");
            return false;
        }
        this.mCurrentPreviewSize = new Size(previewSize);
        return true;
    }

    public void setPreviewFormat(int format) {
        this.mCurrentPreviewFormat = format;
    }

    public void setFaceDetectionType(int type) {
        this.mFaceDetectionType = type;
    }

    public int getCurrentPreviewFormat() {
        return this.mCurrentPreviewFormat;
    }

    public Size getCurrentPhotoSize() {
        return new Size(this.mCurrentPhotoSize);
    }

    public boolean setPhotoSize(Size photoSize) {
        if (this.mSizesLocked) {
            Log.w(TAG, "Attempt to change photo size while locked");
            return false;
        }
        this.mCurrentPhotoSize = new Size(photoSize);
        return true;
    }

    public void setPhotoFormat(int format) {
        this.mCurrentPhotoFormat = format;
    }

    public int getCurrentPhotoFormat() {
        return this.mCurrentPhotoFormat;
    }

    public void setPhotoJpegCompressionQuality(int quality) {
        if (quality < 1 || quality > 100) {
            Log.w(TAG, "Ignoring JPEG quality that falls outside the expected range");
        } else {
            this.mJpegCompressQuality = (byte) quality;
        }
    }

    public int getPhotoJpegCompressionQuality() {
        return this.mJpegCompressQuality;
    }

    public float getCurrentZoomRatio() {
        return this.mCurrentZoomRatio;
    }

    public void setZoomRatio(float ratio) {
        this.mCurrentZoomRatio = ratio;
    }

    public void setExposureCompensationIndex(int index) {
        this.mExposureCompensationIndex = index;
    }

    public void setColorEffect(CameraCapabilities.ColorEffect colorEffect) {
        this.mColorEffect = colorEffect;
    }

    public void setColorEffectParam(CameraCapabilities.ColorEffect colorEffect, String value) {
        this.mColorEffect = colorEffect;
        this.mColorEffectParam = value;
    }

    public CameraCapabilities.ColorEffect getColorEffect() {
        return this.mColorEffect;
    }

    public String getColorEffectParam() {
        return this.mColorEffectParam;
    }

    public int getExposureCompensationIndex() {
        return this.mExposureCompensationIndex;
    }

    public void setAutoExposureLock(boolean locked) {
        this.mAutoExposureLocked = locked;
    }

    public boolean isAutoExposureLocked() {
        return this.mAutoExposureLocked;
    }

    public void setMeteringAreas(List<Camera.Area> areas) {
        this.mMeteringAreas.clear();
        if (areas != null) {
            this.mMeteringAreas.addAll(areas);
        }
    }

    public List<Camera.Area> getMeteringAreas() {
        return new ArrayList(this.mMeteringAreas);
    }

    public CameraCapabilities.FlashMode getCurrentFlashMode() {
        return this.mCurrentFlashMode;
    }

    public void setFlashMode(CameraCapabilities.FlashMode flashMode) {
        this.mCurrentFlashMode = flashMode;
    }

    public void setFocusMode(CameraCapabilities.FocusMode focusMode) {
        this.mCurrentFocusMode = focusMode;
    }

    public CameraCapabilities.FocusMode getCurrentFocusMode() {
        return this.mCurrentFocusMode;
    }

    public void setFocusAreas(List<Camera.Area> areas) {
        this.mFocusAreas.clear();
        if (areas != null) {
            this.mFocusAreas.addAll(areas);
        }
    }

    public List<Camera.Area> getFocusAreas() {
        return new ArrayList(this.mFocusAreas);
    }

    public void setWhiteBalance(CameraCapabilities.WhiteBalance whiteBalance) {
        this.mWhiteBalance = whiteBalance;
    }

    public CameraCapabilities.WhiteBalance getWhiteBalance() {
        return this.mWhiteBalance;
    }

    public void setIsoMode(CameraCapabilities.IsoMode isoMode) {
        this.mIsoMode = isoMode;
    }

    public CameraCapabilities.IsoMode getIsoMode() {
        return this.mIsoMode;
    }

    public void setBurstCaptureMode(CameraCapabilities.BurstCapture burstCaptureMode) {
        this.mBurstCaptureMode = burstCaptureMode;
    }

    public void setBurstCaptureNum(int burstCaptureNum) {
        this.mBurstCaptureNum = burstCaptureNum;
    }

    public CameraCapabilities.BurstCapture getBurstCaptureMode() {
        return this.mBurstCaptureMode;
    }

    public int getBurstCaptureNum() {
        return this.mBurstCaptureNum;
    }

    public int getBestIndex() {
        return this.mBestIndex;
    }

    public void setAntibanding(CameraCapabilities.AntiBanding antiBanding) {
        this.mAntiBanding = antiBanding;
    }

    public CameraCapabilities.AntiBanding getAntibanding() {
        return this.mAntiBanding;
    }

    public void setAutoWhiteBalanceLock(boolean locked) {
        this.mAutoWhiteBalanceLocked = locked;
    }

    public boolean isAutoWhiteBalanceLocked() {
        return this.mAutoWhiteBalanceLocked;
    }

    public CameraCapabilities.SceneMode getCurrentSceneMode() {
        return this.mCurrentSceneMode;
    }

    public void setSceneMode(CameraCapabilities.SceneMode sceneMode) {
        this.mCurrentSceneMode = sceneMode;
    }

    public void setFaceBeautify(boolean faceBeautify) {
        this.mIsFaceBeautify = faceBeautify;
    }

    public boolean getFaceBeautify() {
        return this.mIsFaceBeautify;
    }

    public int getBrightness() {
        return this.mBrightness;
    }

    public void setBrightness(int value) {
        this.mBrightness = value;
    }

    public int getSharpness() {
        return this.mSharpness;
    }

    public void setSharpness(int value) {
        this.mSharpness = value;
    }

    public int getContrast() {
        return this.mContrast;
    }

    public void setContrast(int value) {
        this.mContrast = value;
    }

    public int getSaturation() {
        return this.mSaturation;
    }

    public void setSaturation(int value) {
        this.mSaturation = value;
    }

    public void setExposureMode(int value) {
        this.mExposureMode = value;
    }

    public void setShutterSpeed(double value) {
        this.mExposureTime = value;
    }

    public void setFocusPosition(int value) {
        this.mFocusPosition = value;
    }

    public void setAGain(double value) {
        this.mAGain = value;
    }

    public void setDGain(double value) {
        this.mDGain = value;
    }

    public void setIspGain(double value) {
        this.mIspGain = value;
    }

    public void setBGain(double value) {
        this.mBGain = value;
    }

    public void setGbGain(double value) {
        this.mGbGain = value;
    }

    public void setGrGain(double value) {
        this.mGrGain = value;
    }

    public void setRGain(double value) {
        this.mRGain = value;
    }

    public void setYDns(boolean value) {
        this.mYDns = value;
    }

    public void setUVDns(boolean value) {
        this.mUVDns = value;
    }

    public void setDnsTimes(int value) {
        this.mDnsTimes = value;
    }

    public void setVideoStabilization(boolean enabled) {
        this.mVideoStabilizationEnabled = enabled;
    }

    public boolean isVideoStabilizationEnabled() {
        return this.mVideoStabilizationEnabled;
    }

    public void setVideoTNR(boolean enabled) {
        this.mVideoTnrEnabled = enabled;
    }

    public boolean isVideoTNREnabled() {
        return this.mVideoTnrEnabled;
    }

    public void setRecordingHintEnabled(boolean hintEnabled) {
        this.mRecordingHintEnabled = hintEnabled;
    }

    public boolean isRecordingHintEnabled() {
        return this.mRecordingHintEnabled;
    }

    public void setGpsData(GpsData data) {
        this.mGpsData = new GpsData(data);
    }

    public GpsData getGpsData() {
        if (this.mGpsData == null) {
            return null;
        }
        return new GpsData(this.mGpsData);
    }

    public void clearGpsData() {
        this.mGpsData = null;
    }

    public void setExifThumbnailSize(Size s) {
        this.mExifThumbnailSize = s;
    }

    public Size getExifThumbnailSize() {
        if (this.mExifThumbnailSize == null) {
            return null;
        }
        return new Size(this.mExifThumbnailSize);
    }
}
