package com.android.settings.datausage;

import android.app.ActivityManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.pm.UserInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.settings.R;
import com.android.settings.datausage.CycleAdapter;
import com.android.settingslib.AppItem;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;
import com.android.settingslib.net.SummaryForAllUidLoader;
import com.android.settingslib.net.UidDetailProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataUsageList extends DataUsageBase {
    private PreferenceGroup mApps;
    private boolean mBinding;
    private ChartDataUsagePreference mChart;
    private ChartData mChartData;
    private CycleAdapter mCycleAdapter;
    private Spinner mCycleSpinner;
    private View mHeader;
    private INetworkStatsSession mStatsSession;
    private int mSubId;
    private NetworkTemplate mTemplate;
    private UidDetailProvider mUidDetailProvider;
    private Preference mUsageAmount;
    private final Map<String, Boolean> mMobileDataEnabled = new HashMap();
    private AdapterView.OnItemSelectedListener mCycleListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            CycleAdapter.CycleItem cycle = (CycleAdapter.CycleItem) DataUsageList.this.mCycleSpinner.getSelectedItem();
            Log.d("DataUsage", "showing cycle " + cycle + ", start=" + cycle.start + ", end=" + cycle.end + "]");
            DataUsageList.this.mChart.setVisibleRange(cycle.start, cycle.end);
            DataUsageList.this.updateDetailData();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    private final LoaderManager.LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderManager.LoaderCallbacks<ChartData>() {
        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(DataUsageList.this.getActivity(), DataUsageList.this.mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            DataUsageList.this.setLoading(false, true);
            DataUsageList.this.mChartData = data;
            DataUsageList.this.mChart.setNetworkStats(DataUsageList.this.mChartData.network);
            DataUsageList.this.updatePolicy(true);
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
            DataUsageList.this.mChartData = null;
            DataUsageList.this.mChart.setNetworkStats(null);
        }
    };
    private final LoaderManager.LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderManager.LoaderCallbacks<NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(DataUsageList.this.getActivity(), DataUsageList.this.mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            int[] restrictedUids = DataUsageList.this.services.mPolicyManager.getUidsWithPolicy(1);
            DataUsageList.this.bindStats(data, restrictedUids);
            updateEmptyVisible();
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {
            DataUsageList.this.bindStats(null, new int[0]);
            updateEmptyVisible();
        }

        private void updateEmptyVisible() {
            if ((DataUsageList.this.mApps.getPreferenceCount() != 0) == (DataUsageList.this.getPreferenceScreen().getPreferenceCount() != 0)) {
                return;
            }
            if (DataUsageList.this.mApps.getPreferenceCount() != 0) {
                DataUsageList.this.getPreferenceScreen().addPreference(DataUsageList.this.mUsageAmount);
                DataUsageList.this.getPreferenceScreen().addPreference(DataUsageList.this.mApps);
            } else {
                DataUsageList.this.getPreferenceScreen().removeAll();
            }
        }
    };

    @Override
    protected int getMetricsCategory() {
        return 341;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        if (!isBandwidthControlEnabled()) {
            Log.w("DataUsage", "No bandwidth control; leaving");
            getActivity().finish();
        }
        try {
            this.mStatsSession = this.services.mStatsService.openSession();
            this.mUidDetailProvider = new UidDetailProvider(context);
            addPreferencesFromResource(R.xml.data_usage_list);
            this.mUsageAmount = findPreference("usage_amount");
            this.mChart = (ChartDataUsagePreference) findPreference("chart_data");
            this.mApps = (PreferenceGroup) findPreference("apps_group");
            Bundle args = getArguments();
            this.mSubId = args.getInt("sub_id", -1);
            this.mTemplate = args.getParcelable("network_template");
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        this.mHeader = setPinnedHeaderView(R.layout.apps_filter_spinner);
        this.mCycleSpinner = (Spinner) this.mHeader.findViewById(R.id.filter_spinner);
        this.mCycleAdapter = new CycleAdapter(getContext(), new CycleAdapter.SpinnerInterface() {
            @Override
            public void setAdapter(CycleAdapter cycleAdapter) {
                DataUsageList.this.mCycleSpinner.setAdapter((SpinnerAdapter) cycleAdapter);
            }

            @Override
            public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
                DataUsageList.this.mCycleSpinner.setOnItemSelectedListener(listener);
            }

            @Override
            public Object getSelectedItem() {
                return DataUsageList.this.mCycleSpinner.getSelectedItem();
            }

            @Override
            public void setSelection(int position) {
                DataUsageList.this.mCycleSpinner.setSelection(position);
            }
        }, this.mCycleListener, true);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBody();
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                try {
                    Thread.sleep(2000L);
                    DataUsageList.this.services.mStatsService.forceUpdate();
                    return null;
                } catch (RemoteException e) {
                    return null;
                } catch (InterruptedException e2) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(Void result) {
                if (!DataUsageList.this.isAdded()) {
                    return;
                }
                DataUsageList.this.updateBody();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    @Override
    public void onDestroy() {
        this.mUidDetailProvider.clearCache();
        this.mUidDetailProvider = null;
        TrafficStats.closeQuietly(this.mStatsSession);
        super.onDestroy();
    }

    public void updateBody() {
        SubscriptionInfo sir;
        this.mBinding = true;
        if (isAdded()) {
            Context context = getActivity();
            getLoaderManager().restartLoader(2, ChartDataLoader.buildArgs(this.mTemplate, null), this.mChartDataCallbacks);
            getActivity().invalidateOptionsMenu();
            this.mBinding = false;
            int seriesColor = context.getColor(R.color.sim_noitification);
            if (this.mSubId != -1 && (sir = this.services.mSubscriptionManager.getActiveSubscriptionInfo(this.mSubId)) != null) {
                seriesColor = sir.getIconTint();
            }
            int secondaryColor = Color.argb(127, Color.red(seriesColor), Color.green(seriesColor), Color.blue(seriesColor));
            this.mChart.setColors(seriesColor, secondaryColor);
        }
    }

    public void updatePolicy(boolean refreshCycle) {
        NetworkPolicy policy = this.services.mPolicyEditor.getPolicy(this.mTemplate);
        Log.d("DataUsage", "updatePolicy, mTemplate = " + this.mTemplate + ", polocy = " + policy);
        if (isNetworkPolicyModifiable(policy, this.mSubId) && isMobileDataAvailable(this.mSubId)) {
            this.mChart.setNetworkPolicy(policy);
            this.mHeader.findViewById(R.id.filter_settings).setVisibility(0);
            this.mHeader.findViewById(R.id.filter_settings).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle args = new Bundle();
                    args.putParcelable("network_template", DataUsageList.this.mTemplate);
                    DataUsageList.this.startFragment(DataUsageList.this, BillingCycleSettings.class.getName(), R.string.billing_cycle, 0, args);
                }
            });
        } else {
            this.mChart.setNetworkPolicy(null);
            this.mHeader.findViewById(R.id.filter_settings).setVisibility(8);
        }
        if (!refreshCycle || !this.mCycleAdapter.updateCycleList(policy, this.mChartData)) {
            return;
        }
        updateDetailData();
    }

    public void updateDetailData() {
        Log.d("DataUsage", "updateDetailData()");
        long start = this.mChart.getInspectStart();
        long end = this.mChart.getInspectEnd();
        long now = System.currentTimeMillis();
        Context context = getActivity();
        NetworkStatsHistory.Entry entry = null;
        if (this.mChartData != null) {
            entry = this.mChartData.network.getValues(start, end, now, (NetworkStatsHistory.Entry) null);
        }
        getLoaderManager().restartLoader(3, SummaryForAllUidLoader.buildArgs(this.mTemplate, start, end), this.mSummaryCallbacks);
        long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0L;
        String totalPhrase = Formatter.formatFileSize(context, totalBytes);
        this.mUsageAmount.setTitle(getString(R.string.data_used_template, new Object[]{totalPhrase}));
    }

    public void bindStats(NetworkStats stats, int[] restrictedUids) {
        int collapseKey;
        int category;
        ArrayList<AppItem> items = new ArrayList<>();
        long largest = 0;
        int currentUserId = ActivityManager.getCurrentUser();
        UserManager userManager = UserManager.get(getContext());
        List<UserHandle> profiles = userManager.getUserProfiles();
        SparseArray<AppItem> knownItems = new SparseArray<>();
        NetworkStats.Entry entry = null;
        int size = stats != null ? stats.size() : 0;
        for (int i = 0; i < size; i++) {
            entry = stats.getValues(i, entry);
            int uid = entry.uid;
            int userId = UserHandle.getUserId(uid);
            if (UserHandle.isApp(uid)) {
                if (profiles.contains(new UserHandle(userId))) {
                    if (userId != currentUserId) {
                        int managedKey = UidDetailProvider.buildKeyForUser(userId);
                        largest = accumulate(managedKey, knownItems, entry, 0, items, largest);
                    }
                    collapseKey = uid;
                    category = 2;
                } else {
                    UserInfo info = userManager.getUserInfo(userId);
                    if (info == null) {
                        collapseKey = -4;
                        category = 2;
                    } else {
                        collapseKey = UidDetailProvider.buildKeyForUser(userId);
                        category = 0;
                    }
                }
            } else if (uid == -4 || uid == -5) {
                collapseKey = uid;
                category = 2;
            } else {
                collapseKey = 1000;
                category = 2;
            }
            largest = accumulate(collapseKey, knownItems, entry, category, items, largest);
        }
        for (int uid2 : restrictedUids) {
            if (profiles.contains(new UserHandle(UserHandle.getUserId(uid2)))) {
                AppItem item = knownItems.get(uid2);
                if (item == null) {
                    item = new AppItem(uid2);
                    item.total = -1L;
                    items.add(item);
                    knownItems.put(item.key, item);
                }
                item.restricted = true;
            }
        }
        Collections.sort(items);
        this.mApps.removeAll();
        for (int i2 = 0; i2 < items.size(); i2++) {
            int percentTotal = largest != 0 ? (int) ((items.get(i2).total * 100) / largest) : 0;
            AppDataUsagePreference preference = new AppDataUsagePreference(getContext(), items.get(i2), percentTotal, this.mUidDetailProvider);
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference2) {
                    AppDataUsagePreference pref = (AppDataUsagePreference) preference2;
                    AppItem item2 = pref.getItem();
                    DataUsageList.this.startAppDataUsage(item2);
                    return true;
                }
            });
            this.mApps.addPreference(preference);
        }
    }

    public void startAppDataUsage(AppItem item) {
        Bundle args = new Bundle();
        args.putParcelable("app_item", item);
        args.putParcelable("network_template", this.mTemplate);
        startFragment(this, AppDataUsage.class.getName(), R.string.app_data_usage, 0, args);
    }

    private static long accumulate(int collapseKey, SparseArray<AppItem> knownItems, NetworkStats.Entry entry, int itemCategory, ArrayList<AppItem> items, long largest) {
        int uid = entry.uid;
        AppItem item = knownItems.get(collapseKey);
        if (item == null) {
            item = new AppItem(collapseKey);
            item.category = itemCategory;
            items.add(item);
            knownItems.put(item.key, item);
        }
        item.addUid(uid);
        item.total += entry.rxBytes + entry.txBytes;
        return Math.max(largest, item.total);
    }

    public static boolean hasReadyMobileRadio(Context context) {
        ConnectivityManager conn = ConnectivityManager.from(context);
        TelephonyManager tele = TelephonyManager.from(context);
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            Log.d("DataUsage", "hasReadyMobileRadio: subInfoList=null");
            return false;
        }
        boolean isReady = true;
        for (SubscriptionInfo subInfo : subInfoList) {
            isReady &= tele.getSimState(subInfo.getSimSlotIndex()) == 5;
            Log.d("DataUsage", "hasReadyMobileRadio: subInfo=" + subInfo);
        }
        boolean z = conn.isNetworkSupported(0) ? isReady : false;
        Log.d("DataUsage", "hasReadyMobileRadio: conn.isNetworkSupported(TYPE_MOBILE)=" + conn.isNetworkSupported(0) + " isReady=" + isReady);
        return z;
    }
}
