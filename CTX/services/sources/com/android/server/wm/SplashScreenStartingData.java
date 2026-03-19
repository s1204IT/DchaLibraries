package com.android.server.wm;

import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import com.android.server.policy.WindowManagerPolicy;

class SplashScreenStartingData extends StartingData {
    private final CompatibilityInfo mCompatInfo;
    private final int mIcon;
    private final int mLabelRes;
    private final int mLogo;
    private final Configuration mMergedOverrideConfiguration;
    private final CharSequence mNonLocalizedLabel;
    private final String mPkg;
    private final int mTheme;
    private final int mWindowFlags;

    SplashScreenStartingData(WindowManagerService windowManagerService, String str, int i, CompatibilityInfo compatibilityInfo, CharSequence charSequence, int i2, int i3, int i4, int i5, Configuration configuration) {
        super(windowManagerService);
        this.mPkg = str;
        this.mTheme = i;
        this.mCompatInfo = compatibilityInfo;
        this.mNonLocalizedLabel = charSequence;
        this.mLabelRes = i2;
        this.mIcon = i3;
        this.mLogo = i4;
        this.mWindowFlags = i5;
        this.mMergedOverrideConfiguration = configuration;
    }

    @Override
    WindowManagerPolicy.StartingSurface createStartingSurface(AppWindowToken appWindowToken) {
        return this.mService.mPolicy.addSplashScreen(appWindowToken.token, this.mPkg, this.mTheme, this.mCompatInfo, this.mNonLocalizedLabel, this.mLabelRes, this.mIcon, this.mLogo, this.mWindowFlags, this.mMergedOverrideConfiguration, appWindowToken.getDisplayContent().getDisplayId());
    }
}
