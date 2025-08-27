package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;

/* loaded from: classes.dex */
public class AppHeaderViewPreferenceController extends BasePreferenceController implements AppInfoDashboardFragment.Callback, LifecycleObserver, OnStart {
    private static final String KEY_HEADER = "header_view";
    private EntityHeaderController mEntityHeaderController;
    private LayoutPreference mHeader;
    private final Lifecycle mLifecycle;
    private final String mPackageName;
    private final AppInfoDashboardFragment mParent;

    public AppHeaderViewPreferenceController(Context context, AppInfoDashboardFragment appInfoDashboardFragment, String str, Lifecycle lifecycle) {
        super(context, KEY_HEADER);
        this.mParent = appInfoDashboardFragment;
        this.mPackageName = str;
        this.mLifecycle = lifecycle;
        if (this.mLifecycle != null) {
            this.mLifecycle.addObserver(this);
        }
    }

    @Override // com.android.settings.core.BasePreferenceController
    public int getAvailabilityStatus() {
        return 0;
    }

    @Override // com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mHeader = (LayoutPreference) preferenceScreen.findPreference(KEY_HEADER);
        this.mEntityHeaderController = EntityHeaderController.newInstance(this.mParent.getActivity(), this.mParent, this.mHeader.findViewById(R.id.entity_header)).setPackageName(this.mPackageName).setButtonActions(0, 0).bindHeaderButtons();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        this.mEntityHeaderController.setRecyclerView(this.mParent.getListView(), this.mLifecycle).styleActionBar(this.mParent.getActivity());
    }

    @Override // com.android.settings.applications.appinfo.AppInfoDashboardFragment.Callback
    public void refreshUi() {
        setAppLabelAndIcon(this.mParent.getPackageInfo(), this.mParent.getAppEntry());
    }

    private void setAppLabelAndIcon(PackageInfo packageInfo, ApplicationsState.AppEntry appEntry) {
        Activity activity = this.mParent.getActivity();
        boolean zIsInstant = AppUtils.isInstant(packageInfo.applicationInfo);
        this.mEntityHeaderController.setLabel(appEntry).setIcon(appEntry).setSummary(zIsInstant ? null : this.mContext.getString(Utils.getInstallationStatus(appEntry.info))).setIsInstantApp(zIsInstant).done(activity, false);
    }
}
