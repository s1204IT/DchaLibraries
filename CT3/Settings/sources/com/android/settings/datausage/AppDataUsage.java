package com.android.settings.datausage;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.drawable.Drawable;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.text.format.Formatter;
import android.util.ArraySet;
import android.view.View;
import android.widget.AdapterView;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.datausage.CycleAdapter;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settingslib.AppItem;
import com.android.settingslib.Utils;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;
import com.android.settingslib.net.UidDetailProvider;
import java.util.Iterator;

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
    private String mPackageName;
    private NetworkPolicy mPolicy;
    private SwitchPreference mRestrictBackground;
    private long mStart;
    private INetworkStatsSession mStatsSession;
    private NetworkTemplate mTemplate;
    private Preference mTotalUsage;
    private SwitchPreference mUnrestrictedData;
    private final ArraySet<String> mPackages = new ArraySet<>();
    private AdapterView.OnItemSelectedListener mCycleListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            CycleAdapter.CycleItem cycle = (CycleAdapter.CycleItem) AppDataUsage.this.mCycle.getSelectedItem();
            AppDataUsage.this.mStart = cycle.start;
            AppDataUsage.this.mEnd = cycle.end;
            AppDataUsage.this.bindData();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    private final LoaderManager.LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderManager.LoaderCallbacks<ChartData>() {
        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(AppDataUsage.this.getActivity(), AppDataUsage.this.mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            AppDataUsage.this.mChartData = data;
            AppDataUsage.this.mCycleAdapter.updateCycleList(AppDataUsage.this.mPolicy, AppDataUsage.this.mChartData);
            AppDataUsage.this.bindData();
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle args = getArguments();
        try {
            this.mStatsSession = this.services.mStatsService.openSession();
            this.mAppItem = args != null ? (AppItem) args.getParcelable("app_item") : null;
            this.mTemplate = args != null ? (NetworkTemplate) args.getParcelable("network_template") : null;
            if (this.mTemplate == null) {
                Context context = getContext();
                this.mTemplate = DataUsageSummary.getDefaultTemplate(context, DataUsageSummary.getDefaultSubscriptionId(context));
            }
            if (this.mAppItem == null) {
                int uid = args != null ? args.getInt("uid", -1) : getActivity().getIntent().getIntExtra("uid", -1);
                if (uid == -1) {
                    getActivity().finish();
                } else {
                    addUid(uid);
                    this.mAppItem = new AppItem(uid);
                    this.mAppItem.addUid(uid);
                }
            } else {
                for (int i = 0; i < this.mAppItem.uids.size(); i++) {
                    addUid(this.mAppItem.uids.keyAt(i));
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
                    PackageManager pm = getPackageManager();
                    try {
                        ApplicationInfo info = pm.getApplicationInfo(this.mPackages.valueAt(0), 0);
                        this.mIcon = info.loadIcon(pm);
                        this.mLabel = info.loadLabel(pm);
                        this.mPackageName = info.packageName;
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
                if (!UserHandle.isApp(this.mAppItem.key)) {
                    removePreference("unrestricted_data_saver");
                    removePreference("restrict_background");
                } else {
                    this.mRestrictBackground = (SwitchPreference) findPreference("restrict_background");
                    this.mRestrictBackground.setOnPreferenceChangeListener(this);
                    this.mUnrestrictedData = (SwitchPreference) findPreference("unrestricted_data_saver");
                    this.mUnrestrictedData.setOnPreferenceChangeListener(this);
                }
                this.mDataSaverBackend = new DataSaverBackend(getContext());
                this.mAppSettings = findPreference("app_settings");
                this.mAppSettingsIntent = new Intent("android.intent.action.MANAGE_NETWORK_USAGE");
                this.mAppSettingsIntent.addCategory("android.intent.category.DEFAULT");
                PackageManager pm2 = getPackageManager();
                boolean matchFound = false;
                Iterator packageName$iterator = this.mPackages.iterator();
                while (true) {
                    if (!packageName$iterator.hasNext()) {
                        break;
                    }
                    String packageName = (String) packageName$iterator.next();
                    this.mAppSettingsIntent.setPackage(packageName);
                    if (pm2.resolveActivity(this.mAppSettingsIntent, 0) != null) {
                        matchFound = true;
                        break;
                    }
                }
                if (!matchFound) {
                    removePreference("app_settings");
                    this.mAppSettings = null;
                }
                if (this.mPackages.size() > 1) {
                    this.mAppList = (PreferenceCategory) findPreference("app_list");
                    for (int i2 = 1; i2 < this.mPackages.size(); i2++) {
                        new AppPrefLoader(this, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, this.mPackages.valueAt(i2));
                    }
                    return;
                }
                removePreference("app_list");
                return;
            }
            if (this.mAppItem.key == -4) {
                this.mLabel = getContext().getString(R.string.data_usage_uninstalled_apps_users);
            } else if (this.mAppItem.key == -5) {
                this.mLabel = getContext().getString(R.string.tether_settings_title_all);
            } else {
                int userId = UidDetailProvider.getUserIdForKey(this.mAppItem.key);
                UserManager um = UserManager.get(getActivity());
                UserInfo info2 = um.getUserInfo(userId);
                getPackageManager();
                this.mIcon = Utils.getUserIcon(getActivity(), um, info2);
                this.mLabel = Utils.getUserLabel(getActivity(), info2);
                this.mPackageName = getActivity().getPackageName();
            }
            removePreference("unrestricted_data_saver");
            removePreference("app_settings");
            removePreference("restrict_background");
            removePreference("app_list");
        } catch (RemoteException e2) {
            throw new RuntimeException(e2);
        }
    }

    @Override
    public void onDestroy() {
        TrafficStats.closeQuietly(this.mStatsSession);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mDataSaverBackend != null) {
            this.mDataSaverBackend.addListener(this);
        }
        this.mPolicy = this.services.mPolicyEditor.getPolicy(this.mTemplate);
        getLoaderManager().restartLoader(2, ChartDataLoader.buildArgs(this.mTemplate, this.mAppItem), this.mChartDataCallbacks);
        updatePrefs();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mDataSaverBackend == null) {
            return;
        }
        this.mDataSaverBackend.remListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == this.mRestrictBackground) {
            this.mDataSaverBackend.setIsBlacklisted(this.mAppItem.key, this.mPackageName, ((Boolean) newValue).booleanValue() ? false : true);
            return true;
        }
        if (preference != this.mUnrestrictedData) {
            return false;
        }
        this.mDataSaverBackend.setIsWhitelisted(this.mAppItem.key, this.mPackageName, ((Boolean) newValue).booleanValue());
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == this.mAppSettings) {
            getActivity().startActivityAsUser(this.mAppSettingsIntent, new UserHandle(UserHandle.getUserId(this.mAppItem.key)));
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void updatePrefs() {
        updatePrefs(getAppRestrictBackground(), getUnrestrictData());
    }

    private void updatePrefs(boolean restrictBackground, boolean unrestrictData) {
        if (this.mRestrictBackground != null) {
            this.mRestrictBackground.setChecked(!restrictBackground);
        }
        if (this.mUnrestrictedData == null) {
            return;
        }
        if (restrictBackground) {
            this.mUnrestrictedData.setVisible(false);
        } else {
            this.mUnrestrictedData.setVisible(true);
            this.mUnrestrictedData.setChecked(unrestrictData);
        }
    }

    private void addUid(int uid) {
        String[] packages = getPackageManager().getPackagesForUid(uid);
        if (packages == null) {
            return;
        }
        for (String str : packages) {
            this.mPackages.add(str);
        }
    }

    public void bindData() {
        long foregroundBytes;
        long backgroundBytes;
        if (this.mChartData == null || this.mStart == 0) {
            foregroundBytes = 0;
            backgroundBytes = 0;
            this.mCycle.setVisible(false);
        } else {
            this.mCycle.setVisible(true);
            long now = System.currentTimeMillis();
            NetworkStatsHistory.Entry entry = this.mChartData.detailDefault.getValues(this.mStart, this.mEnd, now, (NetworkStatsHistory.Entry) null);
            backgroundBytes = entry.rxBytes + entry.txBytes;
            NetworkStatsHistory.Entry entry2 = this.mChartData.detailForeground.getValues(this.mStart, this.mEnd, now, entry);
            foregroundBytes = entry2.rxBytes + entry2.txBytes;
        }
        long totalBytes = backgroundBytes + foregroundBytes;
        Context context = getContext();
        this.mTotalUsage.setSummary(Formatter.formatFileSize(context, totalBytes));
        this.mForegroundUsage.setSummary(Formatter.formatFileSize(context, foregroundBytes));
        this.mBackgroundUsage.setSummary(Formatter.formatFileSize(context, backgroundBytes));
    }

    private boolean getAppRestrictBackground() {
        int uid = this.mAppItem.key;
        int uidPolicy = this.services.mPolicyManager.getUidPolicy(uid);
        return (uidPolicy & 1) != 0;
    }

    private boolean getUnrestrictData() {
        if (this.mDataSaverBackend != null) {
            return this.mDataSaverBackend.isWhitelisted(this.mAppItem.key);
        }
        return false;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View header = setPinnedHeaderView(R.layout.app_header);
        String strValueAt = this.mPackages.size() != 0 ? this.mPackages.valueAt(0) : null;
        int uid = 0;
        if (strValueAt != null) {
            try {
                uid = getPackageManager().getPackageUid(strValueAt, 0);
            } catch (PackageManager.NameNotFoundException e) {
            }
        } else {
            uid = 0;
        }
        AppHeader.setupHeaderView(getActivity(), this.mIcon, this.mLabel, strValueAt, uid, AppHeader.includeAppInfo(this), 0, header, null);
    }

    @Override
    protected int getMetricsCategory() {
        return 343;
    }

    private class AppPrefLoader extends AsyncTask<String, Void, Preference> {
        AppPrefLoader(AppDataUsage this$0, AppPrefLoader appPrefLoader) {
            this();
        }

        private AppPrefLoader() {
        }

        @Override
        public Preference doInBackground(String... params) {
            PackageManager pm = AppDataUsage.this.getPackageManager();
            String pkg = params[0];
            try {
                ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
                Preference preference = new Preference(AppDataUsage.this.getPrefContext());
                preference.setIcon(info.loadIcon(pm));
                preference.setTitle(info.loadLabel(pm));
                preference.setSelectable(false);
                return preference;
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        @Override
        public void onPostExecute(Preference pref) {
            if (pref == null || AppDataUsage.this.mAppList == null) {
                return;
            }
            AppDataUsage.this.mAppList.addPreference(pref);
        }
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
        if (!this.mAppItem.uids.get(uid, false)) {
            return;
        }
        updatePrefs(getAppRestrictBackground(), isWhitelisted);
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
        if (!this.mAppItem.uids.get(uid, false)) {
            return;
        }
        updatePrefs(isBlacklisted, getUnrestrictData());
    }
}
