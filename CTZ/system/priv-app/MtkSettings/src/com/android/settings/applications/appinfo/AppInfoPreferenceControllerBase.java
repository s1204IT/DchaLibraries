package com.android.settings.applications.appinfo;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.BasePreferenceController;

/* loaded from: classes.dex */
public abstract class AppInfoPreferenceControllerBase extends BasePreferenceController implements AppInfoDashboardFragment.Callback {
    private final Class<? extends SettingsPreferenceFragment> mDetailFragmentClass;
    protected AppInfoDashboardFragment mParent;
    protected Preference mPreference;

    public AppInfoPreferenceControllerBase(Context context, String str) {
        super(context, str);
        this.mDetailFragmentClass = getDetailFragmentClass();
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), this.mPreferenceKey) && this.mDetailFragmentClass != null) {
            AppInfoDashboardFragment.startAppInfoFragment(this.mDetailFragmentClass, -1, getArguments(), this.mParent, this.mParent.getAppEntry());
            return true;
        }
        return false;
    }

    @Override // com.android.settings.applications.appinfo.AppInfoDashboardFragment.Callback
    public void refreshUi() {
        updateState(this.mPreference);
    }

    public void setParentFragment(AppInfoDashboardFragment appInfoDashboardFragment) {
        this.mParent = appInfoDashboardFragment;
        appInfoDashboardFragment.addToCallbackList(this);
    }

    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return null;
    }

    protected Bundle getArguments() {
        return null;
    }
}
