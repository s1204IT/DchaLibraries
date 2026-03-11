package com.android.settings;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;
import com.android.internal.util.ArrayUtils;
import com.android.settings.ChooseLockPassword;
import com.android.settings.ChooseLockPattern;
import com.android.settings.Settings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilitySettingsForSetupWizard;
import com.android.settings.accessibility.CaptionPropertiesFragment;
import com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.ChooseAccountActivity;
import com.android.settings.accounts.ManagedProfileSettings;
import com.android.settings.applications.AdvancedAppSettings;
import com.android.settings.applications.DrawOverlayDetails;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.ManageAssist;
import com.android.settings.applications.NotificationApps;
import com.android.settings.applications.ProcessStatsSummary;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.applications.UsageAccessDetails;
import com.android.settings.applications.VrListenerSettings;
import com.android.settings.applications.WriteSettingsDetails;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.dashboard.SearchResultsSummary;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deviceinfo.ImeiInformation;
import com.android.settings.deviceinfo.PrivateVolumeForget;
import com.android.settings.deviceinfo.PrivateVolumeSettings;
import com.android.settings.deviceinfo.PublicVolumeSettings;
import com.android.settings.deviceinfo.SimStatus;
import com.android.settings.deviceinfo.Status;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageDetail;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment2;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.localepicker.LocaleListEditor;
import com.android.settings.location.LocationSettings;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.notification.NotificationStation;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenAccessSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModeEventRuleSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeScheduleRuleSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.qstile.DevelopmentTiles;
import com.android.settings.search.DynamicIndexableContentMonitor;
import com.android.settings.search.Index;
import com.android.settings.sim.SimSettings;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiAPITest;
import com.android.settings.wifi.WifiInfo;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.drawer.Tile;
import com.mediatek.audioprofile.SoundEnhancement;
import com.mediatek.hdmi.HdmiSettings;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.nfc.NfcSettings;
import com.mediatek.settings.hotknot.HotKnotSettings;
import com.mediatek.settings.wfd.WfdSinkSurfaceFragment;
import com.mediatek.wifi.WifiGprsSelector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends SettingsDrawerActivity implements PreferenceManager.OnPreferenceTreeClickListener, PreferenceFragment.OnPreferenceStartFragmentCallback, ButtonBarHandler, FragmentManager.OnBackStackChangedListener, SearchView.OnQueryTextListener, SearchView.OnCloseListener, MenuItem.OnActionExpandListener {
    private static final String[] ENTRY_FRAGMENTS = {WirelessSettings.class.getName(), WifiSettings.class.getName(), AdvancedWifiSettings.class.getName(), SavedAccessPointsWifiSettings.class.getName(), BluetoothSettings.class.getName(), SimSettings.class.getName(), TetherSettings.class.getName(), WifiP2pSettings.class.getName(), VpnSettings.class.getName(), DateTimeSettings.class.getName(), LocaleListEditor.class.getName(), InputMethodAndLanguageSettings.class.getName(), AvailableVirtualKeyboardFragment.class.getName(), SpellCheckersSettings.class.getName(), UserDictionaryList.class.getName(), UserDictionarySettings.class.getName(), HomeSettings.class.getName(), DisplaySettings.class.getName(), DeviceInfoSettings.class.getName(), ManageApplications.class.getName(), NotificationApps.class.getName(), ManageAssist.class.getName(), ProcessStatsUi.class.getName(), NotificationStation.class.getName(), LocationSettings.class.getName(), SecuritySettings.class.getName(), UsageAccessDetails.class.getName(), PrivacySettings.class.getName(), DeviceAdminSettings.class.getName(), AccessibilitySettings.class.getName(), AccessibilitySettingsForSetupWizard.class.getName(), CaptionPropertiesFragment.class.getName(), ToggleDaltonizerPreferenceFragment.class.getName(), TextToSpeechSettings.class.getName(), StorageSettings.class.getName(), PrivateVolumeForget.class.getName(), PrivateVolumeSettings.class.getName(), PublicVolumeSettings.class.getName(), DevelopmentSettings.class.getName(), AndroidBeam.class.getName(), WifiDisplaySettings.class.getName(), PowerUsageSummary.class.getName(), AccountSyncSettings.class.getName(), AccountSettings.class.getName(), CryptKeeperSettings.class.getName(), DataUsageSummary.class.getName(), DreamSettings.class.getName(), UserSettings.class.getName(), NotificationAccessSettings.class.getName(), ZenAccessSettings.class.getName(), PrintSettingsFragment.class.getName(), PrintJobSettingsFragment.class.getName(), TrustedCredentialsSettings.class.getName(), PaymentSettings.class.getName(), KeyboardLayoutPickerFragment.class.getName(), KeyboardLayoutPickerFragment2.class.getName(), PhysicalKeyboardFragment.class.getName(), ZenModeSettings.class.getName(), SoundSettings.class.getName(), ConfigureNotificationSettings.class.getName(), ChooseLockPassword.ChooseLockPasswordFragment.class.getName(), ChooseLockPattern.ChooseLockPatternFragment.class.getName(), InstalledAppDetails.class.getName(), BatterySaverSettings.class.getName(), AppNotificationSettings.class.getName(), OtherSoundSettings.class.getName(), ApnSettings.class.getName(), ApnEditor.class.getName(), WifiCallingSettings.class.getName(), ZenModePrioritySettings.class.getName(), ZenModeAutomationSettings.class.getName(), ZenModeScheduleRuleSettings.class.getName(), ZenModeEventRuleSettings.class.getName(), ZenModeVisualInterruptionSettings.class.getName(), ProcessStatsUi.class.getName(), PowerUsageDetail.class.getName(), ProcessStatsSummary.class.getName(), DrawOverlayDetails.class.getName(), WriteSettingsDetails.class.getName(), AdvancedAppSettings.class.getName(), WallpaperTypeSettings.class.getName(), VrListenerSettings.class.getName(), ManagedProfileSettings.class.getName(), ChooseAccountActivity.class.getName(), IccLockSettings.class.getName(), ImeiInformation.class.getName(), SimStatus.class.getName(), Status.class.getName(), TestingSettings.class.getName(), WifiAPITest.class.getName(), WifiInfo.class.getName(), WfdSinkSurfaceFragment.class.getName(), HotKnotSettings.class.getName(), SoundEnhancement.class.getName(), NfcSettings.class.getName(), HdmiSettings.class.getName(), WifiGprsSelector.class.getName()};
    private static final String[] LIKE_SHORTCUT_INTENT_ACTION_ARRAY = {"android.settings.APPLICATION_DETAILS_SETTINGS"};
    private ActionBar mActionBar;
    private ViewGroup mContent;
    private ComponentName mCurrentSuggestion;
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
    private String[] SETTINGS_FOR_RESTRICTED = {Settings.WifiSettingsActivity.class.getName(), Settings.BluetoothSettingsActivity.class.getName(), Settings.DataUsageSummaryActivity.class.getName(), Settings.SimSettingsActivity.class.getName(), Settings.WirelessSettingsActivity.class.getName(), Settings.HotKnotSettingsActivity.class.getName(), Settings.HomeSettingsActivity.class.getName(), Settings.SoundSettingsActivity.class.getName(), Settings.DisplaySettingsActivity.class.getName(), Settings.StorageSettingsActivity.class.getName(), Settings.ManageApplicationsActivity.class.getName(), Settings.PowerUsageSummaryActivity.class.getName(), Settings.LocationSettingsActivity.class.getName(), Settings.SecuritySettingsActivity.class.getName(), Settings.InputMethodAndLanguageSettingsActivity.class.getName(), Settings.UserSettingsActivity.class.getName(), Settings.AccountSettingsActivity.class.getName(), Settings.DateTimeSettingsActivity.class.getName(), Settings.DeviceInfoSettingsActivity.class.getName(), Settings.AccessibilitySettingsActivity.class.getName(), Settings.PrintSettingsActivity.class.getName(), Settings.PaymentSettingsActivity.class.getName()};
    private String[] EXTRA_PACKAGE_FOR_UNRESTRICTED = {"com.mediatek.schpwronoff", "com.mediatek.op09.plugin"};
    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean batteryPresent;
            String action = intent.getAction();
            if (!"android.intent.action.BATTERY_CHANGED".equals(action) || SettingsActivity.this.mBatteryPresent == (batteryPresent = Utils.isBatteryPresent(intent))) {
                return;
            }
            SettingsActivity.this.mBatteryPresent = batteryPresent;
            SettingsActivity.this.updateTilesList();
        }
    };
    private final BroadcastReceiver mUserAddRemoveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals("android.intent.action.USER_ADDED") && !action.equals("android.intent.action.USER_REMOVED")) {
                return;
            }
            Index.getInstance(SettingsActivity.this.getApplicationContext()).update();
        }
    };
    private final DynamicIndexableContentMonitor mDynamicIndexableContentMonitor = new DynamicIndexableContentMonitor();
    private int mMainContentId = R.id.main_content;
    private boolean mSearchMenuItemExpanded = false;
    private ArrayList<DashboardCategory> mCategories = new ArrayList<>();
    private boolean mNeedToRevertToInitialFragment = false;

    public SwitchBar getSwitchBar() {
        return this.mSwitchBar;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        CharSequence title = pref.getTitle();
        if (pref.getFragment().equals(WallpaperTypeSettings.class.getName())) {
            title = getString(R.string.wallpaper_settings_fragment_title);
        }
        startPreferencePanel(pref.getFragment(), pref.getExtras(), -1, title, null, 0);
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Index.getInstance(this).update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return !this.mDisplaySearch ? false : false;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (name.equals(getPackageName() + "_preferences")) {
            return new SharedPreferencesLogger(this, getMetricsTag());
        }
        return super.getSharedPreferences(name, mode);
    }

    private String getMetricsTag() {
        String tag = getClass().getName();
        if (getIntent() != null && getIntent().hasExtra(":settings:show_fragment")) {
            tag = getIntent().getStringExtra(":settings:show_fragment");
        }
        if (tag.startsWith("com.android.settings.")) {
            return tag.replace("com.android.settings.", "");
        }
        return tag;
    }

    private static boolean isShortCutIntent(Intent intent) {
        Set<String> categories = intent.getCategories();
        if (categories != null) {
            return categories.contains("com.android.settings.SHORTCUT");
        }
        return false;
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
        System.currentTimeMillis();
        if (Utils.isMonkeyRunning()) {
            finish();
        }
        getMetaData();
        Intent intent = getIntent();
        if (intent.hasExtra("settings:ui_options")) {
            getWindow().setUiOptions(intent.getIntExtra("settings:ui_options", 0));
        }
        if (intent.getBooleanExtra(":settings:hide_drawer", false)) {
            setIsDrawerPresent(false);
        }
        this.mDevelopmentPreferences = getSharedPreferences("development", 0);
        String initialFragmentName = intent.getStringExtra(":settings:show_fragment");
        this.mIsShortcut = (isShortCutIntent(intent) || isLikeShortCutIntent(intent)) ? true : intent.getBooleanExtra(":settings:show_fragment_as_shortcut", false);
        ComponentName cn = intent.getComponent();
        String className = cn.getClassName();
        this.mIsShowingDashboard = (className.equals(Settings.class.getName()) || className.equals(Settings.WirelessSettings.class.getName()) || className.equals(Settings.DeviceSettings.class.getName()) || className.equals(Settings.PersonalSettings.class.getName())) ? true : className.equals(Settings.WirelessSettings.class.getName());
        boolean isSubSettings = !(this instanceof SubSettings) ? intent.getBooleanExtra(":settings:show_fragment_as_subsetting", false) : true;
        if (isSubSettings && (themeResId = getThemeResId()) != 2131689959 && themeResId != 2131689961) {
            setTheme(R.style.Theme_SubSettings);
        }
        setContentView(this.mIsShowingDashboard ? R.layout.settings_main_dashboard : R.layout.settings_main_prefs);
        this.mContent = (ViewGroup) findViewById(this.mMainContentId);
        getFragmentManager().addOnBackStackChangedListener(this);
        if (this.mIsShowingDashboard) {
            if (Utils.isLowStorage(this)) {
                Log.w("Settings", "Cannot update the Indexer as we are running low on storage space!");
            } else {
                System.currentTimeMillis();
                Index.getInstance(getApplicationContext()).update();
            }
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
        } else if (this.mIsShowingDashboard) {
            this.mDisplayHomeAsUpEnabled = false;
            this.mDisplaySearch = Utils.isDeviceProvisioned(this);
            this.mInitialTitleResId = R.string.dashboard_title;
            switchToFragment(DashboardSummary.class.getName(), null, false, false, this.mInitialTitleResId, this.mInitialTitle, false);
        } else {
            this.mDisplaySearch = false;
            if (this.mIsShortcut) {
                this.mDisplayHomeAsUpEnabled = isSubSettings;
            } else if (isSubSettings) {
                this.mDisplayHomeAsUpEnabled = true;
            } else {
                this.mDisplayHomeAsUpEnabled = false;
            }
            setTitleFromIntent(intent);
            Bundle initialArguments = intent.getBundleExtra(":settings:show_fragment_args");
            switchToFragment(initialFragmentName, initialArguments, true, false, this.mInitialTitleResId, this.mInitialTitle, false);
        }
        this.mActionBar = getActionBar();
        if (this.mActionBar != null) {
            this.mActionBar.setDisplayHomeAsUpEnabled(this.mDisplayHomeAsUpEnabled);
            this.mActionBar.setHomeButtonEnabled(this.mDisplayHomeAsUpEnabled);
        }
        this.mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        if (this.mSwitchBar != null) {
            this.mSwitchBar.setMetricsTag(getMetricsTag());
        }
        if (!intent.getBooleanExtra("extra_prefs_show_button_bar", false) || (buttonBar = findViewById(R.id.button_bar)) == null) {
            return;
        }
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

    protected void setMainContentId(int contentId) {
        this.mMainContentId = contentId;
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
        if (title == null) {
            return;
        }
        setTitle(title);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mCategories.size() > 0) {
            outState.putParcelableArrayList(":settings:categories", this.mCategories);
        }
        outState.putBoolean(":settings:show_home_as_up", this.mDisplayHomeAsUpEnabled);
        outState.putBoolean(":settings:show_search", this.mDisplaySearch);
        if (!this.mDisplaySearch) {
            return;
        }
        outState.putBoolean(":settings:search_menu_expanded", this.mSearchMenuItem != null ? this.mSearchMenuItem.isActionViewExpanded() : false);
        String query = this.mSearchView != null ? this.mSearchView.getQuery().toString() : "";
        outState.putString(":settings:search_query", query);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.mNeedToRevertToInitialFragment) {
            revertToInitialFragment();
        }
        this.mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                SettingsActivity.this.updateTilesList();
            }
        };
        this.mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(this.mDevelopmentPreferencesListener);
        registerReceiver(this.mBatteryInfoReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        registerReceiver(this.mUserAddRemoveReceiver, new IntentFilter("android.intent.action.USER_ADDED"));
        registerReceiver(this.mUserAddRemoveReceiver, new IntentFilter("android.intent.action.USER_REMOVED"));
        this.mDynamicIndexableContentMonitor.register(this, 1);
        if (this.mDisplaySearch && !TextUtils.isEmpty(this.mSearchQuery)) {
            onQueryTextSubmit(this.mSearchQuery);
        }
        updateTilesList();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(this.mBatteryInfoReceiver);
        unregisterReceiver(this.mUserAddRemoveReceiver);
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
        if (startingFragment != null) {
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
        return superIntent;
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
        if (userHandle.getIdentifier() == UserHandle.myUserId()) {
            startPreferencePanel(fragmentClass, args, titleRes, titleText, null, 0);
            return;
        }
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
        transaction.replace(this.mMainContentId, fragment);
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
        transaction.replace(this.mMainContentId, f);
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

    public void updateTilesList() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                SettingsActivity.this.doUpdateTilesList();
            }
        });
    }

    public void doUpdateTilesList() {
        PackageManager pm = getPackageManager();
        UserManager um = UserManager.get(this);
        boolean isAdmin = um.isAdminUser();
        String packageName = getPackageName();
        setTileEnabled(new ComponentName(packageName, Settings.WifiSettingsActivity.class.getName()), pm.hasSystemFeature("android.hardware.wifi"), isAdmin, pm);
        setTileEnabled(new ComponentName(packageName, Settings.BluetoothSettingsActivity.class.getName()), pm.hasSystemFeature("android.hardware.bluetooth"), isAdmin, pm);
        setTileEnabled(new ComponentName(packageName, Settings.DataUsageSummaryActivity.class.getName()), Utils.isBandwidthControlEnabled(), isAdmin, pm);
        setTileEnabled(new ComponentName(packageName, Settings.SimSettingsActivity.class.getName()), Utils.showSimCardTile(this), isAdmin, pm);
        setTileEnabled(new ComponentName(packageName, Settings.PowerUsageSummaryActivity.class.getName()), this.mBatteryPresent, isAdmin, pm);
        ComponentName componentName = new ComponentName(packageName, Settings.UserSettingsActivity.class.getName());
        boolean z = UserManager.supportsMultipleUsers() && !Utils.isMonkeyRunning();
        setTileEnabled(componentName, z, isAdmin, pm);
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        int hceFlg = Settings.Global.getInt(getContentResolver(), "nfc_hce_on", 0);
        ComponentName componentName2 = new ComponentName(packageName, Settings.PaymentSettingsActivity.class.getName());
        boolean z2 = pm.hasSystemFeature("android.hardware.nfc") && pm.hasSystemFeature("android.hardware.nfc.hce") && nfcAdapter != null && nfcAdapter.isEnabled() && hceFlg == 1;
        setTileEnabled(componentName2, z2, isAdmin, pm);
        setTileEnabled(new ComponentName(packageName, Settings.PrintSettingsActivity.class.getName()), pm.hasSystemFeature("android.software.print"), isAdmin, pm);
        boolean showDev = this.mDevelopmentPreferences.getBoolean("show", Build.TYPE.equals("eng")) && !um.hasUserRestriction("no_debugging_features");
        setTileEnabled(new ComponentName(packageName, Settings.DevelopmentSettingsActivity.class.getName()), showDev, isAdmin, pm);
        DevelopmentTiles.setTilesEnabled(this, showDev);
        HotKnotAdapter hotKnotAdapter = HotKnotAdapter.getDefaultAdapter(this);
        setTileEnabled(new ComponentName(packageName, Settings.HotKnotSettingsActivity.class.getName()), hotKnotAdapter != null, isAdmin, pm);
        if (isAdmin) {
            return;
        }
        List<DashboardCategory> categories = getDashboardCategories();
        for (DashboardCategory category : categories) {
            for (Tile tile : category.tiles) {
                ComponentName component = tile.intent.getComponent();
                if (packageName.equals(component.getPackageName()) && !ArrayUtils.contains(this.SETTINGS_FOR_RESTRICTED, component.getClassName())) {
                    setTileEnabled(component, false, isAdmin, pm);
                } else if (ArrayUtils.contains(this.EXTRA_PACKAGE_FOR_UNRESTRICTED, component.getPackageName())) {
                    setTileEnabled(component, false, isAdmin, pm);
                }
            }
        }
    }

    private void setTileEnabled(ComponentName component, boolean enabled, boolean isAdmin, PackageManager pm) {
        if (!isAdmin && getPackageName().equals(component.getPackageName()) && !ArrayUtils.contains(this.SETTINGS_FOR_RESTRICTED, component.getClassName())) {
            enabled = false;
        }
        setTileEnabled(component, enabled);
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(), 128);
            if (ai == null || ai.metaData == null) {
                return;
            }
            this.mFragmentClass = ai.metaData.getString("com.android.settings.FRAGMENT_CLASS");
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

    @Override
    protected void onTileClicked(Tile tile) {
        if (this.mIsShowingDashboard) {
            openTile(tile);
        } else {
            super.onTileClicked(tile);
        }
    }

    @Override
    public void onProfileTileOpen() {
        if (this.mIsShowingDashboard) {
            return;
        }
        finish();
    }

    private void switchToSearchResultsFragmentIfNeeded() {
        if (this.mSearchResultsFragment != null) {
            return;
        }
        Fragment current = getFragmentManager().findFragmentById(this.mMainContentId);
        if (current != null && (current instanceof SearchResultsSummary)) {
            this.mSearchResultsFragment = (SearchResultsSummary) current;
        } else {
            this.mSearchResultsFragment = (SearchResultsSummary) switchToFragment(SearchResultsSummary.class.getName(), null, false, true, R.string.search_results_title, null, true);
        }
        this.mSearchResultsFragment.setSearchView(this.mSearchView);
        this.mSearchMenuItemExpanded = true;
    }

    public void needToRevertToInitialFragment() {
        this.mNeedToRevertToInitialFragment = true;
    }

    private void revertToInitialFragment() {
        this.mNeedToRevertToInitialFragment = false;
        this.mSearchResultsFragment = null;
        this.mSearchMenuItemExpanded = false;
        getFragmentManager().popBackStackImmediate(":settings:prefs", 1);
        if (this.mSearchMenuItem == null) {
            return;
        }
        this.mSearchMenuItem.collapseActionView();
    }

    public Intent getResultIntentData() {
        return this.mResultIntentData;
    }

    public void startSuggestion(Intent intent) {
        this.mCurrentSuggestion = intent.getComponent();
        try {
            startActivityForResult(intent, 42);
        } catch (ActivityNotFoundException e) {
            Log.w("Settings", "ActivityNotFoundException", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 42 && this.mCurrentSuggestion != null && resultCode != 0) {
            getPackageManager().setComponentEnabledSetting(this.mCurrentSuggestion, 2, 1);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
