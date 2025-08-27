package com.android.settings.core;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import com.android.settings.widget.MasterSwitchPreference;

/* loaded from: classes.dex */
public abstract class TogglePreferenceController extends BasePreferenceController implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "TogglePrefController";

    public abstract boolean isChecked();

    public abstract boolean setChecked(boolean z);

    public TogglePreferenceController(Context context, String str) {
        super(context, str);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        if (preference instanceof TwoStatePreference) {
            ((TwoStatePreference) preference).setChecked(isChecked());
        } else if (preference instanceof MasterSwitchPreference) {
            ((MasterSwitchPreference) preference).setChecked(isChecked());
        } else {
            refreshSummary(preference);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public final boolean onPreferenceChange(Preference preference, Object obj) {
        return setChecked(((Boolean) obj).booleanValue());
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getSliceType() {
        return 1;
    }
}
