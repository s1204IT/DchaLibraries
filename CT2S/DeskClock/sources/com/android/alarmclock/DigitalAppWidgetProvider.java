package com.android.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.widget.RemoteViews;
import com.android.deskclock.DeskClock;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.worldclock.CitiesActivity;
import java.util.Locale;

public class DigitalAppWidgetProvider extends AppWidgetProvider {
    private ComponentName mComponentName;
    private PendingIntent mPendingIntent;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        startAlarmOnQuarterHour(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        cancelAlarmOnQuarterHour(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AppWidgetManager appWidgetManager;
        String action = intent.getAction();
        super.onReceive(context, intent);
        if ("com.android.deskclock.ON_QUARTER_HOUR".equals(action) || "android.intent.action.DATE_CHANGED".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.LOCALE_CHANGED".equals(action)) {
            AppWidgetManager appWidgetManager2 = AppWidgetManager.getInstance(context);
            if (appWidgetManager2 != null) {
                int[] appWidgetIds = appWidgetManager2.getAppWidgetIds(getComponentName(context));
                for (int appWidgetId : appWidgetIds) {
                    appWidgetManager2.notifyAppWidgetViewDataChanged(appWidgetId, R.id.digital_appwidget_listview);
                    RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);
                    float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
                    WidgetUtils.setTimeFormat(widget, 0, R.id.the_clock);
                    WidgetUtils.setClockSize(context, widget, ratio);
                    refreshAlarm(context, widget);
                    appWidgetManager2.partiallyUpdateAppWidget(appWidgetId, widget);
                }
            }
            if (!"com.android.deskclock.ON_QUARTER_HOUR".equals(action)) {
                cancelAlarmOnQuarterHour(context);
            }
            startAlarmOnQuarterHour(context);
            return;
        }
        if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(action) || "android.intent.action.SCREEN_ON".equals(action)) {
            AppWidgetManager appWidgetManager3 = AppWidgetManager.getInstance(context);
            if (appWidgetManager3 != null) {
                int[] appWidgetIds2 = appWidgetManager3.getAppWidgetIds(getComponentName(context));
                for (int appWidgetId2 : appWidgetIds2) {
                    RemoteViews widget2 = new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);
                    refreshAlarm(context, widget2);
                    appWidgetManager3.partiallyUpdateAppWidget(appWidgetId2, widget2);
                }
                return;
            }
            return;
        }
        if ("com.android.deskclock.worldclock.update".equals(action) && (appWidgetManager = AppWidgetManager.getInstance(context)) != null) {
            for (int i : appWidgetManager.getAppWidgetIds(getComponentName(context))) {
                appWidgetManager.notifyAppWidgetViewDataChanged(i, R.id.digital_appwidget_listview);
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            float ratio = WidgetUtils.getScaleRatio(context, null, appWidgetId);
            updateClock(context, appWidgetManager, appWidgetId, ratio);
        }
        startAlarmOnQuarterHour(context);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        float ratio = WidgetUtils.getScaleRatio(context, newOptions, appWidgetId);
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        updateClock(context, widgetManager, appWidgetId, ratio);
    }

    private void updateClock(Context context, AppWidgetManager appWidgetManager, int appWidgetId, float ratio) {
        RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.digital_appwidget);
        Bundle newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (newOptions != null && newOptions.getInt("appWidgetCategory", -1) != 2) {
            widget.setOnClickPendingIntent(R.id.digital_appwidget, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) DeskClock.class), 0));
        }
        refreshAlarm(context, widget);
        WidgetUtils.setTimeFormat(widget, 0, R.id.the_clock);
        WidgetUtils.setClockSize(context, widget, ratio);
        CharSequence dateFormat = DateFormat.getBestDateTimePattern(Locale.getDefault(), context.getString(R.string.abbrev_wday_month_day_no_year));
        widget.setCharSequence(R.id.date, "setFormat12Hour", dateFormat);
        widget.setCharSequence(R.id.date, "setFormat24Hour", dateFormat);
        Intent intent = new Intent(context, (Class<?>) DigitalAppWidgetService.class);
        intent.putExtra("appWidgetId", appWidgetId);
        intent.setData(Uri.parse(intent.toUri(1)));
        widget.setRemoteAdapter(R.id.digital_appwidget_listview, intent);
        widget.setPendingIntentTemplate(R.id.digital_appwidget_listview, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) CitiesActivity.class), 0));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.digital_appwidget_listview);
        appWidgetManager.updateAppWidget(appWidgetId, widget);
    }

    protected void refreshAlarm(Context context, RemoteViews widget) {
        String nextAlarm = Utils.getNextAlarm(context);
        if (!TextUtils.isEmpty(nextAlarm)) {
            widget.setTextViewText(R.id.nextAlarm, context.getString(R.string.control_set_alarm_with_existing, nextAlarm));
            widget.setViewVisibility(R.id.nextAlarm, 0);
        } else {
            widget.setViewVisibility(R.id.nextAlarm, 8);
        }
    }

    private void startAlarmOnQuarterHour(Context context) {
        if (context != null) {
            long onQuarterHour = Utils.getAlarmOnQuarterHour();
            PendingIntent quarterlyIntent = getOnQuarterHourPendingIntent(context);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
            if (Utils.isKitKatOrLater()) {
                alarmManager.setExact(1, onQuarterHour, quarterlyIntent);
            } else {
                alarmManager.set(1, onQuarterHour, quarterlyIntent);
            }
        }
    }

    public void cancelAlarmOnQuarterHour(Context context) {
        if (context != null) {
            PendingIntent quarterlyIntent = getOnQuarterHourPendingIntent(context);
            ((AlarmManager) context.getSystemService("alarm")).cancel(quarterlyIntent);
        }
    }

    private PendingIntent getOnQuarterHourPendingIntent(Context context) {
        if (this.mPendingIntent == null) {
            this.mPendingIntent = PendingIntent.getBroadcast(context, 0, new Intent("com.android.deskclock.ON_QUARTER_HOUR"), 268435456);
        }
        return this.mPendingIntent;
    }

    private ComponentName getComponentName(Context context) {
        if (this.mComponentName == null) {
            this.mComponentName = new ComponentName(context, getClass());
        }
        return this.mComponentName;
    }
}
