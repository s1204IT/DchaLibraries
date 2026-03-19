package com.android.server.power.batterysaver;

import android.app.ActivityManagerInternal;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.Preconditions;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.power.BatterySaverPolicy;
import java.util.ArrayList;

public class BatterySaverController implements BatterySaverPolicy.BatterySaverPolicyListener {
    static final boolean DEBUG = false;
    public static final int REASON_AUTOMATIC_OFF = 1;
    public static final int REASON_AUTOMATIC_ON = 0;
    public static final int REASON_INTERACTIVE_CHANGED = 5;
    public static final int REASON_MANUAL_OFF = 3;
    public static final int REASON_MANUAL_ON = 2;
    public static final int REASON_PLUGGED_IN = 7;
    public static final int REASON_POLICY_CHANGED = 6;
    public static final int REASON_SETTING_CHANGED = 8;
    public static final int REASON_STICKY_RESTORE = 4;
    static final String TAG = "BatterySaverController";
    private final BatterySaverPolicy mBatterySaverPolicy;
    private final BatterySavingStats mBatterySavingStats;
    private final Context mContext;

    @GuardedBy("mLock")
    private boolean mEnabled;
    private final FileUpdater mFileUpdater;
    private final MyHandler mHandler;

    @GuardedBy("mLock")
    private boolean mIsInteractive;

    @GuardedBy("mLock")
    private boolean mIsPluggedIn;
    private final Object mLock;
    private final Plugin[] mPlugins;
    private PowerManager mPowerManager;
    private boolean mPreviouslyEnabled;

    @GuardedBy("mLock")
    private final ArrayList<PowerManagerInternal.LowPowerModeListener> mListeners = new ArrayList<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean z;
            z = true;
            switch (intent.getAction()) {
                case "android.intent.action.SCREEN_ON":
                case "android.intent.action.SCREEN_OFF":
                    if (!BatterySaverController.this.isEnabled()) {
                        BatterySaverController.this.updateBatterySavingStats();
                        return;
                    } else {
                        BatterySaverController.this.mHandler.postStateChanged(false, 5);
                        return;
                    }
                case "android.intent.action.BATTERY_CHANGED":
                    synchronized (BatterySaverController.this.mLock) {
                        BatterySaverController batterySaverController = BatterySaverController.this;
                        if (intent.getIntExtra("plugged", 0) == 0) {
                            z = false;
                        }
                        batterySaverController.mIsPluggedIn = z;
                        break;
                    }
                    break;
                case "android.os.action.DEVICE_IDLE_MODE_CHANGED":
                case "android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED":
                    break;
                default:
                    return;
            }
            BatterySaverController.this.updateBatterySavingStats();
        }
    };

    public interface Plugin {
        void onBatterySaverChanged(BatterySaverController batterySaverController);

        void onSystemReady(BatterySaverController batterySaverController);
    }

    public BatterySaverController(Object obj, Context context, Looper looper, BatterySaverPolicy batterySaverPolicy, BatterySavingStats batterySavingStats) {
        this.mLock = obj;
        this.mContext = context;
        this.mHandler = new MyHandler(looper);
        this.mBatterySaverPolicy = batterySaverPolicy;
        this.mBatterySaverPolicy.addListener(this);
        this.mFileUpdater = new FileUpdater(context);
        this.mBatterySavingStats = batterySavingStats;
        ArrayList arrayList = new ArrayList();
        arrayList.add(new BatterySaverLocationPlugin(this.mContext));
        this.mPlugins = (Plugin[]) arrayList.toArray(new Plugin[arrayList.size()]);
    }

    public void addListener(PowerManagerInternal.LowPowerModeListener lowPowerModeListener) {
        synchronized (this.mLock) {
            this.mListeners.add(lowPowerModeListener);
        }
    }

    public void systemReady() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        intentFilter.addAction("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        this.mFileUpdater.systemReady(((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).isRuntimeRestarted());
        this.mHandler.postSystemReady();
    }

    private PowerManager getPowerManager() {
        if (this.mPowerManager == null) {
            this.mPowerManager = (PowerManager) Preconditions.checkNotNull((PowerManager) this.mContext.getSystemService(PowerManager.class));
        }
        return this.mPowerManager;
    }

    @Override
    public void onBatterySaverPolicyChanged(BatterySaverPolicy batterySaverPolicy) {
        if (!isEnabled()) {
            return;
        }
        this.mHandler.postStateChanged(true, 6);
    }

    private class MyHandler extends Handler {
        private static final int ARG_DONT_SEND_BROADCAST = 0;
        private static final int ARG_SEND_BROADCAST = 1;
        private static final int MSG_STATE_CHANGED = 1;
        private static final int MSG_SYSTEM_READY = 2;

        public MyHandler(Looper looper) {
            super(looper);
        }

        public void postStateChanged(boolean z, int i) {
            int i2;
            if (z) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            obtainMessage(1, i2, i).sendToTarget();
        }

        public void postSystemReady() {
            obtainMessage(2, 0, 0).sendToTarget();
        }

        @Override
        public void dispatchMessage(Message message) {
            switch (message.what) {
                case 1:
                    BatterySaverController.this.handleBatterySaverStateChanged(message.arg1 == 1, message.arg2);
                    break;
                case 2:
                    for (Plugin plugin : BatterySaverController.this.mPlugins) {
                        plugin.onSystemReady(BatterySaverController.this);
                    }
                    break;
            }
        }
    }

    public void enableBatterySaver(boolean z, int i) {
        synchronized (this.mLock) {
            if (this.mEnabled == z) {
                return;
            }
            this.mEnabled = z;
            this.mHandler.postStateChanged(true, i);
        }
    }

    public boolean isEnabled() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mEnabled;
        }
        return z;
    }

    public boolean isInteractive() {
        boolean z;
        synchronized (this.mLock) {
            z = this.mIsInteractive;
        }
        return z;
    }

    public BatterySaverPolicy getBatterySaverPolicy() {
        return this.mBatterySaverPolicy;
    }

    public boolean isLaunchBoostDisabled() {
        return isEnabled() && this.mBatterySaverPolicy.isLaunchBoostDisabled();
    }

    void handleBatterySaverStateChanged(boolean z, int i) {
        PowerManagerInternal.LowPowerModeListener[] lowPowerModeListenerArr;
        boolean z2;
        ArrayMap<String, String> fileValues;
        boolean zIsInteractive = getPowerManager().isInteractive();
        synchronized (this.mLock) {
            EventLogTags.writeBatterySaverMode(this.mPreviouslyEnabled ? 1 : 0, this.mEnabled ? 1 : 0, zIsInteractive ? 1 : 0, this.mEnabled ? this.mBatterySaverPolicy.toEventLogString() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, i);
            this.mPreviouslyEnabled = this.mEnabled;
            lowPowerModeListenerArr = (PowerManagerInternal.LowPowerModeListener[]) this.mListeners.toArray(new PowerManagerInternal.LowPowerModeListener[this.mListeners.size()]);
            z2 = this.mEnabled;
            this.mIsInteractive = zIsInteractive;
            if (z2) {
                fileValues = this.mBatterySaverPolicy.getFileValues(zIsInteractive);
            } else {
                fileValues = null;
            }
        }
        PowerManagerInternal powerManagerInternal = (PowerManagerInternal) LocalServices.getService(PowerManagerInternal.class);
        if (powerManagerInternal != null) {
            powerManagerInternal.powerHint(5, z2 ? 1 : 0);
        }
        updateBatterySavingStats();
        if (ArrayUtils.isEmpty(fileValues)) {
            this.mFileUpdater.restoreDefault();
        } else {
            this.mFileUpdater.writeFiles(fileValues);
        }
        for (Plugin plugin : this.mPlugins) {
            plugin.onBatterySaverChanged(this);
        }
        if (z) {
            this.mContext.sendBroadcastAsUser(new Intent("android.os.action.POWER_SAVE_MODE_CHANGING").putExtra("mode", z2).addFlags(1073741824), UserHandle.ALL);
            Intent intent = new Intent("android.os.action.POWER_SAVE_MODE_CHANGED");
            intent.addFlags(1073741824);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            Intent intent2 = new Intent("android.os.action.POWER_SAVE_MODE_CHANGED_INTERNAL");
            intent2.addFlags(1073741824);
            this.mContext.sendBroadcastAsUser(intent2, UserHandle.ALL, "android.permission.DEVICE_POWER");
            for (PowerManagerInternal.LowPowerModeListener lowPowerModeListener : lowPowerModeListenerArr) {
                lowPowerModeListener.onLowPowerModeChanged(this.mBatterySaverPolicy.getBatterySaverPolicy(lowPowerModeListener.getServiceType(), z2));
            }
        }
    }

    private void updateBatterySavingStats() {
        int i;
        PowerManager powerManager = getPowerManager();
        if (powerManager == null) {
            Slog.wtf(TAG, "PowerManager not initialized");
            return;
        }
        boolean zIsInteractive = powerManager.isInteractive();
        if (powerManager.isDeviceIdleMode()) {
            i = 2;
        } else {
            i = powerManager.isLightDeviceIdleMode() ? 1 : 0;
        }
        synchronized (this.mLock) {
            if (this.mIsPluggedIn) {
                this.mBatterySavingStats.startCharging();
                return;
            }
            this.mBatterySavingStats.transitionState(this.mEnabled ? 1 : 0, zIsInteractive ? 1 : 0, i);
        }
    }
}
