package com.mediatek.settings.security;

import android.content.Context;
import android.content.Intent;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class PermissionControlPreferenceController extends AbstractPreferenceController {
    public PermissionControlPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return this.mContext.getPackageManager().resolveActivity(new Intent("com.mediatek.security.TRIGGER_HISTORY"), 0) != null;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "permission_request";
    }
}
