package com.mediatek.camera.common.loader;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.mode.DeviceUsage;

public abstract class FeatureEntryBase implements IFeatureEntry {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(FeatureEntryBase.class.getSimpleName());
    protected Context mContext;
    protected CameraDeviceManagerFactory.CameraApi mDefaultCameraApi;
    protected DeviceSpec mDeviceSpec;
    protected Resources mResources;

    public FeatureEntryBase(Context context, Resources resources) {
        if (context instanceof Activity) {
            this.mContext = context.getApplicationContext();
        } else {
            this.mContext = context;
        }
        this.mResources = this.mContext.getResources();
    }

    @Override
    public DeviceUsage updateDeviceUsage(String str, DeviceUsage deviceUsage) {
        return deviceUsage;
    }

    @Override
    public void setDeviceSpec(DeviceSpec deviceSpec) {
        this.mDeviceSpec = deviceSpec;
        this.mDefaultCameraApi = this.mDeviceSpec.getDefaultCameraApi();
    }

    @Override
    public void notifyBeforeOpenCamera(String str, CameraDeviceManagerFactory.CameraApi cameraApi) {
    }

    @Override
    public int getStage() {
        return 2;
    }

    @Override
    public IAppUi.ModeItem getModeItem() {
        return null;
    }

    protected boolean isThirdPartyIntent(Activity activity) {
        String action = activity.getIntent().getAction();
        return "android.media.action.IMAGE_CAPTURE".equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action);
    }
}
