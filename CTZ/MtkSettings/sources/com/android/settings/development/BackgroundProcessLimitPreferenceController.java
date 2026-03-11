package com.android.settings.development;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BackgroundProcessLimitPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    private final String[] mListSummaries;
    private final String[] mListValues;

    public BackgroundProcessLimitPreferenceController(Context context) {
        super(context);
        this.mListValues = context.getResources().getStringArray(R.array.app_process_limit_values);
        this.mListSummaries = context.getResources().getStringArray(R.array.app_process_limit_entries);
    }

    @Override
    public String getPreferenceKey() {
        return "app_process_limit";
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        writeAppProcessLimitOptions(obj);
        updateAppProcessLimitOptions();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateAppProcessLimitOptions();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeAppProcessLimitOptions(null);
    }

    private void updateAppProcessLimitOptions() {
        try {
            int processLimit = getActivityManagerService().getProcessLimit();
            int i = 0;
            int i2 = 0;
            while (true) {
                if (i2 >= this.mListValues.length) {
                    break;
                }
                if (Integer.parseInt(this.mListValues[i2]) < processLimit) {
                    i2++;
                } else {
                    i = i2;
                    break;
                }
            }
            ListPreference listPreference = (ListPreference) this.mPreference;
            listPreference.setValue(this.mListValues[i]);
            listPreference.setSummary(this.mListSummaries[i]);
        } catch (RemoteException e) {
        }
    }

    private void writeAppProcessLimitOptions(Object obj) {
        int i;
        if (obj == null) {
            i = -1;
        } else {
            try {
                i = Integer.parseInt(obj.toString());
            } catch (RemoteException e) {
                return;
            }
        }
        getActivityManagerService().setProcessLimit(i);
        updateAppProcessLimitOptions();
    }

    IActivityManager getActivityManagerService() {
        return ActivityManager.getService();
    }
}
