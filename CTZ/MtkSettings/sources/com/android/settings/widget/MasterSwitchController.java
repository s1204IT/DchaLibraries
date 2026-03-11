package com.android.settings.widget;

import android.support.v7.preference.Preference;
import com.android.settingslib.RestrictedLockUtils;

public class MasterSwitchController extends SwitchWidgetController implements Preference.OnPreferenceChangeListener {
    private final MasterSwitchPreference mPreference;

    public MasterSwitchController(MasterSwitchPreference masterSwitchPreference) {
        this.mPreference = masterSwitchPreference;
    }

    @Override
    public void updateTitle(boolean z) {
    }

    @Override
    public void startListening() {
        this.mPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void stopListening() {
        this.mPreference.setOnPreferenceChangeListener(null);
    }

    @Override
    public void setChecked(boolean z) {
        this.mPreference.setChecked(z);
    }

    @Override
    public boolean isChecked() {
        return this.mPreference.isChecked();
    }

    @Override
    public void setEnabled(boolean z) {
        this.mPreference.setSwitchEnabled(z);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mListener != null) {
            return this.mListener.onSwitchToggled(((Boolean) obj).booleanValue());
        }
        return false;
    }

    @Override
    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        this.mPreference.setDisabledByAdmin(enforcedAdmin);
    }
}
