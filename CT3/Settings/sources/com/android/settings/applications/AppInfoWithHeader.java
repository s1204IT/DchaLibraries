package com.android.settings.applications;

import android.os.Bundle;
import android.util.Log;
import com.android.settings.AppHeader;

public abstract class AppInfoWithHeader extends AppInfoBase {
    private boolean mCreated;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (this.mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        this.mCreated = true;
        if (this.mPackageInfo == null) {
            return;
        }
        AppHeader.createAppHeader(this, this.mPackageInfo.applicationInfo.loadIcon(this.mPm), this.mPackageInfo.applicationInfo.loadLabel(this.mPm), this.mPackageName, this.mPackageInfo.applicationInfo.uid, 0);
    }
}
