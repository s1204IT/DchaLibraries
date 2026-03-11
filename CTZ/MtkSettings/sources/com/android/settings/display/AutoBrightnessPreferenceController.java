package com.android.settings.display;

import android.R;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.DisplaySettings;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;

public class AutoBrightnessPreferenceController extends TogglePreferenceController {
    private final int DEFAULT_VALUE;
    private final String SYSTEM_KEY;

    public AutoBrightnessPreferenceController(Context context, String str) {
        super(context, str);
        this.SYSTEM_KEY = "screen_brightness_mode";
        this.DEFAULT_VALUE = 0;
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(this.mContext.getContentResolver(), "screen_brightness_mode", 0) != 0;
    }

    @Override
    public boolean setChecked(boolean z) {
        Settings.System.putInt(this.mContext.getContentResolver(), "screen_brightness_mode", z ? 1 : 0);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        if (this.mContext.getResources().getBoolean(R.^attr-private.borderTop)) {
            return 0;
        }
        return 2;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "auto_brightness");
    }

    @Override
    public ResultPayload getResultPayload() {
        return new InlineSwitchPayload("screen_brightness_mode", 1, 1, DatabaseIndexingUtils.buildSearchResultPageIntent(this.mContext, DisplaySettings.class.getName(), getPreferenceKey(), this.mContext.getString(com.android.settings.R.string.display_settings)), isAvailable(), 0);
    }
}
