package com.android.settings;

import android.app.ActionBar;
import android.app.ActivityManager;
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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.FeatureFlagUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toolbar;
import com.android.internal.util.ArrayUtils;
import com.android.settings.Settings;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.backup.BackupSettingsActivity;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.gateway.SettingsGateway;
import com.android.settings.dashboard.DashboardFeatureProvider;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DeviceIndexFeatureProvider;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.instrumentation.SharedPreferencesLogger;
import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.SettingsDrawerActivity;
import com.android.settingslib.utils.ThreadUtils;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends SettingsDrawerActivity implements FragmentManager.OnBackStackChangedListener, PreferenceFragment.OnPreferenceStartFragmentCallback, PreferenceManager.OnPreferenceTreeClickListener, ButtonBarHandler {
    private ViewGroup mContent;
    private DashboardFeatureProvider mDashboardFeatureProvider;
    private BroadcastReceiver mDevelopmentSettingsListener;
    private String mFragmentClass;
    private CharSequence mInitialTitle;
    private int mInitialTitleResId;
    private boolean mIsShowingDashboard;
    private Button mNextButton;
    private SwitchBar mSwitchBar;
    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean zIsBatteryPresent;
            if ("android.intent.action.BATTERY_CHANGED".equals(intent.getAction()) && SettingsActivity.this.mBatteryPresent != (zIsBatteryPresent = Utils.isBatteryPresent(intent))) {
                SettingsActivity.this.mBatteryPresent = zIsBatteryPresent;
                SettingsActivity.this.updateTilesList();
            }
        }
    };
    private ArrayList<DashboardCategory> mCategories = new ArrayList<>();

    public SwitchBar getSwitchBar() {
        return this.mSwitchBar;
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment, Preference preference) {
        int metricsCategory;
        SubSettingLauncher arguments = new SubSettingLauncher(this).setDestination(preference.getFragment()).setArguments(preference.getExtras());
        if (preferenceFragment instanceof Instrumentable) {
            metricsCategory = ((Instrumentable) preferenceFragment).getMetricsCategory();
        } else {
            metricsCategory = 0;
        }
        arguments.setSourceMetricsCategory(metricsCategory).setTitle(-1).launch();
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public SharedPreferences getSharedPreferences(String str, int i) {
        if (str.equals(getPackageName() + "_preferences")) {
            return new SharedPreferencesLogger(this, getMetricsTag(), FeatureFactory.getFactory(this).getMetricsFeatureProvider());
        }
        return super.getSharedPreferences(str, i);
    }

    private String getMetricsTag() {
        String name = getClass().getName();
        if (getIntent() != null && getIntent().hasExtra(":settings:show_fragment")) {
            name = getIntent().getStringExtra(":settings:show_fragment");
        }
        if (name.startsWith("com.android.settings.")) {
            return name.replace("com.android.settings.", "");
        }
        return name;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        View viewFindViewById;
        int i;
        super.onCreate(bundle);
        Log.d("SettingsActivity", "Starting onCreate");
        if (isLockTaskModePinned() && !isSettingsRunOnTop() && !isLaunchableInTaskModePinned()) {
            Log.w("SettingsActivity", "Devices lock task mode pinned.");
            finish();
        }
        if (!isFinishing() && Utils.isMonkeyRunning()) {
            Log.d("SettingsActivity", "finish due to monkey user");
            finish();
            return;
        }
        System.currentTimeMillis();
        this.mDashboardFeatureProvider = FeatureFactory.getFactory(this).getDashboardFeatureProvider(this);
        getMetaData();
        Intent intent = getIntent();
        if (intent.hasExtra("settings:ui_options")) {
            getWindow().setUiOptions(intent.getIntExtra("settings:ui_options", 0));
        }
        String stringExtra = intent.getStringExtra(":settings:show_fragment");
        intent.getComponent().getClassName();
        this.mIsShowingDashboard = false;
        boolean z = (this instanceof SubSettings) || intent.getBooleanExtra(":settings:show_fragment_as_subsetting", false);
        if (z) {
            setTheme(R.style.Theme_SubSettings);
        }
        setContentView(this.mIsShowingDashboard ? R.layout.settings_main_dashboard : R.layout.settings_main_prefs);
        this.mContent = (ViewGroup) findViewById(R.id.main_content);
        getFragmentManager().addOnBackStackChangedListener(this);
        if (bundle != null) {
            setTitleFromIntent(intent);
            ArrayList parcelableArrayList = bundle.getParcelableArrayList(":settings:categories");
            if (parcelableArrayList != null) {
                this.mCategories.clear();
                this.mCategories.addAll(parcelableArrayList);
                setTitleFromBackStack();
            }
        } else {
            launchSettingFragment(stringExtra, z, intent);
        }
        boolean zIsDeviceProvisioned = Utils.isDeviceProvisioned(this);
        if (this.mIsShowingDashboard) {
            View viewFindViewById2 = findViewById(R.id.search_bar);
            if (zIsDeviceProvisioned) {
                i = 0;
            } else {
                i = 4;
            }
            viewFindViewById2.setVisibility(i);
            findViewById(R.id.action_bar).setVisibility(8);
            Toolbar toolbar = (Toolbar) findViewById(R.id.search_action_bar);
            FeatureFactory.getFactory(this).getSearchFeatureProvider().initSearchToolbar(this, toolbar);
            setActionBar(toolbar);
            View navigationView = toolbar.getNavigationView();
            navigationView.setClickable(false);
            navigationView.setImportantForAccessibility(2);
            navigationView.setBackground(null);
        }
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(zIsDeviceProvisioned);
            actionBar.setHomeButtonEnabled(zIsDeviceProvisioned);
            actionBar.setDisplayShowTitleEnabled(!this.mIsShowingDashboard);
        }
        this.mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        if (this.mSwitchBar != null) {
            this.mSwitchBar.setMetricsTag(getMetricsTag());
        }
        if (intent.getBooleanExtra("extra_prefs_show_button_bar", false) && (viewFindViewById = findViewById(R.id.button_bar)) != null) {
            viewFindViewById.setVisibility(0);
            Button button = (Button) findViewById(R.id.back_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SettingsActivity.this.setResult(0, null);
                    SettingsActivity.this.finish();
                }
            });
            Button button2 = (Button) findViewById(R.id.skip_button);
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SettingsActivity.this.setResult(-1, null);
                    SettingsActivity.this.finish();
                }
            });
            this.mNextButton = (Button) findViewById(R.id.next_button);
            this.mNextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SettingsActivity.this.setResult(-1, null);
                    SettingsActivity.this.finish();
                }
            });
            if (intent.hasExtra("extra_prefs_set_next_text")) {
                String stringExtra2 = intent.getStringExtra("extra_prefs_set_next_text");
                if (TextUtils.isEmpty(stringExtra2)) {
                    this.mNextButton.setVisibility(8);
                } else {
                    this.mNextButton.setText(stringExtra2);
                }
            }
            if (intent.hasExtra("extra_prefs_set_back_text")) {
                String stringExtra3 = intent.getStringExtra("extra_prefs_set_back_text");
                if (TextUtils.isEmpty(stringExtra3)) {
                    button.setVisibility(8);
                } else {
                    button.setText(stringExtra3);
                }
            }
            if (intent.getBooleanExtra("extra_prefs_show_skip", false)) {
                button2.setVisibility(0);
            }
        }
    }

    void launchSettingFragment(String str, boolean z, Intent intent) {
        if (!this.mIsShowingDashboard && str != null) {
            setTitleFromIntent(intent);
            switchToFragment(str, intent.getBundleExtra(":settings:show_fragment_args"), true, false, this.mInitialTitleResId, this.mInitialTitle, false);
        } else {
            this.mInitialTitleResId = R.string.dashboard_title;
            switchToFragment(DashboardSummary.class.getName(), null, false, false, this.mInitialTitleResId, this.mInitialTitle, false);
        }
    }

    private void setTitleFromIntent(Intent intent) {
        Log.d("SettingsActivity", "Starting to set activity title");
        int intExtra = intent.getIntExtra(":settings:show_fragment_title_resid", -1);
        if (intExtra > 0) {
            this.mInitialTitle = null;
            this.mInitialTitleResId = intExtra;
            String stringExtra = intent.getStringExtra(":settings:show_fragment_title_res_package_name");
            if (stringExtra != null) {
                try {
                    this.mInitialTitle = createPackageContextAsUser(stringExtra, 0, new UserHandle(UserHandle.myUserId())).getResources().getText(this.mInitialTitleResId);
                    setTitle(this.mInitialTitle);
                    this.mInitialTitleResId = -1;
                    return;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.w("SettingsActivity", "Could not find package" + stringExtra);
                }
            } else {
                setTitle(this.mInitialTitleResId);
            }
        } else {
            this.mInitialTitleResId = -1;
            CharSequence stringExtra2 = intent.getStringExtra(":settings:show_fragment_title");
            if (stringExtra2 == null) {
                stringExtra2 = getTitle();
            }
            this.mInitialTitle = stringExtra2;
            setTitle(this.mInitialTitle);
        }
        Log.d("SettingsActivity", "Done setting title");
    }

    @Override
    public void onBackStackChanged() {
        setTitleFromBackStack();
    }

    private void setTitleFromBackStack() {
        int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
        if (backStackEntryCount == 0) {
            if (this.mInitialTitleResId > 0) {
                setTitle(this.mInitialTitleResId);
                return;
            } else {
                setTitle(this.mInitialTitle);
                return;
            }
        }
        setTitleFromBackStackEntry(getFragmentManager().getBackStackEntryAt(backStackEntryCount - 1));
    }

    private void setTitleFromBackStackEntry(FragmentManager.BackStackEntry backStackEntry) {
        CharSequence breadCrumbTitle;
        int breadCrumbTitleRes = backStackEntry.getBreadCrumbTitleRes();
        if (breadCrumbTitleRes > 0) {
            breadCrumbTitle = getText(breadCrumbTitleRes);
        } else {
            breadCrumbTitle = backStackEntry.getBreadCrumbTitle();
        }
        if (breadCrumbTitle != null) {
            setTitle(breadCrumbTitle);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        saveState(bundle);
    }

    void saveState(Bundle bundle) {
        if (this.mCategories.size() > 0) {
            bundle.putParcelableArrayList(":settings:categories", this.mCategories);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.mDevelopmentSettingsListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SettingsActivity.this.updateTilesList();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mDevelopmentSettingsListener, new IntentFilter("com.android.settingslib.development.DevelopmentSettingsEnabler.SETTINGS_CHANGED"));
        registerReceiver(this.mBatteryInfoReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        updateTilesList();
        updateDeviceIndex();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mDevelopmentSettingsListener);
        this.mDevelopmentSettingsListener = null;
        unregisterReceiver(this.mBatteryInfoReceiver);
    }

    @Override
    public void setTaskDescription(ActivityManager.TaskDescription taskDescription) {
        taskDescription.setIcon(getBitmapFromXmlResource(R.drawable.ic_launcher_settings));
        super.setTaskDescription(taskDescription);
    }

    protected boolean isValidFragment(String str) {
        for (int i = 0; i < SettingsGateway.ENTRY_FRAGMENTS.length; i++) {
            if (SettingsGateway.ENTRY_FRAGMENTS[i].equals(str)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Intent getIntent() {
        Bundle bundle;
        Intent intent = super.getIntent();
        String startingFragmentClass = getStartingFragmentClass(intent);
        if (startingFragmentClass != null) {
            Intent intent2 = new Intent(intent);
            intent2.putExtra(":settings:show_fragment", startingFragmentClass);
            Bundle bundleExtra = intent.getBundleExtra(":settings:show_fragment_args");
            if (bundleExtra != null) {
                bundle = new Bundle(bundleExtra);
            } else {
                bundle = new Bundle();
            }
            bundle.putParcelable("intent", intent);
            intent2.putExtra(":settings:show_fragment_args", bundle);
            return intent2;
        }
        return intent;
    }

    private String getStartingFragmentClass(Intent intent) {
        if (this.mFragmentClass != null) {
            return this.mFragmentClass;
        }
        String className = intent.getComponent().getClassName();
        if (className.equals(getClass().getName())) {
            return null;
        }
        if ("com.android.settings.RunningServices".equals(className) || "com.android.settings.applications.StorageUse".equals(className)) {
            return ManageApplications.class.getName();
        }
        return className;
    }

    public void finishPreferencePanel(int i, Intent intent) {
        setResult(i, intent);
        finish();
    }

    private Fragment switchToFragment(String str, Bundle bundle, boolean z, boolean z2, int i, CharSequence charSequence, boolean z3) {
        Log.d("SettingsActivity", "Switching to fragment " + str);
        if (z && !isValidFragment(str)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: " + str);
        }
        Fragment fragmentInstantiate = Fragment.instantiate(this, str, bundle);
        FragmentTransaction fragmentTransactionBeginTransaction = getFragmentManager().beginTransaction();
        fragmentTransactionBeginTransaction.replace(R.id.main_content, fragmentInstantiate);
        if (z3) {
            TransitionManager.beginDelayedTransition(this.mContent);
        }
        if (z2) {
            fragmentTransactionBeginTransaction.addToBackStack(":settings:prefs");
        }
        if (i > 0) {
            fragmentTransactionBeginTransaction.setBreadCrumbTitle(i);
        } else if (charSequence != null) {
            fragmentTransactionBeginTransaction.setBreadCrumbTitle(charSequence);
        }
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        Log.d("SettingsActivity", "Executed frag manager pendingTransactions");
        return fragmentInstantiate;
    }

    private void updateTilesList() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                SettingsActivity.this.doUpdateTilesList();
            }
        });
    }

    private void updateDeviceIndex() {
        final DeviceIndexFeatureProvider deviceIndexFeatureProvider = FeatureFactory.getFactory(this).getDeviceIndexFeatureProvider();
        ThreadUtils.postOnBackgroundThread(new Runnable() {
            @Override
            public final void run() {
                deviceIndexFeatureProvider.updateIndex(this.f$0, false);
            }
        });
    }

    private void doUpdateTilesList() {
        PackageManager packageManager = getPackageManager();
        UserManager userManager = UserManager.get(this);
        boolean zIsAdminUser = userManager.isAdminUser();
        FeatureFactory factory = FeatureFactory.getFactory(this);
        String packageName = getPackageName();
        StringBuilder sb = new StringBuilder();
        boolean z = setTileEnabled(sb, new ComponentName(packageName, Settings.PowerUsageSummaryActivity.class.getName()), this.mBatteryPresent, zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.SimSettingsActivity.class.getName()), Utils.showSimCardTile(this), zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.ConnectedDeviceDashboardActivity.class.getName()), UserManager.isDeviceInDemoMode(this) ^ true, zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.DataUsageSummaryActivity.class.getName()), Utils.isBandwidthControlEnabled(), zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.BluetoothSettingsActivity.class.getName()), packageManager.hasSystemFeature("android.hardware.bluetooth"), zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.WifiSettingsActivity.class.getName()), packageManager.hasSystemFeature("android.hardware.wifi"), zIsAdminUser))))));
        boolean zIsEnabled = FeatureFlagUtils.isEnabled(this, "settings_data_usage_v2");
        boolean z2 = setTileEnabled(sb, new ComponentName(packageName, Settings.DateTimeSettingsActivity.class.getName()), UserManager.isDeviceInDemoMode(this) ^ true, zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.NetworkDashboardActivity.class.getName()), UserManager.isDeviceInDemoMode(this) ^ true, zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.UserSettingsActivity.class.getName()), UserManager.supportsMultipleUsers() && !Utils.isMonkeyRunning(), zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.DataUsageSummaryLegacyActivity.class.getName()), Utils.isBandwidthControlEnabled() && !zIsEnabled, zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.DataUsageSummaryActivity.class.getName()), Utils.isBandwidthControlEnabled() && zIsEnabled, zIsAdminUser) || z))));
        boolean z3 = DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(this) && !Utils.isMonkeyRunning();
        boolean z4 = userManager.isAdminUser() || userManager.isDemoUser();
        boolean z5 = setTileEnabled(sb, new ComponentName(packageName, Settings.WifiDisplaySettingsActivity.class.getName()), WifiDisplaySettings.isAvailable(this), zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, BackupSettingsActivity.class.getName()), true, zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.DevelopmentSettingsDashboardActivity.class.getName()), z3, z4) || z2));
        boolean zIsAboutPhoneV2Enabled = factory.getAccountFeatureProvider().isAboutPhoneV2Enabled(this);
        boolean z6 = setTileEnabled(sb, new ComponentName(packageName, Settings.DeviceInfoSettingsActivity.class.getName()), zIsAboutPhoneV2Enabled ^ true, zIsAdminUser) || (setTileEnabled(sb, new ComponentName(packageName, Settings.MyDeviceInfoActivity.class.getName()), zIsAboutPhoneV2Enabled, zIsAdminUser) || z5);
        if (!zIsAdminUser) {
            List<DashboardCategory> allCategories = this.mDashboardFeatureProvider.getAllCategories();
            synchronized (allCategories) {
                for (DashboardCategory dashboardCategory : allCategories) {
                    int tilesCount = dashboardCategory.getTilesCount();
                    boolean z7 = z6;
                    for (int i = 0; i < tilesCount; i++) {
                        ComponentName component = dashboardCategory.getTile(i).intent.getComponent();
                        String className = component.getClassName();
                        boolean z8 = ArrayUtils.contains(SettingsGateway.SETTINGS_FOR_RESTRICTED, className) || (z4 && Settings.DevelopmentSettingsDashboardActivity.class.getName().equals(className));
                        if (packageName.equals(component.getPackageName()) && !z8) {
                            z7 = setTileEnabled(sb, component, false, zIsAdminUser) || z7;
                        }
                    }
                    z6 = z7;
                }
            }
        }
        if (!z6) {
            Log.d("SettingsActivity", "No enabled state changed, skipping updateCategory call");
            return;
        }
        Log.d("SettingsActivity", "Enabled state changed for some tiles, reloading all categories " + sb.toString());
        updateCategories();
    }

    private boolean setTileEnabled(StringBuilder sb, ComponentName componentName, boolean z, boolean z2) {
        if (!z2 && getPackageName().equals(componentName.getPackageName()) && !ArrayUtils.contains(SettingsGateway.SETTINGS_FOR_RESTRICTED, componentName.getClassName())) {
            z = false;
        }
        boolean tileEnabled = setTileEnabled(componentName, z);
        if (tileEnabled) {
            sb.append(componentName.toShortString());
            sb.append(",");
        }
        return tileEnabled;
    }

    private void getMetaData() {
        try {
            ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), 128);
            if (activityInfo != null && activityInfo.metaData != null) {
                this.mFragmentClass = activityInfo.metaData.getString("com.android.settings.FRAGMENT_CLASS");
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("SettingsActivity", "Cannot get Metadata for: " + getComponentName().toString());
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

    public boolean isLaunchableInTaskModePinned() {
        return false;
    }

    Bitmap getBitmapFromXmlResource(int i) {
        Drawable drawable = getResources().getDrawable(i, getTheme());
        Canvas canvas = new Canvas();
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmapCreateBitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmapCreateBitmap;
    }

    private boolean isLockTaskModePinned() {
        return ((ActivityManager) getApplicationContext().getSystemService(ActivityManager.class)).getLockTaskModeState() == 2;
    }

    private boolean isSettingsRunOnTop() {
        return TextUtils.equals(getPackageName(), ((ActivityManager) getApplicationContext().getSystemService(ActivityManager.class)).getRunningTasks(1).get(0).baseActivity.getPackageName());
    }
}
