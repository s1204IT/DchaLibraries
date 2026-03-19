package com.mediatek.camera.feature.setting.ais;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;

public class AISEntry extends FeatureEntryBase {
    private AIS mAIS;

    public AISEntry(Context context, Resources resources) {
        super(context, resources);
        this.mAIS = null;
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        if (isThirdPartyIntent(activity)) {
            return false;
        }
        return true;
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        this.mAIS = new AIS();
        return this.mAIS;
    }
}
