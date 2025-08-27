package com.android.settings.development;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

/* loaded from: classes.dex */
public class DebugGpuOverdrawPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final String[] mListSummaries;
    private final String[] mListValues;

    public DebugGpuOverdrawPreferenceController(Context context) {
        super(context);
        this.mListValues = context.getResources().getStringArray(R.array.debug_hw_overdraw_values);
        this.mListSummaries = context.getResources().getStringArray(R.array.debug_hw_overdraw_entries);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "debug_hw_overdraw";
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        writeDebugHwOverdrawOptions(obj);
        updateDebugHwOverdrawOptions();
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        updateDebugHwOverdrawOptions();
    }

    private void writeDebugHwOverdrawOptions(Object obj) {
        SystemProperties.set("debug.hwui.overdraw", obj == null ? "" : obj.toString());
        SystemPropPoker.getInstance().poke();
    }

    private void updateDebugHwOverdrawOptions() {
        String str = SystemProperties.get("debug.hwui.overdraw", "");
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
        ListPreference listPreference = (ListPreference) this.mPreference;
        listPreference.setValue(this.mListValues[i]);
        listPreference.setSummary(this.mListSummaries[i]);
    }
}
