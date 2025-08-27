package com.android.settings.datausage;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
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
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datausage.CellDataPreference;
import com.android.settings.datausage.CycleAdapter;
import com.android.settings.widget.LoadingViewController;
import com.android.settingslib.AppItem;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoader;
import com.android.settingslib.net.SummaryForAllUidLoader;
import com.android.settingslib.net.UidDetailProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class DataUsageList extends DataUsageBase {
    private PreferenceGroup mApps;
    private ChartDataUsagePreference mChart;
    private ChartData mChartData;
    private CycleAdapter mCycleAdapter;
    private Spinner mCycleSpinner;
    private View mHeader;
    private LoadingViewController mLoadingViewController;
    private INetworkStatsSession mStatsSession;
    NetworkTemplate mTemplate;
    private UidDetailProvider mUidDetailProvider;
    private Preference mUsageAmount;
    private final CellDataPreference.DataStateListener mDataStateListener = new CellDataPreference.DataStateListener() { // from class: com.android.settings.datausage.DataUsageList.1
        @Override // android.database.ContentObserver
        public void onChange(boolean z) {
            DataUsageList.this.updatePolicy();
        }
    };
    int mSubId = -1;
    private AdapterView.OnItemSelectedListener mCycleListener = new AdapterView.OnItemSelectedListener() { // from class: com.android.settings.datausage.DataUsageList.5
        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
            CycleAdapter.CycleItem cycleItem = (CycleAdapter.CycleItem) DataUsageList.this.mCycleSpinner.getSelectedItem();
            DataUsageList.this.mChart.setVisibleRange(cycleItem.start, cycleItem.end);
            DataUsageList.this.updateDetailData();
        }

        @Override // android.widget.AdapterView.OnItemSelectedListener
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };
    private final LoaderManager.LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderManager.LoaderCallbacks<ChartData>() { // from class: com.android.settings.datausage.DataUsageList.6
        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<ChartData> onCreateLoader(int i, Bundle bundle) {
            return new ChartDataLoader(DataUsageList.this.getActivity(), DataUsageList.this.mStatsSession, bundle);
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<ChartData> loader, ChartData chartData) throws Resources.NotFoundException {
            DataUsageList.this.mLoadingViewController.showContent(false);
            DataUsageList.this.mChartData = chartData;
            DataUsageList.this.mChart.setNetworkStats(DataUsageList.this.mChartData.network);
            DataUsageList.this.updatePolicy();
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoaderReset(Loader<ChartData> loader) {
            DataUsageList.this.mChartData = null;
            DataUsageList.this.mChart.setNetworkStats(null);
        }
    };
    private final LoaderManager.LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderManager.LoaderCallbacks<NetworkStats>() { // from class: com.android.settings.datausage.DataUsageList.7
        @Override // android.app.LoaderManager.LoaderCallbacks
        public Loader<NetworkStats> onCreateLoader(int i, Bundle bundle) {
            return new SummaryForAllUidLoader(DataUsageList.this.getActivity(), DataUsageList.this.mStatsSession, bundle);
        }

        /* JADX DEBUG: Method merged with bridge method: onLoadFinished(Landroid/content/Loader;Ljava/lang/Object;)V */
        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats networkStats) {
            DataUsageList.this.bindStats(networkStats, DataUsageList.this.services.mPolicyManager.getUidsWithPolicy(1));
            updateEmptyVisible();
        }

        @Override // android.app.LoaderManager.LoaderCallbacks
        public void onLoaderReset(Loader<NetworkStats> loader) {
            DataUsageList.this.bindStats(null, new int[0]);
            updateEmptyVisible();
        }

        private void updateEmptyVisible() {
            if ((DataUsageList.this.mApps.getPreferenceCount() != 0) != (DataUsageList.this.getPreferenceScreen().getPreferenceCount() != 0)) {
                if (DataUsageList.this.mApps.getPreferenceCount() != 0) {
                    DataUsageList.this.getPreferenceScreen().addPreference(DataUsageList.this.mUsageAmount);
                    DataUsageList.this.getPreferenceScreen().addPreference(DataUsageList.this.mApps);
                } else {
                    DataUsageList.this.getPreferenceScreen().removeAll();
                }
            }
        }
    };

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 341;
    }

    @Override // com.android.settings.datausage.DataUsageBase, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        if (!isBandwidthControlEnabled()) {
            Log.w("DataUsage", "No bandwidth control; leaving");
            getActivity().finish();
        }
        try {
            this.mStatsSession = this.services.mStatsService.openSession();
            this.mUidDetailProvider = new UidDetailProvider(activity);
            addPreferencesFromResource(R.xml.data_usage_list);
            this.mUsageAmount = findPreference("usage_amount");
            this.mChart = (ChartDataUsagePreference) findPreference("chart_data");
            this.mApps = (PreferenceGroup) findPreference("apps_group");
            processArgument();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override // android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mHeader = setPinnedHeaderView(R.layout.apps_filter_spinner);
        this.mHeader.findViewById(R.id.filter_settings).setOnClickListener(new View.OnClickListener() { // from class: com.android.settings.datausage.-$$Lambda$DataUsageList$YolaBauY8HvHsYGl5vfnCCKHiAQ
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                DataUsageList.lambda$onViewCreated$0(this.f$0, view2);
            }
        });
        this.mCycleSpinner = (Spinner) this.mHeader.findViewById(R.id.filter_spinner);
        this.mCycleAdapter = new CycleAdapter(this.mCycleSpinner.getContext(), new CycleAdapter.SpinnerInterface() { // from class: com.android.settings.datausage.DataUsageList.2
            @Override // com.android.settings.datausage.CycleAdapter.SpinnerInterface
            public void setAdapter(CycleAdapter cycleAdapter) {
                DataUsageList.this.mCycleSpinner.setAdapter((SpinnerAdapter) cycleAdapter);
            }

            @Override // com.android.settings.datausage.CycleAdapter.SpinnerInterface
            public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener) {
                DataUsageList.this.mCycleSpinner.setOnItemSelectedListener(onItemSelectedListener);
            }

            @Override // com.android.settings.datausage.CycleAdapter.SpinnerInterface
            public Object getSelectedItem() {
                return DataUsageList.this.mCycleSpinner.getSelectedItem();
            }

            @Override // com.android.settings.datausage.CycleAdapter.SpinnerInterface
            public void setSelection(int i) {
                DataUsageList.this.mCycleSpinner.setSelection(i);
            }
        }, this.mCycleListener, true);
        this.mLoadingViewController = new LoadingViewController(getView().findViewById(R.id.loading_container), getListView());
        this.mLoadingViewController.showLoadingViewDelayed();
    }

    public static /* synthetic */ void lambda$onViewCreated$0(DataUsageList dataUsageList, View view) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("network_template", dataUsageList.mTemplate);
        new SubSettingLauncher(dataUsageList.getContext()).setDestination(BillingCycleSettings.class.getName()).setTitle(R.string.billing_cycle).setSourceMetricsCategory(dataUsageList.getMetricsCategory()).setArguments(bundle).launch();
    }

    /* JADX WARN: Type inference failed for: r0v1, types: [com.android.settings.datausage.DataUsageList$3] */
    @Override // com.android.settings.datausage.DataUsageBase, com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        this.mDataStateListener.setListener(true, this.mSubId, getContext());
        updateBody();
        new AsyncTask<Void, Void, Void>() { // from class: com.android.settings.datausage.DataUsageList.3
            /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
            @Override // android.os.AsyncTask
            protected Void doInBackground(Void... voidArr) throws InterruptedException {
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

            /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
            @Override // android.os.AsyncTask
            protected void onPostExecute(Void r1) {
                if (DataUsageList.this.isAdded()) {
                    DataUsageList.this.updateBody();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        this.mDataStateListener.setListener(false, this.mSubId, getContext());
    }

    @Override // com.android.settings.datausage.DataUsageBase, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onDestroy() {
        this.mUidDetailProvider.clearCache();
        this.mUidDetailProvider = null;
        TrafficStats.closeQuietly(this.mStatsSession);
        super.onDestroy();
    }

    void processArgument() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            this.mSubId = arguments.getInt("sub_id", -1);
            this.mTemplate = arguments.getParcelable("network_template");
        }
        if (this.mTemplate == null && this.mSubId == -1) {
            Intent intent = getIntent();
            this.mSubId = intent.getIntExtra("android.provider.extra.SUB_ID", -1);
            this.mTemplate = intent.getParcelableExtra("network_template");
        }
    }

    private void updateBody() {
        SubscriptionInfo activeSubscriptionInfo;
        if (isAdded()) {
            Activity activity = getActivity();
            getLoaderManager().restartLoader(2, ChartDataLoader.buildArgs(this.mTemplate, null), this.mChartDataCallbacks);
            getActivity().invalidateOptionsMenu();
            int color = activity.getColor(R.color.sim_noitification);
            if (this.mSubId != -1 && (activeSubscriptionInfo = this.services.mSubscriptionManager.getActiveSubscriptionInfo(this.mSubId)) != null) {
                color = activeSubscriptionInfo.getIconTint();
            }
            this.mChart.setColors(color, Color.argb(127, Color.red(color), Color.green(color), Color.blue(color)));
        }
    }

    private void updatePolicy() {
        NetworkPolicy policy = this.services.mPolicyEditor.getPolicy(this.mTemplate);
        View viewFindViewById = this.mHeader.findViewById(R.id.filter_settings);
        if (isNetworkPolicyModifiable(policy, this.mSubId) && isMobileDataAvailable(this.mSubId)) {
            this.mChart.setNetworkPolicy(policy);
            viewFindViewById.setVisibility(0);
        } else {
            this.mChart.setNetworkPolicy(null);
            viewFindViewById.setVisibility(8);
        }
        if (this.mCycleAdapter.updateCycleList(policy, this.mChartData)) {
            updateDetailData();
        }
    }

    private void updateDetailData() {
        NetworkStatsHistory.Entry values;
        long inspectStart = this.mChart.getInspectStart();
        long inspectEnd = this.mChart.getInspectEnd();
        long jCurrentTimeMillis = System.currentTimeMillis();
        Activity activity = getActivity();
        if (this.mChartData != null) {
            values = this.mChartData.network.getValues(inspectStart, inspectEnd, jCurrentTimeMillis, (NetworkStatsHistory.Entry) null);
        } else {
            values = null;
        }
        getLoaderManager().restartLoader(3, SummaryForAllUidLoader.buildArgs(this.mTemplate, inspectStart, inspectEnd), this.mSummaryCallbacks);
        this.mUsageAmount.setTitle(getString(R.string.data_used_template, new Object[]{DataUsageUtils.formatDataUsage(activity, values != null ? values.rxBytes + values.txBytes : 0L)}));
    }

    public void bindStats(NetworkStats networkStats, int[] iArr) {
        int i;
        NetworkStats.Entry entry;
        int i2;
        int i3;
        int i4;
        ArrayList arrayList = new ArrayList();
        int currentUser = ActivityManager.getCurrentUser();
        UserManager userManager = UserManager.get(getContext());
        List<UserHandle> userProfiles = userManager.getUserProfiles();
        SparseArray sparseArray = new SparseArray();
        NetworkStats.Entry entry2 = null;
        long jAccumulate = 0;
        int i5 = 0;
        for (int size = networkStats != null ? networkStats.size() : 0; i5 < size; size = i2) {
            NetworkStats.Entry values = networkStats.getValues(i5, entry2);
            int i6 = values.uid;
            int userId = UserHandle.getUserId(i6);
            int iBuildKeyForUser = -4;
            int i7 = 2;
            if (UserHandle.isApp(i6)) {
                if (userProfiles.contains(new UserHandle(userId))) {
                    if (userId != currentUser) {
                        i = i6;
                        entry = values;
                        i2 = size;
                        i3 = i5;
                        jAccumulate = accumulate(UidDetailProvider.buildKeyForUser(userId), sparseArray, values, 0, arrayList, jAccumulate);
                    } else {
                        i = i6;
                        entry = values;
                        i2 = size;
                        i3 = i5;
                    }
                } else {
                    entry = values;
                    i2 = size;
                    i3 = i5;
                    if (userManager.getUserInfo(userId) != null) {
                        iBuildKeyForUser = UidDetailProvider.buildKeyForUser(userId);
                        i7 = 0;
                    }
                    i4 = iBuildKeyForUser;
                }
            } else {
                i = i6;
                entry = values;
                i2 = size;
                i3 = i5;
                i4 = (i == -4 || i == -5) ? i : 1000;
            }
            jAccumulate = accumulate(i4, sparseArray, entry, i7, arrayList, jAccumulate);
            i5 = i3 + 1;
            entry2 = entry;
        }
        for (int i8 : iArr) {
            if (userProfiles.contains(new UserHandle(UserHandle.getUserId(i8)))) {
                AppItem appItem = (AppItem) sparseArray.get(i8);
                if (appItem == null) {
                    appItem = new AppItem(i8);
                    appItem.total = -1L;
                    arrayList.add(appItem);
                    sparseArray.put(appItem.key, appItem);
                }
                appItem.restricted = true;
            }
        }
        Collections.sort(arrayList);
        this.mApps.removeAll();
        for (int i9 = 0; i9 < arrayList.size(); i9++) {
            AppDataUsagePreference appDataUsagePreference = new AppDataUsagePreference(getContext(), (AppItem) arrayList.get(i9), jAccumulate != 0 ? (int) ((((AppItem) arrayList.get(i9)).total * 100) / jAccumulate) : 0, this.mUidDetailProvider);
            appDataUsagePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() { // from class: com.android.settings.datausage.DataUsageList.4
                @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
                public boolean onPreferenceClick(Preference preference) {
                    DataUsageList.this.startAppDataUsage(((AppDataUsagePreference) preference).getItem());
                    return true;
                }
            });
            this.mApps.addPreference(appDataUsagePreference);
        }
    }

    private void startAppDataUsage(AppItem appItem) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("app_item", appItem);
        bundle.putParcelable("network_template", this.mTemplate);
        new SubSettingLauncher(getContext()).setDestination(AppDataUsage.class.getName()).setTitle(R.string.app_data_usage).setArguments(bundle).setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    private static long accumulate(int i, SparseArray<AppItem> sparseArray, NetworkStats.Entry entry, int i2, ArrayList<AppItem> arrayList, long j) {
        int i3 = entry.uid;
        AppItem appItem = sparseArray.get(i);
        if (appItem == null) {
            appItem = new AppItem(i);
            appItem.category = i2;
            arrayList.add(appItem);
            sparseArray.put(appItem.key, appItem);
        }
        appItem.addUid(i3);
        appItem.total += entry.rxBytes + entry.txBytes;
        return Math.max(j, appItem.total);
    }

    public static boolean hasReadyMobileRadio(Context context) {
        ConnectivityManager connectivityManagerFrom = ConnectivityManager.from(context);
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(context);
        List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList == null) {
            return false;
        }
        Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
        boolean z = true;
        while (it.hasNext()) {
            z &= telephonyManagerFrom.getSimState(it.next().getSimSlotIndex()) == 5;
        }
        return connectivityManagerFrom.isNetworkSupported(0) && z;
    }
}
