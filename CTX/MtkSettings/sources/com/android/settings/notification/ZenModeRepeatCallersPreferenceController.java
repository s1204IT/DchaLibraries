package com.android.settings.notification;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeRepeatCallersPreferenceController extends AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {
    private final ZenModeBackend mBackend;
    private final int mRepeatCallersThreshold;

    public ZenModeRepeatCallersPreferenceController(Context context, Lifecycle lifecycle, int i) {
        super(context, "zen_mode_repeat_callers", lifecycle);
        this.mRepeatCallersThreshold = i;
        this.mBackend = ZenModeBackend.getInstance(context);
    }

    @Override
    public String getPreferenceKey() {
        return "zen_mode_repeat_callers";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        setRepeatCallerSummary(preferenceScreen.findPreference("zen_mode_repeat_callers"));
    }

    @Override
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
                if (this.mBackend.isPriorityCategoryEnabled(8) && this.mBackend.getPriorityCallSenders() == 0) {
                    switchPreference.setEnabled(false);
                    switchPreference.setChecked(true);
                } else {
                    switchPreference.setEnabled(true);
                    switchPreference.setChecked(this.mBackend.isPriorityCategoryEnabled(16));
                }
                break;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        if (ZenModeSettingsBase.DEBUG) {
            Log.d("PrefControllerMixin", "onPrefChange allowRepeatCallers=" + zBooleanValue);
        }
        this.mMetricsFeatureProvider.action(this.mContext, 171, zBooleanValue);
        this.mBackend.saveSoundPolicy(16, zBooleanValue);
        return true;
    }

    private void setRepeatCallerSummary(Preference preference) {
        preference.setSummary(this.mContext.getString(R.string.zen_mode_repeat_callers_summary, Integer.valueOf(this.mRepeatCallersThreshold)));
    }
}
