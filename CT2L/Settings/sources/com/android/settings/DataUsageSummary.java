package com.android.settings;

import android.animation.LayoutTransition;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkPolicyManager;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import com.android.internal.util.Preconditions;
import com.android.settings.drawable.InsetBoundsDrawable;
import com.android.settings.net.ChartData;
import com.android.settings.net.ChartDataLoader;
import com.android.settings.net.DataUsageMeteredSettings;
import com.android.settings.net.NetworkPolicyEditor;
import com.android.settings.net.SummaryForAllUidLoader;
import com.android.settings.net.UidDetail;
import com.android.settings.net.UidDetailProvider;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.ChartDataUsageView;
import com.android.settings.widget.ChartNetworkSeriesView;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import libcore.util.Objects;

public class DataUsageSummary extends HighlightingFragment implements Indexable {
    private DataUsageAdapter mAdapter;
    private TextView mAppBackground;
    private View mAppDetail;
    private TextView mAppForeground;
    private ImageView mAppIcon;
    private Switch mAppRestrict;
    private View mAppRestrictView;
    private Button mAppSettings;
    private Intent mAppSettingsIntent;
    private LinearLayout mAppSwitches;
    private ViewGroup mAppTitles;
    private TextView mAppTotal;
    private boolean mBinding;
    private ChartDataUsageView mChart;
    private ChartData mChartData;
    private CycleAdapter mCycleAdapter;
    private Spinner mCycleSpinner;
    private TextView mCycleSummary;
    private View mCycleView;
    private Switch mDataEnabled;
    private boolean mDataEnabledSupported;
    private View mDataEnabledView;
    private ChartNetworkSeriesView mDetailedSeries;
    private Switch mDisableAtLimit;
    private boolean mDisableAtLimitSupported;
    private View mDisableAtLimitView;
    private View mDisclaimer;
    private TextView mEmpty;
    private ViewGroup mHeader;
    private ListView mListView;
    private MenuItem mMenuCellularNetworks;
    private MenuItem mMenuRestrictBackground;
    private MenuItem mMenuShowEthernet;
    private MenuItem mMenuShowWifi;
    private MenuItem mMenuSimCards;
    private Map<Integer, String> mMobileTagMap;
    private INetworkManagementService mNetworkService;
    private LinearLayout mNetworkSwitches;
    private ViewGroup mNetworkSwitchesContainer;
    private NetworkPolicyEditor mPolicyEditor;
    private NetworkPolicyManager mPolicyManager;
    private SharedPreferences mPrefs;
    private ChartNetworkSeriesView mSeries;
    private INetworkStatsService mStatsService;
    private INetworkStatsSession mStatsSession;
    private View mStupidPadding;
    private List<SubscriptionInfo> mSubInfoList;
    private SubscriptionManager mSubscriptionManager;
    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private ViewGroup mTabsContainer;
    private TelephonyManager mTelephonyManager;
    private NetworkTemplate mTemplate;
    private UidDetailProvider mUidDetailProvider;
    private static final StringBuilder sBuilder = new StringBuilder(50);
    private static final Formatter sFormatter = new Formatter(sBuilder, Locale.getDefault());
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> result = new ArrayList<>();
            Resources res = context.getResources();
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.data_usage_summary_title);
            data.screenTitle = res.getString(R.string.data_usage_summary_title);
            result.add(data);
            SearchIndexableRaw data2 = new SearchIndexableRaw(context);
            data2.key = "data_usage_enable_mobile";
            data2.title = res.getString(R.string.data_usage_enable_mobile);
            data2.screenTitle = res.getString(R.string.data_usage_summary_title);
            result.add(data2);
            SearchIndexableRaw data3 = new SearchIndexableRaw(context);
            data3.key = "data_usage_disable_mobile_limit";
            data3.title = res.getString(R.string.data_usage_disable_mobile_limit);
            data3.screenTitle = res.getString(R.string.data_usage_summary_title);
            result.add(data3);
            SearchIndexableRaw data4 = new SearchIndexableRaw(context);
            data4.key = "data_usage_cycle";
            data4.title = res.getString(R.string.data_usage_cycle);
            data4.screenTitle = res.getString(R.string.data_usage_summary_title);
            result.add(data4);
            return result;
        }
    };
    private int mInsetSide = 0;
    private boolean mShowWifi = false;
    private boolean mShowEthernet = false;
    private AppItem mCurrentApp = null;
    private String mCurrentTab = null;
    private String mIntentTab = null;
    private final Map<String, Boolean> mMobileDataEnabled = new HashMap();
    private TabHost.TabContentFactory mEmptyTabContent = new TabHost.TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(DataUsageSummary.this.mTabHost.getContext());
        }
    };
    private TabHost.OnTabChangeListener mTabListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            DataUsageSummary.this.updateBody();
        }
    };
    private View.OnClickListener mDataEnabledListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!DataUsageSummary.this.mBinding) {
                boolean dataEnabled = !DataUsageSummary.this.mDataEnabled.isChecked();
                String currentTab = DataUsageSummary.this.mCurrentTab;
                if (DataUsageSummary.isMobileTab(currentTab)) {
                    if (!dataEnabled) {
                        ConfirmDataDisableFragment.show(DataUsageSummary.this, DataUsageSummary.this.getSubId(DataUsageSummary.this.mCurrentTab));
                    } else if (Utils.showSimCardTile(DataUsageSummary.this.getActivity())) {
                        DataUsageSummary.this.handleMultiSimDataDialog();
                    } else {
                        DataUsageSummary.this.setMobileDataEnabled(DataUsageSummary.this.getSubId(currentTab), true);
                    }
                }
                DataUsageSummary.this.updatePolicy(false);
            }
        }
    };
    private View.OnClickListener mDisableAtLimitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean disableAtLimit = !DataUsageSummary.this.mDisableAtLimit.isChecked();
            if (!disableAtLimit) {
                DataUsageSummary.this.setPolicyLimitBytes(-1L);
            } else {
                ConfirmLimitFragment.show(DataUsageSummary.this);
            }
        }
    };
    private View.OnClickListener mAppRestrictListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            boolean restrictBackground = !DataUsageSummary.this.mAppRestrict.isChecked();
            if (!restrictBackground) {
                DataUsageSummary.this.setAppRestrictBackground(false);
            } else {
                ConfirmAppRestrictFragment.show(DataUsageSummary.this);
            }
        }
    };
    private AdapterView.OnItemClickListener mListListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            view.getContext();
            AppItem app = (AppItem) parent.getItemAtPosition(position);
            if (DataUsageSummary.this.mUidDetailProvider != null && app != null) {
                UidDetail detail = DataUsageSummary.this.mUidDetailProvider.getUidDetail(app.key, true);
                AppDetailsFragment.show(DataUsageSummary.this, app, detail.label);
            }
        }
    };
    private AdapterView.OnItemSelectedListener mCycleListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            CycleItem cycle = (CycleItem) parent.getItemAtPosition(position);
            if (!(cycle instanceof CycleChangeItem)) {
                DataUsageSummary.this.mChart.setVisibleRange(cycle.start, cycle.end);
                DataUsageSummary.this.updateDetailData();
            } else {
                CycleEditorFragment.show(DataUsageSummary.this);
                DataUsageSummary.this.mCycleSpinner.setSelection(0);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };
    private final LoaderManager.LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderManager.LoaderCallbacks<ChartData>() {
        @Override
        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
            return new ChartDataLoader(DataUsageSummary.this.getActivity(), DataUsageSummary.this.mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
            DataUsageSummary.this.mChartData = data;
            DataUsageSummary.this.mChart.bindNetworkStats(DataUsageSummary.this.mChartData.network);
            DataUsageSummary.this.mChart.bindDetailNetworkStats(DataUsageSummary.this.mChartData.detail);
            DataUsageSummary.this.updatePolicy(true);
            DataUsageSummary.this.updateAppDetail();
            if (DataUsageSummary.this.mChartData.detail != null) {
                DataUsageSummary.this.mListView.smoothScrollToPosition(0);
            }
        }

        @Override
        public void onLoaderReset(Loader<ChartData> loader) {
            DataUsageSummary.this.mChartData = null;
            DataUsageSummary.this.mChart.bindNetworkStats(null);
            DataUsageSummary.this.mChart.bindDetailNetworkStats(null);
        }
    };
    private final LoaderManager.LoaderCallbacks<NetworkStats> mSummaryCallbacks = new LoaderManager.LoaderCallbacks<NetworkStats>() {
        @Override
        public Loader<NetworkStats> onCreateLoader(int id, Bundle args) {
            return new SummaryForAllUidLoader(DataUsageSummary.this.getActivity(), DataUsageSummary.this.mStatsSession, args);
        }

        @Override
        public void onLoadFinished(Loader<NetworkStats> loader, NetworkStats data) {
            int[] restrictedUids = DataUsageSummary.this.mPolicyManager.getUidsWithPolicy(1);
            DataUsageSummary.this.mAdapter.bindStats(data, restrictedUids);
            updateEmptyVisible();
        }

        @Override
        public void onLoaderReset(Loader<NetworkStats> loader) {
            DataUsageSummary.this.mAdapter.bindStats(null, new int[0]);
            updateEmptyVisible();
        }

        private void updateEmptyVisible() {
            boolean isEmpty = DataUsageSummary.this.mAdapter.isEmpty() && !DataUsageSummary.this.isAppDetailMode();
            DataUsageSummary.this.mEmpty.setVisibility(isEmpty ? 0 : 8);
            DataUsageSummary.this.mStupidPadding.setVisibility(isEmpty ? 0 : 8);
        }
    };
    private ChartDataUsageView.DataUsageChartListener mChartListener = new ChartDataUsageView.DataUsageChartListener() {
        @Override
        public void onWarningChanged() {
            DataUsageSummary.this.setPolicyWarningBytes(DataUsageSummary.this.mChart.getWarningBytes());
        }

        @Override
        public void onLimitChanged() {
            DataUsageSummary.this.setPolicyLimitBytes(DataUsageSummary.this.mChart.getLimitBytes());
        }

        @Override
        public void requestWarningEdit() {
            WarningEditorFragment.show(DataUsageSummary.this);
        }

        @Override
        public void requestLimitEdit() {
            LimitEditorFragment.show(DataUsageSummary.this);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity();
        this.mNetworkService = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        this.mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
        this.mPolicyManager = NetworkPolicyManager.from(context);
        this.mTelephonyManager = TelephonyManager.from(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mPrefs = getActivity().getSharedPreferences("data_usage", 0);
        this.mPolicyEditor = new NetworkPolicyEditor(this.mPolicyManager);
        this.mPolicyEditor.read();
        this.mSubInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        this.mMobileTagMap = initMobileTabTag(this.mSubInfoList);
        try {
            if (!this.mNetworkService.isBandwidthControlEnabled()) {
                Log.w("DataUsage", "No bandwidth control; leaving");
                getActivity().finish();
            }
        } catch (RemoteException e) {
            Log.w("DataUsage", "No bandwidth control; leaving");
            getActivity().finish();
        }
        try {
            this.mStatsSession = this.mStatsService.openSession();
            this.mShowWifi = this.mPrefs.getBoolean("show_wifi", false);
            this.mShowEthernet = this.mPrefs.getBoolean("show_ethernet", false);
            if (!hasReadyMobileRadio(context)) {
                this.mShowWifi = true;
                this.mShowEthernet = true;
            }
            setHasOptionsMenu(true);
        } catch (RemoteException e2) {
            throw new RuntimeException(e2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        View view = inflater.inflate(R.layout.data_usage_summary, container, false);
        this.mUidDetailProvider = new UidDetailProvider(context);
        this.mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        this.mTabsContainer = (ViewGroup) view.findViewById(R.id.tabs_container);
        this.mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
        this.mListView = (ListView) view.findViewById(android.R.id.list);
        if (this.mListView.getScrollBarStyle() == 33554432) {
        }
        this.mInsetSide = 0;
        Utils.prepareCustomPreferencesList(container, view, this.mListView, false);
        this.mTabHost.setup();
        this.mTabHost.setOnTabChangedListener(this.mTabListener);
        this.mHeader = (ViewGroup) inflater.inflate(R.layout.data_usage_header, (ViewGroup) this.mListView, false);
        this.mHeader.setClickable(true);
        this.mListView.addHeaderView(new View(context), null, true);
        this.mListView.addHeaderView(this.mHeader, null, true);
        this.mListView.setItemsCanFocus(true);
        if (this.mInsetSide > 0) {
            insetListViewDrawables(this.mListView, this.mInsetSide);
            this.mHeader.setPaddingRelative(this.mInsetSide, 0, this.mInsetSide, 0);
        }
        this.mNetworkSwitchesContainer = (ViewGroup) this.mHeader.findViewById(R.id.network_switches_container);
        this.mNetworkSwitches = (LinearLayout) this.mHeader.findViewById(R.id.network_switches);
        this.mDataEnabled = new Switch(inflater.getContext());
        this.mDataEnabled.setClickable(false);
        this.mDataEnabled.setFocusable(false);
        this.mDataEnabledView = inflatePreference(inflater, this.mNetworkSwitches, this.mDataEnabled);
        this.mDataEnabledView.setTag(R.id.preference_highlight_key, "data_usage_enable_mobile");
        this.mDataEnabledView.setClickable(true);
        this.mDataEnabledView.setFocusable(true);
        this.mDataEnabledView.setOnClickListener(this.mDataEnabledListener);
        this.mNetworkSwitches.addView(this.mDataEnabledView);
        this.mDisableAtLimit = new Switch(inflater.getContext());
        this.mDisableAtLimit.setClickable(false);
        this.mDisableAtLimit.setFocusable(false);
        this.mDisableAtLimitView = inflatePreference(inflater, this.mNetworkSwitches, this.mDisableAtLimit);
        this.mDisableAtLimitView.setTag(R.id.preference_highlight_key, "data_usage_disable_mobile_limit");
        this.mDisableAtLimitView.setClickable(true);
        this.mDisableAtLimitView.setFocusable(true);
        this.mDisableAtLimitView.setOnClickListener(this.mDisableAtLimitListener);
        this.mNetworkSwitches.addView(this.mDisableAtLimitView);
        this.mCycleView = inflater.inflate(R.layout.data_usage_cycles, (ViewGroup) this.mNetworkSwitches, false);
        this.mCycleView.setTag(R.id.preference_highlight_key, "data_usage_cycle");
        this.mCycleSpinner = (Spinner) this.mCycleView.findViewById(R.id.cycles_spinner);
        this.mCycleAdapter = new CycleAdapter(context);
        this.mCycleSpinner.setAdapter((SpinnerAdapter) this.mCycleAdapter);
        this.mCycleSpinner.setOnItemSelectedListener(this.mCycleListener);
        this.mCycleSummary = (TextView) this.mCycleView.findViewById(R.id.cycle_summary);
        this.mNetworkSwitches.addView(this.mCycleView);
        this.mSeries = (ChartNetworkSeriesView) view.findViewById(R.id.series);
        this.mDetailedSeries = (ChartNetworkSeriesView) view.findViewById(R.id.detail_series);
        this.mChart = (ChartDataUsageView) this.mHeader.findViewById(R.id.chart);
        this.mChart.setListener(this.mChartListener);
        this.mChart.bindNetworkPolicy(null);
        this.mAppDetail = this.mHeader.findViewById(R.id.app_detail);
        this.mAppIcon = (ImageView) this.mAppDetail.findViewById(R.id.app_icon);
        this.mAppTitles = (ViewGroup) this.mAppDetail.findViewById(R.id.app_titles);
        this.mAppForeground = (TextView) this.mAppDetail.findViewById(R.id.app_foreground);
        this.mAppBackground = (TextView) this.mAppDetail.findViewById(R.id.app_background);
        this.mAppSwitches = (LinearLayout) this.mAppDetail.findViewById(R.id.app_switches);
        this.mAppSettings = (Button) this.mAppDetail.findViewById(R.id.app_settings);
        this.mAppRestrict = new Switch(inflater.getContext());
        this.mAppRestrict.setClickable(false);
        this.mAppRestrict.setFocusable(false);
        this.mAppRestrictView = inflatePreference(inflater, this.mAppSwitches, this.mAppRestrict);
        this.mAppRestrictView.setClickable(true);
        this.mAppRestrictView.setFocusable(true);
        this.mAppRestrictView.setOnClickListener(this.mAppRestrictListener);
        this.mAppSwitches.addView(this.mAppRestrictView);
        this.mDisclaimer = this.mHeader.findViewById(R.id.disclaimer);
        this.mEmpty = (TextView) this.mHeader.findViewById(android.R.id.empty);
        this.mStupidPadding = this.mHeader.findViewById(R.id.stupid_padding);
        UserManager um = (UserManager) context.getSystemService("user");
        this.mAdapter = new DataUsageAdapter(um, this.mUidDetailProvider, this.mInsetSide);
        this.mListView.setOnItemClickListener(this.mListListener);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        return view;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        Intent intent = getActivity().getIntent();
        this.mIntentTab = computeTabFromIntent(intent);
        updateTabs();
    }

    @Override
    public void onResume() {
        super.onResume();
        getView().post(new Runnable() {
            @Override
            public void run() {
                DataUsageSummary.this.highlightViewIfNeeded();
            }
        });
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... params) {
                try {
                    Thread.sleep(2000L);
                    DataUsageSummary.this.mStatsService.forceUpdate();
                    return null;
                } catch (RemoteException e) {
                    return null;
                } catch (InterruptedException e2) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(Void result) {
                if (DataUsageSummary.this.isAdded()) {
                    DataUsageSummary.this.updateBody();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.data_usage, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Context context = getActivity();
        boolean appDetailMode = isAppDetailMode();
        boolean isOwner = ActivityManager.getCurrentUser() == 0;
        this.mMenuShowWifi = menu.findItem(R.id.data_usage_menu_show_wifi);
        if (hasWifiRadio(context) && hasReadyMobileRadio(context)) {
            this.mMenuShowWifi.setVisible(!appDetailMode);
        } else {
            this.mMenuShowWifi.setVisible(false);
        }
        this.mMenuShowEthernet = menu.findItem(R.id.data_usage_menu_show_ethernet);
        if (hasEthernet(context) && hasReadyMobileRadio(context)) {
            this.mMenuShowEthernet.setVisible(!appDetailMode);
        } else {
            this.mMenuShowEthernet.setVisible(false);
        }
        this.mMenuRestrictBackground = menu.findItem(R.id.data_usage_menu_restrict_background);
        this.mMenuRestrictBackground.setVisible(hasReadyMobileRadio(context) && isOwner && !appDetailMode);
        MenuItem metered = menu.findItem(R.id.data_usage_menu_metered);
        if (hasReadyMobileRadio(context)) {
            metered.setVisible(!appDetailMode);
        } else {
            metered.setVisible(false);
        }
        this.mMenuSimCards = menu.findItem(R.id.data_usage_menu_sim_cards);
        this.mMenuSimCards.setVisible(false);
        this.mMenuCellularNetworks = menu.findItem(R.id.data_usage_menu_cellular_networks);
        this.mMenuCellularNetworks.setVisible(hasReadyMobileRadio(context) && !appDetailMode && isOwner);
        MenuItem help = menu.findItem(R.id.data_usage_menu_help);
        String helpUrl = getResources().getString(R.string.help_url_data_usage);
        if (!TextUtils.isEmpty(helpUrl)) {
            HelpUtils.prepareHelpMenuItem(context, help, helpUrl);
        } else {
            help.setVisible(false);
        }
        updateMenuTitles();
    }

    private void updateMenuTitles() {
        if (this.mPolicyManager.getRestrictBackground()) {
            this.mMenuRestrictBackground.setTitle(R.string.data_usage_menu_allow_background);
        } else {
            this.mMenuRestrictBackground.setTitle(R.string.data_usage_menu_restrict_background);
        }
        if (this.mShowWifi) {
            this.mMenuShowWifi.setTitle(R.string.data_usage_menu_hide_wifi);
        } else {
            this.mMenuShowWifi.setTitle(R.string.data_usage_menu_show_wifi);
        }
        if (this.mShowEthernet) {
            this.mMenuShowEthernet.setTitle(R.string.data_usage_menu_hide_ethernet);
        } else {
            this.mMenuShowEthernet.setTitle(R.string.data_usage_menu_show_ethernet);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.data_usage_menu_restrict_background:
                boolean restrictBackground = !this.mPolicyManager.getRestrictBackground();
                if (restrictBackground) {
                    ConfirmRestrictFragment.show(this);
                    return true;
                }
                setRestrictBackground(false);
                return true;
            case R.id.data_usage_menu_show_wifi:
                this.mShowWifi = this.mShowWifi ? false : true;
                this.mPrefs.edit().putBoolean("show_wifi", this.mShowWifi).apply();
                updateMenuTitles();
                updateTabs();
                return true;
            case R.id.data_usage_menu_show_ethernet:
                this.mShowEthernet = this.mShowEthernet ? false : true;
                this.mPrefs.edit().putBoolean("show_ethernet", this.mShowEthernet).apply();
                updateMenuTitles();
                updateTabs();
                return true;
            case R.id.data_usage_menu_metered:
                SettingsActivity sa = (SettingsActivity) getActivity();
                sa.startPreferencePanel(DataUsageMeteredSettings.class.getCanonicalName(), null, R.string.data_usage_metered_title, null, this, 0);
                return true;
            case R.id.data_usage_menu_sim_cards:
                return true;
            case R.id.data_usage_menu_cellular_networks:
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setComponent(new ComponentName("com.android.phone", "com.android.phone.MobileNetworkSettings"));
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroy() {
        this.mDataEnabledView = null;
        this.mDisableAtLimitView = null;
        this.mUidDetailProvider.clearCache();
        this.mUidDetailProvider = null;
        TrafficStats.closeQuietly(this.mStatsSession);
        super.onDestroy();
    }

    private void ensureLayoutTransitions() {
        if (this.mChart.getLayoutTransition() == null) {
            this.mTabsContainer.setLayoutTransition(buildLayoutTransition());
            this.mHeader.setLayoutTransition(buildLayoutTransition());
            this.mNetworkSwitchesContainer.setLayoutTransition(buildLayoutTransition());
            LayoutTransition chartTransition = buildLayoutTransition();
            chartTransition.disableTransitionType(2);
            chartTransition.disableTransitionType(3);
            this.mChart.setLayoutTransition(chartTransition);
        }
    }

    private static LayoutTransition buildLayoutTransition() {
        LayoutTransition transition = new LayoutTransition();
        transition.setAnimateParentHierarchy(false);
        return transition;
    }

    private void updateTabs() {
        Context context = getActivity();
        this.mTabHost.clearAllTabs();
        int simCount = this.mTelephonyManager.getSimCount();
        for (int i = 0; i < simCount; i++) {
            SubscriptionInfo sir = Utils.findRecordBySlotId(context, i);
            if (sir != null) {
                addMobileTab(context, sir, simCount > 1);
            }
        }
        if (this.mShowWifi && hasWifiRadio(context)) {
            this.mTabHost.addTab(buildTabSpec("wifi", R.string.data_usage_tab_wifi));
        }
        if (this.mShowEthernet && hasEthernet(context)) {
            this.mTabHost.addTab(buildTabSpec("ethernet", R.string.data_usage_tab_ethernet));
        }
        boolean noTabs = this.mTabWidget.getTabCount() == 0;
        boolean multipleTabs = this.mTabWidget.getTabCount() > 1;
        this.mTabWidget.setVisibility(multipleTabs ? 0 : 8);
        if (this.mIntentTab != null) {
            if (Objects.equal(this.mIntentTab, this.mTabHost.getCurrentTabTag())) {
                updateBody();
            } else {
                this.mTabHost.setCurrentTabByTag(this.mIntentTab);
            }
            this.mIntentTab = null;
            return;
        }
        if (noTabs) {
            updateBody();
        }
    }

    private TabHost.TabSpec buildTabSpec(String tag, int titleRes) {
        return this.mTabHost.newTabSpec(tag).setIndicator(getText(titleRes)).setContent(this.mEmptyTabContent);
    }

    private TabHost.TabSpec buildTabSpec(String tag, CharSequence title) {
        return this.mTabHost.newTabSpec(tag).setIndicator(title).setContent(this.mEmptyTabContent);
    }

    public void updateBody() {
        this.mBinding = true;
        if (isAdded()) {
            Context context = getActivity();
            Resources resources = context.getResources();
            String currentTab = this.mTabHost.getCurrentTabTag();
            boolean isOwner = ActivityManager.getCurrentUser() == 0;
            if (currentTab == null) {
                Log.w("DataUsage", "no tab selected; hiding body");
                this.mListView.setVisibility(8);
                return;
            }
            this.mListView.setVisibility(0);
            this.mCurrentTab = currentTab;
            this.mDataEnabledSupported = isOwner;
            this.mDisableAtLimitSupported = true;
            if (isMobileTab(currentTab)) {
                setPreferenceTitle(this.mDataEnabledView, R.string.data_usage_enable_mobile);
                setPreferenceTitle(this.mDisableAtLimitView, R.string.data_usage_disable_mobile_limit);
                this.mDataEnabledSupported = isMobileDataAvailable(getSubId(currentTab));
                this.mTemplate = NetworkTemplate.buildTemplateMobileAll(getActiveSubscriberId(context, getSubId(currentTab)));
                this.mTemplate = NetworkTemplate.normalize(this.mTemplate, this.mTelephonyManager.getMergedSubscriberIds());
            } else if ("3g".equals(currentTab)) {
                setPreferenceTitle(this.mDataEnabledView, R.string.data_usage_enable_3g);
                setPreferenceTitle(this.mDisableAtLimitView, R.string.data_usage_disable_3g_limit);
                this.mTemplate = NetworkTemplate.buildTemplateMobile3gLower(getActiveSubscriberId(context));
            } else if ("4g".equals(currentTab)) {
                setPreferenceTitle(this.mDataEnabledView, R.string.data_usage_enable_4g);
                setPreferenceTitle(this.mDisableAtLimitView, R.string.data_usage_disable_4g_limit);
                this.mTemplate = NetworkTemplate.buildTemplateMobile4g(getActiveSubscriberId(context));
            } else if ("wifi".equals(currentTab)) {
                this.mDataEnabledSupported = false;
                this.mDisableAtLimitSupported = false;
                this.mTemplate = NetworkTemplate.buildTemplateWifiWildcard();
            } else if ("ethernet".equals(currentTab)) {
                this.mDataEnabledSupported = false;
                this.mDisableAtLimitSupported = false;
                this.mTemplate = NetworkTemplate.buildTemplateEthernet();
            } else {
                throw new IllegalStateException("unknown tab: " + currentTab);
            }
            getLoaderManager().restartLoader(2, ChartDataLoader.buildArgs(this.mTemplate, this.mCurrentApp), this.mChartDataCallbacks);
            getActivity().invalidateOptionsMenu();
            this.mBinding = false;
            int seriesColor = resources.getColor(R.color.sim_noitification);
            if (this.mCurrentTab != null && this.mCurrentTab.length() > "mobile".length()) {
                int slotId = Integer.parseInt(this.mCurrentTab.substring("mobile".length(), this.mCurrentTab.length()));
                SubscriptionInfo sir = Utils.findRecordBySlotId(context, slotId);
                if (sir != null) {
                    seriesColor = sir.getIconTint();
                }
            }
            int secondaryColor = Color.argb(127, Color.red(seriesColor), Color.green(seriesColor), Color.blue(seriesColor));
            this.mSeries.setChartColor(-16777216, seriesColor, secondaryColor);
            this.mDetailedSeries.setChartColor(-16777216, seriesColor, secondaryColor);
        }
    }

    public boolean isAppDetailMode() {
        return this.mCurrentApp != null;
    }

    public void updateAppDetail() {
        Context context = getActivity();
        PackageManager pm = context.getPackageManager();
        LayoutInflater inflater = getActivity().getLayoutInflater();
        if (isAppDetailMode()) {
            this.mAppDetail.setVisibility(0);
            this.mCycleAdapter.setChangeVisible(false);
            this.mChart.bindNetworkPolicy(null);
            final int uid = this.mCurrentApp.key;
            UidDetail detail = this.mUidDetailProvider.getUidDetail(uid, true);
            this.mAppIcon.setImageDrawable(detail.icon);
            this.mAppTitles.removeAllViews();
            View title = null;
            if (detail.detailLabels != null) {
                int n = detail.detailLabels.length;
                for (int i = 0; i < n; i++) {
                    CharSequence label = detail.detailLabels[i];
                    CharSequence contentDescription = detail.detailContentDescriptions[i];
                    title = inflater.inflate(R.layout.data_usage_app_title, this.mAppTitles, false);
                    TextView appTitle = (TextView) title.findViewById(R.id.app_title);
                    appTitle.setText(label);
                    appTitle.setContentDescription(contentDescription);
                    this.mAppTitles.addView(title);
                }
            } else {
                title = inflater.inflate(R.layout.data_usage_app_title, this.mAppTitles, false);
                TextView appTitle2 = (TextView) title.findViewById(R.id.app_title);
                appTitle2.setText(detail.label);
                appTitle2.setContentDescription(detail.contentDescription);
                this.mAppTitles.addView(title);
            }
            if (title != null) {
                this.mAppTotal = (TextView) title.findViewById(R.id.app_summary);
            } else {
                this.mAppTotal = null;
            }
            String[] packageNames = pm.getPackagesForUid(uid);
            if (packageNames != null && packageNames.length > 0) {
                this.mAppSettingsIntent = new Intent("android.intent.action.MANAGE_NETWORK_USAGE");
                this.mAppSettingsIntent.addCategory("android.intent.category.DEFAULT");
                boolean matchFound = false;
                int len$ = packageNames.length;
                int i$ = 0;
                while (true) {
                    if (i$ >= len$) {
                        break;
                    }
                    String packageName = packageNames[i$];
                    this.mAppSettingsIntent.setPackage(packageName);
                    if (pm.resolveActivity(this.mAppSettingsIntent, 0) == null) {
                        i$++;
                    } else {
                        matchFound = true;
                        break;
                    }
                }
                this.mAppSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (DataUsageSummary.this.isAdded()) {
                            DataUsageSummary.this.getActivity().startActivityAsUser(DataUsageSummary.this.mAppSettingsIntent, new UserHandle(UserHandle.getUserId(uid)));
                        }
                    }
                });
                this.mAppSettings.setEnabled(matchFound);
                this.mAppSettings.setVisibility(0);
            } else {
                this.mAppSettingsIntent = null;
                this.mAppSettings.setOnClickListener(null);
                this.mAppSettings.setVisibility(8);
            }
            updateDetailData();
            if (UserHandle.isApp(uid) && !this.mPolicyManager.getRestrictBackground() && isBandwidthControlEnabled() && hasReadyMobileRadio(context)) {
                setPreferenceTitle(this.mAppRestrictView, R.string.data_usage_app_restrict_background);
                setPreferenceSummary(this.mAppRestrictView, getString(R.string.data_usage_app_restrict_background_summary));
                this.mAppRestrictView.setVisibility(0);
                this.mAppRestrict.setChecked(getAppRestrictBackground());
                return;
            }
            this.mAppRestrictView.setVisibility(8);
            return;
        }
        this.mAppDetail.setVisibility(8);
        this.mCycleAdapter.setChangeVisible(true);
        this.mChart.bindDetailNetworkStats(null);
    }

    public void setPolicyWarningBytes(long warningBytes) {
        this.mPolicyEditor.setPolicyWarningBytes(this.mTemplate, warningBytes);
        updatePolicy(false);
    }

    public void setPolicyLimitBytes(long limitBytes) {
        this.mPolicyEditor.setPolicyLimitBytes(this.mTemplate, limitBytes);
        updatePolicy(false);
    }

    private boolean isMobileDataEnabled(int subId) {
        if (this.mMobileDataEnabled.get(String.valueOf(subId)) != null) {
            boolean isEnable = this.mMobileDataEnabled.get(String.valueOf(subId)).booleanValue();
            this.mMobileDataEnabled.put(String.valueOf(subId), null);
            return isEnable;
        }
        boolean isEnable2 = this.mTelephonyManager.getDataEnabled(subId);
        if (Utils.showSimCardTile(getActivity())) {
            return isEnable2 && SubscriptionManager.getDefaultDataSubId() == subId;
        }
        return isEnable2;
    }

    public void setMobileDataEnabled(int subId, boolean enabled) {
        this.mTelephonyManager.setDataEnabled(subId, enabled);
        this.mMobileDataEnabled.put(String.valueOf(subId), Boolean.valueOf(enabled));
        updatePolicy(false);
    }

    private boolean isNetworkPolicyModifiable(NetworkPolicy policy) {
        return policy != null && isBandwidthControlEnabled() && this.mDataEnabled.isChecked() && ActivityManager.getCurrentUser() == 0;
    }

    private boolean isBandwidthControlEnabled() {
        try {
            return this.mNetworkService.isBandwidthControlEnabled();
        } catch (RemoteException e) {
            Log.w("DataUsage", "problem talking with INetworkManagementService: " + e);
            return false;
        }
    }

    public void setRestrictBackground(boolean restrictBackground) {
        this.mPolicyManager.setRestrictBackground(restrictBackground);
        updateMenuTitles();
    }

    private boolean getAppRestrictBackground() {
        int uid = this.mCurrentApp.key;
        int uidPolicy = this.mPolicyManager.getUidPolicy(uid);
        return (uidPolicy & 1) != 0;
    }

    public void setAppRestrictBackground(boolean restrictBackground) {
        int uid = this.mCurrentApp.key;
        this.mPolicyManager.setUidPolicy(uid, restrictBackground ? 1 : 0);
        this.mAppRestrict.setChecked(restrictBackground);
    }

    public void updatePolicy(boolean refreshCycle) {
        boolean dataEnabledVisible = this.mDataEnabledSupported;
        boolean disableAtLimitVisible = this.mDisableAtLimitSupported;
        if (isAppDetailMode()) {
            dataEnabledVisible = false;
            disableAtLimitVisible = false;
        }
        if (isMobileTab(this.mCurrentTab)) {
            this.mBinding = true;
            this.mDataEnabled.setChecked(isMobileDataEnabled(getSubId(this.mCurrentTab)));
            this.mBinding = false;
        }
        NetworkPolicy policy = this.mPolicyEditor.getPolicy(this.mTemplate);
        if (isNetworkPolicyModifiable(policy) && isMobileDataAvailable(getSubId(this.mCurrentTab))) {
            this.mDisableAtLimit.setChecked((policy == null || policy.limitBytes == -1) ? false : true);
            if (!isAppDetailMode()) {
                this.mChart.bindNetworkPolicy(policy);
            }
        } else {
            disableAtLimitVisible = false;
            this.mChart.bindNetworkPolicy(null);
        }
        this.mDataEnabledView.setVisibility(dataEnabledVisible ? 0 : 8);
        this.mDisableAtLimitView.setVisibility(disableAtLimitVisible ? 0 : 8);
        if (refreshCycle) {
            updateCycleList(policy);
        }
    }

    private void updateCycleList(NetworkPolicy policy) {
        CycleItem previousItem = (CycleItem) this.mCycleSpinner.getSelectedItem();
        this.mCycleAdapter.clear();
        Context context = this.mCycleSpinner.getContext();
        long historyStart = Long.MAX_VALUE;
        long historyEnd = Long.MIN_VALUE;
        if (this.mChartData != null) {
            historyStart = this.mChartData.network.getStart();
            historyEnd = this.mChartData.network.getEnd();
        }
        long now = System.currentTimeMillis();
        if (historyStart == Long.MAX_VALUE) {
            historyStart = now;
        }
        if (historyEnd == Long.MIN_VALUE) {
            historyEnd = now + 1;
        }
        boolean hasCycles = false;
        if (policy != null) {
            long cycleEnd = NetworkPolicyManager.computeNextCycleBoundary(historyEnd, policy);
            while (cycleEnd > historyStart) {
                long cycleStart = NetworkPolicyManager.computeLastCycleBoundary(cycleEnd, policy);
                Log.d("DataUsage", "generating cs=" + cycleStart + " to ce=" + cycleEnd + " waiting for hs=" + historyStart);
                this.mCycleAdapter.add(new CycleItem(context, cycleStart, cycleEnd));
                cycleEnd = cycleStart;
                hasCycles = true;
            }
            this.mCycleAdapter.setChangePossible(isNetworkPolicyModifiable(policy));
        }
        if (!hasCycles) {
            long cycleEnd2 = historyEnd;
            while (cycleEnd2 > historyStart) {
                long cycleStart2 = cycleEnd2 - 2419200000L;
                this.mCycleAdapter.add(new CycleItem(context, cycleStart2, cycleEnd2));
                cycleEnd2 = cycleStart2;
            }
            this.mCycleAdapter.setChangePossible(false);
        }
        if (this.mCycleAdapter.getCount() > 0) {
            int position = this.mCycleAdapter.findNearestPosition(previousItem);
            this.mCycleSpinner.setSelection(position);
            CycleItem selectedItem = this.mCycleAdapter.getItem(position);
            if (!Objects.equal(selectedItem, previousItem)) {
                this.mCycleListener.onItemSelected(this.mCycleSpinner, null, position, 0L);
                return;
            } else {
                updateDetailData();
                return;
            }
        }
        updateDetailData();
    }

    public void disableDataForOtherSubscriptions(SubscriptionInfo currentSir) {
        if (this.mSubInfoList != null) {
            for (SubscriptionInfo subInfo : this.mSubInfoList) {
                if (subInfo.getSubscriptionId() != currentSir.getSubscriptionId()) {
                    setMobileDataEnabled(subInfo.getSubscriptionId(), false);
                }
            }
        }
    }

    public void handleMultiSimDataDialog() {
        Context context = getActivity();
        final SubscriptionInfo currentSir = getCurrentTabSubInfo(context);
        if (currentSir != null) {
            SubscriptionManager subscriptionManager = this.mSubscriptionManager;
            SubscriptionManager subscriptionManager2 = this.mSubscriptionManager;
            SubscriptionInfo nextSir = subscriptionManager.getActiveSubscriptionInfo(SubscriptionManager.getDefaultDataSubId());
            if (!Utils.showSimCardTile(context) || (nextSir != null && currentSir != null && currentSir.getSubscriptionId() == nextSir.getSubscriptionId())) {
                setMobileDataEnabled(currentSir.getSubscriptionId(), true);
                if (nextSir != null && currentSir != null && currentSir.getSubscriptionId() == nextSir.getSubscriptionId()) {
                    disableDataForOtherSubscriptions(currentSir);
                }
                updateBody();
                return;
            }
            String previousName = nextSir == null ? context.getResources().getString(R.string.sim_selection_required_pref) : nextSir.getDisplayName().toString();
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.sim_change_data_title);
            builder.setMessage(getActivity().getResources().getString(R.string.sim_change_data_message, currentSir.getDisplayName(), previousName));
            builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    DataUsageSummary.this.mSubscriptionManager.setDefaultDataSubId(currentSir.getSubscriptionId());
                    DataUsageSummary.this.setMobileDataEnabled(currentSir.getSubscriptionId(), true);
                    DataUsageSummary.this.disableDataForOtherSubscriptions(currentSir);
                    DataUsageSummary.this.updateBody();
                }
            });
            builder.setNegativeButton(R.string.cancel, (DialogInterface.OnClickListener) null);
            builder.create().show();
        }
    }

    public void updateDetailData() {
        long start = this.mChart.getInspectStart();
        long end = this.mChart.getInspectEnd();
        long now = System.currentTimeMillis();
        Context context = getActivity();
        NetworkStatsHistory.Entry entry = null;
        if (isAppDetailMode() && this.mChartData != null && this.mChartData.detail != null) {
            NetworkStatsHistory.Entry entry2 = this.mChartData.detailDefault.getValues(start, end, now, (NetworkStatsHistory.Entry) null);
            long defaultBytes = entry2.rxBytes + entry2.txBytes;
            NetworkStatsHistory.Entry entry3 = this.mChartData.detailForeground.getValues(start, end, now, entry2);
            long foregroundBytes = entry3.rxBytes + entry3.txBytes;
            long totalBytes = defaultBytes + foregroundBytes;
            if (this.mAppTotal != null) {
                this.mAppTotal.setText(android.text.format.Formatter.formatFileSize(context, totalBytes));
            }
            this.mAppBackground.setText(android.text.format.Formatter.formatFileSize(context, defaultBytes));
            this.mAppForeground.setText(android.text.format.Formatter.formatFileSize(context, foregroundBytes));
            entry = this.mChartData.detail.getValues(start, end, now, (NetworkStatsHistory.Entry) null);
            getLoaderManager().destroyLoader(3);
            this.mCycleSummary.setVisibility(8);
        } else {
            if (this.mChartData != null) {
                entry = this.mChartData.network.getValues(start, end, now, (NetworkStatsHistory.Entry) null);
            }
            this.mCycleSummary.setVisibility(0);
            getLoaderManager().restartLoader(3, SummaryForAllUidLoader.buildArgs(this.mTemplate, start, end), this.mSummaryCallbacks);
        }
        long totalBytes2 = entry != null ? entry.rxBytes + entry.txBytes : 0L;
        String totalPhrase = android.text.format.Formatter.formatFileSize(context, totalBytes2);
        this.mCycleSummary.setText(totalPhrase);
        if ((!isMobileTab(this.mCurrentTab) && !"3g".equals(this.mCurrentTab) && !"4g".equals(this.mCurrentTab)) || isAppDetailMode()) {
            this.mDisclaimer.setVisibility(8);
        } else {
            this.mDisclaimer.setVisibility(0);
        }
        ensureLayoutTransitions();
    }

    private static String getActiveSubscriberId(Context context) {
        TelephonyManager tele = TelephonyManager.from(context);
        String actualSubscriberId = tele.getSubscriberId();
        String retVal = SystemProperties.get("test.subscriberid", actualSubscriberId);
        return retVal;
    }

    private static String getActiveSubscriberId(Context context, int subId) {
        TelephonyManager tele = TelephonyManager.from(context);
        String retVal = tele.getSubscriberId(subId);
        return retVal;
    }

    public static class CycleItem implements Comparable<CycleItem> {
        public long end;
        public CharSequence label;
        public long start;

        CycleItem(CharSequence label) {
            this.label = label;
        }

        public CycleItem(Context context, long start, long end) {
            this.label = DataUsageSummary.formatDateRange(context, start, end);
            this.start = start;
            this.end = end;
        }

        public String toString() {
            return this.label.toString();
        }

        public boolean equals(Object o) {
            if (!(o instanceof CycleItem)) {
                return false;
            }
            CycleItem another = (CycleItem) o;
            return this.start == another.start && this.end == another.end;
        }

        @Override
        public int compareTo(CycleItem another) {
            return Long.compare(this.start, another.start);
        }
    }

    public static String formatDateRange(Context context, long start, long end) {
        String string;
        synchronized (sBuilder) {
            sBuilder.setLength(0);
            string = DateUtils.formatDateRange(context, sFormatter, start, end, 65552, null).toString();
        }
        return string;
    }

    public static class CycleChangeItem extends CycleItem {
        public CycleChangeItem(Context context) {
            super(context.getString(R.string.data_usage_change_cycle));
        }
    }

    public static class CycleAdapter extends ArrayAdapter<CycleItem> {
        private final CycleChangeItem mChangeItem;
        private boolean mChangePossible;
        private boolean mChangeVisible;

        public CycleAdapter(Context context) {
            super(context, R.layout.data_usage_cycle_item);
            this.mChangePossible = false;
            this.mChangeVisible = false;
            setDropDownViewResource(R.layout.data_usage_cycle_item_dropdown);
            this.mChangeItem = new CycleChangeItem(context);
        }

        public void setChangePossible(boolean possible) {
            this.mChangePossible = possible;
            updateChange();
        }

        public void setChangeVisible(boolean visible) {
            this.mChangeVisible = visible;
            updateChange();
        }

        private void updateChange() {
            remove(this.mChangeItem);
            if (this.mChangePossible && this.mChangeVisible) {
                add(this.mChangeItem);
            }
        }

        public int findNearestPosition(CycleItem target) {
            if (target != null) {
                int count = getCount();
                for (int i = count - 1; i >= 0; i--) {
                    CycleItem item = getItem(i);
                    if (!(item instanceof CycleChangeItem) && item.compareTo(target) >= 0) {
                        return i;
                    }
                }
            }
            return 0;
        }
    }

    public static class AppItem implements Parcelable, Comparable<AppItem> {
        public static final Parcelable.Creator<AppItem> CREATOR = new Parcelable.Creator<AppItem>() {
            @Override
            public AppItem createFromParcel(Parcel in) {
                return new AppItem(in);
            }

            @Override
            public AppItem[] newArray(int size) {
                return new AppItem[size];
            }
        };
        public int category;
        public final int key;
        public boolean restricted;
        public long total;
        public SparseBooleanArray uids;

        public AppItem() {
            this.uids = new SparseBooleanArray();
            this.key = 0;
        }

        public AppItem(int key) {
            this.uids = new SparseBooleanArray();
            this.key = key;
        }

        public AppItem(Parcel parcel) {
            this.uids = new SparseBooleanArray();
            this.key = parcel.readInt();
            this.uids = parcel.readSparseBooleanArray();
            this.total = parcel.readLong();
        }

        public void addUid(int uid) {
            this.uids.put(uid, true);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.key);
            dest.writeSparseBooleanArray(this.uids);
            dest.writeLong(this.total);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public int compareTo(AppItem another) {
            int comparison = Integer.compare(this.category, another.category);
            if (comparison == 0) {
                return Long.compare(another.total, this.total);
            }
            return comparison;
        }
    }

    public static class DataUsageAdapter extends BaseAdapter {
        private final int mInsetSide;
        private ArrayList<AppItem> mItems = Lists.newArrayList();
        private long mLargest;
        private final UidDetailProvider mProvider;
        private final UserManager mUm;

        public DataUsageAdapter(UserManager userManager, UidDetailProvider provider, int insetSide) {
            this.mProvider = (UidDetailProvider) Preconditions.checkNotNull(provider);
            this.mInsetSide = insetSide;
            this.mUm = userManager;
        }

        public void bindStats(NetworkStats stats, int[] restrictedUids) {
            int collapseKey;
            int category;
            this.mItems.clear();
            this.mLargest = 0L;
            int currentUserId = ActivityManager.getCurrentUser();
            List<UserHandle> profiles = this.mUm.getUserProfiles();
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
                            accumulate(managedKey, knownItems, entry, 0);
                        }
                        collapseKey = uid;
                        category = 2;
                    } else {
                        UserInfo info = this.mUm.getUserInfo(userId);
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
                accumulate(collapseKey, knownItems, entry, category);
            }
            for (int uid2 : restrictedUids) {
                if (profiles.contains(new UserHandle(UserHandle.getUserId(uid2)))) {
                    AppItem item = knownItems.get(uid2);
                    if (item == null) {
                        item = new AppItem(uid2);
                        item.total = -1L;
                        this.mItems.add(item);
                        knownItems.put(item.key, item);
                    }
                    item.restricted = true;
                }
            }
            if (!this.mItems.isEmpty()) {
                AppItem title = new AppItem();
                title.category = 1;
                this.mItems.add(title);
            }
            Collections.sort(this.mItems);
            notifyDataSetChanged();
        }

        private void accumulate(int collapseKey, SparseArray<AppItem> knownItems, NetworkStats.Entry entry, int itemCategory) {
            int uid = entry.uid;
            AppItem item = knownItems.get(collapseKey);
            if (item == null) {
                item = new AppItem(collapseKey);
                item.category = itemCategory;
                this.mItems.add(item);
                knownItems.put(item.key, item);
            }
            item.addUid(uid);
            item.total += entry.rxBytes + entry.txBytes;
            if (this.mLargest < item.total) {
                this.mLargest = item.total;
            }
        }

        @Override
        public int getCount() {
            return this.mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return this.mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return this.mItems.get(position).key;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            AppItem item = this.mItems.get(position);
            return item.category == 1 ? 1 : 0;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (position > this.mItems.size()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            return getItemViewType(position) == 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppItem item = this.mItems.get(position);
            if (getItemViewType(position) == 1) {
                if (convertView == null) {
                    convertView = DataUsageSummary.inflateCategoryHeader(LayoutInflater.from(parent.getContext()), parent);
                }
                TextView title = (TextView) convertView.findViewById(android.R.id.title);
                title.setText(R.string.data_usage_app);
            } else {
                if (convertView == null) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.data_usage_item, parent, false);
                    if (this.mInsetSide > 0) {
                        convertView.setPaddingRelative(this.mInsetSide, 0, this.mInsetSide, 0);
                    }
                }
                Context context = parent.getContext();
                TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                ProgressBar progress = (ProgressBar) convertView.findViewById(android.R.id.progress);
                UidDetailTask.bindView(this.mProvider, item, convertView);
                if (item.restricted && item.total <= 0) {
                    text1.setText(R.string.data_usage_app_restricted);
                    progress.setVisibility(8);
                } else {
                    text1.setText(android.text.format.Formatter.formatFileSize(context, item.total));
                    progress.setVisibility(0);
                }
                int percentTotal = this.mLargest != 0 ? (int) ((item.total * 100) / this.mLargest) : 0;
                progress.setProgress(percentTotal);
            }
            return convertView;
        }
    }

    public static class AppDetailsFragment extends Fragment {
        public static void show(DataUsageSummary parent, AppItem app, CharSequence label) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("app", app);
                AppDetailsFragment fragment = new AppDetailsFragment();
                fragment.setArguments(args);
                fragment.setTargetFragment(parent, 0);
                FragmentTransaction ft = parent.getFragmentManager().beginTransaction();
                ft.add(fragment, "appDetails");
                ft.addToBackStack("appDetails");
                ft.setBreadCrumbTitle(parent.getResources().getString(R.string.data_usage_app_summary_title));
                ft.commitAllowingStateLoss();
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mCurrentApp = (AppItem) getArguments().getParcelable("app");
            target.updateBody();
        }

        @Override
        public void onStop() {
            super.onStop();
            DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            target.mCurrentApp = null;
            target.updateBody();
        }
    }

    public static class ConfirmLimitFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            NetworkPolicy policy;
            if (parent.isAdded() && (policy = parent.mPolicyEditor.getPolicy(parent.mTemplate)) != null) {
                Resources res = parent.getResources();
                long minLimitBytes = (long) (policy.warningBytes * 1.2f);
                String currentTab = parent.mCurrentTab;
                if ("3g".equals(currentTab) || "4g".equals(currentTab) || DataUsageSummary.isMobileTab(currentTab)) {
                    CharSequence message = res.getString(R.string.data_usage_limit_dialog_mobile);
                    long limitBytes = Math.max(5368709120L, minLimitBytes);
                    Bundle args = new Bundle();
                    args.putCharSequence("message", message);
                    args.putLong("limitBytes", limitBytes);
                    ConfirmLimitFragment dialog = new ConfirmLimitFragment();
                    dialog.setArguments(args);
                    dialog.setTargetFragment(parent, 0);
                    dialog.show(parent.getFragmentManager(), "confirmLimit");
                    return;
                }
                throw new IllegalArgumentException("unknown current tab: " + currentTab);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            CharSequence message = getArguments().getCharSequence("message");
            final long limitBytes = getArguments().getLong("limitBytes");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_limit_dialog_title);
            builder.setMessage(message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DataUsageSummary target = (DataUsageSummary) ConfirmLimitFragment.this.getTargetFragment();
                    if (target != null) {
                        target.setPolicyLimitBytes(limitBytes);
                    }
                }
            });
            return builder.create();
        }
    }

    public static class CycleEditorFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("template", parent.mTemplate);
                CycleEditorFragment dialog = new CycleEditorFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "cycleEditor");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
            View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, (ViewGroup) null, false);
            final NumberPicker cycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);
            final NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            int cycleDay = editor.getPolicyCycleDay(template);
            cycleDayPicker.setMinValue(1);
            cycleDayPicker.setMaxValue(31);
            cycleDayPicker.setValue(cycleDay);
            cycleDayPicker.setWrapSelectorWheel(true);
            builder.setTitle(R.string.data_usage_cycle_editor_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cycleDayPicker.clearFocus();
                    int cycleDay2 = cycleDayPicker.getValue();
                    String cycleTimezone = new Time().timezone;
                    editor.setPolicyCycleDay(template, cycleDay2, cycleTimezone);
                    target.updatePolicy(true);
                }
            });
            return builder.create();
        }
    }

    public static class WarningEditorFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("template", parent.mTemplate);
                WarningEditorFragment dialog = new WarningEditorFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "warningEditor");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
            View view = dialogInflater.inflate(R.layout.data_usage_bytes_editor, (ViewGroup) null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);
            final NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            long warningBytes = editor.getPolicyWarningBytes(template);
            long limitBytes = editor.getPolicyLimitBytes(template);
            bytesPicker.setMinValue(0);
            if (limitBytes != -1) {
                bytesPicker.setMaxValue(((int) (limitBytes / 1048576)) - 1);
            } else {
                bytesPicker.setMaxValue(Integer.MAX_VALUE);
            }
            bytesPicker.setValue((int) (warningBytes / 1048576));
            bytesPicker.setWrapSelectorWheel(false);
            builder.setTitle(R.string.data_usage_warning_editor_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    bytesPicker.clearFocus();
                    long bytes = ((long) bytesPicker.getValue()) * 1048576;
                    editor.setPolicyWarningBytes(template, bytes);
                    target.updatePolicy(false);
                }
            });
            return builder.create();
        }
    }

    public static class LimitEditorFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                Bundle args = new Bundle();
                args.putParcelable("template", parent.mTemplate);
                LimitEditorFragment dialog = new LimitEditorFragment();
                dialog.setArguments(args);
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "limitEditor");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            final DataUsageSummary target = (DataUsageSummary) getTargetFragment();
            final NetworkPolicyEditor editor = target.mPolicyEditor;
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());
            View view = dialogInflater.inflate(R.layout.data_usage_bytes_editor, (ViewGroup) null, false);
            final NumberPicker bytesPicker = (NumberPicker) view.findViewById(R.id.bytes);
            final NetworkTemplate template = (NetworkTemplate) getArguments().getParcelable("template");
            long warningBytes = editor.getPolicyWarningBytes(template);
            long limitBytes = editor.getPolicyLimitBytes(template);
            bytesPicker.setMaxValue(Integer.MAX_VALUE);
            if (warningBytes != -1 && limitBytes > 0) {
                bytesPicker.setMinValue(((int) (warningBytes / 1048576)) + 1);
            } else {
                bytesPicker.setMinValue(0);
            }
            bytesPicker.setValue((int) (limitBytes / 1048576));
            bytesPicker.setWrapSelectorWheel(false);
            builder.setTitle(R.string.data_usage_limit_editor_title);
            builder.setView(view);
            builder.setPositiveButton(R.string.data_usage_cycle_editor_positive, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    bytesPicker.clearFocus();
                    long bytes = ((long) bytesPicker.getValue()) * 1048576;
                    editor.setPolicyLimitBytes(template, bytes);
                    target.updatePolicy(false);
                }
            });
            return builder.create();
        }
    }

    public static class ConfirmDataDisableFragment extends DialogFragment {
        static int mSubId;

        public static void show(DataUsageSummary parent, int subId) {
            mSubId = subId;
            if (parent.isAdded()) {
                ConfirmDataDisableFragment dialog = new ConfirmDataDisableFragment();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmDataDisable");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(R.string.data_usage_disable_mobile);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DataUsageSummary target = (DataUsageSummary) ConfirmDataDisableFragment.this.getTargetFragment();
                    if (target != null) {
                        target.setMobileDataEnabled(ConfirmDataDisableFragment.mSubId, false);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class ConfirmRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                ConfirmRestrictFragment dialog = new ConfirmRestrictFragment();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmRestrict");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_restrict_background_title);
            if (Utils.hasMultipleUsers(context)) {
                builder.setMessage(R.string.data_usage_restrict_background_multiuser);
            } else {
                builder.setMessage(R.string.data_usage_restrict_background);
            }
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DataUsageSummary target = (DataUsageSummary) ConfirmRestrictFragment.this.getTargetFragment();
                    if (target != null) {
                        target.setRestrictBackground(true);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class DeniedRestrictFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_app_restrict_background);
            builder.setMessage(R.string.data_usage_restrict_denied_dialog);
            builder.setPositiveButton(android.R.string.ok, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    public static class ConfirmAppRestrictFragment extends DialogFragment {
        public static void show(DataUsageSummary parent) {
            if (parent.isAdded()) {
                ConfirmAppRestrictFragment dialog = new ConfirmAppRestrictFragment();
                dialog.setTargetFragment(parent, 0);
                dialog.show(parent.getFragmentManager(), "confirmAppRestrict");
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context context = getActivity();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.data_usage_app_restrict_dialog_title);
            builder.setMessage(R.string.data_usage_app_restrict_dialog);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DataUsageSummary target = (DataUsageSummary) ConfirmAppRestrictFragment.this.getTargetFragment();
                    if (target != null) {
                        target.setAppRestrictBackground(true);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
            return builder.create();
        }
    }

    private static String computeTabFromIntent(Intent intent) {
        NetworkTemplate template = intent.getParcelableExtra("android.net.NETWORK_TEMPLATE");
        if (template == null) {
            int subId = intent.getIntExtra("subscription", -1);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                return "mobile" + String.valueOf(subId);
            }
            return null;
        }
        switch (template.getMatchRule()) {
            case 1:
                return "mobile";
            case 2:
                return "3g";
            case 3:
                return "4g";
            case 4:
                return "wifi";
            default:
                return null;
        }
    }

    private static class UidDetailTask extends AsyncTask<Void, Void, UidDetail> {
        private final AppItem mItem;
        private final UidDetailProvider mProvider;
        private final View mTarget;

        private UidDetailTask(UidDetailProvider provider, AppItem item, View target) {
            this.mProvider = (UidDetailProvider) Preconditions.checkNotNull(provider);
            this.mItem = (AppItem) Preconditions.checkNotNull(item);
            this.mTarget = (View) Preconditions.checkNotNull(target);
        }

        public static void bindView(UidDetailProvider provider, AppItem item, View target) {
            UidDetailTask existing = (UidDetailTask) target.getTag();
            if (existing != null) {
                existing.cancel(false);
            }
            UidDetail cachedDetail = provider.getUidDetail(item.key, false);
            if (cachedDetail != null) {
                bindView(cachedDetail, target);
            } else {
                target.setTag(new UidDetailTask(provider, item, target).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]));
            }
        }

        private static void bindView(UidDetail detail, View target) {
            ImageView icon = (ImageView) target.findViewById(android.R.id.icon);
            TextView title = (TextView) target.findViewById(android.R.id.title);
            if (detail != null) {
                icon.setImageDrawable(detail.icon);
                title.setText(detail.label);
                title.setContentDescription(detail.contentDescription);
            } else {
                icon.setImageDrawable(null);
                title.setText((CharSequence) null);
            }
        }

        @Override
        protected void onPreExecute() {
            bindView(null, this.mTarget);
        }

        @Override
        public UidDetail doInBackground(Void... params) {
            return this.mProvider.getUidDetail(this.mItem.key, true);
        }

        @Override
        public void onPostExecute(UidDetail result) {
            bindView(result, this.mTarget);
        }
    }

    public static boolean hasReadyMobileRadio(Context context) {
        ConnectivityManager conn = ConnectivityManager.from(context);
        TelephonyManager tele = TelephonyManager.from(context);
        List<SubscriptionInfo> subInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        if (subInfoList == null) {
            return false;
        }
        boolean isReady = true;
        for (SubscriptionInfo subInfo : subInfoList) {
            isReady &= tele.getSimState(subInfo.getSimSlotIndex()) == 5;
        }
        boolean retVal = conn.isNetworkSupported(0) && isReady;
        return retVal;
    }

    public static boolean hasReadyMobileRadio(Context context, int subId) {
        ConnectivityManager conn = ConnectivityManager.from(context);
        TelephonyManager tele = TelephonyManager.from(context);
        int slotId = SubscriptionManager.getSlotId(subId);
        boolean isReady = tele.getSimState(slotId) == 5;
        return conn.isNetworkSupported(0) && isReady;
    }

    public static boolean hasWifiRadio(Context context) {
        ConnectivityManager conn = ConnectivityManager.from(context);
        return conn.isNetworkSupported(1);
    }

    public boolean hasEthernet(Context context) {
        long ethernetBytes;
        ConnectivityManager conn = ConnectivityManager.from(context);
        boolean hasEthernet = conn.isNetworkSupported(9);
        if (this.mStatsSession != null) {
            try {
                ethernetBytes = this.mStatsSession.getSummaryForNetwork(NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE).getTotalBytes();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        } else {
            ethernetBytes = 0;
        }
        return hasEthernet && ethernetBytes > 0;
    }

    private static View inflatePreference(LayoutInflater inflater, ViewGroup root, View widget) {
        View view = inflater.inflate(R.layout.preference, root, false);
        LinearLayout widgetFrame = (LinearLayout) view.findViewById(android.R.id.widget_frame);
        widgetFrame.addView(widget, new LinearLayout.LayoutParams(-2, -2));
        return view;
    }

    public static View inflateCategoryHeader(LayoutInflater inflater, ViewGroup root) {
        TypedArray a = inflater.getContext().obtainStyledAttributes(null, com.android.internal.R.styleable.Preference, android.R.attr.preferenceCategoryStyle, 0);
        int resId = a.getResourceId(3, 0);
        return inflater.inflate(resId, root, false);
    }

    private static void insetListViewDrawables(ListView view, int insetSide) {
        Drawable selector = view.getSelector();
        Drawable divider = view.getDivider();
        Drawable stub = new ColorDrawable(0);
        view.setSelector(stub);
        view.setDivider(stub);
        view.setSelector(new InsetBoundsDrawable(selector, insetSide));
        view.setDivider(new InsetBoundsDrawable(divider, insetSide));
    }

    private static void setPreferenceTitle(View parent, int resId) {
        TextView title = (TextView) parent.findViewById(android.R.id.title);
        title.setText(resId);
    }

    private static void setPreferenceSummary(View parent, CharSequence string) {
        TextView summary = (TextView) parent.findViewById(android.R.id.summary);
        summary.setVisibility(0);
        summary.setText(string);
    }

    private void addMobileTab(Context context, SubscriptionInfo subInfo, boolean isMultiSim) {
        if (subInfo != null && this.mMobileTagMap != null && hasReadyMobileRadio(context, subInfo.getSubscriptionId())) {
            if (isMultiSim) {
                this.mTabHost.addTab(buildTabSpec(this.mMobileTagMap.get(Integer.valueOf(subInfo.getSubscriptionId())), subInfo.getDisplayName()));
            } else {
                this.mTabHost.addTab(buildTabSpec(this.mMobileTagMap.get(Integer.valueOf(subInfo.getSubscriptionId())), R.string.data_usage_tab_mobile));
            }
        }
    }

    private SubscriptionInfo getCurrentTabSubInfo(Context context) {
        if (this.mSubInfoList != null && this.mTabHost != null) {
            int currentTagIndex = this.mTabHost.getCurrentTab();
            int i = 0;
            for (SubscriptionInfo subInfo : this.mSubInfoList) {
                if (hasReadyMobileRadio(context, subInfo.getSubscriptionId())) {
                    int i2 = i + 1;
                    if (i == currentTagIndex) {
                        return subInfo;
                    }
                    i = i2;
                }
            }
        }
        return null;
    }

    private Map<Integer, String> initMobileTabTag(List<SubscriptionInfo> subInfoList) {
        Map<Integer, String> map = null;
        if (subInfoList != null) {
            map = new HashMap<>();
            for (SubscriptionInfo subInfo : subInfoList) {
                String mobileTag = "mobile" + String.valueOf(subInfo.getSubscriptionId());
                map.put(Integer.valueOf(subInfo.getSubscriptionId()), mobileTag);
            }
        }
        return map;
    }

    public static boolean isMobileTab(String currentTab) {
        if (currentTab != null) {
            return currentTab.contains("mobile");
        }
        return false;
    }

    public int getSubId(String currentTab) {
        if (this.mMobileTagMap != null) {
            Set<Integer> set = this.mMobileTagMap.keySet();
            for (Integer subId : set) {
                if (this.mMobileTagMap.get(subId).equals(currentTab)) {
                    return subId.intValue();
                }
            }
        }
        Log.e("DataUsage", "currentTab = " + currentTab + " non mobile tab called this function");
        return -1;
    }

    private boolean isMobileDataAvailable(long subId) {
        int[] subIds = SubscriptionManager.getSubId(0);
        if (subIds != null && subIds[0] == subId) {
            return true;
        }
        int[] subIds2 = SubscriptionManager.getSubId(1);
        if (subIds2 != null && subIds2[0] == subId) {
            return true;
        }
        int[] subIds3 = SubscriptionManager.getSubId(2);
        return subIds3 != null && ((long) subIds3[0]) == subId;
    }
}
