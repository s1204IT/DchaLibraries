package com.mediatek.camera.feature.setting.facedetection;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.DeviceDescription;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.portability.SystemProperties;
import java.util.concurrent.ConcurrentHashMap;

public class FaceDetectionEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FaceDetectionEntry.class.getSimpleName());
    private String mCameraId;

    public FaceDetectionEntry(Context context, Resources resources) {
        super(context, resources);
        this.mCameraId = "0";
    }

    @Override
    public void notifyBeforeOpenCamera(String str, CameraDeviceManagerFactory.CameraApi cameraApi) {
        super.notifyBeforeOpenCamera(str, cameraApi);
        this.mCameraId = str;
        LogHelper.d(TAG, "[notifyBeforeOpenCamera] mCameraId = " + this.mCameraId);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        if (SystemProperties.getInt("vendor.mtk.camera.app.fd.disable", 0) == 1) {
            LogHelper.d(TAG, "[isSupport] has set vendor.mtk.camera.app.fd.disable as 1, return false");
            return false;
        }
        switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi[cameraApi.ordinal()]) {
        }
        return false;
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
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        return new FaceDetection();
    }

    private boolean isSupportInAPI1(String str) {
        ConcurrentHashMap<String, DeviceDescription> deviceDescriptionMap = this.mDeviceSpec.getDeviceDescriptionMap();
        if (str == null || deviceDescriptionMap == null || deviceDescriptionMap.size() <= 0) {
            LogHelper.d(TAG, "[isSupportInAPI1] cameraId = " + str + ", return false 1");
            return false;
        }
        if (!deviceDescriptionMap.containsKey(str)) {
            LogHelper.d(TAG, "[isSupportInAPI1] cameraId = " + str + ", return false 2");
            return false;
        }
        Camera.Parameters parameters = deviceDescriptionMap.get(str).getParameters();
        if (parameters != null) {
            return parameters.getMaxNumDetectedFaces() > 0;
        }
        LogHelper.d(TAG, "[isSupportInAPI1] cameraId = " + str + ", return false 3");
        return false;
    }

    private boolean isSupportInAPI2(String str) {
        ConcurrentHashMap<String, DeviceDescription> deviceDescriptionMap = this.mDeviceSpec.getDeviceDescriptionMap();
        if (str == null || deviceDescriptionMap == null || deviceDescriptionMap.size() <= 0) {
            LogHelper.d(TAG, "[isSupportInAPI2] cameraId = " + str + ", return false 1");
            return false;
        }
        if (!deviceDescriptionMap.containsKey(str)) {
            LogHelper.d(TAG, "[isSupportInAPI2] cameraId = " + str + ", return false 2");
            return false;
        }
        CameraCharacteristics cameraCharacteristics = deviceDescriptionMap.get(str).getCameraCharacteristics();
        if (cameraCharacteristics == null) {
            LogHelper.d(TAG, "[isSupportInAPI2] cameraId = " + str + ", return false 3");
            return false;
        }
        return FaceCaptureRequestConfig.isFaceDetectionSupported(cameraCharacteristics);
    }
}
