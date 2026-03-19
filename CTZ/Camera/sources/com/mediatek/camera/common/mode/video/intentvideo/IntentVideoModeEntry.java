package com.mediatek.camera.common.mode.video.intentvideo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.IAppUi;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.mode.ICameraMode;

public class IntentVideoModeEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(IntentVideoModeEntry.class.getSimpleName());

    public IntentVideoModeEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        boolean zEquals = "android.media.action.VIDEO_CAPTURE".equals(activity.getIntent().getAction());
        LogHelper.i(TAG, "[isSupport] : " + zEquals);
        return zEquals;
    }

    public String getFeatureEntryName() {
        return IntentVideoModeEntry.class.getName();
    }

    @Override
    public Class getType() {
        return ICameraMode.class;
    }

    @Override
    public Object createInstance() {
        return new IntentVideoMode();
    }

    @Override
    public IAppUi.ModeItem getModeItem() {
        IAppUi.ModeItem modeItem = new IAppUi.ModeItem();
        modeItem.mType = "Video";
        modeItem.mPriority = 10;
        modeItem.mClassName = getFeatureEntryName();
        modeItem.mModeName = "Video";
        modeItem.mSupportedCameraIds = new String[]{"0", "1"};
        return modeItem;
    }
}
