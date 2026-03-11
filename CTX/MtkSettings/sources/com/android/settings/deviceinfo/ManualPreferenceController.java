package com.android.settings.deviceinfo;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ManualPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public ManualPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_manual);
    }

    @Override
    public String getPreferenceKey() {
        return "manual";
    }
}
