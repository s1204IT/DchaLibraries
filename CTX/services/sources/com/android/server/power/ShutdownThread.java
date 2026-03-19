package com.android.server.power;

import android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.app.admin.SecurityLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.os.FileUtils;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TimingsTraceLog;
import com.android.internal.telephony.ITelephony;
import com.android.server.LocalServices;
import com.android.server.RescueParty;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.job.controllers.JobStatus;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.mediatek.server.MtkSystemServiceFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ShutdownThread extends Thread {
    private static final int ACTION_DONE_POLL_WAIT_MS = 500;
    private static final int ACTIVITY_MANAGER_STOP_PERCENT = 4;
    private static final int BROADCAST_STOP_PERCENT = 2;
    private static final int MAX_BROADCAST_TIME = 10000;
    private static final int MAX_RADIO_WAIT_TIME = 12000;
    private static final int MAX_SHUTDOWN_WAIT_TIME = 20000;
    private static final int MAX_UNCRYPT_WAIT_TIME = 900000;
    private static final String METRICS_FILE_BASENAME = "/data/system/shutdown-metrics";
    private static final int MOUNT_SERVICE_STOP_PERCENT = 20;
    private static final int PACKAGE_MANAGER_STOP_PERCENT = 6;
    private static final int RADIOS_STATE_POLL_SLEEP_MS = 100;
    private static final int RADIO_STOP_PERCENT = 18;
    public static final String REBOOT_SAFEMODE_PROPERTY = "persist.sys.safemode";
    public static final String RO_SAFEMODE_PROPERTY = "ro.sys.safemode";
    public static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";
    private static final int SHUTDOWN_VIBRATE_MS = 500;
    private static final String TAG = "ShutdownThread";
    protected static String mReason;
    protected static boolean mReboot;
    protected static boolean mRebootHasProgressBar;
    protected static boolean mRebootSafeMode;
    private static AlertDialog sConfirmDialog;
    private boolean mActionDone;
    private final Object mActionDoneSync = new Object();
    protected Context mContext;
    private PowerManager.WakeLock mCpuWakeLock;
    protected Handler mHandler;
    protected PowerManager mPowerManager;
    private ProgressDialog mProgressDialog;
    protected PowerManager.WakeLock mScreenWakeLock;
    private static final Object sIsStartedGuard = new Object();
    private static boolean sIsStarted = false;
    protected static final ShutdownThread sInstance = MtkSystemServiceFactory.getInstance().makeMtkShutdownThread();
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private static final ArrayMap<String, Long> TRON_METRICS = new ArrayMap<>();
    private static String METRIC_SYSTEM_SERVER = "shutdown_system_server";
    private static String METRIC_SEND_BROADCAST = "shutdown_send_shutdown_broadcast";
    private static String METRIC_AM = "shutdown_activity_manager";
    private static String METRIC_PM = "shutdown_package_manager";
    private static String METRIC_RADIOS = "shutdown_radios";
    private static String METRIC_RADIO = "shutdown_radio";
    private static String METRIC_SHUTDOWN_TIME_START = "begin_shutdown";

    public static void shutdown(Context context, String str, boolean z) {
        mReboot = false;
        mRebootSafeMode = false;
        mReason = str;
        shutdownInner(context, z);
    }

    private static void shutdownInner(final Context context, boolean z) {
        int i;
        int i2;
        context.assertRuntimeOverlayThemable();
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Request to shutdown already running, returning.");
                return;
            }
            int integer = context.getResources().getInteger(R.integer.config_defaultAlarmVibrationIntensity);
            if (mRebootSafeMode) {
                i = R.string.keyguard_password_enter_puk_code;
            } else if (integer == 2) {
                i = R.string.media_route_chooser_extended_settings;
            } else {
                i = R.string.media_route_button_content_description;
            }
            Log.d(TAG, "Notifying thread to start shutdown longPressBehavior=" + integer);
            if (z) {
                CloseDialogReceiver closeDialogReceiver = new CloseDialogReceiver(context);
                if (sConfirmDialog != null) {
                    sConfirmDialog.dismiss();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                if (mRebootSafeMode) {
                    i2 = R.string.keyguard_password_enter_puk_prompt;
                } else {
                    i2 = R.string.keyguard_accessibility_slide_unlock;
                }
                sConfirmDialog = builder.setTitle(i2).setMessage(i).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i3) {
                        ShutdownThread.beginShutdownSequence(context);
                    }
                }).setNegativeButton(R.string.no, (DialogInterface.OnClickListener) null).create();
                closeDialogReceiver.dialog = sConfirmDialog;
                sConfirmDialog.setOnDismissListener(closeDialogReceiver);
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
            context.registerReceiver(this, new IntentFilter("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            this.dialog.cancel();
        }

        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            this.mContext.unregisterReceiver(this);
        }
    }

    public static void reboot(Context context, String str, boolean z) {
        mReboot = true;
        mRebootSafeMode = false;
        mRebootHasProgressBar = false;
        mReason = str;
        shutdownInner(context, z);
    }

    public static void rebootSafeMode(Context context, boolean z) {
        if (((UserManager) context.getSystemService("user")).hasUserRestriction("no_safe_boot")) {
            return;
        }
        mReboot = true;
        mRebootSafeMode = true;
        mRebootHasProgressBar = false;
        mReason = null;
        shutdownInner(context, z);
    }

    private static ProgressDialog showShutdownDialog(Context context) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        if (mReason != null && mReason.startsWith("recovery-update")) {
            mRebootHasProgressBar = RecoverySystem.UNCRYPT_PACKAGE_FILE.exists() && !RecoverySystem.BLOCK_MAP_FILE.exists();
            progressDialog.setTitle(context.getText(R.string.kg_failed_attempts_now_wiping));
            if (mRebootHasProgressBar) {
                progressDialog.setMax(100);
                progressDialog.setProgress(0);
                progressDialog.setIndeterminate(false);
                progressDialog.setProgressNumberFormat(null);
                progressDialog.setProgressStyle(1);
                progressDialog.setMessage(context.getText(R.string.kg_failed_attempts_almost_at_login));
            } else {
                if (showSysuiReboot()) {
                    return null;
                }
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage(context.getText(R.string.kg_failed_attempts_almost_at_wipe));
            }
        } else if (mReason != null && mReason.equals("recovery")) {
            if (RescueParty.isAttemptingFactoryReset()) {
                progressDialog.setTitle(context.getText(R.string.keyguard_accessibility_slide_unlock));
                progressDialog.setMessage(context.getText(R.string.media_route_chooser_searching));
                progressDialog.setIndeterminate(true);
            } else {
                progressDialog.setTitle(context.getText(R.string.keyguard_password_wrong_pin_code));
                progressDialog.setMessage(context.getText(R.string.keyguard_password_entry_touch_hint));
                progressDialog.setIndeterminate(true);
            }
        } else {
            if (showSysuiReboot()) {
                return null;
            }
            progressDialog.setTitle(context.getText(R.string.keyguard_accessibility_slide_unlock));
            progressDialog.setMessage(context.getText(R.string.media_route_chooser_searching));
            progressDialog.setIndeterminate(true);
        }
        progressDialog.setCancelable(false);
        progressDialog.getWindow().setType(2009);
        if (sInstance.mIsShowShutdownDialog(context)) {
            progressDialog.show();
        }
        return progressDialog;
    }

    private static boolean showSysuiReboot() {
        if (!sInstance.mIsShowShutdownSysui()) {
            return false;
        }
        Log.d(TAG, "Attempting to use SysUI shutdown UI");
        try {
            if (((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).showShutdownUi(mReboot, mReason)) {
                Log.d(TAG, "SysUI handling shutdown UI");
                return true;
            }
        } catch (Exception e) {
        }
        Log.d(TAG, "SysUI is unavailable");
        return false;
    }

    private static void beginShutdownSequence(Context context) {
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Shutdown sequence already running, returning.");
                return;
            }
            sIsStarted = true;
            sInstance.mProgressDialog = showShutdownDialog(context);
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
            if (SecurityLog.isLoggingEnabled()) {
                SecurityLog.writeEvent(210010, new Object[0]);
            }
            sInstance.mHandler = new Handler() {
            };
            if (sInstance.mStartShutdownSeq(context)) {
                sInstance.start();
            }
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
        TimingsTraceLog timingsTraceLogNewTimingsLog = newTimingsLog();
        timingsTraceLogNewTimingsLog.traceBegin("SystemServerShutdown");
        metricShutdownStart();
        metricStarted(METRIC_SYSTEM_SERVER);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ShutdownThread.this.actionDone();
            }
        };
        StringBuilder sb = new StringBuilder();
        sb.append(mReboot ? "1" : "0");
        sb.append(mReason != null ? mReason : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        SystemProperties.set(SHUTDOWN_ACTION_PROPERTY, sb.toString());
        if (mRebootSafeMode) {
            SystemProperties.set(REBOOT_SAFEMODE_PROPERTY, "1");
        }
        metricStarted(METRIC_SEND_BROADCAST);
        timingsTraceLogNewTimingsLog.traceBegin("SendShutdownBroadcast");
        Log.i(TAG, "Sending shutdown broadcast...");
        this.mActionDone = false;
        Intent intent = new Intent("android.intent.action.ACTION_SHUTDOWN");
        intent.addFlags(1342177280);
        this.mContext.sendOrderedBroadcastAsUser(intent, UserHandle.ALL, null, broadcastReceiver, this.mHandler, 0, null, null);
        long jElapsedRealtime = SystemClock.elapsedRealtime() + JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
        synchronized (this.mActionDoneSync) {
            while (true) {
                if (this.mActionDone) {
                    break;
                }
                long jElapsedRealtime2 = jElapsedRealtime - SystemClock.elapsedRealtime();
                if (jElapsedRealtime2 <= 0) {
                    Log.w(TAG, "Shutdown broadcast timed out");
                    break;
                } else {
                    if (mRebootHasProgressBar) {
                        sInstance.setRebootProgress((int) ((((JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY - jElapsedRealtime2) * 1.0d) * 2.0d) / 10000.0d), null);
                    }
                    try {
                        this.mActionDoneSync.wait(Math.min(jElapsedRealtime2, 500L));
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(2, null);
        }
        timingsTraceLogNewTimingsLog.traceEnd();
        metricEnded(METRIC_SEND_BROADCAST);
        Log.i(TAG, "Shutting down activity manager...");
        timingsTraceLogNewTimingsLog.traceBegin("ShutdownActivityManager");
        metricStarted(METRIC_AM);
        IActivityManager iActivityManagerAsInterface = IActivityManager.Stub.asInterface(ServiceManager.checkService("activity"));
        if (iActivityManagerAsInterface != null) {
            try {
                iActivityManagerAsInterface.shutdown(10000);
            } catch (RemoteException e2) {
            }
        }
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(4, null);
        }
        timingsTraceLogNewTimingsLog.traceEnd();
        metricEnded(METRIC_AM);
        Log.i(TAG, "Shutting down package manager...");
        timingsTraceLogNewTimingsLog.traceBegin("ShutdownPackageManager");
        metricStarted(METRIC_PM);
        PackageManagerService packageManagerService = (PackageManagerService) ServiceManager.getService(Settings.ATTR_PACKAGE);
        if (packageManagerService != null) {
            packageManagerService.shutdown();
        }
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(6, null);
        }
        timingsTraceLogNewTimingsLog.traceEnd();
        metricEnded(METRIC_PM);
        timingsTraceLogNewTimingsLog.traceBegin("ShutdownRadios");
        metricStarted(METRIC_RADIOS);
        shutdownRadios(MAX_RADIO_WAIT_TIME);
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(18, null);
        }
        timingsTraceLogNewTimingsLog.traceEnd();
        metricEnded(METRIC_RADIOS);
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(20, null);
            uncrypt();
        }
        mShutdownSeqFinish(this.mContext);
        timingsTraceLogNewTimingsLog.traceEnd();
        metricEnded(METRIC_SYSTEM_SERVER);
        saveMetrics(mReboot, mReason);
        rebootOrShutdown(this.mContext, mReboot, mReason);
    }

    private static TimingsTraceLog newTimingsLog() {
        return new TimingsTraceLog("ShutdownTiming", 524288L);
    }

    private static void metricStarted(String str) {
        synchronized (TRON_METRICS) {
            TRON_METRICS.put(str, Long.valueOf((-1) * SystemClock.elapsedRealtime()));
        }
    }

    private static void metricEnded(String str) {
        synchronized (TRON_METRICS) {
            TRON_METRICS.put(str, Long.valueOf(SystemClock.elapsedRealtime() + TRON_METRICS.get(str).longValue()));
        }
    }

    private static void metricShutdownStart() {
        synchronized (TRON_METRICS) {
            TRON_METRICS.put(METRIC_SHUTDOWN_TIME_START, Long.valueOf(System.currentTimeMillis()));
        }
    }

    private void setRebootProgress(final int i, final CharSequence charSequence) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (ShutdownThread.this.mProgressDialog != null) {
                    ShutdownThread.this.mProgressDialog.setProgress(i);
                    if (charSequence != null) {
                        ShutdownThread.this.mProgressDialog.setMessage(charSequence);
                    }
                }
            }
        });
    }

    private void shutdownRadios(final int i) {
        long j = i;
        final long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
        final boolean[] zArr = new boolean[1];
        Thread thread = new Thread() {
            @Override
            public void run() {
                boolean z;
                TimingsTraceLog timingsTraceLogNewTimingsLog = ShutdownThread.newTimingsLog();
                ITelephony iTelephonyAsInterface = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                if (iTelephonyAsInterface == null) {
                    z = true;
                    if (!z) {
                    }
                } else {
                    try {
                        if (!iTelephonyAsInterface.needMobileRadioShutdown()) {
                            z = true;
                            if (!z) {
                                Log.w(ShutdownThread.TAG, "Turning off cellular radios...");
                                ShutdownThread.metricStarted(ShutdownThread.METRIC_RADIO);
                                iTelephonyAsInterface.shutdownMobileRadios();
                            }
                        } else {
                            z = false;
                            if (!z) {
                            }
                        }
                    } catch (RemoteException e) {
                        Log.e(ShutdownThread.TAG, "RemoteException during radio shutdown", e);
                        z = true;
                    }
                }
                Log.i(ShutdownThread.TAG, "Waiting for Radio...");
                long j2 = jElapsedRealtime;
                long jElapsedRealtime2 = SystemClock.elapsedRealtime();
                while (true) {
                    if (j2 - jElapsedRealtime2 > 0) {
                        if (ShutdownThread.mRebootHasProgressBar) {
                            ShutdownThread.sInstance.setRebootProgress(((int) ((((((long) i) - r5) * 1.0d) * 12.0d) / ((double) i))) + 6, null);
                        }
                        if (!z) {
                            try {
                                z = !iTelephonyAsInterface.needMobileRadioShutdown();
                            } catch (RemoteException e2) {
                                Log.e(ShutdownThread.TAG, "RemoteException during radio shutdown", e2);
                                z = true;
                            }
                            if (z) {
                                Log.i(ShutdownThread.TAG, "Radio turned off.");
                                ShutdownThread.metricEnded(ShutdownThread.METRIC_RADIO);
                                timingsTraceLogNewTimingsLog.logDuration("ShutdownRadio", ((Long) ShutdownThread.TRON_METRICS.get(ShutdownThread.METRIC_RADIO)).longValue());
                            }
                        }
                        if (z) {
                            Log.i(ShutdownThread.TAG, "Radio shutdown complete.");
                            zArr[0] = true;
                            return;
                        } else {
                            SystemClock.sleep(100L);
                            j2 = jElapsedRealtime;
                            jElapsedRealtime2 = SystemClock.elapsedRealtime();
                        }
                    } else {
                        return;
                    }
                }
            }
        };
        thread.start();
        try {
            thread.join(j);
        } catch (InterruptedException e) {
        }
        if (!zArr[0]) {
            Log.w(TAG, "Timed out waiting for Radio shutdown.");
        }
    }

    public static void rebootOrShutdown(Context context, boolean z, String str) {
        if (z) {
            Log.i(TAG, "Rebooting, reason: " + str);
            PowerManagerService.lowLevelReboot(str);
            Log.e(TAG, "Reboot failed, will attempt shutdown instead");
            str = null;
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
        sInstance.mLowLevelShutdownSeq(context);
        Log.i(TAG, "Performing low-level shutdown...");
        PowerManagerService.lowLevelShutdown(str);
    }

    private static void saveMetrics(boolean z, String str) {
        FileOutputStream fileOutputStream;
        StringBuilder sb = new StringBuilder();
        sb.append("reboot:");
        sb.append(z ? "y" : "n");
        sb.append(",");
        sb.append("reason:");
        sb.append(str);
        int size = TRON_METRICS.size();
        for (int i = 0; i < size; i++) {
            String strKeyAt = TRON_METRICS.keyAt(i);
            long jLongValue = TRON_METRICS.valueAt(i).longValue();
            if (jLongValue < 0) {
                Log.e(TAG, "metricEnded wasn't called for " + strKeyAt);
            } else {
                sb.append(',');
                sb.append(strKeyAt);
                sb.append(':');
                sb.append(jLongValue);
            }
        }
        File file = new File("/data/system/shutdown-metrics.tmp");
        boolean z2 = true;
        try {
            fileOutputStream = new FileOutputStream(file);
            try {
                fileOutputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            } finally {
            }
        } catch (IOException e) {
            e = e;
            z2 = false;
        }
        try {
            fileOutputStream.close();
        } catch (IOException e2) {
            e = e2;
            Log.e(TAG, "Cannot save shutdown metrics", e);
        }
        if (z2) {
            file.renameTo(new File("/data/system/shutdown-metrics.txt"));
        }
    }

    private void uncrypt() {
        Log.i(TAG, "Calling uncrypt and monitoring the progress...");
        final RecoverySystem.ProgressListener progressListener = new RecoverySystem.ProgressListener() {
            @Override
            public void onProgress(int i) {
                if (i < 0 || i >= 100) {
                    if (i == 100) {
                        ShutdownThread.sInstance.setRebootProgress(i, ShutdownThread.this.mContext.getText(R.string.kg_failed_attempts_almost_at_wipe));
                        return;
                    }
                    return;
                }
                ShutdownThread.sInstance.setRebootProgress(((int) ((((double) i) * 80.0d) / 100.0d)) + 20, ShutdownThread.this.mContext.getText(R.string.kg_enter_confirm_pin_hint));
            }
        };
        final boolean[] zArr = {false};
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    RecoverySystem.processPackage(ShutdownThread.this.mContext, new File(FileUtils.readTextFile(RecoverySystem.UNCRYPT_PACKAGE_FILE, 0, null)), progressListener);
                } catch (IOException e) {
                    Log.e(ShutdownThread.TAG, "Error uncrypting file", e);
                }
                zArr[0] = true;
            }
        };
        thread.start();
        try {
            thread.join(900000L);
        } catch (InterruptedException e) {
        }
        if (!zArr[0]) {
            Log.w(TAG, "Timed out waiting for uncrypt.");
            try {
                FileUtils.stringToFile(RecoverySystem.UNCRYPT_STATUS_FILE, String.format("uncrypt_time: %d\nuncrypt_error: %d\n", 900, 100));
            } catch (IOException e2) {
                Log.e(TAG, "Failed to write timeout message to uncrypt status", e2);
            }
        }
    }

    protected boolean mIsShowShutdownSysui() {
        return true;
    }

    protected boolean mIsShowShutdownDialog(Context context) {
        return true;
    }

    protected boolean mStartShutdownSeq(Context context) {
        return true;
    }

    protected void mShutdownSeqFinish(Context context) {
    }

    protected void mLowLevelShutdownSeq(Context context) {
    }
}
