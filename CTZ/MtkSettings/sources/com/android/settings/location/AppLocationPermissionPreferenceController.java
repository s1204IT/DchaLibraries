package com.android.settings.location;

import android.content.Context;
import android.provider.Settings;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class AppLocationPermissionPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public AppLocationPermissionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "app_level_permissions";
    }

    @Override
    public boolean isAvailable() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "location_settings_link_to_permissions_enabled", 1) == 1;
    }
}
