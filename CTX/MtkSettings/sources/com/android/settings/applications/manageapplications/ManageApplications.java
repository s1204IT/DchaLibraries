package com.android.settings.applications.manageapplications;

import android.app.Activity;
import android.app.usage.IUsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceFrameLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppStateAppOpsBridge;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.applications.AppStateDirectoryAccessBridge;
import com.android.settings.applications.AppStateInstallAppsBridge;
import com.android.settings.applications.AppStateNotificationBridge;
import com.android.settings.applications.AppStateOverlayBridge;
import com.android.settings.applications.AppStatePowerBridge;
import com.android.settings.applications.AppStateUsageBridge;
import com.android.settings.applications.AppStateWriteSettingsBridge;
import com.android.settings.applications.AppStorageSettings;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.applications.DirectoryAccessDetails;
import com.android.settings.applications.InstalledAppCounter;
import com.android.settings.applications.UsageAccessDetails;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.applications.appinfo.DrawOverlayDetails;
import com.android.settings.applications.appinfo.ExternalSourcesDetails;
import com.android.settings.applications.appinfo.WriteSettingsDetails;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.fuelgauge.HighPowerDetail;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.LoadingViewController;
import com.android.settings.wifi.AppStateChangeWifiStateBridge;
import com.android.settings.wifi.ChangeWifiStateDetails;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

public class ManageApplications extends InstrumentedFragment implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    static final boolean DEBUG = Log.isLoggable("ManageApplications", 3);
    public static final Set<Integer> LIST_TYPES_WITH_INSTANT = new ArraySet(Arrays.asList(0, 3));
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    private ApplicationsAdapter mApplications;
    private ApplicationsState mApplicationsState;
    private String mCurrentPkgName;
    private int mCurrentUid;
    private View mEmptyView;
    private AppFilterItem mFilter;
    private FilterSpinnerAdapter mFilterAdapter;
    private Spinner mFilterSpinner;
    CharSequence mInvalidSizeStr;
    private boolean mIsWorkOnly;
    private View mListContainer;
    public int mListType;
    private View mLoadingContainer;
    private NotificationBackend mNotificationBackend;
    private Menu mOptionsMenu;
    private RecyclerView mRecyclerView;
    private ResetAppsHelper mResetAppsHelper;
    private View mRootView;
    private boolean mShowSystem;
    int mSortOrder = R.id.sort_order_alpha;
    private View mSpinnerHeader;
    private int mStorageType;
    private IUsageStatsManager mUsageStatsManager;
    private UserManager mUserManager;
    private String mVolumeUuid;
    private int mWorkUserId;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        Activity activity = getActivity();
        this.mApplicationsState = ApplicationsState.getInstance(activity.getApplication());
        Intent intent = activity.getIntent();
        Bundle arguments = getArguments();
        int intExtra = intent.getIntExtra(":settings:show_fragment_title_resid", R.string.application_info_label);
        String string = arguments != null ? arguments.getString("classname") : null;
        if (string == null) {
            string = intent.getComponent().getClassName();
        }
        if (string.equals(Settings.StorageUseActivity.class.getName())) {
            if (arguments != null && arguments.containsKey("volumeUuid")) {
                this.mVolumeUuid = arguments.getString("volumeUuid");
                this.mStorageType = arguments.getInt("storageType", 0);
                this.mListType = 3;
            } else {
                this.mListType = 0;
            }
            this.mSortOrder = R.id.sort_order_size;
        } else if (string.equals(Settings.UsageAccessSettingsActivity.class.getName())) {
            this.mListType = 4;
            intExtra = R.string.usage_access;
        } else if (string.equals(Settings.HighPowerApplicationsActivity.class.getName())) {
            this.mListType = 5;
            this.mShowSystem = true;
            intExtra = R.string.high_power_apps;
        } else if (string.equals(Settings.OverlaySettingsActivity.class.getName())) {
            this.mListType = 6;
            intExtra = R.string.system_alert_window_settings;
        } else if (string.equals(Settings.WriteSettingsActivity.class.getName())) {
            this.mListType = 7;
            intExtra = R.string.write_settings;
        } else if (string.equals(Settings.ManageExternalSourcesActivity.class.getName())) {
            this.mListType = 8;
            intExtra = R.string.install_other_apps;
        } else if (string.equals(Settings.GamesStorageActivity.class.getName())) {
            this.mListType = 9;
            this.mSortOrder = R.id.sort_order_size;
        } else if (string.equals(Settings.MoviesStorageActivity.class.getName())) {
            this.mListType = 10;
            this.mSortOrder = R.id.sort_order_size;
        } else if (string.equals(Settings.PhotosStorageActivity.class.getName())) {
            this.mListType = 11;
            this.mSortOrder = R.id.sort_order_size;
            this.mStorageType = arguments.getInt("storageType", 0);
        } else if (string.equals(Settings.DirectoryAccessSettingsActivity.class.getName())) {
            this.mListType = 12;
            intExtra = R.string.directory_access;
        } else if (string.equals(Settings.ChangeWifiStateActivity.class.getName())) {
            this.mListType = 13;
            intExtra = R.string.change_wifi_state_title;
        } else if (string.equals(Settings.NotificationAppListActivity.class.getName())) {
            this.mListType = 1;
            this.mUsageStatsManager = IUsageStatsManager.Stub.asInterface(ServiceManager.getService("usagestats"));
            this.mUserManager = UserManager.get(getContext());
            this.mNotificationBackend = new NotificationBackend();
            this.mSortOrder = R.id.sort_order_recent_notification;
            intExtra = R.string.app_notifications_title;
        } else {
            if (intExtra == -1) {
                intExtra = R.string.application_info_label;
            }
            this.mListType = 0;
        }
        AppFilterRegistry appFilterRegistry = AppFilterRegistry.getInstance();
        this.mFilter = appFilterRegistry.get(appFilterRegistry.getDefaultFilterType(this.mListType));
        this.mIsWorkOnly = arguments != null ? arguments.getBoolean("workProfileOnly") : false;
        this.mWorkUserId = arguments != null ? arguments.getInt("workId") : -1;
        if (bundle != null) {
            this.mSortOrder = bundle.getInt("sortOrder", this.mSortOrder);
            this.mShowSystem = bundle.getBoolean("showSystem", this.mShowSystem);
        }
        this.mInvalidSizeStr = activity.getText(R.string.invalid_size_value);
        this.mResetAppsHelper = new ResetAppsHelper(activity);
        if (intExtra > 0) {
            activity.setTitle(intExtra);
        }
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mRootView = layoutInflater.inflate(R.layout.manage_applications_apps, (ViewGroup) null);
        this.mLoadingContainer = this.mRootView.findViewById(R.id.loading_container);
        this.mListContainer = this.mRootView.findViewById(R.id.list_container);
        if (this.mListContainer != null) {
            this.mEmptyView = this.mListContainer.findViewById(android.R.id.empty);
            this.mApplications = new ApplicationsAdapter(this.mApplicationsState, this, this.mFilter, bundle);
            if (bundle != null) {
                this.mApplications.mHasReceivedLoadEntries = bundle.getBoolean("hasEntries", false);
                this.mApplications.mHasReceivedBridgeCallback = bundle.getBoolean("hasBridge", false);
            }
            int userId = this.mIsWorkOnly ? this.mWorkUserId : UserHandle.getUserId(this.mCurrentUid);
            if (this.mStorageType == 1) {
                Context context = getContext();
                this.mApplications.setExtraViewController(new MusicViewHolderController(context, new StorageStatsSource(context), this.mVolumeUuid, UserHandle.of(userId)));
            } else if (this.mStorageType == 3) {
                Context context2 = getContext();
                this.mApplications.setExtraViewController(new PhotosViewHolderController(context2, new StorageStatsSource(context2), this.mVolumeUuid, UserHandle.of(userId)));
            }
            this.mRecyclerView = (RecyclerView) this.mListContainer.findViewById(R.id.apps_list);
            this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), 1, false));
            this.mRecyclerView.setAdapter(this.mApplications);
        }
        if (viewGroup instanceof PreferenceFrameLayout) {
            this.mRootView.getLayoutParams().removeBorders = true;
        }
        createHeader();
        this.mResetAppsHelper.onRestoreInstanceState(bundle);
        return this.mRootView;
    }

    void createHeader() {
        Activity activity = getActivity();
        FrameLayout frameLayout = (FrameLayout) this.mRootView.findViewById(R.id.pinned_header);
        this.mSpinnerHeader = activity.getLayoutInflater().inflate(R.layout.apps_filter_spinner, (ViewGroup) frameLayout, false);
        this.mFilterSpinner = (Spinner) this.mSpinnerHeader.findViewById(R.id.filter_spinner);
        this.mFilterAdapter = new FilterSpinnerAdapter(this);
        this.mFilterSpinner.setAdapter((SpinnerAdapter) this.mFilterAdapter);
        this.mFilterSpinner.setOnItemSelectedListener(this);
        frameLayout.addView(this.mSpinnerHeader, 0);
        AppFilterRegistry appFilterRegistry = AppFilterRegistry.getInstance();
        this.mFilterAdapter.enableFilter(appFilterRegistry.getDefaultFilterType(this.mListType));
        if (this.mListType == 0 && UserManager.get(getActivity()).getUserProfiles().size() > 1) {
            this.mFilterAdapter.enableFilter(8);
            this.mFilterAdapter.enableFilter(9);
        }
        if (this.mListType == 1) {
            this.mFilterAdapter.enableFilter(6);
            this.mFilterAdapter.enableFilter(7);
            this.mFilterAdapter.disableFilter(2);
        }
        if (this.mListType == 5) {
            this.mFilterAdapter.enableFilter(1);
        }
        ApplicationsState.AppFilter compositeFilter = getCompositeFilter(this.mListType, this.mStorageType, this.mVolumeUuid);
        if (this.mIsWorkOnly) {
            compositeFilter = new ApplicationsState.CompoundFilter(compositeFilter, appFilterRegistry.get(9).getFilter());
        }
        if (compositeFilter != null) {
            this.mApplications.setCompositeFilter(compositeFilter);
        }
    }

    static ApplicationsState.AppFilter getCompositeFilter(int i, int i2, String str) {
        ApplicationsState.VolumeFilter volumeFilter = new ApplicationsState.VolumeFilter(str);
        if (i == 3) {
            if (i2 == 1) {
                return new ApplicationsState.CompoundFilter(ApplicationsState.FILTER_AUDIO, volumeFilter);
            }
            if (i2 == 0) {
                return new ApplicationsState.CompoundFilter(ApplicationsState.FILTER_OTHER_APPS, volumeFilter);
            }
            return volumeFilter;
        }
        if (i == 9) {
            return new ApplicationsState.CompoundFilter(ApplicationsState.FILTER_GAMES, volumeFilter);
        }
        if (i == 10) {
            return new ApplicationsState.CompoundFilter(ApplicationsState.FILTER_MOVIES, volumeFilter);
        }
        if (i == 11) {
            return new ApplicationsState.CompoundFilter(ApplicationsState.FILTER_PHOTOS, volumeFilter);
        }
        return null;
    }

    @Override
    public int getMetricsCategory() {
        switch (this.mListType) {
            case 3:
                if (this.mStorageType == 1) {
                }
                break;
        }
        return 221;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateView();
        if (this.mApplications != null) {
            this.mApplications.resume(this.mSortOrder);
            this.mApplications.updateLoading();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        this.mResetAppsHelper.onSaveInstanceState(bundle);
        bundle.putInt("sortOrder", this.mSortOrder);
        bundle.putBoolean("showSystem", this.mShowSystem);
        bundle.putBoolean("hasEntries", this.mApplications.mHasReceivedLoadEntries);
        bundle.putBoolean("hasBridge", this.mApplications.mHasReceivedBridgeCallback);
        if (this.mApplications != null) {
            this.mApplications.onSaveInstanceState(bundle);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.mApplications != null) {
            this.mApplications.pause();
        }
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
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1 && this.mCurrentPkgName != null) {
            if (this.mListType != 1) {
                if (this.mListType != 5 && this.mListType != 6 && this.mListType != 7) {
                    this.mApplicationsState.requestSize(this.mCurrentPkgName, UserHandle.getUserId(this.mCurrentUid));
                    return;
                } else {
                    this.mApplications.mExtraInfoBridge.forceUpdate(this.mCurrentPkgName, this.mCurrentUid);
                    return;
                }
            }
            this.mApplications.mExtraInfoBridge.forceUpdate(this.mCurrentPkgName, this.mCurrentUid);
        }
    }

    private void startApplicationDetailsActivity() {
        int i = this.mListType;
        if (i == 1) {
            startAppInfoFragment(AppNotificationSettings.class, R.string.notifications_title);
        }
        switch (i) {
            case 3:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_settings);
                break;
            case 4:
                startAppInfoFragment(UsageAccessDetails.class, R.string.usage_access);
                break;
            case 5:
                HighPowerDetail.show(this, this.mCurrentUid, this.mCurrentPkgName, 1);
                break;
            case 6:
                startAppInfoFragment(DrawOverlayDetails.class, R.string.overlay_settings);
                break;
            case 7:
                startAppInfoFragment(WriteSettingsDetails.class, R.string.write_system_settings);
                break;
            case 8:
                startAppInfoFragment(ExternalSourcesDetails.class, R.string.install_other_apps);
                break;
            case 9:
                startAppInfoFragment(AppStorageSettings.class, R.string.game_storage_settings);
                break;
            case AccessPoint.Speed.MODERATE:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_movies_tv);
                break;
            case 11:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_photos_videos);
                break;
            case 12:
                startAppInfoFragment(DirectoryAccessDetails.class, R.string.directory_access);
                break;
            case 13:
                startAppInfoFragment(ChangeWifiStateDetails.class, R.string.change_wifi_state_title);
                break;
            default:
                startAppInfoFragment(AppInfoDashboardFragment.class, R.string.application_info_label);
                break;
        }
    }

    private void startAppInfoFragment(Class<?> cls, int i) {
        AppInfoBase.startAppInfoFragment(cls, i, this.mCurrentPkgName, this.mCurrentUid, this, 1, getMetricsCategory());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        HelpUtils.prepareHelpMenuItem(activity, menu, getHelpResource(), getClass().getName());
        this.mOptionsMenu = menu;
        menuInflater.inflate(R.menu.manage_apps, menu);
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

    int getHelpResource() {
        if (this.mListType == 0) {
            return R.string.help_uri_apps;
        }
        if (this.mListType == 4) {
            return R.string.help_url_usage_access;
        }
        return R.string.help_uri_notifications;
    }

    void updateOptionsMenu() {
        if (this.mOptionsMenu == null) {
            return;
        }
        this.mOptionsMenu.findItem(R.id.advanced).setVisible(false);
        this.mOptionsMenu.findItem(R.id.sort_order_alpha).setVisible(this.mListType == 3 && this.mSortOrder != R.id.sort_order_alpha);
        this.mOptionsMenu.findItem(R.id.sort_order_size).setVisible(this.mListType == 3 && this.mSortOrder != R.id.sort_order_size);
        this.mOptionsMenu.findItem(R.id.show_system).setVisible((this.mShowSystem || this.mListType == 5) ? false : true);
        this.mOptionsMenu.findItem(R.id.hide_system).setVisible(this.mShowSystem && this.mListType != 5);
        this.mOptionsMenu.findItem(R.id.reset_app_preferences).setVisible(this.mListType == 0);
        this.mOptionsMenu.findItem(R.id.sort_order_recent_notification).setVisible(false);
        this.mOptionsMenu.findItem(R.id.sort_order_frequent_notification).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        switch (menuItem.getItemId()) {
            case R.id.advanced:
                if (this.mListType == 1) {
                    new SubSettingLauncher(getContext()).setDestination(ConfigureNotificationSettings.class.getName()).setTitle(R.string.configure_notification_settings).setSourceMetricsCategory(getMetricsCategory()).setResultListener(this, 2).launch();
                } else {
                    new SubSettingLauncher(getContext()).setDestination(DefaultAppSettings.class.getName()).setTitle(R.string.configure_apps).setSourceMetricsCategory(getMetricsCategory()).setResultListener(this, 2).launch();
                }
                return true;
            case R.id.hide_system:
            case R.id.show_system:
                this.mShowSystem = !this.mShowSystem;
                this.mApplications.rebuild();
                break;
            case R.id.reset_app_preferences:
                this.mResetAppsHelper.buildResetDialog();
                return true;
            case R.id.sort_order_alpha:
            case R.id.sort_order_size:
                if (this.mApplications != null) {
                    this.mApplications.rebuild(itemId);
                }
                break;
            default:
                return false;
        }
        updateOptionsMenu();
        return true;
    }

    @Override
    public void onClick(View view) {
        if (this.mApplications == null) {
            return;
        }
        int childAdapterPosition = this.mRecyclerView.getChildAdapterPosition(view);
        if (childAdapterPosition == -1) {
            Log.w("ManageApplications", "Cannot find position for child, skipping onClick handling");
            return;
        }
        if (this.mApplications.getApplicationCount() > childAdapterPosition) {
            ApplicationsState.AppEntry appEntry = this.mApplications.getAppEntry(childAdapterPosition);
            this.mCurrentPkgName = appEntry.info.packageName;
            this.mCurrentUid = appEntry.info.uid;
            startApplicationDetailsActivity();
            return;
        }
        this.mApplications.mExtraViewController.onClick(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long j) {
        this.mFilter = this.mFilterAdapter.getFilter(i);
        this.mApplications.setFilter(this.mFilter);
        if (DEBUG) {
            Log.d("ManageApplications", "Selecting filter " + this.mFilter);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    public void updateView() {
        updateOptionsMenu();
        Activity activity = getActivity();
        if (activity != null) {
            activity.invalidateOptionsMenu();
        }
    }

    public void setHasDisabled(boolean z) {
        if (this.mListType != 0) {
            return;
        }
        this.mFilterAdapter.setFilterEnabled(3, z);
        this.mFilterAdapter.setFilterEnabled(5, z);
    }

    public void setHasInstant(boolean z) {
        if (LIST_TYPES_WITH_INSTANT.contains(Integer.valueOf(this.mListType))) {
            this.mFilterAdapter.setFilterEnabled(4, z);
        }
    }

    static class FilterSpinnerAdapter extends ArrayAdapter<CharSequence> {
        private final Context mContext;
        private final ArrayList<AppFilterItem> mFilterOptions;
        private final ManageApplications mManageApplications;

        public FilterSpinnerAdapter(ManageApplications manageApplications) {
            super(manageApplications.getContext(), R.layout.filter_spinner_item);
            this.mFilterOptions = new ArrayList<>();
            this.mContext = manageApplications.getContext();
            this.mManageApplications = manageApplications;
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }

        public AppFilterItem getFilter(int i) {
            return this.mFilterOptions.get(i);
        }

        public void setFilterEnabled(int i, boolean z) {
            if (z) {
                enableFilter(i);
            } else {
                disableFilter(i);
            }
        }

        public void enableFilter(int i) {
            int i2;
            AppFilterItem appFilterItem = AppFilterRegistry.getInstance().get(i);
            if (this.mFilterOptions.contains(appFilterItem)) {
                return;
            }
            if (ManageApplications.DEBUG) {
                Log.d("ManageApplications", "Enabling filter " + appFilterItem);
            }
            this.mFilterOptions.add(appFilterItem);
            Collections.sort(this.mFilterOptions);
            View view = this.mManageApplications.mSpinnerHeader;
            if (this.mFilterOptions.size() > 1) {
                i2 = 0;
            } else {
                i2 = 8;
            }
            view.setVisibility(i2);
            notifyDataSetChanged();
            if (this.mFilterOptions.size() == 1) {
                if (ManageApplications.DEBUG) {
                    Log.d("ManageApplications", "Auto selecting filter " + appFilterItem);
                }
                this.mManageApplications.mFilterSpinner.setSelection(0);
                this.mManageApplications.onItemSelected(null, null, 0, 0L);
            }
        }

        public void disableFilter(int i) {
            int i2;
            AppFilterItem appFilterItem = AppFilterRegistry.getInstance().get(i);
            if (!this.mFilterOptions.remove(appFilterItem)) {
                return;
            }
            if (ManageApplications.DEBUG) {
                Log.d("ManageApplications", "Disabling filter " + appFilterItem);
            }
            Collections.sort(this.mFilterOptions);
            View view = this.mManageApplications.mSpinnerHeader;
            if (this.mFilterOptions.size() > 1) {
                i2 = 0;
            } else {
                i2 = 8;
            }
            view.setVisibility(i2);
            notifyDataSetChanged();
            if (this.mManageApplications.mFilter == appFilterItem && this.mFilterOptions.size() > 0) {
                if (ManageApplications.DEBUG) {
                    Log.d("ManageApplications", "Auto selecting filter " + this.mFilterOptions.get(0));
                }
                this.mManageApplications.mFilterSpinner.setSelection(0);
                this.mManageApplications.onItemSelected(null, null, 0, 0L);
            }
        }

        @Override
        public int getCount() {
            return this.mFilterOptions.size();
        }

        @Override
        public CharSequence getItem(int i) {
            return this.mContext.getText(this.mFilterOptions.get(i).getTitle());
        }
    }

    static class ApplicationsAdapter extends RecyclerView.Adapter<ApplicationViewHolder> implements AppStateBaseBridge.Callback, ApplicationsState.Callbacks {
        private AppFilterItem mAppFilter;
        private ApplicationsState.AppFilter mCompositeFilter;
        private final Context mContext;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private final AppStateBaseBridge mExtraInfoBridge;
        private FileViewHolderController mExtraViewController;
        private boolean mHasReceivedBridgeCallback;
        private boolean mHasReceivedLoadEntries;
        private int mLastIndex;
        private final LoadingViewController mLoadingViewController;
        private final ManageApplications mManageApplications;
        OnScrollListener mOnScrollListener;
        private RecyclerView mRecyclerView;
        private boolean mResumed;
        private final ApplicationsState.Session mSession;
        private final ApplicationsState mState;
        private int mLastSortMode = -1;
        private int mWhichSize = 0;

        public ApplicationsAdapter(ApplicationsState applicationsState, ManageApplications manageApplications, AppFilterItem appFilterItem, Bundle bundle) {
            this.mLastIndex = -1;
            setHasStableIds(true);
            this.mState = applicationsState;
            this.mSession = applicationsState.newSession(this);
            this.mManageApplications = manageApplications;
            this.mLoadingViewController = new LoadingViewController(this.mManageApplications.mLoadingContainer, this.mManageApplications.mListContainer);
            this.mContext = manageApplications.getActivity();
            this.mAppFilter = appFilterItem;
            if (this.mManageApplications.mListType == 1) {
                this.mExtraInfoBridge = new AppStateNotificationBridge(this.mContext, this.mState, this, manageApplications.mUsageStatsManager, manageApplications.mUserManager, manageApplications.mNotificationBackend);
            } else if (this.mManageApplications.mListType == 4) {
                this.mExtraInfoBridge = new AppStateUsageBridge(this.mContext, this.mState, this);
            } else if (this.mManageApplications.mListType == 5) {
                this.mExtraInfoBridge = new AppStatePowerBridge(this.mContext, this.mState, this);
            } else if (this.mManageApplications.mListType == 6) {
                this.mExtraInfoBridge = new AppStateOverlayBridge(this.mContext, this.mState, this);
            } else if (this.mManageApplications.mListType == 7) {
                this.mExtraInfoBridge = new AppStateWriteSettingsBridge(this.mContext, this.mState, this);
            } else if (this.mManageApplications.mListType == 8) {
                this.mExtraInfoBridge = new AppStateInstallAppsBridge(this.mContext, this.mState, this);
            } else if (this.mManageApplications.mListType == 12) {
                this.mExtraInfoBridge = new AppStateDirectoryAccessBridge(this.mState, this);
            } else if (this.mManageApplications.mListType == 13) {
                this.mExtraInfoBridge = new AppStateChangeWifiStateBridge(this.mContext, this.mState, this);
            } else {
                this.mExtraInfoBridge = null;
            }
            if (bundle != null) {
                this.mLastIndex = bundle.getInt("state_last_scroll_index");
            }
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            this.mRecyclerView = recyclerView;
            this.mOnScrollListener = new OnScrollListener(this);
            this.mRecyclerView.addOnScrollListener(this.mOnScrollListener);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            this.mRecyclerView.removeOnScrollListener(this.mOnScrollListener);
            this.mOnScrollListener = null;
            this.mRecyclerView = null;
        }

        public void setCompositeFilter(ApplicationsState.AppFilter appFilter) {
            this.mCompositeFilter = appFilter;
            rebuild();
        }

        public void setFilter(AppFilterItem appFilterItem) {
            this.mAppFilter = appFilterItem;
            if (7 == appFilterItem.getFilterType()) {
                rebuild(R.id.sort_order_frequent_notification);
            } else if (6 == appFilterItem.getFilterType()) {
                rebuild(R.id.sort_order_recent_notification);
            } else {
                rebuild();
            }
        }

        public void setExtraViewController(FileViewHolderController fileViewHolderController) {
            this.mExtraViewController = fileViewHolderController;
            ThreadUtils.postOnBackgroundThread(new Runnable() {
                @Override
                public final void run() {
                    ManageApplications.ApplicationsAdapter.lambda$setExtraViewController$1(this.f$0);
                }
            });
        }

        public static void lambda$setExtraViewController$1(final ApplicationsAdapter applicationsAdapter) {
            applicationsAdapter.mExtraViewController.queryStats();
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.onExtraViewCompleted();
                }
            });
        }

        public void resume(int i) {
            if (ManageApplications.DEBUG) {
                Log.i("ManageApplications", "Resume!  mResumed=" + this.mResumed);
            }
            if (!this.mResumed) {
                this.mResumed = true;
                this.mSession.onResume();
                this.mLastSortMode = i;
                if (this.mExtraInfoBridge != null) {
                    this.mExtraInfoBridge.resume();
                }
                rebuild();
                return;
            }
            rebuild(i);
        }

        public void pause() {
            if (this.mResumed) {
                this.mResumed = false;
                this.mSession.onPause();
                if (this.mExtraInfoBridge != null) {
                    this.mExtraInfoBridge.pause();
                }
            }
        }

        public void onSaveInstanceState(Bundle bundle) {
            bundle.putInt("state_last_scroll_index", ((LinearLayoutManager) this.mManageApplications.mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        }

        public void release() {
            this.mSession.onDestroy();
            if (this.mExtraInfoBridge != null) {
                this.mExtraInfoBridge.release();
            }
        }

        public void rebuild(int i) {
            if (i == this.mLastSortMode) {
                return;
            }
            this.mManageApplications.mSortOrder = i;
            this.mLastSortMode = i;
            rebuild();
        }

        @Override
        public ApplicationViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View viewNewView;
            if (this.mManageApplications.mListType == 1) {
                viewNewView = ApplicationViewHolder.newView(viewGroup, true);
            } else {
                viewNewView = ApplicationViewHolder.newView(viewGroup, false);
            }
            return new ApplicationViewHolder(viewNewView, shouldUseStableItemHeight(this.mManageApplications.mListType));
        }

        @Override
        public int getItemViewType(int i) {
            return (hasExtraView() && (getItemCount() - 1 == i)) ? 1 : 0;
        }

        public void rebuild() {
            ApplicationsState.AppFilter compoundFilter;
            final Comparator<ApplicationsState.AppEntry> comparator;
            if (this.mHasReceivedLoadEntries) {
                if (this.mExtraInfoBridge != null && !this.mHasReceivedBridgeCallback) {
                    return;
                }
                if (Environment.isExternalStorageEmulated()) {
                    this.mWhichSize = 0;
                } else {
                    this.mWhichSize = 1;
                }
                ApplicationsState.AppFilter filter = this.mAppFilter.getFilter();
                if (this.mCompositeFilter != null) {
                    filter = new ApplicationsState.CompoundFilter(filter, this.mCompositeFilter);
                }
                if (!this.mManageApplications.mShowSystem) {
                    if (ManageApplications.LIST_TYPES_WITH_INSTANT.contains(Integer.valueOf(this.mManageApplications.mListType))) {
                        compoundFilter = new ApplicationsState.CompoundFilter(filter, ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER_AND_INSTANT);
                    } else {
                        compoundFilter = new ApplicationsState.CompoundFilter(filter, ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER);
                    }
                } else {
                    compoundFilter = filter;
                }
                switch (this.mLastSortMode) {
                    case R.id.sort_order_frequent_notification:
                        comparator = AppStateNotificationBridge.FREQUENCY_NOTIFICATION_COMPARATOR;
                        break;
                    case R.id.sort_order_recent_notification:
                        comparator = AppStateNotificationBridge.RECENT_NOTIFICATION_COMPARATOR;
                        break;
                    case R.id.sort_order_size:
                        switch (this.mWhichSize) {
                            case 1:
                                comparator = ApplicationsState.INTERNAL_SIZE_COMPARATOR;
                                break;
                            case 2:
                                comparator = ApplicationsState.EXTERNAL_SIZE_COMPARATOR;
                                break;
                            default:
                                comparator = ApplicationsState.SIZE_COMPARATOR;
                                break;
                        }
                        break;
                    default:
                        comparator = ApplicationsState.ALPHA_COMPARATOR;
                        break;
                }
                final ApplicationsState.CompoundFilter compoundFilter2 = new ApplicationsState.CompoundFilter(compoundFilter, ApplicationsState.FILTER_NOT_HIDE);
                ThreadUtils.postOnBackgroundThread(new Runnable() {
                    @Override
                    public final void run() {
                        ManageApplications.ApplicationsAdapter.lambda$rebuild$3(this.f$0, compoundFilter2, comparator);
                    }
                });
            }
        }

        public static void lambda$rebuild$3(final ApplicationsAdapter applicationsAdapter, ApplicationsState.AppFilter appFilter, Comparator comparator) {
            final ArrayList<ApplicationsState.AppEntry> arrayListRebuild = applicationsAdapter.mSession.rebuild(appFilter, comparator, false);
            if (arrayListRebuild != null) {
                ThreadUtils.postOnMainThread(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.onRebuildComplete(arrayListRebuild);
                    }
                });
            }
        }

        static boolean shouldUseStableItemHeight(int i) {
            if (i != 1) {
                return true;
            }
            return false;
        }

        private static boolean packageNameEquals(PackageItemInfo packageItemInfo, PackageItemInfo packageItemInfo2) {
            if (packageItemInfo == null || packageItemInfo2 == null || packageItemInfo.packageName == null || packageItemInfo2.packageName == null) {
                return false;
            }
            return packageItemInfo.packageName.equals(packageItemInfo2.packageName);
        }

        private ArrayList<ApplicationsState.AppEntry> removeDuplicateIgnoringUser(ArrayList<ApplicationsState.AppEntry> arrayList) {
            int size = arrayList.size();
            ArrayList<ApplicationsState.AppEntry> arrayList2 = new ArrayList<>(size);
            ApplicationInfo applicationInfo = null;
            int i = 0;
            while (i < size) {
                ApplicationsState.AppEntry appEntry = arrayList.get(i);
                ApplicationInfo applicationInfo2 = appEntry.info;
                if (!packageNameEquals(applicationInfo, appEntry.info)) {
                    arrayList2.add(appEntry);
                }
                i++;
                applicationInfo = applicationInfo2;
            }
            arrayList2.trimToSize();
            return arrayList2;
        }

        @Override
        public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> arrayList) {
            int filterType = this.mAppFilter.getFilterType();
            if (filterType == 0 || filterType == 1) {
                arrayList = removeDuplicateIgnoringUser(arrayList);
            }
            this.mEntries = arrayList;
            notifyDataSetChanged();
            if (getItemCount() == 0) {
                this.mManageApplications.mRecyclerView.setVisibility(8);
                this.mManageApplications.mEmptyView.setVisibility(0);
            } else {
                this.mManageApplications.mEmptyView.setVisibility(8);
                this.mManageApplications.mRecyclerView.setVisibility(0);
            }
            if (this.mLastIndex != -1 && getItemCount() > this.mLastIndex) {
                this.mManageApplications.mRecyclerView.getLayoutManager().scrollToPosition(this.mLastIndex);
                this.mLastIndex = -1;
            }
            if (this.mSession.getAllApps().size() != 0 && this.mManageApplications.mListContainer.getVisibility() != 0) {
                this.mLoadingViewController.showContent(true);
            }
            if (this.mManageApplications.mListType == 4) {
                return;
            }
            this.mManageApplications.setHasDisabled(this.mState.haveDisabledApps());
            this.mManageApplications.setHasInstant(this.mState.haveInstantApps());
        }

        void updateLoading() {
            if (this.mHasReceivedLoadEntries && this.mSession.getAllApps().size() != 0) {
                this.mLoadingViewController.showContent(false);
            } else {
                this.mLoadingViewController.showLoadingViewDelayed();
            }
        }

        @Override
        public void onExtraInfoUpdated() {
            this.mHasReceivedBridgeCallback = true;
            rebuild();
        }

        @Override
        public void onRunningStateChanged(boolean z) {
            this.mManageApplications.getActivity().setProgressBarIndeterminateVisibility(z);
        }

        @Override
        public void onPackageListChanged() {
            rebuild();
        }

        @Override
        public void onPackageIconChanged() {
        }

        @Override
        public void onLoadEntriesCompleted() {
            this.mHasReceivedLoadEntries = true;
            rebuild();
        }

        @Override
        public void onPackageSizeChanged(String str) {
            if (this.mEntries == null) {
                return;
            }
            int size = this.mEntries.size();
            for (int i = 0; i < size; i++) {
                ApplicationInfo applicationInfo = this.mEntries.get(i).info;
                if (applicationInfo != null || TextUtils.equals(str, applicationInfo.packageName)) {
                    if (TextUtils.equals(this.mManageApplications.mCurrentPkgName, applicationInfo.packageName)) {
                        rebuild();
                        return;
                    }
                    this.mOnScrollListener.postNotifyItemChange(i);
                }
            }
        }

        @Override
        public void onLauncherInfoChanged() {
            if (!this.mManageApplications.mShowSystem) {
                rebuild();
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (this.mLastSortMode == R.id.sort_order_size) {
                rebuild();
            }
        }

        public void onExtraViewCompleted() {
            if (!hasExtraView()) {
                return;
            }
            notifyItemChanged(getItemCount() - 1);
        }

        @Override
        public int getItemCount() {
            if (this.mEntries == null) {
                return 0;
            }
            return this.mEntries.size() + (hasExtraView() ? 1 : 0);
        }

        public int getApplicationCount() {
            if (this.mEntries != null) {
                return this.mEntries.size();
            }
            return 0;
        }

        public ApplicationsState.AppEntry getAppEntry(int i) {
            return this.mEntries.get(i);
        }

        @Override
        public long getItemId(int i) {
            if (i == this.mEntries.size()) {
                return -1L;
            }
            return this.mEntries.get(i).id;
        }

        public boolean isEnabled(int i) {
            if (getItemViewType(i) == 1 || this.mManageApplications.mListType != 5) {
                return true;
            }
            return !PowerWhitelistBackend.getInstance(this.mContext).isSysWhitelisted(this.mEntries.get(i).info.packageName);
        }

        @Override
        public void onBindViewHolder(ApplicationViewHolder applicationViewHolder, int i) {
            if (this.mEntries != null && this.mExtraViewController != null && i == this.mEntries.size()) {
                this.mExtraViewController.setupView(applicationViewHolder);
            } else {
                ApplicationsState.AppEntry appEntry = this.mEntries.get(i);
                synchronized (appEntry) {
                    applicationViewHolder.setTitle(appEntry.label);
                    this.mState.ensureIcon(appEntry);
                    applicationViewHolder.setIcon(appEntry.icon);
                    updateSummary(applicationViewHolder, appEntry);
                    updateSwitch(applicationViewHolder, appEntry);
                    applicationViewHolder.updateDisableView(appEntry.info);
                }
                applicationViewHolder.setEnabled(isEnabled(i));
            }
            applicationViewHolder.itemView.setOnClickListener(this.mManageApplications);
        }

        private void updateSummary(ApplicationViewHolder applicationViewHolder, ApplicationsState.AppEntry appEntry) {
            int i;
            int i2 = this.mManageApplications.mListType;
            if (i2 == 1) {
                if (appEntry.extraInfo == null) {
                    applicationViewHolder.setSummary((CharSequence) null);
                } else {
                    applicationViewHolder.setSummary(AppStateNotificationBridge.getSummary(this.mContext, (AppStateNotificationBridge.NotificationsSentState) appEntry.extraInfo, this.mLastSortMode == R.id.sort_order_recent_notification));
                    return;
                }
            }
            switch (i2) {
                case 4:
                    if (appEntry.extraInfo != null) {
                        if (new AppStateUsageBridge.UsageState((AppStateAppOpsBridge.PermissionState) appEntry.extraInfo).isPermissible()) {
                            i = R.string.app_permission_summary_allowed;
                        } else {
                            i = R.string.app_permission_summary_not_allowed;
                        }
                        applicationViewHolder.setSummary(i);
                    } else {
                        applicationViewHolder.setSummary((CharSequence) null);
                    }
                    break;
                case 5:
                    applicationViewHolder.setSummary(HighPowerDetail.getSummary(this.mContext, appEntry));
                    break;
                case 6:
                    applicationViewHolder.setSummary(DrawOverlayDetails.getSummary(this.mContext, appEntry));
                    break;
                case 7:
                    applicationViewHolder.setSummary(WriteSettingsDetails.getSummary(this.mContext, appEntry));
                    break;
                case 8:
                    applicationViewHolder.setSummary(ExternalSourcesDetails.getPreferenceSummary(this.mContext, appEntry));
                    break;
                default:
                    switch (i2) {
                        case 12:
                            applicationViewHolder.setSummary((CharSequence) null);
                            break;
                        case 13:
                            applicationViewHolder.setSummary(ChangeWifiStateDetails.getSummary(this.mContext, appEntry));
                            break;
                        default:
                            applicationViewHolder.updateSizeText(appEntry, this.mManageApplications.mInvalidSizeStr, this.mWhichSize);
                            break;
                    }
                    break;
            }
        }

        private void updateSwitch(ApplicationViewHolder applicationViewHolder, ApplicationsState.AppEntry appEntry) {
            if (this.mManageApplications.mListType == 1) {
                applicationViewHolder.updateSwitch(((AppStateNotificationBridge) this.mExtraInfoBridge).getSwitchOnClickListener(appEntry), AppStateNotificationBridge.enableSwitch(appEntry), AppStateNotificationBridge.checkSwitch(appEntry));
                if (appEntry.extraInfo == null) {
                    applicationViewHolder.setSummary((CharSequence) null);
                } else {
                    applicationViewHolder.setSummary(AppStateNotificationBridge.getSummary(this.mContext, (AppStateNotificationBridge.NotificationsSentState) appEntry.extraInfo, this.mLastSortMode == R.id.sort_order_recent_notification));
                }
            }
        }

        private boolean hasExtraView() {
            return this.mExtraViewController != null && this.mExtraViewController.shouldShow();
        }

        public static class OnScrollListener extends RecyclerView.OnScrollListener {
            private ApplicationsAdapter mAdapter;
            private boolean mDelayNotifyDataChange;
            private int mScrollState = 0;

            public OnScrollListener(ApplicationsAdapter applicationsAdapter) {
                this.mAdapter = applicationsAdapter;
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int i) {
                this.mScrollState = i;
                if (this.mScrollState == 0 && this.mDelayNotifyDataChange) {
                    this.mDelayNotifyDataChange = false;
                    this.mAdapter.notifyDataSetChanged();
                }
            }

            public void postNotifyItemChange(int i) {
                if (this.mScrollState == 0) {
                    this.mAdapter.notifyItemChanged(i);
                } else {
                    this.mDelayNotifyDataChange = true;
                }
            }
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        private SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean z) {
            if (z) {
                new InstalledAppCounter(this.mContext, -1, new PackageManagerWrapper(this.mContext.getPackageManager())) {
                    @Override
                    protected void onCountComplete(int i) {
                        SummaryProvider.this.mLoader.setSummary(SummaryProvider.this, SummaryProvider.this.mContext.getString(R.string.apps_summary, Integer.valueOf(i)));
                    }
                }.execute(new Void[0]);
            }
        }
    }
}
