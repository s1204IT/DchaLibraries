package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.ZenModeSettings;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeBehaviorSoundPreferenceController extends AbstractZenModePreferenceController implements PreferenceControllerMixin {
    private final ZenModeSettings.SummaryBuilder mSummaryBuilder;

    public ZenModeBehaviorSoundPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, "zen_sound_vibration_settings", lifecycle);
        this.mSummaryBuilder = new ZenModeSettings.SummaryBuilder(context);
    }

    @Override
    public String getPreferenceKey() {
        return "zen_sound_vibration_settings";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(this.mSummaryBuilder.getSoundSettingSummary(getPolicy()));
    }
}
