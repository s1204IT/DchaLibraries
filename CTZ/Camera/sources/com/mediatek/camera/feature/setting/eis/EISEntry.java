package com.mediatek.camera.feature.setting.eis;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;

public class EISEntry extends FeatureEntryBase {
    private EIS mEIS;

    public EISEntry(Context context, Resources resources) {
        super(context, resources);
        this.mEIS = null;
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
        this.mEIS = new EIS();
        return this.mEIS;
    }
}
