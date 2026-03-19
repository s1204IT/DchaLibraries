package com.mediatek.camera.common.mode.photo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.mode.CameraApiHelper;
import com.mediatek.camera.common.mode.ICameraMode;

public class PhotoModeEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(PhotoModeEntry.class.getSimpleName());

    public PhotoModeEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        String action = activity.getIntent().getAction();
        boolean z = ("android.media.action.IMAGE_CAPTURE".equals(action) || "android.media.action.VIDEO_CAPTURE".equals(action)) ? false : true;
        LogHelper.i(TAG, "[isSupport] : " + z);
        return z;
    }

    public String getFeatureEntryName() {
        return PhotoModeEntry.class.getName();
    }

    @Override
    public Class getType() {
        return ICameraMode.class;
    }

    @Override
    public Object createInstance() {
        return new PhotoMode();
    }

    @Override
    public IAppUi.ModeItem getModeItem() {
        IAppUi.ModeItem modeItem = new IAppUi.ModeItem();
        modeItem.mType = "Picture";
        modeItem.mPriority = 10;
        modeItem.mClassName = getFeatureEntryName();
        modeItem.mModeName = this.mResources.getString(this.mResources.getIdentifier("normal_mode_title", "string", this.mContext.getPackageName()));
        if (CameraApiHelper.getLogicalCameraId() == null) {
            modeItem.mSupportedCameraIds = new String[]{"0", "1"};
        } else {
            modeItem.mSupportedCameraIds = new String[]{"0", "1", CameraApiHelper.getLogicalCameraId()};
        }
        return modeItem;
    }
}
