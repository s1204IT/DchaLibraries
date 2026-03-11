package com.android.settings.notification;

import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.notification.NotifyingApp;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.StringUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RecentNotifyingAppsPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    static final String KEY_DIVIDER = "all_notifications_divider";
    static final String KEY_SEE_ALL = "all_notifications";
    private static final Set<String> SKIP_SYSTEM_PACKAGES = new ArraySet();
    private final ApplicationsState mApplicationsState;
    private List<NotifyingApp> mApps;
    private PreferenceCategory mCategory;
    private Preference mDivider;
    private final Fragment mHost;
    private final IconDrawableFactory mIconDrawableFactory;
    private final NotificationBackend mNotificationBackend;
    private final PackageManager mPm;
    private Preference mSeeAllPref;
    private final int mUserId;

    static {
        SKIP_SYSTEM_PACKAGES.addAll(Arrays.asList("android", "com.android.phone", "com.android.settings", "com.android.systemui", "com.android.providers.calendar", "com.android.providers.media"));
    }

    public RecentNotifyingAppsPreferenceController(Context context, NotificationBackend notificationBackend, Application application, Fragment fragment) {
        this(context, notificationBackend, application == null ? null : ApplicationsState.getInstance(application), fragment);
    }

    RecentNotifyingAppsPreferenceController(Context context, NotificationBackend notificationBackend, ApplicationsState applicationsState, Fragment fragment) {
        super(context);
        this.mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        this.mUserId = UserHandle.myUserId();
        this.mPm = context.getPackageManager();
        this.mHost = fragment;
        this.mApplicationsState = applicationsState;
        this.mNotificationBackend = notificationBackend;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return "recent_notifications_category";
    }

    @Override
    public void updateNonIndexableKeys(List<String> list) {
        super.updateNonIndexableKeys(list);
        list.add("recent_notifications_category");
        list.add(KEY_DIVIDER);
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mCategory = (PreferenceCategory) preferenceScreen.findPreference(getPreferenceKey());
        this.mSeeAllPref = preferenceScreen.findPreference(KEY_SEE_ALL);
        this.mDivider = preferenceScreen.findPreference(KEY_DIVIDER);
        super.displayPreference(preferenceScreen);
        refreshUi(this.mCategory.getContext());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshUi(this.mCategory.getContext());
        this.mSeeAllPref.setTitle(this.mContext.getString(R.string.recent_notifications_see_all_title));
    }

    void refreshUi(Context context) {
        reloadData();
        List<NotifyingApp> displayableRecentAppList = getDisplayableRecentAppList();
        if (displayableRecentAppList != null && !displayableRecentAppList.isEmpty()) {
            displayRecentApps(context, displayableRecentAppList);
        } else {
            displayOnlyAllAppsLink();
        }
    }

    void reloadData() {
        this.mApps = this.mNotificationBackend.getRecentApps();
    }

    private void displayOnlyAllAppsLink() {
        this.mCategory.setTitle((CharSequence) null);
        this.mDivider.setVisible(false);
        this.mSeeAllPref.setTitle(R.string.notifications_title);
        this.mSeeAllPref.setIcon((Drawable) null);
        for (int preferenceCount = this.mCategory.getPreferenceCount() - 1; preferenceCount >= 0; preferenceCount--) {
            Preference preference = this.mCategory.getPreference(preferenceCount);
            if (!TextUtils.equals(preference.getKey(), KEY_SEE_ALL)) {
                this.mCategory.removePreference(preference);
            }
        }
    }

    private void displayRecentApps(Context context, List<NotifyingApp> list) {
        boolean z;
        this.mCategory.setTitle(R.string.recent_notifications);
        this.mDivider.setVisible(true);
        this.mSeeAllPref.setSummary((CharSequence) null);
        this.mSeeAllPref.setIcon(R.drawable.ic_chevron_right_24dp);
        ArrayMap arrayMap = new ArrayMap();
        int preferenceCount = this.mCategory.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = this.mCategory.getPreference(i);
            String key = preference.getKey();
            if (!TextUtils.equals(key, KEY_SEE_ALL)) {
                arrayMap.put(key, (NotificationAppPreference) preference);
            }
        }
        int size = list.size();
        for (int i2 = 0; i2 < size; i2++) {
            NotifyingApp notifyingApp = list.get(i2);
            final String str = notifyingApp.getPackage();
            final ApplicationsState.AppEntry entry = this.mApplicationsState.getEntry(notifyingApp.getPackage(), this.mUserId);
            if (entry != null) {
                NotificationAppPreference notificationAppPreference = (NotificationAppPreference) arrayMap.remove(str);
                if (notificationAppPreference == null) {
                    notificationAppPreference = new NotificationAppPreference(context);
                    z = false;
                } else {
                    z = true;
                }
                notificationAppPreference.setKey(str);
                notificationAppPreference.setTitle(entry.label);
                notificationAppPreference.setIcon(this.mIconDrawableFactory.getBadgedIcon(entry.info));
                notificationAppPreference.setIconSize(2);
                notificationAppPreference.setSummary(StringUtil.formatRelativeTime(this.mContext, System.currentTimeMillis() - notifyingApp.getLastNotified(), true));
                notificationAppPreference.setOrder(i2);
                Bundle bundle = new Bundle();
                bundle.putString("package", str);
                bundle.putInt("uid", entry.info.uid);
                notificationAppPreference.setIntent(new SubSettingLauncher(this.mHost.getActivity()).setDestination(AppNotificationSettings.class.getName()).setTitle(R.string.notifications_title).setArguments(bundle).setSourceMetricsCategory(133).toIntent());
                notificationAppPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public final boolean onPreferenceChange(Preference preference2, Object obj) {
                        return RecentNotifyingAppsPreferenceController.lambda$displayRecentApps$0(this.f$0, str, entry, preference2, obj);
                    }
                });
                notificationAppPreference.setChecked(!this.mNotificationBackend.getNotificationsBanned(str, entry.info.uid));
                if (!z) {
                    this.mCategory.addPreference(notificationAppPreference);
                }
            }
        }
        Iterator it = arrayMap.values().iterator();
        while (it.hasNext()) {
            this.mCategory.removePreference((Preference) it.next());
        }
    }

    public static boolean lambda$displayRecentApps$0(RecentNotifyingAppsPreferenceController recentNotifyingAppsPreferenceController, String str, ApplicationsState.AppEntry appEntry, Preference preference, Object obj) {
        recentNotifyingAppsPreferenceController.mNotificationBackend.setNotificationsEnabledForPackage(str, appEntry.info.uid, !(((Boolean) obj).booleanValue() ^ true));
        return true;
    }

    private List<NotifyingApp> getDisplayableRecentAppList() {
        Collections.sort(this.mApps);
        ArrayList arrayList = new ArrayList(5);
        int i = 0;
        for (NotifyingApp notifyingApp : this.mApps) {
            if (this.mApplicationsState.getEntry(notifyingApp.getPackage(), this.mUserId) != null && shouldIncludePkgInRecents(notifyingApp.getPackage())) {
                arrayList.add(notifyingApp);
                i++;
                if (i >= 5) {
                    break;
                }
            }
        }
        return arrayList;
    }

    private boolean shouldIncludePkgInRecents(String str) {
        if (SKIP_SYSTEM_PACKAGES.contains(str)) {
            Log.d("RecentNotisCtrl", "System package, skipping " + str);
            return false;
        }
        if (this.mPm.resolveActivity(new Intent().addCategory("android.intent.category.LAUNCHER").setPackage(str), 0) == null) {
            ApplicationsState.AppEntry entry = this.mApplicationsState.getEntry(str, this.mUserId);
            if (entry == null || entry.info == null || !AppUtils.isInstant(entry.info)) {
                Log.d("RecentNotisCtrl", "Not a user visible or instant app, skipping " + str);
                return false;
            }
            return true;
        }
        return true;
    }
}
