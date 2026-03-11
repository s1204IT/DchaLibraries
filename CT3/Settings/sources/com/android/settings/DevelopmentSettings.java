package com.android.settings;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.backup.IBackupManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IShortcutService;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.usb.IUsbManager;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.service.persistentdata.PersistentDataBlockManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.IWebViewUpdateService;
import android.webkit.WebViewProviderInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.Toast;
import com.android.internal.app.LocalePicker;
import com.android.settings.applications.BackgroundCheckSummary;
import com.android.settings.fuelgauge.InactiveApps;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDevExt;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class DevelopmentSettings extends RestrictedSettingsFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener, Indexable {
    private static final int[] MOCK_LOCATION_APP_OPS = {58};
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        private boolean isShowingDeveloperOptions(Context context) {
            return context.getSharedPreferences("development", 0).getBoolean("show", Build.TYPE.equals("eng"));
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
            if (!isShowingDeveloperOptions(context)) {
                return null;
            }
            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.development_prefs;
            return Arrays.asList(sir);
        }

        @Override
        public List<String> getNonIndexableKeys(Context context) {
            if (!isShowingDeveloperOptions(context)) {
                return null;
            }
            List<String> keys = new ArrayList<>();
            if (!DevelopmentSettings.showEnableOemUnlockPreference()) {
                keys.add("oem_unlock_enable");
            }
            return keys;
        }
    };
    private Dialog mAdbDialog;
    private Dialog mAdbKeysDialog;
    private final ArrayList<Preference> mAllPrefs;
    private ListPreference mAnimatorDurationScale;
    private ListPreference mAppProcessLimit;
    private IBackupManager mBackupManager;
    private SwitchPreference mBluetoothDisableAbsVolume;
    private SwitchPreference mBtHciSnoopLog;
    private Preference mBugreport;
    private SwitchPreference mBugreportInPower;
    private Preference mClearAdbKeys;
    private ColorModePreference mColorModePreference;
    private SwitchPreference mColorTemperaturePreference;
    private String mDebugApp;
    private Preference mDebugAppPref;
    private ListPreference mDebugHwOverdraw;
    private SwitchPreference mDebugLayout;
    private SwitchPreference mDebugViewAttributes;
    private AlertDialog mDialog;
    private boolean mDialogClicked;
    private SwitchPreference mDisableOverlays;
    private final HashSet<Preference> mDisabledPrefs;
    private boolean mDontPokeProperties;
    private DevicePolicyManager mDpm;
    private EditText mEditText;
    private SwitchPreference mEnableAdb;
    private Dialog mEnableDialog;
    private SwitchPreference mEnableOemUnlock;
    private SwitchPreference mEnableTerminal;
    private IDevExt mExt;
    private SwitchPreference mForceAllowOnExternal;
    private SwitchPreference mForceHardwareUi;
    private SwitchPreference mForceMsaa;
    private SwitchPreference mForceResizable;
    private SwitchPreference mForceRtlLayout;
    private FrameLayout mFrameLayout;
    private boolean mHaveDebugSettings;
    private SwitchPreference mImmediatelyDestroyActivities;
    private RestrictedSwitchPreference mKeepScreenOn;
    private boolean mLastEnabledState;
    private ListPreference mLogdSize;
    private SwitchPreference mMobileDataAlwaysOn;
    private String mMockLocationApp;
    private Preference mMockLocationAppPref;
    private PersistentDataBlockManager mOemUnlockManager;
    private SwitchPreference mOtaDisableAutomaticUpdate;
    private ListPreference mOverlayDisplayDevices;
    private PreferenceScreen mPassword;
    private SwitchPreference mPointerLocation;
    private final ArrayList<SwitchPreference> mResetSwitchPrefs;
    private SwitchPreference mScreenCaptureOn;
    private SwitchPreference mShowAllANRs;
    private SwitchPreference mShowCpuUsage;
    private SwitchPreference mShowHwLayersUpdates;
    private SwitchPreference mShowHwScreenUpdates;
    private ListPreference mShowNonRectClip;
    private SwitchPreference mShowScreenUpdates;
    private SwitchPreference mShowTouches;
    private ListPreference mSimulateColorSpace;
    private SwitchPreference mStrictMode;
    private SwitchBar mSwitchBar;
    private ListPreference mTrackFrameTime;
    private ListPreference mTransitionAnimationScale;
    private SwitchPreference mUSBAudio;
    private UserManager mUm;
    private boolean mUnavailable;
    private ListPreference mUsbConfiguration;
    private BroadcastReceiver mUsbReceiver;
    private SwitchPreference mVerifyAppsOverUsb;
    private View mView;
    private SwitchPreference mWaitForDebugger;
    private SwitchPreference mWebViewMultiprocess;
    private ListPreference mWebViewProvider;
    private IWebViewUpdateService mWebViewUpdateService;
    private SwitchPreference mWifiAggressiveHandover;
    private SwitchPreference mWifiAllowScansWithTraffic;
    private SwitchPreference mWifiDisplayCertification;
    private WifiManager mWifiManager;
    private SwitchPreference mWifiVerboseLogging;
    private ListPreference mWindowAnimationScale;
    private IWindowManager mWindowManager;

    public DevelopmentSettings() {
        super("no_debugging_features");
        this.mAllPrefs = new ArrayList<>();
        this.mResetSwitchPrefs = new ArrayList<>();
        this.mDisabledPrefs = new HashSet<>();
        this.mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DevelopmentSettings.this.updateUsbConfigurationValues();
            }
        };
    }

    @Override
    protected int getMetricsCategory() {
        return 39;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (BenesseExtension.getDchaState() != 3 && BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists() && getUid(BenesseExtension.IGNORE_DCHA_COMPLETED_FILE) != 0) {
            if (this.mDialog != null) {
                this.mDialog.setDismissMessage(null);
                this.mDialog = null;
            }
            if (this.mEditText != null) {
                this.mEditText = null;
            }
            this.mEditText = new EditText(getActivity());
            this.mEditText.setInputType(129);
            this.mDialog = new AlertDialog.Builder(getActivity()).setTitle(R.string.unlock_set_unlock_password_title).setView(this.mEditText).setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    DevelopmentSettings.this.m363com_android_settings_DevelopmentSettings_lambda$1(arg0, arg1);
                }
            }).setNegativeButton(R.string.dlg_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    DevelopmentSettings.this.m364com_android_settings_DevelopmentSettings_lambda$2(arg0, arg1);
                }
            }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface arg0) {
                    DevelopmentSettings.this.m365com_android_settings_DevelopmentSettings_lambda$3(arg0);
                }
            }).create();
            this.mDialog.show();
        }
        this.mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        this.mWebViewUpdateService = IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate"));
        this.mOemUnlockManager = (PersistentDataBlockManager) getActivity().getSystemService("persistent_data_block");
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        this.mUm = (UserManager) getSystemService("user");
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted() || !Utils.isDeviceProvisioned(getActivity())) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
            return;
        }
        addPreferencesFromResource(R.xml.development_prefs);
        PreferenceGroup debugDebuggingCategory = (PreferenceGroup) findPreference("debug_debugging_category");
        this.mEnableAdb = findAndInitSwitchPref("enable_adb");
        this.mClearAdbKeys = findPreference("clear_adb_keys");
        if (!SystemProperties.getBoolean("ro.adb.secure", false) && debugDebuggingCategory != null) {
            debugDebuggingCategory.removePreference(this.mClearAdbKeys);
        }
        this.mScreenCaptureOn = findAndInitSwitchPref("screen_capture_on");
        this.mAllPrefs.add(this.mClearAdbKeys);
        this.mEnableTerminal = findAndInitSwitchPref("enable_terminal");
        if (!isPackageInstalled(getActivity(), "com.android.terminal")) {
            debugDebuggingCategory.removePreference(this.mEnableTerminal);
            this.mEnableTerminal = null;
        }
        this.mBugreport = findPreference("bugreport");
        this.mBugreportInPower = findAndInitSwitchPref("bugreport_in_power");
        this.mKeepScreenOn = (RestrictedSwitchPreference) findAndInitSwitchPref("keep_screen_on");
        this.mBtHciSnoopLog = findAndInitSwitchPref("bt_hci_snoop_log");
        if (!getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
            removePreference(this.mBtHciSnoopLog);
            this.mBtHciSnoopLog = null;
        }
        this.mEnableOemUnlock = findAndInitSwitchPref("oem_unlock_enable");
        if (!showEnableOemUnlockPreference()) {
            removePreference(this.mEnableOemUnlock);
            this.mEnableOemUnlock = null;
        }
        this.mDebugViewAttributes = findAndInitSwitchPref("debug_view_attributes");
        this.mForceAllowOnExternal = findAndInitSwitchPref("force_allow_on_external");
        this.mPassword = (PreferenceScreen) findPreference("local_backup_password");
        this.mAllPrefs.add(this.mPassword);
        if (!this.mUm.isAdminUser()) {
            disableForUser(this.mEnableAdb);
            disableForUser(this.mClearAdbKeys);
            disableForUser(this.mEnableTerminal);
            disableForUser(this.mPassword);
        }
        this.mDebugAppPref = findPreference("debug_app");
        this.mAllPrefs.add(this.mDebugAppPref);
        this.mWaitForDebugger = findAndInitSwitchPref("wait_for_debugger");
        this.mMockLocationAppPref = findPreference("mock_location_app");
        this.mAllPrefs.add(this.mMockLocationAppPref);
        this.mVerifyAppsOverUsb = findAndInitSwitchPref("verify_apps_over_usb");
        if (!showVerifierSetting()) {
            if (debugDebuggingCategory != null) {
                debugDebuggingCategory.removePreference(this.mVerifyAppsOverUsb);
            } else {
                this.mVerifyAppsOverUsb.setEnabled(false);
            }
        }
        this.mStrictMode = findAndInitSwitchPref("strict_mode");
        this.mPointerLocation = findAndInitSwitchPref("pointer_location");
        this.mShowTouches = findAndInitSwitchPref("show_touches");
        this.mShowScreenUpdates = findAndInitSwitchPref("show_screen_updates");
        this.mDisableOverlays = findAndInitSwitchPref("disable_overlays");
        this.mShowCpuUsage = findAndInitSwitchPref("show_cpu_usage");
        this.mForceHardwareUi = findAndInitSwitchPref("force_hw_ui");
        this.mForceMsaa = findAndInitSwitchPref("force_msaa");
        this.mTrackFrameTime = addListPreference("track_frame_time");
        this.mShowNonRectClip = addListPreference("show_non_rect_clip");
        this.mShowHwScreenUpdates = findAndInitSwitchPref("show_hw_screen_udpates");
        this.mShowHwLayersUpdates = findAndInitSwitchPref("show_hw_layers_udpates");
        this.mDebugLayout = findAndInitSwitchPref("debug_layout");
        this.mForceRtlLayout = findAndInitSwitchPref("force_rtl_layout_all_locales");
        this.mDebugHwOverdraw = addListPreference("debug_hw_overdraw");
        this.mWifiDisplayCertification = findAndInitSwitchPref("wifi_display_certification");
        this.mWifiVerboseLogging = findAndInitSwitchPref("wifi_verbose_logging");
        this.mWifiAggressiveHandover = findAndInitSwitchPref("wifi_aggressive_handover");
        this.mWifiAllowScansWithTraffic = findAndInitSwitchPref("wifi_allow_scan_with_traffic");
        this.mMobileDataAlwaysOn = findAndInitSwitchPref("mobile_data_always_on");
        this.mLogdSize = addListPreference("select_logd_size");
        this.mUsbConfiguration = addListPreference("select_usb_configuration");
        this.mWebViewProvider = addListPreference("select_webview_provider");
        this.mWebViewMultiprocess = findAndInitSwitchPref("enable_webview_multiprocess");
        this.mBluetoothDisableAbsVolume = findAndInitSwitchPref("bluetooth_disable_absolute_volume");
        this.mWindowAnimationScale = addListPreference("window_animation_scale");
        this.mTransitionAnimationScale = addListPreference("transition_animation_scale");
        this.mAnimatorDurationScale = addListPreference("animator_duration_scale");
        this.mOverlayDisplayDevices = addListPreference("overlay_display_devices");
        this.mSimulateColorSpace = addListPreference("simulate_color_space");
        this.mUSBAudio = findAndInitSwitchPref("usb_audio");
        this.mForceResizable = findAndInitSwitchPref("force_resizable_activities");
        this.mImmediatelyDestroyActivities = (SwitchPreference) findPreference("immediately_destroy_activities");
        this.mAllPrefs.add(this.mImmediatelyDestroyActivities);
        this.mResetSwitchPrefs.add(this.mImmediatelyDestroyActivities);
        this.mAppProcessLimit = addListPreference("app_process_limit");
        this.mShowAllANRs = (SwitchPreference) findPreference("show_all_anrs");
        this.mAllPrefs.add(this.mShowAllANRs);
        this.mResetSwitchPrefs.add(this.mShowAllANRs);
        Preference hdcpChecking = findPreference("hdcp_checking");
        if (hdcpChecking != null) {
            this.mAllPrefs.add(hdcpChecking);
            removePreferenceForProduction(hdcpChecking);
        }
        PreferenceScreen convertFbePreference = (PreferenceScreen) findPreference("convert_to_file_encryption");
        try {
            IBinder service = ServiceManager.getService("mount");
            IMountService mountService = IMountService.Stub.asInterface(service);
            if (!mountService.isConvertibleToFBE()) {
                removePreference("convert_to_file_encryption");
            } else if ("file".equals(SystemProperties.get("ro.crypto.type", "none"))) {
                convertFbePreference.setEnabled(false);
                convertFbePreference.setSummary(getResources().getString(R.string.convert_to_file_encryption_done));
            }
        } catch (RemoteException e) {
            removePreference("convert_to_file_encryption");
        }
        this.mOtaDisableAutomaticUpdate = findAndInitSwitchPref("ota_disable_automatic_update");
        removePreference(this.mOtaDisableAutomaticUpdate);
        this.mColorModePreference = (ColorModePreference) findPreference("color_mode");
        this.mColorModePreference.updateCurrentAndSupported();
        if (this.mColorModePreference.getTransformsCount() < 2) {
            removePreference("color_mode");
            this.mColorModePreference = null;
        }
        updateWebViewProviderOptions();
        this.mColorTemperaturePreference = (SwitchPreference) findPreference("color_temperature");
        if (getResources().getBoolean(R.bool.config_enableColorTemperature)) {
            this.mAllPrefs.add(this.mColorTemperaturePreference);
            this.mResetSwitchPrefs.add(this.mColorTemperaturePreference);
        } else {
            removePreference("color_temperature");
            this.mColorTemperaturePreference = null;
        }
        this.mExt = UtilsExt.getDevExtPlugin(getActivity());
    }

    void m363com_android_settings_DevelopmentSettings_lambda$1(DialogInterface dialog, int which) {
        if (BenesseExtension.checkPassword(this.mEditText.getText().toString())) {
            this.mView.setVisibility(8);
        } else {
            getActivity().finish();
        }
    }

    void m364com_android_settings_DevelopmentSettings_lambda$2(DialogInterface dialog, int which) {
        getActivity().finish();
    }

    void m365com_android_settings_DevelopmentSettings_lambda$3(DialogInterface dialog) {
        getActivity().finish();
    }

    private ListPreference addListPreference(String prefKey) {
        ListPreference pref = (ListPreference) findPreference(prefKey);
        this.mAllPrefs.add(pref);
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    private void disableForUser(Preference pref) {
        if (pref == null) {
            return;
        }
        pref.setEnabled(false);
        this.mDisabledPrefs.add(pref);
    }

    private SwitchPreference findAndInitSwitchPref(String key) {
        SwitchPreference pref = (SwitchPreference) findPreference(key);
        if (pref == null) {
            throw new IllegalArgumentException("Cannot find preference with key = " + key);
        }
        this.mAllPrefs.add(pref);
        this.mResetSwitchPrefs.add(pref);
        return pref;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        if (this.mUnavailable) {
            this.mSwitchBar.setEnabled(false);
            return;
        }
        this.mSwitchBar.addOnSwitchChangeListener(this);
        if (BenesseExtension.getDchaState() == 3 || !BenesseExtension.COUNT_DCHA_COMPLETED_FILE.exists() || getUid(BenesseExtension.IGNORE_DCHA_COMPLETED_FILE) == 0) {
            return;
        }
        this.mFrameLayout.addView(this.mView, 0, new ViewGroup.LayoutParams(-1, -1));
    }

    private boolean removePreferenceForProduction(Preference preference) {
        if ("user".equals(Build.TYPE)) {
            removePreference(preference);
            return true;
        }
        return false;
    }

    private void removePreference(Preference preference) {
        getPreferenceScreen().removePreference(preference);
        this.mAllPrefs.remove(preference);
        this.mResetSwitchPrefs.remove(preference);
    }

    private void setPrefsEnabledState(boolean enabled) {
        for (int i = 0; i < this.mAllPrefs.size(); i++) {
            Preference pref = this.mAllPrefs.get(i);
            pref.setEnabled(enabled && !this.mDisabledPrefs.contains(pref));
        }
        updateAllOptions();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mView.bringToFront();
        if (this.mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.development_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(getActivity());
        this.mKeepScreenOn.setDisabledByAdmin(admin);
        if (admin == null) {
            this.mDisabledPrefs.remove(this.mKeepScreenOn);
        } else {
            this.mDisabledPrefs.add(this.mKeepScreenOn);
        }
        ContentResolver cr = getActivity().getContentResolver();
        this.mLastEnabledState = Settings.Global.getInt(cr, "development_settings_enabled", 0) != 0;
        this.mSwitchBar.setChecked((this.mEnableDialog == null || !this.mEnableDialog.isShowing()) ? this.mLastEnabledState : true);
        setPrefsEnabledState(this.mLastEnabledState);
        if (this.mHaveDebugSettings && !this.mLastEnabledState) {
            Settings.Global.putInt(getActivity().getContentResolver(), "development_settings_enabled", 1);
            this.mLastEnabledState = true;
            this.mSwitchBar.setChecked(this.mLastEnabledState);
            setPrefsEnabledState(this.mLastEnabledState);
        }
        this.mSwitchBar.show();
        if (this.mColorModePreference != null) {
            this.mColorModePreference.startListening();
            this.mColorModePreference.updateCurrentAndSupported();
        }
        this.mExt.customUSBPreference(this.mEnableAdb);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mColorModePreference == null) {
            return;
        }
        this.mColorModePreference.stopListening();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.hardware.usb.action.USB_STATE");
        if (getActivity().registerReceiver(this.mUsbReceiver, filter) == null) {
            updateUsbConfigurationValues();
        }
        View root = super.onCreateView(inflater, container, savedInstanceState);
        this.mFrameLayout = (FrameLayout) root.findViewById(android.R.id.list_container);
        this.mView = new View(getActivity());
        this.mView.setBackgroundColor(-16777216);
        this.mView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.mUnavailable) {
            return;
        }
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
        getActivity().unregisterReceiver(this.mUsbReceiver);
    }

    void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
        this.mHaveDebugSettings |= value;
    }

    public void updateAllOptions() {
        Context context = getActivity();
        ContentResolver cr = context.getContentResolver();
        this.mHaveDebugSettings = false;
        boolean isChecked = (this.mAdbDialog != null && this.mAdbDialog.isShowing()) || Settings.Global.getInt(cr, "adb_enabled", 0) != 0;
        updateSwitchPreference(this.mEnableAdb, isChecked);
        this.mExt.customUSBPreference(this.mEnableAdb);
        if (this.mEnableTerminal != null) {
            updateSwitchPreference(this.mEnableTerminal, context.getPackageManager().getApplicationEnabledSetting("com.android.terminal") == 1);
        }
        updateSwitchPreference(this.mBugreportInPower, Settings.Secure.getInt(cr, "bugreport_in_power_menu", 0) != 0);
        updateSwitchPreference(this.mKeepScreenOn, Settings.Global.getInt(cr, "stay_on_while_plugged_in", 0) != 0);
        if (this.mBtHciSnoopLog != null) {
            updateSwitchPreference(this.mBtHciSnoopLog, Settings.Secure.getInt(cr, "bluetooth_hci_log", 0) != 0);
        }
        if (this.mEnableOemUnlock != null) {
            updateSwitchPreference(this.mEnableOemUnlock, Utils.isOemUnlockEnabled(getActivity()));
        }
        updateSwitchPreference(this.mDebugViewAttributes, Settings.Global.getInt(cr, "debug_view_attributes", 0) != 0);
        updateSwitchPreference(this.mForceAllowOnExternal, Settings.Global.getInt(cr, "force_allow_on_external", 0) != 0);
        updateSwitchPreference(this.mScreenCaptureOn, Settings.System.getInt(cr, "screen_capture_on", 0) != 0);
        updateHdcpValues();
        updatePasswordSummary();
        updateDebuggerOptions();
        updateMockLocation();
        updateStrictModeVisualOptions();
        updatePointerLocationOptions();
        updateShowTouchesOptions();
        updateFlingerOptions();
        updateCpuUsageOptions();
        updateHardwareUiOptions();
        updateMsaaOptions();
        updateTrackFrameTimeOptions();
        updateShowNonRectClipOptions();
        updateShowHwScreenUpdatesOptions();
        updateShowHwLayersUpdatesOptions();
        updateDebugHwOverdrawOptions();
        updateDebugLayoutOptions();
        updateAnimationScaleOptions();
        updateOverlayDisplayDevicesOptions();
        updateImmediatelyDestroyActivitiesOptions();
        updateAppProcessLimitOptions();
        updateShowAllANRsOptions();
        updateVerifyAppsOverUsbOptions();
        updateOtaDisableAutomaticUpdateOptions();
        updateBugreportOptions();
        updateForceRtlOptions();
        updateLogdSizeValues();
        updateWifiDisplayCertificationOptions();
        updateWifiVerboseLoggingOptions();
        updateWifiAggressiveHandoverOptions();
        updateWifiAllowScansWithTrafficOptions();
        updateMobileDataAlwaysOnOptions();
        updateSimulateColorSpace();
        updateUSBAudioOptions();
        updateForceResizableOptions();
        updateWebViewMultiprocessOptions();
        updateWebViewProviderOptions();
        updateOemUnlockOptions();
        if (this.mColorTemperaturePreference != null) {
            updateColorTemperature();
        }
        updateBluetoothDisableAbsVolumeOptions();
    }

    private void resetDangerousOptions() {
        this.mDontPokeProperties = true;
        for (int i = 0; i < this.mResetSwitchPrefs.size(); i++) {
            SwitchPreference cb = this.mResetSwitchPrefs.get(i);
            if (cb.isChecked()) {
                cb.setChecked(false);
                onPreferenceTreeClick(cb);
            }
        }
        resetDebuggerOptions();
        writeLogdSizeOption(null);
        writeAnimationScaleOption(0, this.mWindowAnimationScale, null);
        writeAnimationScaleOption(1, this.mTransitionAnimationScale, null);
        writeAnimationScaleOption(2, this.mAnimatorDurationScale, null);
        if (usingDevelopmentColorSpace()) {
            writeSimulateColorSpace(-1);
        }
        writeOverlayDisplayDevicesOptions(null);
        writeAppProcessLimitOptions(null);
        this.mHaveDebugSettings = false;
        updateAllOptions();
        this.mDontPokeProperties = false;
        pokeSystemProperties();
    }

    private void updateWebViewProviderOptions() {
        try {
            WebViewProviderInfo[] providers = this.mWebViewUpdateService.getValidWebViewPackages();
            if (providers == null) {
                Log.e("DevelopmentSettings", "No WebView providers available");
                return;
            }
            ArrayList<String> options = new ArrayList<>();
            ArrayList<String> values = new ArrayList<>();
            for (int n = 0; n < providers.length; n++) {
                if (Utils.isPackageEnabled(getActivity(), providers[n].packageName)) {
                    options.add(providers[n].description);
                    values.add(providers[n].packageName);
                }
            }
            this.mWebViewProvider.setEntries((CharSequence[]) options.toArray(new String[options.size()]));
            this.mWebViewProvider.setEntryValues((CharSequence[]) values.toArray(new String[values.size()]));
            String value = this.mWebViewUpdateService.getCurrentWebViewPackageName();
            if (value == null) {
                value = "";
            }
            for (int i = 0; i < values.size(); i++) {
                if (value.contentEquals(values.get(i))) {
                    this.mWebViewProvider.setValueIndex(i);
                    return;
                }
            }
        } catch (RemoteException e) {
        }
    }

    private void updateWebViewMultiprocessOptions() {
        updateSwitchPreference(this.mWebViewMultiprocess, Settings.Global.getInt(getActivity().getContentResolver(), "webview_multiprocess", 0) != 0);
    }

    private void writeWebViewMultiprocessOptions() {
        boolean value = this.mWebViewMultiprocess.isChecked();
        Settings.Global.putInt(getActivity().getContentResolver(), "webview_multiprocess", value ? 1 : 0);
        try {
            String wv_package = this.mWebViewUpdateService.getCurrentWebViewPackageName();
            ActivityManagerNative.getDefault().killPackageDependents(wv_package, -1);
        } catch (RemoteException e) {
        }
    }

    private void updateHdcpValues() {
        ListPreference hdcpChecking = (ListPreference) findPreference("hdcp_checking");
        if (hdcpChecking == null) {
            return;
        }
        String currentValue = SystemProperties.get("persist.sys.hdcp_checking");
        String[] values = getResources().getStringArray(R.array.hdcp_checking_values);
        String[] summaries = getResources().getStringArray(R.array.hdcp_checking_summaries);
        int index = 1;
        int i = 0;
        while (true) {
            if (i >= values.length) {
                break;
            }
            if (!currentValue.equals(values[i])) {
                i++;
            } else {
                index = i;
                break;
            }
        }
        hdcpChecking.setValue(values[index]);
        hdcpChecking.setSummary(summaries[index]);
        hdcpChecking.setOnPreferenceChangeListener(this);
    }

    private void updatePasswordSummary() {
        try {
            if (this.mBackupManager.hasBackupPassword()) {
                this.mPassword.setSummary(R.string.local_backup_password_summary_change);
            } else {
                this.mPassword.setSummary(R.string.local_backup_password_summary_none);
            }
        } catch (RemoteException e) {
        }
    }

    private void writeBtHciSnoopLogOptions() {
        if (this.mBtHciSnoopLog == null) {
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.configHciSnoopLog(this.mBtHciSnoopLog.isChecked());
        Settings.Secure.putInt(getActivity().getContentResolver(), "bluetooth_hci_log", this.mBtHciSnoopLog.isChecked() ? 1 : 0);
    }

    private boolean writeWebViewProviderOptions(Object newValue) {
        boolean zEquals = false;
        try {
            String updatedProvider = this.mWebViewUpdateService.changeProviderAndSetting(newValue == null ? "" : newValue.toString());
            updateWebViewProviderOptions();
            if (newValue == null) {
                return false;
            }
            zEquals = newValue.equals(updatedProvider);
            return zEquals;
        } catch (RemoteException e) {
            return zEquals;
        }
    }

    private void writeDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(this.mDebugApp, this.mWaitForDebugger.isChecked(), true);
        } catch (RemoteException e) {
        }
    }

    private void writeMockLocation() {
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService("appops");
        List<AppOpsManager.PackageOps> packageOps = appOpsManager.getPackagesForOps(MOCK_LOCATION_APP_OPS);
        if (packageOps != null) {
            for (AppOpsManager.PackageOps packageOp : packageOps) {
                if (((AppOpsManager.OpEntry) packageOp.getOps().get(0)).getMode() != 2) {
                    String oldMockLocationApp = packageOp.getPackageName();
                    try {
                        ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(oldMockLocationApp, 512);
                        appOpsManager.setMode(58, ai.uid, oldMockLocationApp, 2);
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
        }
        if (TextUtils.isEmpty(this.mMockLocationApp)) {
            return;
        }
        try {
            ApplicationInfo ai2 = getActivity().getPackageManager().getApplicationInfo(this.mMockLocationApp, 512);
            appOpsManager.setMode(58, ai2.uid, this.mMockLocationApp, 0);
        } catch (PackageManager.NameNotFoundException e2) {
        }
    }

    private static void resetDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp((String) null, false, true);
        } catch (RemoteException e) {
        }
    }

    private void updateDebuggerOptions() {
        String label;
        this.mDebugApp = Settings.Global.getString(getActivity().getContentResolver(), "debug_app");
        updateSwitchPreference(this.mWaitForDebugger, Settings.Global.getInt(getActivity().getContentResolver(), "wait_for_debugger", 0) != 0);
        if (this.mDebugApp != null && this.mDebugApp.length() > 0) {
            try {
                ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(this.mDebugApp, 512);
                CharSequence lab = getActivity().getPackageManager().getApplicationLabel(ai);
                label = lab != null ? lab.toString() : this.mDebugApp;
            } catch (PackageManager.NameNotFoundException e) {
                label = this.mDebugApp;
            }
            this.mDebugAppPref.setSummary(getResources().getString(R.string.debug_app_set, label));
            this.mWaitForDebugger.setEnabled(true);
            this.mHaveDebugSettings = true;
            return;
        }
        this.mDebugAppPref.setSummary(getResources().getString(R.string.debug_app_not_set));
        this.mWaitForDebugger.setEnabled(false);
    }

    private void updateMockLocation() {
        AppOpsManager appOpsManager = (AppOpsManager) getSystemService("appops");
        List<AppOpsManager.PackageOps> packageOps = appOpsManager.getPackagesForOps(MOCK_LOCATION_APP_OPS);
        if (packageOps != null) {
            Iterator packageOp$iterator = packageOps.iterator();
            while (true) {
                if (!packageOp$iterator.hasNext()) {
                    break;
                }
                AppOpsManager.PackageOps packageOp = (AppOpsManager.PackageOps) packageOp$iterator.next();
                if (((AppOpsManager.OpEntry) packageOp.getOps().get(0)).getMode() == 0) {
                    this.mMockLocationApp = packageOps.get(0).getPackageName();
                    break;
                }
            }
        }
        if (!TextUtils.isEmpty(this.mMockLocationApp)) {
            String label = this.mMockLocationApp;
            try {
                ApplicationInfo ai = getActivity().getPackageManager().getApplicationInfo(this.mMockLocationApp, 512);
                CharSequence appLabel = getPackageManager().getApplicationLabel(ai);
                if (appLabel != null) {
                    label = appLabel.toString();
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            this.mMockLocationAppPref.setSummary(getString(R.string.mock_location_app_set, new Object[]{label}));
            this.mHaveDebugSettings = true;
            return;
        }
        this.mMockLocationAppPref.setSummary(getString(R.string.mock_location_app_not_set));
    }

    private void updateVerifyAppsOverUsbOptions() {
        updateSwitchPreference(this.mVerifyAppsOverUsb, Settings.Global.getInt(getActivity().getContentResolver(), "verifier_verify_adb_installs", 1) != 0);
        this.mVerifyAppsOverUsb.setEnabled(enableVerifierSetting());
    }

    private void writeVerifyAppsOverUsbOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(), "verifier_verify_adb_installs", this.mVerifyAppsOverUsb.isChecked() ? 1 : 0);
    }

    private void updateOtaDisableAutomaticUpdateOptions() {
        updateSwitchPreference(this.mOtaDisableAutomaticUpdate, Settings.Global.getInt(getActivity().getContentResolver(), "ota_disable_automatic_update", 0) != 1);
    }

    private void writeOtaDisableAutomaticUpdateOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(), "ota_disable_automatic_update", this.mOtaDisableAutomaticUpdate.isChecked() ? 0 : 1);
    }

    private boolean enableVerifierSetting() {
        ContentResolver cr = getActivity().getContentResolver();
        if (Settings.Global.getInt(cr, "adb_enabled", 0) == 0 || Settings.Global.getInt(cr, "package_verifier_enable", 1) == 0) {
            return false;
        }
        PackageManager pm = getActivity().getPackageManager();
        Intent verification = new Intent("android.intent.action.PACKAGE_NEEDS_VERIFICATION");
        verification.setType("application/vnd.android.package-archive");
        verification.addFlags(1);
        List<ResolveInfo> receivers = pm.queryBroadcastReceivers(verification, 0);
        return receivers.size() != 0;
    }

    private boolean showVerifierSetting() {
        return Settings.Global.getInt(getActivity().getContentResolver(), "verifier_setting_visible", 1) > 0;
    }

    public static boolean showEnableOemUnlockPreference() {
        return !SystemProperties.get("ro.frp.pst").equals("");
    }

    private boolean enableOemUnlockPreference() {
        int flashLockState = -1;
        if (this.mOemUnlockManager != null) {
            flashLockState = this.mOemUnlockManager.getFlashLockState();
        }
        return flashLockState != 0;
    }

    private void updateOemUnlockOptions() {
        if (this.mEnableOemUnlock == null) {
            return;
        }
        this.mEnableOemUnlock.setEnabled(enableOemUnlockPreference());
    }

    private void updateBugreportOptions() {
        this.mBugreport.setEnabled(true);
        this.mBugreportInPower.setEnabled(true);
        setBugreportStorageProviderStatus();
    }

    private void setBugreportStorageProviderStatus() {
        ComponentName componentName = new ComponentName("com.android.shell", "com.android.shell.BugreportStorageProvider");
        boolean enabled = this.mBugreportInPower.isChecked();
        getPackageManager().setComponentEnabledSetting(componentName, enabled ? 1 : 0, 0);
    }

    private static int currentStrictModeActiveIndex() {
        if (TextUtils.isEmpty(SystemProperties.get("persist.sys.strictmode.visual"))) {
            return 0;
        }
        boolean enabled = SystemProperties.getBoolean("persist.sys.strictmode.visual", false);
        return enabled ? 1 : 2;
    }

    private void writeStrictModeVisualOptions() {
        try {
            this.mWindowManager.setStrictModeVisualIndicatorPreference(this.mStrictMode.isChecked() ? "1" : "");
        } catch (RemoteException e) {
        }
    }

    private void updateStrictModeVisualOptions() {
        updateSwitchPreference(this.mStrictMode, currentStrictModeActiveIndex() == 1);
    }

    private void writePointerLocationOptions() {
        Settings.System.putInt(getActivity().getContentResolver(), "pointer_location", this.mPointerLocation.isChecked() ? 1 : 0);
    }

    private void updatePointerLocationOptions() {
        updateSwitchPreference(this.mPointerLocation, Settings.System.getInt(getActivity().getContentResolver(), "pointer_location", 0) != 0);
    }

    private void writeShowTouchesOptions() {
        Settings.System.putInt(getActivity().getContentResolver(), "show_touches", this.mShowTouches.isChecked() ? 1 : 0);
    }

    private void updateShowTouchesOptions() {
        updateSwitchPreference(this.mShowTouches, Settings.System.getInt(getActivity().getContentResolver(), "show_touches", 0) != 0);
    }

    private void updateFlingerOptions() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger == null) {
                return;
            }
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            flinger.transact(1010, data, reply, 0);
            reply.readInt();
            reply.readInt();
            int showUpdates = reply.readInt();
            updateSwitchPreference(this.mShowScreenUpdates, showUpdates != 0);
            reply.readInt();
            int disableOverlays = reply.readInt();
            updateSwitchPreference(this.mDisableOverlays, disableOverlays != 0);
            reply.recycle();
            data.recycle();
        } catch (RemoteException e) {
        }
    }

    private void writeShowUpdatesOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger == null) {
                return;
            }
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            int showUpdates = this.mShowScreenUpdates.isChecked() ? 1 : 0;
            data.writeInt(showUpdates);
            flinger.transact(1002, data, null, 0);
            data.recycle();
            updateFlingerOptions();
        } catch (RemoteException e) {
        }
    }

    private void writeDisableOverlaysOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger == null) {
                return;
            }
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            int disableOverlays = this.mDisableOverlays.isChecked() ? 1 : 0;
            data.writeInt(disableOverlays);
            flinger.transact(1008, data, null, 0);
            data.recycle();
            updateFlingerOptions();
        } catch (RemoteException e) {
        }
    }

    private void updateHardwareUiOptions() {
        updateSwitchPreference(this.mForceHardwareUi, SystemProperties.getBoolean("persist.sys.ui.hw", false));
    }

    private void writeHardwareUiOptions() {
        SystemProperties.set("persist.sys.ui.hw", this.mForceHardwareUi.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateMsaaOptions() {
        updateSwitchPreference(this.mForceMsaa, SystemProperties.getBoolean("debug.egl.force_msaa", false));
    }

    private void writeMsaaOptions() {
        SystemProperties.set("debug.egl.force_msaa", this.mForceMsaa.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateTrackFrameTimeOptions() {
        String value = SystemProperties.get("debug.hwui.profile");
        if (value == null) {
            value = "";
        }
        CharSequence[] values = this.mTrackFrameTime.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mTrackFrameTime.setValueIndex(i);
                this.mTrackFrameTime.setSummary(this.mTrackFrameTime.getEntries()[i]);
                return;
            }
        }
        this.mTrackFrameTime.setValueIndex(0);
        this.mTrackFrameTime.setSummary(this.mTrackFrameTime.getEntries()[0]);
    }

    private void writeTrackFrameTimeOptions(Object newValue) {
        SystemProperties.set("debug.hwui.profile", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateTrackFrameTimeOptions();
    }

    private void updateShowNonRectClipOptions() {
        String value = SystemProperties.get("debug.hwui.show_non_rect_clip");
        if (value == null) {
            value = "hide";
        }
        CharSequence[] values = this.mShowNonRectClip.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mShowNonRectClip.setValueIndex(i);
                this.mShowNonRectClip.setSummary(this.mShowNonRectClip.getEntries()[i]);
                return;
            }
        }
        this.mShowNonRectClip.setValueIndex(0);
        this.mShowNonRectClip.setSummary(this.mShowNonRectClip.getEntries()[0]);
    }

    private void writeShowNonRectClipOptions(Object newValue) {
        SystemProperties.set("debug.hwui.show_non_rect_clip", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateShowNonRectClipOptions();
    }

    private void updateShowHwScreenUpdatesOptions() {
        updateSwitchPreference(this.mShowHwScreenUpdates, SystemProperties.getBoolean("debug.hwui.show_dirty_regions", false));
    }

    private void writeShowHwScreenUpdatesOptions() {
        SystemProperties.set("debug.hwui.show_dirty_regions", this.mShowHwScreenUpdates.isChecked() ? "true" : null);
        pokeSystemProperties();
    }

    private void updateShowHwLayersUpdatesOptions() {
        updateSwitchPreference(this.mShowHwLayersUpdates, SystemProperties.getBoolean("debug.hwui.show_layers_updates", false));
    }

    private void writeShowHwLayersUpdatesOptions() {
        SystemProperties.set("debug.hwui.show_layers_updates", this.mShowHwLayersUpdates.isChecked() ? "true" : null);
        pokeSystemProperties();
    }

    private void updateDebugHwOverdrawOptions() {
        String value = SystemProperties.get("debug.hwui.overdraw");
        if (value == null) {
            value = "";
        }
        CharSequence[] values = this.mDebugHwOverdraw.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mDebugHwOverdraw.setValueIndex(i);
                this.mDebugHwOverdraw.setSummary(this.mDebugHwOverdraw.getEntries()[i]);
                return;
            }
        }
        this.mDebugHwOverdraw.setValueIndex(0);
        this.mDebugHwOverdraw.setSummary(this.mDebugHwOverdraw.getEntries()[0]);
    }

    private void writeDebugHwOverdrawOptions(Object newValue) {
        SystemProperties.set("debug.hwui.overdraw", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateDebugHwOverdrawOptions();
    }

    private void updateDebugLayoutOptions() {
        updateSwitchPreference(this.mDebugLayout, SystemProperties.getBoolean("debug.layout", false));
    }

    private void writeDebugLayoutOptions() {
        SystemProperties.set("debug.layout", this.mDebugLayout.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateSimulateColorSpace() {
        ContentResolver cr = getContentResolver();
        boolean enabled = Settings.Secure.getInt(cr, "accessibility_display_daltonizer_enabled", 0) != 0;
        if (enabled) {
            String mode = Integer.toString(Settings.Secure.getInt(cr, "accessibility_display_daltonizer", -1));
            this.mSimulateColorSpace.setValue(mode);
            int index = this.mSimulateColorSpace.findIndexOfValue(mode);
            if (index < 0) {
                this.mSimulateColorSpace.setSummary(getString(R.string.daltonizer_type_overridden, new Object[]{getString(R.string.accessibility_display_daltonizer_preference_title)}));
                return;
            } else {
                this.mSimulateColorSpace.setSummary("%s");
                return;
            }
        }
        this.mSimulateColorSpace.setValue(Integer.toString(-1));
    }

    private boolean usingDevelopmentColorSpace() {
        ContentResolver cr = getContentResolver();
        boolean enabled = Settings.Secure.getInt(cr, "accessibility_display_daltonizer_enabled", 0) != 0;
        if (enabled) {
            String mode = Integer.toString(Settings.Secure.getInt(cr, "accessibility_display_daltonizer", -1));
            int index = this.mSimulateColorSpace.findIndexOfValue(mode);
            if (index >= 0) {
                return true;
            }
        }
        return false;
    }

    private void writeSimulateColorSpace(Object value) {
        ContentResolver cr = getContentResolver();
        int newMode = Integer.parseInt(value.toString());
        if (newMode < 0) {
            Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", 0);
        } else {
            Settings.Secure.putInt(cr, "accessibility_display_daltonizer_enabled", 1);
            Settings.Secure.putInt(cr, "accessibility_display_daltonizer", newMode);
        }
    }

    private void updateColorTemperature() {
        updateSwitchPreference(this.mColorTemperaturePreference, SystemProperties.getBoolean("persist.sys.debug.color_temp", false));
    }

    private void writeColorTemperature() {
        SystemProperties.set("persist.sys.debug.color_temp", this.mColorTemperaturePreference.isChecked() ? "1" : "0");
        pokeSystemProperties();
        Toast.makeText(getActivity(), R.string.color_temperature_toast, 1).show();
    }

    private void updateUSBAudioOptions() {
        updateSwitchPreference(this.mUSBAudio, Settings.Secure.getInt(getContentResolver(), "usb_audio_automatic_routing_disabled", 0) != 0);
    }

    private void writeUSBAudioOptions() {
        Settings.Secure.putInt(getContentResolver(), "usb_audio_automatic_routing_disabled", this.mUSBAudio.isChecked() ? 1 : 0);
    }

    private void updateForceResizableOptions() {
        updateSwitchPreference(this.mForceResizable, Settings.Global.getInt(getContentResolver(), "force_resizable_activities", 0) != 0);
    }

    private void writeForceResizableOptions() {
        Settings.Global.putInt(getContentResolver(), "force_resizable_activities", this.mForceResizable.isChecked() ? 1 : 0);
    }

    private void updateForceRtlOptions() {
        updateSwitchPreference(this.mForceRtlLayout, Settings.Global.getInt(getActivity().getContentResolver(), "debug.force_rtl", 0) != 0);
    }

    private void writeForceRtlOptions() {
        boolean value = this.mForceRtlLayout.isChecked();
        Settings.Global.putInt(getActivity().getContentResolver(), "debug.force_rtl", value ? 1 : 0);
        SystemProperties.set("debug.force_rtl", value ? "1" : "0");
        LocalePicker.updateLocale(getActivity().getResources().getConfiguration().locale);
    }

    private void updateWifiDisplayCertificationOptions() {
        updateSwitchPreference(this.mWifiDisplayCertification, Settings.Global.getInt(getActivity().getContentResolver(), "wifi_display_certification_on", 0) != 0);
    }

    private void writeWifiDisplayCertificationOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(), "wifi_display_certification_on", this.mWifiDisplayCertification.isChecked() ? 1 : 0);
    }

    private void updateWifiVerboseLoggingOptions() {
        boolean enabled = this.mWifiManager.getVerboseLoggingLevel() > 0;
        updateSwitchPreference(this.mWifiVerboseLogging, enabled);
    }

    private void writeWifiVerboseLoggingOptions() {
        this.mWifiManager.enableVerboseLogging(this.mWifiVerboseLogging.isChecked() ? 1 : 0);
    }

    private void updateWifiAggressiveHandoverOptions() {
        boolean enabled = this.mWifiManager.getAggressiveHandover() > 0;
        updateSwitchPreference(this.mWifiAggressiveHandover, enabled);
    }

    private void writeWifiAggressiveHandoverOptions() {
        this.mWifiManager.enableAggressiveHandover(this.mWifiAggressiveHandover.isChecked() ? 1 : 0);
    }

    private void updateWifiAllowScansWithTrafficOptions() {
        boolean enabled = this.mWifiManager.getAllowScansWithTraffic() > 0;
        updateSwitchPreference(this.mWifiAllowScansWithTraffic, enabled);
    }

    private void writeWifiAllowScansWithTrafficOptions() {
        this.mWifiManager.setAllowScansWithTraffic(this.mWifiAllowScansWithTraffic.isChecked() ? 1 : 0);
    }

    private void updateBluetoothDisableAbsVolumeOptions() {
        updateSwitchPreference(this.mBluetoothDisableAbsVolume, SystemProperties.getBoolean("persist.bluetooth.disableabsvol", false));
    }

    private void writeBluetoothDisableAbsVolumeOptions() {
        SystemProperties.set("persist.bluetooth.disableabsvol", this.mBluetoothDisableAbsVolume.isChecked() ? "true" : "false");
    }

    private void updateMobileDataAlwaysOnOptions() {
        updateSwitchPreference(this.mMobileDataAlwaysOn, Settings.Global.getInt(getActivity().getContentResolver(), "mobile_data_always_on", 0) != 0);
    }

    private void writeMobileDataAlwaysOnOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(), "mobile_data_always_on", this.mMobileDataAlwaysOn.isChecked() ? 1 : 0);
    }

    private String defaultLogdSizeValue() {
        String defaultValue = SystemProperties.get("ro.logd.size");
        if (defaultValue == null || defaultValue.length() == 0) {
            if (SystemProperties.get("ro.config.low_ram").equals("true")) {
                return "65536";
            }
            return "262144";
        }
        return defaultValue;
    }

    private void updateLogdSizeValues() {
        if (this.mLogdSize == null) {
            return;
        }
        String currentTag = SystemProperties.get("persist.log.tag");
        String currentValue = SystemProperties.get("persist.logd.size");
        if (currentTag != null && currentTag.startsWith("Settings")) {
            currentValue = "32768";
        }
        if (currentValue == null || currentValue.length() == 0) {
            currentValue = defaultLogdSizeValue();
        }
        String[] values = getResources().getStringArray(R.array.select_logd_size_values);
        String[] titles = getResources().getStringArray(R.array.select_logd_size_titles);
        int index = 2;
        if (SystemProperties.get("ro.config.low_ram").equals("true")) {
            this.mLogdSize.setEntries(R.array.select_logd_size_lowram_titles);
            titles = getResources().getStringArray(R.array.select_logd_size_lowram_titles);
            index = 1;
        }
        String[] summaries = getResources().getStringArray(R.array.select_logd_size_summaries);
        for (int i = 0; i < titles.length; i++) {
            if (currentValue.equals(values[i]) || currentValue.equals(titles[i])) {
                index = i;
                break;
            }
        }
        this.mLogdSize.setValue(values[index]);
        this.mLogdSize.setSummary(summaries[index]);
        this.mLogdSize.setOnPreferenceChangeListener(this);
    }

    private void writeLogdSizeOption(Object newValue) {
        String snetValue;
        boolean zEquals = newValue != null ? newValue.toString().equals("32768") : false;
        String currentTag = SystemProperties.get("persist.log.tag");
        if (currentTag == null) {
            currentTag = "";
        }
        String newTag = currentTag.replaceAll(",+Settings", "").replaceFirst("^Settings,*", "").replaceAll(",+", ",").replaceFirst(",+$", "");
        if (zEquals) {
            newValue = "65536";
            String snetValue2 = SystemProperties.get("persist.log.tag.snet_event_log");
            if ((snetValue2 == null || snetValue2.length() == 0) && ((snetValue = SystemProperties.get("log.tag.snet_event_log")) == null || snetValue.length() == 0)) {
                SystemProperties.set("persist.log.tag.snet_event_log", "I");
            }
            if (newTag.length() != 0) {
                newTag = "," + newTag;
            }
            newTag = "Settings" + newTag;
        }
        if (!newTag.equals(currentTag)) {
            SystemProperties.set("persist.log.tag", newTag);
        }
        String defaultValue = defaultLogdSizeValue();
        String size = (newValue == null || newValue.toString().length() == 0) ? defaultValue : newValue.toString();
        if (defaultValue.equals(size)) {
            size = "";
        }
        SystemProperties.set("persist.logd.size", size);
        SystemProperties.set("ctl.start", "logd-reinit");
        pokeSystemProperties();
        updateLogdSizeValues();
    }

    public void updateUsbConfigurationValues() {
        if (this.mUsbConfiguration == null) {
            return;
        }
        UsbManager manager = (UsbManager) getSystemService("usb");
        String[] values = getResources().getStringArray(R.array.usb_configuration_values);
        String[] titles = getResources().getStringArray(R.array.usb_configuration_titles);
        int index = 0;
        int i = 0;
        while (true) {
            if (i >= titles.length) {
                break;
            }
            if (!manager.isFunctionEnabled(values[i])) {
                i++;
            } else {
                index = i;
                break;
            }
        }
        this.mUsbConfiguration.setValue(values[index]);
        this.mUsbConfiguration.setSummary(titles[index]);
        this.mUsbConfiguration.setOnPreferenceChangeListener(this);
    }

    private void writeUsbConfigurationOption(Object newValue) {
        UsbManager manager = (UsbManager) getActivity().getSystemService("usb");
        String function = newValue.toString();
        manager.setCurrentFunction(function);
        if (function.equals("none")) {
            manager.setUsbDataUnlocked(false);
        } else {
            manager.setUsbDataUnlocked(true);
        }
    }

    private void updateCpuUsageOptions() {
        updateSwitchPreference(this.mShowCpuUsage, Settings.Global.getInt(getActivity().getContentResolver(), "show_processes", 0) != 0);
    }

    private void writeCpuUsageOptions() {
        boolean value = this.mShowCpuUsage.isChecked();
        Settings.Global.putInt(getActivity().getContentResolver(), "show_processes", value ? 1 : 0);
        Intent service = new Intent().setClassName("com.android.systemui", "com.android.systemui.LoadAverageService");
        if (value) {
            getActivity().startService(service);
        } else {
            getActivity().stopService(service);
        }
    }

    private void writeImmediatelyDestroyActivitiesOptions() {
        try {
            ActivityManagerNative.getDefault().setAlwaysFinish(this.mImmediatelyDestroyActivities.isChecked());
        } catch (RemoteException e) {
        }
    }

    private void updateImmediatelyDestroyActivitiesOptions() {
        updateSwitchPreference(this.mImmediatelyDestroyActivities, Settings.Global.getInt(getActivity().getContentResolver(), "always_finish_activities", 0) != 0);
    }

    private void updateAnimationScaleValue(int which, ListPreference pref) {
        try {
            float scale = this.mWindowManager.getAnimationScale(which);
            if (scale != 1.0f) {
                this.mHaveDebugSettings = true;
            }
            CharSequence[] values = pref.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                float val = Float.parseFloat(values[i].toString());
                if (scale <= val) {
                    pref.setValueIndex(i);
                    pref.setSummary(pref.getEntries()[i]);
                    return;
                }
            }
            pref.setValueIndex(values.length - 1);
            pref.setSummary(pref.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void updateAnimationScaleOptions() {
        updateAnimationScaleValue(0, this.mWindowAnimationScale);
        updateAnimationScaleValue(1, this.mTransitionAnimationScale);
        updateAnimationScaleValue(2, this.mAnimatorDurationScale);
    }

    private void writeAnimationScaleOption(int which, ListPreference pref, Object newValue) {
        float scale;
        if (newValue != null) {
            try {
                scale = Float.parseFloat(newValue.toString());
            } catch (RemoteException e) {
                return;
            }
        } else {
            scale = 1.0f;
        }
        this.mWindowManager.setAnimationScale(which, scale);
        updateAnimationScaleValue(which, pref);
    }

    private void updateOverlayDisplayDevicesOptions() {
        String value = Settings.Global.getString(getActivity().getContentResolver(), "overlay_display_devices");
        if (value == null) {
            value = "";
        }
        CharSequence[] values = this.mOverlayDisplayDevices.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mOverlayDisplayDevices.setValueIndex(i);
                this.mOverlayDisplayDevices.setSummary(this.mOverlayDisplayDevices.getEntries()[i]);
                return;
            }
        }
        this.mOverlayDisplayDevices.setValueIndex(0);
        this.mOverlayDisplayDevices.setSummary(this.mOverlayDisplayDevices.getEntries()[0]);
    }

    private void writeOverlayDisplayDevicesOptions(Object newValue) {
        Settings.Global.putString(getActivity().getContentResolver(), "overlay_display_devices", (String) newValue);
        updateOverlayDisplayDevicesOptions();
    }

    private void updateAppProcessLimitOptions() {
        try {
            int limit = ActivityManagerNative.getDefault().getProcessLimit();
            CharSequence[] values = this.mAppProcessLimit.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                int val = Integer.parseInt(values[i].toString());
                if (val >= limit) {
                    if (i != 0) {
                        this.mHaveDebugSettings = true;
                    }
                    this.mAppProcessLimit.setValueIndex(i);
                    this.mAppProcessLimit.setSummary(this.mAppProcessLimit.getEntries()[i]);
                    return;
                }
            }
            this.mAppProcessLimit.setValueIndex(0);
            this.mAppProcessLimit.setSummary(this.mAppProcessLimit.getEntries()[0]);
        } catch (RemoteException e) {
        }
    }

    private void writeAppProcessLimitOptions(Object newValue) {
        int limit;
        if (newValue != null) {
            try {
                limit = Integer.parseInt(newValue.toString());
            } catch (RemoteException e) {
                return;
            }
        } else {
            limit = -1;
        }
        ActivityManagerNative.getDefault().setProcessLimit(limit);
        updateAppProcessLimitOptions();
    }

    private void writeShowAllANRsOptions() {
        Settings.Secure.putInt(getActivity().getContentResolver(), "anr_show_background", this.mShowAllANRs.isChecked() ? 1 : 0);
    }

    private void updateShowAllANRsOptions() {
        updateSwitchPreference(this.mShowAllANRs, Settings.Secure.getInt(getActivity().getContentResolver(), "anr_show_background", 0) != 0);
    }

    private void confirmEnableOemUnlock() {
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == -1) {
                    Utils.setOemUnlockEnabled(DevelopmentSettings.this.getActivity(), true);
                } else {
                    DevelopmentSettings.this.mEnableOemUnlock.setChecked(false);
                }
            }
        };
        DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (DevelopmentSettings.this.getActivity() == null) {
                    return;
                }
                DevelopmentSettings.this.updateAllOptions();
            }
        };
        new AlertDialog.Builder(getActivity()).setTitle(R.string.confirm_enable_oem_unlock_title).setMessage(R.string.confirm_enable_oem_unlock_text).setPositiveButton(R.string.enable_text, onClickListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).setOnDismissListener(onDismissListener).create().show();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (switchView == this.mSwitchBar.getSwitch() && isChecked != this.mLastEnabledState) {
            if (!isChecked) {
                resetDangerousOptions();
                Settings.Global.putInt(getActivity().getContentResolver(), "development_settings_enabled", 0);
                this.mLastEnabledState = isChecked;
                setPrefsEnabledState(this.mLastEnabledState);
                return;
            }
            this.mDialogClicked = false;
            if (this.mEnableDialog != null) {
                dismissDialogs();
            }
            this.mEnableDialog = new AlertDialog.Builder(getActivity()).setMessage(getActivity().getResources().getString(R.string.dev_settings_warning_message)).setTitle(R.string.dev_settings_warning_title).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).show();
            this.mEnableDialog.setOnDismissListener(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000) {
            if (resultCode != -1) {
                return;
            }
            this.mDebugApp = data.getAction();
            writeDebuggerOptions();
            updateDebuggerOptions();
            return;
        }
        if (requestCode == 1001) {
            if (resultCode != -1) {
                return;
            }
            this.mMockLocationApp = data.getAction();
            writeMockLocation();
            updateMockLocation();
            return;
        }
        if (requestCode == 0) {
            if (resultCode != -1) {
                return;
            }
            if (this.mEnableOemUnlock.isChecked()) {
                confirmEnableOemUnlock();
                return;
            } else {
                Utils.setOemUnlockEnabled(getActivity(), false);
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        if (preference == this.mEnableAdb) {
            if (this.mEnableAdb.isChecked()) {
                this.mDialogClicked = false;
                if (this.mAdbDialog != null) {
                    dismissDialogs();
                }
                this.mAdbDialog = new AlertDialog.Builder(getActivity()).setMessage(getActivity().getResources().getString(R.string.adb_warning_message)).setTitle(R.string.adb_warning_title).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).show();
                this.mAdbDialog.setOnDismissListener(this);
            } else {
                Settings.Global.putInt(getActivity().getContentResolver(), "adb_enabled", 0);
                this.mVerifyAppsOverUsb.setEnabled(false);
                this.mVerifyAppsOverUsb.setChecked(false);
                onPreferenceTreeClick(this.mVerifyAppsOverUsb);
                updateBugreportOptions();
            }
        } else if (preference == this.mClearAdbKeys) {
            if (this.mAdbKeysDialog != null) {
                dismissDialogs();
            }
            this.mAdbKeysDialog = new AlertDialog.Builder(getActivity()).setMessage(R.string.adb_keys_warning_message).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
        } else if (preference == this.mEnableTerminal) {
            PackageManager pm = getActivity().getPackageManager();
            pm.setApplicationEnabledSetting("com.android.terminal", this.mEnableTerminal.isChecked() ? 1 : 0, 0);
        } else if (preference == this.mBugreportInPower) {
            Settings.Secure.putInt(getActivity().getContentResolver(), "bugreport_in_power_menu", this.mBugreportInPower.isChecked() ? 1 : 0);
            setBugreportStorageProviderStatus();
        } else if (preference == this.mKeepScreenOn) {
            Settings.Global.putInt(getActivity().getContentResolver(), "stay_on_while_plugged_in", this.mKeepScreenOn.isChecked() ? 3 : 0);
        } else if (preference == this.mBtHciSnoopLog && this.mBtHciSnoopLog != null) {
            writeBtHciSnoopLogOptions();
        } else if (preference == this.mEnableOemUnlock && this.mEnableOemUnlock.isEnabled()) {
            if (!this.mEnableOemUnlock.isChecked()) {
                Utils.setOemUnlockEnabled(getActivity(), false);
            } else if (!showKeyguardConfirmation(getResources(), 0)) {
                confirmEnableOemUnlock();
            }
        } else if (preference == this.mMockLocationAppPref) {
            Intent intent = new Intent(getActivity(), (Class<?>) AppPicker.class);
            intent.putExtra("com.android.settings.extra.REQUESTIING_PERMISSION", "android.permission.ACCESS_MOCK_LOCATION");
            startActivityForResult(intent, 1001);
        } else if (preference == this.mDebugViewAttributes) {
            Settings.Global.putInt(getActivity().getContentResolver(), "debug_view_attributes", this.mDebugViewAttributes.isChecked() ? 1 : 0);
        } else if (preference == this.mForceAllowOnExternal) {
            Settings.Global.putInt(getActivity().getContentResolver(), "force_allow_on_external", this.mForceAllowOnExternal.isChecked() ? 1 : 0);
        } else if (preference == this.mScreenCaptureOn) {
            Settings.System.putInt(getActivity().getContentResolver(), "screen_capture_on", this.mScreenCaptureOn.isChecked() ? 1 : 0);
        } else if (preference == this.mDebugAppPref) {
            Intent intent2 = new Intent(getActivity(), (Class<?>) AppPicker.class);
            intent2.putExtra("com.android.settings.extra.DEBUGGABLE", true);
            startActivityForResult(intent2, 1000);
        } else if (preference == this.mWaitForDebugger) {
            writeDebuggerOptions();
        } else if (preference == this.mVerifyAppsOverUsb) {
            writeVerifyAppsOverUsbOptions();
        } else if (preference == this.mOtaDisableAutomaticUpdate) {
            writeOtaDisableAutomaticUpdateOptions();
        } else if (preference == this.mStrictMode) {
            writeStrictModeVisualOptions();
        } else if (preference == this.mPointerLocation) {
            writePointerLocationOptions();
        } else if (preference == this.mShowTouches) {
            writeShowTouchesOptions();
        } else if (preference == this.mShowScreenUpdates) {
            writeShowUpdatesOption();
        } else if (preference == this.mDisableOverlays) {
            writeDisableOverlaysOption();
        } else if (preference == this.mShowCpuUsage) {
            writeCpuUsageOptions();
        } else if (preference == this.mImmediatelyDestroyActivities) {
            writeImmediatelyDestroyActivitiesOptions();
        } else if (preference == this.mShowAllANRs) {
            writeShowAllANRsOptions();
        } else if (preference == this.mForceHardwareUi) {
            writeHardwareUiOptions();
        } else if (preference == this.mForceMsaa) {
            writeMsaaOptions();
        } else if (preference == this.mShowHwScreenUpdates) {
            writeShowHwScreenUpdatesOptions();
        } else if (preference == this.mShowHwLayersUpdates) {
            writeShowHwLayersUpdatesOptions();
        } else if (preference == this.mDebugLayout) {
            writeDebugLayoutOptions();
        } else if (preference == this.mForceRtlLayout) {
            writeForceRtlOptions();
        } else if (preference == this.mWifiDisplayCertification) {
            writeWifiDisplayCertificationOptions();
        } else if (preference == this.mWifiVerboseLogging) {
            writeWifiVerboseLoggingOptions();
        } else if (preference == this.mWifiAggressiveHandover) {
            writeWifiAggressiveHandoverOptions();
        } else if (preference == this.mWifiAllowScansWithTraffic) {
            writeWifiAllowScansWithTrafficOptions();
        } else if (preference == this.mMobileDataAlwaysOn) {
            writeMobileDataAlwaysOnOptions();
        } else if (preference == this.mColorTemperaturePreference) {
            writeColorTemperature();
        } else if (preference == this.mUSBAudio) {
            writeUSBAudioOptions();
        } else if (preference == this.mForceResizable) {
            writeForceResizableOptions();
        } else if ("inactive_apps".equals(preference.getKey())) {
            startInactiveAppsFragment();
        } else if ("background_check".equals(preference.getKey())) {
            startBackgroundCheckFragment();
        } else if (preference == this.mBluetoothDisableAbsVolume) {
            writeBluetoothDisableAbsVolumeOptions();
        } else if (preference == this.mWebViewMultiprocess) {
            writeWebViewMultiprocessOptions();
        } else {
            if (!"reset_shortcut_manager_throttling".equals(preference.getKey())) {
                return super.onPreferenceTreeClick(preference);
            }
            confirmResetShortcutManagerThrottling();
        }
        return false;
    }

    private void startInactiveAppsFragment() {
        ((SettingsActivity) getActivity()).startPreferencePanel(InactiveApps.class.getName(), null, R.string.inactive_apps_title, null, null, 0);
    }

    private void startBackgroundCheckFragment() {
        ((SettingsActivity) getActivity()).startPreferencePanel(BackgroundCheckSummary.class.getName(), null, R.string.background_check_title, null, null, 0);
    }

    private boolean showKeyguardConfirmation(Resources resources, int requestCode) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(requestCode, resources.getString(R.string.oem_unlock_enable));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ("hdcp_checking".equals(preference.getKey())) {
            SystemProperties.set("persist.sys.hdcp_checking", newValue.toString());
            updateHdcpValues();
            pokeSystemProperties();
            return true;
        }
        if (preference == this.mWebViewProvider) {
            if (newValue == null) {
                Log.e("DevelopmentSettings", "Tried to set a null WebView provider");
                return false;
            }
            if (writeWebViewProviderOptions(newValue)) {
                return true;
            }
            Toast toast = Toast.makeText(getActivity(), R.string.select_webview_provider_toast_text, 0);
            toast.show();
            return false;
        }
        if (preference == this.mLogdSize) {
            writeLogdSizeOption(newValue);
            return true;
        }
        if (preference == this.mUsbConfiguration) {
            writeUsbConfigurationOption(newValue);
            return true;
        }
        if (preference == this.mWindowAnimationScale) {
            writeAnimationScaleOption(0, this.mWindowAnimationScale, newValue);
            return true;
        }
        if (preference == this.mTransitionAnimationScale) {
            writeAnimationScaleOption(1, this.mTransitionAnimationScale, newValue);
            return true;
        }
        if (preference == this.mAnimatorDurationScale) {
            writeAnimationScaleOption(2, this.mAnimatorDurationScale, newValue);
            return true;
        }
        if (preference == this.mOverlayDisplayDevices) {
            writeOverlayDisplayDevicesOptions(newValue);
            return true;
        }
        if (preference == this.mTrackFrameTime) {
            writeTrackFrameTimeOptions(newValue);
            return true;
        }
        if (preference == this.mDebugHwOverdraw) {
            writeDebugHwOverdrawOptions(newValue);
            return true;
        }
        if (preference == this.mShowNonRectClip) {
            writeShowNonRectClipOptions(newValue);
            return true;
        }
        if (preference == this.mAppProcessLimit) {
            writeAppProcessLimitOptions(newValue);
            return true;
        }
        if (preference != this.mSimulateColorSpace) {
            return false;
        }
        writeSimulateColorSpace(newValue);
        return true;
    }

    private void dismissDialogs() {
        if (this.mAdbDialog != null) {
            this.mAdbDialog.dismiss();
            this.mAdbDialog = null;
        }
        if (this.mAdbKeysDialog != null) {
            this.mAdbKeysDialog.dismiss();
            this.mAdbKeysDialog = null;
        }
        if (this.mEnableDialog == null) {
            return;
        }
        this.mEnableDialog.dismiss();
        this.mEnableDialog = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (dialog == this.mAdbDialog) {
            if (which == -1) {
                this.mDialogClicked = true;
                Settings.Global.putInt(getActivity().getContentResolver(), "adb_enabled", 1);
                this.mVerifyAppsOverUsb.setEnabled(true);
                updateVerifyAppsOverUsbOptions();
                updateBugreportOptions();
                return;
            }
            this.mEnableAdb.setChecked(false);
            return;
        }
        if (dialog == this.mAdbKeysDialog) {
            if (which != -1) {
                return;
            }
            try {
                IBinder b = ServiceManager.getService("usb");
                IUsbManager service = IUsbManager.Stub.asInterface(b);
                service.clearUsbDebuggingKeys();
                return;
            } catch (RemoteException e) {
                Log.e("DevelopmentSettings", "Unable to clear adb keys", e);
                return;
            }
        }
        if (dialog != this.mEnableDialog) {
            return;
        }
        if (which == -1) {
            this.mDialogClicked = true;
            Settings.Global.putInt(getActivity().getContentResolver(), "development_settings_enabled", 1);
            this.mLastEnabledState = true;
            setPrefsEnabledState(this.mLastEnabledState);
            return;
        }
        this.mSwitchBar.setChecked(false);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dialog == this.mAdbDialog) {
            if (!this.mDialogClicked) {
                this.mEnableAdb.setChecked(false);
            }
            this.mAdbDialog = null;
        } else {
            if (dialog != this.mEnableDialog) {
                return;
            }
            if (!this.mDialogClicked) {
                this.mSwitchBar.setChecked(false);
            }
            this.mEnableDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        dismissDialogs();
        super.onDestroy();
    }

    void pokeSystemProperties() {
        if (this.mDontPokeProperties) {
            return;
        }
        new SystemPropPoker().execute(new Void[0]);
    }

    public static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        @Override
        public Void doInBackground(Void... params) {
            String[] services = ServiceManager.listServices();
            for (String service : services) {
                IBinder obj = ServiceManager.checkService(service);
                if (obj != null) {
                    Parcel data = Parcel.obtain();
                    try {
                        obj.transact(1599295570, data, null, 0);
                    } catch (RemoteException e) {
                    } catch (Exception e2) {
                        Log.i("DevelopmentSettings", "Someone wrote a bad service '" + service + "' that doesn't like to be poked: " + e2);
                    }
                    data.recycle();
                }
            }
            return null;
        }
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void confirmResetShortcutManagerThrottling() {
        final IShortcutService service = IShortcutService.Stub.asInterface(ServiceManager.getService("shortcut"));
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which != -1) {
                    return;
                }
                try {
                    service.resetThrottling();
                } catch (RemoteException e) {
                }
            }
        };
        new AlertDialog.Builder(getActivity()).setTitle(R.string.confirm_reset_shortcut_manager_throttling_title).setMessage(R.string.confirm_reset_shortcut_manager_throttling_message).setPositiveButton(R.string.okay, onClickListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create().show();
    }

    private int getUid(File file) {
        if (!file.exists()) {
            return -1;
        }
        return FileUtils.getUid(file.getPath());
    }
}
