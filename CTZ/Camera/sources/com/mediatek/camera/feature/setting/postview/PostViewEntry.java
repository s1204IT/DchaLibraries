package com.mediatek.camera.feature.setting.postview;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;

public class PostViewEntry extends FeatureEntryBase {
    public PostViewEntry(Context context, Resources resources) {
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
        return new PostView();
    }
}
