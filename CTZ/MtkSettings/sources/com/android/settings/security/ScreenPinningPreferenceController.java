package com.android.settings.security;

import android.content.Context;
import android.provider.Settings;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

public class ScreenPinningPreferenceController extends BasePreferenceController {
    private static final String KEY_SCREEN_PINNING = "screen_pinning_settings";

    public ScreenPinningPreferenceController(Context context) {
        super(context, KEY_SCREEN_PINNING);
    }

    @Override
    public int getAvailabilityStatus() {
        return this.mContext.getResources().getBoolean(R.bool.config_show_screen_pinning_settings) ? 0 : 2;
    }

    @Override
    public CharSequence getSummary() {
        if (Settings.System.getInt(this.mContext.getContentResolver(), "lock_to_app_enabled", 0) != 0) {
            return this.mContext.getText(R.string.switch_on_text);
        }
        return this.mContext.getText(R.string.switch_off_text);
    }
}
