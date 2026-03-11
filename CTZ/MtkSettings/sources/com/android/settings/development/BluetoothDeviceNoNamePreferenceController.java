package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BluetoothDeviceNoNamePreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY = "persist.bluetooth.showdeviceswithoutnames";

    public BluetoothDeviceNoNamePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "bluetooth_show_devices_without_names";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SystemProperties.set(BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, ((Boolean) obj).booleanValue() ? "true" : "false");
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(SystemProperties.getBoolean(BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, false));
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(BLUETOOTH_SHOW_DEVICES_WITHOUT_NAMES_PROPERTY, "false");
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
