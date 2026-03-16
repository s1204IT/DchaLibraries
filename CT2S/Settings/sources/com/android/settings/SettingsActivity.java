package com.android.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.settings.ChooseLockPassword;
import com.android.settings.ChooseLockPattern;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.CaptionPropertiesFragment;
import com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.dashboard.DashboardCategory;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.dashboard.DashboardTile;
import com.android.settings.dashboard.NoHomeDialogFragment;
import com.android.settings.dashboard.SearchResultsSummary;
import com.android.settings.deviceinfo.Memory;
import com.android.settings.deviceinfo.UsbSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.location.LocationSettings;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ConditionProviderSettings;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.notification.NotificationAppList;
import com.android.settings.notification.NotificationSettings;
import com.android.settings.notification.NotificationStation;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.quicklaunch.QuickLaunchSettings;
import com.android.settings.search.DynamicIndexableContentMonitor;
import com.android.settings.search.Index;
import com.android.settings.sim.SimSettings;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.voice.VoiceInputSettings;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParserException;

public class SettingsActivity extends Activity implements FragmentManager.OnBackStackChangedListener, PreferenceFragment.OnPreferenceStartFragmentCallback, PreferenceManager.OnPreferenceTreeClickListener, MenuItem.OnActionExpandListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener, ButtonBarHandler {
    private ActionBar mActionBar;
    private ViewGroup mContent;
    private SharedPreferences mDevelopmentPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;
    private boolean mDisplayHomeAsUpEnabled;
    private boolean mDisplaySearch;
    private String mFragmentClass;
    private CharSequence mInitialTitle;
    private int mInitialTitleResId;
    private boolean mIsShortcut;
    private boolean mIsShowingDashboard;
    private Button mNextButton;
    private Intent mResultIntentData;
    private MenuItem mSearchMenuItem;
    private String mSearchQuery;
    private SearchResultsSummary mSearchResultsFragment;
    private SearchView mSearchView;
    private SwitchBar mSwitchBar;
    private static boolean sShowNoHomeNotice = false;
    private static final String[] ENTRY_FRAGMENTS = {WirelessSettings.class.getName(), WifiSettings.class.getName(), AdvancedWifiSettings.class.getName(), SavedAccessPointsWifiSettings.class.getName(), BluetoothSettings.class.getName(), SimSettings.class.getName(), TetherSettings.class.getName(), WifiP2pSettings.class.getName(), VpnSettings.class.getName(), DateTimeSettings.class.getName(), LocalePicker.class.getName(), InputMethodAndLanguageSettings.class.getName(), VoiceInputSettings.class.getName(), SpellCheckersSettings.class.getName(), UserDictionaryList.class.getName(), UserDictionarySettings.class.getName(), HomeSettings.class.getName(), DisplaySettings.class.getName(), DeviceInfoSettings.class.getName(), ManageApplications.class.getName(), ProcessStatsUi.class.getName(), NotificationStation.class.getName(), LocationSettings.class.getName(), SecuritySettings.class.getName(), UsageAccessSettings.class.getName(), PrivacySettings.class.getName(), DeviceAdminSettings.class.getName(), AccessibilitySettings.class.getName(), CaptionPropertiesFragment.class.getName(), ToggleDaltonizerPreferenceFragment.class.getName(), TextToSpeechSettings.class.getName(), Memory.class.getName(), DevelopmentSettings.class.getName(), UsbSettings.class.getName(), AndroidBeam.class.getName(), WifiDisplaySettings.class.getName(), PowerUsageSummary.class.getName(), AccountSyncSettings.class.getName(), AccountSettings.class.getName(), CryptKeeperSettings.class.getName(), DataUsageSummary.class.getName(), DreamSettings.class.getName(), UserSettings.class.getName(), NotificationAccessSettings.class.getName(), ConditionProviderSettings.class.getName(), PrintSettingsFragment.class.getName(), PrintJobSettingsFragment.class.getName(), TrustedCredentialsSettings.class.getName(), PaymentSettings.class.getName(), KeyboardLayoutPickerFragment.class.getName(), ZenModeSettings.class.getName(), NotificationSettings.class.getName(), ChooseLockPassword.ChooseLockPasswordFragment.class.getName(), ChooseLockPattern.ChooseLockPatternFragment.class.getName(), InstalledAppDetails.class.getName(), BatterySaverSettings.class.getName(), NotificationAppList.class.getName(), AppNotificationSettings.class.getName(), OtherSoundSettings.class.getName(), QuickLaunchSettings.class.getName(), ApnSettings.class.getName()};
    private static final String[] LIKE_SHORTCUT_INTENT_ACTION_ARRAY = {"android.settings.APPLICATION_DETAILS_SETTINGS"};
    private int[] SETTINGS_FOR_RESTRICTED = {R.id.wireless_section, R.id.wifi_settings, R.id.bluetooth_settings, R.id.data_usage_settings, R.id.sim_settings, R.id.wireless_settings, R.id.device_section, R.id.notification_settings, R.id.display_settings, R.id.storage_settings, R.id.application_settings, R.id.battery_settings, R.id.personal_section, R.id.location_settings, R.id.security_settings, R.id.language_settings, R.id.user_settings, R.id.account_settings, R.id.system_section, R.id.date_time_settings, R.id.about_settings, R.id.accessibility_settings, R.id.print_settings, R.id.nfc_payment_settings, R.id.home_settings, R.id.dashboard};
    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean batteryPresent;
            String action = intent.getAction();
            if ("android.intent.action.BATTERY_CHANGED".equals(action) && SettingsActivity.this.mBatteryPresent != (batteryPresent = Utils.isBatteryPresent(intent))) {
                SettingsActivity.this.mBatteryPresent = batteryPresent;
                SettingsActivity.this.invalidateCategories(true);
            }
        }
    };
    private final DynamicIndexableContentMonitor mDynamicIndexableContentMonitor = new DynamicIndexableContentMonitor();
    private boolean mSearchMenuItemExpanded = false;
    private ArrayList<DashboardCategory> mCategories = new ArrayList<>();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    boolean forceRefresh = msg.getData().getBoolean("msg_data_force_refresh");
                    if (forceRefresh) {
                        SettingsActivity.this.buildDashboardCategories(SettingsActivity.this.mCategories);
                    }
                    break;
            }
        }
    };
    private boolean mNeedToRevertToInitialFragment = false;
    private int mHomeActivitiesCount = 1;

    public SwitchBar getSwitchBar() {
        return this.mSwitchBar;
    }

    public List<DashboardCategory> getDashboardCategories(boolean forceRefresh) {
        if (forceRefresh || this.mCategories.size() == 0) {
            buildDashboardCategories(this.mCategories);
        }
        return this.mCategories;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        int titleRes = pref.getTitleRes();
        if (pref.getFragment().equals(WallpaperTypeSettings.class.getName())) {
            titleRes = R.string.wallpaper_settings_fragment_title;
        } else if (pref.getFragment().equals(OwnerInfoSettings.class.getName()) && UserHandle.myUserId() != 0) {
            titleRes = UserManager.get(this).isLinkedUser() ? R.string.profile_info_settings_title : R.string.user_info_settings_title;
        }
        startPreferencePanel(pref.getFragment(), pref.getExtras(), titleRes, pref.getTitle(), null, 0);
        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return false;
    }

    private void invalidateCategories(boolean forceRefresh) {
        if (!this.mHandler.hasMessages(1)) {
            Message msg = new Message();
            msg.what = 1;
            msg.getData().putBoolean("msg_data_force_refresh", forceRefresh);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Index.getInstance(this).update();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.mNeedToRevertToInitialFragment) {
            revertToInitialFragment();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!this.mDisplaySearch) {
            return false;
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        String query = this.mSearchQuery;
        this.mSearchMenuItem = menu.findItem(R.id.search);
        this.mSearchView = (SearchView) this.mSearchMenuItem.getActionView();
        if (this.mSearchMenuItem == null || this.mSearchView == null) {
            return false;
        }
        if (this.mSearchResultsFragment != null) {
            this.mSearchResultsFragment.setSearchView(this.mSearchView);
        }
        this.mSearchMenuItem.setOnActionExpandListener(this);
        this.mSearchView.setOnQueryTextListener(this);
        this.mSearchView.setOnCloseListener(this);
        if (this.mSearchMenuItemExpanded) {
            this.mSearchMenuItem.expandActionView();
        }
        this.mSearchView.setQuery(query, true);
        return true;
    }

    private static boolean isShortCutIntent(Intent intent) {
        Set<String> categories = intent.getCategories();
        return categories != null && categories.contains("com.android.settings.SHORTCUT");
    }

    private static boolean isLikeShortCutIntent(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return false;
        }
        for (int i = 0; i < LIKE_SHORTCUT_INTENT_ACTION_ARRAY.length; i++) {
            if (LIKE_SHORTCUT_INTENT_ACTION_ARRAY[i].equals(action)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        View buttonBar;
        int themeResId;
        super.onCreate(savedState);
        getMetaData();
        Intent intent = getIntent();
        if (intent.hasExtra("settings:ui_options")) {
            getWindow().setUiOptions(intent.getIntExtra("settings:ui_options", 0));
        }
        this.mDevelopmentPreferences = getSharedPreferences("development", 0);
        String initialFragmentName = intent.getStringExtra(":settings:show_fragment");
        this.mIsShortcut = isShortCutIntent(intent) || isLikeShortCutIntent(intent) || intent.getBooleanExtra(":settings:show_fragment_as_shortcut", false);
        ComponentName cn = intent.getComponent();
        String className = cn.getClassName();
        this.mIsShowingDashboard = className.equals(Settings.class.getName());
        boolean isSubSettings = className.equals(SubSettings.class.getName()) || intent.getBooleanExtra(":settings:show_fragment_as_subsetting", false);
        if (isSubSettings && (themeResId = getThemeResId()) != 2131361864 && themeResId != 2131361866) {
            setTheme(R.style.Theme_SubSettings);
        }
        setContentView(this.mIsShowingDashboard ? R.layout.settings_main_dashboard : R.layout.settings_main_prefs);
        this.mContent = (ViewGroup) findViewById(R.id.main_content);
        getFragmentManager().addOnBackStackChangedListener(this);
        if (this.mIsShowingDashboard) {
            Index.getInstance(getApplicationContext()).update();
        }
        if (savedState != null) {
            this.mSearchMenuItemExpanded = savedState.getBoolean(":settings:search_menu_expanded");
            this.mSearchQuery = savedState.getString(":settings:search_query");
            setTitleFromIntent(intent);
            ArrayList<DashboardCategory> categories = savedState.getParcelableArrayList(":settings:categories");
            if (categories != null) {
                this.mCategories.clear();
                this.mCategories.addAll(categories);
                setTitleFromBackStack();
            }
            this.mDisplayHomeAsUpEnabled = savedState.getBoolean(":settings:show_home_as_up");
            this.mDisplaySearch = savedState.getBoolean(":settings:show_search");
            this.mHomeActivitiesCount = savedState.getInt(":settings:home_activities_count", 1);
        } else if (!this.mIsShowingDashboard) {
            if (this.mIsShortcut) {
                this.mDisplayHomeAsUpEnabled = isSubSettings;
                this.mDisplaySearch = false;
            } else if (isSubSettings) {
                this.mDisplayHomeAsUpEnabled = true;
                this.mDisplaySearch = true;
            } else {
                this.mDisplayHomeAsUpEnabled = false;
                this.mDisplaySearch = false;
            }
            setTitleFromIntent(intent);
            Bundle initialArguments = intent.getBundleExtra(":settings:show_fragment_args");
            switchToFragment(initialFragmentName, initialArguments, true, false, this.mInitialTitleResId, this.mInitialTitle, false);
        } else {
            this.mDisplayHomeAsUpEnabled = false;
            this.mDisplaySearch = true;
            this.mInitialTitleResId = R.string.dashboard_title;
            switchToFragment(DashboardSummary.class.getName(), null, false, false, this.mInitialTitleResId, this.mInitialTitle, false);
        }
        this.mActionBar = getActionBar();
        if (this.mActionBar != null) {
            this.mActionBar.setDisplayHomeAsUpEnabled(this.mDisplayHomeAsUpEnabled);
            this.mActionBar.setHomeButtonEnabled(this.mDisplayHomeAsUpEnabled);
        }
        this.mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        if (intent.getBooleanExtra("extra_prefs_show_button_bar", false) && (buttonBar = findViewById(R.id.button_bar)) != null) {
            buttonBar.setVisibility(0);
            Button backButton = (Button) findViewById(R.id.back_button);
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsActivity.this.setResult(0, SettingsActivity.this.getResultIntentData());
                    SettingsActivity.this.finish();
                }
            });
            Button skipButton = (Button) findViewById(R.id.skip_button);
            skipButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsActivity.this.setResult(-1, SettingsActivity.this.getResultIntentData());
                    SettingsActivity.this.finish();
                }
            });
            this.mNextButton = (Button) findViewById(R.id.next_button);
            this.mNextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SettingsActivity.this.setResult(-1, SettingsActivity.this.getResultIntentData());
                    SettingsActivity.this.finish();
                }
            });
            if (intent.hasExtra("extra_prefs_set_next_text")) {
                String buttonText = intent.getStringExtra("extra_prefs_set_next_text");
                if (TextUtils.isEmpty(buttonText)) {
                    this.mNextButton.setVisibility(8);
                } else {
                    this.mNextButton.setText(buttonText);
                }
            }
            if (intent.hasExtra("extra_prefs_set_back_text")) {
                String buttonText2 = intent.getStringExtra("extra_prefs_set_back_text");
                if (TextUtils.isEmpty(buttonText2)) {
                    backButton.setVisibility(8);
                } else {
                    backButton.setText(buttonText2);
                }
            }
            if (intent.getBooleanExtra("extra_prefs_show_skip", false)) {
                skipButton.setVisibility(0);
            }
        }
        this.mHomeActivitiesCount = getHomeActivitiesCount();
    }

    private int getHomeActivitiesCount() {
        ArrayList<ResolveInfo> homeApps = new ArrayList<>();
        getPackageManager().getHomeActivities(homeApps);
        return homeApps.size();
    }

    private void setTitleFromIntent(Intent intent) {
        int initialTitleResId = intent.getIntExtra(":settings:show_fragment_title_resid", -1);
        if (initialTitleResId > 0) {
            this.mInitialTitle = null;
            this.mInitialTitleResId = initialTitleResId;
            String initialTitleResPackageName = intent.getStringExtra(":settings:show_fragment_title_res_package_name");
            if (initialTitleResPackageName != null) {
                try {
                    Context authContext = createPackageContextAsUser(initialTitleResPackageName, 0, new UserHandle(UserHandle.myUserId()));
                    this.mInitialTitle = authContext.getResources().getText(this.mInitialTitleResId);
                    setTitle(this.mInitialTitle);
                    this.mInitialTitleResId = -1;
                    return;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w("Settings", "Could not find package" + initialTitleResPackageName);
                    return;
                }
            }
            setTitle(this.mInitialTitleResId);
            return;
        }
        this.mInitialTitleResId = -1;
        CharSequence stringExtra = intent.getStringExtra(":settings:show_fragment_title");
        if (stringExtra == null) {
            stringExtra = getTitle();
        }
        this.mInitialTitle = stringExtra;
        setTitle(this.mInitialTitle);
    }

    @Override
    public void onBackStackChanged() {
        setTitleFromBackStack();
    }

    private int setTitleFromBackStack() {
        int count = getFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            if (this.mInitialTitleResId > 0) {
                setTitle(this.mInitialTitleResId);
            } else {
                setTitle(this.mInitialTitle);
            }
            return 0;
        }
        FragmentManager.BackStackEntry bse = getFragmentManager().getBackStackEntryAt(count - 1);
        setTitleFromBackStackEntry(bse);
        return count;
    }

    private void setTitleFromBackStackEntry(FragmentManager.BackStackEntry bse) {
        CharSequence title;
        int titleRes = bse.getBreadCrumbTitleRes();
        if (titleRes > 0) {
            title = getText(titleRes);
        } else {
            title = bse.getBreadCrumbTitle();
        }
        if (title != null) {
            setTitle(title);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mCategories.size() > 0) {
            outState.putParcelableArrayList(":settings:categories", this.mCategories);
        }
        outState.putBoolean(":settings:show_home_as_up", this.mDisplayHomeAsUpEnabled);
        outState.putBoolean(":settings:show_search", this.mDisplaySearch);
        if (this.mDisplaySearch) {
            boolean isExpanded = this.mSearchMenuItem != null && this.mSearchMenuItem.isActionViewExpanded();
            outState.putBoolean(":settings:search_menu_expanded", isExpanded);
            String query = this.mSearchView != null ? this.mSearchView.getQuery().toString() : "";
            outState.putString(":settings:search_query", query);
        }
        outState.putInt(":settings:home_activities_count", this.mHomeActivitiesCount);
    }

    @Override
    public void onResume() {
        super.onResume();
        int newHomeActivityCount = getHomeActivitiesCount();
        if (newHomeActivityCount != this.mHomeActivitiesCount) {
            this.mHomeActivitiesCount = newHomeActivityCount;
            invalidateCategories(true);
        }
        this.mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                SettingsActivity.this.invalidateCategories(true);
            }
        };
        this.mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(this.mDevelopmentPreferencesListener);
        registerReceiver(this.mBatteryInfoReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        this.mDynamicIndexableContentMonitor.register(this);
        if (this.mDisplaySearch && !TextUtils.isEmpty(this.mSearchQuery)) {
            onQueryTextSubmit(this.mSearchQuery);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(this.mBatteryInfoReceiver);
        this.mDynamicIndexableContentMonitor.unregister();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(this.mDevelopmentPreferencesListener);
        this.mDevelopmentPreferencesListener = null;
    }

    protected boolean isValidFragment(String fragmentName) {
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Intent getIntent() {
        Bundle args;
        Intent superIntent = super.getIntent();
        String startingFragment = getStartingFragmentClass(superIntent);
        if (startingFragment == null) {
            return superIntent;
        }
        Intent modIntent = new Intent(superIntent);
        modIntent.putExtra(":settings:show_fragment", startingFragment);
        Bundle args2 = superIntent.getExtras();
        if (args2 != null) {
            args = new Bundle(args2);
        } else {
            args = new Bundle();
        }
        args.putParcelable("intent", superIntent);
        modIntent.putExtra(":settings:show_fragment_args", args);
        return modIntent;
    }

    private String getStartingFragmentClass(Intent intent) {
        if (this.mFragmentClass != null) {
            return this.mFragmentClass;
        }
        String intentClass = intent.getComponent().getClassName();
        if (intentClass.equals(getClass().getName())) {
            return null;
        }
        if ("com.android.settings.ManageApplications".equals(intentClass) || "com.android.settings.RunningServices".equals(intentClass) || "com.android.settings.applications.StorageUse".equals(intentClass)) {
            return ManageApplications.class.getName();
        }
        return intentClass;
    }

    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes, CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        String title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                title = "";
            }
        }
        Utils.startWithFragment(this, fragmentClass, args, resultTo, resultRequestCode, titleRes, title, this.mIsShortcut);
    }

    public void startPreferencePanelAsUser(String fragmentClass, Bundle args, int titleRes, CharSequence titleText, UserHandle userHandle) {
        String title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                title = "";
            }
        }
        Utils.startWithFragmentAsUser(this, fragmentClass, args, titleRes, title, this.mIsShortcut, userHandle);
    }

    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        setResult(resultCode, resultData);
        finish();
    }

    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, fragment);
        if (push) {
            transaction.setTransition(4097);
            transaction.addToBackStack(":settings:prefs");
        } else {
            transaction.setTransition(4099);
        }
        transaction.commitAllowingStateLoss();
    }

    private Fragment switchToFragment(String fragmentName, Bundle args, boolean validate, boolean addToBackStack, int titleResId, CharSequence title, boolean withTransition) {
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: " + fragmentName);
        }
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, f);
        if (withTransition) {
            TransitionManager.beginDelayedTransition(this.mContent);
        }
        if (addToBackStack) {
            transaction.addToBackStack(":settings:prefs");
        }
        if (titleResId > 0) {
            transaction.setBreadCrumbTitle(titleResId);
        } else if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        return f;
    }

    private void buildDashboardCategories(List<DashboardCategory> categories) {
        categories.clear();
        loadCategoriesFromResource(R.xml.dashboard_categories, categories);
        updateTilesList(categories);
    }

    private void loadCategoriesFromResource(int resid, List<DashboardCategory> target) {
        int type;
        XmlResourceParser parser = null;
        try {
            try {
                parser = getResources().getXml(resid);
                AttributeSet attrs = Xml.asAttributeSet(parser);
                do {
                    type = parser.next();
                    if (type == 1) {
                        break;
                    }
                } while (type != 2);
                String nodeName = parser.getName();
                if (!"dashboard-categories".equals(nodeName)) {
                    throw new RuntimeException("XML document must start with <preference-categories> tag; found" + nodeName + " at " + parser.getPositionDescription());
                }
                Bundle curBundle = null;
                int outerDepth = parser.getDepth();
                while (true) {
                    int type2 = parser.next();
                    if (type2 == 1 || (type2 == 3 && parser.getDepth() <= outerDepth)) {
                        break;
                    }
                    if (type2 != 3 && type2 != 4) {
                        if ("dashboard-category".equals(parser.getName())) {
                            DashboardCategory category = new DashboardCategory();
                            TypedArray sa = obtainStyledAttributes(attrs, com.android.internal.R.styleable.PreferenceHeader);
                            category.id = sa.getResourceId(1, -1);
                            TypedValue tv = sa.peekValue(2);
                            if (tv != null && tv.type == 3) {
                                if (tv.resourceId != 0) {
                                    category.titleRes = tv.resourceId;
                                } else {
                                    category.title = tv.string;
                                }
                            }
                            sa.recycle();
                            int innerDepth = parser.getDepth();
                            while (true) {
                                int type3 = parser.next();
                                if (type3 == 1 || (type3 == 3 && parser.getDepth() <= innerDepth)) {
                                    break;
                                }
                                if (type3 != 3 && type3 != 4) {
                                    String innerNodeName = parser.getName();
                                    if (innerNodeName.equals("dashboard-tile")) {
                                        DashboardTile tile = new DashboardTile();
                                        TypedArray sa2 = obtainStyledAttributes(attrs, com.android.internal.R.styleable.PreferenceHeader);
                                        tile.id = sa2.getResourceId(1, -1);
                                        TypedValue tv2 = sa2.peekValue(2);
                                        if (tv2 != null && tv2.type == 3) {
                                            if (tv2.resourceId != 0) {
                                                tile.titleRes = tv2.resourceId;
                                            } else {
                                                tile.title = tv2.string;
                                            }
                                        }
                                        TypedValue tv3 = sa2.peekValue(3);
                                        if (tv3 != null && tv3.type == 3) {
                                            if (tv3.resourceId != 0) {
                                                tile.summaryRes = tv3.resourceId;
                                            } else {
                                                tile.summary = tv3.string;
                                            }
                                        }
                                        tile.iconRes = sa2.getResourceId(0, 0);
                                        tile.fragment = sa2.getString(4);
                                        sa2.recycle();
                                        if (curBundle == null) {
                                            curBundle = new Bundle();
                                        }
                                        int innerDepth2 = parser.getDepth();
                                        while (true) {
                                            int type4 = parser.next();
                                            if (type4 == 1 || (type4 == 3 && parser.getDepth() <= innerDepth2)) {
                                                break;
                                            }
                                            if (type4 != 3 && type4 != 4) {
                                                String innerNodeName2 = parser.getName();
                                                if (innerNodeName2.equals("extra")) {
                                                    getResources().parseBundleExtra("extra", attrs, curBundle);
                                                    XmlUtils.skipCurrentTag(parser);
                                                } else if (innerNodeName2.equals("intent")) {
                                                    tile.intent = Intent.parseIntent(getResources(), parser, attrs);
                                                } else {
                                                    XmlUtils.skipCurrentTag(parser);
                                                }
                                            }
                                        }
                                        if (curBundle.size() > 0) {
                                            tile.fragmentArguments = curBundle;
                                            curBundle = null;
                                        }
                                        if (tile.id != 2131690058 || Utils.showSimCardTile(this)) {
                                            category.addTile(tile);
                                        }
                                    } else {
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                }
                            }
                            target.add(category);
                        } else {
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error parsing categories", e);
            } catch (XmlPullParserException e2) {
                throw new RuntimeException("Error parsing categories", e2);
            }
        } finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    private void updateTilesList(List<DashboardCategory> target) {
        NfcAdapter adapter;
        boolean showDev = this.mDevelopmentPreferences.getBoolean("show", Build.TYPE.equals("eng"));
        UserManager um = (UserManager) getSystemService("user");
        int size = target.size();
        for (int i = 0; i < size; i++) {
            DashboardCategory category = target.get(i);
            for (int n = category.getTilesCount() - 1; n >= 0; n--) {
                DashboardTile tile = category.getTile(n);
                boolean removeTile = false;
                int id = (int) tile.id;
                if (id == R.id.operator_settings || id == R.id.manufacturer_settings) {
                    if (!Utils.updateTileToSpecificActivityFromMetaDataOrRemove(this, tile)) {
                        removeTile = true;
                    }
                } else if (id == R.id.wifi_settings) {
                    if (!getPackageManager().hasSystemFeature("android.hardware.wifi")) {
                        removeTile = true;
                    }
                } else if (id == R.id.bluetooth_settings) {
                    if (!getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
                        removeTile = true;
                    }
                } else if (id == R.id.data_usage_settings) {
                    INetworkManagementService netManager = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
                    try {
                        if (!netManager.isBandwidthControlEnabled()) {
                            removeTile = true;
                        }
                    } catch (RemoteException e) {
                    }
                } else if (id == R.id.battery_settings) {
                    if (!this.mBatteryPresent) {
                        removeTile = true;
                    }
                } else if (id == R.id.home_settings) {
                    if (!updateHomeSettingTiles(tile)) {
                        removeTile = true;
                    }
                } else if (id == R.id.user_settings) {
                    boolean hasMultipleUsers = ((UserManager) getSystemService("user")).getUserCount() > 1;
                    if ((!UserManager.supportsMultipleUsers() && !hasMultipleUsers) || Utils.isMonkeyRunning()) {
                        removeTile = true;
                    }
                } else if (id == R.id.nfc_payment_settings) {
                    if (!getPackageManager().hasSystemFeature("android.hardware.nfc") || (adapter = NfcAdapter.getDefaultAdapter(this)) == null || !adapter.isEnabled() || !getPackageManager().hasSystemFeature("android.hardware.nfc.hce")) {
                        removeTile = true;
                    }
                } else if (id == R.id.print_settings) {
                    boolean hasPrintingSupport = getPackageManager().hasSystemFeature("android.software.print");
                    if (!hasPrintingSupport) {
                        removeTile = true;
                    }
                } else if (id == R.id.development_settings && (!showDev || um.hasUserRestriction("no_debugging_features"))) {
                    removeTile = true;
                }
                if (UserHandle.myUserId() != 0 && !ArrayUtils.contains(this.SETTINGS_FOR_RESTRICTED, id)) {
                    removeTile = true;
                }
                if (removeTile && n < category.getTilesCount()) {
                    category.removeTile(n);
                }
            }
        }
    }

    private boolean updateHomeSettingTiles(DashboardTile tile) {
        SharedPreferences sp = getSharedPreferences("home_prefs", 0);
        if (sp.getBoolean("do_show", false)) {
            return true;
        }
        try {
            this.mHomeActivitiesCount = getHomeActivitiesCount();
        } catch (Exception e) {
            Log.w("Settings", "Problem looking up home activity!", e);
        }
        if (this.mHomeActivitiesCount < 2) {
            if (sShowNoHomeNotice) {
                sShowNoHomeNotice = false;
                NoHomeDialogFragment.show(this);
            }
            return false;
        }
        if (tile.fragmentArguments == null) {
            tile.fragmentArguments = new Bundle();
        }
        tile.fragmentArguments.putBoolean("show", true);
        sp.edit().putBoolean("do_show", true).apply();
        return true;
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(), 128);
            if (ai != null && ai.metaData != null) {
                this.mFragmentClass = ai.metaData.getString("com.android.settings.FRAGMENT_CLASS");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("Settings", "Cannot get Metadata for: " + getComponentName().toString());
        }
    }

    @Override
    public boolean hasNextButton() {
        return this.mNextButton != null;
    }

    @Override
    public Button getNextButton() {
        return this.mNextButton;
    }

    @Override
    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return super.shouldUpRecreateTask(new Intent(this, (Class<?>) SettingsActivity.class));
    }

    public static void requestHomeNotice() {
        sShowNoHomeNotice = true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        switchToSearchResultsFragmentIfNeeded();
        this.mSearchQuery = query;
        return this.mSearchResultsFragment.onQueryTextSubmit(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        this.mSearchQuery = newText;
        if (this.mSearchResultsFragment == null) {
            return false;
        }
        return this.mSearchResultsFragment.onQueryTextChange(newText);
    }

    @Override
    public boolean onClose() {
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == this.mSearchMenuItem.getItemId()) {
            switchToSearchResultsFragmentIfNeeded();
            return true;
        }
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (item.getItemId() == this.mSearchMenuItem.getItemId() && this.mSearchMenuItemExpanded) {
            revertToInitialFragment();
            return true;
        }
        return true;
    }

    private void switchToSearchResultsFragmentIfNeeded() {
        if (this.mSearchResultsFragment == null) {
            Fragment current = getFragmentManager().findFragmentById(R.id.main_content);
            if (current != null && (current instanceof SearchResultsSummary)) {
                this.mSearchResultsFragment = (SearchResultsSummary) current;
            } else {
                this.mSearchResultsFragment = (SearchResultsSummary) switchToFragment(SearchResultsSummary.class.getName(), null, false, true, R.string.search_results_title, null, true);
            }
            this.mSearchResultsFragment.setSearchView(this.mSearchView);
            this.mSearchMenuItemExpanded = true;
        }
    }

    public void needToRevertToInitialFragment() {
        this.mNeedToRevertToInitialFragment = true;
    }

    private void revertToInitialFragment() {
        this.mNeedToRevertToInitialFragment = false;
        this.mSearchResultsFragment = null;
        this.mSearchMenuItemExpanded = false;
        getFragmentManager().popBackStackImmediate(":settings:prefs", 1);
        if (this.mSearchMenuItem != null) {
            this.mSearchMenuItem.collapseActionView();
        }
    }

    public Intent getResultIntentData() {
        return this.mResultIntentData;
    }

    public void setResultIntentData(Intent resultIntentData) {
        this.mResultIntentData = resultIntentData;
    }
}
