package com.android.settings.datausage;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.ArraySet;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import com.android.settings.R;
import com.android.settings.datausage.CycleAdapter;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.AppItem;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;
import com.android.settingslib.net.UidDetail;
import com.android.settingslib.net.UidDetailProvider;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.Iterator;

/* loaded from: classes.dex */
public class AppDataUsage extends DataUsageBase implements Preference.OnPreferenceChangeListener, DataSaverBackend.Listener {
    private AppItem mAppItem;
    private PreferenceCategory mAppList;
    private Preference mAppSettings;
    private Intent mAppSettingsIntent;
    private Preference mBackgroundUsage;
    private ChartData mChartData;
    private SpinnerPreference mCycle;
    private CycleAdapter mCycleAdapter;
    private DataSaverBackend mDataSaverBackend;
    private long mEnd;
    private Preference mForegroundUsage;
    private Drawable mIcon;
    private CharSequence mLabel;
    private PackageManagerWrapper mPackageManagerWrapper;
    private String mPackageName;
    private NetworkPolicy mPolicy;
    private RestrictedSwitchPreference mRestrictBackground;
    private long mStart;
    private INetworkStatsSession mStatsSession;
    private NetworkTemplate mTemplate;
    private Preference mTotalUsage;
    private RestrictedSwitchPreference mUnrestrictedData;
    private final ArraySet<String> mPackages = new ArraySet<>();
    private AdapterView.OnItemSelectedListener mCycleListener = new AdapterView.OnItemSelectedListener() { // from class: com.android.settings.datausage.AppDataUsage.1
        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
            CycleAdapter.CycleItem cycleItem = (CycleAdapter.CycleItem) AppDataUsage.this.mCycle.getSelectedItem();
            AppDataUsage.this.mStart = cycleItem.start;
            AppDataUsage.this.mEnd = cycleItem.end;
            AppDataUsage.this.bindData();
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };
    private final LoaderManager.LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderManager.LoaderCallbacks<ChartData>() { // from class: com.android.settings.datausage.AppDataUsage.2
        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<ChartData> onCreateLoader(int i, Bundle bundle) {
            return new ChartDataLoader(AppDataUsage.this.getActivity(), AppDataUsage.this.mStatsSession, bundle);
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<ChartData> loader, ChartData chartData) {
            AppDataUsage.this.mChartData = chartData;
            AppDataUsage.this.mCycleAdapter.updateCycleList(AppDataUsage.this.mPolicy, AppDataUsage.this.mChartData);
            AppDataUsage.this.bindData();
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoaderReset(Loader<ChartData> loader) {
        }
    };
    private final LoaderManager.LoaderCallbacks<ArraySet<Preference>> mAppPrefCallbacks = new LoaderManager.LoaderCallbacks<ArraySet<Preference>>() { // from class: com.android.settings.datausage.AppDataUsage.3
        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<ArraySet<Preference>> onCreateLoader(int i, Bundle bundle) {
            return new AppPrefLoader(AppDataUsage.this.getPrefContext(), AppDataUsage.this.mPackages, AppDataUsage.this.getPackageManager());
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<ArraySet<Preference>> loader, ArraySet<Preference> arraySet) {
            if (arraySet != null && AppDataUsage.this.mAppList != null) {
                Iterator<Preference> it = arraySet.iterator();
                while (it.hasNext()) {
                    AppDataUsage.this.mAppList.addPreference(it.next());
                }
            }
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoaderReset(Loader<ArraySet<Preference>> loader) {
        }
    };

    @Override // com.android.settings.datausage.DataUsageBase, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        super.onCreate(bundle);
        this.mPackageManagerWrapper = new PackageManagerWrapper(getPackageManager());
        Bundle arguments = getArguments();
        try {
            this.mStatsSession = this.services.mStatsService.openSession();
            this.mAppItem = arguments != null ? (AppItem) arguments.getParcelable("app_item") : null;
            this.mTemplate = arguments != null ? (NetworkTemplate) arguments.getParcelable("network_template") : null;
            if (this.mTemplate == null) {
                Context context = getContext();
                this.mTemplate = DataUsageUtils.getDefaultTemplate(context, DataUsageUtils.getDefaultSubscriptionId(context));
            }
            boolean z = false;
            if (this.mAppItem == null) {
                int i = arguments != null ? arguments.getInt("uid", -1) : getActivity().getIntent().getIntExtra("uid", -1);
                if (i == -1) {
                    getActivity().finish();
                } else {
                    addUid(i);
                    this.mAppItem = new AppItem(i);
                    this.mAppItem.addUid(i);
                }
            } else {
                for (int i2 = 0; i2 < this.mAppItem.uids.size(); i2++) {
                    addUid(this.mAppItem.uids.keyAt(i2));
                }
            }
            addPreferencesFromResource(R.xml.app_data_usage);
            this.mTotalUsage = findPreference("total_usage");
            this.mForegroundUsage = findPreference("foreground_usage");
            this.mBackgroundUsage = findPreference("background_usage");
            this.mCycle = (SpinnerPreference) findPreference("cycle");
            this.mCycleAdapter = new CycleAdapter(getContext(), this.mCycle, this.mCycleListener, false);
            if (this.mAppItem.key > 0) {
                if (this.mPackages.size() != 0) {
                    try {
                        ApplicationInfo applicationInfoAsUser = this.mPackageManagerWrapper.getApplicationInfoAsUser(this.mPackages.valueAt(0), 0, UserHandle.getUserId(this.mAppItem.key));
                        this.mIcon = IconDrawableFactory.newInstance(getActivity()).getBadgedIcon(applicationInfoAsUser);
                        this.mLabel = applicationInfoAsUser.loadLabel(this.mPackageManagerWrapper.getPackageManager());
                        this.mPackageName = applicationInfoAsUser.packageName;
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                if (!UserHandle.isApp(this.mAppItem.key)) {
                    removePreference("unrestricted_data_saver");
                    removePreference("restrict_background");
                } else {
                    this.mRestrictBackground = (RestrictedSwitchPreference) findPreference("restrict_background");
                    this.mRestrictBackground.setOnPreferenceChangeListener(this);
                    this.mUnrestrictedData = (RestrictedSwitchPreference) findPreference("unrestricted_data_saver");
                    this.mUnrestrictedData.setOnPreferenceChangeListener(this);
                }
                this.mDataSaverBackend = new DataSaverBackend(getContext());
                this.mAppSettings = findPreference("app_settings");
                this.mAppSettingsIntent = new Intent("android.intent.action.MANAGE_NETWORK_USAGE");
                this.mAppSettingsIntent.addCategory("android.intent.category.DEFAULT");
                PackageManager packageManager = getPackageManager();
                Iterator<String> it = this.mPackages.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    this.mAppSettingsIntent.setPackage(it.next());
                    if (packageManager.resolveActivity(this.mAppSettingsIntent, 0) != null) {
                        z = true;
                        break;
                    }
                }
                if (!z) {
                    removePreference("app_settings");
                    this.mAppSettings = null;
                }
                if (this.mPackages.size() > 1) {
                    this.mAppList = (PreferenceCategory) findPreference("app_list");
                    getLoaderManager().initLoader(3, Bundle.EMPTY, this.mAppPrefCallbacks);
                    return;
                } else {
                    removePreference("app_list");
                    return;
                }
            }
            Activity activity = getActivity();
            UidDetail uidDetail = new UidDetailProvider(activity).getUidDetail(this.mAppItem.key, true);
            this.mIcon = uidDetail.icon;
            this.mLabel = uidDetail.label;
            this.mPackageName = activity.getPackageName();
            removePreference("unrestricted_data_saver");
            removePreference("app_settings");
            removePreference("restrict_background");
            removePreference("app_list");
        } catch (RemoteException e2) {
            throw new RuntimeException(e2);
        }
    }

    @Override // com.android.settings.datausage.DataUsageBase, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        TrafficStats.closeQuietly(this.mStatsSession);
        super.onDestroy();
    }

    @Override // com.android.settings.datausage.DataUsageBase, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.mDataSaverBackend != null) {
            this.mDataSaverBackend.addListener(this);
        }
        this.mPolicy = this.services.mPolicyEditor.getPolicy(this.mTemplate);
        getLoaderManager().restartLoader(2, ChartDataLoader.buildArgs(this.mTemplate, this.mAppItem), this.mChartDataCallbacks);
        updatePrefs();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        if (this.mDataSaverBackend != null) {
            this.mDataSaverBackend.remListener(this);
        }
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceChangeListener
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mRestrictBackground) {
            this.mDataSaverBackend.setIsBlacklisted(this.mAppItem.key, this.mPackageName, !((Boolean) obj).booleanValue());
            updatePrefs();
            return true;
        }
        if (preference == this.mUnrestrictedData) {
            this.mDataSaverBackend.setIsWhitelisted(this.mAppItem.key, this.mPackageName, ((Boolean) obj).booleanValue());
            return true;
        }
        return false;
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.support.v7.preference.PreferenceManager.OnPreferenceTreeClickListener
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == this.mAppSettings) {
            getActivity().startActivityAsUser(this.mAppSettingsIntent, new UserHandle(UserHandle.getUserId(this.mAppItem.key)));
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    void updatePrefs() {
        updatePrefs(getAppRestrictBackground(), getUnrestrictData());
    }

    private void updatePrefs(boolean z, boolean z2) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfMeteredDataRestricted = RestrictedLockUtils.checkIfMeteredDataRestricted(getContext(), this.mPackageName, UserHandle.getUserId(this.mAppItem.key));
        if (this.mRestrictBackground != null) {
            this.mRestrictBackground.setChecked(!z);
            this.mRestrictBackground.setDisabledByAdmin(enforcedAdminCheckIfMeteredDataRestricted);
        }
        if (this.mUnrestrictedData != null) {
            if (z) {
                this.mUnrestrictedData.setVisible(false);
                return;
            }
            this.mUnrestrictedData.setVisible(true);
            this.mUnrestrictedData.setChecked(z2);
            this.mUnrestrictedData.setDisabledByAdmin(enforcedAdminCheckIfMeteredDataRestricted);
        }
    }

    private void addUid(int i) {
        String[] packagesForUid = getPackageManager().getPackagesForUid(i);
        if (packagesForUid != null) {
            for (String str : packagesForUid) {
                this.mPackages.add(str);
            }
        }
    }

    private void bindData() {
        long j;
        long j2 = 0;
        if (this.mChartData == null || this.mStart == 0) {
            this.mCycle.setVisible(false);
            j = 0;
        } else {
            this.mCycle.setVisible(true);
            long jCurrentTimeMillis = System.currentTimeMillis();
            NetworkStatsHistory.Entry values = this.mChartData.detailDefault.getValues(this.mStart, this.mEnd, jCurrentTimeMillis, (NetworkStatsHistory.Entry) null);
            long j3 = values.rxBytes + values.txBytes;
            NetworkStatsHistory.Entry values2 = this.mChartData.detailForeground.getValues(this.mStart, this.mEnd, jCurrentTimeMillis, values);
            j = values2.rxBytes + values2.txBytes;
            j2 = j3;
        }
        Context context = getContext();
        this.mTotalUsage.setSummary(DataUsageUtils.formatDataUsage(context, j2 + j));
        this.mForegroundUsage.setSummary(DataUsageUtils.formatDataUsage(context, j));
        this.mBackgroundUsage.setSummary(DataUsageUtils.formatDataUsage(context, j2));
    }

    private boolean getAppRestrictBackground() {
        return (this.services.mPolicyManager.getUidPolicy(this.mAppItem.key) & 1) != 0;
    }

    private boolean getUnrestrictData() {
        if (this.mDataSaverBackend != null) {
            return this.mDataSaverBackend.isWhitelisted(this.mAppItem.key);
        }
        return false;
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        int packageUidAsUser;
        super.onViewCreated(view, bundle);
        String strValueAt = this.mPackages.size() != 0 ? this.mPackages.valueAt(0) : null;
        if (strValueAt != null) {
            try {
                packageUidAsUser = this.mPackageManagerWrapper.getPackageUidAsUser(strValueAt, UserHandle.getUserId(this.mAppItem.key));
            } catch (PackageManager.NameNotFoundException e) {
                Log.w("AppDataUsage", "Skipping UID because cannot find package " + strValueAt);
            }
        } else {
            packageUidAsUser = 0;
        }
        boolean z = this.mAppItem.key > 0;
        Activity activity = getActivity();
        getPreferenceScreen().addPreference(EntityHeaderController.newInstance(activity, this, null).setRecyclerView(getListView(), getLifecycle()).setUid(packageUidAsUser).setHasAppInfoLink(z).setButtonActions(0, 0).setIcon(this.mIcon).setLabel(this.mLabel).setPackageName(strValueAt).done(activity, getPrefContext()));
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 343;
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onDataSaverChanged(boolean z) {
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onWhitelistStatusChanged(int i, boolean z) {
        if (this.mAppItem.uids.get(i, false)) {
            updatePrefs(getAppRestrictBackground(), z);
        }
    }

    @Override // com.android.settings.datausage.DataSaverBackend.Listener
    public void onBlacklistStatusChanged(int i, boolean z) {
        if (this.mAppItem.uids.get(i, false)) {
            updatePrefs(z, getUnrestrictData());
        }
    }
}
