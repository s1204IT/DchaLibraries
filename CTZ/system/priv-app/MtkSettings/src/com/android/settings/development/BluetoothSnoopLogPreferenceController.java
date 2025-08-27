package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class BluetoothSnoopLogPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String BLUETOOTH_BTSNOOP_ENABLE_PROPERTY = "persist.bluetooth.btsnoopenable";

    public BluetoothSnoopLogPreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bt_hci_snoop_log";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, Boolean.toString(((Boolean) obj).booleanValue()));
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        ((SwitchPreference) this.mPreference).setChecked(SystemProperties.getBoolean(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, false));
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(BLUETOOTH_BTSNOOP_ENABLE_PROPERTY, Boolean.toString(false));
        ((SwitchPreference) this.mPreference).setChecked(false);
    }
}
