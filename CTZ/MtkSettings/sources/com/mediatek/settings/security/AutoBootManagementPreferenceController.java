package com.mediatek.settings.security;

import android.content.Context;
import android.content.Intent;
import com.android.settingslib.core.AbstractPreferenceController;

public class AutoBootManagementPreferenceController extends AbstractPreferenceController {
    public AutoBootManagementPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getPackageManager().resolveActivity(new Intent("com.mediatek.security.AUTO_BOOT"), 0) != null;
    }

    @Override
    public String getPreferenceKey() {
        return "auto_boot_management";
    }
}
