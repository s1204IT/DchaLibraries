package com.mediatek.camera.feature.setting;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;

public class CameraSwitcherEntry extends FeatureEntryBase {
    private CameraSwitcher mCameraSwitcher;

    public CameraSwitcherEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        return true;
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        if (this.mCameraSwitcher == null) {
            this.mCameraSwitcher = new CameraSwitcher();
        }
        return this.mCameraSwitcher;
    }
}
