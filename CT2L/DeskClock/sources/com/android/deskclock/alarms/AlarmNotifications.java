package com.android.deskclock.alarms;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

public final class AlarmNotifications {
    public static void registerNextAlarmWithAlarmManager(Context context, AlarmInstance instance) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
        int flags = instance == null ? 536870912 : 0;
        PendingIntent operation = PendingIntent.getBroadcast(context, 0, AlarmStateManager.createIndicatorIntent(context), flags);
        if (instance != null) {
            long alarmTime = instance.getAlarmTime().getTimeInMillis();
            PendingIntent viewIntent = PendingIntent.getActivity(context, instance.hashCode(), createViewAlarmIntent(context, instance), 134217728);
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(alarmTime, viewIntent);
            alarmManager.setAlarmClock(info, operation);
            return;
        }
        if (operation != null) {
            alarmManager.cancel(operation);
        }
    }

    public static void showLowPriorityNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying low priority notification for alarm instance: " + instance.mId, new Object[0]);
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context).setContentTitle(resources.getString(R.string.alarm_alert_predismiss_title)).setContentText(AlarmUtils.getAlarmText(context, instance)).setSmallIcon(R.drawable.stat_notify_alarm).setOngoing(false).setAutoCancel(false).setPriority(0).setCategory("alarm").setVisibility(1).setLocalOnly(true);
        Intent hideIntent = AlarmStateManager.createStateChangeIntent(context, "DELETE_TAG", instance, 2);
        notification.setDeleteIntent(PendingIntent.getBroadcast(context, instance.hashCode(), hideIntent, 134217728));
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG", instance, 7);
        notification.addAction(R.drawable.ic_alarm_off_black, resources.getString(R.string.alarm_alert_dismiss_now_text), PendingIntent.getBroadcast(context, instance.hashCode(), dismissIntent, 134217728));
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(), viewAlarmIntent, 134217728));
        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showHighPriorityNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying high priority notification for alarm instance: " + instance.mId, new Object[0]);
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context).setContentTitle(resources.getString(R.string.alarm_alert_predismiss_title)).setContentText(AlarmUtils.getAlarmText(context, instance)).setSmallIcon(R.drawable.stat_notify_alarm).setOngoing(true).setAutoCancel(false).setPriority(1).setCategory("alarm").setVisibility(1).setLocalOnly(true);
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG", instance, 7);
        notification.addAction(R.drawable.ic_alarm_off_black, resources.getString(R.string.alarm_alert_dismiss_now_text), PendingIntent.getBroadcast(context, instance.hashCode(), dismissIntent, 134217728));
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(), viewAlarmIntent, 134217728));
        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showSnoozeNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying snoozed notification for alarm instance: " + instance.mId, new Object[0]);
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context).setContentTitle(instance.getLabelOrDefault(context)).setContentText(resources.getString(R.string.alarm_alert_snooze_until, AlarmUtils.getFormattedTime(context, instance.getAlarmTime()))).setSmallIcon(R.drawable.stat_notify_alarm).setOngoing(true).setAutoCancel(false).setPriority(2).setCategory("alarm").setVisibility(1).setLocalOnly(true);
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG", instance, 7);
        notification.addAction(R.drawable.ic_alarm_off_black, resources.getString(R.string.alarm_alert_dismiss_text), PendingIntent.getBroadcast(context, instance.hashCode(), dismissIntent, 134217728));
        Intent viewAlarmIntent = createViewAlarmIntent(context, instance);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(), viewAlarmIntent, 134217728));
        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showMissedNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying missed notification for alarm instance: " + instance.mId, new Object[0]);
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        String label = instance.mLabel;
        String alarmTime = AlarmUtils.getFormattedTime(context, instance.getAlarmTime());
        String contextText = instance.mLabel.isEmpty() ? alarmTime : context.getString(R.string.alarm_missed_text, alarmTime, label);
        Notification.Builder notification = new Notification.Builder(context).setContentTitle(context.getString(R.string.alarm_missed_title)).setContentText(contextText).setSmallIcon(R.drawable.stat_notify_alarm).setPriority(1).setCategory("alarm").setVisibility(1).setLocalOnly(true);
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG", instance, 7);
        notification.setDeleteIntent(PendingIntent.getBroadcast(context, instance.hashCode(), dismissIntent, 134217728));
        Intent showAndDismiss = AlarmInstance.createIntent(context, AlarmStateManager.class, instance.mId);
        showAndDismiss.setAction("show_and_dismiss_alarm");
        notification.setContentIntent(PendingIntent.getBroadcast(context, instance.hashCode(), showAndDismiss, 134217728));
        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void showAlarmNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Displaying alarm notification for alarm instance: " + instance.mId, new Object[0]);
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        context.sendBroadcast(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"));
        Resources resources = context.getResources();
        Notification.Builder notification = new Notification.Builder(context).setContentTitle(instance.getLabelOrDefault(context)).setContentText(AlarmUtils.getFormattedTime(context, instance.getAlarmTime())).setSmallIcon(R.drawable.stat_notify_alarm).setOngoing(true).setAutoCancel(false).setDefaults(4).setWhen(0L).setCategory("alarm").setVisibility(1).setLocalOnly(true);
        Intent snoozeIntent = AlarmStateManager.createStateChangeIntent(context, "SNOOZE_TAG", instance, 4);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(), snoozeIntent, 134217728);
        notification.addAction(R.drawable.ic_snooze_black, resources.getString(R.string.alarm_alert_snooze_text), snoozePendingIntent);
        Intent dismissIntent = AlarmStateManager.createStateChangeIntent(context, "DISMISS_TAG", instance, 7);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, instance.hashCode(), dismissIntent, 134217728);
        notification.addAction(R.drawable.ic_alarm_off_black, resources.getString(R.string.alarm_alert_dismiss_text), dismissPendingIntent);
        Intent contentIntent = AlarmInstance.createIntent(context, AlarmActivity.class, instance.mId);
        notification.setContentIntent(PendingIntent.getActivity(context, instance.hashCode(), contentIntent, 134217728));
        Intent fullScreenIntent = AlarmInstance.createIntent(context, AlarmActivity.class, instance.mId);
        fullScreenIntent.setAction("fullscreen_activity");
        fullScreenIntent.setFlags(268697600);
        notification.setFullScreenIntent(PendingIntent.getActivity(context, instance.hashCode(), fullScreenIntent, 134217728), true);
        notification.setPriority(2);
        nm.cancel(instance.hashCode());
        nm.notify(instance.hashCode(), notification.build());
    }

    public static void clearNotification(Context context, AlarmInstance instance) {
        LogUtils.v("Clearing notifications for alarm instance: " + instance.mId, new Object[0]);
        NotificationManager nm = (NotificationManager) context.getSystemService("notification");
        nm.cancel(instance.hashCode());
    }

    private static Intent createViewAlarmIntent(Context context, AlarmInstance instance) {
        long alarmId = instance.mAlarmId == null ? -1L : instance.mAlarmId.longValue();
        Intent viewAlarmIntent = Alarm.createIntent(context, DeskClock.class, alarmId);
        viewAlarmIntent.putExtra("deskclock.select.tab", 0);
        viewAlarmIntent.putExtra("deskclock.scroll.to.alarm", alarmId);
        viewAlarmIntent.addFlags(268435456);
        return viewAlarmIntent;
    }
}
