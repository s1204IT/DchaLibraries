package com.mediatek.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.internal.os.InstallerConnection;
import com.android.server.am.ActivityManagerService;
import com.android.server.audio.AudioService;
import com.android.server.pm.Installer;
import com.mediatek.am.AMEventHookAction;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.AMEventHookResult;
import com.mediatek.ipomanager.ActivityManagerPlus;
import dalvik.system.VMRuntime;

public class PowerOffAlarmUtility {
    private static final String ALARM_BOOT_DONE = "android.intent.action.normal.boot.done";
    private static final String REMOVE_IPOWIN = "alarm.boot.remove.ipowin";
    private static final String TAG = "PowerOffAlarmUtility";
    private static PowerOffAlarmUtility mInstance;
    private Context mContext;
    private ActivityManagerService mService;
    private boolean mRollback = false;
    public boolean mFirstBoot = false;

    public static PowerOffAlarmUtility getInstance(Context ctx, ActivityManagerService aService) {
        if (mInstance != null) {
            return mInstance;
        }
        if (ctx != null && aService != null) {
            mInstance = new PowerOffAlarmUtility(ctx, aService);
        }
        return mInstance;
    }

    private PowerOffAlarmUtility(Context ctx, ActivityManagerService aService) {
        this.mContext = ctx;
        this.mService = aService;
        registerNormalBootReceiver(this.mContext);
        boolean recover = SystemProperties.getBoolean("persist.sys.ams.recover", false);
        if (!recover) {
            return;
        }
        checkFlightMode(true, false);
    }

    public void launchPowerOffAlarm(Boolean recover, Boolean shutdown) {
        if (recover != null && shutdown != null) {
            checkFlightMode(recover.booleanValue(), shutdown.booleanValue());
        }
        this.mContext.sendStickyBroadcast(new Intent("android.intent.action.LAUNCH_POWEROFF_ALARM"));
    }

    public static boolean isAlarmBoot() {
        String bootReason = SystemProperties.get("sys.boot.reason");
        return bootReason != null && bootReason.equals("1");
    }

    private final void registerNormalBootReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.normal.boot");
        filter.addAction("android.intent.action.normal.shutdown");
        filter.addAction("android.intent.action.normal.boot.done");
        filter.addAction(REMOVE_IPOWIN);
        filter.addAction("android.intent.action.ACTION_BOOT_IPO");
        this.mFirstBoot = true;
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (intent == null) {
                    return;
                }
                String action = intent.getAction();
                if ("android.intent.action.normal.boot".equals(action)) {
                    Log.i(PowerOffAlarmUtility.TAG, "DeskClock normally boots-up device");
                    if (PowerOffAlarmUtility.this.mRollback) {
                        PowerOffAlarmUtility.this.checkFlightMode(false, false);
                    }
                    if (PowerOffAlarmUtility.this.mFirstBoot) {
                        synchronized (PowerOffAlarmUtility.this.mService) {
                            try {
                                ActivityManagerService.boostPriorityForLockedSection();
                                PowerOffAlarmUtility.this.mService.resumeTopActivityOnSystemReadyFocusedStackLocked();
                            } catch (Throwable th) {
                                throw th;
                            }
                        }
                        return;
                    }
                    ActivityManagerPlus.ipoBootCompleted();
                    return;
                }
                if ("android.intent.action.normal.shutdown".equals(action)) {
                    Log.v(PowerOffAlarmUtility.TAG, "DeskClock normally shutdowns device");
                    ActivityManagerPlus.createIPOWin();
                    if (!PowerOffAlarmUtility.this.mRollback) {
                        return;
                    }
                    PowerOffAlarmUtility.this.checkFlightMode(false, true);
                    return;
                }
                if ("android.intent.action.normal.boot.done".equals(action)) {
                    Log.w(PowerOffAlarmUtility.TAG, "ALARM_BOOT_DONE normally shutdowns device");
                    synchronized (PowerOffAlarmUtility.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            PowerOffAlarmUtility.this.mService.resumeTopActivityOnSystemReadyFocusedStackLocked();
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    return;
                }
                if (PowerOffAlarmUtility.REMOVE_IPOWIN.equals(action)) {
                    ActivityManagerPlus.removeIPOWin();
                } else {
                    if (!"android.intent.action.ACTION_BOOT_IPO".equals(action) || !PowerOffAlarmUtility.isAlarmBoot()) {
                        return;
                    }
                    Slog.v(PowerOffAlarmUtility.TAG, "power off alarm enabled");
                    PowerOffAlarmUtility.this.launchPowerOffAlarm(false, false);
                }
            }
        }, filter);
    }

    private void checkFlightMode(boolean recover, boolean shutdown) {
        Log.v(TAG, "mRollback = " + this.mRollback + ", recover = " + recover);
        if (recover) {
            Log.v(TAG, "since system crash, switch flight mode to off");
            Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
            SystemProperties.set("persist.sys.ams.recover", "false");
            return;
        }
        if (this.mRollback) {
            this.mRollback = false;
            SystemProperties.set("persist.sys.ams.recover", "false");
            Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
            if (shutdown) {
                return;
            }
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.addFlags(536870912);
            intent.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, false);
            this.mContext.sendBroadcast(intent);
            return;
        }
        boolean mode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
        if (mode) {
            return;
        }
        Log.v(TAG, "turn on flight mode");
        SystemProperties.set("persist.sys.ams.recover", "true");
        this.mRollback = true;
        Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 1);
        Intent intent_turn_on = new Intent("android.intent.action.AIRPLANE_MODE");
        intent_turn_on.addFlags(536870912);
        intent_turn_on.putExtra(AudioService.CONNECT_INTENT_KEY_STATE, true);
        this.mContext.sendBroadcast(intent_turn_on);
    }

    private void markBootComplete(Installer installer) {
        ArraySet<String> completedIsas = new ArraySet<>();
        for (String abi : Build.SUPPORTED_ABIS) {
            Process.establishZygoteConnectionForAbi(abi);
            String instructionSet = VMRuntime.getInstructionSet(abi);
            if (!completedIsas.contains(instructionSet)) {
                try {
                    installer.markBootComplete(VMRuntime.getInstructionSet(abi));
                } catch (InstallerConnection.InstallerException e) {
                    Slog.e(TAG, "Unable to mark boot complete for abi: " + abi, e);
                }
                completedIsas.add(instructionSet);
            }
        }
    }

    public static PowerOffAlarmUtility getInstance(AMEventHookData.SystemReady data) {
        Context context = (Context) data.get(AMEventHookData.SystemReady.Index.context);
        ActivityManagerService ams = (ActivityManagerService) data.get(AMEventHookData.SystemReady.Index.ams);
        return getInstance(context, ams);
    }

    public void onSystemReady(AMEventHookData.SystemReady data, AMEventHookResult result) {
        int phase = data.getInt(AMEventHookData.SystemReady.Index.phase);
        switch (phase) {
            case 200:
                this.mFirstBoot = true;
                break;
            case 300:
                if (!isAlarmBoot()) {
                    result.addAction(AMEventHookAction.AM_SkipHomeActivityLaunching);
                }
                break;
            case 400:
                if (isAlarmBoot()) {
                    result.addAction(AMEventHookAction.AM_PostEnableScreenAfterBoot);
                }
                break;
        }
    }

    public void onAfterPostEnableScreenAfterBoot(AMEventHookData.AfterPostEnableScreenAfterBoot data, AMEventHookResult result) {
        Installer installer = (Installer) data.get(AMEventHookData.AfterPostEnableScreenAfterBoot.Index.installer);
        markBootComplete(installer);
        Slog.v(TAG, "power off alarm enabled");
        launchPowerOffAlarm(false, false);
        result.addAction(AMEventHookAction.AM_Interrupt);
    }
}
