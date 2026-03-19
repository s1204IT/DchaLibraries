package com.mediatek.camera.feature.mode.panorama;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Camera;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.mode.ICameraMode;

public class PanoramaEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PanoramaEntry.class.getSimpleName());

    public PanoramaEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        boolean z;
        if (isPlatformSupport() && !isThirdPartyIntent(activity)) {
            z = true;
        } else {
            z = false;
        }
        LogHelper.d(TAG, "[isSupport] : " + z);
        return z;
    }

    public String getFeatureEntryName() {
        return PanoramaEntry.class.getName();
    }

    @Override
    public Class getType() {
        return ICameraMode.class;
    }

    @Override
    public Object createInstance() {
        return new PanoramaMode();
    }

    @Override
    public IAppUi.ModeItem getModeItem() {
        IAppUi.ModeItem modeItem = new IAppUi.ModeItem();
        modeItem.mModeUnselectedIcon = this.mContext.getResources().getDrawable(R.drawable.ic_panorama_mode_unselected);
        modeItem.mModeSelectedIcon = this.mContext.getResources().getDrawable(R.drawable.ic_panorama_mode_selected);
        modeItem.mType = "Picture";
        modeItem.mPriority = 50;
        modeItem.mClassName = getFeatureEntryName();
        modeItem.mModeName = this.mContext.getString(R.string.pano_dialog_title);
        modeItem.mSupportedCameraIds = new String[]{"0"};
        return modeItem;
    }

    private boolean isPlatformSupport() {
        Camera.Parameters parameters;
        if (CameraDeviceManagerFactory.CameraApi.API1.equals(this.mDefaultCameraApi) && (parameters = this.mDeviceSpec.getDeviceDescriptionMap().get("0").getParameters()) != null && parameters.get("cap-mode-values") != null) {
            boolean zContains = parameters.get("cap-mode-values").contains("autorama");
            LogHelper.d(TAG, "isSupport = " + zContains);
            return zContains;
        }
        return false;
    }
}
