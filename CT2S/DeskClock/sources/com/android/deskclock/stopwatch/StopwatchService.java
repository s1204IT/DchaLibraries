package com.android.deskclock.stopwatch;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

public class StopwatchService extends Service {
    private long mElapsedTime;
    private boolean mLoadApp;
    private NotificationManager mNotificationManager;
    private int mNumLaps;
    private long mStartTime;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        this.mNumLaps = 0;
        this.mElapsedTime = 0L;
        this.mStartTime = 0L;
        this.mLoadApp = false;
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return 2;
        }
        if (this.mStartTime == 0 || this.mElapsedTime == 0 || this.mNumLaps == 0) {
            readFromSharedPrefs();
        }
        String actionType = intent.getAction();
        long actionTime = intent.getLongExtra("message_time", Utils.getTimeNow());
        boolean showNotif = intent.getBooleanExtra("show_notification", true);
        if (actionType.equals("start_stopwatch")) {
            this.mStartTime = actionTime;
            writeSharedPrefsStarted(this.mStartTime, showNotif);
            if (showNotif) {
                setNotification(this.mStartTime - this.mElapsedTime, true, this.mNumLaps);
            } else {
                saveNotification(this.mStartTime - this.mElapsedTime, true, this.mNumLaps);
            }
        } else if (actionType.equals("lap_stopwatch")) {
            this.mNumLaps++;
            long lapTimeElapsed = (actionTime - this.mStartTime) + this.mElapsedTime;
            writeSharedPrefsLap(lapTimeElapsed, showNotif);
            if (showNotif) {
                setNotification(this.mStartTime - this.mElapsedTime, true, this.mNumLaps);
            } else {
                saveNotification(this.mStartTime - this.mElapsedTime, true, this.mNumLaps);
            }
        } else if (actionType.equals("stop_stopwatch")) {
            this.mElapsedTime += actionTime - this.mStartTime;
            writeSharedPrefsStopped(this.mElapsedTime, showNotif);
            if (showNotif) {
                setNotification(actionTime - this.mElapsedTime, false, this.mNumLaps);
            } else {
                saveNotification(this.mElapsedTime, false, this.mNumLaps);
            }
        } else if (actionType.equals("reset_stopwatch")) {
            this.mLoadApp = false;
            writeSharedPrefsReset(showNotif);
            clearSavedNotification();
            stopSelf();
        } else if (actionType.equals("reset_and_launch_stopwatch")) {
            this.mLoadApp = true;
            writeSharedPrefsReset(showNotif);
            clearSavedNotification();
            closeNotificationShade();
            stopSelf();
        } else if (actionType.equals("share_stopwatch")) {
            closeNotificationShade();
            Intent shareIntent = new Intent("android.intent.action.SEND");
            shareIntent.setType("text/plain");
            shareIntent.putExtra("android.intent.extra.SUBJECT", Stopwatches.getShareTitle(getApplicationContext()));
            shareIntent.putExtra("android.intent.extra.TEXT", Stopwatches.buildShareResults(getApplicationContext(), this.mElapsedTime, readLapsFromPrefs()));
            Intent chooserIntent = Intent.createChooser(shareIntent, null);
            chooserIntent.addFlags(268435456);
            getApplication().startActivity(chooserIntent);
        } else if (actionType.equals("show_notification")) {
            if (!showSavedNotification()) {
                stopSelf();
            }
        } else if (actionType.equals("kill_notification")) {
            this.mNotificationManager.cancel(2147483646);
        }
        return 1;
    }

    @Override
    public void onDestroy() {
        this.mNotificationManager.cancel(2147483646);
        clearSavedNotification();
        this.mNumLaps = 0;
        this.mElapsedTime = 0L;
        this.mStartTime = 0L;
        if (this.mLoadApp) {
            Intent activityIntent = new Intent(getApplicationContext(), (Class<?>) DeskClock.class);
            activityIntent.addFlags(268435456);
            activityIntent.putExtra("deskclock.select.tab", 3);
            startActivity(activityIntent);
            this.mLoadApp = false;
        }
    }

    private void setNotification(long clockBaseTime, boolean clockRunning, int numLaps) {
        Context context = getApplicationContext();
        Intent intent = new Intent(context, (Class<?>) DeskClock.class);
        intent.addFlags(268435456);
        intent.putExtra("deskclock.select.tab", 3);
        intent.addCategory("stopwatch");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 1207959552);
        RemoteViews remoteViewsCollapsed = new RemoteViews(getPackageName(), R.layout.stopwatch_notif_collapsed);
        remoteViewsCollapsed.setOnClickPendingIntent(R.id.swn_collapsed_hitspace, pendingIntent);
        remoteViewsCollapsed.setChronometer(R.id.swn_collapsed_chronometer, clockBaseTime, null, clockRunning);
        remoteViewsCollapsed.setImageViewResource(R.id.notification_icon, R.drawable.stat_notify_stopwatch);
        RemoteViews remoteViewsExpanded = new RemoteViews(getPackageName(), R.layout.stopwatch_notif_expanded);
        remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_expanded_hitspace, pendingIntent);
        remoteViewsExpanded.setChronometer(R.id.swn_expanded_chronometer, clockBaseTime, null, clockRunning);
        remoteViewsExpanded.setImageViewResource(R.id.notification_icon, R.drawable.stat_notify_stopwatch);
        if (clockRunning) {
            remoteViewsExpanded.setTextViewText(R.id.swn_left_button, getResources().getText(R.string.sw_lap_button));
            Intent leftButtonIntent = new Intent(context, (Class<?>) StopwatchService.class);
            leftButtonIntent.setAction("lap_stopwatch");
            remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_left_button, PendingIntent.getService(context, 0, leftButtonIntent, 0));
            remoteViewsExpanded.setTextViewCompoundDrawablesRelative(R.id.swn_left_button, R.drawable.ic_notify_lap, 0, 0, 0);
            remoteViewsExpanded.setTextViewText(R.id.swn_right_button, getResources().getText(R.string.sw_stop_button));
            Intent rightButtonIntent = new Intent(context, (Class<?>) StopwatchService.class);
            rightButtonIntent.setAction("stop_stopwatch");
            remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_right_button, PendingIntent.getService(context, 0, rightButtonIntent, 0));
            remoteViewsExpanded.setTextViewCompoundDrawablesRelative(R.id.swn_right_button, R.drawable.ic_notify_stop, 0, 0, 0);
            if (numLaps > 0) {
                String lapText = String.format(context.getString(R.string.sw_notification_lap_number), Integer.valueOf(numLaps));
                remoteViewsCollapsed.setTextViewText(R.id.swn_collapsed_laps, lapText);
                remoteViewsCollapsed.setViewVisibility(R.id.swn_collapsed_laps, 0);
                remoteViewsExpanded.setTextViewText(R.id.swn_expanded_laps, lapText);
                remoteViewsExpanded.setViewVisibility(R.id.swn_expanded_laps, 0);
            } else {
                remoteViewsCollapsed.setViewVisibility(R.id.swn_collapsed_laps, 8);
                remoteViewsExpanded.setViewVisibility(R.id.swn_expanded_laps, 8);
            }
        } else {
            remoteViewsExpanded.setTextViewText(R.id.swn_left_button, getResources().getText(R.string.sw_reset_button));
            Intent leftButtonIntent2 = new Intent(context, (Class<?>) StopwatchService.class);
            leftButtonIntent2.setAction("reset_and_launch_stopwatch");
            remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_left_button, PendingIntent.getService(context, 0, leftButtonIntent2, 0));
            remoteViewsExpanded.setTextViewCompoundDrawablesRelative(R.id.swn_left_button, R.drawable.ic_notify_reset, 0, 0, 0);
            remoteViewsExpanded.setTextViewText(R.id.swn_right_button, getResources().getText(R.string.sw_start_button));
            Intent rightButtonIntent2 = new Intent(context, (Class<?>) StopwatchService.class);
            rightButtonIntent2.setAction("start_stopwatch");
            remoteViewsExpanded.setOnClickPendingIntent(R.id.swn_right_button, PendingIntent.getService(context, 0, rightButtonIntent2, 0));
            remoteViewsExpanded.setTextViewCompoundDrawablesRelative(R.id.swn_right_button, R.drawable.ic_notify_start, 0, 0, 0);
            remoteViewsCollapsed.setTextViewText(R.id.swn_collapsed_laps, getString(R.string.swn_stopped));
            remoteViewsCollapsed.setViewVisibility(R.id.swn_collapsed_laps, 0);
            remoteViewsExpanded.setTextViewText(R.id.swn_expanded_laps, getString(R.string.swn_stopped));
            remoteViewsExpanded.setViewVisibility(R.id.swn_expanded_laps, 0);
        }
        Intent dismissIntent = new Intent(context, (Class<?>) StopwatchService.class);
        dismissIntent.setAction("reset_stopwatch");
        Notification notification = new Notification.Builder(context).setAutoCancel(!clockRunning).setContent(remoteViewsCollapsed).setOngoing(clockRunning).setDeleteIntent(PendingIntent.getService(context, 0, dismissIntent, 0)).setSmallIcon(R.drawable.ic_tab_stopwatch_activated).setPriority(2).setLocalOnly(true).build();
        notification.bigContentView = remoteViewsExpanded;
        this.mNotificationManager.notify(2147483646, notification);
    }

    private void saveNotification(long clockTime, boolean clockRunning, int numLaps) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        if (clockRunning) {
            editor.putLong("notif_clock_base", clockTime);
            editor.putLong("notif_clock_elapsed", -1L);
            editor.putBoolean("notif_clock_running", true);
        } else {
            editor.putLong("notif_clock_elapsed", clockTime);
            editor.putLong("notif_clock_base", -1L);
            editor.putBoolean("notif_clock_running", false);
        }
        editor.putBoolean("sw_update_circle", false);
        editor.apply();
    }

    private boolean showSavedNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long clockBaseTime = prefs.getLong("notif_clock_base", -1L);
        long clockElapsedTime = prefs.getLong("notif_clock_elapsed", -1L);
        boolean clockRunning = prefs.getBoolean("notif_clock_running", false);
        int numLaps = prefs.getInt("sw_lap_num", -1);
        if (clockBaseTime == -1) {
            if (clockElapsedTime == -1) {
                return false;
            }
            this.mElapsedTime = clockElapsedTime;
            clockBaseTime = Utils.getTimeNow() - clockElapsedTime;
        }
        setNotification(clockBaseTime, clockRunning, numLaps);
        return true;
    }

    private void clearSavedNotification() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("notif_clock_base");
        editor.remove("notif_clock_running");
        editor.remove("notif_clock_elapsed");
        editor.apply();
    }

    private void closeNotificationShade() {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
        sendBroadcast(intent);
    }

    private void readFromSharedPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        this.mStartTime = prefs.getLong("sw_start_time", 0L);
        this.mElapsedTime = prefs.getLong("sw_accum_time", 0L);
        this.mNumLaps = prefs.getInt("sw_lap_num", 0);
    }

    private long[] readLapsFromPrefs() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int numLaps = prefs.getInt("sw_lap_num", 0);
        long[] laps = new long[numLaps];
        long prevLapElapsedTime = 0;
        for (int lap_i = 0; lap_i < numLaps; lap_i++) {
            String key = "sw_lap_time_" + Integer.toString(lap_i + 1);
            long lap = prefs.getLong(key, 0L);
            if (lap == prevLapElapsedTime && lap_i == numLaps - 1) {
                lap = this.mElapsedTime;
            }
            laps[(numLaps - lap_i) - 1] = lap - prevLapElapsedTime;
            prevLapElapsedTime = lap;
        }
        return laps;
    }

    private void writeToSharedPrefs(Long startTime, Long lapTimeElapsed, Long elapsedTime, Integer state, boolean updateCircle) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        if (startTime != null) {
            editor.putLong("sw_start_time", startTime.longValue());
            this.mStartTime = startTime.longValue();
        }
        if (lapTimeElapsed != null) {
            int numLaps = prefs.getInt("sw_lap_num", 0);
            if (numLaps == 0) {
                this.mNumLaps++;
                numLaps++;
            }
            editor.putLong("sw_lap_time_" + Integer.toString(numLaps), lapTimeElapsed.longValue());
            int numLaps2 = numLaps + 1;
            editor.putLong("sw_lap_time_" + Integer.toString(numLaps2), lapTimeElapsed.longValue());
            editor.putInt("sw_lap_num", numLaps2);
        }
        if (elapsedTime != null) {
            editor.putLong("sw_accum_time", elapsedTime.longValue());
            this.mElapsedTime = elapsedTime.longValue();
        }
        if (state != null) {
            if (state.intValue() == 0) {
                editor.putInt("sw_state", 0);
            } else if (state.intValue() == 1) {
                editor.putInt("sw_state", 1);
            } else if (state.intValue() == 2) {
                editor.putInt("sw_state", 2);
            }
        }
        editor.putBoolean("sw_update_circle", updateCircle);
        editor.apply();
    }

    private void writeSharedPrefsStarted(long startTime, boolean updateCircle) {
        writeToSharedPrefs(Long.valueOf(startTime), null, null, 1, updateCircle);
        if (updateCircle) {
            long time = Utils.getTimeNow();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            long intervalStartTime = prefs.getLong("sw_ctv_interval_start", -1L);
            if (intervalStartTime != -1) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("sw_ctv_interval_start", time);
                editor.putBoolean("sw_ctv_paused", false);
                editor.apply();
            }
        }
    }

    private void writeSharedPrefsLap(long lapTimeElapsed, boolean updateCircle) {
        writeToSharedPrefs(null, Long.valueOf(lapTimeElapsed), null, null, updateCircle);
        if (updateCircle) {
            long time = Utils.getTimeNow();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();
            long[] laps = readLapsFromPrefs();
            int numLaps = laps.length;
            long lapTime = laps[1];
            if (numLaps == 2) {
                editor.putLong("sw_ctv_interval", lapTime);
            } else {
                editor.putLong("sw_ctv_marker_time", lapTime);
            }
            editor.putLong("sw_ctv_accum_time", 0L);
            if (numLaps < 99) {
                editor.putLong("sw_ctv_interval_start", time);
                editor.putBoolean("sw_ctv_paused", false);
            } else {
                editor.putLong("sw_ctv_interval_start", -1L);
            }
            editor.apply();
        }
    }

    private void writeSharedPrefsStopped(long elapsedTime, boolean updateCircle) {
        writeToSharedPrefs(null, null, Long.valueOf(elapsedTime), 2, updateCircle);
        if (updateCircle) {
            long time = Utils.getTimeNow();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            long accumulatedTime = prefs.getLong("sw_ctv_accum_time", 0L);
            long intervalStartTime = prefs.getLong("sw_ctv_interval_start", -1L);
            long accumulatedTime2 = accumulatedTime + (time - intervalStartTime);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong("sw_ctv_accum_time", accumulatedTime2);
            editor.putBoolean("sw_ctv_paused", true);
            editor.putLong("sw_ctv_current_interval", accumulatedTime2);
            editor.apply();
        }
    }

    private void writeSharedPrefsReset(boolean updateCircle) {
        writeToSharedPrefs(null, null, null, 0, updateCircle);
    }
}
