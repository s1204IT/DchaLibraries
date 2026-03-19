package com.android.server.power;

import android.R;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerSaveState;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.KeyValueListParser;
import android.util.Slog;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ConcurrentUtils;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.power.batterysaver.BatterySavingStats;
import com.android.server.power.batterysaver.CpuFrequencies;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class BatterySaverPolicy extends ContentObserver {
    public static final boolean DEBUG = false;
    private static final String KEY_ADJUST_BRIGHTNESS_DISABLED = "adjust_brightness_disabled";
    private static final String KEY_ADJUST_BRIGHTNESS_FACTOR = "adjust_brightness_factor";
    private static final String KEY_ANIMATION_DISABLED = "animation_disabled";
    private static final String KEY_AOD_DISABLED = "aod_disabled";
    private static final String KEY_CPU_FREQ_INTERACTIVE = "cpufreq-i";
    private static final String KEY_CPU_FREQ_NONINTERACTIVE = "cpufreq-n";
    private static final String KEY_DATASAVER_DISABLED = "datasaver_disabled";
    private static final String KEY_FIREWALL_DISABLED = "firewall_disabled";
    private static final String KEY_FORCE_ALL_APPS_STANDBY = "force_all_apps_standby";
    private static final String KEY_FORCE_BACKGROUND_CHECK = "force_background_check";
    private static final String KEY_FULLBACKUP_DEFERRED = "fullbackup_deferred";
    private static final String KEY_GPS_MODE = "gps_mode";
    private static final String KEY_KEYVALUE_DEFERRED = "keyvaluebackup_deferred";
    private static final String KEY_LAUNCH_BOOST_DISABLED = "launch_boost_disabled";
    private static final String KEY_OPTIONAL_SENSORS_DISABLED = "optional_sensors_disabled";
    private static final String KEY_SEND_TRON_LOG = "send_tron_log";
    private static final String KEY_SOUNDTRIGGER_DISABLED = "soundtrigger_disabled";
    private static final String KEY_VIBRATION_DISABLED = "vibration_disabled";
    public static final String SECURE_KEY_GPS_MODE = "batterySaverGpsMode";
    private static final String TAG = "BatterySaverPolicy";

    @GuardedBy("mLock")
    private boolean mAccessibilityEnabled;

    @GuardedBy("mLock")
    private boolean mAdjustBrightnessDisabled;

    @GuardedBy("mLock")
    private float mAdjustBrightnessFactor;

    @GuardedBy("mLock")
    private boolean mAnimationDisabled;

    @GuardedBy("mLock")
    private boolean mAodDisabled;
    private final BatterySavingStats mBatterySavingStats;
    private final ContentResolver mContentResolver;
    private final Context mContext;

    @GuardedBy("mLock")
    private boolean mDataSaverDisabled;

    @GuardedBy("mLock")
    private String mDeviceSpecificSettings;

    @GuardedBy("mLock")
    private String mDeviceSpecificSettingsSource;

    @GuardedBy("mLock")
    private String mEventLogKeys;

    @GuardedBy("mLock")
    private ArrayMap<String, String> mFilesForInteractive;

    @GuardedBy("mLock")
    private ArrayMap<String, String> mFilesForNoninteractive;

    @GuardedBy("mLock")
    private boolean mFireWallDisabled;

    @GuardedBy("mLock")
    private boolean mForceAllAppsStandby;

    @GuardedBy("mLock")
    private boolean mForceBackgroundCheck;

    @GuardedBy("mLock")
    private boolean mFullBackupDeferred;

    @GuardedBy("mLock")
    private int mGpsMode;
    private final Handler mHandler;

    @GuardedBy("mLock")
    private boolean mKeyValueBackupDeferred;

    @GuardedBy("mLock")
    private boolean mLaunchBoostDisabled;

    @GuardedBy("mLock")
    private final List<BatterySaverPolicyListener> mListeners;
    private final Object mLock;

    @GuardedBy("mLock")
    private boolean mOptionalSensorsDisabled;

    @GuardedBy("mLock")
    private boolean mSendTronLog;

    @GuardedBy("mLock")
    private String mSettings;

    @GuardedBy("mLock")
    private boolean mSoundTriggerDisabled;

    @GuardedBy("mLock")
    private boolean mVibrationDisabledConfig;

    @GuardedBy("mLock")
    private boolean mVibrationDisabledEffective;

    public interface BatterySaverPolicyListener {
        void onBatterySaverPolicyChanged(BatterySaverPolicy batterySaverPolicy);
    }

    public BatterySaverPolicy(Object obj, Context context, BatterySavingStats batterySavingStats) {
        super(BackgroundThread.getHandler());
        this.mListeners = new ArrayList();
        this.mLock = obj;
        this.mHandler = BackgroundThread.getHandler();
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mBatterySavingStats = batterySavingStats;
    }

    public void systemReady() {
        ConcurrentUtils.wtfIfLockHeld(TAG, this.mLock);
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("battery_saver_constants"), false, this);
        this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("battery_saver_device_specific_constants"), false, this);
        AccessibilityManager accessibilityManager = (AccessibilityManager) this.mContext.getSystemService(AccessibilityManager.class);
        accessibilityManager.addAccessibilityStateChangeListener(new AccessibilityManager.AccessibilityStateChangeListener() {
            @Override
            public final void onAccessibilityStateChanged(boolean z) {
                BatterySaverPolicy.lambda$systemReady$0(this.f$0, z);
            }
        });
        boolean zIsEnabled = accessibilityManager.isEnabled();
        synchronized (this.mLock) {
            this.mAccessibilityEnabled = zIsEnabled;
        }
        onChange(true, null);
    }

    public static void lambda$systemReady$0(BatterySaverPolicy batterySaverPolicy, boolean z) {
        synchronized (batterySaverPolicy.mLock) {
            batterySaverPolicy.mAccessibilityEnabled = z;
        }
        batterySaverPolicy.refreshSettings();
    }

    public void addListener(BatterySaverPolicyListener batterySaverPolicyListener) {
        synchronized (this.mLock) {
            this.mListeners.add(batterySaverPolicyListener);
        }
    }

    @VisibleForTesting
    String getGlobalSetting(String str) {
        return Settings.Global.getString(this.mContentResolver, str);
    }

    @VisibleForTesting
    int getDeviceSpecificConfigResId() {
        return R.string.accessibility_system_action_dpad_right_label;
    }

    @Override
    public void onChange(boolean z, Uri uri) {
        refreshSettings();
    }

    private void refreshSettings() {
        final BatterySaverPolicyListener[] batterySaverPolicyListenerArr;
        synchronized (this.mLock) {
            String globalSetting = getGlobalSetting("battery_saver_constants");
            String globalSetting2 = getGlobalSetting("battery_saver_device_specific_constants");
            this.mDeviceSpecificSettingsSource = "battery_saver_device_specific_constants";
            if (TextUtils.isEmpty(globalSetting2) || "null".equals(globalSetting2)) {
                globalSetting2 = this.mContext.getString(getDeviceSpecificConfigResId());
                this.mDeviceSpecificSettingsSource = "(overlay)";
            }
            updateConstantsLocked(globalSetting, globalSetting2);
            batterySaverPolicyListenerArr = (BatterySaverPolicyListener[]) this.mListeners.toArray(new BatterySaverPolicyListener[this.mListeners.size()]);
        }
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                BatterySaverPolicy.lambda$refreshSettings$1(this.f$0, batterySaverPolicyListenerArr);
            }
        });
    }

    public static void lambda$refreshSettings$1(BatterySaverPolicy batterySaverPolicy, BatterySaverPolicyListener[] batterySaverPolicyListenerArr) {
        for (BatterySaverPolicyListener batterySaverPolicyListener : batterySaverPolicyListenerArr) {
            batterySaverPolicyListener.onBatterySaverPolicyChanged(batterySaverPolicy);
        }
    }

    @GuardedBy("mLock")
    @VisibleForTesting
    void updateConstantsLocked(String str, String str2) {
        this.mSettings = str;
        this.mDeviceSpecificSettings = str2;
        KeyValueListParser keyValueListParser = new KeyValueListParser(',');
        try {
            keyValueListParser.setString(str);
        } catch (IllegalArgumentException e) {
            Slog.wtf(TAG, "Bad battery saver constants: " + str);
        }
        this.mVibrationDisabledConfig = keyValueListParser.getBoolean(KEY_VIBRATION_DISABLED, true);
        this.mAnimationDisabled = keyValueListParser.getBoolean(KEY_ANIMATION_DISABLED, false);
        this.mSoundTriggerDisabled = keyValueListParser.getBoolean(KEY_SOUNDTRIGGER_DISABLED, true);
        this.mFullBackupDeferred = keyValueListParser.getBoolean(KEY_FULLBACKUP_DEFERRED, true);
        this.mKeyValueBackupDeferred = keyValueListParser.getBoolean(KEY_KEYVALUE_DEFERRED, true);
        this.mFireWallDisabled = keyValueListParser.getBoolean(KEY_FIREWALL_DISABLED, false);
        this.mAdjustBrightnessDisabled = keyValueListParser.getBoolean(KEY_ADJUST_BRIGHTNESS_DISABLED, true);
        this.mAdjustBrightnessFactor = keyValueListParser.getFloat(KEY_ADJUST_BRIGHTNESS_FACTOR, 0.5f);
        this.mDataSaverDisabled = keyValueListParser.getBoolean(KEY_DATASAVER_DISABLED, true);
        this.mLaunchBoostDisabled = keyValueListParser.getBoolean(KEY_LAUNCH_BOOST_DISABLED, true);
        this.mForceAllAppsStandby = keyValueListParser.getBoolean(KEY_FORCE_ALL_APPS_STANDBY, true);
        this.mForceBackgroundCheck = keyValueListParser.getBoolean(KEY_FORCE_BACKGROUND_CHECK, true);
        this.mOptionalSensorsDisabled = keyValueListParser.getBoolean(KEY_OPTIONAL_SENSORS_DISABLED, true);
        this.mAodDisabled = keyValueListParser.getBoolean(KEY_AOD_DISABLED, true);
        this.mSendTronLog = keyValueListParser.getBoolean(KEY_SEND_TRON_LOG, false);
        this.mGpsMode = keyValueListParser.getInt(KEY_GPS_MODE, Settings.Secure.getInt(this.mContentResolver, SECURE_KEY_GPS_MODE, 2));
        try {
            keyValueListParser.setString(str2);
        } catch (IllegalArgumentException e2) {
            Slog.wtf(TAG, "Bad device specific battery saver constants: " + str2);
        }
        this.mFilesForInteractive = new CpuFrequencies().parseString(keyValueListParser.getString(KEY_CPU_FREQ_INTERACTIVE, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)).toSysFileMap();
        this.mFilesForNoninteractive = new CpuFrequencies().parseString(keyValueListParser.getString(KEY_CPU_FREQ_NONINTERACTIVE, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)).toSysFileMap();
        this.mVibrationDisabledEffective = this.mVibrationDisabledConfig && !this.mAccessibilityEnabled;
        StringBuilder sb = new StringBuilder();
        if (this.mForceAllAppsStandby) {
            sb.append("A");
        }
        if (this.mForceBackgroundCheck) {
            sb.append("B");
        }
        if (this.mVibrationDisabledEffective) {
            sb.append("v");
        }
        if (this.mAnimationDisabled) {
            sb.append("a");
        }
        if (this.mSoundTriggerDisabled) {
            sb.append("s");
        }
        if (this.mFullBackupDeferred) {
            sb.append("F");
        }
        if (this.mKeyValueBackupDeferred) {
            sb.append("K");
        }
        if (!this.mFireWallDisabled) {
            sb.append("f");
        }
        if (!this.mDataSaverDisabled) {
            sb.append("d");
        }
        if (!this.mAdjustBrightnessDisabled) {
            sb.append("b");
        }
        if (this.mLaunchBoostDisabled) {
            sb.append("l");
        }
        if (this.mOptionalSensorsDisabled) {
            sb.append("S");
        }
        if (this.mAodDisabled) {
            sb.append("o");
        }
        if (this.mSendTronLog) {
            sb.append("t");
        }
        sb.append(this.mGpsMode);
        this.mEventLogKeys = sb.toString();
        this.mBatterySavingStats.setSendTronLog(this.mSendTronLog);
    }

    public PowerSaveState getBatterySaverPolicy(int i, boolean z) {
        synchronized (this.mLock) {
            PowerSaveState.Builder globalBatterySaverEnabled = new PowerSaveState.Builder().setGlobalBatterySaverEnabled(z);
            if (!z) {
                return globalBatterySaverEnabled.setBatterySaverEnabled(z).build();
            }
            switch (i) {
                case 1:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(z).setGpsMode(this.mGpsMode).build();
                case 2:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mVibrationDisabledEffective).build();
                case 3:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mAnimationDisabled).build();
                case 4:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mFullBackupDeferred).build();
                case 5:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mKeyValueBackupDeferred).build();
                case 6:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(!this.mFireWallDisabled).build();
                case 7:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(!this.mAdjustBrightnessDisabled).setBrightnessFactor(this.mAdjustBrightnessFactor).build();
                case 8:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mSoundTriggerDisabled).build();
                case 9:
                default:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(z).build();
                case 10:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(!this.mDataSaverDisabled).build();
                case 11:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mForceAllAppsStandby).build();
                case 12:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mForceBackgroundCheck).build();
                case 13:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mOptionalSensorsDisabled).build();
                case 14:
                    return globalBatterySaverEnabled.setBatterySaverEnabled(this.mAodDisabled).build();
            }
        }
    }

    public int getGpsMode() {
        int i;
        synchronized (this.mLock) {
            i = this.mGpsMode;
        }
        return i;
    }

    public ArrayMap<String, String> getFileValues(boolean z) {
        ArrayMap<String, String> arrayMap;
        synchronized (this.mLock) {
            try {
                arrayMap = z ? this.mFilesForInteractive : this.mFilesForNoninteractive;
            } catch (Throwable th) {
                throw th;
            }
        }
        return arrayMap;
    }

    public boolean isLaunchBoostDisabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mLaunchBoostDisabled;
        }
        return z;
    }

    public String toEventLogString() {
        String str;
        synchronized (this.mLock) {
            str = this.mEventLogKeys;
        }
        return str;
    }

    public void dump(PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println();
            this.mBatterySavingStats.dump(printWriter, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            printWriter.println();
            printWriter.println("Battery saver policy (*NOTE* they only apply when battery saver is ON):");
            printWriter.println("  Settings: battery_saver_constants");
            printWriter.println("    value: " + this.mSettings);
            printWriter.println("  Settings: " + this.mDeviceSpecificSettingsSource);
            printWriter.println("    value: " + this.mDeviceSpecificSettings);
            printWriter.println();
            printWriter.println("  mAccessibilityEnabled=" + this.mAccessibilityEnabled);
            printWriter.println("  vibration_disabled:config=" + this.mVibrationDisabledConfig);
            printWriter.println("  vibration_disabled:effective=" + this.mVibrationDisabledEffective);
            printWriter.println("  animation_disabled=" + this.mAnimationDisabled);
            printWriter.println("  fullbackup_deferred=" + this.mFullBackupDeferred);
            printWriter.println("  keyvaluebackup_deferred=" + this.mKeyValueBackupDeferred);
            printWriter.println("  firewall_disabled=" + this.mFireWallDisabled);
            printWriter.println("  datasaver_disabled=" + this.mDataSaverDisabled);
            printWriter.println("  launch_boost_disabled=" + this.mLaunchBoostDisabled);
            printWriter.println("  adjust_brightness_disabled=" + this.mAdjustBrightnessDisabled);
            printWriter.println("  adjust_brightness_factor=" + this.mAdjustBrightnessFactor);
            printWriter.println("  gps_mode=" + this.mGpsMode);
            printWriter.println("  force_all_apps_standby=" + this.mForceAllAppsStandby);
            printWriter.println("  force_background_check=" + this.mForceBackgroundCheck);
            printWriter.println("  optional_sensors_disabled=" + this.mOptionalSensorsDisabled);
            printWriter.println("  aod_disabled=" + this.mAodDisabled);
            printWriter.println("  send_tron_log=" + this.mSendTronLog);
            printWriter.println();
            printWriter.print("  Interactive File values:\n");
            dumpMap(printWriter, "    ", this.mFilesForInteractive);
            printWriter.println();
            printWriter.print("  Noninteractive File values:\n");
            dumpMap(printWriter, "    ", this.mFilesForNoninteractive);
        }
    }

    private void dumpMap(PrintWriter printWriter, String str, ArrayMap<String, String> arrayMap) {
        if (arrayMap == null) {
            return;
        }
        int size = arrayMap.size();
        for (int i = 0; i < size; i++) {
            printWriter.print(str);
            printWriter.print(arrayMap.keyAt(i));
            printWriter.print(": '");
            printWriter.print(arrayMap.valueAt(i));
            printWriter.println("'");
        }
    }

    @VisibleForTesting
    public void setAccessibilityEnabledForTest(boolean z) {
        synchronized (this.mLock) {
            this.mAccessibilityEnabled = z;
        }
    }
}
