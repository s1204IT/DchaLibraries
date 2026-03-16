package com.android.server.power;

import android.R;
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
import android.nfc.INfcAdapter;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemVibrator;
import android.util.Log;
import com.android.internal.telephony.ITelephony;

public final class ShutdownThread extends Thread {
    private static final int MAX_BROADCAST_TIME = 10000;
    private static final int MAX_RADIO_WAIT_TIME = 12000;
    private static final int MAX_SHUTDOWN_WAIT_TIME = 20000;
    private static final int PHONE_STATE_POLL_SLEEP_MSEC = 500;
    public static final String REBOOT_SAFEMODE_PROPERTY = "persist.sys.safemode";
    public static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";
    private static final int SHUTDOWN_VIBRATE_MS = 500;
    private static final String TAG = "ShutdownThread";
    private static boolean mReboot;
    private static String mRebootReason;
    private static boolean mRebootSafeMode;
    private static AlertDialog sConfirmDialog;
    private boolean mActionDone;
    private final Object mActionDoneSync = new Object();
    private Context mContext;
    private PowerManager.WakeLock mCpuWakeLock;
    private Handler mHandler;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mScreenWakeLock;
    private static Object sIsStartedGuard = new Object();
    private static boolean sIsStarted = false;
    private static final ShutdownThread sInstance = new ShutdownThread();
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();

    private ShutdownThread() {
    }

    public static void shutdown(Context context, boolean confirm) {
        mReboot = false;
        mRebootSafeMode = false;
        shutdownInner(context, confirm);
    }

    static void shutdownInner(final Context context, boolean confirm) {
        int resourceId;
        int i = R.string.accessibility_service_action_perform_description;
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Request to shutdown already running, returning.");
                return;
            }
            int longPressBehavior = context.getResources().getInteger(R.integer.config_burnInProtectionMinVerticalOffset);
            if (mRebootSafeMode) {
                resourceId = R.string.accessibility_service_warning_description;
            } else {
                resourceId = longPressBehavior == 2 ? R.string.accessibility_service_screen_control_description : mReboot ? 17039612 : 17039613;
            }
            Log.d(TAG, "Notifying thread to start shutdown longPressBehavior=" + longPressBehavior);
            if (confirm) {
                CloseDialogReceiver closer = new CloseDialogReceiver(context);
                if (sConfirmDialog != null) {
                    sConfirmDialog.dismiss();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                if (mRebootSafeMode) {
                    i = R.string.accessibility_service_screen_control_title;
                } else if (!mReboot) {
                    i = 17039613;
                }
                sConfirmDialog = builder.setTitle(i).setMessage(resourceId).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ShutdownThread.beginShutdownSequence(context);
                    }
                }).setNegativeButton(R.string.no, (DialogInterface.OnClickListener) null).create();
                closer.dialog = sConfirmDialog;
                sConfirmDialog.setOnDismissListener(closer);
                sConfirmDialog.getWindow().setType(2009);
                sConfirmDialog.show();
                return;
            }
            beginShutdownSequence(context);
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
        mRebootReason = reason;
        shutdownInner(context, confirm);
    }

    public static void rebootSafeMode(Context context, boolean confirm) {
        mReboot = true;
        mRebootSafeMode = true;
        mRebootReason = null;
        shutdownInner(context, confirm);
    }

    private static void beginShutdownSequence(Context context) {
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Shutdown sequence already running, returning.");
                return;
            }
            sIsStarted = true;
            ProgressDialog pd = new ProgressDialog(context);
            pd.setTitle(context.getText(R.string.accessibility_label_communal_profile));
            pd.setMessage(context.getText(R.string.accessibility_select_shortcut_menu_title));
            pd.setIndeterminate(true);
            pd.setCancelable(false);
            pd.getWindow().setType(2009);
            pd.show();
            sInstance.mContext = context;
            sInstance.mPowerManager = (PowerManager) context.getSystemService("power");
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
            sInstance.mHandler = new Handler() {
            };
            sInstance.start();
        }
    }

    void actionDone() {
        synchronized (this.mActionDoneSync) {
            this.mActionDone = true;
            this.mActionDoneSync.notifyAll();
        }
    }

    @Override
    public void run() {
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
        if (com.android.server.power.ShutdownThread.mRebootReason != null) {
            r2 = com.android.server.power.ShutdownThread.mRebootReason;
        } else {
            r2 = "";
        }
        r22 = r4.append(r2).toString();
        android.os.SystemProperties.set(com.android.server.power.ShutdownThread.SHUTDOWN_ACTION_PROPERTY, r22);
        if (com.android.server.power.ShutdownThread.mRebootSafeMode) {
            android.os.SystemProperties.set(com.android.server.power.ShutdownThread.REBOOT_SAFEMODE_PROPERTY, "1");
        }
        android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Sending shutdown broadcast...");
        r23.mActionDone = false;
        r3 = new android.content.Intent("android.intent.action.ACTION_SHUTDOWN");
        r3.addFlags(268435456);
        r23.mContext.sendOrderedBroadcastAsUser(r3, android.os.UserHandle.ALL, null, r6, r23.mHandler, 0, null, null);
        r18 = android.os.SystemClock.elapsedRealtime() + 10000;
        r4 = r23.mActionDoneSync;
        synchronized (r4) {
            ;
            while (true) {
                if (!r23.mActionDone) {
                    r12 = r18 - android.os.SystemClock.elapsedRealtime();
                    if ((r12 > 0 ? 1 : (r12 == 0 ? 0 : -1)) <= 0) {
                        android.util.Log.w(com.android.server.power.ShutdownThread.TAG, "Shutdown broadcast timed out");
                    } else {
                        r23.mActionDoneSync.wait(r12);
                    }
                }
            }
        }
        android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Shutting down activity manager...");
        r11 = android.app.ActivityManagerNative.asInterface(android.os.ServiceManager.checkService("activity"));
        if (r11 != null) {
            r11.shutdown(10000);
        }
        android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Shutting down package manager...");
        r21 = (com.android.server.pm.PackageManagerService) android.os.ServiceManager.getService("package");
        if (r21 != null) {
            r21.shutdown();
        }
        shutdownRadios(com.android.server.power.ShutdownThread.MAX_RADIO_WAIT_TIME);
        r20 = new android.os.storage.IMountShutdownObserver.Stub() {
            {
            }

            public void onShutDownComplete(int r4) throws android.os.RemoteException {
                android.util.Log.w(com.android.server.power.ShutdownThread.TAG, "Result code " + r4 + " from MountService.shutdown");
                com.android.server.power.ShutdownThread.this.actionDone();
            }
        };
        android.util.Log.i(com.android.server.power.ShutdownThread.TAG, "Shutting down MountService");
        r23.mActionDone = false;
        r16 = android.os.SystemClock.elapsedRealtime() + 20000;
        r4 = r23.mActionDoneSync;
        synchronized (r4) {
            ;
            r15 = android.os.storage.IMountService.Stub.asInterface(android.os.ServiceManager.checkService("mount"));
            if (r15 != null) {
                r15.shutdown(r20);
            } else {
                android.util.Log.w(com.android.server.power.ShutdownThread.TAG, "MountService unavailable for shutdown");
            }
            if (!r23.mActionDone) {
                r12 = r16 - android.os.SystemClock.elapsedRealtime();
                if ((r12 > 0 ? 1 : (r12 == 0 ? 0 : -1)) <= 0) {
                    android.util.Log.w(com.android.server.power.ShutdownThread.TAG, "Shutdown wait timed out");
                } else {
                    r23.mActionDoneSync.wait(r12);
                    if (!r23.mActionDone) {
                    }
                }
            }
        }
        android.media.AudioSystem.setParameters("power_off=on");
        rebootOrShutdown(com.android.server.power.ShutdownThread.mReboot, com.android.server.power.ShutdownThread.mRebootReason);
        return;
    }

    private void shutdownRadios(int timeout) {
        final long endTime = SystemClock.elapsedRealtime() + ((long) timeout);
        final boolean[] done = new boolean[1];
        Thread t = new Thread() {
            @Override
            public void run() {
                boolean nfcOff;
                boolean bluetoothOff;
                boolean radioOff;
                INfcAdapter nfc = INfcAdapter.Stub.asInterface(ServiceManager.checkService("nfc"));
                ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                IBluetoothManager bluetooth = IBluetoothManager.Stub.asInterface(ServiceManager.checkService("bluetooth_manager"));
                if (nfc != null) {
                    try {
                        nfcOff = nfc.getState() == 1;
                        if (!nfcOff) {
                            Log.w(ShutdownThread.TAG, "Turning off NFC...");
                            nfc.disable(false);
                        }
                    } catch (RemoteException ex) {
                        Log.e(ShutdownThread.TAG, "RemoteException during NFC shutdown", ex);
                        nfcOff = true;
                    }
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
                        if (!radioOff) {
                            Log.w(ShutdownThread.TAG, "Turning off cellular radios...");
                            phone.shutdownMobileRadios();
                        }
                    } catch (RemoteException ex3) {
                        Log.e(ShutdownThread.TAG, "RemoteException during radio shutdown", ex3);
                        radioOff = true;
                    }
                }
                Log.i(ShutdownThread.TAG, "Waiting for NFC, Bluetooth and Radio...");
                while (SystemClock.elapsedRealtime() < endTime) {
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
                    }
                    SystemClock.sleep(500L);
                }
            }
        };
        t.start();
        try {
            t.join(timeout);
        } catch (InterruptedException e) {
        }
        if (!done[0]) {
            Log.w(TAG, "Timed out waiting for NFC, Radio and Bluetooth shutdown.");
        }
    }

    public static void rebootOrShutdown(boolean reboot, String reason) {
        if (reboot) {
            Log.i(TAG, "Rebooting, reason: " + reason);
            PowerManagerService.lowLevelReboot(reason);
            Log.e(TAG, "Reboot failed, will attempt shutdown instead");
        } else {
            try {
                new SystemVibrator().vibrate(500L, VIBRATION_ATTRIBUTES);
            } catch (Exception e) {
                Log.w(TAG, "Failed to vibrate during shutdown.", e);
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e2) {
            }
        }
        Log.i(TAG, "Performing low-level shutdown...");
        PowerManagerService.lowLevelShutdown();
    }
}
