package com.android.settings.notification;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settingslib.core.lifecycle.Lifecycle;

/* loaded from: classes.dex */
public class ZenModeSystemPreferenceController extends AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {
    public ZenModeSystemPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, "zen_mode_system", lifecycle);
    }

    @Override // com.android.settings.notification.AbstractZenModePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "zen_mode_system";
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
                switchPreference.setEnabled(false);
                switchPreference.setChecked(false);
                break;
            case 3:
                switchPreference.setEnabled(false);
                switchPreference.setChecked(false);
                break;
            default:
                switchPreference.setEnabled(true);
                switchPreference.setChecked(this.mBackend.isPriorityCategoryEnabled(128));
                break;
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        if (ZenModeSettingsBase.DEBUG) {
            Log.d("PrefControllerMixin", "onPrefChange allowSystem=" + zBooleanValue);
        }
        this.mMetricsFeatureProvider.action(this.mContext, 1340, zBooleanValue);
        this.mBackend.saveSoundPolicy(128, zBooleanValue);
        return true;
    }
}
