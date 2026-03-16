package com.android.alarmclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;

public class AnalogAppWidgetProvider extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.appwidget.action.APPWIDGET_UPDATE".equals(action)) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.analog_appwidget);
            views.setOnClickPendingIntent(R.id.analog_appwidget, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) DeskClock.class), 0));
            int[] appWidgetIds = intent.getIntArrayExtra("appWidgetIds");
            AppWidgetManager gm = AppWidgetManager.getInstance(context);
            gm.updateAppWidget(appWidgetIds, views);
        }
    }
}
