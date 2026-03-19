package com.mediatek.camera.feature.setting.zoom;

import android.hardware.Camera;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraProxy;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.common.setting.ISettingManager;
import com.mediatek.camera.feature.setting.zoom.IZoomConfig;
import java.util.List;
import java.util.Locale;

public class ZoomParameterConfig implements ICameraSetting.IParametersConfigure, IZoomConfig {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ZoomParameterConfig.class.getSimpleName());
    private int mBasicZoomRatio;
    private CameraProxy mCameraProxy;
    private double mDistanceRatio;
    private boolean mIsSmoothZoomSupported;
    private boolean mIsUserInteraction;
    private boolean mIsZoomSupported;
    private int mMaxZoom;
    private ISettingManager.SettingDeviceRequester mSettingDeviceRequester;
    private int mZoomLevel;
    private List<Integer> mZoomRatios;
    private IZoomConfig.OnZoomLevelUpdateListener mZoomUpdateListener;
    private int mLastZoomLevel = -1;
    private boolean mIsZoomStopped = true;
    private Object mSyncSmoothState = new Object();
    private final Camera.OnZoomChangeListener mZoomListener = new Camera.OnZoomChangeListener() {
        @Override
        public void onZoomChange(int i, boolean z, Camera camera) {
            LogHelper.d(ZoomParameterConfig.TAG, "[onZoomChange] zoomValue = " + i + ", stopped = " + z);
            if (z) {
                synchronized (ZoomParameterConfig.this.mSyncSmoothState) {
                    ZoomParameterConfig.this.mIsZoomStopped = true;
                    ZoomParameterConfig.this.mCameraProxy.stopSmoothZoom();
                }
            }
        }
    };

    public ZoomParameterConfig(ISettingManager.SettingDeviceRequester settingDeviceRequester) {
        this.mSettingDeviceRequester = settingDeviceRequester;
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

    public void setZoomUpdateListener(IZoomConfig.OnZoomLevelUpdateListener onZoomLevelUpdateListener) {
        this.mZoomUpdateListener = onZoomLevelUpdateListener;
    }

    @Override
    public void setOriginalParameters(Camera.Parameters parameters) {
        this.mZoomRatios = parameters.getZoomRatios();
        this.mIsZoomSupported = parameters.isZoomSupported();
        this.mIsSmoothZoomSupported = parameters.isSmoothZoomSupported();
        this.mMaxZoom = parameters.getMaxZoom();
        LogHelper.d(TAG, "[setOriginalParameters] mIsZoomSupported = " + this.mIsZoomSupported);
    }

    @Override
    public boolean configParameters(Camera.Parameters parameters) {
        if (!this.mIsZoomSupported) {
            return false;
        }
        if ("off".equals(this.mZoomUpdateListener.onGetOverrideValue())) {
            reset(parameters);
            return false;
        }
        this.mZoomLevel = calculateZoomLevel(this.mDistanceRatio);
        parameters.setZoom(this.mZoomLevel);
        this.mLastZoomLevel = this.mZoomLevel;
        if (this.mIsUserInteraction) {
            this.mZoomUpdateListener.onZoomLevelUpdate(getZoomRatio());
        }
        LogHelper.d(TAG, "[configParameters] this: " + this + ", mZoomLevel = " + this.mZoomLevel + ", mDistanceRatio = " + this.mDistanceRatio);
        return false;
    }

    @Override
    public void configCommand(CameraProxy cameraProxy) {
        if (!this.mIsSmoothZoomSupported) {
            return;
        }
        this.mCameraProxy = cameraProxy;
        this.mZoomLevel = calculateZoomLevel(this.mDistanceRatio);
        cameraProxy.setZoomChangeListener(this.mZoomListener);
        synchronized (this.mSyncSmoothState) {
            cameraProxy.startSmoothZoom(this.mZoomLevel);
            this.mIsZoomStopped = false;
            this.mLastZoomLevel = this.mZoomLevel;
        }
        if (this.mIsUserInteraction) {
            this.mZoomUpdateListener.onZoomLevelUpdate(getZoomRatio());
        }
        LogHelper.d(TAG, "[configCommand] mZoomLevel = " + this.mZoomLevel + ", mDistanceRatio = " + this.mDistanceRatio + ", cameraProxy = " + cameraProxy);
    }

    @Override
    public void sendSettingChangeRequest() {
        if (isZoomValid()) {
            if (this.mIsZoomSupported) {
                LogHelper.d(TAG, "[sendSettingChangeRequest]");
                this.mSettingDeviceRequester.requestChangeSettingValue("key_camera_zoom");
            } else if (this.mIsSmoothZoomSupported) {
                this.mSettingDeviceRequester.requestChangeCommand("key_camera_zoom");
            }
        }
    }

    private boolean isZoomValid() {
        return this.mZoomLevel >= 0 && this.mZoomLevel <= this.mMaxZoom && calculateZoomLevel(this.mDistanceRatio) != this.mLastZoomLevel && this.mIsZoomStopped;
    }

    private void reset(Camera.Parameters parameters) {
        LogHelper.i(TAG, "[reset]");
        parameters.setZoom(0);
        this.mLastZoomLevel = 0;
    }

    private void calculateBasicRatio() {
        if (this.mZoomRatios != null) {
            if (this.mLastZoomLevel == -1) {
                this.mBasicZoomRatio = this.mZoomRatios.get(0).intValue();
            } else {
                this.mBasicZoomRatio = this.mZoomRatios.get(this.mLastZoomLevel).intValue();
            }
        }
        LogHelper.d(TAG, "[calculateBasicRatio] mBasicZoomRatio = " + this.mBasicZoomRatio + ", mLastZoomLevel = " + this.mLastZoomLevel);
    }

    private int calculateZoomLevel(double d) {
        if (this.mZoomRatios == null) {
            return 0;
        }
        int size = this.mZoomRatios.size();
        int i = size - 1;
        int iIntValue = this.mZoomRatios.get(i).intValue();
        int iIntValue2 = this.mZoomRatios.get(0).intValue();
        if (size == 1) {
        }
        int i2 = (int) (((double) this.mBasicZoomRatio) + (((double) (iIntValue - iIntValue2)) * d));
        if (i2 <= iIntValue2) {
            return 0;
        }
        if (i2 < iIntValue) {
            int i3 = 0;
            while (i3 < i) {
                int iIntValue3 = this.mZoomRatios.get(i3).intValue();
                int i4 = i3 + 1;
                int iIntValue4 = this.mZoomRatios.get(i4).intValue();
                if (i2 < iIntValue3 || i2 >= iIntValue4) {
                    i3 = i4;
                } else {
                    return i3;
                }
            }
            return 0;
        }
        return i;
    }

    private String getZoomRatio() {
        float fIntValue;
        if (this.mZoomRatios != null) {
            fIntValue = this.mZoomRatios.get(this.mLastZoomLevel).intValue() / 100.0f;
        } else {
            fIntValue = 1.0f;
        }
        return "x" + String.format(Locale.ENGLISH, "%.1f", Float.valueOf(fIntValue));
    }
}
