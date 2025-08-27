package com.android.settings.location;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.AppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class RecentLocationRequestPreferenceController extends LocationBasePreferenceController {
    static final String KEY_SEE_ALL_BUTTON = "recent_location_requests_see_all_button";
    private PreferenceCategory mCategoryRecentLocationRequests;
    private final LocationSettings mFragment;
    private final RecentLocationApps mRecentLocationApps;
    private Preference mSeeAllButton;

    static class PackageEntryClickedListener implements Preference.OnPreferenceClickListener {
        private final DashboardFragment mFragment;
        private final String mPackage;
        private final UserHandle mUserHandle;

        public PackageEntryClickedListener(DashboardFragment dashboardFragment, String str, UserHandle userHandle) {
            this.mFragment = dashboardFragment;
            this.mPackage = str;
            this.mUserHandle = userHandle;
        }

        @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
        public boolean onPreferenceClick(Preference preference) {
            Bundle bundle = new Bundle();
            bundle.putString("package", this.mPackage);
            new SubSettingLauncher(this.mFragment.getContext()).setDestination(AppInfoDashboardFragment.class.getName()).setArguments(bundle).setTitle(R.string.application_info_label).setUserHandle(this.mUserHandle).setSourceMetricsCategory(this.mFragment.getMetricsCategory()).launch();
            return true;
        }
    }

    public RecentLocationRequestPreferenceController(Context context, LocationSettings locationSettings, Lifecycle lifecycle) {
        this(context, locationSettings, lifecycle, new RecentLocationApps(context));
    }

    RecentLocationRequestPreferenceController(Context context, LocationSettings locationSettings, Lifecycle lifecycle, RecentLocationApps recentLocationApps) {
        super(context, lifecycle);
        this.mFragment = locationSettings;
        this.mRecentLocationApps = recentLocationApps;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "recent_location_requests";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mCategoryRecentLocationRequests = (PreferenceCategory) preferenceScreen.findPreference("recent_location_requests");
        this.mSeeAllButton = preferenceScreen.findPreference(KEY_SEE_ALL_BUTTON);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        this.mCategoryRecentLocationRequests.removeAll();
        this.mSeeAllButton.setVisible(false);
        Context context = preference.getContext();
        List<RecentLocationApps.Request> appListSorted = this.mRecentLocationApps.getAppListSorted();
        if (appListSorted.size() > 3) {
            for (int i = 0; i < 3; i++) {
                this.mCategoryRecentLocationRequests.addPreference(createAppPreference(context, appListSorted.get(i)));
            }
            this.mSeeAllButton.setVisible(true);
            return;
        }
        if (appListSorted.size() > 0) {
            Iterator<RecentLocationApps.Request> it = appListSorted.iterator();
            while (it.hasNext()) {
                this.mCategoryRecentLocationRequests.addPreference(createAppPreference(context, it.next()));
            }
            return;
        }
        AppPreference appPreferenceCreateAppPreference = createAppPreference(context);
        appPreferenceCreateAppPreference.setTitle(R.string.location_no_recent_apps);
        appPreferenceCreateAppPreference.setSelectable(false);
        this.mCategoryRecentLocationRequests.addPreference(appPreferenceCreateAppPreference);
    }

    @Override // com.android.settings.location.LocationEnabler.LocationModeChangeListener
    public void onLocationModeChanged(int i, boolean z) {
        this.mCategoryRecentLocationRequests.setEnabled(this.mLocationEnabler.isEnabled(i));
    }

    AppPreference createAppPreference(Context context) {
        return new AppPreference(context);
    }

    AppPreference createAppPreference(Context context, RecentLocationApps.Request request) {
        AppPreference appPreferenceCreateAppPreference = createAppPreference(context);
        appPreferenceCreateAppPreference.setSummary(request.contentDescription);
        appPreferenceCreateAppPreference.setIcon(request.icon);
        appPreferenceCreateAppPreference.setTitle(request.label);
        appPreferenceCreateAppPreference.setOnPreferenceClickListener(new PackageEntryClickedListener(this.mFragment, request.packageName, request.userHandle));
        return appPreferenceCreateAppPreference;
    }
}
