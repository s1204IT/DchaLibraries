package com.mediatek.camera.feature.setting.hdr;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;

public class HdrEntry extends FeatureEntryBase {
    private Hdr mHdr;

    public HdrEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        return !isThirdPartyIntent(activity);
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public int getStage() {
        return 1;
    }

    @Override
    public Object createInstance() {
        this.mHdr = new Hdr();
        return this.mHdr;
    }
}
