package com.android.settings.users;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.drawable.CircleFramedDrawable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class AppRestrictionsFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, View.OnClickListener {
    private static final String TAG = AppRestrictionsFragment.class.getSimpleName();
    private PreferenceGroup mAppList;
    private boolean mAppListChanged;
    private AsyncTask mAppLoadingTask;
    protected IPackageManager mIPm;
    private boolean mNewUser;
    protected PackageManager mPackageManager;
    protected boolean mRestrictedProfile;
    private PackageInfo mSysPackageInfo;
    protected UserHandle mUser;
    private List<ApplicationInfo> mUserApps;
    protected UserManager mUserManager;
    private List<SelectableAppInfo> mVisibleApps;
    HashMap<String, Boolean> mSelectedPackages = new HashMap<>();
    private boolean mFirstTime = true;
    private int mCustomRequestCode = 1000;
    private HashMap<Integer, AppRestrictionsPreference> mCustomRequestMap = new HashMap<>();
    private BroadcastReceiver mUserBackgrounding = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AppRestrictionsFragment.this.mAppListChanged) {
                AppRestrictionsFragment.this.applyUserAppsStates();
            }
        }
    };
    private BroadcastReceiver mPackageObserver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppRestrictionsFragment.this.onPackageChanged(intent);
        }
    };

    static class SelectableAppInfo {
        CharSequence activityName;
        CharSequence appName;
        Drawable icon;
        SelectableAppInfo masterEntry;
        String packageName;

        SelectableAppInfo() {
        }

        public String toString() {
            return this.packageName + ": appName=" + ((Object) this.appName) + "; activityName=" + ((Object) this.activityName) + "; icon=" + this.icon + "; masterEntry=" + this.masterEntry;
        }
    }

    static class AppRestrictionsPreference extends SwitchPreference {
        private boolean hasSettings;
        private boolean immutable;
        private View.OnClickListener listener;
        private List<Preference> mChildren;
        private boolean panelOpen;
        private ArrayList<RestrictionEntry> restrictions;

        AppRestrictionsPreference(Context context, View.OnClickListener listener) {
            super(context);
            this.mChildren = new ArrayList();
            setLayoutResource(R.layout.preference_app_restrictions);
            this.listener = listener;
        }

        public void setSettingsEnabled(boolean enable) {
            this.hasSettings = enable;
        }

        void setRestrictions(ArrayList<RestrictionEntry> restrictions) {
            this.restrictions = restrictions;
        }

        void setImmutable(boolean immutable) {
            this.immutable = immutable;
        }

        boolean isImmutable() {
            return this.immutable;
        }

        ArrayList<RestrictionEntry> getRestrictions() {
            return this.restrictions;
        }

        boolean isPanelOpen() {
            return this.panelOpen;
        }

        void setPanelOpen(boolean open) {
            this.panelOpen = open;
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            View appRestrictionsSettings = view.findViewById(R.id.app_restrictions_settings);
            appRestrictionsSettings.setVisibility(this.hasSettings ? 0 : 8);
            view.findViewById(R.id.settings_divider).setVisibility(this.hasSettings ? 0 : 8);
            appRestrictionsSettings.setOnClickListener(this.listener);
            appRestrictionsSettings.setTag(this);
            View appRestrictionsPref = view.findViewById(R.id.app_restrictions_pref);
            appRestrictionsPref.setOnClickListener(this.listener);
            appRestrictionsPref.setTag(this);
            ViewGroup widget = (ViewGroup) view.findViewById(android.R.id.widget_frame);
            widget.setEnabled(!isImmutable());
            if (widget.getChildCount() > 0) {
                final Switch toggle = (Switch) widget.getChildAt(0);
                toggle.setEnabled(isImmutable() ? false : true);
                toggle.setTag(this);
                toggle.setClickable(true);
                toggle.setFocusable(true);
                toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AppRestrictionsPreference.this.listener.onClick(toggle);
                    }
                });
            }
        }
    }

    protected void init(Bundle icicle) {
        if (icicle != null) {
            this.mUser = new UserHandle(icicle.getInt("user_id"));
        } else {
            Bundle args = getArguments();
            if (args != null) {
                if (args.containsKey("user_id")) {
                    this.mUser = new UserHandle(args.getInt("user_id"));
                }
                this.mNewUser = args.getBoolean("new_user", false);
            }
        }
        if (this.mUser == null) {
            this.mUser = Process.myUserHandle();
        }
        this.mPackageManager = getActivity().getPackageManager();
        this.mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        this.mUserManager = (UserManager) getActivity().getSystemService("user");
        this.mRestrictedProfile = this.mUserManager.getUserInfo(this.mUser.getIdentifier()).isRestricted();
        try {
            this.mSysPackageInfo = this.mPackageManager.getPackageInfo("android", 64);
        } catch (PackageManager.NameNotFoundException e) {
        }
        addPreferencesFromResource(R.xml.app_restrictions);
        this.mAppList = getAppPreferenceGroup();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("user_id", this.mUser.getIdentifier());
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(this.mUserBackgrounding, new IntentFilter("android.intent.action.USER_BACKGROUND"));
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        getActivity().registerReceiver(this.mPackageObserver, packageFilter);
        this.mAppListChanged = false;
        if (this.mAppLoadingTask == null || this.mAppLoadingTask.getStatus() == AsyncTask.Status.FINISHED) {
            this.mAppLoadingTask = new AppLoadingTask().execute((Void[]) null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mNewUser = false;
        getActivity().unregisterReceiver(this.mUserBackgrounding);
        getActivity().unregisterReceiver(this.mPackageObserver);
        if (this.mAppListChanged) {
            new Thread() {
                @Override
                public void run() {
                    AppRestrictionsFragment.this.applyUserAppsStates();
                }
            }.start();
        }
    }

    public void onPackageChanged(Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getData().getSchemeSpecificPart();
        AppRestrictionsPreference pref = (AppRestrictionsPreference) findPreference(getKeyForPackage(packageName));
        if (pref != null) {
            if (("android.intent.action.PACKAGE_ADDED".equals(action) && pref.isChecked()) || ("android.intent.action.PACKAGE_REMOVED".equals(action) && !pref.isChecked())) {
                pref.setEnabled(true);
            }
        }
    }

    protected PreferenceGroup getAppPreferenceGroup() {
        return getPreferenceScreen();
    }

    Drawable getCircularUserIcon() {
        Bitmap userIcon = this.mUserManager.getUserIcon(this.mUser.getIdentifier());
        if (userIcon == null) {
            return null;
        }
        return CircleFramedDrawable.getInstance(getActivity(), userIcon);
    }

    public void applyUserAppsStates() {
        int userId = this.mUser.getIdentifier();
        if (!this.mUserManager.getUserInfo(userId).isRestricted() && userId != UserHandle.myUserId()) {
            Log.e(TAG, "Cannot apply application restrictions on another user!");
            return;
        }
        for (Map.Entry<String, Boolean> entry : this.mSelectedPackages.entrySet()) {
            String packageName = entry.getKey();
            boolean enabled = entry.getValue().booleanValue();
            applyUserAppState(packageName, enabled);
        }
    }

    private void applyUserAppState(String packageName, boolean enabled) {
        int userId = this.mUser.getIdentifier();
        if (enabled) {
            try {
                ApplicationInfo info = this.mIPm.getApplicationInfo(packageName, 8192, userId);
                if (info == null || !info.enabled || (info.flags & 8388608) == 0) {
                    this.mIPm.installExistingPackageAsUser(packageName, this.mUser.getIdentifier());
                }
                if (info != null && (info.flags & 134217728) != 0 && (info.flags & 8388608) != 0) {
                    disableUiForPackage(packageName);
                    this.mIPm.setApplicationHiddenSettingAsUser(packageName, false, userId);
                    return;
                }
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        try {
            if (this.mIPm.getApplicationInfo(packageName, 0, userId) != null) {
                if (this.mRestrictedProfile) {
                    this.mIPm.deletePackageAsUser(packageName, (IPackageDeleteObserver) null, this.mUser.getIdentifier(), 4);
                } else {
                    disableUiForPackage(packageName);
                    this.mIPm.setApplicationHiddenSettingAsUser(packageName, true, userId);
                }
            }
        } catch (RemoteException e2) {
        }
    }

    private void disableUiForPackage(String packageName) {
        AppRestrictionsPreference pref = (AppRestrictionsPreference) findPreference(getKeyForPackage(packageName));
        if (pref != null) {
            pref.setEnabled(false);
        }
    }

    private boolean isSystemPackage(String packageName) {
        try {
            PackageInfo pi = this.mPackageManager.getPackageInfo(packageName, 0);
            if (pi.applicationInfo == null) {
                return false;
            }
            int flags = pi.applicationInfo.flags;
            return ((flags & 1) == 0 && (flags & 128) == 0) ? false : true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void addSystemImes(Set<String> excludePackages) {
        Context context = getActivity();
        if (context != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService("input_method");
            List<InputMethodInfo> imis = imm.getInputMethodList();
            for (InputMethodInfo imi : imis) {
                try {
                    if (imi.isDefault(context) && isSystemPackage(imi.getPackageName())) {
                        excludePackages.add(imi.getPackageName());
                    }
                } catch (Resources.NotFoundException e) {
                }
            }
        }
    }

    private void addSystemApps(List<SelectableAppInfo> visibleApps, Intent intent, Set<String> excludePackages) {
        int enabled;
        ApplicationInfo targetUserAppInfo;
        if (getActivity() != null) {
            PackageManager pm = this.mPackageManager;
            List<ResolveInfo> launchableApps = pm.queryIntentActivities(intent, 8704);
            for (ResolveInfo app : launchableApps) {
                if (app.activityInfo != null && app.activityInfo.applicationInfo != null) {
                    String packageName = app.activityInfo.packageName;
                    int flags = app.activityInfo.applicationInfo.flags;
                    if ((flags & 1) != 0 || (flags & 128) != 0) {
                        if (!excludePackages.contains(packageName) && (((enabled = pm.getApplicationEnabledSetting(packageName)) != 4 && enabled != 2) || ((targetUserAppInfo = getAppInfoForUser(packageName, 0, this.mUser)) != null && (targetUserAppInfo.flags & 8388608) != 0))) {
                            SelectableAppInfo info = new SelectableAppInfo();
                            info.packageName = app.activityInfo.packageName;
                            info.appName = app.activityInfo.applicationInfo.loadLabel(pm);
                            info.icon = app.activityInfo.loadIcon(pm);
                            info.activityName = app.activityInfo.loadLabel(pm);
                            if (info.activityName == null) {
                                info.activityName = info.appName;
                            }
                            visibleApps.add(info);
                        }
                    }
                }
            }
        }
    }

    private ApplicationInfo getAppInfoForUser(String packageName, int flags, UserHandle user) {
        try {
            return this.mIPm.getApplicationInfo(packageName, flags, user.getIdentifier());
        } catch (RemoteException e) {
            return null;
        }
    }

    private class AppLoadingTask extends AsyncTask<Void, Void, Void> {
        private AppLoadingTask() {
        }

        @Override
        public Void doInBackground(Void... params) {
            AppRestrictionsFragment.this.fetchAndMergeApps();
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            AppRestrictionsFragment.this.populateApps();
        }

        @Override
        protected void onPreExecute() {
        }
    }

    public void fetchAndMergeApps() {
        this.mAppList.setOrderingAsAdded(false);
        this.mVisibleApps = new ArrayList();
        Context context = getActivity();
        if (context != null) {
            PackageManager pm = this.mPackageManager;
            IPackageManager ipm = this.mIPm;
            HashSet<String> excludePackages = new HashSet<>();
            addSystemImes(excludePackages);
            Intent launcherIntent = new Intent("android.intent.action.MAIN");
            launcherIntent.addCategory("android.intent.category.LAUNCHER");
            addSystemApps(this.mVisibleApps, launcherIntent, excludePackages);
            Intent widgetIntent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
            addSystemApps(this.mVisibleApps, widgetIntent, excludePackages);
            List<ApplicationInfo> installedApps = pm.getInstalledApplications(8192);
            for (ApplicationInfo app : installedApps) {
                if ((app.flags & 8388608) != 0) {
                    if ((app.flags & 1) == 0 && (app.flags & 128) == 0) {
                        SelectableAppInfo info = new SelectableAppInfo();
                        info.packageName = app.packageName;
                        info.appName = app.loadLabel(pm);
                        info.activityName = info.appName;
                        info.icon = app.loadIcon(pm);
                        this.mVisibleApps.add(info);
                    } else {
                        try {
                            PackageInfo pi = pm.getPackageInfo(app.packageName, 0);
                            if (this.mRestrictedProfile && pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                                this.mSelectedPackages.put(app.packageName, false);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                        }
                    }
                }
            }
            this.mUserApps = null;
            try {
                this.mUserApps = ipm.getInstalledApplications(8192, this.mUser.getIdentifier()).getList();
            } catch (RemoteException e2) {
            }
            if (this.mUserApps != null) {
                for (ApplicationInfo app2 : this.mUserApps) {
                    if ((app2.flags & 8388608) != 0 && (app2.flags & 1) == 0 && (app2.flags & 128) == 0) {
                        SelectableAppInfo info2 = new SelectableAppInfo();
                        info2.packageName = app2.packageName;
                        info2.appName = app2.loadLabel(pm);
                        info2.activityName = info2.appName;
                        info2.icon = app2.loadIcon(pm);
                        this.mVisibleApps.add(info2);
                    }
                }
            }
            Collections.sort(this.mVisibleApps, new AppLabelComparator());
            Set<String> dedupPackageSet = new HashSet<>();
            for (int i = this.mVisibleApps.size() - 1; i >= 0; i--) {
                SelectableAppInfo info3 = this.mVisibleApps.get(i);
                String both = info3.packageName + "+" + ((Object) info3.activityName);
                if (!TextUtils.isEmpty(info3.packageName) && !TextUtils.isEmpty(info3.activityName) && dedupPackageSet.contains(both)) {
                    this.mVisibleApps.remove(i);
                } else {
                    dedupPackageSet.add(both);
                }
            }
            HashMap<String, SelectableAppInfo> packageMap = new HashMap<>();
            for (SelectableAppInfo info4 : this.mVisibleApps) {
                if (packageMap.containsKey(info4.packageName)) {
                    info4.masterEntry = packageMap.get(info4.packageName);
                } else {
                    packageMap.put(info4.packageName, info4);
                }
            }
        }
    }

    private boolean isPlatformSigned(PackageInfo pi) {
        return (pi == null || pi.signatures == null || !this.mSysPackageInfo.signatures[0].equals(pi.signatures[0])) ? false : true;
    }

    private boolean isAppEnabledForUser(PackageInfo pi) {
        if (pi == null) {
            return false;
        }
        int flags = pi.applicationInfo.flags;
        return (8388608 & flags) != 0 && (134217728 & flags) == 0;
    }

    public void populateApps() {
        Context context = getActivity();
        if (context != null) {
            PackageManager pm = this.mPackageManager;
            IPackageManager ipm = this.mIPm;
            int userId = this.mUser.getIdentifier();
            if (Utils.getExistingUser(this.mUserManager, this.mUser) != null) {
                this.mAppList.removeAll();
                Intent restrictionsIntent = new Intent("android.intent.action.GET_RESTRICTION_ENTRIES");
                List<ResolveInfo> receivers = pm.queryBroadcastReceivers(restrictionsIntent, 0);
                int i = 0;
                for (SelectableAppInfo app : this.mVisibleApps) {
                    String packageName = app.packageName;
                    if (packageName != null) {
                        boolean isSettingsApp = packageName.equals(context.getPackageName());
                        AppRestrictionsPreference p = new AppRestrictionsPreference(context, this);
                        boolean hasSettings = resolveInfoListHasPackage(receivers, packageName);
                        p.setIcon(app.icon != null ? app.icon.mutate() : null);
                        p.setChecked(false);
                        p.setTitle(app.activityName);
                        if (app.masterEntry != null) {
                            p.setSummary(context.getString(R.string.user_restrictions_controlled_by, app.masterEntry.activityName));
                        }
                        p.setKey(getKeyForPackage(packageName));
                        p.setSettingsEnabled((hasSettings || isSettingsApp) && app.masterEntry == null);
                        p.setPersistent(false);
                        p.setOnPreferenceChangeListener(this);
                        p.setOnPreferenceClickListener(this);
                        PackageInfo pi = null;
                        try {
                            pi = ipm.getPackageInfo(packageName, 8256, userId);
                        } catch (RemoteException e) {
                        }
                        if (pi != null) {
                            if (pi.requiredForAllUsers || isPlatformSigned(pi)) {
                                p.setChecked(true);
                                p.setImmutable(true);
                                if (hasSettings || isSettingsApp) {
                                    if (hasSettings && app.masterEntry == null) {
                                        requestRestrictionsForApp(packageName, p, false);
                                    }
                                }
                            } else if (!this.mNewUser && isAppEnabledForUser(pi)) {
                                p.setChecked(true);
                            }
                            if (this.mRestrictedProfile && pi.requiredAccountType != null && pi.restrictedAccountType == null) {
                                p.setChecked(false);
                                p.setImmutable(true);
                                p.setSummary(R.string.app_not_supported_in_limited);
                            }
                            if (this.mRestrictedProfile && pi.restrictedAccountType != null) {
                                p.setSummary(R.string.app_sees_restricted_accounts);
                            }
                            if (app.masterEntry != null) {
                                p.setImmutable(true);
                                p.setChecked(this.mSelectedPackages.get(packageName).booleanValue());
                            }
                            this.mAppList.addPreference(p);
                            if (isSettingsApp) {
                                p.setOrder(100);
                            } else {
                                p.setOrder((i + 2) * 100);
                            }
                            this.mSelectedPackages.put(packageName, Boolean.valueOf(p.isChecked()));
                            this.mAppListChanged = true;
                            i++;
                        }
                    }
                }
                if (this.mNewUser && this.mFirstTime) {
                    this.mFirstTime = false;
                    applyUserAppsStates();
                }
            }
        }
    }

    private String getKeyForPackage(String packageName) {
        return "pkg_" + packageName;
    }

    private class AppLabelComparator implements Comparator<SelectableAppInfo> {
        private AppLabelComparator() {
        }

        @Override
        public int compare(SelectableAppInfo lhs, SelectableAppInfo rhs) {
            String lhsLabel = lhs.activityName.toString();
            String rhsLabel = rhs.activityName.toString();
            return lhsLabel.toLowerCase().compareTo(rhsLabel.toLowerCase());
        }
    }

    private boolean resolveInfoListHasPackage(List<ResolveInfo> receivers, String packageName) {
        for (ResolveInfo info : receivers) {
            if (info.activityInfo.packageName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void updateAllEntries(String prefKey, boolean checked) {
        for (int i = 0; i < this.mAppList.getPreferenceCount(); i++) {
            Preference pref = this.mAppList.getPreference(i);
            if ((pref instanceof AppRestrictionsPreference) && prefKey.equals(pref.getKey())) {
                ((AppRestrictionsPreference) pref).setChecked(checked);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getTag() instanceof AppRestrictionsPreference) {
            AppRestrictionsPreference pref = (AppRestrictionsPreference) v.getTag();
            if (v.getId() == R.id.app_restrictions_settings) {
                onAppSettingsIconClicked(pref);
                return;
            }
            if (!pref.isImmutable()) {
                pref.setChecked(!pref.isChecked());
                String packageName = pref.getKey().substring("pkg_".length());
                this.mSelectedPackages.put(packageName, Boolean.valueOf(pref.isChecked()));
                if (pref.isChecked() && pref.hasSettings && pref.restrictions == null) {
                    requestRestrictionsForApp(packageName, pref, false);
                }
                this.mAppListChanged = true;
                if (!this.mRestrictedProfile) {
                    applyUserAppState(packageName, pref.isChecked());
                }
                updateAllEntries(pref.getKey(), pref.isChecked());
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key != null && key.contains(";")) {
            StringTokenizer st = new StringTokenizer(key, ";");
            String packageName = st.nextToken();
            String restrictionKey = st.nextToken();
            AppRestrictionsPreference appPref = (AppRestrictionsPreference) this.mAppList.findPreference("pkg_" + packageName);
            ArrayList<RestrictionEntry> restrictions = appPref.getRestrictions();
            if (restrictions != null) {
                for (RestrictionEntry entry : restrictions) {
                    if (entry.getKey().equals(restrictionKey)) {
                        switch (entry.getType()) {
                            case 1:
                                entry.setSelectedState(((Boolean) newValue).booleanValue());
                                if (packageName.equals(getActivity().getPackageName())) {
                                    RestrictionUtils.setRestrictions(getActivity(), restrictions, this.mUser);
                                    return true;
                                }
                                this.mUserManager.setApplicationRestrictions(packageName, RestrictionUtils.restrictionsToBundle(restrictions), this.mUser);
                                return true;
                            case 2:
                            case 3:
                                ListPreference listPref = (ListPreference) preference;
                                entry.setSelectedString((String) newValue);
                                String readable = findInArray(entry.getChoiceEntries(), entry.getChoiceValues(), (String) newValue);
                                listPref.setSummary(readable);
                                if (packageName.equals(getActivity().getPackageName())) {
                                }
                                break;
                            case 4:
                                Set<String> set = (Set) newValue;
                                String[] selectedValues = new String[set.size()];
                                set.toArray(selectedValues);
                                entry.setAllSelectedStrings(selectedValues);
                                if (packageName.equals(getActivity().getPackageName())) {
                                }
                                break;
                        }
                    }
                }
                return true;
            }
            return true;
        }
        return true;
    }

    private void removeRestrictionsForApp(AppRestrictionsPreference preference) {
        for (Preference p : preference.mChildren) {
            this.mAppList.removePreference(p);
        }
        preference.mChildren.clear();
    }

    private void onAppSettingsIconClicked(AppRestrictionsPreference preference) {
        if (preference.getKey().startsWith("pkg_")) {
            if (preference.isPanelOpen()) {
                removeRestrictionsForApp(preference);
            } else {
                String packageName = preference.getKey().substring("pkg_".length());
                if (packageName.equals(getActivity().getPackageName())) {
                    ArrayList<RestrictionEntry> restrictions = RestrictionUtils.getRestrictions(getActivity(), this.mUser);
                    onRestrictionsReceived(preference, packageName, restrictions);
                } else {
                    requestRestrictionsForApp(packageName, preference, true);
                }
            }
            preference.setPanelOpen(preference.isPanelOpen() ? false : true);
        }
    }

    private void requestRestrictionsForApp(String packageName, AppRestrictionsPreference preference, boolean invokeIfCustom) {
        Bundle oldEntries = this.mUserManager.getApplicationRestrictions(packageName, this.mUser);
        Intent intent = new Intent("android.intent.action.GET_RESTRICTION_ENTRIES");
        intent.setPackage(packageName);
        intent.putExtra("android.intent.extra.restrictions_bundle", oldEntries);
        intent.addFlags(32);
        getActivity().sendOrderedBroadcast(intent, null, new RestrictionsResultReceiver(packageName, preference, invokeIfCustom), null, -1, null, null);
    }

    class RestrictionsResultReceiver extends BroadcastReceiver {
        boolean invokeIfCustom;
        String packageName;
        AppRestrictionsPreference preference;

        RestrictionsResultReceiver(String packageName, AppRestrictionsPreference preference, boolean invokeIfCustom) {
            this.packageName = packageName;
            this.preference = preference;
            this.invokeIfCustom = invokeIfCustom;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle results = getResultExtras(true);
            ArrayList<RestrictionEntry> restrictions = results.getParcelableArrayList("android.intent.extra.restrictions_list");
            Intent restrictionsIntent = (Intent) results.getParcelable("android.intent.extra.restrictions_intent");
            if (restrictions != null && restrictionsIntent == null) {
                AppRestrictionsFragment.this.onRestrictionsReceived(this.preference, this.packageName, restrictions);
                if (AppRestrictionsFragment.this.mRestrictedProfile) {
                    AppRestrictionsFragment.this.mUserManager.setApplicationRestrictions(this.packageName, RestrictionUtils.restrictionsToBundle(restrictions), AppRestrictionsFragment.this.mUser);
                    return;
                }
                return;
            }
            if (restrictionsIntent != null) {
                this.preference.setRestrictions(restrictions);
                if (this.invokeIfCustom && AppRestrictionsFragment.this.isResumed()) {
                    assertSafeToStartCustomActivity(restrictionsIntent);
                    int requestCode = AppRestrictionsFragment.this.generateCustomActivityRequestCode(this.preference);
                    AppRestrictionsFragment.this.startActivityForResult(restrictionsIntent, requestCode);
                }
            }
        }

        private void assertSafeToStartCustomActivity(Intent intent) {
            if (intent.getPackage() == null || !intent.getPackage().equals(this.packageName)) {
                List<ResolveInfo> resolveInfos = AppRestrictionsFragment.this.mPackageManager.queryIntentActivities(intent, 0);
                if (resolveInfos.size() == 1) {
                    ActivityInfo activityInfo = resolveInfos.get(0).activityInfo;
                    if (!this.packageName.equals(activityInfo.packageName)) {
                        throw new SecurityException("Application " + this.packageName + " is not allowed to start activity " + intent);
                    }
                }
            }
        }
    }

    public void onRestrictionsReceived(AppRestrictionsPreference preference, String packageName, ArrayList<RestrictionEntry> restrictions) {
        removeRestrictionsForApp(preference);
        Context context = preference.getContext();
        int count = 1;
        for (RestrictionEntry entry : restrictions) {
            Preference p = null;
            switch (entry.getType()) {
                case 1:
                    p = new SwitchPreference(context);
                    p.setTitle(entry.getTitle());
                    p.setSummary(entry.getDescription());
                    ((SwitchPreference) p).setChecked(entry.getSelectedState());
                    break;
                case 2:
                case 3:
                    p = new ListPreference(context);
                    p.setTitle(entry.getTitle());
                    String value = entry.getSelectedString();
                    if (value == null) {
                        value = entry.getDescription();
                    }
                    p.setSummary(findInArray(entry.getChoiceEntries(), entry.getChoiceValues(), value));
                    ((ListPreference) p).setEntryValues(entry.getChoiceValues());
                    ((ListPreference) p).setEntries(entry.getChoiceEntries());
                    ((ListPreference) p).setValue(value);
                    ((ListPreference) p).setDialogTitle(entry.getTitle());
                    break;
                case 4:
                    p = new MultiSelectListPreference(context);
                    p.setTitle(entry.getTitle());
                    ((MultiSelectListPreference) p).setEntryValues(entry.getChoiceValues());
                    ((MultiSelectListPreference) p).setEntries(entry.getChoiceEntries());
                    HashSet<String> set = new HashSet<>();
                    String[] arr$ = entry.getAllSelectedStrings();
                    for (String s : arr$) {
                        set.add(s);
                    }
                    ((MultiSelectListPreference) p).setValues(set);
                    ((MultiSelectListPreference) p).setDialogTitle(entry.getTitle());
                    break;
            }
            if (p != null) {
                p.setPersistent(false);
                p.setOrder(preference.getOrder() + count);
                p.setKey(preference.getKey().substring("pkg_".length()) + ";" + entry.getKey());
                this.mAppList.addPreference(p);
                p.setOnPreferenceChangeListener(this);
                p.setIcon(R.drawable.empty_icon);
                preference.mChildren.add(p);
                count++;
            }
        }
        preference.setRestrictions(restrictions);
        if (count == 1 && preference.isImmutable() && preference.isChecked()) {
            this.mAppList.removePreference(preference);
        }
    }

    public int generateCustomActivityRequestCode(AppRestrictionsPreference preference) {
        this.mCustomRequestCode++;
        this.mCustomRequestMap.put(Integer.valueOf(this.mCustomRequestCode), preference);
        return this.mCustomRequestCode;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AppRestrictionsPreference pref = this.mCustomRequestMap.get(Integer.valueOf(requestCode));
        if (pref == null) {
            Log.w(TAG, "Unknown requestCode " + requestCode);
            return;
        }
        if (resultCode == -1) {
            String packageName = pref.getKey().substring("pkg_".length());
            ArrayList<RestrictionEntry> list = data.getParcelableArrayListExtra("android.intent.extra.restrictions_list");
            Bundle bundle = data.getBundleExtra("android.intent.extra.restrictions_bundle");
            if (list != null) {
                pref.setRestrictions(list);
                this.mUserManager.setApplicationRestrictions(packageName, RestrictionUtils.restrictionsToBundle(list), this.mUser);
            } else if (bundle != null) {
                this.mUserManager.setApplicationRestrictions(packageName, bundle, this.mUser);
            }
        }
        this.mCustomRequestMap.remove(Integer.valueOf(requestCode));
    }

    private String findInArray(String[] choiceEntries, String[] choiceValues, String selectedString) {
        for (int i = 0; i < choiceValues.length; i++) {
            if (choiceValues[i].equals(selectedString)) {
                return choiceEntries[i];
            }
        }
        return selectedString;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (!preference.getKey().startsWith("pkg_")) {
            return false;
        }
        AppRestrictionsPreference arp = (AppRestrictionsPreference) preference;
        if (arp.isImmutable()) {
            return true;
        }
        String packageName = arp.getKey().substring("pkg_".length());
        boolean newEnabledState = arp.isChecked() ? false : true;
        arp.setChecked(newEnabledState);
        this.mSelectedPackages.put(packageName, Boolean.valueOf(newEnabledState));
        updateAllEntries(arp.getKey(), newEnabledState);
        this.mAppListChanged = true;
        applyUserAppState(packageName, newEnabledState);
        return true;
    }
}
