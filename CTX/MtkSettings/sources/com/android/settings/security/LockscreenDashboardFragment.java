package com.android.settings.security;

import android.content.Context;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.notification.LockScreenNotificationPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.security.OwnerInfoPreferenceController;
import com.android.settings.users.AddUserWhenLockedPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LockscreenDashboardFragment extends DashboardFragment implements OwnerInfoPreferenceController.OwnerInfoCallback {
    static final String KEY_ADD_USER_FROM_LOCK_SCREEN = "security_lockscreen_add_users_when_locked";
    static final String KEY_LOCK_SCREEN_NOTIFICATON = "security_setting_lock_screen_notif";
    static final String KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE = "security_setting_lock_screen_notif_work";
    static final String KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER = "security_setting_lock_screen_notif_work_header";
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean z) {
            SearchIndexableResource searchIndexableResource = new SearchIndexableResource(context);
            searchIndexableResource.xmlResId = R.xml.security_lockscreen_settings;
            return Arrays.asList(searchIndexableResource);
        }

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            ArrayList arrayList = new ArrayList();
            arrayList.add(new LockScreenNotificationPreferenceController(context));
            arrayList.add(new AddUserWhenLockedPreferenceController(context, LockscreenDashboardFragment.KEY_ADD_USER_FROM_LOCK_SCREEN, null));
            arrayList.add(new OwnerInfoPreferenceController(context, null, null));
            arrayList.add(new LockdownButtonPreferenceController(context));
            return arrayList;
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            List<String> nonIndexableKeys = super.getNonIndexableKeys(context);
            nonIndexableKeys.add(LockscreenDashboardFragment.KEY_ADD_USER_FROM_LOCK_SCREEN);
            nonIndexableKeys.add(LockscreenDashboardFragment.KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE);
            return nonIndexableKeys;
        }
    };
    private OwnerInfoPreferenceController mOwnerInfoPreferenceController;

    @Override
    public int getMetricsCategory() {
        return 882;
    }

    @Override
    protected String getLogTag() {
        return "LockscreenDashboardFragment";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.security_lockscreen_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_lockscreen;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        ArrayList arrayList = new ArrayList();
        Lifecycle lifecycle = getLifecycle();
        LockScreenNotificationPreferenceController lockScreenNotificationPreferenceController = new LockScreenNotificationPreferenceController(context, KEY_LOCK_SCREEN_NOTIFICATON, KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE_HEADER, KEY_LOCK_SCREEN_NOTIFICATON_WORK_PROFILE);
        lifecycle.addObserver(lockScreenNotificationPreferenceController);
        arrayList.add(lockScreenNotificationPreferenceController);
        arrayList.add(new AddUserWhenLockedPreferenceController(context, KEY_ADD_USER_FROM_LOCK_SCREEN, lifecycle));
        this.mOwnerInfoPreferenceController = new OwnerInfoPreferenceController(context, this, lifecycle);
        arrayList.add(this.mOwnerInfoPreferenceController);
        arrayList.add(new LockdownButtonPreferenceController(context));
        return arrayList;
    }

    @Override
    public void onOwnerInfoUpdated() {
        if (this.mOwnerInfoPreferenceController != null) {
            this.mOwnerInfoPreferenceController.updateSummary();
        }
    }
}
