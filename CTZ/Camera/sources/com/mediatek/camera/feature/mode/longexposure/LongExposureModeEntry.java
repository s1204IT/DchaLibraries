package com.mediatek.camera.feature.mode.longexposure;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Range;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.mode.ICameraMode;
import java.util.concurrent.ConcurrentHashMap;

public class LongExposureModeEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(LongExposureModeEntry.class.getSimpleName());
    private String mCameraId;

    public LongExposureModeEntry(Context context, Resources resources) {
        super(context, resources);
        this.mCameraId = "0";
    }

    @Override
    public void notifyBeforeOpenCamera(String str, CameraDeviceManagerFactory.CameraApi cameraApi) {
        super.notifyBeforeOpenCamera(str, cameraApi);
        this.mCameraId = str;
        LogHelper.d(TAG, "[notifyBeforeOpenCamera] mCameraId = " + this.mCameraId);
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi = new int[CameraDeviceManagerFactory.CameraApi.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi[CameraDeviceManagerFactory.CameraApi.API1.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi[CameraDeviceManagerFactory.CameraApi.API2.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi[cameraApi.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                Camera.Parameters parameters = this.mDeviceSpec.getDeviceDescriptionMap().get(this.mCameraId).getParameters();
                if (isThirdPartyIntent(activity) || !isSupportInAPI1(parameters)) {
                }
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (isThirdPartyIntent(activity) || !isSupportInAPI2(this.mCameraId)) {
                }
                break;
        }
        return false;
    }

    public String getFeatureEntryName() {
        return LongExposureModeEntry.class.getName();
    }

    @Override
    public Class getType() {
        return ICameraMode.class;
    }

    @Override
    public Object createInstance() {
        return new LongExposureMode();
    }

    @Override
    public IAppUi.ModeItem getModeItem() {
        IAppUi.ModeItem modeItem = new IAppUi.ModeItem();
        modeItem.mType = "Picture";
        modeItem.mModeUnselectedIcon = this.mContext.getResources().getDrawable(R.drawable.ic_exposure_mode_unselected);
        modeItem.mModeSelectedIcon = this.mContext.getResources().getDrawable(R.drawable.ic_exposure_mode_selected);
        modeItem.mPriority = 35;
        modeItem.mClassName = getFeatureEntryName();
        modeItem.mModeName = String.valueOf(this.mResources.getString(R.string.long_exposure_motion_title));
        modeItem.mSupportedCameraIds = new String[]{"0"};
        return modeItem;
    }

    private boolean isSupportInAPI2(String str) {
        ConcurrentHashMap<String, DeviceDescription> deviceDescriptionMap = this.mDeviceSpec.getDeviceDescriptionMap();
        if (str == null || deviceDescriptionMap == null || deviceDescriptionMap.size() <= 0) {
            LogHelper.w(TAG, "[isSupportInAPI2] cameraId = " + str + ",deviceDescriptionMap " + deviceDescriptionMap);
            return false;
        }
        if (!deviceDescriptionMap.containsKey(str)) {
            LogHelper.w(TAG, "[isSupportInAPI2] cameraId " + str + " does not in device map");
            return false;
        }
        CameraCharacteristics cameraCharacteristics = deviceDescriptionMap.get(str).getCameraCharacteristics();
        if (cameraCharacteristics == null) {
            LogHelper.d(TAG, "[isSupportInAPI2] characteristics is null");
            return false;
        }
        return isLongExposureSupported(cameraCharacteristics);
    }

    private boolean isLongExposureSupported(CameraCharacteristics cameraCharacteristics) {
        if (cameraCharacteristics == null) {
            LogHelper.w(TAG, "characteristics is null");
            return false;
        }
        Range range = (Range) cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        LogHelper.d(TAG, "");
        return range != null && ((Long) range.getUpper()).longValue() >= 1000000000;
    }

    private boolean isSupportInAPI1(Camera.Parameters parameters) {
        if (parameters == null) {
            LogHelper.i(TAG, "[isSupportInAPI1] parameters is null!");
            return false;
        }
        return isLongExposureSupported(parameters);
    }

    private boolean isLongExposureSupported(Camera.Parameters parameters) {
        if (parameters == null) {
            LogHelper.w(TAG, "[isLongExposureSupported] originalParameters is null");
            return false;
        }
        if (!(parameters.get("manual-cap-values") != null && parameters.get("manual-cap-values").contains("on"))) {
            LogHelper.w(TAG, "[isLongExposureSupported] isManualCapSupported is false");
            return false;
        }
        String str = parameters.get("max-exposure-time");
        if (str == null) {
            LogHelper.w(TAG, "[isLongExposureSupported] maxExposureTime is null");
            return false;
        }
        LogHelper.w(TAG, "[isLongExposureSupported] maxExposureTime = " + str);
        return str != null && ((long) (Integer.parseInt(str) / 1000)) >= 1;
    }
}
