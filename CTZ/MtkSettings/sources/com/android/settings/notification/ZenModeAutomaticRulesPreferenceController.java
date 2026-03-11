package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.app.Fragment;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.Map;

public class ZenModeAutomaticRulesPreferenceController extends AbstractZenModeAutomaticRulePreferenceController {
    protected PreferenceCategory mPreferenceCategory;

    public ZenModeAutomaticRulesPreferenceController(Context context, Fragment fragment, Lifecycle lifecycle) {
        super(context, "zen_mode_automatic_rules", fragment, lifecycle);
    }

    @Override
    public String getPreferenceKey() {
        return "zen_mode_automatic_rules";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreferenceCategory = (PreferenceCategory) preferenceScreen.findPreference(getPreferenceKey());
        this.mPreferenceCategory.setPersistent(false);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        this.mPreferenceCategory.removeAll();
        for (Map.Entry<String, AutomaticZenRule> entry : sortedRules()) {
            this.mPreferenceCategory.addPreference(new ZenRulePreference(this.mPreferenceCategory.getContext(), entry, this.mParent, this.mMetricsFeatureProvider));
        }
    }
}
