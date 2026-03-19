package com.mediatek.camera.feature.setting.shutterspeed;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.camera2.CameraCharacteristics;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;
import java.util.concurrent.ConcurrentHashMap;

public class ShutterSpeedEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ShutterSpeedEntry.class.getSimpleName());
    private String mCameraId;

    public ShutterSpeedEntry(Context context, Resources resources) {
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
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (isThirdPartyIntent(activity) || !isSupportInAPI2(this.mCameraId)) {
                }
                break;
        }
        return false;
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        return new ShutterSpeed();
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
        return ShutterSpeedHelper.isShutterSpeedSupported(cameraCharacteristics);
    }
}
