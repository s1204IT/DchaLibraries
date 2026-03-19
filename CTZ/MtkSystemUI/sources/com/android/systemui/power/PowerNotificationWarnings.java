package com.android.systemui.power;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.util.Slog;
import android.view.View;
import com.android.settingslib.Utils;
import com.android.settingslib.fuelgauge.BatterySaverUtils;
import com.android.settingslib.utils.PowerUtil;
import com.android.systemui.SystemUI;
import com.android.systemui.power.PowerUI;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.util.NotificationChannels;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class PowerNotificationWarnings implements PowerUI.WarningsUI {
    private int mBatteryLevel;
    private int mBucket;
    private final Context mContext;
    private Estimate mEstimate;
    private SystemUIDialog mHighTempDialog;
    private boolean mHighTempWarning;
    private boolean mInvalidCharger;
    private long mLowWarningThreshold;
    private final NotificationManager mNoMan;
    private boolean mPlaySound;
    private final PowerManager mPowerMan;
    private SystemUIDialog mSaverConfirmation;
    private SystemUIDialog mSaverEnabledConfirmation;
    private long mScreenOffTime;
    private long mSevereWarningThreshold;
    private boolean mShowAutoSaverSuggestion;
    private int mShowing;
    private SystemUIDialog mThermalShutdownDialog;
    private boolean mWarning;
    private long mWarningTriggerTimeMs;
    private static final boolean DEBUG = PowerUI.DEBUG;
    private static final String[] SHOWING_STRINGS = {"SHOWING_NOTHING", "SHOWING_WARNING", "SHOWING_SAVER", "SHOWING_INVALID_CHARGER", "SHOWING_AUTO_SAVER_SUGGESTION"};
    private static final AudioAttributes AUDIO_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Receiver mReceiver = new Receiver();
    private final Intent mOpenBatterySettings = settings("android.intent.action.POWER_USAGE_SUMMARY");

    public PowerNotificationWarnings(Context context) {
        this.mContext = context;
        this.mNoMan = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
        this.mPowerMan = (PowerManager) context.getSystemService("power");
        this.mReceiver.init();
    }

    @Override
    public void dump(PrintWriter printWriter) {
        printWriter.print("mWarning=");
        printWriter.println(this.mWarning);
        printWriter.print("mPlaySound=");
        printWriter.println(this.mPlaySound);
        printWriter.print("mInvalidCharger=");
        printWriter.println(this.mInvalidCharger);
        printWriter.print("mShowing=");
        printWriter.println(SHOWING_STRINGS[this.mShowing]);
        printWriter.print("mSaverConfirmation=");
        printWriter.println(this.mSaverConfirmation != null ? "not null" : null);
        printWriter.print("mSaverEnabledConfirmation=");
        printWriter.println(this.mSaverEnabledConfirmation != null ? "not null" : null);
        printWriter.print("mHighTempWarning=");
        printWriter.println(this.mHighTempWarning);
        printWriter.print("mHighTempDialog=");
        printWriter.println(this.mHighTempDialog != null ? "not null" : null);
        printWriter.print("mThermalShutdownDialog=");
        printWriter.println(this.mThermalShutdownDialog != null ? "not null" : null);
    }

    private int getLowBatteryAutoTriggerDefaultLevel() {
        return this.mContext.getResources().getInteger(R.integer.config_defaultAnalogClockSecondsHandFps);
    }

    @Override
    public void update(int i, int i2, long j) {
        this.mBatteryLevel = i;
        if (i2 >= 0) {
            this.mWarningTriggerTimeMs = 0L;
        } else if (i2 < this.mBucket) {
            this.mWarningTriggerTimeMs = System.currentTimeMillis();
        }
        this.mBucket = i2;
        this.mScreenOffTime = j;
    }

    @Override
    public void updateEstimate(Estimate estimate) {
        this.mEstimate = estimate;
        if (estimate.estimateMillis <= this.mLowWarningThreshold) {
            this.mWarningTriggerTimeMs = System.currentTimeMillis();
        }
    }

    @Override
    public void updateThresholds(long j, long j2) {
        this.mLowWarningThreshold = j;
        this.mSevereWarningThreshold = j2;
    }

    private void updateNotification() {
        if (DEBUG) {
            Slog.d("PowerUI.Notification", "updateNotification mWarning=" + this.mWarning + " mPlaySound=" + this.mPlaySound + " mInvalidCharger=" + this.mInvalidCharger);
        }
        if (this.mInvalidCharger) {
            showInvalidChargerNotification();
            this.mShowing = 3;
            return;
        }
        if (this.mWarning) {
            showWarningNotification();
            this.mShowing = 1;
        } else if (this.mShowAutoSaverSuggestion) {
            if (this.mShowing != 4) {
                showAutoSaverSuggestionNotification();
            }
            this.mShowing = 4;
        } else {
            this.mNoMan.cancelAsUser("low_battery", 2, UserHandle.ALL);
            this.mNoMan.cancelAsUser("low_battery", 3, UserHandle.ALL);
            this.mNoMan.cancelAsUser("auto_saver", 49, UserHandle.ALL);
            this.mShowing = 0;
        }
    }

    private void showInvalidChargerNotification() {
        Notification.Builder color = new Notification.Builder(this.mContext, NotificationChannels.ALERTS).setSmallIcon(com.android.systemui.R.drawable.ic_power_low).setWhen(0L).setShowWhen(false).setOngoing(true).setContentTitle(this.mContext.getString(com.android.systemui.R.string.invalid_charger_title)).setContentText(this.mContext.getString(com.android.systemui.R.string.invalid_charger_text)).setColor(this.mContext.getColor(R.color.car_colorPrimary));
        SystemUI.overrideNotificationAppName(this.mContext, color, false);
        Notification notificationBuild = color.build();
        this.mNoMan.cancelAsUser("low_battery", 3, UserHandle.ALL);
        this.mNoMan.notifyAsUser("low_battery", 2, notificationBuild, UserHandle.ALL);
    }

    protected void showWarningNotification() {
        String str = NumberFormat.getPercentInstance().format(((double) this.mBatteryLevel) / 100.0d);
        String string = this.mContext.getString(com.android.systemui.R.string.battery_low_title);
        String string2 = this.mContext.getString(com.android.systemui.R.string.battery_low_percent_format, str);
        if (this.mEstimate != null) {
            string2 = getHybridContentString(str);
        }
        Notification.Builder visibility = new Notification.Builder(this.mContext, NotificationChannels.BATTERY).setSmallIcon(com.android.systemui.R.drawable.ic_power_low).setWhen(this.mWarningTriggerTimeMs).setShowWhen(false).setContentText(string2).setContentTitle(string).setOnlyAlertOnce(true).setDeleteIntent(pendingBroadcast("PNW.dismissedWarning")).setStyle(new Notification.BigTextStyle().bigText(string2)).setVisibility(1);
        if (BenesseExtension.getDchaState() == 0 && hasBatterySettings()) {
            visibility.setContentIntent(pendingBroadcast("PNW.batterySettings"));
        }
        if (this.mEstimate == null || this.mBucket < 0 || this.mEstimate.estimateMillis < this.mSevereWarningThreshold) {
            visibility.setColor(Utils.getColorAttr(this.mContext, R.attr.colorError));
        }
        if (BenesseExtension.getDchaState() == 0) {
            visibility.addAction(0, this.mContext.getString(com.android.systemui.R.string.battery_saver_start_action), pendingBroadcast("PNW.startSaver"));
        }
        visibility.setOnlyAlertOnce(!this.mPlaySound);
        this.mPlaySound = false;
        SystemUI.overrideNotificationAppName(this.mContext, visibility, false);
        Notification notificationBuild = visibility.build();
        this.mNoMan.cancelAsUser("low_battery", 2, UserHandle.ALL);
        this.mNoMan.notifyAsUser("low_battery", 3, notificationBuild, UserHandle.ALL);
    }

    private void showAutoSaverSuggestionNotification() {
        Notification.Builder contentText = new Notification.Builder(this.mContext, NotificationChannels.HINTS).setSmallIcon(com.android.systemui.R.drawable.ic_power_saver).setWhen(0L).setShowWhen(false).setContentTitle(this.mContext.getString(com.android.systemui.R.string.auto_saver_title)).setContentText(this.mContext.getString(com.android.systemui.R.string.auto_saver_text, Integer.valueOf(getLowBatteryAutoTriggerDefaultLevel())));
        contentText.setContentIntent(pendingBroadcast("PNW.enableAutoSaver"));
        contentText.setDeleteIntent(pendingBroadcast("PNW.dismissAutoSaverSuggestion"));
        contentText.addAction(0, this.mContext.getString(com.android.systemui.R.string.no_auto_saver_action), pendingBroadcast("PNW.autoSaverNoThanks"));
        SystemUI.overrideNotificationAppName(this.mContext, contentText, false);
        this.mNoMan.notifyAsUser("auto_saver", 49, contentText.build(), UserHandle.ALL);
    }

    private String getHybridContentString(String str) {
        return PowerUtil.getBatteryRemainingStringFormatted(this.mContext, this.mEstimate.estimateMillis, str, this.mEstimate.isBasedOnUsage);
    }

    private PendingIntent pendingBroadcast(String str) {
        return PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent(str).setPackage(this.mContext.getPackageName()).setFlags(268435456), 0, UserHandle.CURRENT);
    }

    private static Intent settings(String str) {
        return new Intent(str).setFlags(1551892480);
    }

    @Override
    public boolean isInvalidChargerWarningShowing() {
        return this.mInvalidCharger;
    }

    @Override
    public void dismissHighTemperatureWarning() {
        if (!this.mHighTempWarning) {
            return;
        }
        this.mHighTempWarning = false;
        dismissHighTemperatureWarningInternal();
    }

    private void dismissHighTemperatureWarningInternal() {
        this.mNoMan.cancelAsUser("high_temp", 4, UserHandle.ALL);
    }

    @Override
    public void showHighTemperatureWarning() {
        if (this.mHighTempWarning) {
            return;
        }
        this.mHighTempWarning = true;
        Notification.Builder color = new Notification.Builder(this.mContext, NotificationChannels.ALERTS).setSmallIcon(com.android.systemui.R.drawable.ic_device_thermostat_24).setWhen(0L).setShowWhen(false).setContentTitle(this.mContext.getString(com.android.systemui.R.string.high_temp_title)).setContentText(this.mContext.getString(com.android.systemui.R.string.high_temp_notif_message)).setVisibility(1).setContentIntent(pendingBroadcast("PNW.clickedTempWarning")).setDeleteIntent(pendingBroadcast("PNW.dismissedTempWarning")).setColor(Utils.getColorAttr(this.mContext, R.attr.colorError));
        SystemUI.overrideNotificationAppName(this.mContext, color, false);
        this.mNoMan.notifyAsUser("high_temp", 4, color.build(), UserHandle.ALL);
    }

    private void showHighTemperatureDialog() {
        if (this.mHighTempDialog != null) {
            return;
        }
        SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
        systemUIDialog.setIconAttribute(R.attr.alertDialogIcon);
        systemUIDialog.setTitle(com.android.systemui.R.string.high_temp_title);
        systemUIDialog.setMessage(com.android.systemui.R.string.high_temp_dialog_message);
        systemUIDialog.setPositiveButton(R.string.ok, null);
        systemUIDialog.setShowForAllUsers(true);
        systemUIDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public final void onDismiss(DialogInterface dialogInterface) {
                this.f$0.mHighTempDialog = null;
            }
        });
        systemUIDialog.show();
        this.mHighTempDialog = systemUIDialog;
    }

    void dismissThermalShutdownWarning() {
        this.mNoMan.cancelAsUser("high_temp", 39, UserHandle.ALL);
    }

    private void showThermalShutdownDialog() {
        if (this.mThermalShutdownDialog != null) {
            return;
        }
        SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
        systemUIDialog.setIconAttribute(R.attr.alertDialogIcon);
        systemUIDialog.setTitle(com.android.systemui.R.string.thermal_shutdown_title);
        systemUIDialog.setMessage(com.android.systemui.R.string.thermal_shutdown_dialog_message);
        systemUIDialog.setPositiveButton(R.string.ok, null);
        systemUIDialog.setShowForAllUsers(true);
        systemUIDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public final void onDismiss(DialogInterface dialogInterface) {
                this.f$0.mThermalShutdownDialog = null;
            }
        });
        systemUIDialog.show();
        this.mThermalShutdownDialog = systemUIDialog;
    }

    @Override
    public void showThermalShutdownWarning() {
        Notification.Builder color = new Notification.Builder(this.mContext, NotificationChannels.ALERTS).setSmallIcon(com.android.systemui.R.drawable.ic_device_thermostat_24).setWhen(0L).setShowWhen(false).setContentTitle(this.mContext.getString(com.android.systemui.R.string.thermal_shutdown_title)).setContentText(this.mContext.getString(com.android.systemui.R.string.thermal_shutdown_message)).setVisibility(1).setContentIntent(pendingBroadcast("PNW.clickedThermalShutdownWarning")).setDeleteIntent(pendingBroadcast("PNW.dismissedThermalShutdownWarning")).setColor(Utils.getColorAttr(this.mContext, R.attr.colorError));
        SystemUI.overrideNotificationAppName(this.mContext, color, false);
        this.mNoMan.notifyAsUser("high_temp", 39, color.build(), UserHandle.ALL);
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

    @Override
    public void showLowBatteryWarning(boolean z) {
        Slog.i("PowerUI.Notification", "show low battery warning: level=" + this.mBatteryLevel + " [" + this.mBucket + "] playSound=" + z);
        this.mPlaySound = z;
        this.mWarning = true;
        updateNotification();
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

    private void showAutoSaverSuggestion() {
        this.mShowAutoSaverSuggestion = true;
        updateNotification();
    }

    private void dismissAutoSaverSuggestion() {
        this.mShowAutoSaverSuggestion = false;
        updateNotification();
    }

    @Override
    public void userSwitched() {
        updateNotification();
    }

    private void showStartSaverConfirmation() {
        if (this.mSaverConfirmation != null) {
            return;
        }
        SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
        systemUIDialog.setTitle(com.android.systemui.R.string.battery_saver_confirmation_title);
        systemUIDialog.setMessage(getBatterySaverDescription());
        if (isEnglishLocale()) {
            systemUIDialog.setMessageHyphenationFrequency(0);
        }
        systemUIDialog.setMessageMovementMethod(LinkMovementMethod.getInstance());
        systemUIDialog.setNegativeButton(R.string.cancel, null);
        systemUIDialog.setPositiveButton(com.android.systemui.R.string.battery_saver_confirmation_ok, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.setSaverMode(true, false);
            }
        });
        systemUIDialog.setShowForAllUsers(true);
        systemUIDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public final void onDismiss(DialogInterface dialogInterface) {
                this.f$0.mSaverConfirmation = null;
            }
        });
        systemUIDialog.show();
        this.mSaverConfirmation = systemUIDialog;
    }

    private boolean isEnglishLocale() {
        return Objects.equals(Locale.getDefault().getLanguage(), Locale.ENGLISH.getLanguage());
    }

    private CharSequence getBatterySaverDescription() {
        String string = this.mContext.getText(com.android.systemui.R.string.help_uri_battery_saver_learn_more_link_target).toString();
        if (TextUtils.isEmpty(string)) {
            return this.mContext.getText(R.string.accessibility_autoclick_double_click);
        }
        SpannableString spannableString = new SpannableString(this.mContext.getText(R.string.accessibility_autoclick_drag));
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannableString);
        for (Annotation annotation : (Annotation[]) spannableString.getSpans(0, spannableString.length(), Annotation.class)) {
            if ("url".equals(annotation.getValue())) {
                int spanStart = spannableString.getSpanStart(annotation);
                int spanEnd = spannableString.getSpanEnd(annotation);
                URLSpan uRLSpan = new URLSpan(string) {
                    @Override
                    public void updateDrawState(TextPaint textPaint) {
                        super.updateDrawState(textPaint);
                        textPaint.setUnderlineText(false);
                    }

                    @Override
                    public void onClick(View view) {
                        if (PowerNotificationWarnings.this.mSaverConfirmation != null) {
                            PowerNotificationWarnings.this.mSaverConfirmation.dismiss();
                        }
                        PowerNotificationWarnings.this.mContext.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS").setFlags(268435456));
                        if (BenesseExtension.getDchaState() != 0) {
                            return;
                        }
                        Uri uri = Uri.parse(getURL());
                        Context context = view.getContext();
                        Intent flags = new Intent("android.intent.action.VIEW", uri).setFlags(268435456);
                        try {
                            context.startActivity(flags);
                        } catch (ActivityNotFoundException e) {
                            Log.w("PowerUI.Notification", "Activity was not found for intent, " + flags.toString());
                        }
                    }
                };
                spannableStringBuilder.setSpan(uRLSpan, spanStart, spanEnd, spannableString.getSpanFlags(uRLSpan));
            }
        }
        return spannableStringBuilder;
    }

    private void showAutoSaverEnabledConfirmation() {
        if (BenesseExtension.getDchaState() == 0 && this.mSaverEnabledConfirmation == null) {
            final Intent flags = new Intent("android.settings.BATTERY_SAVER_SETTINGS").setFlags(268435456);
            SystemUIDialog systemUIDialog = new SystemUIDialog(this.mContext);
            systemUIDialog.setTitle(com.android.systemui.R.string.auto_saver_enabled_title);
            systemUIDialog.setMessage(this.mContext.getString(com.android.systemui.R.string.auto_saver_enabled_text, Integer.valueOf(getLowBatteryAutoTriggerDefaultLevel())));
            systemUIDialog.setPositiveButton(com.android.systemui.R.string.auto_saver_okay_action, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    this.f$0.onAutoSaverEnabledConfirmationClosed();
                }
            });
            systemUIDialog.setNeutralButton(com.android.systemui.R.string.open_saver_setting_action, new DialogInterface.OnClickListener() {
                @Override
                public final void onClick(DialogInterface dialogInterface, int i) {
                    PowerNotificationWarnings.lambda$showAutoSaverEnabledConfirmation$5(this.f$0, flags, dialogInterface, i);
                }
            });
            systemUIDialog.setShowForAllUsers(true);
            systemUIDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public final void onDismiss(DialogInterface dialogInterface) {
                    this.f$0.onAutoSaverEnabledConfirmationClosed();
                }
            });
            systemUIDialog.show();
            this.mSaverEnabledConfirmation = systemUIDialog;
        }
    }

    public static void lambda$showAutoSaverEnabledConfirmation$5(PowerNotificationWarnings powerNotificationWarnings, Intent intent, DialogInterface dialogInterface, int i) {
        powerNotificationWarnings.mContext.startActivity(intent);
        powerNotificationWarnings.onAutoSaverEnabledConfirmationClosed();
    }

    private void onAutoSaverEnabledConfirmationClosed() {
        this.mSaverEnabledConfirmation = null;
    }

    private void setSaverMode(boolean z, boolean z2) {
        BatterySaverUtils.setPowerSaveMode(this.mContext, z, z2);
    }

    private void scheduleAutoBatterySaver() {
        int integer = this.mContext.getResources().getInteger(R.integer.config_defaultBinderHeavyHitterWatcherBatchSize);
        if (integer == 0) {
            integer = 15;
        }
        BatterySaverUtils.ensureAutoBatterySaver(this.mContext, integer);
        showAutoSaverEnabledConfirmation();
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        public void init() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("PNW.batterySettings");
            intentFilter.addAction("PNW.startSaver");
            intentFilter.addAction("PNW.dismissedWarning");
            intentFilter.addAction("PNW.clickedTempWarning");
            intentFilter.addAction("PNW.dismissedTempWarning");
            intentFilter.addAction("PNW.clickedThermalShutdownWarning");
            intentFilter.addAction("PNW.dismissedThermalShutdownWarning");
            intentFilter.addAction("PNW.startSaverConfirmation");
            intentFilter.addAction("PNW.autoSaverSuggestion");
            intentFilter.addAction("PNW.enableAutoSaver");
            intentFilter.addAction("PNW.autoSaverNoThanks");
            intentFilter.addAction("PNW.dismissAutoSaverSuggestion");
            PowerNotificationWarnings.this.mContext.registerReceiverAsUser(this, UserHandle.ALL, intentFilter, "android.permission.DEVICE_POWER", PowerNotificationWarnings.this.mHandler);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Slog.i("PowerUI.Notification", "Received " + action);
            if (action.equals("PNW.batterySettings")) {
                if (BenesseExtension.getDchaState() == 0) {
                    PowerNotificationWarnings.this.dismissLowBatteryNotification();
                    PowerNotificationWarnings.this.mContext.startActivityAsUser(PowerNotificationWarnings.this.mOpenBatterySettings, UserHandle.CURRENT);
                    return;
                }
                return;
            }
            if (action.equals("PNW.startSaver")) {
                PowerNotificationWarnings.this.setSaverMode(true, true);
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                return;
            }
            if (action.equals("PNW.startSaverConfirmation")) {
                PowerNotificationWarnings.this.dismissLowBatteryNotification();
                PowerNotificationWarnings.this.showStartSaverConfirmation();
                return;
            }
            if (action.equals("PNW.dismissedWarning")) {
                PowerNotificationWarnings.this.dismissLowBatteryWarning();
                return;
            }
            if ("PNW.clickedTempWarning".equals(action)) {
                PowerNotificationWarnings.this.dismissHighTemperatureWarningInternal();
                PowerNotificationWarnings.this.showHighTemperatureDialog();
                return;
            }
            if ("PNW.dismissedTempWarning".equals(action)) {
                PowerNotificationWarnings.this.dismissHighTemperatureWarningInternal();
                return;
            }
            if ("PNW.clickedThermalShutdownWarning".equals(action)) {
                PowerNotificationWarnings.this.dismissThermalShutdownWarning();
                PowerNotificationWarnings.this.showThermalShutdownDialog();
                return;
            }
            if ("PNW.dismissedThermalShutdownWarning".equals(action)) {
                PowerNotificationWarnings.this.dismissThermalShutdownWarning();
                return;
            }
            if ("PNW.autoSaverSuggestion".equals(action)) {
                PowerNotificationWarnings.this.showAutoSaverSuggestion();
                return;
            }
            if ("PNW.dismissAutoSaverSuggestion".equals(action)) {
                PowerNotificationWarnings.this.dismissAutoSaverSuggestion();
                return;
            }
            if ("PNW.enableAutoSaver".equals(action)) {
                PowerNotificationWarnings.this.dismissAutoSaverSuggestion();
                PowerNotificationWarnings.this.scheduleAutoBatterySaver();
            } else if ("PNW.autoSaverNoThanks".equals(action)) {
                PowerNotificationWarnings.this.dismissAutoSaverSuggestion();
                BatterySaverUtils.suppressAutoBatterySaver(context);
            }
        }
    }
}
