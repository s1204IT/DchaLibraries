package com.android.server.power;

import android.R;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.IBluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.ConnectivityManager;
import android.nfc.INfcAdapter;
import android.os.FileUtils;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.UserManager;
import android.os.storage.IMountService;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import com.android.internal.app.ShutdownManager;
import com.android.internal.telephony.ITelephony;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.common.MPlugin;
import com.mediatek.common.bootanim.IBootAnimExt;
import com.mediatek.datashaping.DataShapingUtils;
import java.io.File;
import java.io.IOException;
import java.lang.Thread;

public final class ShutdownThread extends Thread {
    private static final String ACTION_PRE_SHUTDOWN = "android.intent.action.ACTION_PRE_SHUTDOWN";
    private static final String ACTION_SHUTDOWN_IPO = "android.intent.action.ACTION_SHUTDOWN_IPO";
    private static final int ACTIVITY_MANAGER_STOP_PERCENT = 4;
    public static final String AUDIT_SAFEMODE_PROPERTY = "persist.sys.audit_safemode";
    private static final int BROADCAST_STOP_PERCENT = 2;
    private static final int IPO_SHUTDOWN_FLOW = 1;
    private static final int MAX_BROADCAST_TIME = 10000;
    private static final int MAX_RADIO_WAIT_TIME = 12000;
    private static final int MAX_SHUTDOWN_WAIT_TIME = 20000;
    private static final int MAX_UNCRYPT_WAIT_TIME = 900000;
    private static final int MIN_SHUTDOWN_ANIMATION_PLAY_TIME = 5000;
    private static final int MOUNT_SERVICE_STOP_PERCENT = 20;
    private static final int NORMAL_SHUTDOWN_FLOW = 0;
    private static final int PACKAGE_MANAGER_STOP_PERCENT = 6;
    private static final int PHONE_STATE_POLL_SLEEP_MSEC = 500;
    private static final int RADIO_STOP_PERCENT = 18;
    public static final String REBOOT_SAFEMODE_PROPERTY = "persist.sys.safemode";
    public static final String RO_SAFEMODE_PROPERTY = "ro.sys.safemode";
    public static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";
    private static final int SHUTDOWN_VIBRATE_MS = 500;
    private static final String TAG = "ShutdownThread";
    private static final String changeToNormalMessage = "change shutdown flow from ipo to normal";
    private static String command = null;
    private static String mReason = null;
    private static boolean mReboot = false;
    private static boolean mRebootHasProgressBar = false;
    private static boolean mRebootSafeMode = false;
    private static final boolean mSpew = true;
    private static AlertDialog sConfirmDialog;
    private boolean mActionDone;
    private Context mContext;
    private PowerManager.WakeLock mCpuWakeLock;
    private Handler mHandler;
    private PowerManager mPowerManager;
    private ProgressDialog mProgressDialog;
    private PowerManager.WakeLock mScreenWakeLock;
    private int mShutdownFlow;
    private static Object sIsStartedGuard = new Object();
    private static boolean sIsStarted = false;
    private static final ShutdownThread sInstance = new ShutdownThread();
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private static Object mShutdownThreadSync = new Object();
    private static long beginAnimationTime = 0;
    private static long endAnimationTime = 0;
    private static boolean bConfirmForAnimation = true;
    private static boolean bPlayaudio = true;
    private static final Object mEnableAnimatingSync = new Object();
    private static boolean mEnableAnimating = true;
    private static IBootAnimExt mIBootAnim = null;
    private static Runnable mDelayDim = new Runnable() {
        @Override
        public void run() {
            Log.d(ShutdownThread.TAG, "setBacklightBrightness: Off");
            if (ShutdownThread.sInstance.mScreenWakeLock != null && ShutdownThread.sInstance.mScreenWakeLock.isHeld()) {
                ShutdownThread.sInstance.mScreenWakeLock.release();
            }
            if (ShutdownThread.sInstance.mPowerManager == null) {
                ShutdownThread.sInstance.mPowerManager = (PowerManager) ShutdownThread.sInstance.mContext.getSystemService("power");
            }
            ShutdownThread.sInstance.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 7, 0);
        }
    };
    private final Object mActionDoneSync = new Object();
    private ShutdownManager mShutdownManager = ShutdownManager.getInstance();

    private ShutdownThread() {
    }

    public static void EnableAnimating(boolean enable) {
        synchronized (mEnableAnimatingSync) {
            mEnableAnimating = enable;
        }
    }

    public static void shutdown(Context context, String reason, boolean confirm) {
        mReboot = false;
        mRebootSafeMode = false;
        mReason = reason;
        Log.d(TAG, "!!! Request to shutdown !!!");
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (StackTraceElement element : stack) {
            Log.d(TAG, "    |----" + element.toString());
        }
        if (SystemProperties.getBoolean("ro.monkey", false)) {
            Log.d(TAG, "Cannot request to shutdown when Monkey is running, returning.");
        } else {
            shutdownInner(context, confirm);
        }
    }

    static void shutdownInner(final Context context, boolean confirm) {
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Request to shutdown already running, returning.");
                return;
            }
            int longPressBehavior = context.getResources().getInteger(R.integer.config_cursorWindowSize);
            int resourceId = mRebootSafeMode ? R.string.accessibility_system_action_screenshot_label : longPressBehavior == 2 ? R.string.accessibility_system_action_quick_settings_label : R.string.accessibility_system_action_power_dialog_label;
            Log.d(TAG, "Notifying thread to start shutdown longPressBehavior=" + longPressBehavior);
            if (!confirm) {
                beginShutdownSequence(context);
                return;
            }
            CloseDialogReceiver closer = new CloseDialogReceiver(context);
            if (sConfirmDialog != null) {
                sConfirmDialog.dismiss();
            }
            bConfirmForAnimation = confirm;
            Log.d(TAG, "PowerOff dialog doesn't exist. Create it first");
            sConfirmDialog = new AlertDialog.Builder(context).setTitle(mRebootSafeMode ? R.string.accessibility_system_action_recents_label : R.string.accessibility_system_action_dpad_right_label).setMessage(resourceId).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ShutdownThread.beginShutdownSequence(context);
                }
            }).setNegativeButton(R.string.no, (DialogInterface.OnClickListener) null).create();
            closer.dialog = sConfirmDialog;
            sConfirmDialog.setOnDismissListener(closer);
            sConfirmDialog.getWindow().setType(2009);
            sConfirmDialog.show();
        }
    }

    private static class CloseDialogReceiver extends BroadcastReceiver implements DialogInterface.OnDismissListener {
        public Dialog dialog;
        private Context mContext;

        CloseDialogReceiver(Context context) {
            this.mContext = context;
            IntentFilter filter = new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS");
            context.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.dialog.cancel();
        }

        @Override
        public void onDismiss(DialogInterface unused) {
            this.mContext.unregisterReceiver(this);
        }
    }

    public static void reboot(Context context, String reason, boolean confirm) {
        mReboot = true;
        mRebootSafeMode = false;
        mRebootHasProgressBar = false;
        mReason = reason;
        Log.d(TAG, "reboot");
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (StackTraceElement element : stack) {
            Log.d(TAG, "     |----" + element.toString());
        }
        shutdownInner(context, confirm);
    }

    public static void rebootSafeMode(Context context, boolean confirm) {
        UserManager um = (UserManager) context.getSystemService("user");
        if (um.hasUserRestriction("no_safe_boot")) {
            return;
        }
        mReboot = true;
        mRebootSafeMode = true;
        mRebootHasProgressBar = false;
        mReason = null;
        Log.d(TAG, "rebootSafeMode");
        shutdownInner(context, confirm);
    }

    private static boolean configShutdownAnimation(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService("power");
        if (!bConfirmForAnimation && !pm.isScreenOn()) {
            bPlayaudio = false;
        } else {
            bPlayaudio = true;
        }
        try {
            String cust = SystemProperties.get("persist.operator.optr");
            if (mIBootAnim == null) {
                mIBootAnim = (IBootAnimExt) MPlugin.createInstance(IBootAnimExt.class.getName(), context);
            }
            if (cust != null && cust.equals("CUST")) {
                return true;
            }
            boolean mShutOffAnimation = mIBootAnim.isCustBootAnim();
            return mShutOffAnimation;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int getScreenTurnOffTime(Context context) {
        int screenTurnOffTime = 0;
        try {
            if (mIBootAnim == null) {
                mIBootAnim = (IBootAnimExt) MPlugin.createInstance(IBootAnimExt.class.getName(), context);
            }
            screenTurnOffTime = mIBootAnim.getScreenTurnOffTime();
            Log.d(TAG, "IBootAnim get screenTurnOffTime : " + screenTurnOffTime);
            return screenTurnOffTime;
        } catch (Exception e) {
            e.printStackTrace();
            return screenTurnOffTime;
        }
    }

    private static void beginShutdownSequence(Context context) {
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Shutdown sequence already running, returning.");
                return;
            }
            sIsStarted = true;
            ProgressDialog pd = new ProgressDialog(context);
            if ("recovery-update".equals(mReason)) {
                boolean z = RecoverySystem.UNCRYPT_PACKAGE_FILE.exists() && !RecoverySystem.BLOCK_MAP_FILE.exists();
                mRebootHasProgressBar = z;
                pd.setTitle(context.getText(R.string.accessibility_system_action_home_label));
                if (mRebootHasProgressBar) {
                    pd.setMax(100);
                    pd.setProgress(0);
                    pd.setIndeterminate(false);
                    pd.setProgressNumberFormat(null);
                    pd.setProgressStyle(1);
                    pd.setMessage(context.getText(R.string.accessibility_system_action_lock_screen_label));
                } else {
                    pd.setIndeterminate(true);
                    pd.setMessage(context.getText(R.string.accessibility_system_action_menu_label));
                }
            } else if ("recovery".equals(mReason)) {
                pd.setTitle(context.getText(R.string.accessibility_system_action_notifications_label));
                pd.setMessage(context.getText(R.string.accessibility_system_action_on_screen_a11y_shortcut_chooser_label));
                pd.setIndeterminate(true);
            } else {
                pd.setTitle(context.getText(R.string.accessibility_system_action_dpad_right_label));
                pd.setMessage(context.getText(R.string.accessibility_system_action_on_screen_a11y_shortcut_label));
                pd.setIndeterminate(true);
            }
            pd.setCancelable(false);
            pd.getWindow().setType(2009);
            sInstance.mContext = context;
            sInstance.mPowerManager = (PowerManager) context.getSystemService("power");
            sInstance.mHandler = new Handler() {
            };
            beginAnimationTime = 0L;
            boolean mShutOffAnimation = configShutdownAnimation(context);
            int screenTurnOffTime = getScreenTurnOffTime(context);
            synchronized (mEnableAnimatingSync) {
                if (mEnableAnimating) {
                    if (mShutOffAnimation) {
                        Log.d(TAG, "mIBootAnim.isCustBootAnim() is true");
                        bootanimCust(context);
                    } else {
                        pd.show();
                        sInstance.mProgressDialog = pd;
                    }
                    sInstance.mHandler.postDelayed(mDelayDim, screenTurnOffTime);
                }
            }
            sInstance.mCpuWakeLock = null;
            try {
                sInstance.mCpuWakeLock = sInstance.mPowerManager.newWakeLock(1, "ShutdownThread-cpu");
                sInstance.mCpuWakeLock.setReferenceCounted(false);
                sInstance.mCpuWakeLock.acquire();
            } catch (SecurityException e) {
                Log.w(TAG, "No permission to acquire wake lock", e);
                sInstance.mCpuWakeLock = null;
            }
            sInstance.mScreenWakeLock = null;
            if (sInstance.mPowerManager.isScreenOn()) {
                try {
                    sInstance.mScreenWakeLock = sInstance.mPowerManager.newWakeLock(26, "ShutdownThread-screen");
                    sInstance.mScreenWakeLock.setReferenceCounted(false);
                    sInstance.mScreenWakeLock.acquire();
                } catch (SecurityException e2) {
                    Log.w(TAG, "No permission to acquire wake lock", e2);
                    sInstance.mScreenWakeLock = null;
                }
            }
            if (sInstance.getState() != Thread.State.NEW || sInstance.isAlive()) {
                if (sInstance.mShutdownFlow == 1) {
                    Log.d(TAG, "ShutdownThread exists already");
                    checkShutdownFlow();
                    synchronized (mShutdownThreadSync) {
                        mShutdownThreadSync.notify();
                    }
                    return;
                }
                Log.e(TAG, "Thread state is not normal! froce to shutdown!");
                delayForPlayAnimation();
                sInstance.mPowerManager.goToSleep(SystemClock.uptimeMillis(), 7, 0);
                PowerManagerService.lowLevelShutdown(mReason);
                return;
            }
            sInstance.start();
        }
    }

    private static void bootanimCust(Context context) {
        SystemProperties.set("service.shutanim.running", "0");
        Log.i(TAG, "set service.shutanim.running to 0");
        try {
            boolean isRotaionEnabled = Settings.System.getInt(context.getContentResolver(), "accelerometer_rotation", 1) != 0;
            if (isRotaionEnabled) {
                IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
                if (wm != null) {
                    wm.freezeRotation(0);
                }
                Settings.System.putInt(context.getContentResolver(), "accelerometer_rotation", 0);
                Settings.System.putInt(context.getContentResolver(), "accelerometer_rotation_restore", 1);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NullPointerException e2) {
            Log.e(TAG, "check Rotation: context object is null when get Rotation");
        }
        beginAnimationTime = SystemClock.elapsedRealtime() + DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC;
        try {
            IWindowManager wm2 = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            if (wm2 != null) {
                wm2.setEventDispatching(false);
            }
        } catch (RemoteException e3) {
            e3.printStackTrace();
        }
        startBootAnimation();
    }

    private static void startBootAnimation() {
        Log.d(TAG, "Set 'service.bootanim.exit' = 0).");
        SystemProperties.set("service.bootanim.exit", "0");
        if (bPlayaudio) {
            SystemProperties.set("ctl.start", "banim_shutmp3");
            Log.d(TAG, "bootanim:shut mp3");
        } else {
            SystemProperties.set("ctl.start", "banim_shutnomp3");
            Log.d(TAG, "bootanim:shut nomp3");
        }
    }

    void actionDone() {
        synchronized (this.mActionDoneSync) {
            this.mActionDone = true;
            this.mActionDoneSync.notifyAll();
        }
    }

    private static void delayForPlayAnimation() {
        if (beginAnimationTime <= 0) {
            return;
        }
        endAnimationTime = beginAnimationTime - SystemClock.elapsedRealtime();
        if (endAnimationTime <= 0) {
            return;
        }
        try {
            Thread.currentThread();
            Thread.sleep(endAnimationTime);
        } catch (InterruptedException e) {
            Log.e(TAG, "Shutdown stop bootanimation Thread.currentThread().sleep exception!");
        }
    }

    private static void checkShutdownFlow() {
        String IPODisableProp = SystemProperties.get("sys.ipo.disable");
        boolean isIPOEnabled = !IPODisableProp.equals("1");
        boolean isIPOsupport = SystemProperties.get("ro.mtk_ipo_support").equals("1");
        boolean passIPOEncryptionCondition = checkEncryption();
        boolean isSafeMode = false;
        try {
            IWindowManager wm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
            if (wm != null) {
                isSafeMode = wm.isSafeModeEnabled();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "checkShutdownFlow: IPO_Support=" + isIPOsupport + " mReboot=" + mReboot + " sys.ipo.disable=" + IPODisableProp + " isSafeMode=" + isSafeMode + " passEncryptionCondition=" + passIPOEncryptionCondition);
        if (!isIPOsupport || mReboot || !isIPOEnabled || isSafeMode || !passIPOEncryptionCondition) {
            sInstance.mShutdownFlow = 0;
            return;
        }
        try {
            boolean isIPOEnabled2 = Settings.System.getInt(sInstance.mContext.getContentResolver(), "ipo_setting", 1) == 1;
            if (!isIPOEnabled2 || "1".equals(SystemProperties.get("sys.ipo.battlow"))) {
                sInstance.mShutdownFlow = 0;
            } else {
                sInstance.mShutdownFlow = 1;
            }
            Log.d(TAG, "checkShutdownFlow: isIPOEnabled=" + isIPOEnabled2 + " mShutdownFlow=" + sInstance.mShutdownFlow);
        } catch (NullPointerException e2) {
            Log.e(TAG, "checkShutdownFlow: fail to get IPO setting");
            sInstance.mShutdownFlow = 0;
        }
    }

    private void switchToLauncher() {
        Log.i(TAG, "set launcher as foreground");
        Intent intent1 = new Intent("android.intent.action.MAIN");
        intent1.addCategory("android.intent.category.HOME");
        intent1.setFlags(268435456);
        this.mContext.startActivity(intent1);
    }

    @Override
    public void run() {
        checkShutdownFlow();
        while (this.mShutdownFlow == 1) {
            this.mShutdownManager.saveStates(this.mContext);
            this.mShutdownManager.enterShutdown(this.mContext);
            switchToLauncher();
            running();
        }
        if (this.mShutdownFlow == 1) {
            return;
        }
        this.mShutdownManager.enterShutdown(this.mContext);
        switchToLauncher();
        running();
    }

    private void running() {
        com.android.server.power.ShutdownThread.command = android.os.SystemProperties.get("sys.ipo.pwrdncap");
        r6 = new android.content.BroadcastReceiver() {
            {
            }

            @Override
            public void onReceive(android.content.Context r2, android.content.Intent r3) {
                com.android.server.power.ShutdownThread.this.actionDone();
            }
        };
        r4 = new java.lang.StringBuilder();
        if (com.android.server.power.ShutdownThread.mReboot) {
            r2 = "1";
        } else {
            r2 = "0";
        }
        r4 = r4.append(r2);
        if (com.android.server.power.ShutdownThread.mReason != null) {
            r2 = com.android.server.power.ShutdownThread.mReason;
        } else {
            r2 = "";
        }
        r30 = r4.append(r2).toString();
        android.os.SystemProperties.set(com.android.server.power.ShutdownThread.SHUTDOWN_ACTION_PROPERTY, r30);
        if (com.android.server.power.ShutdownThread.mRebootSafeMode) {
            android.os.SystemProperties.set(com.android.server.power.ShutdownThread.REBOOT_SAFEMODE_PROPERTY, "1");
        }
        android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Sending shutdown broadcast...");
        r33.mActionDone = false;
        r33.mContext.sendBroadcast(new android.content.Intent(com.android.server.power.ShutdownThread.ACTION_PRE_SHUTDOWN));
        r3 = new android.content.Intent("android.intent.action.ACTION_SHUTDOWN");
        r3.putExtra("_mode", r33.mShutdownFlow);
        r3.addFlags(268435456);
        r33.mContext.sendOrderedBroadcastAsUser(r3, android.os.UserHandle.ALL, null, r6, r33.mHandler, 0, null, null);
        r24 = android.os.SystemClock.elapsedRealtime() + com.android.server.job.controllers.JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        r4 = r33.mActionDoneSync;
        synchronized (r4) {
            ;
            while (true) {
                if (!r33.mActionDone) {
                    r16 = r24 - android.os.SystemClock.elapsedRealtime();
                    if ((r16 > 0 ? 1 : (r16 == 0 ? 0 : -1)) <= 0) {
                    } else {
                        if (com.android.server.power.ShutdownThread.mRebootHasProgressBar) {
                            r0 = (int) (((((double) (com.android.server.job.controllers.JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY - r16)) * 1.0d) * 2.0d) / 10000.0d);
                            com.android.server.power.ShutdownThread.sInstance.setRebootProgress(r0, null);
                        }
                        r33.mActionDoneSync.wait(java.lang.Math.min(r16, 500));
                    }
                }
            }
        }
        if (com.android.server.power.ShutdownThread.mRebootHasProgressBar) {
            com.android.server.power.ShutdownThread.sInstance.setRebootProgress(2, null);
        }
        if (r33.mShutdownFlow == 1) {
            r33.mActionDone = false;
            r3 = new android.content.Intent("android.intent.action.ACTION_SHUTDOWN_IPO");
            r3.addFlags(268435456);
            r33.mContext.sendOrderedBroadcast(r3, null, r6, r33.mHandler, 0, null, null);
            r26 = android.os.SystemClock.elapsedRealtime() + com.android.server.job.controllers.JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
            r4 = r33.mActionDoneSync;
            synchronized (r4) {
                ;
                while (true) {
                    if (!r33.mActionDone) {
                        r16 = r26 - android.os.SystemClock.elapsedRealtime();
                        if ((r16 > 0 ? 1 : (r16 == 0 ? 0 : -1)) <= 0) {
                        } else {
                            r33.mActionDoneSync.wait(r16);
                        }
                    }
                }
            }
        }
        if (r33.mShutdownFlow != 1) {
            android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Shutting down activity manager...");
            r15 = android.app.ActivityManagerNative.asInterface(android.os.ServiceManager.checkService("activity"));
            if (r15 != null) {
                r15.shutdown(10000);
            }
            if (com.android.server.power.ShutdownThread.mRebootHasProgressBar) {
                com.android.server.power.ShutdownThread.sInstance.setRebootProgress(4, null);
            }
        }
        android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Shutting down package manager...");
        r29 = (com.android.server.pm.PackageManagerService) android.os.ServiceManager.getService("package");
        if (r29 != null) {
            r29.shutdown();
        }
        if (com.android.server.power.ShutdownThread.mRebootHasProgressBar) {
            com.android.server.power.ShutdownThread.sInstance.setRebootProgress(6, null);
        }
        android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Shutting down radios...");
        shutdownRadios(com.android.server.power.ShutdownThread.MAX_RADIO_WAIT_TIME);
        if (com.android.server.power.ShutdownThread.mRebootHasProgressBar) {
            com.android.server.power.ShutdownThread.sInstance.setRebootProgress(18, null);
        }
        if (r33.mShutdownFlow != 1 || (!com.android.server.power.ShutdownThread.command.equals("1") && !com.android.server.power.ShutdownThread.command.equals("3"))) {
            r28 = new android.os.storage.IMountShutdownObserver.Stub() {
                {
                }

                public void onShutDownComplete(int r5) throws android.os.RemoteException {
                    android.util.Log.w(com.android.server.power.ShutdownThread.TAG, "Result code " + r5 + " from MountService.shutdown");
                    if (r5 < 0) {
                        com.android.server.power.ShutdownThread.this.mShutdownFlow = 0;
                    }
                    com.android.server.power.ShutdownThread.this.actionDone();
                }
            };
            android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Shutting down MountService");
            r33.mActionDone = false;
            r22 = android.os.SystemClock.elapsedRealtime() + 20000;
            r4 = r33.mActionDoneSync;
            synchronized (r4) {
                ;
                r21 = android.os.storage.IMountService.Stub.asInterface(android.os.ServiceManager.checkService("mount"));
                if (r21 != null) {
                    r21.shutdown(r28);
                } else {
                    android.util.Log.w(com.android.server.power.ShutdownThread.TAG, "MountService unavailable for shutdown");
                }
                if (!r33.mActionDone) {
                    r16 = r22 - android.os.SystemClock.elapsedRealtime();
                    if ((r16 > 0 ? 1 : (r16 == 0 ? 0 : -1)) > 0) {
                        if (com.android.server.power.ShutdownThread.mRebootHasProgressBar) {
                            r0 = (int) (((((double) (20000 - r16)) * 1.0d) * 2.0d) / 20000.0d);
                            r2 = com.android.server.power.ShutdownThread.sInstance;
                            r2.setRebootProgress(r0 + 18, null);
                        }
                        r33.mActionDoneSync.wait(java.lang.Math.min(r16, 500));
                        if (!r33.mActionDone) {
                        }
                    } else {
                        android.util.Log.w(com.android.server.power.ShutdownThread.TAG, "Shutdown wait timed out");
                        if (r33.mShutdownFlow == 1) {
                            android.util.Log.d(com.android.server.power.ShutdownThread.TAG, "change shutdown flow from ipo to normal: MountService");
                            r33.mShutdownFlow = 0;
                        }
                    }
                }
            }
            android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "MountService shut done...");
        } else {
            android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "bypass MountService!");
        }
        android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "set service.shutanim.running to 1");
        android.os.SystemProperties.set("service.shutanim.running", "1");
        if (r33.mShutdownFlow != 1) {
            if (r33.mContext != null) {
                r32 = new android.os.SystemVibrator(r33.mContext);
                r32.vibrate(500, com.android.server.power.ShutdownThread.VIBRATION_ATTRIBUTES);
                java.lang.Thread.sleep(500);
            }
            android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Performing ipo low-level shutdown...");
            delayForPlayAnimation();
            if (com.android.server.power.ShutdownThread.sInstance.mScreenWakeLock != null && com.android.server.power.ShutdownThread.sInstance.mScreenWakeLock.isHeld()) {
                com.android.server.power.ShutdownThread.sInstance.mScreenWakeLock.release();
            }
            com.android.server.power.ShutdownThread.sInstance.mHandler.removeCallbacks(com.android.server.power.ShutdownThread.mDelayDim);
            r33.mShutdownManager.shutdown(r33.mContext);
            r33.mShutdownManager.finishShutdown(r33.mContext);
            if (com.android.server.power.ShutdownThread.sInstance.mProgressDialog != null) {
                com.android.server.power.ShutdownThread.sInstance.mProgressDialog.dismiss();
            } else {
                if ((com.android.server.power.ShutdownThread.beginAnimationTime > 0 ? 1 : (com.android.server.power.ShutdownThread.beginAnimationTime == 0 ? 0 : -1)) > 0) {
                    android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "service.bootanim.exit = 1");
                    android.os.SystemProperties.set("service.bootanim.exit", "1");
                }
            }
            r2 = com.android.server.power.ShutdownThread.sIsStartedGuard;
            synchronized (r2) {
                ;
                com.android.server.power.ShutdownThread.sIsStarted = false;
            }
            com.android.server.power.ShutdownThread.sInstance.mPowerManager.wakeUp(android.os.SystemClock.uptimeMillis(), "shutdown");
            com.android.server.power.ShutdownThread.sInstance.mCpuWakeLock.acquire(2000);
            r4 = com.android.server.power.ShutdownThread.mShutdownThreadSync;
            synchronized (r4) {
                ;
                com.android.server.power.ShutdownThread.mShutdownThreadSync.wait();
            }
            return;
        } else {
            if (com.android.server.power.ShutdownThread.mRebootHasProgressBar) {
                com.android.server.power.ShutdownThread.sInstance.setRebootProgress(20, null);
                uncrypt();
            }
            if ((com.android.server.power.ShutdownThread.mReboot && com.android.server.power.ShutdownThread.mReason != null && com.android.server.power.ShutdownThread.mReason.equals("recovery")) || !com.android.server.power.ShutdownThread.mReboot) {
                delayForPlayAnimation();
            }
            com.android.server.power.ShutdownThread.sInstance.mPowerManager.goToSleep(android.os.SystemClock.uptimeMillis(), 7, 0);
            rebootOrShutdown(r33.mContext, com.android.server.power.ShutdownThread.mReboot, com.android.server.power.ShutdownThread.mReason);
            return;
        }
    }

    private void setRebootProgress(final int progress, final CharSequence message) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (ShutdownThread.this.mProgressDialog == null) {
                    return;
                }
                ShutdownThread.this.mProgressDialog.setProgress(progress);
                if (message == null) {
                    return;
                }
                ShutdownThread.this.mProgressDialog.setMessage(message);
            }
        });
    }

    private void shutdownRadios(final int timeout) {
        final boolean bypassRadioOff;
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (!cm.isNetworkSupported(0)) {
            bypassRadioOff = true;
        } else if (this.mShutdownFlow == 1) {
            bypassRadioOff = !command.equals("2") ? command.equals("3") : true;
        } else {
            bypassRadioOff = false;
        }
        final long endTime = SystemClock.elapsedRealtime() + ((long) timeout);
        final boolean[] done = new boolean[1];
        Thread t = new Thread() {
            @Override
            public void run() {
                boolean nfcOff;
                boolean bluetoothOff;
                boolean radioOff;
                Log.w(ShutdownThread.TAG, "task run");
                INfcAdapter nfc = INfcAdapter.Stub.asInterface(ServiceManager.checkService("nfc"));
                ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                IBluetoothManager bluetooth = IBluetoothManager.Stub.asInterface(ServiceManager.checkService("bluetooth_manager"));
                if (nfc == null) {
                    nfcOff = true;
                } else {
                    try {
                        nfcOff = nfc.getState() == 1;
                    } catch (RemoteException ex) {
                        Log.e(ShutdownThread.TAG, "RemoteException during NFC shutdown", ex);
                        nfcOff = true;
                    }
                }
                if (!nfcOff) {
                    Log.w(ShutdownThread.TAG, "Turning off NFC...");
                    nfc.disable(false);
                }
                if (bluetooth != null) {
                    try {
                        bluetoothOff = !bluetooth.isEnabled();
                        if (!bluetoothOff) {
                            Log.w(ShutdownThread.TAG, "Disabling Bluetooth...");
                            bluetooth.disable(false);
                        }
                    } catch (RemoteException ex2) {
                        Log.e(ShutdownThread.TAG, "RemoteException during bluetooth shutdown", ex2);
                        bluetoothOff = true;
                    }
                }
                if (phone != null) {
                    try {
                        radioOff = !phone.needMobileRadioShutdown();
                        if (!radioOff && ShutdownThread.this.mShutdownFlow != 1) {
                            Log.w(ShutdownThread.TAG, "Turning off cellular radios...");
                            phone.shutdownMobileRadios();
                        }
                    } catch (RemoteException ex3) {
                        Log.e(ShutdownThread.TAG, "RemoteException during radio shutdown", ex3);
                        radioOff = true;
                    }
                }
                Log.i(ShutdownThread.TAG, "Waiting for NFC, Bluetooth and Radio...");
                long delay = endTime - SystemClock.elapsedRealtime();
                if (!bypassRadioOff) {
                    while (delay > 0) {
                        if (ShutdownThread.mRebootHasProgressBar) {
                            int status = (int) ((((((long) timeout) - delay) * 1.0d) * 12.0d) / ((double) timeout));
                            ShutdownThread.sInstance.setRebootProgress(status + 6, null);
                        }
                        if (!bluetoothOff) {
                            try {
                                bluetoothOff = !bluetooth.isEnabled();
                            } catch (RemoteException ex4) {
                                Log.e(ShutdownThread.TAG, "RemoteException during bluetooth shutdown", ex4);
                                bluetoothOff = true;
                            }
                            if (bluetoothOff) {
                                Log.i(ShutdownThread.TAG, "Bluetooth turned off.");
                            }
                        }
                        if (!radioOff) {
                            try {
                                radioOff = !phone.needMobileRadioShutdown();
                            } catch (RemoteException ex5) {
                                Log.e(ShutdownThread.TAG, "RemoteException during radio shutdown", ex5);
                                radioOff = true;
                            }
                            if (radioOff) {
                                Log.i(ShutdownThread.TAG, "Radio turned off.");
                            }
                        }
                        if (!nfcOff) {
                            try {
                                nfcOff = nfc.getState() == 1;
                            } catch (RemoteException ex6) {
                                Log.e(ShutdownThread.TAG, "RemoteException during NFC shutdown", ex6);
                                nfcOff = true;
                            }
                            if (nfcOff) {
                                Log.i(ShutdownThread.TAG, "NFC turned off.");
                            }
                        }
                        if (radioOff && bluetoothOff && nfcOff) {
                            Log.i(ShutdownThread.TAG, "NFC, Radio and Bluetooth shutdown complete.");
                            done[0] = true;
                            return;
                        } else {
                            SystemClock.sleep(500L);
                            delay = endTime - SystemClock.elapsedRealtime();
                        }
                    }
                    return;
                }
                done[0] = true;
                Log.i(ShutdownThread.TAG, "bypass RadioOff!");
            }
        };
        t.start();
        try {
            t.join(timeout);
        } catch (InterruptedException e) {
        }
        if (done[0]) {
            return;
        }
        Log.w(TAG, "Timed out waiting for NFC, Radio and Bluetooth shutdown.");
        if (this.mShutdownFlow != 1) {
            return;
        }
        Log.d(TAG, "change shutdown flow from ipo to normal: BT/MD");
        this.mShutdownFlow = 0;
    }

    public static void rebootOrShutdown(Context context, boolean reboot, String reason) {
        if (reboot) {
            Log.i(TAG, "Rebooting, reason: " + reason);
            PowerManagerService.lowLevelReboot(reason);
            Log.e(TAG, "Reboot failed, will attempt shutdown instead");
            reason = null;
        } else if (context != null) {
            try {
                new SystemVibrator(context).vibrate(500L, VIBRATION_ATTRIBUTES);
            } catch (Exception e) {
                Log.w(TAG, "Failed to vibrate during shutdown.", e);
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e2) {
            }
        }
        Log.i(TAG, "Performing low-level shutdown...");
        PowerManagerService.lowLevelShutdown(reason);
    }

    private static boolean checkEncryption() {
        String encryptionProgress = SystemProperties.get("vold.encrypt_progress");
        String state = SystemProperties.get("ro.crypto.state");
        String cryptoType = SystemProperties.get("ro.crypto.type");
        int passwordQuality = new LockPatternUtils(sInstance.mContext).getKeyguardStoredPasswordQuality(ActivityManager.getCurrentUser());
        if (!encryptionProgress.equals("100") && !encryptionProgress.equals("")) {
            Log.e(TAG, "encryption in progress");
            return false;
        }
        if (!state.equals("encrypted")) {
            Log.d(TAG, "ro.crypto.state: " + state);
            return true;
        }
        if (cryptoType.equals("file")) {
            Log.d(TAG, "FBE: PasswordQuality:" + passwordQuality);
            return passwordQuality == 0;
        }
        if (cryptoType.equals("block")) {
            try {
                IMountService service = IMountService.Stub.asInterface(ServiceManager.checkService("mount"));
                if (service != null) {
                    int type = service.getPasswordType();
                    Log.d(TAG, "FDE: phone encrypted type: " + type);
                    return type == 1;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error calling mount service " + e);
            }
        }
        return false;
    }

    private void uncrypt() {
        Log.i(TAG, "Calling uncrypt and monitoring the progress...");
        final RecoverySystem.ProgressListener progressListener = new RecoverySystem.ProgressListener() {
            @Override
            public void onProgress(int status) {
                if (status >= 0 && status < 100) {
                    CharSequence msg = ShutdownThread.this.mContext.getText(R.string.accessibility_system_action_media_play_pause_label);
                    ShutdownThread.sInstance.setRebootProgress(((int) ((((double) status) * 80.0d) / 100.0d)) + 20, msg);
                } else {
                    if (status != 100) {
                        return;
                    }
                    CharSequence msg2 = ShutdownThread.this.mContext.getText(R.string.accessibility_system_action_menu_label);
                    ShutdownThread.sInstance.setRebootProgress(status, msg2);
                }
            }
        };
        final boolean[] done = {false};
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    String filename = FileUtils.readTextFile(RecoverySystem.UNCRYPT_PACKAGE_FILE, 0, null);
                    RecoverySystem.processPackage(ShutdownThread.this.mContext, new File(filename), progressListener);
                } catch (IOException e) {
                    Log.e(ShutdownThread.TAG, "Error uncrypting file", e);
                }
                done[0] = true;
            }
        };
        t.start();
        try {
            t.join(900000L);
        } catch (InterruptedException e) {
        }
        if (done[0]) {
            return;
        }
        Log.w(TAG, "Timed out waiting for uncrypt.");
    }
}
