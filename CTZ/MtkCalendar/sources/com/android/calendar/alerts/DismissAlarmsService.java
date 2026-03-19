package com.android.calendar.alerts;

import android.app.IntentService;
import android.app.TaskStackBuilder;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.CalendarContract;
import android.util.Log;
import com.android.calendar.EventInfoActivity;
import java.util.ArrayList;
import java.util.Iterator;

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
        Uri uri;
        ArrayList<ContentProviderOperation> arrayList;
        Log.d("DismissAlarmsService", "onReceive: a=" + intent.getAction() + " " + intent.toString());
        try {
            long longExtra = intent.getLongExtra("eventid", -1L);
            long longExtra2 = intent.getLongExtra("eventstart", -1L);
            long longExtra3 = intent.getLongExtra("eventend", -1L);
            long[] longArrayExtra = intent.getLongArrayExtra("eventids");
            intent.getIntExtra("notificationid", -1);
            boolean booleanExtra = intent.getBooleanExtra("eventshowed", false);
            Uri uri2 = CalendarContract.CalendarAlerts.CONTENT_URI;
            ArrayList<String> arrayList2 = new ArrayList<>();
            if (longExtra != -1) {
                AlertService.getEventIdToNotificationIdMap().remove(Long.valueOf(longExtra));
                arrayList2.add("(state=1 OR state=100) AND event_id=" + longExtra);
            } else if (longArrayExtra != null && longArrayExtra.length > 0) {
                buildMultipleEventsQuery(longArrayExtra, arrayList2);
                int length = longArrayExtra.length;
                int i = 0;
                while (i < length) {
                    AlertService.getEventIdToNotificationIdMap().remove(Long.valueOf(longArrayExtra[i]));
                    i++;
                    uri2 = uri2;
                }
            } else {
                uri = uri2;
                arrayList2.add("state=1 OR state=100");
                AlertService.getEventIdToNotificationIdMap().clear();
                ContentResolver contentResolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                long jCurrentTimeMillis = System.currentTimeMillis();
                String str = " AND end<=" + jCurrentTimeMillis;
                String str2 = " AND end>" + jCurrentTimeMillis;
                arrayList = new ArrayList<>();
                contentValues.put("state", (Integer) 2);
                if (!booleanExtra) {
                    Iterator<String> it = arrayList2.iterator();
                    while (it.hasNext()) {
                        Uri uri3 = uri;
                        arrayList.add(ContentProviderOperation.newUpdate(uri3).withValues(contentValues).withSelection(it.next(), null).build());
                        uri = uri3;
                    }
                } else {
                    Uri uri4 = uri;
                    for (Iterator<String> it2 = arrayList2.iterator(); it2.hasNext(); it2 = it2) {
                        String next = it2.next();
                        arrayList.add(ContentProviderOperation.newUpdate(uri4).withValues(contentValues).withSelection(next + str, null).build());
                    }
                    contentValues.put("state", (Integer) 100);
                    for (String str3 : arrayList2) {
                        arrayList.add(ContentProviderOperation.newUpdate(uri4).withValues(contentValues).withSelection(str3 + str2, null).build());
                    }
                }
                if (arrayList.size() > 0) {
                    try {
                        contentResolver.applyBatch("com.android.calendar", arrayList);
                    } catch (OperationApplicationException e) {
                        e.printStackTrace();
                    } catch (RemoteException e2) {
                        e2.printStackTrace();
                    }
                }
                AlertUtils.scheduleNextNotificationRefresh(this, null, System.currentTimeMillis());
                if ("com.android.calendar.SHOW".equals(intent.getAction())) {
                    Intent[] intents = TaskStackBuilder.create(this).addParentStack(EventInfoActivity.class).addNextIntent(AlertUtils.buildEventViewIntent(this, longExtra, longExtra2, longExtra3)).getIntents();
                    intents[0].setFlags(intents[0].getFlags() & (-16385));
                    startActivitiesAsUser(intents, null, new UserHandle(UserHandle.myUserId()));
                }
                AlertUtils.postUnreadNumber(this);
            }
            uri = uri2;
            ContentResolver contentResolver2 = getContentResolver();
            ContentValues contentValues2 = new ContentValues();
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            String str4 = " AND end<=" + jCurrentTimeMillis2;
            String str22 = " AND end>" + jCurrentTimeMillis2;
            arrayList = new ArrayList<>();
            contentValues2.put("state", (Integer) 2);
            if (!booleanExtra) {
            }
            if (arrayList.size() > 0) {
            }
            AlertUtils.scheduleNextNotificationRefresh(this, null, System.currentTimeMillis());
            if ("com.android.calendar.SHOW".equals(intent.getAction())) {
            }
            AlertUtils.postUnreadNumber(this);
        } catch (SecurityException e3) {
            Log.d("DismissAlarmsService", "Exception SecurityException ");
        }
    }

    private void buildMultipleEventsQuery(long[] jArr, ArrayList<String> arrayList) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append("state");
        sb.append("=");
        sb.append(1);
        sb.append(" OR ");
        sb.append("state");
        sb.append("=");
        sb.append(100);
        sb.append(")");
        StringBuilder sb2 = new StringBuilder();
        if (jArr.length > 0) {
            sb.append(" AND (");
            int length = jArr.length;
            for (int i = 0; i < length; i++) {
                sb2.append("event_id");
                sb2.append("=");
                sb2.append(jArr[i]);
                if (i != 0 && (i % 500 == 0 || i == length - 1)) {
                    sb2.append(")");
                    arrayList.add(sb.toString() + sb2.toString());
                    sb2.delete(0, sb2.length());
                } else {
                    sb2.append(" OR ");
                }
            }
        }
    }
}
