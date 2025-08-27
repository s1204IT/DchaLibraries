package com.android.settings.widget;

import android.support.v7.preference.Preference;
import com.android.settingslib.RestrictedLockUtils;

/* loaded from: classes.dex */
public class MasterSwitchController extends SwitchWidgetController implements Preference.OnPreferenceChangeListener {
    private final MasterSwitchPreference mPreference;

    public MasterSwitchController(MasterSwitchPreference masterSwitchPreference) {
        this.mPreference = masterSwitchPreference;
    }

    @Override // com.android.settings.widget.SwitchWidgetController
    public void updateTitle(boolean z) {
    }

    @Override // com.android.settings.widget.SwitchWidgetController
    public void startListening() {
        this.mPreference.setOnPreferenceChangeListener(this);
    }

    @Override // com.android.settings.widget.SwitchWidgetController
    public void stopListening() {
        this.mPreference.setOnPreferenceChangeListener(null);
    }

    @Override // com.android.settings.widget.SwitchWidgetController
    public void setChecked(boolean z) {
        this.mPreference.setChecked(z);
    }

    @Override // com.android.settings.widget.SwitchWidgetController
    public boolean isChecked() {
        return this.mPreference.isChecked();
    }

    @Override // com.android.settings.widget.SwitchWidgetController
    public void setEnabled(boolean z) {
        this.mPreference.setSwitchEnabled(z);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mListener != null) {
            return this.mListener.onSwitchToggled(((Boolean) obj).booleanValue());
        }
        return false;
    }

    @Override // com.android.settings.widget.SwitchWidgetController
    public void setDisabledByAdmin(RestrictedLockUtils.EnforcedAdmin enforcedAdmin) {
        this.mPreference.setDisabledByAdmin(enforcedAdmin);
    }
}
