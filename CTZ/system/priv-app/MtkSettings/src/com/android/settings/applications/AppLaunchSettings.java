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

/* loaded from: classes.dex */
public class AppLaunchSettings extends AppInfoWithHeader implements Preference.OnPreferenceChangeListener, View.OnClickListener {
    private static final Intent sBrowserIntent = new Intent().setAction("android.intent.action.VIEW").addCategory("android.intent.category.BROWSABLE").setData(Uri.parse("http:"));
    private AppDomainsPreference mAppDomainUrls;
    private DropDownPreference mAppLinkState;
    private ClearDefaultsPreference mClearDefaultsPreference;
    private boolean mHasDomainUrls;
    private boolean mIsBrowser;
    private PackageManager mPm;

    @Override // com.android.settings.applications.AppInfoBase, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.installed_app_launch_settings);
        this.mAppDomainUrls = (AppDomainsPreference) findPreference("app_launch_supported_domain_urls");
        this.mClearDefaultsPreference = (ClearDefaultsPreference) findPreference("app_launch_clear_defaults");
        this.mAppLinkState = (DropDownPreference) findPreference("app_link_state");
        this.mPm = getActivity().getPackageManager();
        if (BenesseExtension.getDchaState() == 0) {
            this.mIsBrowser = isBrowserApp(this.mPackageName);
        } else {
            this.mIsBrowser = false;
        }
        this.mHasDomainUrls = (this.mAppEntry.info.privateFlags & 16) != 0;
        if (!this.mIsBrowser) {
            CharSequence[] entries = getEntries(this.mPackageName, this.mPm.getIntentFilterVerifications(this.mPackageName), this.mPm.getAllIntentFilters(this.mPackageName));
            this.mAppDomainUrls.setTitles(entries);
            this.mAppDomainUrls.setValues(new int[entries.length]);
        }
        buildStateDropDown();
    }

    private boolean isBrowserApp(String str) {
        sBrowserIntent.setPackage(str);
        List listQueryIntentActivitiesAsUser = this.mPm.queryIntentActivitiesAsUser(sBrowserIntent, 131072, UserHandle.myUserId());
        int size = listQueryIntentActivitiesAsUser.size();
        for (int i = 0; i < size; i++) {
            ResolveInfo resolveInfo = (ResolveInfo) listQueryIntentActivitiesAsUser.get(i);
            if (resolveInfo.activityInfo != null && resolveInfo.handleAllWebDataURI) {
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
        if (this.mHasDomainUrls) {
            int intentVerificationStatusAsUser = this.mPm.getIntentVerificationStatusAsUser(this.mPackageName, UserHandle.myUserId());
            DropDownPreference dropDownPreference = this.mAppLinkState;
            if (intentVerificationStatusAsUser == 0) {
                intentVerificationStatusAsUser = 4;
            }
            dropDownPreference.setValue(Integer.toString(intentVerificationStatusAsUser));
            this.mAppLinkState.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() { // from class: com.android.settings.applications.AppLaunchSettings.1
                @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
                public boolean onPreferenceChange(Preference preference, Object obj) {
                    return AppLaunchSettings.this.updateAppLinkState(Integer.parseInt((String) obj));
                }
            });
        }
    }

    private boolean updateAppLinkState(int i) {
        if (this.mIsBrowser) {
            return false;
        }
        int iMyUserId = UserHandle.myUserId();
        if (this.mPm.getIntentVerificationStatusAsUser(this.mPackageName, iMyUserId) == i) {
            return false;
        }
        boolean zUpdateIntentVerificationStatusAsUser = this.mPm.updateIntentVerificationStatusAsUser(this.mPackageName, i, iMyUserId);
        if (zUpdateIntentVerificationStatusAsUser) {
            return i == this.mPm.getIntentVerificationStatusAsUser(this.mPackageName, iMyUserId);
        }
        Log.e("AppLaunchSettings", "Couldn't update intent verification status!");
        return zUpdateIntentVerificationStatusAsUser;
    }

    private CharSequence[] getEntries(String str, List<IntentFilterVerificationInfo> list, List<IntentFilter> list2) {
        ArraySet<String> handledDomains = Utils.getHandledDomains(this.mPm, str);
        return (CharSequence[]) handledDomains.toArray(new CharSequence[handledDomains.size()]);
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected boolean refreshUi() {
        this.mClearDefaultsPreference.setPackageName(this.mPackageName);
        this.mClearDefaultsPreference.setAppEntry(this.mAppEntry);
        return true;
    }

    @Override // com.android.settings.applications.AppInfoBase
    protected AlertDialog createDialog(int i, int i2) {
        return null;
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        return true;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 17;
    }
}
