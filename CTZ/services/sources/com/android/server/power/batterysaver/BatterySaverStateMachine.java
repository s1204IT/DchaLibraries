package com.android.server.power.batterysaver;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.server.EventLogTags;
import com.mediatek.server.powerhal.PowerHalManager;
import java.io.PrintWriter;

public class BatterySaverStateMachine {
    private static final boolean DEBUG = false;
    private static final String TAG = "BatterySaverStateMachine";

    @GuardedBy("mLock")
    private int mBatteryLevel;
    private final BatterySaverController mBatterySaverController;

    @GuardedBy("mLock")
    private boolean mBatterySaverSnoozing;

    @GuardedBy("mLock")
    private boolean mBatteryStatusSet;

    @GuardedBy("mLock")
    private boolean mBootCompleted;
    private final Context mContext;

    @GuardedBy("mLock")
    private boolean mIsBatteryLevelLow;

    @GuardedBy("mLock")
    private boolean mIsPowered;

    @GuardedBy("mLock")
    private int mLastChangedIntReason;

    @GuardedBy("mLock")
    private String mLastChangedStrReason;
    private final Object mLock;

    @GuardedBy("mLock")
    private boolean mSettingBatterySaverEnabled;

    @GuardedBy("mLock")
    private boolean mSettingBatterySaverEnabledSticky;

    @GuardedBy("mLock")
    private int mSettingBatterySaverTriggerThreshold;

    @GuardedBy("mLock")
    private boolean mSettingsLoaded;
    private final ContentObserver mSettingsObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean z) {
            synchronized (BatterySaverStateMachine.this.mLock) {
                BatterySaverStateMachine.this.refreshSettingsLocked();
            }
        }
    };
    private final Runnable mThresholdChangeLogger = new Runnable() {
        @Override
        public final void run() {
            EventLogTags.writeBatterySaverSetting(this.f$0.mSettingBatterySaverTriggerThreshold);
        }
    };

    public BatterySaverStateMachine(Object obj, Context context, BatterySaverController batterySaverController) {
        this.mLock = obj;
        this.mContext = context;
        this.mBatterySaverController = batterySaverController;
    }

    private boolean isBatterySaverEnabled() {
        return this.mBatterySaverController.isEnabled();
    }

    private boolean isAutoBatterySaverConfigured() {
        return this.mSettingBatterySaverTriggerThreshold > 0;
    }

    public void onBootCompleted() {
        putGlobalSetting("low_power", 0);
        runOnBgThread(new Runnable() {
            @Override
            public final void run() {
                BatterySaverStateMachine.lambda$onBootCompleted$0(this.f$0);
            }
        });
    }

    public static void lambda$onBootCompleted$0(BatterySaverStateMachine batterySaverStateMachine) {
        ContentResolver contentResolver = batterySaverStateMachine.mContext.getContentResolver();
        contentResolver.registerContentObserver(Settings.Global.getUriFor("low_power"), false, batterySaverStateMachine.mSettingsObserver, 0);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("low_power_sticky"), false, batterySaverStateMachine.mSettingsObserver, 0);
        contentResolver.registerContentObserver(Settings.Global.getUriFor("low_power_trigger_level"), false, batterySaverStateMachine.mSettingsObserver, 0);
        synchronized (batterySaverStateMachine.mLock) {
            batterySaverStateMachine.mBootCompleted = true;
            batterySaverStateMachine.refreshSettingsLocked();
            batterySaverStateMachine.doAutoBatterySaverLocked();
        }
    }

    @VisibleForTesting
    void runOnBgThread(Runnable runnable) {
        BackgroundThread.getHandler().post(runnable);
    }

    @VisibleForTesting
    void runOnBgThreadLazy(Runnable runnable, int i) {
        Handler handler = BackgroundThread.getHandler();
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, i);
    }

    void refreshSettingsLocked() {
        setSettingsLocked(getGlobalSetting("low_power", 0) != 0, getGlobalSetting("low_power_sticky", 0) != 0, getGlobalSetting("low_power_trigger_level", 0));
    }

    @VisibleForTesting
    void setSettingsLocked(boolean z, boolean z2, int i) {
        this.mSettingsLoaded = true;
        boolean z3 = this.mSettingBatterySaverEnabled != z;
        boolean z4 = this.mSettingBatterySaverEnabledSticky != z2;
        boolean z5 = this.mSettingBatterySaverTriggerThreshold != i;
        if (!z3 && !z4 && !z5) {
            return;
        }
        this.mSettingBatterySaverEnabled = z;
        this.mSettingBatterySaverEnabledSticky = z2;
        this.mSettingBatterySaverTriggerThreshold = i;
        if (z5) {
            runOnBgThreadLazy(this.mThresholdChangeLogger, PowerHalManager.ROTATE_BOOST_TIME);
        }
        if (z3) {
            enableBatterySaverLocked(z, true, 8, z ? "Global.low_power changed to 1" : "Global.low_power changed to 0");
        }
    }

    public void setBatteryStatus(boolean z, int i, boolean z2) {
        synchronized (this.mLock) {
            boolean z3 = true;
            this.mBatteryStatusSet = true;
            boolean z4 = this.mIsPowered != z;
            boolean z5 = this.mBatteryLevel != i;
            if (this.mIsBatteryLevelLow == z2) {
                z3 = false;
            }
            if (z4 || z5 || z3) {
                this.mIsPowered = z;
                this.mBatteryLevel = i;
                this.mIsBatteryLevelLow = z2;
                doAutoBatterySaverLocked();
            }
        }
    }

    private void doAutoBatterySaverLocked() {
        if (!this.mBootCompleted || !this.mSettingsLoaded || !this.mBatteryStatusSet) {
            return;
        }
        if (!this.mIsBatteryLevelLow) {
            updateSnoozingLocked(false, "Battery not low");
        }
        if (this.mIsPowered) {
            updateSnoozingLocked(false, "Plugged in");
            enableBatterySaverLocked(false, false, 7, "Plugged in");
        } else {
            if (this.mSettingBatterySaverEnabledSticky) {
                enableBatterySaverLocked(true, true, 4, "Sticky restore");
                return;
            }
            if (this.mIsBatteryLevelLow) {
                if (!this.mBatterySaverSnoozing && isAutoBatterySaverConfigured()) {
                    enableBatterySaverLocked(true, false, 0, "Auto ON");
                    return;
                }
                return;
            }
            enableBatterySaverLocked(false, false, 1, "Auto OFF");
        }
    }

    public void setBatterySaverEnabledManually(boolean z) {
        synchronized (this.mLock) {
            try {
                enableBatterySaverLocked(z, true, z ? 2 : 3, z ? "Manual ON" : "Manual OFF");
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private void enableBatterySaverLocked(boolean z, boolean z2, int i, String str) {
        if (this.mBatterySaverController.isEnabled() == z) {
            return;
        }
        if (z && this.mIsPowered) {
            return;
        }
        this.mLastChangedIntReason = i;
        this.mLastChangedStrReason = str;
        if (z2) {
            if (z) {
                updateSnoozingLocked(false, "Manual snooze OFF");
            } else if (isBatterySaverEnabled() && this.mIsBatteryLevelLow) {
                updateSnoozingLocked(true, "Manual snooze");
            }
        }
        this.mSettingBatterySaverEnabled = z;
        putGlobalSetting("low_power", z ? 1 : 0);
        if (z2) {
            this.mSettingBatterySaverEnabledSticky = z;
            putGlobalSetting("low_power_sticky", z ? 1 : 0);
        }
        this.mBatterySaverController.enableBatterySaver(z, i);
    }

    private void updateSnoozingLocked(boolean z, String str) {
        if (this.mBatterySaverSnoozing == z) {
            return;
        }
        this.mBatterySaverSnoozing = z;
    }

    @VisibleForTesting
    protected void putGlobalSetting(String str, int i) {
        Settings.Global.putInt(this.mContext.getContentResolver(), str, i);
    }

    @VisibleForTesting
    protected int getGlobalSetting(String str, int i) {
        return Settings.Global.getInt(this.mContext.getContentResolver(), str, i);
    }

    public void dump(PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println();
            printWriter.println("Battery saver state machine:");
            printWriter.print("  Enabled=");
            printWriter.println(this.mBatterySaverController.isEnabled());
            printWriter.print("  mLastChangedIntReason=");
            printWriter.println(this.mLastChangedIntReason);
            printWriter.print("  mLastChangedStrReason=");
            printWriter.println(this.mLastChangedStrReason);
            printWriter.print("  mBootCompleted=");
            printWriter.println(this.mBootCompleted);
            printWriter.print("  mSettingsLoaded=");
            printWriter.println(this.mSettingsLoaded);
            printWriter.print("  mBatteryStatusSet=");
            printWriter.println(this.mBatteryStatusSet);
            printWriter.print("  mBatterySaverSnoozing=");
            printWriter.println(this.mBatterySaverSnoozing);
            printWriter.print("  mIsPowered=");
            printWriter.println(this.mIsPowered);
            printWriter.print("  mBatteryLevel=");
            printWriter.println(this.mBatteryLevel);
            printWriter.print("  mIsBatteryLevelLow=");
            printWriter.println(this.mIsBatteryLevelLow);
            printWriter.print("  mSettingBatterySaverEnabled=");
            printWriter.println(this.mSettingBatterySaverEnabled);
            printWriter.print("  mSettingBatterySaverEnabledSticky=");
            printWriter.println(this.mSettingBatterySaverEnabledSticky);
            printWriter.print("  mSettingBatterySaverTriggerThreshold=");
            printWriter.println(this.mSettingBatterySaverTriggerThreshold);
        }
    }

    public void dumpProto(ProtoOutputStream protoOutputStream, long j) {
        synchronized (this.mLock) {
            long jStart = protoOutputStream.start(j);
            protoOutputStream.write(1133871366145L, this.mBatterySaverController.isEnabled());
            protoOutputStream.write(1133871366146L, this.mBootCompleted);
            protoOutputStream.write(1133871366147L, this.mSettingsLoaded);
            protoOutputStream.write(1133871366148L, this.mBatteryStatusSet);
            protoOutputStream.write(1133871366149L, this.mBatterySaverSnoozing);
            protoOutputStream.write(1133871366150L, this.mIsPowered);
            protoOutputStream.write(1120986464263L, this.mBatteryLevel);
            protoOutputStream.write(1133871366152L, this.mIsBatteryLevelLow);
            protoOutputStream.write(1133871366153L, this.mSettingBatterySaverEnabled);
            protoOutputStream.write(1133871366154L, this.mSettingBatterySaverEnabledSticky);
            protoOutputStream.write(1120986464267L, this.mSettingBatterySaverTriggerThreshold);
            protoOutputStream.end(jStart);
        }
    }
}
