package com.android.settings.notification;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeRemindersPreferenceController extends AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {
    public ZenModeRemindersPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, "zen_mode_reminders", lifecycle);
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "zen_mode_reminders";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        SwitchPreference switchPreference = (SwitchPreference) preference;
        switch (getZenMode()) {
            case 2:
            case 3:
                switchPreference.setEnabled(false);
                switchPreference.setChecked(false);
                break;
            default:
                switchPreference.setEnabled(true);
                switchPreference.setChecked(this.mBackend.isPriorityCategoryEnabled(1));
                break;
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        if (ZenModeSettingsBase.DEBUG) {
            Log.d("PrefControllerMixin", "onPrefChange allowReminders=" + zBooleanValue);
        }
        this.mMetricsFeatureProvider.action(this.mContext, 167, zBooleanValue);
        this.mBackend.saveSoundPolicy(1, zBooleanValue);
        return true;
    }
}
