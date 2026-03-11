package com.android.settings.system;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class AdditionalSystemUpdatePreferenceController extends BasePreferenceController {
    private static final String KEY_UPDATE_SETTING = "additional_system_update_settings";

    public AdditionalSystemUpdatePreferenceController(Context context) {
        super(context, KEY_UPDATE_SETTING);
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.bool.config_additional_system_update_setting_enable)) {
            return 0;
        }
        return 2;
    }
}
