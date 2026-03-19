package com.mediatek.camera.common.device;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v1.CameraDeviceManagerImpl;
import com.mediatek.camera.common.device.v2.Camera2DeviceManagerImpl;
import com.mediatek.camera.common.device.v2.Camera2Proxy;

public class CameraDeviceManagerFactory {
    private static final int FIRST_SDK_WITH_API_2 = 21;
    private static final LogUtil.Tag TAG = new LogUtil.Tag("CamDeviceMgrFac");
    private static CameraDeviceManagerFactory sCameraDeviceManagerFactory;
    private static Activity sCurrentActivity;
    private CameraDeviceManager mCamera2DeviceManager;
    private CameraDeviceManager mCameraDeviceManager;

    public enum CameraApi {
        API1,
        API2
    }

    private CameraDeviceManagerFactory() {
    }

    public static synchronized CameraDeviceManagerFactory getInstance() {
        if (sCameraDeviceManagerFactory == null) {
            sCameraDeviceManagerFactory = new CameraDeviceManagerFactory();
        }
        return sCameraDeviceManagerFactory;
    }

    public synchronized CameraDeviceManager getCameraDeviceManager(Context context, CameraApi cameraApi) throws UnsupportedOperationException {
        LogHelper.d(TAG, "getCameraDeviceManager,context = " + context + ",api = " + cameraApi);
        checkConditionsBeforeGetManager(context, cameraApi);
        switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi[cameraApi.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                return getApi1DeviceManager(context);
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                return getApi2DeviceManager(context);
            default:
                throw new UnsupportedOperationException("Get camera device manager,the API: " + cameraApi + " version don't support");
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi = new int[CameraApi.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi[CameraApi.API1.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$common$device$CameraDeviceManagerFactory$CameraApi[CameraApi.API2.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public static void setCurrentActivity(Activity activity) {
        sCurrentActivity = activity;
    }

    public static synchronized void release(Activity activity) {
        if (sCurrentActivity != activity) {
            LogHelper.i(TAG, "[release] return for other activity is used");
            return;
        }
        if (sCameraDeviceManagerFactory != null) {
            sCameraDeviceManagerFactory.releaseAllCameraDeviceManagerInstance();
            sCameraDeviceManagerFactory = null;
        }
        sCurrentActivity = null;
    }

    private void releaseAllCameraDeviceManagerInstance() {
        if (this.mCamera2DeviceManager != null) {
            this.mCamera2DeviceManager.recycle();
            this.mCamera2DeviceManager = null;
        }
    }

    private void checkConditionsBeforeGetManager(Context context, CameraApi cameraApi) {
        if (cameraApi == null) {
            throw new UnsupportedOperationException("Get camera device manager,API version is not allowed to null");
        }
        if (context == null && CameraApi.API2 == cameraApi) {
            throw new UnsupportedOperationException("Get camera device manager with api 2 must need context");
        }
    }

    private CameraDeviceManager getApi1DeviceManager(Context context) {
        if (this.mCameraDeviceManager == null) {
            this.mCameraDeviceManager = new CameraDeviceManagerImpl(context);
        }
        LogHelper.d(TAG, "getApi1DeviceManager: " + this.mCameraDeviceManager);
        return this.mCameraDeviceManager;
    }

    private CameraDeviceManager getApi2DeviceManager(Context context) {
        if (CameraApi.API2 == getHighestSupportedApi()) {
            if (this.mCamera2DeviceManager == null) {
                this.mCamera2DeviceManager = new Camera2DeviceManagerImpl(context);
            }
            LogHelper.d(TAG, "getApi2DeviceManager: " + this.mCamera2DeviceManager);
            return this.mCamera2DeviceManager;
        }
        throw new UnsupportedOperationException("Get camera device manager,API2 is not supported on this project");
    }

    private CameraApi getHighestSupportedApi() {
        if (Build.VERSION.SDK_INT >= FIRST_SDK_WITH_API_2 || Build.VERSION.CODENAME.equals("L")) {
            return CameraApi.API2;
        }
        return CameraApi.API1;
    }
}
