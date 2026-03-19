package com.mediatek.camera.feature.setting.noisereduction;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;

public class NoiseReductionEntry extends FeatureEntryBase {
    private NoiseReduction mNoiseReduction;

    public NoiseReductionEntry(Context context, Resources resources) {
        super(context, resources);
        this.mNoiseReduction = null;
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
        this.mNoiseReduction = new NoiseReduction();
        return this.mNoiseReduction;
    }
}
