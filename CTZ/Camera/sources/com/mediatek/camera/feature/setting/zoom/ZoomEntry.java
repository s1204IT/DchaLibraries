package com.mediatek.camera.feature.setting.zoom;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.portability.SystemProperties;

public class ZoomEntry extends FeatureEntryBase {
    private static final boolean sIsDualCameraSupport;

    static {
        sIsDualCameraSupport = SystemProperties.getInt("ro.vendor.mtk_cam_dualzoom_support", 0) == 1;
    }

    public ZoomEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        return !sIsDualCameraSupport || isThirdPartyIntent(activity);
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        return new Zoom();
    }
}
