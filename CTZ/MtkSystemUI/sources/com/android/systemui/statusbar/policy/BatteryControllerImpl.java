package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.fuelgauge.BatterySaverUtils;
import com.android.systemui.statusbar.policy.BatteryController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BatteryControllerImpl extends BroadcastReceiver implements BatteryController {
    private static final boolean DEBUG = Log.isLoggable("BatteryController", 3);
    protected boolean mAodPowerSave;
    private final ArrayList<BatteryController.BatteryStateChangeCallback> mChangeCallbacks;
    protected boolean mCharged;
    protected boolean mCharging;
    private final Context mContext;
    private boolean mDemoMode;
    private final Handler mHandler;
    private boolean mHasReceivedBattery;
    protected int mLevel;
    protected boolean mPluggedIn;
    private final PowerManager mPowerManager;
    protected boolean mPowerSave;
    private boolean mTestmode;

    public BatteryControllerImpl(Context context) {
        this(context, (PowerManager) context.getSystemService(PowerManager.class));
    }

    @VisibleForTesting
    BatteryControllerImpl(Context context, PowerManager powerManager) {
        this.mChangeCallbacks = new ArrayList<>();
        this.mTestmode = false;
        this.mHasReceivedBattery = false;
        this.mContext = context;
        this.mHandler = new Handler();
        this.mPowerManager = powerManager;
        registerReceiver();
        updatePowerSave();
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGING");
        intentFilter.addAction("com.android.systemui.BATTERY_LEVEL_TEST");
        this.mContext.registerReceiver(this, intentFilter);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("BatteryController state:");
        printWriter.print("  mLevel=");
        printWriter.println(this.mLevel);
        printWriter.print("  mPluggedIn=");
        printWriter.println(this.mPluggedIn);
        printWriter.print("  mCharging=");
        printWriter.println(this.mCharging);
        printWriter.print("  mCharged=");
        printWriter.println(this.mCharged);
        printWriter.print("  mPowerSave=");
        printWriter.println(this.mPowerSave);
    }

    @Override
    public void setPowerSaveMode(boolean z) {
        BatterySaverUtils.setPowerSaveMode(this.mContext, z, true);
    }

    @Override
    public void addCallback(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        synchronized (this.mChangeCallbacks) {
            this.mChangeCallbacks.add(batteryStateChangeCallback);
        }
        if (this.mHasReceivedBattery) {
            batteryStateChangeCallback.onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
            batteryStateChangeCallback.onPowerSaveChanged(this.mPowerSave);
        }
    }

    @Override
    public void removeCallback(BatteryController.BatteryStateChangeCallback batteryStateChangeCallback) {
        synchronized (this.mChangeCallbacks) {
            this.mChangeCallbacks.remove(batteryStateChangeCallback);
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        boolean z = true;
        if (!action.equals("android.intent.action.BATTERY_CHANGED")) {
            if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) {
                updatePowerSave();
                return;
            }
            if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGING")) {
                setPowerSave(intent.getBooleanExtra("mode", false));
                return;
            } else {
                if (action.equals("com.android.systemui.BATTERY_LEVEL_TEST")) {
                    this.mTestmode = true;
                    this.mHandler.post(new Runnable() {
                        int saveLevel;
                        boolean savePlugged;
                        int curLevel = 0;
                        int incr = 1;
                        Intent dummy = new Intent("android.intent.action.BATTERY_CHANGED");

                        {
                            this.saveLevel = BatteryControllerImpl.this.mLevel;
                            this.savePlugged = BatteryControllerImpl.this.mPluggedIn;
                        }

                        @Override
                        public void run() {
                            if (this.curLevel < 0) {
                                BatteryControllerImpl.this.mTestmode = false;
                                this.dummy.putExtra("level", this.saveLevel);
                                this.dummy.putExtra("plugged", this.savePlugged);
                                this.dummy.putExtra("testmode", false);
                            } else {
                                this.dummy.putExtra("level", this.curLevel);
                                this.dummy.putExtra("plugged", this.incr > 0 ? 1 : 0);
                                this.dummy.putExtra("testmode", true);
                            }
                            context.sendBroadcast(this.dummy);
                            if (BatteryControllerImpl.this.mTestmode) {
                                this.curLevel += this.incr;
                                if (this.curLevel == 100) {
                                    this.incr *= -1;
                                }
                                BatteryControllerImpl.this.mHandler.postDelayed(this, 200L);
                            }
                        }
                    });
                    return;
                }
                return;
            }
        }
        if (!this.mTestmode || intent.getBooleanExtra("testmode", false)) {
            this.mHasReceivedBattery = true;
            this.mLevel = (int) ((100.0f * intent.getIntExtra("level", 0)) / intent.getIntExtra("scale", 100));
            this.mPluggedIn = intent.getIntExtra("plugged", 0) != 0;
            int intExtra = intent.getIntExtra("status", 1);
            this.mCharged = intExtra == 5;
            if (!this.mCharged && intExtra != 2) {
                z = false;
            }
            this.mCharging = z;
            fireBatteryLevelChanged();
        }
    }

    @Override
    public boolean isPowerSave() {
        return this.mPowerSave;
    }

    @Override
    public boolean isAodPowerSave() {
        return this.mAodPowerSave;
    }

    private void updatePowerSave() {
        setPowerSave(this.mPowerManager.isPowerSaveMode());
    }

    private void setPowerSave(boolean z) {
        if (z == this.mPowerSave) {
            return;
        }
        this.mPowerSave = z;
        this.mAodPowerSave = this.mPowerManager.getPowerSaveState(14).batterySaverEnabled;
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("Power save is ");
            sb.append(this.mPowerSave ? "on" : "off");
            Log.d("BatteryController", sb.toString());
        }
        firePowerSaveChanged();
    }

    protected void fireBatteryLevelChanged() {
        synchronized (this.mChangeCallbacks) {
            int size = this.mChangeCallbacks.size();
            for (int i = 0; i < size; i++) {
                this.mChangeCallbacks.get(i).onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
            }
        }
    }

    private void firePowerSaveChanged() {
        synchronized (this.mChangeCallbacks) {
            int size = this.mChangeCallbacks.size();
            for (int i = 0; i < size; i++) {
                this.mChangeCallbacks.get(i).onPowerSaveChanged(this.mPowerSave);
            }
        }
    }

    @Override
    public void dispatchDemoCommand(String str, Bundle bundle) {
        if (!this.mDemoMode && str.equals("enter")) {
            this.mDemoMode = true;
            this.mContext.unregisterReceiver(this);
            return;
        }
        if (this.mDemoMode && str.equals("exit")) {
            this.mDemoMode = false;
            registerReceiver();
            updatePowerSave();
        } else if (this.mDemoMode && str.equals("battery")) {
            String string = bundle.getString("level");
            String string2 = bundle.getString("plugged");
            String string3 = bundle.getString("powersave");
            if (string != null) {
                this.mLevel = Math.min(Math.max(Integer.parseInt(string), 0), 100);
            }
            if (string2 != null) {
                this.mPluggedIn = Boolean.parseBoolean(string2);
            }
            if (string3 != null) {
                this.mPowerSave = string3.equals("true");
                firePowerSaveChanged();
            }
            fireBatteryLevelChanged();
        }
    }
}
