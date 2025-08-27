package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class BluetoothAvrcpVersionPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final String BLUETOOTH_AVRCP_VERSION_PROPERTY = "persist.bluetooth.avrcpversion";
    private final String[] mListSummaries;
    private final String[] mListValues;

    public BluetoothAvrcpVersionPreferenceController(Context context) {
        super(context);
        this.mListValues = context.getResources().getStringArray(R.array.bluetooth_avrcp_version_values);
        this.mListSummaries = context.getResources().getStringArray(R.array.bluetooth_avrcp_versions);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "bluetooth_select_avrcp_version";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SystemProperties.set(BLUETOOTH_AVRCP_VERSION_PROPERTY, obj.toString());
        updateState(this.mPreference);
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        ListPreference listPreference = (ListPreference) preference;
        String str = SystemProperties.get(BLUETOOTH_AVRCP_VERSION_PROPERTY);
        int i = 0;
        int i2 = 0;
        while (true) {
            if (i2 >= this.mListValues.length) {
                break;
            }
            if (!TextUtils.equals(str, this.mListValues[i2])) {
                i2++;
            } else {
                i = i2;
                break;
            }
        }
        listPreference.setValue(this.mListValues[i]);
        listPreference.setSummary(this.mListSummaries[i]);
    }
}
