package com.android.systemui.power;

import android.R;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HardwarePropertiesManager;
import android.os.IBinder;
import android.os.IThermalEventListener;
import android.os.IThermalService;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.Temperature;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.utils.ThreadUtils;
import com.android.systemui.Dependency;
import com.android.systemui.SystemUI;
import com.android.systemui.power.PowerUI;
import com.android.systemui.statusbar.phone.StatusBar;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Arrays;

public class PowerUI extends SystemUI {
    static final boolean DEBUG = Log.isLoggable("PowerUI", 3);
    private static final long SIX_HOURS_MILLIS = Duration.ofHours(6).toMillis();
    private EnhancedEstimates mEnhancedEstimates;
    private HardwarePropertiesManager mHardwarePropertiesManager;
    private int mLowBatteryAlertCloseLevel;
    private boolean mLowWarningShownThisChargeCycle;
    private long mNextLogTime;
    private int mNumTemps;
    private PowerManager mPowerManager;
    private boolean mSevereWarningShownThisChargeCycle;
    private IThermalService mThermalService;
    private float mThresholdTemp;
    private WarningsUI mWarnings;
    private final Handler mHandler = new Handler();

    @VisibleForTesting
    final Receiver mReceiver = new Receiver();
    private final Configuration mLastConfiguration = new Configuration();
    private long mTimeRemaining = Long.MAX_VALUE;
    private int mPlugType = 0;
    private int mInvalidCharger = 0;
    private final int[] mLowBatteryReminderLevels = new int[2];
    private long mScreenOffTime = -1;
    private float[] mRecentTemps = new float[125];

    @VisibleForTesting
    int mBatteryLevel = 100;

    @VisibleForTesting
    int mBatteryStatus = 1;
    private final Runnable mUpdateTempCallback = new Runnable() {
        @Override
        public final void run() {
            this.f$0.updateTemperatureWarning();
        }
    };

    public interface WarningsUI {
        void dismissHighTemperatureWarning();

        void dismissInvalidChargerWarning();

        void dismissLowBatteryWarning();

        void dump(PrintWriter printWriter);

        boolean isInvalidChargerWarningShowing();

        void showHighTemperatureWarning();

        void showInvalidChargerWarning();

        void showLowBatteryWarning(boolean z);

        void showThermalShutdownWarning();

        void update(int i, int i2, long j);

        void updateEstimate(Estimate estimate);

        void updateLowBatteryWarning();

        void updateThresholds(long j, long j2);

        void userSwitched();
    }

    @Override
    public void start() {
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mHardwarePropertiesManager = (HardwarePropertiesManager) this.mContext.getSystemService("hardware_properties");
        this.mScreenOffTime = this.mPowerManager.isScreenOn() ? -1L : SystemClock.elapsedRealtime();
        this.mWarnings = (WarningsUI) Dependency.get(WarningsUI.class);
        this.mEnhancedEstimates = (EnhancedEstimates) Dependency.get(EnhancedEstimates.class);
        this.mLastConfiguration.setTo(this.mContext.getResources().getConfiguration());
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("low_power_trigger_level"), false, new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z) {
                PowerUI.this.updateBatteryWarningLevels();
            }
        }, -1);
        updateBatteryWarningLevels();
        this.mReceiver.init();
        showThermalShutdownDialog();
        initTemperatureWarning();
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        if ((this.mLastConfiguration.updateFrom(configuration) & 3) != 0) {
            this.mHandler.post(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.initTemperatureWarning();
                }
            });
        }
    }

    void updateBatteryWarningLevels() {
        int integer = this.mContext.getResources().getInteger(R.integer.config_autoBrightnessLightSensorRate);
        int integer2 = this.mContext.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterWatcherBatchSize);
        if (integer2 < integer) {
            integer2 = integer;
        }
        this.mLowBatteryReminderLevels[0] = integer2;
        this.mLowBatteryReminderLevels[1] = integer;
        this.mLowBatteryAlertCloseLevel = this.mLowBatteryReminderLevels[0] + this.mContext.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterAutoSamplerBatchSize);
    }

    private int findBatteryLevelBucket(int i) {
        if (i >= this.mLowBatteryAlertCloseLevel) {
            return 1;
        }
        if (i > this.mLowBatteryReminderLevels[0]) {
            return 0;
        }
        for (int length = this.mLowBatteryReminderLevels.length - 1; length >= 0; length--) {
            if (i <= this.mLowBatteryReminderLevels[length]) {
                return (-1) - length;
            }
        }
        throw new RuntimeException("not possible!");
    }

    @VisibleForTesting
    final class Receiver extends BroadcastReceiver {
        Receiver() {
        }

        public void init() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
            intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            PowerUI.this.mContext.registerReceiver(this, intentFilter, null, PowerUI.this.mHandler);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(action)) {
                ThreadUtils.postOnBackgroundThread(new Runnable() {
                    @Override
                    public final void run() {
                        PowerUI.Receiver.lambda$onReceive$0(this.f$0);
                    }
                });
                return;
            }
            if (!"android.intent.action.BATTERY_CHANGED".equals(action)) {
                if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    PowerUI.this.mScreenOffTime = SystemClock.elapsedRealtime();
                    return;
                }
                if ("android.intent.action.SCREEN_ON".equals(action)) {
                    PowerUI.this.mScreenOffTime = -1L;
                    return;
                }
                if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    PowerUI.this.mWarnings.userSwitched();
                    return;
                }
                Slog.w("PowerUI", "unknown intent: " + intent);
                return;
            }
            int i = PowerUI.this.mBatteryLevel;
            PowerUI.this.mBatteryLevel = intent.getIntExtra("level", 100);
            int i2 = PowerUI.this.mBatteryStatus;
            PowerUI.this.mBatteryStatus = intent.getIntExtra("status", 1);
            int i3 = PowerUI.this.mPlugType;
            PowerUI.this.mPlugType = intent.getIntExtra("plugged", 1);
            int i4 = PowerUI.this.mInvalidCharger;
            PowerUI.this.mInvalidCharger = intent.getIntExtra("invalid_charger", 0);
            final boolean z = PowerUI.this.mPlugType != 0;
            final boolean z2 = i3 != 0;
            final int iFindBatteryLevelBucket = PowerUI.this.findBatteryLevelBucket(i);
            final int iFindBatteryLevelBucket2 = PowerUI.this.findBatteryLevelBucket(PowerUI.this.mBatteryLevel);
            if (PowerUI.DEBUG) {
                Slog.d("PowerUI", "buckets   ....." + PowerUI.this.mLowBatteryAlertCloseLevel + " .. " + PowerUI.this.mLowBatteryReminderLevels[0] + " .. " + PowerUI.this.mLowBatteryReminderLevels[1]);
                StringBuilder sb = new StringBuilder();
                sb.append("level          ");
                sb.append(i);
                sb.append(" --> ");
                sb.append(PowerUI.this.mBatteryLevel);
                Slog.d("PowerUI", sb.toString());
                Slog.d("PowerUI", "status         " + i2 + " --> " + PowerUI.this.mBatteryStatus);
                Slog.d("PowerUI", "plugType       " + i3 + " --> " + PowerUI.this.mPlugType);
                Slog.d("PowerUI", "invalidCharger " + i4 + " --> " + PowerUI.this.mInvalidCharger);
                Slog.d("PowerUI", "bucket         " + iFindBatteryLevelBucket + " --> " + iFindBatteryLevelBucket2);
                Slog.d("PowerUI", "plugged        " + z2 + " --> " + z);
            }
            PowerUI.this.mWarnings.update(PowerUI.this.mBatteryLevel, iFindBatteryLevelBucket2, PowerUI.this.mScreenOffTime);
            if (i4 == 0 && PowerUI.this.mInvalidCharger != 0) {
                Slog.d("PowerUI", "showing invalid charger warning");
                PowerUI.this.mWarnings.showInvalidChargerWarning();
                return;
            }
            if (i4 == 0 || PowerUI.this.mInvalidCharger != 0) {
                if (PowerUI.this.mWarnings.isInvalidChargerWarningShowing()) {
                    return;
                }
            } else {
                PowerUI.this.mWarnings.dismissInvalidChargerWarning();
            }
            ThreadUtils.postOnBackgroundThread(new Runnable() {
                @Override
                public final void run() {
                    PowerUI.this.maybeShowBatteryWarning(z, z2, iFindBatteryLevelBucket, iFindBatteryLevelBucket2);
                }
            });
        }

        public static void lambda$onReceive$0(Receiver receiver) {
            if (PowerUI.this.mPowerManager.isPowerSaveMode()) {
                PowerUI.this.mWarnings.dismissLowBatteryWarning();
            }
        }
    }

    protected void maybeShowBatteryWarning(boolean z, boolean z2, int i, int i2) {
        Estimate estimate;
        boolean zIsPowerSaveMode = this.mPowerManager.isPowerSaveMode();
        boolean z3 = i2 != i || z2;
        boolean zIsHybridNotificationEnabled = this.mEnhancedEstimates.isHybridNotificationEnabled();
        if (zIsHybridNotificationEnabled && (estimate = this.mEnhancedEstimates.getEstimate()) != null) {
            this.mTimeRemaining = estimate.estimateMillis;
            this.mWarnings.updateEstimate(estimate);
            this.mWarnings.updateThresholds(this.mEnhancedEstimates.getLowWarningThreshold(), this.mEnhancedEstimates.getSevereWarningThreshold());
            if (this.mBatteryLevel >= 45 && this.mTimeRemaining > SIX_HOURS_MILLIS) {
                this.mLowWarningShownThisChargeCycle = false;
                this.mSevereWarningShownThisChargeCycle = false;
            }
        }
        if (shouldShowLowBatteryWarning(z, z2, i, i2, this.mTimeRemaining, zIsPowerSaveMode, this.mBatteryStatus)) {
            this.mWarnings.showLowBatteryWarning(z3);
            if (zIsHybridNotificationEnabled) {
                if (this.mTimeRemaining < this.mEnhancedEstimates.getSevereWarningThreshold() || this.mBatteryLevel < this.mLowBatteryReminderLevels[1]) {
                    this.mSevereWarningShownThisChargeCycle = true;
                    return;
                } else {
                    this.mLowWarningShownThisChargeCycle = true;
                    return;
                }
            }
            return;
        }
        if (shouldDismissLowBatteryWarning(z, i, i2, this.mTimeRemaining, zIsPowerSaveMode)) {
            this.mWarnings.dismissLowBatteryWarning();
        } else {
            this.mWarnings.updateLowBatteryWarning();
        }
    }

    @VisibleForTesting
    boolean shouldShowLowBatteryWarning(boolean z, boolean z2, int i, int i2, long j, boolean z3, int i3) {
        if (this.mEnhancedEstimates.isHybridNotificationEnabled()) {
            return isEnhancedTrigger(z, j, z3, i3);
        }
        return (z || z3 || (i2 >= i && !z2) || i2 >= 0 || i3 == 1) ? false : true;
    }

    @VisibleForTesting
    boolean shouldDismissLowBatteryWarning(boolean z, int i, int i2, long j, boolean z2) {
        boolean z3 = this.mEnhancedEstimates.isHybridNotificationEnabled() && j > this.mEnhancedEstimates.getLowWarningThreshold();
        boolean z4 = i2 > i && i2 > 0;
        if (!z2 && !z) {
            if (!z4) {
                return false;
            }
            if (this.mEnhancedEstimates.isHybridNotificationEnabled() && !z3) {
                return false;
            }
        }
        return true;
    }

    private boolean isEnhancedTrigger(boolean z, long j, boolean z2, int i) {
        if (z || z2 || i == 1) {
            return false;
        }
        return (!this.mLowWarningShownThisChargeCycle && ((j > this.mEnhancedEstimates.getLowWarningThreshold() ? 1 : (j == this.mEnhancedEstimates.getLowWarningThreshold() ? 0 : -1)) < 0 || this.mBatteryLevel <= this.mLowBatteryReminderLevels[0])) || (!this.mSevereWarningShownThisChargeCycle && ((j > this.mEnhancedEstimates.getSevereWarningThreshold() ? 1 : (j == this.mEnhancedEstimates.getSevereWarningThreshold() ? 0 : -1)) < 0 || this.mBatteryLevel <= this.mLowBatteryReminderLevels[1]));
    }

    private void initTemperatureWarning() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (Settings.Global.getInt(contentResolver, "show_temperature_warning", this.mContext.getResources().getInteger(com.android.systemui.R.integer.config_showTemperatureWarning)) == 0) {
            return;
        }
        this.mThresholdTemp = Settings.Global.getFloat(contentResolver, "warning_temperature", r1.getInteger(com.android.systemui.R.integer.config_warningTemperature));
        if (this.mThresholdTemp < 0.0f) {
            float[] deviceTemperatures = this.mHardwarePropertiesManager.getDeviceTemperatures(3, 2);
            if (deviceTemperatures == null || deviceTemperatures.length == 0 || deviceTemperatures[0] == -3.4028235E38f) {
                return;
            } else {
                this.mThresholdTemp = deviceTemperatures[0] - r1.getInteger(com.android.systemui.R.integer.config_warningTemperatureTolerance);
            }
        }
        if (this.mThermalService == null) {
            IBinder service = ServiceManager.getService("thermalservice");
            if (service != null) {
                this.mThermalService = IThermalService.Stub.asInterface(service);
                try {
                    this.mThermalService.registerThermalEventListener(new ThermalEventListener());
                } catch (RemoteException e) {
                }
            } else {
                Slog.w("PowerUI", "cannot find thermalservice, no throttling push notifications");
            }
        }
        setNextLogTime();
        this.mHandler.removeCallbacks(this.mUpdateTempCallback);
        updateTemperatureWarning();
    }

    private void showThermalShutdownDialog() {
        if (this.mPowerManager.getLastShutdownReason() == 4) {
            this.mWarnings.showThermalShutdownWarning();
        }
    }

    @VisibleForTesting
    protected void updateTemperatureWarning() {
        float[] deviceTemperatures = this.mHardwarePropertiesManager.getDeviceTemperatures(3, 0);
        if (deviceTemperatures.length != 0) {
            float f = deviceTemperatures[0];
            float[] fArr = this.mRecentTemps;
            int i = this.mNumTemps;
            this.mNumTemps = i + 1;
            fArr[i] = f;
            StatusBar statusBar = (StatusBar) getComponent(StatusBar.class);
            if (statusBar != null && !statusBar.isDeviceInVrMode() && f >= this.mThresholdTemp) {
                logAtTemperatureThreshold(f);
                this.mWarnings.showHighTemperatureWarning();
            } else {
                this.mWarnings.dismissHighTemperatureWarning();
            }
        }
        logTemperatureStats();
        this.mHandler.postDelayed(this.mUpdateTempCallback, 30000L);
    }

    private void logAtTemperatureThreshold(float f) {
        StringBuilder sb = new StringBuilder();
        sb.append("currentTemp=");
        sb.append(f);
        sb.append(",thresholdTemp=");
        sb.append(this.mThresholdTemp);
        sb.append(",batteryStatus=");
        sb.append(this.mBatteryStatus);
        sb.append(",recentTemps=");
        for (int i = 0; i < this.mNumTemps; i++) {
            sb.append(this.mRecentTemps[i]);
            sb.append(',');
        }
        Slog.i("PowerUI", sb.toString());
    }

    private void logTemperatureStats() {
        if (this.mNextLogTime > System.currentTimeMillis() && this.mNumTemps != 125) {
            return;
        }
        if (this.mNumTemps > 0) {
            float f = this.mRecentTemps[0];
            float f2 = this.mRecentTemps[0];
            float f3 = this.mRecentTemps[0];
            for (int i = 1; i < this.mNumTemps; i++) {
                float f4 = this.mRecentTemps[i];
                f += f4;
                if (f4 > f3) {
                    f3 = f4;
                }
                if (f4 < f2) {
                    f2 = f4;
                }
            }
            float f5 = f / this.mNumTemps;
            Slog.i("PowerUI", "avg=" + f5 + ",min=" + f2 + ",max=" + f3);
            MetricsLogger.histogram(this.mContext, "device_skin_temp_avg", (int) f5);
            MetricsLogger.histogram(this.mContext, "device_skin_temp_min", (int) f2);
            MetricsLogger.histogram(this.mContext, "device_skin_temp_max", (int) f3);
        }
        setNextLogTime();
        this.mNumTemps = 0;
    }

    private void setNextLogTime() {
        this.mNextLogTime = System.currentTimeMillis() + 3600000;
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("mLowBatteryAlertCloseLevel=");
        printWriter.println(this.mLowBatteryAlertCloseLevel);
        printWriter.print("mLowBatteryReminderLevels=");
        printWriter.println(Arrays.toString(this.mLowBatteryReminderLevels));
        printWriter.print("mBatteryLevel=");
        printWriter.println(Integer.toString(this.mBatteryLevel));
        printWriter.print("mBatteryStatus=");
        printWriter.println(Integer.toString(this.mBatteryStatus));
        printWriter.print("mPlugType=");
        printWriter.println(Integer.toString(this.mPlugType));
        printWriter.print("mInvalidCharger=");
        printWriter.println(Integer.toString(this.mInvalidCharger));
        printWriter.print("mScreenOffTime=");
        printWriter.print(this.mScreenOffTime);
        if (this.mScreenOffTime >= 0) {
            printWriter.print(" (");
            printWriter.print(SystemClock.elapsedRealtime() - this.mScreenOffTime);
            printWriter.print(" ago)");
        }
        printWriter.println();
        printWriter.print("soundTimeout=");
        printWriter.println(Settings.Global.getInt(this.mContext.getContentResolver(), "low_battery_sound_timeout", 0));
        printWriter.print("bucket: ");
        printWriter.println(Integer.toString(findBatteryLevelBucket(this.mBatteryLevel)));
        printWriter.print("mThresholdTemp=");
        printWriter.println(Float.toString(this.mThresholdTemp));
        printWriter.print("mNextLogTime=");
        printWriter.println(Long.toString(this.mNextLogTime));
        this.mWarnings.dump(printWriter);
    }

    private final class ThermalEventListener extends IThermalEventListener.Stub {
        private ThermalEventListener() {
        }

        public void notifyThrottling(boolean z, Temperature temperature) {
            PowerUI.this.mHandler.removeCallbacks(PowerUI.this.mUpdateTempCallback);
            PowerUI.this.updateTemperatureWarning();
        }
    }
}
