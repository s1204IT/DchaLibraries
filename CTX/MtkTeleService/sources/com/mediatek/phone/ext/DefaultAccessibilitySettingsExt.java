package com.mediatek.phone.ext;

import android.app.Fragment;
import android.util.Log;

public class DefaultAccessibilitySettingsExt implements IAccessibilitySettingsExt {
    @Override
    public void handleCallStateChanged(Fragment fragment, int i, int i2, int i3) {
        Log.d("DefaultAccessibilitySettingsExt", "handleCallStateChanged");
    }
}
