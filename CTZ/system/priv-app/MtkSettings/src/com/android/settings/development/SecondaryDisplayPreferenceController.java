package com.android.settings.development;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/* loaded from: classes.dex */
public class SecondaryDisplayPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final String[] mListSummaries;
    private final String[] mListValues;

    public SecondaryDisplayPreferenceController(Context context) {
        super(context);
        this.mListValues = context.getResources().getStringArray(R.array.overlay_display_devices_values);
        this.mListSummaries = context.getResources().getStringArray(R.array.overlay_display_devices_entries);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "overlay_display_devices";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        writeSecondaryDisplayDevicesOption(obj.toString());
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        updateSecondaryDisplayDevicesOptions();
    }

    @Override // com.android.settingslib.development.DeveloperOptionsPreferenceController
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeSecondaryDisplayDevicesOption(null);
    }

    private void updateSecondaryDisplayDevicesOptions() {
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "overlay_display_devices");
        int i = 0;
        int i2 = 0;
        while (true) {
            if (i2 >= this.mListValues.length) {
                break;
            }
            if (!TextUtils.equals(string, this.mListValues[i2])) {
                i2++;
            } else {
                i = i2;
                break;
            }
        }
        ListPreference listPreference = (ListPreference) this.mPreference;
        listPreference.setValue(this.mListValues[i]);
        listPreference.setSummary(this.mListSummaries[i]);
    }

    private void writeSecondaryDisplayDevicesOption(String str) {
        Settings.Global.putString(this.mContext.getContentResolver(), "overlay_display_devices", str);
        updateSecondaryDisplayDevicesOptions();
    }
}
