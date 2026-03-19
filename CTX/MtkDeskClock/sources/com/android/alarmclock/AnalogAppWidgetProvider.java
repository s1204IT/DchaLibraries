package com.android.alarmclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;

public class AnalogAppWidgetProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager == null) {
            return;
        }
        DataModel.getDataModel().updateWidgetCount(getClass(), appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass())).length, R.string.category_analog_widget);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        super.onUpdate(context, appWidgetManager, iArr);
        for (int i : iArr) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.analog_appwidget);
            if (Utils.isWidgetClickable(appWidgetManager, i)) {
                remoteViews.setOnClickPendingIntent(R.id.analog_appwidget, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) DeskClock.class), 0));
            }
            appWidgetManager.updateAppWidget(i, remoteViews);
        }
    }
}
