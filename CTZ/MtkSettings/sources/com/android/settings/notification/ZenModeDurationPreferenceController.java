package com.android.settings.notification;

import android.app.FragmentManager;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeDurationPreferenceController extends AbstractZenModePreferenceController implements Preference.OnPreferenceClickListener, PreferenceControllerMixin {
    private FragmentManager mFragment;

    public ZenModeDurationPreferenceController(Context context, Lifecycle lifecycle, FragmentManager fragmentManager) {
        super(context, "zen_mode_duration_settings", lifecycle);
        this.mFragment = fragmentManager;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "zen_mode_duration_settings";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        preferenceScreen.findPreference("zen_mode_duration_settings").setOnPreferenceClickListener(this);
    }

    @Override
    public void updateState(Preference preference) {
        String string;
        super.updateState(preference);
        int zenDuration = getZenDuration();
        if (zenDuration < 0) {
            string = this.mContext.getString(R.string.zen_mode_duration_summary_always_prompt);
        } else if (zenDuration == 0) {
            string = this.mContext.getString(R.string.zen_mode_duration_summary_forever);
        } else if (zenDuration >= 60) {
            int i = zenDuration / 60;
            string = this.mContext.getResources().getQuantityString(R.plurals.zen_mode_duration_summary_time_hours, i, Integer.valueOf(i));
        } else {
            string = this.mContext.getResources().getString(R.string.zen_mode_duration_summary_time_minutes, Integer.valueOf(zenDuration));
        }
        preference.setSummary(string);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        new SettingsZenDurationDialog().show(this.mFragment, "ZenModeDurationDialog");
        return true;
    }
}
