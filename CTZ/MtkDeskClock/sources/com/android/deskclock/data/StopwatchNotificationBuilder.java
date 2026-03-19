package com.android.deskclock.data;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.RemoteViews;
import com.android.deskclock.NotificationChannelManager;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.stopwatch.StopwatchService;
import java.util.ArrayList;
import java.util.Iterator;

class StopwatchNotificationBuilder {
    StopwatchNotificationBuilder() {
    }

    public Notification build(Context context, NotificationModel notificationModel, Stopwatch stopwatch) {
        PendingIntent service = PendingIntent.getService(context, 0, new Intent(context, (Class<?>) StopwatchService.class).setAction(StopwatchService.ACTION_SHOW_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification), 1207959552);
        boolean zIsRunning = stopwatch.isRunning();
        String packageName = context.getPackageName();
        Resources resources = context.getResources();
        long jElapsedRealtime = SystemClock.elapsedRealtime() - stopwatch.getTotalTime();
        RemoteViews remoteViews = new RemoteViews(packageName, R.layout.chronometer_notif_content);
        remoteViews.setChronometer(R.id.chronometer, jElapsedRealtime, null, zIsRunning);
        ArrayList arrayList = new ArrayList(2);
        if (zIsRunning) {
            arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_pause_24dp, resources.getText(R.string.sw_pause_button), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) StopwatchService.class).setAction(StopwatchService.ACTION_PAUSE_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification))).build());
            if (DataModel.getDataModel().canAddMoreLaps()) {
                arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_sw_lap_24dp, resources.getText(R.string.sw_lap_button), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) StopwatchService.class).setAction(StopwatchService.ACTION_LAP_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification))).build());
            }
            int size = DataModel.getDataModel().getLaps().size();
            if (size > 0) {
                remoteViews.setTextViewText(R.id.state, resources.getString(R.string.sw_notification_lap_number, Integer.valueOf(size + 1)));
                remoteViews.setViewVisibility(R.id.state, 0);
            } else {
                remoteViews.setViewVisibility(R.id.state, 8);
            }
        } else {
            arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_start_24dp, resources.getText(R.string.sw_start_button), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) StopwatchService.class).setAction(StopwatchService.ACTION_START_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification))).build());
            arrayList.add(new NotificationCompat.Action.Builder(R.drawable.ic_reset_24dp, resources.getText(R.string.sw_reset_button), Utils.pendingServiceIntent(context, new Intent(context, (Class<?>) StopwatchService.class).setAction(StopwatchService.ACTION_RESET_STOPWATCH).putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_notification))).build());
            remoteViews.setTextViewText(R.id.state, resources.getString(R.string.swn_paused));
            remoteViews.setViewVisibility(R.id.state, 0);
        }
        NotificationCompat.Builder color = new NotificationCompat.Builder(context).setLocalOnly(true).setOngoing(zIsRunning).setCustomContentView(remoteViews).setContentIntent(service).setAutoCancel(stopwatch.isPaused()).setPriority(2).setSmallIcon(R.drawable.stat_notify_stopwatch).setStyle(new NotificationCompat.DecoratedCustomViewStyle()).setColor(ContextCompat.getColor(context, R.color.default_background));
        NotificationChannelManager.applyChannel(color, context, NotificationChannelManager.Channel.DEFAULT_NOTIFICATION);
        if (Utils.isNOrLater()) {
            color.setGroup(notificationModel.getStopwatchNotificationGroupKey());
        }
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            color.addAction((NotificationCompat.Action) it.next());
        }
        return color.build();
    }
}
