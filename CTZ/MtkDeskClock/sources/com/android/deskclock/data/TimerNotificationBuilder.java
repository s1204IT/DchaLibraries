package com.android.deskclock.data;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.RemoteViews;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.NotificationChannelManager;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.timer.ExpiredTimersActivity;
import com.android.deskclock.timer.TimerService;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class TimerNotificationBuilder {
    private static final int REQUEST_CODE_MISSING = 1;
    private static final int REQUEST_CODE_UPCOMING = 0;

    TimerNotificationBuilder() {
    }

    public Notification build(Context context, NotificationModel notificationModel, List<Timer> list) {
        String label;
        String string;
        Timer timer = list.get(0);
        int size = list.size();
        boolean zIsRunning = timer.isRunning();
        Resources resources = context.getResources();
        long chronometerBase = getChronometerBase(timer);
        String packageName = context.getPackageName();
        ArrayList arrayList = new ArrayList(2);
        if (size == 1) {
            if (zIsRunning) {
                if (TextUtils.isEmpty(timer.getLabel())) {
                    label = resources.getString(R.string.timer_notification_label);
                } else {
                    label = timer.getLabel();
                }
                arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_pause_24dp, resources.getText(R.string.timer_pause), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) TimerService.class).setAction(TimerService.ACTION_PAUSE_TIMER).putExtra(TimerService.EXTRA_TIMER_ID, timer.getId()))).build());
                arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_add_24dp, resources.getText(R.string.timer_plus_1_min), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) TimerService.class).setAction(TimerService.ACTION_ADD_MINUTE_TIMER_UNEXPIRED).putExtra(TimerService.EXTRA_TIMER_ID, timer.getId()))).build());
            } else {
                String string2 = resources.getString(R.string.timer_paused);
                arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_start_24dp, resources.getText(R.string.sw_resume_button), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) TimerService.class).setAction(TimerService.ACTION_START_TIMER).putExtra(TimerService.EXTRA_TIMER_ID, timer.getId()))).build());
                arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_reset_24dp, resources.getText(R.string.sw_reset_button), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) TimerService.class).setAction(TimerService.ACTION_RESET_TIMER).putExtra(TimerService.EXTRA_TIMER_ID, timer.getId()))).build());
                label = string2;
            }
        } else {
            String string3 = zIsRunning ? resources.getString(R.string.timers_in_use, Integer.valueOf(size)) : resources.getString(R.string.timers_stopped, Integer.valueOf(size));
            arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_reset_24dp, resources.getText(R.string.timer_reset_all), Utils.pendingServiceIntent(context, TimerService.createResetUnexpiredTimersIntent(context))).build());
            label = string3;
        }
        NotificationCompat.Builder color = new NotificationCompat.Builder(context).setOngoing(true).setLocalOnly(true).setShowWhen(false).setAutoCancel(false).setContentIntent(PendingIntent.getService(context, 0, new Intent(context, (Class<?>) TimerService.class).setAction(TimerService.ACTION_SHOW_TIMER).putExtra(TimerService.EXTRA_TIMER_ID, timer.getId()).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification), 1207959552)).setPriority(1).setCategory(NotificationCompat.CATEGORY_ALARM).setSmallIcon(R.drawable.stat_notify_timer).setSortKey(notificationModel.getTimerNotificationSortKey()).setVisibility(1).setStyle(new NotificationCompat.DecoratedCustomViewStyle()).setColor(ContextCompat.getColor(context, R.color.default_background));
        NotificationChannelManager.applyChannel(color, context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            color.addAction((NotificationCompat.Action) it.next());
        }
        if (Utils.isNOrLater()) {
            color.setCustomContentView(buildChronometer(packageName, chronometerBase, zIsRunning, label)).setGroup(notificationModel.getTimerNotificationGroupKey());
        } else {
            if (size == 1) {
                string = TimerStringFormatter.formatTimeRemaining(context, timer.getRemainingTime(), false);
            } else if (zIsRunning) {
                string = context.getString(R.string.next_timer_notif, TimerStringFormatter.formatTimeRemaining(context, timer.getRemainingTime(), false));
            } else {
                string = context.getString(R.string.all_timers_stopped_notif);
            }
            color.setContentTitle(label).setContentText(string);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
            Intent intentCreateUpdateNotificationIntent = TimerService.createUpdateNotificationIntent(context);
            long remainingTime = timer.getRemainingTime();
            if (timer.isRunning() && remainingTime > 60000) {
                TimerModel.schedulePendingIntent(alarmManager, SystemClock.elapsedRealtime() + (remainingTime % 60000), PendingIntent.getService(context, 0, intentCreateUpdateNotificationIntent, 1207959552));
            } else {
                PendingIntent service = PendingIntent.getService(context, 0, intentCreateUpdateNotificationIntent, 1610612736);
                if (service != null) {
                    alarmManager.cancel(service);
                    service.cancel();
                }
            }
        }
        return color.build();
    }

    Notification buildHeadsUp(Context context, List<Timer> list) {
        String string;
        String string2;
        Timer timer = list.get(0);
        PendingIntent pendingIntentPendingServiceIntent = Utils.pendingServiceIntent(context, TimerService.createResetExpiredTimersIntent(context));
        int size = list.size();
        ArrayList arrayList = new ArrayList(2);
        if (size != 1) {
            string = context.getString(R.string.timer_multi_times_up, Integer.valueOf(size));
            arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_stop_24dp, context.getString(R.string.timer_stop_all), pendingIntentPendingServiceIntent).build());
        } else {
            string = timer.getLabel();
            if (TextUtils.isEmpty(string)) {
                string = context.getString(R.string.timer_times_up);
            }
            arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_stop_24dp, context.getString(R.string.timer_stop), pendingIntentPendingServiceIntent).build());
            arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_add_24dp, context.getString(R.string.timer_plus_1_min), Utils.pendingServiceIntent(context, TimerService.createAddMinuteTimerIntent(context, timer.getId()))).build());
        }
        long chronometerBase = getChronometerBase(timer);
        String packageName = context.getPackageName();
        NotificationCompat.Builder color = new NotificationCompat.Builder(context).setOngoing(true).setLocalOnly(true).setShowWhen(false).setAutoCancel(false).setContentIntent(Utils.pendingActivityIntent(context, new Intent(context, (Class<?>) ExpiredTimersActivity.class))).setPriority(2).setDefaults(4).setSmallIcon(R.drawable.stat_notify_timer).setFullScreenIntent(Utils.pendingActivityIntent(context, new Intent(context, (Class<?>) ExpiredTimersActivity.class).setFlags(268697600)), true).setStyle(new NotificationCompat.DecoratedCustomViewStyle()).setColor(ContextCompat.getColor(context, R.color.default_background));
        NotificationChannelManager.applyChannel(color, context, NotificationChannelManager.Channel.EVENT_EXPIRED);
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            color.addAction((NotificationCompat.Action) it.next());
        }
        if (Utils.isNOrLater()) {
            color.setCustomContentView(buildChronometer(packageName, chronometerBase, true, string));
        } else {
            if (size != 1) {
                string2 = context.getString(R.string.timer_multi_times_up, Integer.valueOf(size));
            } else {
                string2 = context.getString(R.string.timer_times_up);
            }
            color.setContentTitle(string).setContentText(string2);
        }
        return color.build();
    }

    Notification buildMissed(Context context, NotificationModel notificationModel, List<Timer> list) {
        String string;
        NotificationCompat.Action actionBuild;
        Timer timer = list.get(0);
        int size = list.size();
        long chronometerBase = getChronometerBase(timer);
        String packageName = context.getPackageName();
        Resources resources = context.getResources();
        if (size == 1) {
            if (TextUtils.isEmpty(timer.getLabel())) {
                string = resources.getString(R.string.missed_timer_notification_label);
            } else {
                string = resources.getString(R.string.missed_named_timer_notification_label, timer.getLabel());
            }
            actionBuild = new NotificationCompat.Action.Builder(R.drawable.ic_reset_24dp, resources.getText(R.string.timer_reset), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) TimerService.class).setAction(TimerService.ACTION_RESET_TIMER).putExtra(TimerService.EXTRA_TIMER_ID, timer.getId()))).build();
        } else {
            string = resources.getString(R.string.timer_multi_missed, Integer.valueOf(size));
            actionBuild = new NotificationCompat.Action.Builder(R.drawable.ic_reset_24dp, resources.getText(R.string.timer_reset_all), Utils.pendingServiceIntent(context, TimerService.createResetMissedTimersIntent(context))).build();
        }
        NotificationCompat.Builder color = new NotificationCompat.Builder(context).setLocalOnly(true).setShowWhen(false).setAutoCancel(false).setContentIntent(PendingIntent.getService(context, 1, new Intent(context, (Class<?>) TimerService.class).setAction(TimerService.ACTION_SHOW_TIMER).putExtra(TimerService.EXTRA_TIMER_ID, timer.getId()).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification), 1207959552)).setPriority(1).setCategory(NotificationCompat.CATEGORY_ALARM).setSmallIcon(R.drawable.stat_notify_timer).setVisibility(1).setSortKey(notificationModel.getTimerNotificationMissedSortKey()).setStyle(new NotificationCompat.DecoratedCustomViewStyle()).addAction(actionBuild).setColor(ContextCompat.getColor(context, R.color.default_background));
        NotificationChannelManager.applyChannel(color, context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION);
        if (Utils.isNOrLater()) {
            color.setCustomContentView(buildChronometer(packageName, chronometerBase, true, string)).setGroup(notificationModel.getTimerNotificationGroupKey());
        } else {
            color.setContentText(AlarmUtils.getFormattedTime(context, timer.getWallClockExpirationTime())).setContentTitle(string);
        }
        return color.build();
    }

    private static long getChronometerBase(Timer timer) {
        long remainingTime = timer.getRemainingTime();
        if (remainingTime >= 0) {
            remainingTime += 1000;
        }
        return SystemClock.elapsedRealtime() + remainingTime;
    }

    @TargetApi(24)
    private RemoteViews buildChronometer(String str, long j, boolean z, CharSequence charSequence) {
        RemoteViews remoteViews = new RemoteViews(str, R.layout.chronometer_notif_content);
        remoteViews.setChronometerCountDown(R.id.chronometer, true);
        remoteViews.setChronometer(R.id.chronometer, j, null, z);
        remoteViews.setTextViewText(R.id.state, charSequence);
        return remoteViews;
    }
}
