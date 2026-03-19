package com.mediatek.camera.feature.setting.zoom;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.view.Surface;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.zoom.IZoomConfig;
import java.util.List;
import java.util.Locale;

@TargetApi(21)
public class ZoomCaptureRequestConfig implements ICameraSetting.ICaptureRequestConfigure, IZoomConfig {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ZoomCaptureRequestConfig.class.getSimpleName());
    private double mDistanceRatio;
    private boolean mIsUserInteraction;
    private float mMaxZoom;
    private Rect mSensorRect;
    private ISettingManager.SettingDevice2Requester mSettingDevice2Requester;
    private IZoomConfig.OnZoomLevelUpdateListener mZoomUpdateListener;
    private float mLastZoomRatio = -1.0f;
    private float mBasicZoomRatio = 1.0f;
    private float mCurZoomRatio = 1.0f;

    public ZoomCaptureRequestConfig(ISettingManager.SettingDevice2Requester settingDevice2Requester) {
        this.mSettingDevice2Requester = settingDevice2Requester;
    }

    @Override
    public void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics) {
        this.mSensorRect = (Rect) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        this.mMaxZoom = ((Float) cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)).floatValue();
        LogHelper.d(TAG, "[setCameraCharacteristics] MaxZoom: " + this.mMaxZoom);
    }

    @Override
    public void configCaptureRequest(CaptureRequest.Builder builder) {
        if (builder == null) {
            LogHelper.d(TAG, "[configCaptureRequest] captureBuilder is null");
            return;
        }
        if ("off".equals(this.mZoomUpdateListener.onGetOverrideValue())) {
            LogHelper.d(TAG, "[configCaptureRequest] this: " + this);
            reset(builder);
            return;
        }
        this.mCurZoomRatio = calculateZoomRatio(this.mDistanceRatio);
        builder.set(CaptureRequest.SCALER_CROP_REGION, cropRegionForZoom(this.mCurZoomRatio));
        this.mLastZoomRatio = this.mCurZoomRatio;
        if (this.mIsUserInteraction) {
            this.mZoomUpdateListener.onZoomLevelUpdate(getPatternRatio());
        }
        LogHelper.d(TAG, "[configCaptureRequest] this: " + this + ", mCurZoomRatio = " + this.mCurZoomRatio + ", mDistanceRatio = " + this.mDistanceRatio);
    }

    @Override
    public void configSessionSurface(List<Surface> list) {
    }

    @Override
    public Surface configRawSurface() {
        return null;
    }

    @Override
    public CameraCaptureSession.CaptureCallback getRepeatingCaptureCallback() {
        return null;
    }

    @Override
    public void sendSettingChangeRequest() {
        if (isZoomValid()) {
            LogHelper.d(TAG, "[sendSettingChangeRequest]");
            this.mSettingDevice2Requester.createAndChangeRepeatingRequest();
        }
    }

    public void setZoomUpdateListener(IZoomConfig.OnZoomLevelUpdateListener onZoomLevelUpdateListener) {
        this.mZoomUpdateListener = onZoomLevelUpdateListener;
    }

    @Override
    public void onScalePerformed(double d) {
        this.mDistanceRatio = d;
    }

    @Override
    public void onScaleStatus(boolean z) {
        this.mIsUserInteraction = z;
        this.mDistanceRatio = 0.0d;
        calculateBasicRatio();
    }

    private boolean isZoomValid() {
        LogHelper.d(TAG, "[isZoomValid] mCurZoomRatio = " + this.mCurZoomRatio + ", zoomRatio = " + calculateZoomRatio(this.mDistanceRatio) + ", mLastZoomRatio = " + this.mLastZoomRatio);
        boolean z = this.mCurZoomRatio >= 1.0f && this.mCurZoomRatio <= this.mMaxZoom && calculateZoomRatio(this.mDistanceRatio) != this.mLastZoomRatio;
        LogHelper.d(TAG, "[isZoomValid] needZoom = " + z);
        return z;
    }

    private void calculateBasicRatio() {
        if (this.mLastZoomRatio == -1.0f) {
            this.mBasicZoomRatio = 1.0f;
        } else {
            this.mBasicZoomRatio = this.mLastZoomRatio;
        }
    }

    private Rect cropRegionForZoom(float f) {
        int iWidth = this.mSensorRect.width() / 2;
        int iHeight = this.mSensorRect.height() / 2;
        int iWidth2 = (int) ((this.mSensorRect.width() * 0.5f) / f);
        int iHeight2 = (int) ((0.5f * this.mSensorRect.height()) / f);
        return new Rect(iWidth - iWidth2, iHeight - iHeight2, iWidth + iWidth2, iHeight + iHeight2);
    }

    private void reset(CaptureRequest.Builder builder) {
        LogHelper.d(TAG, "[reset]");
        builder.set(CaptureRequest.SCALER_CROP_REGION, cropRegionForZoom(1.0f));
        this.mLastZoomRatio = 1.0f;
    }

    private String getPatternRatio() {
        return "x" + String.format(Locale.ENGLISH, "%.1f", Float.valueOf(this.mCurZoomRatio));
    }

    private float calculateZoomRatio(double d) {
        float f = this.mMaxZoom;
        float f2 = (float) (((double) this.mBasicZoomRatio) + (((double) (f - 1.0f)) * d));
        if (f2 <= 1.0f) {
            return 1.0f;
        }
        return f2 >= f ? f : f2;
    }
}
