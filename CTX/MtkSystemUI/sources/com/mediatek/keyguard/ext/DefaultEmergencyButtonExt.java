package com.mediatek.keyguard.ext;

import android.content.Intent;
import android.view.View;

public class DefaultEmergencyButtonExt implements IEmergencyButtonExt {
    private static final String TAG = "DefaultEmergencyButtonExt";

    @Override
    public boolean showEccByServiceState(boolean[] zArr, int i) {
        for (boolean z : zArr) {
            if (z) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void customizeEmergencyIntent(Intent intent, int i) {
    }

    @Override
    public boolean showEccInNonSecureUnlock() {
        return false;
    }

    @Override
    public void setEmergencyButtonVisibility(View view, float f) {
    }
}
