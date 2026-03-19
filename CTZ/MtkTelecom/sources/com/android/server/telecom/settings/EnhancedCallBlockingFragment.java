package com.android.server.telecom.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.server.telecom.R;

public class EnhancedCallBlockingFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.enhanced_call_blocking_settings);
        setOnPreferenceChangeListener("block_numbers_not_in_contacts_setting");
        setOnPreferenceChangeListener("block_private_number_calls_setting");
        setOnPreferenceChangeListener("block_payphone_calls_setting");
        setOnPreferenceChangeListener("block_unknown_calls_setting");
    }

    private void setOnPreferenceChangeListener(String str) {
        SwitchPreference switchPreference = (SwitchPreference) findPreference(str);
        if (switchPreference != null) {
            switchPreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEnhancedBlockPref("block_numbers_not_in_contacts_setting");
        updateEnhancedBlockPref("block_private_number_calls_setting");
        updateEnhancedBlockPref("block_payphone_calls_setting");
        updateEnhancedBlockPref("block_unknown_calls_setting");
    }

    private void updateEnhancedBlockPref(String str) {
        SwitchPreference switchPreference = (SwitchPreference) findPreference(str);
        if (switchPreference != null) {
            switchPreference.setChecked(BlockedNumbersUtil.getEnhancedBlockSetting(getActivity(), str));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        BlockedNumbersUtil.setEnhancedBlockSetting(getActivity(), preference.getKey(), ((Boolean) obj).booleanValue());
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.xml.layout_customized_listview, viewGroup, false);
    }
}
