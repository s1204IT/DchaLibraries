package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class RegulatoryInfoPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private static final Intent INTENT_PROBE = new Intent("android.settings.SHOW_REGULATORY_INFO");

    public RegulatoryInfoPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !this.mContext.getPackageManager().queryIntentActivities(INTENT_PROBE, 0).isEmpty();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "regulatory_info";
    }
}
