package com.android.settings.location;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.location.RecentLocationRequestPreferenceController;
import com.android.settings.widget.AppPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.location.RecentLocationApps;
import java.util.Iterator;

/* loaded from: classes.dex */
public class RecentLocationRequestSeeAllPreferenceController extends LocationBasePreferenceController {
    private PreferenceCategory mCategoryAllRecentLocationRequests;
    private final RecentLocationRequestSeeAllFragment mFragment;
    private RecentLocationApps mRecentLocationApps;

    public RecentLocationRequestSeeAllPreferenceController(Context context, Lifecycle lifecycle, RecentLocationRequestSeeAllFragment recentLocationRequestSeeAllFragment) {
        this(context, lifecycle, recentLocationRequestSeeAllFragment, new RecentLocationApps(context));
    }

    RecentLocationRequestSeeAllPreferenceController(Context context, Lifecycle lifecycle, RecentLocationRequestSeeAllFragment recentLocationRequestSeeAllFragment, RecentLocationApps recentLocationApps) {
        super(context, lifecycle);
        this.mFragment = recentLocationRequestSeeAllFragment;
        this.mRecentLocationApps = recentLocationApps;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "all_recent_location_requests";
    }

    @Override // com.android.settings.location.LocationEnabler.LocationModeChangeListener
    public void onLocationModeChanged(int i, boolean z) {
        this.mCategoryAllRecentLocationRequests.setEnabled(this.mLocationEnabler.isEnabled(i));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mCategoryAllRecentLocationRequests = (PreferenceCategory) preferenceScreen.findPreference("all_recent_location_requests");
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        this.mCategoryAllRecentLocationRequests.removeAll();
        Iterator<RecentLocationApps.Request> it = this.mRecentLocationApps.getAppListSorted().iterator();
        while (it.hasNext()) {
            this.mCategoryAllRecentLocationRequests.addPreference(createAppPreference(preference.getContext(), it.next()));
        }
    }

    AppPreference createAppPreference(Context context, RecentLocationApps.Request request) {
        AppPreference appPreference = new AppPreference(context);
        appPreference.setSummary(request.contentDescription);
        appPreference.setIcon(request.icon);
        appPreference.setTitle(request.label);
        appPreference.setOnPreferenceClickListener(new RecentLocationRequestPreferenceController.PackageEntryClickedListener(this.mFragment, request.packageName, request.userHandle));
        return appPreference;
    }
}
