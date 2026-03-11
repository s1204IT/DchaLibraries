package com.mediatek.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class CustomizeBuildVersionPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public CustomizeBuildVersionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "custom_build_version";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(SystemProperties.get("ro.mediatek.version.release", this.mContext.getResources().getString(R.string.device_info_default)));
    }
}
