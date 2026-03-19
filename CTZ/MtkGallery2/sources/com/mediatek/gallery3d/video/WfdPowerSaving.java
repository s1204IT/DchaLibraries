package com.mediatek.gallery3d.video;

import android.content.Context;
import android.view.Window;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.galleryportable.WfdConnectionAdapter;

public class WfdPowerSaving extends PowerSaving {
    private static final int EXTENSION_MODE_LIST_END = 12;
    private static final int EXTENSION_MODE_LIST_START = 10;
    private static final String TAG = "VP_WfdPowerSaving";

    public WfdPowerSaving(Context context, Window window) {
        super(context, window);
    }

    @Override
    protected int getPowerSavingMode() {
        int powerSavingMode = WfdConnectionAdapter.getPowerSavingMode(this.mContext);
        if (powerSavingMode >= 10 && powerSavingMode <= 12) {
            powerSavingMode -= 10;
        }
        Log.v(TAG, "getWfdPowerSavingMode(): " + powerSavingMode);
        return powerSavingMode;
    }

    @Override
    protected int getDelayTime() {
        return WfdConnectionAdapter.getPowerSavingDelay(this.mContext) * 1000;
    }
}
