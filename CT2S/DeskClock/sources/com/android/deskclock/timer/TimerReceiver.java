package com.android.deskclock.timer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.TimerRingService;
import com.android.deskclock.Utils;
import java.util.ArrayList;

public class TimerReceiver extends BroadcastReceiver {
    ArrayList<TimerObj> mTimers;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("TimerReceiver", "Received intent " + intent.toString());
        String actionType = intent.getAction();
        if ("notif_in_use_cancel".equals(actionType)) {
            cancelInUseNotification(context);
            return;
        }
        if (this.mTimers == null) {
            this.mTimers = new ArrayList<>();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        TimerObj.getTimersFromSharedPrefs(prefs, this.mTimers);
        if ("notif_in_use_show".equals(actionType)) {
            showInUseNotification(context);
            return;
        }
        if ("notif_times_up_show".equals(actionType)) {
            showTimesUpNotification(context);
            return;
        }
        if ("notif_times_up_cancel".equals(actionType)) {
            cancelTimesUpNotification(context);
            return;
        }
        if (!intent.hasExtra("timer.intent.extra")) {
            Log.e("TimerReceiver", "got intent without Timer data");
            return;
        }
        int timerId = intent.getIntExtra("timer.intent.extra", -1);
        if (timerId == -1) {
            Log.d("TimerReceiver", "OnReceive:intent without Timer data for " + actionType);
        }
        TimerObj t = Timers.findTimer(this.mTimers, timerId);
        if ("times_up".equals(actionType)) {
            if (t == null) {
                Log.d("TimerReceiver", " timer not found in list - do nothing");
                return;
            }
            t.mState = 3;
            t.writeToSharedPref(prefs);
            Log.d("TimerReceiver", "playing ringtone");
            Intent si = new Intent();
            si.setClass(context, TimerRingService.class);
            context.startService(si);
            if (getNextRunningTimer(this.mTimers, false, Utils.getTimeNow()) == null) {
                cancelInUseNotification(context);
            } else {
                showInUseNotification(context);
            }
            Intent timersAlert = new Intent(context, (Class<?>) TimerAlertFullScreen.class);
            timersAlert.setFlags(268697600);
            context.startActivity(timersAlert);
        } else if ("timer_reset".equals(actionType) || "delete_timer".equals(actionType) || "timer_done".equals(actionType)) {
            stopRingtoneIfNoTimesup(context);
        } else if ("notif_times_up_stop".equals(actionType)) {
            if (t == null) {
                Log.d("TimerReceiver", "timer to stop not found in list - do nothing");
                return;
            }
            if (t.mState != 3) {
                Log.d("TimerReceiver", "action to stop but timer not in times-up state - do nothing");
                return;
            }
            t.mState = t.getDeleteAfterUse() ? 6 : 5;
            long j = t.mSetupLength;
            t.mOriginalLength = j;
            t.mTimeLeft = j;
            t.writeToSharedPref(prefs);
            prefs.edit().putBoolean("from_notification", true).apply();
            cancelTimesUpNotification(context, t);
            if (t.getDeleteAfterUse()) {
                t.deleteFromSharedPref(prefs);
            }
            stopRingtoneIfNoTimesup(context);
        } else if ("notif_times_up_plus_one".equals(actionType)) {
            if (t == null) {
                Log.d("TimerReceiver", "timer to +1m not found in list - do nothing");
                return;
            }
            if (t.mState != 3) {
                Log.d("TimerReceiver", "action to +1m but timer not in times up state - do nothing");
                return;
            }
            t.mState = 1;
            t.mStartTime = Utils.getTimeNow();
            t.mOriginalLength = 60000L;
            t.mTimeLeft = 60000L;
            t.writeToSharedPref(prefs);
            prefs.edit().putBoolean("from_notification", true).apply();
            cancelTimesUpNotification(context, t);
            if (!prefs.getBoolean("notif_app_open", false)) {
                showInUseNotification(context);
            }
            stopRingtoneIfNoTimesup(context);
        } else if ("timer_update".equals(actionType)) {
            if (t == null) {
                Log.d("TimerReceiver", " timer to update not found in list - do nothing");
                return;
            } else if (t.mState == 3) {
                cancelTimesUpNotification(context, t);
                showTimesUpNotification(context, t);
            }
        }
        updateNextTimesup(context);
    }

    private void stopRingtoneIfNoTimesup(Context context) {
        if (Timers.findExpiredTimer(this.mTimers) == null) {
            Log.d("TimerReceiver", "stopping ringtone");
            Intent si = new Intent();
            si.setClass(context, TimerRingService.class);
            context.stopService(si);
        }
    }

    private void updateNextTimesup(Context context) {
        TimerObj t = getNextRunningTimer(this.mTimers, false, Utils.getTimeNow());
        long nextTimesup = t == null ? -1L : t.getTimesupTime();
        int timerId = t == null ? -1 : t.mTimerId;
        Intent intent = new Intent();
        intent.setAction("times_up");
        intent.setClass(context, TimerReceiver.class);
        intent.addFlags(268435456);
        if (!this.mTimers.isEmpty()) {
            intent.putExtra("timer.intent.extra", timerId);
        }
        AlarmManager mngr = (AlarmManager) context.getSystemService("alarm");
        PendingIntent p = PendingIntent.getBroadcast(context, 0, intent, 1207959552);
        if (t != null) {
            if (Utils.isKitKatOrLater()) {
                mngr.setExact(2, nextTimesup, p);
            } else {
                mngr.set(2, nextTimesup, p);
            }
            Log.d("TimerReceiver", "Setting times up to " + nextTimesup);
            return;
        }
        mngr.cancel(p);
        Log.v("TimerReceiver", "no next times up");
    }

    private void showInUseNotification(Context context) {
        String title;
        String contentText;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean appOpen = prefs.getBoolean("notif_app_open", false);
        ArrayList<TimerObj> timersInUse = Timers.timersInUse(this.mTimers);
        int numTimersInUse = timersInUse.size();
        if (!appOpen && numTimersInUse != 0) {
            Long nextBroadcastTime = null;
            long now = Utils.getTimeNow();
            if (timersInUse.size() == 1) {
                TimerObj timer = timersInUse.get(0);
                boolean timerIsTicking = timer.isTicking();
                String label = timer.getLabelOrDefault(context);
                title = timerIsTicking ? label : context.getString(R.string.timer_stopped);
                long timeLeft = timerIsTicking ? timer.getTimesupTime() - now : timer.mTimeLeft;
                contentText = buildTimeRemaining(context, timeLeft);
                if (timerIsTicking && timeLeft > 60000) {
                    nextBroadcastTime = Long.valueOf(getBroadcastTime(now, timeLeft));
                }
            } else {
                TimerObj timer2 = getNextRunningTimer(timersInUse, false, now);
                if (timer2 == null) {
                    title = String.format(context.getString(R.string.timers_stopped), Integer.valueOf(numTimersInUse));
                    contentText = context.getString(R.string.all_timers_stopped_notif);
                } else {
                    title = String.format(context.getString(R.string.timers_in_use), Integer.valueOf(numTimersInUse));
                    long completionTime = timer2.getTimesupTime();
                    long timeLeft2 = completionTime - now;
                    contentText = String.format(context.getString(R.string.next_timer_notif), buildTimeRemaining(context, timeLeft2));
                    if (timeLeft2 <= 60000) {
                        TimerObj timerWithUpdate = getNextRunningTimer(timersInUse, true, now);
                        if (timerWithUpdate != null) {
                            long completionTime2 = timerWithUpdate.getTimesupTime();
                            nextBroadcastTime = Long.valueOf(getBroadcastTime(now, completionTime2 - now));
                        }
                    } else {
                        nextBroadcastTime = Long.valueOf(getBroadcastTime(now, timeLeft2));
                    }
                }
            }
            showCollapsedNotificationWithNext(context, title, contentText, nextBroadcastTime);
        }
    }

    private long getBroadcastTime(long now, long timeUntilBroadcast) {
        long seconds = timeUntilBroadcast / 1000;
        return ((seconds - ((seconds / 60) * 60)) * 1000) + now;
    }

    private void showCollapsedNotificationWithNext(Context context, String title, String text, Long nextBroadcastTime) {
        Intent activityIntent = new Intent(context, (Class<?>) DeskClock.class);
        activityIntent.addFlags(268435456);
        activityIntent.putExtra("deskclock.select.tab", 2);
        PendingIntent pendingActivityIntent = PendingIntent.getActivity(context, 0, activityIntent, 1207959552);
        showCollapsedNotification(context, title, text, 1, pendingActivityIntent, 2147483645, false);
        if (nextBroadcastTime != null) {
            Intent nextBroadcast = new Intent();
            nextBroadcast.setAction("notif_in_use_show");
            PendingIntent pendingNextBroadcast = PendingIntent.getBroadcast(context, 0, nextBroadcast, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
            if (Utils.isKitKatOrLater()) {
                alarmManager.setExact(3, nextBroadcastTime.longValue(), pendingNextBroadcast);
            } else {
                alarmManager.set(3, nextBroadcastTime.longValue(), pendingNextBroadcast);
            }
        }
    }

    private static void showCollapsedNotification(Context context, String title, String text, int priority, PendingIntent pendingIntent, int notificationId, boolean showTicker) {
        Notification.Builder builder = new Notification.Builder(context).setAutoCancel(false).setContentTitle(title).setContentText(text).setDeleteIntent(pendingIntent).setOngoing(true).setPriority(priority).setShowWhen(false).setSmallIcon(R.drawable.stat_notify_timer).setCategory("alarm").setVisibility(1).setLocalOnly(true);
        if (showTicker) {
            builder.setTicker(text);
        }
        Notification notification = builder.build();
        notification.contentIntent = pendingIntent;
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        notificationManager.notify(notificationId, notification);
    }

    private String buildTimeRemaining(Context context, long timeLeft) {
        if (timeLeft < 0) {
            Log.v("TimerReceiver", "Will not show notification for timer already expired.");
            return null;
        }
        long seconds = timeLeft / 1000;
        long minutes = seconds / 60;
        long j = seconds - (60 * minutes);
        long hours = minutes / 60;
        long minutes2 = minutes - (60 * hours);
        if (hours > 99) {
            hours = 0;
        }
        String hourSeq = hours == 0 ? "" : hours == 1 ? context.getString(R.string.hour) : context.getString(R.string.hours, Long.toString(hours));
        String minSeq = minutes2 == 0 ? "" : minutes2 == 1 ? context.getString(R.string.minute) : context.getString(R.string.minutes, Long.toString(minutes2));
        boolean dispHour = hours > 0;
        boolean dispMinute = minutes2 > 0;
        int index = (dispHour ? 1 : 0) | (dispMinute ? 2 : 0);
        String[] formats = context.getResources().getStringArray(R.array.timer_notifications);
        return String.format(formats[index], hourSeq, minSeq);
    }

    private TimerObj getNextRunningTimer(ArrayList<TimerObj> timers, boolean requireNextUpdate, long now) {
        long nextTimesup = Long.MAX_VALUE;
        boolean nextTimerFound = false;
        TimerObj t = null;
        for (TimerObj tmp : timers) {
            if (tmp.mState == 1) {
                long timesupTime = tmp.getTimesupTime();
                long timeLeft = timesupTime - now;
                if (timesupTime < nextTimesup && (!requireNextUpdate || timeLeft > 60)) {
                    nextTimesup = timesupTime;
                    nextTimerFound = true;
                    t = tmp;
                }
            }
        }
        if (nextTimerFound) {
            return t;
        }
        return null;
    }

    private void cancelInUseNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        notificationManager.cancel(2147483645);
    }

    private void showTimesUpNotification(Context context) {
        for (TimerObj timerObj : Timers.timersInTimesUp(this.mTimers)) {
            showTimesUpNotification(context, timerObj);
        }
    }

    private void showTimesUpNotification(Context context, TimerObj timerObj) {
        PendingIntent contentIntent = PendingIntent.getActivity(context, timerObj.mTimerId, new Intent(context, (Class<?>) TimerAlertFullScreen.class).putExtra("timer.intent.extra", timerObj.mTimerId), 134217728);
        PendingIntent addOneMinuteAction = PendingIntent.getBroadcast(context, timerObj.mTimerId, new Intent("notif_times_up_plus_one").putExtra("timer.intent.extra", timerObj.mTimerId), 134217728);
        PendingIntent stopIntent = PendingIntent.getBroadcast(context, timerObj.mTimerId, new Intent("notif_times_up_stop").putExtra("timer.intent.extra", timerObj.mTimerId), 134217728);
        Notification notification = new Notification.Builder(context).setContentIntent(contentIntent).addAction(R.drawable.ic_menu_add, context.getResources().getString(R.string.timer_plus_1_min), addOneMinuteAction).addAction(timerObj.getDeleteAfterUse() ? android.R.drawable.ic_menu_close_clear_cancel : R.drawable.ic_notify_stop, timerObj.getDeleteAfterUse() ? context.getResources().getString(R.string.timer_done) : context.getResources().getString(R.string.timer_stop), stopIntent).setContentTitle(timerObj.getLabelOrDefault(context)).setContentText(context.getResources().getString(R.string.timer_times_up)).setSmallIcon(R.drawable.stat_notify_timer).setOngoing(true).setAutoCancel(false).setPriority(2).setDefaults(4).setWhen(0L).setCategory("alarm").setVisibility(1).setLocalOnly(true).build();
        ((NotificationManager) context.getSystemService("notification")).notify(timerObj.mTimerId, notification);
        Log.v("TimerReceiver", "Setting times-up notification for " + timerObj.getLabelOrDefault(context) + " #" + timerObj.mTimerId);
    }

    private void cancelTimesUpNotification(Context context) {
        for (TimerObj timerObj : Timers.timersInTimesUp(this.mTimers)) {
            cancelTimesUpNotification(context, timerObj);
        }
    }

    private void cancelTimesUpNotification(Context context, TimerObj timerObj) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        notificationManager.cancel(timerObj.mTimerId);
        Log.v("TimerReceiver", "Canceling times-up notification for " + timerObj.getLabelOrDefault(context) + " #" + timerObj.mTimerId);
    }
}
