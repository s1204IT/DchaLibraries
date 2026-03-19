package com.mediatek.camera.feature.setting.aaaroidebug;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.portability.SystemProperties;

public class AaaRoiDebugEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(AaaRoiDebugEntry.class.getSimpleName());

    public AaaRoiDebugEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        if (!CameraDeviceManagerFactory.CameraApi.API2.equals(cameraApi) || SystemProperties.getInt("vendor.mtk.camera.app.3a.debug", 0) != 1) {
            return false;
        }
        LogHelper.d(TAG, "[isSupport] return true");
        return true;
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        return new AaaRoiDebug();
    }
}
