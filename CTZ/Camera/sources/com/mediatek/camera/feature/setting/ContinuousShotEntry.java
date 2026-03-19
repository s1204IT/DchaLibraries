package com.mediatek.camera.feature.setting;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.CameraDeviceManagerFactory;
import com.mediatek.camera.common.loader.FeatureEntryBase;
import com.mediatek.camera.common.setting.ICameraSetting;
import com.mediatek.camera.portability.SystemProperties;

public class ContinuousShotEntry extends FeatureEntryBase {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(ContinuousShotEntry.class.getSimpleName());
    private static final String NAME = ContinuousShotEntry.class.getName();
    public static boolean mIsBurstMode = false;

    public ContinuousShotEntry(Context context, Resources resources) {
        super(context, resources);
    }

    @Override
    public boolean isSupport(CameraDeviceManagerFactory.CameraApi cameraApi, Activity activity) {
        if (SystemProperties.getInt("vendor.mtkcamapp.cshot.platform", 0) == -1 && ActivityManager.isLowRamDeviceStatic()) {
            LogHelper.i(TAG, "[isSupport] False");
            return false;
        }
        return !isThirdPartyIntent(activity);
    }

    @Override
    public Class getType() {
        return ICameraSetting.class;
    }

    @Override
    public Object createInstance() {
        int i = SystemProperties.getInt("vendor.mtkcamapp.cshot.version", 0);
        if (i == 1) {
            return new ContinuousShot2();
        }
        if (i == 2) {
            mIsBurstMode = true;
            return new ContinuousShotRepeatingBurstMode();
        }
        mIsBurstMode = true;
        return new ContinuousShotBurstMode();
    }
}
