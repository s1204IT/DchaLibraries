package com.android.systemui.tv.pip;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import com.android.systemui.SystemUI;

public class PipUI extends SystemUI {
    private boolean mSupportPip;

    @Override
    public void start() {
        boolean zHasSystemFeature;
        PackageManager pm = this.mContext.getPackageManager();
        if (!pm.hasSystemFeature("android.software.picture_in_picture")) {
            zHasSystemFeature = false;
        } else {
            zHasSystemFeature = pm.hasSystemFeature("android.software.leanback");
        }
        this.mSupportPip = zHasSystemFeature;
        if (!this.mSupportPip) {
            return;
        }
        PipManager pipManager = PipManager.getInstance();
        pipManager.initialize(this.mContext);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!this.mSupportPip) {
            return;
        }
        PipManager.getInstance().onConfigurationChanged();
    }
}
