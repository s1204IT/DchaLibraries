package com.android.settings;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.backup.IBackupManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.usb.IUsbManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.IWindowManager;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class DevelopmentSettings extends SettingsPreferenceFragment implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener, Indexable, SwitchBar.OnSwitchChangeListener {
    private static String DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES = "262144";
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
                return keys;
            }
            return keys;
        }
    };
    private Dialog mAdbDialog;
    private Dialog mAdbKeysDialog;
    private SwitchPreference mAllowMockLocation;
    private ListPreference mAnimatorDurationScale;
    private ListPreference mAppProcessLimit;
    private IBackupManager mBackupManager;
    private SwitchPreference mBtHciSnoopLog;
    private Preference mBugreport;
    private SwitchPreference mBugreportInPower;
    private Preference mClearAdbKeys;
    private String mDebugApp;
    private Preference mDebugAppPref;
    private ListPreference mDebugHwOverdraw;
    private SwitchPreference mDebugLayout;
    private SwitchPreference mDebugViewAttributes;
    private boolean mDialogClicked;
    private SwitchPreference mDisableOverlays;
    private boolean mDontPokeProperties;
    private DevicePolicyManager mDpm;
    private SwitchPreference mEnableAdb;
    private Dialog mEnableDialog;
    private SwitchPreference mEnableOemUnlock;
    private SwitchPreference mEnableTerminal;
    private SwitchPreference mForceHardwareUi;
    private SwitchPreference mForceMsaa;
    private SwitchPreference mForceRtlLayout;
    private boolean mHaveDebugSettings;
    private SwitchPreference mImmediatelyDestroyActivities;
    private SwitchPreference mIspParameters;
    private SwitchPreference mKeepScreenOn;
    private boolean mLastEnabledState;
    private ListPreference mLogdSize;
    private ListPreference mOpenGLTraces;
    private ListPreference mOverlayDisplayDevices;
    private PreferenceScreen mPassword;
    private SwitchPreference mPointerLocation;
    private PreferenceScreen mProcessStats;
    private SwitchPreference mRawDump;
    private ListPreference mRawProcessType;
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
    private Preference mTouchPanelVersionPref;
    private ListPreference mTrackFrameTime;
    private ListPreference mTransitionAnimationScale;
    private SwitchPreference mUSBAudio;
    private UserManager mUm;
    private boolean mUnavailable;
    private SwitchPreference mUseAwesomePlayer;
    private SwitchPreference mVerifyAppsOverUsb;
    private SwitchPreference mWaitForDebugger;
    private SwitchPreference mWifiAggressiveHandover;
    private SwitchPreference mWifiAllowScansWithTraffic;
    private SwitchPreference mWifiDisplayCertification;
    private WifiManager mWifiManager;
    private SwitchPreference mWifiVerboseLogging;
    private ListPreference mWindowAnimationScale;
    private IWindowManager mWindowManager;
    private final ArrayList<Preference> mAllPrefs = new ArrayList<>();
    private final ArrayList<SwitchPreference> mResetSwitchPrefs = new ArrayList<>();
    private final HashSet<Preference> mDisabledPrefs = new HashSet<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mBackupManager = IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        this.mUm = (UserManager) getSystemService("user");
        this.mWifiManager = (WifiManager) getSystemService("wifi");
        if (Process.myUserHandle().getIdentifier() != 0 || this.mUm.hasUserRestriction("no_debugging_features") || Settings.Global.getInt(getActivity().getContentResolver(), "device_provisioned", 0) == 0) {
            this.mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getActivity(), null));
            return;
        }
        addPreferencesFromResource(R.xml.development_prefs);
        PreferenceGroup debugDebuggingCategory = (PreferenceGroup) findPreference("debug_debugging_category");
        this.mTouchPanelVersionPref = findPreference("touchpanel_version");
        if ("TAB-A03-BS".equals(Build.MODEL)) {
            this.mTouchPanelVersionPref.setSummary(getDeviceVersion("elan-touchscreen"));
            this.mAllPrefs.add(this.mTouchPanelVersionPref);
            this.mDisabledPrefs.add(this.mTouchPanelVersionPref);
        } else {
            getPreferenceScreen().removePreference(this.mTouchPanelVersionPref);
            this.mTouchPanelVersionPref = null;
        }
        this.mEnableAdb = findAndInitSwitchPref("enable_adb");
        this.mClearAdbKeys = findPreference("clear_adb_keys");
        if (!SystemProperties.getBoolean("ro.adb.secure", false) && debugDebuggingCategory != null) {
            debugDebuggingCategory.removePreference(this.mClearAdbKeys);
        }
        this.mAllPrefs.add(this.mClearAdbKeys);
        this.mEnableTerminal = findAndInitSwitchPref("enable_terminal");
        if (!isPackageInstalled(getActivity(), "com.android.terminal")) {
            debugDebuggingCategory.removePreference(this.mEnableTerminal);
            this.mEnableTerminal = null;
        }
        this.mBugreport = findPreference("bugreport");
        this.mBugreportInPower = findAndInitSwitchPref("bugreport_in_power");
        this.mKeepScreenOn = findAndInitSwitchPref("keep_screen_on");
        this.mBtHciSnoopLog = findAndInitSwitchPref("bt_hci_snoop_log");
        this.mEnableOemUnlock = findAndInitSwitchPref("oem_unlock_enable");
        if (!showEnableOemUnlockPreference()) {
            removePreference(this.mEnableOemUnlock);
            this.mEnableOemUnlock = null;
        }
        this.mAllowMockLocation = findAndInitSwitchPref("allow_mock_location");
        this.mScreenCaptureOn = findAndInitSwitchPref("screen_capture_on");
        this.mDebugViewAttributes = findAndInitSwitchPref("debug_view_attributes");
        this.mPassword = (PreferenceScreen) findPreference("local_backup_password");
        this.mAllPrefs.add(this.mPassword);
        if (!Process.myUserHandle().equals(UserHandle.OWNER)) {
            disableForUser(this.mEnableAdb);
            disableForUser(this.mClearAdbKeys);
            disableForUser(this.mEnableTerminal);
            disableForUser(this.mPassword);
        }
        this.mDebugAppPref = findPreference("debug_app");
        this.mAllPrefs.add(this.mDebugAppPref);
        this.mWaitForDebugger = findAndInitSwitchPref("wait_for_debugger");
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
        this.mLogdSize = addListPreference("select_logd_size");
        this.mWindowAnimationScale = addListPreference("window_animation_scale");
        this.mTransitionAnimationScale = addListPreference("transition_animation_scale");
        this.mAnimatorDurationScale = addListPreference("animator_duration_scale");
        this.mOverlayDisplayDevices = addListPreference("overlay_display_devices");
        this.mOpenGLTraces = addListPreference("enable_opengl_traces");
        this.mSimulateColorSpace = addListPreference("simulate_color_space");
        this.mUseAwesomePlayer = findAndInitSwitchPref("use_awesomeplayer");
        this.mUSBAudio = findAndInitSwitchPref("usb_audio");
        this.mImmediatelyDestroyActivities = (SwitchPreference) findPreference("immediately_destroy_activities");
        this.mAllPrefs.add(this.mImmediatelyDestroyActivities);
        this.mResetSwitchPrefs.add(this.mImmediatelyDestroyActivities);
        this.mAppProcessLimit = addListPreference("app_process_limit");
        this.mShowAllANRs = (SwitchPreference) findPreference("show_all_anrs");
        this.mAllPrefs.add(this.mShowAllANRs);
        this.mResetSwitchPrefs.add(this.mShowAllANRs);
        this.mRawDump = findAndInitSwitchPref("raw_dump");
        this.mIspParameters = findAndInitSwitchPref("isp_parameters");
        this.mRawProcessType = addListPreference("raw_process_type");
        if ("0".equals(SystemProperties.get("persist.service.camera.isptype")) || "".equals(SystemProperties.get("persist.service.camera.isptype"))) {
            ((PreferenceGroup) findPreference("debug_camera_category")).removePreference(findPreference("raw_process_type"));
        }
        Preference hdcpChecking = findPreference("hdcp_checking");
        if (hdcpChecking != null) {
            this.mAllPrefs.add(hdcpChecking);
            removePreferenceForProduction(hdcpChecking);
        }
        this.mProcessStats = (PreferenceScreen) findPreference("proc_stats");
        this.mAllPrefs.add(this.mProcessStats);
    }

    private ListPreference addListPreference(String prefKey) {
        ListPreference pref = (ListPreference) findPreference(prefKey);
        this.mAllPrefs.add(pref);
        pref.setOnPreferenceChangeListener(this);
        return pref;
    }

    private void disableForUser(Preference pref) {
        if (pref != null) {
            pref.setEnabled(false);
            this.mDisabledPrefs.add(pref);
        }
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
        } else {
            this.mSwitchBar.addOnSwitchChangeListener(this);
        }
    }

    private boolean removePreferenceForProduction(Preference preference) {
        if (!"user".equals(Build.TYPE)) {
            return false;
        }
        removePreference(preference);
        return true;
    }

    private void removePreference(Preference preference) {
        getPreferenceScreen().removePreference(preference);
        this.mAllPrefs.remove(preference);
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
        if (this.mUnavailable) {
            TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
            getListView().setEmptyView(emptyView);
            if (emptyView != null) {
                emptyView.setText(R.string.development_settings_not_available);
                return;
            }
            return;
        }
        if (this.mDpm.getMaximumTimeToLock(null) > 0) {
            this.mDisabledPrefs.add(this.mKeepScreenOn);
        } else {
            this.mDisabledPrefs.remove(this.mKeepScreenOn);
        }
        String rawDump = SystemProperties.get("persist.service.camera.rawdump");
        String rawProcessType = SystemProperties.get("persist.service.camera.rawproc");
        if ("1".equals(SystemProperties.get("persist.service.camera.isptype"))) {
            if (!"0".equals(rawDump) && !rawDump.equals("")) {
                this.mDisabledPrefs.add(this.mRawProcessType);
            } else if (!"0".equals(rawProcessType) && !rawProcessType.equals("")) {
                this.mDisabledPrefs.add(this.mRawDump);
            }
        }
        ContentResolver cr = getActivity().getContentResolver();
        this.mLastEnabledState = Settings.Global.getInt(cr, "development_settings_enabled", 0) != 0;
        this.mSwitchBar.setChecked(this.mLastEnabledState);
        setPrefsEnabledState(this.mLastEnabledState);
        if (this.mHaveDebugSettings && !this.mLastEnabledState) {
            Settings.Global.putInt(getActivity().getContentResolver(), "development_settings_enabled", 1);
            this.mLastEnabledState = true;
            this.mSwitchBar.setChecked(this.mLastEnabledState);
            setPrefsEnabledState(this.mLastEnabledState);
        }
        this.mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!this.mUnavailable) {
            this.mSwitchBar.removeOnSwitchChangeListener(this);
            this.mSwitchBar.hide();
        }
    }

    void updateSwitchPreference(SwitchPreference switchPreference, boolean value) {
        switchPreference.setChecked(value);
        this.mHaveDebugSettings |= value;
    }

    private void updateAllOptions() {
        Context context = getActivity();
        ContentResolver cr = context.getContentResolver();
        this.mHaveDebugSettings = false;
        updateSwitchPreference(this.mEnableAdb, Settings.Global.getInt(cr, "adb_enabled", 0) != 0);
        if (this.mEnableTerminal != null) {
            updateSwitchPreference(this.mEnableTerminal, context.getPackageManager().getApplicationEnabledSetting("com.android.terminal") == 1);
        }
        updateSwitchPreference(this.mBugreportInPower, Settings.Secure.getInt(cr, "bugreport_in_power_menu", 0) != 0);
        updateSwitchPreference(this.mKeepScreenOn, Settings.Global.getInt(cr, "stay_on_while_plugged_in", 0) != 0);
        updateSwitchPreference(this.mBtHciSnoopLog, Settings.Secure.getInt(cr, "bluetooth_hci_log", 0) != 0);
        if (this.mEnableOemUnlock != null) {
            updateSwitchPreference(this.mEnableOemUnlock, Utils.isOemUnlockEnabled(getActivity()));
        }
        updateSwitchPreference(this.mAllowMockLocation, Settings.Secure.getInt(cr, "mock_location", 0) != 0);
        updateSwitchPreference(this.mDebugViewAttributes, Settings.Global.getInt(cr, "debug_view_attributes", 0) != 0);
        updateSwitchPreference(this.mScreenCaptureOn, Settings.System.getInt(cr, "screen_capture_on", 0) != 0);
        updateHdcpValues();
        updatePasswordSummary();
        updateDebuggerOptions();
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
        updateOpenGLTracesOptions();
        updateImmediatelyDestroyActivitiesOptions();
        updateAppProcessLimitOptions();
        updateShowAllANRsOptions();
        updateVerifyAppsOverUsbOptions();
        updateBugreportOptions();
        updateForceRtlOptions();
        updateLogdSizeValues();
        updateWifiDisplayCertificationOptions();
        updateWifiVerboseLoggingOptions();
        updateWifiAggressiveHandoverOptions();
        updateWifiAllowScansWithTrafficOptions();
        updateSimulateColorSpace();
        updateUseNuplayerOptions();
        updateUSBAudioOptions();
        updateRawDumpOptions();
        updateRawProcessTypeOptions();
        updateIspParametersOptions();
    }

    private void resetDangerousOptions() {
        this.mDontPokeProperties = true;
        for (int i = 0; i < this.mResetSwitchPrefs.size(); i++) {
            SwitchPreference cb = this.mResetSwitchPrefs.get(i);
            if (cb.isChecked()) {
                cb.setChecked(false);
                onPreferenceTreeClick(null, cb);
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

    private void updateHdcpValues() {
        ListPreference hdcpChecking = (ListPreference) findPreference("hdcp_checking");
        if (hdcpChecking != null) {
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
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.configHciSnoopLog(this.mBtHciSnoopLog.isChecked());
        Settings.Secure.putInt(getActivity().getContentResolver(), "bluetooth_hci_log", this.mBtHciSnoopLog.isChecked() ? 1 : 0);
    }

    private void writeDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(this.mDebugApp, this.mWaitForDebugger.isChecked(), true);
        } catch (RemoteException e) {
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

    private void updateVerifyAppsOverUsbOptions() {
        updateSwitchPreference(this.mVerifyAppsOverUsb, Settings.Global.getInt(getActivity().getContentResolver(), "verifier_verify_adb_installs", 1) != 0);
        this.mVerifyAppsOverUsb.setEnabled(enableVerifierSetting());
    }

    private void writeVerifyAppsOverUsbOptions() {
        Settings.Global.putInt(getActivity().getContentResolver(), "verifier_verify_adb_installs", this.mVerifyAppsOverUsb.isChecked() ? 1 : 0);
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

    private static boolean showEnableOemUnlockPreference() {
        return !SystemProperties.get("ro.frp.pst").equals("");
    }

    private void updateBugreportOptions() {
        if ("user".equals(Build.TYPE)) {
            ContentResolver resolver = getActivity().getContentResolver();
            boolean adbEnabled = Settings.Global.getInt(resolver, "adb_enabled", 0) != 0;
            if (adbEnabled) {
                this.mBugreport.setEnabled(true);
                this.mBugreportInPower.setEnabled(true);
                return;
            } else {
                this.mBugreport.setEnabled(false);
                this.mBugreportInPower.setEnabled(false);
                this.mBugreportInPower.setChecked(false);
                Settings.Secure.putInt(resolver, "bugreport_in_power_menu", 0);
                return;
            }
        }
        this.mBugreportInPower.setEnabled(true);
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
            if (flinger != null) {
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
            }
        } catch (RemoteException e) {
        }
    }

    private void writeShowUpdatesOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                int showUpdates = this.mShowScreenUpdates.isChecked() ? 1 : 0;
                data.writeInt(showUpdates);
                flinger.transact(1002, data, null, 0);
                data.recycle();
                updateFlingerOptions();
            }
        } catch (RemoteException e) {
        }
    }

    private void writeDisableOverlaysOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                int disableOverlays = this.mDisableOverlays.isChecked() ? 1 : 0;
                data.writeInt(disableOverlays);
                flinger.transact(1008, data, null, 0);
                data.recycle();
                updateFlingerOptions();
            }
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

    private void updateUseNuplayerOptions() {
        updateSwitchPreference(this.mUseAwesomePlayer, SystemProperties.getBoolean("persist.sys.media.use-awesome", false));
    }

    private void writeUseAwesomePlayerOptions() {
        SystemProperties.set("persist.sys.media.use-awesome", this.mUseAwesomePlayer.isChecked() ? "true" : "false");
        pokeSystemProperties();
    }

    private void updateUSBAudioOptions() {
        updateSwitchPreference(this.mUSBAudio, Settings.Secure.getInt(getContentResolver(), "usb_audio_automatic_routing_disabled", 0) != 0);
    }

    private void writeUSBAudioOptions() {
        Settings.Secure.putInt(getContentResolver(), "usb_audio_automatic_routing_disabled", this.mUSBAudio.isChecked() ? 1 : 0);
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

    private void updateLogdSizeValues() {
        if (this.mLogdSize != null) {
            String currentValue = SystemProperties.get("persist.logd.size");
            if (currentValue == null && (currentValue = SystemProperties.get("ro.logd.size")) == null) {
                currentValue = "256K";
            }
            String[] values = getResources().getStringArray(R.array.select_logd_size_values);
            String[] titles = getResources().getStringArray(R.array.select_logd_size_titles);
            if (SystemProperties.get("ro.config.low_ram").equals("true")) {
                this.mLogdSize.setEntries(R.array.select_logd_size_lowram_titles);
                titles = getResources().getStringArray(R.array.select_logd_size_lowram_titles);
            }
            String[] summaries = getResources().getStringArray(R.array.select_logd_size_summaries);
            int index = 1;
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
    }

    private void writeLogdSizeOption(Object newValue) {
        String currentValue = SystemProperties.get("ro.logd.size");
        if (currentValue != null) {
            DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES = currentValue;
        }
        String size = newValue != null ? newValue.toString() : DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES;
        SystemProperties.set("persist.logd.size", size);
        pokeSystemProperties();
        try {
            Process p = Runtime.getRuntime().exec("logcat -b all -G " + size);
            p.waitFor();
            Log.i("DevelopmentSettings", "Logcat ring buffer sizes set to: " + size);
        } catch (Exception e) {
            Log.w("DevelopmentSettings", "Cannot set logcat ring buffer sizes", e);
        }
        updateLogdSizeValues();
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
        if (newValue == null) {
            scale = 1.0f;
        } else {
            try {
                scale = Float.parseFloat(newValue.toString());
            } catch (RemoteException e) {
                return;
            }
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

    private void updateOpenGLTracesOptions() {
        String value = SystemProperties.get("debug.egl.trace");
        if (value == null) {
            value = "";
        }
        CharSequence[] values = this.mOpenGLTraces.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mOpenGLTraces.setValueIndex(i);
                this.mOpenGLTraces.setSummary(this.mOpenGLTraces.getEntries()[i]);
                return;
            }
        }
        this.mOpenGLTraces.setValueIndex(0);
        this.mOpenGLTraces.setSummary(this.mOpenGLTraces.getEntries()[0]);
    }

    private void writeOpenGLTracesOptions(Object newValue) {
        SystemProperties.set("debug.egl.trace", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateOpenGLTracesOptions();
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
        if (newValue == null) {
            limit = -1;
        } else {
            try {
                limit = Integer.parseInt(newValue.toString());
            } catch (RemoteException e) {
                return;
            }
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
        DialogInterface.OnClickListener onConfirmListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Utils.setOemUnlockEnabled(DevelopmentSettings.this.getActivity(), true);
                DevelopmentSettings.this.updateAllOptions();
            }
        };
        new AlertDialog.Builder(getActivity()).setTitle(R.string.confirm_enable_oem_unlock_title).setMessage(R.string.confirm_enable_oem_unlock_text).setPositiveButton(R.string.enable_text, onConfirmListener).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create().show();
    }

    private void updateRawDumpOptions() {
        String value = SystemProperties.get("persist.service.camera.rawdump");
        boolean rawDump = ("0".equals(value) || "".equals(value)) ? false : true;
        updateSwitchPreference(this.mRawDump, rawDump);
    }

    private void writeRawDumpOptions() {
        String str;
        boolean boxStatus = this.mRawDump.isChecked();
        boolean isA51Isp = "0".equals(SystemProperties.get("persist.service.camera.isptype")) || "".equals(SystemProperties.get("persist.service.camera.isptype"));
        if (boxStatus) {
            str = isA51Isp ? "1" : "2";
        } else {
            str = "0";
        }
        SystemProperties.set("persist.service.camera.rawdump", str);
        pokeSystemProperties();
        if ("1".equals(SystemProperties.get("persist.service.camera.isptype"))) {
            if (boxStatus) {
                this.mDisabledPrefs.add(this.mRawProcessType);
            } else {
                this.mDisabledPrefs.remove(this.mRawProcessType);
            }
        }
        ContentResolver cr = getActivity().getContentResolver();
        this.mLastEnabledState = Settings.Global.getInt(cr, "development_settings_enabled", 0) != 0;
        this.mSwitchBar.setChecked(this.mLastEnabledState);
        setPrefsEnabledState(this.mLastEnabledState);
    }

    private void updateIspParametersOptions() {
        String value = SystemProperties.get("persist.service.camera.ispparam");
        boolean ispParam = ("0".equals(value) || "".equals(value)) ? false : true;
        updateSwitchPreference(this.mIspParameters, ispParam);
    }

    private void writeIspParametersOptions() {
        String str;
        boolean boxStatus = this.mIspParameters.isChecked();
        boolean isA51Isp = "0".equals(SystemProperties.get("persist.service.camera.isptype")) || "".equals(SystemProperties.get("persist.service.camera.isptype"));
        if (boxStatus) {
            str = isA51Isp ? "1" : "2";
        } else {
            str = "0";
        }
        SystemProperties.set("persist.service.camera.ispparam", str);
        pokeSystemProperties();
    }

    private void updateRawProcessTypeOptions() {
        String value = SystemProperties.get("persist.service.camera.rawproc");
        CharSequence[] values = this.mRawProcessType.getEntryValues();
        for (int i = 0; i < values.length; i++) {
            if (value.contentEquals(values[i])) {
                this.mRawProcessType.setValueIndex(i);
                this.mRawProcessType.setSummary(this.mRawProcessType.getEntries()[i]);
                return;
            }
        }
        this.mRawProcessType.setValueIndex(0);
        this.mRawProcessType.setSummary(this.mRawProcessType.getEntries()[0]);
    }

    private void writeRawProcessTypeOptions(Object newValue) {
        SystemProperties.set("persist.service.camera.rawproc", newValue == null ? "" : newValue.toString());
        pokeSystemProperties();
        updateRawProcessTypeOptions();
        if (!"0".equals(newValue.toString())) {
            this.mDisabledPrefs.add(this.mRawDump);
        } else {
            this.mDisabledPrefs.remove(this.mRawDump);
        }
        ContentResolver cr = getActivity().getContentResolver();
        this.mLastEnabledState = Settings.Global.getInt(cr, "development_settings_enabled", 0) != 0;
        this.mSwitchBar.setChecked(this.mLastEnabledState);
        setPrefsEnabledState(this.mLastEnabledState);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (switchView == this.mSwitchBar.getSwitch() && isChecked != this.mLastEnabledState) {
            if (isChecked) {
                this.mDialogClicked = false;
                if (this.mEnableDialog != null) {
                    dismissDialogs();
                }
                this.mEnableDialog = new AlertDialog.Builder(getActivity()).setMessage(getActivity().getResources().getString(R.string.dev_settings_warning_message)).setTitle(R.string.dev_settings_warning_title).setPositiveButton(android.R.string.yes, this).setNegativeButton(android.R.string.no, this).show();
                this.mEnableDialog.setOnDismissListener(this);
                return;
            }
            resetDangerousOptions();
            Settings.Global.putInt(getActivity().getContentResolver(), "development_settings_enabled", 0);
            this.mLastEnabledState = isChecked;
            setPrefsEnabledState(this.mLastEnabledState);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1000) {
            if (resultCode == -1) {
                this.mDebugApp = data.getAction();
                writeDebuggerOptions();
                updateDebuggerOptions();
                return;
            }
            return;
        }
        if (requestCode == 0) {
            if (resultCode == -1) {
                if (this.mEnableOemUnlock.isChecked()) {
                    confirmEnableOemUnlock();
                    return;
                } else {
                    Utils.setOemUnlockEnabled(getActivity(), false);
                    return;
                }
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
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
                return false;
            }
            Settings.Global.putInt(getActivity().getContentResolver(), "adb_enabled", 0);
            this.mVerifyAppsOverUsb.setEnabled(false);
            this.mVerifyAppsOverUsb.setChecked(false);
            updateBugreportOptions();
            return false;
        }
        if (preference == this.mClearAdbKeys) {
            if (this.mAdbKeysDialog != null) {
                dismissDialogs();
            }
            this.mAdbKeysDialog = new AlertDialog.Builder(getActivity()).setMessage(R.string.adb_keys_warning_message).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).show();
            return false;
        }
        if (preference == this.mEnableTerminal) {
            PackageManager pm = getActivity().getPackageManager();
            pm.setApplicationEnabledSetting("com.android.terminal", this.mEnableTerminal.isChecked() ? 1 : 0, 0);
            return false;
        }
        if (preference == this.mBugreportInPower) {
            Settings.Secure.putInt(getActivity().getContentResolver(), "bugreport_in_power_menu", this.mBugreportInPower.isChecked() ? 1 : 0);
            return false;
        }
        if (preference == this.mKeepScreenOn) {
            Settings.Global.putInt(getActivity().getContentResolver(), "stay_on_while_plugged_in", this.mKeepScreenOn.isChecked() ? 3 : 0);
            return false;
        }
        if (preference == this.mBtHciSnoopLog) {
            writeBtHciSnoopLogOptions();
            return false;
        }
        if (preference == this.mEnableOemUnlock) {
            if (showKeyguardConfirmation(getResources(), 0)) {
                return false;
            }
            if (this.mEnableOemUnlock.isChecked()) {
                confirmEnableOemUnlock();
                return false;
            }
            Utils.setOemUnlockEnabled(getActivity(), false);
            return false;
        }
        if (preference == this.mAllowMockLocation) {
            Settings.Secure.putInt(getActivity().getContentResolver(), "mock_location", this.mAllowMockLocation.isChecked() ? 1 : 0);
            return false;
        }
        if (preference == this.mScreenCaptureOn) {
            Settings.System.putInt(getActivity().getContentResolver(), "screen_capture_on", this.mScreenCaptureOn.isChecked() ? 1 : 0);
            return false;
        }
        if (preference == this.mDebugViewAttributes) {
            Settings.Global.putInt(getActivity().getContentResolver(), "debug_view_attributes", this.mDebugViewAttributes.isChecked() ? 1 : 0);
            return false;
        }
        if (preference == this.mDebugAppPref) {
            startActivityForResult(new Intent(getActivity(), (Class<?>) AppPicker.class), 1000);
            return false;
        }
        if (preference == this.mWaitForDebugger) {
            writeDebuggerOptions();
            return false;
        }
        if (preference == this.mVerifyAppsOverUsb) {
            writeVerifyAppsOverUsbOptions();
            return false;
        }
        if (preference == this.mStrictMode) {
            writeStrictModeVisualOptions();
            return false;
        }
        if (preference == this.mPointerLocation) {
            writePointerLocationOptions();
            return false;
        }
        if (preference == this.mShowTouches) {
            writeShowTouchesOptions();
            return false;
        }
        if (preference == this.mShowScreenUpdates) {
            writeShowUpdatesOption();
            return false;
        }
        if (preference == this.mDisableOverlays) {
            writeDisableOverlaysOption();
            return false;
        }
        if (preference == this.mShowCpuUsage) {
            writeCpuUsageOptions();
            return false;
        }
        if (preference == this.mImmediatelyDestroyActivities) {
            writeImmediatelyDestroyActivitiesOptions();
            return false;
        }
        if (preference == this.mShowAllANRs) {
            writeShowAllANRsOptions();
            return false;
        }
        if (preference == this.mForceHardwareUi) {
            writeHardwareUiOptions();
            return false;
        }
        if (preference == this.mForceMsaa) {
            writeMsaaOptions();
            return false;
        }
        if (preference == this.mShowHwScreenUpdates) {
            writeShowHwScreenUpdatesOptions();
            return false;
        }
        if (preference == this.mShowHwLayersUpdates) {
            writeShowHwLayersUpdatesOptions();
            return false;
        }
        if (preference == this.mDebugLayout) {
            writeDebugLayoutOptions();
            return false;
        }
        if (preference == this.mForceRtlLayout) {
            writeForceRtlOptions();
            return false;
        }
        if (preference == this.mWifiDisplayCertification) {
            writeWifiDisplayCertificationOptions();
            return false;
        }
        if (preference == this.mWifiVerboseLogging) {
            writeWifiVerboseLoggingOptions();
            return false;
        }
        if (preference == this.mWifiAggressiveHandover) {
            writeWifiAggressiveHandoverOptions();
            return false;
        }
        if (preference == this.mWifiAllowScansWithTraffic) {
            writeWifiAllowScansWithTrafficOptions();
            return false;
        }
        if (preference == this.mUseAwesomePlayer) {
            writeUseAwesomePlayerOptions();
            return false;
        }
        if (preference == this.mUSBAudio) {
            writeUSBAudioOptions();
            return false;
        }
        if (preference == this.mRawDump) {
            writeRawDumpOptions();
            return false;
        }
        if (preference == this.mIspParameters) {
            writeIspParametersOptions();
            return false;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private boolean showKeyguardConfirmation(Resources resources, int requestCode) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(requestCode, resources.getString(R.string.oem_unlock_enable_pin_prompt), resources.getString(R.string.oem_unlock_enable_pin_description));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ("hdcp_checking".equals(preference.getKey())) {
            SystemProperties.set("persist.sys.hdcp_checking", newValue.toString());
            updateHdcpValues();
            pokeSystemProperties();
            return true;
        }
        if (preference == this.mLogdSize) {
            writeLogdSizeOption(newValue);
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
        if (preference == this.mOpenGLTraces) {
            writeOpenGLTracesOptions(newValue);
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
        if (preference == this.mSimulateColorSpace) {
            writeSimulateColorSpace(newValue);
        } else if (preference == this.mRawProcessType) {
            writeRawProcessTypeOptions(newValue);
            return true;
        }
        return false;
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
        if (this.mEnableDialog != null) {
            this.mEnableDialog.dismiss();
            this.mEnableDialog = null;
        }
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
            if (which == -1) {
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
            return;
        }
        if (dialog == this.mEnableDialog) {
            if (which == -1) {
                this.mDialogClicked = true;
                Settings.Global.putInt(getActivity().getContentResolver(), "development_settings_enabled", 1);
                this.mLastEnabledState = true;
                setPrefsEnabledState(this.mLastEnabledState);
                return;
            }
            this.mSwitchBar.setChecked(false);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (dialog == this.mAdbDialog) {
            if (!this.mDialogClicked) {
                this.mEnableAdb.setChecked(false);
            }
            this.mAdbDialog = null;
        } else if (dialog == this.mEnableDialog) {
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
        if (!this.mDontPokeProperties) {
            new SystemPropPoker().execute(new Void[0]);
        }
    }

    static class SystemPropPoker extends AsyncTask<Void, Void, Void> {
        SystemPropPoker() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
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
            } catch (RemoteException e3) {
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

    private String getVersionString(String dev) {
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(dev));
            Throwable th = null;
            try {
                byte[] readBytes = new byte[Math.min(fileInputStream.available(), 4)];
                fileInputStream.read(readBytes);
                String str = new String(readBytes);
                if (fileInputStream == null) {
                    return str;
                }
                if (0 == 0) {
                    fileInputStream.close();
                    return str;
                }
                try {
                    fileInputStream.close();
                    return str;
                } catch (Throwable x2) {
                    th.addSuppressed(x2);
                    return str;
                }
            } catch (Throwable th2) {
                th = th2;
                if (fileInputStream != null) {
                }
                throw th;
            }
        } catch (FileNotFoundException | IOException e) {
            return "unknown";
        }
    }

    private String getDeviceVersion(String devName) throws Throwable {
        FileReader fr;
        Throwable th;
        Throwable th2;
        File inputDevice = new File("/sys/class/input");
        if (!inputDevice.isDirectory()) {
            return "unknown";
        }
        File[] list = inputDevice.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && pathname.getAbsolutePath().startsWith("/sys/class/input/input");
            }
        });
        for (File dev : list) {
            try {
                fr = new FileReader(new File(dev.getAbsolutePath() + "/name"));
                th = null;
            } catch (Exception e) {
            }
            try {
                try {
                    BufferedReader br = new BufferedReader(fr);
                    Throwable th3 = null;
                    try {
                        String name = br.readLine();
                        if (br != null) {
                            if (0 != 0) {
                                try {
                                    br.close();
                                } catch (Throwable x2) {
                                    th3.addSuppressed(x2);
                                }
                            } else {
                                br.close();
                            }
                        }
                        if (fr != null) {
                            if (0 != 0) {
                                try {
                                    fr.close();
                                } catch (Throwable x22) {
                                    th.addSuppressed(x22);
                                }
                            } else {
                                fr.close();
                            }
                        }
                        if (devName.equals(name)) {
                            return getVersionString(dev.getAbsolutePath() + "/id/version");
                        }
                    } catch (Throwable th4) {
                        if (br != null) {
                            if (th3 != null) {
                                try {
                                    br.close();
                                } catch (Throwable x23) {
                                    th3.addSuppressed(x23);
                                }
                            } else {
                                br.close();
                            }
                        }
                        throw th4;
                    }
                } catch (Throwable th5) {
                    try {
                        throw th5;
                    } catch (Throwable th6) {
                        th2 = th5;
                        th = th6;
                        if (fr != null) {
                            if (th2 != null) {
                                try {
                                    fr.close();
                                } catch (Throwable x24) {
                                    th2.addSuppressed(x24);
                                }
                            } else {
                                fr.close();
                            }
                        }
                        throw th;
                    }
                }
            } catch (Throwable th7) {
                th = th7;
                th2 = null;
                if (fr != null) {
                }
                throw th;
            }
        }
        return "unknown";
    }
}
