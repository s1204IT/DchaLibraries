package com.android.systemui.power;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import com.android.systemui.R;
import com.android.systemui.power.PowerUI;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import java.io.PrintWriter;
import java.text.NumberFormat;

public class PowerNotificationWarnings implements PowerUI.WarningsUI {
    private int mBatteryLevel;
    private int mBucket;
    private long mBucketDroppedNegativeTimeMs;
    private final Context mContext;
    private boolean mInvalidCharger;
    private final NotificationManager mNoMan;
    private boolean mPlaySound;
    private final PowerManager mPowerMan;
    private boolean mSaver;
    private SystemUIDialog mSaverConfirmation;
    private long mScreenOffTime;
    private int mShowing;
    private boolean mWarning;
    private static final boolean DEBUG = PowerUI.DEBUG;
    private static final String[] SHOWING_STRINGS = {"SHOWING_NOTHING", "SHOWING_WARNING", "SHOWING_SAVER", "SHOWING_INVALID_CHARGER"};
    private static final AudioAttributes AUDIO_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private final Handler mHandler = new Handler();
    private final Receiver mReceiver = new Receiver();
    private final Intent mOpenBatterySettings = settings("android.intent.action.POWER_USAGE_SUMMARY");
    private final Intent mOpenSaverSettings = settings("android.settings.BATTERY_SAVER_SETTINGS");
    private final DialogInterface.OnClickListener mStartSaverMode = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    PowerNotificationWarnings.this.setSaverMode(true);
                }
            });
        }
    };

    public PowerNotificationWarnings(Context context, PhoneStatusBar phoneStatusBar) {
        this.mContext = context;
        this.mNoMan = (NotificationManager) context.getSystemService("notification");
        this.mPowerMan = (PowerManager) context.getSystemService("power");
        this.mReceiver.init();
    }

    @Override
    public void dump(PrintWriter pw) {
        pw.print("mSaver=");
        pw.println(this.mSaver);
        pw.print("mWarning=");
        pw.println(this.mWarning);
        pw.print("mPlaySound=");
        pw.println(this.mPlaySound);
        pw.print("mInvalidCharger=");
        pw.println(this.mInvalidCharger);
        pw.print("mShowing=");
        pw.println(SHOWING_STRINGS[this.mShowing]);
        pw.print("mSaverConfirmation=");
        pw.println(this.mSaverConfirmation != null ? "not null" : null);
    }

    @Override
    public void update(int batteryLevel, int bucket, long screenOffTime) {
        this.mBatteryLevel = batteryLevel;
        if (bucket >= 0) {
            this.mBucketDroppedNegativeTimeMs = 0L;
        } else if (bucket < this.mBucket) {
            this.mBucketDroppedNegativeTimeMs = System.currentTimeMillis();
        }
        this.mBucket = bucket;
        this.mScreenOffTime = screenOffTime;
    }

    @Override
    public void showSaverMode(boolean mode) {
        this.mSaver = mode;
        if (this.mSaver && this.mSaverConfirmation != null) {
            this.mSaverConfirmation.dismiss();
        }
        updateNotification();
    }

    private void updateNotification() {
        if (DEBUG) {
            Slog.d("PowerUI.Notification", "updateNotification mWarning=" + this.mWarning + " mPlaySound=" + this.mPlaySound + " mSaver=" + this.mSaver + " mInvalidCharger=" + this.mInvalidCharger);
        }
        if (this.mInvalidCharger) {
            showInvalidChargerNotification();
            this.mShowing = 3;
        } else if (this.mWarning) {
            showWarningNotification();
            this.mShowing = 1;
        } else if (this.mSaver) {
            showSaverNotification();
            this.mShowing = 2;
        } else {
            this.mNoMan.cancelAsUser("low_battery", 100, UserHandle.ALL);
            this.mShowing = 0;
        }
    }

    private void showInvalidChargerNotification() {
        Notification.Builder nb = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_power_low).setWhen(0L).setShowWhen(false).setOngoing(true).setContentTitle(this.mContext.getString(R.string.invalid_charger_title)).setContentText(this.mContext.getString(R.string.invalid_charger_text)).setPriority(2).setVisibility(1).setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_600));
        Notification n = nb.build();
        if (n.headsUpContentView != null) {
            n.headsUpContentView.setViewVisibility(android.R.id.replaceText, 8);
        }
        this.mNoMan.notifyAsUser("low_battery", 100, n, UserHandle.ALL);
    }

    private void showWarningNotification() {
        int textRes = this.mSaver ? R.string.battery_low_percent_format_saver_started : R.string.battery_low_percent_format;
        String percentage = NumberFormat.getPercentInstance().format(((double) this.mBatteryLevel) / 100.0d);
        Notification.Builder nb = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_power_low).setWhen(this.mBucketDroppedNegativeTimeMs).setShowWhen(false).setContentTitle(this.mContext.getString(R.string.battery_low_title)).setContentText(this.mContext.getString(textRes, percentage)).setOnlyAlertOnce(true).setDeleteIntent(pendingBroadcast("PNW.dismissedWarning")).setPriority(2).setVisibility(1).setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_700));
        int dcha_state = BenesseExtension.getDchaState();
        if (dcha_state == 0) {
            if (hasBatterySettings()) {
                nb.setContentIntent(pendingBroadcast("PNW.batterySettings"));
            }
            if (!this.mSaver) {
                nb.addAction(0, this.mContext.getString(R.string.battery_saver_start_action), pendingBroadcast("PNW.startSaver"));
            } else {
                addStopSaverAction(nb);
            }
        }
        if (this.mPlaySound) {
            attachLowBatterySound(nb);
            this.mPlaySound = false;
        }
        Notification n = nb.build();
        if (n.headsUpContentView != null) {
            n.headsUpContentView.setViewVisibility(android.R.id.replaceText, 8);
        }
        this.mNoMan.notifyAsUser("low_battery", 100, n, UserHandle.ALL);
    }

    private void showSaverNotification() {
        Notification.Builder nb = new Notification.Builder(this.mContext).setSmallIcon(R.drawable.ic_power_saver).setContentTitle(this.mContext.getString(R.string.battery_saver_notification_title)).setContentText(this.mContext.getString(R.string.battery_saver_notification_text)).setOngoing(true).setShowWhen(false).setVisibility(1).setColor(this.mContext.getResources().getColor(android.R.color.system_accent3_700));
        addStopSaverAction(nb);
        if (hasSaverSettings()) {
            nb.setContentIntent(pendingActivity(this.mOpenSaverSettings));
        }
        this.mNoMan.notifyAsUser("low_battery", 100, nb.build(), UserHandle.ALL);
    }

    private void addStopSaverAction(Notification.Builder nb) {
        nb.addAction(0, this.mContext.getString(R.string.battery_saver_notification_action_text), pendingBroadcast("PNW.stopSaver"));
    }

    private void dismissSaverNotification() {
        if (this.mSaver) {
            Slog.i("PowerUI.Notification", "dismissing saver notification");
        }
        this.mSaver = false;
        updateNotification();
    }

    private PendingIntent pendingActivity(Intent intent) {
        return PendingIntent.getActivityAsUser(this.mContext, 0, intent, 0, null, UserHandle.CURRENT);
    }

    private PendingIntent pendingBroadcast(String action) {
        return PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(action), 0, UserHandle.CURRENT);
    }

    private static Intent settings(String action) {
        return new Intent(action).setFlags(1551892480);
    }

    @Override
    public boolean isInvalidChargerWarningShowing() {
        return this.mInvalidCharger;
    }

    @Override
    public void updateLowBatteryWarning() {
        updateNotification();
    }

    @Override
    public void dismissLowBatteryWarning() {
        if (DEBUG) {
            Slog.d("PowerUI.Notification", "dismissing low battery warning: level=" + this.mBatteryLevel);
        }
        dismissLowBatteryNotification();
    }

    private void dismissLowBatteryNotification() {
        if (this.mWarning) {
            Slog.i("PowerUI.Notification", "dismissing low battery notification");
        }
        this.mWarning = false;
        updateNotification();
    }

    private boolean hasBatterySettings() {
        return this.mOpenBatterySettings.resolveActivity(this.mContext.getPackageManager()) != null;
    }

    private boolean hasSaverSettings() {
        return this.mOpenSaverSettings.resolveActivity(this.mContext.getPackageManager()) != null;
    }

    @Override
    public void showLowBatteryWarning(boolean playSound) {
        Slog.i("PowerUI.Notification", "show low battery warning: level=" + this.mBatteryLevel + " [" + this.mBucket + "] playSound=" + playSound);
        this.mPlaySound = playSound;
        this.mWarning = true;
        updateNotification();
    }

    private void attachLowBatterySound(Notification.Builder b) {
        String soundPath;
        Uri soundUri;
        ContentResolver cr = this.mContext.getContentResolver();
        int silenceAfter = Settings.Global.getInt(cr, "low_battery_sound_timeout", 0);
        long offTime = SystemClock.elapsedRealtime() - this.mScreenOffTime;
        if (silenceAfter > 0 && this.mScreenOffTime > 0 && offTime > silenceAfter) {
            Slog.i("PowerUI.Notification", "screen off too long (" + offTime + "ms, limit " + silenceAfter + "ms): not waking up the user with low battery sound");
            return;
        }
        if (DEBUG) {
            Slog.d("PowerUI.Notification", "playing low battery sound. pick-a-doop!");
        }
        if (Settings.Global.getInt(cr, "power_sounds_enabled", 1) == 1 && (soundPath = Settings.Global.getString(cr, "low_battery_sound")) != null && (soundUri = Uri.parse("file://" + soundPath)) != null) {
            b.setSound(soundUri, AUDIO_ATTRIBUTES);
            if (DEBUG) {
                Slog.d("PowerUI.Notification", "playing sound " + soundUri);
            }
        }
    }

    @Override
    public void dismissInvalidChargerWarning() {
        dismissInvalidChargerNotification();
    }

    private void dismissInvalidChargerNotification() {
        if (this.mInvalidCharger) {
            Slog.i("PowerUI.Notification", "dismissing invalid charger notification");
        }
        this.mInvalidCharger = false;
        updateNotification();
    }

    @Override
    public void showInvalidChargerWarning() {
        this.mInvalidCharger = true;
        updateNotification();
    }

    @Override
    public void userSwitched() {
        updateNotification();
    }

    private void showStartSaverConfirmation() {
        if (this.mSaverConfirmation == null) {
            SystemUIDialog d = new SystemUIDialog(this.mContext);
            d.setTitle(R.string.battery_saver_confirmation_title);
            d.setMessage(android.R.string.network_available_sign_in_detailed);
            d.setNegativeButton(android.R.string.cancel, null);
            d.setPositiveButton(R.string.battery_saver_confirmation_ok, this.mStartSaverMode);
            d.setShowForAllUsers(true);
            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    PowerNotificationWarnings.this.mSaverConfirmation = null;
                }
            });
            d.show();
            this.mSaverConfirmation = d;
        }
    }

    private void setSaverMode(boolean mode) {
        this.mPowerMan.setPowerSaveMode(mode);
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction("PNW.batterySettings");
            filter.addAction("PNW.startSaver");
            filter.addAction("PNW.stopSaver");
            filter.addAction("PNW.dismissedWarning");
            PowerNotificationWarnings.this.mContext.registerReceiverAsUser(this, UserHandle.ALL, filter, null, PowerNotificationWarnings.this.mHandler);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Slog.i("PowerUI.Notification", "Received " + action);
            if (action.equals("PNW.batterySettings")) {
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.mContext.startActivityAsUser(PowerNotificationWarnings.this.mOpenBatterySettings, UserHandle.CURRENT);
                return;
            }
            if (action.equals("PNW.startSaver")) {
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.showStartSaverConfirmation();
            } else if (action.equals("PNW.stopSaver")) {
                PowerNotificationWarnings.this.dismissSaverNotification();
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.setSaverMode(false);
            } else if (action.equals("PNW.dismissedWarning")) {
                PowerNotificationWarnings.this.dismissLowBatteryWarning();
            }
        }
    }
}
