package com.android.settings.development;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class KeepActivitiesPreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {
    static final int SETTING_VALUE_OFF = 0;
    private IActivityManager mActivityManager;

    public KeepActivitiesPreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "immediately_destroy_activities";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mActivityManager = getActivityManager();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        writeImmediatelyDestroyActivitiesOptions(((Boolean) obj).booleanValue());
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        ((SwitchPreference) this.mPreference).setChecked(Settings.Global.getInt(this.mContext.getContentResolver(), "always_finish_activities", 0) != 0);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeImmediatelyDestroyActivitiesOptions(false);
        ((SwitchPreference) this.mPreference).setChecked(false);
    }

    private void writeImmediatelyDestroyActivitiesOptions(boolean z) {
        try {
            this.mActivityManager.setAlwaysFinish(z);
        } catch (RemoteException e) {
        }
    }

    IActivityManager getActivityManager() {
        return ActivityManager.getService();
    }
}
