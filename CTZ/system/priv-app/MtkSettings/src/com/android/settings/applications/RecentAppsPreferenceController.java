package com.android.settings.applications;

import android.app.Application;
import android.app.Fragment;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.AppPreference;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public class RecentAppsPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, Comparator<UsageStats> {
    static final String KEY_DIVIDER = "all_app_info_divider";
    static final String KEY_SEE_ALL = "all_app_info";
    private static final Set<String> SKIP_SYSTEM_PACKAGES = new ArraySet();
    private final ApplicationsState mApplicationsState;
    private Calendar mCal;
    private PreferenceCategory mCategory;
    private Preference mDivider;
    private boolean mHasRecentApps;
    private final Fragment mHost;
    private final IconDrawableFactory mIconDrawableFactory;
    private final PackageManager mPm;
    private Preference mSeeAllPref;
    private List<UsageStats> mStats;
    private final UsageStatsManager mUsageStatsManager;
    private final int mUserId;

    static {
        SKIP_SYSTEM_PACKAGES.addAll(Arrays.asList("android", "com.android.phone", "com.android.settings", "com.android.systemui", "com.android.providers.calendar", "com.android.providers.media"));
    }

    public RecentAppsPreferenceController(Context context, Application application, Fragment fragment) {
        this(context, application == null ? null : ApplicationsState.getInstance(application), fragment);
    }

    RecentAppsPreferenceController(Context context, ApplicationsState applicationsState, Fragment fragment) {
        super(context);
        this.mIconDrawableFactory = IconDrawableFactory.newInstance(context);
        this.mUserId = UserHandle.myUserId();
        this.mPm = context.getPackageManager();
        this.mHost = fragment;
        this.mUsageStatsManager = (UsageStatsManager) context.getSystemService("usagestats");
        this.mApplicationsState = applicationsState;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "recent_apps_category";
    }

    @Override // com.android.settings.core.PreferenceControllerMixin
    public void updateNonIndexableKeys(List<String> list) {
        super.updateNonIndexableKeys(list);
        list.add("recent_apps_category");
        list.add(KEY_DIVIDER);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mCategory = (PreferenceCategory) preferenceScreen.findPreference(getPreferenceKey());
        this.mSeeAllPref = preferenceScreen.findPreference(KEY_SEE_ALL);
        this.mDivider = preferenceScreen.findPreference(KEY_DIVIDER);
        super.displayPreference(preferenceScreen);
        refreshUi(this.mCategory.getContext());
    }

    /* JADX WARN: Type inference failed for: r4v3, types: [com.android.settings.applications.RecentAppsPreferenceController$1] */
    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshUi(this.mCategory.getContext());
        new InstalledAppCounter(this.mContext, -1, new PackageManagerWrapper(this.mContext.getPackageManager())) { // from class: com.android.settings.applications.RecentAppsPreferenceController.1
            @Override // com.android.settings.applications.AppCounter
            protected void onCountComplete(int i) {
                if (RecentAppsPreferenceController.this.mHasRecentApps) {
                    RecentAppsPreferenceController.this.mSeeAllPref.setTitle(RecentAppsPreferenceController.this.mContext.getString(R.string.see_all_apps_title, Integer.valueOf(i)));
                } else {
                    RecentAppsPreferenceController.this.mSeeAllPref.setSummary(RecentAppsPreferenceController.this.mContext.getString(R.string.apps_summary, Integer.valueOf(i)));
                }
            }
        }.execute(new Void[0]);
    }

    /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
    @Override // java.util.Comparator
    public final int compare(UsageStats usageStats, UsageStats usageStats2) {
        return Long.compare(usageStats2.getLastTimeUsed(), usageStats.getLastTimeUsed());
    }

    void refreshUi(Context context) {
        reloadData();
        List<UsageStats> displayableRecentAppList = getDisplayableRecentAppList();
        if (displayableRecentAppList != null && !displayableRecentAppList.isEmpty()) {
            this.mHasRecentApps = true;
            displayRecentApps(context, displayableRecentAppList);
        } else {
            this.mHasRecentApps = false;
            displayOnlyAppInfo();
        }
    }

    void reloadData() {
        this.mCal = Calendar.getInstance();
        this.mCal.add(6, -1);
        this.mStats = this.mUsageStatsManager.queryUsageStats(4, this.mCal.getTimeInMillis(), System.currentTimeMillis());
    }

    private void displayOnlyAppInfo() {
        this.mCategory.setTitle((CharSequence) null);
        this.mDivider.setVisible(false);
        this.mSeeAllPref.setTitle(R.string.applications_settings);
        this.mSeeAllPref.setIcon((Drawable) null);
        for (int preferenceCount = this.mCategory.getPreferenceCount() - 1; preferenceCount >= 0; preferenceCount--) {
            Preference preference = this.mCategory.getPreference(preferenceCount);
            if (!TextUtils.equals(preference.getKey(), KEY_SEE_ALL)) {
                this.mCategory.removePreference(preference);
            }
        }
    }

    private void displayRecentApps(Context context, List<UsageStats> list) {
        boolean z;
        this.mCategory.setTitle(R.string.recent_app_category_title);
        this.mDivider.setVisible(true);
        this.mSeeAllPref.setSummary((CharSequence) null);
        this.mSeeAllPref.setIcon(R.drawable.ic_chevron_right_24dp);
        ArrayMap arrayMap = new ArrayMap();
        int preferenceCount = this.mCategory.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = this.mCategory.getPreference(i);
            String key = preference.getKey();
            if (!TextUtils.equals(key, KEY_SEE_ALL)) {
                arrayMap.put(key, preference);
            }
        }
        int size = list.size();
        for (int i2 = 0; i2 < size; i2++) {
            final String packageName = list.get(i2).getPackageName();
            final ApplicationsState.AppEntry entry = this.mApplicationsState.getEntry(packageName, this.mUserId);
            if (entry != null) {
                Preference appPreference = (Preference) arrayMap.remove(packageName);
                if (appPreference == null) {
                    appPreference = new AppPreference(context);
                    z = false;
                } else {
                    z = true;
                }
                appPreference.setKey(packageName);
                appPreference.setTitle(entry.label);
                appPreference.setIcon(this.mIconDrawableFactory.getBadgedIcon(entry.info));
                appPreference.setSummary(StringUtil.formatRelativeTime(this.mContext, System.currentTimeMillis() - r7.getLastTimeUsed(), false));
                appPreference.setOrder(i2);
                appPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.settings.applications.-$$Lambda$RecentAppsPreferenceController$benLpqwf0HURWhX82bB7mmwJ8Oo
                    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
                    public final boolean onPreferenceClick(Preference preference2) {
                        return RecentAppsPreferenceController.lambda$displayRecentApps$0(this.f$0, packageName, entry, preference2);
                    }
                });
                if (!z) {
                    this.mCategory.addPreference(appPreference);
                }
            }
        }
        Iterator it = arrayMap.values().iterator();
        while (it.hasNext()) {
            this.mCategory.removePreference((Preference) it.next());
        }
    }

    public static /* synthetic */ boolean lambda$displayRecentApps$0(RecentAppsPreferenceController recentAppsPreferenceController, String str, ApplicationsState.AppEntry appEntry, Preference preference) {
        AppInfoBase.startAppInfoFragment(AppInfoDashboardFragment.class, R.string.application_info_label, str, appEntry.info.uid, recentAppsPreferenceController.mHost, 1001, 748);
        return true;
    }

    private List<UsageStats> getDisplayableRecentAppList() {
        ArrayList arrayList = new ArrayList();
        ArrayMap arrayMap = new ArrayMap();
        int size = this.mStats.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            UsageStats usageStats = this.mStats.get(i2);
            if (shouldIncludePkgInRecents(usageStats)) {
                String packageName = usageStats.getPackageName();
                UsageStats usageStats2 = (UsageStats) arrayMap.get(packageName);
                if (usageStats2 == null) {
                    arrayMap.put(packageName, usageStats);
                } else {
                    usageStats2.add(usageStats);
                }
            }
        }
        ArrayList<UsageStats> arrayList2 = new ArrayList();
        arrayList2.addAll(arrayMap.values());
        Collections.sort(arrayList2, this);
        for (UsageStats usageStats3 : arrayList2) {
            if (this.mApplicationsState.getEntry(usageStats3.getPackageName(), this.mUserId) != null) {
                arrayList.add(usageStats3);
                i++;
                if (i >= 5) {
                    break;
                }
            }
        }
        return arrayList;
    }

    private boolean shouldIncludePkgInRecents(UsageStats usageStats) {
        String packageName = usageStats.getPackageName();
        if (usageStats.getLastTimeUsed() < this.mCal.getTimeInMillis()) {
            Log.d("RecentAppsCtrl", "Invalid timestamp, skipping " + packageName);
            return false;
        }
        if (SKIP_SYSTEM_PACKAGES.contains(packageName)) {
            Log.d("RecentAppsCtrl", "System package, skipping " + packageName);
            return false;
        }
        if (this.mPm.resolveActivity(new Intent().addCategory("android.intent.category.LAUNCHER").setPackage(packageName), 0) == null) {
            ApplicationsState.AppEntry entry = this.mApplicationsState.getEntry(packageName, this.mUserId);
            if (entry == null || entry.info == null || !AppUtils.isInstant(entry.info)) {
                Log.d("RecentAppsCtrl", "Not a user visible or instant app, skipping " + packageName);
                return false;
            }
            return true;
        }
        return true;
    }
}
