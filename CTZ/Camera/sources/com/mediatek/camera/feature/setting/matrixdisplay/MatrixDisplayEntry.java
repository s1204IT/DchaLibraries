package com.mediatek.camera.feature.setting.matrixdisplay;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.portability.SystemProperties;

public class MatrixDisplayEntry extends FeatureEntryBase {
    public MatrixDisplayEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        return !isThirdPartyIntent(activity) && isPlatformSupport() && SystemProperties.getInt("ro.vendor.mtk_cam_lomo_support", 0) == 1;
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        return new MatrixDisplay();
    }

    private boolean isPlatformSupport() {
        if (CameraDeviceManagerFactory.CameraApi.API1.equals(this.mDefaultCameraApi)) {
            return true;
        }
        return false;
    }
}
