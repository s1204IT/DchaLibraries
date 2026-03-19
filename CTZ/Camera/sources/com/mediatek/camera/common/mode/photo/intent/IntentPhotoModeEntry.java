package com.mediatek.camera.common.mode.photo.intent;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.mode.ICameraMode;

public class IntentPhotoModeEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(IntentPhotoModeEntry.class.getSimpleName());

    public IntentPhotoModeEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        boolean zEquals = "android.media.action.IMAGE_CAPTURE".equals(activity.getIntent().getAction());
        LogHelper.i(TAG, "[isSupport] : " + zEquals);
        return zEquals;
    }

    public String getFeatureEntryName() {
        return IntentPhotoModeEntry.class.getName();
    }

    @Override
    public Class getType() {
        return ICameraMode.class;
    }

    @Override
    public Object createInstance() {
        return new IntentPhotoMode();
    }

    @Override
    public IAppUi.ModeItem getModeItem() {
        IAppUi.ModeItem modeItem = new IAppUi.ModeItem();
        modeItem.mType = "Picture";
        modeItem.mPriority = 10;
        modeItem.mClassName = getFeatureEntryName();
        modeItem.mModeName = "Photo";
        modeItem.mSupportedCameraIds = new String[]{"0", "1"};
        return modeItem;
    }
}
