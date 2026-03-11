package com.android.settings.display;

import android.content.Context;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.settings.FeatureOption;

public class AmbientDisplayPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    private static final int MY_USER_ID = UserHandle.myUserId();
    private final AmbientDisplayConfiguration mConfig;
    private final String mKey;

    public AmbientDisplayPreferenceController(Context context, AmbientDisplayConfiguration ambientDisplayConfiguration, String str) {
        super(context);
        this.mConfig = ambientDisplayConfiguration;
        this.mKey = str;
    }

    @Override
    public boolean isAvailable() {
        return this.mConfig.available() && !FeatureOption.MTK_AOD_SUPPORT;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (this.mConfig.alwaysOnEnabled(MY_USER_ID)) {
            preference.setSummary(R.string.ambient_display_screen_summary_always_on);
            return;
        }
        if (this.mConfig.pulseOnNotificationEnabled(MY_USER_ID)) {
            preference.setSummary(R.string.ambient_display_screen_summary_notifications);
        } else if (this.mConfig.enabled(MY_USER_ID)) {
            preference.setSummary(R.string.switch_on_text);
        } else {
            preference.setSummary(R.string.switch_off_text);
        }
    }

    @Override
    public String getPreferenceKey() {
        return this.mKey;
    }
}
