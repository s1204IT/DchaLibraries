package com.android.settings.notification;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
public class ConfigureNotificationSettings extends DashboardFragment {
    static final String KEY_LOCKSCREEN = "lock_screen_notifications";
    static final String KEY_LOCKSCREEN_WORK_PROFILE = "lock_screen_notifications_profile";
    static final String KEY_LOCKSCREEN_WORK_PROFILE_HEADER = "lock_screen_notifications_profile_header";
    static final String KEY_SWIPE_DOWN = "gesture_swipe_down_fingerprint_notifications";
    private RingtonePreference mRequestPreference;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() { // from class: com.android.settings.notification.ConfigureNotificationSettings.2
        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProviderFactory
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() { // from class: com.android.settings.notification.ConfigureNotificationSettings.3
        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.configure_notification_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return ConfigureNotificationSettings.buildPreferenceControllers(context, null, null, null);
        }

        @Override // com.android.settings.search.BaseSearchIndexProvider, com.android.settings.search.Indexable.SearchIndexProvider
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add(ConfigureNotificationSettings.KEY_SWIPE_DOWN);
            nonIndexableKeys.add(ConfigureNotificationSettings.KEY_LOCKSCREEN);
            nonIndexableKeys.add(ConfigureNotificationSettings.KEY_LOCKSCREEN_WORK_PROFILE);
            nonIndexableKeys.add(ConfigureNotificationSettings.KEY_LOCKSCREEN_WORK_PROFILE_HEADER);
            nonIndexableKeys.add("zen_mode_notifications");
            return nonIndexableKeys;
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 337;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected String getLogTag() {
        return "ConfigNotiSettings";
    }

    @Override // com.android.settings.dashboard.DashboardFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.configure_notification_settings;
    }

    @Override // com.android.settings.dashboard.DashboardFragment
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        Application application;
        Activity activity = getActivity();
        if (activity != null) {
            application = activity.getApplication();
        } else {
            application = null;
        }
        return buildPreferenceControllers(context, getLifecycle(), application, this);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context, Lifecycle lifecycle, Application application, Fragment fragment) {
        ArrayList arrayList = new ArrayList();
        PulseNotificationPreferenceController pulseNotificationPreferenceController = new PulseNotificationPreferenceController(context);
        LockScreenNotificationPreferenceController lockScreenNotificationPreferenceController = new LockScreenNotificationPreferenceController(context, KEY_LOCKSCREEN, KEY_LOCKSCREEN_WORK_PROFILE_HEADER, KEY_LOCKSCREEN_WORK_PROFILE);
        if (lifecycle != null) {
            lifecycle.addObserver(pulseNotificationPreferenceController);
            lifecycle.addObserver(lockScreenNotificationPreferenceController);
        }
        arrayList.add(new RecentNotifyingAppsPreferenceController(context, new NotificationBackend(), application, fragment));
        arrayList.add(pulseNotificationPreferenceController);
        arrayList.add(lockScreenNotificationPreferenceController);
        arrayList.add(new NotificationRingtonePreferenceController(context) { // from class: com.android.settings.notification.ConfigureNotificationSettings.1
            @Override // com.android.settings.notification.NotificationRingtonePreferenceController, com.android.settingslib.core.AbstractPreferenceController
            public String getPreferenceKey() {
                return "notification_default_ringtone";
            }
        });
        arrayList.add(new ZenModePreferenceController(context, lifecycle, "zen_mode_notifications"));
        return arrayList;
    }

    @Override // com.android.settings.dashboard.DashboardFragment, android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            this.mRequestPreference = (RingtonePreference) preference;
            this.mRequestPreference.onPrepareRingtonePickerIntent(this.mRequestPreference.getIntent());
            startActivityForResultAsUser(this.mRequestPreference.getIntent(), 200, null, UserHandle.of(this.mRequestPreference.getUserId()));
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        if (this.mRequestPreference != null) {
            this.mRequestPreference.onActivityResult(i, i2, intent);
            this.mRequestPreference = null;
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        if (this.mRequestPreference != null) {
            bundle.putString("selected_preference", this.mRequestPreference.getKey());
        }
    }

    static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private NotificationBackend mBackend = new NotificationBackend();
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        protected void setBackend(NotificationBackend notificationBackend) {
            this.mBackend = notificationBackend;
        }

        @Override // com.android.settings.dashboard.SummaryLoader.SummaryProvider
        public void setListening(boolean z) {
            if (!z) {
                return;
            }
            int blockedAppCount = this.mBackend.getBlockedAppCount();
            if (blockedAppCount == 0) {
                this.mSummaryLoader.setSummary(this, this.mContext.getText(R.string.app_notification_listing_summary_zero));
            } else {
                this.mSummaryLoader.setSummary(this, this.mContext.getResources().getQuantityString(R.plurals.app_notification_listing_summary_others, blockedAppCount, Integer.valueOf(blockedAppCount)));
            }
        }
    }
}
