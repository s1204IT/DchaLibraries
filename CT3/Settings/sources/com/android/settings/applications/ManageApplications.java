package com.android.settings.applications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.icu.text.AlphabeticIndex;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.LocaleList;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceFrameLayout;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.android.settings.AppHeader;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.applications.AppStateUsageBridge;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.fuelgauge.HighPowerDetail;
import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ManageApplications extends InstrumentedFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {
    public ApplicationsAdapter mApplications;
    private ApplicationsState mApplicationsState;
    private String mCurrentPkgName;
    private int mCurrentUid;
    public int mFilter;
    private FilterSpinnerAdapter mFilterAdapter;
    private Spinner mFilterSpinner;
    private boolean mFinishAfterDialog;
    private LayoutInflater mInflater;
    CharSequence mInvalidSizeStr;
    private View mListContainer;
    public int mListType;
    private ListView mListView;
    private View mLoadingContainer;
    private NotificationBackend mNotifBackend;
    private Menu mOptionsMenu;
    private ResetAppsHelper mResetAppsHelper;
    private View mRootView;
    private boolean mShowSystem;
    private int mSortOrder = R.id.sort_order_alpha;
    private View mSpinnerHeader;
    private String mVolumeName;
    private String mVolumeUuid;
    static final boolean DEBUG = Log.isLoggable("ManageApplications", 3);
    public static final int[] FILTER_LABELS = {R.string.high_power_filter_on, R.string.filter_all_apps, R.string.filter_all_apps, R.string.filter_enabled_apps, R.string.filter_apps_disabled, R.string.filter_notif_blocked_apps, R.string.filter_notif_silent, R.string.filter_notif_sensitive_apps, R.string.filter_notif_hide_notifications_apps, R.string.filter_notif_priority_apps, R.string.filter_personal_apps, R.string.filter_work_apps, R.string.filter_with_domain_urls_apps, R.string.filter_all_apps, R.string.filter_overlay_apps, R.string.filter_write_settings_apps};
    public static final ApplicationsState.AppFilter[] FILTERS = {new ApplicationsState.CompoundFilter(AppStatePowerBridge.FILTER_POWER_WHITELISTED, ApplicationsState.FILTER_ALL_ENABLED), new ApplicationsState.CompoundFilter(ApplicationsState.FILTER_WITHOUT_DISABLED_UNTIL_USED, ApplicationsState.FILTER_ALL_ENABLED), ApplicationsState.FILTER_EVERYTHING, ApplicationsState.FILTER_ALL_ENABLED, ApplicationsState.FILTER_DISABLED, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_SILENCED, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_HIDE_SENSITIVE, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_HIDE_ALL, AppStateNotificationBridge.FILTER_APP_NOTIFICATION_PRIORITY, ApplicationsState.FILTER_PERSONAL, ApplicationsState.FILTER_WORK, ApplicationsState.FILTER_WITH_DOMAIN_URLS, AppStateUsageBridge.FILTER_APP_USAGE, AppStateOverlayBridge.FILTER_SYSTEM_ALERT_WINDOW, AppStateWriteSettingsBridge.FILTER_WRITE_SETTINGS};
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader, null);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());
        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        String className = args != null ? args.getString("classname") : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(Settings.AllApplicationsActivity.class.getName())) {
            this.mShowSystem = true;
        } else if (className.equals(Settings.NotificationAppListActivity.class.getName())) {
            this.mListType = 1;
            this.mNotifBackend = new NotificationBackend();
        } else if (className.equals(Settings.DomainsURLsAppListActivity.class.getName())) {
            this.mListType = 2;
        } else if (className.equals(Settings.StorageUseActivity.class.getName())) {
            if (args != null && args.containsKey("volumeUuid")) {
                this.mVolumeUuid = args.getString("volumeUuid");
                this.mVolumeName = args.getString("volumeName");
                this.mListType = 3;
            } else {
                this.mListType = 0;
            }
            this.mSortOrder = R.id.sort_order_size;
        } else if (className.equals(Settings.UsageAccessSettingsActivity.class.getName())) {
            this.mListType = 4;
        } else if (className.equals(Settings.HighPowerApplicationsActivity.class.getName())) {
            this.mListType = 5;
            this.mShowSystem = true;
        } else if (className.equals(Settings.OverlaySettingsActivity.class.getName())) {
            this.mListType = 6;
        } else if (className.equals(Settings.WriteSettingsActivity.class.getName())) {
            this.mListType = 7;
        } else {
            this.mListType = 0;
        }
        this.mFilter = getDefaultFilter();
        if (savedInstanceState != null) {
            this.mSortOrder = savedInstanceState.getInt("sortOrder", this.mSortOrder);
            this.mShowSystem = savedInstanceState.getBoolean("showSystem", this.mShowSystem);
        }
        this.mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);
        this.mResetAppsHelper = new ResetAppsHelper(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mInflater = inflater;
        this.mRootView = inflater.inflate(R.layout.manage_applications_apps, (ViewGroup) null);
        this.mLoadingContainer = this.mRootView.findViewById(R.id.loading_container);
        this.mLoadingContainer.setVisibility(0);
        this.mListContainer = this.mRootView.findViewById(R.id.list_container);
        if (this.mListContainer != null) {
            View emptyView = this.mListContainer.findViewById(android.R.id.empty);
            ListView lv = (ListView) this.mListContainer.findViewById(android.R.id.list);
            if (emptyView != null) {
                lv.setEmptyView(emptyView);
            }
            lv.setOnItemClickListener(this);
            lv.setSaveEnabled(true);
            lv.setItemsCanFocus(true);
            lv.setTextFilterEnabled(true);
            this.mListView = lv;
            this.mApplications = new ApplicationsAdapter(this.mApplicationsState, this, this.mFilter);
            if (savedInstanceState != null) {
                this.mApplications.mHasReceivedLoadEntries = savedInstanceState.getBoolean("hasEntries", false);
                this.mApplications.mHasReceivedBridgeCallback = savedInstanceState.getBoolean("hasBridge", false);
            }
            this.mListView.setAdapter((ListAdapter) this.mApplications);
            this.mListView.setRecyclerListener(this.mApplications);
            this.mListView.setFastScrollEnabled(isFastScrollEnabled());
            Utils.prepareCustomPreferencesList(container, this.mRootView, this.mListView, false);
        }
        if (container instanceof PreferenceFrameLayout) {
            this.mRootView.getLayoutParams().removeBorders = true;
        }
        createHeader();
        this.mResetAppsHelper.onRestoreInstanceState(savedInstanceState);
        return this.mRootView;
    }

    private void createHeader() {
        Activity activity = getActivity();
        FrameLayout pinnedHeader = (FrameLayout) this.mRootView.findViewById(R.id.pinned_header);
        this.mSpinnerHeader = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.apps_filter_spinner, (ViewGroup) pinnedHeader, false);
        this.mFilterSpinner = (Spinner) this.mSpinnerHeader.findViewById(R.id.filter_spinner);
        this.mFilterAdapter = new FilterSpinnerAdapter(this);
        this.mFilterSpinner.setAdapter((SpinnerAdapter) this.mFilterAdapter);
        this.mFilterSpinner.setOnItemSelectedListener(this);
        pinnedHeader.addView(this.mSpinnerHeader, 0);
        this.mFilterAdapter.enableFilter(getDefaultFilter());
        if (this.mListType == 0 && UserManager.get(getActivity()).getUserProfiles().size() > 1) {
            this.mFilterAdapter.enableFilter(10);
            this.mFilterAdapter.enableFilter(11);
        }
        if (this.mListType == 1) {
            this.mFilterAdapter.enableFilter(5);
            this.mFilterAdapter.enableFilter(6);
            this.mFilterAdapter.enableFilter(7);
            this.mFilterAdapter.enableFilter(8);
            this.mFilterAdapter.enableFilter(9);
        }
        if (this.mListType == 5) {
            this.mFilterAdapter.enableFilter(1);
        }
        if (this.mListType != 3) {
            return;
        }
        this.mApplications.setOverrideFilter(new ApplicationsState.VolumeFilter(this.mVolumeUuid));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (this.mListType != 3) {
            return;
        }
        FrameLayout pinnedHeader = (FrameLayout) this.mRootView.findViewById(R.id.pinned_header);
        AppHeader.createAppHeader(getActivity(), (Drawable) null, this.mVolumeName, (String) null, -1, pinnedHeader);
    }

    private int getDefaultFilter() {
        switch (this.mListType) {
            case DefaultWfcSettingsExt.CREATE:
                return 12;
            case DefaultWfcSettingsExt.DESTROY:
            default:
                return 2;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                return 13;
            case 5:
                return 0;
            case 6:
                return 14;
            case 7:
                return 15;
        }
    }

    private boolean isFastScrollEnabled() {
        switch (this.mListType) {
            case DefaultWfcSettingsExt.RESUME:
            case DefaultWfcSettingsExt.PAUSE:
            case DefaultWfcSettingsExt.DESTROY:
                if (this.mSortOrder == R.id.sort_order_alpha) {
                }
                break;
        }
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        switch (this.mListType) {
        }
        return 221;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
        updateOptionsMenu();
        if (this.mApplications == null) {
            return;
        }
        this.mApplications.resume(this.mSortOrder);
        this.mApplications.updateLoading();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mResetAppsHelper.onSaveInstanceState(outState);
        outState.putInt("sortOrder", this.mSortOrder);
        outState.putBoolean("showSystem", this.mShowSystem);
        outState.putBoolean("hasEntries", this.mApplications.mHasReceivedLoadEntries);
        outState.putBoolean("hasBridge", this.mApplications.mHasReceivedBridgeCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mApplications == null) {
            return;
        }
        this.mApplications.pause();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mResetAppsHelper.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mApplications != null) {
            this.mApplications.release();
        }
        this.mRootView = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 1 || this.mCurrentPkgName == null) {
            return;
        }
        if (this.mListType == 1) {
            this.mApplications.mExtraInfoBridge.forceUpdate(this.mCurrentPkgName, this.mCurrentUid);
            return;
        }
        if (this.mListType == 5 || this.mListType == 6 || this.mListType == 7) {
            if (this.mFinishAfterDialog) {
                getActivity().onBackPressed();
                return;
            } else {
                this.mApplications.mExtraInfoBridge.forceUpdate(this.mCurrentPkgName, this.mCurrentUid);
                return;
            }
        }
        this.mApplicationsState.requestSize(this.mCurrentPkgName, UserHandle.getUserId(this.mCurrentUid));
    }

    private void startApplicationDetailsActivity() {
        switch (this.mListType) {
            case DefaultWfcSettingsExt.PAUSE:
                startAppInfoFragment(AppNotificationSettings.class, R.string.app_notifications_title);
                break;
            case DefaultWfcSettingsExt.CREATE:
                startAppInfoFragment(AppLaunchSettings.class, R.string.auto_launch_label);
                break;
            case DefaultWfcSettingsExt.DESTROY:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_settings);
                break;
            case DefaultWfcSettingsExt.CONFIG_CHANGE:
                startAppInfoFragment(UsageAccessDetails.class, R.string.usage_access);
                break;
            case 5:
                HighPowerDetail.show(this, this.mCurrentPkgName, 1, this.mFinishAfterDialog);
                break;
            case 6:
                startAppInfoFragment(DrawOverlayDetails.class, R.string.overlay_settings);
                break;
            case 7:
                startAppInfoFragment(WriteSettingsDetails.class, R.string.write_system_settings);
                break;
            default:
                startAppInfoFragment(InstalledAppDetails.class, R.string.application_info_label);
                break;
        }
    }

    private void startAppInfoFragment(Class<?> fragment, int titleRes) {
        AppInfoBase.startAppInfoFragment(fragment, titleRes, this.mCurrentPkgName, this.mCurrentUid, this, 1);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mListType == 2) {
            return;
        }
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, this.mListType == 0 ? R.string.help_uri_apps : R.string.help_uri_notifications, getClass().getName());
        this.mOptionsMenu = menu;
        inflater.inflate(R.menu.manage_apps, menu);
        updateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    @Override
    public void onDestroyOptionsMenu() {
        this.mOptionsMenu = null;
    }

    void updateOptionsMenu() {
        boolean z = false;
        if (this.mOptionsMenu == null) {
            return;
        }
        this.mOptionsMenu.findItem(R.id.advanced).setVisible(this.mListType == 0 || this.mListType == 1);
        MenuItem menuItemFindItem = this.mOptionsMenu.findItem(R.id.sort_order_alpha);
        boolean z2 = this.mListType == 3 && this.mSortOrder != R.id.sort_order_alpha;
        menuItemFindItem.setVisible(z2);
        MenuItem menuItemFindItem2 = this.mOptionsMenu.findItem(R.id.sort_order_size);
        boolean z3 = this.mListType == 3 && this.mSortOrder != R.id.sort_order_size;
        menuItemFindItem2.setVisible(z3);
        MenuItem menuItemFindItem3 = this.mOptionsMenu.findItem(R.id.show_system);
        boolean z4 = (this.mShowSystem || this.mListType == 5) ? false : true;
        menuItemFindItem3.setVisible(z4);
        MenuItem menuItemFindItem4 = this.mOptionsMenu.findItem(R.id.hide_system);
        if (this.mShowSystem && this.mListType != 5) {
            z = true;
        }
        menuItemFindItem4.setVisible(z);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        switch (item.getItemId()) {
            case R.id.advanced:
                if (this.mListType == 1) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(ConfigureNotificationSettings.class.getName(), null, R.string.configure_notification_settings, null, this, 2);
                } else {
                    ((SettingsActivity) getActivity()).startPreferencePanel(AdvancedAppSettings.class.getName(), null, R.string.configure_apps, null, this, 2);
                }
                return true;
            case R.id.show_system:
            case R.id.hide_system:
                this.mShowSystem = !this.mShowSystem;
                this.mApplications.rebuild(false);
                break;
            case R.id.sort_order_alpha:
            case R.id.sort_order_size:
                this.mSortOrder = menuId;
                this.mListView.setFastScrollEnabled(isFastScrollEnabled());
                if (this.mApplications != null) {
                    this.mApplications.rebuild(this.mSortOrder);
                }
                break;
            case R.id.reset_app_preferences:
                this.mResetAppsHelper.buildResetDialog();
                return true;
            default:
                return false;
        }
        updateOptionsMenu();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (this.mApplications == null || this.mApplications.getCount() <= position) {
            return;
        }
        ApplicationsState.AppEntry entry = this.mApplications.getAppEntry(position);
        this.mCurrentPkgName = entry.info.packageName;
        this.mCurrentUid = entry.info.uid;
        startApplicationDetailsActivity();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.mFilter = this.mFilterAdapter.getFilter(position);
        this.mApplications.setFilter(this.mFilter);
        if (DEBUG) {
            Log.d("ManageApplications", "Selecting filter " + this.mFilter);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void updateView() {
        updateOptionsMenu();
        Activity host = getActivity();
        if (host == null) {
            return;
        }
        host.invalidateOptionsMenu();
    }

    public void setHasDisabled(boolean hasDisabledApps) {
        if (this.mListType != 0) {
            return;
        }
        this.mFilterAdapter.setFilterEnabled(3, hasDisabledApps);
        this.mFilterAdapter.setFilterEnabled(4, hasDisabledApps);
    }

    static class FilterSpinnerAdapter extends ArrayAdapter<CharSequence> {
        private final ArrayList<Integer> mFilterOptions;
        private final ManageApplications mManageApplications;

        public FilterSpinnerAdapter(ManageApplications manageApplications) {
            super(manageApplications.getActivity(), R.layout.filter_spinner_item);
            this.mFilterOptions = new ArrayList<>();
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            this.mManageApplications = manageApplications;
        }

        public int getFilter(int position) {
            return this.mFilterOptions.get(position).intValue();
        }

        public void setFilterEnabled(int filter, boolean enabled) {
            if (enabled) {
                enableFilter(filter);
            } else {
                disableFilter(filter);
            }
        }

        public void enableFilter(int filter) {
            if (this.mFilterOptions.contains(Integer.valueOf(filter))) {
                return;
            }
            if (ManageApplications.DEBUG) {
                Log.d("ManageApplications", "Enabling filter " + filter);
            }
            this.mFilterOptions.add(Integer.valueOf(filter));
            Collections.sort(this.mFilterOptions);
            this.mManageApplications.mSpinnerHeader.setVisibility(this.mFilterOptions.size() > 1 ? 0 : 8);
            notifyDataSetChanged();
            if (this.mFilterOptions.size() != 1) {
                return;
            }
            if (ManageApplications.DEBUG) {
                Log.d("ManageApplications", "Auto selecting filter " + filter);
            }
            this.mManageApplications.mFilterSpinner.setSelection(0);
            this.mManageApplications.onItemSelected(null, null, 0, 0L);
        }

        public void disableFilter(int filter) {
            if (!this.mFilterOptions.remove(Integer.valueOf(filter))) {
                return;
            }
            if (ManageApplications.DEBUG) {
                Log.d("ManageApplications", "Disabling filter " + filter);
            }
            Collections.sort(this.mFilterOptions);
            this.mManageApplications.mSpinnerHeader.setVisibility(this.mFilterOptions.size() > 1 ? 0 : 8);
            notifyDataSetChanged();
            if (this.mManageApplications.mFilter != filter || this.mFilterOptions.size() <= 0) {
                return;
            }
            if (ManageApplications.DEBUG) {
                Log.d("ManageApplications", "Auto selecting filter " + this.mFilterOptions.get(0));
            }
            this.mManageApplications.mFilterSpinner.setSelection(0);
            this.mManageApplications.onItemSelected(null, null, 0, 0L);
        }

        @Override
        public int getCount() {
            return this.mFilterOptions.size();
        }

        @Override
        public CharSequence getItem(int position) {
            return getFilterString(this.mFilterOptions.get(position).intValue());
        }

        private CharSequence getFilterString(int filter) {
            return this.mManageApplications.getString(ManageApplications.FILTER_LABELS[filter]);
        }
    }

    static class ApplicationsAdapter extends BaseAdapter implements Filterable, ApplicationsState.Callbacks, AppStateBaseBridge.Callback, AbsListView.RecyclerListener, SectionIndexer {
        private static final SectionInfo[] EMPTY_SECTIONS = new SectionInfo[0];
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private final Handler mBgHandler;
        private final Context mContext;
        CharSequence mCurFilterPrefix;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private final AppStateBaseBridge mExtraInfoBridge;
        private int mFilterMode;
        private boolean mHasReceivedBridgeCallback;
        private boolean mHasReceivedLoadEntries;
        private AlphabeticIndex.ImmutableIndex mIndex;
        private final ManageApplications mManageApplications;
        private ApplicationsState.AppFilter mOverrideFilter;
        private PackageManager mPm;
        private int[] mPositionToSectionIndex;
        private boolean mResumed;
        private final ApplicationsState.Session mSession;
        private final ApplicationsState mState;
        private final ArrayList<View> mActive = new ArrayList<>();
        private int mLastSortMode = -1;
        private int mWhichSize = 0;
        private SectionInfo[] mSections = EMPTY_SECTIONS;
        private Filter mFilter = new Filter() {
            @Override
            protected Filter.FilterResults performFiltering(CharSequence constraint) {
                ArrayList<ApplicationsState.AppEntry> entries = ApplicationsAdapter.this.applyPrefixFilter(constraint, ApplicationsAdapter.this.mBaseEntries);
                Filter.FilterResults fr = new Filter.FilterResults();
                fr.values = entries;
                fr.count = entries.size();
                return fr;
            }

            @Override
            protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
                ApplicationsAdapter.this.mCurFilterPrefix = constraint;
                ApplicationsAdapter.this.mEntries = (ArrayList) results.values;
                ApplicationsAdapter.this.rebuildSections();
                ApplicationsAdapter.this.notifyDataSetChanged();
            }
        };
        private final Handler mFgHandler = new Handler();

        public ApplicationsAdapter(ApplicationsState state, ManageApplications manageApplications, int filterMode) {
            this.mState = state;
            this.mBgHandler = new Handler(this.mState.getBackgroundLooper());
            this.mSession = state.newSession(this);
            this.mManageApplications = manageApplications;
            this.mContext = manageApplications.getActivity();
            this.mPm = this.mContext.getPackageManager();
            this.mFilterMode = filterMode;
            if (this.mManageApplications.mListType == 1) {
                this.mExtraInfoBridge = new AppStateNotificationBridge(this.mContext, this.mState, this, manageApplications.mNotifBackend);
                return;
            }
            if (this.mManageApplications.mListType == 4) {
                this.mExtraInfoBridge = new AppStateUsageBridge(this.mContext, this.mState, this);
                return;
            }
            if (this.mManageApplications.mListType == 5) {
                this.mExtraInfoBridge = new AppStatePowerBridge(this.mState, this);
                return;
            }
            if (this.mManageApplications.mListType == 6) {
                this.mExtraInfoBridge = new AppStateOverlayBridge(this.mContext, this.mState, this);
            } else if (this.mManageApplications.mListType == 7) {
                this.mExtraInfoBridge = new AppStateWriteSettingsBridge(this.mContext, this.mState, this);
            } else {
                this.mExtraInfoBridge = null;
            }
        }

        public void setOverrideFilter(ApplicationsState.AppFilter overrideFilter) {
            this.mOverrideFilter = overrideFilter;
            rebuild(true);
        }

        public void setFilter(int filter) {
            this.mFilterMode = filter;
            rebuild(true);
        }

        public void resume(int sort) {
            if (ManageApplications.DEBUG) {
                Log.i("ManageApplications", "Resume!  mResumed=" + this.mResumed);
            }
            if (!this.mResumed) {
                this.mResumed = true;
                this.mSession.resume();
                this.mLastSortMode = sort;
                if (this.mExtraInfoBridge != null) {
                    this.mExtraInfoBridge.resume();
                }
                rebuild(false);
                return;
            }
            rebuild(sort);
        }

        public void pause() {
            if (!this.mResumed) {
                return;
            }
            this.mResumed = false;
            this.mSession.pause();
            if (this.mExtraInfoBridge == null) {
                return;
            }
            this.mExtraInfoBridge.pause();
        }

        public void release() {
            this.mSession.release();
            if (this.mExtraInfoBridge == null) {
                return;
            }
            this.mExtraInfoBridge.release();
        }

        public void rebuild(int sort) {
            if (sort == this.mLastSortMode) {
                return;
            }
            this.mLastSortMode = sort;
            rebuild(true);
        }

        public void rebuild(boolean eraseold) {
            final Comparator<ApplicationsState.AppEntry> comparatorObj;
            if (this.mHasReceivedLoadEntries) {
                if (this.mExtraInfoBridge != null && !this.mHasReceivedBridgeCallback) {
                    return;
                }
                if (ManageApplications.DEBUG) {
                    Log.i("ManageApplications", "Rebuilding app list...");
                }
                boolean emulated = Environment.isExternalStorageEmulated();
                if (emulated) {
                    this.mWhichSize = 0;
                } else {
                    this.mWhichSize = 1;
                }
                ApplicationsState.AppFilter filterObj = ManageApplications.FILTERS[this.mFilterMode];
                if (this.mOverrideFilter != null) {
                    filterObj = this.mOverrideFilter;
                }
                if (!this.mManageApplications.mShowSystem) {
                    filterObj = new ApplicationsState.CompoundFilter(filterObj, ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER);
                }
                switch (this.mLastSortMode) {
                    case R.id.sort_order_size:
                        switch (this.mWhichSize) {
                            case DefaultWfcSettingsExt.PAUSE:
                                comparatorObj = ApplicationsState.INTERNAL_SIZE_COMPARATOR;
                                break;
                            case DefaultWfcSettingsExt.CREATE:
                                comparatorObj = ApplicationsState.EXTERNAL_SIZE_COMPARATOR;
                                break;
                            default:
                                comparatorObj = ApplicationsState.SIZE_COMPARATOR;
                                break;
                        }
                        break;
                    default:
                        comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
                        break;
                }
                final ApplicationsState.AppFilter finalFilterObj = filterObj;
                this.mBgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ApplicationsAdapter.this.m682xf40fefa8(finalFilterObj, comparatorObj);
                    }
                });
            }
        }

        void m682xf40fefa8(ApplicationsState.AppFilter finalFilterObj, Comparator comparatorObj) {
            final ArrayList<ApplicationsState.AppEntry> entries = this.mSession.rebuild(finalFilterObj, comparatorObj, false);
            if (entries != null) {
                this.mFgHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        this.val$this.m683xf40fefa9(entries);
                    }
                });
            }
        }

        private static boolean packageNameEquals(PackageItemInfo info1, PackageItemInfo info2) {
            if (info1 == null || info2 == null || info1.packageName == null || info2.packageName == null) {
                return false;
            }
            return info1.packageName.equals(info2.packageName);
        }

        private ArrayList<ApplicationsState.AppEntry> removeDuplicateIgnoringUser(ArrayList<ApplicationsState.AppEntry> entries) {
            int size = entries.size();
            ArrayList<ApplicationsState.AppEntry> returnEntries = new ArrayList<>(size);
            PackageItemInfo lastInfo = null;
            for (int i = 0; i < size; i++) {
                ApplicationsState.AppEntry appEntry = entries.get(i);
                PackageItemInfo info = appEntry.info;
                if (!packageNameEquals(lastInfo, appEntry.info)) {
                    returnEntries.add(appEntry);
                }
                lastInfo = info;
            }
            returnEntries.trimToSize();
            return returnEntries;
        }

        @Override
        public void m683xf40fefa9(ArrayList<ApplicationsState.AppEntry> entries) {
            if (this.mFilterMode == 0 || this.mFilterMode == 1) {
                entries = removeDuplicateIgnoringUser(entries);
            }
            this.mBaseEntries = entries;
            if (this.mBaseEntries != null) {
                this.mEntries = applyPrefixFilter(this.mCurFilterPrefix, this.mBaseEntries);
                rebuildSections();
            } else {
                this.mEntries = null;
                this.mSections = EMPTY_SECTIONS;
                this.mPositionToSectionIndex = null;
            }
            notifyDataSetChanged();
            if (this.mSession.getAllApps().size() != 0 && this.mManageApplications.mListContainer.getVisibility() != 0) {
                Utils.handleLoadingContainer(this.mManageApplications.mLoadingContainer, this.mManageApplications.mListContainer, true, true);
            }
            if (this.mManageApplications.mListType == 4) {
                return;
            }
            this.mManageApplications.setHasDisabled(this.mState.haveDisabledApps());
        }

        public void rebuildSections() {
            if (this.mEntries != null && this.mManageApplications.mListView.isFastScrollEnabled()) {
                if (this.mIndex == null) {
                    LocaleList locales = this.mContext.getResources().getConfiguration().getLocales();
                    if (locales.size() == 0) {
                        locales = new LocaleList(Locale.ENGLISH);
                    }
                    AlphabeticIndex index = new AlphabeticIndex(locales.get(0));
                    int localeCount = locales.size();
                    for (int i = 1; i < localeCount; i++) {
                        index.addLabels(locales.get(i));
                    }
                    index.addLabels(Locale.ENGLISH);
                    this.mIndex = index.buildImmutableIndex();
                }
                ArrayList<SectionInfo> sections = new ArrayList<>();
                int lastSecId = -1;
                int totalEntries = this.mEntries.size();
                this.mPositionToSectionIndex = new int[totalEntries];
                for (int pos = 0; pos < totalEntries; pos++) {
                    String label = this.mEntries.get(pos).label;
                    AlphabeticIndex.ImmutableIndex immutableIndex = this.mIndex;
                    if (TextUtils.isEmpty(label)) {
                        label = "";
                    }
                    int secId = immutableIndex.getBucketIndex(label);
                    if (secId != lastSecId) {
                        lastSecId = secId;
                        sections.add(new SectionInfo(this.mIndex.getBucket(secId).getLabel(), pos));
                    }
                    this.mPositionToSectionIndex[pos] = sections.size() - 1;
                }
                this.mSections = (SectionInfo[]) sections.toArray(EMPTY_SECTIONS);
                return;
            }
            this.mSections = EMPTY_SECTIONS;
            this.mPositionToSectionIndex = null;
        }

        public void updateLoading() {
            Utils.handleLoadingContainer(this.mManageApplications.mLoadingContainer, this.mManageApplications.mListContainer, this.mHasReceivedLoadEntries && this.mSession.getAllApps().size() != 0, false);
        }

        ArrayList<ApplicationsState.AppEntry> applyPrefixFilter(CharSequence prefix, ArrayList<ApplicationsState.AppEntry> origEntries) {
            if (prefix == null || prefix.length() == 0) {
                return origEntries;
            }
            String prefixStr = ApplicationsState.normalize(prefix.toString());
            String spacePrefixStr = " " + prefixStr;
            ArrayList<ApplicationsState.AppEntry> newEntries = new ArrayList<>();
            for (int i = 0; i < origEntries.size(); i++) {
                ApplicationsState.AppEntry entry = origEntries.get(i);
                String nlabel = entry.getNormalizedLabel();
                if (nlabel.startsWith(prefixStr) || nlabel.indexOf(spacePrefixStr) != -1) {
                    newEntries.add(entry);
                }
            }
            return newEntries;
        }

        @Override
        public void onExtraInfoUpdated() {
            this.mHasReceivedBridgeCallback = true;
            rebuild(false);
        }

        @Override
        public void onRunningStateChanged(boolean running) {
            this.mManageApplications.getActivity().setProgressBarIndeterminateVisibility(running);
        }

        @Override
        public void onPackageListChanged() {
            rebuild(false);
        }

        @Override
        public void onPackageIconChanged() {
        }

        @Override
        public void onLoadEntriesCompleted() {
            this.mHasReceivedLoadEntries = true;
            rebuild(false);
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            for (int i = 0; i < this.mActive.size(); i++) {
                AppViewHolder holder = (AppViewHolder) this.mActive.get(i).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        updateSummary(holder);
                    }
                    if (holder.entry.info.packageName.equals(this.mManageApplications.mCurrentPkgName) && this.mLastSortMode == R.id.sort_order_size) {
                        rebuild(false);
                        return;
                    }
                    return;
                }
            }
        }

        @Override
        public void onLauncherInfoChanged() {
            if (this.mManageApplications.mShowSystem) {
                return;
            }
            rebuild(false);
        }

        @Override
        public void onAllSizesComputed() {
            if (this.mLastSortMode != R.id.sort_order_size) {
                return;
            }
            rebuild(false);
        }

        @Override
        public int getCount() {
            if (this.mEntries != null) {
                return this.mEntries.size();
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return this.mEntries.get(position);
        }

        public ApplicationsState.AppEntry getAppEntry(int position) {
            return this.mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return this.mEntries.get(position).id;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (this.mManageApplications.mListType != 5) {
                return true;
            }
            ApplicationsState.AppEntry entry = this.mEntries.get(position);
            return !PowerWhitelistBackend.getInstance().isSysWhitelisted(entry.info.packageName);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder holder = AppViewHolder.createOrRecycle(this.mManageApplications.mInflater, convertView);
            View convertView2 = holder.rootView;
            ApplicationsState.AppEntry entry = this.mEntries.get(position);
            synchronized (entry) {
                holder.entry = entry;
                if (entry.label != null) {
                    holder.appName.setText(entry.label);
                }
                this.mState.ensureIcon(entry);
                if (entry.icon != null) {
                    holder.appIcon.setImageDrawable(entry.icon);
                }
                updateSummary(holder);
                if ((entry.info.flags & 8388608) == 0) {
                    holder.disabled.setVisibility(0);
                    holder.disabled.setText(R.string.not_installed);
                } else if (!entry.info.enabled) {
                    holder.disabled.setVisibility(0);
                    holder.disabled.setText(R.string.disabled);
                } else {
                    holder.disabled.setVisibility(8);
                }
            }
            this.mActive.remove(convertView2);
            this.mActive.add(convertView2);
            convertView2.setEnabled(isEnabled(position));
            return convertView2;
        }

        private void updateSummary(AppViewHolder holder) {
            int i;
            switch (this.mManageApplications.mListType) {
                case DefaultWfcSettingsExt.PAUSE:
                    if (holder.entry.extraInfo != null) {
                        holder.summary.setText(InstalledAppDetails.getNotificationSummary((NotificationBackend.AppRow) holder.entry.extraInfo, this.mContext));
                    } else {
                        holder.summary.setText((CharSequence) null);
                    }
                    break;
                case DefaultWfcSettingsExt.CREATE:
                    holder.summary.setText(getDomainsSummary(holder.entry.info.packageName));
                    break;
                case DefaultWfcSettingsExt.DESTROY:
                default:
                    holder.updateSizeText(this.mManageApplications.mInvalidSizeStr, this.mWhichSize);
                    break;
                case DefaultWfcSettingsExt.CONFIG_CHANGE:
                    if (holder.entry.extraInfo != null) {
                        TextView textView = holder.summary;
                        if (new AppStateUsageBridge.UsageState((AppStateAppOpsBridge.PermissionState) holder.entry.extraInfo).isPermissible()) {
                            i = R.string.switch_on_text;
                        } else {
                            i = R.string.switch_off_text;
                        }
                        textView.setText(i);
                    } else {
                        holder.summary.setText((CharSequence) null);
                    }
                    break;
                case 5:
                    holder.summary.setText(HighPowerDetail.getSummary(this.mContext, holder.entry));
                    break;
                case 6:
                    holder.summary.setText(DrawOverlayDetails.getSummary(this.mContext, holder.entry));
                    break;
                case 7:
                    holder.summary.setText(WriteSettingsDetails.getSummary(this.mContext, holder.entry));
                    break;
            }
        }

        @Override
        public Filter getFilter() {
            return this.mFilter;
        }

        @Override
        public void onMovedToScrapHeap(View view) {
            this.mActive.remove(view);
        }

        private CharSequence getDomainsSummary(String packageName) {
            int domainStatus = this.mPm.getIntentVerificationStatusAsUser(packageName, UserHandle.myUserId());
            if (domainStatus == 3) {
                return this.mContext.getString(R.string.domain_urls_summary_none);
            }
            ArraySet<String> result = Utils.getHandledDomains(this.mPm, packageName);
            if (result.size() == 0) {
                return this.mContext.getString(R.string.domain_urls_summary_none);
            }
            return result.size() == 1 ? this.mContext.getString(R.string.domain_urls_summary_one, result.valueAt(0)) : this.mContext.getString(R.string.domain_urls_summary_some, result.valueAt(0));
        }

        @Override
        public Object[] getSections() {
            return this.mSections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return this.mSections[sectionIndex].position;
        }

        @Override
        public int getSectionForPosition(int position) {
            return this.mPositionToSectionIndex[position];
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        SummaryProvider(Context context, SummaryLoader loader, SummaryProvider summaryProvider) {
            this(context, loader);
        }

        private SummaryProvider(Context context, SummaryLoader loader) {
            this.mContext = context;
            this.mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            new AppCounter(this.mContext) {
                @Override
                protected void onCountComplete(int num) {
                    SummaryProvider.this.mLoader.setSummary(SummaryProvider.this, SummaryProvider.this.mContext.getString(R.string.apps_summary, Integer.valueOf(num)));
                }

                @Override
                protected boolean includeInCount(ApplicationInfo info) {
                    if ((info.flags & 128) != 0 || (info.flags & 1) == 0) {
                        return true;
                    }
                    Intent launchIntent = new Intent("android.intent.action.MAIN", (Uri) null).addCategory("android.intent.category.LAUNCHER").setPackage(info.packageName);
                    int userId = UserHandle.getUserId(info.uid);
                    List<ResolveInfo> intents = this.mPm.queryIntentActivitiesAsUser(launchIntent, 786944, userId);
                    return (intents == null || intents.size() == 0) ? false : true;
                }
            }.execute(new Void[0]);
        }
    }

    private static class SectionInfo {
        final String label;
        final int position;

        public SectionInfo(String label, int position) {
            this.label = label;
            this.position = position;
        }

        public String toString() {
            return this.label;
        }
    }
}
