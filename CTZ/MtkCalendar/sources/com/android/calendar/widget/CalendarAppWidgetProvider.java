package com.android.calendar.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.CalendarContract;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.calendar.AllInOneActivity;
import com.android.calendar.EventInfoActivity;
import com.android.calendar.R;
import com.android.calendar.Utils;

public class CalendarAppWidgetProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("CalendarAppWidgetProvider", "AppWidgetProvider got the intent: " + intent.toString());
        if (Utils.getWidgetUpdateAction(context).equals(action) || action.equals("android.intent.action.TIMEZONE_CHANGED")) {
            try {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                performUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(getComponentName(context)), null);
                return;
            } catch (SecurityException e) {
                Log.d("CalendarAppWidgetProvider", "Security exception, permissions denied");
                return;
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onDisabled(Context context) {
        try {
            ((AlarmManager) context.getSystemService("alarm")).cancel(getUpdateIntent(context));
        } catch (SecurityException e) {
            Log.d("CalendarAppWidgetProvider", "Security exception, permissions denied");
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        try {
            performUpdate(context, appWidgetManager, iArr, null);
        } catch (SecurityException e) {
            Log.d("CalendarAppWidgetProvider", "Security exception, permissions denied");
        }
    }

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, (Class<?>) CalendarAppWidgetProvider.class);
    }

    private void performUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr, long[] jArr) {
        for (int i : iArr) {
            Log.d("CalendarAppWidgetProvider", "Building widget update...");
            Intent intent = new Intent(context, (Class<?>) CalendarAppWidgetService.class);
            intent.putExtra("appWidgetId", i);
            if (jArr != null) {
                intent.putExtra("com.android.calendar.EXTRA_EVENT_IDS", jArr);
            }
            intent.setData(Uri.parse(intent.toUri(1)));
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.appwidget);
            Time time = new Time(Utils.getTimeZone(context, null));
            time.setToNow();
            long millis = time.toMillis(true);
            String dayOfWeekString = DateUtils.getDayOfWeekString(time.weekDay + 1, 20);
            String dateRange = Utils.formatDateRange(context, millis, millis, 524312);
            remoteViews.setTextViewText(R.id.day_of_week, dayOfWeekString);
            remoteViews.setTextViewText(R.id.date, dateRange);
            remoteViews.setRemoteAdapter(i, R.id.events_list, intent);
            appWidgetManager.notifyAppWidgetViewDataChanged(i, R.id.events_list);
            Intent intent2 = new Intent("android.intent.action.VIEW");
            intent2.setClass(context, AllInOneActivity.class);
            intent2.setData(Uri.parse("content://com.android.calendar/time/" + millis));
            remoteViews.setOnClickPendingIntent(R.id.header, PendingIntent.getActivity(context, 0, intent2, 0));
            remoteViews.setPendingIntentTemplate(R.id.events_list, getLaunchPendingIntentTemplate(context));
            appWidgetManager.updateAppWidget(i, remoteViews);
        }
    }

    static PendingIntent getUpdateIntent(Context context) {
        Intent intent = new Intent(Utils.getWidgetScheduledUpdateAction(context));
        intent.setDataAndType(CalendarContract.CONTENT_URI, "vnd.android.data/update");
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    static PendingIntent getLaunchPendingIntentTemplate(Context context) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        intent.setFlags(268484608);
        intent.setClass(context, AllInOneActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 134217728);
    }

    static Intent getLaunchFillInIntent(Context context, long j, long j2, long j3, boolean z) {
        Intent intent = new Intent();
        String str = "content://com.android.calendar/events";
        if (j != 0) {
            intent.putExtra("DETAIL_VIEW", true);
            intent.setFlags(268484608);
            str = "content://com.android.calendar/events/" + j;
            intent.setClass(context, EventInfoActivity.class);
        } else {
            intent.setClass(context, AllInOneActivity.class);
        }
        intent.setData(Uri.parse(str));
        intent.putExtra("beginTime", j2);
        intent.putExtra("endTime", j3);
        intent.putExtra("allDay", z);
        return intent;
    }
}
