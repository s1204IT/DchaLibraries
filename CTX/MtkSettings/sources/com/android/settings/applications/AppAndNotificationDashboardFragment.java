package com.android.settings.applications;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.EmergencyBroadcastPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppAndNotificationDashboardFragment extends DashboardFragment {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.app_and_notification;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return AppAndNotificationDashboardFragment.buildPreferenceControllers(context, null, null);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add(new SpecialAppAccessPreferenceController(context).getPreferenceKey());
            return nonIndexableKeys;
        }
    };

    @Override
    public int getMetricsCategory() {
        return 748;
    }

    @Override
    protected String getLogTag() {
        return "AppAndNotifDashboard";
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_apps_and_notifications;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.app_and_notification;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        Application application;
        Activity activity = getActivity();
        if (activity != null) {
            application = activity.getApplication();
        } else {
            application = null;
        }
        return buildPreferenceControllers(context, application, this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Application application, Fragment fragment) {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new EmergencyBroadcastPreferenceController(context, "app_and_notif_cell_broadcast_settings"));
        arrayList.add(new SpecialAppAccessPreferenceController(context));
        arrayList.add(new RecentAppsPreferenceController(context, application, fragment));
        return arrayList;
    }
}
