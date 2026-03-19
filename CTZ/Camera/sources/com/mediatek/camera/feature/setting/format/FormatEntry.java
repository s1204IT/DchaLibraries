package com.mediatek.camera.feature.setting.format;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Environment;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;
import java.io.File;

public class FormatEntry extends FeatureEntryBase {
    private static boolean SUPPORT_HEIF = new File(Environment.getExternalStorageDirectory(), "SUPPORT_HEIF").exists();

    public FormatEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        return !CameraDeviceManagerFactory.CameraApi.API1.equals(cameraApi) && CameraDeviceManagerFactory.CameraApi.API2.equals(cameraApi) && SUPPORT_HEIF;
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
        return new Format();
    }
}
