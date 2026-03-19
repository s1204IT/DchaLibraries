package com.android.deskclock.alarms;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import com.android.deskclock.AlarmClockFragment;
import com.android.deskclock.AlarmUtils;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.NotificationChannelManager;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

final class AlarmNotifications {
    private static final int ALARM_FIRING_NOTIFICATION_ID = 2147483640;
    private static final int ALARM_GROUP_MISSED_NOTIFICATION_ID = 2147483642;
    private static final int ALARM_GROUP_NOTIFICATION_ID = 2147483643;
    static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
    private static final String MISSED_GROUP_KEY = "4";
    private static final DateFormat SORT_KEY_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final String UPCOMING_GROUP_KEY = "1";

    AlarmNotifications() {
    }

    public static void registerNextAlarmWithAlarmManager(Context context, AlarmInstance alarmInstance) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, AlarmStateManager.createIndicatorIntent(context), alarmInstance == null ? 536870912 : 0);
        if (alarmInstance != null) {
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarmInstance.getAlarmTime().getTimeInMillis(), PendingIntent.getActivity(context, alarmInstance.hashCode(), createViewAlarmIntent(context, alarmInstance), 134217728)), broadcast);
        } else if (broadcast != null) {
            alarmManager.cancel(broadcast);
        }
    }

    static synchronized void showLowPriorityNotification(Context context, AlarmInstance alarmInstance) {
        LogUtils.v("Displaying low priority notification for alarm instance: " + alarmInstance.mId, new Object[0]);
        NotificationCompat.Builder localOnly = new NotificationCompat.Builder(context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION).setShowWhen(false).setContentTitle(context.getString(R.string.alarm_alert_predismiss_title)).setContentText(AlarmUtils.getAlarmText(context, alarmInstance, true)).setColor(ContextCompat.getColor(context, R.color.default_background)).setSmallIcon(R.drawable.stat_notify_alarm).setAutoCancel(false).setSortKey(createSortKey(alarmInstance)).setPriority(0).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(1).setLocalOnly(true);
        if (Utils.isNOrLater()) {
            localOnly.setGroup("1");
        }
        Intent intentCreateStateChangeIntent = AlarmStateManager.createStateChangeIntent(context, AlarmStateManager.ALARM_DELETE_TAG, alarmInstance, 2);
        int iHashCode = alarmInstance.hashCode();
        localOnly.setDeleteIntent(PendingIntent.getService(context, iHashCode, intentCreateStateChangeIntent, 134217728));
        localOnly.addAction(R.drawable.ic_alarm_off_24dp, context.getString(R.string.alarm_alert_dismiss_text), PendingIntent.getService(context, iHashCode, AlarmStateManager.createStateChangeIntent(context, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance, 8), 134217728));
        localOnly.setContentIntent(PendingIntent.getActivity(context, iHashCode, createViewAlarmIntent(context, alarmInstance), 134217728));
        NotificationManagerCompat notificationManagerCompatFrom = NotificationManagerCompat.from(context);
        Notification notificationBuild = localOnly.build();
        notificationManagerCompatFrom.notify(iHashCode, notificationBuild);
        updateUpcomingAlarmGroupNotification(context, -1, notificationBuild);
    }

    static synchronized void showHighPriorityNotification(Context context, AlarmInstance alarmInstance) {
        LogUtils.v("Displaying high priority notification for alarm instance: " + alarmInstance.mId, new Object[0]);
        NotificationCompat.Builder localOnly = new NotificationCompat.Builder(context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION).setShowWhen(false).setContentTitle(context.getString(R.string.alarm_alert_predismiss_title)).setContentText(AlarmUtils.getAlarmText(context, alarmInstance, true)).setColor(ContextCompat.getColor(context, R.color.default_background)).setSmallIcon(R.drawable.stat_notify_alarm).setAutoCancel(false).setSortKey(createSortKey(alarmInstance)).setPriority(1).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(1).setLocalOnly(true);
        if (Utils.isNOrLater()) {
            localOnly.setGroup("1");
        }
        Intent intentCreateStateChangeIntent = AlarmStateManager.createStateChangeIntent(context, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance, 8);
        int iHashCode = alarmInstance.hashCode();
        localOnly.addAction(R.drawable.ic_alarm_off_24dp, context.getString(R.string.alarm_alert_dismiss_text), PendingIntent.getService(context, iHashCode, intentCreateStateChangeIntent, 134217728));
        localOnly.setContentIntent(PendingIntent.getActivity(context, iHashCode, createViewAlarmIntent(context, alarmInstance), 134217728));
        NotificationManagerCompat notificationManagerCompatFrom = NotificationManagerCompat.from(context);
        Notification notificationBuild = localOnly.build();
        notificationManagerCompatFrom.notify(iHashCode, notificationBuild);
        updateUpcomingAlarmGroupNotification(context, -1, notificationBuild);
    }

    @TargetApi(24)
    private static boolean isGroupSummary(Notification notification) {
        return (notification.flags & 512) == 512;
    }

    @TargetApi(24)
    private static Notification getFirstActiveNotification(Context context, String str, int i, Notification notification) {
        for (StatusBarNotification statusBarNotification : ((NotificationManager) context.getSystemService("notification")).getActiveNotifications()) {
            Notification notification2 = statusBarNotification.getNotification();
            if (!isGroupSummary(notification2) && str.equals(notification2.getGroup()) && statusBarNotification.getId() != i && (notification == null || notification2.getSortKey().compareTo(notification.getSortKey()) < 0)) {
                notification = notification2;
            }
        }
        return notification;
    }

    @TargetApi(24)
    private static Notification getActiveGroupSummaryNotification(Context context, String str) {
        for (StatusBarNotification statusBarNotification : ((NotificationManager) context.getSystemService("notification")).getActiveNotifications()) {
            Notification notification = statusBarNotification.getNotification();
            if (isGroupSummary(notification) && str.equals(notification.getGroup())) {
                return notification;
            }
        }
        return null;
    }

    private static void updateUpcomingAlarmGroupNotification(Context context, int i, Notification notification) {
        if (!Utils.isNOrLater()) {
            return;
        }
        NotificationManagerCompat notificationManagerCompatFrom = NotificationManagerCompat.from(context);
        Notification firstActiveNotification = getFirstActiveNotification(context, "1", i, notification);
        if (firstActiveNotification == null) {
            notificationManagerCompatFrom.cancel(ALARM_GROUP_NOTIFICATION_ID);
            return;
        }
        Notification activeGroupSummaryNotification = getActiveGroupSummaryNotification(context, "1");
        if (activeGroupSummaryNotification == null || !Objects.equals(activeGroupSummaryNotification.contentIntent, firstActiveNotification.contentIntent)) {
            notificationManagerCompatFrom.notify(ALARM_GROUP_NOTIFICATION_ID, new NotificationCompat.Builder(context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION).setShowWhen(false).setContentIntent(firstActiveNotification.contentIntent).setColor(ContextCompat.getColor(context, R.color.default_background)).setSmallIcon(R.drawable.stat_notify_alarm).setGroup("1").setGroupSummary(true).setPriority(1).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(1).setLocalOnly(true).build());
        }
    }

    private static void updateMissedAlarmGroupNotification(Context context, int i, Notification notification) {
        if (!Utils.isNOrLater()) {
            return;
        }
        NotificationManagerCompat notificationManagerCompatFrom = NotificationManagerCompat.from(context);
        Notification firstActiveNotification = getFirstActiveNotification(context, MISSED_GROUP_KEY, i, notification);
        if (firstActiveNotification == null) {
            notificationManagerCompatFrom.cancel(ALARM_GROUP_MISSED_NOTIFICATION_ID);
            return;
        }
        Notification activeGroupSummaryNotification = getActiveGroupSummaryNotification(context, MISSED_GROUP_KEY);
        if (activeGroupSummaryNotification == null || !Objects.equals(activeGroupSummaryNotification.contentIntent, firstActiveNotification.contentIntent)) {
            notificationManagerCompatFrom.notify(ALARM_GROUP_MISSED_NOTIFICATION_ID, new NotificationCompat.Builder(context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION).setShowWhen(false).setContentIntent(firstActiveNotification.contentIntent).setColor(ContextCompat.getColor(context, R.color.default_background)).setSmallIcon(R.drawable.stat_notify_alarm).setGroup(MISSED_GROUP_KEY).setGroupSummary(true).setPriority(1).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(1).setLocalOnly(true).build());
        }
    }

    static synchronized void showSnoozeNotification(Context context, AlarmInstance alarmInstance) {
        LogUtils.v("Displaying snoozed notification for alarm instance: " + alarmInstance.mId, new Object[0]);
        NotificationCompat.Builder localOnly = new NotificationCompat.Builder(context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION).setShowWhen(false).setContentTitle(alarmInstance.getLabelOrDefault(context)).setContentText(context.getString(R.string.alarm_alert_snooze_until, AlarmUtils.getFormattedTime(context, alarmInstance.getAlarmTime()))).setColor(ContextCompat.getColor(context, R.color.default_background)).setSmallIcon(R.drawable.stat_notify_alarm).setAutoCancel(false).setSortKey(createSortKey(alarmInstance)).setPriority(2).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(1).setLocalOnly(true);
        if (Utils.isNOrLater()) {
            localOnly.setGroup("1");
        }
        Intent intentCreateStateChangeIntent = AlarmStateManager.createStateChangeIntent(context, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance, 7);
        int iHashCode = alarmInstance.hashCode();
        localOnly.addAction(R.drawable.ic_alarm_off_24dp, context.getString(R.string.alarm_alert_dismiss_text), PendingIntent.getService(context, iHashCode, intentCreateStateChangeIntent, 134217728));
        localOnly.setContentIntent(PendingIntent.getActivity(context, iHashCode, createViewAlarmIntent(context, alarmInstance), 134217728));
        NotificationManagerCompat notificationManagerCompatFrom = NotificationManagerCompat.from(context);
        Notification notificationBuild = localOnly.build();
        notificationManagerCompatFrom.notify(iHashCode, notificationBuild);
        updateUpcomingAlarmGroupNotification(context, -1, notificationBuild);
    }

    static synchronized void showMissedNotification(Context context, AlarmInstance alarmInstance) {
        LogUtils.v("Displaying missed notification for alarm instance: " + alarmInstance.mId, new Object[0]);
        String str = alarmInstance.mLabel;
        String formattedTime = AlarmUtils.getFormattedTime(context, alarmInstance.getAlarmTime());
        NotificationCompat.Builder contentTitle = new NotificationCompat.Builder(context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION).setShowWhen(false).setContentTitle(context.getString(R.string.alarm_missed_title));
        if (!alarmInstance.mLabel.isEmpty()) {
            formattedTime = context.getString(R.string.alarm_missed_text, formattedTime, str);
        }
        NotificationCompat.Builder localOnly = contentTitle.setContentText(formattedTime).setColor(ContextCompat.getColor(context, R.color.default_background)).setSortKey(createSortKey(alarmInstance)).setSmallIcon(R.drawable.stat_notify_alarm).setPriority(1).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(1).setLocalOnly(true);
        if (Utils.isNOrLater()) {
            localOnly.setGroup(MISSED_GROUP_KEY);
        }
        int iHashCode = alarmInstance.hashCode();
        localOnly.setDeleteIntent(PendingIntent.getService(context, iHashCode, AlarmStateManager.createStateChangeIntent(context, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance, 7), 134217728));
        Intent intentCreateIntent = AlarmInstance.createIntent(context, AlarmStateManager.class, alarmInstance.mId);
        intentCreateIntent.putExtra(EXTRA_NOTIFICATION_ID, iHashCode);
        intentCreateIntent.setAction(AlarmStateManager.SHOW_AND_DISMISS_ALARM_ACTION);
        localOnly.setContentIntent(PendingIntent.getBroadcast(context, iHashCode, intentCreateIntent, 134217728));
        NotificationManagerCompat notificationManagerCompatFrom = NotificationManagerCompat.from(context);
        Notification notificationBuild = localOnly.build();
        notificationManagerCompatFrom.notify(iHashCode, notificationBuild);
        updateMissedAlarmGroupNotification(context, -1, notificationBuild);
    }

    static synchronized void showAlarmNotification(Service service, AlarmInstance alarmInstance) {
        LogUtils.v("Displaying alarm notification for alarm instance: " + alarmInstance.mId, new Object[0]);
        Resources resources = service.getResources();
        NotificationCompat.Builder localOnly = new NotificationCompat.Builder(service, NotificationChannelManager.Channel.EVENT_EXPIRED).setContentTitle(alarmInstance.getLabelOrDefault(service)).setContentText(AlarmUtils.getFormattedTime(service, alarmInstance.getAlarmTime())).setColor(ContextCompat.getColor(service, R.color.default_background)).setSmallIcon(R.drawable.stat_notify_alarm).setOngoing(true).setAutoCancel(false).setDefaults(4).setWhen(0L).setCategory(NotificationCompat.CATEGORY_ALARM).setVisibility(1).setLocalOnly(true);
        Intent intentCreateStateChangeIntent = AlarmStateManager.createStateChangeIntent(service, AlarmStateManager.ALARM_SNOOZE_TAG, alarmInstance, 4);
        intentCreateStateChangeIntent.putExtra(AlarmStateManager.FROM_NOTIFICATION_EXTRA, true);
        localOnly.addAction(R.drawable.ic_snooze_24dp, resources.getString(R.string.alarm_alert_snooze_text), PendingIntent.getService(service, ALARM_FIRING_NOTIFICATION_ID, intentCreateStateChangeIntent, 134217728));
        Intent intentCreateStateChangeIntent2 = AlarmStateManager.createStateChangeIntent(service, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance, 7);
        intentCreateStateChangeIntent2.putExtra(AlarmStateManager.FROM_NOTIFICATION_EXTRA, true);
        localOnly.addAction(R.drawable.ic_alarm_off_24dp, resources.getString(R.string.alarm_alert_dismiss_text), PendingIntent.getService(service, ALARM_FIRING_NOTIFICATION_ID, intentCreateStateChangeIntent2, 134217728));
        localOnly.setContentIntent(PendingIntent.getActivity(service, ALARM_FIRING_NOTIFICATION_ID, AlarmInstance.createIntent(service, AlarmActivity.class, alarmInstance.mId), 134217728));
        Intent intentCreateIntent = AlarmInstance.createIntent(service, AlarmActivity.class, alarmInstance.mId);
        intentCreateIntent.setAction("fullscreen_activity");
        intentCreateIntent.setFlags(268697600);
        localOnly.setFullScreenIntent(PendingIntent.getActivity(service, ALARM_FIRING_NOTIFICATION_ID, intentCreateIntent, 134217728), true);
        localOnly.setPriority(2);
        NotificationChannelManager.applyChannel(localOnly, service, NotificationChannelManager.Channel.EVENT_EXPIRED);
        clearNotification(service, alarmInstance);
        service.startForeground(ALARM_FIRING_NOTIFICATION_ID, localOnly.build());
    }

    static synchronized void clearNotification(Context context, AlarmInstance alarmInstance) {
        LogUtils.v("Clearing notifications for alarm instance: " + alarmInstance.mId, new Object[0]);
        NotificationManagerCompat notificationManagerCompatFrom = NotificationManagerCompat.from(context);
        int iHashCode = alarmInstance.hashCode();
        notificationManagerCompatFrom.cancel(iHashCode);
        updateUpcomingAlarmGroupNotification(context, iHashCode, null);
        updateMissedAlarmGroupNotification(context, iHashCode, null);
    }

    static void updateNotification(Context context, AlarmInstance alarmInstance) {
        int i = alarmInstance.mAlarmState;
        if (i == 1) {
            showLowPriorityNotification(context, alarmInstance);
            return;
        }
        if (i != 6) {
            switch (i) {
                case 3:
                    showHighPriorityNotification(context, alarmInstance);
                    break;
                case 4:
                    showSnoozeNotification(context, alarmInstance);
                    break;
                default:
                    LogUtils.d("No notification to update", new Object[0]);
                    break;
            }
            return;
        }
        showMissedNotification(context, alarmInstance);
    }

    static Intent createViewAlarmIntent(Context context, AlarmInstance alarmInstance) {
        long jLongValue = alarmInstance.mAlarmId == null ? -1L : alarmInstance.mAlarmId.longValue();
        return Alarm.createIntent(context, DeskClock.class, jLongValue).putExtra(AlarmClockFragment.SCROLL_TO_ALARM_INTENT_EXTRA, jLongValue).addFlags(268435456);
    }

    private static String createSortKey(AlarmInstance alarmInstance) {
        String str = SORT_KEY_FORMAT.format(alarmInstance.getAlarmTime().getTime());
        if (!(alarmInstance.mAlarmState == 6)) {
            return str;
        }
        return "MISSED " + str;
    }
}
