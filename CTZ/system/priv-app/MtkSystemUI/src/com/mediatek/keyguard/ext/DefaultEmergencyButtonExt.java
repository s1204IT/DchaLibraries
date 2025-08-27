package com.mediatek.keyguard.ext;

import android.content.Intent;
import android.view.View;

/* loaded from: classes.dex */
public class DefaultEmergencyButtonExt implements IEmergencyButtonExt {
    private static final String TAG = "DefaultEmergencyButtonExt";

    @Override // com.mediatek.keyguard.ext.IEmergencyButtonExt
    public boolean showEccByServiceState(boolean[] zArr, int i) {
        for (boolean z : zArr) {
            if (z) {
                return true;
            }
        }
        return false;
    }

    @Override // com.mediatek.keyguard.ext.IEmergencyButtonExt
    public void customizeEmergencyIntent(Intent intent, int i) {
    }

    @Override // com.mediatek.keyguard.ext.IEmergencyButtonExt
    public boolean showEccInNonSecureUnlock() {
        return false;
    }

    @Override // com.mediatek.keyguard.ext.IEmergencyButtonExt
    public void setEmergencyButtonVisibility(View view, float f) {
    }
}
