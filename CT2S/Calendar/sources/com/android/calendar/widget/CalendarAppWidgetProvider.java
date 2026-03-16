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
import android.widget.RemoteViews;
import com.android.calendar.AllInOneActivity;
import com.android.calendar.EventInfoActivity;
import com.android.calendar.R;
import com.android.calendar.Utils;

public class CalendarAppWidgetProvider extends AppWidgetProvider {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Utils.getWidgetUpdateAction(context).equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            performUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(getComponentName(context)), null);
        } else if (action.equals("android.intent.action.PROVIDER_CHANGED") || action.equals("android.intent.action.TIME_SET") || action.equals("android.intent.action.TIMEZONE_CHANGED") || action.equals("android.intent.action.DATE_CHANGED") || action.equals(Utils.getWidgetScheduledUpdateAction(context))) {
            Intent service = new Intent(context, (Class<?>) CalendarAppWidgetService.class);
            context.startService(service);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService("alarm");
        PendingIntent pendingUpdate = getUpdateIntent(context);
        am.cancel(pendingUpdate);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        performUpdate(context, appWidgetManager, appWidgetIds, null);
    }

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, (Class<?>) CalendarAppWidgetProvider.class);
    }

    private void performUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds, long[] changedEventIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent updateIntent = new Intent(context, (Class<?>) CalendarAppWidgetService.class);
            updateIntent.putExtra("appWidgetId", appWidgetId);
            if (changedEventIds != null) {
                updateIntent.putExtra("com.android.calendar.EXTRA_EVENT_IDS", changedEventIds);
            }
            updateIntent.setData(Uri.parse(updateIntent.toUri(1)));
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.appwidget);
            Time time = new Time(Utils.getTimeZone(context, null));
            time.setToNow();
            long millis = time.toMillis(true);
            String dayOfWeek = DateUtils.getDayOfWeekString(time.weekDay + 1, 20);
            String date = Utils.formatDateRange(context, millis, millis, 524312);
            views.setTextViewText(R.id.day_of_week, dayOfWeek);
            views.setTextViewText(R.id.date, date);
            views.setRemoteAdapter(appWidgetId, R.id.events_list, updateIntent);
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.events_list);
            Intent launchCalendarIntent = new Intent("android.intent.action.VIEW");
            launchCalendarIntent.setClass(context, AllInOneActivity.class);
            launchCalendarIntent.setData(Uri.parse("content://com.android.calendar/time/" + millis));
            PendingIntent launchCalendarPendingIntent = PendingIntent.getActivity(context, 0, launchCalendarIntent, 0);
            views.setOnClickPendingIntent(R.id.header, launchCalendarPendingIntent);
            PendingIntent updateEventIntent = getLaunchPendingIntentTemplate(context);
            views.setPendingIntentTemplate(R.id.events_list, updateEventIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    static PendingIntent getUpdateIntent(Context context) {
        Intent intent = new Intent(Utils.getWidgetScheduledUpdateAction(context));
        intent.setDataAndType(CalendarContract.CONTENT_URI, "vnd.android.data/update");
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    static PendingIntent getLaunchPendingIntentTemplate(Context context) {
        Intent launchIntent = new Intent();
        launchIntent.setAction("android.intent.action.VIEW");
        launchIntent.setFlags(268484608);
        launchIntent.setClass(context, AllInOneActivity.class);
        return PendingIntent.getActivity(context, 0, launchIntent, 134217728);
    }

    static Intent getLaunchFillInIntent(Context context, long id, long start, long end, boolean allDay) {
        Intent fillInIntent = new Intent();
        String dataString = "content://com.android.calendar/events";
        if (id != 0) {
            fillInIntent.putExtra("DETAIL_VIEW", true);
            fillInIntent.setFlags(268484608);
            dataString = "content://com.android.calendar/events/" + id;
            fillInIntent.setClass(context, EventInfoActivity.class);
        } else {
            fillInIntent.setClass(context, AllInOneActivity.class);
        }
        Uri data = Uri.parse(dataString);
        fillInIntent.setData(data);
        fillInIntent.putExtra("beginTime", start);
        fillInIntent.putExtra("endTime", end);
        fillInIntent.putExtra("allDay", allDay);
        return fillInIntent;
    }
}
