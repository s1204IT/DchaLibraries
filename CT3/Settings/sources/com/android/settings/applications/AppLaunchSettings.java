package com.android.settings.applications;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import com.android.settings.R;
import com.android.settings.Utils;
import java.util.List;

public class AppLaunchSettings extends AppInfoWithHeader implements View.OnClickListener, Preference.OnPreferenceChangeListener {
    private static final Intent sBrowserIntent = new Intent().setAction("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").setData(Uri.parse("http:"));
    private AppDomainsPreference mAppDomainUrls;
    private DropDownPreference mAppLinkState;
    private ClearDefaultsPreference mClearDefaultsPreference;
    private boolean mHasDomainUrls;
    private boolean mIsBrowser;
    private PackageManager mPm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.installed_app_launch_settings);
        this.mAppDomainUrls = (AppDomainsPreference) findPreference("app_launch_supported_domain_urls");
        this.mClearDefaultsPreference = (ClearDefaultsPreference) findPreference("app_launch_clear_defaults");
        this.mAppLinkState = (DropDownPreference) findPreference("app_link_state");
        this.mPm = getActivity().getPackageManager();
        int dcha_state = BenesseExtension.getDchaState();
        if (dcha_state == 0) {
            this.mIsBrowser = isBrowserApp(this.mPackageName);
        } else {
            this.mIsBrowser = false;
        }
        this.mHasDomainUrls = (this.mAppEntry.info.privateFlags & 16) != 0;
        if (!this.mIsBrowser) {
            List iviList = this.mPm.getIntentFilterVerifications(this.mPackageName);
            List filters = this.mPm.getAllIntentFilters(this.mPackageName);
            CharSequence[] entries = getEntries(this.mPackageName, iviList, filters);
            this.mAppDomainUrls.setTitles(entries);
            this.mAppDomainUrls.setValues(new int[entries.length]);
        }
        buildStateDropDown();
    }

    private boolean isBrowserApp(String packageName) {
        sBrowserIntent.setPackage(packageName);
        List<ResolveInfo> list = this.mPm.queryIntentActivitiesAsUser(sBrowserIntent, 131072, UserHandle.myUserId());
        int count = list.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo != null && info.handleAllWebDataURI) {
                return true;
            }
        }
        return false;
    }

    private void buildStateDropDown() {
        if (this.mIsBrowser) {
            this.mAppLinkState.setShouldDisableView(true);
            this.mAppLinkState.setEnabled(false);
            this.mAppDomainUrls.setShouldDisableView(true);
            this.mAppDomainUrls.setEnabled(false);
            return;
        }
        this.mAppLinkState.setEntries(new CharSequence[]{getString(R.string.app_link_open_always), getString(R.string.app_link_open_ask), getString(R.string.app_link_open_never)});
        this.mAppLinkState.setEntryValues(new CharSequence[]{Integer.toString(2), Integer.toString(4), Integer.toString(3)});
        this.mAppLinkState.setEnabled(this.mHasDomainUrls);
        if (!this.mHasDomainUrls) {
            return;
        }
        int state = this.mPm.getIntentVerificationStatusAsUser(this.mPackageName, UserHandle.myUserId());
        DropDownPreference dropDownPreference = this.mAppLinkState;
        if (state == 0) {
            state = 4;
        }
        dropDownPreference.setValue(Integer.toString(state));
        this.mAppLinkState.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return AppLaunchSettings.this.updateAppLinkState(Integer.parseInt((String) newValue));
            }
        });
    }

    public boolean updateAppLinkState(int newState) {
        if (this.mIsBrowser) {
            return false;
        }
        int userId = UserHandle.myUserId();
        int priorState = this.mPm.getIntentVerificationStatusAsUser(this.mPackageName, userId);
        if (priorState == newState) {
            return false;
        }
        boolean success = this.mPm.updateIntentVerificationStatusAsUser(this.mPackageName, newState, userId);
        if (success) {
            int updatedState = this.mPm.getIntentVerificationStatusAsUser(this.mPackageName, userId);
            return newState == updatedState;
        }
        Log.e("AppLaunchSettings", "Couldn't update intent verification status!");
        return success;
    }

    private CharSequence[] getEntries(String packageName, List<IntentFilterVerificationInfo> iviList, List<IntentFilter> filters) {
        ArraySet<String> result = Utils.getHandledDomains(this.mPm, packageName);
        return (CharSequence[]) result.toArray(new CharSequence[result.size()]);
    }

    @Override
    protected boolean refreshUi() {
        this.mClearDefaultsPreference.setPackageName(this.mPackageName);
        this.mClearDefaultsPreference.setAppEntry(this.mAppEntry);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    protected int getMetricsCategory() {
        return 17;
    }
}
