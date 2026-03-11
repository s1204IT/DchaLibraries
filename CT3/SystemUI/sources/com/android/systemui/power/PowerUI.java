package com.android.systemui.power;

import android.R;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class PowerUI extends SystemUI {
    static final boolean DEBUG = Log.isLoggable("PowerUI", 3);
    private int mLowBatteryAlertCloseLevel;
    private PowerManager mPowerManager;
    private WarningsUI mWarnings;
    private final Handler mHandler = new Handler();
    private final Receiver mReceiver = new Receiver(this, null);
    private int mBatteryLevel = 100;
    private int mBatteryStatus = 1;
    private int mPlugType = 0;
    private int mInvalidCharger = 0;
    private final int[] mLowBatteryReminderLevels = new int[2];
    private long mScreenOffTime = -1;

    public interface WarningsUI {
        void dismissInvalidChargerWarning();

        void dismissLowBatteryWarning();

        void dump(PrintWriter printWriter);

        boolean isInvalidChargerWarningShowing();

        void showInvalidChargerWarning();

        void showLowBatteryWarning(boolean z);

        void update(int i, int i2, long j);

        void updateLowBatteryWarning();

        void userSwitched();
    }

    @Override
    public void start() {
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mScreenOffTime = this.mPowerManager.isScreenOn() ? -1L : SystemClock.elapsedRealtime();
        this.mWarnings = new PowerNotificationWarnings(this.mContext, (PhoneStatusBar) getComponent(PhoneStatusBar.class));
        ContentObserver obs = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                PowerUI.this.updateBatteryWarningLevels();
            }
        };
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor("low_power_trigger_level"), false, obs, -1);
        updateBatteryWarningLevels();
        this.mReceiver.init();
    }

    void updateBatteryWarningLevels() {
        int critLevel = this.mContext.getResources().getInteger(R.integer.config_datause_threshold_bytes);
        ContentResolver resolver = this.mContext.getContentResolver();
        int defWarnLevel = this.mContext.getResources().getInteger(R.integer.config_debugSystemServerPssThresholdBytes);
        int warnLevel = Settings.Global.getInt(resolver, "low_power_trigger_level", defWarnLevel);
        if (warnLevel == 0) {
            warnLevel = defWarnLevel;
        }
        if (warnLevel < critLevel) {
            warnLevel = critLevel;
        }
        this.mLowBatteryReminderLevels[0] = warnLevel;
        this.mLowBatteryReminderLevels[1] = critLevel;
        this.mLowBatteryAlertCloseLevel = this.mLowBatteryReminderLevels[0] + this.mContext.getResources().getInteger(R.integer.config_defaultActionModeHideDurationMillis);
    }

    public int findBatteryLevelBucket(int level) {
        if (level >= this.mLowBatteryAlertCloseLevel) {
            return 1;
        }
        if (level > this.mLowBatteryReminderLevels[0]) {
            return 0;
        }
        int N = this.mLowBatteryReminderLevels.length;
        for (int i = N - 1; i >= 0; i--) {
            if (level <= this.mLowBatteryReminderLevels[i]) {
                return (-1) - i;
            }
        }
        throw new RuntimeException("not possible!");
    }

    private final class Receiver extends BroadcastReceiver {
        Receiver(PowerUI this$0, Receiver receiver) {
            this();
        }

        private Receiver() {
        }

        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.BATTERY_CHANGED");
            filter.addAction("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGING");
            filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
            PowerUI.this.mContext.registerReceiver(this, filter, null, PowerUI.this.mHandler);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals("android.intent.action.BATTERY_CHANGED")) {
                if ("android.intent.action.SCREEN_OFF".equals(action)) {
                    PowerUI.this.mScreenOffTime = SystemClock.elapsedRealtime();
                    return;
                }
                if ("android.intent.action.SCREEN_ON".equals(action)) {
                    PowerUI.this.mScreenOffTime = -1L;
                    return;
                } else if ("android.intent.action.USER_SWITCHED".equals(action)) {
                    PowerUI.this.mWarnings.userSwitched();
                    return;
                } else {
                    Slog.w("PowerUI", "unknown intent: " + intent);
                    return;
                }
            }
            int oldBatteryLevel = PowerUI.this.mBatteryLevel;
            PowerUI.this.mBatteryLevel = intent.getIntExtra("level", 100);
            int oldBatteryStatus = PowerUI.this.mBatteryStatus;
            PowerUI.this.mBatteryStatus = intent.getIntExtra("status", 1);
            int oldPlugType = PowerUI.this.mPlugType;
            PowerUI.this.mPlugType = intent.getIntExtra("plugged", 1);
            int oldInvalidCharger = PowerUI.this.mInvalidCharger;
            PowerUI.this.mInvalidCharger = intent.getIntExtra("invalid_charger", 0);
            boolean plugged = PowerUI.this.mPlugType != 0;
            boolean oldPlugged = oldPlugType != 0;
            int oldBucket = PowerUI.this.findBatteryLevelBucket(oldBatteryLevel);
            int bucket = PowerUI.this.findBatteryLevelBucket(PowerUI.this.mBatteryLevel);
            if (PowerUI.DEBUG) {
                Slog.d("PowerUI", "buckets   ....." + PowerUI.this.mLowBatteryAlertCloseLevel + " .. " + PowerUI.this.mLowBatteryReminderLevels[0] + " .. " + PowerUI.this.mLowBatteryReminderLevels[1]);
                Slog.d("PowerUI", "level          " + oldBatteryLevel + " --> " + PowerUI.this.mBatteryLevel);
                Slog.d("PowerUI", "status         " + oldBatteryStatus + " --> " + PowerUI.this.mBatteryStatus);
                Slog.d("PowerUI", "plugType       " + oldPlugType + " --> " + PowerUI.this.mPlugType);
                Slog.d("PowerUI", "invalidCharger " + oldInvalidCharger + " --> " + PowerUI.this.mInvalidCharger);
                Slog.d("PowerUI", "bucket         " + oldBucket + " --> " + bucket);
                Slog.d("PowerUI", "plugged        " + oldPlugged + " --> " + plugged);
            }
            PowerUI.this.mWarnings.update(PowerUI.this.mBatteryLevel, bucket, PowerUI.this.mScreenOffTime);
            if (oldInvalidCharger == 0 && PowerUI.this.mInvalidCharger != 0) {
                Slog.d("PowerUI", "showing invalid charger warning");
                PowerUI.this.mWarnings.showInvalidChargerWarning();
                return;
            }
            if (oldInvalidCharger != 0 && PowerUI.this.mInvalidCharger == 0) {
                PowerUI.this.mWarnings.dismissInvalidChargerWarning();
            } else if (PowerUI.this.mWarnings.isInvalidChargerWarningShowing()) {
                return;
            }
            boolean isPowerSaver = PowerUI.this.mPowerManager.isPowerSaveMode();
            if (!plugged && !isPowerSaver && ((bucket < oldBucket || oldPlugged) && PowerUI.this.mBatteryStatus != 1 && bucket < 0)) {
                PowerUI.this.mWarnings.showLowBatteryWarning(bucket == oldBucket ? oldPlugged : true);
                return;
            }
            if (isPowerSaver || plugged || (bucket > oldBucket && bucket > 0)) {
                PowerUI.this.mWarnings.dismissLowBatteryWarning();
            } else {
                PowerUI.this.mWarnings.updateLowBatteryWarning();
            }
        }
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("mLowBatteryAlertCloseLevel=");
        pw.println(this.mLowBatteryAlertCloseLevel);
        pw.print("mLowBatteryReminderLevels=");
        pw.println(Arrays.toString(this.mLowBatteryReminderLevels));
        pw.print("mBatteryLevel=");
        pw.println(Integer.toString(this.mBatteryLevel));
        pw.print("mBatteryStatus=");
        pw.println(Integer.toString(this.mBatteryStatus));
        pw.print("mPlugType=");
        pw.println(Integer.toString(this.mPlugType));
        pw.print("mInvalidCharger=");
        pw.println(Integer.toString(this.mInvalidCharger));
        pw.print("mScreenOffTime=");
        pw.print(this.mScreenOffTime);
        if (this.mScreenOffTime >= 0) {
            pw.print(" (");
            pw.print(SystemClock.elapsedRealtime() - this.mScreenOffTime);
            pw.print(" ago)");
        }
        pw.println();
        pw.print("soundTimeout=");
        pw.println(Settings.Global.getInt(this.mContext.getContentResolver(), "low_battery_sound_timeout", 0));
        pw.print("bucket: ");
        pw.println(Integer.toString(findBatteryLevelBucket(this.mBatteryLevel)));
        this.mWarnings.dump(pw);
    }
}
