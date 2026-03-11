package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import com.android.systemui.statusbar.policy.BatteryController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class BatteryControllerImpl extends BroadcastReceiver implements BatteryController {
    private static final boolean DEBUG = Log.isLoggable("BatteryController", 3);
    protected boolean mCharged;
    protected boolean mCharging;
    private final Context mContext;
    private boolean mDemoMode;
    protected int mLevel;
    protected boolean mPluggedIn;
    private final PowerManager mPowerManager;
    protected boolean mPowerSave;
    private final ArrayList<BatteryController.BatteryStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    private boolean mTestmode = false;
    private final Handler mHandler = new Handler();

    public BatteryControllerImpl(Context context) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        registerReceiver();
        updatePowerSave();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGING");
        filter.addAction("com.android.systemui.BATTERY_LEVEL_TEST");
        this.mContext.registerReceiver(this, filter);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BatteryController state:");
        pw.print("  mLevel=");
        pw.println(this.mLevel);
        pw.print("  mPluggedIn=");
        pw.println(this.mPluggedIn);
        pw.print("  mCharging=");
        pw.println(this.mCharging);
        pw.print("  mCharged=");
        pw.println(this.mCharged);
        pw.print("  mPowerSave=");
        pw.println(this.mPowerSave);
    }

    @Override
    public void setPowerSaveMode(boolean powerSave) {
        this.mPowerManager.setPowerSaveMode(powerSave);
    }

    @Override
    public void addStateChangedCallback(BatteryController.BatteryStateChangeCallback cb) {
        this.mChangeCallbacks.add(cb);
        cb.onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
        cb.onPowerSaveChanged(this.mPowerSave);
    }

    @Override
    public void removeStateChangedCallback(BatteryController.BatteryStateChangeCallback cb) {
        this.mChangeCallbacks.remove(cb);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        boolean z = true;
        String action = intent.getAction();
        if (action.equals("android.intent.action.BATTERY_CHANGED")) {
            if (this.mTestmode && !intent.getBooleanExtra("testmode", false)) {
                return;
            }
            this.mLevel = (int) ((intent.getIntExtra("level", 0) * 100.0f) / intent.getIntExtra("scale", 100));
            this.mPluggedIn = intent.getIntExtra("plugged", 0) != 0;
            int status = intent.getIntExtra("status", 1);
            this.mCharged = status == 5;
            if (!this.mCharged && status != 2) {
                z = false;
            }
            this.mCharging = z;
            fireBatteryLevelChanged();
            return;
        }
        if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGED")) {
            updatePowerSave();
            return;
        }
        if (action.equals("android.os.action.POWER_SAVE_MODE_CHANGING")) {
            setPowerSave(intent.getBooleanExtra("mode", false));
        } else {
            if (!action.equals("com.android.systemui.BATTERY_LEVEL_TEST")) {
                return;
            }
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
        }
    }

    @Override
    public boolean isPowerSave() {
        return this.mPowerSave;
    }

    private void updatePowerSave() {
        setPowerSave(this.mPowerManager.isPowerSaveMode());
    }

    private void setPowerSave(boolean powerSave) {
        if (powerSave == this.mPowerSave) {
            return;
        }
        this.mPowerSave = powerSave;
        if (DEBUG) {
            Log.d("BatteryController", "Power save is " + (this.mPowerSave ? "on" : "off"));
        }
        firePowerSaveChanged();
    }

    protected void fireBatteryLevelChanged() {
        for (int i = 0; i < this.mChangeCallbacks.size(); i++) {
            BatteryController.BatteryStateChangeCallback cb = this.mChangeCallbacks.get(i);
            if (cb != null) {
                cb.onBatteryLevelChanged(this.mLevel, this.mPluggedIn, this.mCharging);
            }
        }
    }

    private void firePowerSaveChanged() {
        for (int i = 0; i < this.mChangeCallbacks.size(); i++) {
            BatteryController.BatteryStateChangeCallback cb = this.mChangeCallbacks.get(i);
            if (cb != null) {
                cb.onPowerSaveChanged(this.mPowerSave);
            }
        }
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            this.mContext.unregisterReceiver(this);
            return;
        }
        if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            registerReceiver();
            updatePowerSave();
        } else {
            if (!this.mDemoMode || !command.equals("battery")) {
                return;
            }
            String level = args.getString("level");
            String plugged = args.getString("plugged");
            if (level != null) {
                this.mLevel = Math.min(Math.max(Integer.parseInt(level), 0), 100);
            }
            if (plugged != null) {
                this.mPluggedIn = Boolean.parseBoolean(plugged);
            }
            fireBatteryLevelChanged();
        }
    }
}
