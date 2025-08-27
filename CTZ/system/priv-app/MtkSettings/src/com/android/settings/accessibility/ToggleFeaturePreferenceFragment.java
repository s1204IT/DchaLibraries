package com.android.settings.accessibility;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;

/* loaded from: classes.dex */
public abstract class ToggleFeaturePreferenceFragment extends SettingsPreferenceFragment {
    protected String mPreferenceKey;
    protected Intent mSettingsIntent;
    protected CharSequence mSettingsTitle;
    protected SwitchBar mSwitchBar;
    protected ToggleSwitch mToggleSwitch;

    protected abstract void onPreferenceToggled(String str, boolean z);

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (getPreferenceScreenResId() <= 0) {
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        updateSwitchBarText(this.mSwitchBar);
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        onProcessArguments(getArguments());
        if (this.mSettingsTitle != null && this.mSettingsIntent != null) {
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            Preference preference = new Preference(preferenceScreen.getContext());
            preference.setTitle(this.mSettingsTitle);
            preference.setIconSpaceReserved(true);
            preference.setIntent(this.mSettingsIntent);
            preferenceScreen.addPreference(preference);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        installActionBarToggleSwitch();
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onDestroyView() {
        super.onDestroyView();
        removeActionBarToggleSwitch();
    }

    protected void updateSwitchBarText(SwitchBar switchBar) {
        switchBar.setSwitchBarText(R.string.accessibility_service_master_switch_title, R.string.accessibility_service_master_switch_title);
    }

    protected void onInstallSwitchBarToggleSwitch() {
    }

    protected void onRemoveSwitchBarToggleSwitch() {
    }

    private void installActionBarToggleSwitch() {
        this.mSwitchBar.show();
        onInstallSwitchBarToggleSwitch();
    }

    private void removeActionBarToggleSwitch() {
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        onRemoveSwitchBarToggleSwitch();
        this.mSwitchBar.hide();
    }

    public void setTitle(String str) {
        getActivity().setTitle(str);
    }

    protected void onProcessArguments(Bundle bundle) {
        this.mPreferenceKey = bundle.getString("preference_key");
        if (bundle.containsKey("checked")) {
            this.mSwitchBar.setCheckedInternal(bundle.getBoolean("checked"));
        }
        if (bundle.containsKey("resolve_info")) {
            getActivity().setTitle(((ResolveInfo) bundle.getParcelable("resolve_info")).loadLabel(getPackageManager()).toString());
        } else if (bundle.containsKey("title")) {
            setTitle(bundle.getString("title"));
        }
        if (bundle.containsKey("summary_res")) {
            this.mFooterPreferenceMixin.createFooterPreference().setTitle(bundle.getInt("summary_res"));
        } else if (bundle.containsKey("summary")) {
            this.mFooterPreferenceMixin.createFooterPreference().setTitle(bundle.getCharSequence("summary"));
        }
    }
}
