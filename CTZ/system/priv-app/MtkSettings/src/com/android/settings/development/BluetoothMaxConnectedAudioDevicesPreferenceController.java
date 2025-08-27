package com.android.settings.development;

import android.R;
import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class BluetoothMaxConnectedAudioDevicesPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String MAX_CONNECTED_AUDIO_DEVICES_PROPERTY = "persist.bluetooth.maxconnectedaudiodevices";
    private final int mDefaultMaxConnectedAudioDevices;

    public BluetoothMaxConnectedAudioDevicesPreferenceController(Context context) {
        super(context);
        this.mDefaultMaxConnectedAudioDevices = this.mContext.getResources().getInteger(R.integer.config_attentionMaximumExtension);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bluetooth_max_connected_audio_devices";
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        ListPreference listPreference = (ListPreference) this.mPreference;
        CharSequence[] entries = listPreference.getEntries();
        entries[0] = String.format(entries[0].toString(), Integer.valueOf(this.mDefaultMaxConnectedAudioDevices));
        listPreference.setEntries(entries);
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        String string = obj.toString();
        if (((ListPreference) preference).findIndexOfValue(string) <= 0) {
            string = "";
        }
        SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, string);
        updateState(preference);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ListPreference listPreference = (ListPreference) preference;
        CharSequence[] entries = listPreference.getEntries();
        String str = SystemProperties.get(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY);
        int i = 0;
        if (!str.isEmpty()) {
            int iFindIndexOfValue = listPreference.findIndexOfValue(str);
            if (iFindIndexOfValue < 0) {
                SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, "");
            } else {
                i = iFindIndexOfValue;
            }
        }
        listPreference.setValueIndex(i);
        listPreference.setSummary(entries[i]);
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchEnabled() {
        super.onDeveloperOptionsSwitchEnabled();
        updateState(this.mPreference);
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(MAX_CONNECTED_AUDIO_DEVICES_PROPERTY, "");
        updateState(this.mPreference);
    }
}
