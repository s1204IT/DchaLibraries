package com.mediatek.settings.security;

import android.content.Context;
import android.content.Intent;
import com.android.settingslib.core.AbstractPreferenceController;

public class DataprotectionPreferenceController extends AbstractPreferenceController {
    public DataprotectionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getPackageManager().resolveActivity(new Intent("com.mediatek.dataprotection.ACTION_START_MAIN"), 0) != null;
    }

    @Override
    public String getPreferenceKey() {
        return "data_protection_key";
    }
}
