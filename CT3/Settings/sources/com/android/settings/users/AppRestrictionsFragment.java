package com.android.settings.users;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.users.AppRestrictionsHelper;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class AppRestrictionsFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, View.OnClickListener, Preference.OnPreferenceClickListener, AppRestrictionsHelper.OnDisableUiForPackageListener {
    private static final String TAG = AppRestrictionsFragment.class.getSimpleName();
    private PreferenceGroup mAppList;
    private boolean mAppListChanged;
    private AsyncTask mAppLoadingTask;
    private AppRestrictionsHelper mHelper;
    protected IPackageManager mIPm;
    private boolean mNewUser;
    protected PackageManager mPackageManager;
    protected boolean mRestrictedProfile;
    private PackageInfo mSysPackageInfo;
    protected UserHandle mUser;
    protected UserManager mUserManager;
    private boolean mFirstTime = true;
    private int mCustomRequestCode = 1000;
    private HashMap<Integer, AppRestrictionsPreference> mCustomRequestMap = new HashMap<>();
    private BroadcastReceiver mUserBackgrounding = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!AppRestrictionsFragment.this.mAppListChanged) {
                return;
            }
            AppRestrictionsFragment.this.mHelper.applyUserAppsStates(AppRestrictionsFragment.this);
        }
    };
    private BroadcastReceiver mPackageObserver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppRestrictionsFragment.this.onPackageChanged(intent);
        }
    };

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
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
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
            if (widget.getChildCount() <= 0) {
                return;
            }
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
        this.mHelper = new AppRestrictionsHelper(getContext(), this.mUser);
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
        this.mAppList.setOrderingAsAdded(false);
    }

    @Override
    protected int getMetricsCategory() {
        return 97;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("user_id", this.mUser.getIdentifier());
    }

    @Override
    public void onResume() {
        AppLoadingTask appLoadingTask = null;
        super.onResume();
        getActivity().registerReceiver(this.mUserBackgrounding, new IntentFilter("android.intent.action.USER_BACKGROUND"));
        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction("android.intent.action.PACKAGE_ADDED");
        packageFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        packageFilter.addDataScheme("package");
        getActivity().registerReceiver(this.mPackageObserver, packageFilter);
        this.mAppListChanged = false;
        if (this.mAppLoadingTask != null && this.mAppLoadingTask.getStatus() != AsyncTask.Status.FINISHED) {
            return;
        }
        this.mAppLoadingTask = new AppLoadingTask(this, appLoadingTask).execute(new Void[0]);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mNewUser = false;
        getActivity().unregisterReceiver(this.mUserBackgrounding);
        getActivity().unregisterReceiver(this.mPackageObserver);
        if (!this.mAppListChanged) {
            return;
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                AppRestrictionsFragment.this.mHelper.applyUserAppsStates(AppRestrictionsFragment.this);
                return null;
            }
        }.execute(new Void[0]);
    }

    public void onPackageChanged(Intent intent) {
        String action = intent.getAction();
        String packageName = intent.getData().getSchemeSpecificPart();
        AppRestrictionsPreference pref = (AppRestrictionsPreference) findPreference(getKeyForPackage(packageName));
        if (pref == null) {
            return;
        }
        if ((!"android.intent.action.PACKAGE_ADDED".equals(action) || !pref.isChecked()) && (!"android.intent.action.PACKAGE_REMOVED".equals(action) || pref.isChecked())) {
            return;
        }
        pref.setEnabled(true);
    }

    protected PreferenceGroup getAppPreferenceGroup() {
        return getPreferenceScreen();
    }

    @Override
    public void onDisableUiForPackage(String packageName) {
        AppRestrictionsPreference pref = (AppRestrictionsPreference) findPreference(getKeyForPackage(packageName));
        if (pref == null) {
            return;
        }
        pref.setEnabled(false);
    }

    private class AppLoadingTask extends AsyncTask<Void, Void, Void> {
        AppLoadingTask(AppRestrictionsFragment this$0, AppLoadingTask appLoadingTask) {
            this();
        }

        private AppLoadingTask() {
        }

        @Override
        public Void doInBackground(Void... params) {
            AppRestrictionsFragment.this.mHelper.fetchAndMergeApps();
            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            AppRestrictionsFragment.this.populateApps();
        }
    }

    private boolean isPlatformSigned(PackageInfo pi) {
        if (pi == null || pi.signatures == null) {
            return false;
        }
        return this.mSysPackageInfo.signatures[0].equals(pi.signatures[0]);
    }

    private boolean isAppEnabledForUser(PackageInfo pi) {
        if (pi == null) {
            return false;
        }
        int flags = pi.applicationInfo.flags;
        int privateFlags = pi.applicationInfo.privateFlags;
        return (8388608 & flags) != 0 && (privateFlags & 1) == 0;
    }

    public void populateApps() {
        Context context = getActivity();
        if (context == null) {
            return;
        }
        PackageManager pm = this.mPackageManager;
        IPackageManager ipm = this.mIPm;
        int userId = this.mUser.getIdentifier();
        if (Utils.getExistingUser(this.mUserManager, this.mUser) == null) {
            return;
        }
        this.mAppList.removeAll();
        Intent restrictionsIntent = new Intent("android.intent.action.GET_RESTRICTION_ENTRIES");
        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(restrictionsIntent, 0);
        for (AppRestrictionsHelper.SelectableAppInfo app : this.mHelper.getVisibleApps()) {
            String packageName = app.packageName;
            if (packageName != null) {
                boolean isSettingsApp = packageName.equals(context.getPackageName());
                AppRestrictionsPreference p = new AppRestrictionsPreference(getPrefContext(), this);
                boolean hasSettings = resolveInfoListHasPackage(receivers, packageName);
                if (isSettingsApp) {
                    addLocationAppRestrictionsPreference(app, p);
                    this.mHelper.setPackageSelected(packageName, true);
                } else {
                    PackageInfo pi = null;
                    try {
                        pi = ipm.getPackageInfo(packageName, 8256, userId);
                    } catch (RemoteException e) {
                    }
                    if (pi != null && (!this.mRestrictedProfile || !isAppUnsupportedInRestrictedProfile(pi))) {
                        p.setIcon(app.icon != null ? app.icon.mutate() : null);
                        p.setChecked(false);
                        p.setTitle(app.activityName);
                        p.setKey(getKeyForPackage(packageName));
                        p.setSettingsEnabled(hasSettings && app.masterEntry == null);
                        p.setPersistent(false);
                        p.setOnPreferenceChangeListener(this);
                        p.setOnPreferenceClickListener(this);
                        p.setSummary(getPackageSummary(pi, app));
                        if (pi.requiredForAllUsers || isPlatformSigned(pi)) {
                            p.setChecked(true);
                            p.setImmutable(true);
                            if (hasSettings) {
                                if (app.masterEntry == null) {
                                    requestRestrictionsForApp(packageName, p, false);
                                }
                            }
                        } else if (!this.mNewUser && isAppEnabledForUser(pi)) {
                            p.setChecked(true);
                        }
                        if (app.masterEntry != null) {
                            p.setImmutable(true);
                            p.setChecked(this.mHelper.isPackageSelected(packageName));
                        }
                        p.setOrder((this.mAppList.getPreferenceCount() + 2) * 100);
                        this.mHelper.setPackageSelected(packageName, p.isChecked());
                        this.mAppList.addPreference(p);
                    }
                }
            }
        }
        this.mAppListChanged = true;
        if (!this.mNewUser || !this.mFirstTime) {
            return;
        }
        this.mFirstTime = false;
        this.mHelper.applyUserAppsStates(this);
    }

    private String getPackageSummary(PackageInfo pi, AppRestrictionsHelper.SelectableAppInfo app) {
        if (app.masterEntry != null) {
            if (this.mRestrictedProfile && pi.restrictedAccountType != null) {
                return getString(R.string.app_sees_restricted_accounts_and_controlled_by, new Object[]{app.masterEntry.activityName});
            }
            return getString(R.string.user_restrictions_controlled_by, new Object[]{app.masterEntry.activityName});
        }
        if (pi.restrictedAccountType != null) {
            return getString(R.string.app_sees_restricted_accounts);
        }
        return null;
    }

    private static boolean isAppUnsupportedInRestrictedProfile(PackageInfo pi) {
        return pi.requiredAccountType != null && pi.restrictedAccountType == null;
    }

    private void addLocationAppRestrictionsPreference(AppRestrictionsHelper.SelectableAppInfo app, AppRestrictionsPreference p) {
        String packageName = app.packageName;
        p.setIcon(R.drawable.ic_settings_location);
        p.setKey(getKeyForPackage(packageName));
        ArrayList<RestrictionEntry> restrictions = RestrictionUtils.getRestrictions(getActivity(), this.mUser);
        RestrictionEntry locationRestriction = restrictions.get(0);
        p.setTitle(locationRestriction.getTitle());
        p.setRestrictions(restrictions);
        p.setSummary(locationRestriction.getDescription());
        p.setChecked(locationRestriction.getSelectedState());
        p.setPersistent(false);
        p.setOnPreferenceClickListener(this);
        p.setOrder(100);
        this.mAppList.addPreference(p);
    }

    private String getKeyForPackage(String packageName) {
        return "pkg_" + packageName;
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
        if (!(v.getTag() instanceof AppRestrictionsPreference)) {
            return;
        }
        AppRestrictionsPreference pref = (AppRestrictionsPreference) v.getTag();
        if (v.getId() == R.id.app_restrictions_settings) {
            onAppSettingsIconClicked(pref);
            return;
        }
        if (pref.isImmutable()) {
            return;
        }
        pref.setChecked(!pref.isChecked());
        String packageName = pref.getKey().substring("pkg_".length());
        if (packageName.equals(getActivity().getPackageName())) {
            ((RestrictionEntry) pref.restrictions.get(0)).setSelectedState(pref.isChecked());
            RestrictionUtils.setRestrictions(getActivity(), pref.restrictions, this.mUser);
            return;
        }
        this.mHelper.setPackageSelected(packageName, pref.isChecked());
        if (pref.isChecked() && pref.hasSettings && pref.restrictions == null) {
            requestRestrictionsForApp(packageName, pref, false);
        }
        this.mAppListChanged = true;
        if (!this.mRestrictedProfile) {
            this.mHelper.applyUserAppState(packageName, pref.isChecked(), this);
        }
        updateAllEntries(pref.getKey(), pref.isChecked());
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
                            case DefaultWfcSettingsExt.PAUSE:
                                entry.setSelectedState(((Boolean) newValue).booleanValue());
                                this.mUserManager.setApplicationRestrictions(packageName, RestrictionsManager.convertRestrictionsToBundle(restrictions), this.mUser);
                                return true;
                            case DefaultWfcSettingsExt.CREATE:
                            case DefaultWfcSettingsExt.DESTROY:
                                ListPreference listPref = (ListPreference) preference;
                                entry.setSelectedString((String) newValue);
                                String readable = findInArray(entry.getChoiceEntries(), entry.getChoiceValues(), (String) newValue);
                                listPref.setSummary(readable);
                                this.mUserManager.setApplicationRestrictions(packageName, RestrictionsManager.convertRestrictionsToBundle(restrictions), this.mUser);
                                return true;
                            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                                Set<String> set = (Set) newValue;
                                String[] selectedValues = new String[set.size()];
                                set.toArray(selectedValues);
                                entry.setAllSelectedStrings(selectedValues);
                                this.mUserManager.setApplicationRestrictions(packageName, RestrictionsManager.convertRestrictionsToBundle(restrictions), this.mUser);
                                return true;
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
        if (!preference.getKey().startsWith("pkg_")) {
            return;
        }
        if (preference.isPanelOpen()) {
            removeRestrictionsForApp(preference);
        } else {
            String packageName = preference.getKey().substring("pkg_".length());
            requestRestrictionsForApp(packageName, preference, true);
        }
        preference.setPanelOpen(preference.isPanelOpen() ? false : true);
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
                AppRestrictionsFragment.this.onRestrictionsReceived(this.preference, restrictions);
                if (!AppRestrictionsFragment.this.mRestrictedProfile) {
                    return;
                }
                AppRestrictionsFragment.this.mUserManager.setApplicationRestrictions(this.packageName, RestrictionsManager.convertRestrictionsToBundle(restrictions), AppRestrictionsFragment.this.mUser);
                return;
            }
            if (restrictionsIntent == null) {
                return;
            }
            this.preference.setRestrictions(restrictions);
            if (!this.invokeIfCustom || !AppRestrictionsFragment.this.isResumed()) {
                return;
            }
            assertSafeToStartCustomActivity(restrictionsIntent);
            int requestCode = AppRestrictionsFragment.this.generateCustomActivityRequestCode(this.preference);
            AppRestrictionsFragment.this.startActivityForResult(restrictionsIntent, requestCode);
        }

        private void assertSafeToStartCustomActivity(Intent intent) {
            if (intent.getPackage() != null && intent.getPackage().equals(this.packageName)) {
                return;
            }
            List<ResolveInfo> resolveInfos = AppRestrictionsFragment.this.mPackageManager.queryIntentActivities(intent, 0);
            if (resolveInfos.size() != 1) {
                return;
            }
            ActivityInfo activityInfo = resolveInfos.get(0).activityInfo;
            if (this.packageName.equals(activityInfo.packageName)) {
            } else {
                throw new SecurityException("Application " + this.packageName + " is not allowed to start activity " + intent);
            }
        }
    }

    public void onRestrictionsReceived(AppRestrictionsPreference preference, ArrayList<RestrictionEntry> restrictions) {
        removeRestrictionsForApp(preference);
        int count = 1;
        for (RestrictionEntry entry : restrictions) {
            Preference p = null;
            switch (entry.getType()) {
                case DefaultWfcSettingsExt.PAUSE:
                    p = new SwitchPreference(getPrefContext());
                    p.setTitle(entry.getTitle());
                    p.setSummary(entry.getDescription());
                    ((SwitchPreference) p).setChecked(entry.getSelectedState());
                    break;
                case DefaultWfcSettingsExt.CREATE:
                case DefaultWfcSettingsExt.DESTROY:
                    p = new ListPreference(getPrefContext());
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
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    p = new MultiSelectListPreference(getPrefContext());
                    p.setTitle(entry.getTitle());
                    ((MultiSelectListPreference) p).setEntryValues(entry.getChoiceValues());
                    ((MultiSelectListPreference) p).setEntries(entry.getChoiceEntries());
                    HashSet<String> set = new HashSet<>();
                    Collections.addAll(set, entry.getAllSelectedStrings());
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
        if (count != 1 || !preference.isImmutable() || !preference.isChecked()) {
            return;
        }
        this.mAppList.removePreference(preference);
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
                this.mUserManager.setApplicationRestrictions(packageName, RestrictionsManager.convertRestrictionsToBundle(list), this.mUser);
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
        if (!arp.isImmutable()) {
            String packageName = arp.getKey().substring("pkg_".length());
            boolean newEnabledState = arp.isChecked() ? false : true;
            arp.setChecked(newEnabledState);
            this.mHelper.setPackageSelected(packageName, newEnabledState);
            updateAllEntries(arp.getKey(), newEnabledState);
            this.mAppListChanged = true;
            this.mHelper.applyUserAppState(packageName, newEnabledState, this);
        }
        return true;
    }
}
