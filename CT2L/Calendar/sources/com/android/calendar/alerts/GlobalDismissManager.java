package com.android.calendar.alerts;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.util.Pair;
import com.android.calendar.CloudNotificationBackplane;
import com.android.calendar.ExtensionsFactory;
import com.android.calendar.R;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GlobalDismissManager extends BroadcastReceiver {
    static final String[] EVENT_PROJECTION = {"_id", "calendar_id"};
    static final String[] EVENT_SYNC_PROJECTION = {"_id", "_sync_id"};
    static final String[] CALENDARS_PROJECTION = {"_id", "account_name", "account_type"};
    private static HashMap<GlobalDismissId, Long> sReceiverDismissCache = new HashMap<>();
    private static HashMap<LocalDismissId, Long> sSenderDismissCache = new HashMap<>();

    private static class GlobalDismissId {
        public final String mAccountName;
        public final long mStartTime;
        public final String mSyncId;

        private GlobalDismissId(String accountName, String syncId, long startTime) {
            if (accountName == null) {
                throw new IllegalArgumentException("Account Name can not be set to null");
            }
            if (syncId == null) {
                throw new IllegalArgumentException("SyncId can not be set to null");
            }
            this.mAccountName = accountName;
            this.mSyncId = syncId;
            this.mStartTime = startTime;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GlobalDismissId that = (GlobalDismissId) o;
            return this.mStartTime == that.mStartTime && this.mAccountName.equals(that.mAccountName) && this.mSyncId.equals(that.mSyncId);
        }

        public int hashCode() {
            int result = this.mAccountName.hashCode();
            return (((result * 31) + this.mSyncId.hashCode()) * 31) + ((int) (this.mStartTime ^ (this.mStartTime >>> 32)));
        }
    }

    public static class LocalDismissId {
        public final String mAccountName;
        public final String mAccountType;
        public final long mEventId;
        public final long mStartTime;

        public LocalDismissId(String accountType, String accountName, long eventId, long startTime) {
            if (accountType == null) {
                throw new IllegalArgumentException("Account Type can not be null");
            }
            if (accountName == null) {
                throw new IllegalArgumentException("Account Name can not be null");
            }
            this.mAccountType = accountType;
            this.mAccountName = accountName;
            this.mEventId = eventId;
            this.mStartTime = startTime;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LocalDismissId that = (LocalDismissId) o;
            return this.mEventId == that.mEventId && this.mStartTime == that.mStartTime && this.mAccountName.equals(that.mAccountName) && this.mAccountType.equals(that.mAccountType);
        }

        public int hashCode() {
            int result = this.mAccountType.hashCode();
            return (((((result * 31) + this.mAccountName.hashCode()) * 31) + ((int) (this.mEventId ^ (this.mEventId >>> 32)))) * 31) + ((int) (this.mStartTime ^ (this.mStartTime >>> 32)));
        }
    }

    public static class AlarmId {
        public long mEventId;
        public long mStart;

        public AlarmId(long id, long start) {
            this.mEventId = id;
            this.mStart = start;
        }
    }

    public static void processEventIds(Context context, Set<Long> eventIds) {
        String senderId = context.getResources().getString(R.string.notification_sender_id);
        if (senderId == null || senderId.isEmpty()) {
            Log.i("GlobalDismissManager", "no sender configured");
            return;
        }
        Map<Long, Long> eventsToCalendars = lookupEventToCalendarMap(context, eventIds);
        Set<Long> calendars = new LinkedHashSet<>();
        calendars.addAll(eventsToCalendars.values());
        if (calendars.isEmpty()) {
            Log.d("GlobalDismissManager", "found no calendars for events");
            return;
        }
        Map<Long, Pair<String, String>> calendarsToAccounts = lookupCalendarToAccountMap(context, calendars);
        if (calendarsToAccounts.isEmpty()) {
            Log.d("GlobalDismissManager", "found no accounts for calendars");
            return;
        }
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet();
        for (Pair<String, String> accountPair : calendarsToAccounts.values()) {
            if ("com.google".equals(accountPair.first)) {
                linkedHashSet.add(accountPair.second);
            }
        }
        SharedPreferences prefs = context.getSharedPreferences("com.android.calendar.alerts.GDM", 0);
        Set<String> existingAccounts = prefs.getStringSet("known_accounts", new HashSet());
        linkedHashSet.removeAll(existingAccounts);
        if (!linkedHashSet.isEmpty()) {
            CloudNotificationBackplane cnb = ExtensionsFactory.getCloudNotificationBackplane();
            if (cnb.open(context)) {
                for (String account : linkedHashSet) {
                    try {
                        if (cnb.subscribeToGroup(senderId, account, account)) {
                            existingAccounts.add(account);
                        }
                    } catch (IOException e) {
                    }
                }
                cnb.close();
                prefs.edit().putStringSet("known_accounts", existingAccounts).commit();
            }
        }
    }

    public static void syncSenderDismissCache(Context context) {
        String senderId = context.getResources().getString(R.string.notification_sender_id);
        if ("".equals(senderId)) {
            Log.i("GlobalDismissManager", "no sender configured");
            return;
        }
        CloudNotificationBackplane cnb = ExtensionsFactory.getCloudNotificationBackplane();
        if (!cnb.open(context)) {
            Log.i("GlobalDismissManager", "Unable to open cloud notification backplane");
        }
        long currentTime = System.currentTimeMillis();
        ContentResolver resolver = context.getContentResolver();
        synchronized (sSenderDismissCache) {
            Iterator<Map.Entry<LocalDismissId, Long>> it = sSenderDismissCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<LocalDismissId, Long> entry = it.next();
                LocalDismissId dismissId = entry.getKey();
                Uri uri = asSync(CalendarContract.Events.CONTENT_URI, dismissId.mAccountType, dismissId.mAccountName);
                Cursor cursor = resolver.query(uri, EVENT_SYNC_PROJECTION, "_id = " + dismissId.mEventId, null, null);
                try {
                    cursor.moveToPosition(-1);
                    int sync_id_idx = cursor.getColumnIndex("_sync_id");
                    if (sync_id_idx != -1) {
                        while (cursor.moveToNext()) {
                            String syncId = cursor.getString(sync_id_idx);
                            if (syncId != null) {
                                Bundle data = new Bundle();
                                long startTime = dismissId.mStartTime;
                                String accountName = dismissId.mAccountName;
                                data.putString("com.android.calendar.alerts.sync_id", syncId);
                                data.putString("com.android.calendar.alerts.start_time", Long.toString(startTime));
                                data.putString("com.android.calendar.alerts.account_name", accountName);
                                try {
                                    cnb.send(accountName, syncId + ":" + startTime, data);
                                    it.remove();
                                } catch (IOException e) {
                                }
                            }
                        }
                    }
                    cursor.close();
                    if (currentTime - entry.getValue().longValue() > 3600000) {
                        it.remove();
                    }
                } catch (Throwable th) {
                    cursor.close();
                    throw th;
                }
            }
        }
        cnb.close();
    }

    public static void dismissGlobally(Context context, List<AlarmId> alarmIds) {
        Set<Long> eventIds = new HashSet<>(alarmIds.size());
        Iterator<AlarmId> it = alarmIds.iterator();
        while (it.hasNext()) {
            eventIds.add(Long.valueOf(it.next().mEventId));
        }
        Map<Long, Long> eventsToCalendars = lookupEventToCalendarMap(context, eventIds);
        if (eventsToCalendars.isEmpty()) {
            Log.d("GlobalDismissManager", "found no calendars for events");
            return;
        }
        Set<Long> calendars = new LinkedHashSet<>();
        calendars.addAll(eventsToCalendars.values());
        Map<Long, Pair<String, String>> calendarsToAccounts = lookupCalendarToAccountMap(context, calendars);
        if (calendarsToAccounts.isEmpty()) {
            Log.d("GlobalDismissManager", "found no accounts for calendars");
            return;
        }
        long currentTime = System.currentTimeMillis();
        for (AlarmId alarmId : alarmIds) {
            Long calendar = eventsToCalendars.get(Long.valueOf(alarmId.mEventId));
            Pair<String, String> account = calendarsToAccounts.get(calendar);
            if ("com.google".equals(account.first)) {
                LocalDismissId dismissId = new LocalDismissId((String) account.first, (String) account.second, alarmId.mEventId, alarmId.mStart);
                synchronized (sSenderDismissCache) {
                    sSenderDismissCache.put(dismissId, Long.valueOf(currentTime));
                }
            }
        }
        syncSenderDismissCache(context);
    }

    private static Uri asSync(Uri uri, String accountType, String account) {
        return uri.buildUpon().appendQueryParameter("caller_is_syncadapter", "true").appendQueryParameter("account_name", account).appendQueryParameter("account_type", accountType).build();
    }

    private static String buildMultipleIdQuery(Set<Long> ids, String key) {
        StringBuilder selection = new StringBuilder();
        boolean first = true;
        for (Long id : ids) {
            if (first) {
                first = false;
            } else {
                selection.append(" OR ");
            }
            selection.append(key);
            selection.append("=");
            selection.append(id);
        }
        return selection.toString();
    }

    private static Map<Long, Long> lookupEventToCalendarMap(Context context, Set<Long> eventIds) {
        Map<Long, Long> eventsToCalendars = new HashMap<>();
        ContentResolver resolver = context.getContentResolver();
        String eventSelection = buildMultipleIdQuery(eventIds, "_id");
        Cursor eventCursor = resolver.query(CalendarContract.Events.CONTENT_URI, EVENT_PROJECTION, eventSelection, null, null);
        try {
            eventCursor.moveToPosition(-1);
            int calendar_id_idx = eventCursor.getColumnIndex("calendar_id");
            int event_id_idx = eventCursor.getColumnIndex("_id");
            if (calendar_id_idx != -1 && event_id_idx != -1) {
                while (eventCursor.moveToNext()) {
                    eventsToCalendars.put(Long.valueOf(eventCursor.getLong(event_id_idx)), Long.valueOf(eventCursor.getLong(calendar_id_idx)));
                }
            }
            return eventsToCalendars;
        } finally {
            eventCursor.close();
        }
    }

    private static Map<Long, Pair<String, String>> lookupCalendarToAccountMap(Context context, Set<Long> calendars) {
        Map<Long, Pair<String, String>> calendarsToAccounts = new HashMap<>();
        ContentResolver resolver = context.getContentResolver();
        String calendarSelection = buildMultipleIdQuery(calendars, "_id");
        Cursor calendarCursor = resolver.query(CalendarContract.Calendars.CONTENT_URI, CALENDARS_PROJECTION, calendarSelection, null, null);
        try {
            calendarCursor.moveToPosition(-1);
            int calendar_id_idx = calendarCursor.getColumnIndex("_id");
            int account_name_idx = calendarCursor.getColumnIndex("account_name");
            int account_type_idx = calendarCursor.getColumnIndex("account_type");
            if (calendar_id_idx != -1 && account_name_idx != -1 && account_type_idx != -1) {
                while (calendarCursor.moveToNext()) {
                    Long id = Long.valueOf(calendarCursor.getLong(calendar_id_idx));
                    String name = calendarCursor.getString(account_name_idx);
                    String type = calendarCursor.getString(account_type_idx);
                    if (name != null && type != null) {
                        calendarsToAccounts.put(id, new Pair<>(type, name));
                    }
                }
            }
            return calendarsToAccounts;
        } finally {
            calendarCursor.close();
        }
    }

    public static void syncReceiverDismissCache(Context context) {
        ContentResolver resolver = context.getContentResolver();
        long currentTime = System.currentTimeMillis();
        synchronized (sReceiverDismissCache) {
            Iterator<Map.Entry<GlobalDismissId, Long>> it = sReceiverDismissCache.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<GlobalDismissId, Long> entry = it.next();
                GlobalDismissId globalDismissId = entry.getKey();
                Uri uri = asSync(CalendarContract.Events.CONTENT_URI, "com.google", globalDismissId.mAccountName);
                Cursor cursor = resolver.query(uri, EVENT_SYNC_PROJECTION, "_sync_id = '" + globalDismissId.mSyncId + "'", null, null);
                try {
                    int event_id_idx = cursor.getColumnIndex("_id");
                    cursor.moveToFirst();
                    if (event_id_idx != -1 && !cursor.isAfterLast()) {
                        long eventId = cursor.getLong(event_id_idx);
                        ContentValues values = new ContentValues();
                        String selection = "(state=1 OR state=0) AND event_id=" + eventId + " AND begin=" + globalDismissId.mStartTime;
                        values.put("state", (Integer) 2);
                        int rows = resolver.update(CalendarContract.CalendarAlerts.CONTENT_URI, values, selection, null);
                        if (rows > 0) {
                            it.remove();
                        }
                    }
                    cursor.close();
                    if (currentTime - entry.getValue().longValue() > 3600000) {
                        it.remove();
                    }
                } catch (Throwable th) {
                    cursor.close();
                    throw th;
                }
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        new AsyncTask<Pair<Context, Intent>, Void, Void>() {
            @Override
            protected Void doInBackground(Pair<Context, Intent>... params) {
                Context context2 = (Context) params[0].first;
                Intent intent2 = (Intent) params[0].second;
                if (intent2.hasExtra("com.android.calendar.alerts.sync_id") && intent2.hasExtra("com.android.calendar.alerts.account_name") && intent2.hasExtra("com.android.calendar.alerts.start_time")) {
                    synchronized (GlobalDismissManager.sReceiverDismissCache) {
                        GlobalDismissManager.sReceiverDismissCache.put(new GlobalDismissId(intent2.getStringExtra("com.android.calendar.alerts.account_name"), intent2.getStringExtra("com.android.calendar.alerts.sync_id"), Long.parseLong(intent2.getStringExtra("com.android.calendar.alerts.start_time"))), Long.valueOf(System.currentTimeMillis()));
                    }
                    AlertService.updateAlertNotification(context2);
                }
                return null;
            }
        }.execute(new Pair(context, intent));
    }
}
