package com.android.settings.overlay;

import android.app.Activity;
import android.content.Context;

/* loaded from: classes.dex */
public interface SupportFeatureProvider {
    String getNewDeviceIntroUrl(Context context);

    void startSupportV2(Activity activity);
}
