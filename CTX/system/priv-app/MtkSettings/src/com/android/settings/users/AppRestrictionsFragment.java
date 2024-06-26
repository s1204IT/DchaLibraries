package com.android.settings.users;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
/* loaded from: classes.dex */
public class AppRestrictionsFragment extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, View.OnClickListener, AppRestrictionsHelper.OnDisableUiForPackageListener {
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
    private BroadcastReceiver mUserBackgrounding = new BroadcastReceiver() { // from class: com.android.settings.users.AppRestrictionsFragment.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (AppRestrictionsFragment.this.mAppListChanged) {
                AppRestrictionsFragment.this.mHelper.applyUserAppsStates(AppRestrictionsFragment.this);
            }
        }
    };
    private BroadcastReceiver mPackageObserver = new BroadcastReceiver() { // from class: com.android.settings.users.AppRestrictionsFragment.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            AppRestrictionsFragment.this.onPackageChanged(intent);
        }
    };

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class AppRestrictionsPreference extends SwitchPreference {
        private boolean hasSettings;
        private boolean immutable;
        private View.OnClickListener listener;
        private List<Preference> mChildren;
        private boolean panelOpen;
        private ArrayList<RestrictionEntry> restrictions;

        AppRestrictionsPreference(Context context, View.OnClickListener onClickListener) {
            super(context);
            this.mChildren = new ArrayList();
            setLayoutResource(R.layout.preference_app_restrictions);
            this.listener = onClickListener;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void setSettingsEnabled(boolean z) {
            this.hasSettings = z;
        }

        void setRestrictions(ArrayList<RestrictionEntry> arrayList) {
            this.restrictions = arrayList;
        }

        void setImmutable(boolean z) {
            this.immutable = z;
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

        void setPanelOpen(boolean z) {
            this.panelOpen = z;
        }

        @Override // android.support.v14.preference.SwitchPreference, android.support.v7.preference.Preference
        public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
            super.onBindViewHolder(preferenceViewHolder);
            View findViewById = preferenceViewHolder.findViewById(R.id.app_restrictions_settings);
            findViewById.setVisibility(this.hasSettings ? 0 : 8);
            preferenceViewHolder.findViewById(R.id.settings_divider).setVisibility(this.hasSettings ? 0 : 8);
            findViewById.setOnClickListener(this.listener);
            findViewById.setTag(this);
            View findViewById2 = preferenceViewHolder.findViewById(R.id.app_restrictions_pref);
            findViewById2.setOnClickListener(this.listener);
            findViewById2.setTag(this);
            ViewGroup viewGroup = (ViewGroup) preferenceViewHolder.findViewById(16908312);
            viewGroup.setEnabled(!isImmutable());
            if (viewGroup.getChildCount() > 0) {
                final Switch r6 = (Switch) viewGroup.getChildAt(0);
                r6.setEnabled(!isImmutable());
                r6.setTag(this);
                r6.setClickable(true);
                r6.setFocusable(true);
                r6.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.android.settings.users.AppRestrictionsFragment.AppRestrictionsPreference.1
                    @Override // android.widget.CompoundButton.OnCheckedChangeListener
                    public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                        AppRestrictionsPreference.this.listener.onClick(r6);
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void init(Bundle bundle) {
        if (bundle != null) {
            this.mUser = new UserHandle(bundle.getInt("user_id"));
        } else {
            Bundle arguments = getArguments();
            if (arguments != null) {
                if (arguments.containsKey("user_id")) {
                    this.mUser = new UserHandle(arguments.getInt("user_id"));
                }
                this.mNewUser = arguments.getBoolean("new_user", false);
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

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 97;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("user_id", this.mUser.getIdentifier());
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(this.mUserBackgrounding, new IntentFilter("android.intent.action.USER_BACKGROUND"));
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addDataScheme("package");
        getActivity().registerReceiver(this.mPackageObserver, intentFilter);
        this.mAppListChanged = false;
        if (this.mAppLoadingTask == null || this.mAppLoadingTask.getStatus() == AsyncTask.Status.FINISHED) {
            this.mAppLoadingTask = new AppLoadingTask().execute(new Void[0]);
        }
    }

    /* JADX WARN: Type inference failed for: r1v3, types: [com.android.settings.users.AppRestrictionsFragment$3] */
    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mNewUser = false;
        getActivity().unregisterReceiver(this.mUserBackgrounding);
        getActivity().unregisterReceiver(this.mPackageObserver);
        if (this.mAppListChanged) {
            new AsyncTask<Void, Void, Void>() { // from class: com.android.settings.users.AppRestrictionsFragment.3
                /* JADX INFO: Access modifiers changed from: protected */
                @Override // android.os.AsyncTask
                public Void doInBackground(Void... voidArr) {
                    AppRestrictionsFragment.this.mHelper.applyUserAppsStates(AppRestrictionsFragment.this);
                    return null;
                }
            }.execute(new Void[0]);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPackageChanged(Intent intent) {
        String action = intent.getAction();
        AppRestrictionsPreference appRestrictionsPreference = (AppRestrictionsPreference) findPreference(getKeyForPackage(intent.getData().getSchemeSpecificPart()));
        if (appRestrictionsPreference == null) {
            return;
        }
        if (("android.intent.action.PACKAGE_ADDED".equals(action) && appRestrictionsPreference.isChecked()) || ("android.intent.action.PACKAGE_REMOVED".equals(action) && !appRestrictionsPreference.isChecked())) {
            appRestrictionsPreference.setEnabled(true);
        }
    }

    protected PreferenceGroup getAppPreferenceGroup() {
        return getPreferenceScreen();
    }

    @Override // com.android.settingslib.users.AppRestrictionsHelper.OnDisableUiForPackageListener
    public void onDisableUiForPackage(String str) {
        AppRestrictionsPreference appRestrictionsPreference = (AppRestrictionsPreference) findPreference(getKeyForPackage(str));
        if (appRestrictionsPreference != null) {
            appRestrictionsPreference.setEnabled(false);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AppLoadingTask extends AsyncTask<Void, Void, Void> {
        private AppLoadingTask() {
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public Void doInBackground(Void... voidArr) {
            AppRestrictionsFragment.this.mHelper.fetchAndMergeApps();
            return null;
        }

        /* JADX INFO: Access modifiers changed from: protected */
        @Override // android.os.AsyncTask
        public void onPostExecute(Void r1) {
            AppRestrictionsFragment.this.populateApps();
        }
    }

    private boolean isPlatformSigned(PackageInfo packageInfo) {
        return (packageInfo == null || packageInfo.signatures == null || !this.mSysPackageInfo.signatures[0].equals(packageInfo.signatures[0])) ? false : true;
    }

    private boolean isAppEnabledForUser(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return false;
        }
        return (packageInfo.applicationInfo.flags & 8388608) != 0 && (packageInfo.applicationInfo.privateFlags & 1) == 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void populateApps() {
        PackageInfo packageInfo;
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        PackageManager packageManager = this.mPackageManager;
        IPackageManager iPackageManager = this.mIPm;
        int identifier = this.mUser.getIdentifier();
        if (Utils.getExistingUser(this.mUserManager, this.mUser) == null) {
            return;
        }
        this.mAppList.removeAll();
        List<ResolveInfo> queryBroadcastReceivers = packageManager.queryBroadcastReceivers(new Intent("android.intent.action.GET_RESTRICTION_ENTRIES"), 0);
        for (AppRestrictionsHelper.SelectableAppInfo selectableAppInfo : this.mHelper.getVisibleApps()) {
            String str = selectableAppInfo.packageName;
            if (str != null) {
                boolean equals = str.equals(activity.getPackageName());
                AppRestrictionsPreference appRestrictionsPreference = new AppRestrictionsPreference(getPrefContext(), this);
                boolean resolveInfoListHasPackage = resolveInfoListHasPackage(queryBroadcastReceivers, str);
                if (equals) {
                    addLocationAppRestrictionsPreference(selectableAppInfo, appRestrictionsPreference);
                    this.mHelper.setPackageSelected(str, true);
                } else {
                    try {
                        packageInfo = iPackageManager.getPackageInfo(str, 4194368, identifier);
                    } catch (RemoteException e) {
                        packageInfo = null;
                    }
                    if (packageInfo != null && (!this.mRestrictedProfile || !isAppUnsupportedInRestrictedProfile(packageInfo))) {
                        appRestrictionsPreference.setIcon(selectableAppInfo.icon != null ? selectableAppInfo.icon.mutate() : null);
                        appRestrictionsPreference.setChecked(false);
                        appRestrictionsPreference.setTitle(selectableAppInfo.activityName);
                        appRestrictionsPreference.setKey(getKeyForPackage(str));
                        appRestrictionsPreference.setSettingsEnabled(resolveInfoListHasPackage && selectableAppInfo.masterEntry == null);
                        appRestrictionsPreference.setPersistent(false);
                        appRestrictionsPreference.setOnPreferenceChangeListener(this);
                        appRestrictionsPreference.setOnPreferenceClickListener(this);
                        appRestrictionsPreference.setSummary(getPackageSummary(packageInfo, selectableAppInfo));
                        if (packageInfo.requiredForAllUsers || isPlatformSigned(packageInfo)) {
                            appRestrictionsPreference.setChecked(true);
                            appRestrictionsPreference.setImmutable(true);
                            if (resolveInfoListHasPackage) {
                                if (selectableAppInfo.masterEntry == null) {
                                    requestRestrictionsForApp(str, appRestrictionsPreference, false);
                                }
                            }
                        } else if (!this.mNewUser && isAppEnabledForUser(packageInfo)) {
                            appRestrictionsPreference.setChecked(true);
                        }
                        if (selectableAppInfo.masterEntry != null) {
                            appRestrictionsPreference.setImmutable(true);
                            appRestrictionsPreference.setChecked(this.mHelper.isPackageSelected(str));
                        }
                        appRestrictionsPreference.setOrder(100 * (this.mAppList.getPreferenceCount() + 2));
                        this.mHelper.setPackageSelected(str, appRestrictionsPreference.isChecked());
                        this.mAppList.addPreference(appRestrictionsPreference);
                    }
                }
            }
        }
        this.mAppListChanged = true;
        if (this.mNewUser && this.mFirstTime) {
            this.mFirstTime = false;
            this.mHelper.applyUserAppsStates(this);
        }
    }

    private String getPackageSummary(PackageInfo packageInfo, AppRestrictionsHelper.SelectableAppInfo selectableAppInfo) {
        if (selectableAppInfo.masterEntry != null) {
            if (this.mRestrictedProfile && packageInfo.restrictedAccountType != null) {
                return getString(R.string.app_sees_restricted_accounts_and_controlled_by, new Object[]{selectableAppInfo.masterEntry.activityName});
            }
            return getString(R.string.user_restrictions_controlled_by, new Object[]{selectableAppInfo.masterEntry.activityName});
        } else if (packageInfo.restrictedAccountType != null) {
            return getString(R.string.app_sees_restricted_accounts);
        } else {
            return null;
        }
    }

    private static boolean isAppUnsupportedInRestrictedProfile(PackageInfo packageInfo) {
        return packageInfo.requiredAccountType != null && packageInfo.restrictedAccountType == null;
    }

    private void addLocationAppRestrictionsPreference(AppRestrictionsHelper.SelectableAppInfo selectableAppInfo, AppRestrictionsPreference appRestrictionsPreference) {
        String str = selectableAppInfo.packageName;
        appRestrictionsPreference.setIcon(R.drawable.ic_settings_location);
        appRestrictionsPreference.setKey(getKeyForPackage(str));
        ArrayList<RestrictionEntry> restrictions = RestrictionUtils.getRestrictions(getActivity(), this.mUser);
        RestrictionEntry restrictionEntry = restrictions.get(0);
        appRestrictionsPreference.setTitle(restrictionEntry.getTitle());
        appRestrictionsPreference.setRestrictions(restrictions);
        appRestrictionsPreference.setSummary(restrictionEntry.getDescription());
        appRestrictionsPreference.setChecked(restrictionEntry.getSelectedState());
        appRestrictionsPreference.setPersistent(false);
        appRestrictionsPreference.setOnPreferenceClickListener(this);
        appRestrictionsPreference.setOrder(100);
        this.mAppList.addPreference(appRestrictionsPreference);
    }

    private String getKeyForPackage(String str) {
        return "pkg_" + str;
    }

    private boolean resolveInfoListHasPackage(List<ResolveInfo> list, String str) {
        for (ResolveInfo resolveInfo : list) {
            if (resolveInfo.activityInfo.packageName.equals(str)) {
                return true;
            }
        }
        return false;
    }

    private void updateAllEntries(String str, boolean z) {
        for (int i = 0; i < this.mAppList.getPreferenceCount(); i++) {
            Preference preference = this.mAppList.getPreference(i);
            if ((preference instanceof AppRestrictionsPreference) && str.equals(preference.getKey())) {
                ((AppRestrictionsPreference) preference).setChecked(z);
            }
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        if (view.getTag() instanceof AppRestrictionsPreference) {
            AppRestrictionsPreference appRestrictionsPreference = (AppRestrictionsPreference) view.getTag();
            if (view.getId() == R.id.app_restrictions_settings) {
                onAppSettingsIconClicked(appRestrictionsPreference);
            } else if (!appRestrictionsPreference.isImmutable()) {
                appRestrictionsPreference.setChecked(!appRestrictionsPreference.isChecked());
                String substring = appRestrictionsPreference.getKey().substring("pkg_".length());
                if (substring.equals(getActivity().getPackageName())) {
                    ((RestrictionEntry) appRestrictionsPreference.restrictions.get(0)).setSelectedState(appRestrictionsPreference.isChecked());
                    RestrictionUtils.setRestrictions(getActivity(), appRestrictionsPreference.restrictions, this.mUser);
                    return;
                }
                this.mHelper.setPackageSelected(substring, appRestrictionsPreference.isChecked());
                if (appRestrictionsPreference.isChecked() && appRestrictionsPreference.hasSettings && appRestrictionsPreference.restrictions == null) {
                    requestRestrictionsForApp(substring, appRestrictionsPreference, false);
                }
                this.mAppListChanged = true;
                if (!this.mRestrictedProfile) {
                    this.mHelper.applyUserAppState(substring, appRestrictionsPreference.isChecked(), this);
                }
                updateAllEntries(appRestrictionsPreference.getKey(), appRestrictionsPreference.isChecked());
            }
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        String key = preference.getKey();
        if (key != null && key.contains(";")) {
            StringTokenizer stringTokenizer = new StringTokenizer(key, ";");
            String nextToken = stringTokenizer.nextToken();
            String nextToken2 = stringTokenizer.nextToken();
            PreferenceGroup preferenceGroup = this.mAppList;
            ArrayList<RestrictionEntry> restrictions = ((AppRestrictionsPreference) preferenceGroup.findPreference("pkg_" + nextToken)).getRestrictions();
            if (restrictions != null) {
                Iterator<RestrictionEntry> it = restrictions.iterator();
                while (it.hasNext()) {
                    RestrictionEntry next = it.next();
                    if (next.getKey().equals(nextToken2)) {
                        switch (next.getType()) {
                            case 1:
                                next.setSelectedState(((Boolean) obj).booleanValue());
                                this.mUserManager.setApplicationRestrictions(nextToken, RestrictionsManager.convertRestrictionsToBundle(restrictions), this.mUser);
                                return true;
                            case 2:
                            case 3:
                                String str = (String) obj;
                                next.setSelectedString(str);
                                ((ListPreference) preference).setSummary(findInArray(next.getChoiceEntries(), next.getChoiceValues(), str));
                                this.mUserManager.setApplicationRestrictions(nextToken, RestrictionsManager.convertRestrictionsToBundle(restrictions), this.mUser);
                                return true;
                            case 4:
                                Set set = (Set) obj;
                                String[] strArr = new String[set.size()];
                                set.toArray(strArr);
                                next.setAllSelectedStrings(strArr);
                                this.mUserManager.setApplicationRestrictions(nextToken, RestrictionsManager.convertRestrictionsToBundle(restrictions), this.mUser);
                                return true;
                        }
                    }
                }
                return true;
            }
            return true;
        }
        return false;
    }

    private void removeRestrictionsForApp(AppRestrictionsPreference appRestrictionsPreference) {
        for (Preference preference : appRestrictionsPreference.mChildren) {
            this.mAppList.removePreference(preference);
        }
        appRestrictionsPreference.mChildren.clear();
    }

    private void onAppSettingsIconClicked(AppRestrictionsPreference appRestrictionsPreference) {
        if (appRestrictionsPreference.getKey().startsWith("pkg_")) {
            if (appRestrictionsPreference.isPanelOpen()) {
                removeRestrictionsForApp(appRestrictionsPreference);
            } else {
                requestRestrictionsForApp(appRestrictionsPreference.getKey().substring("pkg_".length()), appRestrictionsPreference, true);
            }
            appRestrictionsPreference.setPanelOpen(!appRestrictionsPreference.isPanelOpen());
        }
    }

    private void requestRestrictionsForApp(String str, AppRestrictionsPreference appRestrictionsPreference, boolean z) {
        Bundle applicationRestrictions = this.mUserManager.getApplicationRestrictions(str, this.mUser);
        Intent intent = new Intent("android.intent.action.GET_RESTRICTION_ENTRIES");
        intent.setPackage(str);
        intent.putExtra("android.intent.extra.restrictions_bundle", applicationRestrictions);
        intent.addFlags(32);
        getActivity().sendOrderedBroadcast(intent, null, new RestrictionsResultReceiver(str, appRestrictionsPreference, z), null, -1, null, null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class RestrictionsResultReceiver extends BroadcastReceiver {
        boolean invokeIfCustom;
        String packageName;
        AppRestrictionsPreference preference;

        RestrictionsResultReceiver(String str, AppRestrictionsPreference appRestrictionsPreference, boolean z) {
            this.packageName = str;
            this.preference = appRestrictionsPreference;
            this.invokeIfCustom = z;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Bundle resultExtras = getResultExtras(true);
            ArrayList<RestrictionEntry> parcelableArrayList = resultExtras.getParcelableArrayList("android.intent.extra.restrictions_list");
            Intent intent2 = (Intent) resultExtras.getParcelable("android.intent.extra.restrictions_intent");
            if (parcelableArrayList != null && intent2 == null) {
                AppRestrictionsFragment.this.onRestrictionsReceived(this.preference, parcelableArrayList);
                if (AppRestrictionsFragment.this.mRestrictedProfile) {
                    AppRestrictionsFragment.this.mUserManager.setApplicationRestrictions(this.packageName, RestrictionsManager.convertRestrictionsToBundle(parcelableArrayList), AppRestrictionsFragment.this.mUser);
                }
            } else if (intent2 != null) {
                this.preference.setRestrictions(parcelableArrayList);
                if (this.invokeIfCustom && AppRestrictionsFragment.this.isResumed()) {
                    assertSafeToStartCustomActivity(intent2);
                    AppRestrictionsFragment.this.startActivityForResult(intent2, AppRestrictionsFragment.this.generateCustomActivityRequestCode(this.preference));
                }
            }
        }

        private void assertSafeToStartCustomActivity(Intent intent) {
            if (intent.getPackage() != null && intent.getPackage().equals(this.packageName)) {
                return;
            }
            List<ResolveInfo> queryIntentActivities = AppRestrictionsFragment.this.mPackageManager.queryIntentActivities(intent, 0);
            if (queryIntentActivities.size() != 1) {
                return;
            }
            if (!this.packageName.equals(queryIntentActivities.get(0).activityInfo.packageName)) {
                throw new SecurityException("Application " + this.packageName + " is not allowed to start activity " + intent);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onRestrictionsReceived(AppRestrictionsPreference appRestrictionsPreference, ArrayList<RestrictionEntry> arrayList) {
        removeRestrictionsForApp(appRestrictionsPreference);
        Iterator<RestrictionEntry> it = arrayList.iterator();
        int i = 1;
        while (it.hasNext()) {
            RestrictionEntry next = it.next();
            Preference preference = null;
            switch (next.getType()) {
                case 1:
                    preference = new SwitchPreference(getPrefContext());
                    preference.setTitle(next.getTitle());
                    preference.setSummary(next.getDescription());
                    ((SwitchPreference) preference).setChecked(next.getSelectedState());
                    break;
                case 2:
                case 3:
                    preference = new ListPreference(getPrefContext());
                    preference.setTitle(next.getTitle());
                    String selectedString = next.getSelectedString();
                    if (selectedString == null) {
                        selectedString = next.getDescription();
                    }
                    preference.setSummary(findInArray(next.getChoiceEntries(), next.getChoiceValues(), selectedString));
                    ListPreference listPreference = (ListPreference) preference;
                    listPreference.setEntryValues(next.getChoiceValues());
                    listPreference.setEntries(next.getChoiceEntries());
                    listPreference.setValue(selectedString);
                    listPreference.setDialogTitle(next.getTitle());
                    break;
                case 4:
                    preference = new MultiSelectListPreference(getPrefContext());
                    preference.setTitle(next.getTitle());
                    MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) preference;
                    multiSelectListPreference.setEntryValues(next.getChoiceValues());
                    multiSelectListPreference.setEntries(next.getChoiceEntries());
                    HashSet hashSet = new HashSet();
                    Collections.addAll(hashSet, next.getAllSelectedStrings());
                    multiSelectListPreference.setValues(hashSet);
                    multiSelectListPreference.setDialogTitle(next.getTitle());
                    break;
            }
            if (preference != null) {
                preference.setPersistent(false);
                preference.setOrder(appRestrictionsPreference.getOrder() + i);
                preference.setKey(appRestrictionsPreference.getKey().substring("pkg_".length()) + ";" + next.getKey());
                this.mAppList.addPreference(preference);
                preference.setOnPreferenceChangeListener(this);
                preference.setIcon(R.drawable.empty_icon);
                appRestrictionsPreference.mChildren.add(preference);
                i++;
            }
        }
        appRestrictionsPreference.setRestrictions(arrayList);
        if (i == 1 && appRestrictionsPreference.isImmutable() && appRestrictionsPreference.isChecked()) {
            this.mAppList.removePreference(appRestrictionsPreference);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int generateCustomActivityRequestCode(AppRestrictionsPreference appRestrictionsPreference) {
        this.mCustomRequestCode++;
        this.mCustomRequestMap.put(Integer.valueOf(this.mCustomRequestCode), appRestrictionsPreference);
        return this.mCustomRequestCode;
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        AppRestrictionsPreference appRestrictionsPreference = this.mCustomRequestMap.get(Integer.valueOf(i));
        if (appRestrictionsPreference == null) {
            String str = TAG;
            Log.w(str, "Unknown requestCode " + i);
            return;
        }
        if (i2 == -1) {
            String substring = appRestrictionsPreference.getKey().substring("pkg_".length());
            ArrayList<RestrictionEntry> parcelableArrayListExtra = intent.getParcelableArrayListExtra("android.intent.extra.restrictions_list");
            Bundle bundleExtra = intent.getBundleExtra("android.intent.extra.restrictions_bundle");
            if (parcelableArrayListExtra != null) {
                appRestrictionsPreference.setRestrictions(parcelableArrayListExtra);
                this.mUserManager.setApplicationRestrictions(substring, RestrictionsManager.convertRestrictionsToBundle(parcelableArrayListExtra), this.mUser);
            } else if (bundleExtra != null) {
                this.mUserManager.setApplicationRestrictions(substring, bundleExtra, this.mUser);
            }
        }
        this.mCustomRequestMap.remove(Integer.valueOf(i));
    }

    private String findInArray(String[] strArr, String[] strArr2, String str) {
        for (int i = 0; i < strArr2.length; i++) {
            if (strArr2[i].equals(str)) {
                return strArr[i];
            }
        }
        return str;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().startsWith("pkg_")) {
            AppRestrictionsPreference appRestrictionsPreference = (AppRestrictionsPreference) preference;
            if (!appRestrictionsPreference.isImmutable()) {
                String substring = appRestrictionsPreference.getKey().substring("pkg_".length());
                boolean z = !appRestrictionsPreference.isChecked();
                appRestrictionsPreference.setChecked(z);
                this.mHelper.setPackageSelected(substring, z);
                updateAllEntries(appRestrictionsPreference.getKey(), z);
                this.mAppListChanged = true;
                this.mHelper.applyUserAppState(substring, z, this);
            }
            return true;
        }
        return false;
    }
}
