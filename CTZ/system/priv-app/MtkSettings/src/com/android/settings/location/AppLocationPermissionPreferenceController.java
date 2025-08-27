package com.android.settings.location;

import android.content.Context;
import android.provider.Settings;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/* loaded from: classes.dex */
public class AppLocationPermissionPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public AppLocationPermissionPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "app_level_permissions";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "location_settings_link_to_permissions_enabled", 1) == 1;
    }
}
