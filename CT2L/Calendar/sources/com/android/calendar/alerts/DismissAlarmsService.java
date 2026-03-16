package com.android.calendar.alerts;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import com.android.calendar.EventInfoActivity;
import com.android.calendar.alerts.GlobalDismissManager;
import java.util.LinkedList;
import java.util.List;

public class DismissAlarmsService extends IntentService {
    private static final String[] PROJECTION = {"state"};

    public DismissAlarmsService() {
        super("DismissAlarmsService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onHandleIntent(Intent intent) {
        String selection;
        Log.d("DismissAlarmsService", "onReceive: a=" + intent.getAction() + " " + intent.toString());
        long eventId = intent.getLongExtra("eventid", -1L);
        long eventStart = intent.getLongExtra("eventstart", -1L);
        long eventEnd = intent.getLongExtra("eventend", -1L);
        long[] eventIds = intent.getLongArrayExtra("eventids");
        long[] eventStarts = intent.getLongArrayExtra("starts");
        int notificationId = intent.getIntExtra("notificationid", -1);
        List<GlobalDismissManager.AlarmId> alarmIds = new LinkedList<>();
        Uri uri = CalendarContract.CalendarAlerts.CONTENT_URI;
        if (eventId != -1) {
            alarmIds.add(new GlobalDismissManager.AlarmId(eventId, eventStart));
            selection = "state=1 AND event_id=" + eventId;
        } else if (eventIds != null && eventIds.length > 0 && eventStarts != null && eventIds.length == eventStarts.length) {
            selection = buildMultipleEventsQuery(eventIds);
            for (int i = 0; i < eventIds.length; i++) {
                alarmIds.add(new GlobalDismissManager.AlarmId(eventIds[i], eventStarts[i]));
            }
        } else {
            selection = "state=1";
        }
        GlobalDismissManager.dismissGlobally(getApplicationContext(), alarmIds);
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(PROJECTION[0], (Integer) 2);
        resolver.update(uri, values, selection, null);
        if (notificationId != -1) {
            NotificationManager nm = (NotificationManager) getSystemService("notification");
            nm.cancel(notificationId);
        }
        if ("com.android.calendar.SHOW".equals(intent.getAction())) {
            Intent i2 = AlertUtils.buildEventViewIntent(this, eventId, eventStart, eventEnd);
            TaskStackBuilder.create(this).addParentStack(EventInfoActivity.class).addNextIntent(i2).startActivities();
        }
    }

    private String buildMultipleEventsQuery(long[] eventIds) {
        StringBuilder selection = new StringBuilder();
        selection.append("state");
        selection.append("=");
        selection.append(1);
        if (eventIds.length > 0) {
            selection.append(" AND (");
            selection.append("event_id");
            selection.append("=");
            selection.append(eventIds[0]);
            for (int i = 1; i < eventIds.length; i++) {
                selection.append(" OR ");
                selection.append("event_id");
                selection.append("=");
                selection.append(eventIds[i]);
            }
            selection.append(")");
        }
        return selection.toString();
    }
}
