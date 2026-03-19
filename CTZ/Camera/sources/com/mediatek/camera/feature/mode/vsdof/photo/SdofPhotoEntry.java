package com.mediatek.camera.feature.mode.vsdof.photo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.R;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.ICameraMode;

public class SdofPhotoEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SdofPhotoEntry.class.getSimpleName());

    public SdofPhotoEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        if (isThirdPartyIntent(activity)) {
            LogHelper.i(TAG, "[isSupport] false, third party intent.");
            return false;
        }
        if (this.mDeviceSpec.getDeviceDescriptionMap().size() >= 2) {
            return CameraApiHelper.getLogicalCameraId() != null;
        }
        LogHelper.i(TAG, "[isSupport] false, camera ids < 2");
        return false;
    }

    public String getFeatureEntryName() {
        return SdofPhotoEntry.class.getName();
    }

    @Override
    public Class getType() {
        return ICameraMode.class;
    }

    @Override
    public Object createInstance() {
        return new SdofPhotoMode();
    }

    @Override
    public IAppUi.ModeItem getModeItem() {
        IAppUi.ModeItem modeItem = new IAppUi.ModeItem();
        modeItem.mModeSelectedIcon = this.mResources.getDrawable(R.drawable.ic_sdof_mode_selected);
        modeItem.mModeUnselectedIcon = this.mResources.getDrawable(R.drawable.ic_sdof_mode_unselected);
        modeItem.mShutterIcon = null;
        modeItem.mType = "Picture";
        modeItem.mPriority = 20;
        modeItem.mClassName = getFeatureEntryName();
        modeItem.mModeName = this.mResources.getString(R.string.sdof_mode_name);
        if (CameraApiHelper.getLogicalCameraId() == null) {
            modeItem.mSupportedCameraIds = new String[]{"0"};
        } else {
            modeItem.mSupportedCameraIds = new String[]{"0", CameraApiHelper.getLogicalCameraId()};
        }
        return modeItem;
    }
}
